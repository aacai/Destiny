package zhiqiu.app.destiny.qizheng

import com.tyme.solar.SolarTime
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.iztro.calendar.Calendar
import zhiqiu.qizheng.ClockParts
import zhiqiu.qizheng.PanZhiPresets
import zhiqiu.qizheng.QizhengBuilder
import zhiqiu.qizheng.QizhengChart
import zhiqiu.qizheng.QizhengConfig
import zhiqiu.qizheng.QizhengDefaults

/** 宿主档案（Profile）→ 七政排盘的薄适配层；核心模块不认识 Profile */
fun buildFromProfile(
    profile: Profile,
    overrideYear: Int? = null,
    overrideMonth: Int? = null,
    overrideDay: Int? = null,
    overrideHour: Int? = null,
    overrideMinute: Int? = null,
    note: String = "",
    config: QizhengConfig = PanZhiPresets.default,
): QizhengChart {
    val (y, m, d, h, mi) = profile.resolveClock()
    val cy = overrideYear ?: y
    val age = if (overrideYear != null) (cy - y).coerceAtLeast(0) else 0
    return QizhengBuilder.build(
        year = cy,
        month = overrideMonth ?: m,
        day = overrideDay ?: d,
        hour = overrideHour ?: h,
        minute = overrideMinute ?: mi,
        gender = profile.gender,
        lon = profile.longitude ?: QizhengDefaults.LonBeijing,
        lat = profile.latitude ?: QizhengDefaults.LatBeijing,
        note = note,
        ageYears = age,
        config = config,
    )
}

fun Profile.resolveClock(): ClockParts {
    val solar = if (birthdayType == "lunar") {
        Calendar.lunar2solar(birthday, isLeapMonth).toString()
    } else {
        birthday
    }
    val parts = Calendar.normalizeDateStr(solar)
    val h = clockHour ?: maxOf(timeIndex * 2 - 1, 0)
    val mi = clockMinute ?: 30
    return ClockParts(parts[0], parts[1], parts[2], h, mi)
}

