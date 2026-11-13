plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.content.skills.magic.spellAttacks)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.content.interfaces.bank)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.death)
    implementation(projects.api.clue)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)

    integrationImplementation(projects.api.testing.testRandom)
    integrationImplementation(projects.api.hitPlugin)
    integrationImplementation(projects.api.npc)
    integrationImplementation(projects.api.death)
    integrationImplementation(projects.api.repo)
}
