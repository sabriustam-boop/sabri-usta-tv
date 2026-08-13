package com.sabriusta.tv

import com.google.common.truth.Truth.assertThat
import com.sabriusta.tv.data.m3u.M3uMediaType
import com.sabriusta.tv.data.m3u.M3uParser
import org.junit.Test

class TypeDetectionTest {

    @Test
    fun `radio bayragi her seyin onune gecer`() {
        val type = M3uParser.detectType(
            url = "https://ornek.test/yayin.mp4",
            group = "Filmler",
            isRadioFlag = true,
            durationSeconds = 5400
        )
        assertThat(type).isEqualTo(M3uMediaType.RADIO)
    }

    @Test
    fun `radyo grup adlari taninir`() {
        listOf("Radyo", "RADIO", "Muzik Kanallari", "FM Istasyonlari").forEach { group ->
            assertThat(
                M3uParser.detectType("https://ornek.test/s", group, false, null)
            ).isEqualTo(M3uMediaType.RADIO)
        }
    }

    @Test
    fun `film grup adlari taninir`() {
        listOf("Filmler", "VOD Sinema", "Belgesel", "Diziler").forEach { group ->
            assertThat(
                M3uParser.detectType("https://ornek.test/s", group, false, null)
            ).isEqualTo(M3uMediaType.MOVIE)
        }
    }

    @Test
    fun `pozitif sure kayitli icerik demektir`() {
        assertThat(
            M3uParser.detectType("https://ornek.test/yayin", "Genel", false, 7200)
        ).isEqualTo(M3uMediaType.MOVIE)
    }

    @Test
    fun `varsayilan tur tv olur`() {
        assertThat(
            M3uParser.detectType("https://ornek.test/canli/index.m3u8", "Ulusal", false, -1)
        ).isEqualTo(M3uMediaType.TV)
    }

    @Test
    fun `sorgu parametreli video adresi film sayilir`() {
        assertThat(
            M3uParser.detectType("https://ornek.test/film.mp4?token=abc", "Genel", false, null)
        ).isEqualTo(M3uMediaType.MOVIE)
    }
}
