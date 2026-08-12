package com.example.virtual_steer.network

import com.example.virtual_steer.model.ControllerState
import org.junit.Assert.*
import org.junit.Test

class PacketSerializerTest {

    @Test
    fun serializationTest() {

        val serializer = PacketSerializer()

        val original = ControllerState(
            steering = 0.42f,
            throttle = 0.91f,
            brake = 0.13f,
            clutch = 0.05f,
            handbrake = true,
            gearUp = false,
            gearDown = true,
            pause = false,
            horn = true,
            camera = false,
            headlights = true
        )

        val packet = serializer.serialize(original)

        assertEquals(Protocol.PACKET_SIZE, packet.size)

        val decoded = serializer.deserialize(packet)

        assertEquals(original.steering, decoded.steering, 0.001f)
        assertEquals(original.throttle, decoded.throttle, 0.001f)
        assertEquals(original.brake, decoded.brake, 0.001f)
        assertEquals(original.clutch, decoded.clutch, 0.001f)

        assertEquals(original.handbrake, decoded.handbrake)
        assertEquals(original.gearUp, decoded.gearUp)
        assertEquals(original.gearDown, decoded.gearDown)
        assertEquals(original.pause, decoded.pause)
        assertEquals(original.horn, decoded.horn)
        assertEquals(original.camera, decoded.camera)
        assertEquals(original.headlights, decoded.headlights)
    }
}
