package com.example.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PlatformSource(val label: String) {
    SPOTIFY("Spotify"),
    YOUTUBE_MUSIC("YouTube Music")
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class StreamQuality(val labelId: String, val bitrateLabel: String) {
    LOW("Hemat Kuota", "96 kbps"),
    NORMAL("Normal", "160 kbps"),
    HIGH("Tinggi", "320 kbps"),
    LOSSLESS("Hi-Fi Lossless", "1411 kbps")
}

enum class DownloadQuality(val labelId: String, val bitrateLabel: String) {
    NORMAL("Standar", "160 kbps"),
    HIGH("Tinggi", "320 kbps"),
    LOSSLESS("Lossless WAV", "1411 kbps")
}

enum class GlassIntensity(val labelId: String, val surfaceAlpha: Float, val blurRadiusDp: Int) {
    SUBTLE("Halus", 0.09f, 12),
    BALANCED("Seimbang", 0.14f, 22),
    DEEP("Mendalam", 0.21f, 34)
}

enum class EqPreset(val labelId: String, val bands: List<Int>) {
    FLAT("Flat", listOf(0, 0, 0, 0, 0)),
    BASS_BOOST("Bass Boost", listOf(7, 5, 0, 1, 2)),
    POP("Pop", listOf(-1, 3, 5, 3, -1)),
    ROCK("Rock", listOf(5, 3, -1, 3, 5)),
    JAZZ("Jazz", listOf(3, 1, -2, 2, 4)),
    CUSTOM("Kustom", listOf(0, 0, 0, 0, 0))
}

enum class SynthTimbre {
    WARM_EP,
    SUB_DREAM,
    CRYSTAL_PLUCK,
    VELVET_PAD
}

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationSec: Int,
    val bpm: Int,
    val rootMidi: Int,
    val scaleIntervals: List<Int>,
    val timbre: SynthTimbre,
    @DrawableRes val coverRes: Int,
    val coverKey: String,
    val accentColor: Color,
    val platform: PlatformSource,
    val releaseYear: Int,
    val artworkUrl: String = "",
    val previewUrl: String = "",
    val spotifyUri: String = "",
    val youtubeVideoId: String = "",
    val youtubeTitle: String = "",
    val isImported: Boolean = false
)

data class AlbumCollection(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val year: Int,
    @DrawableRes val coverRes: Int,
    val accentColor: Color,
    val platform: PlatformSource,
    val trackIds: List<String>
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Pendengar Arun",
    val autoSync: Boolean = true,
    val ytMusicConnected: Boolean = true,
    val ytMusicAccount: String = "arun.listener@youtube.com",
    val spotifyConnected: Boolean = true,
    val spotifyAccount: String = "@arun_listener",
    val linkedApps: List<String> = listOf("Apple Music"),
    val streamQuality: String = StreamQuality.HIGH.name,
    val downloadQuality: String = DownloadQuality.HIGH.name,
    val glassIntensity: String = GlassIntensity.BALANCED.name,
    val eqPreset: String = EqPreset.FLAT.name,
    val eqBands: List<Long> = listOf(0L, 0L, 0L, 0L, 0L),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun streamQualityEnum(): StreamQuality =
        enumValues<StreamQuality>().firstOrNull { it.name == streamQuality } ?: StreamQuality.HIGH

    fun downloadQualityEnum(): DownloadQuality =
        enumValues<DownloadQuality>().firstOrNull { it.name == downloadQuality } ?: DownloadQuality.HIGH

    fun glassIntensityEnum(): GlassIntensity =
        enumValues<GlassIntensity>().firstOrNull { it.name == glassIntensity } ?: GlassIntensity.BALANCED

    fun eqPresetEnum(): EqPreset =
        enumValues<EqPreset>().firstOrNull { it.name == eqPreset } ?: EqPreset.FLAT

    fun eqBandInts(): List<Int> {
        val list = eqBands.take(5).map { it.toInt().coerceIn(-12, 12) }
        return if (list.size == 5) list else listOf(0, 0, 0, 0, 0)
    }
}

@Keep
@IgnoreExtraProperties
data class PlaylistDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val trackIds: List<String> = emptyList(),
    val coverKey: String = "midnight",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LibraryTrackDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val liked: Boolean = false,
    val savedAlbum: Boolean = false,
    val downloaded: Boolean = false,
    val playCount: Long = 0L,
    val totalListenSec: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlayHistoryDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val trackTitle: String = "",
    val artistName: String = "",
    val genre: String = "",
    val platform: String = PlatformSource.SPOTIFY.name,
    val listenedSeconds: Long = 0L,
    @ServerTimestamp val playedAt: Timestamp? = null
) {
    fun platformEnum(): PlatformSource =
        enumValues<PlatformSource>().firstOrNull { it.name == platform } ?: PlatformSource.SPOTIFY
}

@Keep
@IgnoreExtraProperties
data class ImportedTrackDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val genre: String = "Spotify Import",
    val durationSec: Long = 180L,
    val artworkUrl: String = "",
    val previewUrl: String = "",
    val spotifyUri: String = "",
    val youtubeVideoId: String = "",
    val youtubeTitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMusicTrack(fallbackCoverRes: Int): MusicTrack {
        val hash = (trackId + title + artist).hashCode()
        val positive = if (hash == Int.MIN_VALUE) 0 else kotlin.math.abs(hash)
        val timbres = SynthTimbre.entries
        val accents = listOf(
            Color(0xFF1DB954),
            Color(0xFF38BDF8),
            Color(0xFF34D399),
            Color(0xFFA855F7),
            Color(0xFFF59E0B)
        )
        val isYtImport = trackId.startsWith("yt_") ||
            trackId.startsWith("sp_yt_") ||
            spotifyUri.contains("youtube", ignoreCase = true) ||
            genre.contains("YouTube", ignoreCase = true)
        return MusicTrack(
            id = trackId.ifBlank { id },
            title = title.ifBlank { if (isYtImport) "Lagu YouTube Music" else "Lagu Spotify" },
            artist = artist.ifBlank { if (isYtImport) "YouTube Music" else "Artis Spotify" },
            album = album.ifBlank { if (isYtImport) "Impor YouTube Music" else "Impor Spotify" },
            genre = genre.ifBlank { if (isYtImport) "YouTube Music" else "Spotify Import" },
            durationSec = durationSec.toInt().coerceIn(10, 7200),
            bpm = 84 + (positive % 44),
            rootMidi = 52 + (positive % 12),
            scaleIntervals = if (positive % 2 == 0) listOf(0, 3, 7, 10, 14) else listOf(0, 4, 7, 11, 14),
            timbre = timbres[positive % timbres.size],
            coverRes = fallbackCoverRes,
            coverKey = if (isYtImport) "midnight" else "emerald",
            accentColor = accents[positive % accents.size],
            platform = if (isYtImport) PlatformSource.YOUTUBE_MUSIC else PlatformSource.SPOTIFY,
            releaseYear = 2026,
            artworkUrl = artworkUrl,
            previewUrl = "",
            spotifyUri = spotifyUri,
            youtubeVideoId = youtubeVideoId,
            youtubeTitle = youtubeTitle,
            isImported = true
        )
    }
}
