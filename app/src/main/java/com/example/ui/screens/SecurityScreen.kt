package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SecurityScoreGauge
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PermissionManager
import com.example.viewmodel.MainViewModel

@Composable
fun SecurityScreen(
    viewModel: MainViewModel
) {
    val secStatus by viewModel.securityStatus.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(horizontal = 18.dp)
            .testTag("security_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Security Center",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Hardware diagnostics and permission compliance",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Score gauge
            SecurityScoreGauge(score = secStatus.score)

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "System Security Score: ${secStatus.score}/100",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SECURITY INTEGRITY CHECKS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            SecurityCheckRow(
                title = "Master Cryptographic Secret",
                subtitle = "PBKDF2-HMAC-SHA256 • 120,000 rounds",
                passed = secStatus.pinSet,
                statusText = if (secStatus.pinSet) "ACTIVE (${secStatus.credentialMode.uppercase()})" else "NOT CONFIGURED"
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SecurityCheckRow(
                title = "Accessibility Inspection Service",
                subtitle = "Required to detect protected apps on foreground",
                passed = secStatus.accessibilityEnabled,
                statusText = if (secStatus.accessibilityEnabled) "RUNNING" else "DISABLED",
                actionLabel = if (!secStatus.accessibilityEnabled) "Enable" else null,
                onAction = { PermissionManager.openAccessibilitySettings(context) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SecurityCheckRow(
                title = "Biometric Authentication",
                subtitle = "Fingerprint / Class 3 biometric scanner",
                passed = secStatus.biometricEnabled && secStatus.biometricAvailable,
                statusText = if (secStatus.biometricAvailable) {
                    if (secStatus.biometricEnabled) "ENABLED" else "DISABLED"
                } else "NO HARDWARE"
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SecurityCheckRow(
                title = "Battery Optimization Exemption",
                subtitle = "Prevents OS from killing background lock service",
                passed = !secStatus.batteryOptimized,
                statusText = if (!secStatus.batteryOptimized) "EXEMPTED (STABLE)" else "BATTERY OPTIMIZED",
                actionLabel = if (secStatus.batteryOptimized) "Exempt" else null,
                onAction = { PermissionManager.openBatteryOptimizationSettings(context) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SecurityCheckRow(
                title = "Boot & Restart Resilience",
                subtitle = "Restores locks immediately upon phone reboot",
                passed = secStatus.bootProtection,
                statusText = if (secStatus.bootProtection) "ARMED" else "DISABLED"
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SecurityCheckRow(
                title = "Device Admin Hardening",
                subtitle = "Provides elevated policy compliance",
                passed = secStatus.deviceAdminEnabled,
                statusText = if (secStatus.deviceAdminEnabled) "GRANTED" else "OPTIONAL",
                actionLabel = if (!secStatus.deviceAdminEnabled) "Configure" else null,
                onAction = { PermissionManager.openDeviceAdminSettings(context) }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            // Failed Attempts card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Brute-Force Attack Defense", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Consecutive failed attempts: ${secStatus.failedAttempts}",
                        color = if (secStatus.failedAttempts > 0) NeonCoral else TextSecondary,
                        fontSize = 13.sp
                    )
                    if (secStatus.cooldownMs > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Penalty Lockout Active: ${(secStatus.cooldownMs / 1000)}s remaining",
                            color = NeonCoral,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
fun SecurityCheckRow(
    title: String,
    subtitle: String,
    passed: Boolean,
    statusText: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCardSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = statusText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (passed) NeonEmerald else NeonCoral
                )
                if (actionLabel != null && onAction != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onAction,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(actionLabel, fontSize = 11.sp, color = NeonViolet)
                    }
                }
            }
        }
    }
}
