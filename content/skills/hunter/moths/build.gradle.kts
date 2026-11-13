plugins {
    id("base-conventions")
    id("integration-test-suite")
}

group = "org.rsmod.content.skills.hunter"

dependencies {
    implementation(projects.content.skills.herblore)
    implementation(projects.api.invtx)
    implementation(projects.api.npc)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.api.utils.utilsSkills)
    integrationImplementation(projects.content.skills.herblore)
    integrationImplementation(projects.api.config)
}
