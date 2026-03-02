package com.lumera.app.data.torrent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.AddTorrentParams
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class TorrentService : Service() {

    @Inject lateinit var engine: TorrentEngine

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var streamProxy: StreamProxy? = null

    companion object {
        var onStreamReady: ((String) -> Unit)? = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val magnetLink = intent?.getStringExtra("MAGNET_LINK") ?: return START_NOT_STICKY

        try {
            startForegroundService()
            engine.start(this)
            startDownload(magnetLink)
        } catch (e: Exception) {
            Log.e("LumeraTorrent", "Critical Error starting service: ${e.message}")
            stopSelf() // Stop correctly if we crash
        }

        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "torrent_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Torrent Download", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Lumera Streaming")
            .setContentText("Downloading metadata...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .build()

        // --- FIX FOR ANDROID 14 CRASH ---
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }
        // --------------------------------
    }

    private fun startDownload(magnet: String) {
        scope.launch {
            Log.d("LumeraTorrent", "Adding magnet...")

            try {
                val session = engine.getSession()
                val saveDir = engine.getDownloadPath(this@TorrentService)

                // 1. Parse Magnet
                val params = AddTorrentParams.parseMagnetUri(magnet)
                val infoHash = params.infoHash()

                // 2. Start Download
                session.download(magnet, saveDir)

                // 3. Find Handle
                var handle: com.frostwire.jlibtorrent.TorrentHandle? = null
                var attempts = 0

                while (handle == null && attempts < 20) {
                    handle = session.find(infoHash)
                    delay(500)
                    attempts++
                }

                if (handle == null) {
                    Log.e("LumeraTorrent", "Failed to get handle.")
                    stopSelf()
                    return@launch
                }

                Log.d("LumeraTorrent", "Metadata wait...")

                // 4. Wait for Metadata
                attempts = 0
                while (!handle.status().hasMetadata() && attempts < 60) {
                    delay(1000)
                    attempts++
                }

                if (!handle.status().hasMetadata()) {
                    Log.e("LumeraTorrent", "No metadata found.")
                    stopSelf()
                    return@launch
                }

                // 5. Select Largest File
                val torrentInfo = handle.torrentFile()
                val numFiles = torrentInfo.numFiles()
                var largestFileIndex = 0
                var largestFileSize = 0L

                for (i in 0 until numFiles) {
                    if (torrentInfo.files().fileSize(i) > largestFileSize) {
                        largestFileSize = torrentInfo.files().fileSize(i)
                        largestFileIndex = i
                    }
                }

                // 6. Set Priority
                val priorities = Array(numFiles) { Priority.IGNORE }
                priorities[largestFileIndex] = Priority.SEVEN
                handle.prioritizeFiles(priorities)

                // (Sequential download line removed to prevent errors)

                // 7. Start Proxy
                val movieFile = File(saveDir, torrentInfo.files().filePath(largestFileIndex))
                stopProxy()
                val proxy = StreamProxy(movieFile)
                proxy.start()
                streamProxy = proxy

                // 8. Ready!
                val localUrl = "http://127.0.0.1:${proxy.listeningPort}"

                launch(Dispatchers.Main) {
                    onStreamReady?.invoke(localUrl)
                }
            } catch (e: Exception) {
                Log.e("LumeraTorrent", "Error inside download loop: ${e.message}")
                stopSelf()
            }
        }
    }

    private fun stopProxy() {
        try {
            streamProxy?.stop()
            streamProxy = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        stopProxy()
        try { engine.stop() } catch (e: Exception) {}
        job.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}