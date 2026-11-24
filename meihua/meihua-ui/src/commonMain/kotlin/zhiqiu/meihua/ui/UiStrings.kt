package zhiqiu.meihua.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

object MeiHuaStrings {
    enum class Language { ZH, EN }

    private val languageState = mutableStateOf(Language.ZH)

    var language: Language
        get() = languageState.value
        set(value) { languageState.value = value }

    private val zh = mapOf(
        "back_short" to "‹ 返回",
        "save_gua" to "保存此卦",
        "now" to "此刻",
        "lunar" to "农历",
        "numbers" to "报数",
        "question" to "占问",
        "rule" to "规则说明",
        "edit" to "编辑",
        "note" to "备注",
    )

    private val en = mapOf(
        "back_short" to "‹ Back",
        "save_gua" to "Save Case",
        "now" to "Now",
        "lunar" to "Lunar",
        "numbers" to "Numbers",
        "question" to "Question",
        "rule" to "Rules",
        "edit" to "Edit",
        "note" to "Note",
    )

    fun t(key: String): String =
        (if (languageState.value == Language.ZH) zh else en)[key] ?: key

    val backShort: String @Composable get() = t("back_short")
    val saveGua: String @Composable get() = t("save_gua")
    val now: String @Composable get() = t("now")
    val lunar: String @Composable get() = t("lunar")
    val numbers: String @Composable get() = t("numbers")
    val question: String @Composable get() = t("question")
    val rule: String @Composable get() = t("rule")
    val edit: String @Composable get() = t("edit")
    val note: String @Composable get() = t("note")
}
