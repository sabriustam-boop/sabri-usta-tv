package com.sabriusta.tv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        MediaItemEntity::class,
        CategoryEntity::class,
        FavoriteEntity::class,
        WatchHistoryEntity::class,
        PlaybackProgressEntity::class,
        RadioHistoryEntity::class,
        CustomSourceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun radioHistoryDao(): RadioHistoryDao
    abstract fun customSourceDao(): CustomSourceDao

    companion object {
        const val NAME = "sabri_usta_tv.db"
    }
}
