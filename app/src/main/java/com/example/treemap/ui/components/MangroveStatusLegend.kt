package com.example.treemap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.treemap.data.model.EntryCategory
import com.example.treemap.ui.theme.StatusAtRisk
import com.example.treemap.ui.theme.StatusFair
import com.example.treemap.ui.theme.StatusThriving

@Composable
fun MangroveStatusLegend(
    activeCategory: EntryCategory?,
    onCategoryClick: (EntryCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mangrove Status",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (activeCategory != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onCategoryClick(null) }
                            .padding(2.dp)
                    )
                }
            }

            LegendRow(
                icon = Icons.Default.Spa,
                label = "Thriving Growth",
                badgeBg = StatusThriving,
                isSelected = activeCategory == EntryCategory.THRIVING_GROWTH,
                onClick = {
                    onCategoryClick(if (activeCategory == EntryCategory.THRIVING_GROWTH) null else EntryCategory.THRIVING_GROWTH)
                }
            )

            LegendRow(
                icon = Icons.Default.TrendingUp,
                label = "Fair Growth",
                badgeBg = StatusFair,
                isSelected = activeCategory == EntryCategory.FAIR_GROWTH,
                onClick = {
                    onCategoryClick(if (activeCategory == EntryCategory.FAIR_GROWTH) null else EntryCategory.FAIR_GROWTH)
                }
            )

            LegendRow(
                icon = Icons.Default.WarningAmber,
                label = "At Risk / Dying",
                badgeBg = StatusAtRisk,
                isSelected = activeCategory == EntryCategory.AT_RISK_DYING,
                onClick = {
                    onCategoryClick(if (activeCategory == EntryCategory.AT_RISK_DYING) null else EntryCategory.AT_RISK_DYING)
                }
            )
        }
    }
}

@Composable
private fun LegendRow(
    icon: ImageVector,
    label: String,
    badgeBg: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) badgeBg.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
