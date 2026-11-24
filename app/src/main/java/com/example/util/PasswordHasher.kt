package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256

    data class HashedPassword(
        val hashHex: String,
        val saltHex: String
    )

    fun hashPassword(password: String): HashedPassword {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltHex = salt.toHex()
        val hash = pbkdf2(password.toCharArray(), salt)
        return HashedPassword(hashHex = hash.toHex(), saltHex = saltHex)
    }

    fun verifyPassword(candidatePassword: String, expectedHashHex: String, saltHex: String): Boolean {
        val salt = hexToBytes(saltHex) ?: return false
        val expectedBytes = hexToBytes(expectedHashHex) ?: return false
        val actualBytes = pbkdf2(candidatePassword.toCharArray(), salt)
        return MessageDigest.isEqual(actualBytes, expectedBytes)
    }

    private fun pbkdf2(chars: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0 || hex.isEmpty()) return null
        return runCatching {
            ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }.getOrNull()
    }
}
