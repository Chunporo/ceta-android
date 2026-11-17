package com.ceta.cinema.data.local.secure

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore by preferencesDataStore(name = "tokens")

/** Tokens live in DataStore, encrypted with an AES/GCM key that never leaves the Android Keystore. */
@Singleton
class KeystoreTokenStore @Inject constructor(@ApplicationContext private val context: Context) : TokenStore {
    private val accessKey = stringPreferencesKey("access")
    private val refreshKey = stringPreferencesKey("refresh")

    override suspend fun saveTokens(access: String, refresh: String) {
        context.tokenDataStore.edit {
            it[accessKey] = encrypt(access)
            it[refreshKey] = encrypt(refresh)
        }
    }

    override suspend fun access(): String? = read(accessKey)

    override suspend fun refresh(): String? = read(refreshKey)

    override suspend fun clear() {
        context.tokenDataStore.edit { it.clear() }
    }

    private suspend fun read(key: androidx.datastore.preferences.core.Preferences.Key<String>): String? {
        val stored = context.tokenDataStore.data.first()[key] ?: return null
        // A key lost to a backup restore or a lock-screen reset cannot decrypt: treat it as signed out.
        return runCatching { decrypt(stored) }.getOrNull()
    }

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, secretKey()) }
        val out = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes, 0, IV_BYTES))
        }
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }

    private companion object {
        const val ALIAS = "ceta_tokens"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
    }
}
