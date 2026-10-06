package com.example.model

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val isProtected: Boolean = false
)

data class SecurityStatus(
    val pinSet: Boolean,
    val credentialMode: String,
    val biometricEnabled: Boolean,
    val biometricAvailable: Boolean,
    val accessibilityEnabled: Boolean,
    val notificationsEnabled: Boolean,
    val bootProtection: Boolean,
    val deviceAdminEnabled: Boolean,
    val batteryOptimized: Boolean,
    val cooldownMs: Long,
    val failedAttempts: Int,
    val score: Int
)

data class ActivityLogItem(
    val id: Long,
    val eventType: String,
    val packageName: String?,
    val timestamp: Long,
    val success: Boolean,
    val details: String?
)

data class DashboardStats(
    val protectedCount: Int,
    val lockedNow: Int,
    val unlocksToday: Int,
    val failedToday: Int
)
