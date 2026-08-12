package com.example.virtual_steer.navigation

sealed class Screen(val route: String) {
    object Pair : Screen("pair")
    object Home : Screen("home")
    object Driving : Screen("driving")
    object Settings : Screen("settings")
    object Calibration : Screen("calibration")
    object Diagnostics : Screen("diagnostics")
    object LayoutEditor : Screen("layout_editor")
}
