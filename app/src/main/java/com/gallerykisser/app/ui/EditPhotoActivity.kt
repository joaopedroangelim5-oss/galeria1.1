package com.gallerykisser.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.gallerykisser.app.R
import com.gallerykisser.app.data.GalleryRepository
import com.gallerykisser.app.data.MediaItem

/**
 * Edição simples direto no arquivo já importado: girar 90°, cortar e carimbar um texto.
 * Ao salvar, sobrescreve o mesmo arquivo (o nome/local não muda, só o conteúdo).
 */
class EditPhotoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MEDIA_ID = "extra_media_id"
    }

    private lateinit var repository: GalleryRepository
    private lateinit var ivEdit: android.widget.ImageView
    private lateinit var cropOverlay: CropOverlayView
    private lateinit var btnConfirmCrop: View

    private var bitmap: Bitmap? = null
    private var item: MediaItem? = null
    private var cropMode = false
    private var edited = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        repository = GalleryRepository(applicationContext)
        ivEdit = findViewById(R.id.ivEdit)
        cropOverlay = findViewById(R.id.cropOverlay)
        btnConfirmCrop = findViewById(R.id.btnConfirmCrop)

        val toolbar = findViewById<Toolbar>(R.id.toolbarEdit)
        toolbar.setNavigationOnClickListener { confirmExitIfNeeded() }
        toolbar.inflateMenu(R.menu.menu_edit)
        toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.actionSaveEdit) {
                saveAndFinish()
                true
            } else {
                false
            }
        }

        findViewById<View>(R.id.btnRotate).setOnClickListener { rotateBitmap() }
        findViewById<View>(R.id.btnCrop).setOnClickListener { toggleCropMode() }
        findViewById<View>(R.id.btnText).setOnClickListener { showTextDialog() }
        btnConfirmCrop.setOnClickListener { applyCrop() }

        val mediaId = intent.getLongExtra(EXTRA_MEDIA_ID, -1L)
        lifecycleScope.launch {
            val loadedItem = repository.getById(mediaId)
            if (loadedItem == null) {
                finish()
                return@launch
            }
            item = loadedItem
            val file = repository.fileFor(loadedItem)
            bitmap = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.absolutePath) }
            ivEdit.setImageBitmap(bitmap)
        }
    }

    override fun onBackPressed() {
        confirmExitIfNeeded()
    }

    private fun confirmExitIfNeeded() {
        if (!edited) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.edit_discard_title)
            .setMessage(R.string.edit_discard_msg)
            .setPositiveButton(R.string.edit_discard) { _, _ -> finish() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun rotateBitmap() {
        val src = bitmap ?: return
        val matrix = Matrix().apply { postRotate(90f) }
        bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        ivEdit.setImageBitmap(bitmap)
        edited = true
        if (cropMode) cropOverlay.resetRect(ivEdit)
    }

    private fun toggleCropMode() {
        cropMode = !cropMode
        cropOverlay.visibility = if (cropMode) View.VISIBLE else View.GONE
        btnConfirmCrop.visibility = if (cropMode) View.VISIBLE else View.GONE
        if (cropMode) cropOverlay.resetRect(ivEdit)
    }

    private fun applyCrop() {
        val src = bitmap ?: return
        val bitmapRect = cropOverlay.getBitmapRect(ivEdit, src.width, src.height)
        if (bitmapRect != null) {
            bitmap = Bitmap.createBitmap(
                src,
                bitmapRect.left,
                bitmapRect.top,
                bitmapRect.width(),
                bitmapRect.height()
            )
            ivEdit.setImageBitmap(bitmap)
            edited = true
        }
        toggleCropMode()
    }

    private fun showTextDialog() {
        val input = EditText(this).apply { hint = getString(R.string.edit_text_hint) }
        AlertDialog.Builder(this)
            .setTitle(R.string.edit_add_text)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) stampText(text)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun stampText(text: String) {
        val src = bitmap ?: return
        val mutable = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val paint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = mutable.width * 0.07f
            setShadowLayer(10f, 0f, 0f, Color.BLACK)
        }
        canvas.drawText(text, mutable.width / 2f, mutable.height * 0.9f, paint)
        bitmap = mutable
        ivEdit.setImageBitmap(bitmap)
        edited = true
    }

    private fun saveAndFinish() {
        val bmp = bitmap
        val currentItem = item
        if (bmp == null || currentItem == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            val file = repository.fileFor(currentItem)
            withContext(Dispatchers.IO) {
                file.outputStream().use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 92, out) }
            }
            repository.refreshSize(currentItem)
            Toast.makeText(this@EditPhotoActivity, R.string.edit_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
