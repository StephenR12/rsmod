plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.realm)
    implementation(projects.api.repo)
    integrationImplementation(projects.api.combat.combatCommons)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.realm)
}
