import biotex.*;
import java.math.*;
import java.nio.file.*;
import java.util.*;
import static biotex.JsonStore.*;

public class JsonAcceptanceTests {
    private static int checks=0;
    private static void yes(String name,boolean value){checks++;if(!value)throw new AssertionError(name);}
    private static void eq(String name,BigDecimal value,String expected){checks++;if(value==null||value.compareTo(new BigDecimal(expected))!=0)throw new AssertionError(name+": "+value);}
    public static void main(String[] args)throws Exception{
        Path temp=Files.createTempDirectory("biotex-json-test-"),file=temp.resolve("data.json");JsonStore store=new JsonStore(file);JsonService service=new JsonService(store);
        yes("JSON file created",Files.isRegularFile(file)&&Files.readString(file).contains("schemaVersion"));
        long product=service.addProduct("TEST-1","Deneme Ürünü","Set","",true);long material=service.addMaterial("MAT-1","Kumaş","M2",null);
        service.addPrice(material,"Tedarikçi","0.146","USD","2026-11-03",true);service.addRate("2026-11-03","USD","3.8");
        service.addLine(map("productId",product,"description","Kumaş","materialId",material,"unit","M2","method","AREA","widthCm","150","heightCm","180","repeats","5","wasteRate","0","costFactor","1.015"));
        eq("decimal area",service.calculate(product,"1","2026-11-03").unitCost(),"7.602147");
        long approved=service.saveEstimate(product,"1","2026-11-03",0,true);service.addPrice(material,"Tedarikçi","1","TRY","2026-11-04",true);
        yes("live price updated",service.calculate(product,"1","2026-11-04").unitCost().compareTo(new BigDecimal("7.602147"))!=0);
        eq("approved snapshot immutable",new BigDecimal(str(service.one("estimates",approved).get("unitCost"))),"7.602147");
        yes("persistent across reopen",new JsonService(new JsonStore(file)).rows("products").size()==1);
        Path backup=temp.resolve("backup.json");store.backup(backup);service.addProduct("LATER","Later","","",false);store.restore(backup);yes("backup restore",service.rows("products").size()==1);
        long child=service.addProduct("CHILD","Alt","","",false);long comp=service.addComponent(product,"Bileşen","1");service.linkSubproduct(comp,child,false);long reverse=service.addComponent(child,"Ters","1");boolean rejected=false;try{service.linkSubproduct(reverse,product,false);}catch(IllegalArgumentException e){rejected=true;}yes("cycle rejected",rejected);
        long duplicate=service.copyProduct(product,"TEST-COPY");yes("independent recipe IDs",service.lines(duplicate).size()==1&&num(service.lines(duplicate).get(0).get("id"))!=num(service.lines(product).get(0).get("id")));
        long copiedLine=num(service.lines(duplicate).get(0).get("id"));service.updateLine(copiedLine,map("manualPrice","2","manualCurrency","TRY"));yes("copy edit isolated",service.lines(product).get(0).get("manualPrice")==null);
        var demand=service.demand(product,"2");eq("demand",demand.get(0).quantity(),"27");
        var source=Path.of(args[0]);byte[] original=Files.readAllBytes(source);var preview=Importer.preview(source);yes("67 source books",preview.size()==67);yes("template profiles",preview.stream().anyMatch(p->p.template().equals("SET"))&&preview.stream().anyMatch(p->p.template().equals("ÖNLÜK")));
        int first=Importer.commit(store,preview),second=Importer.commit(store,preview);yes("idempotent import",first==35&&second==0);yes("source unchanged",Arrays.equals(original,Files.readAllBytes(source)));
        yes("issue log",service.rows("issues").size()>500);yes("JSON no SQLite",!Files.readString(file).startsWith("SQLite format"));
        yes("import persisted in JSON",new JsonService(new JsonStore(file)).rows("imports").size()==36);
        Path xlsx=temp.resolve("report.xlsx"),pdf=temp.resolve("report.pdf");Reports.xlsx(xlsx,List.of(new String[]{"Ürün","Tutar"},new String[]{"Türkçe","1,5"}));Reports.pdf(pdf,"Biotex Teklif",List.of("Türkçe rapor","Birim fiyat: 1,5 TRY"),Path.of(args[1]));yes("reports",Files.size(pdf)>1000&&XlsxReader.read(Files.readAllBytes(xlsx)).sheet("Rapor").value("A2").equals("Türkçe"));
        System.out.println("PASS "+checks+" JSON acceptance assertions; imported="+first+"; issues="+service.rows("issues").size());
    }
}
