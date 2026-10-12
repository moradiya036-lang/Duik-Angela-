package com.example.duik.ui.panels

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duik.model.CropRect
import com.example.duik.viewmodel.DuikUiState

data class SlicePreset(
  val label: String,
  val targetBoneId: String,
  val rect: CropRect
)

@Composable
fun DuikCutoutPanel(
  uiState: DuikUiState,
  onPickImage: () -> Unit,
  onAddSlicedPart: (String, String) -> Unit,
  onUpdateCropRect: (CropRect) -> Unit,
  modifier: Modifier = Modifier
) {
  val activeCrop = uiState.activeCropRect ?: CropRect(0.40f, 0.20f, 0.60f, 0.46f)
  var partName by remember { mutableStateOf("New Manga Part") }
  var targetBoneId by remember { mutableStateOf(uiState.selectedBoneId ?: "head") }

  val slicePresets = listOf(
    SlicePreset("Head", "head", CropRect(0.04f, 0.03f, 0.22f, 0.22f)),
    SlicePreset("Torso", "spine", CropRect(0.40f, 0.20f, 0.60f, 0.46f)),
    SlicePreset("Left Arm", "arm_upper_l", CropRect(0.06f, 0.33f, 0.16f, 0.48f)),
    SlicePreset("Left Forearm", "arm_lower_l", CropRect(0.05f, 0.48f, 0.14f, 0.68f)),
    SlicePreset("Left Shin", "leg_lower_l", CropRect(0.14f, 0.72f, 0.28f, 0.95f)),
    SlicePreset("Right Shin", "leg_lower_r", CropRect(0.71f, 0.72f, 0.85f, 0.95f))
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFF141720))
      .padding(12.dp)
  ) {
    // Header & Import action
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Manga & PNG Slicer",
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = if (uiState.customImageUri != null) "Custom Image Active" else "Preset Manga Sheet Active",
          color = Color(0xFF00E5FF),
          fontSize = 11.sp
        )
      }

      Button(
        onClick = onPickImage,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242C3D)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("import_image_button")
      ) {
        Icon(Icons.Default.Image, contentDescription = "Import", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Import PNG", color = Color.White, fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Quick Slice Preset Chips
    Text(
      text = "QUICK SPRITE PRESETS",
      color = Color(0xFF7A8399),
      fontSize = 10.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(4.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      slicePresets.forEach { preset ->
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (activeCrop == preset.rect) Color(0xFF004D5A) else Color(0xFF1D222F),
          modifier = Modifier.clickable {
            onUpdateCropRect(preset.rect)
            partName = preset.label
            targetBoneId = preset.targetBoneId
          }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = preset.label, color = Color.White, fontSize = 11.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Target Bone Selection Chips
    Text(
      text = "BIND CUTOUT TO BONE",
      color = Color(0xFF7A8399),
      fontSize = 10.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(4.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      uiState.bones.filterNot { it.isController }.forEach { bone ->
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (targetBoneId == bone.id) Color(0xFFFFB300) else Color(0xFF1D222F),
          modifier = Modifier.clickable { targetBoneId = bone.id }
        ) {
          Text(
            text = bone.name,
            color = if (targetBoneId == bone.id) Color(0xFF101217) else Color(0xFFB0B7C6),
            fontSize = 11.sp,
            fontWeight = if (targetBoneId == bone.id) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Slice Confirmation Action
    Button(
      onClick = {
        onAddSlicedPart(partName, targetBoneId)
      },
      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
      shape = RoundedCornerShape(8.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("slice_and_bind_button")
    ) {
      Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF101217), modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Slice '$partName' & Bind to Bone",
        color = Color(0xFF101217),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp
      )
    }
  }
}
