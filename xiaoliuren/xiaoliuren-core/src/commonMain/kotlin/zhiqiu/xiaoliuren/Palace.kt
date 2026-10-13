package zhiqiu.xiaoliuren

/** 六宫吉凶等级：三吉三凶。 */
enum class Luck(val label: String, val auspicious: Boolean) {
    GREAT_AUSPICIOUS("大吉", true),
    MEDIUM_AUSPICIOUS("中吉", true),
    SMALL_AUSPICIOUS("小吉", true),
    SMALL_MALEFIC("小凶", false),
    MEDIUM_MALEFIC("中凶", false),
    GREAT_MALEFIC("大凶", false),
}

/**
 * 小六壬六宫，掌诀顺序固定不变，左手三指顺时针布列。
 *
 * 五行取自各宫本属性，方位由五行推出（木东、火南、金西、水北、土中）；
 * `niMaster` 为倪海厦《天纪》口授断语，`poem` 为通行的《断曰》口诀。
 */
enum class Palace(
    val label: String,
    val wuxing: String,
    val direction: String,
    val beast: String,
    val state: String,
    val luck: Luck,
    val matterNumbers: String,
    val niMaster: String,
    val poem: List<String>,
    /** 左手掌诀上的指节位置：0 食指根 … 5 中指根 */
    val fingerSlot: Int,
) {
    DA_AN(
        label = "大安",
        wuxing = "木",
        direction = "东",
        beast = "青龙",
        state = "身未动时",
        luck = Luck.GREAT_AUSPICIOUS,
        matterNumbers = "一五七",
        niMaster = "事事安稳，宜守不宜动；约一个礼拜左右可见所要的结果。",
        poem = listOf(
            "大安事事昌，求财在坤方",
            "失物去不远，宅舍保安康",
            "行人身未动，病者主无妨",
            "将军回田野，仔细更推详",
        ),
        fingerSlot = 0,
    ),
    LIU_LIAN(
        label = "留连",
        wuxing = "水",
        direction = "北",
        beast = "玄武",
        state = "卒未归时",
        luck = Luck.SMALL_MALEFIC,
        matterNumbers = "二八十",
        niMaster = "事情停滞不前、纠缠难成；此行不利，宜缓。",
        poem = listOf(
            "留连事难成，求谋日未明",
            "官事只宜缓，去者未回程",
            "失物南方见，急讨方称心",
            "更须防口舌，人口且平平",
        ),
        fingerSlot = 1,
    ),
    SU_XI(
        label = "速喜",
        wuxing = "火",
        direction = "南",
        beast = "朱雀",
        state = "人便至时",
        luck = Luck.MEDIUM_AUSPICIOUS,
        matterNumbers = "三六九",
        niMaster = "喜信将至，立竿见影，马上就能看到所要的结果。",
        poem = listOf(
            "速喜喜来临，求财向南行",
            "失物申午未，逢人路上寻",
            "官事有福德，病者无祸侵",
            "田宅六畜吉，行人有信音",
        ),
        fingerSlot = 2,
    ),
    CHI_KOU(
        label = "赤口",
        wuxing = "金",
        direction = "西",
        beast = "白虎",
        state = "官事凶时",
        luck = Luck.MEDIUM_MALEFIC,
        matterNumbers = "四七十",
        niMaster = "主口舌官非，有第三者介入或人为是非干扰，宜防争端。",
        poem = listOf(
            "赤口主口舌，官非切要防",
            "失物速速讨，行人有惊慌",
            "六畜多作怪，病者出西方",
            "更须防咒诅，诚恐染瘟肓",
        ),
        fingerSlot = 3,
    ),
    XIAO_JI(
        label = "小吉",
        wuxing = "水",
        direction = "北",
        beast = "六合",
        state = "人来喜时",
        luck = Luck.SMALL_AUSPICIOUS,
        matterNumbers = "一五七",
        niMaster = "和合可成，有人从中助力；约两个礼拜后可见结果。",
        poem = listOf(
            "小吉最吉昌，路上好商量",
            "阴人来报喜，失物在坤方",
            "行人即便至，交关甚是强",
            "凡事皆和合，病者叩穹苍",
        ),
        fingerSlot = 4,
    ),
    KONG_WANG(
        label = "空亡",
        wuxing = "土",
        direction = "中",
        beast = "勾陈",
        state = "音信稀时",
        luck = Luck.GREAT_MALEFIC,
        matterNumbers = "三六九",
        niMaster = "大凶，诸事落空、虚耗灾殃；此事不宜，宜作罢或改期。",
        poem = listOf(
            "空亡事不祥，阴人多乖张",
            "求财无利益，行人有灾殃",
            "失物寻不见，官事有刑伤",
            "病人逢暗鬼，解禳保安康",
        ),
        fingerSlot = 5,
    ),
    ;

    companion object {
        /** 掌诀顺序（顺时针）：大安 → 留连 → 速喜 → 赤口 → 小吉 → 空亡 */
        val ORDER: List<Palace> = entries.toList()

        fun of(index: Int): Palace = ORDER[floorMod(index, ORDER_SIZE)]

        const val ORDER_SIZE = 6
    }
}

internal fun floorMod(value: Int, modulus: Int): Int = ((value % modulus) + modulus) % modulus

/** 地支序号：子=0 … 亥=11 */
val EARTHLY_BRANCHES = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

/** 时辰显示名，如「未时」。 */
fun branchName(index: Int): String = EARTHLY_BRANCHES[floorMod(index, 12)]

/** 各地支时辰对应的阳历小时区间，如「未 13–15」。 */
fun branchHourRange(index: Int): String {
    val start = (23 + floorMod(index, 12) * 2) % 24
    val end = (start + 2) % 24
    return "${start.toString().padStart(2, '0')}–${end.toString().padStart(2, '0')}"
}
