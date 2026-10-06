package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.LockoraApplication
import com.example.model.ActivityLogItem
import com.example.model.AppInfo
import com.example.model.DashboardStats
import com.example.model.SecurityStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LockoraApplication
    private val repo = app.repository
    private val secManager = app.securityManager

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    private val _stats = MutableStateFlow(DashboardStats(0, 0, 0, 0))
    val stats: StateFlow<DashboardStats> = _stats.asStateFlow()

    private val _securityStatus = MutableStateFlow(
        SecurityStatus(
            pinSet = false,
            credentialMode = "pin",
            biometricEnabled = false,
            biometricAvailable = false,
            accessibilityEnabled = false,
            notificationsEnabled = false,
            bootProtection = true,
            deviceAdminEnabled = false,
            batteryOptimized = false,
            cooldownMs = 0L,
            failedAttempts = 0,
            score = 30
        )
    )
    val securityStatus: StateFlow<SecurityStatus> = _securityStatus.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<ActivityLogItem>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLogItem>> = _activityLogs.asStateFlow()

    private val _isOnboardingDone = MutableStateFlow(true)
    val isOnboardingDone: StateFlow<Boolean> = _isOnboardingDone.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    init {
        refreshAll()
        viewModelScope.launch {
            repo.logsFlow.collect { logs ->
                _activityLogs.value = logs
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isOnboardingDone.value = secManager.isCredentialSet() && repo.isOnboardingCompleted()
            _stats.value = repo.getStats()
            _securityStatus.value = repo.getSecurityStatus()
            loadApps()
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _installedApps.value = repo.getInstalledApps(includeSystem = true)
            _isLoadingApps.value = false
        }
    }

    fun toggleAppProtection(packageName: String, label: String, protect: Boolean) {
        viewModelScope.launch {
            repo.setAppProtected(packageName, label, protect)
            // Update local state instantly
            _installedApps.value = _installedApps.value.map {
                if (it.packageName == packageName) it.copy(isProtected = protect) else it
            }
            _stats.value = repo.getStats()
            _toastEvent.emit(if (protect) "Protected $label" else "Unprotected $label")
        }
    }

    fun lockAll() {
        viewModelScope.launch {
            repo.lockAll()
            _stats.value = repo.getStats()
            _toastEvent.emit("All protected apps locked")
        }
    }

    fun tempUnlock(secret: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repo.unlockAll(secret)
            if (ok) {
                _stats.value = repo.getStats()
                _toastEvent.emit("Protected apps temporarily unlocked")
            } else {
                _toastEvent.emit("Invalid credentials")
            }
            onResult(ok)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setBiometricEnabled(enabled)
            _securityStatus.value = repo.getSecurityStatus()
            _toastEvent.emit("Biometric unlock updated")
        }
    }

    fun setAutoLockTimeout(timeoutMs: Long) {
        viewModelScope.launch {
            repo.setAutoLockTimeoutMs(timeoutMs)
            _toastEvent.emit("Auto-lock timeout updated")
        }
    }

    fun setLockOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            repo.setLockOnBoot(enabled)
            _securityStatus.value = repo.getSecurityStatus()
        }
    }

    fun setLockAfterScreenOff(enabled: Boolean) {
        viewModelScope.launch {
            repo.setLockAfterScreenOff(enabled)
        }
    }

    fun clearActivityLogs(secret: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repo.clearLogs(secret)
            if (ok) {
                _toastEvent.emit("Activity history cleared")
            } else {
                _toastEvent.emit("Incorrect credentials")
            }
            onResult(ok)
        }
    }

    fun resetLockora(secret: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repo.resetAll(secret)
            if (ok) {
                _isOnboardingDone.value = false
                refreshAll()
                _toastEvent.emit("Lockora has been reset")
            } else {
                _toastEvent.emit("Incorrect credentials")
            }
            onResult(ok)
        }
    }

    fun setupMasterCredential(secret: String, mode: String, enableBio: Boolean) {
        viewModelScope.launch {
            secManager.setCredential(secret.toCharArray(), mode)
            if (enableBio) {
                repo.setBiometricEnabled(true)
            }
            repo.setOnboardingCompleted(true)
            _isOnboardingDone.value = true
            refreshAll()
            _toastEvent.emit("Master security setup complete")
        }
    }

    fun changeMasterCredential(currentSecret: String, newSecret: String, mode: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (!secManager.verify(currentSecret.toCharArray())) {
                _toastEvent.emit("Current credential is wrong")
                onResult(false)
                return@launch
            }
            secManager.setCredential(newSecret.toCharArray(), mode)
            _toastEvent.emit("Credential changed successfully")
            onResult(true)
        }
    }
}
