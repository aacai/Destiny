package zhiqiu.app.destiny.ui.bazi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import zhiqiu.app.destiny.bazi.original.toFlowChart
import zhiqiu.app.destiny.bazi.original.toOriginalChart
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.ui.theme.DestinyColorMode
import zhiqiu.app.destiny.ui.theme.DestinyTheme
import zhiqiu.app.destiny.ui.theme.LocalDestinyAppThemes
import zhiqiu.app.destiny.ui.theme.LocalDestinyColorMode
import zhiqiu.iztro.bazi.flow.FlowSelection
import zhiqiu.iztro.bazi.ui.BaziColorMode
import zhiqiu.iztro.bazi.ui.BaziElementType
import zhiqiu.iztro.bazi.ui.BaziFlowPage
import zhiqiu.iztro.bazi.ui.BaziOriginalPage
import zhiqiu.iztro.bazi.ui.BaziThemeProvider

/** 八字页：排盘走宿主适配层（Profile→chart），盘面来自 bazi-ui 模块 */
@Composable
fun BaziSection(profile: Profile) {
    var subTab by remember { mutableIntStateOf(0) }
    var info by remember { mutableStateOf<GlossaryItem?>(null) }
    val destinyMode = LocalDestinyColorMode.current
    val baziMode = if (destinyMode == DestinyColorMode.Dark) BaziColorMode.Dark else BaziColorMode.Light
    val accent = DestinyTheme.accent
    val baziThemeConfig = LocalDestinyAppThemes.current.bazi

    val onBaziElementClick: (BaziElementType, String) -> Unit = { type, name ->
        info = when (type) {
            BaziElementType.ShenSha -> SHEN_SHA_GLOSSARY[name]
                ?: GlossaryItem(name, "神煞", listOf("资料" to "该神煞资料整理中"))
            BaziElementType.TenGod -> {
                val full = tenGodFull(name)
                TEN_GOD_GLOSSARY[full]
                    ?: GlossaryItem(full, "十神", listOf("资料" to "该十神资料整理中"))
            }
        }
    }

    BaziThemeProvider(mode = baziMode, config = baziThemeConfig) {
        Column(modifier = Modifier.fillMaxSize()) {
            SecondaryTabRow(
                selectedTabIndex = subTab,
                modifier = Modifier.height(40.dp),
                containerColor = Color.Transparent,
                contentColor = accent,
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
                        BaziOriginalPage(chart = chart, onBaziElementClick = onBaziElementClick)
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
                        BaziFlowPage(
                            chart = chart,
                            onSelectionChange = { selection = it },
                        )
                    }
                }
            }
            info?.let { item -> BaziInfoDialog(item) { info = null } }
        }
    }
}
