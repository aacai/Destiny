package zhiqiu.app.destiny.ui.bazi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import zhiqiu.app.destiny.oinkcompat.SecondaryTabRow
import zhiqiu.app.destiny.oinkcompat.Tab
import zhiqiu.app.destiny.oinkcompat.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zhiqiu.app.destiny.bazi.original.toFlowChart
import zhiqiu.app.destiny.bazi.original.toOriginalChart
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.ui.theme.DestinyColorMode
import zhiqiu.app.destiny.ui.theme.DestinyTheme
import zhiqiu.app.destiny.ui.theme.LocalDestinyAppThemes
import zhiqiu.app.destiny.ui.theme.LocalDestinyColorMode
import zhiqiu.iztro.bazi.flow.FlowChart
import zhiqiu.iztro.bazi.flow.FlowSelection
import zhiqiu.iztro.bazi.original.OriginalChart
import zhiqiu.iztro.bazi.ui.BaziColorMode
import zhiqiu.iztro.bazi.ui.BaziElementType
import zhiqiu.iztro.bazi.ui.BaziFlowPage
import zhiqiu.iztro.bazi.ui.BaziOriginalPage
import zhiqiu.iztro.bazi.ui.BaziThemeProvider

/** 八字页：排盘走宿主适配层（Profile→chart），盘面来自 bazi-ui 模块 */
@Composable
fun BaziSection(
    profile: Profile,
    modifier: Modifier = Modifier,
) {
    var subTab by remember { mutableIntStateOf(0) }
    var info by remember { mutableStateOf<GlossaryItem?>(null) }
    val destinyMode = LocalDestinyColorMode.current
    val baziMode = if (destinyMode == DestinyColorMode.Dark) BaziColorMode.Dark else BaziColorMode.Light
    val accent = DestinyTheme.accent
    val muted = DestinyTheme.muted
    val baziThemeConfig = LocalDestinyAppThemes.current.bazi

    val profileKey = listOf(
        profile.id, profile.birthday, profile.timeIndex, profile.gender,
        profile.birthdayType, profile.isLeapMonth,
    )

    // 原局后台排盘，避免切 Tab 时主线程卡顿
    var originalChart by remember(profile.id) { mutableStateOf<OriginalChart?>(null) }
    var originalFailed by remember(profile.id) { mutableStateOf(false) }
    LaunchedEffect(profileKey) {
        originalFailed = false
        originalChart = null
        val chart = withContext(Dispatchers.Default) {
            runCatching { profile.toOriginalChart() }.getOrNull()
        }
        originalChart = chart
        originalFailed = chart == null
    }

    var selection by remember(profile.id) { mutableStateOf<FlowSelection?>(null) }
    var flowVisited by remember(profile.id) { mutableStateOf(false) }
    var flowChart by remember(profile.id) { mutableStateOf<FlowChart?>(null) }
    var flowFailed by remember(profile.id) { mutableStateOf(false) }
    LaunchedEffect(subTab) {
        if (subTab == 1) flowVisited = true
    }
    LaunchedEffect(profileKey, selection, flowVisited) {
        if (!flowVisited) return@LaunchedEffect
        flowFailed = false
        val chart = withContext(Dispatchers.Default) {
            runCatching { profile.toFlowChart(selection) }.getOrNull()
        }
        flowChart = chart
        flowFailed = chart == null
    }

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
        Column(modifier = modifier.fillMaxSize()) {
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
                0 -> when {
                    originalChart != null -> BaziOriginalPage(
                        chart = originalChart!!,
                        onBaziElementClick = onBaziElementClick,
                    )
                    originalFailed -> Text("八字排盘失败，请检查出生信息", modifier = Modifier.fillMaxSize())
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("排盘中…", color = muted)
                    }
                }
                else -> when {
                    flowChart != null -> BaziFlowPage(
                        chart = flowChart!!,
                        onSelectionChange = { selection = it },
                    )
                    flowFailed -> Text("流盘排盘失败，请检查出生信息", modifier = Modifier.fillMaxSize())
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("排盘中…", color = muted)
                    }
                }
            }
            info?.let { item -> BaziInfoDialog(item) { info = null } }
        }
    }
}
