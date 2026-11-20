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
    /** 单段上限：超过则切开，避免 Compose Text 整本一段渲不动（表现为空白） */
    private const val MAX_PARA_CHARS = 480

    /** 书目与 composeResources/files/books 下 ASCII .txt 一一对应（打进各端包） */
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

    /**
     * 加载并解析古籍。资源缺失或解析失败返回 null（不再抛到 UI 协程里卡住「加载中」）。
     */
    suspend fun loadBook(id: String): Book? {
        cache[id]?.let { return it }
        val meta = catalog.find { it.id == id } ?: return null
        return runCatching {
            val txt = Res.readBytes("$RESOURCE_DIR$id.txt").decodeToString()
            parse(meta, txt).also { cache[id] = it }
        }.getOrNull()
    }

    /** 古籍 txt 解析：=/#/卷/○ 标题 + 空行分段；超长段切块，保证可渲染 */
    fun parse(meta: BookMeta, txt: String): Book {
        val lines = txt.lines()
        val sections = mutableListOf<BookSection>()
        var cur = BookSection(meta.title, mutableListOf())
        val paragraph = StringBuilder()

        fun emitPara(text: String) {
            val t = text.trim()
            if (t.isEmpty()) return
            val paras = cur.paragraphs as MutableList
            if (t.length <= MAX_PARA_CHARS) {
                paras.add(t)
                return
            }
            // 先按换行拆；仍过长再按句读切
            t.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.forEach { chunk ->
                if (chunk.length <= MAX_PARA_CHARS) {
                    paras.add(chunk)
                } else {
                    paras.addAll(splitBySentence(chunk, MAX_PARA_CHARS))
                }
            }
        }

        fun flushPara() {
            emitPara(paragraph.toString())
            paragraph.clear()
        }

        fun newSection(t: String) {
            flushPara()
            if (cur.paragraphs.isNotEmpty() || cur.title.isNotEmpty()) sections.add(cur)
            cur = BookSection(t, mutableListOf())
        }

        lines.forEachIndexed { i, raw ->
            val line = raw.trim()
                .removePrefix("\uFEFF")
                .let { if (it.startsWith(":\u3000\u3000")) it.removePrefix(":\u3000\u3000").trim() else it }
                .let { if (it.startsWith("：\u3000\u3000")) it.removePrefix("：\u3000\u3000").trim() else it }
            when {
                line.isEmpty() || line == "__TOC__" -> flushPara()
                line.startsWith("*[[") -> {}
                // wiki 卷目「第一卷論…」连写跳过；裸「卷一」「第一卷」作标题
                volumeTocLine.matches(line) -> {}
                isMarkdownOrEqHeading(line) -> {
                    val t = stripHeadingMarks(line)
                    if (t.isNotEmpty()) newSection(t)
                }
                isVolumeOrCircleHeading(line) -> newSection(line.trimStart('○', '●', '【', '】').trim())
                isHeadingLine(line) && isIsolatedHeading(lines, i) -> newSection(line)
                else -> {
                    // 行与行之间留换行，便于超长缓冲按行切开；中文本身不需要空格
                    if (paragraph.isNotEmpty()) paragraph.append('\n')
                    paragraph.append(line)
                    // 无空行的书：积到上限就按句读提前落段，避免整卷并成一块
                    if (paragraph.length >= MAX_PARA_CHARS) {
                        val s = paragraph.toString()
                        val cut = lastSentenceBreak(s, MAX_PARA_CHARS)
                        if (cut > 0) {
                            emitPara(s.substring(0, cut))
                            paragraph.clear()
                            paragraph.append(s.substring(cut).trimStart())
                        }
                    }
                }
            }
        }
        flushPara()
        if (cur.paragraphs.isNotEmpty() || cur.title.isNotEmpty()) sections.add(cur)

        val cleaned = sections.filter { it.paragraphs.isNotEmpty() || it.title.isNotEmpty() }
        return Book(
            meta,
            meta.title,
            cleaned.ifEmpty { listOf(BookSection(meta.title, listOf("（正文为空或未能解析）"))) },
        )
    }

    private fun isMarkdownOrEqHeading(line: String): Boolean {
        if (line.startsWith("=") && line.endsWith("=") && line.trim('=') != line) return true
        if (line.startsWith("#")) return true
        // 网页壳标题行：第003章 《子平真诠》… | 古籍典藏
        if (line.contains("| 古籍") || (line.startsWith("第") && line.contains("章") && line.contains("《"))) return true
        // 纯等号分隔线
        if (line.length >= 8 && line.all { it == '=' || it == '-' }) return true
        return false
    }

    private fun stripHeadingMarks(line: String): String {
        var t = line.trim()
        if (t.all { it == '=' || it == '-' }) return ""
        t = t.trim('=').trim()
        while (t.startsWith("#")) t = t.removePrefix("#").trim()
        t = t.replace(Regex("\\s+"), " ")
        // 去掉网页壳尾巴
        t = t.substringBefore("|").trim()
        return t
    }

    /** 卷一 / ○论五行 / 【渊海子平】 */
    private fun isVolumeOrCircleHeading(line: String): Boolean {
        if (line.length > 24) return false
        if (bareVolume.matches(line)) return true
        if (line.startsWith("○") || line.startsWith("●")) return true
        if (line.startsWith("【") && line.endsWith("】") && line.length <= 16) return true
        return false
    }

    /** 短行标题判定：≤14 字、无句读、非括号补注、非干支柱图、非命例时间串 */
    private fun isHeadingLine(line: String): Boolean {
        if (line.length > 14) return false
        if (line.startsWith("（") || line.startsWith("(")) return false
        if (line.any { it in HEADING_PUNCT }) return false
        if (line.all { it in GANZHI_CHARS || it.isWhitespace() }) return false
        if (line.endsWith("时") || (line.contains("日") && line.contains("时"))) return false
        return true
    }

    /** 独立成行：上一行为空，且下一行不像紧挨短标题/柱图 */
    private fun isIsolatedHeading(lines: List<String>, i: Int): Boolean {
        val prevBlank = i == 0 || lines[i - 1].isBlank()
        if (!prevBlank) return false
        val next = if (i + 1 < lines.size) lines[i + 1].trim() else ""
        return next.isEmpty() || next.length > 14 || next.any { it in HEADING_PUNCT }
    }

    private fun splitBySentence(text: String, max: Int): List<String> {
        if (text.length <= max) return listOf(text)
        val out = mutableListOf<String>()
        var rest = text
        while (rest.length > max) {
            val cut = lastSentenceBreak(rest, max).takeIf { it > 0 } ?: max
            out += rest.substring(0, cut).trim()
            rest = rest.substring(cut).trimStart()
        }
        if (rest.isNotEmpty()) out += rest
        return out
    }

    /** 在 limit 附近找最近句读；找不到返回 0 */
    private fun lastSentenceBreak(text: String, limit: Int): Int {
        val end = minOf(limit, text.length)
        val window = text.substring(0, end)
        for (i in window.indices.reversed()) {
            if (window[i] in "。！？；\n") return i + 1
        }
        return 0
    }

    private const val GANZHI_CHARS = "甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥"
    private const val HEADING_PUNCT = "。！？；，、：…·—“”‘’《》"

    /** wiki 卷目行：第X卷+卷题（无空格相连） */
    private val volumeTocLine = Regex("^第[一二三四五六七八九十○0-9]+卷\\S.+$")
    /** 裸卷标：卷一 / 第一卷 */
    private val bareVolume = Regex("^第?[一二三四五六七八九十百○0-9]+卷$")
}
