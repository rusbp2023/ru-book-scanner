package com.rubookscanner.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.rubookscanner.app.data.Store
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Átlátszó, felület nélküli Activity: a megosztott szót a szólistába teszi. */
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
            var t: Strings = StringsHu
            try {
                t = stringsFor(store.settingsFlow.first().uiLanguage)
                store.addWord(word)
                toast(t.wordAddedToList(word))
            } catch (e: Exception) {
                toast(t.errorPrefix(e.message))
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
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
}
