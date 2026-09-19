package com.gallerykisser.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gallerykisser.app.R
import com.gallerykisser.app.util.PinManager

/**
 * Portão de entrada do Modo Seguro: exige o mesmo PIN do bloqueio do app antes de
 * mostrar a galeria escondida. Se ainda não existe PIN configurado, pede pra
 * configurar um primeiro, por segurança (senão qualquer um entraria sem digitar nada).
 */
class SecureLockActivity : AppCompatActivity() {

    private var keypad: PinKeypad? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!PinManager.hasPin(this)) {
            Toast.makeText(this, R.string.secure_needs_pin, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_pin)
        findViewById<TextView>(R.id.tvPinTitle).text = getString(R.string.secure_enter_pin)

        keypad = PinKeypad(this) { pin ->
            if (PinManager.verify(this, pin)) {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_SECURE_MODE, true)
                )
                finish()
            } else {
                keypad?.showError()
            }
        }
    }
}
