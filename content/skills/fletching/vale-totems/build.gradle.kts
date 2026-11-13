plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.death)
    integrationImplementation(projects.api.death)
    implementation(projects.api.clue)
    implementation(projects.content.skills.woodcutting)
    implementation(projects.api.shops)
    integrationImplementation(projects.api.shops)
}
