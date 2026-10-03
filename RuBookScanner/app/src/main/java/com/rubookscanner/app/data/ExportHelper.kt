package com.rubookscanner.app.data

/** Egyetlen pakli tartalmát sima szöveggé alakítja. */
fun buildDeckExportText(deckName: String, cards: List<Flashcard>, emptyText: String): String {
    val sb = StringBuilder()
    sb.append(deckName).append("\n")
    sb.append("=".repeat(deckName.length)).append("\n\n")
    if (cards.isEmpty()) {
        sb.append(emptyText).append("\n")
    } else {
        val width = cards.maxOf { it.dictionaryForm.length }.coerceAtMost(40)
        cards.forEach { c ->
            val pad = (width - c.dictionaryForm.length).coerceAtLeast(0) + 10
            sb.append(c.dictionaryForm).append(" ".repeat(pad)).append(c.translation).append("\n")
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

/** Egy beolvasott pakli: név + (szótári alak, fordítás) párok. */
data class ParsedDeck(val name: String, val cards: List<Pair<String, String>>)

private fun isUnderline(line: String): Boolean {
    val s = line.trim()
    return s.isNotEmpty() && s.all { it == '=' }
}

private val columnSeparator = Regex("\\t+| {2,}")

private fun parseCardLine(line: String): Pair<String, String>? {
    if (line.isEmpty()) return null

    // Új formátum: a két szót tabulátor vagy legalább két szóköz választja el.
    val m = columnSeparator.find(line)
    if (m != null && m.range.first > 0) {
        val first = line.substring(0, m.range.first).trim()
        val second = line.substring(m.range.last + 1).trim()
        if (first.isNotEmpty() && second.isNotEmpty()) return first to second
    }

    // Régi formátum (gondolatjeles) is beolvasható marad.
    val separators = listOf(" — ", " – ", " - ")
    for (sep in separators) {
        val idx = line.indexOf(sep)
        if (idx > 0) {
            val first = line.substring(0, idx).trim()
            val second = line.substring(idx + sep.length).trim()
            if (first.isNotEmpty() && second.isNotEmpty()) return first to second
        }
    }
    return null
}

/**
 * A letöltött .txt formátum visszaolvasása. Egy pakli neve az a sor, amit "====" vonal követ.
 * Egy- vagy többpaklis fájl is jó. Ha nincs pakli-fejléc, egyetlen névtelen paklit ad vissza.
 */
fun parseDecksFromText(text: String): List<ParsedDeck> {
    val lines = text.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n').split("\n")
    val decks = mutableListOf<ParsedDeck>()
    var currentName = ""
    var currentCards = mutableListOf<Pair<String, String>>()
    var inDeck = false
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        val nextIsUnderline = i + 1 < lines.size && isUnderline(lines[i + 1])
        if (line.isNotEmpty() && nextIsUnderline) {
            if (inDeck || currentCards.isNotEmpty()) decks.add(ParsedDeck(currentName, currentCards))
            currentName = line
            currentCards = mutableListOf()
            inDeck = true
            i += 2
            continue
        }
        parseCardLine(line)?.let { currentCards.add(it) }
        i++
    }
    if (inDeck || currentCards.isNotEmpty()) decks.add(ParsedDeck(currentName, currentCards))
    return decks
}
