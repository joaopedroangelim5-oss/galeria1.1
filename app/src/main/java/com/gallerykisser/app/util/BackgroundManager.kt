package com.gallerykisser.app.util

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Guarda o plano de fundo customizado escolhido pelo usuário para a área da galeria.
 * A imagem escolhida é copiada para dentro do armazenamento privado do app
 * (mesma lógica das fotos importadas), assim continua funcionando mesmo se o
 * arquivo original for apagado do aparelho, e também não some da galeria pública.
 */
object BackgroundManager {
    private const val PREFS = "gk_prefs"
    private const val KEY_BG_FILE = "background_file"
    private const val BG_FILE_NAME = "gk_background.img"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun bgFile(context: Context) = File(context.filesDir, BG_FILE_NAME)

    fun hasBackground(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BG_FILE, false) && bgFile(context).exists()

    fun getBackgroundFile(context: Context): File? =
        if (hasBackground(context)) bgFile(context) else null

    fun setBackground(context: Context, sourceUri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                bgFile(context).outputStream().use { output -> input.copyTo(output) }
            }
            prefs(context).edit().putBoolean(KEY_BG_FILE, true).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearBackground(context: Context) {
        bgFile(context).delete()
        prefs(context).edit().putBoolean(KEY_BG_FILE, false).apply()
    }
}
