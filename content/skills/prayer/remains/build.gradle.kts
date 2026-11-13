plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.death)
    implementation(projects.api.registry)
    implementation(projects.content.interfaces.bank)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.realm)
    implementation(projects.api.repo)
    integrationImplementation(projects.api.combat.combatCommons)
    integrationImplementation(projects.api.death)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.realm)
    integrationImplementation(projects.content.interfaces.bank)
}
