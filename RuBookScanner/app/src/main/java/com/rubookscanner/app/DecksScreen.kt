package com.rubookscanner.app

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    onOpenDeck: (Long) -> Unit,
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
    var helpDialog by remember { mutableStateOf<Int?>(null) } // 0 = letöltés, 1 = feltöltés
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
    helpDialog?.let { which ->
        AlertDialog(
            onDismissRequest = { helpDialog = null },
            title = { Text(if (which == 0) t.helpDownloadTitle else t.helpUploadTitle) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        if (which == 0) t.helpDownloadBody else t.helpUploadBody,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { helpDialog = null }) { Text(t.closeLabel) }
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

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WButton(
                onClick = {
                    pendingExportText = buildAllDecksExportText(decks, allCards, t.emptyDeck)
                    createDocLauncher.launch(t.allDecksFileName)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) { WLabel(t.downloadAll) }
            Spacer(Modifier.width(8.dp))
            HelpButton { helpDialog = 0 }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            WButton(
                onClick = { openDocLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) { WLabel(t.uploadDeck) }
            Spacer(Modifier.width(8.dp))
            HelpButton { helpDialog = 1 }
        }

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
                                .height(84.dp)
                                .padding(start = 10.dp, end = 44.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DeckRadio(
                                selected = deck.id == activeDeckId,
                                onClick = { onSetActive(deck.id) }
                            )
                            // a név rész besüllyeszthető gomb: megnyomva a Kártya fülre ugrik
                            DeckNameButton(
                                onClick = { onOpenDeck(deck.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(start = 0.dp, end = 0.dp, top = 5.dp, bottom = 5.dp)
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

/** Kis négyzet alakú "?" gomb: a letöltés / feltöltés tudnivalóit nyitja meg. */
@Composable
private fun HelpButton(onClick: () -> Unit) {
    WButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        contentPadding = PaddingValues(0.dp)
    ) { Text("?", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
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
 * A pakli neve körüli besüllyesztett, gombszerű rész: balra és jobbra egy-egy függőleges
 * szegéllyel (árok), enyhén sötétebb belsővel. Megnyomva mélyebbre süllyed.
 */
@Composable
private fun DeckNameButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .drawBehind {
                val w = size.width
                val h = size.height
                val r = 8.dp.toPx()
                val corner = CornerRadius(r, r)

                // alig sötétebb belső: szinte egyezik a bársonnyal, nyomáskor mélyebb
                drawRoundRect(
                    if (pressed) Color(0x40000000) else Color(0x14000000),
                    Offset(0f, 0f),
                    Size(w, h),
                    corner
                )
                // finom belső árnyék a felső élnél
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(if (pressed) Color(0x59000000) else Color(0x26000000), Color(0x00000000)),
                        startY = 0f,
                        endY = 6.dp.toPx()
                    ),
                    topLeft = Offset(r, 0f),
                    size = Size(w - 2 * r, 6.dp.toPx())
                )
                // bemélyedt perem: alul/jobbra halvány fényes ajak, fölötte sötét vékony vonal
                val lw = 1.dp.toPx()
                drawRoundRect(
                    Color(0x26FFC8B4),
                    Offset(lw / 2, lw / 2 + 1.dp.toPx()),
                    Size(w - lw, h - lw),
                    corner,
                    style = Stroke(lw)
                )
                drawRoundRect(
                    Color(0x9E000000),
                    Offset(lw / 2, lw / 2),
                    Size(w - lw, h - lw),
                    corner,
                    style = Stroke(lw)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .offset(y = if (pressed) 1.dp else 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

/**
 * Kristálygömb-szerű rádiógomb: aktívan mélykék, áttetsző, fényes üveggömb ragyogással,
 * inaktívan sötét, halvány üveggömb a foglalatában.
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
                    // mélykék, áttetsző kristálygömb: kék ragyogás, árnyék, üvegtest,
                    // belső fénytörés, peremfény és csillanás
                    drawCircle(Color(0x222F6BFF), radius = socket + 5.dp.toPx(), center = c)
                    drawCircle(Color(0x442F6BFF), radius = socket + 2.dp.toPx(), center = c)
                    drawCircle(Color(0x66000000), radius = r, center = c + Offset(0f, 2.dp.toPx()))
                    // üvegtest: felül világosabb, szélén mélykék, enyhén áttetsző
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xE6A8C8FF), Color(0xCC2F67E8), Color(0xF20B1E66)),
                            center = c + Offset(-r * 0.3f, -r * 0.35f),
                            radius = r * 1.5f
                        ),
                        radius = r,
                        center = c
                    )
                    // belső fénytörés lent jobbra (a gömbön átszűrődő fény)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x992F9BFF), Color(0x002F9BFF)),
                            center = c + Offset(r * 0.3f, r * 0.35f),
                            radius = r * 0.6f
                        ),
                        radius = r * 0.6f,
                        center = c + Offset(r * 0.3f, r * 0.35f)
                    )
                    // peremfény
                    drawCircle(Color(0x66BBD8FF), radius = r, center = c, style = Stroke(1.dp.toPx()))
                    // csillanás
                    drawCircle(
                        Color(0xCCFFFFFF),
                        radius = r * 0.24f,
                        center = c + Offset(-r * 0.4f, -r * 0.45f)
                    )
                    drawCircle(
                        Color(0x66FFFFFF),
                        radius = r * 0.1f,
                        center = c + Offset(r * 0.38f, r * 0.5f)
                    )
                } else {
                    // inaktív: sötét, áttetsző üveggömb a foglalatában
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF263247), Color(0xFF0E131C)),
                            center = c + Offset(-r * 0.2f, -r * 0.25f),
                            radius = r
                        ),
                        radius = r * 0.8f,
                        center = c
                    )
                    drawCircle(Color(0x33BBD8FF), radius = r * 0.8f, center = c, style = Stroke(1.dp.toPx()))
                    drawCircle(
                        Color(0x40FFFFFF),
                        radius = r * 0.14f,
                        center = c + Offset(-r * 0.3f, -r * 0.35f)
                    )
                }
            }
    )
}
