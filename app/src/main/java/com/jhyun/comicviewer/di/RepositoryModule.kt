package com.jhyun.comicviewer.di

import com.jhyun.comicviewer.data.LibraryRepository
import com.jhyun.comicviewer.data.LibraryRepositoryImpl
import com.jhyun.comicviewer.data.SettingsStore
import com.jhyun.comicviewer.data.SettingsStoreImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsStore(impl: SettingsStoreImpl): SettingsStore
}
