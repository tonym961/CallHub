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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.telecom.Call
import it.iotatec.callhub.R
import it.iotatec.callhub.dialer.CallManager
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
            IncomingControls()
        } else {
            ActiveControls(isMuted = state.isMuted, isSpeakerOn = state.isSpeakerOn, isOnHold = state.isOnHold)
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
private fun IncomingControls() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RoundButton(Icons.Filled.CallEnd, stringResource(R.string.action_reject), Color(0xFFD32F2F)) { CallManager.reject() }
        RoundButton(Icons.Filled.Call, stringResource(R.string.action_answer), Color(0xFF2E7D32)) { CallManager.answer() }
    }
}

@Composable
private fun ActiveControls(isMuted: Boolean, isSpeakerOn: Boolean, isOnHold: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                Icons.AutoMirrored.Filled.VolumeUp,
                stringResource(R.string.action_speaker),
                if (isSpeakerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) { CallManager.toggleSpeaker() }
            RoundButton(
                Icons.Filled.Pause,
                stringResource(R.string.action_hold),
                if (isOnHold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) { CallManager.toggleHold() }
        }
        Spacer(Modifier.height(40.dp))
        RoundButton(Icons.Filled.CallEnd, stringResource(R.string.action_hangup), Color(0xFFD32F2F)) { CallManager.hangup() }
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
