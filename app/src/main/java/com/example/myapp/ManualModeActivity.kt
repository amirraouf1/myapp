package com.example.sun_position_manual

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import kotlin.math.*

class ManualModeActivity : AppCompatActivity() {

    private lateinit var latitudeDegreeInput: EditText
    private lateinit var latitudeMinuteInput: EditText
    private lateinit var latitudeSecondInput: EditText
    private lateinit var latitudeDirectionSpinner: Spinner
    private lateinit var longitudeDegreeInput: EditText
    private lateinit var longitudeMinuteInput: EditText
    private lateinit var longitudeSecondInput: EditText
    private lateinit var longitudeDirectionSpinner: Spinner
    private lateinit var dateInput: EditText
    private lateinit var timeInput: EditText
    private lateinit var timezoneHourSpinner: Spinner
    private lateinit var timezoneMinuteSpinner: Spinner
    private lateinit var calculateButton: Button
    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)

        // Initializing the views from the XML layout
        latitudeDegreeInput = findViewById(R.id.latitude_degree_input)
        latitudeMinuteInput = findViewById(R.id.latitude_minute_input)
        latitudeSecondInput = findViewById(R.id.latitude_second_input)
        latitudeDirectionSpinner = findViewById(R.id.latitude_direction_spinner)
        longitudeDegreeInput = findViewById(R.id.longitude_degree_input)
        longitudeMinuteInput = findViewById(R.id.longitude_minute_input)
        longitudeSecondInput = findViewById(R.id.longitude_second_input)
        longitudeDirectionSpinner = findViewById(R.id.longitude_direction_spinner)
        dateInput = findViewById(R.id.date_input)
        timeInput = findViewById(R.id.time_input)
        timezoneHourSpinner = findViewById(R.id.timezone_hour_spinner)
        timezoneMinuteSpinner = findViewById(R.id.timezone_minute_spinner)
        calculateButton = findViewById(R.id.calculate_button)
        resultTextView = findViewById(R.id.result_text)

        // Set up date picker for date input
        dateInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                dateInput.setText("$selectedYear-${selectedMonth + 1}-$selectedDay")
            }, year, month, day)
            datePickerDialog.show()
        }

        // Set up time picker for time input
        timeInput.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                timeInput.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
            }, hour, minute, true)
            timePickerDialog.show()
        }

        // Set up time zone spinners
        val hourOptions = (-12..14).toList().map { it.toString() }
        val minuteOptions = listOf("0", "15", "30", "45")

        val hourAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hourOptions)
        hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timezoneHourSpinner.adapter = hourAdapter

        val minuteAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, minuteOptions)
        minuteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timezoneMinuteSpinner.adapter = minuteAdapter

        // Set click listener for the calculate button
        calculateButton.setOnClickListener {
            // Call the method to calculate sun position here
            calculateSunPosition()
        }
    }

    private fun calculateSunPosition() {
        try {
            // Fetching latitude input in degrees, minutes, seconds
            val latitudeDegree = latitudeDegreeInput.text.toString().toDoubleOrNull() ?: 0.0
            val latitudeMinute = latitudeMinuteInput.text.toString().toDoubleOrNull() ?: 0.0
            val latitudeSecond = latitudeSecondInput.text.toString().toDoubleOrNull() ?: 0.0
            val latitudeDirection = latitudeDirectionSpinner.selectedItem.toString()

            // Fetching longitude input in degrees, minutes, seconds
            val longitudeDegree = longitudeDegreeInput.text.toString().toDoubleOrNull() ?: 0.0
            val longitudeMinute = longitudeMinuteInput.text.toString().toDoubleOrNull() ?: 0.0
            val longitudeSecond = longitudeSecondInput.text.toString().toDoubleOrNull() ?: 0.0
            val longitudeDirection = longitudeDirectionSpinner.selectedItem.toString()

            // Fetching other input values
            val date = dateInput.text.toString() // Format: yyyy-MM-dd
            val time = timeInput.text.toString() // Format: HH:mm
            val timezoneHour = timezoneHourSpinner.selectedItem.toString().toInt()
            val timezoneMinute = timezoneMinuteSpinner.selectedItem.toString().toInt()

            // Calculate time zone offset in hours
            val timeZoneOffset = timezoneHour + timezoneMinute / 60.0

            // Convert latitude and longitude to decimal degrees
            val latitude = convertToDecimalDegrees(latitudeDegree, latitudeMinute, latitudeSecond, latitudeDirection)
            val longitude = convertToDecimalDegrees(longitudeDegree, longitudeMinute, longitudeSecond, longitudeDirection)

            // Convert date and time to Calendar instance
            val dateParts = date.split("-").map { it.toInt() }
            val timeParts = time.split(":").map { it.toInt() }
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.set(dateParts[0], dateParts[1] - 1, dateParts[2], timeParts[0], timeParts[1])
            calendar.add(Calendar.HOUR_OF_DAY, -timeZoneOffset.toInt())
            calendar.add(Calendar.MINUTE, -timezoneMinute)

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
            Toast.makeText(this, "Please enter valid values", Toast.LENGTH_SHORT).show()
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

    private fun convertToDecimalDegrees(degree: Double, minute: Double, second: Double, direction: String): Double {
        var decimalDegree = degree + (minute / 60) + (second / 3600)
        if (direction == "S" || direction == "W") {
            decimalDegree *= -1
        }
        return decimalDegree
    }
}
