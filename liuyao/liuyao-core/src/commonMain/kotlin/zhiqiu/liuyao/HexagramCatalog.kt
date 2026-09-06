package zhiqiu.liuyao

/**
 * 六十四卦目录：卦名（上卦＋下卦）、宫位、世系。
 * 上/下卦写法与传统一致，如「天风姤」= 上乾下巽。
 */
data class HexagramEntry(
    val fullName: String,
    val upper: Trigram,
    val lower: Trigram,
    val palace: Palace,
    val type: HexagramType,
) {
    val shiPosition: Int get() = type.shiPosition
    /** 应爻恒与世爻隔两位。 */
    val yingPosition: Int get() = (shiPosition + 2) % 6 + 1
    /** 初→上六爻阴阳。 */
    val bitsBottomUp: List<Int> get() = lower.bits.toList() + upper.bits.toList()
    val bitsKey: String get() = bitsBottomUp.joinToString("")
}

private fun e(
    fullName: String,
    upper: Trigram,
    lower: Trigram,
    palace: Palace,
    type: HexagramType,
) = HexagramEntry(fullName, upper, lower, palace, type)

val HEXAGRAM_CATALOG: List<HexagramEntry> = listOf(
    // —— 乾宫属金 ——
    e("乾为天", Trigram.QIAN, Trigram.QIAN, Palace.QIAN, HexagramType.BEN_GONG),
    e("天风姤", Trigram.QIAN, Trigram.XUN, Palace.QIAN, HexagramType.FIRST),
    e("天山遁", Trigram.QIAN, Trigram.GEN, Palace.QIAN, HexagramType.SECOND),
    e("天地否", Trigram.QIAN, Trigram.KUN, Palace.QIAN, HexagramType.THIRD),
    e("风地观", Trigram.XUN, Trigram.KUN, Palace.QIAN, HexagramType.FOURTH),
    e("山地剥", Trigram.GEN, Trigram.KUN, Palace.QIAN, HexagramType.FIFTH),
    e("火地晋", Trigram.LI, Trigram.KUN, Palace.QIAN, HexagramType.YOU_HUN),
    e("火天大有", Trigram.LI, Trigram.QIAN, Palace.QIAN, HexagramType.GUI_HUN),
    // —— 兑宫属金 ——
    e("兑为泽", Trigram.DUI, Trigram.DUI, Palace.DUI, HexagramType.BEN_GONG),
    e("泽水困", Trigram.DUI, Trigram.KAN, Palace.DUI, HexagramType.FIRST),
    e("泽地萃", Trigram.DUI, Trigram.KUN, Palace.DUI, HexagramType.SECOND),
    e("泽山咸", Trigram.DUI, Trigram.GEN, Palace.DUI, HexagramType.THIRD),
    e("水山蹇", Trigram.KAN, Trigram.GEN, Palace.DUI, HexagramType.FOURTH),
    e("地山谦", Trigram.KUN, Trigram.GEN, Palace.DUI, HexagramType.FIFTH),
    e("雷山小过", Trigram.ZHEN, Trigram.GEN, Palace.DUI, HexagramType.YOU_HUN),
    e("雷泽归妹", Trigram.ZHEN, Trigram.DUI, Palace.DUI, HexagramType.GUI_HUN),
    // —— 离宫属火 ——
    e("离为火", Trigram.LI, Trigram.LI, Palace.LI, HexagramType.BEN_GONG),
    e("火山旅", Trigram.LI, Trigram.GEN, Palace.LI, HexagramType.FIRST),
    e("火风鼎", Trigram.LI, Trigram.XUN, Palace.LI, HexagramType.SECOND),
    e("火水未济", Trigram.LI, Trigram.KAN, Palace.LI, HexagramType.THIRD),
    e("山水蒙", Trigram.GEN, Trigram.KAN, Palace.LI, HexagramType.FOURTH),
    e("风水涣", Trigram.XUN, Trigram.KAN, Palace.LI, HexagramType.FIFTH),
    e("天水讼", Trigram.QIAN, Trigram.KAN, Palace.LI, HexagramType.YOU_HUN),
    e("天火同人", Trigram.QIAN, Trigram.LI, Palace.LI, HexagramType.GUI_HUN),
    // —— 震宫属木 ——
    e("震为雷", Trigram.ZHEN, Trigram.ZHEN, Palace.ZHEN, HexagramType.BEN_GONG),
    e("雷地豫", Trigram.ZHEN, Trigram.KUN, Palace.ZHEN, HexagramType.FIRST),
    e("雷水解", Trigram.ZHEN, Trigram.KAN, Palace.ZHEN, HexagramType.SECOND),
    e("雷风恒", Trigram.ZHEN, Trigram.XUN, Palace.ZHEN, HexagramType.THIRD),
    e("地风升", Trigram.KUN, Trigram.XUN, Palace.ZHEN, HexagramType.FOURTH),
    e("水风井", Trigram.KAN, Trigram.XUN, Palace.ZHEN, HexagramType.FIFTH),
    e("泽风大过", Trigram.DUI, Trigram.XUN, Palace.ZHEN, HexagramType.YOU_HUN),
    e("泽雷随", Trigram.DUI, Trigram.ZHEN, Palace.ZHEN, HexagramType.GUI_HUN),
    // —— 巽宫属木 ——
    e("巽为风", Trigram.XUN, Trigram.XUN, Palace.XUN, HexagramType.BEN_GONG),
    e("风天小畜", Trigram.XUN, Trigram.QIAN, Palace.XUN, HexagramType.FIRST),
    e("风火家人", Trigram.XUN, Trigram.LI, Palace.XUN, HexagramType.SECOND),
    e("风雷益", Trigram.XUN, Trigram.ZHEN, Palace.XUN, HexagramType.THIRD),
    e("天雷无妄", Trigram.QIAN, Trigram.ZHEN, Palace.XUN, HexagramType.FOURTH),
    e("火雷噬嗑", Trigram.LI, Trigram.ZHEN, Palace.XUN, HexagramType.FIFTH),
    e("山雷颐", Trigram.GEN, Trigram.ZHEN, Palace.XUN, HexagramType.YOU_HUN),
    e("山风蛊", Trigram.GEN, Trigram.XUN, Palace.XUN, HexagramType.GUI_HUN),
    // —— 坎宫属水 ——
    e("坎为水", Trigram.KAN, Trigram.KAN, Palace.KAN, HexagramType.BEN_GONG),
    e("水泽节", Trigram.KAN, Trigram.DUI, Palace.KAN, HexagramType.FIRST),
    e("水雷屯", Trigram.KAN, Trigram.ZHEN, Palace.KAN, HexagramType.SECOND),
    e("水火既济", Trigram.KAN, Trigram.LI, Palace.KAN, HexagramType.THIRD),
    e("泽火革", Trigram.DUI, Trigram.LI, Palace.KAN, HexagramType.FOURTH),
    e("雷火丰", Trigram.ZHEN, Trigram.LI, Palace.KAN, HexagramType.FIFTH),
    e("地火明夷", Trigram.KUN, Trigram.LI, Palace.KAN, HexagramType.YOU_HUN),
    e("地水师", Trigram.KUN, Trigram.KAN, Palace.KAN, HexagramType.GUI_HUN),
    // —— 艮宫属土 ——
    e("艮为山", Trigram.GEN, Trigram.GEN, Palace.GEN, HexagramType.BEN_GONG),
    e("山火贲", Trigram.GEN, Trigram.LI, Palace.GEN, HexagramType.FIRST),
    e("山天大畜", Trigram.GEN, Trigram.QIAN, Palace.GEN, HexagramType.SECOND),
    e("山泽损", Trigram.GEN, Trigram.DUI, Palace.GEN, HexagramType.THIRD),
    e("火泽睽", Trigram.LI, Trigram.DUI, Palace.GEN, HexagramType.FOURTH),
    e("天泽履", Trigram.QIAN, Trigram.DUI, Palace.GEN, HexagramType.FIFTH),
    e("风泽中孚", Trigram.XUN, Trigram.DUI, Palace.GEN, HexagramType.YOU_HUN),
    e("风山渐", Trigram.XUN, Trigram.GEN, Palace.GEN, HexagramType.GUI_HUN),
    // —— 坤宫属土 ——
    e("坤为地", Trigram.KUN, Trigram.KUN, Palace.KUN, HexagramType.BEN_GONG),
    e("地雷复", Trigram.KUN, Trigram.ZHEN, Palace.KUN, HexagramType.FIRST),
    e("地泽临", Trigram.KUN, Trigram.DUI, Palace.KUN, HexagramType.SECOND),
    e("地天泰", Trigram.KUN, Trigram.QIAN, Palace.KUN, HexagramType.THIRD),
    e("雷天大壮", Trigram.ZHEN, Trigram.QIAN, Palace.KUN, HexagramType.FOURTH),
    e("泽天夬", Trigram.DUI, Trigram.QIAN, Palace.KUN, HexagramType.FIFTH),
    e("水天需", Trigram.KAN, Trigram.QIAN, Palace.KUN, HexagramType.YOU_HUN),
    e("水地比", Trigram.KAN, Trigram.KUN, Palace.KUN, HexagramType.GUI_HUN),
)

private val HEXAGRAM_BY_BITS: Map<String, HexagramEntry> =
    HEXAGRAM_CATALOG.associateBy { it.bitsKey }

/** 按初→上阴阳查卦。 */
fun hexagramOf(bitsBottomUp: List<Int>): HexagramEntry =
    HEXAGRAM_BY_BITS[bitsBottomUp.joinToString("")] ?: error("未知卦象: $bitsBottomUp")

fun hexagramOf(upper: Trigram, lower: Trigram): HexagramEntry =
    HEXAGRAM_CATALOG.first { it.upper == upper && it.lower == lower }

/** 纳甲：取某卦第 position 爻（1=初…6=上）的干支。 */
fun najiaOf(entry: HexagramEntry, position: Int): Ganzhi {
    require(position in 1..6)
    return if (position <= 3) {
        Ganzhi(entry.lower.lowerStems[position - 1], entry.lower.lowerBranches[position - 1])
    } else {
        Ganzhi(entry.upper.upperStems[position - 4], entry.upper.upperBranches[position - 4])
    }
}
