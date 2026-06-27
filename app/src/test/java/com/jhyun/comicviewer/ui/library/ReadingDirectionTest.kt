package com.jhyun.comicviewer.ui.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReadingDirectionTest {
    @Test
    fun `좌측 계열만 RTL 이다`() {
        val rtl = ReadingDirection.entries.filter { it.isRtl }

        assertThat(rtl).containsExactly(
            ReadingDirection.Left,
            ReadingDirection.LeftVertical,
            ReadingDirection.SmoothLeft,
        )
    }

    @Test
    fun `수직 계열은 두 가지뿐이다`() {
        val vertical = ReadingDirection.entries.filter { it.isVertical }

        assertThat(vertical).containsExactly(
            ReadingDirection.RightVertical,
            ReadingDirection.LeftVertical,
        )
    }

    @Test
    fun `S 계열만 부드러운 스크롤이다`() {
        val smooth = ReadingDirection.entries.filter { it.isSmooth }

        assertThat(smooth).containsExactly(
            ReadingDirection.SmoothRight,
            ReadingDirection.SmoothLeft,
        )
    }

    @Test
    fun `우측 좌측만 페이지 스냅 모드다`() {
        val paged = ReadingDirection.entries.filter { it.isPaged }

        assertThat(paged).containsExactly(ReadingDirection.Right, ReadingDirection.Left)
    }

    @Test
    fun `한 방향은 수직과 부드러움을 동시에 갖지 않는다`() {
        ReadingDirection.entries.forEach {
            assertThat(it.isVertical && it.isSmooth).isFalse()
        }
    }
}
