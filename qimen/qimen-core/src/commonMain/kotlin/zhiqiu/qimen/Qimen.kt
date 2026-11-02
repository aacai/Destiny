package zhiqiu.qimen

import com.tyme.solar.SolarTime
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

fun cast(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    plate: PlateKind,
    boundary: DayBoundary = DayBoundary.Hour23,
    method: JuMethod = JuMethod.ChaiBu,
): QimenChart {
    val time = SolarTime(year, month, day, hour, minute, 0)
    val ju = resolveJu(year, month, day, hour, minute, boundary, method)
    val pillars = pillarsOf(time, boundary)
    val hourPillar = pillars[3]
    val laid = arrange(
        ju = ju.ju,
        yang = ju.dun == Dun.Yang,
        hourStem = hourPillar.stem,
        hourBranch = hourPillar.branch,
        plate = plate,
    )
    return QimenChart(
        solarText = formatSolar(year, month, day, hour, minute),
        pillars = pillars,
        termName = ju.termName,
        yuan = ju.yuan,
        dun = ju.dun,
        ju = ju.ju,
        plate = plate,
        boundary = boundary,
        xunLabel = laid.xunLabel,
        dunStem = laid.dunStem,
        kong = laid.kong,
        horse = laid.horse,
        zhiFu = laid.zhiFu,
        zhiShi = laid.zhiShi,
        centerEarth = laid.centerEarth,
        cells = laid.cells,
    )
}

fun castNow(
    plate: PlateKind = PlateKind.Zhuan,
    boundary: DayBoundary = DayBoundary.Hour23,
): QimenChart {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return cast(now.year, now.month.number, now.day, now.hour, now.minute, plate, boundary)
}

private fun formatSolar(year: Int, month: Int, day: Int, hour: Int, minute: Int): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-" +
        "${day.toString().padStart(2, '0')} ${hour.toString().padStart(2, '0')}:" +
        minute.toString().padStart(2, '0')
