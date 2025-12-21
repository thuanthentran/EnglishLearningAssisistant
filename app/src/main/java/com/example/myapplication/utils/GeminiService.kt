package com.example.myapplication.utils

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {
    private val model = GenerativeModel(
        modelName = ApiConfig.GEMINI_MODEL,
        apiKey = ApiConfig.GEMINI_API_KEY
    )

    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text("Vui lòng nhận diện và trích xuất toàn bộ nội dung bài tập từ hình ảnh này. Trả lại chính xác toàn bộ text của bài tập.")
                }
            )
            response.text ?: "Không thể nhận diện text từ ảnh"
        } catch (e: Exception) {
            "Lỗi: ${e.message}"
        }
    }

    suspend fun solveProblem(bitmap: Bitmap, problemText: String): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = model.generateContent(
                content {
                    image(bitmap)
                    text("""
                        Đây là bài tập sau:
                        
                        $problemText
                        
                        Vui lòng:
                        1. Phân tích chi tiết bài toán
                        2. Cung cấp lời giải từng bước
                        3. Giải thích các công thức hoặc khái niệm được sử dụng
                        4. Đưa ra kết luận cuối cùng
                        
                        Trả lời bằng tiếng Việt, rõ ràng và dễ hiểu.
                    """.trimIndent())
                }
            )
            response.text ?: "Không thể tạo lời giải"
        } catch (e: Exception) {
            "Lỗi: ${e.message}"
        }
    }
}

