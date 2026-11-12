plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.clue)
    implementation(projects.api.invtx)
    implementation(projects.engine.utilsBits)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.skills.firemaking)
    integrationImplementation(projects.api.clue)
    integrationImplementation(projects.api.areaChecker)
    integrationImplementation(projects.api.cache)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.registry)
    integrationImplementation(projects.api.stats.levelmod)
    integrationImplementation(projects.engine.annotations)
    integrationImplementation(libs.fastutil)
    integrationImplementation(libs.openrs2.buffer)
    integrationImplementation(libs.openrs2.cache)
}
