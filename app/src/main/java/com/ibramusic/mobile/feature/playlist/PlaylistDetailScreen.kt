package com.ibramusic.mobile.feature.playlist

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.unit.sp
import com.ibramusic.mobile.core.designsystem.AccentRose
import com.ibramusic.mobile.core.designsystem.BrandEmerald
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ibramusic.mobile.core.designsystem.BrandEmerald
import com.ibramusic.mobile.core.designsystem.DarkCanvas
import com.ibramusic.mobile.core.designsystem.DarkCard
import com.ibramusic.mobile.core.designsystem.DarkGlassBorder
import com.ibramusic.mobile.core.designsystem.Zinc400
import com.ibramusic.mobile.core.designsystem.Zinc50
import com.ibramusic.mobile.core.designsystem.Zinc800
import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track
import com.ibramusic.mobile.feature.common.SwipeToQueueContainer
import com.ibramusic.mobile.feature.common.TrackOptionsBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    viewModel: PlaylistViewModel,
    onBack: () -> Unit,
    onGoToArtist: (String, String?) -> Unit = { _, _ -> },
    onGoToAlbum: (String, String, String?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    LaunchedEffect(playlistId) {
        viewModel.selectPlaylist(playlistId)
    }

    val playlists by viewModel.playlists.collectAsState()
    val playlist = playlists.find { it.id == playlistId }
    val tracks by viewModel.currentPlaylistTracks.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()

    val effectiveCoverUrl = playlist?.coverUrl.takeIf { !it.isNullOrEmpty() }
        ?: tracks.firstOrNull()?.artworkUrl

    var selectedTrackForMenu by remember { mutableStateOf<Track?>(null) }
    var trackPendingDeleteFromPlaylist by remember { mutableStateOf<Track?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember(playlist?.name) { mutableStateOf(playlist?.name ?: "") }
    var editCoverUrl by remember(playlist?.coverUrl) { mutableStateOf(playlist?.coverUrl ?: "") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Volver",
                        tint = Zinc50
                    )
                }

                Text(
                    text = playlist?.name ?: "Playlist",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Zinc50
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center
                )

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Opciones",
                            tint = Zinc50
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(DarkCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar playlist", color = Zinc50) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = Zinc50
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                editName = playlist?.name ?: ""
                                editCoverUrl = playlist?.coverUrl ?: ""
                                showEditDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Descargar playlist", color = Zinc50) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = BrandEmerald
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                viewModel.downloadPlaylist(tracks)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar playlist", color = Color(0xFFEF4444)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444)
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                viewModel.deletePlaylist(playlistId)
                                onBack()
                            }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Hero Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Artwork Cover
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF27272A), Color(0xFF18181B))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!effectiveCoverUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = effectiveCoverUrl,
                                    contentDescription = playlist?.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = Zinc400,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = playlist?.name ?: "Playlist",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Zinc50
                            ),
                            textAlign = TextAlign.Center
                        )

                        if (!playlist?.description.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = playlist?.description ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Zinc400),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val downloadedCount = remember(tracks, activeDownloads) {
                            tracks.count { it.availableOffline || activeDownloads[it.id] == 100 }
                        }
                        val allDownloaded = tracks.isNotEmpty() && downloadedCount == tracks.size
                        val subtitleText = remember(tracks.size, downloadedCount) {
                            when {
                                tracks.isEmpty() -> "0 canciones"
                                downloadedCount == tracks.size -> "${tracks.size} canciones • Descargada"
                                downloadedCount > 0 -> "${tracks.size} canciones • $downloadedCount descargadas"
                                else -> "${tracks.size} canciones"
                            }
                        }

                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (downloadedCount > 0) BrandEmerald else Zinc400,
                                fontWeight = if (downloadedCount > 0) FontWeight.Medium else FontWeight.Normal
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Play, Shuffle & Download Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.playAll(tracks, shuffle = false) },
                                enabled = tracks.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Zinc50,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Reproducir",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reproducir",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.playAll(tracks, shuffle = true) },
                                enabled = tracks.isNotEmpty(),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = "Aleatorio",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Aleatorio",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.downloadPlaylist(tracks) },
                                enabled = tracks.isNotEmpty() && !allDownloaded,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandEmerald),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (allDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                                    contentDescription = "Descargar playlist",
                                    tint = BrandEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        allDownloaded -> "Descargada"
                                        downloadedCount > 0 -> "Descargar resto (${tracks.size - downloadedCount})"
                                        else -> "Descargar"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = BrandEmerald),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Empty State or Track Items
                if (tracks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Esta playlist está vacía.\nBusca canciones y selecciona 'Añadir a playlist' para añadir tus favoritas.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Zinc400),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                        SwipeToQueueContainer(
                            onAddToQueue = { viewModel.addToQueue(track) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.playTrack(track, tracks) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Zinc400,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.width(24.dp)
                                )

                                AsyncImage(
                                    model = track.artworkUrl,
                                    contentDescription = track.title,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkCard),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    val dlProgress = activeDownloads[track.id]
                                    if (dlProgress != null && dlProgress in 1..99) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                progress = { dlProgress / 100f },
                                                color = BrandEmerald,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Descargando $dlProgress%",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = BrandEmerald,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    } else if (dlProgress == -1) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ErrorOutline,
                                                contentDescription = null,
                                                tint = AccentRose,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Error al descargar",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = AccentRose,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = Zinc50
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (track.availableOffline || dlProgress == 100) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.DownloadDone,
                                                contentDescription = "Descargada",
                                                tint = BrandEmerald,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = track.artistName,
                                        style = MaterialTheme.typography.bodySmall.copy(color = Zinc400),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { selectedTrackForMenu = track },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MoreVert,
                                        contentDescription = "Opciones",
                                        tint = Zinc400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Track Options Bottom Sheet
        selectedTrackForMenu?.let { track ->
            val isDownloaded = track.availableOffline || (activeDownloads[track.id] ?: 0) == 100
            TrackOptionsBottomSheet(
                track = track,
                playlists = playlists,
                isInPlaylist = true,
                isDownloaded = isDownloaded,
                onDismiss = { selectedTrackForMenu = null },
                onPlayNext = { viewModel.playNext(track) },
                onAddToQueue = { viewModel.addToQueue(track) },
                onToggleFavorite = { viewModel.toggleLike(track) },
                onAddToPlaylist = { targetPlaylistId ->
                    viewModel.addTrackToPlaylist(targetPlaylistId, track)
                },
                onCreatePlaylistAndAdd = { name ->
                    viewModel.createPlaylist(name) { newId ->
                        viewModel.addTrackToPlaylist(newId, track)
                    }
                },
                onRemoveFromPlaylist = {
                    if (isDownloaded) {
                        trackPendingDeleteFromPlaylist = track
                    } else {
                        viewModel.removeTrackFromPlaylist(playlistId, track.id)
                    }
                },
                onDownloadTrack = {
                    viewModel.downloadTrack(track)
                },
                onDeleteDownload = {
                    viewModel.deleteDownload(track.id)
                },
                onGoToArtist = onGoToArtist,
                onGoToAlbum = onGoToAlbum
            )
        }

        // Prompt to delete downloaded file when removing from playlist
        if (trackPendingDeleteFromPlaylist != null) {
            val trackToDelete = trackPendingDeleteFromPlaylist!!
            AlertDialog(
                onDismissRequest = { trackPendingDeleteFromPlaylist = null },
                containerColor = Color(0xFF18181B),
                title = {
                    Text(
                        text = "Eliminar canción",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                text = {
                    Text(
                        text = "¿Deseas eliminar también el archivo descargado de tu dispositivo o solo quitarla de la playlist \"${playlist?.name ?: "Playlist"}\"?",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9CA3AF))
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.removeTrackFromPlaylist(playlistId, trackToDelete.id)
                            viewModel.deleteDownload(trackToDelete.id)
                            trackPendingDeleteFromPlaylist = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Eliminar de ambos", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { trackPendingDeleteFromPlaylist = null }
                        ) {
                            Text("Cancelar", color = Color(0xFF9CA3AF))
                        }
                        TextButton(
                            onClick = {
                                viewModel.removeTrackFromPlaylist(playlistId, trackToDelete.id)
                                trackPendingDeleteFromPlaylist = null
                            }
                        ) {
                            Text("Solo de la playlist", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )
        }
        // Edit Playlist Dialog
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                containerColor = DarkCard,
                title = {
                    Text(
                        text = "Editar playlist",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Zinc50
                        )
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Nombre de la playlist", color = Zinc400) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Zinc50,
                                unfocusedTextColor = Zinc50,
                                focusedContainerColor = Color(0x331E1E1E),
                                unfocusedContainerColor = Color(0x331E1E1E),
                                cursorColor = BrandEmerald,
                                focusedIndicatorColor = BrandEmerald,
                                unfocusedIndicatorColor = DarkGlassBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = editCoverUrl,
                            onValueChange = { editCoverUrl = it },
                            label = { Text("URL de imagen de portada (opcional)", color = Zinc400) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Zinc50,
                                unfocusedTextColor = Zinc50,
                                focusedContainerColor = Color(0x331E1E1E),
                                unfocusedContainerColor = Color(0x331E1E1E),
                                cursorColor = BrandEmerald,
                                focusedIndicatorColor = BrandEmerald,
                                unfocusedIndicatorColor = DarkGlassBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "Si no defines una URL, se usará automáticamente la carátula de la primera canción.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Zinc400)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editName.isNotBlank()) {
                                viewModel.updatePlaylist(playlistId, editName, editCoverUrl)
                                showEditDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandEmerald)
                    ) {
                        Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancelar", color = Zinc400)
                    }
                }
            )
        }
    }
}

