package com.lumera.app.data.torrent

import android.content.Context
import android.util.Log
import com.lumera.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import org.libtorrent4j.AlertListener
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.alerts.Alert
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val session = SessionManager()
    private var isStarted = false
    private val stateFile = File(context.filesDir, "session_state.dat")

    init {
        // Clean up leftover torrent files from a previous crash (normal exits clean up in TorrentService.onDestroy)
        val downloadDir = getDownloadPath()
        if (downloadDir.exists()) {
            downloadDir.deleteRecursively()
            if (BuildConfig.DEBUG) Log.d("LumeraTorrent", "Cleaned up stale downloads from previous crash")
        }

        // Pre-warm: start the session immediately so DHT bootstraps in the background
        start()
    }

    fun start() {
        if (isStarted) return

        session.addListener(object : AlertListener {
            override fun types(): IntArray? = null
            override fun alert(alert: Alert<*>) {
                val type = alert.type().toString()
                val msg = alert.message()
                if (type.contains("LISTEN", ignoreCase = true) ||
                    type.contains("ERROR", ignoreCase = true) ||
                    type.contains("DHT", ignoreCase = true) ||
                    type.contains("PORTMAP", ignoreCase = true) ||
                    msg.contains("listen", ignoreCase = true) ||
                    msg.contains("error", ignoreCase = true) ||
                    msg.contains("bind", ignoreCase = true)) {
                    if (BuildConfig.DEBUG) Log.w("LumeraTorrent", "ALERT [$type]: $msg")
                } else {
                    if (BuildConfig.DEBUG) Log.v("LumeraTorrent", "alert [$type]: $msg")
                }
            }
        })

        val settings = SettingsPack().apply {
            setEnableDht(true)
            setDhtBootstrapNodes(
                "router.bittorrent.com:6881," +
                "router.utorrent.com:6881," +
                "dht.transmissionbt.com:6881," +
                "dht.aelitis.com:6881"
            )
            connectionsLimit(200)
            activeDownloads(1)
            activeSeeds(1)
        }

        // Try restoring saved session state (DHT routing table) for faster bootstrap
        val params = try {
            if (stateFile.exists()) {
                val saved = stateFile.readBytes()
                if (BuildConfig.DEBUG) Log.d("LumeraTorrent", "Restoring session state (${saved.size} bytes)")
                SessionParams(saved)
            } else {
                SessionParams(settings)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("LumeraTorrent", "Failed to restore session state, starting fresh", e)
            stateFile.delete()
            SessionParams(settings)
        }

        session.start(params)

        // Always apply our settings on top — ensures current config even when restoring old state
        session.applySettings(settings)

        if (BuildConfig.DEBUG) Log.d("LumeraTorrent", "Endpoints after start: ${session.listenEndpoints()}")

        val savePath = File(context.getExternalFilesDir(null), "downloads")
        if (!savePath.exists()) savePath.mkdirs()

        if (BuildConfig.DEBUG) Log.d("LumeraTorrent", "Engine Started. Saving to: ${savePath.absolutePath}")
        isStarted = true
    }

    fun getSession(): SessionManager {
        start() // Ensure started
        return session
    }

    fun isDhtWarmed(): Boolean {
        return isStarted && session.isDhtRunning() &&
                session.listenEndpoints().isNotEmpty() &&
                session.stats().dhtNodes() > 0
    }

    fun saveState() {
        if (!isStarted) return
        try {
            val state = session.saveState()
            stateFile.writeBytes(state)
            if (BuildConfig.DEBUG) Log.d("LumeraTorrent", "Saved session state (${state.size} bytes, dhtNodes=${session.stats().dhtNodes()})")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("LumeraTorrent", "Failed to save session state", e)
        }
    }

    fun getDownloadPath(): File {
        return File(context.getExternalFilesDir(null), "downloads")
    }
}
