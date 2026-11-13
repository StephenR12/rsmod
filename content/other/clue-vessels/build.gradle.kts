plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.clue)
    implementation(projects.api.pluginCommons)
    integrationImplementation(projects.api.clue)
}
