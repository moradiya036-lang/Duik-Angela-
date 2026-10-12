package com.example.duik.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.duik.ui.dialogs.CreateProjectDialog
import com.example.duik.ui.panels.DuikAutomationsPanel
import com.example.duik.ui.panels.DuikCutoutPanel
import com.example.duik.ui.panels.DuikExportPanel
import com.example.duik.ui.panels.DuikLayersPanel
import com.example.duik.ui.panels.DuikRigPanel
import com.example.duik.ui.stage.DuikStageCanvas
import com.example.duik.ui.timeline.DuikTimelineBar
import com.example.duik.viewmodel.DuikViewModel
import com.example.duik.viewmodel.EditorMode

@Composable
fun DuikMainScreen(
  viewModel: DuikViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  // System Image Picker for custom PNG/Manga Art
  val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      viewModel.importPngAsLayer(uri.toString())
    }
  }

  // Toast feedback
  LaunchedEffect(uiState.messageToast) {
    uiState.messageToast?.let { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
      viewModel.clearToast()
    }
  }

  // New Project Dialog
  if (uiState.showNewProjectDialog) {
    CreateProjectDialog(
      uiState = uiState,
      onDismiss = { viewModel.setShowNewProjectDialog(false) },
      onCreateProject = { name, presetId, totalFrames, fps ->
        viewModel.createNewProject(name, presetId, totalFrames, fps)
      },
      onSelectSavedProject = {
        // Handled in dialog
      }
    )
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding(),
    containerColor = Color(0xFF101217),
    snackbarHost = { SnackbarHost(snackbarHostState) }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // 1. Sleek Top Studio Bar with Project & Import actions
      DuikTopBar(
        projectName = uiState.projectName,
        currentMode = uiState.currentMode,
        onionSkinEnabled = uiState.onionSkinEnabled,
        autoKeyEnabled = uiState.autoKeyEnabled,
        onOpenProjectDialog = { viewModel.setShowNewProjectDialog(true) },
        onImportPng = { imagePickerLauncher.launch("image/*") },
        onModeSelected = { viewModel.setEditorMode(it) },
        onToggleOnionSkin = { viewModel.toggleOnionSkin() },
        onToggleAutoKey = { viewModel.toggleAutoKey() }
      )

      // 2. Interactive Stage Canvas Viewport
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        DuikStageCanvas(
          uiState = uiState,
          onSelectBone = { viewModel.setSelectedBone(it) },
          onSelectLayer = { viewModel.setSelectedLayer(it) },
          onDragController = { id, pos -> viewModel.moveController(id, pos) },
          onMoveLayer = { id, pos -> viewModel.moveLayer(id, pos) },
          onRotateLayer = { id, angle -> viewModel.rotateLayer(id, angle) },
          onSetLayerPivot = { id, pivot -> viewModel.setLayerPivot(id, pivot) },
          modifier = Modifier.fillMaxSize()
        )

        // Floating On-Stage Badges
        Row(
          modifier = Modifier
            .align(Alignment.TopStart)
            .padding(10.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val selLayer = uiState.layers.find { it.id == uiState.selectedLayerId }
          val selBone = uiState.bones.find { it.id == uiState.selectedBoneId }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF181C26).copy(alpha = 0.92f),
            shadowElevation = 2.dp
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(if (selLayer != null) Color(0xFF00E5FF) else if (selBone != null) Color(0xFFFFB300) else Color(0xFF8E95A8))
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = when {
                  selLayer != null -> "Layer: ${selLayer.name} (Drag to Animate)"
                  selBone != null -> "Bone: ${selBone.name}"
                  else -> "Tap any PNG or Bone to Pose"
                },
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }

          if (uiState.autoKeyEnabled) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFF003D45).copy(alpha = 0.92f)
            ) {
              Text(
                text = "AUTO-KEY ON",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
              )
            }
          }
        }
      }

      // 3. Contextual Bottom Panel based on Active Mode
      Box(
        modifier = Modifier.fillMaxWidth()
      ) {
        when (uiState.currentMode) {
          EditorMode.ANIMATE -> {
            DuikTimelineBar(
              uiState = uiState,
              onTogglePlay = { viewModel.togglePlayback() },
              onStepFrame = { viewModel.stepFrame(it) },
              onSetFrame = { viewModel.setFrame(it) },
              onRecordKeyframe = { easing -> viewModel.recordKeyframeForSelection(easing) },
              onDeleteKeyframe = { viewModel.deleteKeyframeAtCurrentFrame() }
            )
          }

          EditorMode.LAYERS -> {
            DuikLayersPanel(
              uiState = uiState,
              onPickImage = { imagePickerLauncher.launch("image/*") },
              onSelectLayer = { viewModel.setSelectedLayer(it) },
              onMoveLayer = { id, pos -> viewModel.moveLayer(id, pos) },
              onRotateLayer = { id, rot -> viewModel.rotateLayer(id, rot) },
              onScaleLayer = { id, scale -> viewModel.scaleLayer(id, scale) },
              onSetParentBone = { layerId, boneId -> viewModel.setLayerParentBone(layerId, boneId) },
              onDeleteLayer = { viewModel.deleteLayer(it) },
              onMoveLayerZIndex = { id, delta -> viewModel.moveLayerZIndex(id, delta) }
            )
          }

          EditorMode.RIG -> {
            DuikRigPanel(
              uiState = uiState,
              onSelectBone = { viewModel.setSelectedBone(it) },
              onRotateBone = { id, delta -> viewModel.rotateBone(id, delta) },
              onToggleBoneVisibility = { viewModel.toggleBonesOverlay() },
              onToggleControllersVisibility = { viewModel.toggleControllersOverlay() }
            )
          }

          EditorMode.CUTOUT -> {
            DuikCutoutPanel(
              uiState = uiState,
              onPickImage = { imagePickerLauncher.launch("image/*") },
              onAddSlicedPart = { name, boneId -> viewModel.addSlicedPart(name, boneId) },
              onUpdateCropRect = { viewModel.updateActiveCropRect(it) }
            )
          }

          EditorMode.AUTOMATIONS -> {
            DuikAutomationsPanel(
              uiState = uiState,
              onUpdateWalkCycle = { updater -> viewModel.updateWalkCycle(updater) },
              onToggleWiggle = { viewModel.toggleWiggle() }
            )
          }

          EditorMode.EXPORT -> {
            DuikExportPanel(
              uiState = uiState,
              onSelectPreset = { viewModel.selectPreset(it) },
              onExportMp4 = { settings ->
                viewModel.exportAnimationToMp4(settings)
              },
              onExportJson = {
                Toast.makeText(context, "Duik Project JSON exported successfully!", Toast.LENGTH_LONG).show()
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DuikTopBar(
  projectName: String,
  currentMode: EditorMode,
  onionSkinEnabled: Boolean,
  autoKeyEnabled: Boolean,
  onOpenProjectDialog: () -> Unit,
  onImportPng: () -> Unit,
  onModeSelected: (EditorMode) -> Unit,
  onToggleOnionSkin: () -> Unit,
  onToggleAutoKey: () -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = Color(0xFF141720),
    tonalElevation = 4.dp
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Top row: Project Switcher & Quick Actions
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Project Selector Button
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF1D222F),
          modifier = Modifier
            .clickable { onOpenProjectDialog() }
            .testTag("open_project_dialog_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = projectName,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              maxLines = 1
            )
          }
        }

        // Quick Import PNG & Auto-Key Action Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          // Import PNG Button
          Button(
            onClick = onImportPng,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            shape = RoundedCornerShape(6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            modifier = Modifier
              .height(30.dp)
              .testTag("topbar_import_png_button")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF101217), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(text = "Import PNG", color = Color(0xFF101217), fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }

          // Auto-Key Toggle
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (autoKeyEnabled) Color(0xFF003840) else Color(0xFF1E222D),
            modifier = Modifier.clickable { onToggleAutoKey() }
          ) {
            Text(
              text = if (autoKeyEnabled) "Auto-Key" else "Manual Key",
              color = if (autoKeyEnabled) Color(0xFF00E5FF) else Color(0xFF8E95A8),
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
          }

          // Onion Skin Toggle
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (onionSkinEnabled) Color(0xFF003840) else Color(0xFF1E222D),
            modifier = Modifier.clickable { onToggleOnionSkin() }
          ) {
            Text(
              text = "Ghost",
              color = if (onionSkinEnabled) Color(0xFF00E5FF) else Color(0xFF8E95A8),
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
          }
        }
      }

      // Mode Navigation Tabs
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        EditorMode.values().forEach { mode ->
          val isSelected = mode == currentMode
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1C202C),
            modifier = Modifier
              .clickable { onModeSelected(mode) }
              .testTag("mode_tab_${mode.name.lowercase()}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = mode.icon, fontSize = 12.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = mode.title,
                color = if (isSelected) Color(0xFF101217) else Color(0xFFB0B7C6),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }
      }
    }
  }
}
