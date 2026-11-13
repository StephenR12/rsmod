@file:Suppress("UnstableApiUsage")

val catalogs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val junitVersion by lazy { catalogs.findVersion("junit").get().requiredVersion }

plugins {
    `jvm-test-suite`
}

testing.suites {
    val integration by registering(JvmTestSuite::class) {
        useJUnitJupiter(junitVersion)
        targets.all {
            dependencies {
                implementation(project())
                implementation(project(":api:testing"))
            }
            testTask.configure {
                // A requested pack replaces files that the integration game server reads at startup.
                mustRunAfter(":packCache")
                workingDir = rootDir
                systemProperty("junit.jupiter.extensions.autodetection.enabled", true)
                systemProperty("junit.jupiter.execution.parallel.enabled", true)
                systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
                systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "same_thread")
            }
        }
    }
}
