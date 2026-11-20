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
import zhiqiu.app.destiny.oinkcompat.Switch
import zhiqiu.app.destiny.oinkcompat.SwitchDefaults
import zhiqiu.app.destiny.oinkcompat.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 七政四余设置页（独立整页，不含宿主概念）。
 *
 * 坐标系 / 宿制 / 固定命宫 / 化曜流派 / 盘面染色，全部即时生效：
 * 每一项变更通过对应回调上抛，由宿主重排序盘。
 */
@Composable
fun QizhengSettingsPage(
    darkTheme: Boolean = false,
    themeConfig: QizhengThemeConfig = QizhengThemeConfig.Default,
    /** 0 黄道回归 1 黄道恒星 2 赤道恒星 */
    coordIdx: Int,
    /** 0果老 1回归今宿 2回归古宿 3古宿岁差 4星学大成 5玄枵虚中 6自定义 7郑案今宿 */
    xiuIdx: Int,
    /** -1 自动（时加太阳数至卯），0..11 = 子..亥 */
    fixedMing: Int,
    /** 0 果老化曜 1 天官化曜 */
    huaYaoIdx: Int,
    /** 染二十八宿 */
    xiuTint: Boolean,
    /** 真太阳时（经度+均时差校正） */
    trueSolar: Boolean = false,
    /** 立命：0 果老时辰 1 日出 2 日落（固定命宫优先于本项） */
    mingModeIdx: Int = 0,
    /** 紫气沿赤道匀速（对齐星命排盘「紫气赤道匀速」） */
    ziQiEquatorial: Boolean = false,
    /** 罗计：0 传统罗降交 1 印度罗升交 */
    luoJiIdx: Int = 0,
    /** 安身：0 果老太阴 1 琴堂逆酉 2 日出 3 日落 */
    shenGongIdx: Int = 0,
    /** 天盘时分转动 */
    tianPan: Boolean = false,
    onCoordChange: (Int) -> Unit,
    onXiuChange: (Int) -> Unit,
    onFixedMingChange: (Int) -> Unit,
    onHuaYaoChange: (Int) -> Unit,
    onXiuTintChange: (Boolean) -> Unit,
    onTrueSolarChange: (Boolean) -> Unit = {},
    onMingModeChange: (Int) -> Unit = {},
    onZiQiEquatorialChange: (Boolean) -> Unit = {},
    onLuoJiChange: (Int) -> Unit = {},
    onShenGongChange: (Int) -> Unit = {},
    onTianPanChange: (Boolean) -> Unit = {},
    onBack: () -> Unit,
) {
    val c = themeConfig.colors(darkTheme)
    val Page = c.page
    val Ink = c.ink
    val Muted = c.muted
    val Line = c.hairline
    val ChipBg = c.chipBg
    val ChipOn = c.chipOn
    val Accent = c.accent
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Page),
    ) {
        // 标题栏固定
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                QizhengStrings.back,
                color = Accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(QizhengStrings.settings, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(44.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {

        GroupLabel("盘制（坐标系）", c)
        ChipRow(
            labels = listOf("黄道回归", "黄道恒星", "赤道恒星"),
            selected = coordIdx,
            c = c,
            onSelect = onCoordChange,
        )

        GroupLabel("宿制", c)
        ChipRow(
            labels = listOf(
                "果老星宗", "回归今宿", "回归古宿", "古宿岁差",
                "星学大成", "玄枵虚中", "自定义宿度", "郑案今宿",
            ),
            selected = xiuIdx,
            c = c,
            onSelect = onXiuChange,
        )

        GroupLabel("固定命宫", c)
        ChipRow(
            labels = listOf("自动") + "子丑寅卯辰巳午未申酉戌亥".map { it.toString() },
            selected = fixedMing + 1,
            c = c,
            onSelect = { onFixedMingChange(it - 1) },
        )

        GroupLabel("立命时辰", c)
        ChipRow(
            labels = listOf("生时（果老）", "日出", "日落"),
            selected = mingModeIdx.coerceIn(0, 2),
            c = c,
            onSelect = onMingModeChange,
        )

        GroupLabel("安身", c)
        ChipRow(
            labels = listOf("果老·太阴宫", "琴堂·逆至酉", "日出安身", "日落安身"),
            selected = shenGongIdx.coerceIn(0, 3),
            c = c,
            onSelect = onShenGongChange,
        )

        GroupLabel("罗计印式", c)
        ChipRow(
            labels = listOf("传统·罗降交", "印度·罗升交"),
            selected = luoJiIdx.coerceIn(0, 1),
            c = c,
            onSelect = onLuoJiChange,
        )

        GroupLabel("化曜流派", c)
        ChipRow(
            labels = listOf("果老化曜", "天官化曜"),
            selected = huaYaoIdx,
            c = c,
            onSelect = onHuaYaoChange,
        )

        GroupLabel("时间", c)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("真太阳时", color = Ink, fontSize = 14.sp)
                Text("按出生地经度 + 均时差校正（对齐星命排盘默认）", color = Muted, fontSize = 11.sp)
            }
            Switch(
                checked = trueSolar,
                onCheckedChange = onTrueSolarChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Accent),
            )
        }

        GroupLabel("四余", c)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("紫气赤道匀速", color = Ink, fontSize = 14.sp)
                Text("沿赤经匀进再折回黄经（对齐星命排盘选项）", color = Muted, fontSize = 11.sp)
            }
            Switch(
                checked = ziQiEquatorial,
                onCheckedChange = onZiQiEquatorialChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Accent),
            )
        }

        GroupLabel("盘面", c)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("天盘时分", color = Ink, fontSize = 14.sp)
                Text("按时分重算命度并重映射命宫/十二宫", color = Muted, fontSize = 11.sp)
            }
            Switch(
                checked = tianPan,
                onCheckedChange = onTianPanChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Accent),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        } // scroll body
    }
}

@Composable
private fun GroupLabel(text: String, c: QizhengColors) {
    Text(
        text,
        color = c.muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ChipRow(
    labels: List<String>,
    selected: Int,
    c: QizhengColors,
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
                    .background(if (on) c.chipOn else c.chipBg, RoundedCornerShape(6.dp))
                    .border(1.dp, if (on) c.accent else c.hairline, RoundedCornerShape(6.dp))
                    .clickable { onSelect(i) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    label,
                    color = if (on) c.accent else c.ink,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
