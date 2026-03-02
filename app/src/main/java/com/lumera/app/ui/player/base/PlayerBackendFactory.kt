package com.lumera.app.ui.player.base

import android.content.Context

object PlayerBackendFactory {
    fun create(
        context: Context,
        backendType: PlayerBackendType,
        playbackSettings: PlaybackSettings = PlaybackSettings()
    ): PlayerRuntime {
        return when (backendType) {
            PlayerBackendType.EXOPLAYER -> {
                val backend = ExoPlayerBackend(context.applicationContext, playbackSettings)
                PlayerRuntime(
                    playbackController = backend,
                    renderSurface = backend
                )
            }
        }
    }
}
