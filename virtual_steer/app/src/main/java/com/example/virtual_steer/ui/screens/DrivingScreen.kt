package com.example.virtual_steer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtual_steer.ui.components.AnalogPedalZone
import com.example.virtual_steer.ui.components.PedalType
import com.example.virtual_steer.ui.components.PedalUIConfig
import com.example.virtual_steer.model.PedalResponseCurve
import com.example.virtual_steer.model.PedalDiagnostics
import com.example.virtual_steer.ui.components.RacingButton
import com.example.virtual_steer.ui.theme.*

@Composable
fun DrivingScreen(
    latencyMs: Int = 32,
    steeringAngle: Float = 0f,
    pcName: String = "Unknown PC",
    batteryLevel: Int = 100,
    packetRate: Int = 0,
    showRadio: Boolean = true,
    startInEditMode: Boolean = false,
    
    // Layout coordinates
    pauseX: Float = 0.90f,
    pauseY: Float = 0.08f,
    camX: Float = 0.80f,
    camY: Float = 0.08f,
    lightsX: Float = 0.70f,
    lightsY: Float = 0.08f,
    gearDownX: Float = 0.38f,
    gearDownY: Float = 0.90f,
    handbrakeX: Float = 0.50f,
    handbrakeY: Float = 0.90f,
    gearUpX: Float = 0.62f,
    gearUpY: Float = 0.90f,
    radioX: Float = 0.88f,
    radioY: Float = 0.50f,

    onPauseClick: () -> Unit = {},
    onCamClick: () -> Unit = {},
    onLightsClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onBrakeChange: (Float) -> Unit = {},
    onThrottleChange: (Float) -> Unit = {},
    onHandbrakeChange: (Boolean) -> Unit = {},
    onGearDownChange: (Boolean) -> Unit = {},
    onGearUpChange: (Boolean) -> Unit = {},
    onRadioClick: () -> Unit = {},
    onSaveLayout: (
        pauseX: Float, pauseY: Float,
        camX: Float, camY: Float,
        lightsX: Float, lightsY: Float,
        gearDownX: Float, gearDownY: Float,
        handbrakeX: Float, handbrakeY: Float,
        gearUpX: Float, gearUpY: Float,
        radioX: Float, radioY: Float
    ) -> Unit = { _,_, _,_, _,_, _,_, _,_, _,_, _,_ -> },
    onBrakeDiagnostics: (PedalDiagnostics) -> Unit = {},
    onThrottleDiagnostics: (PedalDiagnostics) -> Unit = {}
) {
    // Shared config for pedals
    val pedalConfig = remember {
        PedalUIConfig(
            smoothingEnabled = true,
            smoothingFactor = 0.25f,
            responseCurve = PedalResponseCurve.RACING,
            showDebug = true,
            maxDragPx = 400f
        )
    }

    var isEditingLayout by remember { mutableStateOf(startInEditMode) }

    // Coordinates states
    var pausePos by remember(pauseX, pauseY) { mutableStateOf(Offset(pauseX, pauseY)) }
    var camPos by remember(camX, camY) { mutableStateOf(Offset(camX, camY)) }
    var lightsPos by remember(lightsX, lightsY) { mutableStateOf(Offset(lightsX, lightsY)) }
    var gearDownPos by remember(gearDownX, gearDownY) { mutableStateOf(Offset(gearDownX, gearDownY)) }
    var handbrakePos by remember(handbrakeX, handbrakeY) { mutableStateOf(Offset(handbrakeX, handbrakeY)) }
    var gearUpPos by remember(gearUpX, gearUpY) { mutableStateOf(Offset(gearUpX, gearUpY)) }
    var radioPos by remember(radioX, radioY) { mutableStateOf(Offset(radioX, radioY)) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // Helper to position and make elements draggable
        @Composable
        fun getModifierForButton(
            position: Offset,
            buttonWidthDp: Int,
            buttonHeightDp: Int,
            onPositionChanged: (Offset) -> Unit
        ): Modifier {
            val currentPosition by rememberUpdatedState(position)
            val currentOnPositionChanged by rememberUpdatedState(onPositionChanged)
            val xDp = with(density) { (position.x * screenWidthPx).toDp() }
            val yDp = with(density) { (position.y * screenHeightPx).toDp() }
            
            return Modifier
                .offset(
                    x = xDp - (buttonWidthDp / 2).dp,
                    y = yDp - (buttonHeightDp / 2).dp
                )
                .pointerInput(isEditingLayout) {
                    if (!isEditingLayout) return@pointerInput
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = (currentPosition.x + dragAmount.x / screenWidthPx).coerceIn(0.02f, 0.98f)
                        val newY = (currentPosition.y + dragAmount.y / screenHeightPx).coerceIn(0.02f, 0.98f)
                        currentOnPositionChanged(Offset(newX, newY))
                    }
                }
                .then(
                    if (isEditingLayout) {
                        Modifier.border(1.5.dp, AccentYellow, RoundedCornerShape(8.dp))
                    } else {
                        Modifier
                    }
                )
        }

        // ==========================================
        // 1. INTERACTION ZONES (Bottom Half)
        // Only active if not editing layout
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .align(Alignment.BottomCenter)
        ) {
            // LEFT ZONE: BRAKE
            Box(modifier = Modifier.weight(1f)) {
                AnalogPedalZone(
                    type = PedalType.BRAKE,
                    onValueChange = { if (!isEditingLayout) onBrakeChange(it) },
                    config = pedalConfig,
                    onDiagnosticsUpdate = onBrakeDiagnostics
                )
            }
            
            // RIGHT ZONE: THROTTLE
            Box(modifier = Modifier.weight(1f)) {
                AnalogPedalZone(
                    type = PedalType.THROTTLE,
                    onValueChange = { if (!isEditingLayout) onThrottleChange(it) },
                    config = pedalConfig,
                    onDiagnosticsUpdate = onThrottleDiagnostics
                )
            }
        }

        // ==========================================
        // 2. HUD CONTROLS (Top)
        // ==========================================

        // TOP-LEFT: Status
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HudStatRow("PC", pcName)
        }

        // TOP-CENTER: Control Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isEditingLayout) {
                HudButton(
                    label = "💾 SAVE & EXIT",
                    onClick = {
                        isEditingLayout = false
                        onSaveLayout(
                            pausePos.x, pausePos.y,
                            camPos.x, camPos.y,
                            lightsPos.x, lightsPos.y,
                            gearDownPos.x, gearDownPos.y,
                            handbrakePos.x, handbrakePos.y,
                            gearUpPos.x, gearUpPos.y,
                            radioPos.x, radioPos.y
                        )
                    },
                    color = ThrottleGreen
                )
            } else {
                HudButton(label = "STOP DRIVING", onClick = onBackClick, color = BrakeRed)
            }
        }

        // ==========================================
        // 3. DRAGGABLE VIRTUAL BUTTONS
        // ==========================================

        // PAUSE Button
        Box(
            modifier = getModifierForButton(
                position = pausePos,
                buttonWidthDp = 68,
                buttonHeightDp = 42,
                onPositionChanged = { pausePos = it }
            )
        ) {
            HudButton(label = "Pause", onClick = { if (!isEditingLayout) onPauseClick() })
        }

        // CAM Button
        Box(
            modifier = getModifierForButton(
                position = camPos,
                buttonWidthDp = 68,
                buttonHeightDp = 42,
                onPositionChanged = { camPos = it }
            )
        ) {
            HudButton(label = "Cam", onClick = { if (!isEditingLayout) onCamClick() })
        }

        // LIGHTS Button
        Box(
            modifier = getModifierForButton(
                position = lightsPos,
                buttonWidthDp = 68,
                buttonHeightDp = 42,
                onPositionChanged = { lightsPos = it }
            )
        ) {
            HudButton(label = "Lights", onClick = { if (!isEditingLayout) onLightsClick() })
        }

        // GEAR DOWN Button
        Box(
            modifier = getModifierForButton(
                position = gearDownPos,
                buttonWidthDp = 64,
                buttonHeightDp = 42,
                onPositionChanged = { gearDownPos = it }
            )
        ) {
            RacingButton(
                text = "GEAR-",
                modifier = Modifier.width(64.dp),
                onPressedChange = { if (!isEditingLayout) onGearDownChange(it) }
            ) {}
        }

        // HANDBRAKE Button
        Box(
            modifier = getModifierForButton(
                position = handbrakePos,
                buttonWidthDp = 70,
                buttonHeightDp = 42,
                onPositionChanged = { handbrakePos = it }
            )
        ) {
            RacingButton(
                text = "HBRAKE",
                modifier = Modifier.width(70.dp),
                onPressedChange = { if (!isEditingLayout) onHandbrakeChange(it) }
            ) {}
        }

        // GEAR UP Button
        Box(
            modifier = getModifierForButton(
                position = gearUpPos,
                buttonWidthDp = 64,
                buttonHeightDp = 42,
                onPositionChanged = { gearUpPos = it }
            )
        ) {
            RacingButton(
                text = "GEAR+",
                modifier = Modifier.width(64.dp),
                onPressedChange = { if (!isEditingLayout) onGearUpChange(it) }
            ) {}
        }

        // RADIO Button
        if (showRadio) {
            Box(
                modifier = getModifierForButton(
                    position = radioPos,
                    buttonWidthDp = 80,
                    buttonHeightDp = 42,
                    onPositionChanged = { radioPos = it }
                )
            ) {
                RacingButton(
                    text = "📻 RADIO",
                    modifier = Modifier.width(80.dp),
                    onClick = { if (!isEditingLayout) onRadioClick() }
                )
            }
        }

        // Layout Editor Hint Overlay
        if (isEditingLayout) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(2.dp, AccentYellow, RoundedCornerShape(0.dp)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "LAYOUT EDITOR ACTIVE: DRAG ANY BUTTON TO REARRANGE, THEN CLICK 'SAVE'",
                    color = AccentYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun HudStatRow(label: String, value: String, color: Color = Color.White) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "$label:",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun HudButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.9f)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(GridPanelBg)
            .border(1.dp, MetallicBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
