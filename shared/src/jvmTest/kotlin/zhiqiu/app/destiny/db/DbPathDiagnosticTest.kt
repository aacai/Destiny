package zhiqiu.app.destiny.db

import androidx.room3.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import zhiqiu.app.destiny.profile.Profile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull

class DbPathDiagnosticTest {

    @Test
    fun getRoomDatabase_builds_file_db() {
        // 验证生产用的 getRoomDatabase(getDatabaseBuilder()) 装配路径：返回的是带驱动的“文件库”，
        // 而非 Room 为 @ConstructedBy 生成的裸 AppDatabase_Impl()（那种会抛 UninitializedPropertyAccessException）。
        val dbFile = File(System.getProperty("java.io.tmpdir"), "destiny-diag.db")
        dbFile.delete()
        val db = getRoomDatabase(Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath))
        runBlocking(Dispatchers.IO) {
            db.profileDao().insert(
                Profile(
                    id = "diag-x", name = "诊断", gender = "女", birthdayType = "solar",
                    birthday = "2000-01-01", timeIndex = 1,
                    qizhengPanZhi = "diag", createdAt = 0, updatedAt = 0,
                ),
            )
            assertNotNull(db.profileDao().selectById("diag-x"))
        }
    }
}
