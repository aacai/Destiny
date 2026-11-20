package zhiqiu.qizheng

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XingGeTest {
    @Test
    fun luoJiZhongFen_onlyWhenRahuInWu() {
        val base = QizhengBuilder.build(2026, 8, 29, 14, 5, gender = "男")
        val names = base.patterns.map { it.name }.toSet()
        // 罗计中分不得无条件恒真：仅罗居午才成
        val rah = base.stars.first { it.key == "罗" }
        if (rah.branch == "午") {
            assertTrue("罗计中分" in names)
        } else {
            assertFalse("罗计中分" in names, "罗在${rah.branch}不应成罗计中分，实际=$names")
        }
    }

    @Test
    fun patternsNonEmptyOnGoldenSample() {
        val c = QizhengBuilder.build(2026, 8, 29, 14, 5, gender = "男")
        val xi = c.patterns.filter { it.auspicious }
        val ji = c.patterns.filter { !it.auspicious }
        assertTrue(c.patterns.isNotEmpty(), "星格应有命中条目")
        println("喜=${xi.map { it.name }}")
        println("忌=${ji.map { it.name }}")
    }
}
