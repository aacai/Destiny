package zhiqiu.app.destiny.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Image
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.User
import kotlinx.coroutines.delay
import zhiqiu.app.destiny.db.ProfileImage
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.time.TIME_INDEX_LABELS

private val Paper = Color(0xFFF5F0E6)
private val Rule = Color(0xFFD9D0C0)
private val TextMain = Color(0xFF1A1A1A)
private val TextSub = Color(0xFF6B6560)
private val Stamp = Color(0xFF8B3A3A)

private data class MemoGroup(
    val key: String,
    val title: String,
)

private val MEMO_GROUPS = listOf(
    MemoGroup("bazi", "八字"),
    MemoGroup("ziwei", "紫微"),
    MemoGroup("qizheng", "七政"),
)

/**
 * 批注 Tab：命例总批 + 分盘面附图，本页直接编辑、自动保存。
 */
@Composable
fun MemoTabScreen(
    profile: Profile,
    imagesByCategory: Map<String, List<ProfileImage>>,
    resolvePath: (ProfileImage) -> String,
    onNoteSaved: (String) -> Unit,
    onPickImage: (category: String) -> Unit,
    onDeleteImage: (ProfileImage) -> Unit,
) {
    var noteDraft by remember(profile.id) { mutableStateOf(profile.note) }
    var previewPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profile.note) {
        if (profile.note != noteDraft) noteDraft = profile.note
    }
    LaunchedEffect(noteDraft) {
        if (noteDraft == profile.note) return@LaunchedEffect
        delay(600)
        onNoteSaved(noteDraft.trim())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            ProfileBanner(profile)
        }

        item {
            Spacer(Modifier.height(20.dp))
            SectionLabel("总批注")
            OutlinedTextField(
                value = noteDraft,
                onValueChange = { noteDraft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                minLines = 6,
                placeholder = {
                    Text(
                        "命局总论、流年大事、待验证的断语…",
                        color = TextSub,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                    )
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = TextMain,
                ),
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Stamp,
                    unfocusedBorderColor = Rule,
                    cursorColor = Stamp,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFFFFCF7),
                ),
            )
        }

        items(MEMO_GROUPS, key = { it.key }) { group ->
            val imgs = imagesByCategory[group.key].orEmpty()
            Spacer(Modifier.height(28.dp))
            SectionLabel("${group.title}附图")
            AttachmentStrip(
                images = imgs,
                resolvePath = resolvePath,
                onAdd = { onPickImage(group.key) },
                onPreview = { previewPath = it },
                onDelete = onDeleteImage,
            )
        }
    }

    ImagePreviewOverlay(absolutePath = previewPath, onDismiss = { previewPath = null })
}

@Composable
private fun ProfileBanner(profile: Profile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2520))
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Stamp.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    buildString {
                        append(profile.solarDateDisplay.ifBlank { profile.birthday })
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
                color = Stamp.copy(alpha = 0.9f),
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(Stamp.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            color = Stamp,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = Rule, thickness = 0.5.dp)
    }
}

@Composable
private fun AttachmentStrip(
    images: List<ProfileImage>,
    resolvePath: (ProfileImage) -> String,
    onAdd: () -> Unit,
    onPreview: (String) -> Unit,
    onDelete: (ProfileImage) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            AddTile(onClick = onAdd)
        }
        items(images, key = { it.id }) { image ->
            ThumbnailTile(
                path = resolvePath(image),
                onClick = { onPreview(resolvePath(image)) },
                onDelete = { onDelete(image) },
            )
        }
    }
    if (images.isEmpty()) {
        Text(
            "点左侧方框插入截图或照片",
            color = TextSub,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(88.dp)
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFEBE4D8))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.material3.Icon(
                imageVector = FeatherIcons.Image,
                contentDescription = "添加图片",
                tint = TextSub,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text("添加", color = TextSub, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ThumbnailTile(
    path: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(88.dp)
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
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
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.Icon(
                imageVector = FeatherIcons.Trash2,
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}
