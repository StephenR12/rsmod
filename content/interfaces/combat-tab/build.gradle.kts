plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    integrationImplementation(projects.api.spells)
    integrationImplementation(projects.api.spellsAutocast)
    integrationImplementation(projects.api.combat.combatCommons)
    integrationImplementation(projects.api.spellsRunes)
    implementation(libs.fastutil)
    implementation(projects.api.combat.combatWeapon)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.specials)
    implementation(projects.api.spells)
    implementation(projects.api.spellsAutocast)
}
