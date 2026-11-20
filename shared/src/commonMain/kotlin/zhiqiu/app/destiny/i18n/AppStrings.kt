package zhiqiu.app.destiny.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

object AppStrings {
    enum class Language { ZH, EN }

    private val languageState = mutableStateOf(Language.ZH)

    var language: Language
        get() = languageState.value
        set(value) { languageState.value = value }

    private val zh = mapOf(
        "back" to "返回",
        "back_short" to "‹ 返回",
        "save" to "保存",
        "cancel" to "取消",
        "confirm" to "确定",
        "done" to "完成",
        "close" to "关闭",
        "clear" to "清除",
        "delete" to "删除",
        "all" to "全部",
        "unnamed" to "未命名",
        "default_group" to "默认",
        "add_profile" to "添加档案",
        "edit_profile" to "编辑档案",
        "basic_info" to "基本信息",
        "archive" to "档案",
        "share_link" to "分享到链接",
        "import_link" to "从链接导入",
        "about" to "关于",
        "bookshelf" to "书架",
        "contents" to "目录",
        "settings" to "设置",
        "password" to "密码",
        "export_failed" to "导出失败",
        "import_success" to "导入成功",
        "bazi" to "八字",
        "ziwei" to "紫微",
        "qizheng" to "七政",
        "case" to "案例",
        "male" to "男",
        "female" to "女",
        "solar" to "公历",
        "lunar" to "农历",
        "got_it" to "知道了",
        "day_mode" to "日间模式",
        "night_mode" to "夜间模式",
        "data_backup" to "数据与备份",
        "export_backup" to "导出备份",
        "import_backup" to "导入备份",
        "ancient_texts" to "古籍原文",
    )

    private val en = mapOf(
        "back" to "Back",
        "back_short" to "‹ Back",
        "save" to "Save",
        "cancel" to "Cancel",
        "confirm" to "OK",
        "done" to "Done",
        "close" to "Close",
        "clear" to "Clear",
        "delete" to "Delete",
        "all" to "All",
        "unnamed" to "Untitled",
        "default_group" to "Default",
        "add_profile" to "Add Profile",
        "edit_profile" to "Edit Profile",
        "basic_info" to "Basic Info",
        "archive" to "Archive",
        "share_link" to "Share Link",
        "import_link" to "Import from Link",
        "about" to "About",
        "bookshelf" to "Bookshelf",
        "contents" to "Contents",
        "settings" to "Settings",
        "password" to "Password",
        "export_failed" to "Export failed",
        "import_success" to "Import successful",
        "bazi" to "Bazi",
        "ziwei" to "Zi Wei",
        "qizheng" to "Qizheng",
        "case" to "Case",
        "male" to "Male",
        "female" to "Female",
        "solar" to "Gregorian",
        "lunar" to "Lunar",
        "got_it" to "Got it",
        "day_mode" to "Day mode",
        "night_mode" to "Night mode",
        "data_backup" to "Data & Backup",
        "export_backup" to "Export Backup",
        "import_backup" to "Import Backup",
        "ancient_texts" to "Ancient Texts",
    )

    fun t(key: String): String =
        (if (languageState.value == Language.ZH) zh else en)[key] ?: key

    val back: String @Composable get() = t("back")
    val backShort: String @Composable get() = t("back_short")
    val save: String @Composable get() = t("save")
    val cancel: String @Composable get() = t("cancel")
    val confirm: String @Composable get() = t("confirm")
    val done: String @Composable get() = t("done")
    val close: String @Composable get() = t("close")
    val clear: String @Composable get() = t("clear")
    val delete: String @Composable get() = t("delete")
    val all: String @Composable get() = t("all")
    val unnamed: String @Composable get() = t("unnamed")
    val defaultGroup: String @Composable get() = t("default_group")
    val addProfile: String @Composable get() = t("add_profile")
    val editProfile: String @Composable get() = t("edit_profile")
    val basicInfo: String @Composable get() = t("basic_info")
    val archive: String @Composable get() = t("archive")
    val shareLink: String @Composable get() = t("share_link")
    val importLink: String @Composable get() = t("import_link")
    val about: String @Composable get() = t("about")
    val bookshelf: String @Composable get() = t("bookshelf")
    val contents: String @Composable get() = t("contents")
    val settings: String @Composable get() = t("settings")
    val password: String @Composable get() = t("password")
    val exportFailed: String @Composable get() = t("export_failed")
    val importSuccess: String @Composable get() = t("import_success")
    val bazi: String @Composable get() = t("bazi")
    val ziwei: String @Composable get() = t("ziwei")
    val qizheng: String @Composable get() = t("qizheng")
    val case: String @Composable get() = t("case")
    val male: String @Composable get() = t("male")
    val female: String @Composable get() = t("female")
    val solar: String @Composable get() = t("solar")
    val lunar: String @Composable get() = t("lunar")
    val gotIt: String @Composable get() = t("got_it")
    val dayMode: String @Composable get() = t("day_mode")
    val nightMode: String @Composable get() = t("night_mode")
    val dataBackup: String @Composable get() = t("data_backup")
    val exportBackup: String @Composable get() = t("export_backup")
    val importBackup: String @Composable get() = t("import_backup")
    val ancientTexts: String @Composable get() = t("ancient_texts")
}
