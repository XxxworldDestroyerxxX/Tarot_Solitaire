plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tarotsolitaire"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.tarotsolitaire"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)


    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // BOM
    implementation(platform(libs.firebase.bom))


// Firebase (ללא גרסאות)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.ai)


// One-shot (Guava ListenableFuture)
    implementation(libs.guava)


// Streaming (Reactive Streams Publisher)


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// Apply the Google Services plugin to process google-services.json
apply(plugin = "com.google.gms.google-services")
