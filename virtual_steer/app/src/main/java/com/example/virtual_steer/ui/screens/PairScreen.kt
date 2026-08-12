package com.example.virtual_steer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.virtual_steer.model.DiscoveredServer
import com.example.virtual_steer.ui.theme.*
import com.example.virtual_steer.viewmodel.ConnectionStatus
import com.example.virtual_steer.viewmodel.ControllerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreen(
    viewModel: ControllerViewModel,
    onPairSuccess: () -> Unit
) {
    val discoveredServers by viewModel.discoveredServers.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    
    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("4444") }

    LaunchedEffect(connectionState.status) {
        if (connectionState.status == ConnectionStatus.CONNECTED) {
            onPairSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CarbonDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "VIRTUAL STEER",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
        
        Text(
            text = "PAIR WITH PC",
            color = ThrottleGreen,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Discovery Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = ThrottleGreen,
                strokeWidth = 2.dp
            )
            Text(
                text = "SEARCHING...",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Available Receivers
        Text(
            text = "AVAILABLE RECEIVERS",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (discoveredServers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No PC found yet.\nCheck same Wi-Fi or Hotspot.",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(discoveredServers) { server ->
                        ServerCard(server = server) {
                            viewModel.connectToPC(server.ip, server.port, server.name)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Manual IP Fallback
        Text(
            text = "MANUAL CONNECTION",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualIp,
                onValueChange = { manualIp = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("192.168.x.x", color = Color.White.copy(alpha = 0.3f)) },
                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ThrottleGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                ),
                singleLine = true
            )
            
            Button(
                onClick = { 
                    val port = manualPort.toIntOrNull() ?: 4444
                    if (manualIp.isNotEmpty()) {
                        viewModel.connectToPC(manualIp, port, "Manual PC")
                    }
                },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GridPanelBg),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MetallicBorder)
            ) {
                Text("CONNECT", color = ThrottleGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
        
        AnimatedVisibility(visible = connectionState.status == ConnectionStatus.CONNECTING) {
            Text(
                text = "Connecting to ${connectionState.serverIp}...",
                color = AccentYellow,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun ServerCard(
    server: DiscoveredServer,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GridPanelBg)
            .border(1.dp, MetallicBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = server.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = server.ip,
                    color = ThrottleGreen,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Port ${server.port}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "READY",
                color = ThrottleGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
