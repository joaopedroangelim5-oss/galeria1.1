package com.gallerykisser.app.ui

import com.gallerykisser.app.data.MediaItem
import java.text.SimpleDateFormat
import java.util.*

sealed class GalleryListItem {
    data class DateHeader(val label: String) : GalleryListItem()
    data class Entry(val media: MediaItem) : GalleryListItem()
}

object DateGrouping {
    private val dayFormat = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("pt", "BR"))

    /** Recebe a lista já ordenada por data (mais recente primeiro) e insere cabeçalhos de dia. */
    fun group(items: List<MediaItem>): List<GalleryListItem> {
        val result = mutableListOf<GalleryListItem>()
        var lastDayKey: String? = null
        val cal = Calendar.getInstance()

        for (item in items) {
            cal.timeInMillis = item.dateAdded
            val dayKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
            if (dayKey != lastDayKey) {
                result.add(GalleryListItem.DateHeader(labelFor(item.dateAdded, cal)))
                lastDayKey = dayKey
            }
            result.add(GalleryListItem.Entry(item))
        }
        return result
    }

    private fun labelFor(timeMillis: Long, cal: Calendar): String {
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(cal, today) -> "Hoje"
            isSameDay(cal, yesterday) -> "Ontem"
            else -> dayFormat.format(Date(timeMillis)).replaceFirstChar { it.uppercase() }
        }
    }

    private fun isSameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
