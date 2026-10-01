package com.rubookscanner.app

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rubookscanner.app.data.Deck
import com.rubookscanner.app.data.Flashcard
import com.rubookscanner.app.data.buildAllDecksExportText
import com.rubookscanner.app.data.buildDeckExportText

@Composable
fun DecksScreen(
    decks: List<Deck>,
    activeDeckId: Long?,
    allCards: List<Flashcard>,
    onSetActive: (Long) -> Unit,
    onCreateDeck: (String) -> Unit,
    onDeleteDeck: (Long) -> Unit
) {
    val context = LocalContext.current
    var newDeckName by remember { mutableStateOf("") }
    var pendingExportText by remember { mutableStateOf<String?>(null) }

    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            writeTextToUri(context, uri, pendingExportText.orEmpty())
        }
        pendingExportText = null
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Paklik", style = MaterialTheme.typography.titleLarge)

        Row(
            Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newDeckName,
                onValueChange = { newDeckName = it },
                label = { Text("Új pakli neve") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (newDeckName.isNotBlank()) {
                        onCreateDeck(newDeckName.trim())
                        newDeckName = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) { Text("Létrehozás") }
        }

        OutlinedButton(
            onClick = {
                pendingExportText = buildAllDecksExportText(decks, allCards)
                createDocLauncher.launch("osszes_pakli.txt")
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Összes pakli letöltése") }

        Divider(Modifier.padding(vertical = 12.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            items(decks, key = { it.id }) { deck ->
                val count = allCards.count { it.deckId == deck.id }
                Card(
                    Modifier
                        .padding(vertical = 6.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = deck.id == activeDeckId,
                            onClick = { onSetActive(deck.id) }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(deck.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "$count kártya",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = {
                            val cardsForDeck = allCards.filter { it.deckId == deck.id }
                            pendingExportText = buildDeckExportText(deck.name, cardsForDeck)
                            createDocLauncher.launch("${deck.name}.txt")
                        }) { Text("⬇") }
                        IconButton(onClick = { onDeleteDeck(deck.id) }) { Text("✕") }
                    }
                }
            }
        }
    }
}

private fun writeTextToUri(context: Context, uri: android.net.Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(text.toByteArray(Charsets.UTF_8))
    }
}
