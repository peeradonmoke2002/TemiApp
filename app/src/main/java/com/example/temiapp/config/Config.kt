package com.example.temiapp.config

object Config {
    // Store the API base URL
    var apiBaseUrl: String = "http://192.168.1.104:3002" // Change this to your actual API base URL

    // Store the RabbitMQ IP address and port
    var rabbitMQHost: String = "192.168.1.104" // Change this to your actual RabbitMQ host
    var rabbitMQPort: Int = 5672
    var rabbitMQUsername: String = "admin" // RabbitMQ username
    var rabbitMQPassword: String = "123456" // RabbitMQ password

    // Additional configurations can go here
}
