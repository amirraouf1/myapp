package com.example.sun_position_manual

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the buttons in the layout
        val manualModeButton: Button = findViewById(R.id.button_manual_mode)
        val automaticModeButton: Button = findViewById(R.id.button_automatic_mode)

        // Set click listener for Manual Mode button
        manualModeButton.setOnClickListener {
            startManualMode()
        }

        // Set click listener for Automatic Mode button
        automaticModeButton.setOnClickListener {
            startAutomaticMode()
        }
    }

    // Launch Manual Mode Activity
    private fun startManualMode() {
        val intent = Intent(this, ManualModeActivity::class.java)
        startActivity(intent)
    }

    // Launch Automatic Mode Activity
    private fun startAutomaticMode() {
        val intent = Intent(this, AutoModeActivity::class.java)
        startActivity(intent)
    }
}
