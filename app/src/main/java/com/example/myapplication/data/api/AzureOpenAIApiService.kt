package com.example.myapplication.data.api

import com.example.myapplication.data.model.AzureOpenAIChatRequest
import com.example.myapplication.data.model.AzureOpenAIChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API Service for Azure OpenAI API
 * Base URL: https://azureaiengine.openai.azure.com/
 */
interface AzureOpenAIApiService {

    /**
     * Chat Completions endpoint
     * POST /openai/deployments/{deployment-id}/chat/completions?api-version={api-version}
     *
     * @param deploymentId The deployment name (e.g., "gpt-4", "gpt-35-turbo")
     * @param apiVersion API version (e.g., "2024-08-01-preview")
     * @param apiKey Azure OpenAI API Key
     * @param request Chat request body
     */
    @POST("openai/deployments/{deployment-id}/chat/completions")
    suspend fun chatCompletions(
        @Path("deployment-id") deploymentId: String,
        @Query("api-version") apiVersion: String,
        @Header("api-key") apiKey: String,
        @Body request: AzureOpenAIChatRequest
    ): Response<AzureOpenAIChatResponse>
}

