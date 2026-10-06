package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.LockoraApplication
import com.example.util.NotificationHelper
import com.example.util.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LockoraDeviceAdminReceiver : DeviceAdminReceiver()

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_PRESENT -> {
                val app = context.applicationContext as LockoraApplication
                CoroutineScope(Dispatchers.IO).launch {
                    app.repository.clearAllSessions()
                    if (!PermissionManager.isAccessibilityEnabled(context)) {
                        NotificationHelper.notify(
                            context,
                            "Lockora App Lock",
                            "Accessibility service is inactive. Please tap to enable protection.",
                            1002
                        )
                    }
                }
            }
        }
    }
}
