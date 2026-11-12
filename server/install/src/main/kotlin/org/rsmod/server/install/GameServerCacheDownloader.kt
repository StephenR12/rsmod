package org.rsmod.server.install

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.michaelbull.logging.InlineLogger
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.outputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import org.rsmod.api.cache.delta.CacheArchiveIndexManifestExporter
import org.rsmod.api.cache.delta.CacheDeltaJson
import org.rsmod.api.core.Build
import org.rsmod.server.shared.DirectoryConstants

fun main(args: Array<String>): Unit = GameServerCacheDownloader().main(args)

class GameServerCacheDownloader : CliktCommand(name = "cache-download") {
    private val cacheUrl by option("-cacheUrl").default(Build.CACHE_URL)
    private val xteaUrl by option("-xteaUrl").default(Build.XTEA_URL)
    private val preferredCacheDir by option("-cacheBaseDir")

    private val logger = InlineLogger()

    private val cacheDir: Path
        get() = preferredCacheDir?.let { Paths.get(it) } ?: DirectoryConstants.CACHE_PATH

    private val vanillaCacheDir: Path
        get() = cacheDir.resolve("vanilla")

    private val xteaFile: Path
        get() = cacheDir.resolve("xteas.json")

    @ExperimentalPathApi
    override fun run() {
        val client =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        cacheDir.createDirectories()

        val stagingDir = createTempDirectory(cacheDir, ".cache-download-")
        try {
            // TODO: add support for extracting .tar.gz files
            val stagedCacheDir = stagingDir.resolve("vanilla")
            val stagedXteaFile = stagingDir.resolve("xteas.json")
            stagedCacheDir.createDirectories()
            downloadAndExtractCache(client, cacheUrl, stagedCacheDir)
            downloadOrFindXteaFile(client, stagedCacheDir, xteaUrl, stagedXteaFile)
            CacheDownloadStaging.install(
                stagedCacheDir = stagedCacheDir,
                stagedXteaFile = stagedXteaFile,
                targetCacheDir = vanillaCacheDir,
                targetXteaFile = xteaFile,
            )
        } finally {
            stagingDir.deleteRecursively()
        }
    }

    private fun downloadAndExtractCache(client: OkHttpClient, cacheUrl: String, outputDir: Path) {
        val cacheArchiveFile = createTempArchiveFile(cacheUrl, outputDir)
        cacheArchiveFile.toFile()

        logger.info { "Downloading cache archive from: $cacheUrl" }
        downloadCache(client, cacheUrl, cacheArchiveFile)
        logger.info { "Downloaded cache archive." }

        logger.info { "Extracting cache archive files to: ${outputDir.toAbsolutePath()}" }
        extractCache(cacheArchiveFile, outputDir)
        logger.info { "Extracted cache archive files." }

        cacheArchiveFile.deleteExisting()
    }

    private fun downloadCache(client: OkHttpClient, cacheUrl: String, archiveOutput: Path) {
        client.download(cacheUrl, archiveOutput)
    }

    private fun extractCache(archive: Path, outputDir: Path) {
        archive.extractFiles(outputDir)
    }

    private fun downloadOrFindXteaFile(
        client: OkHttpClient,
        cacheArchiveDir: Path,
        xteaUrl: String,
        output: Path,
    ): Unit =
        when {
            xteaUrl.isBlank() -> {
                logger.info {
                    "`xteaUrl` parameter not set. " +
                        "Searching for `xteas.json` file in cache archive instead..."
                }
                retrieveXteaFileFromCacheDir(cacheArchiveDir, output)
                logger.info { "Retrieved `xteas.json` file and placed into: $output" }
            }
            xteaUrl.endsWith(".json") -> {
                logger.info { "Downloading xtea file from: $xteaUrl" }
                downloadXteaFile(client, xteaUrl, output)
                logger.info { "Downloaded xtea file." }
            }
            else -> {
                logger.info { "Downloading xtea file archive from: $xteaUrl" }
                TODO("Download and extract zip file that should contain xteas.json")
            }
        }

    private fun retrieveXteaFileFromCacheDir(cacheDir: Path, outputDir: Path) {
        val files = cacheDir.listDirectoryEntries()
        val xtea = files.firstOrNull { it.name == "xteas.json" }
        if (xtea == null) {
            throw FileNotFoundException(
                "`xteas.json` file could not be found in cache archive. " +
                    "`xteaUrl` program parameter must be set."
            )
        }
        xtea.moveTo(outputDir, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun downloadXteaFile(client: OkHttpClient, xteaUrl: String, outputFile: Path) {
        client.download(xteaUrl, outputFile)
    }
}

/** Validates a complete download before replacing the last accepted cache and XTEA pair. */
@OptIn(ExperimentalPathApi::class)
object CacheDownloadStaging {
    fun install(
        stagedCacheDir: Path,
        stagedXteaFile: Path,
        targetCacheDir: Path,
        targetXteaFile: Path,
        verifyIdentity: (Path, Path) -> Unit = ::verifyPinnedIdentity,
    ) {
        recoverInterruptedPromotion(targetCacheDir, targetXteaFile)
        validate(stagedCacheDir, stagedXteaFile)
        verifyIdentity(stagedCacheDir, stagedXteaFile)
        promote(stagedCacheDir, stagedXteaFile, targetCacheDir, targetXteaFile)
    }

    fun recoverInterruptedPromotion(targetCacheDir: Path, targetXteaFile: Path) {
        val parent = requireCommonParent(targetCacheDir, targetXteaFile)
        if (!parent.isDirectory()) {
            return
        }
        val backups = parent.listDirectoryEntries(".cache-backup-*").filter(Path::isDirectory)
        require(backups.size <= 1) {
            "Multiple interrupted cache promotions require manual recovery: $backups"
        }
        val backupDir = backups.singleOrNull() ?: return
        val committed = backupDir.resolve("committed").isRegularFile()
        if (!committed || !targetCacheDir.isDirectory() || !targetXteaFile.isRegularFile()) {
            recoverBackup(backupDir, targetCacheDir, targetXteaFile)
        }
        backupDir.deleteRecursively()
    }

    private fun validate(stagedCacheDir: Path, stagedXteaFile: Path) {
        require(stagedCacheDir.isDirectory()) { "Downloaded cache directory is missing." }
        val requiredCacheFiles =
            listOf("main_file_cache.dat2", "main_file_cache.idx255", "main_file_cache.idx0")
        requiredCacheFiles.forEach { filename ->
            val file = stagedCacheDir.resolve(filename)
            require(file.isRegularFile() && file.fileSize() > 0L) {
                "Downloaded cache is incomplete: missing or empty $filename."
            }
        }
        require(stagedXteaFile.isRegularFile() && stagedXteaFile.fileSize() > 0L) {
            "Downloaded XTEA file is missing or empty."
        }
    }

    fun verifyPinnedIdentity(stagedCacheDir: Path, stagedXteaFile: Path) {
        require(sha256(stagedXteaFile) == Build.XTEA_SHA256) {
            "Downloaded XTEAs do not match the pinned revision contract."
        }
        val manifest =
            CacheArchiveIndexManifestExporter.export(
                cacheDirectory = stagedCacheDir,
                revision = Build.FINAL_SUBREVISION,
                archive = Build.CACHE_ARCHIVE,
                capturedAt = Build.CACHE_CAPTURED_AT,
            )
        require(manifest.entries.size == Build.CACHE_GROUPS) {
            "Downloaded cache group count does not match the pinned revision contract."
        }
        require(
            manifest.entries.sumOf { it.attributes.getValue("fileCount").toInt() } ==
                Build.CACHE_FILE_IDENTITIES
        ) {
            "Downloaded cache file count does not match the pinned revision contract."
        }
        val manifestFile = createTempFile(stagedCacheDir.parent, ".archive-index-", ".json")
        try {
            CacheDeltaJson.writeManifest(manifestFile, manifest)
            require(sha256(manifestFile) == Build.CACHE_ARCHIVE_INDEX_MANIFEST_SHA256) {
                "Downloaded cache content does not match the pinned revision contract."
            }
        } finally {
            manifestFile.deleteIfExists()
        }
    }

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        path.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun promote(
        stagedCacheDir: Path,
        stagedXteaFile: Path,
        targetCacheDir: Path,
        targetXteaFile: Path,
    ) {
        val parent = requireCommonParent(targetCacheDir, targetXteaFile)
        parent.createDirectories()
        val backupDir = createTempDirectory(parent, ".cache-backup-")
        val backupCacheDir = backupDir.resolve("vanilla")
        val backupXteaFile = backupDir.resolve("xteas.json")
        Files.writeString(
            backupDir.resolve("promotion-state"),
            "cacheExisted=${targetCacheDir.exists()}\nxteaExisted=${targetXteaFile.exists()}\n",
        )
        var discardBackup = false
        try {
            if (targetCacheDir.exists()) {
                moveAtomically(targetCacheDir, backupCacheDir)
            }
            if (targetXteaFile.exists()) {
                moveAtomically(targetXteaFile, backupXteaFile)
            }
            moveAtomically(stagedCacheDir, targetCacheDir)
            moveAtomically(stagedXteaFile, targetXteaFile)
            Files.createFile(backupDir.resolve("committed"))
            discardBackup = true
        } catch (failure: Exception) {
            runCatching { recoverBackup(backupDir, targetCacheDir, targetXteaFile) }
                .onSuccess { discardBackup = true }
                .onFailure(failure::addSuppressed)
            throw failure
        } finally {
            if (discardBackup) {
                backupDir.deleteRecursively()
            }
        }
    }

    private fun recoverBackup(backupDir: Path, targetCacheDir: Path, targetXteaFile: Path) {
        val state =
            Files.readAllLines(backupDir.resolve("promotion-state"))
                .filter(String::isNotBlank)
                .associate { line ->
                    val (key, value) = line.split("=", limit = 2)
                    key to value.toBooleanStrict()
                }
        val cacheExisted =
            requireNotNull(state["cacheExisted"]) { "Promotion journal is missing cache state." }
        val xteaExisted =
            requireNotNull(state["xteaExisted"]) { "Promotion journal is missing XTEA state." }
        val backupCacheDir = backupDir.resolve("vanilla")
        val backupXteaFile = backupDir.resolve("xteas.json")

        if (backupCacheDir.exists()) {
            if (targetCacheDir.exists()) {
                targetCacheDir.deleteRecursively()
            }
            moveAtomically(backupCacheDir, targetCacheDir)
        } else if (!cacheExisted && targetCacheDir.exists()) {
            targetCacheDir.deleteRecursively()
        }

        if (backupXteaFile.exists()) {
            targetXteaFile.deleteIfExists()
            moveAtomically(backupXteaFile, targetXteaFile)
        } else if (!xteaExisted) {
            targetXteaFile.deleteIfExists()
        }
    }

    private fun requireCommonParent(targetCacheDir: Path, targetXteaFile: Path): Path {
        val parent =
            requireNotNull(targetCacheDir.parent) { "Cache target must have a parent directory." }
        require(targetXteaFile.parent == parent) {
            "Cache and XTEA targets must share one parent directory."
        }
        return parent
    }

    private fun moveAtomically(source: Path, target: Path) {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target)
        }
    }
}

private fun createTempArchiveFile(url: String, outputDir: Path): Path {
    val cacheArchiveName = url.substringAfterLast("/")
    val cacheArchivePrefix = cacheArchiveName.substringBeforeLast(".")
    val cacheArchiveExtension = ".${cacheArchiveName.substringAfterLast(".")}"
    return createTempFile(outputDir, cacheArchivePrefix, cacheArchiveExtension)
}

private fun OkHttpClient.download(url: String, output: Path) {
    val request = Request.Builder().url(url).header("User-Agent", "rsmod").build()

    newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Failed to download cache from: error=${response.code}, url=$url")
        }
        output.outputStream().use { outputStream ->
            response.body.byteStream().copyTo(outputStream)
        }
    }
}

private fun Path.extractFiles(outputDir: Path) {
    ZipInputStream(inputStream()).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val outputFile = outputDir.resolve(File(entry.name).name)
                outputFile.outputStream(StandardOpenOption.CREATE).use { output ->
                    zip.copyTo(output)
                }
            }
            entry = zip.nextEntry
        }
    }
}
