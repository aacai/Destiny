package zhiqiu.app.destiny

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import zhiqiu.app.destiny.db.getRoomDatabase
import zhiqiu.app.destiny.db.getDatabaseBuilder
import zhiqiu.app.destiny.profile.ProfileRepository
import zhiqiu.app.destiny.sharing.ImageStorage
import java.io.File

actual fun createProfileRepository(): ProfileRepository {
    val dbFile = File(System.getProperty("user.home"), ".destiny/destiny.db")
    val imageStorage = ImageStorage(dbFile.parentFile.resolve("images").absolutePath)
    return ProfileRepository(getRoomDatabase(getDatabaseBuilder()), imageStorage)
}

actual fun deleteAppData() {
    val dir = File(System.getProperty("user.home"), ".destiny")
    File(dir, "destiny.db").delete()
    File(dir, "destiny.db-wal").delete()
    File(dir, "destiny.db-shm").delete()
    File(dir, "images").deleteRecursively()
}

@Composable
actual fun rememberProfileRepository(): ProfileRepository = remember { createProfileRepository() }
