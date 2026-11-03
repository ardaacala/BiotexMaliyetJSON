package biotex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/** Guided, read-only tour. A click on the highlighted control advances without running its action. */
public final class Tutorial {
    public record Step(String page,String tab,String target,String title,String explanation) {}
    private static Step step(String page,String target,String title,String explanation){return new Step(page,null,target,title,explanation);}
    private static Step tab(String page,String tab,String target,String title,String explanation){return new Step(page,tab,target,title,explanation);}

    public static final List<String> PAGES=List.of("Genel Bakış","Ürünler","Malzemeler","Tedarikçi Fiyatları","Maliyet Hesapla","Teklifler","Raporlar","Excel Aktarımı","Ayarlar");
    public static List<Step> forPage(String page){return switch(page){
        case "Genel Bakış"->List.of(
            step(page,"content","Başlangıç ekranı","Üstte ürün, malzeme, fiyat ve teklif sayıları vardır. Alt tarafta son ürünleri görürsünüz. Sol menüden istediğiniz bölüme geçebilirsiniz."),
            step(page,"action:Yeni ürün","İlk ürününüz","Yeni ürün düğmesi kod, ad, kategori, beden ve steril bilgisini ister. Ürün kaydedilince set, reçete ve operasyon sekmeleri açılır."),
            step(page,"action:Excel aktarımı","Mevcut dosyaları kullanın","Elinizde Excel reçeteleri varsa buradan içe aktarma ekranına geçin. Önce önizleme yapın; veri ancak aktarım onaylandığında kaydedilir."));
        case "Ürünler"->List.of(
            step(page,"action:Yeni ürün","Ürün ekleme","Yeni ürün ile benzersiz bir ürün kodu ve ürün adı girin. İsterseniz kategori, beden ve steril durumunu da belirleyin."),
            step(page,"field:search","Ürün arama","Arama kutusuna kod veya ad yazın. Tablo anında süzülür; kutuyu temizleyince bütün ürünler görünür."),
            step(page,"action:Detay","Ürünü düzenleme","Tablodan bir ürün seçip Detay'a basın veya satıra çift tıklayın. İçeride Genel bilgiler, Set içeriği, Reçete, Operasyonlar ve Maliyet sekmeleri vardır."),
            step(page,"action:Kopyala","Benzer ürün oluşturma","Bir satır seçip Kopyala'ya basın. Yeni kod girerek ürünü, setini, reçetesini ve operasyonlarını ayrı kayıtlar olarak çoğaltırsınız."));
        case "Malzemeler"->List.of(
            step(page,"action:Yeni malzeme","Malzeme kartı","Stok kodu, ad ve birim girin. Kumaş gibi malzemelerde gramajı da ekleyin. Reçetede malzeme ID'si ile bu karta bağlanılır."),
            step(page,"table","Malzeme listesi","Burada her malzemenin ID'si, kodu, birimi ve gramajı görünür. Fiyat tanımlarken listedeki ID'yi kullanın."));
        case "Tedarikçi Fiyatları"->List.of(
            step(page,"action:Fiyat ekle","Fiyat alternatifi","Malzeme ID'si, tedarikçi, birim fiyat, para birimi ve geçerlilik tarihi girin. Aynı malzemeye birden fazla alternatif eklenebilir."),
            step(page,"action:Seçili fiyat yap","Hesapta kullanılacak fiyat","Listeden bir fiyat satırı seçip bu düğmeye basın. Maliyet hesabı yalnız seçili fiyatı kullanır. Dövizli fiyat için Ayarlar'da kur da gerekir."),
            step(page,"table","Fiyatları kontrol edin","Seçili sütunu ve tarihi gözden geçirin. Kaynak Excel'den gelen fiyatlar sizin seçiminiz olmadan hesapta kullanılmaz."));
        case "Maliyet Hesapla"->List.of(
            step(page,"field:product","Ürün seçin","Önce hesabını yapmak istediğiniz ürünü seçin. Ürünün reçetesi, operasyonları ve maliyet profili hesaplamaya katılır."),
            step(page,"field:quantity","Sipariş adedi","Adet değişince toplam sipariş tutarı ve sipariş kapsamlı kurallar buna göre hesaplanır. Sıfırdan büyük bir sayı girin."),
            step(page,"field:date","Kur tarihi","Dövizli fiyatlar için bu tarihte veya daha önce girilmiş son kur kullanılır. Eksik kur onayı engeller."),
            step(page,"action:Hesapla","Canlı hesap","Girdileri değiştirdikten sonra Hesapla'ya basın. Ortadaki satır dökümünü ve sağdaki maliyet özetini inceleyin."),
            step(page,"action:Taslak kaydet","Taslak ve onay","Eksik fiyat veya kur varsa taslak kaydedebilirsiniz. Onayla yalnız bütün engeller çözüldüğünde çalışır. Müşteri ID'si isterseniz Teklifler'de eklenebilir."));
        case "Teklifler"->List.of(
            step(page,"action:Müşteri ekle","Müşteri kaydı","Ad, adres ve vergi numarasıyla müşteri oluşturun. Verilen ID'yi Maliyet Hesapla ekranında teklife bağlayabilirsiniz."),
            step(page,"table","Teklif arşivi","Taslak ve onaylı hesaplar burada listelenir. Onaylı teklifin anlık görüntüsü saklanır; sonraki fiyat değişiklikleri bu kaydı değiştirmez."),
            step(page,"action:Anlık görüntü","Hesabın geçmişi","Bir teklif seçip Anlık görüntü'ye basın. Kaydedildiği andaki ürün, reçete, fiyat, kur ve hesap sonucunu görürsünüz."));
        case "Raporlar"->List.of(
            step(page,"table","Önce kayıt seçin","Teklif PDF, Maliyet PDF veya Maliyet XLSX için tablodan bir teklif seçin. Müşteriye verilen PDF yalnız onaylı tekliften çıkarılır."),
            step(page,"action:Teklif PDF","Müşteriye teklif","Bu PDF müşteriye uygun fiyat ve toplamı içerir; iç maliyet dökümünü göstermez. Açılan pencereden kaydetme yerini seçersiniz."),
            step(page,"action:Maliyet PDF","İç raporlar","Maliyet PDF ve Maliyet XLSX iç kullanım içindir. İhtiyaç XLSX ürün ve sipariş adedine göre malzeme gereksinimini verir."),
            step(page,"action:Karşılaştırma XLSX","Ürünleri karşılaştırın","Sipariş adedi ve kur tarihi vererek ürünlerin maliyet ve satış fiyatlarını tek Excel dosyasında karşılaştırın."));
        case "Excel Aktarımı"->List.of(
            step(page,"action:Dosya seç","Dosya önizleme","ZIP veya tek XLSX seçin. Okuma işlemi kayıtları değiştirmez; çalışma kitapları ve şablonları tabloda listelenir."),
            step(page,"table","Dosyaları inceleyin","İlk sütundaki işaretleri değiştirerek aktarılacak dosyaları seçin. Bir satıra tıklayınca alt bölümde uyarıları ve kaynak hücrelerini görürsünüz."),
            step(page,"action:Seçilenleri aktar","Aktarımı tamamlayın","İnceleme bittikten sonra Seçilenleri aktar'a basın. Aynı içerik tekrar eklenmez; sorunlar JSON verisinde saklanır."));
        case "Ayarlar"->List.of(
            tab(page,"Kurlar","action:Kur ekle","Döviz kuru","Kur tarihi, USD veya EUR ve 1 birimin TRY karşılığını girin. Dövizli malzeme fiyatları için gereklidir."),
            tab(page,"Profiller","action:Yeni profil","Maliyet profili","Satır katsayısı, ek gider, kâr yöntemi ve işçilik süresi kuralını bir profilde toplayın. Ürün kartında profil ID'sini seçebilirsiniz."),
            tab(page,"Kurallar","action:Kural ekle","Ek maliyet kuralları","Sabit veya oranlı giderleri ürün ya da sipariş kapsamında tanımlayın. Önce ilgili profil ID'sini belirleyin."),
            tab(page,"Veri ve yedek","action:Yedek al","JSON yedeği","Bütün kayıtlar yerel JSON dosyasındadır. Yedek al ile güvenli bir kopya oluşturun; geri yükleme mevcut verinin yerine geçer."));
        case "Ürün Detayı"->List.of(
            tab(page,"Genel bilgiler","action:Düzenle","Ürün kartı","Kod, ad, kategori, beden, steril durum ve maliyet profili bu sekmede düzenlenir. Profil ID'si Ayarlar bölümünden bulunur."),
            tab(page,"Set içeriği","action:Bileşen ekle","Set bileşenleri","Setteki her parçayı adı ve adediyle ekleyin. Alt ürün bağla ile başka bir ürünün ID'sini bileşene bağlayabilirsiniz."),
            tab(page,"Reçete","action:Satır ekle","Malzeme reçetesi","Her tüketim satırında malzeme ID'si, birim, yöntem, miktar veya ölçüler, fire ve gerekirse manuel fiyatı girin. Satır seçip Düzenle ile düzeltin."),
            tab(page,"Operasyonlar","action:Operasyon ekle","İşçilik işlemleri","Operasyon adı, süre, kişi sayısı, tekrar, dakika maliyeti ve kapsamı girin. Siparişlik işlemler için BATCH kapsamını seçin."),
            tab(page,"Maliyet","action:Maliyet Hesapla","Hesaba geçin","Bu düğme seçili ürünü Maliyet Hesapla ekranına taşır. Orada adet ve kur tarihini girerek sonucu görürsünüz."));
        default->List.of();
    };}
    public static List<Step> fullTour(boolean hasProduct){List<Step> all=new ArrayList<>();for(String page:PAGES){all.addAll(forPage(page));if(hasProduct&&page.equals("Ürünler"))all.addAll(forPage("Ürün Detayı"));}return all;}

    private final JFrame frame;private final JPanel body;private final Consumer<String> navigate;private final Runnable completed;
    private final Overlay overlay=new Overlay();private List<Step> steps=List.of();private int index;private boolean full;
    public Tutorial(JFrame frame,JPanel body,Consumer<String> navigate,Runnable completed){this.frame=frame;this.body=body;this.navigate=navigate;this.completed=completed;}
    public void start(List<Step> requested,boolean full){if(requested.isEmpty())return;stop(false);this.steps=requested;this.index=0;this.full=full;frame.getRootPane().setGlassPane(overlay);overlay.setVisible(true);overlay.timer.start();showStep();}
    public void stop(boolean finished){overlay.timer.stop();overlay.setVisible(false);if(full&&finished)completed.run();steps=List.of();}
    private void move(int delta){int next=index+delta;if(next>=steps.size()){stop(true);return;}if(next<0)return;index=next;showStep();}
    private void showStep(){Step step=steps.get(index);if(!Objects.equals(frame.getRootPane().getClientProperty("tutorial.page"),step.page()))navigate.accept(step.page());frame.getRootPane().putClientProperty("tutorial.page",step.page());if(step.tab()!=null){JTabbedPane tabs=findTabs(body);if(tabs!=null)for(int i=0;i<tabs.getTabCount();i++)if(step.tab().equals(tabs.getTitleAt(i))){tabs.setSelectedIndex(i);break;}}SwingUtilities.invokeLater(overlay::refresh);}
    private static JTabbedPane findTabs(Container parent){for(Component c:parent.getComponents()){if(c instanceof JTabbedPane tabs)return tabs;if(c instanceof Container nested){JTabbedPane found=findTabs(nested);if(found!=null)return found;}}return null;}
    private static Component findNamed(Container parent,String name){for(Component c:parent.getComponents()){if(name.equals(c.getName())&&c.isShowing())return c;if(c instanceof Container nested){Component found=findNamed(nested,name);if(found!=null)return found;}}return null;}

    private final class Overlay extends JComponent {
        private void move(int delta){Tutorial.this.move(delta);}
        private final JPanel card=new JPanel();private final JLabel progress=Ui.label("",11,true),heading=Ui.label("",17,true),description=Ui.label("",13,false),hint=Ui.label("",11,false);
        private final JButton back=Ui.button("Geri",false,()->Tutorial.this.move(-1)),next=Ui.button("İleri",true,()->Tutorial.this.move(1)),close=Ui.button("Kapat",false,()->stop(true));
        private boolean pulse,matched;private Rectangle target=new Rectangle();private final javax.swing.Timer timer=new javax.swing.Timer(420,e->{pulse=!pulse;repaint();});
        Overlay(){setOpaque(false);setLayout(null);card.setBackground(Color.WHITE);card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Ui.BLUE,2,true),BorderFactory.createEmptyBorder(16,18,16,18)));card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));for(JLabel label:List.of(progress,heading,description,hint))label.setAlignmentX(LEFT_ALIGNMENT);card.add(progress);card.add(Box.createVerticalStrut(8));card.add(heading);card.add(Box.createVerticalStrut(9));card.add(description);card.add(Box.createVerticalStrut(10));hint.setText("İşaretli alana tıklayın veya İleri'yi seçin. Eğitim işlem yapmaz.");card.add(hint);card.add(Box.createVerticalStrut(14));JPanel buttons=Ui.row(back,next,close);buttons.setAlignmentX(LEFT_ALIGNMENT);card.add(buttons);add(card);addMouseListener(new MouseAdapter(){public void mouseClicked(MouseEvent e){if(target.contains(e.getPoint()))move(1);}});addComponentListener(new ComponentAdapter(){public void componentResized(ComponentEvent e){refresh();}});getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"close");getActionMap().put("close",new AbstractAction(){public void actionPerformed(ActionEvent e){stop(true);}});getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,0),"next");getActionMap().put("next",new AbstractAction(){public void actionPerformed(ActionEvent e){move(1);}});}
        private Rectangle findTarget(Step step){if(step.target().startsWith("tab:")){JTabbedPane tabs=findTabs(body);if(tabs!=null)for(int i=0;i<tabs.getTabCount();i++)if(step.target().substring(4).equals(tabs.getTitleAt(i))){matched=true;return SwingUtilities.convertRectangle(tabs,tabs.getBoundsAt(i),this);}}if(step.target().equals("content")){matched=true;return SwingUtilities.convertRectangle(body,new Rectangle(0,0,body.getWidth(),body.getHeight()),this);}Component c;if(step.target().equals("table"))c=findFirst(body,JScrollPane.class);else if(step.target().equals("field:search"))c=findFirst(body,JTextField.class);else c=findNamed(body,step.target());matched=c!=null;if(c==null)c=body;Rectangle r=SwingUtilities.convertRectangle(c,new Rectangle(0,0,c.getWidth(),c.getHeight()),this);Rectangle visible=new Rectangle(5,5,Math.max(1,getWidth()-10),Math.max(1,getHeight()-10));return r.intersection(visible);}
        private Component findFirst(Container parent,Class<?> type){for(Component c:parent.getComponents()){if(type.isInstance(c)&&c.isShowing())return c;if(c instanceof Container nested){Component found=findFirst(nested,type);if(found!=null)return found;}}return null;}
        void refresh(){if(steps.isEmpty()||getWidth()==0)return;Step step=steps.get(index);progress.setText((index+1)+" / "+steps.size()+"  ·  "+step.page()+(step.tab()==null?"":" / "+step.tab()));heading.setText(step.title());description.setText("<html><div style='width:340px'>"+step.explanation()+"</div></html>");back.setEnabled(index>0);next.setText(index==steps.size()-1?"Bitir":"İleri");target=findTarget(step);int cardWidth=Math.min(430,Math.max(330,getWidth()-32));int cardHeight=card.getPreferredSize().height;int x=target.x+target.width+18;if(x+cardWidth>getWidth()-12)x=target.x-cardWidth-18;if(x<12)x=Math.max(12,(getWidth()-cardWidth)/2);int y=Math.max(12,Math.min(target.y,getHeight()-cardHeight-12));card.setBounds(x,y,cardWidth,cardHeight);card.revalidate();repaint();}
        protected void paintComponent(Graphics raw){if(steps.isEmpty())return;Graphics2D g=(Graphics2D)raw.create();Area shade=new Area(new Rectangle(0,0,getWidth(),getHeight()));shade.subtract(new Area(new RoundRectangle2D.Double(target.x-6,target.y-6,target.width+12,target.height+12,16,16)));g.setColor(new Color(14,31,50,145));g.fill(shade);g.setStroke(new BasicStroke(pulse?5f:3f));g.setColor(pulse?new Color(183,200,0):new Color(31,145,229));g.drawRoundRect(target.x-4,target.y-4,target.width+8,target.height+8,14,14);g.dispose();}
    }
}
