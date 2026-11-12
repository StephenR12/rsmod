plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.skills.crafting.commons)
    implementation(projects.content.skills.smithing.smelting)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.content.skills.crafting.commons)
    integrationImplementation(projects.content.skills.smithing.smelting)
}
