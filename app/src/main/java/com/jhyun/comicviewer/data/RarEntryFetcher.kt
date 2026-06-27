package com.jhyun.comicviewer.data

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import com.github.junrar.Archive
import okio.Buffer

/**
 * Coil 이 [RarEntryRef] 를 만나면 cbr/rar 내부 엔트리를 추출해 디코딩합니다.
 * (rar 는 랜덤 액세스가 어려워 매 요청마다 스트림을 새로 열어 해당 엔트리만 추출합니다.)
 */
class RarEntryFetcher(
    private val ref: RarEntryRef,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val resolver = options.context.contentResolver
        val bytes =
            resolver.openInputStream(ref.rarUri)?.use { input ->
                Archive(input).use { archive ->
                    val header =
                        archive.fileHeaders.firstOrNull { it.fileName == ref.entryName }
                            ?: error("rar 엔트리를 찾을 수 없습니다: ${ref.entryName}")
                    archive.getInputStream(header).use { it.readBytes() }
                }
            } ?: error("cbr 을 열 수 없습니다: ${ref.rarUri}")

        return SourceResult(
            source = ImageSource(Buffer().apply { write(bytes) }, options.context),
            mimeType = null,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<RarEntryRef> {
        override fun create(
            data: RarEntryRef,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = RarEntryFetcher(data, options)
    }
}
