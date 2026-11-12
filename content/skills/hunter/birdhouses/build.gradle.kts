plugins {
    id("base-conventions")
    id("integration-test-suite")
}

// Keep this runtime module distinct from content/skills/crafting/birdhouses. Gradle resolves
// project dependencies by module coordinates, and both projects otherwise publish as
// org.rsmod:birdhouses:0.0.1, silently substituting the Crafting plugin for this Hunter plugin.
group = "org.rsmod.content.skills.hunter"

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.content.skills.crafting.birdhouses)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.content.skills.crafting.birdhouses)
}
