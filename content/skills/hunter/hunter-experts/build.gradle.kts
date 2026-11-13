plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.content.skills.pets)
    implementation(projects.api.utils.utilsSkills)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    integrationImplementation(libs.fastutil)
}
