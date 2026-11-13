plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.api.shops)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.content.skills.fishing)
}
