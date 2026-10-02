package com.rubookscanner.app.data

/** Egyetlen pakli tartalmát sima szöveggé alakítja. */
fun buildDeckExportText(deckName: String, cards: List<Flashcard>, emptyText: String): String {
    val sb = StringBuilder()
    sb.append(deckName).append("\n")
    sb.append("=".repeat(deckName.length)).append("\n\n")
    if (cards.isEmpty()) {
        sb.append(emptyText).append("\n")
    } else {
        cards.forEach { c ->
            sb.append("${c.dictionaryForm} — ${c.translation}\n")
        }
    }
    return sb.toString()
}

/** Az összes pakli tartalmát egyetlen sima szöveggé alakítja, paklinként elválasztva. */
fun buildAllDecksExportText(decks: List<Deck>, allCards: List<Flashcard>, emptyText: String): String {
    val sb = StringBuilder()
    decks.forEach { deck ->
        val cardsForDeck = allCards.filter { it.deckId == deck.id }
        sb.append(buildDeckExportText(deck.name, cardsForDeck, emptyText))
        sb.append("\n")
    }
    return sb.toString()
}
