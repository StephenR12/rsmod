# Cache upgrade playbook

Do not run this playbook while cache 233 parity is the active target. It defines the later,
repeatable transition to a user-selected revision.

The first planned post-baseline target is cache
[238.1](https://oldschool.runescape.wiki/w/RuneScape:Cache/2026/238.1). This records intent only:
do not replace the accepted 233.4 cache, symbols, protocol or evidence before every regular-game
233.4 feature is closed. At migration time, create a sibling `cache-238` evidence directory and
execute the complete inventory/delta/backlog process below; never rewrite the 233.4 baseline.

Selectable League worlds are a separate post-regular-game world-profile project, not an automatic
consequence of loading cache 238.1. Keep the normal profile authoritative and place League season,
ruleset and world identity behind explicit configuration. Each supported League requires its own
versioned inventory for world flags and login selection, account/progression namespace, XP/drop
multipliers, area locks, relics/passives, tasks/points, unlock interfaces, seasonal items/NPCs/
maps, deaths, shops and rewards. Tests must prove that selecting or restarting a League world
cannot leak state or rules into normal worlds and that clients are revision-matched to both.

1. Copy `revision-contract.json` for the new revision and record the requested major/subrevision,
   game/environment/language, release date, cache archive ID and capture timestamp, exact vanilla
   archive-index and XTEA checksums, download/catalog URLs, protocol artifact version/modules and
   authoritative update pages. Update `Build` and the dependency catalog to the same contract in
   one change; `validateCache233Parity` must reject every mixed-revision combination.
2. Copy the prior revision manifest and ledgers into a new revision directory. Never overwrite the
   cache-233 evidence.
3. Acquire the target vanilla cache into a staging location, export and review its whole-cache
   archive-index manifest, then pin the accepted cache and XTEA SHA-256 values in the new revision
   contract. The built-in downloader extracts into a sibling staging directory, validates required
   disk indexes, regenerates the exact pinned archive-index manifest and verifies the XTEA checksum
   before moving either accepted path. Validation failures preserve the current cache; promotion
   failures attempt to restore both backups. Promotion writes the original-path state before its
   first move and commits only after both new paths exist. Server startup idempotently restores an
   incomplete journal or keeps a committed pair; multiple journals require manual resolution.
4. Generate inventories for all cache domains and diff them against the prior manifest: archives,
   objects, NPCs, locations, maps, interfaces/components, client scripts, vars, enums, structs,
   database rows/tables, animations, spot animations, projectiles, sounds and music.
   The implemented semantic-config exporter accepts an independently staged cache and its
   revision-matched symbols: `./gradlew :api:cache-delta:run --args='export-configs <cache-dir>
   <symbols-dir> <revision> <archive-id> <captured-at> <manifest.json>'`. It includes objects, NPCs,
   locations, player variables/bitfields, varclients, raw vanilla varobjects, inventories, params,
   enums, structs, database tables/rows, sequences, spot animations and any vanilla
   projectile-motion config group. Cache 233.4 has no
   such projectile group; its 16 projectile profiles are rsmod build-layer definitions. Export those
   from each enriched runtime cache with `export-projectiles <enriched-cache-dir> <symbols-dir>
   <revision>-rsmod-enriched <archive-id> <captured-at> <manifest.json>` and diff those two manifests
   separately. Inventory the enriched server-variable layer with
   `export-server-variables <enriched-cache-dir> <symbols-dir> <revision>-rsmod-enriched <archive-id>
   <captured-at> <manifest.json>` and diff that separately as well. It covers cache-backed varns and
   optional varnbits plus rsmod build-layer varobjects, varcons and controllers; the current
   varnbit/varconbit sets are explicitly empty. Run `export-client-contracts <cache-dir> <revision> <archive-id> <captured-at>
   <manifest.json>` against each vanilla cache to inventory every raw animation frame/base,
   interface component, client script, keyframe, sound effect, song, jingle, Vorbis sample and MIDI
   patch. A raw delta proves exact file change only; every changed file still needs its applicable
   semantic decoder or client-facing review before closure. `export-core` remains available for the
   smaller legacy core inventory. Generate the target revision's symbols before this step; never
   silently reuse cache-233 symbols for a later cache.
   Run `export-archive-indexes <untouched-vanilla-cache-dir> <revision> <archive-id> <captured-at>
   <manifest.json>` for both vanilla caches. Never point this baseline export at rsmod's derived
   `.data/cache/game` directory. It fingerprints every JS5 archive/group contract, including version,
   checksums, lengths, names and exact ordered file identities, without needing payload decryption.
   Diff this whole-cache inventory before any semantic delta so newly added, removed or replaced
   undecoded groups cannot escape the backlog. Reconcile its archive/group totals, game/build,
   language and capture timestamp with the authoritative archive catalog before accepting it. Then
   export the enriched runtime cache under an explicit `<revision>-rsmod-enriched` label and diff it
   against the accepted vanilla manifest. Any enrichment delta outside the expected build pipeline
   is a release blocker.
   Run `export-maps <cache-dir> <xteas.json> <revision> <archive-id> <captured-at> <manifest.json>`
   for each vanilla cache. It requires a unique, correctly named four-word landscape key for every
   square and emits separate terrain and location identities with exact decrypted fingerprints and
   collision-sensitive decoded summaries. Treat every raw change and every newly reported decoder
   remainder as critical until a per-tile semantic review accounts for it.
   Separately run `export-npc-spawns <enriched-cache-dir> <xteas.json> <symbols-dir> <revision>-rsmod-enriched
   <archive-id> <captured-at> <manifest.json>` for each enriched runtime. It inventories the
   server-only `nX_Z` groups actually loaded by `GameMapDecoder`, preserving raw group fingerprints
   and canonical per-placement identity, coordinates, plane, duplicate ordinal, NPC type fields,
   symbols and combat/drop risks. Diff enriched against enriched; never present these build-layer
   placements as vanilla-cache evidence. Any target revision also needs its production-wide spawn
   source reconciled separately, because a stable local enrichment diff cannot prove that omitted
   regions are complete.
   Once the two inventories exist, run the deterministic comparison layer with absolute paths:
   `./gradlew :api:cache-delta:run --args='<baseline.json> <target.json> <delta.json>'`. Review the
   stable risk tags, then generate the one-change-per-task backlog with
   `./gradlew :api:cache-delta:run --args='plan-upgrade <baseline.json> <target.json>
   <backlog.json>'`. The listed semantic config families, raw client-contract archives and
   deterministic backlog generator are implemented; the remaining semantic and domain exporters
   are still required.
5. Classify each delta as added, removed, renamed, morphed, value-only, map-only, interface-only,
   protocol-sensitive, behavioral-risk, beta/test or production-accessible.
6. Update `Build.MAJOR`, cache/XTEA URLs and both rsprot dependencies together. Refuse mixed-revision
   startup.
7. Regenerate typed symbols and compile before editing gameplay. Map renamed identities explicitly;
   do not replace them with raw IDs. Diff the revision-matched client's world-type masks against
   `WorldFlag` and migrate persisted `worlds.flags` deliberately if any mask changes. For
   Firemaking, diff every combustible log identity and Light operation, level/XP/membership table,
   attempt chance and cadence, fire/ashes identities, lifetime range, movement order and newly
   production-accessible special-lighting route before carrying the ordinary-log contract forward.
   Separately diff the Barbarian bow log table, eligible and prohibited bow identities, training
   varbit layout/state meanings, per-bow sequences and spot animation before carrying that contract
   forward. Diff all five loose firelighter, coloured-log and coloured-fire identities; the
   uncharged/charged holder forms and inventory operations; holder capacity, colour/charge state,
   accepted log families, load/uncharge behavior and one-charge consumption before carrying the
   coloured-fire contract forward. For pyre-log preparation, diff every source/output identity,
   sacred-oil dose object and consumption rule, quest-step exception and completion boundary, members/level gates,
   quantity interface and maximum, immediate manual and automatic cadence, base XP, Pyromancer
   modifiers, feedback and audiovisual ordering. For Shades of Mort'ton funeral pyres, diff every
   log/remains/stage/stand identity and operation, compatibility and level table, quest-state rules,
   Firemaking and Prayer XP, diary modifiers, coin probability/ranges, key weights, reward ownership
   and lifetime, stage timing, stand selection and audiovisual/packet ordering. For Barbarian pyre
   ships, diff all site, stage, log, bone, axe, bow, NPC, reward and var identities; the dual skill
   table, construction and spirit timing, training/diary transitions, Prayer-charge rules and cap,
   Ferocious-spirit combat ownership/drop table, and site transforms before carrying the contract
   forward. For handheld light sources, diff every unlit/lit pair, Firemaking level, membership and
   inventory/worn operation slot; candle-lantern assembly, tinderbox routing, dry-stick level and
   Rub-together semantics, XP, brightness, extinguishability, gas behavior, darkness timing and all
   quest/diary/area hooks. For lamp oil, diff swamp tar, both still locations and map placements,
   every empty/filled container pair, loaded-state representation, one-dose behavior, membership,
   persistence/instance scoping, presentation and the Rimmington-only impling-jar route. For
   Herblore herb cleaning, diff every grimy/clean identity, level and XP row, inventory operation,
   membership and quest gate, immediate/manual request policy, automatic cadence and selection
   order, interruption, messages, audio and packet ordering before carrying the contract forward.
   For unfinished potions, diff every herb, liquid and output identity, level, XP, membership and
   quest rule, quantity interface, manual/automatic cadence, inventory placement, interruption,
   sequence, sound, messages and packet ordering, keeping water and blood bases distinct.
   For finished potions, diff every unfinished input, secondary, three-dose and eligible four-dose
   output, level, XP, quantity behavior and cadence, plus chemistry-amulet and alchemist's-amulet
   identities, charge storage, rolls and depletion transforms, both prescription-goggle identities
   and preservation rules before carrying the recipe or equipment contract forward. Diff the
   chemistry-amulet Break/Options operations, shared-charge reset and production messages; the
   alchemist's Charge/Check/Uncharge and goggles Change operation slots, transcript text, count
   limits, near-cap behavior, noted refunds, bank-side routes, initial charges and audiovisual packet
   order as a separate lifecycle contract. For ingredient crushing, diff every pestle, input and
   output identity, membership, level/XP rule, selected-slot behavior, initial/repeat cadence,
   sequences, feedback and interruption. Diff crystal shards separately for quest state, count
   prompt, 1:10 conversion, all-at-once execution, capacity and stale-dialogue behavior. Diff lava
   scales separately for all three interpolated success rolls, visible-level treatment, the Hard
   Wilderness Diary varbit and floored 50% yield multiplier. Diff the free-to-play knife/chocolate
   route separately for tool identity, cadence and presentation. Diff Gorak claws separately for
   quest-item acquisition, possession-based processing and its uniform three-tick repeat. Keep these
   families separate from Suqah tooth's delayed 1/2/3 cadence, the additional fixed 0/2/3 routes,
   the three immediate one-shot quest ingredients, stack-to-non-stack capacity, explicit quest-state
   recipes including black dye's mushroom/vial/The Golem gate, raw/cooked karambwanji cadence,
   blamish slime's three snail identities/sample bottles/0-then-1/2/3 cadence, multi-input
   processing, blamish oil's explicit no-quest gate and Wesley services.
   Diff both oily-rod conversions separately for zero/one-tick timing, rod identity, reversal and
   continued no-Heroes'-Quest access.
   Recalculate the
   maximum loc footprint, diff generic door content-group membership and
   shared door translations, and compare every published prohibited-area polygon/message before
   carrying fire placement forward; do not import post-cutoff Sailing log rows or Sailing-only
   placement messages into an older cache baseline.
8. Import each generated atomic task as one feature row. Resolve every required cache, symbol,
   dated Wiki/update and risk-specific evidence item; record either the implementation or an
   evidence-backed no-op; and satisfy every generated acceptance criterion before closing it.
9. Implement and verify the delta while running the entire prior revision regression suite.
10. Run RuneLite connection and gameplay acceptance using a client matched to the new revision.
11. Mark the new revision supported only when all delta rows are verified and no prior verified row
    regressed.

The future diff tool must produce stable, reviewable machine-readable output so the same cache pair
always yields the same ordered delta apart from explicitly recorded metadata.
