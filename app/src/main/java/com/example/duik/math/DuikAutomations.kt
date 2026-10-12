package com.example.duik.math

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class WalkCycleSettings(
  val enabled: Boolean = false,
  val speed: Float = 1.0f,
  val strideLength: Float = 90f,
  val stepHeight: Float = 38f,
  val hipBob: Float = 14f,
  val armSwing: Float = 60f,
  val bodyLean: Float = 4f
)

data class SecondaryMotionSettings(
  val wiggleEnabled: Boolean = false,
  val wiggleFrequency: Float = 2.5f,
  val wiggleAmplitude: Float = 12f,
  val springLagEnabled: Boolean = false,
  val springDamping: Float = 0.85f
)

object DuikAutomations {

  /**
   * Generates Duik Angela harmonic wiggle noise for secondary motion (e.g., hair, ribbons, swords).
   */
  fun calculateWiggle(timeSec: Float, seed: Int, frequency: Float, amplitude: Float): Float {
    val phase1 = timeSec * frequency * 2f * PI.toFloat() + seed * 1.37f
    val phase2 = timeSec * (frequency * 1.618f) * 2f * PI.toFloat() + seed * 2.89f
    return (sin(phase1) * 0.7f + sin(phase2) * 0.3f) * amplitude
  }

  /**
   * Procedural Walk Cycle posing engine for Duik Angela biped rigs.
   * Calculates offsets for:
   * - Left Foot IK Target
   * - Right Foot IK Target
   * - Pelvis / Hips Position Offset
   * - Left Hand IK Target
   * - Right Hand IK Target
   * - Torso & Head Rotation
   */
  data class WalkCycleOffsets(
    val leftFootPos: Vec2,
    val rightFootPos: Vec2,
    val hipsOffset: Vec2,
    val leftHandPos: Vec2,
    val rightHandPos: Vec2,
    val spineAngleOffset: Float,
    val headAngleOffset: Float
  )

  fun computeWalkCycle(
    cycleTimeSec: Float,
    baseLeftFootRest: Vec2,
    baseRightFootRest: Vec2,
    baseLeftHandRest: Vec2,
    baseRightHandRest: Vec2,
    settings: WalkCycleSettings
  ): WalkCycleOffsets {
    if (!settings.enabled) {
      return WalkCycleOffsets(
        leftFootPos = baseLeftFootRest,
        rightFootPos = baseRightFootRest,
        hipsOffset = Vec2.Zero,
        leftHandPos = baseLeftHandRest,
        rightHandPos = baseRightHandRest,
        spineAngleOffset = 0f,
        headAngleOffset = 0f
      )
    }

    val progress = (cycleTimeSec * settings.speed) % 1.0f
    val radPhase = progress * 2f * PI.toFloat()

    // Hips bob down on contact, up on passing (frequency 2x)
    val hipY = -abs(sin(radPhase)) * settings.hipBob
    val hipX = sin(radPhase) * (settings.strideLength * 0.08f)
    val hipsOffset = Vec2(hipX, hipY)

    // Left Foot trajectory:
    // Stride moves horizontally back and forth, lifts up during swing phase (half of cycle)
    val footPhaseL = radPhase
    val footPhaseR = radPhase + PI.toFloat() // 180 deg out of phase

    val footX_L = cos(footPhaseL) * (settings.strideLength * 0.5f)
    // Lift foot only during forward swing phase
    val liftL = if (sin(footPhaseL) < 0f) -sin(footPhaseL) * settings.stepHeight else 0f
    val leftFootPos = baseLeftFootRest + Vec2(footX_L, -liftL)

    val footX_R = cos(footPhaseR) * (settings.strideLength * 0.5f)
    val liftR = if (sin(footPhaseR) < 0f) -sin(footPhaseR) * settings.stepHeight else 0f
    val rightFootPos = baseRightFootRest + Vec2(footX_R, -liftR)

    // Arm swing is opposite to foot swing (counter-balancing torso)
    val armX_L = -cos(footPhaseL) * (settings.armSwing * 0.5f)
    val armY_L = sin(footPhaseL * 2f) * (settings.armSwing * 0.15f)
    val leftHandPos = baseLeftHandRest + Vec2(armX_L, armY_L)

    val armX_R = -cos(footPhaseR) * (settings.armSwing * 0.5f)
    val armY_R = sin(footPhaseR * 2f) * (settings.armSwing * 0.15f)
    val rightHandPos = baseRightHandRest + Vec2(armX_R, armY_R)

    // Torso lean and gentle head counter-tilt
    val spineAngle = sin(radPhase) * settings.bodyLean
    val headAngle = -spineAngle * 0.5f

    return WalkCycleOffsets(
      leftFootPos = leftFootPos,
      rightFootPos = rightFootPos,
      hipsOffset = hipsOffset,
      leftHandPos = leftHandPos,
      rightHandPos = rightHandPos,
      spineAngleOffset = spineAngle,
      headAngleOffset = headAngle
    )
  }
}
