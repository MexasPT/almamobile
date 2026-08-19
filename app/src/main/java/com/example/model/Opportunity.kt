package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "opportunities")
data class Opportunity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "Cliente" or "Lead"
    val clientId: String? = null,
    val clientName: String? = null,
    val leadCompany: String? = null,
    val subject: String, // "AlmaForce", "Faturação", "Outros"
    val customSubject: String? = null,
    val observations: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val streetAddress: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = true,
    val emailSent: Boolean = true
) {
    val displayEntityName: String
        get() = if (type == "Cliente") (clientName ?: "Cliente não especificado") else (leadCompany ?: "Lead não especificada")

    val finalSubject: String
        get() = if (subject == "Outros" && !customSubject.isNullOrBlank()) customSubject else subject
}
