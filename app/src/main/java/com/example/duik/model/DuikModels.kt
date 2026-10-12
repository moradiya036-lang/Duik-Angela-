package com.example.duik.model

import com.example.duik.math.DuikEasingType
import com.example.duik.math.Vec2

enum class ControllerType(val label: String) {
  NONE("Bone Only"),
  CROSSHAIR("IK Target"),
  CIRCLE("FK Pivot"),
  BOX("Master Root"),
  FOOT_ROLL("Foot Roll Controller"),
  SLIDER("Morph Slider")
}

enum class PropertyType(val displayName: String) {
  POS_X("Position X"),
  POS_Y("Position Y"),
  ROTATION("Rotation (°)"),
  SCALE_X("Scale X"),
  SCALE_Y("Scale Y");
}

data class Bone(
  val id: String,
  val name: String,
  val parentId: String? = null,
  val restLength: Float = 70f,
  val restAngleDeg: Float = 0f,
  val color: Long = 0xFFFFB300, // Duik Amber
  val isController: Boolean = false,
  val controllerType: ControllerType = ControllerType.NONE,
  val attachedLayerId: String? = null,
  val pivotRatio: Vec2 = Vec2(0.5f, 0.1f), // Pivot relative to layer rect (0..1)
  val visible: Boolean = true,
  val locked: Boolean = false,
  val enablesWiggle: Boolean = false,
  val wiggleAmp: Float = 10f
)

data class IKConstraint(
  val id: String,
  val name: String,
  val rootBoneId: String,
  val midBoneId: String,
  val endBoneId: String,
  val controllerBoneId: String,
  val clockwiseBend: Boolean = false,
  val allowStretch: Boolean = true,
  val enabled: Boolean = true
)

data class PuppetPin(
  val id: String,
  val name: String,
  val layerId: String,
  val normalizedPos: Vec2, // 0..1 inside layer bounds
  val currentOffset: Vec2 = Vec2.Zero,
  val weight: Float = 1.0f
)

enum class LayerSourceType {
  BUILTIN_SPRITE_SHEET,
  BUILTIN_NINJA,
  CUSTOM_URI,
  SOLID_COLOR
}

data class CropRect(
  val left: Float,
  val top: Float,
  val right: Float,
  val bottom: Float
) {
  val width: Float get() = right - left
  val height: Float get() = bottom - top
}

data class ImageLayer(
  val id: String,
  val name: String,
  val sourceType: LayerSourceType = LayerSourceType.BUILTIN_SPRITE_SHEET,
  val customUriString: String? = null,
  val cropRect: CropRect? = null, // In normalized 0..1 coordinates of source bitmap
  val zIndex: Int = 0,
  val visible: Boolean = true,
  val opacity: Float = 1.0f,
  val baseWidth: Float = 120f,
  val baseHeight: Float = 120f,
  val flipHorizontal: Boolean = false,
  // Free manual transform properties for direct PNG animation
  val posX: Float = 0f,
  val posY: Float = 0f,
  val rotationDeg: Float = 0f,
  val scaleX: Float = 1f,
  val scaleY: Float = 1f,
  val pivotX: Float = 0.5f, // 0..1 anchor point
  val pivotY: Float = 0.5f, // 0..1 anchor point
  val parentBoneId: String? = null
)

data class Keyframe(
  val id: String,
  val frame: Int,
  val value: Float,
  val easing: DuikEasingType = DuikEasingType.EASE_IN_OUT
)

data class AnimationTrack(
  val id: String,
  val targetId: String, // boneId OR layerId!
  val property: PropertyType,
  val isLayerTrack: Boolean = false,
  val keyframes: List<Keyframe> = emptyList()
) {
  constructor(
    id: String,
    boneId: String,
    property: PropertyType,
    keyframes: List<Keyframe> = emptyList()
  ) : this(id, boneId, property, false, keyframes)

  // Backward compatibility alias
  val boneId: String get() = targetId
}

data class AnimationClip(
  val name: String = "Main Animation",
  val totalFrames: Int = 60,
  val fps: Int = 24,
  val tracks: List<AnimationTrack> = emptyList()
)

/**
 * Computed world transform of a bone at a specific evaluation point.
 */
data class BoneWorldTransform(
  val boneId: String,
  val startPos: Vec2,
  val endPos: Vec2,
  val angleDeg: Float,
  val length: Float,
  val isController: Boolean
)

data class ProjectMetadata(
  val id: String,
  val name: String,
  val description: String,
  val isBlank: Boolean = false,
  val totalFrames: Int = 48,
  val fps: Int = 24
)
