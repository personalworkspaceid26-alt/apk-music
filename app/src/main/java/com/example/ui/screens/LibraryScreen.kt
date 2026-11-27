package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MusicCatalog
import com.example.model.GlassIntensity
import com.example.model.LibraryTrackDoc
import com.example.model.MusicTrack
import com.example.model.PlaylistDoc
import com.example.ui.components.GlassCard
import com.example.ui.components.PlatformBadge
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.ArunCyan
import com.example.ui.theme.ArunEmerald
import com.example.ui.theme.ArunRose
import com.example.ui.theme.GlassBorderSpecular
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YouTubeRed

enum class LibraryTab(val label: String) {
    PLAYLIST("Playlist"),
    SPOTIFY_IMPORT("Impor Musik"),
    KOLEKSI("Koleksi"),
    DISUKAI("Disukai"),
    DIUNDUH("Diunduh")
}

@Composable
fun LibraryScreen(
    allTracks: List<MusicTrack>,
    playlists: List<PlaylistDoc>,
    libraryTracks: Map<String, LibraryTrackDoc>,
    offlineCacheBytes: Long,
    currentTrack: MusicTrack?,
    isPlaying: Boolean,
    glassIntensity: GlassIntensity,
    selectedPlaylist: PlaylistDoc?,
    onSelectPlaylist: (PlaylistDoc?) -> Unit,
    onCreatePlaylist: (name: String, desc: String, coverKey: String) -> Unit,
    onUpdatePlaylist: (PlaylistDoc, String, String, List<String>) -> Unit,
    onDeletePlaylist: (PlaylistDoc) -> Unit,
    onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit,
    onToggleLike: (MusicTrack, Boolean) -> Unit,
    onToggleSaveAlbum: (List<String>, Boolean) -> Unit,
    onToggleDownload: (MusicTrack, Boolean) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    onOpenSpotifyImport: () -> Unit,
    onImportYouTubeLink: (String) -> Unit,
    onDeleteImportedTrack: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(LibraryTab.PLAYLIST) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<PlaylistDoc?>(null) }
    var ytPlaylistLink by remember { mutableStateOf("") }

    if (selectedPlaylist != null) {
        BackHandler { onSelectPlaylist(null) }
        val latest = playlists.firstOrNull { it.id == selectedPlaylist.id } ?: selectedPlaylist
        PlaylistDetailView(
            playlist = latest,
            allTracks = allTracks,
            libraryTracks = libraryTracks,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            glassIntensity = glassIntensity,
            onBack = { onSelectPlaylist(null) },
            onEdit = { editingPlaylist = latest },
            onDelete = {
                onDeletePlaylist(latest)
                onSelectPlaylist(null)
            },
            onPlayTrack = onPlayTrack,
            onRemoveTrack = { trackId ->
                onUpdatePlaylist(
                    latest,
                    latest.name,
                    latest.description,
                    latest.trackIds.filterNot { it == trackId }
                )
            },
            onToggleLike = onToggleLike,
            onToggleDownload = onToggleDownload,
            onAddToPlaylist = onAddToPlaylist
        )
    } else {
        val importedTracks = remember(allTracks) {
            allTracks.filter { it.isImported }
        }
        val likedTracks = remember(libraryTracks, allTracks) {
            allTracks.filter { libraryTracks[it.id]?.liked == true }
        }
        val downloadedTracks = remember(libraryTracks, allTracks) {
            allTracks.filter { libraryTracks[it.id]?.downloaded == true }
        }
        val savedAlbums = remember(libraryTracks) {
            MusicCatalog.albums.filter { alb ->
                alb.trackIds.any { id -> libraryTracks[id]?.savedAlbum == true }
            }
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .testTag("library_screen"),
            contentPadding = PaddingValues(top = 80.dp, bottom = 180.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Pustaka",
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Playlist, koleksi & unduhan real-time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenSpotifyImport,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, SpotifyGreen),
                            modifier = Modifier.testTag("library_spotify_import_button")
                        ) {
                            Icon(Icons.Rounded.LibraryAdd, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Impor", color = SpotifyGreen, fontWeight = FontWeight.SemiBold)
                        }
                        if (activeTab == LibraryTab.PLAYLIST) {
                            Button(
                                onClick = { showCreateDialog = true },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ArunCyan,
                                    contentColor = Color(0xFF041421)
                                ),
                                modifier = Modifier.testTag("create_playlist_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Buat", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Liquid Glass Tab Selector
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LibraryTab.entries) { tab ->
                        val selected = activeTab == tab
                        Surface(
                            shape = CircleShape,
                            color = if (selected) ArunCyan.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, if (selected) ArunCyan else GlassBorderSubtle),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { activeTab = tab }
                                .testTag("library_tab_${tab.name.lowercase()}")
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) ArunCyan else TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            when (activeTab) {
                LibraryTab.PLAYLIST -> {
                    if (playlists.isEmpty()) {
                        item {
                            EmptyLibraryCard(
                                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                title = "Belum Ada Playlist",
                                subtitle = "Ketuk tombol Buat di atas untuk menyusun playlist pribadi Anda."
                            )
                        }
                    } else {
                        items(playlists, key = { it.id }) { pl ->
                            GlassCard(
                                glassIntensity = glassIntensity,
                                borderColor = GlassBorderSpecular,
                                onClick = { onSelectPlaylist(pl) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = MusicCatalog.coverResForKey(pl.coverKey)),
                                        contentDescription = pl.name,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(14.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pl.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = pl.description.ifBlank { "${pl.trackIds.size} lagu tersimpan" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${pl.trackIds.size} lagu",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ArunCyan
                                        )
                                    }
                                    IconButton(
                                        onClick = { editingPlaylist = pl },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit playlist", tint = TextSecondary)
                                    }
                                    IconButton(
                                        onClick = { onDeletePlaylist(pl) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Hapus playlist", tint = ArunRose)
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.SPOTIFY_IMPORT -> {
                    item {
                        GlassCard(glassIntensity = glassIntensity, borderColor = SpotifyGreen.copy(alpha = 0.45f)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${importedTracks.size} Lagu Impor (Spotify & YT Music)",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Diputar penuh via audio YouTube Music & tersinkron cloud",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    Button(
                                        onClick = onOpenSpotifyImport,
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SpotifyGreen,
                                            contentColor = Color(0xFF041421)
                                        )
                                    ) {
                                        Text("Impor", fontWeight = FontWeight.Bold)
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = ytPlaylistLink,
                                        onValueChange = { ytPlaylistLink = it },
                                        placeholder = { Text("Tautan lagu / playlist YT Music...", color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            onImportYouTubeLink(ytPlaylistLink)
                                            ytPlaylistLink = ""
                                        },
                                        enabled = ytPlaylistLink.isNotBlank(),
                                        border = BorderStroke(1.dp, YouTubeRed),
                                        shape = CircleShape
                                    ) {
                                        Text("Impor YT", color = YouTubeRed, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    if (importedTracks.isEmpty()) {
                        item {
                            EmptyLibraryCard(
                                icon = Icons.Rounded.LibraryAdd,
                                title = "Belum Ada Impor Spotify",
                                subtitle = "Ketuk tombol Impor di atas untuk mengimpor lagu atau playlist dari link Spotify maupun file .json/.csv."
                            )
                        }
                    } else {
                        items(importedTracks, key = { "lib_sp_${it.id}" }) { track ->
                            val lib = libraryTracks[track.id]
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                TrackRowItem(
                                    track = track,
                                    isCurrent = currentTrack?.id == track.id,
                                    isPlaying = isPlaying && currentTrack?.id == track.id,
                                    isLiked = lib?.liked == true,
                                    isDownloaded = lib?.downloaded == true,
                                    glassIntensity = glassIntensity,
                                    onPlayClick = { onPlayTrack(track, importedTracks) },
                                    onLikeClick = { onToggleLike(track, lib?.liked == true) },
                                    onDownloadClick = { onToggleDownload(track, lib?.downloaded == true) },
                                    onAddToPlaylistClick = { onAddToPlaylist(track) }
                                )
                                TextButton(onClick = { onDeleteImportedTrack(track.id) }) {
                                    Text("Hapus dari daftar impor", color = ArunRose, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                LibraryTab.KOLEKSI -> {
                    item {
                        Text(
                            text = "Album Tersimpan (${savedAlbums.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    items(MusicCatalog.albums, key = { it.id }) { album ->
                        val isSaved = album.trackIds.any { libraryTracks[it]?.savedAlbum == true }
                        GlassCard(
                            glassIntensity = glassIntensity,
                            borderColor = if (isSaved) album.accentColor.copy(alpha = 0.55f) else GlassBorderSubtle,
                            onClick = {
                                val tracks = album.trackIds.mapNotNull { id ->
                                    allTracks.firstOrNull { it.id == id } ?: MusicCatalog.findTrack(id)
                                }
                                if (tracks.isNotEmpty()) onPlayTrack(tracks.first(), tracks)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = album.coverRes),
                                    contentDescription = album.title,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(album.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text("${album.artist} • ${album.genre}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    Spacer(Modifier.height(4.dp))
                                    PlatformBadge(platform = album.platform)
                                }
                                IconButton(
                                    onClick = { onToggleSaveAlbum(album.trackIds, isSaved) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = if (isSaved) "Hapus dari koleksi" else "Simpan ke koleksi",
                                        tint = if (isSaved) album.accentColor else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryTab.DISUKAI -> {
                    if (likedTracks.isEmpty()) {
                        item {
                            EmptyLibraryCard(
                                icon = Icons.Rounded.Favorite,
                                title = "Belum Ada Lagu Disukai",
                                subtitle = "Ketuk ikon hati pada lagu favorit Anda untuk menyinkronkannya ke sini."
                            )
                        }
                    } else {
                        items(likedTracks, key = { it.id }) { track ->
                            val lib = libraryTracks[track.id]
                            TrackRowItem(
                                track = track,
                                isCurrent = currentTrack?.id == track.id,
                                isPlaying = isPlaying && currentTrack?.id == track.id,
                                isLiked = true,
                                isDownloaded = lib?.downloaded == true,
                                glassIntensity = glassIntensity,
                                onPlayClick = { onPlayTrack(track, likedTracks) },
                                onLikeClick = { onToggleLike(track, true) },
                                onDownloadClick = { onToggleDownload(track, lib?.downloaded == true) },
                                onAddToPlaylistClick = { onAddToPlaylist(track) }
                            )
                        }
                    }
                }

                LibraryTab.DIUNDUH -> {
                    item {
                        GlassCard(glassIntensity = glassIntensity, borderColor = ArunEmerald.copy(alpha = 0.4f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Rounded.DownloadDone, contentDescription = null, tint = ArunEmerald)
                                    Column {
                                        Text("${downloadedTracks.size} Lagu Offline", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(
                                            text = "Penyimpanan lokal: ${formatBytes(offlineCacheBytes)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ArunEmerald)
                            }
                        }
                    }
                    if (downloadedTracks.isEmpty()) {
                        item {
                            EmptyLibraryCard(
                                icon = Icons.Rounded.Album,
                                title = "Belum Ada Unduhan Offline",
                                subtitle = "Ketuk ikon unduh pada lagu mana saja untuk mendengarkan tanpa koneksi."
                            )
                        }
                    } else {
                        items(downloadedTracks, key = { it.id }) { track ->
                            val lib = libraryTracks[track.id]
                            TrackRowItem(
                                track = track,
                                isCurrent = currentTrack?.id == track.id,
                                isPlaying = isPlaying && currentTrack?.id == track.id,
                                isLiked = lib?.liked == true,
                                isDownloaded = true,
                                glassIntensity = glassIntensity,
                                onPlayClick = { onPlayTrack(track, downloadedTracks) },
                                onLikeClick = { onToggleLike(track, lib?.liked == true) },
                                onDownloadClick = { onToggleDownload(track, true) },
                                onAddToPlaylistClick = { onAddToPlaylist(track) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        PlaylistEditorDialog(
            initialName = "",
            initialDesc = "",
            initialCoverKey = "midnight",
            title = "Buat Playlist Baru",
            showCoverPicker = true,
            onConfirm = { name, desc, cover ->
                onCreatePlaylist(name, desc, cover)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    val plToEdit = editingPlaylist
    if (plToEdit != null) {
        PlaylistEditorDialog(
            initialName = plToEdit.name,
            initialDesc = plToEdit.description,
            initialCoverKey = plToEdit.coverKey,
            title = "Edit Playlist",
            showCoverPicker = false,
            onConfirm = { name, desc, _ ->
                onUpdatePlaylist(plToEdit, name, desc, plToEdit.trackIds)
                editingPlaylist = null
            },
            onDismiss = { editingPlaylist = null }
        )
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: PlaylistDoc,
    allTracks: List<MusicTrack>,
    libraryTracks: Map<String, LibraryTrackDoc>,
    currentTrack: MusicTrack?,
    isPlaying: Boolean,
    glassIntensity: GlassIntensity,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit,
    onRemoveTrack: (String) -> Unit,
    onToggleLike: (MusicTrack, Boolean) -> Unit,
    onToggleDownload: (MusicTrack, Boolean) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit
) {
    val tracks = remember(playlist.trackIds, allTracks) {
        playlist.trackIds.mapNotNull { id ->
            allTracks.firstOrNull { it.id == id } ?: MusicCatalog.findTrack(id)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 80.dp, bottom = 180.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit playlist", tint = TextPrimary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Hapus playlist", tint = ArunRose)
                    }
                }
            }
        }

        item {
            GlassCard(glassIntensity = glassIntensity, borderColor = GlassBorderSpecular) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = MusicCatalog.coverResForKey(playlist.coverKey)),
                        contentDescription = playlist.name,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.name, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        if (playlist.description.isNotBlank()) {
                            Text(playlist.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("${tracks.size} lagu", style = MaterialTheme.typography.labelLarge, color = ArunCyan)
                    }
                    if (tracks.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = ArunCyan,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { onPlayTrack(tracks.first(), tracks) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Putar semua", tint = Color(0xFF041421))
                            }
                        }
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                EmptyLibraryCard(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    title = "Playlist Masih Kosong",
                    subtitle = "Tambahkan lagu dari halaman Beranda atau Pencarian dengan ikon Tambah ke Playlist."
                )
            }
        } else {
            items(tracks, key = { it.id }) { track ->
                val lib = libraryTracks[track.id]
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TrackRowItem(
                        track = track,
                        isCurrent = currentTrack?.id == track.id,
                        isPlaying = isPlaying && currentTrack?.id == track.id,
                        isLiked = lib?.liked == true,
                        isDownloaded = lib?.downloaded == true,
                        glassIntensity = glassIntensity,
                        onPlayClick = { onPlayTrack(track, tracks) },
                        onLikeClick = { onToggleLike(track, lib?.liked == true) },
                        onDownloadClick = { onToggleDownload(track, lib?.downloaded == true) },
                        onAddToPlaylistClick = { onAddToPlaylist(track) }
                    )
                    TextButton(onClick = { onRemoveTrack(track.id) }) {
                        Text("Keluarkan dari playlist", color = ArunRose, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    GlassCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = ArunCyan, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
    }
}

@Composable
private fun PlaylistEditorDialog(
    initialName: String,
    initialDesc: String,
    initialCoverKey: String,
    title: String,
    showCoverPicker: Boolean,
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var desc by remember { mutableStateOf(initialDesc) }
    var coverKey by remember { mutableStateOf(initialCoverKey) }
    val covers = listOf("midnight" to "Malam", "sunset" to "Senja", "emerald" to "Zamrud", "violet" to "Neon")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deskripsi") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showCoverPicker) {
                    Text("Tema Sampul", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        covers.forEach { (key, label) ->
                            val sel = coverKey == key
                            Surface(
                                shape = CircleShape,
                                color = if (sel) ArunCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, if (sel) ArunCyan else GlassBorderSubtle),
                                modifier = Modifier.clickable { coverKey = key }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (sel) ArunCyan else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), desc.trim(), coverKey) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ArunCyan, contentColor = Color(0xFF041421))
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

internal fun formatBytes(bytes: Long): String {
    return when {
        bytes <= 0L -> "0 KB"
        bytes < 1024L * 1024L -> "${(bytes / 1024L).coerceAtLeast(1L)} KB"
        else -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
    }
}
