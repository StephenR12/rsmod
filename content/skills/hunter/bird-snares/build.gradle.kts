plugins {
    id("base-conventions")
    id("integration-test-suite")
}

group = "org.rsmod.content.skills.hunter"

dependencies {
    integrationImplementation(projects.content.skills.hunter.hunterExperts)
    integrationImplementation(projects.content.skills.crafting.hunterPouches)
    implementation(projects.content.skills.crafting.hunterPouches)
    implementation(projects.content.skills.pets)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.utils.utilsSkills)
    integrationImplementation(projects.api.hunt)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.hitPlugin)
}
