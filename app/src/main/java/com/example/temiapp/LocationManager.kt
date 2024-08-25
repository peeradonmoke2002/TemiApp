package com.example.temiapp

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.robotemi.sdk.Robot

class LocationManager(private val robot: Robot, private val context: Context) {

    fun showSavePositionDialog() {
        val options = arrayOf("Save as Home", "Save as Position A", "Save as Position B")
        val checkedItem = -1  // No item selected by default

        AlertDialog.Builder(context)
            .setTitle("Select where to save the position")
            .setSingleChoiceItems(options, checkedItem) { dialog, which ->
                when (which) {
                    0 -> savePosition("Temi-home")
                    1 -> savePosition("Position A")
                    2 -> savePosition("Position B")
                }
                dialog.dismiss()  // Dismiss the dialog after a choice is made
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun savePosition(locationName: String) {
        val result = robot.saveLocation(locationName)
        if (result) {
            Log.d("LocationManager", "Successfully saved location: $locationName")

            // Show confirmation dialog
            AlertDialog.Builder(context)
                .setTitle("Position Saved")
                .setMessage("Location '$locationName' has been saved.")
                .setPositiveButton("OK", null)
                .show()
        } else {
            Log.e("LocationManager", "Failed to save location: $locationName")

            // Show failure dialog
            AlertDialog.Builder(context)
                .setTitle("Save Failed")
                .setMessage("Failed to save location '$locationName'. Please try again.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    fun goToLocation(location: String) {
        val availableLocations = robot.locations
        if (availableLocations.contains(location)) {
            robot.goTo(location)
            Log.d("LocationManager", "Navigating to location: $location")
        } else {
            Log.e("LocationManager", "Location not found: $location")
            AlertDialog.Builder(context)
                .setTitle("Location Not Found")
                .setMessage("The location '$location' was not found. Please save the location first.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

}
