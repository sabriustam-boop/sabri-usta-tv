package com.sabriusta.tv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY isBuiltIn DESC, id ASC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE enabled = 1")
    suspend fun enabledPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun byId(id: Long): PlaylistEntity?

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE playlists SET autoUpdate = :auto WHERE id = :id")
    suspend fun setAutoUpdate(id: Long, auto: Boolean)

    @Query("UPDATE playlists SET itemCount = :count, lastUpdatedAt = :time, lastError = :error WHERE id = :id")
    suspend fun markUpdated(id: Long, count: Int, time: Long, error: String?)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items WHERE type = :type ORDER BY sortIndex ASC, name ASC LIMIT :limit")
    fun observeByType(type: String, limit: Int = 5000): Flow<List<MediaItemEntity>>

    @Query(
        """
        SELECT * FROM media_items
        WHERE type = :type
          AND (:category = '' OR category = :category)
          AND (:query = '' OR name LIKE '%' || :query || '%')
        ORDER BY sortIndex ASC, name ASC
        LIMIT :limit
        """
    )
    fun search(type: String, category: String, query: String, limit: Int = 3000): Flow<List<MediaItemEntity>>

    @Query("SELECT DISTINCT category FROM media_items WHERE type = :type ORDER BY category ASC")
    fun observeCategories(type: String): Flow<List<String>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun byId(id: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE type = :type ORDER BY sortIndex ASC, name ASC LIMIT :limit")
    suspend fun listByType(type: String, limit: Int = 5000): List<MediaItemEntity>

    @Query("SELECT * FROM media_items ORDER BY addedAt DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int = 20): Flow<List<MediaItemEntity>>

    @Query("SELECT COUNT(*) FROM media_items WHERE type = :type")
    fun observeCount(type: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>)

    @Query("DELETE FROM media_items WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun observeByType(type: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE type = :type")
    suspend fun deleteByType(type: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE type = :type ORDER BY addedAt DESC")
    fun observeByType(type: String): Flow<List<FavoriteEntity>>

    @Query("SELECT itemId FROM favorites")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE itemId = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Query("SELECT * FROM favorites")
    suspend fun listAll(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE itemId = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM favorites")
    suspend fun clear()
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 30): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE type = :type ORDER BY lastPlayedAt DESC LIMIT 1")
    fun observeLast(type: String): Flow<WatchHistoryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WatchHistoryEntity)

    @Query("DELETE FROM watch_history")
    suspend fun clear()
}

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE itemId = :id")
    suspend fun byId(id: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE positionMs > 5000 ORDER BY updatedAt DESC LIMIT :limit")
    fun observeContinueWatching(limit: Int = 20): Flow<List<PlaybackProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress")
    suspend fun clear()
}

@Dao
interface RadioHistoryDao {
    @Query("SELECT * FROM radio_history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<RadioHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: RadioHistoryEntity)

    @Query("DELETE FROM radio_history")
    suspend fun clear()
}

@Dao
interface CustomSourceDao {
    @Query("SELECT * FROM custom_sources ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<CustomSourceEntity>>

    @Query("SELECT * FROM custom_sources WHERE type = :type ORDER BY addedAt DESC")
    fun observeByType(type: String): Flow<List<CustomSourceEntity>>

    @Query("SELECT * FROM custom_sources WHERE id = :id")
    suspend fun byId(id: String): CustomSourceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: CustomSourceEntity)

    @Query("DELETE FROM custom_sources WHERE id = :id")
    suspend fun delete(id: String)
}
