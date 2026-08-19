package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "attendance_records")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "ENTRADA" or "SAIDA"
    val collaboratorName: String,
    val collaboratorId: String,
    val companyEmail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val entryTimestamp: Long? = null, // Linked entry time when type is SAIDA
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val streetAddress: String = "",
    val isSynced: Boolean = true,
    val emailSent: Boolean = true
) {
    val formattedTimestamp: String
        get() {
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedEntryTimestamp: String?
        get() {
            if (entryTimestamp == null) return null
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(entryTimestamp))
        }
}
