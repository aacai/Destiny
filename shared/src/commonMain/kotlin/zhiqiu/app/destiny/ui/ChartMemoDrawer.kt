package zhiqiu.app.destiny.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import zhiqiu.app.destiny.oinkcompat.Icon
import zhiqiu.app.destiny.oinkcompat.Text
import zhiqiu.app.destiny.oinkcompat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronDown
import compose.icons.feathericons.ChevronUp
import compose.icons.feathericons.Image
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import zhiqiu.app.destiny.db.ProfileImage
import zhiqiu.app.destiny.ui.note.NoteAttachmentStrip
import zhiqiu.app.destiny.ui.note.NoteEditorState
import zhiqiu.app.destiny.ui.note.NoteSaveStatusLabel

private val SheetBg = Color(0xFF1E1C1A)
private val SheetSurface = Color(0xFF2B2826)
private val SheetInk = Color(0xFFF2EDE6)
private val SheetMuted = Color(0xFF9A948C)
private val SheetAccent = Color(0xFFC9A66B)

private const val PEEK_FRACTION = 0.48f
private const val EXPANDED_FRACTION = 0.96f

/**
 * 批注写字 Sheet：半屏 / 上拉全屏。
 * 打字只改本地草稿，不自动 upsert，避免盘面页跟着重组卡顿；完成/关闭时再 flush。
 */
@Composable
fun ChartMemoDrawer(
    chartLabel: String,
    noteState: NoteEditorState,
    images: List<ProfileImage>,
    resolvePath: (ProfileImage) -> String,
    onPickImage: () -> Unit,
    onDeleteImage: (ProfileImage) -> Unit,
    onOpenFullMemo: () -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showAttachments by remember { mutableStateOf(false) }
    val fraction = remember { Animatable(PEEK_FRACTION) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    // 用离散状态驱动字号，避免 fraction 每帧带动 TextField 重组
    var expanded by remember { mutableStateOf(false) }
    var sheetHeightFraction by remember { mutableFloatStateOf(PEEK_FRACTION) }

    LaunchedEffect(Unit) {
        fraction.snapTo(PEEK_FRACTION * 0.92f)
        fraction.animateTo(PEEK_FRACTION, spring(stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(fraction) {
        snapshotFlow { fraction.value }
            .collect { sheetHeightFraction = it }
    }
    LaunchedEffect(fraction) {
        snapshotFlow { fraction.value >= (PEEK_FRACTION + EXPANDED_FRACTION) / 2f }
            .distinctUntilChanged()
            .collect { expanded = it }
    }

    fun dismiss() {
        noteState.flush()
        scope.launch {
            fraction.animateTo(0.02f, spring(stiffness = Spring.StiffnessMedium))
            onClose()
        }
    }

    fun snapTo(target: Float) {
        scope.launch {
            fraction.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    fun onDragEnd() {
        val v = fraction.value
        val dy = dragAccum
        dragAccum = 0f
        when {
            v < PEEK_FRACTION - 0.12f || (v <= PEEK_FRACTION + 0.05f && dy > 80f) -> dismiss()
            v > (PEEK_FRACTION + EXPANDED_FRACTION) / 2f || dy < -80f -> snapTo(EXPANDED_FRACTION)
            else -> snapTo(PEEK_FRACTION)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f + 0.42f * sheetHeightFraction))
                .clickable(onClick = ::dismiss),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(sheetHeightFraction.coerceIn(0.02f, 1f))
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(SheetBg),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(maxH) {
                        detectVerticalDragGestures(
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onVerticalDrag = { _, dragAmount ->
                                dragAccum += dragAmount
                                val deltaFraction = -dragAmount / maxH
                                val next = (fraction.value + deltaFraction).coerceIn(0.02f, EXPANDED_FRACTION)
                                scope.launch { fraction.snapTo(next) }
                            },
                        )
                    }
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SheetMuted.copy(alpha = 0.55f)),
                )
                Text(
                    text = if (expanded) "下滑可缩小" else "上拉全屏书写",
                    color = SheetMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "写批注",
                        color = SheetAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        chartLabel,
                        color = SheetMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                NoteSaveStatusLabel(noteState.status, light = false)
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = {
                    if (expanded) snapTo(PEEK_FRACTION) else snapTo(EXPANDED_FRACTION)
                }) {
                    Icon(
                        imageVector = if (expanded) FeatherIcons.ChevronDown else FeatherIcons.ChevronUp,
                        contentDescription = if (expanded) "缩小" else "全屏",
                        tint = SheetMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
                TextButton(onClick = ::dismiss) {
                    Text("完成", color = SheetAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // 独立叶子：只订阅 draft，避免顶栏/附图跟着每个字重组
            NoteSheetBody(
                draft = noteState.draft,
                onDraftChange = { noteState.update(it, autoSave = false) },
                expanded = expanded,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 8.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showAttachments = !showAttachments }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            FeatherIcons.Image,
                            contentDescription = null,
                            tint = SheetMuted,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (images.isEmpty()) "附图" else "附图（${images.size}）",
                            color = SheetMuted,
                            fontSize = 13.sp,
                        )
                    }
                    Text(
                        if (showAttachments) "收起" else "展开",
                        color = SheetAccent,
                        fontSize = 12.sp,
                    )
                }
                if (showAttachments) {
                    NoteAttachmentStrip(
                        images = images,
                        resolvePath = resolvePath,
                        onAdd = onPickImage,
                        onDelete = onDeleteImage,
                        dark = true,
                        compact = true,
                        emptyHint = "添加本盘截图",
                    )
                    Text(
                        text = "管理全部分盘附图 →",
                        color = SheetAccent,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable {
                                noteState.flush()
                                onOpenFullMemo()
                            }
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteSheetBody(
    draft: String,
    onDraftChange: (String) -> Unit,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = draft,
        onValueChange = onDraftChange,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SheetSurface)
            .padding(16.dp),
        textStyle = TextStyle(
            color = SheetInk,
            fontSize = if (expanded) 17.sp else 16.sp,
            lineHeight = if (expanded) 28.sp else 26.sp,
        ),
        cursorBrush = SolidColor(SheetAccent),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize()) {
                if (draft.isEmpty()) {
                    Text(
                        "在这里写下命局要点、流年断语…\n上拉可全屏书写",
                        color = SheetMuted,
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                    )
                }
                inner()
            }
        },
    )
}
