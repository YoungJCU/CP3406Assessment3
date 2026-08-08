package com.youngjcu.pclab.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.youngjcu.pclab.data.repository.LearningStatistics
import com.youngjcu.pclab.data.repository.ThemePreference
import com.youngjcu.pclab.data.repository.UserSettings
import com.youngjcu.pclab.domain.model.BuildDraft
import com.youngjcu.pclab.domain.model.Evaluation
import com.youngjcu.pclab.domain.model.HardwareCatalogue
import com.youngjcu.pclab.domain.model.HardwarePart
import com.youngjcu.pclab.domain.model.Mission
import com.youngjcu.pclab.domain.model.OutcomeStatus
import com.youngjcu.pclab.domain.model.PartCategory
import com.youngjcu.pclab.domain.model.RuleOutcome
import java.text.DateFormat
import java.util.Date

@Composable
fun LoadingScreen(modifier: Modifier, isLoading: Boolean, error: String?, onRetry: () -> Unit) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Loading the hardware learning catalogue…")
        } else {
            Text("Catalogue unavailable", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(error.orEmpty())
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun HomeScreen(
    catalogue: HardwareCatalogue?,
    statistics: LearningStatistics,
    onStartMission: (Int) -> Unit,
    onStatistics: () -> Unit,
    onSettings: () -> Unit
) {
    val mission = catalogue?.missions?.firstOrNull { it.id !in statistics.completedMissionIds }
        ?: catalogue?.missions?.firstOrNull()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("PC Builder Lab", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Learn hardware decisions by building, testing and explaining.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Today’s learning mission", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(mission?.title ?: "No mission available", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(mission?.description.orEmpty())
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { mission?.let { onStartMission(it.id) } }, enabled = mission != null) {
                        Text(if (mission?.id in statistics.completedMissionIds) "Practice again" else "Start mission")
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Completed", "${statistics.completedMissionIds.size}/${catalogue?.missions?.size ?: 0}", Modifier.weight(1f))
                StatCard("Average score", "${statistics.averageScore}%", Modifier.weight(1f))
                StatCard("Favourites", statistics.favourites.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            Text("Quick access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onStatistics) { Text("Statistics") }
                OutlinedButton(onClick = onSettings) { Text("Accessibility & settings") }
            }
        }
        item {
            Text("How it works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Choose each component, then read the compatibility, budget and performance explanations. The app teaches the reason behind every result.")
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuilderScreen(
    mission: Mission?,
    catalogue: HardwareCatalogue?,
    draft: BuildDraft,
    onSelectPart: (HardwarePart) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    apiLoading: Boolean,
    apiError: String?,
    onRetryApi: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(mission?.title ?: "Build PC") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Budget: S$${mission?.budget ?: 0}", fontWeight = FontWeight.Bold)
                        Text("Selected: S$${draft.totalCost} • ${PartCategory.entries.count { draft.part(it) != null }}/7 parts")
                        Spacer(Modifier.height(8.dp))
                        Text(mission?.hint.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                CatalogueStatusCard(
                    catalogue = catalogue,
                    isLoading = apiLoading,
                    error = apiError,
                    onRetry = onRetryApi
                )
            }
            item { Text("Choose your components", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            items(PartCategory.entries.toList(), key = { it.name }) { category ->
                PartSelector(
                    category = category,
                    selected = draft.part(category),
                    options = catalogue?.parts?.get(category).orEmpty(),
                    onSelect = onSelectPart
                )
            }
            item {
                Button(onClick = onSubmit, enabled = draft.isComplete, modifier = Modifier.fillMaxWidth()) {
                    Text(if (draft.isComplete) "Evaluate my build" else "Choose all 7 components")
                }
            }
        }
    }
}

@Composable
private fun CatalogueStatusCard(
    catalogue: HardwareCatalogue?,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when {
                isLoading && catalogue == null -> Text("Loading hardware options…")
                error != null && catalogue == null -> {
                    Text(error)
                    OutlinedButton(onClick = onRetry) { Text("Retry catalogue") }
                }
                else -> {
                    val count = catalogue?.parts?.values?.sumOf { it.size } ?: 0
                    Text("$count hardware options loaded", fontWeight = FontWeight.Medium)
                    Text("Tap any component card to compare options and read its learning tip.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartSelector(category: PartCategory, selected: HardwarePart?, options: List<HardwarePart>, onSelect: (HardwarePart) -> Unit) {
    var showPicker by remember(category) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = options.isNotEmpty()) { showPicker = true },
        colors = CardDefaults.cardColors(
            containerColor = if (selected == null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(category.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                if (selected == null) {
                    Text(
                        if (options.isEmpty()) "No options available yet" else "Tap to compare ${options.size} options",
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(selected.name, fontWeight = FontWeight.Medium)
                    Text("S$${selected.price} • ${selected.primaryDetails()}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text("Learning tip", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(selected.learningNote, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(
                imageVector = if (selected == null) Icons.Default.Info else Icons.Default.CheckCircle,
                contentDescription = if (selected == null) "Choose ${category.label}" else "${category.label} selected",
                tint = if (selected == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
            )
        }
    }
    if (showPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showPicker = false }, sheetState = sheetState) {
            ComponentPickerSheet(
                category = category,
                options = options,
                selectedId = selected?.id,
                onSelect = {
                    onSelect(it)
                    showPicker = false
                }
            )
        }
    }
}

@Composable
private fun ComponentPickerSheet(
    category: PartCategory,
    options: List<HardwarePart>,
    selectedId: Int?,
    onSelect: (HardwarePart) -> Unit
) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Choose ${category.label}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Compare the details, then choose the option that suits the mission.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(options, key = { it.id }) { part ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(part) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (part.id == selectedId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(part.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("S$${part.price} • ${part.primaryDetails()}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Learning tip", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(part.learningNote, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun HardwarePart.primaryDetails(): String = when (category) {
    PartCategory.CPU -> listOfNotNull(socket, supportedRam).joinToString(" • ")
    PartCategory.MOTHERBOARD -> listOfNotNull(socket, supportedRam, formFactor).joinToString(" • ")
    PartCategory.GPU -> "${gpuLengthMm ?: 0} mm • ${power} W"
    PartCategory.RAM -> "${ramCapacityGb ?: 0} GB • ${ramGeneration.orEmpty()}"
    PartCategory.STORAGE -> "${storageCapacityGb ?: 0} GB ${formFactor.orEmpty()}"
    PartCategory.PSU -> "${psuWattage ?: 0} W"
    PartCategory.CASE -> "GPU up to ${maxGpuLengthMm ?: 0} mm"
}

@Composable
fun ResultScreen(mission: Mission?, evaluation: Evaluation?, onSaveFavourite: () -> Unit, onBackHome: () -> Unit) {
    if (evaluation == null) {
        LoadingScreen(Modifier.fillMaxSize(), false, "Select all components before evaluating your build.", onBackHome)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Mission result", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(mission?.title.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Score", "${evaluation.score}%", Modifier.weight(1f))
                StatCard("Cost", "S$${evaluation.totalCost}", Modifier.weight(1f))
                StatCard("Performance", "${evaluation.performanceScore}", Modifier.weight(1f))
            }
        }
        item { Text("What your build teaches", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        items(evaluation.outcomes, key = { it.title }) { outcome -> OutcomeCard(outcome) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onSaveFavourite) { Text("Save as favourite") }
                Button(onClick = onBackHome) { Text("Back to home") }
            }
        }
    }
}

@Composable
private fun OutcomeCard(outcome: RuleOutcome) {
    val (icon, color, label) = when (outcome.status) {
        OutcomeStatus.PASS -> Triple(Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary, "Pass")
        OutcomeStatus.WARNING -> Triple(Icons.Default.Info, MaterialTheme.colorScheme.tertiary, "Needs input")
        OutcomeStatus.FAIL -> Triple(Icons.Default.Error, MaterialTheme.colorScheme.error, "Needs attention")
    }
    Card {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = label, tint = color)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(outcome.title, fontWeight = FontWeight.Bold)
                Text(outcome.explanation)
            }
        }
    }
}

@Composable
fun StatisticsScreen(statistics: LearningStatistics, missions: List<Mission>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Learning statistics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Text("${statistics.completedMissionIds.size} of ${missions.size} missions completed")
            LinearProgressIndicator(
                progress = { if (missions.isEmpty()) 0f else statistics.completedMissionIds.size.toFloat() / missions.size },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
        item { StatCard("Average score", "${statistics.averageScore}%", Modifier.fillMaxWidth()) }
        item { Text("Recent builds", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (statistics.recentResults.isEmpty()) {
            item { Text("Complete a mission to see your decision history here.") }
        } else {
            items(statistics.recentResults, key = { it.id }) { result ->
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text(result.missionTitle, fontWeight = FontWeight.Bold)
                        Text("Score ${result.score}% • S$${result.totalCost}")
                        Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(result.completedAt)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { Text("Favourite builds", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        if (statistics.favourites.isEmpty()) {
            item { Text("Save a mission build as a favourite to compare it later.") }
        } else {
            items(statistics.favourites, key = { it.id }) { favourite ->
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text(favourite.label, fontWeight = FontWeight.Bold)
                        Text("S$${favourite.totalCost}")
                        Text(favourite.buildSummary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: UserSettings,
    onThemeChange: (ThemePreference) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onColourBlindChange: (Boolean) -> Unit,
    onResetProgress: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Text("Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreference.entries.forEach { theme ->
                    FilterChip(selected = settings.theme == theme, onClick = { onThemeChange(theme) }, label = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }
        }
        item {
            Text("Font size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Slider(value = settings.fontScale, onValueChange = onFontScaleChange, valueRange = 1f..1.3f, steps = 2)
            Text("Current scale: ${(settings.fontScale * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Colour-blind palette", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Use blue, orange and green status colours alongside descriptive labels.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = settings.colourBlindMode, onCheckedChange = onColourBlindChange)
            }
        }
        item {
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Learning progress stays on this device. The app does not ask for an account, location or contacts.")
        }
        item {
            OutlinedButton(onClick = { showResetDialog = true }) { Text("Reset learning progress") }
        }
    }
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset learning progress?") },
            text = { Text("This removes locally stored mission history and favourite builds. It does not affect any other device data.") },
            confirmButton = {
                Button(onClick = { onResetProgress(); showResetDialog = false }) { Text("Reset") }
            },
            dismissButton = { OutlinedButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
        )
    }
}
