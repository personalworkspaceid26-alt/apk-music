package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.audio.ArunAudioEngine
import com.example.data.YouTubeCandidate
import com.example.model.GlassIntensity
import com.example.model.MusicTrack
import com.example.model.PlatformSource
import com.example.model.PlaylistDoc
import com.example.ui.theme.ArunCyan
import com.example.ui.theme.ArunEmerald
import com.example.ui.theme.ArunIndigo
import com.example.ui.theme.ArunRose
import com.example.ui.theme.GlassBorderSpecular
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YouTubeRed
import com.example.ui.viewmodel.SpotifyImportUiState
import com.example.ui.viewmodel.YouTubePickerUiState

@Composable
fun LiquidGlassAtmosphere(
    accentColor: Color,
    glassIntensity: GlassIntensity,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatedPrimary by animateColorAsState(
        targetValue = accentColor,
        animationSpec = tween(durationMillis = 700),
        label = "ambientPrimary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = ArunIndigo,
        animationSpec = tween(durationMillis = 700),
        label = "ambientSecondary"
    )
    val bloomAlpha = when (glassIntensity) {
        GlassIntensity.SUBTLE -> 0.16f
        GlassIntensity.BALANCED -> 0.24f
        GlassIntensity.DEEP -> 0.34f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Blurred ambient color blooms behind frosted glass surfaces
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .blur(95.dp)
                .clip(CircleShape)
                .background(animatedPrimary.copy(alpha = bloomAlpha))
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 70.dp, y = 40.dp)
                .blur(100.dp)
                .clip(CircleShape)
                .background(animatedSecondary.copy(alpha = bloomAlpha * 0.85f))
        )
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glassIntensity: GlassIntensity = GlassIntensity.BALANCED,
    shape: Shape = RoundedCornerShape(24.dp),
    tintColor: Color = Color.White,
    borderColor: Color = GlassBorderSpecular,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickMod),
        shape = shape,
        color = tintColor.copy(alpha = glassIntensity.surfaceAlpha),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(content = content)
    }
}

@Composable
fun PlatformBadge(
    platform: PlatformSource,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (platform) {
        PlatformSource.SPOTIFY -> SpotifyGreen
        PlatformSource.YOUTUBE_MUSIC -> YouTubeRed
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = badgeColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Text(
                text = if (platform == PlatformSource.YOUTUBE_MUSIC) "YT Music" else "Spotify",
                style = MaterialTheme.typography.labelMedium,
                color = badgeColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SpectrumVisualizer(
    bars: List<Float>,
    isPlaying: Boolean,
    accentColor: Color,
    barCount: Int = 16,
    maxHeight: Dp = 28.dp,
    barWidth: Dp = 3.dp,
    modifier: Modifier = Modifier
) {
    val displayBars = remember(bars, barCount) {
        if (bars.size == barCount) bars
        else List(barCount) { idx ->
            bars.getOrElse((idx * bars.size) / barCount) { 0.1f }
        }
    }
    Row(
        modifier = modifier.height(maxHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        displayBars.forEachIndexed { index, rawAmp ->
            val targetFraction = if (isPlaying) rawAmp.coerceIn(0.14f, 1f) else 0.14f
            val animatedHeight by animateFloatAsState(
                targetValue = targetFraction,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "bar_$index"
            )
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(maxHeight * animatedHeight)
                    .clip(CircleShape)
                    .background(
                        if (isPlaying) accentColor else TextMuted.copy(alpha = 0.45f)
                    )
            )
        }
    }
}

@Composable
fun TrackRowItem(
    track: MusicTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean,
    isDownloaded: Boolean,
    glassIntensity: GlassIntensity,
    onPlayClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (isCurrent) track.accentColor.copy(alpha = 0.7f) else GlassBorderSubtle
    val tint = if (isCurrent) track.accentColor else Color.White

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onPlayClick)
            .testTag("track_row_${track.id}"),
        shape = RoundedCornerShape(18.dp),
        color = tint.copy(alpha = if (isCurrent) 0.16f else glassIntensity.surfaceAlpha * 0.85f),
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(52.dp)) {
                AlbumArtImage(
                    artworkUrl = track.artworkUrl,
                    fallbackRes = track.coverRes,
                    contentDescription = "Sampul ${track.title}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                            contentDescription = "Sedang diputar",
                            tint = track.accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) track.accentColor else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} • ${ArunAudioEngine.formatDuration(track.durationSec)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlatformBadge(platform = track.platform)
                    if (track.isImported) {
                        Surface(
                            shape = CircleShape,
                            color = YouTubeRed.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, YouTubeRed.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "YT Full",
                                style = MaterialTheme.typography.labelSmall,
                                color = YouTubeRed,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Tersimpan offline",
                            tint = ArunEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isLiked) "Hapus dari disukai" else "Sukai lagu",
                    tint = if (isLiked) ArunRose else TextMuted
                )
            }

            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isDownloaded) Icons.Rounded.CheckCircle else Icons.Rounded.DownloadForOffline,
                    contentDescription = if (isDownloaded) "Hapus unduhan" else "Unduh offline",
                    tint = if (isDownloaded) ArunEmerald else TextMuted
                )
            }

            IconButton(
                onClick = onAddToPlaylistClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = "Tambahkan ke playlist",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    track: MusicTrack,
    playlists: List<PlaylistDoc>,
    onSelectPlaylist: (PlaylistDoc) -> Unit,
    onCreateNewPlaylist: (name: String, desc: String) -> Unit,
    onDismiss: () -> Unit
) {
    var creatingNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = if (creatingNew) "Buat Playlist Baru" else "Simpan ke Playlist",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (creatingNew) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Lagu: ${track.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArunCyan
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nama Playlist") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Deskripsi (opsional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih playlist untuk \"${track.title}\":",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (playlists.isEmpty()) {
                        Text(
                            text = "Belum ada playlist. Buat playlist pertama Anda sekarang.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(playlists, key = { it.id }) { pl ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, GlassBorderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectPlaylist(pl)
                                            onDismiss()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(pl.name, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                                            Text("${pl.trackIds.size} lagu", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                                        }
                                        Icon(Icons.Rounded.Add, contentDescription = null, tint = ArunCyan)
                                    }
                                }
                            }
                        }
                    }
                    TextButton(onClick = { creatingNew = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Buat Playlist Baru")
                    }
                }
            }
        },
        confirmButton = {
            if (creatingNew) {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreateNewPlaylist(newName.trim(), newDesc.trim())
                            onDismiss()
                        }
                    },
                    enabled = newName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ArunCyan, contentColor = Color(0xFF041421))
                ) {
                    Text("Simpan")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun AlbumArtImage(
    artworkUrl: String,
    fallbackRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (artworkUrl.isBlank()) {
        Image(
            painter = painterResource(id = fallbackRes),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        AsyncImage(
            model = artworkUrl,
            contentDescription = contentDescription,
            placeholder = painterResource(id = fallbackRes),
            error = painterResource(id = fallbackRes),
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@Composable
fun SpotifyImportDialog(
    importState: SpotifyImportUiState,
    onImportUrl: (url: String, saveAsPlaylist: Boolean) -> Unit,
    onImportFile: (uri: android.net.Uri, saveAsPlaylist: Boolean) -> Unit,
    onResetState: () -> Unit,
    onDismiss: () -> Unit
) {
    var modeUrl by remember { mutableStateOf(true) }
    var urlInput by remember { mutableStateOf("") }
    var saveAsPlaylist by remember { mutableStateOf(true) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportFile(uri, saveAsPlaylist)
        }
    }

    val isImporting = importState is SpotifyImportUiState.Importing

    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        modifier = Modifier.testTag("spotify_import_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(SpotifyGreen)
                )
                Text("Impor dari Spotify & YT Music", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (modeUrl) SpotifyGreen.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, if (modeUrl) SpotifyGreen else GlassBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable(enabled = !isImporting) {
                                modeUrl = true
                                onResetState()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Link, contentDescription = null, tint = if (modeUrl) SpotifyGreen else TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "Tautan",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (modeUrl) SpotifyGreen else TextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (!modeUrl) SpotifyGreen.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, if (!modeUrl) SpotifyGreen else GlassBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable(enabled = !isImporting) {
                                modeUrl = false
                                onResetState()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = if (!modeUrl) SpotifyGreen else TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "File Ekspor",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (!modeUrl) SpotifyGreen else TextSecondary
                            )
                        }
                    }
                }

                if (modeUrl) {
                    Text(
                        text = "Tempel tautan lagu, album, atau playlist publik dari Spotify (open.spotify.com/...) maupun YouTube Music (watch?v=..., youtu.be/..., /shorts/..., playlist?list=...).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        enabled = !isImporting,
                        label = { Text("Tautan Spotify / YouTube Music") },
                        placeholder = { Text("https://music.youtube.com/watch?v=...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spotify_url_input")
                    )
                } else {
                    Text(
                        text = "Pilih file ekspor Spotify Anda: YourLibrary.json, Playlist1.json, atau CSV dari Exportify.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/json",
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        enabled = !isImporting,
                        border = BorderStroke(1.dp, SpotifyGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spotify_pick_file_button")
                    ) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pilih File .json / .csv", color = SpotifyGreen, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isImporting) { saveAsPlaylist = !saveAsPlaylist }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = saveAsPlaylist,
                        enabled = !isImporting,
                        onCheckedChange = { saveAsPlaylist = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Simpan juga sebagai Playlist baru",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }

                when (importState) {
                    is SpotifyImportUiState.Importing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = SpotifyGreen,
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = "Mengambil metadata lagu dari Spotify...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SpotifyGreen
                            )
                        }
                    }
                    is SpotifyImportUiState.Success -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ArunEmerald.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, ArunEmerald.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ArunEmerald)
                                Text(
                                    text = "Berhasil mengimpor ${importState.trackCount} lagu (${importState.playlistName}).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ArunEmerald
                                )
                            }
                        }
                    }
                    is SpotifyImportUiState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ArunRose.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, ArunRose.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = ArunRose)
                                Text(
                                    text = importState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ArunRose
                                )
                            }
                        }
                    }
                    SpotifyImportUiState.Idle -> Unit
                }
            }
        },
        confirmButton = {
            if (modeUrl) {
                Button(
                    onClick = { onImportUrl(urlInput, saveAsPlaylist) },
                    enabled = urlInput.isNotBlank() && !isImporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = Color(0xFF041421)
                    ),
                    modifier = Modifier.testTag("spotify_import_submit_button")
                ) {
                    Text(if (isImporting) "Mengimpor..." else "Impor Lagu", fontWeight = FontWeight.Bold)
                }
            } else if (importState is SpotifyImportUiState.Success) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = Color(0xFF041421)
                    )
                ) {
                    Text("Selesai", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting
            ) {
                Text(if (importState is SpotifyImportUiState.Success) "Tutup" else "Batal")
            }
        }
    )
}

@Composable
fun YouTubeSourcePickerDialog(
    state: YouTubePickerUiState,
    onSelectCandidate: (MusicTrack, YouTubeCandidate) -> Unit,
    onRetrySearch: (MusicTrack) -> Unit,
    onDismiss: () -> Unit
) {
    if (state is YouTubePickerUiState.Idle) return

    val targetTrack = when (state) {
        is YouTubePickerUiState.Loading -> state.track
        is YouTubePickerUiState.Loaded -> state.track
        is YouTubePickerUiState.Error -> state.track
        YouTubePickerUiState.Idle -> return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        modifier = Modifier.testTag("youtube_source_picker_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(YouTubeRed)
                )
                Text("Pilih Sumber Audio YouTube", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${targetTrack.title} • ${targetTrack.artist}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ArunCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                when (state) {
                    is YouTubePickerUiState.Loading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = YouTubeRed,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Mencari audio penuh di YouTube...", color = TextSecondary)
                        }
                    }

                    is YouTubePickerUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ArunRose.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, ArunRose.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = ArunRose)
                                    Text(state.message, style = MaterialTheme.typography.bodyMedium, color = ArunRose)
                                }
                            }
                            OutlinedButton(
                                onClick = { onRetrySearch(targetTrack) },
                                border = BorderStroke(1.dp, ArunCyan),
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = ArunCyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Coba Lagi", color = ArunCyan)
                            }
                        }
                    }

                    is YouTubePickerUiState.Loaded -> {
                        LazyColumn(
                            modifier = Modifier.height(280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.candidates, key = { it.videoId }) { cand ->
                                val isSelected = cand.videoId == targetTrack.youtubeVideoId
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) YouTubeRed.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) YouTubeRed.copy(alpha = 0.7f) else GlassBorderSubtle
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onSelectCandidate(targetTrack, cand) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AlbumArtImage(
                                            artworkUrl = cand.thumbnailUrl,
                                            fallbackRes = targetTrack.coverRes,
                                            contentDescription = cand.title,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cand.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isSelected) YouTubeRed else TextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = "${cand.channel} • ${ArunAudioEngine.formatDuration(cand.durationSec)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.PlayCircle,
                                            contentDescription = "Putar versi ini",
                                            tint = if (isSelected) YouTubeRed else ArunCyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    YouTubePickerUiState.Idle -> Unit
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

