plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.temiapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.temiapp"
        minSdk = 23
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    viewBinding {
        enable = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    // Core AndroidX libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.service)

    // ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-rtsp:1.4.1")
    implementation("androidx.media3:media3-common:1.4.1")

    // Test libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // RabbitMQ and WebRTC libraries
    implementation("com.rabbitmq:amqp-client:4.10.0")
    implementation("io.getstream:stream-webrtc-android:1.1.3")
    implementation("io.getstream:stream-webrtc-android-ui:1.1.3")

    // Multidex for support on lower versions
    implementation("androidx.multidex:multidex:2.0.1")

    // SLF4J Logging
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("org.slf4j:slf4j-simple:1.7.32")

    // Temi SDK (only one implementation should be uncommented)
    implementation("com.robotemi:sdk:1.131.4")

    // Retrofit dependencies for API calls
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // ViewPager and Fragment libraries
    implementation("androidx.viewpager:viewpager:1.0.0")
    implementation("androidx.fragment:fragment:1.3.6")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.arthenica:mobile-ffmpeg-full-gpl:4.4.LTS")
}
