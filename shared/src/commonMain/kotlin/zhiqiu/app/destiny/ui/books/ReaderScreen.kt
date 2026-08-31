package zhiqiu.app.destiny.ui.books

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.List
import compose.icons.feathericons.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------- 阅读主题（番茄式 5 背景） ----------
data class ReaderTheme(
    val name: String,
    val bg: Color,
    val text: Color,
    val quote: Color,
    val divider: Color,
    val bar: Color,
    val dark: Boolean,
)

private val READER_THEMES = listOf(
    ReaderTheme("白色", Color(0xFFFFFFFF), Color(0xFF262626), Color(0xFF8C6E63), Color(0xFFE0E0E0), Color(0xF7FFFFFF), false),
    ReaderTheme("米黄", Color(0xFFFAF4E5), Color(0xFF3A3226), Color(0xFF8A6D3B), Color(0xFFE3D9BC), Color(0xF7FAF4E5), false),
    ReaderTheme("护眼", Color(0xFFE0EBDE), Color(0xFF2C3A2C), Color(0xFF5B7A5B), Color(0xFFC4D4C4), Color(0xF7E0EBDE), false),
    ReaderTheme("深灰", Color(0xFF2B2B2B), Color(0xFFB8B8B8), Color(0xFF8F8F78), Color(0xFF444444), Color(0xF72B2B2B), true),
    ReaderTheme("夜间", Color(0xFF000000), Color(0xFF9E9E9E), Color(0xFF7A6F55), Color(0xFF222222), Color(0xF7000000), true),
)

/** 阅读偏好 + 每本书章节进度（持久化到 Room 键值表，跨启动生效） */
object ReaderPrefs {
    private var store: ReaderStore? = null

    private val fontSizeState = mutableFloatStateOf(20f)
    private val lineSpacingState = mutableFloatStateOf(1.5f)
    private val themeIdxState = mutableIntStateOf(1)

    var fontSize: Float
        get() = fontSizeState.floatValue
        set(v) {
            fontSizeState.floatValue = v
            store?.put("reader.fontSize", v.toString())
        }
    var lineSpacing: Float
        get() = lineSpacingState.floatValue
        set(v) {
            lineSpacingState.floatValue = v
            store?.put("reader.lineSpacing", v.toString())
        }
    var themeIdx: Int
        get() = themeIdxState.intValue
        set(v) {
            themeIdxState.intValue = v
            store?.put("reader.themeIdx", v.toString())
        }

    /** 会话内立即生效的章节进度缓存；持久化走 saveLastSection */
    val lastSection = mutableMapOf<String, Int>()

    /** 接入 Room 存取器并载入已存偏好（只生效一次） */
    suspend fun attach(s: ReaderStore) {
        if (store != null) return
        store = s
        s.get("reader.fontSize")?.toFloatOrNull()?.let { fontSizeState.floatValue = it.coerceIn(14f, 32f) }
        s.get("reader.lineSpacing")?.toFloatOrNull()?.let { lineSpacingState.floatValue = it.coerceIn(1f, 2.2f) }
        s.get("reader.themeIdx")?.toIntOrNull()?.let { themeIdxState.intValue = it.coerceIn(0, READER_THEMES.size - 1) }
    }

    suspend fun loadLastSection(bookId: String): Int =
        store?.get("reader.pos.$bookId")?.toIntOrNull() ?: lastSection[bookId] ?: 0

    fun saveLastSection(bookId: String, idx: Int) {
        lastSection[bookId] = idx
        store?.put("reader.pos.$bookId", idx.toString())
    }
}

/** 扁平化条目：章节头 + 段落 */
private data class ReaderItem(val sectionIdx: Int, val isHeader: Boolean, val text: String?)

@Composable
fun ReaderScreen(
    bookId: String,
    readerStore: ReaderStore,
    onBack: () -> Unit,
) {
    var book by remember { mutableStateOf<Book?>(null) }
    // 顶栏/底栏默认显示（含返回键），点内容区中央可切换沉浸
    var menuVisible by remember { mutableStateOf(true) }
    var showToc by remember { mutableStateOf(false) }
    var showAa by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 先载入持久化偏好，再渲染正文（避免主题/字号闪默认值）
    var prefsReady by remember { mutableStateOf(false) }
    LaunchedEffect(readerStore) {
        ReaderPrefs.attach(readerStore)
        prefsReady = true
    }

    LaunchedEffect(bookId) { book = BookStore.loadBook(bookId) }
    val b = book

    if (!prefsReady) {
        Box(modifier = Modifier.fillMaxSize().background(READER_THEMES[1].bg))
        return
    }

    val theme = READER_THEMES[ReaderPrefs.themeIdx]

    if (b == null) {
        Box(modifier = Modifier.fillMaxSize().background(theme.bg), contentAlignment = Alignment.Center) {
            Text("加载中…", color = theme.text, fontSize = 14.sp)
        }
        return
    }

    // 扁平化：每章头 + 每段
    val items = remember(b) {
        buildList {
            b.sections.forEachIndexed { si, sec ->
                add(ReaderItem(si, isHeader = true, text = null))
                sec.paragraphs.forEach { add(ReaderItem(si, isHeader = false, text = it)) }
            }
        }
    }
    val anchors = remember(b) {
        buildList {
            var idx = 0
            b.sections.forEach { sec ->
                add(idx)
                idx += 1 + sec.paragraphs.size
            }
        }
    }
    val currentSection by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex
                .let { first -> items.getOrNull(first)?.sectionIdx ?: 0 }
        }
    }
    // 全书阅读百分比（按已滚动条目近似）
    val readPercent by remember {
        derivedStateOf {
            if (items.isEmpty()) 0
            else ((listState.firstVisibleItemIndex.toFloat() / items.size) * 100).toInt().coerceIn(0, 100)
        }
    }
    // 恢复上次阅读进度
    LaunchedEffect(b, prefsReady) {
        if (!prefsReady) return@LaunchedEffect
        val idx = ReaderPrefs.loadLastSection(bookId)
        if (idx > 0 && idx < anchors.size) {
            listState.scrollToItem(anchors[idx])
        }
    }
    // 滚动停止 1 秒后持久化进度（防抖）
    LaunchedEffect(currentSection) {
        delay(1000)
        ReaderPrefs.saveLastSection(bookId, currentSection)
    }

    fun jumpSection(si: Int, animated: Boolean = true) {
        val a = anchors.getOrNull(si) ?: return
        ReaderPrefs.saveLastSection(bookId, si)
        scope.launch {
            if (animated) listState.animateScrollToItem(a) else listState.scrollToItem(a)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(theme.bg),
    ) {
        // 顶部菜单：占位布局，不遮挡正文
        AnimatedVisibility(visible = menuVisible, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.bar)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = "返回", tint = theme.text)
                }
                Text(
                    b.title,
                    color = theme.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showToc = !showToc; showAa = false }) {
                    Icon(FeatherIcons.List, contentDescription = "目录", tint = theme.text)
                }
            }
        }

        // 内容区：点左/右三分之一翻章，点中间切换菜单
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(items) {
                    detectTapGestures { offset ->
                        when {
                            offset.x < size.width / 3f -> {
                                jumpSection((currentSection - 1).coerceAtLeast(0))
                            }
                            offset.x > size.width * 2f / 3f -> {
                                jumpSection((currentSection + 1).coerceAtMost(b.sections.size - 1))
                            }
                            else -> {
                                menuVisible = !menuVisible
                                showAa = false
                                showToc = false
                            }
                        }
                    }
                },
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
            ) {
                itemsIndexed(items) { _, item ->
                    val sec = b.sections[item.sectionIdx]
                    if (item.isHeader) {
                        Text(
                            sec.title.ifBlank { b.title },
                            color = theme.text,
                            fontSize = (ReaderPrefs.fontSize * 1.15f).sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = (ReaderPrefs.fontSize * 1.15f * ReaderPrefs.lineSpacing).sp,
                            modifier = Modifier.padding(top = 18.dp, bottom = 12.dp),
                        )
                    } else {
                        Text(
                            "        ${item.text}",
                            color = theme.text,
                            fontSize = ReaderPrefs.fontSize.sp,
                            lineHeight = (ReaderPrefs.fontSize * ReaderPrefs.lineSpacing).sp,
                            modifier = Modifier.padding(bottom = (ReaderPrefs.fontSize * 0.55f).dp),
                        )
                    }
                }
            }

            // 目录弹层
            if (showToc) {
                TocPanel(
                    book = b,
                    current = currentSection,
                    theme = theme,
                    onPick = { si ->
                        jumpSection(si)
                        showToc = false
                    },
                    onClose = { showToc = false },
                )
            }
        }

        // 底部菜单 + Aa 面板：占位布局，不遮挡正文
        AnimatedVisibility(visible = menuVisible, enter = fadeIn(), exit = fadeOut()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (showAa) {
                    AaPanel(
                        theme = theme,
                        sectionCount = b.sections.size,
                        currentSection = currentSection,
                        onSeek = { si -> jumpSection(si, animated = false) },
                        onDismiss = { showAa = false },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.bar)
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton2("上一章", theme) { jumpSection((currentSection - 1).coerceAtLeast(0)) }
                    TextButton2("目录", theme) { showToc = !showToc; showAa = false }
                    Text(
                        "${currentSection + 1}/${b.sections.size} · $readPercent%",
                        color = theme.text,
                        fontSize = 12.sp,
                    )
                    IconButton(onClick = { showAa = !showAa; showToc = false }) {
                        Icon(FeatherIcons.Type, contentDescription = "设置", tint = theme.text)
                    }
                    TextButton2("下一章", theme) {
                        jumpSection((currentSection + 1).coerceAtMost(b.sections.size - 1))
                    }
                }
            }
        }
    }
}

@Composable
private fun TextButton2(text: String, theme: ReaderTheme, onClick: () -> Unit) {
    Text(
        text,
        color = theme.text,
        fontSize = 14.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

/** Aa 设置面板：章节进度滑条 + 字号滑块 + 行距 + 背景（即时生效并持久化） */
@Composable
private fun AaPanel(
    theme: ReaderTheme,
    sectionCount: Int,
    currentSection: Int,
    onSeek: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = if (theme.dark) Color(0xFFFF6F5E) else Color(0xFFFF4C40)
    var seek by remember(currentSection) { mutableFloatStateOf(currentSection.toFloat()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.bar)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        // 章节进度滑条
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("进度", color = theme.text, fontSize = 13.sp)
            Slider(
                value = seek,
                onValueChange = { seek = it },
                onValueChangeFinished = { onSeek(seek.roundToInt()) },
                valueRange = 0f..(sectionCount - 1).coerceAtLeast(1).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = theme.divider,
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            Text(
                "${seek.roundToInt() + 1}章",
                color = theme.text,
                fontSize = 12.sp,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 字号
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", color = theme.text, fontSize = 13.sp)
            Slider(
                value = ReaderPrefs.fontSize,
                onValueChange = { ReaderPrefs.fontSize = it },
                valueRange = 16f..28f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = theme.divider,
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
            )
            Text("A", color = theme.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 行距
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("行距", color = theme.text, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(12.dp))
            listOf(1.2f, 1.5f, 1.8f).forEach { sp ->
                val active = ReaderPrefs.lineSpacing == sp
                Text(
                    if (sp == 1.2f) "紧凑" else if (sp == 1.5f) "标准" else "宽松",
                    color = if (active) accent else theme.text,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { ReaderPrefs.lineSpacing = sp }
                        .border(1.dp, if (active) accent else theme.divider, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        // 背景
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("背景", color = theme.text, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(12.dp))
            READER_THEMES.forEachIndexed { i, t ->
                val active = ReaderPrefs.themeIdx == i
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .width(34.dp)
                        .height(34.dp)
                        .background(t.bg, CircleShape)
                        .border(
                            2.dp,
                            if (active) accent else t.divider,
                            if (active) RoundedCornerShape(50) else CircleShape,
                        )
                        .clickable { ReaderPrefs.themeIdx = i },
                    contentAlignment = Alignment.Center,
                ) {
                    if (active) {
                        Text("✓", color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "完成",
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onDismiss).padding(6.dp),
            )
        }
    }
}

/** 章节目录弹层（打开时自动定位到当前章） */
@Composable
private fun TocPanel(
    book: Book,
    current: Int,
    theme: ReaderTheme,
    onPick: (Int) -> Unit,
    onClose: () -> Unit,
) {
    val accent = if (theme.dark) Color(0xFFFF6F5E) else Color(0xFFFF4C40)
    val tocState = rememberLazyListState()
    LaunchedEffect(book) {
        if (current > 3) tocState.scrollToItem((current - 1).coerceAtLeast(0))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg.copy(alpha = 0.55f))
            .clickable(onClick = onClose, indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .background(theme.bar)
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { }
                .padding(top = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("目录", color = theme.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text("共 ${book.sections.size} 章", color = theme.quote, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(state = tocState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(book.sections) { si, sec ->
                    val active = si == current
                    Text(
                        sec.title.ifBlank { book.title },
                        color = if (active) accent else theme.text,
                        fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(si) }
                            .padding(horizontal = 18.dp, vertical = 11.dp),
                    )
                }
            }
        }
    }
}
