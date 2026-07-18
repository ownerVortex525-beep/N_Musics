plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)  // ← ADDED: Compose plugin
}

android {
    namespace = "com.n.musics"
    compileSdk = 35  // ← CHANGED: 36 se 35 (stable)

    defaultConfig {
        applicationId = "com.n.musics"
        minSdk = 26  // ← CHANGED: 35 se 26 (Android 8.0+)
        targetSdk = 35  // ← CHANGED: 34 se 35
        versionCode = 1
        versionName = "1.0"
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

    buildFeatures {
        compose = true  // ← CHANGED: viewBinding se compose
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Media3 ExoPlayer (Music Player)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    
    // Coil (Image Loading for Album Art)
    implementation(libs.coil.compose)
    
    // Socket.io (Real-time Connection)
    implementation(libs.socket.io)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    
    // Debug Tools
    debugImplementation(libs.androidx.compose.ui.tooling)
}