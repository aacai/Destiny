package zhiqiu.app.destiny

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import zhiqiu.app.destiny.db.getRoomDatabase
import zhiqiu.app.destiny.db.getDatabaseBuilder
import zhiqiu.app.destiny.platform.applicationContext
import zhiqiu.app.destiny.profile.ProfileRepository
import zhiqiu.app.destiny.sharing.ImageStorage
import java.io.File

actual fun createProfileRepository(): ProfileRepository {
    val context = applicationContext
    val dbFile = context.getDatabasePath("destiny.db")!!
    val imageStorage = ImageStorage(dbFile.parentFile!!.resolve("images").absolutePath)
    return ProfileRepository(getRoomDatabase(getDatabaseBuilder(context)), imageStorage)
}

actual fun deleteAppData() {
    val dbFile = applicationContext.getDatabasePath("destiny.db")!!
    val parent = dbFile.parentFile
    if (parent != null) {
        File(parent, "destiny.db").delete()
        File(parent, "destiny.db-wal").delete()
        File(parent, "destiny.db-shm").delete()
        File(parent, "images").deleteRecursively()
    }
}

@Composable
actual fun rememberProfileRepository(): ProfileRepository = remember { createProfileRepository() }
