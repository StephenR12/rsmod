import groovy.json.JsonSlurper
import java.security.MessageDigest
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.manes.versions)
    alias(libs.plugins.gradle.download)
    id("kotlin-conventions")
}

subprojects {
    tasks.withType<Test>().configureEach {
        if (name == "integration") {
            // The cache-backed test harness starts the real network service but never accepts
            // clients. Port zero asks the OS for an isolated ephemeral port, allowing tests to run
            // alongside a local rsmod server and other integration-test JVMs.
            systemProperty("rsmod.network.port", "0")
        }
    }
}

val cache233Manifest = layout.projectDirectory.file("docs/emulation/cache-233/manifest.json")
val cache233RevisionContract =
    layout.projectDirectory.file("docs/emulation/cache-233/revision-contract.json")
val cache233BuildSource = layout.projectDirectory.file("api/core/src/main/kotlin/org/rsmod/api/core/Build.kt")
val cache233ArchiveManifest =
    layout.projectDirectory.file(".data/audit/cache-233.4-archive-index-manifest.json")
val dependencyCatalog = layout.projectDirectory.file("gradle/libs.versions.toml")

tasks.register("validateCacheRevision") {
    group = "verification"
    description = "Validates the active cache, protocol, and server revision contract."
    val contractFile = layout.projectDirectory.file("docs/emulation/cache-238/revision-contract.json")
    inputs.file(contractFile)
    inputs.file(cache233BuildSource)
    inputs.file(layout.projectDirectory.file("gradle/libs.versions.toml"))
    doLast {
        val contract = JsonSlurper().parse(contractFile.asFile) as Map<*, *>
        val cache = contract["cache"] as Map<*, *>
        val protocol = contract["protocol"] as Map<*, *>
        val source = cache233BuildSource.asFile.readText()
        fun constant(name: String): String {
            val match = Regex(
                """const\s+val\s+$name\s*:\s*\w+\s*=\s*(?:"([^"]*)"|([0-9_]+))"""
            ).find(source) ?: error("Missing literal Build.$name")
            return match.groupValues.drop(1).first(String::isNotEmpty).replace("_", "")
        }
        val expected = mapOf(
            "MAJOR" to contract["major"],
            "FINAL_SUBREVISION" to contract["finalSubrevision"],
            "CACHE_ARCHIVE" to cache["archive"],
            "CACHE_CAPTURED_AT" to cache["capturedAt"],
            "CACHE_GROUPS" to cache["groups"],
            "CACHE_FILE_IDENTITIES" to cache["fileIdentities"],
            "CACHE_ARCHIVE_INDEX_MANIFEST_SHA256" to cache["manifestSha256"],
            "XTEA_SHA256" to cache["xteaSha256"],
            "CACHE_URL" to cache["diskUrl"],
            "XTEA_URL" to cache["keysUrl"],
        )
        expected.forEach { (name, value) ->
            check(constant(name) == value.toString()) { "Build.$name disagrees with the revision contract." }
        }
        val catalog = layout.projectDirectory.file("gradle/libs.versions.toml").asFile.readText()
        check(catalog.contains("rsprot = " + '"' + protocol["version"] + '"'))
        check(catalog.contains("module = " + '"' + protocol["apiModule"] + '"'))
        check(catalog.contains("module = " + '"' + protocol["sharedModule"] + '"'))
        val manifest = layout.projectDirectory.file(cache["manifest"].toString()).asFile
        if (manifest.exists()) {
            val digest = MessageDigest.getInstance("SHA-256")
            manifest.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            check(actual == cache["manifestSha256"]) { "The installed archive manifest fingerprint disagrees with the revision contract." }
        }
        logger.lifecycle("Active cache revision ${contract["major"]}, archive ${cache["archive"]}, protocol ${protocol["version"]} verified.")
    }
}

tasks.register("validateCache233Parity") {
    group = "verification"
    dependsOn("validateCacheRevision")
    description = "Validates historical cache-233 parity evidence and the active revision lock."
    inputs.file(cache233Manifest)
    inputs.file(cache233RevisionContract)
    inputs.file(cache233BuildSource)
    inputs.files(provider { listOf(cache233ArchiveManifest.asFile).filter { it.exists() } })
    inputs.file(dependencyCatalog)

    doLast {
        val manifest = JsonSlurper().parse(cache233Manifest.asFile) as Map<*, *>
        val contract = JsonSlurper().parse(cache233RevisionContract.asFile) as Map<*, *>
        check(contract["schemaVersion"].toString() == "1") {
            "Unsupported revision contract schema: ${contract["schemaVersion"]}"
        }
        val cache = contract["cache"] as? Map<*, *>
            ?: throw GradleException("Revision contract is missing cache metadata.")
        val protocol = contract["protocol"] as? Map<*, *>
            ?: throw GradleException("Revision contract is missing protocol metadata.")
        val target = manifest["target"] as? Map<*, *>
            ?: throw GradleException("Parity manifest is missing target metadata.")
        check(target["major"].toString() == contract["major"].toString()) {
            "Parity target major does not match the revision contract."
        }
        check(target["finalSubrevision"] == contract["finalSubrevision"]) {
            "Parity target subrevision does not match the revision contract."
        }
        check(target["openRs2Archive"].toString() == cache["archive"].toString()) {
            "Parity target archive does not match the revision contract."
        }
        check(target["openRs2CapturedAt"] == cache["capturedAt"]) {
            "Parity target capture time does not match the revision contract."
        }

        val pinnedBuildSource = cache233BuildSource.asFile.readText()
        fun kotlinConstant(name: String): String {
            val pattern =
                Regex(
                    """const\s+val\s+${Regex.escape(name)}\s*:\s*\w+\s*=\s*(?:\"([^\"]*)\"|([0-9_]+))"""
                )
            val match = pattern.find(pinnedBuildSource)
                ?: throw GradleException("Build.$name is missing or is not a literal constant.")
            return match.groupValues.drop(1).first(String::isNotEmpty)
        }
        // Keep the 233.4 evidence usable after an explicitly selected major upgrade.
        if (kotlinConstant("MAJOR") == contract["major"].toString()) {
            check(kotlinConstant("MAJOR") == contract["major"].toString()) {
                "Build.MAJOR does not match the revision contract."
            }
            check(kotlinConstant("FINAL_SUBREVISION") == contract["finalSubrevision"]) {
                "Build.FINAL_SUBREVISION does not match the revision contract."
            }
            check(kotlinConstant("CACHE_ARCHIVE") == cache["archive"].toString()) {
                "Build.CACHE_ARCHIVE does not match the revision contract."
            }
            check(kotlinConstant("CACHE_CAPTURED_AT") == cache["capturedAt"]) {
                "Build.CACHE_CAPTURED_AT does not match the revision contract."
            }
            check(kotlinConstant("CACHE_GROUPS").replace("_", "") == cache["groups"].toString()) {
                "Build.CACHE_GROUPS does not match the revision contract."
            }
            check(
                kotlinConstant("CACHE_FILE_IDENTITIES").replace("_", "") ==
                    cache["fileIdentities"].toString()
            ) {
                "Build.CACHE_FILE_IDENTITIES does not match the revision contract."
            }
            check(
                kotlinConstant("CACHE_ARCHIVE_INDEX_MANIFEST_SHA256") == cache["manifestSha256"]
            ) {
                "Build.CACHE_ARCHIVE_INDEX_MANIFEST_SHA256 does not match the revision contract."
            }
            check(kotlinConstant("XTEA_SHA256") == cache["xteaSha256"]) {
                "Build.XTEA_SHA256 does not match the revision contract."
            }
            check(kotlinConstant("CACHE_URL") == cache["diskUrl"]) {
                "Build.CACHE_URL does not match the revision contract."
            }
            check(kotlinConstant("XTEA_URL") == cache["keysUrl"]) {
                "Build.XTEA_URL does not match the revision contract."
            }
        }

        check(cache["manifest"] == ".data/audit/cache-233.4-archive-index-manifest.json") {
            "Revision contract must point at the checked vanilla archive-index manifest."
        }
        if (cache233ArchiveManifest.asFile.exists()) {
            val digest = MessageDigest.getInstance("SHA-256")
            cache233ArchiveManifest.asFile.inputStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val archiveManifestSha256 =
                digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
            check(archiveManifestSha256 == cache["manifestSha256"]) {
                "Vanilla archive-index manifest checksum does not match the revision contract."
            }
        } else {
            logger.lifecycle(
                "Local archive-index audit is absent; validated its pinned path and checksum only."
            )
        }

        if (kotlinConstant("MAJOR") == contract["major"].toString()) {
            val versions = dependencyCatalog.asFile.readText()
            fun tomlString(name: String): String {
                val match =
                    Regex("(?m)^\\s*${Regex.escape(name)}\\s*=\\s*\"([^\"]+)\"\\s*$")
                        .find(versions)
                        ?: throw GradleException("Dependency catalog is missing $name.")
                return match.groupValues[1]
            }
            fun tomlModule(name: String): String {
                val match =
                    Regex(
                            "(?m)^\\s*${Regex.escape(name)}\\s*=\\s*\\{[^}]*module\\s*=\\s*\"([^\"]+)\"[^}]*}\\s*$"
                        )
                        .find(versions)
                        ?: throw GradleException("Dependency catalog is missing module $name.")
                return match.groupValues[1]
            }
            check(tomlString("rsprot") == protocol["version"]) {
                "rsprot version does not match the revision contract."
            }
            check(tomlModule("rsprot-api") == protocol["apiModule"]) {
                "rsprot API module does not match the revision contract."
            }
            check(tomlModule("rsprot-shared") == protocol["sharedModule"]) {
                "rsprot shared module does not match the revision contract."
            }
        }

        val expectedSkills =
            setOf(
                "attack",
                "strength",
                "defence",
                "hitpoints",
                "ranged",
                "prayer",
                "magic",
                "runecraft",
                "crafting",
                "mining",
                "smithing",
                "fishing",
                "cooking",
                "firemaking",
                "woodcutting",
                "agility",
                "herblore",
                "thieving",
                "fletching",
                "slayer",
                "farming",
                "construction",
                "hunter",
            )
        val productionSkills =
            (manifest["productionSkills"] as? List<*>)?.map(Any?::toString)?.toSet()
                ?: throw GradleException("Parity manifest is missing productionSkills.")
        check(productionSkills == expectedSkills) {
            "Cache 233 must track exactly the 23 production skills. " +
                "missing=${expectedSkills - productionSkills}, extra=${productionSkills - expectedSkills}"
        }

        val validStatuses =
            setOf(
                "discovered",
                "researched",
                "designed",
                "implemented",
                "unit-tested",
                "integration-tested",
                "client-tested",
                "verified",
            )
        val features = manifest["features"] as? List<*>
            ?: throw GradleException("Parity manifest is missing features.")
        val featureMaps = features.mapIndexed { index, feature ->
            feature as? Map<*, *>
                ?: throw GradleException("Feature at index $index is not an object.")
        }
        val ids = featureMaps.map { it["id"]?.toString().orEmpty() }
        check(ids.none(String::isBlank)) { "Every parity feature requires a stable id." }
        check(ids.size == ids.toSet().size) { "Parity feature ids must be unique." }
        featureMaps.forEach { feature ->
            val id = feature["id"]
            val status = feature["status"]?.toString()
            check(status in validStatuses) { "Feature $id has invalid status: $status" }
            check(!feature["category"].toString().isBlank()) { "Feature $id is missing category." }
            check(!feature["name"].toString().isBlank()) { "Feature $id is missing name." }
            check(feature["sources"] is List<*>) { "Feature $id must have a sources list." }
            check(feature["tests"] is List<*>) { "Feature $id must have a tests list." }
        }

        val counts = featureMaps.groupingBy { it["status"].toString() }.eachCount().toSortedMap()
        logger.lifecycle("Historical cache 233 parity manifest valid: ${featureMaps.size} feature groups, $counts")

        val requireComplete =
            providers.gradleProperty("cacheParityRequireComplete").orNull.toBoolean()
        if (requireComplete) {
            val incomplete = featureMaps.filter { it["status"] != "verified" }
            check(incomplete.isEmpty()) {
                "Cache 233 parity is incomplete: ${incomplete.size} feature groups are not verified."
            }
        }
    }
}

tasks.register("reportCache233Parity") {
    group = "verification"
    description = "Prints cache-233 parity totals by category and status."
    dependsOn("validateCache233Parity")

    doLast {
        val manifest = JsonSlurper().parse(cache233Manifest.asFile) as Map<*, *>
        val features = (manifest["features"] as List<*>).map { it as Map<*, *> }
        features
            .groupBy { it["category"].toString() }
            .toSortedMap()
            .forEach { (category, entries) ->
                val counts = entries.groupingBy { it["status"].toString() }.eachCount().toSortedMap()
                logger.lifecycle("$category: ${entries.size} $counts")
            }
    }
}

allprojects {
    group = "org.rsmod"
    version = "0.0.1"
}

dependencies {
    implementation(projects.server.install)
}

tasks.register("run") {
    group = "application"
    description = "Runs the RS Mod game server"

    dependsOn(":server:app:run")
}

tasks.register<JavaExec>("install") {
    group = "installation"
    description = "Runs the complete RS Mod server installation task."

    mainClass.set("org.rsmod.server.install.GameServerInstallKt")
    classpath = sourceSets["main"].runtimeClasspath

    doLast { logger.lifecycle("Installation process completed.") }
}

tasks.register<JavaExec>("cleanInstall") {
    group = "installation"
    description = "Cleans up any partial or corrupted artifacts from previous RS Mod installations."

    args = getArgsFromProperty("rsmodInstallClean")
    mainClass.set("org.rsmod.server.install.GameServerCleanInstallKt")
    classpath = sourceSets["main"].runtimeClasspath

    doFirst { logger.lifecycle("Starting clean up of any previous installation attempts...") }
    doLast { logger.lifecycle("Clean-up process completed. You can now run the `install` task.") }
}

tasks.register<JavaExec>("downloadCache") {
    group = "cache"
    description = "Runs the cache download & extract task."

    args = getArgsFromProperty("cacheDownload")
    mainClass.set("org.rsmod.server.install.GameServerCacheDownloaderKt")
    classpath = sourceSets["main"].runtimeClasspath

    doFirst { logger.lifecycle("Starting the cache download process...") }
    doLast { logger.lifecycle("Cache download completed.") }
}

tasks.register<JavaExec>("packCache") {
    group = "cache"
    description = "Runs the cache packer task."

    args = getArgsFromProperty("cachePack")
    mainClass.set("org.rsmod.server.install.GameServerCachePackerKt")
    classpath = sourceSets["main"].runtimeClasspath

    doFirst { logger.lifecycle("Starting the cache-packing process...") }
    doLast { logger.lifecycle("Cache-packing process completed.") }
}

tasks.register<JavaExec>("generateRsa") {
    group = "security"
    description = "Runs the rsa-key generation task."

    args = getArgsFromProperty("rsa")
    mainClass.set("org.rsmod.server.install.GameNetworkRsaGeneratorKt")
    classpath = sourceSets["main"].runtimeClasspath

    doFirst { logger.lifecycle("Starting the rsa-key generation process...") }
    doLast { logger.lifecycle("RSA generation process completed.") }
}

tasks.register<JavaExec>("setupLogbackNovice") {
    description = "Copies the novice logback configuration."

    mainClass.set("org.rsmod.server.install.GameServerLogbackCopyKt")
    classpath = sourceSets["main"].runtimeClasspath

    doFirst { logger.lifecycle("Starting logback copy for novice configuration...") }
    doLast { logger.lifecycle("Logback novice configuration copied successfully.") }
}

tasks.register<JavaExec>("setupLogbackAdvanced") {
    description = "Copies the novice logback configuration."

    mainClass.set("org.rsmod.server.install.GameServerLogbackCopyKt")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("--advanced-logback")

    doFirst { logger.lifecycle("Starting logback copy for advanced configuration...") }
    doLast { logger.lifecycle("Logback advanced configuration copied successfully.") }
}

fun getArgsFromProperty(propertyName: String): List<String> {
    val argsProp = project.findProperty(propertyName)
    return argsProp?.toString()?.split(" ") ?: emptyList()
}
