plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.engine.utilsBits)
    integrationImplementation(projects.api.player)
}
