package zhiqiu.app.destiny.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import zhiqiu.app.destiny.oinkcompat.AlertDialog
import zhiqiu.app.destiny.oinkcompat.HorizontalDivider
import zhiqiu.app.destiny.oinkcompat.Icon
import zhiqiu.app.destiny.oinkcompat.Text
import zhiqiu.app.destiny.oinkcompat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Image
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.User
import zhiqiu.app.destiny.db.ProfileImage
import zhiqiu.app.destiny.i18n.AppStrings
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.time.TIME_INDEX_LABELS
import zhiqiu.app.destiny.ui.LocalImage
import zhiqiu.app.destiny.ui.theme.DestinyTheme

val MEMO_IMAGE_GROUPS = listOf(
    "bazi" to "八字",
    "ziwei" to "紫微",
    "qizheng" to "七政",
)

@Composable
fun NoteSaveStatusLabel(
    status: NoteSaveStatus,
    modifier: Modifier = Modifier,
    light: Boolean = true,
) {
    val theme = DestinyTheme
    val (text, color) = when (status) {
        NoteSaveStatus.Idle -> return
        NoteSaveStatus.Dirty -> "未保存" to if (light) theme.muted else Color(0xFF9A948C)
        NoteSaveStatus.Saving -> "保存中…" to if (light) theme.accent else Color(0xFFC9A66B)
        NoteSaveStatus.Saved -> "已保存" to if (light) theme.accent else Color(0xFFC9A66B)
    }
    Text(text, color = color, fontSize = 11.sp, modifier = modifier)
}

@Composable
fun ProfileNoteBanner(profile: Profile, modifier: Modifier = Modifier) {
    val theme = DestinyTheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2520))
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(theme.danger.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FeatherIcons.User,
                    contentDescription = null,
                    tint = Color(0xFFE8D5D5),
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    profile.name.ifBlank { "未命名" },
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    buildString {
                        append(profile.solarDateDisplay.ifBlank { profile.birthday.ifBlank { "无生日" } })
                        append(" · ")
                        append(TIME_INDEX_LABELS.getOrElse(profile.timeIndex) { "未知" })
                        if (profile.baziSummary.isNotBlank()) {
                            append(" · ")
                            append(profile.baziSummary)
                        }
                    },
                    color = Color(0xFFB8B0A8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
        if (profile.groupName.isNotBlank() && profile.groupName != "默认") {
            Spacer(Modifier.height(8.dp))
            Text(
                profile.groupName,
                color = theme.danger.copy(alpha = 0.9f),
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(theme.danger.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun NoteSectionLabel(text: String, modifier: Modifier = Modifier) {
    val theme = DestinyTheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            color = theme.danger,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = theme.line, thickness = 0.5.dp)
    }
}

/** 总批注编辑框。dark = Sheet 深色，否则 Tab 浅色。 */
@Composable
fun NoteTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    minHeight: Dp = 140.dp,
    placeholder: String = "命局总论、流年要点、待验证的断语…",
) {
    val ink = if (dark) Color(0xFFF2EDE6) else DestinyTheme.ink
    val muted = if (dark) Color(0xFF9A948C) else DestinyTheme.muted
    val surface = if (dark) Color(0xFF2B2826) else Color(0xFFFFFCF7)
    val border = if (dark) Color(0xFF3A3632) else DestinyTheme.line

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(border)
            .padding(1.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(surface),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(14.dp),
            textStyle = TextStyle(color = ink, fontSize = 15.sp, lineHeight = 23.sp),
            cursorBrush = SolidColor(if (dark) Color(0xFFC9A66B) else DestinyTheme.danger),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, color = muted, fontSize = 15.sp, lineHeight = 23.sp)
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
fun NoteAttachmentStrip(
    images: List<ProfileImage>,
    resolvePath: (ProfileImage) -> String,
    onAdd: () -> Unit,
    onDelete: (ProfileImage) -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    emptyHint: String = "点左侧添加截图或照片",
    compact: Boolean = false,
) {
    var previewPath by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<ProfileImage?>(null) }
    val theme = DestinyTheme
    val muted = if (dark) Color(0xFF9A948C) else theme.muted
    val addBg = if (dark) Color(0xFF2B2826) else Color(0xFFEBE4D8)
    val tileW = if (compact) 64.dp else 88.dp

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
        ) {
            item {
                Box(
                    modifier = Modifier
                        .width(tileW)
                        .then(if (compact) Modifier.size(tileW) else Modifier.aspectRatio(0.75f))
                        .clip(RoundedCornerShape(6.dp))
                        .background(addBg)
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = FeatherIcons.Image,
                            contentDescription = "添加图片",
                            tint = muted,
                            modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                        )
                        if (!compact) {
                            Spacer(Modifier.height(4.dp))
                            Text("添加", color = muted, fontSize = 11.sp)
                        }
                    }
                }
            }
            items(images, key = { it.id }) { image ->
                val path = resolvePath(image)
                Box(
                    modifier = Modifier
                        .width(tileW)
                        .then(if (compact) Modifier.size(tileW) else Modifier.aspectRatio(0.75f))
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { previewPath = path },
                ) {
                    LocalImage(
                        absolutePath = path,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(if (compact) 18.dp else 22.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { pendingDelete = image },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Trash2,
                            contentDescription = AppStrings.delete,
                            tint = Color.White,
                            modifier = Modifier.size(if (compact) 10.dp else 12.dp),
                        )
                    }
                }
            }
        }
        if (images.isEmpty()) {
            Text(
                emptyHint,
                color = muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }

    pendingDelete?.let { image ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除附图？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(image)
                        pendingDelete = null
                    },
                ) { Text(AppStrings.delete, color = DestinyTheme.danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(AppStrings.cancel)
                }
            },
        )
    }

    zhiqiu.app.destiny.ui.ImagePreviewOverlay(
        absolutePath = previewPath,
        onDismiss = { previewPath = null },
    )
}
