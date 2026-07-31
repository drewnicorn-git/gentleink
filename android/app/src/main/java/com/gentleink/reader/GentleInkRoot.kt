package com.gentleink.reader

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gentleink.reader.epub.EpubFilterPipeline
import com.gentleink.reader.epub.EpubParser
import com.gentleink.reader.filter.FilterMode
import com.gentleink.reader.filter.FilterProfile
import com.gentleink.reader.ui.LibraryScreen
import com.gentleink.reader.ui.OnboardingScreen
import com.gentleink.reader.ui.ReaderScreen
import com.gentleink.reader.ui.SettingsScreen
import com.gentleink.reader.ui.theme.GentleInkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun GentleInkRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as GentleInkApp
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val mode by app.settings.filterMode.collectAsState(initial = FilterMode.SUBSTITUTE)
    val profile by app.settings.filterProfile.collectAsState(initial = FilterProfile.FAMILY)
    val onboardingDone by app.settings.onboardingDone.collectAsState(initial = false)

    var books by remember { mutableStateOf(app.books.listBooks()) }
    var openBook by remember { mutableStateOf<com.gentleink.reader.epub.EpubBook?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun refreshLibrary() {
        books = app.books.listBooks()
    }

    fun loadAndOpen(bookId: String) {
        scope.launch {
            loading = true
            runCatching {
                withContext(Dispatchers.IO) {
                    val file = app.books.bookFile(bookId) ?: error("Book not found")
                    val parsed = EpubParser.parse(file)
                    EpubFilterPipeline.filterBook(parsed) { text ->
                        app.filter.filterText(text, mode, profile).text
                    }
                }
            }.onSuccess { filtered ->
                openBook = filtered
                navController.navigate("reader")
            }
            loading = false
        }
    }

    GentleInkTheme {
        NavHost(
            navController = navController,
            startDestination = if (onboardingDone) "library" else "onboarding"
        ) {
            composable("onboarding") {
                OnboardingScreen {
                    scope.launch { app.settings.setOnboardingDone() }
                    navController.navigate("library") { popUpTo("onboarding") { inclusive = true } }
                }
            }
            composable("library") {
                LibraryScreen(
                    books = books,
                    onOpenBook = { loadAndOpen(it) },
                    onImport = { navController.navigate("import") },
                    onSettings = { navController.navigate("settings") }
                )
            }
            composable("import") {
                ImportHandler(
                    onImported = {
                        refreshLibrary()
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    filter = app.filter,
                    mode = mode,
                    profile = profile,
                    onModeChange = { scope.launch { app.settings.setFilterMode(it) } },
                    onProfileChange = { scope.launch { app.settings.setFilterProfile(it) } },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("reader") {
                openBook?.let { book ->
                    ReaderScreen(book = book, onBack = {
                        openBook = null
                        navController.popBackStack()
                    })
                }
            }
        }
    }
}

@Composable
private fun ImportHandler(onImported: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as GentleInkApp
    val scope = rememberCoroutineScope()

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            onCancel()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        val parsed = EpubParser.parseBytes(bytes)
                        app.books.importBook(
                            sourceBytes = bytes,
                            originalName = uri.lastPathSegment ?: "book.epub",
                            title = parsed.title,
                            author = parsed.author
                        )
                    } ?: error("Could not read file")
                }
            }
            onImported()
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        launcher.launch(arrayOf("application/epub+zip", "application/octet-stream"))
    }
}
