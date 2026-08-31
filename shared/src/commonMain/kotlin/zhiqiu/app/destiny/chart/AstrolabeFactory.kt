package zhiqiu.app.destiny.chart

import zhiqiu.app.destiny.profile.Profile
import zhiqiu.iztro.astro.byLunar
import zhiqiu.iztro.astro.bySolar
import zhiqiu.iztro.calendar.Calendar
import zhiqiu.iztro.model.Astrolabe

fun Profile.toAstrolabe(): Astrolabe =
    if (birthdayType == "lunar") {
        byLunar(
            lunarDateStr = birthday,
            timeIndex = timeIndex,
            gender = gender,
            isLeapMonth = isLeapMonth,
            fixLeap = fixLeap,
        )
    } else {
        bySolar(
            solarDate = birthday,
            timeIndex = timeIndex,
            gender = gender,
            fixLeap = fixLeap,
        )
    }

fun Profile.resolvedSolarDate(): String =
    if (birthdayType == "lunar") {
        Calendar.lunar2solar(birthday, isLeapMonth).toString()
    } else {
        birthday
    }

fun Profile.computeBaziSummary(): String =
    toAstrolabe().rawDates.chineseDate.format()
