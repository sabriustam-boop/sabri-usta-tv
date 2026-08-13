package com.sabriusta.tv.data.remote

import android.content.ContentResolver
import android.net.Uri
import com.sabriusta.tv.core.UrlValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * M3U indirici.
 *  - Yalnizca http/https semalari
 *  - Her yonlendirme adiminda hedef adres yeniden dogrulanir (SSRF korumasi)
 *  - En fazla 5 yonlendirme, 20 MB boyut siniri, zaman asimi sinirlari
 *  - Hata mesajlarinda tam URL degil maskelenmis adres kullanilir
 */
@Singleton
class PlaylistFetcher @Inject constructor(
    private val contentResolver: ContentResolver
) {
    companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
        const val MAX_REDIRECTS = 5
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(true)
        .build()

    sealed interface FetchResult {
        data class Success(val content: String) : FetchResult
        data class Failure(val message: String) : FetchResult
    }

    private sealed interface Step {
        data class Done(val result: FetchResult) : Step
        data class Redirect(val url: String) : Step
    }

    suspend fun fetchFromUrl(rawUrl: String, allowHttp: Boolean): FetchResult = withContext(Dispatchers.IO) {
        var currentUrl = rawUrl.trim()
        var attempts = 0
        while (attempts <= MAX_REDIRECTS) {
            attempts++
            val validation = UrlValidator.validate(currentUrl, allowHttp)
            if (validation is UrlValidator.Result.Invalid) {
                return@withContext FetchResult.Failure(validation.reason)
            }
            val dnsProblem = checkResolvedAddresses(currentUrl)
            if (dnsProblem != null) return@withContext FetchResult.Failure(dnsProblem)

            when (val step = performRequest(currentUrl)) {
                is Step.Done -> return@withContext step.result
                is Step.Redirect -> currentUrl = step.url
            }
        }
        FetchResult.Failure("Cok fazla yonlendirme yapildi, islem guvenlik nedeniyle durduruldu.")
    }

    private fun performRequest(url: String): Step {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SabriUstaTV/1.0.0")
            .header("Accept", "*/*")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isRedirect) {
                    val location = response.header("Location")
                    if (location.isNullOrBlank()) {
                        Step.Done(FetchResult.Failure("Sunucu gecersiz bir yonlendirme dondurdu."))
                    } else {
                        Step.Redirect(resolveRedirect(url, location))
                    }
                } else if (!response.isSuccessful) {
                    Step.Done(
                        FetchResult.Failure(
                            "Liste indirilemedi (HTTP ${response.code}) - ${UrlValidator.mask(url)}"
                        )
                    )
                } else {
                    val body = response.body
                    if (body == null) {
                        Step.Done(FetchResult.Failure("Sunucudan bos yanit alindi."))
                    } else if (body.contentLength() > MAX_BYTES) {
                        Step.Done(FetchResult.Failure("Liste cok buyuk (20 MB sinirini asiyor)."))
                    } else {
                        val bytes = readLimited(body.byteStream())
                        if (bytes == null) {
                            Step.Done(FetchResult.Failure("Liste cok buyuk (20 MB sinirini asiyor)."))
                        } else {
                            Step.Done(FetchResult.Success(bytes.toString(Charsets.UTF_8)))
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Step.Done(FetchResult.Failure("Baglanti kurulamadi - ${UrlValidator.mask(url)}"))
        } catch (e: Exception) {
            Step.Done(FetchResult.Failure("Beklenmeyen hata: liste okunamadi."))
        }
    }

    suspend fun readFromUri(uri: Uri): FetchResult = withContext(Dispatchers.IO) {
        try {
            val stream = contentResolver.openInputStream(uri)
                ?: return@withContext FetchResult.Failure("Dosya acilamadi.")
            stream.use { input ->
                val bytes = readLimited(input)
                    ?: return@withContext FetchResult.Failure("Dosya cok buyuk (20 MB sinirini asiyor).")
                FetchResult.Success(bytes.toString(Charsets.UTF_8))
            }
        } catch (e: SecurityException) {
            FetchResult.Failure("Dosyaya erisim izni yok. Dosyayi yeniden secin.")
        } catch (e: Exception) {
            FetchResult.Failure("Dosya okunamadi.")
        }
    }

    /** Sinira kadar okur; sinir asilirsa null doner. */
    private fun readLimited(input: InputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            if (total > MAX_BYTES) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun resolveRedirect(base: String, location: String): String = try {
        URI(base).resolve(location).toString()
    } catch (e: Exception) {
        location
    }

    private fun checkResolvedAddresses(url: String): String? = try {
        val host = URI(url).host
        if (host.isNullOrBlank()) {
            "Adreste gecerli bir sunucu adi yok."
        } else {
            val addresses = InetAddress.getAllByName(host)
            if (addresses.any { UrlValidator.isBlockedAddress(it) }) {
                "Yerel ag adreslerine baglanti engellendi."
            } else {
                null
            }
        }
    } catch (e: Exception) {
        "Sunucu adresi cozumlenemedi."
    }
}
