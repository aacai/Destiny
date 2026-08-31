package zhiqiu.qizheng

import kotlin.test.Test

/**
 * 交叉核对数据导出：三个核对生辰的本引擎基准值（回归今宿·果老盘制）。
 *
 * 输出 /tmp/qizheng-crosscheck.txt，供与在线盘（真天象 VSOP87D / 天官手记 Swiss Ephemeris）
 * 逐星比对使用。核对表模板见 docs/qizheng-crosscheck.md。
 */
class CrossCheckDumpTest {

    private data class Sample(val title: String, val y: Int, val m: Int, val d: Int, val hh: Int, val mm: Int, val gender: String)

    private val samples = listOf(
        Sample("样例A 示例默认 2026-08-29 14:05 男", 2026, 8, 29, 14, 5, "男"),
        Sample("样例B 2004-01-21 22:42 女", 2004, 1, 21, 22, 42, "女"),
        Sample("样例C 1993-06-01 12:20 男", 1993, 6, 1, 12, 20, "男"),
    )

    @Test
    fun dump_crosscheck_baseline() {
        val out = StringBuilder()
        out.appendLine("七政四余交叉核对基准值（本引擎，盘制 = 果老星宗 = 黄道回归 + 今宿）")
        out.appendLine("生成时间：见文件 mtime；黄经 = 回归黄道经度（度）；宿度 = 今度")
        out.appendLine()
        for (s in samples) {
            val c = QizhengBuilder.build(
                year = s.y, month = s.m, day = s.d, hour = s.hh, minute = s.mm, gender = s.gender,
                config = PanZhiPresets.GuoLao,
            )
            val moon = c.stars.first { it.key == "月" }
            out.appendLine("=== ${s.title} ===")
            out.appendLine("四柱: ${c.baziLabel}  (${c.stemLine} ${c.branchLine})  ${c.nayin}")
            out.appendLine("命宫: ${c.mingBranch}宫  立命: ${c.mingCenterTop}  ${c.mingCenterBottom}")
            out.appendLine("立命黄经: ${"%.2f".format(c.mingDuLon)}°  身宫(月亮所在): ${moon.branch}宫")
            out.appendLine("星曜  黄经      宫  宿+度          逆行")
            for (st in c.stars) {
                val retro = if (st.retro) "R" else ""
                out.appendLine(
                    "${st.key}    ${"%9.2f".format(st.longitude)}°  ${st.branch}  ${st.xiu}${"%5.1f".format(st.xiuDegree)}  $retro",
                )
            }
            out.appendLine()
        }
        java.io.File("/tmp/qizheng-crosscheck.txt").writeText(out.toString())
        println(out.toString())

        // 调试：样例C（1993-06-01）的宿界与孛宿定位
        val dbg = StringBuilder()
        val zeroC = XiuTable.zeroDeg(Ephemeris.centuriesTt(AstroMath.julianDay(1993, 6, 1, 4.0)))
        dbg.appendLine("xiuZero(1993) = $zeroC  frame=MODERN")
        for (i in 0 until 28) {
            val s = XiuTable.startOf(i, zeroC)
            val w = XiuTable.widthAt(i, zeroC)
            dbg.appendLine("${XiuTable.names()[i]}  start=${"%.2f".format(s)}  width=${"%.2f".format(w)}")
        }
        dbg.appendLine("locate(355.39, MODERN读数) = ${XiuTable.locate(355.39, zeroC, XiuSystem.MODERN)}")
        dbg.appendLine("locate(355.39, ANCIENT读数) = ${XiuTable.locate(355.39, zeroC, XiuSystem.ANCIENT)}")
        java.io.File("/tmp/qizheng-crosscheck-dbg.txt").writeText(dbg.toString())
    }
}
