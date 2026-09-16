package zhiqiu.app.destiny

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import zhiqiu.app.destiny.db.getDatabaseBuilder
import zhiqiu.app.destiny.db.getRoomDatabase
import zhiqiu.app.destiny.profile.ProfileRepository
import zhiqiu.app.destiny.sharing.ImageStorage

actual fun createProfileRepository(): ProfileRepository {
    val fs = FakeFileSystem().apply {
        emulateUnix()
        createDirectories("/destiny-images".toPath())
    }
    val imageStorage = ImageStorage(imagesRoot = "/destiny-images", fs = fs)
    return ProfileRepository(getRoomDatabase(getDatabaseBuilder()), imageStorage)
}

actual fun deleteAppData() {
    // 内存存储，刷新页面即重置
}

actual fun exitApp() {
    // Web 无独立进程；留在当前页即可
}

@Composable
actual fun rememberProfileRepository(): ProfileRepository = remember { createProfileRepository() }
