package com.rubookscanner.app.data

data class WordItem(
    val id: Long,
    val original: String
)

data class Deck(
    val id: Long,
    val name: String
)

data class Flashcard(
    val id: Long,
    val dictionaryForm: String,
    val translation: String,
    val known: Boolean = false,
    val deckId: Long = 0L
)

enum class AiProvider { ANTHROPIC, OPENAI, GEMINI }

/** A könyv nyelve. A label a nyelv saját neve, a promptName az AI-nak szóló kérésben szerepel. */
enum class SourceLanguage(val label: String, val promptName: String) {
    RUSSIAN("Русский", "orosz"),
    ENGLISH("English", "angol"),
    GERMAN("Deutsch", "német"),
    ITALIAN("Italiano", "olasz"),
    FRENCH("Français", "francia"),
    SPANISH("Español", "spanyol")
}

/** A fordítás nyelve és az app felületének nyelve. */
enum class AppLang(val label: String, val promptName: String) {
    HUNGARIAN("Magyar", "magyar"),
    ENGLISH("English", "angol"),
    GERMAN("Deutsch", "német"),
    ITALIAN("Italiano", "olasz"),
    FRENCH("Français", "francia"),
    SPANISH("Español", "spanyol"),
    RUSSIAN("Русский", "orosz")
}

data class AiSettings(
    val provider: AiProvider = AiProvider.GEMINI,
    val apiKey: String = "",
    val model: String = defaultModelFor(AiProvider.GEMINI),
    val baseUrl: String = "",
    val sourceLanguage: SourceLanguage = SourceLanguage.RUSSIAN,
    val uiLanguage: AppLang = AppLang.HUNGARIAN,
    val targetLanguage: AppLang = AppLang.HUNGARIAN
)

fun defaultModelFor(provider: AiProvider): String = when (provider) {
    AiProvider.ANTHROPIC -> "claude-sonnet-4-6"
    AiProvider.OPENAI -> "gpt-4o-mini"
    AiProvider.GEMINI -> "gemini-3.1-flash-lite"
}
