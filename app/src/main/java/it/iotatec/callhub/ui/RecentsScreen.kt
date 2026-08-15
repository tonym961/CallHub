package it.iotatec.callhub.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.iotatec.callhub.R
import it.iotatec.callhub.data.db.CallEventEntity
import it.iotatec.callhub.data.model.CallDirection
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

@Composable
fun RecentsScreen(
    events: List<CallEventEntity>,
    modifier: Modifier = Modifier,
    onCall: (String) -> Unit = {},
    onSaveNote: (Long, String?) -> Unit = { _, _ -> }
) {
    if (events.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.empty_recents), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var query by remember { mutableStateOf("") }
    val filtered = remember(events, query) {
        if (query.isBlank()) events
        else events.filter {
            it.displayName?.contains(query, ignoreCase = true) == true ||
                it.phoneNumber?.contains(query) == true ||
                it.note?.contains(query, ignoreCase = true) == true
        }
    }

    // Collapse consecutive calls with the same number/source into one row + count.
    val grouped = remember(filtered) {
        val out = mutableListOf<Pair<CallEventEntity, Int>>()
        filtered.forEach { e ->
            val last = out.lastOrNull()
            if (last != null && last.first.phoneNumber != null &&
                last.first.phoneNumber == e.phoneNumber && last.first.source == e.source
            ) {
                out[out.lastIndex] = last.first to (last.second + 1)
            } else {
                out.add(e to 1)
            }
        }
        out
    }

    var editing by remember { mutableStateOf<CallEventEntity?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.search_calls)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(grouped, key = { it.first.id }) { (event, count) ->
                CallRow(event, count, onCall, onEditNote = { editing = event })
                HorizontalDivider()
            }
        }
    }

    editing?.let { ev ->
        NoteDialog(
            initial = ev.note,
            onDismiss = { editing = null },
            onSave = { text -> onSaveNote(ev.id, text); editing = null }
        )
    }
}

@Composable
private fun NoteDialog(initial: String?, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var text by remember { mutableStateOf(initial.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.note_label)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
private fun CallRow(event: CallEventEntity, count: Int, onCall: (String) -> Unit, onEditNote: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = event.phoneNumber != null) {
                event.phoneNumber?.let(onCall)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = directionIcon(event.direction),
            contentDescription = event.direction.name,
            tint = if (event.direction == CallDirection.MISSED) Color(0xFFD32F2F)
            else MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            val name = event.displayName ?: event.phoneNumber ?: stringResource(R.string.unknown_caller)
            Text(
                text = if (count > 1) stringResource(R.string.calls_count, name, count) else name,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(event.source.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (event.isVideo) {
                    Icon(
                        Icons.Filled.Videocam,
                        contentDescription = "Video",
                        modifier = Modifier.padding(top = 1.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                event.durationSec?.let {
                    Text(
                        text = "· ${formatDuration(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            event.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
        Text(
            text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(event.startTime)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = onEditNote) {
            Icon(
                Icons.AutoMirrored.Filled.Notes,
                contentDescription = stringResource(R.string.note_label),
                tint = if (event.note != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun directionIcon(direction: CallDirection): ImageVector = when (direction) {
    CallDirection.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
    CallDirection.MISSED, CallDirection.REJECTED -> Icons.AutoMirrored.Filled.CallMissed
    else -> Icons.AutoMirrored.Filled.CallReceived
}

private fun formatDuration(seconds: Long): String {
    val m = TimeUnit.SECONDS.toMinutes(seconds)
    val s = seconds - TimeUnit.MINUTES.toSeconds(m)
    return "%d:%02d".format(m, s)
}
