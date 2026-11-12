plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.content.skills.crafting.commons)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.content.skills.crafting.commons)
    integrationImplementation(projects.content.skills.crafting.leatherworking)
}
