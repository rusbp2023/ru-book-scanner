package com.rubookscanner.app

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubookscanner.app.data.AiClient
import com.rubookscanner.app.data.AiSettings
import com.rubookscanner.app.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AppDarkColors = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF0B1A2A),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFD6E8FF),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF0B1F0D),
    tertiary = Color(0xFFBCAAA4),
    background = Color(0xFF0D1117),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF111A24),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1B2838),
    onSurfaceVariant = Color(0xFFB8C7D9),
    outline = Color(0xFF3F5F80),
    outlineVariant = Color(0xFF2A3B50),
    error = Color(0xFFEF9A9A)
)

enum class AppScreen { SCAN, WORDS, CARDS, DECKS, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#0D1117")
        window.navigationBarColor = android.graphics.Color.parseColor("#1E1E22")
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        val store = Store(applicationContext)
        setContent {
            MaterialTheme(colorScheme = AppDarkColors) {
                AppRoot(store)
            }
        }
    }
}

@Composable
fun AppRoot(store: Store) {
    val settings by store.settingsFlow.collectAsStateWithLifecycle(initialValue = AiSettings())
    CompositionLocalProvider(LocalStrings provides stringsFor(settings.uiLanguage)) {
        AppContent(store)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(store: Store) {
    val t = LocalStrings.current
    var screen by remember { mutableStateOf(AppScreen.SCAN) }
    val words by store.wordsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val cards by store.flashcardsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val decks by store.decksFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeDeckId by store.activeDeckIdFlow.collectAsStateWithLifecycle(initialValue = null)
    val settings by store.settingsFlow.collectAsStateWithLifecycle(initialValue = AiSettings())
    val shuffleOrders by store.shuffleOrdersFlow.collectAsStateWithLifecycle(initialValue = emptyMap())
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingCrops = remember { mutableStateListOf<Bitmap>() }

    LaunchedEffect(Unit) {
        store.ensureDefaultDeck()
    }

        val deckCards = cards.filter { it.deckId == activeDeckId }
    val baseCards = if (settings.showKnown) deckCards else deckCards.filter { !it.known }
    val savedOrder = activeDeckId?.let { shuffleOrders[it] }
    val cardsInActiveDeck = if (savedOrder == null) {
        baseCards
    } else {
        val byId = baseCards.associateBy { it.id }
        val inOrder = savedOrder.toSet()
        savedOrder.mapNotNull { byId[it] } + baseCards.filter { it.id !in inOrder }
    }
    val isShuffled = savedOrder != null
    val allKnownHidden = deckCards.isNotEmpty() && baseCards.isEmpty()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E22))
                    .navigationBarsPadding()
            ) {
            NeonIndicator(screen.ordinal)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NavButton(screen == AppScreen.SCAN, Modifier.weight(1f), { screen = AppScreen.SCAN }) {
                    Text(t.navScan, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                NavButton(screen == AppScreen.WORDS, Modifier.weight(1f), { screen = AppScreen.WORDS }) {
                    Text(t.navWord, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                NavButton(screen == AppScreen.CARDS, Modifier.weight(1f), { screen = AppScreen.CARDS }) {
                    Text(t.navCard, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                NavButton(screen == AppScreen.DECKS, Modifier.weight(1f), { screen = AppScreen.DECKS }) {
                    Text(t.navDeck, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                NavButton(screen == AppScreen.SETTINGS, Modifier.weight(0.7f), { screen = AppScreen.SETTINGS }) {
                    Icon(Icons.Filled.Settings, contentDescription = t.settingsDesc)
                }
            }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (screen) {
                AppScreen.SCAN -> ScannerScreen(
                    settings = settings,
                    onHideInfo = { scope.launch { store.saveSettings(settings.copy(showInfo = false)) } },
                    pendingCrops = pendingCrops,
                    onButtonPosChange = { fx, fy ->
                        scope.launch { store.saveSettings(settings.copy(buttonX = fx, buttonY = fy)) }
                    },
                    decks = decks,
                    activeDeckId = activeDeckId,
                    onSelectDeck = { id -> scope.launch { store.setActiveDeck(id) } },
                    onFlashcardsAccepted = { newCards ->
                        scope.launch {
                            val deckId = activeDeckId
                            if (deckId == null) {
                                snackbarHostState.showSnackbar(t.noActiveDeck)
                            } else {
                                store.addFlashcards(newCards, deckId)
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val msgJob = launch {
                                    snackbarHostState.showSnackbar(t.cardsAdded(newCards.size))
                                }
                                kotlinx.coroutines.delay(1200)
                                msgJob.cancel()
                            }
                        }
                    }
                )

                AppScreen.WORDS -> WordListScreen(
                    showInfoButton = settings.showInfo,
                    onHideInfo = { scope.launch { store.saveSettings(settings.copy(showInfo = false)) } },
                    words = words,
                    loading = loading,
                    decks = decks,
                    activeDeckId = activeDeckId,
                    onSelectDeck = { id -> scope.launch { store.setActiveDeck(id) } },
                    onDelete = { id -> scope.launch { store.removeWord(id) } },
                    onClear = { scope.launch { store.clearWords() } },
                    onAddManual = { text -> scope.launch { store.addWord(text) } },
                    onGenerate = {
                        val deckId = activeDeckId
                        if (settings.apiKey.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(t.enterApiKeyFirst)
                            }
                        } else if (deckId == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar(t.noActiveDeck)
                            }
                        } else {
                            loading = true
                            scope.launch {
                                try {
                                    val originals = words.map { it.original }
                                    val client = AiClient(settings)
                                    val result = withContext(Dispatchers.IO) { client.lookupWords(originals) }
                                    store.addFlashcards(result, deckId)
                                    store.clearWords()
                                    screen = AppScreen.CARDS
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(t.errorPrefix(e.message))
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    }
                )

                AppScreen.CARDS -> FlashcardScreen(
                    cards = cardsInActiveDeck,
                    decks = decks,
                    activeDeckId = activeDeckId,
                    onSelectDeck = { id -> scope.launch { store.setActiveDeck(id) } },
                    onDelete = { id -> scope.launch { store.deleteFlashcard(id) } },
                    onToggleKnown = { c -> scope.launch { store.updateFlashcard(c.copy(known = !c.known)) } },
                    onEdit = { c -> scope.launch { store.updateFlashcard(c) } },
                    allKnownHidden = allKnownHidden,
                    cardFront = settings.cardFront,
                    showStress = settings.showStress,
                    isShuffled = isShuffled,
                    onShuffle = { activeDeckId?.let { id -> scope.launch { store.shuffleDeck(id) } } },
                    onResetOrder = { activeDeckId?.let { id -> scope.launch { store.resetDeckOrder(id) } } }
                )

                AppScreen.DECKS -> DecksScreen(
                    decks = decks,
                    activeDeckId = activeDeckId,
                    allCards = cards,
                    exportStress = settings.exportStress,
                    onSetActive = { id -> scope.launch { store.setActiveDeck(id) } },
                    onOpenDeck = { id ->
                        scope.launch {
                            store.setActiveDeck(id)
                            screen = AppScreen.CARDS
                        }
                    },
                    onCreateDeck = { name -> scope.launch { store.createDeck(name) } },
                    onDeleteDeck = { id -> scope.launch { store.deleteDeck(id) } },
                    onRenameDeck = { id, name -> scope.launch { store.renameDeck(id, name) } },
                    onImportDecks = { parsed, fallbackName ->
                        scope.launch {
                            try {
                                val total = store.importDecks(parsed, fallbackName)
                                snackbarHostState.showSnackbar(t.decksUploaded(parsed.size, total))
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(t.errorPrefix(e.message))
                            }
                        }
                    },
                    onMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                )

                AppScreen.SETTINGS -> SettingsScreen(
                    settings = settings,
                    onResetButtonPos = {
                        scope.launch {
                            store.saveSettings(settings.copy(buttonX = 0.5f, buttonY = 1f))
                            snackbarHostState.showSnackbar(t.settingsSaved)
                        }
                    },
                    onSave = { s ->
                        scope.launch {
                            store.saveSettings(s)
                            // visszajelzés az új (esetleg épp átváltott) app nyelven
                            snackbarHostState.showSnackbar(stringsFor(s.uiLanguage).settingsSaved)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NavButton(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val blue = Color(0xFF64B5F6)
    val shape = RoundedCornerShape(10.dp)
    val top = if (selected) Color(0xFF41648F) else Color(0xFF2F3B4C)
    val bottom = if (selected) Color(0xFF1A2C45) else Color(0xFF171E28)
    val rimTop = if (selected) Color(0xFF8CC8FA) else Color(0xFF5A7391)
    val rimBottom = if (selected) Color(0xFF2B4D75) else Color(0xFF222C3A)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.dp, Brush.verticalGradient(listOf(rimTop, rimBottom)), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides blue) {
            content()
        }
    }
}

/** Thin neon slider above the bottom bar: glows over the active tab and slides to the tapped one. */
@Composable
private fun NeonIndicator(selected: Int) {
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(0f) }
    val weights = listOf(1f, 1f, 1f, 1f, 0.7f) // same weights as the nav buttons
    val padPx = with(density) { 8.dp.toPx() }
    val gapPx = with(density) { 6.dp.toPx() }
    val unit = if (widthPx > 0f) (widthPx - 2 * padPx - 4 * gapPx) / weights.sum() else 0f
    var start = padPx
    for (i in 0 until selected) start += weights[i] * unit + gapPx
    val tabW = weights[selected] * unit
    val x by animateFloatAsState(start, tween(300, easing = FastOutSlowInEasing), label = "neonX")
    val w by animateFloatAsState(tabW, tween(300, easing = FastOutSlowInEasing), label = "neonW")
    val blue = Color(0xFF64B5F6)
    val bright = Color(0xFFB3E0FF)
    Box(
        Modifier
            .fillMaxWidth()
            .height(12.dp)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .drawBehind {
                if (w > 0f) {
                    val lineH = 2.5.dp.toPx()
                    val bottom = size.height
                    val inset = w * 0.2f
                    val lx = x + inset
                    val lw = w - 2 * inset
                    // soft layered glow around the line (no lighter rectangle)
                    listOf(8f to 0.10f, 5f to 0.18f, 2.5f to 0.34f).forEach { (g, a) ->
                        val gp = g.dp.toPx()
                        drawRoundRect(
                            blue.copy(alpha = a),
                            Offset(lx - gp, bottom - lineH - gp),
                            Size(lw + 2 * gp, lineH + 2 * gp),
                            CornerRadius(gp + 2.dp.toPx())
                        )
                    }
                    // the bright neon line
                    drawRoundRect(
                        bright,
                        Offset(lx, bottom - lineH),
                        Size(lw, lineH),
                        CornerRadius(lineH / 2f)
                    )
                }
            }
    )
}
