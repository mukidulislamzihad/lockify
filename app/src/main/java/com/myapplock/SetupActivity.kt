package com.myapplock

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/** First-time setup, or changing the PIN/pattern. */
class SetupActivity : AppCompatActivity() {

    private var mode = Store.MODE_PIN
    private var first: String? = null
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG
        root = Ui.centered(this)
        setContentView(root)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Leaving before setup is finished closes the whole app (avoids a loop)
                if (Store.isSetup(this@SetupActivity)) finish() else finishAffinity()
            }
        })
        showChoose()
    }

    private fun btnLp() = LinearLayout.LayoutParams(dp(280), ViewGroup.LayoutParams.WRAP_CONTENT)
        .also { it.setMargins(0, dp(6), 0, dp(6)) }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun label(text: String, size: Float): TextView {
        val t = TextView(this)
        t.text = text
        t.textSize = size
        t.setTextColor(Ui.TEXT)
        t.gravity = Gravity.CENTER
        t.setPadding(0, dp(8), 0, dp(16))
        return t
    }

    private fun showChoose() {
        root.removeAllViews()
        val logo = android.widget.ImageView(this)
        logo.setImageResource(R.drawable.ic_logo)
        root.addView(logo, LinearLayout.LayoutParams(dp(96), dp(96)))
        root.addView(label("Welcome to Lockify", 24f).also { it.setTypeface(it.typeface, android.graphics.Typeface.BOLD) })
        val sub = label(Ui.TAGLINE, 13f)
        sub.setTextColor(Ui.CYAN)
        root.addView(sub)
        root.addView(label("Choose your lock method", 16f))

        val pin = Ui.primaryButton(this, "PIN (numbers)")
        pin.setOnClickListener {
            mode = Store.MODE_PIN
            first = null
            showEnter(false)
        }
        root.addView(pin, btnLp())

        val pat = Ui.ghostButton(this, "Pattern")
        pat.setOnClickListener {
            mode = Store.MODE_PATTERN
            first = null
            showEnter(false)
        }
        root.addView(pat, btnLp())

        val hint = label("You can turn on fingerprint later from the main screen.", 13f)
        hint.setTextColor(Ui.MUTED)
        root.addView(hint)
    }

    private fun showEnter(confirm: Boolean) {
        root.removeAllViews()
        val msg = when {
            confirm -> "Enter it again to confirm"
            mode == Store.MODE_PIN -> "Enter a new PIN (4-12 digits), then tap OK"
            else -> "Draw a pattern (at least 4 dots)"
        }
        root.addView(label(msg, 20f))

        val cred = CredentialView(this, mode)
        cred.onShort = {
            Toast.makeText(this, "Use at least 4", Toast.LENGTH_SHORT).show()
        }
        cred.onSubmit = { s ->
            if (!confirm) {
                first = s
                showEnter(true)
            } else if (s == first) {
                done(s)
            } else {
                Toast.makeText(this, "Didn't match, start again", Toast.LENGTH_SHORT).show()
                first = null
                showEnter(false)
            }
        }
        root.addView(cred)
    }

    private fun done(secret: String) {
        val key = Store.setCredential(this, mode, secret)
        Session.mainUnlocked = true
        val dlg = AlertDialog.Builder(this)
            .setTitle("Recovery key")
            .setMessage(
                "If you forget your PIN/pattern, this key is the only way back in.\n" +
                    "Write it down on paper now and keep it somewhere safe. It will not be shown again:\n\n$key"
            )
            .setCancelable(false)
            .setPositiveButton("I've saved it") { _, _ -> finish() }
            .create()
        dlg.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        dlg.show()
    }
}
