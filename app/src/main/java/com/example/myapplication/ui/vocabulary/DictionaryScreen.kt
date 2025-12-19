package com.example.myapplication.ui.vocabulary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.model.DictionaryWord
import com.example.myapplication.data.repository.DictionaryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DictionaryRepository(context) }

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DictionaryWord>>(emptyList()) }

    /* ✅ QUERY DB CHUẨN – KHÔNG DUPLICATE */
    LaunchedEffect(query) {
        results = if (query.length < 2) {
            emptyList()
        } else {
            repository.search(query)
        }
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
                placeholder = { Text("Nhập từ cần tìm...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            /* ❌ NO RESULT */
            if (results.isEmpty() && query.length >= 2) {
                Text(
                    text = "Không tìm thấy từ phù hợp",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            /* 📘 RESULT LIST */
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = results,
                    key = { it.id }   // ⭐ BẮT BUỘC – TRÁNH DUPLICATE
                ) { word ->
                    DictionaryItem(word)
                }
            }
        }
    }
}

/* =========================
   ITEM
   ========================= */
@Composable
fun DictionaryItem(word: DictionaryWord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

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
    }
}
