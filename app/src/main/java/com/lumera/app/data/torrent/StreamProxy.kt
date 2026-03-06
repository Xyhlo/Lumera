package com.lumera.app.data.torrent

import android.util.Log
import com.lumera.app.BuildConfig
import fi.iki.elonen.NanoHTTPD
import java.io.File

class StreamProxy(
    private val file: File,
    private val stream: TorrentStream
) : NanoHTTPD("127.0.0.1", 0) {

    override fun serve(session: IHTTPSession): Response {
        val fileLength = stream.fileSize
        val mimeType = inferMimeType(file.name)
        val rangeHeader = session.headers["range"]

        return try {
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                servePartialContent(rangeHeader, fileLength, mimeType)
            } else {
                serveFullContent(fileLength, mimeType)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e("LumeraTorrent", "StreamProxy serve error: ${e.message}")
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Stream error")
        }
    }

    private fun serveFullContent(fileLength: Long, mimeType: String): Response {
        val tis = TorrentInputStream(file, stream)
        return newFixedLengthResponse(Response.Status.OK, mimeType, tis, fileLength).apply {
            addHeader("Accept-Ranges", "bytes")
            addHeader("Content-Length", fileLength.toString())
        }
    }

    private fun servePartialContent(
        rangeHeader: String,
        fileLength: Long,
        mimeType: String
    ): Response {
        val rangeSpec = rangeHeader.removePrefix("bytes=").trim()
        val dashIndex = rangeSpec.indexOf('-')
        if (dashIndex < 0) {
            return newFixedLengthResponse(
                Response.Status.RANGE_NOT_SATISFIABLE,
                MIME_PLAINTEXT,
                "Invalid range"
            )
        }

        val startStr = rangeSpec.substring(0, dashIndex).trim()
        val endStr = rangeSpec.substring(dashIndex + 1).trim()

        val start: Long
        val end: Long

        if (startStr.isNotEmpty()) {
            start = startStr.toLongOrNull() ?: return newFixedLengthResponse(
                Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "Invalid range"
            )
            end = if (endStr.isNotEmpty()) {
                endStr.toLongOrNull()?.coerceAtMost(fileLength - 1) ?: (fileLength - 1)
            } else {
                fileLength - 1
            }
        } else if (endStr.isNotEmpty()) {
            val suffixLength = endStr.toLongOrNull() ?: return newFixedLengthResponse(
                Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "Invalid range"
            )
            start = (fileLength - suffixLength).coerceAtLeast(0L)
            end = fileLength - 1
        } else {
            return newFixedLengthResponse(
                Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "Invalid range"
            )
        }

        if (start > end || start >= fileLength) {
            return newFixedLengthResponse(
                Response.Status.RANGE_NOT_SATISFIABLE, MIME_PLAINTEXT, "Range not satisfiable"
            ).apply {
                addHeader("Content-Range", "bytes */$fileLength")
            }
        }

        val contentLength = end - start + 1
        val rangeStream = TorrentInputStream(file, stream, startOffset = start, length = contentLength)

        return newFixedLengthResponse(
            Response.Status.PARTIAL_CONTENT, mimeType, rangeStream, contentLength
        ).apply {
            addHeader("Accept-Ranges", "bytes")
            addHeader("Content-Range", "bytes $start-$end/$fileLength")
            addHeader("Content-Length", contentLength.toString())
        }
    }

    private fun inferMimeType(filename: String): String {
        val lower = filename.lowercase()
        return when {
            lower.endsWith(".mkv") -> "video/x-matroska"
            lower.endsWith(".mp4") || lower.endsWith(".m4v") -> "video/mp4"
            lower.endsWith(".avi") -> "video/x-msvideo"
            lower.endsWith(".webm") -> "video/webm"
            lower.endsWith(".ts") -> "video/mp2t"
            else -> "video/mp4"
        }
    }
}
