package zhiqiu.app.destiny

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import zhiqiu.app.destiny.db.AppDatabase
import zhiqiu.app.destiny.profile.ProfileRepository
import java.io.File

@Composable
actual fun rememberProfileRepository(): ProfileRepository {
    return remember {
        val dbFile = File(System.getProperty("user.home"), ".destiny/destiny.db")
        val db = Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration()
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        ProfileRepository(db)
    }
}
