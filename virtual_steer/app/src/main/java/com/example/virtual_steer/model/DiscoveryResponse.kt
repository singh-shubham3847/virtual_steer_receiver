package com.example.virtual_steer.model

import kotlinx.serialization.Serializable

@Serializable
data class DiscoveryResponse(
    val hostname: String,
    val port: Int
)
