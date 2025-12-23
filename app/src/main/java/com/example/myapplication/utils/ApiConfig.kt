package com.example.myapplication.utils

import com.example.myapplication.BuildConfig

/**
 * LLM Provider options
 */
enum class LLMProvider {
    OLLAMA,      // Self-hosted Ollama on VM
    AZURE_OPENAI // Azure OpenAI Service
}

/**
 * API Configuration
 *
 * CẢNH BÁO: Tại đây chỉ cho development
 * Để production, di chuyển API Key vào:
 * 1. Android Secret Gradle Properties
 * 2. Cloud-based Key Management Service
 * 3. Backend Server (khuyến nghị)
 */
object ApiConfig {
    // Current LLM Provider - Change this to switch between providers
    var currentLLMProvider: LLMProvider = LLMProvider.AZURE_OPENAI

    // Gemini API Configuration (Legacy - kept for reference)
    const val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
    const val GEMINI_MODEL = "gemini-2.5-flash"

    // Gemini API Endpoints
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    // Ollama LLM API Configuration (VM hosted)
    const val OLLAMA_BASE_URL = "http://20.6.129.252:11434/"
    const val OLLAMA_DEFAULT_MODEL = "llama3.2"

    // Azure OpenAI Configuration
    val AZURE_OPENAI_API_KEY = BuildConfig.AZURE_OPENAI_API_KEY
    val AZURE_OPENAI_ENDPOINT = BuildConfig.AZURE_OPENAI_ENDPOINT
    const val AZURE_OPENAI_API_VERSION = "2025-01-01-preview"
    const val AZURE_OPENAI_DEPLOYMENT = "o4-mini" // Your deployment name

    // Timeout configurations
    const val API_TIMEOUT_MS = 60000 // 60 seconds
    const val API_RETRY_COUNT = 3

    // LLM specific timeouts (can take longer)
    const val LLM_CONNECT_TIMEOUT_SEC = 30L
    const val LLM_READ_TIMEOUT_SEC = 120L
    const val LLM_WRITE_TIMEOUT_SEC = 60L
}

