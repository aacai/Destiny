package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.app.destiny.oinkcompat.Text
import zhiqiu.app.destiny.oinkcompat.TextButton
import zhiqiu.app.destiny.ui.theme.DestinyTheme

/** 占卜案例的通用只读详情：标题 + 若干「标签→内容」行。梅花/奇门等暂无专用盘时复用。 */
@Composable
fun SimpleCaseDetail(
    title: String,
    rows: List<Pair<String, String>>,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DestinyTheme.page)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        TextButton(onClick = onBack) { Text("‹ 返回", color = DestinyTheme.accent, fontSize = 15.sp) }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, color = DestinyTheme.ink, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
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
