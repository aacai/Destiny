package zhiqiu.qizheng

/**
 * 政余星格（《果老星宗》口径，对照星命排盘 ZYJS.`xingge`）。
 * 只返回命中的喜格 / 忌格；盘面不画格名，由「星格」页展示。
 */
object XingGe {
    private val Branches = MingGong.Branches
    private val LordOfBranch = mapOf(
        "子" to "土", "丑" to "土", "寅" to "木", "卯" to "火",
        "辰" to "金", "巳" to "水", "午" to "日", "未" to "月",
        "申" to "水", "酉" to "金", "戌" to "火", "亥" to "木",
    )
    private val PalaceOrder = listOf(
        "命宫", "相貌", "福德", "官禄", "迁移", "疾厄",
        "夫妻", "奴仆", "男女", "田宅", "兄弟", "财帛",
    )
    private val XiShen = setOf(
        "紫微", "禄勋", "驿马", "岁驾", "岁殿", "斗杓", "唐符", "国印",
        "天贵", "玉贵", "卦气", "长生", "帝旺",
    )
    private val JiShen = setOf(
        "阳刃", "羊刃", "天雄", "的杀", "劫杀", "劫煞", "飞廉", "剑锋",
        "大耗", "天耗", "地耗", "死符", "病符", "孤辰", "寡宿", "三刑", "六害", "天狗",
    )
    private val GoodPalaces = setOf("命宫", "财帛", "田宅", "官禄", "福德", "夫妻", "男女")
    private val BadPalaces = setOf("奴仆", "疾厄", "迁移", "兄弟", "相貌")
    private val EmptyStar = StarView("", "", 0.0, 0.0, "", "", 0.0, false)

    fun evaluate(
        stars: List<StarView>,
        palaces: List<PalaceView>,
        monthBranch: String,
        lunarDayName: String,
        hour: Int,
        mingBranch: String,
        shenBranch: String?,
        shenShaByBranch: List<List<String>>,
        mingXiu: String = "",
        duLordKey: String = "",
    ): List<PatternView> {
        val byKey = stars.associateBy { it.key }
        fun g(key: String): StarView = byKey[key] ?: EmptyStar.copy(key = key, label = key)
        fun alone(branch: String): Boolean =
            branch.isNotEmpty() && stars.count { it.branch == branch } == 1

        val sun = g("日"); val moon = g("月"); val mer = g("水"); val ven = g("金")
        val mar = g("火"); val jup = g("木"); val sat = g("土")
        val ket = g("计"); val rah = g("罗"); val yue = g("孛"); val zi = g("炁")

        val season = when (monthBranch) {
            in setOf("寅", "卯", "辰") -> "春"
            in setOf("巳", "午", "未") -> "夏"
            in setOf("申", "酉", "戌") -> "秋"
            else -> "冬"
        }
        val zhouYe = if (hour in 6..17) "昼" else "夜"
        val phase = when {
            lunarDayName in setOf(
                "十一", "十二", "十三", "十四", "十五",
                "十六", "十七", "十八", "十九", "二十",
            ) -> "弦"
            lunarDayName in setOf(
                "初一", "初二", "初三", "初四", "初五",
                "廿六", "廿七", "廿八", "廿九", "三十",
            ) -> "晦"
            else -> ""
        }

        val branchOfPalace = palaces.associate { it.name to it.branch }
        val mingIdx = Branches.indexOf(mingBranch).coerceAtLeast(0)
        fun palaceAtBranchIndex(bi: Int): String =
            PalaceOrder[(bi - mingIdx + 12) % 12]
        fun shenAt(branch: String): List<String> {
            if (branch.isEmpty()) return emptyList()
            val i = Branches.indexOf(branch)
            return if (i >= 0) shenShaByBranch.getOrElse(i) { emptyList() } else emptyList()
        }
        fun hit(name: String, auspicious: Boolean, detail: String = name) =
            PatternView(name, hit = true, detail = detail, desc = descOf(name, auspicious), auspicious = auspicious)
        fun lordOf(palace: String): StarView? =
            LordOfBranch[branchOfPalace[palace]]?.let { byKey[it] }
        fun inPalace(st: StarView, palace: String): Boolean =
            st.branch.isNotEmpty() && st.branch == branchOfPalace[palace]

        return buildList {
            // —— 星度喜格 ——
            if (sun.branch == "午") add(hit("日居日位", true))
            if (sun.branch == "卯") add(hit("日出扶桑", true))
            if (moon.branch == "未") add(hit("月入月垣", true))
            if (ven.branch == "辰" || ven.branch == "酉") add(hit("太白居垣", true))
            if (jup.branch == "寅" || jup.branch == "亥") add(hit("岁星居垣", true))
            if (mer.branch == "巳" || mer.branch == "申") add(hit("辰星居垣", true))
            if (mar.branch == "卯" || mar.branch == "戌") add(hit("荧惑居垣", true))
            if (sat.branch == "子" || sat.branch == "丑") add(hit("镇星居垣", true))
            if (sun.xiu in setOf("昴", "虚", "房", "星")) add(hit("日到日躔", true))
            if (moon.xiu in setOf("心", "危", "毕", "张")) add(hit("月升月殿", true))
            if (mer.branch == moon.branch && zhouYe == "昼") add(hit("日月并明", true))
            if (sun.branch in setOf("寅","卯","辰") && moon.branch in setOf("申","酉","戌")) add(hit("日东月西", true))
            if (sun.branch in setOf("巳","午","未") && moon.branch in setOf("子","丑","亥")) add(hit("日南月北", true))
            if ((sun.branch in setOf("寅","卯","辰") && moon.branch in setOf("申","酉","戌")) || (sun.branch in setOf("巳","午","未") && moon.branch in setOf("子","丑","亥"))) add(hit("阴阳得地", true))
            if (sun.branch == "午" && moon.branch == "未") add(hit("日月居垣", true))
            if (sun.xiu in setOf("昴","虚","房","星") && moon.xiu in setOf("心","危","毕","张")) add(hit("日月升殿", true))
            if (alone(moon.branch)) add(hit("孤月独明", true))
            if (sun.branch == ven.branch && sun.branch == mer.branch) add(hit("金水从阳", true))
            if (sun.branch == mer.branch) add(hit("水日会合", true))
            if (sun.xiu == "星" && sun.branch == "午") add(hit("日帝居阳", true))
            if (sun.branch == ven.branch && monthBranch in setOf("申","酉")) add(hit("白虎从驾", true))
            if (sun.branch == jup.branch && monthBranch in setOf("寅","卯")) add(hit("青龙扶砚", true))
            if (sun.branch == mer.branch && monthBranch in setOf("亥","子")) add(hit("玄武持旗", true))
            if (sun.branch == mar.branch && monthBranch in setOf("巳","午")) add(hit("朱雀衔符", true))
            if (sun.branch == sat.branch && monthBranch in setOf("辰","戌","丑","未")) add(hit("勾陈镇殿", true))
            if (moon.branch == ven.branch && season != "冬" && zhouYe != "昼") add(hit("金助月华", true))
            if (moon.branch == mar.branch && sun.branch == ven.branch && zhouYe == "夜") add(hit("火金侍月", true))
            if (moon.branch == jup.branch && season != "冬" && zhouYe == "夜" && phase != "晦") add(hit("木月清贵", true))
            if (moon.branch == mer.branch && season != "冬" && phase != "晦") add(hit("水涵蟾魄", true))
            if (moon.branch == mar.branch && phase != "晦" && zhouYe != "昼") add(hit("火月同宵", true))
            if (moon.branch == "丑" && moon.xiu == "斗") add(hit("太阴朝斗", true))
            if (moon.branch == "酉") add(hit("月在沧海", true))
            if (mer.branch == ven.branch && season != "冬") add(hit("金水相涵", true))
            if (mer.branch == ven.branch && moon.branch == "辰") add(hit("水润金明", true))
            if (sat.branch == ven.branch && season != "春" && season != "夏") add(hit("土金坚实", true))
            if (jup.branch == mar.branch && (season == "春" || season == "冬")) add(hit("木火文明", true))
            if (jup.branch == "亥" && ket.branch == "亥") add(hit("木计逢鱼", true))
            if (mer.branch == "子") add(hit("水清宝瓶", true))
            if (sat.branch == "子" && mer.branch == sat.branch) add(hit("土宜见水", true))
            if (sat.branch == "申" && mer.branch == sat.branch) add(hit("水土相会", true))
            if (sat.branch == "亥" && mer.branch == sat.branch) add(hit("水土朝北", true))
            if (mer.branch == "巳") add(hit("水临双女", true))
            if (mar.branch == sat.branch && season != "夏" && monthBranch != "申") add(hit("火土高强", true))
            if (mar.branch == "亥" && mar.xiu == "室") add(hit("火照天门", true))
            if (sat.branch == "丑" && mar.branch == sat.branch) add(hit("土荧相会", true))
            if (sat.xiu == "牛" && mar.xiu == sat.xiu) add(hit("火土会牛", true))
            if (mar.xiu == sat.xiu && zhouYe == "昼") add(hit("火土昼逢", true))
            if (mar.branch == "午") add(hit("火旺南离", true))
            if (sat.branch == "丑") add(hit("土号太常", true))
            if (sat.branch == "子") add(hit("土好宝瓶", true))
            if (rah.branch == moon.branch && phase == "晦" && zhouYe == "夜") add(hit("罗月交辉", true))
            if (zi.branch == moon.branch && phase != "晦" && zhouYe == "夜" && season != "冬") add(hit("祥云捧月", true))
            if (yue.branch == moon.branch && phase != "晦" && zhouYe == "夜" && season != "冬") add(hit("太乙抱蟾", true))
            if (ven.branch == ket.branch && season != "冬") add(hit("金计同垣", true))
            if (jup.branch == rah.branch && (season == "春" || season == "冬")) add(hit("木罗会合", true))
            if (jup.branch == yue.branch && season != "冬") add(hit("木孛符印", true))
            if (sat.branch == rah.branch && season != "夏" && monthBranch != "申") add(hit("土罗相会", true))
            if (rah.branch == "午") add(hit("罗计中分", true))
            if (rah.branch == "子") add(hit("出乾入巽", true))
            if (alone(ket.branch) && alone(rah.branch) && alone(yue.branch) && alone(zi.branch)) add(hit("四余独步", true))
            if (sun.branch == moon.branch && sun.branch == mer.branch && sun.branch == ven.branch && sun.branch == mar.branch && sun.branch == jup.branch && sun.branch == sat.branch) add(hit("日月联辉", true))
            if (sun.branch == moon.branch && sun.branch == mer.branch) add(hit("水附阳光", true))

            // —— 星度忌格 ——
            if (sun.branch == "未") add(hit("日居月位", false))
            if (moon.branch == "午") add(hit("月到日宫", false))
            if (sun.xiu in setOf("心","危","毕","张")) add(hit("日躔月度", false))
            if (moon.xiu in setOf("房","虚","昴","星")) add(hit("月躔日宿", false))
            if (moon.branch in setOf("寅","卯","辰") && sun.branch in setOf("申","酉","戌")) add(hit("日西月东", false))
            if (moon.branch in setOf("巳","午","未") && sun.branch in setOf("子","丑","亥")) add(hit("日北月南", false))
            if ((moon.branch in setOf("寅","卯","辰") && sun.branch in setOf("申","酉","戌")) || (moon.branch in setOf("巳","午","未") && sun.branch in setOf("子","丑","亥"))) add(hit("日月失所", false))
            if (sun.branch == "未" && moon.branch == "午") add(hit("日月失垣", false))
            if (sun.xiu in setOf("心","危","毕","张") && moon.xiu in setOf("房","虚","昴","星")) add(hit("日月失殿", false))
            if (phase == "晦" && zhouYe == "夜") add(hit("阴阳俱晦", false))
            if (alone(sun.branch)) add(hit("孤阳无辅", false))
            if (alone(moon.branch) && season == "冬") add(hit("寒月单行", false))
            if (ven.branch == "寅") add(hit("金骑人马", false))
            if (ven.branch == "亥") add(hit("金在木宫", false))
            if (ven.branch == "卯" || ven.branch == "戌") add(hit("金乘火位", false))
            if (ven.branch == mar.branch) add(hit("火金交战", false))
            if (ven.branch == jup.branch) add(hit("金木同宫", false))
            if (ven.branch == "午") add(hit("金居日分", false))
            if (ven.branch == mar.branch && ven.branch == jup.branch && ven.branch == ket.branch) add(hit("木计火金", false))
            if (jup.branch == "子") add(hit("木打宝瓶", false))
            if (jup.branch == "丑") add(hit("木入土室", false))
            if (jup.branch == "辰" || jup.branch == "酉") add(hit("木入金乡", false))
            if (jup.branch == sat.branch) add(hit("木土相克", false))
            if (mer.branch == "戌") add(hit("水泛白羊", false))
            if (mer.branch == "卯") add(hit("水乘火位", false))
            if (mer.branch == "子" || mer.branch == "丑") add(hit("水居土室", false))
            if (mer.branch == mar.branch) add(hit("水火相刑", false))
            if (mer.branch == yue.branch && mer.branch == "巳") add(hit("水孛逢楚", false))
            if (mer.branch == yue.branch && mer.branch == "寅") add(hit("孛忧水人", false))
            if (mer.branch == yue.branch && mer.branch == "丑") add(hit("泉枯牛壑", false))
            if (mar.branch == "酉") add(hit("火烧牛角", false))
            if (mar.branch == "辰") add(hit("火到金乡", false))
            if (mar.branch == "巳" || mar.branch == "申") add(hit("火居水地", false))
            if (mar.branch == "午" || mar.branch == rah.branch) add(hit("火遇金罗", false))
            if (sat.branch == "巳") add(hit("土埋双女", false))
            if (sat.branch == "申") add(hit("土居水地", false))
            if (sat.branch == "亥" || sat.branch == "寅") add(hit("土在木宫", false))
            if (sat.branch == mer.branch) add(hit("土水相激", false))
            if (sun.branch == mar.branch && sun.branch == rah.branch) add(hit("火罗犯日", false))
            if (sun.branch == jup.branch && sun.branch == zi.branch) add(hit("木蔽阳光", false))
            if (sun.branch == ket.branch) add(hit("计犯太阳", false))
            if (sun.branch == rah.branch) add(hit("罗犯太阳", false))
            if (sun.branch == zi.branch) add(hit("炁日同宫", false))
            if (sun.branch == yue.branch) add(hit("孛犯太阳", false))
            if (moon.branch == sat.branch && moon.branch == ket.branch) add(hit("土计掩月", false))
            if (ven.branch == rah.branch) add(hit("金罗同克", false))
            if (mer.branch == ket.branch) add(hit("水计相刑", false))
            if (mar.branch == yue.branch) add(hit("火孛共战", false))
            if (sat.branch == yue.branch) add(hit("土孛混杂", false))
            if (ket.branch == yue.branch) add(hit("计孛交战", false))
            if (rah.branch == yue.branch) add(hit("孛罗交战", false))
            if (mar.branch == "子" && mer.branch == "午") add(hit("水火相射", false))
            if (ven.branch == "寅" && jup.branch == "酉") add(hit("山泽沉埋", false))

            // —— 日月拱 / 夹 ——
            val sunBi = Branches.indexOf(sun.branch)
            val moonBi = Branches.indexOf(moon.branch)
            if (sunBi >= 0 && moonBi >= 0) {
                fun archIdx(a: Int, b: Int): Int? {
                    val n = kotlin.math.abs(a - b)
                    return when (n) {
                        4 -> if (a > b) (a + 4) % 12 else (b + 4) % 12
                        8 -> if (a > b) (b + 4) % 12 else (a + 4) % 12
                        else -> null
                    }
                }
                fun clampIdx(a: Int, b: Int): Int? {
                    val n = kotlin.math.abs(a - b)
                    return when (n) {
                        2 -> if (a > b) (b + 1) % 12 else (a + 1) % 12
                        10 -> if (a > b) (a + 1) % 12 else (b + 1) % 12
                        else -> null
                    }
                }
                archIdx(sunBi, moonBi)?.let { qi ->
                    val pn = palaceAtBranchIndex(qi)
                    when {
                        pn in BadPalaces -> add(hit("日月拱${pn.take(1)}宫", false))
                        pn in GoodPalaces -> add(hit("日月拱${pn.take(1)}宫", true))
                    }
                    shenAt(Branches[qi]).forEach { ss ->
                        when {
                            ss in XiShen -> add(hit("日月拱$ss", true))
                            ss in JiShen -> add(hit("日月拱$ss", false))
                        }
                    }
                }
                clampIdx(sunBi, moonBi)?.let { qi ->
                    val pn = palaceAtBranchIndex(qi)
                    when {
                        pn in BadPalaces -> add(hit("日月夹${pn.take(1)}宫", false))
                        pn in GoodPalaces -> add(hit("日月夹${pn.take(1)}宫", true))
                    }
                }
            }

            // —— 身命神煞格 ——
            val yangRen = setOf("羊刃", "阳刃")
            if (shenAt(mingBranch).any { it in yangRen }) add(hit("命坐刃乡", false))
            shenBranch?.let { sb ->
                if (shenAt(sb).any { it in yangRen }) add(hit("身坐刃乡", false))
                if (shenAt(mingBranch).any { it in yangRen } && shenAt(sb).any { it in yangRen }) {
                    add(hit("身命坐刃", false))
                }
                val ss = shenAt(sb)
                if ("禄勋" in ss && "卦气" in ss) add(hit("身坐卦禄", true))
                if ("华盖" in ss) add(hit("身居闲极", true))
            }
            branchOfPalace["福德"]?.let { if (shenAt(it).any { n -> n in yangRen }) add(hit("福宫坐刃", false)) }
            branchOfPalace["夫妻"]?.let { if (shenAt(it).any { n -> n in yangRen }) add(hit("刃临妻位", false)) }
            branchOfPalace["男女"]?.let { if (shenAt(it).any { n -> n in yangRen }) add(hit("刃临儿位", false)) }

            listOf("飞廉", "剑锋", "大耗", "天耗", "地耗", "死符", "病符", "的杀", "劫杀", "天雄").forEach { n ->
                val aliases = if (n == "劫杀") setOf("劫杀", "劫煞") else setOf(n)
                if (shenAt(mingBranch).any { it in aliases }) add(hit("命临$n", false))
                shenBranch?.let { sb ->
                    if (shenAt(sb).any { it in aliases }) add(hit("身临$n", false))
                }
            }
            listOf("卦气", "禄勋", "斗杓", "长生", "帝旺", "岁驾", "国印").forEach { n ->
                if (n in shenAt(mingBranch)) add(hit("命临$n", true))
                shenBranch?.let { sb -> if (n in shenAt(sb)) add(hit("身临$n", true)) }
            }
            val mingSs = shenAt(mingBranch)
            if ("斗杓" in mingSs && mingSs.any { it in setOf("帝旺", "长生") }) add(hit("斗旺坐命", true))
            if (mingSs.any { it in setOf("国印", "唐符") }) add(hit("命临官贵", true))
            branchOfPalace["官禄"]?.let { if ("斗杓" in shenAt(it)) add(hit("斗杓指禄", true)) }

            // —— 宫主喜忌 ——
            lordOf("福德")?.let { fu ->
                lordOf("官禄")?.let { guan ->
                    if (fu.branch == guan.branch) add(hit("福官会聚", true, "福德主与官禄主同宫"))
                    if (inPalace(fu, "命宫") && inPalace(guan, "命宫")) add(hit("福官守命", true))
                    if (inPalace(fu, "福德") && inPalace(guan, "福德")) add(hit("福官居福", true))
                    if (inPalace(fu, "夫妻") && inPalace(guan, "夫妻")) add(hit("福官守妻", true))
                    if (inPalace(fu, "财帛") && inPalace(guan, "财帛")) add(hit("福官临财", true))
                    if (inPalace(fu, "田宅") && inPalace(guan, "田宅")) add(hit("福官临田", true))
                    if (inPalace(fu, "官禄") && inPalace(guan, "官禄")) add(hit("福官居官", true))
                    if (inPalace(fu, "男女") && inPalace(guan, "男女")) add(hit("福官守儿", true))
                }
            }
            if (inPalace(moon, "男女")) add(hit("身居男女", true, "身宫入男女宫"))
            if (inPalace(moon, "命宫")) add(hit("身居命宫", true))
            if (inPalace(moon, "福德")) add(hit("身居福德", true))
            lordOf("命宫")?.let { if (inPalace(it, "命宫")) add(hit("命主守命", true)) }
            lordOf("官禄")?.let { if (inPalace(it, "命宫")) add(hit("官星朝命", true)) }
            lordOf("田宅")?.let { if (inPalace(it, "田宅")) add(hit("田宅归垣", true)) }
            lordOf("夫妻")?.let { if (inPalace(it, "夫妻")) add(hit("妻守妻宫", true)) }
            lordOf("夫妻")?.let { if (inPalace(it, "相貌")) add(hit("夫入相貌", false)) }
            lordOf("财帛")?.let { if (inPalace(it, "迁移")) add(hit("财入迁移", false)) }
            lordOf("男女")?.let { if (inPalace(it, "迁移")) add(hit("嗣入迁移", false)) }
            lordOf("命宫")?.let { if (inPalace(it, "疾厄")) add(hit("命入疾厄", false)) }

            // —— 复合格 ——
            if (LordOfBranch[branchOfPalace["命宫"]] == "木" &&
                LordOfBranch[branchOfPalace["官禄"]] == "木" &&
                jup.branch in setOf("寅", "亥") &&
                (zi.branch in setOf("寅", "亥") || yue.branch in setOf("寅", "亥"))
            ) add(hit("木炁连枝", true))

            if (mingBranch in setOf("亥", "申") && jup.branch == "亥" && mer.branch == "申" &&
                rah.branch in setOf("午", "子")
            ) add(hit("天地开明", true))

            if (duLordKey == "水" && mer.branch == "亥") add(hit("双鱼戏水", true))
            if (duLordKey == "水" && mer.xiu == "参" && sun.branch == mer.branch) add(hit("玉猿守昆", true))
            if (mingXiu == "箕" && moon.xiu == "张" && mer.xiu == "轸") add(hit("玉女嫦娥", true))
            if (mingBranch == "辰" && mer.branch == "卯" && mar.branch == "巳") {
                add(hit("风雷鼓舞", true))
                add(hit("风雷相博", false))
            }
            if (zi.xiu == "斗" && yue.xiu == "斗") add(hit("炁孛朝斗", true))
            if ((mar.xiu == "室" || mar.xiu == "翼") && mer.xiu == "轸" && yue.xiu == "壁") {
                add(hit("火孛擎天", true))
            }

            val south = setOf("巳", "午", "未")
            val north = setOf("亥", "子", "丑")
            val seven = listOf(sun, moon, mer, ven, mar, jup, sat)
            if (mingBranch in south && seven.all { it.branch in south }) add(hit("众曜拱南", true))
            if (mingBranch in north && mer.branch == "亥" &&
                listOf(sun, moon, ven, mar, jup, sat).all { it.branch in north }
            ) add(hit("群星朝北", true))

            branchOfPalace["奴仆"]?.let { if (sun.branch == it) add(hit("日陷奴宫", false)) }

            fun hasAny(branch: String?, names: Set<String>) =
                branch != null && shenAt(branch).any { it in names }
            if (hasAny(mingBranch, setOf("的杀")) && hasAny(mingBranch, yangRen)) add(hit("的刃守命", false))
            if (hasAny(mingBranch, setOf("天雄")) && hasAny(mingBranch, yangRen)) add(hit("刃雄守命", false))
            if (hasAny(mingBranch, setOf("的杀")) && hasAny(mingBranch, setOf("劫杀", "劫煞"))) add(hit("的劫守命", false))
            val fuB = branchOfPalace["福德"]
            val qiB = branchOfPalace["夫妻"]
            val erB = branchOfPalace["男女"]
            if (hasAny(fuB, setOf("的杀")) && hasAny(fuB, yangRen)) add(hit("的刃居福", false))
            if (hasAny(qiB, setOf("的杀")) && hasAny(qiB, yangRen)) add(hit("的刃妻位", false))
            if (hasAny(qiB, setOf("天雄")) && hasAny(qiB, setOf("空亡"))) add(hit("空雄妻位", false))
            if (hasAny(qiB, setOf("孤辰")) && hasAny(qiB, setOf("劫杀", "劫煞"))) add(hit("孤劫临妻", false))
            if (hasAny(erB, setOf("寡宿")) && hasAny(erB, setOf("天狗"))) add(hit("狗寡儿宫", false))
            if (erB != null && ket.branch == erB && hasAny(erB, yangRen)) add(hit("计刃儿位", false))
            if (hasAny(erB, setOf("天狗"))) add(hit("天狗临儿", false))
        }.distinctBy { it.name to it.auspicious }
    }

    private fun descOf(name: String, auspicious: Boolean): String =
        XingGeDesc.of(name, auspicious)
}
