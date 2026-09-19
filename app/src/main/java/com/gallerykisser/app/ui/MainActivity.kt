package com.gallerykisser.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.gallerykisser.app.R
import com.gallerykisser.app.data.FolderEntity
import com.gallerykisser.app.data.MediaItem
import com.gallerykisser.app.util.BackgroundManager

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FOLDER_ID = "extra_folder_id"
        const val EXTRA_FOLDER_NAME = "extra_folder_name"
        const val EXTRA_SECURE_MODE = "extra_secure_mode"
        const val SPAN_COUNT = 3
    }

    private lateinit var viewModel: GalleryViewModel
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: View
    private lateinit var ivBackground: android.widget.ImageView
    private lateinit var fabAdd: View
    private lateinit var adapter: GalleryAdapter
    private lateinit var layoutManager: GridLayoutManager

    private var currentFolderId: Long? = null
    private var currentFolders: List<FolderEntity> = emptyList()
    private var currentMedia: List<MediaItem> = emptyList()
    private var secureMode = false
    private var screenTitle: String = ""

    private val selectedIds = linkedSetOf<Long>()
    private var selectionMode = false

    // Import de fotos/vídeos via seletor do sistema (não precisa de permissão em runtime)
    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val flags = uris.map { uri ->
                contentResolver.getType(uri)?.startsWith("video") == true
            }
            viewModel.importUris(uris, flags)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        secureMode = intent.getBooleanExtra(EXTRA_SECURE_MODE, false)
        currentFolderId = if (intent.hasExtra(EXTRA_FOLDER_ID)) intent.getLongExtra(EXTRA_FOLDER_ID, -1) else null

        viewModel = ViewModelProvider(this)[GalleryViewModel::class.java]
        viewModel.setSecureMode(secureMode)
        viewModel.setFolder(currentFolderId)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        tvEmpty = findViewById(R.id.tvEmpty)
        ivBackground = findViewById(R.id.ivCustomBackground)
        fabAdd = findViewById(R.id.fabAdd)

        screenTitle = if (secureMode) {
            getString(R.string.secure_mode)
        } else {
            intent.getStringExtra(EXTRA_FOLDER_NAME) ?: getString(R.string.menu_all_photos)
        }
        toolbar.title = screenTitle

        if (secureMode) {
            // No Modo Seguro não existe navegação por pastas nem plano de fundo customizado:
            // é só a galeria escondida, com um botão de voltar simples.
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            fabAdd.visibility = View.GONE
            (tvEmpty as? android.widget.TextView)?.text = getString(R.string.secure_mode_empty)
            toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_back)
            toolbar.setNavigationOnClickListener {
                if (selectionMode) exitSelectionMode() else finish()
            }
        } else {
            toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_menu)
            toolbar.setNavigationOnClickListener {
                if (selectionMode) exitSelectionMode() else drawerLayout.openDrawer(GravityCompat.START)
            }
            fabAdd.setOnClickListener {
                pickMediaLauncher.launch(arrayOf("image/*", "video/*"))
            }
            setupDrawer()
            applyBackground()
        }

        setupRecyclerView()

        viewModel.items.observe(this) { media ->
            currentMedia = media
            tvEmpty.visibility = if (media.isEmpty()) View.VISIBLE else View.GONE
            adapter.submitList(DateGrouping.group(media))
        }

        viewModel.folders.observe(this) { folders ->
            currentFolders = folders
            if (!secureMode) rebuildFolderMenu(folders)
        }
    }

    private fun setupRecyclerView() {
        adapter = GalleryAdapter(
            repository = viewModel.repository,
            onClick = { media, _ -> onMediaClick(media) },
            onLongClick = { media -> onMediaLongClick(media) },
            isSelected = { id -> selectedIds.contains(id) }
        )

        layoutManager = GridLayoutManager(this, SPAN_COUNT)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (adapter.isHeader(position)) SPAN_COUNT else 1
        }
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
    }

    private fun onMediaClick(media: MediaItem) {
        if (selectionMode) {
            toggleSelection(media.id)
        } else {
            val ids = currentMedia.map { it.id }.toLongArray()
            val startIndex = currentMedia.indexOfFirst { it.id == media.id }.coerceAtLeast(0)
            startActivity(
                Intent(this, ViewerActivity::class.java)
                    .putExtra(ViewerActivity.EXTRA_MEDIA_IDS, ids)
                    .putExtra(ViewerActivity.EXTRA_START_INDEX, startIndex)
            )
        }
    }

    private fun onMediaLongClick(media: MediaItem) {
        if (!selectionMode) enterSelectionMode()
        toggleSelection(media.id)
    }

    private fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
        if (selectedIds.isEmpty()) exitSelectionMode() else updateSelectionTitle()
        adapter.submitList(DateGrouping.group(currentMedia))
    }

    private fun enterSelectionMode() {
        selectionMode = true
        toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_back)
        toolbar.menu.clear()
        toolbar.inflateMenu(if (secureMode) R.menu.menu_selection_secure else R.menu.menu_selection)
        toolbar.setOnMenuItemClickListener { item -> handleSelectionMenu(item) }
        updateSelectionTitle()
    }

    private fun exitSelectionMode() {
        selectionMode = false
        selectedIds.clear()
        toolbar.menu.clear()
        toolbar.navigationIcon = ContextCompat.getDrawable(
            this, if (secureMode) R.drawable.ic_back else R.drawable.ic_menu
        )
        toolbar.title = screenTitle
        adapter.submitList(DateGrouping.group(currentMedia))
    }

    private fun updateSelectionTitle() {
        toolbar.title = selectedIds.size.toString()
    }

    private fun selectAll() {
        selectedIds.clear()
        selectedIds.addAll(currentMedia.map { it.id })
        updateSelectionTitle()
        adapter.submitList(DateGrouping.group(currentMedia))
    }

    private fun handleSelectionMenu(item: MenuItem): Boolean {
        val selectedItems = currentMedia.filter { selectedIds.contains(it.id) }
        when (item.itemId) {
            R.id.actionSelectAll -> selectAll()
            R.id.actionDelete -> confirmDelete(selectedItems)
            R.id.actionExport -> {
                viewModel.exportItems(selectedItems) { count ->
                    Toast.makeText(this, "$count · ${getString(R.string.export_success)}", Toast.LENGTH_SHORT).show()
                    exitSelectionMode()
                }
            }
            R.id.actionMove -> showMoveDialog(selectedItems)
            R.id.actionSecure -> {
                viewModel.setSecure(selectedItems.map { it.id }, true) {
                    Toast.makeText(this, R.string.moved_to_secure, Toast.LENGTH_SHORT).show()
                    exitSelectionMode()
                }
            }
            R.id.actionRemoveSecure -> {
                viewModel.setSecure(selectedItems.map { it.id }, false) {
                    Toast.makeText(this, R.string.removed_from_secure, Toast.LENGTH_SHORT).show()
                    exitSelectionMode()
                }
            }
        }
        return true
    }

    private fun confirmDelete(items: List<MediaItem>) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_msg)
            .setPositiveButton(R.string.delete_selected) { _, _ ->
                viewModel.deleteItems(items) { exitSelectionMode() }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showMoveDialog(items: List<MediaItem>) {
        val names = mutableListOf(getString(R.string.menu_all_photos))
        val ids = mutableListOf<Long?>(null)
        currentFolders.forEach { names.add(it.name); ids.add(it.id) }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.move_to_folder)
            .setItems(names.toTypedArray()) { _, which ->
                viewModel.moveToFolder(items.map { it.id }, ids[which]) { exitSelectionMode() }
            }
            .show()
    }

    private fun setupDrawer() {
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navAllPhotos -> {
                    if (currentFolderId != null) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
                R.id.navNewFolder -> showCreateFolderDialog()
                R.id.navSettings -> startActivity(Intent(this, SettingsActivity::class.java))
                else -> {
                    val folder = currentFolders.firstOrNull { it.id.toInt() == item.itemId }
                    if (folder != null) {
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .putExtra(EXTRA_FOLDER_ID, folder.id)
                                .putExtra(EXTRA_FOLDER_NAME, folder.name)
                        )
                        finish()
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun rebuildFolderMenu(folders: List<FolderEntity>) {
        val menu = navView.menu
        menu.removeGroup(R.id.groupFolders)
        folders.forEach { folder ->
            menu.add(R.id.groupFolders, folder.id.toInt(), Menu.NONE, folder.name)
                .setIcon(R.drawable.ic_folder)
                .setCheckable(true)
        }
        menu.setGroupCheckable(R.id.groupFolders, true, true)
    }

    private fun showCreateFolderDialog() {
        val input = EditText(this).apply { hint = getString(R.string.new_folder_hint) }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.menu_new_folder)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) viewModel.createFolder(name) {}
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun applyBackground() {
        val bg = BackgroundManager.getBackgroundFile(this)
        if (bg != null) {
            Glide.with(this).load(bg).centerCrop().into(ivBackground)
        } else {
            ivBackground.setImageDrawable(null)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!secureMode) applyBackground()
    }
}
