package com.rubookscanner.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.rubookscanner.app.data.AiClient
import com.rubookscanner.app.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Átlátszó, felület nélküli Activity: a megosztott szót lefordítja, és az aktív paklihoz adja. */
class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val word = extractText(intent)?.trim()?.take(200)
        if (word.isNullOrBlank()) {
            finish()
            return
        }

        val store = Store(applicationContext)
        lifecycleScope.launch {
            try {
                val settings = store.settingsFlow.first()
                val deckId = store.activeDeckIdFlow.first()

                if (settings.apiKey.isBlank()) {
                    toast("Előbb add meg az API kulcsot a Beállításoknál!")
                } else if (deckId == null) {
                    toast("Nincs aktív pakli — hozz létre egyet a Paklik fülön!")
                } else {
                    toast("Fordítás: $word …")
                    val cards = withContext(Dispatchers.IO) {
                        AiClient(settings).lookupWords(listOf(word))
                    }
                    store.addFlashcards(cards, deckId)
                    toast("${cards.size} kártya hozzáadva")
                }
            } catch (e: Exception) {
                toast("Hiba: ${e.message}")
            } finally {
                finish()
            }
        }
    }

    private fun extractText(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        else -> null
    }

    private fun toast(msg: String) =
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
}
