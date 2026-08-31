package zhiqiu.qizheng

import java.io.File
import kotlin.test.Test

/**
 * 与权威在线排盘「星命排盘 V1.25」(https://xingmingpp.xyz/) 逐项核对。
 * 同一生辰：2026-08-29 14:05，男，默认北京。
 * 网站口径：回归今宿·黄道，默认真太阳时（界面显示「真太阳时14:05」）。
 *
 * 参考值（web）从网站 DOM 原文提取（黄经度 / 落宫 / 宿）：
 *   日 155.9397 巳 星 | 月 348.7667 亥 危 | 水 157.4572 巳 张
 *   金 200.9342 辰 轸 | 火 101.6467 未 井 | 木 133.1206 午 柳
 *   土 013.8303 戌 壁 | 计 329.8358 子 虚 | 罗 149.7547 午 星
 *   孛 268.0639 寅 尾 | 炁 172.1486 巳 张
 * 立命 丑5.94 箕4.30 | 安身 亥18.77 危15.03
 */
class CompareXingmingppTest {

    @Test
    fun compare_to_xingmingpp() {
        val out = StringBuilder()
        // 网站参考: 黄经(度), 落宫, 宿
        val ref = mapOf(
            "日" to Triple(155.9397, "巳", "星"),
            "月" to Triple(348.7667, "亥", "危"),
            "水" to Triple(157.4572, "巳", "张"),
            "金" to Triple(200.9342, "辰", "轸"),
            "火" to Triple(101.6467, "未", "井"),
            "木" to Triple(133.1206, "午", "柳"),
            "土" to Triple(13.8303, "戌", "壁"),
            "计" to Triple(329.8358, "子", "虚"),
            "罗" to Triple(149.7547, "午", "星"),
            "孛" to Triple(268.0639, "寅", "尾"),
            "炁" to Triple(172.1486, "巳", "张"),
        )
        for ((cfg, label) in listOf(QizhengConfig() to "CIVIL", PanZhiPresets.TrueSolar to "TRUE_SOLAR")) {
            val c = QizhengBuilder.build(2026, 8, 29, 14, 5, "男", config = cfg)
            out.appendLine(
                "===== $label =====  bazi=${c.baziLabel} ming=${c.mingBranch} " +
                    "shen=${c.shenBranch} zero=${"%.4f".format(c.xiuZeroDeg)}"
            )
            ref.forEach { (k, t) ->
                val (wl, wb, wx) = t
                val s = c.stars.firstOrNull { it.key == k }
                if (s != null) {
                    val dLon = s.longitude - wl
                    val branchOk = if (s.branch == wb) "✓" else "✗"
                    val xiuOk = if (s.xiu == wx) "✓" else "✗"
                    out.appendLine(
                        "%-2s web lon=%8.4f %s %s | mine lon=%8.4f %s %s%s dLon=%+7.4f  %s%s".format(
                            k, wl, wb, wx, s.longitude, s.branch, s.xiu,
                            "%.2f".format(s.xiuDegree), dLon, branchOk, xiuOk
                        )
                    )
                } else {
                    out.appendLine("$k  —— 未找到")
                }
            }
            out.appendLine("ming=${c.mingBranch}(web 丑) shen=${c.shenBranch}(web 亥)")
            out.appendLine()
        }
        println(out)
        File("/tmp/compare-xingmingpp.txt").writeText(out.toString())
    }
}
