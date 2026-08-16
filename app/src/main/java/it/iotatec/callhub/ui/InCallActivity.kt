package it.iotatec.callhub.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

/**
 * Full-screen call UI, launched by [it.iotatec.callhub.dialer.CallHubInCallService]
 * when a call is added. Shows over the lock screen for incoming calls and turns the
 * screen off when the phone is held to the ear (proximity wake lock).
 */
class InCallActivity : ComponentActivity() {

    private var proximityLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        acquireProximity()
        setContent {
            CallHubTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    InCallScreen(onFinished = { finish() })
                }
            }
        }
    }

    override fun onDestroy() {
        proximityLock?.let { if (it.isHeld) runCatching { it.release() } }
        super.onDestroy()
    }

    private fun acquireProximity() {
        val pm = getSystemService(PowerManager::class.java) ?: return
        if (pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            proximityLock = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "callhub:incall")
            runCatching { proximityLock?.acquire(60 * 60 * 1000L) }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(
                Intent(context, InCallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
