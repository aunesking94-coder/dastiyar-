package com.dastiyar.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class ChatAction {
    data class AddTask(
        val title: String,
        val date: String?,
        val time: String?,
        val durationMin: Int,
        val priority: String,
        val category: String
    ) : ChatAction()

    data class RemoveTask(val title: String, val date: String?) : ChatAction()
    data class MarkDone(val title: String) : ChatAction()
    data class AddHabit(val name: String, val targetPerWeek: Int) : ChatAction()
    data class AddMemory(val text: String) : ChatAction()
    data class Rebuild(val date: String) : ChatAction()
}

data class AiReply(
    val text: String,
    val actions: List<ChatAction>
)

class GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun chat(
        apiKey: String,
        model: String,
        system: String,
        history: List<Pair<String, String>>,
        newUserMessage: String
    ): AiReply = withContext(Dispatchers.IO) {
        val contents = JSONArray()
        for ((role, text) in history) {
            contents.put(
                JSONObject()
                    .put("role", role)
                    .put("parts", JSONArray().put(JSONObject().put("text", text)))
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", newUserMessage)))
        )

        val body = JSONObject()
            .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", 0.5).put("maxOutputTokens", 1024))

        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=$apiKey")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()

        val resp = client.newCall(req).execute()
        val raw = resp.body?.string() ?: ""
        val json = try { JSONObject(raw) } catch (e: Exception) { JSONObject() }

        if (!resp.isSuccessful) {
            val err = json.optJSONObject("error")?.optString("message") ?: "خطای سرویس (HTTP ${resp.code})"
            return@withContext AiReply(text = "⚠️ $err", actions = emptyList())
        }

        val text = json.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text") ?: ""

        val actions = parseActions(text)
        AiReply(text = cleanText(text), actions = actions)
    }

    private fun cleanText(text: String): String {
        val idx = text.indexOf("##ACTIONS##")
        return if (idx >= 0) text.substring(0, idx).trim() else text.trim()
    }

    private fun parseActions(text: String): List<ChatAction> {
        val trimmed = text.trim()
        val marker = trimmed.indexOf("##ACTIONS##")
        val payload = if (marker >= 0) {
            trimmed.substring(marker + "##ACTIONS##".length).substringAfter('[').substringBeforeLast(']')
        } else {
            null
        }
        val jsonText = if (payload != null) "[" + payload + "]" else null
        if (jsonText == null) return emptyList()

        return try {
            val arr = JSONArray(jsonText)
            val result = mutableListOf<ChatAction>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                when (o.optString("type")) {
                    "task_add" -> result.add(
                        ChatAction.AddTask(
                            title = o.optString("title"),
                            date = o.optString("date").ifBlank { null },
                            time = o.optString("time").ifBlank { null },
                            durationMin = o.optInt("duration", 60),
                            priority = o.optString("priority").ifBlank { "med" },
                            category = o.optString("category").ifBlank { "عمومی" }
                        )
                    )
                    "task_remove" -> result.add(
                        ChatAction.RemoveTask(title = o.optString("title"), date = o.optString("date").ifBlank { null })
                    )
                    "task_done" -> result.add(ChatAction.MarkDone(title = o.optString("title")))
                    "habit_add" -> result.add(
                        ChatAction.AddHabit(
                            name = o.optString("name"),
                            targetPerWeek = o.optInt("target", 3)
                        )
                    )
                    "memory_add" -> result.add(ChatAction.AddMemory(text = o.optString("text")))
                    "rebuild" -> result.add(ChatAction.Rebuild(date = o.optString("date").ifBlank { "tomorrow" }))
                }
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}