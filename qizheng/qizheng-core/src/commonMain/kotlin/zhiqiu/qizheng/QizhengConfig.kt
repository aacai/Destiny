package zhiqiu.qizheng

/**
 * 七政四余「盘制」配置：把各流派的分歧点集中声明，贯穿
 * [QizhengBuilder.build] → 各计算模块，取代原先散落的硬编码常量与全局可变状态。
 *
 * 设计原则：
 * 1. **默认值 = 黄金样例口径**（回归今宿·黄道），故不传配置时行为与改造前完全一致；
 * 2. 所有分歧点都是**纯数据**，计算函数不得再持有跨调用的可变状态。
 */

// ------------------------------------------------------------------ 黄道基准

/** 十二宫的黄道基准：是否随岁差漂移 */
enum class ZodiacMode(val label: String) {
    /** 回归（热带）：春分点恒为戌宫 0°，十二宫随岁差相对恒星西移 */
    TROPICAL("回归"),

    /** 恒星：十二宫锚定恒星，春分点相对宫位东移，需 ayanamsa 岁差校正 */
    SIDEREAL("恒星"),
}

/**
 * 恒星黄道岁差模型（ayanamsa），仅在 [ZodiacMode.SIDEREAL] 下生效。
 *
 * [offsetAtJ2000]：J2000.0 时春分点相对恒星零点的后退量（度）。
 * 统一按总岁差 [XiuTable.PrecessionDegPerCentury] 外推；
 * 各模型对岁差率的定义本有细微差别，此处取标准近似。
 *
 * ⚠️ **数值未经权威星历表核对**，为 J2000 基准的常用近似值，
 * 不同软件/机构间常有 0.1° 量级的出入。若需与特定软件对盘，请核对后修正此处。
 */
enum class Ayanamsa(val label: String, val offsetAtJ2000: Double) {
    NONE("无", 0.0),

    /** 印度官方标准（Chitrapaksha），锚定角宿一 Spica 于黄经 180° */
    LAHIRI("Lahiri", 23.85306),

    /** KP（Krishnamurti Paddhati），近乎 Lahiri，为 KP 次主星系统微调 */
    KRISHNAMURTI("Krishnamurti", 23.749),

    /** B.V. Raman 独立推算，零点约在公元 397 年 */
    RAMAN("Raman", 22.370),

    /** 西方恒星标准，锚定 Aldebaran–Antares 轴，零点约在公元 221 年 */
    FAGAN_BRADLEY("Fagan/Bradley", 24.042),
    ;

    /**
     * 距 J2000 [centuries] 世纪时的岁差量（度）。
     *
     * 采用 Lahiri 官方多项式 `A(T) = 23.85306 + 1.39722·T + 0.00018·T² − 0.000005·T³`。
     * 各系统年变率一致（约 1.39722°/世纪 ≈ 50.3″/年），差别只在常数项。
     */
    fun offset(centuries: Double): Double {
        val t = centuries
        return offsetAtJ2000 + 1.39722 * t + 0.00018 * t * t - 0.000005 * t * t * t
    }
}

// ------------------------------------------------------------------ 二十八宿

/**
 * **宿内深度的读数量纲**（不影响宿界位置）。
 *
 * 两套同源于汉代宿度表，**宿界在盘上的角度位置完全一致** —— 宿界由
 * [XiuTable] 的今宿坐标系给出，已含岁差修正。差别只在宿内读数：
 * - [ANCIENT]：古度 365.25 制，如「箕四」「斗 19.4」—— 传统文献口径，**默认**
 * - [MODERN]：今度 360 制，如「箕 4.0」「斗 19.1」
 */
enum class XiuSystem(val label: String) {
    ANCIENT("古度读数"),
    MODERN("今度读数"),
}

/**
 * **宿界框架**：二十八宿在盘上的分界用什么宽度、锚在何处。
 *
 * | 框架 | 宿宽 | 角宿零点锚 |
 * |---|---|---|
 * | [MODERN] | 今宿宽（实测今宿界，见 [XiuTable.startsModern]） | 随岁差东移（回归黄经） |
 * | [ANCIENT_SCALED] | 古宿宽 × 360/365.25（回归古宿） | 随岁差东移（回归黄经） |
 * | [ANCIENT_J2000] | 古宿宽 × 360/365.25 | 固定 J2000 恒星锚（古宿岁差） |
 */
enum class XiuFrame(val label: String) {
    MODERN("今宿界"),
    ANCIENT_SCALED("回归古宿"),
    ANCIENT_J2000("古宿岁差"),
}

// ------------------------------------------------------------------ 宫位与立命

/** 宫位系统。目前七政四余传统只用整宫制，此处预留扩展 */
enum class HouseSystem(val label: String) {
    /** 每宫 30° 等分，黄经 / 30 落地支 */
    WHOLE_SIGN("整宫制"),
}

/**
 * 立命法：命宫由「生时 + 太阳宫」推得，各派的分歧在**顺数至何支**。
 *
 * | 制 | 规则 | 备注 |
 * |---|---|---|
 * | [GUOLAO] | 生时加太阳宫，顺数至**卯** | 《果老星宗》正统，本项目默认 |
 * | [SHEN_TO_YOU] | 生时加太阳宫，顺数至**酉** | 琴堂「逢酉安命」一说，⚠️ 待与参考盘核对 |
 */
enum class MingGongMethod(val label: String, val targetBranchIndex: Int) {
    GUOLAO("果老·顺至卯", 3),
    SHEN_TO_YOU("琴堂·顺至酉", 9),
}

/**
 * 身宫定法。
 *
 * 据《七政四余》考据（引江晓原等研究）：身宫概念移植自印度占星学的
 * 月亮身宫盘 **Chandra Chart**；**「琴堂派以逢酉安身论身宫，然果老派或耶律派的
 * 身宫取法和 Chandra Chart 一模一样」**。
 *
 * | 制 | 规则 | 流派 |
 * |---|---|---|
 * | [GUOLAO_MOON] | 身宫 = **太阴（月亮）所在宫** | 果老派、耶律派（= Chandra Chart），**本项目默认** |
 * | [QINTANG_YOU] | 太阴宫起生时，**逆**数至酉 | 琴堂派「逢酉安身」 |
 */
enum class ShenGongMethod(val label: String) {
    GUOLAO_MOON("果老·太阴宫"),
    QINTANG_YOU("琴堂·太阴逆至酉"),
}

// ------------------------------------------------------------------ 时间基准

/**
 * 计时基准。出生时刻是钟表读数，排盘可用平太阳时或校正到真太阳时。
 *
 * 真太阳时 = 平太阳时 + 经度差校正 + 均时差(EoT)。
 * 经度差按出生地经度相对**时区标准经度**（本项目恒为东经 120°）每度 4 分钟。
 */
enum class ClockMode(val label: String) {
    /** 钟表读数直接用，不作校正 */
    CIVIL("平太阳时"),

    /** 按出生地经度 + 均时差校正为真太阳时 */
    TRUE_SOLAR("真太阳时"),
}

// ------------------------------------------------------------------ 四余

/**
 * 罗睺 / 计都的交点约定。**两派定义正好相反，切换后罗、计对调。**
 *
 * | 约定 | 罗睺 | 计都 | 出处 |
 * |---|---|---|---|
 * | [TRADITIONAL] | 降交点（南交点） | 升交点（北交点） | 沈括《梦溪笔谈》、赵友钦《革象新书》，七政四余正统 |
 * | [INDIAN] | 升交点（北交点） | 降交点（南交点） | 印度 Rahu/Ketu 原义，近现代排盘软件多用 |
 *
 * 黄金样例 §6.3.1 已拍板取 [TRADITIONAL]。
 */
enum class NodeConvention(val label: String) {
    TRADITIONAL("传统·罗降交"),
    INDIAN("印度·罗升交"),
}

/**
 * 紫炁：传统虚星，非天文实点，**周期与行向各家不一**。
 *
 * [periodYears]：行一周天的年数；[retrograde]：是否逆行。
 */
enum class ZiQiMode(val label: String, val periodYears: Double, val retrograde: Boolean) {
    /** 主流：28 年顺行，历元 1975-03-13 紫炁在 230.5°（已与参考盘落翼宿校对） */
    YEARS_28("28年顺", 28.0, false),

    /** 闰余派：29 年顺行（取闰月之余）。⚠️ 有此一说，**未经核对** */
    YEARS_29("29年顺", 29.0, false),

    /** 逆行派：28 年逆行。⚠️ 有此一说，**未经核对** */
    YEARS_28_REV("28年逆", 28.0, true),
}

// ------------------------------------------------------------------ 神煞

/** 神煞套件 */
enum class ShenShaSet(val label: String) {
    /** 现行一套：外圈年支岁煞 + 内圈日干贵人 */
    GUOLAO("果老神煞"),

    /** 不排神煞 */
    NONE("不排"),
}

// ------------------------------------------------------------------ 亮度

/**
 * 庙旺利陷（星曜亮度）。
 *
 * ⚠️ 庙旺表各传本互有出入，本表取**通行版本**，尚未逐条与特定善本核对。
 * 故默认 [OFF]，由用户自行开启；如与所用流派不符，改 [Dignity.table] 即可。
 */
enum class DignityMode(val label: String) {
    OFF("不显示"),
    GUOLAO("果老庙旺"),
}

// ------------------------------------------------------------------ 配置主体

data class QizhengConfig(
    // 黄道
    val zodiac: ZodiacMode = ZodiacMode.TROPICAL,
    val ayanamsa: Ayanamsa = Ayanamsa.LAHIRI,
    /** 赤道恒星制：全盘按赤经布（宫界/宿界/星曜均转赤经）；zodiac 需为恒星 */
    val equatorial: Boolean = false,

    // 二十八宿（宿界框架 + 宿内读数量纲）
    val xiuSystem: XiuSystem = XiuSystem.ANCIENT,
    val xiuFrame: XiuFrame = XiuFrame.MODERN,

    // 宫位与立命
    val houseSystem: HouseSystem = HouseSystem.WHOLE_SIGN,
    val mingGongMethod: MingGongMethod = MingGongMethod.GUOLAO,
    /** 固定命宫（支索引 0..11）；null = 按「时加太阳数至卯」自动推 */
    val fixedMingBranch: Int? = null,
    val useShenGong: Boolean = true,
    /** 身宫定法，见 [ShenGongMethod]；默认果老派（= 月亮所在宫） */
    val shenGongMethod: ShenGongMethod = ShenGongMethod.GUOLAO_MOON,

    // 化曜
    val huaYaoSchool: HuaYaoSchool = HuaYaoSchool.GUOLAO,

    // 时间
    val clockMode: ClockMode = ClockMode.CIVIL,

    // 四余
    val nodeConvention: NodeConvention = NodeConvention.TRADITIONAL,
    val useZiQi: Boolean = true,
    val ziQiMode: ZiQiMode = ZiQiMode.YEARS_28,

    // 神煞
    val shenShaSet: ShenShaSet = ShenShaSet.GUOLAO,
    val showYearShenSha: Boolean = true,
    val showDayShenSha: Boolean = true,
    /** 每支最多显示几个神煞（0 = 不限） */
    val shenShaMaxPerBranch: Int = 10,

    // 亮度
    val dignityMode: DignityMode = DignityMode.OFF,
) {
    /**
     * 排盘结果顶栏显示的盘制名，如「回归今宿·黄道」。
     * 宿界框架 [xiuFrame] 计入盘制名（今宿/回归古宿/古宿岁差）。
     */
    val displayName: String
        get() = buildString {
            append(
                when {
                    equatorial -> "赤道恒星"
                    zodiac == ZodiacMode.SIDEREAL -> "恒星"
                    else -> "回归"
                }
            )
            append(
                when (xiuFrame) {
                    XiuFrame.MODERN -> "今宿"
                    XiuFrame.ANCIENT_SCALED -> "古宿"
                    XiuFrame.ANCIENT_J2000 -> "古宿岁差"
                }
            )
            if (!equatorial) append("·黄道")
        }

    /** 盘制详情，逐项列出本盘采用的口径 */
    val detail: String
        get() = buildList {
            add(
                when {
                    equatorial -> "赤经布盘(恒星宫界)"
                    zodiac == ZodiacMode.SIDEREAL -> "恒星黄道(${ayanamsa.label})"
                    else -> "热带黄道"
                }
            )
            add(xiuFrame.label)
            add(xiuSystem.label)
            add(mingGongMethod.label + "立命")
            if (fixedMingBranch != null) add("固定命宫${"子丑寅卯辰巳午未申酉戌亥"[fixedMingBranch]}")
            add("洞微大限")
            add(
                if (nodeConvention == NodeConvention.TRADITIONAL) "罗降交/计升交" else "罗升交/计降交"
            )
            add(if (useZiQi) "孛远点/炁${ziQiMode.label}" else "孛远点/无紫炁")
            if (shenShaSet == ShenShaSet.NONE) add("无神煞")
            if (!useShenGong) add("无身宫")
            if (clockMode == ClockMode.TRUE_SOLAR) add("真太阳时")
            if (dignityMode != DignityMode.OFF) add("庙旺")
            add(huaYaoSchool.label)
        }.joinToString(" · ")
}

/**
 * 黄道偏移（ayanamsa，度）：回归制恒为 0；恒星制按「J2000 起的世纪数」外推。
 * 传入的是 TT 世纪数（见 [Ephemeris.centuriesTt]）。
 */
fun QizhengConfig.zodiacOffset(centuriesTt: Double): Double =
    if (zodiac == ZodiacMode.SIDEREAL) ayanamsa.offset(centuriesTt) else 0.0

// ------------------------------------------------------------------ 预置盘制

object PanZhiPresets {
    /** 果老星宗（默认盘制）：回归 · 古度读数 · 传统罗计 —— 黄金样例口径 */
    val GuoLao = QizhengConfig()

    /** 今度读数：宿内深度改用 360 度制（如「箕 4.0」，默认为古度「箕四」） */
    val ModernReading = QizhengConfig(xiuSystem = XiuSystem.MODERN)

    /** 回归古宿：宿界改用古宿宽（365.25 古度折 360 今度），锚仍随岁差东移 */
    val Ancient = QizhengConfig(xiuFrame = XiuFrame.ANCIENT_SCALED)

    /** 古宿岁差：古宿宽 + J2000 恒星锚（不随回归岁差漂移） */
    val AncientJ2000 = QizhengConfig(xiuFrame = XiuFrame.ANCIENT_J2000)

    /** 郑案今宿：恒星黄道（Lahiri 岁差）+ 今宿界，即 MOIRA「郑氏星案恒星制」口径 */
    val ZhengAn = QizhengConfig(
        zodiac = ZodiacMode.SIDEREAL,
        ayanamsa = Ayanamsa.LAHIRI,
    )

    /** 赤道恒星：恒星宫界转赤经，全盘按赤经布（星曜/宿界/宫界均按赤经） */
    val Equatorial = QizhengConfig(
        zodiac = ZodiacMode.SIDEREAL,
        ayanamsa = Ayanamsa.LAHIRI,
        equatorial = true,
    )

    /** 旧名兼容：= [ZhengAn] */
    val Sidereal = ZhengAn

    /** 恒星 + 西方岁差模型 */
    val SiderealFagan = QizhengConfig(
        zodiac = ZodiacMode.SIDEREAL,
        ayanamsa = Ayanamsa.FAGAN_BRADLEY,
    )

    /** 印度制：恒星黄道 + 印度罗计（罗=升交点） */
    val Indian = QizhengConfig(
        zodiac = ZodiacMode.SIDEREAL,
        ayanamsa = Ayanamsa.LAHIRI,
        nodeConvention = NodeConvention.INDIAN,
    )

    /** 真太阳时：按出生地经度 + 均时差校正时刻 */
    val TrueSolar = QizhengConfig(clockMode = ClockMode.TRUE_SOLAR)

    /** 简版：不排神煞、不用紫炁（只留七政 + 孛） */
    val Minimal = QizhengConfig(shenShaSet = ShenShaSet.NONE, useZiQi = false)

    /**
     * 庙旺版：在果老盘制上显示庙旺利陷。
     * 🛑 **数据不可靠，暂不从 UI 暴露**（未加入 [all]），详见 [Dignity] 的核对说明。
     */
    val WithDignity = QizhengConfig(dignityMode = DignityMode.GUOLAO)

    /** 可选盘制清单（名称 → 配置），供 UI 下拉/弹窗使用 */
    val all: List<Pair<String, QizhengConfig>> = listOf(
        "果老星宗" to GuoLao,
        "回归今宿" to ModernReading,
        "回归古宿" to Ancient,
        "古宿岁差" to AncientJ2000,
        "郑案今宿" to ZhengAn,
        "赤道恒星" to Equatorial,
        "真太阳时" to TrueSolar,
        "简版（无神煞）" to Minimal,
        // 🛑 「庙旺版」已撤下：庙旺表与《果老星宗》宫名诗冲突，见 Dignity 的说明
    )

    /** 默认盘制 */
    val default: QizhengConfig get() = GuoLao

    /** 默认盘制名，UI 状态的初值 */
    const val defaultName = "果老星宗"

    /**
     * 按名取盘制；未命中返回 null。
     * UI 只持久化盘制名（字符串），配置本身由此查回，避免序列化整个配置。
     */
    fun byName(name: String): QizhengConfig? = all.firstOrNull { it.first == name }?.second

    /** 按名取盘制，未命中时退回默认 */
    fun byNameOrDefault(name: String): QizhengConfig = byName(name) ?: default
}
