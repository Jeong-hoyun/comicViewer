package com.jhyun.comicviewer.util

import android.net.Uri
import com.jhyun.comicviewer.data.DirectoryListing
import com.jhyun.comicviewer.data.FolderEntry
import com.jhyun.comicviewer.data.ImageDoc
import com.jhyun.comicviewer.data.LibraryRepository
import com.jhyun.comicviewer.data.local.ReadingProgressEntity
import com.jhyun.comicviewer.data.local.SourceFolderEntity
import kotlinx.coroutines.flow.MutableStateFlow

/** ViewModel 테스트용 인메모리 가짜 저장소. */
class FakeLibraryRepository : LibraryRepository {
    val foldersFlow = MutableStateFlow<List<SourceFolderEntity>>(emptyList())
    override val folders = foldersFlow

    /** listDirectory 가 반환할 값. 테스트에서 docId 별로 지정 가능. */
    var listings: Map<String?, DirectoryListing> = emptyMap()
    var defaultListing: DirectoryListing =
        DirectoryListing(
            subfolders = emptyList(),
            images = emptyList(),
            folderUri = Uri.EMPTY,
            folderDocId = "root",
        )
    var pages: List<ImageDoc> = emptyList()

    val addedUris = mutableListOf<Uri>()
    val removed = mutableListOf<SourceFolderEntity>()
    var nextName = "추가된 폴더"

    override suspend fun addFolder(treeUri: Uri): String {
        addedUris.add(treeUri)
        return nextName
    }

    override suspend fun removeFolder(folder: SourceFolderEntity) {
        removed.add(folder)
        foldersFlow.value = foldersFlow.value - folder
    }

    override suspend fun listDirectory(
        treeUri: Uri,
        parentDocId: String?,
    ): DirectoryListing = listings[parentDocId] ?: defaultListing

    override suspend fun listPages(
        treeUri: Uri,
        entry: FolderEntry,
    ): List<ImageDoc> = pages

    val recentlyReadFlow = MutableStateFlow<List<ReadingProgressEntity>>(emptyList())
    override val recentlyRead = recentlyReadFlow

    /** comicUri → 저장된 페이지. 테스트에서 지정 가능. */
    var progressByUri: Map<String, Int> = emptyMap()
    val savedProgress = mutableListOf<ReadingProgressEntity>()

    override suspend fun getProgress(comicUri: String): Int? = progressByUri[comicUri]

    override suspend fun saveProgress(
        treeUri: Uri,
        entry: FolderEntry,
        page: Int,
        pageCount: Int,
    ) {
        savedProgress.add(
            ReadingProgressEntity(
                comicUri = entry.uri.toString(),
                treeUri = treeUri.toString(),
                docId = entry.documentId,
                name = entry.name,
                isArchive = entry.isArchive,
                lastPage = page,
                pageCount = pageCount,
                updatedAt = 0L,
            ),
        )
    }
}
