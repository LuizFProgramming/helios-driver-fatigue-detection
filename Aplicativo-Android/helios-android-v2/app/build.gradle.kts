import java.security.MessageDigest

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
        versionCode = 6
        versionName = "0.4.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Só ARM (celulares e centrais multimídia). x86 fica no debug, para o emulador.
            ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
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
    // RNF01 (offline): a telemetria do MediaPipe é bloqueada no AndroidManifest (remoção do backend "cct").
    // NÃO excluir o módulo transport-backend-cct aqui: TaskRunner.create sempre cria o RemoteLoggingClient,
    // que lê CCTDestination.INSTANCE; sem a classe, o app fecha ao abrir (conferido no bytecode do 0.10.35).
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.opencv)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

/**
 * O modelo do MediaPipe precisa ser exatamente o testado (ideia do DriveSense). Se alguém trocar o
 * arquivo, o build falha em vez de rodar com outro modelo sem ninguém perceber.
 * Para atualizar o modelo de propósito: troque o arquivo e o valor abaixo juntos.
 */
val verificarModelo by tasks.registering {
    group = "verification"
    description = "Confere o SHA-256 de assets/face_landmarker.task."
    val modelo = file("src/main/assets/face_landmarker.task")
    val esperado = "64184e229b263107bc2b804c6625db1341ff2bb731874b0bcc2fe6544e0bc9ff"
    inputs.file(modelo)
    doLast {
        val atual = MessageDigest.getInstance("SHA-256").digest(modelo.readBytes())
            .joinToString("") { "%02x".format(it) }
        if (atual != esperado) {
            throw GradleException("face_landmarker.task diferente do esperado.\nEsperado: $esperado\nEncontrado: $atual")
        }
    }
}
tasks.named("preBuild") { dependsOn(verificarModelo) }

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt.yml"))
}
