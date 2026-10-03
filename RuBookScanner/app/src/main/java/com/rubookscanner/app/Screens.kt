package com.rubookscanner.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rubookscanner.app.data.AiProvider
import com.rubookscanner.app.data.AiSettings
import com.rubookscanner.app.data.AppLang
import com.rubookscanner.app.data.Flashcard
import com.rubookscanner.app.data.WordItem
import com.rubookscanner.app.data.defaultModelFor
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun WordListScreen(
    words: List<WordItem>,
    loading: Boolean,
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
        Text(t.wordsHint, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = manualText,
                onValueChange = { manualText = it },
                label = { Text(t.addWordManually) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                onAddManual(manualText)
                manualText = ""
            }) { Text("+") }
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
        Row {
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                Text(t.clearList)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onGenerate,
                enabled = words.isNotEmpty() && !loading,
                modifier = Modifier.weight(1f)
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(t.generateCards(words.size))
                }
            }
        }
    }
}

@Composable
fun FlashcardScreen(
    cards: List<Flashcard>,
    onDelete: (Long) -> Unit,
    onToggleKnown: (Flashcard) -> Unit,
    onEdit: (Flashcard) -> Unit,
    isShuffled: Boolean,
    onShuffle: () -> Unit,
    onResetOrder: () -> Unit
) {
    val t = LocalStrings.current

    if (cards.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                t.noCardsYet,
                modifier = Modifier.padding(24.dp),
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
    var slideIn by remember { mutableStateOf(false) }
    val dragX = remember { Animatable(0f) }

    // animációk állapota
    val delAnim = remember { Animatable(0f) }
    var deletingId by remember { mutableStateOf<Long?>(null) }
    val knownFlash = remember { Animatable(0f) }
    val checkScale = remember { Animatable(0f) }
    val checkAlpha = remember { Animatable(0f) }
    val shuffleAnim = remember { Animatable(0f) }

    val safeIndex = index.coerceIn(0, cards.size - 1)
    val card = cards[safeIndex]

    val deleting = deletingId == card.id
    val delProgress = if (deleting) delAnim.value else 0f
    val dragProg = if (widthPx > 0f && !slideIn) (abs(dragX.value) / widthPx).coerceIn(0f, 1f) else 0f
    // 0..1: mennyire "emelkedik előre" a mögöttes kártya
    val prog = maxOf(dragProg, delProgress)
    val shuf = shuffleAnim.value
    val frontScale = 1f - 0.3f * delProgress

    // dir = +1: következő kártya (a mostani balra kicsúszik, a mögötte lévő előre jön)
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
            if (dir > 0) {
                index = (safeIndex + 1) % cards.size
                dragX.snapTo(0f)
            } else {
                slideIn = true
                index = (safeIndex - 1 + cards.size) % cards.size
                dragX.snapTo(-widthPx)
                dragX.animateTo(0f, tween(220))
                slideIn = false
            }
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
            OutlinedButton(
                onClick = {
                    if (isShuffled) onResetOrder() else onShuffle()
                    index = 0
                    flipped = false
                    playShuffle()
                },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(if (isShuffled) t.originalOrder else t.shuffle)
            }
            Text(
                "${safeIndex + 1} / ${cards.size}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.Center)
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
            val edgeColor = Color(0xFF0F1A27)
            val edgeBorder = BorderStroke(1.dp, Color(0xFF2A3B50))

            // a pakli mögöttes kártyái (a legtávolabbi van legalul)
            for (depth in minOf(2, cards.size - 1) downTo 1) {
                val eff = depth - prog
                val backCard = cards[(safeIndex + depth) % cards.size]
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp, bottom = 32.dp)
                        .graphicsLayer {
                            val s = 1f - 0.05f * eff
                            scaleX = s
                            scaleY = s
                            translationY = 22.dp.toPx() * eff
                            rotationZ = -5f * sin(PI.toFloat() * 5f * shuf) * (1f - shuf)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = lerpColor(frontColor, edgeColor, (eff * 0.45f).coerceIn(0f, 1f)),
                        contentColor = Color(0xFFEAF2FB)
                    ),
                    border = edgeBorder
                ) {
                    if (depth == 1) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                backCard.translation,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.graphicsLayer { alpha = prog }
                            )
                        }
                    }
                }
            }

            // az első (aktuális) kártya
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp, bottom = 32.dp)
                    .graphicsLayer {
                        translationX = dragX.value
                        scaleX = frontScale
                        scaleY = frontScale
                        alpha = (1f - delProgress) * (1f - 0.65f * sin(PI.toFloat() * shuf))
                        rotationY = 360f * shuf
                        rotationZ = 5f * sin(PI.toFloat() * 5f * shuf) * (1f - shuf)
                        cameraDistance = 12f * density
                    },
                onClick = { flipped = !flipped },
                colors = CardDefaults.cardColors(
                    containerColor = frontColor,
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
                        if (!flipped) card.translation else card.dictionaryForm,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    // rugózó zöld pipa
                    Text(
                        "✓",
                        fontSize = 110.sp,
                        color = Color(0xFF81C784),
                        modifier = Modifier.graphicsLayer {
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { showDeleteDialog = true }) { Text(t.delete) }
            OutlinedButton(onClick = {
                val becomingKnown = !card.known
                onToggleKnown(card)
                if (becomingKnown) playKnown()
            }) {
                Text(if (card.known) t.known else t.markKnown)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = { go(-1) }) { Text(t.previous) }
            Button(onClick = { go(1) }) { Text(t.next) }
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
                        onEdit(card.copy(dictionaryForm = editDict.trim(), translation = editTrans.trim()))
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
                        // a kártya összezsugorodik és eltűnik, a mögötte lévő előre jön
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
    onSave: (AiSettings) -> Unit
) {
    val t = LocalStrings.current
    val uriHandler = LocalUriHandler.current
    var provider by remember(settings) { mutableStateOf(settings.provider) }
    var apiKey by remember(settings) { mutableStateOf(settings.apiKey) }
    var model by remember(settings) { mutableStateOf(settings.model) }
    var baseUrl by remember(settings) { mutableStateOf(settings.baseUrl) }
    var sourceLanguage by remember(settings) { mutableStateOf(settings.sourceLanguage) }
    var targetLanguage by remember(settings) { mutableStateOf(settings.targetLanguage) }
    var uiLanguage by remember(settings) { mutableStateOf(settings.uiLanguage) }

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
            selectedText = sourceLanguage.label,
            options = com.rubookscanner.app.data.SourceLanguage.entries,
            optionLabel = { it.label },
            onSelect = { sourceLanguage = it }
        )
        Spacer(Modifier.height(12.dp))
        DropdownField(
            label = t.translationLanguageLabel,
            selectedText = targetLanguage.label,
            options = AppLang.entries,
            optionLabel = { it.label },
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

        Text(t.aiProviderTitle, style = MaterialTheme.typography.titleMedium, color = Color(0xFFFFA726))
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = t.providerLabel,
            selectedText = provider.name,
            options = AiProvider.entries,
            optionLabel = { it.name },
            onSelect = {
                provider = it
                model = defaultModelFor(it)
            }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(t.apiKeyLabel) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text(t.modelLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text(t.baseUrlLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                onSave(
                    AiSettings(
                        provider = provider,
                        apiKey = apiKey,
                        model = model,
                        baseUrl = baseUrl,
                        sourceLanguage = sourceLanguage,
                        uiLanguage = uiLanguage,
                        targetLanguage = targetLanguage
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(t.save)
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
