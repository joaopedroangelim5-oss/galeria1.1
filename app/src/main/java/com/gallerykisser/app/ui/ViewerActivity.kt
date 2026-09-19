package com.gallerykisser.app.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import com.gallerykisser.app.R
import com.gallerykisser.app.data.GalleryRepository
import com.gallerykisser.app.data.MediaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MEDIA_IDS = "extra_media_ids"
        const val EXTRA_START_INDEX = "extra_start_index"
    }

    private lateinit var repository: GalleryRepository
    private lateinit var ivFull: ZoomableImageView
    private lateinit var videoFull: android.widget.VideoView
    private var ids: LongArray = LongArray(0)
    private var index: Int = 0
    private var currentItem: MediaItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        repository = GalleryRepository(applicationContext)
        ivFull = findViewById(R.id.ivFull)
        videoFull = findViewById(R.id.videoFull)

        ids = intent.getLongArrayExtra(EXTRA_MEDIA_IDS) ?: LongArray(0)
        index = intent.getIntExtra(EXTRA_START_INDEX, 0)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnPrev).setOnClickListener { move(-1) }
        findViewById<View>(R.id.btnNext).setOnClickListener { move(1) }
        findViewById<View>(R.id.btnInfo).setOnClickListener { showInfoDialog() }
        findViewById<View>(R.id.btnShare).setOnClickListener { shareCurrent() }
        findViewById<View>(R.id.btnEdit).setOnClickListener { editCurrent() }
    }

    private fun move(delta: Int) {
        val newIndex = index + delta
        if (newIndex in ids.indices) {
            index = newIndex
            showCurrent()
        }
    }

    private fun showCurrent() {
        videoFull.stopPlayback()
        ivFull.resetZoom()
        val mediaId = ids.getOrNull(index) ?: return
        lifecycleScope.launch {
            val item = repository.getById(mediaId) ?: return@launch
            currentItem = item
            val file = repository.fileFor(item)

            findViewById<View>(R.id.btnEdit).visibility = if (item.isVideo) View.GONE else View.VISIBLE

            if (item.isVideo) {
                ivFull.visibility = View.GONE
                videoFull.visibility = View.VISIBLE
                videoFull.setVideoURI(Uri.fromFile(file))
                videoFull.setOnPreparedListener { it.isLooping = true }
                videoFull.start()
            } else {
                videoFull.visibility = View.GONE
                ivFull.visibility = View.VISIBLE
                Glide.with(ivFull)
                    .load(file)
                    .signature(com.bumptech.glide.signature.ObjectKey(file.lastModified()))
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .into(ivFull)
            }
        }
    }

    // Recarrega a mídia atual sempre que a tela volta ao primeiro plano — cobre tanto a
    // abertura inicial quanto o retorno da tela de edição (garante que a versão editada apareça).
    override fun onResume() {
        super.onResume()
        showCurrent()
    }

    override fun onPause() {
        super.onPause()
        if (videoFull.isPlaying) videoFull.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoFull.stopPlayback()
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }

    private fun showInfoDialog() {
        val item = currentItem ?: return
        val file = repository.fileFor(item)
        val sizeStr = formatSize(file.length())
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(item.dateAdded))
        val dimsStr = if (!item.isVideo) {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            if (opts.outWidth > 0) "${opts.outWidth} × ${opts.outHeight} px" else "—"
        } else {
            "—"
        }
        val message = getString(R.string.info_template, item.displayName, sizeStr, dimsStr, dateStr)

        AlertDialog.Builder(this)
            .setTitle(R.string.info_title)
            .setMessage(message)
            .setPositiveButton(R.string.ok, null)
            .setNeutralButton(R.string.rename) { _, _ -> showRenameDialog(item) }
            .show()
    }

    private fun showRenameDialog(item: MediaItem) {
        val input = EditText(this).apply { setText(item.displayName) }
        AlertDialog.Builder(this)
            .setTitle(R.string.rename)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        repository.renameDisplayName(item.id, newName)
                        currentItem = item.copy(displayName = newName)
                        Toast.makeText(this@ViewerActivity, R.string.rename_done, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun shareCurrent() {
        val item = currentItem ?: return
        val file = repository.fileFor(item)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val mime = if (item.isVideo) "video/mp4" else "image/jpeg"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(sendIntent, getString(R.string.share_via)))
    }

    private fun editCurrent() {
        val item = currentItem ?: return
        if (item.isVideo) return
        startActivity(Intent(this, EditPhotoActivity::class.java).putExtra(EditPhotoActivity.EXTRA_MEDIA_ID, item.id))
    }
}
