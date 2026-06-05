plugins {
    alias(libs.plugins.android.application)
    id("io.sentry.android.gradle") version "6.0.0" apply false
}

android {
    namespace = "com.loginactivity.ftdriver"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.loginactivity.ftdriver"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.3"

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
    buildFeatures {
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.md"
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.11.0")
    implementation("com.amazonaws:aws-android-sdk-s3:2.74.0")
    implementation("com.amazonaws:aws-android-sdk-core:2.74.0")
    implementation("com.sun.mail:jakarta.mail:2.0.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.amazonaws:aws-android-sdk-s3:2.74.0")
    implementation("com.amazonaws:aws-android-sdk-core:2.74.0")
    implementation("com.itextpdf:itextg:5.5.10")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(platform("io.sentry:sentry-bom:8.0.0"))
    implementation("io.sentry:sentry-android")
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.legacy.support.v4)
    implementation(libs.activity)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.navigation.navigation.fragment)
    implementation(libs.androidx.navigation.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
