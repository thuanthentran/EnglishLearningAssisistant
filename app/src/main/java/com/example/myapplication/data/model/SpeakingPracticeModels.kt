package com.example.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * Enum for Speaking exam types
 */
enum class SpeakingExamType(val displayName: String, val taskType: String) {
    TOEIC_Q11("TOEIC Speaking Q11", "Express an Opinion")
}

/**
 * Data class for Speaking Practice input
 */
data class SpeakingPracticeInput(
    val examType: SpeakingExamType = SpeakingExamType.TOEIC_Q11,
    val prompt: String = "",
    val audioFilePath: String = "",
    val transcribedText: String = ""
)

/**
 * Result from Speech-to-Text transcription
 */
data class TranscriptionResult(
    val text: String,
    val duration: Float? = null,
    val language: String? = null
)

/**
 * Response model for Azure OpenAI Transcription API
 */
data class AzureTranscriptionResponse(
    @SerializedName("text")
    val text: String?,

    @SerializedName("duration")
    val duration: Float?,

    @SerializedName("language")
    val language: String?,

    @SerializedName("error")
    val error: AzureTranscriptionError?
)

data class AzureTranscriptionError(
    @SerializedName("message")
    val message: String?,

    @SerializedName("type")
    val type: String?,

    @SerializedName("code")
    val code: String?
)

/**
 * Result from AI Speaking feedback
 */
data class SpeakingFeedbackResult(
    val examType: SpeakingExamType,
    val prompt: String,
    val transcribedText: String,
    val feedback: String,
    val overallScore: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * TOEIC Speaking Question 11 Rubric
 * Task: Express an Opinion
 * Time: 60 seconds preparation, 60 seconds to speak
 * Score: 0-5 scale
 */
object TOEICSpeakingQ11Rubric {
    const val RUBRIC = """
TOEIC Speaking Question 11 - Express an Opinion Scoring Rubric:

=== TASK DESCRIPTION ===
- Question Type: Express an opinion on a given topic
- Preparation Time: 60 seconds (45 seconds in some versions)
- Response Time: 60 seconds
- Purpose: Test ability to express and support an opinion clearly

=== SCORING SCALE (0-5) ===

Score 5 (Advanced High):
- Response is highly relevant to the question with a clear, well-developed opinion
- Provides multiple specific reasons and/or examples to support the opinion
- Ideas are logically organized with clear connections between points
- Language use is highly effective with a wide range of vocabulary
- Grammar is accurate with only minor errors that do not affect comprehension
- Pronunciation is clear and natural; stress and intonation enhance meaning
- Speech is fluent with minimal hesitation

Score 4 (Advanced Low):
- Response is relevant with a clear opinion
- Provides adequate reasons and/or examples to support the opinion
- Ideas are generally well-organized with some logical progression
- Language use is effective with good vocabulary range
- Grammar is mostly accurate with some errors that rarely affect comprehension
- Pronunciation is generally clear with occasional unclear words
- Speech is mostly fluent with some hesitation

Score 3 (Intermediate High):
- Response addresses the topic but opinion may not be fully clear
- Provides some reasons or examples but may be limited or underdeveloped
- Organization is adequate but may lack clear connections
- Language use is appropriate but vocabulary may be limited
- Grammar errors are noticeable and may occasionally affect comprehension
- Pronunciation is understandable but with some unclear portions
- Speech may have noticeable hesitations or pauses

Score 2 (Intermediate Low):
- Response is partially relevant; opinion is unclear or weakly stated
- Limited support with few or irrelevant reasons/examples
- Organization is weak or unclear
- Vocabulary is limited and may be inappropriate at times
- Frequent grammar errors that often affect comprehension
- Pronunciation issues may make some portions difficult to understand
- Speech is choppy with frequent pauses and hesitations

Score 1 (Novice):
- Response barely addresses the topic or is off-topic
- Little to no support for any opinion
- No clear organization
- Very limited vocabulary
- Grammar errors severely impede comprehension
- Pronunciation makes much of the response unintelligible
- Speech is very fragmented

Score 0:
- No response or response is completely unrelated to the topic
- Response is in a language other than English

=== KEY EVALUATION CRITERIA ===

1. CONTENT & RELEVANCE (30%):
   - Is the opinion clearly stated?
   - Are reasons/examples relevant and specific?
   - Is the response complete within the time limit?

2. ORGANIZATION & COHERENCE (20%):
   - Is there a clear structure (opinion → reasons → conclusion)?
   - Are ideas connected logically?
   - Are transitions used appropriately?

3. LANGUAGE USE & VOCABULARY (20%):
   - Is vocabulary appropriate and varied?
   - Are expressions natural and idiomatic?
   - Is the register appropriate for the context?

4. GRAMMAR & ACCURACY (15%):
   - Are sentence structures correct?
   - Are verb tenses used appropriately?
   - Do errors impede understanding?

5. DELIVERY (15%):
   - Is pronunciation clear and understandable?
   - Is speech pace appropriate (not too fast/slow)?
   - Are there excessive pauses or fillers?

=== SAMPLE QUESTION FORMATS ===
- "Do you agree or disagree with the following statement? [Statement]. Use specific reasons and examples to support your answer."
- "Some people believe [X], while others think [Y]. Which view do you agree with? Explain why."
- "What is your opinion about [topic]? Give reasons to support your view."

=== TIPS FOR HIGH SCORES ===
1. State your opinion clearly in the first sentence
2. Provide 2-3 specific reasons with examples
3. Use transition words (First, Additionally, Furthermore, In conclusion)
4. Speak at a natural pace - don't rush
5. Use the full 60 seconds but finish your thought
6. If you make a mistake, continue speaking - don't stop
"""
}
