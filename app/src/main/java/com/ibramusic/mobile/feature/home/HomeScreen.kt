package com.ibramusic.mobile.feature.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ibramusic.mobile.core.designsystem.AccentRose
import com.ibramusic.mobile.core.designsystem.BorderActive
import com.ibramusic.mobile.core.designsystem.BrandEmerald
import com.ibramusic.mobile.core.designsystem.ChipContainer
import com.ibramusic.mobile.core.designsystem.DarkCard
import com.ibramusic.mobile.core.designsystem.DarkGlassBorder
import com.ibramusic.mobile.core.designsystem.HeroCardGradient
import com.ibramusic.mobile.core.designsystem.NoirBlack
import com.ibramusic.mobile.core.designsystem.TextPrimary
import com.ibramusic.mobile.core.designsystem.TextSecondary
import com.ibramusic.mobile.core.designsystem.TextTertiary
import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.feature.common.SwipeToQueueContainer
import com.ibramusic.mobile.feature.common.TrackOptionsBottomSheet
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onTrackSelected: (Track) -> Unit,
    onPlaylistClick: (String) -> Unit = {},
    onLikedClick: () -> Unit = {},
    onGoToArtist: ((String, String?) -> Unit)? = null,
    onGoToAlbum: ((String, String, String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var selectedTrackForMenu by remember { mutableStateOf<Track?>(null) }
    var selectedFilter by remember { mutableStateOf("Todo") }

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val dynamicGreeting = when {
        currentHour in 5..11 -> "Buenos días"
        currentHour in 12..19 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    val hasContent = uiState.trending.isNotEmpty() || uiState.recommendations.isNotEmpty() || uiState.recentlyPlayed.isNotEmpty()
    if (uiState.isLoading && !hasContent) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(NoirBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NoirBlack)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Dynamic Greeting Hero Banner (matching ibramusic-one.vercel.app)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    Color.White.copy(alpha = 0.03f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 24.dp, vertical = 22.dp)
                ) {
                    // Top highlight specular line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.50f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = dynamicGreeting,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 28.sp,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Text(
                            text = "Studio streaming en alta fidelidad",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF9CA3AF),
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }

            // Quick Access 2x2 Grid (Tus Playlists / Favoritos)
            if (playlists.isNotEmpty() || uiState.trending.isNotEmpty()) {
                item {
                    val quickItems = playlists.take(4)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Slot 1: Liked songs shortcut
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkCard)
                                    .border(1.dp, DarkGlassBorder, RoundedCornerShape(10.dp))
                                    .clickable { onLikedClick() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(Color(0xFF881337), AccentRose)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Favorite,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Tus Me Gusta",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            }

                            // Slot 2
                            val secondItem = quickItems.getOrNull(0)
                            if (secondItem != null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkCard)
                                        .border(1.dp, DarkGlassBorder, RoundedCornerShape(10.dp))
                                        .clickable { onPlaylistClick(secondItem.id) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                                                .background(Color(0xFF1E293B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!secondItem.coverUrl.isNullOrEmpty()) {
                                                AsyncImage(
                                                    model = secondItem.coverUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Rounded.MusicNote,
                                                    contentDescription = null,
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = secondItem.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        // Row 2
                        if (quickItems.size >= 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickItems.drop(1).take(2).forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DarkCard)
                                            .border(1.dp, DarkGlassBorder, RoundedCornerShape(10.dp))
                                            .clickable { onPlaylistClick(item.id) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
                                                    .background(Color(0xFF1E293B)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!item.coverUrl.isNullOrEmpty()) {
                                                    AsyncImage(
                                                        model = item.coverUrl,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Rounded.MusicNote,
                                                        contentDescription = null,
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }



            // Trending Tracks Section ("Tendencias Globales")
            if (uiState.trending.isNotEmpty()) {
                item {
                    Text(
                        text = "Tendencias Globales",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.trending, key = { it.id }) { track ->
                            TrendingTrackCard(
                                track = track,
                                onClick = {
                                    viewModel.playTrack(track, uiState.trending)
                                    onTrackSelected(track)
                                },
                                onOptionsClick = { selectedTrackForMenu = track }
                            )
                        }
                    }
                }
            }

            // Recommendations Section ("Recomendado para ti")
            if (uiState.recommendations.isNotEmpty()) {
                item {
                    Text(
                        text = "Recomendado para ti",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.recommendations, key = { it.id }) { track ->
                            TrendingTrackCard(
                                track = track,
                                onClick = {
                                    viewModel.playTrack(track, uiState.recommendations)
                                    onTrackSelected(track)
                                },
                                onOptionsClick = { selectedTrackForMenu = track }
                            )
                        }
                    }
                }
            }

            // Recently Played Section ("Escuchado recientemente")
            if (uiState.recentlyPlayed.isNotEmpty()) {
                item {
                    Text(
                        text = "Escuchado recientemente",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(uiState.recentlyPlayed, key = { it.id }) { track ->
                    SwipeToQueueContainer(
                        onAddToQueue = { viewModel.addToQueue(track) }
                    ) {
                        HistoryTrackRow(
                            track = track,
                            onClick = {
                                viewModel.playTrack(track, uiState.recentlyPlayed)
                                onTrackSelected(track)
                            },
                            onOptionsClick = { selectedTrackForMenu = track }
                        )
                    }
                }
            }
        }

        // Context Menu Bottom Sheet
        selectedTrackForMenu?.let { track ->
            TrackOptionsBottomSheet(
                track = track,
                isFavorite = track.isLiked,
                playlists = playlists,
                onDismiss = { selectedTrackForMenu = null },
                onPlayNext = { viewModel.playNext(track) },
                onAddToQueue = { viewModel.addToQueue(track) },
                onToggleFavorite = { viewModel.toggleLike(track) },
                onAddToPlaylist = { playlistId ->
                    viewModel.addTrackToPlaylist(playlistId, track)
                },
                onCreatePlaylistAndAdd = { name ->
                    viewModel.createPlaylist(name) { newId ->
                        viewModel.addTrackToPlaylist(newId, track)
                    }
                },
                onDownloadTrack = {
                    viewModel.downloadTrack(track)
                },
                onGoToArtist = onGoToArtist,
                onGoToAlbum = onGoToAlbum
            )
        }
    }
}

@Composable
fun TrendingTrackCard(
    track: Track,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(148.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard)
                .border(1.dp, DarkGlassBorder, RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Play button overlay in bottom right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = track.artistName,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onOptionsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Options",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryTrackRow(
    track: Track,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkCard)
                    .border(1.dp, DarkGlassBorder, RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artistName,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onOptionsClick) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Options",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
