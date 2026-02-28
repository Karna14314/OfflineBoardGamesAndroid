package com.offlinegames.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Lightweight sound manager using [SoundPool] for minimal latency.
 *
 * Loads a small set of PCM sound effects into memory at construction.
 * All sounds are played on the main or render thread without blocking.
 *
 * Usage:
 * ```kotlin
 * val sounds = SoundManager(context)
 * sounds.playClick()
 * sounds.release() // call in onDestroy
 * ```
 *
 * Note: actual sound files (click.ogg, win.ogg) must be placed in
 * `app/src/main/res/raw/`. The manager silently skips missing files.
 */
class SoundManager(context: Context) {

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(audioAttributes)
        .build()

    // Sound IDs — 0 means not loaded (file absent)
    private val clickSoundId: Int = loadRaw(context, "click")
    private val winSoundId:   Int = loadRaw(context, "win")

    /** Play the short click sound on valid move. */
    fun playClick() = play(clickSoundId)

    /** Play the win fanfare on game end. */
    fun playWin() = play(winSoundId)

    /** Release native SoundPool resources. Call from Activity.onDestroy(). */
    fun release() = soundPool.release()

    // ── Internals ────────────────────────────────────────────────────────────

    private fun play(soundId: Int) {
        if (soundId == 0) return
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private fun loadRaw(context: Context, name: String): Int {
        val resId = context.resources.getIdentifier(name, "raw", context.packageName)
        return if (resId != 0) soundPool.load(context, resId, 1) else 0
    }
}
