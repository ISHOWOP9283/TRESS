package com.example.treemap.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Backwards compatibility alias for MapTreeLogoBadge.
 */
@Composable
fun GreenMapLogoBadge(
    size: Dp = 160.dp,
    showText: Boolean = true,
    modifier: Modifier = Modifier
) {
    MapTreeLogoBadge(
        size = size,
        showText = showText,
        modifier = modifier
    )
}
