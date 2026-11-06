package com.ibramusic.mobile.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ibramusic.mobile.core.designsystem.BrandEmerald
import com.ibramusic.mobile.core.designsystem.DarkCanvas
import com.ibramusic.mobile.core.designsystem.DarkCard
import com.ibramusic.mobile.core.designsystem.DarkGlassBorder
import com.ibramusic.mobile.core.designsystem.GlassCard
import com.ibramusic.mobile.core.designsystem.Zinc400
import com.ibramusic.mobile.core.designsystem.Zinc50
import com.ibramusic.mobile.core.designsystem.Zinc600
import com.ibramusic.mobile.core.designsystem.Zinc800
import com.ibramusic.mobile.feature.common.EqualizerBottomSheet

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToStats: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val audioQuality by viewModel.audioQuality.collectAsState()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsState()
    val crossfadeDuration by viewModel.crossfadeDuration.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val resolvedAvatarBitmap = remember(userAvatar) {
        if (userAvatar != null && (userAvatar!!.startsWith("data:image", ignoreCase = true) || (userAvatar!!.length > 200 && !userAvatar!!.startsWith("http", ignoreCase = true) && !userAvatar!!.startsWith("file:", ignoreCase = true)))) {
            try {
                val base64Data = if (userAvatar!!.contains(",")) userAvatar!!.substringAfter(",") else userAvatar!!
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    val authError by viewModel.authError.collectAsState()
    val isLoadingAuth by viewModel.isLoadingAuth.collectAsState()
    val activePreset by viewModel.equalizerManager.activePreset.collectAsState()
    val isManualOfflineMode by viewModel.isManualOfflineMode.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()

    var showEqualizerSheet by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCanvas)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Zinc50,
                    letterSpacing = (-0.5).sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Section: Estadísticas
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToStats() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x2610B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = null,
                                tint = BrandEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Tus Estadísticas",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = Zinc50,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Minutos escuchados, podio de artistas y canciones",
                                style = MaterialTheme.typography.bodySmall.copy(color = Zinc400)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Zinc400
                    )
                }
            }
        }

        // Section: Supabase Account & Cloud Sync
        item {
            Text(
                text = "Supabase Account",
                style = MaterialTheme.typography.titleSmall.copy(color = Zinc400),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isGuest) {
                        Text(
                            text = "Guest Mode",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Zinc50,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "Connect to Supabase to sync your liked songs and playlists across devices.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Zinc400)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("Email", color = Zinc600) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkCard,
                                unfocusedContainerColor = DarkCard,
                                focusedBorderColor = Zinc50,
                                unfocusedBorderColor = DarkGlassBorder,
                                focusedTextColor = Zinc50,
                                unfocusedTextColor = Zinc50
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            placeholder = { Text("Password", color = Zinc600) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkCard,
                                unfocusedContainerColor = DarkCard,
                                focusedBorderColor = Zinc50,
                                unfocusedBorderColor = DarkGlassBorder,
                                focusedTextColor = Zinc50,
                                unfocusedTextColor = Zinc50
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!authError.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = authError!!,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFEF4444))
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isLoadingAuth) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Zinc50, modifier = Modifier.size(24.dp))
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.signIn(emailInput, passwordInput) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Zinc50, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Sign In", fontWeight = FontWeight.SemiBold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.signUp(emailInput, passwordInput) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc50),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Sign Up")
                                }
                            }
                        }
                    } else {
                        // Logged in user with Avatar and Profile header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E1E1E))
                                    .border(1.5.dp, BrandEmerald.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (resolvedAvatarBitmap != null) {
                                    Image(
                                        bitmap = resolvedAvatarBitmap.asImageBitmap(),
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (!userAvatar.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = userAvatar,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (!userName.isNullOrEmpty()) {
                                    Text(
                                        text = userName!!.take(2).uppercase(),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = "Avatar",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = if (!userName.isNullOrEmpty()) userName!! else (userEmail?.substringBefore("@") ?: "Usuario"),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Zinc50,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = userEmail ?: "Cuenta sincronizada",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Zinc400)
                                )
                                Text(
                                    text = "Sincronizado con IbraMusic Web (user_sync)",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF10B981))
                                )
                            }
                        }

                        val syncStatus by viewModel.syncStatus.collectAsState()
                        if (!syncStatus.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = syncStatus!!,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF10B981))
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = viewModel::syncNow,
                                colors = ButtonDefaults.buttonColors(containerColor = Zinc50, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Sincronizar ahora", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = viewModel::signOut,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33EF4444), contentColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cerrar sesión")
                            }
                        }
                    }
                }
            }
        }

        // Section: Playback
        item {
            Text(
                text = "Playback",
                style = MaterialTheme.typography.titleSmall.copy(color = Zinc400),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Audio Stream Engine",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Zinc50, fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "High-Fidelity 320kbps Studio Streaming Active",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF10B981))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Crossfade",
                                style = MaterialTheme.typography.bodyLarge.copy(color = Zinc50)
                            )
                            Text(
                                text = "Allows smooth transition between songs",
                                style = MaterialTheme.typography.bodySmall.copy(color = Zinc400)
                            )
                        }

                        Switch(
                            checked = crossfadeEnabled,
                            onCheckedChange = viewModel::setCrossfadeEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Zinc50,
                                checkedTrackColor = Zinc800
                            )
                        )
                    }

                    if (crossfadeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Duration: ${crossfadeDuration}s",
                            style = MaterialTheme.typography.bodySmall.copy(color = Zinc400)
                        )
                        Slider(
                            value = crossfadeDuration.toFloat(),
                            onValueChange = { viewModel.setCrossfadeDuration(it.toInt()) },
                            valueRange = 1f..12f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = Zinc50,
                                activeTrackColor = Zinc50
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Modo offline",
                                style = MaterialTheme.typography.bodyLarge.copy(color = Zinc50)
                            )
                            Text(
                                text = if (isManualOfflineMode) "Modo offline manual activado"
                                       else if (!isOnline) "Sin conexión a internet (modo offline automático)"
                                       else "Reproducir y mostrar únicamente música descargada",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (!isOnline) Color(0xFFF59E0B) else Zinc400
                                )
                            )
                        }

                        Switch(
                            checked = isManualOfflineMode,
                            onCheckedChange = viewModel::setManualOfflineMode,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Zinc50,
                                checkedTrackColor = Zinc800
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Equalizer & Audio Presets Shortcut Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x221E1E1E))
                            .border(1.dp, DarkGlassBorder, RoundedCornerShape(12.dp))
                            .clickable { showEqualizerSheet = true }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = BrandEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Ecualizador & Audio",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Zinc50
                                    )
                                )
                                Text(
                                    text = "Perfil activo: ${activePreset.title}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = BrandEmerald)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Zinc400
                        )
                    }
                }
            }
        }

        // Section: Appearance
        item {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleSmall.copy(color = Zinc400),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Zinc50)
                    )
                    Text(
                        text = "Active: ${theme.replaceFirstChar { it.uppercase() }} Canvas",
                        style = MaterialTheme.typography.bodySmall.copy(color = Zinc400)
                    )
                }
            }
        }

        // Section: App Info
        item {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleSmall.copy(color = Zinc400),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "IbraMusic Mobile v1.0.0",
                        style = MaterialTheme.typography.bodyLarge.copy(color = Zinc50, fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Native Android Media3 / ExoPlayer Engine",
                        style = MaterialTheme.typography.bodySmall.copy(color = Zinc400)
                    )
                }
            }
        }
    }

    if (showEqualizerSheet) {
        EqualizerBottomSheet(
            equalizerManager = viewModel.equalizerManager,
            onDismiss = { showEqualizerSheet = false }
        )
    }
}
