package com.jhyun.comicviewer.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceFolderDao {
    @Query("SELECT * FROM source_folders ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<SourceFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: SourceFolderEntity)

    @Delete
    suspend fun delete(folder: SourceFolderEntity)
}
