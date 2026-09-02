package zhiqiu.app.destiny.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/** 从本地绝对路径加载图片（各平台用 Coil 支持的文件模型）。 */
@Composable
fun LocalImage(
    absolutePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    AsyncImage(
        model = localImageModel(absolutePath),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}

expect fun localImageModel(absolutePath: String): Any
