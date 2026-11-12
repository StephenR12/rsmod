plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.invtx)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.realm)
    implementation(projects.api.specials)
    implementation(projects.content.skills.cooking)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.realm)
    integrationImplementation(projects.api.specials)
    integrationImplementation(projects.content.skills.cooking)
    integrationImplementation(libs.fastutil)
}
