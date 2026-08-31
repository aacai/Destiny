package zhiqiu.qizheng

/**
 * 立命：生时加太阳宫，顺数至卯 → 命宫；再逆布十二宫。
 * 黄道宫 ↔ 地支：春分 0°=戌 …（星宗常用映射）
 *
 * 涉及黄经↔地支的换算均接受 [zodiacOffset]（恒星黄道时的 ayanamsa 量）。
 * 回归制传 0，行为与加此参数前完全一致。
 */
object MingGong {
    val Branches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

    /** 与星命排盘参考一致的全称宫名（命宫起逆布） */
    val PalaceNames = listOf(
        "命宫", "财帛", "兄弟", "田宅", "男女", "奴仆",
        "夫妻", "疾厄", "迁移", "官禄", "福德", "相貌",
    )

    /**
     * 黄经 → 地支宫索引。
     * [zodiacOffset] 恒星黄道时春分点相对恒星的后退量（度），回归制为 0。
     */
    fun longitudeToBranchIndex(lon: Double, zodiacOffset: Double = 0.0): Int {
        val sign = ((AstroMath.norm360(lon - zodiacOffset) / 30.0).toInt()) % 12
        return (10 - sign + 12) % 12
    }

    /** 地支宫起始黄经（宫头），与 [longitudeToBranchIndex] 互逆 */
    fun branchIndexToLonStart(branchIndex: Int, zodiacOffset: Double = 0.0): Double {
        val sign = (10 - branchIndex + 12) % 12
        return AstroMath.norm360(sign * 30.0 + zodiacOffset)
    }

    /**
     * 赤道制：宫界按赤经划分（恒星黄道宫界转赤经），返回赤经下的宫索引。
     * [ra] 为星曜赤经。注意按黄经符号升序遍历（支序与黄经方向相反）。
     */
    fun equatorialBranchIndex(ra: Double, zodiacOffset: Double = 0.0): Int {
        for (s in 0 until 12) {
            val start = AstroMath.rightAscension(AstroMath.norm360(s * 30.0 + zodiacOffset))
            val nextSign = (s + 1) % 12
            val next = AstroMath.rightAscension(AstroMath.norm360(nextSign * 30.0 + zodiacOffset)) +
                if (nextSign == 0) 360.0 else 0.0
            val width = next - start
            val delta = AstroMath.norm360(ra - start)
            if (delta < width) return (10 - s + 12) % 12
        }
        return 0
    }

    fun hourToBranchIndex(hour: Int, minute: Int): Int {
        val total = hour * 60 + minute
        val shifted = (total + 60) % (24 * 60)
        return (shifted / 120) % 12
    }

    /**
     * 命宫：生时加太阳宫，顺数至 [MingGongMethod.targetBranchIndex]。
     *
     * 果老法顺至卯（索引 3）；琴堂传本有顺至酉（索引 9）一说，待与参考盘核对。
     */
    fun mingBranchIndex(
        sunLon: Double,
        hourBranchIndex: Int,
        method: MingGongMethod = MingGongMethod.GUOLAO,
        zodiacOffset: Double = 0.0,
        equatorial: Boolean = false,
    ): Int {
        val sunIdx = if (equatorial) equatorialBranchIndex(sunLon, zodiacOffset)
        else longitudeToBranchIndex(sunLon, zodiacOffset)
        val steps = (method.targetBranchIndex - hourBranchIndex + 12) % 12
        return (sunIdx + steps) % 12
    }

    /**
     * 身宫，见 [ShenGongMethod] 的流派说明。
     *
     * 与立命法对照：立命是「**太阳**宫起生时，**顺**数至卯」，
     * 琴堂身宫则是「**太阴**宫起生时，**逆**数至酉」 —— 起算星与方向均相反。
     *
     * 琴堂法的对称性自检：酉时（支 9）生人，步数为 0，身宫即太阴所在宫，
     * 与立命法中「卯时生人命宫 = 太阳宫」同构。
     */
    fun shenBranchIndex(
        moonLon: Double,
        hourBranchIndex: Int,
        method: ShenGongMethod = ShenGongMethod.GUOLAO_MOON,
        zodiacOffset: Double = 0.0,
        equatorial: Boolean = false,
    ): Int {
        fun idx(lon: Double) = if (equatorial) equatorialBranchIndex(lon, zodiacOffset)
        else longitudeToBranchIndex(lon, zodiacOffset)
        return when (method) {
            ShenGongMethod.GUOLAO_MOON -> idx(moonLon)
            ShenGongMethod.QINTANG_YOU -> {
                val moonIdx = idx(moonLon)
                val you = 9
                (moonIdx - (you - hourBranchIndex + 12) % 12 + 12) % 12
            }
        }
    }

    fun twelvePalaces(mingBranchIndex: Int): List<Palace> {
        return PalaceNames.mapIndexed { i, name ->
            val bi = (mingBranchIndex - i + 12) % 12
            Palace(name = name, branch = Branches[bi], branchIndex = bi)
        }
    }

    data class Palace(val name: String, val branch: String, val branchIndex: Int)
}
