package com.example.virtual_steer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.virtual_steer.navigation.NavGraph
import com.example.virtual_steer.ui.theme.Virtual_steerTheme
import com.example.virtual_steer.viewmodel.ControllerViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            Virtual_steerTheme {
                val viewModel: ControllerViewModel = viewModel()

                LaunchedEffect(Unit) {
                    viewModel.startSensor()
                }

                NavGraph(controllerViewModel = viewModel)
            }
        }
    }
}
