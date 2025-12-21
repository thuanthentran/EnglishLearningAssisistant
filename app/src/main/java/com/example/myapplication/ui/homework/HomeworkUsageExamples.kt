package com.example.myapplication.ui.homework

import android.content.Context
import android.graphics.Bitmap
import com.example.myapplication.data.DatabaseHelper
import com.example.myapplication.data.HomeworkRepository

/**
 * File ví dụ về cách sử dụng HomeworkRepository và DatabaseHelper
 * để lấy dữ liệu từ SQLite Database
 */

// ===== CÁCH 1: Sử dụng qua Repository (RECOMMENDED) =====

suspend fun exampleSaveHomework(
    context: Context,
    username: String,
    bitmap: Bitmap,
    recognizedText: String,
    solution: String
) {
    val repository = HomeworkRepository(context)

    // Lưu bài tập (tự động lưu ảnh + metadata vào DB)
    val homeworkId = repository.saveHomework(
        username = username,
        bitmap = bitmap,
        recognizedText = recognizedText,
        solution = solution
    )

    println("Bài tập được lưu với ID: $homeworkId")
}

suspend fun exampleGetAllHomework(context: Context, username: String) {
    val repository = HomeworkRepository(context)

    // Lấy tất cả bài tập của user
    val homeworks = repository.getAllHomeworkByUsername(username)

    homeworks.forEach { homework ->
        println("ID: ${homework.id}")
        println("Đề bài: ${homework.recognizedText}")
        println("Lời giải: ${homework.solution}")
        println("Ngày tạo: ${homework.createdAt}")
        println("---")
    }
}

suspend fun exampleLoadImage(context: Context, imagePath: String) {
    val repository = HomeworkRepository(context)

    // Tải ảnh từ file system
    val bitmap = repository.loadHomeworkImage(imagePath)

    if (bitmap != null) {
        println("Ảnh được tải thành công, kích thước: ${bitmap.width}x${bitmap.height}")
    } else {
        println("Không thể tải ảnh")
    }
}

suspend fun exampleDeleteHomework(context: Context, homeworkId: Long) {
    val repository = HomeworkRepository(context)

    // Xóa bài tập (tự động xóa cả file ảnh)
    val success = repository.deleteHomework(homeworkId)

    println("Xóa bài tập: ${if (success) "Thành công" else "Thất bại"}")
}

// ===== CÁCH 2: Sử dụng trực tiếp DatabaseHelper =====

fun exampleDirectDatabaseUsage(context: Context) {
    val dbHelper = DatabaseHelper(context)

    // Lưu bài tập
    val homeworkId = dbHelper.saveHomework(
        username = "student123",
        imagePath = "uuid-file-name.jpg",  // Lưu từ file system riêng
        recognizedText = "Giải phương trình: x + 5 = 10",
        solution = "x = 5"
    )

    println("Bài tập được lưu với ID: $homeworkId")

    // Lấy tất cả bài tập
    val allHomeworks = dbHelper.getAllHomeworkByUsername("student123")
    println("Tổng số bài tập: ${allHomeworks.size}")

    // Lấy bài tập theo ID
    val homework = dbHelper.getHomeworkById(homeworkId)
    homework?.let {
        println("Đề bài: ${it.recognizedText}")
        println("Lời giải: ${it.solution}")
    }

    // Lấy số lượng bài tập
    val count = dbHelper.getHomeworkCountByUsername("student123")
    println("Số bài tập của user: $count")

    // Cập nhật bài tập
    val updated = dbHelper.updateHomework(
        id = homeworkId,
        imagePath = "new-uuid-file-name.jpg",
        recognizedText = "Giải phương trình: 2x + 5 = 15",
        solution = "x = 5"
    )

    println("Cập nhật bài tập: ${if (updated) "Thành công" else "Thất bại"}")

    // Xóa bài tập
    val deleted = dbHelper.deleteHomework(homeworkId)
    println("Xóa bài tập: ${if (deleted) "Thành công" else "Thất bại"}")

    // Xóa tất cả bài tập của user
    val allDeleted = dbHelper.deleteAllHomeworkByUsername("student123")
    println("Xóa tất cả bài tập: ${if (allDeleted) "Thành công" else "Thất bại"}")
}

// ===== CÁCH 3: Sử dụng trong ViewModel =====

/**
 * Ví dụ về cách ViewModel sử dụng Repository
 *
 * class HomeworkViewModel(
 *     private val repository: HomeworkRepository,
 *     private val currentUsername: String
 * ) : ViewModel() {
 *     private val _uiState = MutableStateFlow(HomeworkUiState())
 *     val uiState: StateFlow<HomeworkUiState> = _uiState
 *
 *     // Tự động load lịch sử khi khởi tạo
 *     init {
 *         loadPreviousSolutions()
 *     }
 *
 *     private fun loadPreviousSolutions() {
 *         viewModelScope.launch {
 *             try {
 *                 val homeworkItems = repository.getAllHomeworkByUsername(currentUsername)
 *                 val solutions = mutableListOf<HomeworkSolution>()
 *
 *                 for (item in homeworkItems) {
 *                     val bitmap = repository.loadHomeworkImage(item.imagePath)
 *                     solutions.add(
 *                         HomeworkSolution(
 *                             id = item.id.toString(),
 *                             imageBitmap = bitmap,
 *                             recognizedText = item.recognizedText,
 *                             solution = item.solution
 *                         )
 *                     )
 *                 }
 *
 *                 _uiState.value = _uiState.value.copy(
 *                     previousSolutions = solutions
 *                 )
 *             } catch (e: Exception) {
 *                 android.util.Log.e("HomeworkViewModel", "Error: ${e.message}")
 *             }
 *         }
 *     }
 *
 *     fun solveProblem() {
 *         viewModelScope.launch {
 *             try {
 *                 val bitmap = _uiState.value.currentBitmap ?: return@launch
 *
 *                 // Gọi AI để nhận diện text và tạo lời giải
 *                 val recognizedText = GeminiService.recognizeText(bitmap)
 *                 val solution = GeminiService.solveProblem(bitmap, recognizedText)
 *
 *                 // Lưu vào database
 *                 val homeworkId = repository.saveHomework(
 *                     username = currentUsername,
 *                     bitmap = bitmap,
 *                     recognizedText = recognizedText,
 *                     solution = solution
 *                 )
 *
 *                 // Update UI State
 *                 _uiState.value = _uiState.value.copy(
 *                     solution = HomeworkSolution(
 *                         id = homeworkId.toString(),
 *                         imageBitmap = bitmap,
 *                         recognizedText = recognizedText,
 *                         solution = solution
 *                     ),
 *                     isLoading = false
 *                 )
 *             } catch (e: Exception) {
 *                 _uiState.value = _uiState.value.copy(
 *                     error = "Lỗi: ${e.message}",
 *                     isLoading = false
 *                 )
 *             }
 *         }
 *     }
 *
 *     fun deleteHomework(id: String) {
 *         viewModelScope.launch {
 *             try {
 *                 repository.deleteHomework(id.toLong())
 *                 loadPreviousSolutions() // Reload
 *             } catch (e: Exception) {
 *                 _uiState.value = _uiState.value.copy(
 *                     error = "Lỗi khi xóa: ${e.message}"
 *                 )
 *             }
 *         }
 *     }
 * }
 */

// ===== CÁCH 4: Truy vấn SQL tùy chỉnh =====

fun exampleCustomQuery(context: Context) {
    val dbHelper = DatabaseHelper(context)
    val db = dbHelper.readableDatabase

    // Ví dụ: Tìm kiếm bài tập theo keyword
    val cursor = db.rawQuery(
        "SELECT * FROM ${DatabaseHelper.TABLE_HOMEWORK} " +
        "WHERE ${DatabaseHelper.HOMEWORK_USERNAME} = ? " +
        "AND ${DatabaseHelper.HOMEWORK_RECOGNIZED_TEXT} LIKE ? " +
        "ORDER BY ${DatabaseHelper.HOMEWORK_CREATED_AT} DESC",
        arrayOf("student123", "%keyword%")
    )

    while (cursor.moveToNext()) {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.HOMEWORK_ID))
        val text = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.HOMEWORK_RECOGNIZED_TEXT))
        println("ID: $id, Text: $text")
    }

    cursor.close()
    db.close()
}

// ===== NOTES =====
/**
 * ✅ Nên sử dụng: HomeworkRepository (RECOMMENDED)
 *    - Đơn giản, đã xử lý logic lưu file
 *    - An toàn, coroutine-based
 *    - Dễ test, có interface rõ ràng
 *
 * ⚠️  Dùng trực tiếp DatabaseHelper khi:
 *    - Cần thao tác SQL phức tạp
 *    - Cần performance cao
 *    - Không cần xử lý file ảnh
 *
 * ✅ Luôn dùng suspend function / coroutine
 *    - Database operations là blocking
 *    - Tránh ANR (Application Not Responding)
 *
 * ✅ Hãy close() database sau khi dùng
 *    - Tránh memory leak
 *
 * ✅ Sử dụng try-catch để xử lý exception
 *    - Database operations có thể fail
 */

