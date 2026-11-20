package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import zhiqiu.app.destiny.oinkcompat.HorizontalDivider
import zhiqiu.app.destiny.oinkcompat.Icon
import zhiqiu.app.destiny.oinkcompat.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import compose.icons.FeatherIcons
import compose.icons.feathericons.BookOpen
import compose.icons.feathericons.Compass
import compose.icons.feathericons.Edit
import compose.icons.feathericons.FileText
import compose.icons.feathericons.Star
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import zhiqiu.app.destiny.db.ProfileImage
import zhiqiu.app.destiny.platform.AppBackHandler
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.profile.ProfileRepository
import zhiqiu.app.destiny.ui.bazi.BaziSection
import zhiqiu.app.destiny.ui.note.rememberNoteEditorState
import zhiqiu.app.destiny.ui.qizheng.QizhengSection
import zhiqiu.app.destiny.ui.theme.DestinyColorMode
import zhiqiu.app.destiny.ui.theme.DestinyTheme
import zhiqiu.app.destiny.ui.theme.LocalDestinyAppThemes
import zhiqiu.iztro.ui.IztroColorMode
import zhiqiu.iztro.ui.Iztrolabe

private val TAB_CATEGORY = listOf("bazi", "ziwei", "qizheng")
private val TAB_LABEL = listOf("八字", "紫微", "七政")

@Composable
fun ChartPagerScreen(
    profile: Profile,
    repository: ProfileRepository,
    onBack: () -> Unit,
    colorMode: DestinyColorMode = DestinyColorMode.Light,
    onColorModeChange: (DestinyColorMode) -> Unit = {},
    onSaveQizhengPanZhi: (String) -> Unit = {},
) {
    var tab by remember { mutableIntStateOf(if (profile.birthday.isBlank()) 3 else 0) }
    var sheetOpen by remember { mutableStateOf(false) }
    var pickCategory by remember { mutableStateOf<String?>(null) }
    // 八字/紫微首次进入后保活，避免每次切 Tab 整盘销毁重建
    var baziMounted by remember(profile.id) { mutableStateOf(false) }
    var ziweiMounted by remember(profile.id) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val theme = DestinyTheme
    val iztroMode = if (colorMode == DestinyColorMode.Dark) IztroColorMode.Dark else IztroColorMode.Light
    val iztroThemeConfig = LocalDestinyAppThemes.current.iztro
    val hasBirth = profile.birthday.isNotBlank()

    val noteState = rememberNoteEditorState(
        profileId = profile.id,
        savedNote = profile.note,
        onSave = { text ->
            // 仅更新 note，避免无意义字段变化；已在 Default 线程
            repository.upsert(profile.copy(note = text))
        },
    )

    LaunchedEffect(tab, hasBirth) {
        if (tab == 0 && hasBirth) baziMounted = true
        if (tab == 1 && hasBirth) ziweiMounted = true
    }

    // 离开盘面页时 flush 批注
    DisposableEffect(profile.id) {
        onDispose { noteState.flush() }
    }

    val category = TAB_CATEGORY.getOrNull(tab)
    val tabImages by (if (category != null) {
        repository.observeImages(profile.id, category)
    } else {
        flowOf(emptyList())
    }).collectAsState(initial = emptyList())

    val allImages by repository.observeImages(profile.id).collectAsState(initial = emptyList())
    val imagesByCategory = remember(allImages) { allImages.groupBy { it.category } }

    val pickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        onResult = { file ->
            val cat = pickCategory ?: return@rememberFilePickerLauncher
            pickCategory = null
            if (file != null) {
                scope.launch(Dispatchers.Default) {
                    val bytes = runCatching { file.readBytes() }.getOrNull() ?: return@launch
                    val ext = file.name.substringAfterLast('.', "")
                    repository.addImage(profile.id, cat, bytes, ext)
                }
            }
        },
    )

    fun openFullMemo() {
        noteState.flush()
        sheetOpen = false
        tab = 3
    }

    val resolveImagePath: (ProfileImage) -> String = { image ->
        repository.imageAbsolutePath(image.relativePath)
    }
    val onPickImageCategory: (String) -> Unit = { cat ->
        pickCategory = cat
        pickerLauncher.launch()
    }
    val onDeleteImageAction: (ProfileImage) -> Unit = { image ->
        scope.launch(Dispatchers.Default) { repository.removeImage(image) }
    }

    Box(modifier = Modifier.fillMaxSize().background(theme.page)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(theme.page)
                    .padding(start = 4.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IosBackButton(onClick = {
                    noteState.flush()
                    onBack()
                })
                Spacer(Modifier.width(4.dp))
                Text(
                    profile.name.ifBlank { "原盘" },
                    color = theme.ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = theme.line)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if ((baziMounted || tab == 0) && hasBirth) {
                    val baziVisible = tab == 0
                    BaziSection(
                        profile = profile,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (baziVisible) 1f else 0f)
                            .graphicsLayer { alpha = if (baziVisible) 1f else 0f }
                            .then(if (baziVisible) Modifier else Modifier.size(0.dp)),
                    )
                }
                if ((ziweiMounted || tab == 1) && hasBirth) {
                    val ziweiVisible = tab == 1
                    Iztrolabe(
                        birthday = profile.birthday,
                        birthTime = profile.timeIndex,
                        gender = profile.gender,
                        birthdayType = profile.birthdayType,
                        isLeapMonth = profile.isLeapMonth,
                        fixLeap = profile.fixLeap,
                        name = profile.name,
                        colorMode = iztroMode,
                        onColorModeChange = { mode ->
                            onColorModeChange(
                                if (mode == IztroColorMode.Dark) DestinyColorMode.Dark else DestinyColorMode.Light,
                            )
                        },
                        themeConfig = iztroThemeConfig,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (ziweiVisible) 1f else 0f)
                            .graphicsLayer { alpha = if (ziweiVisible) 1f else 0f }
                            .then(if (ziweiVisible) Modifier else Modifier.size(0.dp)),
                    )
                }
                when (tab) {
                    0, 1 -> if (!hasBirth) {
                        MemoTabScreen(
                            profile = profile,
                            noteState = noteState,
                            imagesByCategory = imagesByCategory,
                            resolvePath = resolveImagePath,
                            onPickImage = onPickImageCategory,
                            onDeleteImage = onDeleteImageAction,
                        )
                    }
                    2 -> if (hasBirth) {
                        QizhengSection(profile = profile, onSavePanZhi = onSaveQizhengPanZhi)
                    } else {
                        MemoTabScreen(
                            profile = profile,
                            noteState = noteState,
                            imagesByCategory = imagesByCategory,
                            resolvePath = resolveImagePath,
                            onPickImage = onPickImageCategory,
                            onDeleteImage = onDeleteImageAction,
                        )
                    }
                    else -> MemoTabScreen(
                        profile = profile,
                        noteState = noteState,
                        imagesByCategory = imagesByCategory,
                        resolvePath = resolveImagePath,
                        onPickImage = onPickImageCategory,
                        onDeleteImage = onDeleteImageAction,
                    )
                }

            }
            HorizontalDivider(thickness = 0.5.dp, color = theme.line)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(theme.page)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val tabs = buildList {
                    if (profile.birthday.isNotBlank()) {
                        add(0 to ("八字" to FeatherIcons.BookOpen))
                        add(1 to ("紫微" to FeatherIcons.Star))
                        add(2 to ("七政" to FeatherIcons.Compass))
                    }
                    add(3 to ("批注" to FeatherIcons.FileText))
                }
                tabs.forEach { (idx, pair) ->
                    val (label, icon) = pair
                    val selected = tab == idx
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable {
                                // 进批注 Tab 时关掉 Sheet 并 flush
                                if (idx == 3 && sheetOpen) {
                                    noteState.flush()
                                    sheetOpen = false
                                }
                                tab = idx
                            }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (selected) theme.accent else theme.muted,
                        )
                        Text(
                            label,
                            color = if (selected) theme.accent else theme.muted,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
        if (tab in 0..2 && !sheetOpen && profile.birthday.isNotBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 72.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8B3A3A))
                    .clickable { sheetOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FeatherIcons.Edit,
                    contentDescription = "批注",
                    tint = Color.White,
                )
            }
        }

        // Sheet 打开时拦截返回：先关 Sheet，不退出盘面页
        AppBackHandler(enabled = sheetOpen) {
            noteState.flush()
            sheetOpen = false
        }

        // Sheet 盖在整页（含底栏）之上，才能上拉真正全屏写
        if (sheetOpen && category != null) {
            ChartMemoDrawer(
                chartLabel = TAB_LABEL[tab],
                noteState = noteState,
                images = tabImages,
                resolvePath = resolveImagePath,
                onPickImage = {
                    pickCategory = category
                    pickerLauncher.launch()
                },
                onDeleteImage = onDeleteImageAction,
                onOpenFullMemo = ::openFullMemo,
                onClose = { sheetOpen = false },
            )
        }
    }
}
