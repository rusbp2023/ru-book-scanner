package com.rubookscanner.app

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.rubookscanner.app.data.AiClient
import com.rubookscanner.app.data.CardFront
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubookscanner.app.data.AiProvider
import com.rubookscanner.app.data.AiSettings
import com.rubookscanner.app.data.AppLang
import com.rubookscanner.app.data.Deck
import com.rubookscanner.app.data.Flashcard
import com.rubookscanner.app.data.WordItem
import com.rubookscanner.app.data.ProviderConfig
import com.rubookscanner.app.data.defaultModelFor
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.launch

/** A felső (képernyőn lévő) gombok feliratának színe: világos, enyhén meleg szürke. */
private val ButtonTextColor = Color(0xFFE6DDD3)

/**
 * Egységes, domború (gradienses) gomb az egész apphoz: kicsit világosabb a háttérnél,
 * felül fényes, alul sötétebb, nyomáskor besüllyed.
 */
@Composable
fun WButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(10.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val top = if (pressed) Color(0xFF1B232F) else Color(0xFF34425A)
    val bottom = if (pressed) Color(0xFF2B374A) else Color(0xFF182029)
    val rimTop = if (pressed) Color(0xFF222C3A) else Color(0xFF6182A6)
    val rimBottom = if (pressed) Color(0xFF4F6A8A) else Color(0xFF222C3A)
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.dp, Brush.verticalGradient(listOf(rimTop, rimBottom)), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides ButtonTextColor) {
            content()
        }
    }
}

/** Az egységes gombok felirata: középre igazított, legfeljebb két soros. */
@Composable
fun WLabel(text: String) {
    Text(
        text,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
}

/**
 * Egyvonalas szövegmező besüllyesztett, térhatású kinézettel: sötétebb a háttérnél,
 * felül belső árnyék, a keret alul világosabb. Fix 56 dp magas, így a gombokkal egy vonalba illik.
 */
@Composable
fun WTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF05080C), Color(0xFF0E151D))))
            .drawBehind {
                // belső árnyék a felső élnél
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0x99000000), Color(0x00000000)),
                        startY = 0f,
                        endY = 10.dp.toPx()
                    ),
                    size = Size(size.width, 10.dp.toPx())
                )
            }
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0xFF2B3748), Color(0xFF51677F))),
                shape
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Color(0xFFE6EDF3), fontSize = 16.sp),
            cursorBrush = SolidColor(Color(0xFF64B5F6)),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = Color(0xFF7D8B9B),
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                inner()
            }
        )
    }
}

/**
 * Pakli valaszto: a WButton-hoz hasonlo domboruval (felul vilagos, alul sotet gradiens, vilagos-sotet perem,
 * nyomaskor besullyed), de a pakli barsony szineivel.
 */
@Composable
fun DeckPicker(
    decks: List<Deck>,
    activeDeckId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 150.dp
) {
    val t = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val activeName = decks.firstOrNull { it.id == activeDeckId }?.name.orEmpty()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(10.dp)
    val top = if (pressed) Color(0xFF2E1313) else Color(0xFF6E3736)
    val bottom = if (pressed) Color(0xFF4A2423) else Color(0xFF2E1313)
    val rimTop = if (pressed) Color(0xFF2A1414) else Color(0xFFB98B80)
    val rimBottom = if (pressed) Color(0xFF8A5E55) else Color(0xFF2A1414)
    val ink = Color(0xFFEDE6DA)
    val inkSoft = Color(0xFFD2BBB0)
    Box(modifier.widthIn(max = maxWidth)) {
        Row(
            Modifier
                .clip(shape)
                .background(Brush.verticalGradient(listOf(top, bottom)))
                .border(1.dp, Brush.verticalGradient(listOf(rimTop, rimBottom)), shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { expanded = true }
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f, fill = false)) {
                Text(
                    t.navDeck,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 11.sp,
                    color = inkSoft,
                    maxLines = 1
                )
                Text(
                    activeName.ifBlank { "-" },
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("\u25BE", color = inkSoft, fontSize = 22.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF351818))
        ) {
            decks.forEach { deck ->
                val active = deck.id == activeDeckId
                DropdownMenuItem(
                    text = {
                        Text(
                            deck.name,
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) Color(0xFFFFC8B4) else ink
                        )
                    },
                    onClick = {
                        onSelect(deck.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun WordListScreen(
    words: List<WordItem>,
    loading: Boolean,
    decks: List<Deck>,
    activeDeckId: Long?,
    onSelectDeck: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit,
    onAddManual: (String) -> Unit,
    onGenerate: () -> Unit
) {
    val t = LocalStrings.current
    var manualText by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // bal felső sarok: pakliválasztó, alatta kezdődik a szöveg
        DeckPicker(
            decks = decks,
            activeDeckId = activeDeckId,
            onSelect = onSelectDeck,
            maxWidth = 200.dp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            t.wordsHint,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WTextField(
                value = manualText,
                onValueChange = { manualText = it },
                placeholder = t.addWordManually,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            WButton(
                onClick = {
                    onAddManual(manualText)
                    manualText = ""
                },
                modifier = Modifier.size(56.dp),
                contentPadding = PaddingValues(0.dp)
            ) { Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(words, key = { it.id }) { w ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(w.original, style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { onDelete(w.id) }) {
                        Text("✕")
                    }
                }
                Divider()
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WButton(
                onClick = onClear,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) { WLabel(t.clearList) }
            WButton(
                onClick = onGenerate,
                enabled = words.isNotEmpty() && !loading,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    WLabel(t.generateCards(words.size))
                }
            }
        }
    }
}

@Composable
fun FlashcardScreen(
    cards: List<Flashcard>,
    decks: List<Deck>,
    activeDeckId: Long?,
    onSelectDeck: (Long) -> Unit,
    allKnownHidden: Boolean,
    cardFront: CardFront,
    showStress: Boolean,
    onDelete: (Long) -> Unit,
    onToggleKnown: (Flashcard) -> Unit,
    onEdit: (Flashcard) -> Unit,
    isShuffled: Boolean,
    onShuffle: () -> Unit,
    onResetOrder: () -> Unit
) {
    val t = LocalStrings.current

    if (cards.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            DeckPicker(
                decks = decks,
                activeDeckId = activeDeckId,
                onSelect = onSelectDeck,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                maxWidth = 200.dp
            )
            Text(
                if (allKnownHidden) t.allKnownHidden else t.noCardsYet,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    val scope = rememberCoroutineScope()
    var index by remember { mutableStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var widthPx by remember { mutableStateOf(0f) }
    var animating by remember { mutableStateOf(false) }
    val dragX = remember { Animatable(0f) }

    // animációk állapota
    val delAnim = remember { Animatable(0f) }
    var deletingId by remember { mutableStateOf<Long?>(null) }
    val knownFlash = remember { Animatable(0f) }
    val checkScale = remember { Animatable(0f) }
    val checkAlpha = remember { Animatable(0f) }
    val shuffleAnim = remember { Animatable(0f) }

    // pakliváltáskor induljon az elejéről
    LaunchedEffect(activeDeckId) {
        index = 0
        flipped = false
    }

    val safeIndex = index.coerceIn(0, cards.size - 1)
    val card = cards[safeIndex]
    val dictDisplay = if (showStress && card.stressedForm.isNotBlank()) card.stressedForm else card.dictionaryForm

    val deleting = deletingId == card.id
    val delProgress = if (deleting) delAnim.value else 0f
    val shuf = shuffleAnim.value
    val frontScale = 1f - 0.3f * delProgress

    // dir = +1: következő kártya (a mostani balra kicsúszik, az új jobbról jön be)
    // dir = -1: előző kártya (a mostani jobbra csúszik ki, az új balról jön be)
    fun go(dir: Int) {
        if (animating || deleting) return
        if (cards.size < 2 || widthPx <= 0f) {
            scope.launch { dragX.animateTo(0f, tween(150)) }
            return
        }
        scope.launch {
            animating = true
            dragX.animateTo(-dir * widthPx, tween(180))
            flipped = false
            index = (safeIndex + dir + cards.size) % cards.size
            dragX.snapTo(dir * widthPx)
            dragX.animateTo(0f, tween(220))
            animating = false
        }
    }

    fun playShuffle() {
        scope.launch {
            shuffleAnim.snapTo(0f)
            shuffleAnim.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
    }

    fun playKnown() {
        scope.launch {
            knownFlash.snapTo(1f)
            knownFlash.animateTo(0f, tween(500))
        }
        scope.launch {
            checkAlpha.snapTo(1f)
            checkScale.snapTo(0.2f)
            checkScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
            checkAlpha.animateTo(0f, tween(250))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth()) {
            WButton(
                onClick = {
                    if (isShuffled) onResetOrder() else onShuffle()
                    index = 0
                    flipped = false
                    playShuffle()
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                WLabel(if (isShuffled) t.originalOrder else t.shuffle)
            }
            Text(
                "${safeIndex + 1} / ${cards.size}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.Center)
            )
            DeckPicker(
                decks = decks,
                activeDeckId = activeDeckId,
                onSelect = onSelectDeck,
                modifier = Modifier.align(Alignment.CenterStart),
                maxWidth = 120.dp
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { widthPx = it.width.toFloat() }
                .pointerInput(safeIndex, cards.size, deleting) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val threshold = widthPx * 0.25f
                            when {
                                animating || deleting -> {}
                                dragX.value < -threshold -> go(1)
                                dragX.value > threshold -> go(-1)
                                else -> scope.launch { dragX.animateTo(0f, tween(150)) }
                            }
                        },
                        onDragCancel = {
                            if (!animating && !deleting) scope.launch { dragX.animateTo(0f, tween(150)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (!animating && !deleting) {
                                change.consume()
                                scope.launch { dragX.snapTo(dragX.value + dragAmount) }
                            }
                        }
                    )
                }
        ) {
            val frontColor = Color(0xFF16263A)
            val backColor = Color(0xFF1F3752)
            val edgeBorder = BorderStroke(1.dp, Color(0xFF2A3B50))

            // az aktuális kártya
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
                    .graphicsLayer {
                        translationX = dragX.value
                        scaleX = frontScale
                        scaleY = frontScale
                        alpha = (1f - delProgress) * (1f - 0.65f * sin(PI.toFloat() * shuf))
                        rotationY = 360f * shuf
                        rotationZ = 5f * sin(PI.toFloat() * 5f * shuf) * (1f - shuf)
                        cameraDistance = 12f * density
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { flipped = !flipped },
                colors = CardDefaults.cardColors(
                    containerColor = if (flipped) backColor else frontColor,
                    contentColor = Color(0xFFEAF2FB)
                ),
                border = edgeBorder
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // zöld felvillanás "tudom" jelölésnél
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = knownFlash.value }
                            .background(Color(0x4D66BB6A))
                    )
                    Text(
                        if ((cardFront == CardFront.TRANSLATION) != flipped) card.translation else dictDisplay,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    // rugózó zöld pipa a kártya jobb alsó részén
                    Text(
                        "✓",
                        fontSize = 72.sp,
                        color = Color(0xFF81C784),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .graphicsLayer {
                                scaleX = checkScale.value
                                scaleY = checkScale.value
                                alpha = checkAlpha.value
                            }
                    )
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text("✎", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) { WLabel(t.delete) }
            WButton(
                onClick = {
                    val becomingKnown = !card.known
                    onToggleKnown(card)
                    if (becomingKnown) playKnown()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) { WLabel(if (card.known) t.known else t.markKnown) }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WButton(
                onClick = { go(-1) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) { WLabel(t.previous) }
            WButton(
                onClick = { go(1) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) { WLabel(t.next) }
        }
    }

    if (showEditDialog) {
        var editDict by remember(card.id) { mutableStateOf(card.dictionaryForm) }
        var editTrans by remember(card.id) { mutableStateOf(card.translation) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(t.editCard) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editDict,
                        onValueChange = { editDict = it },
                        label = { Text(t.dictionaryFormLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editTrans,
                        onValueChange = { editTrans = it },
                        label = { Text(t.translationLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editDict.isNotBlank() && editTrans.isNotBlank(),
                    onClick = {
                        showEditDialog = false
                        val newDict = editDict.trim()
                        onEdit(
                            card.copy(
                                dictionaryForm = newDict,
                                translation = editTrans.trim(),
                                stressedForm = if (newDict == card.dictionaryForm) card.stressedForm else ""
                            )
                        )
                    }
                ) { Text(t.save) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text(t.cancel) }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(t.confirmDeleteCard) },
            text = { Text(card.dictionaryForm) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    val idToDelete = card.id
                    deletingId = idToDelete
                    scope.launch {
                        // a kártya összezsugorodik és eltűnik
                        delAnim.snapTo(0f)
                        delAnim.animateTo(1f, tween(220))
                        onDelete(idToDelete)
                        flipped = false
                    }
                }) { Text(t.yesDelete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(t.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    selectedText: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AiSettings,
    onSave: (AiSettings) -> Unit,
    onResetButtonPos: () -> Unit
) {
    val t = LocalStrings.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var showKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testOk by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var provider by remember(settings) { mutableStateOf(settings.provider) }
    var configs by remember(settings) {
        mutableStateOf(
            AiProvider.entries.associateWith { p ->
                settings.providerConfigs[p] ?: ProviderConfig(
                    apiKey = if (p == settings.provider) settings.apiKey else "",
                    model = if (p == settings.provider) settings.model else defaultModelFor(p),
                    baseUrl = if (p == settings.provider) settings.baseUrl else ""
                )
            }
        )
    }
    val cfg = configs.getValue(provider)
    val apiKey = cfg.apiKey
    val model = cfg.model
    val baseUrl = cfg.baseUrl
    var sourceLanguage by remember(settings) { mutableStateOf(settings.sourceLanguage) }
    var targetLanguage by remember(settings) { mutableStateOf(settings.targetLanguage) }
    var uiLanguage by remember(settings) { mutableStateOf(settings.uiLanguage) }
    var cardFront by remember(settings) { mutableStateOf(settings.cardFront) }
    var showKnown by remember(settings) { mutableStateOf(settings.showKnown) }
    var showStress by remember(settings) { mutableStateOf(settings.showStress) }
    var exportStress by remember(settings) { mutableStateOf(settings.exportStress) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(t.languagesTitle, style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFA726))
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = t.bookLanguageLabel,
            selectedText = t.langName(AppLang.valueOf(sourceLanguage.name)),
            options = com.rubookscanner.app.data.SourceLanguage.entries,
            optionLabel = { t.langName(AppLang.valueOf(it.name)) },
            onSelect = { sourceLanguage = it }
        )
        Spacer(Modifier.height(12.dp))
        DropdownField(
            label = t.translationLanguageLabel,
            selectedText = t.langName(targetLanguage),
            options = AppLang.entries,
            optionLabel = { t.langName(it) },
            onSelect = { targetLanguage = it }
        )
        Spacer(Modifier.height(12.dp))
        DropdownField(
            label = t.appLanguageLabel,
            selectedText = uiLanguage.label,
            options = AppLang.entries,
            optionLabel = { it.label },
            onSelect = { uiLanguage = it }
        )
        Spacer(Modifier.height(20.dp))

        Text(t.customizeTitle, style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFA726))
        Spacer(Modifier.height(8.dp))
        WButton(
            onClick = onResetButtonPos,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) { WLabel(t.resetButtonPos) }
        Spacer(Modifier.height(12.dp))
        DropdownField(
            label = t.cardFrontLabel,
            selectedText = if (cardFront == CardFront.TRANSLATION) t.translationLanguageLabel else t.bookLanguageLabel,
            options = CardFront.entries,
            optionLabel = { if (it == CardFront.TRANSLATION) t.translationLanguageLabel else t.bookLanguageLabel },
            onSelect = { cardFront = it }
        )
        Spacer(Modifier.height(12.dp))
        DropdownField(
            label = t.showKnownLabel,
            selectedText = if (showKnown) t.optYes else t.optNo,
            options = listOf(true, false),
            optionLabel = { if (it) t.optYes else t.optNo },
            onSelect = { showKnown = it }
        )
        if (sourceLanguage == com.rubookscanner.app.data.SourceLanguage.RUSSIAN) {
            Spacer(Modifier.height(12.dp))
            DropdownField(
                label = t.showStressLabel,
                selectedText = if (showStress) t.optYes else t.optNo,
                options = listOf(true, false),
                optionLabel = { if (it) t.optYes else t.optNo },
                onSelect = { showStress = it }
            )
            Spacer(Modifier.height(12.dp))
            DropdownField(
                label = t.exportStressLabel,
                selectedText = if (exportStress) t.optYes else t.optNo,
                options = listOf(true, false),
                optionLabel = { if (it) t.optYes else t.optNo },
                onSelect = { exportStress = it }
            )
        }
        Spacer(Modifier.height(20.dp))

        Text(t.aiProviderTitle, style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFA726))
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = t.providerLabel,
            selectedText = provider.name,
            options = AiProvider.entries,
            optionLabel = { it.name },
            onSelect = { provider = it }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { configs = configs + (provider to cfg.copy(apiKey = it)) },
            label = { Text(t.apiKeyLabel) },
            singleLine = true,
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Text(if (showKey) "🙈" else "👁")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { configs = configs + (provider to cfg.copy(model = it)) },
            label = { Text(t.modelLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { configs = configs + (provider to cfg.copy(baseUrl = it)) },
            label = { Text(t.baseUrlLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        WButton(
            onClick = {
                testing = true
                testResult = null
                scope.launch {
                    try {
                        val probe = settings.copy(
                            provider = provider,
                            apiKey = apiKey.trim(),
                            model = model,
                            baseUrl = baseUrl
                        )
                        withContext(Dispatchers.IO) { AiClient(probe).testConnection() }
                        testOk = true
                        testResult = t.keyOk
                    } catch (e: Exception) {
                        testOk = false
                        testResult = t.errorPrefix(e.message?.take(200))
                    } finally {
                        testing = false
                    }
                }
            },
            enabled = apiKey.isNotBlank() && !testing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (testing) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                WLabel(t.testKey)
            }
        }
        testResult?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = if (testOk) Color(0xFF81C784) else MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(20.dp))
        WButton(
            onClick = {
                onSave(
                    AiSettings(
                        provider = provider,
                        apiKey = apiKey,
                        model = model,
                        baseUrl = baseUrl,
                        providerConfigs = configs,
                        sourceLanguage = sourceLanguage,
                        uiLanguage = uiLanguage,
                        targetLanguage = targetLanguage,
                        buttonX = settings.buttonX,
                        buttonY = settings.buttonY,
                        showKnown = showKnown,
                        cardFront = cardFront,
                        showStress = showStress,
                        exportStress = exportStress
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            WLabel(t.save)
        }
        Spacer(Modifier.height(20.dp))
        Text(t.settingsTip, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        val keyUrl = when (provider) {
            AiProvider.ANTHROPIC -> "https://console.anthropic.com/settings/keys"
            AiProvider.OPENAI -> "https://platform.openai.com/api-keys"
            AiProvider.GEMINI -> "https://aistudio.google.com/apikey"
        }
        Text(
            keyUrl,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64B5F6),
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { uriHandler.openUri(keyUrl) }
        )
    }
}
