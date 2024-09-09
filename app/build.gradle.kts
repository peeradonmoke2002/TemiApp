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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.robotemi:sdk:1.131.4")
//    implementation(files("libs/amqp-client-5.17.1.jar"))
    implementation("com.rabbitmq:amqp-client:4.10.0")
    implementation ("androidx.multidex:multidex:2.0.1")
    implementation ("org.slf4j:slf4j-api:1.7.32")
    implementation ("org.slf4j:slf4j-simple:1.7.32")
}