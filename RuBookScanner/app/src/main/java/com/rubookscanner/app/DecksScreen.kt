package com.rubookscanner.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlin.random.Random
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.rubookscanner.app.data.ParsedDeck
import com.rubookscanner.app.data.buildAllDecksExportText
import com.rubookscanner.app.data.buildDeckExportText
import com.rubookscanner.app.data.parseDecksFromText

@Composable
fun DecksScreen(
    decks: List<Deck>,
    activeDeckId: Long?,
    allCards: List<Flashcard>,
    onSetActive: (Long) -> Unit,
    onCreateDeck: (String) -> Unit,
    onDeleteDeck: (Long) -> Unit,
    onRenameDeck: (Long, String) -> Unit,
    onImportDecks: (List<ParsedDeck>, String) -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val t = LocalStrings.current
    var newDeckName by remember { mutableStateOf("") }
    var pendingExportText by remember { mutableStateOf<String?>(null) }
    var deckToDelete by remember { mutableStateOf<Deck?>(null) }
    var deckToRename by remember { mutableStateOf<Deck?>(null) }
    val createDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            writeTextToUri(context, uri, pendingExportText.orEmpty())
        }
        pendingExportText = null
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val parsed = parseDecksFromText(readTextFromUri(context, uri))
                if (parsed.sumOf { it.cards.size } == 0) {
                    onMessage(t.uploadNothingFound)
                } else {
                    onImportDecks(parsed, displayNameOf(context, uri))
                }
            } catch (e: Exception) {
                onMessage(t.errorPrefix(e.message))
            }
        }
    }

    deckToDelete?.let { deck ->
        AlertDialog(
            onDismissRequest = { deckToDelete = null },
            title = { Text(t.confirmDeleteDeck(deck.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteDeck(deck.id)
                    deckToDelete = null
                }) { Text(t.yesDelete) }
            },
            dismissButton = {
                TextButton(onClick = { deckToDelete = null }) { Text(t.cancel) }
            }
        )
    }
    deckToRename?.let { deck ->
        var renameText by remember(deck.id) { mutableStateOf(deck.name) }
        AlertDialog(
            onDismissRequest = { deckToRename = null },
            title = { Text(t.renameDeck) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameText.isNotBlank(),
                    onClick = {
                        onRenameDeck(deck.id, renameText.trim())
                        deckToRename = null
                    }
                ) { Text(t.save) }
            },
            dismissButton = {
                TextButton(onClick = { deckToRename = null }) { Text(t.cancel) }
            }
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            Modifier.padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newDeckName,
                onValueChange = { newDeckName = it },
                label = { Text(t.newDeckName) },
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
            ) { Text(t.create) }
        }

        OutlinedButton(
            onClick = {
                pendingExportText = buildAllDecksExportText(decks, allCards, t.emptyDeck)
                createDocLauncher.launch(t.allDecksFileName)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(t.downloadAll) }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { openDocLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(t.uploadDeck) }

        Divider(Modifier.padding(vertical = 12.dp))

        LazyColumn(Modifier.fillMaxSize()) {
                items(decks, key = { it.id }) { deck ->
                val count = allCards.count { it.deckId == deck.id }
                val ink = Color(0xFF3E3226)
                DeckSlab(
                    selected = deck.id == activeDeckId,
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = deck.id == activeDeckId,
                            onClick = { onSetActive(deck.id) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF1565C0),
                                unselectedColor = ink
                            )
                        )
                        // fehér címke, mint a kartonon
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                                .background(Color(0xFFF7F4EC), RoundedCornerShape(3.dp))
                                .border(1.5.dp, ink, RoundedCornerShape(3.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    deck.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    t.cardCount(count),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B5B4A)
                                )
                            }
                        }
                        IconButton(onClick = { deckToRename = deck }) {
                            Text("✎", fontSize = 22.sp, color = ink)
                        }
                        IconButton(onClick = {
                            val cardsForDeck = allCards.filter { it.deckId == deck.id }
                            pendingExportText = buildDeckExportText(deck.name, cardsForDeck, t.emptyDeck)
                            createDocLauncher.launch("${deck.name}.txt")
                        }) { Text("⬇", fontSize = 26.sp, color = ink) }
                        IconButton(onClick = { deckToDelete = deck }) {
                            Text("✕", color = ink)
                        }
                    }
                }
            }
        }
    }
}

private fun writeTextToUri(context: Context, uri: Uri, text: String) {
    context.contentResolver.openOutputStream(uri)?.use { stream ->
        stream.write(text.toByteArray(Charsets.UTF_8))
    }
}

private fun readTextFromUri(context: Context, uri: Uri): String =
    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        ?: ""

/** A kiválasztott fájl neve kiterjesztés nélkül (ha a fájlban nincs pakli-fejléc, ez lesz a pakli neve). */
private fun displayNameOf(context: Context, uri: Uri): String {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) name = c.getString(idx)
    }
    return (name ?: "").substringBeforeLast('.')
}
/**
 * Fektetett, kartonba csomagolt pakli: felső felület papír/karton textúrával, alatta
 * egy kicsit lejjebb és jobbra tolt alsó felület, a kettőt összekötő egy-egy kis vonallal.
 */
@Composable
private fun DeckSlab(
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val depth = 8.dp    // ennyivel van lejjebb az alsó felület
    val shift = 6.dp    // ennyivel van jobbra az alsó felület
    val radius = 12.dp
    val topLight = Color(0xFFD6C4A6)
    val topDark = Color(0xFFC6B08D)
    val bottomColor = Color(0xFF9C8765)
    val edgeColor = if (selected) Color(0xFF1565C0) else Color(0xFF6D5A43)
    Box(
        modifier = modifier
            .padding(end = shift, bottom = depth + 2.dp)
            .drawBehind {
                val dy = depth.toPx()
                val dx = shift.toPx()
                val rad = radius.toPx()
                val corner = CornerRadius(rad, rad)
                val lineW = if (selected) 2.5.dp.toPx() else 1.5.dp.toPx()
                val line = Stroke(width = lineW)
                val w = size.width
                val h = size.height

                // alsó felület (lejjebb és jobbra tolva)
                drawRoundRect(bottomColor, Offset(dx, dy), Size(w, h), corner)
                drawRoundRect(edgeColor, Offset(dx, dy), Size(w, h), corner, style = line)

                // a két szintet összekötő kis vonalak: bal oldalon lent, jobb oldalon fent
                drawLine(edgeColor, Offset(0f, h - rad), Offset(dx, h - rad + dy), lineW)
                drawLine(edgeColor, Offset(w, rad), Offset(w + dx, rad + dy), lineW)

                // felső felület: karton alapszín enyhe színátmenettel
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(topLight, topDark), startY = 0f, endY = h),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = corner
                )

                // papírszálak és apró pöttyök (mindig ugyanaz a minta, a felületen belül)
                val faceShape = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, rad, rad)) }
                clipPath(faceShape) {
                    val rnd = Random(7)
                    repeat(170) {
                        val x = rnd.nextFloat() * w
                        val y = rnd.nextFloat() * h
                        val len = (6 + rnd.nextFloat() * 24).dp.toPx()
                        val dark = rnd.nextBoolean()
                        drawLine(
                            if (dark) Color(0x22604A2E) else Color(0x33FFFFFF),
                            Offset(x, y),
                            Offset(x + len, y + (rnd.nextFloat() - 0.5f) * 2.dp.toPx()),
                            0.8.dp.toPx()
                        )
                    }
                    repeat(130) {
                        drawCircle(
                            Color(0x2A3E2A12),
                            radius = (0.5f + rnd.nextFloat()).dp.toPx(),
                            center = Offset(rnd.nextFloat() * w, rnd.nextFloat() * h)
                        )
                    }
                }

                drawRoundRect(edgeColor, Offset(0f, 0f), Size(w, h), corner, style = line)
            }
    ) {
        content()
    }
}
