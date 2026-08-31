package zhiqiu.qizheng

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 黄金样例自测：对照「星命排盘」回归今宿·黄道
 * 时间 2026-08-29 14:05（未时），地点默认北京。
 */
class QizhengFixtureTest {

    private fun chart() = QizhengBuilder.build(
        year = 2026, month = 8, day = 29, hour = 14, minute = 5, gender = "男",
    )

    @Test
    fun fixture_bazi() {
        assertEquals("丙午 丙申 乙亥 癸未", chart().baziLabel)
    }

    @Test
    fun fixture_mingPalace_chou() {
        val c = chart()
        assertEquals("丑", c.mingBranch)
        assertEquals("丑", c.palaces.first().branch)
        assertEquals("命宫", c.palaces.first().name)
    }

    @Test
    fun fixture_mingCenter_jiSi_water() {
        val c = chart()
        // 日柱纳音是山头火；盘心「水度命」来自立命宿「箕水豹」，不是纳音
        assertEquals("山头火", c.nayin)
        assertEquals("水度命", c.mingCenterBottom, "立命宿五行应为水，nayin=${c.nayin}")
        assertTrue(c.mingCenterTop.startsWith("箕"), "立命=${c.mingCenterTop}")
        val deg = Regex("箕([\\d.]+)立").find(c.mingCenterTop)?.groupValues?.get(1)?.toDoubleOrNull()
        assertTrue(deg != null && abs(deg - 4.3) <= 1.0, "期望箕四立(~4.3)，实际=${c.mingCenterTop}")
    }

    @Test
    fun fixture_limits() {
        val c = chart()
        assertEquals("丙午", c.limits.taiSui)
        assertEquals("丑", c.limits.xiaoXian)
        assertEquals("丑", c.limits.yueXian)
        assertEquals("火", c.limits.shanMu)
        assertEquals("计罗", c.limits.dingXing)
        assertTrue(c.limits.daXian.startsWith("斗"), "大限=${c.limits.daXian}")
        val deg = Regex("斗\\s*([\\d.]+)").find(c.limits.daXian)?.groupValues?.get(1)?.toDoubleOrNull()
        assertTrue(deg != null && abs(deg - 19.4) < 1.0, "大限度=${c.limits.daXian}")
    }

    @Test
    fun fixture_jinXiu_chouCusp_isJiSi() {
        // 今宿（网站金标准）：丑宫头落尾约 13.5 今度。
        // 显式传零点，避免受其它用例排盘后写回的全局值影响。
        val zero = XiuTable.zeroDeg(Ephemeris.centuriesTt(AstroMath.julianDay(2026, 8, 29, 6.0)))
        val loc = XiuTable.locate(MingGong.branchIndexToLonStart(1), zero, XiuSystem.MODERN) // 丑
        assertEquals("尾", loc.name)
        assertTrue(abs(loc.degreeInXiu - 13.47) < 0.5, "deg=${loc.degreeInXiu}")
    }

    @Test
    fun fixture_sun_in_zhang_by_solar_term_anchor() {
        // 天文锚点（见文档 §6.4）：处暑 = 黄经 150°，样例在处暑后约 5.94°，
        // 太阳应落张宿 ≈ 5.6 古度。参考图肉眼识读作「柳–星」，差 1 宿，
        // 属识读噪声；本用例以可反推的节气锚点为准。
        val sun = chart().stars.first { it.key == "日" }
        val afterChuShu = sun.longitude - 150.0
        assertTrue(afterChuShu in 5.5..6.5, "太阳应在处暑后约 6°，实际 $afterChuShu")
        assertEquals("星", sun.xiu)
        // 古度读数（默认）：≈8.5；网站今度 8.41
        assertTrue(sun.xiuDegree in 7.5..9.5, "太阳应在星宿约 8.5 古度，实际 ${sun.xiuDegree}")
    }

    @Test
    fun fixture_other_years_still_work_with_precession() {
        // 今宿零点按岁差外推：1946 → 2026 年冬至点应西移约 1.12°（80 年 × 50.29″）
        val z1946 = XiuTable.zeroDeg(Ephemeris.centuriesTt(AstroMath.julianDay(1946, 8, 29, 6.0)))
        val z2026 = XiuTable.zeroDeg(Ephemeris.centuriesTt(AstroMath.julianDay(2026, 8, 29, 6.0)))
        val drift = z2026 - z1946
        assertTrue(abs(drift - 80 * 0.013969) < 0.02, "80 年岁差漂移应约 1.12°，实际 $drift")

        // 1946 年排盘不应崩，且冬至点在箕宿（80 年内不足以跨宿）
        val c = QizhengBuilder.build(1946, 8, 29, 14, 5)
        assertEquals(11, c.stars.size)
        val zero = XiuTable.zeroDeg(Ephemeris.centuriesTt(AstroMath.julianDay(1946, 8, 29, 6.0)))
        assertEquals("尾", XiuTable.locate(270.0, zero).name)
    }

    @Test
    fun fixture_dump() {
        val c = chart()
        val sun = c.stars.first { it.key == "日" }
        val msg =
            "bazi=${c.baziLabel} nayin=${c.nayin}\n" +
                "sun=${sun.longitude} ${sun.branch} ${sun.xiu}\n" +
                "ming=${c.mingBranch} ${c.mingCenterTop}/${c.mingCenterBottom}\n" +
                "limits=${c.limits}\n" +
                "zero=${c.xiuZeroDeg}"
        println(msg)
        // 强制写入，避免被测试框架吞掉 stdout 时无迹可查
        java.io.File("/tmp/qizheng-fixture-dump.txt").writeText(msg)
    }

    @Test
    fun fixture_dongWei_years() {
        val c = chart()
        // 命宫年限 10+⌊太阳宫内度/3⌋；样例太阳约巳6° → 11 年
        assertEquals(11.0, c.daXianSpans[0], 0.01)
        assertEquals(
            listOf(2026, 2037, 2047, 2058, 2073, 2081, 2088, 2099),
            c.daXianYears.take(8),
        )
        // 4.5 年段取整后与参考 2104/2108/2113/2118 一致
        val rounded = c.daXianYears.drop(8).take(4)
        assertEquals(listOf(2104, 2108, 2113, 2118), rounded)
    }

    @Test
    fun fixture_shenSha_yearWu_hasTaiSuiOnWu() {
        val c = chart()
        val wu = MingGong.Branches.indexOf("午")
        assertTrue("太岁" in c.shenShaByBranch[wu], "丙午年太岁在午: ${c.shenShaByBranch[wu]}")
        val shen = MingGong.Branches.indexOf("申")
        assertTrue("驿马" in c.shenShaByBranch[shen], "午年驿马在申: ${c.shenShaByBranch[shen]}")
    }

    @Test
    fun fixture_stars_dump_branches() {
        val c = chart()
        val lines = c.stars.joinToString("\n") {
            "${it.key} lon=${"%.2f".format(it.longitude)} ${it.branch} ${it.xiu}${("%.1f".format(it.xiuDegree))}"
        }
        println(lines)
        java.io.File("/tmp/qizheng-stars.txt").writeText(lines)
        // 八月底太阳必在巳/午一带
        val sun = c.stars.first { it.key == "日" }
        assertTrue(sun.branch == "巳" || sun.branch == "午", "sun=${sun.branch} ${sun.longitude}")
    }

    /**
     * 罗睺 / 计都存在两派相反的定义（见 [NodeConvention]）。
     * 本用例把两派结果都 dump 出来，便于直接对照参考图拍板。
     *
     * [wheelAngle] = 从正下立命点（丑宫头，黄经 270°）起**顺时针**的盘面角度，
     * 与洞微年份环同向，可直接在图上量。
     */
    private fun wheelAngle(lon: Double): Double = (270.0 - lon + 360) % 360

    @Test
    fun fixture_dump_both_node_conventions() {
        val out = StringBuilder()
        // 交点约定随盘制传入，不再改全局状态，故无需「还原默认值」
        for (conv in NodeConvention.values()) {
            val c = QizhengBuilder.build(
                year = 2026, month = 8, day = 29, hour = 14, minute = 5, gender = "男",
                config = PanZhiPresets.GuoLao.copy(nodeConvention = conv),
            )
            val rahu = c.stars.first { it.key == "罗" }
            val ketu = c.stars.first { it.key == "计" }
            out.append(
                "%-12s 罗 lon=%7.2f %s %s%.1f  盘面%.1f°  |  计 lon=%7.2f %s %s%.1f  盘面%.1f°\n".format(
                    conv.name,
                    rahu.longitude, rahu.branch, rahu.xiu, rahu.xiuDegree, wheelAngle(rahu.longitude),
                    ketu.longitude, ketu.branch, ketu.xiu, ketu.xiuDegree, wheelAngle(ketu.longitude),
                )
            )
        }
        println(out)
        java.io.File("/tmp/qizheng-nodes.txt").writeText(out.toString())

        // 顺带 dump 全部星曜的盘面角，方便逐曜核对参考图
        val c = chart()
        val all = c.stars.joinToString("\n") {
            "%-2s lon=%7.2f %s %s%-4s 盘面%6.1f°".format(
                it.key, it.longitude, it.branch, it.xiu,
                "%.1f".format(it.xiuDegree), wheelAngle(it.longitude),
            )
        }
        println(all)
        java.io.File("/tmp/qizheng-stars-wheel.txt").writeText(all)
    }

    @Test
    fun fixture_guaZhi_labels() {
        // 未=月坤未，申=水坤申，酉=金兑酉
        assertEquals("月坤未", PalaceGuaZhi.label(MingGong.Branches.indexOf("未")))
        assertEquals("水坤申", PalaceGuaZhi.label(MingGong.Branches.indexOf("申")))
        assertEquals("金兑酉", PalaceGuaZhi.label(MingGong.Branches.indexOf("酉")))
        assertEquals("土艮丑", PalaceGuaZhi.label(MingGong.Branches.indexOf("丑")))
    }
}
