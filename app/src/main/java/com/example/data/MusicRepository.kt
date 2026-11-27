package com.example.data

import android.content.Context
import com.example.R
import com.example.model.DownloadQuality
import com.example.model.EqPreset
import com.example.model.GlassIntensity
import com.example.model.ImportedTrackDoc
import com.example.model.LibraryTrackDoc
import com.example.model.MusicTrack
import com.example.model.PlayHistoryDoc
import com.example.model.PlaylistDoc
import com.example.model.StreamQuality
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MusicRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
    }

    private fun <T> Flow<T>.guardFirestoreErrors(
        userId: String,
        opType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, opType, path)
        }
        throw error
    }

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        require(userId.isNotBlank()) { "userId tidak boleh kosong" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .guardFirestoreErrors(userId, OperationType.GET, path)
        )
    }

    fun observePlaylists(userId: String): Flow<List<PlaylistDoc>> = flow {
        require(userId.isNotBlank()) { "userId tidak boleh kosong" }
        val path = "users/$userId/playlists"
        emitAll(
            db.collection("users").document(userId).collection("playlists")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PlaylistDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .guardFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeLibraryTracks(userId: String): Flow<List<LibraryTrackDoc>> = flow {
        require(userId.isNotBlank()) { "userId tidak boleh kosong" }
        val path = "users/$userId/library_tracks"
        emitAll(
            db.collection("users").document(userId).collection("library_tracks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LibraryTrackDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .guardFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observePlayHistory(userId: String): Flow<List<PlayHistoryDoc>> = flow {
        require(userId.isNotBlank()) { "userId tidak boleh kosong" }
        val path = "users/$userId/play_history"
        emitAll(
            db.collection("users").document(userId).collection("play_history")
                .orderBy("playedAt", Query.Direction.DESCENDING)
                .limit(60)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PlayHistoryDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .guardFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeImportedTracks(userId: String): Flow<List<ImportedTrackDoc>> = flow {
        require(userId.isNotBlank()) { "userId tidak boleh kosong" }
        val path = "users/$userId/imported_tracks"
        emitAll(
            db.collection("users").document(userId).collection("imported_tracks")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ImportedTrackDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .guardFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun ensureUserSeeded(userId: String, displayName: String, email: String?): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "UID pengguna tidak sesuai." }
            val userRef = db.collection("users").document(uid)
            val snap = userRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().take(80).ifEmpty { "Pendengar Arun" }
                val ytAcc = email?.take(100)?.ifBlank { "arun@youtube.com" } ?: "arun@youtube.com"
                val spAcc = "@${safeName.lowercase().replace(Regex("[^a-z0-9_]"), "_").take(24).ifEmpty { "arun_user" }}"
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "autoSync" to true,
                    "ytMusicConnected" to true,
                    "ytMusicAccount" to ytAcc,
                    "spotifyConnected" to true,
                    "spotifyAccount" to spAcc,
                    "linkedApps" to listOf("Apple Music"),
                    "streamQuality" to StreamQuality.HIGH.name,
                    "downloadQuality" to DownloadQuality.HIGH.name,
                    "glassIntensity" to GlassIntensity.BALANCED.name,
                    "eqPreset" to EqPreset.FLAT.name,
                    "eqBands" to listOf(0L, 0L, 0L, 0L, 0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userRef.set(payload).await()

                // Seed initial starter playlists & liked state once for a rich first-launch experience
                val starterPlaylists = listOf(
                    Triple("pl_starter_malam", "Refraksi Malam", listOf("trk_midnight_01", "trk_midnight_05", "trk_violet_04")),
                    Triple("pl_starter_fokus", "Fokus Liquid", listOf("trk_emerald_03", "trk_sunset_10", "trk_emerald_11"))
                )
                starterPlaylists.forEachIndexed { idx, (plId, name, trackIds) ->
                    val plData = mapOf(
                        "userId" to uid,
                        "name" to name,
                        "description" to if (idx == 0) "Kurasi otomatis malam hari lintas platform" else "Alunan ambient & lo-fi untuk bekerja",
                        "trackIds" to trackIds,
                        "coverKey" to if (idx == 0) "midnight" else "emerald",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    userRef.collection("playlists").document(plId).set(plData).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfileFields(userId: String, updates: Map<String, Any>): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "UID tidak cocok." }
            val allowedKeys = setOf(
                "displayName", "autoSync", "ytMusicConnected", "ytMusicAccount",
                "spotifyConnected", "spotifyAccount", "linkedApps", "streamQuality",
                "downloadQuality", "glassIntensity", "eqPreset", "eqBands"
            )
            val filtered = updates.filterKeys { it in allowedKeys }.toMutableMap()
            filtered["updatedAt"] = FieldValue.serverTimestamp()
            db.collection("users").document(uid).update(filtered).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(
        userId: String,
        name: String,
        description: String,
        coverKey: String,
        trackIds: List<String> = emptyList()
    ): Result<Unit> {
        val playlistId = "pl_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docPath = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Nama playlist wajib diisi." }
            val cleanDesc = description.trim().take(240)
            val payload = mapOf(
                "userId" to uid,
                "name" to cleanName,
                "description" to cleanDesc,
                "trackIds" to trackIds.take(100),
                "coverKey" to coverKey.take(40).ifBlank { "midnight" },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("playlists").document(playlistId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePlaylist(
        userId: String,
        playlistId: String,
        name: String,
        description: String,
        trackIds: List<String>
    ): Result<Unit> {
        val docPath = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(playlistId.isNotBlank())
            val cleanName = name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Nama playlist wajib diisi." }
            db.collection("users").document(uid).collection("playlists").document(playlistId)
                .update(
                    mapOf(
                        "name" to cleanName,
                        "description" to description.trim().take(240),
                        "trackIds" to trackIds.take(100),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(userId: String, playlistId: String): Result<Unit> {
        val docPath = "users/$userId/playlists/$playlistId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(playlistId.isNotBlank())
            db.collection("users").document(uid).collection("playlists").document(playlistId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertLibraryTrackFlag(
        userId: String,
        trackId: String,
        liked: Boolean? = null,
        savedAlbum: Boolean? = null,
        downloaded: Boolean? = null
    ): Result<Unit> {
        val docPath = "users/$userId/library_tracks/$trackId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(trackId.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) { "ID lagu tidak valid." }
            val ref = db.collection("users").document(uid).collection("library_tracks").document(trackId)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "trackId" to trackId,
                    "liked" to (liked ?: false),
                    "savedAlbum" to (savedAlbum ?: false),
                    "downloaded" to (downloaded ?: false),
                    "playCount" to 0L,
                    "totalListenSec" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
                if (liked != null) updates["liked"] = liked
                if (savedAlbum != null) updates["savedAlbum"] = savedAlbum
                if (downloaded != null) updates["downloaded"] = downloaded
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordPlay(userId: String, track: MusicTrack, listenedSeconds: Long): Result<Unit> {
        val entryId = "hist_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docPath = "users/$userId/play_history/$entryId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val safeDuration = listenedSeconds.coerceIn(0L, 7200L)
            val historyPayload = mapOf(
                "userId" to uid,
                "trackId" to track.id,
                "trackTitle" to track.title.take(120),
                "artistName" to track.artist.take(120),
                "genre" to track.genre.take(60),
                "platform" to track.platform.name,
                "listenedSeconds" to safeDuration,
                "playedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("play_history").document(entryId)
                .set(historyPayload).await()

            val libRef = db.collection("users").document(uid).collection("library_tracks").document(track.id)
            val libSnap = libRef.get().await()
            if (!libSnap.exists()) {
                libRef.set(
                    mapOf(
                        "userId" to uid,
                        "trackId" to track.id,
                        "liked" to false,
                        "savedAlbum" to false,
                        "downloaded" to false,
                        "playCount" to 1L,
                        "totalListenSec" to safeDuration,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                libRef.update(
                    mapOf(
                        "playCount" to FieldValue.increment(1L),
                        "totalListenSec" to FieldValue.increment(safeDuration),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearPlayHistory(userId: String, entryIds: List<String>): Result<Unit> {
        val path = "users/$userId/play_history"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val batch = db.batch()
            entryIds.take(400).forEach { id ->
                if (id.isNotBlank()) {
                    batch.delete(db.collection("users").document(uid).collection("play_history").document(id))
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveImportedSpotifyBatch(
        userId: String,
        importBatch: SpotifyImportBatch,
        saveAsPlaylist: Boolean
    ): Result<Int> {
        val basePath = "users/$userId/imported_tracks"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val userRef = db.collection("users").document(uid)
            val importedIds = mutableListOf<String>()

            importBatch.tracks.take(80).forEach { item ->
                val cleanId = item.trackId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
                if (cleanId.isNotBlank()) {
                    val docRef = userRef.collection("imported_tracks").document(cleanId)
                    val snap = docRef.get().await()
                    val safeTitle = item.title.trim().take(120).ifBlank { "Lagu Impor" }
                    val safeArtist = item.artist.trim().take(120).ifBlank { "Artis" }
                    val safeAlbum = item.album.trim().take(120).ifBlank { "Impor Musik" }
                    val safeGenre = item.genre.trim().take(60).ifBlank { "Spotify Import" }
                    val safeDuration = item.durationSec.coerceIn(10L, 7200L)
                    if (!snap.exists()) {
                        docRef.set(
                            mapOf(
                                "userId" to uid,
                                "trackId" to cleanId,
                                "title" to safeTitle,
                                "artist" to safeArtist,
                                "album" to safeAlbum,
                                "genre" to safeGenre,
                                "durationSec" to safeDuration,
                                "artworkUrl" to item.artworkUrl.take(500),
                                "previewUrl" to "",
                                "spotifyUri" to item.spotifyUri.take(200),
                                "youtubeVideoId" to item.youtubeVideoId.take(32),
                                "youtubeTitle" to item.youtubeTitle.take(160),
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } else {
                        val updateMap = mutableMapOf<String, Any>(
                            "title" to safeTitle,
                            "artist" to safeArtist,
                            "album" to safeAlbum,
                            "genre" to safeGenre,
                            "durationSec" to safeDuration,
                            "artworkUrl" to item.artworkUrl.take(500),
                            "previewUrl" to "",
                            "spotifyUri" to item.spotifyUri.take(200),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        if (item.youtubeVideoId.isNotBlank()) {
                            updateMap["youtubeVideoId"] = item.youtubeVideoId.take(32)
                            updateMap["youtubeTitle"] = item.youtubeTitle.take(160)
                        }
                        docRef.update(updateMap).await()
                    }
                    importedIds.add(cleanId)
                }
            }

            if (saveAsPlaylist && importedIds.isNotEmpty()) {
                val plId = "pl_${UUID.randomUUID().toString().replace("-", "").take(16)}"
                val plName = importBatch.suggestedPlaylistName.trim().take(80).ifBlank { "Impor Spotify" }
                userRef.collection("playlists").document(plId).set(
                    mapOf(
                        "userId" to uid,
                        "name" to plName,
                        "description" to "Diimpor dari Spotify (${importedIds.size} lagu)",
                        "trackIds" to importedIds.take(100),
                        "coverKey" to "emerald",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(importedIds.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, basePath)
            Result.failure(e)
        }
    }

    suspend fun updateImportedTrackYouTubeInfo(
        userId: String,
        trackId: String,
        youtubeVideoId: String,
        youtubeTitle: String,
        durationSec: Long
    ): Result<Unit> {
        val docPath = "users/$userId/imported_tracks/$trackId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(trackId.isNotBlank())
            db.collection("users").document(uid).collection("imported_tracks").document(trackId)
                .update(
                    mapOf(
                        "youtubeVideoId" to youtubeVideoId.take(32),
                        "youtubeTitle" to youtubeTitle.take(160),
                        "durationSec" to durationSec.coerceIn(10L, 7200L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun triggerSyncCheck(userId: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val userRef = db.collection("users").document(uid)
            try {
                userRef.get(Source.SERVER).await()
            } catch (_: Exception) {
                userRef.get(Source.DEFAULT).await()
            }
            userRef.collection("playlists").limit(5).get().await()
            userRef.collection("imported_tracks").limit(10).get().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteImportedTrack(userId: String, trackId: String): Result<Unit> {
        val docPath = "users/$userId/imported_tracks/$trackId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            require(trackId.isNotBlank())
            db.collection("users").document(uid).collection("imported_tracks").document(trackId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
