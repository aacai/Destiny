package zhiqiu.liuyao

/**
 * 常用神煞：以占日（及年/月）干支起例，命中卦中地支即临该爻。
 * 日支三合局：驿马、咸池、华盖、劫煞、将星、亡神、灾煞；
 * 日干：羊刃、飞刃、天乙、禄、文昌、金舆；
 * 年支：红鸾、天喜；月支：天医。
 */
enum class ShenSha(val label: String) {
    YANG_REN("羊刃"),
    YI_MA("驿马"),
    TAO_HUA("咸池"),
    TIAN_YI("贵人"),
    HUA_GAI("华盖"),
    JIE_SHA("劫煞"),
    LU("禄"),
    WEN_CHANG("文昌"),
    JIN_YU("金舆"),
    JIANG_XING("将星"),
    WANG_SHEN("亡神"),
    ZAI_SHA("灾煞"),
    FEI_REN("飞刃"),
    HONG_LUAN("红鸾"),
    TIAN_XI("天喜"),
    TIAN_YI_YUE("天医"),
}

/** 三合局分组：0=申子辰水局，1=寅午戌火局，2=巳酉丑金局，3=亥卯未木局。 */
private fun sanHeGroup(branch: Branch): Int = when (branch) {
    Branch.SHEN, Branch.ZI, Branch.CHEN -> 0
    Branch.YIN, Branch.WU, Branch.XU -> 1
    Branch.SI, Branch.YOU, Branch.CHOU -> 2
    Branch.MAO, Branch.WEI, Branch.HAI -> 3
}

private fun Branch.opposite(): Branch = Branch.entries[(ordinal + 6) % 12]

/** 驿马：申子辰马在寅，寅午戌马在申，巳酉丑马在亥，亥卯未马在巳。 */
fun yiMaOf(dayBranch: Branch): Branch = when (sanHeGroup(dayBranch)) {
    0 -> Branch.YIN
    1 -> Branch.SHEN
    2 -> Branch.HAI
    else -> Branch.SI
}

/** 桃花（咸池）：申子辰见酉，寅午戌见卯，巳酉丑见午，亥卯未见子。 */
fun taoHuaOf(dayBranch: Branch): Branch = when (sanHeGroup(dayBranch)) {
    0 -> Branch.YOU
    1 -> Branch.MAO
    2 -> Branch.WU
    else -> Branch.ZI
}

/** 华盖：申子辰见辰，寅午戌见戌，巳酉丑见丑，亥卯未见未。 */
fun huaGaiOf(dayBranch: Branch): Branch = when (sanHeGroup(dayBranch)) {
    0 -> Branch.CHEN
    1 -> Branch.XU
    2 -> Branch.CHOU
    else -> Branch.WEI
}

/** 劫煞：申子辰见巳，寅午戌见亥，巳酉丑见寅，亥卯未见申。 */
fun jieShaOf(dayBranch: Branch): Branch = when (sanHeGroup(dayBranch)) {
    0 -> Branch.SI
    1 -> Branch.HAI
    2 -> Branch.YIN
    else -> Branch.SHEN
}

/** 将星：申子辰见子，寅午戌见午，巳酉丑见酉，亥卯未见卯。 */
fun jiangXingOf(dayBranch: Branch): Branch = when (sanHeGroup(dayBranch)) {
    0 -> Branch.ZI
    1 -> Branch.WU
    2 -> Branch.YOU
    else -> Branch.MAO
}

/** 亡神：申子辰见亥，寅午戌见巳，巳酉丑见申，亥卯未见寅。 */
fun wangShenOf(dayBranch: Branch): Branch = when (sanHeGroup(dayBranch)) {
    0 -> Branch.HAI
    1 -> Branch.SI
    2 -> Branch.SHEN
    else -> Branch.YIN
}

/** 灾煞：将星之冲。 */
fun zaiShaOf(dayBranch: Branch): Branch = jiangXingOf(dayBranch).opposite()

/** 羊刃：以日干起，刃在劫前一位（阳干顺、阴干逆的帝旺位）。 */
fun yangRenOf(dayStem: Stem): Branch = when (dayStem) {
    Stem.JIA -> Branch.MAO
    Stem.YI -> Branch.YIN
    Stem.BING, Stem.WU -> Branch.WU
    Stem.DING, Stem.JI -> Branch.SI
    Stem.GENG -> Branch.YOU
    Stem.XIN -> Branch.SHEN
    Stem.REN -> Branch.ZI
    Stem.GUI -> Branch.HAI
}

/** 飞刃：羊刃对冲。 */
fun feiRenOf(dayStem: Stem): Branch = yangRenOf(dayStem).opposite()

/** 天乙贵人：甲戊庚牛羊，乙己鼠猴乡，丙丁猪鸡位，壬癸兔蛇藏，辛逢虎马。 */
fun tianYiOf(dayStem: Stem): Set<Branch> = when (dayStem) {
    Stem.JIA, Stem.WU, Stem.GENG -> setOf(Branch.CHOU, Branch.WEI)
    Stem.YI, Stem.JI -> setOf(Branch.ZI, Branch.SHEN)
    Stem.BING, Stem.DING -> setOf(Branch.HAI, Branch.YOU)
    Stem.REN, Stem.GUI -> setOf(Branch.MAO, Branch.SI)
    Stem.XIN -> setOf(Branch.YIN, Branch.WU)
}

/** 禄神：甲寅乙卯，丙戊巳，丁己午，庚申辛酉，壬亥癸子。 */
fun luOf(dayStem: Stem): Branch = when (dayStem) {
    Stem.JIA -> Branch.YIN
    Stem.YI -> Branch.MAO
    Stem.BING, Stem.WU -> Branch.SI
    Stem.DING, Stem.JI -> Branch.WU
    Stem.GENG -> Branch.SHEN
    Stem.XIN -> Branch.YOU
    Stem.REN -> Branch.HAI
    Stem.GUI -> Branch.ZI
}

/** 文昌：甲巳乙午，丙戊申，丁己酉，庚亥辛子，壬寅癸卯。 */
fun wenChangOf(dayStem: Stem): Branch = when (dayStem) {
    Stem.JIA -> Branch.SI
    Stem.YI -> Branch.WU
    Stem.BING, Stem.WU -> Branch.SHEN
    Stem.DING, Stem.JI -> Branch.YOU
    Stem.GENG -> Branch.HAI
    Stem.XIN -> Branch.ZI
    Stem.REN -> Branch.YIN
    Stem.GUI -> Branch.MAO
}

/** 金舆：甲辰乙巳，丙戊未，丁己申，庚戌辛亥，壬丑癸寅。 */
fun jinYuOf(dayStem: Stem): Branch = when (dayStem) {
    Stem.JIA -> Branch.CHEN
    Stem.YI -> Branch.SI
    Stem.BING, Stem.WU -> Branch.WEI
    Stem.DING, Stem.JI -> Branch.SHEN
    Stem.GENG -> Branch.XU
    Stem.XIN -> Branch.HAI
    Stem.REN -> Branch.CHOU
    Stem.GUI -> Branch.YIN
}

/** 红鸾：以年支起，子年卯、丑年寅…亥年辰。 */
fun hongLuanOf(yearBranch: Branch): Branch =
    Branch.entries[(3 - yearBranch.ordinal + 12) % 12]

/** 天喜：红鸾对冲。 */
fun tianXiOf(yearBranch: Branch): Branch = hongLuanOf(yearBranch).opposite()

/** 天医：月支前一位（寅月在丑，卯月在寅…）。 */
fun tianYiYueOf(monthBranch: Branch): Branch =
    Branch.entries[(monthBranch.ordinal + 11) % 12]

/** 占时干支出发，算出每个神煞对应的地支。 */
fun shenShaBranches(dayInfo: DayInfo): Map<ShenSha, Set<Branch>> =
    shenShaBranches(
        dayStem = dayInfo.dayStem,
        dayBranch = dayInfo.dayBranch,
        yearBranch = dayInfo.yearBranch,
        monthBranch = dayInfo.monthBranch,
    )

/** 与 [shenShaBranches] 相同，便于单测只传干支。 */
fun shenShaBranches(
    dayStem: Stem,
    dayBranch: Branch,
    yearBranch: Branch = dayBranch,
    monthBranch: Branch = dayBranch,
): Map<ShenSha, Set<Branch>> = mapOf(
    ShenSha.YANG_REN to setOf(yangRenOf(dayStem)),
    ShenSha.YI_MA to setOf(yiMaOf(dayBranch)),
    ShenSha.TAO_HUA to setOf(taoHuaOf(dayBranch)),
    ShenSha.TIAN_YI to tianYiOf(dayStem),
    ShenSha.HUA_GAI to setOf(huaGaiOf(dayBranch)),
    ShenSha.JIE_SHA to setOf(jieShaOf(dayBranch)),
    ShenSha.LU to setOf(luOf(dayStem)),
    ShenSha.WEN_CHANG to setOf(wenChangOf(dayStem)),
    ShenSha.JIN_YU to setOf(jinYuOf(dayStem)),
    ShenSha.JIANG_XING to setOf(jiangXingOf(dayBranch)),
    ShenSha.WANG_SHEN to setOf(wangShenOf(dayBranch)),
    ShenSha.ZAI_SHA to setOf(zaiShaOf(dayBranch)),
    ShenSha.FEI_REN to setOf(feiRenOf(dayStem)),
    ShenSha.HONG_LUAN to setOf(hongLuanOf(yearBranch)),
    ShenSha.TIAN_XI to setOf(tianXiOf(yearBranch)),
    ShenSha.TIAN_YI_YUE to setOf(tianYiYueOf(monthBranch)),
)
