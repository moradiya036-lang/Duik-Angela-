package com.example.duik.slice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import com.example.duik.model.CropRect
import com.example.duik.model.ImageLayer
import com.example.duik.model.LayerSourceType
import java.io.InputStream

object DuikImageManager {

  private val bitmapCache = mutableMapOf<String, Bitmap>()

  fun loadSourceBitmap(context: Context, layer: ImageLayer): Bitmap? {
    val key = when (layer.sourceType) {
      LayerSourceType.BUILTIN_SPRITE_SHEET -> "res_manga_sheet"
      LayerSourceType.BUILTIN_NINJA -> "res_ninja_art"
      LayerSourceType.CUSTOM_URI -> layer.customUriString ?: "custom_null"
      LayerSourceType.SOLID_COLOR -> "solid_color"
    }

    if (bitmapCache.containsKey(key)) {
      return bitmapCache[key]
    }

    val loaded: Bitmap? = when (layer.sourceType) {
      LayerSourceType.BUILTIN_SPRITE_SHEET -> {
        loadResourceByName(context, "manga_character_sheet_1791791702343")
      }
      LayerSourceType.BUILTIN_NINJA -> {
        loadResourceByName(context, "manga_ninja_art_1791791725606")
      }
      LayerSourceType.CUSTOM_URI -> {
        layer.customUriString?.let { uriStr ->
          try {
            val uri = Uri.parse(uriStr)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { BitmapFactory.decodeStream(it) }
          } catch (e: Exception) {
            null
          }
        }
      }
      LayerSourceType.SOLID_COLOR -> null
    }

    if (loaded != null) {
      bitmapCache[key] = loaded
    }
    return loaded
  }

  fun decodeAndCacheUri(context: Context, uriStr: String): Bitmap? {
    if (bitmapCache.containsKey(uriStr)) {
      return bitmapCache[uriStr]
    }
    return try {
      val uri = Uri.parse(uriStr)
      val inputStream = context.contentResolver.openInputStream(uri)
      val bmp = inputStream?.use { BitmapFactory.decodeStream(it) }
      if (bmp != null) {
        bitmapCache[uriStr] = bmp
      }
      bmp
    } catch (e: Exception) {
      null
    }
  }

  private fun loadResourceByName(context: Context, resName: String): Bitmap? {
    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
    return if (resId != 0) {
      BitmapFactory.decodeResource(context.resources, resId)
    } else {
      null
    }
  }

  /**
   * Slices a sub-region of a bitmap based on normalized CropRect (0..1)
   * and optionally makes near-white / plain background transparent.
   */
  fun getCroppedSubBitmap(
    sourceBitmap: Bitmap,
    cropRect: CropRect?,
    removeLightBackground: Boolean = false
  ): Bitmap {
    if (cropRect == null) {
      return if (removeLightBackground) removeBackground(sourceBitmap) else sourceBitmap
    }

    val srcW = sourceBitmap.width
    val srcH = sourceBitmap.height

    val x = (cropRect.left * srcW).toInt().coerceIn(0, srcW - 1)
    val y = (cropRect.top * srcH).toInt().coerceIn(0, srcH - 1)
    val w = ((cropRect.right - cropRect.left) * srcW).toInt().coerceIn(1, srcW - x)
    val h = ((cropRect.bottom - cropRect.top) * srcH).toInt().coerceIn(1, srcH - y)

    val cropped = Bitmap.createBitmap(sourceBitmap, x, y, w, h)
    return if (removeLightBackground) removeBackground(cropped) else cropped
  }

  /**
   * Color-keying / chroma-keying helper for manga art on white/light backgrounds.
   */
  fun removeBackground(src: Bitmap, threshold: Int = 238): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val width = out.width
    val height = out.height
    val pixels = IntArray(width * height)
    out.getPixels(pixels, 0, width, 0, 0, width, height)

    for (i in pixels.indices) {
      val c = pixels[i]
      val r = AndroidColor.red(c)
      val g = AndroidColor.green(c)
      val b = AndroidColor.blue(c)
      if (r >= threshold && g >= threshold && b >= threshold) {
        pixels[i] = AndroidColor.TRANSPARENT
      }
    }
    out.setPixels(pixels, 0, width, 0, 0, width, height)
    return out
  }

  fun registerCustomBitmap(key: String, bitmap: Bitmap) {
    bitmapCache[key] = bitmap
  }
}
