package com.example.model

data class GpsLocation(
    val latitude: Double = 38.7223,
    val longitude: Double = -9.1393,
    val altitude: Double = 45.0,
    val accuracy: Float = 5.0f,
    val speed: Float = 0.0f,
    val street: String = "Avenida da Liberdade",
    val postalCode: String = "1250-096",
    val city: String = "Lisboa",
    val district: String = "Lisboa",
    val country: String = "Portugal",
    val fullAddress: String = "Avenida da Liberdade, Lisboa, Portugal",
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String = "GPS (Fused)",
    val satellitesCount: Int = 14,
    val isGpsEnabled: Boolean = true,
    val hasFix: Boolean = true
) {
    val signalStrength: String
        get() = when {
            accuracy <= 8.0f -> "Excelente (±${accuracy.toInt()}m)"
            accuracy <= 20.0f -> "Bom (±${accuracy.toInt()}m)"
            accuracy <= 50.0f -> "Médio (±${accuracy.toInt()}m)"
            accuracy > 50.0f -> "Fraco (±${accuracy.toInt()}m)"
            else -> "A pesquisar satélites..."
        }

    val streetWithNumber: String
        get() = if (street.isNotBlank()) {
            if (city.isNotBlank() && !street.contains(city)) "$street, $city" else street
        } else {
            fullAddress.ifBlank { "Lat: %.5f, Lon: %.5f".format(latitude, longitude) }
        }

    val mapsUrl: String
        get() = "https://maps.google.com/?q=$latitude,$longitude"
}
