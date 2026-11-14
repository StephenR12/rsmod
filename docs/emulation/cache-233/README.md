# Cache 233 parity programme

> The active world now targets [revision 238 for Demonic Pacts](../cache-238/README.md). This report records the earlier cache-233 checkpoint.

This directory is the durable record for production-game parity with OSRS cache 233.4 (1 October
2025). The records deliberately separate cache identity from gameplay claims: cache definitions
show what the client contains, while dated Wiki and official sources establish server behaviour.

The machine-readable source is `manifest.json`. Validate its schema and the repository revision lock
with:

```shell
./gradlew validateCache233Parity
```

When the ignored local archive-index audit export is present, validation also hashes its full
contents. Fresh clones still validate the pinned artifact path and checksum without requiring the
70 MB generated audit file in Git.

Print current totals with:

```shell
./gradlew reportCache233Parity
```

The strict completion gate is intentionally red until every entry is verified:

```shell
./gradlew validateCache233Parity -PcacheParityRequireComplete=true
```

The manifest currently contains the first-pass feature families. It is not an assertion that the
feature universe is fully enumerated. Each family must be expanded into atomic, testable interactions
as cache and historical-Wiki inventories are produced.

`STANDARD_SPELLBOOK.md` is the explicit implemented-versus-missing inventory for all cache-233
standard spellbook entries.

The [in-game skill and diary test guide](SKILLS_AND_DIARIES.md) contains the local launch commands, coverage for all 23 skills, diary checks, current test results and deferred work.
