package biotex;

import java.math.*;
import java.util.*;

/** Pure decimal arithmetic. A missing input is an issue, never a zero price. */
public final class CostEngine {
    public static final MathContext MC=new MathContext(20,RoundingMode.HALF_UP);
    public static BigDecimal d(String s) { if(s==null||s.isBlank())return null;return new BigDecimal(s.trim().replace(',','.')); }
    public static BigDecimal required(String s,String field) {BigDecimal v=d(s);if(v==null)throw new IllegalArgumentException(field+" eksik");return v;}
    public record Line(String label,String method,String unit,String widthCm,String heightCm,String repeats,String manualQty,String wasteRate,String price,String currency,String rate,String factor,String gsm) {}
    public record LineResult(BigDecimal net,BigDecimal physicalWaste,BigDecimal costQuantity,BigDecimal tryPrice,BigDecimal cost,String detail,List<String> issues) {}
    public record Operation(String name,String duration,String timeUnit,String people,String repeats,String minuteRate,String tolerance,boolean corrected,String scope) {}
    public record Rule(String name,String kind,String basis,String rate,String amount,String divisor,boolean sterileOnly,String scope) {}
    public record Estimate(BigDecimal material,BigDecimal labor,BigDecimal extras,BigDecimal orderFixed,BigDecimal unitCost,BigDecimal unitPrice,BigDecimal orderTotal,List<LineResult> lines,List<String> issues,Map<String,BigDecimal> breakdown) {
        public boolean approvable(){return issues.isEmpty()&&unitCost!=null&&unitPrice!=null&&orderTotal!=null;}
    }
    private static BigDecimal nz(BigDecimal x){return x==null?BigDecimal.ZERO:x;}
    private static BigDecimal positive(BigDecimal x,String label){if(x==null||x.signum()<=0)throw new IllegalArgumentException(label+" sıfırdan büyük olmalı");return x;}
    private static BigDecimal nonnegative(BigDecimal x,String label){if(x==null||x.signum()<0)throw new IllegalArgumentException(label+" negatif olamaz");return x;}
    public static LineResult line(Line l){
        List<String> issues=new ArrayList<>();
        try {
            BigDecimal repeats=positive(required(l.repeats(),"Tekrar"),"Tekrar");
            BigDecimal net;
            switch(l.method()) {
                case "AREA" -> net=positive(required(l.widthCm(),"En"),"En").multiply(positive(required(l.heightCm(),"Boy"),"Boy"),MC).divide(new BigDecimal("10000"),MC).multiply(repeats,MC);
                case "LENGTH" -> net=positive(required(l.heightCm(),"Boy"),"Boy").divide(new BigDecimal("100"),MC).multiply(repeats,MC);
                case "EACH" -> net=repeats;
                case "LEGACY_MT" -> net=positive(required(l.heightCm(),"Boy"),"Boy").divide(new BigDecimal("100"),MC);
                case "MANUAL", "WEIGHT" -> net=nonnegative(required(l.manualQty(),"Tüketim"),"Tüketim");
                default -> throw new IllegalArgumentException("Bilinmeyen tüketim yöntemi");
            }
            if ("KG".equals(l.unit()) && "AREA".equals(l.method())) {
                BigDecimal gsm=positive(d(l.gsm()),"Gramaj");
                net=net.multiply(gsm,MC).divide(new BigDecimal("1000"),MC);
            }
            BigDecimal waste=nonnegative(nz(d(l.wasteRate())),"Fire");
            BigDecimal physical=net.multiply(waste,MC);
            BigDecimal costQty=net.add(physical,MC);
            BigDecimal factor=positive(l.factor()==null?BigDecimal.ONE:d(l.factor()),"Maliyet katsayısı");
            BigDecimal price=nonnegative(required(l.price(),"Birim fiyat"),"Birim fiyat");
            BigDecimal rate="TRY".equals(l.currency())||"TRL".equals(l.currency())?BigDecimal.ONE:positive(d(l.rate()),"Döviz kuru");
            BigDecimal tryPrice=price.multiply(rate,MC);
            BigDecimal cost=costQty.multiply(tryPrice,MC).multiply(factor,MC);
            return new LineResult(net,physical,costQty,tryPrice,cost,net.toPlainString()+" "+l.unit()+" + fire "+physical.toPlainString()+" × "+price.toPlainString()+" "+l.currency()+" × kur "+rate.toPlainString()+" × katsayı "+factor.toPlainString(),issues);
        } catch(Exception e){issues.add(l.label()+": "+e.getMessage());return new LineResult(null,null,null,null,null,"",issues);}
    }
    public static BigDecimal labor(Operation o){
        BigDecimal duration=nonnegative(required(o.duration(),"Süre"),"Süre");
        BigDecimal minutes="SEC".equals(o.timeUnit())?duration.divide(new BigDecimal("60"),MC):duration;
        if(o.corrected())minutes=minutes.divide(positive(d(o.tolerance()),"Tolerans"),MC);
        return minutes.multiply(positive(required(o.people(),"Kişi"),"Kişi"),MC).multiply(positive(required(o.repeats(),"Tekrar"),"Tekrar"),MC).multiply(nonnegative(required(o.minuteRate(),"Dakika maliyeti"),"Dakika maliyeti"),MC);
    }
    public static BigDecimal salePrice(BigDecimal cost,BigDecimal rate,String method){
        nonnegative(cost,"Maliyet");nonnegative(rate,"Kâr oranı");
        if("MARGIN".equals(method)){if(rate.compareTo(BigDecimal.ONE)>=0)throw new IllegalArgumentException("Satış marjı %100 olamaz");return cost.divide(BigDecimal.ONE.subtract(rate),MC);}
        return cost.multiply(BigDecimal.ONE.add(rate),MC);
    }
    public static Estimate estimate(List<Line> lines,List<Operation> operations,List<Rule> rules,String orderQuantity,String markup,String markupMethod,boolean sterile){
        List<String> issues=new ArrayList<>();List<LineResult> detail=new ArrayList<>();Map<String,BigDecimal> breakdown=new LinkedHashMap<>();
        BigDecimal material=BigDecimal.ZERO,laborCost=BigDecimal.ZERO,extras=BigDecimal.ZERO,orderFixed=BigDecimal.ZERO,qty=null;
        try{qty=positive(d(orderQuantity),"Sipariş adedi");}catch(Exception e){issues.add(e.getMessage());}
        for(Line l:lines){LineResult r=line(l);detail.add(r);issues.addAll(r.issues());if(r.cost()!=null)material=material.add(r.cost(),MC);}
        for(Operation o:operations)try{if("BATCH".equals(o.scope()))orderFixed=orderFixed.add(labor(o),MC);else laborCost=laborCost.add(labor(o),MC);}catch(Exception e){issues.add(o.name()+": "+e.getMessage());}
        BigDecimal base=material.add(laborCost,MC);
        for(Rule rule:rules){
            if(rule.sterileOnly()&&!sterile)continue;
            try{
                BigDecimal value;
                switch(rule.kind()){
                    case "RATE" -> {BigDecimal basis="MATERIAL".equals(rule.basis())?material:base;value=basis.multiply(nonnegative(required(rule.rate(),"Oran"),"Oran"),MC);}
                    case "FIXED" -> value=nonnegative(required(rule.amount(),"Tutar"),"Tutar");
                    case "DIVIDE" -> value=nonnegative(required(rule.amount(),"Tutar"),"Tutar").divide(positive(d(rule.divisor()),"Bölücü"),MC);
                    default -> throw new IllegalArgumentException("Bilinmeyen kural");
                }
                if("ORDER".equals(rule.scope()))orderFixed=orderFixed.add(value,MC);else extras=extras.add(value,MC);breakdown.put(rule.name(),value);
            }catch(Exception e){issues.add(rule.name()+": "+e.getMessage());}
        }
        BigDecimal cost=issues.isEmpty()?base.add(extras,MC):null,price=null,total=null;
        if(cost!=null)try{price=salePrice(cost,nz(d(markup)),markupMethod);total=qty==null?null:price.multiply(qty,MC).add(salePrice(orderFixed,nz(d(markup)),markupMethod),MC);}catch(Exception e){issues.add(e.getMessage());}
        return new Estimate(material,laborCost,extras,orderFixed,cost,price,total,detail,issues,breakdown);
    }
}
