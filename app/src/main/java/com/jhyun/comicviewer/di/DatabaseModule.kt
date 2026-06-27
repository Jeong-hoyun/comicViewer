package com.jhyun.comicviewer.di

import android.content.Context
import androidx.room.Room
import com.jhyun.comicviewer.data.local.AppDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "comicviewer.db")
            .build()

    @Provides
    fun provideSourceFolderDao(db: AppDatabase): SourceFolderDao = db.sourceFolderDao()
}
