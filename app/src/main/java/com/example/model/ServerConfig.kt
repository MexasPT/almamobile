package com.example.model

import android.util.Base64

data class ServerConfig(
    val ip: String = "94.126.169.16",
    val port: String = "3306",
    val databaseName: String = "almafor3_base",
    val dbUser: String = "almafor3_base",
    val dbPassword: String = "8pO(()(S6EYQ3]",
    val useHttps: Boolean = false,
    val apiKey: String = "XfRn1BEJM6sa4Wmpc3TxEdVqbhYvf07G",
    val apiUser: String = "admin",
    val apiPassword: String = "branco4admin"
) {
    val baseUrl: String
        get() {
            val scheme = if (useHttps) "https" else "http"
            val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
            val hostOnly = cleanIp.split("/")[0].split(":")[0]
            val explicitPort = if (cleanIp.contains(":") && !cleanIp.startsWith("http")) {
                cleanIp.split(":")[1].split("/")[0]
            } else null
            
            // If port is a database port (3306, 3307, 3308), web server runs on standard 80/443
            val webPort = explicitPort ?: if (port != "3306" && port != "3307" && port != "3308" && port != "80" && port != "443" && port.isNotBlank()) port else null
            val pathPart = if (cleanIp.contains("/")) "/" + cleanIp.substringAfter("/") else ""
            
            return if (webPort != null) {
                "$scheme://$hostOnly:$webPort$pathPart"
            } else {
                "$scheme://$hostOnly$pathPart"
            }
        }

    val basicAuthHeader: String
        get() {
            val user = apiUser.ifBlank { "admin" }
            val pass = apiPassword.ifBlank { "branco4admin" }
            val credentials = "$user:$pass"
            val base64 = Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            return "Basic $base64"
        }
}
