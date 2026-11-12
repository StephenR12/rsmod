plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.engine.utilsBits)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.death)
    implementation(projects.api.npc)
    implementation(projects.api.invtx)
    implementation(projects.api.repo)
    implementation(projects.content.generic.genericLocs)
    integrationImplementation(projects.content.generic.genericLocs)
    integrationImplementation(projects.api.death)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.repo)
    integrationImplementation(libs.fastutil)
}
