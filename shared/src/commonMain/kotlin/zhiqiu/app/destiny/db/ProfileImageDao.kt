package zhiqiu.app.destiny.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileImageDao {
    /** 某案例下的全部图片，按显示顺序排序 */
    @Query(
        "SELECT * FROM ProfileImage " +
            "WHERE profileId = :profileId " +
            "ORDER BY sortOrder ASC, createdAt ASC",
    )
    fun selectByProfile(profileId: String): Flow<List<ProfileImage>>

    /** 某案例下某个模块（八字/紫微/七政）的图片 */
    @Query(
        "SELECT * FROM ProfileImage " +
            "WHERE profileId = :profileId AND category = :category " +
            "ORDER BY sortOrder ASC, createdAt ASC",
    )
    fun selectByProfileAndCategory(profileId: String, category: String): Flow<List<ProfileImage>>

    @Query("SELECT * FROM ProfileImage ORDER BY profileId ASC, sortOrder ASC, createdAt ASC")
    fun selectAll(): Flow<List<ProfileImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ProfileImage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(images: List<ProfileImage>)

    @Query("DELETE FROM ProfileImage WHERE id = :id")
    suspend fun deleteById(id: String)

    /** 删除某案例下某个模块的所有图片（导入同 id 覆盖前先清理） */
    @Query("DELETE FROM ProfileImage WHERE profileId = :profileId AND category = :category")
    suspend fun deleteByProfileAndCategory(profileId: String, category: String)
}
