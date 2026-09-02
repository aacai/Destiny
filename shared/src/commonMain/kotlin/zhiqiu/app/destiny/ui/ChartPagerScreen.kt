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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.profile.ProfileRepository
import zhiqiu.app.destiny.ui.bazi.BaziSection
import zhiqiu.app.destiny.ui.qizheng.QizhengSection
import zhiqiu.iztro.ui.Iztrolabe

private val TAB_CATEGORY = listOf("bazi", "ziwei", "qizheng")
private val TAB_LABEL = listOf("八字", "紫微", "七政")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartPagerScreen(
    profile: Profile,
    repository: ProfileRepository,
    onBack: () -> Unit,
    onSaveQizhengPanZhi: (String) -> Unit = {},
) {
    var tab by remember { mutableIntStateOf(0) }
    var drawerOpen by remember { mutableStateOf(false) }
    var noteDraft by remember(profile.id) { mutableStateOf(profile.note) }
    var pickCategory by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tab) { drawerOpen = false }
    LaunchedEffect(profile.note) {
        if (!drawerOpen) noteDraft = profile.note
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
                scope.launch(Dispatchers.IO) {
                    val bytes = runCatching { file.readBytes() }.getOrNull() ?: return@launch
                    val ext = file.name.substringAfterLast('.', "")
                    repository.addImage(profile.id, cat, bytes, ext)
                }
            }
        },
    )

    fun saveNote() {
        val trimmed = noteDraft.trim()
        if (trimmed == profile.note) return
        scope.launch(Dispatchers.IO) {
            repository.upsert(profile.copy(note = trimmed))
        }
    }

    fun closeDrawer() {
        saveNote()
        drawerOpen = false
    }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(44.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(start = 4.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IosBackButton(onClick = onBack)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        profile.name.ifBlank { "原盘" },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2DED2))
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(FeatherIcons.BookOpen, contentDescription = "八字") },
                    label = { Text("八字") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(FeatherIcons.Star, contentDescription = "紫微") },
                    label = { Text("紫微") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(FeatherIcons.Compass, contentDescription = "七政") },
                    label = { Text("七政") },
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(FeatherIcons.FileText, contentDescription = "批注") },
                    label = { Text("批注") },
                )
            }
        },
        floatingActionButton = {
            if (tab in 0..2 && !drawerOpen) {
                FloatingActionButton(
                    onClick = {
                        noteDraft = profile.note
                        drawerOpen = true
                    },
                    containerColor = Color(0xFF8B3A3A),
                    contentColor = Color.White,
                ) {
                    Icon(
                        imageVector = FeatherIcons.Edit,
                        contentDescription = "批注",
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (tab) {
                0 -> BaziSection(profile)
                1 -> Iztrolabe(
                    birthday = profile.birthday,
                    birthTime = profile.timeIndex,
                    gender = profile.gender,
                    birthdayType = profile.birthdayType,
                    isLeapMonth = profile.isLeapMonth,
                    fixLeap = profile.fixLeap,
                    name = profile.name,
                    modifier = Modifier.fillMaxSize(),
                )
                2 -> QizhengSection(
                    profile = profile,
                    onSavePanZhi = onSaveQizhengPanZhi,
                )
                else -> MemoTabScreen(
                    profile = profile,
                    imagesByCategory = imagesByCategory,
                    resolvePath = { image -> repository.imageAbsolutePath(image.relativePath) },
                    onNoteSaved = { text ->
                        scope.launch(Dispatchers.IO) {
                            repository.upsert(profile.copy(note = text))
                        }
                    },
                    onPickImage = { cat ->
                        pickCategory = cat
                        pickerLauncher.launch()
                    },
                    onDeleteImage = { image ->
                        scope.launch(Dispatchers.IO) { repository.removeImage(image) }
                    },
                )
            }

            if (drawerOpen && category != null) {
                ChartMemoDrawer(
                    sheetTitle = "${TAB_LABEL[tab]}便笺",
                    note = noteDraft,
                    images = tabImages,
                    resolvePath = { image -> repository.imageAbsolutePath(image.relativePath) },
                    onNoteChange = { noteDraft = it },
                    onPickImage = {
                        pickCategory = category
                        pickerLauncher.launch()
                    },
                    onDeleteImage = { image ->
                        scope.launch(Dispatchers.IO) { repository.removeImage(image) }
                    },
                    onClose = ::closeDrawer,
                )
            }
        }
    }
}
