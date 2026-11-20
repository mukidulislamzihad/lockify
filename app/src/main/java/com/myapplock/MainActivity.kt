package com.myapplock

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.biometric.BiometricManager

class MainActivity : AppCompatActivity() {

    private data class AppItem(val pkg: String, val label: String, val icon: Drawable)

    private val apps = ArrayList<AppItem>()
    private val lockedSet = HashSet<String>()
    private lateinit var adapter: BaseAdapter

    private lateinit var tvAcc: TextView
    private lateinit var tvOv: TextView
    private lateinit var tvAdm: TextView
    private lateinit var tvBat: TextView
    private lateinit var swBio: SwitchCompat

    private val comp by lazy { ComponentName(this, AdminReceiver::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setPadding(dp(16), dp(20), dp(16), dp(8))

        // Brand block
        val brand = LinearLayout(this)
        brand.orientation = LinearLayout.VERTICAL
        brand.gravity = Gravity.CENTER_HORIZONTAL
        val logo = ImageView(this)
        logo.setImageResource(R.drawable.ic_logo)
        brand.addView(logo, LinearLayout.LayoutParams(dp(88), dp(88)))
        val title = Ui.text(this, "Lockify", 30f, Ui.TEXT, true)
        title.setPadding(0, dp(10), 0, 0)
        brand.addView(title)
        val tag = Ui.text(this, Ui.TAGLINE, 13f, Ui.CYAN)
        tag.setPadding(0, dp(2), 0, dp(16))
        brand.addView(tag)
        header.addView(brand, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // Protection setup card
        val setup = Ui.card(this)
        setup.addView(Ui.text(this, "Protection setup", 17f, Ui.TEXT, true))
        val hint = Ui.text(this, "Turn every item below to ✅, then switch on the apps you want to lock.", 13f, Ui.MUTED)
        hint.setPadding(0, dp(4), 0, dp(8))
        setup.addView(hint)

        tvAcc = statusRow(setup, "Enable") {
            openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        tvOv = statusRow(setup, "Allow") {
            openSettings(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
        tvAdm = statusRow(setup, "Enable") {
            openSettings(
                Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
                    .putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Required for uninstall protection"
                    )
            )
        }
        tvBat = statusRow(setup, "Allow") {
            openSettings(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        }
        header.addView(setup, cardLp(0, 0, 0, 12))

        // Settings card
        val opts = Ui.card(this)
        swBio = SwitchCompat(this)
        swBio.text = "Unlock with fingerprint"
        swBio.setTextColor(Ui.TEXT)
        swBio.setPadding(0, dp(4), 0, dp(10))
        opts.addView(swBio)

        val change = Ui.primaryButton(this, "Change PIN / Pattern")
        change.setOnClickListener { startActivity(Intent(this, SetupActivity::class.java)) }
        opts.addView(change, cardLp(0, 0, 0, 8))

        val pause = Ui.ghostButton(this, "10-min protection pause (to uninstall / disable)")
        pause.setOnClickListener { confirmPause() }
        opts.addView(pause)
        header.addView(opts, cardLp(0, 0, 0, 12))

        val listTitle = Ui.text(this, "Apps to lock", 18f, Ui.TEXT, true)
        listTitle.setPadding(dp(4), dp(8), 0, dp(4))
        header.addView(listTitle)

        // Developer footer
        val footer = Ui.card(this)
        footer.gravity = Gravity.CENTER_HORIZONTAL
        footer.addView(Ui.text(this, "Developed by", 12f, Ui.MUTED))
        val dev = Ui.text(this, Ui.DEV_NAME, 18f, Ui.TEXT, true)
        dev.setPadding(0, dp(2), 0, dp(2))
        footer.addView(dev)
        val role = Ui.text(this, Ui.DEV_ROLE, 12f, Ui.MUTED)
        role.gravity = Gravity.CENTER
        footer.addView(role)
        val portfolio = Ui.smallButton(this, "View Portfolio")
        portfolio.setOnClickListener {
            Session.graceUntil = SystemClock.elapsedRealtime() + 30_000
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Ui.DEV_URL)))
            } catch (_: Exception) {
                Toast.makeText(this, "Couldn't open link", Toast.LENGTH_SHORT).show()
            }
        }
        val plp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        plp.topMargin = dp(10)
        footer.addView(portfolio, plp)
        val footWrap = LinearLayout(this)
        footWrap.setPadding(dp(16), dp(12), dp(16), dp(24))
        footWrap.addView(footer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val list = ListView(this)
        list.setBackgroundColor(Ui.BG)
        list.divider = null
        list.dividerHeight = 0
        list.setSelector(android.R.color.transparent)
        list.addHeaderView(header, null, false)
        list.addFooterView(footWrap, null, false)
        adapter = object : BaseAdapter() {
            override fun getCount(): Int = apps.size
            override fun getItem(position: Int): Any = apps[position]
            override fun getItemId(position: Int): Long = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val outer = (convertView as? LinearLayout) ?: buildRow()
                val row = outer.getChildAt(0) as LinearLayout
                val item = apps[position]
                (row.getChildAt(0) as ImageView).setImageDrawable(item.icon)
                (row.getChildAt(1) as TextView).text = item.label
                val sw = row.getChildAt(2) as SwitchCompat
                sw.setOnCheckedChangeListener(null)
                sw.isChecked = item.pkg in lockedSet
                sw.setOnCheckedChangeListener { _, on ->
                    Store.setLocked(this@MainActivity, item.pkg, on)
                    if (on) lockedSet.add(item.pkg) else lockedSet.remove(item.pkg)
                }
                return outer
            }
        }
        list.adapter = adapter
        setContentView(list)

        loadApps()
    }

    private fun buildRow(): LinearLayout {
        val outer = LinearLayout(this)
        outer.setPadding(dp(16), dp(4), dp(16), dp(4))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.background = Ui.rounded(Ui.CARD, 14, this, Ui.CARD_STROKE)
        row.setPadding(dp(14), dp(10), dp(14), dp(10))
        row.addView(ImageView(this), LinearLayout.LayoutParams(dp(40), dp(40)))
        val name = Ui.text(this, "", 16f)
        name.setPadding(dp(12), 0, dp(12), 0)
        row.addView(name, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(SwitchCompat(this))
        outer.addView(row, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return outer
    }

    private fun cardLp(l: Int, t: Int, r: Int, b: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).also { it.setMargins(dp(l), dp(t), dp(r), dp(b)) }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val items = pm.queryIntentActivities(intent, 0)
                .map {
                    AppItem(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm))
                }
                .filter { it.pkg != packageName }
                .distinctBy { it.pkg }
                .sortedBy { it.label.lowercase() }
            runOnUiThread {
                apps.clear()
                apps.addAll(items)
                adapter.notifyDataSetChanged()
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        if (!Store.isSetup(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            return
        }
        if (!Session.mainUnlocked) {
            startActivity(
                Intent(this, LockActivity::class.java).putExtra(LockActivity.EXTRA_SELF, true)
            )
            return
        }
        lockedSet.clear()
        lockedSet.addAll(Store.lockedApps(this))
        adapter.notifyDataSetChanged()
        refreshStatus()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) Session.mainUnlocked = false
    }

    // ---------- Status ----------

    private fun accEnabled(): Boolean {
        val s = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val full = ComponentName(this, LockService::class.java).flattenToString()
        val short = ComponentName(this, LockService::class.java).flattenToShortString()
        return s.split(':').any { it.equals(full, true) || it.equals(short, true) }
    }

    private fun refreshStatus() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        tvAcc.text = mark(accEnabled()) + " Accessibility Service (core lock engine)"
        tvOv.text = mark(Settings.canDrawOverlays(this)) + " Display over other apps"
        tvAdm.text = mark(dpm.isAdminActive(comp)) + " Device Admin (uninstall protection)"
        tvBat.text = mark(pm.isIgnoringBatteryOptimizations(packageName)) + " Battery optimization exemption"

        swBio.setOnCheckedChangeListener(null)
        swBio.isChecked = Store.biometric(this)
        swBio.setOnCheckedChangeListener { _, on ->
            if (on) {
                val ok = BiometricManager.from(this)
                    .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                    BiometricManager.BIOMETRIC_SUCCESS
                if (ok) {
                    Store.setBiometric(this, true)
                } else {
                    swBio.setOnCheckedChangeListener(null)
                    swBio.isChecked = false
                    refreshStatus()
                    Toast.makeText(
                        this, "No fingerprint set up on this phone", Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Store.setBiometric(this, false)
            }
        }
    }

    private fun mark(ok: Boolean): String = if (ok) "✅" else "❌"

    // ---------- Helpers ----------

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun statusRow(parent: LinearLayout, btnText: String, onClick: () -> Unit): TextView {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(4), 0, dp(4))
        val t = Ui.text(this, "", 14f)
        row.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val b = Ui.smallButton(this, btnText)
        b.setOnClickListener { onClick() }
        row.addView(b)
        parent.addView(row)
        return t
    }

    private fun openSettings(i: Intent) {
        // Settings is normally locked; allow a 30-second grace when opened from here
        Session.graceUntil = SystemClock.elapsedRealtime() + 30_000
        try {
            startActivity(i)
        } catch (_: Exception) {
            Toast.makeText(this, "Couldn't open", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmPause() {
        AlertDialog.Builder(this)
            .setTitle("Protection pause")
            .setMessage(
                "For 10 minutes, the locks on Settings / Play Store / Installer and Device Admin " +
                    "will be turned off so you can uninstall or disable the app. After 10 minutes " +
                    "the lock turns back on (you will need to re-enable Device Admin)."
            )
            .setPositiveButton("Pause") { _, _ -> pauseProtection() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pauseProtection() {
        Store.setPause(this, System.currentTimeMillis() + 10 * 60 * 1000L)
        try {
            val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.removeActiveAdmin(comp)
        } catch (_: Exception) {
        }
        Toast.makeText(this, "10-minute pause is on. You can uninstall now.", Toast.LENGTH_LONG).show()
        refreshStatus()
    }
}
