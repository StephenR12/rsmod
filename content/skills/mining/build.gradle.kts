plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.content.skills.pets)
    implementation(projects.content.skills.prayer.remains)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.registry)
    implementation(projects.api.clue)
    implementation(projects.api.shops)
    implementation(projects.api.death)
    integrationImplementation(projects.api.shops)
    implementation(projects.content.other.coalBag)
    implementation(projects.content.other.gemBag)
    implementation(projects.engine.utilsBits)
    integrationImplementation(projects.api.gameProcess)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.stats.xpmod)
    integrationImplementation(projects.api.utils.utilsSkills)
}
