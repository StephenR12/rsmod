plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatWeapon)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.combat.combatScripts)
    implementation(projects.api.invtx)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)

    integrationImplementation(projects.api.hitPlugin)
    integrationImplementation(projects.api.combat.combatWeapon)
}
