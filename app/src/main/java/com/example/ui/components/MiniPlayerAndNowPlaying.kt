package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.ArunAudioEngine
import com.example.data.YouTubeAudioResolver
import com.example.model.GlassIntensity
import com.example.model.MusicTrack
import com.example.model.RepeatMode
import com.example.ui.theme.ArunCyan
import com.example.ui.theme.ArunEmerald
import com.example.ui.theme.ArunRose
import com.example.ui.theme.GlassBorderSpecular
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YouTubeRed

@Composable
fun PersistentMiniPlayer(
    track: MusicTrack,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    positionSec: Int,
    isLiked: Boolean,
    glassIntensity: GlassIntensity,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ObsidianSurface.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, track.accentColor.copy(alpha = 0.48f)),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenFullPlayer)
            .testTag("persistent_mini_player")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtImage(
                    artworkUrl = track.artworkUrl,
                    fallbackRes = track.coverRes,
                    contentDescription = "Sampul ${track.title}",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        PlatformBadge(platform = track.platform)
                    }
                }
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Sukai",
                        tint = if (isLiked) ArunRose else TextMuted
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = track.accentColor,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onTogglePlayPause)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color(0xFF041421),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = Color(0xFF041421)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Lagu berikutnya",
                        tint = TextPrimary
                    )
                }
            }
            val progress = if (track.durationSec > 0) {
                positionSec.toFloat() / track.durationSec.toFloat()
            } else 0f
            if (isBuffering) {
                LinearProgressIndicator(
                    color = track.accentColor,
                    trackColor = Color.White.copy(alpha = 0.10f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = track.accentColor,
                    trackColor = Color.White.copy(alpha = 0.10f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
        }
    }
}

@Composable
fun FullScreenNowPlayingModal(
    track: MusicTrack,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    youtubeStatusLabel: String? = null,
    positionSec: Int,
    volume: Float,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    spectrumBars: List<Float>,
    isLiked: Boolean,
    isDownloaded: Boolean,
    glassIntensity: GlassIntensity,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenYouTubePicker: (() -> Unit)? = null
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(positionSec.toFloat()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Deep dynamic ambient bloom matching album cover
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.TopCenter)
                .blur(110.dp)
                .clip(CircleShape)
                .background(track.accentColor.copy(alpha = 0.36f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_now_playing")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Tutup pemutar penuh",
                        tint = TextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SEDANG DIPUTAR",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    PlatformBadge(platform = track.platform)
                }

                IconButton(
                    onClick = onAddToPlaylist,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Tambahkan ke playlist",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Album Art with Glass Frame
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, GlassBorderSpecular),
                modifier = Modifier
                    .size(290.dp)
            ) {
                AlbumArtImage(
                    artworkUrl = track.artworkUrl,
                    fallbackRes = track.coverRes,
                    contentDescription = "Sampul album ${track.album}",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(RoundedCornerShape(26.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Real-time audio spectrum visualizer
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = glassIntensity.surfaceAlpha * 0.8f),
                border = BorderStroke(1.dp, GlassBorderSpecular.copy(alpha = 0.35f)),
                modifier = Modifier.widthIn(max = 540.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SpectrumVisualizer(
                        bars = spectrumBars,
                        isPlaying = isPlaying,
                        accentColor = track.accentColor,
                        barCount = 16,
                        maxHeight = 32.dp,
                        barWidth = 4.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Track Info + Quick Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Sukai lagu",
                        tint = if (isLiked) ArunRose else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(
                    onClick = onToggleDownload,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.DownloadForOffline,
                        contentDescription = "Unduh offline",
                        tint = if (isDownloaded) ArunEmerald else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Real-time Seek Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
            ) {
                val sliderPos = if (isScrubbing) scrubValue else positionSec.toFloat()
                Slider(
                    value = sliderPos.coerceIn(0f, track.durationSec.toFloat()),
                    onValueChange = {
                        isScrubbing = true
                        scrubValue = it
                    },
                    onValueChangeFinished = {
                        isScrubbing = false
                        onSeekTo(scrubValue.toInt())
                    },
                    valueRange = 0f..track.durationSec.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = track.accentColor,
                        activeTrackColor = track.accentColor,
                        inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ArunAudioEngine.formatDuration(sliderPos.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    if (isBuffering) {
                        Text(
                            text = "Memuat audio penuh...",
                            style = MaterialTheme.typography.labelSmall,
                            color = ArunCyan
                        )
                    }
                    Text(
                        text = ArunAudioEngine.formatDuration(track.durationSec),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Transport Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Acak",
                        tint = if (shuffleEnabled) track.accentColor else TextMuted
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Sebelumnya",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = track.accentColor,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("full_player_play_pause")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = Color(0xFF041421),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Jeda" else "Putar",
                                tint = Color(0xFF041421),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Berikutnya",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Ulangi",
                        tint = if (repeatMode != RepeatMode.OFF) track.accentColor else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Volume Control + Equalizer Shortcut inside Glass Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = glassIntensity.surfaceAlpha),
                border = BorderStroke(1.dp, GlassBorderSpecular.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeDown,
                        contentDescription = "Volume rendah",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = TextPrimary,
                            activeTrackColor = track.accentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.14f)
                        )
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Volume tinggi",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onOpenEqualizer)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Equalizer,
                                contentDescription = "Buka Ekualizer",
                                tint = track.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "EQ",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (track.isImported || track.youtubeVideoId.isNotBlank() || track.platform == com.example.model.PlatformSource.YOUTUBE_MUSIC) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = YouTubeRed.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, YouTubeRed.copy(alpha = 0.42f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 540.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = youtubeStatusLabel ?: if (track.youtubeTitle.isNotBlank()) {
                                "Audio Penuh YouTube Music • ${track.youtubeTitle}"
                            } else if (track.isImported) {
                                "Sumber Audio Penuh: YouTube Music"
                            } else {
                                "Tersedia di YouTube Music"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onOpenYouTubePicker != null && (track.isImported || track.youtubeVideoId.isNotBlank())) {
                                OutlinedButton(
                                    onClick = onOpenYouTubePicker,
                                    border = BorderStroke(1.dp, GlassBorderSubtle),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("change_youtube_source_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SwapHoriz,
                                        contentDescription = null,
                                        tint = ArunCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ganti Sumber", color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    val targetUrl = if (track.youtubeVideoId.isNotBlank()) {
                                        YouTubeAudioResolver.buildMusicWatchUrl(track.youtubeVideoId)
                                    } else {
                                        YouTubeAudioResolver.buildMusicSearchUrl("${track.title} ${track.artist}")
                                    }
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
                                    }
                                },
                                border = BorderStroke(1.dp, YouTubeRed.copy(alpha = 0.55f)),
                                shape = CircleShape,
                                modifier = if (onOpenYouTubePicker == null || (!track.isImported && track.youtubeVideoId.isBlank())) {
                                    Modifier.fillMaxWidth().testTag("open_youtube_music_button")
                                } else {
                                    Modifier.testTag("open_youtube_music_button")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = "Buka di YouTube Music",
                                    tint = YouTubeRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("Buka di YT Music", color = YouTubeRed, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
