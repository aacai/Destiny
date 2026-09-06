package zhiqiu.liuyao

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZhouyiTextsTest {

    @Test
    fun all64HexagramsHaveTexts() {
        HEXAGRAM_CATALOG.forEach { entry ->
            val t = zhouyiOf(entry)
            assertEquals(6, t.yaoCi.size, entry.fullName)
            assertTrue(t.guaCi.isNotBlank(), entry.fullName)
            assertTrue(t.yaoCi.all { it.isNotBlank() }, entry.fullName)
        }
    }

    @Test
    fun qianKunSpotCheck() {
        val qian = zhouyiOf(hexagramOf(listOf(1, 1, 1, 1, 1, 1)))
        assertEquals("元亨利贞。", qian.guaCi)
        assertEquals("潜龙，勿用。", qian.yaoCi[0])
        assertEquals("见龙在田，利见大人。", qian.yaoCi[1])
        assertEquals("亢龙有悔。", qian.yaoCi[5])
        assertEquals("见群龙无首，吉。", qian.yongCi)
        val kun = zhouyiOf(hexagramOf(listOf(0, 0, 0, 0, 0, 0)))
        assertTrue(kun.guaCi.startsWith("元亨，利牝马之贞"))
        assertEquals("利永贞。", kun.yongCi)
        val tai = zhouyiOf(HEXAGRAM_CATALOG.first { it.fullName == "地天泰" })
        assertEquals("小往大来，吉，亨。", tai.guaCi)
        assertNull(tai.yongCi)
    }

    @Test
    fun yaoNames() {
        assertEquals("初九", yaoName(1, true))
        assertEquals("初六", yaoName(1, false))
        assertEquals("九五", yaoName(5, true))
        assertEquals("上六", yaoName(6, false))
        assertEquals("六三", yaoName(3, false))
    }

    @Test
    fun translations_coverAll64() {
        HEXAGRAM_CATALOG.forEach { entry ->
            val t = zhouyiTranslationOf(entry)
            assertEquals(6, t.yaoYi.size, entry.fullName)
            assertTrue(t.guaYi.isNotBlank(), entry.fullName)
            assertTrue(t.yaoYi.all { it.isNotBlank() }, entry.fullName)
        }
        val qian = zhouyiTranslationOf(hexagramOf(listOf(1, 1, 1, 1, 1, 1)))
        assertTrue(qian.yaoYi[0].contains("潜藏"), qian.yaoYi[0])
        val tai = zhouyiTranslationOf(HEXAGRAM_CATALOG.first { it.fullName == "地天泰" })
        assertTrue(tai.guaYi.isNotBlank())
    }

    @Test
    fun selectionDisplay() {
        assertEquals("通用", ZhanSelection().display)
        assertEquals("财运生意·求财", ZhanSelection(ZhanShi.WEALTH, "求财").display)
        assertEquals(6, ZhanShi.WEALTH.subs.size)
    }
}
