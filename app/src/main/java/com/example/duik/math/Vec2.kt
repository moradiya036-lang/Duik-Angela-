package com.example.duik.math

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class Vec2(val x: Float = 0f, val y: Float = 0f) {
  operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)
  operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)
  operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)
  operator fun div(scalar: Float): Vec2 = if (scalar != 0f) Vec2(x / scalar, y / scalar) else Vec2(0f, 0f)
  operator fun unaryMinus(): Vec2 = Vec2(-x, -y)

  fun length(): Float = hypot(x, y)
  fun lengthSquared(): Float = x * x + y * y

  fun normalized(): Vec2 {
    val len = length()
    return if (len > 0.00001f) Vec2(x / len, y / len) else Vec2(0f, 0f)
  }

  fun distanceTo(other: Vec2): Float = (other - this).length()

  fun angleDegrees(): Float {
    val rad = atan2(y, x)
    return Math.toDegrees(rad.toDouble()).toFloat()
  }

  fun rotateDegrees(angleDeg: Float): Vec2 {
    val rad = Math.toRadians(angleDeg.toDouble())
    val c = cos(rad).toFloat()
    val s = sin(rad).toFloat()
    return Vec2(x * c - y * s, x * s + y * c)
  }

  companion object {
    val Zero = Vec2(0f, 0f)
    val One = Vec2(1f, 1f)

    fun lerp(a: Vec2, b: Vec2, t: Float): Vec2 =
      Vec2(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

    fun fromAngleAndLength(angleDeg: Float, length: Float): Vec2 {
      val rad = Math.toRadians(angleDeg.toDouble())
      return Vec2((cos(rad) * length).toFloat(), (sin(rad) * length).toFloat())
    }
  }
}
