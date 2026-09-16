package zhiqiu.app.destiny.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Web MVP：内存库（刷新即丢）。驱动走 WebWorkerSQLiteDriver；
 * worker 内固定打开 `:memory:`，不做 OPFS 持久化。
 */
fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    return Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(WebWorkerSQLiteDriver(createSQLiteWorker()))
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun createSQLiteWorker(): Worker =
    js("new Worker(new URL('sqlite-wasm-worker/worker.js', import.meta.url), { type: 'module' })")
