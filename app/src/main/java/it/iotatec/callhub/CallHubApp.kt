package it.iotatec.callhub

import android.app.Application
import android.util.Log
import it.iotatec.callhub.data.repo.CallRepository
import it.iotatec.callhub.dialer.spam.CommunityReputationProvider
import it.iotatec.callhub.dialer.spam.CommunityReputationStore
import it.iotatec.callhub.dialer.spam.Reputation
import it.iotatec.callhub.sip.LinphoneSipEngine
import it.iotatec.callhub.sip.SipManager
import it.iotatec.callhub.sip.SipRegistry
import it.iotatec.callhub.ui.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallHubApp : Application() {

    val repository: CallRepository by lazy { CallRepository.get(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppTheme.load(this)
        // Swap the stub SIP engine for the real liblinphone one; fall back on failure.
        runCatching { SipRegistry.engine = LinphoneSipEngine(this) }
            .onFailure { Log.w("CallHubApp", "Linphone init failed, keeping stub engine", it) }
        // Register any saved SIP accounts so incoming calls work from launch.
        runCatching { SipManager.registerAll(this) }
            .onFailure { Log.w("CallHubApp", "SIP registerAll failed", it) }
        // Community caller-ID: use the list-backed reputation provider and refresh it.
        Reputation.provider = CommunityReputationProvider(this)
        CoroutineScope(Dispatchers.IO).launch { runCatching { CommunityReputationStore.refresh(this@CallHubApp) } }
    }

    companion object {
        lateinit var instance: CallHubApp
            private set
    }
}
