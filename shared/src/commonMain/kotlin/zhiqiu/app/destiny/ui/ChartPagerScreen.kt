package zhiqiu.app.destiny.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.BookOpen
import compose.icons.feathericons.Compass
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

/** 各 tab 对应的图片分类名（与 [zhiqiu.app.destiny.db.ProfileImage.category] 一致）；批注 tab 无图片分类 */
private val TAB_CATEGORY = listOf("bazi", "ziwei", "qizheng")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartPagerScreen(
    profile: Profile,
    repository: ProfileRepository,
    onBack: () -> Unit,
    onSaveQizhengPanZhi: (String) -> Unit = {},
) {
    var tab by remember { mutableIntStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val category = TAB_CATEGORY.getOrNull(tab)
    val images by (if (category != null) {
        repository.observeImages(profile.id, category)
    } else {
        flowOf(emptyList())
    }).collectAsState(initial = emptyList())

    val pickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        onResult = { file ->
            if (file != null) {
                scope.launch(Dispatchers.IO) {
                    val cat = category ?: return@launch
                    val bytes = runCatching { file.readBytes() }.getOrNull() ?: return@launch
                    val ext = file.name.substringAfterLast('.', "")
                    repository.addImage(profile.id, cat, bytes, ext)
                }
            }
        },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile.name.ifBlank { "原盘" }) },
                navigationIcon = {
                    IosBackButton(onClick = onBack)
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = {
                        Icon(
                            imageVector = FeatherIcons.BookOpen,
                            contentDescription = "八字",
                        )
                    },
                    label = { Text("八字") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = {
                        Icon(
                            imageVector = FeatherIcons.Star,
                            contentDescription = "紫微",
                        )
                    },
                    label = { Text("紫微") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = {
                        Icon(
                            imageVector = FeatherIcons.Compass,
                            contentDescription = "七政",
                        )
                    },
                    label = { Text("七政") },
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = {
                        Icon(
                            imageVector = FeatherIcons.FileText,
                            contentDescription = "批注",
                        )
                    },
                    label = { Text("批注") },
                )
            }
        },
        floatingActionButton = {
            if (tab == 3) {
                FloatingActionButton(
                    onClick = { showEditor = true },
                    containerColor = Color(0xFF1B5E20),
                ) {
                    Icon(
                        imageVector = FeatherIcons.FileText,
                        contentDescription = "编辑批注",
                        tint = Color.White,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (category != null) {
                ImageGallery(
                    images = images,
                    absolutePathOf = { repository.imageAbsolutePath(it.relativePath) },
                    onAdd = { pickerLauncher.launch() },
                    onRemove = { img ->
                        scope.launch(Dispatchers.IO) { repository.removeImage(img) }
                    },
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                    else -> AnnotationSection(profile = profile)
                }
            }
        }
    }

    if (showEditor) {
        AnnotationEditorDialog(
            profile = profile,
            onSave = { updated ->
                scope.launch(Dispatchers.IO) { repository.upsert(updated) }
                showEditor = false
            },
            onDismiss = { showEditor = false },
        )
    }
}
