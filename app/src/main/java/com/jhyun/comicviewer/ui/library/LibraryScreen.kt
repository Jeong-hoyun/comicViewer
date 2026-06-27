package com.jhyun.comicviewer.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jhyun.comicviewer.data.local.SourceFolderEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()

    // SAF: 폴더(tree) 선택 런처
    val pickFolder = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.addFolder(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("내 만화") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { pickFolder.launch(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("폴더 추가") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (folders.isEmpty()) {
                EmptyState()
            } else {
                Text(
                    text = "라이브러리 폴더",
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp),
                )
                folders.forEach { folder ->
                    FolderRow(
                        folder = folder,
                        onClick = { viewModel.scan(Uri.parse(folder.uri)) },
                        onRemove = { viewModel.removeFolder(folder) },
                    )
                }
                HorizontalDivider()
                ScanResult(scanState)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.size(16.dp))
        Text("아직 추가된 폴더가 없어요.")
        Text("우측 하단 ‘폴더 추가’로 만화가 있는 폴더를 선택하세요.")
    }
}

@Composable
private fun FolderRow(
    folder: SourceFolderEntity,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(folder.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "제거")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    )
}

@Composable
private fun ScanResult(state: ScanUiState) {
    when (state) {
        is ScanUiState.Idle -> {
            Text(
                "폴더를 탭하면 그 안의 이미지 페이지를 스캔합니다.",
                modifier = Modifier.padding(16.dp),
            )
        }

        is ScanUiState.Loading -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(12.dp))
                Text("스캔 중…")
            }
        }

        is ScanUiState.Loaded -> {
            Text(
                "이미지 ${state.images.size}개 발견",
                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.images, key = { it.uri.toString() }) { image ->
                    ListItem(headlineContent = {
                        Text(image.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    })
                }
            }
        }
    }
}
