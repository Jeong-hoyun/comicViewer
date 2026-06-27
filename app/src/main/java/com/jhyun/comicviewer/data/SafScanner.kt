package com.jhyun.comicviewer.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.jhyun.comicviewer.core.NaturalOrderComparator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** SAF tree 안의 이미지 페이지 한 장. */
data class ImageDoc(
    val uri: Uri,
    val name: String,
)

/**
 * SAF tree Uri 하위를 재귀적으로 훑어 이미지 파일 목록을 반환합니다.
 * DocumentsContract 의 child-documents 쿼리를 폴더당 1회만 날려 N+1 을 피합니다.
 */
class SafScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")
    private val naturalComparator = NaturalOrderComparator()

    fun listImages(treeUri: Uri): List<ImageDoc> {
        val results = mutableListOf<ImageDoc>()
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)

        val queue = ArrayDeque<String>()
        queue.add(rootDocId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )

        while (queue.isNotEmpty()) {
            val parentDocId = queue.removeFirst()
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (c.moveToNext()) {
                    val docId = c.getString(idIdx)
                    val name = c.getString(nameIdx) ?: continue
                    val mime = c.getString(mimeIdx)

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        queue.add(docId)
                    } else if (isImage(name, mime)) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        results.add(ImageDoc(docUri, name))
                    }
                }
            }
        }

        return results.sortedWith(compareBy(naturalComparator) { it.name })
    }

    private fun isImage(name: String, mime: String?): Boolean {
        if (mime != null && mime.startsWith("image/")) return true
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in imageExtensions
    }
}
