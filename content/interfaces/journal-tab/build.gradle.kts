plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    integrationImplementation(projects.api.spells)
    implementation(projects.api.pluginCommons)
    implementation(projects.content.skills.magic.spellUtility)
}
