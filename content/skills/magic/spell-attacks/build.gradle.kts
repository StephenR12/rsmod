plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.random)
    implementation(projects.api.spells)
    implementation(projects.content.skills.magic.spellUtility)
    integrationImplementation(projects.api.combat.combatCommons)
    integrationImplementation(projects.api.combat.combatManager)
    integrationImplementation(projects.api.combat.combatWeapon)
    integrationImplementation(projects.api.config)
    integrationImplementation(projects.api.death)
    integrationImplementation(projects.api.hitPlugin)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.registry)
    integrationImplementation(projects.api.spells)
    integrationImplementation(projects.api.spellsAutocast)
    integrationImplementation(projects.api.spellsRunes)
    integrationImplementation(libs.fastutil)
}
