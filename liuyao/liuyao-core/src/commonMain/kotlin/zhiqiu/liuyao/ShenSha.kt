package zhiqiu.liuyao

/** 常用神煞：以占日干支起例，命中卦中地支即临该爻。 */
enum class ShenSha(val label: String) {
    YI_MA("驿马"),
    TAO_HUA("桃花"),
    YANG_REN("羊刃"),
    TIAN_YI("天乙贵人"),
    HUA_GAI("华盖"),
    JIE_SHA("劫煞"),
}

/** 三合局分组：0=申子辰水局，1=寅午戌火局，2=巳酉丑金局，3=亥卯未木局。 */
private fun sanHeGroup(branch: Branch): Int = when (branch) {
    Branch.SHEN, Branch.ZI, Branch.CHEN -> 0
    Branch.YIN, Branch.WU, Branch.XU -> 1
    Branch.SI, Branch.YOU, Branch.CHOU -> 2
    Branch.MAO, Branch.WEI, Branch.HAI -> 3
}

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

/** 天乙贵人：甲戊庚牛羊，乙己鼠猴乡，丙丁猪鸡位，壬癸兔蛇藏，辛逢虎马。 */
fun tianYiOf(dayStem: Stem): Set<Branch> = when (dayStem) {
    Stem.JIA, Stem.WU, Stem.GENG -> setOf(Branch.CHOU, Branch.WEI)
    Stem.YI, Stem.JI -> setOf(Branch.ZI, Branch.SHEN)
    Stem.BING, Stem.DING -> setOf(Branch.HAI, Branch.YOU)
    Stem.REN, Stem.GUI -> setOf(Branch.MAO, Branch.SI)
    Stem.XIN -> setOf(Branch.YIN, Branch.WU)
}

/** 占日干支出发，算出每个神煞对应的地支。 */
fun shenShaBranches(dayStem: Stem, dayBranch: Branch): Map<ShenSha, Set<Branch>> = mapOf(
    ShenSha.YI_MA to setOf(yiMaOf(dayBranch)),
    ShenSha.TAO_HUA to setOf(taoHuaOf(dayBranch)),
    ShenSha.YANG_REN to setOf(yangRenOf(dayStem)),
    ShenSha.TIAN_YI to tianYiOf(dayStem),
    ShenSha.HUA_GAI to setOf(huaGaiOf(dayBranch)),
    ShenSha.JIE_SHA to setOf(jieShaOf(dayBranch)),
)
