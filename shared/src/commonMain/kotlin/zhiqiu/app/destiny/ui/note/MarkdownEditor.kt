package zhiqiu.app.destiny.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.app.destiny.oinkcompat.Text
import zhiqiu.app.destiny.ui.theme.DestinyColors

/** 工具栏项：label 展示、title 提示、insert 插入的标记、wrap 是否包裹选区 */
private data class ToolAction(val label: String, val insert: String, val wrap: Boolean = true)

/**
 * 单一 Markdown 编辑框：工具栏在光标处插入标记，文本由外部持有并自动保存。
 * 保持纯文本，不引入富文本/预览渲染的复杂实现。
 */
@Composable
fun MarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    colors: DestinyColors,
    modifier: Modifier = Modifier,
    minHeight: Dp = 360.dp,
    onPickImage: (() -> Unit)? = null,
) {
    var field by remember { mutableStateOf(TextFieldValue(value)) }
    if (field.text != value) field = field.copy(text = value)

    val actions = listOf(
        ToolAction("H1", "# ", wrap = false),
        ToolAction("H2", "## ", wrap = false),
        ToolAction("B", "**"),
        ToolAction("I", "*"),
        ToolAction("•", "- ", wrap = false),
        ToolAction("❝", "> ", wrap = false),
        ToolAction("</>", "`"),
        ToolAction("🔗", "[](url)"),
        ToolAction("🖼", "![](path)"),
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        ) {
            actions.forEach { a ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.panel)
                        .clickable {
                            if (a.label == "🖼" && onPickImage != null) {
                                onPickImage()
                            } else {
                                field = applyAction(a, field).also { onValueChange(it.text) }
                            }
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Text(a.label, color = colors.ink, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = field,
            onValueChange = { next -> field = next; onValueChange(next.text) },
            modifier = Modifier
                .fillMaxWidth()
                .height(minHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.card)
                .padding(14.dp),
            textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = colors.ink),
            decorationBox = { inner ->
                if (field.text.isEmpty()) {
                    Text("用 Markdown 写下命局批注…\n\n# 标题   **粗体**   - 列表   > 引用", color = colors.muted, fontSize = 14.sp, lineHeight = 24.sp)
                }
                inner()
            },
        )
    }
}

private fun applyAction(a: ToolAction, current: TextFieldValue): TextFieldValue {
    val start = current.selection.start
    val end = current.selection.end
    val selected = current.text.substring(start, end)
    val (insert, cursor) = when {
        a.wrap -> {
            val body = if (selected.isNotEmpty()) selected else "文本"
            "${a.insert}$body${a.insert}" to (start + a.insert.length + body.length)
        }
        a.insert == "[](url)" -> "[$selected](url)" to (start + selected.length + 3)
        a.insert == "![](path)" -> "![$selected](path)" to (start + selected.length + 4)
        else -> a.insert to (start + a.insert.length)
    }
    val next = current.text.substring(0, start) + insert + current.text.substring(end)
    return current.copy(text = next, selection = TextRange(cursor))
}
