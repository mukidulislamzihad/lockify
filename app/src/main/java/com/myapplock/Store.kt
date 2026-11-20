package com.myapplock

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * All settings are kept in encrypted SharedPreferences.
 * The PIN/pattern is never saved directly, only its PBKDF2 hash.
 */
object Store {
    private const val FILE = "lock_store"
    private const val ITER = 120_000
    private const val MAX_LOCK_MS = 60L * 60 * 1000

    const val MODE_PIN = "pin"
    const val MODE_PATTERN = "pattern"

    @Volatile private var cached: SharedPreferences? = null
    @Volatile private var lockedCache: Set<String>? = null

    private fun prefs(ctx: Context): SharedPreferences {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val app = ctx.applicationContext
            val sp = try {
                open(app)
            } catch (e: Exception) {
                // If file/key is corrupted, start fresh (treated as "not set up", i.e. fail-closed)
                try {
                    val ks = KeyStore.getInstance("AndroidKeyStore")
                    ks.load(null)
                    ks.deleteEntry("_androidx_security_master_key_")
                } catch (_: Exception) {
                }
                app.deleteSharedPreferences(FILE)
                open(app)
            }
            cached = sp
            return sp
        }
    }

    private fun open(app: Context): SharedPreferences {
        val key = MasterKey.Builder(app)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            app, FILE, key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun hash(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITER, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun b64(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun b64d(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)

    // ---------- Setup / credentials ----------

    fun isSetup(ctx: Context): Boolean = prefs(ctx).contains("hash")

    fun mode(ctx: Context): String = prefs(ctx).getString("mode", MODE_PIN) ?: MODE_PIN

    /** Sets a new PIN/pattern and returns a one-time recovery key to show. */
    fun setCredential(ctx: Context, mode: String, secret: String): String {
        val rnd = SecureRandom()
        val salt = ByteArray(16).also { rnd.nextBytes(it) }
        val rsalt = ByteArray(16).also { rnd.nextBytes(it) }
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val raw = (1..16).map { alphabet[rnd.nextInt(alphabet.length)] }.joinToString("")
        prefs(ctx).edit()
            .putString("mode", mode)
            .putString("salt", b64(salt))
            .putString("hash", b64(hash(secret, salt)))
            .putString("rsalt", b64(rsalt))
            .putString("rhash", b64(hash(raw, rsalt)))
            .putInt("fails", 0)
            .putLong("lock_until", 0L)
            .commit()
        return raw.chunked(4).joinToString("-")
    }

    private fun check(ctx: Context, secret: String): Boolean {
        val p = prefs(ctx)
        val h = b64d(p.getString("hash", "") ?: "")
        val salt = b64d(p.getString("salt", "") ?: "")
        if (h.isEmpty()) return false
        return MessageDigest.isEqual(h, hash(secret, salt))
    }

    // ---------- Wrong attempts & lockout ----------

    fun lockoutRemainingMs(ctx: Context): Long {
        val until = prefs(ctx).getLong("lock_until", 0L)
        val rem = until - System.currentTimeMillis()
        return if (rem <= 0) 0L else rem.coerceAtMost(MAX_LOCK_MS)
    }

    /** True if correct. Always false during lockout. Delay grows after 5 wrong tries. */
    fun verify(ctx: Context, secret: String): Boolean {
        if (lockoutRemainingMs(ctx) > 0) return false
        val p = prefs(ctx)
        if (check(ctx, secret)) {
            p.edit().putInt("fails", 0).putLong("lock_until", 0L).commit()
            return true
        }
        val fails = p.getInt("fails", 0) + 1
        val e = p.edit().putInt("fails", fails)
        if (fails >= 5) {
            val ms = (30_000L shl (fails - 5).coerceAtMost(7)).coerceAtMost(MAX_LOCK_MS)
            e.putLong("lock_until", System.currentTimeMillis() + ms)
        }
        e.commit()
        return false
    }

    fun verifyRecovery(ctx: Context, input: String): Boolean {
        val norm = input.uppercase().filter { it.isLetterOrDigit() }
        if (norm.length != 16) return false
        val p = prefs(ctx)
        val h = b64d(p.getString("rhash", "") ?: "")
        val salt = b64d(p.getString("rsalt", "") ?: "")
        if (h.isEmpty()) return false
        val ok = MessageDigest.isEqual(h, hash(norm, salt))
        if (ok) p.edit().putInt("fails", 0).putLong("lock_until", 0L).commit()
        return ok
    }

    // ---------- Locked apps list ----------

    fun lockedApps(ctx: Context): Set<String> {
        lockedCache?.let { return it }
        val s = (prefs(ctx).getStringSet("locked", emptySet()) ?: emptySet()).toSet()
        lockedCache = s
        return s
    }

    fun setLocked(ctx: Context, pkg: String, on: Boolean) {
        val s = lockedApps(ctx).toMutableSet()
        if (on) s.add(pkg) else s.remove(pkg)
        prefs(ctx).edit().putStringSet("locked", s).commit()
        lockedCache = s
    }

    // ---------- Other settings ----------

    fun biometric(ctx: Context): Boolean = prefs(ctx).getBoolean("biometric", false)
    fun setBiometric(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean("biometric", on).commit()
    }

    /** Temporary pause for uninstalling: Settings/Play Store/Installer stay unlocked until this time. */
    fun pauseUntil(ctx: Context): Long = prefs(ctx).getLong("pause_until", 0L)
    fun setPause(ctx: Context, untilMs: Long) {
        prefs(ctx).edit().putLong("pause_until", untilMs).commit()
    }
}
