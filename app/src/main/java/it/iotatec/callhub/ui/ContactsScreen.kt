package it.iotatec.callhub.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.iotatec.callhub.R
import it.iotatec.callhub.data.repo.ContactsRepository
import it.iotatec.callhub.data.repo.FavoritesRepository
import it.iotatec.callhub.data.repo.PhoneContact
import it.iotatec.callhub.util.CallPlacer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ContactsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var favorites by remember { mutableStateOf(FavoritesRepository.favorites(context)) }

    val contacts by produceState(initialValue = emptyList<PhoneContact>()) {
        value = withContext(Dispatchers.IO) { ContactsRepository.load(context) }
    }

    val filtered = remember(contacts, query) {
        if (query.isBlank()) contacts
        else contacts.filter {
            it.name.contains(query, ignoreCase = true) || it.number.contains(query)
        }
    }
    val favoriteContacts = remember(contacts, favorites) {
        contacts.filter { favorites.contains(it.number) }
    }

    val onToggleFav: (String) -> Unit = { number ->
        FavoritesRepository.toggle(context, number)
        favorites = FavoritesRepository.favorites(context)
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.search_contact)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        if (filtered.isEmpty() && favoriteContacts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_contacts), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                if (query.isBlank() && favoriteContacts.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.favorites_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(favoriteContacts, key = { "fav_" + it.name + it.number }) { c ->
                        ContactRow(c, isFavorite = true, onToggleFav = onToggleFav) { CallPlacer.place(context, c.number) }
                        HorizontalDivider()
                    }
                }
                items(filtered, key = { it.name + it.number }) { c ->
                    ContactRow(c, isFavorite = favorites.contains(c.number), onToggleFav = onToggleFav) { CallPlacer.place(context, c.number) }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: PhoneContact,
    isFavorite: Boolean,
    onToggleFav: (String) -> Unit,
    onCall: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onCall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ContactAvatar(contact)
            Column {
                Text(contact.name, fontWeight = FontWeight.Medium)
                Text(contact.number, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        IconButton(onClick = { onToggleFav(contact.number) }) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContactAvatar(contact: PhoneContact) {
    val context = LocalContext.current
    val photo by produceState<ImageBitmap?>(initialValue = null, contact.photoUri) {
        value = contact.photoUri?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
        val bitmap = photo
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = contact.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(contact.name.firstOrNull()?.uppercase() ?: "?", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
        }
    }
}
