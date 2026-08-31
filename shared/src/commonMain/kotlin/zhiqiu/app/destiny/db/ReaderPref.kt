package zhiqiu.app.destiny.db

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert

/** 阅读器等轻量偏好的键值表 */
@Entity(tableName = "reader_pref")
data class ReaderPrefEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface ReaderPrefDao {
    @Query("SELECT value FROM reader_pref WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Upsert
    suspend fun upsert(entity: ReaderPrefEntity)
}
