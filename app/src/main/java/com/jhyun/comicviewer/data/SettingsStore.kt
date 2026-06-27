package com.jhyun.comicviewer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 사용자 설정 저장소. 리더 기본값/정렬을 영구 보존합니다. */
interface SettingsStore {
    val sortOrder: Flow<SortOrder>
    val readerDirection: Flow<String?>
    val readerLayout: Flow<String?>

    suspend fun setSortOrder(value: SortOrder)

    suspend fun setReaderDirection(name: String)

    suspend fun setReaderLayout(name: String)
}

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsStoreImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SettingsStore {
        private val keySort = stringPreferencesKey("sort_order")
        private val keyDirection = stringPreferencesKey("reader_direction")
        private val keyLayout = stringPreferencesKey("reader_layout")

        override val sortOrder: Flow<SortOrder> =
            context.dataStore.data.map { prefs ->
                prefs[keySort]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() } ?: SortOrder.NameAsc
            }

        override val readerDirection: Flow<String?> =
            context.dataStore.data.map { it[keyDirection] }

        override val readerLayout: Flow<String?> =
            context.dataStore.data.map { it[keyLayout] }

        override suspend fun setSortOrder(value: SortOrder) {
            context.dataStore.edit { it[keySort] = value.name }
        }

        override suspend fun setReaderDirection(name: String) {
            context.dataStore.edit { it[keyDirection] = name }
        }

        override suspend fun setReaderLayout(name: String) {
            context.dataStore.edit { it[keyLayout] = name }
        }
    }
