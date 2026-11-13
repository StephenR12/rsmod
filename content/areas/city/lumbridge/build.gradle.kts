plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.content.interfaces.journalTab)
    implementation(projects.api.invtx)
    implementation(projects.api.pluginCommons)
    implementation(projects.api.scriptAdvanced)
    implementation(projects.content.other.degradableEquipment)
}
