package zhiqiu.app.destiny.ui.books

import destiny.shared.generated.resources.Res

/** 书目元信息（书架展示用） */
data class BookMeta(
    val id: String,
    val title: String,
    val category: String,
    /** 封面渐变色（番茄式彩色封面） */
    val coverFrom: Long,
    val coverTo: Long,
)

data class Book(
    val meta: BookMeta,
    val title: String,
    val sections: List<BookSection>,
)

/** 章节：标题 + 若干段落 */
data class BookSection(
    val title: String,
    val paragraphs: List<String>,
)

object BookStore {
    private const val RESOURCE_DIR = "files/books/"

    /** 书目与 corpus/books 下古籍一一对应（拷贝为 ASCII 文件名打包进资源） */
    val catalog: List<BookMeta> = listOf(
        BookMeta("sanmingtonghui", "三命通会", "命理", 0xFF2E6FB7, 0xFF1B3E68),
        BookMeta("yuanhaiziping", "渊海子平", "命理", 0xFFE2574C, 0xFF9A2A21),
        BookMeta("qiongtongbaojian", "穷通宝鉴", "命理", 0xFFD68910, 0xFF7E5109),
        BookMeta("shenfengtongkao", "神峰通考", "命理", 0xFF16A085, 0xFF0B5345),
        BookMeta("wuxingjingji", "五行精纪", "命理", 0xFF8E44AD, 0xFF4A235A),
        BookMeta("lixuzhongmingshu", "李虚中命书", "命理", 0xFF2980B9, 0xFF154360),
        BookMeta("bazitiyao", "八字提要", "命理", 0xFFD35400, 0xFF7E3300),
        BookMeta("qianliminggao", "千里命稿", "命理", 0xFF1ABC9C, 0xFF0E6251),
        BookMeta("zipingzhenquan", "子平真诠", "命理", 0xFF2C3E50, 0xFF1B2631),
        BookMeta("ditiunsui", "滴天髓", "命理", 0xFF9B59B6, 0xFF6C3483),
        BookMeta("ditiunsuichanwei", "滴天髓阐微", "命理", 0xFFE67E22, 0xFF9C5405),
    )

    private val cache = mutableMapOf<String, Book>()

    suspend fun loadBook(id: String): Book? {
        cache[id]?.let { return it }
        val meta = catalog.find { it.id == id } ?: return null
        val txt = Res.readBytes("$RESOURCE_DIR$id.txt").decodeToString()
        return parse(meta, txt).also { cache[id] = it }
    }

    /** 古籍 txt 解析：= 标题 / 短行章节 / 空行分段 */
    fun parse(meta: BookMeta, txt: String): Book {
        val lines = txt.lines()
        val sections = mutableListOf<BookSection>()
        var cur = BookSection("", mutableListOf<String>())
        val paragraph = StringBuilder()

        fun flushPara() {
            val t = paragraph.toString().trim()
            if (t.isNotEmpty()) (cur.paragraphs as MutableList).add(t)
            paragraph.clear()
        }

        fun newSection(t: String) {
            flushPara()
            if (cur.paragraphs.isNotEmpty() || cur.title.isNotEmpty()) sections.add(cur)
            cur = BookSection(t, mutableListOf())
        }

        lines.forEachIndexed { i, raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> flushPara()
                // wiki 导航行 *[[/卷一|卷一]] 直接跳过
                line.startsWith("*[[") -> {}
                // wiki 卷目行「第一卷論六十甲子上」（正文卷标记是裸「第一卷」）跳过
                volumeTocLine.matches(line) -> {}
                // = 标题（= 五行总论 = / ==论月令== / === 三春甲木 ===）
                line.startsWith("=") && line.endsWith("=") && line.trim('=') != line -> {
                    val t = line.trim('=').trim().replace(Regex("\\s+"), " ")
                    if (t.isNotEmpty()) newSection(t)
                }
                // 短行章节标题（叙 / 原序 / 五星正说类 等），需排除柱图与命例时间串
                isHeadingLine(line) && isIsolatedHeading(lines, i) -> newSection(line)
                else -> paragraph.append(line)
            }
        }
        flushPara()
        if (cur.paragraphs.isNotEmpty() || cur.title.isNotEmpty()) sections.add(cur)

        return Book(meta, meta.title, sections.ifEmpty { listOf(BookSection("", emptyList())) })
    }

    /** 短行标题判定：≤14 字、无句读、非括号补注、非干支柱图、非命例时间串 */
    private fun isHeadingLine(line: String): Boolean {
        if (line.length > 14) return false
        if (line.startsWith("（") || line.startsWith("(")) return false
        if (line.any { it in HEADING_PUNCT }) return false
        // 纯干支字符行：四柱柱图（庚丙庚丙 / 壬 旺 庚 甲 等）
        if (line.all { it in GANZHI_CHARS || it.isWhitespace() }) return false
        // 命例时间串：庚子日丙子时… / 己亥时 / 时日月年
        if (line.endsWith("时") || (line.contains("日") && line.contains("时"))) return false
        return true
    }

    /** 独立成行判定：上一行为空（段首）且下一行不是紧挨的短行（柱图连排） */
    private fun isIsolatedHeading(lines: List<String>, i: Int): Boolean {
        val prevBlank = i == 0 || lines[i - 1].isBlank()
        if (!prevBlank) return false
        val next = if (i + 1 < lines.size) lines[i + 1].trim() else ""
        return next.isEmpty() || next.length > 14 || next.any { it in HEADING_PUNCT }
    }

    private const val GANZHI_CHARS = "甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥"
    private const val HEADING_PUNCT = "。！？；，、：…·—“”‘’《》"

    /** wiki 卷目行：第X卷+卷题（无空格相连） */
    private val volumeTocLine = Regex("^第[一二三四五六七八九十○0-9]+卷[^ ].*$")
}
