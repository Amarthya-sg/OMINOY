package come.asg.lemudroid.app.mobile.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import coil.compose.AsyncImage
import come.asg.lemudroid.R
import come.asg.lemudroid.app.mobile.feature.settings.retroachievements.RetroAchievementsAccountBridge
import come.asg.lemudroid.app.mobile.feature.settings.retroachievements.readGameplayCredentials
import come.asg.lemudroid.app.mobile.shared.compose.ui.LemuroidGameCard
import come.asg.lemudroid.app.mobile.shared.compose.ui.LemuroidGameImage
import come.asg.lemudroid.app.mobile.shared.compose.ui.OminoyHomeColors as HomeColors
import come.asg.lemudroid.app.shared.main.GamePlaytimeStats
import come.asg.lemudroid.app.shared.game.RetroAchievementsEarnedStore
import come.asg.lemudroid.app.shared.settings.DataFolderPickerLauncher
import come.asg.lemudroid.app.utils.android.ComposableLifecycle
import come.asg.lemudroid.common.displayDetailsSettingsScreen
import come.asg.lemudroid.lib.library.db.entity.Game
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.io.File

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onGameFavoriteToggle: (Game, Boolean) -> Unit,
    onOpenCoreSelection: () -> Unit,
    onOpenSearch: () -> Unit,
    onHelpPressed: () -> Unit,
) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext

    LaunchedEffect(Unit) {
        if (RetroAchievementsAccountBridge.credentials.value == null) {
            readGameplayCredentials(applicationContext)?.let(RetroAchievementsAccountBridge::set)
        }
    }

    ComposableLifecycle { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            viewModel.updatePermissions(applicationContext)
        }
    }

    val permissionsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) context.displayDetailsSettingsScreen()
        }

    val state = viewModel.getViewStates().collectAsState(HomeViewModel.UIState()).value
    HomeScreen(
        modifier = modifier,
        state = state,
        onGameClicked = onGameClick,
        onGameLongClick = onGameLongClick,
        onGameFavoriteToggle = onGameFavoriteToggle,
        onOpenCoreSelection = onOpenCoreSelection,
        onOpenSearch = onOpenSearch,
        onHelpPressed = onHelpPressed,
        onEnableNotificationsClicked = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onEnableMicrophoneClicked = { permissionsLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onSetDirectoryClicked = { viewModel.changeLocalStorageFolder(context) },
        onSetDataFolderClicked = { DataFolderPickerLauncher.pickFolder(context) },
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun HomeScreen(
    modifier: Modifier,
    state: HomeViewModel.UIState,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onGameFavoriteToggle: (Game, Boolean) -> Unit,
    onOpenCoreSelection: () -> Unit,
    onOpenSearch: () -> Unit,
    onHelpPressed: () -> Unit,
    onEnableNotificationsClicked: () -> Unit,
    onEnableMicrophoneClicked: () -> Unit,
    onSetDirectoryClicked: () -> Unit,
    onSetDataFolderClicked: () -> Unit,
) {
    val retroAchievementsCredentials by RetroAchievementsAccountBridge.credentials.collectAsState()
    val retroAchievementsLoggedIn = retroAchievementsCredentials != null
    val games = remember(state.recentGames, state.favoritesGames, state.discoveryGames) {
        state.recentGames.take(10).ifEmpty { state.favoritesGames.take(10).ifEmpty { state.discoveryGames.take(10) } }
    }
    var notificationDismissed by rememberSaveable { mutableStateOf(false) }
    var selectedGameId by remember(games) { mutableStateOf(games.firstOrNull()?.id) }
    val carouselState = rememberLazyListState()
    val selectedGame = games.firstOrNull { it.id == selectedGameId } ?: games.firstOrNull()
    val context = LocalContext.current
    var statsGame by remember { mutableStateOf<Game?>(null) }
    var statsAchievement by remember { mutableStateOf<RetroAchievementsEarnedStore.Achievement?>(null) }
    var statsRefreshToken by remember { mutableStateOf(0) }
    val openStats = { game: Game ->
        statsGame = game
        statsRefreshToken++
    }
    LaunchedEffect(carouselState, games) {
        snapshotFlow { carouselState.firstVisibleItemIndex }
            .map { games.getOrNull(it)?.id }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { selectedGameId = it }
    }

    Box(modifier = modifier.fillMaxSize().background(HomeColors.Background)) {
        Box(Modifier.align(Alignment.TopStart).offset(x = (-140).dp, y = 40.dp).size(470.dp).blur(55.dp).background(Brush.radialGradient(listOf(HomeColors.PurpleGlow.copy(alpha = .58f), Color.Transparent))))
        Box(Modifier.align(Alignment.TopEnd).offset(x = 160.dp, y = 190.dp).size(440.dp).blur(55.dp).background(Brush.radialGradient(listOf(HomeColors.TealGlow.copy(alpha = .6f), Color.Transparent))))
        Box(Modifier.align(Alignment.Center).offset(x = (-20).dp, y = 340.dp).size(430.dp).blur(60.dp).background(Brush.radialGradient(listOf(HomeColors.RoseGlow.copy(alpha = .36f), Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .12f), Color.Black.copy(alpha = .54f), Color.Black))))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 144.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 24.dp, end = 16.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "OMINOY",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp,
                )
                Text("RETRO, READY.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
            }
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Search games", tint = MaterialTheme.colorScheme.onSurface)
            }
            Surface(shape = CircleShape, color = Color(0xFF8AEBDD), modifier = Modifier.padding(start = 4.dp)) {
                IconButton(onClick = onHelpPressed, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.Info, contentDescription = "Updates", tint = Color(0xFF10131A))
                }
            }
        }

        AnimatedVisibility(state.showNoNotificationPermissionCard && !notificationDismissed) {
            HomeNotification(
                R.string.home_notification_title,
                R.string.home_notification_message,
                R.string.home_notification_action,
                onEnableNotificationsClicked,
                onDismiss = { notificationDismissed = true },
            )
        }
        AnimatedVisibility(state.showNoGamesCard) {
            HomeNotification(
                R.string.home_empty_title,
                R.string.home_empty_message,
                R.string.home_empty_action,
                onSetDirectoryClicked,
                enabled = !state.indexInProgress,
            )
        }
        AnimatedVisibility(state.showNoDataFolderCard) {
            HomeNotification(R.string.data_folder_setup_title, R.string.data_folder_setup_message, R.string.data_folder_setup_action, onSetDataFolderClicked)
        }
        AnimatedVisibility(state.showNoMicrophonePermissionCard) {
            HomeNotification(R.string.home_microphone_title, R.string.home_microphone_message, R.string.home_microphone_action, onEnableMicrophoneClicked)
        }
        AnimatedVisibility(state.showDesmumeDeprecatedCard) {
            HomeNotification(
                R.string.home_notification_desmume_deprecated_title,
                R.string.home_notification_desmume_deprecated_message,
                R.string.home_notification_desmume_deprecated_action,
                onOpenCoreSelection,
            )
        }

        Text(
            text = if (selectedGame?.lastPlayedAt != null) "Pick up where you left off" else "Your next adventure",
            modifier = Modifier.padding(start = 24.dp, top = 4.dp, end = 24.dp),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 25.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.7).sp,
        )

        if (selectedGame != null) {
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
            LazyRow(
                state = carouselState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                itemsIndexed(games, key = { _, game -> game.id }) { index, game ->
                    val selected = selectedGame.id == game.id
                    val scale by animateFloatAsState(if (selected) 1f else 0.92f, tween(240), label = "coverScale")
                    val opacity by animateFloatAsState(if (selected) 1f else 0.76f, tween(240), label = "coverOpacity")
                    Box(
                        modifier = Modifier.width(250.dp).height(250.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = opacity }
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF342A4A), Color(0xFF17131F), Color(0xFF271A29)),
                                ),
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                            .combinedClickable(
                                onClick = {
                                    selectedGameId = game.id
                                    coroutineScope.launch { carouselState.animateScrollToItem(index) }
                                },
                                onLongClick = { onGameLongClick(game) },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        LemuroidGameImage(
                            game = game,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)),
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.52f),
                        ) {
                            IconButton(
                                onClick = { openStats(game) },
                                modifier = Modifier.size(38.dp),
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                            ) {
                                Icon(Icons.Filled.Info, contentDescription = "${game.displayName()} play statistics")
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = selectedGame.displayName(),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${selectedGame.systemId.uppercase()}  ·  ${if (selectedGame.lastPlayedAt == null) "Not played yet" else "Recently played"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                        IconButton(onClick = { onGameFavoriteToggle(selectedGame, !selectedGame.isFavorite) }) {
                            Icon(
                                imageVector = if (selectedGame.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (selectedGame.isFavorite) "Remove favorite" else "Add favorite",
                                tint = if (selectedGame.isFavorite) Color(0xFFFF83A8) else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Button(
                        onClick = { onGameClicked(selectedGame) },
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF4F1F8), contentColor = Color(0xFF0C0B10)),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (selectedGame.lastPlayedAt == null) "Play" else "Resume", fontWeight = FontWeight.Bold)
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                        IconButton(onClick = { onGameLongClick(selectedGame) }) {
                            Icon(Icons.Filled.MoreHoriz, contentDescription = "More game actions", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(minOf(games.size, 7)) { index ->
                        Box(
                            Modifier.size(if (games[index].id == selectedGame.id) 18.dp else 5.dp, 5.dp)
                                .clip(CircleShape)
                                .background(if (games[index].id == selectedGame.id) Color(0xFF8AEBDD) else Color.White.copy(alpha = 0.24f)),
                        )
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Start your library",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Choose your ROM folder in Settings to bring your games here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        HomeRow("Recently played", state.recentGames, onGameClicked, onGameLongClick) { openStats(it) }
        HomeRow("Favorites", state.favoritesGames, onGameClicked, onGameLongClick) { openStats(it) }
        HomeRow("Discover", state.discoveryGames, onGameClicked, onGameLongClick) { openStats(it) }
        Spacer(Modifier.height(8.dp))
        }
        statsGame?.let { game ->
            val playStats = remember(game.id, statsRefreshToken) {
                val totalMs = GamePlaytimeStats.totalMs(context, game.id)
                val recordedLastPlayed = GamePlaytimeStats.lastPlayed(context, game.id)
                val lastPlayed = recordedLastPlayed.takeIf { it > 0L }?.let(::Date) ?: game.lastPlayedAt?.let(::Date)
                Triple(totalMs, GamePlaytimeStats.sessions(context, game.id), lastPlayed)
            }
            val totalMinutes = playStats.first / 60_000L
            val played = if (totalMinutes >= 60) "${totalMinutes / 60} hr ${totalMinutes % 60} min" else "$totalMinutes min"
            val cachedAchievements = remember(game.id, game.title, game.displayName(), game.fileName, statsRefreshToken) {
                val directlyLinked = RetroAchievementsEarnedStore.read(context, game.id)
                val syncedByTitle = RetroAchievementsEarnedStore.readAll(context).filter { cached ->
                    cached.localLibraryGameId == game.id || game.matchesCachedAchievementTitle(cached.gameTitle)
                }
                (directlyLinked + syncedByTitle).distinctBy { it.raGameId to it.id }
            }
            val persistedAccount = remember(game.id, statsRefreshToken) { readGameplayCredentials(context) }
            HomeGameStatsDialog(
                game = game,
                played = played,
                sessions = playStats.second,
                lastPlayed = playStats.third,
                achievements = cachedAchievements,
                accountAvailable = retroAchievementsLoggedIn || persistedAccount != null,
                onClose = { statsGame = null },
                onAchievement = { statsAchievement = it },
            )
        }
        statsAchievement?.let { achievement ->
            val game = statsGame
            if (game != null) HomeAchievementDetailDialog(game, achievement) { statsAchievement = null }
        }

    }
}

private fun Game.matchesCachedAchievementTitle(cachedTitle: String): Boolean {
    val localNames = listOf(title, displayName(), fileName.substringBeforeLast('.', fileName))
        .map(::normalizeHomeGameTitle)
        .filter { it.length >= 4 }
    val cached = normalizeHomeGameTitle(cachedTitle)
    if (cached.length < 4) return false
    return localNames.any { local ->
        local == cached ||
            (minOf(local.length, cached.length) >= 7 && (local.contains(cached) || cached.contains(local))) ||
            run {
                val localTokens = local.split(' ').filter { it.length > 2 }.toSet()
                val cacheTokens = cached.split(' ').filter { it.length > 2 }.toSet()
                val smaller = minOf(localTokens.size, cacheTokens.size)
                smaller > 0 && localTokens.intersect(cacheTokens).size.toFloat() / smaller >= .75f
            }
    }
}

private fun normalizeHomeGameTitle(value: String): String {
    val withoutTags = value
        .replace(Regex("\\([^)]*\\)|\\[[^]]*]"), " ")
        .replace(Regex("\\.(zip|7z|rar|nes|gb|gbc|gba|sfc|smc|bin|rom)$", RegexOption.IGNORE_CASE), " ")
    val ascii = java.text.Normalizer.normalize(withoutTags, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
    val ignored = setOf("usa", "europe", "japan", "world", "rev", "revision", "version", "proto", "beta", "the", "and")
    return ascii.split(' ').filterNot { it in ignored }.joinToString(" ")
}

@Composable
private fun HomeGameStatsDialog(
    game: Game,
    played: String,
    sessions: Int,
    lastPlayed: Date?,
    achievements: List<RetroAchievementsEarnedStore.Achievement>,
    accountAvailable: Boolean,
    onClose: () -> Unit,
    onAchievement: (RetroAchievementsEarnedStore.Achievement) -> Unit,
) {
    val earned = achievements.filter { it.earned }
    val pending = achievements.filterNot { it.earned }
    val progress = if (achievements.isNotEmpty()) earned.size.toFloat() / achievements.size else 0f
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = .72f)).pointerInput(onClose) { detectTapGestures(onTap = { onClose() }) },
            contentAlignment = Alignment.Center,
        ) {
            val interaction = remember { MutableInteractionSource() }
            Column(
                Modifier.align(Alignment.Center).fillMaxWidth(.94f).widthIn(max = 440.dp)
                    .heightIn(max = maxHeight * .86f)
                    .clip(RoundedCornerShape(30.dp)).background(HomeColors.CardVariant)
                    .clickable(interactionSource = interaction, indication = null) { }
                    .verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(HomeColors.Surface)) {
                        LemuroidGameImage(game = game, modifier = Modifier.fillMaxSize())
                    }
                    Column(Modifier.weight(1f)) {
                        Text(game.displayName(), color = HomeColors.Text, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2)
                        Text("${game.systemId.uppercase()} · Last played ${lastPlayed?.let { android.text.format.DateFormat.getMediumDateFormat(androidx.compose.ui.platform.LocalContext.current).format(it) } ?: "Never"}", color = HomeColors.Muted, fontSize = 12.sp)
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) { Text("×", fontSize = 26.sp, color = HomeColors.Muted) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeStatTile(played, "Time played", Modifier.weight(1f))
                    HomeStatTile(sessions.toString(), "Sessions", Modifier.weight(1f))
                }
                Surface(shape = RoundedCornerShape(22.dp), color = HomeColors.Card, border = androidx.compose.foundation.BorderStroke(1.dp, HomeColors.Line)) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Achievements", color = HomeColors.Text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("${earned.size} / ${achievements.size}", color = HomeColors.Mint, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = HomeColors.Mint, trackColor = Color.White.copy(alpha = .12f))
                        if (achievements.isEmpty()) {
                            Text(
                                if (accountAvailable) "Achievement details are not cached for this game yet. Launch it with RetroAchievements sync enabled to load them."
                                else "Log in to RetroAchievements to see earned and pending achievements.",
                                color = HomeColors.Muted, fontSize = 13.sp, lineHeight = 18.sp,
                            )
                        } else {
                            HomeAchievementGroup("EARNED", earned, locked = false, onAchievement = onAchievement)
                            HomeAchievementGroup("PENDING", pending, locked = true, onAchievement = onAchievement)
                        }
                    }
                }
                TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("Close", color = HomeColors.Mint, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun HomeStatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = HomeColors.Card, border = androidx.compose.foundation.BorderStroke(1.dp, HomeColors.Line)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, color = HomeColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, color = HomeColors.Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun HomeAchievementGroup(
    title: String,
    achievements: List<RetroAchievementsEarnedStore.Achievement>,
    locked: Boolean,
    onAchievement: (RetroAchievementsEarnedStore.Achievement) -> Unit,
) {
    Text("$title (${achievements.size})", color = HomeColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 5.dp))
    if (achievements.isEmpty()) {
        Text(if (locked) "All done!" else "Nothing earned yet", color = HomeColors.Muted, fontSize = 12.sp)
    } else {
        val rows = ((achievements.size + 3) / 4).coerceAtMost(3)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().height((rows * 58 + (rows - 1) * 9).dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp), verticalArrangement = Arrangement.spacedBy(9.dp),
            userScrollEnabled = false,
        ) {
            items(achievements.take(12), key = { it.id }) { achievement ->
                val badgePath = if (locked) achievement.lockedBadgePath.ifBlank { achievement.badgePath } else achievement.badgePath
                Surface(
                    modifier = Modifier.size(58.dp).clip(RoundedCornerShape(13.dp)).combinedClickable(onClick = { onAchievement(achievement) }, onLongClick = null),
                    color = HomeColors.SurfaceVariant,
                    border = if (!locked) androidx.compose.foundation.BorderStroke(1.dp, HomeColors.Gold.copy(alpha = .5f)) else null,
                ) {
                    val badgeBackground = if (locked) {
                        Brush.linearGradient(listOf(Color.Gray.copy(alpha = .14f), Color.Gray.copy(alpha = .14f)))
                    } else {
                        Brush.linearGradient(listOf(HomeColors.Gold.copy(alpha = .38f), HomeColors.SurfaceVariant))
                    }
                    Box(Modifier.fillMaxSize().background(badgeBackground), contentAlignment = Alignment.Center) {
                        if (badgePath.isNotBlank() && File(badgePath).isFile) {
                            AsyncImage(model = File(badgePath), contentDescription = achievement.title, modifier = Modifier.fillMaxSize().padding(5.dp).clip(RoundedCornerShape(9.dp)), alpha = if (locked) .48f else 1f)
                        } else Text(if (locked) "◇" else "★", fontSize = 22.sp, fontWeight = FontWeight.Black, color = if (locked) HomeColors.Muted else HomeColors.Gold)
                    }
                }
            }
        }
        if (achievements.size > 12) Text("+${achievements.size - 12} more · open a game in the library for the full list", color = HomeColors.Muted, fontSize = 11.sp)
    }
}

@Composable
private fun HomeAchievementDetailDialog(
    game: Game,
    achievement: RetroAchievementsEarnedStore.Achievement,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val path = if (achievement.earned) achievement.badgePath else achievement.lockedBadgePath.ifBlank { achievement.badgePath }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = .78f)).pointerInput(onClose) { detectTapGestures(onTap = { onClose() }) },
            contentAlignment = Alignment.BottomCenter,
        ) {
            val interaction = remember { MutableInteractionSource() }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth(.96f).widthIn(max = 680.dp).heightIn(max = maxHeight * .88f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(HomeColors.CardVariant)
                    .clickable(interactionSource = interaction, indication = null) { }
                    .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = .22f)))
                Box(Modifier.size(96.dp).clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(HomeColors.Gold.copy(alpha = .7f), HomeColors.SurfaceVariant))), contentAlignment = Alignment.Center) {
                    if (path.isNotBlank() && File(path).isFile) AsyncImage(model = File(path), contentDescription = "${achievement.title} badge", modifier = Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(22.dp)), alpha = if (achievement.earned) 1f else .48f) else Text(if (achievement.earned) "★" else "◇", color = HomeColors.Gold, fontSize = 38.sp)
                }
                Text(achievement.title, color = HomeColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text("${game.displayName()} · ${game.systemId.uppercase()}", color = HomeColors.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeChip("${achievement.points} pts", HomeColors.Gold)
                    HomeChip(if (achievement.earned) "✓ Unlocked" else "Locked", if (achievement.earned) HomeColors.Mint else HomeColors.Muted)
                    if (achievement.hardcoreEarnedAt > 0L) HomeChip("★ Hardcore", HomeColors.Gold)
                }
                HomeDetailSection("HOW TO EARN") { Text(achievement.description.ifBlank { "No description is available." }, color = HomeColors.Text, fontSize = 15.sp, lineHeight = 21.sp) }
                HomeDetailSection(if (achievement.earned) "UNLOCKED" else "STATUS") {
                    if (achievement.earned) {
                        val date = if (achievement.earnedAt > 0L) android.text.format.DateFormat.getMediumDateFormat(context).format(Date(achievement.earnedAt)) else "Date unavailable"
                        Text("$date · ${if (achievement.hardcoreEarnedAt > 0L) "Hardcore" else "Softcore"}", color = HomeColors.Text, fontSize = 14.sp)
                    } else Text("Not unlocked yet. Keep playing to earn ${achievement.points} points.", color = HomeColors.Muted, fontSize = 14.sp)
                }
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(99.dp)) { Text("Close") }
            }
        }
    }
}

@Composable
private fun HomeChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(99.dp), color = color.copy(alpha = .14f)) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HomeDetailSection(title: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = HomeColors.Card, border = androidx.compose.foundation.BorderStroke(1.dp, HomeColors.Line)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = HomeColors.Muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeRow(
    title: String,
    games: List<Game>,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onInfoClick: (Game) -> Unit,
) {
    if (games.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        ) {
            items(games, key = { it.id }) { game ->
                LemuroidGameCard(
                    modifier = Modifier.width(142.dp),
                    game = game,
                    onClick = { onGameClicked(game) },
                    onLongClick = { onGameLongClick(game) },
                    onInfoClick = { onInfoClick(game) },
                )
            }
        }
    }
}

@Composable
private fun HomeNotification(
    titleId: Int,
    messageId: Int,
    actionId: Int,
    onAction: () -> Unit,
    enabled: Boolean = true,
    onDismiss: (() -> Unit)? = null,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(22.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(containerColor = HomeColors.SurfaceVariant.copy(alpha = .94f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(titleId), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = HomeColors.Text)
            Text(stringResource(messageId), style = MaterialTheme.typography.bodyMedium, color = HomeColors.Muted, lineHeight = 20.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (onDismiss != null) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(99.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) { Text("Not now") }
                    Spacer(Modifier.width(8.dp))
                }
                OutlinedButton(onClick = onAction, enabled = enabled, shape = RoundedCornerShape(99.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(actionId), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
