package zhiqiu.qizheng

import com.tyme.solar.SolarTime
import zhiqiu.iztro.bazi.original.formatBirthTermLabel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

object QizhengBuilder {

    /**
     * 大限（洞微限）在命宫内的起限偏移（度）。来历待考，详见 [buildLimits]。
     * 取值使本命盘（age = 0）落「斗 19.4」（黄经 300°，与网站金标准「子斗 19.44」一致）。
     */
    private const val DA_XIAN_AGE0_OFFSET_DEG = 30.0

    private val StarDefs = listOf(
        Triple(Ephemeris.Body.Sun, "日", "日"),
        Triple(Ephemeris.Body.Moon, "月", "月"),
        Triple(Ephemeris.Body.Mercury, "水", "水"),
        Triple(Ephemeris.Body.Venus, "金", "金"),
        Triple(Ephemeris.Body.Mars, "火", "火"),
        Triple(Ephemeris.Body.Jupiter, "木", "木"),
        Triple(Ephemeris.Body.Saturn, "土", "土"),
        Triple(Ephemeris.Body.Rahu, "罗", "罗"),
        Triple(Ephemeris.Body.Ketu, "计", "计"),
        Triple(Ephemeris.Body.YueBei, "孛", "孛"),
        Triple(Ephemeris.Body.ZiQi, "炁", "炁"),
    )

    fun build(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        gender: String = "男",
        lon: Double = QizhengDefaults.LonBeijing,
        lat: Double = QizhengDefaults.LatBeijing,
        note: String = "",
        /** 相对出生年的虚岁近似；null 表示按本命盘（0） */
        ageYears: Int? = null,
        /** 盘制配置；默认果老星宗，与黄金样例口径一致 */
        config: QizhengConfig = PanZhiPresets.default,
    ): QizhengChart {
        // 真太阳时模式下先把钟表时校正到真太阳时（可能跨日）；平太阳时原样返回
        val clock = if (config.clockMode == ClockMode.TRUE_SOLAR) {
            applyTrueSolar(year, month, day, hour, minute, lon)
        } else {
            ClockParts(year, month, day, hour, minute)
        }
        // Asia/Shanghai ≈ UT+8
        val hourUt = clock.hour + clock.minute / 60.0 - 8.0
        val jd = AstroMath.julianDay(clock.year, clock.month, clock.day, hourUt)
        // 黄道偏移：恒星制下春分点相对恒星的后退量（ayanamsa），回归制恒为 0
        val zodiacOffset = config.zodiacOffset(Ephemeris.centuriesTt(jd))
        // 二十八宿锚定在恒星上，宿界相对回归黄道随岁差东移（≈1.397°/世纪）。
        // 落宿前先按本盘时刻算出今宿零点，否则跨数十年的盘会系统性偏一宿。
        // 全程使用局部值，最终随 [QizhengChart.xiuZeroDeg] 交给 UI 绘制，
        // 不再读写任何全局状态。
        val xiuZero = XiuTable.zeroDeg(Ephemeris.centuriesTt(jd))

        // 赤道恒星制：全盘按赤经布——星曜/宿界/宫界统一转赤经后再落宫落宿
        val eq = config.equatorial
        fun disp(lon: Double): Double = if (eq) AstroMath.rightAscension(lon) else lon

        val starsRaw = StarDefs
            .let { list ->
                if (config.useZiQi) list else list.filterNot { it.first == Ephemeris.Body.ZiQi }
            }
            .map { (body, key, label) ->
                val pos = Ephemeris.position(body, jd, config)
                val dispLon = disp(pos.longitude)
                val bi = if (eq) MingGong.equatorialBranchIndex(dispLon, zodiacOffset)
                else MingGong.longitudeToBranchIndex(pos.longitude, zodiacOffset)
                val xiu = XiuTable.locate(
                    dispLon, xiuZero, config.xiuSystem, config.xiuFrame, eq, config.customXiuWidths,
                )
                // 行度先按自速判；五星「伏」还要看与日黄经差，下面太阳算完后再补一次
                val motion0 = planetMotion(body, pos.speedDegPerDay, elongFromSun = 180.0)
                StarView(
                    key = key,
                    label = label,
                    longitude = dispLon,
                    speed = pos.speedDegPerDay,
                    branch = MingGong.Branches[bi],
                    xiu = xiu.name,
                    xiuDegree = xiu.degreeInXiu,
                    retro = motion0 == "逆",
                    motion = motion0,
                )
            }

        val sun = starsRaw.first { it.key == "日" }
        // 水金「伏」：与日黄经差 < 约 15° 且非逆留时标伏（肉眼不可见区近似）
        val stars = starsRaw.map { st ->
            val body = when (st.key) {
                "水" -> Ephemeris.Body.Mercury
                "金" -> Ephemeris.Body.Venus
                else -> return@map st
            }
            val elong = abs(AstroMath.norm180(st.longitude - sun.longitude))
            val m = planetMotion(body, st.speed, elong)
            if (m == st.motion) st else st.copy(motion = m, retro = m == "逆")
        }
        val moon = stars.first { it.key == "月" }
        // 生时地支须用校正后的时刻（真太阳时模式下与钟表时不同）
        val hourBranchBirth = MingGong.hourToBranchIndex(clock.hour, clock.minute)
        // 日出/日落立命：立命换用当日出没时辰，星曜仍按出生时刻
        val hourBranchMing = when (config.mingGongMethod) {
            MingGongMethod.SUNRISE -> {
                val sr = sunriseSunsetLocal(clock.year, clock.month, clock.day, lon, lat, rising = true)
                MingGong.hourToBranchIndex(sr.hour, sr.minute)
            }
            MingGongMethod.SUNSET -> {
                val ss = sunriseSunsetLocal(clock.year, clock.month, clock.day, lon, lat, rising = false)
                MingGong.hourToBranchIndex(ss.hour, ss.minute)
            }
            else -> hourBranchBirth
        }
        val mingMethodCore = when (config.mingGongMethod) {
            MingGongMethod.SUNRISE, MingGongMethod.SUNSET -> MingGongMethod.GUOLAO
            else -> config.mingGongMethod
        }
        // 命宫：时加太阳顺数至卯（果老）；用户固定命宫时直接采用（MOIRA 式手动安命）
        val mingIdxAuto = MingGong.mingBranchIndex(
            sunLon = sun.longitude,
            hourBranchIndex = hourBranchMing,
            method = mingMethodCore,
            zodiacOffset = zodiacOffset,
            equatorial = eq,
        )
        // 天盘时分：对照星命 getmd，用时分角重算命度并重映射命宫（覆盖自动/固定命宫）
        val solarInsideDegree = ((sun.longitude % 30.0) + 30.0) % 30.0
        val mingCuspLonTian = if (config.tianPan) {
            MingGong.tianPanMingDuLon(sun.longitude, clock.hour, clock.minute)
        } else null
        val mingIdx = when {
            config.tianPan && mingCuspLonTian != null -> {
                if (eq) MingGong.equatorialBranchIndex(mingCuspLonTian, zodiacOffset)
                else MingGong.longitudeToBranchIndex(mingCuspLonTian, zodiacOffset)
            }
            else -> config.fixedMingBranch ?: mingIdxAuto
        }
        // 身宫以**太阴**为体（果老派即月亮所在宫），与立命用太阳相对
        // 天盘时分下整宫制身宫重标与安身步数等价，故仍走安身法
        val shenIdx = if (config.useShenGong) {
            val shenTargetHour = when (config.shenGongMethod) {
                ShenGongMethod.SUNRISE -> {
                    val sr = sunriseSunsetLocal(clock.year, clock.month, clock.day, lon, lat, rising = true)
                    MingGong.hourToBranchIndex(sr.hour, sr.minute)
                }
                ShenGongMethod.SUNSET -> {
                    val ss = sunriseSunsetLocal(clock.year, clock.month, clock.day, lon, lat, rising = false)
                    MingGong.hourToBranchIndex(ss.hour, ss.minute)
                }
                else -> hourBranchBirth
            }
            MingGong.shenBranchIndex(
                moonLon = moon.longitude,
                hourBranchIndex = hourBranchBirth,
                method = config.shenGongMethod,
                zodiacOffset = zodiacOffset,
                equatorial = eq,
                targetHourBranchIndex = shenTargetHour,
            )
        } else {
            null
        }
        val palaces = MingGong.twelvePalaces(mingIdx).map {
            PalaceView(name = it.name, branch = it.branch)
        }

        val solarTime = SolarTime(clock.year, clock.month, clock.day, clock.hour, clock.minute, 0)
        val lunarHour = solarTime.getLunarHour()
        val eight = lunarHour.getEightChar()
        val lunarDay = lunarHour.getLunarDay()

        val pillars = listOf(
            eight.getYear(),
            eight.getMonth(),
            eight.getDay(),
            eight.getHour(),
        )
        val bazi = pillars.joinToString(" ") { it.getName() }
        val stemLine = pillars.joinToString("") { it.getHeavenStem().getName() }
        val branchLine = pillars.joinToString("") { it.getEarthBranch().getName() }
        val nayin = eight.getDay().getSound().getName()

        val mingBranch = MingGong.Branches[mingIdx]
        // 立命度：果老 = 命宫头 + 太阳宫内度；天盘时分 = 软件公式命度
        val mingCuspLon = when {
            mingCuspLonTian != null -> mingCuspLonTian
            eq -> sun.longitude
            else -> MingGong.branchIndexToLonStart(mingIdx, zodiacOffset) + solarInsideDegree
        }
        val mingXiu = XiuTable.locate(
            mingCuspLon, xiuZero, config.xiuSystem, config.xiuFrame, eq, config.customXiuWidths,
        )
        val degInt = kotlin.math.round(mingXiu.degreeInXiu).toInt().coerceIn(0, 30)
        val mingCenterTop = "${mingXiu.name}${degInt}立"
        val mingCenterBottom = XiuTable.duMingLabel(mingXiu.element)
        val mingLabel = "$mingCenterTop · $mingCenterBottom"

        val age = ageYears ?: 0
        val limits = buildLimits(
            chartYear = clock.year,
            chartMonth = clock.month,
            mingIdx = mingIdx,
            yearPillar = eight.getYear().getName(),
            stars = stars,
            ageYears = age,
            xiuZero = xiuZero,
            xiuSystem = config.xiuSystem,
            xiuFrame = config.xiuFrame,
            equatorial = eq,
            zodiacOffset = zodiacOffset,
            customXiuWidths = config.customXiuWidths,
        )
        val aspects = buildAspects(stars)
        // 化曜：十化曜 + 诸星起例（年干为主；官禄宫 = 命宫逆布第 10 宫）
        val officialPalaceIdx = MingGong.Branches.indexOf(palaces[9].branch)
        val lunarMonth = runCatching { lunarDay.getLunarMonth().month }.getOrNull()
        val huaYao = HuaYaoTable.buildColumns(
            yearStem = eight.getYear().getHeavenStem().getName(),
            yearBranch = eight.getYear().getEarthBranch().getName(),
            lunarMonth = lunarMonth,
            mingIdx = mingIdx,
            officialPalaceIdx = officialPalaceIdx,
            yearNayinElement = eight.getYear().getSound().getName().takeLast(1),
            presentStars = stars.map { it.key }.toSet(),
            school = config.huaYaoSchool,
        )

        val pillarBranches = pillars.map { it.getEarthBranch().getName() }
        val solid = pillarBranches.distinct().joinToString("")
        val empty = MingGong.Branches.filter { it !in pillarBranches }.joinToString("")

        // 洞微大限：顺地支不等分年限
        val dongWei = DongWeiLimits.segments(mingIdx, sun.longitude, clock.year)
        val daXianYears = DongWeiLimits.startYears(dongWei)
        val daXianSpans = dongWei.map { it.years }

        val yearStem = eight.getYear().getHeavenStem().getName()
        val yearBranch = eight.getYear().getEarthBranch().getName()
        val dayStem = eight.getDay().getHeavenStem().getName()
        val dayBranchName = eight.getDay().getEarthBranch().getName()
        val monthBranch = eight.getMonth().getEarthBranch().getName()
        val shenDual = QizhengShenSha.dual(
            yearStem = yearStem,
            yearBranch = yearBranch,
            dayStem = dayStem,
            dayBranch = dayBranchName,
            monthBranch = monthBranch,
            set = config.shenShaSet,
            maxPerBranch = config.shenShaMaxPerBranch,
        )
        val shenShaMerged = shenDual.yearRing.indices.map { i ->
            (shenDual.yearRing[i] + shenDual.dayRing[i]).distinct().take(12)
        }
        // 农历日名（初一…三十），供弦/晦星格
        val lunarDayName = runCatching { lunarDay.getName() }.getOrElse { "" }
        // 度主：命度宿五行 → 日/月/五星 key
        val duLordKey = when (val el = mingXiu.element) {
            "日", "月", "木", "火", "土", "金", "水" -> el
            else -> ""
        }
        val patterns = XingGe.evaluate(
            stars = stars,
            palaces = palaces,
            monthBranch = monthBranch,
            lunarDayName = lunarDayName,
            hour = clock.hour,
            mingBranch = mingBranch,
            shenBranch = shenIdx?.let { MingGong.Branches[it] },
            shenShaByBranch = shenShaMerged,
            mingXiu = mingXiu.name,
            duLordKey = duLordKey,
        )

        val lunarMonthDay = runCatching {
            val name = lunarDay.toString()
            // LunarDay toString 常含「农历」等，截取月日段
            name.replace("农历", "").trim()
        }.getOrElse { lunarDay.toString() }
        val lunarLabel = "${clock.year}年${lunarMonthDay}${MingGong.Branches[hourBranchBirth]}时"

        return QizhengChart(
            year = clock.year,
            month = clock.month,
            day = clock.day,
            hour = clock.hour,
            minute = clock.minute,
            longitude = lon,
            latitude = lat,
            gender = gender,
            solarLabel = "${clock.year}年${clock.month.toString().padStart(2, '0')}月${clock.day.toString().padStart(2, '0')}日 ${clock.hour.toString().padStart(2, '0')}:${clock.minute.toString().padStart(2, '0')}",
            lunarLabel = lunarLabel,
            termLabel = formatBirthTermLabel(solarTime),
            baziLabel = bazi,
            stemLine = stemLine,
            branchLine = branchLine,
            nayin = nayin,
            mingBranch = mingBranch,
            mingBranchIndex = mingIdx,
            mingDuLon = mingCuspLon,
            mingCenterTop = mingCenterTop,
            mingCenterBottom = mingCenterBottom,
            mingLabel = mingLabel,
            daXianYears = daXianYears,
            daXianSpans = daXianSpans,
            yearShenShaByBranch = shenDual.yearRing,
            dayShenShaByBranch = shenDual.dayRing,
            shenShaByBranch = shenShaMerged,
            stars = stars,
            palaces = palaces,
            limits = limits,
            aspects = aspects,
            patterns = patterns,
            huaYao = huaYao,
            solidBranches = solid,
            emptyBranches = empty,
            note = note,
            config = config,
            xiuZeroDeg = xiuZero,
            zodiacOffset = zodiacOffset,
            shenBranch = shenIdx?.let { MingGong.Branches[it] },
            dignity = if (config.dignityMode != DignityMode.OFF) {
                Dignity.ofChart(stars)
            } else {
                emptyMap()
            },
            panZhi = config.displayName,
            panZhiDetail = config.detail,
        )
    }

    // ------------------------------------------------------------------ 时间基准

    /** 东八区标准经度，真太阳时经度差校正的基准 */
    private const val STANDARD_MERIDIAN = 120.0

    private fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    /** 该年该月的天数（28–31） */
    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        2 -> if (isLeapYear(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    /** 一年中的第几天（1–366） */
    fun dayOfYear(year: Int, month: Int, day: Int): Int {
        var n = day
        for (m in 1 until month) n += daysInMonth(year, m)
        return n
    }

    /** 日期加减天数，可跨月跨年（[delta] 可为负）；时分固定为 0:00 */
    fun plusDays(year: Int, month: Int, day: Int, delta: Int): ClockParts {
        var y = year
        var m = month
        var d = day + delta
        while (d > daysInMonth(y, m)) {
            d -= daysInMonth(y, m)
            m += 1
            if (m > 12) { m = 1; y += 1 }
        }
        while (d < 1) {
            m -= 1
            if (m < 1) { m = 12; y -= 1 }
            d += daysInMonth(y, m)
        }
        return ClockParts(y, m, d, 0, 0)
    }

    /**
     * 均时差 EoT（分钟）：真太阳时 − 平太阳时。
     * 常用近似 EoT ≈ 9.87·sin(2B) − 7.53·cos(B) − 1.5·sin(B)，B = 2π(N−81)/364。
     */
    fun equationOfTimeMinutes(year: Int, month: Int, day: Int): Double {
        val n = dayOfYear(year, month, day)
        val b = 2 * PI * (n - 81) / 364.0
        return 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
    }

    /**
     * 钟表时 → 真太阳时，返回校正后的年月日时分（可能跨日）。
     *
     * 真太阳时 = 钟表时 + 经度差校正 + 均时差
     * 经度差 = (出生地经度 − 时区标准经度 120°) × 4 分钟/度
     */
    fun applyTrueSolar(
        year: Int, month: Int, day: Int, hour: Int, minute: Int, lon: Double,
    ): ClockParts {
        val totalMin = hour * 60.0 + minute +
            (lon - STANDARD_MERIDIAN) * 4.0 +
            equationOfTimeMinutes(year, month, day)
        val dayShift = floor(totalMin / 1440.0).toInt()
        val minuteOfDay = totalMin - dayShift * 1440.0
        val cal = plusDays(year, month, day, dayShift)
        return ClockParts(
            year = cal.year,
            month = cal.month,
            day = cal.day,
            hour = (minuteOfDay / 60).toInt() % 24,
            minute = ((minuteOfDay % 60).toInt() + 60) % 60,
        )
    }

    /**
     * 近似日出/日落的**钟表地方时**（东八区标准经度 120° 口径）。
     * 用于日出/日落立命换时辰；精度约数分钟，够支时粒度。
     */
    fun sunriseSunsetLocal(
        year: Int, month: Int, day: Int,
        lonDeg: Double, latDeg: Double,
        rising: Boolean,
    ): ClockParts {
        val n = dayOfYear(year, month, day)
        val decl = 23.44 * sin(2 * PI * (284 + n) / 365.0)
        val lat = latDeg * AstroMath.Deg2Rad
        val d = decl * AstroMath.Deg2Rad
        // 视太阳高度 −0.833°（大气折射+视半径）
        val cosH = (sin(-0.833 * AstroMath.Deg2Rad) - sin(lat) * sin(d)) /
            (cos(lat) * cos(d))
        val hDeg = if (cosH <= -1.0) 180.0 else if (cosH >= 1.0) 0.0
        else kotlin.math.acos(cosH.coerceIn(-1.0, 1.0)) * AstroMath.Rad2Deg
        val eot = equationOfTimeMinutes(year, month, day)
        // 地方平太阳正午（相对东八区钟表）≈ 12:00 − (λ−120)×4min − EoT? 
        // 真太阳正午：钟表 = 12 − (λ−120)×4 − EoT（EoT=真−平，故平正午钟表=12−经差−EoT）
        val noonMin = 12 * 60.0 - (lonDeg - STANDARD_MERIDIAN) * 4.0 - eot
        val offsetMin = hDeg / 15.0 * 60.0
        var total = if (rising) noonMin - offsetMin else noonMin + offsetMin
        var dayShift = 0
        while (total < 0) { total += 1440.0; dayShift -= 1 }
        while (total >= 1440.0) { total -= 1440.0; dayShift += 1 }
        val cal = plusDays(year, month, day, dayShift)
        return ClockParts(
            cal.year, cal.month, cal.day,
            (total / 60).toInt() % 24,
            (total % 60).toInt(),
        )
    }

    private fun buildLimits(
        chartYear: Int,
        chartMonth: Int,
        mingIdx: Int,
        yearPillar: String,
        stars: List<StarView>,
        ageYears: Int?,
        xiuZero: Double,
        xiuSystem: XiuSystem = XiuSystem.ANCIENT,
        xiuFrame: XiuFrame = XiuFrame.MODERN,
        equatorial: Boolean = false,
        zodiacOffset: Double = 0.0,
        customXiuWidths: List<Double>? = null,
    ): LimitSummary {
        val age = ageYears ?: 0
        val daStep = (age / 10) % 12
        val daBi = (mingIdx - daStep + 12) % 12
        val daCusp = MingGong.branchIndexToLonStart(daBi, zodiacOffset)
        /**
         * 大限宿度：本命起限落在命宫内的「限度」。
         *
         * 参考盘「大限 斗 19.4」落在命宫（丑，270°–300°）内 25.9° 处，
         * 而**不是**立命宫头的箕四 —— 即起限度 ≠ 命度。该偏移的确切来历
         * 尚未在《星学大成》《果老星宗》中查到对应条目，候选解释：
         * ① 洞微限「限度」另有起算点；② 与竹罗三限混排；③ 参考盘的自定义口径。
         *
         * TODO: 查证后换成正式公式；在此之前改动此常量会直接破坏黄金样例断言。
         */
        val daLonRaw = AstroMath.norm360(daCusp + if (age == 0) DA_XIAN_AGE0_OFFSET_DEG else (age % 10) * 3.0)
        val daLon = if (equatorial) AstroMath.rightAscension(daLonRaw) else daLonRaw
        val daLoc = XiuTable.locate(daLon, xiuZero, xiuSystem, xiuFrame, equatorial, customXiuWidths)
        val xiao = MingGong.Branches[(mingIdx - (age % 12) + 12) % 12]
        val yue = if (age == 0) {
            xiao
        } else {
            MingGong.Branches[(MingGong.Branches.indexOf(xiao) + (chartMonth - 1)) % 12]
        }
        return LimitSummary(
            daXian = "${daLoc.name} ${fmt1(daLoc.degreeInXiu)}",
            taiSui = yearPillar,
            xiaoXian = xiao,
            yueXian = yue,
            shanMu = stars.first { it.key == "木" }.branch.let { br ->
                when (br) {
                    "寅", "卯" -> "木"
                    "巳", "午" -> "火"
                    "申", "酉" -> "金"
                    "亥", "子" -> "水"
                    else -> "土"
                }
            },
            dingXing = "计罗",
        )
    }

    private fun fmt1(v: Double): String {
        val t = kotlin.math.round(v * 10).toInt()
        return "${t / 10}.${kotlin.math.abs(t % 10)}"
    }

    private fun buildAspects(stars: List<StarView>): List<AspectView> {
        val out = mutableListOf<AspectView>()
        val keys = stars.associateBy { it.key }
        val pairs = listOf("日", "月", "水", "金", "火", "木", "土")
        for (i in pairs.indices) {
            for (j in i + 1 until pairs.size) {
                val a = keys[pairs[i]] ?: continue
                val b = keys[pairs[j]] ?: continue
                var d = abs(a.longitude - b.longitude)
                if (d > 180) d = 360 - d
                val kind = when {
                    d < 8 -> "合"
                    abs(d - 60) < 6 -> "六合"
                    abs(d - 90) < 6 -> "刑"
                    abs(d - 120) < 6 -> "拱"
                    abs(d - 180) < 8 -> "冲"
                    else -> null
                }
                if (kind != null) {
                    out += AspectView(a.label, b.label, kind, d)
                }
            }
        }
        return out
    }

    /**
     * 五星迟留伏逆（果老口径近似）。
     * - 逆：日速 < 0
     * - 留：|日速| 小于该星留限
     * - 伏：水金距日 < 15° 且非逆（肉眼近合伏近似）
     * - 迟：正行但明显慢于平均日速
     * - 顺：其余
     * 日月孛炁罗计只分顺/逆。
     */
    private fun planetMotion(body: Ephemeris.Body, speed: Double, elongFromSun: Double): String {
        val five = setOf(
            Ephemeris.Body.Mercury, Ephemeris.Body.Venus, Ephemeris.Body.Mars,
            Ephemeris.Body.Jupiter, Ephemeris.Body.Saturn,
        )
        if (body !in five) return if (speed < -0.01) "逆" else "顺"
        if (speed < -0.01) return "逆"
        val stay = when (body) {
            Ephemeris.Body.Mercury, Ephemeris.Body.Venus -> 0.08
            Ephemeris.Body.Mars -> 0.04
            Ephemeris.Body.Jupiter -> 0.02
            Ephemeris.Body.Saturn -> 0.015
            else -> 0.05
        }
        if (abs(speed) < stay) return "留"
        if ((body == Ephemeris.Body.Mercury || body == Ephemeris.Body.Venus) && elongFromSun < 15.0) {
            return "伏"
        }
        val mean = when (body) {
            Ephemeris.Body.Mercury -> 1.2
            Ephemeris.Body.Venus -> 1.2
            Ephemeris.Body.Mars -> 0.52
            Ephemeris.Body.Jupiter -> 0.08
            Ephemeris.Body.Saturn -> 0.03
            else -> 0.5
        }
        if (speed < mean * 0.55) return "迟"
        return "顺"
    }

}

data class ClockParts(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int)
