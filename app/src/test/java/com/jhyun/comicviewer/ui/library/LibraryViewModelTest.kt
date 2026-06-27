package com.jhyun.comicviewer.ui.library

import android.app.Application
import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jhyun.comicviewer.data.DirectoryListing
import com.jhyun.comicviewer.data.FolderEntry
import com.jhyun.comicviewer.data.ImageDoc
import com.jhyun.comicviewer.data.local.BookmarkEntity
import com.jhyun.comicviewer.util.FakeLibraryRepository
import com.jhyun.comicviewer.util.FakeSettingsStore
import com.jhyun.comicviewer.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, manifest = Config.NONE)
class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo = FakeLibraryRepository()
    private val settings = FakeSettingsStore()

    private fun viewModel() = LibraryViewModel(repo, settings)

    private fun uri(value: String) = Uri.parse(value)

    private fun image(name: String) = ImageDoc(uri("content://img/$name"), name)

    // ImageDoc(model, name) — model 위치 인자로 Uri 전달.

    private fun folder(
        name: String,
        docId: String = name,
        images: Int = 1,
        sub: Boolean = false,
    ) = FolderEntry(
        uri = uri("content://f/$docId"),
        documentId = docId,
        name = name,
        cover = uri("content://c/$docId"),
        imageCount = images,
        hasSubfolders = sub,
    )

    private fun listing(
        subfolders: List<FolderEntry> = emptyList(),
        images: List<ImageDoc> = emptyList(),
        docId: String = "root",
    ) = DirectoryListing(subfolders, images, uri("content://tree/$docId"), docId)

    @Test
    fun `초기 디렉토리 상태는 Empty`() {
        assertThat(viewModel().directory.value).isEqualTo(DirectoryUiState.Empty)
    }

    @Test
    fun `openLibrary 는 Loaded 상태를 만든다`() {
        repo.defaultListing = listing(subfolders = listOf(folder("A"), folder("B")))
        val vm = viewModel()

        vm.openLibrary(uri("content://tree/root"), "MangaLib")

        val state = vm.directory.value as DirectoryUiState.Loaded
        assertThat(state.title).isEqualTo("MangaLib")
        assertThat(state.subfolders).hasSize(2)
        assertThat(state.canGoUp).isFalse()
        assertThat(state.selfComic).isNull()
    }

    @Test
    fun `browseInto 면 canGoUp, browseUp 이면 원래 폴더로 복귀`() {
        repo.defaultListing = listing(subfolders = listOf(folder("A")))
        val vm = viewModel()
        vm.openLibrary(uri("content://tree/root"), "MangaLib")

        vm.browseInto(folder("Sub", docId = "sub"))
        val inSub = vm.directory.value as DirectoryUiState.Loaded
        assertThat(inSub.canGoUp).isTrue()
        assertThat(inSub.title).isEqualTo("Sub")

        vm.browseUp()
        val back = vm.directory.value as DirectoryUiState.Loaded
        assertThat(back.canGoUp).isFalse()
        assertThat(back.title).isEqualTo("MangaLib")
    }

    @Test
    fun `이미지만 있으면 현재 폴더를 selfComic 으로 만든다`() {
        repo.defaultListing = listing(images = listOf(image("1"), image("2")))
        val vm = viewModel()

        vm.openLibrary(uri("content://tree/root"), "SeriesA")

        val state = vm.directory.value as DirectoryUiState.Loaded
        assertThat(state.selfComic).isNotNull()
        assertThat(state.selfComic!!.imageCount).isEqualTo(2)
        assertThat(state.selfComic!!.name).isEqualTo("SeriesA")
    }

    @Test
    fun `이미지와 하위 폴더가 섞여 있어도 이미지가 있으면 selfComic 과 하위 폴더 모두 표시`() {
        repo.defaultListing = listing(subfolders = listOf(folder("A")), images = listOf(image("1")))
        val vm = viewModel()

        vm.openLibrary(uri("content://tree/root"), "Mixed")

        val state = vm.directory.value as DirectoryUiState.Loaded
        assertThat(state.selfComic).isNotNull()
        assertThat(state.selfComic!!.imageCount).isEqualTo(1)
        assertThat(state.subfolders).hasSize(1)
    }

    @Test
    fun `이미지 없이 하위 폴더만 있으면 selfComic 은 null`() {
        repo.defaultListing = listing(subfolders = listOf(folder("A")))
        val vm = viewModel()

        vm.openLibrary(uri("content://tree/root"), "Shelf")

        assertThat((vm.directory.value as DirectoryUiState.Loaded).selfComic).isNull()
    }

    @Test
    fun `openComic 은 reader 를 세팅하고 preview 를 닫는다`() {
        repo.pages = listOf(image("1"), image("2"), image("3"))
        val vm = viewModel()
        vm.openLibrary(uri("content://tree/root"), "lib")
        vm.showPreview(folder("SeriesA", docId = "sa", images = 3))

        vm.openComic(folder("SeriesA", docId = "sa", images = 3), fromStart = true)

        assertThat(vm.preview.value).isNull()
        val reader = vm.reader.value
        assertThat(reader).isNotNull()
        assertThat(reader!!.title).isEqualTo("SeriesA")
        assertThat(reader.pages).hasSize(3)
    }

    @Test
    fun `showPreview 와 dismissPreview 가 순서대로 방출된다`() =
        runTest {
            val vm = viewModel()
            vm.preview.test {
                assertThat(awaitItem()).isNull()
                vm.showPreview(folder("X"))
                assertThat(awaitItem()?.entry?.name).isEqualTo("X")
                vm.dismissPreview()
                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `이어보기는 저장된 페이지부터 열고 첫페이지보기는 0부터 연다`() {
        repo.pages = List(10) { image("p$it") }
        repo.progressByUri = mapOf("content://f/sa" to 4)
        val vm = viewModel()
        vm.openLibrary(uri("content://tree/root"), "lib")

        vm.openComic(folder("SeriesA", docId = "sa", images = 10), fromStart = false)
        assertThat(vm.reader.value!!.startPage).isEqualTo(4)

        vm.openComic(folder("SeriesA", docId = "sa", images = 10), fromStart = true)
        assertThat(vm.reader.value!!.startPage).isEqualTo(0)
    }

    @Test
    fun `리더 페이지가 바뀌면 진행도를 저장한다`() {
        repo.pages = List(10) { image("p$it") }
        val vm = viewModel()
        vm.openLibrary(uri("content://tree/root"), "lib")
        vm.openComic(folder("SeriesA", docId = "sa", images = 10), fromStart = true)

        vm.onReaderPageChanged(5)

        assertThat(repo.savedProgress.last().lastPage).isEqualTo(5)
        assertThat(repo.savedProgress.last().pageCount).isEqualTo(10)
    }

    @Test
    fun `책갈피 토글은 추가한 뒤 다시 제거한다`() {
        repo.pages = List(5) { image("p$it") }
        val vm = viewModel()
        vm.openLibrary(uri("content://tree/root"), "lib")
        vm.openComic(folder("SeriesA", docId = "sa", images = 5), fromStart = true)

        vm.toggleBookmark(2)
        assertThat(repo.bookmarksFlow.value.map { it.page }).containsExactly(2)

        vm.toggleBookmark(2)
        assertThat(repo.bookmarksFlow.value).isEmpty()
    }

    @Test
    fun `책갈피로 열면 해당 페이지부터 연다`() {
        repo.pages = List(10) { image("p$it") }
        val vm = viewModel()

        vm.openBookmark(
            BookmarkEntity(
                comicUri = "content://f/sa",
                treeUri = "content://tree/root",
                docId = "sa",
                name = "SeriesA",
                isArchive = false,
                page = 6,
                createdAt = 0L,
            ),
        )

        assertThat(vm.reader.value!!.startPage).isEqualTo(6)
    }
}
