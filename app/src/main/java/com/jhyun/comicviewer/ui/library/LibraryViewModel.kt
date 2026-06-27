package com.jhyun.comicviewer.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhyun.comicviewer.data.DirectoryListing
import com.jhyun.comicviewer.data.FolderEntry
import com.jhyun.comicviewer.data.ImageDoc
import com.jhyun.comicviewer.data.LibraryRepository
import com.jhyun.comicviewer.data.SettingsStore
import com.jhyun.comicviewer.data.SortOrder
import com.jhyun.comicviewer.data.local.BookmarkEntity
import com.jhyun.comicviewer.data.local.ReadingProgressEntity
import com.jhyun.comicviewer.data.local.SourceFolderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DirectoryUiState {
    data object Empty : DirectoryUiState

    data object Loading : DirectoryUiState

    data class Loaded(
        val subfolders: List<FolderEntry>,
        /** 현재 폴더 자체가 "이미지만 있는" 만화일 때의 항목 (없으면 null). */
        val selfComic: FolderEntry?,
        val title: String,
        val canGoUp: Boolean,
    ) : DirectoryUiState
}

/** 리더 상태. startPage 부터 열립니다(이어보기). */
data class ReaderState(
    val title: String,
    val pages: List<ImageDoc>,
    val startPage: Int = 0,
)

/** 미리보기 다이얼로그 상태. resumePage 가 있으면 "이어보기" 노출. */
data class PreviewState(
    val entry: FolderEntry,
    val resumePage: Int?,
)

private data class BrowseLevel(
    val docId: String?,
    val title: String,
)

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val repository: LibraryRepository,
        private val settings: SettingsStore,
    ) : ViewModel() {
        /** 정렬 기준(영속). */
        val sortOrder: StateFlow<SortOrder> =
            settings.sortOrder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SortOrder.NameAsc)

        /** 리더 기본 읽기 방향(영속). */
        val readerDirection: StateFlow<ReadingDirection> =
            settings.readerDirection
                .map {
                    it?.let { n -> runCatching { ReadingDirection.valueOf(n) }.getOrNull() }
                        ?: ReadingDirection.Right
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingDirection.Right)

        /** 리더 기본 페이지 레이아웃(영속). */
        val readerLayout: StateFlow<PageLayout> =
            settings.readerLayout
                .map { it?.let { n -> runCatching { PageLayout.valueOf(n) }.getOrNull() } ?: PageLayout.Single }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PageLayout.Single)

        fun setSortOrder(value: SortOrder) {
            viewModelScope.launch { settings.setSortOrder(value) }
        }

        fun setReaderDirection(value: ReadingDirection) {
            viewModelScope.launch { settings.setReaderDirection(value.name) }
        }

        fun setReaderLayout(value: PageLayout) {
            viewModelScope.launch { settings.setReaderLayout(value.name) }
        }

        val folders: StateFlow<List<SourceFolderEntity>> =
            repository.folders.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

        private val _directory = MutableStateFlow<DirectoryUiState>(DirectoryUiState.Empty)
        val directory: StateFlow<DirectoryUiState> = _directory.asStateFlow()

        /** 최근 읽은 만화(히스토리 탭). */
        val recentlyRead: StateFlow<List<ReadingProgressEntity>> =
            repository.recentlyRead.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

        /** 전체 책갈피(책갈피 탭). */
        val bookmarks: StateFlow<List<BookmarkEntity>> =
            repository.bookmarks.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

        /** 현재 열린 만화의 책갈피 페이지 집합(리더 책갈피 아이콘 표시용). */
        private val openComicUriFlow = MutableStateFlow<String?>(null)
        val bookmarkedPages: StateFlow<Set<Int>> =
            combine(openComicUriFlow, repository.bookmarks) { uri, all ->
                if (uri == null) emptySet() else all.filter { it.comicUri == uri }.map { it.page }.toSet()
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet(),
            )

        /** 미리보기 다이얼로그 대상 (null 이면 닫힘). */
        private val _preview = MutableStateFlow<PreviewState?>(null)
        val preview: StateFlow<PreviewState?> = _preview.asStateFlow()

        /** 열린 뷰어 (null 이면 닫힘). */
        private val _reader = MutableStateFlow<ReaderState?>(null)
        val reader: StateFlow<ReaderState?> = _reader.asStateFlow()

        private var root: Uri? = null
        private val stack = ArrayDeque<BrowseLevel>()

        // 현재 열린 만화(진행도 저장용).
        private var openComicEntry: FolderEntry? = null
        private var openComicTree: Uri? = null
        private var openComicPageCount: Int = 0

        fun addFolder(uri: Uri) {
            viewModelScope.launch {
                val name = repository.addFolder(uri)
                openLibrary(uri, name)
            }
        }

        fun openLibrary(
            uri: Uri,
            name: String,
        ) {
            root = uri
            stack.clear()
            stack.addLast(BrowseLevel(docId = null, title = name))
            load()
        }

        fun browseInto(entry: FolderEntry) {
            stack.addLast(BrowseLevel(entry.documentId, entry.name))
            load()
        }

        fun browseUp() {
            if (stack.size > 1) {
                stack.removeLast()
                load()
            }
        }

        private fun load() {
            val treeUri = root ?: return
            val level = stack.last()
            viewModelScope.launch {
                _directory.value = DirectoryUiState.Loading
                val listing = repository.listDirectory(treeUri, level.docId)
                _directory.value =
                    DirectoryUiState.Loaded(
                        subfolders = listing.subfolders,
                        selfComic = selfComicOrNull(level, listing),
                        title = level.title,
                        canGoUp = stack.size > 1,
                    )
            }
        }

        /** 현재 폴더에 직접 이미지가 있으면 → 그 폴더 자체를 만화 1권으로 표현(하위 폴더 유무 무관). */
        private fun selfComicOrNull(
            level: BrowseLevel,
            listing: DirectoryListing,
        ): FolderEntry? {
            if (listing.images.isEmpty()) return null
            return FolderEntry(
                uri = listing.folderUri,
                documentId = listing.folderDocId,
                name = level.title,
                cover = listing.images.first().model,
                imageCount = listing.images.size,
                hasSubfolders = false,
            )
        }

        fun showPreview(entry: FolderEntry) {
            viewModelScope.launch {
                _preview.value = PreviewState(entry, repository.getProgress(entry.uri.toString()))
            }
        }

        fun dismissPreview() {
            _preview.value = null
        }

        /** 미리보기에서 만화 열기. fromStart=false 면 저장된 페이지부터(이어보기). */
        fun openComic(
            entry: FolderEntry,
            fromStart: Boolean,
        ) {
            val treeUri = root ?: return
            launchOpen(treeUri, entry, if (fromStart) 0 else null)
        }

        /** 히스토리 항목에서 다시 열기(저장된 페이지부터). */
        fun openFromHistory(item: ReadingProgressEntity) {
            val treeUri = Uri.parse(item.treeUri)
            root = treeUri
            launchOpen(treeUri, entryOf(item.comicUri, item.docId, item.name, item.isArchive, item.pageCount), null)
        }

        /** 책갈피에서 해당 페이지로 열기. */
        fun openBookmark(item: BookmarkEntity) {
            val treeUri = Uri.parse(item.treeUri)
            root = treeUri
            launchOpen(treeUri, entryOf(item.comicUri, item.docId, item.name, item.isArchive, 0), item.page)
        }

        private fun entryOf(
            comicUri: String,
            docId: String,
            name: String,
            isArchive: Boolean,
            pageCount: Int,
        ) = FolderEntry(
            uri = Uri.parse(comicUri),
            documentId = docId,
            name = name,
            cover = null,
            imageCount = pageCount,
            hasSubfolders = false,
            isArchive = isArchive,
        )

        /** explicitPage 가 null 이면 저장된 진행도부터, 아니면 그 페이지부터. */
        private fun launchOpen(
            treeUri: Uri,
            entry: FolderEntry,
            explicitPage: Int?,
        ) {
            viewModelScope.launch {
                val pages = repository.listPages(treeUri, entry)
                val start = explicitPage ?: (repository.getProgress(entry.uri.toString()) ?: 0)
                openComicEntry = entry
                openComicTree = treeUri
                openComicPageCount = pages.size
                openComicUriFlow.value = entry.uri.toString()
                _reader.value =
                    ReaderState(
                        title = entry.name,
                        pages = pages,
                        startPage = start.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
                    )
                _preview.value = null
            }
        }

        /** 현재 열린 만화의 page 책갈피를 토글. */
        fun toggleBookmark(page: Int) {
            val entry = openComicEntry ?: return
            val treeUri = openComicTree ?: return
            viewModelScope.launch { repository.toggleBookmark(treeUri, entry, page) }
        }

        /** 리더에서 현재 페이지가 바뀔 때 진행도 저장. */
        fun onReaderPageChanged(page: Int) {
            val entry = openComicEntry ?: return
            val treeUri = openComicTree ?: return
            viewModelScope.launch {
                repository.saveProgress(treeUri, entry, page, openComicPageCount)
            }
        }

        fun closeReader() {
            _reader.value = null
            openComicEntry = null
            openComicTree = null
            openComicUriFlow.value = null
        }

        fun removeFolder(folder: SourceFolderEntity) {
            viewModelScope.launch { repository.removeFolder(folder) }
        }
    }
