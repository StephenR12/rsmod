plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.realm)
    implementation(projects.api.invtx)
    implementation(projects.api.pluginCommons)
    integrationImplementation(projects.api.player)
}
