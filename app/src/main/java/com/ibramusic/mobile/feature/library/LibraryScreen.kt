package com.ibramusic.mobile.feature.library

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.feature.common.SwipeToQueueContainer
import com.ibramusic.mobile.feature.common.TrackOptionsBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onTrackSelected: (Track) -> Unit,
    onPlaylistClick: (String) -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    onGoToArtist: (String, String?) -> Unit = { _, _ -> },
    onGoToAlbum: (String, String, String?) -> Unit = { _, _, _ -> },
    onCreatePlaylistClick: () -> Unit = {},
    onImportSpotifyClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val followedArtists by viewModel.followedArtists.collectAsState()
    val savedAlbums by viewModel.savedAlbums.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val offlinePlaylists by viewModel.offlinePlaylists.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()

    val selectedNormalTab by viewModel.selectedNormalTab.collectAsState()
    val selectedOfflineTab by viewModel.selectedOfflineTab.collectAsState()

    var filterQuery by remember { mutableStateOf("") }
    var selectedTrackForMenu by remember { mutableStateOf<Track?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NoirBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Offline Mode Alert Banner (if offline)
            if (isOfflineMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22F59E0B))
                        .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Modo Offline Activo",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            )
                            Text(
                                text = "Mostrando canciones y playlists disponibles en este dispositivo",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isOfflineMode) "Biblioteca Offline" else "Tu Biblioteca",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    val subtitleText = if (isOfflineMode) {
                        when (selectedOfflineTab) {
                            LibraryOfflineTab.DOWNLOADS -> "${downloadedTracks.size} canciones descargadas"
                            LibraryOfflineTab.OFFLINE_PLAYLISTS -> "${offlinePlaylists.size} playlists offline"
                        }
                    } else {
                        when (selectedNormalTab) {
                            LibraryNormalTab.PLAYLISTS -> "${playlists.size} playlists"
                            LibraryNormalTab.ARTISTS -> "${followedArtists.size} artistas seguidos"
                            LibraryNormalTab.ALBUMS -> "${savedAlbums.size} álbumes guardados"
                        }
                    }
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                }

                // Header Action Buttons
                if (!isOfflineMode && selectedNormalTab == LibraryNormalTab.PLAYLISTS) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .clickable { onCreatePlaylistClick() }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Crear",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                )
                            }
                        }
                    }
                } else if (isOfflineMode && selectedOfflineTab == LibraryOfflineTab.DOWNLOADS && downloadedTracks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                viewModel.playAll(downloadedTracks)
                                onTrackSelected(downloadedTracks.first())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Reproducir todo",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tabs Row
            if (isOfflineMode) {
                TabRow(
                    selectedTabIndex = selectedOfflineTab.ordinal,
                    containerColor = NoirBlack,
                    contentColor = TextPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedOfflineTab.ordinal]),
                            color = Color.White
                        )
                    },
                    divider = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(DarkGlassBorder)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedOfflineTab == LibraryOfflineTab.DOWNLOADS,
                        onClick = { viewModel.selectOfflineTab(LibraryOfflineTab.DOWNLOADS) },
                        text = {
                            Text(
                                "Descargas (${downloadedTracks.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedOfflineTab == LibraryOfflineTab.DOWNLOADS) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedOfflineTab == LibraryOfflineTab.DOWNLOADS) TextPrimary else TextSecondary
                                )
                            )
                        }
                    )
                    Tab(
                        selected = selectedOfflineTab == LibraryOfflineTab.OFFLINE_PLAYLISTS,
                        onClick = { viewModel.selectOfflineTab(LibraryOfflineTab.OFFLINE_PLAYLISTS) },
                        text = {
                            Text(
                                "Playlists offline (${offlinePlaylists.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedOfflineTab == LibraryOfflineTab.OFFLINE_PLAYLISTS) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedOfflineTab == LibraryOfflineTab.OFFLINE_PLAYLISTS) TextPrimary else TextSecondary
                                )
                            )
                        }
                    )
                }
            } else {
                TabRow(
                    selectedTabIndex = selectedNormalTab.ordinal,
                    containerColor = NoirBlack,
                    contentColor = TextPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedNormalTab.ordinal]),
                            color = Color.White
                        )
                    },
                    divider = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(DarkGlassBorder)
                        )
                    }
                ) {
                    Tab(
                        selected = selectedNormalTab == LibraryNormalTab.PLAYLISTS,
                        onClick = { viewModel.selectNormalTab(LibraryNormalTab.PLAYLISTS) },
                        text = {
                            Text(
                                "Playlists (${playlists.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedNormalTab == LibraryNormalTab.PLAYLISTS) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedNormalTab == LibraryNormalTab.PLAYLISTS) TextPrimary else TextSecondary
                                )
                            )
                        }
                    )
                    Tab(
                        selected = selectedNormalTab == LibraryNormalTab.ARTISTS,
                        onClick = { viewModel.selectNormalTab(LibraryNormalTab.ARTISTS) },
                        text = {
                            Text(
                                "Artistas (${followedArtists.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedNormalTab == LibraryNormalTab.ARTISTS) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedNormalTab == LibraryNormalTab.ARTISTS) TextPrimary else TextSecondary
                                )
                            )
                        }
                    )
                    Tab(
                        selected = selectedNormalTab == LibraryNormalTab.ALBUMS,
                        onClick = { viewModel.selectNormalTab(LibraryNormalTab.ALBUMS) },
                        text = {
                            Text(
                                "Álbumes (${savedAlbums.size})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selectedNormalTab == LibraryNormalTab.ALBUMS) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedNormalTab == LibraryNormalTab.ALBUMS) TextPrimary else TextSecondary
                                )
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Filter Bar (vertically centered, completely fix placeholder clipping)
            val showSearch = if (isOfflineMode) {
                (selectedOfflineTab == LibraryOfflineTab.DOWNLOADS && downloadedTracks.isNotEmpty()) ||
                        (selectedOfflineTab == LibraryOfflineTab.OFFLINE_PLAYLISTS && offlinePlaylists.isNotEmpty())
            } else {
                (selectedNormalTab == LibraryNormalTab.PLAYLISTS && playlists.isNotEmpty()) ||
                        (selectedNormalTab == LibraryNormalTab.ARTISTS && followedArtists.isNotEmpty()) ||
                        (selectedNormalTab == LibraryNormalTab.ALBUMS && savedAlbums.isNotEmpty())
            }

            if (showSearch) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ChipContainer)
                        .border(1.dp, DarkGlassBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        BasicTextField(
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (filterQuery.isEmpty()) {
                                        Text(
                                            text = "Filtrar en biblioteca...",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = TextTertiary,
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Body Content
            if (isOfflineMode) {
                // OFFLINE CONTENT
                when (selectedOfflineTab) {
                    LibraryOfflineTab.DOWNLOADS -> {
                        val filteredTracks = remember(downloadedTracks, filterQuery) {
                            if (filterQuery.isBlank()) downloadedTracks
                            else downloadedTracks.filter {
                                it.title.contains(filterQuery, ignoreCase = true) ||
                                        it.artistName.contains(filterQuery, ignoreCase = true)
                            }
                        }

                        if (downloadedTracks.isEmpty()) {
                            EmptyLibraryView(
                                icon = Icons.Rounded.CloudOff,
                                title = "No hay canciones descargadas",
                                subtitle = "Descarga canciones mientras estés online para escucharlas sin internet."
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredTracks, key = { it.id }) { track ->
                                    SwipeToQueueContainer(
                                        onAddToQueue = { viewModel.addToQueue(track) }
                                    ) {
                                        LibraryDownloadedTrackRow(
                                            track = track,
                                            onTrackClick = {
                                                viewModel.playTrack(track, filteredTracks)
                                                onTrackSelected(track)
                                            },
                                            onOptionsClick = { selectedTrackForMenu = track }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LibraryOfflineTab.OFFLINE_PLAYLISTS -> {
                        val filteredOfflinePlaylists = remember(offlinePlaylists, filterQuery) {
                            if (filterQuery.isBlank()) offlinePlaylists
                            else offlinePlaylists.filter { it.name.contains(filterQuery, ignoreCase = true) }
                        }

                        if (offlinePlaylists.isEmpty()) {
                            EmptyLibraryView(
                                icon = Icons.Rounded.QueueMusic,
                                title = "No hay playlists offline",
                                subtitle = "Las playlists que contengan canciones descargadas aparecerán aquí."
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredOfflinePlaylists, key = { it.id }) { playlist ->
                                    OfflinePlaylistCard(
                                        playlist = playlist,
                                        onClick = { onPlaylistClick(playlist.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // NORMAL CONTENT (Playlists, Artistas, Álbumes)
                when (selectedNormalTab) {
                    LibraryNormalTab.PLAYLISTS -> {
                        val filteredPlaylists = remember(playlists, filterQuery) {
                            if (filterQuery.isBlank()) playlists
                            else playlists.filter { it.name.contains(filterQuery, ignoreCase = true) }
                        }

                        if (playlists.isEmpty()) {
                            EmptyLibraryView(
                                icon = Icons.Rounded.QueueMusic,
                                title = "No tienes playlists aún",
                                subtitle = "Crea tu primera playlist o importa una de Spotify."
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredPlaylists, key = { it.id }) { playlist ->
                                    CleanPlaylistCard(
                                        playlist = playlist,
                                        onClick = { onPlaylistClick(playlist.id) }
                                    )
                                }
                            }
                        }
                    }

                    LibraryNormalTab.ARTISTS -> {
                        val filteredArtists = remember(followedArtists, filterQuery) {
                            if (filterQuery.isBlank()) followedArtists
                            else followedArtists.filter { it.name.contains(filterQuery, ignoreCase = true) }
                        }

                        if (followedArtists.isEmpty()) {
                            EmptyLibraryView(
                                icon = Icons.Rounded.Person,
                                title = "No sigues a ningún artista",
                                subtitle = "Busca a tus artistas favoritos y pulsa 'Seguir' para tenerlos a mano."
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredArtists, key = { it.id }) { artist ->
                                    LibraryArtistCard(
                                        artist = artist,
                                        onClick = { onArtistClick(artist) }
                                    )
                                }
                            }
                        }
                    }

                    LibraryNormalTab.ALBUMS -> {
                        val filteredAlbums = remember(savedAlbums, filterQuery) {
                            if (filterQuery.isBlank()) savedAlbums
                            else savedAlbums.filter {
                                it.title.contains(filterQuery, ignoreCase = true) ||
                                        it.artistName.contains(filterQuery, ignoreCase = true)
                            }
                        }

                        if (savedAlbums.isEmpty()) {
                            EmptyLibraryView(
                                icon = Icons.Rounded.Album,
                                title = "No tienes álbumes guardados",
                                subtitle = "Explora álbumes y pulsa 'Guardar' para verlos aquí."
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredAlbums, key = { it.id }) { album ->
                                    LibraryAlbumCard(
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
            TrackOptionsBottomSheet(
                track = track,
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
fun EmptyLibraryView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                color = TextTertiary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CleanPlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
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
                if (!playlist.coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = playlist.coverUrl,
                        contentDescription = playlist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = playlist.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Clean subtitle - no ugly offline badges in normal view per user request
            Text(
                text = "${playlist.trackCount} canciones",
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OfflinePlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
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
                if (!playlist.coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = playlist.coverUrl,
                        contentDescription = playlist.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.70f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DownloadDone,
                            contentDescription = null,
                            tint = BrandEmerald,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Offline",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = playlist.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${playlist.downloadedTrackCount} descargadas",
                fontSize = 12.sp,
                color = BrandEmerald,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LibraryArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
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
                text = "Artista seguido",
                fontSize = 11.sp,
                color = BrandEmerald,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LibraryAlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
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
        }
    }
}

@Composable
fun LibraryDownloadedTrackRow(
    track: Track,
    onTrackClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
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
                .size(48.dp)
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
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.DownloadDone,
                    contentDescription = "Descargada",
                    tint = BrandEmerald,
                    modifier = Modifier.size(13.dp)
                )
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
