# Biotex Maliyet Yönetimi · JSON sürümü

Bu proje `AuraCosmetics` dışında bağımsızdır. Windows'ta çevrimdışı çalışır; Microsoft Excel kurulumu ve SQLite veritabanı gerekmez. Uygulama verilerini `%LOCALAPPDATA%\BiotexMaliyetJSON\biotex-data.json` dosyasında saklar. Her değişiklik önce geçici JSON dosyasına yazılır, ardından ana dosya değiştirilir.

## Çalıştırma

1. `dist/BiotexMaliyetJSON-windows.zip` arşivini bir klasöre çıkarın.
2. `BiotexMaliyetJSON.exe` dosyasını açın. Paketteki `app` ve `runtime` klasörleri aynı yerde kalsın.
3. Ürün oluşturun veya **Excel Aktarımı** ekranında paketin `app/sources/maliyet-hesaplamalari.zip` dosyasını önizleyip aktarın.

## Ekranlar

- **Genel Bakış:** Ürün, malzeme, fiyat ve teklif sayıları.
- **Ürünler:** Ürün ekleme, kopyalama, düzenleme; set bileşeni, reçete ve operasyon ayrıntıları.
- **Malzemeler / Tedarikçi Fiyatları:** Malzeme kartları ve seçili fiyat alternatifi. Kaynak Excel fiyatları otomatik seçilmez.
- **Maliyet Hesapla:** Ürün, adet ve kur tarihiyle canlı hesap; satır ayrıntısı, onay engelleri, taslak ve onaylı teklif.
- **Teklifler:** Kayıtlı teklif ve anlık görüntü. Onaylı teklif sonraki fiyat değişikliklerinden etkilenmez.
- **Raporlar:** Müşteriye verilen teklif PDF'si, iç maliyet PDF/XLSX, malzeme ihtiyaç ve ürün karşılaştırma XLSX dosyaları.
- **Excel Aktarımı:** ZIP/XLSX önizleme, şablon tanıma, sorun listesi ve tekrarsız kayıt.
- **Ayarlar:** Tarihli kur, maliyet profili, ek gider kuralı, JSON yedeği ve geri yükleme.

Ondalık virgül desteklenir. Satış fiyatı ekranda iki ondalık basamakla gösterilir; hesap ve kayıtta tam ondalık hassasiyet korunur. `MARKUP` maliyet üstü kâr, `MARGIN` gerçek satış marjıdır. `PRODUCT` birim, `ORDER` sipariş kapsamıdır.

## Bilinen sınırlar

- Önceki SQLite sürümünün veri dosyası otomatik taşınmaz. Bu sürüm yeni JSON veri deposuyla açılır; kaynak Excel arşivi yeniden aktarılabilir.
- Kapasiteye dayalı alternatif fiyat, KDV/nakliye kararları, tam koli/palet yuvarlaması ve bazı kaynak talep/doğrulama alanları henüz uygulanmadı.
- Excel'deki kayıtlı formül sonuçları yeniden hesaplanmış sonuç sayılmaz. Güncel fiyat veya kur eksikse onay engellenir.
- Kaynak tedarikçi fiyatlarının vergi ve nakliye durumu otomatik doğrulanmaz. Kullanıcı seçmeden maliyet hesabında kullanılmaz.

## Geliştirme

JDK 21 kurulu Windows'ta `powershell -ExecutionPolicy Bypass -File .\build.ps1` komutu kaynakları derler, kabul testlerini çalıştırır ve taşınabilir Windows uygulama görüntüsünü üretir. Proje kaynakları `src/biotex/`, testleri `tests/`, logo `assets/` ve kaynak ZIP `sources/` içindedir.
