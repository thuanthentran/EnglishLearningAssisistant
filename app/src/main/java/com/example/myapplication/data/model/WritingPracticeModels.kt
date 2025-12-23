package com.example.myapplication.data.model

/**
 * Enum for exam types supported in Writing Practice
 */
enum class WritingExamType(val displayName: String, val essayType: String) {
    IELTS_TASK2("IELTS Writing Task 2", "Full Essay"),
    TOEIC_Q8("TOEIC Writing Q8", "Opinion Essay")
}

/**
 * AI Mode for Writing Practice
 */
enum class WritingAIMode(val displayName: String) {
    SUGGESTION("Suggestion Mode"),
    SCORING("Scoring Mode")
}

/**
 * Data class for Writing Practice input
 */
data class WritingPracticeInput(
    val examType: WritingExamType = WritingExamType.IELTS_TASK2,
    val prompt: String = "",
    val userEssay: String = "",
    val aiMode: WritingAIMode = WritingAIMode.SUGGESTION
) {
    /**
     * Automatically determine AI mode based on whether user has provided an essay
     */
    fun getEffectiveAIMode(): WritingAIMode {
        return if (userEssay.isNotBlank()) WritingAIMode.SCORING else WritingAIMode.SUGGESTION
    }
}

/**
 * Result from AI feedback
 */
data class WritingFeedbackResult(
    val examType: WritingExamType,
    val aiMode: WritingAIMode,
    val prompt: String,
    val userEssay: String?,
    val feedback: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * IELTS Writing Task 2 Rubric
 */
object IELTSWritingRubric {
    const val RUBRIC = """
IELTS Writing Task 2 Band Descriptors:

Band 9:
- Task Response: Fully addresses all parts, presents a fully developed position with relevant, extended ideas
- Coherence & Cohesion: Uses cohesion in such a way that it attracts no attention; skilfully manages paragraphing
- Lexical Resource: Uses a wide range of vocabulary with very natural and sophisticated control of lexical features
- Grammatical Range & Accuracy: Uses a wide range of structures with full flexibility and accuracy

Band 8:
- Task Response: Sufficiently addresses all parts, presents a well-developed response with relevant ideas
- Coherence & Cohesion: Sequences information logically, manages all aspects of cohesion well
- Lexical Resource: Uses a wide range of vocabulary fluently and flexibly; rare minor errors
- Grammatical Range & Accuracy: Uses a wide range of structures; the majority error-free

Band 7:
- Task Response: Addresses all parts, presents a clear position with extended ideas
- Coherence & Cohesion: Logically organizes information; uses a range of cohesive devices
- Lexical Resource: Uses a sufficient range of vocabulary with some flexibility
- Grammatical Range & Accuracy: Uses a variety of complex structures; frequent error-free sentences

Band 6:
- Task Response: Addresses all parts though some may be more fully covered; relevant main ideas but some may be underdeveloped
- Coherence & Cohesion: Arranges information coherently; uses cohesive devices effectively but may be faulty
- Lexical Resource: Uses an adequate range of vocabulary; some errors in word choice but not impede communication
- Grammatical Range & Accuracy: Uses a mix of simple and complex sentence forms; some errors but rarely impede communication

Band 5:
- Task Response: Addresses the task only partially; format may be inappropriate
- Coherence & Cohesion: Presents information with some organisation; may lack overall progression
- Lexical Resource: Uses a limited range of vocabulary; noticeable errors in spelling
- Grammatical Range & Accuracy: Uses only a limited range of structures; errors may cause some difficulty

Criteria Weight:
- Task Response: 25%
- Coherence and Cohesion: 25%
- Lexical Resource: 25%
- Grammatical Range and Accuracy: 25%
"""
}

/**
 * TOEIC Writing Q8 Rubric
 */
object TOEICWritingRubric {
    const val RUBRIC = """
TOEIC Writing Question 8 (Opinion Essay) Scoring Rubric:

Score 5 (Excellent):
- Relevant opinions and well-supported reasons/examples
- Well-organized response with clear progression
- Uses a variety of sentence structures and vocabulary
- Uses grammar and vocabulary accurately with minor errors
- Consistent use of language appropriate for the task

Score 4 (Good):
- Relevant opinions with adequate support
- Adequately organized with some progression
- Uses a variety of sentence structures and vocabulary
- Some grammatical and vocabulary errors, but meaning is clear
- Generally appropriate language use

Score 3 (Fair):
- Opinions may be vague or only partially supported
- Organization may be limited or inconsistent
- Limited variety in sentence structures and vocabulary
- Errors may obscure meaning at times
- May have inappropriate language use at times

Score 2 (Limited):
- Opinions unclear or poorly supported
- Poorly organized or lacks progression
- Limited vocabulary and sentence structures
- Frequent errors that obscure meaning
- Inappropriate language use

Score 1 (Minimal):
- Does not respond to the task
- Severely limited vocabulary and sentence structures
- Numerous errors throughout
- Meaning is unclear

Score 0:
- No response or response is not in English

Key Evaluation Areas:
- Quality and relevance of opinions
- Organization and coherence
- Grammar accuracy
- Vocabulary range and appropriateness
- Task completion (minimum 300 words recommended)
"""
}

