package com.jhyun.comicviewer.di

import android.content.Context
import androidx.room.Room
import com.jhyun.comicviewer.data.local.AppDatabase
import com.jhyun.comicviewer.data.local.BookmarkDao
import com.jhyun.comicviewer.data.local.ReadingProgressDao
import com.jhyun.comicviewer.data.local.SourceFolderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, "comicviewer.db")
            // PoC 단계: 스키마 변경 시 마이그레이션 대신 재생성(데이터 초기화 허용).
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideSourceFolderDao(db: AppDatabase): SourceFolderDao = db.sourceFolderDao()

    @Provides
    fun provideReadingProgressDao(db: AppDatabase): ReadingProgressDao = db.readingProgressDao()

    @Provides
    fun provideBookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()
}
