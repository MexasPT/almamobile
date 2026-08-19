package com.example.model

data class SmtpConfig(
    val senderName: String = "AlmaForce CRM",
    val senderEmail: String = "notificacoes@almaforce.pt",
    val host: String = "smtp.almaforce.pt",
    val port: String = "587",
    val requireAuth: Boolean = true,
    val securityType: String = "TLS", // "TLS", "SSL", "NONE"
    val username: String = "",
    val password: String = ""
)
