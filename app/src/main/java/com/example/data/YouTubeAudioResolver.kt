package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class YouTubeCandidate(
    val videoId: String,
    val title: String,
    val channel: String,
    val durationSec: Int,
    val thumbnailUrl: String
)

data class YouTubeResolvedAudio(
    val videoId: String,
    val title: String,
    val channel: String,
    val durationSec: Int,
    val thumbnailUrl: String,
    val streamUrl: String,
    val userAgent: String,
    val mimeType: String
)

data class YouTubePlaylistResult(
    val playlistId: String,
    val title: String,
    val tracks: List<YouTubeCandidate>
)

object YouTubeAudioResolver {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val youtubeUrlRegex = Regex(
        """(?:https?://(?:www\.|m\.|music\.)?youtube\.com/(?:watch\?[^#\s]*v=|shorts/|embed/)([a-zA-Z0-9_-]{11}))|(?:https?://youtu\.be/([a-zA-Z0-9_-]{11}))""",
        RegexOption.IGNORE_CASE
    )

    private val youtubePlaylistRegex = Regex(
        """https?://(?:www\.|m\.|music\.)?youtube\.com/(?:playlist|watch)\?[^#\s]*list=([a-zA-Z0-9_-]{8,80})""",
        RegexOption.IGNORE_CASE
    )

    private data class InnertubeClientConfig(
        val clientName: String,
        val clientVersion: String,
        val userAgent: String,
        val osName: String,
        val osVersion: String,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val androidSdkVersion: Int? = null
    )

    private val playerClients = listOf(
        InnertubeClientConfig(
            clientName = "IOS",
            clientVersion = "19.45.4",
            userAgent = "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iOS 18_1_0 like Mac OS X;)",
            osName = "iPhone",
            osVersion = "18.1.0.22B83",
            deviceMake = "Apple",
            deviceModel = "iPhone16,2"
        ),
        InnertubeClientConfig(
            clientName = "ANDROID_VR",
            clientVersion = "1.60.19",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
            osName = "Android",
            osVersion = "12L",
            androidSdkVersion = 32
        ),
        InnertubeClientConfig(
            clientName = "ANDROID",
            clientVersion = "19.44.38",
            userAgent = "com.google.android.youtube/19.44.38 (Linux; U; Android 14) gzip",
            osName = "Android",
            osVersion = "14",
            androidSdkVersion = 34
        )
    )

    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://api.piped.private.coffee"
    )

    private val invidiousInstances = listOf(
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de"
    )

    fun extractVideoIdFromUrl(rawInput: String): String? {
        val m = youtubeUrlRegex.find(rawInput.trim()) ?: return null
        return m.groupValues[1].ifEmpty { m.groupValues[2] }.takeIf { it.length == 11 }
    }

    fun extractPlaylistIdFromUrl(rawInput: String): String? {
        val m = youtubePlaylistRegex.find(rawInput.trim()) ?: return null
        return m.groupValues[1].takeIf { it.length >= 8 }
    }

    fun buildWatchUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    fun buildMusicWatchUrl(videoId: String): String = "https://music.youtube.com/watch?v=$videoId"

    fun buildMusicSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        return "https://music.youtube.com/search?q=$encoded"
    }

    suspend fun fetchPlaylist(
        playlistId: String,
        limit: Int = 40
    ): Result<YouTubePlaylistResult> = withContext(Dispatchers.IO) {
        try {
            val cleanId = playlistId.trim()
            if (cleanId.length < 8) {
                return@withContext Result.failure(IllegalArgumentException("ID playlist YouTube tidak valid."))
            }

            val items = mutableListOf<YouTubeCandidate>()
            var playlistTitle = "Playlist YouTube Music"

            // Tier 1: Innertube browse endpoint
            try {
                val browseId = if (cleanId.startsWith("VL")) cleanId else "VL$cleanId"
                val bodyJson = JSONObject().apply {
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", "WEB")
                            put("clientVersion", "2.20241126.01.00")
                            put("hl", "en")
                            put("gl", "US")
                        })
                    })
                    put("browseId", browseId)
                }
                val req = Request.Builder()
                    .url("https://www.youtube.com/youtubei/v1/browse?prettyPrint=false")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0.0.0 Safari/537.36")
                    .header("Content-Type", "application/json")
                    .post(bodyJson.toString().toRequestBody(jsonMediaType))
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val raw = resp.body?.string().orEmpty()
                        if (raw.isNotBlank()) {
                            val root = JSONObject(raw)
                            extractPlaylistTitle(root)?.let { playlistTitle = it }
                            collectPlaylistVideoRenderers(root, items, limit * 2)
                        }
                    }
                }
            } catch (_: Exception) {
            }

            // Tier 2: HTML playlist page ytInitialData fallback
            if (items.isEmpty()) {
                try {
                    val req = Request.Builder()
                        .url("https://www.youtube.com/playlist?list=$cleanId")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0.0.0 Safari/537.36")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val html = resp.body?.string().orEmpty()
                            val marker = "var ytInitialData = "
                            val start = html.indexOf(marker)
                            if (start >= 0) {
                                val jsonStart = start + marker.length
                                val end = html.indexOf(";</script>", jsonStart)
                                if (end > jsonStart) {
                                    val root = JSONObject(html.substring(jsonStart, end))
                                    extractPlaylistTitle(root)?.let { playlistTitle = it }
                                    collectPlaylistVideoRenderers(root, items, limit * 2)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }

            // Tier 3: Piped playlist API fallback
            if (items.isEmpty()) {
                for (base in pipedInstances) {
                    try {
                        val req = Request.Builder()
                            .url("$base/playlists/$cleanId")
                            .header("User-Agent", "ArunMusic/1.1.1")
                            .build()
                        client.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) return@use
                            val obj = JSONObject(resp.body?.string().orEmpty())
                            obj.optString("name").trim().takeIf { it.isNotBlank() }?.let { playlistTitle = it }
                            val related = obj.optJSONArray("relatedStreams") ?: return@use
                            for (i in 0 until related.length()) {
                                val s = related.optJSONObject(i) ?: continue
                                val vid = s.optString("url").substringAfter("v=", "").take(11)
                                if (!vid.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) continue
                                items.add(
                                    YouTubeCandidate(
                                        videoId = vid,
                                        title = s.optString("title").take(140).ifBlank { "Lagu YouTube Music" },
                                        channel = s.optString("uploaderName").take(80).ifBlank { "YouTube Music" },
                                        durationSec = s.optInt("duration", 195).coerceIn(15, 7200),
                                        thumbnailUrl = s.optString("thumbnail").take(400)
                                            .ifBlank { "https://i.ytimg.com/vi/$vid/hqdefault.jpg" }
                                    )
                                )
                            }
                        }
                        if (items.isNotEmpty()) break
                    } catch (_: Exception) {
                    }
                }
            }

            val deduped = items.distinctBy { it.videoId }.take(limit)
            if (deduped.isEmpty()) {
                Result.failure(IllegalStateException("Playlist YouTube Music tidak dapat dimuat atau bersifat privat."))
            } else {
                Result.success(YouTubePlaylistResult(cleanId, playlistTitle.take(80), deduped))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractPlaylistTitle(root: JSONObject): String? {
        val header = root.optJSONObject("header")
        val title1 = header?.optJSONObject("playlistHeaderRenderer")?.optJSONObject("title")?.optString("simpleText")
        if (!title1.isNullOrBlank()) return title1.trim()
        val title2 = root.optJSONObject("metadata")?.optJSONObject("playlistMetadataRenderer")?.optString("title")
        if (!title2.isNullOrBlank()) return title2.trim()
        return null
    }

    private fun collectPlaylistVideoRenderers(
        node: Any?,
        out: MutableList<YouTubeCandidate>,
        maxCount: Int
    ) {
        if (out.size >= maxCount || node == null) return
        when (node) {
            is JSONObject -> {
                val pvr = node.optJSONObject("playlistVideoRenderer")
                if (pvr != null) {
                    val vid = pvr.optString("videoId").trim()
                    if (vid.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
                        val title = extractRunsText(pvr.optJSONObject("title"))
                            .ifBlank { pvr.optJSONObject("title")?.optString("simpleText").orEmpty() }
                            .trim()
                        if (title.isNotBlank()) {
                            val channel = extractRunsText(pvr.optJSONObject("shortBylineText"))
                                .ifBlank { "YouTube Music" }
                            val dur = pvr.optString("lengthSeconds").toIntOrNull()
                                ?: parseDurationString(pvr.optJSONObject("lengthText")?.optString("simpleText").orEmpty())
                                    .let { if (it > 0) it else 195 }
                            val thumbs = pvr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                            val thumb = if (thumbs != null && thumbs.length() > 0) {
                                thumbs.optJSONObject(thumbs.length() - 1)?.optString("url").orEmpty()
                            } else "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
                            out.add(
                                YouTubeCandidate(
                                    videoId = vid,
                                    title = title.take(140),
                                    channel = channel.take(80),
                                    durationSec = dur.coerceIn(15, 7200),
                                    thumbnailUrl = thumb.take(400)
                                )
                            )
                        }
                    }
                }
                val keys = node.keys()
                while (keys.hasNext() && out.size < maxCount) {
                    val key = keys.next()
                    if (key == "playlistVideoRenderer") continue
                    collectPlaylistVideoRenderers(node.opt(key), out, maxCount)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    if (out.size >= maxCount) break
                    collectPlaylistVideoRenderers(node.opt(i), out, maxCount)
                }
            }
        }
    }

    suspend fun searchCandidates(
        query: String,
        limit: Int = 6
    ): Result<List<YouTubeCandidate>> = withContext(Dispatchers.IO) {
        try {
            val cleanQuery = query.trim().take(160)
            if (cleanQuery.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Kata kunci pencarian kosong."))
            }

            // Tier 1: YouTube Innertube WEB search
            val results = mutableListOf<YouTubeCandidate>()
            searchInnertubeWeb(cleanQuery, limit, results)

            // Tier 2: YouTube HTML ytInitialData search
            if (results.isEmpty()) {
                searchYouTubeHtml(cleanQuery, limit, results)
            }

            // Tier 3: Piped Search API fallback
            if (results.isEmpty()) {
                searchPiped(cleanQuery, limit, results)
            }

            val deduped = results.distinctBy { it.videoId }.take(limit)
            if (deduped.isEmpty()) {
                Result.failure(IllegalStateException("Tidak ditemukan hasil lagu di YouTube untuk \"$cleanQuery\"."))
            } else {
                Result.success(deduped)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveTrack(
        title: String,
        artist: String,
        existingVideoId: String = "",
        skipFirstClient: Boolean = false
    ): Result<YouTubeResolvedAudio> = withContext(Dispatchers.IO) {
        if (existingVideoId.isNotBlank()) {
            val direct = resolveStreamForVideo(
                videoId = existingVideoId,
                fallbackTitle = title,
                skipClientIndex = if (skipFirstClient) 0 else -1
            )
            if (direct.isSuccess) return@withContext direct
        }

        val query = buildSearchQuery(title, artist)
        val candidatesRes = searchCandidates(query, limit = 5)
        val candidates = candidatesRes.getOrElse { err ->
            return@withContext Result.failure(err)
        }

        var lastError: Throwable? = null
        for (cand in candidates.take(3)) {
            val streamRes = resolveStreamForVideo(
                videoId = cand.videoId,
                fallbackTitle = cand.title.ifBlank { title },
                fallbackChannel = cand.channel,
                fallbackDurationSec = cand.durationSec,
                fallbackThumb = cand.thumbnailUrl,
                skipClientIndex = if (skipFirstClient) 0 else -1
            )
            if (streamRes.isSuccess) {
                return@withContext streamRes
            }
            lastError = streamRes.exceptionOrNull()
        }

        Result.failure(
            lastError ?: IllegalStateException("Audio full dari YouTube tidak tersedia saat ini.")
        )
    }

    suspend fun resolveStreamForVideo(
        videoId: String,
        fallbackTitle: String = "",
        fallbackChannel: String = "",
        fallbackDurationSec: Int = 0,
        fallbackThumb: String = "",
        skipClientIndex: Int = -1
    ): Result<YouTubeResolvedAudio> = withContext(Dispatchers.IO) {
        if (!videoId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return@withContext Result.failure(IllegalArgumentException("ID video YouTube tidak valid."))
        }

        var lastError: Throwable? = null

        playerClients.forEachIndexed { idx, cfg ->
            if (idx == skipClientIndex) return@forEachIndexed
            val res = tryInnertubePlayer(
                videoId = videoId,
                cfg = cfg,
                fallbackTitle = fallbackTitle,
                fallbackChannel = fallbackChannel,
                fallbackDurationSec = fallbackDurationSec,
                fallbackThumb = fallbackThumb
            )
            if (res.isSuccess) return@withContext res
            lastError = res.exceptionOrNull()
        }

        for (base in pipedInstances) {
            val res = tryPipedStreams(base, videoId, fallbackTitle, fallbackChannel, fallbackDurationSec, fallbackThumb)
            if (res.isSuccess) return@withContext res
            lastError = res.exceptionOrNull()
        }

        for (base in invidiousInstances) {
            val res = tryInvidiousStreams(base, videoId, fallbackTitle, fallbackChannel, fallbackDurationSec, fallbackThumb)
            if (res.isSuccess) return@withContext res
            lastError = res.exceptionOrNull()
        }

        Result.failure(lastError ?: IllegalStateException("Gagal mengambil stream audio YouTube."))
    }

    private fun buildSearchQuery(title: String, artist: String): String {
        val cleanArtist = artist.trim()
        val cleanTitle = title.trim()
        return if (cleanArtist.isBlank() ||
            cleanArtist.equals("Artis Spotify", ignoreCase = true) ||
            cleanArtist.equals("YouTube Music", ignoreCase = true)
        ) {
            "$cleanTitle official audio"
        } else {
            "$cleanTitle $cleanArtist official audio"
        }
    }

    private fun searchInnertubeWeb(
        query: String,
        limit: Int,
        out: MutableList<YouTubeCandidate>
    ) {
        try {
            val bodyJson = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB")
                        put("clientVersion", "2.20241126.01.00")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
                put("query", query)
            }
            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0.0.0 Safari/537.36")
                .header("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) return
                val root = JSONObject(raw)
                collectVideoRenderers(root, out, limit * 2)
            }
        } catch (_: Exception) {
        }
    }

    private fun searchYouTubeHtml(
        query: String,
        limit: Int,
        out: MutableList<YouTubeCandidate>
    ) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val req = Request.Builder()
                .url("https://www.youtube.com/results?search_query=$encoded")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/126.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return
                val html = resp.body?.string().orEmpty()
                val marker = "var ytInitialData = "
                val start = html.indexOf(marker)
                if (start < 0) return
                val jsonStart = start + marker.length
                val end = html.indexOf(";</script>", jsonStart)
                if (end <= jsonStart) return
                val root = JSONObject(html.substring(jsonStart, end))
                collectVideoRenderers(root, out, limit * 2)
            }
        } catch (_: Exception) {
        }
    }

    private fun searchPiped(
        query: String,
        limit: Int,
        out: MutableList<YouTubeCandidate>
    ) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        for (base in pipedInstances) {
            try {
                val req = Request.Builder()
                    .url("$base/search?q=$encoded&filter=videos")
                    .header("User-Agent", "ArunMusic/1.1.1")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string().orEmpty()
                    val obj = JSONObject(body)
                    val items = obj.optJSONArray("items") ?: return@use
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val itemUrl = item.optString("url")
                        val vid = itemUrl.substringAfter("v=", "").take(11)
                        if (!vid.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) continue
                        val dur = item.optInt("duration", 180).coerceIn(15, 7200)
                        if (dur < 25) continue
                        out.add(
                            YouTubeCandidate(
                                videoId = vid,
                                title = item.optString("title").take(140),
                                channel = item.optString("uploaderName").take(80),
                                durationSec = dur,
                                thumbnailUrl = item.optString("thumbnail").take(400)
                            )
                        )
                        if (out.size >= limit) return
                    }
                }
                if (out.isNotEmpty()) return
            } catch (_: Exception) {
            }
        }
    }

    private fun collectVideoRenderers(
        node: Any?,
        out: MutableList<YouTubeCandidate>,
        maxCount: Int
    ) {
        if (out.size >= maxCount || node == null) return
        when (node) {
            is JSONObject -> {
                val renderer = node.optJSONObject("videoRenderer")
                if (renderer != null) {
                    parseVideoRenderer(renderer)?.let { cand ->
                        // Skip ultra-short YouTube Shorts clips when resolving full songs
                        if (cand.durationSec >= 35 || out.isEmpty()) {
                            out.add(cand)
                        }
                    }
                }
                val keys = node.keys()
                while (keys.hasNext() && out.size < maxCount) {
                    val key = keys.next()
                    if (key == "videoRenderer") continue
                    collectVideoRenderers(node.opt(key), out, maxCount)
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    if (out.size >= maxCount) break
                    collectVideoRenderers(node.opt(i), out, maxCount)
                }
            }
        }
    }

    private fun parseVideoRenderer(r: JSONObject): YouTubeCandidate? {
        val videoId = r.optString("videoId").trim()
        if (!videoId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) return null

        val title = extractRunsText(r.optJSONObject("title")).ifBlank {
            r.optJSONObject("title")?.optString("simpleText").orEmpty()
        }.trim()
        if (title.isBlank()) return null

        val channel = extractRunsText(r.optJSONObject("ownerText"))
            .ifBlank { extractRunsText(r.optJSONObject("longBylineText")) }
            .ifBlank { "YouTube" }

        val lengthStr = r.optJSONObject("lengthText")?.optString("simpleText").orEmpty()
        val durationSec = parseDurationString(lengthStr)
        if (durationSec <= 0) return null // Skip live streams

        val thumbs = r.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumbUrl = if (thumbs != null && thumbs.length() > 0) {
            thumbs.optJSONObject(thumbs.length() - 1)?.optString("url").orEmpty()
        } else {
            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        }

        return YouTubeCandidate(
            videoId = videoId,
            title = title.take(140),
            channel = channel.take(80),
            durationSec = durationSec.coerceIn(15, 7200),
            thumbnailUrl = thumbUrl.take(400)
        )
    }

    private fun tryInnertubePlayer(
        videoId: String,
        cfg: InnertubeClientConfig,
        fallbackTitle: String,
        fallbackChannel: String,
        fallbackDurationSec: Int,
        fallbackThumb: String
    ): Result<YouTubeResolvedAudio> {
        return try {
            val clientObj = JSONObject().apply {
                put("clientName", cfg.clientName)
                put("clientVersion", cfg.clientVersion)
                put("hl", "en")
                put("gl", "US")
                put("osName", cfg.osName)
                put("osVersion", cfg.osVersion)
                cfg.deviceMake?.let { put("deviceMake", it) }
                cfg.deviceModel?.let { put("deviceModel", it) }
                cfg.androidSdkVersion?.let { put("androidSdkVersion", it) }
            }
            val payload = JSONObject().apply {
                put("context", JSONObject().put("client", clientObj))
                put("videoId", videoId)
                put("contentCheckOk", true)
                put("racyCheckOk", true)
            }
            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .header("User-Agent", cfg.userAgent)
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return Result.failure(IllegalStateException("HTTP ${resp.code} dari YouTube (${cfg.clientName})"))
                }
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) {
                    return Result.failure(IllegalStateException("Respon kosong dari YouTube"))
                }
                val root = JSONObject(body)
                val status = root.optJSONObject("playabilityStatus")?.optString("status").orEmpty()
                if (status.isNotBlank() && status != "OK") {
                    val reason = root.optJSONObject("playabilityStatus")?.optString("reason").orEmpty()
                    return Result.failure(IllegalStateException(reason.ifBlank { "Status putar YouTube: $status" }))
                }

                val details = root.optJSONObject("videoDetails")
                val videoTitle = details?.optString("title")?.takeIf { it.isNotBlank() } ?: fallbackTitle.ifBlank { "Lagu YouTube" }
                val author = details?.optString("author")?.takeIf { it.isNotBlank() } ?: fallbackChannel.ifBlank { "YouTube Music" }
                val durFromDetails = details?.optString("lengthSeconds")?.toIntOrNull() ?: 0

                val streamingData = root.optJSONObject("streamingData")
                    ?: return Result.failure(IllegalStateException("Streaming data tidak tersedia"))

                val bestFormat = pickBestFormat(streamingData.optJSONArray("adaptiveFormats"))
                    ?: pickBestFormat(streamingData.optJSONArray("formats"))
                    ?: return Result.failure(IllegalStateException("Format audio langsung tidak ditemukan"))

                val streamDurationSec = (bestFormat.approxDurationMs / 1000L).toInt()
                val finalDuration = when {
                    durFromDetails > 0 -> durFromDetails
                    streamDurationSec > 0 -> streamDurationSec
                    fallbackDurationSec > 0 -> fallbackDurationSec
                    else -> 210
                }.coerceIn(15, 7200)

                val thumb = fallbackThumb.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }

                Result.success(
                    YouTubeResolvedAudio(
                        videoId = videoId,
                        title = videoTitle.take(150),
                        channel = author.take(100),
                        durationSec = finalDuration,
                        thumbnailUrl = thumb,
                        streamUrl = bestFormat.url,
                        userAgent = cfg.userAgent,
                        mimeType = bestFormat.mimeType
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class PickedFormat(
        val url: String,
        val mimeType: String,
        val bitrate: Int,
        val approxDurationMs: Long
    )

    private fun pickBestFormat(arr: JSONArray?): PickedFormat? {
        if (arr == null || arr.length() == 0) return null
        val audioMp4 = mutableListOf<PickedFormat>()
        val audioOther = mutableListOf<PickedFormat>()
        val muxedMp4 = mutableListOf<PickedFormat>()

        for (i in 0 until arr.length()) {
            val f = arr.optJSONObject(i) ?: continue
            val directUrl = f.optString("url").trim()
            if (directUrl.isBlank() || !directUrl.startsWith("http")) continue
            val mime = f.optString("mimeType").lowercase()
            val bitrate = f.optInt("bitrate", 0)
            val approxMs = f.optString("approxDurationMs").toLongOrNull() ?: 0L
            val candidate = PickedFormat(directUrl, mime, bitrate, approxMs)

            when {
                mime.startsWith("audio/mp4") -> audioMp4.add(candidate)
                mime.startsWith("audio/") -> audioOther.add(candidate)
                mime.startsWith("video/mp4") -> muxedMp4.add(candidate)
            }
        }

        // Prefer AAC/MP4 audio (itag 140/139), then Opus/WebM audio, then muxed MP4 (itag 18)
        return audioMp4.maxByOrNull { it.bitrate }
            ?: audioOther.maxByOrNull { it.bitrate }
            ?: muxedMp4.minByOrNull { it.bitrate }
    }

    private fun tryPipedStreams(
        baseUrl: String,
        videoId: String,
        fallbackTitle: String,
        fallbackChannel: String,
        fallbackDurationSec: Int,
        fallbackThumb: String
    ): Result<YouTubeResolvedAudio> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/streams/$videoId")
                .header("User-Agent", "ArunMusic/1.1.1")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return Result.failure(IllegalStateException("Piped HTTP ${resp.code}"))
                val obj = JSONObject(resp.body?.string().orEmpty())
                val audioStreams = obj.optJSONArray("audioStreams")
                    ?: return Result.failure(IllegalStateException("Audio stream kosong"))

                var bestUrl = ""
                var bestMime = "audio/mp4"
                var bestBitrate = -1
                for (i in 0 until audioStreams.length()) {
                    val s = audioStreams.optJSONObject(i) ?: continue
                    val u = s.optString("url").trim()
                    if (!u.startsWith("http")) continue
                    val mime = s.optString("mimeType", "audio/mp4").lowercase()
                    val br = s.optInt("bitrate", 0)
                    val score = br + if (mime.contains("mp4") || mime.contains("m4a")) 50_000 else 0
                    if (score > bestBitrate) {
                        bestBitrate = score
                        bestUrl = u
                        bestMime = mime
                    }
                }
                if (bestUrl.isBlank()) return Result.failure(IllegalStateException("URL audio Piped kosong"))

                val dur = obj.optInt("duration", fallbackDurationSec).let {
                    if (it > 0) it else 210
                }.coerceIn(15, 7200)

                Result.success(
                    YouTubeResolvedAudio(
                        videoId = videoId,
                        title = obj.optString("title").ifBlank { fallbackTitle.ifBlank { "Lagu YouTube" } }.take(150),
                        channel = obj.optString("uploader").ifBlank { fallbackChannel.ifBlank { "YouTube" } }.take(100),
                        durationSec = dur,
                        thumbnailUrl = obj.optString("thumbnailUrl").ifBlank {
                            fallbackThumb.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }
                        },
                        streamUrl = bestUrl,
                        userAgent = "ArunMusic/1.1.1",
                        mimeType = bestMime
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun tryInvidiousStreams(
        baseUrl: String,
        videoId: String,
        fallbackTitle: String,
        fallbackChannel: String,
        fallbackDurationSec: Int,
        fallbackThumb: String
    ): Result<YouTubeResolvedAudio> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/api/v1/videos/$videoId")
                .header("User-Agent", "ArunMusic/1.1.1")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return Result.failure(IllegalStateException("Invidious HTTP ${resp.code}"))
                val obj = JSONObject(resp.body?.string().orEmpty())
                val adaptive = obj.optJSONArray("adaptiveFormats")
                var bestUrl = ""
                var bestMime = "audio/mp4"
                var bestScore = -1
                if (adaptive != null) {
                    for (i in 0 until adaptive.length()) {
                        val s = adaptive.optJSONObject(i) ?: continue
                        val type = s.optString("type").lowercase()
                        if (!type.startsWith("audio/")) continue
                        val u = s.optString("url").trim()
                        if (!u.startsWith("http")) continue
                        val br = s.optString("bitrate").toIntOrNull() ?: 0
                        val score = br + if (type.contains("mp4")) 50_000 else 0
                        if (score > bestScore) {
                            bestScore = score
                            bestUrl = u
                            bestMime = type
                        }
                    }
                }
                if (bestUrl.isBlank()) return Result.failure(IllegalStateException("Audio Invidious kosong"))
                val dur = obj.optInt("lengthSeconds", fallbackDurationSec).let {
                    if (it > 0) it else 210
                }.coerceIn(15, 7200)

                Result.success(
                    YouTubeResolvedAudio(
                        videoId = videoId,
                        title = obj.optString("title").ifBlank { fallbackTitle.ifBlank { "Lagu YouTube" } }.take(150),
                        channel = obj.optString("author").ifBlank { fallbackChannel.ifBlank { "YouTube" } }.take(100),
                        durationSec = dur,
                        thumbnailUrl = fallbackThumb.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
                        streamUrl = bestUrl,
                        userAgent = "ArunMusic/1.1.1",
                        mimeType = bestMime
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractRunsText(obj: JSONObject?): String {
        val runs = obj?.optJSONArray("runs") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            sb.append(runs.optJSONObject(i)?.optString("text").orEmpty())
        }
        return sb.toString()
    }

    private fun parseDurationString(raw: String): Int {
        val parts = raw.trim().split(":").mapNotNull { it.toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }
}
