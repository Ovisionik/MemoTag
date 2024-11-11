package com.ovisionik.memotag.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object BitmapUtils {
    suspend fun getBitmapFromUrlAsync(src: String?): Bitmap? = withContext(Dispatchers.IO) {
        val bmp:Bitmap?
        try {
            val url = URL(src)
            val connection =
                url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            bmp = BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext null
        }
        return@withContext bmp
    }

    fun ByteArray.toBitmap(): Bitmap {
        return BitmapFactory.decodeByteArray(this, 0, this.size)
    }

    fun Bitmap.toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)

        return stream.toByteArray()
    }

    fun Bitmap.compressTo1MIO():Bitmap {
        val ONE_KIO = 1024
        val ONE_MIO = ONE_KIO * ONE_KIO
        //var bufferSize = Integer.MAX_VALUE
        var quality = 100
        val byteArrayOutputStream = ByteArrayOutputStream()
        do {
            byteArrayOutputStream.reset()
            this.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            val bufferSize = byteArrayOutputStream.size()
            Log.d("compressedImageFile","quality: $quality -> length: $bufferSize")
            quality -= 10
        } while (bufferSize > ONE_MIO)
        return this
    }

    /**
     * Remove x% of it's width/height from it's extremities
     *
     * eg i want to crop/reduce the height by 20% -> height: 0.2
     *
     * @param width The percentage of width to REMOVE
     * (from 0.01 to 0.99 where 0.01 = 1% and 0.99 = 99%)
     *
     * @param height The percentage of height to REMOVE
     * (from 0.01 to 0.99 where 0.01 = 1% and 0.99 = 99%)
     *
     * @return Resized Bitmap
     */
    fun Bitmap.removeXPercent(width: Double, height: Double): Bitmap {

        if (width >= 1 || height >= 1) { return this }
        //input = % to remove, not to keep, so let's get how much we need to cut
        val keepWidth = 1.0 - width
        val keepHeight = 1.0 - height

        val desPxW = this.width * keepWidth
        val desPxH = this.height * keepHeight

        val midW = this.width/2 - desPxW/2
        val midH = this.height/2 - desPxH/2

        return Bitmap.createBitmap(this, midW.toInt(), midH.toInt(), desPxW.toInt(), desPxH.toInt())
    }
}