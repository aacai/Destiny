package zhiqiu.app.destiny.profile

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import zhiqiu.crypto.chacha20Poly1305Open
import zhiqiu.crypto.chacha20Poly1305Seal
import zhiqiu.crypto.fromHex
import zhiqiu.crypto.pbkdf2HmacSha256
import zhiqiu.crypto.randomBytes
import zhiqiu.crypto.toHex
import zhiqiu.app.destiny.db.ProfileImage
import zhiqiu.app.destiny.db.ReaderPrefEntity

/**
 * 全量数据备份结构。包含档案表与阅读偏好键值表，导入时兼容本结构；
 * 同时也兼容「仅含 profiles 数组」的旧版 / 手工编辑形式。
 */
@Serializable
private data class DataBackup(
    val app: String = "Destiny",
    val version: Int = 2,
    val profiles: List<Profile>,
    val readerPrefs: List<ReaderPrefEntity> = emptyList(),
    val images: List<ProfileImage> = emptyList(),
)

/**
 * 加密导出信封（ChaCha20-Poly1305，RFC 8439）。未加密的备份不含这些字段（[encrypted] 缺省为 false）。
 * - [nonce]：ChaCha20-Poly1305 的 12 字节随机数（须对每个备份唯一）。
 * - [data]：`密文 || 16 字节 tag`。
 */
@Serializable
private data class BackupEnvelope(
    val app: String = "Destiny",
    val version: Int = 2,
    val encrypted: Boolean = false,
    val kdf: String = "",
    val cipher: String = "",
    val iterations: Int = 0,
    val salt: String = "",
    val nonce: String = "",
    val data: String = "",
)

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private const val PBKDF2_ITERATIONS = 120_000
private const val KEY_LEN = 32
private val UTF8 = charset("UTF-8")

/** 将所有档案、阅读偏好与图片元数据序列化为可读性好的 JSON 字符串（明文） */
fun exportAllJson(
    profiles: List<Profile>,
    readerPrefs: List<ReaderPrefEntity>,
    images: List<ProfileImage> = emptyList(),
): String {
    return json.encodeToString(
        DataBackup(profiles = profiles, readerPrefs = readerPrefs, images = images),
    )
}

/**
 * 将明文备份 JSON 用密码加密，返回信封 JSON 字符串。
 * 构造：PBKDF2-HMAC-SHA256 派生 32 字节密钥 → ChaCha20-Poly1305 认证加密（RFC 8439，AEAD 自带完整性校验）。
 */
fun exportEncryptedJson(plaintext: String, password: String): String {
    require(password.isNotBlank()) { "密码不能为空" }
    val salt = randomBytes(16)
    val nonce = randomBytes(12)
    val key = pbkdf2HmacSha256(password.toByteArray(UTF8), salt, PBKDF2_ITERATIONS, KEY_LEN)
    val sealed = chacha20Poly1305Seal(key, nonce, plaintext.toByteArray(UTF8))
    return json.encodeToString(
        BackupEnvelope(
            encrypted = true,
            kdf = "pbkdf2-hmac-sha256",
            cipher = "chacha20-poly1305",
            iterations = PBKDF2_ITERATIONS,
            salt = salt.toHex(),
            nonce = nonce.toHex(),
            data = sealed.toHex(),
        ),
    )
}

/** 判断一份文本是否为加密备份（用于 UI 决定是否要求输入密码） */
fun isEncryptedBackup(text: String): Boolean =
    runCatching { json.decodeFromString<BackupEnvelope>(text.trim()) }.getOrNull()?.encrypted == true

/** 解析后的备份数据 */
data class ParsedBackup(
    val profiles: List<Profile>,
    val readerPrefs: List<ReaderPrefEntity>,
    val images: List<ProfileImage>,
)

/**
 * 从 JSON 解析出档案与阅读偏好。自动识别加密信封（此时需要 [password]）。
 * - 未加密：支持 [DataBackup] 包裹结构或裸 profiles 数组两种形式。
 * - 已加密：用密码解信封得到明文后再解析；密码错误或服务被篡改会抛异常。
 * 解析失败（包括未提供密码的加密备份）抛出异常，由调用方捕获并提示用户。
 */
fun importAllFromJson(text: String, password: String? = null): ParsedBackup {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return ParsedBackup(emptyList(), emptyList(), emptyList())
    val plainText = if (isEncryptedBackup(trimmed)) {
        val pw = password ?: throw IllegalArgumentException("该备份已加密，请输入密码")
        decryptEnvelope(trimmed, pw)
    } else {
        trimmed
    }
    val backup = runCatching { json.decodeFromString<DataBackup>(plainText) }
        .getOrElse {
            val profiles = json.decodeFromString<List<Profile>>(plainText)
            return ParsedBackup(profiles, emptyList(), emptyList())
        }
    return ParsedBackup(backup.profiles, backup.readerPrefs, backup.images)
}

private fun decryptEnvelope(envelopeText: String, password: String): String {
    val env = json.decodeFromString<BackupEnvelope>(envelopeText)
    require(env.encrypted) { "并非加密备份" }
    require(env.cipher == "chacha20-poly1305") { "不支持的加密算法: ${env.cipher}" }
    val key = pbkdf2HmacSha256(password.toByteArray(UTF8), env.salt.fromHex(), env.iterations, KEY_LEN)
    return runCatching {
        chacha20Poly1305Open(key, env.nonce.fromHex(), env.data.fromHex()).toString(UTF8)
    }.getOrElse { throw IllegalArgumentException("密码错误或数据已被篡改") }
}
