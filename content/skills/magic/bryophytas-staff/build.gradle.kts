plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.engine.utilsBits)
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.invtx)
    implementation(projects.api.objCharges)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.type.typeBuilders)
    implementation(projects.api.type.typeScriptDsl)
    integrationImplementation(libs.guice)
    integrationImplementation(projects.api.combat.combatCommons)
    integrationImplementation(projects.api.combat.combatManager)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.spellsRunes)
    integrationImplementation(projects.engine.events)
}
