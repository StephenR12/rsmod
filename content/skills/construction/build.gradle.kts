plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.content.skills.pets)
    implementation(projects.content.skills.skillcapes)
    implementation(projects.api.specials)
    implementation(projects.content.skills.herblore)
    implementation(projects.api.account)
    implementation(projects.api.db)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.api.registry)
    implementation(projects.content.skills.magic.spellUtility)
    integrationImplementation(projects.api.account)
    integrationImplementation(projects.api.db)
    integrationImplementation(projects.content.skills.magic.spellUtility)
    integrationImplementation(projects.content.skills.pets)
    integrationImplementation(libs.fastutil)
    integrationImplementation(libs.rsprot.api)
}
