package com.myapplock

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class AdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Turning off Device Admin allows Lockify to be uninstalled and reduces your protection."
    }
}
