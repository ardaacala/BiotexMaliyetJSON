import biotex.*;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import static biotex.JsonStore.*;

/** Walk every tour step in a real Swing frame and verify that its highlight is visible. */
public class TutorialUiTests {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        FlatLightLaf.setup();Path data=Files.createTempDirectory("biotex-guide-").resolve("data.json");JsonStore store=new JsonStore(data);store.update(s->{s.tutorialCompleted=true;return null;});JsonService service=new JsonService(store);
        long product=service.addProduct("GUIDE-1","Örnek Set","Set","",false);
        service.addLine(map("productId",product,"description","Örnek kumaş","unit","AD","method","MANUAL","manualQty","1","manualPrice","10","manualCurrency","TRY","repeats","1","wasteRate","0"));
        App[] app=new App[1];SwingUtilities.invokeAndWait(()->{try{app[0]=new App(Path.of("."),data);app[0].setSize(Integer.getInteger("biotex.test.width",1000),720);app[0].setVisible(true);}catch(Exception e){throw new RuntimeException(e);}});
        Field tutorialField=App.class.getDeclaredField("tutorial");tutorialField.setAccessible(true);Tutorial guide=(Tutorial)tutorialField.get(app[0]);
        Method start=App.class.getDeclaredMethod("startGuide",boolean.class);start.setAccessible(true);Method move=Tutorial.class.getDeclaredMethod("move",int.class);move.setAccessible(true);
        Field overlayField=Tutorial.class.getDeclaredField("overlay");overlayField.setAccessible(true);Object overlay=overlayField.get(guide);Field targetField=overlay.getClass().getDeclaredField("target");targetField.setAccessible(true);Field matchedField=overlay.getClass().getDeclaredField("matched");matchedField.setAccessible(true);
        java.util.List<Tutorial.Step> steps=Tutorial.fullTour(true);check(steps.size()==36,"Expected 36 steps, got "+steps.size());
        SwingUtilities.invokeAndWait(()->{try{start.invoke(app[0],true);}catch(Exception e){throw new RuntimeException(e);}});
        for(int i=0;i<steps.size();i++){
            if(i>0)SwingUtilities.invokeAndWait(()->{try{move.invoke(guide,1);}catch(Exception e){throw new RuntimeException(e);}});
            SwingUtilities.invokeAndWait(()->{});
            Rectangle target=(Rectangle)targetField.get(overlay);
            check(matchedField.getBoolean(overlay),"Missing actual UI target at step "+(i+1)+": "+steps.get(i));
            check(target.width>10&&target.height>10,"Invisible target at step "+(i+1)+": "+steps.get(i));
            check(target.intersects(new Rectangle(0,0,app[0].getWidth(),app[0].getHeight())),"Off-screen target at step "+(i+1));
            if(i==17||i==34){int index=i;SwingUtilities.invokeAndWait(()->{try{BufferedImage image=new BufferedImage(app[0].getWidth(),app[0].getHeight(),BufferedImage.TYPE_INT_RGB);app[0].paint(image.getGraphics());Files.createDirectories(Path.of("art"));ImageIO.write(image,"png",Path.of("art/tutorial-"+(index==17?"cost":"settings")+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}});}
        }
        SwingUtilities.invokeAndWait(()->{try{move.invoke(guide,1);start.invoke(app[0],false);}catch(Exception e){throw new RuntimeException(e);}});
        SwingUtilities.invokeAndWait(()->{});
        check(((java.util.List<?>)Tutorial.class.getDeclaredMethod("forPage",String.class).invoke(null,"Ayarlar")).size()==4,"Settings guide steps missing");
        check(((JComponent)overlay).isVisible(),"Section guide did not reopen");
        SwingUtilities.invokeAndWait(()->{guide.stop(true);app[0].dispose();});
        check(new JsonStore(data).read(s->s.tutorialCompleted),"Tour completion was not saved to JSON");
        check(new JsonService(new JsonStore(data)).rows("products").size()==1,"The tutorial changed product data");
        System.out.println("PASS 36 guided UI steps; highlights visible; JSON completion saved; product data unchanged");
    }
}
