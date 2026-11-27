package com.example.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class SpotifyImportedCandidate(
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Long,
    val artworkUrl: String,
    val previewUrl: String,
    val spotifyUri: String,
    val youtubeVideoId: String = "",
    val youtubeTitle: String = "",
    val genre: String = "Spotify Import"
)

data class SpotifyImportBatch(
    val sourceLabel: String,
    val suggestedPlaylistName: String,
    val tracks: List<SpotifyImportedCandidate>
)

object SpotifyImporter {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val spotifyUrlRegex = Regex(
        """(?:https?://open\.spotify\.com/(?:intl-[a-z-]+/)?(track|playlist|album)/([a-zA-Z0-9]+))|(?:spotify:(track|playlist|album):([a-zA-Z0-9]+))""",
        RegexOption.IGNORE_CASE
    )

    suspend fun importFromUrl(rawInput: String): Result<SpotifyImportBatch> = withContext(Dispatchers.IO) {
        try {
            val cleaned = rawInput.trim()

            // Allow importing direct YouTube / YouTube Music playlist links (playlist?list=...)
            val ytPlaylistId = YouTubeAudioResolver.extractPlaylistIdFromUrl(cleaned)
            val ytId = YouTubeAudioResolver.extractVideoIdFromUrl(cleaned)
            if (ytPlaylistId != null && (cleaned.contains("/playlist", ignoreCase = true) || ytId == null)) {
                val plResult = YouTubeAudioResolver.fetchPlaylist(ytPlaylistId).getOrElse { err ->
                    return@withContext Result.failure(err)
                }
                val plTracks = plResult.tracks.map { item ->
                    SpotifyImportedCandidate(
                        trackId = sanitizeId("yt_${item.videoId}"),
                        title = item.title.take(120),
                        artist = item.channel.take(120),
                        album = plResult.title.take(120),
                        durationSec = item.durationSec.toLong(),
                        artworkUrl = item.thumbnailUrl.take(500),
                        previewUrl = "",
                        spotifyUri = YouTubeAudioResolver.buildMusicWatchUrl(item.videoId).take(200),
                        youtubeVideoId = item.videoId,
                        youtubeTitle = item.title.take(160),
                        genre = "YouTube Music"
                    )
                }
                return@withContext Result.success(
                    SpotifyImportBatch(
                        sourceLabel = cleaned,
                        suggestedPlaylistName = plResult.title.take(80),
                        tracks = plTracks
                    )
                )
            }

            // Allow importing direct YouTube / YouTube Music song links as well
            if (ytId != null) {
                val resolved = YouTubeAudioResolver.resolveStreamForVideo(ytId).getOrElse { err ->
                    return@withContext Result.failure(err)
                }
                val cand = SpotifyImportedCandidate(
                    trackId = sanitizeId("yt_$ytId"),
                    title = resolved.title.take(120),
                    artist = resolved.channel.take(120),
                    album = "YouTube Music",
                    durationSec = resolved.durationSec.toLong(),
                    artworkUrl = resolved.thumbnailUrl.take(500),
                    previewUrl = "",
                    spotifyUri = YouTubeAudioResolver.buildMusicWatchUrl(ytId).take(200),
                    youtubeVideoId = ytId,
                    youtubeTitle = resolved.title.take(160),
                    genre = "YouTube Music"
                )
                return@withContext Result.success(
                    SpotifyImportBatch(
                        sourceLabel = cleaned,
                        suggestedPlaylistName = resolved.title.take(80),
                        tracks = listOf(cand)
                    )
                )
            }

            val match = spotifyUrlRegex.find(cleaned)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Tautan tidak valid. Gunakan link Spotify (open.spotify.com/...) atau YouTube Music (music.youtube.com/... / youtube.com/...).")
                )

            val type = (match.groupValues[1].ifEmpty { match.groupValues[3] }).lowercase()
            val spotifyId = match.groupValues[2].ifEmpty { match.groupValues[4] }
            val canonicalUrl = "https://open.spotify.com/$type/$spotifyId"
            val embedUrl = "https://open.spotify.com/embed/$type/$spotifyId"

            val embedReq = Request.Builder()
                .url(embedUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0 Mobile Safari/537.36")
                .build()

            var playlistTitle = "Impor Spotify"
            val candidates = mutableListOf<SpotifyImportedCandidate>()

            client.newCall(embedReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val html = resp.body?.string().orEmpty()
                    val jsonStr = extractNextDataJson(html)
                    if (jsonStr != null) {
                        parseEmbedNextData(jsonStr, type, spotifyId, candidates)?.let { title ->
                            playlistTitle = title
                        }
                    }
                }
            }

            // Fallback / enrichment via Spotify public oEmbed endpoint
            if (candidates.isEmpty()) {
                val encoded = URLEncoder.encode(canonicalUrl, "UTF-8")
                val oembedReq = Request.Builder()
                    .url("https://open.spotify.com/oembed?url=$encoded")
                    .header("User-Agent", "ArunMusic/1.1.1")
                    .build()

                client.newCall(oembedReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        val json = JSONObject(body)
                        val title = json.optString("title").trim()
                        val thumb = json.optString("thumbnail_url").trim()
                        if (title.isNotEmpty()) {
                            playlistTitle = title
                            candidates.add(
                                SpotifyImportedCandidate(
                                    trackId = sanitizeId("sp_${type}_$spotifyId"),
                                    title = title.take(120),
                                    artist = "Artis Spotify",
                                    album = if (type == "album") title.take(120) else "Impor Spotify",
                                    durationSec = 210L,
                                    artworkUrl = thumb.take(500),
                                    previewUrl = "",
                                    spotifyUri = "spotify:$type:$spotifyId"
                                )
                            )
                        }
                    }
                }
            }

            if (candidates.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("Tidak dapat mengambil data lagu dari tautan Spotify tersebut. Pastikan playlist/album bersifat publik atau coba impor file ekspor Spotify (.json/.csv).")
                )
            }

            val enriched = enrichInitialTracksWithYouTube(candidates.take(80))

            Result.success(
                SpotifyImportBatch(
                    sourceLabel = canonicalUrl,
                    suggestedPlaylistName = playlistTitle.take(80),
                    tracks = enriched
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun enrichInitialTracksWithYouTube(
        tracks: List<SpotifyImportedCandidate>
    ): List<SpotifyImportedCandidate> {
        return tracks.mapIndexed { idx, cand ->
            if (idx < 3 && cand.youtubeVideoId.isBlank()) {
                val qArtist = if (cand.artist.equals("Artis Spotify", ignoreCase = true)) "" else cand.artist
                val yt = YouTubeAudioResolver.searchCandidates("${cand.title} $qArtist official audio".trim(), limit = 1)
                    .getOrNull()?.firstOrNull()
                if (yt != null) {
                    cand.copy(
                        durationSec = yt.durationSec.toLong().coerceIn(15L, 7200L),
                        artworkUrl = cand.artworkUrl.ifBlank { yt.thumbnailUrl },
                        youtubeVideoId = yt.videoId,
                        youtubeTitle = yt.title.take(160)
                    )
                } else cand
            } else cand
        }
    }

    suspend fun importFromFile(context: Context, uri: Uri, displayName: String?): Result<SpotifyImportBatch> =
        withContext(Dispatchers.IO) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@withContext Result.failure(IllegalStateException("Gagal membaca isi file."))

                val trimmed = content.trim()
                if (trimmed.isEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("File kosong."))
                }

                val candidates = mutableListOf<SpotifyImportedCandidate>()
                var suggestedName = displayName
                    ?.substringBeforeLast('.')
                    ?.replace('_', ' ')
                    ?.take(80)
                    ?.ifBlank { "Impor File Spotify" }
                    ?: "Impor File Spotify"

                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    suggestedName = parseSpotifyExportJson(trimmed, suggestedName, candidates)
                } else {
                    parseExportifyCsv(trimmed, candidates)
                }

                if (candidates.isEmpty()) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Format file tidak dikenali. Gunakan file YourLibrary.json, Playlist1.json, atau CSV Exportify dari Spotify.")
                    )
                }

                Result.success(
                    SpotifyImportBatch(
                        sourceLabel = displayName ?: "File Spotify",
                        suggestedPlaylistName = suggestedName.take(80),
                        tracks = candidates.take(80)
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun extractNextDataJson(html: String): String? {
        val marker = "<script id=\"__NEXT_DATA__\""
        val startIdx = html.indexOf(marker)
        if (startIdx < 0) return null
        val contentStart = html.indexOf('>', startIdx)
        if (contentStart < 0) return null
        val endIdx = html.indexOf("</script>", contentStart)
        if (endIdx < 0) return null
        return html.substring(contentStart + 1, endIdx)
    }

    private fun parseEmbedNextData(
        jsonStr: String,
        type: String,
        spotifyId: String,
        out: MutableList<SpotifyImportedCandidate>
    ): String? {
        val root = JSONObject(jsonStr)
        val entity = root.optJSONObject("props")
            ?.optJSONObject("pageProps")
            ?.optJSONObject("state")
            ?.optJSONObject("data")
            ?.optJSONObject("entity")
            ?: return null

        val entityTitle = entity.optString("title")
            .ifBlank { entity.optString("name") }
            .ifBlank { "Impor Spotify" }

        val artworkUrl = extractCoverUrl(entity)
        val trackList = entity.optJSONArray("trackList")

        if (trackList != null && trackList.length() > 0) {
            for (i in 0 until trackList.length()) {
                val item = trackList.optJSONObject(i) ?: continue
                val title = item.optString("title").trim()
                if (title.isEmpty()) continue
                val artist = item.optString("subtitle").trim().ifEmpty { "Artis Spotify" }
                val uri = item.optString("uri").ifEmpty { "spotify:track:${spotifyId}_$i" }
                val uid = item.optString("uid").ifEmpty { uri.substringAfterLast(':').ifEmpty { "${spotifyId}_$i" } }
                val durationMs = item.optLong("duration", 195_000L)
                val durationSec = (durationMs / 1000L).coerceIn(15L, 7200L)
                val preview = item.optJSONObject("audioPreview")?.optString("url").orEmpty()
                val itemArt = extractCoverUrl(item).ifBlank { artworkUrl }

                out.add(
                    SpotifyImportedCandidate(
                        trackId = sanitizeId("sp_$uid"),
                        title = title.take(120),
                        artist = artist.take(120),
                        album = if (type == "album") entityTitle.take(120) else entityTitle.take(120),
                        durationSec = durationSec,
                        artworkUrl = itemArt.take(500),
                        previewUrl = preview.take(500),
                        spotifyUri = uri.take(200)
                    )
                )
            }
        } else if (type == "track") {
            val subtitle = entity.optJSONArray("artists")?.let { arr ->
                (0 until arr.length()).mapNotNull { idx ->
                    arr.optJSONObject(idx)?.optString("name")?.takeIf { it.isNotBlank() }
                }.joinToString(", ")
            }.orEmpty().ifBlank { entity.optString("subtitle").ifBlank { "Artis Spotify" } }

            val durationMs = entity.optLong("duration", 195_000L)
            val preview = entity.optJSONObject("audioPreview")?.optString("url").orEmpty()
            out.add(
                SpotifyImportedCandidate(
                    trackId = sanitizeId("sp_$spotifyId"),
                    title = entityTitle.take(120),
                    artist = subtitle.take(120),
                    album = "Single Spotify",
                    durationSec = (durationMs / 1000L).coerceIn(15L, 7200L),
                    artworkUrl = artworkUrl.take(500),
                    previewUrl = preview.take(500),
                    spotifyUri = "spotify:track:$spotifyId"
                )
            )
        }

        return entityTitle
    }

    private fun extractCoverUrl(obj: JSONObject): String {
        val visual = obj.optJSONObject("visualIdentity")?.optJSONArray("image")
        if (visual != null && visual.length() > 0) {
            return visual.optJSONObject(0)?.optString("url").orEmpty()
        }
        val sources = obj.optJSONObject("coverArt")?.optJSONArray("sources")
        if (sources != null && sources.length() > 0) {
            return sources.optJSONObject(0)?.optString("url").orEmpty()
        }
        return ""
    }

    private fun parseSpotifyExportJson(
        raw: String,
        defaultName: String,
        out: MutableList<SpotifyImportedCandidate>
    ): String {
        var name = defaultName
        if (raw.startsWith("[")) {
            val arr = JSONArray(raw)
            parseGenericTrackArray(arr, defaultName, out)
            return name
        }
        val obj = JSONObject(raw)
        // Spotify GDPR YourLibrary.json: { "tracks": [ { "artist": "...", "album": "...", "track": "...", "uri": "..." } ] }
        obj.optJSONArray("tracks")?.let { arr ->
            parseGenericTrackArray(arr, "Pustaka Spotify", out)
            if (out.isNotEmpty()) name = "Pustaka Spotify"
        }
        // Spotify GDPR Playlist1.json: { "playlists": [ { "name": "...", "items": [ { "track": { ... } } ] } ] }
        obj.optJSONArray("playlists")?.let { playlists ->
            for (p in 0 until playlists.length()) {
                val pl = playlists.optJSONObject(p) ?: continue
                val plName = pl.optString("name").ifBlank { name }
                if (p == 0) name = plName
                val items = pl.optJSONArray("items") ?: continue
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val tr = item.optJSONObject("track") ?: item
                    val title = tr.optString("trackName").ifBlank { tr.optString("track") }.ifBlank { tr.optString("name") }.trim()
                    if (title.isEmpty()) continue
                    val artist = tr.optString("artistName").ifBlank { tr.optString("artist") }.ifBlank { "Artis Spotify" }
                    val album = tr.optString("albumName").ifBlank { tr.optString("album") }.ifBlank { plName }
                    val uri = tr.optString("trackUri").ifBlank { tr.optString("uri") }
                    val idSeed = uri.ifBlank { "$title|$artist|$album" }
                    out.add(
                        SpotifyImportedCandidate(
                            trackId = stableId(idSeed),
                            title = title.take(120),
                            artist = artist.take(120),
                            album = album.take(120),
                            durationSec = 195L,
                            artworkUrl = "",
                            previewUrl = "",
                            spotifyUri = uri.take(200)
                        )
                    )
                }
            }
        }
        return name
    }

    private fun parseGenericTrackArray(
        arr: JSONArray,
        fallbackAlbum: String,
        out: MutableList<SpotifyImportedCandidate>
    ) {
        for (i in 0 until arr.length()) {
            val tr = arr.optJSONObject(i) ?: continue
            val title = tr.optString("track")
                .ifBlank { tr.optString("trackName") }
                .ifBlank { tr.optString("title") }
                .ifBlank { tr.optString("name") }
                .trim()
            if (title.isEmpty()) continue
            val artist = tr.optString("artist")
                .ifBlank { tr.optString("artistName") }
                .ifBlank { "Artis Spotify" }
            val album = tr.optString("album")
                .ifBlank { tr.optString("albumName") }
                .ifBlank { fallbackAlbum }
            val uri = tr.optString("uri").ifBlank { tr.optString("trackUri") }
            val durationMs = tr.optLong("duration_ms", tr.optLong("durationMs", 195_000L))
            out.add(
                SpotifyImportedCandidate(
                    trackId = stableId(uri.ifBlank { "$title|$artist|$album" }),
                    title = title.take(120),
                    artist = artist.take(120),
                    album = album.take(120),
                    durationSec = (durationMs / 1000L).coerceIn(15L, 7200L),
                    artworkUrl = tr.optString("artworkUrl").take(500),
                    previewUrl = tr.optString("previewUrl").take(500),
                    spotifyUri = uri.take(200)
                )
            )
        }
    }

    private fun parseExportifyCsv(raw: String, out: MutableList<SpotifyImportedCandidate>) {
        val lines = raw.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return
        val headers = splitCsvLine(lines.first()).map { it.trim().lowercase() }
        val idxTitle = headers.indexOfFirst { it.contains("track name") || it == "track" || it == "title" }
        val idxArtist = headers.indexOfFirst { it.contains("artist") }
        val idxAlbum = headers.indexOfFirst { it.contains("album") }
        val idxUri = headers.indexOfFirst { it.contains("uri") }
        val idxDur = headers.indexOfFirst { it.contains("duration") }
        if (idxTitle < 0) return

        for (line in lines.drop(1)) {
            val cols = splitCsvLine(line)
            val title = cols.getOrNull(idxTitle)?.trim().orEmpty()
            if (title.isEmpty()) continue
            val artist = cols.getOrNull(idxArtist)?.trim().orEmpty().ifBlank { "Artis Spotify" }
            val album = cols.getOrNull(idxAlbum)?.trim().orEmpty().ifBlank { "Impor Spotify" }
            val uri = cols.getOrNull(idxUri)?.trim().orEmpty()
            val durMs = cols.getOrNull(idxDur)?.trim()?.toLongOrNull() ?: 195_000L
            val durSec = if (durMs > 1000L) durMs / 1000L else durMs
            out.add(
                SpotifyImportedCandidate(
                    trackId = stableId(uri.ifBlank { "$title|$artist|$album" }),
                    title = title.take(120),
                    artist = artist.take(120),
                    album = album.take(120),
                    durationSec = durSec.coerceIn(15L, 7200L),
                    artworkUrl = "",
                    previewUrl = "",
                    spotifyUri = uri.take(200)
                )
            )
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(ch)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    private fun stableId(seed: String): String {
        val spId = seed.substringAfterLast(':').takeIf {
            seed.startsWith("spotify:track:") && it.matches(Regex("^[a-zA-Z0-9]+$"))
        }
        if (spId != null) return sanitizeId("sp_$spId")
        val digest = MessageDigest.getInstance("SHA-1").digest(seed.toByteArray())
        val hex = digest.joinToString("") { "%02x".format(it) }.take(16)
        return "sp_$hex"
    }

    private fun sanitizeId(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64).ifBlank { "sp_track" }
    }
}
