package com.sabriusta.tv.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object MediaType {
    const val TV = "TV"
    const val RADIO = "RADIO"
    const val MOVIE = "MOVIE"
}

object SourceType {
    const val URL = "URL"
    const val FILE = "FILE"
    const val TEXT = "TEXT"
    const val LOCAL_VIDEO = "LOCAL_VIDEO"
    const val DIRECT = "DIRECT"
    const val CATALOG = "CATALOG"
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** URL kaynagi veya content:// belgesi. Hassas oldugu icin loglanmaz. */
    val source: String,
    val sourceType: String,
    val lastUpdatedAt: Long = 0,
    val autoUpdate: Boolean = false,
    val enabled: Boolean = true,
    val itemCount: Int = 0,
    val lastError: String? = null,
    val isBuiltIn: Boolean = false
)

@Entity(
    tableName = "media_items",
    indices = [Index("playlistId"), Index("type"), Index("category"), Index("name")]
)
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val playlistId: Long,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val category: String = "Genel",
    val type: String = MediaType.TV,
    val tvgId: String? = null,
    val description: String? = null,
    val durationMs: Long? = null,
    val addedAt: Long = 0,
    val sortIndex: Int = 0
)

@Entity(tableName = "categories", primaryKeys = ["name", "type"])
data class CategoryEntity(
    val name: String,
    val type: String,
    val itemCount: Int = 0
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val itemId: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val type: String,
    val category: String = "Genel",
    val addedAt: Long = 0
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val itemId: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val type: String,
    val lastPlayedAt: Long = 0
)

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val itemId: String,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val updatedAt: Long = 0
) {
    val percent: Int
        get() = if (durationMs > 0) ((positionMs * 100) / durationMs).toInt().coerceIn(0, 100) else 0
}

@Entity(tableName = "radio_history")
data class RadioHistoryEntity(
    @PrimaryKey val itemId: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val lastPlayedAt: Long = 0
)

/** Kullanicinin tek tek ekledigi dogrudan yasal adresler ve cihazdaki video dosyalari. */
@Entity(tableName = "custom_sources")
data class CustomSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val type: String,
    val sourceType: String = SourceType.DIRECT,
    val logoUrl: String? = null,
    val description: String? = null,
    val addedAt: Long = 0
)
