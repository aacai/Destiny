package zhiqiu.qizheng

/**
 * 十干化曜 / 天官化曜（《果老星宗·诸星起例》，已与参考盘 丙午 丙申 乙亥 癸未 例逐项核对）。
 *
 * 1. 变曜（十干配星）：「甲火乙孛丙属木，丁是金星戊上求，己人太阴庚是水，辛气壬计癸罗睺」；
 *    十化曜顺次「禄暗福耗荫，贵刑印囚权」——年干 g 的第 i 化曜落在 [GanToStar][(g+i)%10]。
 *    太阳为诸星之君，不化曜。
 * 2. 十神（天官派口径）：禄=比肩 暗=劫财 福=食神 耗=伤官 荫=偏财 贵=正财 刑=七杀 印=正官 囚=枭印 权=印绶。
 * 3. 其余起例皆从年干（爵星/天马地驿从年支、值难从月、科甲从命宫对宫、天经地纬从五虎遁）。
 */
enum class HuaYaoSchool(val label: String) {
    GUOLAO("果老化曜"),
    TIANGUAN("天官化曜"),
}

/** 一颗星曜的化曜栏：[star] 为星曜 key，[labels] 为该星所化的诸名（去重保序） */
data class HuaYaoColumn(
    val star: String,
    val labels: List<String>,
)

object HuaYaoTable {

    /** 十一曜固定栏序（对照参考盘：日月水金火木土计罗孛炁） */
    val Stars = listOf("日", "月", "水", "金", "火", "木", "土", "计", "罗", "孛", "炁")

    private val Gans = "甲乙丙丁戊己庚辛壬癸".toList()
    private val Branches = "子丑寅卯辰巳午未申酉戌亥".toList()

    /** 十干化禄星（变曜起点），太阳不与 */
    private val GanToStar = listOf("火", "孛", "木", "金", "土", "月", "水", "炁", "计", "罗")

    /** 十化曜名（顺次：禄暗福耗荫贵刑印囚权） */
    private val HuaNames = listOf("天禄", "天暗", "天福", "天耗", "天荫", "天贵", "天刑", "天印", "天囚", "天权")

    /** 十化曜对应的十神（天官派口径） */
    private val ShiShen = listOf("比肩", "劫财", "食神", "伤官", "偏财", "正财", "七杀", "正官", "枭印", "印绶")

    /** 五行相克：克 [w] 者 */
    private val KeOf = mapOf("木" to "金", "火" to "水", "土" to "木", "金" to "火", "水" to "土")

    /** 干 → 五行 */
    private val GanElement = listOf("木", "木", "火", "火", "土", "土", "金", "金", "水", "水")

    /** 支 → 支神五行（寅卯木 巳午火 申酉金 亥子水 辰戌丑未土；不取寅亥合木论） */
    private val BranchElement = mapOf(
        2 to "木", 3 to "木", 5 to "火", 6 to "火",
        8 to "金", 9 to "金", 11 to "水", 0 to "水",
        4 to "土", 10 to "土", 1 to "土", 7 to "土",
    )

    /** 十二宫主宰（宫主星）：子丑土 寅亥木 卯戌火 辰酉金 巳申水 午日 未月 */
    private val PalaceLord = listOf("土", "土", "木", "火", "金", "水", "日", "月", "水", "金", "火", "木")

    /** 干禄宫：甲寅 乙卯 丙巳 丁午 戊巳 己午 庚申 辛酉 壬亥 癸子 */
    private val LuGong = listOf(2, 3, 5, 6, 5, 6, 8, 9, 11, 0)

    /** 卦气起点宫：壬甲从乾(亥) 乙癸坤(申) 戊坎(子) 丙艮(寅) 庚震(卯) 辛巽(辰) 己离(午) 丁兑(酉) */
    private val GuaQiStart = listOf(11, 8, 2, 9, 0, 6, 3, 4, 11, 8)

    /** 五虎遁年起月干：甲己起丙 乙庚起戊 丙辛起庚 丁壬起壬 戊癸起甲（皆自寅起） */
    private val WuHuDun = listOf(2, 4, 6, 8, 0, 2, 4, 6, 8, 0)

    private fun ganIdx(c: String) = Gans.indexOfFirst { it.toString() == c }
    private fun branchIdx(c: String) = Branches.indexOfFirst { it.toString() == c }

    /** 五虎遁至 [branchIdx0] 宫所得天干索引（寅=起点） */
    private fun dunGan(yearGan: Int, branchIdx0: Int): Int =
        (WuHuDun[yearGan] + (branchIdx0 - 2 + 12)) % 10

    /**
     * 化曜全表。
     *
     * [yearStem]/[yearBranch] 年柱干支；[lunarMonth] 农历月（1..12，用于值难，未知传 null）；
     * [mingIdx] 命宫支索引；[officialPalaceIdx] 官禄宫支索引（命宫逆布第 10 宫）；
     * [yearNayinElement] 年柱纳音五行（金木水火土）。
     *
     * 返回：星曜 key → 化名列表（果老口径含「天X」；十神名单列于 [tenGodOf]）。
     */
    fun build(
        yearStem: String,
        yearBranch: String,
        lunarMonth: Int?,
        mingIdx: Int,
        officialPalaceIdx: Int,
        yearNayinElement: String,
    ): Map<String, List<String>> {
        val g = ganIdx(yearStem)
        val yb = branchIdx(yearBranch)
        val byStar = LinkedHashMap<String, MutableList<String>>()
        fun add(star: String, label: String) {
            if (star in Stars) byStar.getOrPut(star) { mutableListOf() }.add(label)
        }

        // 1) 十化曜 + 十神：化曜 i 的星 = GanToStar[(g+i)%10]
        val shiShen = mutableMapOf<String, String>()
        for (i in 0 until 10) {
            val star = GanToStar[(g + i) % 10]
            add(star, HuaNames[i])
            shiShen[star] = ShiShen[i]
        }

        // 2) 诸星起例（从年干）
        // 催官：甲金乙水丙日丁罗戊木己气庚孛辛土壬月癸计
        add(listOf("金", "水", "日", "罗", "木", "炁", "孛", "土", "月", "计")[g], "催官")
        // 禄神：甲兼木孛 乙水 丙计 丁罗 戊土 己火 庚金 辛气 壬日 癸月
        listOf(listOf("木", "孛"), listOf("水"), listOf("计"), listOf("罗"), listOf("土"), listOf("火"), listOf("金"), listOf("炁"), listOf("日"), listOf("月"))[g]
            .forEach { add(it, "禄神") }
        // 喜神：甲罗乙计丙气丁水戊月己土庚金辛木壬孛癸火
        add(listOf("罗", "计", "炁", "水", "月", "土", "金", "木", "孛", "火")[g], "喜神")
        // 魁星：甲月乙日丙罗丁计戊火己金庚木辛孛壬气癸水（传本「庚木癸水」与「庚水癸木」互异，取原文）
        add(listOf("月", "日", "罗", "计", "火", "金", "木", "孛", "炁", "水")[g], "魁星")
        // 官星（天官）：甲气乙水丙罗丁计戊孛己火庚金辛木壬月癸土
        add(listOf("炁", "水", "罗", "计", "孛", "火", "金", "木", "月", "土")[g], "官星")
        // 印星：甲木乙日丙火丁月戊土己罗庚金辛计壬水癸孛
        add(listOf("木", "日", "火", "月", "土", "罗", "金", "计", "水", "孛")[g], "印星")
        // 文星：甲罗乙计丙戊金丁火己气庚木辛土壬日癸月
        add(listOf("罗", "计", "金", "火", "金", "炁", "木", "土", "日", "月")[g], "文星")
        // 科名：甲乙木 丙丁火 戊己土 庚辛金 壬癸水
        add(GanElement[g], "科名")
        // 仁元：年干五行
        add(GanElement[g], "仁元")
        // 寿元：年纳音五行
        add(yearNayinElement, "寿元")
        // 生官：甲月乙土丙气丁水戊罗己计庚孛辛火壬金癸木
        add(listOf("月", "土", "炁", "水", "罗", "计", "孛", "火", "金", "木")[g], "生官")

        // 3) 从年支
        // 爵星：子申土 亥未火 午丑水 卯气 寅巳木 酉戌金 辰孛
        add(
            when (yb) {
                0, 8 -> "土"
                11, 7 -> "火"
                6, 1 -> "水"
                3 -> "炁"
                2, 5 -> "木"
                9, 10 -> "金"
                else -> "孛"
            },
            "爵星",
        )
        // 天马/地驿：申子辰→火/木 寅午戌→水/金 亥卯未→木/火 巳酉丑→计/水
        val (tianMa, diYi) = when (yb) {
            8, 0, 4 -> "火" to "木"
            2, 6, 10 -> "水" to "金"
            11, 3, 7 -> "木" to "火"
            else -> "计" to "水"
        }
        add(tianMa, "天马")
        add(diYi, "地驿")
        // 禄元：年干禄宫之宫主
        add(PalaceLord[LuGong[g]], "禄元")
        // 马元：年支驿马宫之宫主
        val maGong = when (yb) {
            8, 0, 4 -> 2
            2, 6, 10 -> 8
            5, 9, 1 -> 11
            else -> 5
        }
        add(PalaceLord[maGong], "马元")

        // 4) 从月：值难（正二日 三四月 五六火罗 七八水孛 九十木气 十一十二金）
        if (lunarMonth != null && lunarMonth in 1..12) {
            val nan = when (lunarMonth) {
                1, 2 -> listOf("日")
                3, 4 -> listOf("月")
                5, 6 -> listOf("火", "罗")
                7, 8 -> listOf("水", "孛")
                9, 10 -> listOf("木", "炁")
                else -> listOf("金")
            }
            nan.forEach { add(it, "值难") }
        }

        // 5) 从命宫
        // 科甲：命宫对宫之宫主
        add(PalaceLord[(mingIdx + 6) % 12], "科甲")
        // 天经：五虎遁至命宫干之五行；地纬：命宫支神五行
        add(GanElement[dunGan(g, mingIdx)], "天经")
        add(BranchElement[mingIdx]!!, "地纬")
        // 天元禄：五虎遁至命宫干之化禄
        add(GanToStar[dunGan(g, mingIdx)], "天元")
        // 人元禄：五虎遁至官禄宫，取克其干五行者
        add(KeOf[GanElement[dunGan(g, officialPalaceIdx)]]!!, "人元")
        // 职元：卦气从年干起，顺数至命宫得干，取其化禄
        val zhiGan = (g + (mingIdx - GuaQiStart[g] + 12)) % 10
        add(GanToStar[zhiGan], "职元")
        // 局主：职元干所合之干（甲己 乙庚 丙辛 丁壬 戊癸）化禄
        val juGan = (zhiGan + 5) % 10
        add(GanToStar[juGan], "局主")

        return byStar
    }

    /**
     * 带十神索引的构建：返回 化名表 + 每星十神。
     * UI 果老模式显示 [columns] 全部化名；天官模式把「天X」替换为其十神。
     */
    fun buildColumns(
        yearStem: String,
        yearBranch: String,
        lunarMonth: Int?,
        mingIdx: Int,
        officialPalaceIdx: Int,
        yearNayinElement: String,
        presentStars: Set<String>,
        school: HuaYaoSchool,
    ): List<HuaYaoColumn> {
        val byStar = build(yearStem, yearBranch, lunarMonth, mingIdx, officialPalaceIdx, yearNayinElement)
        val g = ganIdx(yearStem)
        val tenGod = buildMap {
            for (i in 0 until 10) put(GanToStar[(g + i) % 10], ShiShen[i])
        }
        return Stars.filter { it in presentStars }.map { star ->
            val labels = byStar[star].orEmpty()
            val shown = when (school) {
                HuaYaoSchool.GUOLAO -> labels
                HuaYaoSchool.TIANGUAN ->
                    // 「天X」换为其十神名，其余起例名保留
                    labels.map { l -> if (l in HuaNames) tenGod[star] ?: l else l }.distinct()
            }
            HuaYaoColumn(star, shown)
        }
    }
}
