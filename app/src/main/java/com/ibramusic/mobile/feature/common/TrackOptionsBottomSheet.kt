package com.ibramusic.mobile.feature.common

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.ibramusic.mobile.core.designsystem.AccentRose
import com.ibramusic.mobile.core.designsystem.BarBackground
import com.ibramusic.mobile.core.designsystem.BorderActive
import com.ibramusic.mobile.core.designsystem.ChipContainer
import com.ibramusic.mobile.core.designsystem.DarkCard
import com.ibramusic.mobile.core.designsystem.DarkGlassBorder
import com.ibramusic.mobile.core.designsystem.TextPrimary
import com.ibramusic.mobile.core.designsystem.TextSecondary
import com.ibramusic.mobile.core.designsystem.TextTertiary
import com.ibramusic.mobile.domain.model.Playlist
import com.ibramusic.mobile.domain.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsBottomSheet(
    track: Track,
    isFavorite: Boolean = false,
    playlists: List<Playlist> = emptyList(),
    isInPlaylist: Boolean = false,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (playlistId: String) -> Unit,
    onCreatePlaylistAndAdd: ((name: String) -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onSetPlaybackSpeed: (() -> Unit)? = null,
    onSetSleepTimer: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
    onDownloadTrack: (() -> Unit)? = null,
    onDeleteDownload: (() -> Unit)? = null,
    onOpenQueue: (() -> Unit)? = null,
    onGoToArtist: ((artistName: String, artistId: String?) -> Unit)? = null,
    onGoToAlbum: ((albumTitle: String, artistName: String, albumId: String?) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BarBackground,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 36.dp)
        ) {
            // Track Info Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = track.title,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCard)
                        .border(1.dp, BorderActive, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artistName,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Divider(color = DarkGlassBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

            if (!showPlaylistPicker) {
                // Blueprint 6.1: TrackContextMenu actions

                // Favorite action
                ActionRow(
                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    title = if (isFavorite) "Quitar de Favoritos" else "Añadir a Favoritos",
                    iconTint = if (isFavorite) AccentRose else TextSecondary,
                    onClick = {
                        onToggleFavorite()
                        onDismiss()
                    }
                )

                // Open Queue action
                if (onOpenQueue != null) {
                    ActionRow(
                        icon = Icons.Rounded.QueueMusic,
                        title = "Ver cola de reproducción",
                        iconTint = TextSecondary,
                        onClick = {
                            onDismiss()
                            onOpenQueue()
                        }
                    )
                }

                // Add to Queue action
                ActionRow(
                    icon = Icons.Rounded.QueueMusic,
                    title = "Añadir a la Cola",
                    iconTint = TextSecondary,
                    onClick = {
                        onAddToQueue()
                        onDismiss()
                    }
                )

                // Play next action
                ActionRow(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = "Reproducir Siguiente",
                    iconTint = TextSecondary,
                    onClick = {
                        onPlayNext()
                        onDismiss()
                    }
                )

                // Add to Playlist action
                ActionRow(
                    icon = Icons.Rounded.PlaylistAdd,
                    title = "Añadir a Playlist",
                    iconTint = TextSecondary,
                    onClick = {
                        showPlaylistPicker = true
                    }
                )

                // Offline download action
                if (isDownloaded) {
                    ActionRow(
                        icon = Icons.Rounded.Delete,
                        title = "Eliminar descarga del dispositivo",
                        iconTint = AccentRose,
                        onClick = {
                            onDismiss()
                            onDeleteDownload?.invoke() ?: onDownloadTrack?.invoke()
                        }
                    )
                } else {
                    ActionRow(
                        icon = Icons.Rounded.Download,
                        title = "Descargar en dispositivo",
                        iconTint = TextSecondary,
                        onClick = {
                            onDismiss()
                            onDownloadTrack?.invoke()
                        }
                    )
                }

                // Share link action
                ActionRow(
                    icon = Icons.Rounded.Share,
                    title = "Compartir Enlace",
                    iconTint = TextSecondary,
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Escucha \"${track.title}\" de ${track.artistName} en ibraStream")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir con"))
                        onDismiss()
                    }
                )

                // Go to Artist
                if (track.artistName.isNotBlank() && onGoToArtist != null) {
                    ActionRow(
                        icon = Icons.Rounded.Person,
                        title = "Ir al artista (${track.artistName})",
                        iconTint = TextSecondary,
                        onClick = {
                            onDismiss()
                            onGoToArtist(track.artistName, track.artists.firstOrNull()?.id)
                        }
                    )
                }

                // Go to Album
                if (!track.album?.title.isNullOrEmpty() && onGoToAlbum != null) {
                    ActionRow(
                        icon = Icons.Rounded.Album,
                        title = "Ir al álbum (${track.album!!.title})",
                        iconTint = TextSecondary,
                        onClick = {
                            onDismiss()
                            onGoToAlbum(track.album!!.title, track.artistName, track.album!!.id)
                        }
                    )
                }

                // Playback speed action
                if (onSetPlaybackSpeed != null) {
                    ActionRow(
                        icon = Icons.Rounded.Speed,
                        title = "Velocidad de reproducción",
                        iconTint = TextSecondary,
                        onClick = {
                            onDismiss()
                            onSetPlaybackSpeed()
                        }
                    )
                }

                // Sleep timer action
                if (onSetSleepTimer != null) {
                    ActionRow(
                        icon = Icons.Rounded.Schedule,
                        title = "Temporizador de apagado",
                        iconTint = TextSecondary,
                        onClick = {
                            onDismiss()
                            onSetSleepTimer()
                        }
                    )
                }

                // Remove from playlist (contextual)
                if (isInPlaylist && onRemoveFromPlaylist != null) {
                    ActionRow(
                        icon = Icons.Rounded.Delete,
                        title = "Eliminar de esta playlist",
                        iconTint = Color(0xFFEF4444),
                        onClick = {
                            onRemoveFromPlaylist()
                            onDismiss()
                        }
                    )
                }
            } else {
                // Subview: Pick a playlist
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seleccionar Playlist",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(ChipContainer)
                            .clickable { showPlaylistPicker = false }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Volver", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Create new playlist option
                ActionRow(
                    icon = Icons.Rounded.Add,
                    title = "Nueva playlist...",
                    iconTint = TextPrimary,
                    onClick = {
                        showCreatePlaylistDialog = true
                    }
                )

                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tienes playlists creadas todavía",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddToPlaylist(playlist.id)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkCard)
                                        .border(1.dp, DarkGlassBorder, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlaylistPlay,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${playlist.trackCount} canciones",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        Dialog(onDismissRequest = {
            showCreatePlaylistDialog = false
            newPlaylistName = ""
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF121212))
                    .border(1.dp, BorderActive, RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nueva Playlist",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        IconButton(
                            onClick = {
                                showCreatePlaylistDialog = false
                                newPlaylistName = ""
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = TextSecondary)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ChipContainer)
                            .border(1.dp, BorderActive, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        TextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text("Nombre de la playlist...", color = TextTertiary) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.dp, BorderActive, RoundedCornerShape(24.dp))
                                .clickable {
                                    showCreatePlaylistDialog = false
                                    newPlaylistName = ""
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancelar", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                                .clickable {
                                    if (newPlaylistName.isNotBlank()) {
                                        onCreatePlaylistAndAdd?.invoke(newPlaylistName.trim())
                                        showCreatePlaylistDialog = false
                                        newPlaylistName = ""
                                        onDismiss()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Crear", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    iconTint: Color = TextSecondary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        )
    }
}
