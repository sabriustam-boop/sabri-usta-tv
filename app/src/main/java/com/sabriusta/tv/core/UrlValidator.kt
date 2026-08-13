package com.sabriusta.tv.core

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI

/**
 * Kullanicidan gelen tum adresler once buradan gecer.
 * Amac: sema kisitlamasi + SSRF (yerel ag / loopback / link-local) engellemesi.
 */
object UrlValidator {

    private val ALLOWED_SCHEMES = setOf("http", "https")

    private val BLOCKED_HOST_NAMES = setOf(
        "localhost", "localhost.localdomain", "ip6-localhost", "ip6-loopback"
    )

    sealed interface Result {
        data class Valid(val url: String) : Result
        data class Invalid(val reason: String) : Result
    }

    fun validate(rawUrl: String, allowHttp: Boolean): Result {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return Result.Invalid("Adres bos olamaz.")
        if (trimmed.any { it == '\n' || it == '\r' || it == '\u0000' }) {
            return Result.Invalid("Adres gecersiz karakter iceriyor.")
        }

        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            return Result.Invalid("Adres bicimi gecersiz.")
        }

        val scheme = uri.scheme?.lowercase()
            ?: return Result.Invalid("Adres http:// veya https:// ile baslamalidir.")

        if (scheme !in ALLOWED_SCHEMES) {
            return Result.Invalid("Yalnizca http ve https adresleri kabul edilir. \"$scheme\" desteklenmiyor.")
        }
        if (scheme == "http" && !allowHttp) {
            return Result.Invalid(
                "Bu adres sifresiz HTTP kullaniyor. Ayarlar > \"HTTP yayinlarina izin ver\" secenegini acmadan eklenemez."
            )
        }

        val host = uri.host?.lowercase()
            ?: return Result.Invalid("Adreste gecerli bir sunucu adi yok.")
        if (host.isBlank()) return Result.Invalid("Adreste gecerli bir sunucu adi yok.")
        if (host in BLOCKED_HOST_NAMES) {
            return Result.Invalid("Yerel ag adreslerine baglanti engellendi.")
        }
        if (isBlockedLiteralHost(host)) {
            return Result.Invalid("Yerel ag adreslerine baglanti engellendi.")
        }
        return Result.Valid(trimmed)
    }

    /** Host bir IP adresi olarak yazilmissa dogrudan kontrol eder (DNS gerektirmez). */
    fun isBlockedLiteralHost(host: String): Boolean {
        val cleaned = host.removePrefix("[").removeSuffix("]")
        if (!looksLikeIpLiteral(cleaned)) return false
        return try {
            isBlockedAddress(InetAddress.getByName(cleaned))
        } catch (e: Exception) {
            true
        }
    }

    fun looksLikeIpLiteral(host: String): Boolean {
        if (host.contains(':')) return true
        val parts = host.split('.')
        if (parts.size != 4) return false
        return parts.all { part -> part.isNotEmpty() && part.all { it.isDigit() } && part.toIntOrNull()?.let { it in 0..255 } == true }
    }

    /** DNS cozumlemesi sonrasi tekrar kontrol icin kullanilir. */
    fun isBlockedAddress(address: InetAddress): Boolean {
        if (address.isLoopbackAddress) return true
        if (address.isAnyLocalAddress) return true
        if (address.isLinkLocalAddress) return true
        if (address.isSiteLocalAddress) return true
        if (address.isMulticastAddress) return true

        val bytes = address.address
        if (address is Inet4Address && bytes.size == 4) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            // 100.64.0.0/10 - operator (CGNAT) agi
            if (b0 == 100 && b1 in 64..127) return true
            // 0.0.0.0/8
            if (b0 == 0) return true
            // 169.254.0.0/16 link-local (isLinkLocal zaten yakalar, ek guvence)
            if (b0 == 169 && b1 == 254) return true
            // 192.0.0.0/24, 192.0.2.0/24 ozel amacli bloklar
            if (b0 == 192 && b1 == 0) return true
            // 198.18.0.0/15 benchmark
            if (b0 == 198 && b1 in 18..19) return true
            // 240.0.0.0/4 rezerve
            if (b0 >= 240) return true
        }
        if (address is Inet6Address && bytes.size == 16) {
            val b0 = bytes[0].toInt() and 0xFF
            // fc00::/7 unique local
            if (b0 and 0xFE == 0xFC) return true
            // IPv4-mapped adresleri de kontrol et
            if (address.isIPv4CompatibleAddress) return true
        }
        return false
    }

    /** Loglara ve hata mesajlarina yazilirken kullanici adi/parola/token gizlenir. */
    fun mask(url: String): String {
        return try {
            val uri = URI(url.trim())
            val host = uri.host ?: return "***"
            val scheme = uri.scheme ?: "***"
            val path = uri.path.orEmpty()
            val shortPath = if (path.length > 24) path.take(24) + "..." else path
            val query = if (uri.rawQuery.isNullOrEmpty()) "" else "?***"
            "$scheme://$host$shortPath$query"
        } catch (e: Exception) {
            "***"
        }
    }
}
