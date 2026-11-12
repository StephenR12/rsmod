package org.rsmod.server.install

import java.nio.file.Path
import org.junit.jupiter.api.Test

class CacheDownloadStagingIntegrationTest {
    @Test
    fun `accepted cache and xteas match the complete pinned identity`() {
        CacheDownloadStaging.verifyPinnedIdentity(
            stagedCacheDir = Path.of(".data/cache/vanilla"),
            stagedXteaFile = Path.of(".data/cache/xteas.json"),
        )
    }
}
