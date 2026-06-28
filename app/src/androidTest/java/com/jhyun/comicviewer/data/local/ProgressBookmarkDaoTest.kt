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
class ProgressBookmarkDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var progressDao: ReadingProgressDao
    private lateinit var bookmarkDao: BookmarkDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        progressDao = db.readingProgressDao()
        bookmarkDao = db.bookmarkDao()
    }

    @After
    fun tearDown() = db.close()

    private fun progress(
        uri: String,
        page: Int = 0,
    ) = ReadingProgressEntity(
        comicUri = uri,
        treeUri = "content://tree/root",
        docId = "doc",
        name = uri,
        isArchive = false,
        lastPage = page,
        pageCount = 10,
        updatedAt = page.toLong(),
    )

    private fun bookmark(
        uri: String,
        page: Int,
    ) = BookmarkEntity(
        comicUri = uri,
        treeUri = "content://tree/root",
        docId = "doc",
        name = uri,
        isArchive = false,
        page = page,
        createdAt = page.toLong(),
    )

    // ---- ReadingProgress ----

    @Test
    fun progress_upsert_get_delete() =
        runTest {
            progressDao.upsert(progress("content://u1", page = 5))
            assertThat(progressDao.get("content://u1")?.lastPage).isEqualTo(5)

            progressDao.delete("content://u1")
            assertThat(progressDao.get("content://u1")).isNull()
        }

    @Test
    fun progress_upsert_replacesSameUri() =
        runTest {
            progressDao.upsert(progress("content://u1", page = 1))
            progressDao.upsert(progress("content://u1", page = 7))

            val all = progressDao.observeAll().first()
            assertThat(all).hasSize(1)
            assertThat(all.first().lastPage).isEqualTo(7)
        }

    // ---- Bookmark ----

    @Test
    fun bookmark_insert_get_delete() =
        runTest {
            bookmarkDao.insert(bookmark("content://u1", page = 3))
            val saved = bookmarkDao.get("content://u1", 3)
            assertThat(saved).isNotNull()

            bookmarkDao.delete(saved!!)
            assertThat(bookmarkDao.get("content://u1", 3)).isNull()
        }

    @Test
    fun bookmark_uniquePerComicAndPage() =
        runTest {
            bookmarkDao.insert(bookmark("content://u1", page = 3))
            bookmarkDao.insert(bookmark("content://u1", page = 3)) // 같은 (uri, page) → 교체

            val all = bookmarkDao.observeAll().first()
            assertThat(all.count { it.comicUri == "content://u1" && it.page == 3 }).isEqualTo(1)
        }
}
