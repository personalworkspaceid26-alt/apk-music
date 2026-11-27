package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AddLink
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.model.GlassIntensity
import com.example.model.PlayHistoryDoc
import com.example.model.PlatformSource
import com.example.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.theme.ArunAmber
import com.example.ui.theme.ArunCyan
import com.example.ui.theme.ArunEmerald
import com.example.ui.theme.ArunIndigo
import com.example.ui.theme.ArunRose
import com.example.ui.theme.GlassBorderSpecular
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.YouTubeRed
import com.example.ui.viewmodel.LayoutPreviewMode
import com.example.ui.viewmodel.SyncStateSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun YouHubScreen(
    profile: UserProfile?,
    playHistory: List<PlayHistoryDoc>,
    offlineCacheBytes: Long,
    glassIntensity: GlassIntensity,
    syncState: SyncStateSnapshot,
    layoutPreviewMode: LayoutPreviewMode,
    onSyncNow: () -> Unit,
    onChangeLayoutMode: (LayoutPreviewMode) -> Unit,
    onImportYouTubeLink: (String) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onUpdatePlatformConnection: (PlatformSource, Boolean, String) -> Unit,
    onToggleLinkedApp: (String, List<String>) -> Unit,
    onOpenRecognizer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenActivityRecap: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSpotifyImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var linkDialogPlatform by remember { mutableStateOf<PlatformSource?>(null) }
    var showOtherAppsDialog by remember { mutableStateOf(false) }
    var ytQuickUrl by remember { mutableStateOf("") }
    val syncTimeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("id-ID")) }

    val linkedApps = profile?.linkedApps ?: emptyList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("you_hub_screen"),
        contentPadding = PaddingValues(top = 80.dp, bottom = 180.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Profile Header
        item {
            GlassCard(glassIntensity = glassIntensity, borderColor = GlassBorderSpecular) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ArunCyan.copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, ArunCyan),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = ArunCyan, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.displayName ?: "Pendengar Arun",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Akun Google Tersinkronisasi • Cloud Real-Time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ArunEmerald
                        )
                    }
                }
            }
        }

        // 1. Pusat Koneksi Akun
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pusat Koneksi Akun", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)

                GlassCard(glassIntensity = glassIntensity, borderColor = GlassBorderSpecular) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PlatformAccountRow(
                            title = "YouTube Music",
                            connected = profile?.ytMusicConnected == true,
                            account = profile?.ytMusicAccount ?: "",
                            accent = YouTubeRed,
                            onAction = {
                                if (profile?.ytMusicConnected == true) {
                                    onUpdatePlatformConnection(PlatformSource.YOUTUBE_MUSIC, false, "")
                                } else {
                                    linkDialogPlatform = PlatformSource.YOUTUBE_MUSIC
                                }
                            }
                        )

                        HorizontalDivider(color = GlassBorderSubtle)

                        PlatformAccountRow(
                            title = "Spotify",
                            connected = profile?.spotifyConnected == true,
                            account = profile?.spotifyAccount ?: "",
                            accent = SpotifyGreen,
                            onAction = {
                                if (profile?.spotifyConnected == true) {
                                    onUpdatePlatformConnection(PlatformSource.SPOTIFY, false, "")
                                } else {
                                    linkDialogPlatform = PlatformSource.SPOTIFY
                                }
                            }
                        )

                        OutlinedButton(
                            onClick = onOpenSpotifyImport,
                            border = BorderStroke(1.dp, SpotifyGreen.copy(alpha = 0.65f)),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hub_spotify_import_button")
                        ) {
                            Icon(Icons.Rounded.LibraryAdd, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Impor dari Spotify / File Ekspor", color = SpotifyGreen, fontWeight = FontWeight.SemiBold)
                        }

                        HorizontalDivider(color = GlassBorderSubtle)

                        // Quick YouTube Music Link & Playlist Sync
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Tautan & Playlist YouTube Music",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = ytQuickUrl,
                                    onValueChange = { ytQuickUrl = it },
                                    placeholder = { Text("music.youtube.com/watch?v=... / playlist", color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        onImportYouTubeLink(ytQuickUrl)
                                        ytQuickUrl = ""
                                    },
                                    enabled = ytQuickUrl.isNotBlank(),
                                    border = BorderStroke(1.dp, YouTubeRed),
                                    shape = CircleShape
                                ) {
                                    Text("Sinkronkan", color = YouTubeRed, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        HorizontalDivider(color = GlassBorderSubtle)

                        // Auto-Sync Toggle & Real-Time Revision Metadata
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, tint = ArunCyan)
                                Column {
                                    Text(
                                        text = "Sinkronisasi Otomatis dengan Akun",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Rev #${syncState.revision} • ${syncTimeFormatter.format(Date(syncState.updatedAtMillis))} • Sesi ${syncState.sessionId}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextMuted
                                    )
                                }
                            }
                            Switch(
                                checked = profile?.autoSync ?: true,
                                onCheckedChange = onToggleAutoSync,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF041421),
                                    checkedTrackColor = ArunCyan
                                ),
                                modifier = Modifier.testTag("auto_sync_switch")
                            )
                        }

                        OutlinedButton(
                            onClick = onSyncNow,
                            enabled = !syncState.isSyncing,
                            border = BorderStroke(1.dp, ArunCyan.copy(alpha = 0.65f)),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hub_sync_now_button")
                        ) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, tint = ArunCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (syncState.isSyncing) "Menyinkronkan..." else "Sinkronkan Sekarang",
                                color = ArunCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        HorizontalDivider(color = GlassBorderSubtle)

                        // Layout Mode Switcher (Mobile vs Desktop Preview)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tata Letak Preview (Mobile / Desktop)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LayoutPreviewMode.entries.forEach { mode ->
                                    val selected = layoutPreviewMode == mode
                                    Surface(
                                        shape = CircleShape,
                                        color = if (selected) ArunCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                                        border = BorderStroke(1.dp, if (selected) ArunCyan else GlassBorderSubtle),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { onChangeLayoutMode(mode) }
                                            .testTag("hub_layout_${mode.name.lowercase()}")
                                    ) {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) ArunCyan else TextSecondary,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = GlassBorderSubtle)

                        // Other Music Apps
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Aplikasi Musik Lainnya", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = if (linkedApps.isEmpty()) "Belum ada aplikasi tambahan" else linkedApps.joinToString(", "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = { showOtherAppsDialog = true },
                                border = BorderStroke(1.dp, ArunCyan.copy(alpha = 0.6f)),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Rounded.AddLink, contentDescription = null, tint = ArunCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Integrasi", color = ArunCyan)
                            }
                        }
                    }
                }
            }
        }

        // 2. Fitur & Alat Audio
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Fitur & Alat Audio", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HubTileCard(
                        icon = Icons.Rounded.Mic,
                        title = "Kenali Lagu",
                        subtitle = "Deteksi akustik",
                        accent = ArunCyan,
                        glassIntensity = glassIntensity,
                        onClick = onOpenRecognizer,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hub_kenali_lagu")
                    )
                    HubTileCard(
                        icon = Icons.Rounded.Equalizer,
                        title = "Ekualizer",
                        subtitle = profile?.eqPresetEnum()?.labelId ?: "Flat",
                        accent = ArunIndigo,
                        glassIntensity = glassIntensity,
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("hub_ekualizer")
                    )
                }
            }
        }

        // 3. Aktivitas & Wawasan
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Aktivitas & Wawasan", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                GlassCard(
                    glassIntensity = glassIntensity,
                    borderColor = GlassBorderSubtle,
                    onClick = onOpenActivityRecap,
                    modifier = Modifier.testTag("hub_riwayat_recap")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HubRowItem(
                            icon = Icons.Rounded.History,
                            title = "Riwayat Pemutaran",
                            subtitle = "${playHistory.size} aktivitas lagu tercatat",
                            tint = ArunEmerald
                        )
                        HorizontalDivider(color = GlassBorderSubtle)
                        HubRowItem(
                            icon = Icons.Rounded.AutoGraph,
                            title = "Statistik & Recap",
                            subtitle = "Waktu dengar, top artis & grafik genre",
                            tint = ArunAmber
                        )
                    }
                }
            }
        }

        // 4. Sistem (Tentang & Pengaturan)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sistem", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                GlassCard(
                    glassIntensity = glassIntensity,
                    borderColor = GlassBorderSubtle,
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("hub_pengaturan")
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HubRowItem(
                            icon = Icons.Rounded.Settings,
                            title = "Pengaturan",
                            subtitle = "Kualitas streaming, tema kaca & cache (${formatBytes(offlineCacheBytes)})",
                            tint = ArunCyan
                        )
                        HorizontalDivider(color = GlassBorderSubtle)
                        HubRowItem(
                            icon = Icons.Rounded.Info,
                            title = "Tentang Arun Music",
                            subtitle = "Versi ${BuildConfig.VERSION_NAME} • Informasi & kredit pengembang",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }

        // Bottom Creator Footer visible on Hub as well as inside Pengaturan
        item {
            GlassCard(glassIntensity = glassIntensity, borderColor = GlassBorderSpecular.copy(alpha = 0.4f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Dikembangkan oleh @r.fansy_",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/r.fansy_"))
                                )
                            } catch (_: Exception) {
                            }
                        },
                        border = BorderStroke(1.dp, ArunCyan.copy(alpha = 0.6f)),
                        shape = CircleShape
                    ) {
                        Text("Instagram @r.fansy_", color = ArunCyan, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = ArunCyan, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "© 2026 Arun Music by @r.fansy_. All Rights Reserved 2026.",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    val linkTarget = linkDialogPlatform
    if (linkTarget != null) {
        var accountInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { linkDialogPlatform = null },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Tautkan ${linkTarget.label}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Masukkan email atau username akun ${linkTarget.label} Anda untuk sinkronisasi katalog:")
                    OutlinedTextField(
                        value = accountInput,
                        onValueChange = { accountInput = it },
                        label = { Text(if (linkTarget == PlatformSource.YOUTUBE_MUSIC) "Email YouTube Music" else "Username Spotify") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val acc = accountInput.trim().ifEmpty {
                            if (linkTarget == PlatformSource.YOUTUBE_MUSIC) "arun@youtube.com" else "@arun_user"
                        }
                        onUpdatePlatformConnection(linkTarget, true, acc)
                        linkDialogPlatform = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArunCyan, contentColor = Color(0xFF041421))
                ) {
                    Text("Hubungkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { linkDialogPlatform = null }) { Text("Batal") }
            }
        )
    }

    if (showOtherAppsDialog) {
        val allSupportedApps = listOf("Apple Music", "SoundCloud", "Deezer", "Tidal", "Bandcamp")
        AlertDialog(
            onDismissRequest = { showOtherAppsDialog = false },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Integrasi Aplikasi Musik Lainnya") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allSupportedApps.forEach { appName ->
                        val isLinked = appName in linkedApps
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleLinkedApp(appName, linkedApps) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isLinked,
                                onCheckedChange = { onToggleLinkedApp(appName, linkedApps) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(appName, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOtherAppsDialog = false }) { Text("Selesai") }
            }
        )
    }
}

@Composable
private fun PlatformAccountRow(
    title: String,
    connected: Boolean,
    account: String,
    accent: Color,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                if (connected) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Terhubung", tint = accent, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = if (connected) "Terhubung: ${account.ifBlank { "Akun Aktif" }}" else "Belum ditautkan",
                style = MaterialTheme.typography.bodyMedium,
                color = if (connected) TextSecondary else TextMuted
            )
        }
        OutlinedButton(
            onClick = onAction,
            border = BorderStroke(1.dp, if (connected) ArunRose.copy(alpha = 0.5f) else accent),
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (connected) Icons.Rounded.LinkOff else Icons.Rounded.AddLink,
                contentDescription = null,
                tint = if (connected) ArunRose else accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (connected) "Putuskan" else "Hubungkan",
                color = if (connected) ArunRose else accent
            )
        }
    }
}

@Composable
private fun HubTileCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    glassIntensity: GlassIntensity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        glassIntensity = glassIntensity,
        borderColor = accent.copy(alpha = 0.5f),
        onClick = onClick,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.18f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun HubRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.16f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
    }
}
