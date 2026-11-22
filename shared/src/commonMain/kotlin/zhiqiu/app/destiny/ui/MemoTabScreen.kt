package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.app.destiny.oinkcompat.Text
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.ui.note.MarkdownEditor
import zhiqiu.app.destiny.ui.note.NoteEditorState
import zhiqiu.app.destiny.ui.note.NoteSaveStatusLabel
import zhiqiu.app.destiny.ui.theme.DestinyTheme

/**
 * 批注 Tab：单一大型 Markdown 编辑器，取代原先分散的「总批 + 三分附图」。
 * 文本存于 Profile.note，由 [noteState] 自动保存；插图以相对路径写入 Markdown，
 * 预览时经 [resolvePath] 解析为绝对路径。
 */
@Composable
fun MemoTabScreen(
    profile: Profile,
    noteState: NoteEditorState,
    onPickMarkdownImage: () -> Unit,
) {
    val c = DestinyTheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.page),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(profile.name.ifBlank { "未命名" }, color = c.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (profile.birthday.isNotBlank()) {
                Text(profile.birthday, color = c.muted, fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "批注",
                color = c.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            NoteSaveStatusLabel(noteState.status)
        }

        MarkdownEditor(
            value = noteState.draft,
            onValueChange = { noteState.update(it, autoSave = true) },
            colors = c,
            minHeight = 420.dp,
            onPickImage = onPickMarkdownImage,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}
