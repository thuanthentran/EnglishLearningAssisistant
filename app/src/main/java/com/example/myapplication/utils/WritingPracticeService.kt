package com.example.myapplication.utils

import android.util.Log
import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.AzureOpenAIRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for Writing Practice feature
 * Provides AI feedback for IELTS Writing Task 2 and TOEIC Writing Q8
 */
object WritingPracticeService {
    private const val TAG = "WritingPracticeService"

    private val azureOpenAIRepository = AzureOpenAIRepository()

    /**
     * Get AI feedback for writing practice
     * @param input Writing practice input containing exam type, prompt, and optional user essay
     * @return Result containing feedback or error
     */
    suspend fun getFeedback(input: WritingPracticeInput): Result<WritingFeedbackResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting feedback for ${input.examType.displayName}, mode: ${input.getEffectiveAIMode()}")

            val prompt = buildPrompt(input)
            Log.d(TAG, "Built prompt: ${prompt.take(500)}...")

            val result = azureOpenAIRepository.generateWritingFeedback(prompt)

            result.map { feedback ->
                WritingFeedbackResult(
                    examType = input.examType,
                    aiMode = input.getEffectiveAIMode(),
                    prompt = input.prompt,
                    userEssay = input.userEssay.ifBlank { null },
                    feedback = feedback
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting feedback", e)
            Result.failure(e)
        }
    }

    /**
     * Build the prompt for Azure OpenAI based on exam type and AI mode
     */
    private fun buildPrompt(input: WritingPracticeInput): String {
        val effectiveMode = input.getEffectiveAIMode()
        val examType = input.examType
        val rubric = getRubricForExamType(examType)

        return when (effectiveMode) {
            WritingAIMode.SUGGESTION -> buildSuggestionPrompt(input, rubric)
            WritingAIMode.SCORING -> buildScoringPrompt(input, rubric)
        }
    }

    /**
     * Get the appropriate rubric for the exam type
     */
    private fun getRubricForExamType(examType: WritingExamType): String {
        return when (examType) {
            WritingExamType.IELTS_TASK2 -> IELTSWritingRubric.RUBRIC
            WritingExamType.TOEIC_Q8 -> TOEICWritingRubric.RUBRIC
        }
    }

    /**
     * Build prompt for Suggestion Mode
     * Provides ideas, structure, and tips without writing the essay
     */
    private fun buildSuggestionPrompt(input: WritingPracticeInput, rubric: String): String {
        val examSpecificContext = when (input.examType) {
            WritingExamType.IELTS_TASK2 -> """
                |=== IELTS WRITING TASK 2 CONTEXT ===
                |Exam: IELTS Academic/General Training
                |Task: Writing Task 2 (Essay)
                |Time allowed: 40 minutes
                |Minimum words: 250 words
                |Essay type: Full argumentative/discursive essay
                |
                |IELTS TASK 2 QUESTION TYPES:
                |- Opinion/Agree-Disagree: State and justify your opinion
                |- Discussion (Discuss both views): Present both sides, then give opinion
                |- Problem-Solution: Identify causes/problems and propose solutions
                |- Advantages-Disadvantages: Analyze pros and cons
                |- Two-part question: Answer two related questions
                |
                |IELTS SPECIFIC REQUIREMENTS:
                |- Academic tone and formal register
                |- Clear thesis statement in introduction
                |- Well-developed body paragraphs with examples
                |- Coherent conclusion that summarizes and restates position
                |- Use of academic vocabulary and complex sentence structures
                |- Avoid personal anecdotes; use general examples
                |
                |TYPICAL IELTS STRUCTURE:
                |1. Introduction (paraphrase topic + thesis) - 2-3 sentences
                |2. Body Paragraph 1 (main argument + support) - 5-7 sentences
                |3. Body Paragraph 2 (second argument or counterargument) - 5-7 sentences
                |4. Conclusion (summary + final thought) - 2-3 sentences
            """.trimMargin()

            WritingExamType.TOEIC_Q8 -> """
                |=== TOEIC WRITING QUESTION 8 CONTEXT ===
                |Exam: TOEIC Speaking & Writing Test
                |Task: Question 8 - Write an Opinion Essay
                |Time allowed: 30 minutes
                |Recommended words: 300+ words
                |Essay type: Opinion essay (agree/disagree format ONLY)
                |
                |TOEIC Q8 SPECIFIC FORMAT:
                |- Always an "agree or disagree" question format
                |- Often related to workplace, business, or everyday life topics
                |- Must clearly state your opinion
                |- Support with reasons AND specific examples from experience
                |
                |TOEIC SPECIFIC REQUIREMENTS:
                |- Clear, direct opinion statement
                |- Practical, real-world examples (personal or observed)
                |- Business-appropriate but accessible language
                |- Logical organization with clear transitions
                |- Can use first-person perspective and personal examples
                |- Focus on clarity over academic complexity
                |
                |TYPICAL TOEIC Q8 STRUCTURE:
                |1. Introduction: State opinion clearly (1-2 sentences)
                |2. Reason 1 + Example (detailed paragraph)
                |3. Reason 2 + Example (detailed paragraph)  
                |4. (Optional) Reason 3 or Counterpoint
                |5. Conclusion: Restate opinion (1-2 sentences)
                |
                |KEY DIFFERENCE FROM IELTS:
                |- More personal/practical examples allowed
                |- Simpler, clearer language preferred over complex academic style
                |- Business/workplace context often expected
            """.trimMargin()
        }

        return """
            |You are an English writing examiner and tutor.
            |
            |STRICT RULES:
            |- Do NOT write a full essay.
            |- Do NOT write complete paragraphs or sentences that could be copied.
            |- Only give ideas, structure suggestions, vocabulary tips, and abstract guidance.
            |- Keep suggestions at a conceptual level, not ready-to-use sentences.
            |- Respond in English.
            |
            |$examSpecificContext
            |
            |Question/Prompt: ${input.prompt}
            |
            |SCORING RUBRIC (for reference):
            |$rubric
            |
            |Please provide suggestions SPECIFIC to ${if (input.examType == WritingExamType.IELTS_TASK2) "IELTS Task 2" else "TOEIC Q8"}:
            |
            |1. **Question Type Analysis:**
            |   - Identify what type of question this is
            |   - What the question is really asking
            |
            |2. **Essay Structure Suggestion:**
            |   - Recommended paragraph breakdown for THIS exam type
            |   - What each paragraph should address (briefly)
            |
            |3. **Key Ideas to Consider:**
            |   - 2-3 main arguments or points to explore
            |   - ${if (input.examType == WritingExamType.IELTS_TASK2) "Academic examples to consider" else "Personal/workplace examples to consider"}
            |
            |4. **Vocabulary Tips:**
            |   - ${if (input.examType == WritingExamType.IELTS_TASK2) "Academic vocabulary and formal expressions" else "Clear, professional vocabulary for workplace context"}
            |   - Useful linking words and phrases
            |
            |5. **Common Pitfalls for ${if (input.examType == WritingExamType.IELTS_TASK2) "IELTS" else "TOEIC"}:**
            |   - Exam-specific mistakes to avoid
            |
            |Remember: Guide the student without writing the essay for them.
        """.trimMargin()
    }

    /**
     * Build prompt for Scoring Mode
     * Evaluates the user's essay and provides detailed feedback
     */
    private fun buildScoringPrompt(input: WritingPracticeInput, rubric: String): String {
        val examSpecificContext = when (input.examType) {
            WritingExamType.IELTS_TASK2 -> """
                |=== IELTS WRITING TASK 2 SCORING CONTEXT ===
                |Exam: IELTS Academic/General Training
                |Task: Writing Task 2 (Essay)
                |Time allowed: 40 minutes
                |Minimum words: 250 words (PENALTY if under)
                |
                |IELTS SCORING CRITERIA (each worth 25%):
                |1. Task Response: Does it fully address all parts of the question? Clear position?
                |2. Coherence & Cohesion: Logical flow, paragraphing, linking devices
                |3. Lexical Resource: Vocabulary range, accuracy, appropriateness
                |4. Grammatical Range & Accuracy: Sentence variety, error frequency
                |
                |IELTS-SPECIFIC EVALUATION POINTS:
                |- Is the register appropriately academic/formal?
                |- Are examples general/universal rather than personal anecdotes?
                |- Is there a clear thesis in the introduction?
                |- Does each body paragraph have a clear topic sentence?
                |- Does the conclusion effectively summarize without new ideas?
            """.trimMargin()

            WritingExamType.TOEIC_Q8 -> """
                |=== TOEIC WRITING QUESTION 8 SCORING CONTEXT ===
                |Exam: TOEIC Speaking & Writing Test
                |Task: Question 8 - Opinion Essay
                |Time allowed: 30 minutes
                |Recommended words: 300+ words
                |
                |TOEIC SCORING SCALE (0-5):
                |5 = Excellent: Clear opinion, well-supported, minor errors only
                |4 = Good: Clear opinion, adequate support, some errors
                |3 = Fair: Opinion present but weak support or organization issues
                |2 = Limited: Unclear opinion, poor organization, frequent errors
                |1 = Minimal: Fails to respond adequately
                |0 = No response or not in English
                |
                |TOEIC-SPECIFIC EVALUATION POINTS:
                |- Is the opinion stated clearly and directly?
                |- Are reasons supported by specific, concrete examples?
                |- Are examples from personal/workplace experience appropriate?
                |- Is the language clear and professional (not overly academic)?
                |- Is the response practical and relatable?
                |
                |KEY DIFFERENCE FROM IELTS:
                |- TOEIC allows and even expects personal examples
                |- Clarity is valued over academic complexity
                |- Business/workplace relevance is a plus
            """.trimMargin()
        }

        val scoringInstructions = when (input.examType) {
            WritingExamType.IELTS_TASK2 -> """
                |**IELTS Band Score Assessment (1-9 scale):**
                |
                |- Task Response: [X.0] / 9
                |  (Did the essay fully address all parts? Clear position throughout?)
                |
                |- Coherence and Cohesion: [X.0] / 9
                |  (Logical paragraphing? Effective use of cohesive devices?)
                |
                |- Lexical Resource: [X.0] / 9
                |  (Vocabulary range? Accuracy? Academic appropriateness?)
                |
                |- Grammatical Range and Accuracy: [X.0] / 9
                |  (Sentence variety? Error frequency and impact?)
                |
                |- **Overall Band Score: [X.0] / 9**
            """.trimMargin()

            WritingExamType.TOEIC_Q8 -> """
                |**TOEIC Score Assessment (0-5 scale):**
                |
                |- Opinion Clarity & Support: [X] / 5
                |  (Is the opinion clear? Are reasons well-supported with examples?)
                |
                |- Organization & Coherence: [X] / 5
                |  (Logical structure? Clear progression of ideas?)
                |
                |- Language Use: [X] / 5
                |  (Grammar accuracy? Vocabulary appropriateness? Clarity?)
                |
                |- **Overall Score: [X] / 5**
            """.trimMargin()
        }

        val examSpecificFeedback = when (input.examType) {
            WritingExamType.IELTS_TASK2 -> """
                |6. **IELTS-Specific Feedback:**
                |   - Is the academic register appropriate?
                |   - Are examples sufficiently general/universal?
                |   - Thesis clarity and conclusion effectiveness
                |   - Would this score well on the official IELTS test?
            """.trimMargin()

            WritingExamType.TOEIC_Q8 -> """
                |6. **TOEIC-Specific Feedback:**
                |   - Is the opinion direct and easy to identify?
                |   - Are the examples practical and relatable?
                |   - Is the language clear without being overly complex?
                |   - Would this score well on the official TOEIC test?
            """.trimMargin()
        }

        return """
            |You are an experienced ${if (input.examType == WritingExamType.IELTS_TASK2) "IELTS" else "TOEIC"} writing examiner.
            |
            |STRICT RULES:
            |- Do NOT rewrite the user's essay.
            |- Do NOT provide a model answer.
            |- Only provide scoring, feedback, and specific improvement suggestions.
            |- Point out errors with brief corrections, not full rewrites.
            |- Score according to OFFICIAL ${if (input.examType == WritingExamType.IELTS_TASK2) "IELTS" else "TOEIC"} criteria.
            |- Respond in English.
            |
            |$examSpecificContext
            |
            |Question/Prompt: ${input.prompt}
            |
            |User's Essay:
            |${input.userEssay}
            |
            |OFFICIAL SCORING RUBRIC:
            |$rubric
            |
            |=== EVALUATION FOR ${if (input.examType == WritingExamType.IELTS_TASK2) "IELTS WRITING TASK 2" else "TOEIC WRITING Q8"} ===
            |
            |1. **Score Assessment:**
            |$scoringInstructions
            |
            |2. **Strengths:**
            |   - What the essay does well
            |   - Strong points to maintain
            |
            |3. **Areas for Improvement:**
            |   - Specific weaknesses with examples from the essay
            |   - Concrete suggestions for improvement
            |
            |4. **Grammar & Vocabulary Corrections:**
            |   - List specific errors with brief corrections
            |   - Suggest better word choices where applicable
            |
            |5. **Structure & Coherence Feedback:**
            |   - How well-organized is the essay?
            |   - Suggestions for better flow and transitions
            |
            |$examSpecificFeedback
            |
            |7. **Actionable Next Steps:**
            |   - 2-3 specific things to focus on for improvement
            |   - How to better meet ${if (input.examType == WritingExamType.IELTS_TASK2) "IELTS" else "TOEIC"} requirements
            |
            |Word count: approximately ${input.userEssay.split("\\s+".toRegex()).size} words
            |${if (input.examType == WritingExamType.IELTS_TASK2 && input.userEssay.split("\\s+".toRegex()).size < 250) "⚠️ WARNING: Essay is under 250 words - this will result in a penalty on IELTS!" else ""}
        """.trimMargin()
    }

    /**
     * Check if the API is available
     */
    suspend fun isApiAvailable(): Boolean {
        return azureOpenAIRepository.isApiAvailable()
    }
}

