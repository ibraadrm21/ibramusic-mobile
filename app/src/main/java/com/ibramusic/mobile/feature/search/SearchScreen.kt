package com.ibramusic.mobile.feature.search

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ibramusic.mobile.core.designsystem.BorderActive
import com.ibramusic.mobile.core.designsystem.BrandEmerald
import com.ibramusic.mobile.core.designsystem.ChipContainer
import com.ibramusic.mobile.core.designsystem.DarkCard
import com.ibramusic.mobile.core.designsystem.DarkGlassBorder
import com.ibramusic.mobile.core.designsystem.GlassCard
import com.ibramusic.mobile.core.designsystem.NoirBlack
import com.ibramusic.mobile.core.designsystem.TextPrimary
import com.ibramusic.mobile.core.designsystem.TextSecondary
import com.ibramusic.mobile.core.designsystem.TextTertiary
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.feature.common.SwipeToQueueContainer
import com.ibramusic.mobile.feature.common.TrackOptionsBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onTrackSelected: (Track) -> Unit,
    onArtistClick: (Artist) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    onGoToArtist: (String, String?) -> Unit = { _, _ -> },
    onGoToAlbum: (String, String, String?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    var selectedTrackForMenu by remember { mutableStateOf<Track?>(null) }

    Box(modifier = modifier.fillMaxSize().background(NoirBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Universal Search Input (Blueprint 3: Search)
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = {
                    Text(
                        text = "Canciones, artistas, álbumes...",
                        color = TextTertiary,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Buscar",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Limpiar",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ChipContainer,
                    unfocusedContainerColor = ChipContainer,
                    focusedBorderColor = BorderActive,
                    unfocusedBorderColor = DarkGlassBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // InnerTube Real-time Autocomplete Suggestions (Section 3.2 Harmony Music Blueprint)
            if (uiState.suggestions.isNotEmpty() && uiState.query.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.suggestions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF222222))
                                .border(1.dp, BorderActive, RoundedCornerShape(20.dp))
                                .clickable { viewModel.onSuggestionSelected(suggestion) }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = suggestion,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // When query is BLANK -> TRENDING NOW
            if (uiState.query.isBlank()) {
                LazyColumn(
                    contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // TRENDING NOW
                    if (uiState.trendingTracks.isNotEmpty()) {
                        item {
                            Text(
                                text = "TRENDING NOW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = TextTertiary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        itemsIndexed(uiState.trendingTracks, key = { _, track -> track.id }) { index, track ->
                            TrendingTrackRow(
                                rank = index + 1,
                                track = track,
                                onTrackClick = {
                                    viewModel.playTrack(track, uiState.trendingTracks)
                                    onTrackSelected(track)
                                },
                                onOptionsClick = { selectedTrackForMenu = track },
                                downloadProgress = activeDownloads[track.id]
                            )
                        }
                    }
                }
            } else {
                // When searching: Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SearchCategory.values().toList()) { category ->
                        val selected = uiState.selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.selectCategory(category) },
                            label = {
                                Text(
                                    text = when (category) {
                                        SearchCategory.ALL -> "Todo"
                                        SearchCategory.TRACKS -> "Canciones (${uiState.tracks.size})"
                                        SearchCategory.ARTISTS -> "Artistas (${uiState.artists.size})"
                                        SearchCategory.ALBUMS -> "Álbumes (${uiState.albums.size})"
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ChipContainer,
                                labelColor = TextSecondary,
                                selectedContainerColor = Color.White,
                                selectedLabelColor = Color.Black
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = DarkGlassBorder,
                                selectedBorderColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Results Section
                if (uiState.isSearching) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else if (uiState.tracks.isEmpty() && uiState.artists.isEmpty() && uiState.albums.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron resultados para \"${uiState.query}\"",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    when (uiState.selectedCategory) {
                        SearchCategory.ALL -> {
                            LazyColumn(
                                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (uiState.tracks.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Canciones",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }

                                    items(uiState.tracks.take(5), key = { it.id }) { track ->
                                        SwipeToQueueContainer(
                                            onAddToQueue = { viewModel.addToQueue(track) }
                                        ) {
                                            SearchResultTrackRow(
                                                track = track,
                                                onTrackClick = {
                                                    viewModel.playTrack(track, uiState.tracks)
                                                    onTrackSelected(track)
                                                },
                                                onOptionsClick = { selectedTrackForMenu = track },
                                                downloadProgress = activeDownloads[track.id]
                                            )
                                        }
                                    }
                                }

                                if (uiState.artists.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Artistas",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                        )

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(uiState.artists, key = { it.id }) { artist ->
                                                SearchArtistCard(
                                                    artist = artist,
                                                    onClick = { onArtistClick(artist) }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (uiState.albums.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Álbumes",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                        )

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            items(uiState.albums, key = { it.id }) { album ->
                                                SearchAlbumCard(
                                                    album = album,
                                                    onClick = { onAlbumClick(album) }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (uiState.tracks.size > 5) {
                                    item {
                                        Text(
                                            text = "Más canciones",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                                        )
                                    }

                                    items(uiState.tracks.drop(5), key = { it.id }) { track ->
                                        SwipeToQueueContainer(
                                            onAddToQueue = { viewModel.addToQueue(track) }
                                        ) {
                                            SearchResultTrackRow(
                                                track = track,
                                                onTrackClick = {
                                                    viewModel.playTrack(track, uiState.tracks)
                                                    onTrackSelected(track)
                                                },
                                                onOptionsClick = { selectedTrackForMenu = track },
                                                downloadProgress = activeDownloads[track.id]
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SearchCategory.TRACKS -> {
                            LazyColumn(
                                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.tracks, key = { it.id }) { track ->
                                    SwipeToQueueContainer(
                                        onAddToQueue = { viewModel.addToQueue(track) }
                                    ) {
                                        SearchResultTrackRow(
                                            track = track,
                                            onTrackClick = {
                                                viewModel.playTrack(track, uiState.tracks)
                                                onTrackSelected(track)
                                            },
                                            onOptionsClick = { selectedTrackForMenu = track },
                                            downloadProgress = activeDownloads[track.id]
                                        )
                                    }
                                }
                            }
                        }

                        SearchCategory.ARTISTS -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.artists, key = { it.id }) { artist ->
                                    SearchArtistCard(
                                        artist = artist,
                                        onClick = { onArtistClick(artist) }
                                    )
                                }
                            }
                        }

                        SearchCategory.ALBUMS -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.albums, key = { it.id }) { album ->
                                    SearchAlbumCard(
                                        album = album,
                                        onClick = { onAlbumClick(album) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Track Context Menu Bottom Sheet
        selectedTrackForMenu?.let { track ->
            val isDownloaded = track.availableOffline || (activeDownloads[track.id] ?: 0) == 100
            TrackOptionsBottomSheet(
                track = track,
                playlists = playlists,
                isDownloaded = isDownloaded,
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
                onDeleteDownload = {
                    viewModel.removeDownload(track.id)
                },
                onGoToArtist = onGoToArtist,
                onGoToAlbum = onGoToAlbum
            )
        }
    }
}

@Composable
fun TrendingTrackRow(
    rank: Int,
    track: Track,
    onTrackClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloadProgress: Int? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onTrackClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (rank <= 3) Color.White else TextTertiary,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Start
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkCard)
                .border(1.dp, DarkGlassBorder, RoundedCornerShape(10.dp))
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
            if (downloadProgress != null && downloadProgress in 1..99) {
                Text(
                    text = "Descargando $downloadProgress%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BrandEmerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (track.availableOffline || downloadProgress == 100) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.DownloadDone,
                        contentDescription = "Descargada",
                        tint = BrandEmerald,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = track.artistName,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Opciones",
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SearchResultTrackRow(
    track: Track,
    onTrackClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloadProgress: Int? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onTrackClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkCard)
                .border(1.dp, DarkGlassBorder, RoundedCornerShape(10.dp))
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
            if (downloadProgress != null && downloadProgress in 1..99) {
                Text(
                    text = "Descargando $downloadProgress%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BrandEmerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (track.availableOffline || downloadProgress == 100) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.DownloadDone,
                        contentDescription = "Descargada",
                        tint = BrandEmerald,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = track.artistName,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onOptionsClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Opciones",
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SearchArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .width(136.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(DarkCard),
                contentAlignment = Alignment.Center
            ) {
                if (!artist.artworkUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = artist.artworkUrl,
                        contentDescription = artist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = artist.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Artista",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SearchAlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard),
                contentAlignment = Alignment.Center
            ) {
                if (!album.artworkUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = album.artworkUrl,
                        contentDescription = album.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = album.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = album.artistName,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (album.releaseYear != null) {
                Text(
                    text = "${album.releaseYear}",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }
    }
}
