package com.example.duik.ui.stage

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.example.duik.math.Vec2
import com.example.duik.model.Bone
import com.example.duik.model.BoneWorldTransform
import com.example.duik.model.ControllerType
import com.example.duik.model.ImageLayer
import com.example.duik.slice.DuikImageManager
import com.example.duik.viewmodel.DuikUiState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private enum class DragTargetMode {
  NONE,
  CANVAS_PAN,
  BONE_CONTROLLER,
  LAYER_TRANSLATE,
  LAYER_ROTATE,
  LAYER_PIVOT
}

@Composable
fun DuikStageCanvas(
  uiState: DuikUiState,
  onSelectBone: (String?) -> Unit,
  onSelectLayer: (String?) -> Unit,
  onDragController: (String, Vec2) -> Unit,
  onMoveLayer: (String, Vec2) -> Unit,
  onRotateLayer: (String, Float) -> Unit,
  onSetLayerPivot: (String, Vec2) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  var zoomScale by remember { mutableFloatStateOf(1.0f) }
  var panOffset by remember { mutableStateOf(Offset.Zero) }

  var dragMode by remember { mutableStateOf(DragTargetMode.NONE) }
  var activeDraggingId by remember { mutableStateOf<String?>(null) }
  var initialDragWorld by remember { mutableStateOf(Vec2.Zero) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF101217))
      .pointerInput(uiState.bones, uiState.layers, uiState.computedTransforms, zoomScale, panOffset) {
        detectTapGestures { tapOffset ->
          val centerX = size.width / 2f + panOffset.x
          val centerY = size.height / 2f + panOffset.y
          val worldX = (tapOffset.x - centerX) / zoomScale
          val worldY = (tapOffset.y - centerY) / zoomScale
          val tapWorld = Vec2(worldX, worldY)

          // 1. Check if tapped any bone / controller
          var clickedBone: String? = null
          var minBoneDist = 48f / zoomScale

          for (b in uiState.bones) {
            val trans = uiState.computedTransforms[b.id] ?: continue
            val posToCheck = if (b.isController) trans.startPos else trans.endPos
            val d = posToCheck.distanceTo(tapWorld)
            if (d < minBoneDist) {
              minBoneDist = d
              clickedBone = b.id
            }
          }

          if (clickedBone != null) {
            onSelectBone(clickedBone)
            return@detectTapGestures
          }

          // 2. Check if tapped any ImageLayer
          var clickedLayer: String? = null
          for (layer in uiState.layers.filter { it.visible }.reversed()) {
            val halfW = (layer.baseWidth * layer.scaleX) / 2f
            val halfH = (layer.baseHeight * layer.scaleY) / 2f

            // If layer is attached to a bone, check near bone position
            val centerPos = if (layer.parentBoneId != null) {
              uiState.computedTransforms[layer.parentBoneId]?.startPos ?: Vec2(layer.posX, layer.posY)
            } else {
              Vec2(layer.posX, layer.posY)
            }

            if (tapWorld.x in (centerPos.x - halfW)..(centerPos.x + halfW) &&
                tapWorld.y in (centerPos.y - halfH)..(centerPos.y + halfH)) {
              clickedLayer = layer.id
              break
            }
          }

          if (clickedLayer != null) {
            onSelectLayer(clickedLayer)
          } else {
            // Deselect all
            onSelectBone(null)
            onSelectLayer(null)
          }
        }
      }
      .pointerInput(uiState.selectedBoneId, uiState.selectedLayerId, uiState.layers, zoomScale, panOffset) {
        detectDragGestures(
          onDragStart = { startOffset ->
            val centerX = size.width / 2f + panOffset.x
            val centerY = size.height / 2f + panOffset.y
            val worldX = (startOffset.x - centerX) / zoomScale
            val worldY = (startOffset.y - centerY) / zoomScale
            val tapWorld = Vec2(worldX, worldY)
            initialDragWorld = tapWorld

            val selBoneId = uiState.selectedBoneId
            val selLayerId = uiState.selectedLayerId

            // Check if selected layer's rotation knob was tapped
            if (selLayerId != null) {
              val layer = uiState.layers.find { it.id == selLayerId }
              if (layer != null) {
                val layerPos = Vec2(layer.posX, layer.posY)
                val halfH = (layer.baseHeight * layer.scaleY) / 2f
                val rotHandlePos = layerPos + Vec2(0f, -halfH - 28f).rotateDegrees(layer.rotationDeg)
                if (tapWorld.distanceTo(rotHandlePos) < 35f / zoomScale) {
                  dragMode = DragTargetMode.LAYER_ROTATE
                  activeDraggingId = selLayerId
                  return@detectDragGestures
                }

                // Check pivot handle
                val pivotOffset = Vec2(
                  (layer.pivotX - 0.5f) * layer.baseWidth * layer.scaleX,
                  (layer.pivotY - 0.5f) * layer.baseHeight * layer.scaleY
                ).rotateDegrees(layer.rotationDeg)
                val pivotWorldPos = layerPos + pivotOffset
                if (tapWorld.distanceTo(pivotWorldPos) < 25f / zoomScale) {
                  dragMode = DragTargetMode.LAYER_PIVOT
                  activeDraggingId = selLayerId
                  return@detectDragGestures
                }

                // Check layer body translation
                val halfW = (layer.baseWidth * layer.scaleX) / 2f
                if (tapWorld.x in (layerPos.x - halfW)..(layerPos.x + halfW) &&
                    tapWorld.y in (layerPos.y - halfH)..(layerPos.y + halfH)) {
                  dragMode = DragTargetMode.LAYER_TRANSLATE
                  activeDraggingId = selLayerId
                  return@detectDragGestures
                }
              }
            }

            // Check if dragging a bone controller
            if (selBoneId != null) {
              val trans = uiState.computedTransforms[selBoneId]
              if (trans != null && trans.startPos.distanceTo(tapWorld) < 55f / zoomScale) {
                dragMode = DragTargetMode.BONE_CONTROLLER
                activeDraggingId = selBoneId
                return@detectDragGestures
              }
            }

            // Search any bone controller nearby
            for (b in uiState.bones) {
              if (!b.isController) continue
              val trans = uiState.computedTransforms[b.id] ?: continue
              if (trans.startPos.distanceTo(tapWorld) < 50f / zoomScale) {
                dragMode = DragTargetMode.BONE_CONTROLLER
                activeDraggingId = b.id
                onSelectBone(b.id)
                return@detectDragGestures
              }
            }

            // Default to panning canvas
            dragMode = DragTargetMode.CANVAS_PAN
            activeDraggingId = null
          },
          onDrag = { change, dragAmount ->
            change.consume()
            val deltaWorld = Vec2(dragAmount.x / zoomScale, dragAmount.y / zoomScale)

            when (dragMode) {
              DragTargetMode.BONE_CONTROLLER -> {
                activeDraggingId?.let { boneId ->
                  val curPos = uiState.controllerPositions[boneId] ?: Vec2.Zero
                  onDragController(boneId, curPos + deltaWorld)
                }
              }
              DragTargetMode.LAYER_TRANSLATE -> {
                activeDraggingId?.let { layerId ->
                  val layer = uiState.layers.find { it.id == layerId }
                  if (layer != null) {
                    val nextPos = Vec2(layer.posX + deltaWorld.x, layer.posY + deltaWorld.y)
                    onMoveLayer(layerId, nextPos)
                  }
                }
              }
              DragTargetMode.LAYER_ROTATE -> {
                activeDraggingId?.let { layerId ->
                  val layer = uiState.layers.find { it.id == layerId }
                  if (layer != null) {
                    val layerPos = Vec2(layer.posX, layer.posY)
                    val toPointer = (initialDragWorld + deltaWorld) - layerPos
                    val rawAngle = toPointer.angleDegrees() + 90f
                    onRotateLayer(layerId, rawAngle % 360f)
                  }
                }
              }
              DragTargetMode.LAYER_PIVOT -> {
                activeDraggingId?.let { layerId ->
                  val layer = uiState.layers.find { it.id == layerId }
                  if (layer != null) {
                    val normDelta = Vec2(
                      deltaWorld.x / (layer.baseWidth * layer.scaleX),
                      deltaWorld.y / (layer.baseHeight * layer.scaleY)
                    )
                    onSetLayerPivot(layerId, Vec2(layer.pivotX + normDelta.x, layer.pivotY + normDelta.y))
                  }
                }
              }
              DragTargetMode.CANVAS_PAN, DragTargetMode.NONE -> {
                panOffset += dragAmount
              }
            }
          },
          onDragEnd = {
            dragMode = DragTargetMode.NONE
            activeDraggingId = null
          },
          onDragCancel = {
            dragMode = DragTargetMode.NONE
            activeDraggingId = null
          }
        )
      }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasCenter = Offset(size.width / 2f + panOffset.x, size.height / 2f + panOffset.y)

      // 1. Studio Grid & Center Axes
      drawStudioGrid(canvasCenter, zoomScale)

      // 2. Render Artwork (both bone-attached and free-floating PNG layers)
      drawArtworkLayers(
        context = context,
        uiState = uiState,
        canvasCenter = canvasCenter,
        zoom = zoomScale
      )

      // 3. Render Transform Gizmo for Selected Layer (After Effects / Duik style)
      if (uiState.selectedLayerId != null) {
        val selLayer = uiState.layers.find { it.id == uiState.selectedLayerId }
        if (selLayer != null && selLayer.visible) {
          drawLayerTransformGizmo(
            layer = selLayer,
            canvasCenter = canvasCenter,
            zoom = zoomScale
          )
        }
      }

      // 4. Duik Bones and IK Linkage
      if (uiState.showBonesOverlay && uiState.bones.isNotEmpty()) {
        drawDuikBones(
          bones = uiState.bones,
          transforms = uiState.computedTransforms,
          selectedBoneId = uiState.selectedBoneId,
          canvasCenter = canvasCenter,
          zoom = zoomScale
        )
      }

      // 5. Duik Controllers Overlay
      if (uiState.showControllersOverlay && uiState.bones.isNotEmpty()) {
        drawDuikControllers(
          bones = uiState.bones,
          transforms = uiState.computedTransforms,
          selectedBoneId = uiState.selectedBoneId,
          canvasCenter = canvasCenter,
          zoom = zoomScale
        )
      }
    }
  }
}

private fun DrawScope.drawStudioGrid(center: Offset, zoom: Float) {
  val gridSpacing = 40f * zoom
  val startX = (center.x % gridSpacing)
  val startY = (center.y % gridSpacing)

  var x = startX
  while (x < size.width) {
    drawLine(Color(0xFF1B1E28), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
    x += gridSpacing
  }

  var y = startY
  while (y < size.height) {
    drawLine(Color(0xFF1B1E28), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    y += gridSpacing
  }

  // Crosshair center axes
  drawLine(Color(0xFF282E40), Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 2f)
  drawLine(Color(0xFF282E40), Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 2f)
}

private fun DrawScope.drawArtworkLayers(
  context: android.content.Context,
  uiState: DuikUiState,
  canvasCenter: Offset,
  zoom: Float
) {
  val sortedLayers = uiState.layers.filter { it.visible }.sortedBy { it.zIndex }
  val boneMap = uiState.bones.associateBy { it.id }

  drawIntoCanvas { canvas ->
    val nativeCanvas = canvas.nativeCanvas
    val paint = Paint().apply {
      isAntiAlias = true
      isFilterBitmap = true
    }

    for (layer in sortedLayers) {
      val srcBitmap = DuikImageManager.loadSourceBitmap(context, layer) ?: continue
      val cropped = DuikImageManager.getCroppedSubBitmap(
        sourceBitmap = srcBitmap,
        cropRect = layer.cropRect,
        removeLightBackground = true
      )

      val matrix = Matrix()

      if (layer.parentBoneId != null) {
        // Layer driven by bone hierarchy
        val bone = boneMap[layer.parentBoneId]
        val trans = uiState.computedTransforms[layer.parentBoneId]
        if (trans != null && bone != null) {
          val boneScreenStart = Offset(
            canvasCenter.x + trans.startPos.x * zoom,
            canvasCenter.y + trans.startPos.y * zoom
          )
          val pivotX = cropped.width * bone.pivotRatio.x
          val pivotY = cropped.height * bone.pivotRatio.y
          matrix.postTranslate(-pivotX, -pivotY)

          val scaleX = (layer.baseWidth / cropped.width) * zoom
          val scaleY = (layer.baseHeight / cropped.height) * zoom
          matrix.postScale(scaleX, scaleY)
          matrix.postRotate(trans.angleDeg)
          matrix.postTranslate(boneScreenStart.x, boneScreenStart.y)
        }
      } else {
        // Free / Standalone PNG Layer
        val screenCenterX = canvasCenter.x + layer.posX * zoom
        val screenCenterY = canvasCenter.y + layer.posY * zoom

        val pivotPixelX = cropped.width * layer.pivotX
        val pivotPixelY = cropped.height * layer.pivotY

        matrix.postTranslate(-pivotPixelX, -pivotPixelY)

        val scaleX = (layer.baseWidth * layer.scaleX / cropped.width) * zoom
        val scaleY = (layer.baseHeight * layer.scaleY / cropped.height) * zoom
        matrix.postScale(scaleX, scaleY)
        matrix.postRotate(layer.rotationDeg)
        matrix.postTranslate(screenCenterX, screenCenterY)
      }

      paint.alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
      nativeCanvas.drawBitmap(cropped, matrix, paint)
    }
  }
}

private fun DrawScope.drawLayerTransformGizmo(
  layer: ImageLayer,
  canvasCenter: Offset,
  zoom: Float
) {
  val cx = canvasCenter.x + layer.posX * zoom
  val cy = canvasCenter.y + layer.posY * zoom
  val halfW = (layer.baseWidth * layer.scaleX * zoom) / 2f
  val halfH = (layer.baseHeight * layer.scaleY * zoom) / 2f

  val rotRad = Math.toRadians(layer.rotationDeg.toDouble())
  val cosA = cos(rotRad).toFloat()
  val sinA = sin(rotRad).toFloat()

  fun rotatePoint(px: Float, py: Float): Offset {
    val rx = px * cosA - py * sinA
    val ry = px * sinA + py * cosA
    return Offset(cx + rx, cy + ry)
  }

  val pTopLeft = rotatePoint(-halfW, -halfH)
  val pTopRight = rotatePoint(halfW, -halfH)
  val pBottomRight = rotatePoint(halfW, halfH)
  val pBottomLeft = rotatePoint(-halfW, halfH)

  // Bounding box border
  val boxPath = Path().apply {
    moveTo(pTopLeft.x, pTopLeft.y)
    lineTo(pTopRight.x, pTopRight.y)
    lineTo(pBottomRight.x, pBottomRight.y)
    lineTo(pBottomLeft.x, pBottomLeft.y)
    close()
  }
  drawPath(boxPath, Color(0xFF00E5FF), style = Stroke(width = 1.8f))

  // 4 Corner scale handles
  listOf(pTopLeft, pTopRight, pBottomRight, pBottomLeft).forEach { corner ->
    drawRect(Color(0xFF00E5FF), topLeft = Offset(corner.x - 5f, corner.y - 5f), size = Size(10f, 10f))
    drawRect(Color(0xFF101217), topLeft = Offset(corner.x - 3f, corner.y - 3f), size = Size(6f, 6f))
  }

  // Rotation Handle stem and knob at top
  val midTop = rotatePoint(0f, -halfH)
  val rotKnob = rotatePoint(0f, -halfH - 28f)
  drawLine(Color(0xFF00E5FF), midTop, rotKnob, strokeWidth = 1.8f)
  drawCircle(Color(0xFF00E5FF), radius = 6.5f, center = rotKnob)
  drawCircle(Color(0xFF101217), radius = 3.5f, center = rotKnob)

  // Pivot / Anchor point crosshair
  val pivotOffset = rotatePoint((layer.pivotX - 0.5f) * halfW * 2f, (layer.pivotY - 0.5f) * halfH * 2f)
  drawCircle(Color(0xFFFFB300), radius = 5.5f, center = pivotOffset, style = Stroke(width = 2f))
  drawLine(Color(0xFFFFB300), Offset(pivotOffset.x - 8f, pivotOffset.y), Offset(pivotOffset.x + 8f, pivotOffset.y), strokeWidth = 1.5f)
  drawLine(Color(0xFFFFB300), Offset(pivotOffset.x, pivotOffset.y - 8f), Offset(pivotOffset.x, pivotOffset.y + 8f), strokeWidth = 1.5f)
}

private fun DrawScope.drawDuikBones(
  bones: List<Bone>,
  transforms: Map<String, BoneWorldTransform>,
  selectedBoneId: String?,
  canvasCenter: Offset,
  zoom: Float
) {
  for (bone in bones) {
    if (bone.isController || !bone.visible) continue
    val trans = transforms[bone.id] ?: continue

    val p1 = Offset(canvasCenter.x + trans.startPos.x * zoom, canvasCenter.y + trans.startPos.y * zoom)
    val p2 = Offset(canvasCenter.x + trans.endPos.x * zoom, canvasCenter.y + trans.endPos.y * zoom)

    val isSelected = bone.id == selectedBoneId
    val boneColor = if (isSelected) Color(0xFFFF4081) else Color(bone.color)

    val boneLen = trans.length * zoom
    if (boneLen < 4f) continue

    val angleRad = Math.toRadians(trans.angleDeg.toDouble())
    val cosA = cos(angleRad).toFloat()
    val sinA = sin(angleRad).toFloat()

    val perpX = -sinA
    val perpY = cosA

    val headWidth = (boneLen * 0.18f).coerceIn(6f * zoom, 16f * zoom)
    val waistDist = (boneLen * 0.22f).coerceAtLeast(8f)

    val pHeadL = Offset(p1.x + cosA * waistDist + perpX * headWidth, p1.y + sinA * waistDist + perpY * headWidth)
    val pHeadR = Offset(p1.x + cosA * waistDist - perpX * headWidth, p1.y + sinA * waistDist - perpY * headWidth)

    val bonePath = Path().apply {
      moveTo(p1.x, p1.y)
      lineTo(pHeadL.x, pHeadL.y)
      lineTo(p2.x, p2.y)
      lineTo(pHeadR.x, pHeadR.y)
      close()
    }

    drawPath(bonePath, color = boneColor.copy(alpha = if (isSelected) 0.5f else 0.32f))
    drawPath(bonePath, color = boneColor, style = Stroke(width = if (isSelected) 2.5f else 1.5f))

    drawCircle(color = boneColor, radius = (4.5f * zoom).coerceIn(3.5f, 9f), center = p1)
    drawCircle(color = Color(0xFF101217), radius = (2.2f * zoom).coerceIn(1.8f, 4.5f), center = p1)
  }
}

private fun DrawScope.drawDuikControllers(
  bones: List<Bone>,
  transforms: Map<String, BoneWorldTransform>,
  selectedBoneId: String?,
  canvasCenter: Offset,
  zoom: Float
) {
  for (bone in bones) {
    if (!bone.isController) continue
    val trans = transforms[bone.id] ?: continue
    val center = Offset(canvasCenter.x + trans.startPos.x * zoom, canvasCenter.y + trans.startPos.y * zoom)
    val isSelected = bone.id == selectedBoneId

    val ctrlColor = if (isSelected) Color(0xFFFF4081) else Color(0xFF00E5FF)
    val sizePx = (18f * zoom).coerceIn(14f, 32f)

    when (bone.controllerType) {
      ControllerType.CROSSHAIR -> {
        drawCircle(ctrlColor.copy(alpha = 0.2f), radius = sizePx, center = center)
        drawCircle(ctrlColor, radius = sizePx, center = center, style = Stroke(width = 2.2f))
        val tickLen = sizePx * 0.6f
        drawLine(ctrlColor, Offset(center.x - sizePx - tickLen, center.y), Offset(center.x - sizePx + 4f, center.y), strokeWidth = 2.2f)
        drawLine(ctrlColor, Offset(center.x + sizePx - 4f, center.y), Offset(center.x + sizePx + tickLen, center.y), strokeWidth = 2.2f)
        drawLine(ctrlColor, Offset(center.x, center.y - sizePx - tickLen), Offset(center.x, center.y - sizePx + 4f), strokeWidth = 2.2f)
        drawLine(ctrlColor, Offset(center.x, center.y + sizePx - 4f), Offset(center.x, center.y + sizePx + tickLen), strokeWidth = 2.2f)
        drawCircle(ctrlColor, radius = 3.5f, center = center)
      }
      ControllerType.FOOT_ROLL -> {
        val rectW = sizePx * 2.2f
        val rectH = sizePx * 1.1f
        drawRoundRect(
          color = ctrlColor.copy(alpha = 0.25f),
          topLeft = Offset(center.x - rectW / 2f, center.y - rectH / 2f),
          size = Size(rectW, rectH),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )
        drawRoundRect(
          color = ctrlColor,
          topLeft = Offset(center.x - rectW / 2f, center.y - rectH / 2f),
          size = Size(rectW, rectH),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
          style = Stroke(width = 2.2f)
        )
        drawCircle(ctrlColor, radius = 4f, center = center)
      }
      ControllerType.BOX -> {
        val boxSize = sizePx * 1.8f
        drawRect(ctrlColor.copy(alpha = 0.2f), topLeft = Offset(center.x - boxSize / 2f, center.y - boxSize / 2f), size = Size(boxSize, boxSize))
        drawRect(ctrlColor, topLeft = Offset(center.x - boxSize / 2f, center.y - boxSize / 2f), size = Size(boxSize, boxSize), style = Stroke(width = 2.2f))
        drawCircle(ctrlColor, radius = 4f, center = center)
      }
      else -> {
        drawCircle(ctrlColor, radius = sizePx * 0.7f, center = center)
      }
    }
  }
}
