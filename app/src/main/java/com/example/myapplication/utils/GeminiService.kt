package com.example.myapplication.utils

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.myapplication.data.repository.AzureOpenAIRepository
import com.example.myapplication.data.repository.LLMRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service for homework problem recognition and solving
 * Uses ML Kit for text recognition and LLM API (Ollama or Azure OpenAI) for solving
 */
object GeminiService {
    private const val TAG = "GeminiService"

    // LLM Repositories
    private val ollamaRepository = LLMRepository()
    private val azureOpenAIRepository = AzureOpenAIRepository()

    // ML Kit Text Recognizer
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Recognize text from bitmap using ML Kit
     */
    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            suspendCancellableCoroutine { continuation ->
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val recognizedText = visionText.text
                        if (recognizedText.isNotBlank()) {
                            Log.d(TAG, "Text recognized successfully: ${recognizedText.take(100)}...")
                            continuation.resume(recognizedText)
                        } else {
                            Log.w(TAG, "No text found in image")
                            continuation.resume("Không tìm thấy text trong ảnh")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Text recognition failed", e)
                        continuation.resumeWithException(e)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in recognizeText", e)
            "Lỗi nhận diện: ${e.message}"
        }
    }

    /**
     * Solve the problem using configured LLM provider (Ollama or Azure OpenAI)
     */
    suspend fun solveProblem(bitmap: Bitmap, problemText: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentProvider = ApiConfig.currentLLMProvider
            Log.d(TAG, "Solving problem with LLM provider: $currentProvider")

            // Convert bitmap to base64 for image support
            val base64Image = bitmapToBase64(bitmap)

            when (currentProvider) {
                LLMProvider.AZURE_OPENAI -> solveWithAzureOpenAI(problemText, base64Image)
                LLMProvider.OLLAMA -> solveWithOllama(problemText, base64Image)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in solveProblem", e)
            "Lỗi: ${e.message}"
        }
    }

    /**
     * Solve problem using Azure OpenAI
     */
    private suspend fun solveWithAzureOpenAI(problemText: String, base64Image: String): String {
        Log.d(TAG, "Using Azure OpenAI for solving...")

        // Try with image first
        val result = azureOpenAIRepository.generateSolutionWithImage(
            problemText = problemText,
            base64Image = base64Image
        )

        return result.getOrElse { imageError ->
            Log.w(TAG, "Azure OpenAI vision failed, trying text-only: ${imageError.message}")

            // Fallback to text-only
            val textResult = azureOpenAIRepository.generateSolution(problemText)
            textResult.getOrElse { textError ->
                Log.e(TAG, "Azure OpenAI text-only also failed", textError)
                "Lỗi khi giải bài tập với Azure OpenAI: ${textError.message}"
            }
        }
    }

    /**
     * Solve problem using Ollama (self-hosted LLM)
     */
    private suspend fun solveWithOllama(problemText: String, base64Image: String): String {
        Log.d(TAG, "Using Ollama for solving...")

        // Try using chat endpoint with image support first
        val result = ollamaRepository.generateSolutionWithChat(
            problemText = problemText,
            base64Image = base64Image
        )

        return result.getOrElse { chatError ->
            Log.w(TAG, "Ollama chat endpoint failed, trying generate endpoint: ${chatError.message}")

            // Fallback to generate endpoint (text only)
            val generateResult = ollamaRepository.generateSolution(problemText)
            generateResult.getOrElse { generateError ->
                Log.e(TAG, "Both Ollama endpoints failed", generateError)
                "Lỗi khi giải bài tập với Ollama: ${generateError.message}"
            }
        }
    }

    /**
     * Check if the current LLM API is available
     */
    suspend fun isApiAvailable(): Boolean {
        return when (ApiConfig.currentLLMProvider) {
            LLMProvider.AZURE_OPENAI -> azureOpenAIRepository.isApiAvailable()
            LLMProvider.OLLAMA -> ollamaRepository.isApiAvailable()
        }
    }

    /**
     * Switch LLM provider
     */
    fun setLLMProvider(provider: LLMProvider) {
        ApiConfig.currentLLMProvider = provider
        Log.d(TAG, "LLM provider switched to: $provider")
    }

    /**
     * Get current LLM provider
     */
    fun getCurrentProvider(): LLMProvider = ApiConfig.currentLLMProvider

    /**
     * Convert bitmap to base64 string for API calls
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        return try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting bitmap to base64", e)
            ""
        }
    }
}

