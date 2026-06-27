package com.jhyun.comicviewer.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 특정 만화의 특정 페이지 책갈피. (comicUri, page) 는 유일. */
@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["comicUri", "page"], unique = true)],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val comicUri: String,
    val treeUri: String,
    val docId: String,
    val name: String,
    val isArchive: Boolean,
    /** 0-based 페이지. */
    val page: Int,
    val createdAt: Long,
)
