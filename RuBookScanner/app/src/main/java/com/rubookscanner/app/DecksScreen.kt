package com.rubookscanner.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
                val paperInk = Color(0xFF2E2A26)
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
                            // a papírcsík ezen a részen fut körbe a pakli borítóján
                            Box(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    Modifier.graphicsLayer { rotationZ = -2.5f },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        deck.name,
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp,
                                        color = paperInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        t.cardCount(count),
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 12.sp,
                                        color = Color(0xFF55504A)
                                    )
                                }
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
 * alatta az elülső oldalfelület adja a vastagságot. A rádiógomb és a ceruza ikon közötti részen
 * szürkés papírcsík fut körbe (a felső és az oldalfelületen is).
 * A bandStart / bandEndInset a papírcsík helye: balról ennyire kezdődik, jobbról ennyi marad ki.
 */
@Composable
private fun DeckSlab(
    modifier: Modifier = Modifier,
    bandStart: Dp = 58.dp,
    bandEndInset: Dp = 88.dp,
    content: @Composable () -> Unit
) {
    val depth = 12.dp   // az oldalfelület vastagsága
    val velvetLight = Color(0xFF3A4054)
    val velvetDark = Color(0xFF262B3A)
    val sideLight = Color(0xFF1F2330)
    val sideDark = Color(0xFF14171F)
    val bandLight = Color(0xFFCAC7C0)
    val bandDark = Color(0xFFB4B0A7)
    val bandSideLight = Color(0xFF8F8B83)
    val bandSideDark = Color(0xFF6E6A63)
    val edgeColor = Color(0x26FFFFFF)
    Box(
        modifier = modifier
            .padding(bottom = depth + 1.dp)
            .drawBehind {
                val dy = depth.toPx()
                val line = Stroke(width = 1.dp.toPx())
                val w = size.width
                val h = size.height
                val x1 = bandStart.toPx()
                val x2 = w - bandEndInset.toPx()

                // 1) elülső oldalfelület (vastagság)
                drawRect(
                    Brush.verticalGradient(listOf(sideLight, sideDark), startY = h, endY = h + dy),
                    Offset(0f, h),
                    Size(w, dy)
                )

                // 2) bársony felső felület
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

                // halvány élek: külső körvonal + a felső és az elülső felület találkozása (megvilágított él)
                drawRect(edgeColor, Offset(0f, 0f), Size(w, h + dy), style = line)
                drawLine(Color(0x40FFFFFF), Offset(0f, h), Offset(w, h), 1.dp.toPx())

                // 3) körbefutó papírcsík
                if (x2 - x1 > 20f) {
                    val bw = x2 - x1
                    val sh = 5.dp.toPx()
                    // árnyék a csík két oldalán, a bársonyon
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0x55000000)),
                            startX = x1 - sh,
                            endX = x1
                        ),
                        Offset(x1 - sh, 0f),
                        Size(sh, h + dy)
                    )
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color(0x55000000), Color.Transparent),
                            startX = x2,
                            endX = x2 + sh
                        ),
                        Offset(x2, 0f),
                        Size(sh, h + dy)
                    )
                    // a csík az oldalfelületen
                    drawRect(
                        Brush.verticalGradient(listOf(bandSideLight, bandSideDark), startY = h, endY = h + dy),
                        Offset(x1, h),
                        Size(bw, dy)
                    )
                    // a csík a felső felületen
                    drawRect(
                        Brush.verticalGradient(listOf(bandLight, bandDark), startY = 0f, endY = h),
                        Offset(x1, 0f),
                        Size(bw, h)
                    )
                    // papírszálak
                    clipRect(x1, 0f, x2, h) {
                        val prnd = Random(5)
                        repeat(70) {
                            val x = x1 + prnd.nextFloat() * bw
                            val y = prnd.nextFloat() * h
                            val len = (10 + prnd.nextFloat() * 20).dp.toPx()
                            drawLine(
                                if (prnd.nextBoolean()) Color(0x1A000000) else Color(0x22FFFFFF),
                                Offset(x, y),
                                Offset(x + len, y + (prnd.nextFloat() - 0.5f) * 2.dp.toPx()),
                                0.7.dp.toPx()
                            )
                        }
                    }
                    // a csík élei
                    drawLine(Color(0x44000000), Offset(x1, 0f), Offset(x1, h + dy), 1.dp.toPx())
                    drawLine(Color(0x44000000), Offset(x2, 0f), Offset(x2, h + dy), 1.dp.toPx())
                    drawLine(Color(0x40000000), Offset(x1, h), Offset(x2, h), 1.dp.toPx())
                }
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
