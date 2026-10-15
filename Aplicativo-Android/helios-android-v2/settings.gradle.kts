pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "helios"

// dominio: regras puras (EAR, PERCLOS, eventos, classificação). Zero dependências.
// protocolos: partes "inteligentes" dos adaptadores (MJPEG, malha facial, alarme, tela). Só JVM.
// app: Android. Adaptadores finos + interface. Toda decisão fica nos dois módulos acima.
include(":dominio", ":protocolos", ":app")
