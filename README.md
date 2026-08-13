# Sabri Usta TV

**TV • Radyo • Film — Tek Uygulamada Keyifli Yayin**

Canli TV, internet radyosu ve yasal/kamu mali film iceriklerini tek uygulamada toplayan,
kullanicinin kendi M3U/M3U8 listelerini ekleyebildigi Android uygulamasi.

| | |
|---|---|
| Paket adi | `com.sabriusta.tv` |
| Surum | 1.0.0 (versionCode 1) |
| Min. Android | 8.0 (API 26) |
| Hedef SDK | 35 |
| Dil | Kotlin, Jetpack Compose |
| Mimari | MVVM + katmanli (data / core / ui), Hilt DI |
| Oynatici | AndroidX Media3 (ExoPlayer + MediaSession) |

---

## ⚠️ Bu paket derlenmis APK icermiyor — nedeni ve cozumu

Bu proje, **Android SDK'si kurulu olmayan ve ag erisimi kapali** bir ortamda uretildi
(`dl.google.com` dahil tum disa baglantilar `host_not_allowed` ile reddediliyor). Bu yuzden
SDK indirilemedi, Gradle bagimliliklari cozulemedi ve APK burada derlenemedi.

Bunun yerine, talimattaki yedek yol uygulandi: **`.github/workflows/android-build.yml`**
sabitlenmis surumlerle testleri calistirir, lint yapar, debug + imzasiz release APK uretir,
SHA-256 hesaplar ve artifact olarak yukler.

### APK'yi GitHub Actions'tan alma (adim adim)

1. Bu klasoru bir GitHub deposuna yukleyin:
   ```bash
   cd sabri-usta-tv
   git init && git add . && git commit -m "Sabri Usta TV 1.0.0"
   git branch -M main
   git remote add origin https://github.com/KULLANICI/sabri-usta-tv.git
   git push -u origin main
   ```
2. GitHub'da depoya gidin → **Actions** sekmesi → **Sabri Usta TV - Derleme** akisi otomatik baslar
   (baslamazsa **Run workflow** dugmesine basin).
3. Akis bitince (yaklasik 8–15 dk) calisma sayfasinin altindaki **Artifacts** bolumunden indirin:
   - `sabri-usta-tv-apk` → `sabri-usta-tv-v1.0.0-debug.apk`,
     `sabri-usta-tv-v1.0.0-release-unsigned.apk`, `SHA256SUMS.txt`
   - `sabri-usta-tv-raporlar` → unit test ve lint raporlari
4. ZIP'i acin, `sabri-usta-tv-v1.0.0-debug.apk` dosyasini telefona kopyalayip kurun
   (Ayarlar → *Bilinmeyen kaynaklara izin ver*).

> Not: `sabri-usta-tv-v1.0.0-release-unsigned.apk` **imzasizdir**, dogrudan kurulamaz.
> Kurmak icin kendi anahtarinizla imzalamaniz gerekir (asagida).

### Kendi bilgisayarinizda derleme

```bash
# Gereksinimler: JDK 17, Android SDK (API 35), Gradle 8.13+ veya Android Studio
cd sabri-usta-tv
gradle wrapper --gradle-version 8.13      # wrapper jar'i uretir (bir kez)
./gradlew testDebugUnitTest lintDebug     # testler + lint
./gradlew assembleDebug                   # app/build/outputs/apk/debug/
./gradlew assembleRelease                 # imzasiz release
sha256sum app/build/outputs/apk/debug/*.apk
```

Android Studio kullanacaksaniz: **Open** → bu klasoru secin → Gradle sync → **Run**.

`local.properties` gerekiyorsa: `sdk.dir=/Android/Sdk/yolunuz`

### Release APK'yi imzalama

```bash
keytool -genkey -v -keystore sabri.jks -keyalg RSA -keysize 2048 -validity 10000 -alias sabri
$ANDROID_HOME/build-tools/35.0.0/apksigner sign --ks sabri.jks \
  --out sabri-usta-tv-v1.0.0-release.apk sabri-usta-tv-v1.0.0-release-unsigned.apk
```

---

## Ozellikler

### Bolumler
Ana Sayfa · Canli TV · Radyo · Filmler · Favoriler · Ayarlar (alt menu)

- **Ana Sayfa:** Izlemeye devam et, favori kanallar, son oynatilanlar, son eklenen yayinlar,
  bolum kisayollari (kanal/istasyon/film sayilariyla), M3U ekleme karti, buyuk kartlar.
- **Canli TV:** kanal listesi + logolar, kategori filtreleri, arama, favori, son izlenen kanal,
  tam ekran, HLS/M3U8 + DASH + MP4 ve Media3'un destekledigi diger bicimler.
- **Radyo:** istasyon listesi, tur/sehir kategorileri (M3U grup adindan), arama, favoriler,
  **arka planda calma**, bildirim ve kilit ekrani kontrolleri, **uyku zamanlayicisi (15/30/45/60/90 dk)**,
  son calinan istasyon, AAC/MP3 akislari.
- **Filmler:** kapak, ad, aciklama, kategori, arama, favori, **ilerleme kaydi ve kaldigi yerden devam**,
  altyazi secimi, **harici SRT/VTT**, ses kanali secimi, oynatma hizi, cihazdan video secme (SAF).
- **Favoriler / Ayarlar:** tam liste asagida.

### M3U/M3U8 sistemi
- Ekleme: **baglanti yapistirma**, **telefondan dosya secme (SAF)**, **metni dogrudan yapistirma**.
- Liste basina: ad, kaynak, son guncelleme, otomatik guncelleme, manuel yenile, duzenle, sil,
  etkin/pasif, kanal sayisi, hatali baglanti uyarisi.
- Ayristirici: `#EXTM3U`, `#EXTINF`, `tvg-id`, `tvg-name`, `tvg-logo`, `group-title`, `radio=true`,
  `#EXTGRP`, kanal adi, akis adresi. Bos satirlari atlar, **bozuk satir tum listeyi durdurmaz**,
  TV/radyo/film ayrimi yapar, istege bagli tekillestirme, buyuk listeler `Dispatchers.Default`
  uzerinde 500'luk parcalar halinde yazilir (arayuz donmaz).

### Oynatici
Oynat/duraklat · 10 sn ileri-geri · **canli yayina don** · tam ekran · ekran yonu ·
**kenar hareketiyle ses ve parlaklik** · kalite (video track) secimi · altyazi ac/kapa ve secim ·
ses kanali secimi · oynatma hizi (0.5x–2x) · ekran kilidi · **resim icinde resim** · onceki/sonraki yayin.

Turkce hata mesajlari: internet yok · adres gecersiz · sunucu yanit vermiyor · bicim desteklenmiyor ·
icerik oynatilamadi · erisim reddedildi · yayin gecici olarak kapali — her birinde **Yeniden dene**.

### Veritabani (Room)
`Playlist` · `MediaItem` · `Category` · `Favorite` · `WatchHistory` · `PlaybackProgress` ·
`RadioHistory` · `CustomSource` — favoriler ve gecmis uygulama yeniden acildiginda korunur.

### Ayarlar
Tema (Sistem/Acik/Koyu, varsayilan koyu) · varsayilan ekran yonu · otomatik tam ekran ·
yalnizca Wi-Fi · mobil veri uyarisi · tampon boyutu (10–120 sn) · tekillestirme ·
HTTP izni (risk uyarili) · gecmisi temizle · favorileri ve M3U listelerini disa/ice aktarma ·
uygulama hakkinda · gizlilik · yasal kullanim bildirimi.

---

## Guvenlik

### SSRF korumasi (`core/UrlValidator.kt` + `data/remote/PlaylistFetcher.kt`)
- Yalnizca `http` / `https`. `file:`, `javascript:`, `content:`, `data:`, `ftp:` reddedilir.
- `localhost`, loopback (127.0.0.0/8, ::1), link-local (169.254/16), ozel aglar (10/8, 172.16/12,
  192.168/16), CGNAT (100.64/10), IPv6 unique-local (fc00::/7), multicast ve rezerve bloklar engellenir.
- Yonlendirmeler **elle** takip edilir (`followRedirects(false)`) ve **her adimda hedef yeniden dogrulanir**;
  en fazla 5 yonlendirme.
- DNS cozumlemesi sonrasi donen tum IP adresleri ayrica kontrol edilir.
- 20 MB indirme siniri, 15 sn baglanti / 30 sn okuma / 120 sn toplam zaman asimi.
- Loglarda ve hata mesajlarinda URL maskelenir: `https://sunucu.com/get.php?***`.

### Diger
- WebView yok, gomulu site yok, reklam agi yok, takip yok, DRM asma yok.
- API anahtari kaynak kodda yok.
- Release'te R8 ile `Log.v/d/i` cagrilari kaldirilir; `minifyEnabled` + `shrinkResources` acik.
- Disa acilan bilesen sayisi minimumda: yalnizca `MainActivity` (LAUNCHER) exported.
  `RadioPlaybackService` **exported degildir**. Deep link tanimlanmamistir.
- Yedekleme kurallari veritabanini ve DataStore'u haric tutar (liste adresleriniz yedege cikmaz).

### Cleartext (HTTP) konusunda acik not
Android'de `networkSecurityConfig` **calisma aninda degistirilemez**. "HTTP yayinlarina izin ver"
secenegi gercekten calissin diye platform seviyesinde cleartext acik birakildi; **asil engelleme
uygulama katmanindadir** (`UrlValidator`) ve **varsayilan degeri kapalidir**. Ayar kapaliyken hicbir
`http://` adresi liste olarak eklenemez ve oynatilamaz. `localhost` / `127.0.0.1` icin ayrica
platform seviyesinde de cleartext kapatilmistir. Alternatif (cleartext'i tamamen kapatmak) secilseydi
Ayarlar'daki anahtar hicbir zaman calismayan sahte bir dugme olurdu — bu istenmediginden bu yol secildi.

---

## Ilk sürümde bilerek yapilmayanlar (sahte dugme birakilmadi)

| Ozellik | Neden yok |
|---|---|
| **Chromecast** | Duzgun uygulanmasi Google Cast SDK + Google Play Services bagimliligi ve gercek cihazla test gerektirir. Bu ortamda dogrulanamadigi icin calismayan bir yayin dugmesi koymak yerine tamamen cikarildi. |
| **EPG / yayin akisi rehberi** | Sartnamede istenmedi; XMLTV kaynagi olmadan sahte veri gosterilemez. |
| **Retrofit** | Projede cagrilacak bir REST API yok; liste indirme dogrudan OkHttp ile yapiliyor. Kullanilmayan bagimlilik eklenmedi. |
| **Sehir bazli radyo kategorisi (ayri alan)** | M3U formatinda standart bir "sehir" alani yoktur. Kategoriler `group-title` degerinden uretilir; listede sehir grubu varsa otomatik gorunur. |
| **Reklam modulu** | Sartnameye uygun olarak yalnizca **kapali arayuz** birakildi; hicbir reklam SDK'si eklenmedi. Ileride `ui/` altina eklenecek bir modul icin veri modeli ve yerlesim yeri hazir, varsayilan surum reklamsizdir. |

---

## Proje yapisi

```
app/src/main/java/com/sabriusta/tv/
├── SabriUstaTvApp.kt          # Hilt uygulama sinifi, ilk acilis katalog yuklemesi
├── MainActivity.kt            # Tek Activity, PiP, ekran yonu
├── core/
│   ├── UrlValidator.kt        # Sema + SSRF dogrulama, URL maskeleme
│   ├── PlaybackErrors.kt      # Media3 hata kodu → Turkce mesaj
│   └── NetworkMonitor.kt      # Cevrimici / Wi-Fi durumu
├── data/
│   ├── m3u/                   # M3uParser (toleransli), modeller
│   ├── local/                 # Room: 8 entity + DAO + AppDatabase
│   ├── prefs/                 # DataStore ayarlar
│   ├── remote/                # PlaylistFetcher (SSRF-guvenli indirici)
│   ├── catalog/               # starter_catalog.json yukleyici
│   └── repo/                  # PlaylistRepository, MediaRepository
├── di/AppModule.kt            # Hilt saglayicilari
├── player/
│   ├── RadioPlaybackService.kt  # MediaSessionService + uyku zamanlayici
│   └── RadioController.kt       # MediaController koprusu
└── ui/                        # theme, nav, components + 8 ekran
app/src/main/res/raw/starter_catalog.json   # tek katalog dosyasi
app/src/test/                  # 4 unit test sinifi (30+ test)
app/src/androidTest/           # Room + Compose arayuz testleri
```

## Testler

```bash
./gradlew testDebugUnitTest          # JVM testleri
./gradlew connectedDebugAndroidTest  # cihaz/emulator gerektirir
```

- `M3uParserTest` — temel ayristirma, oznitelikler, TV/radyo/film ayrimi, bos satir,
  bozuk satir toleransi, tekillestirme, isim uretimi, `#EXTGRP`, virgullu grup adi,
  20.000 kayitlik liste performansi.
- `UrlValidatorTest` — sema kisitlamasi, HTTP izni, tehlikeli semalar, localhost/loopback,
  ozel ag ve CGNAT bloklari, genel IP kabulu, bozuk adresler, maskeleme.
- `PlaybackErrorsTest` — hata kodu → Turkce mesaj esleme.
- `TypeDetectionTest` — tur tespiti oncelik kurallari.
- `DatabaseTest` (androidTest) — playlist/yayin kaydi, arama+kategori filtresi, favori ekle/sil,
  gecmis ve ilerleme, liste silme.
- `ComponentsUiTest` (androidTest) — bos durum, hata kutusu + yeniden dene, kanal satiri tiklama.

## Belgeler

- [Kullanim Rehberi](docs/KULLANIM_REHBERI.md)
- [Gizlilik Metni](docs/GIZLILIK.md)
- [Yasal Kullanim Bildirimi](docs/YASAL_KULLANIM.md)

## Baslangic katalogunu degistirme

Tek dosya: `app/src/main/res/raw/starter_catalog.json`. Her kaynak icin `name`, `publisher`,
`url`, `logo`, `country`, `category`, `type`, `sourcePage`, `license` alanlari vardir.
Dosya bozuksa veya okunamazsa uygulama **bos katalogla acilir, cokmez**; calismayan tek bir
yayin ise yalnizca kendi hata mesajini gosterir.
