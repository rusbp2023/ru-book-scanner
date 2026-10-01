package com.rubookscanner.app.data

/** Egyetlen pakli tartalmát sima szöveggé alakítja. */
fun buildDeckExportText(deckName: String, cards: List<Flashcard>): String {
    val sb = StringBuilder()
    sb.append(deckName).append("\n")
    sb.append("=".repeat(deckName.length)).append("\n\n")
    if (cards.isEmpty()) {
        sb.append("(nincs még kártya ebben a paliban)\n")
    } else {
        cards.forEach { c ->
            sb.append("${c.original} — ${c.dictionaryForm} — ${c.translation}\n")
        }
    }
    return sb.toString()
}

/** Az összes pakli tartalmát egyetlen sima szöveggé alakítja, paklinként elválasztva. */
fun buildAllDecksExportText(decks: List<Deck>, allCards: List<Flashcard>): String {
    val sb = StringBuilder()
    decks.forEach { deck ->
        val cardsForDeck = allCards.filter { it.deckId == deck.id }
        sb.append(buildDeckExportText(deck.name, cardsForDeck))
        sb.append("\n")
    }
    return sb.toString()
}
