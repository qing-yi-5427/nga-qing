package com.qingyi5427.ngaqing.ui.design

import android.app.Activity
import android.graphics.Bitmap
import androidx.core.view.drawToBitmap
import java.io.File
import java.io.FileOutputStream

/** Call on the UI thread after Compose has settled; avoids PixelCopy in Robolectric. */
internal fun saveNativeScreenshot(activity: Activity, filename: String): File {
    require(filename.endsWith(".png") && '/' !in filename && '\\' !in filename)
    val output = File("build/outputs/ui-evidence/$filename")
    output.parentFile?.mkdirs()
    FileOutputStream(output).use { stream ->
        activity.window.decorView.drawToBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
    return output
}
