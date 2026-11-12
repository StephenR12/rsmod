plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.other.coalBag)
    implementation(projects.content.other.gemBag)
    implementation(projects.engine.utilsBits)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.utils.utilsSkills)
}
