package com.example.core

import java.security.SecureRandom
import java.util.UUID

/**
 * Generates RFC 9562 UUIDv7 identifiers with a 48-bit millisecond Unix timestamp prefix
 * for chronological sorting across SQLite and Firestore.
 */
object UuidV7 {
    private val random = SecureRandom()

    fun generate(timestampMs: Long = System.currentTimeMillis()): String {
        val randomBytes = ByteArray(10)
        random.nextBytes(randomBytes)

        var msb = (timestampMs and 0xFFFFFFFFFFFFL) shl 16
        msb = msb or (0x7L shl 12) // version 7
        msb = msb or (((randomBytes[0].toLong() and 0x0FL) shl 8) or (randomBytes[1].toLong() and 0xFFL))

        var lsb = 0L
        for (i in 2 until 10) {
            lsb = (lsb shl 8) or (randomBytes[i].toLong() and 0xFFL)
        }
        lsb = (lsb and 0x3FFFFFFFFFFFFFFFL) or Long.MIN_VALUE // variant 10xx

        return UUID(msb, lsb).toString()
    }
}
