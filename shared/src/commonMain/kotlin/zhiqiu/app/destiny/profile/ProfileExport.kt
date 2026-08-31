package zhiqiu.app.destiny.profile

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import zhiqiu.crypto.aes256Ctr
import zhiqiu.crypto.constantTimeEquals
import zhiqiu.crypto.fromHex
import zhiqiu.crypto.hmacSha256
import zhiqiu.crypto.pbkdf2HmacSha256
import zhiqiu.crypto.randomBytes
import zhiqiu.crypto.toHex
import zhiqiu.app.destiny.db.ReaderPrefEntity

/**
 * 全量数据备份结构。包含档案表与阅读偏好键值表，导入时兼容本结构；
 * 同时也兼容「仅含 profiles 数组」的旧版 / 手工编辑形式。
 */
@Serializable
private data class DataBackup(
    val app: String = "Destiny",
    val version: Int = 1,
    val profiles: List<Profile>,
    val readerPrefs: List<ReaderPrefEntity> = emptyList(),
)

/**
 * 加密导出信封。未加密的备份不含这些字段（[encrypted] 缺省为 false）。
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
    val iv: String = "",
    val mac: String = "",
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

/** 将所有档案与阅读偏好序列化为可读性好的 JSON 字符串（明文） */
fun exportAllJson(profiles: List<Profile>, readerPrefs: List<ReaderPrefEntity>): String {
    return json.encodeToString(DataBackup(profiles = profiles, readerPrefs = readerPrefs))
}

/**
 * 将明文备份 JSON 用密码加密，返回信封 JSON 字符串。
 * 构造：PBKDF2-HMAC-SHA256 派生 32 字节密钥 → AES-256-CTR 加密 → HMAC-SHA256 认证（Encrypt-then-MAC）。
 */
fun exportEncryptedJson(plaintext: String, password: String): String {
    require(password.isNotBlank()) { "密码不能为空" }
    val salt = randomBytes(16)
    val iv = randomBytes(16)
    val key = pbkdf2HmacSha256(password.toByteArray(UTF8), salt, PBKDF2_ITERATIONS, KEY_LEN)
    val ct = aes256Ctr(key, iv, plaintext.toByteArray(UTF8))
    val mac = hmacSha256(key, iv + ct)
    return json.encodeToString(
        BackupEnvelope(
            encrypted = true,
            kdf = "pbkdf2-hmac-sha256",
            cipher = "aes-256-ctr-hmac-sha256",
            iterations = PBKDF2_ITERATIONS,
            salt = salt.toHex(),
            iv = iv.toHex(),
            mac = mac.toHex(),
            data = ct.toHex(),
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
)

/**
 * 从 JSON 解析出档案与阅读偏好。自动识别加密信封（此时需要 [password]）。
 * - 未加密：支持 [DataBackup] 包裹结构或裸 profiles 数组两种形式。
 * - 已加密：用密码解信封得到明文后再解析；密码错误或服务被篡改会抛异常。
 * 解析失败（包括未提供密码的加密备份）抛出异常，由调用方捕获并提示用户。
 */
fun importAllFromJson(text: String, password: String? = null): ParsedBackup {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return ParsedBackup(emptyList(), emptyList())
    val plainText = if (isEncryptedBackup(trimmed)) {
        val pw = password ?: throw IllegalArgumentException("该备份已加密，请输入密码")
        decryptEnvelope(trimmed, pw)
    } else {
        trimmed
    }
    val backup = runCatching { json.decodeFromString<DataBackup>(plainText) }
        .getOrElse {
            val profiles = json.decodeFromString<List<Profile>>(plainText)
            return ParsedBackup(profiles, emptyList())
        }
    return ParsedBackup(backup.profiles, backup.readerPrefs)
}

private fun decryptEnvelope(envelopeText: String, password: String): String {
    val env = json.decodeFromString<BackupEnvelope>(envelopeText)
    require(env.encrypted) { "并非加密备份" }
    val salt = env.salt.fromHex()
    val iv = env.iv.fromHex()
    val ct = env.data.fromHex()
    val mac = env.mac.fromHex()
    val key = pbkdf2HmacSha256(password.toByteArray(UTF8), salt, env.iterations, KEY_LEN)
    val expectedMac = hmacSha256(key, iv + ct)
    if (!constantTimeEquals(mac, expectedMac)) {
        throw IllegalArgumentException("密码错误或数据已被篡改")
    }
    return aes256Ctr(key, iv, ct).toString(UTF8)
}
