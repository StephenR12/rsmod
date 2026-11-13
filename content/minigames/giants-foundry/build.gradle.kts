plugins {
    id("base-conventions")
    id("integration-test-suite")
}
dependencies {
    implementation(projects.api.invtx)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.skills.smithing.smelting)
}
