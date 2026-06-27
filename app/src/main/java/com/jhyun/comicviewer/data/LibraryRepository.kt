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

@Singleton
class LibraryRepository @Inject constructor(
    private val dao: SourceFolderDao,
    private val scanner: SafScanner,
    @ApplicationContext private val context: Context,
) {
    val folders: Flow<List<SourceFolderEntity>> = dao.observeAll()

    /** SAF 로 고른 폴더의 읽기 권한을 영구 보존하고 DB 에 저장합니다. */
    suspend fun addFolder(treeUri: Uri) {
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
            )
        )
    }

    suspend fun removeFolder(folder: SourceFolderEntity) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(folder.uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        dao.delete(folder)
    }

    suspend fun scanFolder(uri: Uri): List<ImageDoc> = withContext(Dispatchers.IO) {
        scanner.listImages(uri)
    }
}
