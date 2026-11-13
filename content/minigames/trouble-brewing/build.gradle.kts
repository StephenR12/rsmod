plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.player)
    implementation(projects.api.invtx)
    implementation(projects.api.npc)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
}
