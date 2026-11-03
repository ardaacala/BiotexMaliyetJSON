package biotex;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/** Read-only OOXML reader. Formula text and cached result are deliberately separate. */
public final class XlsxReader {
    public record Cell(String value, String formula, String type) {
        public boolean isFormula() { return formula != null && !formula.isBlank(); }
    }
    public record Sheet(String name, Map<String, Cell> cells) {
        public Cell cell(String address) { return cells.getOrDefault(address, new Cell("", null, "")); }
        public String value(String address) { return cell(address).value(); }
    }
    public record Book(Map<String, Sheet> sheets, List<String> externalLinks) {
        public Sheet sheet(String name) { return sheets.get(name); }
    }

    private static Document xml(byte[] bytes) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }
    private static List<Element> elements(Node parent, String local) {
        List<Element> out = new ArrayList<>();
        NodeList nodes = ((Element)parent).getElementsByTagNameNS("*", local);
        for (int i=0; i<nodes.getLength(); i++) out.add((Element)nodes.item(i));
        return out;
    }
    private static String firstText(Element e, String local) {
        var matches = elements(e, local);
        return matches.isEmpty() ? "" : matches.get(0).getTextContent();
    }
    private static String textOf(Element e, String local) {
        StringBuilder b = new StringBuilder();
        for (Element x : elements(e, local)) b.append(x.getTextContent());
        return b.toString();
    }
    public static Book read(byte[] bytes) throws Exception {
        if (bytes.length > 100_000_000) throw new IOException("Çalışma kitabı çok büyük");
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry=z.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = entry.getName().replace('\\','/');
                if (name.startsWith("/") || name.contains("../")) throw new IOException("Güvensiz ZIP yolu");
                if (entry.getSize() > 30_000_000 || files.size()>1500) throw new IOException("ZIP sınırı aşıldı");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                z.transferTo(out);
                if (out.size()>30_000_000) throw new IOException("ZIP girdisi çok büyük");
                files.put(name, out.toByteArray());
            }
        }
        byte[] wb = files.get("xl/workbook.xml");
        if (wb == null) throw new IOException("Geçerli XLSX bulunamadı");
        List<String> strings = new ArrayList<>();
        if (files.containsKey("xl/sharedStrings.xml")) {
            Document shared = xml(files.get("xl/sharedStrings.xml"));
            for (Element si : elements(shared.getDocumentElement(), "si")) strings.add(textOf(si,"t"));
        }
        Map<String,String> rels = new HashMap<>();
        byte[] relBytes = files.get("xl/_rels/workbook.xml.rels");
        if (relBytes != null) for (Element e : elements(xml(relBytes).getDocumentElement(),"Relationship")) {
            String target = e.getAttribute("Target").replace('\\','/');
            if (!target.startsWith("xl/")) target = "xl/" + target.replaceFirst("^/", "");
            rels.put(e.getAttribute("Id"), target);
        }
        Map<String,Sheet> sheets = new LinkedHashMap<>();
        Document workbook = xml(wb);
        for (Element se : elements(workbook.getDocumentElement(),"sheet")) {
            String rid = se.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships","id");
            byte[] data = files.get(rels.get(rid));
            if (data == null) continue;
            Map<String,Cell> cells = new LinkedHashMap<>();
            for (Element c : elements(xml(data).getDocumentElement(),"c")) {
                String type = c.getAttribute("t");
                String val = firstText(c,"v");
                if ("s".equals(type)) {
                    try { val = strings.get(Integer.parseInt(val)); } catch (Exception ignored) { val=""; }
                } else if ("inlineStr".equals(type)) val = textOf(c,"t");
                String formula = firstText(c,"f");
                cells.put(c.getAttribute("r"), new Cell(val, formula.isEmpty()?null:formula, type));
            }
            sheets.put(se.getAttribute("name"), new Sheet(se.getAttribute("name"),cells));
        }
        List<String> links = new ArrayList<>();
        for (var e : files.keySet()) if (e.startsWith("xl/externalLinks/")) links.add(e);
        return new Book(sheets,links);
    }
}
