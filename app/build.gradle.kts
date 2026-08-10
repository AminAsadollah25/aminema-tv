import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.amin.tvos"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.amin.tvos"
        minSdk = 28          // Android 9+
        targetSdk = 35
        versionCode = 41
        versionName = "0.17.1"
    }

    androidResources {
        // Aminema currently ships only Persian/English UI. Avoid packaging the
        // transitive libraries' unused locale resources on low-storage TV boxes.
        localeFilters += listOf("en", "fa")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // The TV installs the debug-signed update channel today. Give that APK
            // the same code/resource shrinking as release while preserving its
            // package id and signing continuity for one-click updates.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Configure your own keystore for release signing:
            // signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // The Compose runtime-lint 1.7.2 artifact is binary-incompatible with Lint 31.13.2:
        // multiple detectors crash before reporting any issue. Disable only that artifact's
        // 14 checks; Android/UI/Material/Navigation lint remains enabled. Remove this list
        // after the planned, separately-tested Compose runtime upgrade.
        disable += setOf(
            "AutoboxingStateValueProperty",
            "AutoboxingStateCreation",
            "CoroutineCreationDuringComposition",
            "FlowOperatorInvokedInComposition",
            "ComposableLambdaParameterNaming",
            "ComposableLambdaParameterPosition",
            "ComposableNaming",
            "StateFlowValueCalledInComposition",
            "CompositionLocalNaming",
            "MutableCollectionMutableState",
            "ProduceStateDoesNotAssignValue",
            "RememberReturnType",
            "OpaqueUnitKey",
            "UnrememberedMutableState"
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Poster/image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Persistence
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Modern WebView helpers
    implementation("androidx.webkit:webkit:1.12.0")

    testImplementation("junit:junit:4.13.2")
}
