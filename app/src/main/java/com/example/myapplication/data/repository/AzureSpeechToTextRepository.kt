package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.BuildConfig
import com.example.myapplication.data.api.AzureSpeechToTextApiService
import com.example.myapplication.data.model.TranscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Repository for Azure Speech-to-Text functionality
 * Uses Azure OpenAI gpt-4o-mini-transcribe deployment
 */
class AzureSpeechToTextRepository {

    companion object {
        private const val TAG = "AzureSpeechToTextRepo"

        // Get from BuildConfig (stored in local.properties)
        private val API_KEY = BuildConfig.AZURE_STT_API_KEY
        private val BASE_URL = BuildConfig.AZURE_STT_ENDPOINT

        // Azure STT settings
        private const val API_VERSION = "2025-03-01-preview"
        private const val DEPLOYMENT_ID = "gpt-4o-mini-transcribe"
        private const val MODEL_NAME = "gpt-4o-mini-transcribe"

        // Timeout settings
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 120L
        private const val WRITE_TIMEOUT = 120L
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

    private val apiService: AzureSpeechToTextApiService by lazy {
        retrofit.create(AzureSpeechToTextApiService::class.java)
    }

    /**
     * Transcribe audio file to text
     * @param audioFile The audio file to transcribe (mp3, wav, m4a, etc.)
     * @return Result containing the transcribed text or error
     */
    suspend fun transcribeAudio(audioFile: File): Result<TranscriptionResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Transcribing audio file: ${audioFile.name}, size: ${audioFile.length()} bytes")

            if (!audioFile.exists()) {
                return@withContext Result.failure(Exception("Audio file does not exist"))
            }

            // Determine media type based on file extension
            val mediaType = when (audioFile.extension.lowercase()) {
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "m4a" -> "audio/m4a"
                "ogg" -> "audio/ogg"
                "webm" -> "audio/webm"
                "flac" -> "audio/flac"
                else -> "audio/mpeg" // Default to mp3
            }.toMediaType()

            // Create multipart file
            val requestFile = audioFile.asRequestBody(mediaType)
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)

            // Create model request body
            val modelBody = MODEL_NAME.toRequestBody("text/plain".toMediaType())

            val response = apiService.transcribeAudio(
                deploymentId = DEPLOYMENT_ID,
                apiVersion = API_VERSION,
                apiKey = API_KEY,
                file = filePart,
                model = modelBody
            )

            if (response.isSuccessful) {
                val responseBody = response.body()?.string()
                Log.d(TAG, "Transcription response: $responseBody")

                if (responseBody != null) {
                    try {
                        val jsonObject = JSONObject(responseBody)
                        val text = jsonObject.optString("text", "")
                        val duration = jsonObject.optDouble("duration", 0.0).toFloat()
                        val language = jsonObject.optString("language", "").takeIf { it.isNotEmpty() }

                        if (text.isNotBlank()) {
                            Log.d(TAG, "Transcription successful: ${text.take(100)}...")
                            Result.success(TranscriptionResult(
                                text = text,
                                duration = if (duration > 0) duration else null,
                                language = language
                            ))
                        } else {
                            Log.e(TAG, "Empty transcription result")
                            Result.failure(Exception("Empty transcription result"))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transcription response", e)
                        Result.failure(Exception("Error parsing response: ${e.message}"))
                    }
                } else {
                    Log.e(TAG, "Empty response body")
                    Result.failure(Exception("Empty response from API"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Transcription API request failed: $errorBody")
                Result.failure(Exception("API request failed: $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing audio", e)
            Result.failure(e)
        }
    }

    /**
     * Check if the API is available
     */
    fun isConfigured(): Boolean {
        return API_KEY.isNotBlank() && BASE_URL.isNotBlank()
    }
}

