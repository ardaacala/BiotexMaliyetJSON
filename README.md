# Biotex Maliyet Yönetimi

Windows için çevrimdışı çalışan, JSON veri deposu kullanan maliyet ve teklif uygulaması.

- [Türkçe kurulum ve kullanım rehberi](./README_TR.md)
- [Kaynak Excel inceleme notları](./SOURCE_AUDIT.md)

JDK 21 bulunan Windows ortamında `powershell -ExecutionPolicy Bypass -File .\build.ps1` komutu testleri çalıştırır ve taşınabilir uygulama paketini `dist/` altında üretir. Hazır Windows paketi için bu deponun **Releases** bölümünü kullanın.

Uygulama verileri `%LOCALAPPDATA%\BiotexMaliyetJSON\biotex-data.json` yolunda tutulur. Kaynak Excel arşivi ticari maliyet ve fiyat bilgileri içerebilir; bu depo özel erişimli tutulmalıdır.
