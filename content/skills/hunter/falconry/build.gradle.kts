plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    integrationImplementation(projects.content.skills.crafting.hunterPouches)
    implementation(projects.content.skills.crafting.hunterPouches)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.utils.utilsSkills)
    integrationImplementation(projects.api.player)
}
