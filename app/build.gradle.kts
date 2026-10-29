plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.marco.mlaerei"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.marco.mlaerei"
        minSdk = 26
        targetSdk = 34
        versionCode = runNumber
        versionName = "1.0.$runNumber"
    }

    // Chiave di firma di test inclusa nel repo (app/debug.keystore): tiene la firma
    // uguale a ogni build, così l'APK si aggiorna sopra la versione già installata.
    // Se nel build ci sono i dati della chiave privata (KEYSTORE_PATH, KEYSTORE_PASSWORD, impostati
    // da GitHub Actions con i secrets) si firma con quella; altrimenti con la chiave di test.
    val releaseKeystorePath: String? = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
    val releaseKeystorePassword: String? = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
    val useReleaseKey = releaseKeystorePath != null && releaseKeystorePassword != null && file(releaseKeystorePath).exists()

    signingConfigs {
        create("shared") {
            if (useReleaseKey) {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = "mlskywatch"
                keyPassword = releaseKeystorePassword
            } else {
                storeFile = file("debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
