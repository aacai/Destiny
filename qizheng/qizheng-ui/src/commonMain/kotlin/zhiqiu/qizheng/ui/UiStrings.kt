package zhiqiu.qizheng.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

object QizhengStrings {
    enum class Language { ZH, EN }

    private val languageState = mutableStateOf(Language.ZH)

    var language: Language
        get() = languageState.value
        set(value) { languageState.value = value }

    private val zh = mapOf(
        "back" to "← 返回",
        "settings" to "七政四余设置",
    )

    private val en = mapOf(
        "back" to "← Back",
        "settings" to "Qizheng Settings",
    )

    fun t(key: String): String =
        (if (languageState.value == Language.ZH) zh else en)[key] ?: key

    val back: String @Composable get() = t("back")
    val settings: String @Composable get() = t("settings")
}
