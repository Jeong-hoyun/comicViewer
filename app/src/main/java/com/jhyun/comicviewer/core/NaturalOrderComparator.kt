package com.jhyun.comicviewer.core

/**
 * 파일명을 사람이 기대하는 순서로 정렬합니다.
 * 일반 문자열 정렬: 1, 10, 2, 3 ...
 * 자연 정렬:        1, 2, 3, 10 ...
 */
class NaturalOrderComparator : Comparator<String> {
    override fun compare(
        a: String,
        b: String,
    ): Int {
        var ia = 0
        var ib = 0
        while (ia < a.length && ib < b.length) {
            val ca = a[ia]
            val cb = b[ib]
            if (ca.isDigit() && cb.isDigit()) {
                // 숫자 덩어리끼리 수치 비교
                val startA = ia
                val startB = ib
                while (ia < a.length && a[ia].isDigit()) ia++
                while (ib < b.length && b[ib].isDigit()) ib++
                val numA = a.substring(startA, ia).trimStart('0').ifEmpty { "0" }
                val numB = b.substring(startB, ib).trimStart('0').ifEmpty { "0" }
                if (numA.length != numB.length) return numA.length - numB.length
                val cmp = numA.compareTo(numB)
                if (cmp != 0) return cmp
            } else {
                val cmp = ca.lowercaseChar().compareTo(cb.lowercaseChar())
                if (cmp != 0) return cmp
                ia++
                ib++
            }
        }
        return (a.length - ia) - (b.length - ib)
    }
}
