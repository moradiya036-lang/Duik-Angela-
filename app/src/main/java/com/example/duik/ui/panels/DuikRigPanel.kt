package com.example.duik.ui.panels

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duik.model.Bone
import com.example.duik.model.IKConstraint
import com.example.duik.viewmodel.DuikUiState

@Composable
fun DuikRigPanel(
  uiState: DuikUiState,
  onSelectBone: (String) -> Unit,
  onRotateBone: (String, Float) -> Unit,
  onToggleBoneVisibility: () -> Unit,
  onToggleControllersVisibility: () -> Unit,
  modifier: Modifier = Modifier
) {
  val selectedBone = uiState.bones.find { it.id == uiState.selectedBoneId }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFF141720))
      .padding(12.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Duik Angela Rig Tree",
          color = Color.White,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${uiState.bones.size} Nodes",
          color = Color(0xFF00E5FF),
          fontSize = 11.sp,
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF003840))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (uiState.showBonesOverlay) Color(0xFF1E3A42) else Color(0xFF1E222D),
          modifier = Modifier.clickable { onToggleBoneVisibility() }
        ) {
          Text(
            text = if (uiState.showBonesOverlay) "Bones On" else "Bones Off",
            color = if (uiState.showBonesOverlay) Color(0xFF00E5FF) else Color(0xFF8E95A8),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (uiState.showControllersOverlay) Color(0xFF38321E) else Color(0xFF1E222D),
          modifier = Modifier.clickable { onToggleControllersVisibility() }
        ) {
          Text(
            text = if (uiState.showControllersOverlay) "Controllers On" else "Controllers Off",
            color = if (uiState.showControllersOverlay) Color(0xFFFFB300) else Color(0xFF8E95A8),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2-Bone IK Constraints Overview
    if (uiState.ikConstraints.isNotEmpty()) {
      Text(
        text = "ACTIVE 2-BONE IK CHAINS",
        color = Color(0xFF7A8399),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        uiState.ikConstraints.forEach { ik ->
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF1C2230),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(6.dp)) {
              Text(text = ik.name, color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text(
                text = "${if (ik.clockwiseBend) "CW Bend" else "CCW Bend"} • Stretch",
                color = Color(0xFF8E95A8),
                fontSize = 9.sp
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
    }

    // Selected Bone Inspector Card
    if (selectedBone != null) {
      Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C202C)),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
      ) {
        Column(modifier = Modifier.padding(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(if (selectedBone.isController) Color(0xFF00E5FF) else Color(selectedBone.color))
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = selectedBone.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = if (selectedBone.isController) "IK Controller" else "FK Bone",
              color = if (selectedBone.isController) Color(0xFF00E5FF) else Color(0xFFFFB300),
              fontSize = 11.sp
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Quick Rotate / Adjust Slider
          val curRot = uiState.localRotations[selectedBone.id] ?: 0f
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = "FK Angle: ${curRot.toInt()}°", color = Color(0xFFB0B7C6), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF262C3D),
                modifier = Modifier.clickable { onRotateBone(selectedBone.id, -10f) }
              ) {
                Text(text = "-10°", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFF262C3D),
                modifier = Modifier.clickable { onRotateBone(selectedBone.id, 10f) }
              ) {
                Text(text = "+10°", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
              }
            }
          }
        }
      }
    }

    // Bone List
    Text(
      text = "HIERARCHY",
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
      items(uiState.bones) { bone ->
        val isSelected = bone.id == uiState.selectedBoneId
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF232D42) else Color(0xFF181B24))
            .clickable { onSelectBone(bone.id) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (bone.isController) Color(0xFF00E5FF) else Color(bone.color))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = bone.name,
              color = if (isSelected) Color(0xFF00E5FF) else Color.White,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          }

          Text(
            text = if (bone.attachedLayerId != null) "Layer Bound" else (bone.parentId ?: "Root"),
            color = Color(0xFF71798E),
            fontSize = 10.sp
          )
        }
      }
    }
  }
}
