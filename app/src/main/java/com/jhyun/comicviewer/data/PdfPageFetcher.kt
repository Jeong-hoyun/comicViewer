package com.jhyun.comicviewer.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfRenderer
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension

/** PdfRenderer 는 스레드 안전하지 않으므로 전역 락으로 직렬화. */
private val PdfRenderLock = Any()

/**
 * Coil 이 [PdfPageRef] 를 만나면 PdfRenderer 로 해당 페이지를 비트맵으로 렌더합니다.
 */
class PdfPageFetcher(
    private val ref: PdfPageRef,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val resolver = options.context.contentResolver
        val targetWidth = (options.size.width as? Dimension.Pixels)?.px ?: 0

        val bitmap =
            synchronized(PdfRenderLock) {
                resolver.openFileDescriptor(ref.pdfUri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        renderer.openPage(ref.pageIndex).use { page ->
                            val w = if (targetWidth in 1..4000) targetWidth else page.width * 2
                            val scale = w.toFloat() / page.width
                            val h = (page.height * scale).toInt().coerceAtLeast(1)
                            Bitmap.createBitmap(w.coerceAtLeast(1), h, Bitmap.Config.ARGB_8888).also { bmp ->
                                bmp.eraseColor(Color.WHITE) // PDF 투명 배경 → 흰색
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                        }
                    }
                } ?: error("pdf 를 열 수 없습니다: ${ref.pdfUri}")
            }

        return DrawableResult(
            drawable = BitmapDrawable(options.context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    class Factory : Fetcher.Factory<PdfPageRef> {
        override fun create(
            data: PdfPageRef,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = PdfPageFetcher(data, options)
    }
}
