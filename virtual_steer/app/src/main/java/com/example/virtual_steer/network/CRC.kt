package com.example.virtual_steer.network

/**
 * CRC-16 (XMODEM) Implementation
 */
object CRC {

    private const val POLYNOMIAL = 0x1021

    fun calculate(data: ByteArray): Short {
        var crc = 0x0000

        for (b in data) {
            for (i in 0..7) {
                val bit = (b.toInt() shr (7 - i) and 1) == 1
                val c15 = (crc shr 15 and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) {
                    crc = crc xor POLYNOMIAL
                }
            }
        }

        return (crc and 0xFFFF).toShort()
    }
}
