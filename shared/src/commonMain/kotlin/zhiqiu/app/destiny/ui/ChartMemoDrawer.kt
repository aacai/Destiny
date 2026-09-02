package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Camera
import compose.icons.feathericons.X
import zhiqiu.app.destiny.db.ProfileImage

private val DrawerBg = Color(0xFF1E1C1A)
private val DrawerSurface = Color(0xFF2B2826)
private val DrawerInk = Color(0xFFF2EDE6)
private val DrawerMuted = Color(0xFF9A948C)
private val DrawerAccent = Color(0xFFC9A66B)

/** 盘面页底部便笺抽屉：深色纸面风格，下滑可收起。 */
@Composable
fun ChartMemoDrawer(
    sheetTitle: String,
    note: String,
    images: List<ProfileImage>,
    resolvePath: (ProfileImage) -> String,
    onNoteChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onDeleteImage: (ProfileImage) -> Unit,
    onClose: () -> Unit,
) {
    var previewPath by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.38f))
                .clickable(onClick = onClose),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(DrawerBg)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, drag ->
                        if (drag > 28f) onClose()
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DrawerMuted.copy(alpha = 0.5f)),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    sheetTitle,
                    color = DrawerAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClose) {
                    Text("收起", color = DrawerMuted, fontSize = 14.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BasicTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DrawerSurface)
                        .padding(14.dp),
                    textStyle = TextStyle(
                        color = DrawerInk,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                    ),
                    decorationBox = { inner ->
                        if (note.isEmpty()) {
                            Text(
                                "随手记下这一盘的要点…",
                                color = DrawerMuted,
                                fontSize = 15.sp,
                                lineHeight = 23.sp,
                            )
                        }
                        inner()
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("附图", color = DrawerMuted, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onPickImage)
                            .background(DrawerSurface)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Camera,
                            contentDescription = null,
                            tint = DrawerAccent,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("插入", color = DrawerAccent, fontSize = 12.sp)
                    }
                }

                if (images.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(images, key = { it.id }) { image ->
                            val path = resolvePath(image)
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { previewPath = path },
                            ) {
                                LocalImage(
                                    absolutePath = path,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clickable { onDeleteImage(image) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = FeatherIcons.X,
                                        contentDescription = "删除",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ImagePreviewOverlay(absolutePath = previewPath, onDismiss = { previewPath = null })
}
