plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.random)
    implementation(projects.api.scriptAdvanced)
    testImplementation(kotlin("test"))
    integrationImplementation(projects.api.combat.combatManager)
    integrationImplementation(projects.api.testing)
}

tasks.test { workingDir = rootDir }
