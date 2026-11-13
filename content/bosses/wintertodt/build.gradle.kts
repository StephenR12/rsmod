plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.content.skills.pets)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.utils.utilsSkills)
    implementation(projects.content.skills.woodcutting)
    implementation(projects.content.skills.firemaking)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.content.skills.firemaking)
    integrationImplementation(projects.content.skills.cooking)
}
