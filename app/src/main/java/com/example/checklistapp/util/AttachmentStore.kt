package com.example.checklistapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object AttachmentStore {

    private fun attachmentsDir(context: Context) =
        File(context.filesDir, "attachments").apply { mkdirs() }

    private fun stagingDir(context: Context) =
        File(context.filesDir, "staging").apply { mkdirs() }

    fun stageFromUri(context: Context, uri: Uri, displayName: String): String? {
        val safe = displayName.replace(Regex("[^A-Za-z0-9._ ()-]"), "_").take(80)
        val target = File(stagingDir(context), "${UUID.randomUUID()}_$safe")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            } ?: return null
            "staging/${target.name}"
        } catch (e: Exception) {
            target.delete(); null
        }
    }

    fun commitStaged(context: Context, stagedRelPath: String, itemId: Long): String {
        val staged = File(context.filesDir, stagedRelPath)
        val finalDir = File(attachmentsDir(context), "item_$itemId").apply { mkdirs() }
        val final = File(finalDir, staged.name)
        if (!staged.renameTo(final)) {
            staged.copyTo(final, overwrite = true); staged.delete()
        }
        return "attachments/item_$itemId/${final.name}"
    }

    fun deleteByRelativePath(context: Context, relPath: String) {
        if (relPath.isBlank()) return
        File(context.filesDir, relPath).delete()
    }

    fun cleanupStaging(context: Context) {
        stagingDir(context).listFiles()?.forEach { it.delete() }
    }

    fun openIntent(context: Context, relPath: String, mimeType: String): Intent {
        val file = File(context.filesDir, relPath)
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun duplicateFile(context: Context, relPath: String): String? {
        val src = File(context.filesDir, relPath)
        if (!src.exists()) return null
        val dst = File(
            src.parentFile,
            "${UUID.randomUUID()}_${src.nameWithoutExtension}.${src.extension}"
        )
        return if (src.copyTo(dst, overwrite = true).exists())
            dst.relativeTo(context.filesDir).path else null
    }
}
