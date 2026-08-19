package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vtiger_users_cache")
data class User(
    @PrimaryKey val id: String,
    val userName: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val roleName: String = "Comercial",
    val status: String = "Active",
    val phoneMobile: String = "",
    val department: String = "Vendas & Suporte",
    val lastLogin: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() {
            val name = "$firstName $lastName".trim()
            return if (name.isNotBlank()) name else userName
        }
}
