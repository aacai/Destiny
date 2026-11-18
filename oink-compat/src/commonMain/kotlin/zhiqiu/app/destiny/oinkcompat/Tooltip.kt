package zhiqiu.app.destiny.oinkcompat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import zhiqiu.applewidgets.kmp.oink.theme.OinkTheme

/** MD3 TooltipBox 兼容层：点击时显示提示气泡。 */
@Composable
fun TooltipBox(
    positionProvider: Any?,
    tooltip: @Composable () -> Unit,
    state: Any?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var show by remember { mutableStateOf(false) }
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.clickable { show = !show }) { content() }
        if (show) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { show = false },
            ) { tooltip() }
        }
    }
}

@Composable
fun PlainTooltip(content: @Composable () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(OinkTheme.colors.pop)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) { content() }
}

object TooltipDefaults {
    fun rememberTooltipPositionProvider(position: TooltipAnchorPosition): Any? = null
    @Composable
    fun plainTooltipColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
    ): Any? = null
}

enum class TooltipAnchorPosition { Above, Below, Start, End }

@Composable
fun rememberTooltipState(): Any? = null
