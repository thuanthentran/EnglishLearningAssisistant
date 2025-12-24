package com.example.myapplication.ui.vocabulary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.DictionaryWord
import com.example.myapplication.data.repository.DictionaryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DictionaryRepository(context) }
    val learnedWordsViewModel: LearnedWordsViewModel = viewModel(
        factory = LearnedWordsViewModel.Factory(context)
    )

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DictionaryWord>>(emptyList()) }
    var showOnlyLearned by remember { mutableStateOf(false) }
    var learnedWords by remember { mutableStateOf<List<DictionaryWord>>(emptyList()) }

    // Observe learned word IDs
    val learnedWordIds by learnedWordsViewModel.learnedWordIds.collectAsState()

    // Sync từ cloud khi mở màn hình từ điển
    LaunchedEffect(Unit) {
        learnedWordsViewModel.syncFromCloud()
    }

    // Load từ đã học khi bật filter hoặc khi learnedWordIds thay đổi
    LaunchedEffect(showOnlyLearned, learnedWordIds) {
        if (showOnlyLearned && learnedWordIds.isNotEmpty()) {
            learnedWords = repository.getWordsByIds(learnedWordIds)
        }
    }

    /* ✅ QUERY DB CHUẨN – KHÔNG DUPLICATE */
    LaunchedEffect(query) {
        results = if (query.length < 2) {
            emptyList()
        } else {
            repository.search(query)
        }
    }

    // Danh sách hiển thị dựa trên filter
    val displayList = if (showOnlyLearned) {
        if (query.length >= 2) {
            // Filter từ đã học theo query
            learnedWords.filter { it.word.contains(query, ignoreCase = true) }
        } else {
            learnedWords
        }
    } else {
        results
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Từ điển Anh – Việt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {

            /* 🔍 SEARCH BAR */
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                placeholder = {
                    Text(if (showOnlyLearned) "Tìm trong từ đã học..." else "Nhập từ cần tìm...")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            /* 🏷️ FILTER CHIP */
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = showOnlyLearned,
                    onClick = { showOnlyLearned = !showOnlyLearned },
                    label = {
                        Text(
                            if (showOnlyLearned)
                                "Từ đã học (${learnedWordIds.size})"
                            else
                                "Chỉ hiện từ đã học"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showOnlyLearned) Icons.Filled.Check else Icons.Default.FilterList,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF4CAF50),
                        selectedLeadingIconColor = Color(0xFF4CAF50)
                    )
                )

                if (showOnlyLearned && learnedWordIds.isEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Chưa có từ nào",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            /* ❌ NO RESULT */
            if (displayList.isEmpty()) {
                if (showOnlyLearned) {
                    if (learnedWordIds.isEmpty()) {
                        Text(
                            text = "Bạn chưa học từ nào. Hãy đánh dấu từ đã học!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (query.length >= 2) {
                        Text(
                            text = "Không tìm thấy từ đã học phù hợp",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (query.length >= 2) {
                    Text(
                        text = "Không tìm thấy từ phù hợp",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            /* 📘 RESULT LIST */
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = displayList,
                    key = { it.id }   // ⭐ BẮT BUỘC – TRÁNH DUPLICATE
                ) { word ->
                    DictionaryItem(
                        word = word,
                        isLearned = learnedWordIds.contains(word.id),
                        onToggleLearned = { learnedWordsViewModel.toggleLearned(word.id) }
                    )
                }
            }
        }
    }
}

/* =========================
   ITEM
   ========================= */
@Composable
fun DictionaryItem(
    word: DictionaryWord,
    isLearned: Boolean,
    onToggleLearned: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.word,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                if (word.phonetic.isNotBlank()) {
                    Text(
                        text = word.phonetic,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (word.type.isNotBlank()) {
                    Text(
                        text = "(${word.type})",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = word.definition,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Nút đánh dấu đã học
            IconButton(
                onClick = onToggleLearned,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isLearned) Icons.Filled.Check else Icons.Outlined.CheckCircle,
                    contentDescription = if (isLearned) "Đã học" else "Đánh dấu đã học",
                    tint = if (isLearned) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
