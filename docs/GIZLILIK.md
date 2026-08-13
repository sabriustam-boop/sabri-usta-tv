# Gizlilik Metni - Sabri Usta TV

Son guncelleme: 2026

## Kisaca

Sabri Usta TV **hicbir kisisel veriyi toplamaz, sunucuya gondermez ve ucuncu taraflarla paylasmaz.**
Uygulamada reklam agi, analiz (analytics) kutuphanesi veya takip sistemi bulunmaz.

## Cihazda saklanan veriler

Asagidaki veriler yalnizca telefonunuzun kendi depolamasinda tutulur:

| Veri | Nerede tutulur | Amac |
|---|---|---|
| Ekledginiz M3U listeleri ve kaynak adresleri | Yerel Room veritabani | Listelerinizi yeniden yuklemek |
| Kanal, radyo ve film kayitlari | Yerel Room veritabani | Bolumleri listelemek |
| Favoriler | Yerel Room veritabani | Favoriler bolumu |
| Izleme gecmisi ve kaldiginiz sure | Yerel Room veritabani | "Izlemeye devam et" |
| Ayarlar (tema, tampon, HTTP izni vb.) | DataStore | Tercihlerinizi hatirlamak |

Bu verilerin tamami uygulamayi kaldirdiginizda silinir. Ayarlar > "Izleme gecmisini temizle"
ile gecmisi istediginiz zaman kendiniz de silebilirsiniz.

## Ag baglantilari

Uygulama yalnizca su durumlarda internete cikar:

1. Sizin ekediginiz M3U listesini indirmek icin,
2. Sectiginiz yayini oynatmak icin,
3. Kanal logolarini gostermek icin.

Baglanti kurulan sunucular tamamen sizin ekediginiz kaynaklardir. Bu sunucularin kendi
gizlilik politikalari gecerlidir ve tarafimizca denetlenemez.

## Hassas baglantilarin korunmasi

- M3U adreslerindeki kullanici adi, parola ve token bilgileri **hicbir zaman duz metin olarak
  loglanmaz**; hata mesajlarinda adres maskelenir (ornek: `https://sunucu.com/get.php?***`).
- Veritabani ve ayarlar dosyasi Android bulut yedeklemesinin **disinda** tutulur
  (`data_extraction_rules.xml` ve `backup_rules.xml`), boylece liste adresleriniz cihaz
  degistirirken disari cikmaz.
- Release surumunde debug loglari R8 tarafindan tamamen kaldirilir.

## Izinler

| Izin | Neden gerekli |
|---|---|
| INTERNET | Yayin oynatmak ve liste indirmek |
| ACCESS_NETWORK_STATE | Baglanti yok / Wi-Fi kontrolu |
| FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK | Radyonun arka planda calmasi |
| POST_NOTIFICATIONS | Radyo oynatma bildirimi (Android 13+, sizin onayinizla) |
| READ_MEDIA_VIDEO | Yalnizca cihazdaki bir videoyu secerseniz (Android 13+) |

Genis depolama izni istenmez; dosya secimi Storage Access Framework ile yapilir ve yalnizca
sizin sectiginiz tek dosyaya erisilir.
