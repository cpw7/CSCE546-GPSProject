package com.cpw.gpsproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var locationManager: LocationManager
    private lateinit var statusText: TextView
    private lateinit var locationText: TextView
    private lateinit var geofenceText: TextView
    private lateinit var radiusInput: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private var currentLocation: Location? = null
    private var fenceLatitude: Double? = null
    private var fenceLongitude: Double? = null
    private var receivingUpdates = false

    private val locationListener = LocationListener { location ->
        updateLocation(location, "Live GPS update received.")
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            statusText.text = "Location permission granted. Set an emulator GPS location, then tap Get Current Location or Start GPS Updates."
            getLastKnownLocation()
        } else {
            statusText.text = "Location permission denied. The app needs location permission to read GPS data."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        buildScreen()
    }

    private fun buildScreen() {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(18, 18, 18))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 32)
        }

        val title = TextView(this).apply {
            text = "GPS Project"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Reads GPS/location data, shows latitude and longitude, and checks a simple boundary area."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        root.addView(subtitle)

        statusText = makeBox("Status: Waiting for location permission.")
        root.addView(statusText)

        locationText = makeBox("Latitude: --\nLongitude: --\nAccuracy: --\nAltitude: --\nSpeed: --\nBearing: --\nProvider: --\nTime: --")
        root.addView(locationText)

        val radiusLabel = TextView(this).apply {
            text = "Geofence radius in meters"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 18, 0, 4)
        }
        root.addView(radiusLabel)

        radiusInput = EditText(this).apply {
            setText("100")
            textSize = 18f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.rgb(45, 45, 45))
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(18, 12, 18, 12)
        }
        root.addView(radiusInput)

        geofenceText = makeBox("Geofence: No center set yet. Tap Set Boundary Center Here after receiving a location.")
        root.addView(geofenceText)

        val buttonRow1 = makeButtonRow()
        buttonRow1.addView(makeButton("REQUEST PERMISSION") {
            requestLocationPermission()
        })
        buttonRow1.addView(makeButton("GET CURRENT LOCATION") {
            runWithPermission { getLastKnownLocation() }
        })
        root.addView(buttonRow1)

        val buttonRow2 = makeButtonRow()
        startButton = makeButton("START GPS UPDATES") {
            runWithPermission { startLocationUpdates() }
        }
        stopButton = makeButton("STOP GPS UPDATES") {
            stopLocationUpdates()
        }
        buttonRow2.addView(startButton)
        buttonRow2.addView(stopButton)
        root.addView(buttonRow2)

        val buttonRow3 = makeButtonRow()
        buttonRow3.addView(makeButton("SET BOUNDARY CENTER HERE") {
            setFenceCenter()
        })
        buttonRow3.addView(makeButton("OPEN LOCATION SETTINGS") {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        })
        root.addView(buttonRow3)

        val directions = TextView(this).apply {
            text = "Emulator testing: click the emulator's three-dot menu, open Location, enter a latitude/longitude, and click Set Location. Then return to this app and press Get Current Location or Start GPS Updates."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 22, 0, 0)
        }
        root.addView(directions)

        scrollView.addView(root)
        setContentView(scrollView)
    }

    private fun makeBox(message: String): TextView {
        return TextView(this).apply {
            text = message
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(35, 35, 35))
            setPadding(20, 18, 20, 18)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 10, 0, 10)
            layoutParams = params
        }
    }

    private fun makeButtonRow(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)
            layoutParams = params
        }
    }

    private fun makeButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(80, 80, 85))
            setOnClickListener { action() }
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            params.setMargins(8, 4, 8, 4)
            layoutParams = params
        }
    }

    private fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun runWithPermission(action: () -> Unit) {
        if (hasLocationPermission()) {
            action()
        } else {
            statusText.text = "Location permission is required first."
            requestLocationPermission()
        }
    }

    private fun enabledLocationProviders(): List<String> {
        return locationManager.getProviders(true).filter { provider ->
            provider == LocationManager.GPS_PROVIDER || provider == LocationManager.NETWORK_PROVIDER
        }
    }

    private fun getLastKnownLocation() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        try {
            val providers = enabledLocationProviders()
            if (providers.isEmpty()) {
                statusText.text = "No location provider is enabled. Open Location Settings or set an emulator GPS location."
                return
            }

            val bestLocation = providers
                .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
                .maxByOrNull { location -> location.time }

            if (bestLocation != null) {
                updateLocation(bestLocation, "Current location loaded from ${bestLocation.provider}.")
            } else {
                statusText.text = "No saved location yet. Set an emulator GPS location and press Start GPS Updates."
            }
        } catch (exception: SecurityException) {
            statusText.text = "Location permission error: ${exception.message}"
        }
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        try {
            val providers = enabledLocationProviders()
            if (providers.isEmpty()) {
                statusText.text = "No location provider is enabled. Open Location Settings or set an emulator GPS location."
                return
            }

            for (provider in providers) {
                locationManager.requestLocationUpdates(
                    provider,
                    2000L,
                    0f,
                    locationListener,
                    Looper.getMainLooper()
                )
            }

            receivingUpdates = true
            startButton.isEnabled = false
            stopButton.isEnabled = true
            statusText.text = "Receiving GPS/location updates. Change the emulator location to see the values update."
        } catch (exception: SecurityException) {
            statusText.text = "Location permission error: ${exception.message}"
        }
    }

    private fun stopLocationUpdates() {
        if (receivingUpdates) {
            locationManager.removeUpdates(locationListener)
            receivingUpdates = false
            startButton.isEnabled = true
            stopButton.isEnabled = true
            statusText.text = "GPS/location updates stopped."
        } else {
            statusText.text = "GPS/location updates are not currently running."
        }
    }

    private fun updateLocation(location: Location, statusMessage: String) {
        currentLocation = location

        val altitude = if (location.hasAltitude()) {
            String.format(Locale.US, "%.1f m", location.altitude)
        } else {
            "Not available"
        }

        val speed = if (location.hasSpeed()) {
            String.format(Locale.US, "%.2f m/s", location.speed)
        } else {
            "Not available"
        }

        val bearing = if (location.hasBearing()) {
            String.format(Locale.US, "%.1f degrees", location.bearing)
        } else {
            "Not available"
        }

        val accuracy = if (location.hasAccuracy()) {
            String.format(Locale.US, "%.1f m", location.accuracy)
        } else {
            "Not available"
        }

        val timeText = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date(location.time))

        locationText.text = buildString {
            appendLine("Latitude: ${String.format(Locale.US, "%.6f", location.latitude)}")
            appendLine("Longitude: ${String.format(Locale.US, "%.6f", location.longitude)}")
            appendLine("Accuracy: $accuracy")
            appendLine("Altitude: $altitude")
            appendLine("Speed: $speed")
            appendLine("Bearing: $bearing")
            appendLine("Provider: ${location.provider}")
            append("Time: $timeText")
        }

        statusText.text = statusMessage
        checkFenceStatus()
    }

    private fun setFenceCenter() {
        val location = currentLocation
        if (location == null) {
            statusText.text = "Get a GPS/location reading before setting the boundary center."
            return
        }

        fenceLatitude = location.latitude
        fenceLongitude = location.longitude
        statusText.text = "Boundary center set to the current GPS location."
        checkFenceStatus()
    }

    private fun checkFenceStatus() {
        val location = currentLocation
        val centerLat = fenceLatitude
        val centerLon = fenceLongitude

        if (location == null || centerLat == null || centerLon == null) {
            geofenceText.text = "Geofence: No center set yet. Tap Set Boundary Center Here after receiving a location."
            return
        }

        val radiusMeters = radiusInput.text.toString().toFloatOrNull()?.coerceAtLeast(1f) ?: 100f
        val results = FloatArray(1)
        Location.distanceBetween(
            centerLat,
            centerLon,
            location.latitude,
            location.longitude,
            results
        )

        val distance = results[0]
        val inside = distance <= radiusMeters
        val insideText = if (inside) "INSIDE" else "OUTSIDE"

        geofenceText.text = buildString {
            appendLine("Geofence status: $insideText boundary")
            appendLine("Boundary center: ${String.format(Locale.US, "%.6f", centerLat)}, ${String.format(Locale.US, "%.6f", centerLon)}")
            appendLine("Radius: ${String.format(Locale.US, "%.1f", radiusMeters)} m")
            append("Distance from center: ${String.format(Locale.US, "%.1f", distance)} m")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(locationListener)
        } catch (_: Exception) {
            // Nothing else needed during shutdown.
        }
    }
}
