plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.invtx)
    implementation(projects.api.objPlugin)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    integrationImplementation(projects.api.objPlugin)
    integrationImplementation(projects.api.player)
}
