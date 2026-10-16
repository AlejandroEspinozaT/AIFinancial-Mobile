package com.aifinancial.mobile.ui.capture

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * `ACTION_IMAGE_CAPTURE`/`TakePicture` needs a pre-created file and a
 * `content://` URI for it (a plain `file://` URI is blocked by
 * `FileUriExposedException` on modern Android) — this is that one place.
 */
object CaptureFileFactory {

    fun createCaptureFile(context: Context): File {
        val dir = File(context.filesDir, "captures").apply { mkdirs() }
        return File(dir, "${UUID.randomUUID()}.jpg")
    }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
