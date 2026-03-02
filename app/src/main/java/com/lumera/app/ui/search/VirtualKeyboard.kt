package com.lumera.app.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lumera.app.ui.components.GlassButton

@Composable
fun VirtualKeyboard(
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 6-Column Grid for TV
    val keys = listOf(
        "A", "B", "C", "D", "E", "F",
        "G", "H", "I", "J", "K", "L",
        "M", "N", "O", "P", "Q", "R",
        "S", "T", "U", "V", "W", "X",
        "Y", "Z", "1", "2", "3", "4",
        "5", "6", "7", "8", "9", "0"
    )

    Column(
        modifier = modifier.width(320.dp), // Fixed width for the panel
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. CHARACTER GRID
        val columns = 6
        val rows = keys.chunked(columns)

        rows.forEach { rowKeys ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowKeys.forEach { key ->
                    // Using your new GlassButton!
                    GlassButton(
                        text = key,
                        onClick = { onKeyPress(key) },
                        modifier = Modifier.weight(1f).height(45.dp),
                        textColor = Color.White
                    )
                }
            }
        }

        // 2. ACTION ROW (Space, Backspace)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Space Bar
            GlassButton(
                text = "SPACE", // Or use Icon inside if you modify GlassButton
                onClick = { onKeyPress(" ") },
                modifier = Modifier.weight(2f).height(45.dp)
            )

            // Backspace (Custom GlassCard implementation to support Icon)
            // Since GlassButton takes String, we can either modify it or just pass text "<"
            // OR we create a custom GlassCard here for the icon support.
            // Let's use a simple text "DEL" or "<" for consistency with your GlassButton
            GlassButton(
                text = "DEL",
                onClick = onBackspace,
                modifier = Modifier.weight(1f).height(45.dp),
                isPrimary = true // Make it pop with accent color
            )
        }
    }
}