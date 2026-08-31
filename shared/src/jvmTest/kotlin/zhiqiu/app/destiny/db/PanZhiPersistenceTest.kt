package zhiqiu.app.destiny.db

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import zhiqiu.app.destiny.db.AppDatabase
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.profile.ProfileRepository
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 盘制持久化自测：验证 Profile 能经 Room 正常写入/读回（含 qizhengPanZhi 列）。
 *
 * 数据库层已切换到 Room，schema 变更走 fallbackToDestructiveMigration（删库重建），
 * 因此不再需要手写的 SQLDelight 迁移逻辑。
 */
class PanZhiPersistenceTest {

    @Test
    fun round_trip_profile_with_panZhi() {
        val dbFile = File(System.getProperty("java.io.tmpdir"), "destiny-test.db")
        dbFile.delete()
        val db = Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration()
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        val repo = ProfileRepository(db)

        val saved = runBlocking {
            repo.upsert(
                Profile(
                    id = "x", name = "新档", gender = "女", birthdayType = "solar",
                    birthday = "2000-01-01", timeIndex = 1,
                    qizhengPanZhi = "印度制", createdAt = 0, updatedAt = 0,
                ),
            )
        }
        assertEquals("印度制", saved.qizhengPanZhi)
        assertEquals("印度制", runBlocking { repo.getById("x") }?.qizhengPanZhi)
    }
}
