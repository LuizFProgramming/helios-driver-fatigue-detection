plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

// AGP 9 já compila Kotlin (built-in Kotlin): não aplicar org.jetbrains.kotlin.android.
android {
    namespace = "br.unicid.helios.app"
    // 37 porque o Compose BOM 2026.09 exige compilar contra a API 37. targetSdk continua 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "br.unicid.helios"
        // 29 = Android 10: WifiNetworkSpecifier (conectar na rede HELIOS-CAM sem perder os dados móveis).
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }

    // O modelo .task precisa ficar descomprimido para o MediaPipe mapear direto da memória.
    androidResources { noCompress += "task" }

    testOptions { unitTests.all { it.useJUnitPlatform() } }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":protocolos"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.opencv)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt.yml"))
}
