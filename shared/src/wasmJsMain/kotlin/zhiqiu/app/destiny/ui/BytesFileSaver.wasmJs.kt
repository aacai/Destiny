package zhiqiu.app.destiny.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop

@Composable
actual fun rememberBytesFileSaver(
    onSaved: (fileName: String) -> Unit,
    onFailed: () -> Unit,
): BytesFileSaver = remember(onSaved, onFailed) {
    BytesFileSaver { bytes, suggestedName ->
        runCatching {
            downloadBytes(bytes, suggestedName)
            onSaved(suggestedName)
        }.onFailure { onFailed() }
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun downloadBytes(bytes: ByteArray, fileName: String) {
    triggerBrowserDownload(Base64.encode(bytes), fileName)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun triggerBrowserDownload(base64: String, fileName: String): Unit = js(
    "((b64, name) => { const bin = atob(b64); const arr = new Uint8Array(bin.length); for (let i = 0; i < bin.length; i++) arr[i] = bin.charCodeAt(i); const blob = new Blob([arr], { type: 'application/zip' }); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = name; a.click(); URL.revokeObjectURL(url); })(base64, fileName)",
)
