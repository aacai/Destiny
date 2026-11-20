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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.abs
import kotlin.math.roundToInt
import zhiqiu.app.destiny.i18n.AppStrings
import zhiqiu.app.destiny.oinkcompat.Icon
import zhiqiu.app.destiny.oinkcompat.IconButton
import zhiqiu.app.destiny.oinkcompat.Slider
import zhiqiu.app.destiny.oinkcompat.SliderDefaults
import zhiqiu.app.destiny.oinkcompat.Text

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

/** 0=连续滚动 1=左右翻页 */
private const val TURN_SCROLL = 0
private const val TURN_PAGE = 1

/** 翻页动画：0=滑动 1=淡入 2=无 */
private const val ANIM_SLIDE = 0
private const val ANIM_FADE = 1
private const val ANIM_NONE = 2

/** 阅读偏好 + 每本书章节进度（持久化到 Room 键值表，跨启动生效） */
object ReaderPrefs {
    private var store: ReaderStore? = null

    private val fontSizeState = mutableFloatStateOf(20f)
    private val lineSpacingState = mutableFloatStateOf(1.5f)
    private val themeIdxState = mutableIntStateOf(1)
    private val turnModeState = mutableIntStateOf(TURN_SCROLL)
    private val pageAnimState = mutableIntStateOf(ANIM_SLIDE)

    var fontSize: Float
        get() = fontSizeState.floatValue
        set(v) {
            fontSizeState.floatValue = v.coerceIn(14f, 32f)
            store?.put("reader.fontSize", fontSizeState.floatValue.toString())
        }
    var lineSpacing: Float
        get() = lineSpacingState.floatValue
        set(v) {
            lineSpacingState.floatValue = v.coerceIn(1f, 2.2f)
            store?.put("reader.lineSpacing", lineSpacingState.floatValue.toString())
        }
    var themeIdx: Int
        get() = themeIdxState.intValue
        set(v) {
            themeIdxState.intValue = v.coerceIn(0, READER_THEMES.size - 1)
            store?.put("reader.themeIdx", themeIdxState.intValue.toString())
        }
    var turnMode: Int
        get() = turnModeState.intValue
        set(v) {
            turnModeState.intValue = v.coerceIn(0, 1)
            store?.put("reader.turnMode", turnModeState.intValue.toString())
        }
    var pageAnim: Int
        get() = pageAnimState.intValue
        set(v) {
            pageAnimState.intValue = v.coerceIn(0, 2)
            store?.put("reader.pageAnim", pageAnimState.intValue.toString())
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
        s.get("reader.turnMode")?.toIntOrNull()?.let { turnModeState.intValue = it.coerceIn(0, 1) }
        s.get("reader.pageAnim")?.toIntOrNull()?.let { pageAnimState.intValue = it.coerceIn(0, 2) }
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
    var loadFailed by remember { mutableStateOf(false) }
    // 顶栏/底栏默认显示（含返回键），点内容区中央可切换沉浸
    var menuVisible by remember { mutableStateOf(true) }
    var showToc by remember { mutableStateOf(false) }
    var showAa by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var prefsReady by remember { mutableStateOf(false) }
    LaunchedEffect(readerStore) {
        ReaderPrefs.attach(readerStore)
        prefsReady = true
    }

    LaunchedEffect(bookId) {
        loadFailed = false
        book = null
        val loaded = BookStore.loadBook(bookId)
        book = loaded
        loadFailed = loaded == null
    }
    val b = book

    if (!prefsReady) {
        Box(
            modifier = Modifier.fillMaxSize().background(READER_THEMES[1].bg),
            contentAlignment = Alignment.Center,
        ) {
            Text("准备阅读偏好…", color = READER_THEMES[1].text, fontSize = 14.sp)
        }
        return
    }

    val theme = READER_THEMES[ReaderPrefs.themeIdx]

    if (b == null) {
        Box(modifier = Modifier.fillMaxSize().background(theme.bg)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                Icon(FeatherIcons.ArrowLeft, contentDescription = AppStrings.back, tint = theme.text)
            }
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (loadFailed) "无法打开此书" else "加载中…",
                    color = theme.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (loadFailed) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "正文资源缺失或解析失败。书目在包内，可返回书架重试。",
                        color = theme.quote,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        return
    }

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

    val pageMode = ReaderPrefs.turnMode == TURN_PAGE
    // 按字号估算每页容量；翻页模式用
    val charsPerPage = remember(ReaderPrefs.fontSize, ReaderPrefs.lineSpacing) {
        val base = 980f * (18f / ReaderPrefs.fontSize) * (1.5f / ReaderPrefs.lineSpacing)
        base.roundToInt().coerceIn(320, 1400)
    }
    val pages = remember(items, charsPerPage, pageMode) {
        if (!pageMode) emptyList() else paginateItems(items, charsPerPage)
    }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    val currentSection by remember {
        derivedStateOf {
            if (pageMode) {
                val pageItems = pages.getOrNull(pagerState.currentPage).orEmpty()
                pageItems.firstOrNull()?.sectionIdx ?: 0
            } else {
                listState.firstVisibleItemIndex
                    .let { first -> items.getOrNull(first)?.sectionIdx ?: 0 }
            }
        }
    }
    val readPercent by remember {
        derivedStateOf {
            if (pageMode) {
                if (pages.isEmpty()) 0
                else (((pagerState.currentPage + 1).toFloat() / pages.size) * 100).toInt().coerceIn(0, 100)
            } else {
                if (items.isEmpty()) 0
                else ((listState.firstVisibleItemIndex.toFloat() / items.size) * 100).toInt().coerceIn(0, 100)
            }
        }
    }

    LaunchedEffect(b, prefsReady, pageMode) {
        if (!prefsReady) return@LaunchedEffect
        val idx = ReaderPrefs.loadLastSection(bookId)
        if (pageMode) {
            val target = pages.indexOfFirst { pg -> pg.any { it.sectionIdx == idx } }.coerceAtLeast(0)
            if (target in pages.indices) pagerState.scrollToPage(target)
        } else if (idx > 0 && idx < anchors.size) {
            listState.scrollToItem(anchors[idx])
        }
    }
    LaunchedEffect(currentSection) {
        delay(1000)
        ReaderPrefs.saveLastSection(bookId, currentSection)
    }

    fun jumpSection(si: Int, animated: Boolean = true) {
        val clamped = si.coerceIn(0, b.sections.size - 1)
        ReaderPrefs.saveLastSection(bookId, clamped)
        scope.launch {
            if (pageMode) {
                val target = pages.indexOfFirst { pg -> pg.any { it.sectionIdx == clamped } }.coerceAtLeast(0)
                goToPage(pagerState, target, animated && ReaderPrefs.pageAnim != ANIM_NONE)
            } else {
                val a = anchors.getOrNull(clamped) ?: return@launch
                if (animated) listState.animateScrollToItem(a) else listState.scrollToItem(a)
            }
        }
    }

    fun turnPage(delta: Int) {
        if (!pageMode) {
            jumpSection(currentSection + delta)
            return
        }
        val target = (pagerState.currentPage + delta).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        scope.launch {
            goToPage(pagerState, target, ReaderPrefs.pageAnim != ANIM_NONE)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(theme.bg),
    ) {
        AnimatedVisibility(visible = menuVisible, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.bar)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(FeatherIcons.ArrowLeft, contentDescription = AppStrings.back, tint = theme.text)
                }
                Text(
                    b.title,
                    color = theme.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showToc = !showToc; showAa = false }) {
                    Icon(FeatherIcons.List, contentDescription = AppStrings.contents, tint = theme.text)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(pageMode, pages.size, b.sections.size) {
                    detectTapGestures { offset ->
                        when {
                            offset.x < size.width / 3f -> turnPage(-1)
                            offset.x > size.width * 2f / 3f -> turnPage(1)
                            else -> {
                                menuVisible = !menuVisible
                                showAa = false
                                showToc = false
                            }
                        }
                    }
                },
        ) {
            if (pageMode) {
                PageTurnContent(
                    book = b,
                    pages = pages,
                    pagerState = pagerState,
                    theme = theme,
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
                ) {
                    itemsIndexed(items) { _, item ->
                        ReaderBlock(book = b, item = item, theme = theme)
                    }
                }
            }

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

        AnimatedVisibility(visible = menuVisible, enter = fadeIn(), exit = fadeOut()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (showAa) {
                    AaPanel(
                        theme = theme,
                        sectionCount = b.sections.size,
                        currentSection = currentSection,
                        pageCount = pages.size,
                        currentPage = pagerState.currentPage,
                        pageMode = pageMode,
                        onSeek = { si -> jumpSection(si, animated = false) },
                        onSeekPage = { p ->
                            scope.launch { goToPage(pagerState, p, false) }
                        },
                        onDismiss = { showAa = false },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.bar)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton2(if (pageMode) "上一页" else "上一章", theme) { turnPage(-1) }
                    TextButton2(AppStrings.contents, theme) { showToc = !showToc; showAa = false }
                    Text(
                        if (pageMode) {
                            "${pagerState.currentPage + 1}/${pages.size.coerceAtLeast(1)} · $readPercent%"
                        } else {
                            "${currentSection + 1}/${b.sections.size} · $readPercent%"
                        },
                        color = theme.text,
                        fontSize = 12.sp,
                    )
                    IconButton(onClick = { showAa = !showAa; showToc = false }) {
                        Icon(FeatherIcons.Type, contentDescription = AppStrings.settings, tint = theme.text)
                    }
                    TextButton2(if (pageMode) "下一页" else "下一章", theme) { turnPage(1) }
                }
            }
        }
    }
}

private suspend fun goToPage(pagerState: PagerState, page: Int, animated: Boolean) {
    val target = page.coerceIn(0, (pagerState.pageCount - 1).coerceAtLeast(0))
    if (animated) pagerState.animateScrollToPage(target) else pagerState.scrollToPage(target)
}

/** 按字符预算把扁平条目切成页 */
private fun paginateItems(items: List<ReaderItem>, charsPerPage: Int): List<List<ReaderItem>> {
    if (items.isEmpty()) return listOf(emptyList())
    val pages = mutableListOf<List<ReaderItem>>()
    var bucket = mutableListOf<ReaderItem>()
    var used = 0
    items.forEach { item ->
        val cost = if (item.isHeader) 40 else (item.text?.length ?: 0).coerceAtLeast(20)
        if (bucket.isNotEmpty() && used + cost > charsPerPage) {
            pages += bucket
            bucket = mutableListOf()
            used = 0
        }
        bucket += item
        used += cost
    }
    if (bucket.isNotEmpty()) pages += bucket
    return pages
}

@Composable
private fun PageTurnContent(
    book: Book,
    pages: List<List<ReaderItem>>,
    pagerState: PagerState,
    theme: ReaderTheme,
) {
    if (pages.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无正文", color = theme.quote, fontSize = 14.sp)
        }
        return
    }
    val fade = ReaderPrefs.pageAnim == ANIM_FADE
    // 「无动画」只影响点按瞬切（goToPage 用 scrollToPage）；左右滑仍走系统 pager
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true,
    ) { page ->
        val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (fade) Modifier.graphicsLayer { alpha = (1f - abs(offset)).coerceIn(0.2f, 1f) }
                    else Modifier,
                )
                .padding(horizontal = 22.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Column {
                pages[page].forEach { item ->
                    ReaderBlock(book = book, item = item, theme = theme)
                }
            }
        }
    }
}

@Composable
private fun ReaderBlock(book: Book, item: ReaderItem, theme: ReaderTheme) {
    val sec = book.sections[item.sectionIdx]
    if (item.isHeader) {
        Text(
            sec.title.ifBlank { book.title },
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

/** Aa 设置：字号 / 行距 / 翻页方式 / 翻页动画 / 背景 */
@Composable
private fun AaPanel(
    theme: ReaderTheme,
    sectionCount: Int,
    currentSection: Int,
    pageCount: Int,
    currentPage: Int,
    pageMode: Boolean,
    onSeek: (Int) -> Unit,
    onSeekPage: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val accent = if (theme.dark) Color(0xFFFF6F5E) else Color(0xFFFF4C40)
    var seek by remember(currentSection) { mutableFloatStateOf(currentSection.toFloat()) }
    var pageSeek by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.bar)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        // 进度
        if (pageMode && pageCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("页码", color = theme.text, fontSize = 13.sp)
                Slider(
                    value = pageSeek,
                    onValueChange = { pageSeek = it },
                    onValueChangeFinished = { onSeekPage(pageSeek.roundToInt()) },
                    valueRange = 0f..(pageCount - 1).coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = theme.divider,
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                )
                Text("${pageSeek.roundToInt() + 1}页", color = theme.text, fontSize = 12.sp)
            }
        } else {
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
                Text("${seek.roundToInt() + 1}章", color = theme.text, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // 字号：A- / 滑块 / A+
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "A−",
                color = theme.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { ReaderPrefs.fontSize = ReaderPrefs.fontSize - 1f }
                    .padding(8.dp),
            )
            Slider(
                value = ReaderPrefs.fontSize,
                onValueChange = { ReaderPrefs.fontSize = it },
                valueRange = 14f..32f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = theme.divider,
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            Text(
                "A+",
                color = theme.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { ReaderPrefs.fontSize = ReaderPrefs.fontSize + 1f }
                    .padding(8.dp),
            )
            Text(
                "${ReaderPrefs.fontSize.roundToInt()}",
                color = theme.quote,
                fontSize = 12.sp,
                modifier = Modifier.width(28.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // 行距
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("行距", color = theme.text, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(12.dp))
            listOf(1.2f to "紧凑", 1.5f to "标准", 1.8f to "宽松").forEach { (sp, label) ->
                val active = abs(ReaderPrefs.lineSpacing - sp) < 0.05f
                Text(
                    label,
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

        // 翻页方式
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("翻页", color = theme.text, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(12.dp))
            listOf(TURN_SCROLL to "连续滚动", TURN_PAGE to "左右翻页").forEach { (mode, label) ->
                val active = ReaderPrefs.turnMode == mode
                Text(
                    label,
                    color = if (active) accent else theme.text,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { ReaderPrefs.turnMode = mode }
                        .border(1.dp, if (active) accent else theme.divider, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        if (pageMode) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("动画", color = theme.text, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(12.dp))
                listOf(ANIM_SLIDE to "滑动", ANIM_FADE to "淡入", ANIM_NONE to "无").forEach { (anim, label) ->
                    val active = ReaderPrefs.pageAnim == anim
                    Text(
                        label,
                        color = if (active) accent else theme.text,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { ReaderPrefs.pageAnim = anim }
                            .border(1.dp, if (active) accent else theme.divider, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
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
                AppStrings.done,
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
            .clickable(
                onClick = onClose,
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .background(theme.bar)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                ) { }
                .padding(top = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(AppStrings.contents, color = theme.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
