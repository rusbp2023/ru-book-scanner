package com.rubookscanner.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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
        // az új pakli neve és a létrehozás gomb egy vonalban, egyforma (56 dp) magasan
        Row(
            Modifier.padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WTextField(
                value = newDeckName,
                onValueChange = { newDeckName = it },
                placeholder = t.newDeckName,
                modifier = Modifier.weight(1f)
            )
            WButton(
                onClick = {
                    if (newDeckName.isNotBlank()) {
                        onCreateDeck(newDeckName.trim())
                        newDeckName = ""
                    }
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .height(56.dp)
            ) { WLabel(t.create) }
        }

        WButton(
            onClick = {
                pendingExportText = buildAllDecksExportText(decks, allCards, t.emptyDeck)
                createDocLauncher.launch(t.allDecksFileName)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) { WLabel(t.downloadAll) }

        Spacer(Modifier.height(8.dp))

        WButton(
            onClick = { openDocLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) { WLabel(t.uploadDeck) }

        Spacer(Modifier.height(12.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            items(decks, key = { it.id }) { deck ->
                val count = allCards.count { it.deckId == deck.id }
                val ink = Color(0xFFEDE6DA)
                val inkSoft = Color(0xFFD2BBB0)
                DeckSlab(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .fillMaxWidth()
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 84.dp)
                                .padding(start = 10.dp, end = 44.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DeckRadio(
                                selected = deck.id == activeDeckId,
                                onClick = { onSetActive(deck.id) }
                            )
                            Column(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    deck.name,
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 20.sp,
                                    color = ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    t.cardCount(count),
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 12.sp,
                                    color = inkSoft
                                )
                            }
                            IconButton(
                                onClick = { deckToRename = deck },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Text("✎", fontSize = 22.sp, color = ink)
                            }
                        }
                        // jobb felső sarok: törlés
                        IconButton(
                            onClick = { deckToDelete = deck },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 2.dp, end = 4.dp)
                                .size(40.dp)
                        ) {
                            Text("✕", color = ink)
                        }
                        // jobb alsó sarok: letöltés
                        IconButton(
                            onClick = {
                                val cardsForDeck = allCards.filter { it.deckId == deck.id }
                                pendingExportText = buildDeckExportText(deck.name, cardsForDeck, t.emptyDeck)
                                createDocLauncher.launch("${deck.name}.txt")
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 2.dp, end = 4.dp)
                                .size(40.dp)
                        ) {
                            Text("⬇", fontSize = 22.sp, color = ink)
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
 * Lapos, de kissé domború pakli: mélyvörös-barnás bársony felület, gradienses élekkel
 * (felül világosabb, alul és oldalt sötétedő), finom bársony-"szőrrel" és halvány árnyékkal.
 */
@Composable
private fun DeckSlab(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val radius = 14.dp
    val velvetLight = Color(0xFF63302F)
    val velvetDark = Color(0xFF3A1717)
    Box(
        modifier = modifier
            .padding(bottom = 4.dp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val rad = radius.toPx()
                val corner = CornerRadius(rad, rad)

                // halvány árnyék a pakli alatt
                drawRoundRect(Color(0x66000000), Offset(0f, 3.dp.toPx()), Size(w, h), corner)

                // bársony alap, enyhe függőleges színátmenettel
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(velvetLight, velvetDark), startY = 0f, endY = h),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = corner
                )

                val shape = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, rad, rad)) }
                clipPath(shape) {
                    // puha fényes sáv középen (bársony fényvisszaverés)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0x00FFFFFF), Color(0x1AFFC8B4), Color(0x00FFFFFF)),
                            startX = 0f,
                            endX = w
                        ),
                        size = Size(w, h)
                    )
                    // domborúság: felül világosabb, alul sötétedő
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0x2EFFFFFF), Color(0x00FFFFFF)),
                            startY = 0f,
                            endY = h * 0.45f
                        ),
                        size = Size(w, h)
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0x00000000), Color(0x4D000000)),
                            startY = h * 0.55f,
                            endY = h
                        ),
                        size = Size(w, h)
                    )
                    // oldalt sötétedő élek (a domborúság lekerekedése)
                    val edgeW = 18.dp.toPx()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0x66000000), Color(0x00000000)),
                            startX = 0f,
                            endX = edgeW
                        ),
                        size = Size(w, h)
                    )
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0x00000000), Color(0x66000000)),
                            startX = w - edgeW,
                            endX = w
                        ),
                        size = Size(w, h)
                    )
                    // finom bársony-"szőr" pöttyök
                    val rnd = Random(11)
                    repeat(420) {
                        drawCircle(
                            if (rnd.nextBoolean()) Color(0x16FFD2C0) else Color(0x22000000),
                            radius = (0.4f + rnd.nextFloat() * 0.5f).dp.toPx(),
                            center = Offset(rnd.nextFloat() * w, rnd.nextFloat() * h)
                        )
                    }
                }

                // gradienses perem: felül világos, alul sötét
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0x66FFC8B4), Color(0x14FFFFFF), Color(0x80000000)),
                        startY = 0f,
                        endY = h
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h),
                    cornerRadius = corner,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
    ) {
        content()
    }
}

/**
 * Sárga, kiemelkedő, gombszerű rádiógomb. Aktívan fényes és kiemelkedik (kis fényfolttal és
 * árnyékkal), inaktívan sötétebb és lesüllyedt a foglalatába.
 */
@Composable
private fun DeckRadio(selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            )
            .drawBehind {
                val c = center
                val r = 13.dp.toPx()
                val socket = r + 4.dp.toPx()
                // foglalat
                drawCircle(Color(0xFF111319), radius = socket, center = c)
                drawCircle(Color(0x22FFFFFF), radius = socket, center = c, style = Stroke(1.dp.toPx()))
                if (selected) {
                    // halvány ragyogás, árnyék, domború gomb, fényfolt
                    drawCircle(Color(0x33FFD600), radius = socket + 3.dp.toPx(), center = c)
                    drawCircle(Color(0x66000000), radius = r, center = c + Offset(0f, 2.dp.toPx()))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFF59D), Color(0xFFFFD600), Color(0xFFC49A00)),
                            center = c + Offset(-r * 0.35f, -r * 0.4f),
                            radius = r * 1.7f
                        ),
                        radius = r,
                        center = c
                    )
                    drawCircle(
                        Color(0x99FFFFFF),
                        radius = r * 0.28f,
                        center = c + Offset(-r * 0.4f, -r * 0.45f)
                    )
                } else {
                    // lesüllyedt, sötét sárga
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF6B5F22), Color(0xFF3D3715)),
                            center = c,
                            radius = r
                        ),
                        radius = r * 0.8f,
                        center = c
                    )
                }
            }
    )
}
