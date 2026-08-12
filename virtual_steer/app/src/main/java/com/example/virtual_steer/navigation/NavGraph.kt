package com.example.virtual_steer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.virtual_steer.ui.screens.*
import com.example.virtual_steer.viewmodel.ControllerViewModel
import com.example.virtual_steer.viewmodel.SettingsViewModel

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    controllerViewModel: ControllerViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val steeringAngle by controllerViewModel.steeringAngle.collectAsState()
    val connectionState by controllerViewModel.connectionState.collectAsState()
    val diagnostics by controllerViewModel.diagnostics.collectAsState()
    val config by settingsViewModel.config.collectAsState()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Pair.route
    ) {
        composable(Screen.Pair.route) {
            PairScreen(
                viewModel = controllerViewModel,
                onPairSuccess = { 
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Pair.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                connectionStatus = connectionState.status,
                pcName = connectionState.serverName,
                latencyMs = connectionState.latencyMs,
                batteryLevel = diagnostics.battery,
                onStartDriving = { navController.navigate(Screen.Driving.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onDiagnosticsClick = { navController.navigate(Screen.Diagnostics.route) }
            )
        }

        composable(Screen.Driving.route) {
            DrivingScreen(
                steeringAngle = steeringAngle,
                pcName = connectionState.serverName,
                latencyMs = diagnostics.network.latencyMs,
                packetRate = diagnostics.network.packetRate,
                showRadio = config.ui.showRadio,
                pauseX = config.ui.pauseX,
                pauseY = config.ui.pauseY,
                camX = config.ui.camX,
                camY = config.ui.camY,
                lightsX = config.ui.lightsX,
                lightsY = config.ui.lightsY,
                gearDownX = config.ui.gearDownX,
                gearDownY = config.ui.gearDownY,
                handbrakeX = config.ui.handbrakeX,
                handbrakeY = config.ui.handbrakeY,
                gearUpX = config.ui.gearUpX,
                gearUpY = config.ui.gearUpY,
                radioX = config.ui.radioX,
                radioY = config.ui.radioY,
                onBackClick = { navController.popBackStack() },
                onBrakeChange = { controllerViewModel.updateBrake(it) },
                onThrottleChange = { controllerViewModel.updateThrottle(it) },
                onHandbrakeChange = { controllerViewModel.updateHandbrake(it) },
                onGearDownChange = { controllerViewModel.updateGearDown(it) },
                onGearUpChange = { controllerViewModel.updateGearUp(it) },
                onPauseClick = { controllerViewModel.pulsePause() },
                onCamClick = { controllerViewModel.pulseCamera() },
                onLightsClick = { controllerViewModel.pulseHeadlights() },
                onRadioClick = { controllerViewModel.pulseDpadRight() },
                onSaveLayout = { pX, pY, cX, cY, lX, lY, gdX, gdY, hX, hY, guX, guY, rX, rY ->
                    settingsViewModel.updateUI { u ->
                        u.copy(
                            pauseX = pX, pauseY = pY,
                            camX = cX, camY = cY,
                            lightsX = lX, lightsY = lY,
                            gearDownX = gdX, gearDownY = gdY,
                            handbrakeX = hX, handbrakeY = hY,
                            gearUpX = guX, gearUpY = guY,
                            radioX = rX, radioY = rY
                        )
                    }
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() },
                onCalibrationClick = { navController.navigate(Screen.Calibration.route) },
                onDiagnosticsClick = { navController.navigate(Screen.Diagnostics.route) },
                onCustomizeLayoutClick = { navController.navigate(Screen.LayoutEditor.route) }
            )
        }
        
        composable(Screen.Calibration.route) {
            CalibrationScreen(
                viewModel = controllerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(
                viewModel = controllerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.LayoutEditor.route) {
            DrivingScreen(
                steeringAngle = steeringAngle,
                pcName = connectionState.serverName,
                latencyMs = diagnostics.network.latencyMs,
                packetRate = diagnostics.network.packetRate,
                showRadio = config.ui.showRadio,
                startInEditMode = true,
                pauseX = config.ui.pauseX,
                pauseY = config.ui.pauseY,
                camX = config.ui.camX,
                camY = config.ui.camY,
                lightsX = config.ui.lightsX,
                lightsY = config.ui.lightsY,
                gearDownX = config.ui.gearDownX,
                gearDownY = config.ui.gearDownY,
                handbrakeX = config.ui.handbrakeX,
                handbrakeY = config.ui.handbrakeY,
                gearUpX = config.ui.gearUpX,
                gearUpY = config.ui.gearUpY,
                radioX = config.ui.radioX,
                radioY = config.ui.radioY,
                onBackClick = { navController.popBackStack() },
                onBrakeChange = { controllerViewModel.updateBrake(it) },
                onThrottleChange = { controllerViewModel.updateThrottle(it) },
                onHandbrakeChange = { controllerViewModel.updateHandbrake(it) },
                onGearDownChange = { controllerViewModel.updateGearDown(it) },
                onGearUpChange = { controllerViewModel.updateGearUp(it) },
                onPauseClick = { controllerViewModel.pulsePause() },
                onCamClick = { controllerViewModel.pulseCamera() },
                onLightsClick = { controllerViewModel.pulseHeadlights() },
                onRadioClick = { controllerViewModel.pulseDpadRight() },
                onSaveLayout = { pX, pY, cX, cY, lX, lY, gdX, gdY, hX, hY, guX, guY, rX, rY ->
                    settingsViewModel.updateUI { u ->
                        u.copy(
                            pauseX = pX, pauseY = pY,
                            camX = cX, camY = cY,
                            lightsX = lX, lightsY = lY,
                            gearDownX = gdX, gearDownY = gdY,
                            handbrakeX = hX, handbrakeY = hY,
                            gearUpX = guX, gearUpY = guY,
                            radioX = rX, radioY = rY
                        )
                    }
                    navController.popBackStack() // Go back to settings after saving layout config
                }
            )
        }
    }
}
