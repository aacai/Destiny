package zhiqiu.app.destiny.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Trash2
import kotlinx.io.files.Path
import zhiqiu.app.destiny.db.ProfileImage

/** 命例批注图片网格：展示缩略图，支持新增与删除。图片通过绝对路径交给 Coil3 加载。 */
@Composable
fun ImageGallery(
    images: List<ProfileImage>,
    absolutePathOf: (ProfileImage) -> String,
    onAdd: () -> Unit,
    onRemove: (ProfileImage) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(images, key = { it.id }) { img ->
            Box {
                AsyncImage(
                    model = Path(absolutePathOf(img)),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop,
                )
                IconButton(
                    onClick = { onRemove(img) },
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        imageVector = FeatherIcons.Trash2,
                        contentDescription = "删除图片",
                        tint = Color.White,
                    )
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FeatherIcons.Plus,
                    contentDescription = "添加图片",
                    tint = Color(0xFF8A8578),
                )
            }
        }
    }
}
