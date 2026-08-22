package com.example.treemap.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object ImageStorageHelper {

    private fun getPhotosDirectory(context: Context): File {
        val dir = File(context.filesDir, "mangrove_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Saves a captured Bitmap from camera to app's internal storage
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val dir = getPhotosDirectory(context)
            val fileName = "IMG_FIELD_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}_${UUID.randomUUID().toString().take(6)}.jpg"
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copies an image from a Content URI (e.g. from gallery picker) to app's internal storage
     */
    fun saveUriToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            val dir = getPhotosDirectory(context)
            val fileName = "IMG_GALLERY_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
            val file = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    outputStream.flush()
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a sample botanical / field observation image for demo and testing
     */
    fun createSampleMangrovePhoto(
        context: Context,
        label: String,
        subLabel: String,
        speciesName: String,
        bgThemeColorHex: String
    ): String? {
        return try {
            val width = 600
            val height = 450
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background
            val parsedColor = try {
                Color.parseColor(bgThemeColorHex)
            } catch (e: Exception) {
                Color.parseColor("#1B4D3E")
            }

            val bgPaint = Paint().apply {
                color = parsedColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Water horizon gradient/band
            val waterPaint = Paint().apply {
                color = Color.parseColor("#0C2E2B")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, height * 0.55f, width.toFloat(), height.toFloat(), waterPaint)

            // Estuary / Tree Trunk Illustration
            val trunkPaint = Paint().apply {
                color = Color.parseColor("#3E2723")
                style = Paint.Style.STROKE
                strokeWidth = 14f
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }

            // Stilt Roots
            val rootPath = Path().apply {
                moveTo(width * 0.5f, height * 0.45f)
                quadTo(width * 0.35f, height * 0.65f, width * 0.25f, height * 0.85f)
                moveTo(width * 0.5f, height * 0.45f)
                quadTo(width * 0.45f, height * 0.68f, width * 0.40f, height * 0.88f)
                moveTo(width * 0.5f, height * 0.45f)
                quadTo(width * 0.55f, height * 0.68f, width * 0.60f, height * 0.88f)
                moveTo(width * 0.5f, height * 0.45f)
                quadTo(width * 0.65f, height * 0.65f, width * 0.75f, height * 0.85f)
            }
            canvas.drawPath(rootPath, trunkPaint)

            // Foliage Canopy Circles
            val foliagePaint = Paint().apply {
                color = Color.parseColor("#2E7D32")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(width * 0.5f, height * 0.32f, 75f, foliagePaint)

            foliagePaint.color = Color.parseColor("#388E3C")
            canvas.drawCircle(width * 0.4f, height * 0.36f, 60f, foliagePaint)
            canvas.drawCircle(width * 0.6f, height * 0.36f, 60f, foliagePaint)

            foliagePaint.color = Color.parseColor("#4CAF50")
            canvas.drawCircle(width * 0.5f, height * 0.26f, 50f, foliagePaint)

            // Header Overlay Card
            val cardPaint = Paint().apply {
                color = Color.argb(200, 10, 20, 20)
                style = Paint.Style.FILL
            }
            val cardRect = RectF(20f, 20f, (width - 20).toFloat(), 120f)
            canvas.drawRoundRect(cardRect, 16f, 16f, cardPaint)

            // Text info
            val titleTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 28f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText(label, 40f, 60f, titleTextPaint)

            val subTextPaint = Paint().apply {
                color = Color.parseColor("#80CBC4")
                textSize = 20f
                isAntiAlias = true
            }
            canvas.drawText("$speciesName · $subLabel", 40f, 95f, subTextPaint)

            // GPS watermark at bottom
            val gpsPaint = Paint().apply {
                color = Color.argb(220, 255, 255, 255)
                textSize = 18f
                isAntiAlias = true
            }
            canvas.drawText("📍 FIELD TELEMETRY VERIFIED", 30f, height - 30f, gpsPaint)

            saveBitmapToInternalStorage(context, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
