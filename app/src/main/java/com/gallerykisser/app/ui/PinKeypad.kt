package com.gallerykisser.app.ui

import android.app.Activity
import android.widget.Button
import android.widget.TextView
import com.gallerykisser.app.R

/**
 * Liga os botões numéricos de activity_pin.xml e mantém o PIN digitado até
 * atingir 4 dígitos, quando chama onComplete. Cuida também dos "pontinhos"
 * visuais e da mensagem de erro.
 */
class PinKeypad(
    activity: Activity,
    private val onComplete: (String) -> Unit
) {
    private val dots = listOf(
        activity.findViewById<android.view.View>(R.id.dot1),
        activity.findViewById(R.id.dot2),
        activity.findViewById(R.id.dot3),
        activity.findViewById(R.id.dot4)
    )
    private val tvError = activity.findViewById<TextView>(R.id.tvPinError)
    private var current = StringBuilder()

    init {
        val digitIds = intArrayOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )
        digitIds.forEach { id ->
            val button = activity.findViewById<Button>(id)
            button.setOnClickListener { onDigit(button.text.toString()) }
        }
        activity.findViewById<Button>(R.id.btnDel).setOnClickListener { onDelete() }
    }

    private fun onDigit(digit: String) {
        if (current.length >= 4) return
        current.append(digit)
        updateDots()
        if (current.length == 4) {
            val pin = current.toString()
            onComplete(pin)
        }
    }

    private fun onDelete() {
        if (current.isNotEmpty()) {
            current.deleteCharAt(current.length - 1)
            updateDots()
        }
    }

    fun reset() {
        current.clear()
        updateDots()
    }

    fun showError() {
        tvError.visibility = android.view.View.VISIBLE
        reset()
    }

    fun hideError() {
        tvError.visibility = android.view.View.INVISIBLE
    }

    private fun updateDots() {
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < current.length) R.drawable.bg_pin_dot_filled else R.drawable.bg_pin_dot
            )
        }
    }
}
