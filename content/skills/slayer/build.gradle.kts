plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.content.skills.skillcapes)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.objCharges)
    integrationImplementation(projects.api.combat.combatCommons)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.hitPlugin)
}
