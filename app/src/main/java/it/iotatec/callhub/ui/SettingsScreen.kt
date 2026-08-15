package it.iotatec.callhub.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import it.iotatec.callhub.R
import it.iotatec.callhub.dialer.spam.SpamRepository
import it.iotatec.callhub.dialer.spam.SystemBlocklist
import it.iotatec.callhub.sip.SipAccount
import it.iotatec.callhub.sip.SipAccountStore
import it.iotatec.callhub.sip.SipManager

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
