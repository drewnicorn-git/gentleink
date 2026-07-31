package com.gentleink.reader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gentleink.reader.data.LibraryBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    books: List<LibraryBook>,
    onOpenBook: (String) -> Unit,
    onImport: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GentleInk Library") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onImport) {
                Icon(Icons.Default.Add, contentDescription = "Import EPUB")
            }
        }
    ) { padding ->
        if (books.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("No books yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Tap + to import a DRM-free EPUB. GentleInk filters profanity while you read — all processing stays on your device.",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(books, key = { it.id }) { book ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenBook(book.id) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(book.title, fontWeight = FontWeight.SemiBold)
                            book.author?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
        }
    }
}
