package com.sabriusta.tv

import com.google.common.truth.Truth.assertThat
import com.sabriusta.tv.data.m3u.M3uMediaType
import com.sabriusta.tv.data.m3u.M3uParser
import org.junit.Test

class M3uParserTest {

    private val sample = """
        #EXTM3U
        #EXTINF:-1 tvg-id="trt1.tr" tvg-name="TRT 1" tvg-logo="https://ornek.test/trt1.png" group-title="Ulusal",TRT 1
        https://ornek.test/trt1/index.m3u8
        #EXTINF:-1 tvg-logo="https://ornek.test/radyo.png" group-title="Radyo" radio="true",Ornek Radyo
        https://ornek.test/radyo/stream
        #EXTINF:5400 group-title="Film",Ornek Film
        https://ornek.test/filmler/ornek.mp4
    """.trimIndent()

    @Test
    fun `temel liste dogru ayristirilir`() {
        val result = M3uParser.parse(sample)
        assertThat(result.entries).hasSize(3)
        assertThat(result.skippedLines).isEmpty()
    }

    @Test
    fun `oznitelikler okunur`() {
        val entry = M3uParser.parse(sample).entries.first()
        assertThat(entry.name).isEqualTo("TRT 1")
        assertThat(entry.tvgId).isEqualTo("trt1.tr")
        assertThat(entry.tvgName).isEqualTo("TRT 1")
        assertThat(entry.logoUrl).isEqualTo("https://ornek.test/trt1.png")
        assertThat(entry.group).isEqualTo("Ulusal")
        assertThat(entry.url).isEqualTo("https://ornek.test/trt1/index.m3u8")
    }

    @Test
    fun `tv radyo ve film ayrimi yapilir`() {
        val entries = M3uParser.parse(sample).entries
        assertThat(entries[0].type).isEqualTo(M3uMediaType.TV)
        assertThat(entries[1].type).isEqualTo(M3uMediaType.RADIO)
        assertThat(entries[2].type).isEqualTo(M3uMediaType.MOVIE)
    }

    @Test
    fun `radio true olmadan da ses uzantisi radyo sayilir`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Muzik Akisi
            https://ornek.test/akis/kanal.mp3
        """.trimIndent()
        assertThat(M3uParser.parse(content).entries.single().type).isEqualTo(M3uMediaType.RADIO)
    }

    @Test
    fun `bos satirlar atlanir ve liste durmaz`() {
        val content = """
            #EXTM3U


            #EXTINF:-1,Kanal A
            https://ornek.test/a.m3u8

            #EXTINF:-1,Kanal B
            https://ornek.test/b.m3u8
        """.trimIndent()
        assertThat(M3uParser.parse(content).entries).hasSize(2)
    }

    @Test
    fun `bozuk satir tum listeyi durdurmaz`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Kanal A
            bu-bir-adres-degil
            #EXTINF:-1,Kanal B
            https://ornek.test/b.m3u8
            ftp://ornek.test/engellenen
            #EXTINF:-1,Kanal C
            https://ornek.test/c.m3u8
        """.trimIndent()
        val result = M3uParser.parse(content)
        assertThat(result.entries).hasSize(2)
        assertThat(result.entries.map { it.name }).containsExactly("Kanal B", "Kanal C")
        assertThat(result.skippedLines).hasSize(2)
    }

    @Test
    fun `tekilleştirme ayni adresi bir kez birakir`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Kanal A
            https://ornek.test/ayni.m3u8
            #EXTINF:-1,Kanal A Yedek
            https://ornek.test/ayni.m3u8
        """.trimIndent()
        val deduped = M3uParser.parse(content, deduplicate = true)
        assertThat(deduped.entries).hasSize(1)
        assertThat(deduped.duplicatesRemoved).isEqualTo(1)

        val raw = M3uParser.parse(content, deduplicate = false)
        assertThat(raw.entries).hasSize(2)
    }

    @Test
    fun `isim yoksa adresten uretilir`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,
            https://ornek.test/kanallar/haber_kanali.m3u8
        """.trimIndent()
        assertThat(M3uParser.parse(content).entries.single().name).isEqualTo("haber kanali")
    }

    @Test
    fun `EXTGRP etiketi grup olarak kullanilir`() {
        val content = """
            #EXTM3U
            #EXTINF:-1,Kanal A
            #EXTGRP:Spor
            https://ornek.test/a.m3u8
        """.trimIndent()
        assertThat(M3uParser.parse(content).entries.single().group).isEqualTo("Spor")
    }

    @Test
    fun `grup adinda virgul olan oznitelik ismi bozmaz`() {
        val content = """
            #EXTM3U
            #EXTINF:-1 group-title="Haber, Politika",NTV
            https://ornek.test/ntv.m3u8
        """.trimIndent()
        val entry = M3uParser.parse(content).entries.single()
        assertThat(entry.name).isEqualTo("NTV")
        assertThat(entry.group).isEqualTo("Haber, Politika")
    }

    @Test
    fun `buyuk liste makul surede islenir`() {
        val builder = StringBuilder("#EXTM3U\n")
        repeat(20_000) { index ->
            builder.append("#EXTINF:-1 group-title=\"Grup ${index % 40}\",Kanal $index\n")
            builder.append("https://ornek.test/kanal/$index.m3u8\n")
        }
        val start = System.currentTimeMillis()
        val result = M3uParser.parse(builder.toString())
        val elapsed = System.currentTimeMillis() - start
        assertThat(result.entries).hasSize(20_000)
        assertThat(elapsed).isLessThan(8_000L)
    }
}
