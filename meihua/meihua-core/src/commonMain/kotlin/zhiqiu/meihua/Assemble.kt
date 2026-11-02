package zhiqiu.meihua

import zhiqiu.liuyao.HexagramEntry
import zhiqiu.liuyao.Trigram
import zhiqiu.liuyao.hexagramOf
import zhiqiu.liuyao.zhouyiOf

/**
 * 由上卦、下卦、动爻装配完整梅花盘。
 * 动爻 1=初…6=上；动爻在下卦(1–3)则下为用，在上卦(4–6)则上为用。
 */
fun assemble(
    upper: Trigram,
    lower: Trigram,
    movingYao: Int,
    trace: CastTrace,
    stamp: LunarStamp? = null,
    lunarMonthForWang: Int? = stamp?.month,
    chengGuaShu: Int? = null,
    posture: Posture? = null,
    question: String = "",
): MeiHuaChart {
    require(movingYao in 1..6) { "动爻须在 1..6，实际为 $movingYao" }

    val ben = hexagramOf(upper, lower)
    val bits = ben.bitsBottomUp.toMutableList()
    bits[movingYao - 1] = if (bits[movingYao - 1] == 1) 0 else 1
    val bian = hexagramOf(bits)

    val (huLower, huUpper) = mutualTrigrams(bits = ben.bitsBottomUp)
    var hu = hexagramOf(huUpper, huLower)
    var huFromBian = false
    // 乾坤无互：互出仍与本卦相同则改取变卦之互
    if (hu.fullName == ben.fullName) {
        val (bl, bu) = mutualTrigrams(bits = bian.bitsBottomUp)
        hu = hexagramOf(bu, bl)
        huFromBian = true
    }

    val tiIsUpper = movingYao <= 3
    val ti = if (tiIsUpper) upper else lower
    val yong = if (tiIsUpper) lower else upper
    val relation = tiYongRelationOf(ti, yong)

    val bianYong = if (tiIsUpper) bian.lower else bian.upper
    val bianRelation = tiYongRelationOf(ti, bianYong)

    val (rawHuLower, rawHuUpper) = mutualTrigrams(bits = ben.bitsBottomUp)
    val displayHuLower = if (huFromBian) hu.lower else rawHuLower
    val displayHuUpper = if (huFromBian) hu.upper else rawHuUpper
    // 体互/用互：体在上则上互为体互
    val tiHu = if (tiIsUpper) displayHuUpper else displayHuLower
    val yongHu = if (tiIsUpper) displayHuLower else displayHuUpper

    val season = lunarMonthForWang?.let { seasonElementOf(it) }
    fun wang(t: Trigram) = season?.let { wangShuaiOf(t.element, it) } ?: WangShuai.XIU

    val yingQi = if (chengGuaShu != null && posture != null) {
        yingQiDaysOf(chengGuaShu, posture)
    } else null

    return MeiHuaChart(
        method = trace.method,
        upper = upper,
        lower = lower,
        movingYao = movingYao,
        ben = ben,
        hu = hu,
        bian = bian,
        huFromBian = huFromBian,
        ti = ti,
        yong = yong,
        tiIsUpper = tiIsUpper,
        relation = relation,
        bianYong = bianYong,
        bianRelation = bianRelation,
        changeSummary = changeSummaryOf(relation, bianRelation),
        tiHu = tiHu,
        yongHu = yongHu,
        tiWang = wang(ti),
        yongWang = wang(yong),
        huWang = wang(hu.upper), // 互卦整体参考上互五行亦可；展示时用卦五行各算
        bianWang = wang(bianYong),
        lunarMonth = lunarMonthForWang,
        chengGuaShu = chengGuaShu,
        yingQiDays = yingQi,
        posture = posture,
        stamp = stamp,
        trace = trace,
        tiXiang = xiangOf(ti),
        yongXiang = xiangOf(yong),
        benText = zhouyiOf(ben),
        bianText = zhouyiOf(bian),
        question = question,
    )
}

/**
 * 互卦：二三四爻为下互，三四五爻为上互（爻序初=0）。
 * 返回 Pair(下互, 上互)。
 */
fun mutualTrigrams(bits: List<Int>): Pair<Trigram, Trigram> {
    require(bits.size == 6)
    val lowerBits = listOf(bits[1], bits[2], bits[3])
    val upperBits = listOf(bits[2], bits[3], bits[4])
    return trigramFromBits(lowerBits) to trigramFromBits(upperBits)
}

fun mutualTrigrams(entry: HexagramEntry): Pair<Trigram, Trigram> =
    mutualTrigrams(entry.bitsBottomUp)

fun trigramFromBits(bitsBottomUp: List<Int>): Trigram {
    require(bitsBottomUp.size == 3)
    return Trigram.entries.first { it.bits.toList() == bitsBottomUp }
}
