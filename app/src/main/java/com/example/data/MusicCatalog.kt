package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.model.AlbumCollection
import com.example.model.MusicTrack
import com.example.model.PlatformSource
import com.example.model.SynthTimbre

object MusicCatalog {

    val tracks: List<MusicTrack> = listOf(
        MusicTrack(
            id = "trk_midnight_01",
            title = "Kaca Malam",
            artist = "Nadia Arun & Kael",
            album = "Refraksi Kota",
            genre = "Electronic",
            durationSec = 198,
            bpm = 112,
            rootMidi = 57, // A3
            scaleIntervals = listOf(0, 3, 7, 10, 14),
            timbre = SynthTimbre.WARM_EP,
            coverRes = R.drawable.img_album_midnight,
            coverKey = "midnight",
            accentColor = Color(0xFF38BDF8),
            platform = PlatformSource.SPOTIFY,
            releaseYear = 2026
        ),
        MusicTrack(
            id = "trk_sunset_02",
            title = "Senja di Pelabuhan",
            artist = "Raka Pratama",
            album = "Cahaya Pesisir",
            genre = "Indie Pop",
            durationSec = 214,
            bpm = 96,
            rootMidi = 60, // C4
            scaleIntervals = listOf(0, 4, 7, 11, 14),
            timbre = SynthTimbre.CRYSTAL_PLUCK,
            coverRes = R.drawable.img_album_sunset,
            coverKey = "sunset",
            accentColor = Color(0xFFF59E0B),
            platform = PlatformSource.YOUTUBE_MUSIC,
            releaseYear = 2026
        ),
        MusicTrack(
            id = "trk_emerald_03",
            title = "Hujan Kristal",
            artist = "Lembayung Collective",
            album = "Hutan Kaca",
            genre = "Ambient",
            durationSec = 242,
            bpm = 84,
            rootMidi = 53, // F3
            scaleIntervals = listOf(0, 4, 7, 9, 11),
            timbre = SynthTimbre.VELVET_PAD,
            coverRes = R.drawable.img_album_emerald,
            coverKey = "emerald",
            accentColor = Color(0xFF10B981),
            platform = PlatformSource.SPOTIFY,
            releaseYear = 2025
        ),
        MusicTrack(
            id = "trk_violet_04",
            title = "Gema Ultraviolet",
            artist = "Sora Vania",
            album = "Neon Spektrum",
            genre = "R&B",
            durationSec = 186,
            bpm = 102,
            rootMidi = 55, // G3
            scaleIntervals = listOf(0, 3, 7, 10, 12),
            timbre = SynthTimbre.SUB_DREAM,
            coverRes = R.drawable.img_album_violet,
            coverKey = "violet",
            accentColor = Color(0xFFA855F7),
            platform = PlatformSource.YOUTUBE_MUSIC,
            releaseYear = 2026
        ),
        MusicTrack(
            id = "trk_midnight_05",
            title = "Orbit Tenang",
            artist = "Nadia Arun & Kael",
            album = "Refraksi Kota",
            genre = "Lo-Fi",
            durationSec = 175,
            bpm = 88,
            rootMidi = 62, // D4
            scaleIntervals = listOf(0, 3, 7, 10, 14),
            timbre = SynthTimbre.WARM_EP,
            coverRes = R.drawable.img_album_midnight,
            coverKey = "midnight",
            accentColor = Color(0xFF38BDF8),
            platform = PlatformSource.SPOTIFY,
            releaseYear = 2026
        ),
        MusicTrack(
            id = "trk_sunset_06",
            title = "Langkah Emas",
            artist = "Tara & Biru",
            album = "Cahaya Pesisir",
            genre = "Indie Pop",
            durationSec = 204,
            bpm = 108,
            rootMidi = 58, // Bb3
            scaleIntervals = listOf(0, 4, 7, 11, 14),
            timbre = SynthTimbre.CRYSTAL_PLUCK,
            coverRes = R.drawable.img_album_sunset,
            coverKey = "sunset",
            accentColor = Color(0xFFFB7185),
            platform = PlatformSource.SPOTIFY,
            releaseYear = 2025
        ),
        MusicTrack(
            id = "trk_emerald_07",
            title = "Napas Embun",
            artist = "Lembayung Collective",
            album = "Hutan Kaca",
            genre = "Ambient",
            durationSec = 228,
            bpm = 78,
            rootMidi = 52, // E3
            scaleIntervals = listOf(0, 3, 7, 11, 14),
            timbre = SynthTimbre.VELVET_PAD,
            coverRes = R.drawable.img_album_emerald,
            coverKey = "emerald",
            accentColor = Color(0xFF34D399),
            platform = PlatformSource.YOUTUBE_MUSIC,
            releaseYear = 2026
        ),
        MusicTrack(
            id = "trk_violet_08",
            title = "Kilau Prisma",
            artist = "Sora Vania",
            album = "Neon Spektrum",
            genre = "Synth Pop",
            durationSec = 192,
            bpm = 118,
            rootMidi = 59, // B3
            scaleIntervals = listOf(0, 3, 7, 10, 14),
            timbre = SynthTimbre.SUB_DREAM,
            coverRes = R.drawable.img_album_violet,
            coverKey = "violet",
            accentColor = Color(0xFFC084FC),
            platform = PlatformSource.SPOTIFY,
            releaseYear = 2026
        ),
        MusicTrack(
            id = "trk_midnight_09",
            title = "Detak Tengah Malam",
            artist = "Davin Kusuma",
            album = "Frekuensi 02",
            genre = "Electronic",
            durationSec = 209,
            bpm = 124,
            rootMidi = 54, // F#3
            scaleIntervals = listOf(0, 3, 7, 10, 12),
            timbre = SynthTimbre.SUB_DREAM,
            coverRes = R.drawable.img_album_midnight,
            coverKey = "midnight",
            accentColor = Color(0xFF60A5FA),
            platform = PlatformSource.YOUTUBE_MUSIC,
            releaseYear = 2025
        ),
        MusicTrack(
            id = "trk_sunset_10",
            title = "Kopi & Piringan Hitam",
            artist = "Raka Pratama",
            album = "Ruang Hangat",
            genre = "Lo-Fi",
            durationSec = 165,
            bpm = 82,
            rootMidi = 60,
            scaleIntervals = listOf(0, 4, 7, 9, 11),
            timbre = SynthTimbre.WARM_EP,
            coverRes = R.drawable.img_album_sunset,
            coverKey = "sunset",
            accentColor = Color(0xFFFBBF24),
            platform = PlatformSource.YOUTUBE_MUSIC,
            releaseYear = 2026
        ),
        MusicTrack(
            id = "trk_emerald_11",
            title = "Arah Angin Timur",
            artist = "Kirana",
            album = "Taman Sunyi",
            genre = "Jazz",
            durationSec = 221,
            bpm = 94,
            rootMidi = 56, // Ab3
            scaleIntervals = listOf(0, 4, 7, 11, 14),
            timbre = SynthTimbre.WARM_EP,
            coverRes = R.drawable.img_album_emerald,
            coverKey = "emerald",
            accentColor = Color(0xFF2DD4BF),
            platform = PlatformSource.SPOTIFY,
            releaseYear = 2025
        ),
        MusicTrack(
            id = "trk_violet_12",
            title = "Bayang di Cermin",
            artist = "Tara & Biru",
            album = "Distorsi Manis",
            genre = "R&B",
            durationSec = 194,
            bpm = 98,
            rootMidi = 57,
            scaleIntervals = listOf(0, 3, 7, 10, 14),
            timbre = SynthTimbre.CRYSTAL_PLUCK,
            coverRes = R.drawable.img_album_violet,
            coverKey = "violet",
            accentColor = Color(0xFFF472B6),
            platform = PlatformSource.YOUTUBE_MUSIC,
            releaseYear = 2026
        )
    )

    val albums: List<AlbumCollection> = listOf(
        AlbumCollection(
            id = "alb_refraksi",
            title = "Refraksi Kota",
            artist = "Nadia Arun & Kael",
            genre = "Electronic",
            year = 2026,
            coverRes = R.drawable.img_album_midnight,
            accentColor = Color(0xFF38BDF8),
            platform = PlatformSource.SPOTIFY,
            trackIds = listOf("trk_midnight_01", "trk_midnight_05")
        ),
        AlbumCollection(
            id = "alb_pesisir",
            title = "Cahaya Pesisir",
            artist = "Raka Pratama",
            genre = "Indie Pop",
            year = 2026,
            coverRes = R.drawable.img_album_sunset,
            accentColor = Color(0xFFF59E0B),
            platform = PlatformSource.YOUTUBE_MUSIC,
            trackIds = listOf("trk_sunset_02", "trk_sunset_06")
        ),
        AlbumCollection(
            id = "alb_hutan",
            title = "Hutan Kaca",
            artist = "Lembayung Collective",
            genre = "Ambient",
            year = 2025,
            coverRes = R.drawable.img_album_emerald,
            accentColor = Color(0xFF10B981),
            platform = PlatformSource.SPOTIFY,
            trackIds = listOf("trk_emerald_03", "trk_emerald_07")
        ),
        AlbumCollection(
            id = "alb_neon",
            title = "Neon Spektrum",
            artist = "Sora Vania",
            genre = "R&B / Synth Pop",
            year = 2026,
            coverRes = R.drawable.img_album_violet,
            accentColor = Color(0xFFA855F7),
            platform = PlatformSource.YOUTUBE_MUSIC,
            trackIds = listOf("trk_violet_04", "trk_violet_08")
        )
    )

    fun findTrack(trackId: String): MusicTrack? = tracks.firstOrNull { it.id == trackId }

    fun coverResForKey(coverKey: String): Int = when (coverKey) {
        "sunset" -> R.drawable.img_album_sunset
        "emerald" -> R.drawable.img_album_emerald
        "violet" -> R.drawable.img_album_violet
        else -> R.drawable.img_album_midnight
    }
}
