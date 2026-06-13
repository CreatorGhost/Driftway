package com.kododake.aabrowser.media

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

/**
 * Owns the app's [MediaSessionCompat] and audio focus so HTML5 media playing in the WebView can
 * keep playing when the app is backgrounded — including when driving starts and the car host
 * hides the UI. The session is intentionally decoupled from any single WebView/tab: playback
 * lives in the page, and native transport actions are routed back to the page via [callback].
 *
 * Per the Android Auto driver-distraction model, video frames must stop while moving but AUDIO
 * may continue (the YouTube Music / Spotify model). This controller is what makes that possible.
 */
class MediaSessionController(
    private val context: Context,
    private val callback: TransportCallback
) {
    interface TransportCallback {
        fun onPlay()
        fun onPause()
        fun onStopRequested()
        fun onSeekTo(positionMs: Long)
    }

    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var wasTransientlyPaused = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss (another app took over): pause and release focus so a later
                // play re-requests it — otherwise hasAudioFocus stays true and we'd play without
                // actually holding focus.
                wasTransientlyPaused = false
                callback.onPause()
                abandonAudioFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Short interruption (e.g. a call): pause but keep focus so we can auto-resume.
                wasTransientlyPaused = true
                callback.onPause()
            }
            // The system ducks us automatically for short nav prompts; keep playing.
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> { /* no-op */ }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Only auto-resume if WE paused for a transient loss — not if the user paused.
                if (wasTransientlyPaused) {
                    wasTransientlyPaused = false
                    callback.onPlay()
                }
            }
        }
    }

    private val session = MediaSessionCompat(context, "AABrowserMedia").apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() = callback.onPlay()
            override fun onPause() = callback.onPause()
            override fun onStop() = callback.onStopRequested()
            override fun onSeekTo(pos: Long) = callback.onSeekTo(pos)
        })
    }

    val sessionToken: MediaSessionCompat.Token get() = session.sessionToken

    /** @return true if playback actually started (audio focus granted), false if denied. */
    fun onPlaybackStarted(positionMs: Long): Boolean {
        // If focus is denied (e.g. a phone call holds it), don't go PLAYING / start the FGS — tell
        // the page to pause so we never play over another app or create conflicting media state.
        if (!requestAudioFocus()) {
            callback.onPause()
            return false
        }
        session.isActive = true
        setState(PlaybackStateCompat.STATE_PLAYING, positionMs)
        MediaPlaybackService.start(context, sessionToken)
        return true
    }

    /** Lightweight position refresh for an already-playing session (no focus/service churn). */
    fun onPlaybackProgress(positionMs: Long) {
        setState(PlaybackStateCompat.STATE_PLAYING, positionMs)
    }

    fun onPlaybackPaused(positionMs: Long) {
        setState(PlaybackStateCompat.STATE_PAUSED, positionMs)
        // Session stays active and the (paused) notification stays so the user can resume.
    }

    fun onPlaybackStopped() {
        // Setting STOPPED is observed by MediaPlaybackService, which then tears itself down.
        setState(PlaybackStateCompat.STATE_STOPPED, 0)
        session.isActive = false
        abandonAudioFocus()
    }

    private var lastTitle: String? = null
    private var lastArtist: String? = null
    private var lastDurationMs: Long = -1L

    fun updateMetadata(title: String?, artist: String?, artwork: Bitmap?, durationMs: Long) {
        // Defense-in-depth against per-tick churn: skip if nothing meaningful changed.
        if (artwork == null && title == lastTitle && artist == lastArtist &&
            durationMs == lastDurationMs
        ) {
            return
        }
        lastTitle = title
        lastArtist = artist
        lastDurationMs = durationMs
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title.orEmpty())
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist.orEmpty())
        if (durationMs > 0) {
            builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
        }
        if (artwork != null) {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
        }
        session.setMetadata(builder.build())
    }

    private fun setState(state: Int, positionMs: Long) {
        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SEEK_TO
        // Playback speed must be 0 for non-playing states so media UIs don't advance the
        // displayed position while paused/stopped.
        val speed = if (state == PlaybackStateCompat.STATE_PLAYING) 1f else 0f
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, positionMs, speed, SystemClock.elapsedRealtime())
                .build()
        )
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(focusListener)
            .setWillPauseWhenDucked(false)
            .build()
        audioFocusRequest = request
        hasAudioFocus =
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
        hasAudioFocus = false
    }

    fun release() {
        // Releasing the session fires onSessionDestroyed on the service, which stops itself.
        abandonAudioFocus()
        session.isActive = false
        session.release()
    }
}
