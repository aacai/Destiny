package zhiqiu.app.destiny.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable
import zhiqiu.app.destiny.profile.Profile

/**
 * 命例（案例）的批注图片。
 *
 * 存储布局按「案例 id → 模块」两级目录组织，避免所有图片平铺在一起：
 * 实际文件落在 `<图片根>/images/<profileId>/<category>/<fileName>`，
 * [relativePath] 即其中 `images/<profileId>/<category>/<fileName>` 这一段（不含图片根），
 * 与备份 zip 包内路径保持一致，便于人工对照查找。
 *
 * @param id 图片 id（建议用随机 uuid）
 * @param profileId 所属案例 id，外键关联 [Profile]，案例被删除时级联删除其图片
 * @param category 模块目录名，如 `bazi`（八字）、`ziwei`（紫微）、`qizheng`（七政）
 * @param relativePath 相对图片根的路径，形如 `images/<profileId>/<category>/<fileName>`
 * @param createdAt 创建时间戳（毫秒），用于排序
 * @param sortOrder 同一模块内的显示顺序，越小越靠前
 */
@Entity(
    tableName = "ProfileImage",
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId")],
)
@Serializable
data class ProfileImage(
    @PrimaryKey val id: String,
    val profileId: String,
    val category: String,
    val relativePath: String,
    val createdAt: Long,
    val sortOrder: Int = 0,
)
