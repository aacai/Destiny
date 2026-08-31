package zhiqiu.app.destiny.ui.bazi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import zhiqiu.app.destiny.bazi.original.toFlowChart
import zhiqiu.app.destiny.bazi.original.toOriginalChart
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.iztro.bazi.flow.FlowSelection
import zhiqiu.iztro.bazi.ui.BaziFlowPage
import zhiqiu.iztro.bazi.ui.BaziElementType
import zhiqiu.iztro.bazi.ui.BaziOriginalPage

private val Accent = Color(0xFF26C6C6)

/** 八字页：排盘走宿主适配层（Profile→chart），盘面来自 bazi-ui 模块 */
@Composable
fun BaziSection(profile: Profile) {
    var subTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(
            selectedTabIndex = subTab,
            containerColor = Color.Transparent,
            contentColor = Accent,
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = {
                    Text(
                        "原局",
                        fontWeight = if (subTab == 0) FontWeight.Bold else FontWeight.Normal,
                    )
                },
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = {
                    Text(
                        "流盘",
                        fontWeight = if (subTab == 1) FontWeight.Bold else FontWeight.Normal,
                    )
                },
            )
        }
        when (subTab) {
            0 -> {
                val chart = remember(profile) {
                    runCatching { profile.toOriginalChart() }.getOrNull()
                }
                if (chart == null) {
                    Text("八字排盘失败，请检查出生信息", modifier = Modifier.fillMaxSize())
                } else {
                    var info by remember { mutableStateOf<GlossaryItem?>(null) }
                    BaziOriginalPage(
                        chart = chart,
                        onBaziElementClick = { type, name ->
                            info = when (type) {
                                BaziElementType.ShenSha -> SHEN_SHA_GLOSSARY[name]
                                    ?: GlossaryItem(name, "神煞", listOf("资料" to "该神煞资料整理中"))
                                BaziElementType.TenGod -> {
                                    val full = tenGodFull(name)
                                    TEN_GOD_GLOSSARY[full]
                                        ?: GlossaryItem(full, "十神", listOf("资料" to "该十神资料整理中"))
                                }
                            }
                        },
                    )
                    info?.let { item -> BaziInfoDialog(item) { info = null } }
                }
            }
            else -> {
                var selection by remember(profile.id) { mutableStateOf<FlowSelection?>(null) }
                val chart = remember(profile, selection) {
                    runCatching { profile.toFlowChart(selection) }.getOrNull()
                }
                if (chart == null) {
                    Text("流盘排盘失败，请检查出生信息", modifier = Modifier.fillMaxSize())
                } else {
                    BaziFlowPage(chart = chart, onSelectionChange = { selection = it })
                }
            }
        }
    }
}
