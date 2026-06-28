package com.jhyun.comicviewer.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.FileInputStream
import java.nio.channels.SeekableByteChannel

/**
 * 열린 zip 아카이브를 작은 LRU 로 캐시해 **중앙 디렉토리 재파싱을 없앱니다.**
 *
 * 7,000+ 페이지 zip 을 리더에서 빠르게 넘길 때, 매 페이지마다 zip 을 새로 열면
 * 엔트리 목록 파싱이 중첩되어 렌더가 끊기거나 빈 화면이 됩니다.
 * 이 캐시는 같은 zip 을 재사용하고, 추출은 클래스 락으로 직렬화(빠른 바이트 복사)합니다.
 * 비트맵 디코딩은 락 밖(호출자/Coil)에서 일어납니다.
 */
object ArchivePageCache {
    private const val MAX_OPEN = 3

    private class Handle(
        val pfd: ParcelFileDescriptor,
        val channel: SeekableByteChannel,
        val zip: ZipFile,
    ) {
        fun close() {
            runCatching { zip.close() }
            runCatching { channel.close() }
            runCatching { pfd.close() }
        }
    }

    private val lock = Any()

    private val cache =
        object : LinkedHashMap<String, Handle>(0, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Handle>): Boolean {
                val evict = size > MAX_OPEN
                if (evict) eldest.value.close()
                return evict
            }
        }

    private fun open(
        context: Context,
        uri: Uri,
    ): Handle {
        val pfd =
            context.contentResolver.openFileDescriptor(uri, "r")
                ?: error("아카이브를 열 수 없습니다: $uri")
        val channel = FileInputStream(pfd.fileDescriptor).channel
        val zip = ZipFile.builder().setSeekableByteChannel(channel).get()
        return Handle(pfd, channel, zip)
    }

    /** 캐시된(없으면 새로 연) ZipFile 로 block 실행. 추출은 직렬화됨. */
    fun <T> withZip(
        context: Context,
        uri: Uri,
        block: (ZipFile) -> T,
    ): T =
        synchronized(lock) {
            val handle = cache[uri.toString()] ?: open(context, uri).also { cache[uri.toString()] = it }
            block(handle.zip)
        }

    /** zip 내부 엔트리 1개의 바이트(이미지 디코딩 전). */
    fun readEntry(
        context: Context,
        uri: Uri,
        entryName: String,
    ): ByteArray =
        withZip(context, uri) { zip ->
            val entry = zip.getEntry(entryName) ?: error("zip 엔트리를 찾을 수 없습니다: $entryName")
            zip.getInputStream(entry).use { it.readBytes() }
        }
}
