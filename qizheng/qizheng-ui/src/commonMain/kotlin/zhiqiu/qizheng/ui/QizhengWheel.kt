package zhiqiu.qizheng.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import zhiqiu.qizheng.AstroMath
import zhiqiu.qizheng.MingGong
import zhiqiu.qizheng.PalaceGuaZhi
import zhiqiu.qizheng.QizhengChart
import zhiqiu.qizheng.StarView
import zhiqiu.qizheng.XiuTable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val Line = Color(0xFF212121)
private val Soft = Color(0xFF9E9E9E)
private val Tick = Color(0xFF424242)
private val Ink = Color(0xFF111111)
private val StarGreen = Color(0xFF2E7D32)
private val Red = Color(0xFFC62828)
private val MercuryMag = Color(0xFFC2185B)
private val MoonTeal = Color(0xFF00897B)
private val SunGold = Color(0xFFF9A825)
private val Paper = Color(0xFFFFFFFF)
private val Cream = Color(0xFFFDFBF4)
private val Hairline = Color(0xFFC9C4B8)
private val AccentGreen = Color(0xFF1B5E20)

private const val WRing = 1.8f
private const val WSpoke = 1.4f
private const val WTick = 1.2f

/**
 * 环序（外→内）：
 * 年份 → 岁数刻度 → 神煞(年) → 神煞(日) → 七政四余(按黄道度) → 二十八宿 → 十二宫名 → 宫主卦支 → 立命盘心
 */
@Composable
fun QizhengWheel(
    chart: QizhengChart,
    size: Dp,
    modifier: Modifier = Modifier,
    xiuColors: List<Color>? = null,
    onXiuClick: ((Int) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    val basePx = with(LocalDensity.current) { size.toPx() }
    val cX = basePx / 2f
    val cY = basePx / 2f
    val rTintIn = min(cX, cY) * 0.84f * 0.52f
    val rTintOut = min(cX, cY) * 0.84f * 0.60f
    val ming = chart.mingBranchIndex
    // 宿零点随岁差走，绘制必须与排盘用同一个值（取代原先的全局状态）
    val xiuZero = chart.xiuZeroDeg
    // 宿界框架（今宿界/回归古宿/古宿岁差），绘制与排盘须同一框架
    val xiuFrame = chart.config.xiuFrame
    // 赤道恒星制：盘面全按赤经布
    val eq = chart.config.equatorial
    val spans = chart.daXianSpans.ifEmpty { List(12) { 10.0 } }
    val totalYears = spans.sum().coerceAtLeast(1.0)
    val yearShen = chart.yearShenShaByBranch.ifEmpty { chart.shenShaByBranch }
    val dayShen = chart.dayShenShaByBranch

    Canvas(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val dist = kotlin.math.hypot(offset.x - cX, offset.y - cY)
                    if (dist < rTintIn || dist > rTintOut) return@detectTapGestures
                    val a = kotlin.math.atan2(-(offset.y - cY), (offset.x - cX))
                    var best = -1
                    var bestD = Double.MAX_VALUE
                    for (idx in 0 until 28) {
                        val start = XiuTable.startOf(idx, xiuZero, xiuFrame, eq)
                        val width = XiuTable.widthAt(idx, xiuZero, xiuFrame, eq)
                        val aMid = lonScreenAngle(start + width / 2.0, ming, eq, chart.zodiacOffset)
                        val d = kotlin.math.abs(
                            kotlin.math.atan2(
                                kotlin.math.sin(a - aMid),
                                kotlin.math.cos(a - aMid),
                            ),
                        )
                        val half = width * (PI * 2 / 360.0) / 2.0
                        if (d <= half && d < bestD) { bestD = d; best = idx }
                    }
                    if (best >= 0) onXiuClick?.invoke(best)
                }
            },
    ) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        // 收缩绘制半径，外圈留白：年份等文字贴边会被画布裁掉
        val r = min(cx, cy) * 0.84f
        val tickUnit = r * 0.012f

        // 外→内半径
        val rAgeOut = r
        val rAgeIn = r * 0.915f
        val rShenYOut = rAgeIn
        val rShenYIn = r * 0.80f
        val rShenDOut = rShenYIn
        val rShenDIn = r * 0.70f
        val rStarOut = rShenDIn
        val rStarIn = r * 0.60f
        val rXiuOut = rStarIn
        val rXiuIn = r * 0.52f
        val rPalaceOut = rXiuIn
        val rPalaceIn = r * 0.40f
        val rGuaOut = rPalaceIn
        val rGuaIn = r * 0.28f
        val rCore = r * 0.20f
        val rStar = (rStarOut + rStarIn) / 2f

        fun angOfLon(lon: Double): Double = lonScreenAngle(lon, ming, eq, chart.zodiacOffset)

        /** 宫 i 的宫头经度（赤道制为赤经） */
        fun palaceStart(i: Int): Double =
            if (eq) AstroMath.rightAscension(
                MingGong.branchIndexToLonStart(i, chart.zodiacOffset)
            ) else MingGong.branchIndexToLonStart(i, chart.zodiacOffset)

        val ageZero = angOfLon(palaceStart(ming))
        fun angOfAgeFraction(frac: Double): Double = ageZero - frac * (PI * 2)

        fun pt(ang: Double, rr: Float): Offset = Offset(
            cx + (rr * cos(ang)).toFloat(),
            cy - (rr * sin(ang)).toFloat(),
        )

        drawCircle(Paper, rAgeOut * 1.08f, Offset(cx, cy))

        // 二十八宿染色：只填宿环格子。屏幕角走短弧（不按黄经逐点，避免宫界处锯齿）
        if (xiuColors != null) {
            for (idx in 0 until 28) {
                val start = XiuTable.startOf(idx, xiuZero, xiuFrame, eq)
                val width = XiuTable.widthAt(idx, xiuZero, xiuFrame, eq)
                val a0 = angOfLon(start)
                val sweep = shortDelta(a0, angOfLon(start + width))
                val steps = 16
                val path = Path().apply {
                    for (k in 0..steps) {
                        val p = pt(a0 + sweep * k / steps, rXiuOut)
                        if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                    }
                    for (k in steps downTo 0) {
                        val p = pt(a0 + sweep * k / steps, rXiuIn)
                        lineTo(p.x, p.y)
                    }
                    close()
                }
                drawPath(path, xiuColors.getOrElse(idx) { Paper })
            }
        }

        listOf(rAgeOut, rAgeIn, rShenYIn, rShenDIn, rStarIn, rXiuIn, rPalaceIn, rGuaIn, rCore)
            .forEach { rr -> drawCircle(Line, rr, Offset(cx, cy), style = Stroke(WRing)) }

        // 十二宫辐条
        for (i in 0 until 12) {
            val a = angOfLon(palaceStart(i))
            drawLine(Line, pt(a, rCore), pt(a, rAgeOut), strokeWidth = WSpoke)
        }
        // 立命红线：指向**命度**（命宫宫头 + 太阳宫内度，盘心「箕四立」之点）。
        // 大限环仍以命宫宫头为起点（ageZero），两者相差太阳宫内度。
        val mingDuAng = angOfLon(chart.mingDuLon)
        drawLine(Red, Offset(cx, cy), pt(mingDuAng, rAgeOut * 1.02f), strokeWidth = 3.0f)

        // 1) 外圈年份
        var cum = 0.0
        for (step in spans.indices) {
            val a = angOfAgeFraction(cum / totalYears)
            val year = chart.daXianYears.getOrElse(step) { chart.year }
            val layout = measurer.measure(
                year.toString(),
                TextStyle(fontSize = 11.sp, color = Ink, fontWeight = FontWeight.Bold),
            )
            drawRadialText(layout, pt(a, rAgeOut * 1.055f), a)
            cum += spans[step]
        }

        // 2) 岁数刻度
        val ageMax = minOf(100, kotlin.math.ceil(totalYears).toInt())
        for (age in 0 until ageMax) {
            val frac = age / totalYears
            if (frac > 1.0) break
            val aEdge = angOfAgeFraction(frac)
            drawLine(Soft, pt(aEdge, rAgeOut), pt(aEdge, rAgeIn), strokeWidth = WTick)
        }
        for (age in 1..ageMax) {
            val frac = (age - 0.5) / totalYears
            if (frac > 1.0) break
            val aMid = angOfAgeFraction(frac)
            val layout = measurer.measure(age.toString(), TextStyle(fontSize = 5.sp, color = Ink))
            drawRadialText(layout, pt(aMid, (rAgeOut + rAgeIn) / 2f), aMid)
        }

        // 3) 外侧神煞（年）
        drawShenShaRing(
            measurer, yearShen, rShenYOut, rShenYIn, tickUnit, ming, ::angOfLon, ::pt,
            palaceStart = ::palaceStart,
        )
        // 4) 内侧神煞（日）
        drawShenShaRing(
            measurer, dayShen, rShenDOut, rShenDIn, tickUnit, ming, ::angOfLon, ::pt,
            palaceStart = ::palaceStart,
        )

        // 5) 七政四余：黄道度刻度（画在星曜层内缘，明显）+ 星曜错开 + 引线连回真实刻度
        val starRad = (r * 0.028f).coerceIn(8f, 11f)
        // 布局仿参考图：星曜摆外侧，刻度留在内缘，引线自外向内连
        val rStarCenter = rStarOut - starRad - 3f
        val rTickBase = rStarIn
        val tickMaxLen = tickUnit * 2.6f

        // ---- 刻度环：每度一根短线，30°/5° 加长加粗（赤道制按黄经度转赤经定位）----
        for (deg in 0 until 360) {
            val a = angOfLon(if (eq) AstroMath.rightAscension(deg.toDouble()) else deg.toDouble())
            val len = when {
                deg % 30 == 0 -> tickUnit * 2.6f   // 宫界长刻度
                deg % 5 == 0 -> tickUnit * 1.7f    // 五度中刻度
                else -> tickUnit * 1.0f            // 每度短刻度
            }
            drawLine(
                Tick, pt(a, rTickBase), pt(a, rTickBase + len),
                strokeWidth = when {
                    deg % 30 == 0 -> 1.6f
                    deg % 5 == 0 -> 1.15f
                    else -> 0.7f
                },
            )
        }

        // ---- 星曜层（避让 + 引线 + 星徽 + 文字 + 庙旺）----
        // 注意：此层**最后绘制**（仅晚于宫名/卦支），否则宿环界线与宿名会盖住星曜文字下缘
        fun drawStarLayer() {
            // 切向避让：角距不足则顺延，避免星曜互相叠压
            data class StarItem(val index: Int, val star: StarView, val angle: Double)
            val sortedStars = chart.stars.mapIndexed { i, s -> StarItem(i, s, angOfLon(s.longitude)) }
                .sortedBy { it.angle }
            val minSep = (starRad * 2f + 4f) / rStarCenter  // 最小角距（弧度，含间隙）
            val count = sortedStars.size
            val adj = DoubleArray(count) { sortedStars[it].angle }
            val twoPi = 2.0 * PI

            // 正向贪心：逐个确保与前一个拉开 minSep
            for (i in 1 until count) {
                if (adj[i] - adj[i - 1] < minSep) adj[i] = adj[i - 1] + minSep
            }
            // 环形闭合：末尾若顶到起点（绕回），反向回推
            if (count > 1 && adj[count - 1] + minSep > adj[0] + twoPi) {
                adj[count - 1] = adj[0] + twoPi - minSep
                for (i in count - 2 downTo 0) {
                    if (adj[i + 1] - adj[i] < minSep) adj[i] = adj[i + 1] - minSep
                }
            }
            val adjMap = HashMap<Int, Double>(count)
            sortedStars.forEachIndexed { i, item -> adjMap[item.index] = adj[i] }

            // 引线 + 圆点 + 文字
            chart.stars.forEachIndexed { idx, star ->
                val realAng = angOfLon(star.longitude)
                val shownAng = adjMap[idx] ?: realAng   // 避让后的展示角
                val p = pt(shownAng, rStarCenter)

                // ★ 引线：自星曜（外侧、避让后）连到它在刻度环上的真实位置（内侧）
                val pTick = pt(realAng, rTickBase + tickMaxLen * 0.62f)
                drawLine(Soft, p, pTick, strokeWidth = 0.9f, cap = StrokeCap.Round)

                val style = planetStyle(star.key)
                if (style.badge) {
                    drawCircle(style.fill, starRad, p)
                    // 彩色星徽描白边，与宿环色格分离更清晰
                    drawCircle(Paper, starRad, p, style = Stroke(1.4f))
                } else {
                    drawCircle(Paper, starRad, p)
                    drawCircle(StarGreen, starRad, p, style = Stroke(1.9f))
                }
                val layout = measurer.measure(
                    star.label,
                    TextStyle(fontSize = 11.sp, color = style.text, fontWeight = FontWeight.Bold),
                )
                drawText(
                    layout,
                    topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f),
                )
                if (star.retro) {
                    drawCircle(Paper, 3.4f, Offset(p.x + starRad * 0.7f, p.y - starRad * 0.7f))
                    drawCircle(Red, 2.2f, Offset(p.x + starRad * 0.7f, p.y - starRad * 0.7f))
                }
                // 庙旺利陷：仅在盘制开启时标注，标在星曜朝盘心一侧，不占外圈
                chart.dignity[star.key]?.let { level ->
                    val dp = pt(shownAng, rStarCenter - starRad - 6f)
                    val dLayout = measurer.measure(
                        level,
                        TextStyle(fontSize = 7.sp, color = SunGold, fontWeight = FontWeight.Bold),
                    )
                    drawText(
                        dLayout,
                        topLeft = Offset(dp.x - dLayout.size.width / 2f, dp.y - dLayout.size.height / 2f),
                    )
                }
            }
        }

        // 6) 二十八宿（染色时加粗宿界，避免色块糊边）
        val xiuBorder = if (xiuColors != null) Ink else Soft
        val xiuBorderW = if (xiuColors != null) 1.8f else 1.3f
        for (idx in 0 until 28) {
            val start = XiuTable.startOf(idx, xiuZero, xiuFrame, eq)
            val a0 = angOfLon(start)
            drawLine(xiuBorder, pt(a0, rXiuOut), pt(a0, rXiuIn), strokeWidth = xiuBorderW)
            val mid = start + XiuTable.widthAt(idx, xiuZero, xiuFrame, eq) / 2.0
            val a = angOfLon(mid)
            val layout = measurer.measure(
                XiuTable.names()[idx],
                TextStyle(fontSize = 11.sp, color = Ink, fontWeight = FontWeight.SemiBold),
            )
            drawRadialText(layout, pt(a, (rXiuOut + rXiuIn) / 2f), a)
        }

        // 7) 十二宫名（疾厄、夫妻…）整词径向，不竖排
        for (i in 0 until 12) {
            val a = angOfLon(palaceStart(i) + 15.0)
            val palace = chart.palaces.firstOrNull { it.branch == MingGong.Branches[i] }
            val layout = measurer.measure(
                palace?.name.orEmpty(),
                TextStyle(fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium),
            )
            drawRadialText(layout, pt(a, (rPalaceOut + rPalaceIn) / 2f), a)
        }

        // 8) 宫主+卦+支（月坤未…）整词径向，不竖排
        for (i in 0 until 12) {
            val a = angOfLon(palaceStart(i) + 15.0)
            val layout = measurer.measure(
                PalaceGuaZhi.label(i),
                TextStyle(fontSize = 11.sp, color = Ink, fontWeight = FontWeight.SemiBold),
            )
            drawRadialText(layout, pt(a, (rGuaOut + rGuaIn) / 2f), a)
        }

        // 星曜层最后画：任何环线/宿名/宫名都不再盖住星曜文字
        drawStarLayer()

        // 9) 盘心立命
        val top = measurer.measure(
            chart.mingCenterTop,
            TextStyle(fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Bold),
        )
        val bottom = measurer.measure(
            chart.mingCenterBottom,
            TextStyle(fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.Bold),
        )
        drawText(top, topLeft = Offset(cx - top.size.width / 2f, cy - top.size.height - 2f))
        drawText(bottom, topLeft = Offset(cx - bottom.size.width / 2f, cy + 2f))
    }
}

private fun DrawScope.drawShenShaRing(
    measurer: androidx.compose.ui.text.TextMeasurer,
    byBranch: List<List<String>>,
    rOut: Float,
    rIn: Float,
    tickUnit: Float,
    ming: Int,
    angOfLon: (Double) -> Double,
    pt: (Double, Float) -> Offset,
    /** 宫 i 宫头经度（赤道制为赤经） */
    palaceStart: (Int) -> Double,
) {
    for (i in 0 until 12) {
        val aCenter = angOfLon(palaceStart(i) + 15.0)
        val names = byBranch.getOrElse(i) { emptyList() }
        if (names.isEmpty()) continue

        // 每个神煞名沿半径竖排（字头朝外贴圆弧），多个神煞在圆周方向排开
        val n = names.size
        val half = (15.0 - 2.0) * PI / 180.0
        val fontSize = when {
            n <= 3 -> 9.sp
            n <= 5 -> 8.sp
            else -> 7.sp
        }
        val rMid = (rOut + rIn) / 2f
        names.forEachIndexed { k, name ->
            val frac = (k + 0.5f) / n
            val a = aCenter - half + frac * (2.0 * half)
            drawRadialStacked(
                measurer, name, a, rMid,
                fontSize = fontSize, color = StarGreen, weight = FontWeight.SemiBold, pt,
                maxOuterR = rOut,
            )
        }
    }
}

/**
 * 多字沿半径竖排：外字在外、内字在内，每字字头朝外贴圆弧。
 */
private fun DrawScope.drawRadialStacked(
    measurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    ang: Double,
    rMid: Float,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    weight: FontWeight,
    pt: (Double, Float) -> Offset,
    /** 堆叠最外字允许到达的最大半径，防止长名溢出所在环 */
    maxOuterR: Float = Float.MAX_VALUE,
) {
    if (text.isEmpty()) return
    val chars = text.toList()
    val step = fontSize.toPx() * 1.2f
    // 首字半径 = 中线 + (字数-1)/2 步长；超出环外缘时整体内移
    val r0 = minOf(rMid + (chars.size - 1) * step / 2f, maxOuterR - step)
    chars.forEachIndexed { i, ch ->
        val layout = measurer.measure(
            ch.toString(),
            TextStyle(fontSize = fontSize, color = color, fontWeight = weight),
        )
        drawRadialText(layout, pt(ang, r0 - i * step), ang)
    }
}

private fun DrawScope.drawRadialText(
    layout: TextLayoutResult,
    center: Offset,
    ang: Double,
) {
    // 字头朝外、字脚朝圆心，贴圆弧
    var deg = (-ang * 180.0 / kotlin.math.PI).toFloat() + 90f
    val n = ((deg % 360f) + 360f) % 360f
    if (n > 90f && n < 270f) deg += 180f
    rotate(degrees = deg, pivot = center) {
        drawText(
            layout,
            topLeft = Offset(
                center.x - layout.size.width / 2f,
                center.y - layout.size.height / 2f,
            ),
        )
    }
}

/** 立命点（命宫宫头）在正下；返回宫中心角。 */
private fun branchScreenAngle(branchIndex: Int, mingIndex: Int): Double {
    val stepsClockwise = (branchIndex - mingIndex + 12) % 12
    return -PI / 2 - PI / 12 - stepsClockwise * (PI * 2 / 12)
}

/**
 * 黄经（或赤经）→屏幕角：宫界连续、整体单调（经度增 = 逆时针）。
 *
 * 旧实现宫内度数沿顺时针走（`- within`），与支序的顺时针排布方向相反，
 * 每跨 30° 宫界角度折返 60°，跨宫界的宿（如虚宿）被折叠画到邻格上，
 * 造成「牛危之间空格无字」等错位。现改为宫内度数同为逆时针，与宫序一致。
 *
 * 赤道制（[equatorial]=true）：宫界为恒星宫界黄经转赤经，宫宽不再均等，
 * 段内按赤经比例插值到该宫的 30° 屏幕弧段。
 */
private fun lonScreenAngle(
    lon: Double,
    mingIndex: Int,
    equatorial: Boolean = false,
    zodiacOffset: Double = 0.0,
): Double {
    val n = ((lon % 360.0) + 360.0) % 360.0
    if (!equatorial) {
        val sign = (n / 30.0).toInt() % 12
        val branchIdx = (10 - sign + 12) % 12
        val within = (n % 30.0) / 30.0
        return branchScreenAngle(branchIdx, mingIndex) + PI / 12 + within * (PI * 2 / 12)
    }
    // 赤道制：宫界 = 恒星宫界黄经转赤经，按黄经符号升序遍历（支序与黄经方向相反）
    for (s in 0 until 12) {
        val start = AstroMath.rightAscension(AstroMath.norm360(s * 30.0 + zodiacOffset))
        val nextSign = (s + 1) % 12
        val next = AstroMath.rightAscension(AstroMath.norm360(nextSign * 30.0 + zodiacOffset)) +
            if (nextSign == 0) 360.0 else 0.0
        val width = next - start
        val delta = ((n - start) % 360.0 + 360.0) % 360.0
        if (delta < width) {
            val frac = delta / width
            val branchIdx = (10 - s + 12) % 12
            return branchScreenAngle(branchIdx, mingIndex) + PI / 12 + frac * (PI * 2 / 12)
        }
    }
    // 兜底
    return branchScreenAngle(0, mingIndex) + PI / 12
}

/** 屏幕角从 a0 到 a1 的短弧增量，范围 (-π, π] */
private fun shortDelta(a0: Double, a1: Double): Double {
    var d = a1 - a0
    while (d > PI) d -= PI * 2
    while (d <= -PI) d += PI * 2
    return d
}

private data class PlanetDraw(val badge: Boolean, val fill: Color, val text: Color)

private fun planetStyle(key: String): PlanetDraw = when (key) {
    "水" -> PlanetDraw(true, MercuryMag, Color.White)
    "月" -> PlanetDraw(true, MoonTeal, Color.White)
    "日" -> PlanetDraw(true, SunGold, Color(0xFF3E2723))
    "土" -> PlanetDraw(true, Red, Color.White)
    "火" -> PlanetDraw(true, Color(0xFFE57373), Color.White)
    "木" -> PlanetDraw(false, Paper, StarGreen)
    "金" -> PlanetDraw(false, Paper, StarGreen)
    "罗", "计" -> PlanetDraw(true, Color(0xFF5D4037), Color.White)
    "孛", "炁" -> PlanetDraw(false, Paper, StarGreen)
    else -> PlanetDraw(false, Paper, StarGreen)
}
