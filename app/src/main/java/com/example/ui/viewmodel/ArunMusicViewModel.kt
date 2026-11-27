package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.audio.ArunAudioEngine
import com.example.audio.OfflineCacheManager
import com.example.audio.SongRecognizer
import com.example.data.MusicCatalog
import com.example.data.MusicRepository
import com.example.data.SpotifyImporter
import com.example.data.YouTubeAudioResolver
import com.example.data.YouTubeCandidate
import com.example.data.YouTubeResolvedAudio
import com.example.model.DownloadQuality
import com.example.model.EqPreset
import com.example.model.GlassIntensity
import com.example.model.ImportedTrackDoc
import com.example.model.LibraryTrackDoc
import com.example.model.MusicTrack
import com.example.model.PlayHistoryDoc
import com.example.model.PlatformSource
import com.example.model.PlaylistDoc
import com.example.model.RepeatMode
import com.example.model.StreamQuality
import com.example.model.SynthTimbre
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface RecognitionState {
    data object Idle : RecognitionState
    data class Listening(val amplitude: Float) : RecognitionState
    data class Matched(val track: MusicTrack, val confidencePercent: Int) : RecognitionState
}

sealed interface SpotifyImportUiState {
    data object Idle : SpotifyImportUiState
    data object Importing : SpotifyImportUiState
    data class Success(val trackCount: Int, val playlistName: String) : SpotifyImportUiState
    data class Error(val message: String) : SpotifyImportUiState
}

sealed interface YouTubePickerUiState {
    data object Idle : YouTubePickerUiState
    data class Loading(val track: MusicTrack) : YouTubePickerUiState
    data class Loaded(val track: MusicTrack, val candidates: List<YouTubeCandidate>) : YouTubePickerUiState
    data class Error(val track: MusicTrack, val message: String) : YouTubePickerUiState
}

enum class LayoutPreviewMode(val label: String) {
    AUTO("Otomatis"),
    MOBILE("Mobile"),
    DESKTOP("Desktop")
}

data class SyncStateSnapshot(
    val revision: Int = 1,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val sessionId: String = "",
    val isSyncing: Boolean = false,
    val lastSource: String = "Cloud Firestore"
)

sealed interface LiveYtSearchUiState {
    data object Idle : LiveYtSearchUiState
    data object Loading : LiveYtSearchUiState
    data class Loaded(val query: String, val tracks: List<MusicTrack>) : LiveYtSearchUiState
    data class Error(val message: String) : LiveYtSearchUiState
}

class ArunMusicViewModel(
    appContext: Context,
    private val repository: MusicRepository,
    private val offlineCacheManager: OfflineCacheManager,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String?
) : ViewModel() {

    private fun <T> Flow<T>.toUiStateFlow(tagLabel: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $tagLabel", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat $tagLabel"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val profileState: StateFlow<UiState<UserProfile?>> =
        repository.observeProfile(currentUserId).toUiStateFlow("profil")

    val playlistsState: StateFlow<UiState<List<PlaylistDoc>>> =
        repository.observePlaylists(currentUserId).toUiStateFlow("playlist")

    val libraryTracksState: StateFlow<UiState<List<LibraryTrackDoc>>> =
        repository.observeLibraryTracks(currentUserId).toUiStateFlow("pustaka")

    val playHistoryState: StateFlow<UiState<List<PlayHistoryDoc>>> =
        repository.observePlayHistory(currentUserId).toUiStateFlow("riwayat")

    val importedTracksState: StateFlow<UiState<List<ImportedTrackDoc>>> =
        repository.observeImportedTracks(currentUserId).toUiStateFlow("impor spotify")

    val allCatalogTracks: StateFlow<List<MusicTrack>> = importedTracksState
        .map { state ->
            val imported = (state as? UiState.Success)?.data.orEmpty().map {
                it.toMusicTrack(R.drawable.img_album_emerald)
            }
            imported + MusicCatalog.tracks
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MusicCatalog.tracks
        )

    private val audioEngine = ArunAudioEngine(
        context = appContext.applicationContext,
        onTrackFinished = { handleTrackCompleted() },
        onStreamError = { trk, msg -> handleStreamPlaybackError(trk, msg) }
    )

    val currentTrack: StateFlow<MusicTrack?> = audioEngine.currentTrack
    val isPlaying: StateFlow<Boolean> = audioEngine.isPlaying
    val isBuffering: StateFlow<Boolean> = audioEngine.isBuffering
    val positionSec: StateFlow<Int> = audioEngine.positionSec
    val volume: StateFlow<Float> = audioEngine.volume
    val spectrumBars: StateFlow<List<Float>> = audioEngine.spectrumBars

    private val _youtubeStatusLabel = MutableStateFlow<String?>(null)
    val youtubeStatusLabel: StateFlow<String?> = _youtubeStatusLabel.asStateFlow()

    private val _youtubeCandidatesState = MutableStateFlow<YouTubePickerUiState>(YouTubePickerUiState.Idle)
    val youtubeCandidatesState: StateFlow<YouTubePickerUiState> = _youtubeCandidatesState.asStateFlow()

    private var resolveJob: Job? = null
    private val resolvedStreamCache = mutableMapOf<String, YouTubeResolvedAudio>()
    private val streamRetryCounts = mutableMapOf<String, Int>()

    private val _queue = MutableStateFlow<List<MusicTrack>>(MusicCatalog.tracks)
    val queue: StateFlow<List<MusicTrack>> = _queue.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _platformFilter = MutableStateFlow<PlatformSource?>(null)
    val platformFilter: StateFlow<PlatformSource?> = _platformFilter.asStateFlow()

    private val _genreFilter = MutableStateFlow<String?>(null)
    val genreFilter: StateFlow<String?> = _genreFilter.asStateFlow()

    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()

    private val _spotifyImportState = MutableStateFlow<SpotifyImportUiState>(SpotifyImportUiState.Idle)
    val spotifyImportState: StateFlow<SpotifyImportUiState> = _spotifyImportState.asStateFlow()

    private val _offlineCacheBytes = MutableStateFlow(0L)
    val offlineCacheBytes: StateFlow<Long> = _offlineCacheBytes.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _eqBandsLocal = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val eqBandsLocal: StateFlow<List<Int>> = _eqBandsLocal.asStateFlow()

    private val _layoutPreviewMode = MutableStateFlow(LayoutPreviewMode.AUTO)
    val layoutPreviewMode: StateFlow<LayoutPreviewMode> = _layoutPreviewMode.asStateFlow()

    private val _syncState = MutableStateFlow(
        SyncStateSnapshot(
            revision = 1,
            updatedAtMillis = System.currentTimeMillis(),
            sessionId = UUID.randomUUID().toString().take(8),
            isSyncing = false,
            lastSource = "Cloud Firestore"
        )
    )
    val syncState: StateFlow<SyncStateSnapshot> = _syncState.asStateFlow()

    private val _liveYtSearchState = MutableStateFlow<LiveYtSearchUiState>(LiveYtSearchUiState.Idle)
    val liveYtSearchState: StateFlow<LiveYtSearchUiState> = _liveYtSearchState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserSeeded(currentUserId, userDisplayName, userEmail)
                .onFailure { _uiMessage.value = "Gagal inisialisasi data cloud: ${it.localizedMessage}" }
            _offlineCacheBytes.value = offlineCacheManager.getCacheSizeBytes()
        }
        viewModelScope.launch {
            profileState.collect { state ->
                if (state is UiState.Success && state.data != null) {
                    val bands = state.data.eqBandInts()
                    _eqBandsLocal.value = bands
                    audioEngine.updateEqBands(bands)
                }
            }
        }
        viewModelScope.launch {
            combine(
                profileState,
                playlistsState,
                libraryTracksState,
                importedTracksState
            ) { p, pl, lib, imp ->
                listOf(p, pl, lib, imp).count { it is UiState.Success }
            }.collect { successStreams ->
                if (successStreams > 0) {
                    bumpRevision("Cloud Firestore")
                }
            }
        }
    }

    private fun bumpRevision(source: String = "Cloud Firestore") {
        val curr = _syncState.value
        _syncState.value = curr.copy(
            revision = curr.revision + 1,
            updatedAtMillis = System.currentTimeMillis(),
            lastSource = source
        )
    }

    fun setLayoutPreviewMode(mode: LayoutPreviewMode) {
        _layoutPreviewMode.value = mode
    }

    fun syncNow() {
        if (_syncState.value.isSyncing) return
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(isSyncing = true)
            val res = repository.triggerSyncCheck(currentUserId)
            _offlineCacheBytes.value = offlineCacheManager.getCacheSizeBytes()
            val nextRev = _syncState.value.revision + 1
            _syncState.value = _syncState.value.copy(
                revision = nextRev,
                updatedAtMillis = System.currentTimeMillis(),
                isSyncing = false,
                lastSource = "Cloud Firestore"
            )
            _uiMessage.value = if (res.isSuccess) {
                "Sinkronisasi real-time selesai (Rev #$nextRev)"
            } else {
                "Sinkronisasi lokal aktif; menunggu koneksi cloud"
            }
        }
    }

    fun consumeMessage() {
        _uiMessage.value = null
    }

    fun playTrack(track: MusicTrack, newQueue: List<MusicTrack>? = null) {
        if (newQueue != null && newQueue.isNotEmpty()) {
            _queue.value = newQueue
        }
        resolveJob?.cancel()
        streamRetryCounts[track.id] = 0

        if (track.isImported || track.youtubeVideoId.isNotBlank()) {
            playImportedYouTubeTrack(track = track, forceRefresh = false, skipFirstClient = false)
        } else {
            _youtubeStatusLabel.value = null
            audioEngine.playTrack(track, 0)
        }

        viewModelScope.launch {
            val res = repository.recordPlay(currentUserId, track, (track.durationSec / 2).toLong().coerceAtLeast(30L))
            if (res.isFailure) {
                Log.w(TAG, "Gagal mencatat riwayat putar: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    private fun playImportedYouTubeTrack(
        track: MusicTrack,
        forceRefresh: Boolean,
        skipFirstClient: Boolean
    ) {
        if (!forceRefresh) {
            val cached = resolvedStreamCache[track.id]
            if (cached != null) {
                val updatedTrack = track.copy(
                    youtubeVideoId = cached.videoId,
                    youtubeTitle = cached.title,
                    durationSec = cached.durationSec
                )
                _youtubeStatusLabel.value = "Audio Penuh YouTube • ${cached.title}"
                audioEngine.playStream(updatedTrack, cached.streamUrl, cached.userAgent, 0)
                return
            }
        }

        audioEngine.prepareExternalLoading(track)
        _youtubeStatusLabel.value = "Mencari audio full dari YouTube..."

        resolveJob = viewModelScope.launch {
            val resolvedRes = YouTubeAudioResolver.resolveTrack(
                title = track.title,
                artist = track.artist,
                existingVideoId = track.youtubeVideoId,
                skipFirstClient = skipFirstClient
            )
            resolvedRes.fold(
                onSuccess = { resolved ->
                    resolvedStreamCache[track.id] = resolved
                    val updatedTrack = track.copy(
                        youtubeVideoId = resolved.videoId,
                        youtubeTitle = resolved.title,
                        durationSec = resolved.durationSec
                    )
                    _youtubeStatusLabel.value = "Audio Penuh YouTube • ${resolved.title}"
                    audioEngine.playStream(updatedTrack, resolved.streamUrl, resolved.userAgent, 0)

                    if (track.isImported && (track.youtubeVideoId != resolved.videoId || track.durationSec != resolved.durationSec)) {
                        launch {
                            repository.updateImportedTrackYouTubeInfo(
                                userId = currentUserId,
                                trackId = track.id,
                                youtubeVideoId = resolved.videoId,
                                youtubeTitle = resolved.title,
                                durationSec = resolved.durationSec.toLong()
                            )
                        }
                    }
                },
                onFailure = { err ->
                    _youtubeStatusLabel.value = null
                    audioEngine.pause()
                    _uiMessage.value = err.localizedMessage ?: "Gagal memuat audio full YouTube untuk \"${track.title}\""
                }
            )
        }
    }

    private fun handleStreamPlaybackError(track: MusicTrack, message: String) {
        resolvedStreamCache.remove(track.id)
        val attempt = (streamRetryCounts[track.id] ?: 0) + 1
        if (attempt <= 1) {
            streamRetryCounts[track.id] = attempt
            Log.w(TAG, "Stream YouTube gagal, mencoba sumber cadangan untuk ${track.title}: $message")
            playImportedYouTubeTrack(track = track, forceRefresh = true, skipFirstClient = true)
        } else {
            streamRetryCounts.remove(track.id)
            _youtubeStatusLabel.value = null
            _uiMessage.value = "Gagal memutar audio YouTube: $message"
        }
    }

    fun openYouTubeSourcePicker(track: MusicTrack) {
        _youtubeCandidatesState.value = YouTubePickerUiState.Loading(track)
        viewModelScope.launch {
            val cleanArtist = track.artist.trim()
            val query = if (cleanArtist.isBlank() || cleanArtist.equals("Artis Spotify", ignoreCase = true)) {
                "${track.title} official audio"
            } else {
                "${track.title} $cleanArtist"
            }
            val result = YouTubeAudioResolver.searchCandidates(query, limit = 8)
            result.fold(
                onSuccess = { list ->
                    _youtubeCandidatesState.value = YouTubePickerUiState.Loaded(track, list)
                },
                onFailure = { err ->
                    _youtubeCandidatesState.value = YouTubePickerUiState.Error(
                        track,
                        err.localizedMessage ?: "Gagal mencari sumber audio di YouTube"
                    )
                }
            )
        }
    }

    fun selectYouTubeCandidate(track: MusicTrack, candidate: YouTubeCandidate) {
        _youtubeCandidatesState.value = YouTubePickerUiState.Idle
        resolvedStreamCache.remove(track.id)
        streamRetryCounts.remove(track.id)
        val updated = track.copy(
            youtubeVideoId = candidate.videoId,
            youtubeTitle = candidate.title,
            durationSec = candidate.durationSec
        )
        playTrack(updated)
    }

    fun closeYouTubePicker() {
        _youtubeCandidatesState.value = YouTubePickerUiState.Idle
    }

    fun togglePlayPause() {
        if (audioEngine.currentTrack.value == null) {
            val first = allCatalogTracks.value.firstOrNull() ?: MusicCatalog.tracks.first()
            playTrack(first, allCatalogTracks.value)
        } else if (resolveJob?.isActive == true && !audioEngine.isPlaying.value) {
            resolveJob?.cancel()
            audioEngine.pause()
            _youtubeStatusLabel.value = null
        } else {
            audioEngine.togglePlayPause()
        }
    }

    fun seekTo(sec: Int) {
        audioEngine.seekTo(sec)
    }

    fun setVolume(vol: Float) {
        audioEngine.setVolume(vol)
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun playNext() {
        val list = _queue.value.ifEmpty { allCatalogTracks.value }
        val curr = audioEngine.currentTrack.value
        val next = if (_shuffleEnabled.value && list.size > 1) {
            list.filter { it.id != curr?.id }.random()
        } else {
            val idx = list.indexOfFirst { it.id == curr?.id }
            if (idx < 0 || idx + 1 >= list.size) list.first() else list[idx + 1]
        }
        playTrack(next)
    }

    fun playPrevious() {
        if (audioEngine.positionSec.value > 4) {
            audioEngine.seekTo(0)
            return
        }
        val list = _queue.value.ifEmpty { allCatalogTracks.value }
        val curr = audioEngine.currentTrack.value
        val idx = list.indexOfFirst { it.id == curr?.id }
        val prev = if (idx <= 0) list.last() else list[idx - 1]
        playTrack(prev)
    }

    private fun handleTrackCompleted() {
        val curr = audioEngine.currentTrack.value ?: return
        when (_repeatMode.value) {
            RepeatMode.ONE -> playTrack(curr)
            RepeatMode.ALL -> playNext()
            RepeatMode.OFF -> {
                val list = _queue.value.ifEmpty { allCatalogTracks.value }
                val idx = list.indexOfFirst { it.id == curr.id }
                if (idx >= 0 && idx + 1 < list.size) {
                    playTrack(list[idx + 1])
                }
            }
        }
    }

    fun updateSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setPlatformFilter(source: PlatformSource?) {
        _platformFilter.value = source
    }

    fun setGenreFilter(genre: String?) {
        _genreFilter.value = genre
    }

    fun importSpotifyUrl(url: String, saveAsPlaylist: Boolean) {
        if (_spotifyImportState.value is SpotifyImportUiState.Importing) return
        viewModelScope.launch {
            _spotifyImportState.value = SpotifyImportUiState.Importing
            val parsed = SpotifyImporter.importFromUrl(url)
            parsed.fold(
                onSuccess = { batch ->
                    val saved = repository.saveImportedSpotifyBatch(currentUserId, batch, saveAsPlaylist)
                    saved.fold(
                        onSuccess = { count ->
                            _spotifyImportState.value = SpotifyImportUiState.Success(count, batch.suggestedPlaylistName)
                            _uiMessage.value = "$count lagu berhasil diimpor & disinkronkan"
                            bumpRevision("Impor Tautan")
                        },
                        onFailure = { err ->
                            _spotifyImportState.value = SpotifyImportUiState.Error(err.localizedMessage ?: "Gagal menyimpan impor")
                        }
                    )
                },
                onFailure = { err ->
                    _spotifyImportState.value = SpotifyImportUiState.Error(err.localizedMessage ?: "Gagal mengimpor tautan")
                }
            )
        }
    }

    fun importOrPlayYouTubeLink(
        url: String,
        saveAsPlaylist: Boolean = true,
        playImmediately: Boolean = true
    ) {
        val cleaned = url.trim()
        if (cleaned.isBlank()) {
            _uiMessage.value = "Tempel tautan YouTube Music atau Spotify terlebih dahulu"
            return
        }
        if (_spotifyImportState.value is SpotifyImportUiState.Importing) return
        viewModelScope.launch {
            _spotifyImportState.value = SpotifyImportUiState.Importing
            val parsed = SpotifyImporter.importFromUrl(cleaned)
            parsed.fold(
                onSuccess = { batch ->
                    val isPlaylistLink = batch.tracks.size > 1 || cleaned.contains("playlist?list=", ignoreCase = true)
                    val saved = repository.saveImportedSpotifyBatch(
                        userId = currentUserId,
                        importBatch = batch,
                        saveAsPlaylist = saveAsPlaylist && isPlaylistLink
                    )
                    saved.fold(
                        onSuccess = { count ->
                            _spotifyImportState.value = SpotifyImportUiState.Success(count, batch.suggestedPlaylistName)
                            bumpRevision("YouTube Music")
                            _uiMessage.value = if (count > 1) {
                                "$count lagu dari ${batch.suggestedPlaylistName} berhasil disinkronkan"
                            } else {
                                "\"${batch.tracks.firstOrNull()?.title ?: "Lagu"}\" siap diputar penuh"
                            }
                            if (playImmediately && batch.tracks.isNotEmpty()) {
                                val tracks = batch.tracks.map { cand ->
                                    ImportedTrackDoc(
                                        id = cand.trackId,
                                        userId = currentUserId,
                                        trackId = cand.trackId,
                                        title = cand.title,
                                        artist = cand.artist,
                                        album = cand.album,
                                        genre = cand.genre,
                                        durationSec = cand.durationSec,
                                        artworkUrl = cand.artworkUrl,
                                        previewUrl = "",
                                        spotifyUri = cand.spotifyUri,
                                        youtubeVideoId = cand.youtubeVideoId,
                                        youtubeTitle = cand.youtubeTitle
                                    ).toMusicTrack(R.drawable.img_album_midnight)
                                }
                                playTrack(tracks.first(), tracks)
                            }
                        },
                        onFailure = { err ->
                            _spotifyImportState.value = SpotifyImportUiState.Error(err.localizedMessage ?: "Gagal menyimpan tautan")
                            _uiMessage.value = err.localizedMessage ?: "Gagal menyimpan tautan"
                        }
                    )
                },
                onFailure = { err ->
                    _spotifyImportState.value = SpotifyImportUiState.Error(err.localizedMessage ?: "Tautan YouTube Music / Spotify tidak valid")
                    _uiMessage.value = err.localizedMessage ?: "Tautan YouTube Music / Spotify tidak valid"
                }
            )
        }
    }

    fun searchYouTubeMusicLive(query: String) {
        val clean = query.trim()
        if (clean.length < 2) {
            _liveYtSearchState.value = LiveYtSearchUiState.Idle
            return
        }
        viewModelScope.launch {
            _liveYtSearchState.value = LiveYtSearchUiState.Loading
            val res = YouTubeAudioResolver.searchCandidates(clean, limit = 8)
            res.fold(
                onSuccess = { list ->
                    val tracks = list.map { cand ->
                        MusicTrack(
                            id = "yt_${cand.videoId}",
                            title = cand.title,
                            artist = cand.channel,
                            album = "YouTube Music",
                            genre = "YouTube Music",
                            durationSec = cand.durationSec,
                            bpm = 110,
                            rootMidi = 57,
                            scaleIntervals = listOf(0, 3, 7, 10, 14),
                            timbre = SynthTimbre.WARM_EP,
                            coverRes = R.drawable.img_album_midnight,
                            coverKey = "midnight",
                            accentColor = Color(0xFFFF4D6D),
                            platform = PlatformSource.YOUTUBE_MUSIC,
                            releaseYear = 2026,
                            artworkUrl = cand.thumbnailUrl,
                            previewUrl = "",
                            spotifyUri = YouTubeAudioResolver.buildMusicWatchUrl(cand.videoId),
                            youtubeVideoId = cand.videoId,
                            youtubeTitle = cand.title,
                            isImported = true
                        )
                    }
                    _liveYtSearchState.value = LiveYtSearchUiState.Loaded(clean, tracks)
                },
                onFailure = { err ->
                    _liveYtSearchState.value = LiveYtSearchUiState.Error(
                        err.localizedMessage ?: "Gagal mencari di YouTube Music"
                    )
                }
            )
        }
    }

    fun saveLiveTrackToLibrary(track: MusicTrack) {
        importOrPlayYouTubeLink(
            url = YouTubeAudioResolver.buildMusicWatchUrl(track.youtubeVideoId),
            saveAsPlaylist = false,
            playImmediately = false
        )
    }

    fun importSpotifyFile(context: Context, uri: Uri, fileName: String?, saveAsPlaylist: Boolean) {
        if (_spotifyImportState.value is SpotifyImportUiState.Importing) return
        viewModelScope.launch {
            _spotifyImportState.value = SpotifyImportUiState.Importing
            val parsed = SpotifyImporter.importFromFile(context, uri, fileName)
            parsed.fold(
                onSuccess = { batch ->
                    val saved = repository.saveImportedSpotifyBatch(currentUserId, batch, saveAsPlaylist)
                    saved.fold(
                        onSuccess = { count ->
                            _spotifyImportState.value = SpotifyImportUiState.Success(count, batch.suggestedPlaylistName)
                            _uiMessage.value = "$count lagu Spotify berhasil diimpor dari file"
                        },
                        onFailure = { err ->
                            _spotifyImportState.value = SpotifyImportUiState.Error(err.localizedMessage ?: "Gagal menyimpan impor")
                        }
                    )
                },
                onFailure = { err ->
                    _spotifyImportState.value = SpotifyImportUiState.Error(err.localizedMessage ?: "Gagal membaca file Spotify")
                }
            )
        }
    }

    fun resetSpotifyImportState() {
        _spotifyImportState.value = SpotifyImportUiState.Idle
    }

    fun deleteImportedTrack(trackId: String) {
        viewModelScope.launch {
            val res = repository.deleteImportedTrack(currentUserId, trackId)
            _uiMessage.value = if (res.isSuccess) "Lagu impor dihapus" else "Gagal menghapus lagu impor"
        }
    }

    fun toggleLikeTrack(track: MusicTrack, currentlyLiked: Boolean) {
        viewModelScope.launch {
            val result = repository.upsertLibraryTrackFlag(
                userId = currentUserId,
                trackId = track.id,
                liked = !currentlyLiked
            )
            _uiMessage.value = if (result.isSuccess) {
                if (!currentlyLiked) "Ditambahkan ke Lagu Disukai" else "Dihapus dari Lagu Disukai"
            } else {
                "Gagal memperbarui lagu disukai"
            }
        }
    }

    fun toggleSaveAlbum(trackIds: List<String>, currentlySaved: Boolean) {
        viewModelScope.launch {
            var allOk = true
            trackIds.forEach { id ->
                val res = repository.upsertLibraryTrackFlag(
                    userId = currentUserId,
                    trackId = id,
                    savedAlbum = !currentlySaved
                )
                if (res.isFailure) allOk = false
            }
            _uiMessage.value = if (allOk) {
                if (!currentlySaved) "Album disimpan ke Koleksi" else "Album dihapus dari Koleksi"
            } else {
                "Gagal memperbarui koleksi"
            }
        }
    }

    fun toggleDownloadTrack(track: MusicTrack, currentlyDownloaded: Boolean) {
        viewModelScope.launch {
            if (!currentlyDownloaded) {
                _offlineCacheBytes.value = offlineCacheManager.downloadTrack(track)
                val res = repository.upsertLibraryTrackFlag(
                    userId = currentUserId,
                    trackId = track.id,
                    downloaded = true
                )
                _uiMessage.value = if (res.isSuccess) {
                    "\"${track.title}\" selesai diunduh untuk mode offline"
                } else {
                    "Gagal menyinkronkan status unduhan"
                }
            } else {
                _offlineCacheBytes.value = offlineCacheManager.removeTrack(track.id)
                val res = repository.upsertLibraryTrackFlag(
                    userId = currentUserId,
                    trackId = track.id,
                    downloaded = false
                )
                _uiMessage.value = if (res.isSuccess) {
                    "Unduhan \"${track.title}\" dihapus"
                } else {
                    "Gagal menghapus status unduhan"
                }
            }
        }
    }

    fun createPlaylist(name: String, description: String, coverKey: String, initialTrackId: String? = null) {
        viewModelScope.launch {
            val ids = if (initialTrackId != null) listOf(initialTrackId) else emptyList()
            val res = repository.createPlaylist(currentUserId, name, description, coverKey, ids)
            _uiMessage.value = if (res.isSuccess) "Playlist \"$name\" berhasil dibuat" else "Gagal membuat playlist"
        }
    }

    fun updatePlaylist(playlist: PlaylistDoc, newName: String, newDesc: String, newTrackIds: List<String>) {
        viewModelScope.launch {
            val res = repository.updatePlaylist(currentUserId, playlist.id, newName, newDesc, newTrackIds)
            _uiMessage.value = if (res.isSuccess) "Playlist diperbarui" else "Gagal memperbarui playlist"
        }
    }

    fun addTrackToPlaylist(playlist: PlaylistDoc, trackId: String) {
        if (trackId in playlist.trackIds) {
            _uiMessage.value = "Lagu sudah ada di \"${playlist.name}\""
            return
        }
        viewModelScope.launch {
            val res = repository.updatePlaylist(
                currentUserId,
                playlist.id,
                playlist.name,
                playlist.description,
                playlist.trackIds + trackId
            )
            _uiMessage.value = if (res.isSuccess) "Ditambahkan ke \"${playlist.name}\"" else "Gagal menambahkan lagu"
        }
    }

    fun deletePlaylist(playlist: PlaylistDoc) {
        viewModelScope.launch {
            val res = repository.deletePlaylist(currentUserId, playlist.id)
            _uiMessage.value = if (res.isSuccess) "Playlist \"${playlist.name}\" dihapus" else "Gagal menghapus playlist"
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateProfileFields(currentUserId, mapOf("autoSync" to enabled))
        }
    }

    fun updatePlatformConnection(
        platform: PlatformSource,
        connected: Boolean,
        accountName: String
    ) {
        viewModelScope.launch {
            val updates = when (platform) {
                PlatformSource.YOUTUBE_MUSIC -> mapOf(
                    "ytMusicConnected" to connected,
                    "ytMusicAccount" to accountName.trim().take(100)
                )
                PlatformSource.SPOTIFY -> mapOf(
                    "spotifyConnected" to connected,
                    "spotifyAccount" to accountName.trim().take(100)
                )
            }
            val res = repository.updateProfileFields(currentUserId, updates)
            _uiMessage.value = if (res.isSuccess) {
                if (connected) "${platform.label} berhasil ditautkan" else "${platform.label} diputuskan"
            } else {
                "Gagal memperbarui koneksi ${platform.label}"
            }
        }
    }

    fun toggleLinkedMusicApp(appName: String, currentApps: List<String>) {
        viewModelScope.launch {
            val updated = if (appName in currentApps) {
                currentApps - appName
            } else {
                (currentApps + appName).take(10)
            }
            val res = repository.updateProfileFields(currentUserId, mapOf("linkedApps" to updated))
            if (res.isSuccess) {
                _uiMessage.value = if (appName in updated) "$appName terhubung" else "$appName dilepas"
            }
        }
    }

    fun selectEqPreset(preset: EqPreset) {
        val bands = preset.bands
        _eqBandsLocal.value = bands
        audioEngine.updateEqBands(bands)
        viewModelScope.launch {
            repository.updateProfileFields(
                currentUserId,
                mapOf(
                    "eqPreset" to preset.name,
                    "eqBands" to bands.map { it.toLong() }
                )
            )
        }
    }

    fun updateEqBand(index: Int, valueDb: Int) {
        val updated = _eqBandsLocal.value.toMutableList()
        if (index in updated.indices) {
            updated[index] = valueDb.coerceIn(-12, 12)
            _eqBandsLocal.value = updated
            audioEngine.updateEqBands(updated)
            viewModelScope.launch {
                repository.updateProfileFields(
                    currentUserId,
                    mapOf(
                        "eqPreset" to EqPreset.CUSTOM.name,
                        "eqBands" to updated.map { it.toLong() }
                    )
                )
            }
        }
    }

    fun updateSettings(
        streamQuality: StreamQuality? = null,
        downloadQuality: DownloadQuality? = null,
        glassIntensity: GlassIntensity? = null
    ) {
        val updates = mutableMapOf<String, Any>()
        streamQuality?.let { updates["streamQuality"] = it.name }
        downloadQuality?.let { updates["downloadQuality"] = it.name }
        glassIntensity?.let { updates["glassIntensity"] = it.name }
        if (updates.isEmpty()) return
        viewModelScope.launch {
            val res = repository.updateProfileFields(currentUserId, updates)
            if (res.isSuccess) _uiMessage.value = "Pengaturan disimpan"
        }
    }

    fun clearOfflineCache(downloadedIds: List<String>) {
        viewModelScope.launch {
            _offlineCacheBytes.value = offlineCacheManager.clearAll()
            downloadedIds.forEach { id ->
                repository.upsertLibraryTrackFlag(currentUserId, id, downloaded = false)
            }
            _uiMessage.value = "Cache audio offline berhasil dibersihkan"
        }
    }

    fun clearHistory(historyEntries: List<PlayHistoryDoc>) {
        viewModelScope.launch {
            val res = repository.clearPlayHistory(currentUserId, historyEntries.map { it.id })
            _uiMessage.value = if (res.isSuccess) "Riwayat pemutaran dibersihkan" else "Gagal membersihkan riwayat"
        }
    }

    fun startSongRecognition() {
        if (_recognitionState.value is RecognitionState.Listening) return
        viewModelScope.launch {
            _recognitionState.value = RecognitionState.Listening(0.2f)
            val matched = SongRecognizer.listenAndIdentify(
                currentlyPlayingTrack = if (audioEngine.isPlaying.value) audioEngine.currentTrack.value else null,
                onAmplitudeSample = { amp ->
                    _recognitionState.value = RecognitionState.Listening(amp)
                }
            )
            _recognitionState.value = RecognitionState.Matched(matched, confidencePercent = 96)
        }
    }

    fun resetSongRecognition() {
        _recognitionState.value = RecognitionState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        resolveJob?.cancel()
        audioEngine.release()
    }

    private companion object {
        const val TAG = "ArunMusicVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
