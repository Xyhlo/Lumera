package com.lumera.app.data.torrent

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile

class StreamProxy(
    private val file: File
) : NanoHTTPD("127.0.0.1", 0) {

    override fun serve(session: IHTTPSession): Response {
        if (!file.exists() || !file.canRead()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
        }

        val fileLength = file.length()
        val mimeType = inferMimeType(file.name)
        val rangeHeader = session.headers["range"]

        return try {
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                servePartialContent(rangeHeader, fileLength, mimeType)
            } else {
                serveFullContent(fileLength, mimeType)
            }
        } catch (e: Exception) {
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Stream error")
        }
    }

    private fun serveFullContent(fileLength: Long, mimeType: String): Response {
        val fis = FileInputStream(file)
        return newFixedLengthResponse(Response.Status.OK, mimeType, fis, fileLength).apply {
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
        val raf = RandomAccessFile(file, "r")
        raf.seek(start)
        val rangeStream = BoundedInputStream(raf, contentLength)

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

    private class BoundedInputStream(
        private val raf: RandomAccessFile,
        private var remaining: Long
    ) : java.io.InputStream() {

        override fun read(): Int {
            if (remaining <= 0) return -1
            val b = raf.read()
            if (b >= 0) remaining--
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (remaining <= 0) return -1
            val toRead = len.toLong().coerceAtMost(remaining).toInt()
            val bytesRead = raf.read(b, off, toRead)
            if (bytesRead > 0) remaining -= bytesRead
            return bytesRead
        }

        override fun close() {
            raf.close()
        }
    }
}
