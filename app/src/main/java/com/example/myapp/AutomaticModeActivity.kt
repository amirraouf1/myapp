package com.example.sun_position_manual

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class AutoModeActivity : AppCompatActivity() {

    private lateinit var latitudeLabel: TextView
    private lateinit var longitudeLabel: TextView
    private lateinit var dateLabel: TextView
    private lateinit var timeLabel: TextView
    private lateinit var timezoneLabel: TextView
    private lateinit var calculateButton: Button
    private lateinit var resultTextView: TextView

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auto)

        // Initializing views from XML layout
        latitudeLabel = findViewById(R.id.latitude_label)
        longitudeLabel = findViewById(R.id.longitude_label)
        dateLabel = findViewById(R.id.date_label)
        timeLabel = findViewById(R.id.time_label)
        timezoneLabel = findViewById(R.id.timezone_label)
        calculateButton = findViewById(R.id.calculate_button)
        resultTextView = findViewById(R.id.result_text)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }

        // Set click listener for the calculate button
        calculateButton.setOnClickListener {
            if (latitudeLabel.text == "Latitude: Retrieving..." ||
                longitudeLabel.text == "Longitude: Retrieving..." ||
                dateLabel.text == "Date: Retrieving..." ||
                timeLabel.text == "Time: Retrieving..." ||
                timezoneLabel.text == "Time Zone: Retrieving..."
            ) {
                Toast.makeText(this, "Please wait until all data is retrieved", Toast.LENGTH_SHORT).show()
            } else {
                // Call a method to calculate sun position based on retrieved data
                calculateSunPosition()
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    latitudeLabel.text = "Latitude: ${it.latitude}"
                    longitudeLabel.text = "Longitude: ${it.longitude}"

                    val currentDate = Calendar.getInstance().time
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    dateLabel.text = "Date: ${dateFormat.format(currentDate)}"
                    timeLabel.text = "Time: ${timeFormat.format(currentDate)}"

                    val timeZone = TimeZone.getDefault()
                    timezoneLabel.text = "Time Zone: ${timeZone.displayName} (UTC${getTimeZoneOffset(timeZone)})"
                } ?: run {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getTimeZoneOffset(timeZone: TimeZone): String {
        val hours = timeZone.rawOffset / 3600000
        val minutes = (timeZone.rawOffset % 3600000) / 60000
        return String.format("%+02d:%02d", hours, minutes)
    }

    private fun calculateSunPosition() {
        try {
            // Extract latitude, longitude, date, and time
            val latitude = latitudeLabel.text.toString().replace("Latitude: ", "").toDouble()
            val longitude = longitudeLabel.text.toString().replace("Longitude: ", "").toDouble()
            val date = dateLabel.text.toString().replace("Date: ", "")
            val time = timeLabel.text.toString().replace("Time: ", "")
            val timeZoneOffset = getTimeZoneOffset(TimeZone.getDefault())

            // Convert date and time to Calendar instance
            val dateParts = date.split("-").map { it.toInt() }
            val timeParts = time.split(":").map { it.toInt() }
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.set(dateParts[0], dateParts[1] - 1, dateParts[2], timeParts[0], timeParts[1])

            // Adjust for time zone offset
            val timeZoneParts = timeZoneOffset.split(":")
            val timeZoneHour = timeZoneParts[0].toInt()
            val timeZoneMinute = timeZoneParts[1].toInt()
            calendar.add(Calendar.HOUR_OF_DAY, -timeZoneHour)
            calendar.add(Calendar.MINUTE, -timeZoneMinute)

            // Calculate Julian Date
            val julianDate = calculateJulianDate(calendar.time)
            val julianCentury = (julianDate - 2451545.0) / 36525.0

            // Calculate declination and equation of time
            val (declination, eqOfTime) = calculateSolarDeclinationAndEoT(julianCentury)

            // Calculate Solar Time and Hour Angle
            val solarTime = (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) + eqOfTime + (4 * longitude)) / 60.0
            val hourAngle = 15 * (solarTime - 12)

            // Calculate Solar Position (Azimuth and Altitude)
            val (azimuth, altitude) = calculateSolarPosition(latitude, declination, hourAngle)

            // Display the result
            resultTextView.text = "Azimuth: %.2f°, Altitude: %.2f°".format(azimuth, altitude)

        } catch (e: Exception) {
            Toast.makeText(this, "Error calculating sun position: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateJulianDate(date: Date): Double {
        val calendar = Calendar.getInstance()
        calendar.time = date

        var year = calendar.get(Calendar.YEAR)
        var month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is zero-based
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        if (month <= 2) {
            year--
            month += 12
        }

        val a = year / 100
        val b = 2 - a + (a / 4)
        val jd = (365.25 * (year + 4716)).toInt() + (30.6001 * (month + 1)).toInt() + day + b - 1524.5
        val fractionOfDay = (calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0) / 24.0

        return jd + fractionOfDay
    }

    private fun calculateSolarDeclinationAndEoT(julianCentury: Double): Pair<Double, Double> {
        val l0 = (280.46646 + julianCentury * (36000.76983 + julianCentury * 0.0003032)) % 360
        val m = 357.52911 + julianCentury * (35999.05029 - 0.0001537 * julianCentury)
        val e = 0.016708634 - julianCentury * (0.000042037 + 0.0000001267 * julianCentury)

        val c = sin(Math.toRadians(m)) * (1.914602 - julianCentury * (0.004817 + 0.000014 * julianCentury)) +
                sin(Math.toRadians(2 * m)) * (0.019993 - 0.000101 * julianCentury) +
                sin(Math.toRadians(3 * m)) * 0.000289

        val trueLongitude = l0 + c
        val omega = 125.04 - 1934.136 * julianCentury
        val lambda = trueLongitude - 0.00569 - 0.00478 * sin(Math.toRadians(omega))

        val declination = Math.toDegrees(asin(sin(Math.toRadians(lambda)) * sin(Math.toRadians(23.439292 - 0.000013 * julianCentury))))

        val y = tan(Math.toRadians(23.439292 - 0.000013 * julianCentury) / 2).pow(2)
        val eqOfTime = 4 * Math.toDegrees(
            y * sin(2 * Math.toRadians(l0)) -
                    2 * e * sin(Math.toRadians(m)) +
                    4 * e * y * sin(Math.toRadians(m)) * cos(2 * Math.toRadians(l0)) -
                    0.5 * y * y * sin(4 * Math.toRadians(l0)) -
                    1.25 * e * e * sin(2 * Math.toRadians(m))
        )

        return Pair(declination, eqOfTime)
    }

    private fun calculateSolarPosition(latitude: Double, declination: Double, hourAngle: Double): Pair<Double, Double> {
        val latitudeRad = Math.toRadians(latitude)
        val declinationRad = Math.toRadians(declination)
        val hourAngleRad = Math.toRadians(hourAngle)

        val altitude = Math.toDegrees(
            asin(sin(latitudeRad) * sin(declinationRad) +
                    cos(latitudeRad) * cos(declinationRad) * cos(hourAngleRad))
        )

        val azimuth = Math.toDegrees(
            acos(
                (sin(declinationRad) - sin(latitudeRad) * sin(Math.toRadians(altitude))) /
                        (cos(latitudeRad) * cos(Math.toRadians(altitude)))
            )
        )

        return if (hourAngle > 0) Pair(360 - azimuth, altitude) else Pair(azimuth, altitude)
    }
}
