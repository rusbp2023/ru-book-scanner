package com.rubookscanner.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rubookscanner.app.data.AiProvider
import com.rubookscanner.app.data.AiSettings
import com.rubookscanner.app.data.AppLang
import com.rubookscanner.app.data.Flashcard
import com.rubookscanner.app.data.SourceLanguage
import com.rubookscanner.app.data.WordItem
import com.rubookscanner.app.data.defaultModelFor
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

@Composable
fun WordListScreen(
    words: List<WordItem>,
    loading: Boolean,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit,
    onAddManual: (String) -> Unit,
    onGenerate: () -> Unit
) {
    val t = LocalStrings.current
    var manualText by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(t.wordsHint, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = manualText,
                onValueChange = { manualText = it },
                label = { Text(t.addWordManually) },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                onAddManual(manualText)
                manualText = ""
            }) { Text("+") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(words, key = { it.id }) { w ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(w.original, style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { onDelete(w.id) }) {
                        Text("✕")
                    }
                }
                Divider()
            }
        }
        Spacer(Modifier.height(12.dp))
        Row {
            OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                Text(t.clearList)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onGenerate,
                enabled = words.isNotEmpty() && !loading,
                modifier = Modifier.weight(1f)
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(t.generateCards(words.size))
                }
            }
        }
    }
}

@Composable
fun FlashcardScreen(
    cards: List<Flashcard>,
    onDelete: (Long) -> Unit,
    onToggleKnown: (Flashcard) -> Unit
) {
    val t = LocalStrings.current

    if (cards.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                t.noCardsYet,
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    var index by remember { mutableStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val safeIndex = index.coerceIn(0, cards.size - 1)
    val card = cards[safeIndex]

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("${safeIndex + 1} / ${cards.size}", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(16.dp))
                Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            onClick = { flipped = !flipped },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF16263A),
                contentColor = Color(0xFFEAF2FB)
            )
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!flipped) {
                        Text(card.dictionaryForm, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(t.originalLabel(card.original), style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(card.translation, style = MaterialTheme.typography.headlineMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (flipped) t.tapBack else t.tapForTranslation,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { showDeleteDialog = true }) { Text(t.delete) }
            OutlinedButton(onClick = { onToggleKnown(card) }) {
                Text(if (card.known) t.known else t.markKnown)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = {
                flipped = false
                index = (safeIndex - 1 + cards.size) % cards.size
            }) { Text(t.previous) }
            Button(onClick = {
                flipped = false
                index = (safeIndex + 1) % cards.size
            }) { Text(t.next) }
        }
    }
        if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(t.confirmDeleteCard) },
            text = { Text(card.dictionaryForm) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete(card.id)
                    flipped = false
                }) { Text(t.yesDelete) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(t.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    selectedText: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AiSettings,
    onSave: (AiSettings) -> Unit
) {
    val t = LocalStrings.current
    var provider by remember(settings) { mutableStateOf(settings.provider) }
    var apiKey by remember(settings) { mutableStateOf(settings.apiKey) }
    var model by remember(settings) { mutableStateOf(settings.model) }
    var baseUrl by remember(settings) { mutableStateOf(settings.baseUrl) }
    var sourceLanguage by remember(settings) { mutableStateOf(settings.sourceLanguage) }
    var targetLanguage by remember(settings) { mutableStateOf(settings.targetLanguage) }
    var uiLanguage by remember(settings) { mutableStateOf(settings.uiLanguage) }

    val srcLabel: (SourceLanguage) -> String = {
        if (it == SourceLanguage.RUSSIAN) t.langRussian else t.langEnglish
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(t.languagesTitle, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = t.bookLanguageLabel,
            selectedText = srcLabel(sourceLanguage),
            options = SourceLanguage.entries,
            optionLabel = srcLabel,
            onSelect = { sourceLanguage = it }
        )
        Spacer(Modifier.height(12.dp))
        DropdownField(
            label = t.translationLanguageLabel,
            selectedText = targetLanguage.label,
            options = AppLang.entries,
            optionLabel = { it.label },
            onSelect = { targetLanguage = it }
        )
        Spacer(Modifier.height(12.dp))
        DropdownField(
            label = t.appLanguageLabel,
            selectedText = uiLanguage.label,
            options = AppLang.entries,
            optionLabel = { it.label },
            onSelect = { uiLanguage = it }
        )
        Spacer(Modifier.height(20.dp))

        Text(t.aiProviderTitle, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        DropdownField(
            label = t.providerLabel,
            selectedText = provider.name,
            options = AiProvider.entries,
            optionLabel = { it.name },
            onSelect = {
                provider = it
                model = defaultModelFor(it)
            }
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(t.apiKeyLabel) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text(t.modelLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text(t.baseUrlLabel) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                onSave(
                    AiSettings(
                        provider = provider,
                        apiKey = apiKey,
                        model = model,
                        baseUrl = baseUrl,
                        sourceLanguage = sourceLanguage,
                        uiLanguage = uiLanguage,
                        targetLanguage = targetLanguage
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(t.save)
        }
        Spacer(Modifier.height(20.dp))
        Text(t.settingsTip, style = MaterialTheme.typography.bodySmall)
    }
}
