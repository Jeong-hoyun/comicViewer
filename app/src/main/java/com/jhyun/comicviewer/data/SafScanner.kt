package com.jhyun.comicviewer.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.jhyun.comicviewer.core.NaturalOrderComparator
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.FileInputStream
import javax.inject.Inject

/** zip/cbz 내부의 한 이미지 엔트리. Coil 커스텀 Fetcher 로 로드됩니다. */
data class ZipEntryRef(
    val zipUri: Uri,
    val entryName: String,
)

/** 리더가 표시할 한 페이지. model 은 Coil 로 로드 가능한 값(Uri=파일, ZipEntryRef=zip 내부). */
data class ImageDoc(
    val model: Any,
    val name: String,
)

/** 디렉토리 탐색 결과의 항목 한 개(하위 폴더 또는 zip/cbz 만화). */
data class FolderEntry(
    /** 폴더/아카이브의 document Uri. */
    val uri: Uri,
    val documentId: String,
    val name: String,
    /** 표지로 쓸 첫 이미지의 Coil model (없으면 null). */
    val cover: Any?,
    /** 안의 이미지 개수. */
    val imageCount: Int,
    /** 하위에 또 다른 폴더가 있는지. */
    val hasSubfolders: Boolean,
    /** zip/cbz 아카이브이면 true. */
    val isArchive: Boolean = false,
) {
    /** 이미지로 바로 열 수 있는 만화(이미지만 있는 폴더 또는 아카이브). */
    val isComic: Boolean get() = imageCount > 0 && !hasSubfolders
}

/** 특정 폴더의 직접 자식들(하위 폴더/아카이브 + 이미지). */
data class DirectoryListing(
    /** 하위 폴더와 zip/cbz 아카이브(둘 다 만화 항목으로 표시). */
    val subfolders: List<FolderEntry>,
    val images: List<ImageDoc>,
    val folderUri: Uri,
    val folderDocId: String,
)

/**
 * SAF tree Uri 하위를 훑어 이미지/폴더/아카이브 목록을 반환합니다.
 * zip/cbz 는 압축 해제 없이 ParcelFileDescriptor 로 랜덤 액세스(Commons Compress)합니다.
 */
class SafScanner
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "avif")
        private val archiveExtensions = setOf("zip", "cbz")
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

        /** parentDocId(미지정 시 root)의 직접 자식만 나열합니다. 하위 폴더/아카이브는 얕게 분류합니다. */
        fun listChildren(
            treeUri: Uri,
            parentDocId: String = DocumentsContract.getTreeDocumentId(treeUri),
        ): DirectoryListing {
            val subfolderIds = mutableListOf<Pair<String, String>>() // docId to name
            val archiveIds = mutableListOf<Pair<String, String>>() // docId to name
            val images = mutableListOf<ImageDoc>()

            queryChildren(treeUri, parentDocId) { docId, name, mime ->
                when {
                    mime == DocumentsContract.Document.MIME_TYPE_DIR -> subfolderIds.add(docId to name)
                    isArchive(name) -> archiveIds.add(docId to name)
                    isImage(name, mime) -> images.add(ImageDoc(buildDocUri(treeUri, docId), name))
                }
            }

            val folders = subfolderIds.map { (docId, name) -> inspectFolder(treeUri, docId, name) }
            val archives = archiveIds.map { (docId, name) -> inspectArchive(treeUri, docId, name) }

            return DirectoryListing(
                subfolders = (folders + archives).sortedWith(compareBy(naturalComparator) { it.name }),
                images = images.sortedWith(compareBy(naturalComparator) { it.name }),
                folderUri = buildDocUri(treeUri, parentDocId),
                folderDocId = parentDocId,
            )
        }

        /** zip/cbz 한 권의 이미지 페이지를 자연 정렬해 반환합니다. */
        fun listZipImages(zipUri: Uri): List<ImageDoc> =
            readZip(zipUri) { zip ->
                imageEntryNames(zip).map { entryName ->
                    ImageDoc(
                        model = ZipEntryRef(zipUri, entryName),
                        name = entryName.substringAfterLast('/'),
                    )
                }
            }

        /** 폴더 1개를 얕게 살펴 표지/이미지 수/하위폴더 유무를 계산합니다. */
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
                cover = coverId?.let { buildDocUri(treeUri, it) },
                imageCount = imageNames.size,
                hasSubfolders = hasSubfolders,
            )
        }

        /** zip/cbz 1개를 열어 표지/페이지 수를 계산합니다. 실패 시 빈 항목. */
        private fun inspectArchive(
            treeUri: Uri,
            docId: String,
            name: String,
        ): FolderEntry {
            val zipUri = buildDocUri(treeUri, docId)
            val names = runCatching { readZip(zipUri) { imageEntryNames(it) } }.getOrDefault(emptyList())
            return FolderEntry(
                uri = zipUri,
                documentId = docId,
                name = name,
                cover = names.firstOrNull()?.let { ZipEntryRef(zipUri, it) },
                imageCount = names.size,
                hasSubfolders = false,
                isArchive = true,
            )
        }

        /** zip 안의 이미지 엔트리 이름을 자연 정렬해 반환. */
        private fun imageEntryNames(zip: ZipFile): List<String> {
            val out = mutableListOf<String>()
            val entries = zip.entries
            while (entries.hasMoreElements()) {
                val entry: ZipArchiveEntry = entries.nextElement()
                if (!entry.isDirectory && isImage(entry.name, null)) out.add(entry.name)
            }
            return out.sortedWith(naturalComparator)
        }

        /** content Uri 의 zip 을 ParcelFileDescriptor 채널로 열어(랜덤 액세스) block 을 실행. */
        private fun <T> readZip(
            zipUri: Uri,
            block: (ZipFile) -> T,
        ): T {
            val pfd =
                context.contentResolver.openFileDescriptor(zipUri, "r")
                    ?: error("zip 을 열 수 없습니다: $zipUri")
            return pfd.use {
                FileInputStream(it.fileDescriptor).channel.use { channel ->
                    ZipFile.builder().setSeekableByteChannel(channel).get().use { zip ->
                        block(zip)
                    }
                }
            }
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
            return name.substringAfterLast('.', "").lowercase() in imageExtensions
        }

        private fun isArchive(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in archiveExtensions
    }
