package com.jhyun.comicviewer.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jhyun.comicviewer.data.DirectoryListing
import com.jhyun.comicviewer.data.FolderEntry
import com.jhyun.comicviewer.data.ImageDoc
import com.jhyun.comicviewer.data.LibraryRepository
import com.jhyun.comicviewer.data.local.SourceFolderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/** "첫페이지보기"로 연 간단 뷰어 상태. */
data class ReaderState(
    val title: String,
    val pages: List<ImageDoc>,
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
    ) : ViewModel() {
        val folders: StateFlow<List<SourceFolderEntity>> =
            repository.folders.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

        private val _directory = MutableStateFlow<DirectoryUiState>(DirectoryUiState.Empty)
        val directory: StateFlow<DirectoryUiState> = _directory.asStateFlow()

        /** 미리보기 다이얼로그 대상 (null 이면 닫힘). */
        private val _preview = MutableStateFlow<FolderEntry?>(null)
        val preview: StateFlow<FolderEntry?> = _preview.asStateFlow()

        /** 열린 뷰어 (null 이면 닫힘). */
        private val _reader = MutableStateFlow<ReaderState?>(null)
        val reader: StateFlow<ReaderState?> = _reader.asStateFlow()

        private var root: Uri? = null
        private val stack = ArrayDeque<BrowseLevel>()

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

        /** 하위 폴더 없이 이미지만 있으면 → 현재 폴더 자체를 만화 1권으로 표현. */
        private fun selfComicOrNull(
            level: BrowseLevel,
            listing: DirectoryListing,
        ): FolderEntry? {
            if (listing.subfolders.isNotEmpty() || listing.images.isEmpty()) return null
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
            _preview.value = entry
        }

        fun dismissPreview() {
            _preview.value = null
        }

        fun openComic(entry: FolderEntry) {
            val treeUri = root ?: return
            viewModelScope.launch {
                val pages = repository.listPages(treeUri, entry)
                _reader.value = ReaderState(entry.name, pages)
                _preview.value = null
            }
        }

        fun closeReader() {
            _reader.value = null
        }

        fun removeFolder(folder: SourceFolderEntity) {
            viewModelScope.launch { repository.removeFolder(folder) }
        }
    }
