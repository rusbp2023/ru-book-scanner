package com.rubookscanner.app

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rubookscanner.app.data.AiClient
import com.rubookscanner.app.data.AiSettings
import com.rubookscanner.app.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme

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
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val pendingCrops = remember { mutableStateListOf<Bitmap>() }

    LaunchedEffect(Unit) {
        store.ensureDefaultDeck()
    }

    val cardsInActiveDeck = cards.filter { it.deckId == activeDeckId }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E22))
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                NavButton(screen == AppScreen.SCAN, Modifier.weight(1f), { screen = AppScreen.SCAN }) {
                    Text("Scan", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                NavButton(screen == AppScreen.WORDS, Modifier.weight(1f), { screen = AppScreen.WORDS }) {
                    Text("Word", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                NavButton(screen == AppScreen.CARDS, Modifier.weight(1f), { screen = AppScreen.CARDS }) {
                    Text("Card", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                NavButton(screen == AppScreen.DECKS, Modifier.weight(1f), { screen = AppScreen.DECKS }) {
                    Text("Deck", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                NavButton(screen == AppScreen.SETTINGS, Modifier.weight(0.7f), { screen = AppScreen.SETTINGS }) {
                    Icon(Icons.Filled.Settings, contentDescription = t.settingsDesc)
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (screen) {
                AppScreen.SCAN -> ScannerScreen(
                    settings = settings,
                    pendingCrops = pendingCrops,
                    onFlashcardsAccepted = { newCards ->
                        scope.launch {
                            val deckId = activeDeckId
                            if (deckId == null) {
                                snackbarHostState.showSnackbar(t.noActiveDeck)
                            } else {
                                store.addFlashcards(newCards, deckId)
                                snackbarHostState.showSnackbar(t.cardsAdded(newCards.size))
                            }
                        }
                    }
                )

                AppScreen.WORDS -> WordListScreen(
                    words = words,
                    loading = loading,
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
                    onDelete = { id -> scope.launch { store.deleteFlashcard(id) } },
                    onToggleKnown = { c -> scope.launch { store.updateFlashcard(c.copy(known = !c.known)) } }
                )

                AppScreen.DECKS -> DecksScreen(
                    decks = decks,
                    activeDeckId = activeDeckId,
                    allCards = cards,
                    onSetActive = { id -> scope.launch { store.setActiveDeck(id) } },
                    onCreateDeck = { name -> scope.launch { store.createDeck(name) } },
                    onDeleteDeck = { id -> scope.launch { store.deleteDeck(id) } }
                )

                AppScreen.SETTINGS -> SettingsScreen(
                    settings = settings,
                    onSave = { s -> scope.launch { store.saveSettings(s) } }
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
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) blue else Color(0xFF3F5F80)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = blue,
            containerColor = if (selected) Color(0xFF26364A) else Color.Transparent
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        content()
    }
}
