package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager as AndroidBiometricManager

object BiometricHelper {

    fun isHardwareAvailable(context: Context): Boolean {
        val bm = AndroidBiometricManager.from(context)
        return bm.canAuthenticate(AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                AndroidBiometricManager.BIOMETRIC_SUCCESS
    }

    fun isEnrolled(context: Context): Boolean {
        val bm = AndroidBiometricManager.from(context)
        val res = bm.canAuthenticate(AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK)
        return res != AndroidBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED &&
                res != AndroidBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    }

    fun getStatusString(context: Context): String {
        val bm = AndroidBiometricManager.from(context)
        return when (bm.canAuthenticate(AndroidBiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            AndroidBiometricManager.BIOMETRIC_SUCCESS -> "Available & enrolled"
            AndroidBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Not enrolled in device settings"
            AndroidBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware"
            AndroidBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Hardware unavailable"
            else -> "Not available"
        }
    }
}
