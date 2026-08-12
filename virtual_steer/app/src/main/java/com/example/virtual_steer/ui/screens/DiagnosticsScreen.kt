package com.example.virtual_steer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtual_steer.model.PedalDiagnostics
import com.example.virtual_steer.model.SteeringDiagnostics
import com.example.virtual_steer.ui.components.RacingHeader
import com.example.virtual_steer.ui.components.TuningCard
import com.example.virtual_steer.ui.theme.*
import com.example.virtual_steer.viewmodel.ControllerViewModel
import com.example.virtual_steer.viewmodel.LogEntry
import com.example.virtual_steer.viewmodel.LogLevel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: ControllerViewModel,
    onBackClick: () -> Unit
) {
    val diag by viewModel.diagnostics.collectAsState()
    val logs by viewModel.eventLogs.collectAsState()
    val lastPacket by viewModel.lastPacket.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DIAGNOSTICS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
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
            RacingHeader("Live Telemetry")
            SteeringGraph(diag.steering.finalOutput)
            
            Spacer(modifier = Modifier.height(16.dp))

            RacingHeader("Steering")
            SteeringDiagCard(diag.steering)

            RacingHeader("Pedals")
            PedalDiagCard("Throttle", diag.throttle)
            Spacer(modifier = Modifier.height(16.dp))
            PedalDiagCard("Brake", diag.brake)

            RacingHeader("Sensors")
            TuningCard("Frequencies") {
                DiagRow("Rotation Vector", String.format(Locale.US, "%.1f Hz", diag.sensors.rotationFreq))
                DiagRow("Gyroscope", String.format(Locale.US, "%.1f Hz", diag.sensors.gyroFreq))
                DiagRow("Accelerometer", String.format(Locale.US, "%.1f Hz", diag.sensors.accelFreq))
            }

            RacingHeader("Packet Viewer")
            PacketHexViewer(lastPacket)

            RacingHeader("Event Log")
            EventLogViewer(logs)

            RacingHeader("Performance")
            TuningCard("System") {
                DiagRow("Battery", "${diag.battery}%")
                DiagRow("UI FPS", "${diag.fps}")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SteeringGraph(value: Float) {
    val points = remember { mutableStateListOf<Float>() }
    
    LaunchedEffect(value) {
        points.add(value)
        if (points.size > 100) points.removeAt(0)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(GridPanelBg)
            .border(1.dp, MetallicBorder, RoundedCornerShape(8.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            
            // Draw Center Line
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY)
            )

            if (points.size > 1) {
                val path = Path()
                val stepX = width / 100f
                
                points.forEachIndexed { index, point ->
                    val x = index * stepX
                    val y = centerY - (point * (height / 2))
                    
                    if (index == 0) path.moveTo(x, y)
                    else path.lineTo(x, y)
                }
                
                drawPath(
                    path = path,
                    color = ThrottleGreen,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun PacketHexViewer(packet: ByteArray?) {
    TuningCard("Latest UDP Packet") {
        if (packet == null) {
            Text("No packets sent yet", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        } else {
            val hex = packet.joinToString(" ") { String.format("%02X", it) }
            Text(
                text = hex,
                color = AccentYellow,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Size: ${packet.size} bytes", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun EventLogViewer(logs: List<LogEntry>) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
    
    TuningCard("System Logs") {
        Column(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
            logs.forEach { log ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = "[${dateFormat.format(Date(log.timestamp))}] ",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${log.tag}: ",
                        color = ThrottleGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = log.message,
                        color = when(log.level) {
                            LogLevel.WARN -> AccentYellow
                            LogLevel.ERROR -> BrakeRed
                            else -> Color.White
                        },
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SteeringDiagCard(s: SteeringDiagnostics) {
    TuningCard("Internal State") {
        DiagRow("Raw Angle", String.format(Locale.US, "%.2f°", s.rawAngle))
        DiagRow("Calibration", String.format(Locale.US, "%.2f°", s.calibrationOffset))
        DiagRow("Calibrated", String.format(Locale.US, "%.2f°", s.calibratedAngle))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
        DiagRow("Dead Zone Output", String.format(Locale.US, "%.2f°", s.deadZoneApplied))
        DiagRow("Smoothed", String.format(Locale.US, "%.2f°", s.smoothedAngle))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
        DiagRow("Final Output", String.format(Locale.US, "%.4f", s.finalOutput), color = ThrottleGreen)
        DiagRow("Percentage", String.format(Locale.US, "%.1f%%", s.percentage), color = ThrottleGreen)
    }
}

@Composable
private fun PedalDiagCard(title: String, p: PedalDiagnostics) {
    TuningCard(title.uppercase()) {
        DiagRow("Raw Value", String.format(Locale.US, "%.3f", p.rawValue))
        DiagRow("Dead Zone Output", String.format(Locale.US, "%.3f", p.deadZoneApplied))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))
        DiagRow("Curve Output", String.format(Locale.US, "%.3f", p.curveOutput))
        DiagRow("Smoothed", String.format(Locale.US, "%.3f", p.smoothedValue))
        DiagRow("Final Output", String.format(Locale.US, "%.3f", p.finalOutput), color = ThrottleGreen)
    }
}

@Composable
private fun DiagRow(label: String, value: String, color: Color = Color.White) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
