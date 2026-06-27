package com.jhyun.comicviewer.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SourceFolderDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: SourceFolderDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .build()
        dao = db.sourceFolderDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun folder(
        uri: String,
        name: String = uri,
        addedAt: Long = 0L,
    ) = SourceFolderEntity(uri = uri, displayName = name, addedAt = addedAt)

    @Test
    fun upsert_then_observeAll_returnsInserted() =
        runTest {
            dao.upsert(folder("content://u1", "폴더1", 100))

            val all = dao.observeAll().first()

            assertThat(all).hasSize(1)
            assertThat(all.first().displayName).isEqualTo("폴더1")
        }

    @Test
    fun observeAll_ordersByAddedAtDescending() =
        runTest {
            dao.upsert(folder("content://a", "A", addedAt = 100))
            dao.upsert(folder("content://c", "C", addedAt = 300))
            dao.upsert(folder("content://b", "B", addedAt = 200))

            val names = dao.observeAll().first().map { it.displayName }

            assertThat(names).containsExactly("C", "B", "A").inOrder()
        }

    @Test
    fun upsert_withSameUri_replacesRow() =
        runTest {
            dao.upsert(folder("content://u1", "이전 이름", addedAt = 1))
            dao.upsert(folder("content://u1", "새 이름", addedAt = 2))

            val all = dao.observeAll().first()

            assertThat(all).hasSize(1)
            assertThat(all.first().displayName).isEqualTo("새 이름")
            assertThat(all.first().addedAt).isEqualTo(2)
        }

    @Test
    fun delete_removesRow() =
        runTest {
            val target = folder("content://u1", "삭제 대상", addedAt = 1)
            dao.upsert(target)
            dao.upsert(folder("content://u2", "유지", addedAt = 2))

            dao.delete(target)

            val all = dao.observeAll().first()
            assertThat(all).hasSize(1)
            assertThat(all.first().displayName).isEqualTo("유지")
        }
}
