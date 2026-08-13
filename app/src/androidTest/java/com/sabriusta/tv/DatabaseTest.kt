package com.sabriusta.tv

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sabriusta.tv.data.local.AppDatabase
import com.sabriusta.tv.data.local.FavoriteEntity
import com.sabriusta.tv.data.local.MediaItemEntity
import com.sabriusta.tv.data.local.MediaType
import com.sabriusta.tv.data.local.PlaybackProgressEntity
import com.sabriusta.tv.data.local.PlaylistEntity
import com.sabriusta.tv.data.local.SourceType
import com.sabriusta.tv.data.local.WatchHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun playlistVeYayinlarKaydedilir() = runTest {
        val playlistId = db.playlistDao().insert(
            PlaylistEntity(name = "Test", source = "https://ornek.test/l.m3u", sourceType = SourceType.URL)
        )
        db.mediaItemDao().insertAll(
            listOf(
                item("a", playlistId, MediaType.TV, "Kanal A", "Ulusal"),
                item("b", playlistId, MediaType.RADIO, "Radyo B", "Muzik"),
                item("c", playlistId, MediaType.MOVIE, "Film C", "Sinema")
            )
        )
        assertThat(db.mediaItemDao().listByType(MediaType.TV)).hasSize(1)
        assertThat(db.mediaItemDao().observeCount(MediaType.RADIO).first()).isEqualTo(1)
        assertThat(db.mediaItemDao().observeCategories(MediaType.MOVIE).first()).containsExactly("Sinema")
    }

    @Test
    fun aramaVeKategoriFiltresiCalisir() = runTest {
        val playlistId = db.playlistDao().insert(
            PlaylistEntity(name = "Test", source = "x", sourceType = SourceType.TEXT)
        )
        db.mediaItemDao().insertAll(
            listOf(
                item("a", playlistId, MediaType.TV, "Haber Kanali", "Haber"),
                item("b", playlistId, MediaType.TV, "Spor Kanali", "Spor")
            )
        )
        assertThat(db.mediaItemDao().search(MediaType.TV, "", "Spor").first()).hasSize(1)
        assertThat(db.mediaItemDao().search(MediaType.TV, "Haber", "").first()).hasSize(1)
        assertThat(db.mediaItemDao().search(MediaType.TV, "", "").first()).hasSize(2)
    }

    @Test
    fun favoriEklenirVeSilinir() = runTest {
        val favorite = FavoriteEntity(
            itemId = "a", name = "Kanal A", url = "https://ornek.test/a", type = MediaType.TV
        )
        db.favoriteDao().insert(favorite)
        assertThat(db.favoriteDao().isFavorite("a")).isTrue()
        assertThat(db.favoriteDao().observeAll().first()).hasSize(1)

        db.favoriteDao().delete("a")
        assertThat(db.favoriteDao().isFavorite("a")).isFalse()
    }

    @Test
    fun izlemeGecmisiVeIlerlemeSaklanir() = runTest {
        db.watchHistoryDao().insert(
            WatchHistoryEntity("a", "Film", "https://ornek.test/a.mp4", null, MediaType.MOVIE, 100)
        )
        db.playbackProgressDao().upsert(PlaybackProgressEntity("a", 30_000, 120_000, 100))

        assertThat(db.watchHistoryDao().observeRecent().first()).hasSize(1)
        val progress = db.playbackProgressDao().byId("a")
        assertThat(progress).isNotNull()
        assertThat(progress!!.percent).isEqualTo(25)
        assertThat(db.playbackProgressDao().observeContinueWatching().first()).hasSize(1)
    }

    @Test
    fun listeSilinincePlaylistYayinlariSilinir() = runTest {
        val playlistId = db.playlistDao().insert(
            PlaylistEntity(name = "Test", source = "x", sourceType = SourceType.TEXT)
        )
        db.mediaItemDao().insertAll(listOf(item("a", playlistId, MediaType.TV, "Kanal A", "Genel")))
        db.mediaItemDao().deleteByPlaylist(playlistId)
        db.playlistDao().delete(playlistId)

        assertThat(db.mediaItemDao().listByType(MediaType.TV)).isEmpty()
        assertThat(db.playlistDao().count()).isEqualTo(0)
    }

    private fun item(id: String, playlistId: Long, type: String, name: String, category: String) =
        MediaItemEntity(
            id = id,
            playlistId = playlistId,
            name = name,
            url = "https://ornek.test/$id",
            category = category,
            type = type,
            addedAt = 1
        )
}
