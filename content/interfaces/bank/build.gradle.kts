plugins {
    id("base-conventions")
    id("integration-test-suite")
}

dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.repo)
    implementation(projects.engine.utilsBits)
    testImplementation(projects.api.testing.testParams)
}
