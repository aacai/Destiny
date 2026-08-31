package zhiqiu.qizheng

/**
 * 内圈标签：宫主星 + 八卦分野 + 地支，如「月坤未」「水坤申」「金兑酉」。
 * 八卦：乾戌亥、坎子、艮丑寅、震卯、巽辰巳、离午、坤未申、兑酉。
 * 宫主：戌火…未月、申水、酉金…（春分点起戌=白羊）。
 */
object PalaceGuaZhi {
    private val Gua = listOf(
        "坎", "艮", "艮", "震", "巽", "巽",
        "离", "坤", "坤", "兑", "乾", "乾",
    )
    private val ZhuXing = listOf(
        "土", "土", "木", "火", "金", "水",
        "日", "月", "水", "金", "火", "木",
    )

    fun label(branchIndex: Int): String {
        val i = ((branchIndex % 12) + 12) % 12
        return "${ZhuXing[i]}${Gua[i]}${MingGong.Branches[i]}"
    }

    fun labels(): List<String> = MingGong.Branches.indices.map { label(it) }
}
