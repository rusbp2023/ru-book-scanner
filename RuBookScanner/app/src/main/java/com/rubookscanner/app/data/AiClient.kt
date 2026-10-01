package com.rubookscanner.app.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Egy AiClient hívást intéz az adott szolgáltatóhoz (Anthropic / OpenAI / Gemini),
 * és a kapott orosz szólistából szótári alak + magyar fordítás párokat állít elő.
 */
class AiClient(private val settings: AiSettings) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json".toMediaType()

    /** Blokkoló hívás — mindig háttérszálon (Dispatchers.IO) hívd! */
    fun lookupWords(words: List<String>): List<Flashcard> {
        if (words.isEmpty()) return emptyList()
        if (settings.apiKey.isBlank()) throw IllegalStateException("Nincs megadva API kulcs a Beállításoknál.")

        val prompt = buildPrompt(words)
        val rawText = when (settings.provider) {
            AiProvider.ANTHROPIC -> callAnthropic(prompt)
            AiProvider.OPENAI -> callOpenAi(prompt)
            AiProvider.GEMINI -> callGemini(prompt)
        }
        return parseResponse(rawText, words)
    }
        /**
     * Blokkoló hívás — mindig háttérszálon (Dispatchers.IO) hívd!
     * Több kivágott szóképet küld el EGY AI-hívásban, és ugyanannyi kártyát ad vissza,
     * a képek sorrendjében.
     */
    fun lookupWordsFromImages(imagesBase64: List<String>): List<Flashcard> {
        if (imagesBase64.isEmpty()) return emptyList()
        if (settings.apiKey.isBlank()) throw IllegalStateException("Nincs megadva API kulcs a Beállításoknál.")

        val prompt = buildBatchImagePrompt(imagesBase64.size)
        val rawText = when (settings.provider) {
            AiProvider.ANTHROPIC -> callAnthropicVisionBatch(prompt, imagesBase64)
            AiProvider.OPENAI -> callOpenAiVisionBatch(prompt, imagesBase64)
            AiProvider.GEMINI -> callGeminiVisionBatch(prompt, imagesBase64)
        }
        return parseResponse(rawText, List(imagesBase64.size) { "" })
    }
    private fun buildPrompt(words: List<String>): String {
        val list = words.joinToString("\n") { "- $it" }
        return """
            A következő orosz szavak ragozott/toldalékolt alakban vannak megadva, egy weboldalról kimásolva.
            Minden szóhoz add meg:
            1. a szótári alapalakot (ige esetén infinitivus, főnév esetén egyes szám alanyeset, stb.)
            2. a legjellemzőbb magyar fordítást, röviden.

            Válaszolj KIZÁRÓLAG egy JSON tömbbel, semmi mást ne írj a válaszba (se magyarázatot, se code fence-t).
            A formátum pontosan ez legyen:
            [{"original":"...","dictionary_form":"...","translation":"..."}]

            A szavak:
            $list
        """.trimIndent()
    }

    private fun callAnthropic(prompt: String): String {
        val body = JSONObject().apply {
            put("model", settings.model.ifBlank { "claude-sonnet-4-6" })
            put("max_tokens", 2000)
            put(
                "messages",
                JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            )
        }
        val url = settings.baseUrl.ifBlank { "https://api.anthropic.com/v1/messages" }
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", settings.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException("Anthropic hiba (${resp.code}): $respBody")
            val json = JSONObject(respBody)
            val content = json.getJSONArray("content")
            val sb = StringBuilder()
            for (i in 0 until content.length()) {
                val block = content.getJSONObject(i)
                if (block.optString("type") == "text") sb.append(block.getString("text"))
            }
            return sb.toString()
        }
    }

    private fun callOpenAi(prompt: String): String {
        val body = JSONObject().apply {
            put("model", settings.model.ifBlank { "gpt-4o-mini" })
            put(
                "messages",
                JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            )
        }
        val url = settings.baseUrl.ifBlank { "https://api.openai.com/v1/chat/completions" }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${settings.apiKey}")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException("OpenAI hiba (${resp.code}): $respBody")
            val json = JSONObject(respBody)
            val choices = json.getJSONArray("choices")
            val message = choices.getJSONObject(0).getJSONObject("message")
            return message.getString("content")
        }
    }

    private fun callGemini(prompt: String): String {
        val model = settings.model.ifBlank { "gemini-2.0-flash" }
        val url = settings.baseUrl.ifBlank {
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${settings.apiKey}"
        }
        val body = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply { put("text", prompt) }))
                })
            )
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException("Gemini hiba (${resp.code}): $respBody")
            val json = JSONObject(respBody)
            val candidates = json.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            return parts.getJSONObject(0).getString("text")
        }
    }

    /** Blokkoló hívás — mindig háttérszálon (Dispatchers.IO) hívd! Egy fotóból ismeri fel + fordítja a szót. */
    fun lookupWordFromImage(imageBase64: String): Flashcard {
        if (settings.apiKey.isBlank()) throw IllegalStateException("Nincs megadva API kulcs a Beállításoknál.")

        val prompt = buildImagePrompt()
        val rawText = when (settings.provider) {
            AiProvider.ANTHROPIC -> callAnthropicVision(prompt, imageBase64)
            AiProvider.OPENAI -> callOpenAiVision(prompt, imageBase64)
            AiProvider.GEMINI -> callGeminiVision(prompt, imageBase64)
        }
        return parseSingleCardResponse(rawText)
    }

    private fun buildImagePrompt(): String = """
        Ez a kép egy nyomtatott lap fotója, aminek pontosan a közepén egy kis piros pötty van egy orosz szón.
        Azonosítsd ezt az egy orosz szót (amelyik pontosan a piros pötty alatt, vagy ahhoz a legközelebb van).
        Adj meg hozzá:
        1. a szót pontosan úgy, ahogy a lapon áll (ragozott/toldalékolt alakban)
        2. a szótári alapalakot (ige esetén infinitivus, főnév esetén egyes szám alanyeset, stb.)
        3. a legjellemzőbb magyar fordítást, röviden.

        Válaszolj KIZÁRÓLAG egy JSON objektummal, semmi mást ne írj a válaszba (se magyarázatot, se code fence-t).
        A formátum pontosan ez legyen:
        {"original":"...","dictionary_form":"...","translation":"..."}
    """.trimIndent()

    private fun callAnthropicVision(prompt: String, imageBase64: String): String {
        val content = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "image")
                put(
                    "source",
                    JSONObject().apply {
                        put("type", "base64")
                        put("media_type", "image/jpeg")
                        put("data", imageBase64)
                    }
                )
            })
            put(JSONObject().apply {
                put("type", "text")
                put("text", prompt)
            })
        }
        val body = JSONObject().apply {
            put("model", settings.model.ifBlank { "claude-sonnet-4-6" })
            put("max_tokens", 500)
            put(
                "messages",
                JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", content)
                })
            )
        }
        val url = settings.baseUrl.ifBlank { "https://api.anthropic.com/v1/messages" }
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", settings.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException("Anthropic hiba (${resp.code}): $respBody")
            val json = JSONObject(respBody)
            val contentArr = json.getJSONArray("content")
            val sb = StringBuilder()
            for (i in 0 until contentArr.length()) {
                val block = contentArr.getJSONObject(i)
                if (block.optString("type") == "text") sb.append(block.getString("text"))
            }
            return sb.toString()
        }
    }

    private fun callOpenAiVision(prompt: String, imageBase64: String): String {
        val content = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text")
                put("text", prompt)
            })
            put(JSONObject().apply {
                put("type", "image_url")
                put(
                    "image_url",
                    JSONObject().apply { put("url", "data:image/jpeg;base64,$imageBase64") }
                )
            })
        }
        val body = JSONObject().apply {
            put("model", settings.model.ifBlank { "gpt-4o-mini" })
            put(
                "messages",
                JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", content)
                })
            )
        }
        val url = settings.baseUrl.ifBlank { "https://api.openai.com/v1/chat/completions" }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${settings.apiKey}")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException("OpenAI hiba (${resp.code}): $respBody")
            val json = JSONObject(respBody)
            val choices = json.getJSONArray("choices")
            val message = choices.getJSONObject(0).getJSONObject("message")
            return message.getString("content")
        }
    }

    private fun callGeminiVision(prompt: String, imageBase64: String): String {
        val model = settings.model.ifBlank { "gemini-2.0-flash" }
        val url = settings.baseUrl.ifBlank {
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${settings.apiKey}"
        }
        val parts = JSONArray().apply {
            put(JSONObject().apply { put("text", prompt) })
            put(
                JSONObject().apply {
                    put(
                        "inline_data",
                        JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", imageBase64)
                        }
                    )
                }
            )
        }
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply { put("parts", parts) }))
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException("Gemini hiba (${resp.code}): $respBody")
            val json = JSONObject(respBody)
            val candidates = json.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val partsArr = content.getJSONArray("parts")
            return partsArr.getJSONObject(0).getString("text")
        }
    }
         private fun buildBatchImagePrompt(count: Int): String = """
        Az alábbi $count kép mindegyike egy-egy kivágott részletet mutat egy nyomtatott orosz szövegről;
        mindegyiken pontosan egy releváns orosz szó van középen.
        Minden képhez, a képek sorrendjében, add meg:
        1. a szót pontosan úgy, ahogy a képen áll (ragozott/toldalékolt alakban)
        2. a szótári alapalakot
        3. a legjellemzőbb magyar fordítást, röviden.

        Válaszolj KIZÁRÓLAG egy JSON tömbbel, pontosan $count elemmel, a képek sorrendjében, semmi mást ne írj:
        [{"original":"...","dictionary_form":"...","translation":"..."}]
    """.trimIndent()

    private fun callAnthropicVisionBatch(prompt: String, imagesBase64: List<String>): String {
        val content = JSONArray()
        imagesBase64.forEachIndexed { idx, b64 ->
            content.put(JSONObject().apply {
                put("type", "text")
                put("text", "Kép ${idx + 1}:")
            })
            content.put(JSONObject().apply {
                put("type", "image")
                put(
                    "source",
                    JSONObject().apply {
                        put("type", "base64")
                        put("media_type", "image/jpeg")
                        put("data", b64)
                    }
                )
            })
        }
        content.put(JSONObject().apply {
            put("type", "text")
            put("text", prompt)
        })
        val body = JSONObject().apply {
            put("model", settings.model.ifBlank { "claude-sonnet-4-6" })
            put("max_tokens", 300 + imagesBase64.size * 150)
            put(
                "messages",
                JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", content)
                })
            )
        }
        val url = settings.baseUrl.ifBlank { "https://api.anthropic.com/v1/messages" }
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", settings.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException("Anthropic hiba (${resp.code}): $respBody")
            val json = JSONObject(respBody)
            val contentArr = json.getJSONArray("content")
            val sb = StringBuilder()
            for (i in 0 until contentArr.length()) {
                val block = contentArr.getJSONObject(i)
                if (block.optString("type") == "text") sb.append(block.getString("text"))
            }
            return sb.toString()
        }
    }

    private fun callOpenAiVisionBatch(prompt: String, imagesBase64: List<String>): String {
        val content = JSONArray()
        content.put(JSONObject().apply {
            put("type", "text")
            put("text", prompt)
        })
        imagesBase64.forEach { b64 ->
            content.put(JSONObject().apply {
                put("type", "image_url")
                put(
                    "image_url",
                    JSONObject().apply { put("url", "data:image/jpeg;base64,$b64") }
                )
            })
        }
        val body = JSONObject().apply {
            put("model", settings.model.ifBlank { "gpt-4o-mini" })
            put(
                "messages",
                JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", content)
                })
            )
        }
        val url = settings.baseUrl.ifBlank { "https://api.openai.com/v1/chat/completions" }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${settings.apiKey}")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException("OpenAI hiba (${resp.code}): $respBody")
            val json = JSONObject(respBody)
            val choices = json.getJSONArray("choices")
            val message = choices.getJSONObject(0).getJSONObject("message")
            return message.getString("content")
        }
    }

    private fun callGeminiVisionBatch(prompt: String, imagesBase64: List<String>): String {
        val model = settings.model.ifBlank { "gemini-2.0-flash" }
        val url = settings.baseUrl.ifBlank {
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${settings.apiKey}"
        }
        val parts = JSONArray()
        parts.put(JSONObject().apply { put("text", prompt) })
        imagesBase64.forEach { b64 ->
            parts.put(
                JSONObject().apply {
                    put(
                        "inline_data",
                        JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", b64)
                        }
                    )
                }
            )
        }
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply { put("parts", parts) }))
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string() ?: ""
            if (!resp.isSuccessful) throw RuntimeException("Gemini hiba (${resp.code}): $respBody")
            val json = JSONObject(respBody)
            val candidates = json.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val partsArr = content.getJSONArray("parts")
            return partsArr.getJSONObject(0).getString("text")
        }
    }
    private fun parseResponse(raw: String, originalWords: List<String>): List<Flashcard> {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val startIdx = cleaned.indexOf('[')
        val endIdx = cleaned.lastIndexOf(']')
        if (startIdx == -1 || endIdx == -1) {
            throw RuntimeException("Nem sikerült értelmezni az AI válaszát: $cleaned")
        }
        val jsonPart = cleaned.substring(startIdx, endIdx + 1)
        val arr = JSONArray(jsonPart)
        val out = mutableListOf<Flashcard>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Flashcard(
                    id = 0L,
                    original = o.optString("original", originalWords.getOrElse(i) { "" }),
                    dictionaryForm = o.optString("dictionary_form", ""),
                    translation = o.optString("translation", "")
                )
            )
        }
        return out
    }

    private fun parseSingleCardResponse(raw: String): Flashcard {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val startIdx = cleaned.indexOf('{')
        val endIdx = cleaned.lastIndexOf('}')
        if (startIdx == -1 || endIdx == -1) {
            throw RuntimeException("Nem sikerült értelmezni az AI válaszát: $cleaned")
        }
        val obj = JSONObject(cleaned.substring(startIdx, endIdx + 1))
        return Flashcard(
            id = 0L,
            original = obj.optString("original", ""),
            dictionaryForm = obj.optString("dictionary_form", ""),
            translation = obj.optString("translation", "")
        )
    }
}
