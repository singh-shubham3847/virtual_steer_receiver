package com.example.virtual_steer.model

import kotlinx.serialization.Serializable

@Serializable
data class ControllerConfig(
    val steering: SteeringConfig = SteeringConfig(),
    val pedals: PedalConfig = PedalConfig(),
    val network: NetworkConfig = NetworkConfig(),
    val ui: UIConfig = UIConfig()
)
