package com.ibramusic.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ibramusic.mobile.core.designsystem.AccentRose
import com.ibramusic.mobile.core.designsystem.BarBackground
import com.ibramusic.mobile.core.designsystem.BorderActive
import com.ibramusic.mobile.core.designsystem.BrandEmerald
import com.ibramusic.mobile.core.designsystem.DarkCanvas
import com.ibramusic.mobile.core.designsystem.DarkGlassBorder
import com.ibramusic.mobile.core.designsystem.IbraMusicTheme
import com.ibramusic.mobile.core.designsystem.MiniPlayerBar
import com.ibramusic.mobile.core.designsystem.NoirBlack
import com.ibramusic.mobile.core.designsystem.TextPrimary
import com.ibramusic.mobile.core.designsystem.TextSecondary
import com.ibramusic.mobile.core.designsystem.TextTertiary
import com.ibramusic.mobile.core.navigation.IbraNavHost
import com.ibramusic.mobile.core.navigation.Screen
import com.ibramusic.mobile.core.navigation.bottomNavScreens
import com.ibramusic.mobile.data.sync.SupabaseSyncManager
import com.ibramusic.mobile.feature.player.NowPlayingViewModel
import com.ibramusic.mobile.domain.repository.AuthRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.ibramusic.mobile.feature.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var supabaseSyncManager: SupabaseSyncManager

    @Inject
    lateinit var authRepository: AuthRepository

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onResume() {
        super.onResume()
        supabaseSyncManager.triggerInstantSync()
        lifecycleScope.launch {
            try {
                authRepository.refreshUserProfile()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestNotificationPermission()

        setContent {
            IbraMusicTheme {
                val navController = rememberNavController()
                val nowPlayingViewModel: NowPlayingViewModel = hiltViewModel()
                val settingsViewModel: SettingsViewModel = hiltViewModel()

                val playbackState by nowPlayingViewModel.playbackState.collectAsState()
                val isLiked by nowPlayingViewModel.isLiked.collectAsState()
                val userEmail by settingsViewModel.userEmail.collectAsState()
                val userAvatar by settingsViewModel.userAvatar.collectAsState()
                val userName by settingsViewModel.userName.collectAsState()
                val isGuest by settingsViewModel.isGuest.collectAsState()
                val isOfflineMode by settingsViewModel.isOfflineMode.collectAsState()

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

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val isFullScreenRoute = currentRoute == Screen.NowPlaying.route || currentRoute == Screen.Queue.route

                var showAccountOverlay by remember { mutableStateOf(false) }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NoirBlack),
                    topBar = {
                        if (!isFullScreenRoute) {
                            // Top Bar (56px) matching ibramusic-one.vercel.app
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xF50F0F0F))
                                    .border(width = 0.5.dp, color = Color(0x14FFFFFF))
                                    .statusBarsPadding()
                                    .height(56.dp)
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // App Brand: Logo box + "ibrastream"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_ibrastream_logo),
                                            contentDescription = "ibrastream logo",
                                            tint = Color.White,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Text(
                                        text = "ibrastream",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (-0.5).sp
                                    )
                                }

                                // Top Right Action Icons: Offline indicator + Search + Avatar
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Offline Mode Indicator (Orange cloud with cross in header)
                                    if (isOfflineMode) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0x22F59E0B))
                                                .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    navController.navigate(Screen.Settings.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CloudOff,
                                                contentDescription = "Modo Offline Activo",
                                                tint = Color(0xFFF59E0B),
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }

                                    // Search Icon Button (36x36 rounded-xl bg-white/5)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                navController.navigate(Screen.Search.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Search,
                                            contentDescription = "Buscar",
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Profile Avatar (36x36 rounded-xl bg-white/5)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                            .clickable { showAccountOverlay = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (resolvedAvatarBitmap != null) {
                                            Image(
                                                bitmap = resolvedAvatarBitmap.asImageBitmap(),
                                                contentDescription = "Perfil",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else if (!userAvatar.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = userAvatar,
                                                contentDescription = "Perfil",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else if (!userName.isNullOrEmpty()) {
                                            Text(
                                                text = userName!!.take(2).uppercase(),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Person,
                                                contentDescription = "Perfil",
                                                tint = Color(0xFF9CA3AF),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (!isFullScreenRoute) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                            ) {
                                // MiniPlayer móvil flotante
                                AnimatedVisibility(
                                    visible = playbackState.currentTrack != null,
                                    enter = slideInVertically(initialOffsetY = { it }),
                                    exit = slideOutVertically(targetOffsetY = { it })
                                ) {
                                    MiniPlayerBar(
                                        playbackState = playbackState,
                                        onTogglePlay = nowPlayingViewModel::togglePlayPause,
                                        onSkipNext = nowPlayingViewModel::skipNext,
                                        onClick = { navController.navigate(Screen.NowPlaying.route) },
                                        onToggleFavorite = nowPlayingViewModel::toggleLike,
                                        isFavorite = isLiked
                                    )
                                }

                                // Bottom Navigation Bar (64px) matching ibramusic-one.vercel.app
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .background(Color(0xF50F0F0F))
                                        .border(width = 0.5.dp, color = Color(0x14FFFFFF)),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    bottomNavScreens.forEach { screen ->
                                        val isInsidePlaylistDetail = screen == Screen.Playlists && currentRoute?.startsWith("playlist/") == true
                                        val isSelected = currentRoute == screen.route || isInsidePlaylistDetail

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    if (currentRoute == screen.route) return@clickable
                                                    if (screen == Screen.Playlists && currentRoute?.startsWith("playlist/") == true) {
                                                        navController.navigate(Screen.Playlists.route) {
                                                            popUpTo(navController.graph.findStartDestination().id) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                        }
                                                    } else {
                                                        navController.navigate(screen.route) {
                                                            popUpTo(navController.graph.findStartDestination().id) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) Color.White.copy(alpha = 0.10f) else Color.Transparent)
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                screen.icon?.let { icon ->
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = screen.title,
                                                        modifier = Modifier.size(20.dp),
                                                        tint = if (isSelected) Color.White else Color(0xFF6B7280)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = screen.title,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.2.sp,
                                                color = if (isSelected) Color.White else Color(0xFF52525B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NoirBlack)
                            .padding(
                                top = if (isFullScreenRoute) 0.dp else innerPadding.calculateTopPadding(),
                                bottom = if (isFullScreenRoute) 0.dp else innerPadding.calculateBottomPadding()
                            )
                    ) {
                        IbraNavHost(
                            navController = navController,
                            nowPlayingViewModel = nowPlayingViewModel
                        )
                    }
                }

                // Blueprint 5.1: MobileAccountOverlay
                if (showAccountOverlay) {
                    Dialog(
                        onDismissRequest = { showAccountOverlay = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.80f))
                                .clickable { showAccountOverlay = false }
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color(0xFF121212))
                                    .border(1.dp, BorderActive, RoundedCornerShape(28.dp))
                                    .clickable(enabled = false) {}
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { showAccountOverlay = false },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cerrar",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Avatar grande con borde limpio
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E1E1E))
                                        .border(1.5.dp, if (!isGuest) BrandEmerald.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f), CircleShape),
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
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = "Avatar",
                                            tint = Color.White,
                                            modifier = Modifier.size(46.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (!isGuest) (userName ?: userEmail?.substringBefore("@") ?: "Usuario") else "Invitado",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = userEmail ?: "Sin cuenta conectada",
                                    fontSize = 12.sp,
                                    color = TextTertiary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(if (!isGuest) BrandEmerald else Color(0xFFEAB308), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (!isGuest) "Sincronizado en la nube" else "Modo Local / Invitado",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (!isGuest) BrandEmerald else Color(0xFFEAB308)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Botones de opciones
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            showAccountOverlay = false
                                            navController.navigate(Screen.Stats.route)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x2610B981)),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.BarChart, contentDescription = null, tint = BrandEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Tus Estadísticas", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            showAccountOverlay = false
                                            navController.navigate(Screen.Settings.route)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x14FFFFFF)),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Editar Perfil / Ajustes", fontSize = 13.sp, color = TextPrimary)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            showAccountOverlay = false
                                            navController.navigate(Screen.Playlists.route)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x14FFFFFF)),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.Public, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Playlists Públicas", fontSize = 13.sp, color = TextPrimary)
                                        }
                                    }

                                    if (!isGuest) {
                                        Button(
                                            onClick = {
                                                settingsViewModel.signOut()
                                                showAccountOverlay = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x24EF4444)),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("Cerrar Sesión", fontSize = 13.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
