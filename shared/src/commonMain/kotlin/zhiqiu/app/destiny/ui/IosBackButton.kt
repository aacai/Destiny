package zhiqiu.app.destiny.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import zhiqiu.app.destiny.oinkcompat.Icon
import zhiqiu.app.destiny.oinkcompat.Text
import zhiqiu.app.destiny.oinkcompat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronLeft
import zhiqiu.app.destiny.ui.theme.DestinyTheme

@Composable
fun IosBackButton(
    onClick: () -> Unit,
    label: String = "返回",
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = FeatherIcons.ChevronLeft,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = DestinyTheme.accent,
            )
            Text(
                text = label,
                color = DestinyTheme.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
