plugins {
    id("base-conventions")
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.guice)
    implementation(projects.api.player)
    implementation(projects.api.playerOutput)
    implementation(projects.api.random)
    implementation(projects.api.repo)
    implementation(projects.api.invtx)
    implementation(projects.api.config)
    implementation(projects.api.type.typeReferences)
    implementation(projects.engine.map)
    implementation(projects.engine.game)
}
