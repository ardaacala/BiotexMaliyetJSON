package biotex;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.image.*;

public final class Reports {
    private static String xml(String s){return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    private static void entry(ZipOutputStream z,String name,String value)throws Exception{z.putNextEntry(new ZipEntry(name));z.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));z.closeEntry();}
    /** Minimal standards-compliant XLSX, static values only. */
    public static void xlsx(Path output,List<String[]> rows)throws Exception{
        Files.createDirectories(output.toAbsolutePath().getParent());
        try(ZipOutputStream z=new ZipOutputStream(Files.newOutputStream(output))){
            entry(z,"[Content_Types].xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/></Types>");
            entry(z,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(z,"xl/workbook.xml","<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Rapor\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(z,"xl/_rels/workbook.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            StringBuilder sheet=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
            for(int i=0;i<rows.size();i++){sheet.append("<row r=\"").append(i+1).append("\">");String[] row=rows.get(i);for(int j=0;j<row.length;j++){String address=""+(char)('A'+j)+(i+1);sheet.append("<c r=\"").append(address).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(row[j])).append("</t></is></c>");}sheet.append("</row>");}sheet.append("</sheetData></worksheet>");
            entry(z,"xl/worksheets/sheet1.xml",sheet.toString());
        }
    }
    public static void pdf(Path output,String title,List<String> lines,Path logo)throws Exception{
        Files.createDirectories(output.toAbsolutePath().getParent());
        try(PDDocument doc=new PDDocument()){
            Path fontPath=Path.of("C:/Windows/Fonts/arial.ttf");
            PDFont font=PDType0Font.load(doc,fontPath.toFile());
            PDImageXObject image=Files.exists(logo)?PDImageXObject.createFromFile(logo.toString(),doc):null;
            PDPage page=null;PDPageContentStream stream=null;float y=0;
            for(int i=-1;i<lines.size();i++){
                if(page==null||y<55){if(stream!=null)stream.close();page=new PDPage(PDRectangle.A4);doc.addPage(page);stream=new PDPageContentStream(doc,page);y=790;
                    if(image!=null){float w=120,h=w*image.getHeight()/image.getWidth();stream.drawImage(image,45,y-h,w,h);y-=h+18;}
                    stream.beginText();stream.setFont(font,16);stream.newLineAtOffset(45,y);stream.showText(title);stream.endText();y-=30;
                }
                if(i<0)continue;
                String line=lines.get(i);if(line.length()>110)line=line.substring(0,107)+"...";
                stream.beginText();stream.setFont(font,10);stream.newLineAtOffset(45,y);stream.showText(line.replace('\n',' '));stream.endText();y-=18;
            }
            if(stream!=null)stream.close();doc.save(output.toFile());
        }
    }
}
