package zhiqiu.app.destiny.db

import androidx.room3.Database
import androidx.room3.RoomDatabase
import zhiqiu.app.destiny.profile.Profile

@Database(entities = [Profile::class, ReaderPrefEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun readerPrefDao(): ReaderPrefDao
}
