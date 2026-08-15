package it.iotatec.callhub.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.iotatec.callhub.R
import it.iotatec.callhub.util.CallPlacer

@Composable
fun DialpadScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }

    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = number.ifEmpty { " " },
            fontSize = 34.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.weight(1f))

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (digit, letters) ->
                    DialKey(digit, letters) { number += digit }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.size(72.dp))
            Surface(
                onClick = { if (number.isNotBlank()) CallPlacer.place(context, number) },
                shape = CircleShape,
                color = Color(0xFF2E7D32),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .size(72.dp)
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
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DialKey(digit: String, letters: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .padding(6.dp)
            .size(72.dp)
            .aspectRatio(1f)
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
