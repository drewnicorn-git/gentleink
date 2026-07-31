package com.gentleink.reader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var modeExpanded by remember { mutableStateOf(false) }
    var profileExpanded by remember { mutableStateOf(false) }

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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = modeExpanded, onExpandedChange = { modeExpanded = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        readOnly = true,
                        value = mode.name.lowercase().replaceFirstChar { it.titlecase() },
                        onValueChange = {},
                        label = { Text("Mode") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                        FilterMode.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name.lowercase().replaceFirstChar { c -> c.titlecase() }) },
                                onClick = { onModeChange(item); modeExpanded = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = profileExpanded, onExpandedChange = { profileExpanded = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        readOnly = true,
                        value = profile.name.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() },
                        onValueChange = {},
                        label = { Text("Profile") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = profileExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = profileExpanded, onDismissRequest = { profileExpanded = false }) {
                        FilterProfile.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.titlecase() }) },
                                onClick = { onProfileChange(item); profileExpanded = false }
                            )
                        }
                    }
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
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
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
        Text("Welcome to GentleInk", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
