package com.jhyun.comicviewer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.request.Options
import com.jhyun.comicviewer.data.ZipEntryFetcher
import com.jhyun.comicviewer.data.ZipEntryRef
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ComicViewerApp :
    Application(),
    ImageLoaderFactory {
    // zip/cbz 내부 이미지 로드 + 표지 썸네일 메모리·디스크 캐시.
    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .components {
                add(ZipEntryFetcher.Factory())
                // ZipEntryRef 에 안정적인 캐시 키 부여(디스크 캐시 적중용).
                add(Keyer<ZipEntryRef> { data: ZipEntryRef, _: Options -> "${data.zipUri}#${data.entryName}" })
            }.memoryCache {
                MemoryCache.Builder(this).maxSizePercent(0.25).build()
            }.diskCache {
                DiskCache
                    .Builder()
                    .directory(cacheDir.resolve("cover_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100MB
                    .build()
            }.build()
}
