package zhiqiu.liuyao

/** 爻的四种形态：只有老阳/老阴为动爻。 */
enum class YaoKind(
    val label: String,
    val shortLabel: String,
    val isYang: Boolean,
    val isMoving: Boolean,
    /** 动爻标记：老阳 ○，老阴 × */
    val mark: String?,
) {
    LAO_YANG("老阳", "○", true, true, "○"),
    SHAO_YANG("少阳", "▬", true, false, null),
    SHAO_YIN("少阴", "▬▬", false, false, null),
    LAO_YIN("老阴", "×", false, true, "×"),
}

enum class Element(val label: String) {
    WOOD("木"),
    FIRE("火"),
    EARTH("土"),
    METAL("金"),
    WATER("水"),
}

infix fun Element.generates(other: Element): Boolean = when (this) {
    Element.WOOD -> other == Element.FIRE
    Element.FIRE -> other == Element.EARTH
    Element.EARTH -> other == Element.METAL
    Element.METAL -> other == Element.WATER
    Element.WATER -> other == Element.WOOD
}

infix fun Element.controls(other: Element): Boolean = when (this) {
    Element.WOOD -> other == Element.EARTH
    Element.EARTH -> other == Element.WATER
    Element.WATER -> other == Element.FIRE
    Element.FIRE -> other == Element.METAL
    Element.METAL -> other == Element.WOOD
}

enum class Stem(val label: String, val element: Element) {
    JIA("甲", Element.WOOD),
    YI("乙", Element.WOOD),
    BING("丙", Element.FIRE),
    DING("丁", Element.FIRE),
    WU("戊", Element.EARTH),
    JI("己", Element.EARTH),
    GENG("庚", Element.METAL),
    XIN("辛", Element.METAL),
    REN("壬", Element.WATER),
    GUI("癸", Element.WATER),
}

enum class Branch(val label: String, val element: Element) {
    ZI("子", Element.WATER),
    CHOU("丑", Element.EARTH),
    YIN("寅", Element.WOOD),
    MAO("卯", Element.WOOD),
    CHEN("辰", Element.EARTH),
    SI("巳", Element.FIRE),
    WU("午", Element.FIRE),
    WEI("未", Element.EARTH),
    SHEN("申", Element.METAL),
    YOU("酉", Element.METAL),
    XU("戌", Element.EARTH),
    HAI("亥", Element.WATER),
}

fun stemOf(label: String): Stem = Stem.entries.first { it.label == label }

fun branchOf(label: String): Branch = Branch.entries.first { it.label == label }

data class Ganzhi(val stem: Stem, val branch: Branch) {
    val label: String get() = "${stem.label}${branch.label}"
    val element: Element get() = branch.element
}

/**
 * 八卦：纳甲序列按「初二三 / 四五六」两组存放。
 * 乾内甲外壬、坤内乙外癸，其余上下同干。
 */
enum class Trigram(
    val label: String,
    val element: Element,
    /** 初→三爻阴阳，1 为阳 */
    val bits: IntArray,
    val lowerStems: Array<Stem>,
    val lowerBranches: Array<Branch>,
    val upperStems: Array<Stem>,
    val upperBranches: Array<Branch>,
) {
    QIAN(
        "乾", Element.METAL, intArrayOf(1, 1, 1),
        arrayOf(Stem.JIA, Stem.JIA, Stem.JIA),
        arrayOf(Branch.ZI, Branch.YIN, Branch.CHEN),
        arrayOf(Stem.REN, Stem.REN, Stem.REN),
        arrayOf(Branch.WU, Branch.SHEN, Branch.XU),
    ),
    DUI(
        "兑", Element.METAL, intArrayOf(1, 1, 0),
        arrayOf(Stem.DING, Stem.DING, Stem.DING),
        arrayOf(Branch.SI, Branch.MAO, Branch.CHOU),
        arrayOf(Stem.DING, Stem.DING, Stem.DING),
        arrayOf(Branch.HAI, Branch.YOU, Branch.WEI),
    ),
    LI(
        "离", Element.FIRE, intArrayOf(1, 0, 1),
        arrayOf(Stem.JI, Stem.JI, Stem.JI),
        arrayOf(Branch.MAO, Branch.CHOU, Branch.HAI),
        arrayOf(Stem.JI, Stem.JI, Stem.JI),
        arrayOf(Branch.YOU, Branch.WEI, Branch.SI),
    ),
    ZHEN(
        "震", Element.WOOD, intArrayOf(1, 0, 0),
        arrayOf(Stem.GENG, Stem.GENG, Stem.GENG),
        arrayOf(Branch.ZI, Branch.YIN, Branch.CHEN),
        arrayOf(Stem.GENG, Stem.GENG, Stem.GENG),
        arrayOf(Branch.WU, Branch.SHEN, Branch.XU),
    ),
    XUN(
        "巽", Element.WOOD, intArrayOf(0, 1, 1),
        arrayOf(Stem.XIN, Stem.XIN, Stem.XIN),
        arrayOf(Branch.CHOU, Branch.HAI, Branch.YOU),
        arrayOf(Stem.XIN, Stem.XIN, Stem.XIN),
        arrayOf(Branch.WEI, Branch.SI, Branch.MAO),
    ),
    KAN(
        "坎", Element.WATER, intArrayOf(0, 1, 0),
        arrayOf(Stem.WU, Stem.WU, Stem.WU),
        arrayOf(Branch.YIN, Branch.CHEN, Branch.WU),
        arrayOf(Stem.WU, Stem.WU, Stem.WU),
        arrayOf(Branch.SHEN, Branch.XU, Branch.ZI),
    ),
    GEN(
        "艮", Element.EARTH, intArrayOf(0, 0, 1),
        arrayOf(Stem.BING, Stem.BING, Stem.BING),
        arrayOf(Branch.CHEN, Branch.WU, Branch.SHEN),
        arrayOf(Stem.BING, Stem.BING, Stem.BING),
        arrayOf(Branch.XU, Branch.ZI, Branch.YIN),
    ),
    KUN(
        "坤", Element.EARTH, intArrayOf(0, 0, 0),
        arrayOf(Stem.YI, Stem.YI, Stem.YI),
        arrayOf(Branch.WEI, Branch.SI, Branch.MAO),
        arrayOf(Stem.GUI, Stem.GUI, Stem.GUI),
        arrayOf(Branch.CHOU, Branch.HAI, Branch.YOU),
    ),
}

fun trigramOf(label: String): Trigram = Trigram.entries.first { it.label == label }

/** 宫位：八纯卦为宫主，宫五行统领全宫六亲。 */
enum class Palace(val label: String, val element: Element, val host: Trigram) {
    QIAN("乾", Element.METAL, Trigram.QIAN),
    DUI("兑", Element.METAL, Trigram.DUI),
    LI("离", Element.FIRE, Trigram.LI),
    ZHEN("震", Element.WOOD, Trigram.ZHEN),
    XUN("巽", Element.WOOD, Trigram.XUN),
    KAN("坎", Element.WATER, Trigram.KAN),
    GEN("艮", Element.EARTH, Trigram.GEN),
    KUN("坤", Element.EARTH, Trigram.KUN),
}

/** 卦在宫中的世系：决定世爻位置，应爻恒与世爻隔两位。 */
enum class HexagramType(val label: String, val shiPosition: Int) {
    BEN_GONG("本宫", 6),
    FIRST("一世", 1),
    SECOND("二世", 2),
    THIRD("三世", 3),
    FOURTH("四世", 4),
    FIFTH("五世", 5),
    YOU_HUN("游魂", 4),
    GUI_HUN("归魂", 3),
}

/** 六亲：以宫五行为「我」。 */
enum class SixKin(val label: String) {
    PARENT("父母"),
    SIBLING("兄弟"),
    CHILD("子孙"),
    WEALTH("妻财"),
    GHOST("官鬼"),
}

fun sixKinOf(palaceElement: Element, yaoElement: Element): SixKin = when {
    yaoElement == palaceElement -> SixKin.SIBLING
    yaoElement generates palaceElement -> SixKin.PARENT
    palaceElement generates yaoElement -> SixKin.CHILD
    palaceElement controls yaoElement -> SixKin.WEALTH
    else -> SixKin.GHOST
}

/** 六神：按占日天干起，初爻起首神向上轮排。 */
enum class SixGod(val label: String) {
    QING_LONG("青龙"),
    ZHU_QUE("朱雀"),
    GOU_CHEN("勾陈"),
    TENG_SHE("螣蛇"),
    BAI_HU("白虎"),
    XUAN_WU("玄武"),
}

fun firstGodOf(dayStem: Stem): SixGod = when (dayStem) {
    Stem.JIA, Stem.YI -> SixGod.QING_LONG
    Stem.BING, Stem.DING -> SixGod.ZHU_QUE
    Stem.WU -> SixGod.GOU_CHEN
    Stem.JI -> SixGod.TENG_SHE
    Stem.GENG, Stem.XIN -> SixGod.BAI_HU
    Stem.REN, Stem.GUI -> SixGod.XUAN_WU
}

fun godAt(dayStem: Stem, position: Int): SixGod {
    val all = SixGod.entries
    return all[(all.indexOf(firstGodOf(dayStem)) + (position - 1)) % all.size]
}

/** 爻位名：初九/初六 … 上九/上六。 */
fun yaoName(position: Int, isYang: Boolean): String {
    val place = when (position) {
        1 -> "初"
        6 -> "上"
        else -> when (position) {
            2 -> "二"
            3 -> "三"
            4 -> "四"
            else -> "五"
        }
    }
    val yinYang = if (isYang) "九" else "六"
    return if (position == 1 || position == 6) place + yinYang else yinYang + place
}

/** 爻位次名：初爻/二爻/三爻/四爻/五爻/上爻。 */
fun yaoPlaceName(position: Int): String = when (position) {
    1 -> "初爻"
    2 -> "二爻"
    3 -> "三爻"
    4 -> "四爻"
    5 -> "五爻"
    else -> "上爻"
}
