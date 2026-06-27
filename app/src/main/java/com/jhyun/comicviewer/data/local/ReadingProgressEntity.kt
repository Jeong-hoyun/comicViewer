package com.jhyun.comicviewer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 만화 1권의 마지막으로 읽은 페이지(이어보기 / 히스토리). */
@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    /** 만화의 document Uri 문자열(식별자). */
    @PrimaryKey val comicUri: String,
    /** 소속 라이브러리 tree Uri (히스토리에서 다시 열 때 필요). */
    val treeUri: String,
    val docId: String,
    val name: String,
    val isArchive: Boolean,
    /** 0-based 마지막 페이지. */
    val lastPage: Int,
    val pageCount: Int,
    val updatedAt: Long,
)
