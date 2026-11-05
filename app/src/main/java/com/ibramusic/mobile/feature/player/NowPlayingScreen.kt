package com.ibramusic.mobile.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ibramusic.mobile.core.designsystem.AccentRose
import com.ibramusic.mobile.core.designsystem.BorderActive
import com.ibramusic.mobile.core.designsystem.BrandEmerald
import com.ibramusic.mobile.core.designsystem.DarkCanvas
import com.ibramusic.mobile.core.designsystem.DarkCard
import com.ibramusic.mobile.core.designsystem.DarkGlassBorder
import com.ibramusic.mobile.core.designsystem.TextPrimary
import com.ibramusic.mobile.core.designsystem.TextSecondary
import com.ibramusic.mobile.core.designsystem.TextTertiary
import com.ibramusic.mobile.core.designsystem.ambientCoverGlow
import com.ibramusic.mobile.core.designsystem.Zinc400
import com.ibramusic.mobile.core.designsystem.Zinc50
import com.ibramusic.mobile.core.designsystem.Zinc600
import com.ibramusic.mobile.core.designsystem.Zinc800
import com.ibramusic.mobile.domain.model.RepeatMode
import com.ibramusic.mobile.feature.common.EqualizerBottomSheet
import com.ibramusic.mobile.feature.common.TrackOptionsBottomSheet

enum class PlayerView {
    OVERVIEW,
    LYRICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: NowPlayingViewModel,
    onDismiss: () -> Unit,
    onNavigateToQueue: () -> Unit,
    onGoToArtist: ((String, String?) -> Unit)? = null,
    onGoToAlbum: ((String, String, String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.playbackState.collectAsState()
    val isLiked by viewModel.isLiked.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()
    val currentSpeed by viewModel.playbackSpeed.collectAsState()
    val sleepTimerMinutes by viewModel.sleepTimerMinutesLeft.collectAsState()

    val currentTrack = state.currentTrack

    var selectedView by remember { mutableStateOf(PlayerView.OVERVIEW) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }
    var showTrackOptions by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        // Immersive ambient glow backdrop from active album art
        if (!currentTrack?.artworkUrl.isNullOrEmpty()) {
            AsyncImage(
                model = currentTrack?.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.18f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                DarkCanvas.copy(alpha = 0.85f),
                                DarkCanvas
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: [<- Back]  [ Overview | Lyrics ]  [Queue] [Waves dot] [(i)]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: <- Back
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Back",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Center: 2-Pill Switcher [ Overview | Lyrics ]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x1AFFFFFF))
                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(20.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isOverview = selectedView == PlayerView.OVERVIEW
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isOverview) Color.White else Color.Transparent)
                            .clickable { selectedView = PlayerView.OVERVIEW }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isOverview) Color.Black else TextSecondary,
                                fontSize = 13.sp
                            )
                        )
                    }

                    val isLyrics = selectedView == PlayerView.LYRICS
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isLyrics) Color.White else Color.Transparent)
                            .clickable { selectedView = PlayerView.LYRICS }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = if (isLyrics) Color.Black else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Lyrics",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLyrics) Color.Black else TextSecondary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }

                // Right Actions: Queue + Info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Queue Button (replaces EQ button as requested)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x1AFFFFFF))
                            .clickable { onNavigateToQueue() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QueueMusic,
                            contentDescription = "Cola de reproducción",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Info button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x1AFFFFFF))
                            .clickable { showTrackOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "Opciones",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Sleep Timer indicator chip if active
            if (sleepTimerMinutes != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x2610B981))
                        .clickable { showSleepTimerDialog = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sleep: ${sleepTimerMinutes}m",
                        color = Color(0xFF10B981),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            when (selectedView) {
                PlayerView.OVERVIEW -> {
                    // Lower the player content so controls sit comfortably near the bottom
                    Spacer(modifier = Modifier.weight(0.60f))

                    // Large Album Artwork: Big and immersive as in first pic (0.86f width, 1:1 aspect ratio)
                    // Supports vertical swipe gesture: Swipe up -> Queue, Swipe down -> Dismiss
                    var coverDragY by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient Artwork Glow (ArchiveTune / PixelPlayer inspired)
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.92f)
                                .ambientCoverGlow(glowColor = Color(0x35FFFFFF))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(0xFF141414))
                                .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(22.dp))
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onDragStart = { coverDragY = 0f },
                                        onDragEnd = {
                                            if (coverDragY < -70f) {
                                                onNavigateToQueue()
                                            } else if (coverDragY > 90f) {
                                                onDismiss()
                                            }
                                            coverDragY = 0f
                                        },
                                        onDragCancel = { coverDragY = 0f },
                                        onVerticalDrag = { _, dragAmount ->
                                            coverDragY += dragAmount
                                        }
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = currentTrack?.artworkUrl,
                                contentDescription = currentTrack?.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Title & Artist + Red Heart (Clean, like Nemzzz)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.90f)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentTrack?.title ?: "No hay canción",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (currentTrack?.isExplicit == true) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x33FFFFFF))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "E",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentTrack?.artistName ?: "IbraMusic",
                                color = Color(0xFFA1A1AA),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = viewModel::toggleLike,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Me gusta",
                                tint = if (isLiked) Color(0xFFFF334B) else Color(0xFFA1A1AA),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seek Slider & Duration
                    Column(
                        modifier = Modifier.fillMaxWidth(0.90f)
                    ) {
                        val currentPosition = if (isDraggingSlider) dragPositionMs.toLong() else state.positionMs
                        val totalDuration = state.durationMs.coerceAtLeast(1L)
                        val sliderValue = (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)

                        Slider(
                            value = sliderValue,
                            onValueChange = { fraction ->
                                isDraggingSlider = true
                                dragPositionMs = fraction * totalDuration
                            },
                            onValueChangeFinished = {
                                viewModel.seekTo(dragPositionMs.toLong())
                                isDraggingSlider = false
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(currentPosition),
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatDuration(totalDuration),
                                color = Color(0xFFA1A1AA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Transport Controls - lifted up to natural thumb height (matching Image 2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.90f)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = viewModel::toggleShuffle) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (state.shuffleEnabled) Color.White else Color(0xFF71717A),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = viewModel::skipPrevious,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color.Black,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = viewModel::skipNext,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(onClick = viewModel::cycleRepeatMode) {
                            val repeatIcon = when (state.repeatMode) {
                                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                else -> Icons.Rounded.Repeat
                            }
                            val repeatColor = when (state.repeatMode) {
                                RepeatMode.OFF -> Color(0xFF71717A)
                                else -> Color.White
                            }
                            Icon(
                                imageVector = repeatIcon,
                                contentDescription = "Repeat",
                                tint = repeatColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Moderate bottom space so controls sit nicely towards the bottom of the screen
                    Spacer(modifier = Modifier.weight(0.40f))
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }

                PlayerView.LYRICS -> {
                    // Synchronized Live Lyrics View
                    if (isLoadingLyrics) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Zinc50)
                        }
                    } else if (lyrics.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se encontraron letras sincronizadas para esta canción",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Zinc400),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val listState = rememberLazyListState()
                        val currentMs = state.positionMs

                        // Auto-scroll to active lyric
                        val activeIndex = lyrics.indexOfLast { it.timeMs <= currentMs }.coerceAtLeast(0)
                        LaunchedEffect(activeIndex) {
                            if (activeIndex >= 0 && activeIndex < lyrics.size) {
                                listState.animateScrollToItem(activeIndex)
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            items(lyrics) { line ->
                                val isCurrent = line.timeMs <= currentMs &&
                                    (lyrics.getOrNull(lyrics.indexOf(line) + 1)?.timeMs ?: Long.MAX_VALUE) > currentMs

                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = if (isCurrent) 22.sp else 17.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) Zinc50 else Zinc600,
                                        lineHeight = 28.sp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.seekTo(line.timeMs) }
                                        .padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Speed Modal Dialog
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                title = { Text("Velocidad de reproducción", color = Zinc50) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            val selected = currentSpeed == speed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Color(0x33FFFFFF) else Color.Transparent)
                                    .clickable {
                                        viewModel.setPlaybackSpeed(speed)
                                        showSpeedDialog = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${speed}x",
                                    color = if (selected) Zinc50 else Zinc400,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Zinc50)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Cerrar", color = Zinc50)
                    }
                },
                containerColor = DarkCard
            )
        }

        // Sleep Timer Modal Dialog
        if (showSleepTimerDialog) {
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                title = { Text("Temporizador de apagado", color = Zinc50) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0 to "Desactivado",
                            15 to "15 minutos",
                            30 to "30 minutos",
                            45 to "45 minutos",
                            60 to "1 hora"
                        ).forEach { (mins, label) ->
                            val selected = if (mins == 0) sleepTimerMinutes == null else sleepTimerMinutes == mins
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) Color(0x33FFFFFF) else Color.Transparent)
                                    .clickable {
                                        viewModel.setSleepTimer(mins)
                                        showSleepTimerDialog = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) Zinc50 else Zinc400,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Zinc50)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("Cerrar", color = Zinc50)
                    }
                },
                containerColor = DarkCard
            )
        }

        // Track Options Bottom Sheet
        if (showTrackOptions && currentTrack != null) {
            val activeDownloads by viewModel.activeDownloads.collectAsState()
            val isDownloaded = currentTrack.availableOffline || (activeDownloads[currentTrack.id] ?: 0) == 100
            TrackOptionsBottomSheet(
                track = currentTrack,
                playlists = playlists,
                isFavorite = isLiked,
                isDownloaded = isDownloaded,
                onDismiss = { showTrackOptions = false },
                onPlayNext = { viewModel.playNext(currentTrack) },
                onAddToQueue = { viewModel.addToQueue(currentTrack) },
                onToggleFavorite = { viewModel.toggleLike() },
                onAddToPlaylist = { playlistId ->
                    viewModel.addTrackToPlaylist(playlistId, currentTrack)
                },
                onCreatePlaylistAndAdd = { name ->
                    viewModel.createPlaylist(name) { newId ->
                        viewModel.addTrackToPlaylist(newId, currentTrack)
                    }
                },
                onDownloadTrack = {
                    viewModel.downloadTrack(currentTrack)
                },
                onDeleteDownload = {
                    viewModel.removeDownload(currentTrack.id)
                },
                onSetPlaybackSpeed = {
                    showSpeedDialog = true
                },
                onSetSleepTimer = {
                    showSleepTimerDialog = true
                },
                onOpenQueue = onNavigateToQueue,
                onGoToArtist = { artistName, artistId ->
                    onDismiss()
                    onGoToArtist?.invoke(artistName, artistId)
                },
                onGoToAlbum = { albumTitle, artistName, albumId ->
                    onDismiss()
                    onGoToAlbum?.invoke(albumTitle, artistName, albumId)
                }
            )
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
