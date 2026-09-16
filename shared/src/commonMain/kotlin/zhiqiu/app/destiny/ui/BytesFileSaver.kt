package zhiqiu.app.destiny.ui

import androidx.compose.runtime.Composable

/**
 * 跨平台「保存字节到用户选择的文件」。
 * Web 上 FileKit 的 FileSaver 尚未覆盖 wasm，故用浏览器下载代替。
 */
fun interface BytesFileSaver {
    fun save(bytes: ByteArray, suggestedName: String)
}

@Composable
expect fun rememberBytesFileSaver(
    onSaved: (fileName: String) -> Unit,
    onFailed: () -> Unit,
): BytesFileSaver
