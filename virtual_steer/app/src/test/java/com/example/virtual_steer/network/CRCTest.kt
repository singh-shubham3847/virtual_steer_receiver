package com.example.virtual_steer.network

import org.junit.Assert.assertEquals
import org.junit.Test

class CRCTest {

    @Test
    fun testCrcCalculation() {
        // Known values for CRC-16/XMODEM (CCITT)
        // "123456789" -> 0x31C3
        val data = "123456789".toByteArray()
        val crc = CRC.calculate(data)
        
        assertEquals(0x31C3.toShort(), crc)
    }

    @Test
    fun testEmptyData() {
        val crc = CRC.calculate(ByteArray(0))
        assertEquals(0.toShort(), crc)
    }
}
