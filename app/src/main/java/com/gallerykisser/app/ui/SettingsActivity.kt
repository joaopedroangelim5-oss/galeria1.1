package com.gallerykisser.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.gallerykisser.app.R
import com.gallerykisser.app.util.BackgroundManager

class SettingsActivity : AppCompatActivity() {

    private val pickBackgroundLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            BackgroundManager.setBackground(this, it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Toolbar>(R.id.toolbarSettings).setNavigationOnClickListener { finish() }

        findViewById<android.view.View>(R.id.rowBackground).setOnClickListener { showBackgroundOptions() }
        findViewById<android.view.View>(R.id.rowPin).setOnClickListener {
            startActivity(Intent(this, PinSetupActivity::class.java))
        }
        findViewById<android.view.View>(R.id.rowSecure).setOnClickListener {
            startActivity(Intent(this, SecureLockActivity::class.java))
        }

        showAppVersion()
    }

    private fun showAppVersion() {
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
        findViewById<TextView>(R.id.tvVersion).text =
            getString(R.string.settings_version_template, getString(R.string.app_name), versionName ?: "1.0")
    }

    private fun showBackgroundOptions() {
        val options = arrayOf(getString(R.string.choose_background_image), getString(R.string.reset_background))
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_background)
            .setItems(options) { _, which ->
                if (which == 0) {
                    pickBackgroundLauncher.launch(arrayOf("image/*"))
                } else {
                    BackgroundManager.clearBackground(this)
                }
            }
            .show()
    }
}
