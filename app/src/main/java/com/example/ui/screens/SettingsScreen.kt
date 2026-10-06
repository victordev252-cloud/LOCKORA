package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.LockoraApplication
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberDarkCard
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val secStatus by viewModel.securityStatus.collectAsState()

    var showChangePinDialog by remember { mutableStateOf(false) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var changeError by remember { mutableStateOf<String?>(null) }

    var showResetDialog by remember { mutableStateOf(false) }
    var resetPin by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(horizontal = 18.dp)
            .testTag("settings_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "Preferences, timeout configurations, and master secret",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "AUTHENTICATION & RE-LOCK",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardSurface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Change Master Secret", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Update your master PIN or password", fontSize = 12.sp, color = TextSecondary)
                    }
                    OutlinedButton(
                        onClick = { showChangePinDialog = true },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Change", color = NeonViolet)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardSurface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Biometric Authentication", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Unlock protected apps via fingerprint or face", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = secStatus.biometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = NeonViolet,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberDarkCard
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardSurface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lock on Device Boot", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Clears temporary unlock sessions immediately after restart", fontSize = 12.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = secStatus.bootProtection,
                        onCheckedChange = { viewModel.setLockOnBoot(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = NeonViolet,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = CyberDarkCard
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ABOUT & RESET",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Lockora Engine v${LockoraApplication.VERSION_NAME}", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Zero-telemetry offline Android App Lock. No internet permissions, no external logging, 100% on-device cryptography.", fontSize = 13.sp, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        item {
            Button(
                onClick = { showResetDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Reset Lockora", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePinDialog = false
                currentPin = ""
                newPin = ""
                confirmNewPin = ""
                changeError = null
            },
            title = { Text("Update Master Secret", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { currentPin = it },
                        label = { Text("Current PIN / Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = { Text("New PIN (min 4 digits) or Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmNewPin,
                        onValueChange = { confirmNewPin = it },
                        label = { Text("Confirm New Secret") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (changeError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(changeError!!, color = NeonCoral, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length < 4) {
                            changeError = "Secret must be at least 4 characters/digits."
                            return@Button
                        }
                        if (newPin != confirmNewPin) {
                            changeError = "New secret confirmation does not match."
                            return@Button
                        }
                        val mode = if (newPin.all { it.isDigit() }) "pin" else "password"
                        viewModel.changeMasterCredential(currentPin, newPin, mode) { ok ->
                            if (ok) {
                                showChangePinDialog = false
                                currentPin = ""
                                newPin = ""
                                confirmNewPin = ""
                                changeError = null
                            } else {
                                changeError = "Current secret is incorrect."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonViolet)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showChangePinDialog = false
                    changeError = null
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberCardSurface
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = {
                showResetDialog = false
                resetPin = ""
            },
            title = { Text("Wipe All Data & Reset", color = NeonCoral) },
            text = {
                Column {
                    Text("This will remove all protected app configurations, clear access logs, and delete your master secret. Enter master PIN/password to proceed:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetPin,
                        onValueChange = { resetPin = it },
                        label = { Text("Master Credential") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetLockora(resetPin) { ok ->
                            if (ok) {
                                showResetDialog = false
                                resetPin = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral)
                ) {
                    Text("Confirm Factory Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberCardSurface
        )
    }
}
