package com.example.myapplication.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API Service for Azure OpenAI Audio Transcription API
 * Endpoint: https://23521471-0428-audio-resource.cognitiveservices.azure.com/
 */
interface AzureSpeechToTextApiService {

    /**
     * Audio Transcription endpoint using gpt-4o-mini-transcribe
     * POST /openai/deployments/{deployment-id}/audio/transcriptions?api-version={api-version}
     *
     * @param deploymentId The deployment name (e.g., "gpt-4o-mini-transcribe")
     * @param apiVersion API version (e.g., "2025-03-01-preview")
     * @param apiKey Azure API Key (using api-key header)
     * @param file Audio file to transcribe
     * @param model Model name
     */
    @Multipart
    @POST("openai/deployments/{deployment-id}/audio/transcriptions")
    suspend fun transcribeAudio(
        @Path("deployment-id") deploymentId: String,
        @Query("api-version") apiVersion: String,
        @Header("api-key") apiKey: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody
    ): Response<ResponseBody>
}
