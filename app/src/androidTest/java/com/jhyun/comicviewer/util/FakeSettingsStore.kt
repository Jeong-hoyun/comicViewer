package com.jhyun.comicviewer.util

import com.jhyun.comicviewer.data.SettingsStore
import com.jhyun.comicviewer.data.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow

/** 인메모리 가짜 설정 저장소. */
class FakeSettingsStore : SettingsStore {
    private val sort = MutableStateFlow(SortOrder.NameAsc)
    private val direction = MutableStateFlow<String?>(null)
    private val layout = MutableStateFlow<String?>(null)

    override val sortOrder = sort
    override val readerDirection = direction
    override val readerLayout = layout

    override suspend fun setSortOrder(value: SortOrder) {
        sort.value = value
    }

    override suspend fun setReaderDirection(name: String) {
        direction.value = name
    }

    override suspend fun setReaderLayout(name: String) {
        layout.value = name
    }
}
