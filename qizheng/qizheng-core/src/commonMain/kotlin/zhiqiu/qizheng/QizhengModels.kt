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
    val motion: String = if (retro) "逆" else "顺",
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
    val desc: String = "",
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
    val stemLine: String,
    val branchLine: String,
    val nayin: String,
    val mingBranch: String,
    val mingBranchIndex: Int,
    val mingDuLon: Double,
    val mingCenterTop: String,
    val mingCenterBottom: String,
    val mingLabel: String,
    val daXianYears: List<Int>,
    val daXianSpans: List<Double>,
    val yearShenShaByBranch: List<List<String>>,
    val dayShenShaByBranch: List<List<String>>,
    val shenShaByBranch: List<List<String>>,
    val stars: List<StarView>,
    val palaces: List<PalaceView>,
    val limits: LimitSummary,
    val aspects: List<AspectView>,
    val patterns: List<PatternView>,
    val huaYao: List<HuaYaoColumn>,
    val solidBranches: String,
    val emptyBranches: String,
    val note: String = "",
    val panZhi: String = QizhengDefaults.PanZhiName,
    val panZhiDetail: String = QizhengDefaults.PanZhiDetail,
    val config: QizhengConfig = PanZhiPresets.default,
    val xiuZeroDeg: Double = 0.0,
    val zodiacOffset: Double = 0.0,
    val shenBranch: String? = null,
    val dignity: Map<String, String> = emptyMap(),
)

object QizhengDefaults {
    const val LonBeijing = 116.4074
    const val LatBeijing = 39.9042
    const val PanZhiName = "回归今宿·黄道"
    const val PanZhiDetail = "热带黄道 · 今宿 · 果老立命 · 洞微大限 · 罗降交/计升交/孛远点/炁28周"
}
