package com.ovisionik.memotag.Utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream


object BitmapUtils {
    const val ONE_KIO = 1024
    const val ONE_MIO = ONE_KIO * ONE_KIO

    /**
     * Compress, if needed, an image file to be lower than or equal to 1 Mio
     *
     * @param filePath Image file path
     *
     * @return Stream containing data of the compressed image. Can be null
     */
    fun compressedImageFile(filePath: String): InputStream? {
        var quality = 100
        var inputStream: InputStream? = null
        if (filePath.isNotEmpty()) {
            var bufferSize = Integer.MAX_VALUE
            val byteArrayOutputStream = ByteArrayOutputStream()
            try {
                val bitmap = BitmapFactory.decodeFile(filePath)
                do {
                    if (bitmap != null) {
                        byteArrayOutputStream.reset()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
                        bufferSize = byteArrayOutputStream.size()
                        Log.d("compressedImageFile","quality: $quality -> length: $bufferSize")
                        quality -= 10
                    }
                } while (bufferSize > ONE_MIO)
                inputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
                byteArrayOutputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("compressedImageFile error", "${e.message}")
            }
        }
        return inputStream
    }

    fun Bitmap.CompressTo1MIO():Bitmap {
        val ONE_KIO = 1024
        val ONE_MIO = ONE_KIO * ONE_KIO
        var bufferSize = Integer.MAX_VALUE
        var quality = 100
        val byteArrayOutputStream = ByteArrayOutputStream()
        do {
            byteArrayOutputStream.reset()
            this.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            bufferSize = byteArrayOutputStream.size()
            Log.d("compressedImageFile","quality: $quality -> length: $bufferSize")
            quality -= 10
        } while (bufferSize > ONE_MIO)
        return this
    }

    fun ByteArray.toBitmap(): Bitmap {
        return BitmapFactory.decodeByteArray(this, 0, this.size)
    }

    fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)

        return stream.toByteArray()
    }
}