package com.lumera.app.data.torrent

import android.content.Context
import android.util.Log
import com.lumera.app.BuildConfig
import org.libtorrent4j.AlertListener
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.alerts.Alert
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentEngine @Inject constructor() {

    private val session = SessionManager()
    private var isStarted = false

    fun start(context: Context) {
        if (isStarted) return

        // Listen for ALL alerts to diagnose listen failures
        session.addListener(object : AlertListener {
            override fun types(): IntArray? = null // null = all alert types
            override fun alert(alert: Alert<*>) {
                val type = alert.type().toString()
                val msg = alert.message()
                // Log listen-related and error alerts
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
        val params = SessionParams(settings)
        session.start(params)

        if (BuildConfig.DEBUG) Log.d("LumeraTorrent", "Endpoints after start: ${session.listenEndpoints()}")

        val savePath = File(context.getExternalFilesDir(null), "downloads")
        if (!savePath.exists()) savePath.mkdirs()

        if (BuildConfig.DEBUG) Log.d("LumeraTorrent", "Engine Started. Saving to: ${savePath.absolutePath}")
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
