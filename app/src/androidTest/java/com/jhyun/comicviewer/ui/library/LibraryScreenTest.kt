package com.jhyun.comicviewer.ui.library

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jhyun.comicviewer.data.DirectoryListing
import com.jhyun.comicviewer.data.FolderEntry
import com.jhyun.comicviewer.data.local.SourceFolderEntity
import com.jhyun.comicviewer.ui.theme.ComicViewerTheme
import com.jhyun.comicviewer.util.FakeLibraryRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val repo = FakeLibraryRepository()

    private fun setScreen() {
        composeRule.setContent {
            ComicViewerTheme(darkTheme = true) {
                val viewModel = remember { LibraryViewModel(repo) }
                LibraryScreen(viewModel = viewModel)
            }
        }
    }

    private fun comic(
        name: String,
        docId: String = name,
        pages: Int = 4,
    ) = FolderEntry(
        uri = Uri.parse("content://f/$docId"),
        documentId = docId,
        name = name,
        cover = null, // 표지 로딩 없이 아이콘만 → 결정적 테스트
        imageCount = pages,
        hasSubfolders = false,
    )

    @Test
    fun `스토리지 탭에 Recent 와 라이브러리 폴더가 표시된다`() {
        repo.foldersFlow.value = listOf(SourceFolderEntity("content://tree/x", "MyLib", 1L))

        setScreen()

        composeRule.onNodeWithText("Recent").assertIsDisplayed()
        composeRule.onNodeWithText("라이브러리 폴더").assertIsDisplayed()
    }

    @Test
    fun `히스토리 탭은 플레이스홀더를 보여준다`() {
        setScreen()

        composeRule.onNodeWithText("히스토리").performClick()

        composeRule.onNodeWithText("히스토리가 비어 있어요.").assertIsDisplayed()
    }

    @Test
    fun `폴더를 열면 만화 목록이 보이고 탭하면 미리보기 다이얼로그가 뜬다`() {
        repo.foldersFlow.value = listOf(SourceFolderEntity("content://tree/x", "MyLib", 1L))
        repo.defaultListing =
            DirectoryListing(
                subfolders = listOf(comic("SeriesA", docId = "sa", pages = 4)),
                images = emptyList(),
                folderUri = Uri.parse("content://tree/x"),
                folderDocId = "root",
            )

        setScreen()

        // 스토리지 → Recent 탭하면 디렉토리로 이동하며 라이브러리 로드
        composeRule.onNodeWithText("Recent").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("SeriesA").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("SeriesA").assertIsDisplayed()

        // 만화 항목 탭 → 미리보기 다이얼로그
        composeRule.onNodeWithText("SeriesA").performClick()
        composeRule.onNodeWithText("첫페이지보기").assertIsDisplayed()
        composeRule.onNodeWithText("총 4페이지").assertIsDisplayed()
    }
}
