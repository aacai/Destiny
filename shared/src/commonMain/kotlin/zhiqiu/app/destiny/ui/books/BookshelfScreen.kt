package zhiqiu.app.destiny.ui.books

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import zhiqiu.app.destiny.ui.books.BookPalette.Ink
import zhiqiu.app.destiny.ui.books.BookPalette.Muted
import zhiqiu.app.destiny.ui.books.BookPalette.PageBg

/** 书架：番茄风浅灰底 + 彩色封面网格 */
internal object BookPalette {
    val PageBg = Color(0xFFF6F5F2)
    val Ink = Color(0xFF222222)
    val Muted = Color(0xFF8A8578)
}
@Composable
fun BookshelfScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = FeatherIcons.ArrowLeft, contentDescription = "返回", tint = Ink)
            }
            Text(
                "书架",
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
            Text(
                "命 · 卜 · 山 · 医 · 相",
                color = Muted,
                fontSize = 12.sp,
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(BookStore.catalog, key = { it.id }) { meta ->
                BookCover(meta = meta, onClick = { onOpen(meta.id) })
            }
        }
    }
}

@Composable
private fun BookCover(meta: BookMeta, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(meta.coverFrom), Color(meta.coverTo)),
                    ),
                    RoundedCornerShape(8.dp),
                )
                .padding(14.dp),
        ) {
            Column {
                Text(
                    meta.category,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    meta.title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                )
            }
            Text(
                "卷",
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            meta.title,
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            "古籍 · 全本",
            color = Muted,
            fontSize = 11.sp,
        )
    }
}
