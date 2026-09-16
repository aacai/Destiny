package zhiqiu.app.destiny.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
actual fun rememberBytesFileSaver(
    onSaved: (fileName: String) -> Unit,
    onFailed: () -> Unit,
): BytesFileSaver {
    val scope = rememberCoroutineScope()
    val pending = remember { arrayOfNulls<ByteArray>(1) }
    val launcher = rememberFileSaverLauncher(
        dialogSettings = FileKitDialogSettings.createDefault(),
        onResult = { file: PlatformFile? ->
            val payload = pending[0]
            pending[0] = null
            if (file != null && payload != null) {
                scope.launch(Dispatchers.Default) {
                    runCatching {
                        file.write(payload)
                        onSaved(file.name)
                    }.onFailure { onFailed() }
                }
            }
        },
    )
    return remember(launcher) {
        BytesFileSaver { bytes, suggestedName ->
            pending[0] = bytes
            val base = suggestedName.substringBeforeLast('.')
            val ext = suggestedName.substringAfterLast('.', "zip")
            launcher.launch(suggestedName = base, defaultExtension = ext, allowedExtensions = setOf(ext))
        }
    }
}
