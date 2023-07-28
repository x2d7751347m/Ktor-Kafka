package com.x2d7751347m.plugins

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

object ImageUtil {
    fun compressImage(data: ByteArray): ByteArray {
        val deflater = Deflater()
        deflater.setLevel(Deflater.BEST_COMPRESSION)
        deflater.setInput(data)
        deflater.finish()
        val outputStream = ByteArrayOutputStream(data.size)
        val tmp = ByteArray(4 * 1024)
        while (!deflater.finished()) {
            val size: Int = deflater.deflate(tmp)
            outputStream.write(tmp, 0, size)
        }
        try {
            outputStream.close()
        } catch (_: Exception) {
        }
        return outputStream.toByteArray()
    }

    fun decompressImage(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val outputStream = ByteArrayOutputStream(data.size)
        val tmp = ByteArray(4 * 1024)
        try {
            while (!inflater.finished()) {
                val count: Int = inflater.inflate(tmp)
                outputStream.write(tmp, 0, count)
            }
            outputStream.close()
        } catch (_: Exception) {
        }
        return outputStream.toByteArray()
    }
}