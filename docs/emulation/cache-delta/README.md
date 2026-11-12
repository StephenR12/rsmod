# Deterministic cache delta manifests

`api/cache-delta` compares two versioned cache-inventory manifests and emits a stable, reviewable
JSON delta. It is deliberately independent from cache 233 gameplay code: a later revision can be
inventoried without overwriting the cache-233 evidence.

Each input manifest records its schema version, revision, OpenRS2 archive, capture timestamp and a
flat list of entries. Every entry has a stable `domain` plus `key`, an optional typed symbol, a
content fingerprint, sorted string attributes and explicit risk tags. Inventory exporters should
cover objects, NPCs, locations, maps, interfaces, client scripts, vars, enums, structs, params,
database tables and rows, sequences, spot animations, projectiles, sounds, music and protocol
metadata. The fingerprint must represent all behavior-relevant decoded fields for that domain.

The first production exporter covers every object, NPC and location definition. It fingerprints the
exact raw per-definition config bytes and records the decoded behavior fields that make a review
useful: names and operations, object equipment/state/parameter metadata, NPC combat/movement/morph
metadata and location collision/size/morph/sound metadata. Typed symbols come from the matching
`obj.sym`, `npc.sym` and `loc.sym` files; missing symbols stay explicit instead of being invented.

Export the core inventory with:

```shell
./gradlew :api:cache-delta:run \
  --args='export-core /absolute/path/cache /absolute/path/symbols 233.4 2317 2025-10-01T10:45:07.114859Z /absolute/path/core-manifest.json'
```

The checked local 233.4 audit at `.data/audit/cache-233.4-core-manifest.json` contains 103,695
entries: 31,172 objects, 14,799 NPCs and 57,724 locations. Its SHA-256 is
`baf6400cac0b057c6050b102347f22c55dc1b61499c9ed7fe5171df067ba88f5`.

The broader semantic-config exporter includes those core definitions and every cached varp, varbit,
varclient, vanilla varobject, inventory, parameter, enum, struct, database table/row, sequence, spot
animation and, when present, projectile-motion config:

```shell
./gradlew :api:cache-delta:run \
  --args='export-configs /absolute/path/cache /absolute/path/symbols 233.4 2317 2025-10-01T10:45:07.114859Z /absolute/path/semantic-config-manifest.json'
```

It records all decoded behavior fields in canonical form as well as each definition's exact raw
fingerprint. All 1,261 varclients expose their persistence contract (494 persistent and 767
transient) and have revision-matched symbols. The 107 vanilla varobjects are retained as explicit
opaque records because no semantic decoder or matching vanilla symbol exists yet. Empty/sentinel
enum records without key/value literals are likewise retained instead of silently discarded. Cache
233.4 has no vanilla `PROJANIM` config group; its 16 `projanim.sym` identities are supplied by
rsmod's build layer, so the exporter records zero rather than fabricating cache entries. A later
cache with that group will export all seven trajectory/timing fields. Two independent exports of the
checked 233.4 cache produced the same 116,612,793-byte, 165,477-entry file across 15 domains, byte
for byte. The local audit is
`.data/audit/cache-233.4-semantic-config-manifest.json`; its SHA-256 is
`fe869b9fc02b89a41778a4d8b92695c2b5248ec409192c990acb01d1b53fff7a`.

The 16 projectile profiles that rsmod adds to its runtime cache are inventoried separately, with an
explicit enriched revision label so they cannot be mistaken for vanilla archive records:

```shell
./gradlew :api:cache-delta:run \
  --args='export-projectiles /absolute/path/enriched-cache /absolute/path/symbols 233.4-rsmod-enriched 2317 2025-10-01T10:45:07.114859Z /absolute/path/projectiles.json'
```

The checked `.data/audit/cache-233.4-rsmod-projectile-profiles.json` contains all 16 symbol-backed
profiles and has SHA-256
`3ef92868bd34edeaaf3bc14514ea0c7e4d4ceda6fc7bd0901d6fab8b797d5def`. A second independent
export is byte-identical; a production self-diff reports 16 unchanged and no changes.

Server-owned variable namespaces are inventoried separately for the same provenance reason:

```shell
./gradlew :api:cache-delta:run \
  --args='export-server-variables /absolute/path/enriched-cache /absolute/path/symbols 233.4-rsmod-enriched 2317 2025-10-01T10:45:07.114859Z /absolute/path/server-variables.json'
```

The command decodes enriched-cache varns and optional varnbits, and fingerprints canonical
rsmod build definitions for varobjects, varcons and controllers. Cache 233.4 contains six varns,
eleven build-layer varobjects, three varcons and one controller; its varnbit and varconbit namespaces
are empty. All 21 records have symbols. The checked
`.data/audit/cache-233.4-rsmod-variable-namespaces.json` is 7,704 bytes with SHA-256
`7ecefc34c096609e2e65d57056fc654723dcc81838db44a1063d579d06c79ff4`. A second independent
export is byte-identical; a production self-diff reports 21 unchanged and no changes.

Terrain and XTEA-protected location maps have their own vanilla-cache inventory:

```shell
./gradlew :api:cache-delta:run \
  --args='export-maps /absolute/path/cache /absolute/path/xteas.json 233.4 2317 2025-10-01T10:45:07.114859Z /absolute/path/maps.json'
```

The exporter requires one unique, four-word archive-5 XTEA record with the correct landscape-group
name for every square. It fingerprints the full decrypted terrain and location files, records the
terrain rules that feed collision (blocked, link-below and roof flags), rendering attributes,
location placement/type/shape/level totals, and a non-reversible XTEA-key fingerprint. The checked
`.data/audit/cache-233.4-map-square-manifest.json` contains 2,266 terrain and 2,266 location records,
is 3,342,129 bytes, and has SHA-256
`55b0e443569f8f22482f4a8897683b52224d31bd727e2a574966b97f4f6ec3df`. Two independent exports
are byte-identical; a production self-diff reports 4,532 unchanged and no changes. All terrain files
contain bytes beyond the existing four-plane server and workspace RuneLite decode: 2,265 have one
remaining byte and map 64_73 has 9,564. Those bytes remain inside the exact raw fingerprint and are
reported explicitly rather than silently treated as decoded semantics.

The byte-oriented client-contract exporter inventories formats that do not yet have semantic
decoders without pretending to understand their behavior:

```shell
./gradlew :api:cache-delta:run \
  --args='export-client-contracts /absolute/path/cache 233.4 2317 2025-10-01T10:45:07.114859Z /absolute/path/client-contracts.json'
```

It covers every file in animation frames and bases, interface components, client scripts,
keyframe sets, sound effects, songs, jingles, Vorbis samples and MIDI patches. Each stable
`domain/group:file` identity retains exact file bytes as SHA-256 plus archive, group, file, version,
checksum, length and name-hash metadata from the JS5 index. The checked
`.data/audit/cache-233.4-client-contract-archives.json` contains 225,204 files and has SHA-256
`3214d8b6c2a331401ee95dcbf1727d6e26da945ca5d606607a2f7c5325437e5d`. Two independent exports
are byte-identical; a production full-scale self-diff reports 225,204 unchanged and no changes.
This closes raw change detection for those archives, not semantic interpretation or gameplay parity.

The archive-index exporter inventories the structural identity of the entire untouched vanilla cache without
needing to interpret or decrypt group payloads:

```shell
./gradlew :api:cache-delta:run \
  --args='export-archive-indexes /absolute/path/cache 233.4 2317 2025-10-01T10:45:07.114859Z /absolute/path/archive-indexes.json'
```

Every JS5 group is keyed by archive/group ID and fingerprints its version, compressed and
uncompressed checksums and lengths, group name hash, and exact ordered file ID/name-hash set. This
makes an added, removed, renamed, resized or replaced cache group visible even when no semantic
decoder exists or its payload needs XTEA keys. The checked
`.data/audit/cache-233.4-archive-index-manifest.json` contains 116,016 groups across 23 non-empty
archive indexes and 640,720 files, is 73,039,829 bytes, and has SHA-256
`26ef85c11abc5a056df60c9cbc264f031e442ae2d455d49e1a5fa33c295227ae`. Two independent exports
are byte-identical; a production self-diff reports 116,016 unchanged and no changes. The official
OpenRS2 archive-2317 record independently reports 116,016 groups and identifies it as the
oldschool/live/en build-233 cache captured on 2025-10-01.

rsmod's `.data/cache/game` is a derived runtime cache and must never be substituted for that
baseline. Its separately labelled
`.data/audit/cache-233.4-rsmod-enriched-archive-index-manifest.json` contains 116,389 groups and
641,449 files, is 73,266,266 bytes, and has SHA-256
`d0439f11fa33e31d9e29f28d6d22535db843727d5130e0c36660003e5c4cd791`. The vanilla-to-runtime
delta is explicitly pinned at 373 added groups, 12 changed groups and 116,004 unchanged groups:
five server-owned config groups and 368 map groups are added, while twelve existing archive-2
config groups are rewritten by the enrichment pipeline.

Run a diff with:

```shell
./gradlew :api:cache-delta:run \
  --args='/absolute/path/baseline-manifest.json /absolute/path/target-manifest.json /absolute/path/delta.json'
```

Absolute paths avoid ambiguity because Gradle runs the application from the module project
directory. Relative paths are accepted and resolved from that directory.

Turn the same two manifests directly into an atomic upgrade backlog with:

```shell
./gradlew :api:cache-delta:run \
  --args='plan-upgrade /absolute/path/baseline-manifest.json /absolute/path/target-manifest.json /absolute/path/backlog.json'
```

The planner emits exactly one task per changed `domain/key` identity. Stable hash-based task IDs do
not depend on list position. Every task retains both fingerprints and symbols, lists the exact
changed fields and risk tags, and requires baseline/target cache evidence, revision-matched symbols,
official Wiki or update evidence, an implementation-or-evidence-backed no-op decision, cache-233.4
regression coverage, target-contract coverage, manifest updates and the full unit/integration gate.
Map/collision, client-contract and protocol changes are classified as critical; other risk-bearing
or non-additive changes are high; risk-free additions are normal. Reversing input entry order
produces byte-identical backlog JSON.

The tool rejects unsupported schemas, incomplete revision metadata, blank identities or
fingerprints, and duplicate domain/key pairs. It canonicalizes source order, attributes and risk
tags before comparing. Output changes are ordered by domain and key and classified as `Added`,
`Removed` or `Changed`. It also derives migration flags for typed-symbol changes, morph changes,
maps/collision, client contracts and protocol-sensitive domains while preserving exporter-supplied
risk tags.

This module now supplies the deterministic comparison layer, core object/NPC/location export, the
behavior-defining semantic config families listed above, the separate rsmod projectile and variable
inventories, exact map/landscape change detection with decoded collision-sensitive summaries, exact
raw change detection for ten client-contract archives, complete JS5 archive/group structural
identity and deterministic generation of atomic, evidence-gated upgrade tasks.
Semantic decoders for interfaces/components, client scripts, raw animations and audio/music still
remain, as do per-tile map/collision semantic deltas beyond the decoded summaries,
vanilla-varobject semantics, protocol metadata and authoritative archive acquisition/provenance
verification. They must be added and validated before the future-cache completion gate can pass.

## Input example

```json
{
  "schemaVersion": 1,
  "revision": "233.4",
  "archive": 2317,
  "capturedAt": "2025-10-01T10:45:07.114859Z",
  "entries": [
    {
      "domain": "objects",
      "key": "22370",
      "symbol": "bryophytas_staff",
      "fingerprint": "sha256:domain-specific-canonical-value",
      "attributes": {
        "operations": "Wield,Check,Uncharge"
      },
      "riskTags": [
        "item-charge-risk"
      ]
    }
  ]
}
```
