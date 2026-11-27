package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.PlatformSource
import com.example.ui.components.GlassCard
import com.example.ui.components.PlatformBadge
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.ArunCyan
import com.example.ui.theme.ArunRose
import com.example.ui.theme.GlassBorderSpecular
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YouTubeRed
import com.example.ui.viewmodel.LiveYtSearchUiState

@Composable
fun SearchScreen(
    allTracks: List<MusicTrack>,
    searchQuery: String,
    platformFilter: PlatformSource?,
    genreFilter: String?,
    libraryTracks: Map<String, LibraryTrackDoc>,
    currentTrack: MusicTrack?,
    isPlaying: Boolean,
    glassIntensity: GlassIntensity,
    liveYtSearchState: LiveYtSearchUiState,
    isImportingLink: Boolean,
    onQueryChange: (String) -> Unit,
    onPlatformFilterChange: (PlatformSource?) -> Unit,
    onGenreFilterChange: (String?) -> Unit,
    onImportOrPlayLink: (String) -> Unit,
    onSearchYouTubeMusic: (String) -> Unit,
    onSaveLiveTrack: (MusicTrack) -> Unit,
    onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit,
    onToggleLike: (MusicTrack, Boolean) -> Unit,
    onToggleDownload: (MusicTrack, Boolean) -> Unit,
    onAddToPlaylist: (MusicTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    var quickYtLink by remember { mutableStateOf("") }
    val genres = remember(allTracks) {
        allTracks.map { it.genre }.distinct()
    }

    val filteredTracks = remember(searchQuery, platformFilter, genreFilter, allTracks) {
        val q = searchQuery.trim().lowercase()
        allTracks.filter { track ->
            val matchPlatform = platformFilter == null || track.platform == platformFilter
            val matchGenre = genreFilter == null || track.genre.equals(genreFilter, ignoreCase = true)
            val matchQuery = q.isEmpty() ||
                track.title.lowercase().contains(q) ||
                track.artist.lowercase().contains(q) ||
                track.album.lowercase().contains(q) ||
                track.genre.lowercase().contains(q)
            matchPlatform && matchGenre && matchQuery
        }
    }

    val matchingArtists = remember(filteredTracks) {
        filteredTracks.map { it.artist }.distinct()
    }

    val matchingAlbums = remember(searchQuery, platformFilter) {
        val q = searchQuery.trim().lowercase()
        MusicCatalog.albums.filter { alb ->
            val matchPlat = platformFilter == null || alb.platform == platformFilter
            val matchQ = q.isEmpty() || alb.title.lowercase().contains(q) || alb.artist.lowercase().contains(q)
            matchPlat && matchQ
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen"),
        contentPadding = PaddingValues(top = 80.dp, bottom = 180.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Pencarian",
                style = MaterialTheme.typography.displaySmall,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Katalog instan terhubung Spotify & YouTube Music",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Instant Real-time Search Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Cari lagu, artis, album, atau genre...", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = "Cari", tint = ArunCyan)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Hapus pencarian", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = glassIntensity.surfaceAlpha),
                    unfocusedContainerColor = Color.White.copy(alpha = glassIntensity.surfaceAlpha * 0.8f),
                    focusedBorderColor = ArunCyan,
                    unfocusedBorderColor = GlassBorderSpecular,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input")
            )
        }

        // Quick YouTube Music / Spotify Link Paste Bar
        item {
            GlassCard(
                glassIntensity = glassIntensity,
                borderColor = YouTubeRed.copy(alpha = 0.42f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Link, contentDescription = null, tint = YouTubeRed, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Putar / Impor Tautan YouTube Music",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quickYtLink,
                            onValueChange = { quickYtLink = it },
                            enabled = !isImportingLink,
                            placeholder = { Text("music.youtube.com/watch?v=... atau playlist?list=...", color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_yt_link_input")
                        )
                        Button(
                            onClick = {
                                onImportOrPlayLink(quickYtLink)
                                quickYtLink = ""
                            },
                            enabled = quickYtLink.isNotBlank() && !isImportingLink,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = YouTubeRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("search_yt_link_submit")
                        ) {
                            if (isImportingLink) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Putar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Platform Source Filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterGlassChip(
                        label = "Semua Platform",
                        selected = platformFilter == null,
                        accent = ArunCyan,
                        onClick = { onPlatformFilterChange(null) }
                    )
                }
                item {
                    FilterGlassChip(
                        label = "Spotify",
                        selected = platformFilter == PlatformSource.SPOTIFY,
                        accent = SpotifyGreen,
                        onClick = {
                            onPlatformFilterChange(
                                if (platformFilter == PlatformSource.SPOTIFY) null else PlatformSource.SPOTIFY
                            )
                        }
                    )
                }
                item {
                    FilterGlassChip(
                        label = "YouTube Music",
                        selected = platformFilter == PlatformSource.YOUTUBE_MUSIC,
                        accent = YouTubeRed,
                        onClick = {
                            onPlatformFilterChange(
                                if (platformFilter == PlatformSource.YOUTUBE_MUSIC) null else PlatformSource.YOUTUBE_MUSIC
                            )
                        }
                    )
                }
            }
        }

        // Genre Filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterGlassChip(
                        label = "Semua Genre",
                        selected = genreFilter == null,
                        accent = ArunCyan,
                        onClick = { onGenreFilterChange(null) }
                    )
                }
                items(genres) { g ->
                    FilterGlassChip(
                        label = g,
                        selected = genreFilter == g,
                        accent = ArunCyan,
                        onClick = { onGenreFilterChange(if (genreFilter == g) null else g) }
                    )
                }
            }
        }

        // Live YouTube Music Catalog Search Action & Results
        if (searchQuery.trim().length >= 2) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onSearchYouTubeMusic(searchQuery) },
                        border = BorderStroke(1.dp, YouTubeRed.copy(alpha = 0.65f)),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_live_yt_button")
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = YouTubeRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cari \"$searchQuery\" langsung di YouTube Music", color = YouTubeRed, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    when (liveYtSearchState) {
                        is LiveYtSearchUiState.Loading -> {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = YouTubeRed, strokeWidth = 2.dp)
                                Text("Mencari di katalog YouTube Music...", color = TextSecondary)
                            }
                        }
                        is LiveYtSearchUiState.Error -> {
                            Text(liveYtSearchState.message, color = ArunRose, style = MaterialTheme.typography.bodyMedium)
                        }
                        is LiveYtSearchUiState.Loaded -> {
                            Text(
                                text = "Hasil YouTube Music (${liveYtSearchState.tracks.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                        LiveYtSearchUiState.Idle -> Unit
                    }
                }
            }

            if (liveYtSearchState is LiveYtSearchUiState.Loaded) {
                items(liveYtSearchState.tracks, key = { "live_${it.id}" }) { track ->
                    val lib = libraryTracks[track.id]
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        TrackRowItem(
                            track = track,
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = isPlaying && currentTrack?.id == track.id,
                            isLiked = lib?.liked == true,
                            isDownloaded = lib?.downloaded == true,
                            glassIntensity = glassIntensity,
                            onPlayClick = { onPlayTrack(track, liveYtSearchState.tracks) },
                            onLikeClick = { onToggleLike(track, lib?.liked == true) },
                            onDownloadClick = { onToggleDownload(track, lib?.downloaded == true) },
                            onAddToPlaylistClick = { onAddToPlaylist(track) }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { onSaveLiveTrack(track) },
                                border = BorderStroke(1.dp, GlassBorderSubtle),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, tint = ArunCyan, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Simpan ke Pustaka", style = MaterialTheme.typography.labelMedium, color = ArunCyan)
                            }
                        }
                    }
                }
            }
        }

        // Matching Artists
        if (matchingArtists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Artis",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(matchingArtists) { artist ->
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = glassIntensity.surfaceAlpha),
                                border = BorderStroke(1.dp, GlassBorderSubtle),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { onQueryChange(artist) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = ArunCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = artist,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Matching Albums
        if (matchingAlbums.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Album",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(matchingAlbums, key = { it.id }) { alb ->
                            GlassCard(
                                glassIntensity = glassIntensity,
                                borderColor = alb.accentColor.copy(alpha = 0.45f),
                                onClick = {
                                    val tracks = alb.trackIds.mapNotNull { id ->
                                        allTracks.firstOrNull { it.id == id } ?: MusicCatalog.findTrack(id)
                                    }
                                    if (tracks.isNotEmpty()) onPlayTrack(tracks.first(), tracks)
                                },
                                modifier = Modifier.width(210.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = alb.coverRes),
                                        contentDescription = alb.title,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = alb.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = alb.artist,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        PlatformBadge(platform = alb.platform)
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Putar album",
                                        tint = alb.accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Track Results Header
        item {
            Text(
                text = "Lagu (${filteredTracks.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        if (filteredTracks.isEmpty()) {
            item {
                GlassCard(glassIntensity = glassIntensity) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada lagu yang cocok dengan pencarian \"$searchQuery\".",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredTracks, key = { it.id }) { track ->
                val lib = libraryTracks[track.id]
                TrackRowItem(
                    track = track,
                    isCurrent = currentTrack?.id == track.id,
                    isPlaying = isPlaying && currentTrack?.id == track.id,
                    isLiked = lib?.liked == true,
                    isDownloaded = lib?.downloaded == true,
                    glassIntensity = glassIntensity,
                    onPlayClick = { onPlayTrack(track, filteredTracks) },
                    onLikeClick = { onToggleLike(track, lib?.liked == true) },
                    onDownloadClick = { onToggleDownload(track, lib?.downloaded == true) },
                    onAddToPlaylistClick = { onAddToPlaylist(track) }
                )
            }
        }
    }
}

@Composable
private fun FilterGlassChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (selected) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (selected) accent else GlassBorderSubtle),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) accent else TextSecondary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
