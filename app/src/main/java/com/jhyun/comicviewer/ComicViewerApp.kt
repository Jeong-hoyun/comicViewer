package com.jhyun.comicviewer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.jhyun.comicviewer.data.ZipEntryFetcher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ComicViewerApp :
    Application(),
    ImageLoaderFactory {
    // zip/cbz 내부 이미지를 Coil 로 로드할 수 있도록 커스텀 Fetcher 등록.
    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .components { add(ZipEntryFetcher.Factory()) }
            .build()
}
