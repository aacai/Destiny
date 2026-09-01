package zhiqiu.app.destiny.profile

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import zhiqiu.app.destiny.db.AppDatabase
import zhiqiu.app.destiny.sharing.ImageStorage
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileBackupTest {

    @Test
    fun exportImportRoundTripPreservesImages() {
        val baseDir = File(System.getProperty("java.io.tmpdir"), "destiny-bk-${Random.nextInt()}")
        baseDir.deleteRecursively()
        val dbFile = File(baseDir, "destiny.db")
        val imageStorage = ImageStorage(File(baseDir, "images").absolutePath)
        val db = Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration()
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        val repo = ProfileRepository(db, imageStorage)

        val profile = runBlocking {
            val p = repo.upsert(
                Profile(
                    id = "", name = "命盘A", gender = "男", birthdayType = "公历",
                    birthday = "1990-01-01", timeIndex = 12, createdAt = 0, updatedAt = 0,
                ),
            )
            repo.addImage(p.id, "bazi", byteArrayOf(1, 2, 3), "jpg")
            repo.addImage(p.id, "bazi", byteArrayOf(4, 5, 6, 7), "png")
            p
        }

        val zip = File(baseDir, "backup.zip").absolutePath
        runBlocking { repo.exportBackup(zip) }
        assertTrue(File(zip).exists(), "备份 zip 应已生成")

        // 清空数据后再导入，验证可完整恢复
        runBlocking {
            repo.delete(profile.id)
            assertEquals(0, repo.observeImages(profile.id).first().size)
            repo.importBackup(zip)
        }

        runBlocking {
            val images = repo.observeImages(profile.id).first()
            assertEquals(2, images.size)
            val bytes = images.mapNotNull { repo.readImage(it.relativePath) }
            assertTrue(bytes.any { it.contentEquals(byteArrayOf(1, 2, 3)) }, "jpg 内容应一致")
            assertTrue(bytes.any { it.contentEquals(byteArrayOf(4, 5, 6, 7)) }, "png 内容应一致")
        }

        baseDir.deleteRecursively()
    }
}
