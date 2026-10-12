package com.example

import com.example.duik.engine.DuikRigEngine
import com.example.duik.engine.LocalPose
import com.example.duik.math.DuikEasing
import com.example.duik.math.DuikEasingType
import com.example.duik.math.TwoBoneIKSolver
import com.example.duik.math.Vec2
import com.example.duik.presets.DuikPresets
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testTwoBoneIKSolver_reachableTarget() {
    val root = Vec2(0f, 0f)
    val target = Vec2(0f, 100f)
    val l1 = 60f
    val l2 = 60f

    val result = TwoBoneIKSolver.solve(
      rootPos = root,
      targetPos = target,
      length1 = l1,
      length2 = l2,
      clockwise = false
    )

    // Mid joint should have moved away from straight line to bend
    assertNotEquals(0f, result.midPos.x, 0.001f)
    // Distance from root to mid should match l1
    assertEquals(l1, result.rootPosDistance(root), 0.1f)
  }

  private fun com.example.duik.math.IKResult.rootPosDistance(root: Vec2): Float {
    return (midPos - root).length()
  }

  @Test
  fun testDuikEasing_endpoints() {
    for (easing in DuikEasingType.values()) {
      val startVal = DuikEasing.interpolate(0f, easing)
      val endVal = DuikEasing.interpolate(1f, easing)
      assertEquals(0f, startVal, 0.001f)
      assertEquals(1f, endVal, 0.001f)
    }
  }

  @Test
  fun testDuikRigEngine_evaluatesPreset() {
    val preset = DuikPresets.KaiMangaPreset
    val transforms = DuikRigEngine.evaluateSkeleton(
      bones = preset.bones,
      ikConstraints = preset.ikConstraints,
      localPose = LocalPose(
        localTranslations = emptyMap(),
        localRotations = emptyMap(),
        controllerPositions = preset.initialControllerPositions
      ),
      walkOffsets = null,
      timeSec = 0f,
      wiggleEnabled = false
    )

    assertTrue("Should compute transforms for all bones", transforms.isNotEmpty())
    assertNotNull(transforms["spine"])
    assertNotNull(transforms["head"])
  }
}
