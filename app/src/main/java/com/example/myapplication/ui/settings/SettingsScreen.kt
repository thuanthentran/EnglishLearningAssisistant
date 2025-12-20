package com.example.myapplication.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myapplication.data.UserPreferences

// Màu sắc gradient
private val PrimaryGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
)

private val AccentGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
)

// Danh sách avatar
data class AvatarOption(
    val icon: ImageVector,
    val backgroundColor: Color,
    val name: String
)

val avatarOptions = listOf(
    AvatarOption(Icons.Default.Person, Color(0xFF667eea), "Mặc định"),
    AvatarOption(Icons.Default.Face, Color(0xFF11998e), "Vui vẻ"),
    AvatarOption(Icons.Default.SentimentSatisfied, Color(0xFFf5576c), "Hạnh phúc"),
    AvatarOption(Icons.Default.School, Color(0xFFFF9800), "Học sinh"),
    AvatarOption(Icons.Default.Star, Color(0xFFFFD700), "Ngôi sao"),
    AvatarOption(Icons.Default.EmojiEmotions, Color(0xFF9C27B0), "Cảm xúc"),
    AvatarOption(Icons.Default.Pets, Color(0xFF4CAF50), "Thú cưng"),
    AvatarOption(Icons.Default.SportsEsports, Color(0xFF2196F3), "Game thủ"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDarkModeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    // ViewModel
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(context)
    )

    // Collect states from ViewModel
    val passwordChangeState by viewModel.passwordChangeState.collectAsState()
    val forgotPasswordState by viewModel.forgotPasswordState.collectAsState()

    var isDarkMode by remember { mutableStateOf(userPreferences.isDarkMode()) }
    var selectedAvatarIndex by remember { mutableStateOf(userPreferences.getAvatarIndex()) }
    var nickname by remember { mutableStateOf(userPreferences.getNickname() ?: "") }
    var avatarUri by remember { mutableStateOf(userPreferences.getAvatarUri()) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // Handle password change success
    LaunchedEffect(passwordChangeState.isSuccess) {
        if (passwordChangeState.isSuccess) {
            showChangePasswordDialog = false
            Toast.makeText(context, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
            viewModel.resetPasswordChangeState()
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Persist permission for the URI
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Some URIs don't support persistent permissions
            }
            avatarUri = it.toString()
            userPreferences.setAvatarUri(it.toString())
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F7FA),
        animationSpec = tween(300),
        label = "backgroundColor"
    )

    val cardColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
        animationSpec = tween(300),
        label = "cardColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isDarkMode) Color.White else Color(0xFF1a1a2e),
        animationSpec = tween(300),
        label = "textColor"
    )

    val secondaryTextColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666),
        animationSpec = tween(300),
        label = "secondaryTextColor"
    )

    val dividerColor by animateColorAsState(
        targetValue = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFE8E8E8),
        animationSpec = tween(300),
        label = "dividerColor"
    )

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cài đặt",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDarkMode) Color(0xFF2D2D2D) else Color.White
                            )
                            .shadow(4.dp, CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF667eea)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Profile Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Background gradient decoration - taller for better visual
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                brush = PrimaryGradient,
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar with edit button - centered properly
                        Box(
                            modifier = Modifier.wrapContentSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, Color.White, CircleShape)
                                    .shadow(12.dp, CircleShape)
                                    .clickable { showAvatarDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarUri != null) {
                                    // Show uploaded image
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(avatarUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Avatar",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Show icon avatar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(avatarOptions[selectedAvatarIndex].backgroundColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            avatarOptions[selectedAvatarIndex].icon,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.size(60.dp),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            // Edit button - positioned at bottom end of avatar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-4).dp, y = (-4).dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF667eea))
                                    .border(3.dp, Color.White, CircleShape)
                                    .clickable { showAvatarDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Đổi avatar",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // User name - centered
                        Text(
                            text = nickname.ifEmpty { userPreferences.getUsername() ?: "Người dùng" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        // Email - centered with icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = secondaryTextColor
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = userPreferences.getEmail() ?: "Chưa có email",
                                fontSize = 15.sp,
                                color = secondaryTextColor,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // Quick action buttons - evenly distributed
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            QuickActionButton(
                                icon = Icons.Default.Edit,
                                label = "Sửa biệt danh",
                                onClick = { showNicknameDialog = true },
                                backgroundColor = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF0F0F0),
                                iconColor = Color(0xFF667eea),
                                textColor = textColor
                            )
                            QuickActionButton(
                                icon = Icons.Default.PhotoCamera,
                                label = "Đổi ảnh",
                                onClick = { showAvatarDialog = true },
                                backgroundColor = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF0F0F0),
                                iconColor = Color(0xFF11998e),
                                textColor = textColor
                            )
                            QuickActionButton(
                                icon = Icons.Default.Lock,
                                label = "Đổi mật khẩu",
                                onClick = { showChangePasswordDialog = true },
                                backgroundColor = if (isDarkMode) Color(0xFF2D2D2D) else Color(0xFFF0F0F0),
                                iconColor = Color(0xFFE57373),
                                textColor = textColor
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Settings Section
            SectionTitle(
                title = "Giao diện",
                icon = Icons.Default.Palette,
                textColor = textColor
            )

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column {
                    // Dark Mode Toggle
                    SettingsItemEnhanced(
                        icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        iconBackgroundColor = if (isDarkMode) Color(0xFF5C6BC0) else Color(0xFFFFB74D),
                        title = "Chế độ tối",
                        subtitle = if (isDarkMode) "Đang bật - Dễ chịu cho mắt" else "Đang tắt - Chế độ sáng",
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        trailing = {
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = {
                                    isDarkMode = it
                                    userPreferences.setDarkMode(it)
                                    onDarkModeChanged(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF667eea),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFBDBDBD)
                                )
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))


            // Security Section
            SectionTitle(
                title = "Bảo mật",
                icon = Icons.Default.Security,
                textColor = textColor
            )

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column {
                    // Change Password
                    SettingsItemEnhanced(
                        icon = Icons.Default.Lock,
                        iconBackgroundColor = Color(0xFFE57373),
                        title = "Đổi mật khẩu",
                        subtitle = "Thay đổi mật khẩu đăng nhập",
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        onClick = { showChangePasswordDialog = true },
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = secondaryTextColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = dividerColor,
                        thickness = 1.dp
                    )

                    // Forgot Password
                    SettingsItemEnhanced(
                        icon = Icons.Default.VpnKey,
                        iconBackgroundColor = Color(0xFF64B5F6),
                        title = "Quên mật khẩu",
                        subtitle = "Khôi phục mật khẩu qua email",
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        onClick = { showForgotPasswordDialog = true },
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = secondaryTextColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // App Info Section
            SectionTitle(
                title = "Thông tin ứng dụng",
                icon = Icons.Default.Info,
                textColor = textColor
            )

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column {
                    SettingsItemEnhanced(
                        icon = Icons.Default.NewReleases,
                        iconBackgroundColor = Color(0xFF42A5F5),
                        title = "Phiên bản",
                        subtitle = "1.0.0 (Bản mới nhất)",
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = dividerColor,
                        thickness = 1.dp
                    )

                    SettingsItemEnhanced(
                        icon = Icons.Default.Code,
                        iconBackgroundColor = Color(0xFFAB47BC),
                        title = "Phát triển bởi",
                        subtitle = "English Learning Team ❤️",
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = dividerColor,
                        thickness = 1.dp
                    )

                    SettingsItemEnhanced(
                        icon = Icons.Default.Security,
                        iconBackgroundColor = Color(0xFF66BB6A),
                        title = "Chính sách bảo mật",
                        subtitle = "Xem chi tiết về bảo mật",
                        textColor = textColor,
                        secondaryTextColor = secondaryTextColor,
                        onClick = { /* TODO: Navigate to privacy policy */ },
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = secondaryTextColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // Avatar Selection Dialog
    if (showAvatarDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarDialog = false },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = null,
                        tint = Color(0xFF667eea),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Chọn Avatar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Upload from gallery button
                    Button(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF667eea)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Tải ảnh từ thư viện",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Divider with text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.LightGray
                        )
                        Text(
                            "  hoặc chọn icon  ",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color.LightGray
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Grid of avatars - 4 columns
                    for (rowIndex in 0 until (avatarOptions.size + 3) / 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (colIndex in 0 until 4) {
                                val index = rowIndex * 4 + colIndex
                                if (index < avatarOptions.size) {
                                    AvatarItem(
                                        avatar = avatarOptions[index],
                                        isSelected = avatarUri == null && index == selectedAvatarIndex,
                                        onClick = {
                                            selectedAvatarIndex = index
                                            avatarUri = null
                                            userPreferences.setAvatarIndex(index)
                                            userPreferences.setAvatarUri(null)
                                        }
                                    )
                                } else {
                                    Spacer(Modifier.size(60.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Show option to remove custom avatar if exists
                    if (avatarUri != null) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                avatarUri = null
                                userPreferences.setAvatarUri(null)
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFE57373),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Xóa ảnh tùy chỉnh", color = Color(0xFFE57373))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAvatarDialog = false }
                ) {
                    Text(
                        "Xong",
                        color = Color(0xFF667eea),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            },
            containerColor = cardColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Nickname Dialog
    if (showNicknameDialog) {
        var tempNickname by remember { mutableStateOf(nickname) }

        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Badge,
                        contentDescription = null,
                        tint = Color(0xFF11998e),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Đặt biệt danh",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempNickname,
                        onValueChange = { tempNickname = it },
                        label = { Text("Biệt danh của bạn") },
                        placeholder = { Text("Nhập biệt danh...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF667eea),
                            focusedLabelColor = Color(0xFF667eea),
                            cursorColor = Color(0xFF667eea)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Biệt danh sẽ được hiển thị thay cho tên tài khoản",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        nickname = tempNickname
                        userPreferences.setNickname(tempNickname)
                        showNicknameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF667eea)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Lưu", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) {
                    Text("Hủy", color = Color.Gray)
                }
            },
            containerColor = cardColor,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = {
                showChangePasswordDialog = false
                viewModel.resetPasswordChangeState()
            },
            onChangePassword = { currentPassword, newPassword ->
                // Gọi Firebase để đổi mật khẩu - không cần username
                viewModel.changePassword(currentPassword, newPassword)
            },
            isLoading = passwordChangeState.isLoading,
            externalError = passwordChangeState.error,
            cardColor = cardColor,
            textColor = textColor
        )
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            onDismiss = {
                showForgotPasswordDialog = false
                viewModel.resetForgotPasswordState()
            },
            onSendReset = { email ->
                viewModel.sendPasswordResetEmail(email)
            },
            isLoading = forgotPasswordState.isLoading,
            isSuccess = forgotPasswordState.isSuccess,
            externalError = forgotPasswordState.error,
            defaultEmail = userPreferences.getEmail() ?: "",
            cardColor = cardColor,
            textColor = textColor
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: ImageVector,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF667eea),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = textColor
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    iconColor: Color,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettingsItemEnhanced(
    icon: ImageVector,
    iconBackgroundColor: Color,
    title: String,
    subtitle: String,
    textColor: Color,
    secondaryTextColor: Color,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBackgroundColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconBackgroundColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = textColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        trailing?.invoke()
    }
}

@Composable
private fun AvatarItem(
    avatar: AvatarOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(avatar.backgroundColor)
            .then(
                if (isSelected) Modifier.border(3.dp, Color(0xFF667eea), CircleShape)
                else Modifier.border(2.dp, Color.Transparent, CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            avatar.icon,
            contentDescription = avatar.name,
            modifier = Modifier.size(30.dp),
            tint = Color.White
        )

        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF667eea))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String) -> Unit,
    isLoading: Boolean = false,
    externalError: String? = null,
    cardColor: Color,
    textColor: Color
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val errorMessage = externalError ?: localError

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE57373).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFE57373),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Đổi mật khẩu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = textColor
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; localError = null },
                    label = { Text("Mật khẩu hiện tại") },
                    placeholder = { Text("Nhập mật khẩu hiện tại") },
                    singleLine = true,
                    visualTransformation = if (showCurrentPassword)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                            Icon(
                                if (showCurrentPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF667eea),
                        focusedLabelColor = Color(0xFF667eea),
                        cursorColor = Color(0xFF667eea)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; localError = null },
                    label = { Text("Mật khẩu mới") },
                    placeholder = { Text("Nhập mật khẩu mới") },
                    singleLine = true,
                    visualTransformation = if (showNewPassword)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF667eea),
                        focusedLabelColor = Color(0xFF667eea),
                        cursorColor = Color(0xFF667eea)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; localError = null },
                    label = { Text("Xác nhận mật khẩu mới") },
                    placeholder = { Text("Nhập lại mật khẩu mới") },
                    singleLine = true,
                    visualTransformation = if (showConfirmPassword)
                        androidx.compose.ui.text.input.VisualTransformation.None
                    else
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF667eea),
                        focusedLabelColor = Color(0xFF667eea),
                        cursorColor = Color(0xFF667eea)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMessage, color = Color(0xFFE57373), fontSize = 13.sp)
                }

                Spacer(Modifier.height(8.dp))
                Text("Mật khẩu phải có ít nhất 6 ký tự", fontSize = 12.sp, color = Color.Gray)

                if (isLoading) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF667eea),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Đang xử lý...", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        currentPassword.isEmpty() -> localError = "Vui lòng nhập mật khẩu hiện tại"
                        newPassword.isEmpty() -> localError = "Vui lòng nhập mật khẩu mới"
                        newPassword.length < 6 -> localError = "Mật khẩu mới phải có ít nhất 6 ký tự"
                        newPassword != confirmPassword -> localError = "Mật khẩu xác nhận không khớp"
                        else -> onChangePassword(currentPassword, newPassword)
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667eea),
                    disabledContainerColor = Color(0xFF667eea).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Đổi mật khẩu", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Hủy", color = if (isLoading) Color.Gray.copy(alpha = 0.5f) else Color.Gray)
            }
        },
        containerColor = cardColor,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onSendReset: (email: String) -> Unit,
    isLoading: Boolean = false,
    isSuccess: Boolean = false,
    externalError: String? = null,
    defaultEmail: String,
    cardColor: Color,
    textColor: Color
) {
    var email by remember { mutableStateOf(defaultEmail) }
    var localError by remember { mutableStateOf<String?>(null) }
    val errorMessage = externalError ?: localError

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF64B5F6).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isSuccess) Icons.Default.MarkEmailRead else Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isSuccess) "Email đã được gửi!" else "Quên mật khẩu",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = textColor
                )
            }
        },
        text = {
            if (isSuccess) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Chúng tôi đã gửi link đặt lại mật khẩu đến email:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        email,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color(0xFF667eea),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Vui lòng kiểm tra hộp thư đến (và cả thư rác) để tiếp tục.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column {
                    Text(
                        "Nhập email đã đăng ký để nhận link đặt lại mật khẩu",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; localError = null },
                        label = { Text("Email") },
                        placeholder = { Text("example@email.com") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF667eea),
                            focusedLabelColor = Color(0xFF667eea),
                            cursorColor = Color(0xFF667eea)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage, color = Color(0xFFE57373), fontSize = 13.sp)
                    }
                    if (isLoading) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF667eea),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Đang gửi...", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isSuccess) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Đã hiểu", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        when {
                            email.isEmpty() -> localError = "Vui lòng nhập email"
                            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                                localError = "Email không hợp lệ"
                            else -> onSendReset(email)
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF667eea),
                        disabledContainerColor = Color(0xFF667eea).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gửi email", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            if (!isSuccess) {
                TextButton(onClick = onDismiss, enabled = !isLoading) {
                    Text("Hủy", color = if (isLoading) Color.Gray.copy(alpha = 0.5f) else Color.Gray)
                }
            }
        },
        containerColor = cardColor,
        shape = RoundedCornerShape(24.dp)
    )
}
