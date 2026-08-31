package zhiqiu.app.destiny.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import compose.icons.FeatherIcons
import compose.icons.feathericons.BookOpen
import compose.icons.feathericons.Compass
import compose.icons.feathericons.Star
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.ui.bazi.BaziSection
import zhiqiu.app.destiny.ui.qizheng.QizhengSection
import zhiqiu.iztro.ui.Iztrolabe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartPagerScreen(
    profile: Profile,
    onBack: () -> Unit,
    onSaveQizhengNote: (String) -> Unit = {},
    /** 七政盘制名（对应 PanZhiPresets.all 的名称） */
    onSaveQizhengPanZhi: (String) -> Unit = {},
) {
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile.name.ifBlank { "原盘" }) },
                navigationIcon = {
                    IosBackButton(onClick = onBack)
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = {
                        Icon(
                            imageVector = FeatherIcons.BookOpen,
                            contentDescription = "八字",
                        )
                    },
                    label = { Text("八字") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = {
                        Icon(
                            imageVector = FeatherIcons.Star,
                            contentDescription = "紫微",
                        )
                    },
                    label = { Text("紫微") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = {
                        Icon(
                            imageVector = FeatherIcons.Compass,
                            contentDescription = "七政",
                        )
                    },
                    label = { Text("七政") },
                )
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (tab) {
                0 -> BaziSection(profile)
                1 -> Iztrolabe(
                    birthday = profile.birthday,
                    birthTime = profile.timeIndex,
                    gender = profile.gender,
                    birthdayType = profile.birthdayType,
                    isLeapMonth = profile.isLeapMonth,
                    fixLeap = profile.fixLeap,
                    name = profile.name,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> QizhengSection(
                    profile = profile,
                    onSaveNote = onSaveQizhengNote,
                    onSavePanZhi = onSaveQizhengPanZhi,
                )
            }
        }
    }
}
