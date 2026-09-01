package zhiqiu.app.destiny

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSFileManager
import zhiqiu.app.destiny.db.getRoomDatabase
import zhiqiu.app.destiny.db.getDatabaseBuilder
import zhiqiu.app.destiny.db.documentDirectory
import zhiqiu.app.destiny.profile.ProfileRepository
import zhiqiu.app.destiny.sharing.ImageStorage

actual fun createProfileRepository(): ProfileRepository {
    val dbFile = documentDirectory() + "/destiny.db"
    val imageStorage = ImageStorage(documentDirectory() + "/images")
    return ProfileRepository(getRoomDatabase(getDatabaseBuilder()), imageStorage)
}

actual fun deleteAppData() {
    val dir = documentDirectory()
    val fm = NSFileManager.defaultManager
    fm.removeItemAtPath(dir + "/destiny.db", null)
    fm.removeItemAtPath(dir + "/destiny.db-wal", null)
    fm.removeItemAtPath(dir + "/destiny.db-shm", null)
    fm.removeItemAtPath(dir + "/images", null)
}

@Composable
actual fun rememberProfileRepository(): ProfileRepository = remember { createProfileRepository() }
