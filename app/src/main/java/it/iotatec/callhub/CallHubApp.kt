package it.iotatec.callhub

import android.app.Application
import it.iotatec.callhub.data.repo.CallRepository

class CallHubApp : Application() {

    val repository: CallRepository by lazy { CallRepository.get(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: CallHubApp
            private set
    }
}
