package com.example.duik.ui.dialogs

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.duik.presets.DuikPresets
import com.example.duik.viewmodel.DuikUiState

@Composable
fun CreateProjectDialog(
  uiState: DuikUiState,
  onDismiss: () -> Unit,
  onCreateProject: (name: String, presetId: String, totalFrames: Int, fps: Int) -> Unit,
  onSelectSavedProject: (presetId: String) -> Unit
) {
  var projectName by remember { mutableStateOf("My PNG Animation") }
  var selectedPresetId by remember { mutableStateOf(DuikPresets.BlankProjectPreset.id) }
  var totalFrames by remember { mutableIntStateOf(48) }
  var fps by remember { mutableIntStateOf(24) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF181B24)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Create New Project",
              color = Color.White,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF8E95A8))
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Project Name Input
        Text(text = "PROJECT NAME", color = Color(0xFF8E95A8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
          value = projectName,
          onValueChange = { projectName = it },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("project_name_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00E5FF),
            unfocusedBorderColor = Color(0xFF2E3444),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Starting Template / Rig Type
        Text(text = "PROJECT TYPE & RIG TEMPLATE", color = Color(0xFF8E95A8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        val templates = listOf(
          Triple(DuikPresets.BlankProjectPreset.id, "Blank (Custom PNG)", "Clean slate for importing & animating PNGs directly"),
          Triple(DuikPresets.KaiMangaPreset.id, "Kai Manga Warrior", "Preloaded 2-Bone anime character rig"),
          Triple(DuikPresets.NinjaKunoichiPreset.id, "Kunoichi Ninja", "Manga artwork with wind scarf physics"),
          Triple(DuikPresets.DuikHomunculusPreset.id, "Duik Homunculus", "Universal AE 16-node biped skeletal rig")
        )

        templates.forEach { (id, title, desc) ->
          val isSelected = selectedPresetId == id
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) Color(0xFF003840) else Color(0xFF1E222D),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isSelected) Color(0xFF00E5FF) else Color.Transparent
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 6.dp)
              .clickable { selectedPresetId = id }
          ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
              Text(
                text = title,
                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(text = desc, color = Color(0xFF8E95A8), fontSize = 10.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Frame Count & FPS selection
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "DURATION", color = Color(0xFF8E95A8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf(24, 48, 60, 120).forEach { f ->
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = if (totalFrames == f) Color(0xFF00E5FF) else Color(0xFF262C3D),
                  modifier = Modifier.clickable { totalFrames = f }
                ) {
                  Text(
                    text = "${f}f",
                    color = if (totalFrames == f) Color(0xFF101217) else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(text = "FPS", color = Color(0xFF8E95A8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              listOf(12, 24, 30, 60).forEach { r ->
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = if (fps == r) Color(0xFFFFB300) else Color(0xFF262C3D),
                  modifier = Modifier.clickable { fps = r }
                ) {
                  Text(
                    text = "$r",
                    color = if (fps == r) Color(0xFF101217) else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Create Project Button
        Button(
          onClick = {
            onCreateProject(projectName, selectedPresetId, totalFrames, fps)
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("confirm_create_project_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF101217), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Create Animation Project",
            color = Color(0xFF101217),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }
      }
    }
  }
}
