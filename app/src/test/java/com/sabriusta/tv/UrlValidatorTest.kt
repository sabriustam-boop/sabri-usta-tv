package com.sabriusta.tv

import com.google.common.truth.Truth.assertThat
import com.sabriusta.tv.core.UrlValidator
import org.junit.Test
import java.net.InetAddress

class UrlValidatorTest {

    @Test
    fun `https adres kabul edilir`() {
        val result = UrlValidator.validate("https://ornek.test/liste.m3u", allowHttp = false)
        assertThat(result).isInstanceOf(UrlValidator.Result.Valid::class.java)
    }

    @Test
    fun `http varsayilan olarak reddedilir`() {
        val result = UrlValidator.validate("http://ornek.test/liste.m3u", allowHttp = false)
        assertThat(result).isInstanceOf(UrlValidator.Result.Invalid::class.java)
    }

    @Test
    fun `http izin verildiginde kabul edilir`() {
        val result = UrlValidator.validate("http://ornek.test/liste.m3u", allowHttp = true)
        assertThat(result).isInstanceOf(UrlValidator.Result.Valid::class.java)
    }

    @Test
    fun `tehlikeli semalar engellenir`() {
        val schemes = listOf(
            "file:///sdcard/liste.m3u",
            "javascript:alert(1)",
            "content://com.ornek/liste",
            "ftp://ornek.test/liste.m3u",
            "data:text/plain;base64,QQ=="
        )
        schemes.forEach { url ->
            assertThat(UrlValidator.validate(url, allowHttp = true))
                .isInstanceOf(UrlValidator.Result.Invalid::class.java)
        }
    }

    @Test
    fun `localhost ve loopback engellenir`() {
        val urls = listOf(
            "http://localhost:8080/liste.m3u",
            "http://127.0.0.1/liste.m3u",
            "https://127.5.5.5/liste.m3u",
            "http://[::1]/liste.m3u"
        )
        urls.forEach { url ->
            assertThat(UrlValidator.validate(url, allowHttp = true))
                .isInstanceOf(UrlValidator.Result.Invalid::class.java)
        }
    }

    @Test
    fun `ozel yerel ag adresleri engellenir`() {
        val urls = listOf(
            "http://192.168.1.10/liste.m3u",
            "http://10.0.0.5/liste.m3u",
            "http://172.16.4.4/liste.m3u",
            "http://169.254.1.1/liste.m3u",
            "http://100.100.5.5/liste.m3u",
            "http://0.0.0.0/liste.m3u"
        )
        urls.forEach { url ->
            assertThat(UrlValidator.validate(url, allowHttp = true))
                .isInstanceOf(UrlValidator.Result.Invalid::class.java)
        }
    }

    @Test
    fun `genel ip adresi kabul edilir`() {
        val result = UrlValidator.validate("https://93.184.216.34/liste.m3u", allowHttp = false)
        assertThat(result).isInstanceOf(UrlValidator.Result.Valid::class.java)
    }

    @Test
    fun `bos ve bozuk adresler reddedilir`() {
        listOf("", "   ", "sadece-metin", "https://", "http://\nornek.test").forEach { url ->
            assertThat(UrlValidator.validate(url, allowHttp = true))
                .isInstanceOf(UrlValidator.Result.Invalid::class.java)
        }
    }

    @Test
    fun `cozumlenen adresler icin engelleme calisir`() {
        assertThat(UrlValidator.isBlockedAddress(InetAddress.getByName("127.0.0.1"))).isTrue()
        assertThat(UrlValidator.isBlockedAddress(InetAddress.getByName("192.168.0.1"))).isTrue()
        assertThat(UrlValidator.isBlockedAddress(InetAddress.getByName("8.8.8.8"))).isFalse()
    }

    @Test
    fun `maskeleme kullanici adi parola ve token gizler`() {
        val masked = UrlValidator.mask("https://kullanici:parola@ornek.test/get.php?username=abc&password=xyz")
        assertThat(masked).doesNotContain("parola")
        assertThat(masked).doesNotContain("xyz")
        assertThat(masked).doesNotContain("username")
        assertThat(masked).contains("ornek.test")
    }
}
