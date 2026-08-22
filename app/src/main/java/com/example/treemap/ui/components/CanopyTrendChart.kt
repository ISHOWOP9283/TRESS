package com.example.treemap.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun CanopyTrendChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier
        .width(130.dp)
        .height(54.dp),
    lineColor: Color = Color(0xFF2E6F64),
    fillColor: Color = Color(0x332E6F64)
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (dataPoints.size < 2) return@Canvas

        val minVal = (dataPoints.minOrNull() ?: 0f) * 0.85f
        val maxVal = (dataPoints.maxOrNull() ?: 100f) * 1.15f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        // Draw soft baseline grid
        val gridY1 = height * 0.33f
        val gridY2 = height * 0.66f
        val gridColor = Color(0xFFE2EBE7)

        drawLine(
            color = gridColor,
            start = Offset(0f, gridY1),
            end = Offset(width, gridY1),
            strokeWidth = 1f
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, gridY2),
            end = Offset(width, gridY2),
            strokeWidth = 1f
        )

        // Calculate points
        val stepX = width / (dataPoints.size - 1)
        val points = dataPoints.mapIndexed { index, value ->
            val x = index * stepX
            val normalized = (value - minVal) / range
            val y = height - (normalized * height)
            Offset(x, y.coerceIn(4f, height - 4f))
        }

        // Create smooth curve path
        val strokePath = Path()
        val fillPath = Path()

        strokePath.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, height)
        fillPath.lineTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

            strokePath.cubicTo(
                controlPoint1.x, controlPoint1.y,
                controlPoint2.x, controlPoint2.y,
                p1.x, p1.y
            )
            fillPath.cubicTo(
                controlPoint1.x, controlPoint1.y,
                controlPoint2.x, controlPoint2.y,
                p1.x, p1.y
            )
        }

        fillPath.lineTo(points.last().x, height)
        fillPath.close()

        // Draw gradient area fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    fillColor,
                    fillColor.copy(alpha = 0.05f)
                ),
                startY = 0f,
                endY = height
            )
        )

        // Draw smooth line
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw latest data point dot
        val lastPoint = points.last()
        drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = lastPoint
        )
        drawCircle(
            color = lineColor,
            radius = 2.5.dp.toPx(),
            center = lastPoint
        )
    }
}
