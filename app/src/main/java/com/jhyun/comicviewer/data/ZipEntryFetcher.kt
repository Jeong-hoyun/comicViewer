package com.jhyun.comicviewer.data

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.FileInputStream

/**
 * Coil 이 [ZipEntryRef] 를 만나면 zip 내부 엔트리를 읽어 디코딩합니다.
 * ParcelFileDescriptor 채널로 zip 을 랜덤 액세스하여 해당 엔트리만 메모리로 읽습니다.
 */
class ZipEntryFetcher(
    private val ref: ZipEntryRef,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val resolver = options.context.contentResolver
        val bytes =
            resolver
                .openFileDescriptor(ref.zipUri, "r")
                ?.use { pfd ->
                    FileInputStream(pfd.fileDescriptor).channel.use { channel ->
                        ZipFile.builder().setSeekableByteChannel(channel).get().use { zip ->
                            val entry =
                                zip.getEntry(ref.entryName)
                                    ?: error("zip 엔트리를 찾을 수 없습니다: ${ref.entryName}")
                            zip.getInputStream(entry).use { it.readBytes() }
                        }
                    }
                } ?: error("zip 을 열 수 없습니다: ${ref.zipUri}")

        return SourceResult(
            source = ImageSource(Buffer().apply { write(bytes) }, options.context),
            mimeType = null,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<ZipEntryRef> {
        override fun create(
            data: ZipEntryRef,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = ZipEntryFetcher(data, options)
    }
}
