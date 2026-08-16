package it.iotatec.callhub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import android.telecom.CallAudioState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.telecom.Call
import it.iotatec.callhub.R
import it.iotatec.callhub.data.repo.QuickRepliesRepository
import it.iotatec.callhub.dialer.CallManager
import it.iotatec.callhub.util.CallRecorder
import it.iotatec.callhub.util.SmsSender
import kotlinx.coroutines.delay

@Composable
fun InCallScreen(onFinished: () -> Unit) {
    val state by CallManager.state.collectAsState()

    // Close the screen once the call is gone.
    LaunchedEffect(state.hasCall) { if (!state.hasCall) onFinished() }

    val isRinging = state.callState == Call.STATE_RINGING
    val isConnecting = state.callState == Call.STATE_DIALING ||
        state.callState == Call.STATE_CONNECTING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))
        Text(
            text = state.name ?: state.number ?: stringResource(R.string.unknown_caller),
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                isRinging -> stringResource(R.string.call_incoming)
                isConnecting -> stringResource(R.string.call_dialing)
                state.isOnHold -> stringResource(R.string.call_on_hold)
                state.callState == Call.STATE_ACTIVE -> CallTimerText(state.connectTimeMillis)
                else -> ""
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.weight(1f))

        if (isRinging) {
            IncomingControls(state.number)
        } else {
            ActiveControls(
                isMuted = state.isMuted,
                isSpeakerOn = state.isSpeakerOn,
                isOnHold = state.isOnHold,
                canMerge = state.canMerge,
                supportedRoutes = state.supportedRoutes,
                audioRoute = state.audioRoute
            )
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun CallTimerText(connectTimeMillis: Long): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(connectTimeMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    if (connectTimeMillis <= 0) return "00:00"
    val secs = ((now - connectTimeMillis) / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(secs / 60, secs % 60)
}

@Composable
private fun IncomingControls(number: String?) {
    val context = LocalContext.current
    var showReplies by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showReplies) {
            QuickRepliesRepository.get(context).forEach { text ->
                TextButton(onClick = {
                    number?.let { SmsSender.send(context, it, text) }
                    CallManager.reject()
                }) { Text(text) }
            }
        } else if (number != null && SmsSender.canSend(context)) {
            TextButton(onClick = { showReplies = true }) {
                Text(stringResource(R.string.reject_with_message))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RoundButton(Icons.Filled.CallEnd, stringResource(R.string.action_reject), Color(0xFFD32F2F)) { CallManager.reject() }
            RoundButton(Icons.Filled.Call, stringResource(R.string.action_answer), Color(0xFF2E7D32)) { CallManager.answer() }
        }
    }
}

@Composable
private fun ActiveControls(isMuted: Boolean, isSpeakerOn: Boolean, isOnHold: Boolean, canMerge: Boolean, supportedRoutes: Int, audioRoute: Int) {
    val context = LocalContext.current
    var showKeypad by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(CallRecorder.isRecording()) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (showKeypad) {
            DtmfPad(onKey = { CallManager.playDtmf(it) }, onClose = { showKeypad = false })
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RoundButton(
                    if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    stringResource(R.string.action_mute),
                    if (isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) { CallManager.toggleMute() }
                RoundButton(
                    Icons.Filled.Dialpad,
                    stringResource(R.string.action_keypad),
                    MaterialTheme.colorScheme.surfaceVariant
                ) { showKeypad = true }
                Box {
                    var audioMenu by remember { mutableStateOf(false) }
                    val hasBt = supportedRoutes and CallAudioState.ROUTE_BLUETOOTH != 0
                    val hasWired = supportedRoutes and CallAudioState.ROUTE_WIRED_HEADSET != 0
                    val audioIcon = when (audioRoute) {
                        CallAudioState.ROUTE_BLUETOOTH -> Icons.Filled.Bluetooth
                        CallAudioState.ROUTE_WIRED_HEADSET -> Icons.Filled.Headset
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    }
                    RoundButton(
                        audioIcon,
                        stringResource(R.string.action_speaker),
                        if (isSpeakerOn || audioRoute == CallAudioState.ROUTE_BLUETOOTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) { if (hasBt || hasWired) audioMenu = true else CallManager.toggleSpeaker() }
                    DropdownMenu(expanded = audioMenu, onDismissRequest = { audioMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.audio_earpiece)) }, onClick = { CallManager.setAudioRoute(CallAudioState.ROUTE_EARPIECE); audioMenu = false })
                        DropdownMenuItem(text = { Text(stringResource(R.string.action_speaker)) }, onClick = { CallManager.setAudioRoute(CallAudioState.ROUTE_SPEAKER); audioMenu = false })
                        if (hasBt) DropdownMenuItem(text = { Text(stringResource(R.string.audio_bluetooth)) }, onClick = { CallManager.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH); audioMenu = false })
                        if (hasWired) DropdownMenuItem(text = { Text(stringResource(R.string.audio_wired)) }, onClick = { CallManager.setAudioRoute(CallAudioState.ROUTE_WIRED_HEADSET); audioMenu = false })
                    }
                }
                RoundButton(
                    Icons.Filled.Pause,
                    stringResource(R.string.action_hold),
                    if (isOnHold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) { CallManager.toggleHold() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                if (canMerge) {
                    RoundButton(
                        Icons.AutoMirrored.Filled.CallMerge,
                        stringResource(R.string.action_merge),
                        MaterialTheme.colorScheme.primary
                    ) { CallManager.merge() }
                }
                if (CallRecorder.canRecord()) {
                    RoundButton(
                        Icons.Filled.FiberManualRecord,
                        stringResource(R.string.action_record),
                        if (recording) Color(0xFFD32F2F) else MaterialTheme.colorScheme.surfaceVariant
                    ) { recording = CallRecorder.toggle(context) }
                }
            }
            if (canMerge || CallRecorder.canRecord()) Spacer(Modifier.height(16.dp))
            Spacer(Modifier.height(40.dp))
            RoundButton(Icons.Filled.CallEnd, stringResource(R.string.action_hangup), Color(0xFFD32F2F)) { CallManager.hangup() }
        }
    }
}

@Composable
private fun DtmfPad(onKey: (Char) -> Unit, onClose: () -> Unit) {
    val rows = listOf("123", "456", "789", "*0#")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { c ->
                    Surface(
                        onClick = { onKey(c) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(6.dp).size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(c.toString(), fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        RoundButton(Icons.Filled.Close, "", MaterialTheme.colorScheme.surfaceVariant, onClick = onClose)
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, bg: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = bg,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = Color.White)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
