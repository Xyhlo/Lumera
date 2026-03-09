package com.lumera.app.data.torrent

import android.util.Log
import com.lumera.app.BuildConfig
import org.libtorrent4j.Priority
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * Blocking InputStream backed by an in-memory piece cache.
 * Reads full pieces from disk (after verifying download via havePiece()),
 * caches them in [PieceCache], and serves byte ranges from the cached data.
 * After disk truncation, the cache provides seamless playback while
 * libtorrent re-downloads current pieces.
 */
class TorrentInputStream(
    private val file: File,
    private val stream: TorrentStream,
    private val cache: PieceCache,
    startOffset: Long = 0L,
    length: Long = -1L
) : InputStream() {

    private var raf: RandomAccessFile? = null
    private var position: Long = startOffset
    private var remaining: Long = if (length > 0) length else (stream.fileSize - startOffset)

    companion object {
        private const val TAG = "LumeraTorrent"
        private const val PIECE_WAIT_POLL_MS = 200L
        private const val PIECE_WAIT_TIMEOUT_MS = 90_000L
        private const val FILE_WAIT_TIMEOUT_MS = 60_000L
        private const val LOOKAHEAD = 15
        private const val DEADLINE_MS = 1000
        /** Pieces behind playhead kept available for short rewinds (~20 MB at 4 MB/piece). */
        private const val TRAIL_KEEP = 5
    }

    /** Tracks the trailing edge so we only de-prioritize the delta, not a growing loop. */
    @Volatile
    private var lastTrailEdge = -1

    init {
        // Wait for the file to exist on disk before opening
        val deadline = System.currentTimeMillis() + FILE_WAIT_TIMEOUT_MS
        while (!file.exists()) {
            if (System.currentTimeMillis() >= deadline) {
                throw IOException("Timeout waiting for torrent file to appear: ${file.name}")
            }
            Thread.sleep(PIECE_WAIT_POLL_MS)
        }
        raf = RandomAccessFile(file, "r")
    }

    override fun read(): Int {
        if (remaining <= 0) return -1
        val buf = ByteArray(1)
        val n = read(buf, 0, 1)
        return if (n == 1) buf[0].toInt() and 0xFF else -1
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (remaining <= 0) return -1

        val pieceIndex = stream.pieceIndexForOffset(position)
        val pieceData = readPieceData(pieceIndex)

        // Calculate offset within this piece
        val pieceStartInFile = (pieceIndex.toLong() * stream.pieceLength) - stream.fileOffset
        val offsetInPiece = (position - pieceStartInFile).toInt()
        val availableInPiece = pieceData.size - offsetInPiece
        val toRead = len.toLong().coerceAtMost(remaining).coerceAtMost(availableInPiece.toLong()).toInt()

        if (toRead <= 0) return -1

        System.arraycopy(pieceData, offsetInPiece, b, off, toRead)
        position += toRead
        remaining -= toRead
        return toRead
    }

    override fun close() {
        raf?.close()
        raf = null
    }

    /**
     * Returns the full piece data for [pieceIndex], serving from cache if available,
     * otherwise waiting for the piece to download, reading it from disk, and caching it.
     */
    private fun readPieceData(pieceIndex: Int): ByteArray {
        // 1. Check cache first — critical after disk truncation when havePiece() returns false
        cache.get(pieceIndex)?.let { return it }

        // 2. Wait for the piece to be available on disk (sets priorities, deadlines, de-prioritizes trail)
        waitForPiece(pieceIndex)

        // 3. Read the full piece from disk
        val pieceStartAbsolute = pieceIndex.toLong() * stream.pieceLength
        val pieceStartInFile = pieceStartAbsolute - stream.fileOffset
        // Last piece may be smaller than pieceLength
        val pieceEnd = ((pieceIndex + 1).toLong() * stream.pieceLength) - stream.fileOffset
        val pieceSize = (pieceEnd.coerceAtMost(stream.fileSize) - pieceStartInFile).toInt()

        val data = ByteArray(pieceSize)
        val currentRaf = ensureRaf()
        synchronized(currentRaf) {
            currentRaf.seek(pieceStartInFile)
            var read = 0
            while (read < pieceSize) {
                val n = currentRaf.read(data, read, pieceSize - read)
                if (n < 0) break
                read += n
            }
        }

        // 4. Cache it
        cache.put(pieceIndex, data)
        return data
    }

    /**
     * Ensures the RandomAccessFile is open. After disk truncation the file may have
     * been recreated, so we re-open if the previous handle was closed or stale.
     */
    private fun ensureRaf(): RandomAccessFile {
        raf?.let { return it }
        val newRaf = RandomAccessFile(file, "r")
        raf = newRaf
        return newRaf
    }

    /**
     * Block until the piece at [pieceIndex] is available on disk.
     * Also sets priorities and deadlines for lookahead pieces, and
     * de-prioritizes pieces behind the playhead to prevent unbounded downloading.
     */
    private fun waitForPiece(pieceIndex: Int) {
        // De-prioritize pieces behind the playhead to prevent unbounded downloading.
        val newTrailEdge = (pieceIndex - TRAIL_KEEP).coerceAtLeast(stream.firstPiece)
        val prevTrailEdge = lastTrailEdge.coerceAtLeast(stream.firstPiece)
        if (newTrailEdge > prevTrailEdge) {
            for (idx in prevTrailEdge until newTrailEdge) {
                try {
                    stream.handle.piecePriority(idx, Priority.IGNORE)
                } catch (_: Exception) { break }
            }
            lastTrailEdge = newTrailEdge
            if (BuildConfig.DEBUG && newTrailEdge - prevTrailEdge > 0) {
                Log.v(TAG, "De-prioritized pieces $prevTrailEdge..<$newTrailEdge (playhead=$pieceIndex)")
            }
        }

        // Set lookahead priorities + deadlines
        for (i in 0 until LOOKAHEAD) {
            val idx = pieceIndex + i
            if (idx > stream.lastPiece) break
            try {
                if (!stream.handle.havePiece(idx)) {
                    stream.handle.piecePriority(idx, Priority.TOP_PRIORITY)
                    stream.handle.setPieceDeadline(idx, DEADLINE_MS * (i + 1))
                }
            } catch (e: Exception) {
                throw IOException("Torrent handle invalidated", e)
            }
        }

        // Already have it — fast path
        try {
            if (stream.handle.havePiece(pieceIndex)) return
        } catch (e: Exception) {
            throw IOException("Torrent handle invalidated", e)
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "Waiting for piece $pieceIndex")

        val deadline = System.currentTimeMillis() + PIECE_WAIT_TIMEOUT_MS
        while (true) {
            try {
                if (stream.handle.havePiece(pieceIndex)) return
            } catch (e: Exception) {
                throw IOException("Torrent handle invalidated", e)
            }
            if (System.currentTimeMillis() >= deadline) {
                if (BuildConfig.DEBUG) Log.w(TAG, "Timeout waiting for piece $pieceIndex")
                throw IOException("Timeout waiting for torrent piece $pieceIndex")
            }
            Thread.sleep(PIECE_WAIT_POLL_MS)
        }
    }
}
