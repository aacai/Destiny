package zhiqiu.liuyao.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.liuyao.HexagramEntry
import zhiqiu.liuyao.HEXAGRAM_CATALOG
import zhiqiu.liuyao.PINYIN
import zhiqiu.liuyao.RARE_CHARS
import zhiqiu.liuyao.Trigram
import zhiqiu.liuyao.daXiangOf
import zhiqiu.liuyao.tuanOf
import zhiqiu.liuyao.wenyanOf
import zhiqiu.liuyao.xiaoXiangOf
import zhiqiu.liuyao.yongXiangOf
import zhiqiu.liuyao.yongYiOf
import zhiqiu.liuyao.wenyanYiOf
import zhiqiu.liuyao.zhouyiOf
import zhiqiu.liuyao.zhouyiTranslationOf

/**
 * 注音文本：正文正常排版，仅对 [RARE_CHARS] 中的难认字在其后加小号上标拼音。
 * 经文区、爻行与易经浏览页共用。
 */
@Composable
internal fun PinyinText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    modifier: Modifier = Modifier,
) {
    val pySize = (fontSize.value * 0.55f).sp
    Text(
        buildAnnotatedString {
            text.forEach { ch ->
                val s = ch.toString()
                if (s[0] in '一'..'鿿' && s in RARE_CHARS) {
                    append(s)
                    PINYIN[s]?.let { py ->
                        withStyle(
                            SpanStyle(
                                color = PanAccent,
                                fontSize = pySize,
                                baselineShift = BaselineShift.Superscript,
                            ),
                        ) { append(py) }
                    }
                } else {
                    append(s)
                }
            }
        },
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight,
        modifier = modifier,
    )
}

/** 易经浏览：64 卦目录 → 单卦全文（卦辞/大象/彖/爻辞/文言）。 */
@Composable
fun ZhouyiBrowserScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf<HexagramEntry?>(null) }
    var showPinyin by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(PanCardBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { if (selected != null) selected = null else onBack() }) {
                Text("‹ ${if (selected != null) "目录" else "返回"}", color = PanAccent, fontSize = 15.sp)
            }
            Text(
                selected?.fullName ?: "易经六十四卦",
                color = PanInk,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected != null) {
                Text("拼音注音", color = PanMuted, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Switch(checked = showPinyin, onCheckedChange = { showPinyin = it })
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = PanLine)
        val sel = selected
        if (sel == null) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(HEXAGRAM_CATALOG) { entry ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { selected = entry }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                entry.fullName,
                                color = PanInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Text("${entry.palace.label}宫", color = PanMuted, fontSize = 12.sp)
                        }
                        Text(
                            "卦辞：${zhouyiOf(entry).guaCi}",
                            color = PanMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = PanLine.copy(alpha = 0.5f))
                }
            }
        } else {
            ZhouyiDetail(entry = sel, showPinyin = showPinyin)
        }
    }
}

/** 分节标题：两侧细线＋居中标题，用于区分卦辞/大象/彖/爻辞等板块。 */
@Composable
private fun SectionLabel(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(Modifier.weight(1f), thickness = 0.5.dp, color = PanLine)
        Text(
            " $text ",
            color = PanAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        HorizontalDivider(Modifier.weight(1f), thickness = 0.5.dp, color = PanLine)
    }
}

/** 单卦全文详情。 */
@Composable
private fun ZhouyiDetail(entry: HexagramEntry, showPinyin: Boolean) {
    val text = zhouyiOf(entry)
    val yi = zhouyiTranslationOf(entry)
    val daXiang = daXiangOf(entry)
    val xiaoXiang = xiaoXiangOf(entry)
    val tuan = tuanOf(entry)
    val wenyan = wenyanOf(entry)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        @Composable
        fun pt(s: String, color: Color, size: TextUnit, lh: TextUnit) {
            if (showPinyin) PinyinText(s, color, size, lh)
            else Text(s, color = color, fontSize = size, lineHeight = lh)
        }
        SectionLabel("卦辞")
        pt(text.guaCi, PanInk, 15.sp, 21.sp)
        Text("译：${yi.guaYi}", color = PanMuted, fontSize = 13.sp, lineHeight = 18.sp)
        if (daXiang.isNotEmpty()) {
            SectionLabel("大象传")
            pt(daXiang, PanAccent, 13.sp, 18.sp)
        }
        if (tuan.isNotEmpty()) {
            SectionLabel("彖传")
            pt(tuan, PanAccent, 13.sp, 18.sp)
        }
        SectionLabel("爻辞（六爻）")
        entry.bitsBottomUp.forEachIndexed { i, bit ->
            val pos = i + 1
            val yang = bit == 1
            val yaoName = when (pos) {
                1 -> if (yang) "初九" else "初六"
                6 -> if (yang) "上九" else "上六"
                else -> (if (yang) "九" else "六") + "二三四五"[pos - 2]
            }
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(PanCardBg, RoundedCornerShape(8.dp))
                    .border(1.dp, PanLine, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                pt("$yaoName：${text.yaoCi[i]}", PanInk, 14.sp, 19.sp)
                Text("译：${yi.yaoYi[i]}", color = PanMuted, fontSize = 13.sp, lineHeight = 17.sp)
                xiaoXiang.getOrNull(i)?.let { xiao ->
                    pt("象：$xiao", PanAccent, 12.5.sp, 17.sp)
                }
            }
        }
        // 乾坤全变附用九/用六
        text.yongCi?.let { yong ->
            val tag = if (entry.upper == Trigram.QIAN) "用九" else "用六"
            SectionLabel(tag)
            pt(yong, PanInk, 14.sp, 19.sp)
            yongYiOf(entry)?.let { yy ->
                Text("译：$yy", color = PanMuted, fontSize = 13.sp, lineHeight = 17.sp)
            }
            yongXiangOf(entry)?.let { yx -> pt("象：$yx", PanAccent, 13.sp, 18.sp) }
        }
        if (wenyan != null) {
            SectionLabel("文言")
            pt(wenyan, PanInk, 13.sp, 18.sp)
        }
        wenyanYiOf(entry)?.let { wy ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                wy.split("‖").filter { it.isNotBlank() }.forEach { line ->
                    Text("译：$line", color = PanMuted, fontSize = 13.sp, lineHeight = 17.sp)
                }
            }
        }
        Spacer(Modifier.padding(bottom = 16.dp))
    }
}
