import com.android.build.api.variant.BuildConfigField
import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android) // Ensure this uses Kotlin 1.9.24 or latest
    alias(libs.plugins.google.gms.google.services)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" // Specify KSP version
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.verseloom"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.verseloom"
        minSdk = 28
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 1
        versionName = "1.0" // Fixed: proper Kotlin syntax for versionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add your BuildConfig fields here
        buildConfigField("String", "GEMINI_API_KEY",
            "\"${System.getenv("GEMINI_API_KEY") ?: project.properties["GEMINI_API_KEY"] as? String ?: "YOUR_DEFAULT_KEY_HERE"}\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true  // This enables BuildConfig generation
    }
}

dependencies {
    // Firebase
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.google.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.database)
    implementation(libs.google.firebase.firestore)
    implementation(libs.activity)
    implementation(libs.firebase.vertexai)

    // Room
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.53")
    ksp("com.google.dagger:hilt-android-compiler:2.53")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0") // Keep if using Compose

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1") // Update to latest

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0") // Align with runtime-ktx
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")

    // Generative AI

        //implementation("com.google.genai:google-genai:0.1.0")
    implementation(libs.generativeai)

    implementation ("com.google.android.material:material:1.12.0")
    implementation ("com.google.android.material:material:1.12.0-alpha02")




    // AndroidX and Material
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}