package zhiqiu.app.destiny.oinkcompat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.applewidgets.kmp.oink.basic.OinkButton
import zhiqiu.applewidgets.kmp.oink.basic.OinkButtonVariant
import zhiqiu.applewidgets.kmp.oink.basic.OinkCheckbox
import zhiqiu.applewidgets.kmp.oink.basic.OinkIcon
import zhiqiu.applewidgets.kmp.oink.basic.OinkIconButton
import zhiqiu.applewidgets.kmp.oink.basic.OinkSlider
import zhiqiu.applewidgets.kmp.oink.basic.OinkSwitch
import zhiqiu.applewidgets.kmp.oink.basic.OinkText
import zhiqiu.applewidgets.kmp.oink.form.OinkTextField
import zhiqiu.applewidgets.kmp.oink.layout.OinkDivider
import zhiqiu.applewidgets.kmp.oink.theme.OinkTheme

/**
 * MD3 → Oink 兼容层。[Text] 等函数保持与 androidx.compose.material3 常用签名一致，
 * 内部统一委托 Oink 组件，业务侧只需替换 import。
 */

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = OinkTheme.textStyles.body,
) {
    val resolved = style.copy(
        color = if (color == Color.Unspecified) style.color else color,
        fontSize = if (fontSize == TextUnit.Unspecified) style.fontSize else fontSize,
        fontStyle = fontStyle ?: style.fontStyle,
        fontWeight = fontWeight ?: style.fontWeight,
        fontFamily = fontFamily ?: style.fontFamily,
        letterSpacing = if (letterSpacing == TextUnit.Unspecified) style.letterSpacing else letterSpacing,
        textDecoration = textDecoration ?: style.textDecoration,
        lineHeight = if (lineHeight == TextUnit.Unspecified) style.lineHeight else lineHeight,
    )
    OinkText(
        text = text,
        modifier = modifier,
        style = resolved,
        textAlign = textAlign,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
    )
}

@Composable
fun Text(
    text: androidx.compose.ui.text.AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: ((androidx.compose.ui.text.TextLayoutResult) -> Unit)? = null,
    style: TextStyle = OinkTheme.textStyles.body,
) {
    val resolved = style.copy(
        color = if (color == Color.Unspecified) style.color else color,
        fontSize = if (fontSize == TextUnit.Unspecified) style.fontSize else fontSize,
        fontWeight = fontWeight ?: style.fontWeight,
        fontStyle = fontStyle ?: style.fontStyle,
        fontFamily = fontFamily ?: style.fontFamily,
        letterSpacing = if (letterSpacing == TextUnit.Unspecified) style.letterSpacing else letterSpacing,
        textDecoration = textDecoration ?: style.textDecoration,
        lineHeight = if (lineHeight == TextUnit.Unspecified) style.lineHeight else lineHeight,
    )
    androidx.compose.foundation.text.BasicText(
        text = text,
        modifier = modifier,
        style = resolved.copy(textAlign = textAlign ?: resolved.textAlign),
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
    )
}

object ButtonDefaults {
    @Composable
    fun buttonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): Any? = null
}

object SwitchDefaults {
    @Composable
    fun colors(
        checkedTrackColor: Color = Color.Unspecified,
        uncheckedTrackColor: Color = Color.Unspecified,
        checkedThumbColor: Color = Color.Unspecified,
        uncheckedThumbColor: Color = Color.Unspecified,
    ): Any? = null
}

object SliderDefaults {
    @Composable
    fun colors(
        thumbColor: Color = Color.Unspecified,
        activeTrackColor: Color = Color.Unspecified,
        inactiveTrackColor: Color = Color.Unspecified,
    ): Any? = null
}

object CheckboxDefaults {
    @Composable
    fun colors(
        checkedColor: Color = Color.Unspecified,
        uncheckedColor: Color = Color.Unspecified,
        checkmarkColor: Color = Color.Unspecified,
    ): Any? = null
}

object OutlinedTextFieldDefaults {
    @Composable
    fun colors(
        focusedTextColor: Color = Color.Unspecified,
        unfocusedTextColor: Color = Color.Unspecified,
        focusedBorderColor: Color = Color.Unspecified,
        unfocusedBorderColor: Color = Color.Unspecified,
        cursorColor: Color = Color.Unspecified,
        focusedContainerColor: Color = Color.Unspecified,
        unfocusedContainerColor: Color = Color.Unspecified,
    ): Any? = null
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues? = null,
    colors: Any? = null,
    shape: androidx.compose.ui.graphics.Shape? = null,
    elevation: Any? = null,
    content: @Composable RowScope.() -> Unit,
) {
    OinkButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant = OinkButtonVariant.Accent,
        insideMargin = contentPadding,
        content = content,
    )
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues? = null,
    colors: Any? = null,
    content: @Composable RowScope.() -> Unit,
) {
    OinkButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant = OinkButtonVariant.Ghost,
        content = content,
    )
}

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OinkButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant = OinkButtonVariant.Outline,
        content = content,
    )
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(40.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) { content() }
}

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    OinkIcon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = if (tint == Color.Unspecified) OinkTheme.colors.text else tint,
    )
}

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    thickness: androidx.compose.ui.unit.Dp = 1.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 1.dp,
) {
    if (color == Color.Unspecified) {
        OinkDivider(modifier = modifier)
    } else {
        androidx.compose.foundation.layout.Box(
            modifier
                .fillMaxWidth()
                .height(thickness)
                .background(color),
        )
    }
}

@Composable
fun Divider(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    thickness: androidx.compose.ui.unit.Dp = 1.dp,
) {
    HorizontalDivider(modifier, color, thickness)
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: Any? = null,
) {
    OinkSwitch(
        checked = checked,
        onCheckedChange = { onCheckedChange?.invoke(it) },
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: Any? = null,
) {
    OinkCheckbox(
        checked = checked,
        onCheckedChange = { onCheckedChange?.invoke(it) },
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: Any? = null,
) {
    OinkSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        showBubble = false,
    )
}

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: androidx.compose.ui.unit.Dp = 4.dp,
) {
    zhiqiu.applewidgets.kmp.oink.basic.OinkSpinner(
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth,
    )
}

@Composable
fun LinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
) {
    zhiqiu.applewidgets.kmp.oink.basic.OinkProgress(
        modifier = modifier,
        progress = progress(),
    )
}

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    textStyle: TextStyle = TextStyle.Default,
    shape: androidx.compose.ui.graphics.Shape? = null,
    colors: Any? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OinkTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        placeholder = null,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}
