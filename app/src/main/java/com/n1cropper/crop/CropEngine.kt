package com.n1cropper.crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.abs

object CropEngine {

    const val TARGET_RATIO = 4.0 / 3.0
    const val TOLERANCE = 0.03

    fun needsCrop(width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) return false
        val ratio = width.toDouble() / height.toDouble()
        return abs(ratio - TARGET_RATIO) > TOLERANCE
    }

    fun cropTo43(file: File): Boolean {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            val origW = opts.outWidth
            val origH = opts.outHeight
            if (origW <= 0 || origH <= 0) return false

            val ratio = origW.toDouble() / origH.toDouble()
            if (abs(ratio - TARGET_RATIO) <= TOLERANCE) return true

            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false
            val rotatedBitmap = fixOrientation(bitmap, file.absolutePath)

            val newW: Int
            val newH: Int
            val x: Int
            val y: Int

            val currentRatio = rotatedBitmap.width.toDouble() / rotatedBitmap.height.toDouble()
            if (currentRatio > TARGET_RATIO) {
                newH = rotatedBitmap.height
                newW = (rotatedBitmap.height * TARGET_RATIO).toInt()
                x = (rotatedBitmap.width - newW) / 2
                y = 0
            } else {
                newW = rotatedBitmap.width
                newH = (rotatedBitmap.width / TARGET_RATIO).toInt()
                x = 0
                y = (rotatedBitmap.height - newH) / 2
            }

            val cropped = Bitmap.createBitmap(rotatedBitmap, x, y, newW, newH)
            val isPng = file.name.endsWith(".png", ignoreCase = true)
            val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            val quality = if (isPng) 100 else 95

            val outputData = ByteArrayOutputStream().use { baos ->
                cropped.compress(format, quality, baos)
                baos.toByteArray()
            }
            file.writeBytes(outputData)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun fixOrientation(bitmap: Bitmap, path: String): Bitmap {
        return try {
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}
