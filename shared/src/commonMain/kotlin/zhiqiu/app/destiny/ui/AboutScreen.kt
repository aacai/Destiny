package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import zhiqiu.app.destiny.oinkcompat.HorizontalDivider
import zhiqiu.app.destiny.oinkcompat.Text
import zhiqiu.app.destiny.oinkcompat.TextButton
import zhiqiu.app.destiny.i18n.AppStrings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.app.destiny.ui.theme.DestinyTheme

/** 项目仓库地址，与 git remote origin 保持一致。 */
const val OFFICIAL_ADDRESS: String = "https://github.com/aacai/Destiny"

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(DestinyTheme.page),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text(AppStrings.backShort, color = DestinyTheme.accent, fontSize = 15.sp) }
            Text(AppStrings.about, color = DestinyTheme.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(thickness = 0.5.dp, color = DestinyTheme.line)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                Text("Destiny 命理", color = DestinyTheme.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(AppStrings.aboutTagline, color = DestinyTheme.muted, fontSize = 14.sp, lineHeight = 20.sp)
            }

            AboutSection(AppStrings.aboutFeatures) {
                FeatureLine(AppStrings.aboutFeatureLife)
                Spacer(modifier = Modifier.height(10.dp))
                FeatureLine(AppStrings.aboutFeatureDivination)
                Spacer(modifier = Modifier.height(10.dp))
                FeatureLine(AppStrings.aboutFeatureBooks)
                Spacer(modifier = Modifier.height(10.dp))
                FeatureLine(AppStrings.aboutFeatureBackup)
            }

            AboutSection(AppStrings.aboutProject) {
                Text(AppStrings.aboutRepo, color = DestinyTheme.muted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                SelectionContainer {
                    Text(OFFICIAL_ADDRESS, color = DestinyTheme.accent, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(AppStrings.aboutPlatforms, color = DestinyTheme.muted, fontSize = 12.sp)
            }

            AboutSection(AppStrings.aboutPrivacy) {
                Text(AppStrings.aboutPrivacyText, color = DestinyTheme.muted, fontSize = 13.sp, lineHeight = 19.sp)
            }

            AboutSection(AppStrings.aboutDisclaimer) {
                Text(AppStrings.aboutDisclaimerText, color = DestinyTheme.muted, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DestinyTheme.card)
            .border(1.dp, DestinyTheme.line, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(title, color = DestinyTheme.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun FeatureLine(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(DestinyTheme.accent),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text,
            color = DestinyTheme.ink,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f),
        )
    }
}
