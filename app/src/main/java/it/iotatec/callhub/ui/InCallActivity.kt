package it.iotatec.callhub.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

/**
 * Full-screen call UI, launched by [it.iotatec.callhub.dialer.CallHubInCallService]
 * when a call is added. Shows over the lock screen for incoming calls.
 */
class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        setContent {
            CallHubTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    InCallScreen(onFinished = { finish() })
                }
            }
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
