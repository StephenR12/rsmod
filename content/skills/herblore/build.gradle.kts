plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.scriptAdvanced)
    integrationImplementation(projects.engine.utilsBits)
    integrationImplementation(projects.api.shops)
    implementation(projects.content.interfaces.bank)
    implementation(projects.api.shops)
    implementation(projects.engine.utilsBits)
    implementation(projects.api.market)
    implementation(projects.api.invtx)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.realm)
    implementation(projects.api.specials)
    implementation(projects.content.skills.cooking)
    integrationImplementation(projects.api.player)
    integrationImplementation(projects.api.realm)
    integrationImplementation(projects.api.specials)
    integrationImplementation(projects.content.skills.cooking)
    integrationImplementation(projects.content.interfaces.bank)
    integrationImplementation(projects.content.skills.magic.spellUtility)
    integrationImplementation(libs.fastutil)
}
