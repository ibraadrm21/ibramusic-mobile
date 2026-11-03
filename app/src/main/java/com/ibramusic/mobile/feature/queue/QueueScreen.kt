package com.ibramusic.mobile.feature.queue

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.ibramusic.mobile.core.designsystem.AccentRose
import com.ibramusic.mobile.core.designsystem.BrandEmerald
import com.ibramusic.mobile.core.designsystem.DarkCanvas
import com.ibramusic.mobile.core.designsystem.DarkCard
import com.ibramusic.mobile.core.designsystem.Zinc400
import com.ibramusic.mobile.core.designsystem.Zinc50
import com.ibramusic.mobile.core.designsystem.Zinc600
import com.ibramusic.mobile.domain.model.QueueItem
import com.ibramusic.mobile.domain.model.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class DragQueueSection {
    USER_QUEUE,
    PLAYLIST_QUEUE
}

/**
 * High-performance, tactile Drag & Drop reorder state for Compose LazyColumn.
 * - Items swap in real-time as the dragged item moves past adjacent items.
 * - Non-dragged items smoothly glide out of the way with Compose animateItemPlacement().
 * - Dragged item stays 100% locked under finger touch (no jumping, no flashing indicator lines).
 * - Tactile haptic feedback on pick-up and slot-crossing.
 * - Reorder committed cleanly on finger release.
 */
class QueueReorderState(
    private val hapticFeedback: HapticFeedback? = null
) {
    var draggingItemId by mutableStateOf<String?>(null)
        private set
    var draggingSection by mutableStateOf<DragQueueSection?>(null)
        private set
    var dragOffsetY by mutableFloatStateOf(0f)
        private set
    var draggedCurrentIndex by mutableIntStateOf(-1)
        private set
    var initialIndex by mutableIntStateOf(-1)
        private set

    var userQueueWorking by mutableStateOf<List<QueueItem>>(emptyList())
        private set
    var playlistQueueWorking by mutableStateOf<List<QueueItem>>(emptyList())
        private set

    val isDragging: Boolean
        get() = draggingItemId != null

    fun isItemBeingDragged(id: String): Boolean = draggingItemId == id

    fun syncFromUpstream(userQueue: List<QueueItem>, playlistQueue: List<QueueItem>) {
        if (!isDragging) {
            userQueueWorking = userQueue
            playlistQueueWorking = playlistQueue
        }
    }

    fun startDrag(item: QueueItem, section: DragQueueSection) {
        val list = when (section) {
            DragQueueSection.USER_QUEUE -> userQueueWorking
            DragQueueSection.PLAYLIST_QUEUE -> playlistQueueWorking
        }
        val idx = list.indexOfFirst { it.queueEntryId == item.queueEntryId }
        if (idx < 0) return

        draggingItemId = item.queueEntryId
        draggingSection = section
        initialIndex = idx
        draggedCurrentIndex = idx
        dragOffsetY = 0f
        try {
            hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
    }

    fun onDragDelta(deltaY: Float, rowHeightPx: Float) {
        if (!isDragging || rowHeightPx <= 0f) return
        dragOffsetY += deltaY

        when (draggingSection) {
            DragQueueSection.USER_QUEUE -> {
                // Moving DOWN: dragged item passes half-way into next item
                while (dragOffsetY > rowHeightPx * 0.5f && draggedCurrentIndex < userQueueWorking.lastIndex) {
                    val mutable = userQueueWorking.toMutableList()
                    val moved = mutable.removeAt(draggedCurrentIndex)
                    mutable.add(draggedCurrentIndex + 1, moved)
                    userQueueWorking = mutable
                    draggedCurrentIndex += 1
                    dragOffsetY -= rowHeightPx
                    try {
                        hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                }
                // Moving UP: dragged item passes half-way into previous item
                while (dragOffsetY < -rowHeightPx * 0.5f && draggedCurrentIndex > 0) {
                    val mutable = userQueueWorking.toMutableList()
                    val moved = mutable.removeAt(draggedCurrentIndex)
                    mutable.add(draggedCurrentIndex - 1, moved)
                    userQueueWorking = mutable
                    draggedCurrentIndex -= 1
                    dragOffsetY += rowHeightPx
                    try {
                        hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                }
            }
            DragQueueSection.PLAYLIST_QUEUE -> {
                while (dragOffsetY > rowHeightPx * 0.5f && draggedCurrentIndex < playlistQueueWorking.lastIndex) {
                    val mutable = playlistQueueWorking.toMutableList()
                    val moved = mutable.removeAt(draggedCurrentIndex)
                    mutable.add(draggedCurrentIndex + 1, moved)
                    playlistQueueWorking = mutable
                    draggedCurrentIndex += 1
                    dragOffsetY -= rowHeightPx
                    try {
                        hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                }
                while (dragOffsetY < -rowHeightPx * 0.5f && draggedCurrentIndex > 0) {
                    val mutable = playlistQueueWorking.toMutableList()
                    val moved = mutable.removeAt(draggedCurrentIndex)
                    mutable.add(draggedCurrentIndex - 1, moved)
                    playlistQueueWorking = mutable
                    draggedCurrentIndex -= 1
                    dragOffsetY += rowHeightPx
                    try {
                        hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } catch (_: Exception) {}
                }
            }
            null -> Unit
        }
    }

    fun onAutoScrolled(scrollDeltaPx: Float, rowHeightPx: Float) {
        if (!isDragging) return
        dragOffsetY += scrollDeltaPx
        onDragDelta(0f, rowHeightPx)
    }

    fun endDrag(
        onCommitUser: (from: Int, to: Int) -> Unit,
        onCommitPlaylist: (from: Int, to: Int) -> Unit
    ) {
        val from = initialIndex
        val to = draggedCurrentIndex
        val section = draggingSection

        draggingItemId = null
        draggingSection = null
        initialIndex = -1
        draggedCurrentIndex = -1
        dragOffsetY = 0f

        if (from >= 0 && to >= 0 && from != to) {
            when (section) {
                DragQueueSection.USER_QUEUE -> onCommitUser(from, to)
                DragQueueSection.PLAYLIST_QUEUE -> onCommitPlaylist(from, to)
                null -> Unit
            }
        }
    }

    fun cancelDrag() {
        draggingItemId = null
        draggingSection = null
        initialIndex = -1
        draggedCurrentIndex = -1
        dragOffsetY = 0f
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QueueScreen(
    viewModel: QueueViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val queueState by viewModel.queueState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val reorderState = remember { QueueReorderState(haptic) }
    val listState = rememberLazyListState()

    val density = LocalDensity.current
    var measuredRowHeightPx by remember { mutableFloatStateOf(with(density) { 72.dp.toPx() }) }

    // Keep working lists synced with upstream queue state when not dragging
    LaunchedEffect(queueState.userQueue, queueState.playlistQueue) {
        reorderState.syncFromUpstream(queueState.userQueue, queueState.playlistQueue)
    }

    // Smooth auto-scroll when dragging near viewport boundaries
    LaunchedEffect(reorderState.isDragging) {
        if (!reorderState.isDragging) return@LaunchedEffect
        val edgeDistance = with(density) { 90.dp.toPx() }

        while (reorderState.isDragging) {
            val layoutInfo = listState.layoutInfo
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            val draggedItemInfo = layoutInfo.visibleItemsInfo.find { it.key == reorderState.draggingItemId }

            if (draggedItemInfo != null && viewportHeight > 0) {
                val visualTop = draggedItemInfo.offset + reorderState.dragOffsetY
                val visualBottom = visualTop + draggedItemInfo.size

                val topEdge = edgeDistance
                val bottomEdge = viewportHeight - edgeDistance

                var scrollAmount = 0f
                if (visualTop < topEdge && listState.canScrollBackward) {
                    val ratio = ((topEdge - visualTop) / edgeDistance).coerceIn(0.1f, 1f)
                    scrollAmount = -16f * ratio
                } else if (visualBottom > bottomEdge && listState.canScrollForward) {
                    val ratio = ((visualBottom - bottomEdge) / edgeDistance).coerceIn(0.1f, 1f)
                    scrollAmount = 16f * ratio
                }

                if (scrollAmount != 0f) {
                    val consumed = listState.scrollBy(scrollAmount)
                    reorderState.onAutoScrolled(consumed, measuredRowHeightPx)
                }
            }
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = !reorderState.isDragging
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = Zinc50
                    )
                }

                Text(
                    text = "Cola de reproducción",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Zinc50
                    )
                )

                if (reorderState.userQueueWorking.isNotEmpty()) {
                    TextButton(
                        onClick = viewModel::clearUserQueue,
                        enabled = !reorderState.isDragging,
                        colors = ButtonDefaults.textButtonColors(contentColor = Zinc400)
                    ) {
                        Text("Limpiar")
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            LazyColumn(
                state = listState,
                userScrollEnabled = !reorderState.isDragging,
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Now Playing Section
                queueState.current?.let { currentItem ->
                    item(key = "current_playing_header") {
                        Text(
                            text = "Reproduciendo ahora",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Zinc400
                            ),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .alpha(if (reorderState.isDragging) 0.40f else 1f)
                        )
                    }

                    item(key = "current_playing_track_${currentItem.queueEntryId}") {
                        QueueTrackRow(
                            track = currentItem.track,
                            isCurrent = true,
                            isBeingDragged = false,
                            isInteractive = !reorderState.isDragging,
                            onTrackClick = {},
                            onDelete = {},
                            dragHandleModifier = Modifier,
                            modifier = Modifier.alpha(if (reorderState.isDragging) 0.40f else 1f)
                        )
                    }
                }

                // 2. User Queue Section (Priority queued tracks)
                if (reorderState.userQueueWorking.isNotEmpty()) {
                    item(key = "user_queue_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp)
                                .alpha(if (reorderState.isDragging) 0.50f else 1f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "A continuación en la cola (${reorderState.userQueueWorking.size})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandEmerald
                                )
                            )
                            Text(
                                text = "Desliza a la izquierda para borrar",
                                style = MaterialTheme.typography.bodySmall.copy(color = Zinc600)
                            )
                        }
                    }

                    items(
                        items = reorderState.userQueueWorking,
                        key = { it.queueEntryId }
                    ) { item ->
                        val isBeingDragged = reorderState.isItemBeingDragged(item.queueEntryId)

                        val itemModifier = if (isBeingDragged) {
                            Modifier
                                .zIndex(100f)
                                .graphicsLayer {
                                    translationY = reorderState.dragOffsetY
                                    scaleX = 1.04f
                                    scaleY = 1.04f
                                    shadowElevation = 20.dp.toPx()
                                    shape = RoundedCornerShape(10.dp)
                                    clip = false
                                }
                        } else {
                            Modifier
                                .animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                .zIndex(1f)
                                .alpha(if (reorderState.isDragging) 0.50f else 1f)
                        }

                        QueueTrackRow(
                            track = item.track,
                            isCurrent = false,
                            isBeingDragged = isBeingDragged,
                            isInteractive = !reorderState.isDragging,
                            onTrackClick = {
                                if (!reorderState.isDragging) {
                                    viewModel.playTrack(item.track)
                                }
                            },
                            onDelete = { viewModel.removeFromQueue(item.queueEntryId) },
                            dragHandleModifier = Modifier.pointerInput(item.queueEntryId) {
                                detectDragGestures(
                                    onDragStart = {
                                        reorderState.startDrag(item, DragQueueSection.USER_QUEUE)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        reorderState.onDragDelta(dragAmount.y, measuredRowHeightPx)
                                    },
                                    onDragEnd = {
                                        reorderState.endDrag(
                                            onCommitUser = { from, to -> viewModel.reorderUserQueue(from, to) },
                                            onCommitPlaylist = { from, to -> viewModel.reorderPlaylistQueue(from, to) }
                                        )
                                    },
                                    onDragCancel = {
                                        reorderState.cancelDrag()
                                    }
                                )
                            },
                            modifier = itemModifier.onGloballyPositioned { coords ->
                                if (coords.size.height > 0 && !isBeingDragged) {
                                    measuredRowHeightPx = coords.size.height.toFloat() + with(density) { 8.dp.toPx() }
                                }
                            }
                        )
                    }
                }

                // 3. Playlist / Context Queue Section
                if (reorderState.playlistQueueWorking.isNotEmpty()) {
                    item(key = "playlist_queue_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp, bottom = 4.dp)
                                .alpha(if (reorderState.isDragging) 0.40f else 1f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "De la lista (${reorderState.playlistQueueWorking.size})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Zinc400
                                )
                            )
                            Text(
                                text = "Desliza a la izquierda para borrar",
                                style = MaterialTheme.typography.bodySmall.copy(color = Zinc600)
                            )
                        }
                    }

                    items(
                        items = reorderState.playlistQueueWorking,
                        key = { it.queueEntryId }
                    ) { item ->
                        val isBeingDragged = reorderState.isItemBeingDragged(item.queueEntryId)

                        val itemModifier = if (isBeingDragged) {
                            Modifier
                                .zIndex(100f)
                                .graphicsLayer {
                                    translationY = reorderState.dragOffsetY
                                    scaleX = 1.04f
                                    scaleY = 1.04f
                                    shadowElevation = 20.dp.toPx()
                                    shape = RoundedCornerShape(10.dp)
                                    clip = false
                                }
                        } else {
                            Modifier
                                .animateItemPlacement(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                .zIndex(1f)
                                .alpha(if (reorderState.isDragging) 0.50f else 1f)
                        }

                        QueueTrackRow(
                            track = item.track,
                            isCurrent = false,
                            isBeingDragged = isBeingDragged,
                            isInteractive = !reorderState.isDragging,
                            onTrackClick = {
                                if (!reorderState.isDragging) {
                                    viewModel.playTrack(item.track)
                                }
                            },
                            onDelete = { viewModel.removeFromQueue(item.queueEntryId) },
                            dragHandleModifier = Modifier.pointerInput(item.queueEntryId) {
                                detectDragGestures(
                                    onDragStart = {
                                        reorderState.startDrag(item, DragQueueSection.PLAYLIST_QUEUE)
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        reorderState.onDragDelta(dragAmount.y, measuredRowHeightPx)
                                    },
                                    onDragEnd = {
                                        reorderState.endDrag(
                                            onCommitUser = { from, to -> viewModel.reorderUserQueue(from, to) },
                                            onCommitPlaylist = { from, to -> viewModel.reorderPlaylistQueue(from, to) }
                                        )
                                    },
                                    onDragCancel = {
                                        reorderState.cancelDrag()
                                    }
                                )
                            },
                            modifier = itemModifier.onGloballyPositioned { coords ->
                                if (coords.size.height > 0 && !isBeingDragged) {
                                    measuredRowHeightPx = coords.size.height.toFloat() + with(density) { 8.dp.toPx() }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single Queue Row:
 * Decouples the track info (with horizontal swipe-to-delete) from the drag handle on the right.
 * Touching the handle exclusively triggers vertical reorder dragging, never swipe gestures.
 */
@Composable
fun QueueTrackRow(
    track: Track,
    isCurrent: Boolean,
    onTrackClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isBeingDragged: Boolean = false,
    isInteractive: Boolean = true,
    dragHandleModifier: Modifier = Modifier
) {
    val backgroundColor = if (isCurrent) DarkCard else Color(0x1A1E1E1E)
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, Color(0x15FFFFFF), shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track content: Wrapped in SwipeToDeleteQueueContainer (only on the left/track part)
        Box(modifier = Modifier.weight(1f)) {
            if (!isCurrent) {
                SwipeToDeleteQueueContainer(
                    onDelete = onDelete,
                    enabled = isInteractive && !isBeingDragged
                ) {
                    TrackInfoContent(
                        track = track,
                        isCurrent = false,
                        isBeingDragged = isBeingDragged,
                        isInteractive = isInteractive,
                        onClick = onTrackClick
                    )
                }
            } else {
                TrackInfoContent(
                    track = track,
                    isCurrent = true,
                    isBeingDragged = false,
                    isInteractive = false,
                    onClick = {}
                )
            }
        }

        // Dedicated Touch Target Drag Handle: completely outside SwipeToDeleteContainer
        if (!isCurrent) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(64.dp)
                    .then(dragHandleModifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = "Arrastrar para reordenar",
                    tint = Zinc400,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackInfoContent(
    track: Track,
    isCurrent: Boolean,
    isBeingDragged: Boolean,
    isInteractive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = isInteractive && !isBeingDragged,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkCard)
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) BrandEmerald else Zinc50
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artistName,
                style = MaterialTheme.typography.bodySmall.copy(color = Zinc600),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Swipe to delete container: Swiping left removes the item from the queue.
 * Automatically disabled while reorder dragging is active to avoid gesture collision.
 */
@Composable
fun SwipeToDeleteQueueContainer(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    swipeThresholdDp: Float = 96f,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { swipeThresholdDp.dp.toPx() }

    val offsetX = remember { Animatable(0f) }

    val draggableState = rememberDraggableState { delta ->
        if (!enabled) return@rememberDraggableState
        coroutineScope.launch {
            val newOffset = (offsetX.value + delta).coerceIn(-thresholdPx * 1.5f, 0f)
            offsetX.snapTo(newOffset)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        // Background reveal: Red Delete
        if (offsetX.value < -8f && enabled) {
            val progress = (-offsetX.value / thresholdPx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentRose.copy(alpha = 0.90f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
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
                    Text(
                        text = if (-offsetX.value >= thresholdPx) "Soltar para eliminar" else "Eliminar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Eliminar de la cola",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Foreground content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    onDragStopped = {
                        if (!enabled) return@draggable
                        coroutineScope.launch {
                            if (-offsetX.value >= thresholdPx) {
                                onDelete()
                            }
                            offsetX.animateTo(0f, animationSpec = tween(durationMillis = 200))
                        }
                    }
                )
        ) {
            content()
        }
    }
}
