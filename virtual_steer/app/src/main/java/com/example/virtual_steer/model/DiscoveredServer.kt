package com.example.virtual_steer.model

data class DiscoveredServer(
    val ip: String,
    val port: Int,
    val name: String,
    val lastSeen: Long = System.currentTimeMillis()
)
