package zhiqiu.app.destiny.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import zhiqiu.app.destiny.profile.Profile

@Dao
interface ProfileDao {
    @Query("SELECT * FROM Profile ORDER BY updatedAt DESC")
    fun selectAll(): Flow<List<Profile>>

    @Query("SELECT * FROM Profile WHERE id = :id")
    suspend fun selectById(id: String): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: Profile)

    @Query("DELETE FROM Profile WHERE id = :id")
    suspend fun deleteById(id: String)
}
