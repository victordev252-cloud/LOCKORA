package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Master credential storage using PBKDF2-HMAC-SHA256, 120,000 iterations, 16-byte cryptographically secure salt.
 * Never stores raw PIN/password in memory or disk.
 */
class SecurityManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCredentialSet(): Boolean = prefs.contains(KEY_HASH)

    fun setCredential(secret: CharArray, mode: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(secret, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_MODE, mode)
            .apply()
    }

    fun verify(secret: CharArray): Boolean {
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expected = Base64.decode(hashB64, Base64.NO_WRAP)
        val actual = pbkdf2(secret, salt)
        return constantTimeEquals(expected, actual)
    }

    fun credentialMode(): String = prefs.getString(KEY_MODE, "pin") ?: "pin"

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun failedAttemptCount(): Int = prefs.getInt(KEY_FAIL_COUNT, 0)
    fun lastFailTime(): Long = prefs.getLong(KEY_FAIL_TIME, 0L)

    fun recordFailure() {
        prefs.edit()
            .putInt(KEY_FAIL_COUNT, failedAttemptCount() + 1)
            .putLong(KEY_FAIL_TIME, System.currentTimeMillis())
            .apply()
    }

    fun resetFailures() {
        prefs.edit()
            .putInt(KEY_FAIL_COUNT, 0)
            .putLong(KEY_FAIL_TIME, 0L)
            .apply()
    }

    fun cooldownRemainingMs(): Long {
        val count = failedAttemptCount()
        val last = lastFailTime()
        if (count < 5) return 0L
        val delay = when {
            count < 10 -> 30_000L
            count < 15 -> 5 * 60_000L
            else -> 30 * 60_000L
        }
        val elapsed = System.currentTimeMillis() - last
        return (delay - elapsed).coerceAtLeast(0L)
    }

    private fun pbkdf2(secret: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret, salt, ITERATIONS, 256)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val out = skf.generateSecret(spec).encoded
        spec.clearPassword()
        return out
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var r = 0
        for (i in a.indices) {
            r = r or (a[i].toInt() xor b[i].toInt())
        }
        return r == 0
    }

    companion object {
        private const val PREFS_NAME = "lockora_secure_prefs"
        private const val KEY_HASH = "hash"
        private const val KEY_SALT = "salt"
        private const val KEY_MODE = "mode"
        private const val KEY_FAIL_COUNT = "fail_count"
        private const val KEY_FAIL_TIME = "fail_time"
        private const val ITERATIONS = 120_000

        @Volatile
        private var instance: SecurityManager? = null

        fun get(context: Context): SecurityManager =
            instance ?: synchronized(this) {
                instance ?: SecurityManager(context).also { instance = it }
            }
    }
}
