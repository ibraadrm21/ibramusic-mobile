package com.ibramusic.mobile.feature.common

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ibramusic.mobile.core.designsystem.BrandEmerald
import com.ibramusic.mobile.core.designsystem.DarkCard
import com.ibramusic.mobile.core.designsystem.DarkGlassBorder
import com.ibramusic.mobile.core.designsystem.NoirBlack
import com.ibramusic.mobile.core.designsystem.TextPrimary
import com.ibramusic.mobile.core.designsystem.TextSecondary
import com.ibramusic.mobile.core.designsystem.TextTertiary
import com.ibramusic.mobile.core.designsystem.Zinc400
import com.ibramusic.mobile.core.designsystem.Zinc50
import com.ibramusic.mobile.core.designsystem.Zinc600
import com.ibramusic.mobile.playback.audiofx.EqualizerManager
import com.ibramusic.mobile.playback.audiofx.SoundPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
    equalizerManager: EqualizerManager,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled by equalizerManager.enabled.collectAsState()
    val activePreset by equalizerManager.activePreset.collectAsState()
    val bandLevels by equalizerManager.bandLevels.collectAsState()
    val bassStrength by equalizerManager.bassStrength.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkCard,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x2210B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = BrandEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Ecualizador & Audio",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = activePreset.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { equalizerManager.setEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = BrandEmerald,
                            uncheckedThumbColor = Zinc400,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Presets Horizontal Row
            Text(
                text = "PERFILES DE SONIDO",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Zinc400,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SoundPreset.values()) { preset ->
                    val isSelected = activePreset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) BrandEmerald
                                else Color(0x1AFFFFFF)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) BrandEmerald else DarkGlassBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { equalizerManager.selectPreset(preset) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color.Black else TextPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bass Boost Slider Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Waves,
                        contentDescription = null,
                        tint = BrandEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Bass Boost (Graves)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                }
                Text(
                    text = "${(bassStrength / 10)}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandEmerald
                    )
                )
            }

            Slider(
                value = bassStrength.toFloat(),
                onValueChange = { equalizerManager.setBassStrength(it.toInt()) },
                valueRange = 0f..1000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = BrandEmerald,
                    inactiveTrackColor = Color(0x22FFFFFF)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5-Band Equalizer Frequency Sliders
            Text(
                text = "ECUALIZADOR GRÁFICO (5 BANDAS)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Zinc400,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            val bandLabels = listOf("60 Hz\n(Bajos)", "230 Hz\n(Medio-bajo)", "910 Hz\n(Medios)", "3.6 kHz\n(Voces)", "14 kHz\n(Agudos)")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x1A000000), RoundedCornerShape(12.dp))
                    .border(1.dp, DarkGlassBorder, RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                bandLevels.take(5).forEachIndexed { index, level ->
                    val dbValue = level / 100 // level is in millibels (-1000 to +1000 = -10dB to +10dB)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (dbValue > 0) "+${dbValue}dB" else "${dbValue}dB",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (dbValue != 0) BrandEmerald else TextSecondary,
                                fontSize = 10.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Custom vertical-like stepper or slider
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF))
                                    .clickable {
                                        val newLvl = (level + 150).coerceAtMost(1000)
                                        equalizerManager.setBandLevel(index, newLvl)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            // Visual vertical level meter
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x33FFFFFF)),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                val normalizedHeight = ((level + 1000f) / 2000f).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((56 * normalizedHeight).dp)
                                        .background(
                                            if (dbValue > 0) BrandEmerald
                                            else if (dbValue < 0) Color(0xFFEF4444)
                                            else Zinc400
                                        )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF))
                                    .clickable {
                                        val newLvl = (level - 150).coerceAtLeast(-1000)
                                        equalizerManager.setBandLevel(index, newLvl)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = bandLabels.getOrElse(index) { "B$index" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 9.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

