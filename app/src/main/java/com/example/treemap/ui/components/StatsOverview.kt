package com.example.treemap.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.treemap.data.model.EntryCategory
import com.example.treemap.data.model.EntryStats
import com.example.treemap.ui.theme.MangroveTealPrimary
import com.example.treemap.ui.theme.StatusAtRisk
import com.example.treemap.ui.theme.StatusFair
import com.example.treemap.ui.theme.StatusThriving

@Composable
fun StatsOverview(
    stats: EntryStats,
    activeCategory: EntryCategory?,
    onCategoryClick: (EntryCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Total Points",
                count = stats.total,
                iconColor = MangroveTealPrimary,
                isSelected = activeCategory == null,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MangroveTealPrimary
                    )
                },
                onClick = { onCategoryClick(null) },
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Thriving",
                count = stats.thrivingCount,
                iconColor = StatusThriving,
                isSelected = activeCategory == EntryCategory.THRIVING_GROWTH,
                icon = {
                    Icon(
                        imageVector = EntryCategory.THRIVING_GROWTH.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                },
                onClick = {
                    onCategoryClick(if (activeCategory == EntryCategory.THRIVING_GROWTH) null else EntryCategory.THRIVING_GROWTH)
                },
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Fair",
                count = stats.fairCount,
                iconColor = StatusFair,
                isSelected = activeCategory == EntryCategory.FAIR_GROWTH,
                icon = {
                    Icon(
                        imageVector = EntryCategory.FAIR_GROWTH.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                },
                onClick = {
                    onCategoryClick(if (activeCategory == EntryCategory.FAIR_GROWTH) null else EntryCategory.FAIR_GROWTH)
                },
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "At Risk",
                count = stats.atRiskCount,
                iconColor = StatusAtRisk,
                isSelected = activeCategory == EntryCategory.AT_RISK_DYING,
                icon = {
                    Icon(
                        imageVector = EntryCategory.AT_RISK_DYING.icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                },
                onClick = {
                    onCategoryClick(if (activeCategory == EntryCategory.AT_RISK_DYING) null else EntryCategory.AT_RISK_DYING)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    iconColor: Color,
    isSelected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) iconColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) iconColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) iconColor else iconColor.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
