package com.example.virtual_steer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtual_steer.ui.components.RacingHeader
import com.example.virtual_steer.ui.components.TuningCard
import com.example.virtual_steer.ui.theme.*
import com.example.virtual_steer.viewmodel.ControllerViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    viewModel: ControllerViewModel,
    onBackClick: () -> Unit
) {
    val diag by viewModel.diagnostics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CALIBRATION",
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
            
            RacingHeader("Steering Center")
            TuningCard("Current Orientation") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(CarbonDark)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "CURRENT ANGLE",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            String.format(Locale.US, "%.1f°", diag.steering.rawAngle),
                            color = AccentYellow,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { viewModel.calibrateCenter() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ThrottleGreen),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "SET CURRENT POSITION AS CENTER",
                        color = CarbonDark,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = { viewModel.resetCalibration() },
                    modifier = Modifier.fillMaxWidth(),
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(width = 1.dp, brush = androidx.compose.ui.graphics.SolidColor(BrakeRed)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "RESET CALIBRATION",
                        color = BrakeRed,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            RacingHeader("Quick Info")
            TuningCard("Active Offset") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Offset Value", color = Color.White.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace)
                    Text(String.format(Locale.US, "%.2f°", diag.steering.calibrationOffset), color = Color.White, fontFamily = FontFamily.Monospace)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
