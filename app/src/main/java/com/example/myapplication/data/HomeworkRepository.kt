package com.example.myapplication.data

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class HomeworkRepository(private val context: Context) {
    private val dbHelper = DatabaseHelper(context)
    private val homeworkCacheDir = File(context.cacheDir, "homework_cache")

    init {
        if (!homeworkCacheDir.exists()) {
            homeworkCacheDir.mkdirs()
        }
    }

    /**
     * Lưu bài tập vào database
     * @param username: Tên người dùng
     * @param bitmap: Hình ảnh bài tập
     * @param recognizedText: Đề bài nhận diện được
     * @param solution: Lời giải
     * @return ID của bài tập được lưu
     */
    suspend fun saveHomework(
        username: String,
        bitmap: Bitmap,
        recognizedText: String,
        solution: String
    ): Long = withContext(Dispatchers.IO) {
        // Lưu ảnh vào file system
        val imagePath = saveBitmapToFile(bitmap)

        // Lưu vào database
        return@withContext dbHelper.saveHomework(
            username = username,
            imagePath = imagePath,
            recognizedText = recognizedText,
            solution = solution
        )
    }

    /**
     * Lấy tất cả bài tập của một user
     * @param username: Tên người dùng
     * @return Danh sách bài tập
     */
    suspend fun getAllHomeworkByUsername(username: String): List<HomeworkItem> =
        withContext(Dispatchers.IO) {
            return@withContext dbHelper.getAllHomeworkByUsername(username)
        }

    /**
     * Lấy bài tập theo ID
     * @param id: ID của bài tập
     * @return HomeworkItem hoặc null
     */
    suspend fun getHomeworkById(id: Long): HomeworkItem? =
        withContext(Dispatchers.IO) {
            return@withContext dbHelper.getHomeworkById(id)
        }

    /**
     * Lấy ảnh bài tập từ file path
     * @param imagePath: Đường dẫn tệp ảnh
     * @return Bitmap hoặc null
     */
    suspend fun loadHomeworkImage(imagePath: String): Bitmap? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val file = File(homeworkCacheDir, imagePath)
                if (file.exists()) {
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeworkRepository", "Error loading image: ${e.message}")
                null
            }
        }

    /**
     * Xóa bài tập theo ID
     * @param id: ID của bài tập
     * @return true nếu xóa thành công
     */
    suspend fun deleteHomework(id: Long): Boolean =
        withContext(Dispatchers.IO) {
            val homework = dbHelper.getHomeworkById(id)
            if (homework != null) {
                deleteImageFile(homework.imagePath)
            }
            return@withContext dbHelper.deleteHomework(id)
        }

    /**
     * Xóa tất cả bài tập của một user
     * @param username: Tên người dùng
     * @return true nếu xóa thành công
     */
    suspend fun deleteAllHomeworkByUsername(username: String): Boolean =
        withContext(Dispatchers.IO) {
            val homeworks = dbHelper.getAllHomeworkByUsername(username)
            homeworks.forEach { homework ->
                deleteImageFile(homework.imagePath)
            }
            return@withContext dbHelper.deleteAllHomeworkByUsername(username)
        }

    /**
     * Lấy số lượng bài tập của một user
     * @param username: Tên người dùng
     * @return Số lượng bài tập
     */
    suspend fun getHomeworkCountByUsername(username: String): Int =
        withContext(Dispatchers.IO) {
            return@withContext dbHelper.getHomeworkCountByUsername(username)
        }

    /**
     * Lưu bitmap vào file system
     * @param bitmap: Bitmap cần lưu
     * @return Tên file được lưu
     */
    private fun saveBitmapToFile(bitmap: Bitmap): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        val file = File(homeworkCacheDir, fileName)

        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }

        return fileName
    }

    /**
     * Xóa file ảnh
     * @param fileName: Tên file
     */
    private fun deleteImageFile(fileName: String) {
        try {
            val file = File(homeworkCacheDir, fileName)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeworkRepository", "Error deleting image: ${e.message}")
        }
    }
}

