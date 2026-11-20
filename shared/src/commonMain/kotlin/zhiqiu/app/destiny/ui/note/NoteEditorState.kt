package zhiqiu.app.destiny.ui.note

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NoteSaveStatus {
    Idle,
    Dirty,
    Saving,
    Saved,
}

/**
 * 命例总批草稿。
 * - [update] 默认不自动落库（Sheet 打字用），避免 upsert 触发整页重组卡顿
 * - 批注 Tab 可开 [autoSave]；关闭/完成时务必 [flush]
 */
@Stable
class NoteEditorState(
    initialNote: String,
    private val scope: CoroutineScope,
    private val saveProvider: () -> (suspend (String) -> Unit),
) {
    var draft by mutableStateOf(initialNote)
        private set
    var status by mutableStateOf(NoteSaveStatus.Idle)
        private set
    var dirty by mutableStateOf(false)
        private set

    private var debounceJob: Job? = null
    private var savedAckJob: Job? = null

    fun update(text: String, autoSave: Boolean = false) {
        if (text == draft) return
        draft = text
        if (!dirty) dirty = true
        // 已 Dirty 时不再改 status，减少无关键重组
        if (status != NoteSaveStatus.Dirty && status != NoteSaveStatus.Saving) {
            status = NoteSaveStatus.Dirty
        }
        if (autoSave) scheduleDebounce()
    }

    /** 外部 profile.note 更新：仅草稿干净时跟进 */
    fun syncFromExternal(savedNote: String) {
        if (dirty) return
        if (draft != savedNote) draft = savedNote
        if (status == NoteSaveStatus.Saved) status = NoteSaveStatus.Idle
    }

    fun flush() {
        debounceJob?.cancel()
        debounceJob = null
        if (!dirty) return
        val trimmed = draft.trim()
        dirty = false
        scope.launch {
            status = NoteSaveStatus.Saving
            runCatching {
                withContext(Dispatchers.Default) {
                    saveProvider().invoke(trimmed)
                }
            }
            markSaved()
        }
    }

    private fun scheduleDebounce() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(1500)
            if (!dirty) return@launch
            val trimmed = draft.trim()
            dirty = false
            status = NoteSaveStatus.Saving
            runCatching {
                withContext(Dispatchers.Default) {
                    saveProvider().invoke(trimmed)
                }
            }
            markSaved()
        }
    }

    private fun markSaved() {
        status = NoteSaveStatus.Saved
        savedAckJob?.cancel()
        savedAckJob = scope.launch {
            delay(1000)
            if (status == NoteSaveStatus.Saved && !dirty) {
                status = NoteSaveStatus.Idle
            }
        }
    }
}

@Composable
fun rememberNoteEditorState(
    profileId: String,
    savedNote: String,
    onSave: suspend (String) -> Unit,
): NoteEditorState {
    val scope = rememberCoroutineScope()
    val latestSave = rememberUpdatedState(onSave)
    val state = remember(profileId) {
        NoteEditorState(
            initialNote = savedNote,
            scope = scope,
            saveProvider = { latestSave.value },
        )
    }
    LaunchedEffect(savedNote, profileId) {
        state.syncFromExternal(savedNote)
    }
    return state
}
