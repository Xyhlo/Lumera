package com.lumera.app.data.torrent

import android.util.Log
import com.lumera.app.BuildConfig

/**
 * LRU in-memory cache for torrent piece data.
 * Keeps recently-read pieces in RAM so playback can continue
 * seamlessly after disk truncation reclaims storage.
 */
class PieceCache(private val maxBytes: Long = MAX_CACHE_BYTES) {

    companion object {
        private const val TAG = "LumeraTorrent"
        /** 200 MB — ~50 pieces at 4 MB/piece ≈ 3+ minutes of buffer. */
        const val MAX_CACHE_BYTES = 200L * 1024 * 1024
    }

    private val map = object : LinkedHashMap<Int, ByteArray>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, ByteArray>?): Boolean {
            if (currentBytes > maxBytes && eldest != null) {
                currentBytes -= eldest.value.size
                if (BuildConfig.DEBUG) Log.v(TAG, "Cache evicted piece ${eldest.key} (${eldest.value.size / 1024}KB)")
                return true
            }
            return false
        }
    }

    private var currentBytes = 0L

    /** Most recently accessed piece index — used by cleanup logic to re-prioritize around playhead. */
    @Volatile
    var latestAccessedPiece: Int = -1
        private set

    @Synchronized
    fun get(pieceIndex: Int): ByteArray? {
        val data = map[pieceIndex]
        if (data != null) latestAccessedPiece = pieceIndex
        return data
    }

    @Synchronized
    fun put(pieceIndex: Int, data: ByteArray) {
        map.remove(pieceIndex)?.let { currentBytes -= it.size }
        currentBytes += data.size
        map[pieceIndex] = data
        latestAccessedPiece = pieceIndex
    }

    @Synchronized
    fun clear() {
        map.clear()
        currentBytes = 0
    }
}
