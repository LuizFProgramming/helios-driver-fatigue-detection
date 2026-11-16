import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.compose.screenshot)
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
        versionCode = 12
        versionName = "0.4.8"
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

    // Capturas das prévias (src/screenshotTest): updateDebugScreenshotTest grava, validateDebugScreenshotTest compara.
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    // Os modelos (.task do MediaPipe, .tflite da CNN do olho) ficam descomprimidos para serem mapeados direto da memória.
    androidResources { noCompress += listOf("task", "tflite") }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
        // Robolectric precisa dos recursos (fonte, ícones) para medir as telas.
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin { jvmToolchain(17) }

// O Robolectric só simula o Android 16 (API 36, o targetSdk) em Java 21. O código continua compilado para Java 17.
tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}

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
    implementation(libs.litert)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.sqlite.jdbc)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Telas na JVM: Compose UI test (JUnit 4) com Robolectric, rodando na plataforma JUnit 5 pelo vintage.
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.junit.vintage.engine)
    debugImplementation(libs.compose.ui.test.manifest)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.compose.ui.tooling)
}

/**
 * O modelo do MediaPipe precisa ser exatamente o testado (ideia do DriveSense). Se alguém trocar o
 * arquivo, o build falha em vez de rodar com outro modelo sem ninguém perceber.
 * Para atualizar o modelo de propósito: troque o arquivo e o valor abaixo juntos.
 */
val verificarModelo by tasks.registering {
    group = "verification"
    description = "Confere o SHA-256 dos modelos em assets (face_landmarker.task e olho_cnn.tflite)."
    val modelos = mapOf(
        file("src/main/assets/face_landmarker.task") to "64184e229b263107bc2b804c6625db1341ff2bb731874b0bcc2fe6544e0bc9ff",
        // CNN do olho: a "media" treinada só com a CEW, escolhida por scripts/experimento_olhos/comparar_tamanhos.py.
        file("src/main/assets/olho_cnn.tflite") to "178b0feb4537a9bf9eb5e7777feebbf7096de74087ca784779669507bfc1274f",
    )
    inputs.files(modelos.keys)
    doLast {
        modelos.forEach { (modelo, esperado) ->
            val atual = MessageDigest.getInstance("SHA-256").digest(modelo.readBytes())
                .joinToString("") { "%02x".format(it) }
            if (atual != esperado) {
                throw GradleException("${modelo.name} diferente do esperado.\nEsperado: $esperado\nEncontrado: $atual")
            }
        }
    }
}
tasks.named("preBuild") { dependsOn(verificarModelo) }

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt.yml"))
}
