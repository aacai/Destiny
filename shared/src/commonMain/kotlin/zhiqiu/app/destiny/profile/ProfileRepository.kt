package zhiqiu.app.destiny.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import zhiqiu.app.destiny.db.AppDatabase
import zhiqiu.app.destiny.db.ProfileImage
import zhiqiu.app.destiny.db.ReaderPrefEntity
import zhiqiu.app.destiny.sharing.BackupBundle
import zhiqiu.app.destiny.sharing.ImageStorage
import zhiqiu.app.destiny.ui.books.ReaderStore
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ProfileRepository(
    private val database: AppDatabase,
    private val imageStorage: ImageStorage,
) {
    private val dao get() = database.profileDao()
    private val imageDao get() = database.profileImageDao()

    /** 阅读器偏好/进度存取（Room 键值表） */
    val readerStore: ReaderStore = ReaderStore(database.readerPrefDao())

    fun observeAll(): Flow<List<Profile>> = dao.selectAll()

    suspend fun getAll(): List<Profile> = withContext(Dispatchers.Default) { dao.selectAll().first() }

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
        imageStorage.deleteByProfile(id)
        dao.deleteById(id)
    }

    // ---- 图片：存图 / 取图 / 删图 ----

    fun observeImages(profileId: String): Flow<List<ProfileImage>> = imageDao.selectByProfile(profileId)

    fun observeImages(profileId: String, category: String): Flow<List<ProfileImage>> =
        imageDao.selectByProfileAndCategory(profileId, category)

    /** 保存一张图片字节并登记元数据，返回其记录（供 UI 绑定） */
    @OptIn(ExperimentalTime::class)
    suspend fun addImage(
        profileId: String,
        category: String,
        bytes: ByteArray,
        extension: String,
    ): ProfileImage = withContext(Dispatchers.Default) {
        val relativePath = imageStorage.save(profileId, category, bytes, extension)
        val image = ProfileImage(
            id = newImageId(),
            profileId = profileId,
            category = category,
            relativePath = relativePath,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        imageDao.insert(image)
        image
    }

    /** 删除单张图片（磁盘文件 + 数据库记录） */
    suspend fun removeImage(image: ProfileImage) = withContext(Dispatchers.Default) {
        imageStorage.delete(image.relativePath)
        imageDao.deleteById(image.id)
    }

    /** 读取图片字节（预览/打包用）；不存在返回 null */
    fun readImage(relativePath: String): ByteArray? = imageStorage.read(relativePath)

    /** 图片本地绝对路径，供 Coil 等直接加载 */
    fun imageAbsolutePath(relativePath: String): String = imageStorage.absolutePath(relativePath)

    // ---- 整库备份（含图片，可加密）----

    /**
     * 导出整库为 zip 备份包：`backup.json`（档案 + 偏好 + 图片元数据，可选加密）+ `images/...`。
     *
     * @param zipPath 输出 zip 路径（已存在则覆盖）
     * @param password 非空则对 `backup.json` 用 ChaCha20-Poly1305 加密
     */
    suspend fun exportBackup(zipPath: String, password: String? = null) = withContext(Dispatchers.Default) {
        val outPath = zipPath.toPath()
        outPath.parent?.let { FileSystem.SYSTEM.createDirectories(it) }
        val profiles = dao.selectAll().first()
        val prefs = database.readerPrefDao().getAll()
        val images = imageDao.selectAll().first()
        val plain = exportAllJson(profiles, prefs, images)
        val backupJson = if (password.isNullOrBlank()) plain else exportEncryptedJson(plain, password)
        val imageBytes = images.associate { it.relativePath to (imageStorage.read(it.relativePath) ?: byteArrayOf()) }
        BackupBundle().pack(outPath, backupJson, imageBytes)
    }

    /**
     * 从 zip 备份包导入整库：先解包得到 `backup.json` 与图片字节，再写回数据库与本地图片目录。
     *
     * @param zipPath 输入 zip 路径
     * @param password 若备份已加密则提供密码；错误会抛 [IllegalArgumentException]
     */
    suspend fun importBackup(zipPath: String, password: String? = null) = withContext(Dispatchers.Default) {
        val unpacked = BackupBundle().unpack(zipPath.toPath())
        val parsed = importAllFromJson(unpacked.backupJson, password)
        for (p in parsed.profiles) upsert(p)
        upsertAllPrefs(parsed.readerPrefs)
        for (img in parsed.images) {
            val bytes = unpacked.images[img.relativePath] ?: continue
            imageStorage.writeAt(img.relativePath, bytes)
            imageDao.insert(img)
        }
    }

    /**
     * 导出整库备份包的字节（同 [exportBackup]，但返回 zip 字节，便于直接保存到文件或上传）。
     */
    suspend fun exportBackupBytes(password: String? = null): ByteArray = withContext(Dispatchers.Default) {
        val tmp = tempZipPath()
        exportBackup(tmp.toString(), password)
        FileSystem.SYSTEM.read(tmp) { readByteArray() }
    }

    /**
     * 从备份包字节导入整库（同 [importBackup]，但直接接收 zip 字节）。
     */
    suspend fun importBackupBytes(zipBytes: ByteArray, password: String? = null) = withContext(Dispatchers.Default) {
        val tmp = tempZipPath()
        tmp.parent?.let { FileSystem.SYSTEM.createDirectories(it) }
        FileSystem.SYSTEM.write(tmp) { write(zipBytes, 0, zipBytes.size) }
        try {
            importBackup(tmp.toString(), password)
        } finally {
            runCatching { FileSystem.SYSTEM.delete(tmp) }
        }
    }

    /** 在图片根目录下生成一个一次性的临时 zip 路径 */
    private fun tempZipPath(): Path =
        "${imageStorage.root}/.destiny-import-${Random.nextInt(1_000_000_000)}.zip".toPath()
}

private fun newId(): String =
    buildString(32) {
        repeat(32) {
            append("abcdefghijklmnopqrstuvwxyz0123456789"[Random.nextInt(36)])
        }
    }

private fun newImageId(): String = "img_" + newId()
