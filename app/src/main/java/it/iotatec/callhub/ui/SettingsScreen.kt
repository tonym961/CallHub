package it.iotatec.callhub.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import it.iotatec.callhub.R
import it.iotatec.callhub.data.repo.BackupManager
import it.iotatec.callhub.data.repo.QuickRepliesRepository
import it.iotatec.callhub.dialer.spam.SpamRepository
import it.iotatec.callhub.dialer.spam.SystemBlocklist
import it.iotatec.callhub.sip.SipAccount
import it.iotatec.callhub.sip.SipAccountStore
import it.iotatec.callhub.sip.SipManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        val themeMode by AppTheme.mode.collectAsState()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { m ->
                FilterChip(
                    selected = themeMode == m,
                    onClick = { AppTheme.set(context, m) },
                    label = {
                        Text(
                            stringResource(
                                when (m) {
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                    ThemeMode.LIGHT -> R.string.theme_light
                                    ThemeMode.DARK -> R.string.theme_dark
                                }
                            )
                        )
                    }
                )
            }
        }
        val accent by AppTheme.accent.collectAsState()
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppTheme.ACCENT_PRESETS.forEach { c ->
                val selected = accent == c
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            BorderStroke(if (selected) 3.dp else 1.dp, MaterialTheme.colorScheme.outline),
                            CircleShape
                        )
                        .clickable { AppTheme.setAccent(context, c) }
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text(stringResource(R.string.settings_spam_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        var blockAnon by remember { mutableStateOf(SpamRepository.blockAnonymous(context)) }
        SwitchRow(stringResource(R.string.settings_block_anonymous), blockAnon) {
            blockAnon = it; SpamRepository.setBlockAnonymous(context, it)
        }

        var blockNonContacts by remember { mutableStateOf(SpamRepository.blockNonContacts(context)) }
        SwitchRow(stringResource(R.string.settings_block_non_contacts), blockNonContacts) {
            blockNonContacts = it; SpamRepository.setBlockNonContacts(context, it)
        }

        var newBlocked by remember { mutableStateOf("") }
        var blocked by remember { mutableStateOf(SpamRepository.blockedNumbers(context).toList()) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newBlocked,
                onValueChange = { newBlocked = it },
                label = { Text(stringResource(R.string.settings_number_to_block)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Button(onClick = {
                if (newBlocked.isNotBlank()) {
                    SpamRepository.addBlocked(context, newBlocked)
                    SystemBlocklist.block(context, newBlocked)
                    blocked = SpamRepository.blockedNumbers(context).toList()
                    newBlocked = ""
                }
            }) { Text(stringResource(R.string.action_block)) }
        }
        blocked.forEach { n ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(n)
                OutlinedButton(onClick = {
                    SpamRepository.removeBlocked(context, n)
                    SystemBlocklist.unblock(context, n)
                    blocked = SpamRepository.blockedNumbers(context).toList()
                }) { Text(stringResource(R.string.action_remove)) }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text(stringResource(R.string.settings_quick_replies), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        var newReply by remember { mutableStateOf("") }
        var replies by remember { mutableStateOf(QuickRepliesRepository.get(context)) }
        replies.forEach { r ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(r, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    QuickRepliesRepository.remove(context, r)
                    replies = QuickRepliesRepository.get(context)
                }) { Text(stringResource(R.string.action_remove)) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newReply,
                onValueChange = { newReply = it },
                label = { Text(stringResource(R.string.new_reply)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (newReply.isNotBlank()) {
                    QuickRepliesRepository.add(context, newReply)
                    replies = QuickRepliesRepository.get(context)
                    newReply = ""
                }
            }) { Text("+") }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text(stringResource(R.string.settings_sip_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        val existing = remember { SipAccountStore.load(context) }
        var displayName by remember { mutableStateOf(existing?.displayName ?: "") }
        var username by remember { mutableStateOf(existing?.username ?: "") }
        var domain by remember { mutableStateOf(existing?.domain ?: "") }
        var password by remember { mutableStateOf(existing?.password ?: "") }
        var port by remember { mutableStateOf((existing?.port ?: 5060).toString()) }

        OutlinedTextField(displayName, { displayName = it }, label = { Text(stringResource(R.string.sip_display_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(username, { username = it }, label = { Text(stringResource(R.string.sip_username)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(domain, { domain = it }, label = { Text(stringResource(R.string.sip_domain)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            password, { password = it }, label = { Text(stringResource(R.string.sip_password)) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            port, { port = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.sip_port)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
        )

        val registered by SipManager.registered.collectAsState()
        val statusWord = stringResource(if (registered) R.string.sip_registered else R.string.sip_not_registered)
        Text(
            stringResource(R.string.sip_status, statusWord),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                val account = SipAccount(
                    displayName = displayName.ifBlank { username },
                    username = username,
                    domain = domain,
                    password = password,
                    port = port.toIntOrNull() ?: 5060
                )
                SipAccountStore.save(context, account)
                SipManager.registerPhoneAccount(context, account)
            },
            enabled = username.isNotBlank() && domain.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.sip_save_register)) }

        Text(
            stringResource(R.string.sip_engine_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Text(stringResource(R.string.settings_backup), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(BackupManager.exportJson(context).toByteArray())
                }
            }
        }
        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                val json = context.contentResolver.openInputStream(it)?.use { ins -> ins.readBytes().decodeToString() }
                if (!json.isNullOrBlank()) runCatching { BackupManager.importJson(context, json) }
            }
        }
        val blocklistLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                val txt = context.contentResolver.openInputStream(it)?.use { ins -> ins.readBytes().decodeToString() }
                if (!txt.isNullOrBlank()) BackupManager.importBlocklist(context, txt)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { exportLauncher.launch("callhub-backup.json") }) { Text(stringResource(R.string.action_export)) }
            Button(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text(stringResource(R.string.action_import)) }
        }
        OutlinedButton(onClick = { blocklistLauncher.launch(arrayOf("text/plain")) }) {
            Text(stringResource(R.string.import_blocklist))
        }

        val scope = rememberCoroutineScope()
        var blocklistUrl by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = blocklistUrl,
                onValueChange = { blocklistUrl = it },
                label = { Text(stringResource(R.string.blocklist_url)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val url = blocklistUrl.trim()
                    if (url.isNotBlank()) scope.launch {
                        val text = withContext(Dispatchers.IO) {
                            runCatching { URL(url).readText() }.getOrNull()
                        }
                        if (!text.isNullOrBlank()) BackupManager.importBlocklist(context, text)
                        blocklistUrl = ""
                    }
                },
                enabled = blocklistUrl.isNotBlank()
            ) { Text(stringResource(R.string.action_import)) }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
