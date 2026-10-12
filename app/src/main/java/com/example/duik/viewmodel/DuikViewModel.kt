package com.example.duik.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.duik.engine.DuikRigEngine
import com.example.duik.engine.LocalPose
import com.example.duik.export.ExportResult
import com.example.duik.export.ExportSettings
import com.example.duik.export.Mp4AnimationExporter
import com.example.duik.math.DuikAutomations
import com.example.duik.math.DuikEasing
import com.example.duik.math.DuikEasingType
import com.example.duik.math.Vec2
import com.example.duik.math.WalkCycleSettings
import com.example.duik.model.AnimationClip
import com.example.duik.model.AnimationTrack
import com.example.duik.model.Bone
import com.example.duik.model.BoneWorldTransform
import com.example.duik.model.CropRect
import com.example.duik.model.IKConstraint
import com.example.duik.model.ImageLayer
import com.example.duik.model.Keyframe
import com.example.duik.model.LayerSourceType
import com.example.duik.model.ProjectMetadata
import com.example.duik.model.PropertyType
import com.example.duik.model.PuppetPin
import com.example.duik.presets.DuikPreset
import com.example.duik.presets.DuikPresets
import com.example.duik.slice.DuikImageManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class EditorMode(val title: String, val icon: String) {
  ANIMATE("Animate", "🎬"),
  LAYERS("PNG Layers", "🖼"),
  RIG("Rigging", "🦴"),
  CUTOUT("Slice & Cutout", "✂"),
  AUTOMATIONS("Walk & Physics", "⚙"),
  EXPORT("Export", "💾")
}

data class DuikUiState(
  val currentMode: EditorMode = EditorMode.ANIMATE,
  val projectName: String = "Kai: Manga Blade Master",
  val selectedPresetId: String = DuikPresets.KaiMangaPreset.id,
  val bones: List<Bone> = DuikPresets.KaiMangaPreset.bones,
  val ikConstraints: List<IKConstraint> = DuikPresets.KaiMangaPreset.ikConstraints,
  val layers: List<ImageLayer> = DuikPresets.KaiMangaPreset.layers,
  val puppetPins: List<PuppetPin> = emptyList(),
  val clip: AnimationClip = DuikPresets.KaiMangaPreset.defaultClip,
  val currentFrame: Int = 0,
  val isPlaying: Boolean = false,
  val loopPlayback: Boolean = true,
  val onionSkinEnabled: Boolean = true,
  val showBonesOverlay: Boolean = true,
  val showControllersOverlay: Boolean = true,
  val autoKeyEnabled: Boolean = true,
  val walkCycleSettings: WalkCycleSettings = WalkCycleSettings(enabled = false),
  val wiggleEnabled: Boolean = true,
  val selectedBoneId: String? = null,
  val selectedLayerId: String? = "layer_torso",
  val controllerPositions: Map<String, Vec2> = DuikPresets.KaiMangaPreset.initialControllerPositions,
  val localRotations: Map<String, Float> = emptyMap(),
  val computedTransforms: Map<String, BoneWorldTransform> = emptyMap(),
  val activeCropRect: CropRect? = CropRect(0.40f, 0.20f, 0.60f, 0.46f),
  val customImageUri: String? = null,
  val messageToast: String? = null,
  val showNewProjectDialog: Boolean = false,
  val isExportingVideo: Boolean = false,
  val exportProgress: Float = 0f,
  val exportStatusMessage: String = "",
  val lastExportedVideoUri: Uri? = null,
  val savedProjects: List<ProjectMetadata> = listOf(
    ProjectMetadata("p_kai", "Kai: Manga Blade Master", "Pre-rigged 2-Bone anime character", false, 48, 24),
    ProjectMetadata("p_kunoichi", "Kunoichi Shadow", "Manga ninja with scarf physics", false, 40, 24),
    ProjectMetadata("p_homunculus", "Duik Angela Homunculus", "Standard AE biped skeleton", false, 24, 24),
    ProjectMetadata("p_blank_sample", "Custom Manga PNG Project", "Blank project for custom images", true, 60, 24)
  )
)

class DuikViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(DuikUiState())
  val uiState: StateFlow<DuikUiState> = _uiState.asStateFlow()

  private var playJob: Job? = null

  init {
    recomputeTransforms()
  }

  fun setEditorMode(mode: EditorMode) {
    _uiState.update { it.copy(currentMode = mode) }
  }

  fun setShowNewProjectDialog(show: Boolean) {
    _uiState.update { it.copy(showNewProjectDialog = show) }
  }

  fun toggleAutoKey() {
    _uiState.update { it.copy(autoKeyEnabled = !it.autoKeyEnabled) }
  }

  /**
   * Create a new project from scratch or from a template.
   */
  fun createNewProject(name: String, presetId: String, totalFrames: Int, fps: Int) {
    pause()
    val chosenPreset = DuikPresets.allPresets.find { it.id == presetId } ?: DuikPresets.BlankProjectPreset

    val newProjectMeta = ProjectMetadata(
      id = "proj_${System.currentTimeMillis()}",
      name = name.ifBlank { "Untitled Animation" },
      description = "Custom project created at ${System.currentTimeMillis()}",
      isBlank = presetId == DuikPresets.BlankProjectPreset.id,
      totalFrames = totalFrames,
      fps = fps
    )

    _uiState.update { current ->
      current.copy(
        projectName = newProjectMeta.name,
        selectedPresetId = presetId,
        bones = chosenPreset.bones,
        ikConstraints = chosenPreset.ikConstraints,
        layers = chosenPreset.layers,
        clip = AnimationClip(
          name = newProjectMeta.name,
          totalFrames = totalFrames,
          fps = fps,
          tracks = chosenPreset.defaultClip.tracks
        ),
        currentFrame = 0,
        controllerPositions = chosenPreset.initialControllerPositions,
        localRotations = emptyMap(),
        selectedBoneId = chosenPreset.bones.firstOrNull { it.isController }?.id,
        selectedLayerId = chosenPreset.layers.firstOrNull()?.id,
        showNewProjectDialog = false,
        savedProjects = listOf(newProjectMeta) + current.savedProjects,
        messageToast = "Created new project: '${newProjectMeta.name}'!"
      )
    }
    recomputeTransforms()
  }

  fun selectPreset(preset: DuikPreset) {
    pause()
    _uiState.update {
      it.copy(
        projectName = preset.name,
        selectedPresetId = preset.id,
        bones = preset.bones,
        ikConstraints = preset.ikConstraints,
        layers = preset.layers,
        clip = preset.defaultClip,
        currentFrame = 0,
        controllerPositions = preset.initialControllerPositions,
        localRotations = emptyMap(),
        selectedBoneId = preset.bones.firstOrNull { b -> b.isController }?.id,
        selectedLayerId = preset.layers.firstOrNull()?.id,
        messageToast = "Loaded: ${preset.name}"
      )
    }
    recomputeTransforms()
  }

  fun exportAnimationToMp4(settings: ExportSettings = ExportSettings()) {
    if (_uiState.value.isExportingVideo) return
    pause()
    _uiState.update {
      it.copy(
        isExportingVideo = true,
        exportProgress = 0f,
        exportStatusMessage = "Starting video render..."
      )
    }

    viewModelScope.launch {
      val context = getApplication<Application>()
      val result = Mp4AnimationExporter.exportToGalleryMp4(
        context = context,
        uiState = _uiState.value,
        settings = settings
      ) { progress, message ->
        _uiState.update {
          it.copy(
            exportProgress = progress,
            exportStatusMessage = message
          )
        }
      }

      if (result.success && result.uri != null) {
        _uiState.update {
          it.copy(
            isExportingVideo = false,
            lastExportedVideoUri = result.uri,
            messageToast = "Successfully saved MP4 video to Gallery!"
          )
        }
      } else {
        _uiState.update {
          it.copy(
            isExportingVideo = false,
            messageToast = "Export error: ${result.errorMessage ?: "Unknown error"}"
          )
        }
      }
    }
  }

  fun clearExportedVideo() {
    _uiState.update { it.copy(lastExportedVideoUri = null) }
  }

  /**
   * Directly import any PNG or photo as a standalone, animatable ImageLayer.
   */
  fun importPngAsLayer(uriString: String, layerName: String? = null) {
    val context = getApplication<Application>()
    val bitmap = DuikImageManager.decodeAndCacheUri(context, uriString)
    val width = bitmap?.width?.toFloat() ?: 200f
    val height = bitmap?.height?.toFloat() ?: 200f

    val aspect = if (height > 0f) width / height else 1f
    val baseH = 160f
    val baseW = baseH * aspect

    val layerId = "layer_png_${System.currentTimeMillis()}"
    val name = layerName ?: "Imported PNG ${_uiState.value.layers.size + 1}"

    val newLayer = ImageLayer(
      id = layerId,
      name = name,
      sourceType = LayerSourceType.CUSTOM_URI,
      customUriString = uriString,
      cropRect = null,
      zIndex = _uiState.value.layers.size + 1,
      visible = true,
      opacity = 1f,
      baseWidth = baseW.coerceIn(60f, 400f),
      baseHeight = baseH.coerceIn(60f, 400f),
      posX = 0f,
      posY = 0f,
      rotationDeg = 0f,
      scaleX = 1f,
      scaleY = 1f,
      pivotX = 0.5f,
      pivotY = 0.5f,
      parentBoneId = null
    )

    _uiState.update { current ->
      current.copy(
        layers = current.layers + newLayer,
        selectedLayerId = layerId,
        selectedBoneId = null,
        currentMode = EditorMode.ANIMATE,
        messageToast = "Imported '$name'! Drag handles to position & animate."
      )
    }

    // Record initial keyframe at currentFrame
    recordKeyframeForSelectedLayer(DuikEasingType.EASE_IN_OUT)
  }

  /**
   * Manually move an imported PNG layer on the stage canvas.
   */
  fun moveLayer(layerId: String, newPos: Vec2) {
    _uiState.update { current ->
      val updated = current.layers.map { layer ->
        if (layer.id == layerId) layer.copy(posX = newPos.x, posY = newPos.y) else layer
      }
      current.copy(layers = updated)
    }

    if (_uiState.value.autoKeyEnabled) {
      recordKeyframeForLayer(layerId, PropertyType.POS_X, newPos.x)
      recordKeyframeForLayer(layerId, PropertyType.POS_Y, newPos.y)
    }
  }

  /**
   * Manually rotate an imported PNG layer around its pivot/anchor point.
   */
  fun rotateLayer(layerId: String, newAngleDeg: Float) {
    _uiState.update { current ->
      val updated = current.layers.map { layer ->
        if (layer.id == layerId) layer.copy(rotationDeg = newAngleDeg) else layer
      }
      current.copy(layers = updated)
    }

    if (_uiState.value.autoKeyEnabled) {
      recordKeyframeForLayer(layerId, PropertyType.ROTATION, newAngleDeg)
    }
  }

  /**
   * Manually scale an imported PNG layer.
   */
  fun scaleLayer(layerId: String, scaleFactor: Float) {
    _uiState.update { current ->
      val updated = current.layers.map { layer ->
        if (layer.id == layerId) {
          val nextScaleX = (layer.scaleX * scaleFactor).coerceIn(0.1f, 10f)
          val nextScaleY = (layer.scaleY * scaleFactor).coerceIn(0.1f, 10f)
          layer.copy(scaleX = nextScaleX, scaleY = nextScaleY)
        } else layer
      }
      current.copy(layers = updated)
    }

    val layer = _uiState.value.layers.find { it.id == layerId }
    if (layer != null && _uiState.value.autoKeyEnabled) {
      recordKeyframeForLayer(layerId, PropertyType.SCALE_X, layer.scaleX)
      recordKeyframeForLayer(layerId, PropertyType.SCALE_Y, layer.scaleY)
    }
  }

  /**
   * Change the pivot / anchor point of an imported PNG layer (0..1 normalized).
   */
  fun setLayerPivot(layerId: String, newPivot: Vec2) {
    _uiState.update { current ->
      val updated = current.layers.map { layer ->
        if (layer.id == layerId) layer.copy(pivotX = newPivot.x.coerceIn(0f, 1f), pivotY = newPivot.y.coerceIn(0f, 1f)) else layer
      }
      current.copy(
        layers = updated,
        messageToast = "Anchor point updated to (${String.format("%.2f", newPivot.x)}, ${String.format("%.2f", newPivot.y)})"
      )
    }
  }

  /**
   * Bind or unbind a layer to/from a Bone.
   */
  fun setLayerParentBone(layerId: String, boneId: String?) {
    _uiState.update { current ->
      val updated = current.layers.map { layer ->
        if (layer.id == layerId) layer.copy(parentBoneId = boneId) else layer
      }
      val updatedBones = current.bones.map { bone ->
        if (bone.id == boneId) bone.copy(attachedLayerId = layerId) else bone
      }
      current.copy(
        layers = updated,
        bones = updatedBones,
        messageToast = if (boneId != null) "Layer parented to bone '$boneId'!" else "Layer unparented (free transform)."
      )
    }
    recomputeTransforms()
  }

  /**
   * Delete a layer.
   */
  fun deleteLayer(layerId: String) {
    _uiState.update { current ->
      val updated = current.layers.filterNot { it.id == layerId }
      val updatedTracks = current.clip.tracks.filterNot { it.targetId == layerId }
      current.copy(
        layers = updated,
        clip = current.clip.copy(tracks = updatedTracks),
        selectedLayerId = updated.firstOrNull()?.id,
        messageToast = "Layer deleted"
      )
    }
  }

  /**
   * Reorder layer Z-index.
   */
  fun moveLayerZIndex(layerId: String, delta: Int) {
    _uiState.update { current ->
      val updated = current.layers.map { layer ->
        if (layer.id == layerId) layer.copy(zIndex = (layer.zIndex + delta).coerceAtLeast(0)) else layer
      }
      current.copy(layers = updated)
    }
  }

  fun togglePlayback() {
    if (_uiState.value.isPlaying) pause() else play()
  }

  fun play() {
    playJob?.cancel()
    _uiState.update { it.copy(isPlaying = true) }
    playJob = viewModelScope.launch {
      val fps = _uiState.value.clip.fps
      val intervalMs = (1000L / fps).coerceAtLeast(16L)
      while (_uiState.value.isPlaying) {
        delay(intervalMs)
        stepFrame(1)
      }
    }
  }

  fun pause() {
    playJob?.cancel()
    playJob = null
    _uiState.update { it.copy(isPlaying = false) }
  }

  fun setFrame(frame: Int) {
    val total = _uiState.value.clip.totalFrames
    val clamped = frame.coerceIn(0, total - 1)
    _uiState.update { it.copy(currentFrame = clamped) }
    evaluateKeyframesAtCurrentFrame()
    recomputeTransforms()
  }

  fun stepFrame(delta: Int) {
    val state = _uiState.value
    val total = state.clip.totalFrames
    var next = state.currentFrame + delta
    if (next >= total) {
      next = if (state.loopPlayback) 0 else total - 1
    } else if (next < 0) {
      next = if (state.loopPlayback) total - 1 else 0
    }
    setFrame(next)
  }

  fun setSelectedBone(boneId: String?) {
    _uiState.update {
      it.copy(
        selectedBoneId = boneId,
        selectedLayerId = if (boneId != null) null else it.selectedLayerId
      )
    }
  }

  fun setSelectedLayer(layerId: String?) {
    _uiState.update {
      it.copy(
        selectedLayerId = layerId,
        selectedBoneId = if (layerId != null) null else it.selectedBoneId
      )
    }
  }

  fun toggleOnionSkin() {
    _uiState.update { it.copy(onionSkinEnabled = !it.onionSkinEnabled) }
  }

  fun toggleBonesOverlay() {
    _uiState.update { it.copy(showBonesOverlay = !it.showBonesOverlay) }
  }

  fun toggleControllersOverlay() {
    _uiState.update { it.copy(showControllersOverlay = !it.showControllersOverlay) }
  }

  fun toggleWiggle() {
    _uiState.update { it.copy(wiggleEnabled = !it.wiggleEnabled) }
    recomputeTransforms()
  }

  fun updateWalkCycle(updater: (WalkCycleSettings) -> WalkCycleSettings) {
    _uiState.update { it.copy(walkCycleSettings = updater(it.walkCycleSettings)) }
    recomputeTransforms()
  }

  /**
   * Drag controller position in world coordinates (e.g. IK hand target or IK foot target).
   */
  fun moveController(boneId: String, newPos: Vec2) {
    _uiState.update { current ->
      val updated = current.controllerPositions.toMutableMap()
      updated[boneId] = newPos
      current.copy(controllerPositions = updated)
    }
    if (_uiState.value.autoKeyEnabled) {
      recordKeyframeForBone(boneId, PropertyType.POS_X, newPos.x)
      recordKeyframeForBone(boneId, PropertyType.POS_Y, newPos.y)
    }
    recomputeTransforms()
  }

  /**
   * Rotate a FK bone directly by delta degrees.
   */
  fun rotateBone(boneId: String, deltaDeg: Float) {
    _uiState.update { current ->
      val updated = current.localRotations.toMutableMap()
      val curRot = updated[boneId] ?: 0f
      val newRot = (curRot + deltaDeg) % 360f
      updated[boneId] = newRot
      current.copy(localRotations = updated)
    }
    if (_uiState.value.autoKeyEnabled) {
      val rot = _uiState.value.localRotations[boneId] ?: 0f
      recordKeyframeForBone(boneId, PropertyType.ROTATION, rot)
    }
    recomputeTransforms()
  }

  /**
   * Records keyframe for the currently selected item (Layer or Bone).
   */
  fun recordKeyframeForSelection(easing: DuikEasingType = DuikEasingType.EASE_IN_OUT) {
    val selLayerId = _uiState.value.selectedLayerId
    val selBoneId = _uiState.value.selectedBoneId

    if (selLayerId != null) {
      recordKeyframeForSelectedLayer(easing)
    } else if (selBoneId != null) {
      recordKeyframeForSelectedBone(easing)
    }
  }

  private fun recordKeyframeForSelectedLayer(easing: DuikEasingType = DuikEasingType.EASE_IN_OUT) {
    val layerId = _uiState.value.selectedLayerId ?: return
    val layer = _uiState.value.layers.find { it.id == layerId } ?: return
    val frame = _uiState.value.currentFrame

    _uiState.update { current ->
      val tracks = current.clip.tracks.toMutableList()
      upsertTrackKeyframe(tracks, layerId, PropertyType.POS_X, true, frame, layer.posX, easing)
      upsertTrackKeyframe(tracks, layerId, PropertyType.POS_Y, true, frame, layer.posY, easing)
      upsertTrackKeyframe(tracks, layerId, PropertyType.ROTATION, true, frame, layer.rotationDeg, easing)
      upsertTrackKeyframe(tracks, layerId, PropertyType.SCALE_X, true, frame, layer.scaleX, easing)
      upsertTrackKeyframe(tracks, layerId, PropertyType.SCALE_Y, true, frame, layer.scaleY, easing)

      current.copy(
        clip = current.clip.copy(tracks = tracks),
        messageToast = "Keyframe recorded for layer '${layer.name}' @ frame $frame"
      )
    }
  }

  fun recordKeyframeForSelectedBone(easing: DuikEasingType = DuikEasingType.EASE_IN_OUT) {
    val boneId = _uiState.value.selectedBoneId ?: return
    val bone = _uiState.value.bones.find { it.id == boneId } ?: return
    val frame = _uiState.value.currentFrame

    _uiState.update { current ->
      val currentTracks = current.clip.tracks.toMutableList()

      if (bone.isController) {
        val pos = current.controllerPositions[boneId] ?: Vec2.Zero
        upsertTrackKeyframe(currentTracks, boneId, PropertyType.POS_X, false, frame, pos.x, easing)
        upsertTrackKeyframe(currentTracks, boneId, PropertyType.POS_Y, false, frame, pos.y, easing)
      } else {
        val rot = current.localRotations[boneId] ?: 0f
        upsertTrackKeyframe(currentTracks, boneId, PropertyType.ROTATION, false, frame, rot, easing)
      }

      current.copy(
        clip = current.clip.copy(tracks = currentTracks),
        messageToast = "Keyframe recorded for ${bone.name} @ frame $frame"
      )
    }
  }

  private fun recordKeyframeForLayer(layerId: String, property: PropertyType, value: Float) {
    val frame = _uiState.value.currentFrame
    _uiState.update { current ->
      val tracks = current.clip.tracks.toMutableList()
      upsertTrackKeyframe(tracks, layerId, property, true, frame, value, DuikEasingType.EASE_IN_OUT)
      current.copy(clip = current.clip.copy(tracks = tracks))
    }
  }

  private fun recordKeyframeForBone(boneId: String, property: PropertyType, value: Float) {
    val frame = _uiState.value.currentFrame
    _uiState.update { current ->
      val tracks = current.clip.tracks.toMutableList()
      upsertTrackKeyframe(tracks, boneId, property, false, frame, value, DuikEasingType.EASE_IN_OUT)
      current.copy(clip = current.clip.copy(tracks = tracks))
    }
  }

  private fun upsertTrackKeyframe(
    tracks: MutableList<AnimationTrack>,
    targetId: String,
    property: PropertyType,
    isLayerTrack: Boolean,
    frame: Int,
    value: Float,
    easing: DuikEasingType
  ) {
    val existingIndex = tracks.indexOfFirst { it.targetId == targetId && it.property == property }
    if (existingIndex >= 0) {
      val track = tracks[existingIndex]
      val keyframes = track.keyframes.toMutableList()
      val keyIndex = keyframes.indexOfFirst { it.frame == frame }
      val newKey = Keyframe(id = UUID.randomUUID().toString(), frame = frame, value = value, easing = easing)
      if (keyIndex >= 0) {
        keyframes[keyIndex] = newKey
      } else {
        keyframes.add(newKey)
        keyframes.sortBy { it.frame }
      }
      tracks[existingIndex] = track.copy(keyframes = keyframes)
    } else {
      tracks.add(
        AnimationTrack(
          id = UUID.randomUUID().toString(),
          targetId = targetId,
          property = property,
          isLayerTrack = isLayerTrack,
          keyframes = listOf(Keyframe(UUID.randomUUID().toString(), frame, value, easing))
        )
      )
    }
  }

  fun deleteKeyframeAtCurrentFrame() {
    val targetId = _uiState.value.selectedLayerId ?: _uiState.value.selectedBoneId ?: return
    val frame = _uiState.value.currentFrame
    _uiState.update { current ->
      val updatedTracks = current.clip.tracks.map { track ->
        if (track.targetId == targetId) {
          track.copy(keyframes = track.keyframes.filterNot { it.frame == frame })
        } else track
      }
      current.copy(
        clip = current.clip.copy(tracks = updatedTracks),
        messageToast = "Deleted keyframe @ frame $frame"
      )
    }
  }

  fun clearToast() {
    _uiState.update { it.copy(messageToast = null) }
  }

  fun updateActiveCropRect(rect: CropRect) {
    _uiState.update { it.copy(activeCropRect = rect) }
  }

  fun addSlicedPart(name: String, targetBoneId: String) {
    val crop = _uiState.value.activeCropRect ?: return
    val layerId = "layer_slice_${System.currentTimeMillis()}"

    val newLayer = ImageLayer(
      id = layerId,
      name = name,
      sourceType = if (_uiState.value.customImageUri != null) LayerSourceType.CUSTOM_URI else LayerSourceType.BUILTIN_SPRITE_SHEET,
      customUriString = _uiState.value.customImageUri,
      cropRect = crop,
      zIndex = 2,
      baseWidth = (crop.width * 200f).coerceIn(40f, 300f),
      baseHeight = (crop.height * 200f).coerceIn(40f, 300f)
    )

    _uiState.update { current ->
      val updatedLayers = current.layers + newLayer
      val updatedBones = current.bones.map { b ->
        if (b.id == targetBoneId) b.copy(attachedLayerId = layerId) else b
      }
      current.copy(
        layers = updatedLayers,
        bones = updatedBones,
        selectedLayerId = layerId,
        messageToast = "Sliced '$name' and bound to bone!"
      )
    }
    recomputeTransforms()
  }

  fun importCustomImage(uriString: String) {
    _uiState.update {
      it.copy(
        customImageUri = uriString,
        currentMode = EditorMode.CUTOUT,
        messageToast = "Imported custom artwork! Slice parts or animate full layer."
      )
    }
    // Also create layer
    importPngAsLayer(uriString, "Imported Art")
  }

  private fun evaluateKeyframesAtCurrentFrame() {
    val state = _uiState.value
    val frame = state.currentFrame
    val newControllers = state.controllerPositions.toMutableMap()
    val newRotations = state.localRotations.toMutableMap()
    val updatedLayers = state.layers.map { it.copy() }.toMutableList()

    for (track in state.clip.tracks) {
      if (track.keyframes.isEmpty()) continue
      val evaluatedVal = evaluateTrackAtFrame(track, frame) ?: continue

      if (track.isLayerTrack) {
        val layerIdx = updatedLayers.indexOfFirst { it.id == track.targetId }
        if (layerIdx >= 0) {
          val l = updatedLayers[layerIdx]
          updatedLayers[layerIdx] = when (track.property) {
            PropertyType.POS_X -> l.copy(posX = evaluatedVal)
            PropertyType.POS_Y -> l.copy(posY = evaluatedVal)
            PropertyType.ROTATION -> l.copy(rotationDeg = evaluatedVal)
            PropertyType.SCALE_X -> l.copy(scaleX = evaluatedVal)
            PropertyType.SCALE_Y -> l.copy(scaleY = evaluatedVal)
          }
        }
      } else {
        when (track.property) {
          PropertyType.POS_X -> {
            val cur = newControllers[track.targetId] ?: Vec2.Zero
            newControllers[track.targetId] = Vec2(evaluatedVal, cur.y)
          }
          PropertyType.POS_Y -> {
            val cur = newControllers[track.targetId] ?: Vec2.Zero
            newControllers[track.targetId] = Vec2(cur.x, evaluatedVal)
          }
          PropertyType.ROTATION -> {
            newRotations[track.targetId] = evaluatedVal
          }
          else -> {}
        }
      }
    }

    _uiState.update {
      it.copy(
        controllerPositions = newControllers,
        localRotations = newRotations,
        layers = updatedLayers
      )
    }
  }

  private fun evaluateTrackAtFrame(track: AnimationTrack, frame: Int): Float? {
    val keys = track.keyframes
    if (keys.isEmpty()) return null
    if (keys.size == 1 || frame <= keys.first().frame) return keys.first().value
    if (frame >= keys.last().frame) return keys.last().value

    for (i in 0 until keys.size - 1) {
      val k0 = keys[i]
      val k1 = keys[i + 1]
      if (frame in k0.frame..k1.frame) {
        val span = (k1.frame - k0.frame).toFloat()
        if (span <= 0f) return k0.value
        val t = (frame - k0.frame).toFloat() / span
        return DuikEasing.interpolateFloat(k0.value, k1.value, t, k0.easing)
      }
    }
    return keys.last().value
  }

  private fun recomputeTransforms() {
    val state = _uiState.value
    val timeSec = state.currentFrame.toFloat() / state.clip.fps.toFloat()

    val walkOffsets = if (state.walkCycleSettings.enabled) {
      DuikAutomations.computeWalkCycle(
        cycleTimeSec = timeSec,
        baseLeftFootRest = state.controllerPositions["ctrl_foot_l"] ?: Vec2(45f, 160f),
        baseRightFootRest = state.controllerPositions["ctrl_foot_r"] ?: Vec2(-45f, 160f),
        baseLeftHandRest = state.controllerPositions["ctrl_hand_l"] ?: Vec2(110f, -100f),
        baseRightHandRest = state.controllerPositions["ctrl_hand_r"] ?: Vec2(-105f, -85f),
        settings = state.walkCycleSettings
      )
    } else null

    val transforms = DuikRigEngine.evaluateSkeleton(
      bones = state.bones,
      ikConstraints = state.ikConstraints,
      localPose = LocalPose(
        localTranslations = emptyMap(),
        localRotations = state.localRotations,
        controllerPositions = state.controllerPositions
      ),
      walkOffsets = walkOffsets,
      timeSec = timeSec,
      wiggleEnabled = state.wiggleEnabled
    )

    _uiState.update { it.copy(computedTransforms = transforms) }
  }
}
