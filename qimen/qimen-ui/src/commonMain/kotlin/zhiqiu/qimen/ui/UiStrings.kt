package zhiqiu.qimen.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

object QimenStrings {
    enum class Language { ZH, EN }

    private val languageState = mutableStateOf(Language.ZH)

    var language: Language
        get() = languageState.value
        set(value) { languageState.value = value }

    private val zh = mapOf(
        "back_short" to "‹ 返回",
        "save_ju" to "保存此局",
        "now" to "此刻",
        "pick" to "自选",
        "question" to "占问",
    )

    private val en = mapOf(
        "back_short" to "‹ Back",
        "save_ju" to "Save Case",
        "now" to "Now",
        "pick" to "Pick",
        "question" to "Question",
    )

    fun t(key: String): String =
        (if (languageState.value == Language.ZH) zh else en)[key] ?: key

    val backShort: String @Composable get() = t("back_short")
    val saveJu: String @Composable get() = t("save_ju")
    val now: String @Composable get() = t("now")
    val pick: String @Composable get() = t("pick")
    val question: String @Composable get() = t("question")
}
