package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.ui.components.AppIconView
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.LockoraTheme
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.NeonVioletGlow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.BiometricHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LockScreenActivity : FragmentActivity() {

    private var targetPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        targetPackage = intent.getStringExtra(EXTRA_TARGET_PKG)
        if (targetPackage.isNullOrEmpty()) {
            finish()
            return
        }

        val app = applicationContext as LockoraApplication
        val secManager = app.securityManager
        val repo = app.repository

        lifecycleScope.launch(Dispatchers.Main) {
            val bioAllowed = repo.isBiometricEnabled()
            val bioAvail = BiometricHelper.isHardwareAvailable(this@LockScreenActivity)

            if (bioAllowed && bioAvail) {
                showBiometricPrompt()
            }
        }

        setContent {
            LockoraTheme {
                val appLabel = remember(targetPackage) {
                    try {
                        val pm = packageManager
                        val info = pm.getApplicationInfo(targetPackage!!, 0)
                        pm.getApplicationLabel(info).toString()
                    } catch (_: Throwable) {
                        targetPackage ?: "Protected App"
                    }
                }

                val mode = secManager.credentialMode()
                val bioAvail = BiometricHelper.isHardwareAvailable(this)

                LockScreenUI(
                    packageName = targetPackage!!,
                    appName = appLabel,
                    isPinMode = mode == "pin",
                    canUseBiometrics = bioAvail,
                    onSubmitSecret = { secret, onError ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            if (secManager.cooldownRemainingMs() > 0) {
                                onError("Cooldown lockout active")
                                return@launch
                            }
                            val ok = secManager.verify(secret.toCharArray())
                            if (ok) {
                                secManager.resetFailures()
                                unlockAndResume()
                            } else {
                                secManager.recordFailure()
                                repo.logEvent("PIN_FAILURE", targetPackage, false, "Invalid attempt")
                                onError("Incorrect credential")
                            }
                        }
                    },
                    onRequestBiometrics = {
                        showBiometricPrompt()
                    },
                    onCancel = {
                        cancelAndGoHome()
                    }
                )
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val app = applicationContext as LockoraApplication
                    lifecycleScope.launch(Dispatchers.IO) {
                        app.repository.logEvent("BIOMETRIC_SUCCESS", targetPackage, true)
                        unlockAndResume()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    val app = applicationContext as LockoraApplication
                    lifecycleScope.launch(Dispatchers.IO) {
                        app.repository.logEvent("BIOMETRIC_FAILURE", targetPackage, false, errString.toString())
                    }
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock $targetPackage")
            .setSubtitle("Authenticate via Biometric Scan")
            .setNegativeButtonText("Use PIN/Password")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        try {
            prompt.authenticate(promptInfo)
        } catch (_: Throwable) { }
    }

    private fun unlockAndResume() {
        val pkg = targetPackage ?: return
        val app = applicationContext as LockoraApplication
        lifecycleScope.launch(Dispatchers.IO) {
            val timeout = app.repository.getAutoLockTimeoutMs()
            app.repository.createUnlockSession(pkg, timeout)
            app.repository.logEvent("APP_UNLOCKED", pkg, true)

            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launchIntent)
                }
            } catch (_: Throwable) { }

            finish()
        }
    }

    private fun cancelAndGoHome() {
        val app = applicationContext as LockoraApplication
        lifecycleScope.launch(Dispatchers.IO) {
            app.repository.logEvent("APP_LOCKED", targetPackage, false, "Cancelled by user")
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(homeIntent)
        } catch (_: Throwable) { }
        finish()
    }

    override fun onBackPressed() {
        cancelAndGoHome()
    }

    companion object {
        const val EXTRA_TARGET_PKG = "extra_target_package"
    }
}

@Composable
fun LockScreenUI(
    packageName: String,
    appName: String,
    isPinMode: Boolean,
    canUseBiometrics: Boolean,
    onSubmitSecret: (String, (String) -> Unit) -> Unit,
    onRequestBiometrics: () -> Unit,
    onCancel: () -> Unit
) {
    var pinValue by remember { mutableStateOf("") }
    var passwordValue by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(24.dp)
            .testTag("lock_screen_ui"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppIconView(
            packageName = packageName,
            fallbackLabel = appName,
            size = 72.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "LOCKORA PROTECTS",
            style = MaterialTheme.typography.labelSmall,
            color = NeonVioletGlow,
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Authentication required to open this app",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isPinMode) {
            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < pinValue.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) NeonViolet else CyberCardSurface)
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // PIN Pad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        row.forEach { key ->
                            PinKeyButton(
                                text = key,
                                canUseBiometrics = canUseBiometrics,
                                onClick = {
                                    when (key) {
                                        "DEL" -> {
                                            if (pinValue.isNotEmpty()) {
                                                pinValue = pinValue.dropLast(1)
                                                errorMessage = null
                                            }
                                        }
                                        "BIO" -> {
                                            if (canUseBiometrics) onRequestBiometrics()
                                        }
                                        else -> {
                                            if (pinValue.length < 8) {
                                                pinValue += key
                                                errorMessage = null
                                                if (pinValue.length >= 4) {
                                                    onSubmitSecret(pinValue) { err ->
                                                        errorMessage = err
                                                        pinValue = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // Password Field
            OutlinedTextField(
                value = passwordValue,
                onValueChange = {
                    passwordValue = it
                    errorMessage = null
                },
                placeholder = { Text("Enter Master Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    onSubmitSecret(passwordValue) { err ->
                        errorMessage = err
                        passwordValue = ""
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Unlock")
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(errorMessage!!, color = NeonCoral, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (canUseBiometrics && !isPinMode) {
                OutlinedButton(
                    onClick = onRequestBiometrics,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = NeonEmerald)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Biometric", color = TextPrimary)
                }
            }

            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    }
}

@Composable
fun PinKeyButton(
    text: String,
    canUseBiometrics: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(CyberCardSurface)
            .clickable(onClick = onClick)
            .testTag("pin_key_$text"),
        contentAlignment = Alignment.Center
    ) {
        when (text) {
            "DEL" -> Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = TextPrimary)
            "BIO" -> {
                if (canUseBiometrics) {
                    Icon(Icons.Default.Fingerprint, contentDescription = "Biometric", tint = NeonEmerald)
                }
            }
            else -> Text(text = text, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}
