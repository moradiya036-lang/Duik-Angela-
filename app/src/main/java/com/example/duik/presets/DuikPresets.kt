package com.example.duik.presets

import com.example.duik.math.DuikEasingType
import com.example.duik.math.Vec2
import com.example.duik.model.AnimationClip
import com.example.duik.model.AnimationTrack
import com.example.duik.model.Bone
import com.example.duik.model.ControllerType
import com.example.duik.model.CropRect
import com.example.duik.model.IKConstraint
import com.example.duik.model.ImageLayer
import com.example.duik.model.Keyframe
import com.example.duik.model.LayerSourceType
import com.example.duik.model.PropertyType

data class DuikPreset(
  val id: String,
  val name: String,
  val description: String,
  val bones: List<Bone>,
  val ikConstraints: List<IKConstraint>,
  val layers: List<ImageLayer>,
  val defaultClip: AnimationClip,
  val initialControllerPositions: Map<String, Vec2>
)

object DuikPresets {

  val BlankProjectPreset = createBlankPreset()
  val KaiMangaPreset = createKaiMangaPreset()
  val NinjaKunoichiPreset = createNinjaPreset()
  val DuikHomunculusPreset = createHomunculusPreset()

  val allPresets = listOf(BlankProjectPreset, KaiMangaPreset, NinjaKunoichiPreset, DuikHomunculusPreset)

  private fun createBlankPreset(): DuikPreset {
    return DuikPreset(
      id = "preset_blank",
      name = "Blank PNG Animation Project",
      description = "Clean slate project for importing PNGs, manga art, and manually keyframing transforms.",
      bones = emptyList(),
      ikConstraints = emptyList(),
      layers = emptyList(),
      defaultClip = AnimationClip(
        name = "My PNG Animation",
        totalFrames = 48,
        fps = 24,
        tracks = emptyList()
      ),
      initialControllerPositions = emptyMap()
    )
  }

  private fun createKaiMangaPreset(): DuikPreset {
    val bones = listOf(
      // Master root
      Bone(
        id = "root",
        name = "Master Root",
        parentId = null,
        restLength = 20f,
        restAngleDeg = 0f,
        color = 0xFF00E5FF,
        isController = true,
        controllerType = ControllerType.BOX
      ),
      // Pelvis / Hips
      Bone(
        id = "pelvis",
        name = "Pelvis / Hips",
        parentId = "root",
        restLength = 40f,
        restAngleDeg = -90f, // Upward towards spine
        color = 0xFFFFB300,
        isController = false,
        attachedLayerId = "layer_torso"
      ),
      // Spine / Chest
      Bone(
        id = "spine",
        name = "Spine / Torso",
        parentId = "pelvis",
        restLength = 80f,
        restAngleDeg = 0f,
        color = 0xFFFFB300,
        attachedLayerId = "layer_torso"
      ),
      // Neck & Head
      Bone(
        id = "head",
        name = "Head",
        parentId = "spine",
        restLength = 65f,
        restAngleDeg = 0f,
        color = 0xFFFFB300,
        attachedLayerId = "layer_head"
      ),
      // Dynamic Hair with Wiggle
      Bone(
        id = "hair",
        name = "Hair Tuft (Wiggle)",
        parentId = "head",
        restLength = 45f,
        restAngleDeg = -45f,
        color = 0xFFFF4081,
        enablesWiggle = true,
        wiggleAmp = 15f
      ),

      // Left Arm Chain
      Bone(
        id = "arm_upper_l",
        name = "Left Upper Arm",
        parentId = "spine",
        restLength = 65f,
        restAngleDeg = 110f,
        color = 0xFFFFB300,
        attachedLayerId = "layer_arm_upper_l"
      ),
      Bone(
        id = "arm_lower_l",
        name = "Left Forearm",
        parentId = "arm_upper_l",
        restLength = 60f,
        restAngleDeg = 15f,
        color = 0xFFFFB300,
        attachedLayerId = "layer_arm_lower_l"
      ),
      Bone(
        id = "hand_l",
        name = "Left Hand",
        parentId = "arm_lower_l",
        restLength = 25f,
        restAngleDeg = 0f,
        color = 0xFFFF4081
      ),
      // Left Hand IK Controller
      Bone(
        id = "ctrl_hand_l",
        name = "Hand IK L",
        parentId = "root",
        restLength = 20f,
        color = 0xFF00E5FF,
        isController = true,
        controllerType = ControllerType.CROSSHAIR
      ),

      // Right Arm Chain
      Bone(
        id = "arm_upper_r",
        name = "Right Upper Arm",
        parentId = "spine",
        restLength = 65f,
        restAngleDeg = 70f,
        color = 0xFFFFB300,
        attachedLayerId = "layer_arm_upper_r"
      ),
      Bone(
        id = "arm_lower_r",
        name = "Right Forearm",
        parentId = "arm_upper_r",
        restLength = 60f,
        restAngleDeg = -15f,
        color = 0xFFFFB300,
        attachedLayerId = "layer_arm_lower_r"
      ),
      Bone(
        id = "hand_r",
        name = "Right Hand",
        parentId = "arm_lower_r",
        restLength = 25f,
        restAngleDeg = 0f,
        color = 0xFFFF4081
      ),
      // Right Hand IK Controller
      Bone(
        id = "ctrl_hand_r",
        name = "Hand IK R",
        parentId = "root",
        restLength = 20f,
        color = 0xFF00E5FF,
        isController = true,
        controllerType = ControllerType.CROSSHAIR
      ),

      // Left Leg Chain
      Bone(
        id = "leg_upper_l",
        name = "Left Thigh",
        parentId = "pelvis",
        restLength = 80f,
        restAngleDeg = 170f, // down-left
        color = 0xFFFFB300,
        attachedLayerId = "layer_leg_l"
      ),
      Bone(
        id = "leg_lower_l",
        name = "Left Shin",
        parentId = "leg_upper_l",
        restLength = 80f,
        restAngleDeg = 10f,
        color = 0xFFFFB300,
        attachedLayerId = "layer_shin_l"
      ),
      Bone(
        id = "foot_l",
        name = "Left Foot",
        parentId = "leg_lower_l",
        restLength = 35f,
        restAngleDeg = 80f,
        color = 0xFFFF4081
      ),
      // Left Foot IK Controller
      Bone(
        id = "ctrl_foot_l",
        name = "Foot IK L",
        parentId = "root",
        restLength = 24f,
        color = 0xFF00E5FF,
        isController = true,
        controllerType = ControllerType.FOOT_ROLL
      ),

      // Right Leg Chain
      Bone(
        id = "leg_upper_r",
        name = "Right Thigh",
        parentId = "pelvis",
        restLength = 80f,
        restAngleDeg = -170f, // down-right
        color = 0xFFFFB300,
        attachedLayerId = "layer_leg_r"
      ),
      Bone(
        id = "leg_lower_r",
        name = "Right Shin",
        parentId = "leg_upper_r",
        restLength = 80f,
        restAngleDeg = -10f,
        color = 0xFFFFB300,
        attachedLayerId = "layer_shin_r"
      ),
      Bone(
        id = "foot_r",
        name = "Right Foot",
        parentId = "leg_lower_r",
        restLength = 35f,
        restAngleDeg = -80f,
        color = 0xFFFF4081
      ),
      // Right Foot IK Controller
      Bone(
        id = "ctrl_foot_r",
        name = "Foot IK R",
        parentId = "root",
        restLength = 24f,
        color = 0xFF00E5FF,
        isController = true,
        controllerType = ControllerType.FOOT_ROLL
      )
    )

    val ikConstraints = listOf(
      IKConstraint(
        id = "ik_arm_l",
        name = "IK Left Arm",
        rootBoneId = "arm_upper_l",
        midBoneId = "arm_lower_l",
        endBoneId = "hand_l",
        controllerBoneId = "ctrl_hand_l",
        clockwiseBend = false,
        allowStretch = true
      ),
      IKConstraint(
        id = "ik_arm_r",
        name = "IK Right Arm",
        rootBoneId = "arm_upper_r",
        midBoneId = "arm_lower_r",
        endBoneId = "hand_r",
        controllerBoneId = "ctrl_hand_r",
        clockwiseBend = true,
        allowStretch = true
      ),
      IKConstraint(
        id = "ik_leg_l",
        name = "IK Left Leg",
        rootBoneId = "leg_upper_l",
        midBoneId = "leg_lower_l",
        endBoneId = "foot_l",
        controllerBoneId = "ctrl_foot_l",
        clockwiseBend = true,
        allowStretch = true
      ),
      IKConstraint(
        id = "ik_leg_r",
        name = "IK Right Leg",
        rootBoneId = "leg_upper_r",
        midBoneId = "leg_lower_r",
        endBoneId = "foot_r",
        controllerBoneId = "ctrl_foot_r",
        clockwiseBend = true,
        allowStretch = true
      )
    )

    // Sliced parts from the generated manga sprite sheet
    val layers = listOf(
      ImageLayer(
        id = "layer_head",
        name = "Manga Head",
        sourceType = LayerSourceType.BUILTIN_SPRITE_SHEET,
        cropRect = CropRect(0.04f, 0.03f, 0.22f, 0.22f),
        zIndex = 5,
        baseWidth = 90f,
        baseHeight = 90f
      ),
      ImageLayer(
        id = "layer_torso",
        name = "Jacket Torso",
        sourceType = LayerSourceType.BUILTIN_SPRITE_SHEET,
        cropRect = CropRect(0.40f, 0.20f, 0.60f, 0.46f),
        zIndex = 3,
        baseWidth = 110f,
        baseHeight = 115f
      ),
      ImageLayer(
        id = "layer_arm_upper_l",
        name = "Left Arm Upper",
        sourceType = LayerSourceType.BUILTIN_SPRITE_SHEET,
        cropRect = CropRect(0.06f, 0.33f, 0.16f, 0.48f),
        zIndex = 4,
        baseWidth = 50f,
        baseHeight = 75f
      ),
      ImageLayer(
        id = "layer_arm_lower_l",
        name = "Left Forearm & Glove",
        sourceType = LayerSourceType.BUILTIN_SPRITE_SHEET,
        cropRect = CropRect(0.05f, 0.48f, 0.14f, 0.68f),
        zIndex = 4,
        baseWidth = 45f,
        baseHeight = 70f
      ),
      ImageLayer(
        id = "layer_arm_upper_r",
        name = "Right Arm Upper",
        sourceType = LayerSourceType.BUILTIN_SPRITE_SHEET,
        cropRect = CropRect(0.84f, 0.33f, 0.94f, 0.48f),
        zIndex = 2,
        baseWidth = 50f,
        baseHeight = 75f
      ),
      ImageLayer(
        id = "layer_arm_lower_r",
        name = "Right Forearm",
        sourceType = LayerSourceType.BUILTIN_SPRITE_SHEET,
        cropRect = CropRect(0.85f, 0.48f, 0.95f, 0.68f),
        zIndex = 2,
        baseWidth = 45f,
        baseHeight = 70f
      ),
      ImageLayer(
        id = "layer_leg_l",
        name = "Left Thigh",
        sourceType = LayerSourceType.BUILTIN_SPRITE_SHEET,
        cropRect = CropRect(0.21f, 0.56f, 0.33f, 0.72f),
        zIndex = 1,
        baseWidth = 55f,
        baseHeight = 85f
      ),
      ImageLayer(
        id = "layer_shin_l",
        name = "Left Boot & Shin",
        sourceType = LayerSourceType.BUILTIN_SPRITE_SHEET,
        cropRect = CropRect(0.14f, 0.72f, 0.28f, 0.95f),
        zIndex = 1,
        baseWidth = 60f,
        baseHeight = 90f
      ),
      ImageLayer(
        id = "layer_leg_r",
        name = "Right Thigh",
        sourceType = LayerSourceType.BUILTIN_SPRITE_SHEET,
        cropRect = CropRect(0.67f, 0.56f, 0.79f, 0.72f),
        zIndex = 1,
        baseWidth = 55f,
        baseHeight = 85f
      ),
      ImageLayer(
        id = "layer_shin_r",
        name = "Right Boot & Shin",
        sourceType = LayerSourceType.BUILTIN_SPRITE_SHEET,
        cropRect = CropRect(0.71f, 0.72f, 0.85f, 0.95f),
        zIndex = 1,
        baseWidth = 60f,
        baseHeight = 90f
      )
    )

    // Initial controller positions
    val initialControllers = mapOf(
      "root" to Vec2(0f, -50f),
      "ctrl_hand_l" to Vec2(110f, -100f),
      "ctrl_hand_r" to Vec2(-105f, -85f),
      "ctrl_foot_l" to Vec2(45f, 160f),
      "ctrl_foot_r" to Vec2(-45f, 160f)
    )

    // Pre-animated combat pose and idle breathe clip
    val tracks = listOf(
      AnimationTrack(
        id = "track_hand_l_y",
        boneId = "ctrl_hand_l",
        property = PropertyType.POS_Y,
        keyframes = listOf(
          Keyframe("k1", 0, -100f, DuikEasingType.EASE_IN_OUT),
          Keyframe("k2", 24, -135f, DuikEasingType.OVERSHOOT),
          Keyframe("k3", 48, -100f, DuikEasingType.EASE_IN_OUT)
        )
      ),
      AnimationTrack(
        id = "track_spine_rot",
        boneId = "spine",
        property = PropertyType.ROTATION,
        keyframes = listOf(
          Keyframe("k4", 0, 0f, DuikEasingType.EASE_IN_OUT),
          Keyframe("k5", 24, 6f, DuikEasingType.EASE_IN_OUT),
          Keyframe("k6", 48, 0f, DuikEasingType.EASE_IN_OUT)
        )
      ),
      AnimationTrack(
        id = "track_head_rot",
        boneId = "head",
        property = PropertyType.ROTATION,
        keyframes = listOf(
          Keyframe("k7", 0, 0f, DuikEasingType.EASE_IN_OUT),
          Keyframe("k8", 24, -4f, DuikEasingType.BOUNCE),
          Keyframe("k9", 48, 0f, DuikEasingType.EASE_IN_OUT)
        )
      )
    )

    return DuikPreset(
      id = "preset_kai",
      name = "Kai: Manga Blade Master",
      description = "Full 2-Bone IK biped rig with anime cel-shaded parts, flowing hair wiggle, and hand/foot controllers.",
      bones = bones,
      ikConstraints = ikConstraints,
      layers = layers,
      defaultClip = AnimationClip(
        name = "Dynamic Combat Idle",
        totalFrames = 48,
        fps = 24,
        tracks = tracks
      ),
      initialControllerPositions = initialControllers
    )
  }

  private fun createNinjaPreset(): DuikPreset {
    val bones = listOf(
      Bone("root", "Master Root", null, 20f, 0f, 0xFF00E5FF, true, ControllerType.BOX),
      Bone("torso", "Ninja Torso", "root", 90f, -90f, 0xFFFFB300, attachedLayerId = "layer_ninja_full"),
      Bone("head", "Ninja Head", "torso", 60f, 0f, 0xFFFFB300),
      Bone("scarf_1", "Scarf Segment 1", "torso", 45f, 40f, 0xFFFF4081, enablesWiggle = true, wiggleAmp = 18f),
      Bone("scarf_2", "Scarf Segment 2", "scarf_1", 50f, 20f, 0xFFFF4081, enablesWiggle = true, wiggleAmp = 25f),
      Bone("scarf_3", "Scarf Tip", "scarf_2", 55f, 15f, 0xFFFF4081, enablesWiggle = true, wiggleAmp = 30f),
      Bone("blade", "Katana Blade", "torso", 110f, 10f, 0xFF00E676, enablesWiggle = true, wiggleAmp = 6f)
    )

    val layers = listOf(
      ImageLayer(
        id = "layer_ninja_full",
        name = "Kunoichi Full Art",
        sourceType = LayerSourceType.BUILTIN_NINJA,
        cropRect = null,
        zIndex = 2,
        baseWidth = 260f,
        baseHeight = 260f
      )
    )

    val initialControllers = mapOf(
      "root" to Vec2(0f, 0f)
    )

    val tracks = listOf(
      AnimationTrack(
        id = "track_ninja_root",
        boneId = "root",
        property = PropertyType.POS_Y,
        keyframes = listOf(
          Keyframe("k1", 0, 0f, DuikEasingType.EASE_IN_OUT),
          Keyframe("k2", 20, -18f, DuikEasingType.OVERSHOOT),
          Keyframe("k3", 40, 0f, DuikEasingType.EASE_IN_OUT)
        )
      )
    )

    return DuikPreset(
      id = "preset_ninja",
      name = "Kunoichi Shadow",
      description = "Manga ninja art with multi-segment scarf dynamic wind physics and sword flourish.",
      bones = bones,
      ikConstraints = emptyList(),
      layers = layers,
      defaultClip = AnimationClip("Wind Gust & Poise", 40, 24, tracks),
      initialControllerPositions = initialControllers
    )
  }

  private fun createHomunculusPreset(): DuikPreset {
    val bones = listOf(
      Bone("root", "Master Root", null, 25f, 0f, 0xFF00E5FF, true, ControllerType.BOX),
      Bone("pelvis", "Pelvis", "root", 45f, -90f, 0xFFFFB300),
      Bone("spine", "Spine", "pelvis", 75f, 0f, 0xFFFFB300),
      Bone("head", "Head", "spine", 50f, 0f, 0xFFFFB300),
      Bone("arm_upper_l", "Arm Upper L", "spine", 65f, 110f, 0xFFFFB300),
      Bone("arm_lower_l", "Arm Lower L", "arm_upper_l", 60f, 20f, 0xFFFFB300),
      Bone("hand_l", "Hand L", "arm_lower_l", 25f, 0f, 0xFFFF4081),
      Bone("ctrl_hand_l", "IK Hand L", "root", 20f, 0f, 0xFF00E5FF, true, ControllerType.CROSSHAIR),
      Bone("arm_upper_r", "Arm Upper R", "spine", 65f, 70f, 0xFFFFB300),
      Bone("arm_lower_r", "Arm Lower R", "arm_upper_r", 60f, -20f, 0xFFFFB300),
      Bone("hand_r", "Hand R", "arm_lower_r", 25f, 0f, 0xFFFF4081),
      Bone("ctrl_hand_r", "IK Hand R", "root", 20f, 0f, 0xFF00E5FF, true, ControllerType.CROSSHAIR),
      Bone("leg_upper_l", "Thigh L", "pelvis", 80f, 170f, 0xFFFFB300),
      Bone("leg_lower_l", "Shin L", "leg_upper_l", 80f, 10f, 0xFFFFB300),
      Bone("foot_l", "Foot L", "leg_lower_l", 30f, 80f, 0xFFFF4081),
      Bone("ctrl_foot_l", "IK Foot L", "root", 24f, 0f, 0xFF00E5FF, true, ControllerType.FOOT_ROLL),
      Bone("leg_upper_r", "Thigh R", "pelvis", 80f, -170f, 0xFFFFB300),
      Bone("leg_lower_r", "Shin R", "leg_upper_r", 80f, -10f, 0xFFFFB300),
      Bone("foot_r", "Foot R", "leg_lower_r", 30f, -80f, 0xFFFF4081),
      Bone("ctrl_foot_r", "IK Foot R", "root", 24f, 0f, 0xFF00E5FF, true, ControllerType.FOOT_ROLL)
    )

    val ikConstraints = listOf(
      IKConstraint("ik_arm_l", "IK Arm L", "arm_upper_l", "arm_lower_l", "hand_l", "ctrl_hand_l", false, true),
      IKConstraint("ik_arm_r", "IK Arm R", "arm_upper_r", "arm_lower_r", "hand_r", "ctrl_hand_r", true, true),
      IKConstraint("ik_leg_l", "IK Leg L", "leg_upper_l", "leg_lower_l", "foot_l", "ctrl_foot_l", true, true),
      IKConstraint("ik_leg_r", "IK Leg R", "leg_upper_r", "leg_lower_r", "foot_r", "ctrl_foot_r", true, true)
    )

    val initialControllers = mapOf(
      "root" to Vec2(0f, -40f),
      "ctrl_hand_l" to Vec2(100f, -80f),
      "ctrl_hand_r" to Vec2(-100f, -80f),
      "ctrl_foot_l" to Vec2(40f, 150f),
      "ctrl_foot_r" to Vec2(-40f, 150f)
    )

    return DuikPreset(
      id = "preset_homunculus",
      name = "Duik Angela Homunculus",
      description = "Universal After Effects Duik Angela 2-Bone biped structure ready to parent custom imported images.",
      bones = bones,
      ikConstraints = ikConstraints,
      layers = emptyList(),
      defaultClip = AnimationClip("Rest T-Pose", 24, 24, emptyList()),
      initialControllerPositions = initialControllers
    )
  }
}
