package it.iotatec.callhub.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import it.iotatec.callhub.R
import it.iotatec.callhub.data.repo.ContactsRepository
import it.iotatec.callhub.data.repo.PhoneContact
import it.iotatec.callhub.util.CallPlacer
import it.iotatec.callhub.util.SimSelector
import it.iotatec.callhub.util.T9
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DialpadScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }
    var pendingCall by remember { mutableStateOf<String?>(null) }

    val contacts by produceState(initialValue = emptyList<PhoneContact>()) {
        value = withContext(Dispatchers.IO) { ContactsRepository.load(context) }
    }
    val sims = remember { SimSelector.accounts(context) }

    // T9 matches for the typed digits.
    val digits = number.filter { it.isDigit() }
    val matches = remember(contacts, digits) {
        if (digits.isEmpty()) emptyList()
        else contacts.filter { T9.matches(it.name, it.number, digits) }.take(25)
    }

    val placeCall: (String) -> Unit = { target ->
        if (target.isNotBlank()) {
            if (sims.size > 1) pendingCall = target
            else CallPlacer.place(context, target)
        }
    }

    // SIM chooser for dual-SIM devices.
    pendingCall?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingCall = null },
            confirmButton = {},
            title = { Text(stringResource(R.string.choose_sim)) },
            text = {
                Column {
                    sims.forEach { sim ->
                        TextButton(onClick = {
                            CallPlacer.place(context, target, sim.handle)
                            pendingCall = null
                        }) { Text(sim.label) }
                    }
                }
            }
        )
    }

    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number.ifEmpty { " " },
            fontSize = 34.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        // T9 suggestions fill the space above the keypad.
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (matches.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(matches, key = { it.name + it.number }) { c ->
                        MatchRow(c) { placeCall(c.number) }
                        HorizontalDivider()
                    }
                }
            }
        }

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (digit, letters) ->
                    DialKey(digit, letters) { number += digit }
                }
            }
            Spacer12()
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer72()
            Surface(
                onClick = { placeCall(number) },
                shape = CircleShape,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(horizontal = 24.dp).size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Call, contentDescription = stringResource(R.string.action_call), tint = Color.White)
                }
            }
            IconButton(
                onClick = { if (number.isNotEmpty()) number = number.dropLast(1) },
                modifier = Modifier.size(72.dp)
            ) {
                if (number.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchRow(contact: PhoneContact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(contact.name.firstOrNull()?.uppercase() ?: "?", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
        }
        Column {
            Text(contact.name, fontWeight = FontWeight.Medium)
            Text(contact.number, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Spacer12() = Box(Modifier.height(12.dp))

@Composable
private fun Spacer72() = Box(Modifier.size(72.dp))

@Composable
private fun DialKey(digit: String, letters: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(6.dp).size(72.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(digit, fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface)
            if (letters.isNotEmpty()) {
                Text(letters, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
