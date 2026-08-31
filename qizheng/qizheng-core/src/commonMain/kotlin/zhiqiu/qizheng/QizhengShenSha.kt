package zhiqiu.qizheng

/**
 * 本命神煞落十二支。外圈神煞偏年支岁类，内圈神煞偏日干贵人类。
 */
object QizhengShenSha {
    private val Branches = MingGong.Branches

    data class Dual(
        /** 外侧神煞环（太岁、驿马、华盖、红鸾…） */
        val yearRing: List<List<String>>,
        /** 内侧神煞环（天乙、禄刃、文昌、天德…） */
        val dayRing: List<List<String>>,
    )

    fun dual(
        yearStem: String,
        yearBranch: String,
        dayStem: String,
        dayBranch: String,
        monthBranch: String,
        set: ShenShaSet = ShenShaSet.GUOLAO,
        /** 每支最多保留几个；0 = 不限 */
        maxPerBranch: Int = 5,
    ): Dual {
        if (set == ShenShaSet.NONE) {
            return Dual(List(12) { emptyList() }, List(12) { emptyList() })
        }
        val yb = idx(yearBranch)
        val yearOut = Array(12) { linkedSetOf<String>() }
        val dayOut = Array(12) { linkedSetOf<String>() }

        fun put(target: Array<LinkedHashSet<String>>, branch: String?, name: String) {
            val i = idx(branch ?: return)
            if (i >= 0) target[i] += name
        }

        if (yb >= 0) {
            put(yearOut, yearBranch, "太岁")
            put(yearOut, Branches[(yb + 6) % 12], "岁破")
            put(yearOut, Branches[(yb + 2) % 12], "丧门")
            put(yearOut, Branches[(yb + 8) % 12], "吊客")
            put(yearOut, Branches[(yb + 3) % 12], "官符")
            put(yearOut, Branches[(yb + 9) % 12], "病符")
            put(yearOut, Branches[(yb + 5) % 12], "死符")
            put(yearOut, Branches[(yb + 4) % 12], "小耗")
            put(yearOut, Branches[(yb + 10) % 12], "大耗")
            put(yearOut, yiMa(yb), "驿马")
            put(yearOut, huaGai(yb), "华盖")
            put(yearOut, jiangXing(yb), "将星")
            put(yearOut, taoHua(yb), "桃花")
            put(yearOut, taoHua(yb), "咸池")
            put(yearOut, jieSha(yb), "劫煞")
            put(yearOut, zaiSha(yb), "灾煞")
            put(yearOut, wangShen(yb), "亡神")
            val hong = hongLuan(yb)
            put(yearOut, hong, "红鸾")
            put(yearOut, Branches[(idx(hong) + 6) % 12], "天喜")
            put(yearOut, luShen(yearStem), "岁禄")
            put(yearOut, yangRen(yearStem), "岁刃")
            // 年支丛辰
            put(yearOut, guChen(yb), "孤辰")
            put(yearOut, guaSu(yb), "寡宿")
            put(yearOut, poSui(yb), "破碎")
            // 年干飞刃（羊刃对冲）
            put(yearOut, feiRen(yearStem), "飞刃")
        }

        tianYi(dayStem).forEach { put(dayOut, it, "天乙") }
        put(dayOut, wenChang(dayStem), "文昌")
        put(dayOut, luShen(dayStem), "禄神")
        put(dayOut, yangRen(dayStem), "羊刃")
        put(dayOut, jinYu(dayStem), "金舆")
        put(dayOut, tianDeBranch(monthBranch), "天德")
        put(dayOut, yueDeBranch(monthBranch), "月德")
        val db = idx(dayBranch)
        if (db >= 0) {
            put(dayOut, yiMa(db), "驿马")
            put(dayOut, huaGai(db), "华盖")
            put(dayOut, taoHua(db), "桃花")
        }
        // 日干贵人类
        put(dayOut, tianChu(dayStem), "天厨")
        put(dayOut, guoYin(dayStem), "国印")

        val cap: (List<String>) -> List<String> =
            { if (maxPerBranch > 0) it.take(maxPerBranch) else it }
        return Dual(
            yearRing = yearOut.map { cap(it.toList()) },
            dayRing = dayOut.map { cap(it.toList()) },
        )
    }

    /** 兼容旧调用：年环 ∪ 日环 */
    fun byBranch(
        yearStem: String,
        yearBranch: String,
        dayStem: String,
        dayBranch: String,
        monthBranch: String,
        set: ShenShaSet = ShenShaSet.GUOLAO,
        maxPerBranch: Int = 5,
    ): List<List<String>> {
        val d = dual(yearStem, yearBranch, dayStem, dayBranch, monthBranch, set, maxPerBranch)
        return d.yearRing.indices.map { i ->
            (d.yearRing[i] + d.dayRing[i]).distinct().take(6)
        }
    }

    /** 三会局前一支（孤辰）：寅午戌→巳，申子辰→亥，亥卯未→寅，巳酉丑→申 */
    private fun guChen(b: Int): String = when {
        b in setOf(2, 6, 10) -> "巳"
        b in setOf(8, 0, 4) -> "寅"
        b in setOf(11, 3, 7) -> "亥"
        b in setOf(5, 9, 1) -> "申"
        else -> Branches[b]
    }
    /** 三会局后一支（寡宿）：寅午戌→辰，申子辰→丑，亥卯未→未，巳酉丑→戌 */
    private fun guaSu(b: Int): String = when {
        b in setOf(2, 6, 10) -> "辰"
        b in setOf(8, 0, 4) -> "丑"
        b in setOf(11, 3, 7) -> "未"
        b in setOf(5, 9, 1) -> "戌"
        else -> Branches[b]
    }
    /** 三合局死位（破碎/白衣）：寅午戌→酉，申子辰→巳，亥卯未→申，巳酉丑→寅 */
    private fun poSui(b: Int): String = when {
        b in setOf(2, 6, 10) -> "酉"
        b in setOf(8, 0, 4) -> "巳"
        b in setOf(11, 3, 7) -> "申"
        b in setOf(5, 9, 1) -> "寅"
        else -> Branches[b]
    }
    /** 年干飞刃：羊刃所落地支的对冲 */
    private fun feiRen(stem: String): String {
        val r = yangRen(stem)
        val i = idx(r)
        return if (i >= 0) Branches[(i + 6) % 12] else ""
    }
    /** 日干天厨（食神禄地）：甲巳乙午丙申丁酉戊申己酉庚亥辛子壬寅癸卯 */
    private fun tianChu(stem: String): String = when (stem) {
        "甲" -> "巳"; "乙" -> "午"; "丙" -> "申"; "丁" -> "酉"
        "戊" -> "申"; "己" -> "酉"; "庚" -> "亥"; "辛" -> "子"
        "壬" -> "寅"; "癸" -> "卯"; else -> ""
    }
    /** 日干国印：甲戌乙亥丙丑丁寅戊丑己寅庚辰辛巳壬未癸申 */
    private fun guoYin(stem: String): String = when (stem) {
        "甲" -> "戌"; "乙" -> "亥"; "丙" -> "丑"; "丁" -> "寅"
        "戊" -> "丑"; "己" -> "寅"; "庚" -> "辰"; "辛" -> "巳"
        "壬" -> "未"; "癸" -> "申"; else -> ""
    }

    private fun idx(b: String): Int = Branches.indexOf(b)

    private fun yiMa(b: Int): String = when (b) {
        2, 6, 10 -> "申"
        11, 3, 7 -> "巳"
        5, 9, 1 -> "亥"
        8, 0, 4 -> "寅"
        else -> Branches[b]
    }

    private fun huaGai(b: Int): String = when (b) {
        2, 6, 10 -> "戌"
        11, 3, 7 -> "未"
        8, 0, 4 -> "辰"
        5, 9, 1 -> "丑"
        else -> Branches[b]
    }

    private fun jiangXing(b: Int): String = when (b) {
        2, 6, 10 -> "午"
        11, 3, 7 -> "卯"
        8, 0, 4 -> "子"
        5, 9, 1 -> "酉"
        else -> Branches[b]
    }

    private fun taoHua(b: Int): String = when (b) {
        2, 6, 10 -> "卯"
        11, 3, 7 -> "子"
        8, 0, 4 -> "酉"
        5, 9, 1 -> "午"
        else -> Branches[b]
    }

    private fun jieSha(b: Int): String = when (b) {
        2, 6, 10 -> "亥"
        11, 3, 7 -> "申"
        8, 0, 4 -> "巳"
        5, 9, 1 -> "寅"
        else -> Branches[b]
    }

    private fun zaiSha(b: Int): String = when (b) {
        2, 6, 10 -> "子"
        11, 3, 7 -> "酉"
        8, 0, 4 -> "午"
        5, 9, 1 -> "卯"
        else -> Branches[b]
    }

    private fun wangShen(b: Int): String = when (b) {
        2, 6, 10 -> "巳"
        11, 3, 7 -> "寅"
        8, 0, 4 -> "亥"
        5, 9, 1 -> "申"
        else -> Branches[b]
    }

    private fun hongLuan(b: Int): String = Branches[(3 - b + 12) % 12]

    private fun tianYi(stem: String): List<String> = when (stem) {
        "甲", "戊", "庚" -> listOf("丑", "未")
        "乙", "己" -> listOf("子", "申")
        "丙", "丁" -> listOf("亥", "酉")
        "壬", "癸" -> listOf("巳", "卯")
        "辛" -> listOf("午", "寅")
        else -> emptyList()
    }

    private fun wenChang(stem: String): String = when (stem) {
        "甲" -> "巳"; "乙" -> "午"; "丙" -> "申"; "丁" -> "酉"
        "戊" -> "申"; "己" -> "酉"; "庚" -> "亥"; "辛" -> "子"
        "壬" -> "寅"; "癸" -> "卯"; else -> ""
    }

    private fun luShen(stem: String): String = when (stem) {
        "甲" -> "寅"; "乙" -> "卯"; "丙", "戊" -> "巳"; "丁", "己" -> "午"
        "庚" -> "申"; "辛" -> "酉"; "壬" -> "亥"; "癸" -> "子"; else -> ""
    }

    private fun yangRen(stem: String): String = when (stem) {
        "甲" -> "卯"; "乙" -> "寅"; "丙", "戊" -> "午"; "丁", "己" -> "巳"
        "庚" -> "酉"; "辛" -> "申"; "壬" -> "子"; "癸" -> "亥"; else -> ""
    }

    private fun jinYu(stem: String): String = when (stem) {
        "甲" -> "辰"; "乙" -> "巳"; "丙" -> "未"; "丁" -> "申"
        "戊" -> "未"; "己" -> "申"; "庚" -> "戌"; "辛" -> "亥"
        "壬" -> "丑"; "癸" -> "寅"; else -> ""
    }

    private fun yueDeBranch(monthBranch: String): String? = when (monthBranch) {
        "寅", "午", "戌" -> "午"
        "申", "子", "辰" -> "子"
        "亥", "卯", "未" -> "卯"
        "巳", "酉", "丑" -> "酉"
        else -> null
    }

    private fun tianDeBranch(monthBranch: String): String? = when (monthBranch) {
        "卯" -> "申"; "午" -> "亥"; "酉" -> "寅"; "子" -> "巳"
        "寅" -> "未"; "巳" -> "戌"; "申" -> "丑"; "亥" -> "辰"
        else -> null
    }
}
