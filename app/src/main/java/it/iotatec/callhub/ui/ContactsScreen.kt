package it.iotatec.callhub.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.iotatec.callhub.R
import it.iotatec.callhub.data.repo.ContactsRepository
import it.iotatec.callhub.data.repo.FavoritesRepository
import it.iotatec.callhub.data.repo.PhoneContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val AvatarColors = listOf(
    Color(0xFF1565C0), Color(0xFF6A1B9A), Color(0xFF00838F), Color(0xFFC62828),
    Color(0xFFEF6C00), Color(0xFF2E7D32), Color(0xFF4527A0), Color(0xFFAD1457)
)

private fun colorFor(name: String): Color =
    AvatarColors[(name.hashCode() and 0x7FFFFFFF) % AvatarColors.size]

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(modifier: Modifier = Modifier, onCall: (String) -> Unit = {}) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var favorites by remember { mutableStateOf(FavoritesRepository.favorites(context)) }

    val contacts by produceState(initialValue = emptyList<PhoneContact>()) {
        value = withContext(Dispatchers.IO) { ContactsRepository.load(context) }
    }

    val filtered = remember(contacts, query) {
        if (query.isBlank()) contacts
        else contacts.filter { it.name.contains(query, true) || it.number.contains(query) }
    }
    val favoriteContacts = remember(contacts, favorites) { contacts.filter { favorites.contains(it.number) } }
    val grouped = remember(filtered) {
        filtered.groupBy { it.name.trim().firstOrNull()?.uppercaseChar()?.takeIf { c -> c.isLetter() } ?: '#' }
            .toSortedMap()
    }

    val onToggleFav: (String) -> Unit = { number ->
        FavoritesRepository.toggle(context, number)
        favorites = FavoritesRepository.favorites(context)
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.search_contact)) },
            singleLine = true,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (filtered.isEmpty() && favoriteContacts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_contacts), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize()) {
            if (query.isBlank() && favoriteContacts.isNotEmpty()) {
                stickyHeader { SectionHeader(stringResource(R.string.favorites_title)) }
                items(favoriteContacts, key = { "fav_" + it.name + it.number }) { c ->
                    ContactRow(c, true, onToggleFav) { onCall(c.number) }
                }
            }
            grouped.forEach { (letter, list) ->
                stickyHeader { SectionHeader(letter.toString()) }
                items(list, key = { it.name + it.number }) { c ->
                    ContactRow(c, favorites.contains(c.number), onToggleFav) { onCall(c.number) }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 4.dp)
        )
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCall)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ContactAvatar(contact)
        Column(Modifier.weight(1f)) {
            Text(contact.name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
            Text(contact.number, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { onToggleFav(contact.number) }) {
            Icon(
                if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
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
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use { s ->
                        BitmapFactory.decodeStream(s)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    val bitmap = photo
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contact.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(46.dp).clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape).background(colorFor(contact.name)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                contact.name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}
