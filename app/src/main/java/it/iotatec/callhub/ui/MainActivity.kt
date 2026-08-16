package it.iotatec.callhub.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import it.iotatec.callhub.util.CallingAccounts
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
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO
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
    var settingsRoute by remember { mutableStateOf<SettingsRoute?>(null) }
    var pendingCall by remember { mutableStateOf<String?>(null) }
    val inSettingsDetail = tab == Tab.SETTINGS && settingsRoute != null

    // Shared call handler: ask which SIM / SIP account when there is more than one.
    val onCall: (String) -> Unit = { number ->
        val options = CallingAccounts.list(context)
        if (options.size <= 1) CallPlacer.place(context, number, options.firstOrNull()?.handle)
        else pendingCall = number
    }

    LaunchedEffect(Unit) { vm.refreshNativeCallLog() }
    BackHandler(enabled = inSettingsDetail) { settingsRoute = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (inSettingsDetail) settingsRoute!!.labelRes else tab.labelRes)) },
                navigationIcon = {
                    if (inSettingsDetail) {
                        IconButton(onClick = { settingsRoute = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    val label = stringResource(t.labelRes)
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { settingsRoute = null; tab = t },
                        icon = { Icon(t.icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.RECENTS -> RecentsTab(onSetDefaultDialer, onOpenNotificationAccess, vm, onCall)
                Tab.DIALPAD -> DialpadScreen(onCall = onCall)
                Tab.CONTACTS -> ContactsScreen(onCall = onCall)
                Tab.SETTINGS -> SettingsScreen(route = settingsRoute, onOpen = { settingsRoute = it })
            }
        }
    }

    pendingCall?.let { number ->
        AlertDialog(
            onDismissRequest = { pendingCall = null },
            confirmButton = {},
            title = { Text(stringResource(R.string.choose_sim)) },
            text = {
                Column {
                    CallingAccounts.list(context).forEach { opt ->
                        TextButton(onClick = { CallPlacer.place(context, number, opt.handle); pendingCall = null }) {
                            Text(opt.label)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun RecentsTab(
    onSetDefaultDialer: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    vm: RecentsViewModel,
    onCall: (String) -> Unit
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
            onCall = onCall,
            onSaveNote = { id, note -> vm.setNote(id, note) }
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
