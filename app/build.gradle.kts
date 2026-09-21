import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val geminiKey = localProperties.getProperty("GEMINI_API_KEY", "MOCK_KEY_FOR_NOW")
val bhashiniUserId = localProperties.getProperty("BHASHINI_USER_ID", "MOCK_USER_ID")
val bhashiniApiKey = localProperties.getProperty("BHASHINI_API_KEY", "MOCK_API_KEY")
val liteRtModelPath = localProperties.getProperty("LITERT_MODEL_PATH", "")
val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")

android {
    namespace = "com.nercare.cogcare"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nercare.cogcare"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
        buildConfigField("String", "BHASHINI_USER_ID", "\"$bhashiniUserId\"")
        buildConfigField("String", "BHASHINI_API_KEY", "\"$bhashiniApiKey\"")
        buildConfigField("String", "LITERT_MODEL_PATH", "\"${liteRtModelPath.replace("\\", "\\\\")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
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
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Prevent compression of LiteRT model files
    androidResources {
        noCompress += "tflite"
        noCompress += "lite"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // Vico Charts
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // Coil (image loading)
    implementation(libs.coil.compose)

    // Lottie (animations)
    implementation(libs.lottie.compose)

    // Gemini AI
    implementation(libs.generativeai)

    // LiteRT (formerly TensorFlow Lite)
    implementation(libs.litert)

    // Google AI Edge on-device text generation (LiteRT-backed Gemma models)
    implementation(libs.mediapipe.tasks.genai)

    // DataStore
    implementation(libs.datastore.preferences)

    // Testing
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.matching { it.name in listOf("assembleDebug", "assembleRelease") }.configureEach {
    doLast {
        val buildDir = layout.buildDirectory.get().asFile
        val foundApks = buildDir.walkTopDown().filter { it.extension == "apk" && !it.name.contains("unaligned") }.toList()
        foundApks.forEach { apk ->
            val destRoot = rootProject.file(apk.name)
            apk.copyTo(destRoot, overwrite = true)
            println("Successfully copied ${apk.name} to project root: ${destRoot.absolutePath}")
        }
    }
}
