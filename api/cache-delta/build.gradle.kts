plugins {
    id("base-conventions")
    id("integration-test-suite")
    application
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.netty.buffer)
    implementation(libs.openrs2.cache)
    implementation(projects.api.cache)
    implementation(projects.api.config)
    implementation(projects.api.type.typeBuilders)
    implementation(projects.api.type.typeScriptDsl)
    implementation(projects.api.type.typeSymbols)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
}

application {
    mainClass.set("org.rsmod.api.cache.delta.CacheDeltaMainKt")
}

tasks.named<JavaExec>("run") {
    description = "Exports or compares versioned cache inventory manifests."

    // Gradle's unqualified `run` selector also discovers subproject application tasks. Keep the
    // repository's long-standing `./gradlew run` server launcher compatible while requiring an
    // explicit argument-bearing invocation for this command-line tool.
    onlyIf("cache delta input and output paths were supplied") { args.isNotEmpty() }
}
