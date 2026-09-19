package com.gallerykisser.app.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.util.UUID

/**
 * Todo o conteúdo importado vive em context.filesDir/gk_media/.
 * Esse diretório é privado do app: não aparece no Google Fotos, no app de
 * Arquivos, nem para outros apps — só é acessível através do Gallery Kisser
 * (ou de um root explorer, como qualquer dado privado de app).
 */
class MediaStorage(private val context: Context) {

    private val root: File by lazy {
        File(context.filesDir, "gk_media").apply { if (!exists()) mkdirs() }
    }

    private val thumbRoot: File by lazy {
        File(context.cacheDir, "gk_thumbs").apply { if (!exists()) mkdirs() }
    }

    fun fileFor(fileName: String): File = File(root, fileName)

    fun thumbFileFor(fileName: String): File = File(thumbRoot, "$fileName.jpg")

    /** Copia o conteúdo de uma Uri (escolhida pelo seletor do sistema) para o armazenamento privado. */
    fun importFrom(sourceUri: Uri, isVideo: Boolean): String {
        val ext = if (isVideo) "mp4" else "jpg"
        val fileName = "${UUID.randomUUID()}.$ext"
        val dest = fileFor(fileName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        return fileName
    }

    fun deleteFile(fileName: String) {
        fileFor(fileName).delete()
        thumbFileFor(fileName).delete()
    }

    /** Exporta (salva) um arquivo do app de volta para a galeria pública do aparelho — funciona sem internet. */
    fun exportToPublicStorage(fileName: String, displayName: String, isVideo: Boolean): Boolean {
        val src = fileFor(fileName)
        if (!src.exists()) return false

        val mime = if (isVideo) "video/mp4" else "image/jpeg"
        val collection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val relativeDir = if (isVideo) "Movies/GalleryKisser" else "Pictures/GalleryKisser"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(collection, values) ?: return false
        context.contentResolver.openOutputStream(uri)?.use { out ->
            src.inputStream().use { it.copyTo(out) }
        } ?: return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        }
        return true
    }
}
