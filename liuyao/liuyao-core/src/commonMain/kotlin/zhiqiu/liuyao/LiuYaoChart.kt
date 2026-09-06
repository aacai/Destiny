package zhiqiu.liuyao

/**
 * 装卦结果：单爻（初=1 … 上=6）。
 * 变卦爻只装干支＋六亲（六亲仍以本宫五行为「我」）。
 */
data class InstalledYao(
    val position: Int,
    val isYang: Boolean,
    val kind: YaoKind?,
    val ganzhi: Ganzhi,
    val sixKin: SixKin,
    val sixGod: SixGod?,
    /** 命中的神煞（仅本卦按日柱装）。 */
    val shenSha: List<ShenSha>,
    val isShi: Boolean,
    val isYing: Boolean,
    val isYongShen: Boolean,
    val isMoving: Boolean,
) {
    val yaoName: String get() = yaoName(position, isYang)
    val placeName: String get() = yaoPlaceName(position)
}

data class InstalledHexagram(
    val entry: HexagramEntry,
    /** 初→上。 */
    val yaos: List<InstalledYao>,
)

data class LiuYaoChart(
    val original: InstalledHexagram,
    /** 无动爻时为 null，界面只显示本卦。 */
    val changed: InstalledHexagram?,
    val movingPositions: List<Int>,
    val dayInfo: DayInfo,
    val selection: ZhanSelection,
    val gender: String,
) {
    val hasChange: Boolean get() = changed != null
    /** 用神描述，如「妻财·九二甲寅」；无用神返回空。 */
    val yongShenLine: String get() {
        val yong = original.yaos.filter { it.isYongShen }
        if (yong.isEmpty()) return ""
        return yong.joinToString("、") { "${it.sixKin.label}·${it.yaoName}${it.ganzhi.label}" }
    }
}

fun buildChart(
    /** 初→上共 6 爻，用户逐爻指定。 */
    kinds: List<YaoKind?>,
    dayInfo: DayInfo,
    selection: ZhanSelection = ZhanSelection(),
    gender: String = "男",
): LiuYaoChart? {
    if (kinds.size != 6 || kinds.any { it == null }) return null
    val ks = kinds.filterNotNull()
    val originalEntry = hexagramOf(ks.map { if (it.isYang) 1 else 0 })
    val moving = ks.mapIndexedNotNull { i, k -> if (k.isMoving) i + 1 else null }
    val changedEntry = if (moving.isEmpty()) {
        null
    } else {
        hexagramOf(
            ks.mapIndexed { i, k ->
                val yang = if (k.isMoving) !k.isYang else k.isYang
                if (yang) 1 else 0
            },
        )
    }

    val palaceElement = originalEntry.palace.element
    val targetKins = yongShenKins(selection.topic, gender)
    val shaBranches = shenShaBranches(dayInfo.dayStem, dayInfo.dayBranch)

    fun installYao(
        entry: HexagramEntry,
        position: Int,
        isYang: Boolean,
        kind: YaoKind?,
        isOriginal: Boolean,
    ): InstalledYao {
        val gz = najiaOf(entry, position)
        val kin = sixKinOf(palaceElement, gz.element)
        val sha = if (isOriginal) {
            ShenSha.entries.filter { gz.branch in (shaBranches[it] ?: emptySet()) }
        } else {
            emptyList()
        }
        val shi = position == entry.shiPosition
        val ying = position == entry.yingPosition
        val yong = if (!isOriginal) {
            false
        } else if (shiAsYongShen(selection.topic)) {
            shi
        } else {
            kin in targetKins
        }
        return InstalledYao(
            position = position,
            isYang = isYang,
            kind = kind,
            ganzhi = gz,
            sixKin = kin,
            sixGod = if (isOriginal) godAt(dayInfo.dayStem, position) else null,
            shenSha = sha,
            isShi = shi,
            isYing = ying,
            isYongShen = yong,
            isMoving = kind?.isMoving == true,
        )
    }

    val originalYaos = (1..6).map { p ->
        installYao(originalEntry, p, ks[p - 1].isYang, ks[p - 1], isOriginal = true)
    }
    val changedYaos = changedEntry?.let { ce ->
        (1..6).map { p ->
            val origKind = ks[p - 1]
            val yang = if (origKind.isMoving) !origKind.isYang else origKind.isYang
            installYao(ce, p, yang, kind = null, isOriginal = false)
        }
    }
    return LiuYaoChart(
        original = InstalledHexagram(originalEntry, originalYaos),
        changed = changedEntry?.let { InstalledHexagram(it, changedYaos!!) },
        movingPositions = moving,
        dayInfo = dayInfo,
        selection = selection,
        gender = gender,
    )
}
