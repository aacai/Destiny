package zhiqiu.app.destiny.db

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * 由各平台 [getDatabaseBuilder] 拿到 [RoomDatabase.Builder] 后，统一在此装配驱动与协程上下文。
 *
 * 注意：不要用 [AppDatabaseConstructor.initialize()] 直接取库——Room 为 @ConstructedBy 生成的
 * initialize() 体是未初始化连接的裸 AppDatabase_Impl，运行时会在 RoomDatabase 内部抛
 * UninitializedPropertyAccessException。必须经由 Room.databaseBuilder 装配驱动后再 build()。
 */
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration()
        .build()
}
