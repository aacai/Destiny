package zhiqiu.app.destiny.db

import androidx.room3.RoomDatabase
import kotlinx.coroutines.Dispatchers

/**
 * 由各平台 [getDatabaseBuilder] 配好驱动后，统一在此装配协程上下文与迁移策略。
 *
 * 注意：不要用 [AppDatabaseConstructor.initialize()] 直接取库——Room 为 @ConstructedBy 生成的
 * initialize() 体是未初始化连接的裸 AppDatabase_Impl，运行时会在 RoomDatabase 内部抛
 * UninitializedPropertyAccessException。必须经由 Room.databaseBuilder 装配驱动后再 build()。
 *
 * 驱动必须在各平台 builder 上预先 [RoomDatabase.Builder.setDriver]：
 * Android / JVM / iOS 用 BundledSQLiteDriver；Web（wasm）用 WebWorkerSQLiteDriver。
 */
fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .setQueryCoroutineContext(Dispatchers.Default)
        .fallbackToDestructiveMigration()
        .build()
}
