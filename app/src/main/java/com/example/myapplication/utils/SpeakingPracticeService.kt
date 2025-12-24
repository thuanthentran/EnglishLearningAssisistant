package com.example.myapplication.utils

import android.util.Log
import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.AzureOpenAIRepository
import com.example.myapplication.data.repository.AzureSpeechToTextRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Service for Speaking Practice feature
 * Provides Speech-to-Text transcription and AI scoring for TOEIC Speaking Q11
 */
object SpeakingPracticeService {
    private const val TAG = "SpeakingPracticeService"

    private val speechToTextRepository = AzureSpeechToTextRepository()
    private val azureOpenAIRepository = AzureOpenAIRepository()

    /**
     * Transcribe audio file to text
     * @param audioFile The audio file to transcribe
     * @return Result containing transcription or error
     */
    suspend fun transcribeAudio(audioFile: File): Result<TranscriptionResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting audio transcription for: ${audioFile.name}")
            speechToTextRepository.transcribeAudio(audioFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error in transcription", e)
            Result.failure(e)
        }
    }

    /**
     * Get AI feedback/scoring for speaking practice
     * @param input Speaking practice input containing prompt and transcribed text
     * @return Result containing feedback or error
     */
    suspend fun getSpeakingFeedback(input: SpeakingPracticeInput): Result<SpeakingFeedbackResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting speaking feedback for ${input.examType.displayName}")

            if (input.transcribedText.isBlank()) {
                return@withContext Result.failure(Exception("No transcribed text to evaluate"))
            }

            val prompt = buildScoringPrompt(input)
            Log.d(TAG, "Built scoring prompt: ${prompt.take(500)}...")

            val result = azureOpenAIRepository.generateWritingFeedback(prompt)

            result.map { feedback ->
                // Try to extract overall score from feedback
                val overallScore = extractOverallScore(feedback)

                SpeakingFeedbackResult(
                    examType = input.examType,
                    prompt = input.prompt,
                    transcribedText = input.transcribedText,
                    feedback = feedback,
                    overallScore = overallScore
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting speaking feedback", e)
            Result.failure(e)
        }
    }

    /**
     * Complete flow: Transcribe audio and then score it
     * @param audioFile The audio file to process
     * @param prompt The speaking question/prompt
     * @param examType The exam type
     * @return Result containing feedback or error
     */
    suspend fun transcribeAndScore(
        audioFile: File,
        prompt: String,
        examType: SpeakingExamType = SpeakingExamType.TOEIC_Q11
    ): Result<SpeakingFeedbackResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting transcribe and score flow")

            // Step 1: Transcribe audio
            val transcriptionResult = transcribeAudio(audioFile)
            if (transcriptionResult.isFailure) {
                return@withContext Result.failure(
                    transcriptionResult.exceptionOrNull() ?: Exception("Transcription failed")
                )
            }

            val transcription = transcriptionResult.getOrNull()!!
            Log.d(TAG, "Transcription successful: ${transcription.text.take(100)}...")

            // Step 2: Score the transcribed text
            val input = SpeakingPracticeInput(
                examType = examType,
                prompt = prompt,
                transcribedText = transcription.text
            )

            getSpeakingFeedback(input)
        } catch (e: Exception) {
            Log.e(TAG, "Error in transcribe and score flow", e)
            Result.failure(e)
        }
    }

    /**
     * Build the scoring prompt for Azure OpenAI
     */
    private fun buildScoringPrompt(input: SpeakingPracticeInput): String {
        val rubric = TOEICSpeakingQ11Rubric.RUBRIC

        return """
            |You are an expert TOEIC Speaking examiner with extensive experience in evaluating spoken English responses.
            |Your task is to evaluate a transcribed spoken response for TOEIC Speaking Question 11 (Express an Opinion).
            |
            |=== IMPORTANT NOTES ===
            |- The text below is a TRANSCRIPTION of spoken audio, so it may contain:
            |  * Natural speech patterns (fillers like "um", "uh", "you know")
            |  * Repetitions or self-corrections
            |  * Informal language typical of spoken English
            |- Evaluate based on the CONTENT and COMMUNICATION effectiveness
            |- Consider that this is a 60-second timed response
            |
            |=== QUESTION/PROMPT ===
            |${input.prompt}
            |
            |=== TRANSCRIBED RESPONSE ===
            |${input.transcribedText}
            |
            |=== SCORING RUBRIC ===
            |$rubric
            |
            |=== EVALUATION TASK ===
            |Please provide a comprehensive evaluation including:
            |
            |1. **OVERALL SCORE: [X/5]**
            |   - Give a clear score from 0-5 based on the rubric
            |
            |2. **CONTENT & RELEVANCE (Score: X/5)**
            |   - Is the opinion clearly stated?
            |   - Are the supporting reasons/examples relevant and specific?
            |   - Does the response fully address the question?
            |
            |3. **ORGANIZATION & COHERENCE (Score: X/5)**
            |   - Is there a clear structure?
            |   - Are ideas connected logically?
            |   - Are transitions used effectively?
            |
            |4. **LANGUAGE USE & VOCABULARY (Score: X/5)**
            |   - Is vocabulary appropriate and varied?
            |   - Are expressions natural?
            |
            |5. **GRAMMAR & ACCURACY (Score: X/5)**
            |   - Are sentence structures correct?
            |   - Do errors impede understanding?
            |
            |6. **DELIVERY NOTES** (Based on transcription patterns)
            |   - Comments on speech fluency indicators from the transcription
            |   - Note any excessive fillers or hesitation patterns
            |
            |7. **STRENGTHS:**
            |   - List 2-3 specific things done well
            |
            |8. **AREAS FOR IMPROVEMENT:**
            |   - List 2-3 specific areas to work on
            |
            |9. **SAMPLE IMPROVED RESPONSE:**
            |   - Provide a brief model response (2-3 sentences) showing how to improve
            |
            |Please be constructive, specific, and encouraging in your feedback.
            |Respond in English.
        """.trimMargin()
    }

    /**
     * Extract the overall score from the feedback text
     */
    private fun extractOverallScore(feedback: String): Int? {
        // Try to find patterns like "OVERALL SCORE: 4/5" or "Overall Score: 3/5"
        val patterns = listOf(
            Regex("""OVERALL\s*SCORE[:\s]*(\d)[/\s]*5""", RegexOption.IGNORE_CASE),
            Regex("""Overall\s*Score[:\s]*(\d)[/\s]*5""", RegexOption.IGNORE_CASE),
            Regex("""Score[:\s]*(\d)[/\s]*5""", RegexOption.IGNORE_CASE),
            Regex("""\[(\d)/5\]""")
        )

        for (pattern in patterns) {
            val match = pattern.find(feedback)
            if (match != null) {
                val score = match.groupValues[1].toIntOrNull()
                if (score != null && score in 0..5) {
                    return score
                }
            }
        }
        return null
    }

    /**
     * Check if the service is properly configured
     */
    fun isConfigured(): Boolean {
        return speechToTextRepository.isConfigured()
    }
}

