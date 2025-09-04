package com.example.temiapp.controller

import android.content.Context
import android.content.Intent
import com.robotemi.sdk.Robot
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.temiapp.MainActivity

class RobotController(private val robot: Robot, private val mainActivity: MainActivity) {

    private var x = 0f  // Variable to store forward/backward movement
    private var y = 0f  // Variable to store left/right movement
    private val handler = Handler(Looper.getMainLooper())

    // Define commands as constants for safety and clarity
    companion object {
        const val STOP_COMMAND = "X"
        const val STOP_COMMAND_ALT1 = "Z"
        const val STOP_COMMAND_ALT2 = "C"
        const val MOVE_FORWARD = "MOVE_FORWARD"
        const val MOVE_BACKWARD = "MOVE_BACKWARD"
        const val MOVE_LEFT = "MOVE_LEFT"
        const val MOVE_RIGHT = "MOVE_RIGHT"
        const val HEAD_UP = "HEAD_UP"
        const val HEAD_DOWN = "HEAD_DOWN"
    }

    // Method to handle RabbitMQ messages and control the robot accordingly
    fun handleRabbitMqControllMessage(message: String) {
        handler.post {
            when (message) {
                STOP_COMMAND, STOP_COMMAND_ALT1, STOP_COMMAND_ALT2 -> {
                    stopMovement()  // Instantly stop the robot
                    return@post
                }
                // Move forward and reset `y`
                MOVE_FORWARD -> {
                    x = if (y != 0f) 0.3f else 0.4f
                    y = 0f // Ensure no side movement
                }
                // Move backward and reset `y`
                MOVE_BACKWARD -> {
                    x = if (y != 0f) -0.5f else -0.6f
                    y = 0f // Ensure no side movement
                }
                // Turn left
                MOVE_LEFT -> y = if (x < 0) -1f else 1f

                // Turn right
                MOVE_RIGHT -> y = if (x < 0) 1f else -1f

                // Handle unknown commands safely
                else -> {
                    Log.e("RobotController", "Unknown control command received: $message")
                    return@post
                }
            }
            controlRobotMovement(x, y)
            // Reset inactivity timeout after controlling the robot
            mainActivity.resetInactivityTimeout()
            Log.d("RobotController", "Controlling movement after command: $message")
        }
    }

    // Separate method to handle head control commands
    fun handleRabbitMqHeadControllMessage(message: String) {
        handler.post {
            when (message) {
                HEAD_UP -> {
                    robot.tiltAngle(55, 1f)
                    changeToMainMenu()
                    Log.d("RobotController", "Tilted head up")
                }
                HEAD_DOWN -> {
                    robot.tiltAngle(0, 1f)
                    changeTemiFace()
                    Log.d("RobotController", "Tilted head down")
                }
                else -> {
                    Log.e("RobotController", "Unknown head control command received: $message")
                }
            }
        }
    }

    private fun changeToMainMenu() {
        // Use mainActivity as the context to start the activity
        val intent = Intent(mainActivity, MainActivity::class.java)
        mainActivity.startActivity(intent)
        mainActivity.finish()
    }

    private fun changeTemiFace() {
        Log.d("MainActivity", "changeTemiFace called")
        // Use mainActivity as the context to start the VideoActivity
        val intent = Intent(mainActivity, com.example.temiapp.ui.VideoActivity::class.java)
        mainActivity.startActivity(intent)
    }

    // Method to stop the robot's movement instantly
    private fun stopMovement() {
        robot.stopMovement()
        x = 0f
        y = 0f
        Log.d("RobotController", "Robot fully stopped")
    }

    // Method to control the robot's movement
    private fun controlRobotMovement(x: Float, y: Float) {
        robot.skidJoy(x, y, false)
        Log.d("RobotController", "Controlling movement X: $x, Y: $y")
    }
}
