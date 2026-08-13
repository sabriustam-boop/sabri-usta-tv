package com.sabriusta.tv.data.m3u

/** M3U icerigindeki tek bir kayit. */
data class M3uEntry(
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val group: String = "Genel",
    val tvgId: String? = null,
    val tvgName: String? = null,
    val type: M3uMediaType = M3uMediaType.TV,
    val durationSeconds: Long? = null
)

enum class M3uMediaType { TV, RADIO, MOVIE;
    companion object {
        fun fromKey(key: String): M3uMediaType =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: TV
    }
}

data class M3uParseResult(
    val entries: List<M3uEntry>,
    val skippedLines: List<String>,
    val duplicatesRemoved: Int
) {
    val tvCount: Int get() = entries.count { it.type == M3uMediaType.TV }
    val radioCount: Int get() = entries.count { it.type == M3uMediaType.RADIO }
    val movieCount: Int get() = entries.count { it.type == M3uMediaType.MOVIE }
}
