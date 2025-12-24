package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.api.OllamaApiService
import com.example.myapplication.data.model.OllamaChatMessage
import com.example.myapplication.data.model.OllamaChatRequest
import com.example.myapplication.data.model.OllamaOptions
import com.example.myapplication.data.model.OllamaRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repository for interacting with the LLM API hosted on VM
 * Uses Ollama API at http://20.6.129.252:11434
 */
class LLMRepository {

    companion object {
        private const val TAG = "LLMRepository"
        private const val BASE_URL = "http://20.6.129.252:11434/"
        private const val DEFAULT_MODEL = "qwen2.5:7b"

        // Timeout settings for long-running LLM requests
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 120L
        private const val WRITE_TIMEOUT = 60L
    }

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val apiService: OllamaApiService by lazy {
        retrofit.create(OllamaApiService::class.java)
    }

    /**
     * Generate a solution for a homework problem using the generate endpoint
     * @param problemText The text of the problem to solve
     * @param model The model to use (default: llama3.2)
     * @return Result containing the solution or error
     */
    suspend fun generateSolution(
        problemText: String,
        model: String = DEFAULT_MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating solution for problem: ${problemText.take(100)}...")

            val prompt = buildSolutionPrompt(problemText)

            val request = OllamaRequest(
                model = model,
                prompt = prompt,
                stream = false,
                options = OllamaOptions(
                    temperature = 0.3f, // Lower temperature for more precise answers
                    numPredict = 4096
                )
            )

            val response = apiService.generate(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.response != null) {
                    Log.d(TAG, "Solution generated successfully")
                    Result.success(body.response)
                } else if (body?.error != null) {
                    Log.e(TAG, "API error: ${body.error}")
                    Result.failure(Exception(body.error))
                } else {
                    Log.e(TAG, "Empty response from API")
                    Result.failure(Exception("Empty response from API"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "API request failed: $errorBody")
                Result.failure(Exception("API request failed: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating solution", e)
            Result.failure(e)
        }
    }

    /**
     * Generate a solution using the chat endpoint (supports images)
     * @param problemText The text of the problem
     * @param base64Image Optional base64 encoded image of the problem
     * @param model The model to use
     * @return Result containing the solution or error
     */
    suspend fun generateSolutionWithChat(
        problemText: String,
        base64Image: String? = null,
        model: String = DEFAULT_MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating solution with chat for: ${problemText.take(100)}...")

            val messages = mutableListOf<OllamaChatMessage>()

            // System message
            messages.add(
                OllamaChatMessage(
                    role = "system",
                    content = """
                        |Hãy giải bài tập này, chọn đáp án đúng và trình bày lời giải ngắn gọn trong tiếng Việt
                        """.trimMargin()
                )
            )

            // User message with optional image
            val userContent = if (problemText.isNotBlank()) {
                "Hãy giải bài tập sau:\n$problemText"
            } else {
                "Hãy giải bài tập trong hình ảnh."
            }

            messages.add(
                OllamaChatMessage(
                    role = "user",
                    content = userContent,
                    images = base64Image?.let { listOf(it) }
                )
            )

            val request = OllamaChatRequest(
                model = model,
                messages = messages,
                stream = false
            )

            val response = apiService.chat(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.message?.content != null) {
                    Log.d(TAG, "Chat solution generated successfully")
                    Result.success(body.message.content)
                } else if (body?.error != null) {
                    Log.e(TAG, "API error: ${body.error}")
                    Result.failure(Exception(body.error))
                } else {
                    Log.e(TAG, "Empty response from chat API")
                    Result.failure(Exception("Empty response from API"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Chat API request failed: $errorBody")
                Result.failure(Exception("API request failed: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating chat solution", e)
            Result.failure(e)
        }
    }

    /**
     * Check if the LLM API is available
     * @return true if API is reachable, false otherwise
     */
    suspend fun isApiAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTags()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "API health check failed", e)
            false
        }
    }

    /**
     * Build a prompt for solving homework problems
     */
    private fun buildSolutionPrompt(problemText: String): String {
        return """Hãy giải bài tập này, chọn đáp án đúng và trình bày lời giải chi tiết trong tiếng Việt
            |--- BÀI TẬP ---
            |$problemText
            |--- HẾT BÀI TẬP ---
            """.trimMargin()
    }
}

