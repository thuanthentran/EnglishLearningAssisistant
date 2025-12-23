package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for Ollama API
 */
data class OllamaRequest(
    @SerializedName("model")
    val model: String = "llama3.2",

    @SerializedName("prompt")
    val prompt: String,

    @SerializedName("stream")
    val stream: Boolean = false,

    @SerializedName("options")
    val options: OllamaOptions? = null
)

data class OllamaOptions(
    @SerializedName("temperature")
    val temperature: Float = 0.7f,

    @SerializedName("num_predict")
    val numPredict: Int = 2048
)

/**
 * Response model for Ollama API
 */
data class OllamaResponse(
    @SerializedName("model")
    val model: String?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("response")
    val response: String?,

    @SerializedName("done")
    val done: Boolean?,

    @SerializedName("context")
    val context: List<Int>?,

    @SerializedName("total_duration")
    val totalDuration: Long?,

    @SerializedName("load_duration")
    val loadDuration: Long?,

    @SerializedName("prompt_eval_count")
    val promptEvalCount: Int?,

    @SerializedName("prompt_eval_duration")
    val promptEvalDuration: Long?,

    @SerializedName("eval_count")
    val evalCount: Int?,

    @SerializedName("eval_duration")
    val evalDuration: Long?,

    @SerializedName("error")
    val error: String?
)

/**
 * Chat request model for Ollama API (alternative format)
 */
data class OllamaChatRequest(
    @SerializedName("model")
    val model: String = "llama3.2",

    @SerializedName("messages")
    val messages: List<OllamaChatMessage>,

    @SerializedName("stream")
    val stream: Boolean = false
)

data class OllamaChatMessage(
    @SerializedName("role")
    val role: String, // "system", "user", "assistant"

    @SerializedName("content")
    val content: String,

    @SerializedName("images")
    val images: List<String>? = null // Base64 encoded images
)

data class OllamaChatResponse(
    @SerializedName("model")
    val model: String?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("message")
    val message: OllamaChatMessage?,

    @SerializedName("done")
    val done: Boolean?,

    @SerializedName("total_duration")
    val totalDuration: Long?,

    @SerializedName("error")
    val error: String?
)

