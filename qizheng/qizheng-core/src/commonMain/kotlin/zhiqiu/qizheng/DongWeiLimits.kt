package zhiqiu.qizheng

/**
 * 洞微大限（百六限）：从命宫起，顺地支（子丑寅…）转一圈。
 * 歌诀：命宫十五貌宫十，福德妻宫十一详。官禄十五星高位，迁移止有八年粮。
 * 疾厄七兮…财帛兄弟五年强。田宅子孙并奴仆，四年之半定毫芒。
 * 命宫年限随太阳在宫内度数变：约 10 + ⌊宫内度/3⌋（约 10–20）。
 *
 * @see <a href="https://www.jianshu.com/p/8fd643aaae92">洞微大限排法</a>
 */
object DongWeiLimits {

    data class Segment(
        /** 十二宫名索引：0命 1财 … 11貌 */
        val palaceIndex: Int,
        val branchIndex: Int,
        val years: Double,
        /** 该限起始虚岁（出生年虚岁=1 对应 calendarYear） */
        val startAge: Double,
        val startYear: Int,
    )

    /**
     * 顺行十二限：命→貌→福→官→迁→疾→妻→奴→男→田→兄→财
     * 对应 palaceIndex：0,11,10,9,8,7,6,5,4,3,2,1
     */
    private val PalaceOrder = intArrayOf(0, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)

    private val FixedYears = doubleArrayOf(
        /*命占位*/ 0.0, 10.0, 11.0, 15.0, 8.0, 7.0, 11.0, 4.5, 4.5, 4.5, 5.0, 5.0,
    )

    /** 太阳黄经 → 命宫行限年数 */
    fun mingYears(sunLongitude: Double): Double {
        val inSign = AstroMath.norm360(sunLongitude) % 30.0
        return 10.0 + kotlin.math.floor(inSign / 3.0)
    }

    fun segments(
        mingBranchIndex: Int,
        sunLongitude: Double,
        birthYear: Int,
    ): List<Segment> {
        val mingY = mingYears(sunLongitude)
        var age = 1.0 // 虚岁起限
        var year = birthYear.toDouble()
        return PalaceOrder.mapIndexed { step, palaceIndex ->
            val years = if (step == 0) mingY else FixedYears[step]
            val bi = (mingBranchIndex + step) % 12 // 顺地支
            val seg = Segment(
                palaceIndex = palaceIndex,
                branchIndex = bi,
                years = years,
                startAge = age,
                startYear = roundHalfUp(year),
            )
            age += years
            year += years
            seg
        }
    }

    private fun roundHalfUp(x: Double): Int = kotlin.math.floor(x + 0.5).toInt()

    /** 外圈年份标：每限起点年（十二个） */
    fun startYears(segments: List<Segment>): List<Int> = segments.map { it.startYear }

    /** 总年限（约 106） */
    fun totalYears(segments: List<Segment>): Double = segments.sumOf { it.years }
}
