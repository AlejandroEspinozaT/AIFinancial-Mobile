package com.aifinancial.mobile.pairing

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only place device tokens are read or written. Backed by
 * EncryptedSharedPreferences (Android Keystore-derived key), never plain
 * SharedPreferences and never logged — see specs/architecture/overview.md
 * and the project's code-review skill.
 */
@Singleton
class AuthTokenStore @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    /**
     * `deviceName`/`email` are stored only for Stage 4's Settings screen —
     * see `DeviceAuthResponseDto`'s KDoc on the backend: email is returned
     * specifically so this device can show "linked to ...@..." without a
     * further authenticated call.
     */
    fun saveTokens(deviceId: String, deviceName: String, email: String, accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_DEVICE_NAME, deviceName)
            .putString(KEY_EMAIL, email)
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getDeviceId(): String? = prefs.getString(KEY_DEVICE_ID, null)

    fun getDeviceName(): String? = prefs.getString(KEY_DEVICE_NAME, null)

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun isPaired(): Boolean = getAccessToken() != null

    /**
     * Stage 2's token refresh: the backend rotates both tokens on every
     * refresh (see `RefreshTokenCommandHandler`), but the device id never
     * changes, so it's left untouched here rather than re-read/re-written.
     */
    fun updateTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    /** Called on unlink (Stage 4) or on an unrecoverable refresh failure (Stage 2). */
    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_FILE_NAME = "aifinancial_auth_tokens"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_DEVICE_NAME = "device_name"
        const val KEY_EMAIL = "email"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
