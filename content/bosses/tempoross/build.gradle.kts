plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.clue)
    implementation(projects.api.registry)
    implementation(projects.content.skills.fishing)
    implementation(projects.content.skills.pets)
    integrationImplementation(projects.api.player)
}
