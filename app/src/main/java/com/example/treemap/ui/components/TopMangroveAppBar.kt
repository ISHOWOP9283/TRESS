package com.example.treemap.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.treemap.data.model.EntryCategory
import com.example.treemap.ui.theme.MangroveTealPrimary
import com.example.treemap.ui.theme.StatusAtRisk
import com.example.treemap.ui.theme.StatusFair
import com.example.treemap.ui.theme.StatusThriving
import com.example.treemap.util.LocationHelper
import com.example.treemap.util.PlaceSearchResult

@Composable
fun TopMangroveAppBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onProfileClick: () -> Unit,
    onSearchSubmit: (String) -> Unit = {},
    onSelectPlaceResult: (PlaceSearchResult) -> Unit = {},
    activeCategory: EntryCategory? = null,
    onCategorySelected: (EntryCategory?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    var isSearchFocused by remember { mutableStateOf(false) }

    val suggestions = remember(searchQuery) {
        LocationHelper.getSuggestions(searchQuery)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // 1. Google Maps Floating Search Bar Pill (Profile on right, no separate menu button)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 6.dp,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading Search Icon (Tap to trigger search)
                IconButton(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            onSearchSubmit(searchQuery)
                            focusManager.clearFocus()
                            isSearchFocused = false
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MangroveTealPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Search Input Field
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search area (e.g. Panvel), coordinates...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF9CA3AF),
                                fontSize = 13.5.sp
                            ),
                            maxLines = 1
                        )
                    }

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color(0xFF111827),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(MangroveTealPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                onSearchSubmit(searchQuery)
                                focusManager.clearFocus()
                                isSearchFocused = false
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isSearchFocused = it.isFocused }
                            .testTag("map_search_input")
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onSearchQueryChange("")
                            isSearchFocused = false
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Profile Avatar / User Options (Handles all profile, drawer & admin functions)
                Surface(
                    shape = CircleShape,
                    color = MangroveTealPrimary.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onProfileClick)
                        .testTag("user_profile_header_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "User Profile & Settings",
                            tint = MangroveTealPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Live Geocoding and Location Suggestions Dropdown
        AnimatedVisibility(
            visible = isSearchFocused && suggestions.isNotEmpty(),
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    suggestions.forEachIndexed { index, place ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectPlaceResult(place)
                                    focusManager.clearFocus()
                                    isSearchFocused = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MangroveTealPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (place.isMangroveZone) Icons.Default.LocationOn else Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MangroveTealPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = place.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF111827)
                                    )
                                )
                                Text(
                                    text = place.subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF6B7280),
                                        fontSize = 11.5.sp
                                    ),
                                    maxLines = 1
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (index < suggestions.size - 1) {
                            HorizontalDivider(
                                color = Color(0xFFF3F4F6),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Google Maps Horizontal Quick Filter Chips (No administrative sector/zone chips for regular users)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "All Issues" Chip
            FilterChipPill(
                label = "All Issues",
                dotColor = MangroveTealPrimary,
                isSelected = activeCategory == null,
                onClick = { onCategorySelected(null) }
            )

            // Dynamic Category Filter Chips
            EntryCategory.entries.forEach { cat ->
                val chipColor = when (cat) {
                    EntryCategory.THRIVING_GROWTH -> StatusThriving
                    EntryCategory.FAIR_GROWTH -> StatusFair
                    EntryCategory.AT_RISK_DYING -> StatusAtRisk
                    else -> MangroveTealPrimary
                }

                FilterChipPill(
                    label = cat.label,
                    dotColor = chipColor,
                    isSelected = activeCategory == cat,
                    onClick = {
                        onCategorySelected(if (activeCategory == cat) null else cat)
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    dotColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MangroveTealPrimary else Color.White,
        shadowElevation = if (isSelected) 3.dp else 2.dp,
        tonalElevation = 1.dp,
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("filter_chip_${label.lowercase().replace(" ", "_")}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else dotColor)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = label,
                style = TextStyle(
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Color(0xFF374151)
                )
            )
        }
    }
}
