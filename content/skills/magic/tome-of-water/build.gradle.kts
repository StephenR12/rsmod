plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.engine.utilsBits)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.invtx)
    implementation(projects.api.objCharges)
    implementation(projects.api.pluginCommons)
    integrationImplementation(libs.guice)
    integrationImplementation(projects.api.combat.combatCommons)
    integrationImplementation(projects.api.combat.combatFormulas)
    integrationImplementation(projects.api.combat.combatManager)
    integrationImplementation(projects.api.config)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.random)
    integrationImplementation(projects.api.spellsRunes)
    integrationImplementation(projects.engine.events)
}
