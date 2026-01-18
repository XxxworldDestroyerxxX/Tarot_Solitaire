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

    // Firebase BOM manages versions for Firebase libraries
    implementation(platform("com.google.firebase:firebase-bom:32.2.0"))

    // Use non-KTX (Java) Firebase SDKs to avoid introducing Kotlin runtime
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// Apply the Google Services plugin to process google-services.json
apply(plugin = "com.google.gms.google-services")
