package com.example.virtual_steer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtual_steer.model.PedalDiagnostics
import com.example.virtual_steer.model.PedalResponseCurve
import com.example.virtual_steer.ui.theme.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

enum class PedalType {
    THROTTLE, BRAKE
}

data class PedalUIConfig(
    val smoothingEnabled: Boolean = true,
    val smoothingFactor: Float = 0.25f,
    val responseCurve: PedalResponseCurve = PedalResponseCurve.LINEAR,
    val topDeadZone: Float = 0.01f,
    val bottomDeadZone: Float = 0.02f,
    val showDebug: Boolean = true,
    val maxDragPx: Float = 400f
)

@Composable
fun AnalogPedalZone(
    type: PedalType,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    config: PedalUIConfig = PedalUIConfig(),
    onDiagnosticsUpdate: (PedalDiagnostics) -> Unit = {}
) {
    var rawValue by remember { mutableFloatStateOf(0f) }
    var smoothedValue by remember { mutableFloatStateOf(0f) }
    var touchOrigin by remember { mutableStateOf<Offset?>(null) }
    var currentTouch by remember { mutableStateOf<Offset?>(null) }
    
    // High-precision smoothing
    LaunchedEffect(rawValue, config.smoothingEnabled) {
        if (!config.smoothingEnabled) {
            smoothedValue = rawValue
            val output = applyResponseCurve(smoothedValue, config)
            onValueChange(output)
            onDiagnosticsUpdate(PedalDiagnostics(
                rawValue = currentTouch?.y ?: 0f,
                deadZoneApplied = rawValue,
                curveOutput = output,
                smoothedValue = smoothedValue,
                finalOutput = output,
                percentage = output * 100f
            ))
            return@LaunchedEffect
        }
        
        if (smoothedValue != rawValue) {
            while (abs(rawValue - smoothedValue) > 0.0001f) {
                smoothedValue += (rawValue - smoothedValue) * config.smoothingFactor
                val output = applyResponseCurve(smoothedValue, config)
                onValueChange(output)
                onDiagnosticsUpdate(PedalDiagnostics(
                    rawValue = currentTouch?.y ?: 0f,
                    deadZoneApplied = rawValue,
                    curveOutput = output,
                    smoothedValue = smoothedValue,
                    finalOutput = output,
                    percentage = output * 100f
                ))
                kotlinx.coroutines.delay(8) // Higher frequency for precision
            }
            smoothedValue = rawValue
            val output = applyResponseCurve(smoothedValue, config)
            onValueChange(output)
            onDiagnosticsUpdate(PedalDiagnostics(
                rawValue = currentTouch?.y ?: 0f,
                deadZoneApplied = rawValue,
                curveOutput = output,
                smoothedValue = smoothedValue,
                finalOutput = output,
                percentage = output * 100f
            ))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.first().position

                        when (event.type) {
                            PointerEventType.Press -> {
                                touchOrigin = position
                                currentTouch = position
                                rawValue = 0f
                            }
                            PointerEventType.Move -> {
                                touchOrigin?.let { origin ->
                                    currentTouch = position
                                    // Calculate relative drag UPWARDS from origin
                                    val deltaY = origin.y - position.y
                                    val input = (deltaY / config.maxDragPx).coerceIn(0f, 1f)
                                    rawValue = applyDeadZones(input, config)
                                }
                            }
                            PointerEventType.Release -> {
                                touchOrigin = null
                                currentTouch = null
                                rawValue = 0f
                            }
                        }
                    }
                }
            }
    ) {
        // Render the Radial Gauge at the touch origin
        touchOrigin?.let { origin ->
            RadialGauge(
                type = type,
                progress = smoothedValue,
                outputValue = applyResponseCurve(smoothedValue, config),
                showDebug = config.showDebug,
                modifier = Modifier
                    .size(160.dp)
                    .offset {
                        IntOffset(
                            (origin.x - 80.dp.toPx()).toInt(),
                            (origin.y - 80.dp.toPx()).toInt()
                        )
                    }
            )
        }
    }
}

@Composable
private fun RadialGauge(
    type: PedalType,
    progress: Float,
    outputValue: Float,
    showDebug: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (type == PedalType.THROTTLE) ThrottleGreen else BrakeRed

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            // Original high-precision arc angles
            val startAngle = if (type == PedalType.THROTTLE) 150f else 30f
            val sweepAngle = if (type == PedalType.THROTTLE) -210f else 210f

            // 1. Background Track
            drawArc(
                color = Color(0x22FFFFFF),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Progress Arc
            if (progress > 0.001f) {
                drawArc(
                    color = primaryColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Telemetry Text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (type == PedalType.THROTTLE) "THROTTLE" else "BRAKE",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (showDebug) {
                Text(
                    text = String.format(Locale.US, "%.3f", outputValue),
                    color = primaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun applyDeadZones(input: Float, config: PedalUIConfig): Float {
    return when {
        input < config.bottomDeadZone -> 0f
        input > (1f - config.topDeadZone) -> 1f
        else -> (input - config.bottomDeadZone) / (1f - config.topDeadZone - config.bottomDeadZone)
    }
}

private fun applyResponseCurve(input: Float, config: PedalUIConfig): Float {
    return when (config.responseCurve) {
        PedalResponseCurve.LINEAR -> input
        PedalResponseCurve.RACING -> input.pow(1.8f)
        PedalResponseCurve.AGGRESSIVE -> input.pow(2.5f)
    }
}
