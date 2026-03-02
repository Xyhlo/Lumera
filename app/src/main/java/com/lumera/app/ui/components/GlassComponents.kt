package com.lumera.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// --- 1. GLASS CARD (NOW ACCEPTS FOCUS REQUESTER) ---
@Composable
fun GlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    enabled: Boolean = true,
    isSelected: Boolean = false,
    containerColor: Color = Color.White.copy(alpha = 0.05f),
    focusedContainerColor: Color = Color.White.copy(alpha = 0.1f),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    focusRequester: FocusRequester? = null, // <--- NEW PARAMETER
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    // VISUAL PRIORITY: Selection White > Focus Accent > Idle Dark
    val targetBackgroundColor = if (isSelected) {
        Color.White.copy(0.25f) // Bright White if selected
    } else if (isFocused) {
        focusedContainerColor   // Subtle tint if just focused
    } else {
        containerColor          // Dark if idle
    }

    // Border Glow
    val borderColor by animateColorAsState(targetValue = if (isFocused) accentColor else if (isSelected) Color.White.copy(0.2f) else Color.Transparent, label = "BorderColor")
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.02f else 1.0f, label = "Scale")
    val borderWidth by animateDpAsState(targetValue = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp, label = "BorderWidth")

    Surface(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            // CRITICAL: Attach focus requester to the Surface itself
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        enabled = enabled,
        shape = shape,
        color = targetBackgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content(isFocused)
        }
    }
}

// --- 2. GLASS BUTTON ---
@Composable
fun GlassButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isPrimary: Boolean = false, textColor: Color = Color.White, focusRequester: FocusRequester? = null) {
    val accent = MaterialTheme.colorScheme.primary
    val containerCol = if (isPrimary) accent.copy(0.2f) else Color.White.copy(0.05f)
    val focusedCol = if (isPrimary) accent.copy(0.6f) else Color.White.copy(0.1f)

    GlassCard(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        contentPadding = PaddingValues(0.dp),
        containerColor = containerCol,
        focusedContainerColor = focusedCol,
        focusRequester = focusRequester // Pass it through
    ) { isFocused ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = text.uppercase(), style = MaterialTheme.typography.labelLarge, color = if (isFocused) Color.White else textColor)
        }
    }
}