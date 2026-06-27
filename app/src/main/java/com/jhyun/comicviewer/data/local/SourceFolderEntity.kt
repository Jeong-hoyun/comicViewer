package com.jhyun.comicviewer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 사용자가 SAF 로 추가한 라이브러리 폴더 (tree Uri 단위). */
@Entity(tableName = "source_folders")
data class SourceFolderEntity(
    @PrimaryKey val uri: String,
    val displayName: String,
    val addedAt: Long,
)
