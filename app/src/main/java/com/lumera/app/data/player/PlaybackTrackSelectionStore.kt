package com.lumera.app.data.player

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackTrackSelectionStore @Inject constructor(
    @ApplicationContext context: Context
) {
    data class Selection(
        val audioTrackId: String?,
        val subtitleTrackId: String?,
        val subtitleVerticalOffsetPercent: Int? = null,
        val subtitleSizePercent: Int? = null,
        val subtitleDelayMs: Long? = null
    )

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun getSelection(playbackId: String): Selection? {
        val scopedId = canonicalPlaybackId(playbackId) ?: return null
        val audioTrackId = prefs.getString(audioKey(scopedId), null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val subtitleTrackId = prefs.getString(subtitleKey(scopedId), null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val subtitleOffset = if (prefs.contains(subtitleOffsetKey(scopedId))) {
            prefs.getInt(subtitleOffsetKey(scopedId), 0)
        } else {
            null
        }
        val subtitleSize = if (prefs.contains(subtitleSizeKey(scopedId))) {
            prefs.getInt(subtitleSizeKey(scopedId), 100)
        } else {
            null
        }
        val subtitleDelay = if (prefs.contains(subtitleDelayKey(scopedId))) {
            prefs.getLong(subtitleDelayKey(scopedId), 0L)
        } else {
            null
        }
        if (audioTrackId == null && subtitleTrackId == null && subtitleOffset == null && subtitleSize == null && subtitleDelay == null) return null
        return Selection(
            audioTrackId = audioTrackId,
            subtitleTrackId = subtitleTrackId,
            subtitleVerticalOffsetPercent = subtitleOffset,
            subtitleSizePercent = subtitleSize,
            subtitleDelayMs = subtitleDelay
        )
    }

    fun updateSelection(
        playbackId: String,
        audioTrackId: String?,
        subtitleTrackId: String?,
        subtitleVerticalOffsetPercent: Int? = null,
        subtitleSizePercent: Int? = null,
        subtitleDelayMs: Long? = null,
        updateAudio: Boolean,
        updateSubtitle: Boolean,
        updateSubtitleOffset: Boolean = false,
        updateSubtitleSize: Boolean = false,
        updateSubtitleDelay: Boolean = false
    ) {
        val scopedId = canonicalPlaybackId(playbackId) ?: return
        if (!updateAudio && !updateSubtitle && !updateSubtitleOffset && !updateSubtitleSize && !updateSubtitleDelay) return

        prefs.edit().apply {
            if (updateAudio) {
                val normalizedAudio = audioTrackId?.trim()?.takeIf { it.isNotEmpty() }
                if (normalizedAudio == null) {
                    remove(audioKey(scopedId))
                } else {
                    putString(audioKey(scopedId), normalizedAudio)
                }
            }
            if (updateSubtitle) {
                val normalizedSubtitle = subtitleTrackId?.trim()?.takeIf { it.isNotEmpty() }
                if (normalizedSubtitle == null) {
                    remove(subtitleKey(scopedId))
                } else {
                    putString(subtitleKey(scopedId), normalizedSubtitle)
                }
            }
            if (updateSubtitleOffset) {
                val offset = subtitleVerticalOffsetPercent
                if (offset == null || offset == 0) {
                    remove(subtitleOffsetKey(scopedId))
                } else {
                    putInt(subtitleOffsetKey(scopedId), offset)
                }
            }
            if (updateSubtitleSize) {
                val size = subtitleSizePercent
                if (size == null || size == 100) {
                    remove(subtitleSizeKey(scopedId))
                } else {
                    putInt(subtitleSizeKey(scopedId), size)
                }
            }
            if (updateSubtitleDelay) {
                val delay = subtitleDelayMs
                if (delay == null || delay == 0L) {
                    remove(subtitleDelayKey(scopedId))
                } else {
                    putLong(subtitleDelayKey(scopedId), delay)
                }
            }
        }.apply()
    }

    fun clearSelection(playbackId: String) {
        val scopedId = canonicalPlaybackId(playbackId) ?: return
        prefs.edit()
            .remove(audioKey(scopedId))
            .remove(subtitleKey(scopedId))
            .remove(subtitleOffsetKey(scopedId))
            .remove(subtitleSizeKey(scopedId))
            .remove(subtitleDelayKey(scopedId))
            .apply()
    }

    fun clearSelectionsForPrefix(prefix: String) {
        val editor = prefs.edit()
        prefs.all.keys.forEach { key ->
            if (key.startsWith("${KEY_AUDIO_PREFIX}$prefix") ||
                key.startsWith("${KEY_SUBTITLE_PREFIX}$prefix") ||
                key.startsWith("${KEY_SUBTITLE_OFFSET_PREFIX}$prefix") ||
                key.startsWith("${KEY_SUBTITLE_SIZE_PREFIX}$prefix") ||
                key.startsWith("${KEY_SUBTITLE_DELAY_PREFIX}$prefix")
            ) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    private fun canonicalPlaybackId(playbackId: String): String? {
        return playbackId.trim().takeIf { it.isNotEmpty() }
    }

    private fun audioKey(scopedId: String): String = "${KEY_AUDIO_PREFIX}$scopedId"

    private fun subtitleKey(scopedId: String): String = "${KEY_SUBTITLE_PREFIX}$scopedId"

    private fun subtitleOffsetKey(scopedId: String): String = "${KEY_SUBTITLE_OFFSET_PREFIX}$scopedId"

    private fun subtitleSizeKey(scopedId: String): String = "${KEY_SUBTITLE_SIZE_PREFIX}$scopedId"

    private fun subtitleDelayKey(scopedId: String): String = "${KEY_SUBTITLE_DELAY_PREFIX}$scopedId"

    companion object {
        private const val PREFS_FILE = "playback_track_selection_prefs"
        private const val KEY_AUDIO_PREFIX = "audio_"
        private const val KEY_SUBTITLE_PREFIX = "subtitle_"
        private const val KEY_SUBTITLE_OFFSET_PREFIX = "subtitleOffset_"
        private const val KEY_SUBTITLE_SIZE_PREFIX = "subtitleSize_"
        private const val KEY_SUBTITLE_DELAY_PREFIX = "subtitleDelay_"
    }
}
