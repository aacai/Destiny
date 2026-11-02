package zhiqiu.meihua

import zhiqiu.liuyao.Element
import zhiqiu.liuyao.HexagramEntry
import zhiqiu.liuyao.Trigram
import zhiqiu.liuyao.ZhouyiText
import zhiqiu.liuyao.controls
import zhiqiu.liuyao.generates

/** 端法占时求测者姿态，仅影响应期天数。 */
enum class Posture(val label: String) {
    WALKING("行"),
    STANDING("立"),
    SITTING("坐"),
}

/** 起卦法来源。 */
enum class CastMethod(val label: String) {
    TIME("时间起卦"),
    NUMBERS("数字起卦"),
    XIANG("端法占"),
    MANUAL("手动选卦"),
    RANDOM("随机起卦"),
}

/**
 * 体用五行关系；断语照录《梅花易数》原书。
 * 吉：体克用、用生体、比和；凶：体生用、用克体。
 */
enum class TiYongRelation(
    val label: String,
    val verdict: String,
    val auspicious: Boolean,
) {
    TI_KE_YONG("体克用", "体克用，诸事吉", true),
    YONG_SHENG_TI("用生体", "用生体，有进益之喜", true),
    BI_HE("体用比和", "体用比和，则百事顺遂", true),
    TI_SHENG_YONG("体生用", "体生用，有耗失之患", false),
    YONG_KE_TI("用克体", "用克体，诸事凶", false),
}

/** 月令下五行旺相休囚死。 */
enum class WangShuai(val label: String) {
    WANG("旺"),
    XIANG("相"),
    XIU("休"),
    QIU("囚"),
    SI("死"),
}

/** 取数过程一步，供盘面摊开核对。 */
data class TraceLine(
    val title: String,
    val detail: String,
)

/**
 * 起卦取数痕迹：每个加数、和、余数与口径说明。
 * 四种起卦都填 [lines]，口径条文固定写在 [conventions]。
 */
data class CastTrace(
    val method: CastMethod,
    val lines: List<TraceLine>,
    val summary: String,
    val conventions: List<String> = DEFAULT_CONVENTIONS,
)

val DEFAULT_CONVENTIONS: List<String> = listOf(
    "先天八卦数：乾1兑2离3震4巽5坎6艮7坤8；余0取满数",
    "年首：农历正月初一换年（非立春）",
    "闰月：按本月序数计",
    "时辰：23:00起子；年/时用地支序子=1…亥=12",
    "数字起卦：两数定上下卦，两数之和定动爻（不加时辰）",
    "体用：动爻所在经卦为用，另一为体",
    "乾坤无互：互出仍为本卦时，改取变卦之互",
)

/** 农历时间戳（时间起卦 / 端法用时）。 */
data class LunarStamp(
    /** 年支序，子=1…亥=12 */
    val yearBranchNum: Int,
    val month: Int,
    val day: Int,
    /** 时支序，子=1…亥=12 */
    val hourBranchNum: Int,
    val isLeapMonth: Boolean = false,
    val yearBranchLabel: String = "",
    val hourBranchLabel: String = "",
    val solarText: String = "",
) {
    val lunarText: String
        get() = buildString {
            if (yearBranchLabel.isNotEmpty()) append(yearBranchLabel).append("年")
            if (isLeapMonth) append("闰")
            append(month).append("月")
            append(day).append("日")
            if (hourBranchLabel.isNotEmpty()) append(hourBranchLabel).append("时")
            else append("时序").append(hourBranchNum)
        }
}

/** 八卦说卦取象条目。 */
data class XiangEntry(
    val trigram: Trigram,
    val nature: String,
    val images: List<String>,
)

/**
 * 梅花盘：本/互/变 + 体用 + 旺衰 + 取象 + 经文。
 * [movingYao] 初=1…上=6。
 */
data class MeiHuaChart(
    val method: CastMethod,
    val upper: Trigram,
    val lower: Trigram,
    val movingYao: Int,
    val ben: HexagramEntry,
    val hu: HexagramEntry,
    val bian: HexagramEntry,
    val huFromBian: Boolean,
    val ti: Trigram,
    val yong: Trigram,
    val tiIsUpper: Boolean,
    val relation: TiYongRelation,
    val bianYong: Trigram,
    val bianRelation: TiYongRelation,
    val changeSummary: String,
    val tiHu: Trigram,
    val yongHu: Trigram,
    val tiWang: WangShuai,
    val yongWang: WangShuai,
    val huWang: WangShuai,
    val bianWang: WangShuai,
    val lunarMonth: Int?,
    val chengGuaShu: Int?,
    val yingQiDays: Double?,
    val posture: Posture?,
    val stamp: LunarStamp?,
    val trace: CastTrace,
    val tiXiang: XiangEntry,
    val yongXiang: XiangEntry,
    val benText: ZhouyiText,
    val bianText: ZhouyiText,
    val question: String = "",
) {
    val movingYaoName: String
        get() = when (movingYao) {
            1 -> "初爻"
            2 -> "二爻"
            3 -> "三爻"
            4 -> "四爻"
            5 -> "五爻"
            6 -> "上爻"
            else -> "${movingYao}爻"
        }
}

fun tiYongRelationOf(ti: Trigram, yong: Trigram): TiYongRelation {
    val a = ti.element
    val b = yong.element
    return when {
        a == b -> TiYongRelation.BI_HE
        a controls b -> TiYongRelation.TI_KE_YONG
        b generates a -> TiYongRelation.YONG_SHENG_TI
        a generates b -> TiYongRelation.TI_SHENG_YONG
        b controls a -> TiYongRelation.YONG_KE_TI
        else -> TiYongRelation.BI_HE
    }
}

/**
 * 月令主气：正二月木、四五火、七八金、十十一水，三四六九十二之土月（辰戌丑未）土旺。
 * 农历月 1..12。
 */
fun seasonElementOf(lunarMonth: Int): Element {
    require(lunarMonth in 1..12)
    return when (lunarMonth) {
        3, 6, 9, 12 -> Element.EARTH
        1, 2 -> Element.WOOD
        4, 5 -> Element.FIRE
        7, 8 -> Element.METAL
        10, 11 -> Element.WATER
        else -> error("invalid month $lunarMonth")
    }
}

/** 相对月令主气的旺相休囚死。 */
fun wangShuaiOf(element: Element, season: Element): WangShuai = when {
    element == season -> WangShuai.WANG
    element generates season -> WangShuai.XIANG
    season generates element -> WangShuai.XIU
    element controls season -> WangShuai.QIU
    season controls element -> WangShuai.SI
    else -> WangShuai.XIU
}

fun changeSummaryOf(before: TiYongRelation, after: TiYongRelation): String = when {
    before.auspicious && !after.auspicious -> "用吉变凶，先吉后凶"
    !before.auspicious && after.auspicious -> "用凶变吉，先凶后吉"
    before.auspicious && after.auspicious -> "用变皆吉"
    else -> "用变皆凶"
}

/** 端法应期（日）：行取半、立取成数、坐加倍。 */
fun yingQiDaysOf(chengGuaShu: Int, posture: Posture): Double = when (posture) {
    Posture.WALKING -> chengGuaShu / 2.0
    Posture.STANDING -> chengGuaShu.toDouble()
    Posture.SITTING -> (chengGuaShu * 2).toDouble()
}
