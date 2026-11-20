package com.myapplock

/** In-memory state only (resets when the process dies, which is the safe side). */
object Session {
    /** The app that was just unlocked; becomes null once another app is opened. */
    @Volatile var unlockedPkg: String? = null

    /** Whether Lockify's own screen is unlocked. */
    @Volatile var mainUnlocked = false

    @Volatile var lockShowing = false
    @Volatile var lockTarget: String? = null
    @Volatile var lockClosedAt = 0L

    /** Until this time (elapsedRealtime) Settings etc. are not locked, so permissions can be enabled from the app. */
    @Volatile var graceUntil = 0L
}
