package com.example.duik.engine

import com.example.duik.math.DuikAutomations
import com.example.duik.math.TwoBoneIKSolver
import com.example.duik.math.Vec2
import com.example.duik.math.WalkCycleSettings
import com.example.duik.model.Bone
import com.example.duik.model.BoneWorldTransform
import com.example.duik.model.IKConstraint

data class LocalPose(
  val localTranslations: Map<String, Vec2> = emptyMap(),
  val localRotations: Map<String, Float> = emptyMap(),
  val controllerPositions: Map<String, Vec2> = emptyMap()
)

object DuikRigEngine {

  /**
   * Evaluates the complete skeleton pose:
   * 1. Evaluates base hierarchy transforms from Root downwards.
   * 2. Evaluates IK Constraints (solving 2-bone analytical IK for arms and legs).
   * 3. Applies secondary wiggle/physics.
   */
  fun evaluateSkeleton(
    bones: List<Bone>,
    ikConstraints: List<IKConstraint>,
    localPose: LocalPose,
    walkOffsets: DuikAutomations.WalkCycleOffsets?,
    timeSec: Float,
    wiggleEnabled: Boolean = false
  ): Map<String, BoneWorldTransform> {
    val boneMap = bones.associateBy { it.id }
    val transforms = mutableMapOf<String, BoneWorldTransform>()

    // Identify roots and order bones topologically parent -> child
    val childrenMap = mutableMapOf<String?, MutableList<Bone>>()
    for (b in bones) {
      childrenMap.getOrPut(b.parentId) { mutableListOf() }.add(b)
    }

    // Stack or queue for forward kinematics
    fun solveFK(parentBoneId: String?, parentEndPos: Vec2, parentAngleDeg: Float) {
      val children = childrenMap[parentBoneId] ?: return
      for (b in children) {
        val localOffset = localPose.localTranslations[b.id] ?: Vec2.Zero
        val localRot = localPose.localRotations[b.id] ?: 0f

        var worldAngle = parentAngleDeg + b.restAngleDeg + localRot

        // Secondary motion wiggle if enabled
        if (wiggleEnabled && b.enablesWiggle) {
          val wiggle = DuikAutomations.calculateWiggle(timeSec, b.id.hashCode(), 2.8f, b.wiggleAmp)
          worldAngle += wiggle
        }

        val startPos = parentEndPos + localOffset
        val length = b.restLength
        val endPos = startPos + Vec2.fromAngleAndLength(worldAngle, length)

        transforms[b.id] = BoneWorldTransform(
          boneId = b.id,
          startPos = startPos,
          endPos = endPos,
          angleDeg = worldAngle,
          length = length,
          isController = b.isController
        )

        // Recurse children
        solveFK(b.id, endPos, worldAngle)
      }
    }

    // Master root start position
    val masterRootPos = localPose.controllerPositions["root"] ?: Vec2(0f, 0f)
    solveFK(null, masterRootPos, 0f)

    // Now evaluate 2-Bone IK Constraints
    for (ik in ikConstraints) {
      if (!ik.enabled) continue
      val rootBone = boneMap[ik.rootBoneId] ?: continue
      val midBone = boneMap[ik.midBoneId] ?: continue
      val endBone = boneMap[ik.endBoneId] ?: continue

      val rootTransform = transforms[ik.rootBoneId] ?: continue
      val rootOrigin = rootTransform.startPos

      // Target position comes from controller override or walk cycle
      var targetPos = localPose.controllerPositions[ik.controllerBoneId]
        ?: (transforms[ik.endBoneId]?.endPos ?: (rootOrigin + Vec2(0f, rootBone.restLength + midBone.restLength)))

      // If walk cycle is active and this is a designated limb, apply walk offset
      if (walkOffsets != null) {
        when (ik.id) {
          "ik_leg_l" -> targetPos = walkOffsets.leftFootPos
          "ik_leg_r" -> targetPos = walkOffsets.rightFootPos
          "ik_arm_l" -> targetPos = walkOffsets.leftHandPos
          "ik_arm_r" -> targetPos = walkOffsets.rightHandPos
        }
      }

      val ikResult = TwoBoneIKSolver.solve(
        rootPos = rootOrigin,
        targetPos = targetPos,
        length1 = rootBone.restLength,
        length2 = midBone.restLength,
        clockwise = ik.clockwiseBend,
        allowStretch = ik.allowStretch
      )

      // Update root bone transform
      val newRootTransform = BoneWorldTransform(
        boneId = rootBone.id,
        startPos = rootOrigin,
        endPos = ikResult.midPos,
        angleDeg = ikResult.rootAngleDeg,
        length = ikResult.stretchedLength1,
        isController = rootBone.isController
      )
      transforms[rootBone.id] = newRootTransform

      // Update mid bone transform
      val newMidTransform = BoneWorldTransform(
        boneId = midBone.id,
        startPos = ikResult.midPos,
        endPos = ikResult.actualEndPos,
        angleDeg = ikResult.midAngleDeg,
        length = ikResult.stretchedLength2,
        isController = midBone.isController
      )
      transforms[midBone.id] = newMidTransform

      // Propagate end bone (e.g. Foot or Hand)
      val endLocalRot = localPose.localRotations[endBone.id] ?: 0f
      val endAngle = ikResult.midAngleDeg + endBone.restAngleDeg + endLocalRot
      val endEndPos = ikResult.actualEndPos + Vec2.fromAngleAndLength(endAngle, endBone.restLength)
      transforms[endBone.id] = BoneWorldTransform(
        boneId = endBone.id,
        startPos = ikResult.actualEndPos,
        endPos = endEndPos,
        angleDeg = endAngle,
        length = endBone.restLength,
        isController = endBone.isController
      )
    }

    return transforms
  }
}
