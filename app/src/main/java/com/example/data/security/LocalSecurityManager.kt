package com.example.data.security

import android.util.Base64

object LocalSecurityManager {
    private const val MASK_KEY = 0x5C.toByte() // Standard visual masking key

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val bytes = plainText.toByteArray(Charsets.UTF_8)
            val masked = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor MASK_KEY.toInt()).toByte() }
            Base64.encodeToString(masked, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val decoded = Base64.decode(encryptedText, Base64.NO_WRAP)
            val unmasked = ByteArray(decoded.size) { i -> (decoded[i].toInt() xor MASK_KEY.toInt()).toByte() }
            String(unmasked, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedText
        }
    }
}
