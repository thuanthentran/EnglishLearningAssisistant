package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request model for Azure OpenAI Chat Completions API
 */
data class AzureOpenAIChatRequest(
    @SerializedName("messages")
    val messages: List<AzureOpenAIMessage>,

    @SerializedName("max_completion_tokens")
    val maxCompletionTokens: Int = 16000,

    @SerializedName("top_p")
    val topP: Float? = null,

    @SerializedName("frequency_penalty")
    val frequencyPenalty: Float? = null,

    @SerializedName("presence_penalty")
    val presencePenalty: Float? = null,

    @SerializedName("stop")
    val stop: List<String>? = null
)

/**
 * Message model for Azure OpenAI
 */
data class AzureOpenAIMessage(
    @SerializedName("role")
    val role: String, // "system", "user", "assistant"

    @SerializedName("content")
    val content: Any // Can be String or List<AzureOpenAIContentPart> for vision
)

/**
 * Content part for multimodal messages (text + image)
 */
data class AzureOpenAIContentPart(
    @SerializedName("type")
    val type: String, // "text" or "image_url"

    @SerializedName("text")
    val text: String? = null,

    @SerializedName("image_url")
    val imageUrl: AzureOpenAIImageUrl? = null
)

data class AzureOpenAIImageUrl(
    @SerializedName("url")
    val url: String // Can be base64: "data:image/jpeg;base64,..."
)

/**
 * Response model for Azure OpenAI Chat Completions API
 */
data class AzureOpenAIChatResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("object")
    val objectType: String?,

    @SerializedName("created")
    val created: Long?,

    @SerializedName("model")
    val model: String?,

    @SerializedName("choices")
    val choices: List<AzureOpenAIChoice>?,

    @SerializedName("usage")
    val usage: AzureOpenAIUsage?,

    @SerializedName("error")
    val error: AzureOpenAIError?
)

data class AzureOpenAIChoice(
    @SerializedName("index")
    val index: Int?,

    @SerializedName("message")
    val message: AzureOpenAIResponseMessage?,

    @SerializedName("finish_reason")
    val finishReason: String?
)

data class AzureOpenAIResponseMessage(
    @SerializedName("role")
    val role: String?,

    @SerializedName("content")
    val content: String?
)

data class AzureOpenAIUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int?,

    @SerializedName("completion_tokens")
    val completionTokens: Int?,

    @SerializedName("total_tokens")
    val totalTokens: Int?
)

data class AzureOpenAIError(
    @SerializedName("message")
    val message: String?,

    @SerializedName("type")
    val type: String?,

    @SerializedName("code")
    val code: String?
)

