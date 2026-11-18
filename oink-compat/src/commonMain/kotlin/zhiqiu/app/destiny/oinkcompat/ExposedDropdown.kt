package zhiqiu.app.destiny.oinkcompat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.applewidgets.kmp.oink.form.OinkTextField
import zhiqiu.applewidgets.kmp.oink.theme.OinkTheme

/** MD3 ExposedDropdownMenu 兼容层：用点击展开的只读输入框 + DropdownMenu 实现。 */

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class ExperimentalMaterial3Api

class ExposedDropdownMenuBoxScope

@Composable
fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit,
) {
    Box(modifier) { content(ExposedDropdownMenuBoxScope()) }
}

fun Modifier.menuAnchor(): Modifier = this.clickable { }

object ExposedDropdownMenuDefaults {
    @Composable
    fun TrailingIcon(expanded: Boolean) {
        Text(if (expanded) "▴" else "▾", color = OinkTheme.colors.text2, fontSize = 12.sp)
    }
}

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = TextStyle.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OinkTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled && !readOnly,
        trailing = trailingIcon,
    )
}

@Composable
fun ExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, modifier = modifier.fillMaxWidth(), content = content)
}
