package com.jhyun.comicviewer.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** 햄버거 → 설정 화면. 리더 기본값/볼륨키/앱 정보. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    readerDirection: ReadingDirection,
    readerLayout: PageLayout,
    volumeKeyPaging: Boolean,
    onDirectionChange: (ReadingDirection) -> Unit,
    onLayoutChange: (PageLayout) -> Unit,
    onVolumeKeyPagingChange: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                        }
                    },
                    title = { Text("설정") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
            ) {
                SettingsSection("읽기")
                ChoiceRow(
                    title = "기본 읽기 방향",
                    current = readerDirection.label,
                    options = ReadingDirection.entries.map { it.label to it },
                    onSelect = onDirectionChange,
                )
                ChoiceRow(
                    title = "기본 페이지 레이아웃",
                    current = readerLayout.label,
                    options = PageLayout.entries.map { it.label to it },
                    onSelect = onLayoutChange,
                )
                ListItem(
                    headlineContent = { Text("볼륨키로 페이지 넘김") },
                    supportingContent = { Text("볼륨↓ 다음 · 볼륨↑ 이전") },
                    trailingContent = {
                        Switch(checked = volumeKeyPaging, onCheckedChange = onVolumeKeyPagingChange)
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onVolumeKeyPagingChange(!volumeKeyPaging) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )

                HorizontalDivider()
                SettingsSection("정보")
                ListItem(
                    headlineContent = { Text("버전") },
                    supportingContent = { Text(appVersion()) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                )
            }
        }
    }
}

/** 드롭다운으로 한 값을 고르는 설정 행. */
@Composable
private fun <T> ChoiceRow(
    title: String,
    current: String,
    options: List<Pair<String, T>>,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(current, color = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.fillMaxWidth().clickable { open = true },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (label, value) ->
                DropdownMenuItem(
                    text = { Text(label + if (label == current) "  ✓" else "") },
                    onClick = {
                        onSelect(value)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
    )
}

@Composable
private fun appVersion(): String {
    val context = LocalContext.current
    return remember {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "-"
        }.getOrDefault("-")
    }
}
