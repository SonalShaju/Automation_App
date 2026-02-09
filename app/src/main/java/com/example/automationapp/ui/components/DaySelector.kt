package com.example.automationapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data class representing a day of the week
 */
data class DayOfWeek(
    val code: String,      // e.g., "MON", "TUE"
    val letter: String     // Single letter: "M", "T", etc.
)

/**
 * Standard days of the week with their codes and single-letter labels
 */
val daysOfWeek = listOf(
    DayOfWeek("SUN", "S"),
    DayOfWeek("MON", "M"),
    DayOfWeek("TUE", "T"),
    DayOfWeek("WED", "W"),
    DayOfWeek("THU", "T"),
    DayOfWeek("FRI", "F"),
    DayOfWeek("SAT", "S")
)

/**
 * A compact day selector with circular toggle buttons.
 *
 * @param selectedDays Set of selected day codes (e.g., "MON", "TUE")
 * @param onSelectionChanged Callback when selection changes
 * @param modifier Optional modifier
 */
@Composable
fun DaySelector(
    selectedDays: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        daysOfWeek.forEach { day ->
            DayToggle(
                day = day,
                isSelected = selectedDays.contains(day.code),
                onClick = {
                    val newSelection = if (selectedDays.contains(day.code)) {
                        selectedDays - day.code
                    } else {
                        selectedDays + day.code
                    }
                    onSelectionChanged(newSelection)
                }
            )
        }
    }
}

/**
 * A single day toggle button with circular shape.
 *
 * @param day The day of week data
 * @param isSelected Whether this day is selected
 * @param onClick Click handler
 */
@Composable
private fun DayToggle(
    day: DayOfWeek,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.letter,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Alternative DaySelector with slightly larger touch targets
 * for accessibility on smaller screens.
 */
@Composable
fun DaySelectorLarge(
    selectedDays: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        daysOfWeek.forEach { day ->
            DayToggleLarge(
                day = day,
                isSelected = selectedDays.contains(day.code),
                onClick = {
                    val newSelection = if (selectedDays.contains(day.code)) {
                        selectedDays - day.code
                    } else {
                        selectedDays + day.code
                    }
                    onSelectionChanged(newSelection)
                }
            )
        }
    }
}

@Composable
private fun DayToggleLarge(
    day: DayOfWeek,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.letter,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Preset buttons for quick day selection
 */
@Composable
fun DayPresetButtons(
    onSelectWeekdays: () -> Unit,
    onSelectWeekend: () -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.material3.FilterChip(
            selected = false,
            onClick = onSelectWeekdays,
            label = { Text("Weekdays") },
            modifier = Modifier.weight(1f)
        )
        androidx.compose.material3.FilterChip(
            selected = false,
            onClick = onSelectWeekend,
            label = { Text("Weekend") },
            modifier = Modifier.weight(1f)
        )
    }
}

// Helper constants for day sets
val WEEKDAYS = setOf("MON", "TUE", "WED", "THU", "FRI")
val WEEKEND = setOf("SAT", "SUN")
val ALL_DAYS = setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

