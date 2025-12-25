package com.example.myapplication.ui.home

import androidx.compose.ui.graphics.Color

/**
 * 🎨 BẢNG MÀU HOME SCREEN
 *
 * Chỉnh sửa các giá trị màu ở đây để thay đổi giao diện
 * Format: Color(0xFFRRGGBB) hoặc Color(0xAARRGGBB) với AA = alpha
 */
object HomeColors {

    // ==========================================
    // 🏠 NỀN & BACKGROUND
    // ==========================================

    /** Màu nền chính của màn hình */
    val ScreenBackground = Color(0xFFFAFAFA)  // Off-white, thay đổi thành Color.White nếu muốn trắng tinh

    /** Màu nền Bottom Navigation */
    val BottomNavBackground = Color.White

    /** Màu nền Study Progress Card */
    val StudyCardBackground = Color(0xFFD5E5F6)  // Xanh nhạt


    // ==========================================
    // 🔵 MÀU CHÍNH (Primary)
    // ==========================================

    /** Màu xanh chính - dùng cho icon active, progress bar */
    val Primary = Color(0xFF5B86E5)

    /** Màu gradient tím - đầu */
    val GradientStart = Color(0xFF667eea)

    /** Màu gradient tím - cuối */
    val GradientEnd = Color(0xFF764ba2)


    // ==========================================
    // 📊 HEADER METRICS
    // ==========================================

    /** Màu nền pill chứa metrics */
    val MetricsPillBackground = Color.White

    /** Màu icon Words Learned */
    val WordsIconBackground = Color(0xFF4FC3F7)  // Xanh dương nhạt

    /** Màu icon Streak */
    val StreakIconBackground = Color(0xFFFFD54F)  // Vàng


    // ==========================================
    // 📈 PROGRESS & CHART
    // ==========================================

    /** Màu progress bar (filled) */
    val ProgressBarFilled = Color(0xFF5B86E5)

    /** Màu progress bar (empty/background) */
    val ProgressBarEmpty = Color(0xFFE8EDF3)

    /** Màu chart bar cao (>60%) */
    val ChartBarHigh = Color(0xFF5B86E5)

    /** Màu chart bar thấp */
    val ChartBarLow = Color(0xFF5B86E5).copy(alpha = 0.3f)

    /** Màu tăng (positive) */
    val PositiveGreen = Color(0xFF48BB78)

    /** Màu giảm (negative) */
    val NegativeRed = Color(0xFFE53E3E)


    // ==========================================
    // 🎯 ACTIVITY CARDS
    // ==========================================

    /** Màu nền card Dictionary */
    val DictionaryCardBackground = Color(0xFFFFF279)  // Vàng nhạt

    /** Màu nền card Games */
    val GamesCardBackground = Color(0xFFD5F5F0)  // Mint nhạt

    /** Màu nền card Learn Words */
    val LearnWordsCardBackground = Color(0xFFD6E9FF)  // Xanh nhạt

    /** Màu nền icon trong Activity card */
    val ActivityIconBackground = Color.White.copy(alpha = 0.9f)


    // ==========================================
    // 📝 TEXT COLORS
    // ==========================================

    /** Màu text chính (tiêu đề) */
    val TextPrimary = Color(0xFF2D3748)

    /** Màu text phụ (subtitle, caption) */
    val TextSecondary = Color(0xFF718096)

    /** Màu text trên nền tối */
    val TextOnDark = Color.White


    // ==========================================
    // 🔘 BOTTOM NAVIGATION
    // ==========================================

    /** Màu icon khi được chọn (active) */
    val NavIconActive = Color(0xFF5B86E5)

    /** Màu background icon active */
    val NavIconActiveBackground = Color(0xFF5B86E5)

    /** Màu icon khi không chọn (inactive) */
    val NavIconInactive = Color(0xFF9E9E9E)


    // ==========================================
    // 🎨 BO TRÒN (CORNER RADIUS)
    // ==========================================

    /** Bo tròn cho Activity cards */
    const val ActivityCardRadius = 24  // dp

    /** Bo tròn cho Study Progress card */
    const val StudyCardRadius = 24  // dp

    /** Bo tròn cho Metrics pill */
    const val MetricsPillRadius = 24  // dp

    /** Bo tròn cho buttons */
    const val ButtonRadius = 16  // dp
}

