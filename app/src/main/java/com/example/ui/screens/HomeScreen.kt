package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MusicCatalog
import com.example.model.AlbumCollection
import com.example.model.GlassIntensity
import com.example.model.LibraryTrackDoc
import com.example.model.MusicTrack
import com.example.model.PlayHistoryDoc
import com.example.model.PlaylistDoc
import com.example.model.UserProfile
import com.example.ui.components.AlbumArtImage
import com.example.ui.components.GlassCard
import com.example.ui.components.PlatformBadge
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.ArunCyan
import com.example.ui.theme.ArunEmerald
import com.example.ui.theme.GlassBorderSpecular
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LayoutPreviewMode
import com.example.ui.viewmodel.SyncStateSnapshot

@Composable
fun HomeScreen(
    profile: UserProfile?,
    allTracks: List<MusicTrack>,
    playlists: List<PlaylistDoc>,
    playHistory: List<PlayHistoryDoc>,
    libraryTracks: Map<String, LibraryTrackDoc>,
    currentTrack: MusicTrack?,
    isPlaying: Boolean,
    glassIntensity: GlassIntensity,
    syncState: SyncStateSnapshot,
    layoutPreviewMode: LayoutPreviewMode,
    onCycleLayoutMode: () -> Unit,
    onSyncNow: () -> Unit,
    onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit,
    onToggleLike: (MusicTrack, Boolean) -> Unit,
    onToggleDownload: (MusicTrack, Boolean) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onOpenPlaylistDetail: (PlaylistDoc) -> Unit,
    onOpenSpotifyImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val importedTracks = remember(allTracks) {
        allTracks.filter { it.isImported }
    }

    val recentlyPlayedTracks = remember(playHistory, allTracks) {
        val seen = LinkedHashSet<String>()
        val list = mutableListOf<MusicTrack>()
        for (entry in playHistory) {
            if (seen.add(entry.trackId)) {
                val found = allTracks.firstOrNull { it.id == entry.trackId } ?: MusicCatalog.findTrack(entry.trackId)
                found?.let { list.add(it) }
            }
            if (list.size >= 8) break
        }
        list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(top = 80.dp, bottom = 180.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Welcome Header + Real-time Cloud Sync Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Beranda",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Halo, ${profile?.displayName ?: "Pendengar Arun"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, GlassBorderSubtle),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onCycleLayoutMode)
                            .testTag("home_layout_mode_toggle")
                    ) {
                        Text(
                            text = layoutPreviewMode.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (profile?.autoSync == true) ArunEmerald.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.1f),
                        border = BorderStroke(
                            1.dp,
                            if (profile?.autoSync == true) ArunEmerald.copy(alpha = 0.45f) else GlassBorderSubtle
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onSyncNow)
                            .testTag("home_sync_now_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = "Sinkronkan sekarang",
                                tint = if (profile?.autoSync == true) ArunEmerald else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (syncState.isSyncing) "Menyinkronkan..." else "Rev #${syncState.revision}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (profile?.autoSync == true) ArunEmerald else TextMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Quick Spotify Import Action Banner
        item {
            GlassCard(
                glassIntensity = glassIntensity,
                borderColor = SpotifyGreen.copy(alpha = 0.45f),
                onClick = onOpenSpotifyImport
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SpotifyGreen.copy(alpha = 0.18f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.LibraryAdd, contentDescription = null, tint = SpotifyGreen)
                            }
                        }
                        Column {
                            Text(
                                text = "Impor Spotify & YT Music",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = if (importedTracks.isEmpty()) {
                                    "Tempel link lagu/playlist Spotify atau YouTube Music"
                                } else {
                                    "${importedTracks.size} lagu impor siap putar penuh"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onOpenSpotifyImport,
                        border = BorderStroke(1.dp, SpotifyGreen),
                        shape = CircleShape,
                        modifier = Modifier.testTag("home_spotify_import_button")
                    ) {
                        Text("Impor", color = SpotifyGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Section 1: Baru Saja Diputar (Recently Played - Real-time from Firestore)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = ArunCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Baru Saja Diputar",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                }

                if (recentlyPlayedTracks.isEmpty()) {
                    GlassCard(glassIntensity = glassIntensity) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Belum ada riwayat putar",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Putar lagu pertama Anda dan progres akan langsung tersinkronisasi.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                shape = CircleShape,
                                color = ArunCyan,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val queue = allTracks.ifEmpty { MusicCatalog.tracks }
                                        onPlayTrack(queue.first(), queue)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Putar lagu pembuka",
                                        tint = Color(0xFF041421)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recentlyPlayedTracks, key = { it.id }) { track ->
                            FloatingGlassTrackCard(
                                track = track,
                                glassIntensity = glassIntensity,
                                onPlay = { onPlayTrack(track, recentlyPlayedTracks) }
                            )
                        }
                    }
                }
            }
        }

        // Section 1B: Koleksi Impor Spotify
        if (importedTracks.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Lagu & Playlist Impor (${importedTracks.size})",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(importedTracks, key = { "imp_${it.id}" }) { track ->
                            FloatingGlassTrackCard(
                                track = track,
                                glassIntensity = glassIntensity,
                                onPlay = { onPlayTrack(track, importedTracks) }
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Playlist Kamu (Real-time synced playlists)
        if (playlists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Playlist Kamu",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(playlists, key = { it.id }) { pl ->
                            FloatingPlaylistCard(
                                playlist = pl,
                                glassIntensity = glassIntensity,
                                onPlayPlaylist = {
                                    val tracks = pl.trackIds.mapNotNull { id ->
                                        allTracks.firstOrNull { it.id == id } ?: MusicCatalog.findTrack(id)
                                    }.ifEmpty { allTracks.take(3).ifEmpty { MusicCatalog.tracks.take(3) } }
                                    onPlayTrack(tracks.first(), tracks)
                                },
                                onOpenPlaylist = { onOpenPlaylistDetail(pl) }
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Album Pilihan (Floating Glass Album Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = ArunCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Album Pilihan Liquid Glass",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(MusicCatalog.albums, key = { it.id }) { album ->
                        FloatingAlbumCard(
                            album = album,
                            glassIntensity = glassIntensity,
                            onPlayAlbum = {
                                val tracks = album.trackIds.mapNotNull { id ->
                                    allTracks.firstOrNull { it.id == id } ?: MusicCatalog.findTrack(id)
                                }
                                if (tracks.isNotEmpty()) onPlayTrack(tracks.first(), tracks)
                            }
                        )
                    }
                }
            }
        }

        // Section 4: Semua Rilisan Terkini
        item {
            Text(
                text = "Jelajah Lagu Terkini",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
        }

        items(allTracks, key = { it.id }) { track ->
            val lib = libraryTracks[track.id]
            TrackRowItem(
                track = track,
                isCurrent = currentTrack?.id == track.id,
                isPlaying = isPlaying && currentTrack?.id == track.id,
                isLiked = lib?.liked == true,
                isDownloaded = lib?.downloaded == true,
                glassIntensity = glassIntensity,
                onPlayClick = { onPlayTrack(track, allTracks) },
                onLikeClick = { onToggleLike(track, lib?.liked == true) },
                onDownloadClick = { onToggleDownload(track, lib?.downloaded == true) },
                onAddToPlaylistClick = { onAddToPlaylist(track) }
            )
        }
    }
}

@Composable
private fun FloatingGlassTrackCard(
    track: MusicTrack,
    glassIntensity: GlassIntensity,
    onPlay: () -> Unit
) {
    GlassCard(
        glassIntensity = glassIntensity,
        borderColor = track.accentColor.copy(alpha = 0.45f),
        onClick = onPlay,
        modifier = Modifier.width(164.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.size(140.dp)) {
                AlbumArtImage(
                    artworkUrl = track.artworkUrl,
                    fallbackRes = track.coverRes,
                    contentDescription = "Sampul ${track.title}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    shape = CircleShape,
                    color = track.accentColor,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlay)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Putar ${track.title}",
                            tint = Color(0xFF041421)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            PlatformBadge(platform = track.platform)
        }
    }
}

@Composable
private fun FloatingPlaylistCard(
    playlist: PlaylistDoc,
    glassIntensity: GlassIntensity,
    onPlayPlaylist: () -> Unit,
    onOpenPlaylist: () -> Unit
) {
    GlassCard(
        glassIntensity = glassIntensity,
        borderColor = GlassBorderSpecular,
        onClick = onOpenPlaylist,
        modifier = Modifier.width(188.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Image(
                    painter = painterResource(id = MusicCatalog.coverResForKey(playlist.coverKey)),
                    contentDescription = "Sampul playlist ${playlist.name}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    shape = CircleShape,
                    color = ArunCyan,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlayPlaylist)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Putar playlist ${playlist.name}",
                            tint = Color(0xFF041421)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.trackIds.size} lagu • Real-time",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FloatingAlbumCard(
    album: AlbumCollection,
    glassIntensity: GlassIntensity,
    onPlayAlbum: () -> Unit
) {
    GlassCard(
        glassIntensity = glassIntensity,
        borderColor = album.accentColor.copy(alpha = 0.42f),
        onClick = onPlayAlbum,
        modifier = Modifier.width(176.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                Image(
                    painter = painterResource(id = album.coverRes),
                    contentDescription = "Sampul album ${album.title}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    shape = CircleShape,
                    color = album.accentColor,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Putar album ${album.title}",
                            tint = Color(0xFF041421)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = album.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${album.artist} • ${album.year}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            PlatformBadge(platform = album.platform)
        }
    }
}
