package com.example.temiapp.controller

import com.robotemi.sdk.Robot
import android.os.Handler
import android.os.Looper

class RobotController(private val robot: Robot) {

    private var x = 0f  // Variable to store forward/backward movement
    private var y = 0f  // Variable to store left/right movement

    // Method to handle RabbitMQ messages and control the robot accordingly
    fun handleRabbitMqMessage(message: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            when (message) {
                "X", "Z", "C" -> {
                    stopMovement()
                    return@post
                }
                "MOVE_FORWARD_SMART" -> updateMovementValues(0.5f, 0.6f)
                "MOVE_BACKWARD_SMART" -> updateMovementValues(-0.4f, -0.6f)
                "MOVE_LEFT_SMART" -> updateDirectionValues(-1f, 1f)
                "MOVE_RIGHT_SMART" -> updateDirectionValues(1f, -1f)

                "MOVE_FORWARD_NON_SMART" -> updateMovementValues(0.5f, 0.6f)
                "MOVE_BACKWARD_NON_SMART" -> updateMovementValues(-0.4f, -0.6f)
                "MOVE_LEFT_NON_SMART" -> updateDirectionValues(-1f, 1f)
                "MOVE_RIGHT_NON_SMART" -> updateDirectionValues(1f, -1f)

                "W_UP", "S_UP" -> x = 0f  // Stop forward/backward movement
                "A_UP", "D_UP" -> y = 0f  // Stop left/right movement
            }

            // Check if it's a smart movement command
            val isSmartMovement = message.contains("SMART")
            controlRobotMovement(x, y, isSmartMovement)
        }
    }

    // Method to stop the robot's movement
    private fun stopMovement() {
        robot.stopMovement()
    }

    // Method to update movement values (forward/backward)
    private fun updateMovementValues(nonZeroValue: Float, zeroValue: Float) {
        x = if (y != 0f) nonZeroValue else zeroValue
    }

    // Method to update direction values (left/right)
    private fun updateDirectionValues(negativeValue: Float, positiveValue: Float) {
        y = if (x < 0) negativeValue else positiveValue
    }

    // Method to control the robot's movement
    private fun controlRobotMovement(x: Float, y: Float, isSmartMovement: Boolean) {
        robot.skidJoy(x, y, isSmartMovement)
    }
}
