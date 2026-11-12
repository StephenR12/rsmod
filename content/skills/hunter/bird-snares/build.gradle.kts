plugins {
    id("base-conventions")
    id("integration-test-suite")
}

group = "org.rsmod.content.skills.hunter"

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.utils.utilsSkills)
    integrationImplementation(projects.api.player)
}
