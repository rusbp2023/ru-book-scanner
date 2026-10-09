package com.rubookscanner.app

import androidx.compose.runtime.staticCompositionLocalOf
import com.rubookscanner.app.data.AppLang

interface Strings {
    val settingsDesc: String
    val navScan: String
    val navWord: String
    val navCard: String
    val navDeck: String
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
    val shuffle: String
    val originalOrder: String

    val newDeckName: String
    val create: String
    val renameDeck: String
    val downloadAll: String
    val uploadDeck: String
    fun decksUploaded(decks: Int, cards: Int): String
    val uploadNothingFound: String
    val allDecksFileName: String
    fun cardCount(n: Int): String
    val emptyDeck: String
    val deckShort: String

    val cameraPermissionNeeded: String
    val allowCamera: String
    fun collectedWords(n: Int): String
    fun translateN(n: Int): String

    val languagesTitle: String
    val bookLanguageLabel: String
    val translationLanguageLabel: String
    val appLanguageLabel: String
    val aiProviderTitle: String
    val providerLabel: String
    val apiKeyLabel: String
    val modelLabel: String
    val baseUrlLabel: String
    val save: String
    val settingsTip: String
    val settingsSaved: String

    val customizeTitle: String
    val handModeLabel: String
    val handRight: String
    val handLeft: String
    val handCenter: String
    val showKnownLabel: String
    val optYes: String
    val optNo: String
    val allKnownHidden: String
    val closeLabel: String
    val helpDownloadTitle: String
    val helpDownloadBody: String
    val helpUploadTitle: String
    val helpUploadBody: String
    val cardFrontLabel: String
    val infoTitle: String
    val infoBody: String
    val testKey: String
    val keyOk: String
    val showStressLabel: String
    val exportStressLabel: String
    val exportOrderLabel: String
    val resetButtonPos: String
    val infoShowLabel: String
    val dontShowAgain: String
    val wordsInfoBody: String

    /** A nyelv neve az app (felulet) nyelven. */
    fun langName(l: AppLang): String
}

object StringsHu : Strings {
    override val settingsDesc = "Beállítások"
    override val navScan = "Scan"
    override val navWord = "Szó"
    override val navCard = "Kártya"
    override val navDeck = "Pakli"
    override val noActiveDeck = "Nincs aktív pakli — hozz létre egyet a Pakli fülön!"
    override val enterApiKeyFirst = "Előbb add meg az API kulcsot a Beállításoknál!"
    override fun cardsAdded(n: Int) = "$n kártya hozzáadva"
    override fun errorPrefix(msg: String?) = "Hiba: $msg"
    override val unknownError = "ismeretlen hiba"
    override val cameraError = "kamera hiba"

    override val wordsHint =
        "Jelölj ki egy szót egy [[weboldalon]] vagy bármelyik [[appban]], majd [[Megosztás]] → ez az app. " +
            "A szó itt jelenik meg a listában."
    override val addWordManually = "Szó kézzel hozzáadása"
    override val clearList = "Lista ürítése"
    override fun generateCards(n: Int) = "Kártyák generálása ($n)"
    override fun wordAddedToList(word: String) = "Szólistához adva: $word"

    override val noCardsYet =
        "Még nincs szókártyád. Adj hozzá szavakat a Szó fülön, majd generáld le őket."
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
    override val shuffle = "Keverés"
    override val originalOrder = "Eredeti sorrend"

    override val newDeckName = "Új pakli neve"
    override val create = "Létrehozás"
    override val renameDeck = "Pakli átnevezése"
    override val downloadAll = "Összes pakli letöltése"
    override val uploadDeck = "Pakli feltöltése"
    override fun decksUploaded(decks: Int, cards: Int) =
        if (decks == 1) "Pakli feltöltve ($cards kártya)" else "$decks pakli feltöltve ($cards kártya)"
    override val uploadNothingFound = "Nem található kártya a fájlban."
    override val allDecksFileName = "osszes_pakli.txt"
    override fun cardCount(n: Int) = "$n kártya"
    override val emptyDeck = "(nincs még kártya ebben a pakliban)"
    override val deckShort = "P"

    override val cameraPermissionNeeded = "A szófelismeréshez szükség van a kamera engedélyre."
    override val allowCamera = "Kamera engedélyezése"
    override fun collectedWords(n: Int) = "Összegyűjtött szavak ($n):"
    override fun translateN(n: Int) = "Lefordítás ($n)"

    override val languagesTitle = "Nyelvek"
    override val bookLanguageLabel = "Forrásnyelv"
    override val translationLanguageLabel = "Fordítás nyelve"
    override val appLanguageLabel = "App nyelve"
    override val aiProviderTitle = "AI szolgáltató"
    override val providerLabel = "Szolgáltató"
    override val apiKeyLabel = "API kulcs"
    override val modelLabel = "Modell neve"
    override val baseUrlLabel = "Egyedi API végpont (opcionális)"
    override val save = "Mentés"
    override val settingsTip = "Ingyenes API kulcs beszerzése:"
    override val settingsSaved = "Beállítások mentve"

    override val customizeTitle = "Testreszabás"
    override val handModeLabel = "Mód"
    override val handRight = "Jobb kezes"
    override val handLeft = "Bal kezes"
    override val handCenter = "Közép"
    override val showKnownLabel = "„Tudom” kártyák mutatása"
    override val optYes = "Igen"
    override val optNo = "Nem"
    override val allKnownHidden = "Ebben a pakliban minden kártyát tudsz, ezért el vannak rejtve. A Beállításoknál újra megjelenítheted őket."
    override val closeLabel = "Bezár"
    override val helpDownloadTitle = "Letöltés"
    override val helpDownloadBody = "Egy pakli letöltése: a pakli jobb alsó sarkában lévő ⬇ gombbal. Az „Összes pakli letöltése” gomb az összes paklit egyetlen fájlba menti, egymás után.\n\nA fájl sima .txt (UTF-8). A mentés helyét a rendszer fájlválasztója kérdezi meg, külön tároló-engedély nem kell.\n\nFormátum:\n• a pakli neve külön sorban\n• alatta egy ==== vonal, majd egy üres sor\n• soronként egy kártya: a szótári alak, szóközökkel oszlopba rendezve, utána a fordítás\n\nPélda:\nAlap\n====\n\nдом          ház\n\nA fájlt bármilyen szövegszerkesztőben módosíthatod, majd visszatöltheted."
    override val helpUploadTitle = "Feltöltés"
    override val helpUploadBody = "A „Pakli feltöltése” gomb a letöltött .txt formátumot olvassa vissza. Egy- vagy többpaklis fájl is jó (az „Összes pakli letöltése” fájlja is).\n\n• Mindig új pakli jön létre, még azonos név esetén is, a meglévő paklikat nem érinti.\n• A kártyák „nem tudom” állapotban érkeznek.\n• Ha a fájlban nincs pakli-fejléc (név és alatta ==== vonal), a fájl neve lesz a pakli neve.\n• Egy kártya sorában a szótári alakot és a fordítást tabulátor vagy legalább két szóköz válassza el. A régi, gondolatjeles forma („szó - fordítás”) is működik."
    override val cardFrontLabel = "Kártya előlapja"
    override val infoTitle = "Tudnivalók"
    override val infoBody = "Tipp: érdemes több szót összegyűjteni, és egyszerre lefordítani, nem egyesével.\n\n• Az összes összegyűjtött szó egyetlen AI-kérésben megy el, így kevesebb kérést használsz fel. Az ingyenes API kulcsoknak általában kérésszám-korlátjuk van (percenként és naponta), így ez tovább tart.\n• Fotózz le egymás után több szót, majd nyomd meg a „Lefordítás” gombot.\n• A fotó gomb helyét szabadon átviheted: nyomd hosszan, és húzd oda, ahol kényelmes. A célzó téglalap a gomb helyétől függően kicsit oldalra tolódik.\n• A zseblámpa gomb gyenge fényben segít."
    override val testKey = "Kulcs tesztelése"
    override val keyOk = "A kulcs működik ✓"
    override val showStressLabel = "Hangsúlyjel mutatása a kártyán"
    override val exportStressLabel = "Hangsúlyjel a letöltött fájlban"
    override val exportOrderLabel = "Bal oldalon a fájlban és a szem ikonnál"
    override val resetButtonPos = "Fotó gomb alaphelyzetbe"
    override val infoShowLabel = "Info gombok mutatása"
    override val dontShowAgain = "Ne mutasd többé"
    override val wordsInfoBody = "Tipp: érdemes több szót összegyűjteni, és egyszerre generálni belőlük a kártyákat, nem egyesével.\n\n• Az összes szó egyetlen AI-kérésben megy el, így kevesebb kérést használsz fel. Az ingyenes API kulcsoknak általában kérésszám-korlátjuk van (percenként és naponta), így ez tovább tart.\n• Addig gyűjtsd a szavakat (Megosztás menüből vagy kézzel), amíg össze nem jön egy csomag, és csak utána nyomd meg a „Kártyák generálása” gombot."

    override fun langName(l: AppLang): String = when (l) {
        AppLang.HUNGARIAN -> "Magyar"
        AppLang.ENGLISH -> "Angol"
        AppLang.GERMAN -> "Német"
        AppLang.ITALIAN -> "Olasz"
        AppLang.FRENCH -> "Francia"
        AppLang.SPANISH -> "Spanyol"
        AppLang.RUSSIAN -> "Orosz"
    }
}

object StringsEn : Strings {
    override val settingsDesc = "Settings"
    override val navScan = "Scan"
    override val navWord = "Word"
    override val navCard = "Card"
    override val navDeck = "Deck"
    override val noActiveDeck = "No active deck — create one on the Deck tab!"
    override val enterApiKeyFirst = "Enter your API key in Settings first!"
    override fun cardsAdded(n: Int) = "$n card(s) added"
    override fun errorPrefix(msg: String?) = "Error: $msg"
    override val unknownError = "unknown error"
    override val cameraError = "camera error"

    override val wordsHint =
        "Select a word on a [[web page]] or in any [[app]], then [[Share]] → this app. " +
            "The word will appear in this list."
    override val addWordManually = "Add a word manually"
    override val clearList = "Clear list"
    override fun generateCards(n: Int) = "Generate cards ($n)"
    override fun wordAddedToList(word: String) = "Added to word list: $word"

    override val noCardsYet =
        "You have no flashcards yet. Add words on the Word tab, then generate them."
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
    override val shuffle = "Shuffle"
    override val originalOrder = "Original order"

    override val newDeckName = "New deck name"
    override val create = "Create"
    override val renameDeck = "Rename deck"
    override val downloadAll = "Download all decks"
    override val uploadDeck = "Upload deck"
    override fun decksUploaded(decks: Int, cards: Int) =
        if (decks == 1) "Deck uploaded ($cards cards)" else "$decks decks uploaded ($cards cards)"
    override val uploadNothingFound = "No cards were found in the file."
    override val allDecksFileName = "all_decks.txt"
    override fun cardCount(n: Int) = "$n cards"
    override val emptyDeck = "(no cards in this deck yet)"
    override val deckShort = "D"

    override val cameraPermissionNeeded = "Camera permission is required for word recognition."
    override val allowCamera = "Allow camera"
    override fun collectedWords(n: Int) = "Collected words ($n):"
    override fun translateN(n: Int) = "Translate ($n)"

    override val languagesTitle = "Languages"
    override val bookLanguageLabel = "Source language"
    override val translationLanguageLabel = "Translation language"
    override val appLanguageLabel = "App language"
    override val aiProviderTitle = "AI provider"
    override val providerLabel = "Provider"
    override val apiKeyLabel = "API key"
    override val modelLabel = "Model name"
    override val baseUrlLabel = "Custom API endpoint (optional)"
    override val save = "Save"
    override val settingsTip = "Get a free API key:"
    override val settingsSaved = "Settings saved"

    override val customizeTitle = "Customization"
    override val handModeLabel = "Mode"
    override val handRight = "Right-handed"
    override val handLeft = "Left-handed"
    override val handCenter = "Center"
    override val showKnownLabel = "Show “Known” cards"
    override val optYes = "Yes"
    override val optNo = "No"
    override val allKnownHidden = "You know every card in this deck, so they are hidden. You can show them again in Settings."
    override val closeLabel = "Close"
    override val helpDownloadTitle = "Download"
    override val helpDownloadBody = "Download one deck with the ⬇ button in the bottom right corner of the deck. “Download all decks” saves every deck into a single file, one after another.\n\nThe file is plain .txt (UTF-8). The system file picker asks where to save it; no storage permission is needed.\n\nFormat:\n• the deck name on its own line\n• below it a ==== line, then an empty line\n• then one card per line: the dictionary form, padded with spaces into a column, then the translation\n\nExample:\nBasic\n====\n\nдом          house\n\nYou can edit the file in any text editor and upload it again."
    override val helpUploadTitle = "Upload"
    override val helpUploadBody = "“Upload deck” reads back the downloaded .txt format. A file with one or several decks works (including the “Download all decks” file).\n\n• It always creates a new deck, even if the name already exists; your existing decks are not touched.\n• Cards arrive as not known.\n• If the file has no deck header (a name with a ==== line under it), the file name becomes the deck name.\n• On a card line, the dictionary form and the translation must be separated by a tab or at least two spaces. The old dash format (“word - translation”) also works."
    override val cardFrontLabel = "Card front"
    override val infoTitle = "Info"
    override val infoBody = "Tip: collect several words and translate them together instead of one by one.\n\n• All collected words go out in a single AI request, so you use fewer requests. Free API keys usually have a request limit (per minute and per day), so this makes them last longer.\n• Photograph several words in a row, then press “Translate”.\n• You can move the camera button anywhere: press and hold it, then drag it where it is comfortable. The aiming rectangle shifts a little to the side depending on where the button is.\n• The flashlight button helps in dim light."
    override val testKey = "Test key"
    override val keyOk = "The key works ✓"
    override val showStressLabel = "Show stress marks on cards"
    override val exportStressLabel = "Stress marks in downloaded files"
    override val exportOrderLabel = "Left side in files and deck preview"
    override val resetButtonPos = "Reset camera button position"
    override val infoShowLabel = "Show info buttons"
    override val dontShowAgain = "Don't show again"
    override val wordsInfoBody = "Tip: collect several words and generate the cards together instead of one by one.\n\n• All the words go out in a single AI request, so you use fewer requests. Free API keys usually have a request limit (per minute and per day), so this makes them last longer.\n• Keep collecting words (from the Share menu or by hand) and press “Generate cards” only once you have a batch."

    override fun langName(l: AppLang): String = when (l) {
        AppLang.HUNGARIAN -> "Hungarian"
        AppLang.ENGLISH -> "English"
        AppLang.GERMAN -> "German"
        AppLang.ITALIAN -> "Italian"
        AppLang.FRENCH -> "French"
        AppLang.SPANISH -> "Spanish"
        AppLang.RUSSIAN -> "Russian"
    }
}

object StringsDe : Strings {
    override val settingsDesc = "Einstellungen"
    override val navScan = "Scan"
    override val navWord = "Wort"
    override val navCard = "Karte"
    override val navDeck = "Deck"
    override val noActiveDeck = "Kein aktives Deck — erstelle eines im Deck-Tab!"
    override val enterApiKeyFirst = "Gib zuerst in den Einstellungen deinen API-Schlüssel ein!"
    override fun cardsAdded(n: Int) = "$n Karte(n) hinzugefügt"
    override fun errorPrefix(msg: String?) = "Fehler: $msg"
    override val unknownError = "unbekannter Fehler"
    override val cameraError = "Kamerafehler"

    override val wordsHint =
        "Markiere auf einer [[Webseite]] oder in einer beliebigen [[App]] ein Wort und wähle dann [[Teilen]] → diese App. " +
            "Das Wort erscheint dann in dieser Liste."
    override val addWordManually = "Wort manuell hinzufügen"
    override val clearList = "Liste leeren"
    override fun generateCards(n: Int) = "Karten erstellen ($n)"
    override fun wordAddedToList(word: String) = "Zur Wortliste hinzugefügt: $word"

    override val noCardsYet =
        "Du hast noch keine Karteikarten. Füge im Wort-Tab Wörter hinzu und erstelle dann die Karten."
    override val delete = "Löschen"
    override val confirmDeleteCard = "Möchtest du diese Karte wirklich löschen?"
    override fun confirmDeleteDeck(name: String) = "Möchtest du das Deck \"$name\" mit allen Karten wirklich löschen?"
    override val yesDelete = "Ja, löschen"
    override val cancel = "Abbrechen"
    override val known = "Gekonnt ✓"
    override val markKnown = "Als gekonnt markieren"
    override val editCard = "Karte bearbeiten"
    override val dictionaryFormLabel = "Grundform"
    override val translationLabel = "Übersetzung"
    override val previous = "◀ Zurück"
    override val next = "Weiter ▶"
    override val shuffle = "Mischen"
    override val originalOrder = "Ursprüngliche Reihenfolge"

    override val newDeckName = "Name des neuen Decks"
    override val create = "Erstellen"
    override val renameDeck = "Deck umbenennen"
    override val downloadAll = "Alle Decks herunterladen"
    override val uploadDeck = "Deck hochladen"
    override fun decksUploaded(decks: Int, cards: Int) =
        if (decks == 1) "Deck hochgeladen ($cards Karten)" else "$decks Decks hochgeladen ($cards Karten)"
    override val uploadNothingFound = "In der Datei wurden keine Karten gefunden."
    override val allDecksFileName = "alle_decks.txt"
    override fun cardCount(n: Int) = "$n Karten"
    override val emptyDeck = "(noch keine Karten in diesem Deck)"
    override val deckShort = "D"

    override val cameraPermissionNeeded = "Für die Worterkennung wird die Kameraberechtigung benötigt."
    override val allowCamera = "Kamera erlauben"
    override fun collectedWords(n: Int) = "Gesammelte Wörter ($n):"
    override fun translateN(n: Int) = "Übersetzen ($n)"

    override val languagesTitle = "Sprachen"
    override val bookLanguageLabel = "Ausgangssprache"
    override val translationLanguageLabel = "Sprache der Übersetzung"
    override val appLanguageLabel = "Sprache der App"
    override val aiProviderTitle = "KI-Anbieter"
    override val providerLabel = "Anbieter"
    override val apiKeyLabel = "API-Schlüssel"
    override val modelLabel = "Modellname"
    override val baseUrlLabel = "Eigener API-Endpunkt (optional)"
    override val save = "Speichern"
    override val settingsTip = "Kostenlosen API-Schlüssel erhalten:"
    override val settingsSaved = "Einstellungen gespeichert"

    override val customizeTitle = "Anpassung"
    override val handModeLabel = "Modus"
    override val handRight = "Rechtshänder"
    override val handLeft = "Linkshänder"
    override val handCenter = "Mitte"
    override val showKnownLabel = "„Gekonnt“-Karten anzeigen"
    override val optYes = "Ja"
    override val optNo = "Nein"
    override val allKnownHidden = "In diesem Deck kennst du alle Karten, deshalb sind sie ausgeblendet. In den Einstellungen kannst du sie wieder anzeigen."
    override val closeLabel = "Schließen"
    override val helpDownloadTitle = "Herunterladen"
    override val helpDownloadBody = "Ein einzelnes Deck lädst du mit dem ⬇-Button unten rechts am Deck herunter. „Alle Decks herunterladen“ speichert alle Decks nacheinander in einer einzigen Datei.\n\nDie Datei ist eine einfache .txt-Datei (UTF-8). Den Speicherort fragt die Dateiauswahl des Systems ab, eine Speicherberechtigung ist nicht nötig.\n\nFormat:\n• der Deckname in einer eigenen Zeile\n• darunter eine ====-Linie, danach eine Leerzeile\n• dann pro Zeile eine Karte: die Grundform, mit Leerzeichen in eine Spalte ausgerichtet, danach die Übersetzung\n\nBeispiel:\nGrundstock\n====\n\nдом          Haus\n\nDie Datei kannst du in einem beliebigen Texteditor bearbeiten und wieder hochladen."
    override val helpUploadTitle = "Hochladen"
    override val helpUploadBody = "„Deck hochladen“ liest das heruntergeladene .txt-Format wieder ein. Eine Datei mit einem oder mehreren Decks funktioniert (auch die Datei von „Alle Decks herunterladen“).\n\n• Es entsteht immer ein neues Deck, auch bei gleichem Namen; bestehende Decks bleiben unberührt.\n• Die Karten kommen als „nicht gekonnt“ an.\n• Hat die Datei keine Deck-Kopfzeile (Name mit ====-Linie darunter), wird der Dateiname zum Decknamen.\n• In einer Kartenzeile müssen Grundform und Übersetzung durch einen Tabulator oder mindestens zwei Leerzeichen getrennt sein. Das alte Format mit Bindestrich („Wort - Übersetzung“) funktioniert ebenfalls."
    override val cardFrontLabel = "Vorderseite der Karte"
    override val infoTitle = "Hinweise"
    override val infoBody = "Tipp: Sammle mehrere Wörter und übersetze sie gemeinsam statt einzeln.\n\n• Alle gesammelten Wörter gehen in einer einzigen KI-Anfrage raus, du verbrauchst also weniger Anfragen. Kostenlose API-Schlüssel haben meist ein Anfragelimit (pro Minute und pro Tag), so halten sie länger.\n• Fotografiere mehrere Wörter hintereinander und tippe dann auf „Übersetzen“.\n• Du kannst den Kamera-Button frei verschieben: lange drücken und dorthin ziehen, wo es bequem ist. Das Zielrechteck rückt je nach Position des Buttons ein Stück zur Seite.\n• Die Taschenlampe hilft bei schwachem Licht."
    override val testKey = "Schlüssel testen"
    override val keyOk = "Der Schlüssel funktioniert ✓"
    override val showStressLabel = "Betonungszeichen auf Karten anzeigen"
    override val exportStressLabel = "Betonungszeichen in heruntergeladenen Dateien"
    override val exportOrderLabel = "Linke Spalte in Dateien und Deck-Ansicht"
    override val resetButtonPos = "Kamera-Button zurücksetzen"
    override val infoShowLabel = "Info-Buttons anzeigen"
    override val dontShowAgain = "Nicht mehr anzeigen"
    override val wordsInfoBody = "Tipp: Sammle mehrere Wörter und erstelle die Karten gemeinsam statt einzeln.\n\n• Alle Wörter gehen in einer einzigen KI-Anfrage raus, du verbrauchst also weniger Anfragen. Kostenlose API-Schlüssel haben meist ein Anfragelimit (pro Minute und pro Tag), so halten sie länger.\n• Sammle die Wörter (über das Teilen-Menü oder von Hand) und tippe erst auf „Karten erstellen“, wenn du einen Stapel zusammen hast."

    override fun langName(l: AppLang): String = when (l) {
        AppLang.HUNGARIAN -> "Ungarisch"
        AppLang.ENGLISH -> "Englisch"
        AppLang.GERMAN -> "Deutsch"
        AppLang.ITALIAN -> "Italienisch"
        AppLang.FRENCH -> "Französisch"
        AppLang.SPANISH -> "Spanisch"
        AppLang.RUSSIAN -> "Russisch"
    }
}

object StringsIt : Strings {
    override val settingsDesc = "Impostazioni"
    override val navScan = "Scan"
    override val navWord = "Parola"
    override val navCard = "Scheda"
    override val navDeck = "Mazzo"
    override val noActiveDeck = "Nessun mazzo attivo — creane uno nella sezione Mazzo!"
    override val enterApiKeyFirst = "Inserisci prima la chiave API nelle Impostazioni!"
    override fun cardsAdded(n: Int) = "$n schede aggiunte"
    override fun errorPrefix(msg: String?) = "Errore: $msg"
    override val unknownError = "errore sconosciuto"
    override val cameraError = "errore della fotocamera"

    override val wordsHint =
        "Seleziona una parola in una [[pagina web]] o in qualsiasi [[app]], poi [[Condividi]] → questa app. " +
            "La parola apparirà in questo elenco."
    override val addWordManually = "Aggiungi una parola manualmente"
    override val clearList = "Svuota elenco"
    override fun generateCards(n: Int) = "Genera schede ($n)"
    override fun wordAddedToList(word: String) = "Aggiunta all'elenco: $word"

    override val noCardsYet =
        "Non hai ancora nessuna scheda. Aggiungi parole nella sezione Parola, poi generale."
    override val delete = "Elimina"
    override val confirmDeleteCard = "Vuoi davvero eliminare questa scheda?"
    override fun confirmDeleteDeck(name: String) = "Vuoi davvero eliminare il mazzo \"$name\" con tutte le sue schede?"
    override val yesDelete = "Sì, elimina"
    override val cancel = "Annulla"
    override val known = "Lo so ✓"
    override val markKnown = "Segna come saputa"
    override val editCard = "Modifica scheda"
    override val dictionaryFormLabel = "Forma base"
    override val translationLabel = "Traduzione"
    override val previous = "◀ Precedente"
    override val next = "Successiva ▶"
    override val shuffle = "Mescola"
    override val originalOrder = "Ordine originale"

    override val newDeckName = "Nome del nuovo mazzo"
    override val create = "Crea"
    override val renameDeck = "Rinomina mazzo"
    override val downloadAll = "Scarica tutti i mazzi"
    override val uploadDeck = "Carica mazzo"
    override fun decksUploaded(decks: Int, cards: Int) =
        if (decks == 1) "Mazzo caricato ($cards schede)" else "$decks mazzi caricati ($cards schede)"
    override val uploadNothingFound = "Nel file non è stata trovata nessuna scheda."
    override val allDecksFileName = "tutti_i_mazzi.txt"
    override fun cardCount(n: Int) = "$n schede"
    override val emptyDeck = "(nessuna scheda in questo mazzo)"
    override val deckShort = "M"

    override val cameraPermissionNeeded = "Per il riconoscimento delle parole serve il permesso della fotocamera."
    override val allowCamera = "Consenti fotocamera"
    override fun collectedWords(n: Int) = "Parole raccolte ($n):"
    override fun translateN(n: Int) = "Traduci ($n)"

    override val languagesTitle = "Lingue"
    override val bookLanguageLabel = "Lingua di origine"
    override val translationLanguageLabel = "Lingua della traduzione"
    override val appLanguageLabel = "Lingua dell'app"
    override val aiProviderTitle = "Provider IA"
    override val providerLabel = "Provider"
    override val apiKeyLabel = "Chiave API"
    override val modelLabel = "Nome del modello"
    override val baseUrlLabel = "Endpoint API personalizzato (facoltativo)"
    override val save = "Salva"
    override val settingsTip = "Ottieni una chiave API gratuita:"
    override val settingsSaved = "Impostazioni salvate"

    override val customizeTitle = "Personalizzazione"
    override val handModeLabel = "Modalità"
    override val handRight = "Destrorso"
    override val handLeft = "Mancino"
    override val handCenter = "Centro"
    override val showKnownLabel = "Mostra le schede «Lo so»"
    override val optYes = "Sì"
    override val optNo = "No"
    override val allKnownHidden = "In questo mazzo conosci tutte le schede, quindi sono nascoste. Puoi mostrarle di nuovo nelle Impostazioni."
    override val closeLabel = "Chiudi"
    override val helpDownloadTitle = "Scaricamento"
    override val helpDownloadBody = "Scarichi un singolo mazzo con il pulsante ⬇ in basso a destra del mazzo. «Scarica tutti i mazzi» salva tutti i mazzi uno dopo l'altro in un unico file.\n\nIl file è un semplice .txt (UTF-8). Il punto di salvataggio si sceglie con il selettore di file del sistema, non serve il permesso di archiviazione.\n\nFormato:\n• il nome del mazzo su una riga a sé\n• sotto una linea ====, poi una riga vuota\n• poi una scheda per riga: la forma base, allineata in colonna con degli spazi, poi la traduzione\n\nEsempio:\nBase\n====\n\nдом          casa\n\nPuoi modificare il file con qualsiasi editor di testo e ricaricarlo."
    override val helpUploadTitle = "Caricamento"
    override val helpUploadBody = "«Carica mazzo» rilegge il formato .txt scaricato. Va bene un file con uno o più mazzi (anche quello di «Scarica tutti i mazzi»).\n\n• Crea sempre un nuovo mazzo, anche se il nome esiste già; i mazzi esistenti non vengono toccati.\n• Le schede arrivano come non conosciute.\n• Se il file non ha un'intestazione di mazzo (un nome con una linea ==== sotto), il nome del file diventa il nome del mazzo.\n• In una riga di scheda, forma base e traduzione devono essere separate da una tabulazione o da almeno due spazi. Funziona anche il vecchio formato con il trattino («parola - traduzione»)."
    override val cardFrontLabel = "Fronte della scheda"
    override val infoTitle = "Informazioni"
    override val infoBody = "Consiglio: raccogli più parole e traducile insieme invece che una per una.\n\n• Tutte le parole raccolte partono in un'unica richiesta IA, quindi usi meno richieste. Le chiavi API gratuite di solito hanno un limite di richieste (al minuto e al giorno), così durano di più.\n• Fotografa più parole di fila, poi premi «Traduci».\n• Puoi spostare liberamente il pulsante della fotocamera: tienilo premuto e trascinalo dove ti è comodo. Il rettangolo di mira si sposta un po' di lato a seconda della posizione del pulsante.\n• La torcia aiuta con poca luce."
    override val testKey = "Prova la chiave"
    override val keyOk = "La chiave funziona ✓"
    override val showStressLabel = "Mostra gli accenti sulle schede"
    override val exportStressLabel = "Accenti nei file scaricati"
    override val exportOrderLabel = "Colonna a sinistra nei file e nell'anteprima"
    override val resetButtonPos = "Ripristina la posizione del pulsante"
    override val infoShowLabel = "Mostra i pulsanti info"
    override val dontShowAgain = "Non mostrare più"
    override val wordsInfoBody = "Consiglio: raccogli più parole e genera le schede tutte insieme invece che una per una.\n\n• Tutte le parole partono in un'unica richiesta IA, quindi usi meno richieste. Le chiavi API gratuite di solito hanno un limite di richieste (al minuto e al giorno), così durano di più.\n• Continua a raccogliere parole (dal menu Condividi o a mano) e premi «Genera schede» solo quando ne hai un gruppo."

    override fun langName(l: AppLang): String = when (l) {
        AppLang.HUNGARIAN -> "Ungherese"
        AppLang.ENGLISH -> "Inglese"
        AppLang.GERMAN -> "Tedesco"
        AppLang.ITALIAN -> "Italiano"
        AppLang.FRENCH -> "Francese"
        AppLang.SPANISH -> "Spagnolo"
        AppLang.RUSSIAN -> "Russo"
    }
}

object StringsFr : Strings {
    override val settingsDesc = "Paramètres"
    override val navScan = "Scan"
    override val navWord = "Mot"
    override val navCard = "Carte"
    override val navDeck = "Paquet"
    override val noActiveDeck = "Aucun paquet actif — crée-en un dans l'onglet Paquet !"
    override val enterApiKeyFirst = "Saisis d'abord la clé API dans les Paramètres !"
    override fun cardsAdded(n: Int) = "$n carte(s) ajoutée(s)"
    override fun errorPrefix(msg: String?) = "Erreur : $msg"
    override val unknownError = "erreur inconnue"
    override val cameraError = "erreur de l'appareil photo"

    override val wordsHint =
        "Sélectionne un mot sur une [[page web]] ou dans n'importe quelle [[application]], puis [[Partager]] → cette application. " +
            "Le mot apparaîtra dans cette liste."
    override val addWordManually = "Ajouter un mot manuellement"
    override val clearList = "Vider la liste"
    override fun generateCards(n: Int) = "Générer les cartes ($n)"
    override fun wordAddedToList(word: String) = "Ajouté à la liste : $word"

    override val noCardsYet =
        "Tu n'as pas encore de cartes. Ajoute des mots dans l'onglet Mot, puis génère-les."
    override val delete = "Supprimer"
    override val confirmDeleteCard = "Veux-tu vraiment supprimer cette carte ?"
    override fun confirmDeleteDeck(name: String) = "Veux-tu vraiment supprimer le paquet \"$name\" avec toutes ses cartes ?"
    override val yesDelete = "Oui, supprimer"
    override val cancel = "Annuler"
    override val known = "Connu ✓"
    override val markKnown = "Marquer comme connu"
    override val editCard = "Modifier la carte"
    override val dictionaryFormLabel = "Forme de base"
    override val translationLabel = "Traduction"
    override val previous = "◀ Précédent"
    override val next = "Suivant ▶"
    override val shuffle = "Mélanger"
    override val originalOrder = "Ordre d'origine"

    override val newDeckName = "Nom du nouveau paquet"
    override val create = "Créer"
    override val renameDeck = "Renommer le paquet"
    override val downloadAll = "Télécharger tous les paquets"
    override val uploadDeck = "Importer un paquet"
    override fun decksUploaded(decks: Int, cards: Int) =
        if (decks == 1) "Paquet importé ($cards cartes)" else "$decks paquets importés ($cards cartes)"
    override val uploadNothingFound = "Aucune carte trouvée dans le fichier."
    override val allDecksFileName = "tous_les_paquets.txt"
    override fun cardCount(n: Int) = "$n cartes"
    override val emptyDeck = "(aucune carte dans ce paquet pour l'instant)"
    override val deckShort = "P"

    override val cameraPermissionNeeded = "L'autorisation de l'appareil photo est nécessaire pour la reconnaissance des mots."
    override val allowCamera = "Autoriser l'appareil photo"
    override fun collectedWords(n: Int) = "Mots collectés ($n) :"
    override fun translateN(n: Int) = "Traduire ($n)"

    override val languagesTitle = "Langues"
    override val bookLanguageLabel = "Langue source"
    override val translationLanguageLabel = "Langue de traduction"
    override val appLanguageLabel = "Langue de l'application"
    override val aiProviderTitle = "Fournisseur d'IA"
    override val providerLabel = "Fournisseur"
    override val apiKeyLabel = "Clé API"
    override val modelLabel = "Nom du modèle"
    override val baseUrlLabel = "Point de terminaison API personnalisé (facultatif)"
    override val save = "Enregistrer"
    override val settingsTip = "Obtenir une clé API gratuite :"
    override val settingsSaved = "Paramètres enregistrés"

    override val customizeTitle = "Personnalisation"
    override val handModeLabel = "Mode"
    override val handRight = "Droitier"
    override val handLeft = "Gaucher"
    override val handCenter = "Centre"
    override val showKnownLabel = "Afficher les cartes « Connu »"
    override val optYes = "Oui"
    override val optNo = "Non"
    override val allKnownHidden = "Tu connais toutes les cartes de ce paquet, elles sont donc masquées. Tu peux les afficher à nouveau dans les Paramètres."
    override val closeLabel = "Fermer"
    override val helpDownloadTitle = "Téléchargement"
    override val helpDownloadBody = "Tu télécharges un seul paquet avec le bouton ⬇ en bas à droite du paquet. « Télécharger tous les paquets » enregistre tous les paquets les uns après les autres dans un seul fichier.\n\nLe fichier est un simple .txt (UTF-8). Le sélecteur de fichiers du système demande où l'enregistrer, aucune autorisation de stockage n'est nécessaire.\n\nFormat :\n• le nom du paquet sur sa propre ligne\n• en dessous une ligne ====, puis une ligne vide\n• ensuite une carte par ligne : la forme de base, alignée en colonne avec des espaces, puis la traduction\n\nExemple :\nBase\n====\n\nдом          maison\n\nTu peux modifier le fichier dans n'importe quel éditeur de texte, puis le réimporter."
    override val helpUploadTitle = "Importation"
    override val helpUploadBody = "« Importer un paquet » relit le format .txt téléchargé. Un fichier avec un ou plusieurs paquets convient (y compris celui de « Télécharger tous les paquets »).\n\n• Un nouveau paquet est toujours créé, même si le nom existe déjà ; les paquets existants ne sont pas modifiés.\n• Les cartes arrivent comme non connues.\n• Si le fichier n'a pas d'en-tête de paquet (un nom avec une ligne ==== en dessous), le nom du fichier devient le nom du paquet.\n• Sur une ligne de carte, la forme de base et la traduction doivent être séparées par une tabulation ou au moins deux espaces. L'ancien format avec tiret (« mot - traduction ») fonctionne aussi."
    override val cardFrontLabel = "Recto de la carte"
    override val infoTitle = "Infos"
    override val infoBody = "Astuce : rassemble plusieurs mots et traduis-les ensemble plutôt qu'un par un.\n\n• Tous les mots collectés partent dans une seule requête IA, tu utilises donc moins de requêtes. Les clés API gratuites ont généralement une limite de requêtes (par minute et par jour), elles durent ainsi plus longtemps.\n• Photographie plusieurs mots à la suite, puis appuie sur « Traduire ».\n• Tu peux déplacer librement le bouton de l'appareil photo : appuie longuement dessus et fais-le glisser où c'est confortable. Le rectangle de visée se décale un peu sur le côté selon la position du bouton.\n• La lampe torche aide quand il y a peu de lumière."
    override val testKey = "Tester la clé"
    override val keyOk = "La clé fonctionne ✓"
    override val showStressLabel = "Afficher les accents toniques sur les cartes"
    override val exportStressLabel = "Accents toniques dans les fichiers téléchargés"
    override val exportOrderLabel = "Colonne de gauche dans les fichiers et l'aperçu"
    override val resetButtonPos = "Réinitialiser la position du bouton"
    override val infoShowLabel = "Afficher les boutons d'info"
    override val dontShowAgain = "Ne plus afficher"
    override val wordsInfoBody = "Astuce : rassemble plusieurs mots et génère les cartes ensemble plutôt qu'une par une.\n\n• Tous les mots partent dans une seule requête IA, tu utilises donc moins de requêtes. Les clés API gratuites ont généralement une limite de requêtes (par minute et par jour), elles durent ainsi plus longtemps.\n• Continue à collecter des mots (depuis le menu Partager ou à la main) et appuie sur « Générer les cartes » seulement quand tu en as un lot."

    override fun langName(l: AppLang): String = when (l) {
        AppLang.HUNGARIAN -> "Hongrois"
        AppLang.ENGLISH -> "Anglais"
        AppLang.GERMAN -> "Allemand"
        AppLang.ITALIAN -> "Italien"
        AppLang.FRENCH -> "Français"
        AppLang.SPANISH -> "Espagnol"
        AppLang.RUSSIAN -> "Russe"
    }
}

object StringsEs : Strings {
    override val settingsDesc = "Ajustes"
    override val navScan = "Scan"
    override val navWord = "Palabra"
    override val navCard = "Tarjeta"
    override val navDeck = "Mazo"
    override val noActiveDeck = "No hay ningún mazo activo: ¡crea uno en la pestaña Mazo!"
    override val enterApiKeyFirst = "¡Primero introduce la clave API en Ajustes!"
    override fun cardsAdded(n: Int) = "$n tarjeta(s) añadida(s)"
    override fun errorPrefix(msg: String?) = "Error: $msg"
    override val unknownError = "error desconocido"
    override val cameraError = "error de la cámara"

    override val wordsHint =
        "Selecciona una palabra en una [[página web]] o en cualquier [[app]] y luego [[Compartir]] → esta app. " +
            "La palabra aparecerá en esta lista."
    override val addWordManually = "Añadir una palabra manualmente"
    override val clearList = "Vaciar lista"
    override fun generateCards(n: Int) = "Generar tarjetas ($n)"
    override fun wordAddedToList(word: String) = "Añadida a la lista: $word"

    override val noCardsYet =
        "Todavía no tienes tarjetas. Añade palabras en la pestaña Palabra y luego genéralas."
    override val delete = "Eliminar"
    override val confirmDeleteCard = "¿Seguro que quieres eliminar esta tarjeta?"
    override fun confirmDeleteDeck(name: String) = "¿Seguro que quieres eliminar el mazo \"$name\" con todas sus tarjetas?"
    override val yesDelete = "Sí, eliminar"
    override val cancel = "Cancelar"
    override val known = "Sabida ✓"
    override val markKnown = "Marcar como sabida"
    override val editCard = "Editar tarjeta"
    override val dictionaryFormLabel = "Forma base"
    override val translationLabel = "Traducción"
    override val previous = "◀ Anterior"
    override val next = "Siguiente ▶"
    override val shuffle = "Barajar"
    override val originalOrder = "Orden original"

    override val newDeckName = "Nombre del nuevo mazo"
    override val create = "Crear"
    override val renameDeck = "Renombrar mazo"
    override val downloadAll = "Descargar todos los mazos"
    override val uploadDeck = "Subir mazo"
    override fun decksUploaded(decks: Int, cards: Int) =
        if (decks == 1) "Mazo subido ($cards tarjetas)" else "$decks mazos subidos ($cards tarjetas)"
    override val uploadNothingFound = "No se encontró ninguna tarjeta en el archivo."
    override val allDecksFileName = "todos_los_mazos.txt"
    override fun cardCount(n: Int) = "$n tarjetas"
    override val emptyDeck = "(todavía no hay tarjetas en este mazo)"
    override val deckShort = "M"

    override val cameraPermissionNeeded = "Se necesita el permiso de la cámara para reconocer palabras."
    override val allowCamera = "Permitir cámara"
    override fun collectedWords(n: Int) = "Palabras recopiladas ($n):"
    override fun translateN(n: Int) = "Traducir ($n)"

    override val languagesTitle = "Idiomas"
    override val bookLanguageLabel = "Idioma de origen"
    override val translationLanguageLabel = "Idioma de la traducción"
    override val appLanguageLabel = "Idioma de la app"
    override val aiProviderTitle = "Proveedor de IA"
    override val providerLabel = "Proveedor"
    override val apiKeyLabel = "Clave API"
    override val modelLabel = "Nombre del modelo"
    override val baseUrlLabel = "Endpoint de API personalizado (opcional)"
    override val save = "Guardar"
    override val settingsTip = "Obtener una clave API gratuita:"
    override val settingsSaved = "Ajustes guardados"

    override val customizeTitle = "Personalización"
    override val handModeLabel = "Modo"
    override val handRight = "Diestro"
    override val handLeft = "Zurdo"
    override val handCenter = "Centro"
    override val showKnownLabel = "Mostrar las tarjetas «Sabida»"
    override val optYes = "Sí"
    override val optNo = "No"
    override val allKnownHidden = "En este mazo sabes todas las tarjetas, por eso están ocultas. Puedes volver a mostrarlas en Ajustes."
    override val closeLabel = "Cerrar"
    override val helpDownloadTitle = "Descarga"
    override val helpDownloadBody = "Descargas un solo mazo con el botón ⬇ de la esquina inferior derecha del mazo. «Descargar todos los mazos» guarda todos los mazos uno tras otro en un único archivo.\n\nEl archivo es un .txt sencillo (UTF-8). El selector de archivos del sistema pregunta dónde guardarlo, no hace falta permiso de almacenamiento.\n\nFormato:\n• el nombre del mazo en una línea aparte\n• debajo una línea ====, luego una línea vacía\n• después una tarjeta por línea: la forma base, alineada en columna con espacios, y luego la traducción\n\nEjemplo:\nBase\n====\n\nдом          casa\n\nPuedes editar el archivo con cualquier editor de texto y volver a subirlo."
    override val helpUploadTitle = "Subida"
    override val helpUploadBody = "«Subir mazo» vuelve a leer el formato .txt descargado. Sirve un archivo con uno o varios mazos (también el de «Descargar todos los mazos»).\n\n• Siempre se crea un mazo nuevo, aunque el nombre ya exista; los mazos existentes no se tocan.\n• Las tarjetas llegan como no sabidas.\n• Si el archivo no tiene encabezado de mazo (un nombre con una línea ==== debajo), el nombre del archivo pasa a ser el nombre del mazo.\n• En la línea de una tarjeta, la forma base y la traducción deben ir separadas por un tabulador o por al menos dos espacios. El formato antiguo con guion («palabra - traducción») también funciona."
    override val cardFrontLabel = "Anverso de la tarjeta"
    override val infoTitle = "Información"
    override val infoBody = "Consejo: reúne varias palabras y tradúcelas juntas en lugar de una por una.\n\n• Todas las palabras reunidas se envían en una sola petición de IA, así que usas menos peticiones. Las claves API gratuitas suelen tener un límite de peticiones (por minuto y por día), de modo que duran más.\n• Fotografía varias palabras seguidas y luego pulsa «Traducir».\n• Puedes mover libremente el botón de la cámara: mantenlo pulsado y arrástralo donde te resulte cómodo. El rectángulo de puntería se desplaza un poco hacia un lado según la posición del botón.\n• La linterna ayuda con poca luz."
    override val testKey = "Probar la clave"
    override val keyOk = "La clave funciona ✓"
    override val showStressLabel = "Mostrar los acentos en las tarjetas"
    override val exportStressLabel = "Acentos en los archivos descargados"
    override val exportOrderLabel = "Columna izquierda en archivos y vista previa"
    override val resetButtonPos = "Restablecer la posición del botón"
    override val infoShowLabel = "Mostrar los botones de información"
    override val dontShowAgain = "No mostrar más"
    override val wordsInfoBody = "Consejo: reúne varias palabras y genera las tarjetas juntas en lugar de una por una.\n\n• Todas las palabras se envían en una sola petición de IA, así que usas menos peticiones. Las claves API gratuitas suelen tener un límite de peticiones (por minuto y por día), de modo que duran más.\n• Sigue reuniendo palabras (desde el menú Compartir o a mano) y pulsa «Generar tarjetas» solo cuando tengas un grupo."

    override fun langName(l: AppLang): String = when (l) {
        AppLang.HUNGARIAN -> "Húngaro"
        AppLang.ENGLISH -> "Inglés"
        AppLang.GERMAN -> "Alemán"
        AppLang.ITALIAN -> "Italiano"
        AppLang.FRENCH -> "Francés"
        AppLang.SPANISH -> "Español"
        AppLang.RUSSIAN -> "Ruso"
    }
}

object StringsRu : Strings {
    override val settingsDesc = "Настройки"
    override val navScan = "Скан"
    override val navWord = "Слово"
    override val navCard = "Карточки"
    override val navDeck = "Колода"
    override val noActiveDeck = "Нет активной колоды — создай её на вкладке «Колода»!"
    override val enterApiKeyFirst = "Сначала введи API-ключ в настройках!"
    override fun cardsAdded(n: Int) = "Добавлено карточек: $n"
    override fun errorPrefix(msg: String?) = "Ошибка: $msg"
    override val unknownError = "неизвестная ошибка"
    override val cameraError = "ошибка камеры"

    override val wordsHint =
        "Выдели слово на [[веб-странице]] или в любом [[приложении]], затем [[«Поделиться»]] → это приложение. " +
            "Слово появится в этом списке."
    override val addWordManually = "Добавить слово вручную"
    override val clearList = "Очистить список"
    override fun generateCards(n: Int) = "Создать карточки ($n)"
    override fun wordAddedToList(word: String) = "Добавлено в список: $word"

    override val noCardsYet =
        "У тебя пока нет карточек. Добавь слова на вкладке «Слово», затем создай карточки."
    override val delete = "Удалить"
    override val confirmDeleteCard = "Точно удалить эту карточку?"
    override fun confirmDeleteDeck(name: String) = "Точно удалить колоду \"$name\" со всеми карточками?"
    override val yesDelete = "Да, удалить"
    override val cancel = "Отмена"
    override val known = "Знаю ✓"
    override val markKnown = "Отметить: знаю"
    override val editCard = "Редактировать карточку"
    override val dictionaryFormLabel = "Словарная форма"
    override val translationLabel = "Перевод"
    override val previous = "◀ Назад"
    override val next = "Далее ▶"
    override val shuffle = "Перемешать"
    override val originalOrder = "Исходный порядок"

    override val newDeckName = "Название новой колоды"
    override val create = "Создать"
    override val renameDeck = "Переименовать колоду"
    override val downloadAll = "Скачать все колоды"
    override val uploadDeck = "Загрузить колоду"
    override fun decksUploaded(decks: Int, cards: Int) =
        if (decks == 1) "Колода загружена (карточек: $cards)" else "Загружено колод: $decks (карточек: $cards)"
    override val uploadNothingFound = "В файле не найдено ни одной карточки."
    override val allDecksFileName = "vse_kolody.txt"
    override fun cardCount(n: Int) = "Карточек: $n"
    override val emptyDeck = "(в этой колоде пока нет карточек)"
    override val deckShort = "К"

    override val cameraPermissionNeeded = "Для распознавания слов нужно разрешение на использование камеры."
    override val allowCamera = "Разрешить камеру"
    override fun collectedWords(n: Int) = "Собранные слова ($n):"
    override fun translateN(n: Int) = "Перевести ($n)"

    override val languagesTitle = "Языки"
    override val bookLanguageLabel = "Исходный язык"
    override val translationLanguageLabel = "Язык перевода"
    override val appLanguageLabel = "Язык приложения"
    override val aiProviderTitle = "Поставщик ИИ"
    override val providerLabel = "Поставщик"
    override val apiKeyLabel = "API-ключ"
    override val modelLabel = "Название модели"
    override val baseUrlLabel = "Свой API-адрес (необязательно)"
    override val save = "Сохранить"
    override val settingsTip = "Получить бесплатный API-ключ:"
    override val settingsSaved = "Настройки сохранены"

    override val customizeTitle = "Персонализация"
    override val handModeLabel = "Режим"
    override val handRight = "Для правшей"
    override val handLeft = "Для левшей"
    override val handCenter = "По центру"
    override val showKnownLabel = "Показывать карточки «Знаю»"
    override val optYes = "Да"
    override val optNo = "Нет"
    override val allKnownHidden = "В этой колоде ты знаешь все карточки, поэтому они скрыты. Показать их снова можно в настройках."
    override val closeLabel = "Закрыть"
    override val helpDownloadTitle = "Скачивание"
    override val helpDownloadBody = "Одну колоду можно скачать кнопкой ⬇ в правом нижнем углу колоды. Кнопка «Скачать все колоды» сохраняет все колоды подряд в один файл.\n\nФайл — обычный .txt (UTF-8). Место сохранения спрашивает системный выбор файлов, разрешение на доступ к памяти не нужно.\n\nФормат:\n• название колоды в отдельной строке\n• под ним линия ====, затем пустая строка\n• далее по одной карточке в строке: словарная форма, выровненная пробелами в столбец, затем перевод\n\nПример:\nОсновная\n====\n\nhouse          дом\n\nФайл можно править в любом текстовом редакторе и загружать обратно."
    override val helpUploadTitle = "Загрузка"
    override val helpUploadBody = "«Загрузить колоду» читает скачанный формат .txt. Подойдёт файл с одной или несколькими колодами (в том числе файл из «Скачать все колоды»).\n\n• Всегда создаётся новая колода, даже если такое название уже есть; существующие колоды не затрагиваются.\n• Карточки приходят как «не знаю».\n• Если в файле нет заголовка колоды (название с линией ==== под ним), названием колоды станет имя файла.\n• В строке карточки словарная форма и перевод должны быть разделены табуляцией или минимум двумя пробелами. Старый формат с тире («слово - перевод») тоже работает."
    override val cardFrontLabel = "Лицевая сторона карточки"
    override val infoTitle = "Информация"
    override val infoBody = "Совет: собирай несколько слов и переводи их вместе, а не по одному.\n\n• Все собранные слова уходят одним запросом к ИИ, так что ты тратишь меньше запросов. У бесплатных API-ключей обычно есть лимит запросов (в минуту и в день), поэтому так их хватает дольше.\n• Сфотографируй несколько слов подряд, затем нажми «Перевести».\n• Кнопку камеры можно свободно переместить: нажми и удерживай её, затем перетащи туда, где удобно. Прицельный прямоугольник немного сдвигается в сторону в зависимости от положения кнопки.\n• Фонарик помогает при слабом освещении."
    override val testKey = "Проверить ключ"
    override val keyOk = "Ключ работает ✓"
    override val showStressLabel = "Показывать ударения на карточках"
    override val exportStressLabel = "Ударения в скачанных файлах"
    override val exportOrderLabel = "Левый столбец в файлах и просмотре колоды"
    override val resetButtonPos = "Вернуть кнопку камеры на место"
    override val infoShowLabel = "Показывать кнопки информации"
    override val dontShowAgain = "Больше не показывать"
    override val wordsInfoBody = "Совет: собирай несколько слов и создавай карточки сразу, а не по одной.\n\n• Все слова уходят одним запросом к ИИ, так что ты тратишь меньше запросов. У бесплатных API-ключей обычно есть лимит запросов (в минуту и в день), поэтому так их хватает дольше.\n• Продолжай собирать слова (через меню «Поделиться» или вручную) и нажимай «Создать карточки», только когда наберётся пачка."

    override fun langName(l: AppLang): String = when (l) {
        AppLang.HUNGARIAN -> "Венгерский"
        AppLang.ENGLISH -> "Английский"
        AppLang.GERMAN -> "Немецкий"
        AppLang.ITALIAN -> "Итальянский"
        AppLang.FRENCH -> "Французский"
        AppLang.SPANISH -> "Испанский"
        AppLang.RUSSIAN -> "Русский"
    }
}

val LocalStrings = staticCompositionLocalOf<Strings> { StringsHu }

fun stringsFor(lang: AppLang): Strings = when (lang) {
    AppLang.HUNGARIAN -> StringsHu
    AppLang.ENGLISH -> StringsEn
    AppLang.GERMAN -> StringsDe
    AppLang.ITALIAN -> StringsIt
    AppLang.FRENCH -> StringsFr
    AppLang.SPANISH -> StringsEs
    AppLang.RUSSIAN -> StringsRu
}
