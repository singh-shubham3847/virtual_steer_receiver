package com.example.virtual_steer.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val config: ControllerConfig = ControllerConfig()
)
