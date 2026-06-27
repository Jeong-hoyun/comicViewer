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

/** 디렉토리 탐색 결과의 하위 폴더 한 개. */
data class FolderEntry(
    /** 이 폴더의 document Uri (다시 탐색하거나 페이지를 스캔할 때 사용). */
    val uri: Uri,
    /** 이 폴더의 document id (하위 children 쿼리에 사용). */
    val documentId: String,
    val name: String,
    /** 표지로 쓸 첫 이미지 (이미지가 없으면 null). */
    val coverUri: Uri?,
    /** 폴더 안 이미지 개수(직접 자식 기준). */
    val imageCount: Int,
    /** 하위에 또 다른 폴더가 있는지. */
    val hasSubfolders: Boolean,
) {
    /** "이미지만 있는" 폴더 = 미리보기로 바로 열 수 있는 만화. */
    val isComic: Boolean get() = imageCount > 0 && !hasSubfolders
}

/** 특정 폴더의 직접 자식들(하위 폴더 + 이미지). */
data class DirectoryListing(
    val subfolders: List<FolderEntry>,
    val images: List<ImageDoc>,
    /** 나열 대상 폴더(=현재 폴더) 자체의 document Uri/ID. selfComic 판정에 사용. */
    val folderUri: Uri,
    val folderDocId: String,
)

/**
 * SAF tree Uri 하위를 훑어 이미지/폴더 목록을 반환합니다.
 * DocumentsContract 의 child-documents 쿼리를 폴더당 1회만 날려 N+1 을 피합니다.
 */
class SafScanner
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")
        private val naturalComparator = NaturalOrderComparator()

        private val projection =
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )

        /** treeUri 전체(재귀)의 이미지 목록. startDocId 를 주면 그 하위만. */
        fun listImages(
            treeUri: Uri,
            startDocId: String = DocumentsContract.getTreeDocumentId(treeUri),
        ): List<ImageDoc> {
            val results = mutableListOf<ImageDoc>()
            val queue = ArrayDeque<String>()
            queue.add(startDocId)

            while (queue.isNotEmpty()) {
                val parentDocId = queue.removeFirst()
                queryChildren(treeUri, parentDocId) { docId, name, mime ->
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        queue.add(docId)
                    } else if (isImage(name, mime)) {
                        results.add(ImageDoc(buildDocUri(treeUri, docId), name))
                    }
                }
            }
            return results.sortedWith(compareBy(naturalComparator) { it.name })
        }

        /** parentDocId(미지정 시 root)의 직접 자식만 나열합니다. 하위 폴더는 얕게 분류합니다. */
        fun listChildren(
            treeUri: Uri,
            parentDocId: String = DocumentsContract.getTreeDocumentId(treeUri),
        ): DirectoryListing {
            val subfolderIds = mutableListOf<Pair<String, String>>() // docId to name
            val images = mutableListOf<ImageDoc>()

            queryChildren(treeUri, parentDocId) { docId, name, mime ->
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    subfolderIds.add(docId to name)
                } else if (isImage(name, mime)) {
                    images.add(ImageDoc(buildDocUri(treeUri, docId), name))
                }
            }

            val subfolders =
                subfolderIds
                    .map { (docId, name) -> inspectFolder(treeUri, docId, name) }
                    .sortedWith(compareBy(naturalComparator) { it.name })

            return DirectoryListing(
                subfolders = subfolders,
                images = images.sortedWith(compareBy(naturalComparator) { it.name }),
                folderUri = buildDocUri(treeUri, parentDocId),
                folderDocId = parentDocId,
            )
        }

        /** 폴더 1개를 얕게(직접 자식만) 살펴 표지/이미지 수/하위폴더 유무를 계산합니다. */
        private fun inspectFolder(
            treeUri: Uri,
            docId: String,
            name: String,
        ): FolderEntry {
            val imageNames = mutableListOf<Pair<String, String>>() // imgDocId to name
            var hasSubfolders = false

            queryChildren(treeUri, docId) { childId, childName, mime ->
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    hasSubfolders = true
                } else if (isImage(childName, mime)) {
                    imageNames.add(childId to childName)
                }
            }

            val coverId =
                imageNames
                    .sortedWith(compareBy(naturalComparator) { it.second })
                    .firstOrNull()
                    ?.first

            return FolderEntry(
                uri = buildDocUri(treeUri, docId),
                documentId = docId,
                name = name,
                coverUri = coverId?.let { buildDocUri(treeUri, it) },
                imageCount = imageNames.size,
                hasSubfolders = hasSubfolders,
            )
        }

        private inline fun queryChildren(
            treeUri: Uri,
            parentDocId: String,
            onRow: (docId: String, name: String, mime: String?) -> Unit,
        ) {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (c.moveToNext()) {
                    val name = c.getString(nameIdx) ?: continue
                    onRow(c.getString(idIdx), name, c.getString(mimeIdx))
                }
            }
        }

        private fun buildDocUri(
            treeUri: Uri,
            docId: String,
        ): Uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

        private fun isImage(
            name: String,
            mime: String?,
        ): Boolean {
            if (mime != null && mime.startsWith("image/")) return true
            val ext = name.substringAfterLast('.', "").lowercase()
            return ext in imageExtensions
        }
    }
