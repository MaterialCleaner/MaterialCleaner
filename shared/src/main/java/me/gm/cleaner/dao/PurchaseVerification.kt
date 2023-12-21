package me.gm.cleaner.dao

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import me.gm.cleaner.annotation.App
import me.gm.cleaner.annotation.Server

object PurchaseVerification {

    val encryptedSharedPreferences: SharedPreferences by lazy {
        SecurityHelper.encryptedSharedPreferences("__androidx_security_crypto_encrypted_pref__")
    }

    fun getKeyset(context: Context): String? = context
        .getSharedPreferences("__androidx_security_crypto_encrypted_pref__", Context.MODE_PRIVATE)
        .getString("__androidx_security_crypto_encrypted_prefs_key_keyset__", null)

    @App
    fun updateCertificate(responseBody: String) {
    }

    @App
    fun removeCertificate(purchaseToken: String) {
        encryptedSharedPreferences.edit {
            remove(purchaseToken)
        }
    }

    @App
    inline var isSyncNeeded: Boolean
        get() = false
        set(value) = encryptedSharedPreferences.edit { }

    @App
    inline fun getRevalidationCertificates(): Set<String> {
        return emptySet()
    }

    @App
    inline fun removeRevalidationCertificate() {
    }

    inline fun addRevalidationCertificate(purchaseToken: String) {
    }

    @App
    inline val maybeCertificates: List<String>
        get() = encryptedSharedPreferences
            .all
            .values
            .filterIsInstance<String>()

    @App
    var signatures: Set<String>
        get() = emptySet()
        set(value) = encryptedSharedPreferences.edit { }

    // Cleanup
    @App
    inline val isCleanupPro: Boolean
        get() = true

    // CleanerService
    @App
    inline val isExpressPro: Boolean
        get() = true

    @App
    inline val isStrictPro: Boolean
        get() = true

    @App
    @Server
    inline val isLoosePro: Boolean
        get() = true
}
