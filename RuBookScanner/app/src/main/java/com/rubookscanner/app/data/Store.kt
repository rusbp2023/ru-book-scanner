package com.rubookscanner.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore by preferencesDataStore(name = "ru_flashcards")

private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    try {
        if (name == null) default else enumValueOf<T>(name)
    } catch (e: Exception) {
        default
    }

private object Keys {
    val WORDS = stringPreferencesKey("words_json")
    val FLASHCARDS = stringPreferencesKey("flashcards_json")
    val DECKS = stringPreferencesKey("decks_json")
    val ACTIVE_DECK_ID = stringPreferencesKey("active_deck_id")
    val SHUFFLE = stringPreferencesKey("shuffle_orders_json")
    val PROVIDER = stringPreferencesKey("provider")
    val API_KEY = stringPreferencesKey("api_key")
    val MODEL = stringPreferencesKey("model")
    val BASE_URL = stringPreferencesKey("base_url")
    val SOURCE_LANGUAGE = stringPreferencesKey("source_language")
    val UI_LANGUAGE = stringPreferencesKey("ui_language")
    val TARGET_LANGUAGE = stringPreferencesKey("target_language")
    val HAND_MODE = stringPreferencesKey("hand_mode")
    val CARD_FRONT = stringPreferencesKey("card_front")
    val SHOW_KNOWN = stringPreferencesKey("show_known")
    val SHOW_INFO = stringPreferencesKey("show_info")
    val BUTTON_X = stringPreferencesKey("button_x")
    val BUTTON_Y = stringPreferencesKey("button_y")
    val SHOW_STRESS = stringPreferencesKey("show_stress")
    val EXPORT_STRESS = stringPreferencesKey("export_stress")
    val EXPORT_ORDER = stringPreferencesKey("export_order")
}

class Store(private val context: Context) {

    val wordsFlow: Flow<List<WordItem>> = context.dataStore.data.map { prefs ->
        parseWords(prefs[Keys.WORDS] ?: "[]")
    }

    val flashcardsFlow: Flow<List<Flashcard>> = context.dataStore.data.map { prefs ->
        parseFlashcards(prefs[Keys.FLASHCARDS] ?: "[]")
    }

    val decksFlow: Flow<List<Deck>> = context.dataStore.data.map { prefs ->
        parseDecks(prefs[Keys.DECKS] ?: "[]")
    }

    val activeDeckIdFlow: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_DECK_ID]?.toLongOrNull()
    }
        val shuffleOrdersFlow: Flow<Map<Long, List<Long>>> = context.dataStore.data.map { prefs ->
        parseShuffle(prefs[Keys.SHUFFLE] ?: "{}")
    }

    val settingsFlow: Flow<AiSettings> = context.dataStore.data.map { prefs ->
        val provider = try {
            AiProvider.valueOf(prefs[Keys.PROVIDER] ?: "GEMINI")
        } catch (e: Exception) {
            AiProvider.GEMINI
        }
        // every provider keeps its own key / model / endpoint; the old single values belong to the saved provider
        fun cfgFor(p: AiProvider): ProviderConfig {
            val legacy = p == provider
            return ProviderConfig(
                apiKey = prefs[stringPreferencesKey("api_key_" + p.name)]
                    ?: (if (legacy) (prefs[Keys.API_KEY] ?: "") else ""),
                model = prefs[stringPreferencesKey("model_" + p.name)]
                    ?: (if (legacy) (prefs[Keys.MODEL] ?: defaultModelFor(p)) else defaultModelFor(p)),
                baseUrl = prefs[stringPreferencesKey("base_url_" + p.name)]
                    ?: (if (legacy) (prefs[Keys.BASE_URL] ?: "") else "")
            )
        }
        val configs = AiProvider.entries.associateWith { cfgFor(it) }
        val active = configs.getValue(provider)
        AiSettings(
            provider = provider,
            apiKey = active.apiKey,
            model = active.model,
            baseUrl = active.baseUrl,
            providerConfigs = configs,
            sourceLanguage = enumOrDefault(prefs[Keys.SOURCE_LANGUAGE], SourceLanguage.RUSSIAN),
            uiLanguage = enumOrDefault(prefs[Keys.UI_LANGUAGE], AppLang.HUNGARIAN),
            targetLanguage = enumOrDefault(prefs[Keys.TARGET_LANGUAGE], AppLang.HUNGARIAN),
            buttonX = prefs[Keys.BUTTON_X]?.toFloatOrNull()
                ?: when (prefs[Keys.HAND_MODE]) { "RIGHT" -> 1f; "LEFT" -> 0f; else -> 0.5f },
            buttonY = prefs[Keys.BUTTON_Y]?.toFloatOrNull()
                ?: when (prefs[Keys.HAND_MODE]) { "RIGHT", "LEFT" -> 0f; else -> 1f },
            cardFront = enumOrDefault(prefs[Keys.CARD_FRONT], CardFront.TRANSLATION),
            showKnown = prefs[Keys.SHOW_KNOWN]?.toBooleanStrictOrNull() ?: true,
            showInfo = prefs[Keys.SHOW_INFO]?.toBooleanStrictOrNull() ?: true,
            showStress = prefs[Keys.SHOW_STRESS]?.toBooleanStrictOrNull() ?: true,
            exportStress = prefs[Keys.EXPORT_STRESS]?.toBooleanStrictOrNull() ?: false,
            exportTranslationFirst = prefs[Keys.EXPORT_ORDER]?.toBooleanStrictOrNull() ?: true
        )
    }

    /** Ha még nincs egyetlen pakli sem, létrehoz egy "Alap" nevűt, és aktívvá teszi. */
    suspend fun ensureDefaultDeck() {
        context.dataStore.edit { prefs ->
            val decks = parseDecks(prefs[Keys.DECKS] ?: "[]")
            if (decks.isEmpty()) {
                val newDeck = Deck(1L, "Alap")
                prefs[Keys.DECKS] = serializeDecks(listOf(newDeck))
                prefs[Keys.ACTIVE_DECK_ID] = newDeck.id.toString()
            }
        }
    }

    suspend fun createDeck(name: String): Long {
        var newId = 0L
        context.dataStore.edit { prefs ->
            val decks = parseDecks(prefs[Keys.DECKS] ?: "[]").toMutableList()
            newId = (decks.maxOfOrNull { it.id } ?: 0L) + 1
            decks.add(Deck(newId, name.ifBlank { "Névtelen pakli" }))
            prefs[Keys.DECKS] = serializeDecks(decks)
            prefs[Keys.ACTIVE_DECK_ID] = newId.toString()
        }
        return newId
    }

    /**
     * Feltöltött paklik létrehozása. Mindig új paklik jönnek létre (azonos név esetén is),
     * a meglévőket nem érinti. Visszaadja a létrehozott kártyák számát.
     */
    suspend fun importDecks(parsed: List<ParsedDeck>, fallbackName: String): Int {
        var cardTotal = 0
        context.dataStore.edit { prefs ->
            val decks = parseDecks(prefs[Keys.DECKS] ?: "[]").toMutableList()
            val cards = parseFlashcards(prefs[Keys.FLASHCARDS] ?: "[]").toMutableList()
            var nextDeckId = (decks.maxOfOrNull { it.id } ?: 0L) + 1
            var nextCardId = (cards.maxOfOrNull { it.id } ?: 0L) + 1
            parsed.forEach { p ->
                val deckName = p.name.ifBlank { fallbackName }.ifBlank { "Névtelen pakli" }
                val deck = Deck(nextDeckId, deckName)
                nextDeckId++
                decks.add(deck)
                p.cards.forEach { pair ->
                    cards.add(
                        Flashcard(
                            id = nextCardId,
                            dictionaryForm = pair.first.replace("\u0301", ""),
                            stressedForm = if (pair.first.contains("\u0301")) pair.first else "",
                            translation = pair.second,
                            deckId = deck.id
                        )
                    )
                    nextCardId++
                    cardTotal++
                }
            }
            prefs[Keys.DECKS] = serializeDecks(decks)
            prefs[Keys.FLASHCARDS] = serializeFlashcards(cards)
        }
        return cardTotal
    }
    suspend fun shuffleDeck(deckId: Long) {
        context.dataStore.edit { prefs ->
            val ids = parseFlashcards(prefs[Keys.FLASHCARDS] ?: "[]")
                .filter { it.deckId == deckId }
                .map { it.id }
                .shuffled()
            val map = parseShuffle(prefs[Keys.SHUFFLE] ?: "{}").toMutableMap()
            map[deckId] = ids
            prefs[Keys.SHUFFLE] = serializeShuffle(map)
        }
    }

    suspend fun resetDeckOrder(deckId: Long) {
        context.dataStore.edit { prefs ->
            val map = parseShuffle(prefs[Keys.SHUFFLE] ?: "{}").toMutableMap()
            map.remove(deckId)
            prefs[Keys.SHUFFLE] = serializeShuffle(map)
        }
    }
    suspend fun setActiveDeck(id: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.ACTIVE_DECK_ID] = id.toString() }
    }

    suspend fun renameDeck(id: Long, newName: String) {
        context.dataStore.edit { prefs ->
            val decks = parseDecks(prefs[Keys.DECKS] ?: "[]").map {
                if (it.id == id) it.copy(name = newName) else it
            }
            prefs[Keys.DECKS] = serializeDecks(decks)
        }
    }

    suspend fun deleteDeck(id: Long) {
        context.dataStore.edit { prefs ->
            val decks = parseDecks(prefs[Keys.DECKS] ?: "[]").filterNot { it.id == id }
            prefs[Keys.DECKS] = serializeDecks(decks)
            val cards = parseFlashcards(prefs[Keys.FLASHCARDS] ?: "[]").filterNot { it.deckId == id }
            prefs[Keys.FLASHCARDS] = serializeFlashcards(cards)
                        val shuffleMap = parseShuffle(prefs[Keys.SHUFFLE] ?: "{}").toMutableMap()
            shuffleMap.remove(id)
            prefs[Keys.SHUFFLE] = serializeShuffle(shuffleMap)
            if (prefs[Keys.ACTIVE_DECK_ID]?.toLongOrNull() == id) {
                prefs[Keys.ACTIVE_DECK_ID] = decks.firstOrNull()?.id?.toString() ?: ""
            }
        }
    }

    suspend fun addWord(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        context.dataStore.edit { prefs ->
            val list = parseWords(prefs[Keys.WORDS] ?: "[]").toMutableList()
            val newId = (list.maxOfOrNull { it.id } ?: 0L) + 1
            list.add(WordItem(newId, trimmed))
            prefs[Keys.WORDS] = serializeWords(list)
        }
    }

    suspend fun removeWord(id: Long) {
        context.dataStore.edit { prefs ->
            val list = parseWords(prefs[Keys.WORDS] ?: "[]").filterNot { it.id == id }
            prefs[Keys.WORDS] = serializeWords(list)
        }
    }

    suspend fun clearWords() {
        context.dataStore.edit { prefs -> prefs[Keys.WORDS] = "[]" }
    }

    suspend fun getWordsOnce(): List<WordItem> =
        parseWords(context.dataStore.data.first()[Keys.WORDS] ?: "[]")

    suspend fun addFlashcards(cards: List<Flashcard>, deckId: Long) {
        context.dataStore.edit { prefs ->
            val existing = parseFlashcards(prefs[Keys.FLASHCARDS] ?: "[]").toMutableList()
            var nextId = (existing.maxOfOrNull { it.id } ?: 0L) + 1
            cards.forEach { c ->
                existing.add(c.copy(id = nextId, deckId = deckId))
                nextId++
            }
            prefs[Keys.FLASHCARDS] = serializeFlashcards(existing)
        }
    }

    suspend fun updateFlashcard(card: Flashcard) {
        context.dataStore.edit { prefs ->
            val list = parseFlashcards(prefs[Keys.FLASHCARDS] ?: "[]").map {
                if (it.id == card.id) card else it
            }
            prefs[Keys.FLASHCARDS] = serializeFlashcards(list)
        }
    }

    suspend fun deleteFlashcard(id: Long) {
        context.dataStore.edit { prefs ->
            val list = parseFlashcards(prefs[Keys.FLASHCARDS] ?: "[]").filterNot { it.id == id }
            prefs[Keys.FLASHCARDS] = serializeFlashcards(list)
        }
    }

    suspend fun saveSettings(settings: AiSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROVIDER] = settings.provider.name
            AiProvider.entries.forEach { p ->
                val cfg = if (p == settings.provider) {
                    ProviderConfig(settings.apiKey, settings.model, settings.baseUrl)
                } else {
                    settings.providerConfigs[p] ?: ProviderConfig("", defaultModelFor(p), "")
                }
                prefs[stringPreferencesKey("api_key_" + p.name)] = cfg.apiKey
                prefs[stringPreferencesKey("model_" + p.name)] = cfg.model
                prefs[stringPreferencesKey("base_url_" + p.name)] = cfg.baseUrl
            }
            prefs[Keys.SOURCE_LANGUAGE] = settings.sourceLanguage.name
            prefs[Keys.UI_LANGUAGE] = settings.uiLanguage.name
            prefs[Keys.TARGET_LANGUAGE] = settings.targetLanguage.name
            prefs[Keys.BUTTON_X] = settings.buttonX.toString()
            prefs[Keys.BUTTON_Y] = settings.buttonY.toString()
            prefs[Keys.CARD_FRONT] = settings.cardFront.name
            prefs[Keys.SHOW_KNOWN] = settings.showKnown.toString()
            prefs[Keys.SHOW_INFO] = settings.showInfo.toString()
            prefs[Keys.SHOW_STRESS] = settings.showStress.toString()
            prefs[Keys.EXPORT_STRESS] = settings.exportStress.toString()
            prefs[Keys.EXPORT_ORDER] = settings.exportTranslationFirst.toString()
        }
    }
    private fun parseShuffle(json: String): Map<Long, List<Long>> {
        val obj = JSONObject(json)
        val out = mutableMapOf<Long, List<Long>>()
        obj.keys().forEach { key ->
            val arr = obj.getJSONArray(key)
            val ids = mutableListOf<Long>()
            for (i in 0 until arr.length()) ids.add(arr.getLong(i))
            key.toLongOrNull()?.let { out[it] = ids }
        }
        return out
    }

    private fun serializeShuffle(map: Map<Long, List<Long>>): String {
        val obj = JSONObject()
        map.forEach { (deckId, ids) ->
            val arr = JSONArray()
            ids.forEach { arr.put(it) }
            obj.put(deckId.toString(), arr)
        }
        return obj.toString()
    }
    private fun parseWords(json: String): List<WordItem> {
        val arr = JSONArray(json)
        val out = mutableListOf<WordItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(WordItem(o.getLong("id"), o.getString("original")))
        }
        return out
    }

    private fun serializeWords(list: List<WordItem>): String {
        val arr = JSONArray()
        list.forEach { w ->
            arr.put(JSONObject().apply {
                put("id", w.id)
                put("original", w.original)
            })
        }
        return arr.toString()
    }

    private fun parseDecks(json: String): List<Deck> {
        val arr = JSONArray(json)
        val out = mutableListOf<Deck>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(Deck(o.getLong("id"), o.getString("name")))
        }
        return out
    }

    private fun serializeDecks(list: List<Deck>): String {
        val arr = JSONArray()
        list.forEach { d ->
            arr.put(JSONObject().apply {
                put("id", d.id)
                put("name", d.name)
            })
        }
        return arr.toString()
    }

    private fun parseFlashcards(json: String): List<Flashcard> {
        val arr = JSONArray(json)
        val out = mutableListOf<Flashcard>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Flashcard(
                    id = o.getLong("id"),
                    deckId = o.optLong("deckId", 0L),
                    dictionaryForm = o.getString("dictionaryForm"),
                    translation = o.getString("translation"),
                    known = o.optBoolean("known", false),
                    stressedForm = o.optString("stressedForm", "")
                )
            )
        }
        return out
    }

    private fun serializeFlashcards(list: List<Flashcard>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("deckId", c.deckId)
                put("dictionaryForm", c.dictionaryForm)
                put("translation", c.translation)
                put("known", c.known)
                put("stressedForm", c.stressedForm)
            })
        }
        return arr.toString()
    }
}
