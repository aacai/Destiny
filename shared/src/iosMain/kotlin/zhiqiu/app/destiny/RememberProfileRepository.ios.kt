package zhiqiu.app.destiny

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import zhiqiu.app.destiny.db.AppDatabase
import zhiqiu.app.destiny.profile.ProfileRepository

private fun documentDirectory(): String {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    return paths.first() as String
}

@Composable
actual fun rememberProfileRepository(): ProfileRepository {
    return remember {
        val dbFile = documentDirectory() + "/destiny.db"
        val db = Room.databaseBuilder<AppDatabase>(name = dbFile)
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration()
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        ProfileRepository(db)
    }
}
