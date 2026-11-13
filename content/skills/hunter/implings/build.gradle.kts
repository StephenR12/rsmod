plugins {
    id("base-conventions")
    id("integration-test-suite")
}
dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.clue)
    implementation(projects.api.utils.utilsSkills)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.engine.annotations)
}
