package zhiqiu.qizheng.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Page = Color(0xFFFFFFFF)
private val Ink = Color(0xFF222222)
private val Muted = Color(0xFF757575)
private val Line = Color(0xFFE0E0E0)
private val ChipBg = Color(0xFFF5F5F5)
private val ChipOn = Color(0xFFE8F5E9)
private val Accent = Color(0xFF1B5E20)

/**
 * 七政四余设置页（独立整页，不含宿主概念）。
 *
 * 坐标系 / 宿制 / 固定命宫 / 化曜流派 / 盘面染色，全部即时生效：
 * 每一项变更通过对应回调上抛，由宿主重排序盘。
 */
@Composable
fun QizhengSettingsPage(
    /** 0 黄道回归 1 黄道恒星 2 赤道恒星 */
    coordIdx: Int,
    /** 0 果老星宗 1 回归今宿 2 回归古宿 3 古宿岁差 4 郑案今宿 */
    xiuIdx: Int,
    /** -1 自动（时加太阳数至卯），0..11 = 子..亥 */
    fixedMing: Int,
    /** 0 果老化曜 1 天官化曜 */
    huaYaoIdx: Int,
    /** 染二十八宿 */
    xiuTint: Boolean,
    onCoordChange: (Int) -> Unit,
    onXiuChange: (Int) -> Unit,
    onFixedMingChange: (Int) -> Unit,
    onHuaYaoChange: (Int) -> Unit,
    onXiuTintChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Page)
            .verticalScroll(rememberScrollState()),
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "← 返回",
                color = Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text("七政四余设置", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(44.dp))
        }

        GroupLabel("盘制（坐标系）")
        ChipRow(
            labels = listOf("黄道回归", "黄道恒星", "赤道恒星"),
            selected = coordIdx,
            onSelect = onCoordChange,
        )

        GroupLabel("宿制")
        ChipRow(
            labels = listOf("果老星宗", "回归今宿", "回归古宿", "古宿岁差", "郑案今宿"),
            selected = xiuIdx,
            onSelect = onXiuChange,
        )

        GroupLabel("固定命宫")
        ChipRow(
            labels = listOf("自动") + "子丑寅卯辰巳午未申酉戌亥".map { it.toString() },
            selected = fixedMing + 1,
            onSelect = { onFixedMingChange(it - 1) },
        )

        GroupLabel("化曜流派")
        ChipRow(
            labels = listOf("果老化曜", "天官化曜"),
            selected = huaYaoIdx,
            onSelect = onHuaYaoChange,
        )

        GroupLabel("盘面")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("染二十八宿", color = Ink, fontSize = 14.sp)
                Text("按五行色相给二十八宿环上色", color = Muted, fontSize = 11.sp)
            }
            Switch(
                checked = xiuTint,
                onCheckedChange = onXiuTintChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Accent),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        color = Muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ChipRow(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val on = selected == i
            Box(
                modifier = Modifier
                    .background(if (on) ChipOn else ChipBg, RoundedCornerShape(6.dp))
                    .border(1.dp, if (on) Accent else Line, RoundedCornerShape(6.dp))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    label,
                    color = if (on) Accent else Ink,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
