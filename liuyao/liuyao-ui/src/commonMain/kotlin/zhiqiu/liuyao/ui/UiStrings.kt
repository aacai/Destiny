package zhiqiu.liuyao.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

object LiuYaoStrings {
    enum class Language { ZH, EN }

    private val languageState = mutableStateOf(Language.ZH)

    var language: Language
        get() = languageState.value
        set(value) { languageState.value = value }

    private val zh = mapOf(
        "back_short" to "‹ 返回",
        "start" to "‹ 起卦",
        "restart" to "重起",
        "result" to "排盘结果",
        "case_detail" to "卦例详情",
        "save" to "保存",
    )

    private val en = mapOf(
        "back_short" to "‹ Back",
        "start" to "‹ Cast",
        "restart" to "Restart",
        "result" to "Chart Result",
        "case_detail" to "Case Detail",
        "save" to "Save",
    )

    fun t(key: String): String =
        (if (languageState.value == Language.ZH) zh else en)[key] ?: key

    val backShort: String @Composable get() = t("back_short")
    val start: String @Composable get() = t("start")
    val restart: String @Composable get() = t("restart")
    val result: String @Composable get() = t("result")
    val caseDetail: String @Composable get() = t("case_detail")
    val save: String @Composable get() = t("save")
}
