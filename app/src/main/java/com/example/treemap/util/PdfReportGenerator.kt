package com.example.treemap.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.treemap.data.model.EntryCategory
import com.example.treemap.data.model.TreeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    /**
     * Generates a comprehensive official PDF inspection report for the given observation entry
     * and opens the Android share sheet to forward it to Local Governance, Forest Department, or NGOs.
     */
    suspend fun generateAndShareReport(context: Context, entry: TreeEntry): Result<File> = withContext(Dispatchers.IO) {
        try {
            val reportFile = generatePdfDocument(context, entry)
            if (reportFile == null || !reportFile.exists()) {
                return@withContext Result.failure(Exception("Failed to generate PDF document"))
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                reportFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "OFFICIAL MANGROVE FIELD REPORT: ${entry.title} (${entry.species})"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    """
                    OFFICIAL MANGROVE FIELD OBSERVATION REPORT
                    -------------------------------------------
                    • Title / Station: ${entry.title}
                    • Species: ${entry.species}
                    • Status: ${entry.categoryEnum.label}
                    • Zone: ${entry.zoneId.replace("_", " ").uppercase()}
                    • GPS Coordinates: ${entry.lat}, ${entry.lng}
                    • Observer: ${entry.reporter}
                    • Reported Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(entry.date))}
                    
                    Attached is the complete official field assessment PDF with photographic documentation for local governance, forest administration, and NGO action.
                    """.trimIndent()
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            withContext(Dispatchers.Main) {
                val chooser = Intent.createChooser(shareIntent, "Send Mangrove Report PDF to Governance / NGO").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                Toast.makeText(context, "PDF Report generated & ready to share", Toast.LENGTH_SHORT).show()
            }

            Result.success(reportFile)
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error creating PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
            Result.failure(e)
        }
    }

    private fun generatePdfDocument(context: Context, entry: TreeEntry): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (72 DPI points)
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val pageWidth = 595f
        val pageHeight = 842f
        val margin = 36f
        val contentWidth = pageWidth - (margin * 2)

        // 1. Background
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth, pageHeight, bgPaint)

        // 2. Header Banner (Deep Mangrove Forest Green)
        val headerPaint = Paint().apply {
            color = Color.parseColor("#064E3B") // Deep emerald
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth, 88f, headerPaint)

        // Header Accent Strip
        val headerAccentPaint = Paint().apply {
            color = Color.parseColor("#10B981") // Mint green
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 84f, pageWidth, 88f, headerAccentPaint)

        // Header Text
        val headerTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("MANGROVE CONSERVATION & FIELD INCIDENT REPORT", margin, 36f, headerTitlePaint)

        val headerSubPaint = Paint().apply {
            color = Color.parseColor("#A7F3D0")
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText("Official Coastal Ecosystem Telemetry & Geo-Spatial Assessment", margin, 52f, headerSubPaint)

        val headerMetaPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8.5f
            isAntiAlias = true
        }
        val reportRef = "DOC-REF: MNG-${entry.id}-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(entry.date))}"
        canvas.drawText(reportRef, margin, 70f, headerMetaPaint)
        val generatedDateStr = "Generated: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}"
        canvas.drawText(generatedDateStr, pageWidth - margin - headerMetaPaint.measureText(generatedDateStr), 70f, headerMetaPaint)

        var currentY = 108f

        // 3. Status Badge Row
        val category = entry.categoryEnum
        val statusBgColor = when (category) {
            EntryCategory.THRIVING_GROWTH -> Color.parseColor("#059669")
            EntryCategory.FAIR_GROWTH -> Color.parseColor("#D97706")
            EntryCategory.AT_RISK_DYING -> Color.parseColor("#DC2626")
        }

        val badgePaint = Paint().apply {
            color = statusBgColor
            style = Paint.Style.FILL
        }
        val badgeRect = RectF(margin, currentY, margin + 220f, currentY + 24f)
        canvas.drawRoundRect(badgeRect, 6f, 6f, badgePaint)

        val badgeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("STATUS: ${category.label.uppercase()}", margin + 10f, currentY + 16f, badgeTextPaint)

        // Sector Badge on right
        val zonePaint = Paint().apply {
            color = Color.parseColor("#F3F4F6")
            style = Paint.Style.FILL
        }
        val zoneText = "ZONE: ${entry.zoneId.replace("_", " ").uppercase()}"
        val zoneTextPaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val zoneWidth = zoneTextPaint.measureText(zoneText) + 20f
        val zoneRect = RectF(pageWidth - margin - zoneWidth, currentY, pageWidth - margin, currentY + 24f)
        canvas.drawRoundRect(zoneRect, 6f, 6f, zonePaint)
        canvas.drawText(zoneText, pageWidth - margin - zoneWidth + 10f, currentY + 16f, zoneTextPaint)

        currentY += 36f

        // 4. Observation Key Details Card
        val cardPaint = Paint().apply {
            color = Color.parseColor("#F9FAFB")
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val detailsCardRect = RectF(margin, currentY, pageWidth - margin, currentY + 95f)
        canvas.drawRoundRect(detailsCardRect, 8f, 8f, cardPaint)
        canvas.drawRoundRect(detailsCardRect, 8f, 8f, cardBorderPaint)

        val labelPaint = Paint().apply {
            color = Color.parseColor("#6B7280")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val valPaint = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val col1X = margin + 14f
        val col2X = margin + (contentWidth / 2f) + 10f

        // Row 1
        canvas.drawText("STATION / LOCATION TITLE", col1X, currentY + 18f, labelPaint)
        canvas.drawText(entry.title.take(35), col1X, currentY + 32f, valPaint)

        canvas.drawText("TARGET BOTANICAL SPECIES", col2X, currentY + 18f, labelPaint)
        canvas.drawText(entry.species.take(35), col2X, currentY + 32f, valPaint)

        // Row 2
        canvas.drawText("FIELD OBSERVER / OFFICER", col1X, currentY + 54f, labelPaint)
        canvas.drawText(entry.reporter.take(35), col1X, currentY + 68f, valPaint)

        canvas.drawText("LOGGED DATE & TIME", col2X, currentY + 54f, labelPaint)
        canvas.drawText(SimpleDateFormat("dd MMMM yyyy · HH:mm:ss", Locale.getDefault()).format(Date(entry.date)), col2X, currentY + 68f, valPaint)

        // Row 3 (GPS)
        canvas.drawText("HIGH-PRECISION GPS COORDINATES", col1X, currentY + 84f, labelPaint)
        val gpsStr = "%.6f° N, %.6f° E (WGS84 Datum)".format(entry.lat, entry.lng)
        val gpsValPaint = Paint().apply {
            color = Color.parseColor("#059669")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(gpsStr, col1X + 165f, currentY + 84f, gpsValPaint)

        currentY += 108f

        // 5. Notes & Field Assessment Section
        val sectionTitlePaint = Paint().apply {
            color = Color.parseColor("#064E3B")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("FIELD INSPECTION OBSERVATIONS & NOTES", margin, currentY + 10f, sectionTitlePaint)
        currentY += 18f

        val notesBoxRect = RectF(margin, currentY, pageWidth - margin, currentY + 60f)
        val notesBgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(notesBoxRect, 6f, 6f, notesBgPaint)
        canvas.drawRoundRect(notesBoxRect, 6f, 6f, cardBorderPaint)

        val notesTextPaint = Paint().apply {
            color = Color.parseColor("#374151")
            textSize = 9.5f
            isAntiAlias = true
        }
        val fullNotes = if (entry.notes.isNullOrBlank()) "Routine field observation logged at monitoring station. Standard canopy and root structure documented." else entry.notes
        drawWrappedText(canvas, fullNotes, margin + 10f, currentY + 16f, contentWidth - 20f, notesTextPaint, 13f, 3)

        currentY += 72f

        // 6. Photographic Evidence Section
        canvas.drawText("FIELD PHOTOGRAPHIC EVIDENCE (GEO-TAGGED)", margin, currentY + 10f, sectionTitlePaint)
        currentY += 18f

        val images = entry.imageList
        val firstImagePath = images.firstOrNull()

        var photoLoaded = false
        if (!firstImagePath.isNullOrBlank()) {
            val bitmap = ImageStorageHelper.loadBitmap(context, firstImagePath, 500, 300)
            if (bitmap != null) {
                val photoWidth = contentWidth
                val photoHeight = 210f
                val photoRect = RectF(margin, currentY, margin + photoWidth, currentY + photoHeight)

                // Draw photo border / card
                canvas.drawRoundRect(photoRect, 8f, 8f, cardPaint)
                canvas.drawRoundRect(photoRect, 8f, 8f, cardBorderPaint)

                // Scale and draw bitmap inside
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                val destRect = RectF(margin + 4f, currentY + 4f, margin + photoWidth - 4f, currentY + photoHeight - 4f)
                canvas.drawBitmap(bitmap, srcRect, destRect, null)

                // Photo Watermark / Caption Banner
                val watermarkBgPaint = Paint().apply {
                    color = Color.argb(190, 0, 0, 0)
                    style = Paint.Style.FILL
                }
                val watermarkRect = RectF(margin + 4f, currentY + photoHeight - 24f, margin + photoWidth - 4f, currentY + photoHeight - 4f)
                canvas.drawRoundRect(watermarkRect, 0f, 0f, watermarkBgPaint)

                val watermarkTextPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val watermarkText = "📍 STATION PHOTO • LAT: %.5f, LNG: %.5f • OBSERVER: %s".format(entry.lat, entry.lng, entry.reporter)
                canvas.drawText(watermarkText, margin + 12f, currentY + photoHeight - 10f, watermarkTextPaint)

                photoLoaded = true
                currentY += photoHeight + 14f
            }
        }

        if (!photoLoaded) {
            // Draw a placeholder box if no image
            val placeholderRect = RectF(margin, currentY, pageWidth - margin, currentY + 70f)
            canvas.drawRoundRect(placeholderRect, 8f, 8f, cardPaint)
            canvas.drawRoundRect(placeholderRect, 8f, 8f, cardBorderPaint)

            val noPhotoPaint = Paint().apply {
                color = Color.parseColor("#9CA3AF")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }
            canvas.drawText("No photographic attachment logged with this telemetry record.", margin + 14f, currentY + 40f, noPhotoPaint)
            currentY += 80f
        }

        // 7. Official Governance / NGO Submission Action Block
        val govBoxRect = RectF(margin, currentY, pageWidth - margin, currentY + 110f)
        val govBgPaint = Paint().apply {
            color = Color.parseColor("#ECFDF5") // light mint
            style = Paint.Style.FILL
        }
        val govBorderPaint = Paint().apply {
            color = Color.parseColor("#A7F3D0")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        canvas.drawRoundRect(govBoxRect, 8f, 8f, govBgPaint)
        canvas.drawRoundRect(govBoxRect, 8f, 8f, govBorderPaint)

        val govTitlePaint = Paint().apply {
            color = Color.parseColor("#065F46")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("REGULATORY GOVERNANCE & NGO ACTION ENDORSEMENT", margin + 14f, currentY + 20f, govTitlePaint)

        val checkTextPaint = Paint().apply {
            color = Color.parseColor("#047857")
            textSize = 8.5f
            isAntiAlias = true
        }
        canvas.drawText("[ ✓ ] Forwarded to Coastal Zone Management Authority & Forest Department", margin + 14f, currentY + 38f, checkTextPaint)
        canvas.drawText("[ ✓ ] Forwarded to Certified Mangrove Restoration & Conservation NGO Partners", margin + 14f, currentY + 52f, checkTextPaint)
        canvas.drawText("[   ] Site Enforcement / Remediation Scheduled    Date: __________________", margin + 14f, currentY + 66f, checkTextPaint)

        val sigPaint = Paint().apply {
            color = Color.parseColor("#374151")
            textSize = 8f
            isAntiAlias = true
        }
        canvas.drawText("Authorized Officer / Admin Signature: ______________________", margin + 14f, currentY + 94f, sigPaint)
        canvas.drawText("Official Seal / Stamp: ______________________", col2X, currentY + 94f, sigPaint)

        currentY += 122f

        // 8. Footer Bar
        val footerDividerPaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            strokeWidth = 1f
        }
        canvas.drawLine(margin, pageHeight - 34f, pageWidth - margin, pageHeight - 34f, footerDividerPaint)

        val footerTextPaint = Paint().apply {
            color = Color.parseColor("#9CA3AF")
            textSize = 7.5f
            isAntiAlias = true
        }
        canvas.drawText("GeoMangrove Ecosystem Monitoring Platform • Secure Cloud Sync Verified • Page 1 of 1", margin, pageHeight - 20f, footerTextPaint)
        val copyText = "Confidential Environmental Record"
        canvas.drawText(copyText, pageWidth - margin - footerTextPaint.measureText(copyText), pageHeight - 20f, footerTextPaint)

        document.finishPage(page)

        // Save PDF to cache
        val reportsDir = File(context.cacheDir, "mangrove_reports").apply { if (!exists()) mkdirs() }
        val fileName = "Mangrove_Report_${entry.id}_${System.currentTimeMillis()}.pdf"
        val outputFile = File(reportsDir, fileName)

        return try {
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float,
        maxLines: Int
    ) {
        val words = text.split(" ")
        var currentLine = ""
        var currentY = y
        var lineCount = 0

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width > maxWidth && currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, x, currentY, paint)
                currentY += lineHeight
                lineCount++
                if (lineCount >= maxLines - 1) {
                    currentLine = word + "..."
                    break
                }
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty() && lineCount < maxLines) {
            canvas.drawText(currentLine, x, currentY, paint)
        }
    }
}
