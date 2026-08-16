package it.iotatec.callhub.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.iotatec.callhub.R
import it.iotatec.callhub.data.db.CallEventEntity
import it.iotatec.callhub.data.model.CallDirection
import it.iotatec.callhub.data.repo.ContactsRepository
import it.iotatec.callhub.dialer.spam.SpamRepository
import it.iotatec.callhub.dialer.spam.SystemBlocklist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

private val Red = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    events: List<CallEventEntity>,
    modifier: Modifier = Modifier,
    onCall: (String) -> Unit = {},
    onSaveNote: (Long, String?) -> Unit = { _, _ -> },
    onDelete: (CallEventEntity) -> Unit = {}
) {
    var detailNumber by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = detailNumber != null) { detailNumber = null }

    val number = detailNumber
    if (number != null) {
        CallDetailView(number, events.filter { it.phoneNumber == number }, { detailNumber = null }, onCall, onDelete, onSaveNote, modifier)
        return
    }

    if (events.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.empty_recents), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var missedOnly by remember { mutableStateOf(false) }
    val filtered = remember(events, missedOnly) {
        if (missedOnly) events.filter { it.direction == CallDirection.MISSED } else events
    }
    val grouped = remember(filtered) {
        val out = mutableListOf<Pair<CallEventEntity, Int>>()
        filtered.forEach { e ->
            val last = out.lastOrNull()
            if (last != null && last.first.phoneNumber != null && last.first.phoneNumber == e.phoneNumber && last.first.source == e.source) {
                out[out.lastIndex] = last.first to (last.second + 1)
            } else out.add(e to 1)
        }
        out
    }
    var editing by remember { mutableStateOf<CallEventEntity?>(null) }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !missedOnly, onClick = { missedOnly = false }, label = { Text(stringResource(R.string.filter_all)) })
            FilterChip(selected = missedOnly, onClick = { missedOnly = true }, label = { Text(stringResource(R.string.filter_missed)) })
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(grouped, key = { it.first.id }) { (event, count) ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { v -> if (v == SwipeToDismissBoxValue.EndToStart) { onDelete(event); true } else false }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(Modifier.fillMaxSize().background(Red).padding(end = 24.dp), contentAlignment = Alignment.CenterEnd) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White)
                        }
                    }
                ) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        RecentRow(
                            event, count,
                            onOpen = { event.phoneNumber?.let { detailNumber = it } },
                            onCall = { event.phoneNumber?.let(onCall) },
                            onEditNote = { editing = event }
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }

    editing?.let { ev ->
        NoteDialog(ev.note, { editing = null }, { text -> onSaveNote(ev.id, text); editing = null })
    }
}

@Composable
private fun CallDetailView(
    number: String,
    calls: List<CallEventEntity>,
    onBack: () -> Unit,
    onCall: (String) -> Unit,
    onDelete: (CallEventEntity) -> Unit,
    onSaveNote: (Long, String?) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val name = calls.firstOrNull()?.displayName ?: number

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
            Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            NumberAvatar(number, name, 88.dp, 36.sp)
            Text(number, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                DetailAction(Icons.Filled.Call, stringResource(R.string.action_call)) { onCall(number) }
                DetailAction(Icons.AutoMirrored.Filled.Message, stringResource(R.string.action_message)) {
                    runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                DetailAction(Icons.Filled.PersonAdd, stringResource(R.string.action_add_contact)) {
                    runCatching {
                        context.startActivity(Intent(ContactsContract.Intents.Insert.ACTION).apply {
                            type = ContactsContract.RawContacts.CONTENT_TYPE
                            putExtra(ContactsContract.Intents.Insert.PHONE, number)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                }
                DetailAction(Icons.Filled.Block, stringResource(R.string.action_block)) {
                    SpamRepository.addBlocked(context, number); SystemBlocklist.block(context, number)
                }
            }
        }
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            items(calls, key = { it.id }) { c ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(directionIcon(c.direction), contentDescription = null,
                        tint = if (c.direction == CallDirection.MISSED) Red else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    val src = stringResource(c.source.labelRes)
                    val dur = c.durationSec?.let { " · " + formatDuration(it) }.orEmpty()
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(c.startTime)), style = MaterialTheme.typography.bodyMedium)
                        Text(src + dur, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDelete(c) }) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete_entry), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun DetailAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) { Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecentRow(event: CallEventEntity, count: Int, onOpen: () -> Unit, onCall: () -> Unit, onEditNote: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NumberAvatar(event.phoneNumber, event.displayName ?: event.phoneNumber ?: "?", 44.dp, 17.sp)
        Column(Modifier.weight(1f)) {
            val label = event.displayName ?: event.phoneNumber ?: stringResource(R.string.unknown_caller)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(directionIcon(event.direction), contentDescription = null,
                    tint = if (event.direction == CallDirection.MISSED) Red else MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                Text(if (count > 1) stringResource(R.string.calls_count, label, count) else label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(event.source.labelRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                if (event.isVideo) Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(event.startTime)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            event.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic) }
        }
        IconButton(onClick = onCall) { Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.action_call), tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun NumberAvatar(number: String?, name: String, size: androidx.compose.ui.unit.Dp, textSize: androidx.compose.ui.unit.TextUnit) {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(initialValue = null, number) {
        value = withContext(Dispatchers.IO) {
            val uri = ContactsRepository.photoUriForNumber(context, number) ?: return@withContext null
            runCatching { context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull()
        }
    }
    val bmp = photo
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
    } else {
        val colors = listOf(Color(0xFF1565C0), Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFF2E7D32), Color(0xFFEF6C00), Color(0xFFAD1457))
        val c = colors[(name.hashCode() and 0x7FFFFFFF) % colors.size]
        Box(Modifier.size(size).clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
            Text(name.firstOrNull { it.isLetter() }?.uppercase() ?: "#", color = Color.White, fontWeight = FontWeight.Bold, fontSize = textSize)
        }
    }
}

@Composable
private fun NoteDialog(initial: String?, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var text by remember { mutableStateOf(initial.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(stringResource(R.string.note_label)) }, modifier = Modifier.fillMaxWidth()) }
    )
}

private fun directionIcon(direction: CallDirection): ImageVector = when (direction) {
    CallDirection.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
    CallDirection.MISSED, CallDirection.REJECTED -> Icons.AutoMirrored.Filled.CallMissed
    else -> Icons.AutoMirrored.Filled.CallReceived
}

private fun formatDuration(seconds: Long): String {
    val m = TimeUnit.SECONDS.toMinutes(seconds)
    return "%d:%02d".format(m, seconds - TimeUnit.MINUTES.toSeconds(m))
}
