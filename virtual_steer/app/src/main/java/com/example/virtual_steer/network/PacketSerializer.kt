package com.example.virtual_steer.network

import com.example.virtual_steer.model.ControllerState
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PacketSerializer {

    private var sequence: Short = 0
    
    // Pre-allocated buffer for zero-allocation serialization
    private val bufferArray = ByteArray(Protocol.PACKET_SIZE)
    private val buffer = ByteBuffer.wrap(bufferArray).order(ByteOrder.LITTLE_ENDIAN)

    fun serialize(state: ControllerState): ByteArray {
        buffer.clear()

        buffer.put(PacketOffset.HEADER, Protocol.HEADER)
        buffer.put(PacketOffset.VERSION, Protocol.VERSION)

        buffer.putShort(PacketOffset.SEQUENCE, sequence++)
        buffer.putFloat(PacketOffset.STEERING, state.steering)
        buffer.putFloat(PacketOffset.THROTTLE, state.throttle)
        buffer.putFloat(PacketOffset.BRAKE, state.brake)
        buffer.putFloat(PacketOffset.CLUTCH, state.clutch)

        buffer.putShort(PacketOffset.BUTTONS, packButtons(state))

        // Reset CRC field before calculation
        buffer.putShort(PacketOffset.CRC, 0)

        val crc = CRC.calculate(bufferArray.copyOfRange(0, PacketOffset.CRC))
        buffer.putShort(PacketOffset.CRC, crc)

        // Return a copy to ensure thread safety if used outside the network thread
        // Note: In high-perf scenarios, we might return the internal array and copy it at the socket layer
        return bufferArray.copyOf()
    }

    private fun packButtons(state: ControllerState): Short {
        var buttons = 0
        if (state.handbrake)  buttons = buttons or (1 shl 0)
        if (state.gearUp)     buttons = buttons or (1 shl 1)
        if (state.gearDown)   buttons = buttons or (1 shl 2)
        if (state.pause)      buttons = buttons or (1 shl 3)
        if (state.horn)       buttons = buttons or (1 shl 4)
        if (state.camera)     buttons = buttons or (1 shl 5)
        if (state.headlights) buttons = buttons or (1 shl 6)
        if (state.dpadUp)     buttons = buttons or (1 shl 7)
        if (state.dpadDown)   buttons = buttons or (1 shl 8)
        if (state.dpadLeft)   buttons = buttons or (1 shl 9)
        if (state.dpadRight)  buttons = buttons or (1 shl 10)
        if (state.lb)         buttons = buttons or (1 shl 11)
        if (state.rb)         buttons = buttons or (1 shl 12)
        if (state.back)       buttons = buttons or (1 shl 13)
        return buttons.toShort()
    }

    fun deserialize(packet: ByteArray): ControllerState {
        require(packet.size == Protocol.PACKET_SIZE) { "Invalid packet size" }

        val readBuffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN)

        val header = readBuffer.get(PacketOffset.HEADER)
        require(header == Protocol.HEADER) { "Invalid header" }

        val version = readBuffer.get(PacketOffset.VERSION)
        require(version == Protocol.VERSION) { "Unsupported version" }

        // Verify CRC
        val receivedCrc = readBuffer.getShort(PacketOffset.CRC)
        val calculatedCrc = CRC.calculate(packet.copyOfRange(0, PacketOffset.CRC))
        require(receivedCrc == calculatedCrc) { "CRC mismatch" }

        val steering = readBuffer.getFloat(PacketOffset.STEERING)
        val throttle = readBuffer.getFloat(PacketOffset.THROTTLE)
        val brake = readBuffer.getFloat(PacketOffset.BRAKE)
        val clutch = readBuffer.getFloat(PacketOffset.CLUTCH)
        val buttons = readBuffer.getShort(PacketOffset.BUTTONS).toInt()

        return ControllerState(
            steering = steering,
            throttle = throttle,
            brake = brake,
            clutch = clutch,
            handbrake = (buttons and (1 shl 0)) != 0,
            gearUp = (buttons and (1 shl 1)) != 0,
            gearDown = (buttons and (1 shl 2)) != 0,
            pause = (buttons and (1 shl 3)) != 0,
            horn = (buttons and (1 shl 4)) != 0,
            camera = (buttons and (1 shl 5)) != 0,
            headlights = (buttons and (1 shl 6)) != 0,
            dpadUp = (buttons and (1 shl 7)) != 0,
            dpadDown = (buttons and (1 shl 8)) != 0,
            dpadLeft = (buttons and (1 shl 9)) != 0,
            dpadRight = (buttons and (1 shl 10)) != 0,
            lb = (buttons and (1 shl 11)) != 0,
            rb = (buttons and (1 shl 12)) != 0,
            back = (buttons and (1 shl 13)) != 0
        )
    }
}
