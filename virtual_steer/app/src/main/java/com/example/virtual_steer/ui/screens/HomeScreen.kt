package com.example.virtual_steer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.virtual_steer.ui.theme.*
import com.example.virtual_steer.viewmodel.ConnectionStatus

@Composable
fun HomeScreen(
    connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    pcName: String = "Unknown PC",
    profileName: String = "Default Profile",
    batteryLevel: Int = 82,
    latencyMs: Int = 0,
    onStartDriving: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDiagnosticsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "VIRTUAL STEER",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
        
        Text(
            text = "DASHBOARD",
            color = ThrottleGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Status Dashboard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GridPanelBg)
                .border(1.dp, MetallicBorder, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardRow("STATUS", connectionStatus.name, color = if (connectionStatus == ConnectionStatus.CONNECTED) ThrottleGreen else AccentYellow)
                DashboardRow("PC", pcName)
                DashboardRow("PROFILE", profileName)
                DashboardRow("BATTERY", "$batteryLevel%")
                DashboardRow("PING", "${latencyMs}ms")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main Action
        Button(
            onClick = onStartDriving,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ThrottleGreen),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "START DRIVING",
                color = CarbonDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SecondaryActionButton(
                text = "SETTINGS",
                onClick = onSettingsClick,
                modifier = Modifier.weight(1f)
            )
            SecondaryActionButton(
                text = "DIAGNOSTICS",
                onClick = onDiagnosticsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DashboardRow(label: String, value: String, color: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(GridPanelBg)
            .border(1.dp, MetallicBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
