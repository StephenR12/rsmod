plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.content.skills.pets)
    integrationImplementation(projects.content.skills.mining)
    integrationImplementation(projects.content.interfaces.bank)
    implementation(projects.content.skills.skillcapes)
    implementation(projects.content.other.gemBag)
    implementation(projects.content.skills.mining)
    implementation(projects.content.interfaces.bank)
    implementation(projects.api.shops)
    implementation(projects.api.clue)
    implementation(projects.api.registry)
    implementation(projects.api.invtx)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.spells)
    implementation(projects.engine.utilsBits)
    integrationImplementation(projects.api.shops)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.spells)
}
