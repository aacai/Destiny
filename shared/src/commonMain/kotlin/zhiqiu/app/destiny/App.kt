package zhiqiu.app.destiny

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import zhiqiu.app.destiny.oinkcompat.AlertDialog
import zhiqiu.app.destiny.oinkcompat.MaterialThemeHost
import zhiqiu.app.destiny.oinkcompat.Text
import zhiqiu.app.destiny.oinkcompat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import kotlinx.coroutines.launch
import kotlin.time.Clock
import zhiqiu.app.destiny.profile.CaseType
import zhiqiu.app.destiny.profile.LiuYaoPayload
import zhiqiu.app.destiny.profile.MeiHuaPayload
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.profile.ProfileRepository
import zhiqiu.app.destiny.profile.QimenPayload
import zhiqiu.app.destiny.profile.XiaoLiuRenPayload
import zhiqiu.app.destiny.platform.AppBackHandler
import zhiqiu.app.destiny.platform.NativeTitleBarSync
import zhiqiu.app.destiny.platform.StatusBarIconSync
import zhiqiu.app.destiny.platform.TitleBarBandHeight
import zhiqiu.app.destiny.sharing.BackupSharing
import zhiqiu.app.destiny.sharing.FileIoClient
import zhiqiu.app.destiny.sharing.createSharedHttpClient
import zhiqiu.app.destiny.ui.AboutScreen
import zhiqiu.app.destiny.ui.AddProfileScreen
import zhiqiu.app.destiny.ui.ChartPagerScreen
import zhiqiu.app.destiny.ui.SimpleCaseDetail
import zhiqiu.app.destiny.ui.ProfileListScreen
import zhiqiu.app.destiny.ui.books.BookshelfScreen
import zhiqiu.app.destiny.ui.books.ReaderScreen
import zhiqiu.app.destiny.ui.theme.DestinyAppThemes
import zhiqiu.app.destiny.ui.theme.DestinyColorMode
import zhiqiu.app.destiny.ui.theme.DestinyTheme
import zhiqiu.app.destiny.ui.theme.DestinyThemeProvider
import zhiqiu.app.destiny.ui.theme.LocalDestinyAppThemes
import zhiqiu.liuyao.ui.LiuYaoCaseViewer
import zhiqiu.liuyao.ui.LiuYaoSave
import zhiqiu.liuyao.ui.LiuYaoScreen
import zhiqiu.liuyao.ui.ZhouyiBrowserScreen
import zhiqiu.meihua.ui.MeiHuaScreen
import zhiqiu.qimen.ui.QimenScreen
import zhiqiu.xiaoliuren.ui.XiaoLiuRenCaseViewer
import zhiqiu.xiaoliuren.ui.XiaoLiuRenSave
import zhiqiu.xiaoliuren.ui.XiaoLiuRenScreen

private object Routes {
    const val List = "list"
    const val Add = "add"
    const val Edit = "edit/{profileId}"
    const val Chart = "chart/{profileId}"
    const val Books = "books"
    const val Reader = "reader/{bookId}"
    const val Liuyao = "liuyao"
    const val Yijing = "yijing"
    const val About = "about"
    const val XiaoLiuRen = "xiaoliuren"
    const val MeiHua = "meihua"
    const val Qimen = "qimen"

    fun chart(profileId: String) = "chart/$profileId"
    fun edit(profileId: String) = "edit/$profileId"
    fun reader(bookId: String) = "reader/$bookId"
}

@Composable
fun App() {
    var repository by remember { mutableStateOf<ProfileRepository?>(null) }
    var schemaError by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        schemaError = false
        try {
            val repo = createProfileRepository()
            // 触发数据库打开与 schema 校验，尽早暴露“结构不兼容”错误（否则会在首次查询时崩溃）
            repo.getAll()
            repository = repo
        } catch (e: IllegalStateException) {
            if (e.message?.contains("cannot verify the data integrity", ignoreCase = true) == true) {
                schemaError = true
            } else {
                throw e
            }
        }
    }

    if (repository != null) {
        App(repository = repository!!)
    }

    if (schemaError) {
        AlertDialog(
            onDismissRequest = { /* 必须显式选择，不允许点外部关闭 */ },
            title = { Text("数据库结构已变更") },
            text = {
                Text(
                    "检测到旧数据库与当前应用版本不兼容，无法直接打开。" +
                        "是否删除旧数据（含本地批注图片）并重新创建？此操作不可恢复；" +
                        "如有备份，可在删除后通过“导入备份”恢复。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteAppData()
                        schemaError = false
                        reloadKey++
                    },
                ) { Text("删除旧数据并继续") }
            },
            dismissButton = {
                TextButton(onClick = { exitApp() }) { Text("退出应用") }
            },
        )
    }
}

@Composable
fun App(
    repository: ProfileRepository,
    themes: DestinyAppThemes = DestinyAppThemes.Default,
) {
    val scope = rememberCoroutineScope()
    val profiles by repository.observeAll().collectAsState(initial = emptyList())
    val readerStore = remember(repository) { repository.readerStore }
    val sharing = remember { BackupSharing(FileIoClient(createSharedHttpClient())) }
    val navController = rememberNavController()
    // 主页(Routes.List)是导航栈根，永不 pop；防止连点返回把主页 pop 掉导致白屏。
    fun goBack() {
        if (navController.previousBackStackEntry != null) navController.popBackStack()
    }

    var colorMode by remember { mutableStateOf(DestinyColorMode.Light) }
    var colorModeReady by remember { mutableStateOf(false) }
    LaunchedEffect(readerStore) {
        colorMode = DestinyColorMode.fromPref(readerStore.get(DestinyColorMode.PrefKey))
        colorModeReady = true
    }

    fun setColorMode(mode: DestinyColorMode) {
        colorMode = mode
        readerStore.put(DestinyColorMode.PrefKey, mode.prefValue)
    }

    if (!colorModeReady) return

    CompositionLocalProvider(
        LocalDestinyAppThemes provides themes,
    ) {
        DestinyThemeProvider(mode = colorMode, config = themes.app) {
            NativeTitleBarSync(colorMode)
            StatusBarIconSync(colorMode)
            val colors = DestinyTheme
            MaterialThemeHost(darkTheme = colorMode == DestinyColorMode.Dark) {
                // 系统/手势返回键：主页永远消费掉，不允许把根页面 pop 出栈。
                AppBackHandler(enabled = true) {
                    if (navController.previousBackStackEntry != null) navController.popBackStack()
                }
                NavHost(
                    // 红绿灯色带由宿主统一留出，各页面顶栏不必各自避让
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.page)
                        .padding(top = TitleBarBandHeight)
                        .safeDrawingPadding(),
                    navController = navController,
                    startDestination = Routes.List,
                ) {
                composable(Routes.List) {
                    ProfileListScreen(
                        profiles = profiles,
                        onAdd = { navController.navigate(Routes.Add) },
                        onOpen = { profile ->
                            navController.navigate(Routes.chart(profile.id))
                        },
                        onDelete = { profile ->
                            scope.launch { repository.delete(profile.id) }
                        },
                        onExportBackup = { password -> repository.exportBackupBytes(password) },
                        onImportBackup = { bytes, password -> repository.importBackupBytes(bytes, password) },
                        sharing = sharing,
                        onOpenBooks = { navController.navigate(Routes.Books) },
                        onOpenLiuyao = { navController.navigate(Routes.Liuyao) },
                        onOpenXiaoLiuRen = { navController.navigate(Routes.XiaoLiuRen) },
                        onOpenMeiHua = { navController.navigate(Routes.MeiHua) },
                        onOpenQimen = { navController.navigate(Routes.Qimen) },
                        colorMode = colorMode,
                        onToggleColorMode = { setColorMode(colorMode.toggle()) },
                        onOpenAbout = { navController.navigate(Routes.About) },
                    )
                }
                composable(Routes.About) {
                    AboutScreen(onBack = { goBack() })
                }
                composable(Routes.XiaoLiuRen) {
                    XiaoLiuRenScreen(
                        onBack = { goBack() },
                        darkTheme = colorMode == DestinyColorMode.Dark,
                        themeConfig = themes.xlr,
                        onSave = { save ->
                            scope.launch {
                                val now = Clock.System.now().toEpochMilliseconds()
                                repository.upsert(
                                    Profile(
                                        id = save.id,
                                        name = save.name,
                                        gender = "",
                                        birthdayType = "",
                                        birthday = "",
                                        timeIndex = 0,
                                        note = save.note,
                                        caseType = CaseType.XIAOLIUREN,
                                        payload = XiaoLiuRenPayload(
                                            mode = save.mode, question = save.question,
                                            solarYear = save.solarYear, solarMonth = save.solarMonth,
                                            solarDay = save.solarDay, solarHour = save.solarHour,
                                            solarMinute = save.solarMinute,
                                            lunarMonth = save.lunarMonth, lunarDay = save.lunarDay,
                                            hourBranchIndex = save.hourBranchIndex,
                                            isLeapMonth = save.isLeapMonth,
                                            countMonth = save.countMonth, countDay = save.countDay,
                                            countHour = save.countHour,
                                        ).encode(),
                                        groupName = save.groupName,
                                        createdAt = now,
                                        updatedAt = now,
                                    ),
                                )
                            }
                        },
                    )
                }
                composable(Routes.MeiHua) {
                    MeiHuaScreen(
                        onBack = { goBack() },
                        darkTheme = colorMode == DestinyColorMode.Dark,
                        themeConfig = themes.mh,
                        onSave = { save ->
                            scope.launch {
                                val now = Clock.System.now().toEpochMilliseconds()
                                repository.upsert(
                                    Profile(
                                        id = save.id,
                                        name = save.name,
                                        gender = "", birthdayType = "", birthday = "", timeIndex = 0,
                                        note = save.note,
                                        caseType = CaseType.MEIHUA,
                                        payload = MeiHuaPayload(
                                            tab = save.tab, question = save.question,
                                            lunarMonth = save.lunarMonth, lunarDay = save.lunarDay,
                                            hourBranch = save.hourBranch, yearBranch = save.yearBranch,
                                            leap = save.leap, upperNum = save.upperNum,
                                            lowerNum = save.lowerNum, summary = save.summary,
                                        ).encode(),
                                        groupName = save.groupName,
                                        createdAt = now, updatedAt = now,
                                    ),
                                )
                            }
                        },
                    )
                }
                composable(Routes.Qimen) {
                    QimenScreen(
                        onBack = { goBack() },
                        darkTheme = colorMode == DestinyColorMode.Dark,
                        onSave = { save ->
                            scope.launch {
                                val now = Clock.System.now().toEpochMilliseconds()
                                repository.upsert(
                                    Profile(
                                        id = save.id,
                                        name = save.name,
                                        gender = "", birthdayType = "", birthday = "", timeIndex = 0,
                                        note = save.note,
                                        caseType = CaseType.QIMEN,
                                        payload = QimenPayload(
                                            mode = save.mode, question = save.question,
                                            year = save.year, month = save.month, day = save.day,
                                            hour = save.hour, minute = save.minute,
                                            plate = save.plate, boundary = save.boundary,
                                            solarText = save.solarText, pillarText = save.pillarText,
                                            yuanText = save.yuanText, dunText = save.dunText,
                                        ).encode(),
                                        groupName = save.groupName,
                                        createdAt = now, updatedAt = now,
                                    ),
                                )
                            }
                        },
                    )
                }
                composable(Routes.Liuyao) {
                    LiuYaoScreen(
                        onBack = { goBack() },
                        onOpenYijing = { navController.navigate(Routes.Yijing) },
                        darkTheme = colorMode == DestinyColorMode.Dark,
                        themeConfig = themes.pan,
                        onSave = { save ->
                            scope.launch {
                                val now = Clock.System.now().toEpochMilliseconds()
                                repository.upsert(
                                    Profile(
                                        id = save.id,
                                        name = save.name,
                                        gender = "",
                                        birthdayType = "",
                                        birthday = "",
                                        timeIndex = 0,
                                        note = save.note,
                                        caseType = CaseType.LIUYAO,
                                        payload = LiuYaoPayload(
                                            yaoKinds = save.yaoKinds,
                                            year = save.year, month = save.month, day = save.day,
                                            hour = save.hour, minute = save.minute,
                                            topic = save.topic, sub = save.sub,
                                            guaZhu = save.guaZhu, gender = save.gender,
                                            question = save.question,
                                        ).encode(),
                                        groupName = save.groupName,
                                        createdAt = now,
                                        updatedAt = now,
                                    ),
                                )
                            }
                        },
                    )
                }
                composable(Routes.Yijing) {
                    ZhouyiBrowserScreen(
                        onBack = { goBack() },
                        darkTheme = colorMode == DestinyColorMode.Dark,
                        themeConfig = themes.pan,
                    )
                }
                composable(Routes.Books) {
                    BookshelfScreen(
                        onBack = { goBack() },
                        onOpen = { bookId -> navController.navigate(Routes.reader(bookId)) },
                    )
                }
                composable(
                    route = Routes.Reader,
                    arguments = listOf(
                        navArgument("bookId") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val bookId = entry.arguments?.read { getStringOrNull("bookId") }.orEmpty()
                    ReaderScreen(
                        bookId = bookId,
                        readerStore = readerStore,
                        onBack = { goBack() },
                    )
                }
                composable(Routes.Add) {
                    AddProfileScreen(
                        onSave = { profile ->
                            scope.launch {
                                repository.upsert(profile)
                                goBack()
                            }
                        },
                        onCancel = { goBack() },
                    )
                }
                composable(
                    route = Routes.Edit,
                    arguments = listOf(
                        navArgument("profileId") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val profileId = entry.arguments?.read { getStringOrNull("profileId") }
                    val profile = remember(profileId, profiles) {
                        profiles.find { it.id == profileId }
                    }
                    if (profile == null) {
                        LaunchedEffect(profileId) { goBack() }
                    } else {
                        AddProfileScreen(
                            initial = profile,
                            onSave = { updated ->
                                scope.launch {
                                    repository.upsert(updated)
                                    goBack()
                                }
                            },
                            onCancel = { goBack() },
                        )
                    }
                }
                composable(
                    route = Routes.Chart,
                    arguments = listOf(
                        navArgument("profileId") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val profileId = entry.arguments?.read { getStringOrNull("profileId") }
                    val profile = remember(profileId, profiles) {
                        profiles.find { it.id == profileId }
                    }
                    if (profile == null) {
                        LaunchedEffect(profileId) {
                            goBack()
                        }
                    } else {
                        when (profile.caseType) {
                            CaseType.LIUYAO -> {
                                val p = remember(profile.payload) { LiuYaoPayload.decode(profile.payload) }
                                LiuYaoCaseViewer(
                                    yaoKinds = p?.yaoKinds.orEmpty(),
                                    year = p?.year ?: 0, month = p?.month ?: 0, day = p?.day ?: 0,
                                    hour = p?.hour ?: 0, minute = p?.minute ?: 0,
                                    topic = p?.topic ?: "NONE", sub = p?.sub.orEmpty(),
                                    guaZhu = p?.guaZhu.orEmpty(), gender = p?.gender ?: "男",
                                    question = p?.question ?: profile.name,
                                    note = profile.note,
                                    onBack = { goBack() },
                                    darkTheme = colorMode == DestinyColorMode.Dark,
                                    themeConfig = themes.pan,
                                )
                            }
                            CaseType.XIAOLIUREN -> {
                                val p = remember(profile.payload) { XiaoLiuRenPayload.decode(profile.payload) }
                                XiaoLiuRenCaseViewer(
                                    mode = p?.mode ?: "NOW", question = p?.question ?: profile.name,
                                    solarYear = p?.solarYear ?: 0, solarMonth = p?.solarMonth ?: 0,
                                    solarDay = p?.solarDay ?: 0, solarHour = p?.solarHour ?: 0,
                                    solarMinute = p?.solarMinute ?: 0,
                                    lunarMonth = p?.lunarMonth ?: 0, lunarDay = p?.lunarDay ?: 0,
                                    hourBranchIndex = p?.hourBranchIndex ?: 0,
                                    isLeapMonth = p?.isLeapMonth ?: false,
                                    countMonth = p?.countMonth ?: 0, countDay = p?.countDay ?: 0,
                                    countHour = p?.countHour ?: 0,
                                    onBack = { goBack() },
                                    darkTheme = colorMode == DestinyColorMode.Dark,
                                    themeConfig = themes.xlr,
                                )
                            }
                            CaseType.MEIHUA -> {
                                val p = remember(profile.payload) { MeiHuaPayload.decode(profile.payload) }
                                SimpleCaseDetail(
                                    title = "梅花卦例 · ${profile.name}",
                                    rows = listOfNotNull(
                                        p?.question?.takeIf { it.isNotBlank() }?.let { "占问" to it },
                                        p?.summary?.takeIf { it.isNotBlank() }?.let { "起卦" to it },
                                        profile.note.takeIf { it.isNotBlank() }?.let { "详情" to it },
                                    ),
                                    onBack = { goBack() },
                                )
                            }
                            CaseType.QIMEN -> {
                                val p = remember(profile.payload) { QimenPayload.decode(profile.payload) }
                                SimpleCaseDetail(
                                    title = "奇门局例 · ${profile.name}",
                                    rows = listOfNotNull(
                                        p?.question?.takeIf { it.isNotBlank() }?.let { "占问" to it },
                                        p?.solarText?.takeIf { it.isNotBlank() }?.let { "时间" to it },
                                        p?.pillarText?.takeIf { it.isNotBlank() }?.let { "四柱" to it },
                                        listOfNotNull(p?.yuanText, p?.dunText).joinToString(" · ").takeIf { it.isNotBlank() }?.let { "局式" to it },
                                        profile.note.takeIf { it.isNotBlank() }?.let { "详情" to it },
                                    ),
                                    onBack = { goBack() },
                                )
                            }
                            else -> ChartPagerScreen(
                                profile = profile,
                                repository = repository,
                                onBack = { goBack() },
                                colorMode = colorMode,
                                onColorModeChange = ::setColorMode,
                                onSaveQizhengPanZhi = { panZhi ->
                                    scope.launch {
                                        repository.upsert(profile.copy(qizhengPanZhi = panZhi))
                                    }
                                },
                                onEditProfile = { navController.navigate(Routes.edit(profile.id)) },
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

/** 构建数据库实例（可能抛出 Room 的 schema 不兼容异常）。由各平台 actual 实现。 */
expect fun createProfileRepository(): ProfileRepository

/** 删除旧数据库文件与本地批注图片目录，用于 schema 不兼容时由用户确认后重建。 */
expect fun deleteAppData()

/** 退出应用；Web 端无进程可退时为 no-op。 */
expect fun exitApp()

@Composable
expect fun rememberProfileRepository(): ProfileRepository
