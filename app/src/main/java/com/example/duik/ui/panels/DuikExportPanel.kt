package com.example.duik.ui.panels

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duik.export.ExportSettings
import com.example.duik.export.Mp4AnimationExporter
import com.example.duik.presets.DuikPreset
import com.example.duik.presets.DuikPresets
import com.example.duik.viewmodel.DuikUiState

@Composable
fun DuikExportPanel(
  uiState: DuikUiState,
  onSelectPreset: (DuikPreset) -> Unit,
  onExportMp4: (ExportSettings) -> Unit,
  onExportJson: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  var resolutionSize by remember { mutableIntStateOf(720) }
  var loopCount by remember { mutableIntStateOf(2) }
  var includeBonesInVideo by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFF141720))
      .verticalScroll(rememberScrollState())
      .padding(12.dp)
  ) {
    // 1. MP4 Video Export Section
    Card(
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1B202D)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(text = "Export MP4 Video", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
              Text(text = "Save directly to device Gallery & Movies", color = Color(0xFF8E95A8), fontSize = 11.sp)
            }
          }

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF003840)
          ) {
            Text(
              text = "H.264 MP4",
              color = Color(0xFF00E5FF),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Resolution Picker
        Text(text = "VIDEO RESOLUTION", color = Color(0xFF8E95A8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf(
            512 to "512p (Fast)",
            720 to "720p HD",
            1080 to "1080p Ultra"
          ).forEach { (res, label) ->
            val isSelected = resolutionSize == res
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF242A3A),
              modifier = Modifier
                .weight(1f)
                .clickable { resolutionSize = res }
            ) {
              Text(
                text = label,
                color = if (isSelected) Color(0xFF101217) else Color.White,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Loop / Duration repetitions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(text = "Loop Repetitions", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(text = "Loops clip in video (total: ${(uiState.clip.totalFrames * loopCount / uiState.clip.fps.toFloat()).toInt()}s)", color = Color(0xFF8E95A8), fontSize = 10.sp)
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(1, 2, 3, 4).forEach { count ->
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (loopCount == count) Color(0xFFFFB300) else Color(0xFF242A3A),
                modifier = Modifier.clickable { loopCount = count }
              ) {
                Text(
                  text = "${count}x",
                  color = if (loopCount == count) Color(0xFF101217) else Color.White,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Include Bones overlay switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(text = "Show Rig Skeleton in Video", color = Color.White, fontSize = 12.sp)
            Text(text = if (includeBonesInVideo) "Technical rig showcase" else "Clean artwork only", color = Color(0xFF8E95A8), fontSize = 10.sp)
          }

          Switch(
            checked = includeBonesInVideo,
            onCheckedChange = { includeBonesInVideo = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF), checkedTrackColor = Color(0xFF003840))
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress indicator if rendering
        if (uiState.isExportingVideo) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = uiState.exportStatusMessage, color = Color(0xFF00E5FF), fontSize = 11.sp)
              Text(text = "${(uiState.exportProgress * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { uiState.exportProgress },
              color = Color(0xFF00E5FF),
              trackColor = Color(0xFF242A3A),
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
          }
        }

        // Export MP4 Button
        Button(
          onClick = {
            onExportMp4(
              ExportSettings(
                width = resolutionSize,
                height = resolutionSize,
                loopCount = loopCount,
                includeBones = includeBonesInVideo
              )
            )
          },
          enabled = !uiState.isExportingVideo,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("export_mp4_video_button")
        ) {
          if (uiState.isExportingVideo) {
            CircularProgressIndicator(color = Color(0xFF101217), modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Encoding Video...", color = Color(0xFF101217), fontWeight = FontWeight.Bold, fontSize = 13.sp)
          } else {
            Icon(Icons.Default.Movie, contentDescription = null, tint = Color(0xFF101217), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Render & Save MP4 to Gallery", color = Color(0xFF101217), fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }

        // Post-export Actions (Watch Video / Share)
        if (uiState.lastExportedVideoUri != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF142F26),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Saved to Gallery / Movies / DuikAngela!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
              Spacer(modifier = Modifier.height(8.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                    Mp4AnimationExporter.viewVideoInGallery(context, uiState.lastExportedVideoUri)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF101217), modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(text = "Open Video", color = Color(0xFF101217), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                  onClick = {
                    Mp4AnimationExporter.shareVideo(context, uiState.lastExportedVideoUri)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263345)),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(text = "Share MP4", color = Color.White, fontSize = 11.sp)
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 2. Presets Gallery
    Text(
      text = "LOAD CHARACTERS & RIGS",
      color = Color(0xFF7A8399),
      fontSize = 10.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(6.dp))

    DuikPresets.allPresets.forEach { preset ->
      val isSelected = preset.id == uiState.selectedPresetId
      Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isSelected) Color(0xFF1A2A38) else Color(0xFF1C202C)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp)
          .clickable { onSelectPreset(preset) }
      ) {
        Row(
          modifier = Modifier.padding(10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = preset.name,
              color = if (isSelected) Color(0xFF00E5FF) else Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = preset.description,
              color = Color(0xFF8E95A8),
              fontSize = 10.sp,
              maxLines = 2
            )
          }

          if (isSelected) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFF00E5FF)
            ) {
              Text(
                text = "ACTIVE",
                color = Color(0xFF101217),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. Project Metrics
    Text(
      text = "PROJECT METRICS",
      color = Color(0xFF7A8399),
      fontSize = 10.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(4.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      MetricBadge(label = "Bones", value = "${uiState.bones.size}", modifier = Modifier.weight(1f))
      MetricBadge(label = "IK Chains", value = "${uiState.ikConstraints.size}", modifier = Modifier.weight(1f))
      MetricBadge(label = "Frames", value = "${uiState.clip.totalFrames}", modifier = Modifier.weight(1f))
      MetricBadge(label = "Layers", value = "${uiState.layers.size}", modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 4. Export JSON Button
    Button(
      onClick = onExportJson,
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262C3E)),
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("export_json_button")
    ) {
      Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Export Duik Project JSON / AE Rig",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
      )
    }
  }
}

@Composable
private fun MetricBadge(label: String, value: String, modifier: Modifier = Modifier) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = Color(0xFF1C202C),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(text = value, color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
      Text(text = label, color = Color(0xFF8E95A8), fontSize = 9.sp)
    }
  }
}
