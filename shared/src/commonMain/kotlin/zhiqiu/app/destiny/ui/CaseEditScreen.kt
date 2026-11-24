package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import zhiqiu.app.destiny.oinkcompat.Button
import zhiqiu.app.destiny.oinkcompat.HorizontalDivider
import zhiqiu.app.destiny.oinkcompat.Text
import zhiqiu.app.destiny.oinkcompat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.app.destiny.i18n.AppStrings
import zhiqiu.app.destiny.profile.CaseType
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.ui.theme.DestinyColors
import zhiqiu.app.destiny.ui.theme.DestinyTheme

/** 占卜案例的通用编辑页：改名称、占问与备注，一次性保存。 */
@Composable
fun CaseEditScreen(
    profile: Profile,
    question: String,
    onBack: () -> Unit,
    onSave: (name: String, question: String, note: String) -> Unit,
) {
    var name by remember(profile.id) { mutableStateOf(profile.name) }
    var questionDraft by remember(profile.id) { mutableStateOf(question) }
    var note by remember(profile.id) { mutableStateOf(profile.note) }
    val c = DestinyTheme

    Column(modifier = Modifier.fillMaxSize().background(c.page)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text(AppStrings.backShort, color = c.accent, fontSize = 15.sp) }
            Text(
                "${AppStrings.edit} · ${CaseType.label(profile.caseType)}",
                color = c.ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = c.line)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(AppStrings.caseName, color = c.muted, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Input(c, name, singleLine = true) { name = it }

            Spacer(Modifier.height(14.dp))
            Text(AppStrings.question, color = c.muted, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Input(c, questionDraft, singleLine = true) { questionDraft = it }

            Spacer(Modifier.height(14.dp))
            Text(AppStrings.note, color = c.muted, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Input(c, note, singleLine = false) { note = it }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { onSave(name, questionDraft, note) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(AppStrings.save, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Input(
    c: DestinyColors,
    value: String,
    singleLine: Boolean,
    onChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = singleLine,
        textStyle = TextStyle(fontSize = 15.sp, color = c.ink),
        cursorBrush = SolidColor(c.accent),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (singleLine) Modifier else Modifier.heightIn(min = 220.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(c.card)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}
