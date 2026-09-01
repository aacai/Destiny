package zhiqiu.app.destiny

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import kotlinx.coroutines.launch
import kotlin.system.exitProcess
import zhiqiu.app.destiny.profile.ProfileRepository
import zhiqiu.app.destiny.sharing.BackupSharing
import zhiqiu.app.destiny.sharing.FileIoClient
import zhiqiu.app.destiny.sharing.createSharedHttpClient
import zhiqiu.app.destiny.ui.AddProfileScreen
import zhiqiu.app.destiny.ui.ChartPagerScreen
import zhiqiu.app.destiny.ui.ProfileListScreen
import zhiqiu.app.destiny.ui.books.BookshelfScreen
import zhiqiu.app.destiny.ui.books.ReaderScreen

private object Routes {
    const val List = "list"
    const val Add = "add"
    const val Chart = "chart/{profileId}"
    const val Books = "books"
    const val Reader = "reader/{bookId}"

    fun chart(profileId: String) = "chart/$profileId"
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
                TextButton(onClick = { exitProcess(0) }) { Text("退出应用") }
            },
        )
    }
}

@Composable
fun App(repository: ProfileRepository) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val profiles by repository.observeAll().collectAsState(initial = emptyList())
        val readerStore = remember(repository) { repository.readerStore }
        val sharing = remember { BackupSharing(FileIoClient(createSharedHttpClient())) }
        val navController = rememberNavController()

        NavHost(
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
                )
            }
            composable(Routes.Books) {
                BookshelfScreen(
                    onBack = { navController.popBackStack() },
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
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Add) {
                AddProfileScreen(
                    onSave = { profile ->
                        scope.launch {
                            repository.upsert(profile)
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() },
                )
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
                        navController.popBackStack()
                    }
                } else {
                    ChartPagerScreen(
                        profile = profile,
                        repository = repository,
                        onBack = { navController.popBackStack() },
                        onSaveQizhengPanZhi = { panZhi ->
                            scope.launch {
                                repository.upsert(profile.copy(qizhengPanZhi = panZhi))
                            }
                        },
                    )
                }
            }
        }
    }
}

/** 构建数据库实例（可能抛出 Room 的 schema 不兼容异常）。由各平台 actual 实现。 */
expect fun createProfileRepository(): ProfileRepository

/** 删除旧数据库文件与本地批注图片目录，用于 schema 不兼容时由用户确认后重建。 */
expect fun deleteAppData()

@Composable
expect fun rememberProfileRepository(): ProfileRepository
