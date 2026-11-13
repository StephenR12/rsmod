plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.content.skills.pets)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatScripts)
    implementation(projects.api.npc)
    implementation(projects.api.registry)
    implementation(projects.content.skills.magic.spellUtility)
    implementation(projects.api.clue)
    implementation(projects.api.objPlugin)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.invtx)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    integrationImplementation(projects.api.deathPlugin)
    integrationImplementation(projects.api.npc)
    integrationImplementation(projects.api.clue)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.hitPlugin)
    integrationImplementation(projects.api.registry)
    integrationImplementation(libs.fastutil)
}
