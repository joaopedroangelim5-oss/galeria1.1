package com.gallerykisser.app.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.gallerykisser.app.R
import com.gallerykisser.app.util.PinManager

class PinSetupActivity : AppCompatActivity() {

    private enum class Step { VERIFY_CURRENT, CREATE_NEW, CONFIRM_NEW }

    private lateinit var tvTitle: TextView
    private lateinit var keypad: PinKeypad
    private var step: Step = Step.CREATE_NEW
    private var firstEntry: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)
        tvTitle = findViewById(R.id.tvPinTitle)

        step = if (PinManager.hasPin(this)) Step.VERIFY_CURRENT else Step.CREATE_NEW
        updateTitle()

        keypad = PinKeypad(this) { pin -> onPinEntered(pin) }
    }

    private fun updateTitle() {
        tvTitle.text = when (step) {
            Step.VERIFY_CURRENT -> getString(R.string.pin_enter)
            Step.CREATE_NEW -> getString(R.string.pin_create)
            Step.CONFIRM_NEW -> getString(R.string.pin_confirm)
        }
    }

    private fun onPinEntered(pin: String) {
        when (step) {
            Step.VERIFY_CURRENT -> {
                if (PinManager.verify(this, pin)) {
                    keypad.hideError()
                    keypad.reset()
                    showManageOptions()
                } else {
                    keypad.showError()
                }
            }
            Step.CREATE_NEW -> {
                firstEntry = pin
                step = Step.CONFIRM_NEW
                keypad.hideError()
                keypad.reset()
                updateTitle()
            }
            Step.CONFIRM_NEW -> {
                if (pin == firstEntry) {
                    PinManager.setPin(this, pin)
                    Toast.makeText(this, R.string.pin_enable, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, R.string.pin_mismatch, Toast.LENGTH_SHORT).show()
                    firstEntry = null
                    step = Step.CREATE_NEW
                    keypad.reset()
                    updateTitle()
                }
            }
        }
    }

    private fun showManageOptions() {
        val options = arrayOf(getString(R.string.pin_disable), getString(R.string.pin_create))
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_pin)
            .setItems(options) { _, which ->
                if (which == 0) {
                    PinManager.clearPin(this)
                    Toast.makeText(this, R.string.pin_disable, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    step = Step.CREATE_NEW
                    updateTitle()
                }
            }
            .setOnCancelListener { finish() }
            .show()
    }
}
