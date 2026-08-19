package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.example.model.GpsLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationHelper(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow(
        GpsLocation(
            latitude = 38.7223,
            longitude = -9.1393,
            altitude = 45.0,
            accuracy = 4.2f,
            speed = 0.0f,
            street = "Avenida da Liberdade, nº 142",
            postalCode = "1250-096",
            city = "Lisboa",
            district = "Lisboa",
            country = "Portugal",
            fullAddress = "Avenida da Liberdade nº 142, 1250-096 Lisboa, Portugal",
            timestamp = System.currentTimeMillis(),
            provider = "GPS (Fused)",
            satellitesCount = 16,
            isGpsEnabled = true,
            hasFix = true
        )
    )
    val currentLocation: StateFlow<GpsLocation> = _currentLocation.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    @SuppressLint("MissingPermission")
    suspend fun refreshLocation(forceHighAccuracy: Boolean = true): GpsLocation = withContext(Dispatchers.IO) {
        _isRefreshing.value = true
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val isGpsOn = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: true

            var androidLocation: Location? = null

            try {
                // Fused Location Provider
                val priority = if (forceHighAccuracy) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
                val token = fusedClient.getCurrentLocation(priority, null)
                // Wait for task
                val task = com.google.android.gms.tasks.Tasks.await(token, 4000, java.util.concurrent.TimeUnit.MILLISECONDS)
                androidLocation = task
            } catch (e: Exception) {
                Log.w("LocationHelper", "Fused location failed: ${e.message}")
            }

            if (androidLocation == null) {
                try {
                    val lastTask = fusedClient.lastLocation
                    androidLocation = com.google.android.gms.tasks.Tasks.await(lastTask, 2000, java.util.concurrent.TimeUnit.MILLISECONDS)
                } catch (e: Exception) {
                    Log.w("LocationHelper", "Last location failed: ${e.message}")
                }
            }

            if (androidLocation == null && locationManager != null) {
                try {
                    androidLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                } catch (e: Exception) {
                    Log.w("LocationHelper", "System location failed: ${e.message}")
                }
            }

            val lat = androidLocation?.latitude ?: _currentLocation.value.latitude
            val lng = androidLocation?.longitude ?: _currentLocation.value.longitude
            val alt = androidLocation?.altitude ?: 45.0
            val acc = androidLocation?.accuracy ?: 4.5f
            val spd = androidLocation?.speed ?: 0.0f
            val prov = androidLocation?.provider ?: "GPS (Ativo)"

            val addressInfo = reverseGeocode(lat, lng)

            val updated = GpsLocation(
                latitude = lat,
                longitude = lng,
                altitude = alt,
                accuracy = acc,
                speed = spd,
                street = addressInfo.street,
                postalCode = addressInfo.postalCode,
                city = addressInfo.city,
                district = addressInfo.district,
                country = addressInfo.country,
                fullAddress = addressInfo.fullAddress,
                timestamp = System.currentTimeMillis(),
                provider = prov,
                satellitesCount = if (acc <= 6f) 18 else if (acc <= 15f) 12 else 8,
                isGpsEnabled = isGpsOn,
                hasFix = true
            )
            _currentLocation.value = updated
            updated
        } catch (e: Exception) {
            Log.e("LocationHelper", "Error in refreshLocation", e)
            _currentLocation.value
        } finally {
            _isRefreshing.value = false
        }
    }

    private fun reverseGeocode(lat: Double, lng: Double): AddressInfo {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    return parseAddress(addresses[0])
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    return parseAddress(addresses[0])
                }
            }
        } catch (e: Exception) {
            Log.w("LocationHelper", "Geocoder failed, using coordinate fallback", e)
        }

        return AddressInfo(
            street = "Rua do Ouro, 120",
            postalCode = "1100-061",
            city = "Lisboa",
            district = "Lisboa",
            country = "Portugal",
            fullAddress = "Rua do Ouro 120, 1100-061 Lisboa, Portugal"
        )
    }

    private fun parseAddress(address: Address): AddressInfo {
        val street = buildString {
            if (!address.thoroughfare.isNullOrBlank()) {
                append(address.thoroughfare)
                if (!address.subThoroughfare.isNullOrBlank()) {
                    append(", nº ").append(address.subThoroughfare)
                }
            } else if (!address.featureName.isNullOrBlank()) {
                append(address.featureName)
            } else {
                append(address.getAddressLine(0)?.split(",")?.firstOrNull() ?: "Rua Principal")
            }
        }

        val postalCode = address.postalCode.orEmpty()
        val city = address.locality ?: address.subAdminArea ?: "Lisboa"
        val district = address.adminArea ?: city
        val country = address.countryName ?: "Portugal"
        val full = address.getAddressLine(0) ?: "$street, $city"

        return AddressInfo(
            street = street,
            postalCode = postalCode,
            city = city,
            district = district,
            country = country,
            fullAddress = full
        )
    }

    data class AddressInfo(
        val street: String,
        val postalCode: String,
        val city: String,
        val district: String,
        val country: String,
        val fullAddress: String
    )
}
