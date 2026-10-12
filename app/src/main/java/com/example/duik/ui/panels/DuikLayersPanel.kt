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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.duik.math.Vec2
import com.example.duik.model.ImageLayer
import com.example.duik.viewmodel.DuikUiState

@Composable
fun DuikLayersPanel(
  uiState: DuikUiState,
  onPickImage: () -> Unit,
  onSelectLayer: (String) -> Unit,
  onMoveLayer: (String, Vec2) -> Unit,
  onRotateLayer: (String, Float) -> Unit,
  onScaleLayer: (String, Float) -> Unit,
  onSetParentBone: (String, String?) -> Unit,
  onDeleteLayer: (String) -> Unit,
  onMoveLayerZIndex: (String, Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val selectedLayer = uiState.layers.find { it.id == uiState.selectedLayerId }

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
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "PNG & Artwork Layers",
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${uiState.layers.size} Layers",
          color = Color(0xFF00E5FF),
          fontSize = 11.sp,
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF003840))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }

      Button(
        onClick = onPickImage,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("panel_import_png_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF101217), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "Import PNG", color = Color(0xFF101217), fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Selected Layer Detailed Transform Inspector
    if (selectedLayer != null) {
      Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C202C)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          // Layer Name & Parent status
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = selectedLayer.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Text(
                text = if (selectedLayer.parentBoneId != null) "Bound to Bone: ${selectedLayer.parentBoneId}" else "Free Transform (Manual Animation)",
                color = if (selectedLayer.parentBoneId != null) Color(0xFFFFB300) else Color(0xFF00E5FF),
                fontSize = 11.sp
              )
            }

            Row {
              IconButton(
                onClick = { onMoveLayerZIndex(selectedLayer.id, 1) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Bring Forward", tint = Color(0xFF8E95A8), modifier = Modifier.size(16.dp))
              }
              IconButton(
                onClick = { onMoveLayerZIndex(selectedLayer.id, -1) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Send Backward", tint = Color(0xFF8E95A8), modifier = Modifier.size(16.dp))
              }
              IconButton(
                onClick = { onDeleteLayer(selectedLayer.id) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Transform Quick Nudge Bars (Position, Rotation, Scale)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Position: (${selectedLayer.posX.toInt()}, ${selectedLayer.posY.toInt()})",
              color = Color(0xFFB0B7C6),
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF262C3D),
                modifier = Modifier.clickable { onMoveLayer(selectedLayer.id, Vec2(selectedLayer.posX - 10f, selectedLayer.posY)) }
              ) {
                Text(text = "←X", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF262C3D),
                modifier = Modifier.clickable { onMoveLayer(selectedLayer.id, Vec2(selectedLayer.posX + 10f, selectedLayer.posY)) }
              ) {
                Text(text = "X→", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF262C3D),
                modifier = Modifier.clickable { onMoveLayer(selectedLayer.id, Vec2(selectedLayer.posX, selectedLayer.posY - 10f)) }
              ) {
                Text(text = "↑Y", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF262C3D),
                modifier = Modifier.clickable { onMoveLayer(selectedLayer.id, Vec2(selectedLayer.posX, selectedLayer.posY + 10f)) }
              ) {
                Text(text = "↓Y", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Rotation: ${selectedLayer.rotationDeg.toInt()}° | Scale: ${(selectedLayer.scaleX * 100).toInt()}%",
              color = Color(0xFFB0B7C6),
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF262C3D),
                modifier = Modifier.clickable { onRotateLayer(selectedLayer.id, (selectedLayer.rotationDeg - 15f) % 360f) }
              ) {
                Text(text = "-15°", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF262C3D),
                modifier = Modifier.clickable { onRotateLayer(selectedLayer.id, (selectedLayer.rotationDeg + 15f) % 360f) }
              ) {
                Text(text = "+15°", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF262C3D),
                modifier = Modifier.clickable { onScaleLayer(selectedLayer.id, 1.15f) }
              ) {
                Text(text = "+Size", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF262C3D),
                modifier = Modifier.clickable { onScaleLayer(selectedLayer.id, 0.85f) }
              ) {
                Text(text = "-Size", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Bone Attachment selector
          if (uiState.bones.isNotEmpty()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = "Parent to Bone:", color = Color(0xFF7A8399), fontSize = 11.sp)
              Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = if (selectedLayer.parentBoneId == null) Color(0xFF00E5FF) else Color(0xFF262C3D),
                  modifier = Modifier.clickable { onSetParentBone(selectedLayer.id, null) }
                ) {
                  Text(
                    text = "None (Free)",
                    color = if (selectedLayer.parentBoneId == null) Color(0xFF101217) else Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                  )
                }

                uiState.bones.filterNot { it.isController }.take(4).forEach { bone ->
                  val isParent = selectedLayer.parentBoneId == bone.id
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isParent) Color(0xFFFFB300) else Color(0xFF262C3D),
                    modifier = Modifier.clickable { onSetParentBone(selectedLayer.id, bone.id) }
                  ) {
                    Text(
                      text = bone.name,
                      color = if (isParent) Color(0xFF101217) else Color.White,
                      fontSize = 10.sp,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Layers List
    Text(
      text = "ALL LAYERS (DRAG CANVAS TO ANIMATE)",
      color = Color(0xFF7A8399),
      fontSize = 10.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(4.dp))

    LazyColumn(
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      items(uiState.layers) { layer ->
        val isSelected = layer.id == uiState.selectedLayerId
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF232D42) else Color(0xFF181B24))
            .clickable { onSelectLayer(layer.id) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Image, contentDescription = null, tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFF8E95A8), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = layer.name,
              color = if (isSelected) Color(0xFF00E5FF) else Color.White,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          }

          Text(
            text = "Z: ${layer.zIndex} • ${if (layer.parentBoneId != null) "Bone" else "Free"}",
            color = Color(0xFF71798E),
            fontSize = 10.sp
          )
        }
      }
    }
  }
}
