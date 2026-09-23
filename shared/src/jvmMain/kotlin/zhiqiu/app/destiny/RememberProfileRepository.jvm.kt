package zhiqiu.app.destiny

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import okio.FileSystem
import zhiqiu.app.destiny.db.getRoomDatabase
import zhiqiu.app.destiny.db.getDatabaseBuilder
import zhiqiu.app.destiny.profile.ProfileRepository
import zhiqiu.app.destiny.sharing.ImageStorage
import java.io.File
import kotlin.system.exitProcess

actual fun createProfileRepository(): ProfileRepository {
    val dbFile = File(System.getProperty("user.home"), ".destiny/destiny.db")
    val imageStorage = ImageStorage(dbFile.parentFile.resolve("images").absolutePath, FileSystem.SYSTEM)
    return ProfileRepository(getRoomDatabase(getDatabaseBuilder()), imageStorage)
}

actual fun deleteAppData() {
    val dir = File(System.getProperty("user.home"), ".destiny")
    File(dir, "destiny.db").delete()
    File(dir, "destiny.db-wal").delete()
    File(dir, "destiny.db-shm").delete()
    File(dir, "images").deleteRecursively()
}

actual fun exitApp() {
    exitProcess(0)
}

@Composable
actual fun rememberProfileRepository(): ProfileRepository = remember { createProfileRepository() }
