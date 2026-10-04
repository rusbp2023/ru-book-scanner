package com.rubookscanner.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubookscanner.app.data.Deck
import com.rubookscanner.app.data.Flashcard
import com.rubookscanner.app.data.ParsedDeck
import com.rubookscanner.app.data.buildAllDecksExportText
import com.rubookscanner.app.data.buildDeckExportText
import com.rubookscanner.app.data.parseDecksFromText
import kotlin.random.Random

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
                val ink = Color(0xFFEDE6DA)
                val inkSoft = Color(0xFFB9B2A6)
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
                                selectedColor = Color(0xFF64B5F6),
                                unselectedColor = inkSoft
                            )
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
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
                                color = inkSoft
                            )
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
 * Fektetett, térhatású pakli: felül sötét, barnás-kékes papír felület, alatta ugyanolyan
 * széles, sötétebb oldalfelület adja a vastagságot. Csak halvány élvonalak vannak rajta.
 */
@Composable
private fun DeckSlab(
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val depth = 10.dp   // az oldalfelület vastagsága
    val radius = 14.dp
    val topLight = Color(0xFF454A5A)
    val topDark = Color(0xFF353A48)
    val sideLight = Color(0xFF2A2E39)
    val sideDark = Color(0xFF1C1F27)
    val edgeColor = if (selected) Color(0x4DFFFFFF) else Color(0x26FFFFFF)
    Box(
        modifier = modifier
            .padding(bottom = depth + 1.dp)
            .drawBehind {
                val dy = depth.toPx()
                val rad = radius.toPx()
                val corner = CornerRadius(rad, rad)
                val line = Stroke(width = 1.dp.toPx())
                val w = size.width
                val h = size.height

                // oldalfelület: a felsővel egyező szélességű, alul adja a vastagságot
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(sideLight, sideDark),
                        startY = h - rad,
                        endY = h + dy
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h + dy),
                    cornerRadius = corner
                )
                drawRoundRect(edgeColor, Offset(0f, 0f), Size(w, h + dy), corner, style = line)

                // felső felület: sötét, barnás-kékes alap enyhe színátmenettel
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(topLight, topDark), startY = 0f, endY = h),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = corner
                )

                // papírhatás: finom szálak és pöttyök (mindig ugyanaz a minta, a felületen belül)
                val faceShape = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, rad, rad)) }
                clipPath(faceShape) {
                    val rnd = Random(7)
                    repeat(190) {
                        val x = rnd.nextFloat() * w
                        val y = rnd.nextFloat() * h
                        val len = (6 + rnd.nextFloat() * 26).dp.toPx()
                        val light = rnd.nextBoolean()
                        drawLine(
                            if (light) Color(0x1FE8D5B5) else Color(0x26000000),
                            Offset(x, y),
                            Offset(x + len, y + (rnd.nextFloat() - 0.5f) * 2.dp.toPx()),
                            0.8.dp.toPx()
                        )
                    }
                    repeat(120) {
                        drawCircle(
                            if (rnd.nextBoolean()) Color(0x33000000) else Color(0x14E8D5B5),
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
