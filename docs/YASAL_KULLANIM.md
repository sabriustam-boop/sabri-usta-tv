# Yasal Kullanim Bildirimi - Sabri Usta TV

> Sabri Usta TV yalnizca resmi, ucretsiz, kamu mali veya kullanicinin erisim hakkina sahip
> oldugu yayinlari oynatmak amaciyla gelistirilmistir. Kullanici ekledigi yayin kaynaklarinin
> kullanim hakkindan sorumludur.

Bu bildirim uygulamanin ilk acilisinda gosterilir ve kabul edilmeden M3U ekleme ekrani acilmaz.

## Uygulamanin yaptigi

- Kullanicinin kendi ekledigi M3U/M3U8 listelerini ayristirir ve oynatir.
- Yayinci tarafindan acikca ucretsiz dagitima acilmis resmi yayinlari icerir
  (`app/src/main/res/raw/starter_catalog.json`).
- Kamu mali / Creative Commons lisansli filmleri oynatir.
- Cihazdaki, kullanicinin sectigi video dosyalarini oynatir.

## Uygulamanin yapmadigi

- Korsan kanal listesi **gommez**. Uygulamanin icinde hazir korsan IPTV listesi yoktur.
- Korsan film sitelerini WebView ile **acmaz**; uygulamada gomulu tarayici yoktur.
- DRM korumasini **asmaz**; DRM'li icerik icin yalnizca standart Media3 davranisi gecerlidir.
- Ucretli servislerin abonelik kontrolunu **atlatmaz**.
- Reklam veya acilir pencere **calistirmaz**.

## Kullanicinin sorumlulugu

Uygulamaya ekleyeceginiz her liste ve baglanti icin gerekli kullanim hakkina sahip olmak
sizin sorumlulugunuzdadir. Baskasina ait, izinsiz yeniden yayinlanan icerikleri eklemek
bulundugunuz ulkenin telif mevzuatina aykiri olabilir.

## Baslangic katalogundaki kaynaklar

| Kaynak | Yayinci | Lisans durumu |
|---|---|---|
| NASA TV Public | NASA | NASA icerikleri kamu malidir |
| Apple HLS ornek yayini | Apple | Gelistiriciler icin yayimlanmis resmi ornek akis |
| SomaFM (3 istasyon) | SomaFM | Yayincinin ucretsiz dinlemeye actigi resmi akislar |
| Big Buck Bunny, Sintel, Tears of Steel, Elephants Dream | Blender Foundation | Creative Commons Attribution |

Bu adresler yayinci tarafindan degistirilebilir. Adres degisirse uygulama cokmez; ilgili
yayin icin Turkce hata mesaji gosterilir ve katalog dosyasi guncellenerek duzeltilebilir.
