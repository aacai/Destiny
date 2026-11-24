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
        "saved" to "已保存",
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
        "continue_reading" to "继续阅读",
        "chapter_short" to "第 %1 章",
        "contents" to "目录",
        "settings" to "设置",
        "password" to "密码",
        "export_failed" to "导出失败",
        "import_success" to "导入成功",
        "bazi" to "八字",
        "ziwei" to "紫微",
        "qizheng" to "七政",
        "case" to "案例",
        "edit" to "编辑",
        "note" to "备注",
        "note_section" to "备注信息",
        "save_note" to "保存备注",
        "question" to "占问",
        "case_name" to "名称",
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
        "life_case" to "命例",
        "divination" to "卜卦",
        "new_life_case" to "新建命例",
        "search_hint" to "搜索名字、日期、四柱…",
        "no_match" to "没有匹配的档案",
        "match" to "匹配",
        "total_archives" to "共 %1 条档案",
        "no_archives" to "还没有档案",
        "empty_life" to "创建第一条命例，开始排盘与批注",
        "empty_divination" to "选一种起卦方式，记录第一次占问",
        "confirm_delete" to "确认删除",
        "delete_ask" to "确定删除「%1」吗？此操作不可恢复。",
        "sort" to "排序",
        "sort_created" to "创建",
        "sort_name" to "名字",
        "sort_birth" to "生日",
        "about_tagline" to "八字、紫微、七政与四路卜卦，离线排盘与批注工具。",
        "about_features" to "功能",
        "about_feature_life" to "命例：八字、紫微、七政排盘，支持批注与图片。",
        "about_feature_divination" to "卜卦：六爻、小六壬、梅花易数、奇门遁甲，保存卦例并复盘。",
        "about_feature_books" to "书架：古籍原文阅读。",
        "about_feature_backup" to "备份：本地存储，支持导出、导入与链接分享。",
        "about_project" to "项目",
        "about_repo" to "代码仓库",
        "about_platforms" to "支持 Android、iOS、桌面与网页。",
        "about_privacy" to "隐私",
        "about_privacy_text" to "档案与批注保存在本机。分享链接由临时文件服务生成，链接有效期由服务方决定。",
        "about_disclaimer" to "免责声明",
        "about_disclaimer_text" to "排盘结果仅供传统文化研究与娱乐参考，不构成任何决策依据。",
    )

    private val en = mapOf(
        "back" to "Back",
        "back_short" to "‹ Back",
        "save" to "Save",
        "saved" to "Saved",
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
        "continue_reading" to "Continue reading",
        "chapter_short" to "Ch. %1",
        "contents" to "Contents",
        "settings" to "Settings",
        "password" to "Password",
        "export_failed" to "Export failed",
        "import_success" to "Import successful",
        "bazi" to "Bazi",
        "ziwei" to "Zi Wei",
        "qizheng" to "Qizheng",
        "case" to "Case",
        "edit" to "Edit",
        "note" to "Note",
        "note_section" to "Notes",
        "save_note" to "Save note",
        "question" to "Question",
        "case_name" to "Name",
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
        "life_case" to "Natality",
        "divination" to "Divination",
        "new_life_case" to "New Natality",
        "search_hint" to "Search name, date, pillars…",
        "no_match" to "No matching archives",
        "match" to "Matched",
        "total_archives" to "%1 archives",
        "no_archives" to "No archives yet",
        "empty_life" to "Create your first natality chart and start annotating",
        "empty_divination" to "Pick a casting method to record your first divination",
        "confirm_delete" to "Delete archive",
        "delete_ask" to "Delete \"%1\"? This cannot be undone.",
        "sort" to "Sort",
        "sort_created" to "Created",
        "sort_name" to "Name",
        "sort_birth" to "Birth",
        "about_tagline" to "Bazi, Zi Wei, Qizheng and four divination methods — offline charts with notes.",
        "about_features" to "Features",
        "about_feature_life" to "Natality: Bazi, Zi Wei and Qizheng charts with notes and images.",
        "about_feature_divination" to "Divination: Liuyao, Xiao Liuren, Mei Hua and Qimen, with saved cases.",
        "about_feature_books" to "Bookshelf: classical texts reader.",
        "about_feature_backup" to "Backup: local storage with export, import and link sharing.",
        "about_project" to "Project",
        "about_repo" to "Repository",
        "about_platforms" to "Available on Android, iOS, desktop and web.",
        "about_privacy" to "Privacy",
        "about_privacy_text" to "Archives and notes stay on this device. Share links use a temporary file service; expiry is set by the provider.",
        "about_disclaimer" to "Disclaimer",
        "about_disclaimer_text" to "Results are for cultural study and entertainment only, not decision-making advice.",
    )

    fun t(key: String): String =
        (if (languageState.value == Language.ZH) zh else en)[key] ?: key

    val back: String @Composable get() = t("back")
    val backShort: String @Composable get() = t("back_short")
    val save: String @Composable get() = t("save")
    val saved: String @Composable get() = t("saved")
    val lifeCase: String @Composable get() = t("life_case")
    val divination: String @Composable get() = t("divination")
    val newLifeCase: String @Composable get() = t("new_life_case")
    val searchHint: String @Composable get() = t("search_hint")
    val noMatch: String @Composable get() = t("no_match")
    val match: String @Composable get() = t("match")
    fun totalArchives(count: Int): String = t("total_archives").replace("%1", count.toString())
    val noArchives: String @Composable get() = t("no_archives")
    val emptyLife: String @Composable get() = t("empty_life")
    val emptyDivination: String @Composable get() = t("empty_divination")
    val confirmDelete: String @Composable get() = t("confirm_delete")
    fun deleteAsk(name: String): String = t("delete_ask").replace("%1", name)
    val sort: String @Composable get() = t("sort")
    val aboutTagline: String @Composable get() = t("about_tagline")
    val aboutFeatures: String @Composable get() = t("about_features")
    val aboutFeatureLife: String @Composable get() = t("about_feature_life")
    val aboutFeatureDivination: String @Composable get() = t("about_feature_divination")
    val aboutFeatureBooks: String @Composable get() = t("about_feature_books")
    val aboutFeatureBackup: String @Composable get() = t("about_feature_backup")
    val aboutProject: String @Composable get() = t("about_project")
    val aboutRepo: String @Composable get() = t("about_repo")
    val aboutPlatforms: String @Composable get() = t("about_platforms")
    val aboutPrivacy: String @Composable get() = t("about_privacy")
    val aboutPrivacyText: String @Composable get() = t("about_privacy_text")
    val aboutDisclaimer: String @Composable get() = t("about_disclaimer")
    val aboutDisclaimerText: String @Composable get() = t("about_disclaimer_text")
    val continueReading: String @Composable get() = t("continue_reading")
    fun chapterShort(n: Int): String = t("chapter_short").replace("%1", n.toString())
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
    val edit: String @Composable get() = t("edit")
    val note: String @Composable get() = t("note")
    val noteSection: String @Composable get() = t("note_section")
    val saveNote: String @Composable get() = t("save_note")
    val question: String @Composable get() = t("question")
    val caseName: String @Composable get() = t("case_name")
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
