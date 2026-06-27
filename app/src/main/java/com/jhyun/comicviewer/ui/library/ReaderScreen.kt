package com.jhyun.comicviewer.ui.library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ScreenLockRotation
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.jhyun.comicviewer.data.ImageDoc
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import kotlin.math.roundToInt

/** 페이지 진행 방향 (참고 앱의 6개 그리드). */
enum class ReadingDirection(
    val label: String,
    val icon: ImageVector,
) {
    Right("우측", Icons.AutoMirrored.Filled.ArrowForward),
    Left("좌측", Icons.AutoMirrored.Filled.ArrowBack),
    RightVertical("우측 + 수직", Icons.Filled.ArrowDownward),
    LeftVertical("좌측 + 수직", Icons.Filled.ArrowDownward),
    SmoothRight("우측 (S)", Icons.AutoMirrored.Filled.ArrowForward),
    SmoothLeft("좌측 (S)", Icons.AutoMirrored.Filled.ArrowBack),
    ;

    val isVertical get() = this == RightVertical || this == LeftVertical
    val isSmooth get() = this == SmoothRight || this == SmoothLeft
    val isPaged get() = this == Right || this == Left
    val isRtl get() = this == Left || this == LeftVertical || this == SmoothLeft
}

/** 페이지 레이아웃 (참고 앱의 4개). */
enum class PageLayout(
    val label: String,
) {
    Off("Off"),
    Single("1페이지씩 보기"),
    Double("2페이지씩 보기"),
    Auto("Auto"),
}

private val BACKGROUND_COLORS = listOf(Color.Black, Color.White, Color(0xFF1A1A1A))
private const val SYSTEM_BRIGHTNESS = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE // -1f

/**
 * 만화 리더 화면.
 * - 가운데 탭 → 오버레이(상단바·툴바·하단 시크바) 토글
 * - 툴바: 읽기 방향(6) / 페이지 레이아웃(4) / 회전 잠금 / 밝기 패널
 * - 밝기 패널: 자동 밝기 + 슬라이더 / 배경색 / 화면 켜둠 유지
 */
@Composable
fun ReaderScreen(
    state: ReaderState,
    onClose: () -> Unit,
) {
    val pages = state.pages
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current.findActivity()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var overlayVisible by remember { mutableStateOf(true) }
    var direction by rememberSaveable { mutableStateOf(ReadingDirection.Right) }
    var layout by rememberSaveable { mutableStateOf(PageLayout.Single) }
    var rotationLocked by rememberSaveable { mutableStateOf(false) }
    var keepScreenOn by rememberSaveable { mutableStateOf(false) }
    var autoBrightness by rememberSaveable { mutableStateOf(true) }
    var manualBrightness by rememberSaveable { mutableFloatStateOf(0.5f) }
    var bgIndex by rememberSaveable { mutableIntStateOf(0) }

    var dirPopup by remember { mutableStateOf(false) }
    var layoutPopup by remember { mutableStateOf(false) }
    var brightnessPanel by remember { mutableStateOf(false) }

    val useDouble = layout == PageLayout.Double || (layout == PageLayout.Auto && landscape)

    // 페이지를 "보기 단위"(1장 또는 2장)로 묶음.
    val units =
        remember(pages, useDouble) {
            if (useDouble) pages.indices.chunked(2) else pages.indices.map { listOf(it) }
        }

    val pagerState = rememberPagerState(pageCount = { units.size })
    val verticalState = rememberLazyListState()
    val smoothState = rememberLazyListState()

    val currentUnit =
        when {
            direction.isVertical -> verticalState.firstVisibleItemIndex
            direction.isSmooth -> smoothState.firstVisibleItemIndex
            else -> pagerState.currentPage
        }.coerceIn(0, (units.size - 1).coerceAtLeast(0))
    val currentPageIndex = units.getOrNull(currentUnit)?.firstOrNull() ?: 0

    // --- 윈도우 효과: 리더를 벗어나면 원복 ---
    LaunchedEffect(autoBrightness, manualBrightness) {
        activity?.applyBrightness(if (autoBrightness) SYSTEM_BRIGHTNESS else manualBrightness)
    }
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

    val background = BACKGROUND_COLORS[bgIndex % BACKGROUND_COLORS.size]
    val foreground = if (background == Color.White) Color.Black else Color.White
    val toggleOverlay = { overlayVisible = !overlayVisible }

    val singleScale =
        when {
            layout == PageLayout.Off -> ContentScale.Fit
            direction.isVertical -> ContentScale.FillWidth
            else -> ContentScale.Fit
        }

    Surface(modifier = Modifier.fillMaxSize(), color = background) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (pages.isEmpty()) {
                Text("페이지가 없습니다.", color = foreground, modifier = Modifier.align(Alignment.Center))
            } else {
                when {
                    direction.isVertical ->
                        LazyColumn(state = verticalState, modifier = Modifier.fillMaxSize()) {
                            items(units.size) { i ->
                                PageUnit(
                                    pages = pages,
                                    unit = units[i],
                                    isRtl = direction.isRtl,
                                    zoomable = false,
                                    singleScale = singleScale,
                                    modifier = Modifier.fillMaxWidth(),
                                    onTap = toggleOverlay,
                                )
                            }
                        }

                    direction.isSmooth ->
                        LazyRow(
                            state = smoothState,
                            reverseLayout = direction.isRtl,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            itemsIndexed(units) { _, unit ->
                                PageUnit(
                                    pages = pages,
                                    unit = unit,
                                    isRtl = direction.isRtl,
                                    zoomable = false,
                                    singleScale = singleScale,
                                    modifier = Modifier.fillParentMaxWidth().fillMaxHeight(),
                                    onTap = toggleOverlay,
                                )
                            }
                        }

                    else ->
                        HorizontalPager(
                            state = pagerState,
                            reverseLayout = direction.isRtl,
                            modifier = Modifier.fillMaxSize(),
                        ) { i ->
                            PageUnit(
                                pages = pages,
                                unit = units[i],
                                isRtl = direction.isRtl,
                                zoomable = true,
                                singleScale = singleScale,
                                modifier = Modifier.fillMaxSize(),
                                onTap = toggleOverlay,
                            )
                        }
                }
            }

            // ===== 상단 오버레이 =====
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
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToolbarButton(direction.icon, "읽기 방향") { dirPopup = true }
                        ToolbarButton(Icons.Default.Description, "페이지 레이아웃") { layoutPopup = true }
                        ToolbarButton(
                            if (rotationLocked) Icons.Default.ScreenLockRotation else Icons.Default.ScreenRotation,
                            "회전 잠금",
                        ) { rotationLocked = !rotationLocked }
                        ToolbarButton(Icons.Default.BrightnessMedium, "밝기") { brightnessPanel = true }
                    }
                }
            }

            // ===== 하단 오버레이 (페이지 카운터 + 시크바) =====
            AnimatedVisibility(
                visible = overlayVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Column(modifier = Modifier.fillMaxWidth().background(scrim())) {
                    Text(
                        "${currentPageIndex + 1} / ${pages.size}",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 4.dp),
                    )
                    if (pages.size > 1) {
                        Slider(
                            value = currentPageIndex.toFloat(),
                            onValueChange = { value ->
                                val targetPage = value.roundToInt().coerceIn(0, pages.lastIndex)
                                val targetUnit = units.indexOfFirst { it.contains(targetPage) }.coerceAtLeast(0)
                                scope.launch {
                                    when {
                                        direction.isVertical -> verticalState.scrollToItem(targetUnit)
                                        direction.isSmooth -> smoothState.scrollToItem(targetUnit)
                                        else -> pagerState.scrollToPage(targetUnit)
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

    if (dirPopup) {
        DirectionDialog(
            current = direction,
            onSelect = {
                direction = it
                dirPopup = false
            },
            onDismiss = { dirPopup = false },
        )
    }
    if (layoutPopup) {
        LayoutDialog(
            current = layout,
            onSelect = {
                layout = it
                layoutPopup = false
            },
            onDismiss = { layoutPopup = false },
        )
    }
    if (brightnessPanel) {
        BrightnessDialog(
            autoBrightness = autoBrightness,
            onAutoChange = { autoBrightness = it },
            brightness = manualBrightness,
            onBrightnessChange = {
                autoBrightness = false
                manualBrightness = it.coerceIn(0.01f, 1f)
            },
            backgroundColor = background,
            onCycleBackground = { bgIndex = (bgIndex + 1) % BACKGROUND_COLORS.size },
            keepScreenOn = keepScreenOn,
            onKeepScreenChange = { keepScreenOn = it },
            onDismiss = { brightnessPanel = false },
        )
    }
}

@Composable
private fun PageUnit(
    pages: List<ImageDoc>,
    unit: List<Int>,
    isRtl: Boolean,
    zoomable: Boolean,
    singleScale: ContentScale,
    modifier: Modifier,
    onTap: () -> Unit,
) {
    if (unit.size == 1) {
        val page = pages[unit[0]]
        if (zoomable) {
            ZoomableAsyncImage(
                model = page.model,
                contentDescription = page.name,
                contentScale = singleScale,
                modifier = modifier,
                onClick = { onTap() },
            )
        } else {
            AsyncImage(
                model = page.model,
                contentDescription = page.name,
                contentScale = singleScale,
                modifier = modifier.noRippleClickable(onTap),
            )
        }
    } else {
        val ordered = if (isRtl) unit.reversed() else unit
        Row(modifier = modifier.noRippleClickable(onTap)) {
            ordered.forEach { index ->
                AsyncImage(
                    model = pages[index].model,
                    contentDescription = pages[index].name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun DirectionDialog(
    current: ReadingDirection,
    onSelect: (ReadingDirection) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF333333)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("페이지 진행 방향", color = Color.White, fontWeight = FontWeight.Medium)
                Spacer(Modifier.size(12.dp))
                ReadingDirection.entries.chunked(3).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEach { d ->
                            val selected = d == current
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .clickable { onSelect(d) }
                                        .padding(8.dp),
                            ) {
                                Icon(
                                    d.icon,
                                    contentDescription = null,
                                    tint = if (selected) Color(0xFF2196F3) else Color.White,
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    d.label,
                                    color = if (selected) Color(0xFF2196F3) else Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayoutDialog(
    current: PageLayout,
    onSelect: (PageLayout) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF333333)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("페이지 레이아웃", color = Color.White, fontWeight = FontWeight.Medium)
                Spacer(Modifier.size(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PageLayout.entries.forEach { item ->
                        val selected = item == current
                        Text(
                            item.label,
                            color = if (selected) Color(0xFF2196F3) else Color.White,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clickable { onSelect(item) }
                                    .padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrightnessDialog(
    autoBrightness: Boolean,
    onAutoChange: (Boolean) -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    backgroundColor: Color,
    onCycleBackground: () -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF333333)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BrightnessMedium, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.size(8.dp))
                    Checkbox(checked = autoBrightness, onCheckedChange = onAutoChange)
                    Text("자동 밝기", color = Color.White, modifier = Modifier.weight(1f))
                    Text("${(brightness * 100).roundToInt()}%", color = Color.White)
                }
                Slider(
                    value = brightness,
                    onValueChange = onBrightnessChange,
                    valueRange = 0.01f..1f,
                    enabled = !autoBrightness,
                )
                Spacer(Modifier.size(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .background(backgroundColor, RoundedCornerShape(4.dp))
                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                .clickable { onCycleBackground() },
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("배경색", color = Color.White, modifier = Modifier.weight(1f))
                    Checkbox(checked = keepScreenOn, onCheckedChange = onKeepScreenChange)
                    Text("화면 켜둠 유지", color = Color.White)
                }
            }
        }
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
    window.attributes = window.attributes.apply { screenBrightness = value }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
