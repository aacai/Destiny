package zhiqiu.qizheng

/**
 * 二十八宿（回归今宿）。
 *
 * 传统各宿古度之和 365.25，映射到黄道 360°。
 * 宿零点（角宿距星/岁差）使春分点不再硬对齐角宿起点，由排盘时刻算出后**显式传入**，
 * 见 [zeroDeg] 与 [QizhengChart.xiuZeroDeg]。
 * 黄金样例 2026-08-29 命宫丑宫头 → 箕四（对照星命排盘「回归今宿·黄道」）。
 */
object XiuTable {
    data class Xiu(val name: String, val widthAncient: Double, val element: String)

    /**
     * 古度宽度，和 = 365.25。
     * [element]：宿名口诀五行（角木蛟、亢金龙…箕水豹…），盘心「×度命」取此，不用日柱纳音。
     */
    private val raw = listOf(
        Xiu("角", 12.0, "木"), Xiu("亢", 9.0, "金"), Xiu("氐", 15.0, "土"), Xiu("房", 5.0, "日"),
        Xiu("心", 5.0, "月"), Xiu("尾", 18.0, "火"), Xiu("箕", 11.0, "水"),
        Xiu("斗", 26.0, "木"), Xiu("牛", 8.0, "金"), Xiu("女", 12.0, "土"), Xiu("虚", 10.0, "日"),
        Xiu("危", 17.0, "月"), Xiu("室", 16.0, "火"), Xiu("壁", 9.0, "水"),
        Xiu("奎", 16.0, "木"), Xiu("娄", 12.0, "金"), Xiu("胃", 14.0, "土"), Xiu("昴", 11.0, "日"),
        Xiu("毕", 16.0, "月"), Xiu("觜", 2.0, "火"), Xiu("参", 9.0, "水"),
        Xiu("井", 33.0, "木"), Xiu("鬼", 4.0, "金"), Xiu("柳", 15.0, "土"), Xiu("星", 7.0, "日"),
        Xiu("张", 18.0, "月"), Xiu("翼", 18.0, "火"), Xiu("轸", 17.0, "水"),
    )

    private const val AncientSum = 365.25

    /**
     * 黄经总岁差（general precession in longitude）：5029.0966″/儒略世纪。
     * 二十八宿锚定在恒星上，故宿界相对回归春分点每年东移约 50.29″。
     */
    const val PrecessionDegPerCentury = 1.396971

    /**
     * J2000.0 时的今宿零点（角宿起点在回归黄道上的位置，度）。
     *
     * **核对依据（与权威在线排盘「星命排盘 V1.25」xingmingpp.xyz 逐项比对后标定）**：
     * 网站「回归今宿·黄道」以**角宿距星 Spica（α Vir）的现代黄经**为锚，
     * 2026-08-29 反推得其角宿起点黄经 = 204.21°，对应 J2000 零点 ≈ 203.838°
     * （Spica 实测 J2000 黄经约 203.85°，吻合）。
     *
     * 网站的宿界宽度（今宿）与古宿度表不同（如井宿今宽远小于古宽 33°），
     * 故 [startsModern] 直接采用网站反推的**现代宿宽**，而非古宽 × scale。
     */
    const val ZeroAtJ2000 = 203.838

    /** 给定「J2000 起的世纪数」时的今宿零点 */
    fun zeroDeg(centuriesSinceJ2000: Double): Double =
        ZeroAtJ2000 + PrecessionDegPerCentury * centuriesSinceJ2000

    private val scale = 360.0 / AncientSum

    /** 古宿宽累积起点（角宿基准 0，单位：古度，和 = 365.25） */
    private val startsAncient: DoubleArray = run {
        val arr = DoubleArray(raw.size)
        var acc = 0.0
        for (i in raw.indices) {
            arr[i] = acc
            acc += raw[i].widthAncient
        }
        arr
    }

    /**
     * 现代宿宽下的累积起点（角宿基准 0，单位：今度）。
     *
     * 数据来自「星命排盘 V1.25」(xingmingpp.xyz) 反推：
     * 用 12 个宫头落宿 + 11 个星曜落宿反推 17~18 个宿的现代起点（数据自洽，
     * 如星起点 147.65° 多次出现一致），其余 10 个缺失宿按最近已知宿为锚、
     * 按古宿宽比例线性插值补全，并与已知宿自洽（轸起点推算 346.84 ≈ 实测 346.88）。
     * 顺序同 [raw]：角亢氐房心尾箕斗牛女虚危室壁奎娄胃昴毕觜参井鬼柳星张翼轸。
     */
    private val startsModern: DoubleArray = doubleArrayOf(
        0.00, 12.14, 21.25, 39.89, 46.11, 52.32, 67.43, 76.35,
        100.77, 108.30, 119.57, 129.53, 149.65, 165.32, 178.54, 192.65,
        203.36, 215.58, 225.16, 239.10, 240.84, 251.46, 282.68, 286.46,
        303.44, 311.85, 329.84, 346.88,
    )
    data class Location(
        val name: String,
        /** 宿内古度（0..该宿古宽），对照参考「箕四」「斗19.4」 */
        val degreeInXiu: Double,
        val index: Int,
        /** 宿五行/七政属性：金木水火土日月 → 盘心「水度命」等 */
        val element: String,
    )

    fun names(): List<String> = raw.map { it.name }

    /** [frame] 框架下宿 idx 的角宿基准起点（今度，未加零点） */
    private fun frameStart(idx: Int, frame: XiuFrame): Double = when (frame) {
        XiuFrame.MODERN -> startsModern[idx]
        XiuFrame.ANCIENT_SCALED, XiuFrame.ANCIENT_J2000 -> startsAncient[idx] * scale
    }

    /** 宿界在黄道上的绝对黄经；[equatorial] 时转赤经返回。绘制与计算都应显式传入 [zeroDeg] 与宿界框架 [frame] */
    fun startOf(idx: Int, zeroDeg: Double, frame: XiuFrame = XiuFrame.MODERN, equatorial: Boolean = false): Double {
        val lon = frameStart(idx, frame) + when (frame) {
            XiuFrame.ANCIENT_J2000 -> ZeroAtJ2000
            else -> zeroDeg
        }
        return if (equatorial) AstroMath.rightAscension(lon) else AstroMath.norm360(lon)
    }

    /** 宿宽（今度，360 制），用于绘制宿环（与 [startOf] 同框架同零点；赤道制下按赤经量宽） */
    fun widthAt(idx: Int, zeroDeg: Double, frame: XiuFrame = XiuFrame.MODERN, equatorial: Boolean = false): Double {
        val nextIdx = if (idx + 1 < raw.size) idx + 1 else 0
        val cur = startOf(idx, zeroDeg, frame, equatorial)
        val nextRaw = startOf(nextIdx, zeroDeg, frame, equatorial)
        // 回绕判断看黄经而非数组下标：宿界回绕点在「室→壁」（黄经 360→0），
        // 不一定落在数组末尾（角宿是数组首元，但其起点黄经大于轸宿）
        val next = if (nextRaw < cur) nextRaw + 360.0 else nextRaw
        return next - cur
    }

    /**
     * 落宿。
     *
     * [zeroDeg] 今宿零点（岁差），由排盘时刻算出后传入。
     * [xiuSystem] 宿内读数量纲：[XiuSystem.ANCIENT] 返回 365.25 古度，
     * [XiuSystem.MODERN] 返回 360 今度；**宿界的角度位置两者一致**，仅读数不同。
     * [frame] 宿界框架（今宿界 / 回归古宿 / 古宿岁差），宿界位置随之切换。
     */
    fun locate(
        longitude: Double,
        zeroDeg: Double,
        xiuSystem: XiuSystem = XiuSystem.ANCIENT,
        frame: XiuFrame = XiuFrame.MODERN,
        equatorial: Boolean = false,
    ): Location {
        // 二十八宿首尾相接铺满周天，唯一满足「起点 ≤ lon < 起点+宿宽」的即所在宿
        for (i in 0 until 28) {
            val delta = AstroMath.norm360(longitude - startOf(i, zeroDeg, frame, equatorial))
            if (delta < widthAt(i, zeroDeg, frame, equatorial)) {
                val degModern = delta
                // ANCIENT 读数 = 今度折算回 365.25 古度（保持「箕N」显示风格）；MODERN = 今度本身
                val deg = if (xiuSystem == XiuSystem.ANCIENT) degModern / scale else degModern
                val x = raw[i]
                return Location(x.name, deg, i, x.element)
            }
        }
        // 浮点边界兜底（理论上不可达）
        val x = raw[0]
        return Location(
            x.name,
            AstroMath.norm360(longitude - startOf(0, zeroDeg, frame, equatorial)) / scale,
            0,
            x.element,
        )
    }

    /** 盘心「×度命」：日/月宿记作日度命/月度命，其余用五行 */
    fun duMingLabel(element: String): String = when (element) {
        "日" -> "日度命"
        "月" -> "月度命"
        else -> "${element}度命"
    }
}
