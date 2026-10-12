package com.example.duik.math

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

enum class DuikEasingType(val displayName: String, val iconName: String) {
  LINEAR("Linear", "━"),
  EASE_IN("Ease In", "◢"),
  EASE_OUT("Ease Out", "◣"),
  EASE_IN_OUT("Ease Both", "∿"),
  OVERSHOOT("Duik Overshoot", "⤹"),
  BOUNCE("Duik Bounce", "⤸"),
  ELASTIC("Elastic Spring", "⌇")
}

object DuikEasing {

  fun interpolate(t: Float, type: DuikEasingType): Float {
    val clampedT = t.coerceIn(0f, 1f)
    return when (type) {
      DuikEasingType.LINEAR -> clampedT
      DuikEasingType.EASE_IN -> clampedT * clampedT * clampedT
      DuikEasingType.EASE_OUT -> 1f - (1f - clampedT).pow(3f)
      DuikEasingType.EASE_IN_OUT -> {
        if (clampedT < 0.5f) {
          4f * clampedT * clampedT * clampedT
        } else {
          1f - (-2f * clampedT + 2f).pow(3f) / 2f
        }
      }
      DuikEasingType.OVERSHOOT -> {
        // Classic Duik Angela inertial overshoot formula
        val c4 = (2f * Math.PI / 3f).toFloat()
        if (clampedT == 0f) 0f
        else if (clampedT == 1f) 1f
        else {
          val s = 1.70158f * 1.5f
          val postT = clampedT - 1f
          postT * postT * ((s + 1f) * postT + s) + 1f
        }
      }
      DuikEasingType.BOUNCE -> {
        // Classic Duik bounce decay
        val n1 = 7.5625f
        val d1 = 2.75f
        var x = clampedT
        if (x < 1f / d1) {
          n1 * x * x
        } else if (x < 2f / d1) {
          x -= 1.5f / d1
          n1 * x * x + 0.75f
        } else if (x < 2.5f / d1) {
          x -= 2.25f / d1
          n1 * x * x + 0.9375f
        } else {
          x -= 2.625f / d1
          n1 * x * x + 0.984375f
        }
      }
      DuikEasingType.ELASTIC -> {
        val c4 = (2f * PI / 3f).toFloat()
        if (clampedT == 0f) 0f
        else if (clampedT == 1f) 1f
        else 2f.pow(-10f * clampedT) * sin((clampedT * 10f - 0.75f) * c4) + 1f
      }
    }
  }

  fun interpolateFloat(start: Float, end: Float, t: Float, type: DuikEasingType): Float {
    val factor = interpolate(t, type)
    return start + (end - start) * factor
  }

  fun interpolateVec2(start: Vec2, end: Vec2, t: Float, type: DuikEasingType): Vec2 {
    val factor = interpolate(t, type)
    return Vec2(
      start.x + (end.x - start.x) * factor,
      start.y + (end.y - start.y) * factor
    )
  }
}
