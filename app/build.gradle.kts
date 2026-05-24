plugins {

    id("com.google.gms.google-services")
    alias(libs.plugins.android.application)

}

android {
    namespace = "com.example.visionsign"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.visionsign"
        minSdk = 35
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
    //dependencias utilizadas en el proyecto
    implementation("com.google.ar:core:1.31.0")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation("com.google.firebase:firebase-auth:21.1.0")
    implementation("com.google.firebase:firebase-firestore:24.4.0")
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("com.gorisse.thomas.sceneform:sceneform:1.23.0")
    //firebase
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    implementation("com.google.firebase:firebase-analytics")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}