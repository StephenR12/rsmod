plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.clue)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.interfaces.bank)
    implementation(projects.content.skills.thieving)
    integrationImplementation(projects.api.clue)
    integrationImplementation(projects.api.death)
    integrationImplementation(projects.api.hitPlugin)
    integrationImplementation(projects.api.utils.utilsSkills)
    integrationImplementation(projects.content.skills.thieving)
}
