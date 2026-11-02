package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.app.destiny.ui.theme.DestinyTheme

/**
 * 应用官方地址（关于页展示）。
 * TODO: 替换为实际发布地址（官网 / 代码仓库 / 联系方式）。
 */
const val OFFICIAL_ADDRESS: String = "https://github.com/zhiqiu"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(DestinyTheme.page).safeDrawingPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("‹ 返回", color = DestinyTheme.accent, fontSize = 15.sp) }
            Text("关于", color = DestinyTheme.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(thickness = 0.5.dp, color = DestinyTheme.line)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Destiny 命理", color = DestinyTheme.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "八字 · 七政 · 六爻 · 小六壬 · 梅花易数 · 奇门 · 紫微 ·  bookshelf 古籍阅读，一处以命理与卜筮为主题的离线工具。",
                color = DestinyTheme.muted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("官方地址", color = DestinyTheme.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                SelectionContainer {
                    Text(
                        OFFICIAL_ADDRESS,
                        color = DestinyTheme.accent,
                        fontSize = 14.sp,
                    )
                }
            }
            Text(
                "备份可通过内置的临时文件服务（x0.at）生成分享链接，对方凭链接导入；链接有效期由服务方决定。",
                color = DestinyTheme.muted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }
    }
}
