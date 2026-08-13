package com.sabriusta.tv.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.sabriusta.tv.data.local.AppDatabase
import com.sabriusta.tv.data.local.CategoryDao
import com.sabriusta.tv.data.local.CustomSourceDao
import com.sabriusta.tv.data.local.FavoriteDao
import com.sabriusta.tv.data.local.MediaItemDao
import com.sabriusta.tv.data.local.PlaybackProgressDao
import com.sabriusta.tv.data.local.PlaylistDao
import com.sabriusta.tv.data.local.RadioHistoryDao
import com.sabriusta.tv.data.local.WatchHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()
    @Provides fun provideMediaItemDao(db: AppDatabase): MediaItemDao = db.mediaItemDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideFavoriteDao(db: AppDatabase): FavoriteDao = db.favoriteDao()
    @Provides fun provideWatchHistoryDao(db: AppDatabase): WatchHistoryDao = db.watchHistoryDao()
    @Provides fun provideProgressDao(db: AppDatabase): PlaybackProgressDao = db.playbackProgressDao()
    @Provides fun provideRadioHistoryDao(db: AppDatabase): RadioHistoryDao = db.radioHistoryDao()
    @Provides fun provideCustomSourceDao(db: AppDatabase): CustomSourceDao = db.customSourceDao()

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
}
