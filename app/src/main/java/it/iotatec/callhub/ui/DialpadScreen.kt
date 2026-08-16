package it.iotatec.callhub.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.telephony.TelephonyManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import it.iotatec.callhub.R
import it.iotatec.callhub.data.repo.ContactsRepository
import it.iotatec.callhub.data.repo.FavoritesRepository
import it.iotatec.callhub.data.repo.PhoneContact
import it.iotatec.callhub.util.T9
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialpadScreen(modifier: Modifier = Modifier, onCall: (String) -> Unit = {}) {
    val context = LocalContext.current
    val view = LocalView.current
    val clipboard = LocalClipboardManager.current
    var number by remember { mutableStateOf("") }

    // DTMF tone generator, released with the screen.
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 70) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }

    val contacts by produceState(initialValue = emptyList<PhoneContact>()) {
        value = withContext(Dispatchers.IO) { ContactsRepository.load(context) }
    }

    // T9 matches for the typed digits.
    val digits = number.filter { it.isDigit() }
    val matches = remember(contacts, digits) {
        if (digits.isEmpty()) emptyList()
        else contacts.filter { T9.matches(it.name, it.number, digits) }.take(25)
    }

    // The SIM / SIP account chooser is handled by the shared onCall handler.
    val placeCall: (String) -> Unit = { target -> if (target.isNotBlank()) onCall(target) }

    // Tap: append digit + DTMF tone + haptic tick.
    val press: (String) -> Unit = { digit ->
        number += digit
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        tone?.startTone(dtmfTone(digit), 130)
    }

    // Long-press: 0 → "+", 1 → voicemail, 2-9 → speed-dial favorite.
    val longPress: (String) -> Unit = { digit ->
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        when (digit) {
            "0" -> number += "+"
            "1" -> callVoicemail(context, placeCall)
            "*", "#" -> number += digit
            else -> {
                val idx = digit.toInt() - 2
                val favs = FavoritesRepository.favorites(context).sorted()
                if (idx in favs.indices) placeCall(favs[idx])
            }
        }
    }

    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Long-press the display to paste a number from the clipboard.
        Text(
            text = number.ifEmpty { " " },
            fontSize = 34.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        clipboard.getText()?.text
                            ?.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { number += it; view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
                    }
                )
                .padding(top = 16.dp)
        )

        // T9 suggestions fill the space above the keypad.
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (matches.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(matches, key = { it.name + it.number }) { c ->
                        MatchRow(c) { placeCall(c.number) }
                        HorizontalDivider()
                    }
                }
            }
        }

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (digit, letters) ->
                    DialKey(digit, letters, onClick = { press(digit) }, onLongClick = { longPress(digit) })
                }
            }
            Spacer12()
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer72()
            Surface(
                onClick = { placeCall(number) },
                shape = CircleShape,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(horizontal = 24.dp).size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.action_call), tint = Color.White)
                }
            }
            IconButton(
                onClick = { if (number.isNotEmpty()) number = number.dropLast(1) },
                modifier = Modifier.size(72.dp)
            ) {
                if (number.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchRow(contact: PhoneContact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(contact.name.firstOrNull()?.uppercase() ?: "?", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
        }
        Column {
            Text(contact.name, fontWeight = FontWeight.Medium)
            Text(contact.number, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Spacer12() = Box(Modifier.height(12.dp))

@Composable
private fun Spacer72() = Box(Modifier.size(72.dp))

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKey(digit: String, letters: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(6.dp)
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(digit, fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface)
            if (letters.isNotEmpty()) {
                Text(letters, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Maps a keypad label to its ToneGenerator DTMF constant. */
private fun dtmfTone(digit: String): Int = when (digit) {
    "0" -> ToneGenerator.TONE_DTMF_0
    "1" -> ToneGenerator.TONE_DTMF_1
    "2" -> ToneGenerator.TONE_DTMF_2
    "3" -> ToneGenerator.TONE_DTMF_3
    "4" -> ToneGenerator.TONE_DTMF_4
    "5" -> ToneGenerator.TONE_DTMF_5
    "6" -> ToneGenerator.TONE_DTMF_6
    "7" -> ToneGenerator.TONE_DTMF_7
    "8" -> ToneGenerator.TONE_DTMF_8
    "9" -> ToneGenerator.TONE_DTMF_9
    "*" -> ToneGenerator.TONE_DTMF_S
    "#" -> ToneGenerator.TONE_DTMF_P
    else -> ToneGenerator.TONE_DTMF_0
}

/** Calls the carrier voicemail number, falling back to the dialer's voicemail intent. */
private fun callVoicemail(context: Context, placeCall: (String) -> Unit) {
    val vm = runCatching {
        (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).voiceMailNumber
    }.getOrNull()
    if (!vm.isNullOrBlank()) {
        placeCall(vm)
    } else {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_DIAL, Uri.parse("voicemail:")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
