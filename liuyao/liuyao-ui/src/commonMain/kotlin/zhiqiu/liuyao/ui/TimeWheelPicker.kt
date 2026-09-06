package zhiqiu.liuyao.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * 自包含的滚轮时间选择器（时 + 分），仅依赖 Compose，不引用任何外部业务模块。
 * 样式借鉴 app 档案页的「具体时刻」滚轮，供六爻「占时」复用，保持模块解耦。
 */

/** 单行滚轮选择列：滑动吸附，居中项为当前值。固定宽高避免 AlertDialog 测量穿透到 LazyColumn。 */
@Composable
private fun WheelColumn(
    items: List<String>,
    startIndex: Int,
    onCenterChange: (Int) -> Unit,
) {
    val itemHeight = 34.dp
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = startIndex.coerceIn(0, items.size - 1),
    )
    val centerIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) {
                startIndex
            } else {
                val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
                info.visibleItemsInfo
                    .minByOrNull { abs(it.offset + it.size / 2 - mid) }
                    ?.index ?: startIndex
            }
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { centerIndex }.collect { onCenterChange(it) }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.width(92.dp).height(itemHeight * 5),
        contentPadding = PaddingValues(vertical = itemHeight * 2),
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(items.size) { i ->
            val selected = centerIndex == i
            Box(modifier = Modifier.height(itemHeight), contentAlignment = Alignment.Center) {
                Text(
                    items[i],
                    color = if (selected) PanInk else PanMuted,
                    fontSize = if (selected) 17.sp else 14.sp,
                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                )
            }
        }
    }
}

/** 占时「具体时刻」弹窗：时 + 分两列滚轮，确定后回传。 */
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    var selHour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var selMinute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selHour, selMinute) }) { Text("确定", color = PanAccent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = PanMuted) }
        },
        title = { Text("占时 · 具体时刻", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = PanInk) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelColumn((0..23).map { "%02d".format(it) }, selHour) { selHour = it }
                WheelColumn((0..59).map { "%02d".format(it) }, selMinute) { selMinute = it }
            }
        },
    )
}

/** 占时时刻触发器：显示 HH:MM，点击打开滚轮弹窗（与档案页「具体时刻」交互一致）。 */
@Composable
fun TimeTrigger(hour: Int, minute: Int, onClick: () -> Unit) {
    val hh = hour.coerceIn(0, 23)
    val mm = minute.coerceIn(0, 59)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PanCardBg)
            .border(1.dp, PanLine, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "%02d:%02d".format(hh, mm),
            color = PanInk,
            fontSize = 14.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        )
    }
}
