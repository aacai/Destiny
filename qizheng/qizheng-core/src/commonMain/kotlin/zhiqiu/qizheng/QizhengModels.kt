package zhiqiu.qizheng

data class StarView(
    val key: String,
    val label: String,
    val longitude: Double,
    val speed: Double,
    val branch: String,
    val xiu: String,
    val xiuDegree: Double,
    val retro: Boolean,
)

data class PalaceView(
    val name: String,
    val branch: String,
)

data class AspectView(
    val a: String,
    val b: String,
    val kind: String,
    val orb: Double,
)

data class PatternView(
    val name: String,
    val hit: Boolean,
    val detail: String,
    /** 格局释义（点击查看） */
    val desc: String = "",
    /** true = 政余喜格，false = 政余忌格 */
    val auspicious: Boolean = true,
)

data class LimitSummary(
    val daXian: String,
    val taiSui: String,
    val xiaoXian: String,
    val yueXian: String,
    val shanMu: String,
    val dingXing: String,
)

data class QizhengChart(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val longitude: Double,
    val latitude: Double,
    val gender: String,
    val solarLabel: String,
    val lunarLabel: String,
    val termLabel: String,
    val baziLabel: String,
    /** 天干四字，如 丙丙乙癸 */
    val stemLine: String,
    /** 地支四字，如 午申亥未 */
    val branchLine: String,
    val nayin: String,
    val mingBranch: String,
    val mingBranchIndex: Int,
    /** 立命度黄经（命宫宫头 + 太阳宫内度），盘面立命红线指向此点 */
    val mingDuLon: Double,
    /** 盘心上：如 箕四立命 */
    val mingCenterTop: String,
    /** 盘心下：如 水度命 */
    val mingCenterBottom: String,
    val mingLabel: String,
    /** 洞微大限：各限起点公历年（命→貌→福…顺地支） */
    val daXianYears: List<Int>,
    /** 洞微各限年数（与 [daXianYears] 对齐） */
    val daXianSpans: List<Double>,
    /** 外侧神煞环（年支岁类），下标=地支索引 */
    val yearShenShaByBranch: List<List<String>>,
    /** 内侧神煞环（日干贵人类） */
    val dayShenShaByBranch: List<List<String>>,
    /** 兼容：年∪日 */
    val shenShaByBranch: List<List<String>>,
    val stars: List<StarView>,
    val palaces: List<PalaceView>,
    val limits: LimitSummary,
    val aspects: List<AspectView>,
    val patterns: List<PatternView>,
    /** 化曜：每星一栏（果老「天X」+ 诸星起例名；天官模式十神替换天X，见 [HuaYaoSchool]） */
    val huaYao: List<HuaYaoColumn>,
    val solidBranches: String,
    val emptyBranches: String,
    val note: String = "",
    /** 盘制显示名，如 回归今宿·黄道 */
    val panZhi: String = QizhengDefaults.PanZhiName,
    val panZhiDetail: String = QizhengDefaults.PanZhiDetail,
    // ------------------------------------------------ 多盘制支持
    /** 本盘所用盘制配置，供重绘 / 对比 / 持久化 */
    val config: QizhengConfig = PanZhiPresets.default,
    /** 本盘宿零点（岁差，度）。绘制宿环须与排盘用同一值，取代原先的全局状态 */
    val xiuZeroDeg: Double = 0.0,
    /** 黄道偏移（ayanamsa，度）；回归制为 0 */
    val zodiacOffset: Double = 0.0,
    /** 身宫地支；[QizhengConfig.useShenGong] 关闭时为 null */
    val shenBranch: String? = null,
    /** 星曜庙旺：星曜 key → 等级（庙/旺/乐/喜）；[DignityMode.OFF] 时为空 */
    val dignity: Map<String, String> = emptyMap(),
)

object QizhengDefaults {
    const val LonBeijing = 116.4074
    const val LatBeijing = 39.9042
    /** 首版固定盘制名（后续可切换） */
    const val PanZhiName = "回归今宿·黄道"
    const val PanZhiDetail = "热带黄道 · 今宿 · 果老立命 · 洞微大限 · 罗降交/计升交/孛远点/炁28周"
}
