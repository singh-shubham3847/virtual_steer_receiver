package com.example.virtual_steer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BottomControls(
    modifier: Modifier = Modifier,
    onHandbrakeChange: (Boolean) -> Unit = {},
    onGearDownChange: (Boolean) -> Unit = {},
    onGearUpChange: (Boolean) -> Unit = {}
) {
    Row(
        modifier = modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RacingButton(
            "HBRAKE",
            modifier = Modifier.width(70.dp),
            onPressedChange = onHandbrakeChange
        ) {}
        RacingButton(
            "GEAR-",
            modifier = Modifier.width(64.dp),
            onPressedChange = onGearDownChange
        ) {}
        RacingButton(
            "GEAR+",
            modifier = Modifier.width(64.dp),
            onPressedChange = onGearUpChange
        ) {}
    }
}
