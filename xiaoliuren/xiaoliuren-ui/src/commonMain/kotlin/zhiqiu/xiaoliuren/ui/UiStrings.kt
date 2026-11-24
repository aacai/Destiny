package zhiqiu.xiaoliuren.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

object XiaoLiuRenStrings {
    enum class Language { ZH, EN }

    private val languageState = mutableStateOf(Language.ZH)

    var language: Language
        get() = languageState.value
        set(value) { languageState.value = value }

    private val zh = mapOf(
        "back_short" to "‹ 返回",
        "save_course" to "保存此课",
        "now" to "此刻",
        "solar" to "公历",
        "lunar" to "农历",
        "counts" to "报数",
        "now_desc" to "取设备时间的农历月、农历日与时辰。",
        "solar_desc" to "手选公历年月日时分，自动换算成农历月日与时辰；可为过去的事件补课。",
        "lunar_desc" to "手选农历月日与时辰；闰月按本月计，只标注、不改落宫。",
        "counts_desc" to "随口报三个数，依次作月、日、时之数，推演与农历同式。",
        "rule" to "规则说明",
        "edit" to "编辑",
    )

    private val en = mapOf(
        "back_short" to "‹ Back",
        "save_course" to "Save Case",
        "now" to "Now",
        "solar" to "Gregorian",
        "lunar" to "Lunar",
        "counts" to "Numbers",
        "now_desc" to "Use the device time to derive lunar month, day and hour branch.",
        "solar_desc" to "Pick a Gregorian date and time; lunar month, day and hour are converted automatically.",
        "lunar_desc" to "Pick a lunar month, day and hour. Leap months are labeled but not moved.",
        "counts_desc" to "Enter three numbers for month, day and hour; deduction follows the lunar method.",
        "rule" to "Rules",
        "edit" to "Edit",
    )

    fun t(key: String): String =
        (if (languageState.value == Language.ZH) zh else en)[key] ?: key

    val backShort: String @Composable get() = t("back_short")
    val saveCourse: String @Composable get() = t("save_course")
    val now: String @Composable get() = t("now")
    val solar: String @Composable get() = t("solar")
    val lunar: String @Composable get() = t("lunar")
    val counts: String @Composable get() = t("counts")
    val nowDesc: String @Composable get() = t("now_desc")
    val solarDesc: String @Composable get() = t("solar_desc")
    val lunarDesc: String @Composable get() = t("lunar_desc")
    val countsDesc: String @Composable get() = t("counts_desc")
    val rule: String @Composable get() = t("rule")
    val edit: String @Composable get() = t("edit")
}
