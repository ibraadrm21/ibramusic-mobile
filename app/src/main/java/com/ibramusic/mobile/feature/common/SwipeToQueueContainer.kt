package com.ibramusic.mobile.feature.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ibramusic.mobile.core.designsystem.BrandEmerald
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Wraps a track item and allows swiping to the right (Start to End) to quickly add it to the playback queue.
 * Snaps smoothly back to resting position upon release.
 */
@Composable
fun SwipeToQueueContainer(
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
    swipeThresholdDp: Float = 96f,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { swipeThresholdDp.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    var triggeredFeedback by remember { mutableStateOf(false) }

    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            val newOffset = (offsetX.value + delta).coerceIn(0f, thresholdPx * 1.6f)
            offsetX.snapTo(newOffset)
            if (newOffset >= thresholdPx && !triggeredFeedback) {
                triggeredFeedback = true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        // Background reveal: Emerald Queue action
        if (offsetX.value > 8f) {
            val progress = (offsetX.value / thresholdPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                BrandEmerald.copy(alpha = 0.90f),
                                BrandEmerald.copy(alpha = 0.35f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.graphicsLayer {
                        alpha = progress
                        scaleX = 0.8f + (progress * 0.2f)
                        scaleY = 0.8f + (progress * 0.2f)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlaylistAdd,
                        contentDescription = "Añadir a la cola",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (offsetX.value >= thresholdPx) "¡Soltar para encolar!" else "Encolar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Foreground content with horizontal drag offset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        coroutineScope.launch {
                            if (offsetX.value >= thresholdPx) {
                                onAddToQueue()
                            }
                            triggeredFeedback = false
                            offsetX.animateTo(0f, animationSpec = tween(durationMillis = 220))
                        }
                    }
                )
        ) {
            content()
        }
    }
}

