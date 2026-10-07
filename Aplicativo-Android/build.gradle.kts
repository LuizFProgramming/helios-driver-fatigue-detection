plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.pitest) apply false
}

/**
 * Meta RNF14 adaptada ao Kotlin: proíbe `Any`, `!!` e casts não verificados no código de produção.
 * Busca textual simples; os casos legítimos (raros) ficam listados em config/tipos-permitidos.txt.
 */
val verificarTiposProibidos by tasks.registering {
    group = "verification"
    description = "Falha se houver Any, !! ou UNCHECKED_CAST no código de produção."
    val fontes = fileTree(rootDir) { include("*/src/main/**/*.kt") }
    val permitidos = rootProject.file("config/tipos-permitidos.txt")
    inputs.files(fontes, permitidos)
    doLast {
        val excecoes = permitidos.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()
        val proibido = Regex("""(:\s*Any\??\b)|(<\s*Any\??\s*>)|(\bas\s+Any\b)|(!!)|(UNCHECKED_CAST)""")
        val achados = fontes.files.flatMap { arquivo ->
            arquivo.readLines().mapIndexedNotNull { i, linha ->
                val local = "${arquivo.relativeTo(rootDir).invariantSeparatorsPath}:${i + 1}"
                if (proibido.containsMatchIn(linha) && local !in excecoes) "$local  $linha" else null
            }
        }
        if (achados.isNotEmpty()) throw GradleException("Tipos proibidos:\n" + achados.joinToString("\n"))
    }
}

/** Esteira completa (equivale ao `npm run qualidade` do roadmap 08). */
tasks.register("qualidade") {
    group = "verification"
    description = "detekt + testes + cobertura 100% + mutação + tipos proibidos."
    dependsOn(
        verificarTiposProibidos,
        ":dominio:detekt", ":protocolos:detekt", ":app:detekt",
        ":dominio:koverVerify", ":protocolos:koverVerify",
        ":dominio:pitest", ":protocolos:pitest",
        ":app:testDebugUnitTest",
    )
}
