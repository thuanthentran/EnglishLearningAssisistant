package com.example.myapplication.ui.imagelearning

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.util.concurrent.Executors
import android.speech.tts.TextToSpeech
import java.util.*

// Data class for detected objects
data class DetectedObject(
    val englishName: String,
    val confidence: Float,
    val vietnameseName: String = ""
)

// Vietnamese translations for common objects
val objectTranslations = mapOf(
    // Animals
    "cat" to "con mèo",
    "dog" to "con chó",
    "bird" to "con chim",
    "fish" to "con cá",
    "horse" to "con ngựa",
    "cow" to "con bò",
    "sheep" to "con cừu",
    "elephant" to "con voi",
    "lion" to "sư tử",
    "tiger" to "con hổ",
    "bear" to "con gấu",
    "monkey" to "con khỉ",
    "rabbit" to "con thỏ",
    "mouse" to "con chuột",
    "duck" to "con vịt",
    "chicken" to "con gà",
    "butterfly" to "con bướm",
    "insect" to "côn trùng",
    "pet" to "thú cưng",
    "animal" to "động vật",

    // Food & Drinks
    "food" to "thức ăn",
    "fruit" to "trái cây",
    "vegetable" to "rau củ",
    "apple" to "quả táo",
    "banana" to "quả chuối",
    "orange" to "quả cam",
    "bread" to "bánh mì",
    "rice" to "gạo/cơm",
    "meat" to "thịt",
    "coffee" to "cà phê",
    "tea" to "trà",
    "water" to "nước",
    "milk" to "sữa",
    "juice" to "nước ép",
    "cake" to "bánh ngọt",
    "pizza" to "bánh pizza",
    "sandwich" to "bánh sandwich",
    "salad" to "salad",
    "soup" to "súp",
    "egg" to "trứng",
    "cheese" to "phô mai",
    "drink" to "đồ uống",
    "beverage" to "đồ uống",

    // Objects & Items
    "phone" to "điện thoại",
    "computer" to "máy tính",
    "laptop" to "máy tính xách tay",
    "keyboard" to "bàn phím",
    "mouse" to "chuột máy tính",
    "screen" to "màn hình",
    "table" to "cái bàn",
    "chair" to "cái ghế",
    "book" to "quyển sách",
    "pen" to "cây bút",
    "pencil" to "bút chì",
    "paper" to "giấy",
    "bag" to "cái túi",
    "clock" to "đồng hồ",
    "watch" to "đồng hồ đeo tay",
    "glasses" to "kính mắt",
    "bottle" to "chai",
    "cup" to "cái cốc",
    "plate" to "cái đĩa",
    "bowl" to "cái bát",
    "spoon" to "cái thìa",
    "fork" to "cái nĩa",
    "knife" to "con dao",
    "key" to "chìa khóa",
    "door" to "cửa",
    "window" to "cửa sổ",
    "bed" to "giường",
    "pillow" to "gối",
    "blanket" to "chăn",
    "lamp" to "đèn",
    "light" to "ánh sáng",
    "television" to "tivi",
    "camera" to "máy ảnh",
    "umbrella" to "ô/dù",
    "wallet" to "ví",
    "money" to "tiền",
    "card" to "thẻ",
    "toy" to "đồ chơi",
    "ball" to "quả bóng",
    "doll" to "búp bê",
    "game" to "trò chơi",
    "mirror" to "gương",
    "soap" to "xà phòng",
    "towel" to "khăn",
    "brush" to "bàn chải",
    "comb" to "lược",

    // Vehicles
    "car" to "xe hơi",
    "bus" to "xe buýt",
    "bicycle" to "xe đạp",
    "motorcycle" to "xe máy",
    "train" to "tàu hỏa",
    "airplane" to "máy bay",
    "boat" to "thuyền",
    "ship" to "tàu thủy",
    "truck" to "xe tải",
    "taxi" to "taxi",
    "vehicle" to "phương tiện",
    "wheel" to "bánh xe",

    // Nature
    "tree" to "cây",
    "flower" to "hoa",
    "grass" to "cỏ",
    "leaf" to "lá",
    "mountain" to "núi",
    "river" to "sông",
    "lake" to "hồ",
    "sea" to "biển",
    "beach" to "bãi biển",
    "forest" to "rừng",
    "sky" to "bầu trời",
    "cloud" to "mây",
    "sun" to "mặt trời",
    "moon" to "mặt trăng",
    "star" to "ngôi sao",
    "rain" to "mưa",
    "snow" to "tuyết",
    "plant" to "cây cối",
    "garden" to "vườn",
    "park" to "công viên",

    // Clothing
    "shirt" to "áo sơ mi",
    "pants" to "quần",
    "dress" to "váy",
    "shoes" to "giày",
    "hat" to "mũ",
    "coat" to "áo khoác",
    "jacket" to "áo jacket",
    "sock" to "tất",
    "glove" to "găng tay",
    "scarf" to "khăn quàng",
    "tie" to "cà vạt",
    "belt" to "dây lưng",
    "clothing" to "quần áo",
    "fashion" to "thời trang",

    // People & Body
    "person" to "người",
    "man" to "đàn ông",
    "woman" to "phụ nữ",
    "child" to "trẻ em",
    "baby" to "em bé",
    "face" to "khuôn mặt",
    "hand" to "bàn tay",
    "eye" to "mắt",
    "nose" to "mũi",
    "mouth" to "miệng",
    "ear" to "tai",
    "hair" to "tóc",
    "head" to "đầu",
    "body" to "cơ thể",
    "arm" to "cánh tay",
    "leg" to "chân",
    "foot" to "bàn chân",
    "finger" to "ngón tay",
    "smile" to "nụ cười",
    "family" to "gia đình",
    "friend" to "bạn bè",
    "people" to "mọi người",
    "crowd" to "đám đông",

    // Buildings & Places
    "house" to "nhà",
    "building" to "tòa nhà",
    "school" to "trường học",
    "hospital" to "bệnh viện",
    "office" to "văn phòng",
    "store" to "cửa hàng",
    "restaurant" to "nhà hàng",
    "hotel" to "khách sạn",
    "church" to "nhà thờ",
    "temple" to "đền/chùa",
    "bridge" to "cầu",
    "road" to "đường",
    "street" to "phố",
    "city" to "thành phố",
    "village" to "làng",
    "room" to "phòng",
    "kitchen" to "nhà bếp",
    "bathroom" to "phòng tắm",
    "bedroom" to "phòng ngủ",

    // Technology & Electronics
    "technology" to "công nghệ",
    "electronics" to "đồ điện tử",
    "device" to "thiết bị",
    "machine" to "máy móc",
    "robot" to "robot",
    "internet" to "internet",
    "software" to "phần mềm",
    "hardware" to "phần cứng",
    "battery" to "pin",
    "charger" to "sạc",
    "cable" to "dây cáp",
    "speaker" to "loa",
    "headphone" to "tai nghe",
    "microphone" to "micro",
    "printer" to "máy in",
    "scanner" to "máy quét",

    // Sports & Activities
    "sport" to "thể thao",
    "football" to "bóng đá",
    "basketball" to "bóng rổ",
    "tennis" to "quần vợt",
    "swimming" to "bơi lội",
    "running" to "chạy bộ",
    "gym" to "phòng gym",
    "exercise" to "tập thể dục",
    "game" to "trò chơi",
    "music" to "âm nhạc",
    "dance" to "nhảy múa",
    "art" to "nghệ thuật",
    "painting" to "tranh vẽ",
    "photography" to "nhiếp ảnh",

    // Others
    "indoor" to "trong nhà",
    "outdoor" to "ngoài trời",
    "text" to "văn bản",
    "pattern" to "hoa văn",
    "design" to "thiết kế",
    "color" to "màu sắc",
    "shape" to "hình dạng",
    "size" to "kích thước",
    "number" to "số",
    "letter" to "chữ cái",
    "symbol" to "ký hiệu",
    "sign" to "biển hiệu",
    "logo" to "logo",
    "brand" to "thương hiệu",
    "product" to "sản phẩm",
    "package" to "gói hàng",
    "box" to "hộp",
    "container" to "hộp đựng"
)

// Function to get Vietnamese translation
fun getVietnameseTranslation(englishWord: String): String {
    val lowerWord = englishWord.lowercase(Locale.getDefault())
    return objectTranslations[lowerWord] ?: "Chưa có bản dịch"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageLearningScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedObjects by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Text-to-Speech
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    // Initialize TTS
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // ML Kit Image Labeler
    val imageLabeler = remember {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.5f)
                .build()
        )
    }

    // Cleanup labeler when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            imageLabeler.close()
        }
    }

    // Permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
        } else {
            Toast.makeText(context, "Cần quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show()
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            selectedBitmap = loadBitmapFromUri(context, it)
            detectedObjects = emptyList()
            errorMessage = null
        }
    }

    // Function to analyze image
    fun analyzeImage(bitmap: Bitmap) {
        isLoading = true
        errorMessage = null

        val inputImage = InputImage.fromBitmap(bitmap, 0)

        imageLabeler.process(inputImage)
            .addOnSuccessListener { labels ->
                detectedObjects = labels.map { label ->
                    DetectedObject(
                        englishName = label.text,
                        confidence = label.confidence,
                        vietnameseName = getVietnameseTranslation(label.text)
                    )
                }.sortedByDescending { it.confidence }
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("ImageLearning", "Error detecting objects", e)
                errorMessage = "Không thể phân tích ảnh: ${e.message}"
                isLoading = false
            }
    }

    // Function to speak text
    fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    if (showCamera) {
        CameraScreen(
            onImageCaptured = { bitmap ->
                selectedBitmap = bitmap
                selectedImageUri = null
                showCamera = false
                detectedObjects = emptyList()
                errorMessage = null
            },
            onBack = { showCamera = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Học qua Hình ảnh",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Image Selection Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Camera Button
                    Button(
                        onClick = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF667eea)
                        )
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Chụp ảnh")
                    }

                    // Gallery Button
                    Button(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                        },
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF764ba2)
                        )
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Chọn ảnh")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display selected image
                if (selectedBitmap != null || selectedImageUri != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            selectedBitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Selected Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Analyze Button
                    Button(
                        onClick = {
                            selectedBitmap?.let { analyzeImage(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && selectedBitmap != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF11998e)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Đang phân tích...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Nhận diện vật thể")
                        }
                    }
                } else {
                    // Placeholder when no image is selected
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Chụp hoặc chọn ảnh để bắt đầu",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Error message
                errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detected Objects List
                if (detectedObjects.isNotEmpty()) {
                    Text(
                        "Các vật thể được phát hiện:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(detectedObjects) { obj ->
                            DetectedObjectCard(
                                detectedObject = obj,
                                onSpeak = { speakText(obj.englishName) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetectedObjectCard(
    detectedObject: DetectedObject,
    onSpeak: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Confidence indicator
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF667eea),
                                Color(0xFF764ba2)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${(detectedObject.confidence * 100).toInt()}%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // English and Vietnamese names
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = detectedObject.englishName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = detectedObject.vietnameseName,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Speak button
            IconButton(
                onClick = onSpeak,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF11998e).copy(alpha = 0.1f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Phát âm",
                    tint = Color(0xFF11998e)
                )
            }
        }
    }
}

@Composable
fun CameraScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder()
                            .build()
                            .also { it.setSurfaceProvider(surfaceProvider) }

                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "Use case binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            }
        )

        // Camera controls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = Color.White
                )
            }

            // Capture button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        val capture = imageCapture ?: return@IconButton

                        val photoFile = File(
                            context.cacheDir,
                            "IMG_${System.currentTimeMillis()}.jpg"
                        )

                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        capture.takePicture(
                            outputOptions,
                            cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                    bitmap?.let {
                                        // Run on main thread
                                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                                            onImageCaptured(it)
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("CameraScreen", "Photo capture failed", exception)
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        Toast.makeText(
                                            context,
                                            "Không thể chụp ảnh: ${exception.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF667eea))
                    )
                }
            }
        }
    }
}

// Helper function to load bitmap from URI
fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        Log.e("ImageLearning", "Error loading bitmap", e)
        null
    }
}

