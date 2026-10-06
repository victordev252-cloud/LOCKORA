package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.LockScreenActivity
import com.example.LockoraApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppLockAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastForeground: String? = null
    private var lastLockedAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        if (e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            e.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return

        val pkg = e.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg.startsWith("com.android.systemui")) return
        if (pkg == "android") return
        if (pkg == lastForeground) return
        lastForeground = pkg

        val app = applicationContext as? LockoraApplication ?: return
        val repo = app.repository

        serviceScope.launch(Dispatchers.IO) {
            if (!repo.isAppProtected(pkg)) return@launch
            if (repo.hasValidSession(pkg)) return@launch

            val now = System.currentTimeMillis()
            if (now - lastLockedAt < 800) return@launch
            lastLockedAt = now

            repo.logEvent("APP_LOCKED", pkg, success = true, details = "Locked upon foreground transition")

            val intent = Intent(this@AppLockAccessibilityService, LockScreenActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                putExtra(LockScreenActivity.EXTRA_TARGET_PKG, pkg)
            }
            try {
                startActivity(intent)
            } catch (_: Throwable) { }
        }
    }

    override fun onInterrupt() { }

    override fun onServiceConnected() {
        super.onServiceConnected()
        lastForeground = null
    }
}
