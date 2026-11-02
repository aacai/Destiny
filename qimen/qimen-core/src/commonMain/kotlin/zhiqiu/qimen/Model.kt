package zhiqiu.qimen

/** 盘式。转盘沿八宫环转，飞盘沿洛书飞。 */
enum class PlateKind { Zhuan, Fei }

/**
 * 日柱日界。
 * [Hour23] 子初换日，23:00 起日柱进一日。
 * [Midnight] 子正换日，00:00 起日柱进一日，23 点仍用当日日柱。
 */
enum class DayBoundary { Hour23, Midnight }

/** 定局法。第一期只实现拆补，参数留着以免以后把局法写死在调用点。 */
enum class JuMethod { ChaiBu }

enum class Dun { Yang, Yin }

enum class Yuan { Shang, Zhong, Xia }

enum class Element {
    Wood, Fire, Earth, Metal, Water;

    /** 五行相克：木克土、土克水、水克火、火克金、金克木。 */
    fun controls(other: Element): Boolean = when (this) {
        Wood -> other == Earth
        Earth -> other == Water
        Water -> other == Fire
        Fire -> other == Metal
        Metal -> other == Wood
    }
}

enum class Stem(val label: String) {
    Jia("甲"), Yi("乙"), Bing("丙"), Ding("丁"), Wu("戊"),
    Ji("己"), Geng("庚"), Xin("辛"), Ren("壬"), Gui("癸");

    val sanQi: Boolean get() = this == Yi || this == Bing || this == Ding
}

enum class Branch(val label: String) {
    Zi("子"), Chou("丑"), Yin("寅"), Mao("卯"), Chen("辰"), Si("巳"),
    Wu("午"), Wei("未"), Shen("申"), You("酉"), Xu("戌"), Hai("亥"),
}

/**
 * 九宫。序号即洛书数，枚举顺序与洛书 1..9 一致，定长盘用 ordinal 取宫。
 * 必须写在门、星之前：它们的本宫在初始化时就要拿到宫。
 */
enum class Palace(
    val number: Int,
    val shortName: String,
    val direction: String,
    val element: Element,
    val branches: List<Branch>,
) {
    Kan(1, "坎一", "正北", Element.Water, listOf(Branch.Zi)),
    Kun(2, "坤二", "西南", Element.Earth, listOf(Branch.Wei, Branch.Shen)),
    Zhen(3, "震三", "正东", Element.Wood, listOf(Branch.Mao)),
    Xun(4, "巽四", "东南", Element.Wood, listOf(Branch.Chen, Branch.Si)),
    Zhong(5, "中五", "中宫", Element.Earth, emptyList()),
    Qian(6, "乾六", "西北", Element.Metal, listOf(Branch.Xu, Branch.Hai)),
    Dui(7, "兑七", "正西", Element.Metal, listOf(Branch.You)),
    Gen(8, "艮八", "东北", Element.Earth, listOf(Branch.Chou, Branch.Yin)),
    Li(9, "离九", "正南", Element.Fire, listOf(Branch.Wu)),
    ;

    companion object {
        fun byNumber(number: Int): Palace = entries[number - 1]
    }
}

enum class DoorTone { Lucky, Neutral, Unlucky }

/**
 * 八门本宫。飞盘顺序是本宫序跳过中五：休死伤杜开惊生景。
 */
enum class Door(
    val label: String,
    val home: Palace,
    val element: Element,
    val tone: DoorTone,
) {
    Xiu("休", Palace.Kan, Element.Water, DoorTone.Lucky),
    Si("死", Palace.Kun, Element.Earth, DoorTone.Unlucky),
    Shang("伤", Palace.Zhen, Element.Wood, DoorTone.Unlucky),
    Du("杜", Palace.Xun, Element.Wood, DoorTone.Neutral),
    Kai("开", Palace.Qian, Element.Metal, DoorTone.Lucky),
    Shock("惊", Palace.Dui, Element.Metal, DoorTone.Unlucky),
    Sheng("生", Palace.Gen, Element.Earth, DoorTone.Lucky),
    Scene("景", Palace.Li, Element.Fire, DoorTone.Neutral),
    ;

    val full: String get() = label + "门"
}

/**
 * 九星本宫。飞盘顺序即枚举顺序：蓬芮冲辅禽心柱任英。
 */
enum class Star(val label: String, val home: Palace) {
    Peng("天蓬", Palace.Kan),
    Rui("天芮", Palace.Kun),
    Chong("天冲", Palace.Zhen),
    Fu("天辅", Palace.Xun),
    Qin("天禽", Palace.Zhong),
    Xin("天心", Palace.Qian),
    Zhu("天柱", Palace.Dui),
    Ren("天任", Palace.Gen),
    Ying("天英", Palace.Li),
}

/**
 * 八神。阴遁不改次序，只把白虎、玄武换成勾陈、朱雀，再逆布。
 */
enum class Deity(val yangLabel: String, val yinLabel: String) {
    ZhiFu("值符", "值符"),
    TengShe("腾蛇", "腾蛇"),
    TaiYin("太阴", "太阴"),
    LiuHe("六合", "六合"),
    BaiHu("白虎", "勾陈"),
    XuanWu("玄武", "朱雀"),
    JiuDi("九地", "九地"),
    JiuTian("九天", "九天"),
    ;

    fun label(yang: Boolean): String = if (yang) yangLabel else yinLabel
}

enum class Mark(val label: String) {
    Kong("空"), Ma("马"), Mu("墓"), Xing("刑"), Po("迫"),
}

data class Pillar(val stem: Stem, val branch: Branch) {
    val label: String get() = stem.label + branch.label

    init {
        require((stem.ordinal - branch.ordinal) % 2 == 0) {
            "干支不同阴阳：${stem.label}${branch.label}"
        }
    }
}

data class PalaceCell(
    val palace: Palace,
    val deity: Deity?,
    val stars: List<Star>,
    val door: Door?,
    val heavenStems: List<Stem>,
    val earthStems: List<Stem>,
    val marks: List<Mark>,
)

/** 不含历法的盘面，测试直接喂局数和时干支。 */
data class PlateView(
    val ju: Int,
    val yang: Boolean,
    val plate: PlateKind,
    /** 寄宫之前的九宫地盘干，下标是宫的 ordinal。 */
    val earth: List<Stem>,
    val cells: List<PalaceCell>,
    val zhiFu: Star,
    val zhiShi: Door,
    val centerEarth: Stem,
    val xunLabel: String,
    val dunStem: Stem,
    val kong: List<Branch>,
    val horse: Branch,
) {
    init {
        require(earth.size == 9 && cells.size == 9)
    }

    fun cell(palace: Palace): PalaceCell = cells[palace.ordinal]
}

data class QimenChart(
    val solarText: String,
    val pillars: List<Pillar>,
    val termName: String,
    val yuan: Yuan,
    val dun: Dun,
    val ju: Int,
    val plate: PlateKind,
    val boundary: DayBoundary,
    val xunLabel: String,
    val dunStem: Stem,
    val kong: List<Branch>,
    val horse: Branch,
    val zhiFu: Star,
    val zhiShi: Door,
    val centerEarth: Stem,
    val cells: List<PalaceCell>,
) {
    init {
        require(pillars.size == 4 && cells.size == 9)
    }

    val yang: Boolean get() = dun == Dun.Yang

    val pillarText: String get() = pillars.joinToString(" ") { it.label }

    val kongText: String get() = kong.joinToString("") { it.label }

    val dunText: String get() = (if (yang) "阳遁" else "阴遁") + JU_LABELS[ju - 1] + "局"

    val yuanText: String
        get() = when (yuan) {
            Yuan.Shang -> "上元"
            Yuan.Zhong -> "中元"
            Yuan.Xia -> "下元"
        }

    fun cell(palace: Palace): PalaceCell = cells[palace.ordinal]
}

private val JU_LABELS = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九")
