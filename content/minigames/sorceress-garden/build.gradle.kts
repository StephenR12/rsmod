plugins {
    id("base-conventions")
    id("integration-test-suite")
}
dependencies {
    implementation(projects.api.pluginCommons)
    implementation(projects.api.invtx)
    implementation(projects.api.repo)
    integrationImplementation(projects.api.registry)
}

kotlin {
    target.compilations.getByName("integration").associateWith(target.compilations.getByName("main"))
}
