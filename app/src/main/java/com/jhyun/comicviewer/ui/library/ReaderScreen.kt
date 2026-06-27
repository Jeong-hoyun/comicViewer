package com.jhyun.comicviewer.ui.library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import kotlin.math.roundToInt

/** 리더 읽기 모드. */
enum class ReadingMode(
    val label: String,
) {
    PageLtr("페이지 (좌→우)"),
    PageRtl("페이지 (우→좌)"),
    Vertical("세로 (웹툰)"),
}

private const val SYSTEM_BRIGHTNESS = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE // -1f

/**
 * 만화 리더 화면.
 * - 가운데 탭 → 오버레이(상단바·툴바·하단 시크바) 토글
 * - 페이지(좌→우/우→좌) + 세로 웹툰 모드, 핀치 줌(페이지 모드)
 * - 툴바: 읽기 모드 / 밝기 / 회전 잠금 / 화면 켜둠 유지 / 배경색
 */
@Composable
fun ReaderScreen(
    state: ReaderState,
    onClose: () -> Unit,
) {
    val pages = state.pages
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findActivity()

    var overlayVisible by remember { mutableStateOf(true) }
    var mode by rememberSaveable { mutableStateOf(ReadingMode.PageLtr) }
    var brightness by rememberSaveable { mutableFloatStateOf(SYSTEM_BRIGHTNESS) }
    var showBrightness by remember { mutableStateOf(false) }
    var rotationLocked by rememberSaveable { mutableStateOf(false) }
    var keepScreenOn by rememberSaveable { mutableStateOf(true) }
    var lightBackground by rememberSaveable { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val listState = rememberLazyListState()

    val currentIndex =
        if (mode == ReadingMode.Vertical) listState.firstVisibleItemIndex else pagerState.currentPage

    // --- 윈도우 효과 (밝기 / 회전 / 화면 유지) : 리더를 벗어나면 원복 ---
    LaunchedEffect(brightness) { activity?.applyBrightness(brightness) }
    DisposableEffect(Unit) { onDispose { activity?.applyBrightness(SYSTEM_BRIGHTNESS) } }

    DisposableEffect(rotationLocked) {
        activity?.requestedOrientation =
            if (rotationLocked) {
                ActivityInfo.SCREEN_ORIENTATION_LOCKED
            } else {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        onDispose {}
    }
    DisposableEffect(Unit) {
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    DisposableEffect(keepScreenOn) {
        val window = activity?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {}
    }
    DisposableEffect(Unit) {
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    BackHandler(onBack = onClose)

    val background = if (lightBackground) Color.White else Color.Black
    val foreground = if (lightBackground) Color.Black else Color.White
    val toggleOverlay = { overlayVisible = !overlayVisible }

    Surface(modifier = Modifier.fillMaxSize(), color = background) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                pages.isEmpty() ->
                    Text(
                        "페이지가 없습니다.",
                        color = foreground,
                        modifier = Modifier.align(Alignment.Center),
                    )

                mode == ReadingMode.Vertical ->
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(pages, key = { it.uri.toString() }) { page ->
                            AsyncImage(
                                model = page.uri,
                                contentDescription = page.name,
                                contentScale = ContentScale.FillWidth,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .noRippleClickable(toggleOverlay),
                            )
                        }
                    }

                else ->
                    HorizontalPager(
                        state = pagerState,
                        reverseLayout = mode == ReadingMode.PageRtl,
                        modifier = Modifier.fillMaxSize(),
                    ) { index ->
                        val page = pages[index]
                        ZoomableAsyncImage(
                            model = page.uri,
                            contentDescription = page.name,
                            modifier = Modifier.fillMaxSize(),
                            onClick = { toggleOverlay() },
                        )
                    }
            }

            // ===== 상단 오버레이 (상단바 + 툴바) =====
            AnimatedVisibility(
                visible = overlayVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Column(modifier = Modifier.fillMaxWidth().background(scrim())) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
                        }
                        Text(
                            state.title,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    ReaderToolbar(
                        mode = mode,
                        onModeChange = { mode = it },
                        onToggleBrightness = { showBrightness = !showBrightness },
                        rotationLocked = rotationLocked,
                        onToggleRotation = { rotationLocked = !rotationLocked },
                        keepScreenOn = keepScreenOn,
                        onToggleKeepOn = { keepScreenOn = !keepScreenOn },
                        onToggleBackground = { lightBackground = !lightBackground },
                    )
                }
            }

            // ===== 하단 오버레이 (밝기 슬라이더 + 페이지 카운터 + 시크바) =====
            AnimatedVisibility(
                visible = overlayVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Column(modifier = Modifier.fillMaxWidth().background(scrim())) {
                    if (showBrightness) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.BrightnessMedium,
                                contentDescription = null,
                                tint = Color.White,
                            )
                            Slider(
                                value = if (brightness < 0f) 0.5f else brightness,
                                onValueChange = { brightness = it.coerceIn(0.01f, 1f) },
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            )
                        }
                    }
                    Text(
                        "${currentIndex + 1} / ${pages.size}",
                        color = Color.White,
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 4.dp),
                    )
                    if (pages.size > 1) {
                        Slider(
                            value = currentIndex.toFloat(),
                            onValueChange = { value ->
                                val target = value.roundToInt().coerceIn(0, pages.lastIndex)
                                scope.launch {
                                    if (mode == ReadingMode.Vertical) {
                                        listState.scrollToItem(target)
                                    } else {
                                        pagerState.scrollToPage(target)
                                    }
                                }
                            },
                            valueRange = 0f..pages.lastIndex.toFloat(),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderToolbar(
    mode: ReadingMode,
    onModeChange: (ReadingMode) -> Unit,
    onToggleBrightness: () -> Unit,
    rotationLocked: Boolean,
    onToggleRotation: () -> Unit,
    keepScreenOn: Boolean,
    onToggleKeepOn: () -> Unit,
    onToggleBackground: () -> Unit,
) {
    var modeMenuOpen by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            ToolbarButton(Icons.AutoMirrored.Filled.MenuBook, "읽기 모드") { modeMenuOpen = true }
            DropdownMenu(expanded = modeMenuOpen, onDismissRequest = { modeMenuOpen = false }) {
                ReadingMode.entries.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.label + if (item == mode) "  ✓" else "") },
                        onClick = {
                            onModeChange(item)
                            modeMenuOpen = false
                        },
                    )
                }
            }
        }
        ToolbarButton(Icons.Default.BrightnessMedium, "밝기", onClick = onToggleBrightness)
        ToolbarButton(
            if (rotationLocked) Icons.Default.ScreenLockRotation else Icons.Default.ScreenRotation,
            "회전 잠금",
            onClick = onToggleRotation,
        )
        ToolbarButton(
            if (keepScreenOn) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            "화면 켜둠 유지",
            onClick = onToggleKeepOn,
        )
        ToolbarButton(Icons.Default.InvertColors, "배경색", onClick = onToggleBackground)
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description, tint = Color.White)
    }
}

private fun scrim() = Color(0xCC000000)

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = interaction, indication = null) { onClick() }
}

private fun Activity.applyBrightness(value: Float) {
    window.attributes =
        window.attributes.apply { screenBrightness = value }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
