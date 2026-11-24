package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.app.destiny.oinkcompat.Button
import zhiqiu.app.destiny.oinkcompat.HorizontalDivider
import zhiqiu.app.destiny.oinkcompat.Text
import zhiqiu.app.destiny.oinkcompat.TextButton
import zhiqiu.app.destiny.i18n.AppStrings
import zhiqiu.app.destiny.ui.theme.DestinyTheme

/** 占卜案例的通用只读详情：标题 + 若干「标签→内容」行。梅花/奇门等暂无专用盘时复用。 */
@Composable
fun SimpleCaseDetail(
    title: String,
    rows: List<Pair<String, String>>,
    onBack: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DestinyTheme.page)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text(AppStrings.backShort, color = DestinyTheme.accent, fontSize = 15.sp) }
            Text(
                title,
                color = DestinyTheme.ink,
                fontSize = 17.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            if (onEdit != null) {
                TextButton(onClick = onEdit) { Text(AppStrings.edit, color = DestinyTheme.accent, fontSize = 14.sp) }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = DestinyTheme.line)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DestinyTheme.card)
                    .padding(16.dp),
            ) {
                rows.forEach { (label, value) ->
                    Text(label, color = DestinyTheme.muted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(value, color = DestinyTheme.ink, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/** 案例详情共用的「备注信息」区：显示占问，备注可直接改并保存。 */
@Composable
fun CaseNoteSection(
    question: String,
    note: String,
    onSaveNote: (String) -> Unit,
) {
    var draft by remember(note) { mutableStateOf(note) }
    val c = DestinyTheme
    Spacer(modifier = Modifier.height(16.dp))
    Text(AppStrings.noteSection, color = c.muted, fontSize = 11.sp)
    Spacer(modifier = Modifier.height(6.dp))
    if (question.isNotBlank()) {
        Text("${AppStrings.question}：$question", color = c.ink, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
    }
    BasicTextField(
        value = draft,
        onValueChange = { draft = it },
        textStyle = TextStyle(fontSize = 14.sp, color = c.ink),
        cursorBrush = SolidColor(c.accent),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.card)
            .padding(12.dp),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = { onSaveNote(draft) }) {
        Text(AppStrings.saveNote, fontSize = 13.sp)
    }
    Spacer(modifier = Modifier.height(16.dp))
}
