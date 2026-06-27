package com.jhyun.comicviewer.util

import android.net.Uri
import com.jhyun.comicviewer.data.DirectoryListing
import com.jhyun.comicviewer.data.FolderEntry
import com.jhyun.comicviewer.data.ImageDoc
import com.jhyun.comicviewer.data.LibraryRepository
import com.jhyun.comicviewer.data.local.BookmarkEntity
import com.jhyun.comicviewer.data.local.ReadingProgressEntity
import com.jhyun.comicviewer.data.local.SourceFolderEntity
import kotlinx.coroutines.flow.MutableStateFlow

/** Compose UI 테스트용 인메모리 가짜 저장소. */
class FakeLibraryRepository : LibraryRepository {
    val foldersFlow = MutableStateFlow<List<SourceFolderEntity>>(emptyList())
    override val folders = foldersFlow

    var defaultListing: DirectoryListing =
        DirectoryListing(
            subfolders = emptyList(),
            images = emptyList(),
            folderUri = Uri.EMPTY,
            folderDocId = "root",
        )
    var pages: List<ImageDoc> = emptyList()

    override suspend fun addFolder(treeUri: Uri): String = "추가된 폴더"

    override suspend fun removeFolder(folder: SourceFolderEntity) {
        foldersFlow.value = foldersFlow.value - folder
    }

    override suspend fun listDirectory(
        treeUri: Uri,
        parentDocId: String?,
    ): DirectoryListing = defaultListing

    override suspend fun listPages(
        treeUri: Uri,
        entry: FolderEntry,
    ): List<ImageDoc> = pages

    val recentlyReadFlow = MutableStateFlow<List<ReadingProgressEntity>>(emptyList())
    override val recentlyRead = recentlyReadFlow

    var progressByUri: Map<String, Int> = emptyMap()

    override suspend fun getProgress(comicUri: String): Int? = progressByUri[comicUri]

    override suspend fun saveProgress(
        treeUri: Uri,
        entry: FolderEntry,
        page: Int,
        pageCount: Int,
    ) = Unit

    val bookmarksFlow = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    override val bookmarks = bookmarksFlow

    override suspend fun toggleBookmark(
        treeUri: Uri,
        entry: FolderEntry,
        page: Int,
    ): Boolean = true
}
