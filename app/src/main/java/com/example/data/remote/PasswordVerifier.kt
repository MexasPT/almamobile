package com.example.data.remote

import android.util.Log
import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import java.util.Locale

object PasswordVerifier {

    private const val TAG = "PasswordVerifier"

    /**
     * Verifies a plain password against any hash format supported by YetiForce, Vtiger, and MySQL:
     * - BCrypt ($2y$, $2a$, $2b$) - PHP password_hash default in YetiForce
     * - MD5 (plain MD5, MD5 with username, double MD5)
     * - SHA-1, SHA-256, SHA-512
     * - MySQL PASSWORD() hash (*SHA1(SHA1()))
     * - Plaintext (fallback)
     */
    fun verify(password: String, storedHash: String, username: String = ""): Boolean {
        val cleanHash = storedHash.trim()
        if (cleanHash.isBlank() || password.isBlank()) return false

        // 1. Plaintext direct match
        if (cleanHash == password) {
            return true
        }

        // 2. BCrypt ($2y$, $2a$, $2b$, $2x$)
        if (cleanHash.startsWith("$2a$") || cleanHash.startsWith("$2y$") || cleanHash.startsWith("$2b$") || cleanHash.startsWith("$2x$")) {
            try {
                // BCrypt in Java natively expects $2a$, PHP uses $2y$. They are cryptographically compatible.
                val compatibleHash = cleanHash
                    .replaceFirst("^\\$2y\\$".toRegex(), "\\$2a\\$")
                    .replaceFirst("^\\$2b\\$".toRegex(), "\\$2a\\$")
                    .replaceFirst("^\\$2x\\$".toRegex(), "\\$2a\\$")
                if (BCrypt.checkpw(password, compatibleHash)) {
                    Log.d(TAG, "BCrypt password match succeeded.")
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "BCrypt verification exception: ${e.message}")
            }
        }

        // 3. MD5 Hashes (32 hex characters)
        val md5Plain = hashString(password, "MD5")
        if (cleanHash.equals(md5Plain, ignoreCase = true)) {
            Log.d(TAG, "MD5 plain match succeeded.")
            return true
        }

        if (username.isNotBlank()) {
            val cleanUser = username.trim()
            val md5UserPass = hashString(cleanUser + password, "MD5")
            if (cleanHash.equals(md5UserPass, ignoreCase = true)) {
                Log.d(TAG, "MD5(username + pass) match succeeded.")
                return true
            }

            val md5PassUser = hashString(password + cleanUser, "MD5")
            if (cleanHash.equals(md5PassUser, ignoreCase = true)) {
                Log.d(TAG, "MD5(pass + username) match succeeded.")
                return true
            }

            val md5Double = hashString(md5Plain + cleanUser, "MD5")
            if (cleanHash.equals(md5Double, ignoreCase = true)) {
                Log.d(TAG, "MD5(MD5(pass) + username) match succeeded.")
                return true
            }

            val md5Double2 = hashString(cleanUser + md5Plain, "MD5")
            if (cleanHash.equals(md5Double2, ignoreCase = true)) {
                Log.d(TAG, "MD5(username + MD5(pass)) match succeeded.")
                return true
            }
        }

        // 4. SHA-1 (40 hex characters)
        val sha1Plain = hashString(password, "SHA-1")
        if (cleanHash.equals(sha1Plain, ignoreCase = true)) {
            Log.d(TAG, "SHA-1 match succeeded.")
            return true
        }

        // 5. MySQL 4.1+ PASSWORD() hash: '*' + SHA1(SHA1(password))
        val mysqlPasswordHash = "*" + hashString(hexStringToByteArray(sha1Plain), "SHA-1").uppercase(Locale.ROOT)
        if (cleanHash.equals(mysqlPasswordHash, ignoreCase = true)) {
            Log.d(TAG, "MySQL PASSWORD() match succeeded.")
            return true
        }

        // 6. SHA-256 (64 hex characters)
        val sha256Plain = hashString(password, "SHA-256")
        if (cleanHash.equals(sha256Plain, ignoreCase = true)) {
            Log.d(TAG, "SHA-256 match succeeded.")
            return true
        }

        // 7. SHA-512 (128 hex characters)
        val sha512Plain = hashString(password, "SHA-512")
        if (cleanHash.equals(sha512Plain, ignoreCase = true)) {
            Log.d(TAG, "SHA-512 match succeeded.")
            return true
        }

        return false
    }

    private fun hashString(input: String, algorithm: String): String {
        return try {
            val digest = MessageDigest.getInstance(algorithm)
            val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            bytesToHex(bytes)
        } catch (_: Exception) {
            ""
        }
    }

    private fun hashString(input: ByteArray, algorithm: String): String {
        return try {
            val digest = MessageDigest.getInstance(algorithm)
            val bytes = digest.digest(input)
            bytesToHex(bytes)
        } catch (_: Exception) {
            ""
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
