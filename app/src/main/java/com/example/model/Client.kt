package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vtiger_account_cache")
data class Client(
    @PrimaryKey val id: String,
    val accountName: String,
    val phone: String = "",
    val email: String = "",
    val city: String = "",
    val address: String = "",
    val industry: String = "Geral",
    val vatNumber: String = ""
)
