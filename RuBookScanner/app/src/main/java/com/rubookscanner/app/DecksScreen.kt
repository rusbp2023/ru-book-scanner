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
import androidx.compose.material3.Divider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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

        Divider(Modifier.padding(vertical = 12.dp))

        LazyColumn(Modifier.fillMaxSize()) {
            items(decks, key = { it.id }) { deck ->
                val count = allCards.count { it.deckId == deck.id }
                val ink = Color(0xFFEDE6DA)
                val inkSoft = Color(0xFFB9B2A6)
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
 * Fektetett, tégla alakú pakli éles sarkokkal: sötét, barnás-kékes bársony felső felület,
 * alatta az elülső oldalfelület adja a vastagságot, jobb oldalon pedig egy keskeny, ferde
 * élű oldalfelület látszik (dobozszerű 3D hatás).
 */
@Composable
private fun DeckSlab(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val depth = 12.dp       // az elülső oldalfelület vastagsága
    val sideWidth = 14.dp   // a jobb oldali keskeny oldalfelület szélessége
    val slant = 6.dp        // a jobb oldalfelület felső és alsó élének dőlése
    val velvetLight = Color(0xFF3A4054)
    val velvetDark = Color(0xFF262B3A)
    val frontLight = Color(0xFF1F2330)
    val frontDark = Color(0xFF14171F)
    val rightLight = Color(0xFF181B26)
    val rightDark = Color(0xFF0E1016)
    val edgeColor = Color(0x2EFFFFFF)
    Box(
        modifier = modifier
            .padding(bottom = depth + 1.dp)
            .drawBehind {
                val dy = depth.toPx()
                val sd = sideWidth.toPx()
                val sl = slant.toPx()
                val line = Stroke(width = 1.dp.toPx())
                val w = size.width - sd   // a felső felület szélessége
                val h = size.height       // a felső felület magassága

                // 1) elülső oldalfelület (vastagság)
                drawRect(
                    Brush.verticalGradient(listOf(frontLight, frontDark), startY = h, endY = h + dy),
                    Offset(0f, h),
                    Size(w, dy)
                )

                // 2) jobb oldali keskeny oldalfelület: ferde felső és alsó éllel
                val rightFace = Path().apply {
                    moveTo(w, 0f)
                    lineTo(w + sd, sl)
                    lineTo(w + sd, h + dy - sl)
                    lineTo(w, h + dy)
                    close()
                }
                drawPath(
                    rightFace,
                    Brush.horizontalGradient(listOf(rightLight, rightDark), startX = w, endX = w + sd)
                )

                // 3) bársony felső felület
                drawRect(
                    Brush.verticalGradient(listOf(velvetLight, velvetDark), startY = 0f, endY = h),
                    Offset(0f, 0f),
                    Size(w, h)
                )
                clipRect(0f, 0f, w, h) {
                    // puha fényes sáv középen + felül, finom bársony-"szőr" pöttyökkel
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0x00FFFFFF), Color(0x1AFFFFFF), Color(0x00FFFFFF)),
                            startX = 0f,
                            endX = w
                        ),
                        size = Size(w, h)
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0x16FFFFFF), Color(0x00FFFFFF)),
                            startY = 0f,
                            endY = h * 0.55f
                        ),
                        size = Size(w, h)
                    )
                    val rnd = Random(11)
                    repeat(420) {
                        drawCircle(
                            if (rnd.nextBoolean()) Color(0x14FFFFFF) else Color(0x1F000000),
                            radius = (0.4f + rnd.nextFloat() * 0.5f).dp.toPx(),
                            center = Offset(rnd.nextFloat() * w, rnd.nextFloat() * h)
                        )
                    }
                }

                // 4) halvány élek
                drawRect(edgeColor, Offset(0f, 0f), Size(w, h), style = line)
                drawRect(edgeColor, Offset(0f, h), Size(w, dy), style = line)
                drawPath(rightFace, edgeColor, style = line)
                // megvilágított élek: a felső és az elülső felület találkozása, valamint a ferde felső él
                drawLine(Color(0x40FFFFFF), Offset(0f, h), Offset(w, h), 1.dp.toPx())
                drawLine(Color(0x33FFFFFF), Offset(w, 0f), Offset(w + sd, sl), 1.dp.toPx())
            }
    ) {
        // a tartalom csak a felső felületen belül marad (a jobb oldalfelület helyét kihagyjuk)
        Box(Modifier.padding(end = sideWidth)) {
            content()
        }
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
