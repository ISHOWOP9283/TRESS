package com.example.treemap.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * MapTree™ Official Logo Emblem ("MapTree - PROJECT TOMORROW").
 * Exact replication of the new vector brand mark:
 * - 3D folded isometric perspective map base with roads and green terrain panels
 * - Central vibrant gradient Location Pin pointing into the map
 * - Upward blooming tree branches arching over the pin
 * - 5 lush gradient leaves (1 central top, 2 upper side, 2 lower side)
 * - Modern typography: "Map" (charcoal) + "Tree" (green) with leaf sprout on 'T'
 * - Subtitle: "— PROJECT TOMORROW —"
 */
@Composable
fun MapTreeLogoBadge(
    size: Dp = 160.dp,
    showText: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                val center = Offset(w / 2f, h / 2f)

                // Background clean white
                drawCircle(color = Color.White, radius = w / 2f, center = center)

                val scaleY = if (showText) 0.88f else 1.15f
                val offsetY = if (showText) -h * 0.05f else 0f

                // ----------------------------------------------------
                // 1. ISOMETRIC FOLDED MAP BASE
                // ----------------------------------------------------
                val mapCenterY = (h * 0.58f * scaleY) + offsetY + (if (showText) 0f else h * 0.04f)
                val mapW = w * 0.46f * scaleY
                val mapH = h * 0.16f * scaleY

                // Base Shadow / 3D Extrusion thickness
                val extrudePath = Path().apply {
                    moveTo(center.x - mapW * 0.95f, mapCenterY + mapH * 0.35f)
                    lineTo(center.x - mapW * 0.45f, mapCenterY + mapH * 0.65f)
                    lineTo(center.x, mapCenterY + mapH * 0.42f)
                    lineTo(center.x + mapW * 0.45f, mapCenterY + mapH * 0.70f)
                    lineTo(center.x + mapW * 0.95f, mapCenterY + mapH * 0.40f)
                    lineTo(center.x + mapW * 0.95f, mapCenterY + mapH * 0.52f)
                    lineTo(center.x + mapW * 0.45f, mapCenterY + mapH * 0.82f)
                    lineTo(center.x, mapCenterY + mapH * 0.54f)
                    lineTo(center.x - mapW * 0.45f, mapCenterY + mapH * 0.77f)
                    lineTo(center.x - mapW * 0.95f, mapCenterY + mapH * 0.47f)
                    close()
                }
                drawPath(path = extrudePath, color = Color(0xFF0F5132))

                // Left Map Fold
                val leftFold = Path().apply {
                    moveTo(center.x - mapW * 0.95f, mapCenterY + mapH * 0.35f)
                    lineTo(center.x - mapW * 0.55f, mapCenterY - mapH * 0.45f)
                    lineTo(center.x - mapW * 0.10f, mapCenterY - mapH * 0.20f)
                    lineTo(center.x - mapW * 0.45f, mapCenterY + mapH * 0.65f)
                    close()
                }
                drawPath(
                    path = leftFold,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF8CE33E), Color(0xFF4CAF50)),
                        start = Offset(center.x - mapW, mapCenterY - mapH),
                        end = Offset(center.x - mapW * 0.4f, mapCenterY + mapH)
                    )
                )

                // Center Map Fold (Valley)
                val centerFold = Path().apply {
                    moveTo(center.x - mapW * 0.10f, mapCenterY - mapH * 0.20f)
                    lineTo(center.x + mapW * 0.35f, mapCenterY - mapH * 0.45f)
                    lineTo(center.x + mapW * 0.45f, mapCenterY + mapH * 0.70f)
                    lineTo(center.x, mapCenterY + mapH * 0.42f)
                    lineTo(center.x - mapW * 0.45f, mapCenterY + mapH * 0.65f)
                    close()
                }
                drawPath(
                    path = centerFold,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF388E3C)),
                        start = Offset(center.x, mapCenterY - mapH),
                        end = Offset(center.x, mapCenterY + mapH)
                    )
                )

                // Right Map Fold
                val rightFold = Path().apply {
                    moveTo(center.x + mapW * 0.35f, mapCenterY - mapH * 0.45f)
                    lineTo(center.x + mapW * 0.85f, mapCenterY - mapH * 0.25f)
                    lineTo(center.x + mapW * 0.95f, mapCenterY + mapH * 0.40f)
                    lineTo(center.x + mapW * 0.45f, mapCenterY + mapH * 0.70f)
                    close()
                }
                drawPath(
                    path = rightFold,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF66BB6A), Color(0xFF43A047), Color(0xFF2E7D32)),
                        start = Offset(center.x + mapW * 0.4f, mapCenterY - mapH),
                        end = Offset(center.x + mapW, mapCenterY + mapH)
                    )
                )

                // White Road Grid Lines on Map
                val roadStroke = 2.4f * (w / 200f) * scaleY
                val roadPath1 = Path().apply {
                    moveTo(center.x - mapW * 0.75f, mapCenterY - mapH * 0.10f)
                    lineTo(center.x - mapW * 0.30f, mapCenterY + mapH * 0.20f)
                    lineTo(center.x + mapW * 0.15f, mapCenterY + mapH * 0.05f)
                    lineTo(center.x + mapW * 0.70f, mapCenterY + mapH * 0.15f)
                }
                drawPath(path = roadPath1, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = roadStroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

                val roadPath2 = Path().apply {
                    moveTo(center.x - mapW * 0.35f, mapCenterY - mapH * 0.35f)
                    lineTo(center.x - mapW * 0.30f, mapCenterY + mapH * 0.20f)
                    lineTo(center.x - mapW * 0.20f, mapCenterY + mapH * 0.50f)
                }
                drawPath(path = roadPath2, color = Color.White.copy(alpha = 0.75f), style = Stroke(width = roadStroke * 0.8f, cap = StrokeCap.Round))

                val roadPath3 = Path().apply {
                    moveTo(center.x + mapW * 0.25f, mapCenterY - mapH * 0.35f)
                    lineTo(center.x + mapW * 0.15f, mapCenterY + mapH * 0.05f)
                    lineTo(center.x + mapW * 0.35f, mapCenterY + mapH * 0.55f)
                }
                drawPath(path = roadPath3, color = Color.White.copy(alpha = 0.75f), style = Stroke(width = roadStroke * 0.8f, cap = StrokeCap.Round))


                // ----------------------------------------------------
                // 2. CENTRAL LOCATION PIN
                // ----------------------------------------------------
                val pinCenterY = (h * 0.40f * scaleY) + offsetY
                val pinW = w * 0.22f * scaleY
                val pinH = h * 0.26f * scaleY
                val pinRadius = pinW / 2f

                // Drop shadow under the pin tip
                drawOval(
                    color = Color(0xFF0F5132).copy(alpha = 0.45f),
                    topLeft = Offset(center.x - pinRadius * 0.5f, pinCenterY + pinH * 0.42f),
                    size = Size(pinRadius * 1.0f, pinRadius * 0.35f)
                )

                // Location Pin Body (Lime to Forest Green gradient)
                val pinTopY = pinCenterY - pinH * 0.5f
                val pinBottomY = pinCenterY + pinH * 0.5f

                val pinPath = Path().apply {
                    arcTo(
                        rect = Rect(
                            left = center.x - pinRadius,
                            top = pinTopY,
                            right = center.x + pinRadius,
                            bottom = pinTopY + pinW
                        ),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = true
                    )
                    // Curving to tip
                    cubicTo(
                        center.x + pinRadius, pinTopY + pinW * 0.9f,
                        center.x + pinRadius * 0.4f, pinBottomY - pinH * 0.15f,
                        center.x, pinBottomY
                    )
                    cubicTo(
                        center.x - pinRadius * 0.4f, pinBottomY - pinH * 0.15f,
                        center.x - pinRadius, pinTopY + pinW * 0.9f,
                        center.x - pinRadius, pinTopY + pinRadius
                    )
                    close()
                }

                drawPath(
                    path = pinPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF76FF03),
                            Color(0xFF4CAF50),
                            Color(0xFF2E7D32),
                            Color(0xFF1B5E20)
                        ),
                        startY = pinTopY,
                        endY = pinBottomY
                    )
                )

                // Center White Circular Cutout
                val cutoutRadius = pinRadius * 0.42f
                val cutoutCenter = Offset(center.x, pinTopY + pinRadius)
                drawCircle(
                    color = Color.White,
                    radius = cutoutRadius,
                    center = cutoutCenter
                )

                // ----------------------------------------------------
                // 3. ARCHING TREE BRANCHES
                // ----------------------------------------------------
                val branchStroke = 3.2f * (w / 200f) * scaleY
                val branchColor = Color(0xFF0D3E24)

                // Upper Arch over pin
                val archRadius = pinRadius * 1.35f
                drawArc(
                    color = branchColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - archRadius, cutoutCenter.y - archRadius * 1.15f),
                    size = Size(archRadius * 2f, archRadius * 2f),
                    style = Stroke(width = branchStroke, cap = StrokeCap.Round)
                )

                // Lower Left Branch
                val leftBranch = Path().apply {
                    moveTo(center.x - pinRadius * 0.8f, cutoutCenter.y + pinRadius * 0.35f)
                    cubicTo(
                        center.x - pinRadius * 1.3f, cutoutCenter.y + pinRadius * 0.1f,
                        center.x - pinRadius * 1.6f, cutoutCenter.y - pinRadius * 0.1f,
                        center.x - pinRadius * 1.8f, cutoutCenter.y - pinRadius * 0.3f
                    )
                }
                drawPath(path = leftBranch, color = branchColor, style = Stroke(width = branchStroke * 0.9f, cap = StrokeCap.Round))

                // Lower Right Branch
                val rightBranch = Path().apply {
                    moveTo(center.x + pinRadius * 0.8f, cutoutCenter.y + pinRadius * 0.35f)
                    cubicTo(
                        center.x + pinRadius * 1.3f, cutoutCenter.y + pinRadius * 0.1f,
                        center.x + pinRadius * 1.6f, cutoutCenter.y - pinRadius * 0.1f,
                        center.x + pinRadius * 1.8f, cutoutCenter.y - pinRadius * 0.3f
                    )
                }
                drawPath(path = rightBranch, color = branchColor, style = Stroke(width = branchStroke * 0.9f, cap = StrokeCap.Round))

                // Upper Left Branch to leaf
                val upperLeftBranch = Path().apply {
                    moveTo(center.x - archRadius * 0.8f, cutoutCenter.y - archRadius * 0.8f)
                    lineTo(center.x - pinRadius * 1.4f, cutoutCenter.y - pinRadius * 1.5f)
                }
                drawPath(path = upperLeftBranch, color = branchColor, style = Stroke(width = branchStroke * 0.9f, cap = StrokeCap.Round))

                // Upper Right Branch to leaf
                val upperRightBranch = Path().apply {
                    moveTo(center.x + archRadius * 0.8f, cutoutCenter.y - archRadius * 0.8f)
                    lineTo(center.x + pinRadius * 1.4f, cutoutCenter.y - pinRadius * 1.5f)
                }
                drawPath(path = upperRightBranch, color = branchColor, style = Stroke(width = branchStroke * 0.9f, cap = StrokeCap.Round))

                // Central Stem to Top Leaf
                val centerStem = Path().apply {
                    moveTo(center.x, cutoutCenter.y - archRadius * 1.15f)
                    lineTo(center.x, cutoutCenter.y - archRadius * 1.45f)
                }
                drawPath(path = centerStem, color = branchColor, style = Stroke(width = branchStroke, cap = StrokeCap.Round))


                // ----------------------------------------------------
                // 4. THE 5 LUSH GRADIENT LEAVES
                // ----------------------------------------------------
                // A) TOP CENTRAL LARGE LEAF
                val topLeafCenter = Offset(center.x, cutoutCenter.y - archRadius * 2.2f)
                drawVibrantLeaf(
                    tip = Offset(topLeafCenter.x, topLeafCenter.y - h * 0.11f * scaleY),
                    base = Offset(topLeafCenter.x, topLeafCenter.y + h * 0.08f * scaleY),
                    width = w * 0.19f * scaleY,
                    leftColorStart = Color(0xFF76FF03),
                    leftColorEnd = Color(0xFF43A047),
                    rightColorStart = Color(0xFF4CAF50),
                    rightColorEnd = Color(0xFF1B5E20),
                    veinColor = Color(0xFF0D3E24)
                )

                // B) UPPER LEFT LEAF
                drawRotatedLeaf(
                    center = Offset(center.x - pinRadius * 1.55f, cutoutCenter.y - pinRadius * 1.9f),
                    angle = -38f,
                    length = h * 0.16f * scaleY,
                    width = w * 0.14f * scaleY,
                    colorStart = Color(0xFF76FF03),
                    colorEnd = Color(0xFF2E7D32)
                )

                // C) UPPER RIGHT LEAF
                drawRotatedLeaf(
                    center = Offset(center.x + pinRadius * 1.55f, cutoutCenter.y - pinRadius * 1.9f),
                    angle = 38f,
                    length = h * 0.16f * scaleY,
                    width = w * 0.14f * scaleY,
                    colorStart = Color(0xFF81C784),
                    colorEnd = Color(0xFF1B5E20)
                )

                // D) LOWER LEFT SMALL LEAF
                drawRotatedLeaf(
                    center = Offset(center.x - pinRadius * 1.95f, cutoutCenter.y - pinRadius * 0.45f),
                    angle = -65f,
                    length = h * 0.12f * scaleY,
                    width = w * 0.10f * scaleY,
                    colorStart = Color(0xFF76FF03),
                    colorEnd = Color(0xFF2E7D32)
                )

                // E) LOWER RIGHT SMALL LEAF
                drawRotatedLeaf(
                    center = Offset(center.x + pinRadius * 1.95f, cutoutCenter.y - pinRadius * 0.45f),
                    angle = 65f,
                    length = h * 0.12f * scaleY,
                    width = w * 0.10f * scaleY,
                    colorStart = Color(0xFF81C784),
                    colorEnd = Color(0xFF1B5E20)
                )


                // ----------------------------------------------------
                // 5. BRAND TYPOGRAPHY: "MapTree" & "PROJECT TOMORROW"
                // ----------------------------------------------------
                if (showText) {
                    val paintMap = Paint().apply {
                        color = android.graphics.Color.parseColor("#1F2937") // Dark slate charcoal
                        textSize = (w * 0.155f)
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }

                    val paintTree = Paint().apply {
                        color = android.graphics.Color.parseColor("#059669") // Rich emerald green
                        textSize = (w * 0.155f)
                        isAntiAlias = true
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.LEFT
                    }

                    val paintSubtitle = Paint().apply {
                        color = android.graphics.Color.parseColor("#1F2937")
                        textSize = (w * 0.040f)
                        isAntiAlias = true
                        letterSpacing = 0.22f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }

                    val textY = h * 0.81f

                    // Draw "Map"
                    drawContext.canvas.nativeCanvas.drawText(
                        "Map",
                        center.x - (w * 0.02f),
                        textY,
                        paintMap
                    )

                    // Draw "Tree"
                    drawContext.canvas.nativeCanvas.drawText(
                        "Tree",
                        center.x,
                        textY,
                        paintTree
                    )

                    // Distinctive Twin Green Leaves sprouting on the 'T' of Tree
                    val tSproutX = center.x + (w * 0.082f)
                    val tSproutY = textY - (h * 0.115f)

                    drawRotatedLeaf(
                        center = Offset(tSproutX - (w * 0.015f), tSproutY + (h * 0.012f)),
                        angle = -45f,
                        length = h * 0.055f,
                        width = w * 0.040f,
                        colorStart = Color(0xFF76FF03),
                        colorEnd = Color(0xFF2E7D32)
                    )
                    drawRotatedLeaf(
                        center = Offset(tSproutX + (w * 0.022f), tSproutY - (h * 0.005f)),
                        angle = 35f,
                        length = h * 0.065f,
                        width = w * 0.048f,
                        colorStart = Color(0xFF4CAF50),
                        colorEnd = Color(0xFF1B5E20)
                    )

                    // Subtitle: "— PROJECT TOMORROW —"
                    val subY = h * 0.89f
                    drawContext.canvas.nativeCanvas.drawText(
                        "PROJECT TOMORROW",
                        center.x,
                        subY,
                        paintSubtitle
                    )

                    // Left & Right Green Accent Underline bars
                    val lineStroke = 2.2f * (w / 200f)
                    val subHalfWidth = w * 0.32f
                    val barLen = w * 0.10f

                    drawLine(
                        color = Color(0xFF059669),
                        start = Offset(center.x - subHalfWidth - barLen, subY - (h * 0.012f)),
                        end = Offset(center.x - subHalfWidth - (w * 0.02f), subY - (h * 0.012f)),
                        strokeWidth = lineStroke,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = Color(0xFF059669),
                        start = Offset(center.x + subHalfWidth + (w * 0.02f), subY - (h * 0.012f)),
                        end = Offset(center.x + subHalfWidth + barLen, subY - (h * 0.012f)),
                        strokeWidth = lineStroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * Renders a high fidelity split leaf with center vein
 */
private fun DrawScope.drawVibrantLeaf(
    tip: Offset,
    base: Offset,
    width: Float,
    leftColorStart: Color,
    leftColorEnd: Color,
    rightColorStart: Color,
    rightColorEnd: Color,
    veinColor: Color
) {
    val midY = (tip.y + base.y) / 2f
    val halfW = width / 2f

    // Left Half
    val leftPath = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(
            tip.x - halfW * 1.1f, tip.y + (midY - tip.y) * 0.5f,
            tip.x - halfW * 1.1f, base.y - (base.y - midY) * 0.4f,
            base.x, base.y
        )
        lineTo(tip.x, tip.y)
        close()
    }
    drawPath(
        path = leftPath,
        brush = Brush.linearGradient(
            colors = listOf(leftColorStart, leftColorEnd),
            start = tip,
            end = base
        )
    )

    // Right Half
    val rightPath = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(
            tip.x + halfW * 1.1f, tip.y + (midY - tip.y) * 0.5f,
            tip.x + halfW * 1.1f, base.y - (base.y - midY) * 0.4f,
            base.x, base.y
        )
        lineTo(tip.x, tip.y)
        close()
    }
    drawPath(
        path = rightPath,
        brush = Brush.linearGradient(
            colors = listOf(rightColorStart, rightColorEnd),
            start = tip,
            end = base
        )
    )

    // Center Vein
    drawLine(
        color = veinColor,
        start = Offset(tip.x, tip.y + 2f),
        end = Offset(base.x, base.y),
        strokeWidth = 2.0f,
        cap = StrokeCap.Round
    )
}

/**
 * Renders an angled organic leaf with gradient
 */
private fun DrawScope.drawRotatedLeaf(
    center: Offset,
    angle: Float,
    length: Float,
    width: Float,
    colorStart: Color,
    colorEnd: Color
) {
    val rad = Math.toRadians(angle.toDouble())
    val cos = Math.cos(rad).toFloat()
    val sin = Math.sin(rad).toFloat()

    val tip = rotate(0f, -length * 0.5f, cos, sin, center)
    val base = rotate(0f, length * 0.5f, cos, sin, center)
    val left = rotate(-width * 0.55f, 0f, cos, sin, center)
    val right = rotate(width * 0.55f, 0f, cos, sin, center)

    val leafPath = Path().apply {
        moveTo(tip.x, tip.y)
        cubicTo(left.x, left.y - length * 0.1f, left.x, base.y - length * 0.1f, base.x, base.y)
        cubicTo(right.x, base.y - length * 0.1f, right.x, left.y - length * 0.1f, tip.x, tip.y)
        close()
    }

    drawPath(
        path = leafPath,
        brush = Brush.linearGradient(
            colors = listOf(colorStart, colorEnd),
            start = tip,
            end = base
        )
    )

    // Subtle center vein
    drawLine(
        color = Color(0xFF0D3E24).copy(alpha = 0.65f),
        start = tip,
        end = base,
        strokeWidth = 1.2f,
        cap = StrokeCap.Round
    )
}

private fun rotate(x: Float, y: Float, cos: Float, sin: Float, origin: Offset): Offset {
    return Offset(
        origin.x + (x * cos - y * sin),
        origin.y + (x * sin + y * cos)
    )
}
