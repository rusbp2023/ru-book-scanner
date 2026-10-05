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
        "Jelölj ki egy szót a Chrome-ban a weboldalon, majd Megosztás → ez az app. " +
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
        "Select a word on a web page in Chrome, then Share → this app. " +
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
        "Markiere in Chrome auf einer Webseite ein Wort und wähle dann Teilen → diese App. " +
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
        "Seleziona una parola in una pagina web in Chrome, poi Condividi → questa app. " +
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
        "Sélectionne un mot sur une page web dans Chrome, puis Partager → cette application. " +
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
        "Selecciona una palabra en una página web en Chrome y luego Compartir → esta app. " +
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
        "Выдели слово на веб-странице в Chrome, затем «Поделиться» → это приложение. " +
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
