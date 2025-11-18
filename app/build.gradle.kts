plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "vn.haui.heartlink"
    compileSdk = 36

    defaultConfig {
        applicationId = "vn.haui.heartlink"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true // Enable Multi-Dex

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"dmpjioe8c\"")
            buildConfigField("String", "CLOUDINARY_API_KEY", "\"721764866794639\"")
            buildConfigField("String", "CLOUDINARY_API_SECRET", "\"A8CRHQ_tvjiMVygd0Uo0S9kkNW8\"")
        }
        debug {
            buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"dmpjioe8c\"")
            buildConfigField("String", "CLOUDINARY_API_KEY", "\"721764866794639\"")
            buildConfigField("String", "CLOUDINARY_API_SECRET", "\"A8CRHQ_tvjiMVygd0Uo0S9kkNW8\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
    buildToolsVersion = "36.0.0"
    ndkVersion = "29.0.14033849 rc4"
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.multidex:multidex:2.0.1") // Add Multi-Dex library
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("com.github.yuyakaido:CardStackView:v2.3.4")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.cloudinary.core)
    implementation(libs.play.services.auth)

    // Emoji
    implementation("com.vanniktech:emoji-google:0.8.0")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.preference:preference:1.2.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}