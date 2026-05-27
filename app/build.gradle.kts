plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.visionsign"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.visionsign"
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

    packaging {
        resources {
            pickFirsts += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE.md"
            )
        }
    }

    aaptOptions {
        noCompress("tflite")
    }
}

configurations.all {
    resolutionStrategy {
        force("org.tensorflow:tensorflow-lite:2.4.0")
        force("org.tensorflow:tensorflow-lite-support:0.1.0")
    }
}

dependencies {

    // ================= AR =================
    implementation("com.google.ar:core:1.31.0")
    implementation("com.gorisse.thomas.sceneform:sceneform:1.23.0")

    // ================= MQTT =================
    // CORREGIDO: solo el cliente puro. NO incluir org.eclipse.paho.android.service
    // porque fue eliminado del manifest. MqttClient funciona en un Thread normal.
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    // ================= Firebase =================
    implementation(platform("com.google.firebase:firebase-bom:34.13.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    // ================= TensorFlow Lite =================
    // (mantenido aunque ClasificadorSenas no lo use aún — listo para migración futura)
    implementation("org.tensorflow:tensorflow-lite:2.4.0") {
        exclude(group = "com.google.flatbuffers", module = "flatbuffers-java")
    }
    implementation("org.tensorflow:tensorflow-lite-support:0.1.0") {
        exclude(group = "com.google.flatbuffers", module = "flatbuffers-java")
    }

    // ================= AndroidX =================
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // ================= Testing =================
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}