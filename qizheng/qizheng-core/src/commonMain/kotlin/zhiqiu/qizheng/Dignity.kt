package zhiqiu.qizheng

/**
 * 星曜庙旺利陷（亮度）。
 *
 * 🛑 **本表数据不可靠，已从预置盘制中撤下（[PanZhiPresets.all] 不再含「庙旺版」）。**
 *
 * 核对《果老星宗》宫名诗后，发现本表与原文**条条冲突**：
 *
 * | 星 | 本表(庙) | 果老星宗庙宫 | 星 | 本表(庙) | 果老星宗庙宫 |
 * |---|---|---|---|---|---|
 * | 日 | 午 | **戌** | 水 | 申 | **午** |
 * | 月 | 未 | **戌** | 木 | 寅 | **亥** |
 * | 金 | 酉 | **辰** | 计 | 戌 | **巳 / 亥** |
 * | 火 | 卯 | 卯 ✔ | 孛 | 未 | 未 ✔ |
 *
 * 更根本的问题是：七政四余实为「**庙 / 旺 / 乐 / 喜 / 好**」多档体系
 * （另有按二十八宿的「躔俦 / 升殿」），本表套用的「庙旺利陷」四档模型本身就不适配。
 *
 * 果老星宗宫名诗原文（据百度百科，含传抄讹误，罗既见寅又见午、计既见巳又见亥）：
 * - 庙宫：土丑罗寅火卯中，金在辰宫计在巳…日月戌方云入庙，计都木亥尽亨通
 * - 乐宫：土星子丑金辰酉，末属太阴午太阳。火星卯戌水巳申，木星…亥兼寅，罗睺午上计都子
 * - 喜宫：日寅月亥水辰清，土居午上木居末，火申罗戌便昌荣
 *
 * **重建前请勿启用。** 若要恢复，需先取得无讹误的善本口诀，
 * 并把模型从四档改为「庙旺乐喜好」多档。
 */
object Dignity {

    /** 亮度等级，由强到弱 */
    enum class Level(val label: String) {
        MIAO("庙"),
        WANG("旺"),
        LE("乐"),
        XI("喜"),
    }

    /**
     * 通行庙旺表：星曜 key → 地支 → 等级。
     * key 与 [StarView.key] 一致：日 月 水 金 火 木 土 罗 计 孛 炁。
     */
    val TABLE: Map<String, Map<String, Level>> = mapOf(
        "日" to mapOf("午" to Level.MIAO, "巳" to Level.WANG, "戌" to Level.LE, "寅" to Level.XI),
        "月" to mapOf("未" to Level.MIAO, "辰" to Level.WANG, "酉" to Level.LE, "亥" to Level.XI),
        "木" to mapOf("寅" to Level.MIAO, "亥" to Level.WANG, "未" to Level.LE, "卯" to Level.XI),
        "火" to mapOf("卯" to Level.MIAO, "寅" to Level.WANG, "戌" to Level.LE, "巳" to Level.XI),
        "土" to mapOf("子" to Level.MIAO, "丑" to Level.WANG, "辰" to Level.LE, "申" to Level.XI),
        "金" to mapOf("酉" to Level.MIAO, "申" to Level.WANG, "丑" to Level.LE, "辰" to Level.XI),
        "水" to mapOf("申" to Level.MIAO, "亥" to Level.WANG, "子" to Level.LE, "巳" to Level.XI),
        "炁" to mapOf("亥" to Level.MIAO, "卯" to Level.WANG, "寅" to Level.LE, "未" to Level.XI),
        "孛" to mapOf("未" to Level.MIAO, "申" to Level.WANG, "亥" to Level.LE, "辰" to Level.XI),
        "罗" to mapOf("卯" to Level.MIAO, "午" to Level.WANG, "寅" to Level.LE, "戌" to Level.XI),
        "计" to mapOf("戌" to Level.MIAO, "亥" to Level.WANG, "午" to Level.LE, "寅" to Level.XI),
    )

    /** 查某曜落在某地支时的亮度；无表项返回 null */
    fun levelOf(starKey: String, branch: String): Level? = TABLE[starKey]?.get(branch)

    /** 整盘亮度：星曜 key → 等级标签，只含命中的曜 */
    fun ofChart(stars: List<StarView>): Map<String, String> =
        stars.mapNotNull { s -> levelOf(s.key, s.branch)?.let { s.key to it.label } }.toMap()
}
