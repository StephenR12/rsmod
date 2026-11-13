plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.content.interfaces.bank)
    implementation(projects.api.clue)
    implementation(projects.api.registry)
    implementation(projects.content.skills.pets)
    implementation(projects.content.skills.woodcutting)
    implementation(projects.api.invtx)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.utils.utilsSkills)
    implementation(projects.engine.utilsBits)
    integrationImplementation(projects.content.skills.pets)
    integrationImplementation(projects.content.skills.cooking)
    integrationImplementation(projects.api.player)
}
