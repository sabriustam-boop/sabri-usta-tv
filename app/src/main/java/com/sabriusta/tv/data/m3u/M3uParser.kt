package com.sabriusta.tv.data.m3u

/**
 * Toleransli M3U/M3U8 ayristirici.
 *
 * Kurallar:
 *  - Bos satirlar atlanir.
 *  - Bozuk bir satir tum listeyi durdurmaz, sadece atlanir ve raporlanir.
 *  - TV / Radyo / Film ayrimi oznitelik, grup adi ve dosya uzantisina gore yapilir.
 *  - Hicbir yerde tam URL loglanmaz (cagiran taraf UrlValidator.mask kullanir).
 */
object M3uParser {

    // Ornek: tvg-logo="https://..." group-title="Ulusal"
    private val ATTRIBUTE_REGEX = Regex("([A-Za-z0-9_.\\-]+)\\s*=\\s*\"([^\"]*)\"")

    private val RADIO_GROUP_HINTS = listOf(
        "radyo", "radio", "fm", "muzik", "müzik", "music", "audio", "ses"
    )
    private val MOVIE_GROUP_HINTS = listOf(
        "film", "movie", "sinema", "cinema", "vod", "dizi", "series", "belgesel", "documentary"
    )
    private val AUDIO_EXTENSIONS = listOf(".mp3", ".aac", ".ogg", ".opus", ".m4a", ".wma", ".flac")
    private val VIDEO_FILE_EXTENSIONS = listOf(".mp4", ".mkv", ".avi", ".mov", ".webm", ".m4v", ".flv")

    private data class Pending(
        val name: String?,
        val logo: String?,
        val group: String?,
        val tvgId: String?,
        val tvgName: String?,
        val isRadioFlag: Boolean,
        val durationSeconds: Long?
    )

    fun parse(content: String, deduplicate: Boolean = true): M3uParseResult {
        val entries = mutableListOf<M3uEntry>()
        val skipped = mutableListOf<String>()
        var pending: Pending? = null
        var lineNumber = 0

        content.lineSequence().forEach { rawLine ->
            lineNumber++
            val line = rawLine.trim().removePrefix("\uFEFF").trim()
            when {
                line.isEmpty() -> Unit

                line.startsWith("#EXTM3U", ignoreCase = true) -> Unit

                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pending = runCatching { parseExtInf(line) }.getOrElse {
                        skipped += "Satir $lineNumber: bilgi satiri okunamadi"
                        null
                    }
                }

                line.startsWith("#EXTGRP", ignoreCase = true) -> {
                    val group = line.substringAfter(':', "").trim()
                    if (group.isNotEmpty()) {
                        pending = pending?.copy(group = group) ?: Pending(
                            null, null, group, null, null, false, null
                        )
                    }
                }

                line.startsWith("#") -> Unit // desteklenmeyen etiketler gormezden gelinir

                else -> {
                    val info = pending
                    pending = null
                    val url = line
                    if (!isAcceptableUrl(url)) {
                        skipped += "Satir $lineNumber: gecersiz veya desteklenmeyen adres"
                        return@forEach
                    }
                    val name = info?.name?.takeIf { it.isNotBlank() }
                        ?: info?.tvgName?.takeIf { it.isNotBlank() }
                        ?: fallbackName(url)
                    entries += M3uEntry(
                        name = name,
                        url = url,
                        logoUrl = info?.logo?.takeIf { it.isNotBlank() },
                        group = info?.group?.takeIf { it.isNotBlank() } ?: "Genel",
                        tvgId = info?.tvgId?.takeIf { it.isNotBlank() },
                        tvgName = info?.tvgName?.takeIf { it.isNotBlank() },
                        type = detectType(
                            url = url,
                            group = info?.group,
                            isRadioFlag = info?.isRadioFlag == true,
                            durationSeconds = info?.durationSeconds
                        ),
                        durationSeconds = info?.durationSeconds?.takeIf { it > 0 }
                    )
                }
            }
        }

        if (!deduplicate) {
            return M3uParseResult(entries, skipped, 0)
        }
        val unique = entries.distinctBy { it.url.trim().lowercase() }
        return M3uParseResult(unique, skipped, entries.size - unique.size)
    }

    private fun parseExtInf(line: String): Pending {
        val payload = line.substringAfter(':', "")
        val attributes = ATTRIBUTE_REGEX.findAll(payload)
            .associate { it.groupValues[1].lowercase() to it.groupValues[2] }

        // Isim, son virgulden sonra gelir. Oznitelik degerlerinde virgul olabilecegi icin
        // once oznitelik bloklarini temizleriz.
        val withoutAttributes = ATTRIBUTE_REGEX.replace(payload, "")
        val name = withoutAttributes.substringAfter(',', "").trim()

        val duration = withoutAttributes.substringBefore(',', "")
            .trim()
            .takeWhile { it.isDigit() || it == '-' || it == '.' }
            .toDoubleOrNull()
            ?.toLong()

        val radioFlag = attributes["radio"]?.equals("true", ignoreCase = true) == true

        return Pending(
            name = name.ifBlank { null },
            logo = attributes["tvg-logo"] ?: attributes["logo"],
            group = attributes["group-title"] ?: attributes["group"],
            tvgId = attributes["tvg-id"],
            tvgName = attributes["tvg-name"],
            isRadioFlag = radioFlag,
            durationSeconds = duration?.takeIf { it > 0 }
        )
    }

    private fun isAcceptableUrl(url: String): Boolean {
        val lower = url.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
        if (url.length < 12) return false
        return !url.contains(' ')
    }

    private fun fallbackName(url: String): String {
        val candidate = url.substringAfterLast('/')
            .substringBefore('?')
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
        return candidate.ifBlank { "Isimsiz yayin" }
    }

    fun detectType(
        url: String,
        group: String?,
        isRadioFlag: Boolean,
        durationSeconds: Long?
    ): M3uMediaType {
        if (isRadioFlag) return M3uMediaType.RADIO

        val lowerUrl = url.lowercase().substringBefore('?')
        val lowerGroup = group?.lowercase().orEmpty()

        if (AUDIO_EXTENSIONS.any { lowerUrl.endsWith(it) }) return M3uMediaType.RADIO
        if (RADIO_GROUP_HINTS.any { lowerGroup.contains(it) }) return M3uMediaType.RADIO

        if (MOVIE_GROUP_HINTS.any { lowerGroup.contains(it) }) return M3uMediaType.MOVIE
        if (VIDEO_FILE_EXTENSIONS.any { lowerUrl.endsWith(it) }) return M3uMediaType.MOVIE
        // EXTINF suresi pozitifse canli yayin degil, kayitli icerik demektir.
        if (durationSeconds != null && durationSeconds > 0) return M3uMediaType.MOVIE

        return M3uMediaType.TV
    }
}
