package zhiqiu.app.destiny.bazi.original

import com.tyme.eightchar.EightChar
import com.tyme.solar.SolarTime
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.iztro.bazi.flow.FlowBuilder
import zhiqiu.iztro.bazi.flow.FlowChart
import zhiqiu.iztro.bazi.flow.FlowSelection
import zhiqiu.iztro.bazi.original.OriginalBuilder
import zhiqiu.iztro.bazi.original.OriginalChart
import zhiqiu.iztro.bazi.original.formatBirthTermLabel
import zhiqiu.iztro.bazi.original.formatSolarTimeLabel
import zhiqiu.iztro.calendar.Calendar

/**
 * 宿主档案（Profile）→ 八字模块的薄适配层。
 * 八字模块（bazi-core/bazi-ui）本身不认识 Profile，方便独立开源。
 */
fun Profile.toSolarTime(): SolarTime {
    val solar = if (birthdayType == "lunar") {
        Calendar.lunar2solar(birthday, isLeapMonth).toString()
    } else {
        birthday
    }
    val parts = Calendar.normalizeDateStr(solar)
    val hour = maxOf(timeIndex * 2 - 1, 0)
    // 时辰取中点，便于与节气时刻做「后几天几小时」差算
    return SolarTime(parts[0], parts[1], parts[2], hour, 30, 0)
}

fun Profile.toEightChar(): EightChar =
    toSolarTime().getLunarHour().getEightChar()

fun Profile.toOriginalChart(): OriginalChart {
    val solarTime = toSolarTime()
    val eightChar = solarTime.getLunarHour().getEightChar()

    return OriginalBuilder.build(
        eightChar = eightChar,
        gender = gender,
        solarLabel = formatSolarTimeLabel(solarTime),
        termLabel = formatBirthTermLabel(solarTime),
    )
}

fun Profile.toFlowChart(selection: FlowSelection? = null): FlowChart =
    FlowBuilder.build(toSolarTime(), gender, selection)
