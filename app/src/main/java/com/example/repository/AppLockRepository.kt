package com.example.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.db.ActivityLogEntity
import com.example.db.AppLockDao
import com.example.db.ProtectedAppEntity
import com.example.db.SettingEntity
import com.example.db.UnlockSessionEntity
import com.example.model.ActivityLogItem
import com.example.model.AppInfo
import com.example.model.DashboardStats
import com.example.model.SecurityStatus
import com.example.security.SecurityManager
import com.example.util.BiometricHelper
import com.example.util.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppLockRepository(
    private val context: Context,
    private val dao: AppLockDao,
    private val securityManager: SecurityManager
) {

    // --- App Protection ---
    val protectedAppsFlow: Flow<List<ProtectedAppEntity>> = dao.getAllProtectedAppsFlow()

    suspend fun isAppProtected(packageName: String): Boolean = withContext(Dispatchers.IO) {
        dao.isAppProtected(packageName)
    }

    suspend fun setAppProtected(packageName: String, appName: String, protect: Boolean) = withContext(Dispatchers.IO) {
        if (protect) {
            dao.insertProtectedApp(
                ProtectedAppEntity(
                    packageName = packageName,
                    appName = appName,
                    enabled = true
                )
            )
            logEvent("APP_PROTECTED", packageName, true, "User protected $appName")
        } else {
            dao.deleteProtectedApp(packageName)
            dao.removeSession(packageName)
            logEvent("APP_UNPROTECTED", packageName, true, "User unprotected $appName")
        }
    }

    suspend fun clearAllProtected() = withContext(Dispatchers.IO) {
        dao.clearAllProtectedApps()
        dao.clearAllSessions()
        logEvent("RESET", null, true, "Cleared all protected apps")
    }

    // --- Installed Apps Scanner ---
    suspend fun getInstalledApps(includeSystem: Boolean = true): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val self = context.packageName
        val protectedSet = dao.getProtectedPackageNames().toSet()

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.PackageInfoFlags.of(0)
        } else {
            0
        }

        val packages = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(0)
            }
        } catch (_: Throwable) {
            emptyList()
        }

        val result = mutableListOf<AppInfo>()
        for (p in packages) {
            val appInfo = p.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystem && isSystem) continue
            val pkg = p.packageName
            if (pkg == self) continue

            val label = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Throwable) {
                pkg
            }

            result.add(
                AppInfo(
                    packageName = pkg,
                    label = label,
                    isSystem = isSystem,
                    isProtected = protectedSet.contains(pkg)
                )
            )
        }
        result.sortedWith(compareByDescending<AppInfo> { it.isProtected }.thenBy { it.label.lowercase() })
    }

    // --- Unlock Sessions ---
    suspend fun createUnlockSession(packageName: String, timeoutMs: Long = 60_000L) = withContext(Dispatchers.IO) {
        dao.createSession(
            UnlockSessionEntity(
                packageName = packageName,
                expiresAt = System.currentTimeMillis() + timeoutMs
            )
        )
    }

    suspend fun hasValidSession(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val exp = dao.getSessionExpiration(packageName) ?: return@withContext false
        val now = System.currentTimeMillis()
        if (exp <= now) {
            dao.removeSession(packageName)
            false
        } else {
            true
        }
    }

    suspend fun clearAllSessions() = withContext(Dispatchers.IO) {
        dao.clearAllSessions()
    }

    suspend fun lockAll() = withContext(Dispatchers.IO) {
        dao.clearAllSessions()
        logEvent("APP_LOCKED", null, true, "Lock All triggered")
    }

    suspend fun unlockAll(secret: String): Boolean = withContext(Dispatchers.IO) {
        if (!securityManager.verify(secret.toCharArray())) {
            securityManager.recordFailure()
            logEvent("PIN_FAILURE", null, false, "Unlock All failed verification")
            return@withContext false
        }
        securityManager.resetFailures()
        val timeout = getAutoLockTimeoutMs().takeIf { it > 0 } ?: 300_000L
        val pkgs = dao.getProtectedPackageNames()
        val now = System.currentTimeMillis()
        for (pkg in pkgs) {
            dao.createSession(UnlockSessionEntity(pkg, now + timeout))
        }
        logEvent("APP_UNLOCKED", null, true, "Unlock All successful")
        true
    }

    // --- Logs ---
    val logsFlow: Flow<List<ActivityLogItem>> = dao.getLogsFlow().map { list ->
        list.map {
            ActivityLogItem(
                id = it.id,
                eventType = it.eventType,
                packageName = it.packageName,
                timestamp = it.timestamp,
                success = it.success,
                details = it.details
            )
        }
    }

    suspend fun logEvent(type: String, pkg: String?, success: Boolean, details: String? = null) = withContext(Dispatchers.IO) {
        dao.insertLog(
            ActivityLogEntity(
                eventType = type,
                packageName = pkg,
                timestamp = System.currentTimeMillis(),
                success = success,
                details = details
            )
        )
    }

    suspend fun clearLogs(secret: String): Boolean = withContext(Dispatchers.IO) {
        if (!securityManager.verify(secret.toCharArray())) return@withContext false
        dao.clearAllLogs()
        logEvent("SETTINGS_CHANGED", null, true, "Activity logs cleared")
        true
    }

    // --- Settings & Security State ---
    suspend fun getSetting(key: String, default: String): String = withContext(Dispatchers.IO) {
        dao.getSetting(key) ?: default
    }

    suspend fun setSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        dao.setSetting(SettingEntity(key, value))
    }

    suspend fun isBiometricEnabled(): Boolean = getSetting("biometric_enabled", "false") == "true"
    suspend fun setBiometricEnabled(enabled: Boolean) = setSetting("biometric_enabled", enabled.toString())

    suspend fun isLockAfterScreenOff(): Boolean = getSetting("lock_screen_off", "true") == "true"
    suspend fun setLockAfterScreenOff(enabled: Boolean) = setSetting("lock_screen_off", enabled.toString())

    suspend fun isLockOnBoot(): Boolean = getSetting("lock_boot", "true") == "true"
    suspend fun setLockOnBoot(enabled: Boolean) = setSetting("lock_boot", enabled.toString())

    suspend fun getAutoLockTimeoutMs(): Long = getSetting("autolock_timeout", "60000").toLongOrNull() ?: 60000L
    suspend fun setAutoLockTimeoutMs(ms: Long) = setSetting("autolock_timeout", ms.toString())

    suspend fun isOnboardingCompleted(): Boolean = getSetting("onboarding_done", "false") == "true"
    suspend fun setOnboardingCompleted(done: Boolean) = setSetting("onboarding_done", done.toString())

    suspend fun getTheme(): String = getSetting("theme", "dark")
    suspend fun setTheme(theme: String) = setSetting("theme", theme)

    suspend fun getStats(): DashboardStats = withContext(Dispatchers.IO) {
        val pkgs = dao.getProtectedPackageNames()
        val protectedCount = pkgs.size
        var lockedNow = 0
        for (pkg in pkgs) {
            val exp = dao.getSessionExpiration(pkg)
            if (exp == null || exp <= System.currentTimeMillis()) {
                lockedNow++
            }
        }
        val oneDayAgo = System.currentTimeMillis() - 24 * 3600_000L
        val unlocksToday = dao.getUnlockCountSince(oneDayAgo)
        val failedToday = dao.getFailedCountSince(oneDayAgo)

        DashboardStats(
            protectedCount = protectedCount,
            lockedNow = lockedNow,
            unlocksToday = unlocksToday,
            failedToday = failedToday
        )
    }

    suspend fun getSecurityStatus(): SecurityStatus = withContext(Dispatchers.IO) {
        val pinSet = securityManager.isCredentialSet()
        val mode = securityManager.credentialMode()
        val bioAvail = BiometricHelper.isHardwareAvailable(context)
        val bioEnrolled = isBiometricEnabled()
        val accEnabled = PermissionManager.isAccessibilityEnabled(context)
        val notifEnabled = PermissionManager.isNotificationEnabled(context)
        val bootProt = isLockOnBoot()
        val devAdmin = PermissionManager.isDeviceAdminEnabled(context)
        val battOpt = PermissionManager.isBatteryOptimized(context)
        val cooldown = securityManager.cooldownRemainingMs()
        val fails = securityManager.failedAttemptCount()

        var score = 30
        if (pinSet) score += 25
        if (bioAvail && bioEnrolled) score += 15
        if (accEnabled) score += 20
        if (bootProt) score += 5
        if (!battOpt) score += 5

        SecurityStatus(
            pinSet = pinSet,
            credentialMode = mode,
            biometricEnabled = bioEnrolled,
            biometricAvailable = bioAvail,
            accessibilityEnabled = accEnabled,
            notificationsEnabled = notifEnabled,
            bootProtection = bootProt,
            deviceAdminEnabled = devAdmin,
            batteryOptimized = battOpt,
            cooldownMs = cooldown,
            failedAttempts = fails,
            score = score.coerceIn(0, 100)
        )
    }

    suspend fun resetAll(secret: String): Boolean = withContext(Dispatchers.IO) {
        if (!securityManager.verify(secret.toCharArray())) return@withContext false
        dao.clearAllProtectedApps()
        dao.clearAllSessions()
        dao.clearAllLogs()
        dao.clearAllSettings()
        securityManager.clear()
        true
    }
}
