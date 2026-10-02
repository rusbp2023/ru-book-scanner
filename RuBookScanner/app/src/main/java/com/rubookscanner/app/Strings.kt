package com.rubookscanner.app

import androidx.compose.runtime.staticCompositionLocalOf
import com.rubookscanner.app.data.AppLang

interface Strings {
    val settingsDesc: String
    val noActiveDeck: String
    val enterApiKeyFirst: String
    fun cardsAdded(n: Int): String
    fun errorPrefix(msg: String?): String
    val unknownError: String
    val cameraError: String

    val wordsHint: String
    val addWordManually: String
    val clearList: String
    fun generateCards(n: Int): String
    fun wordAddedToList(word: String): String

    val noCardsYet: String
    fun originalLabel(o: String): String
    val tapBack: String
    val tapForTranslation: String
    val delete: String
    val confirmDeleteCard: String
    fun confirmDeleteDeck(name: String): String
    val yesDelete: String
    val cancel: String
    val known: String
    val markKnown: String
    val editCard: String
    val dictionaryFormLabel: String
    val translationLabel: String
    val previous: String
    val next: String

    val decksTitle: String
    val newDeckName: String
    val create: String
    val downloadAll: String
    val allDecksFileName: String
    fun cardCount(n: Int): String
    val emptyDeck: String

    val cameraPermissionNeeded: String
    val allowCamera: String
    fun collectedWords(n: Int): String
    fun translateN(n: Int): String

    val languagesTitle: String
    val bookLanguageLabel: String
    val translationLanguageLabel: String
    val appLanguageLabel: String
    val langRussian: String
    val langEnglish: String
    val aiProviderTitle: String
    val providerLabel: String
    val apiKeyLabel: String
    val modelLabel: String
    val baseUrlLabel: String
    val save: String
    val settingsTip: String
}

object StringsHu : Strings {
    override val settingsDesc = "Beállítások"
    override val noActiveDeck = "Nincs aktív pakli — hozz létre egyet a Paklik fülön!"
    override val enterApiKeyFirst = "Előbb add meg az API kulcsot a Beállításoknál!"
    override fun cardsAdded(n: Int) = "$n kártya hozzáadva"
    override fun errorPrefix(msg: String?) = "Hiba: $msg"
    override val unknownError = "ismeretlen hiba"
    override val cameraError = "kamera hiba"

    override val wordsHint =
        "Jelölj ki egy szót a Chrome-ban a weboldalon, majd Megosztás → ez az app. " +
            "A szó itt jelenik meg a listában."
    override val addWordManually = "Szó kézzel hozzáadása"
    override val clearList = "Lista ürítése"
    override fun generateCards(n: Int) = "Kártyák generálása ($n)"
    override fun wordAddedToList(word: String) = "Szólistához adva: $word"

    override val noCardsYet =
        "Még nincs szókártyád. Adj hozzá szavakat a Word fülön, majd generáld le őket."
    override fun originalLabel(o: String) = "(eredeti: $o)"
    override val tapBack = "Koppints: vissza"
    override val tapForTranslation = "Koppints a fordításért"
    override val delete = "Törlés"
    override val confirmDeleteCard = "Biztosan törlöd ezt a kártyát?"
    override fun confirmDeleteDeck(name: String) = "Biztosan törlöd a(z) \"$name\" paklit a benne lévő összes kártyával?"
    override val yesDelete = "Igen, törlöm"
    override val cancel = "Mégse"
    override val known = "Tudom ✓"
    override val markKnown = "Megjelöl: tudom"
    override val editCard = "Kártya szerkesztése"
    override val dictionaryFormLabel = "Szótári alak"
    override val translationLabel = "Fordítás"
    override val previous = "◀ Előző"
    override val next = "Következő ▶"

    override val decksTitle = "Paklik"
    override val newDeckName = "Új pakli neve"
    override val create = "Létrehozás"
    override val downloadAll = "Összes pakli letöltése"
    override val allDecksFileName = "osszes_pakli.txt"
    override fun cardCount(n: Int) = "$n kártya"
    override val emptyDeck = "(nincs még kártya ebben a pakliban)"

    override val cameraPermissionNeeded = "A szófelismeréshez szükség van a kamera engedélyre."
    override val allowCamera = "Kamera engedélyezése"
    override fun collectedWords(n: Int) = "Összegyűjtött szavak ($n):"
    override fun translateN(n: Int) = "Lefordítás ($n)"

    override val languagesTitle = "Nyelvek"
    override val bookLanguageLabel = "Könyv nyelve"
    override val translationLanguageLabel = "Fordítás nyelve"
    override val appLanguageLabel = "App nyelve"
    override val langRussian = "Orosz"
    override val langEnglish = "Angol"
    override val aiProviderTitle = "AI szolgáltató"
    override val providerLabel = "Szolgáltató"
    override val apiKeyLabel = "API kulcs"
    override val modelLabel = "Modell neve"
    override val baseUrlLabel = "Egyedi API végpont (opcionális)"
    override val save = "Mentés"
    override val settingsTip =
        "Tipp: az API kulcsot a szolgáltató oldalán kapod (pl. console.anthropic.com, " +
            "platform.openai.com, aistudio.google.com). A kulcs csak a telefonodon tárolódik, " +
            "az AI-nak közvetlenül a telefon küldi el a kéréseket."
}

object StringsEn : Strings {
    override val settingsDesc = "Settings"
    override val noActiveDeck = "No active deck — create one on the Deck tab!"
    override val enterApiKeyFirst = "Enter your API key in Settings first!"
    override fun cardsAdded(n: Int) = "$n card(s) added"
    override fun errorPrefix(msg: String?) = "Error: $msg"
    override val unknownError = "unknown error"
    override val cameraError = "camera error"

    override val wordsHint =
        "Select a word on a web page in Chrome, then Share → this app. " +
            "The word will appear in this list."
    override val addWordManually = "Add a word manually"
    override val clearList = "Clear list"
    override fun generateCards(n: Int) = "Generate cards ($n)"
    override fun wordAddedToList(word: String) = "Added to word list: $word"

    override val noCardsYet =
        "You have no flashcards yet. Add words on the Word tab, then generate them."
    override fun originalLabel(o: String) = "(original: $o)"
    override val tapBack = "Tap: back"
    override val tapForTranslation = "Tap for the translation"
    override val delete = "Delete"
    override val confirmDeleteCard = "Are you sure you want to delete this card?"
    override fun confirmDeleteDeck(name: String) = "Are you sure you want to delete the deck \"$name\" and all its cards?"
    override val yesDelete = "Yes, delete"
    override val cancel = "Cancel"
    override val known = "Known ✓"
    override val markKnown = "Mark as known"
    override val editCard = "Edit card"
    override val dictionaryFormLabel = "Dictionary form"
    override val translationLabel = "Translation"
    override val previous = "◀ Previous"
    override val next = "Next ▶"

    override val decksTitle = "Decks"
    override val newDeckName = "New deck name"
    override val create = "Create"
    override val downloadAll = "Download all decks"
    override val allDecksFileName = "all_decks.txt"
    override fun cardCount(n: Int) = "$n cards"
    override val emptyDeck = "(no cards in this deck yet)"

    override val cameraPermissionNeeded = "Camera permission is required for word recognition."
    override val allowCamera = "Allow camera"
    override fun collectedWords(n: Int) = "Collected words ($n):"
    override fun translateN(n: Int) = "Translate ($n)"

    override val languagesTitle = "Languages"
    override val bookLanguageLabel = "Book language"
    override val translationLanguageLabel = "Translation language"
    override val appLanguageLabel = "App language"
    override val langRussian = "Russian"
    override val langEnglish = "English"
    override val aiProviderTitle = "AI provider"
    override val providerLabel = "Provider"
    override val apiKeyLabel = "API key"
    override val modelLabel = "Model name"
    override val baseUrlLabel = "Custom API endpoint (optional)"
    override val save = "Save"
    override val settingsTip =
        "Tip: you get the API key on the provider's website (e.g. console.anthropic.com, " +
            "platform.openai.com, aistudio.google.com). The key is stored only on your phone, " +
            "and the phone sends requests to the AI directly."
}

val LocalStrings = staticCompositionLocalOf<Strings> { StringsHu }

fun stringsFor(lang: AppLang): Strings =
    if (lang == AppLang.ENGLISH) StringsEn else StringsHu
