plugins {
    id("base-conventions")
    id("integration-test-suite")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.config)
    implementation(projects.api.combat.combatCommons)
    implementation(projects.api.combat.combatManager)
    implementation(projects.api.combat.combatWeapon)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.script)
    implementation(projects.api.type.typeBuilders)
    implementation(projects.api.type.typeReferences)
    implementation(projects.api.utils.utilsVars)
    implementation(projects.engine.events)
    implementation(projects.engine.game)
    implementation(projects.engine.map)
    implementation(projects.engine.plugin)
    testImplementation(projects.api.testing)
    testImplementation(projects.content.other.specialAttacks)
    integrationImplementation(libs.fastutil)
    integrationImplementation(projects.content.other.specialAttacks)
}

tasks.test { workingDir = rootDir }
