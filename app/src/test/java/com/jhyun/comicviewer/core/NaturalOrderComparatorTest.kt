package com.jhyun.comicviewer.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NaturalOrderComparatorTest {
    private val comparator = NaturalOrderComparator()

    @Test
    fun `숫자를 사전식이 아닌 자연 순서로 정렬한다`() {
        val input = listOf("page_10.png", "page_1.png", "page_3.png", "page_2.png")

        val sorted = input.sortedWith(comparator)

        assertThat(sorted)
            .containsExactly("page_1.png", "page_2.png", "page_3.png", "page_10.png")
            .inOrder()
    }

    @Test
    fun `여러 자리수 경계를 올바르게 비교한다`() {
        val input = listOf("9", "10", "100", "2", "1")

        val sorted = input.sortedWith(comparator)

        assertThat(sorted).containsExactly("1", "2", "9", "10", "100").inOrder()
    }

    @Test
    fun `대소문자를 구분하지 않고 정렬한다`() {
        val input = listOf("B.png", "a.png", "C.png")

        val sorted = input.sortedWith(comparator)

        assertThat(sorted).containsExactly("a.png", "B.png", "C.png").inOrder()
    }

    @Test
    fun `선행 0이 있어도 수치로 비교한다`() {
        val input = listOf("img007", "img8", "img06")

        val sorted = input.sortedWith(comparator)

        assertThat(sorted).containsExactly("img06", "img007", "img8").inOrder()
    }
}
