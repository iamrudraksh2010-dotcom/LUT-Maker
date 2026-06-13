package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.GradingParams
import java.io.File
import java.io.FileOutputStream

object LutExporter {

    fun generateCubeString(size: Int, params: GradingParams): String {
        val builder = StringBuilder()
        builder.append("# Created with LUT Maker on Android\n")
        builder.append("# Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n")
        builder.append("LUT_3D_SIZE $size\n\n")

        val sizeMinusOne = (size - 1).toFloat()
        // Outermost: Blue, Middle: Green, Innermost: Red
        for (b in 0 until size) {
            val bIn = if (size > 1) b / sizeMinusOne else 0f
            for (g in 0 until size) {
                val gIn = if (size > 1) g / sizeMinusOne else 0f
                for (r in 0 until size) {
                    val rIn = if (size > 1) r / sizeMinusOne else 0f
                    
                    val output = applyGradingOnRGB(rIn, gIn, bIn, params)
                    builder.append(String.format(java.util.Locale.US, "%.6f %.6f %.6f\n", output[0], output[1], output[2]))
                }
            }
        }
        return builder.toString()
    }

    fun applyGradingOnRGB(rIn: Float, gIn: Float, bIn: Float, params: GradingParams): FloatArray {
        var r = rIn
        var g = gIn
        var b = bIn

        // 1. Preset filter
        if (params.selectedPreset != "None") {
            val presetRGB = applyPresetOnRGB(r, g, b, params.selectedPreset)
            val t = params.presetIntensity
            r = r * (1f - t) + presetRGB[0] * t
            g = g * (1f - t) + presetRGB[1] * t
            b = b * (1f - t) + presetRGB[2] * t
        }

        // 2. Exposure: expMult = 2^exposure
        val expMult = Math.pow(2.0, params.exposure.toDouble()).toFloat()
        r *= expMult
        g *= expMult
        b *= expMult

        // 3. Brightness
        r += params.brightness
        g += params.brightness
        b += params.brightness

        // 4. Contrast
        r = (r - 0.5f) * params.contrast + 0.5f
        g = (g - 0.5f) * params.contrast + 0.5f
        b = (b - 0.5f) * params.contrast + 0.5f

        // 5. Temp & Tint
        if (params.temperature > 0) {
            r += params.temperature * 0.15f
            b -= params.temperature * 0.1f
        } else if (params.temperature < 0) {
            b += -params.temperature * 0.15f
            r -= -params.temperature * 0.1f
        }
        if (params.tint > 0) {
            g += params.tint * 0.12f
        } else if (params.tint < 0) {
            r += -params.tint * 0.08f
            b += -params.tint * 0.1f
            g -= -params.tint * 0.08f
        }

        // 6. Channel Balance
        r *= params.redBalance
        g *= params.greenBalance
        b *= params.blueBalance

        // 7. Gamma (v^gamma)
        if (params.gamma != 1.0f) {
            r = Math.pow(r.coerceIn(0f, 1f).toDouble(), params.gamma.toDouble()).toFloat()
            g = Math.pow(g.coerceIn(0f, 1f).toDouble(), params.gamma.toDouble()).toFloat()
            b = Math.pow(b.coerceIn(0f, 1f).toDouble(), params.gamma.toDouble()).toFloat()
        }

        // 8. Highlights & Shadows
        val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
        if (params.highlights != 1.0f) {
            val highWt = ((luma - 0.4f) / 0.6f).coerceIn(0f, 1f)
            val highSmooth = highWt * highWt * (3f - 2f * highWt)
            val highMult = 1f + (params.highlights - 1f) * highSmooth
            r *= highMult
            g *= highMult
            b *= highMult
        }
        if (params.shadows != 1.0f) {
            val shadowWt = 1f - (luma / 0.6f).coerceIn(0f, 1f)
            val shadowSmooth = shadowWt * shadowWt * (3f - 2f * shadowWt)
            val shadowMult = 1f + (params.shadows - 1f) * shadowSmooth
            r *= shadowMult
            g *= shadowMult
            b *= shadowMult
        }

        // 9. Saturation & Vibrance
        val finalLuma = 0.2126f * r + 0.7152f * g + 0.0722f * b
        if (params.vibrance != 0f) {
            val maxVal = maxOf(r, g, b)
            val minVal = minOf(r, g, b)
            val satVal = if (maxVal > 1e-4f) (maxVal - minVal) / maxVal else 0f
            val vibranceAmt = params.vibrance * (1.0f - satVal)
            val vibMult = 1.0f + vibranceAmt
            r = finalLuma + (r - finalLuma) * vibMult
            g = finalLuma + (g - finalLuma) * vibMult
            b = finalLuma + (b - finalLuma) * vibMult
        }
        if (params.saturation != 1.0f) {
            r = finalLuma + (r - finalLuma) * params.saturation
            g = finalLuma + (g - finalLuma) * params.saturation
            b = finalLuma + (b - finalLuma) * params.saturation
        }

        return floatArrayOf(
            r.coerceIn(0f, 1f),
            g.coerceIn(0f, 1f),
            b.coerceIn(0f, 1f)
        )
    }

    fun applyPresetOnRGB(r: Float, g: Float, b: Float, preset: String): FloatArray {
        var pr = r
        var pg = g
        var pb = b
        when (preset) {
            "Teal & Orange" -> {
                val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                if (luma > 0.5f) {
                    val t = (luma - 0.5f) * 2f
                    pr = pr * (1f - t * 0.15f) + t * 0.15f * 1.0f
                    pg = pg * (1f - t * 0.10f) + t * 0.14f * 0.75f
                    pb = pb * (1f - t * 0.20f) + t * 0.20f * 0.40f
                } else {
                    val t = (0.5f - luma) * 2f
                    pr = pr * (1f - t * 0.20f) + t * 0.10f * 0.20f
                    pg = pg * (1f - t * 0.05f) + t * 0.12f * 0.60f
                    pb = pb * (1f - t * 0.15f) + t * 0.18f * 0.70f
                }
            }
            "Vintage" -> {
                pr = pr * 0.85f + 0.10f
                pg = pg * 0.85f + 0.08f
                pb = pb * 0.75f + 0.05f
            }
            "Cyberpunk" -> {
                val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                if (luma > 0.4f) {
                    pr = pr * 0.90f + 0.25f
                    pg = pg * 0.60f
                    pb = pb * 0.90f + 0.20f
                } else {
                    pr = pr * 0.40f
                    pg = pg * 0.90f + 0.15f
                    pb = pb * 0.90f + 0.25f
                }
            }
            "Cinematic" -> {
                val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                pr = r * 1.05f
                pg = g * 0.98f
                pb = b * 0.92f
                if (luma < 0.4f) {
                    val t = (0.4f - luma) / 0.4f
                    pr -= t * 0.04f
                    pg += t * 0.02f
                    pb += t * 0.06f
                }
            }
            "Noir" -> {
                val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                val contrastLuma = ((luma - 0.5f) * 1.4f + 0.5f).coerceIn(0f, 1f)
                return floatArrayOf(contrastLuma, contrastLuma, contrastLuma)
            }
            "B&W" -> {
                val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                return floatArrayOf(luma, luma, luma)
            }
            "Warm Golden" -> {
                pr = r * 1.15f + 0.04f
                pg = g * 1.05f + 0.02f
                pb = b * 0.85f
            }
            "Cold Breeze" -> {
                pr = r * 0.85f
                pg = g * 1.02f + 0.03f
                pb = b * 1.18f + 0.05f
            }
            "Forest Green" -> {
                pr = r * 0.88f
                pg = g * 1.08f
                pb = b * 0.85f
            }
            "Acid Sunset" -> {
                val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
                pr = r * 1.30f
                pg = g * (1f - luma) * 1.10f
                pb = b * luma * 1.30f
            }
        }
        return floatArrayOf(pr, pg, pb)
    }

    fun exportCubeToDownloads(context: Context, filename: String, size: Int, params: GradingParams): Uri? {
        val cubeString = generateCubeString(size, params)
        val finalFilename = if (filename.endsWith(".cube")) filename else "$filename.cube"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalFilename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(cubeString.toByteArray())
                    }
                    uri
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else {
                null
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, finalFilename)
            try {
                FileOutputStream(file).use { fos ->
                    fos.write(cubeString.toByteArray())
                }
                Uri.fromFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun saveImageToGallery(context: Context, bitmap: Bitmap, filename: String): Uri? {
        val finalFilename = if (filename.endsWith(".jpg") || filename.endsWith(".png")) filename else "$filename.png"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalFilename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LUTMaker")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                try {
                    resolver.openOutputStream(uri)?.use { os ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                    }
                    uri
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else {
                null
            }
        } else {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "LUTMaker").apply { mkdirs() }
            val file = File(appDir, finalFilename)
            try {
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                Uri.fromFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
