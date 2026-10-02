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
    val original: String,
    val dictionaryForm: String,
    val translation: String,
    val known: Boolean = false,
    val deckId: Long = 0L
)

enum class AiProvider { ANTHROPIC, OPENAI, GEMINI }
enum class SourceLanguage(val label: String, val promptName: String) {
    RUSSIAN("Orosz", "orosz"),
    ENGLISH("Angol", "angol")
}
enum class AppLang(val label: String, val promptName: String) {
    HUNGARIAN("Magyar", "magyar"),
    ENGLISH("English", "angol")
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
