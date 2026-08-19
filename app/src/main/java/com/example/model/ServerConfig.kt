package com.example.model

data class ServerConfig(
    val ip: String = "192.168.1.100",
    val port: String = "80",
    val databaseName: String = "yetiforce",
    val dbUser: String = "yetiforce_user",
    val dbPassword: String = "",
    val useHttps: Boolean = false,
    val apiKey: String = ""
) {
    val baseUrl: String
        get() {
            val scheme = if (useHttps || port == "443") "https" else "http"
            val cleanIp = ip.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
            return if (port.isNotBlank() && port != "80" && port != "443" && !cleanIp.contains(":")) {
                "$scheme://$cleanIp:$port"
            } else {
                "$scheme://$cleanIp"
            }
        }
}
