# Demonic Pacts cache upgrade

The active target is Old School RuneScape revision **238**, using [OpenRS2 archive 2565](https://archive.openrs2.org/caches/runescape/2565), captured **3 June 2026 at 13:45:06 UTC**. This is the last archived update while Demonic Pacts League was active, before the 11 June closing/reward-shop update. The compatible local client is RuneLite **1.12.28**. `238-20260603` is this project's snapshot label, not a claimed native minor revision.

The machine-readable [revision contract](revision-contract.json) pins the archive, checksums, protocol dependency and client tag. The original cache-233 reports remain historical evidence. Their earlier test totals do not establish revision-238 compatibility.

## Playing locally

From `rsmod`, run `./gradlew --offline run`. In another terminal run `../runelite/run-rsmod.sh`. The sibling client connects to port 43594, reads this world's RSA public key, and keeps its state in `.runelite-rsmod`. Use a disposable username and a nonempty test password. The default development XP multiplier is 150; use `::xprate 1` for base-rate comparisons.

Use `::giveleagueitems` on a development account to receive 34 League relic tools, echo weapons, echo orbs and the megarare voucher. Available inventory slots fill first, then the bank. A full inventory or bank does not delete existing belongings. Banker’s briefcase, Evil eye and Map of alacrity have destination menus and saved Last-destination actions. The briefcase and eye work when worn too. The catalogs contain 121 bank, 65 Slayer and 157 Agility arrivals, each checked against revision-238 collision. Traversal Agility requirements do not gate the map teleport; listed quest requirements still do.

This cache provides the League models, items, maps and interfaces. A full Demonic Pacts mode, pact progression, League tasks and every relic passive are separate gameplay work. New Sailing terrain and definitions likewise do not implement the Sailing skill.

## Changes and save compatibility

The upgrade migrates the network protocol, map archive layout, config codecs, clientscript parsing, native symbols and interface references together. Reference hashes remain checked. [Identity changes](identity-changes.tsv) records the 118 changed pinned identities, including moved numeric IDs.

Save migration V12 moves the 17 server-specific inventory types from 936–952 into 65000–65016 because the new native cache uses the former IDs. Item IDs are preserved. The production database must be backed up before the first revision-238 start, and its own migration must run; a staging database must never replace current player saves.

Shadow charging validates the item and capacity before taking runes, accepts a charged variant with zero saved charges, and rejects stale dialog slots. The shared charge manager and bank charging follow the same order. Bryophyta charging follows its actual item lifecycle. The combat tab now opens the appropriate autocast selection and validates spell eligibility for weapons such as Kodai.

## Shops and banks

Bank operations have native NPC and location fallbacks where no bespoke handler exists. Added merchants use historical stocks, currencies, and quest prerequisites. [Shop coverage](shop-coverage.tsv) records each of the 504 source variants and distinguishes stock catalogs, existing bespoke interactions, counters, reward shops and unresolved historical data. A bespoke interaction entry does not certify every item in that shop. [Restock limitations](shop-restock-pending.md) names quantities whose historical replenishment intervals are unavailable.

Item currencies include Tokkul, trading sticks, mermaid tears, and reward-shop tickets or points. Exact per-item GP prices preserve special storefront pricing. Reward stores consume saved balances; this work does not automatically award points for unimplemented minigame activities. Quest-gated shops read existing saved quest progress. Quest progression remains deferred.

## First playtest

1. Log in, walk around Lumbridge, open inventory and equipment, and log out and back in.
2. Run `::giveleagueitems` with an empty inventory, then on a disposable account with a full inventory. Check inventory and bank totals.
3. Use all three teleport tools, select a destination and use Last-destination. Wear the briefcase and eye and repeat.
4. Open banks in Lumbridge, Varrock and a destination reached through the briefcase. Deposit, withdraw, swap/insert and withdraw notes. Check the deposit box separately.
5. Charge an uncharged Shadow and a spawned charged Shadow with zero saved charges. Confirm five chaos and two soul runes are consumed per charge. Try insufficient runes and a full staff; no resources should disappear.
6. Equip Kodai, open combat styles and choose a supported autocast spell. Attack a valid target, then change to an incompatible spellbook or weapon and check that selection is cleared appropriately.
7. Buy and sell at an ordinary store, a Tokkul shop, a pub counter and an exact-price store. Check the value preview and actual payment agree, including bulk purchases.
8. Revisit the [skills and diary playtest](../cache-233/SKILLS_AND_DIARIES.md) with revision-238 results recorded separately.

## Verification

The revision-238 checkpoint has **5,005 automated tests**: **3,242 integration** and **1,763 unit** tests. Cache packing, strict native-reference verification and the active revision lock pass. This includes the Shadow charging regressions, Bryophyta charging, autocast selection, inventory/bank overflow, equipped and carried relic teleports, and revision-238 enclosed zone updates. The full matrix is run with `./gradlew --offline --parallel --max-workers=3 test integration validateCacheRevision validateCache233Parity`. The latter also checks historical cache-233 evidence without relabelling it as new gameplay verification.

A process-isolated client test logs a disposable account into the actual port-43594 world, loads Lumbridge, grants the 34 League items, moves to the castle bank and opens it through a banker. It verifies six overflow items through the native bank interface. The final server starts 624 scripts and loads 4,818,408 locations with strict reference checks enabled. Source changes from the charging, autocast, banking/shop and cache-upgrade work are present together in the active checkout. The emulator client contains the previous local-client history, the 1.12.28 upstream source and the tested local configuration changes. No client commits are pushed to RuneLite.

The live save remains the user's existing database. The pre-upgrade cache and a consistent schema-v11 SQLite backup are retained under `.data/backups/rev233-before-238-20261113/`; the live database passes its integrity check after migration to v12. Local backup branches retain the old server and client commits.

The shop audit represents 476 of 504 source variants, including 63 entries with existing bespoke interactions whose complete stock has not been independently compared. The other 28 are individually classified in the coverage file: retired events, locations outside this cache, special interfaces, Deadman-only stock, shops without stock at the cutoff, and unresolved data. Current unresolved store coverage includes Ali's Discount Wares, Multicannon parts, Ramarno, part of Trader Stan's stock, Perdu/Chris reclaim entitlements and five unmatched historical source titles. Four added merchant positions are collision-checked representative trading positions within documented footprints, rather than exact historical spawn observations. Unpublished restock intervals remain disabled and documented.

Client checks still need ordinary player coverage of all shop stocks, bank controls, spells, skills, diary routes and encounter presentation. The complete Demonic Pacts game mode, additional Sailing gameplay and quest progression are outside this cache-upgrade checkpoint. All 23 earlier skill modules and the diary work remain included.

During migration, genuine compatibility fixes were required for all model IDs (including small IDs), native bank and quantity-interface children, essence-pouch Check/Empty order, Mixology price columns, abstract Construction nail requirements, Honest Jimmy's reward prices, new cooking appliances, and the new frost-dragon/strykewyrm bone recipes. The revised spellbook component pins and native tradeability fixtures are checked against the selected archive's canonical symbols and definitions.

The new Prayer values follow the [Strykewyrm bones](https://oldschool.runescape.wiki/w/Strykewyrm_bones) and [Frost dragon bones](https://oldschool.runescape.wiki/w/Frost_dragon_bones) references. Honest Jimmy's prices follow Wiki revision [15142206](https://oldschool.runescape.wiki/w/Special:PermanentLink/15142206).
