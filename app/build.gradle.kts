plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.tehilat.sidur"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tehilat.sidur"
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("com.squareup.okhttp3:okhttp:4.11.0")
    implementation ("com.google.android.gms:play-services-location:21.3.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("com.android.volley:volley:1.2.1")
    implementation(libs.preference)
    implementation(libs.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
