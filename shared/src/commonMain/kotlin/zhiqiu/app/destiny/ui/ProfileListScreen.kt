package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Plus
import compose.icons.feathericons.BookOpen
import compose.icons.feathericons.Search
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.X
import coil3.compose.AsyncImage
import io.github.vinceglb.filekit.*
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import zhiqiu.app.destiny.db.ReaderPrefEntity
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.sharing.BackupSharing
import zhiqiu.app.destiny.sharing.generateQrCodePng
import zhiqiu.app.destiny.time.timeIndexLabel
import androidx.compose.material3.Checkbox
import androidx.compose.ui.text.input.PasswordVisualTransformation

private val PageBg = Color(0xFFF5F3EE)
private val CardBg = Color(0xFFFFFFFF)
private val Ink = Color(0xFF222222)
private val Muted = Color(0xFF8A8578)
private val Line = Color(0xFFE2DED2)
private val Accent = Color(0xFF26A6A6)
private val MaleDot = Color(0xFF4A90D9)
private val FemaleDot = Color(0xFFE57373)

private enum class SortKey(val label: String) {
    CREATED("创建"), NAME("名字"), BIRTH("生日"),
}

/** 从「公历」串里解析 y/m/d 用于生日排序；解析不了返回 Int.MIN_VALUE 排到最后 */
private fun birthSortValue(profile: Profile): Int {
    val text = profile.solarDateDisplay.ifBlank { profile.birthday }
    val m = Regex("(\\d{4})[-/.年](\\d{1,2})[-/.月](\\d{1,2})").find(text) ?: return Int.MIN_VALUE
    val (y, mo, d) = m.destructured
    return (y.toIntOrNull() ?: 0) * 10000 + (mo.toIntOrNull() ?: 0) * 100 + (d.toIntOrNull() ?: 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    profiles: List<Profile>,
    onAdd: () -> Unit,
    onOpen: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onExportBackup: suspend (password: String?) -> ByteArray,
    onImportBackup: suspend (bytes: ByteArray, password: String?) -> Unit,
    sharing: BackupSharing,
    onOpenBooks: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var sortKey by rememberSaveable { mutableStateOf(SortKey.CREATED) }
    var ascending by rememberSaveable { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var exportMsg by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var importMsg by remember { mutableStateOf("") }
    var showShare by remember { mutableStateOf(false) }
    var showLinkImport by remember { mutableStateOf(false) }
    var linkImportMsg by remember { mutableStateOf("") }
    var shareMsg by remember { mutableStateOf("") }
    var isSharing by remember { mutableStateOf(false) }
    var isLinkImporting by remember { mutableStateOf(false) }
    val shareQr = remember { mutableStateOf<ByteArray?>(null) }
    val exportPayload = remember { mutableStateOf<ByteArray?>(null) }
    val importPassword = remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    val saverLauncher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onResult = { file: PlatformFile? ->
            val payload = exportPayload.value
            if (file != null && payload != null) {
                scope.launch(Dispatchers.IO) {
                    exportMsg = runCatching {
                        file.write(payload)
                        "已导出到 ${file.name}"
                    }.getOrDefault("导出失败")
                }
            }
        },
    )
    val pickerLauncher = rememberFilePickerLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        type = FileKitType.File(extensions = listOf("zip")),
        onResult = { file: PlatformFile? ->
            if (file != null) {
                scope.launch(Dispatchers.IO) {
                    val bytes = runCatching { file.readBytes() }.getOrNull()
                    importMsg = if (bytes == null) {
                        "读取文件失败"
                    } else {
                        runCatching {
                            onImportBackup(bytes, importPassword.value.ifBlank { null })
                            "导入成功"
                        }.getOrElse { "导入失败：${it.message}" }
                    }
                }
            }
        },
    )

    val q = query.trim()
    val filtered = profiles.filter { p ->
        q.isBlank() ||
            p.name.contains(q, ignoreCase = true) ||
            p.birthday.contains(q, ignoreCase = true) ||
            p.solarDateDisplay.contains(q, ignoreCase = true) ||
            p.baziSummary.contains(q, ignoreCase = true)
    }
    val sorted = when (sortKey) {
        SortKey.NAME ->
            if (ascending) filtered.sortedBy { it.name } else filtered.sortedByDescending { it.name }
        SortKey.BIRTH ->
            if (ascending) filtered.sortedBy { birthSortValue(it) } else filtered.sortedByDescending { birthSortValue(it) }
        SortKey.CREATED ->
            if (ascending) filtered.sortedBy { it.createdAt } else filtered.sortedByDescending { it.createdAt }
    }

    Scaffold(
        containerColor = PageBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("档案", color = Ink, fontWeight = FontWeight.Bold)
                        Text(
                            if (q.isBlank()) "共 ${profiles.size} 个档案"
                            else "匹配 ${sorted.size} / ${profiles.size}",
                            color = Muted,
                            fontSize = 12.sp,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBg),
                actions = {
                    TextButton(onClick = {
                        exportMsg = ""
                        showExport = true
                    }) {
                        Text("导出", color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = {
                        importMsg = ""
                        showImport = true
                    }) {
                        Text("导入", color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = {
                        shareMsg = ""
                        shareQr.value = null
                        showShare = true
                    }) {
                        Text("分享", color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = {
                        linkImportMsg = ""
                        showLinkImport = true
                    }) {
                        Text("导入链接", color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = onOpenBooks) {
                        Icon(
                            imageVector = FeatherIcons.BookOpen,
                            contentDescription = "书架",
                            tint = Accent,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = Accent,
                contentColor = Color.White,
            ) {
                Icon(imageVector = FeatherIcons.Plus, contentDescription = "添加档案")
            }
        },
    ) { padding ->
        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有档案", color = Muted)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onAdd) { Text("添加档案", color = Accent) }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                )
                SortRow(
                    current = sortKey,
                    ascending = ascending,
                    onSelect = { key ->
                        if (sortKey == key) {
                            ascending = !ascending
                        } else {
                            sortKey = key
                            ascending = key != SortKey.BIRTH
                        }
                    },
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (sorted.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("没有匹配「$q」的档案", color = Muted, fontSize = 14.sp)
                            }
                        }
                    }
                    items(sorted, key = { it.id }) { profile ->
                        ProfileRow(
                            profile = profile,
                            onClick = { onOpen(profile) },
                            onDelete = { onDelete(profile) },
                        )
                    }
                }
            }
        }
    }

    if (showExport) {
        ExportDialog(
            profilesCount = profiles.size,
            message = exportMsg,
            onExport = { password ->
                scope.launch(Dispatchers.IO) {
                    exportPayload.value = runCatching { onExportBackup(password.ifBlank { null }) }
                        .getOrNull()
                    if (exportPayload.value != null) {
                        saverLauncher.launch(
                            suggestedName = "destiny-backup",
                            defaultExtension = "zip",
                            allowedExtensions = setOf("zip"),
                        )
                    } else {
                        exportMsg = "导出失败"
                    }
                }
            },
            onClose = {
                showExport = false
                exportMsg = ""
                exportPayload.value = null
            },
        )
    }
    if (showImport) {
        ImportDialog(
            message = importMsg,
            onPickFile = { pw ->
                importPassword.value = pw
                pickerLauncher.launch()
            },
            onClose = {
                showImport = false
                importMsg = ""
            },
        )
    }
    if (showShare) {
        ShareDialog(
            message = shareMsg,
            qrBytes = shareQr.value,
            loading = isSharing,
            onShare = { password ->
                isSharing = true
                scope.launch(Dispatchers.IO) {
                    val result = runCatching {
                        val zip = onExportBackup(password.ifBlank { null })
                        sharing.share(zip)
                    }
                    result.onSuccess { link ->
                        shareQr.value = runCatching { generateQrCodePng(link, cellSize = 8) }.getOrNull()
                        runCatching { clipboard.setText(AnnotatedString(link)) }
                        shareMsg = link
                    }.onFailure { shareMsg = "分享失败：${it.message}" }
                    isSharing = false
                }
            },
            onCopy = { clipboard.setText(AnnotatedString(shareMsg)) },
            onClose = {
                showShare = false
                shareMsg = ""
                shareQr.value = null
            },
        )
    }
    if (showLinkImport) {
        LinkImportDialog(
            message = linkImportMsg,
            loading = isLinkImporting,
            onImport = { link, password ->
                isLinkImporting = true
                scope.launch(Dispatchers.IO) {
                    linkImportMsg = runCatching {
                        val bytes = sharing.fetch(link)
                        onImportBackup(bytes, password.ifBlank { null })
                        "导入成功"
                    }.getOrElse { "导入失败：${it.message}" }
                    isLinkImporting = false
                }
            },
            onClose = {
                showLinkImport = false
                linkImportMsg = ""
            },
        )
    }
}

@Composable
private fun ExportDialog(
    profilesCount: Int,
    message: String,
    onExport: (password: String) -> Unit,
    onClose: () -> Unit,
) {
    var encrypt by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) { Text("关闭", color = Accent) }
        },
        title = { Text("导出数据", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "共 $profilesCount 个档案及批注图片。点击「导出为文件」生成 destiny-backup.zip（含全部图片）。",
                    fontSize = 12.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = encrypt, onCheckedChange = { encrypt = it })
                    Text("加密导出（用密码保护）", fontSize = 12.sp, color = Ink)
                }
                if (encrypt) {
                    Spacer(Modifier.height(4.dp))
                    BasicInputField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "密码",
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    if (password.isBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("请输入密码后再导出", fontSize = 12.sp, color = Muted)
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { onExport(password) },
                    enabled = !encrypt || password.isNotBlank(),
                ) {
                    Text("导出为文件", color = Accent)
                }
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(message, fontSize = 12.sp, color = Accent)
                }
            }
        },
    )
}

@Composable
private fun ImportDialog(
    message: String,
    onPickFile: (String) -> Unit,
    onClose: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) { Text("取消", color = Muted) }
        },
        title = { Text("导入数据", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "点击「选择文件导入」选取 destiny-backup.zip。导入按 id 合并，已存在的档案与图片将被覆盖。",
                    fontSize = 12.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(8.dp))
                BasicInputField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "密码（加密备份填写）",
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { onPickFile(password) }) {
                    Text("选择文件导入", color = Accent)
                }
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(message, fontSize = 12.sp, color = Accent)
                }
            }
        },
    )
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(CardBg, RoundedCornerShape(10.dp))
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = FeatherIcons.Search,
            contentDescription = null,
            tint = Muted,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("搜索名字、日期、四柱…", color = Muted, fontSize = 14.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = Ink),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = FeatherIcons.X,
                contentDescription = "清除",
                tint = Muted,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun SortRow(
    current: SortKey,
    ascending: Boolean,
    onSelect: (SortKey) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SortKey.entries.forEach { key ->
            val active = key == current
            Row(
                modifier = Modifier
                    .clickable { onSelect(key) }
                    .background(
                        if (active) Accent.copy(alpha = 0.12f) else CardBg,
                        RoundedCornerShape(8.dp),
                    )
                    .border(
                        1.dp,
                        if (active) Accent else Line,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    key.label,
                    color = if (active) Accent else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (active) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        if (ascending) "↑" else "↓",
                        color = Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: Profile,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 性别徽章
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    (if (profile.gender == "女") FemaleDot else MaleDot).copy(alpha = 0.15f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (profile.name.ifBlank { "未" }).take(1),
                color = if (profile.gender == "女") FemaleDot else MaleDot,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    profile.name.ifBlank { "未命名" },
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    profile.gender,
                    color = if (profile.gender == "女") FemaleDot else MaleDot,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (profile.groupName.isNotBlank() && profile.groupName != "默认") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "· ${profile.groupName}",
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                "${profile.solarDateDisplay.ifBlank { profile.birthday }} · ${timeIndexLabel(profile.timeIndex)}",
                color = Muted,
                fontSize = 12.sp,
            )
            if (profile.baziSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(profile.baziSummary, color = Accent, fontSize = 12.sp)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = FeatherIcons.Trash2,
                contentDescription = "删除",
                tint = Line,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ShareDialog(
    message: String,
    qrBytes: ByteArray?,
    loading: Boolean = false,
    onShare: (password: String) -> Unit,
    onCopy: () -> Unit,
    onClose: () -> Unit,
) {
    var encrypt by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (message.startsWith("https://")) {
                    TextButton(onClick = onCopy) {
                        Text("复制链接", color = Accent)
                    }
                }
                TextButton(onClick = onClose) {
                    Text("关闭", color = Accent)
                }
            }
        },
        title = { Text("分享到链接", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "生成备份包并上传到 Litterbox，得到一个 72 小时内有效的分享链接（到期后自动删除，请尽快导入）。",
                    fontSize = 12.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = encrypt, onCheckedChange = { encrypt = it })
                    Text("加密备份（用密码保护）", fontSize = 12.sp, color = Ink)
                }
                if (encrypt) {
                    Spacer(Modifier.height(4.dp))
                    BasicInputField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "密码",
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { onShare(password) },
                    enabled = (!encrypt || password.isNotBlank()) && !loading,
                ) {
                    if (loading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Accent,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("正在生成并上传…", color = Accent)
                        }
                    } else {
                        Text("生成分享链接", color = Accent)
                    }
                }
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    if (message.startsWith("https://")) {
                        Text("分享链接（可复制或扫码）：", fontSize = 12.sp, color = Muted)
                        Spacer(Modifier.height(4.dp))
                        SelectionContainer {
                            Text(message, fontSize = 12.sp, color = Ink)
                        }
                        Spacer(Modifier.height(8.dp))
                        qrBytes?.let { bytes ->
                            AsyncImage(
                                model = bytes,
                                contentDescription = "分享二维码",
                                modifier = Modifier
                                    .size(180.dp)
                                    .align(Alignment.CenterHorizontally),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Text("（已自动复制到剪贴板，也可点「复制链接」）", fontSize = 11.sp, color = Muted)
                    } else {
                        Text(message, fontSize = 12.sp, color = Accent)
                    }
                }
            }
        },
    )
}

@Composable
private fun LinkImportDialog(
    message: String,
    loading: Boolean = false,
    onImport: (link: String, password: String) -> Unit,
    onClose: () -> Unit,
) {
    var link by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) { Text("取消", color = Muted) }
        },
        title = { Text("从链接导入", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "粘贴他人分享的 Litterbox 链接（72 小时内有效），下载并导入。",
                    fontSize = 12.sp,
                    color = Muted,
                )
                Spacer(Modifier.height(8.dp))
                BasicInputField(
                    value = link,
                    onValueChange = { link = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "分享链接",
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                BasicInputField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = "密码（加密备份填写）",
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = { onImport(link, password) },
                    enabled = link.isNotBlank() && !loading,
                ) {
                    if (loading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Accent,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("正在下载并导入…", color = Accent)
                        }
                    } else {
                        Text("下载并导入", color = Accent)
                    }
                }
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(message, fontSize = 12.sp, color = Accent)
                }
            }
        },
    )
}

/**
 * 与 Material3 的 OutlinedTextField 等价，但基于 Foundation 的 BasicTextField，
 * 以避开当前引入的 Material3(alpha) 与 Foundation 版本二进制不兼容（CustomStyle.applyStyle）导致的崩溃。
 */
@Composable
private fun BasicInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .border(1.dp, Muted, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        singleLine = singleLine,
        textStyle = textStyle,
        visualTransformation = visualTransformation,
        decorationBox = { innerTextField ->
            Column {
                if (label != null) {
                    Text(label, fontSize = 12.sp, color = Muted)
                    Spacer(Modifier.height(4.dp))
                }
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, fontSize = 13.sp, color = Muted)
                }
                innerTextField()
            }
        },
    )
}
