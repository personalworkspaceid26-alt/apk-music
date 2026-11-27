package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.ArunAudioEngine
import com.example.model.MusicTrack
import com.example.ui.theme.ArunCyan
import com.example.ui.theme.GlassBorderSpecular
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun InteractiveDynamicIsland(
    track: MusicTrack?,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    positionSec: Int,
    spectrumBars: List<Float>,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        if (track == null) {
            // Compact idle pill
            Surface(
                shape = CircleShape,
                color = ObsidianSurface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, GlassBorderSpecular),
                modifier = Modifier
                    .clickable { onTogglePlayPause() }
                    .testTag("dynamic_island_idle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ArunCyan)
                    )
                    Text(
                        text = "Arun Music • Ketuk untuk putar",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
            }
        } else {
            val accent = track.accentColor
            val cornerRadius = if (expanded) 28.dp else 32.dp
            Surface(
                shape = RoundedCornerShape(cornerRadius),
                color = ObsidianSurface.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                modifier = Modifier
                    .widthIn(min = 240.dp, max = if (expanded) 440.dp else 330.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    .clickable { expanded = !expanded }
                    .testTag("dynamic_island_active")
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (expanded) 16.dp else 12.dp,
                        vertical = if (expanded) 14.dp else 8.dp
                    )
                ) {
                    // Top compact / header row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AlbumArtImage(
                            artworkUrl = track.artworkUrl,
                            fallbackRes = track.coverRes,
                            contentDescription = "Mini sampul ${track.title}",
                            modifier = Modifier
                                .size(if (expanded) 44.dp else 30.dp)
                                .clip(if (expanded) RoundedCornerShape(12.dp) else CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = if (expanded) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (expanded) {
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = accent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            SpectrumVisualizer(
                                bars = spectrumBars,
                                isPlaying = isPlaying,
                                accentColor = accent,
                                barCount = if (expanded) 7 else 5,
                                maxHeight = if (expanded) 22.dp else 16.dp,
                                barWidth = 3.dp
                            )
                        }

                        Icon(
                            imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (expanded) "Tutup kontrol Dynamic Island" else "Perluas kontrol Dynamic Island",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Expanded quick controls
                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            val progress = if (track.durationSec > 0) {
                                positionSec.toFloat() / track.durationSec.toFloat()
                            } else 0f

                            if (isBuffering) {
                                LinearProgressIndicator(
                                    color = accent,
                                    trackColor = Color.White.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0f, 1f) },
                                    color = accent,
                                    trackColor = Color.White.copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = ArunAudioEngine.formatDuration(positionSec),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                Text(
                                    text = ArunAudioEngine.formatDuration(track.durationSec),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PlatformBadge(platform = track.platform)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = onPrevious,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SkipPrevious,
                                            contentDescription = "Sebelumnya",
                                            tint = TextPrimary
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = accent,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .clickable(onClick = onTogglePlayPause)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isBuffering) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    color = Color(0xFF041421),
                                                    strokeWidth = 2.5.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                                                    tint = Color(0xFF041421)
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = onNext,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SkipNext,
                                            contentDescription = "Berikutnya",
                                            tint = TextPrimary
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, GlassBorderSpecular),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            expanded = false
                                            onOpenFullPlayer()
                                        }
                                        .testTag("dynamic_island_open_full")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.OpenInFull,
                                            contentDescription = "Buka pemutar penuh",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Penuh",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
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
