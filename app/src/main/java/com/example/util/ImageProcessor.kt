package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.model.GradingParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object ImageProcessor {

    suspend fun downsampleUri(context: Context, uri: Uri, maxDimension: Int = 800): Bitmap? = withContext(Dispatchers.IO) {
        var input: InputStream? = null
        try {
            input = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            var srcWidth = options.outWidth
            var srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return@withContext null

            // Calculate sample size
            var sampleSize = 1
            while (srcWidth / sampleSize > maxDimension || srcHeight / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            // Decode with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            input = context.contentResolver.openInputStream(uri)
            val sampledBitmap = BitmapFactory.decodeStream(input, null, decodeOptions)
            input?.close()

            // Resize strictly to maxDimension if still larger
            if (sampledBitmap != null) {
                val currentWidth = sampledBitmap.width
                val currentHeight = sampledBitmap.height
                if (currentWidth > maxDimension || currentHeight > maxDimension) {
                    val ratio = currentWidth.toFloat() / currentHeight.toFloat()
                    val targetWidth: Int
                    val targetHeight: Int
                    if (currentWidth > currentHeight) {
                        targetWidth = maxDimension
                        targetHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
                    } else {
                        targetHeight = maxDimension
                        targetWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
                    }
                    Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true)
                } else {
                    sampledBitmap
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try {
                input?.close()
            } catch (ignored: Exception) {}
        }
    }

    suspend fun loadOriginalUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun processBitmap(original: Bitmap, params: GradingParams): Bitmap = withContext(Dispatchers.Default) {
        val width = original.width
        val height = original.height
        val pixels = IntArray(width * height)
        original.getPixels(pixels, 0, width, 0, 0, width, height)

        val expMult = Math.pow(2.0, params.exposure.toDouble()).toFloat()
        val tintVal = params.tint
        val tempVal = params.temperature
        val brightnessVal = params.brightness
        val contrastVal = params.contrast
        val rBal = params.redBalance
        val gBal = params.greenBalance
        val bBal = params.blueBalance
        val saturationVal = params.saturation
        val vibranceVal = params.vibrance
        val gammaVal = params.gamma
        val highlightsVal = params.highlights
        val shadowsVal = params.shadows
        val hasPreset = params.selectedPreset != "None"
        val presetName = params.selectedPreset
        val pIntensity = params.presetIntensity

        for (i in pixels.indices) {
            val color = pixels[i]
            var r = ((color shr 16) and 0xFF) / 255f
            var g = ((color shr 8) and 0xFF) / 255f
            var b = (color and 0xFF) / 255f

            // 1. Preset
            if (hasPreset) {
                val presetRGB = LutExporter.applyPresetOnRGB(r, g, b, presetName)
                r = r * (1f - pIntensity) + presetRGB[0] * pIntensity
                g = g * (1f - pIntensity) + presetRGB[1] * pIntensity
                b = b * (1f - pIntensity) + presetRGB[2] * pIntensity
            }

            // 2. Exposure
            r *= expMult
            g *= expMult
            b *= expMult

            // 3. Brightness
            r += brightnessVal
            g += brightnessVal
            b += brightnessVal

            // 4. Contrast
            r = (r - 0.5f) * contrastVal + 0.5f
            g = (g - 0.5f) * contrastVal + 0.5f
            b = (b - 0.5f) * contrastVal + 0.5f

            // 5. Temp & Tint
            if (tempVal > 0) {
                r += tempVal * 0.15f
                b -= tempVal * 0.10f
            } else if (tempVal < 0) {
                b += -tempVal * 0.15f
                r -= -tempVal * 0.10f
            }
            if (tintVal > 0) {
                g += tintVal * 0.12f
            } else if (tintVal < 0) {
                r += -tintVal * 0.08f
                b += -tintVal * 0.10f
                g -= -tintVal * 0.08f
            }

            // 6. Channel Balance
            r *= rBal
            g *= gBal
            b *= bBal

            // 7. Gamma
            if (gammaVal != 1.0f) {
                r = Math.pow(r.coerceIn(0f, 1f).toDouble(), gammaVal.toDouble()).toFloat()
                g = Math.pow(g.coerceIn(0f, 1f).toDouble(), gammaVal.toDouble()).toFloat()
                b = Math.pow(b.coerceIn(0f, 1f).toDouble(), gammaVal.toDouble()).toFloat()
            }

            // 8. Highlights & Shadows
            val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
            if (highlightsVal != 1.0f) {
                val highWt = ((luma - 0.4f) / 0.6f).coerceIn(0f, 1f)
                val highSmooth = highWt * highWt * (3f - 2f * highWt)
                val highMult = 1f + (highlightsVal - 1f) * highSmooth
                r *= highMult
                g *= highMult
                b *= highMult
            }
            if (shadowsVal != 1.0f) {
                val shadowWt = 1f - (luma / 0.6f).coerceIn(0f, 1f)
                val shadowSmooth = shadowWt * shadowWt * (3f - 2f * shadowWt)
                val shadowMult = 1f + (shadowsVal - 1f) * shadowSmooth
                r *= shadowMult
                g *= shadowMult
                b *= shadowMult
            }

            // 9. Saturation & Vibrance
            val finalLuma = 0.2126f * r + 0.7152f * g + 0.0722f * b
            if (vibranceVal != 0f) {
                val maxVal = maxOf(r, g, b)
                val minVal = minOf(r, g, b)
                val satVal = if (maxVal > 1e-4f) (maxVal - minVal) / maxVal else 0f
                val vibranceAmt = vibranceVal * (1.0f - satVal)
                val vibMult = 1.0f + vibranceAmt
                r = finalLuma + (r - finalLuma) * vibMult
                g = finalLuma + (g - finalLuma) * vibMult
                b = finalLuma + (b - finalLuma) * vibMult
            }
            if (saturationVal != 1.0f) {
                r = finalLuma + (r - finalLuma) * saturationVal
                g = finalLuma + (g - finalLuma) * saturationVal
                b = finalLuma + (b - finalLuma) * saturationVal
            }

            val ri = (r.coerceIn(0f, 1f) * 255f).toInt()
            val gi = (g.coerceIn(0f, 1f) * 255f).toInt()
            val bi = (b.coerceIn(0f, 1f) * 255f).toInt()

            pixels[i] = (color and 0xFF000000.toInt()) or (ri shl 16) or (gi shl 8) or bi
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        result
    }

    suspend fun extractFramesFromVideo(context: Context, videoUri: Uri, count: Int = 4): List<Bitmap> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 5000L

            for (i in 0 until count) {
                // Space intervals evenly
                val timeUs = (durationMs * 1000L * i) / count.coerceAtLeast(1)
                // Extract frame
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    // Downsample frame for fast previewing in the workspace
                    val ratio = frame.width.toFloat() / frame.height.toFloat()
                    val targetWidth = 600
                    val targetHeight = (600 / ratio).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true)
                    list.add(scaled)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
        list
    }
}
