package com.lumera.app.data.torrent

import android.content.Context
import android.util.Log
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.SessionParams
import com.frostwire.jlibtorrent.SettingsPack
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentEngine @Inject constructor() {

    private val session = SessionManager()
    private var isStarted = false

    fun start(context: Context) {
        if (isStarted) return

        // --- FIX: Create explicit settings instead of passing null ---
        val settings = SettingsPack()
        // We can tune settings here later (e.g. limit download speed)

        val params = SessionParams(settings)
        session.start(params)
        // -------------------------------------------------------------

        // 2. Setup Save Directory
        // We use the app's internal cache so we don't need Storage Permissions
        val savePath = File(context.getExternalFilesDir(null), "downloads")
        if (!savePath.exists()) savePath.mkdirs()

        Log.d("LumeraTorrent", "Engine Started. Saving to: ${savePath.absolutePath}")
        isStarted = true
    }

    fun stop() {
        if (isStarted) {
            session.stop()
            isStarted = false
        }
    }

    fun getSession(): SessionManager {
        return session
    }

    fun getDownloadPath(context: Context): File {
        return File(context.getExternalFilesDir(null), "downloads")
    }
}