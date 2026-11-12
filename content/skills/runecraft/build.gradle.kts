plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.invtx)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.content.skills.magic.spellUtility)
    implementation(projects.engine.utilsBits)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.content.skills.magic.spellUtility)
}
