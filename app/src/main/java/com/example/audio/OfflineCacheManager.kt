package com.example.audio

import android.content.Context
import com.example.model.MusicTrack

class OfflineCacheManager(context: Context) {
    private val appContext = context.applicationContext

    suspend fun downloadTrack(track: MusicTrack): Long {
        ArunAudioEngine.cacheTrackOffline(appContext, track)
        return ArunAudioEngine.getOfflineCacheBytes(appContext)
    }

    suspend fun removeTrack(trackId: String): Long {
        ArunAudioEngine.removeOfflineTrack(appContext, trackId)
        return ArunAudioEngine.getOfflineCacheBytes(appContext)
    }

    suspend fun getCacheSizeBytes(): Long {
        return ArunAudioEngine.getOfflineCacheBytes(appContext)
    }

    suspend fun clearAll(): Long {
        ArunAudioEngine.clearOfflineCache(appContext)
        return ArunAudioEngine.getOfflineCacheBytes(appContext)
    }
}
