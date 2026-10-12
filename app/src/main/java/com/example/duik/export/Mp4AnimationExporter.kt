package com.example.duik.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.duik.engine.DuikRigEngine
import com.example.duik.engine.LocalPose
import com.example.duik.math.DuikAutomations
import com.example.duik.math.DuikEasing
import com.example.duik.math.Vec2
import com.example.duik.model.AnimationTrack
import com.example.duik.model.ImageLayer
import com.example.duik.model.PropertyType
import com.example.duik.slice.DuikImageManager
import com.example.duik.viewmodel.DuikUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

data class ExportSettings(
  val width: Int = 720,
  val height: Int = 720,
  val loopCount: Int = 2,
  val includeBones: Boolean = false,
  val backgroundColor: Int = 0xFF101217.toInt()
)

data class ExportResult(
  val success: Boolean,
  val uri: Uri? = null,
  val filePath: String? = null,
  val durationSeconds: Float = 0f,
  val errorMessage: String? = null
)

object Mp4AnimationExporter {

  suspend fun exportToGalleryMp4(
    context: Context,
    uiState: DuikUiState,
    settings: ExportSettings = ExportSettings(),
    onProgress: (Float, String) -> Unit
  ): ExportResult = withContext(Dispatchers.IO) {
    val tempFile = File(context.cacheDir, "render_${System.currentTimeMillis()}.mp4")

    try {
      val totalFramesInClip = uiState.clip.totalFrames.coerceAtLeast(1)
      val fps = uiState.clip.fps.coerceIn(12, 60)
      val totalRenderFrames = totalFramesInClip * settings.loopCount

      onProgress(0.05f, "Initializing video encoder...")

      val width = settings.width
      val height = settings.height
      val bitrate = 2_500_000 // 2.5 Mbps

      val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
        setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        setInteger(MediaFormat.KEY_FRAME_RATE, fps)
        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
      }

      val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
      encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
      encoder.start()

      val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
      var trackIndex = -1
      var muxerStarted = false

      val bufferInfo = MediaCodec.BufferInfo()
      val yuvBuffer = ByteArray(width * height * 3 / 2)
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)
      val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
      }

      val frameDurationUs = (1_000_000L / fps)

      for (renderFrameIdx in 0 until totalRenderFrames) {
        val animFrame = renderFrameIdx % totalFramesInClip
        val progress = 0.1f + (renderFrameIdx.toFloat() / totalRenderFrames.toFloat()) * 0.8f
        onProgress(progress, "Rendering frame ${renderFrameIdx + 1} of $totalRenderFrames...")

        // 1. Evaluate transforms for animFrame
        val evaluatedState = evaluateStateAtFrame(uiState, animFrame)

        // 2. Render frame onto Bitmap
        renderFrameToBitmap(
          context = context,
          canvas = canvas,
          width = width,
          height = height,
          state = evaluatedState,
          settings = settings,
          paint = paint
        )

        // 3. Convert Bitmap ARGB to YUV420SemiPlanar (NV12)
        encodeYUV420SP(yuvBuffer, bitmap, width, height)

        // 4. Feed input buffer to MediaCodec
        val inputBufferIndex = encoder.dequeueInputBuffer(100_000L)
        if (inputBufferIndex >= 0) {
          val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
          inputBuffer?.clear()
          inputBuffer?.put(yuvBuffer)
          val pts = renderFrameIdx * frameDurationUs
          encoder.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, pts, 0)
        }

        // 5. Drain output buffers to Muxer
        trackIndex = drainEncoder(encoder, muxer, bufferInfo, trackIndex, false) {
          muxerStarted = true
        }
      }

      // Signal End of Stream (EOS)
      onProgress(0.92f, "Finalizing MP4 video stream...")
      val eosBufferIndex = encoder.dequeueInputBuffer(100_000L)
      if (eosBufferIndex >= 0) {
        val pts = totalRenderFrames * frameDurationUs
        encoder.queueInputBuffer(eosBufferIndex, 0, 0, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
      }

      // Drain remaining until EOS
      drainEncoder(encoder, muxer, bufferInfo, trackIndex, true) {
        muxerStarted = true
      }

      encoder.stop()
      encoder.release()

      if (muxerStarted) {
        try {
          muxer.stop()
        } catch (e: Exception) {
          // ignore potential stop issue if no samples
        }
      }
      muxer.release()

      onProgress(0.96f, "Saving video to Android Gallery...")

      // 6. Save MP4 to Gallery using MediaStore
      val savedUri = saveVideoToGallery(context, tempFile, uiState.projectName)

      onProgress(1.0f, "Saved to Gallery!")

      val durationSec = totalRenderFrames.toFloat() / fps.toFloat()
      ExportResult(
        success = true,
        uri = savedUri,
        filePath = tempFile.absolutePath,
        durationSeconds = durationSec
      )
    } catch (e: Exception) {
      e.printStackTrace()
      ExportResult(
        success = false,
        errorMessage = e.message ?: "Failed to encode MP4 video"
      )
    }
  }

  private fun drainEncoder(
    encoder: MediaCodec,
    muxer: MediaMuxer,
    bufferInfo: MediaCodec.BufferInfo,
    curTrackIndex: Int,
    endOfStream: Boolean,
    onMuxerStart: () -> Unit
  ): Int {
    var track = curTrackIndex
    var muxerReady = track >= 0

    while (true) {
      val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 20_000L)
      if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
        if (!endOfStream) break
      } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        if (!muxerReady) {
          val newFormat = encoder.outputFormat
          track = muxer.addTrack(newFormat)
          muxer.start()
          muxerReady = true
          onMuxerStart()
        }
      } else if (outIndex >= 0) {
        val encodedData = encoder.getOutputBuffer(outIndex)
        if (encodedData != null && muxerReady) {
          if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && bufferInfo.size != 0) {
            encodedData.position(bufferInfo.offset)
            encodedData.limit(bufferInfo.offset + bufferInfo.size)
            muxer.writeSampleData(track, encodedData, bufferInfo)
          }
        }
        encoder.releaseOutputBuffer(outIndex, false)
        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
          break
        }
      }
    }
    return track
  }

  private fun renderFrameToBitmap(
    context: Context,
    canvas: Canvas,
    width: Int,
    height: Int,
    state: DuikUiState,
    settings: ExportSettings,
    paint: Paint
  ) {
    // 1. Fill background
    canvas.drawColor(settings.backgroundColor)

    val canvasCenterX = width / 2f
    val canvasCenterY = height / 2f

    // 2. Draw Artwork Layers
    val sortedLayers = state.layers.filter { it.visible }.sortedBy { it.zIndex }
    val boneMap = state.bones.associateBy { it.id }

    for (layer in sortedLayers) {
      val srcBitmap = DuikImageManager.loadSourceBitmap(context, layer) ?: continue
      val cropped = DuikImageManager.getCroppedSubBitmap(
        sourceBitmap = srcBitmap,
        cropRect = layer.cropRect,
        removeLightBackground = true
      )

      val matrix = Matrix()

      if (layer.parentBoneId != null) {
        val bone = boneMap[layer.parentBoneId]
        val trans = state.computedTransforms[layer.parentBoneId]
        if (trans != null && bone != null) {
          val boneStart = Vec2(canvasCenterX + trans.startPos.x, canvasCenterY + trans.startPos.y)
          val pivotX = cropped.width * bone.pivotRatio.x
          val pivotY = cropped.height * bone.pivotRatio.y
          matrix.postTranslate(-pivotX, -pivotY)

          val scaleX = (layer.baseWidth / cropped.width)
          val scaleY = (layer.baseHeight / cropped.height)
          matrix.postScale(scaleX, scaleY)
          matrix.postRotate(trans.angleDeg)
          matrix.postTranslate(boneStart.x, boneStart.y)
        }
      } else {
        val screenCenterX = canvasCenterX + layer.posX
        val screenCenterY = canvasCenterY + layer.posY

        val pivotPixelX = cropped.width * layer.pivotX
        val pivotPixelY = cropped.height * layer.pivotY

        matrix.postTranslate(-pivotPixelX, -pivotPixelY)

        val scaleX = (layer.baseWidth * layer.scaleX / cropped.width)
        val scaleY = (layer.baseHeight * layer.scaleY / cropped.height)
        matrix.postScale(scaleX, scaleY)
        matrix.postRotate(layer.rotationDeg)
        matrix.postTranslate(screenCenterX, screenCenterY)
      }

      paint.alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
      canvas.drawBitmap(cropped, matrix, paint)
    }
  }

  private fun evaluateStateAtFrame(state: DuikUiState, frame: Int): DuikUiState {
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

    val timeSec = frame.toFloat() / state.clip.fps.toFloat()
    val walkOffsets = if (state.walkCycleSettings.enabled) {
      DuikAutomations.computeWalkCycle(
        cycleTimeSec = timeSec,
        baseLeftFootRest = newControllers["ctrl_foot_l"] ?: Vec2(45f, 160f),
        baseRightFootRest = newControllers["ctrl_foot_r"] ?: Vec2(-45f, 160f),
        baseLeftHandRest = newControllers["ctrl_hand_l"] ?: Vec2(110f, -100f),
        baseRightHandRest = newControllers["ctrl_hand_r"] ?: Vec2(-105f, -85f),
        settings = state.walkCycleSettings
      )
    } else null

    val transforms = DuikRigEngine.evaluateSkeleton(
      bones = state.bones,
      ikConstraints = state.ikConstraints,
      localPose = LocalPose(
        localTranslations = emptyMap(),
        localRotations = newRotations,
        controllerPositions = newControllers
      ),
      walkOffsets = walkOffsets,
      timeSec = timeSec,
      wiggleEnabled = state.wiggleEnabled
    )

    return state.copy(
      currentFrame = frame,
      controllerPositions = newControllers,
      localRotations = newRotations,
      layers = updatedLayers,
      computedTransforms = transforms
    )
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

  /**
   * Fast conversion from Bitmap ARGB to YUV420SemiPlanar (NV12).
   */
  private fun encodeYUV420SP(yuv420sp: ByteArray, bitmap: Bitmap, width: Int, height: Int) {
    val frameSize = width * height
    val argb = IntArray(frameSize)
    bitmap.getPixels(argb, 0, width, 0, 0, width, height)

    var yIndex = 0
    var uvIndex = frameSize

    for (j in 0 until height) {
      for (i in 0 until width) {
        val c = argb[j * width + i]
        val r = (c shr 16) and 0xff
        val g = (c shr 8) and 0xff
        val b = c and 0xff

        // Y component
        var y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
        if (y < 16) y = 16
        if (y > 235) y = 235
        yuv420sp[yIndex++] = y.toByte()

        // U and V components downsampled 2x2 (NV12: U then V)
        if (j % 2 == 0 && i % 2 == 0) {
          var u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
          var v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
          if (u < 16) u = 16
          if (u > 240) u = 240
          if (v < 16) v = 16
          if (v > 240) v = 240
          yuv420sp[uvIndex++] = u.toByte()
          yuv420sp[uvIndex++] = v.toByte()
        }
      }
    }
  }

  /**
   * Inserts the rendered MP4 file into Android MediaStore Video Media (Gallery / Movies).
   */
  private fun saveVideoToGallery(context: Context, sourceFile: File, projectName: String): Uri? {
    val cleanName = projectName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
    val fileName = "Duik_${cleanName}_${System.currentTimeMillis()}.mp4"

    val contentValues = ContentValues().apply {
      put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
      put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
      put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
      put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/DuikAngela")
        put(MediaStore.Video.Media.IS_PENDING, 1)
      }
    }

    val resolver = context.contentResolver
    val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

    if (videoUri != null) {
      resolver.openOutputStream(videoUri)?.use { out ->
        FileInputStream(sourceFile).use { input ->
          input.copyTo(out)
        }
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
        resolver.update(videoUri, contentValues, null, null)
      }

      // Notify Android media scanner so it shows up in Gallery / Photos instantly
      MediaScannerConnection.scanFile(
        context,
        arrayOf(sourceFile.absolutePath),
        arrayOf("video/mp4")
      ) { _, _ -> }
    }

    return videoUri
  }

  fun shareVideo(context: Context, videoUri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "video/mp4"
      putExtra(Intent.EXTRA_STREAM, videoUri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Animation Video"))
  }

  fun viewVideoInGallery(context: Context, videoUri: Uri) {
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(videoUri, "video/mp4")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
      context.startActivity(viewIntent)
    } catch (e: Exception) {
      // Fallback to share if no default video player
      shareVideo(context, videoUri)
    }
  }
}
