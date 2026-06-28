package com.jhyun.comicviewer.data

import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer

/**
 * Coil 이 [ZipEntryRef] 를 만나면 zip 내부 엔트리를 읽어 디코딩합니다.
 * 매 페이지마다 zip 을 새로 열지 않도록 [ArchivePageCache] 로 열린 아카이브를 재사용합니다
 * (7,000+ 페이지 zip 빠른 넘김 시 빈 화면 방지).
 */
class ZipEntryFetcher(
    private val ref: ZipEntryRef,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val bytes = ArchivePageCache.readEntry(options.context, ref.zipUri, ref.entryName)

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
