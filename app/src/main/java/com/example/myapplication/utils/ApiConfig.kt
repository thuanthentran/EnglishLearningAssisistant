package com.example.myapplication.utils

import com.example.myapplication.BuildConfig

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
    // Gemini API Configuration
    const val GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY
    const val GEMINI_MODEL = "gemini-2.5-flash"

    // API Endpoints
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    // Azure OpenAI Configuration

    const val AZURE_OPENAI_DEPLOYMENT = "o4-mini"
    const val AZURE_OPENAI_API_VERSION = "2024-12-01-preview"

    // Timeout configurations
    const val API_TIMEOUT_MS = 60000 // 60 seconds
    const val API_RETRY_COUNT = 3
}

