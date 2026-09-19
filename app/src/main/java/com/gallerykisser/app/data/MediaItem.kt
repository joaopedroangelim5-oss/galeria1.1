package com.gallerykisser.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa um arquivo de mídia (foto ou vídeo) que foi IMPORTADO para dentro
 * do armazenamento privado do app (files/gk_media/). O arquivo original de onde
 * ele foi importado nunca é tocado — copiamos os bytes para cá, então apagar o
 * original fora do app não afeta esta cópia.
 */
@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long? = null, // null = raiz ("Todas as fotos")
    val fileName: String,       // nome do arquivo dentro de files/gk_media/
    val displayName: String,    // nome original, só para exibição
    val isVideo: Boolean,
    val dateAdded: Long,        // usado para agrupar por data e ordenar
    val sizeBytes: Long = 0,
    val isSecure: Boolean = false // true = só aparece no Modo Seguro
)
