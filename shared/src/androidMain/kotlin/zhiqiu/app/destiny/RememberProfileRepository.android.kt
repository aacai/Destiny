package zhiqiu.app.destiny

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import zhiqiu.app.destiny.db.AppDatabase
import zhiqiu.app.destiny.profile.ProfileRepository

@Composable
actual fun rememberProfileRepository(): ProfileRepository {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val dbFile = context.getDatabasePath("destiny.db")
        val db = Room.databaseBuilder<AppDatabase>(
            context = context,
            name = dbFile.absolutePath,
        ).setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration()
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        ProfileRepository(db)
    }
}
