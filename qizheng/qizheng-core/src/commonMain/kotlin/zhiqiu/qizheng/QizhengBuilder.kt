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

        val stars = StarDefs
            .let { list ->
                if (config.useZiQi) list else list.filterNot { it.first == Ephemeris.Body.ZiQi }
            }
            .map { (body, key, label) ->
                val pos = Ephemeris.position(body, jd, config)
                val dispLon = disp(pos.longitude)
                val bi = if (eq) MingGong.equatorialBranchIndex(dispLon, zodiacOffset)
                else MingGong.longitudeToBranchIndex(pos.longitude, zodiacOffset)
                val xiu = XiuTable.locate(dispLon, xiuZero, config.xiuSystem, config.xiuFrame, eq)
                StarView(
                    key = key,
                    label = label,
                    longitude = dispLon,
                    speed = pos.speedDegPerDay,
                    branch = MingGong.Branches[bi],
                    xiu = xiu.name,
                    xiuDegree = xiu.degreeInXiu,
                    retro = pos.speedDegPerDay < -0.01,
                )
            }

        val sun = stars.first { it.key == "日" }
        val moon = stars.first { it.key == "月" }
        // 生时地支须用校正后的时刻（真太阳时模式下与钟表时不同）
        val hourBranch = MingGong.hourToBranchIndex(clock.hour, clock.minute)
        // 命宫：时加太阳顺数至卯（果老）；用户固定命宫时直接采用（MOIRA 式手动安命）
        val mingIdxAuto = MingGong.mingBranchIndex(
            sunLon = sun.longitude,
            hourBranchIndex = hourBranch,
            method = config.mingGongMethod,
            zodiacOffset = zodiacOffset,
            equatorial = eq,
        )
        val mingIdx = config.fixedMingBranch ?: mingIdxAuto
        // 身宫以**太阴**为体（果老派即月亮所在宫），与立命用太阳相对
        val shenIdx = if (config.useShenGong) {
            MingGong.shenBranchIndex(
                moonLon = moon.longitude,
                hourBranchIndex = hourBranch,
                method = config.shenGongMethod,
                zodiacOffset = zodiacOffset,
                equatorial = eq,
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
        // 立命度（果老立命法）：命宫度 = 命宫宫头黄经 + 太阳所在宫的宫内度。
        // 样例：丑宫头 270° + 太阳宫内度 5.94° = 275.94°（丑 5.94），落宿箕 4.30，与网站一致
        // （此前误用「命宫宫头」作立命度，得尾 13.47，与网站箕 4.30 不符）。
        val solarInsideDegree = sun.longitude % 30.0
        // 命度 = 太阳同络度；赤道制下太阳经度已是赤经，直接取用
        val mingCuspLon = if (eq) {
            sun.longitude
        } else {
            MingGong.branchIndexToLonStart(mingIdx, zodiacOffset) + solarInsideDegree
        }
        val mingXiu = XiuTable.locate(mingCuspLon, xiuZero, config.xiuSystem, config.xiuFrame, eq)
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
        )
        val aspects = buildAspects(stars)
        val patterns = buildPatterns(stars, palaces, eight.getYear().getEarthBranch().getName())
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

        val lunarMonthDay = runCatching {
            val name = lunarDay.toString()
            // LunarDay toString 常含「农历」等，截取月日段
            name.replace("农历", "").trim()
        }.getOrElse { lunarDay.toString() }
        val lunarLabel = "${clock.year}年${lunarMonthDay}${MingGong.Branches[hourBranch]}时"

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
            shenShaByBranch = shenDual.yearRing.indices.map { i ->
                (shenDual.yearRing[i] + shenDual.dayRing[i]).distinct().take(12)
            },
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
        val daLoc = XiuTable.locate(daLon, xiuZero, xiuSystem, xiuFrame, equatorial)
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
     * 政余喜格 / 忌格（《果老星宗》星格体系，按本盘数据逐条判定）。
     * 宫主取十二宫主宰（子丑土 寅亥木 卯戌火 辰酉金 巳申水 午日 未月）；
     * 垣位 = 星五行本宫（木寅亥 火卯戌 土子丑 金辰酉 水巳申，日午 月未）。
     * 条目持续补充，尚未覆盖全部星格。
     */
    private fun buildPatterns(
        stars: List<StarView>,
        palaces: List<PalaceView>,
        yearBranch: String,
    ): List<PatternView> {
        val byKey = stars.associateBy { it.key }
        val branchOfPalace = palaces.associate { it.name to it.branch }
        val lordKey = mapOf(
            "子" to "土", "丑" to "土", "寅" to "木", "亥" to "木",
            "卯" to "火", "戌" to "火", "辰" to "金", "酉" to "金",
            "巳" to "水", "申" to "水", "午" to "日", "未" to "月",
        )
        val yao = mapOf(
            "木" to listOf("寅", "亥"), "火" to listOf("卯", "戌"), "土" to listOf("子", "丑"),
            "金" to listOf("辰", "酉"), "水" to listOf("巳", "申"), "日" to listOf("午"), "月" to listOf("未"),
        )
        val elementOfStar = mapOf(
            "木" to "木", "火" to "火", "土" to "土", "金" to "金", "水" to "水",
            "日" to "日", "月" to "月", "罗" to "火", "计" to "土", "孛" to "水", "炁" to "木",
        )
        val starName = mapOf(
            "日" to "日", "月" to "月", "水" to "水", "金" to "金", "火" to "火", "木" to "木", "土" to "土",
            "罗" to "罗", "计" to "计", "孛" to "孛", "炁" to "紫炁",
        )
        fun star(key: String): StarView? = byKey[key]
        fun palaceBranch(name: String): String? = branchOfPalace[name]
        fun lordOf(palaceName: String): StarView? = lordKey[branchOfPalace[palaceName]]?.let { star(it) }
        fun inPalace(star: StarView, palaceName: String): Boolean = star.branch == branchOfPalace[palaceName]
        fun onYao(star: StarView): Boolean = yao[elementOfStar[star.key]]?.contains(star.branch) == true

        // 劫杀宫（年支三合局）：申子辰→巳 寅午戌→亥 巳酉丑→寅 亥卯未→申
        val jieSha = when (yearBranch) {
            "申", "子", "辰" -> "巳"
            "寅", "午", "戌" -> "亥"
            "巳", "酉", "丑" -> "寅"
            else -> "申"
        }

        // 喜格：星归垣得地
        val xiNames = listOf(
            Triple("日居日位", "日", "太阳居午，归垣得地，主贵显荣昌、光明磊落"),
            Triple("月居月垣", "月", "太阴居未，得垣主清洁秀气、母荫绵长"),
            Triple("木居木位", "木", "木居寅亥，得垣主仁慈忠厚、福寿绵长"),
            Triple("火居火位", "火", "火居卯戌，得垣主明察权威、声名显达"),
            Triple("土居土位", "土", "土居子丑，得垣主厚重诚信、家业安稳"),
            Triple("太白居垣", "金", "金星居辰酉金垣，主义气刚决、财源丰盈"),
            Triple("水居水位", "水", "水居巳申，得垣主智巧聪明、文思敏捷"),
        )
        val xi = buildList {
            // 五行归垣
            xiNames.forEach { (name, key, desc) ->
                star(key)?.let { st ->
                    if (onYao(st)) add(PatternView(name, true, "${starName[key]}居${yao[elementOfStar[key]]?.joinToString("、")}垣位", desc, true))
                }
            }
            // 罗计对分中分周天
            add(PatternView("罗计中分", true, "罗计对分周天", "罗计相距对宫、中分黄道，主威权统御、清浊自分", true))
            // 身宫落位
            star("月")?.let { moon ->
                if (inPalace(moon, "男女")) add(PatternView("身居男女", true, "身宫入男女宫", "身入子女之位，主子嗣得力、晚景安乐", true))
                if (inPalace(moon, "命宫")) add(PatternView("身居命宫", true, "身宫入命宫", "身心相守，主自立自强、性情坚确", true))
                if (inPalace(moon, "福德")) add(PatternView("身居福德", true, "身宫入福德宫", "身居福地，主福泽深厚、心性宽和", true))
            }
            // 宫主守位 / 朝命
            lordOf("命宫")?.let { if (inPalace(it, "命宫")) add(PatternView("命主守命", true, "命主星入命宫", "命主守命，主根基深固、自立坚确", true)) }
            lordOf("官禄")?.let { if (inPalace(it, "命宫")) add(PatternView("官星朝命", true, "官禄主入命宫", "官星朝命，主贵气加身、仕途顺遂", true)) }
            lordOf("田宅")?.let { if (inPalace(it, "田宅")) add(PatternView("田宅归垣", true, "田宅主守田宅宫", "田主守垣，主祖业丰隆、家道殷实", true)) }
            lordOf("夫妻")?.let { if (inPalace(it, "夫妻")) add(PatternView("妻守妻宫", true, "夫妻主守夫妻宫", "妻主守位，主婚姻稳固、内助得力", true)) }
            // 宫主关系
            run {
                val fu = lordOf("福德")
                val guan = lordOf("官禄")
                if (fu != null && guan != null) {
                    if (fu.branch == guan.branch) add(PatternView("福官会聚", true, "福德主与官禄主同宫", "福官同宫，主福贵相济、名利兼收", true))
                    if (inPalace(fu, "官禄") && inPalace(guan, "官禄")) add(PatternView("福官居官", true, "福德主与官禄主同居官禄", "福官同居官禄，主福厚官荣", true))
                }
                lordOf("田宅")?.let { tian -> if (guan != null && inPalace(tian, "官禄") && inPalace(guan, "官禄")) add(PatternView("田官会官", true, "田宅主与官禄主同入官禄", "田官同朝，主家业与官爵并进", true)) }
            }
            // 日月夹命：命宫前后两宫为日、月所在
            run {
                val sun = star("日")
                val moon = star("月")
                val mingBr = branchOfPalace["命宫"]
                if (sun != null && moon != null && mingBr != null) {
                    val prev = MingGong.Branches[(MingGong.Branches.indexOf(mingBr) + 11) % 12]
                    val next = MingGong.Branches[(MingGong.Branches.indexOf(mingBr) + 1) % 12]
                    val sunOn = sun.branch == prev || sun.branch == next
                    val moonOn = moon.branch == prev || moon.branch == next
                    if (sunOn && moonOn) add(PatternView("日月夹命", true, "日月在命宫前后夹辅", "日月夹命，主贵气拱卫、根基深厚", true))
                }
            }
        }

        // 忌格
        val ji = buildList {
            val sun = star("日")
            val moon = star("月")
            if (sun != null && moon != null && sun.branch == moon.branch) {
                add(PatternView("日躔月度", true, "日月同宫", "日月同宫相犯，主阴阳失位、得失参半", false))
            }
            star("土")?.let { tu ->
                if (tu.branch == "寅" || tu.branch == "亥") add(PatternView("土在木宫", true, "土居寅亥木宫受克", "土入木地受克，主家业多艰、根基动摇", false))
            }
            lordOf("田宅")?.let { tian ->
                val yaoList = yao[elementOfStar[tian.key]] ?: emptyList()
                if (tian.branch !in yaoList) add(PatternView("田失躔垣", true, "田宅主失躔垣位", "田主失垣，主祖业难守、置产辛劳", false))
            }
            lordOf("命宫")?.let { ming ->
                val yaoList = yao[elementOfStar[ming.key]] ?: emptyList()
                if (ming.branch !in yaoList) add(PatternView("命主失垣", true, "命主星失躔垣位", "命主失垣，主根基浅薄、运势起伏", false))
                if (inPalace(ming, "疾厄")) add(PatternView("命入疾厄", true, "命主入疾厄宫", "命入疾厄，主体弱多劳、忧思缠身", false))
            }
            lordOf("官禄")?.let { guan ->
                val yaoList = yao[elementOfStar[guan.key]] ?: emptyList()
                if (guan.branch !in yaoList) add(PatternView("官主失垣", true, "官禄主失躔垣位", "官主失垣，主仕途蹇滞、名利虚浮", false))
            }
            lordOf("夫妻")?.let { if (inPalace(it, "相貌")) add(PatternView("夫入相貌", true, "夫妻主入相貌宫", "夫妻主入相貌，主姻缘多阻、聚散难定", false)) }
            star("火")?.let { if (it.branch == "巳" || it.branch == "申") add(PatternView("火居水地", true, "火居巳申水地受克", "火入水地受克，主性急多困、进退失据", false)) }
            run {
                val sunBr = sun?.branch
                if (sunBr != null && listOf("罗", "计", "火").any { star(it)?.branch == sunBr }) {
                    add(PatternView("罗犯太阳", true, "罗计火同宫犯日", "火罗计犯日，主惊扰是非、贵气受损", false))
                }
            }
            moon?.let { mn ->
                if (mn.branch == jieSha) add(PatternView("身临劫杀", true, "身宫临劫杀之位", "身临劫杀，主平安中藏凶、防小人暗损", false))
                if (listOf("计", "孛").any { star(it)?.branch == mn.branch }) {
                    add(PatternView("计孛犯身", true, "计孛同宫犯身", "计孛犯身，主心绪多扰、暗耗不休", false))
                }
            }
            lordOf("财帛")?.let { if (inPalace(it, "迁移")) add(PatternView("财入迁移", true, "财帛主入迁移宫", "财主入迁移，主财随外出、聚散不定", false)) }
            lordOf("男女")?.let { if (inPalace(it, "迁移")) add(PatternView("嗣入迁移", true, "男女主入迁移宫", "嗣主入迁移，主子息远游、聚少离多", false)) }
            star("木")?.let { mu ->
                if (mu.branch == "辰" || mu.branch == "酉") add(PatternView("木逢金制", true, "木居辰酉金地受克", "木入金地受制，主仁德受损、进退两难", false))
            }
            run {
                val w = star("水")
                val t = star("土")
                if (w != null && t != null && w.branch == t.branch) {
                    add(PatternView("水土同宫", true, "水土同宫相克", "水土同宫相犯，主智谋多滞、劳碌难闲", false))
                }
            }
        }

        return xi + ji
    }

}

data class ClockParts(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int)
