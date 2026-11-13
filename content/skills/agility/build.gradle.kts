plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.content.skills.pets)
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.spells)
    implementation(projects.api.clue)
    implementation(projects.api.registry)
    implementation(projects.content.skills.prayer.remains)
    implementation(projects.api.shops)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.content.skills.skillcapes)
    implementation(projects.content.skills.magic.spellUtility)
    implementation(projects.content.skills.firemaking)
    implementation(projects.content.skills.woodcutting)
    implementation(projects.api.pluginCommons)
    integrationImplementation(projects.api.shops)
    integrationImplementation(projects.content.skills.magic.spellUtility)
    integrationImplementation(libs.fastutil)
    implementation(libs.rsprot.api)
}
