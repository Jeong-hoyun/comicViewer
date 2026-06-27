package com.jhyun.comicviewer.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Loading : ScanUiState
    data class Loaded(val images: List<ImageDoc>) : ScanUiState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    val folders: StateFlow<List<SourceFolderEntity>> =
        repository.folders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    fun addFolder(uri: Uri) {
        viewModelScope.launch {
            repository.addFolder(uri)
            scan(uri)
        }
    }

    fun scan(uri: Uri) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Loading
            _scanState.value = ScanUiState.Loaded(repository.scanFolder(uri))
        }
    }

    fun removeFolder(folder: SourceFolderEntity) {
        viewModelScope.launch { repository.removeFolder(folder) }
    }
}
