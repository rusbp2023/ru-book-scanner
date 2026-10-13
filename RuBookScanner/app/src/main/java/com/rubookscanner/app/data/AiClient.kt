package com.rubookscanner.app.data

import com.rubookscanner.app.Strings
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

/** Az AI-hívások hibatípusai. Mindegyikhez külön, lefordított üzenet tartozik (Strings.err...). */
enum class AiErrorKind {
    NO_KEY,
    NO_INTERNET,
    TIMEOUT,
    BAD_KEY,
    MODEL_NOT_FOUND,
    RATE_LIMIT,
    OVERLOADED,
    REJECTED,
    EMPTY,
    BAD_RESPONSE,
    OTHER
}

/** Az AI-hívás hibája: a típus dönti el az üzenetet, a detail a rövid technikai részlet (pl. "HTTP 429"). */
class AiException(
    val kind: AiErrorKind,
    val detail: String? = null,
    val provider: AiProvider? = null,
    cause: Throwable? = null
) : RuntimeException(detail ?: kind.name, cause)

/** A felhasználónak mutatott hiba: érthető üzenet + kis betűs technikai részlet (lehet null). */
data class ErrorInfo(val message: String, val detail: String?)

/** Bármilyen kivételből felhasználóbarát üzenetet és technikai részletet készít, az app nyelvén. */
fun describeError(t: Strings, e: Throwable): ErrorInfo {
    if (e is AiException) {
        val msg = when (e.kind) {
            AiErrorKind.NO_KEY -> t.enterApiKeyFirst
            AiErrorKind.NO_INTERNET -> t.errNoInternet
            AiErrorKind.TIMEOUT -> t.errTimeout
            AiErrorKind.BAD_KEY -> t.errBadKey
            AiErrorKind.MODEL_NOT_FOUND -> t.errModelNotFound
            AiErrorKind.RATE_LIMIT ->
                t.errRateLimit + (if (e.provider == AiProvider.GEMINI) " " + t.errRateLimitGeminiNote else "")
            AiErrorKind.OVERLOADED -> t.errOverloaded
            AiErrorKind.REJECTED -> t.errRejected
            AiErrorKind.EMPTY -> t.errEmpty
            AiErrorKind.BAD_RESPONSE -> t.errBadResponse
            AiErrorKind.OTHER -> t.errOther
        }
        return ErrorInfo(msg, e.detail)
    }
    val detail = e.javaClass.simpleName + (e.message?.let { ": " + it.take(120) } ?: "")
    return ErrorInfo(t.errOther, detail)
}

/**
 * Egy AiClient hívást intéz az adott szolgáltatóhoz (Anthropic / OpenAI / Gemini),
 * és a kapott szólistából / képekből szótári alak + fordítás párokat állít elő.
 */
class AiClient(private val settings: AiSettings) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json".toMediaType()

    private fun requireKey() {
        if (settings.apiKey.isBlank()) throw AiException(AiErrorKind.NO_KEY, null, settings.provider)
    }

    /** Blokkoló hívás — mindig háttérszálon (Dispatchers.IO) hívd! */
    fun lookupWords(words: List<String>): List<Flashcard> {
        if (words.isEmpty()) return emptyList()
        requireKey()
        val prompt = buildPrompt(words)
        val rawText = callText(prompt, (400 + words.size * 120).coerceIn(2000, 8000))
        return parseResponse(rawText)
    }

    /**
     * Blokkoló hívás — mindig háttérszálon (Dispatchers.IO) hívd!
     * Több kivágott szóképet küld el EGY AI-hívásban, és ugyanannyi kártyát ad vissza,
     * a képek sorrendjében.
     */
    fun lookupWordsFromImages(imagesBase64: List<String>, phrases: List<Boolean> = emptyList()): List<Flashcard> {
        if (imagesBase64.isEmpty()) return emptyList()
        requireKey()
        val prompt = buildBatchImagePrompt(imagesBase64.size, phrases)
        val rawText = callVision(prompt, imagesBase64, true)
        return parseResponse(rawText)
    }

    /** Blokkoló hívás (Dispatchers.IO-n hívd): egy mini kérést küld, hogy kiderüljön, jó-e a kulcs. */
    fun testConnection() {
        requireKey()
        val raw = callText("Reply with exactly one word: OK", 100)
        if (raw.isBlank()) throw AiException(AiErrorKind.EMPTY, "empty reply", settings.provider)
    }

    /** Blokkoló hívás — mindig háttérszálon (Dispatchers.IO) hívd! Egy fotóból ismeri fel + fordítja a szót. */
    fun lookupWordFromImage(imageBase64: String): Flashcard {
        requireKey()
        val prompt = buildImagePrompt()
        val rawText = callVision(prompt, listOf(imageBase64), false)
        return parseSingleCardResponse(rawText)
    }

    // ---------------------------------------------------------------- promptok

    /** Orosz forrásnyelvnél a hangsúlyos alakot is keressük (kombináló ékezetjellel). */
    private fun isRussian() = settings.sourceLanguage == SourceLanguage.RUSSIAN

    private fun jsonItem(): String =
        if (isRussian()) {
            """{"original":"...","dictionary_form":"...","stressed_form":"...","translation":"..."}"""
        } else {
            """{"original":"...","dictionary_form":"...","translation":"..."}"""
        }

    private fun stressNote(): String =
        if (isRussian()) {
            "Ezen felül minden elemhez add meg a \"stressed_form\" mezőt is: ez a szótári alapalak hangsúlyos változata, a hangsúlyos magánhangzó után a kombináló ékezetjellel (U+0301), például до́м. Egyszótagú szónál és ё betűs szónál ne tegyél jelet, ilyenkor a stressed_form legyen azonos a dictionary_form-mal. "
        } else {
            ""
        }

    private fun buildPrompt(words: List<String>): String {
        val list = words.joinToString("\n") { "- $it" }
        return """
            A következő ${settings.sourceLanguage.promptName} szavak (esetleg ragozott alakban) vannak megadva, egy weboldalról kimásolva.
            Minden szóhoz add meg:
            1. a szótári alapalakot (ige esetén infinitivus, főnév esetén egyes szám alanyeset, stb.). A szótári alakot a szótárban szereplő írásmóddal add meg: kisbetűvel, kivéve ha a szó a szótár szerint is nagybetűs (például tulajdonnév). Ha a szó csak mondat eleje miatt vagy más okból van nagybetűvel, a szótári alak legyen kisbetűs.
            2. a szótári alapalak legjellemzőbb ${settings.targetLanguage.promptName} fordítását, röviden, ugyancsak szótári alakban (ige: főnévi igenév, főnév: egyes szám alanyeset). NEM a ragozott alak fordítását kérem.

            ${stressNote()}Válaszolj KIZÁRÓLAG egy JSON tömbbel, semmi mást ne írj a válaszba (se magyarázatot, se code fence-t).
            A formátum pontosan ez legyen:
            [${jsonItem()}]

            A szavak:
            $list
        """.trimIndent()
    }

    private fun buildImagePrompt(): String = """
        Ez a kép egy nyomtatott lap fotója, aminek pontosan a közepén egy kis piros pötty van egy orosz szón.
        Azonosítsd ezt az egy orosz szót (amelyik pontosan a piros pötty alatt, vagy ahhoz a legközelebb van).
        Adj meg hozzá:
        1. a szót pontosan úgy, ahogy a lapon áll (ragozott/toldalékolt alakban)
        2. a szótári alapalakot (ige esetén infinitivus, főnév esetén egyes szám alanyeset, stb.)
        3. a szótári alapalak legjellemzőbb ${settings.targetLanguage.promptName} fordítását, röviden, ugyancsak szótári alakban (ige: főnévi igenév, főnév: egyes szám alanyeset). NEM a ragozott alak fordítását kérem.

        ${stressNote()}Válaszolj KIZÁRÓLAG egy JSON objektummal, semmi mást ne írj a válaszba (se magyarázatot, se code fence-t).
        A formátum pontosan ez legyen:
        ${jsonItem()}
    """.trimIndent()

    /** A kifejezést tartalmazó képek sorszámai (1-től) az AI-nak szóló kérésben. */
    private fun phraseNote(phrases: List<Boolean>): String {
        val idx = phrases.withIndex().filter { it.value }.map { it.index + 1 }
        if (idx.isEmpty()) return ""
        return "Kivétel: a(z) ${idx.joinToString(", ")}. képen nem egyetlen szó, hanem egy többszavas kifejezés van, szorosan körbevágva. " +
            "Ezeknél a képen látható teljes kifejezést olvasd ki: a \"dictionary_form\" a kifejezés alapalakja legyen (ha nincs benne ragozott szó, változatlanul, a szótárban szereplő írásmóddal), a \"translation\" a teljes kifejezés rövid, természetes ${settings.targetLanguage.promptName} fordítása, a \"stressed_form\" pedig üres string. "
    }

    private fun buildBatchImagePrompt(count: Int, phrases: List<Boolean> = emptyList()): String = """
        Az alábbi $count kép mindegyike egy-egy kivágott részletet mutat egy nyomtatott ${settings.sourceLanguage.promptName} szövegről;
        mindegyiken pontosan egy releváns ${settings.sourceLanguage.promptName} szó van középen.
        Minden képhez, a képek sorrendjében, add meg:
        1. a szót pontosan úgy, ahogy a képen áll (ragozott/toldalékolt alakban)
        2. a szótári alapalakot, a szótárban szereplő írásmóddal: kisbetűvel, kivéve ha a szó a szótár szerint is nagybetűs (például tulajdonnév). Ha a szó csak mondat eleje miatt vagy más okból van nagybetűvel, a szótári alak legyen kisbetűs.
        3. a szótári alapalak legjellemzőbb ${settings.targetLanguage.promptName} fordítását, röviden, ugyancsak szótári alakban (ige: főnévi igenév, főnév: egyes szám alanyeset). NEM a ragozott alak fordítását kérem.

        ${phraseNote(phrases)}${stressNote()}Válaszolj KIZÁRÓLAG egy JSON tömbbel, pontosan $count elemmel, a képek sorrendjében, semmi mást ne írj:
        [${jsonItem()}]
    """.trimIndent()

    // ---------------------------------------------------------------- hívások

    private fun callText(prompt: String, maxTokens: Int): String = when (settings.provider) {
        AiProvider.ANTHROPIC -> callAnthropic(prompt, maxTokens)
        AiProvider.OPENAI -> callOpenAi(prompt)
        AiProvider.GEMINI -> callGemini(
            JSONArray().put(JSONObject().apply { put("text", prompt) })
        )
    }

    private fun callVision(prompt: String, images: List<String>, labelImages: Boolean): String =
        when (settings.provider) {
            AiProvider.ANTHROPIC -> callAnthropic(
                anthropicVisionContent(prompt, images, labelImages),
                maxOf(500, 300 + images.size * 150)
            )
            AiProvider.OPENAI -> callOpenAi(openAiVisionContent(prompt, images))
            AiProvider.GEMINI -> callGemini(geminiVisionParts(prompt, images))
        }

    private fun anthropicVisionContent(prompt: String, images: List<String>, label: Boolean): JSONArray {
        val content = JSONArray()
        images.forEachIndexed { idx, b64 ->
            if (label) {
                content.put(JSONObject().apply {
                    put("type", "text")
                    put("text", "Kép ${idx + 1}:")
                })
            }
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
        return content
    }

    private fun openAiVisionContent(prompt: String, images: List<String>): JSONArray {
        val content = JSONArray()
        content.put(JSONObject().apply {
            put("type", "text")
            put("text", prompt)
        })
        images.forEach { b64 ->
            content.put(JSONObject().apply {
                put("type", "image_url")
                put(
                    "image_url",
                    JSONObject().apply { put("url", "data:image/jpeg;base64,$b64") }
                )
            })
        }
        return content
    }

    private fun geminiVisionParts(prompt: String, images: List<String>): JSONArray {
        val parts = JSONArray()
        parts.put(JSONObject().apply { put("text", prompt) })
        images.forEach { b64 ->
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
        return parts
    }

    private fun callAnthropic(content: Any, maxTokens: Int): String {
        val body = JSONObject().apply {
            put("model", settings.model.ifBlank { "claude-sonnet-4-6" })
            put("max_tokens", maxTokens)
            put(
                "messages",
                JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("content", content)
                })
            )
        }
        val url = settings.baseUrl.ifBlank { "https://api.anthropic.com/v1/messages" }
        val request = buildRequest(
            url,
            mapOf("x-api-key" to settings.apiKey.trim(), "anthropic-version" to "2023-06-01"),
            body
        )
        return parseAnthropic(execute(request))
    }

    private fun callOpenAi(content: Any): String {
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
        val request = buildRequest(
            url,
            mapOf("Authorization" to "Bearer ${settings.apiKey.trim()}"),
            body
        )
        return parseOpenAi(execute(request))
    }

    private fun callGemini(parts: JSONArray): String {
        val model = settings.model.ifBlank { "gemini-2.0-flash" }
        val url = settings.baseUrl.ifBlank {
            "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${settings.apiKey.trim()}"
        }
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply { put("parts", parts) }))
        }
        val request = buildRequest(url, emptyMap(), body)
        return parseGemini(execute(request))
    }

    private fun buildRequest(url: String, headers: Map<String, String>, body: JSONObject): Request {
        try {
            val builder = Request.Builder().url(url)
            headers.forEach { (k, v) -> builder.addHeader(k, v) }
            builder.addHeader("content-type", "application/json")
            return builder.post(body.toString().toRequestBody(jsonMedia)).build()
        } catch (e: IllegalArgumentException) {
            throw AiException(AiErrorKind.REJECTED, "bad URL or header", settings.provider, e)
        }
    }

    /** Elküldi a kérést; hálózati és HTTP hibákat AiException-né alakítja. */
    private fun execute(request: Request): String {
        try {
            return client.newCall(request).execute().use { resp ->
                val respBody = resp.body?.string() ?: ""
                if (!resp.isSuccessful) throw httpError(resp.code, respBody)
                respBody
            }
        } catch (e: AiException) {
            throw e
        } catch (e: InterruptedIOException) {
            throw AiException(AiErrorKind.TIMEOUT, "timeout", settings.provider, e)
        } catch (e: IOException) {
            val msg = e.message?.let { ": " + it.take(100) } ?: ""
            throw AiException(AiErrorKind.NO_INTERNET, e.javaClass.simpleName + msg, settings.provider, e)
        }
    }

    /** HTTP hibakód + a szolgáltató válaszszövege alapján megállapítja a hiba típusát. */
    private fun httpError(code: Int, body: String): AiException {
        val lower = body.lowercase()
        val kind = when {
            code == 401 || code == 403 -> AiErrorKind.BAD_KEY
            lower.contains("api_key_invalid") ||
                lower.contains("api key not valid") ||
                lower.contains("invalid x-api-key") ||
                lower.contains("incorrect api key") -> AiErrorKind.BAD_KEY
            code == 429 || lower.contains("resource_exhausted") -> AiErrorKind.RATE_LIMIT
            code == 404 -> AiErrorKind.MODEL_NOT_FOUND
            code == 400 && lower.contains("model") &&
                (lower.contains("not found") || lower.contains("does not exist") || lower.contains("not supported")) ->
                AiErrorKind.MODEL_NOT_FOUND
            code == 408 -> AiErrorKind.TIMEOUT
            code >= 500 || lower.contains("overloaded") -> AiErrorKind.OVERLOADED
            code in 400..499 -> AiErrorKind.REJECTED
            else -> AiErrorKind.OTHER
        }
        val api = extractApiError(body)
        val detail = "HTTP $code" + (if (api != null) " · $api" else "")
        return AiException(kind, detail, settings.provider)
    }

    /** A szolgáltató hibaválaszából kiszedi a rövid hibakódot/üzenetet (ha van). */
    private fun extractApiError(body: String): String? = try {
        val err = JSONObject(body).optJSONObject("error")
        if (err == null) {
            null
        } else {
            fun field(key: String): String {
                val v = err.optString(key)
                return if (v == "null") "" else v
            }
            val tag = field("status").ifBlank { field("type") }.ifBlank { field("code") }
            val msg = field("message")
            listOf(tag, msg).filter { it.isNotBlank() }.joinToString(": ").take(160).ifBlank { null }
        }
    } catch (e: Exception) {
        null
    }

    // ---------------------------------------------------------------- válaszok

    private inline fun parseGuard(block: () -> String): String =
        try {
            block()
        } catch (e: AiException) {
            throw e
        } catch (e: JSONException) {
            throw AiException(AiErrorKind.BAD_RESPONSE, "JSON: " + (e.message ?: "").take(100), settings.provider, e)
        }

    private fun parseAnthropic(respBody: String): String = parseGuard {
        val json = JSONObject(respBody)
        if (json.optString("stop_reason") == "max_tokens") {
            throw AiException(AiErrorKind.BAD_RESPONSE, "truncated (max_tokens)", AiProvider.ANTHROPIC)
        }
        val content = json.optJSONArray("content")
        val sb = StringBuilder()
        if (content != null) {
            for (i in 0 until content.length()) {
                val block = content.getJSONObject(i)
                if (block.optString("type") == "text") sb.append(block.optString("text"))
            }
        }
        val text = sb.toString()
        if (text.isBlank()) {
            throw AiException(AiErrorKind.EMPTY, "stop_reason=" + json.optString("stop_reason"), AiProvider.ANTHROPIC)
        }
        text
    }

    private fun parseOpenAi(respBody: String): String = parseGuard {
        val json = JSONObject(respBody)
        val choices = json.optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            throw AiException(AiErrorKind.EMPTY, "no choices", AiProvider.OPENAI)
        }
        val choice = choices.getJSONObject(0)
        val finish = choice.optString("finish_reason")
        val raw = choice.optJSONObject("message")?.optString("content").orEmpty()
        val text = if (raw == "null") "" else raw
        if (text.isBlank()) {
            throw AiException(AiErrorKind.EMPTY, "finish_reason=$finish", AiProvider.OPENAI)
        }
        if (finish == "length") {
            throw AiException(AiErrorKind.BAD_RESPONSE, "truncated (length)", AiProvider.OPENAI)
        }
        text
    }

    private fun parseGemini(respBody: String): String = parseGuard {
        val json = JSONObject(respBody)
        val candidates = json.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            val block = json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            throw AiException(
                AiErrorKind.EMPTY,
                if (block.isNotBlank()) "blocked: $block" else "no candidates",
                AiProvider.GEMINI
            )
        }
        val cand = candidates.getJSONObject(0)
        val finish = cand.optString("finishReason")
        val parts = cand.optJSONObject("content")?.optJSONArray("parts")
        val sb = StringBuilder()
        if (parts != null) {
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (!part.optBoolean("thought", false)) sb.append(part.optString("text"))
            }
        }
        val text = sb.toString()
        if (text.isBlank()) {
            throw AiException(AiErrorKind.EMPTY, "finishReason=$finish", AiProvider.GEMINI)
        }
        if (finish == "MAX_TOKENS") {
            throw AiException(AiErrorKind.BAD_RESPONSE, "truncated (MAX_TOKENS)", AiProvider.GEMINI)
        }
        text
    }

    /** Csak akkor tartjuk meg a hangsúlyos alakot, ha a jel nélkül ugyanaz, mint a szótári alak. */
    private fun cleanStress(stressed: String, dict: String): String {
        val s = stressed.trim()
        if (s.isEmpty() || !s.contains('\u0301')) return ""
        return if (s.replace("\u0301", "").equals(dict.trim(), ignoreCase = true)) s else ""
    }

    private fun parseResponse(raw: String): List<Flashcard> {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val startIdx = cleaned.indexOf('[')
        val endIdx = cleaned.lastIndexOf(']')
        if (startIdx == -1 || endIdx == -1 || endIdx < startIdx) {
            throw AiException(AiErrorKind.BAD_RESPONSE, "no JSON array: " + cleaned.take(80), settings.provider)
        }
        val arr = try {
            JSONArray(cleaned.substring(startIdx, endIdx + 1))
        } catch (e: JSONException) {
            throw AiException(AiErrorKind.BAD_RESPONSE, "JSON: " + (e.message ?: "").take(100), settings.provider, e)
        }
        val out = mutableListOf<Flashcard>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val dict = o.optString("dictionary_form", "")
            out.add(
                Flashcard(
                    id = 0L,
                    dictionaryForm = dict,
                    translation = o.optString("translation", ""),
                    stressedForm = cleanStress(o.optString("stressed_form", ""), dict)
                )
            )
        }
        if (out.isEmpty()) {
            throw AiException(AiErrorKind.EMPTY, "empty array", settings.provider)
        }
        return out
    }

    private fun parseSingleCardResponse(raw: String): Flashcard {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val startIdx = cleaned.indexOf('{')
        val endIdx = cleaned.lastIndexOf('}')
        if (startIdx == -1 || endIdx == -1 || endIdx < startIdx) {
            throw AiException(AiErrorKind.BAD_RESPONSE, "no JSON object: " + cleaned.take(80), settings.provider)
        }
        val obj = try {
            JSONObject(cleaned.substring(startIdx, endIdx + 1))
        } catch (e: JSONException) {
            throw AiException(AiErrorKind.BAD_RESPONSE, "JSON: " + (e.message ?: "").take(100), settings.provider, e)
        }
        val dict = obj.optString("dictionary_form", "")
        return Flashcard(
            id = 0L,
            dictionaryForm = dict,
            translation = obj.optString("translation", ""),
            stressedForm = cleanStress(obj.optString("stressed_form", ""), dict)
        )
    }
}
