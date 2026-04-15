package x.x.memlists.core.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PhotoBitmapLoader {

    suspend fun load(path: String, targetPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (largest / (sample * 2) >= targetPx) sample *= 2

            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = BitmapFactory.decodeFile(path, options) ?: return@withContext null
            applyExifRotation(path, bitmap)
        }.getOrNull()
    }

    private fun applyExifRotation(path: String, bitmap: Bitmap): Bitmap {
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
        } catch (_: Exception) {
            bitmap
        }
    }
}
