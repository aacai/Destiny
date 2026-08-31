package zhiqiu.app.destiny.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import zhiqiu.app.destiny.db.AppDatabase
import zhiqiu.app.destiny.db.ReaderPrefEntity
import zhiqiu.app.destiny.ui.books.ReaderStore
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ProfileRepository(private val database: AppDatabase) {
    private val dao get() = database.profileDao()

    /** 阅读器偏好/进度存取（Room 键值表） */
    val readerStore: ReaderStore = ReaderStore(database.readerPrefDao())

    fun observeAll(): Flow<List<Profile>> = dao.selectAll()

    suspend fun getAllPrefs(): List<ReaderPrefEntity> = withContext(Dispatchers.Default) {
        database.readerPrefDao().getAll()
    }

    suspend fun upsertAllPrefs(list: List<ReaderPrefEntity>) = withContext(Dispatchers.Default) {
        val dao = database.readerPrefDao()
        for (item in list) dao.upsert(item)
    }

    suspend fun getById(id: String): Profile? = withContext(Dispatchers.Default) {
        dao.selectById(id)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun upsert(profile: Profile): Profile = withContext(Dispatchers.Default) {
        val now = profile.updatedAt.takeIf { it > 0L }
            ?: Clock.System.now().toEpochMilliseconds()
        val id = profile.id.ifBlank { newId() }
        val createdAt = if (profile.id.isBlank()) now else profile.createdAt
        val saved = profile.copy(id = id, createdAt = createdAt, updatedAt = now)
        dao.insert(saved)
        saved
    }

    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        dao.deleteById(id)
    }
}

private fun newId(): String =
    buildString(32) {
        repeat(32) {
            append("abcdefghijklmnopqrstuvwxyz0123456789"[Random.nextInt(36)])
        }
    }
