package zhiqiu.app.destiny.profile

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

/** 档案类型常量；字符串直存 Room，避免枚举迁移麻烦。 */
object CaseType {
    const val BAZI = "bazi"
    const val LIUYAO = "liuyao"
    const val XIAOLIUREN = "xiaoliuren"
    const val MEIHUA = "meihua"
    const val QIMEN = "qimen"

    val ALL = listOf(BAZI, LIUYAO, XIAOLIUREN, MEIHUA, QIMEN)

    fun label(type: String): String = when (type) {
        BAZI -> "命例"
        LIUYAO -> "六爻"
        XIAOLIUREN -> "小六壬"
        MEIHUA -> "梅花"
        QIMEN -> "奇门"
        else -> "档案"
    }
}

@Entity
@Serializable
data class Profile(
    @PrimaryKey val id: String,
    val name: String,
    val gender: String,
    val birthdayType: String,
    val birthday: String,
    val timeIndex: Int,
    val isLeapMonth: Boolean = false,
    val fixLeap: Boolean = true,
    val solarDateDisplay: String = "",
    val baziSummary: String = "",
    /** 出生地经度；null 则七政用默认北京 */
    val longitude: Double? = null,
    /** 出生地纬度 */
    val latitude: Double? = null,
    /** 精确钟点（0-23）；null 则用 timeIndex 中点 */
    val clockHour: Int? = null,
    /** 精确分钟（0-59） */
    val clockMinute: Int? = null,
    /**
     * 七政盘制名（对应 [zhiqiu.qizheng.PanZhiPresets.all] 的名称）。
     * 空串表示用默认盘制；只存名字，配置本身由 PanZhiPresets 查回。
     */
    val qizhengPanZhi: String = "",
    /** 批注（命例级：详细批注文本），与八字/紫微/七政无关 */
    val note: String = "",
    /** 案例类型：见 [CaseType]。决定档案页筛选、详情页布局。 */
    val caseType: String = CaseType.BAZI,
    /**
     * 结构化起课数据（JSON）。命例为空；占卜案例存重建盘面所需的最小输入，
     * 由各起课模块定义的序列化 DTO 承载，App.kt 负责编解码。
     */
    val payload: String = "",
    /** 分组：默认/家人/朋友/案例/名人 */
    val groupName: String = "默认",
    val createdAt: Long,
    val updatedAt: Long,
)
