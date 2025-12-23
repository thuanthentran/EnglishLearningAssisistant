package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.api.AzureOpenAIApiService
import com.example.myapplication.data.model.AzureOpenAIChatRequest
import com.example.myapplication.data.model.AzureOpenAIContentPart
import com.example.myapplication.data.model.AzureOpenAIImageUrl
import com.example.myapplication.data.model.AzureOpenAIMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Repository for interacting with Azure OpenAI API
 * Endpoint: https://azureaiengine.openai.azure.com/
 */
class AzureOpenAIRepository {

    companion object {
        private const val TAG = "AzureOpenAIRepository"

        // Get from BuildConfig (stored in local.properties)
        private val API_KEY = BuildConfig.AZURE_OPENAI_API_KEY
        private val BASE_URL = BuildConfig.AZURE_OPENAI_ENDPOINT

        // Azure OpenAI settings
        private const val API_VERSION = "2025-01-01-preview"
        private const val DEFAULT_DEPLOYMENT = "o4-mini" // Your deployment name

        // Model parameters
        private const val MAX_COMPLETION_TOKENS = 16000 // Safe limit for o4-mini

        // Timeout settings
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

    private val apiService: AzureOpenAIApiService by lazy {
        retrofit.create(AzureOpenAIApiService::class.java)
    }

    /**
     * Generate a solution for a homework problem using text only
     * @param problemText The text of the problem to solve
     * @param deploymentId The deployment name to use
     * @return Result containing the solution or error
     */
    suspend fun generateSolution(
        problemText: String,
        deploymentId: String = DEFAULT_DEPLOYMENT
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating solution with Azure OpenAI for: ${problemText.take(100)}...")

            val messages = listOf(
                AzureOpenAIMessage(
                    role = "system",
                    content = """Bạn là một trợ lý giáo dục chuyên giải bài tập. 
                        |Hãy giải bài tập một cách chi tiết, rõ ràng và dễ hiểu.
                        |Trình bày lời giải theo từng bước.
                        |Sử dụng tiếng Việt để trả lời.""".trimMargin()
                ),
                AzureOpenAIMessage(
                    role = "user",
                    content = buildSolutionPrompt(problemText)
                )
            )

            val request = AzureOpenAIChatRequest(
                messages = messages,
                maxCompletionTokens = MAX_COMPLETION_TOKENS
            )

            val response = apiService.chatCompletions(
                deploymentId = deploymentId,
                apiVersion = API_VERSION,
                apiKey = API_KEY,
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                val content = body?.choices?.firstOrNull()?.message?.content

                if (content != null) {
                    Log.d(TAG, "Solution generated successfully with Azure OpenAI")
                    Result.success(content)
                } else if (body?.error != null) {
                    Log.e(TAG, "Azure OpenAI API error: ${body.error.message}")
                    Result.failure(Exception(body.error.message ?: "Unknown API error"))
                } else {
                    Log.e(TAG, "Empty response from Azure OpenAI")
                    Result.failure(Exception("Empty response from API"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Azure OpenAI API request failed: $errorBody")
                Result.failure(Exception("API request failed: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating solution with Azure OpenAI", e)
            Result.failure(e)
        }
    }

    /**
     * Generate a solution with image support (Vision)
     * @param problemText The text of the problem
     * @param base64Image Base64 encoded image of the problem
     * @param deploymentId The deployment name (must support vision, e.g., gpt-4o, gpt-4-vision)
     * @return Result containing the solution or error
     */
    suspend fun generateSolutionWithImage(
        problemText: String,
        base64Image: String?,
        deploymentId: String = DEFAULT_DEPLOYMENT
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating solution with image using Azure OpenAI...")

            // Build content parts
            val contentParts = mutableListOf<AzureOpenAIContentPart>()

            // Add image if provided
            if (!base64Image.isNullOrBlank()) {
                contentParts.add(
                    AzureOpenAIContentPart(
                        type = "image_url",
                        imageUrl = AzureOpenAIImageUrl(
                            url = "data:image/jpeg;base64,$base64Image"
                        )
                    )
                )
            }

            // Add text prompt
            val textPrompt = if (problemText.isNotBlank()) {
                "Hãy giải bài tập sau:\n$problemText"
            } else {
                "Hãy nhận diện và giải bài tập trong hình ảnh này."
            }

            contentParts.add(
                AzureOpenAIContentPart(
                    type = "text",
                    text = textPrompt
                )
            )

            val messages = listOf(
                AzureOpenAIMessage(
                    role = "system",
                    content = """Bạn là một trợ lý giáo dục chuyên giải bài tập. 
                        |Hãy giải bài tập một cách chi tiết, rõ ràng và dễ hiểu.
                        |Trình bày lời giải theo từng bước.
                        |Sử dụng tiếng Việt để trả lời.""".trimMargin()
                ),
                AzureOpenAIMessage(
                    role = "user",
                    content = contentParts
                )
            )

            val request = AzureOpenAIChatRequest(
                messages = messages,
                maxCompletionTokens = MAX_COMPLETION_TOKENS
            )

            val response = apiService.chatCompletions(
                deploymentId = deploymentId,
                apiVersion = API_VERSION,
                apiKey = API_KEY,
                request = request
            )

            if (response.isSuccessful) {
                val body = response.body()
                val content = body?.choices?.firstOrNull()?.message?.content

                if (content != null) {
                    Log.d(TAG, "Vision solution generated successfully")
                    Result.success(content)
                } else if (body?.error != null) {
                    Log.e(TAG, "Azure OpenAI Vision API error: ${body.error.message}")
                    Result.failure(Exception(body.error.message ?: "Unknown API error"))
                } else {
                    Log.e(TAG, "Empty response from Azure OpenAI Vision")
                    Result.failure(Exception("Empty response from API"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Azure OpenAI Vision API request failed: $errorBody")
                Result.failure(Exception("API request failed: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating vision solution", e)
            Result.failure(e)
        }
    }

    /**
     * Check if the Azure OpenAI API is available
     */
    suspend fun isApiAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Simple health check by making a minimal request
            val messages = listOf(
                AzureOpenAIMessage(role = "user", content = "Hi")
            )
            val request = AzureOpenAIChatRequest(
                messages = messages,
                maxCompletionTokens = 5
            )

            val response = apiService.chatCompletions(
                deploymentId = DEFAULT_DEPLOYMENT,
                apiVersion = API_VERSION,
                apiKey = API_KEY,
                request = request
            )
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Azure OpenAI health check failed", e)
            false
        }
    }

    /**
     * Build a prompt for solving homework problems
     */
    private fun buildSolutionPrompt(problemText: String): String {
        return """Hãy giải bài tập sau một cách chi tiết, rõ ràng và dễ hiểu:

--- BÀI TẬP ---
$problemText
--- HẾT BÀI TẬP ---

Yêu cầu:
1. Trình bày lời giải theo từng bước
2. Giải thích rõ ràng từng bước
3. Đưa ra đáp án cuối cùng

Lời giải:"""
    }
}

