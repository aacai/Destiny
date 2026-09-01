package zhiqiu.app.destiny.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import zhiqiu.app.destiny.profile.Profile

@Database(
    entities = [Profile::class, ReaderPrefEntity::class, ProfileImage::class],
    version = 4,
    exportSchema = false,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun readerPrefDao(): ReaderPrefDao
    abstract fun profileImageDao(): ProfileImageDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase>
