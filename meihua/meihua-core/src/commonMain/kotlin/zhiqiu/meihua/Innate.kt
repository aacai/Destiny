package zhiqiu.meihua

import zhiqiu.liuyao.Trigram

/**
 * 先天八卦数：乾1兑2离3震4巽5坎6艮7坤8。
 * 与 [Trigram] 声明顺序一致（ordinal+1）。
 */
fun innateNumber(t: Trigram): Int = t.ordinal + 1

fun trigramOfInnate(n: Int): Trigram {
    require(n in 1..8) { "先天数须在 1..8，实际为 $n" }
    return Trigram.entries[n - 1]
}

/** 除 8 取余，整除取 8。 */
fun rem8(n: Int): Int {
    require(n > 0) { "取余数须为正整数，实际为 $n" }
    val r = n % 8
    return if (r == 0) 8 else r
}

/** 除 6 取余，整除取 6（动爻初…上）。 */
fun rem6(n: Int): Int {
    require(n > 0) { "取余数须为正整数，实际为 $n" }
    val r = n % 6
    return if (r == 0) 6 else r
}

val BRANCH_LABELS: List<String> =
    listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

fun branchLabelOfNum(num1to12: Int): String {
    require(num1to12 in 1..12)
    return BRANCH_LABELS[num1to12 - 1]
}

fun branchNumOfLabel(label: String): Int {
    val i = BRANCH_LABELS.indexOf(label)
    require(i >= 0) { "未知地支: $label" }
    return i + 1
}
