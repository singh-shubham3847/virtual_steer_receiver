package com.example.virtual_steer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtual_steer.model.PedalResponseCurve
import com.example.virtual_steer.ui.components.*
import com.example.virtual_steer.ui.theme.*
import com.example.virtual_steer.viewmodel.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onCalibrationClick: () -> Unit,
    onDiagnosticsClick: () -> Unit,
    onCustomizeLayoutClick: () -> Unit
) {
    val config by viewModel.config.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "TUNING MENU",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("< BACK", color = AccentYellow, fontFamily = FontFamily.Monospace)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CarbonDark,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = CarbonDark
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            
            // STEERING
            RacingHeader("Steering")
            TuningCard("Core Handling") {
                RacingSlider(
                    label = "Sensitivity",
                    value = config.steering.sensitivity,
                    onValueChange = { viewModel.updateSteering { s -> s.copy(sensitivity = it) } },
                    valueRange = 0.5f..2.0f,
                    displayValue = String.format(Locale.US, "%.0f%%", config.steering.sensitivity * 100)
                )
                RacingSlider(
                    label = "Response Curve",
                    value = config.steering.responseCurve,
                    onValueChange = { viewModel.updateSteering { s -> s.copy(responseCurve = it) } },
                    valueRange = 0.5f..3.0f,
                    displayValue = String.format(Locale.US, "%.1f", config.steering.responseCurve)
                )
                RacingSlider(
                    label = "Max Angle",
                    value = config.steering.maxAngle,
                    onValueChange = { viewModel.updateSteering { s -> s.copy(maxAngle = it) } },
                    valueRange = 15f..180f,
                    displayValue = String.format(Locale.US, "%.0f°", config.steering.maxAngle)
                )
                RacingSlider(
                    label = "Dead Zone",
                    value = config.steering.deadZone,
                    onValueChange = { viewModel.updateSteering { s -> s.copy(deadZone = it) } },
                    valueRange = 0f..0.2f,
                    displayValue = String.format(Locale.US, "%.1f%%", config.steering.deadZone * 100)
                )
                RacingSwitch(
                    label = "Invert Steering",
                    checked = config.steering.invert,
                    onCheckedChange = { viewModel.updateSteering { s -> s.copy(invert = it) } }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TuningCard("Sensor Source") {
                RacingRadioButton(
                    label = "Rotation Vector (Gyroscope)",
                    selected = config.steering.useRotationVector,
                    onClick = { viewModel.updateSteering { s -> s.copy(useRotationVector = true) } }
                )
                RacingRadioButton(
                    label = "Accelerometer (Gravity tilt fallback)",
                    selected = !config.steering.useRotationVector,
                    onClick = { viewModel.updateSteering { s -> s.copy(useRotationVector = false) } }
                )
            }

            // PEDALS
            RacingHeader("Pedals")
            TuningCard("Throttle Response") {
                PedalResponseCurve.entries.forEach { curve ->
                    RacingRadioButton(
                        label = curve.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = config.pedals.throttleCurve == curve,
                        onClick = { viewModel.updatePedals { p -> p.copy(throttleCurve = curve) } }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TuningCard("Brake Response") {
                PedalResponseCurve.entries.forEach { curve ->
                    RacingRadioButton(
                        label = curve.name.lowercase().replaceFirstChar { it.uppercase() },
                        selected = config.pedals.brakeCurve == curve,
                        onClick = { viewModel.updatePedals { p -> p.copy(brakeCurve = curve) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TuningCard("Pedal Tuning") {
                RacingSlider(
                    label = "Pedal Dead Zone",
                    value = config.pedals.deadZone,
                    onValueChange = { viewModel.updatePedals { p -> p.copy(deadZone = it) } },
                    valueRange = 0f..0.2f,
                    displayValue = String.format(Locale.US, "%.1f%%", config.pedals.deadZone * 100)
                )
                RacingSlider(
                    label = "Pedal Smoothing",
                    value = config.pedals.smoothing,
                    onValueChange = { viewModel.updatePedals { p -> p.copy(smoothing = it) } },
                    valueRange = 0.05f..0.5f,
                    displayValue = String.format(Locale.US, "%.0f%%", config.pedals.smoothing * 100)
                )
                RacingSwitch(
                    label = "Invert Pedals (Throttle/Brake)",
                    checked = config.pedals.invert,
                    onCheckedChange = { viewModel.updatePedals { p -> p.copy(invert = it) } }
                )
            }

            // NETWORK
            RacingHeader("Network")
            TuningCard("Connection") {
                RacingSwitch(
                    label = "Auto Discovery",
                    checked = config.network.autoDiscover,
                    onCheckedChange = { viewModel.updateNetwork { n -> n.copy(autoDiscover = it) } }
                )
                
                if (!config.network.autoDiscover) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "MANUAL PC IP",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = config.network.manualIp,
                        onValueChange = { viewModel.updateNetwork { n -> n.copy(manualIp = it) } },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThrottleGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
                
                RacingSlider(
                    label = "Packet Rate (Hz)",
                    value = config.network.packetRate.toFloat(),
                    onValueChange = { viewModel.updateNetwork { n -> n.copy(packetRate = it.toInt()) } },
                    valueRange = 10f..200f,
                    displayValue = "${config.network.packetRate} Hz"
                )
            }

            // INTERFACE
            RacingHeader("Interface")
            TuningCard("Appearance") {
                RacingSwitch(
                    label = "Dark Theme",
                    checked = config.ui.darkTheme,
                    onCheckedChange = { viewModel.updateUI { u -> u.copy(darkTheme = it) } }
                )
                RacingSwitch(
                    label = "Show Telemetry",
                    checked = config.ui.showTelemetry,
                    onCheckedChange = { viewModel.updateUI { u -> u.copy(showTelemetry = it) } }
                )
                RacingSwitch(
                    label = "Show FPS",
                    checked = config.ui.showFps,
                    onCheckedChange = { viewModel.updateUI { u -> u.copy(showFps = it) } }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TuningCard("Layout Customization") {
                RacingSwitch(
                    label = "Show Radio Button",
                    checked = config.ui.showRadio,
                    onCheckedChange = { viewModel.updateUI { u -> u.copy(showRadio = it) } }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onCustomizeLayoutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = GridPanelBg),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MetallicBorder, RoundedCornerShape(4.dp))
                ) {
                    Text("🔧 ADJUST BUTTON POSITIONS", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        viewModel.updateUI { u ->
                            u.copy(
                                pauseX = 0.90f, pauseY = 0.08f,
                                camX = 0.80f, camY = 0.08f,
                                lightsX = 0.70f, lightsY = 0.08f,
                                gearDownX = 0.38f, gearDownY = 0.90f,
                                handbrakeX = 0.50f, handbrakeY = 0.90f,
                                gearUpX = 0.62f, gearUpY = 0.90f,
                                radioX = 0.88f, radioY = 0.50f
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GridPanelBg),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MetallicBorder, RoundedCornerShape(4.dp))
                ) {
                    Text("🔄 RESET BUTTON POSITIONS", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // ADVANCED
            RacingHeader("Advanced Tools")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(label = "CALIBRATE", onClick = onCalibrationClick, modifier = Modifier.weight(1f))
                SecondaryButton(label = "DIAGNOSTICS", onClick = onDiagnosticsClick, modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrakeRed),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("RESET ALL TO DEFAULTS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(GridPanelBg)
            .border(1.dp, MetallicBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
