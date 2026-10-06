package com.example.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "protected_apps")
data class ProtectedAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val enabled: Boolean = true,
    val biometricAllowed: Boolean = true,
    val pinAllowed: Boolean = true,
    val autoLockMode: String = "global",
    val sessionTimeout: Long = -1L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val eventType: String,
    val packageName: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean,
    val details: String?
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "unlock_sessions")
data class UnlockSessionEntity(
    @PrimaryKey val packageName: String,
    val expiresAt: Long
)

@Dao
interface AppLockDao {
    // Protected Apps
    @Query("SELECT * FROM protected_apps WHERE enabled = 1")
    fun getAllProtectedAppsFlow(): Flow<List<ProtectedAppEntity>>

    @Query("SELECT packageName FROM protected_apps WHERE enabled = 1")
    suspend fun getProtectedPackageNames(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM protected_apps WHERE packageName = :packageName AND enabled = 1)")
    suspend fun isAppProtected(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProtectedApp(app: ProtectedAppEntity)

    @Query("DELETE FROM protected_apps WHERE packageName = :packageName")
    suspend fun deleteProtectedApp(packageName: String)

    @Query("DELETE FROM protected_apps")
    suspend fun clearAllProtectedApps()

    // Activity Logs
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsFlow(limit: Int = 300): Flow<List<ActivityLogEntity>>

    @Insert
    suspend fun insertLog(log: ActivityLogEntity)

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM activity_logs WHERE success = 0 AND timestamp >= :sinceTimestamp")
    suspend fun getFailedCountSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM activity_logs WHERE eventType IN ('APP_UNLOCKED','PIN_SUCCESS','BIOMETRIC_SUCCESS') AND success = 1 AND timestamp >= :sinceTimestamp")
    suspend fun getUnlockCountSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM activity_logs WHERE packageName = :packageName AND eventType = 'APP_UNLOCKED'")
    suspend fun getUnlockCountForPackage(packageName: String): Int

    @Query("SELECT timestamp FROM activity_logs WHERE packageName = :packageName AND eventType = 'APP_UNLOCKED' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastUnlockTimestamp(packageName: String): Long?

    // Settings
    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): String?

    @Query("SELECT * FROM settings")
    fun getAllSettingsFlow(): Flow<List<SettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)

    @Query("DELETE FROM settings")
    suspend fun clearAllSettings()

    // Unlock Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createSession(session: UnlockSessionEntity)

    @Query("SELECT expiresAt FROM unlock_sessions WHERE packageName = :packageName")
    suspend fun getSessionExpiration(packageName: String): Long?

    @Query("DELETE FROM unlock_sessions WHERE packageName = :packageName")
    suspend fun removeSession(packageName: String)

    @Query("DELETE FROM unlock_sessions WHERE expiresAt <= :now")
    suspend fun pruneExpiredSessions(now: Long)

    @Query("DELETE FROM unlock_sessions")
    suspend fun clearAllSessions()
}

@Database(
    entities = [
        ProtectedAppEntity::class,
        ActivityLogEntity::class,
        SettingEntity::class,
        UnlockSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LockoraDatabase : RoomDatabase() {
    abstract fun dao(): AppLockDao
}
