package zhiqiu.app.destiny.profile

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 占卜案例的结构化起课数据。字段全部用基础类型，避免起课模块依赖 shared。
 * App.kt 在保存时把这些字段编码成 Profile.payload，查看时解码后交给起课模块重建盘面。
 */

@Serializable
data class LiuYaoPayload(
    /** 六爻名（YaoKind.name）：LAO_YANG/SHAO_YANG/SHAO_YIN/LAO_YIN，6 个。 */
    val yaoKinds: List<String> = emptyList(),
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,
    val hour: Int = 0,
    val minute: Int = 0,
    /** ZhanShi.name */
    val topic: String = "NONE",
    val sub: String = "",
    val guaZhu: String = "",
    val gender: String = "男",
    val question: String = "",
) {
    companion object {
        fun decode(json: String): LiuYaoPayload? = runCatching {
            CasePayloadJson.decodeFromString<LiuYaoPayload>(json)
        }.getOrNull()
    }

    fun encode(): String = CasePayloadJson.encodeToString(this)
}

@Serializable
data class XiaoLiuRenPayload(
    /** 起课模式：NOW/SOLAR/LUNAR/COUNTS */
    val mode: String = "NOW",
    val question: String = "",
    val solarYear: Int = 0,
    val solarMonth: Int = 0,
    val solarDay: Int = 0,
    val solarHour: Int = 0,
    val solarMinute: Int = 0,
    val lunarMonth: Int = 0,
    val lunarDay: Int = 0,
    val hourBranchIndex: Int = 0,
    val isLeapMonth: Boolean = false,
    val countMonth: Int = 0,
    val countDay: Int = 0,
    val countHour: Int = 0,
) {
    companion object {
        fun decode(json: String): XiaoLiuRenPayload? = runCatching {
            CasePayloadJson.decodeFromString<XiaoLiuRenPayload>(json)
        }.getOrNull()
    }

    fun encode(): String = CasePayloadJson.encodeToString(this)
}

private val CasePayloadJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
