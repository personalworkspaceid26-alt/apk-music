package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.data.MusicCatalog
import com.example.model.DownloadQuality
import com.example.model.EqPreset
import com.example.model.GlassIntensity
import com.example.model.LibraryTrackDoc
import com.example.model.MusicTrack
import com.example.model.PlayHistoryDoc
import com.example.model.PlatformSource
import com.example.model.StreamQuality
import com.example.model.UserProfile
import com.example.ui.components.AlbumArtImage
import com.example.ui.components.GlassCard
import com.example.ui.components.PlatformBadge
import com.example.ui.theme.ArunAmber
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
import com.example.ui.viewmodel.LayoutPreviewMode
import com.example.ui.viewmodel.RecognitionState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SongRecognitionModal(
    state: RecognitionState,
    glassIntensity: GlassIntensity,
    onStartRecognition: () -> Unit,
    onReset: () -> Unit,
    onPlayTrack: (MusicTrack) -> Unit,
    onLikeTrack: (MusicTrack) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    var permissionDeniedMsg by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedMsg = false
            onStartRecognition()
        } else {
            permissionDeniedMsg = true
        }
    }

    fun triggerRecognize() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            permissionDeniedMsg = false
            onStartRecognition()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.96f))
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    onReset()
                    onClose()
                }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Tutup", tint = TextPrimary)
                }
                Text("Kenali Lagu", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.height(32.dp))

            val targetScale = when (state) {
                is RecognitionState.Listening -> 1f + state.amplitude * 0.45f
                else -> 1f
            }
            val waveScale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "shazamWave"
            )

            Box(
                modifier = Modifier.size(250.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseR = size.minDimension * 0.28f
                    for (ring in 1..3) {
                        val radius = baseR * (1f + ring * 0.26f) * waveScale
                        drawCircle(
                            color = ArunCyan.copy(alpha = (0.35f / ring).coerceIn(0.06f, 0.35f)),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (state is RecognitionState.Listening) ArunCyan else Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.5.dp, ArunCyan),
                    modifier = Modifier
                        .size(124.dp)
                        .clip(CircleShape)
                        .clickable { triggerRecognize() }
                        .testTag("recognize_listen_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state is RecognitionState.Listening) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                            contentDescription = "Mulai kenali lagu",
                            tint = if (state is RecognitionState.Listening) Color(0xFF041421) else ArunCyan,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            when (state) {
                is RecognitionState.Idle -> {
                    Text(
                        text = "Ketuk mikrofon untuk mendeteksi musik di sekitar Anda",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Analisis spektrum akustik real-time lintas katalog Spotify & YouTube Music.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                is RecognitionState.Listening -> {
                    Text(
                        text = "Mendengarkan & mencocokkan sidik suara...",
                        style = MaterialTheme.typography.titleMedium,
                        color = ArunCyan,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Energi sinyal: ${(state.amplitude * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                is RecognitionState.Matched -> {
                    val matched = state.track
                    GlassCard(
                        glassIntensity = glassIntensity,
                        borderColor = matched.accentColor.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ArunEmerald)
                                Text(
                                    text = "Lagu Teridentifikasi (${state.confidencePercent}% cocok)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ArunEmerald
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AlbumArtImage(
                                    artworkUrl = matched.artworkUrl,
                                    fallbackRes = matched.coverRes,
                                    contentDescription = matched.title,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(matched.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                    Text(matched.artist, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    Spacer(Modifier.height(6.dp))
                                    PlatformBadge(platform = matched.platform)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onPlayTrack(matched) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = matched.accentColor,
                                        contentColor = Color(0xFF041421)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Putar Sekarang", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onLikeTrack(matched) },
                                    border = BorderStroke(1.dp, GlassBorderSpecular)
                                ) {
                                    Icon(Icons.Rounded.Favorite, contentDescription = "Sukai", tint = ArunRose, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { triggerRecognize() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Pindai Ulang")
                            }
                        }
                    }
                }
            }

            if (permissionDeniedMsg) {
                Spacer(Modifier.height(16.dp))
                GlassCard(glassIntensity = glassIntensity, borderColor = ArunAmber.copy(alpha = 0.55f)) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Izin mikrofon diperlukan untuk mendengarkan audio sekitar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ArunAmber,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                            Text("Izinkan Mikrofon")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EqualizerModal(
    activePreset: EqPreset,
    bands: List<Int>,
    glassIntensity: GlassIntensity,
    onSelectPreset: (EqPreset) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val freqLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "9 kHz")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.96f))
            .safeDrawingPadding()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Tutup", tint = TextPrimary)
                }
                Text("Ekualizer 5-Band", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = ArunCyan, modifier = Modifier.padding(end = 12.dp))
            }

            Text(
                text = "Preset DSP Real-Time",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EqPreset.entries) { preset ->
                    val selected = activePreset == preset
                    Surface(
                        shape = CircleShape,
                        color = if (selected) ArunCyan.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, if (selected) ArunCyan else GlassBorderSubtle),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onSelectPreset(preset) }
                    ) {
                        Text(
                            text = preset.labelId,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) ArunCyan else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            GlassCard(glassIntensity = glassIntensity, borderColor = GlassBorderSpecular) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    freqLabels.forEachIndexed { idx, freq ->
                        val gain = bands.getOrElse(idx) { 0 }
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(freq, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = if (gain > 0) "+$gain dB" else "$gain dB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (gain != 0) ArunCyan else TextMuted
                                )
                            }
                            Slider(
                                value = gain.toFloat(),
                                onValueChange = { onBandChange(idx, it.toInt()) },
                                valueRange = -12f..12f,
                                steps = 23,
                                colors = SliderDefaults.colors(
                                    thumbColor = ArunCyan,
                                    activeTrackColor = ArunCyan,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.14f)
                                )
                            )
                        }
                    }
                }
            }

            Text(
                text = "Perubahan ekualizer langsung diterapkan pada pemrosesan audio DSP dan tersimpan ke akun Anda.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
    }
}

@Composable
fun ActivityAndRecapModal(
    allTracks: List<MusicTrack>,
    playHistory: List<PlayHistoryDoc>,
    libraryTracks: Map<String, LibraryTrackDoc>,
    glassIntensity: GlassIntensity,
    onPlayTrack: (MusicTrack) -> Unit,
    onClearHistory: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val totalMinutes = remember(playHistory) {
        (playHistory.sumOf { it.listenedSeconds } / 60L).coerceAtLeast(if (playHistory.isNotEmpty()) 1L else 0L)
    }
    val topArtists = remember(playHistory) {
        playHistory.groupingBy { it.artistName }.eachCount().entries.sortedByDescending { it.value }.take(4)
    }
    val topGenres = remember(playHistory) {
        playHistory.groupingBy { it.genre }.eachCount().entries.sortedByDescending { it.value }.take(4)
    }
    val spotifyCount = remember(playHistory) { playHistory.count { it.platformEnum() == PlatformSource.SPOTIFY } }
    val ytCount = remember(playHistory) { playHistory.count { it.platformEnum() == PlatformSource.YOUTUBE_MUSIC } }
    val dateFormatter = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.forLanguageTag("id-ID")) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.96f))
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.Center),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Tutup", tint = TextPrimary)
                    }
                    Text("Riwayat & Recap", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    if (playHistory.isNotEmpty()) {
                        IconButton(onClick = onClearHistory, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Hapus riwayat", tint = ArunRose)
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }
                }
            }

            // Recap Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(
                        glassIntensity = glassIntensity,
                        borderColor = ArunCyan.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = ArunCyan)
                            Spacer(Modifier.height(8.dp))
                            Text("$totalMinutes mnt", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                            Text("Waktu Dengar", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                    }
                    GlassCard(
                        glassIntensity = glassIntensity,
                        borderColor = ArunIndigo.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = ArunIndigo)
                            Spacer(Modifier.height(8.dp))
                            Text("${playHistory.size}x", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                            Text("Total Pemutaran", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        }
                    }
                }
            }

            // Top Genres Bar Chart (Canvas)
            item {
                GlassCard(glassIntensity = glassIntensity, borderColor = GlassBorderSpecular) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Top Genre & Platform", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        if (topGenres.isEmpty()) {
                            Text("Putar beberapa lagu untuk melihat grafik Recap Anda.", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                        } else {
                            val maxVal = topGenres.maxOf { it.value }.coerceAtLeast(1)
                            val barColors = listOf(ArunCyan, ArunIndigo, ArunEmerald, ArunAmber)
                            topGenres.forEachIndexed { idx, (genre, count) ->
                                val barColor = barColors[idx % barColors.size]
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(genre, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                        Text("$count putar", style = MaterialTheme.typography.labelSmall, color = barColor)
                                    }
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                    ) {
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.1f),
                                            cornerRadius = CornerRadius(12f, 12f),
                                            size = size
                                        )
                                        val w = size.width * (count.toFloat() / maxVal.toFloat())
                                        drawRoundRect(
                                            color = barColor,
                                            cornerRadius = CornerRadius(12f, 12f),
                                            size = Size(w.coerceAtLeast(16f), size.height)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = GlassBorderSubtle)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Spotify: $spotifyCount putar", style = MaterialTheme.typography.labelLarge, color = SpotifyGreen)
                                Text("YouTube Music: $ytCount putar", style = MaterialTheme.typography.labelLarge, color = YouTubeRed)
                            }
                        }
                    }
                }
            }

            // Top Artists
            if (topArtists.isNotEmpty()) {
                item {
                    GlassCard(glassIntensity = glassIntensity, borderColor = GlassBorderSubtle) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Artis Teratas", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            topArtists.forEachIndexed { index, (artist, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${index + 1}. $artist", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                    Text("$count kali", style = MaterialTheme.typography.labelMedium, color = ArunCyan)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Log Kronologis Pemutaran", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }

            if (playHistory.isEmpty()) {
                item {
                    GlassCard(glassIntensity = glassIntensity) {
                        Text(
                            text = "Belum ada log pemutaran.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                items(playHistory, key = { it.id }) { entry ->
                    val catalogTrack = allTracks.firstOrNull { it.id == entry.trackId } ?: MusicCatalog.findTrack(entry.trackId)
                    GlassCard(
                        glassIntensity = glassIntensity,
                        borderColor = GlassBorderSubtle,
                        onClick = { if (catalogTrack != null) onPlayTrack(catalogTrack) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.trackTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(entry.artistName, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PlatformBadge(platform = entry.platformEnum())
                                    val timeStr = entry.playedAt?.toDate()?.let { dateFormatter.format(it) } ?: "Baru saja"
                                    Text(timeStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }
                            if (catalogTrack != null) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Putar ulang", tint = ArunCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsAndAboutModal(
    profile: UserProfile?,
    offlineCacheBytes: Long,
    layoutPreviewMode: LayoutPreviewMode,
    onChangeLayoutMode: (LayoutPreviewMode) -> Unit,
    onUpdateStreamQuality: (StreamQuality) -> Unit,
    onUpdateDownloadQuality: (DownloadQuality) -> Unit,
    onUpdateGlassIntensity: (GlassIntensity) -> Unit,
    onClearCache: () -> Unit,
    onSignOut: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val streamQ = profile?.streamQualityEnum() ?: StreamQuality.HIGH
    val downQ = profile?.downloadQualityEnum() ?: DownloadQuality.HIGH
    val glassI = profile?.glassIntensityEnum() ?: GlassIntensity.BALANCED
    var showAboutDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.96f))
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Tutup", tint = TextPrimary)
                }
                Text("Pengaturan & Sistem", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Icon(Icons.Rounded.Settings, contentDescription = null, tint = ArunCyan, modifier = Modifier.padding(end = 12.dp))
            }

            // Streaming Quality
            GlassCard(glassIntensity = glassI, borderColor = GlassBorderSpecular) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kualitas Audio Streaming", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(StreamQuality.entries) { q ->
                            val sel = q == streamQ
                            QualityChip("${q.labelId} (${q.bitrateLabel})", sel) { onUpdateStreamQuality(q) }
                        }
                    }
                    HorizontalDivider(color = GlassBorderSubtle)
                    Text("Kualitas Audio Unduhan", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DownloadQuality.entries) { q ->
                            val sel = q == downQ
                            QualityChip("${q.labelId} (${q.bitrateLabel})", sel) { onUpdateDownloadQuality(q) }
                        }
                    }
                }
            }

            // Glass Theme Intensity
            GlassCard(glassIntensity = glassI, borderColor = GlassBorderSubtle) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Intensitas Tema Liquid Glass", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassIntensity.entries.forEach { g ->
                            val sel = g == glassI
                            QualityChip(g.labelId, sel) { onUpdateGlassIntensity(g) }
                        }
                    }
                    HorizontalDivider(color = GlassBorderSubtle)
                    Text("Tampilan Preview Tata Letak", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LayoutPreviewMode.entries.forEach { mode ->
                            val sel = mode == layoutPreviewMode
                            QualityChip(mode.label, sel) { onChangeLayoutMode(mode) }
                        }
                    }
                }
            }

            // Cache Management
            GlassCard(glassIntensity = glassI, borderColor = GlassBorderSubtle) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manajemen Cache Audio", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Terpakai: ${formatBytes(offlineCacheBytes)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    OutlinedButton(
                        onClick = onClearCache,
                        border = BorderStroke(1.dp, ArunAmber.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = ArunAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bersihkan", color = ArunAmber)
                    }
                }
            }

            // About & Sign Out
            GlassCard(glassIntensity = glassI, borderColor = GlassBorderSubtle) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showAboutDialog = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = ArunCyan)
                            Column {
                                Text("Tentang Arun Music", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("Versi ${BuildConfig.VERSION_NAME} • Build 2026", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                            }
                        }
                        Text("Detail", style = MaterialTheme.typography.labelLarge, color = ArunCyan)
                    }

                    HorizontalDivider(color = GlassBorderSubtle)

                    Button(
                        onClick = onSignOut,
                        colors = ButtonDefaults.buttonColors(containerColor = ArunRose.copy(alpha = 0.2f), contentColor = ArunRose),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Keluar dari Akun Google", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Mandatory Creator Credit & Copyright Footer at the very bottom of Settings
            GlassCard(
                glassIntensity = glassI,
                borderColor = GlassBorderSpecular.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_creator_footer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Dibuat dengan presisi oleh @r.fansy_",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/r.fansy_"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                        },
                        border = BorderStroke(1.dp, ArunCyan.copy(alpha = 0.65f)),
                        shape = CircleShape,
                        modifier = Modifier.testTag("instagram_credit_button")
                    ) {
                        Text("Instagram @r.fansy_", color = ArunCyan, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Buka Instagram @r.fansy_",
                            tint = ArunCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "© 2026 Arun Music by @r.fansy_. All Rights Reserved 2026.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Arun Music v${BuildConfig.VERSION_NAME}") },
            text = {
                Text(
                    "Arun Music adalah pemutar musik modern bertema Liquid Glass dengan Dynamic Island interaktif, " +
                        "ekualizer DSP 5-band real-time, pengenalan lagu akustik, manajemen cache offline, dan sinkronisasi " +
                        "cloud real-time melalui Firebase Firestore.\n\nDikembangkan oleh @r.fansy_."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun QualityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) ArunCyan.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (selected) ArunCyan else GlassBorderSubtle),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) ArunCyan else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
