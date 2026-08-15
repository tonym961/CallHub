package it.iotatec.callhub.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import it.iotatec.callhub.R
import it.iotatec.callhub.update.AppUpdater
import it.iotatec.callhub.util.CallPlacer
import it.iotatec.callhub.util.DefaultDialerHelper

private enum class Tab(@StringRes val labelRes: Int, val icon: ImageVector) {
    RECENTS(R.string.title_recents, Icons.Filled.History),
    DIALPAD(R.string.title_dialpad, Icons.Filled.Dialpad),
    CONTACTS(R.string.title_contacts, Icons.Filled.Contacts),
    SETTINGS(R.string.title_settings, Icons.Filled.Settings)
}

class MainActivity : ComponentActivity() {

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private val requestDialerRole =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askRuntimePermissions()
        // Sideload flavor checks GitHub for updates; Play flavor is a no-op.
        AppUpdater.checkForUpdates(this)

        setContent {
            CallHubTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScaffold(
                        onSetDefaultDialer = {
                            requestDialerRole.launch(DefaultDialerHelper.requestDefaultDialerIntent(this))
                        },
                        onOpenNotificationAccess = {
                            startActivity(DefaultDialerHelper.notificationAccessSettingsIntent())
                        }
                    )
                }
            }
        }
    }

    private fun askRuntimePermissions() {
        val perms = mutableListOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        requestPermissions.launch(perms.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    onSetDefaultDialer: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    vm: RecentsViewModel = viewModel()
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(Tab.RECENTS) }

    LaunchedEffect(Unit) { vm.refreshNativeCallLog() }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(tab.labelRes)) }) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    val label = stringResource(t.labelRes)
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.RECENTS -> RecentsTab(onSetDefaultDialer, onOpenNotificationAccess, vm)
                Tab.DIALPAD -> DialpadScreen()
                Tab.CONTACTS -> ContactsScreen()
                Tab.SETTINGS -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun RecentsTab(
    onSetDefaultDialer: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    vm: RecentsViewModel
) {
    val context = LocalContext.current
    val events by vm.events.collectAsState()

    var isDefaultDialer by remember { mutableStateOf(DefaultDialerHelper.isDefaultDialer(context)) }
    var hasNotifAccess by remember { mutableStateOf(DefaultDialerHelper.isNotificationAccessGranted(context)) }

    Column(Modifier.fillMaxSize()) {
        if (!isDefaultDialer) {
            SetupBanner(
                stringResource(R.string.setup_default_dialer_text),
                stringResource(R.string.setup_default_dialer_button)
            ) {
                onSetDefaultDialer()
                isDefaultDialer = DefaultDialerHelper.isDefaultDialer(context)
            }
        }
        if (!hasNotifAccess) {
            SetupBanner(
                stringResource(R.string.setup_notification_text),
                stringResource(R.string.setup_notification_button)
            ) {
                onOpenNotificationAccess()
                hasNotifAccess = DefaultDialerHelper.isNotificationAccessGranted(context)
            }
        }
        RecentsScreen(
            events = events,
            modifier = Modifier.fillMaxWidth(),
            onCall = { number -> CallPlacer.place(context, number) }
        )
    }
}

@Composable
private fun SetupBanner(text: String, button: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onClick, modifier = Modifier.padding(top = 8.dp)) { Text(button) }
    }
}
