package com.gentleink.reader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gentleink.reader.epub.EpubBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    book: EpubBook,
    onBack: () -> Unit
) {
    var chapterIndex by remember(book) { mutableIntStateOf(0) }
    val chapter = book.chapters[chapterIndex]
    val progress = (chapterIndex + 1).toFloat() / book.chapters.size.coerceAtLeast(1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(book.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            chapter.title,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        enabled = chapterIndex > 0,
                        onClick = { chapterIndex -= 1 }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                    }
                    IconButton(
                        enabled = chapterIndex < book.chapters.lastIndex,
                        onClick = { chapterIndex += 1 }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Chapter ${chapterIndex + 1} of ${book.chapters.size}")
                Text("Filtered", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            }
            EpubWebView(html = chapter.html, modifier = Modifier.weight(1f))
        }
    }
}
