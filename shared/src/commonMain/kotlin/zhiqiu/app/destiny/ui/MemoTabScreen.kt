package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import zhiqiu.app.destiny.oinkcompat.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.app.destiny.db.ProfileImage
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.ui.note.MEMO_IMAGE_GROUPS
import zhiqiu.app.destiny.ui.note.NoteAttachmentStrip
import zhiqiu.app.destiny.ui.note.NoteEditorState
import zhiqiu.app.destiny.ui.note.NoteSaveStatusLabel
import zhiqiu.app.destiny.ui.note.NoteSectionLabel
import zhiqiu.app.destiny.ui.note.NoteTextEditor
import zhiqiu.app.destiny.ui.note.ProfileNoteBanner
import zhiqiu.app.destiny.ui.theme.DestinyTheme

/**
 * 批注 Tab：命例摘要 + 总批 + 三分附图。与 FAB Sheet 共用 [noteState]。
 */
@Composable
fun MemoTabScreen(
    profile: Profile,
    noteState: NoteEditorState,
    imagesByCategory: Map<String, List<ProfileImage>>,
    resolvePath: (ProfileImage) -> String,
    onPickImage: (category: String) -> Unit,
    onDeleteImage: (ProfileImage) -> Unit,
) {
    val theme = DestinyTheme
    val hasNote = noteState.draft.isNotBlank()
    val hasAnyImage = imagesByCategory.values.any { it.isNotEmpty() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.page),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            ProfileNoteBanner(profile)
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NoteSectionLabel("总批注", modifier = Modifier.weight(1f).padding(vertical = 0.dp))
                NoteSaveStatusLabel(noteState.status)
            }
            NoteTextEditor(
                value = noteState.draft,
                onValueChange = { noteState.update(it, autoSave = true) },
                dark = false,
                minHeight = 180.dp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            if (!hasNote && !hasAnyImage) {
                Text(
                    "写下命局总论，或在下方添加各盘附图。盘面内也可点批注按钮速记。",
                    color = theme.muted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }

        items(MEMO_IMAGE_GROUPS, key = { it.first }) { (key, title) ->
            val imgs = imagesByCategory[key].orEmpty()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 22.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                NoteSectionLabel("${title}附图")
                NoteAttachmentStrip(
                    images = imgs,
                    resolvePath = resolvePath,
                    onAdd = { onPickImage(key) },
                    onDelete = onDeleteImage,
                    dark = false,
                    emptyHint = "添加${title}相关截图或照片",
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}
