package com.jhyun.comicviewer.data

/** 디렉토리 항목 정렬 기준. */
enum class SortOrder(
    val label: String,
) {
    NameAsc("이름 ↑"),
    NameDesc("이름 ↓"),
    DateAsc("날짜 ↑"),
    DateDesc("날짜 ↓"),
}
