import org.jetbrains.kotlin.gradle.utils.`is`


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    //파이어베이스
    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.todaylunch"
    compileSdk = 34
    viewBinding.isEnabled = true
    dataBinding.isEnabled = true

    defaultConfig {
        applicationId = "com.example.todaylunch"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Location & Maps
    implementation("com.google.android.gms:play-services-location:21.0.1") // 위치 서비스
    implementation("com.google.android.gms:play-services-maps:18.0.2") // Google Maps SDK for Android

    // Android & UI
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.room.ktx)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation(platform("com.google.firebase:firebase-analytics-ktx"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation(platform("com.google.firebase:firebase-database:20.0.3"))

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
