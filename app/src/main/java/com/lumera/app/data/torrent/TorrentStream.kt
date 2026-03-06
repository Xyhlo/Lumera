package com.lumera.app.data.torrent

import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo

/**
 * Maps a file within a torrent to its piece range.
 * Used by TorrentInputStream to convert byte offsets to piece indices.
 */
data class TorrentStream(
    val handle: TorrentHandle,
    val fileIndex: Int,
    val fileOffset: Long,
    val fileSize: Long,
    val pieceLength: Int,
    val firstPiece: Int,
    val lastPiece: Int
) {
    val numPieces: Int get() = lastPiece - firstPiece + 1

    fun pieceIndexForOffset(fileByteOffset: Long): Int {
        val absoluteOffset = fileOffset + fileByteOffset
        return (absoluteOffset / pieceLength).toInt()
    }

    companion object {
        fun create(
            handle: TorrentHandle,
            torrentInfo: TorrentInfo,
            fileIndex: Int
        ): TorrentStream {
            val files = torrentInfo.files()
            val fileOffset = files.fileOffset(fileIndex)
            val fileSize = files.fileSize(fileIndex)
            val pieceLength = torrentInfo.pieceLength()
            val firstPiece = (fileOffset / pieceLength).toInt()
            val lastPiece = ((fileOffset + fileSize - 1) / pieceLength).toInt()

            return TorrentStream(
                handle = handle,
                fileIndex = fileIndex,
                fileOffset = fileOffset,
                fileSize = fileSize,
                pieceLength = pieceLength,
                firstPiece = firstPiece,
                lastPiece = lastPiece
            )
        }
    }
}
