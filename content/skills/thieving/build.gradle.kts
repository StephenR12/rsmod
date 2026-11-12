plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.clue)
    implementation(projects.api.invtx)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    integrationImplementation(projects.api.clue)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.hitPlugin)
    integrationImplementation(projects.api.registry)
    integrationImplementation(libs.fastutil)
}
