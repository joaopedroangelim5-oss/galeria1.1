package com.gallerykisser.app.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.gallerykisser.app.R
import com.gallerykisser.app.data.GalleryRepository
import com.gallerykisser.app.data.MediaItem

class GalleryAdapter(
    private val repository: GalleryRepository,
    private val onClick: (MediaItem, Int) -> Unit,
    private val onLongClick: (MediaItem) -> Unit,
    private val isSelected: (Long) -> Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<GalleryListItem> = emptyList()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_MEDIA = 1
    }

    fun submitList(newItems: List<GalleryListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun positionOfMedia(mediaId: Long): Int =
        items.indexOfFirst { it is GalleryListItem.Entry && it.media.id == mediaId }

    fun mediaOnly(): List<MediaItem> = items.filterIsInstance<GalleryListItem.Entry>().map { it.media }

    fun isHeader(position: Int): Boolean =
        position in items.indices && items[position] is GalleryListItem.DateHeader

    override fun getItemViewType(position: Int) = when (items[position]) {
        is GalleryListItem.DateHeader -> TYPE_HEADER
        is GalleryListItem.Entry -> TYPE_MEDIA
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_date_header, parent, false))
        } else {
            MediaVH(inflater.inflate(R.layout.item_media_thumb, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val entry = items[position]) {
            is GalleryListItem.DateHeader -> (holder as HeaderVH).bind(entry.label)
            is GalleryListItem.Entry -> (holder as MediaVH).bind(entry.media, position)
        }
    }

    inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val label: TextView = view.findViewById(R.id.tvDateHeader)
        fun bind(text: String) {
            label.text = text
        }
    }

    inner class MediaVH(view: View) : RecyclerView.ViewHolder(view) {
        private val thumb: ImageView = view.findViewById(R.id.ivThumb)
        private val videoBadge: ImageView = view.findViewById(R.id.ivVideoBadge)
        private val selectedOverlay: View = view.findViewById(R.id.selectedOverlay)

        fun bind(media: MediaItem, position: Int) {
            // Torna a célula quadrada (largura da coluna == altura), já que o
            // GridLayoutManager não faz isso sozinho fora de um ConstraintLayout.
            itemView.post {
                val params = itemView.layoutParams
                if (params.height != itemView.width && itemView.width > 0) {
                    params.height = itemView.width
                    itemView.layoutParams = params
                }
            }

            Glide.with(thumb)
                .load(repository.fileFor(media))
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
                .placeholder(R.drawable.bg_thumb_placeholder)
                .into(thumb)

            videoBadge.visibility = if (media.isVideo) View.VISIBLE else View.GONE
            val selected = isSelected(media.id)
            selectedOverlay.visibility = if (selected) View.VISIBLE else View.GONE
            itemView.setBackgroundColor(if (selected) Color.parseColor("#33F0217A") else Color.TRANSPARENT)

            itemView.setOnClickListener { onClick(media, position) }
            itemView.setOnLongClickListener { onLongClick(media); true }
        }
    }
}
