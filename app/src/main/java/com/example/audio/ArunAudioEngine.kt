package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.model.MusicTrack
import com.example.model.SynthTimbre
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(UnstableApi::class)
class ArunAudioEngine(
    context: Context,
    private val onTrackFinished: () -> Unit,
    private val onStreamError: (MusicTrack, String) -> Unit = { _, _ -> }
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playJob: Job? = null

    private val sampleRate = 22050
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack: StateFlow<MusicTrack?> = _currentTrack.asStateFlow()

    private val _positionSec = MutableStateFlow(0)
    val positionSec: StateFlow<Int> = _positionSec.asStateFlow()

    private val _volume = MutableStateFlow(0.78f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _spectrumBars = MutableStateFlow(List(16) { 0.08f })
    val spectrumBars: StateFlow<List<Float>> = _spectrumBars.asStateFlow()

    @Volatile
    private var eqGainsDb = intArrayOf(0, 0, 0, 0, 0)

    @Volatile
    private var currentSampleIndex = 0L

    private val eqCenterFreqs = floatArrayOf(60f, 230f, 910f, 3600f, 9000f)
    private val biquads = Array(5) { BiquadFilter() }

    private var exoPlayer: ExoPlayer? = null
    private var hardwareEqualizer: Equalizer? = null
    private var usingExoPlayer = false
    private var currentUserAgent: String = "ArunMusic/1.1.1"

    init {
        recalculateEqFilters()
    }

    fun updateEqBands(bands: List<Int>) {
        val normalized = IntArray(5) { idx -> bands.getOrElse(idx) { 0 }.coerceIn(-12, 12) }
        eqGainsDb = normalized
        recalculateEqFilters()
        applyHardwareEqualizerBands()
    }

    private fun recalculateEqFilters() {
        for (i in 0 until 5) {
            biquads[i].configurePeaking(
                sampleRate = sampleRate.toFloat(),
                centerFreq = eqCenterFreqs[i],
                q = 1.2f,
                gainDb = eqGainsDb[i].toFloat()
            )
        }
    }

    private fun applyHardwareEqualizerBands() {
        val eq = hardwareEqualizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            for (b in 0 until minOf(numBands, 5)) {
                val targetMb = (eqGainsDb[b] * 100).coerceIn(minMb, maxMb).toShort()
                eq.setBandLevel(b.toShort(), targetMb)
            }
        } catch (_: Throwable) {
        }
    }

    private fun attachHardwareEqualizer(audioSessionId: Int) {
        releaseHardwareEqualizer()
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return
        try {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            hardwareEqualizer = eq
            applyHardwareEqualizerBands()
        } catch (_: Throwable) {
            hardwareEqualizer = null
        }
    }

    private fun releaseHardwareEqualizer() {
        try {
            hardwareEqualizer?.release()
        } catch (_: Throwable) {
        }
        hardwareEqualizer = null
    }

    fun setVolume(vol: Float) {
        val v = vol.coerceIn(0f, 1f)
        _volume.value = v
        mainHandler.post {
            try {
                exoPlayer?.volume = v
            } catch (_: Throwable) {
            }
        }
    }

    fun prepareExternalLoading(track: MusicTrack) {
        playJob?.cancel()
        playJob = null
        releaseExoPlayerSyncSafe()
        usingExoPlayer = true
        _currentTrack.value = track
        _positionSec.value = 0
        _isPlaying.value = false
        _isBuffering.value = true
        _spectrumBars.value = List(16) { 0.14f }
    }

    fun playTrack(track: MusicTrack, startAtSec: Int = 0) {
        releaseExoPlayerSyncSafe()
        _isBuffering.value = false
        _currentTrack.value = track
        currentSampleIndex = startAtSec.coerceIn(0, track.durationSec) * sampleRate.toLong()
        _positionSec.value = startAtSec.coerceIn(0, track.durationSec)
        usingExoPlayer = false
        startAudioLoop(track)
    }

    fun playStream(
        track: MusicTrack,
        streamUrl: String,
        userAgent: String,
        startAtSec: Int = 0
    ) {
        playJob?.cancel()
        playJob = null
        _currentTrack.value = track
        _positionSec.value = startAtSec.coerceIn(0, track.durationSec.coerceAtLeast(1))
        _isBuffering.value = true
        _isPlaying.value = false

        scope.launch(Dispatchers.Main) {
            releaseExoPlayerOnMain()
            try {
                currentUserAgent = userAgent
                val httpClient = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(25, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()

                val dataSourceFactory = OkHttpDataSource.Factory(httpClient)
                    .setUserAgent(userAgent)

                val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
                val player = ExoPlayer.Builder(appContext)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setAudioAttributes(
                        Media3AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                        true
                    )
                    .build()

                exoPlayer = player
                usingExoPlayer = true
                player.volume = _volume.value

                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                _isBuffering.value = true
                            }
                            Player.STATE_READY -> {
                                _isBuffering.value = false
                                val durMs = player.duration
                                if (durMs > 0 && durMs != C.TIME_UNSET) {
                                    val durSec = (durMs / 1000L).toInt().coerceIn(15, 7200)
                                    val curr = _currentTrack.value
                                    if (curr != null && curr.id == track.id && curr.durationSec != durSec) {
                                        _currentTrack.value = curr.copy(durationSec = durSec)
                                    }
                                }
                            }
                            Player.STATE_ENDED -> {
                                _isBuffering.value = false
                                _isPlaying.value = false
                                _spectrumBars.value = List(16) { 0.06f }
                                onTrackFinished()
                            }
                            Player.STATE_IDLE -> {
                                _isBuffering.value = false
                            }
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        if (playing) {
                            startExoPlayerTicker()
                        } else if (!_isBuffering.value) {
                            _spectrumBars.value = List(16) { 0.06f }
                        }
                    }

                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        attachHardwareEqualizer(audioSessionId)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        _isBuffering.value = false
                        _isPlaying.value = false
                        _spectrumBars.value = List(16) { 0.06f }
                        onStreamError(
                            _currentTrack.value ?: track,
                            error.localizedMessage ?: "Gagal memutar stream audio YouTube"
                        )
                    }
                })

                player.setMediaItem(MediaItem.fromUri(streamUrl))
                if (startAtSec > 0) {
                    player.seekTo(startAtSec * 1000L)
                }
                player.prepare()
                player.playWhenReady = true
            } catch (e: Throwable) {
                _isBuffering.value = false
                _isPlaying.value = false
                usingExoPlayer = false
                onStreamError(track, e.localizedMessage ?: "Gagal memulai pemutar audio")
            }
        }
    }

    fun togglePlayPause() {
        val track = _currentTrack.value ?: return
        if (usingExoPlayer) {
            mainHandler.post {
                val player = exoPlayer ?: return@post
                if (player.isPlaying) {
                    player.pause()
                } else {
                    if (player.playbackState == Player.STATE_ENDED) {
                        player.seekTo(0L)
                    }
                    player.play()
                }
            }
            return
        }

        if (_isPlaying.value) {
            pause()
        } else {
            if (_positionSec.value >= track.durationSec) {
                currentSampleIndex = 0L
                _positionSec.value = 0
            }
            startAudioLoop(track)
        }
    }

    fun pause() {
        _isPlaying.value = false
        _isBuffering.value = false
        playJob?.cancel()
        playJob = null
        if (usingExoPlayer) {
            mainHandler.post {
                try {
                    exoPlayer?.pause()
                } catch (_: Throwable) {
                }
            }
        }
        _spectrumBars.value = List(16) { 0.06f }
    }

    fun seekTo(seconds: Int) {
        val track = _currentTrack.value ?: return
        val clamped = seconds.coerceIn(0, track.durationSec.coerceAtLeast(1))
        currentSampleIndex = clamped.toLong() * sampleRate
        _positionSec.value = clamped
        if (usingExoPlayer) {
            mainHandler.post {
                try {
                    exoPlayer?.seekTo(clamped * 1000L)
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun releaseExoPlayerSyncSafe() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            releaseExoPlayerOnMain()
        } else {
            mainHandler.post { releaseExoPlayerOnMain() }
        }
    }

    private fun releaseExoPlayerOnMain() {
        releaseHardwareEqualizer()
        try {
            exoPlayer?.stop()
        } catch (_: Throwable) {
        }
        try {
            exoPlayer?.release()
        } catch (_: Throwable) {
        }
        exoPlayer = null
    }

    private fun startExoPlayerTicker() {
        playJob?.cancel()
        playJob = scope.launch {
            var tick = 0
            while (isActive && usingExoPlayer) {
                val (playingNow, posSecNow) = withContext(Dispatchers.Main) {
                    val p = exoPlayer
                    if (p == null) {
                        false to _positionSec.value
                    } else {
                        val pos = (p.currentPosition / 1000L).toInt().coerceAtLeast(0)
                        p.isPlaying to pos
                    }
                }
                if (!playingNow && !_isBuffering.value) break

                val maxDur = _currentTrack.value?.durationSec?.coerceAtLeast(1) ?: 7200
                _positionSec.value = posSecNow.coerceIn(0, maxDur)

                if (playingNow) {
                    val phase = tick * 0.34
                    _spectrumBars.value = List(16) { idx ->
                        val eqBoost = 1f + (eqGainsDb[(idx * 5) / 16] / 24f)
                        val wave = 0.28f + 0.56f * abs(sin(phase + idx * 0.52).toFloat())
                        (wave * eqBoost * _volume.value.coerceAtLeast(0.3f)).coerceIn(0.10f, 1.0f)
                    }
                }
                tick++
                delay(70L)
            }
        }
    }

    private fun startAudioLoop(track: MusicTrack) {
        playJob?.cancel()
        _isPlaying.value = true
        for (b in biquads) b.reset()

        playJob = scope.launch {
            val minBufBytes = try {
                AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
            } catch (_: Throwable) {
                -1
            }

            val chunkSamples = 1024
            val audioTrack: AudioTrack? = if (minBufBytes > 0) {
                try {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(maxOf(minBufBytes, chunkSamples * 4))
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } catch (_: Throwable) {
                    null
                }
            } else null

            try {
                audioTrack?.play()
                val pcmBuffer = ShortArray(chunkSamples)
                val floatBuffer = FloatArray(chunkSamples)
                val totalSamples = track.durationSec.toLong() * sampleRate

                while (isActive && _isPlaying.value && !usingExoPlayer) {
                    if (currentSampleIndex >= totalSamples) {
                        withContext(Dispatchers.Main) {
                            _isPlaying.value = false
                            _spectrumBars.value = List(16) { 0.06f }
                            onTrackFinished()
                        }
                        break
                    }

                    val vol = _volume.value
                    for (i in 0 until chunkSamples) {
                        val sIdx = currentSampleIndex + i
                        var sample = synthesizeSample(track, sIdx)
                        for (band in 0 until 5) {
                            sample = biquads[band].process(sample)
                        }
                        val scaled = (sample * vol * 0.65f).coerceIn(-0.98f, 0.98f)
                        floatBuffer[i] = scaled
                        pcmBuffer[i] = (scaled * 32767f).toInt().toShort()
                    }

                    currentSampleIndex += chunkSamples
                    _positionSec.value = (currentSampleIndex / sampleRate).toInt().coerceAtMost(track.durationSec)
                    _spectrumBars.value = computeSpectrum16(floatBuffer, track)

                    if (audioTrack != null) {
                        audioTrack.write(pcmBuffer, 0, chunkSamples)
                    } else {
                        delay((chunkSamples * 1000L) / sampleRate)
                    }
                }
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun synthesizeSample(track: MusicTrack, sampleIndex: Long): Float {
        val t = sampleIndex.toDouble() / sampleRate
        val beatDuration = 60.0 / track.bpm
        val beatIndex = (t / beatDuration).toInt()
        val beatPhase = (t % beatDuration) / beatDuration

        val chordProg = intArrayOf(0, 5, 3, 4)
        val barIndex = (beatIndex / 4) % chordProg.size
        val rootStep = chordProg[barIndex]

        val baseFreq = midiToFreq(track.rootMidi + rootStep)
        val thirdFreq = midiToFreq(track.rootMidi + rootStep + track.scaleIntervals.getOrElse(1) { 3 })
        val fifthFreq = midiToFreq(track.rootMidi + rootStep + track.scaleIntervals.getOrElse(2) { 7 })
        val seventhFreq = midiToFreq(track.rootMidi + rootStep + track.scaleIntervals.getOrElse(3) { 10 })

        val padEnv = 0.72 + 0.28 * sin(2.0 * PI * (t / (beatDuration * 4.0)))
        val chordWave = when (track.timbre) {
            SynthTimbre.WARM_EP -> {
                val bell = exp(-beatPhase * 2.5)
                (sin(2.0 * PI * baseFreq * t) * 0.45 +
                    sin(2.0 * PI * thirdFreq * t) * 0.35 +
                    sin(2.0 * PI * fifthFreq * t) * 0.30 +
                    sin(2.0 * PI * seventhFreq * t) * 0.20 * bell) * 0.35
            }
            SynthTimbre.CRYSTAL_PLUCK -> {
                val env = exp(-beatPhase * 3.8)
                (sin(2.0 * PI * baseFreq * t) +
                    0.5 * sin(2.0 * PI * thirdFreq * t) +
                    0.35 * sin(2.0 * PI * fifthFreq * 2.0 * t)) * 0.28 * (0.35 + 0.65 * env)
            }
            SynthTimbre.VELVET_PAD -> {
                (sin(2.0 * PI * baseFreq * t) * 0.5 +
                    sin(2.0 * PI * fifthFreq * t) * 0.4 +
                    sin(2.0 * PI * seventhFreq * t) * 0.3) * 0.32 * padEnv
            }
            SynthTimbre.SUB_DREAM -> {
                (sin(2.0 * PI * baseFreq * 0.5 * t) * 0.55 +
                    sin(2.0 * PI * thirdFreq * t) * 0.35 +
                    sin(2.0 * PI * fifthFreq * t) * 0.25) * 0.36
            }
        }

        val eighthIndex = ((t / (beatDuration * 0.5)).toInt())
        val eighthPhase = (t % (beatDuration * 0.5)) / (beatDuration * 0.5)
        val noteInterval = track.scaleIntervals[eighthIndex % track.scaleIntervals.size]
        val melodyFreq = midiToFreq(track.rootMidi + 12 + noteInterval)
        val melodyEnv = exp(-eighthPhase * 5.2)
        val melody = (sin(2.0 * PI * melodyFreq * t) + 0.25 * sin(2.0 * PI * melodyFreq * 2.0 * t)) * melodyEnv * 0.28

        val bassFreq = midiToFreq(track.rootMidi - 12 + rootStep)
        val kickActive = (beatIndex % 2 == 0) && beatPhase < 0.22
        val kick = if (kickActive) {
            val pitchSweep = 115.0 * exp(-beatPhase * 22.0) + 46.0
            sin(2.0 * PI * pitchSweep * beatPhase * beatDuration) * exp(-beatPhase * 14.0) * 0.42
        } else 0.0
        val bass = sin(2.0 * PI * bassFreq * t) * exp(-beatPhase * 2.4) * 0.25

        return (chordWave + melody + kick + bass).toFloat()
    }

    private fun computeSpectrum16(samples: FloatArray, track: MusicTrack): List<Float> {
        val bins = 16
        val result = MutableList(bins) { 0f }
        val step = samples.size / bins
        for (b in 0 until bins) {
            var energy = 0f
            val start = b * step
            for (i in start until (start + step).coerceAtMost(samples.size)) {
                val s = samples[i]
                energy += s * s
            }
            val rms = sqrt(energy / step.coerceAtLeast(1))
            val eqIndex = (b * 5) / bins
            val eqBoost = 1f + (eqGainsDb[eqIndex] / 24f)
            val freqTilt = 0.85f + 0.35f * abs(sin((b + 1) * 0.65f + (track.bpm / 60f)))
            result[b] = (rms * 3.8f * eqBoost * freqTilt).coerceIn(0.08f, 1.0f)
        }
        return result
    }

    private fun midiToFreq(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

    fun release() {
        pause()
        releaseExoPlayerSyncSafe()
    }

    companion object {
        private const val OFFLINE_DIR = "arun_offline"

        suspend fun cacheTrackOffline(context: Context, track: MusicTrack): File = withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, OFFLINE_DIR).apply { mkdirs() }
            val outFile = File(dir, "${track.id}.wav")
            val sr = 22050
            val durationPreviewSec = 12
            val numSamples = sr * durationPreviewSec
            val dataBytes = numSamples * 2

            FileOutputStream(outFile).use { fos ->
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
                header.put("RIFF".toByteArray())
                header.putInt(36 + dataBytes)
                header.put("WAVE".toByteArray())
                header.put("fmt ".toByteArray())
                header.putInt(16)
                header.putShort(1)
                header.putShort(1)
                header.putInt(sr)
                header.putInt(sr * 2)
                header.putShort(2)
                header.putShort(16)
                header.put("data".toByteArray())
                header.putInt(dataBytes)
                fos.write(header.array())

                val buf = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
                val baseFreq = 440.0 * 2.0.pow((track.rootMidi - 69) / 12.0)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sr
                    val sample = (sin(2.0 * PI * baseFreq * t) * 0.4 +
                        sin(2.0 * PI * baseFreq * 1.5 * t) * 0.25) * 32767.0
                    buf.putShort(sample.toInt().coerceIn(-32767, 32767).toShort())
                    if (!buf.hasRemaining()) {
                        fos.write(buf.array())
                        buf.clear()
                    }
                }
                if (buf.position() > 0) {
                    fos.write(buf.array(), 0, buf.position())
                }
            }
            outFile
        }

        suspend fun removeOfflineTrack(context: Context, trackId: String) = withContext(Dispatchers.IO) {
            val file = File(File(context.filesDir, OFFLINE_DIR), "$trackId.wav")
            if (file.exists()) file.delete()
        }

        suspend fun getOfflineCacheBytes(context: Context): Long = withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, OFFLINE_DIR)
            if (!dir.exists()) return@withContext 0L
            dir.listFiles()?.sumOf { it.length() } ?: 0L
        }

        suspend fun clearOfflineCache(context: Context) = withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, OFFLINE_DIR)
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            }
        }

        fun formatDuration(seconds: Int): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return "%d:%02d".format(mins, secs)
        }
    }
}

private class BiquadFilter {
    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f
    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    fun configurePeaking(sampleRate: Float, centerFreq: Float, q: Float, gainDb: Float) {
        if (abs(gainDb) < 0.1f) {
            b0 = 1f; b1 = 0f; b2 = 0f; a1 = 0f; a2 = 0f
            return
        }
        val a = 10f.pow(gainDb / 40f)
        val omega = (2.0 * PI * centerFreq / sampleRate).toFloat()
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn / (2f * q)

        val rawB0 = 1f + alpha * a
        val rawB1 = -2f * cs
        val rawB2 = 1f - alpha * a
        val rawA0 = 1f + alpha / a
        val rawA1 = -2f * cs
        val rawA2 = 1f - alpha / a

        b0 = rawB0 / rawA0
        b1 = rawB1 / rawA0
        b2 = rawB2 / rawA0
        a1 = rawA1 / rawA0
        a2 = rawA2 / rawA0
    }

    fun process(x: Float): Float {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x
        y2 = y1
        y1 = y
        return y
    }

    fun reset() {
        x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
    }
}
