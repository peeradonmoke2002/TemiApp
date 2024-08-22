package com.example.temiapp

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnRobotReadyListener
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnLocationsUpdatedListener
import com.robotemi.sdk.TtsRequest

class MainActivity : AppCompatActivity(), OnRobotReadyListener, OnGoToLocationStatusChangedListener {

    private lateinit var robot: Robot
    private lateinit var btnGoHome: Button
    private lateinit var btnGoPositionA: Button
    private lateinit var btnGoPositionB: Button
    private lateinit var txtHomePosition: TextView
    private lateinit var txtPositionA: TextView
    private lateinit var txtPositionB: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the Temi Robot instance
        robot = Robot.getInstance()

        // Initialize UI elements
        btnGoHome = findViewById(R.id.btnGoHome)
        btnGoPositionA = findViewById(R.id.btnGoPositionA)
        btnGoPositionB = findViewById(R.id.btnGoPositionB)
        txtHomePosition = findViewById(R.id.txtHomePosition)
        txtPositionA = findViewById(R.id.txtPositionA)
        txtPositionB = findViewById(R.id.txtPositionB)

        // Initially disable the buttons
        btnGoHome.isEnabled = false
        btnGoPositionA.isEnabled = false
        btnGoPositionB.isEnabled = false

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.btnSavePosition).setOnClickListener {
            showSavePositionDialog()
        }

        btnGoHome.setOnClickListener {
            goToLocation("home")
        }

        btnGoPositionA.setOnClickListener {
            goToLocation("Position A")
        }

        btnGoPositionB.setOnClickListener {
            goToLocation("Position B")
        }

        // Register listeners
        robot.addOnGoToLocationStatusChangedListener(this)
    }

    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            // Robot is ready, you can interact with it
            robot.speak(TtsRequest.create("Hello, I am ready!", false))
        }
    }

    private fun showSavePositionDialog() {
        val options = arrayOf("Save as Home", "Save as Position A", "Save as Position B")
        val checkedItem = -1  // No item selected by default

        AlertDialog.Builder(this)
            .setTitle("Select where to save the position")
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                when (which) {
                    0 -> savePosition("home")
                    1 -> savePosition("Position A")
                    2 -> savePosition("Position B")
                }
                dialog.dismiss()  // Dismiss the dialog after a choice is made
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun savePosition(locationName: String) {
        // Save the current location with the specified name
        robot.saveLocation(locationName)

        // Assuming X and Y coordinates are associated with the name, you can update the UI
        when (locationName) {
            "home" -> {
                txtHomePosition.text = "Home Position Saved"
                txtHomePosition.visibility = View.VISIBLE
                btnGoHome.isEnabled = true
            }
            "Position A" -> {
                txtPositionA.text = "Position A Saved"
                txtPositionA.visibility = View.VISIBLE
                btnGoPositionA.isEnabled = true
            }
            "Position B" -> {
                txtPositionB.text = "Position B Saved"
                txtPositionB.visibility = View.VISIBLE
                btnGoPositionB.isEnabled = true
            }
        }

        // Show confirmation dialog
        AlertDialog.Builder(this)
            .setTitle("Position Saved")
            .setMessage("Location '$locationName' has been saved.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun goToLocation(location: String) {
        robot.goTo(location)
    }

    override fun onGoToLocationStatusChanged(location: String, status: String, descriptionId: Int, description: String) {
        Log.d("TemiApp", "Location: $location, Status: $status")
        // You can update UI based on the status, like showing a message when the robot reaches the location
    }
}
