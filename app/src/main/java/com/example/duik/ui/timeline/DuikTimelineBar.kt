package com.example.duik.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duik.math.DuikEasingType
import com.example.duik.viewmodel.DuikUiState

@Composable
fun DuikTimelineBar(
  uiState: DuikUiState,
  onTogglePlay: () -> Unit,
  onStepFrame: (Int) -> Unit,
  onSetFrame: (Int) -> Unit,
  onRecordKeyframe: (DuikEasingType) -> Unit,
  onDeleteKeyframe: () -> Unit,
  modifier: Modifier = Modifier
) {
  val selectedBone = uiState.bones.find { it.id == uiState.selectedBoneId }
  val selectedLayer = uiState.layers.find { it.id == uiState.selectedLayerId }
  val activeTargetId = uiState.selectedLayerId ?: uiState.selectedBoneId
  val currentFrame = uiState.currentFrame
  val totalFrames = uiState.clip.totalFrames

  // Collect keyframe frames for selected layer or bone
  val activeKeyframeFrames = uiState.clip.tracks
    .filter { it.targetId == activeTargetId }
    .flatMap { it.keyframes }
    .map { it.frame }
    .toSet()

  val hasKeyframeAtCurrent = activeKeyframeFrames.contains(currentFrame)

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = Color(0xFF14171F),
    tonalElevation = 6.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      // 1. Timecode and Selected Bone Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(if (uiState.isPlaying) Color(0xFF00E676) else Color(0xFFFFB300))
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Frame $currentFrame / $totalFrames",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "${uiState.clip.fps} FPS",
            color = Color(0xFF00E5FF),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xFF00363D))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }

        Text(
          text = when {
            selectedLayer != null -> "Layer: ${selectedLayer.name}"
            selectedBone != null -> "Bone: ${selectedBone.name}"
            else -> "Select Layer or Bone"
          },
          color = Color(0xFF8E95A8),
          fontSize = 12.sp,
          maxLines = 1
        )
      }

      // 2. Interactive Time Ruler / Scrubber Slider
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(38.dp),
        contentAlignment = Alignment.Center
      ) {
        Slider(
          value = currentFrame.toFloat(),
          onValueChange = { onSetFrame(it.toInt()) },
          valueRange = 0f..(totalFrames - 1).coerceAtLeast(1).toFloat(),
          steps = totalFrames - 2,
          colors = SliderDefaults.colors(
            thumbColor = Color(0xFF00E5FF),
            activeTrackColor = Color(0xFF00E5FF),
            inactiveTrackColor = Color(0xFF242938)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("timeline_slider")
        )
      }

      // 3. Transport Controls & Duik Keyframe Tools
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Transport playback controls
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { onSetFrame(0) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Go to Start", tint = Color(0xFF8E95A8), modifier = Modifier.size(20.dp))
          }

          IconButton(
            onClick = { onStepFrame(-1) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.FastRewind, contentDescription = "Step -1 Frame", tint = Color(0xFF8E95A8), modifier = Modifier.size(20.dp))
          }

          // Play/Pause Main Button
          IconButton(
            onClick = onTogglePlay,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(if (uiState.isPlaying) Color(0xFFFF4081) else Color(0xFF00E676))
              .testTag("play_pause_button")
          ) {
            Icon(
              imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (uiState.isPlaying) "Pause" else "Play",
              tint = Color(0xFF101217),
              modifier = Modifier.size(26.dp)
            )
          }

          IconButton(
            onClick = { onStepFrame(1) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.FastForward, contentDescription = "Step +1 Frame", tint = Color(0xFF8E95A8), modifier = Modifier.size(20.dp))
          }

          IconButton(
            onClick = { onSetFrame(totalFrames - 1) },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.SkipNext, contentDescription = "Go to End", tint = Color(0xFF8E95A8), modifier = Modifier.size(20.dp))
          }
        }

        // Keyframe and Easing action buttons
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Record Keyframe button
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (hasKeyframeAtCurrent) Color(0xFFFFB300) else Color(0xFF1E2330),
            modifier = Modifier
              .clickable { onRecordKeyframe(DuikEasingType.EASE_IN_OUT) }
              .testTag("record_keyframe_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "◆",
                color = if (hasKeyframeAtCurrent) Color(0xFF1A1C24) else Color(0xFFFFB300),
                fontSize = 14.sp
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (hasKeyframeAtCurrent) "Keyed" else "+ Key",
                color = if (hasKeyframeAtCurrent) Color(0xFF1A1C24) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          if (hasKeyframeAtCurrent) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF3B1E28),
              modifier = Modifier
                .clickable { onDeleteKeyframe() }
                .testTag("delete_keyframe_button")
            ) {
              Text(
                text = "✕",
                color = Color(0xFFFF5252),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
              )
            }
          }
        }
      }

      // 4. Duik Angela Easing selector chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        DuikEasingType.values().forEach { easing ->
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF1B1F2A),
            modifier = Modifier.clickable { onRecordKeyframe(easing) }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = easing.iconName, color = Color(0xFF00E5FF), fontSize = 12.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = easing.displayName, color = Color(0xFFB0B7C6), fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}
