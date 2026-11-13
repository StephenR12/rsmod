plugins {
    id("base-conventions")
    id("integration-test-suite")
}
dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.invtx)
    implementation(projects.content.skills.pets)
    integrationImplementation(projects.api.hitPlugin)
    implementation(projects.api.npc)
    implementation(projects.api.repo)
    implementation(projects.content.skills.mining)
}
kotlin {
    target.compilations.getByName("integration").associateWith(target.compilations.getByName("main"))
}
