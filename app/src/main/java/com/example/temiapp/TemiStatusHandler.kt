package com.example.temiapp

import android.util.Log
import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnLocationsUpdatedListener

class TemiStatusHandler(private val robot: Robot) : OnGoToLocationStatusChangedListener, OnLocationsUpdatedListener {

    init {
        robot.addOnGoToLocationStatusChangedListener(this)
        robot.addOnLocationsUpdatedListener(this)
    }

    override fun onGoToLocationStatusChanged(location: String, status: String, descriptionId: Int, description: String) {
        Log.d("TemiStatusHandler", "GoToStatusChanged: status=$status, descriptionId=$descriptionId, description=$description")
        robot.speak(TtsRequest.create(status, false))
        if (description.isNotBlank()) {
            robot.speak(TtsRequest.create(description, false))
        }
    }

    override fun onLocationsUpdated(locations: List<String>) {
        Log.d("TemiStatusHandler", "Locations updated: $locations")
    }

    fun cleanUp() {
        robot.removeOnGoToLocationStatusChangedListener(this)
    }
}
