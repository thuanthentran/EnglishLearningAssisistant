// app/src/main/java/com/example/myapplication/utils/ChatAIService.kt
package com.example.myapplication.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * AI Service for English learning assistance in chat
 * Uses Azure OpenAI API as an English tutor
 */
object ChatAIService {

    private fun buildAzureOpenAIUrl(): String {
        return "${ApiConfig.AZURE_OPENAI_ENDPOINT}openai/deployments/${ApiConfig.AZURE_OPENAI_DEPLOYMENT}/chat/completions?api-version=${ApiConfig.AZURE_OPENAI_API_VERSION}"
    }

    private suspend fun callAzureOpenAI(prompt: String): String = withContext(Dispatchers.IO) {
        val url = URL(buildAzureOpenAIUrl())
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("api-key", ApiConfig.AZURE_OPENAI_API_KEY)
            connection.connectTimeout = ApiConfig.API_TIMEOUT_MS
            connection.readTimeout = ApiConfig.API_TIMEOUT_MS
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("max_completion_tokens", 1000)
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else {
                    throw Exception("No response from API")
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                throw Exception("API Error $responseCode: $errorResponse")
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Translate message between English and Vietnamese
     * Auto-detects language and translates accordingly
     */
    suspend fun translateMessage(message: String): TranslationResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val prompt = """
Bạn là trợ giảng tiếng Anh. Nhiệm vụ: dịch tin nhắn chat.

Quy tắc:
- Nếu tin nhắn là tiếng Anh → dịch sang tiếng Việt
- Nếu tin nhắn là tiếng Việt → dịch sang tiếng Anh
- Chỉ trả về bản dịch, không giải thích
- Giữ nguyên nghĩa và phong cách chat thân mật
- Không dùng emoji
- Không dùng markdown

Tin nhắn cần dịch: "$message"

Trả về theo format:
NGÔN_NGỮ_GỐC: [EN hoặc VI]
BẢN_DỊCH: [bản dịch]
            """.trimIndent()

            val result = callAzureOpenAI(prompt)
            parseTranslationResult(result, message)
        } catch (e: Exception) {
            TranslationResult(
                originalLanguage = "UNKNOWN",
                translatedText = "Lỗi dịch: ${e.message}",
                isError = true
            )
        }
    }

    /**
     * Analyze an English message for Vietnamese learners
     * Provides grammar, vocabulary, and usage analysis
     */
    suspend fun analyzeMessage(message: String): MessageAnalysis = withContext(Dispatchers.IO) {
        return@withContext try {
            val prompt = """
Bạn là trợ giảng tiếng Anh cho người Việt Nam học tiếng Anh.

Tin nhắn cần phân tích: "$message"

Hãy phân tích tin nhắn này và trả về theo format sau (không dùng markdown, không emoji):
NGHĨA: [dịch nghĩa của cả câu sang tiếng Việt]
NGỮ_PHÁP: [giải thích ngắn gọn cấu trúc ngữ pháp chính trong câu]
TỪ_VỰNG: [liệt kê 2-3 từ quan trọng và nghĩa của chúng, mỗi từ một dòng dạng: từ - nghĩa]
PHONG_CÁCH: [trang trọng/thân mật/trung tính và giải thích ngắn]
GHI_CHÚ: [những lưu ý quan trọng về cách dùng nếu có, nếu không có thì để trống]
            """.trimIndent()

            val result = callAzureOpenAI(prompt)
            parseMessageAnalysis(result, message)
        } catch (e: Exception) {
            MessageAnalysis(
                message = message,
                meaning = "Lỗi: ${e.message}",
                isError = true
            )
        }
    }

    /**
     * Explain a specific English word for Vietnamese learners (legacy support)
     */
    suspend fun explainWord(word: String, context: String = ""): WordExplanation = withContext(Dispatchers.IO) {
        return@withContext try {
            val contextPart = if (context.isNotBlank()) {
                "Ngữ cảnh trong chat: \"$context\""
            } else {
                ""
            }

            val prompt = """
Bạn là trợ giảng tiếng Anh cho người Việt Nam học tiếng Anh trình độ cơ bản-trung cấp.

Từ cần giải thích: "$word"
$contextPart

Trả về theo format sau (không dùng markdown, không emoji):
NGHĨA: [nghĩa tiếng Việt ngắn gọn]
TỪ_LOẠI: [danh từ/động từ/tính từ/trạng từ/...]
PHIÊN_ÂM: [phiên âm IPA]
CÁCH_DÙNG: [1-2 câu giải thích cách dùng trong giao tiếp]
VÍ_DỤ_1: [câu ví dụ tiếng Anh đơn giản]
VÍ_DỤ_1_DỊCH: [dịch câu ví dụ sang tiếng Việt]
VÍ_DỤ_2: [câu ví dụ tiếng Anh khác]
VÍ_DỤ_2_DỊCH: [dịch câu ví dụ sang tiếng Việt]
            """.trimIndent()

            val result = callAzureOpenAI(prompt)
            parseWordExplanation(result, word)
        } catch (e: Exception) {
            WordExplanation(
                word = word,
                meaning = "Lỗi: ${e.message}",
                isError = true
            )
        }
    }

    private fun parseTranslationResult(result: String, originalMessage: String): TranslationResult {
        val lines = result.lines()
        var originalLanguage = "UNKNOWN"
        var translatedText = result // Fallback to full result

        for (line in lines) {
            when {
                line.startsWith("NGÔN_NGỮ_GỐC:") -> {
                    originalLanguage = line.substringAfter(":").trim()
                }
                line.startsWith("BẢN_DỊCH:") -> {
                    translatedText = line.substringAfter(":").trim()
                }
            }
        }

        // If parsing failed, use the whole result as translation
        if (translatedText == result && lines.size == 1) {
            translatedText = result.trim()
        }

        return TranslationResult(
            originalLanguage = originalLanguage,
            translatedText = translatedText,
            isError = false
        )
    }

    private fun parseMessageAnalysis(result: String, message: String): MessageAnalysis {
        // Check if result looks like an error or empty
        if (result.isBlank()) {
            return MessageAnalysis(
                message = message,
                meaning = "API không trả về kết quả",
                isError = true
            )
        }

        val lines = result.lines()
        var meaning = ""
        var grammar = ""
        val vocabulary = mutableListOf<VocabularyItem>()
        var style = ""
        var notes = ""
        var foundAnyField = false

        for (line in lines) {
            when {
                line.startsWith("NGHĨA:") -> {
                    meaning = line.substringAfter(":").trim()
                    foundAnyField = true
                }
                line.startsWith("NGỮ_PHÁP:") -> {
                    grammar = line.substringAfter(":").trim()
                    foundAnyField = true
                }
                line.startsWith("TỪ_VỰNG:") -> {
                    // Skip the header line, vocabulary items come in next lines
                    foundAnyField = true
                }
                line.startsWith("PHONG_CÁCH:") -> {
                    style = line.substringAfter(":").trim()
                    foundAnyField = true
                }
                line.startsWith("GHI_CHÚ:") -> {
                    notes = line.substringAfter(":").trim()
                    foundAnyField = true
                }
                line.contains(" - ") && !line.startsWith("NGHĨA:") && !line.startsWith("NGỮ_PHÁP:")
                    && !line.startsWith("PHONG_CÁCH:") && !line.startsWith("GHI_CHÚ:") && !line.startsWith("TỪ_VỰNG:") -> {
                    // This is a vocabulary item in format "word - meaning"
                    val parts = line.split(" - ", limit = 2)
                    if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                        vocabulary.add(VocabularyItem(parts[0].trim(), parts[1].trim()))
                    }
                }
            }
        }

        // If no fields were found, the response format is wrong
        if (!foundAnyField || meaning.isBlank()) {
            return MessageAnalysis(
                message = message,
                meaning = "Lỗi phân tích: API trả về định dạng không đúng.\n\nKết quả thô:\n$result",
                isError = true
            )
        }

        return MessageAnalysis(
            message = message,
            meaning = meaning,
            grammar = grammar,
            vocabulary = vocabulary,
            style = style,
            notes = notes,
            isError = false
        )
    }

    private fun parseWordExplanation(result: String, word: String): WordExplanation {
        val lines = result.lines()
        var meaning = ""
        var wordType = ""
        var pronunciation = ""
        var usage = ""
        val examples = mutableListOf<ExampleSentence>()

        var currentExample = ""

        for (line in lines) {
            when {
                line.startsWith("NGHĨA:") -> meaning = line.substringAfter(":").trim()
                line.startsWith("TỪ_LOẠI:") -> wordType = line.substringAfter(":").trim()
                line.startsWith("PHIÊN_ÂM:") -> pronunciation = line.substringAfter(":").trim()
                line.startsWith("CÁCH_DÙNG:") -> usage = line.substringAfter(":").trim()
                line.startsWith("VÍ_DỤ_1:") -> currentExample = line.substringAfter(":").trim()
                line.startsWith("VÍ_DỤ_1_DỊCH:") -> {
                    if (currentExample.isNotBlank()) {
                        examples.add(ExampleSentence(currentExample, line.substringAfter(":").trim()))
                        currentExample = ""
                    }
                }
                line.startsWith("VÍ_DỤ_2:") -> currentExample = line.substringAfter(":").trim()
                line.startsWith("VÍ_DỤ_2_DỊCH:") -> {
                    if (currentExample.isNotBlank()) {
                        examples.add(ExampleSentence(currentExample, line.substringAfter(":").trim()))
                        currentExample = ""
                    }
                }
            }
        }

        return WordExplanation(
            word = word,
            meaning = meaning.ifBlank { "Không tìm thấy nghĩa" },
            wordType = wordType,
            pronunciation = pronunciation,
            usage = usage,
            examples = examples,
            isError = false
        )
    }
}

/**
 * Result of translation
 */
data class TranslationResult(
    val originalLanguage: String, // "EN" or "VI"
    val translatedText: String,
    val isError: Boolean = false
)

/**
 * Message analysis for learning
 */
data class MessageAnalysis(
    val message: String,
    val meaning: String = "",
    val grammar: String = "",
    val vocabulary: List<VocabularyItem> = emptyList(),
    val style: String = "",
    val notes: String = "",
    val isError: Boolean = false
)

/**
 * Vocabulary item in message analysis
 */
data class VocabularyItem(
    val word: String,
    val meaning: String
)

/**
 * Word explanation for learning
 */
data class WordExplanation(
    val word: String,
    val meaning: String = "",
    val wordType: String = "",
    val pronunciation: String = "",
    val usage: String = "",
    val examples: List<ExampleSentence> = emptyList(),
    val isError: Boolean = false
)

/**
 * Example sentence with translation
 */
data class ExampleSentence(
    val english: String,
    val vietnamese: String
)

