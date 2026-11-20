package com.myapplock

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class LockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PKG = "pkg"
        const val EXTRA_SELF = "self"
    }

    private var target: String? = null
    private var self = false

    private lateinit var title: TextView
    private lateinit var icon: ImageView
    private lateinit var status: TextView
    private lateinit var cred: CredentialView

    private val handler = Handler(Looper.getMainLooper())
    private var bioActive = false

    private val ticker = object : Runnable {
        override fun run() {
            val rem = Store.lockoutRemainingMs(this@LockActivity)
            if (rem > 0) {
                status.text = "Too many wrong attempts. Try again in ${(rem + 999) / 1000} seconds."
                cred.visibility = View.INVISIBLE
                handler.postDelayed(this, 500)
            } else if (cred.visibility != View.VISIBLE) {
                cred.visibility = View.VISIBLE
                status.text = ""
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        if (!Store.isSetup(this)) {
            finish()
            return
        }
        readIntent(intent)
        buildUi()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goHome()
            }
        })
        Session.lockShowing = true
        Session.lockTarget = target
        updateHeader()
        handler.post(ticker)
        handler.postDelayed({ showBiometric() }, 300)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!::cred.isInitialized) return
        readIntent(intent)
        Session.lockTarget = target
        updateHeader()
        cred.clear()
        status.text = ""
        handler.post(ticker)
        handler.postDelayed({ showBiometric() }, 300)
    }

    private fun readIntent(i: Intent) {
        self = i.getBooleanExtra(EXTRA_SELF, false)
        target = i.getStringExtra(EXTRA_PKG)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun buildUi() {
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG
        val root = Ui.centered(this)

        val iconWrap = LinearLayout(this)
        iconWrap.gravity = Gravity.CENTER
        iconWrap.background = Ui.rounded(Ui.CARD, 28, this, Ui.CARD_STROKE)
        iconWrap.setPadding(dp(14), dp(14), dp(14), dp(14))
        icon = ImageView(this)
        iconWrap.addView(icon, LinearLayout.LayoutParams(dp(56), dp(56)))
        root.addView(iconWrap, LinearLayout.LayoutParams(dp(84), dp(84)))

        title = TextView(this)
        title.textSize = 19f
        title.setTextColor(Ui.TEXT)
        title.setTypeface(title.typeface, android.graphics.Typeface.BOLD)
        title.gravity = Gravity.CENTER
        title.setPadding(0, dp(12), 0, dp(4))
        root.addView(title)

        val brand = TextView(this)
        brand.text = "Protected by Lockify"
        brand.textSize = 12f
        brand.setTextColor(Ui.CYAN)
        brand.gravity = Gravity.CENTER
        root.addView(brand)

        status = TextView(this)
        status.setTextColor(Ui.BAD)
        status.gravity = Gravity.CENTER
        status.minHeight = dp(36)
        status.setPadding(0, dp(8), 0, dp(4))
        root.addView(status)

        cred = CredentialView(this, Store.mode(this))
        cred.onShort = {
            Toast.makeText(this, "Use at least 4", Toast.LENGTH_SHORT).show()
        }
        cred.onSubmit = { s ->
            if (Store.lockoutRemainingMs(this) > 0) {
                cred.clear()
                handler.post(ticker)
            } else if (Store.verify(this, s)) {
                success()
            } else {
                cred.flashError()
                status.text = "Wrong! Try again"
                handler.post(ticker)
            }
        }
        root.addView(cred)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        if (Store.biometric(this)) {
            val bio = Ui.smallButton(this, "Fingerprint")
            bio.setOnClickListener { showBiometric() }
            row.addView(bio)
        }
        val rec = Ui.smallButton(this, "Recovery key")
        rec.setOnClickListener { askRecovery() }
        val rlp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rlp.setMargins(dp(6), 0, dp(6), 0)
        row.addView(rec, rlp)
        root.addView(row)

        setContentView(root)
    }

    private fun updateHeader() {
        if (self || target == null) {
            title.text = "Unlock Lockify"
            icon.setImageDrawable(applicationInfo.loadIcon(packageManager))
            return
        }
        val t = target ?: return
        try {
            val pm = packageManager
            val ai = pm.getApplicationInfo(t, 0)
            title.text = "${pm.getApplicationLabel(ai)} is locked"
            icon.setImageDrawable(pm.getApplicationIcon(ai))
        } catch (_: Exception) {
            title.text = "$t is locked"
            icon.setImageDrawable(null)
        }
    }

    private fun success() {
        if (self) Session.mainUnlocked = true else Session.unlockedPkg = target
        finish()
    }

    private fun showBiometric() {
        if (bioActive) return
        if (!Store.biometric(this) || Store.lockoutRemainingMs(this) > 0) return
        val bm = BiometricManager.from(this)
        if (bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            != BiometricManager.BIOMETRIC_SUCCESS
        ) return
        bioActive = true
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    bioActive = false
                    success()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    bioActive = false
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Lockify")
            .setNegativeButtonText("Use PIN / Pattern")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    private fun askRecovery() {
        val et = EditText(this)
        et.hint = "XXXX-XXXX-XXXX-XXXX"
        AlertDialog.Builder(this)
            .setTitle("Enter recovery key")
            .setView(et)
            .setPositiveButton("OK") { _, _ ->
                if (Store.verifyRecovery(this, et.text.toString())) {
                    Session.mainUnlocked = true
                    startActivity(Intent(this, SetupActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Wrong recovery key", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!bioActive) finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        Session.lockShowing = false
        Session.lockClosedAt = SystemClock.elapsedRealtime()
        super.onDestroy()
    }
}
