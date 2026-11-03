package biotex;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.zip.*;

/** ZIP/XLSX preview and atomic import. Never evaluates spreadsheet formulas. */
public final class Importer {
    public record Issue(String severity,String message,String sheet,String cell) {}
    public record Preview(String path,String sha256,String template,String productName,int recipeRows,List<Issue> issues,byte[] bytes) {
        public String display(){return productName+" · "+template+" · "+recipeRows+" satır · "+issues.size()+" sorun";}
    }
    private static String hash(byte[] bytes)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder b=new StringBuilder();for(byte x:h)b.append(String.format("%02x",x));return b.toString();}
    private static boolean filled(String s){return s!=null&&!s.isBlank();}
    private static boolean numeric(String s){try{CostEngine.d(s);return filled(s);}catch(Exception e){return false;}}
    public static List<Preview> preview(Path source)throws Exception {
        List<Preview> out=new ArrayList<>();
        byte[] bytes=Files.readAllBytes(source);
        walk(source.getFileName().toString(),bytes,out,0);
        return out;
    }
    private static void walk(String path,byte[] bytes,List<Preview> out,int depth)throws Exception{
        if(depth>2||bytes.length>110_000_000)throw new IOException("Arşiv sınırı aşıldı");
        if(path.toLowerCase(Locale.ROOT).endsWith(".xlsx")){out.add(inspect(path,bytes));return;}
        if(!path.toLowerCase(Locale.ROOT).endsWith(".zip"))return;
        try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(bytes),java.nio.charset.Charset.forName("IBM857"))){
            ZipEntry e;int count=0;
            while((e=z.getNextEntry())!=null){
                String entry=e.getName().replace('\\','/');
                if(entry.startsWith("/")||entry.contains("../")||entry.matches("^[A-Za-z]:.*"))throw new IOException("Güvensiz ZIP yolu: "+entry);
                if(e.isDirectory()||!(entry.toLowerCase(Locale.ROOT).endsWith(".xlsx")||entry.toLowerCase(Locale.ROOT).endsWith(".zip")))continue;
                if(++count>200)throw new IOException("Arşivde çok fazla dosya");
                ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
                while((n=z.read(buffer))!=-1){b.write(buffer,0,n);if(b.size()>110_000_000)throw new IOException("ZIP girdisi çok büyük");}
                walk(path+" / "+entry,b.toByteArray(),out,depth+1);
            }
        }
    }
    private static Preview inspect(String path,byte[] bytes)throws Exception{
        String sha=hash(bytes); XlsxReader.Book book=XlsxReader.read(bytes);
        XlsxReader.Sheet recipe=book.sheet("Br. Malzeme Maliyeti"), home=book.sheet("Ana Sayfa");
        String name=home==null?"":home.value("C9");
        List<Issue> issues=new ArrayList<>();
        if(!filled(name)){name=Path.of(path.replace(" / ","_")).getFileName().toString();issues.add(new Issue("UYARI","Ürün adı hücrede bulunamadı","Ana Sayfa","C9"));}
        String template="BİLİNMİYOR";int used=0;
        XlsxReader.Sheet supplierSheet=book.sheet("Tedarikçi Fiyat Listesi");
        if(supplierSheet!=null){template="TEDARİKÇİ";name="Tedarikçi Fiyat Listesi";for(int r=7;r<=5000;r++)if(numeric(supplierSheet.value("O"+r))&&filled(supplierSheet.value("I"+r)))used++;issues.add(new Issue("BİLGİ","Kaynak fiyatları alternatif olarak saklanır; güncel fiyat seçimi gerekir","Tedarikçi Fiyat Listesi","O5"));}
        else if(book.sheet("acıbadem kısa excell")!=null||path.toLowerCase(Locale.ROOT).contains("ac")){if(recipe==null){template="ANALİZ";name="Karşılaştırmalı analiz";issues.add(new Issue("BİLGİ","Karşılaştırmalı analiz reçete yerine otomatik maliyet olarak aktarılmadı","",""));}}
        if(recipe!=null){
            if(recipe.value("L6").contains("MİKTAR")||recipe.value("L6").contains("Miktar"))template="SET";
            else if(recipe.value("J6").contains("MİKTAR")||recipe.value("J6").contains("Miktar"))template="ÖNLÜK";
            else if(filled(recipe.value("U6")))template="SET";
            else if(filled(recipe.value("Q6")))template="ÖNLÜK";
            for(int r=7;r<=200;r++){
                String desc=recipe.value(("SET".equals(template)?"E":"D")+r);
                if(!filled(desc))continue;
                used++;
                String qCol="SET".equals(template)?"L":"J",pCol="SET".equals(template)?"U":"Q";
                var quantity=recipe.cell(qCol+r);var price=recipe.cell(pCol+r);
                if(quantity.isFormula()&&quantity.formula().contains("\"MT\"")&&!quantity.formula().contains("*K")&&!quantity.formula().contains("*I"))issues.add(new Issue("BİLGİ","Eski MT formülü tekrar adedini uygulamayabilir","Br. Malzeme Maliyeti",qCol+r));
                if(!quantity.isFormula()&&numeric(quantity.value()))issues.add(new Issue("BİLGİ","Tüketim manuel sabit değer","Br. Malzeme Maliyeti",qCol+r));
                if(!numeric(price.value())||"0".equals(price.value()))issues.add(new Issue("UYARI","Güncel fiyat seçimi gerekli; kaynak sıfırı doğrulanmış fiyat değildir","Br. Malzeme Maliyeti",pCol+r));
            }
            if("SET".equals(template)&&recipe.cell("Q7").formula()!=null&&recipe.cell("Q7").formula().contains(",11,"))issues.add(new Issue("HATA","Aralık dışı VLOOKUP sütunu; IFERROR sonucu gizliyor","Br. Malzeme Maliyeti","Q7"));
        } else if("BİLİNMİYOR".equals(template))issues.add(new Issue("UYARI","Reçete sayfası bulunamadı","",""));
        if(!book.externalLinks().isEmpty())issues.add(new Issue("UYARI","Dış çalışma kitabı bağlantısı var; otomatik çözülmedi","",""));
        var total=book.sheet("Toplam Maliyeti");
        if(total!=null){
            var f8=total.cell("F8");
            if(filled(f8.value())&&!f8.isFormula())issues.add(new Issue("UYARI","Toplam maliyet önbelleği manuel sabit; canlı hesap değildir","Toplam Maliyeti","F8"));
            for(var e:total.cells().entrySet())if(e.getValue().isFormula()&&e.getValue().formula().contains("[1]"))issues.add(new Issue("HATA","Dış kitap formülü çözülmedi","Toplam Maliyeti",e.getKey()));
        }
        if("BİLİNMİYOR".equals(template))issues.add(new Issue("HATA","Şablon tanınamadı; reçete aktarılmayacak","Br. Malzeme Maliyeti","6"));
        return new Preview(path,sha,template,name,used,issues,bytes);
    }
    private record Prepared(Preview preview,XlsxReader.Book book){}
    public static int commit(JsonStore store,List<Preview> chosen)throws Exception{
        List<Prepared> prepared=new ArrayList<>();for(Preview p:chosen)prepared.add(new Prepared(p,XlsxReader.read(p.bytes())));
        return store.update(state->{int added=0;
            for(Prepared item:prepared){Preview p=item.preview();
                if(state.imports.stream().anyMatch(r->p.sha256().equals(r.get("sha256"))))continue;
                long batch=JsonStore.num(state.add("imports",JsonStore.map("sha256",p.sha256(),"path",p.path(),"name",p.productName(),"template",p.template(),"at",Instant.now().toString(),"status","IMPORTED")).get("id"));
                for(Issue issue:p.issues())state.add("issues",JsonStore.map("batchId",batch,"severity",issue.severity(),"message",issue.message(),"sheet",issue.sheet(),"cell",issue.cell()));
                if("TEDARİKÇİ".equals(p.template())){importSupplier(state,item.book().sheet("Tedarikçi Fiyat Listesi"),p,batch);added++;continue;}
                if("ANALİZ".equals(p.template())||"BİLİNMİYOR".equals(p.template()))continue;
                if(state.products.stream().anyMatch(r->p.productName().equals(r.get("name"))&&!p.sha256().equals(r.get("importHash"))))state.add("issues",JsonStore.map("batchId",batch,"severity","UYARI","message","Aynı adlı farklı içerik ayrı sürüm olarak saklandı","sheet","Ana Sayfa","cell","C9"));
                String code="IMP-"+p.sha256().substring(0,10).toUpperCase(Locale.ROOT);
                long product=JsonStore.num(state.add("products",JsonStore.map("code",code,"name",p.productName(),"category","Aktarılan","size","","sterile",false,"active",true,"revision",1,"profileId",1,"importHash",p.sha256(),"sourcePath",p.path())).get("id"));
                XlsxReader.Sheet recipe=item.book().sheet("Br. Malzeme Maliyeti");Long component=null;
                for(int row=7;row<=200;row++){
                    String desc=recipe.value(("SET".equals(p.template())?"E":"D")+row);if(!filled(desc))continue;
                    String componentName=recipe.value("B"+row);
                    if(filled(componentName))component=JsonStore.num(state.add("components",JsonStore.map("productId",product,"position",row,"name",componentName,"quantity","1","purchased",false)).get("id"));
                    String qCol="SET".equals(p.template())?"L":"J",unitCol="SET".equals(p.template())?"G":"E",widthCol="SET".equals(p.template())?"I":"G",heightCol="SET".equals(p.template())?"J":"H",repeatCol="SET".equals(p.template())?"K":"I",priceCol="SET".equals(p.template())?"U":"Q",costCol="SET".equals(p.template())?"V":"R";
                    XlsxReader.Cell q=recipe.cell(qCol+row);String unit=recipe.value(unitCol+row);if(!List.of("AD","MT","M2","KG").contains(unit))unit="AD";
                    String method=q.isFormula()?(unit.equals("MT")?"LEGACY_MT":unit.equals("M2")?"AREA":"EACH"):"MANUAL";
                    String stock="SET".equals(p.template())?recipe.value("D"+row):"";Long material=null;
                    if(filled(stock)){var existing=state.materials.stream().filter(r->stock.equals(r.get("code"))).findFirst();material=existing.isPresent()?JsonStore.num(existing.get().get("id")):JsonStore.num(state.add("materials",JsonStore.map("code",stock,"name",desc,"unit",unit,"active",true)).get("id"));}
                    state.add("recipeLines",JsonStore.map("productId",product,"componentId",component,"materialId",material,"description",desc,"unit",unit,"method",method,"widthCm",recipe.value(widthCol+row),"heightCm",recipe.value(heightCol+row),"repeats",numeric(recipe.value(repeatCol+row))?recipe.value(repeatCol+row):"1","manualQty",numeric(q.value())?q.value():null,"wasteRate","0","costFactor",null,"manualPrice",null,"manualCurrency",null,"sourcePath",p.path(),"sourceSheet","Br. Malzeme Maliyeti","sourceCell",qCol+row,"sourceKind",q.isFormula()?"FORMULA":"CONSTANT","sourceFormula",q.formula(),"sourceCached",recipe.value(costCol+row),"note","Kaynak fiyat: "+recipe.value(priceCol+row)+"; güncel fiyat seçilmedi"));
                }
                state.log("products",product,"IMPORT",p.path());added++;
            }
            return added;
        });
    }
    private static void importSupplier(JsonStore.State state,XlsxReader.Sheet sheet,Preview p,long batch){
        for(int row=7;row<=5000;row++){
            String name=sheet.value("I"+row),amount=sheet.value("O"+row),supplierName=sheet.value("E"+row);
            if(!filled(name)||!numeric(amount))continue;
            String code=sheet.value("H"+row),unit=sheet.value("L"+row),currency=sheet.value("N"+row);
            if("TL".equalsIgnoreCase(currency)||"TRL".equalsIgnoreCase(currency))currency="TRY";
            if(!List.of("TRY","USD","EUR").contains(currency)){state.add("issues",JsonStore.map("batchId",batch,"severity","UYARI","message","Para birimi tanınmadı","sheet","Tedarikçi Fiyat Listesi","cell","N"+row));continue;}
            if(!filled(supplierName))supplierName="Belirtilmemiş tedarikçi";String supplierFinal=supplierName;
            var supplier=state.suppliers.stream().filter(r->supplierFinal.equals(r.get("name"))).findFirst().orElseGet(()->state.add("suppliers",JsonStore.map("name",supplierFinal)));
            if(!filled(code))code="SUP-"+p.sha256().substring(0,8).toUpperCase(Locale.ROOT)+"-"+row;String codeFinal=code;
            var existing=state.materials.stream().filter(r->codeFinal.equals(r.get("code"))).findFirst();var material=existing.isPresent()?existing.get():state.add("materials",JsonStore.map("code",codeFinal,"name",name,"unit",List.of("AD","MT","M2","KG").contains(unit)?unit:"AD","active",true));
            String date=null;try{date=LocalDate.of(1899,12,30).plusDays((long)Double.parseDouble(sheet.value("B"+row))).toString();}catch(Exception ignored){}
            state.add("prices",JsonStore.map("materialId",JsonStore.num(material.get("id")),"supplierId",JsonStore.num(supplier.get("id")),"amount",amount,"currency",currency,"validFrom",date,"selected",false,"sourcePath",p.path(),"sourceCell","O"+row));
        }
    }
}
