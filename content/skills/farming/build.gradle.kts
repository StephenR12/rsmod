plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.content.skills.pets)
    implementation(projects.content.skills.skillcapes)
    implementation(projects.api.account)
    implementation(projects.api.db)
    implementation(projects.api.invtx)
    implementation(projects.api.npc)
    implementation(projects.api.registry)
    implementation(projects.api.repo)
    implementation(projects.content.bosses.sporadic)
    implementation(projects.content.skills.prayer.remains)
    implementation(projects.api.player)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.skills.woodcutting)
    integrationImplementation(projects.api.registry)
    integrationImplementation(projects.content.bosses.sporadic)
    integrationImplementation(projects.api.testing)
}
