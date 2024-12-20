import org.jetbrains.kotlin.gradle.utils.`is`
import java.util.Properties


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
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").reader())
        manifestPlaceholders["MAPS_API_KEY"] = properties.getProperty("MAPS_API_KEY", "")


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // local.properties에서 API 키 읽기

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
    // for 사용자 위치받아오고 거리 계산
    implementation ("com.google.android.gms:play-services-location:21.0.1") // 위치 서비스
    implementation("com.google.android.gms:play-services-maps:19.0.0") // Google Maps SDK for Android
    implementation ("com.google.android.material:material:1.9.0") //bottom sheet 사용

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
    implementation(libs.play.services.measurement.api)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform("com.google.firebase:firebase-database:20.0.3")) //파이어베이스 데이터베이스

    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))


    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation("com.google.firebase:firebase-analytics")


    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries

    // Glide 의존성
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

}
