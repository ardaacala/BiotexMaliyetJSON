import biotex.*;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.nio.file.*;
import static biotex.JsonStore.*;

public class VisualSmoke {
    public static void main(String[] args)throws Exception{
        FlatLightLaf.setup();Path data=Files.createTempDirectory("biotex-visual-").resolve("visual.json");JsonService service=new JsonService(new JsonStore(data));long p=service.addProduct("BIO-001","Cerrahi Örtü Seti","Set","150×180",true);long m=service.addMaterial("KMS-001","Mavi nonwoven kumaş","M2",null);service.addPrice(m,"Örnek tedarikçi","0.146","USD","2026-11-03",true);service.addRate("2026-11-03","USD","3.8");service.addLine(map("productId",p,"materialId",m,"description","Mavi nonwoven kumaş","unit","M2","method","AREA","widthCm","150","heightCm","180","repeats","5","wasteRate","0","costFactor","1.015"));
        final App[] app=new App[1];SwingUtilities.invokeAndWait(()->{try{app[0]=new App(Path.of("."),data);app[0].setSize(Integer.parseInt(args[1]),800);app[0].setVisible(true);var method=App.class.getDeclaredMethod("showPage",String.class);method.setAccessible(true);method.invoke(app[0],args[0]);}catch(Exception e){throw new RuntimeException(e);}});Thread.sleep(1200);BufferedImage image=new BufferedImage(app[0].getWidth(),app[0].getHeight(),BufferedImage.TYPE_INT_RGB);SwingUtilities.invokeAndWait(()->app[0].paint(image.getGraphics()));Files.createDirectories(Path.of("art"));ImageIO.write(image,"png",Path.of("art/"+args[2]+".png").toFile());SwingUtilities.invokeAndWait(()->app[0].dispose());System.exit(0);
    }
}
