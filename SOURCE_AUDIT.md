# Kaynak inceleme notları

İncelenen girdi: [maliyet-hesaplamalari.zip](./sources/maliyet-hesaplamalari.zip). Dış ZIP ve iç ZIP toplam 67 XLSX içeriyor. Özet bazında 36 farklı içerik; bunların 33'ü ürün reçete çalışma kitabı, 2'si tedarikçi listesi sürümü, 1'i karşılaştırmalı analiz. Aynı özet tekrar içe aktarılmaz.

| Kaynak | Gözlenen alanlar | Uygulama eşlemesi |
| --- | --- | --- |
| Büyük Bohça, `Br. Malzeme Maliyeti` | L miktar, U TRY fiyat, V satır tutarı | SET profili; fiyat güncel sayılmaz |
| Cerrahi Önlük ve NMN318, `Br. Malzeme Maliyeti` | J miktar, Q TRY fiyat, R satır tutarı | ÖNLÜK profili; fiyat güncel sayılmaz |
| `Ana Sayfa!C9` | Ürün adı | Ürün kartı; dosya adı kimlik kaynağı değil |
| `Tedarikçi Fiyat Listesi` | B tarih, E tedarikçi, H kod, I ad, L birim, N para birimi, O fiyat | Seçilmemiş fiyat alternatifi |

## Doğrulanan sayısal ilişkiler

- Alan 150×180 cm, tekrar 5: 13,5 m². 0,146 USD, `1 USD = 3,8 TRY`, maliyet katsayısı 1,015: 7,602147 TRY.
- Yeni uzunluk 50 cm × 3 = 1,5 m; eski MT formülünde tekrar uygulanmazsa 0,5 m. Ayrı yöntemler korunur.
- 3,2 insan dakika × 0,26 = 0,832 TRY. Tolerans ancak profil `CORRECTED` olduğunda uygulanır.
- Büyük Bohça kaynak önbelleğinde `Toplam Maliyeti!D8 + E8` yaklaşık `F8` değerine eşittir: 41,0053098 + 5,330690274 ≈ 46,336000074. Bu, kaydedilmiş kaynak ilişkisidir; bütün reçete zincirinin bağımsız doğrulaması değildir.

## Kaynak anomalileri ve karar bekleyen kurallar

- Büyük Bohça `Br. Malzeme Maliyeti!Q7` VLOOKUP aralığında 11. sütunu istiyor; aralık 8 sütunlu. `IFERROR` bu sorunu sıfırla gizliyor. Sıfır fiyat olarak kullanılmaz.
- Büyük Bohça `Toplam Maliyeti!K22` dış kitaba `[1]Üretim Süresi` bağı taşır; dış dosya aranmaz ve kapasite fiyatı hesaplanmaz.
- Büyük Bohça işçilik sayfasındaki sabit 12,52 insan dakika ile operasyon satırlarının 10,44 dakika toplamı farklıdır. Hangi işçilik kaynağının geçerli olduğu ürün bazında kararlaştırılmalıdır.
- Cerrahi Önlük `Toplam Maliyeti!F8` formül değil, kaydedilmiş sabittir; yeni hesap buna zorla eşitlenmez.
- Bazı setlerde %13, bazılarında %18; önlüklerde %12/%15 ek oran görülür. Bunlar tek varsayılan oran yapılmadı. Profil/kural kullanıcı tarafından seçilir.
- Kaynak %1,5 satır katsayısı fiziksel fire olarak adlandırılmadı; maliyet katsayısı ayrıdır.
- Eski döviz kurları ve tedarikçi fiyatları güncel kabul edilmedi. Bir fiyatın vergi/nakliye kapsamı onaylanmadan müşteri teklifine eklenmez.

Kaynak dosyalar salt okunur incelendi; özgün ZIP değiştirilmedi. Karşılaştırmalı çalışma kitabındaki `REÇETE YOK`, yıldızlı değerler ve birim karışıklıkları otomatik ürün maliyetine dönüştürülmez.
