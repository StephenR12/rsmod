plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    integrationImplementation(projects.api.net)
    integrationImplementation(projects.content.skills.skillcapes)
    integrationImplementation(libs.rsprot.api)
    implementation(projects.api.shops)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.content.skills.agility)
    implementation(projects.content.skills.combatTraining)
    implementation(projects.content.skills.construction)
    implementation(projects.content.skills.farming)
    implementation(projects.content.skills.hunter.hunterExperts)
    implementation(projects.content.skills.mining)
    implementation(projects.content.skills.skillcapes)
    implementation(projects.content.skills.woodcutting)
}
