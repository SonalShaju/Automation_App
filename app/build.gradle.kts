import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "com.example.automationapp"
    compileSdk = 36

    room {
        schemaDirectory("$projectDir/schemas")
    }

    defaultConfig {
        applicationId = "com.example.automationapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    configurations {
        create("cleanedAnnotations")
        implementation {
            exclude(group = "org.jetbrains", module = "annotations")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.work.compiler)

    // Room
    implementation(libs.bundles.room)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.room.compiler)
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Location Services
    implementation(libs.play.services.location)


    // Coroutines
    implementation(libs.bundles.coroutines)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // OpenStreetMap (osmdroid) for Map Picker
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Testing
    testImplementation(libs.bundles.testing)

    // Android Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.android.testing)
    androidTestImplementation(libs.androidx.work.testing)
}