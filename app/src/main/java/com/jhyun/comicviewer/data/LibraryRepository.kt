package com.jhyun.comicviewer.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.jhyun.comicviewer.data.local.BookmarkDao
import com.jhyun.comicviewer.data.local.BookmarkEntity
import com.jhyun.comicviewer.data.local.ReadingProgressDao
import com.jhyun.comicviewer.data.local.ReadingProgressEntity
import com.jhyun.comicviewer.data.local.SourceFolderDao
import com.jhyun.comicviewer.data.local.SourceFolderEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 라이브러리(SAF 폴더 + 탐색/스캔) 저장소.
 * ViewModel 테스트에서 Fake 로 교체할 수 있도록 인터페이스로 분리합니다.
 */
interface LibraryRepository {
    val folders: Flow<List<SourceFolderEntity>>

    /** SAF 로 고른 폴더의 읽기 권한을 영구 보존하고 DB 에 저장합니다. displayName 을 반환합니다. */
    suspend fun addFolder(treeUri: Uri): String

    suspend fun removeFolder(folder: SourceFolderEntity)

    /** treeUri 안에서 parentDocId(미지정 시 root) 의 직접 자식(하위 폴더 + 이미지)을 나열합니다. */
    suspend fun listDirectory(
        treeUri: Uri,
        parentDocId: String?,
    ): DirectoryListing

    /** 만화 1권(폴더 또는 zip/cbz)의 이미지 페이지를 반환합니다. */
    suspend fun listPages(
        treeUri: Uri,
        entry: FolderEntry,
    ): List<ImageDoc>

    /** 최근 읽은 만화(이어보기/히스토리), 최신순. */
    val recentlyRead: Flow<List<ReadingProgressEntity>>

    /** 해당 만화의 마지막으로 읽은 페이지(없으면 null). */
    suspend fun getProgress(comicUri: String): Int?

    /** 읽기 진행도를 저장(upsert)합니다. */
    suspend fun saveProgress(
        treeUri: Uri,
        entry: FolderEntry,
        page: Int,
        pageCount: Int,
    )

    /** 모든 책갈피, 최신순. */
    val bookmarks: Flow<List<BookmarkEntity>>

    /** 해당 페이지 책갈피를 토글하고, 토글 후 책갈피 상태(true=추가됨)를 반환합니다. */
    suspend fun toggleBookmark(
        treeUri: Uri,
        entry: FolderEntry,
        page: Int,
    ): Boolean
}

@Singleton
class LibraryRepositoryImpl
    @Inject
    constructor(
        private val dao: SourceFolderDao,
        private val progressDao: ReadingProgressDao,
        private val bookmarkDao: BookmarkDao,
        private val scanner: SafScanner,
        @ApplicationContext private val context: Context,
    ) : LibraryRepository {
        override val folders: Flow<List<SourceFolderEntity>> = dao.observeAll()

        override val recentlyRead: Flow<List<ReadingProgressEntity>> = progressDao.observeAll()

        override val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.observeAll()

        override suspend fun addFolder(treeUri: Uri): String {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            val name = DocumentFile.fromTreeUri(context, treeUri)?.name ?: treeUri.toString()
            dao.upsert(
                SourceFolderEntity(
                    uri = treeUri.toString(),
                    displayName = name,
                    addedAt = System.currentTimeMillis(),
                ),
            )
            return name
        }

        override suspend fun removeFolder(folder: SourceFolderEntity) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(folder.uri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            dao.delete(folder)
        }

        override suspend fun listDirectory(
            treeUri: Uri,
            parentDocId: String?,
        ): DirectoryListing =
            withContext(Dispatchers.IO) {
                if (parentDocId == null) {
                    scanner.listChildren(treeUri)
                } else {
                    scanner.listChildren(treeUri, parentDocId)
                }
            }

        override suspend fun listPages(
            treeUri: Uri,
            entry: FolderEntry,
        ): List<ImageDoc> =
            withContext(Dispatchers.IO) {
                if (entry.isArchive) {
                    scanner.listZipImages(entry.uri)
                } else {
                    scanner.listImagesDirect(treeUri, entry.documentId)
                }
            }

        override suspend fun getProgress(comicUri: String): Int? = progressDao.get(comicUri)?.lastPage

        override suspend fun saveProgress(
            treeUri: Uri,
            entry: FolderEntry,
            page: Int,
            pageCount: Int,
        ) {
            progressDao.upsert(
                ReadingProgressEntity(
                    comicUri = entry.uri.toString(),
                    treeUri = treeUri.toString(),
                    docId = entry.documentId,
                    name = entry.name,
                    isArchive = entry.isArchive,
                    lastPage = page,
                    pageCount = pageCount,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun toggleBookmark(
            treeUri: Uri,
            entry: FolderEntry,
            page: Int,
        ): Boolean {
            val existing = bookmarkDao.get(entry.uri.toString(), page)
            return if (existing != null) {
                bookmarkDao.delete(existing)
                false
            } else {
                bookmarkDao.insert(
                    BookmarkEntity(
                        comicUri = entry.uri.toString(),
                        treeUri = treeUri.toString(),
                        docId = entry.documentId,
                        name = entry.name,
                        isArchive = entry.isArchive,
                        page = page,
                        createdAt = System.currentTimeMillis(),
                    ),
                )
                true
            }
        }
    }
