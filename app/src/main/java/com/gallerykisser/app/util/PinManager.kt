package com.gallerykisser.app.util

import android.content.Context
import java.security.MessageDigest

/**
 * Guarda apenas o HASH (SHA-256 + salt fixo do app) do PIN, nunca o PIN em texto puro.
 */
object PinManager {
    private const val PREFS = "gk_secure_prefs"
    private const val KEY_HASH = "pin_hash"
    private const val KEY_ENABLED = "pin_enabled"
    private const val SALT = "gallery_kisser_salt_v1"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((SALT + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun hasPin(context: Context): Boolean =
        prefs(context).contains(KEY_HASH)

    fun setPin(context: Context, pin: String) {
        prefs(context).edit()
            .putString(KEY_HASH, hash(pin))
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }

    fun disable(context: Context) {
        prefs(context).edit().putBoolean(KEY_ENABLED, false).apply()
    }

    fun clearPin(context: Context) {
        prefs(context).edit().remove(KEY_HASH).putBoolean(KEY_ENABLED, false).apply()
    }

    fun verify(context: Context, pin: String): Boolean =
        prefs(context).getString(KEY_HASH, null) == hash(pin)
}
