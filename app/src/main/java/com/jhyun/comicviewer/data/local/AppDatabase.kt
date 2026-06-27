package com.jhyun.comicviewer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SourceFolderEntity::class, ReadingProgressEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceFolderDao(): SourceFolderDao

    abstract fun readingProgressDao(): ReadingProgressDao
}
