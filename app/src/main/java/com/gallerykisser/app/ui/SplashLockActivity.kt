package com.gallerykisser.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gallerykisser.app.R
import com.gallerykisser.app.util.PinManager

class SplashLockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PinManager.isEnabled(this)) {
            goToGallery()
            return
        }

        setContentView(R.layout.activity_pin)
        findViewById<TextView>(R.id.tvPinTitle).text = getString(R.string.pin_enter)

        val keypad = PinKeypad(this) { pin ->
            if (PinManager.verify(this, pin)) {
                goToGallery()
            } else {
                showWrongPin()
            }
        }
        this.keypad = keypad
    }

    private var keypad: PinKeypad? = null

    private fun showWrongPin() {
        keypad?.showError()
    }

    private fun goToGallery() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
