package zhiqiu.qimen

/** 阳遁顺时针。上南下北时，从坎起：坎艮震巽离坤兑乾。 */
private val RING = listOf(
    Palace.Kan, Palace.Gen, Palace.Zhen, Palace.Xun,
    Palace.Li, Palace.Kun, Palace.Dui, Palace.Qian,
)

private val FLY_DOORS = listOf(
    Door.Xiu, Door.Si, Door.Shang, Door.Du,
    Door.Kai, Door.Shock, Door.Sheng, Door.Scene,
)

/**
 * 由局数和时干支排盘。时干是甲时，甲不落地盘，改寻该甲所遁的六仪。
 */
fun arrange(
    ju: Int,
    yang: Boolean,
    hourStem: Stem,
    hourBranch: Branch,
    plate: PlateKind,
): PlateView {
    val earth = earthStems(ju, yang)
    val xunIndex = xunHeadIndex(hourStem, hourBranch)
    val xunStem = Stem.entries[xunIndex % 10]
    val xunBranch = Branch.entries[xunIndex % 12]
    val dunStem = Stem.entries[4 + xunIndex / 10]
    val xunPalace = Palace.entries[earth.indexOf(dunStem)]
    val zhiFu = Star.entries.first { it.home == xunPalace }
    val zhiShi = if (xunPalace == Palace.Zhong) Door.Si else Door.entries.first { it.home == xunPalace }
    val hourOnEarth = if (hourStem == Stem.Jia) dunStem else hourStem
    val rawFu = Palace.entries[earth.indexOf(hourOnEarth)]
    val steps = (hourBranch.ordinal - xunBranch.ordinal + 12) % 12
    val rawShi = Palace.byNumber(advanceLuo(xunPalace.number, steps, yang))
    val fuLand = land(rawFu, plate)
    val shiLand = land(rawShi, plate)

    val drafts = Palace.entries.map { Draft(it) }
    if (plate == PlateKind.Zhuan) {
        layZhuan(drafts, earth, yang, zhiFu, zhiShi, fuLand, shiLand)
    } else {
        layFei(drafts, earth, yang, zhiFu, zhiShi, fuLand, shiLand)
    }
    val kong = kongBranches(xunBranch)
    val horse = horseOf(hourBranch)
    val cells = drafts.map { it.freeze(kong, horse) }
    return PlateView(
        ju = ju,
        yang = yang,
        plate = plate,
        earth = earth,
        cells = cells,
        zhiFu = zhiFu,
        zhiShi = zhiShi,
        centerEarth = earth[Palace.Zhong.ordinal],
        xunLabel = xunStem.label + xunBranch.label,
        dunStem = dunStem,
        kong = kong,
        horse = horse,
    )
}

/** 转盘落中五就寄坤二。飞盘中五自己入局。 */
private fun land(palace: Palace, plate: PlateKind): Palace =
    if (plate == PlateKind.Zhuan && palace == Palace.Zhong) Palace.Kun else palace

private fun layZhuan(
    drafts: List<Draft>,
    earth: List<Stem>,
    yang: Boolean,
    zhiFu: Star,
    zhiShi: Door,
    fuLand: Palace,
    shiLand: Palace,
) {
    Palace.entries.forEach { palace ->
        if (palace == Palace.Zhong) return@forEach
        val stems = if (palace == Palace.Kun) {
            listOf(earth[Palace.Kun.ordinal], earth[Palace.Zhong.ordinal])
        } else {
            listOf(earth[palace.ordinal])
        }
        drafts[palace.ordinal].earth += stems
    }
    // 天禽没有自己的环位，值符若是天禽，就带着天芮一起转。
    val lead = if (zhiFu == Star.Qin) Star.Rui else zhiFu
    val starSteps = ringSteps(lead.home, fuLand, yang)
    Star.entries.filter { it != Star.Qin }.forEach { star ->
        val dest = advanceRing(star.home, starSteps, yang)
        drafts[dest.ordinal].stars += star
        drafts[dest.ordinal].heaven += earth[star.home.ordinal]
    }
    val qinDest = advanceRing(Star.Rui.home, starSteps, yang)
    drafts[qinDest.ordinal].stars += Star.Qin
    drafts[qinDest.ordinal].heaven += earth[Palace.Zhong.ordinal]

    val doorSteps = ringSteps(zhiShi.home, shiLand, yang)
    Door.entries.forEach { door ->
        val dest = advanceRing(door.home, doorSteps, yang)
        drafts[dest.ordinal].door = door
    }
    Deity.entries.forEachIndexed { index, deity ->
        val dest = advanceRing(fuLand, index, yang)
        drafts[dest.ordinal].deity = deity
    }
}

private fun layFei(
    drafts: List<Draft>,
    earth: List<Stem>,
    yang: Boolean,
    zhiFu: Star,
    zhiShi: Door,
    fuLand: Palace,
    shiLand: Palace,
) {
    Palace.entries.forEach { palace ->
        drafts[palace.ordinal].earth += earth[palace.ordinal]
    }
    var number = fuLand.number
    val starStart = Star.entries.indexOf(zhiFu)
    repeat(9) { index ->
        val star = Star.entries[(starStart + index) % 9]
        val draft = drafts[number - 1]
        draft.stars += star
        draft.heaven += earth[star.home.ordinal]
        number = stepLuo(number, yang)
    }
    number = shiLand.number
    val doorStart = FLY_DOORS.indexOf(zhiShi)
    repeat(8) { index ->
        val door = FLY_DOORS[(doorStart + index) % 8]
        drafts[number - 1].door = door
        number = stepLuo(number, yang)
    }
    number = fuLand.number
    Deity.entries.forEach { deity ->
        drafts[number - 1].deity = deity
        number = stepLuo(number, yang)
    }
}

private fun ringSteps(from: Palace, to: Palace, yang: Boolean): Int {
    val fromIndex = RING.indexOf(from)
    val toIndex = RING.indexOf(to)
    require(fromIndex >= 0 && toIndex >= 0) { "八宫环上没有${from.shortName}或${to.shortName}" }
    val clockwise = (toIndex - fromIndex + RING.size) % RING.size
    return if (yang) clockwise else (RING.size - clockwise) % RING.size
}

private fun advanceRing(from: Palace, steps: Int, yang: Boolean): Palace {
    val index = RING.indexOf(from)
    val delta = if (yang) steps else -steps
    return RING[floorMod(index + delta, RING.size)]
}

private fun floorMod(value: Int, mod: Int): Int {
    val remainder = value % mod
    return if (remainder < 0) remainder + mod else remainder
}

private fun advanceLuo(number: Int, steps: Int, yang: Boolean): Int {
    var cursor = number
    repeat(steps) { cursor = stepLuo(cursor, yang) }
    return cursor
}

private fun stepLuo(number: Int, yang: Boolean): Int = when {
    yang -> if (number == 9) 1 else number + 1
    else -> if (number == 1) 9 else number - 1
}

private fun xunHeadIndex(stem: Stem, branch: Branch): Int {
    var index = stem.ordinal
    while (index % 12 != branch.ordinal) index += 10
    return index - index % 10
}

private fun kongBranches(xunBranch: Branch): List<Branch> = listOf(
    Branch.entries[(xunBranch.ordinal + 10) % 12],
    Branch.entries[(xunBranch.ordinal + 11) % 12],
)

private fun horseOf(branch: Branch): Branch = when (branch) {
    Branch.Shen, Branch.Zi, Branch.Chen -> Branch.Yin
    Branch.Yin, Branch.Wu, Branch.Xu -> Branch.Shen
    Branch.Si, Branch.You, Branch.Chou -> Branch.Hai
    Branch.Hai, Branch.Mao, Branch.Wei -> Branch.Si
}

private fun tombOf(stem: Stem): Palace = when (stem) {
    Stem.Jia, Stem.Yi -> Palace.Kun
    Stem.Bing, Stem.Wu -> Palace.Qian
    Stem.Ding, Stem.Ji -> Palace.Gen
    Stem.Geng, Stem.Xin, Stem.Ren, Stem.Gui -> Palace.Xun
}

private fun punishOf(stem: Stem): Palace? = when (stem) {
    Stem.Wu -> Palace.Zhen
    Stem.Ji -> Palace.Kun
    Stem.Geng -> Palace.Gen
    Stem.Xin -> Palace.Li
    Stem.Ren, Stem.Gui -> Palace.Xun
    Stem.Yi -> Palace.Kan
    Stem.Bing -> Palace.Li
    Stem.Ding -> Palace.Dui
    Stem.Jia -> null
}

private class Draft(val palace: Palace) {
    var deity: Deity? = null
    val stars = mutableListOf<Star>()
    var door: Door? = null
    val heaven = mutableListOf<Stem>()
    val earth = mutableListOf<Stem>()

    fun freeze(kong: List<Branch>, horse: Branch): PalaceCell {
        val marks = buildList {
            if (palace.branches.any { it in kong }) add(Mark.Kong)
            if (horse in palace.branches) add(Mark.Ma)
            val sitting = heaven + earth
            if (sitting.any { tombOf(it) == palace }) add(Mark.Mu)
            if (heaven.any { punishOf(it) == palace }) add(Mark.Xing)
            val gate = door
            if (gate != null && gate.element.controls(palace.element)) add(Mark.Po)
        }
        return PalaceCell(
            palace = palace,
            deity = deity,
            stars = stars.toList(),
            door = door,
            heavenStems = heaven.toList(),
            earthStems = earth.toList(),
            marks = marks,
        )
    }
}
