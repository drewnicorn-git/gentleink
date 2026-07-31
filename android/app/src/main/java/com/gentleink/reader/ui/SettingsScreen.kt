package com.gentleink.reader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gentleink.reader.filter.FilterMode
import com.gentleink.reader.filter.FilterProfile
import com.gentleink.reader.filter.GentleInkFilter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    filter: GentleInkFilter,
    mode: FilterMode,
    profile: FilterProfile,
    onModeChange: (FilterMode) -> Unit,
    onProfileChange: (FilterProfile) -> Unit,
    onBack: () -> Unit
) {
    var sample by remember { mutableStateOf("What the hell! Move your ass or I will kick your ass.") }
    val result = remember(sample, mode, profile) { filter.filterText(sample, mode, profile) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Filter settings apply to all books.", fontWeight = FontWeight.SemiBold)

            Text("Mode", fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterMode.entries.forEach { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = { onModeChange(item) },
                        label = { Text(item.name.lowercase().replaceFirstChar { it.titlecase() }) }
                    )
                }
            }

            Text("Profile", fontWeight = FontWeight.Medium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterProfile.entries.forEach { item ->
                    FilterChip(
                        selected = profile == item,
                        onClick = { onProfileChange(item) },
                        label = {
                            Text(item.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() })
                        }
                    )
                }
            }

            OutlinedTextField(
                value = sample,
                onValueChange = { sample = it },
                label = { Text("Test sample") },
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Preview", fontWeight = FontWeight.SemiBold)
                    Text(result.text, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Text(
                "Substitute replaces profanity with mild words (heck, poop, butt). " +
                    "Mask uses asterisks. Remove deletes matched words. " +
                    "DRM-protected Kindle/Kobo books must be cleaned via Calibre first.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome to GentleInk", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Wholesome reading on every page.")
        Text("• Import DRM-free EPUB files")
        Text("• Profanity is filtered as you read — substitutes like heck for hell")
        Text("• Smart context rules keep bass, assassin, and donkey references intact")
        Text("• Everything runs on your device — no cloud, no account")
        Text("Kindle or Kobo DRM books cannot be opened here. Use the Calibre plugin to pre-clean those, then import the result.")
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Get started")
        }
    }
}
