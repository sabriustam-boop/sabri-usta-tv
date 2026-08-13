# Kurulum ve Derleme Talimatlari

## 1. Gereksinimler

| Arac | Surum |
|---|---|
| JDK | 17 (Temurin onerilir) |
| Android SDK Platform | API 35 |
| Android SDK Build-Tools | 35.0.0 |
| Gradle | 8.13 (veya Android Studio'nun kendi Gradle'i) |
| Android Studio | Ladybug (2024.2) veya daha yenisi — istege bagli |

## 2. Yerelde derleme

```bash
cd sabri-usta-tv

# Gradle wrapper jar'i depoda yer almadigi icin bir kez uretilir:
gradle wrapper --gradle-version 8.13

# SDK yolunu bildirin (Android Studio kullanmiyorsaniz):
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew testDebugUnitTest     # unit testler
./gradlew lintDebug             # lint
./gradlew assembleDebug         # DEBUG APK
./gradlew assembleRelease       # imzasiz RELEASE APK
```

Ciktilar:

```
app/build/outputs/apk/debug/sabri-usta-tv-v1.0.0-debug.apk
app/build/outputs/apk/release/sabri-usta-tv-v1.0.0-release-unsigned.apk
```

SHA-256 dogrulamasi:

```bash
sha256sum app/build/outputs/apk/debug/*.apk app/build/outputs/apk/release/*.apk
```

## 3. Android Studio ile

1. **File > Open** → `sabri-usta-tv` klasorunu secin.
2. Gradle sync bitmesini bekleyin (ilk sync internetten bagimlilik indirir).
3. Ustteki cihaz secicisinden telefonunuzu/emulatoru secin → **Run ▶**.

## 4. GitHub Actions ile (SDK kurmadan)

`.github/workflows/android-build.yml` hazirdir. Depoya push ettiginizde otomatik calisir:

1. JDK 17 + Android SDK + Gradle 8.13 kurulur (tum action surumleri sabitlenmistir).
2. `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease` calisir.
3. APK'lar `sabri-usta-tv-v1.0.0-debug.apk` ve `sabri-usta-tv-v1.0.0-release-unsigned.apk`
   adiyla kopyalanir, **sifir bayt kontrolu** yapilir, SHA-256 hesaplanir.
4. **Actions > ilgili calisma > Artifacts** bolumunden indirilir.

## 5. Telefona kurma

1. `sabri-usta-tv-v1.0.0-debug.apk` dosyasini telefona kopyalayin.
2. Dosya yoneticisinden dokunun.
3. Android "bilinmeyen kaynaklardan kurulum" izni isterse ilgili uygulamaya izin verin.
4. Kurulum bitince **Sabri Usta TV** simgesiyle acilir.

Debug surumu `com.sabriusta.tv.debug` paket adiyla kurulur; release surumuyle yan yana durabilir.

## 6. Sik karsilasilan sorunlar

| Sorun | Cozum |
|---|---|
| `SDK location not found` | `local.properties` icine `sdk.dir=...` yazin |
| `Unsupported class file major version` | JDK 17 kullandiginizdan emin olun (`java -version`) |
| `gradlew: command not found` | Once `gradle wrapper --gradle-version 8.13` calistirin |
| Release APK kurulmuyor | Imzasizdir; `apksigner` ile imzalayin (README'de komut var) |
| Gradle sync bagimlilik indiremiyor | Aginizin `dl.google.com` ve `repo.maven.apache.org` erisimini kontrol edin |
