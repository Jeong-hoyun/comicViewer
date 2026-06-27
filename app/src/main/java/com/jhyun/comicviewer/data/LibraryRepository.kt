package com.jhyun.comicviewer.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
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

    /** 특정 하위 폴더(docId)의 이미지 페이지를 재귀로 스캔합니다. */
    suspend fun listPages(
        treeUri: Uri,
        docId: String,
    ): List<ImageDoc>
}

@Singleton
class LibraryRepositoryImpl
    @Inject
    constructor(
        private val dao: SourceFolderDao,
        private val scanner: SafScanner,
        @ApplicationContext private val context: Context,
    ) : LibraryRepository {
        override val folders: Flow<List<SourceFolderEntity>> = dao.observeAll()

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
            docId: String,
        ): List<ImageDoc> =
            withContext(Dispatchers.IO) {
                scanner.listImages(treeUri, docId)
            }
    }
