package com.jhyun.comicviewer.ui.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.jhyun.comicviewer.core.NaturalOrderComparator
import com.jhyun.comicviewer.data.FolderEntry
import com.jhyun.comicviewer.data.SortOrder
import com.jhyun.comicviewer.data.local.BookmarkEntity
import com.jhyun.comicviewer.data.local.ReadingProgressEntity
import com.jhyun.comicviewer.data.local.SourceFolderEntity

/** 참고 앱(Perfect Viewer 계열)의 상단 탭 구조를 우리 SAF 데이터 모델에 맞게 재해석. */
private enum class LibraryTab(
    val label: String,
    val icon: ImageVector,
) {
    Storage("스토리지", Icons.Default.Folder),
    Directory("디렉토리", Icons.AutoMirrored.Filled.MenuBook),
    History("히스토리", Icons.Default.History),
    Bookmark("책갈피", Icons.Default.Bookmark),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: LibraryViewModel = hiltViewModel()) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val directory by viewModel.directory.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val reader by viewModel.reader.collectAsStateWithLifecycle()
    val recentlyRead by viewModel.recentlyRead.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val bookmarkedPages by viewModel.bookmarkedPages.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val readerDirection by viewModel.readerDirection.collectAsStateWithLifecycle()
    val readerLayout by viewModel.readerLayout.collectAsStateWithLifecycle()
    val volumeKeyPaging by viewModel.volumeKeyPaging.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(LibraryTab.Storage.ordinal) }
    var overflowOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var gridMode by rememberSaveable { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }

    val pickFolder =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri?.let {
                viewModel.addFolder(it)
                selectedTab = LibraryTab.Directory.ordinal
            }
        }

    val openLibrary: (SourceFolderEntity) -> Unit = { folder ->
        viewModel.openLibrary(Uri.parse(folder.uri), folder.displayName)
        selectedTab = LibraryTab.Directory.ordinal
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "설정")
                        }
                    },
                    title = { Text("Paneo") },
                    actions = {
                        IconButton(onClick = { pickFolder.launch(null) }) {
                            Icon(Icons.Default.Add, contentDescription = "폴더 추가")
                        }
                        Box {
                            IconButton(onClick = { sortOpen = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "정렬")
                            }
                            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label + if (order == sortOrder) "  ✓" else "") },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            sortOpen = false
                                        },
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { gridMode = !gridMode }) {
                            Icon(
                                if (gridMode) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = if (gridMode) "리스트 보기" else "그리드 보기",
                            )
                        }
                        IconButton(onClick = { overflowOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "더보기")
                        }
                        DropdownMenu(
                            expanded = overflowOpen,
                            onDismissRequest = { overflowOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("폴더 추가") },
                                onClick = {
                                    overflowOpen = false
                                    pickFolder.launch(null)
                                },
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { pickFolder.launch(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "폴더 추가")
                }
            },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    LibraryTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab.ordinal,
                            onClick = { selectedTab = tab.ordinal },
                            text = { Text(tab.label) },
                        )
                    }
                }

                when (LibraryTab.entries[selectedTab]) {
                    LibraryTab.Storage ->
                        StorageTab(
                            folders = folders,
                            onOpen = openLibrary,
                            onRemove = viewModel::removeFolder,
                            onAdd = { pickFolder.launch(null) },
                        )

                    LibraryTab.Directory ->
                        DirectoryTab(
                            state = directory,
                            sortOrder = sortOrder,
                            gridMode = gridMode,
                            onBrowseInto = viewModel::browseInto,
                            onPreview = viewModel::showPreview,
                            onUp = viewModel::browseUp,
                        )

                    LibraryTab.History ->
                        HistoryTab(
                            items = recentlyRead,
                            onOpen = viewModel::openFromHistory,
                            onRemove = viewModel::removeHistory,
                        )

                    LibraryTab.Bookmark ->
                        BookmarkTab(
                            items = bookmarks,
                            onOpen = viewModel::openBookmark,
                            onRemove = viewModel::removeBookmark,
                        )
                }
            }
        }

        preview?.let { previewState ->
            PreviewDialog(
                state = previewState,
                onDismiss = viewModel::dismissPreview,
                onResume = { viewModel.openComic(previewState.entry, fromStart = false) },
                onStart = { viewModel.openComic(previewState.entry, fromStart = true) },
            )
        }

        reader?.let { state ->
            ReaderScreen(
                state = state,
                onClose = viewModel::closeReader,
                onProgress = viewModel::onReaderPageChanged,
                bookmarkedPages = bookmarkedPages,
                onToggleBookmark = viewModel::toggleBookmark,
                initialDirection = readerDirection,
                initialLayout = readerLayout,
                onDirectionChange = viewModel::setReaderDirection,
                onLayoutChange = viewModel::setReaderLayout,
                volumeKeyPaging = volumeKeyPaging,
            )
        }

        if (showSettings) {
            SettingsScreen(
                readerDirection = readerDirection,
                readerLayout = readerLayout,
                volumeKeyPaging = volumeKeyPaging,
                onDirectionChange = viewModel::setReaderDirection,
                onLayoutChange = viewModel::setReaderLayout,
                onVolumeKeyPagingChange = viewModel::setVolumeKeyPaging,
                onClose = { showSettings = false },
            )
        }
    }
}

@Composable
private fun StorageTab(
    folders: List<SourceFolderEntity>,
    onOpen: (SourceFolderEntity) -> Unit,
    onRemove: (SourceFolderEntity) -> Unit,
    onAdd: () -> Unit,
) {
    var pendingRemove by remember { mutableStateOf<SourceFolderEntity?>(null) }
    pendingRemove?.let { folder ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("폴더 제거") },
            text = { Text("‘${folder.displayName}’ 를 라이브러리에서 제거할까요?\n(기기의 실제 파일은 삭제되지 않습니다.)") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(folder)
                    pendingRemove = null
                }) { Text("제거") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemove = null }) { Text("취소") }
            },
        )
    }

    if (folders.isEmpty()) {
        PlaceholderTab(
            icon = Icons.Default.Folder,
            title = "아직 추가된 폴더가 없어요.",
            body = "우측 하단 버튼으로 만화가 있는 폴더를 선택하세요.",
            actionLabel = "폴더 추가",
            onAction = onAdd,
        )
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        val recent = folders.first() // addedAt DESC → 첫 항목이 가장 최근.
        item {
            ListItem(
                headlineContent = { Text("Recent") },
                supportingContent = {
                    Text(recent.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                leadingContent = { Icon(Icons.Default.Schedule, contentDescription = null) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(recent) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
            HorizontalDivider()
            SectionHeader("라이브러리 폴더")
        }
        items(folders, key = { it.uri }) { folder ->
            ListItem(
                headlineContent = {
                    Text(folder.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                trailingContent = {
                    IconButton(onClick = { pendingRemove = folder }) {
                        Icon(Icons.Default.Delete, contentDescription = "제거")
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(folder) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    }
}

@Composable
private fun DirectoryTab(
    state: DirectoryUiState,
    sortOrder: SortOrder,
    gridMode: Boolean,
    onBrowseInto: (FolderEntry) -> Unit,
    onPreview: (FolderEntry) -> Unit,
    onUp: () -> Unit,
) {
    when (state) {
        is DirectoryUiState.Empty ->
            PlaceholderTab(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "선택된 폴더가 없어요.",
                body = "‘스토리지’ 탭에서 폴더를 탭하면 그 안의 내용이 표시됩니다.",
            )

        is DirectoryUiState.Loading ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(12.dp))
                Text("불러오는 중…")
            }

        is DirectoryUiState.Loaded -> {
            if (state.canGoUp) BackHandler(onBack = onUp)

            val entries =
                remember(state.subfolders, sortOrder) {
                    val natural = NaturalOrderComparator()
                    val byName = compareBy(natural) { e: FolderEntry -> e.name }
                    val byDate = compareBy { e: FolderEntry -> e.lastModified }
                    val comparator =
                        when (sortOrder) {
                            SortOrder.NameAsc -> byName
                            SortOrder.NameDesc -> byName.reversed()
                            SortOrder.DateAsc -> byDate
                            SortOrder.DateDesc -> byDate.reversed()
                        }
                    state.subfolders.sortedWith(comparator)
                }

            // selfComic(현재 폴더 자체) 을 맨 앞에 둔 전체 항목.
            val cells = remember(entries, state.selfComic) { listOfNotNull(state.selfComic) + entries }
            val isEmpty = cells.isEmpty()

            if (gridMode) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 112.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        DirectoryHeader(state.title, state.canGoUp, onUp)
                    }
                    if (isEmpty) {
                        item(span = { GridItemSpan(maxLineSpan) }) { EmptyDirectoryMessage() }
                    }
                    items(cells, key = { it.documentId }) { entry ->
                        if (entry.isComic || entry.isArchive) {
                            ComicCard(entry, onClick = { onPreview(entry) })
                        } else {
                            FolderCard(entry, onClick = { onBrowseInto(entry) })
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { DirectoryHeader(state.title, state.canGoUp, onUp) }
                    if (isEmpty) {
                        item { EmptyDirectoryMessage() }
                    }
                    items(cells, key = { it.documentId }) { entry ->
                        if (entry.isComic || entry.isArchive) {
                            ComicRow(entry, onClick = { onPreview(entry) })
                        } else {
                            FolderRow(entry, onClick = { onBrowseInto(entry) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectoryHeader(
    title: String,
    canGoUp: Boolean,
    onUp: () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        ) {
            if (canGoUp) {
                IconButton(onClick = onUp) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "상위 폴더")
                }
            }
            Text(
                title,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.padding(
                        start = if (canGoUp) 0.dp else 16.dp,
                        top = 12.dp,
                        bottom = 12.dp,
                        end = 16.dp,
                    ),
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun EmptyDirectoryMessage() {
    Text(
        "이 폴더에는 표시할 만화나 하위 폴더가 없어요.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}

/** 그리드: 표지 카드(만화 1권). */
@Composable
private fun ComicCard(
    entry: FolderEntry,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(6.dp),
    ) {
        CoverThumbnail(
            cover = entry.cover,
            modifier = Modifier.fillMaxWidth().aspectRatio(0.72f),
        )
        Spacer(Modifier.size(4.dp))
        Text(entry.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            "${entry.imageCount}p",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 그리드: 하위 폴더 카드(탐색). */
@Composable
private fun FolderCard(
    entry: FolderEntry,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(4.dp))
        Text(entry.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 리스트: 하위 폴더 행(탐색). */
@Composable
private fun FolderRow(
    entry: FolderEntry,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            if (entry.imageCount > 0) Text("이미지 ${entry.imageCount}개 + 하위 폴더") else Text("하위 폴더")
        },
        leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
    )
}

/** 표지 썸네일 + 제목 + 페이지 수 (만화 1권 행). */
@Composable
private fun ComicRow(
    entry: FolderEntry,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverThumbnail(
            cover = entry.cover,
            modifier =
                Modifier
                    .width(52.dp)
                    .aspectRatio(0.72f),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.size(2.dp))
            Text(
                "${entry.imageCount}페이지",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CoverThumbnail(
    cover: Any?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(4.dp),
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (cover != null) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 미리보기 팝업: 표지 + 페이지 수 + (이어보기) + 첫페이지보기. */
@Composable
private fun PreviewDialog(
    state: PreviewState,
    onDismiss: () -> Unit,
    onResume: () -> Unit,
    onStart: () -> Unit,
) {
    val entry = state.entry
    val resumePage = state.resumePage
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CoverThumbnail(
                    cover = entry.cover,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .aspectRatio(0.72f),
                )
                Spacer(Modifier.size(12.dp))
                if (resumePage != null && resumePage > 0) {
                    Text("${resumePage + 1} / ${entry.imageCount} 페이지까지 읽음")
                } else {
                    Text("총 ${entry.imageCount}페이지")
                }
            }
        },
        confirmButton = {
            if (resumePage != null && resumePage > 0) {
                TextButton(onClick = onResume) { Text("이어보기") }
            } else {
                TextButton(onClick = onStart) { Text("첫페이지보기") }
            }
        },
        dismissButton = {
            if (resumePage != null && resumePage > 0) {
                TextButton(onClick = onStart) { Text("첫페이지보기") }
            } else {
                TextButton(onClick = onDismiss) { Text("닫기") }
            }
        },
    )
}

/** 히스토리 탭: 최근 읽은 만화 목록. */
@Composable
private fun HistoryTab(
    items: List<ReadingProgressEntity>,
    onOpen: (ReadingProgressEntity) -> Unit,
    onRemove: (ReadingProgressEntity) -> Unit,
) {
    if (items.isEmpty()) {
        PlaceholderTab(
            icon = Icons.Default.History,
            title = "히스토리가 비어 있어요.",
            body = "만화를 읽으면 여기에 최근 기록이 쌓입니다.",
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.comicUri }) { item ->
            ListItem(
                headlineContent = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text("${item.lastPage + 1} / ${item.pageCount} 페이지") },
                leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                trailingContent = {
                    IconButton(onClick = { onRemove(item) }) {
                        Icon(Icons.Default.Close, contentDescription = "기록 삭제")
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(item) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    }
}

/** 책갈피 탭: 저장된 책갈피 목록 → 탭하면 해당 페이지로. */
@Composable
private fun BookmarkTab(
    items: List<BookmarkEntity>,
    onOpen: (BookmarkEntity) -> Unit,
    onRemove: (BookmarkEntity) -> Unit,
) {
    if (items.isEmpty()) {
        PlaceholderTab(
            icon = Icons.Default.Bookmark,
            title = "저장된 책갈피가 없어요.",
            body = "리더 상단 책갈피 버튼으로 현재 페이지를 저장하세요.",
        )
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.id }) { item ->
            ListItem(
                headlineContent = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text("${item.page + 1} 페이지") },
                leadingContent = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                trailingContent = {
                    IconButton(onClick = { onRemove(item) }) {
                        Icon(Icons.Default.Close, contentDescription = "책갈피 삭제")
                    }
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(item) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
    )
}

@Composable
private fun PlaceholderTab(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(16.dp))
        Text(title)
        Spacer(Modifier.size(4.dp))
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.size(16.dp))
            Text(
                actionLabel,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onAction() }.padding(8.dp),
            )
        }
    }
}
