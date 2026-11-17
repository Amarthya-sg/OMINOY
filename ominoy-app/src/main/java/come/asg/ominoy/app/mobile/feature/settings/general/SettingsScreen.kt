package come.asg.ominoy.app.mobile.feature.settings.general

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import come.asg.ominoy.R
import come.asg.ominoy.app.mobile.feature.main.MainRoute
import come.asg.ominoy.app.mobile.feature.main.navigateToRoute
import come.asg.ominoy.app.mobile.feature.settings.advanced.AdvancedSettingsViewModel
import come.asg.ominoy.app.mobile.feature.settings.retroachievements.RetroAchievementsAccountBridge
import come.asg.ominoy.app.shared.diagnostics.CrashLogStore
import come.asg.ominoy.app.shared.library.LibraryIndexScheduler
import come.asg.ominoy.app.shared.settings.DataFolderPickerLauncher
import come.asg.ominoy.app.utils.android.settings.booleanPreferenceState
import come.asg.ominoy.app.utils.android.settings.indexPreferenceState
import come.asg.ominoy.app.utils.android.settings.intPreferenceState
import come.asg.ominoy.app.utils.android.settings.TouchButtonAppearanceColorSettings
import come.asg.ominoy.app.utils.android.stringListResource
import come.asg.ominoy.app.mobile.shared.compose.ui.OminoyHomeColors as HomeColors
import kotlin.math.roundToInt

private val Teal = Color(0xFF8AEBDD)
private val Line = Color.White.copy(alpha = 0.08f)
private val Muted = Color(0xFF9D98AB)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    advancedViewModel: AdvancedSettingsViewModel,
    navController: NavController,
) {
    val context = LocalContext.current
    val libraryState = viewModel.uiState.collectAsState(SettingsViewModel.State()).value
    val scanInProgress = viewModel.directoryScanInProgress.collectAsState(false).value
    val indexingInProgress = viewModel.indexingInProgress.collectAsState(false).value
    val advancedState = advancedViewModel.uiState.collectAsState().value
    val retroCredentials by RetroAchievementsAccountBridge.credentials.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var showResetDialog by remember { mutableStateOf(false) }

    val exportReportFile = remember(context) { CrashLogStore.latestReport(context) }
    val exportCrashReport = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { destination ->
        val exported = runCatching {
            val report = exportReportFile ?: error("No crash report available")
            val output = destination?.let { context.contentResolver.openOutputStream(it) }
                ?: error("Could not open export destination")
            report.inputStream().use { input -> output.use { input.copyTo(it) } }
        }.isSuccess
        Toast.makeText(
            context,
            context.getString(if (exported) R.string.crash_log_export_success else R.string.crash_log_export_failed),
            Toast.LENGTH_LONG,
        ).show()
    }

    SettingsV2Home(
        modifier = modifier,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        selectedCategory = selectedCategory,
        onCategoryChange = { selectedCategory = it },
        retroUsername = retroCredentials?.username,
        onOpenRetroAchievements = { navController.navigateToRoute(MainRoute.SETTINGS_RETROACHIEVEMENTS) },
        libraryState = libraryState,
        scanInProgress = scanInProgress,
        indexingInProgress = indexingInProgress,
        cacheState = advancedState?.cache,
        exportReportAvailable = exportReportFile != null,
        onOpenDataFolder = { DataFolderPickerLauncher.pickFolder(context) },
        onChangeRomFolder = { viewModel.changeLocalStorageFolder() },
        onRescan = {
            if (scanInProgress) LibraryIndexScheduler.cancelLibrarySync(context)
            else LibraryIndexScheduler.scheduleLibrarySync(context)
        },
        onOpenTouchLayout = { navController.navigateToRoute(MainRoute.SETTINGS_TOUCH_LAYOUT) },
        onOpenGamepads = { navController.navigateToRoute(MainRoute.SETTINGS_INPUT_DEVICES) },
        onOpenViewport = { navController.navigateToRoute(MainRoute.SETTINGS_VIEWPORT_ALIGNMENT) },
        onOpenCloudSaves = { navController.navigateToRoute(MainRoute.SETTINGS_CLOUD_SAVES) },
        onOpenCores = { navController.navigateToRoute(MainRoute.SETTINGS_CORES_SELECTION) },
        onOpenBios = { navController.navigateToRoute(MainRoute.SETTINGS_BIOS) },
        onExportCrashReport = { exportCrashReport.launch("ominoy-crash-log.txt") },
        onFactoryReset = { showResetDialog = true },
    )

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_settings_warning_message_title)) },
            text = { Text(stringResource(R.string.reset_settings_warning_message_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        advancedViewModel.resetAllSettings()
                        showResetDialog = false
                        navController.navigate(MainRoute.SETTINGS.route) {
                            popUpTo(MainRoute.SETTINGS.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SettingsV2Home(
    modifier: Modifier,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    retroUsername: String?,
    onOpenRetroAchievements: () -> Unit,
    libraryState: SettingsViewModel.State,
    scanInProgress: Boolean,
    indexingInProgress: Boolean,
    cacheState: AdvancedSettingsViewModel.CacheState?,
    exportReportAvailable: Boolean,
    onOpenDataFolder: () -> Unit,
    onChangeRomFolder: () -> Unit,
    onRescan: () -> Unit,
    onOpenTouchLayout: () -> Unit,
    onOpenGamepads: () -> Unit,
    onOpenViewport: () -> Unit,
    onOpenCloudSaves: () -> Unit,
    onOpenCores: () -> Unit,
    onOpenBios: () -> Unit,
    onExportCrashReport: () -> Unit,
    onFactoryReset: () -> Unit,
) {
    val context = LocalContext.current
    val q = searchQuery.trim()
    val all = selectedCategory == "All" || q.isNotBlank()
    val directoryName = remember(context, libraryState.currentDirectory) {
        runCatching { DocumentFile.fromTreeUri(context, Uri.parse(libraryState.currentDirectory))?.name }
            .getOrNull() ?: "ROMs"
    }
    val quickSettings = listOf(
        Triple("AMOLED theme", "Pure black backgrounds", R.string.pref_key_amoled_theme) to false,
        Triple("Autosave", "Save automatically when leaving a game", R.string.pref_key_autosave) to true,
        Triple("HD mode", "Higher-resolution rendering when available", R.string.pref_key_hd_mode) to false,
        Triple("Immersive mode", "Hide system bars while playing", R.string.pref_key_enable_immersive_mode) to false,
    )
    val showQuickSearch = q.isNotBlank() && all && quickSettings.any { (entry, _) -> matches(q, entry.first, entry.second) }
    val showRetro = all && (q.isBlank() || matches(q, "RetroAchievements", "Connect an account or view achievement progress"))
    val showQuick = all && q.isBlank()
    val showLibrary = (all || selectedCategory == "Library") &&
        listOf("ROM directory" to "ROMs $directoryName", "Data folder" to "ROMs saves save states previews", "Rescan library" to "Look for new games")
            .any { matches(q, it.first, it.second) }
    val showGeneral = (all || selectedCategory == "General") &&
        listOf("Render scale" to "HD mode quality higher resolution", "Prefer low-latency audio" to "Reduce audio latency", "Cache size limit" to "Maximum storage caching", "Direct game load" to "Reduce loading time cache")
            .any { matches(q, it.first, it.second) }
    val showDisplay = (all || selectedCategory == "Display") &&
        listOf("Shader filter" to "pixels smoothing", "Viewport alignment" to "Position game screen")
            .any { matches(q, it.first, it.second) }
    val showInput = (all || selectedCategory == "Input") &&
        listOf("Touch feedback" to "vibration button presses", "Touch button appearance" to "on-screen controls", "Touch layout editor" to "move resize controls", "Gamepads" to "connected controllers", "Rumble" to "supported games controllers", "Device rumble" to "vibrate device", "Tilt sensor sensitivity" to "tilt sensor")
            .any { matches(q, it.first, it.second) }
    val showCloud = (all || selectedCategory == "Cloud Saves") && matches(q, "Cloud Saves", "Coming soon cloud backup sync")
    val showAdvanced = (all || selectedCategory == "Advanced") &&
        listOf("Change Cores" to "system core", "BIOS" to "detected supported files", "Export latest crash report" to "diagnostic report", "Factory reset" to "reset settings")
            .any { matches(q, it.first, it.second) }

    Box(modifier = modifier.fillMaxSize().background(HomeColors.Background)) {
        Box(
            Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 4.dp).size(380.dp)
                .clip(CircleShape).background(Brush.radialGradient(listOf(HomeColors.PurpleGlow.copy(alpha = .28f), Color.Transparent))),
        )
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = 270.dp, end = 2.dp).size(320.dp)
                .clip(CircleShape).background(Brush.radialGradient(listOf(HomeColors.TealGlow.copy(alpha = .20f), Color.Transparent))),
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsV2Header()
            SettingsSearchField(value = searchQuery, onValueChange = onSearchQueryChange)
            if (q.isBlank()) SettingsCategoryFilters(selected = selectedCategory, onSelect = onCategoryChange)

            if (showRetro) RetroAchievementsCard(retroUsername, onOpenRetroAchievements)
            if (showQuick) QuickSettingsTiles()
            if (showQuickSearch) QuickSettingsSearchResults(q, quickSettings)

            if (showLibrary) {
                SettingsSection("▣", "Library", Color(0xFF8AEBDD)) {
                    if (matches(q, "ROM directory", "ROMs $directoryName")) SettingsActionRow(
                        "ROM directory", directoryName, onClick = onChangeRomFolder, enabled = !indexingInProgress,
                    )
                    if (matches(q, "Data folder", "ROMs saves save states previews")) {
                        SettingsRowDivider()
                        SettingsActionRow("Data folder", "ROMs, saves, save states and previews", onClick = onOpenDataFolder)
                    }
                    if (matches(q, "Rescan library", "Look for new games")) {
                        SettingsRowDivider()
                        SettingsActionRow(
                            if (scanInProgress) "Stop scan" else "Rescan library",
                            if (scanInProgress) "Cancel the current library scan" else "Look for new games",
                            onClick = onRescan,
                            enabled = !indexingInProgress || scanInProgress,
                        )
                    }
                }
            }

            if (showGeneral) GeneralSettingsSection(q, cacheState)
            if (showDisplay) DisplaySettingsSection(q, onOpenViewport)
            if (showInput) InputSettingsSection(q, onOpenTouchLayout, onOpenGamepads)
            if (showCloud) {
                SettingsSection("☁", "Cloud saves", Color(0xFF7EE787)) {
                    if (matches(q, "Cloud Saves", "Cloud save syncing Coming soon")) {
                        SettingsActionRow(
                            "Cloud Saves",
                            "Coming soon · Cloud save backup and sync",
                            onClick = onOpenCloudSaves,
                            accent = Color(0xFF7EE787),
                        )
                    }
                }
            }
            if (showAdvanced) AdvancedSettingsSection(q, exportReportAvailable, onOpenCores, onOpenBios, onExportCrashReport, onFactoryReset)

            if (!showRetro && !showQuick && !showQuickSearch && !showLibrary && !showGeneral && !showDisplay && !showInput && !showCloud && !showAdvanced) {
                Surface(shape = RoundedCornerShape(22.dp), color = HomeColors.CardVariant) {
                    Text(
                        if (q.isBlank()) "No settings in this section" else "No settings match “$q”",
                        color = Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                    )
                }
            }
            Text(
                "OMINOY  ·  RETRO, READY.",
                color = Muted.copy(alpha = .72f),
                fontSize = 10.sp,
                letterSpacing = 1.7.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun matches(query: String, title: String, description: String): Boolean =
    query.isBlank() || "$title $description".contains(query, ignoreCase = true)

@Composable
private fun SettingsV2Header() {
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("YOUR PLAYER", color = Teal, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Text("Settings", color = HomeColors.Text, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.2).sp)
        Text("Tune your library, controls and game experience.", color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun SettingsSearchField(value: String, onValueChange: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(99.dp),
        color = Color(0xFF1C1B24),
        border = BorderStroke(1.dp, Line),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("⌕", color = Muted, fontSize = 22.sp, lineHeight = 22.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = HomeColors.Text, fontSize = 14.sp),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text("Search settings", color = Muted, fontSize = 14.sp)
                    inner()
                },
            )
            if (value.isNotEmpty()) Text("×", color = Muted, fontSize = 21.sp, modifier = Modifier.clickable { onValueChange("") })
        }
    }
}

@Composable
private fun SettingsCategoryFilters(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("All", "Library", "General", "Display", "Input", "Cloud Saves", "Advanced").forEach { category ->
            val active = selected == category
            Surface(
                modifier = Modifier.clickable { onSelect(category) },
                shape = RoundedCornerShape(99.dp),
                color = if (active) Teal else Color(0xFF15141B),
                border = BorderStroke(1.dp, if (active) Teal else Line),
            ) {
                Text(
                    category,
                    color = if (active) Color(0xFF06201D) else Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun RetroAchievementsCard(username: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF3B2A66), Color(0xFF1A1330), Color(0xFF0D3B3F))))
            .clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
            Text("◉", color = Color.White, fontSize = 24.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("RETROACHIEVEMENTS", color = Color.White.copy(alpha = .72f), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
            Text(username ?: "Connect your account", color = HomeColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(if (username == null) "View your achievement profile" else "Account linked · View your profile", color = HomeColors.Text.copy(alpha = .78f), fontSize = 12.sp)
        }
        Text("›", color = Teal, fontSize = 29.sp)
    }
}

@Composable
private fun QuickSettingsTiles() {
    val entries = listOf(
        Triple("AMOLED theme", "Pure black backgrounds", R.string.pref_key_amoled_theme) to false,
        Triple("Autosave", "Save automatically when leaving a game", R.string.pref_key_autosave) to true,
        Triple("HD mode", "Higher-resolution rendering", R.string.pref_key_hd_mode) to false,
        Triple("Immersive mode", "Hide system bars while playing", R.string.pref_key_enable_immersive_mode) to false,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SettingsSectionLabel("QUICK SETTINGS")
        entries.chunked(2).forEach { rowEntries ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowEntries.forEach { (entry, default) ->
                    val (title, subtitle, key) = entry
                    val state = booleanPreferenceState(key, default)
                    Surface(
                        modifier = Modifier.weight(1f).height(112.dp).clickable { state.value = !state.value },
                        shape = RoundedCornerShape(22.dp),
                        color = if (state.value) Color(0xFF10312F) else Color(0xFF15141B),
                        border = BorderStroke(1.dp, if (state.value) Teal.copy(alpha = .52f) else Line),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                when (title) { "AMOLED theme" -> "●"; "Autosave" -> "↺"; "HD mode" -> "✦"; else -> "◐" },
                                color = Teal,
                                fontSize = 19.sp,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(title, color = HomeColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(if (state.value) "On" else "Off", color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsSearchResults(
    query: String,
    entries: List<Pair<Triple<String, String, Int>, Boolean>>,
) {
    val matchingEntries = entries.filter { (entry, _) -> matches(query, entry.first, entry.second) }
    if (matchingEntries.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSectionLabel("QUICK SETTINGS")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF15141B),
                border = BorderStroke(1.dp, Line),
            ) {
                Column {
                    matchingEntries.forEachIndexed { index, (entry, default) ->
                        val state = booleanPreferenceState(entry.third, default)
                        BooleanSettingRow(entry.first, entry.second, state.value, { state.value = it })
                        if (index != matchingEntries.lastIndex) SettingsRowDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneralSettingsSection(query: String, cacheState: AdvancedSettingsViewModel.CacheState?) {
    val hdMode = booleanPreferenceState(R.string.pref_key_hd_mode, false)
    val lowLatency = booleanPreferenceState(R.string.pref_key_low_latency_audio, false)
    val directLoad = booleanPreferenceState(R.string.pref_key_allow_direct_game_load, true)
    val quality = intPreferenceState(stringResource(R.string.pref_key_hd_mode_quality), 1)
    val cacheOptions = cacheState?.displayNames.orEmpty()
    val cacheValues = cacheState?.values.orEmpty()
    val cacheSelection = if (cacheState != null) {
        indexPreferenceState(R.string.pref_key_max_cache_size, cacheState.default, cacheState.values)
    } else {
        null
    }
    if (matches(query, "Render scale", "HD mode quality higher resolution") ||
        matches(query, "Prefer low-latency audio", "Reduce audio latency") ||
        matches(query, "Cache size limit", "Maximum storage caching") ||
        matches(query, "Direct game load", "Reduce loading time cache")
    ) {
        SettingsSection("◐", "General", Color(0xFFB79CFF)) {
            if (matches(query, "Render scale", "HD mode quality higher resolution")) {
                SliderSettingRow(
                    title = "Render scale",
                    subtitle = "Higher values look sharper but use more power",
                    value = quality.value.coerceIn(0, 2).toFloat(),
                    valueLabel = quality.value.coerceIn(0, 2).toString(),
                    enabled = hdMode.value,
                    valueRange = 0f..2f,
                    steps = 1,
                    onValueChange = { quality.value = it.roundToInt() },
                )
            }
            if (matches(query, "Prefer low-latency audio", "Reduce audio latency")) {
                if (matches(query, "Render scale", "HD mode quality higher resolution")) SettingsRowDivider()
                BooleanSettingRow(
                    "Prefer low-latency audio",
                    "Reduce audio latency on supported devices. Might increase audio glitches.",
                    lowLatency.value,
                    { lowLatency.value = it },
                )
            }
            if (matches(query, "Cache size limit", "Maximum storage caching")) {
                if (matches(query, "Render scale", "HD mode quality higher resolution") || matches(query, "Prefer low-latency audio", "Reduce audio latency")) SettingsRowDivider()
                if (cacheSelection != null && cacheOptions.isNotEmpty() && cacheValues.isNotEmpty()) {
                    SegmentedSettingRow(
                        title = "Cache size limit",
                        subtitle = "Maximum storage used for caching",
                        options = cacheOptions,
                        selected = cacheSelection.value.coerceAtLeast(0),
                        onSelected = { cacheSelection.value = it },
                    )
                } else {
                    SettingsTextRow("Cache size limit", "Loading available cache limits…")
                }
            }
            if (matches(query, "Direct game load", "Reduce loading time cache")) {
                if (matches(query, "Render scale", "HD mode quality higher resolution") || matches(query, "Prefer low-latency audio", "Reduce audio latency") || matches(query, "Cache size limit", "Maximum storage caching")) SettingsRowDivider()
                BooleanSettingRow(
                    "Direct game load",
                    "(Experimental) Reduce loading time and cache usage on supported consoles.",
                    directLoad.value,
                    { directLoad.value = it },
                )
            }
        }
    }
}

@Composable
private fun DisplaySettingsSection(query: String, onOpenViewport: () -> Unit) {
    val hdMode = booleanPreferenceState(R.string.pref_key_hd_mode, false)
    val values = stringListResource(R.array.pref_key_shader_filter_values)
    val labels = stringListResource(R.array.pref_key_shader_filter_display_names)
    val shader = indexPreferenceState(R.string.pref_key_shader_filter, "auto", values)
    if (matches(query, "Shader filter", "pixels smoothing") || matches(query, "Viewport alignment", "Position game screen")) {
        SettingsSection("◫", "Display", Color(0xFFFFB86B)) {
            if (matches(query, "Shader filter", "pixels smoothing")) {
                SegmentedSettingRow(
                    title = "Shader filter",
                    subtitle = "Choose how pixels are smoothed",
                    options = labels,
                    selected = shader.value.coerceAtLeast(0),
                    enabled = !hdMode.value,
                    onSelected = { shader.value = it },
                )
            }
            if (matches(query, "Viewport alignment", "Position game screen")) {
                if (matches(query, "Shader filter", "pixels smoothing")) SettingsRowDivider()
                SettingsActionRow("Viewport alignment", "Position of the game screen", onClick = onOpenViewport)
            }
        }
    }
}

@Composable
private fun InputSettingsSection(query: String, onOpenTouchLayout: () -> Unit, onOpenGamepads: () -> Unit) {
    val feedbackValues = stringListResource(R.array.pref_key_haptic_feedback_mode_values)
    val feedbackLabels = stringListResource(R.array.pref_key_haptic_feedback_mode_display_names)
    val feedback = indexPreferenceState(R.string.pref_key_haptic_feedback_mode, "press", feedbackValues)
    val appearanceValues = stringListResource(R.array.pref_key_touch_button_appearance_values)
    val appearanceLabels = stringListResource(R.array.pref_key_touch_button_appearance_display_names)
    val appearance = indexPreferenceState(R.string.pref_key_touch_button_appearance, "classic", appearanceValues)
    val rumble = booleanPreferenceState(R.string.pref_key_enable_rumble, false)
    val deviceRumble = booleanPreferenceState(R.string.pref_key_enable_device_rumble, false)
    val tilt = intPreferenceState(stringResource(R.string.pref_key_tilt_sensitivity_index), 6)
    val showFeedback = matches(query, "Touch feedback", "vibration button presses")
    val showAppearance = matches(query, "Touch button appearance", "on-screen controls")
    val showEditor = matches(query, "Touch layout editor", "move resize controls")
    val showGamepads = matches(query, "Gamepads", "connected controllers")
    val showRumble = matches(query, "Rumble", "supported games controllers")
    val showDeviceRumble = matches(query, "Device rumble", "vibrate device")
    val showTilt = matches(query, "Tilt sensor sensitivity", "tilt sensor")
    if (showFeedback || showAppearance || showEditor || showGamepads || showRumble || showDeviceRumble || showTilt) {
        SettingsSection("🎮", "Input", Color(0xFF72C8FF)) {
            var hasPrevious = false
            if (showFeedback) {
                SegmentedSettingRow("Touch feedback", "Vibration on button presses", feedbackLabels, feedback.value.coerceAtLeast(0), onSelected = { feedback.value = it })
                hasPrevious = true
            }
            if (showAppearance) {
                if (hasPrevious) SettingsRowDivider()
                SegmentedSettingRow(
                    "Touch button appearance",
                    "Choose transparent, frosted white, or a custom color for on-screen controls",
                    appearanceLabels,
                    appearance.value.coerceAtLeast(0),
                    onSelected = { appearance.value = it },
                )
                if (appearance.value == 3 && query.isBlank()) TouchButtonAppearanceColorSettings()
                hasPrevious = true
            }
            if (showEditor) {
                if (hasPrevious) SettingsRowDivider()
                SettingsActionRow("Touch layout editor", "Move and resize on-screen controls", onClick = onOpenTouchLayout)
                hasPrevious = true
            }
            if (showGamepads) {
                if (hasPrevious) SettingsRowDivider()
                SettingsActionRow("Gamepads", "Configure connected controllers", onClick = onOpenGamepads)
                hasPrevious = true
            }
            if (showRumble) {
                if (hasPrevious) SettingsRowDivider()
                BooleanSettingRow("Rumble", "Enable rumble in supported games and controllers", rumble.value, { rumble.value = it })
                hasPrevious = true
            }
            if (showDeviceRumble) {
                if (hasPrevious) SettingsRowDivider()
                BooleanSettingRow(
                    "Device rumble",
                    "When no controller is connected vibrate the device instead",
                    deviceRumble.value,
                    { deviceRumble.value = it },
                    enabled = rumble.value,
                )
                hasPrevious = true
            }
            if (showTilt) {
                if (hasPrevious) SettingsRowDivider()
                SliderSettingRow(
                    title = "Tilt sensor sensitivity",
                    subtitle = "",
                    value = tilt.value.coerceIn(0, 10).toFloat(),
                    valueLabel = tilt.value.coerceIn(0, 10).toString(),
                    valueRange = 0f..10f,
                    steps = 9,
                    onValueChange = { tilt.value = it.roundToInt() },
                )
            }
        }
    }
}

@Composable
private fun AdvancedSettingsSection(
    query: String,
    exportReportAvailable: Boolean,
    onOpenCores: () -> Unit,
    onOpenBios: () -> Unit,
    onExportCrashReport: () -> Unit,
    onFactoryReset: () -> Unit,
) {
    val showCores = matches(query, "Change Cores", "system core")
    val showBios = matches(query, "BIOS", "detected supported files")
    val showCrash = matches(query, "Export latest crash report", "diagnostic report")
    val showReset = matches(query, "Factory reset", "reset settings")
    if (showCores || showBios || showCrash || showReset) {
        SettingsSection("⚙", "Advanced", Color(0xFFF18BB5)) {
            var previous = false
            if (showCores) {
                SettingsActionRow("Change Cores", "Select which core is associated to a system", onClick = onOpenCores)
                previous = true
            }
            if (showBios) {
                if (previous) SettingsRowDivider()
                SettingsActionRow("BIOS", "Display detected and supported BIOS files", onClick = onOpenBios)
                previous = true
            }
            if (showCrash) {
                if (previous) SettingsRowDivider()
                SettingsActionRow(
                    "Export latest crash report",
                    if (exportReportAvailable) "Save the latest diagnostic report to share it" else "No crash reports have been captured on this device.",
                    onClick = onExportCrashReport,
                    enabled = exportReportAvailable,
                )
                previous = true
            }
            if (showReset) {
                if (previous) SettingsRowDivider()
                SettingsActionRow(
                    "Factory reset",
                    "Reset OMINOY to its factory settings",
                    onClick = onFactoryReset,
                    destructive = true,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(icon: String, title: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                Text(icon, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            SettingsSectionLabel(title.uppercase())
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF15141B),
            border = BorderStroke(1.dp, Line),
        ) { Column(content = content) }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(text, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp)
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
    accent: Color = Teal,
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = if (destructive) Color(0xFFFF8A8A) else HomeColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Text("›", color = if (enabled) accent else Muted.copy(alpha = .4f), fontSize = 23.sp)
    }
}

@Composable
private fun SettingsTextRow(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = HomeColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun BooleanSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = if (enabled) HomeColors.Text else Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10151A), checkedTrackColor = Teal),
        )
    }
}

@Composable
private fun SegmentedSettingRow(
    title: String,
    subtitle: String,
    options: List<String>,
    selected: Int,
    enabled: Boolean = true,
    onSelected: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = if (enabled) HomeColors.Text else Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .06f)).padding(3.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            options.forEachIndexed { index, option ->
                val active = selected == index
                Surface(
                    modifier = Modifier.clickable(enabled = enabled) { onSelected(index) },
                    shape = RoundedCornerShape(11.dp),
                    color = if (active) Teal else Color.Transparent,
                ) {
                    Text(
                        option,
                        color = when { !enabled -> Muted.copy(alpha = .5f); active -> Color(0xFF06201D); else -> Muted },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderSettingRow(
    title: String,
    subtitle: String,
    value: Float,
    valueLabel: String,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = if (enabled) HomeColors.Text else Muted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Text(valueLabel, color = if (enabled) Teal else Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal),
        )
    }
}

@Composable
private fun SettingsRowDivider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Line))
}
