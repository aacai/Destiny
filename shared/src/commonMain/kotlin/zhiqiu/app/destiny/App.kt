package zhiqiu.app.destiny

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import kotlinx.coroutines.launch
import zhiqiu.app.destiny.profile.ProfileRepository
import zhiqiu.app.destiny.profile.exportAllJson
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
    App(repository = rememberProfileRepository())
}

@Composable
fun App(repository: ProfileRepository) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val profiles by repository.observeAll().collectAsState(initial = emptyList())
        val readerStore = remember(repository) { repository.readerStore }
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
                    onExportAll = {
                        val prefs = repository.getAllPrefs()
                        exportAllJson(profiles, prefs)
                    },
                    onImportAll = { profileList, prefList ->
                        profileList.forEach { repository.upsert(it) }
                        repository.upsertAllPrefs(prefList)
                    },
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
                        onBack = { navController.popBackStack() },
                        onSaveQizhengNote = { note ->
                            scope.launch {
                                repository.upsert(profile.copy(qizhengNote = note))
                            }
                        },
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

@Composable
expect fun rememberProfileRepository(): ProfileRepository
