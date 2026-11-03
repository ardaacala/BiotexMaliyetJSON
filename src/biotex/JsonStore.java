package biotex;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/** Single-user JSON database. Every mutation writes a complete, atomic new snapshot. */
public final class JsonStore {
    public static final int SCHEMA_VERSION=1;
    public static final class State {
        public int schemaVersion=SCHEMA_VERSION;
        public long nextId=1;
        public boolean tutorialCompleted=false;
        public List<Map<String,Object>> products=new ArrayList<>(), components=new ArrayList<>(), recipeLines=new ArrayList<>(), materials=new ArrayList<>(), suppliers=new ArrayList<>(), prices=new ArrayList<>(), rates=new ArrayList<>(), operations=new ArrayList<>(), profiles=new ArrayList<>(), rules=new ArrayList<>(), customers=new ArrayList<>(), estimates=new ArrayList<>(), imports=new ArrayList<>(), issues=new ArrayList<>(), changes=new ArrayList<>();
        public List<Map<String,Object>> table(String name){return switch(name){case "products"->products;case "components"->components;case "recipeLines"->recipeLines;case "materials"->materials;case "suppliers"->suppliers;case "prices"->prices;case "rates"->rates;case "operations"->operations;case "profiles"->profiles;case "rules"->rules;case "customers"->customers;case "estimates"->estimates;case "imports"->imports;case "issues"->issues;case "changes"->changes;default->throw new IllegalArgumentException("Unknown table: "+name);};}
        public Map<String,Object> add(String table,Map<String,Object> fields){Map<String,Object> row=new LinkedHashMap<>(fields);row.put("id",nextId++);table(table).add(row);return row;}
        public Map<String,Object> byId(String table,long id){return table(table).stream().filter(r->num(r.get("id"))==id).findFirst().orElseThrow(()->new IllegalArgumentException("Kayıt bulunamadı: "+table+" "+id));}
        public void log(String table,long id,String action,String note){add("changes",map("at",Instant.now().toString(),"entity",table,"entityId",id,"action",action,"note",note));}
    }
    private final Path file;private final Gson gson=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();private State state;
    public JsonStore(Path file)throws Exception{this.file=file.toAbsolutePath();Files.createDirectories(this.file.getParent());if(Files.exists(this.file)){state=parse(Files.readString(this.file,StandardCharsets.UTF_8));}else{state=new State();state.add("profiles",map("name","Yeni reçete","lineFactor","1","extraRate","0","markupRate","0","markupMethod","MARKUP","laborMethod","RAW"));write(state);}}
    public Path file(){return file;}
    private State parse(String text){State s=gson.fromJson(text,State.class);if(s==null||s.schemaVersion!=SCHEMA_VERSION)throw new IllegalStateException("Desteklenmeyen JSON şema sürümü");if(s.products==null||s.profiles==null||s.imports==null)throw new IllegalStateException("Eksik JSON tabloları");return s;}
    public synchronized <T>T read(Function<State,T> work){return work.apply(gson.fromJson(gson.toJson(state),State.class));}
    public synchronized <T>T update(Function<State,T> work)throws Exception{State next=gson.fromJson(gson.toJson(state),State.class);T result=work.apply(next);write(next);state=next;return result;}
    private void write(State data)throws IOException{Path tmp=file.resolveSibling(file.getFileName()+".tmp");try{Files.writeString(tmp,gson.toJson(data),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(tmp);}}
    public synchronized void backup(Path target)throws Exception{Files.createDirectories(target.toAbsolutePath().getParent());Files.copy(file,target,StandardCopyOption.REPLACE_EXISTING);}
    public synchronized void restore(Path source)throws Exception{State restored=parse(Files.readString(source,StandardCharsets.UTF_8));write(restored);state=restored;}
    public static long num(Object v){if(v==null)return 0;if(v instanceof Number n)return n.longValue();return Long.parseLong(v.toString());}
    public static String str(Object v){return v==null?null:v.toString();}
    public static Map<String,Object> map(Object... parts){if(parts.length%2!=0)throw new IllegalArgumentException();Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<parts.length;i+=2)out.put(parts[i].toString(),parts[i+1]);return out;}
    public static List<Map<String,Object>> copy(List<Map<String,Object>> rows){List<Map<String,Object>> out=new ArrayList<>();for(var row:rows)out.add(new LinkedHashMap<>(row));return out;}
}
