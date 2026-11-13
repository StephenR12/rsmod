plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatFormulas)
    implementation(projects.api.clue)
    implementation(projects.api.hunt)
    implementation(projects.api.invtx)
    implementation(libs.rsprot.api)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)

    integrationImplementation(projects.content.skills.prayer.remains)
    integrationImplementation(projects.api.invtx)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.repo)
}
