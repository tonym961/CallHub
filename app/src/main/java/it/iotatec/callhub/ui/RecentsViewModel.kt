package it.iotatec.callhub.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.iotatec.callhub.data.db.CallEventEntity
import it.iotatec.callhub.data.repo.CallRepository
import it.iotatec.callhub.dialer.CallLogSync
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecentsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = CallRepository.get(app)

    val events: StateFlow<List<CallEventEntity>> =
        repo.observeRecent().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Pull the latest native calls into the unified store (e.g. on resume). */
    fun refreshNativeCallLog() {
        viewModelScope.launch { CallLogSync.sync(getApplication()) }
    }

    fun setNote(id: Long, note: String?) {
        viewModelScope.launch { repo.updateNote(id, note?.ifBlank { null }) }
    }
}
