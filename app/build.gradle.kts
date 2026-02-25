import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.navigation.safeargs)
    id("kotlin-parcelize")
}

android {
    namespace = "com.dakotagroupstaff"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dakotagroupstaff"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Read BASE_URL from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }
        val baseUrl = localProperties.getProperty("BASE_URL") ?: "https://stagingdakota.my.id/api/v1/"
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
    }

    signingConfigs {
        create("release") {
            val localProps = Properties()
            val localPropsFile = rootProject.file("local.properties")
            if (localPropsFile.exists()) {
                localPropsFile.inputStream().use { localProps.load(it) }
            }
            
            val storeFilePath = localProps.getProperty("RELEASE_STORE_FILE")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            isMinifyEnabled = false
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
        viewBinding = true
        buildConfig = true
    }
    
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        // Treat warnings as non-fatal for release builds
        warningsAsErrors = false
        // Ignore non-critical warnings for Play Store
        disable += setOf(
            "Deprecation",
            "DefaultLocale",
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "UseAppTint"
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    
    // Dependency Injection - Koin
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    
    // Database - Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Security
    implementation(libs.androidx.security.crypto)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    
    // Image Loading - Glide
    implementation(libs.glide)
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    ksp(libs.glide.compiler)
    
    // PhotoView for zoomable images
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    
    // UI Components
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.shimmer)
    
    // Google Location Services
    implementation(libs.play.services.location)
    
    // Google Sign-In
    implementation(libs.play.services.auth)
    
    // ExifInterface for GPS data
    implementation(libs.androidx.exifinterface)
    
    // LeakCanary
    debugImplementation(libs.leakcanary)
    
    // QR Code - ZXing
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)
    
    // WorkManager for background processing
    implementation(libs.androidx.work.runtime.ktx)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}