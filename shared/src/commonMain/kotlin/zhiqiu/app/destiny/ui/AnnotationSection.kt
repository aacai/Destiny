package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.time.TIME_INDEX_LABELS

private val Page = Color(0xFFFFFFFF)
private val CardBg = Color(0xFFF7F7F2)
private val Ink = Color(0xFF222222)
private val Muted = Color(0xFF757575)
private val Line = Color(0xFFE0E0E0)
private val Accent = Color(0xFF1B5E20)

/**
 * 命例级批注视图：与八字/紫微/七政无关，包含「个人信息面板」与「详细批注」两部分。
 * 个人信息编辑请走档案编辑页；此处批注（详细批注文本）由 floating 编辑按钮修改。
 */
@Composable
fun AnnotationSection(profile: Profile) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── 个人信息面板 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(12.dp))
                .border(1.dp, Line, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            Text("个人信息", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            InfoRow("姓名", profile.name.ifBlank { "未命名" })
            InfoRow("性别", profile.gender.ifBlank { "—" })
            InfoRow("生日", profile.solarDateDisplay.ifBlank { profile.birthday })
            InfoRow(
                "时辰",
                TIME_INDEX_LABELS.getOrElse(profile.timeIndex) { "未知时辰" },
            )
            InfoRow(
                "历法",
                (if (profile.birthdayType == "lunar") "农历" else "公历") +
                    if (profile.isLeapMonth) "（闰月）" else "",
            )
            InfoRow("分组", profile.groupName.ifBlank { "默认" })
            if (profile.baziSummary.isNotBlank()) {
                InfoRow("四柱", profile.baziSummary)
            }
        }

        // ── 详细批注 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(12.dp))
                .border(1.dp, Line, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            Text("详细批注", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            if (profile.note.isBlank()) {
                Text(
                    "暂无批注，点击右下角编辑按钮添加…",
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            } else {
                Text(
                    profile.note,
                    color = Ink,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            color = Muted,
            fontSize = 13.sp,
            modifier = Modifier.width(64.dp),
        )
        Text(
            value,
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 批注编辑对话框：编辑命例的详细批注文本。
 * 个人信息面板以只读形式展示，便于对照编辑。
 */
@Composable
fun AnnotationEditorDialog(
    profile: Profile,
    onSave: (Profile) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(profile.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onSave(profile.copy(note = draft.trim())) },
            ) { Text("保存", color = Accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = Muted) }
        },
        title = { Text("编辑批注", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // 个人信息（只读，便于对照）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg, RoundedCornerShape(10.dp))
                        .border(1.dp, Line, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    InfoRow("姓名", profile.name.ifBlank { "未命名" })
                    InfoRow(
                        "生日",
                        "${profile.solarDateDisplay.ifBlank { profile.birthday }} · " +
                            TIME_INDEX_LABELS.getOrElse(profile.timeIndex) { "未知时辰" },
                    )
                    InfoRow("分组", profile.groupName.ifBlank { "默认" })
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    label = { Text("详细批注") },
                    placeholder = { Text("在此写下对该命例的观察、断语与心得…") },
                    textStyle = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
                    singleLine = false,
                )
            }
        },
    )
}
