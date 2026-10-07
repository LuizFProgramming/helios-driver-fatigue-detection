plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.pitest)
}

kotlin { jvmToolchain(17) }

dependencies {

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test { useJUnitPlatform() }

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt.yml"))
}

// RNF09: 100% de linhas e ramos. RNF10 (CRAP < 25) decorre disso, já que CRAP = CC com cobertura total.
kover {
    reports {
        verify {
            rule {
                minBound(100, kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE)
                minBound(100, kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH)
            }
        }
    }
}

// RNF11: 0 mutantes sobreviventes. O Kotlin gera bytecode extra (null checks, data class);
// se aparecerem mutantes impossíveis de matar, listar em avoidCallsTo/excludedMethods com justificativa.
pitest {
    junit5PluginVersion.set(libs.versions.pitestJunit5)
    targetClasses.set(listOf("br.unicid.helios.*"))
    mutationThreshold.set(100)
    timestampedReports.set(false)
    outputFormats.set(listOf("HTML", "XML"))
    excludedMethods.set(listOf("toString", "hashCode", "equals", "copy", "component*"))
}
