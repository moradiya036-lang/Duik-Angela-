package com.example.duik.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.duik.math.WalkCycleSettings
import com.example.duik.viewmodel.DuikUiState

@Composable
fun DuikAutomationsPanel(
  uiState: DuikUiState,
  onUpdateWalkCycle: ((WalkCycleSettings) -> WalkCycleSettings) -> Unit,
  onToggleWiggle: () -> Unit,
  modifier: Modifier = Modifier
) {
  val walk = uiState.walkCycleSettings

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color(0xFF141720))
      .verticalScroll(rememberScrollState())
      .padding(12.dp)
  ) {
    // Walk Cycle Section
    Card(
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1C202C)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Duik Angela Walk Cycle Engine",
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Procedural kinematics for 2-bone biped limbs",
              color = Color(0xFF8E95A8),
              fontSize = 11.sp
            )
          }

          Switch(
            checked = walk.enabled,
            onCheckedChange = { enabled ->
              onUpdateWalkCycle { it.copy(enabled = enabled) }
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color(0xFF00E5FF),
              checkedTrackColor = Color(0xFF004D5A)
            ),
            modifier = Modifier.testTag("walk_cycle_switch")
          )
        }

        if (walk.enabled) {
          Spacer(modifier = Modifier.height(10.dp))

          // Speed
          Text(text = "Walk Speed: ${String.format("%.1f", walk.speed)}x", color = Color(0xFFB0B7C6), fontSize = 11.sp)
          Slider(
            value = walk.speed,
            onValueChange = { s -> onUpdateWalkCycle { it.copy(speed = s) } },
            valueRange = 0.4f..2.5f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
          )

          // Stride Length
          Text(text = "Stride Length: ${walk.strideLength.toInt()}px", color = Color(0xFFB0B7C6), fontSize = 11.sp)
          Slider(
            value = walk.strideLength,
            onValueChange = { v -> onUpdateWalkCycle { it.copy(strideLength = v) } },
            valueRange = 40f..160f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
          )

          // Step Height
          Text(text = "Step Height: ${walk.stepHeight.toInt()}px", color = Color(0xFFB0B7C6), fontSize = 11.sp)
          Slider(
            value = walk.stepHeight,
            onValueChange = { v -> onUpdateWalkCycle { it.copy(stepHeight = v) } },
            valueRange = 10f..65f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
          )

          // Arm Swing
          Text(text = "Arm Counter-Swing: ${walk.armSwing.toInt()}px", color = Color(0xFFB0B7C6), fontSize = 11.sp)
          Slider(
            value = walk.armSwing,
            onValueChange = { v -> onUpdateWalkCycle { it.copy(armSwing = v) } },
            valueRange = 10f..100f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Secondary Wiggle & Physics Section
    Card(
      shape = RoundedCornerShape(10.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF1C202C)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Duik Wiggle & Hair Physics",
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Continuous harmonic oscillation for hair, scarf & weapons",
              color = Color(0xFF8E95A8),
              fontSize = 11.sp
            )
          }

          Switch(
            checked = uiState.wiggleEnabled,
            onCheckedChange = { onToggleWiggle() },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color(0xFFFFB300),
              checkedTrackColor = Color(0xFF5A3E00)
            ),
            modifier = Modifier.testTag("wiggle_switch")
          )
        }
      }
    }
  }
}
