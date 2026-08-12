package com.example.virtual_steer.network

object PacketOffset {
    const val HEADER = 0
    const val VERSION = 1
    const val SEQUENCE = 2
    const val STEERING = 4
    const val THROTTLE = 8
    const val BRAKE = 12
    const val CLUTCH = 16
    const val BUTTONS = 20
    const val RESERVED = 21
    const val CRC = 22
}
