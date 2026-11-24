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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import zhiqiu.app.destiny.oinkcompat.Icon
import zhiqiu.app.destiny.oinkcompat.IconButton
import zhiqiu.app.destiny.oinkcompat.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import zhiqiu.app.destiny.i18n.AppStrings
import zhiqiu.app.destiny.ui.theme.DestinyTheme

/** 书架：跟随应用主题（日/夜）的浅底 + 彩色封面网格 */
@Composable
fun BookshelfScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    readerStore: ReaderStore,
) {
    var positions by remember { mutableStateOf<Map<String, ReaderPosition>>(emptyMap()) }
    LaunchedEffect(readerStore) {
        ReaderPrefs.attach(readerStore)
        positions = BookStore.catalog.associate { it.id to ReaderPrefs.loadPosition(it.id) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DestinyTheme.page)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = FeatherIcons.ArrowLeft, contentDescription = AppStrings.back, tint = DestinyTheme.ink)
            }
            Text(
                AppStrings.bookshelf,
                color = DestinyTheme.ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.padding(horizontal = 6.dp))
            Text(
                "命 · 卜 · 山 · 医 · 相",
                color = DestinyTheme.muted,
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
                BookCover(
                    meta = meta,
                    position = positions[meta.id],
                    onClick = { onOpen(meta.id) },
                )
            }
        }
    }
}

@Composable
private fun BookCover(
    meta: BookMeta,
    position: ReaderPosition?,
    onClick: () -> Unit,
) {
    val hasProgress = position != null && (position.section > 0 || position.item > 0)
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
            color = DestinyTheme.ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            if (hasProgress) {
                "${AppStrings.continueReading} · ${AppStrings.chapterShort((position?.section ?: 0) + 1)}"
            } else {
                "古籍 · 全本"
            },
            color = if (hasProgress) DestinyTheme.accent else DestinyTheme.muted,
            fontSize = 11.sp,
        )
    }
}
