package zhiqiu.app.destiny.ui.books

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zhiqiu.app.destiny.db.ReaderPrefDao
import zhiqiu.app.destiny.db.ReaderPrefEntity

/** 阅读器偏好/进度存取：底层为 Room 键值表 reader_pref */
class ReaderStore(private val dao: ReaderPrefDao) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun get(key: String): String? = withContext(Dispatchers.Default) {
        dao.get(key)
    }

    fun put(key: String, value: String) {
        scope.launch { dao.upsert(ReaderPrefEntity(key, value)) }
    }
}
