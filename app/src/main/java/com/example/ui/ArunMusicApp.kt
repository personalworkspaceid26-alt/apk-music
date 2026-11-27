package com.example.ui

import android.provider.OpenableColumns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.audio.OfflineCacheManager
import com.example.auth.signOut
import com.example.data.MusicRepository
import com.example.model.EqPreset
import com.example.model.GlassIntensity
import com.example.model.MusicTrack
import com.example.model.PlaylistDoc
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.FullScreenNowPlayingModal
import com.example.ui.components.InteractiveDynamicIsland
import com.example.ui.components.LiquidGlassAtmosphere
import com.example.ui.components.PersistentMiniPlayer
import com.example.ui.components.SpotifyImportDialog
import com.example.ui.components.YouTubeSourcePickerDialog
import com.example.ui.screens.ActivityAndRecapModal
import com.example.ui.screens.EqualizerModal
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsAndAboutModal
import com.example.ui.screens.SongRecognitionModal
import com.example.ui.screens.YouHubScreen
import com.example.ui.theme.ArunCyan
import com.example.ui.theme.ArunMusicTheme
import com.example.ui.theme.GlassBorderSpecular
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.viewmodel.ArunMusicViewModel
import com.example.ui.viewmodel.LayoutPreviewMode
import com.example.ui.viewmodel.SpotifyImportUiState
import com.example.ui.viewmodel.UiState
import com.google.firebase.firestore.FirebaseFirestore

private enum class ArunDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("beranda", "Beranda", Icons.Rounded.Home, Icons.Outlined.Home),
    SEARCH("pencarian", "Pencarian", Icons.Rounded.Search, Icons.Outlined.Search),
    LIBRARY("pustaka", "Pustaka", Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic),
    YOU("kamu", "Kamu", Icons.Rounded.Person, Icons.Outlined.Person)
}

private enum class ActiveYouModal {
    NONE,
    RECOGNIZER,
    EQUALIZER,
    RECAP,
    SETTINGS
}

@Composable
fun ArunMusicMainShell(
    currentUserId: String,
    displayName: String,
    email: String?
) {
    val vm: ArunMusicViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ArunMusicViewModel(
                    appContext = app.applicationContext,
                    repository = MusicRepository(db),
                    offlineCacheManager = OfflineCacheManager(app),
                    currentUserId = currentUserId,
                    userDisplayName = displayName,
                    userEmail = email
                )
            }
        }
    )

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val profileState by vm.profileState.collectAsStateWithLifecycle()
    val playlistsState by vm.playlistsState.collectAsStateWithLifecycle()
    val libraryState by vm.libraryTracksState.collectAsStateWithLifecycle()
    val historyState by vm.playHistoryState.collectAsStateWithLifecycle()
    val allCatalogTracks by vm.allCatalogTracks.collectAsStateWithLifecycle()

    val currentTrack by vm.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by vm.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by vm.isBuffering.collectAsStateWithLifecycle()
    val positionSec by vm.positionSec.collectAsStateWithLifecycle()
    val volume by vm.volume.collectAsStateWithLifecycle()
    val shuffleEnabled by vm.shuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by vm.repeatMode.collectAsStateWithLifecycle()
    val spectrumBars by vm.spectrumBars.collectAsStateWithLifecycle()
    val youtubeStatusLabel by vm.youtubeStatusLabel.collectAsStateWithLifecycle()
    val youtubeCandidatesState by vm.youtubeCandidatesState.collectAsStateWithLifecycle()

    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val platformFilter by vm.platformFilter.collectAsStateWithLifecycle()
    val genreFilter by vm.genreFilter.collectAsStateWithLifecycle()
    val recognitionState by vm.recognitionState.collectAsStateWithLifecycle()
    val spotifyImportState by vm.spotifyImportState.collectAsStateWithLifecycle()
    val offlineCacheBytes by vm.offlineCacheBytes.collectAsStateWithLifecycle()
    val eqBands by vm.eqBandsLocal.collectAsStateWithLifecycle()
    val uiMessage by vm.uiMessage.collectAsStateWithLifecycle()
    val layoutPreviewMode by vm.layoutPreviewMode.collectAsStateWithLifecycle()
    val syncState by vm.syncState.collectAsStateWithLifecycle()
    val liveYtSearchState by vm.liveYtSearchState.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val playlists = (playlistsState as? UiState.Success)?.data.orEmpty()
    val libraryList = (libraryState as? UiState.Success)?.data.orEmpty()
    val libraryMap = remember(libraryList) { libraryList.associateBy { it.trackId } }
    val playHistory = (historyState as? UiState.Success)?.data.orEmpty()

    val glassIntensity = profile?.glassIntensityEnum() ?: GlassIntensity.BALANCED
    val dynamicAccent = currentTrack?.accentColor ?: ArunCyan

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ArunDestination.HOME.route

    var showFullPlayer by remember { mutableStateOf(false) }
    var activeModal by remember { mutableStateOf(ActiveYouModal.NONE) }
    var trackForPlaylistDialog by remember { mutableStateOf<MusicTrack?>(null) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistDoc?>(null) }
    var showSpotifyImportDialog by remember { mutableStateOf(false) }

    val openSpotifyImport: () -> Unit = {
        vm.resetSpotifyImportState()
        showSpotifyImportDialog = true
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiMessage) {
        val msg = uiMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            vm.consumeMessage()
        }
    }

    ArunMusicTheme(accentColor = dynamicAccent) {
        LiquidGlassAtmosphere(
            accentColor = dynamicAccent,
            glassIntensity = glassIntensity
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = when (layoutPreviewMode) {
                    LayoutPreviewMode.AUTO -> maxWidth >= 720.dp
                    LayoutPreviewMode.MOBILE -> false
                    LayoutPreviewMode.DESKTOP -> true
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    // Desktop / Tablet Liquid Glass Sidebar Navigation Rail
                    if (isWideScreen) {
                        LiquidGlassSidebar(
                            currentRoute = currentRoute,
                            accentColor = dynamicAccent,
                            layoutPreviewMode = layoutPreviewMode,
                            onSwitchToMobile = { vm.setLayoutPreviewMode(LayoutPreviewMode.MOBILE) },
                            onNavigate = { route ->
                                selectedPlaylist = null
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    // Main Content Area
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        NavHost(
                            navController = navController,
                            startDestination = ArunDestination.HOME.route,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 960.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            composable(ArunDestination.HOME.route) {
                                HomeScreen(
                                    profile = profile,
                                    allTracks = allCatalogTracks,
                                    playlists = playlists,
                                    playHistory = playHistory,
                                    libraryTracks = libraryMap,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    glassIntensity = glassIntensity,
                                    syncState = syncState,
                                    layoutPreviewMode = layoutPreviewMode,
                                    onCycleLayoutMode = {
                                        val next = when (layoutPreviewMode) {
                                            LayoutPreviewMode.AUTO -> LayoutPreviewMode.DESKTOP
                                            LayoutPreviewMode.DESKTOP -> LayoutPreviewMode.MOBILE
                                            LayoutPreviewMode.MOBILE -> LayoutPreviewMode.AUTO
                                        }
                                        vm.setLayoutPreviewMode(next)
                                    },
                                    onSyncNow = vm::syncNow,
                                    onPlayTrack = { track, q -> vm.playTrack(track, q) },
                                    onToggleLike = { track, liked -> vm.toggleLikeTrack(track, liked) },
                                    onToggleDownload = { track, dl -> vm.toggleDownloadTrack(track, dl) },
                                    onAddToPlaylist = { track -> trackForPlaylistDialog = track },
                                    onOpenPlaylistDetail = { pl ->
                                        selectedPlaylist = pl
                                        navController.navigate(ArunDestination.LIBRARY.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenSpotifyImport = openSpotifyImport
                                )
                            }

                            composable(ArunDestination.SEARCH.route) {
                                SearchScreen(
                                    allTracks = allCatalogTracks,
                                    searchQuery = searchQuery,
                                    platformFilter = platformFilter,
                                    genreFilter = genreFilter,
                                    libraryTracks = libraryMap,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    glassIntensity = glassIntensity,
                                    liveYtSearchState = liveYtSearchState,
                                    isImportingLink = spotifyImportState is SpotifyImportUiState.Importing,
                                    onQueryChange = vm::updateSearchQuery,
                                    onPlatformFilterChange = vm::setPlatformFilter,
                                    onGenreFilterChange = vm::setGenreFilter,
                                    onImportOrPlayLink = { link -> vm.importOrPlayYouTubeLink(link, saveAsPlaylist = true, playImmediately = true) },
                                    onSearchYouTubeMusic = vm::searchYouTubeMusicLive,
                                    onSaveLiveTrack = vm::saveLiveTrackToLibrary,
                                    onPlayTrack = { track, q -> vm.playTrack(track, q) },
                                    onToggleLike = { track, liked -> vm.toggleLikeTrack(track, liked) },
                                    onToggleDownload = { track, dl -> vm.toggleDownloadTrack(track, dl) },
                                    onAddToPlaylist = { track -> trackForPlaylistDialog = track }
                                )
                            }

                            composable(ArunDestination.LIBRARY.route) {
                                LibraryScreen(
                                    allTracks = allCatalogTracks,
                                    playlists = playlists,
                                    libraryTracks = libraryMap,
                                    offlineCacheBytes = offlineCacheBytes,
                                    currentTrack = currentTrack,
                                    isPlaying = isPlaying,
                                    glassIntensity = glassIntensity,
                                    selectedPlaylist = selectedPlaylist,
                                    onSelectPlaylist = { selectedPlaylist = it },
                                    onCreatePlaylist = { name, desc, cover -> vm.createPlaylist(name, desc, cover) },
                                    onUpdatePlaylist = vm::updatePlaylist,
                                    onDeletePlaylist = vm::deletePlaylist,
                                    onPlayTrack = { track, q -> vm.playTrack(track, q) },
                                    onToggleLike = { track, liked -> vm.toggleLikeTrack(track, liked) },
                                    onToggleSaveAlbum = vm::toggleSaveAlbum,
                                    onToggleDownload = { track, dl -> vm.toggleDownloadTrack(track, dl) },
                                    onAddToPlaylist = { track -> trackForPlaylistDialog = track },
                                    onOpenSpotifyImport = openSpotifyImport,
                                    onImportYouTubeLink = { link -> vm.importOrPlayYouTubeLink(link, saveAsPlaylist = true, playImmediately = false) },
                                    onDeleteImportedTrack = vm::deleteImportedTrack
                                )
                            }

                            composable(ArunDestination.YOU.route) {
                                YouHubScreen(
                                    profile = profile,
                                    playHistory = playHistory,
                                    offlineCacheBytes = offlineCacheBytes,
                                    glassIntensity = glassIntensity,
                                    syncState = syncState,
                                    layoutPreviewMode = layoutPreviewMode,
                                    onSyncNow = vm::syncNow,
                                    onChangeLayoutMode = vm::setLayoutPreviewMode,
                                    onImportYouTubeLink = { link -> vm.importOrPlayYouTubeLink(link, saveAsPlaylist = true, playImmediately = true) },
                                    onToggleAutoSync = vm::setAutoSync,
                                    onUpdatePlatformConnection = vm::updatePlatformConnection,
                                    onToggleLinkedApp = vm::toggleLinkedMusicApp,
                                    onOpenRecognizer = { activeModal = ActiveYouModal.RECOGNIZER },
                                    onOpenEqualizer = { activeModal = ActiveYouModal.EQUALIZER },
                                    onOpenActivityRecap = { activeModal = ActiveYouModal.RECAP },
                                    onOpenSettings = { activeModal = ActiveYouModal.SETTINGS },
                                    onOpenSpotifyImport = openSpotifyImport
                                )
                            }
                        }

                        // Global Dynamic Island floating at top
                        InteractiveDynamicIsland(
                            track = currentTrack,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            positionSec = positionSec,
                            spectrumBars = spectrumBars,
                            onTogglePlayPause = vm::togglePlayPause,
                            onPrevious = vm::playPrevious,
                            onNext = vm::playNext,
                            onOpenFullPlayer = { showFullPlayer = true },
                            modifier = Modifier.align(Alignment.TopCenter)
                        )

                        // Bottom Dock: Persistent Mini-Player + Mobile Bottom Bar
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .navigationBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SnackbarHost(hostState = snackbarHostState)

                            val activeTrack = currentTrack
                            if (activeTrack != null) {
                                PersistentMiniPlayer(
                                    track = activeTrack,
                                    isPlaying = isPlaying,
                                    isBuffering = isBuffering,
                                    positionSec = positionSec,
                                    isLiked = libraryMap[activeTrack.id]?.liked == true,
                                    glassIntensity = glassIntensity,
                                    onTogglePlayPause = vm::togglePlayPause,
                                    onNext = vm::playNext,
                                    onToggleLike = {
                                        vm.toggleLikeTrack(activeTrack, libraryMap[activeTrack.id]?.liked == true)
                                    },
                                    onOpenFullPlayer = { showFullPlayer = true }
                                )
                            }

                            if (!isWideScreen) {
                                LiquidGlassBottomBar(
                                    currentRoute = currentRoute,
                                    accentColor = dynamicAccent,
                                    onNavigate = { route ->
                                        selectedPlaylist = null
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Full-Screen Now Playing Overlay
            AnimatedVisibility(
                visible = showFullPlayer && currentTrack != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                val track = currentTrack
                if (track != null) {
                    FullScreenNowPlayingModal(
                        track = track,
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        youtubeStatusLabel = youtubeStatusLabel,
                        positionSec = positionSec,
                        volume = volume,
                        shuffleEnabled = shuffleEnabled,
                        repeatMode = repeatMode,
                        spectrumBars = spectrumBars,
                        isLiked = libraryMap[track.id]?.liked == true,
                        isDownloaded = libraryMap[track.id]?.downloaded == true,
                        glassIntensity = glassIntensity,
                        onClose = { showFullPlayer = false },
                        onTogglePlayPause = vm::togglePlayPause,
                        onPrevious = vm::playPrevious,
                        onNext = vm::playNext,
                        onSeekTo = vm::seekTo,
                        onVolumeChange = vm::setVolume,
                        onToggleShuffle = vm::toggleShuffle,
                        onCycleRepeat = vm::cycleRepeatMode,
                        onToggleLike = { vm.toggleLikeTrack(track, libraryMap[track.id]?.liked == true) },
                        onToggleDownload = { vm.toggleDownloadTrack(track, libraryMap[track.id]?.downloaded == true) },
                        onAddToPlaylist = { trackForPlaylistDialog = track },
                        onOpenEqualizer = {
                            showFullPlayer = false
                            activeModal = ActiveYouModal.EQUALIZER
                        },
                        onOpenYouTubePicker = { vm.openYouTubeSourcePicker(track) }
                    )
                }
            }

            // Sub-modals for Kamu (You) Hub tools
            when (activeModal) {
                ActiveYouModal.RECOGNIZER -> {
                    SongRecognitionModal(
                        state = recognitionState,
                        glassIntensity = glassIntensity,
                        onStartRecognition = vm::startSongRecognition,
                        onReset = vm::resetSongRecognition,
                        onPlayTrack = { track ->
                            vm.playTrack(track)
                            activeModal = ActiveYouModal.NONE
                            showFullPlayer = true
                        },
                        onLikeTrack = { track ->
                            vm.toggleLikeTrack(track, libraryMap[track.id]?.liked == true)
                        },
                        onClose = { activeModal = ActiveYouModal.NONE }
                    )
                }
                ActiveYouModal.EQUALIZER -> {
                    EqualizerModal(
                        activePreset = profile?.eqPresetEnum() ?: EqPreset.FLAT,
                        bands = eqBands,
                        glassIntensity = glassIntensity,
                        onSelectPreset = vm::selectEqPreset,
                        onBandChange = vm::updateEqBand,
                        onClose = { activeModal = ActiveYouModal.NONE }
                    )
                }
                ActiveYouModal.RECAP -> {
                    ActivityAndRecapModal(
                        allTracks = allCatalogTracks,
                        playHistory = playHistory,
                        libraryTracks = libraryMap,
                        glassIntensity = glassIntensity,
                        onPlayTrack = { track ->
                            vm.playTrack(track)
                            activeModal = ActiveYouModal.NONE
                        },
                        onClearHistory = { vm.clearHistory(playHistory) },
                        onClose = { activeModal = ActiveYouModal.NONE }
                    )
                }
                ActiveYouModal.SETTINGS -> {
                    SettingsAndAboutModal(
                        profile = profile,
                        offlineCacheBytes = offlineCacheBytes,
                        layoutPreviewMode = layoutPreviewMode,
                        onChangeLayoutMode = vm::setLayoutPreviewMode,
                        onUpdateStreamQuality = { vm.updateSettings(streamQuality = it) },
                        onUpdateDownloadQuality = { vm.updateSettings(downloadQuality = it) },
                        onUpdateGlassIntensity = { vm.updateSettings(glassIntensity = it) },
                        onClearCache = {
                            val dlIds = libraryList.filter { it.downloaded }.map { it.trackId }
                            vm.clearOfflineCache(dlIds)
                        },
                        onSignOut = {
                            activeModal = ActiveYouModal.NONE
                            signOut(credentialManager, scope)
                        },
                        onClose = { activeModal = ActiveYouModal.NONE }
                    )
                }
                ActiveYouModal.NONE -> Unit
            }

            val pendingTrack = trackForPlaylistDialog
            if (pendingTrack != null) {
                AddToPlaylistDialog(
                    track = pendingTrack,
                    playlists = playlists,
                    onSelectPlaylist = { pl -> vm.addTrackToPlaylist(pl, pendingTrack.id) },
                    onCreateNewPlaylist = { name, desc ->
                        vm.createPlaylist(name, desc, pendingTrack.coverKey, pendingTrack.id)
                    },
                    onDismiss = { trackForPlaylistDialog = null }
                )
            }

            if (showSpotifyImportDialog) {
                SpotifyImportDialog(
                    importState = spotifyImportState,
                    onImportUrl = { url, savePlaylist ->
                        vm.importSpotifyUrl(url, savePlaylist)
                    },
                    onImportFile = { uri, savePlaylist ->
                        val fileName = runCatching {
                            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                                if (c.moveToFirst()) c.getString(0) else null
                            }
                        }.getOrNull() ?: uri.lastPathSegment
                        vm.importSpotifyFile(context, uri, fileName, savePlaylist)
                    },
                    onResetState = vm::resetSpotifyImportState,
                    onDismiss = {
                        vm.resetSpotifyImportState()
                        showSpotifyImportDialog = false
                    }
                )
            }

            YouTubeSourcePickerDialog(
                state = youtubeCandidatesState,
                onSelectCandidate = vm::selectYouTubeCandidate,
                onRetrySearch = vm::openYouTubeSourcePicker,
                onDismiss = vm::closeYouTubePicker
            )
        }
    }
}

@Composable
private fun LiquidGlassBottomBar(
    currentRoute: String,
    accentColor: Color,
    onNavigate: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = ObsidianSurface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, GlassBorderSpecular),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp)
            .testTag("bottom_nav_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArunDestination.entries.forEach { dest ->
                val selected = currentRoute == dest.route
                Surface(
                    shape = CircleShape,
                    color = if (selected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable { onNavigate(dest.route) }
                        .testTag("nav_${dest.route}")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = dest.label,
                            tint = if (selected) accentColor else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) accentColor else TextMuted,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassSidebar(
    currentRoute: String,
    accentColor: Color,
    layoutPreviewMode: LayoutPreviewMode,
    onSwitchToMobile: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Surface(
        color = ObsidianSurface.copy(alpha = 0.82f),
        border = BorderStroke(1.dp, GlassBorderSubtle),
        modifier = Modifier
            .width(210.dp)
            .fillMaxHeight()
            .testTag("desktop_sidebar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Arun Music",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(6.dp))
            ArunDestination.entries.forEach { dest ->
                val selected = currentRoute == dest.route
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                    border = if (selected) BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigate(dest.route) }
                        .testTag("sidebar_${dest.route}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = dest.label,
                            tint = if (selected) accentColor else TextMuted
                        )
                        Text(
                            text = dest.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) accentColor else TextMuted,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            if (layoutPreviewMode == LayoutPreviewMode.DESKTOP) {
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, GlassBorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .clickable(onClick = onSwitchToMobile)
                        .testTag("sidebar_switch_mobile")
                ) {
                    Text(
                        text = "Kembali ke Mobile",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
