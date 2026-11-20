package com.myapplock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * Core lock engine. Detects which app came to the front and shows LockActivity if it is locked.
 * The system keeps the accessibility service alive and restarts it on boot/after a crash.
 */
class LockService : AccessibilityService() {

    companion object {
        /** Packages that are always locked to prevent stop/disable/uninstall/clear-data. */
        val BUILTIN = setOf(
            "com.android.settings",
            "com.android.vending",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.miui.packageinstaller",
            "com.miui.securitycenter"
        )

        /** Events from these packages are ignored (keyboard, notifications, system dialogs). */
        val IGNORE = setOf(
            "com.android.systemui",
            "android",
            "com.android.intentresolver",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller"
        )
    }

    private var lastPkg: String? = null
    private var lastLaunch = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Pre-warm the keystore so the first lock is not delayed
        try {
            Store.isSetup(this)
            Store.lockedApps(this)
        } catch (_: Exception) {
        }
    }

    private fun isIme(pkg: String): Boolean {
        return try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.enabledInputMethodList.any { it.packageName == pkg }
        } catch (_: Exception) {
            false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val now = SystemClock.elapsedRealtime()

        // Lockify's own screen: ignored while the lock screen is showing, otherwise it counts as "moved to another app"
        if (pkg == packageName) {
            if (!Session.lockShowing && now - Session.lockClosedAt > 1500) {
                Session.unlockedPkg = null
            }
            return
        }
        if (pkg in IGNORE || isIme(pkg)) return

        val isBuiltin = pkg in BUILTIN

        // fail-closed: if data is wiped (no setup), Settings/Store/Installer stay blocked
        if (!Store.isSetup(this)) {
            if (isBuiltin) performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }

        val builtinActive = System.currentTimeMillis() > Store.pauseUntil(this) &&
            now > Session.graceUntil
        val shouldLock = pkg in Store.lockedApps(this) || (isBuiltin && builtinActive)

        if (!shouldLock) {
            Session.unlockedPkg = null
            return
        }
        if (Session.unlockedPkg == pkg) return

        Session.unlockedPkg = null
        if (Session.lockShowing && Session.lockTarget == pkg) return
        if (pkg == lastPkg && now - lastLaunch < 800) return
        lastPkg = pkg
        lastLaunch = now

        val i = Intent(this, LockActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            .putExtra(LockActivity.EXTRA_PKG, pkg)
        try {
            startActivity(i)
        } catch (_: Exception) {
            // Last resort: send the user home from the app
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onInterrupt() {}
}
