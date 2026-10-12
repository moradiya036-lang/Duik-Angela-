package com.example.duik.math

import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min

data class IKResult(
  val rootAngleDeg: Float,
  val midAngleDeg: Float,
  val midPos: Vec2,
  val actualEndPos: Vec2,
  val stretchedLength1: Float,
  val stretchedLength2: Float,
  val isStretching: Boolean
)

object TwoBoneIKSolver {

  /**
   * Duik Angela 2-Bone Analytical Inverse Kinematics Solver.
   *
   * @param rootPos Origin position of first bone (e.g., Shoulder or Thigh).
   * @param targetPos Desired position of end bone (e.g., Hand or Foot).
   * @param length1 Rest length of upper segment (e.g., Bicep / Upper Leg).
   * @param length2 Rest length of lower segment (e.g., Forearm / Shin).
   * @param clockwise If true, joint bends clockwise (e.g., human elbow vs knee).
   * @param allowStretch If true, bones stretch when target is farther than length1 + length2.
   * @param maxStretch Multiplier for maximum stretch (e.g. 1.5 = 150%).
   */
  fun solve(
    rootPos: Vec2,
    targetPos: Vec2,
    length1: Float,
    length2: Float,
    clockwise: Boolean = false,
    allowStretch: Boolean = true,
    maxStretch: Float = 1.6f
  ): IKResult {
    val toTarget = targetPos - rootPos
    val dist = max(0.001f, toTarget.length())
    val baseAngleToTarget = toTarget.angleDegrees()

    val totalLength = length1 + length2
    val minReach = max(0.001f, kotlin.math.abs(length1 - length2))

    var l1 = length1
    var l2 = length2
    var isStretching = false

    if (dist > totalLength) {
      if (allowStretch) {
        val stretchFactor = min(maxStretch, dist / totalLength)
        l1 = length1 * stretchFactor
        l2 = length2 * stretchFactor
        isStretching = stretchFactor > 1.01f
      }
    }

    val curTotalLength = l1 + l2
    val clampedDist = if (dist > curTotalLength) {
      curTotalLength - 0.0001f
    } else if (dist < minReach) {
      minReach + 0.0001f
    } else {
      dist
    }

    // Law of Cosines
    // cos(alpha) = (L1^2 + Dist^2 - L2^2) / (2 * L1 * Dist)
    val cosAlpha = ((l1 * l1) + (clampedDist * clampedDist) - (l2 * l2)) / (2f * l1 * clampedDist)
    val safeCosAlpha = cosAlpha.coerceIn(-1f, 1f)
    val alphaRad = acos(safeCosAlpha)
    val alphaDeg = Math.toDegrees(alphaRad.toDouble()).toFloat()

    // cos(beta) = (L1^2 + L2^2 - Dist^2) / (2 * L1 * L2)
    val cosBeta = ((l1 * l1) + (l2 * l2) - (clampedDist * clampedDist)) / (2f * l1 * l2)
    val safeCosBeta = cosBeta.coerceIn(-1f, 1f)
    val betaRad = acos(safeCosBeta)
    val betaDeg = Math.toDegrees(betaRad.toDouble()).toFloat()

    val bendSign = if (clockwise) 1f else -1f

    val rootAngle = baseAngleToTarget + (bendSign * alphaDeg)
    val midPos = rootPos + Vec2.fromAngleAndLength(rootAngle, l1)

    // Exterior angle for the mid joint
    val midAngle = rootAngle + (bendSign * (180f - betaDeg))
    val endPos = midPos + Vec2.fromAngleAndLength(midAngle, l2)

    return IKResult(
      rootAngleDeg = rootAngle,
      midAngleDeg = midAngle,
      midPos = midPos,
      actualEndPos = if (dist > curTotalLength && !allowStretch) endPos else targetPos,
      stretchedLength1 = l1,
      stretchedLength2 = l2,
      isStretching = isStretching
    )
  }
}
