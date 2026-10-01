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

enum class AppScreen { SCAN, WORDS, CARDS, DECKS, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(applicationContext)
        setContent {
            MaterialTheme {
                AppRoot(store)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(store: Store) {
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
            NavigationBar(containerColor = Color(0xFF121212)) {
                val navColors = NavigationBarItemDefaults.colors(
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color(0xFF9E9E9E),
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color(0xFF9E9E9E),
                    indicatorColor = Color(0xFF2C2C2E)
                )
                NavigationBarItem(
                    selected = screen == AppScreen.SCAN,
                    onClick = { screen = AppScreen.SCAN },
                    icon = {},
                    label = { Text("Scan", fontWeight = FontWeight.Bold) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = screen == AppScreen.WORDS,
                    onClick = { screen = AppScreen.WORDS },
                    icon = {},
                    label = { Text("Word (${words.size})", fontWeight = FontWeight.Bold) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = screen == AppScreen.CARDS,
                    onClick = { screen = AppScreen.CARDS },
                    icon = {},
                    label = { Text("Card (${cardsInActiveDeck.size})", fontWeight = FontWeight.Bold) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = screen == AppScreen.DECKS,
                    onClick = { screen = AppScreen.DECKS },
                    icon = {},
                    label = { Text("Deck", fontWeight = FontWeight.Bold) },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = screen == AppScreen.SETTINGS,
                    onClick = { screen = AppScreen.SETTINGS },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Beállítások") },
                    label = {},
                    colors = navColors
                )
            }
        }{ padding ->
        Box(Modifier.padding(padding)) {
            when (screen) {
                AppScreen.SCAN -> ScannerScreen(
                    settings = settings,
                    pendingCrops = pendingCrops,
                    onFlashcardsAccepted = { newCards ->
                        scope.launch {
                            val deckId = activeDeckId
                            if (deckId == null) {
                                snackbarHostState.showSnackbar("Nincs aktív pakli — hozz létre egyet a Paklik fülön!")
                            } else {
                                store.addFlashcards(newCards, deckId)
                                snackbarHostState.showSnackbar("${newCards.size} kártya hozzáadva")
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
                                snackbarHostState.showSnackbar("Előbb add meg az API kulcsot a Beállításoknál!")
                            }
                        } else if (deckId == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Nincs aktív pakli — hozz létre egyet a Paklik fülön!")
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
                                    snackbarHostState.showSnackbar("Hiba: ${e.message}")
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
