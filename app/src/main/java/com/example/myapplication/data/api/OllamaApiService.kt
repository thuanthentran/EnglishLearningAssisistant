package com.example.myapplication.data.api

import com.example.myapplication.data.model.OllamaChatRequest
import com.example.myapplication.data.model.OllamaChatResponse
import com.example.myapplication.data.model.OllamaRequest
import com.example.myapplication.data.model.OllamaResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit API Service for Ollama LLM API
 * Base URL: http://20.6.129.252:11434
 */
interface OllamaApiService {

    /**
     * Generate a response using the generate endpoint
     * POST /api/generate
     */
    @POST("api/generate")
    suspend fun generate(@Body request: OllamaRequest): Response<OllamaResponse>

    /**
     * Generate a chat response using the chat endpoint
     * POST /api/chat
     */
    @POST("api/chat")
    suspend fun chat(@Body request: OllamaChatRequest): Response<OllamaChatResponse>

    /**
     * Check if the API is available
     * GET /api/tags
     */
    @GET("api/tags")
    suspend fun getTags(): Response<Any>

    /**
     * Health check endpoint
     * GET /
     */
    @GET("/")
    suspend fun healthCheck(): Response<String>
}

