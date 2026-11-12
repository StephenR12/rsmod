plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.death)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.content.skills.magic.spellUtility)

    integrationImplementation(projects.api.death)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.repo)
    integrationImplementation(projects.content.skills.magic.spellUtility)
}
