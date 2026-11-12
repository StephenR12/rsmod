# Implementation log

## 2026-09-09 — parity programme bootstrap

- Confirmed `Build.MAJOR = 233`; the initial OpenRS2 archive selection was 2293. A later
  provenance audit identified archive 2293 as the 10 September 233.1 capture and corrected the
  final 233.4 baseline to archive 2317, as recorded below.
- Confirmed both rsprot artifacts target OSRS revision 233.
- Confirmed the repository is clean on `main` before parity work.
- Found 26 content projects, 45 local symbol files and 134 explicit TODO/unimplemented matches.
- Confirmed the final production subrevision is 233.4 on 1 October 2025.
- Created the durable parity records and initial machine-readable manifest.
- Added `validateCache233Parity` and `reportCache233Parity` Gradle tasks.
- Recorded that root `integrationTest` is not a valid task; this was task-selection failure, not a
  product-test failure.
- Refactored standard elemental max-hit scaling into an explicit tier model used by live spell
  attacks.
- Added boundary unit tests for all five elemental tiers and a cache-aware test proving that all 20
  elemental combat spell objects register an attack implementation.
- Passed repository-wide unit tests and all 16 cache-aware integration suites: 337 Gradle tasks,
  zero failures.
- Passed the cache-233 manifest validator with 51 feature groups: 45 discovered, 4 researched, 1
  unit-tested and 1 integration-tested.
- Corrected the canonical death-rune reference, which incorrectly resolved to chaos rune, and added
  missing body, cosmic, astral and wrath rune references needed by the production spellbooks.
- Added a cache-backed contract test for all 20 elemental spells covering spellbook/type, required
  level, cast XP, rune quantities and original cache max hit.
- Made integration-test networking use an OS-assigned ephemeral port so the suites can run while a
  local rsmod server remains online.
- Re-ran formatting, manifest validation, repository unit tests and all 16 integration suites with
  the live server still bound to port 43594: 594 Gradle tasks, zero failures. The manifest now has
  53 feature groups: 45 discovered, 4 researched, 1 unit-tested and 3 integration-tested.
- Expanded the standard spellbook into an explicit 71-spell plus two-submenu gap inventory. The
  manifest now validates 54 groups; the newly inventoried family remains `discovered` until its
  individual entries receive dated research and behavioral evidence.
- Added deterministic integrated Wind Strike coverage for atomic level/rune rejection, ordinary
  and staff-substituted rune consumption, cast/damage/Hitpoints XP, hit and splash visual effects,
  elemental weakness matching and autocast target re-engagement.
- Added a combat-dispatch test proving the standard five-tick cast cadence and that attempted early
  repeats consume neither runes nor XP. Kept the production dispatcher internal by placing the
  test in its module friend source set.
- Corrected the game-test injector so `ZoneUpdateMap` is a true singleton, matching the lifecycle
  assumed by `WorldRepository` and making emitted projectile updates observable by integrations.
- The manifest now validates 55 feature groups: 46 discovered, 4 researched, 1 unit-tested and 4
  integration-tested. Elemental gameplay is still not client-tested or verified.
- Implemented Confuse, Weaken, Curse, Vulnerability, Enfeeble and Stun on a shared non-damaging
  spell-attack/effect framework. Added typed cache-editor metadata for affected stat and percentage,
  cache 233 sequences/spotanims, target-owned delayed impacts and retaliation without a damage
  event or defend animation.
- Added the six spells to combat-formula classification and made a charged Tome of Water apply its
  cutoff +20% curse accuracy and +50% drain effectiveness. Tome charge storage, consumption and
  empty-item transition remain explicitly incomplete.
- Added seven cache-aware spell integrations covering all six metadata/registration contracts,
  NPC/player impact, splash, XP, non-stacking/rejection, Tome drain amplification and distinct
  Vulnerability effects. Added formula-operation tests for Tome curse accuracy and a dispatcher
  test proving the five-tick curse cadence.
- Removed a test arrangement that initialized a full server from the ordinary unit-test source set;
  the behavior is now exercised in the intended cache-aware integration and combat friend suites.
- The manifest now validates 58 feature groups: 47 discovered, 5 researched, 1 unit-tested and 5
  integration-tested. Neither the elemental family nor the stat-drain family is client-tested or
  verified.
- Implemented Bind, Snare and Entangle on a shared target-owned movement-binding repository intended
  for reuse by later Ancient and Arceuus freezes. Added exact held/immune rejection, 8/16/24-cycle
  holds, five-cycle refreeze immunity, ten-tile PvP release, Swampbark duration bonuses and a red
  fade warning four cycles before release.
- Separated cache 233's generic max-hit metadata value of 1 from the binding spells' source-backed
  gameplay caps: Bind is non-damaging, Snare caps at 2 and Entangle caps at 5. Successful-cast and
  splash XP use the distinct 31/61/89 and 30/60/70 values.
- Added eight cache-aware integrations covering contracts, effects, XP, splash, damage, atomic
  rejection, immunity, movement, Swampbark, PvP release and fade warning. Added a dispatcher test
  for five-tick binding cadence and a formula regression for the Tome of Water accuracy modifier.
- Left warning audio, production NPC freeze resistance/immunity, Tome charge consumption and
  RuneLite confirmation explicitly incomplete.
- The manifest now validates 61 feature groups: 48 discovered, 6 researched, 1 unit-tested and 6
  integration-tested. No binding row is yet client-tested or verified.
- Passed repository-wide formatting, manifest validation, unit tests and all 16 cache-aware
  integration suites after the binding batch: 595 Gradle tasks, zero failures.
- Implemented Tele Block as a player-only Wilderness combat spell using a shared target-owned state
  repository intended for all future teleport handlers. Added 500/250-cycle blocking, the cache and
  RuneLite-defined 100-cycle immunity encoding, target-only logout prevention, atomic active/immune
  rejection, Wilderness/logout/death cleanup and blighted teleport-sack substitution.
- Added cache 233 sequence, spotanim, synth and varbit references. Successful casts award 95 Magic
  XP and splashes 83.6, use the dedicated success/failure presentation paths, and do not deal damage.
- Added eight cache-aware integrations for metadata, restrictions, hit/splash behavior, duration,
  immunity, cleanup and substitution, plus a PvP combat-dispatch regression proving five-tick
  cadence. Focused Tele Block and timing suites pass.
- Left teleport enforcement, production Wilderness-varbit updates, caster-kill cleanup, exact
  warning/expiry messaging and RuneLite-observed presentation explicitly incomplete. The manifest
  now validates 63 groups: 48 discovered, 7 researched, 1 unit-tested and 7 integration-tested.
- Passed repository-wide formatting, manifest validation, unit tests and all 16 cache-aware
  integration suites after the Tele Block batch: 595 Gradle tasks, zero failures.
- Implemented Crumble Undead as an undead-NPC-only standard combat spell using cache parameter
  65434 rather than name matching. Added atomic rejection, 24.5 base XP, a 15 gameplay max hit,
  Salve amulet (ei)'s 18 max, explicit Chaos-gauntlet exclusion, rune/staff substitution and the
  dedicated cache sequences, graphics, projectile and sounds.
- Added eight cache-aware integrations covering metadata, restrictions, hit/splash paths, XP,
  audiovisuals, max-hit modifiers and the exact restricted-autocast weapon matrix. Added a combat
  dispatcher regression proving five-tick cadence. The focused Crumble and timing suites pass.
- The manifest now validates 64 groups: 48 discovered, 7 researched, 1 unit-tested and 8
  integration-tested. Crumble Undead still requires a production undead-NPC inventory and workspace
  RuneLite observation before it can advance beyond integration-tested.
- Passed repository-wide formatting, manifest validation, unit tests and all 16 cache-aware
  integration suites after the Crumble Undead batch: 596 Gradle tasks, zero failures.
- Implemented Magic Dart's live core with its Magic/Slayer and suitable-staff gates, rune and XP
  flow, current-Magic max-hit formula, ordinary and enchanted hit graphics, green enchanted splash,
  autocast continuation and five-tick cadence.
- Added seven cache-aware integrations for metadata, atomic failures, all manual staff substitutes,
  hit/splash presentation and the exact autocast matrix. Expanded formula boundaries for ordinary,
  enchanted off-task and enchanted on-task max hits; focused behavior, timing and formula tests pass.
- Kept production Slayer-task resolution, Slayer's staff (e) charge lifecycle, Turoth/Kurask
  restrictions and client-observed details explicitly incomplete. Pinned Iban Blast and its two
  staff revisions for the next implementation batch.
- The manifest now validates 67 groups: 48 discovered, 9 researched, 1 unit-tested and 9
  integration-tested. Passed repository-wide formatting, manifest validation, unit tests and all
  16 cache-aware integration suites after the Magic Dart batch: 596 Gradle tasks, zero failures.
- Implemented Iban Blast's combat core with a shared player charge repository backed by cache varp
  162, atomic staff/rune/charge validation, one-charge NPC and two-charge PvP casts, fixed 25 max
  hit, base and damage XP, hit/splash paths, dedicated cache graphics, autocast continuation and
  five-tick cadence.
- Added nine cache-aware integrations covering metadata, atomic failures, PvM/PvP charge costs,
  shared charges across regular/upgraded staves, hit/splash presentation, Tome of Fire substitution
  without a damage boost, and the exact autocast matrix. Added an independent combat-dispatcher
  cadence regression; both focused suites pass.
- Kept quest access, staff acquisition/upgrading/recharge/check operations, warnings, exact
  charge-failure text, audio and client-observed timing explicitly incomplete.
- The manifest now validates 68 groups: 48 discovered, 9 researched, 1 unit-tested and 10
  integration-tested.
- Passed repository-wide formatting, manifest validation, unit tests and all 16 cache-aware
  integration suites after the Iban Blast batch: 595 Gradle tasks, zero failures.
- Implemented Saradomin Strike, Claws of Guthix and Flames of Zamorak with atomic staff/rune gates,
  hit and splash paths, 35 base Magic XP, base max hit 20, matching-cape Charge max hit 30, player
  Prayer drain, non-stacking Defence/Magic drains, dedicated cache graphics, autocast continuation
  and five-tick cadence. Matching cape support covers normal, imbued, locked/trouver and god max
  cape variants.
- Implemented Charge's self-cast core with its level/rune contract, 180 Magic XP, casting sequence,
  RuneLite-compatible seven-minute varp countdown and one-minute cooldown. Added eleven cache-aware
  god/Charge integrations plus an independent combat-dispatcher cadence regression; focused suites
  pass.
- Kept exact Mage Arena varp packing, staff claims, 100-cast unlocks and region enforcement, Staff
  of the dead rune saving, exact messages, audio, logout persistence and client-observed timing
  explicitly incomplete. Nonzero `magearena` varp 267 is a temporary documented claim proxy.
- The manifest now validates 71 groups: 48 discovered, 10 researched, 1 unit-tested and 12
  integration-tested. Passed repository-wide formatting, manifest validation, unit tests and all
  16 cache-aware integration suites after the god-spell/Charge batch: 595 Gradle tasks, zero
  failures.
- Implemented Low and High Level Alchemy as a dedicated utility-spell plugin with atomic inventory
  preflight, cache-value conversion, rune and staff substitution, one-item stack semantics,
  same-stack target-rune reservation, non-alchable rejection, Fountain of Rune behavior, XP,
  audiovisuals and independent three/five-tick rate limits.
- Added eight cache-aware integrations covering both metadata contracts and the implemented success,
  failure and timing paths. Kept Explorer's rings, activity restrictions, the Alchemist's
  Playground exception, queueing, warnings, animation overrides, exact messages and client timing
  explicitly incomplete.
- The manifest now validates 73 groups: 48 discovered, 11 researched, 1 unit-tested and 13
  integration-tested. The focused utility-spell suite passes.
- Passed repository-wide formatting, manifest validation, unit tests and all 17 cache-aware
  integration suites after the alchemy batch: 601 Gradle tasks, zero failures.
- Implemented Bones to Bananas and Bones to Peaches with their cache requirements, inventory-wide
  cutoff bone allowlist, Graveyard 1–4 fruit yields, Peaches unlock, capacity handling, XP,
  audiovisuals and shared one-tick utility cooldown. Added six cache-aware behavior tests plus the
  two-spell cache contract to the utility suite; all 14 utility integrations pass.
- Kept the exact unlock text, Tombs restriction, overflow ordering, diary credit, tablet variants,
  Mage Training Arena purchase flow and client timing explicitly incomplete. The manifest now
  validates 75 groups: 48 discovered, 12 researched, 1 unit-tested and 14 integration-tested.
- Passed repository-wide formatting, manifest validation, unit tests and all 17 cache-aware
  integration suites after the bone-conversion batch: 601 Gradle tasks, zero failures.
- Implemented Superheat Item with an atomic three-tick utility path and one-deep queued cast. Added
  a reusable Smithing smelting registry for the nine ordinary pre-Sailing bars plus cutoff blurite
  and lunar recipes, including their cache quest-state gates. Added iron/steel selection,
  catalyst, loose-coal-before-bag, gold XP equipment, Fountain, exact documented target failures
  and the level-30 unboosted Cooking jogre-bone exception.
- Added cache references for the spell presentation, ores, bars and quest vars, plus 16 focused
  Superheat integrations covering cache contracts, all recipes, XP, materials, gates, failure
  atomicity, substitutions, equipment, bag/catalyst rules, queueing and jogre bones. The complete
  utility suite now passes with 30 tests.
- Kept exact non-success text, inferred failure presentation, perfect-gold exclusion, interruption,
  queue replacement and RuneLite observation explicitly incomplete. The manifest now validates 77
  groups: 48 discovered, 13 researched, 1 unit-tested and 15 integration-tested.
- Passed repository-wide formatting, manifest validation, unit tests and all 17 cache-aware
  integration suites after the Superheat batch: 605 Gradle tasks, zero failures.
- Added the missing interface-on-ground-item protocol path: packet validation, typed/default/content
  script events, interaction routing, per-component approach range and optional line-of-sight.
  Existing ground-item operations retain their original one-tile line-of-sight behavior.
- Implemented Telekinetic Grab with the cache level/rune/XP contract, whole-pile pickup, rune/staff
  substitutions, Fountain behavior, ten-tile obstruction-free approach, identity-safe two-stage
  pickup, plane-change fizzle, three-tick cooldown and one-deep queued cast. Added all 55 named and
  equipable-diary blocked target identities.
- Added 12 cache-aware integrations covering the spell contract, success and delayed impact, staff
  and Fountain paths, full inventory, stale targets, plane changes, every blocked target, queueing,
  targeting policy, full target-event dispatch and fixed near/far projectile values. The focused
  suite passes with zero failures.
- Kept projectile values 35/45 and two-tick impact explicitly inferred. Exact failure text,
  free-world member-item behavior, special-area overrides, interruption, queued-target replacement
  and matching-client observation remain incomplete. The manifest now validates 79 groups: 48
  discovered, 14 researched, 1 unit-tested and 16 integration-tested.
- Passed repository-wide formatting, manifest validation, unit tests and all 17 cache-aware
  integration suites after the Telekinetic Grab batch: 605 Gradle tasks, zero failures.
- Implemented all seven Standard jewellery-enchantment spells through the cache submenu's inventory-
  target events. Added an explicit registry path for submenu child spells, all 44 cutoff
  transformations, first-matching-source selection, the four F2P amulet exceptions, Digsite and
  nature-amulet gates, atomic rune/item/XP flow, Fountain behavior, cache presentation, a one-deep
  three-tick manual queue and seven-tick same-source automatic repeat with movement/combat
  interruption.
- Added 13 cache-aware integrations covering every spell contract and transformation, source-slot
  selection, membership and special gates, atomic failures, manual/automatic timing, both tested
  interruption paths, Fountain behavior and real interface-event dispatch. The focused suite passes
  with zero failures.
- Kept Mage Training Arena shapes, exact messages, the remaining interruption matrix and matching-
  client presentation explicitly incomplete. Level-7 graphic/zenyte sounds, semi-precious sound
  reuse, graphic height and the Digsite varbit interpretation are documented inference boundaries.
- The manifest now validates 81 groups: 48 discovered, 15 researched, 1 unit-tested and 17
  integration-tested. The complete utility suite passes with 55 tests.
- Passed repository-wide formatting, manifest validation, unit tests and all 17 cache-aware
  integration suites after the jewellery-enchantment batch: 605 Gradle tasks, zero failures.
- Implemented Charge Water, Earth, Fire and Air Orb as data-driven location-target utility spells.
  Added exact matching-obelisk identity, same-slot unpowered-orb replacement, members/level/rune
  gates, staff and Fountain substitution, cache audiovisual selection, a one-deep three-tick manual
  queue and six-tick automatic continuation with movement, combat, source and location interruption.
- Added 11 cache-aware integrations covering all four contracts and transformations, full-inventory
  replacement, atomic failures, staff/Fountain behavior, manual and automatic timing, interruption,
  removed-obelisk safety and real interface-on-location event dispatch. The focused suite passes.
- Corrected the existing Charge Water Orb cache-editor XP metadata from 56 to its historical value
  of 66; the new contract test prevents regression.
- Kept Achievement Diary hooks, exact failure messages, cross-utility queue behavior, the complete
  interruption matrix and matching-client presentation explicitly incomplete. The manifest now
  validates 83 groups: 48 discovered, 16 researched, 1 unit-tested and 18 integration-tested. The
  complete utility suite passes with 66 tests.
- Passed repository-wide formatting, manifest validation, unit tests and all 17 cache-aware
  integration suites after the orb-charging batch: 605 Gradle tasks, zero failures. A clean runtime
  smoke check loaded all 128 scripts, bound port 43594 and reported revision 233 before shutdown.
- Implemented Monster Inspect as a free-to-play NPC-target Standard utility spell. Added the level
  42, two-body/two-mind and 30.5-XP contract; shared rune/Fountain handling; typed component,
  interface, sequence, spotanim and synth references; the cache clientscript's five-section payload;
  current combat stats; represented offensive, defensive and other monster attributes; and the
  standard ten-tile interaction path.
- Added six cache-aware integrations covering exact spell and presentation identities, free-world
  success, current-stat output, four-tab combat/attribute formatting, zero attack-bonus omission,
  atomic level/rune failures, Fountain behavior and real interface-on-NPC dispatch. The complete
  utility suite passes with 72 tests.
- Corrected NPC enrichment so configured freeze resistance reaches the packed NPC parameter, and
  configured all four Phantom Muspah combat forms at 33%. Kept production-wide resistance values
  and binding-formula application explicitly incomplete.
- Kept exact client text/order/omission behavior, burn and vampyric tiers, spectral status, complete
  Slayer classification, production-wide NPC attribute values and morph/client timing explicitly
  incomplete. The manifest now validates 85 groups: 48 discovered, 17 researched, 1 unit-tested
  and 19 integration-tested.
- Passed repository-wide formatting, unit tests and all 17 cache-aware integration suites after the
  Monster Inspect batch: 604 Gradle tasks, zero failures, with parity validation passing separately.
  A clean runtime smoke check loaded all 129 scripts, bound port 43594 and reported revision 233
  before shutdown.
- Implemented the fixed-destination core for Varrock, Lumbridge, Falador and Camelot Teleport.
  Added their spellbook handlers, shared membership/Wilderness/Tele Block gates, rune and staff
  substitution, Fountain behavior, Magic XP, cache destinations, player sequence 714, spotanim 111,
  synth 200 and four-cycle movement. Corrected Falador Teleport's editor XP from 47 to the cutoff's
  48.
- Added seven cache-aware integrations covering all four cache contracts and base destinations,
  free-world availability, exact rune and XP use, members-only Camelot, atomic level/rune/Tele
  Block/deep-Wilderness rejection, the level-20 boundary, staff and Fountain paths, movement timing,
  presentation identities and real spellbook-button dispatch. The complete utility suite passes
  with 79 tests.
- Kept landing-area randomisation, Varrock Grand Exchange/Camelot Seers' Village diary toggles,
  production Wilderness state, exact messages/check ordering, interruption/locking and matching-
  client audiovisual timing explicitly incomplete. The manifest now validates 87 groups: 48
  discovered, 18 researched, 1 unit-tested and 20 integration-tested.
- Passed repository-wide formatting, unit tests and all 17 cache-aware integration suites after the
  basic self-teleport batch: 604 Gradle tasks, zero failures. Parity validation passes separately.
  A clean runtime smoke check loaded all 130 scripts, bound port 43594 and reported revision 233
  before shutdown.
- Extended the shared Standard self-teleport handler to Kourend Castle, Ardougne, Civitas illa
  Fortis, Watchtower, Trollheim and Ape Atoll. Added all six cache spellbook buttons, members gates
  and exact cache-233 quest-state boundaries, including post-quest scroll-learning states for
  Ardougne and Watchtower. Corrected the malformed Watchtower destination to 2546,3114.
- Corrected the spell registry's Ape Atoll cache quirk so the normal banana's legacy wear position
  does not turn it into an equipment requirement. One unnoted banana now participates in the same
  atomic requirement transaction as two fire, two water and two law runes.
- Expanded `StandardTeleportSpellsTest` to cover all ten fixed teleport contracts, every six-spell
  unlock boundary, free-world rejection, exact item/rune consumption, missing-banana atomicity and
  a quest-spell interface dispatch. The complete utility suite passes with 83 tests. Landing-area
  randomisation, all three diary destination toggles, exact client messages/order,
  interruption/locking, production Wilderness state and matching-client timing remain open. The
  manifest now validates 88 groups: 48 discovered, 18 researched, 1 unit-tested and 21
  integration-tested.
- Passed repository-wide formatting, unit tests and all 17 cache-aware integration suites after the
  quest self-teleport batch: 604 Gradle tasks, zero failures. Parity validation passes separately at
  88 groups. A clean runtime smoke check loaded all 130 scripts, bound port 43594 and reported
  revision 233 before shutdown.
- Implemented cache-233 Lumbridge Home Teleport as a free, F2P, no-XP 22-tick channel with the five
  captured default animation/graphic/sound phases, Lumbridge movement, active Tele Block and
  level-20 Wilderness gates, and the ten-second post-combat restriction. Normal movement,
  replacement actions and incoming combat interrupt without consuming cooldown.
- Added a reusable player-action interruption event and tokenized Home Teleport channel repository.
  Added permanent epoch-minute cooldown varp 892, live date-minute synchronization through varp
  3078, exact 30-minute/one-minute remaining messages, and cooldown consumption only after the
  successful destination jump. The state is ready to be shared by future Ancient, Lunar and
  Arceuus Home Teleports.
- Added seven cache-aware integrations covering identities, persistent varp scopes, exact tick-22
  movement, F2P interface dispatch, cooldown boundaries, movement/action/combat interruption and
  Tele Block/Wilderness atomicity. The complete utility suite passes with 90 tests. Animation
  overrides, the full action matrix and workspace RuneLite observation remain open. The manifest
  now validates 89 groups: 48 discovered, 18 researched, 1 unit-tested and 22 integration-tested.
- Passed repository-wide formatting, unit tests and all 17 cache-aware integration suites after the
  Lumbridge Home Teleport batch with zero failures. A clean runtime smoke check loaded all 131
  scripts, bound port 43594 and reported revision 233 before shutdown. RuneLite was not opened.
- Generalized the Home Teleport channel across cache components 218:103/104/148. Ancient Magicks
  now lands at the cutoff Edgeville map area and remains usable on F2P as the dated page specifies;
  Lunar and Arceuus require members worlds and land at Lunar Isle and the Dark Altar. All four
  variants use the same tested presentation, interruption rules and permanent cooldown stamp.
- Expanded the focused Home Teleport suite to ten tests, including all four component identities,
  exact base destination coordinates, the historical membership split and cross-spellbook cooldown
  sharing. Landing-area randomisation, Lumbridge-area exceptions and animation overrides remain
  explicit gaps. The manifest now validates 90 groups: 48 discovered, 18 researched, 1 unit-tested
  and 23 integration-tested.
- Passed repository-wide formatting, unit tests and all 17 cache-aware integration suites after the
  four-spellbook Home Teleport batch: 345 Gradle tasks, zero failures. The complete spell-utility
  suite passes 93 tests, and parity validation passes at 90 groups. A clean runtime smoke check
  loaded all 131 scripts, bound port 43594 and reported revision 233 before shutdown. RuneLite was
  not opened.
- Implemented the three cache-233 diary-configurable Standard teleport destinations. Varrock now
  selects Grand Exchange only with medium Varrock completion plus its saved toggle; Camelot selects
  Seers' Village bank only with hard Kandarin completion plus its toggle; and Watchtower selects
  Yanille only with hard Ardougne completion plus its toggle. Base destinations remain unchanged
  when either condition is missing.
- Expanded `StandardTeleportSpellsTest` to 14 tests with exact IDs for all six cache varbits,
  complete base/alternate branch coverage and a guard proving configuration operations cannot cast
  or consume resources. Seers' Village currently uses one valid square from the cutoff polygon;
  configuration-menu flows and landing-area randomisation remain open. The manifest now validates
  91 groups: 48 discovered, 18 researched, 1 unit-tested and 24 integration-tested.
- Passed repository-wide formatting, unit tests and all 17 cache-aware integration suites after the
  alternate-destination batch: 345 Gradle tasks, zero failures. The complete spell-utility suite
  passes 96 tests. A clean runtime smoke check loaded all 131 scripts, bound port 43594 and reported
  revision 233 before shutdown. RuneLite was not opened.
- Implemented Teleother Lumbridge, Falador and Camelot through the player-target spell path. The
  recipient must have Accept Aid enabled and be available; the caster pays the exact cache rune/XP
  contract, receives the cache casting presentation and is rate-limited to ten ticks. The recipient
  receives interface 326 with the caster and destination names and responds through its native
  pause-button protocol.
- Accepted offers recheck Tele Block and the level-20 Wilderness ceiling, play cache impact sequence
  1816 and spotanim 342, wait the sequence duration, interrupt the recipient and select a Wiki-map
  landing tile. The irregular Lumbridge area is fixed to the 16 tiles enclosed by the pinned
  south-west-corner polygon; Falador and Camelot use their radius-two squares. Decline, close and
  logout paths clear offers. Exact official rejection text/resource timing and matching-client
  observation remain explicit gaps.
- Added ten focused cache-aware Teleother tests covering all three contracts, cache components and
  audiovisual identities, native button metadata, exact costs/XP, polygon boundaries, acceptance
  timing, decline/close behavior, Accept Aid, atomic failures, Tele Block/Wilderness enforcement,
  ten-tick rate limiting and real player-target event dispatch. RuneLite remained closed.
- Passed repository-wide formatting, unit tests and all 17 cache-aware integration suites after the
  Teleother batch: 604 Gradle tasks, zero failures. The complete spell-utility suite passes 106
  tests, and parity validation passes at 92 groups: 48 discovered, 18 researched, 1 unit-tested and
  25 integration-tested. A clean runtime smoke check loaded all 132 scripts, bound port 43594 and
  reported revision 233 before shutdown. RuneLite was not opened.
- Implemented the cache-233 Teleport to Target spell core and target teleport scroll unlock. The
  spell honors the all-spellbook level-85, law/chaos/death-rune, 45-XP and four-tick contract;
  revalidates the live assigned target before arrival; lands within 12 tiles; and synchronizes the
  five-use quota and limit varbits. The scroll follows the pinned confirmation transcript and is
  consumed only after an affirmative choice.
- Added an explicit Bounty Hunter target/location repository for future minigame ownership. Lobby
  casts are allowed, crater casts require 20 cycles out of combat, assignments reset quota, and
  logout clears stale locations and reverse target references. Matchmaking and authoritative area
  transitions remain separate researched work rather than guessed coordinate rectangles.
- Corrected the shared rune manager so a null cache spellbook means an all-spellbook spell instead
  of an impossible spellbook match. Nine focused cache-aware tests cover cache IDs and bit layouts,
  scroll accept/decline/already-unlocked behavior, exact costs and XP, cast presentation and timing,
  landing distance, five-use exhaustion, combat gating, atomic failures, target replacement,
  spellbook dispatch and logout cleanup. RuneLite remained closed.
- Passed repository-wide formatting, root unit tests and all 17 cache-aware integration suites after
  the Teleport to Target batch: 345 Gradle tasks, zero failures. The complete spell-utility suite
  passes 115 tests, and parity validation passes at 94 groups: 48 discovered, 19 researched, 1
  unit-tested and 26 integration-tested. A clean runtime smoke check loaded all 133 scripts, bound
  port 43594 and reported revision 233 before shutdown. RuneLite was not opened.
- Implemented the cache-233 Teleport to House core. The members-only level-40 spell consumes one
  air, earth and law rune, grants 30 Magic XP, uses the standard four-tick presentation and enforces
  Tele Block plus the level-20 Wilderness ceiling. House-location varbit 2187 routes outside casts
  to open tiles beside all nine cutoff portals, including Aldarin.
- Disassembled cache clientscript 2615 to pin the component contract: operation 1 enters inside,
  operations 2/3 route outside as the client changes its default Cast operation, and operations 4/5
  are Group choose/previous. Added a POH-entry repository so the future dynamic-house system can
  publish valid regular/build-mode entrance tiles without the spell fabricating an unloaded
  instance. Missing instances and unavailable Group routes reject before costs; logout clears stale
  entries.
- Added seven focused cache-aware tests covering spell/component/varbit identities, all nine outside
  locations, regular and build-mode entry, all five operations, exact costs/XP/presentation,
  membership/level/rune/Tele Block/Wilderness failures and logout cleanup. RuneLite remained closed.
- Passed repository-wide formatting, root unit tests and all 17 cache-aware integration suites after
  the Teleport to House batch: 605 Gradle tasks, zero failures. The complete spell-utility suite
  passes 122 tests, and parity validation passes at 96 groups: 48 discovered, 20 researched, 1
  unit-tested and 27 integration-tested. A clean runtime smoke check loaded all 134 scripts, bound
  port 43594 and reported revision 233 before shutdown. RuneLite was not opened.
- Implemented all ten cache-233 Enchant Crossbow Bolt child spells for both regular and dragon bolt
  families. Each cast transforms up to ten bolts for a full rune cost and XP award, supports normal
  rune substitution and Fountain behavior, preserves unrelated action queues, permits one-tick
  manual recasts and repeats a selected quantity every three ticks.
- Added the cache-native ten-choice `skillmulti` protocol, including validated pause-button input,
  regular/dragon product-icon modes and quantity bounds. Cache object/component/interface,
  sequence, spotanim and synth references are covered along with exact levels, costs, XP and the
  success message. Mixed regular/dragon priority and maximum derivation remain explicit capture
  gaps. Eleven focused cache-aware tests pass. RuneLite remained closed.
- The manifest now validates 98 groups: 48 discovered, 21 researched, 1 unit-tested and 28
  integration-tested.
- Passed repository-wide formatting, root unit tests and all 17 cache-aware integration suites after
  the crossbow-bolt-enchantment batch: 605 Gradle tasks, zero failures. The complete spell-utility
  suite passes 133 tests. An isolated runtime smoke check loaded all 135 scripts, bound an ephemeral
  port and reported revision 233 before shutdown; the user's already-running server was untouched.
  RuneLite was not opened.
- Implemented cache-233 ordinary furnace smelting through the existing Smithing plugin structure.
  All 28 exact furnace loc-operation pairs route to the cache-native eight-choice skill-multi menu
  or the intended ore-target special recipe. The shared registry now drives bronze, iron, silver,
  steel, gold, mithril, adamantite, runite, lovakite, blurite and lunar bars with their cutoff
  materials, levels, membership and quest gates, XP and first/repeat timing.
- Added atomic batch processing, 50% iron failure, the shared persistent 140-ore ring-of-forging
  lifecycle and Check/Break operations, Smithing catalyst and loose-coal-before-bag ordering,
  goldsmith/Smithing/max-cape gold XP, tiered Varrock-armour double bars at Edgeville, Shilo's
  per-bar fee and elite diary exemption, Lovakite furnace enforcement and movement interruption.
  Eleven focused cache-aware integration tests cover the cache interface/clientscript/loc contract
  and those gameplay rules. RuneLite remained closed.
- The manifest now validates 100 groups: 48 discovered, 22 researched, 1 unit-tested and 29
  integration-tested. Repository-wide formatting, root unit tests and all 18 cache-aware integration
  suites passed after the furnace batch: 347 Gradle tasks, zero failures. The complete spell-utility
  suite remains at 133 tests. An isolated runtime smoke check loaded all 136 scripts, bound port 0
  and reported revision 233 before shutdown; the user's already-running server was untouched.
  RuneLite was not opened.
- Implemented the cache-233 steel cannonball Smithing core in the existing furnace plugin. Regular
  and double ammo moulds now produce four/eight cannonballs from one/two steel bars for 25.6/51.2
  Smithing XP, with members, level 35 and Dwarf Cannon mould-progress gates. Steel-bar selection
  prefers the double mould when both are held and two bars are available; directly using a mould
  selects that recipe.
- Added the cache-native one-choice quantity menu, sequence 827, exact four/eight cooling and mould
  removal messages, eight-tick initial and ten-tick repeat cadence, atomic transactions, Shilo
  fee/elite handling, Lovakite rejection, ordinary/cannonball queue isolation and movement/material
  interruption. Six new focused tests bring the Smithing-smelting suite to 17 tests. Morytania diary
  credit, exact failure/menu ordering, the double-mould Shilo fee interpretation and matching-client
  presentation remain explicitly researched gaps. RuneLite remained closed.
- The manifest now validates 102 groups: 48 discovered, 23 researched, 1 unit-tested and 30
  integration-tested. Repository-wide root unit tests and all 18 cache-aware integration suites
  passed after the cannonball batch: 348 Gradle tasks, zero failures. The complete spell-utility
  suite remains at 133 tests. An isolated runtime smoke check loaded all 136 scripts, bound port 0
  and reported revision 233 before shutdown; the user's already-running server was untouched.
  RuneLite was not opened.
- Implemented the cache-233 ordinary gold-jewellery Crafting core as a dedicated plugin while
  reusing the established furnace loc, policy and queue structures. All 32 gold-through-zenyte
  rings, necklaces, unstrung amulets and bracelets now use their exact mould, optional cut gem,
  gold bar, Crafting level, XP and membership contract from the pinned pre-cutoff Crafting page.
- Added cache-native interface 446 selection with all 32 ordinary product components and a guarded
  1–28 quantity payload, three-tick atomic batching, sequence 899, Shilo fee/elite handling,
  Lovakite rejection, movement/material interruption and shared cancellation across ordinary
  smelting, cannonballs and gold jewellery. Seven cache-aware tests exercise the complete recipe
  table and representative gameplay boundaries. Slayer/eternal rings, remembered selection,
  exact client payload/messages, diary credit and matching-client presentation remain explicitly
  researched gaps. RuneLite remained closed.
- The manifest now validates 104 groups: 48 discovered, 24 researched, 1 unit-tested and 31
  integration-tested. Repository-wide root unit tests and all 19 cache-aware integration suites
  passed after the gold-jewellery batch: 351 Gradle tasks, zero failures. An isolated runtime smoke
  check loaded all 137 scripts, bound port 0 and reported revision 233 before shutdown; the user's
  already-running server was untouched. RuneLite was not opened.
- Implemented the complete cache-233 silver-jewellery Crafting production family in the dedicated
  jewellery plugin. The 17 ordinary recipes cover all opal, jade and red-topaz pieces plus symbols,
  sickles, ten unfinished bolts and tiaras. The three special components now produce the Creature
  of Fenkenstrain conductor, In Aid of the Myreque Silvthrill rod and Shadow of the Storm demonic
  sigil with exact moulds, levels, XP, membership and material contracts.
- Cache clientscript 1596 independently pins all 20 product/level/mould triples and contains none of
  the three quest-state varbits. Historical Quest Helper source at commit
  `633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a` confirms production is mould-possession driven,
  including replacement demonic sigils. Atomic multi-material batches preserve moulds, stop when a
  material disappears, share furnace cancellation and retain the established Shilo/Lovakite and
  movement policies. Eight silver tests bring the jewellery suite to 15 tests.
- The manifest now validates 107 groups: 48 discovered, 25 researched, 1 unit-tested and 33
  integration-tested. Repository-wide root unit tests and all 19 cache-aware integration suites
  passed after the silver-jewellery batch: 351 Gradle tasks, zero failures. An isolated runtime
  smoke check loaded all 138 scripts, bound port 0 and reported revision 233 before shutdown; the
  user's already-running server was untouched. RuneLite was not opened.

- Implemented the complete cache-233 gem-cutting family in the established Crafting jewellery
  plugin. All ten opal-through-zenyte transformations now have exact item, level, XP, membership
  and cache-animation contracts. Opal, jade and red topaz use the Wiki's exact level-scaled
  1/256 success curves, create crushed gemstone on failure and award 3.8/5/6.3 failure XP;
  sapphire and rarer gems are guaranteed. Zenyte uses the cache-era post-November-2024 value of
  50 XP. Batches process atomically every two ticks and stop when the chisel or input disappears.
- Implemented all 15 cache-era manual wool-stringing transforms: eleven ordinary metal/gem
  amulets, unblessed and unpowered symbols, M'speak and Salve. Each consumes one unstrung item and
  one ball of wool every two ticks with proper membership boundaries. The first 14 award 4 XP;
  manual Salve stringing preserves its documented zero-XP exception. Nine new integration tests
  cover both families and both item-use orders, bringing the jewellery suite to 24 tests.
- The manifest now validates 110 groups: 48 discovered, 26 researched, 1 unit-tested and 35
  integration-tested. Focused Crafting compilation and all 24 jewellery integration tests pass.
  Repository-wide root unit tests and all 19 cache-aware integration suites also passed: 351
  Gradle tasks, zero failures. An isolated runtime smoke check loaded all 140 scripts, bound port 0
  and reported revision 233 before shutdown; RuneLite remained closed and the user's running server
  was untouched.
- Implemented the cache-233 ordinary glassblowing family in a dedicated Crafting plugin and moved
  hand-production queue identities into a small shared Crafting module. All eight beer-glass through
  empty-light-orb recipes use their exact cache objects, levels, XP, members/tool/material gates,
  generic eight-choice quantity menu, sequence 884 and three-tick atomic batches. Either item-use
  order starts selection, missing inputs stop safely, and glassblowing now mutually replaces gem
  cutting and manual stringing.
- Seven cache-aware integration tests pin the full recipe table, object/sequence identities,
  interface 270/clientscript 2046 payload and events, exact cadence, inventory/XP transformations,
  failure atomicity, both-order held-item dispatch and cross-Crafting queue cancellation. The
  manifest now validates 112 groups: 48 discovered, 27 researched, 1 unit-tested and 36
  integration-tested. Focused compilation, formatting and the glassmaking suite pass. Repository-
  wide root tests and all 20 cache-aware integration suites also passed: 357 Gradle tasks, zero
  failures. An isolated runtime smoke check loaded all 141 scripts, bound port 0 and reported
  revision 233 before shutdown; RuneLite remained closed and the user's running server was
  untouched.
- Extended the glassmaking plugin through the remaining ordinary production chain. Soda ash and a
  bucket of sand can now be used on every shared furnace identity to open the cache-native
  one-product quantity menu and produce molten glass every two ticks, returning the empty bucket
  and awarding 20 Crafting XP. The members/level/material gates, movement stop, atomic transaction
  and mutual furnace/hand-Crafting queue policy are enforced. Empty oil lamps plus frames now make
  empty oil lanterns at level 26 for 50 XP, while empty light orbs plus cave goblin wire make light
  orbs immediately at level 87 for 104 XP; both item orders work.
- Four additional cache-aware tests bring the glassmaking suite to 11 and pin the ten extended
  object identities, interface 270/clientscript 2046 payload, sequence 899, two-tick cadence,
  returned buckets, material/movement stops, queue cancellation, both-order assembly, exact XP and
  level failure atomicity. The manifest now validates 113 groups: 48 discovered, 27 researched, 1
  unit-tested and 37 integration-tested. Focused formatting, compilation and all 11 tests pass.
  Repository-wide root tests and all 20 cache-aware integration suites also passed: 357 Gradle
  tasks, zero failures. An isolated runtime smoke check loaded all 141 scripts, bound port 0 and
  reported revision 233 before shutdown; RuneLite remained closed and the user's running server
  was untouched.
- Added a dedicated cache-233 spinning plugin for the complete pre-cutoff five-product family:
  free-to-play wool to ball of wool, members' flax to bow string, sinew or all five ordinary root
  tiers to crossbow string, magic roots to magic string, and yak hair to rope. It registers eleven
  named wheel identities plus every material-on-wheel path, uses the generic five-choice quantity
  menu, enforces exact levels/XP/membership, preserves the selected raw material, performs atomic
  transforms every three ticks with sequence 894, stops on movement/material loss and shares the
  Crafting cancellation policy.
- Six cache-aware spinning tests pin recipe/input/output identities, interface 270/clientscript 2046
  payload and events, every root alternative, exact XP, cadence, movement stops and atomic failure
  gates. The manifest now validates 115 groups: 48 discovered, 28 researched, 1 unit-tested and 38
  integration-tested. Focused formatting, compilation and all six spinning tests pass. Repository-
  wide root tests and all 21 cache-aware integration suites also passed: 361 Gradle tasks, zero
  failures. An isolated runtime smoke check loaded 27 plugin modules and 142 scripts, bound port 0
  and reported revision 233 before shutdown; RuneLite remained closed and the user's running server
  was untouched.
- Added a dedicated cache-233 pottery plugin for the complete six-product family. Potter's wheels
  shape pot, four cups, pie dish, bowl, plant pot and pot lid with exact cache objects, levels, XP,
  free/member boundaries, native six-choice selection, sequence 883 and three-tick atomic batches.
  Pot-lid production requires One Small Favour varp 416 state 235, matching the state immediately
  after Tassie teaches the recipe. Cup production preflights its net inventory expansion.
- All four ordinary oven identities fire the six unfired products with sequence 1317 and seven-tick
  batches. There is no item-specific firing level gate; each action uses the Wiki's exact Crafting
  level 1–13 success numerators (181/187/194/200/206/213/219/225/232/238/244/251/256 out of 256),
  consumes cracked pottery without XP, and awards the exact item XP on success. Movement/material
  stops and shared Crafting queue replacement are enforced.
- Eight cache-aware pottery tests pin the full recipe/cache table, native menu payload, exact XP and
  four-cup yield, quest/membership/level/space atomicity, cadence, movement cancellation, no firing
  level gate and every probability boundary. Focused formatting, compilation and all eight tests
  pass. The manifest now validates 117 groups: 48 discovered, 29 researched, 1 unit-tested and 39
  integration-tested. Repository-wide root tests and all 21 cache-aware integration suites also
  passed: 366 Gradle tasks, zero failures. An isolated runtime smoke check loaded 28 plugin modules
  and 143 scripts, bound port 0 and reported revision 233 before shutdown; RuneLite remained closed
  and the user's running server was untouched.
- Added a dedicated ordinary leatherworking plugin for all 20 cache-233 soft, hard and dragon-
  leather armour recipes. Per-material native quantity menus expose seven soft-leather products,
  one hardleather body or the three matching dragonhide products. Every recipe uses exact levels,
  inputs, outputs, XP and membership, sequence 1249 and three-tick atomic batching.
- Ordinary needles require thread and consume one spool on every fifth completed armour action,
  with progress retained across interrupted/restarted batches in the same player session. Cache-
  era costume needle 29920 works as both needle and unlimited thread without advancing ordinary-
  spool progress. Either tool/material item order is accepted, and missing tools, thread, level,
  membership or hides reject before mutation.
- Nine cache-aware leatherworking tests pin the full 20-recipe/cache table, native menu payload,
  every transform and XP value, persistent fifth-action thread boundary, costume-needle behavior,
  atomic gates, cadence, material stop and cross-Crafting queue replacement. Focused formatting,
  compilation and all nine tests pass. The manifest now validates 119 groups: 48 discovered, 30
  researched, 1 unit-tested and 40 integration-tested. Repository-wide root tests and all 22
  cache-aware integration suites also passed: 370 Gradle tasks, zero failures. An isolated runtime
  smoke check loaded 29 plugin modules and 144 scripts, bound port 0 and reported revision 233
  before shutdown; RuneLite remained closed and the user's running server was untouched.
- Added a dedicated tannery service plugin for all eight cache-233 raw-hide transformations. The
  native interface 324 now receives the correct output models, recipe names and singular/plural
  coin prices, and its All, X, 5 and 1 controls perform immediate, atomic full-inventory or
  requested-quantity conversions capped by both hides and affordable coins. Snake and dragon hides
  retain their membership gates; tanning has no Crafting-level requirement.
- Ellis and the Al Kharid Tanner use the standard price schedule, Sbott and Chouani use the premium
  schedule, and Eodan uses his exact base schedule with each completed Kourend diary tier applying
  another exact 20-percent reduction. The Ranging Guild Leatherworker follows the Wiki transcript:
  his Talk-to route can open the standard-price service or explain soft and hard leather, while his
  NPC retains no direct Trade action. Closing or replacing the modal invalidates stale selections.
- Ten cache-aware tannery tests pin the interface/component and NPC-operation contracts, all eight
  transforms, every price schedule and diary tier, membership and affordability gates, atomic
  failures, the Leatherworker dialogue route, actual interface button dispatch and stale-action
  rejection. Focused formatting and all ten tests pass. The manifest now validates 121 groups: 48
  discovered, 31 researched, 1 unit-tested and 41 integration-tested. Repository-wide root tests
  and all 24 cache-aware integration suites also passed: 373 Gradle tasks, zero failures. An
  isolated runtime smoke check loaded 30 plugin modules and 145 scripts, bound port 0 and reported
  revision 233 before shutdown; RuneLite remained closed and the user's running server was
  untouched.
- Added a dedicated elemental battlestaff Crafting plugin for the four cache-233 orb recipes. Water,
  earth, fire and air orbs combine with plain battlestaves at levels 54, 58, 62 and 66 for exact
  outputs and 100, 112.5, 125 and 137.5 XP. All recipes are members-only, use native one-product
  interface 270, sequence 7531 and their distinct elemental spot effects.
- Both held-item orders open the quantity menu. Make-X produces its first staff after three ticks
  and each remainder after two ticks; material loss, movement and another Crafting action stop the
  batch. Membership, boosted level and material checks reject atomically before any input changes.
- Eight cache-aware battlestaff tests pin the recipe/cache table, interface payload and quantity
  range, all four transforms and effects, exact Make-X cadence, atomic failure gates, interruption
  behavior and reverse-order item dispatch. Focused formatting and all eight tests pass. The
  manifest now validates 123 groups: 48 discovered, 32 researched, 1 unit-tested and 42
  integration-tested. Repository-wide root tests and all 25 cache-aware integration suites also
  passed: 377 Gradle tasks, zero failures. An isolated runtime smoke check loaded 31 plugin modules
  and 146 scripts, bound port 0 and reported revision 233 before shutdown; RuneLite remained closed
  and the user's running server was untouched. Specialty battlestaff-derived products, POH
  crystal-ball conversions, exact messages and the exact crafting sound remain tracked open work.
- Added a dedicated loom-weaving Crafting plugin for all four cache-233 products. The native
  four-choice quantity menu exposes strip of cloth, empty sack, drift net and basket in cache order;
  each recipe pins exact inputs, material counts, levels, XP and members-only access across the
  Iorwerth, standard and repaired Fossil Island loom identities using sequence 2270.
- Ordinary cloth, sack and basket batches produce their first result after four ticks and repeat
  every three ticks; drift nets produce both first and repeated results every three ticks. Boosted
  level/material gates, direct material-on-loom entry, atomic many-to-one transforms, movement and
  material stops, and replacement against every other Crafting queue are implemented.
- Seven cache-aware weaving tests pin the recipe/cache/loom/animation table, native menu payload,
  all transforms and XP, both cadence families, atomic failure gates and interruption. Focused
  formatting and all seven tests pass. The manifest now validates 125 groups: 48 discovered, 33
  researched, 1 unit-tested and 43 integration-tested. Repository-wide root tests and all 26
  cache-aware integration suites also passed: 381 Gradle tasks, zero failures. An isolated runtime
  smoke check loaded 32 plugin modules and 147 scripts, bound port 0 and reported revision 233
  before shutdown; RuneLite remained closed and the user's running server was untouched. Exact
  messages/sound/order, direct-input menu filtering, every placement and broader activity/credit
  integration remain tracked open work.
- Added a dedicated leather add-on Crafting plugin for the two studded-armour recipes and all five
  cache-233 spiky-vambrace recipes. Leather body plus steel studs makes studded body at level 41 for
  40 XP, leather chaps plus steel studs makes studded chaps at level 44 for 42 XP, and ordinary,
  green, blue, red or black vambraces plus kebbit claws make the corresponding spiky vambraces at
  level 32 for 6 XP. All seven recipes are members-only, accept either held-item order, use sequence
  1249 and complete after three ticks as a single explicit action without a quantity menu.
- Boosted Crafting level, membership and material gates reject before mutation. Each two-to-one
  inventory transaction remains atomic in a full inventory, and movement, material loss or another
  Crafting queue replacing the action cancels safely. Seven cache-aware tests pin the recipe and
  cache-ID table, exact timing and XP, membership/level/material gates, full-inventory behavior,
  interruption, one-action semantics and actual reverse-order held-item dispatch.
- The dated Crafting overview table lists 5.5 XP for spiky vambraces, while its prose and all five
  recipe-specific pages list 6 XP; the implementation follows the unanimous recipe-specific value
  and records the conflict for production-client verification. Focused formatting and all seven
  tests pass. The manifest now validates 126 groups: 48 discovered, 33 researched, 1 unit-tested
  and 44 integration-tested. Repository-wide root tests and all 27 cache-aware integration suites
  also passed: 385 Gradle tasks, zero failures. An isolated runtime smoke check loaded 33 plugin
  modules and 148 scripts, bound port 0 and reported revision 233 before shutdown; RuneLite remained
  closed and the user's running server was untouched. Exact messages, sound and client ordering,
  plus production capture of the disputed XP and animation, remain tracked open work.
- Added a dedicated leather-shield Crafting plugin for hard leather, snakeskin and all four
  dragonhide shields. The six recipes require the exact two matching leathers, oak-through-redwood
  base shield and 15 bronze-through-rune nails at boosted levels 41, 56, 62, 69, 76 and 83. They
  award 70, 100, 124, 140, 156 and 172 XP, are members-only, accept both leather/base item orders
  and use purpose-specific cache sequences 7386/7387/7831/7832/7833/7834.
- Each use-on produces one atomic result after five ticks with no quantity menu. Normal hammer 2347
  and both Imcando forms 25644/29775 work from inventory or their matching equipment slots, as
  required by their cutoff-era Wiki revisions. Membership, boosted level, tool, two-leather,
  base-shield and exact 15-nail gates all run before mutation; full inventories work, while movement,
  tool/material loss or another Crafting action cancels safely.
- Eight cache-aware shield tests pin every recipe/cache/animation identity, transform and XP value,
  all four hammer configurations, every atomic failure gate, five-tick timing, single-action
  behavior, full-inventory handling, interruption and reverse held-item dispatch. Focused formatting
  and all eight tests pass. The manifest now validates 127 groups: 48 discovered, 33 researched,
  1 unit-tested and 45 integration-tested. Repository-wide root tests and all 28 cache-aware
  integration suites also passed: 389 Gradle tasks, zero failures. An isolated runtime smoke check
  loaded 34 plugin modules and 149 scripts, bound port 0 and reported revision 233 before shutdown;
  RuneLite remained closed and the user's running server was untouched. Exact use-on precedence,
  messages, sound, client ordering and task-credit hooks remain tracked open work.
- Added a dedicated specialist leatherworking Crafting plugin for all five ordinary snakeskin
  pieces and both yak-hide armour pieces. Boots, vambraces, bandana, chaps and body consume
  6/8/5/12/15 snakeskins at boosted levels 45/47/48/51/53 for 30/35/45/50/55 XP. Yak legs and top
  consume one/two cured hides at levels 43/46 for 32 XP. All seven recipes are members-only, use
  sequence 1249, complete each result in three ticks and expose the native interface-270 quantity
  menu for their material.
- Pinned the yak unlock to cache varbit 3311 (`fris_quest`) at state 280. The cutoff-era Wiki marks
  partial Fremennik Isles completion as required, and the pinned pre-cutoff Quest Helper state map
  places armour acquisition at 280 between final-decree dialogue state 275 and shield state 290.
  State 279 therefore rejects atomically while 280 and later states work. Ordinary needles consume
  one whole thread per snakeskin or yak result, exactly as each dated item recipe specifies;
  costume needles supply thread without consuming it. This deliberately differs from ordinary
  cow/dragon leather's one-spool-per-five-results contract.
- Twelve cache-aware tests pin every recipe, cache object, animation and quest-var identity; both
  menu payloads and quantity caps; all transforms, XP and three-tick boundaries; whole-thread
  exhaustion; costume-needle batches; membership/level/tool/material and exact quest gates;
  full-inventory atomicity; movement/tool-loss interruption; cross-Crafting replacement; and actual
  reverse-order held-item dispatch. Focused formatting and all twelve tests pass. The manifest now
  validates 128 groups: 48 discovered, 33 researched, 1 unit-tested and 46 integration-tested.
  Repository-wide root tests and all 29 cache-aware integration suites also passed: 393 Gradle
  tasks, zero failures. An isolated runtime smoke check loaded 35 plugin modules and 150 scripts,
  bound port 0 and reported revision 233 before shutdown; RuneLite remained closed and the user's
  running server was untouched. Exact production menu order, messages, sound and client ordering,
  activity restrictions, credit hooks and the remaining suqah/fur/hydra/quest-textile families
  remain tracked open work.
- Added a dedicated hydra-leather Crafting plugin for the Lithkren Vault Machinery. One hydra
  leather 22983 becomes ferocious gloves 22981 and the gloves reverse back to leather, with no
  Crafting level or XP requirement. Both directions require members, Dragon Slayer II completion
  at cache-derived state 215 and a normal or Imcando hammer, confirm before starting and complete
  as an atomic one-for-one transform after seven ticks.
- Registered the cache's `Investigate` and leather `Inspect` operations plus both item-on-machine
  directions. Exact cutoff-era Inspect, confirmation, result and missing-hammer text comes from the
  dated Machinery transcript. Cache client scripts 4024/4029 and quest DB row 32 column 19 prove
  that varbit 6104 reaches completed status at 215. The action rechecks range, quest, tool and input
  before mutation; full inventories succeed while decline, movement and tool/material loss stop
  safely. Both Imcando forms work from inventory or their matching equipment slots.
- Ten cache-aware tests pin objects, loc, sequences, quest varbit/DB end state, exact transcript
  text, both directions, seven-tick timing, zero XP, gates, hammer forms, full-inventory atomicity,
  interruption and direct loc dispatch. Focused formatting and all ten tests pass. The manifest now
  validates 129 groups: 48 discovered, 33 researched, 1 unit-tested and 47 integration-tested.
  Repository-wide root tests and all 30 cache-aware integration suites passed: 397 Gradle tasks,
  zero failures. An isolated runtime smoke check loaded 36 plugin modules and 151 scripts, bound
  port 0 and reported revision 233/Desktop before shutdown; RuneLite remained closed and the user's
  running server was untouched. Exact production animation/sound/order, Imcando applicability,
  direct-input priority and inferred non-transcript failure text remain tracked open work.
- Added a dedicated Lunar-clothing Crafting plugin for the ceremonial outfit stage of Lunar
  Diplomacy. One Suqah leather 9081 makes the torso, trousers, gloves or boots 9097–9100 at visible
  Crafting levels 61/59/52/52 for 35/30/25/25 XP. The exact cutoff-era five-choice transcript menu,
  all level failures, success text and completed-quest rejection are implemented. Cache quest DB
  row 88 proves completion state 190, while the dated Quest Helper map places the clothing stage at
  155, so production is admitted through states 155–189.
- Each garment is a single three-tick sequence-1249 action with an atomic one-for-one transaction.
  Both held-item orders work, the costume needle supplies unlimited thread, and a shared repository
  now carries ordinary-spool progress across standard and Suqah leather actions so the fifth
  combined result consumes one spool. Full inventory replacement and mid-action tool, material or
  quest-state changes are covered.
- Ten cache-aware tests pin the cache/quest contracts, exact recipe and transcript table, timing,
  XP, visible-level boundaries, both quest edges, tool variants, shared thread state, reverse-order
  dispatch, full-inventory safety and interruption. Focused formatting and all ten tests pass. The
  manifest now tracks 130 groups with 48 integration-tested. Production capture remains open for
  the pre-stage and generic failure text, logout persistence, sound and exact client ordering.
  Repository-wide root tests and all 31 cache-aware integration suites passed: 401 Gradle tasks,
  zero failures. An isolated runtime smoke check loaded 37 plugin modules and 152 scripts, bound
  port 0 and reported revision 233/Desktop before shutdown; RuneLite remained closed and the
  user's running server was untouched.
- Expanded specialist leatherworking with the complete three-piece Xerician robe set. Xerician
  fabric 13383 now makes the hat 13385, robe 13389 and top 13387 at boosted Crafting levels
  14/17/22, consuming 3/4/5 fabric and awarding 66/88/110 XP. All three are members-only, use the
  native interface-270 quantity menu and sequence 1249, consume one whole thread with an ordinary
  needle or no thread with a costume needle, and complete atomically every three ticks.
- The thirteen-test specialist suite now pins all ten specialist recipes, exact Xerician cache
  identities/table/menu order, a three-result costume-needle batch, timing and resource behavior in
  addition to the existing snake/yak gates and interruption coverage. Focused formatting and the
  suite pass. The manifest now tracks 131 groups with 49 integration-tested. Exact Xerician
  production messages, sound and client ordering remain open pending matching-client capture.
  Repository-wide root tests and all 31 cache-aware integration suites passed again: 401 Gradle
  tasks, zero failures. An isolated runtime smoke check loaded 37 plugin modules and 152 scripts,
  bound port 0 and reported revision 233/Desktop before shutdown; RuneLite remained closed and the
  user's running server was untouched.
- Added a dedicated Hunter-pouch Crafting plugin covering all five cache-233 recipes. Four larupia
  or fox furs make the small fur/meat pouches at level 35 for 20/30 XP. Three graahk, kyatt or
  sunlight-antelope furs plus the required closed pouch make medium/large upgrades at levels 50/65
  for 40/60 XP. All recipes are members-only, boostable, atomic, three ticks and use sequence 1249
  plus a native one-product interface-270 quantity menu.
- The Wiki's 0.2-thread recipes share the existing five-action spool repository across ordinary
  leather, Lunar clothing and Hunter pouches; costume needles consume no thread. Quantity caps
  account for partial spool progress, fur and closed base pouches, including the explicit closed
  small-meat-pouch upgrade rule. Nine focused integration tests pin all five recipes, cache/open
  identities, transforms, timing, XP, cross-family thread progress, tool variants, gates,
  full-inventory safety, interruption, queue replacement and reverse item order. The focused suite
  and formatting pass. The manifest now tracks 132 groups with 50 integration-tested. Pouch
  storage operations and exact production menu/messages/audio/client ordering remain open.
  Repository-wide root tests and all 32 cache-aware integration suites passed: 405 Gradle tasks,
  zero failures. An isolated runtime smoke check loaded 38 plugin modules and 153 scripts, bound
  port 0 and reported revision 233/Desktop before shutdown; RuneLite remained closed and the
  user's running server was untouched.
- Added a dedicated Asyff Custom Fur Clothing plugin using cache interface 477, dynamic product
  component 6 and inventory 482. It exposes all 20 cache products in their native three-row order,
  validates the displayed slot/object pair and converts the selected product immediately through
  one atomic inventory transaction, including when all 28 player-inventory slots are occupied.
- Implemented every dated Wiki material and coin fee across the camouflage sets, larupia/graahk/
  kyatt outfits, gloves of silence and both weight-reducing capes. Body and leg recipes prioritise
  tatty fur before perfect fur as changed on 15 May 2024; headwear accepts only perfect fur.
  Asyff's cache-defined Trade and Fur Clothing operations open inventory 63 and the custom panel,
  and the dated standard hunting, unusual-material and non-members dialogue is wired in.
- Nine focused cache-aware integrations pin the recipe table, cache product order, NPC operations,
  native interface/inventory packets, all transforms/fees, tatty priority, perfect-only headwear,
  full-inventory behavior, resource failures, members gating, spoof/stale-action protection and
  direct NPC routes. The manifest now tracks 133 groups with 51 integration-tested. Conditional
  Biohazard/Eagle's Peak dialogue, exact production messages/menu operations/audio/activity rules
  and matching-client packet ordering remain open. Repository-wide root tests and all 33
  cache-aware integration suites passed: 409 Gradle tasks, zero failures. An isolated runtime smoke
  check loaded 39 plugin modules and 154 scripts, bound port 0 and reported revision 233/Desktop
  before shutdown; RuneLite remained closed and the user's running server was untouched.
- Added permanent, family-shared Hunter fur and meat pouch stores with exact 14/21/28 tier limits.
  All ten closed/open pouch objects now support Fill, Check and Open/Close; destroying any pouch
  clears its family's store, and the restored bank-side Empty operation transfers only that family
  into the permanent bank. Multiple pouches do not increase capacity and crafting an upgrade keeps
  the shared contents.
- Pinned the exact cache-233 resource policy: 14 accepted furs and nine accepted raw meats, with
  tatty outfit fur and raw bird meat excluded. A public acquisition hook routes only eligible
  Hunter loot through the largest appropriate open pouch and reports overflow to the caller. The
  expanded fifteen-test suite exercises cache operations, permanent inventory configuration,
  allowlists, every tier boundary, duplicate-pouch sharing, open/close preservation, the real
  bank-side event and the real destroy event. Focused tests and formatting pass. The manifest now
  tracks 134 groups with 52 integration-tested. Ordinary Empty bank/Hunter Guild-area gating,
  deposit-box routing and future Hunter-catch call-site wiring remain open.
  Repository-wide root tests and all 33 cache-aware integration suites passed: 409 Gradle tasks,
  zero failures. An isolated runtime smoke check loaded 39 plugin modules and 154 scripts, bound
  port 0 and reported revision 233/Desktop before shutdown; RuneLite remained closed and the
  user's running server was untouched.
- Added a dedicated standard log-cutting Fletching plugin with all 35 pinned recipes across nine
  log families: arrow shafts, ordinary javelin shafts, every ordinary unstrung shortbow/longbow,
  all eight crossbow stocks, six wooden shields and the cache-233 redwood hiking staff. Exact
  boosted levels, one/two-log inputs, output counts and fractional XP are data-driven and each
  result uses one atomic inventory transaction.
- Both item orders for knife-on-log are registered for the ordinary knife and cache-233 Fletching
  knife. The native interface-270 quantity chooser, sequence 1248, two-tick first result,
  three-tick ordinary repeats and two-tick special-knife repeats are implemented. Initiation still
  requires either knife in inventory; once started, the Fletching knife speed reward applies from
  inventory or the left hand, exactly as the dated item page specifies.
- Ten focused cache-aware integrations pin all 35 recipes, every cache identity and menu registry,
  real reverse held-use and product-selection events, exact transforms/XP, membership/boosted-level/
  tool/two-log failures, both timing paths, knife-removal interruption and queue completion. The
  focused suite and formatting pass. The manifest now tracks 136 groups with 53 integration-tested.
  Exact client menu ordering/presentation, sound/messages/inventory ordering, broader interruption
  and all non-log Fletching families remain open pending matching-client evidence.
  Repository-wide root tests and all 34 cache-aware integration suites passed: 413 Gradle tasks,
  zero failures. An isolated runtime smoke check loaded 40 plugin modules and 155 scripts, bound
  port 0 and reported revision 233/Desktop before shutdown; RuneLite remained closed and the
  user's running server was untouched.
- Added a dedicated standard bow-stringing Fletching plugin covering all 12 normal-through-magic
  shortbows and longbows. Each recipe pins its boosted level, unstrung/output identity, bow-string
  input, historical XP and distinct cache sequence. The three asymmetric historical splits are
  preserved: willow shortbow 33.2 XP, maple longbow 58.2 XP and magic shortbow 83.2 XP.
- Both item orders open a native one-product interface-270 quantity chooser. Results complete
  atomically every two ticks, including from a full inventory; resource loss stops without partial
  mutation, and log cutting plus bow stringing now replace one another through the shared Fletching
  queues. Seven focused integrations pin the full recipe/sequence table, native menu payload,
  every transform/XP value, gates, timing, interruption, capacity and real held-use dispatch.
- Focused bow-stringing and log-cutting suites pass. The manifest now tracks 137 groups with 54
  integration-tested. Bow-string-spool support and other Fletching families remain open, as do
  exact menu/messages/audio/client ordering and matching-client verification. Repository-wide root
  tests and all 35 cache-aware integration suites passed: 417 Gradle tasks, zero failures. An
  isolated runtime smoke check loaded 41 plugin modules and 156 scripts, bound port 0 and reported
  revision 233/Desktop before shutdown; RuneLite remained closed and the user's running server was
  untouched.
- Added the cache-233 bow-string-spool core to the standard bow-stringing plugin. Object 31052 now
  exposes its cached Fill and Check routes, accepts loose and noted bow strings through either
  use-on order, combines additional spools, and supplies every ordinary unstrung-bow recipe through
  the existing native quantity chooser. Charges and size live in cache-backed account varbits, so
  stored strings persist when the physical spool is lost and reacquired.
- The six dated capacity tiers are enforced exactly at 250, 500, 2,000, 5,000, 10,000 and 100,000.
  Spool-backed results preserve each bow's exact sequence, XP and two-tick boundary while consuming
  one account charge only after the atomic bow transform succeeds. Empty spools are inert and all
  spool operations retain the members gate.
- Fourteen focused integrations now cover the ordinary bows and spool together: cache object,
  operation and varbit contracts; loose/noted deposits; capacity clipping; account persistence;
  all combine tiers and the terminal cap; real Fill, Check, combine, deposit and bow-use dispatch;
  production cadence/charge exhaustion; membership; and the prior recipe, menu, XP, interruption,
  capacity and cancellation cases. Focused Fletching tests and cache-233 manifest validation pass.
  The manifest now tracks 138 groups: 48 discovered, 34 researched, 1 unit-tested and 55
  integration-tested. Bank-proximate Empty withdrawal and automatic flax-spinning deposits remain
  explicit researched integrations because the repository does not yet expose authoritative bank-
  area or cross-skill hooks. Repository-wide root tests and all 35 cache-aware integration suites
  passed: 417 Gradle tasks, zero failures. An isolated runtime smoke check loaded 41 plugin modules
  and 156 scripts, bound port 0 and reported revision 233/Desktop before shutdown; RuneLite remained
  closed and the user's running server was untouched.
- Added a dedicated standard ammunition Fletching plugin with 28 cache-pinned contracts: headless
  arrows, nine ordinary/broad arrow recipes, ten ordinary/broad feathered-bolt recipes and all eight
  bronze-through-dragon darts. Exact boosted levels, per-item XP, 15/10-item sets, partial final
  sets and cache-233 object/animation identities are data-driven and atomically transformed.
- Arrows use the native one-product quantity flow and two-tick set cadence. Darts and bolts respect
  cache varbit 10971: rapid mode makes one set immediately, while Make-X queues at most ten sets at
  two ticks each. Broad arrows and bolts require the real Broader Fletching varbit 3208. All three
  Fletching plugins now cancel one another through a shared ammunition queue.
- Eight focused integrations pin every recipe, cache identity and setting; exact transforms/XP;
  immediate and Make-X timing; ten-set menu cap; partial sets; membership, level, materials and
  Slayer unlock; reverse held-use; interruption and cross-Fletching replacement. Focused tests and
  formatting pass. The manifest now tracks 139 groups: 48 discovered, 34 researched, 1 unit-tested
  and 56 integration-tested. Javelins, special ammunition, new-account Make-X initialization and
  exact matching-client presentation/order remain explicit follow-up work.
- Repository-wide root tests and all 36 cache-aware integration suites passed: 421 Gradle tasks,
  zero failures. An isolated runtime smoke check loaded 42 plugin modules and 157 scripts, bound
  port 0 and reported revision 233/Desktop before shutdown; RuneLite remained closed and the
  user's running server was untouched.
- Extended the ammunition plugin with all eight bronze-through-dragon javelins, bringing its
  registry to 36 cache-pinned recipes. Historical revisions 14765341 and 14982925 establish the
  exact levels, per-item XP, 15-item sets, one-tick first result and rapid first two sets; the
  documented 3,000-set-per-hour steady rate establishes two-tick repeats. All eight support partial
  final sets, both item orders, boosted levels, membership, atomic transforms and shared Fletching
  cancellation. No unrelated animation is substituted where cache 233 has no dedicated sequence.
- Two new integrations bring the focused ammunition suite to ten tests and pin every javelin object,
  transform, XP total, 1/1/2-tick cadence, partial set, ten-set native quantity payload and reverse
  use. Focused formatting and cache-aware integration pass. Exact matching-client menu, audio,
  packet ordering and no-animation confirmation remain open; RuneLite stayed closed.
- The manifest now tracks 140 groups: 48 discovered, 34 researched, 1 unit-tested and 57
  integration-tested. Repository-wide root tests and all 36 cache-aware integration suites passed:
  421 Gradle tasks, zero failures. An isolated runtime smoke check loaded 42 plugin modules and 157
  scripts, bound port 0 and reported revision 233/Desktop before shutdown; RuneLite remained closed
  and the user's running server was untouched.
- Added a dedicated standard crossbow-assembly Fletching plugin for all eight bronze-through-dragon
  tiers. The data registry pins every stock, limb, unstrung and finished object; boosted level;
  limb/string XP split; and tier-specific limb and stringing sequence from cache 233 and dated Wiki
  revisions. Both stages use the native one-product quantity flow and produce one atomic result
  every two ticks.
- Limb attachment requires a normal hammer or either Imcando hammer form from inventory/equipment.
  Both item orders, full inventories, membership and level gates, resource/tool loss, exact XP,
  conventional filtered success feedback and shared cancellation with all existing Fletching
  batches are covered. Eight focused integrations pin the full contract, menu payloads, every
  transform, both-stage cadence, hammer variants, failure atomicity and real reverse held-use.
- The focused crossbow suite and formatting pass. The manifest now tracks 141 groups: 48 discovered,
  34 researched, 1 unit-tested and 58 integration-tested. Exact success-message punctuation, sound,
  packet ordering, menu memory, wider interruption and diary/task hooks remain matching-client work;
  RuneLite stayed closed and the user's running server was untouched.
- Repository-wide unit tests and all 37 cache-aware integration suites passed after crossbow
  assembly: 425 Gradle tasks, zero failures. An isolated runtime smoke check loaded 43 plugin
  modules and 158 scripts, bound port 0 and reported revision 233/Desktop before shutdown; RuneLite
  remained closed and the user's running server was untouched.

- Extended the shared ammunition registry with all 22 cache-233 tipped-bolt recipes: twelve
  standard/special variants and all ten supported dragon variants. Historical cutoff tables and
  four representative item pages pin every boosted level, material pairing, ten-item/partial set,
  two-tick action and per-bolt XP contract, including topaz's 3.9/4.0 standard/dragon split and
  Broader Fletching for amethyst broad bolts.
- Added typed references for twelve tip items, two previously-unmapped outputs and all eight
  tier-specific attachment sequences. The revision-matched workspace RuneLite constant resolves
  sequence 8473 as the iron/broad animation, so amethyst broad bolts do not rely on a guessed tier.
  The existing cache varbit drives optional Make-X; disabled use schedules one set, while enabled
  use schedules up to ten two-tick sets through the native quantity interface.
- Three focused integrations expand the ammunition suite to thirteen tests and cover all recipes,
  exact animations/XP, output boundaries, quantity payloads, partial and full-inventory transforms,
  membership/level/unlock failures, resource interruption and shared cancellation. Focused
  formatting and cache-aware integration pass; RuneLite stayed closed and the user's running
  server was untouched.

- The manifest now tracks 142 unique groups: 48 discovered, 34 researched, 1 unit-tested and 59
  integration-tested. Repository-wide formatting and parity validation pass. Root unit tests and
  all 37 cache-aware integration suites passed in 425 Gradle tasks with zero failures. An isolated
  runtime smoke loaded 43 plugin modules and 158 scripts, bound port 0 and reported revision
  233/Desktop before shutdown; RuneLite remained closed and the user's running server was untouched.

- Added a dedicated bolt-tip cutting Fletching plugin for all eleven cache-233 gem and pearl
  recipes. Individually pinned historical pages resolve a stale cutoff-table XP conflict and pin
  both pearl yields plus every boosted level, output count and per-source XP value. Cache-backed
  sequences cover all nine gems and the dedicated pearl animation; no raw production IDs or
  substitute animations are used.
- Every recipe opens the native one-product quantity interface and atomically transforms one source
  every five ticks. Dragonstone alone first uses the exact two-choice value warning and exact
  accept/decline feedback from its pinned transcript. Membership, boosted levels, chisel/source
  validation, full inventories, both item orders, resource interruption and cancellation with all
  existing Fletching batches are implemented. Amethyst is intentionally reserved for its separate
  level-83 Crafting product-selection slice.
- Eight focused cache-aware integrations pin the complete registry, native menu payload, every
  transform and exact XP, five-tick cadence, dragonstone dialogue, failure atomicity, interruption,
  full-inventory replacement and reverse held-use. Focused formatting and integration pass.
- The manifest now tracks 143 unique groups: 48 discovered, 34 researched, 1 unit-tested and 60
  integration-tested. Repository-wide formatting and parity validation pass. Root unit tests and
  all 38 cache-aware integration suites passed in 429 Gradle tasks with zero failures. An isolated
  runtime smoke loaded 44 plugin modules and 159 scripts, bound port 0 and reported revision
  233/Desktop before shutdown; RuneLite remained closed and the user's running server was untouched.

- Added a dedicated amethyst-cutting Crafting plugin for all four cache-233 products. One native
  four-choice quantity menu covers bolt tips, arrowtips, javelin tips and dart tips with boosted
  levels 83/85/87/89, exact 15/15/5/8 yields, 60 Crafting XP per source, the members and chisel
  gates, and cache sequence 6295.
- Every selection atomically replaces one amethyst after two ticks, including in a full inventory,
  revalidates the tool, source and selected level between results, supports both held-item orders,
  and participates in the shared Crafting cancellation policy. Exact menu memory, messages, audio,
  packet ordering and the wider matching-client interruption matrix remain explicitly incomplete.
- Eight focused cache-aware integrations pin the registry and cache identities, exact four-product
  menu payload, every transform and XP award, two-tick cadence, all product-level boundaries,
  membership/tool/source failures, full-inventory replacement, reverse held-use, interruption and
  cross-Crafting cancellation. Focused formatting and integration pass.
- The manifest now tracks 144 unique groups: 48 discovered, 34 researched, 1 unit-tested and 61
  integration-tested. Repository-wide formatting and parity validation pass. Root unit tests and
  all 39 cache-aware integration suites passed in 433 Gradle tasks with zero failures. An isolated
  runtime smoke loaded 45 plugin modules and 160 scripts, bound port 0 and reported revision
  233/Desktop before shutdown; RuneLite remained closed and the user's running server was untouched.

- Added a dedicated ballista-assembly Fletching plugin for the complete light and heavy chains.
  Six typed item-on-item recipes cover frame plus limbs, incomplete weapon plus spring, and
  unstrung weapon plus monkey tail at boosted levels 47/72 with exact 15/15/300 and 30/30/600 XP.
- Every stage is members-only, supports both held-item orders, performs one atomic two-to-one
  transform safely from a full inventory, and cancels existing Fletching production. The cache has
  no evidenced assembly animation and the historical creation tables leave ticks unknown, so the
  implementation does not substitute combat sequences; exact timing, messages, audio and client
  ordering remain explicit matching-client work.
- Six focused cache-aware integrations pin all component IDs, all stage transforms and XP awards,
  both full-chain totals, boosted-level and membership boundaries, missing-component atomicity,
  full-inventory reverse held-use and shared cancellation. Focused formatting and integration pass.
- The manifest now tracks 145 unique groups: 48 discovered, 34 researched, 1 unit-tested and 62
  integration-tested. Repository-wide formatting and parity validation pass. Root unit tests and
  all 40 cache-aware integration suites passed in 437 Gradle tasks with zero failures. An isolated
  runtime smoke loaded 46 plugin modules and 161 scripts, bound port 0 and reported revision
  233/Desktop before shutdown; RuneLite remained closed and the user's running server was untouched.

- Added a dedicated Celastrus-battlestaff Fletching plugin. The native bark `Fletch` operation and
  both ordinary/Fletching knife held-use orders now batch one bark into one battlestaff at boosted
  level 40 for 80 XP, with members/tool gates and generic Fletching sequence 1248.
- Production lands the first and ordinary-knife results every four ticks and applies the cache-era
  Fletching knife's one-tick reduction only after the first item, from inventory or off hand. Each
  result is an atomic full-inventory-safe replacement; tool/source loss interrupts and starting it
  cancels the other five queued Fletching families.
- Eight focused cache-aware integrations pin object IDs and the native operation, exact transform,
  XP, animation, both timing paths, boosted-level/membership/tool/source boundaries, full inventory,
  direct and reverse held use, interruption and shared cancellation. Focused formatting and
  integration pass; exact messages, audio, packet ordering and wider client interruption remain
  explicit matching-client work.
- The manifest now tracks 146 unique groups: 48 discovered, 34 researched, 1 unit-tested and 63
  integration-tested. Repository-wide formatting and parity validation pass. Root unit tests and
  all 41 cache-aware integration suites passed in 441 Gradle tasks with zero failures. An isolated
  runtime smoke loaded 47 plugin modules and 162 scripts, bound port 0 and reported revision
  233/Desktop before shutdown; RuneLite remained closed and the user's running server was untouched.

- Added a dedicated toxic-blowpipe creation Fletching plugin. Both chisel/Tanzanite-fang held-use
  orders now create the empty blowpipe at boosted level 78 for 120 XP, with membership and tool
  gates, cache sequence 3015 and a synchronous zero-tick transform matching the published table.
- Each click atomically replaces exactly one fang with empty object 12924, including in a full
  inventory, and cancels all six queued Fletching families. Charged object 12926 is deliberately
  excluded; scales, darts, checking, unloading, uncharging, dismantling, death and weapon behavior
  remain separate lifecycle work.
- Six focused cache-aware integrations pin all three object identities and item states, sequence
  3015, exact immediate transform and XP, boosted-level/membership/tool/source boundaries,
  full-inventory reverse held use and shared cancellation. Focused formatting and integration pass;
  exact messages, audio and client packet ordering remain explicit matching-client work.
- The manifest now tracks 147 unique groups: 48 discovered, 34 researched, 1 unit-tested and 64
  integration-tested. Repository-wide formatting and parity validation pass. Root unit tests and
  all 42 cache-aware integration suites passed in 445 Gradle tasks with zero failures. An isolated
  runtime smoke loaded 48 plugin modules and 163 scripts, bound port 0 and reported revision
  233/Desktop before shutdown; RuneLite remained closed and the user's running server was untouched.

- Extended the standard ammunition plugin with all seven bronze-through-rune brutal-arrow recipes.
  Each combines up to six flighted ogre arrows with the matching nails after two ticks, using exact
  boosted levels 7/18/33/38/49/62/77 and per-arrow XP 1.4/2.6/5.1/6.5/7.5/10.2/12.5.
- The recipes require membership, hammer 2347 and cache Zogre learning varbit 499; play dedicated
  human ogre-Fletching sequence 4433; accept both item orders; preserve partial and full-inventory
  atomicity; and share the existing ammunition/Fletching cancellation queue.
- Three additional focused cache-aware integrations pin all objects, outputs, levels, XP, sequence,
  unlock/tool gates, exact two-tick boundary, partial set, reverse held use and full inventory.
  Focused formatting and the expanded 16-test ammunition suite pass. Achey shafts, wolfbone tips,
  flighting, ordinary ogre arrows and composite ogre bows remain separate work.
- The manifest now tracks 148 unique groups: 48 discovered, 34 researched, 1 unit-tested and 65
  integration-tested. Repository-wide formatting and parity validation pass. Root unit tests and
  all 42 cache-aware integration suites passed in 445 Gradle tasks with zero failures. An isolated
  runtime smoke loaded 48 plugin modules and 163 scripts, bound port 0 and reported revision
  233/Desktop before shutdown; RuneLite remained closed and the user's running server was untouched.

- Extended the ammunition plugin with the final ordinary ogre-arrow stage. Up to six flighted ogre
  arrows 2865 and wolfbone arrowtips 2861 become ogre arrows 2866 after exactly two ticks at boosted
  level 5 for one Fletching XP each, using sequence 4433 and accepting both held-item orders.
- The membership and quest gates are cache-backed: varp 293 is Big Chompy Bird Hunting progress,
  quest DB row 8 inherits unstarted state 0 and defines completion at 65, so only state zero is
  rejected. The transform is atomic with partial sets and a full inventory and shares the existing
  Fletching cancellation queue.
- Three focused cache-aware integrations expand the ammunition suite to 19 tests and pin the cache
  objects, quest table/varp boundaries, level, XP, animation, exact two-tick result, reverse use,
  partial production and inventory safety. Achey-log shafts, wolfbone-tip cutting, flighting and
  composite ogre bows remain separate work.
- The manifest now tracks 149 unique groups: 48 discovered, 34 researched, 1 unit-tested and 66
  integration-tested. Repository-wide formatting and parity validation pass. Root unit tests and
  all 42 cache-aware integration suites passed in 445 Gradle tasks with zero failures. An isolated
  runtime smoke loaded 48 plugin modules and 163 scripts, bound port 0 and reported revision
  233/Desktop before shutdown; RuneLite remained closed and the user's running server was untouched.

- Added the preceding flighted-ogre-arrow stage from the final pre-cutoff Wiki revision 14927093.
  Up to six ogre arrow shafts 2864 now combine with four ordinary feathers 314 each after exactly
  two ticks, producing flighted ogre arrows 2865 for 0.9 Fletching XP apiece at boosted level 5.
- Reused the cache-backed membership and Big Chompy started-state gates and dedicated sequence 4433.
  The ammunition recipe model now explicitly supports non-1:1 secondary-material ratios, including
  rejecting fewer than four feathers before queueing; all existing recipes retain ratio one.
- Three focused integrations cover the exact cache contract, 24-feather six-arrow production,
  animation and two-tick timing, XP, quest/membership/level gates, insufficient feathers, reverse
  held use, partial production and full-inventory atomicity. The focused ammunition suite passes.
- The manifest now tracks 150 unique groups: 48 discovered, 34 researched, 1 unit-tested and 67
  integration-tested. Achey-log shaft cutting, wolfbone-tip cutting and composite-ogre-bow
  production remain separate work. Repository-wide formatting and parity validation pass; root
  unit tests and all 42 cache-aware integration suites passed in 445 Gradle tasks with zero
  failures. An isolated runtime smoke loaded 48 plugin modules and 163 scripts, bound port 0 and
  reported revision 233/Desktop before shutdown; RuneLite remained closed and the user's running
  server was untouched.

- Added cache-233 composite-ogre-bow stringing to the existing bow-stringing plugin. Unstrung comp
  bow 4825 plus bow string 1777 becomes comp ogre bow 4827 after two ticks at boosted level 30 for
  45 Fletching XP, using sequence 4433 and the native quantity flow.
- The recipe requires membership and learned-production varbit 500 (`thzfe_makecompozogrebow`),
  accepts both held-item orders, remains atomic in a full inventory and also consumes one persistent
  bow-string-spool charge because the cutoff spool contract applies to unfinished bows.
- Three focused integrations pin the cache contract and verify loose-string/spool production,
  timing, XP, animation, unlock and level gates, reverse use and full-inventory replacement. The
  focused bow-stringing suite passes. The initial Achey-log/wolf-bone cutting stage remains open.
- The manifest now tracks 151 unique groups: 48 discovered, 34 researched, 1 unit-tested and 68
  integration-tested. Repository-wide formatting and parity validation pass; root unit tests and
  all 42 cache-aware integration suites passed in 445 Gradle tasks with zero failures. An isolated
  runtime smoke loaded 48 plugin modules and 163 scripts, bound port 0 and reported revision
  233/Desktop before shutdown; RuneLite remained closed and the user's running server was untouched.

- Added a dedicated wolfbone-arrowtip plugin shared by Crafting and Fletching. Chisel 1755 plus one
  wolf bone 2859 now produces an inclusive random 2–6 wolfbone arrowtips 2861 after two ticks at
  boosted level 5 in both skills, awarding 2.5 XP per resulting tip to each skill.
- Membership and cache-backed Big Chompy varp 293 state greater than zero are required. Both held
  item orders work, the delayed replacement is atomic in a full inventory, and queue symbol 64 is
  integrated into every existing Crafting and Fletching cancellation path.
- Five focused integrations pin all cache identifiers and exercise every possible yield, exact
  dual XP, sequence 4433, timing, requirements, reverse use, full inventory, resource loss and
  mutual skill interruption. The Wiki confirms a stat-independent random 2–6 range but does not
  publish per-outcome weights, so uniform inclusive selection is explicitly recorded as an
  inference pending production sampling.
- The manifest now tracks 152 unique groups: 48 discovered, 34 researched, 1 unit-tested and 69
  integration-tested. Repository-wide formatting and parity validation pass; root unit tests and
  all 43 cache-aware integration suites passed in 449 Gradle tasks with zero failures. An isolated
  runtime smoke loaded 49 plugin modules and 164 scripts, bound port 0 and reported revision
  233/Desktop before shutdown; RuneLite remained closed and the user's running server was untouched.

- Added a dedicated ogre-arrow-shaft Fletching plugin. Either knife 946 or Fletching knife 31043
  used with one Achey tree log 2862 now produces an inclusive random 2–6 ogre arrow shafts 2864
  after exactly three ticks at boosted level 5, awarding 1.6 Fletching XP per resulting shaft.
- Membership and cache-backed Big Chompy varp 293 state greater than zero are required. Both held
  item orders work, only one log is processed per interaction, the replacement remains atomic in a
  full inventory, and queue symbol 65 is integrated into every existing Fletching cancellation path.
- Five focused integrations pin all cache identifiers and exercise every possible yield, exact XP,
  sequence 4433, timing, requirements, both knives, reverse use, full inventory, resource loss and
  cross-Fletching interruption. The dedicated Wiki recipe is authoritative for 1.6 XP per shaft
  over conflicting broader Achey-log prose; its random 2–6 range has no published outcome weights,
  so uniform inclusive selection remains an explicit inference pending production sampling.
- The manifest now tracks 153 unique groups: 48 discovered, 34 researched, 1 unit-tested and 70
  integration-tested. Repository-wide formatting and parity validation pass; root unit tests and
  all 44 cache-aware integration suites passed in 453 Gradle tasks with zero failures. An isolated
  runtime smoke loaded 50 plugin modules and 165 scripts, bound port 0 and reported revision
  233/Desktop before shutdown; RuneLite remained closed and the user's running server was untouched.

- Extended the Ogre-arrow-shaft plugin into the shared cache-233 Achey-log interaction. Ordinary
  knife 946 or Fletching knife 31043 now opens interface 270/clientscript 2046 with the ogre-arrow-
  shaft and unstrung-comp-bow choices instead of registering a conflicting second held-use handler.
- The new bow choice consumes Achey tree log 2862 and wolf bones 2859 after exactly three ticks,
  creates unstrung comp bow 4825, awards 45 Fletching XP and requires membership, boosted level 30
  and learned-production varbit 500. It uses sequence 4433, accepts both held orders, remains
  atomic in a full inventory and shares the established Ogre/Fletching cancellation queue.
- The focused suite now has eight integrations covering the full cache contract, exact two-choice
  single-action menu payload, all shaft yields, both products' tick/XP/material behavior, quest and
  level gates, both knives, reverse full-inventory use, input loss and cross-Fletching cancellation.
  Historical Wiki revision 14514176 supplies the three-tick recipe; exact menu presentation/order,
  messages, sound and packet order remain explicit workspace-client capture boundaries.
- The manifest now tracks 154 unique groups: 48 discovered, 34 researched, 1 unit-tested and 71
  integration-tested. Repository-wide formatting and parity validation pass; root unit tests and
  all 44 cache-aware integration suites passed in 453 Gradle tasks with zero failures. The existing
  plugin topology remains 50 modules and 165 scripts; an isolated runtime smoke bound port 0 and
  reported revision 233/Desktop before shutdown. RuneLite remained closed and the user's running
  server was untouched.

- Extended the existing ammunition Fletching plugin with all 123 cache-233 poisoned-ammunition
  transformations: 41 arrow, normal-bolt, dart, javelin and throwing-knife bases across weapon
  poison, weapon poison+ and weapon poison++. Each members-only interaction accepts both held-item
  orders, immediately poisons up to five ammunition, consumes one poison vial, returns an empty
  vial and awards no skill XP.
- The transaction is atomic when inventory capacity is insufficient, repeated interactions consume
  the next batch, and the synchronous action clears every established Fletching production queue.
  Typed object references and exact input/output identities are pinned for every recipe.
- Five focused integrations cover the complete cache contract, all 123 transformations, full and
  partial stacks, reverse use, membership, capacity, repetition, no-XP behavior and shared
  cancellation; the ammunition module now has 27 passing integrations. Historical Wiki revisions
  establish the five-per-vial and zero-tick behavior, while exact partial-stack acceptance,
  messages, audio, packet ordering and interruption presentation remain explicit workspace-client
  capture boundaries. Melee-weapon poisoning, cleaning cloth and poison-on-hit combat remain open.
- The manifest now tracks 155 unique groups: 48 discovered, 34 researched, 1 unit-tested and 72
  integration-tested. Repository-wide unit and integration tests passed in 453 Gradle tasks with
  zero failures. The existing plugin topology remains 50 modules and 165 scripts; an isolated
  runtime smoke bound port 0 and reported revision 233/Desktop before shutdown. RuneLite remained
  closed and the user's running server was untouched.

- Extended the toxic-blowpipe Fletching plugin with the standard cache-233 inventory and worn-item
  lifecycle. Empty or loaded blowpipes now accept Zulrah's scales and all nine unpoisoned dart
  tiers in either held-use order, enforce independent 16,383-unit caps and retain their packed
  dart type, dart count and scale count in the three cache-backed inventory-object bit fields.
- Check, Unload, Uncharge and Dismantle are registered against the cache's held/worn options.
  Dart-type swaps return the previous ammunition atomically, uncharge returns all ammunition after
  confirmation and plays synth 2381, and dismantling an empty blowpipe yields 20,000 scales. Every
  mutation has membership, stale-slot and inventory-capacity protection, including full-inventory
  replacements that do not require an unnecessary free slot.
- Eighteen focused integrations pin the object IDs, option ordering, bit layouts, sound ID and
  persisted dart mapping, then cover creation, all accepted charge inputs, both caps, dart swaps,
  unload, uncharge, dismantle, membership, level, queue cancellation and atomic failure behavior.
  Historical Wiki revision 14978936 establishes the cutoff lifecycle; the persisted dart codes are
  an explicit workspace migration contract because neither cache metadata nor the Wiki exposes the
  server's original enum mapping.
- The manifest now tracks 156 unique groups: 48 discovered, 34 researched, 1 unit-tested and 73
  integration-tested. Repository-wide formatting and parity validation pass; root unit tests and
  all cache-aware integration suites passed in 453 Gradle tasks with zero failures. An isolated
  runtime smoke loaded 50 plugin modules and 165 scripts, bound port 0 and reported revision
  233/Desktop before shutdown. Bank charge configuration, ornament variants, scale pickup, death
  behavior and combat/depletion remain open. RuneLite remained closed and the user's running server
  was untouched.

- Extended the same plugin with the cache-233 blazing-blowpipe ornament lifecycle. Trailblazer
  reloaded kit 28690 now combines with standard empty blowpipe 12924 in either held order at zero
  ticks, creates blazing empty object 28687 and plays the source-named Found gem synth 2655. Loaded
  standard blowpipes reject the kit without mutation.
- The existing packed charge implementation now treats 28687/28688 as a style pair: all nine darts,
  scales, swaps, Check, Unload and Uncharge preserve the blazing appearance, with Uncharge returning
  to 28687. Dismantling 28687 returns both standard empty object 12924 and kit 28690 rather than
  scales, and its net-positive inventory transform fails atomically when there is no output slot.
- The focused module now has 23 passing integrations. Exact IDs and cache operation ordering,
  both item orders, full-inventory application, synth 2655, empty-only rejection, packed-style
  transitions, component recovery and capacity failure are covered. The cutoff evidence is Wiki
  revisions 14787705 and 14931538; exact messages, confirmation presentation and packet ordering
  remain explicit client-capture boundaries.
- The manifest now tracks 157 unique groups: 48 discovered, 34 researched, 1 unit-tested and 74
  integration-tested. The combined formatting, parity, unit and integration gate passed all 767
  actionable Gradle tasks with zero failures. An isolated runtime smoke again loaded 50 modules and
  165 scripts, bound port 0 and reported revision 233/Desktop before shutdown. RuneLite remained
  closed and the user's running server was untouched.

## 2026-09-09 — Prayer ordinary remains core

- Added a dedicated Prayer-remains plugin for every ordinary cache-233 inventory Bury and Scatter
  operation: 35 bone objects and five demonic ashes. The explicit recipe catalogue pins exact XP,
  members/free-world availability and the unboostable level-70 superior-dragon-bone boundary.
- Bone burial atomically consumes one item, awards XP immediately, plays sequence 827 and synth
  2738, emits the burial message and uses a two-tick weak cooldown. Ash scattering uses an
  independent two-tick cooldown, so one bone and one ash can both complete in the same tick as the
  historical free-to-play training source describes.
- Long and curved bones consult cache varbit 16616 and revalidate the exact inventory slot after
  confirmation. The functional warning remains below client-tested status because the historical
  title, choices and disable control are not published. Ash animation and sound are likewise left
  unguessed pending revision-matched client capture.
- A cache-wide classifier proves that the only excluded Bury/Scatter objects are the 15 known
  quest/minigame remains. Twelve focused integrations cover catalogue completeness, exact XP and
  membership for all 40 ordinary objects, dispatch, animation/sound/message output, cooldowns,
  combined bone/ash use, superior-bone boundaries, all warning paths, stale-slot atomicity and
  quest-object exclusion.
- The manifest now tracks 160 unique groups: 48 discovered, 35 researched, 1 unit-tested and 76
  integration-tested. The combined formatting, parity, unit and integration gate passed all 773
  actionable Gradle tasks with zero failures. An isolated runtime smoke loaded 51 modules and 166
  scripts, bound port 0 and reported revision 233/Desktop before shutdown. RuneLite remained closed
  and the user's running server was untouched.

## 2026-09-09 — Prayer chapel and Chaos altar offerings

- Extended the Prayer-remains plugin with bone-on-altar dispatch for all 28 cache-233 POH chapel
  altar variants across the seven oak-through-gilded tiers. It applies the exact 100–250% base
  multipliers, counts only the three lit incense-burner identities at +50 percentage points each,
  caps at two burners and uses the three documented acceptance messages.
- POH manual actions enforce a one-tick click boundary, select the first top-left matching bone and
  restart a type-locked automatic queue that repeats every three ticks. Movement, altar removal,
  membership, level and stale-inventory checks stop atomically. Sequence 3705 and spot animation
  624 are cache-pinned; no unevidenced sound was added.
- Added Deep Wilderness Chaos altar handling for shared location 411 only inside zone 368,477,0.
  It grants 350% XP, revalidates the exact manually selected slot, rolls the 50% altar save before
  equipment preservation and sends the documented save feedback. Other placements reject without
  consuming or awarding XP.
- Added all four Zealot objects and the exact 1.25%-per-piece denominator-80 bone preservation roll
  to burial, POH offerings and failed Chaos saves. Ashes remain excluded. Twenty-four focused
  integrations cover the exact cache catalogue and visuals, every multiplier tier, burners,
  dispatch/slot policy, manual and automatic timing, interruption, location guards, both save
  paths, membership and superior-bone boundaries.
- The manifest now tracks 163 unique groups: 48 discovered, 35 researched, 1 unit-tested and 79
  integration-tested. The combined formatting, parity, unit and integration gate passed all 773
  actionable Gradle tasks with zero failures. An isolated runtime smoke loaded 51 modules and 166
  scripts, bound port 0 and reported revision 233/Desktop before shutdown. RuneLite remained closed
  and the user's running server was untouched.

## 2026-09-09 — Prayer-point restoration altar core

- Audited every cache-233 location with a Pray or Pray-at operation and added a disjoint permanent
  catalogue: 60 ordinary restoration locations, two +2 altars, five God Wars altars and 17 special
  quest, spellbook, transport, damage or stateful locations. A cache-wide integration assertion
  will fail if a future cache introduces an unclassified Pray location or any category overlaps.
- Ordinary altar operations now restore Prayer exactly to the base level, including normalizing an
  existing overcharge. The Edgeville Monastery and Nature Grotto altars restore to base +2. Every
  implemented path faces the location, plays cache sequence 645, resets accumulated fractional
  Prayer-drain progress per the 5 April 2023 rule and sends the established recharge message.
- God Wars research pinned the shared ten-minute/out-of-combat rule, +1 per matching equipped item,
  cache timer varbit 4099 and Nex's conditional Hitpoints/run/special restores. Those five altars
  remain isolated pending a complete cache-233 aligned-equipment catalogue and cooldown lifecycle;
  no item-name heuristic was substituted for that contract.
- Four new focused integrations cover the complete 84-location classification, exact category
  counts, real operation dispatch, normal restoration, overcharge normalization, both +2 altars,
  sequence 645 and drain-progress reset. The manifest now tracks 165 unique groups: 48 discovered,
  36 researched, 1 unit-tested and 80 integration-tested. The combined formatting, parity, unit and
  integration gate passed all 773 actionable Gradle tasks with zero failures. An isolated runtime
  smoke loaded 51 modules and 166 scripts, bound port 0 and reported revision 233/Desktop before
  shutdown. RuneLite remained closed and the user's running server was untouched.

## 2026-09-09 — God Wars altar restoration

- Implemented Pray for all four original God Wars boss-room altars and Nex's Zaros altar. Every
  altar rejects combat, shares an exact persistent 600-second deadline and drives cache varbit 4099
  for the workspace RuneLite's ten-minute timer; reconnect and cooldown rejection reconstruct its
  ceiling-minute client value without weakening exact server enforcement.
- Each successful use follows the existing sequence-645 Prayer-restoration path and restores to
  base plus one point per matching worn item. Saradomin and Zamorak use cache params 40 and 41;
  explicit cutoff catalogues pin 34 Armadyl, 30 Bandos and 62 Zaros wearable variants, including
  charged, ornamented, Deadman and Echo Virtus forms while excluding LMS/beta/rotten copies.
- Nex's altar restores only Prayer without Zaros equipment. With at least one Zaros item it also
  restores Hitpoints to base without lowering an existing overheal, run energy to 100% and special
  attack energy to 100%. The combat rejection does not start or extend the cooldown.
- Six focused God Wars integrations cover exact cache IDs and flag counts, all five operation
  bindings, per-item bonuses, conditional Nex restoration, overheal safety, shared cooldown,
  precise expiry, combat rejection and reconnect timer reconstruction. Separate altar Teleport
  operations and byte-exact production messages/audiovisual ordering remain explicit follow-up.
- The manifest now tracks 166 unique groups: 48 discovered, 36 researched, 1 unit-tested and 81
  integration-tested. The combined formatting, parity, unit and integration gate passed all 773
  actionable Gradle tasks with zero failures. An isolated runtime smoke loaded 51 plugin modules
  and 166 scripts, bound port 0 and reported revision 233/Desktop before shutdown. RuneLite remained
  closed and the user's running server was untouched.

## 2026-09-09 — Entrana, Tribal Statue and hazardous Prayer altars

- Pinned the two interactive Entrana Devious Minds altar states and the repaired Tai Bwo Wannai
  Tribal Statue as ordinary restoration rather than leaving their Pray operations in the special
  bucket. Their owning quests still control which location state is reachable; the Prayer plugin
  handles only the cache-exposed Pray operation once that state exists.
- Added dedicated handling for the Forthos Cursed Altar. It faces the altar, uses cache sequence
  645, clears fractional Prayer-drain progress, drains all current Prayer and applies a direct
  typeless hit equal to the amount drained, clamped to 5–30. Zero Prayer still deals 5 damage and
  the hit is intentionally lethal.
- Added dedicated handling for the Darkmeyer vampyric statue. It follows ordinary full Prayer
  restoration and drain-counter reset, then applies the documented random 1–3 Hitpoints cost.
  Direct-hit processing preserves the normal hitsplat and death lifecycle.
- The permanent cache classifier now partitions all 84 Pray/Pray-at locations without overlap into
  63 ordinary, two boosted, five God Wars, two hazardous and 12 special locations. Focused
  integration covers real operation dispatch, all Cursed Altar boundaries and lethal damage, and
  every Darkmeyer random outcome. Exact hazardous-altar presentation and packet ordering remain
  revision-matched client-capture work.
- The manifest now tracks 167 unique groups: 48 discovered, 36 researched, 1 unit-tested and 82
  integration-tested. The combined formatting, parity, unit and integration gate passed all 773
  actionable Gradle tasks with zero failures in 1m 59s. An isolated runtime smoke loaded 51 plugin
  modules and 166 scripts, bound port 0 and reported revision 233/Desktop before shutdown. RuneLite
  remained closed and the user's running server was untouched.

## 2026-09-09 — Elidinis Statuette restoration core

- Moved cache location 10439 out of the special bucket into a dedicated utility-restoration path.
  Cache quest row 139 pins Spirits of the Elid completion to `elidquest` state 60; a forced
  operation at state 59 has no effect.
- Completed uses restore Prayer to base and reset fractional drain progress, heal Hitpoints to base
  before applying the documented visible +7 boost, fill run and special energy, and clear both
  poison and venom client state. Existing Hitpoints boosts above base +7 are preserved.
- Added cache references for quest varbit 1444, poison type varbit 6251 and poison varps 102/3777.
  The workspace RuneLite independently confirms the poison varp/type identities. Focused tests pin
  all IDs, cache completion column 19, real operation dispatch, every restored resource, poison and
  venom clearing, the quest boundary and stronger-overheal preservation.
- The permanent 84-location classifier now partitions 63 ordinary, two boosted, five God Wars, two
  hazardous, one utility-restoration and 11 special Pray locations without overlap. Medium Desert
  Diary credit only when Prayer is actually restored remains a separate researched row because the
  repository has no diary-task service or verified packed task bit. Exact presentation remains
  revision-matched client-capture work.
- The manifest now tracks 169 unique groups: 48 discovered, 37 researched, 1 unit-tested and 83
  integration-tested. The combined formatting, parity, unit and integration gate passed all 773
  actionable Gradle tasks with zero failures in 2m. An isolated runtime smoke loaded 51 plugin
  modules and 166 scripts, bound port 0 and reported revision 233/Desktop before shutdown. RuneLite
  remained closed and the user's running server was untouched.

## 2026-09-09 — Astral and Ancient Pyramid spellbook altars

- Moved cache locations 34771 and 6552 out of the unresolved special bucket into a dedicated
  spellbook-altar category. The permanent classifier now partitions all 84 Pray/Pray-at locations
  into 63 ordinary, two boosted, five God Wars, two hazardous, one utility-restoration, two
  spellbook and nine remaining special locations without overlap.
- Registered the Astral Altar's cache operation 2 and Ancient Pyramid altar's operation 1. Cache
  quest rows pin Lunar Diplomacy completion to `lunar_quest_main` state 190 and Desert Treasure I
  completion to `deserttreasure` state 15; pre-completion forced operations do not mutate state.
- Astral switches Standard, Ancient or Arceuus to Lunar and Lunar back to Standard with the exact
  `Lunar spells activated!`/`deactivated!` messages while preserving current Prayer and fractional
  drain progress. The pyramid switches every non-Ancient book to Ancient and Ancient back to
  Standard with its exact wisdom/memory messages, drains Prayer to zero, clears fractional drain
  progress and remains usable when Prayer already equals zero.
- Five focused integrations pin location IDs and operation slots, quest-row completion values,
  quest varbit/base-var identities, spellbook values, both quest boundaries, every source-book
  branch, messages, sequence 645 and Prayer state behavior. Pre-completion wording, autocast state,
  Desert hard-task credit and matching-client audiovisual/packet ordering remain a separate
  researched row.
- The manifest now tracks 171 unique groups: 48 discovered, 38 researched, 1 unit-tested and 84
  integration-tested. The combined formatting, parity, unit and integration gate passed all 773
  actionable Gradle tasks with zero failures in 2m. An isolated runtime smoke loaded 51 plugin
  modules and 166 scripts, bound port 0 and reported revision 233/Desktop before shutdown. RuneLite
  remained closed and the user's running server was untouched.

## 2026-09-09 — Mage Arena god-cape statues

- Moved cache locations 2873, 2875 and 2874 into a dedicated Mage Arena statue category, leaving
  six special Pray locations. All three cache operations are `Pray-at`; their rewards map exactly
  to Saradomin cape 2412, Guthix cape 2413 and Zamorak cape 2414.
- Successful uses require the existing Mage Arena I completion varp 267, face the statue, play
  sequence 645 and create a player-owned private cape on the interaction tile without changing
  Prayer. Repeated prayer creates multiple copies as documented by the Wiki.
- Three focused integrations pin the location, operation, object and varp contracts; verify the
  pre-completion boundary, private ownership, all three reward mappings, sequence and Prayer
  preservation; and cover repeated same-statue claims. Exact presentation and production
  ground-item visibility/despawn timing remain matching-client capture work.
- The manifest now tracks 172 unique groups: 48 discovered, 38 researched, 1 unit-tested and 85
  integration-tested. The combined formatting, parity, unit and integration gate passed all 773
  actionable Gradle tasks with zero failures in 1m 56s. An isolated runtime smoke loaded 51 plugin
  modules and 166 scripts, bound port 0 and reported revision 233/Desktop before shutdown. RuneLite
  remained closed and the user's running server was untouched.

## 2026-09-09 — Yanille trapped Chaos altar

- Moved cache location 412 out of the unresolved special bucket into a dedicated transport-altars
  category, leaving five stateful Pray locations. The cache exposes `Pray-at`; the Wiki confirms
  that this is a damaging trapdoor into the lower Yanille Agility Dungeon poison-spider chamber.
- The handler faces the altar, plays sequence 645, applies the reproduced fixed and lethal 15
  typeless damage, then lands at world coordinate 2578,9578 without changing Prayer or fractional
  drain progress. The workspace RuneLite hard-clue location at 2576,9583 independently confirms
  that the selected landing tile is inside the intended chamber.
- Three focused integrations pin the cache operation, damage and destination contracts; verify
  Hitpoints, Prayer, drain-state and sequence behavior; and cover the exact lethal boundary.
  Precise trapdoor animation, movement, hitmark, sound and packet ordering remain matching-client
  capture work.
- The Prayer integration suite passes all 50 tests. The manifest now tracks 173 unique groups: 48
  discovered, 38 researched, 1 unit-tested and 86 integration-tested. The combined formatting,
  parity, unit and integration gate passed all 773 actionable Gradle tasks with zero failures in
  1m 54s. An isolated runtime smoke loaded 51 plugin modules and 166 scripts, bound port 0 and
  reported revision 233/Desktop before shutdown. RuneLite remained closed and the user's running
  server was untouched.

## 2026-09-09 — Broken sun altar and unrestricted-world exclusion

- Repartitioned all 84 cache Pray/Pray-at locations into 63 ordinary, two boosted, five God Wars,
  two hazardous, one utility, two spellbook, three Mage Arena, one transport, one non-restoring,
  one non-production and three Goblin Temple components, with no overlap.
- Implemented broken sun altar 34872 as a dedicated no-restoration path. It faces the altar,
  preserves current Prayer and fractional drain progress and sends the exact nothing-happens
  message. Cache varbit 8409 selects ordinary prayer sequence 645 while the Knight of Varlamore is
  absent and sun-prayer sequence 8321 after NPC morph 8720 reveals 8712.
- Classified unrestricted-world altar 7814 as a verified production exclusion. Its five cache
  operations are pinned, but the module registers no handler because the source limits it to
  temporary unrestricted worlds and the production Ferox Enclave uses an ordinary altar.
- Four focused integrations pin both cache contracts, both broken-altar animation states and the
  no-handler production boundary. The Prayer integration suite passes all 54 tests. The manifest
  now tracks 175 unique groups: 48 discovered, 38 researched, 1 unit-tested, 87
  integration-tested and 1 verified.
- The combined formatting, parity, unit and integration gate passed all 773 actionable Gradle tasks
  with zero failures in 1m 58s. An isolated runtime smoke loaded 51 plugin modules and 166 scripts,
  bound port 0 and reported revision 233/Desktop before shutdown. RuneLite remained closed and the
  user's running server was untouched.

## 2026-09-09 — Goblin Temple altar quest boundary

- Corrected the residual cache classification: locations 43089, 43097 and 43098 are three
  clickable pieces of one composite Land of the Goblins temple altar, not three quest-state altar
  variants. All ten component locations are static; only those three expose `Pray-at`.
- Added cache varbit 13599 `lotg` as a typed reference and pinned its persistent base varp 3355,
  bit range 0–8 and quest-row-165 completion state 56 in integration coverage.
- Implemented the exact pre-unlock response `The Big High War God does not answer your prayer`
  without changing Prayer or fractional drain progress. State 36 is the documented inference
  boundary: the cutoff Quest Helper map begins the high-priest sequence there, while the Wiki says
  the altar becomes usable between those encounters. From state 36 onward, all three pieces use
  ordinary Prayer restoration and drain-progress reset.
- Three focused cache/behavior integrations cover states 0, 35, 36 and 56 across every clickable
  component. The Prayer integration suite passes all 57 tests. The manifest now tracks 176 unique
  groups: 48 discovered, 38 researched, 1 unit-tested, 88 integration-tested and 1 verified.
- Exact animation timing, any sound and packet ordering remain matching-client capture work.
- The combined formatting, parity, unit and integration gate passed all 773 actionable Gradle tasks
  with zero failures in 1m 54s. An isolated runtime smoke loaded 51 plugin modules and 166 scripts,
  bound port 0 and reported revision 233/Desktop before shutdown. RuneLite remained closed and the
  user's running server was untouched.

## 2026-09-09 — God Wars altar boss-room exits

- Promoted the separate operation-2 path for all five God Wars altars from classification to
  executable behavior. It remains available during combat and bypasses the restoration operation's
  ten-minute cooldown gate.
- Pinned the four original exterior door tiles to Zamorak 2925,5332,2; Saradomin 2908,5265,0;
  Armadyl 2839,5295,2; and Bandos 2863,5354,2. The Nex altar exits to 2900,5204,0 outside the
  essence-gated safe room; a read-only cache decode independently located its inner prison door at
  2899,5203 and arena barrier at 2909,5202.
- Added `PrayerGodWarsAltarTeleport` and registered `onOpLoc2` beside each altar's existing Pray
  handler. The immediate telejump preserves Prayer, Hitpoints, run energy, special energy,
  fractional Prayer drain and both restoration cooldown values.
- Cache and behavior integration coverage pins all five `[Pray, Teleport]` operation arrays,
  destinations, in-combat use, immediate movement and state preservation. The focused Prayer suite
  passes all 58 tests. The manifest remains at 176 unique groups and advances one group from
  researched to integration-tested: 48 discovered, 37 researched, 1 unit-tested, 89
  integration-tested and 1 verified.
- Exact production animation, sound and packet ordering remain matching-client capture work.
- The combined formatting, parity, unit and integration gate passed all 773 actionable Gradle tasks
  with zero failures in 1m 40s. No additional server process was launched for this slice; the
  previously established isolated revision-233 runtime smoke remains valid. RuneLite remained
  closed and the user's running server was untouched.

## 2026-09-09 — Spellbook-altar autocast retention

- Pinned the relevant Wiki evidence to the cache-233 cutoff: Autocast revision 14930998, Astral
  Altar revision 14960697 and Ancient Pyramid altar revision 14918351. The cutoff Autocast page
  says selections are remembered except for incompatible-staff and PvP staff-equipping cases;
  permanent spellbook switching is not an exception.
- Added integration coverage for both directions of the Astral and Ancient Pyramid switches. The
  active autocast spell and defensive mode, plus saved ordinary-staff and bladed-staff spell and
  defensive-mode state, remain unchanged across every switch.
- Split that completed contract out of the broader researched presentation/diary gap. The focused
  Prayer integration suite passes all 59 tests. The manifest now tracks 177 unique groups: 48
  discovered, 37 researched, 1 unit-tested, 90 integration-tested and 1 verified.
- Exact pre-completion feedback, the Ancient Pyramid Desert hard-task credit and revision-matched
  audiovisual and packet ordering remain open.
- The combined formatting, parity, unit and integration gate passed all 773 actionable Gradle tasks
  with zero failures in 2s after the focused suite populated its outputs. No server or RuneLite
  process was launched; the user's running server was untouched.

## 2026-09-09 — Elidinis Statuette PvP and Deadman rules

- Pinned Elidinis Statuette Wiki revision 14925273, the final revision before the cache-233.4
  cutoff. It directly distinguishes all three Hitpoints outcomes: standard worlds heal then add
  seven, PvP worlds do not restore Hitpoints, and Deadman heals normally but omits the +7 boost.
- Added the revision-matched RuneLite world-flag catalogue to the realm API and loaded the selected
  world's existing `worlds.flags` value into each immutable realm-configuration snapshot. The PvP
  and Deadman masks are pinned to `1 << 2` and `1 << 29`; the upgrade playbook now requires these
  masks to be diffed for future clients and migrated deliberately if necessary.
- Updated the Statuette handler so Prayer, run energy, special energy and poison/venom restoration
  remain common. Standard worlds preserve the existing +7 behavior, PvP leaves Hitpoints exactly
  unchanged, and Deadman restores lost Hitpoints to base without an overheal.
- Cache-contract and behavioral coverage verifies all three modes and the two relevant masks. The
  focused Prayer integration suite passes all 60 tests. The manifest now tracks 178 unique groups:
  48 discovered, 37 researched, 1 unit-tested, 91 integration-tested and 1 verified.
- Medium Desert Diary credit and exact audiovisual/packet ordering remain open. No server or
  RuneLite process was launched; the user's running server was untouched.
- The combined formatting, parity, unit and integration gate passed all 773 actionable Gradle tasks
  with zero failures in 2m 1s.

## 2026-09-09 — Firemaking ordinary-log core

- Pinned Firemaking revision 14960094 and Fire revision 14981396, the final relevant revisions
  before the cache-233.4 cutoff. Together they define all 12 ordinary combustible logs, exact
  levels, XP and membership, temporary boosts, four-tick attempts, the success curve, interaction
  directions, movement order, random 60–119-second lifetime and ashes replacement.
- Added a dedicated Firemaking plugin following the repository's injected service, config-reference
  and script-registration patterns. It supports symmetric tinderbox/log inventory use, ground
  `Light` operation 4 and tinderbox-on-ground targeting, while preserving the selected log object
  throughout retries.
- Successful conversion is transactional: the fire location must register before the ground log is
  deleted, and a deletion failure rolls back the fire. Concurrent attempts on one tile cannot both
  consume/reward. The player steps west, east, south or north according to line-of-walk availability
  and safely remains in place when blocked.
- Added cache-native fire location 26185, ashes object 592, sequence 733 and synth 2597 references.
  Fires last an inclusive random 100–199 server cycles and use the repository's delayed-object
  lifecycle so ashes appear on the same cycle the temporary fire expires.
- Added one boundary unit suite and 14 cache-aware integration tests covering cache identities and
  operations, every exact recipe/XP value, membership and boosted levels, four-tick retries,
  audiovisual references, all interaction routes, missing tools, interruption, occupied-tile
  atomicity, directional movement, same-tile races and fire-to-ashes lifetime.
- The focused Firemaking checks pass. The manifest now tracks 179 unique groups: 48 discovered,
  37 researched, 1 unit-tested, 92 integration-tested and 1 verified. Area/activity restrictions,
  closed-door-front checks and every non-ordinary Firemaking system remain open. No server or
  RuneLite process was launched; the user's running server was untouched.
- The repository-wide formatting, parity, unit and cache-aware integration gate passed all 781
  actionable Gradle tasks with zero failures in 2m 10s.

## 2026-09-09 — Barbarian bow Firemaking core

- Pinned cutoff Barbarian Training revision 14988720 and the existing cutoff Firemaking revision.
  They define the members-only initial oak task, unboostable base 35 Firemaking/11 Crafting entry
  requirements, the mandatory Otto follow-up, all 11 pre-Sailing bow-log levels and ordinary XP.
- Added the cache-native `brut_fire` varbit 3759 contract, including its bits 4–5 and four-state
  progression. The first successful task fire advances state 1 to 2 only after transactional fire
  creation/log deletion; state 2 deliberately waits for Otto's future owning dialogue before state
  3 unlocks general bow lighting.
- Added an explicit 15-bow eligibility catalogue rather than accepting a broad weapon category.
  Training, normal, oak, willow, maple, yew, magic, imbued magic and Seercull families use their
  cache-specific sequences 6713–6720 and shared spot animation 1169; representative prohibited bows
  have no registered handler.
- Bow and log use dispatches symmetrically from inventory, preserves the bow, excludes blisterwood,
  uses the ordinary log's XP and transactional fire/ashes/movement lifecycle, and shares the exact
  four-tick retry chance. Bow attempts do not emit the tinderbox strike synth.
- Added one unit boundary case and nine cache-aware integration scenarios spanning exact cache
  identities, all log rows, every supported bow/animation, progression and membership gates,
  unboostable initial requirements, boosted post-unlock levels, both interaction orders, retry
  cadence, XP, exclusions and atomic failures. The focused module now passes 2 unit and 23
  integration tests.
- The manifest now tracks 180 unique groups: 48 discovered, 37 researched, 1 unit-tested, 93
  integration-tested and 1 verified. Otto's dialogue, pyre ships and the remaining Firemaking
  systems remain open. No server or RuneLite process was launched; the user's running server was
  untouched.
- The repository-wide formatting, parity, unit and cache-aware integration gate passed all 781
  actionable Gradle tasks with zero failures in 2m 3s.

## 2026-09-09 — Firemaking placement core

- Extended cutoff Fire revision 14981396 research from its general restrictions into the three
  exact map polygons it publishes. The concave Dwarven area and inclusive Gilbert/Jiggig
  rectangles now use their exact cutoff messages and level-0 boundaries.
- Replaced exact-origin occupancy with complete rotated-footprint detection for diagonal
  full-tile locs, centrepieces and ground decoration. The query follows loc origins across zone
  boundaries and derives its search extent from cache-233 loc dimensions. Ordinary boundary
  walls, wall decoration and roofs remain legal instead of becoming indoor false positives.
- Added the generic-locs module as an explicit Firemaking dependency and reuse its canonical
  `DoorTranslations.translateOpen` geometry. Closed single, left-double and right-double content
  families block their model swing tile in both supported shapes and all angles; opened diagonal
  doors are deliberately excluded from full-tile scenery rejection.
- Added two polygon unit tests and four cache-aware placement integration tests covering concave
  and inclusive boundaries, plane isolation, a multi-tile footprint whose origin crosses a zone,
  every generic closed-door family/shape/angle, open-door/wall/roof exceptions, exact messages and
  atomic inventory rejection. The focused Firemaking module now passes 4 unit and 27 integration
  tests.
- The cutoff Wiki marks its area catalogue incomplete and does not publish complete geometry for
  most banks or the named minigame, raid, dungeon and boss restrictions. Those messages and names
  are recorded as a researched blocker rather than converted into guessed rectangles; current
  Sailing-only rows are excluded from cache 233.
- The manifest now tracks 182 unique groups: 48 discovered, 38 researched, 1 unit-tested, 94
  integration-tested and 1 verified. No server or RuneLite process was launched; the user's
  running server was untouched.
- The repository-wide formatting, parity, unit and cache-aware integration gate passed all 781
  actionable Gradle tasks with zero failures in 32 seconds.

## 2026-09-09 — Coloured fires and chargeable gnomish firelighter

- Pinned cutoff Gnomish firelighters revision 14856213 and Gnomish firelighter revision 14685884.
  They define the five members-only loose colours, exact coating text, guaranteed 50-XP fires,
  one stored holder colour, 1,000-charge capacity, normal-log-only holder use, one charge per fire
  and atomic uncharging of unused firelighters.
- Added cache-native references for all five loose firelighters, coloured logs and coloured fire
  locations plus uncharged holder 20275 and charged holder 20278. Coloured logs use their exact
  fire identity through tinderbox and completed Barbarian-bow inventory/ground routes, while
  sharing ordinary movement, lifetime and ashes behavior.
- Added server-side per-item colour and charge packing with registered custom varobj symbols
  65527/65528 because neither holder cache object contains charge parameters or a cache-owned
  varobj contract. Tests pin the bit layout, every colour and the full 1,000-charge round trip.
- Holder loading supports immediate same-colour stack loading and top-up to the cap; wrong colours
  require Uncharge. Check and Uncharge use their cache inventory-operation slots, uncharging rolls
  back in a full inventory, and an existing loose stack can receive returned charges.
- A charged holder lights only normal logs, succeeds on the first attempt for 50 XP, creates its
  selected coloured fire and consumes exactly one charge only after fire registration and log
  deletion both commit. The final charge returns the uncharged holder. Explicit ground targeting,
  ground `Light` fallback and pending holder-state mutation are covered.
- Added twelve coloured-fire integration scenarios; the focused module now passes 4 unit and 39
  integration tests. Exact holder feedback and load presentation are not present in the cutoff
  pages, so they remain matching-client capture work rather than being labelled verified.
- The manifest now tracks 184 unique groups: 48 discovered, 38 researched, 1 unit-tested, 96
  integration-tested and 1 verified. No server or RuneLite process was launched; the user's
  running server was untouched.
- The repository-wide formatting, parity, unit and cache-aware integration gate passed all 781
  actionable Gradle tasks with zero failures in 1 minute 47 seconds.

## 2026-09-09 — Pyre-log preparation core

- Pinned the final pre-cutoff Pyre logs, Sacred oil and Shades of Mort'ton minigame revisions plus
  the 31 July 2024 Make-All update. Together they define the level-1 members activity, completed
  quest gate, 11 source-log routes, exact two/three/four-dose recipes, flat 5 XP per dose and
  four-tick automatic continuation. The production training guide separately records immediate,
  repeatable one-tick manual interactions.
- Added typed cache references for all sacred-oil states, source/output logs and four Pyromancer
  pieces, plus cache varp 339 and a dedicated queue symbol. Quest database row 128 column 19 pins
  completion to state 85. Cache interface 270 and client script 2046 supply the native quantity
  menu and its oil-dose-aware maximum.
- Preparation preserves the explicitly selected vial and log across the modal menu, performs the
  first selected conversion immediately, then uses a four-tick automatic queue. Oil remainders and
  output logs replace their inputs in place, including a full inventory; mutations, missing
  resources and competing Firemaking actions cancel without partial consumption or excess XP.
- Firemaking XP now shares the cache-unit Pyromancer calculation: individual hood/garb/robe/boots
  bonuses total 2.0%, the complete set adds 0.5%, and warm gloves are explicitly excluded. It is
  applied to ordinary, coloured, bow and pyre-log-preparation XP.
- Added ten cache-aware integration scenarios covering every recipe/ID/XP row, quest state 84/85,
  membership, oil fragmentation and remainders, menu mutation, immediate/manual and automatic
  timing, mid-batch depletion, full-inventory atomicity, interruption and every outfit boundary.
  The focused module passes 4 unit and 49 integration tests.
- The manifest now tracks 186 unique groups: 48 discovered, 39 researched, 1 unit-tested, 97
  integration-tested and 1 verified. Exact preparation feedback, sequence/audio and packet order,
  plus automatic selection among multiple eligible oil vials, remain matching-client capture work.
  Funeral-pyre cremation is a separate still-open feature. No server or RuneLite process was
  launched; the user's running server was untouched.
- The repository-wide formatting, parity, unit and cache-aware integration gate passed all 781
  actionable Gradle tasks with zero failures in 1 minute 45 seconds.

## 2026-09-10 — Shades of Mort'ton funeral-pyre core

- Pinned the final pre-cutoff Funeral Pyre, Stone stand, Shades of Mort'ton minigame and all six
  shade-remains revisions. They define six pyre sites, ten log/stage families, the complete
  log/remains compatibility and dual-XP tables, the 21% coin branch, weighted key branches and
  private five-minute stone-stand rewards.
- Added typed cache references for the empty, log-loaded and remains-loaded pyres, stone stands,
  all six remains, all 25 shade-key identities, sequences 1303–1305, spot animation 292 and the
  hard/elite Morytania diary completion varbits. The script registers cache-native Build, Use,
  Light, tinderbox and manual item-on-location routes.
- Added best-compatible carried-item selection, boosted Firemaking-level checks, members and quest
  gates, staged-location expiry, stale entity/race checks, Firemaking queue interruption, exact
  Firemaking and Prayer awards, independent 50% diary multipliers and explicit Pyromancer
  exclusion. Rewards use inclusive coin ranges and every documented key weight and remain private
  for all 500 ticks without public reveal.
- Corrected the quest-owned exception after tracing cache varp 339, quest database completion state
  85 and the external Quest Helper state map: state 65 permits preparation of normal pyre logs,
  while states 70–84 permit only the quest's normal-log/Loar cremation. Other preparation and
  cremation tiers require completion at state 85. Quest state mutations themselves remain owned by
  the future Shades of Mort'ton quest implementation.
- Added eleven funeral-pyre integration scenarios covering every valid XP pair, cache ID/operation,
  automatic priority, membership/quest/level/compatibility gates, diary and outfit boundaries,
  every reward-roll boundary, private five-minute placement, interruption, races and stage expiry.
  Added an eleventh preparation scenario for the quest exception. The focused Firemaking module
  passes 4 unit and 61 integration tests.
- The manifest now tracks 188 unique groups: 48 discovered, 40 researched, 1 unit-tested, 98
  integration-tested and 1 verified. Exact unlit-stage lifetime, shade-spirit and audiovisual
  packet ordering, ambiguous stone-stand selection, feedback wording and quest transition timing
  remain cache-233.4 capture/integration work and are not labelled verified. No server or RuneLite
  process was launched; the user's running server was untouched.
- The focused formatting, parity, unit and cache-aware Firemaking integration gate passed 333
  actionable Gradle tasks with zero failures in 12 seconds. The repository-wide formatting,
  parity, unit and cache-aware integration gate then passed all 781 actionable Gradle tasks with
  zero failures in 2 minutes 2 seconds.

## 2026-09-10 — Barbarian pyre ships and ancestral Prayer

- Pinned the final pre-cutoff Funeral pyre ship, Chewed bones, Mangled bones, Barbarian Training,
  Pyre ship reward-table, Peaceful/Ferocious barbarian spirit and Hopespear's Will revisions. They
  establish the 11 log rows, dual boosted skill checks and XP, complete axe/tool contract, training
  and Prayer varbits, 200-charge cap, chewed rewards, mangled-spirit behavior and blessing
  exclusions.
- Added typed cache references for both bone objects, Dragon full helm, every accepted modern and
  felling axe, all reward objects, both spirit NPCs, training/charge/diary vars, eight staged
  locations and the canoe/spirit/ship sequences. The Firemaking plugin now handles `Construct` on
  site 25286 through its existing injected service/script structure.
- Implemented four-tick staged construction with station reservation, boosted Crafting and
  Firemaking gates, chewed-before-mangled selection and atomic material commitment. All 11 rows
  award exact Crafting/Firemaking XP; chewed bones advance the first task, cap charges at 200,
  credit the magic-log Kandarin elite bit and roll the exact Dragon-full-helm pre-roll plus 128-
  weight reward table with private ground fallback.
- Mangled bones summon cache NPC 5566 aggressively for 300 cycles without reward or blessing and
  block the creator from another ship until death/despawn. Tests pin its level-166 cutoff-sourced combat
  profile. The complete drop table and enforcement preventing other players from attacking the
  spirit remain open generic combat/drop-system work.
- Integrated the ancestral blessing into ordinary direct burial: one charge triples eligible base
  XP, is spent even when Zealot preserves the bone and emits the exact cutoff message. Tests prove
  that big bones, demonic ashes and chapel altar offerings do not consume or receive the bonus.
  Bonecrusher support remains open because no Bonecrusher processing path exists in the repository.
- Added seven pyre-ship integration scenarios plus four blessing scenarios covering every recipe,
  accepted axe identity, reward boundary, tool and Bruma exclusion, charge cap, training/diary
  state, levels/membership, spirit lifecycle and live-spirit block. The manifest now tracks 192
  unique groups: 48 discovered, 42 researched, 1 unit-tested, 100 integration-tested and 1
  verified.
- The six production site-model transforms, peaceful-spirit lifetime, cosmetic-axe sequence
  substitutions, exact presentation and ferocious owner/drop rules remain explicitly researched.
  No server or RuneLite process was launched; the user's running server was untouched.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 781 actionable Gradle tasks with zero failures in 5 minutes 4 seconds.

## 2026-09-10 — Handheld Firemaking light sources

- Pinned the cutoff Light sources overview plus ten individual item revisions and the cache-233
  object/operation contract for all 11 unlit/lit pairs. The implementation uses the existing
  injected Firemaking plugin, typed object references and inventory transaction APIs.
- Implemented symmetric tinderbox ignition with boosted levels and members gates for torches,
  candles, candle lanterns, oil lamps/lanterns, normal/sapphire/emerald bullseyes and mining helmets.
  The cutoff-specific dry-stick `Rub-together` action lights the first inventory torch at level 30,
  while direct sticks-on-torch use does nothing. All lighting awards zero XP.
- Registered every lit form on its actual cache-233 Extinguish menu slot, including torch op 4,
  emerald and mining-helmet op 3, and made the equipped mining helmet change form without being
  removed. Preserved the cutoff-published sapphire-lantern extinguish message.
- Implemented the four boosted, members-only, zero-XP candle-lantern combinations from empty
  lantern plus unlit/lit white or black candles with atomic same-slot output.
- Added eight integration scenarios covering exact IDs, levels and menu slots; both tinderbox use
  orders for every pair; dry sticks; boost, membership and level boundaries; every inventory
  Extinguish route; worn helmet conversion; every candle assembly; and zero XP. The focused suite
  passes all eight tests.
- Darkness brightness/damage, only-light extinguish refusal, heat-source torch ignition, gas and
  forced-extinguish rules, Lumbridge diary credit, other quest/area hooks and exact presentation
  remain explicitly researched. No server or RuneLite process was launched.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 781 actionable Gradle tasks with zero failures in 5 minutes 4 seconds (111 executed, 6 from
  cache and 664 up-to-date). The manifest now tracks 194 unique groups: 48 discovered, 43
  researched, 1 unit-tested, 101 integration-tested and 1 verified.

## 2026-09-10 — Lamp-oil still processing

- Pinned the cutoff Lamp oil, Lamp oil still, Lamp still, Swamp tar and Emerald lantern revisions.
  They establish one swamp tar per lamp, loaded oil remaining in the still until used, Rimmington
  empty/full scenery 5909/5910, Dorgesh-Kaan's single-form still 22773 and the five supported empty
  container routes.
- Added typed object/location references and registered swamp-tar and empty-container use on both
  stills. Rimmington changes between its cache scenery forms; Dorgesh-Kaan tracks one shared loaded
  dose server-side. Inventory changes are preflighted and committed only after the relevant still
  transition succeeds, including exact-slot replacement in a full inventory.
- Added seven cache-aware integration scenarios covering exact IDs, membership, every container,
  one-tar consumption, both still models, one-dose state, full-inventory replacement, no XP, stale
  interactions and actual item-on-location dispatch. The focused suite passes all seven tests.
- Exact messages, animation and packet timing, persistence/instance scoping and Rimmington's
  imp-repellent/butterfly-jar route remain explicitly researched. No server or RuneLite process was
  launched; the user's running server was untouched. The manifest now tracks 196 unique groups: 48
  discovered, 44 researched, 1 unit-tested, 102 integration-tested and 1 verified.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 781 actionable Gradle tasks with zero failures in 4 minutes 55 seconds (112 executed, 6 from
  cache and 663 up-to-date).

## 2026-09-10 — Ordinary Herblore herb cleaning

- Pinned the final pre-cutoff Herblore, Herb and Game tick revisions. They establish the complete
  15-row ordinary cleaning table, members/Druidic Ritual training gates, inventory left-click,
  immediate first result, automatic continuation and its two-tick-per-herb cadence.
- Added a dedicated Herblore plugin following the repository's module/script/service/typed-reference
  structure. Cache 233 supplies every grimy/clean identity, `Clean` inventory operation 1 and varp
  80; the revision-matched Quest Helper's active states 0–3 establish completion at state 4.
- Implemented exact-slot atomic replacement, fractional XP, boosted-level checks and same-type
  two-tick automatic continuation. Full inventories, membership, quest, level and disappearing
  queued-resource failures preserve inputs and XP.
- Added six cache-aware integration scenarios covering all 15 contracts and actual held-operation
  dispatch, every recipe and XP row, boosted and insufficient levels, membership and quest gates,
  two-tick batching, full-inventory replacement and missing-resource termination. The focused
  Herblore suite passes all six tests.
- Exact feedback/audio/packet ordering, the observed ten-manual-request-per-tick ceiling,
  mixed-herb continuation and the full interruption matrix remain researched. All potion-making,
  Zahur/Degrime services, tar and Barbarian variants, effects and Mastering Mixology remain open.
  No server or RuneLite process was launched; the user's running server was untouched. The
  manifest now tracks 198 unique groups: 48 discovered, 45 researched, 1 unit-tested, 103
  integration-tested and 1 verified.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 787 actionable Gradle tasks with zero failures in 5 minutes 24 seconds (442 executed, 9 from
  cache and 336 up-to-date).

## 2026-09-10 — Ordinary unfinished Herblore potions

- Pinned the final pre-cutoff Herblore, unfinished-potion calculator, Game tick, snapdragon
  workflow, cadantine-blood and vial-of-blood revisions. Together they establish all 16 standard
  herb/liquid/output/level rows, zero XP, the native item-on-item Make-All flow and one result per tick.
- Added typed references for both liquid bases, all 16 unfinished outputs and cache sequence 363. The
  Herblore script registers both item orders through the repository's symmetric held-use DSL and
  uses the existing cache-native quantity interface.
- Implemented boosted level, members and Druidic Ritual gates, maximum quantity derived from both
  inputs, exact one-tick batching, zero XP and atomic two-input/one-output transactions. Full
  inventories, input removal, movement-style action cancellation and replacement of either
  Herblore queue terminate safely.
- Added seven cache-aware integration scenarios covering every recipe contract, menu packets, all
  16 transformations, sequence selection, one-tick Make-All, zero XP, all gates, full inventory,
  resource loss, shared cancellation and actual dispatch in both item orders. The complete
  Herblore module passes all 13 tests.
- Exact message capitalization, sound, animation repetition and output-slot/packet ordering remain
  researched. The manifest now tracks 199 unique groups: 48
  discovered, 45 researched, 1 unit-tested, 104 integration-tested and 1 verified. No server or
  RuneLite process was launched; the user's running server was untouched.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 787 actionable Gradle tasks with zero failures after the blood-base extension in 4 minutes
  50 seconds (112 executed, 6 from cache and 669 up-to-date).

## 2026-09-10 — Standard finished Herblore potions

- Pinned the final pre-cutoff Herblore and Game tick revisions. Together they establish all 31
  ordinary unfinished-potion/secondary/output recipes, exact boosted levels and XP, both
  cadantine-blood products and one result every two ticks.
- Added typed cache references for all 21 secondary identities and 31 outputs, registered both
  held-item orders with the repository DSL, and reused cache sequence 363 plus the native
  one-product quantity interface.
- Implemented members and Druidic Ritual gates, exact atomic two-input/one-output transforms,
  fractional XP, two-tick batching, full-inventory replacement, late input loss, movement-style
  cancellation and shared cancellation across all three Herblore processes.
- Added ten cache-aware integration scenarios covering all 31 contracts and transformations,
  exact sequence/XP/cadence, the quantity packet, boosted/failure gates, blood-base dispatch, full
  inventory, resource loss, shared queue replacement, all 30 four-dose mappings, deterministic
  equipment-roll success/failure, persistent success-only charges, both depletion transforms,
  combined extra-dose/secondary-preservation behavior and weapon-poison exclusion. The complete
  Herblore module passes all 23 tests.
- Implemented the dated chemistry-amulet, alchemist's-amulet and prescription-goggles production
  rules: 5% and 15% four-dose rolls, five shared chemistry successes, cache-varbit-backed
  alchemist charges, independent 10% preservation for both goggle appearances and no extra XP.
  Equipment state commits only after a successful inventory transaction. Break/Options and
  charging/uncharging interactions, exact roll/packet ordering, one-input menu behavior, message
  capitalization, animation repetition, sound and output slot remain researched. The manifest now
  tracks 200 unique groups: 48 discovered, 45 researched, 1 unit-tested, 105 integration-tested
  and 1 verified. No server or RuneLite process was launched; the user's running server was
  untouched.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 787 actionable Gradle tasks with zero failures in 1 minute 53 seconds (110 executed, 6 from
  cache and 671 up-to-date).

## 2026-09-10 — Herblore production-equipment lifecycle

- Pinned the final pre-cutoff Alchemist's amulet transcript plus cache-233 object identities and
  inventory-operation slots for the uncharged/charged amulet, ordinary/noted chemistry amulets and
  both prescription-goggle appearances.
- Implemented transcript-backed inventory Charge, Check and Uncharge. Charging accepts either
  held-item order, consumes one chemistry amulet per ten charges, caps the quantity at 5,000,
  preserves the selected slot and revalidates item/charge state after the suspended count dialog.
  Uncharging revalidates after confirmation, returns one noted chemistry amulet per ten charges,
  discards the remainder, clears the cache charge varbit and performs the complete change/refund as
  one atomic transaction. Both goggles forms now support cache-native bidirectional Change.
- Added eight cache-aware integration scenarios covering identity/operation contracts, direct and
  both held-use charge routes, the 4,990-to-5,000 cap, exact sourced dialogue, failure preservation,
  noted refunds, full-inventory rollback, both goggles directions and chemistry Break with shared
  charge reset. Exact remaining-charge and crumble messages now replace the ordinary mixing text on
  chemistry successes, using the revision-matched RuneLite recognizers that predate the cutoff. The
  focused equipment/mixing suites and complete 31-test Herblore suite pass with zero failures.
- The 4,991–4,999 boundary, exact full-inventory failure wording, goggles sound/presentation,
  bank-side operations, chemistry-amulet Options and exact Break presentation, and the initial
  50-charge Mastering Mixology acquisition remain explicitly observational. The manifest now tracks
  201 unique groups: 48
  discovered, 45 researched, 1 unit-tested, 106 integration-tested and 1 verified. No server or
  RuneLite process was launched; the user's running server was untouched.
- After adding Break and exact chemistry-success messages, the serialized repository-wide
  formatting, parity, unit and cache-aware integration gate passed all 787 actionable Gradle tasks
  with zero failures in 4 minutes 48 seconds (50 executed, 6 from cache and 731 up-to-date).

## 2026-09-10 — Fixed one-to-one Herblore ingredient crushing

- Pinned the final pre-cutoff pestle-and-mortar page and all eight output pages. Together they
  establish the unicorn horn, chocolate bar, desert goat horn, kebbit teeth, blue dragon scale,
  empty bird nest, superior dragon bones and nihil shard recipes, zero level/XP requirements, an
  immediate first transform, a two-tick first continuation and three-tick repeats.
- Added typed references for pestle 233, all eight input/output pairs and cache sequences 364/11095;
  sequence 364 is corroborated by RuneLite's Herblore constant and both sequences are classified as
  grinding by its idle notifier. The script registers both held-item orders using the repository's
  interaction DSL and runs through the shared Herblore cancellation family.
- Implemented selected-slot atomic replacement, members gating, initial/restart animations and the
  sourced automatic cadence without a Druidic Ritual or level gate and without XP. Full inventories,
  missing tools/inputs and replacement work terminate safely.
- Added six cache-aware integration scenarios covering every identity and transform, real event-bus
  dispatch in both orders, exact cadence, members/no-level/no-quest behavior, full-inventory
  replacement, resource loss and shared cancellation. The complete Herblore module passes all 37
  tests. The manifest now tracks 202 unique groups: 48 discovered, 45 researched, 1 unit-tested,
  107 integration-tested and 1 verified. No server or RuneLite process was launched; the user's
  running server was untouched.
- Exact restart packet timing, feedback/audio/presentation and the broad interruption matrix remain
  observational. Variable-yield lava scales, all-at-once crystal shards, Gorak/quest recipes, the
  knife chocolate route and Wesley service remain separate deferred families.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 787 actionable Gradle tasks with zero failures in 4 minutes 51 seconds (110 executed, 6 from
  cache and 671 up-to-date).

## 2026-09-10 — Crystal-shard batch crushing

- Pinned the final pre-cutoff crystal-dust and crystal-shard revisions: one shard creates ten dust,
  the entered quantity is processed all at once, Song of the Elves completion is required and no
  skill level or XP applies. Pinned cache objects 23962/23964, quest varbit 9016 and sequence 364.
- The final pre-cutoff Quest Helper maps the last active quest states 188/190/192 to the final
  Baxtorian conversation; the isolated plugin uses the following two-step state 194 as completion
  pending central quest ownership.
- Implemented a separate suspended count flow with both item orders, safe quantity/multiplication
  bounds, post-dialogue membership/quest/tool/stack validation, one atomic 1:10 transform,
  full-stack selected-slot preservation, zero XP and shared Herblore cancellation. A partial batch
  without a dust stack or free inventory slot rolls back completely.
- Added five cache-aware integration scenarios for identities and actual dispatch, count capping,
  immediate batch conversion, both gates, full-inventory behavior and stale-dialogue protection.
  Focused tests and the complete 42-test Herblore suite pass. The manifest now tracks 203 unique
  groups: 48 discovered, 45 researched, 1 unit-tested, 108 integration-tested and 1 verified. No
  server or RuneLite process was launched; the user's running server was untouched.
- Exact prompt/failure wording, partial output-slot selection, sound and packet presentation remain
  observational; variable-yield lava scales are the next distinct processing family.

## 2026-09-10 — Lava-scale crushing

- Pinned the final pre-cutoff lava-scale-shard page and the two cutoff calculation modules. Together
  they establish members-only processing with no level/XP gate, a base-three yield plus three exact
  visible-Herblore-level rolls, the Hard Wilderness Diary's floored 50% multiplier and an immediate,
  two-tick, then three-tick automatic cadence.
- Added typed references for scale 11992, shard 11994, Hard Wilderness Diary completion varbit 4468
  and initial/restart sequences 364/11095. Implemented both held-item orders, rsmod's native exact
  1/256 interpolation, per-scale diary multiplication, atomic stacking/selected-slot replacement
  and shared Herblore cancellation without a Druidic Ritual gate or XP.
- Added six cache-aware integration scenarios covering cache identity and real event dispatch, all
  ordinary yields 3–6, diary yields 4/6/7/9, exact continuation cadence, both orders, members and
  no-level behavior, full inventories and missing-tool safety. Focused tests and the complete
  48-test Herblore suite pass. The manifest now tracks 204 unique groups: 48 discovered, 45
  researched, 1 unit-tested, 109 integration-tested and 1 verified. No server or RuneLite process
  was launched; the user's running server was untouched.
- Exact success/failure text, sound, animation restart and matching-client packet/RNG/inventory
  ordering remain observational. Gorak/quest pestle recipes, knife chocolate and Wesley services
  remain separate processing families.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 787 actionable Gradle tasks with zero failures in 4 minutes 51 seconds (110 executed, 6 from
  cache and 671 up-to-date).

## 2026-09-10 — Free-to-play knife chocolate processing

- Reused the already-pinned final pre-cutoff chocolate-dust revision, whose separate knife recipe
  establishes free-to-play access, one bar to one dust, no level/quest/XP requirement and the same
  immediate, two-tick, then three-tick automatic cadence as pestle processing. Pinned cache objects
  946/1973/1975.
- Generalised the ordinary crushing queue to retain its starting tool, registered knife/chocolate
  in both item orders and kept the existing pestle behavior unchanged. Knife batches atomically
  replace the selected slot even in a full inventory and stop if that knife disappears rather than
  continuing through another crushing tool.
- Added two cache-aware scenarios covering the free-to-play identities and event dispatch in both
  orders, full-inventory behavior, zero XP, exact cadence and starting-tool loss. The focused suite
  and complete 50-test Herblore suite pass. The manifest now tracks 205 unique groups: 48
  discovered, 45 researched, 1 unit-tested, 110 integration-tested and 1 verified. No server or
  RuneLite process was launched; the user's running server was untouched.
- The shared grinding sequences remain an explicit cache-name/activity inference. Exact knife
  animation, feedback, sound and matching-client packet/inventory order remain observational.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 787 actionable Gradle tasks with zero failures in 4 minutes 46 seconds (50 executed, 6 from
  cache and 731 up-to-date).

## 2026-09-10 — Gorak-claw crushing

- Pinned the final pre-cutoff Gorak-claw-powder and Gorak-claws revisions. They establish a
  members-only one-claw/one-powder quest-item recipe with no level or XP, an immediate first result
  and uniform three-tick automatic repeats; the powder may be made during and after Fairytale II.
- Added typed cache references for objects 9016/9018 and a dedicated queue because this family lacks
  the ordinary recipes' two-tick first continuation. Implemented both item orders, selected-slot
  atomicity, sequences 364/11095 and shared Herblore cancellation. Legitimate possession is the
  processing gate; acquisition remains quest/drop-owned and no unsupported quest-state check was
  invented.
- Added four cache-aware scenarios covering identity/dispatch, exact cadence, both item orders,
  membership, level/Druidic independence, zero XP, full inventory and missing-pestle safety. The
  focused suite and complete 54-test Herblore suite pass. The manifest now tracks 206 unique groups:
  48 discovered, 45 researched, 1 unit-tested, 111 integration-tested and 1 verified. No server or
  RuneLite process was launched; the user's running server was untouched.
- Exact feedback, sound, animation restart and matching-client packet/inventory ordering remain
  observational, as does any hidden Fairytale progress rejection for artificially injected claws.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 787 actionable Gradle tasks with zero failures in 4 minutes 52 seconds (110 executed, 6 from
  cache and 671 up-to-date).

## 2026-09-10 — Additional fixed 0/2/3 pestle processing

- Inventoried the cutoff pestle recipe surface and pinned eleven final pre-cutoff pages defining
  thirteen further fixed routes: guam, charcoal, all three karambwan states, mud rune, garlic, bat
  bones, ashes, rune shards, anchovies, dried thistle and seaweed. Every route has an immediate
  result, two-tick first continuation and three-tick repeats with no Herblore level or XP.
- Added all 26 exact cache object references and extended the typed recipe table and both-order
  registration without duplicating script structure. Quest-item acquisition remains owned by each
  quest; the processing core applies membership through the pestle but no unrelated Druidic gate.
- Corrected generic output placement for stacked inputs: stackable outputs merge, a consumed slot is
  reused, non-stackable outputs require free slots, and insufficient capacity rejects atomically.
  This also closes the earlier stacked-nihil-shard failure mode.
- Added three cache-aware scenarios for all thirteen identity/dispatch contracts, no-level/no-Druidic
  behavior, zero XP, stack-to-stack and stack-to-non-stack capacity. Focused tests and the complete
  57-test Herblore suite pass. The manifest now tracks 207 unique groups: 48 discovered, 45
  researched, 1 unit-tested, 112 integration-tested and 1 verified. No server or RuneLite process
  was launched; the user's running server was untouched.
- The Pirate Pete crab/kelp/cod routes remain deferred until their exact partial-subquest state is
  pinned. Exact crushing messages, sound, restart presentation and packet order remain observational.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 787 actionable Gradle tasks with zero failures in 4 minutes 51 seconds (110 executed, 6 from
  cache and 671 up-to-date).

### Module integration suites

Run the root unit tests and these cache-aware suites:

```shell
./gradlew test \
  :api:cache:integration \
  :api:combat:combat-formulas:integration \
  :api:combat:combat-scripts:integration \
  :api:game-process:integration \
  :api:inv-plugin:integration \
  :api:invtx:integration \
  :api:player:integration \
  :api:repo:integration \
  :api:shops:integration \
  :api:stats:xpmod:integration \
  :content:interfaces:bank:integration \
  :content:interfaces:combat-tab:integration \
  :content:interfaces:emotes:integration \
  :content:skills:crafting:amethyst-cutting:integration \
  :content:skills:crafting:battlestaves:integration \
  :content:skills:crafting:custom-fur-clothing:integration \
  :content:skills:crafting:glassmaking:integration \
  :content:skills:crafting:hydra-leather:integration \
  :content:skills:crafting:hunter-pouches:integration \
  :content:skills:crafting:jewellery:integration \
  :content:skills:crafting:leather-addons:integration \
  :content:skills:crafting:leather-shields:integration \
  :content:skills:crafting:leatherworking:integration \
  :content:skills:crafting:lunar-clothing:integration \
  :content:skills:crafting:specialist-leatherworking:integration \
  :content:skills:crafting:tanning:integration \
  :content:skills:crafting:pottery:integration \
  :content:skills:crafting:spinning:integration \
  :content:skills:crafting:weaving:integration \
  :content:skills:crafting:wolfbone-arrowtips:integration \
  :content:skills:firemaking:integration \
  :content:skills:herblore:integration \
  :content:skills:fletching:ballista-assembly:integration \
  :content:skills:fletching:battlestaff-fletching:integration \
  :content:skills:fletching:bow-stringing:integration \
  :content:skills:fletching:ammunition:integration \
  :content:skills:fletching:bolt-tip-cutting:integration \
  :content:skills:fletching:crossbow-assembly:integration \
  :content:skills:fletching:log-cutting:integration \
  :content:skills:fletching:ogre-arrow-shafts:integration \
  :content:skills:fletching:toxic-blowpipe:integration \
  :content:skills:magic:spell-attacks:integration \
  :content:skills:magic:spell-utility:integration \
  :content:skills:prayer:remains:integration \
  :content:skills:smithing:smelting:integration \
  :content:skills:woodcutting:integration \
  :content:travel:canoe:integration
```

## 2026-09-10 — Suqah-tooth delayed crushing

- Pinned final pre-cutoff ground-tooth revision 14932139 and Suqah-tooth revision 14424497. The
  recipe uniquely delays its first result by one tick, follows with a two-tick continuation and
  then repeats every three ticks, producing results at ticks 1, 3 and 6 rather than using either
  existing immediate crushing loop.
- Added exact cache objects 9079 and 9082, a dedicated typed queue and held-use route in both orders.
  Each non-stackable tooth is atomically replaced in its own slot; loss of the pestle before any
  delayed result cancels safely and all Herblore production queues replace one another.
- Applied the members pestle rule with zero Herblore level, XP or Druidic Ritual requirement. The
  cutoff Wiki and Quest Helper locate legitimate acquisition/use during Lunar Diplomacy but do not
  state a processing-state gate, so possession remains sufficient during and after the quest.
- Added four cache-aware integration scenarios for identity/dispatch, exact cadence, both orders,
  quest-state-zero and level-one possession, zero XP, free-world rejection and missing-tool safety.
  The focused scenarios and complete 61-test Herblore suite pass. The manifest now tracks 208 unique
  groups: 48 discovered, 45 researched, 1 unit-tested, 113 integration-tested and 1 verified. No
  server or RuneLite process was launched; the user's running server was untouched.
- Exact success/failure text, audio, restart presentation and packet/inventory ordering remain
  matching-client observations.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 787 actionable Gradle tasks with zero failures in 4 minutes 53 seconds (110 executed, 6 from
  cache and 671 up-to-date).

## 2026-09-10 — Standard meat-and-fish Cooking core

- Pinned final pre-cutoff Cooking revision 14978581, burn-level revision 14976624 and skilling
  success-rate revision 14756461. They establish the cache-233 source modifiers, exact endpoint
  curves, visible-level stat-random formula, skill requirements, XP, membership and common batch
  interaction.
- Added the Cooking plugin's first substantive production slice using the repository's injected
  module/script/service structure, typed object/location/sequence/varbit references, player queue
  and atomic inventory transaction APIs. Fourteen recipes cover raw chicken, beef and the standard
  fish progression from shrimps through anglerfish.
- Registered item-use routes on the ordinary fire, generic range, Lumbridge range and both Hosidius
  ranges, plus left-click `Cook` on all ranges. A single selected raw item bypasses the chooser;
  batches use the native skill-multiple interface and produce results at ticks 1, 4, 8 and every
  four ticks thereafter.
- Added exact fire/range/Lumbridge/Hosidius success endpoints, Easy Diary access, elite Diary
  improvement, cooking-gauntlet rules including the swordfish Hosidius exception, and burn
  prevention from both Cooking capes and the max cape. Successful and burnt conversions are
  atomic, XP is success-only, source animations are cache backed, and distance/input/action
  replacement interruption is safe.
- Added six cache-aware integration scenarios pinning every object identity, requirement, source,
  sequence and dispatch key; direct-single behavior; 1/3/4 cadence; success/burn atomicity; all
  modifier branches; cape, diary, membership, boosted-level and movement rules. The focused suite
  passes. Exact UI action mode, messages, audio, packet ordering, broader appliance coverage and
  matching-client behavior remain open, and neither RuneLite nor the running server was touched.
- The complete serialized repository gate (`spotlessCheck`, manifest validation, unit tests and
  cache-aware integration tests) passes all 790 actionable tasks with zero failures in 5 minutes
  5 seconds (113 executed, 6 from cache and 671 up-to-date).

## 2026-09-10 — Cache-wide Cooking appliance registration

- Inventoried every cache-233 location operation and found exactly 48 location types exposing
  `Cook`: 41 ordinary operation-1 ranges, three operation-2 POH fancy-range states, Lumbridge, both
  Hosidius ranges and permanent fire 43475. Kept item-use-only heat sources outside this proven
  cache-operation set for a later independent audit.
- Added typed references for the complete catalogue and registered raw-item use on every source.
  First- and second-operation routes are registered separately, while source policy preserves the
  Lumbridge and Hosidius modifiers and treats 43475 as a fire. This data list is directly compared
  with the decoded cache in an integration test, so later cache additions or removals fail loudly.
- Corrected the Lumbridge range access gap. Cache quest row 17 identifies varp 29 and completion
  state 2; the range now rejects both direct and left-click Cooking until Cook's Assistant is
  complete. The functional rejection text remains client-observational.
- Added two cache-aware scenarios covering exact appliance identity and operation position, all
  operation and raw-item dispatch registrations, quest metadata, pre-completion atomic rejection
  and post-completion success. The complete focused Cooking suite passes eight tests. No server or
  RuneLite process was launched, and the user's running server remains untouched.
- The complete serialized repository gate (`spotlessCheck`, `validateCache233Parity`, `test` and
  `integration`) passes all 790 actionable tasks with zero failures in 5 minutes 2 seconds: 52
  executed, six restored from cache and 732 up-to-date.

## 2026-09-10 — Legacy rabbit, ugthanki, snail and lava-eel Cooking

- Pinned final pre-cutoff Cooked rabbit 14787730, Ugthanki meat 14787727, Thin snail meat
  14787732, Lean snail meat 14787736, Fat snail meat 14787738 and Lava eel 14951861 revisions.
  Together they supply exact levels, XP, membership, source curves and ordinary 1/3/4 timing.
- Added typed cache identities and six standard routes. Rabbit and ugthanki failures become burnt
  meat, all three snail failures become burnt snail, and lava eel explicitly has no burnt output
  and always succeeds. The reusable recipe model now represents that no-failure contract without a
  fabricated object.
- Added a cache-aware scenario covering all source endpoint pairs, standard cadence, successful and
  forced-burn outputs, XP, one-below-level and free-world rejection, plus forced-high-roll lava-eel
  success. The focused Cooking suite passes fifteen tests. Thin-snail diary credit remains assigned
  to the shared diary-task gap, while lava eel remains possession-based instead of receiving an
  unsupported Heroes' Quest Cooking gate.
- Researched the separate Chompy and jubbly spit-roast systems and recorded their source, item,
  return, quest and distinct timing requirements without forcing them through the ordinary range
  pipeline. No server or RuneLite process was launched, and the user's running server remains
  untouched.
- The complete serialized repository gate (`spotlessCheck`, `validateCache233Parity`, `test` and
  `integration`) passes all 790 actionable tasks with zero failures in 5 minutes 15 seconds: 114
  executed, six restored from cache and 670 up-to-date.

## 2026-09-10 — Hunter-meat Cooking

- Pinned nine final pre-cutoff cooked Hunter-meat revisions spanning wild and barb-tailed kebbit,
  larupia, graahk, kyatt, pyre fox, sunlight antelope, dashing kebbit and moonlight antelope. Each
  supplies boosted level, XP, membership, exact source curves and four-tick duration.
- Added 22 typed cache identities and nine raw/cooked/burnt recipes. All use 4/4/4 result timing;
  kyatt preserves its range/gauntlet 30/310 curve and the explicit no-stacking rule at Hosidius.
- Expanded the exact cache contract and added one cache-aware scenario covering 45 source pairs,
  all outputs and burns, every XP award, one-below-level and free-world failures, gauntlet behavior
  and a three-result timing trace. The focused Cooking suite passes fourteen tests. Delayed healing
  from eating these foods remains a separately named item/combat gap. No server or RuneLite process
  was launched, and the user's running server remains untouched.
- The complete serialized repository gate (`spotlessCheck`, `validateCache233Parity`, `test` and
  `integration`) passes all 790 actionable tasks with zero failures in 5 minutes 15 seconds: 114
  executed, six restored from cache and 670 up-to-date.

## 2026-09-10 — Rainbow fish, cave eel and dark crab Cooking

- Pinned final pre-cutoff Rainbow fish 14875737, Cave eel 14703267 and Dark crab 14870790
  revisions. They supply all boosted requirements, XP, membership and fire/range/Hosidius success
  endpoints; rainbow/cave use the standard quick first result while dark crab explicitly takes
  four ticks.
- Added nine typed cache identities and three transactional raw/cooked/burnt routes. All remain
  members-only and gauntlet-unaffected; dark crab uses recipe-owned 4/4/4 timing while existing
  standard foods retain 1/3/4 timing.
- Extended the exact cache contract and added a focused scenario covering all 15 source curves,
  gauntlet non-effect, every success and forced burn, XP, free-world rejection and dark crab's
  three-result cadence. The focused Cooking suite passes thirteen tests. The Wilderness elite
  catch-and-cook task remains assigned to the absent shared diary-task layer. No server or RuneLite
  process was launched, and the user's running server remains untouched.
- The complete serialized repository gate (`spotlessCheck`, `validateCache233Parity`, `test` and
  `integration`) passes all 790 actionable tasks with zero failures in 5 minutes 17 seconds: 114
  executed, six restored from cache and 670 up-to-date.

## 2026-09-10 — Additional ordinary-fish Cooking

- Pinned final pre-cutoff Mackerel 14918373, Cod 14702667, Pike 14992111, Cooked slimy eel
  14774716, Bass 14918367, Sea turtle 14747712 and Manta ray 14952302 revisions. Every page
  supplies the exact boosted requirement, XP, membership and full fire/range/special-range success
  chart; the timing tabs corroborate the existing standard one/three/four-tick pipeline.
- Added 21 typed cache identities and seven raw/cooked/burnt routes. Mackerel, cod, slimy eel, bass,
  sea turtle and manta ray retain members gates while pike is free-to-play; cod keeps its distinct
  fire/range/Lumbridge curves and sea turtle/manta retain their level-99 burn chance.
- Extended the exact cache contract and added a scenario pinning all 35 source endpoint pairs,
  cooking-gauntlet non-effect, every success/burn output, XP rule and free/member boundary. The
  complete focused Cooking suite passes twelve tests. The missing Ardougne elite manta task is
  recorded against the absent diary-task layer instead of inventing Cooking-owned state. No server
  or RuneLite process was launched, and the user's running server remains untouched.
- The complete serialized repository gate (`spotlessCheck`, `validateCache233Parity`, `test` and
  `integration`) passes all 790 actionable tasks with zero failures in 5 minutes 10 seconds: 114
  executed, six restored from cache and 670 up-to-date.

## 2026-09-10 — Ordinary meat aliases and deliberate burning

- Pinned final pre-cutoff Cooked meat 14980684, Cooked mystery meat 14937167, Burnt meat 14973773,
  Burnt chicken 14875954 and chat-message catalogue 14777106 revisions. They separate the five
  omitted raw-meat aliases and two exact-cadence deliberate meat transforms from cooked chicken,
  whose repeat timing is not published and remains observational.
- Added typed cache references and standard recipes for raw rat 2134, bear 2136, boar 25833, yak
  10816 and mystery meat 24782. They share the proven level-1 curves and 1/3/4 transactional queue;
  the first three are free-to-play, while yak and mystery meat retain their members gates.
- Added cooked meat 2142 and cooked mystery meat 24785 as explicit zero-XP deliberate transforms
  to burnt meat 2146. They dispatch on every registered fire/range source, bypass random success
  and accidental-burn/cape policy, retain atomic full-inventory replacement and emit the sourced
  deliberate-burn message.
- Expanded the cache contract and added two behavioral scenarios for every alias, free/member
  boundaries, exact outputs and XP, direct item-use dispatch, a full inventory and the exact 1/3/4
  deliberate cadence. A third regression collapses same-output meat choices and bounds the range
  menu to clientscript 2046's ten products instead of throwing when the inventory contains more.
  The focused Cooking suite passes eleven tests. No server or RuneLite process was launched, and
  the user's running server remains untouched.
- The complete serialized repository gate (`spotlessCheck`, `validateCache233Parity`, `test` and
  `integration`) passes all 790 actionable tasks with zero failures in 5 minutes 9 seconds: 114
  executed, six restored from cache and 670 up-to-date.

## 2026-09-10 — Oily fishing-rod conversions

- Pinned final pre-cutoff ordinary/pearl oily-rod revisions 14985565/14935072. Ordinary rod is an
  immediate transform; pearl rod takes one tick. Both are members-only, zero XP and require no
  Heroes' Quest state; neither source provides an animation.
- Added exact objects 307/1582/1585/22846/23122, queue 81 and a two-recipe service with both orders,
  atomic rod-slot replacement and exact-slot revalidation for the delayed pearl result.
- Added four scenarios covering identities/dispatch, timing, both orders, state-zero/level-one
  possession, membership, full inventory and stale input slots. Focused tests and the complete
  82-test Herblore suite pass.
- The complete repository gate (`spotlessCheck`, manifest validation, unit tests and integration
  tests) passes: 787 actionable tasks in 4m 54s (110 executed, 6 from cache and 671 up-to-date).

## 2026-09-10 — Blamish-oil Herblore

- Pinned final pre-cutoff blamish-oil revision 14719670: harralander potion (unf) plus blamish slime,
  members, boosted level 25, 80 XP, two ticks and an explicit no-Heroes'-Quest requirement.
- Added cache object 1582 and Heroes' Quest varp 188, then registered the recipe through the native
  finished-potion menu and queue for both-order, atomic, resource-safe two-tick batching.
- Extended the exact recipe contract and added a focused boosted-level/state-zero boundary scenario.
  Focused finished-potion tests and the complete 78-test Herblore suite pass.
- The complete repository gate (`spotlessCheck`, manifest validation, unit tests and integration
  tests) passes: 787 actionable tasks in 4m 52s (110 executed, 6 from cache and 671 up-to-date).

## 2026-09-10 — Blamish-snail-slime processing

- Pinned final pre-cutoff blamish-snail-slime revision 14462907: thin, lean or fat raw snail plus
  sample bottle and pestle produce slime immediately, followed by automatic results after one, two
  and then three ticks. The source defines membership but no skill, XP, Druidic or quest-state gate.
- Added exact objects 233/1581/3363/3365/3367/3377, queue 80 and a typed same-snail continuation
  service. Both orders work; each result atomically consumes the selected/matching snail and first
  bottle, places slime in the bottle slot and revalidates every resource before continuation.
- Added four cache-aware scenarios for identities and all dispatch keys, exact 0/1/2/3 cadence, all
  snail types, both orders, level-one and quest-zero possession, membership, full inventory,
  pestle loss and zero XP. Focused tests and the complete 77-test Herblore suite pass.
- The complete repository gate (`spotlessCheck`, manifest validation, unit tests and integration
  tests) passes: 787 actionable tasks in 4m 52s (110 executed, 6 from cache and 671 up-to-date).

## 2026-09-10 — Black-dye mushroom and vial processing

- Pinned final pre-cutoff black-dye revision 14842486, which defines pestle-on-black-mushroom with
  an empty vial as an immediate members recipe and explicitly requires starting The Golem. Cache
  varp 437 and pre-cutoff Quest Helper state mapping pin the first valid state at 1.
- Added exact objects 229/233/4620/4622 and a dedicated two-material transactional service. It
  consumes the selected mushroom and first empty vial atomically, places dye in the vial slot,
  supports both pestle/mushroom orders, uses sequence 364 and cancels stale Herblore work.
- Added four cache-aware scenarios for identity/dispatch, state 0/1, missing vial, both orders,
  full-inventory atomicity, member rejection, zero Herblore level/XP and no Druidic gate. Focused
  tests and the complete 73-test Herblore suite pass. The manifest now tracks 211 unique groups: 48
  discovered, 45 researched, 1 unit-tested, 116 integration-tested and 1 verified. Exact feedback,
  material/slot precedence, audio and packet ordering remain observational. No server or RuneLite
  process was launched, and the user's running server remains untouched.
- The complete repository gate (`spotlessCheck`, manifest validation, unit tests and integration
  tests) passes: 787 actionable tasks in 4m 53s (110 executed, 6 from cache and 671 up-to-date).

## 2026-09-10 — Raw and cooked karambwanji paste

- Pinned final pre-cutoff karambwanji-paste revision 14970675. Raw karambwanji is a zero-delay
  manual action with uniform three-tick automatic continuation, while the cooked tab defines a
  separate immediate transform without a sourced automatic cadence.
- Added cache pairs 3150→3155 and 3151→3156. Raw input is stackable and its output is not, so a
  dedicated queue validates destination capacity before every atomic result and reuses the selected
  slot only for the final unit. Cooked input/output are non-stackable and use the one-shot table.
- Both routes support either item order, require pestle 233 and membership, award zero Cooking and
  Herblore XP, have no Druidic Ritual or invented Tai Bwo Wannai Trio progress gate, and replace all
  other Herblore work safely.
- Added four dedicated cache-aware scenarios and extended the one-shot contract test for identities,
  stackability, dispatch, raw 0/3/3 cadence, both orders, level-one/quest-state-zero possession,
  capacity rejection/recovery, tool loss, free-world rejection and zero XP. Focused tests and the
  complete 69-test Herblore suite pass. The manifest now tracks 210 unique groups: 48 discovered,
  45 researched, 1 unit-tested, 115 integration-tested and 1 verified. Cooked automatic behavior
  and exact presentation remain observational. No server or RuneLite process was launched, and the
  user's running server remains untouched.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 787 actionable Gradle tasks with zero failures in 4 minutes 53 seconds (110 executed, 6 from
  cache and 671 up-to-date).

## 2026-09-10 — Immediate one-shot quest ingredient crushing

- Pinned final pre-cutoff cinnamon revision 14419819, Tchiki nut paste revision 14424608 and
  mysterious crushed meat revision 14422614. Each recipe specifies a zero-tick selected action,
  members access and no automatic continuation cadence.
- Added cache pairs 771→7472, 7573→7575 and 22403→22404 to a shared typed one-shot table with one
  plugin service and both-order held-item registration. The selected input is replaced atomically
  even in a full inventory and the immediate action cancels stale Herblore queues.
- Kept input acquisition in Freeing Sir Amik Varze, Freeing King Awowogei and A Taste of Hope. No
  unrelated Druidic Ritual, Herblore-level or XP rule is applied; hidden quest-state checks and
  injected duplicate behavior remain explicit matching-client observations.
- Added four cache-aware scenarios covering all identities and dispatch keys, both orders, immediate
  timing, one selected result with injected duplicates, level-one/quest-state-zero possession, full
  inventories, zero XP and free-world rejection. Focused tests and the complete 65-test Herblore
  suite pass. The manifest now tracks 209 unique groups: 48 discovered, 45 researched, 1
  unit-tested, 114 integration-tested and 1 verified. No server or RuneLite process was launched,
  and the user's running server remains untouched.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 787 actionable Gradle tasks with zero failures in 4 minutes 53 seconds (110 executed, 6 from
  cache and 671 up-to-date).

## 2026-09-10 — Chompy iron-spit and fire Cooking

- Pinned final pre-cutoff Cooked chompy 14982500, Skewered chompy 14995319, Raw chompy 14995318
  and Iron spit 14995320 revisions. Together they define immediate zero-XP preparation, boostable
  Cooking 30 and Firemaking 20 requirements, fire-only Cooking, a three-tick result, 100 Cooking
  XP on success, 200/255 endpoints, level-99 no-burn behavior and returned-tool outputs.
- Added typed cache references for raw/cooked/ruined/skewered chompy 2876/2878/2880/7230, iron
  spit 7225 and sequence 1016. A dedicated Cooking service supports both item-use orders, exact
  selected-slot revalidation, ordinary and permanent cache fires, success/burn transactions,
  Cooking-cape/max-cape protection and shared replacement of standard Cooking work.
- Kept direct raw-chompy ogre-spit cooking separate and researched. Cache multi-location 6895,
  player varbit 1770 and its nine empty/raw/cooked/burnt Chompy/chicken/jubbly morphs prove that
  this route needs an interruption-safe per-player roast state machine rather than reuse of the
  inventory-only fire queue. Jubbly state 110 is date-pinned by Quest Helper commit `fc4e126`,
  while cache DB row 2313 pins subquest completion at 170.
- Added four cache-aware scenarios covering exact identities and dispatch, both item orders,
  boostability, membership and both skill boundaries, the exact three-tick result, success and
  forced burn, 100/zero XP, spit return, full inventory, cape and level-99 protection, movement and
  stale selected-slot interruption. The focused Cooking suite passes all 19 tests. Exact messages,
  audio and packet ordering remain matching-client observations. The manifest now tracks 225
  unique groups: 48 discovered, 48 researched, 1 unit-tested, 127 integration-tested and 1
  verified. RuneLite was not opened, and the running server was not touched.
- The serialized repository-wide formatting, parity, unit and cache-aware integration gate passed
  all 790 actionable Gradle tasks with zero failures in 5 minutes 11 seconds (112 executed, 6 from
  cache and 672 up-to-date).

## 2026-09-10 — Ogre spit-roast Cooking

- Completed all three inputs accepted by cache-233 ogre spit roast 6895: raw chompy 2876, raw
  chicken 2138 and raw jubbly 7566. The typed recipe table preserves their distinct boostable
  Cooking requirements, XP, success curves, no-burn boundaries, membership and jubbly state-110
  quest gate. Results are cooked/ruined chompy 2878/2880, cooked/burnt chicken 2140/2144 and
  cooked/burnt jubbly 7568/7570.
- Added the cache-native visual contract: player varbit 1770 drives empty state 0, Chompy states
  1/2/3, chicken states 4/5 with its burn deliberately sharing state 3, and jubbly states 6/7/8.
  Base roast sequence 1017 and player place/remove sequences 3181/3182 are typed and pinned.
  Chompy resolves after five ticks; chicken and jubbly after six; the cooked/burnt morph remains
  for one tick, giving jubbly its seven-tick manually restarted cadence.
- Added weak queue 84 and soft timer 12 as a dedicated per-player lifecycle. The timer preserves a
  raw morph only while the queue remains active, then cleans cancellation on the following tick;
  result morphs self-clear after one tick. Movement, other Cooking work, logout, stale selected
  slots and capacity races cannot leave a reserved roast or partially commit inventory/XP. The
  three Cooking/max capes prevent burns while chicken still uses its ordinary level-34 no-burn
  boundary.
- Added six ogre-spit scenarios to the cache-aware Cooking suite, bringing it to 25 passing tests.
  They pin all objects, locations, states, sequences, dispatch registrations, result ticks,
  endpoints, XP, gates, full-inventory transactions, cape behavior and interruption/session
  cleanup. Exact presentation remains deliberately bounded: current messages are matching-client
  observations, while sound identity, packet ordering and the quest-specific seasoned-chompy path
  remain open. RuneLite was not opened and the user's running server was not touched.
- The manifest now tracks 226 unique groups: 48 discovered, 46 researched, 1 unit-tested, 130
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 3 seconds (114 executed, 6 from cache and 670 up-to-date).

## 2026-09-10 — Range-baked bread and pitta bread

- Pinned final pre-cutoff Bread 14918368, Bread dough 14867365, Pitta bread 14982496 and Pitta
  dough 14871428 revisions. They define two range-only recipes with exact cache items, one-tick
  manual/four-tick Make-X behavior, 40 XP, free/member boundaries and boostable requirements.
- Extended the typed Cooking recipe model with source eligibility. Range operations and direct item
  use now expose recipes only on valid sources, while production independently rechecks the source.
  Existing meat, fish and stew candidates retain fire support; bread dough 2307 and pitta dough
  1863 are registered only on ordinary, Lumbridge and Hosidius ranges.
- Bread 2309/burnt bread 2311 uses the cutoff ordinary, Lumbridge and Hosidius endpoint curves and
  the existing 1/3/4 transactional cadence. Pitta bread 1865 is members-only at boosted level 58;
  its theoretical burn boundary is already level 37, so it always succeeds and never creates the
  otherwise present burnt-pitta cache object 1867.
- Added one cache-aware scenario plus expanded the complete recipe/dispatch contract, bringing the
  focused Cooking suite to 26 passing tests. Coverage includes IDs, curves, source sets, success,
  burn, exact cadence, XP, membership, boosts and unchanged/default feedback on fires. Flour-water
  dough preparation, exact presentation, audio and packet ordering remain open. RuneLite was not
  opened and the user's running server was not touched.
- The manifest now tracks 227 unique groups: 48 discovered, 46 researched, 1 unit-tested, 131
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 2 seconds (112 executed, 6 from cache and 672 up-to-date).

## 2026-09-10 — Stew heat stage

- Pinned final pre-cutoff Stew 14882114, Uncooked stew 14928848 and Burnt stew 14419629 revisions.
  They define uncooked/stew/burnt objects 2001/2003/2005, boostable Cooking 25, 117 success-only XP,
  fire and range support, every source endpoint, explicit 1/3/4 cadence and ordinary level-58
  no-burn behavior.
- Added stew to the shared typed source-aware Cooking table. It is available from every registered
  fire and range, including permanent, Lumbridge and Hosidius variants, and retains atomic queue,
  cape, movement, capacity and source-state handling without introducing a one-off path.
- Added one cache-aware scenario and expanded the whole-table cache/dispatch contract, bringing the
  focused Cooking suite to 27 passing tests. It covers forced burn then success timing, both source
  classes, all endpoint pairs, boosts, free-world behavior, level rejection, level-58 and max-cape
  burn prevention, and full-inventory same-slot replacement.
- Kept uncooked-stew assembly separate: the two incomplete states, potato/water/meat/chicken order,
  and spice/curry branch require their own transactional item graph. Exact presentation, audio and
  packet ordering also remain open. RuneLite was not opened and the user's running server was not
  touched.
- The manifest now tracks 228 unique groups: 48 discovered, 46 researched, 1 unit-tested, 132
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes (112 executed, 6 from cache and 672 up-to-date).

## 2026-09-10 — Curry heat stage

- Pinned final pre-cutoff Curry 14703279, Uncooked curry 14891634, Burnt curry 14419630 and
  Cooking/Burn level 14976624 revisions. They define uncooked/cooked/burnt objects 2009/2011/2013,
  boostable members-only Cooking 60, 280 success-only XP, fire and range support, every source
  endpoint, standard four-tick production and ordinary level-74 no-burn behavior.
- Added curry to the shared typed source-aware Cooking table with 38/332 ordinary and Lumbridge,
  50/344 Hosidius and 63/357 elite endpoints. It retains the shared atomic queue, movement,
  capacity, cape and source-state protections without a special-case execution path.
- Added one cache-aware scenario and expanded the whole-table cache/dispatch contract, bringing the
  focused Cooking suite to 28 passing tests. It covers forced burn then success timing, both source
  classes, all endpoint pairs, membership, boosts, full-inventory replacement, level rejection and
  level-74 guaranteed success.
- Kept curry preparation separate: one spice or three curry leaves plus uncooked stew is its own
  zero-XP two-tick item-use graph. Exact presentation, audio and packet ordering also remain open.
  RuneLite was not opened and the user's running server was not touched.
- The manifest now tracks 229 unique groups: 48 discovered, 46 researched, 1 unit-tested, 133
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 19 seconds (114 executed, 6 from cache and 670 up-to-date).

## 2026-09-10 — Baked-potato heat stage

- Pinned final pre-cutoff Baked potato 14889126, Potato 14943915 and Burnt potato 14799746
  revisions. They define potato/baked/burnt objects 1942/6701/6699, a boostable members-only
  Cooking-7 action over a free-to-play raw object, 15 success-only XP, strict range-only support,
  explicit 1/3/4 result cadence and ordinary level-41 no-burn behavior.
- Added the route to the shared typed source-aware Cooking table with 108/472 ordinary and
  Lumbridge, 120/484 Hosidius and 133/497 elite endpoints. Exact cache symbols `potato_baked` and
  `potato_burnt` are bound behind readable API references.
- Added one cache-aware scenario and expanded the whole-table cache/dispatch contract, bringing the
  focused Cooking suite to 29 passing tests. It covers forced burn then success, source rejection,
  all curves, action-versus-object membership, boosts, full-inventory replacement, level rejection
  and level-41 guaranteed success.
- Potato toppings and exact presentation, audio and packet ordering remain open. RuneLite was not
  opened and the user's running server was not touched.
- The manifest now tracks 230 unique groups: 48 discovered, 46 researched, 1 unit-tested, 134
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 19 seconds (114 executed, 6 from cache and 670 up-to-date).

## 2026-09-10 — Ordinary vegetable Cooking heat family

- Pinned twelve final pre-cutoff item revisions for uncooked/success/burnt scrambled egg,
  sweetcorn, onion and mushroom routes. The typed table now carries all twelve cache objects,
  boostable members-only levels 13/28/42/46, XP 50/104/60/60, every ordinary and Hosidius endpoint,
  and no-burn levels 48/54/77/90.
- Extended the shared Cooking queue to support a genuine zero-tick first result. Scrambled egg now
  resolves immediately and schedules later results at +3/+4 gaps; every existing recipe retains
  its prior queue-handler compensation and timing. Sweetcorn, onions and mushrooms use 1/3/4.
- Added a cache-aware family scenario and expanded the whole-table cache/dispatch contract,
  bringing the focused Cooking suite to 30 passing tests. Every route covers forced burn and
  success, boosted and rejected levels, membership, exact source curves, no-burn boundaries and
  cache identities; sweetcorn additionally pins full-inventory same-slot replacement.
- Recorded the source presentation conflict: specific item prose and the cutoff combined burn
  table accept fire and range, while recipe template facility fields often list only a range.
  Fire/range support follows the specific behavior evidence; live client confirmation remains a
  manual acceptance item. Exact messages, audio and packet ordering also remain open. RuneLite was
  not opened and the user's running server was not touched.
- The manifest now tracks 231 unique groups: 48 discovered, 46 researched, 1 unit-tested, 135
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 18 seconds (119 executed, 6 from cache and 665 up-to-date).

## 2026-09-10 — Standard pie baking family

- Pinned the final pre-cutoff revisions for redberry, meat, mud, apple, garden, fish, botanical,
  mushroom, admiral, dragonfruit, wild and summer pies plus Cooking/Burn level 14976624. Together
  they define all 12 raw/cooked cache pairs, shared burnt pie 2329, boosted levels, XP, membership,
  strict range-only use, source curves, no-burn boundaries and 1/3/4 result timing.
- Added all 25 identities as typed references and a data-driven pie family in the shared Cooking
  table. Ordinary, Lumbridge, Hosidius and elite-diary curves remain distinct; dragonfruit is safe
  only in Hosidius at 97/92, while wild and summer remain burnable at 99 without a Cooking cape.
- Added one comprehensive cache-aware scenario and expanded the whole-table contract, bringing the
  focused Cooking suite to 31 passing tests. It exercises every recipe's cache identity, source
  curves, dispatch, membership, boosted and rejected levels, forced burn/success, natural no-burn
  class and fire rejection, plus diary selection, cape immunity and full-inventory replacement.
- Pie-shell/filling assembly, Lunar Bake Pie, exact production messages, audio and packet ordering
  remain separate work. RuneLite was not opened and the user's running server was not touched.
- The manifest now tracks 232 unique groups: 48 discovered, 46 researched, 1 unit-tested, 136
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 23 seconds (114 executed, 6 from cache and 670 up-to-date).

## 2026-09-10 — Remaining standard baked-goods heat stages

- Pinned final pre-cutoff Cake 14918369, Uncooked cake 14424994, Burnt cake 14419624, Plain pizza
  14734873, Uncooked pizza 14734874, Burnt pizza 14419623 and Cooking/Burn level 14976624
  revisions. They define both free-world, range-only heat routes, exact objects, boostable levels
  40/35, success XP 180/143, all source curves, no-burn boundaries and 1/3/4 result timing.
- Added both recipes to the shared typed Cooking table. Cake returns tin 1887 on successful and
  burnt results through a reusable optional-container field; the engine validates the additional
  output slot before every result and stops a batch atomically when capacity is exhausted. Pizza
  retains ordinary one-for-one full-inventory replacement.
- Added one comprehensive cache-aware scenario and expanded the whole-table contract, bringing the
  focused Cooking suite to 32 passing tests. It covers all seven cache identities, forced
  burn/success, success-only XP, F2P and boosted-level behavior, range-only routing, every endpoint,
  ordinary/Hosidius no-burn levels and their preceding burnable levels, cape immunity, returned
  tins and full/near-full inventory boundaries.
- Cake and pizza assembly, toppings, exact production messages, audio, packet ordering, cake
  chooser capacity and matching-client capacity feedback remain separate work. RuneLite was not
  opened and the user's running server was not touched.
- The manifest now tracks 233 unique groups: 48 discovered, 46 researched, 1 unit-tested, 137
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 18 seconds (119 executed, 6 from cache and 665 up-to-date).

## 2026-09-10 — Ordinary dough, cake and pizza preparation

- Pinned final pre-cutoff revisions for pizza base, bread/pastry/pitta dough, cake tin, uncooked
  cake, incomplete/uncooked pizza, chocolate cake and meat/anchovy/pineapple pizza, plus the dated
  production chat-message catalogue. Together they define 24 concrete recipes, exact cache
  identities, boostable requirements, membership, XP, returned containers and one/two-tick result
  timing.
- Added typed references and a data-driven preparation service on queue 85. Pot flour with
  bucket/bowl/jug water opens the cache generic skill-multi interface for bread, pastry, pizza or
  pitta dough and atomically returns both containers. Cake assembly accepts tin use with any of its
  three ingredients, consumes all four inputs after two ticks, returns pot/bucket and emits the
  sourced result message. Tomato must precede cheese for pizza, and all ordinary chocolate, meat,
  anchovy and pineapple topping variants are exact-item handlers.
- Added five cache-aware scenarios, bringing the focused Cooking suite to 37 passing tests. They
  cover all 24 recipes, 15 unique held-item registrations, both use orders, chooser payload,
  boosts, F2P/member restrictions, XP, result boundaries, full-inventory behavior, exact container
  return and stale selected/additional-slot cancellation. Cheese-first pizza and lookalike meats
  deliberately have no recipe registration.
- The exact production dough-menu title/order, non-cake result messages, failure precedence,
  output-slot ordering, animation, sound and packet ordering remain a documented matching-client
  acceptance boundary. RuneLite was not opened and the user's running server was not touched.
- The manifest now tracks 234 unique groups: 48 discovered, 46 researched, 1 unit-tested, 138
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 10 seconds (114 executed, 6 from cache and 670 up-to-date).

## 2026-09-10 — Stew and curry preparation

- Pinned the final pre-cutoff Uncooked stew 14928848, Incomplete stew 14928840, Stew 14882114,
  Uncooked curry 14891634, Curry 14703279, Spice 14915541, Curry leaf 14463589, Bowl of water
  14861229 and Potato 14943915 revisions. Together they define both incomplete-stew identities,
  six F2P stew routes, both members curry routes, exact objects, boostable levels 25/60, zero XP,
  two-tick results, three-leaf quantity and the no-knife contract.
- Added cache-typed references for incomplete stew 1997/1999, spice 2007 and curry leaf 5970, then
  extended the existing FoodPreparation queue with eight data-driven recipes. Bowl water accepts
  potato, cooked meat or cooked chicken; either ingredient order completes uncooked stew 2001;
  uncooked stew accepts one spice or exactly three distinct curry leaves for uncooked curry 2009.
  Both held-item orders dispatch symmetrically and every selected/additional slot is revalidated
  before one atomic commit.
- Added two cache-aware scenarios and expanded the preparation contract, bringing the focused
  Cooking suite to 39 passing tests. They cover all six stew routes and both curry routes, exact
  IDs, 32 total recipes, 23 held registrations, boosted and rejected levels, F2P/member gates,
  zero XP, two-tick timing, full-inventory replacement, two-leaf rejection and stale-third-leaf
  cancellation without partial consumption.
- Potato/meat stew messages follow the cutoff articles. Chicken wording expands their
  `meat/chicken` shorthand and remains explicitly unverified; exact curry messages, failure
  precedence, output-slot order, animation, sound and packet order still require matching-client
  capture. No unsupported audiovisual behavior was added. RuneLite was not opened and the user's
  running server was not touched.
- The manifest now tracks 235 unique groups: 48 discovered, 46 researched, 1 unit-tested, 139
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 9 seconds (113 executed, 6 from cache and 671 up-to-date).

## 2026-09-10 — Standard pie preparation

- Pinned the final pre-cutoff revision for pie shell and every intermediate/raw standard pie item,
  plus historical pie-shell, summer, wild and admiral bulk-production guides. Together they define
  26 exact zero-XP transforms: the shell, four F2P filling alternatives and every ordered mud,
  garden, fish, botanical, mushroom, admiral, dragonfruit, wild and summer stage. They also pin
  boostable levels 1/10/20/30/29/34/47/52/60/70/73/85/95, membership, ordinary cabbage rather
  than Draynor cabbage, both returned buckets and the admiral first-result timing exception.
- Added typed cache references for every new ingredient and intermediate identity, then extended
  the existing FoodPreparation recipe graph and shared queue. Each pie interaction works in either
  held-item order, opens the one-product skill-multi quantity flow bounded by the scarcest input,
  produces ordinary results after two ticks and repeats every two ticks. Admiral salmon produces
  its first result after one tick and repeats every two. Each continuation resolves fresh exact
  slots and atomically consumes inputs, returns a bucket where required and creates the output.
- Added four cache-aware scenarios and expanded the preparation contract, bringing the focused
  Cooking suite to 43 passing tests. They pin all 26 recipes, exact cache IDs, 58 total preparation
  recipes and 49 registrations; all free-world/member and boosted-level paths; both use orders;
  clientscr 2046 payload and quantity; 2/4/6 and admiral 1/3/5 results; zero XP; full-inventory
  returned-container behavior; Draynor-cabbage rejection; stale-open-menu rejection and stale-next-
  batch cancellation without partial consumption.
- The historical sources do not publish literal preparation messages, animation, sound, exact menu
  title/action, output-slot policy or packet order. Those details remain a dedicated workspace
  cache-233 RuneLite manual acceptance case and were not invented. Only local workspace RuneLite
  source was inspected; no client was launched, and the user's running rsmod server was untouched.
- The manifest now tracks 236 unique groups: 48 discovered, 46 researched, 1 unit-tested, 140
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 12 seconds (113 executed, 6 from cache and 671 up-to-date).

## 2026-09-10 — Potato toppings and prerequisite bowl mixtures

- Pinned final pre-cutoff revisions for both butter identities, all five topped potatoes, chilli
  con carne, egg-and-tomato, mushroom-and-onion, all three tuna-and-corn paths, spicy sauce and the
  seven prerequisite bowl-item pages. The 22-route graph captures every exact object, boostable
  level, XP award, members gate and one/two-tick result. It also preserves source-specific details:
  only gnome spice makes spicy sauce; not garlic butter substitutes for ordinary butter; whole
  cooked meat/tuna routes need a knife while already chopped routes do not; and only four recipes
  explicitly return an empty bowl.
- Added typed cache references for every intermediate and final identity and extended the shared
  FoodPreparation model with an optional retained tool. Knife presence is revalidated immediately
  before the result, so losing it during the delay cancels without consuming either ingredient.
  The 22 recipes use the existing symmetric held-item dispatcher, boost/member checks, shared weak
  queue and atomic inventory transaction; both chilli-con-carne paths retain their exceptional
  one-tick result while the other 20 use two ticks.
- Added four cache-aware scenarios and expanded the preparation contract, bringing the focused
  Cooking suite to 47 passing tests. They pin 80 total preparation recipes, 22 potato routes and 71
  registered pairs; exercise every route in alternating held-item order; validate exact IDs,
  levels, XP, membership, ticks, tools and returned containers; cover a boosted and rejected tuna
  boundary, F2P rejection, missing/stale knives, all four bowl returns in a full inventory, explicit
  no-return recipes and cancellation by a newer Cooking action. Ordinary spice has no spicy-sauce
  registration.
- The dated Wiki chat catalogue and local revision-matched RuneLite source expose no exact potato
  preparation feedback, animation, sound or packet ordering. Missing-knife and success wording,
  result-slot policy and audiovisual/packet sequencing remain in the dedicated manual acceptance
  case and were not guessed. The local RuneLite source was inspected but no client was launched;
  the user's running rsmod server was untouched.
- The manifest now tracks 237 unique groups: 48 discovered, 46 researched, 1 unit-tested, 141
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 13 seconds (113 executed, 6 from cache and 671 up-to-date).

## 2026-09-10 — Dairy churn production

- Pinned the last pre-cutoff Dairy churn, pot of cream, pat of butter and cheese revisions against
  cache 233.4. The resulting seven-route table preserves each source-specific level, XP and
  10/19/26-tick duration, including not-garlic butter as a valid cheese source, no empty-pot return
  from cream, and the explicitly sourced empty-bucket drop when a milk result fills the inventory.
- Added typed references for all four cache churn locations, pot of cream, the short/medium/long
  player and churn sequences, all six regular/metal map spot animations and the dedicated Cooking
  queue. A data-driven singleton now registers native `Churn` and held-item use for every valid
  source/location pair, provides source-filtered quantity choices, checks boosted level and
  membership, and atomically revalidates the selected input, distance and shared interruption state
  before every result. Milk returns its bucket to inventory or uses the protected private-owner
  ground-drop fallback when full.
- Added five cache-aware scenarios, bringing the focused Cooking suite to 52 passing tests. They pin
  all seven recipes, four location operations, three sequence tiers and six churn graphics; prove
  exact result ticks, XP and repeated batches; cover boosted and rejected levels, free-world
  rejection, stale slots, movement and newer-action cancellation; and validate both ordinary bucket
  return and the full-inventory private drop.
- Historical message searches did not expose exact cache-233 churn feedback. Older interface
  evidence supports the inherited menu title and cream/butter/cheese ordering, but exact button
  text/action, native-click priority with mixed sources, messages, sound, map-graphic placement and
  packet ordering remain in manual case 29 rather than being claimed as verified. Only the local
  workspace RuneLite source was inspected; no client was launched and the user's running rsmod
  server was untouched.
- The manifest now tracks 238 unique groups: 48 discovered, 46 researched, 1 unit-tested, 142
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 21 seconds (113 executed, 6 from cache and 671 up-to-date).

## 2026-09-10 — Grape-wine making and fermentation

- Pinned the last pre-cutoff ordinary and Zamorak unfermented/finished/bad-wine revisions and cache
  identities. Both variants now use their exact boostable levels, F2P/member split, 48/352 and
  -125/260 success endpoints, natural no-failure levels 68/98, 200 XP only on fermentation success
  and no immediate mixing XP.
- Added typed references for both grape families, all result objects, make-wine sequence 7529 and
  graphic 47, plus dedicated queue 87 and normal timer 13. The injected wine plugin registers both
  symmetric held-use pairs, starts results after three ticks and repeats every two, emits the
  historically preserved squeeze message, and resets one 20-tick fermentation timer after every
  committed jug.
- Fermentation collects both wine types across inventory and bank at once. It applies the sourced
  15-equal-batches-plus-individual-remainder algorithm using the pre-XP current level, performs one
  protected atomic mutation, and awards the combined delayed XP afterward. The shared 60,000 cap
  counts both stores and both wine types. A conservative capacity preflight leaves all wine intact
  and retries next tick if a bank stack could split into more result identities than the bank can
  hold.
- Added six cache-aware scenarios, bringing the focused Cooking suite to 58 passing tests. They pin
  IDs, registration, sequences, graphic animation, timers, curves and constants; prove 3/2/20
  timing and timer reset, exact delayed XP, 15-batch grouping, simultaneous inventory/bank
  conversion, boosted/member/no-fail boundaries, shared-cap enforcement and full-bank atomic retry.
- Exact cap feedback was not found and is deliberately not invented. The quantity presentation,
  full-bank production policy, success allocation between inventory and bank, logout persistence,
  sound and packet order remain manual case 30. Local RuneLite displays 13.8 seconds from graphic
  47 while the dated Wiki specifies 20 ticks from the latest item result; the implementation uses
  the Wiki result boundary pending matching-client packet capture. RuneLite was not launched and
  the user's running server was untouched.
- The manifest now tracks 239 unique groups: 48 discovered, 46 researched, 1 unit-tested, 143
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 22 seconds (123 executed, 6 from cache and 661 up-to-date).

## 2026-09-10 — Nettle tea and hot-water drinks

- Pinned the final pre-cutoff revisions for nettle-water, nettle tea, both milky/ordinary bowl and
  cup variants, bowl/cup hot water, empty cup and milk. The stage-specific evidence resolves zero
  XP for nettles plus bowl water and exactly 52 XP only when nettle-water heats successfully; the
  cup page's displayed 52 XP is an aggregate recipe value and is not awarded again while pouring.
- Added typed references for all fourteen cache identities and registered the complete ordinary
  production graph through the existing Cooking recipe structures. Nettle-water is a boostable
  members-only level-20 two-tick batch. Fires and every classified range heat it with the sourced
  78/412 ordinary, 90/424 Hosidius and 103/437 elite endpoints, empty-bowl failure and natural
  level-54 no-failure boundary. Bowl/cup water now heat without level or XP using the dated one-tick
  first/four-tick repeat behavior.
- Added all zero-XP finishing routes: ordinary and milky bowl tea pour into cups while returning the
  bowl; milk converts either ordinary bowl or cup tea while returning the bucket; and the Poll-82
  bowl-hot-water/empty-cup Make-All route returns every bowl. Transactions remain atomic in a full
  inventory. The shared preparation queue now distinguishes a synchronous first result from a
  queued handler so the zero-tick Make-All route does not inherit an extra compensation tick.
- Added four cache-aware scenarios, bringing the focused Cooking suite to 62 passing tests. They pin
  all identities and registrations, curves, boost/member gates, success/failure outputs, XP stage,
  no-failure boundary, source animations, exact sourced heat cadence, both item orders, quantity
  flow, zero/one/two-tick boundaries and full-inventory container preservation.
- Exact heat/pour messages, sound, packet and result-slot ordering, nettle-water menu presentation,
  nettle-tea heat cadence and the bowl-to-cup automatic repeat boundary are not stated by the dated
  pages. The implemented wording and uncertain boundaries are explicitly recorded as evidence-based
  inferences in the source ledger and manual case 31, not promoted to verified. Drinking effects,
  nettle gathering and the Ghosts Ahoy porcelain-cup handoff remain separate open slices. RuneLite
  was not launched and the user's running rsmod server was untouched.
- The manifest now tracks 240 unique groups: 48 discovered, 46 researched, 1 unit-tested, 144
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 18 seconds (115 executed, 6 from cache and 669 up-to-date).

## 2026-09-10 — Oomlie-wrap preparation and Cooking

- Pinned the last pre-cutoff raw, wrapped, cooked and burnt oomlie revisions against cache 233.4.
  The two-stage contract is boosted members-only Cooking 50: raw oomlie plus palm leaf transforms
  immediately for 10 XP, then the wrapped meat cooks for 30 XP after two ticks manually and every
  four ticks through Make-X.
- Added typed references for all six cache objects and registered symmetric held-item preparation
  plus fire and range Cooking through the existing plugin structures. The heat stage uses the exact
  106/450 fire, 112/476 range, 124/488 Hosidius and 137/501 elite-Hosidius curves.
- Added a recipe-local continuation policy for the historically documented cold-drain method. The
  action still requires level 50 to start, but an already-running oomlie batch can continue after
  the visible level falls, allowing the documented burnt-wrap outcome without changing any other
  recipe's between-result validation.
- Added two cache-aware scenarios and expanded both aggregate contracts, bringing the focused
  Cooking suite to 64 passing tests. They pin identities, registration, boost/member gates, XP,
  exact result cadence, success curves and continued drained burning.
- Raw unwrapped oomlie forced-burning remains researched: the dated page proves fire use, output and
  two messages, but not timing, repetition or range acceptance. Exact oomlie messages, menu, sound,
  slot and packet behavior remain manual case 32. RuneLite was not launched and the user's running
  server was untouched.
- The manifest now tracks 241 unique groups: 48 discovered, 46 researched, 1 unit-tested, 145
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 17 seconds (115 executed, 6 from cache and 669 up-to-date).

## 2026-09-10 — Karambwan Cooking

- Pinned the last pre-cutoff raw, cooked, poison and burnt karambwan revisions plus the contemporary
  Tai Bwo Wannai Trio and Tiadeche pages against cache 233.4. They establish the poison-only
  pre-reward route, post-reward thorough/poison choice, default thorough selection, poison warning,
  190/80 XP, one/four-tick cadence, exact source curves and burnt result.
- Added typed raw/cooked/poison/burnt references, quest/Tiadeche varps, warning and diary varbits,
  dedicated queue 88 and persistent server-only Tinsay-reward varbit 65534. The latter occupies an
  unused bit in the existing persistent generic storage varp because cache 233 has no independent
  client var for the post-quest conversation; it is documented as server state, not cache evidence.
- Added an injected karambwan service and registered raw and burnt held-item interactions for every
  classified fire/range. It implements the two-part unlock, general-range and direct-use menus,
  default cooked ordering, poison confirmation/preference, atomic success/burn conversion, exact
  XP/messages where sourced, ordinary/Hosidius/elite curves, cape immunity, successful-thorough
  diary credit and shared Cooking cancellation. The warning path retains protected access through
  its documented first-result tick before returning subsequent results to the weak queue.
- Added seven cache-aware scenarios, bringing the focused Cooking suite to 71 passing tests. They
  cover cache contracts, both unlock phases, permanent state backing, menus, warning decline and
  confirmation, disabled warnings, full-inventory replacement, both products, exact XP, curves,
  burns, capes, cadence, interruption, diary success-only behavior and burnt-object reheating.
- Exact warning-setting presentation, skill-menu padding/action mode, poison-success source
  authority, sounds and packet order remain manual case 33. RuneLite was not launched and the
  user's running rsmod server was untouched.
- The manifest now tracks 242 unique groups: 48 discovered, 46 researched, 1 unit-tested, 146
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 8 seconds (115 executed, 6 from cache and 669 up-to-date).

## 2026-09-10 — Sinew Cooking

- Pinned pre-cutoff Sinew revision 14890385 against the cache, the existing burnt-meat evidence and
  the local revision-matched RuneLite source. Raw beef, raw bear meat and raw boar meat now expose a
  members-only range choice for sinew at level 1 and 3 XP, alongside their ordinary cooked-meat
  choice.
- Added an injected sinew service and dedicated queue 89. It uses exact 128/512 ordinary, 140/524
  Hosidius and 153/537 elite endpoints, the sourced immediate/3/4 Make-All cadence, range animation,
  atomic full-inventory replacement, Cooking/max-cape protection and shared Cooking cancellation.
  The exact local-client success line is emitted.
- Added two cache-aware scenarios, bringing the focused Cooking suite to 73 passing tests. They pin
  all inputs/results, chooser order, curves, timing, XP, members/fire gates, burn path, all three raw
  variants, capes, full-inventory behavior and movement interruption. The shared chooser change was
  also covered by the complete passing Cooking class.
- The sinew page proves a failure chance but does not state the failure object/message. Burnt-meat
  evidence supplies the shared object; wording, direct one-item presentation, sound and packet order
  remain manual case 34. RuneLite was not launched and the user's running server was untouched.
- The manifest now tracks 243 unique groups: 48 discovered, 46 researched, 1 unit-tested, 147
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 6 seconds (52 executed, 6 from cache and 732 up-to-date).

## 2026-09-10 — Ordinary iron-spit Cooking

- Pinned the final pre-cutoff roast bird meat 14978928, roast beast meat 14873307, roast rabbit
  14787731, skewered bird meat 14745632, skewered beast 14977506, skewered rabbit 14760085 and iron
  spit 14995320 revisions against the cache-233 object graph. These establish all three levels, XP,
  success curves, three-tick roast results, burnt outputs and returned tool.
- Added typed references for the complete 7222-7225 and 9978-9992 object families, injected
  `IronSpitCooking`, dedicated queue 90, symmetric raw-meat/spit registrations and both ordinary and
  permanent-fire routes. Preparation is immediate and zero-XP; roasting is transactional, returns
  the spit, honors level-99 and Cooking/max-cape immunity, and shares Cooking cancellation.
- Kept a source-visible requirement distinction: every preparation checks boosted Firemaking 20;
  bird roasting rechecks it because its roast recipe explicitly lists the skill, while the beast
  and rabbit roast recipes omit it. Manual case 35 requires a matching-client observation before
  that distinction can be promoted.
- Added four cache-aware scenarios, bringing the focused Cooking class to 77 passing tests. They
  cover identities and registrations, all recipes, both held-item orders, boosted/member gates,
  exact timing/XP/curves, success and burn, returned spit, full inventory, capes, movement, stale
  slots and interruption. The focused formatter and integration task passed 333 actionable tasks
  with zero failures in 11 seconds (7 executed, 6 from cache and 320 up-to-date).
- Exact complete chat lines, sound and packet/result-slot ordering remain manual case 35. The local
  RuneLite checkout was only inspected, never launched, and the user's running server was untouched.
- The manifest now tracks 244 unique groups: 48 discovered, 46 researched, 1 unit-tested, 148
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 9 seconds (115 executed, 6 from cache and 669 up-to-date).

## 2026-09-10 — Lunar Bake Pie

- Pinned final pre-cutoff Bake Pie revision 14834971 against spell object 9107, Lunar spellbook
  component child 105, Lunar Diplomacy completion state 190, all 12 pie pairs and their exact
  visible Cooking levels, XP, rune costs and three-tick guaranteed-success behavior.
- Added typed component, animation 4413 and graphic 746 references, queue 91, a RuneLite-compatible
  overlay handler and an automatic inventory-order spell service. Each result atomically replaces
  one raw pie in place, consumes one rune set, awards 60 Magic XP plus full Cooking XP, rechecks the
  visible boosted Cooking level and stops safely on depleted boosts, lost pies, exhausted runes or
  interruption.
- Added six cache-aware scenarios covering contracts, all pie identities, results at ticks 3/6/9,
  members/quest/book/Magic/Cooking gates, guaranteed no-burn results, full inventory, stale slots,
  rune exhaustion, animation/graphic application and interruption. The focused suite passed all
  six scenarios.
- Exact failure wording, mixed-level inventory policy, sound, packet ordering and Varrock elite
  credit remain manual case 36. Cache presentation alternatives 4422/727 were not guessed over the
  local RuneLite evidence for 4413/746. RuneLite was not launched and the user's running server was
  untouched.
- The manifest now tracks 246 unique groups: 48 discovered, 47 researched, 1 unit-tested, 149
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 9 seconds (115 executed, 6 from cache and 669 up-to-date).

## 2026-09-10 — Lunar Humidify

- Pinned final pre-cutoff Humidify revision 14977371 against spell object 11145, Lunar spellbook
  component child 110, Dream Mentor completion state 28, the cache presentation and all cache-233
  item pairs. The January 2026 Sailing seedlings are explicitly outside this baseline.
- Added typed component, Dream Mentor varbit, animation 6294, graphic 1061 and 61 input/output item
  references, plus a RuneLite-compatible overlay handler and injected spell service. One cast
  transactionally replaces every eligible item in its existing slot, charges one astral/three
  water/one fire rune, grants one 65-XP award and applies the shared three-tick utility cooldown.
- The transformation registry covers 40 pairs: ordinary containers, all four depleted waterskins,
  all eight depleted watering cans, clay, the Chambers gourd vial and all 20 pre-Sailing seedlings.
  The spell enforces members, Lunar Diplomacy, Dream Mentor, Lunar spellbook and visible Magic
  requirements before mutation and uses the shared staff, combination-rune, rune-pouch and
  Fountain policies.
- Added seven cache-aware scenarios covering the cache/quest contract, every transform, mixed and
  full inventories, same-slot atomicity, one rune/XP award, all requirement and resource failures,
  staff and Fountain substitutions, presentation and the exact three-tick recast boundary. The
  complete spell-utility module passed 146 scenarios; its focused formatting/integration gate
  passed 333 actionable tasks with zero failures in 11 seconds (6 executed, 6 from cache and 321
  up-to-date).
- Gricoller's 1,000-charge backing state, Olm flame walls, Soul Wars, Desert diary credit, exact
  failure wording, sound and rain/no-rain presentation remain manual case 37. RuneLite was not
  launched and the user's running rsmod server was untouched.
- The manifest now tracks 248 unique groups: 48 discovered, 48 researched, 1 unit-tested, 150
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 3 seconds (114 executed, 6 from cache and 670 up-to-date).

## 2026-09-10 — Lunar Hunter Kit

- Pinned final pre-cutoff Hunter Kit spell revision 14838556 and cache-cutoff item revision
  14987664 against spell object 11146, result item 11159, Lunar component child 114, Dream Mentor
  completion state 28, the exact eight-item contents and both cache sound identities.
- Added typed spell/item/component/animation/graphic/sound references, a RuneLite-compatible
  overlay handler and an injected spell service. Casting enforces members, Lunar Diplomacy, Dream
  Mentor, Lunar spellbook and visible Magic-71 requirements; charges two astral/two earth runes;
  grants 70 Magic XP; uses animation 6303, graphic 1074 and sound 3615; and applies the shared
  three-tick utility cooldown.
- Added the exact duplicate-kit/full-eight-item-set rejection. Opening atomically replaces item
  11159 with all eight contents only when seven additional slots are available and plays sound
  3616. The existing generic Destroy route and cache-enriched destruction note remain authoritative.
- Added eight cache-aware scenarios covering contracts, every gate, rune and capacity atomicity,
  complete versus incomplete content sets, earth-staff and Fountain policies, presentation,
  cooldown, all unpacked items, stale requests and the six-versus-seven-free-slot boundary. The
  focused suite passed all eight scenarios; the complete spell-utility suite passed 154 scenarios,
  and its parity gate reports 250 valid feature groups.
- Entrana's exact island boundary and Poll 85's one-time Feldip Hills/Rellekka expert grant remain
  researched in manual case 38; neither was approximated. RuneLite was inspected locally but not
  launched, and the user's running rsmod server was untouched.
- The manifest now tracks 250 unique groups: 48 discovered, 49 researched, 1 unit-tested, 151
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 1 second (112 executed, 6 from cache and 672 up-to-date).

## 2026-09-10 — Lunar Magic Imbue

- Pinned final pre-cutoff Magic Imbue revision 14915930 and Combination rune revision 14975631
  against spell object 9124, Lunar component child 128, Lunar Diplomacy completion state 190,
  sequence 8970, synth 2888 and active varbit 5438. Cache inspection established that the client
  field is two bits on permanent varp 439; local RuneLite independently documents each displayed
  unit as ten remaining game ticks.
- Added typed spell/component/animation/sound/timer/varbit and steam-staff references, a protected
  overlay handler and a public state repository for later Runecraft consumption. Casting enforces
  members, Lunar Diplomacy, Lunar spellbook and visible Magic-82 requirements; atomically charges
  two astral/seven fire/seven water runes; awards 86 Magic XP; emits sequence 8970 and synth 2888;
  and intentionally leaves the shared utility cooldown ready while locking only Magic Imbue for
  exactly 21 ticks.
- The state synchronizes client varbit 5438 as 2, 1 and 0 across its 21-tick lifetime, is explicitly
  cleared on login/logout because the cache varp is permanent, and supports the steam battlestaff,
  mystic steam staff, twinflame staff, shared rune-pouch/combination substitutions and Fountain
  policy.
- Added seven cache-aware scenarios covering the cache/quest contract, success and every gate,
  atomic rune failures, all three steam-staff substitutions, Fountain behavior, zero shared
  cooldown, the exact 21-tick recast boundary, client-display transitions and lifecycle cleanup.
  The focused suite passed all seven scenarios, and the complete spell-utility suite passed 161
  scenarios.
- The six elemental combination-rune routes, aether runes, Binding necklace lifecycle, exact
  feedback/effect lifecycle, sequence 8982, packet ordering and matching-client observation remain
  researched in manual case 39. RuneLite was inspected locally but not launched, and the user's
  running rsmod server was untouched.
- The manifest now tracks 252 unique groups: 48 discovered, 50 researched, 1 unit-tested, 152
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 3 seconds (116 executed, 6 from cache and 668 up-to-date).

## 2026-09-10 — Lunar Stat Spy

- Pinned final pre-cutoff Stat Spy revision 14277394 against spell object 11147, Lunar component
  child 118, Dream Mentor/Lunar Diplomacy completion states 28/190, side interface 523, all 23
  current/base skill pairs, heading child 94, close child 95, sequence 6293, caster graphic 1060
  and synth 3620. The local revision-matched RuneLite checkout independently corroborated the
  object, component, interface, sequence, caster graphic and level/XP identities without launch.
- Added typed spellbook, interface, component and graphic references plus a dedicated player-target
  service and script. Casting enforces members, both quests, Lunar spellbook and visible Magic 75;
  atomically charges two astral/two cosmic/five body runes; awards 76 Magic XP; sends the caster
  animation/graphic/sound and target graphic; opens the native side panel; fills all 23 current
  and base values plus the target name; notifies the target; ignores Accept Aid; rejects self; and
  uses the researched five-tick lock and shared Fountain policy.
- Cache 233 classifies Stat Spy as `Combat` with nominal max hit 1 despite its informational
  behavior. Renamed the registry surface to `offensiveCombatSpells` and explicitly excluded Stat
  Spy from both PvP and PvN combat registration, preventing duplicate player-target handlers and
  accidental accuracy, damage, combat or skull effects while preserving the cache metadata.
- Added seven cache-aware scenarios covering the full cache/interface contract, every stat row,
  boosted and drained snapshots, runes/XP/presentation, members/quest/book/Magic/resource failures,
  self targeting, Accept Aid independence, Fountain behavior, exact five-tick lock, standard
  eight-tile routing and the close control. The focused suite passed all seven scenarios and the
  complete spell-utility suite passed all 168 scenarios.
- A later Cure Other evidence pass directly compared the dated Jagex-sourced Stat Spy, Cure Other
  and Heal Other animation captures and found the same recipient crystal effect in all three. The
  cache identity and later Lunar asset record map that shared effect to graphic 738, so the earlier
  provisional 736 target graphic was corrected and its regression expectation updated. Exact
  target text, graphic height/slot/delay, cooldown and packet order remain manual case 40; client
  parity therefore remains `researched`, not verified.
- The manifest now tracks 254 unique groups: 48 discovered, 51 researched, 1 unit-tested, 153
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 3 seconds (118 executed, 6 from cache and 666 up-to-date). RuneLite was not launched or
  modified in this slice, and the user's running rsmod server was untouched.

## 2026-09-10 — Lunar Spin Flax

- Pinned final pre-cutoff Spin Flax revision 14960688 against spell object 20589, Lunar component
  child 146, Lunar Diplomacy completion state 190, flax 1779, bow string 1777, sequence 4413,
  graphic 728 and synth 3615. The local revision-matched RuneLite checkout independently
  corroborated the object, component and presentation identities without launch.
- Added a dedicated utility-spell service, overlay handler and one-deep queue. Casting enforces
  members, Lunar Diplomacy, Lunar spellbook and visible Magic 76; atomically charges one astral,
  two nature and five air runes; converts up to five flax in inventory order; awards 75 Magic XP
  and 15 Crafting XP per result; uses the exact five-tick rate and exact no-flax feedback; and
  follows the shared staff, rune-pouch and Fountain policies.
- Extended the bow-string-spool service with silent capacity/storage operations so Spin Flax
  automatically deposits produced strings into its account-backed charges while the spool is
  carried. Partial remaining capacity is filled first and only the overflow is created loose;
  same-slot inventory replacement remains atomic even when the inventory is full.
- Added eight cache-aware scenarios covering cache identities, exact costs/XP/presentation,
  partial and five-item conversion, full inventory, all gates and resource failures, staff and
  Fountain behavior, exact fifth-tick single queuing, overlay dispatch and empty/partial/full spool
  capacity. The focused Spin Flax suite passed all eight scenarios.
- Cutoff Superglass Make research exposed incompatible no-pickup excess-yield averages across its
  spell, giant-seaweed and molten-glass pages. That spell remains unimplemented rather than
  guessing a probability table; the conflict and the required matching-client capture are now in
  the source ledger and known-gaps register.
- The complete utility-spell suite passed all 176 scenarios and the affected bow-stringing suite
  passed all 17. The manifest now tracks 256 unique groups: 48 discovered, 52 researched, 1
  unit-tested, 154 integration-tested and 1 verified. The serialized repository-wide formatting,
  parity, unit and cache-aware integration gate passed all 790 actionable Gradle tasks with zero
  failures in 5 minutes 7 seconds (114 executed, 6 from cache and 670 up-to-date). RuneLite was not
  launched or modified in this slice, and the user's running rsmod server was untouched.

## 2026-09-10 — Lunar String Jewellery

- Pinned final pre-cutoff String Jewellery revision 14775009 and profit-table revision 14257547
  against spell object 9122, Lunar component child 126, Lunar Diplomacy completion state 190,
  sequence 4412, graphic 730, synth 2905 and the 28 cached input/output identities. The local
  revision-matched RuneLite checkout independently corroborated the object, component, sequence
  and graphic without launch.
- Added a dedicated recipe view over the existing Crafting amulet-stringing registry, explicitly
  filtering M'speak because it is supported by manual wool stringing but absent from the spell's
  cutoff tables. All 14 supported transforms remain data-driven through the shared typed recipes,
  while the spell correctly applies fixed 4 Crafting XP to every result, including salve.
- Added the overlay handler and interruptible automatic weak queue. Casting enforces members,
  Lunar Diplomacy, Lunar spellbook and visible Magic 80; each inventory-order result occurs after
  three ticks, atomically transforms the same slot, charges two astral/five water/ten earth runes,
  awards 83 Magic and 4 Crafting XP and emits sequence 4412, graphic 730 and synth 2905. Rune
  substitutions and Fountain policy use the shared spell services.
- Added eight cache-aware scenarios covering the exact recipe set and M'speak exclusion, cache
  identities, three-tick inventory ordering, per-item runes and XP, exhaustion, all access/resource
  failures, salve XP, full inventory, stale state, interruption, staff and Fountain behavior and
  overlay dispatch. The focused suite passed all eight scenarios; the complete utility-spell suite
  passed all 184 scenarios and the affected Crafting jewellery suite passed all 24.
- The cutoff Wiki does not publish literal empty-input feedback, graphic timing/height, repeated
  sound overlap, queue replacement or exact packet order. These remain researched in manual case
  42 rather than being presented as verified production observations. The manifest now tracks 258
  unique groups: 48 discovered, 53 researched, 1 unit-tested, 155 integration-tested and 1
  verified. The serialized repository-wide formatting, parity, unit and cache-aware integration
  gate passed all 790 actionable Gradle tasks with zero failures in 5 minutes 6 seconds (112
  executed, 6 from cache and 672 up-to-date). RuneLite was not launched or modified in this slice,
  and the user's running rsmod server was untouched.

## 2026-09-10 — Lunar Tan Leather

- Pinned final pre-cutoff Tan Leather revision 14982771, Leather revision 14893148 and sound
  revision 14890780 against spell object 1741, Lunar component child 122 with literal `Cast` and
  `Setup` operations, Lunar Diplomacy state 190, hard Fremennik Diary varbit 4493, sequence 3014,
  graphic 322 and synth 2879. The local revision-matched RuneLite checkout independently
  corroborated the component, sequence and graphic identities without launch.
- Added a dedicated utility-spell service, overlay handler, one-deep queue and per-player cowhide
  setup repository. Casting enforces members, Lunar Diplomacy, hard Fremennik Diary, Lunar
  spellbook and visible Magic 78; charges one astral, two nature and five fire runes once per
  batch; awards one 81-XP Magic grant and no Crafting XP; atomically transforms up to five eligible
  inventory slots; emits the cache audiovisuals; and applies the three-tick rate. Setup selects
  soft or hard cowhide output, defaults to soft and is cleared on logout.
- Reused the typed ordinary-tanning registry for all eight outcomes: soft and hard cow leather,
  both snake-hide inputs to snakeskin, and green, blue, red and black dragon leather. Mixed inputs
  and full inventories use same-slot replacement, and staff, rune-pouch and Fountain behavior
  remains centralized in the shared rune service.
- Added eight cache-aware scenarios covering exact cache identities and every recipe, mixed
  inventory ordering and the five-hide cap, Setup and logout reset, partial/no-input atomicity,
  every access/resource gate, the exact third-tick single queued recast, staff and Fountain
  behavior and overlay dispatch. The complete utility-spell suite passed all 192 scenarios and the
  affected tanning suite passed all 10.
- Exact Setup presentation/default persistence, literal empty-input and diary feedback,
  third-click replacement/cancellation, mixed-hide ordering proof, graphic timing/height and
  packet order remain researched in manual case 43. The manifest now tracks 260 unique groups: 48
  discovered, 54 researched, 1 unit-tested, 156 integration-tested and 1 verified. The serialized
  repository-wide formatting, parity, unit and cache-aware integration gate passed all 790
  actionable Gradle tasks with zero failures in 5 minutes 1 second (112 executed, 6 from cache and
  672 up-to-date). RuneLite was not launched or modified in this slice, and the user's running
  rsmod server was untouched.

## 2026-09-10 — Lunar Plank Make

- Pinned final pre-cutoff Plank Make revision 14937338, Sawmill voucher revision 14992202 and
  sound revision 14890672 against spell object 11149, Lunar component child 133, Lunar Diplomacy
  and Dream Mentor completion states 190/28, the four cache-era log/plank pairs, exact
  70/175/350/1,050-coin fees, voucher 28628, confirmation varbits 624/2319/2320/3939, sequence
  6298, graphic 1063 and synth 3617. The local revision-matched RuneLite checkout independently
  corroborated the spell object, component, sequence and graphic identities without launch.
- Added typed object/component/sequence/graphic/sound/varbit/queue references plus a dedicated
  overlay handler, utility-spell service, automatic weak queue and one-deep manual recast queue.
  Casting enforces members, Lunar Diplomacy, Dream Mentor, Lunar spellbook and visible Magic 86;
  each result atomically charges its exact coin fee, two astral/one nature/fifteen earth runes,
  awards 90 Magic XP outside the Fountain of Rune, emits the cache audiovisuals and produces no
  Construction XP. Rune substitutions remain centralized in the shared rune manager, and the
  common utility cooldown repository is now correctly singleton-scoped in production.
- Implemented the dated July 2024 ordering rule: selecting any supported log converts the first
  supported log in inventory and adds its plank to the first available slot. Manual casting
  produces the first result immediately, may queue one recast at tick three and replaces the
  automatic continuation; automatic production gives its second result five ticks later and
  subsequent results six ticks apart. A carried sawmill voucher creates a second matching plank
  only when ordinary inventory capacity already exists and is retained otherwise.
- Added eight cache-aware scenarios covering every recipe and fee, first-available output and
  first-inventory mixed-log ordering, exact immediate/+5/+6 automatic timing, voucher capacity,
  all members/quest/book/level/coin/rune/target failures, atomicity, one-deep manual replacement,
  earth-staff substitution, Fountain behavior and interface-event dispatch. The focused suite and
  complete spell-utility suite passed all eight and 200 scenarios respectively.
- Preserved the source conflict instead of hiding it: the spell page gives an exact immediate,
  five-tick second and six-tick repeat schedule, while voucher recipe rows summarize autocasting
  as five ticks. Exact warning dialogs and setting polarity, authoritative failure text,
  plank-sack voucher capacity, Varrock elite diary credit, interruption/cross-action behavior,
  graphic timing/height and packet ordering remain researched in manual case 44, so this core is
  integration-tested rather than client-tested or verified.
- The manifest now tracks 262 unique groups: 48 discovered, 55 researched, 1 unit-tested, 157
  integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit and
  cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 23 seconds (115 executed, 6 from cache and 669 up-to-date). RuneLite was not launched or
  modified in this slice, and the user's running rsmod server was untouched.

## 2026-09-10 — Lunar Recharge Dragonstone

- Pinned final pre-cutoff Recharge Dragonstone revision 14715163 plus the contemporaneous amulet
  of glory, combat bracelet and skills necklace revisions against spell object 2529, Lunar
  component child 136, four water/one astral/one soul runes, 97.5 Magic XP, hard Fremennik Diary
  varbit 4493, Legends' Quest varp 139 and quest DB completion state 75. The local
  revision-matched RuneLite checkout independently corroborated the component, graphic and
  jewellery identities without launch.
- Added typed references and a dedicated utility-spell service and overlay handler. One cast
  transactionally converts all eligible inventory slots, charges one rune set and one 97.5-XP
  grant for the batch, emits sequence 4409, graphic 282 and synth 139, and uses the shared
  three-tick utility cooldown. Members, Lunar Diplomacy, hard diary, Lunar spellbook, visible
  Magic, rune substitutions and Fountain behavior follow the established spell services.
- Implemented all sixteen evidence-backed zero-through-three transforms: ordinary and trimmed
  glories always recharge to four, while skills necklaces and combat bracelets require Legends'
  Quest state 75. Rings of wealth, eternal glories, equipped items and already-four-through-six
  charge states remain unchanged. Five/six-charge exclusion is deliberately conservative because
  the cutoff source does not resolve whether a four-charge spell would downgrade them.
- Added eight cache-aware scenarios covering all exact object identities and transforms,
  family-specific Legends gating, four/five/six/eternal/wealth/equipment exclusions, one-batch
  runes and XP, full-inventory atomicity, every access/resource gate, water-staff and Fountain
  policy, audiovisual output, exact three-tick recast boundary and interface dispatch. The
  focused suite and complete utility-spell suite passed all eight and 208 scenarios respectively.
- Exact five/six-charge production behavior, trimmed-glory confirmation, already-four-only cast
  consumption, literal feedback, noted/equipped scope, recast/cross-action semantics, graphic
  height/delay and packet ordering remain researched in manual case 45. The manifest now tracks
  264 unique groups: 48 discovered, 56 researched, 1 unit-tested, 158 integration-tested and 1
  verified. The serialized repository-wide formatting, parity, unit and cache-aware integration
  gate passed all 790 actionable Gradle tasks with zero failures in 5 minutes 15 seconds (112
  executed, 6 from cache and 672 up-to-date). RuneLite was not launched or modified in this slice,
  and the user's running rsmod server was untouched.

## 2026-09-10 — Lunar Cure Me

- Pinned final pre-cutoff Cure Me revision 14899448, Antipoison revision 14993758 and Venom
  revision 14984136 plus Jagex's 24 May 2023 cast-rate update against spell object 9113, Lunar
  component child 113, two astral/two cosmic/one law runes, 69 Magic XP, sequence 4411, graphic
  742, synth 2884 and poison varp 102. The local revision-matched RuneLite checkout independently
  corroborated the component, graphic and full poison/venom varp formulas without launch.
- Added typed component, sequence, graphic and synth references plus a dedicated self-cast service
  and overlay handler. Casting enforces members, Lunar Diplomacy, Lunar spellbook and visible Magic
  71; atomically charges the exact runes; awards 69 Magic XP outside the Fountain of Rune; emits
  sequence 4411, graphic 742 at height 96 and synth 2884; and applies the official one-tick cap
  with the documented same-tick feedback. LMS fake-rune behavior remains centralized in the shared
  rune manager.
- Ordinary positive poison is cleared to state zero without granting immunity. Venom is converted
  to ordinary poison with the same next damage using the documented RuneLite contracts:
  `venom_damage = min(20, (state - 999997) * 2)` and
  `poison_state = venom_damage * 5`. Clean and negative immunity states no-op without resources or
  XP. Cure Me is explicitly excluded from the offensive combat registry despite its cache combat
  classification.
- Added eight cache-aware scenarios covering exact spell/interface/state/presentation identities,
  poison clearing, preservation of immunity and undocumented secondary state, all venom damage
  stages and cap behavior, exact one-tick recasting, clean/immunity no-op, every access/resource
  failure, LMS/Fountain policy and overlay dispatch. The focused suite passed all eight scenarios;
  the complete utility-spell suite passed all 216 and the affected combat-spell suite passed all
  69.
- Varp 3777 and its `poison_type` varbit 6251 have no cutoff Wiki or inspected RuneLite Cure Me
  contract, so they are deliberately preserved rather than guessed. Their exact lifecycle,
  clean-component behavior, success feedback, poison-cycle rescheduling, cross-action cooldown,
  graphic slot/delay and packet ordering remain researched in manual case 46. The manifest now
  tracks 266 unique groups: 48 discovered, 57 researched, 1 unit-tested, 159 integration-tested
  and 1 verified. The serialized repository-wide formatting, parity, unit and cache-aware
  integration gate passed all 790 actionable Gradle tasks with zero failures in 5 minutes 7
  seconds (112 executed, 6 from cache and 672 up-to-date). RuneLite was not launched or modified
  in this slice, and the user's running rsmod server was untouched.

## 2026-09-10 — Lunar Cure Other

- Pinned final pre-cutoff Cure Other revision 14120852, Venom revision 14984136, the dated Cure
  Other animation revision 4979872 and sound revision 14885139 against spell object 9110, Lunar
  component child 109, one astral/one law/ten earth runes, 65 Magic XP, sequence 4411, target
  graphic 738, synth 2884, poison varp 102 and Accept Aid varbit 4180. The local revision-matched
  RuneLite checkout independently corroborated the object, component and graphic identities
  without launch.
- Added a typed spellbook component and recipient graphic, a shared `LunarPoisonCure` state formula
  used by Cure Me and Cure Other, and a dedicated player-target support service/script. Casting
  enforces members, Lunar Diplomacy, Lunar spellbook, visible Magic 68 and target Accept Aid;
  atomically charges the exact runes; awards 65 Magic XP outside the Fountain of Rune; emits caster
  sequence 4411, target graphic 738 at height 96 and synth 2884; and uses the standard eight-tile
  player-target route. Earth-providing staves, rune pouches and Fountain policy remain centralized
  in the shared rune manager.
- Ordinary poison clears to zero without immunity and venom downgrades to ordinary poison with the
  same next damage through the shared formula. Self-target, clean state, negative immunity,
  disabled Accept Aid and cooldown attempts leave poison, runes, XP and presentation unchanged.
  Cure Other and Cure Me are explicitly excluded from offensive spell registration despite their
  cache combat classification.
- Direct frame comparison of the dated Jagex-sourced Cure Other, Heal Other and Stat Spy captures
  showed the same recipient crystal effect. Cache identity and the later Lunar asset record resolve
  that effect as graphic 738, so Stat Spy's earlier provisional graphic 736 was corrected and its
  integration regression expectation updated in the same evidence-backed slice.
- Added eight cache-aware Cure Other scenarios covering its cache contract, ordinary poison and all
  venom stages, self/clean/immunity/Accept Aid atomic failures, every access/resource gate, exact
  provisional five-tick locking, earth-staff and Fountain policy and range-eight player targeting.
  The focused Cure Other, Cure Me and Stat Spy run passed all 23 scenarios; the complete utility
  suite passed all 224 and the affected combat-spell suite passed all 69 with zero failures.
- The Wiki does not publish clean/immune target behavior, literal success text, exact rate, graphic
  protocol timing or packet order. Five ticks remains a documented provisional combat interval;
  the differing Cure Other/Cure Me Wiki audio captures and sole typed cache `lunar_cure` synth also
  leave exact sound identity open. These and secondary poison state/scheduling, movement/logout
  races and shared-lock behavior remain manual case 47. The manifest now tracks 268 unique groups:
  48 discovered, 58 researched, 1 unit-tested, 160 integration-tested and 1 verified. The
  serialized repository-wide formatting, parity, unit and cache-aware integration gate passed all
  790 actionable Gradle tasks with zero failures in 5 minutes 7 seconds (110 executed, 6 from cache
  and 674 up-to-date). RuneLite was not launched or modified, and the user's running rsmod server
  was untouched.

## 2026-09-10 — Lunar Cure Group

- Pinned final pre-cutoff Cure Group revision 14896822, its dated animation revision 4979880 and
  sound revision 14885142 against spell object 9116, Lunar component child 117, two astral/two
  cosmic/two law runes, 74 Magic XP, sequence 4409, graphic 744, synth 2884, poison varp 102 and
  Accept Aid varbit 4180. The local revision-matched RuneLite checkout independently corroborated
  the object, component, sequence and graphic identities without launch.
- Added typed component and graphic references plus a dedicated group-support service and overlay
  handler. Casting enforces members, Lunar Diplomacy, Lunar spellbook and visible Magic 74;
  atomically charges the exact runes; awards 74 Magic XP outside the Fountain of Rune; emits
  sequence 4409 and synth 2884 on the caster; applies graphic 744 at height 96 to each eligible
  recipient; and uses the provisional shared five-tick utility cooldown. Rune-pouch and Fountain
  behavior remains centralized in the shared rune manager, with no unsupported LMS fake-rune
  exemption.
- Implemented the documented one-square radius as the caster's same-plane 3-by-3 Chebyshev area,
  including the caster and another player on the caster's tile. The caster is included regardless
  of their own Accept Aid setting; every other player must have Accept Aid enabled. Ordinary
  poison clears to zero, venom downgrades to ordinary poison with the same next damage through
  `LunarPoisonCure`, and clean or immune eligible recipients retain their state while still
  receiving the group graphic. Players at distance two or on another plane are excluded. Cure
  Group is explicitly excluded from offensive spell registration despite its cache combat class.
- Added eight cache-aware scenarios covering exact cache identities and audiovisual output, the
  complete 3-by-3 area including shared tiles, distance and plane exclusions, caster and recipient
  Accept Aid rules, clean and immunity preservation, ordinary poison and every venom stage, all
  access/resource failures, clean-cast consumption, exact provisional five-tick locking, Fountain
  policy and overlay dispatch. The focused suite passed all eight scenarios; the complete utility
  suite passed all 232 and the affected combat-spell suite passed all 69 with zero failures.
- The cutoff evidence does not resolve clean-cast consumption, clean/immune recipient graphics,
  caster Accept Aid independence, literal messages, exact cast rate, movement/logout races,
  player-list snapshot timing, graphic slot/delay, sound identity or packet order. The implemented
  choices are recorded as explicit inference and remain manual case 48 rather than client-tested
  parity. The manifest now tracks 270 unique groups: 48 discovered, 59 researched, 1 unit-tested,
  161 integration-tested and 1 verified. The serialized repository-wide formatting, parity, unit
  and cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures in 5
  minutes 8 seconds (114 executed, 6 from cache and 670 up-to-date). RuneLite was not launched or
  modified, and the user's running rsmod server was untouched.

## 2026-09-10 — Lunar Dream

- Pinned final pre-cutoff Dream revision 14844104, its Jagex-sourced animation revision 4979890
  and archived Mod Ash separate-timer answer against spell object 11148, Lunar component child 125,
  two astral/one cosmic/five body runes, 82 Magic XP, Lunar Diplomacy/Dream Mentor completion
  states 190/28, sequences 6296/6302/7627/7628 and Z graphic 1056. The local revision-matched
  RuneLite checkout independently corroborated the object, sequence and graphic identities without
  launch; the cache contains no Dream-named synth, consistent with the period Wiki's silent-spell
  contract.
- Added typed Dream component, start/sleep/end/Z sequence and Z-graphic references, queue 97, a
  tokenized weak state repository, dedicated service and overlay/session/action script. Casting
  enforces members, both quests, Lunar spellbook, visible Magic 79 and damaged Hitpoints before
  atomically charging runes and awarding 82 Magic XP outside the Fountain of Rune. It starts with
  sequence 7627, transitions after that cache sequence's duration to sleep sequence 6296 and graphic
  1056, and clears the graphic with wake sequence 7628 on completion or interruption.
- Implemented Dream's independent three-Hitpoints-per-minute channel as one heal at elapsed ticks
  34, 67 and 100 of each 100-tick period. This exactly preserves three heals per 60 seconds on the
  server's 600 ms integer grid while keeping the 34/33/33 split and initial phase explicitly
  inference-bound. The channel runs until base Hitpoints, detects damage before healing, ends on
  published player actions, survives chat and rejects an active recast atomically. Login/logout
  clear stale state. Dream is explicitly excluded from offensive spell registration despite the
  cache combat classification.
- Added eight cache-aware scenarios covering exact spell and presentation identities, rune/XP and
  silent-cast behavior, the 34/67/100 schedule, cache-duration sleep transition, full-health wake,
  action/damage interruption, chat and active recast semantics, all access/resource failures,
  Fountain policy, overlay dispatch and session cleanup. The focused Dream suite passed all eight;
  the complete utility suite passed all 240 and the affected combat-spell suite passed all 69 with
  zero failures.
- Exact first-heal phase and 33/34 distribution, Z repeat/protocol timing, the armour-change
  interruption exception, full-health feedback, ordinary regeneration and Rapid Heal/cape/bracelet
  interactions, simultaneous hit/heal order, Nightmare Zone restriction/message conflict, jar-of-
  dreams easter egg, network/session races and packet order remain manual case 49. The manifest now
  tracks 272 unique groups: 48 discovered, 60 researched, 1 unit-tested, 162 integration-tested and
  1 verified. The serialized repository-wide formatting, parity, unit and cache-aware integration
  gate passed all 790 actionable Gradle tasks with zero failures in 5 minutes 5 seconds (110
  executed, 6 from cache and 674 up-to-date). RuneLite was not launched or modified, and the user's
  running rsmod server was untouched.

## 2026-09-10 — Lunar Heal Other

- Pinned historical Heal Other revision 14538824, Jagex-sourced animation revision 4979907, sound
  revision 14885149 and the official 5 May 2021 cooldown hotfix against spell object 9134, Lunar
  component child 140, three astral/one blood/three law runes, 101 Magic XP, sequence 4411, target
  graphic 738, synth 2895 and Accept Aid varbit 4180. The revision-matched RuneLite checkout was
  inspected without launch or modification and independently corroborated the component identity.
- Frame inspection of the dated animation directly proves that 99 current Hitpoints produces a
  visible 74 self-hit, resolving the transfer maximum as `floor(current * 75 / 100)`. Period
  measured testing plus the Wiki's `up to` wording supports capping both heal and caster loss to the
  recipient's deficit. A historical Lunar asset list independently supports sequence 4411 and
  target graphic 738 at height 96. Independent sound-name lists identify synth 2895 as the spell
  sound and 2892 as an individual-recipient sound; only 2895 is emitted pending client capture.
- Added typed component and synth references, an isolated integer transfer formula and a dedicated
  player-target support service/script. Casting enforces members, Lunar Diplomacy, Lunar spellbook,
  visible Magic 92, non-self, Accept Aid, living/damaged recipient and at least 11% of base caster
  Hitpoints before atomically charging resources. It awards 101 Magic XP outside the Fountain of
  Rune, emits sequence 4411, recipient graphic 738 at height 96 and synth 2895, heals the recipient
  and applies the exact same visible typeless instant hit to the caster. Heal Other is explicitly
  excluded from offensive spell registration.
- Added eight cache-aware scenarios covering the exact cache/formula/presentation contract, the
  full 99-to-74 transfer, recipient-deficit capping, threshold/self/full/dead/Accept Aid atomic
  failures, all access and rune gates, rune substitutions, exact provisional five-tick locking,
  absence of the obsolete 90-second cooldown, Fountain behavior and range-eight routing. The
  focused suite passed all eight; the complete utility suite passed all 248 and the affected
  combat-spell suite passed all 69 with zero failures.
- The repository has no authoritative account-mode runtime, so the published Ironman-recipient
  restriction remains a cross-system gap instead of a guessed local flag. Exact feedback, boosted-
  health boundaries, PvP/singles behavior, synth 2892/2895 routing, ordinary/shared locks,
  movement/logout/death races, support-combat effects, protocol timing and packet order remain
  manual case 50. The manifest now tracks 274 unique groups: 48 discovered, 61 researched, 1 unit-
  tested, 163 integration-tested and 1 verified. The serialized repository-wide formatting,
  parity, unit and cache-aware integration gate passed all 790 actionable Gradle tasks with zero
  failures in 4 minutes 57 seconds (112 executed, 6 from cache and 672 up-to-date). RuneLite was
  not launched or modified, and the user's running rsmod server was untouched.

## 2026-09-10 — Lunar Energy Transfer

- Pinned final pre-cutoff Energy Transfer revision 14754923, cutoff Special attacks revision
  14994987, the three official 2015/2016/2018 PvP restriction updates, Jagex-sourced animation
  revision 4979905 and sound revision 14885107 against spell object 9133, Lunar component child
  139, three astral/two law/one nature runes, 100 Magic XP, sequence 4414, target graphic 734,
  synths 2885/2906, Accept Aid varbit 4180 and special/run internal maxima 1,000/10,000. The local
  revision-matched RuneLite checkout was inspected without launch or modification and independently
  corroborated the spell, component, exact sequence and graphic identities.
- The feature-specific Wiki page and cutoff Special attacks page agree that the recipient receives
  full run and special energy; this outweighs a generic cutoff Combat spells table's conflicting
  `some run energy` wording. Frame inspection directly proves the caster's visible red 10 self-hit.
  The exact cache-derived sequence 4414 and its kneeling animation outweigh RuneLite's generic 4411
  target-Lunar grouping. Independent legacy sound-name maps identify 2885 as
  `lunar_energy_transfer` and 2906 as `lunar_transfer_other`; their caster/recipient routing,
  graphic height 96 and the ordinary five-tick rate remain explicit capture-bound inferences.
- Added typed component, sequence, graphic and synth references, a dedicated player-target service
  and overlay script, and shared capped special-energy restoration. Casting now enforces members,
  Lunar Diplomacy, Lunar spellbook, visible Magic 91, a different living Accept-Aid-enabled target,
  caster Hitpoints above 10 and a full caster special bar before resource mutation. Success charges
  exact runes, awards 100 Magic XP outside the Fountain of Rune, drains caster special energy,
  fills the target's run and special pools, emits the cache presentation and applies an exact
  visible typeless 10-point self-hit. Single-way Wilderness transfers and targets with active
  recorded PvP combat on PvP worlds are rejected, and the support spell is excluded from offensive
  combat registration.
- Energy Transfer exposed Bryophyta's staff's false unlimited-nature classification. The official
  launch update and cutoff staff revision 14917258 instead specify up to 1,000 stored nature runes,
  one charge per qualifying cast and a 1/15 chance to save exactly one charge. The false mapping was
  removed and a regression proves wearing the staff without a real nature-rune source does not
  grant a free cast. Cache-233 charge loading/configuration, substitution, saving, depletion,
  bank/trade/death behavior and feedback are tracked as a separate researched lifecycle group.
- Added eight cache-aware scenarios covering exact cache/presentation contracts, full resource
  transfer and cost, the 10/11-Hitpoints boundary, self/dead/Accept Aid/incomplete-special atomic
  failures, every access/rune gate, the Bryophyta regression, single-way Wilderness and recent-
  combat PvP-world restrictions, five-tick and Fountain behavior, and range-eight routing. The
  focused suite passed all eight; affected suites passed 17 shared-rune unit, 256 utility-spell and
  69 combat-spell scenarios with zero failures. Exact PvP-world safe-zone classification,
  `recently` duration, rejection text, already-full-target behavior, shared locks, races, combat
  side effects and packet order remain manual case 51.
- The manifest now tracks 277 unique groups: 48 discovered, 63 researched, 1 unit-tested, 164
  integration-tested and 1 verified. The serialized repository-wide formatting, cache-233 parity,
  unit and cache-aware integration gate passed all 790 actionable Gradle tasks with zero failures
  in 4 minutes 57 seconds (110 executed, 6 from cache and 674 up-to-date). RuneLite was not launched
  or modified, and the user's running rsmod server was untouched.

## 2026-09-10 — Lunar Vengeance Other and rebound

- Pinned final pre-cutoff Vengeance Other revision 14938415, Vengeance revision 14996801 and
  recoil-immune template revision 14902396, plus the official 31 July 2024 blighted-sack update and
  6 June 2019 PvP-animation update. Cache 233.4 resolves object 9135, component 218:141, sequence
  4411, recipient graphic 725, synth 2908 and active/cooldown varbits 2450/2451. It also inventories
  self-Vengeance object 9136, component 218:142, sequences 8316/8317, graphic 726 and synth 2907 for
  the next slice. The local RuneLite checkout corroborated these identities read-only and was not
  launched or modified.
- Added the first general post-application player-hit event so content can react to the actual
  Hitpoints removed after validation and capping. Vengeance Other now enforces members, Lunar
  Diplomacy, Lunar spellbook, visible Magic 93, non-self/living/Accept-Aid recipient, POH-presence
  and single-way-Wilderness gates before charging three astral, ten earth and two death runes.
  Success awards 108 Magic XP outside the Fountain, emits sequence 4411, recipient graphic 725,
  synth 2908 and the exact recipient message, then arms varbit 2450 and starts an exact 50-cycle
  caster cooldown through varbit 2451. The official blighted Vengeance sack works wherever the
  shared blighted-items policy permits it, and the spell is excluded from offensive registration.
- Added a shared Vengeance state repository, future POH-controller presence seam and rebound
  service. The next positive player- or NPC-sourced queued-hit impact reflects
  `max(1, floor(actual damage * 3 / 4))`, leaves the incoming damage intact, speaks the exact phrase
  and consumes once. Unsourced and zero damage retain the effect. The 20-name cutoff recoil-immune
  roster also retains it. Player rebound uses the non-recursive instant processor; NPC rebound uses
  the engine's minimum legal one-cycle queue. Logout clears an armed effect while the in-memory
  caster cooldown remains synchronized on the retained player object.
- Added 11 cache-aware scenarios covering exact cache/presentation/formula contracts, success and
  all access/resource/target failures, POH and Wilderness gates, blighted substitution, exact
  cooldown boundary, range-eight dispatch, player/NPC/environmental and zero-hit behavior,
  minimum-one and lethal-overkill actual-damage rounding, recoil immunity, speech and logout/login
  synchronization. The focused suite passed all 11. Exact messages outside the published recipient
  and rebound text, graphic/sound routing, NPC impact cycle, typed immunity morphs, POH-entry
  cleanup, reconstructed-session cooldown persistence, recoil/Smite/simultaneous-hit order, self
  Vengeance and packet order remain manual case 52.
- The affected suites passed all 267 utility-spell and 51 player-API integration scenarios with
  zero failures. The manifest now tracks 281 unique groups: 48 discovered, 65 researched, 1 unit-
  tested, 166 integration-tested and 1 verified. The serialized repository-wide formatting,
  cache-233 parity, unit and cache-aware integration gate passed all 790 actionable Gradle tasks
  with zero failures in 5 minutes 25 seconds (124 executed, 6 from cache and 660 up-to-date).
  RuneLite was not launched or modified, and the user's running rsmod server was untouched.

## 2026-09-10 — Lunar self Vengeance and animation picker

- Pinned final pre-cutoff Vengeance revision 14996801 and the official 6 June 2019 PvP-animation
  update against spell object 9136, Lunar component 218:142, four astral/ten earth/two death runes,
  112 Magic XP, sequences 8316/8317, graphic 726 and synth 2907. Cache 233.4 further resolves the
  native animation picker as interface 525, dynamic list component 8, client script 7707, table-83
  row 4094, toggle varps 4559/4562, selected override varbit 17316 and League-IV unlock varbit
  17327. The alternate presentation is sequence 10626, graphic 2605 and synth 7569; the reward
  scroll is object 28696. A later-revision Clockwork capture was used only to corroborate the
  protocol shape, while the cutoff cache remains authoritative for every identity and option.
- Added the self-target spell with members, Lunar Diplomacy, Lunar spellbook, visible Magic 94,
  POH-presence, active-effect and shared 50-cycle cooldown gates. A successful cast charges exact
  resources, awards 112 Magic XP outside the Fountain of Rune, arms the existing shared Vengeance
  rebound state and chooses the configured presentation. Ordinary areas use sequence 8316, PvP or
  Wilderness areas use sequence 8317, and the unlocked League-IV option uses sequence 10626,
  graphic 2605 and synth 7569. Self Vengeance is legal in single-way Wilderness and no longer
  cancels walking outside PvP areas. Blighted Vengeance sacks and the Fountain/LMS rune policies
  follow the shared spell-resource implementation. The support spell is excluded from offensive
  combat registration.
- Added a cache-native animation-option repository and overlay/modal scripts. Component 218:142
  Op1 casts Vengeance and Op10 opens interface 525, seeds varp 4559 with row 4094 and runs client
  script 7707 with normalized enabled/allowed masks. Dynamic component 525:8 children toggle the
  default and League-IV choices without permitting a locked option or disabling the final enabled
  choice. Selection is randomized across enabled choices and mirrored to varbit 17316. The shared
  hit/timer/login/logout effect lifecycle was separated from Vengeance Other's target script so
  either spell owns the same one-shot rebound semantics.
- Added ten cache-aware self-Vengeance scenarios covering cache/database/variable contracts,
  default cost/XP/presentation/state, every access/resource/POH/active failure, single-way
  Wilderness and blighted sacks, PvP movement behavior, Fountain and LMS policies, exact
  bidirectional shared-cooldown timing, Op10 picker packets without accidental casting, locked and
  last-choice picker rules, unlock selection and the exact League-IV presentation. The combined
  self/Other focused suite passed all 21 scenarios and the complete utility-spell suite passed all
  277 with zero failures.
- Cache 233 contains no implemented server updater for client PvP-area varbit 8121, so presentation
  currently uses that bit when available and falls back to the authoritative Wilderness varbit.
  Exact safe-zone/PvP-world boundary updates remain manual case 52. The reward-scroll consumption,
  duplicate-unlock, persistence and trade/death lifecycle remain a separate researched group until
  temporally correct evidence resolves them; no guessed item handler was added. The manifest now
  tracks 283 unique groups: 48 discovered, 65 researched, 1 unit-tested, 168 integration-tested
  and 1 verified. The serialized repository-wide formatting, cache-233 parity, unit and cache-aware
  integration gate passed all 790 actionable Gradle tasks with zero failures in 5 minutes 14
  seconds (112 executed, 6 from cache and 672 up-to-date). RuneLite was not launched or modified,
  and the user's running rsmod server was untouched.

## 2026-09-10 — Trailblazer Reloaded Vengeance reward scroll

- Pinned historical reward-scroll revision 14535911 and its Jagex-licensed media revision
  14535905, both dated 18 January 2024, alongside the official Trailblazer Reloaded rewards update
  and cutoff Vengeance revision. They establish that object 28696 is a members-only, tradeable,
  non-stackable consumable with a `Read` operation; it permanently exposes the League-IV option,
  does not select that option automatically and does not affect Vengeance Other. The 5.9-second
  official capture identifies a shared reading animation across the four Trailblazer Reloaded
  reward scrolls, which cache 233.4 resolves as sequence 10625.
- Added a typed reference for the shared scroll-reading sequence and registered object 28696's
  actual held Op1 route in the existing Vengeance script. A valid member read atomically removes
  exactly one scroll from the selected slot, sets unlock varbit 17327 and plays sequence 10625.
  Existing animation toggles are left unchanged, so the newly exposed cosmetic remains disabled
  until the player selects it through the cache-native Animation menu. Non-members and an account
  whose unlock bit is already set retain the item and state.
- Extended the Vengeance cache-aware suite from ten to twelve scenarios. The additions pin the
  scroll's exact ID, name, membership, tradeability, stackability, `Read` operation and sequence;
  exercise real `HeldInteractions` dispatch from a non-default inventory slot; prove one-item
  consumption, unlock mutation, default-selection retention and the newly allowed picker mask; and
  prove non-members and duplicate reads are atomic. The focused suite passed all 12 scenarios and
  the complete utility-spell suite passed all 279 with zero failures.
- The cited historical sources do not publish exact non-members or duplicate-use feedback, and the
  repository does not yet provide an end-to-end reconstructed-account persistence harness. Those
  literal messages and disk/session persistence remain manual case 52 rather than being guessed.
  The scroll group is therefore `integration-tested`, not client-tested or verified. The manifest
  remains at 283 unique groups: 48 discovered, 64 researched, 1 unit-tested, 169 integration-tested
  and 1 verified. The serialized repository-wide formatting, cache-233 parity, unit and cache-aware
  integration gate passed all 790 actionable Gradle tasks with zero failures in 5 minutes 13
  seconds (112 executed, 6 from cache and 672 up-to-date). RuneLite was not launched or modified,
  and the user's running rsmod server was untouched.

## 2026-09-10 — Lunar Heal Group

- Pinned historical Heal Group revision 14407925, the official 5 May 2021 Games Room Tables and
  Fishing Skill Boss Improvements announcement preserved by Steam, and historical Jagex-licensed
  sound revision 14885108. Together they establish Magic 95, four astral/three blood/six law runes,
  124 Magic XP, up to five injured nearby recipients, the 75%-health pool plus five, the food-style
  three-cycle attack-delay addition and exact 90-second cooldown. Cache 233.4 resolves object 9137,
  component 218:143, sequence 4409, recipient graphic 745, synths 2893/2894 and cooldown varbit 925.
  The revision-matched RuneLite checkout corroborated those identities and its 150-game-tick timer
  read-only; it was neither modified nor launched.
- Added a direct overlay spell route and dedicated cooldown repository. Heal Group now enforces
  members, Lunar Diplomacy, Lunar spellbook, visible Magic 95, caster health at or above 11% of
  base and at least one eligible recipient before charging resources. It selects at most five
  living, injured, Accept-Aid players in player-list order within exact same-plane Chebyshev
  distance one, rejects single-way Wilderness and active single-way PvP-world recipients, heals
  the selected players, applies the matching visible typeless self-hit, adds three cycles only to
  an existing attack delay and mirrors the exact 150-tick cooldown through varbit 925 on cast,
  login and expiry. Cache presentation uses caster sequence 4409/synth 2894 and recipient graphic
  745 at height 96/synth 2893; the spell is excluded from offensive routing.
- Authoritative sources do not resolve integer remainder allocation, capped-share redistribution
  or whether unusable healing reduces the caster sacrifice. The implemented bounded inference is
  `floor(current * 75 / 100)` sacrifice, `floor((sacrifice + 5) / recipients)` equal share,
  recipient-deficit caps, discarded remainder, no redistribution and full caster sacrifice. Exact
  feedback, account rules, recipient ordering, authoritative PvP-area state, reconstructed-session
  cooldown persistence, movement/logout/death races and packet timing remain manual case 53, so the
  slice is `integration-tested`, not client-tested or verified.
- Added twelve cache-aware scenarios covering formula/presentation contracts, single and capped
  five-recipient behavior, exact area/plane/Accept-Aid/living/injured selection, every access and
  resource failure, tick-149/tick-150 cooldown timing, active and idle attack-delay cases,
  Wilderness/PvP multiway policy, Fountain of Rune and direct UI dispatch. The focused 12-scenario
  suite and complete 291-scenario utility-spell suite passed. The manifest now tracks 285 unique
  groups: 48 discovered, 65 researched, 1 unit-tested, 170 integration-tested and 1 verified. The
  serialized repository-wide formatting, cache-233 parity, unit and cache-aware integration gate
  passed all 790 actionable Gradle tasks with zero failures in 5 minutes 14 seconds (112 executed,
  6 from cache and 672 up-to-date). RuneLite was not launched or modified, and the user's running
  rsmod server was untouched.

## 2026-09-10 — Lunar Spellbook Swap

- Pinned final pre-cutoff Spellbook Swap revision 14842132 and the cutoff Arceuus access changes.
  Together they establish Magic 96, three astral/two cosmic/one law runes, 130 Magic XP after a
  valid selection, access-filtered alternate books, one successful spell or exactly two minutes,
  a four-tick interaction lock, moving-cast route/run-energy behavior and LMS cancellation. Cache
  233.4 resolves object 11150, component 218:144, sequence 6299, graphic 1062, synth 3613 and
  varbits 3617/4823/9631. The revision-matched RuneLite checkout corroborated the identifiers and
  120-second timer read-only; it was neither modified nor launched.
- Added a focused Lunar overlay service/script and state repository. Selection is atomic: resources
  are preflighted before the menu, then revalidated and consumed only after Standard, unlocked
  Ancient or unlocked Arceuus is chosen. Success applies cache presentation, XP outside the
  Fountain of Rune, an exact 200-tick soft-timer lifecycle, LMS cancellation, same-object login
  retention and orphaned-client-state repair. Existing routes are preserved and a generic
  exclusive map-clock field suppresses running-energy drain for the four cast ticks.
- Added a common synchronous `MagicSpellEvents.Cast` seam after spell access and resource success.
  Spellbook Swap consumes itself there, so failed casts retain the state and every successfully
  committed spell family can revert to Lunar consistently. This defines success at resource
  commitment; downstream-effect failure semantics remain explicit client research rather than an
  unlabelled assumption.
- Added twelve cache-aware Spellbook Swap scenarios and one game-process movement regression. They
  cover cache/presentation contracts, all three selectable books, cancellation and access/resource
  atomicity, exact tick-199/tick-200 expiry, failed and successful next casts, LMS, login repair,
  Fountain behavior, moving-route preservation and four clocks of energy suppression followed by
  ordinary drain. The focused suite and complete 303-scenario utility-spell suite passed, as did
  affected movement, engine, spell-registry and combat-spell suites.
- No authoritative clientscript decode was found for `spellbook_swap_book`; selected
  `Spellbook.varValue` storage remains provisional. Picker presentation, literal feedback, every
  downstream spell-family boundary, exact LMS-cycle ordering, reconstructed-session persistence
  and packet timing remain manual case 54. The core is therefore `integration-tested`, not
  client-tested or verified. The manifest now tracks 287 unique groups: 48 discovered, 66
  researched, 1 unit-tested, 171 integration-tested and 1 verified. The serialized repository-wide
  formatting, cache-233 parity, unit and cache-aware integration gate passed all 790 actionable
  Gradle tasks with zero failures in 5 minutes 28 seconds (125 executed, 6 from cache and 659
  up-to-date). RuneLite was not launched or modified, and the user's running rsmod server was
  untouched.

## 2026-09-10 — Lunar fixed-destination self teleports

- Researched all eight cache-233 Lunar self teleports against the pre-cutoff Teleportation-spells
  table, Lunar spellbook, Lunar Diplomacy, Ice Plateau and generic teleport-sound references. Cache
  contracts pin each object, component, level, rune cost, XP award and destination parameter plus
  the shared sequence 4423, graphic 747, synth 200 and Ourania unlock varbit 5376. Read-only
  inspection of the revision-matched RuneLite checkout corroborated the animation, graphic and
  client identifiers; it was neither launched nor modified.
- Added typed references, a focused Lunar teleport service and direct component script for
  Moonclan, Ourania, Waterbirth, Barbarian Outpost, Khazard, Fishing Guild, Catherby and Ice
  Plateau. The service enforces members, Lunar Diplomacy, Lunar spellbook, visible Magic, exact
  resources, Baba Yaga's separate Ourania unlock, Tele Block and source-Wilderness level 20. It
  uses each cache destination, exact XP, the Lunar audiovisuals and a four-tick delay.
- Added the Ice Plateau Deep Wilderness confirmation before resource commitment. Requirements are
  preflighted, the prior action is stopped on a valid click, acceptance is revalidated before
  charging, and cancel remains atomic. Exact warning interface/text/settings and Ourania's literal
  locked feedback remain deliberately unguessed for manual case 55.
- Added ten cache-aware scenarios covering every cache and component contract, all eight complete
  success paths, exact costs/XP/presentation/four-tick destinations, every common gate, Ourania
  locked/unlocked state, source Wilderness levels 20/21, Ice Plateau acceptance/cancellation,
  staff substitution, Fountain behavior and interface dispatch. The focused suite and complete
  313-scenario utility-spell suite passed. The manifest now tracks 289 unique groups: 48
  discovered, 67 researched, 1 unit-tested, 172 integration-tested and 1 verified.
- The serialized repository-wide `spotlessCheck`, cache-233 parity, unit-test and integration gate
  passed all 790 actionable Gradle tasks with zero failures in 5 minutes 16 seconds (112 executed,
  6 from cache and 672 up-to-date). RuneLite was not launched or modified, and the user's running
  rsmod server was untouched.

## 2026-09-10 — Lunar group teleports

- Pinned the final pre-cache-233.4 revisions of all seven Tele Group pages. Together with cache
  objects 9112/9115/9118/9121/9128/9130/9132, components 218:112/116/120/124/132/135/138 and
  interface 326, they establish every level/rune/XP contract, four-tick cast, same-plane 3-by-3
  Accept-Aid recipient area, independent offers and exact destination polygons. Cache presentation
  is shared sequence 4423, graphic 747 and synth 200.
- Added typed references and a focused Lunar group-teleport service/script for Moonclan,
  Waterbirth, Barbarian, Khazard, Fishing Guild, Catherby and Ice Plateau. The caster is charged
  once and teleports after four ticks while each snapshotted eligible recipient receives an
  independent native confirmation. Accept revalidates recipient Tele Block and source Wilderness;
  decline, close and logout clear only that recipient's offer. Ice Plateau retains the atomic
  pre-cast Deep Wilderness warning.
- Implemented every historical landing shape as an explicit discrete tile set, including the
  five-tile Moonclan row and irregular 23-tile Catherby polygon. The accepted-recipient four-tick
  Lunar presentation is a bounded inference because no authoritative source pins its exact timing.
  The unpublished Moonclan visited-island threshold, account-mode Accept Aid rules, offer timeout/
  text, Ice recipient warning, movement/death races, Deadman behavior and packet order remain
  manual case 56 rather than guessed behavior.
- Added eleven cache-aware scenarios covering cache contracts, all seven cost/XP/presentation
  paths, exact polygon boundaries, shared/adjacent/distant/other-plane recipient selection,
  independent accept/decline/close/logout behavior, every common access and rune gate, caster and
  recipient Tele Block/Wilderness boundaries, Ice accept/cancel, direct UI dispatch, staff
  substitution and Fountain behavior. The focused eleven-scenario suite and complete 324-scenario
  utility-spell suite passed.
- The manifest now tracks 290 unique groups: 48 discovered, 67 researched, 1 unit-tested, 173
  integration-tested and 1 verified. After correcting one formatter-only line wrap, the serialized
  repository-wide `spotlessCheck`, cache-233 parity, unit-test and integration gate passed all 790
  actionable Gradle tasks with zero failures in 5 minutes 16 seconds (117 executed, 6 from cache
  and 667 up-to-date). RuneLite was not launched or modified, and the user's running rsmod server
  was untouched.

## 2026-09-10 — Lunar Stat Restore Pot Share

- Pinned the final pre-cache-233.4 Stat Restore Pot Share and all seven compatible potion-family
  revisions. Cache object 9123 and component 218:127 establish Magic 81, ten earth/ten water/two
  astral runes and 84 XP; dose objects establish every transformation. Cache sequence 4413,
  graphic 733, disease varp 456 and stamina varbits 24/25 provide the runtime presentation and
  state contracts. Read-only RuneLite inspection corroborated the animation, graphic and stamina
  identities; the sibling checkout was neither launched nor modified.
- Added typed references and a focused item-target spell service/script. One cast atomically uses
  one through four doses for same-plane 5-by-5 Accept-Aid recipients, including shared tiles, and
  gives the caster the selected effect for free. The implementation supports Restore, Energy,
  Prayer, Super energy, Super restore, Sanfew and Stamina families, preserves the exact dose chain,
  handles final-vial replacement in a full inventory, enforces members/Lunar Diplomacy/Lunar-
  spellbook/level/resource gates and participates in substitutions and Fountain policy.
- Added exact immediate skill, prayer and run-energy formulas, poison/venom and disease clearing,
  and each recipient's own holy-wrench, Prayer-cape, Max-cape or imbued-Ring-of-the-gods prayer
  modifier. Added session-local stamina state with exact 200-cycle refresh/expiry, client varbit
  synchronization and the documented 70-percent run-drain reduction in the common movement engine.
  The account-mode exception, Sanfew immunity windows, Ring of Endurance extension and persistence
  remain explicit gaps rather than approximations.
- Corrected combat-spell discovery so Stat Restore Pot Share and the future Boost Potion Share are
  routed as support item-target spells instead of attacks. Added eight cache-aware scenarios for
  all contracts, spatial and dose limits, all potion effects and equipment modifiers, stamina
  lifecycle, failure atomicity, Fountain/substitution policy and direct UI dispatch. The focused
  suite passed all eight scenarios; the complete utility-spell integration suite passed all 332
  scenarios across 35 test classes.
- The three-cycle cast lock, graphic height 96 and player-list recipient priority are bounded
  inferences pending manual case 57. The manifest now tracks 292 unique groups: 48 discovered, 68
  researched, 1 unit-tested, 174 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, cache-233 parity, unit-test and integration gate passed all 790 actionable tasks
  with zero failures in 5 minutes 6 seconds (112 executed, 6 from cache and 672 up-to-date).
  RuneLite was not launched or modified, and the user's running rsmod server was untouched.

## 2026-09-11 — Lunar Boost Potion Share ordinary stat-boost core

- Pinned final pre-cache-233.4 Boost Potion Share revision 14766606 and the cutoff revisions for
  thirteen ordinary stat-boost potion families. Together with cache object 9126, component 218:130,
  all 52 dose objects, sequence 4413 and graphic 733, the evidence establishes exact costs, XP,
  transformations and immediate formulas. Read-only RuneLite inspection corroborated those client
  identities; the checkout was neither launched nor modified.
- Added typed references, an item-target spell service/script and a data-driven potion family
  registry. The core covers Attack, Strength, Defence, Ranging, Magic, Super attack, Super strength,
  Super defence, Super combat, Bastion, Battlemage, Agility and Fishing potions with ordinary
  drained-stat/capped-boost semantics. It atomically consumes one through four doses for eligible
  same-plane 5-by-5 recipients, applies the effect to the caster for free, and supports final-vial
  replacement, substitutions, Fountain policy and direct component dispatch.
- Enforced the historical Castle Wars prohibition and literal feedback over the cache-enriched
  level-0 arena rectangle, and rejected the explicitly incompatible Combat potion. The broader
  historical wording permits additional non-stat potions, so antipoison/antidote/anti-venom,
  antifire, goading, Sanfew and every other candidate remain an explicit researched extension
  pending exact duration/state support rather than being guessed.
- Added six cache-aware scenarios covering all contracts, all thirteen immediate families,
  selection and dose limits, free caster effects, Castle Wars, incompatible and access failures,
  full inventories, Fountain/cooldown behavior and UI dispatch. The focused suite passed all six
  scenarios; the complete utility-spell suite passed all 338 integration scenarios across 36 test
  classes. The manifest now tracks 294 groups: 48 discovered, 69 researched, 1 unit-tested, 175
  integration-tested and 1 verified. The serialized repository-wide `spotlessCheck`, cache-233
  parity, unit-test and integration gate passed all 790 actionable tasks with zero failures in
  5 minutes 15 seconds (114 executed, 6 from cache and 670 up-to-date). RuneLite was not launched
  or modified, and the user's running rsmod server was untouched.

## 2026-09-11 — Lunar potion-share poison protection

- Pinned the final pre-cache-233.4 revisions for Antipoison, Superantipoison, Antidote+, Antidote++,
  anti-venom, anti-venom+, extended anti-venom+, Sanfew serum, Poison and Venom. Together with the
  read-only RuneLite varp-102 contract, the evidence resolves the seven exact starting states,
  poison-versus-venom cure behavior, 30-cycle toxin phase, variable short venom windows and
  non-shortening protection rule. All 32 cache dose objects are pinned through typed symbols.
- Added a shared poison-protection repository and timer lifecycle. Antipoison through Antidote++
  downgrade venom to equal-damage poison on the first dose; all anti-venom families cure it
  outright. Negative varp-102 states advance on the global 30-cycle phase, pause at logout and
  resume on login. A weaker remedy cannot shorten existing protection.
- Added exact Sanfew behavior to both potion-share spells: ordinary restore and per-recipient Prayer
  modifier formulas, six-minute poison immunity, disease cure and session-local fifteen-minute
  disease immunity. Dragonfire potions and goading remain separate researched slices because their
  effects require real combat and NPC-aggression consumers; they were not represented as inert
  flags. Interface-paused toxin timing, poison/disease infliction consumers and disease death/
  reconstruction persistence remain explicit gaps.
- Expanded Boost Potion Share to 21 supported families and nine cache-aware scenarios. The focused
  Boost/Stat Restore run passed all 17 scenarios; the complete utility-spell suite passed all 341
  integration scenarios across 36 test classes. The repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate then passed in 5m21s: 790 actionable
  tasks, 112 executed, 6 from cache and 672 up-to-date. RuneLite was not launched or modified, and
  the user's running RSMod server was untouched.

## 2026-09-11 — Player poison and venom lifecycle core

- Extended the pinned Poison and Venom evidence into the shared varp-102 lifecycle: poison deals
  `ceil(severity / 5)` damage and loses one severity per 30-cycle hit until natural expiry; venom
  deals 6, 8, 10, 12, 14, 16, 18 and then permanent 20-damage hits. Exact cache poison/venom
  hitsplats and the standard instant-hit/death processor are used.
- Implemented the historical modal-interface pause. A due toxin or immunity interval polls without
  changing state while a modal is open, applies immediately on the first cycle after closure and
  then restarts a fresh 30-cycle cadence. Login resumes persisted non-zero state on the global
  phase; logout pauses the timer.
- Added clean-target poison and standard-venom infliction seams that reject matching immunity.
  Source-specific NPC, weapon, smoke-spell and special-attack producers and their reapplication
  policies remain explicit follow-up work because the cutoff sources do not prove one universal
  replacement rule.
- Expanded Boost Potion Share to thirteen cache-aware scenarios covering severity decline, natural
  cure, venom escalation/cap, exact hitsplats, interface deferral, immunity rejection, clean-target
  infliction and fatal toxin death queuing. The complete utility-spell suite passed all 345
  integration scenarios across 36 test classes. The repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test`, and `integration` gate then passed in 5m 14s: 790 actionable
  tasks, with 53 executed, 6 restored from cache, and 731 up-to-date. RuneLite was not launched or
  modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Boost Potion Share antifire and standard dragonfire core

- Pinned the final pre-cache-233.4 revisions for ordinary, extended, super and extended-super
  antifire and the common Dragonfire protection table. Cache varbits 3981 and 6101 plus the
  read-only revision-matched RuneLite checkout independently establish 30-cycle ordinary and
  20-cycle superior display units. All 16 cache dose objects are typed.
- Added the four antifire families to Boost Potion Share with exact 600/1,200/300/600-cycle
  durations, cache-visible countdowns, 15-second warnings, expiry messages, same-tier reset and
  resident-session logout/resume behavior.
- Added a reusable common-breath consumer for chromatic and metallic dragons: exact 50/30
  failed/resisted maxima, five-damage shield cap, ten-damage chromatic Protect from Magic cap,
  post-roll ordinary-potion subtraction to 35/15 maximum or zero with a second layer,
  superior-potion immunity and metallic prayer bypass. Boss/special breath tables and source NPC
  attack selection remain deliberately separate.
- Expanded Boost Potion Share to sixteen cache-aware scenarios. The complete utility-spell suite
  passed all 348 integration scenarios across 36 test classes. The repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test`, and `integration` gate then passed in 5m 18s:
  790 actionable tasks, with 112 executed, 6 restored from cache, and 672 up-to-date. RuneLite was
  not launched or modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Boost Potion Share goading and NPC-aggression core

- Pinned Goading potion revision 14993925 against cache objects 30137/30140/30143/30146, varbit
  11294, Slayer-requirement param 65401 and the cache multiway area. Read-only inspection of the
  revision-matched RuneLite timer consumer establishes six-cycle display units; the checkout was
  neither launched nor modified.
- Added Goading potion to Boost Potion Share with an exact 600-cycle effect, initial display value
  100, exact expiry, same-effect reset and resident-session logout pause/resume. Cold reconstruction
  remains bounded to the persisted six-cycle display bucket.
- Integrated active goading into pre-tick NPC acquisition. Only passive visible types exposing the
  ordinary Attack operation are eligible; acquisition is same-plane within Chebyshev radius four,
  enforces the player's visible Slayer level, preserves every existing NPC interaction and routes
  through the ordinary NPC-to-player operation. Multiway scans are immediate; the historical
  “roughly three seconds” single-way replacement is bounded as five cycles.
- Added a semantic player spell-teleport event and routed all implemented Standard, Lunar, group,
  Teleother, house, bounty-target and Home teleport completions through it. Active goading clears
  on that event, while generic relocations used by doors and stairs deliberately do not publish it.
- Added cache-typed exclusions for Fight Caves, Inferno and Jad-challenge Yt-HurKot forms 3128,
  7701, 7705 and 10624 plus Jal-Nib 7691. Exhaustive immunity proof, teleport/cup-of-tea
  cancellation outside implemented spells, the literal early-expiry message and matching-client
  timing remain explicit gaps.
- The focused six-scenario aggression suite, complete 73-scenario game-process suite and complete
  349-scenario utility-spell suite passed with zero failures. The manifest now tracks 298 unique
  groups: 48 discovered, 69 researched, 1 unit-tested, 179 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test`, and `integration`
  gate passed in 5m 9s: 790 actionable tasks, with 111 executed, 6 restored from cache, and 673
  up-to-date. RuneLite was not launched or modified, and the user's running RSMod server was
  untouched.

## 2026-09-11 — Ordinary Cup of tea consumption and Goading cancellation

- Pinned ordinary Cup of tea revision 14943719, Goading potion revision 14993925, the general food
  delay reference and Jagex's 8 March 2023 food-delay explanation. They establish the distinct
  ordinary item, three-Hitpoint heal, `2 + floor(base Attack * 0.02)` boost, empty cup, forced
  overhead speech, immediate Goading cancellation and standard three-cycle food/active-attack
  delays. Two contemporaneous reports from the 6 November 2024 production change supply the exact
  early-expiry string at lower evidentiary authority; the natural-expiry string remains open.
- Added typed cache references for Cup of tea 1978 and sequence 3702. The members-only `Drink`
  handler atomically replaces the selected cup with empty cup 1980 even in a full inventory, stops
  the prior action, performs the cache sequence and sourced speech, heals and boosts with visible
  caps, and consumes at full health. A shared food repository tracks the independent three-cycle
  eat clock and adds three cycles only to an already-active attack delay.
- Added a generic successful-consumable event so Cooking remains independent of Magic. Goading
  listens for ordinary tea and uses the same exact early-expiry path as semantic spell teleports;
  drinking tea without an active effect is silent, and generic protected relocation neither
  cancels nor emits the message. Tea flask, nettle and POH Construction tea identities remain
  separate positive-classification work.
- Five cache-aware tea scenarios and the cross-module regression pass. The complete affected
  suites pass 82 Cooking and 350 utility-spell scenarios with zero failures. The manifest now
  tracks 299 unique groups: 48 discovered, 69 researched, 1 unit-tested, 180 integration-tested
  and 1 verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test`
  and `integration` gate passed in 4m 59s: 790 actionable tasks, with 114 executed, 6 restored from
  cache and 670 up-to-date. RuneLite was not launched or modified, and the user's running RSMod
  server was untouched.

## 2026-09-11 — Direct Goading-potion consumption

- Pinned the final pre-cache-233.4 Goading potion revision 14993925, the Wiki's ordinary-potion
  timing reference, the historical standard potion-message corpus and Jagex's Midsummer 2017 vial-
  smashing update. Cache 233.4 independently establishes objects 30137/30140/30143/30146, their
  `Drink` operations, empty vial 229, sequence 829 `human_eat` and varbit 5668
  `auto_smash_vials`. Read-only inspection of the revision-matched RuneLite checkout corroborated
  sequence 829 as its consuming animation; RuneLite was neither launched nor modified.
- Added a reusable Herblore potion-consumption clock, independent from food and attack delays, and
  a four-dose Goading handler. Successful drinks atomically transform the selected slot
  4→3→2→1→empty vial, including with a completely full inventory; the final vial is removed when
  automatic vial smashing is enabled. Membership, stale-slot and three-cycle repeat-drink checks
  fail without consuming or applying an effect.
- Published the existing generic successful-consumable event only after the inventory transition,
  allowing the Magic plugin to start or reset its existing 600-cycle Goading lifecycle without
  coupling Herblore to Magic. The handler uses sequence 829, leaves attack timing unchanged and
  emits the standard singular/plural/finished dose grammar. `You drink some of your goading
  potion.` is explicitly a cross-item template inference pending a matching cache-233 client
  capture; frame sound and packet ordering remain manual observations.
- Five cache-aware direct-drinking scenarios and the cross-module lifecycle regression passed.
  The complete affected suites contain 87 Herblore and 351 utility-spell integration scenarios
  across 15 and 36 test classes respectively, with zero failures, errors or skips. The manifest
  now tracks 300 unique groups: 48 discovered, 69 researched, 1 unit-tested, 181
  integration-tested and 1 verified. After the strengthened full-inventory test, the final
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 11s: 790 actionable tasks, with 5 executed, 7 restored from cache and 778
  up-to-date. RuneLite was not launched or modified, and the user's running RSMod server was
  untouched.

## 2026-09-11 — Direct ordinary Antipoison consumption

- Pinned final pre-cache-233.4 Antipoison revision 14993758, the ordinary-potion timing reference,
  the historical item-specific chat capture and Jagex's vial-smashing update. Cache 233.4
  independently establishes dose objects 2446/175/177/179, their `Drink` operations and free-to-
  play flags, empty vial 229, sequence 829 and automatic-vial-smashing varbit 5668.
- Added a reusable standard-potion dose descriptor and direct-drinking handler alongside the
  existing shared potion clock. Ordinary Antipoison now transforms the selected slot
  4→3→2→1→empty vial atomically even in a completely full inventory, destroys the last vial when
  automatic smashing is enabled, performs sequence 829 and emits the directly captured drink,
  singular/plural dose and finished-potion messages. It is correctly usable on free worlds,
  leaves attack timing unchanged and shares the three-cycle lock with Goading rather than creating
  a family-local clock.
- Routed the successful-consumable event into the existing poison lifecycle only after inventory
  success. Direct doses cure ordinary poison into exact varp-102 state -5; against venom the first
  dose produces equal-damage ordinary poison and a second dose cures it and grants the 90-second
  immunity state. Clean targets, stale slots and blocked cross-family drinks remain atomic.
- Five cache/inventory scenarios plus the end-to-end venom regression passed. The complete affected
  suites contain 92 Herblore and 352 utility-spell integration scenarios across 16 and 36 test
  classes respectively, with zero failures, errors or skips. The manifest now tracks 301 unique
  groups: 48 discovered, 69 researched, 1 unit-tested, 182 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 4m 56s: 790 actionable tasks, with 50 executed, 6 restored from cache and 734
  up-to-date. Sequence-frame sound and inventory/message/varp packet ordering remain manual
  observations. RuneLite was not launched or modified, and the user's running RSMod server was
  untouched.

## 2026-09-11 — Direct higher poison-remedy consumption

- Pinned the final pre-cache-233.4 revisions for Superantipoison 14993800, Antidote+ 14993895,
  Antidote++ 14993822, Anti-venom 14993833, Anti-venom+ 14993872 and Extended anti-venom+
  14993931. Cache 233.4 independently establishes all 24 four-through-one-dose identities, their
  members flags, `Drink` operations and exact examine descriptions. The stable historical potion-
  message corpus supplies dose grammar. A later Extended anti-venom+ revision directly records
  its unusual first line without a full stop; its post-cutoff date is recorded as a temporal
  caveat, while the other five first lines remain explicit cache-stem template inferences.
- Generalized the standard selected-slot dose descriptor and registered all six families through
  the existing Herblore plugin syntax. Every dose atomically follows its exact 4→3→2→1→vial chain
  in a full inventory, honors automatic vial smashing, performs sequence 829, rejects free-world
  use and shares the independent three-cycle potion clock without delaying attacks.
- Routed the successful-consumable event into the existing poison lifecycle only after inventory
  success. Superantipoison, Antidote+ and Antidote++ preserve the two-dose equal-damage venom
  downgrade/cure path and apply exact states -20/-30/-40. Anti-venom, Anti-venom+ and Extended
  anti-venom+ cure venom immediately and apply exact states -41/-50/-59.
- Five new cache/inventory/timing scenarios plus the end-to-end six-family lifecycle regression
  passed. The complete affected suites contain 97 Herblore and 353 utility-spell integration
  scenarios across 17 and 36 test classes respectively, with zero failures, errors or skips. The
  manifest now tracks 302 unique groups: 48 discovered, 69 researched, 1 unit-tested, 183
  integration-tested and 1 verified. The serialized repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 5m 2s: 790 actionable tasks,
  with 51 executed, 6 restored from cache and 733 up-to-date. RuneLite was not launched or
  modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Direct Sanfew-serum consumption and standard-death cleanup

- Pinned Sanfew serum revision 14993774 and the shared ordinary-potion timing and vial-smashing
  evidence. The historical revision establishes the four dose identities, members-only status,
  every-stat-except-Hitpoints restore formula `floor(base * 0.30) + 4`, Prayer restoration,
  superantipoison venom behavior, six-minute poison immunity, disease cure and fifteen-minute
  disease immunity. Cache 233.4 independently establishes objects 10925/10927/10929/10931, their
  `Drink` operations, exact descriptions, empty vial 229, sequence 829 and varbit 5668. The first
  line `You drink some of your Sanfew serum.` remains a bounded stable-potion-template inference
  pending a matching cache-233 client capture.
- Registered Sanfew through the reusable Herblore dose syntax. Every dose atomically follows its
  exact 4→3→2→1→vial chain in a full inventory, honors automatic vial smashing, rejects free-world
  use, performs sequence 829 and shares the independent three-cycle potion clock without delaying
  attacks. Exact singular, plural and finished-dose grammar uses the established standard-potion
  path.
- Routed successful consumption into the existing shared stat-restoration, poison and disease
  repositories only after the inventory transition. Direct Sanfew restores every supported
  non-Hitpoints stat, preserves Hitpoints, applies the two-dose venom downgrade/cure rule, cures
  disease and starts 1,500 cycles of disease immunity. Added a composable unbound player-death
  event emitted after the standard death reset; the Sanfew listener clears disease immunity there
  without competing for the engine's unique keyed death-queue handler. Nonstandard death paths,
  reconstruction, logout and world-hop behavior remain explicit manual/research gaps.
- Five direct-drinking scenarios, an end-to-end immediate-effects regression and a standard-death
  publication regression passed. The complete affected suites contain 74 game-process, 102
  Herblore and 354 utility-spell integration scenarios across 20, 18 and 36 test classes
  respectively, with zero failures, errors or skips. The manifest now tracks 303 unique groups:
  48 discovered, 69 researched, 1 unit-tested, 184 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 4m 52s: 790 actionable tasks, with 109 executed, 6 restored from cache and 675
  up-to-date. RuneLite was not launched or modified, and the user's running RSMod server was
  untouched.

## 2026-09-11 — Direct antifire-potion consumption

- Pinned the final pre-cutoff Antifire, Extended antifire, Super antifire and Extended super
  antifire revisions and the cache-233.4 contract for all 16 dose objects. Cache quest table 0,
  Dragon Slayer I row 31 and `varps.dragonquest` pin state 0 as not started, state 1 as the first
  accepted state and state 10 as completion. The ordinary `You drink some of your dragon potion.`
  line has a historical production observation; the other three family first lines remain bounded
  template inferences. The exact quest rejection is documented only after the cutoff and is
  explicitly retained as a matching-client verification item rather than presented as cutoff-
  direct evidence.
- Extended the reusable standard-potion dose model with an optional typed quest requirement and
  registered every Antifire-family Op1. Membership and quest gates run before any inventory, timer
  or protection mutation. Every successful dose atomically follows its exact 4→3→2→1→vial chain
  in a full inventory, honors automatic vial smashing, uses sequence 829 and shares the independent
  three-cycle potion clock without delaying attacks.
- Routed successful direct consumption through the generic consumable event into the existing
  antifire protection repository. Ordinary and extended antifire apply/reset the ordinary tier for
  exactly 600 and 1,200 cycles; super and extended-super antifire apply/reset the superior tier for
  exactly 300 and 600 cycles, preserving the already tested cache varbit, warning, expiry and
  dragonfire-resolution behavior.
- Six direct-drinking scenarios and an end-to-end four-family duration regression passed. The
  complete affected suites contain 108 Herblore and 355 utility-spell integration scenarios across
  19 and 36 test classes respectively, with zero failures, errors or skips. The manifest now tracks
  304 unique groups: 48 discovered, 69 researched, 1 unit-tested, 185 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 4m 57s: 790 actionable tasks, with 52 executed, 6 restored from
  cache and 732 up-to-date. RuneLite was not launched or modified, and the user's running RSMod
  server was untouched.

## 2026-09-11 — Direct restoration and run-energy potion consumption

- Pinned the final pre-cache-233.4 Restore, Energy, Prayer, Super energy, Super restore and Stamina
  revisions and the cache contract for all 24 four-through-one-dose objects. Energy is the only
  free-to-play family. A historical production-content corpus directly records the Restore,
  Energy, Prayer and Super restore first lines; Super energy and Stamina remain explicitly bounded
  stable-template inferences. Cache descriptions, `Drink` operations, exact dose identities,
  sequence 829, vial 229 and automatic vial-smashing varbit 5668 are independently covered.
- Registered all six families through the reusable standard-potion model. Every successful dose
  atomically follows its selected-slot 4→3→2→1→vial chain in a full inventory, honors automatic
  vial smashing and shares the independent three-cycle potion clock without delaying attacks.
  Membership rejection occurs before inventory, timer or effect mutation.
- Routed the generic successful-consumption event into the shared Stat Restore Pot Share formulas.
  Restore affects the five combat stats by `floor(base * 0.30) + 10`; Energy restores 10 percent
  maximum run energy; Prayer restores `floor(base * 0.25) + 7`; Super energy restores 20 percent;
  Super restore affects every non-Hitpoints stat and Prayer by `floor(base * 0.25) + 8`; Stamina
  restores 20 percent and applies or resets the exact 200-cycle, 70-percent-drain-reduction state.
  Direct Prayer and Super restore doses also use the drinker's own 27-percent holy-wrench, Prayer-
  cape, Max-cape or imbued-Ring-of-the-gods modifier.
- Five cache/inventory/access/timing scenarios and two end-to-end immediate-effect regressions
  passed. The complete affected suites contain 113 Herblore and 357 utility-spell integration
  scenarios across 20 and 36 classes respectively, with zero failures, errors or skips. The full
  XML audit contains 1,647 unit scenarios across 106 classes and 1,330 integration scenarios across
  162 classes, also with zero failures, errors or skips. The manifest now tracks 305 unique groups:
  48 discovered, 69 researched, 1 unit-tested, 186 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 4m 56s: 790 actionable tasks, with 52 executed, 6 restored from cache and 732 up-
  to-date. Ring of Endurance extension, Stamina persistence/death/world-hop and expiry presentation,
  the two inferred first lines, sequence-frame sound and exact packet ordering remain documented
  manual observations. RuneLite was not launched or modified, and the user's running RSMod server
  was untouched.

## 2026-09-11 — Direct ordinary stat-boost potion consumption

- Pinned the final pre-cache-233.4 Attack, Strength, Defence, Ranging, Magic, Super attack, Super
  strength, Super defence, Super combat, Bastion, Battlemage, Agility and Fishing potion revisions
  and the cache contract for all 52 four-through-one-dose objects. Strength is the sole free-to-play
  family. The historical production-content corpus directly records the ordinary/super Attack,
  Strength and Defence shared base-family lines plus the Ranging, Magic, Agility and Fishing lines;
  the cutoff Wiki revisions directly record both one-message tangy lines. Super combat remains an
  explicitly bounded stable-template inference. Cache descriptions, `Drink` operations, exact dose
  identities, sequence 829, vial 229 and automatic vial-smashing varbit 5668 are independently
  covered.
- Registered all thirteen families through the reusable standard-potion model. Every successful
  dose atomically follows its selected-slot 4→3→2→1→vial chain in a full inventory, honors automatic
  vial smashing and shares the independent three-cycle potion clock without delaying attacks.
  Membership rejection occurs before inventory, timer or effect mutation.
- Routed the generic successful-consumption event into the shared Boost Potion Share formula layer.
  Ordinary Attack, Strength and Defence use `floor(base * 0.10) + 3`; Ranging uses
  `floor(base * 0.10) + 4`; Magic uses four; the super melee boosts use
  `floor(base * 0.15) + 5`; Super combat applies all three; Bastion applies Ranging and Super
  defence; Battlemage applies Magic and Super defence; and Agility and Fishing each use three.
  Agility does not restore run energy because its cutoff revision specifies only the skill boost.
- Five cache/inventory/access/timing scenarios and one end-to-end thirteen-family immediate-effect
  regression passed. The complete affected suites contain 118 Herblore and 358 utility-spell
  integration scenarios across 21 and 36 classes respectively, with zero failures, errors or skips.
  The full XML audit contains 1,647 unit scenarios across 106 classes and 1,336 integration
  scenarios across 163 classes, also with zero failures, errors or skips. The manifest now tracks
  306 unique groups: 48 discovered, 69 researched, 1 unit-tested, 187 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 4m 57s: 790 actionable tasks, with 54 executed, 6 restored from cache
  and 730 up-to-date. Exact Super combat text, sequence-frame sound, packet ordering and ordinary
  stat-decay/Preserve timing remain documented manual observations. RuneLite was not launched or
  modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Direct Combat- and Hunter-potion consumption

- Pinned Combat potion revision 14993842, dated 25 September 2025, and pre-cutoff Hunter potion
  revision 14790574. They establish members-only access, the exact Attack-plus-Strength
  `floor(base * 0.10) + 3` and Hunter fixed-three boosts, the four dose identities and the
  `Drink`/`Empty`/`Drop` operations. Cache 233.4 independently pins Combat
  9739/9741/9743/9745, Hunter 9998/10000/10002/10004 and their case-sensitive descriptions. No
  direct first-line observation was found in either revision or the historical production-content
  corpus, so both family-name lines remain explicitly bounded stable-template inferences.
- Added typed references for the previously unreferenced one- and two-dose forms and registered all
  eight Op1 identities through the reusable standard-potion model. Every successful dose atomically
  follows its selected-slot 4→3→2→1→vial chain in a full inventory, honors automatic vial smashing
  and shares the independent three-cycle potion clock without delaying attacks. Free-world
  rejection occurs before inventory, timer or effect mutation.
- Added a direct-only typed family/effect resolver that applies the shared stat-boost primitive but
  remains outside Boost Potion Share's accepted enum. The spell's atomic-incompatibility regression
  now explicitly covers both Combat and Hunter potions as required by revision 14766606.
- Five cache/inventory/access/timing scenarios and one end-to-end two-family effect regression
  passed. The complete affected suites contain 123 Herblore and 359 utility-spell integration
  scenarios across 22 and 36 classes respectively, with zero failures, errors or skips. The full
  XML audit contains 1,647 unit scenarios across 106 classes and 1,342 integration scenarios across
  164 classes, also with zero failures, errors or skips. The manifest now tracks 307 unique groups:
  48 discovered, 69 researched, 1 unit-tested, 188 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 5m 1s: 790 actionable tasks, with 112 executed, 6 restored from cache and 672 up-
  to-date. Both inferred first lines, sequence-frame sound, packet ordering and ordinary stat-
  decay/Preserve timing remain documented manual observations. RuneLite was not launched or
  modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Direct Prayer-regeneration potion consumption

- Pinned Prayer regeneration potion revision 14993870, dated 25 September 2025, plus the
  contemporary Rising Darkness release notes and the 11 June 2025 death/minigame-reset change.
  Together they establish members-only access, one Prayer point every 12 cycles, exactly 66 grants
  per dose, no Holy wrench, Prayer cape or imbued Ring of the gods modifier, Stat Restore Pot Share
  incompatibility and standard-death cancellation. Cache 233.4 independently pins dose objects
  30125/30128/30131/30134, their exact descriptions, `Drink` operations, timer varbit 11361, buff
  varc 1200 and timer symbol 21. The local revision-matched RuneLite source was read only and
  confirms that varbit 11361 is displayed in 12-cycle units.
- Registered all four dose identities through the reusable standard-potion model, with atomic
  selected-slot 4→3→2→1→vial transitions in a full inventory, automatic vial smashing,
  sequence 829, membership gating and the shared independent three-cycle potion clock. The direct
  first line remains an explicitly bounded stable-template inference because no production capture
  was found in the cutoff sources.
- Added a timed Prayer-regeneration repository and wired it through successful direct consumption.
  It performs no immediate restoration, grants one point every 12 cycles for 66 intervals, never
  exceeds base Prayer, consumes intervals while capped, synchronizes varbit 11361, provisionally
  resets count and phase on redose, preserves resident logout/login phase, reconstructs persisted
  counts conservatively and clears on the standard death-completion event. The spell-share
  rejection regression now explicitly covers this potion family.
- Nine new integration scenarios plus the extended existing share-rejection regression passed.
  The complete affected suites contain 123 Herblore and 368 utility-spell integration scenarios
  across 22 and 37 classes respectively, with zero failures, errors or skips. The full XML audit
  contains 1,647 unit scenarios across 106 classes and 1,351 integration scenarios across 165
  classes, also with zero failures, errors or skips. The manifest now tracks 308 unique groups: 48
  discovered, 69 researched, 1 unit-tested, 189 integration-tested and 1 verified. The serialized
  repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed
  in 4m 59s: 790 actionable tasks, with 110 executed, 6 restored from cache and 674 up-to-date.
  Stat-reset-minigame hooks, exact first-line and redose semantics, cold-session sub-interval,
  nonstandard death/world-hop, varc/buff presentation, sequence-frame sound and packet ordering
  remain documented manual or research gaps. RuneLite was not launched or modified, and the
  user's running RSMod server was untouched.

## 2026-09-11 — Direct Saradomin, Zamorak and Ancient brew consumption

- Pinned the final pre-cache-233.4 Saradomin brew 14993751, Zamorak brew 14993780 and Ancient brew
  14993841 revisions. They establish all base-derived boosts, current-derived drains, overheal/
  overboost caps, Zamorak's minimum-one Hitpoints and current-Prayer rules, Ancient's five-percent
  Prayer overboost ceiling, members access and potion-share incompatibilities. The official Nex
  Rewards update independently corroborates Ancient brew's release formulas. Cache 233.4 pins all
  12 four-through-one-dose identities, exact descriptions and `Drink` operations.
- Added typed one- and two-dose references and registered all three families through the reusable
  standard-potion path. Every dose atomically follows its selected-slot 4→3→2→1→vial chain in a
  full inventory, honors automatic vial smashing, uses sequence 829 and shares the independent
  three-cycle potion clock without delaying attacks. Free-world rejection precedes inventory,
  timer and stat mutation.
- Added a typed immediate-brew effect resolver. Saradomin adds 15-percent-plus-2 Hitpoints and
  20-percent-plus-2 Defence from base while draining Attack, Strength, Ranged and Magic by
  10-percent-plus-2 from each current level. Zamorak adds 20-percent-plus-2 Attack and
  12-percent-plus-2 Strength from base, drains current Defence by 10-percent-plus-2 and current
  Hitpoints by 12 percent without crossing one, and restores ten percent of current Prayer without
  overboost. Ancient adds five-percent-plus-2 Magic, drains current Attack/Strength/Defence by
  10-percent-plus-2 and restores base-derived ten-percent-plus-2 Prayer up to five percent above
  base. Both Lunar share spells reject all three families atomically.
- Saradomin's foul-liquid line is directly recorded by the pinned historical message corpus.
  Zamorak currently uses the same line from a historical community report, and Ancient uses the
  stable family-name template; both are explicitly evidence-qualified pending manual case 60.
  Menaphite remedy was researched but kept separate because its 25-cycle/five-minute renewal must
  cancel an incomplete divine-effect lifecycle and its phase/persistence semantics remain open.
- Five consumption and three end-to-end formula scenarios passed. The complete affected suites
  contain 128 Herblore and 371 utility-spell integration scenarios across 23 and 38 classes,
  respectively, with zero failures, errors or skips. The full XML audit contains 1,647 unit
  scenarios across 106 classes and 1,359 integration scenarios across 167 classes, also with zero
  failures, errors or skips. The manifest now tracks 310 unique groups: 48 discovered, 70
  researched, 1 unit-tested, 190 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m 1s: 790
  actionable tasks, with 116 executed, 6 restored from cache and 668 up-to-date. RuneLite was not
  launched or modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Direct Forgotten brew consumption and upgrade research

- Pinned the final pre-cache-233.4 Forgotten brew revision 14993853 and the contemporary Secrets
  of the North and Boss Rewards update revisions. They establish the eight-percent-plus-3
  base-Magic boost, base-derived ten-percent-plus-2 Prayer restoration capped at five percent over
  base, and current-derived ten-percent-plus-2 Attack, Strength and Defence drains. Cache 233.4
  independently pins all four members dose objects 27629/27632/27635/27638, exact descriptions and
  `Drink` operations.
- Added typed references for all four doses and registered Forgotten brew through the reusable
  standard-potion path. Every dose atomically follows its selected-slot 4→3→2→1→vial chain in a
  full inventory, honors automatic vial smashing, uses sequence 829 and shares the independent
  three-cycle potion clock without delaying attacks. Free-world rejection precedes inventory,
  timer and stat mutation. Its first line remains an explicitly bounded standard-potion-template
  inference pending manual case 60.
- Extended the typed direct-brew effect resolver with Forgotten brew's stronger Magic formula and
  the shared Ancient-style Prayer cap and three-stat drain contract. Both Lunar potion-share spells
  now reject Forgotten brew atomically alongside Saradomin, Zamorak and Ancient brews. One new
  end-to-end formula regression extends the existing five generic consumption scenarios across all
  sixteen dose identities.
- Kept the essence upgrade interaction separate and `researched`. The same historical item revision
  pins 20 ancient essence per matching Ancient-brew dose, boosted Herblore level 91, a three-tick
  action and 36/72/108/145 XP for one through four doses; cache object 27616 and all input/output
  pairs are known. Exact held-use direction, animation, feedback, repeat behavior and capacity
  presentation remain manual observations, so no speculative handler was added.
- The complete affected suites contain 128 Herblore and 372 utility-spell integration scenarios
  across 23 and 38 classes, respectively, with zero failures, errors or skips. The full XML audit
  contains 1,647 unit scenarios across 106 classes and 1,360 integration scenarios across 167
  classes, also with zero failures, errors or skips. The manifest now tracks 311 unique groups: 48
  discovered, 71 researched, 1 unit-tested, 190 integration-tested and 1 verified. The serialized
  repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed
  in 5m 1s: 790 actionable tasks, with 114 executed, 6 restored from cache and 670 up-to-date.
  RuneLite was not launched or modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Direct Surge potion consumption, cooldown and PvP delay

- Pinned the final pre-cache-233.4 Surge potion revision 14993912 and the contemporary official
  Varlamore: The Final Dawn and 29 May 2025 Bounty Hunter update revisions. Together they establish
  the four-dose members item, 25-percent special restoration, one-dose-per-five-minute limit,
  logout-paused cooldown, full-special rejection, natural-expiry text, six-cycle PvP delay and the
  exact 15-tile retention boundary. Cache 233.4 independently pins objects
  30875/30878/30881/30884, exact descriptions and `Drink` operations, sequence 829, spotanim 3232
  with sequence 12094, timer varbit 16270 and buff-start varclient 1258. Read-only inspection of the
  sibling revision-matched RuneLite source confirms the timer uses ten-cycle units; RuneLite was
  neither launched nor modified.
- Added typed references for every dose, the Surge graphic and timer varbit, plus a permanent
  server-only remaining-cycle varp, soft cooldown timer and typed delayed-restore queue. Successful
  direct consumption atomically follows the selected-slot 4→3→2→1→vial chain in a full inventory,
  honors automatic vial smashing, uses sequence 829 and shares the independent three-cycle potion
  lock. Free-world, stale-slot, full-special and active-cooldown rejection precede inventory and
  special mutation. Both Lunar potion-share spells now explicitly reject Surge potion.
- Non-PvP consumption immediately restores 250 hundredth-percent special units up to the 1,000
  cap and plays graphic 3232. Wilderness varbit 5963, PvP-area varbit 8121 or a PvP world flag
  selects a six-cycle soft-queued restoration; Chebyshev distance 15 or less succeeds and 16 or
  more discards it. Logout and death cancel the pending queue. Standard death also clears the
  cooldown. Login resumes a stored cooldown, logout pauses it, each active cycle decrements the
  stored count, and client varbit 16270 is maintained as ceiling division into ten-cycle units.
- Kept activity-owned lifecycle behavior separate and `researched`. The cutoff sources pin pause
  rules between Colosseum and Inferno waves, Theatre of Blood rooms and Doom levels, plus resets
  for deaths, raid/Colosseum entry or exit, the Ferox FFA portal and POH pools. Their owning world
  systems do not yet expose complete hooks, and production does not yet maintain the two PvP-area
  varbits generally. The first drink line, cooldown placeholder grammar and partial-second rounding
  remain bounded inferences; logout during the pending six-cycle effect is not source-defined; the
  delayed graphic, buff-start varclient, frame sound and packet ordering remain manual case 61.
- Seven Surge integration scenarios cover cache contracts, all dose transitions, full-inventory
  behavior, vial smashing, spam locking, exact restoration/cap, atomic rejection, the full
  500-cycle timer, ten-cycle display boundaries, logout/login, expiry, PvP world/varbit selection,
  six-cycle timing, 15/16-tile boundaries and standard-death cleanup. The affected-module gate also
  reran both full Lunar sharing suites and passed in 22 seconds: 332 actionable tasks, with 5
  executed, 6 restored from cache and 321 up-to-date. The full XML audit contains 1,647 unit
  scenarios across 106 classes and 1,367 integration scenarios across 168 classes, with zero
  failures, errors or skips; Herblore contributes 135 scenarios across 24 classes and utility magic
  372 across 38 classes. The manifest now tracks 313 unique groups: 48 discovered, 72 researched,
  1 unit-tested, 191 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m 2s: 790
  actionable tasks, with 110 executed, 6 restored from cache and 674 up-to-date. The user's running
  RSMod server was untouched.

## 2026-09-11 — Forgotten brew upgrade core

- Re-read pinned Forgotten brew revision 14993853 through the Wiki API. It directly specifies all
  four matching-dose routes, 20 ancient essence per dose, boosted Herblore level 91, a three-tick
  action and exact 36/72/108/145 XP. Cache 233.4 pins ancient essence 27616 and the four Ancient-to-
  Forgotten pairs. No cutoff source pins animation, sound, feedback or automatic repetition, so
  none was invented and those presentation details remain manual case 60.
- Added a dedicated typed recipe table and weak Herblore queue. Both held-use orders resolve the
  exact brew and essence slots, access and resources are validated before scheduling and again at
  completion, and the three-tick transaction consumes 20/40/60/80 essence, replaces the brew in
  its original slot even when the inventory is full, and awards the exact non-linear XP. The action
  honors members, Druidic Ritual and boosted-level gates and participates in shared Herblore queue
  cancellation.
- Four focused scenarios and the complete 139-scenario Herblore suite across 25 classes passed.
  The full XML audit contains 1,647 unit scenarios across 106 classes and 1,371 integration
  scenarios across 169 classes, with zero failures, errors or skips. The manifest remains at 313
  unique groups but now records 48 discovered, 71 researched, 1 unit-tested, 192 integration-tested
  and 1 verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test`
  and `integration` gate passed in 5m 12s: 790 actionable tasks, with 114 executed, 6 restored from
  cache and 670 up-to-date. RuneLite was not launched or modified, and the user's running RSMod
  server was untouched.

## 2026-09-11 — Barbarian-mix Herblore production core

- Pinned final pre-cutoff Barbarian Training revision 14988720 for all 29 two-dose recipes, exact
  boosted levels and XP, the five roe-or-caviar recipes and 24 caviar-only recipes. Final Attack,
  Ancient and Extended super antifire mix revisions independently establish one tick per mixture
  across the level range. Cache 233.4 pins both fish eggs, every input/output object, native
  skill-multi interface 270, clientscript 2046, sequence 363 and `brut_herb_potion` varbit 3761.
- Added typed identities and a dedicated recipe/queue implementation following the existing
  Herblore plugin structure. Both held-use orders open native Make-All; each tick revalidates
  members, Druidic Ritual, boosted level, Otto-training state, egg type and resources before one
  atomic potion-plus-egg-to-mix transaction and exact XP award. Full inventories are safe, and the
  action participates in shared Herblore cancellation.
- The pinned cutoff Quest Helper source establishes the exact normal success message and the first
  Attack-mix training message box. Production now emits both in their respective channels and
  advances the training state from tasked 1 to mix-made 2; all other recipes require complete 3.
  Because that Quest Helper revision had moved journal progress to plugin-local configuration,
  whether the production server still updated cache varbit 3761 remains manual case 62 rather than
  a verified claim.
- Five integration scenarios pin all 29 object/level/XP contracts and dispatch routes, exact
  one-tick output and feedback, training progression/gates, both orders, Make-All cadence,
  membership, Druidic Ritual, boosted levels, roe rejection, full-inventory behavior, resource-loss
  interruption and atomicity. The complete Herblore suite contains 144 scenarios across 26 classes.
  The full XML audit contains 1,647 unit scenarios across 106 classes and 1,376 integration
  scenarios across 170 classes, with zero failures, errors or skips.
- The manifest now tracks 314 unique groups: 48 discovered, 71 researched, 1 unit-tested, 193
  integration-tested and 1 verified. The serialized repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 5m 11s: 790 actionable tasks,
  with 114 executed, 6 restored from cache and 670 up-to-date. Barbarian-mix drinking, exact
  production equipment behavior, sound and packet order remain open. RuneLite was not launched or
  modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Barbarian-mix direct consumption and effects

- Pinned the final pre-cutoff Wiki revisions for all 29 Barbarian mixes. They establish the
  seven three-Hitpoints mixes, the 22 six-Hitpoints mixes, source-potion effects, the shared
  three-cycle food delay, the three-cycle attack delay, one-dose transitions and empty-vial
  completion. The dated Relicym's mix page explicitly establishes disease curing. Cache 233.4
  independently pins every two-dose and one-dose object, each `Drink` operation and sequence 829.
- Resolved a source conflict in favor of the user's Wiki-exact requirement: final pre-cutoff
  Zamorak mix revision 14993963 specifies damage as `floor((current Hitpoints - 1) / 8)` followed
  later in the same game tick by six Hitpoints of healing. The sibling release-matched RuneLite
  checkout retains an older 12-percent independent-heal model. The parity ledger records both
  observations and production follows the dated Wiki formula.
- Added direct operation dispatch for all 58 mix objects, exact selected-slot 2→1→empty-vial
  transactions, automatic vial smashing, membership and shared-food-lock gates, sequence 829,
  lumpy-potion and dose messages, per-recipe healing, and publication of the source potion effect
  without losing the consumed mix identity. Existing potion systems now supply stat boosts and
  restoration, poison protection, disease clearing, antifire durations, stamina, Magic essence,
  Ancient brew and ordered Zamorak effects. Lunar potion sharing continues to reject mixes.
- Seven integration scenarios cover all cache identities and dispatch routes, both-dose inventory
  behavior, vial smashing, free-world atomicity, food/attack timing, every healing tier, every
  source-potion effect family and representative Zamorak current-Hitpoints boundaries. The full
  Herblore suite contains 148 scenarios across 27 classes and utility magic contains 375 across
  39 classes. The affected-module gate passed in 22 seconds: 334 actionable tasks, with 5 executed,
  6 restored from cache and 323 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,383 integration
  scenarios across 172 classes, with zero failures, errors or skips. The manifest now tracks 315
  unique groups: 48 discovered, 71 researched, 1 unit-tested, 194 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 5m: 790 actionable tasks, with 110 executed, 6 restored from cache
  and 674 up-to-date. Exact drink punctuation, sound/frame and packet ordering remain manual case
  62. RuneLite was not launched or modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Swamp-tar Herblore production core

- Pinned the final pre-cutoff Guam, Marrentill, Tarromin and Harralander tar revisions
  14628398/14628399/14628415/14628400. They establish the five cache-233 input routes, including
  clean and ground Guam, exact boosted levels 19/31/39/44, 30/42.5/55/72.5 XP, one herb plus 15
  swamp tar into 15 output, a pestle and mortar and three ticks per action. Cache 233.4 independently
  pins all input/output objects and stackability plus dedicated sequence 5249.
- Added typed tar identities, the five-entry recipe table, dedicated weak queue 101, symmetric
  held-item dispatch and a cache-native quantity flow. One available action starts directly; two or
  more use interface 270/clientscript 2046 with the maximum constrained by both herbs and complete
  15-tar groups. Each three-tick result revalidates access, boosted level, pestle and resources,
  commits one atomic transaction and participates in shared Herblore cancellation.
- Deep Research kept the implementation inside the dated cache boundary: the later Irit-tar recipe
  is Sailing-era content and was deliberately excluded. Exact success/failure wording, audio,
  animation restart and packet ordering were not invented; they remain manual case 63. The
  single-action direct-start behavior is recorded as a qualified production observation rather
  than elevated to Wiki evidence.
- Six integration scenarios cover every recipe/cache/dispatch contract, exact three-tick and
  15-unit transformations, XP, direct-versus-menu selection, tar-limited quantity, batching, both
  Guam inputs and item orders, members/Druidic/tool/level/resource gates, boosted levels,
  interruption, full inventory and queue replacement. The complete Herblore suite contains 154
  scenarios across 28 classes.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,389 integration
  scenarios across 173 classes, with zero failures, errors or skips. The manifest now tracks 316
  unique groups: 48 discovered, 71 researched, 1 unit-tested, 195 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 5m 21s: 790 actionable tasks, with 116 executed, 6 restored from
  cache and 668 up-to-date. RuneLite was not launched or modified, and the user's running RSMod
  server was untouched.

## 2026-09-11 — Coconut milk and strong weapon-poison production

- Pinned the final pre-cutoff Coconut/Coconut milk revisions 14748255/14921101 and Weapon
  poison(+)/(++) plus unfinished-vial revisions 14991927/14991890/14929742/14929744. They establish
  the hammer-to-half and half-plus-vial coconut chain, immediate/three/four milk-filling cadence,
  the cactus-spine and two-nightshade first stages, boosted levels 73/82, two ticks per poison
  stage, zero unfinished XP and 190 final XP. The active cutoff Weapon poison(+) recipe's 190 XP
  takes precedence over its historical 165-XP change note under the Wiki-exact policy.
- Added typed cache identities, coconut queue 102 and symmetric handlers. Coconut cracking is an
  immediate selected-slot replacement; filling atomically returns the shell in the half slot and
  milk in the vial slot, then continues after three and four ticks. The sources do not resolve
  automatic coconut cracking or exact presentation, so only the selected coconut is cracked and
  manual case 64 retains those capture boundaries.
- Extended the native unfinished/finished potion structures instead of adding parallel syntax.
  Unfinished queue requests now retain the exact liquid and ingredient pair, which safely
  distinguishes non-stackable Cave nightshade 2398 and stackable Nightshade 27790 despite their
  shared unfinished output 5939. Both strong final stages reuse the existing two-tick quantity
  flow. Prescription goggles can preserve both nightshade forms and either final secondary, while
  cactus spine is consumed and neither chemistry nor alchemist's amulet rolls or loses a charge.
- Nine added integration scenarios cover cache identities/dispatch, both orders, exact output
  slots, full inventories, zero-skill coconut access, immediate/three/four filling, stale resources,
  shared cancellation, all three first-stage recipes, both final recipes, boosted gates, exact
  timing/XP, both goggle forms and both excluded amulets. The complete Herblore suite contains 163
  scenarios across 29 classes. The affected-module gate and manifest validation passed.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,398 integration
  scenarios across 174 classes, with zero failures, errors or skips. The manifest now tracks 318
  unique groups: 48 discovered, 71 researched, 1 unit-tested, 197 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 5m 14s: 790 actionable tasks, with 108 executed, 6 restored from
  cache and 676 up-to-date. RuneLite was not launched or modified, and the user's running RSMod
  server was untouched.

## 2026-09-11 — Antidote+/Antidote++ Herblore production

- Pinned final pre-cutoff Antidote+/Antidote++ revisions 14993895/14993822 and unfinished-vial
  revisions 14929745/14929746. They establish coconut milk plus toadflax at boosted level 68 for a
  two-tick zero-XP unfinished stage and yew roots for a two-tick 155-XP four-dose result; coconut
  milk plus irit at level 79 instead has a one-tick zero-XP unfinished stage, followed by magic
  roots for a two-tick 177.5-XP four-dose result. Cache 233.4 independently pins all eight input,
  intermediate and output identities.
- Extended the existing unfinished and finished recipe structures with both full chains, preserving
  their distinct timing and native quantity flow rather than adding parallel syntax. Both orders,
  boosted levels, members and Druidic Ritual gates, full inventories, material-loss interruption,
  atomic batching and shared Herblore cancellation inherit the established potion path.
- The cutoff Prescription-goggles revision explicitly covers toadflax and irit in the unfinished
  stages; the ordinary secondary rule covers both roots. Automated success rolls preserve each
  herb/root without changing output or XP. Because both recipes directly produce four-dose objects
  and cache 233 has no five-dose identities, chemistry and alchemist's amulets neither change the
  output nor consume charges.
- Two integration scenarios were added and existing exhaustive contract/dispatch scenarios were
  extended. The complete Herblore suite now contains 165 scenarios across 29 classes. The focused
  two-class gate passed, and the affected formatting/manifest/full-Herblore gate passed in 12
  seconds with 336 actionable tasks, 8 executed, 6 restored from cache and 322 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,400 integration
  scenarios across 174 classes, with zero failures, errors or skips. The manifest now tracks 319
  unique groups: 48 discovered, 71 researched, 1 unit-tested, 198 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 5m 12s: 790 actionable tasks, with 112 executed, 6 restored from
  cache and 672 up-to-date. Exact presentation remains manual case 65. RuneLite was neither
  launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Anti-venom Herblore production

- Pinned final pre-cutoff Anti-venom, Anti-venom+ and Extended anti-venom+ revisions
  14993833/14993872/14993931 plus the dated Zulrah's scales, Araxyte venom sac and Torstol pages.
  Together they establish all ten cache-233 routes: four Antidote++ doses with five scales per dose
  at boosted level 87, both clean- and unfinished-torstol Anti-venom+ routes at 94, and four
  Anti-venom+ doses with one sack per dose at 94. Exact results take two/two/three ticks and award
  30/60/90/120, 125, or 20/40/60/80 XP respectively.
- Added a reusable typed dose-scaled recipe and protected mixing path, cache queue 103 and the
  missing Araxyte-sack reference. The native quantity maximum accounts for complete 5/10/15/20-
  scale and 1/2/3/4-sack bundles; every result revalidates members, Druidic Ritual, boosted level
  and exact resources before one atomic transaction. Both held-use orders, full inventories,
  batching, interruption and shared Herblore cancellation follow the established plugin syntax.
- Extended the shared production modifiers without changing ordinary finished potions. Cutoff
  amulet pages explicitly make sub-four-dose Anti-venom and Extended anti-venom+ eligible for one
  bonus dose, one success-only charge, no extra XP and no extra ingredients; four-dose inputs and
  the always-four-dose Anti-venom+ stage do not roll. Prescription goggles preserve the whole
  dose-scaled bundle. A later goggles revision is recorded only as observational clarification that
  a single successful Anti-venom roll retains all 15 or 20 scales, not as post-cutoff production.
  The release-matched RuneLite checkout corroborates dynamic two-/three-/four-dose chemistry
  messages and was inspected read-only.
- Six integration scenarios exhaust every cache identity and dispatch contract, exact timing,
  resource quantity and XP, native menu caps and repeats, both item orders, boosted/access/resource
  boundaries, full inventories, queue replacement and all goggles/amulet combinations. The
  complete Herblore suite now contains 171 scenarios across 30 classes. Exact audiovisual and
  packet presentation plus the Horseshoe Mine Karamja-elite and Sherlock master-clue hooks remain
  the separately researched manual case 66.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,406 integration
  scenarios across 175 classes, with zero failures, errors or skips. The manifest now tracks 321
  unique groups: 48 discovered, 72 researched, 1 unit-tested, 199 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 5m 21s: 790 actionable tasks, with 115 executed, 6 restored from
  cache and 669 up-to-date. RuneLite was neither launched nor modified, and the user's running
  RSMod server was untouched.

## 2026-09-11 — Stamina-potion Herblore production

- Pinned final pre-cutoff Stamina-potion and amylase-crystal revisions 14993745/14918518. They
  establish all four cache-233 routes: one through four Super-energy doses consume the same number
  of amylase crystals at boosted level 77, complete in two ticks and award 25.5 XP per dose. Cache
  233.4 independently pins inputs 3022/3020/3018/3016, secondary 12640, outputs
  12631/12629/12627/12625, stackability, membership, sequence 363 and the native quantity assets.
- Extended the reusable typed dose-scaled recipe structure rather than introducing a Stamina-only
  engine. The four recipes inherit symmetric held-use registration, complete-bundle Make-X caps,
  protected atomic transactions, per-result revalidation, boosted/members/Druidic gates, full-
  inventory behavior, interruption and shared Herblore cancellation. The family split keeps the
  existing exhaustive Anti-venom assertions scoped to those ten recipes while dispatch uses the
  combined fourteen-recipe table.
- The cutoff chemistry, alchemist's-amulet and prescription-goggles sources establish the exact
  composition: a successful goggles roll preserves the whole 1/2/3/4-crystal bundle, a sub-four-
  dose amulet success adds one output dose without another crystal or extra XP, the two rolls are
  independent, and a four-dose input never rolls or consumes an amulet charge. The official 8
  January 2025 Run Energy Changes update additionally requires starting Stamina Make-X without
  cancelling movement; the production service now has an explicit pending-route regression proof.
- Five integration scenarios cover every recipe/cache/dispatch contract, exact two-tick resources
  and XP, native interface 270/clientscript 2046 limits and batching, both item orders, both goggle
  appearances, both amulet families, boosted/access/resource boundaries, full inventories, queue
  replacement, stale resources and route preservation. The complete Herblore suite now contains
  176 scenarios across 31 classes. Exact audiovisual and packet presentation and the end-to-end
  moving client interaction remain separately researched in manual case 67.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,411 integration
  scenarios across 176 classes, with zero failures, errors or skips. The manifest now tracks 323
  unique groups: 48 discovered, 73 researched, 1 unit-tested, 200 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 5m 12s: 790 actionable tasks, with 114 executed, 6 restored from
  cache and 670 up-to-date. RuneLite was neither launched nor modified, and the user's running
  RSMod server was untouched.

## 2026-09-11 — Antifire-upgrade Herblore production

- Pinned final pre-cutoff Extended-antifire, Super-antifire and Extended-super-antifire revisions
  14993875/14993848/14993908. They establish all ten cache-233 routes: every Antifire dose plus an
  equal number of lava shards at boosted level 84; four-dose Antifire plus crushed superior dragon
  bones at 92; every Super-antifire dose plus equal lava shards at 98; and four-dose Extended
  antifire plus crushed bones at 98. Results complete in two ticks and award the exact published
  27.5/55/82.5/110, 130, 40/80/120/160 and 180 XP values respectively.
- Extended the shared typed dose-scaled recipe path and added cache varbit 6140 as the Primula
  learning gate for both crushed-bone routes and every Extended-super-antifire route. Cache 233.4
  independently pins all potion identities, lava shard 11994 as stackable, crushed superior dragon
  bones 21975 as non-stackable, sequence 363, queue 103 and the native interface 270/clientscript
  2046 quantity flow. Each action revalidates access, boosted level, unlock state and complete
  secondary bundles before committing one protected transaction.
- The cutoff amulet revisions explicitly include the variable-dose Extended-antifire and
  Extended-super-antifire recipes: only sub-four-dose inputs can receive one bonus dose, with one
  success-only charge and no extra secondary or XP. Fixed four-dose and four-dose-input recipes do
  not roll or consume a charge. Prescription goggles preserve a whole lava-shard bundle or the
  crushed-bone secondary once per potion; an amulet-created bonus dose needs no secondary. The
  official 8 January 2025 update also establishes that Extended-antifire Make-X starts without
  cancelling movement, which is covered by a pending-route regression.
- Six integration scenarios exhaust all ten cache and dispatch contracts, exact resources, timing
  and XP, complete-bundle menu limits, batching, both held-use orders, route preservation, both
  goggle appearances, both amulet families, the Primula gate, members/Druidic/boosted-level gates,
  stale resources, full inventories, interruption and queue replacement. The bounded Primula
  rejection wording and exact audiovisual/client packet presentation remain manual case 68.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,417 integration
  scenarios across 177 classes, with zero failures, errors or skips; Herblore accounts for 182
  scenarios across 32 classes. The manifest now tracks 325 unique groups: 48 discovered, 74
  researched, 1 unit-tested, 201 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m 12s: 790
  actionable tasks, with 114 executed, 6 restored from cache and 670 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Fish barrel ordinary big-net acquisition

- Extended the open-barrel capacity model across the ordinary nine-roll big-net path. Eligible raw
  mackerel, cod and bass now enter barrel storage first; non-fish results and big bass remain
  inventory-only. Source-order acceptance continues past unstoreable junk when a later fish fits.
- Rada/spirit-flake doubles account for combined per-output capacity and split across the final
  barrel slot and inventory without extra XP or flake use. Barrel-only catches bypass an empty
  inventory transaction while retaining protected storage mutation.
- Three added scenarios bring the focused Fishing suite to 117 with zero failures. The combined
  formatting, parity and focused Fishing run passed in 11s with 651 actionable tasks: 5 executed,
  6 restored from cache and 640 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,542 integration
  scenarios across 179 classes, including all 117 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 359 unique groups with no duplicate IDs: 48 discovered, 80
  researched, 1 unit-tested, 229 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m29s: 793
  actionable tasks, with 53 executed, 6 restored from cache and 734 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Fish barrel storage and ordinary-catch core

- Pinned final pre-cutoff Fish barrel revision 14978270 and official Fishing Skill Boss and Easter
  Event revision 14253445. Together they establish 28 mixed fish, open automatic acquisition,
  Fill/Check/Open/Close, bank-only removal, the full message and exclusions. Cache 233.4 fixes
  identities 25582/25584 and their exact operations.
- Added permanent non-stacking storage and 33 eligible pre-Sailing fish. Fill, functional Check,
  Open/Close, bank-side Empty, Destroy and membership are registered. Ordinary single-output
  catches count barrel capacity, catch from full inventory, keep trophies in inventory, and safely
  split a doubled result on the final barrel slot with one XP/bait award. Extended client details
  remain under manual case 75.
- Five added scenarios bring the focused Fishing suite to 114. They pin identities/operations,
  allowlist, capacity, persistence, Fill/Open/Close, membership, bank/destroy, full-inventory
  routing and doubled final-slot behavior. The combined formatting, parity and focused Fishing run
  passed in 11s with 651 actionable tasks: 7 executed, 6 restored from cache and 638 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,539 integration
  scenarios across 179 classes, including all 114 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 358 unique groups with no duplicate IDs: 48 discovered, 80
  researched, 1 unit-tested, 228 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m35s: 793
  actionable tasks, with 115 executed, 6 restored from cache and 672 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Leaping-fish knife gutting core

- Pinned the final pre-cutoff leaping trout, salmon, sturgeon and fish-offcuts revisions plus the
  official Poll 77 update. They fix the exact level/150 or level/80 gut-success fractions, success-
  only roe/caviar and 10/10/15 Cooking XP, conditional 1/2, 3/4 and 5/6 offcut rolls, immediate
  manual actions and three-tick automatic processing. Cache 233.4 pins knife 946, all six product/
  input objects and dedicated sequence 6702.
- Added a dedicated Fishing-plugin queue. Knife use immediately processes the selected fish and
  restarts a same-type three-tick automatic pass, allowing faster manual reuse. Every action
  revalidates membership, knife and fish, consumes failures without XP, and transactionally emits
  successful outputs. When a full inventory cannot represent two new stackable outputs, the
  primary roe/caviar is preserved and only the secondary offcut is suppressed; this conservative
  boundary remains explicitly pending matching-client capture.
- Five added scenarios bring the focused Fishing suite to 109. They pin configuration and dispatch,
  all probability endpoints, exact outputs and XP, failure loss, automatic cadence, membership and
  knife revalidation, and bounded full-inventory behavior. Formatting, parity and the complete
  focused Fishing suite passed in 11s with 651 actionable tasks: 5 executed, 6 restored from cache
  and 640 up-to-date.
- The required repository-wide gate passed in 5m 38s with 793 actionable tasks: 115 executed, 6
  restored from cache and 672 up-to-date. The generated reports contain 1,647 unit tests across 106
  suites and 1,534 integration tests across 179 suites, with zero failures, errors or skips.

## 2026-09-11 — Menaphite-remedy consumption and renewal

- Pinned final pre-cutoff Menaphite-remedy revision 14993892, the cutoff stat-restoration rule,
  cache 233.4 objects 27202/27205/27208/27211 and release-matched local RuneLite timer evidence.
  Together they establish a delayed first restoration, exactly twenty 25-cycle pulses over five
  minutes, remaining-pulse varbit 14448 and `6 + floor(base * 16 / 100)` restoration for lowered
  Attack, Strength, Defence, Ranged and Magic only. Hitpoints, Prayer and already-boosted stats are
  excluded.
- Added all four doses to the shared protected standard-potion path, preserving selected-slot
  4→3→2→1→vial transitions in full inventories, optional vial smashing, sequence 829, members
  gating and the independent three-cycle potion lock. A typed soft-timer repository owns the
  twenty-pulse count and cadence, resets both on redose, exposes the cache timer varbit, preserves
  the partial interval across resident-process logout and reconstructs persisted counts on login.
- Added a reusable divine-effect cancellation seam for the eight cache-233 divine-potion timer
  families and saturated heart. Drinking the remedy removes pre-existing timer state without
  changing visible boosts; a divine effect started afterward remains valid. Both Lunar potion-
  sharing spells explicitly reject every Menaphite dose through their normal unsupported-family
  path.
- Eight dedicated integration scenarios cover cache identities, descriptions and dispatch, every
  dose transition, full-inventory and free-world atomicity, stale slots, vial smashing, delayed and
  exact restoration formulas, all twenty pulses, redosing, divine-state cancellation, resident
  logout, cold reconstruction and standard-death clearing. The utility-magic suite now contains
  383 scenarios across 40 classes; Herblore remains at 182 scenarios across 32 classes. Exact first
  drink wording, audiovisual and packet presentation, cold-session sub-interval persistence,
  world-hop/nonstandard-death behavior and the complete production divine/saturated-heart
  lifecycles remain separately researched under manual case 60.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,425 integration
  scenarios across 178 classes, with zero failures, errors or skips. The manifest now tracks 326
  unique groups: 48 discovered, 74 researched, 1 unit-tested, 202 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 5m 1s: 790 actionable tasks, with 112 executed, 6 restored from
  cache and 672 up-to-date. RuneLite was neither launched nor modified, and the user's running
  RSMod server was untouched.

## 2026-09-11 — Ordinary small-net and bait Fishing

- Pinned final pre-cutoff Fishing, raw-fish, Fishing-spot, tool, success-rate and tick-manipulation
  Wiki revisions 14980550, 14935359, 14935360, 14957879, 14882830, 14993003, 14968357,
  14944253, 14997126, 14944202, 14756461 and 14986181. Together with cache 233.4 they establish
  the standard five-tick roll cadence; ordered anchovy-before-shrimp and herring-before-sardine
  rolls; exact levels, XP, success thresholds and messages; success-only bait consumption; and
  ordinary or pearl fishing-rod eligibility.
- Added a typed Fishing plugin for all 21 cache-233 `saltfish` NPC identities. Option 1 drives the
  small-net table and option 3 drives the bait table, with cache-backed tools, fish, sequences and
  exact option/examine contracts. Each five-tick attempt revalidates adjacency, boosted Fishing
  level, tool, bait and inventory capacity, performs the cascading protected random rolls, and
  commits successful bait removal and fish addition in one protected inventory transaction.
- Nine integration scenarios prove the exact 21-NPC/cache/event contract, both ordered catch
  tables, direct first-roll timing, fallback rolls, real option-3 routing, success-only bait,
  failure cadence, worn pearl rods, full-inventory atomicity and level/equipment/distance
  revalidation. Ordinary spots are temporarily stationary until their exact cache-233 relocation
  topology is reproduced; movement, click-to-catch presentation, tick-manipulation, sound and the
  wider river/sea tables remain separately researched under manual case 69.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,434 integration
  scenarios across 179 classes, with zero failures, errors or skips; Fishing accounts for nine
  integration scenarios in one class. The manifest now tracks 328 unique groups: 48 discovered,
  75 researched, 1 unit-tested, 203 integration-tested and 1 verified. The serialized
  repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed
  in 5m 24s: 793 actionable tasks, with 114 executed, 6 restored from cache and 673 up-to-date.
  RuneLite was neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Ordinary lure/bait Fishing

- Pinned final pre-cutoff lure/bait spot, trout, salmon, pike, rainbow-fish, feather and rod Wiki
  revisions 14993394, 14935651, 14975761, 14957889, 14791991, 14889095, 14788011, 14986075
  and 14935074. They establish the ordered salmon-before-trout cascade, exact levels 30/20,
  endpoints 16/96 then 32/192, 70/50 XP, five-tick rolls and success-only regular feathers; pike
  level 25, endpoints 16/96, 60 XP and its independent one-in-five six-tick interval; and the
  members-only stripy-feather rainbow route at level 38, endpoints 8/64 and 80 XP.
- Added typed references and direct first/third-operation dispatch for all 26 cache-233 `freshfish`
  NPCs. Cache inspection proves `Lure`, empty, `Bait`, hidden, hidden option slots and the shared
  examine string on every identity. The shared protected loop now resolves typed tool variants to
  their own animations: ordinary sequence 622, pearl fishing-rod sequence 8188 and pearl fly-rod
  sequence 8189. It selects the regular-feather table when regular feathers are present and the
  stripy-feather table otherwise, with membership, level, distance, equipment, bait and capacity
  revalidation plus one atomic bait-to-fish inventory transaction.
- Seven added integration scenarios pin the 26-NPC/cache/event contract, exact catch tables,
  salmon and trout selection/messages, success-only feather use, the carried/worn pearl fly rod,
  pike's extra-tick branch, rainbow membership and cache-specific pearl animations. The complete
  Fishing suite now has 16 scenarios. Exact dual-feather priority in the matching client,
  relocation topology, legacy pike/rainbow and rejection wording, sounds, packet order, tick
  manipulation and end-to-end interruption remain separately researched under manual case 70.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,441 integration
  scenarios across 179 classes, with zero failures, errors or skips. The manifest now tracks 330
  unique groups: 48 discovered, 76 researched, 1 unit-tested, 204 integration-tested and 1
  verified. The affected gate passed with 337 actionable tasks. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m 23s: 793
  actionable tasks, with 114 executed, 6 restored from cache and 673 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Ordinary cage/lobster Fishing

- Pinned final pre-cutoff cage/harpoon-spot, raw-lobster, lobster-pot and Fishing revisions
  14997261, 14935370, 14956268 and 14980550. They establish boosted level 40, 90 XP, success
  endpoints 6/95, the non-consumed lobster pot and ordinary five-tick attempts. Cache 233.4 pins
  lobster pot 301, raw lobster 377 and dedicated sequence 619.
- Added typed NPC references and direct first-option lobster dispatch for the 16 cache identities
  whose operation slots are exactly `Cage`, empty, `Harpoon`. Executable cache assertions also
  reject three source-taxonomy false positives: 5821/10514 are `Net`, empty, `Harpoon`, while
  12776 is `Big Net`, empty, `Harpoon`; none receives lobster option-one dispatch. Each catch uses
  the shared protected loop, revalidates boosted level, pot, adjacency and capacity after five
  ticks, awards 90 XP and inserts raw lobster transactionally without consuming bait or the pot.
- Three new integration scenarios cover cache identity and negative dispatch, the lobster table,
  real option-one five-tick success/XP/message behavior and result-tick level/tool/capacity/
  distance invalidation. The Fishing suite now contains 19 scenarios. The current success line is
  a bounded legacy template; matching-client wording, audiovisual and packet presentation,
  250–530-tick relocation, tick manipulation, interruption and the entire harpoon family remain
  researched under manual case 71.
- The affected formatting/config/Fishing/parity gate passed in 10s with 337 actionable tasks: 7
  executed, 6 restored from cache and 324 up-to-date. The full XML audit contains 1,647 unit
  scenarios across 106 classes and 1,444 integration scenarios across 179 classes, with zero
  failures, errors or skips. The manifest now tracks 332 unique groups: 48 discovered, 77
  researched, 1 unit-tested, 205 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m35s: 793
  actionable tasks, with 114 executed, 6 restored from cache and 673 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Ordinary cage-spot harpoon Fishing

- Pinned final pre-cutoff cage/harpoon-spot, raw-tuna, raw-swordfish, ordinary-harpoon,
  barb-tail-harpoon, big-swordfish and Fishing revisions 14997261, 14882835, 14930407, 14838279,
  14978282, 14957577 and 14980550. Together they establish the 18-NPC cutoff family, ordinary
  five-tick cadence, swordfish-before-tuna selection, boosted levels 50/35, endpoints 4/48 and
  8/64, 100/80 XP, exact raw-fish messages, members-only barb-tail eligibility and the members-only
  one-in-2500 big-swordfish replacement after a successful swordfish roll.
- Added typed ordinary/barb-tail harpoon, tuna, swordfish, big-swordfish and sequence references;
  direct operation-three dispatch across all 18 cutoff identities; carried/worn barb-tail support;
  and a generic rare-replacement contract. Cache assertions pin objects 311, 359, 371, 7991 and
  10129 plus sequences 618 and 5108, while local revision-matched RuneLite independently confirms
  those item and animation identities. NPC 10514 remains excluded from this source family despite
  its cache `Net/Harpoon` operations pending its separate shark-table slice.
- Six new integration scenarios cover the cache/event/table contract, ordinary tuna success,
  swordfish-to-tuna cascade ordering, big-swordfish replacement and free-world suppression,
  carried/worn barb-tail animation and membership, and result-tick level/tool/capacity
  revalidation. The Fishing suite now contains 25 scenarios. Fishing Guild's invisible +7;
  dragon/infernal/crystal rates, charges and cooking; barehanded Strength XP; Rada/spirit-flake
  doubling; conditional clue-bottle/heron rolls; relocation and exact client presentation remain
  outside this production claim under manual case 71.
- The affected formatting/config/Fishing/parity gate passed in 11s with 337 actionable tasks: 8
  executed, 6 restored from cache and 323 up-to-date. The full XML audit contains 1,647 unit
  scenarios across 106 classes and 1,450 integration scenarios across 179 classes, including all
  25 Fishing scenarios, with zero failures, errors or skips. The manifest now tracks 333 unique
  groups: 48 discovered, 77 researched, 1 unit-tested, 206 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 5m35s: 793 actionable tasks, with 114 executed, 6 restored from cache and 673
  up-to-date. RuneLite was neither launched nor modified, and the user's running RSMod server was
  untouched.

## 2026-09-11 — Ordinary big-net/harpoon shark Fishing

- Pinned final pre-cutoff big-net/harpoon-spot, raw-shark, big-shark, ordinary-harpoon,
  barb-tail-harpoon and Fishing revisions 14992635, 14926495, 14930403, 14838279, 14978282 and
  14980550. Together they establish the exact 20-NPC cutoff family, boosted level 76, 110 XP,
  success endpoints 3/40, ordinary five-tick attempts, exact raw-shark feedback, members-only
  ordinary and barb-tail routes, and the members-only one-in-5000 big-shark replacement after a
  successful shark roll.
- Added typed references and direct operation-three dispatch for all 20 source identities, using
  regular sequence 618 and carried/worn barb-tail sequence 5108. Cache assertions pin raw shark
  383, big shark 7993, ordinary harpoon 311 and barb-tail harpoon 10129; local revision-matched
  RuneLite independently confirms those objects and animations. Cache 233.4's unusual NPC 5820
  has `Cage`, empty, `Harpoon` rather than the other identities' `Big Net`/`Net`, empty,
  `Harpoon`; that anomaly is explicitly asserted and no unsupported option-one route is inferred.
- Five new integration scenarios prove the exact cache/event/method contract, five-tick shark
  success and feedback, one-in-5000 big-shark replacement with free-world suppression,
  carried/worn barb-tail membership and animation, and result-tick level/tool/capacity/distance
  revalidation. The Fishing suite now contains 30 scenarios. The nine-roll big-net table,
  Fishing Guild bonus, specialist harpoons, barehanded Strength XP, Rada/spirit-flake doubling,
  fish barrel, secondary loot, relocation and exact client presentation remain separate bounded
  work under manual case 72.
- The affected formatting/config/Fishing/parity gate passed in 3s with 337 actionable tasks: 6
  executed, 6 restored from cache and 325 up-to-date. The full XML audit contains 1,647 unit
  scenarios across 106 classes and 1,455 integration scenarios across 179 classes, including all
  30 Fishing scenarios, with zero failures, errors or skips. The manifest now tracks 336 unique
  groups: 48 discovered, 79 researched, 1 unit-tested, 207 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 5m36s: 793 actionable tasks, with 114 executed, 6 restored from cache and 673
  up-to-date. RuneLite was neither launched nor modified, and the user's running RSMod server was
  untouched.

## 2026-09-11 — Ordinary big-net Fishing

- Pinned final pre-cutoff big-net/harpoon-spot, big-fishing-net, raw-mackerel, raw-cod, raw-bass,
  big-bass, leather-boots, leather-gloves, seaweed, oyster and casket revisions 14992635,
  14997131, 14935361, 14958042, 14977847, 14957574, 14799016, 14799037, 14848011, 14861581 and
  14858594. Together with the source module they establish the exact nine independent rolls and
  order, duplicate mackerel roll, product requirements, XP and endpoints, worn charged-skills-
  necklace casket bonus, base-level-46 bass unlock and one-in-1000 big-bass replacement.
- Added cache-exact big-net, product, charged-necklace and sequence references plus direct first-
  option dispatch for the 19 compatible members of the 20-NPC source family. NPC 5820 remains
  explicitly excluded because cache 233.4 labels its first operation `Cage`. The dedicated
  members-only five-tick method evaluates all eligible rolls in order, applies the necklace bonus,
  performs rare bass replacement, revalidates state and commits only the outputs that fit in the
  remaining inventory slots as one protected transaction.
- Six new integration scenarios prove the cache/event/table contract, all nine rolls and exact XP,
  independent duplicate mackerel, charged-necklace casket modification, base-level bass gating,
  one-in-1000 big bass, atomic partial-capacity handling and result-tick membership/level/tool/
  distance/capacity revalidation. The focused Fishing suite now contains 36 scenarios. Literal
  matching-client junk wording and partial-capacity presentation remain isolated in manual case
  72 rather than being claimed exact.
- The affected formatting/config/Fishing/parity gate passed in 10s with 337 actionable tasks: 7
  executed, 6 restored from cache and 324 up-to-date. The full XML audit contains 1,647 unit
  scenarios across 106 classes and 1,461 integration scenarios across 179 classes, including all
  36 Fishing scenarios, with zero failures, errors or skips. The manifest still tracks 336 unique
  groups: 48 discovered, 78 researched, 1 unit-tested, 208 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 5m23s: 793 actionable tasks, with 114 executed, 6 restored from cache and 673
  up-to-date. RuneLite was neither launched nor modified, and the user's running RSMod server was
  untouched.

## 2026-09-11 — Specialist harpoon catch rates

- Pinned final pre-cutoff dragon-harpoon, infernal-harpoon, crystal-harpoon, Fishing and raw-shark
  revisions 14874226, 14979405, 14969857, 14980550 and 14926495. They establish visible Fishing
  use levels 61/75/71, carried or worn use, dragon/infernal 20% and charged-crystal 35% rate
  increases, depleted dragon-rate fallback and the exact 1/256 flooring order evidenced by the
  level-76 shark values 32/256 regular, 38/256 dragon/infernal and 43/256 crystal.
- Added all 11 cache-233 base, Trailblazer, Trailblazer Reloaded, charged and depleted specialist
  item references and seven cache animations. Cage/swordfish and big-net/shark now share a
  strongest-usable tool table, exact quantized rate adjustment, visible tool-level checks, carried/
  worn support and result-tick validation of the originally selected tool. Stateful infernal auto-
  cooking/degradation and crystal charge consumption/conservation remain a separate bounded slice.
- Five added scenarios pin every cache object, sequence and tool-table row; the 38/256 and 43/256
  rate calculations; actual dragon/crystal boundary successes; strongest-tool selection; inactive-
  crystal fallback; and level/removal revalidation. The focused Fishing suite now contains 41
  scenarios.
- The affected formatting/config/Fishing/parity gate passed in 9s with 337 actionable tasks: 7
  executed, 6 restored from cache and 324 up-to-date. The full XML audit contains 1,647 unit
  scenarios across 106 classes and 1,466 integration scenarios across 179 classes, including all
  41 Fishing scenarios, with zero failures, errors or skips. The manifest now tracks 337 unique
  groups: 48 discovered, 78 researched, 1 unit-tested, 209 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 5m23s: 793 actionable tasks, with 114 executed, 6 restored from cache and 673
  up-to-date. RuneLite was neither launched nor modified, and the user's running RSMod server was
  untouched.

## 2026-09-11 — Fishing Guild ordinary-spot invisible boost

- Pinned final pre-cutoff Fishing, raw-tuna, raw-shark and big-net revisions 14980550, 14882835,
  14926495 and 14997131. Together they establish the Fishing Guild's invisible +7 as a success-
  chance modifier that stacks with visible boosts but does not unlock products. Cache 233.4
  identifies the currently supported ordinary guild spots as cage/harpoon NPC 1510 and big-net/
  harpoon NPC 1511.
- Added a typed two-NPC guild contract. Ordinary cascade rolls and every eligible independent big-
  net roll now receive +7 after the five-tick result revalidation, while product gates continue to
  use visible or explicitly documented base Fishing levels. Specialist harpoon multiplication is
  applied after the guild boost and remains floored to the 1/256 probability grid: level-76 shark
  is 32/256 ordinarily, 35/256 in the guild and 42/256 there with a dragon/infernal-rate tool.
- Three scenarios and expanded table assertions pin both cache NPC identities, those three shark
  rates, an NPC-1510 tuna boundary success, the NPC-1511 shark no-unlock boundary and big-net boost/
  no-unlock behavior. The focused Fishing suite now contains 44 scenarios and passed in 10s with
  334 actionable tasks: 7 executed, 6 restored from cache and 321 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,469 integration
  scenarios across 179 classes, including all 44 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 338 unique groups: 48 discovered, 78 researched, 1 unit-tested,
  210 integration-tested and 1 verified. The serialized repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 5m25s: 793 actionable tasks,
  with 53 executed, 6 restored from cache and 734 up-to-date. RuneLite was neither launched nor
  modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Infernal harpoon passive and depletion

- Pinned final pre-cutoff infernal-harpoon, dragon-harpoon, Fishing and Cooking revisions 14979405,
  14874226, 14980550 and 14978581. They establish a post-catch one-in-three ordinary-fish auto-
  cook, no burn roll or raw output, half normal Cooking XP, charge use only when a fish is consumed,
  5,000 consumed fish per full tool and continued dragon-rate use after depletion.
- Added a versioned 13-bit per-item charge field and explicit cache mappings 21031→21033,
  25059→25367 and 30342→30343. Zero on a charged identity denotes a canonical new 5,000-use tool;
  subsequent state persists exact remaining consumptions. The selected carried or worn instance is
  revalidated and decremented, and its final use preserves style while changing to the empty type.
- Tuna, swordfish and shark now award exactly 50, 70 and 105 Cooking XP when consumed. Big-fish
  replacement resolves first, so big swordfish and big shark are retained without Cooking XP or
  charge use. Failed passive rolls retain both the ordinary raw fish and all charges.
- Four added scenarios and expanded config assertions bring the focused Fishing suite to 48. The
  focused run passed in 9s with 331 actionable tasks: 4 executed, 6 restored from cache and 321 up-
  to-date. The full XML audit contains 1,647 unit scenarios across 106 classes and 1,473 integration
  scenarios across 179 classes, including all 48 Fishing scenarios, with zero failures, errors or
  skips.
- The manifest now tracks 339 unique groups: 48 discovered, 78 researched, 1 unit-tested, 211
  integration-tested and 1 verified. The serialized repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 5m31s: 793 actionable tasks,
  with 117 executed, 6 restored from cache and 670 up-to-date. RuneLite was neither launched nor
  modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Crystal harpoon depletion core

- Pinned final pre-cutoff crystal-harpoon, crystal-equipment, Fishing, big-swordfish and big-shark
  revisions 14969857, 14889770, 14980550, 14957577 and 14930403. They establish 10,000 charges on
  a newly charged tool, a 20,000-charge maximum, one charge per item obtained, ordinary and big-
  fish charge use, and the inactive crystal harpoon retaining the dragon-harpoon catch-rate bonus.
- Added a versioned 15-bit per-item charge field and the exact cache mapping 23762→23764. Zero on
  the charged identity denotes a canonical new 10,000-use tool; subsequent state persists the
  exact remaining uses up to 20,000. A successful catch decrements the exact selected carried or
  worn instance, and its final use changes that instance to the inactive type. Failed rolls do not
  consume a charge.
- Four new scenarios plus expanded config/table coverage pin the carried catch, failed attempt,
  final worn charge and big-swordfish paths. The focused Fishing suite now contains 52 scenarios
  and passed in 13s with 336 actionable tasks: 103 executed, 6 restored from cache and 227 up-to-
  date. Crystal-shard charging, dismantling/inspection, and signet/lucky-penny charge conservation
  remain explicitly open rather than being inferred into this bounded slice.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,477 integration
  scenarios across 179 classes, including all 52 Fishing scenarios, with zero failures, errors,
  skips or duplicate manifest IDs. The manifest now tracks 340 unique groups: 48 discovered, 78
  researched, 1 unit-tested, 212 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m34s: 793
  actionable tasks, with 117 executed, 6 restored from cache and 670 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Fish sack barrel lifecycle core

- Pinned final pre-cutoff Fish barrel revision 14978270 and official Fishing Skill Boss and Easter
  Event revision 14253445. They establish fish-sack assembly into an equipable combined barrel,
  no assembly confirmation, cape-slot equipment and Configure/Dismantle component recovery. Cache
  233.4 fixes sack 22838, closed/open combined forms 25585/25587, wear position 1, held operations
  `Fill/Wear/Configure/Open-or-Close/Destroy` and worn `Check/Open-or-Close` parameters.
- Added members-only, item-order-independent assembly that preserves closed/open state and the
  permanent 28-fish store. Both combined forms now support held Fill, Configure/Dismantle,
  Open/Close and Destroy; the equipped form supports Check and Open/Close and an open worn barrel
  accepts ordinary catches even from a full inventory. Dismantling atomically returns the matching
  ordinary barrel and fish sack, rejects insufficient inventory space without mutation and keeps
  contents intact. Exact Configure presentation remains explicitly bounded by manual case 75.
- Six added scenarios bring the focused Fishing suite to 123. They pin cache identities,
  operations and wear metadata; closed/open assembly, both item orders, membership and no success
  message; carried/worn dispatch and full-inventory acquisition; content/state preservation;
  dismantle capacity safety; and Destroy clearing. The focused six-test run passed in 10s with 650
  actionable tasks: 7 executed, 6 restored from cache and 637 up-to-date. Formatting, parity and
  the complete Fishing suite then passed in 11s with 651 actionable tasks: 5 executed, 6 restored
  from cache and 640 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,548 integration
  scenarios across 179 classes, including all 123 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 360 unique groups with no duplicate IDs: 48 discovered, 80
  researched, 1 unit-tested, 230 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m34s: 793
  actionable tasks, with 115 executed, 6 restored from cache and 672 up-to-date. A runtime smoke
  then loaded all 197 scripts, bound port 43594 and reported revision 233. Only the workspace
  RuneLite checkout was launched; its log confirms local address 127.0.0.1:43594, the bundled
  revision-233 gamepack, the workspace RSMod RSA key and successful client initialization. The Java
  game window was unavailable to the automation surface, so login and the in-game case-75 capture
  remain manual rather than being promoted to client-tested.

## 2026-09-11 — Barbarian heavy-rod Fishing core

- Pinned final pre-cutoff Barbarian Training, Barbarian fishing-spot and all three leaping-fish
  revisions. Together they fix the two spots, both rods, Otto-state prerequisite, five-item bait
  priority, boosted Fishing versus base Strength/Agility gates, highest-first product cascade,
  1/256 success endpoints and exact Fishing/Strength/Agility XP. Cache 233.4 pins NPCs 1542/7323,
  all objects, varbit 3758 and regular/pearl sequence families 9349/9350 and 8190/8193. Pinned Quest
  Helper source supplies the exact first-catch state transition and feedback.
- Production now dispatches both spots through first-option `Use-rod`, selects a carried barbarian
  rod or worn pearl barbarian rod, consumes the first available bait in exact source order and
  performs five-tick protected attempts. Candidate selection respects boosted Fishing and base
  support skills, awards all three XP values once per primary catch, handles the last bait as an
  atomic full-inventory output slot and advances assigned training on the first catch. Exact
  rejection wording and cast/result choreography remain bounded matching-client observations.
- Six added scenarios bring the focused Fishing suite to 104. They pin cache data and dispatch,
  state/world and boost/base gates, all fish and XP tuples, bait priority, full-inventory atomicity,
  pearl-rod use, first-catch progression and bonus-fish coexistence without duplicate bait or XP.
  Formatting, parity and the focused Fishing suite passed in 13s with 651 actionable tasks: 13
  executed, 6 restored from cache and 632 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,529 integration
  scenarios across 179 classes, including all 104 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 354 unique groups with no duplicate IDs: 48 discovered, 78
  researched, 1 unit-tested, 226 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m36s: 793
  actionable tasks, with 115 executed, 6 restored from cache and 672 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Infernal harpoon creation core

- Pinned final pre-cutoff infernal-harpoon and dragon-harpoon revisions 14979405 and 14874226,
  supported by the cache-period Cooking experience table. They establish the permanent base recipe:
  one dragon harpoon plus one smouldering stone, members-only unboosted level 75 Fishing and level
  85 Cooking, 200 Fishing XP and 350 Cooking XP. Cache 233.4 pins inputs 21028/13233 and output
  21031.
- Exposed the previously unreferenced smouldering-stone cache symbol and registered the recipe with
  RSMod's held-item plugin syntax. Either item-use order resolves the exact selected slots and runs
  one atomic two-input-to-one-output transaction. The infernal harpoon replaces the dragon harpoon
  even in a full inventory and inherits the existing canonical 5,000-charge zero-state convention.
- Three scenarios pin cache identities/dispatch, both held-use orders, exact full-inventory output
  placement, initial charges, both XP awards, membership, unboosted requirements and stale-input
  safety. The focused Fishing suite now contains 55 scenarios and passed in 10s with 336 actionable
  tasks: 7 executed, 6 restored from cache and 323 up-to-date. Recharging, ornamented creation and
  exact client presentation remain separate bounded work.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,480 integration
  scenarios across 179 classes, including all 55 Fishing scenarios, with zero failures, errors,
  skips or duplicate manifest IDs. The manifest now tracks 341 unique groups: 48 discovered, 78
  researched, 1 unit-tested, 213 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m30s: 793
  actionable tasks, with 115 executed, 6 restored from cache and 672 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Rada's blessing ordinary single-output core

- Pinned the cache-period Fishing revision 14980550, Jagex's Kebos Lowlands announcement and big-
  bass revision 14957574. Together they establish equipped-only activation, exact tier-1-through-4
  chances of 2/4/6/8 percent, one extra copy of the selected fish including a big-fish replacement,
  and no XP for that copy. Cache 233.4 pins the four objects to 22941/22943/22945/22947.
- Added an RSMod-native typed blessing table and post-selection multiplier to ordinary single-
  output Fishing. The two outputs are inserted atomically while Fishing XP is awarded once. A
  carried blessing does not activate, and one remaining output slot preserves the base catch.
- Following the deep-research workflow's evidence boundary, the production slice deliberately
  excludes active infernal/crystal tools and big-net's independent multi-output table until exact
  charge/passive/capacity ordering is separately sourced. Spirit flakes are researched but remain
  open with those coexistence rules.
- Six added scenarios bring the focused Fishing suite to 61. They pin all four identities/rates,
  the 8-percent success/failure boundary, worn-versus-carried behavior, one XP award, big-
  swordfish duplication, one-slot safety and the charged-crystal exclusion. The focused formatting,
  parity and Fishing gate passed in 11s with 335 actionable tasks: 13 executed, 6 restored from
  cache and 316 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,486 integration
  scenarios across 179 classes, including all 61 Fishing scenarios, with zero failures, errors,
  skips or duplicate manifest IDs. The manifest now tracks 342 unique groups: 48 discovered, 78
  researched, 1 unit-tested, 214 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m33s: 793
  actionable tasks, with 53 executed, 6 restored from cache and 734 up-to-date. RuneLite was neither
  launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Spirit flakes ordinary single-output core

- Pinned final pre-cutoff Spirit flakes revision 14908234 and Jagex's release announcement. They
  establish members-only carried object 25588, a 50-percent double-catch chance, one flake consumed
  on every eligible catch whether the roll succeeds or fails, no extra XP, unchanged ordinary bait
  use, non-fish eligibility and the one-output-slot no-roll/no-consume exception.
- Extended the ordinary single-output transaction to consume the flake and add one or two copies of
  the already-selected result atomically. Rada's equipped tier adds 2–8 percentage points on the
  same 0–99 roll, producing an exact 52–58-percent double-catch chance rather than a third fish.
  Big-fish replacement resolves before multiplication.
- Following the deep-research workflow's evidence boundary, active infernal/crystal tools remain
  excluded pending exact per-output auto-cook and charge ordering. Big net remains separate because
  its nine independent primary rolls and partial-capacity acceptance need a dedicated multiplier
  transaction. Every currently supported ordinary single-output spot lies outside the cutoff
  source's Tempoross, Fishing Trawler, Chambers of Xeric and Drift Net Fishing exclusions.
- Five added scenarios bring the focused Fishing suite to 66. They pin object 25588, the 49/50
  boundary, consumption on successful and failed bonus rolls, one-bait use, one XP award, additive
  Rada-4 57/58 boundaries, big-swordfish duplication and the one-slot no-consume exception. The
  focused suite passed in 15s with 334 actionable tasks: 101 executed, 6 restored from cache and 227
  up-to-date; the subsequent formatting/parity/Fishing gate also passed.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,491 integration
  scenarios across 179 classes, including all 66 Fishing scenarios, with zero failures, errors,
  skips or duplicate manifest IDs. The manifest now tracks 343 unique groups: 48 discovered, 78
  researched, 1 unit-tested, 215 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m43s: 793
  actionable tasks, with 117 executed, 6 restored from cache and 670 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Rada and spirit-flake ordinary big-net multiplication

- Pinned final pre-cutoff Spirit flakes revision 14908234, Civitas illa Fortis fishing-spot
  revision 14992989 and big-bass revision 14957574. Together they establish that the multiplier
  applies to non-fish results, consumes one flake for each eligible catch even after a failed roll,
  gives no XP for the second output, adds the equipped Rada tier and duplicates an already-selected
  big fish. The Civitas spot is evidence for non-fish eligibility only; it is not promoted as an
  implemented fishing method.
- Extended the ordinary big-net transaction to process accepted primary results in source order.
  Each result with at least two remaining output slots receives one additive `50 + Rada percent`
  roll and reserves one flake. A doubled output consumes capacity before later primary rolls, while
  XP and messages remain once per primary result. Fish, junk and big bass share this path.
- Following the deep-research workflow's evidence boundary, the transaction uses inventory space
  present before the action and does not assume that consuming the last flake exposes a new slot in
  the same action. That detail, multiplier feedback and packet order remain matching-client work in
  manual case 72.
- Five added scenarios bring the focused Fishing suite to 71. They pin spirit-flake and Rada
  doubling of leather boots, flake consumption after a failed roll, no extra XP, doubled-output
  capacity priority, big-bass duplication and the one-slot no-roll/no-consume boundary. The focused
  Fishing suite passed in 11s with 334 actionable tasks: 12 executed, 6 restored from cache and 316
  up-to-date. The subsequent formatting, parity and Fishing gate passed in 2s with 335 actionable
  tasks: 4 executed, 6 restored from cache and 325 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,496 integration
  scenarios across 179 classes, including all 71 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 344 unique groups with no duplicate IDs: 48 discovered, 78
  researched, 1 unit-tested, 216 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m44s: 793
  actionable tasks, with 53 executed, 6 restored from cache and 734 up-to-date. RuneLite was neither
  launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Charged-harpoon catch-multiplier coexistence

- Pinned final pre-cutoff infernal-harpoon revision 14979405, crystal-harpoon revision 14969857,
  Spirit flakes revision 14908234 and Fishing revision 14980550. The infernal source explicitly
  gives every Rada/spirit-flake extra fish its own independent cook roll; the crystal source consumes
  one charge for every fish caught.
- Refactored the ordinary catch transaction so multiplication resolves before charged-tool
  passives. Each infernal output now independently rolls 1/3 auto-cooking, awards half normal
  Cooking XP and consumes a charge only if cooked. Crystal tools consume one charge per base or
  extra output. Fishing XP and the primary message remain once, spirit flakes remain one per
  eligible primary catch, and big fish retain their existing infernal exclusion.
- The first focused run exposed that an all-cooked, no-bait/no-flake result has no inventory changes
  and the inventory transaction API rejects an empty transaction. Production now bypasses only
  that empty transaction while still applying tool charges and XP; the original auto-cook and final-
  charge tests then passed unchanged.
- One new infernal scenario, one repurposed charged-crystal boundary scenario and one additional
  big-net multi-result regression bring the focused Fishing suite to 73. They pin one-cooked/one-raw
  independent infernal output with one charge and one half-XP award, Rada-doubled crystal output
  with two charges and one Fishing XP award, and one spirit flake reserved per accepted big-net
  primary catch. The corrected focused suite passed in 11s with 334 actionable tasks: 12 executed,
  6 restored from cache and 316 up-to-date; formatting, parity and Fishing then passed in 3s with
  335 actionable tasks: 4 executed, 6 restored from cache and 325 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,498 integration
  scenarios across 179 classes, including all 73 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 345 unique groups with no duplicate IDs: 48 discovered, 78
  researched, 1 unit-tested, 217 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m40s: 793
  actionable tasks, with 53 executed, 6 restored from cache and 734 up-to-date. RuneLite was neither
  launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Infernal harpoon recharge core

- Pinned final pre-cutoff infernal-harpoon revision 14979405, Trailblazer ornament revision
  14853049 and Reloaded revision 14811396. They establish either a smouldering stone or dragon
  harpoon as a 5,000-charge recharge material and identify the base, Trailblazer and Reloaded
  charged/depleted style pairs; ornamentation is cosmetic rather than a gameplay modifier.
- Registered all six depleted-style/material held-item pairs. Either selection order is accepted,
  membership and exact selected slots are revalidated, and one atomic transaction consumes both
  inputs and writes the matching full charged style into the depleted tool's slot. Recharging has no
  skill requirement or XP and remains safe in a full inventory.
- Two added scenarios bring the focused Fishing suite to 75. They pin all six dispatch keys, every
  style/material/order combination, canonical 5,000-charge state, level-zero and zero-XP behavior,
  members-only rejection and stale-input preservation. The focused suite passed in 11s with 334
  actionable tasks: 12 executed, 6 restored from cache and 316 up-to-date. Formatting, parity and
  Fishing then passed in 3s with 335 actionable tasks: 6 executed, 6 restored from cache and 323 up-
  to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,500 integration
  scenarios across 179 classes, including all 75 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 346 unique groups with no duplicate IDs: 48 discovered, 78
  researched, 1 unit-tested, 218 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m38s: 793
  actionable tasks, with 53 executed, 6 restored from cache and 734 up-to-date. RuneLite was neither
  launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Infernal harpoon Trailblazer ornament core

- Pinned final pre-cutoff Trailblazer tool ornament kit revision 14787699 and ornamented infernal-
  harpoon revision 14853049. They establish members-only kit 25090, charged/depleted outputs 25059
  and 25367, cosmetic-only behavior, a one-tick recipe and reversibility. Cache 233.4 pins base
  inputs 21031 and 21033.
- Added the typed kit reference and both held-item dispatch routes. Either selection order is
  accepted, membership and exact slots are revalidated, and one atomic full-inventory transaction
  creates the matching original Trailblazer style in the base-tool slot. A charged input's packed
  state is copied exactly, preserving partial charges; there is no level requirement or XP.
- Two added scenarios bring the focused Fishing suite to 77. They pin the kit identity, both
  dispatch keys, charged/depleted outputs, both selection orders, full-inventory operation, exact
  123-charge preservation, zero-level/zero-XP behavior, members-only rejection and stale inputs.
  The focused suite passed in 15s with 334 actionable tasks: 101 executed, 6 restored from cache and
  227 up-to-date. Formatting, parity and Fishing then passed in 3s with 335 actionable tasks: 6
  executed, 6 restored from cache and 323 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,502 integration
  scenarios across 179 classes, including all 77 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 347 unique groups with no duplicate IDs: 48 discovered, 78
  researched, 1 unit-tested, 219 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m45s: 793
  actionable tasks, with 117 executed, 6 restored from cache and 670 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Infernal harpoon ornament lifecycle core

- Pinned final pre-cutoff Trailblazer tool ornament kit revision 14787699, ornamented infernal-
  harpoon revision 14853049 and Reloaded revision 14811396. They establish reversible component
  recovery, the charged/depleted original and Reloaded identities, two-way Change-Style behavior
  and the distinct inventory/worn operation surfaces. Direct cache 233.4 inspection pinned the
  complete four item-option arrays and `wear_op1`/`wear_op2` values.
- Registered Revert for all four ornamented identities and Change-Style for the same four from
  cache operation three in inventory, with Change-Style on inventory operation four and worn
  operation three. Revert atomically returns the corresponding plain charged/depleted tool
  plus kit 25090 and leaves a full inventory unchanged; Change-Style remains one-for-one in either
  direction. Every path is members-only, has no skill or XP gate and copies packed charge state
  exactly.
- Five added scenarios bring the focused Fishing suite to 82. They pin the cache operation arrays,
  worn params, all Revert inputs, component return, full-inventory failure, every charged/depleted
  two-way inventory and worn style transition, state preservation, zero-level/zero-XP behavior and
  membership rejection. The first cache-contract run found that an absent resolved `wear_op1`
  reads as the empty-string default rather than null; after correcting only that assertion, the
  focused suite passed in 11s with 334 actionable tasks: 7 executed, 6 restored from cache and 321
  up-to-date. Formatting, parity and Fishing then passed in 4s with 651 actionable tasks: 4
  executed, 6 restored from cache and 641 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,507 integration
  scenarios across 179 classes, including all 82 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 348 unique groups with no duplicate IDs: 48 discovered, 78
  researched, 1 unit-tested, 220 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m31s: 793
  actionable tasks, with 52 executed, 6 restored from cache and 735 up-to-date. RuneLite was neither
  launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Infernal harpoon inspection and ornamented equip core

- Pinned final pre-cutoff infernal-harpoon revision 14979405, ornamented revision 14853049 and
  Reloaded revision 14811396. Together they establish 5,000 initial uses, successful-auto-cook
  charge consumption, Check on each charged visual style and no Check on depleted forms. Direct
  cache 233.4 inspection fixed the unusual operation arrays: base charged Check is held operation
  three; ornamented charged Check is held operation two; worn Check is operation two; all
  ornamented Wield actions are held operation one.
- Registered those exact carried/worn Check routes and explicit first-option Wield for all four
  ornamented charged/depleted identities. Inspection reads canonical zero-state as 5,000 and
  reports partial persisted state without mutation. Equipping preserves packed state. Both paths
  are members-only. The literal charge sentence is deliberately recorded as a bounded inference
  pending matching-client capture.
- Three added scenarios bring the focused Fishing suite to 85. They pin the raw operation arrays,
  worn params, all three charged carried/worn inspection paths, 5,000 and 123-charge reporting,
  membership rejection and all four cache-shifted Wield paths with state preservation. The first
  focused Wield run exposed the earlier registration offset; all Revert and Change-Style
  registrations and their tests were corrected to the cache's third/fourth held slots. The
  corrected focused suite passed in 12s with 650 actionable tasks: 12 executed, 6 restored from
  cache and 632 up-to-date. Formatting, parity and Fishing then passed in 3s with 651 actionable
  tasks: 6 executed, 6 restored from cache and 639 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,510 integration
  scenarios across 179 classes, including all 85 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 349 unique groups with no duplicate IDs: 48 discovered, 78
  researched, 1 unit-tested, 221 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m32s: 793
  actionable tasks, with 52 executed, 6 restored from cache and 735 up-to-date. RuneLite was neither
  launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Crystal harpoon inspection and Revert core

- Pinned final pre-cutoff crystal-harpoon revision 14969857 and crystal-equipment revision
  14889770. They establish active-only carried/worn Check, active/inactive Revert at any time,
  recovery of the dragon tool plus crystal tool seed and forfeiture of every loaded shard. Direct
  cache 233.4 inspection fixed active `null, Wield, Check, Revert, null`, inactive `null, Wield,
  null, Revert, null`, active worn Check and the inactive empty worn-operation default. Added typed
  crystal tool seed 23953.
- Registered active inventory Check on held operation three, active worn Check on operation two and
  active/inactive Revert on held operation four. Check reads canonical 10,000 or exact partial item
  state without mutation. Revert atomically replaces the tool with dragon harpoon 21028 and adds
  seed 23953, rejecting a full inventory without mutation. Both are members-only; exact messages
  and Revert confirmation remain bounded pending matching-client capture.
- Three added scenarios bring the focused Fishing suite to 88. They pin raw item-operation arrays,
  worn params, seed identity, 10,000/12,345 inspection, carried/worn dispatch, both Revert inputs,
  component recovery, full-inventory safety, state forfeiture and membership rejection. The focused
  suite passed in 16s with 650 actionable tasks: 103 executed, 6 restored from cache and 541 up-to-
  date. Formatting, parity and Fishing then passed in 3s with 651 actionable tasks: 6 executed, 6
  restored from cache and 639 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,513 integration
  scenarios across 179 classes, including all 88 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 350 unique groups with no duplicate IDs: 48 discovered, 78
  researched, 1 unit-tested, 222 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m39s: 793
  actionable tasks, with 114 executed, 6 restored from cache and 673 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Crystal harpoon shard-charging core

- Reused pinned crystal-harpoon revision 14969857 and crystal-equipment revision 14889770. They
  establish shard use on active or inactive tools at any time, exactly 100 charges per shard and a
  20,000-use maximum; cache 233.4 pins crystal shard 23962 and active/inactive harpoons 23762/23764.
- Registered both held-use pairs with selected-item-order independence and a bounded quantity
  dialogue. The implementation limits input to the selected shard stack and remaining capacity,
  caps a partial final shard at 20,000, activates inactive tools from zero, operates atomically in a
  full inventory and revalidates both selected objects after dialogue suspension. Membership and
  already-full rejection are explicit; literal prompt and feedback remain matching-client work.
- Two added scenarios bring the focused Fishing suite to 90. They pin both event keys and item
  orders, active 19,850 plus two shards capping at 20,000, inactive plus two shards becoming active
  at 200, oversized input clamping, full-inventory operation, free/full rejection and stale-input
  preservation. After correcting the test harness to let protected access own the suspended
  coroutine, the focused suite passed in 12s with 650 actionable tasks: 7 executed, 6 restored from
  cache and 637 up-to-date. Formatting, parity and Fishing then passed in 3s with 651 actionable
  tasks: 4 executed, 6 restored from cache and 641 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,515 integration
  scenarios across 179 classes, including all 90 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 351 unique groups with no duplicate IDs: 48 discovered, 78
  researched, 1 unit-tested, 223 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m32s: 793
  actionable tasks, with 52 executed, 6 restored from cache and 735 up-to-date. RuneLite was neither
  launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Fishing-tool charge conservation core

- Pinned final pre-cutoff elven-signet revision 14989911, celestial-signet revision 14888892 and
  Ghommal's-lucky-penny revision 14964658. They establish a 10% crystal-equipment save from the
  elven effect, the same effect on both celestial forms, a 5% save from the equipped penny for
  crystal and infernal tools, and independent signet/penny rolls. Cache 233.4 pins ring-slot IDs
  23943/25543/25545 and ammunition-slot penny 27544 (`ca_ammoslot`).
- Charge conservation now runs once per charge-producing fish. A crystal catch rolls an equipped
  elven or either celestial signet at one in ten and the equipped penny at one in twenty; when both
  are present both rolls are evaluated independently and consumption occurs only if both fail.
  Infernal tools receive the penny roll only after a fish succeeds on its independent auto-cook
  roll. Multiplied catches evaluate each output separately, permitting zero, one or two charges to
  be consumed. No proc message was inferred because the cutoff sources publish none.
- Four added scenarios bring the focused Fishing suite to 94. They pin all four cache identities
  and wear slots, every eligible signet form, success/failure boundaries, penny coverage for both
  tool families and independent per-output stacking. The first combined focused run exposed a test-
  harness overlap between two still-running Fishing loops; after explicitly moving away and
  advancing the old loop to termination, the focused formatting and Fishing suite passed in 11s
  with 334 actionable tasks: 7 executed, 6 restored from cache and 321 up-to-date. Formatting,
  parity and Fishing then passed in 11s with 651 actionable tasks: 7 executed, 6 restored from cache
  and 638 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,519 integration
  scenarios across 179 classes, including all 94 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 352 unique groups with no duplicate IDs: 48 discovered, 78
  researched, 1 unit-tested, 224 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m36s: 793
  actionable tasks, with 114 executed, 6 restored from cache and 673 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Barehanded Barbarian harpooning core

- Pinned final pre-cutoff Barbarian Training revision 14988720 and Fishing revision 14980550,
  confirming Otto's harpoon-training unlock, the regular-harpoon speed/rate, boostable Fishing
  thresholds 55/70/96, unboostable Strength thresholds 35/50/76 and exact 8/10/11 Strength XP for
  tuna, swordfish and shark. Pinned Quest Helper commit 3f0992a for the assignment dialogue,
  successful first-catch signal and exact training message. Cache 233.4 fixes progress varbit 3757
  and the complete start/ready/tuna/swordfish/shark animation family at sequences 6703–6711.
- Production now falls back to bare hands only on members worlds with Otto's harpoon training at
  assigned-or-later state and no usable configured harpoon. It checks visible boosted Fishing at
  the ordinary requirement plus 20, base Strength at the ordinary Fishing requirement, preserves
  the regular tool's success curve and five-tick cadence, awards exact Strength XP once per primary
  catch, and advances the first assigned-state catch from one to two with the sourced feedback.
  Ordinary, barb-tail and specialist harpoons retain priority and do not award Strength XP. Cache
  sequence 6709 supplies the production loop; exact multi-stage catch choreography remains manual.
- Four added scenarios bring the focused Fishing suite to 98. They pin the state and nine sequence
  IDs, training/membership gates, Fishing boosts, nonboostable Strength, tuna/swordfish cascade,
  shark thresholds, exact Strength XP, first-catch state transition and ordinary-tool priority.
  The combined formatting, parity and focused Fishing run passed in 12s with 653 actionable tasks:
  17 executed, 6 restored from cache and 630 up-to-date.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,523 integration
  scenarios across 179 classes, including all 98 Fishing scenarios, with zero failures, errors or
  skips. The manifest now tracks 353 unique groups with no duplicate IDs: 48 discovered, 78
  researched, 1 unit-tested, 225 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m42s: 793
  actionable tasks, with 117 executed, 6 restored from cache and 670 up-to-date. RuneLite was
  neither launched nor modified, and the user's running RSMod server was untouched.

## 2026-09-11 — Holy Lake karambwanji Fishing core

- Pinned final pre-cutoff Raw karambwanji revision 14970676, Fishing spot (karambwanji) revision
  14992999, Fishing revision 14980550, Spirit flakes revision 14908234 and Game tick revision
  14986190. Together they establish level 5 and a small fishing net, a six-tick cycle, 5 Fishing
  XP, quantity `floor(current Fishing level / 5) + 1`, raw-karambwanji success endpoints 100/250,
  raw-shrimp fallback endpoints 48/256, whole-catch Rada doubling and additive Rada/spirit-flake
  chances. Cache 233.4 pins Holy Lake NPC 4710, raw karambwanji 3150, raw shrimps 317, small fishing
  net 303 and sequence 621.
- Added a dedicated first-option handler and frozen NPC editor for the three Holy Lake spots. The
  loop enforces membership, visible Fishing level, tool and capacity requirements; faces the spot,
  animates every six ticks, revalidates protected state and distance, performs the sourced
  karambwanji-then-shrimp success cascade, atomically adds the level-scaled stack and awards 5 XP
  once per accepted result. Rada and spirit flakes double the complete stack without extra XP;
  their chances add, and one spirit flake is consumed on each eligible accepted catch whether its
  doubling roll succeeds or fails.
- The two final pre-cutoff pages conflict on the edge case where the inventory is full but already
  contains a karambwanji stack: the later September spot revision says shrimp are no longer caught,
  while the August item revision says a shrimp may be caught and silently discarded. Production
  follows the later account, suppresses the shrimp roll and keeps the action active. The normalized
  karambwanji catch line remains explicitly inferred pending matching-client capture.
- Eight scenarios pin cache identity, option dispatch, requirements, capacity behavior, six-tick
  timing, boosted quantities beyond level 99, exact XP, multiplier boundaries and consumption,
  shrimp fallback, full-inventory continuation and distance revalidation. The focused eight-test
  class and the complete Fishing integration module passed. The full XML audit contains 1,647 unit
  scenarios across 106 classes and 1,556 integration scenarios across 180 classes, with zero
  failures, errors or skips.
- The manifest now tracks 362 unique groups with no duplicate IDs: 48 discovered, 81 researched,
  1 unit-tested, 231 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m48s: 793
  actionable tasks, with 54 executed, 6 restored from cache and 733 up-to-date. The running server
  was then restarted in place and loaded 54 plugin modules and 197 scripts on port 43594 at revision
  233. The single already-open workspace RuneLite process was left running and untouched; manual
  cache-client case 76 remains pending because its Java window is unavailable to automation.

## 2026-09-11 — Post-quest karambwan Fishing core

- Pinned final pre-cutoff Raw karambwan, Karambwan vessel, Fishing spot (karambwan), Fishing and
  Game tick revisions 14995089, 14917620, 14996427, 14980550 and 14986190. Together they establish
  members-only visible level 65, a loaded vessel and raw-karambwanji bait, 50 Fishing XP, 5/160
  success endpoints, one bait consumed on success or failure, the exact failed-roll line and a
  four-tick resource roll. Cache 233.4 pins raw karambwan 3142, raw karambwanji 3150, empty/loaded
  vessels 3157/3159, main spots 4712/4713, Lubufu's quest spot 4714, sequence 1193, Tai Bwo Wannai
  Trio varp 320 and medium Karamja Diary varbit 3588.
- Added typed config references, a frozen post-quest NPC group and a dedicated injected Fishing
  service. Empty vessels load atomically in either held-item order, including in a full inventory.
  The main-ground first-option loop requires a completed quest, membership, visible level 65, a
  loaded vessel and output capacity; it faces the spot, emits the lowering line, animates with
  cache sequence 1193, waits four ticks and revalidates distance and all mutable state.
- Every resolved attempt removes the loaded vessel. A loose raw karambwanji is consumed to rebuild
  a loaded vessel automatically; without one, the vessel returns empty and the action stops. Failed
  attempts consume bait and send the sourced snatch line. Successful attempts add raw karambwan,
  award exactly 50 XP and set the medium-diary varbit. Standard equipped-Rada and carried-spirit-
  flake multiplication applies additively without extra XP, and an open carried/worn fish barrel
  accepts one or both outputs from a full inventory.
- Quest Helper's pre-cutoff state model exposes no stable client variable for Lubufu's exact
  learned-technique substep. The quest-only NPC 4714 is therefore deliberately unregistered rather
  than guessed; its acquisition/dialogue lifecycle remains under manual case 77. Exact start/catch
  presentation, audio, the four-source/three-destination main-spot relocation anomaly, conditional
  clue bottles/heron, packet order, tick manipulation and broad interruption behavior also remain
  in the researched lifecycle row.
- Eight added scenarios pin cache identities and dispatch, item-order-independent vessel loading,
  membership/quest/level/tool/capacity gates, exact four-tick timing, success/failure bait use,
  automatic reload and final empty transition, XP/diary state, Rada/spirit-flake doubling into a
  fish barrel and distance revalidation. The focused class and complete Fishing module passed; the
  module now has 139 scenarios across three classes with zero failures, errors or skips.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,564 integration
  scenarios across 181 classes, with zero failures, errors or skips. The manifest now tracks 364
  unique groups with no duplicate IDs: 48 discovered, 82 researched, 1 unit-tested, 232
  integration-tested and 1 verified. The serialized repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 5m36s: 793 actionable tasks,
  with 114 executed, 6 restored from cache and 673 up-to-date. The running server was then
  restarted in place and loaded 54 plugin modules and 197 scripts on port 43594 at revision 233.
  The single already-open workspace RuneLite process was left running and untouched; manual cache-
  client case 77 remains pending because its Java window is unavailable to automation.

## 2026-09-11 — Cave and Mort Myre eel Fishing core

- Pinned final pre-cutoff Raw slimy eel, Raw cave eel, Fishing spot (frog spawn, eels), Fishing spot
  (swamp), Fishing and Game tick revisions 14964519, 14882826, 14992997, 14993004, 14980550 and
  14986190. Together they establish members-only ordinary/pearl rod plus bait, five-tick attempts,
  equal 10/80 endpoints, the ordered cave-eel-before-slimy-eel cascade, level 38/28, 80 cave XP,
  65 Mort Myre XP and exact catch lines. Cache 233.4 independently pins frogspawn/cave NPCs
  1497–1500/10653, Mort Myre NPCs 2653–2655, their third/first operation slots and examines, raw
  eel objects 5001/3379, rod/bait objects 307/313, pearl rod 22846 and sequences 622/8188.
- Added typed NPC groups, fixed-resource editors and direct `Bait` dispatch for both spot families.
  Two data-driven methods reuse the established protected ordinary-fishing loop, including boosted
  visible levels, carried ordinary or carried/worn pearl rods, success-only bait, atomic capacity,
  five-tick first/repeat rolls, protected distance/tool/state revalidation, Rada/spirit-flake
  multiplication and open-fish-barrel routing without extra XP.
- Eight new scenarios pin cache contracts and dispatch, catch tables/order, exact timing and XP,
  success/failure bait behavior, membership/level/tool/capacity gates, pearl animation, distance
  interruption and doubled barrel routing. The focused class and complete Fishing module passed;
  Fishing now has 147 scenarios across four classes with zero failures, errors or skips.
- Frogspawn net fishing and lava eels remain separate researched rows. For lava eels, the final
  pre-cutoff raw-item page says 60 XP while the final pre-cutoff fishing-spot page says 30 XP; the
  exact Heroes' Quest learned-technique state and Lava Maze diary var also remain unresolved. No
  production behavior was guessed. Manual cases 78–79 capture the required client observations.
- The full XML audit contains 1,647 unit scenarios across 106 classes and 1,572 integration
  scenarios across 182 classes, with zero failures, errors or skips. The manifest now tracks 368
  unique groups with no duplicate IDs: 48 discovered, 85 researched, 1 unit-tested, 233
  integration-tested and 1 verified. The serialized repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 5m31s: 793 actionable tasks,
  with 54 executed, 6 restored from cache and 733 up-to-date.
- The running server was restarted in place and loaded 54 plugin modules and 197 scripts on port
  43594 at revision 233; PID 14123 owns the listener. The single already-open workspace RuneLite
  process was left running and untouched. Manual cache-client cases 78–79 remain pending because
  its Java window is unavailable to automation.

## 2026-09-11 — Frogspawn and swamp-weed net Fishing core

- Pinned final pre-cutoff Frog spawn, Swamp weed, Fishing spot (frog spawn, eels), Echo skill
  success chart, Beta Worlds, Spirit flakes, Rada's blessing 4 and Game tick evidence. Cache 233.4
  independently fixes the five first-option NPCs, small net 303, frog spawn 5004, swamp weed 10978
  and sequence 621. Added typed object references rather than embedding numeric output IDs.
- Added the members-only visible-level-33 method to the established protected ordinary-fishing
  loop and registered first-option dispatch. Each five-tick attempt performs the source-ordered,
  level-scaled 16/96 frog-spawn roll before the constant 10/10 swamp-weed fallback, awards 75 or 1
  XP, applies the shared Rada/spirit-flake multiplier without extra XP, excludes both non-fish
  outputs from the fish barrel and revalidates distance, net and player state.
- Preserved a contemporaneous source conflict instead of hiding it. The historical item chart and
  chart module imply 43/256 frog spawn at level 33 followed by about 35.8/1000 swamp weed, while
  the same-era spot table prints 11/256 and approximately 43/1000. The Wiki beta backlog confirms
  that frog spawn is stat-random but labels swamp weed's random model unresolved. Exact catch lines,
  output-specific Rada eligibility and whether the post-cutoff Camdozaal spirit-flake exception
  already existed also remain manual case 79 observations; production records its message strings
  as ordinary-family inferences.
- Six cache-aware scenarios pin identities and first-option dispatch, method contract and fish-
  barrel exclusion, ordered outcomes and XP, membership/level/tool/capacity gates, failed-roll
  repetition at five ticks, protected distance/tool revalidation and doubled frog spawn with one
  XP award. The focused class and complete Fishing integration module passed; Fishing now has 153
  scenarios across five classes. The full XML audit contains 1,647 unit scenarios across 106
  classes and 1,578 integration scenarios across 183 classes, with zero failures, errors or skips.
- The manifest tracks 368 unique groups with no duplicate IDs: 48 discovered, 84 researched, 1
  unit-tested, 234 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m34s: 793
  actionable tasks, with 116 executed, 8 restored from cache and 669 up-to-date. Runtime restart
  replaced the prior process cleanly; PID 45175 now owns port 43594 after loading revision 233, 54
  plugin modules and 197 scripts. The single already-open workspace RuneLite process was left
  running and untouched; manual case 79 remains pending because its Java window is unavailable to
  automation, and its only observed output is the known non-blocking upstream world-list warning.

## 2026-09-11 — Infernal and sacred eel Fishing core

- Pinned final pre-cutoff Infernal eel, its Rod Fishing spot, Sacred eel, its Fishing spot,
  varbit 4391, Oily pearl fishing rod, Ice gloves, Smiths gloves (i), Fishing, Game tick, Spirit
  flakes, Rada's blessing 4 and Zulrah's scales evidence. Cache 233.4 independently fixes NPCs
  7676/6488, eel objects 21293/13339, ordinary/pearl rods, bait, glove identities, varbit 4391 and
  casting sequences 622/8188.
- Added frozen special-eel NPC definitions and first-option `Bait` dispatch to the established
  protected ordinary-fishing service. Infernal eels require membership, visible Fishing 80, an
  oily ordinary or carried/worn pearl rod, bait and either ice gloves or Smiths gloves (i); their
  30/93 roll awards one eel and 95 XP. Sacred eels require membership, visible Fishing 87, an
  ordinary or carried/worn pearl rod, bait and Zulrah-education varbit 4391 at one or above; their
  0/60 roll awards one eel and 105 XP.
- Extended the data-driven Fishing method contract with reusable alternative worn-equipment and
  minimum-varbit requirements. Start checks and every five-tick result boundary revalidate both
  requirements before bait or output changes. Both methods compose with existing Rada/spirit-flake
  multiplication and carried/worn open fish barrels. Catch messages, the infernal missing-glove
  line and exact five-tick client presentation remain bounded inferences under manual case 80.
- Researched but deliberately did not conflate post-catch processing with catch support. Infernal
  hammer-smashing has exact 29/35 Tokkul, 2/35 onyx-tip and 4/35 lava-shard branches plus distinct
  cache sequences; sacred knife dissection has the boosted Cooking-72 three-to-nine-scale bands,
  level-104 cap and `100 + 3 × scales` XP. Their manual/automatic timing and presentation remain
  separate researched feature groups and the next implementation slice.
- Eight cache-aware scenarios cover identities and dispatch, exact method tables, membership,
  level, tool, bait, glove, permission and capacity gates, failure repetition, wearable pearl rods,
  Smiths-glove substitution, mid-roll glove/permission loss and doubled sacred-eel barrel routing.
  The focused class and complete Fishing module passed; Fishing now has 161 scenarios across six
  classes. The full XML audit contains 1,647 unit scenarios across 106 classes and 1,586 integration
  scenarios across 184 classes, with zero failures, errors or skips.
- The manifest now tracks 372 unique groups with no duplicate IDs: 48 discovered, 87 researched,
  1 unit-tested, 235 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m35s: 793
  actionable tasks, with 116 executed, 6 restored from cache and 671 up-to-date.
- Runtime restart replaced the prior process cleanly; PID 83695 owns port 43594 after loading
  revision 233, 54 plugin modules and 197 scripts with database schema version 6 valid. The single
  already-open workspace RuneLite process was left running and untouched; manual case 80 remains
  pending because its Java window is unavailable to automation, and its only new output is the
  known non-blocking upstream item-price/world-list warning.

## 2026-09-11 — Infernal and sacred eel processing core

- Added typed cache references for Tokkul and sequences 7151/7553/8969 plus a dedicated sacred-eel
  continuation queue. Registered ordinary, Imcando and off-hand Imcando inventory hammer use on
  infernal eels, knife use on sacred eels, and both cached fourth-option `Eat` handlers through the
  existing Fishing plugin script.
- Implemented immediate infernal processing of the first eel with the exact 35-way reward table,
  cache-family animations, atomic full-inventory replacement and same-tick manual spam. Automatic
  continuation remains a separate researched feature because no primary source fixes its cadence;
  the four-tick animation duration was not treated as an action-delay substitute.
- Implemented immediate sacred processing of the first eel at visible Cooking 72, exact level bands
  from 3–5 through capped 7–9 scales, `100 + 3 × scales` Cooking XP and exact two-tick first/three-
  tick repeated automatic cadence. Membership, boosted level, knife and remaining input are checked
  again at each boundary.
- Eight focused cache-aware scenarios pass across contracts/dispatch, formula boundaries, every
  infernal reward edge, first-item targeting, same-tick spam, membership/level/tool gates, safe full
  inventory, sacred automatic timing and interruption, animations, XP and both cached Eat options.
  Exact failure text, the secondary-source sacred Eat line, audiovisual/packet order and infernal
  automatic timing remain explicit manual case-80 verification work.
- The manifest now tracks 373 unique groups with no duplicate IDs: 48 discovered, 86 researched,
  1 unit-tested, 237 integration-tested and 1 verified. Fishing has 169 passing integration
  scenarios across seven classes. The XML audit contains 1,649 unit scenarios across 107 classes
  after adding two focused formula boundary tests, and 1,594
  integration scenarios across 185 classes, with zero failures, errors or skips. The serialized
  repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  5m45s: 793 actionable tasks, with 117 executed, 6 restored from cache and 670 up-to-date.
- Runtime restart replaced PID 83695 cleanly. PID 224 now owns port 43594 after loading cache
  revision 233, 54 plugin modules and 197 scripts with database schema version 6 valid. The one
  already-open workspace RuneLite process was not opened again, modified or otherwise touched.

## 2026-09-11 — Bryophyta's staff stored-nature core

- Replaced the charged staff's false unlimited-nature registration with a finite, item-local rune
  source. The shared rune validator can now describe stored runes on worn objects, source them
  before loose inventory and rune-pouch runes, and split one requirement across those sources. The
  combat rune manager validates every worn source before any mutation, consumes charges only after
  a successful cast, applies the exact 1/15 conservation roll to save one charge even for a multi-
  nature-rune spell, emits `Your staff saved you a nature rune.`, and refreshes the worn identity on
  final-charge depletion.
- Added a focused `content/skills/magic/bryophytas-staff` plugin following the existing injected
  script/lifecycle architecture. Nature-rune use on either cache variant loads the same item to a
  1,000-rune cap, including safe transformation in a full inventory. Inventory and worn Check read
  per-item `charges_16383`; Uncharge destroys every stored rune and restores the uncharged object.
  Typed charged/uncharged variant params were added for objects 22368/22370.
- Corrected the cache-model interpretation discovered during probing: varbit 16139 is the bank's
  permanent auto-charge quantity setting over varp 4677 bits 20–30, not a live account charge
  counter. The cache also links charged object 22370 through BANK_AUTOCHARGE param 2257 to DB row
  4340. These contracts are pinned by integration tests, while bank Configure-Charges remains a
  separate researched group. Death/trade behavior and the Crafting/Zaff creation routes were split
  into their own researched groups rather than being implied complete.
- Two rune-engine unit scenarios cover stored-only requirements and stored/inventory/pouch source
  splitting. Six cache-aware integration scenarios cover object and var contracts, script dispatch,
  cap and full-inventory loading, item-local Check, destructive Uncharge, depletion, multi-rune
  conservation and failed-cast atomicity. Exact load/full/Check/Uncharge presentation, confirmation,
  empirical source priority, bank behavior, death handling and creation/access remain manual case
  81 work, so the finished core is `integration-tested`, not client-tested or verified.
- The XML audit contains 1,651 unit scenarios across 107 classes and 1,600 integration scenarios
  across 186 classes, with zero failures, errors or skips. The manifest now tracks 376 unique groups
  with no duplicate IDs: 48 discovered, 88 researched, 1 unit-tested, 238 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 5m55s: 801 actionable tasks, with 440 executed, 7 restored from cache
  and 354 up-to-date.
- Runtime restart replaced PID 224 cleanly. PID 66824 now owns port 43594 after loading cache
  revision 233, 54 plugin modules and 198 scripts with database schema version 6 valid. The one
  already-open workspace RuneLite process was not opened again, restarted, modified or otherwise
  touched.

## 2026-09-11 — Bryophyta's staff manual creation and equip core

- Added the manual members route to the existing battlestaff Crafting plugin: using battlestaff
  1391 and Bryophyta's essence 22372 in either order requires a current, boostable Crafting level of
  62, consumes the exact selected inputs atomically and creates uncharged staff 22368 in the
  battlestaff slot for zero XP. Full inventories are safe because the output replaces a consumed
  input; stale or substituted selected objects abort without mutation.
- Corrected a cache/runtime impedance mismatch exposed by the focused tests. Raw staff metadata
  puts Crafting 62 in the first generic stat-requirement fields, while the shared equip handler uses
  those fields and the authoritative wield contract is base Attack 30 plus Magic 30. Typed object
  edits now expose the two wield requirements on both charged and uncharged variants, while the
  dedicated recipe owns Crafting 62. Both variants remain usable on free-to-play worlds and an
  equipped charged staff retains its per-item charge state.
- Did not invent the manual recipe's animation or duration: the final cutoff Wiki leaves its tick
  count unknown. Exact creation messages and packet order remain manual case 81. Zaff's separately
  sourced free-to-play service—one essence and 50,000 coins, no Crafting requirement—also remains a
  distinct researched group because the exact NPC route, dialogue, confirmation and capacity order
  were not established by primary evidence.
- Five new cache-aware integration scenarios cover object/member/equip contracts, both item-use
  orders, boostable and base-stat boundaries, full-inventory selected-slot replacement, zero XP,
  stale inputs, free-to-play wielding and charge preservation. The XML audit contains 1,651 unit
  scenarios across 107 classes and 1,605 integration scenarios across 186 classes, with zero
  failures, errors or skips.
- The manifest now tracks 378 unique groups with no duplicate IDs: 48 discovered, 89 researched,
  1 unit-tested, 239 integration-tested and 1 verified. The serialized repository-wide
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 5m41s: 801
  actionable tasks, with 118 executed, 6 restored from cache and 677 up-to-date.
- Runtime restart replaced PID 66824 cleanly. PID 86429 now owns port 43594 after loading cache
  revision 233, 54 plugin modules and 198 scripts with database schema version 6 valid. The one
  already-open workspace RuneLite process was not opened again, restarted, modified or otherwise
  touched.

## 2026-09-11 — Lava eel Fishing core

- Added typed cache references and first-option `Bait` dispatch for the two lava-eel fishing spots,
  NPCs 4928 and 6784, and froze both resource NPCs through the same editor pattern as the existing
  eel spots. The standard Fishing method now accepts oily fishing rod 1585 or carried/worn oily
  pearl fishing rod 23122, consumes fishing bait 313 only on success and awards raw lava eel 2148.
- Resolved the contemporary Wiki conflict in favor of 60 Fishing XP. Final pre-cutoff raw-item and
  Fishing data agree on 60, while the isolated spot table's 30 matches cooked lava eel's Cooking
  XP. The implementation pins visible level 53, 16/96 success endpoints and the ordinary five-tick
  cycle. The discrepancy remains recorded in the source ledger for future matching-client audit.
- Kept the catch action possession-based and deliberately free-world-capable. Heroes' Quest teaches
  and enables acquisition of the oily rod, but no source establishes a second quest-var gate once
  the player possesses one; an explicit free-world/varp-188-zero regression test prevents a normal
  members-content gate from being reintroduced. Exact quest acquisition/dialogue remains separate.
- Six focused cache-aware integration scenarios cover both NPC contracts and dispatch, tool and
  animation identities, the free-world/zero-quest-state exception, exact level/endpoints/XP, missing
  tool and bait, full inventory, success-only bait use, failed-roll continuation and protected
  worn-tool/distance interruption. Fishing now has 175 passing integration scenarios across eight
  classes.
- The individual hard Wilderness Diary task bit is not exposed by aggregate varps 1192/1193 or
  hard-count varbit 6325, so it was not guessed. Diary credit, the explicit no-heron rule,
  clue-bottle and Rada/spirit-flake eligibility, exact context-sensitive rejection/catch text,
  relocation and audiovisual/packet presentation remain in the dedicated researched parity group
  and manual case 79.
- The XML audit contains 1,651 unit scenarios across 107 classes and 1,611 integration scenarios
  across 187 classes, with zero failures, errors or skips. The manifest now tracks 379 unique groups
  with no duplicate IDs: 48 discovered, 89 researched, 1 unit-tested, 240 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 5m33s: 801 actionable tasks, with 57 executed, 6 restored from cache
  and 738 up-to-date.
- Runtime restart replaced PID 86429 cleanly. PID 2301 now owns port 43594 after loading cache
  revision 233, 54 plugin modules and 198 scripts with database schema version 6 valid. The one
  already-open workspace RuneLite process was not opened again, restarted, modified or otherwise
  touched.

## 2026-09-11 — Tome of Water charge lifecycle core

- Replaced the charged Tome's unconditional unlimited-water registration with a positive,
  item-local charge source. A dedicated bits-0–14 varobj field stores up to 20,000 charges on each
  object; soaked pages add 20 each, safely transform object 25576 into 25574 in place and consume a
  partial final page when only part of its value fits. Typed charged/uncharged variant links support
  the shared object-charge manager.
- Standard-spellbook combat casts requiring water runes now spend exactly one Tome charge after a
  successful atomic resource transaction, including splashes; utility spells and other spellbooks
  receive unlimited water without charge use. Zero-charge identities provide neither runes nor
  effects, failed casts spend nothing, and loose water runes remain untouched while the Tome source
  is valid.
- Carried cast state now snapshots Tome use before charge deletion, so the final charge retains its
  combat modifier after the worn object becomes empty. PvM offensive-water accuracy was corrected
  from 20% to the post-May-2024 cutoff value of 10%; PvP remains 20%. Water damage, curse/bind
  accuracy and the 50% stat-drain modifier use the same snapshot through the production formula and
  spell-impact paths.
- Cache-aware coverage pins objects 25574/25576/25578, inventory operation slots, varbit 16152 over
  permanent varp 4682, typed variants, script dispatch, page loading/cap behavior, full inventory,
  utility/combat and spellbook classification, loose-rune priority, failure atomicity, final-charge
  transformation and formula propagation. All ten Tome scenarios plus the focused rune, curse,
  accuracy, max-hit and combat-script regressions pass.
- Pages removal, exact partial-page wording/confirmation, bank Configure-Charges semantics,
  trade/death behavior, exact direct-core messages and audiovisual/packet presentation remain the
  separate researched client/bank group and manual case 82. Cached Pages operations deliberately
  remain without a guessed handler.
- The XML audit contains 1,651 unit scenarios across 107 classes and 1,621 integration scenarios
  across 188 classes, with zero failures, errors or skips. The manifest now tracks 380 unique groups
  with no duplicate IDs: 48 discovered, 89 researched, 1 unit-tested, 241 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 5m59s: 807 actionable tasks, with 129 executed, 6 restored from cache
  and 672 up-to-date.
- Runtime restart replaced PID 2301 cleanly. PID 35822 owns port 43594 after loading cache revision
  233, 54 plugin modules and 199 scripts with database schema version 6 valid. The one already-open
  workspace RuneLite process was not opened again, restarted, modified or otherwise touched.

## 2026-09-11 — Ancient Magicks combat-spell core

- Added all sixteen Ancient combat-spell definitions with their cutoff levels, rune costs, cast XP,
  maximum hits, five-tick cadence and Desert Treasure/Ancient-spellbook/member access checks. Cast
  resources and XP are committed once per cast, while Burst and Barrage select and roll up to nine
  eligible targets independently in the primary target's 3-by-3 area, including large-NPC tile
  intersection.
- Implemented the four spell-family effects through a shared impact path: Smoke poison chance and
  severity, Shadow's non-stacking percentage drains, Blood healing from total rolled damage, and
  Ice binds. Bloodbark, Virtus and all Ancient-sceptre variants are represented with typed object,
  sequence and spot-animation references; enhanced Ice accuracy and enhanced Smoke's temporary
  player-healing reduction flow through the production combat/stat paths.
- Ten cache-aware Ancient scenarios cover every spell contract, atomic access failures, rune/XP
  use, target origin/intersection/cap behavior, all four family effects, full Bloodbark plus sceptre,
  enhanced overheal and healing reduction, enhanced Ice duration, NPC poison and full-Virtus damage.
  A production-formula scenario separately proves the enhanced Ice accuracy multiplier.
- Kept unresolved encounter and client behavior explicit: secondary-player PvP eligibility and team
  rules, Barbarian Assault suppression, general NPC freeze resistance, NPC healing consumers,
  Shadow Sire/Leviathan hooks, exact secondary-target order, distinct cutoff projectile identities,
  messages, audio and packet timing remain researched/manual work. The core is therefore
  `integration-tested`, not client-tested or verified.
- The XML audit contains 1,651 unit scenarios across 107 classes and 1,632 integration scenarios
  across 189 classes, with zero failures, errors or skips. The manifest now tracks 382 unique groups
  with no duplicate IDs: 48 discovered, 90 researched, 1 unit-tested, 242 integration-tested and 1
  verified. The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and
  `integration` gate passed in 6m05s: 807 actionable tasks, with 120 executed, 6 restored from cache
  and 681 up-to-date.
- Runtime restart replaced PID 35822 cleanly. PID 94714 owns port 43594 after loading cache revision
  233, 54 plugin modules and 200 scripts with database schema version 6 valid. The one already-open
  workspace RuneLite process was not opened again, restarted, modified or otherwise touched.

## 2026-09-12 — Arceuus Grasp spells and Mark of Darkness core

- Researched the cutoff spell contracts against the final applicable Wiki records, official Poll
  80 behavior change, July 2024 Purging-staff update, August 2025 fixed-duration update, cache 233.4
  and the workspace RuneLite timer source. This pinned the Ghostly pre-A Kingdom Divided exception,
  exact 10/25/50-percent successful-hit chances, 2/3/4-tick holds, fixed two-tick impacts and Mark's
  separation between its ten-tick recast lock and 300-tick active state.
- Added all three Grasp handlers through rsmod's existing `SpellAttackMap` and queued-impact plugin
  structures. They enforce member and exact quest gates, use one protected rune/XP transaction,
  roll binds only after successful accuracy, double chance to 20/50/100 percent while Mark is
  active, and apply two-tick post-release immunity without Swampbark duration extension.
- Added Mark of Darkness as an Arceuus self-cast with exact level, runes, XP, success and warning
  text, sequence/graphics, ten-tick cast lock, 300-tick ordinary duration and cast-time 1,500-tick
  Purging-staff snapshot. A shared state repository lets both Grasp and the production formula
  collector consume the full active duration rather than mistaking cache varbit 12406 for it.
- Added typed cache references for components 170–174, quest varbit 12296, sequences 8972/8985/8986,
  graphics 1852/1853/1856–1864/1886, three timers and the delayed-impact queue. Cache quest-table
  row 81 column 19 independently resolves completion state 150.
- Nine Arceuus integration scenarios cover cache contracts/assets, chance/duration tables, the
  Ghostly quest exception, atomic Mark gates, cooldown versus duration, warning/expiry, Purging
  snapshot, fixed-delay binding and guaranteed marked Undead Grasp. A formula integration scenario
  independently proves active-state and Demonbane attribute propagation.
- Ward of Arceuus, exact Grasp/Mark audio and packet timing, Mark hit/end sequencing, reconstructed-
  session persistence and full NPC/PvP resistance, immunity and movement behavior remain explicitly
  researched under manual case 84.
- The XML audit contains 1,651 unit scenarios across 107 classes and 1,642 integration scenarios
  across 190 classes, with zero failures, errors or skips. The manifest now tracks 384 unique groups:
  48 discovered, 91 researched, 1 unit-tested, 243 integration-tested and 1 verified. The serialized
  repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  2m46s: 807 actionable tasks, with 130 executed, 6 restored from cache and 671 up-to-date.
- Runtime restart replaced PID 94714 cleanly. PID 27607 owns port 43594 after loading cache revision
  233, 54 plugin modules and 202 scripts with database schema version 6 valid. Existing workspace
  RuneLite PID 24532 still has `/Users/stephenrobb/Dev/OSRSemulator/runelite` as its working
  directory and was not opened again, restarted, modified or otherwise touched.

## 2026-09-12 — Arceuus Demonbane combat-spell core

- Reconstructed the October 1, 2025 Demonbane contract from the final spell-family and individual
  records, the official 9 April accuracy-stacking correction, the 22 May rune reductions, the
  maximum-magic-hit equation, hit-delay research and cache 233.4. This resolves the Inferior quest
  exception, final 3/5/7 fire costs and the distinction between whole-hit and added-damage
  effectiveness rounding.
- Added Inferior, Superior and Dark Demonbane handlers through rsmod's existing `SpellAttackMap`
  structure. They enforce members, quest and demon-only target gates before the atomic cast
  transaction; consume the cache spell requirements; share five-tick combat cadence; emit their
  tier-specific cast/hit effects; and queue hits at the family-fixed two-tick delay.
- Preserved the already-corrected accuracy model: +20% normally, +40% with Purging, +40% with Mark,
  and +80% with both, with the 70%-effective target path producing +14/+28/+28/+56. Corrected the
  max-hit path so demonbane effectiveness scales only the separately floored +25%/+50% Mark-added
  damage, matching the published post-hit-roll equation.
- Added typed cache references for component children 167–169, casting sequence 8977 and tiered
  graphics 1865–1870. Fourteen focused Arceuus scenarios now cover the combined Mark/Grasp/
  Demonbane surface, and formula parameter cases lock ordinary, marked, Purging and resistant
  accuracy/damage arithmetic.
- Exact invalid-target wording and gate priority are explicitly provisional. Demonbane audio and
  packet timing, full autocast-weapon behavior, the production-wide demonic/effectiveness catalog,
  greater-than-100% effectiveness and encounter overrides remain researched under manual case 85.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,647 integration scenarios
  across 190 classes, with zero failures, errors or skips. The manifest now tracks 386 unique
  groups: 48 discovered, 92 researched, 1 unit-tested, 244 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 2m29s: 807 actionable tasks, with 123 executed, 6 restored from cache and 678
  up-to-date.
- Runtime restart replaced PID 27607 cleanly. PID 43110 owns port 43594 after loading cache
  revision 233, 54 plugin modules and 202 scripts with database schema version 6 valid. Existing
  workspace RuneLite PID 24532 still has `/Users/stephenrobb/Dev/OSRSemulator/runelite` as its
  working directory and was not opened again, restarted, modified or otherwise touched.

## 2026-09-12 — Ward of Arceuus defensive core

- Reconstructed the October 1, 2025 Ward contract from the final spell record, the official 25 June
  combat update, Grasp-family research, cache 233.4 and the workspace RuneLite timer source. This
  pins level 73, one cosmic/two nature/four soul runes, 83 XP, members and A Kingdom Divided gates,
  one tick per base Magic level, a 50-tick cooldown and the exact success line.
- Added Ward as an Arceuus self-cast through rsmod's protected launcher and rune-manager transaction.
  It snapshots base Magic on cast, owns independent expiry and cooldown state, uses cache sequence
  8982 and graphic 1851, and resets cooldown varbit 12293 through a typed soft timer.
- Added Ward consumption to the successful Grasp path, reducing an incoming player-target Grasp bind
  to one tick. Added immutable demon-source metadata to the shared hit model and applied Ward's
  floor-rounded 10% damage reduction in the central standard player-hit modifier, so ordinary NPCs
  and inactive Ward state remain unchanged.
- Seventeen focused Arceuus scenarios now include exact Ward cache assets and spell contract,
  atomic free-to-play and quest failures, resource/XP/presentation flow, base-level duration,
  cooldown/expiry, one-tick Undead Grasp and demon-only damage rounding. The production spell
  registry explicitly excludes Ward from offensive attack routing.
- Corruption cancellation/immunity remains open until Lesser and Greater Corruption are implemented.
  The 33% demon prayer-drain reduction requires encounter prayer-drain consumers, and exact audio,
  packet order and reconstructed-session persistence remain researched under manual case 84.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,650 integration scenarios
  across 190 classes, with zero failures, errors or skips. The manifest now tracks 387 unique groups:
  48 discovered, 92 researched, 1 unit-tested, 245 integration-tested and 1 verified. The serialized
  repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  2m27s: 807 actionable tasks, with 126 executed, 6 restored from cache and 675 up-to-date.
- Runtime restart replaced PID 43110 cleanly. PID 63426 owns port 43594 after loading cache revision
  233, 54 plugin modules and 203 scripts with database schema version 6 valid. Existing workspace
  RuneLite PID 24532 still has `/Users/stephenrobb/Dev/OSRSemulator/runelite` as its working
  directory and was not opened again, restarted, modified or otherwise touched.

## 2026-09-12 — Lesser and Greater Corruption core

- Reconstructed the October 1, 2025 contract from the final Corruption-status and individual-spell
  records, the official 30 August 2023 Poll 80 change, the original Arceuus spell update, cache
  233.4 and the workspace RuneLite timer source. This pins Lesser's level 64, one death/two soul
  and 75 XP contract; Greater's level 85, one blood/three soul and 95 XP contract; the shared
  50-tick cooldown; final 50% proc chance; Mark-guaranteed proc and halved interval; and exact
  escalating 1/2/3 versus 2/4/6 Prayer drains.
- Added both spells as protected, transactional Arceuus self-casts with members and exact quest
  gates. They use component children 175/176, sequence 8996, cast graphics 1877/1878 and impact
  graphics 1879/1880, and explicitly remain outside the offensive spell registry.
- Added server-authoritative armed and active-status state, client varbit synchronization and typed
  cooldown/drain timers. A successful player hit consumes the armed effect before the 50% roll;
  Mark guarantees application and changes each ten-tick pulse to five ticks. Active Corruption is
  non-stacking, clears after three pulses and uses cache hitmark 0 with Prayer-floor-safe damage.
- Extended the central hit record with immutable accuracy-success metadata so a successful
  zero-damage magic hit can trigger Corruption while a splash cannot. Player and NPC queued-hit
  constructors preserve existing damage-derived defaults, and the magic attack manager supplies
  explicit success/failure at its accuracy boundary.
- Integrated Ward bidirectionally: casting Ward clears an active Corruption status; a target with
  Ward blocks new application without consuming the attacker's armed effect, which can trigger
  after Ward expires. Five focused scenarios cover spell contracts/assets and registry exclusion,
  atomic gates and shared cooldown, ordinary and marked drain schedules, splash/failed-proc
  behavior, and Ward cancellation/immunity/preservation.
- Exact audio and packet ordering, target wording and already-corrupted priority, reconstructed-
  session/logout/death/world-transition policy, and client buff/cooldown display timing remain
  researched under manual case 86 rather than being claimed verified.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,655 integration scenarios
  across 190 classes, with zero failures, errors or skips. The manifest now tracks 389 unique
  groups: 48 discovered, 93 researched, 1 unit-tested, 246 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 2m19s: 807 actionable tasks, with 138 executed, 6 restored from cache and 663
  up-to-date.
- Runtime restart replaced PID 63426 cleanly. PID 81471 owns port 43594 after loading cache
  revision 233, 54 plugin modules and 204 scripts with database schema version 6 valid. Existing
  workspace RuneLite PID 24532 still has `/Users/stephenrobb/Dev/OSRSemulator/runelite` as its
  working directory and was not opened again, restarted, modified or otherwise touched.

## 2026-09-12 — Vile Vigour core

- Reconstructed the October 1, 2025 contract from the final spell record, the official A Kingdom
  Divided poll material, the Arceuus spellbook reference, Runecraft training guidance, cache 233.4
  and the workspace RuneLite timer source. This pins level 66, three air/one soul rune, 76 XP,
  members and A Kingdom Divided gates, a 17-tick cooldown, and the one-Prayer-point-per-run-percent
  conversion capped by both missing run energy and current Prayer.
- Added Vile Vigour as a protected, transactional Arceuus self-cast. It rejects a no-effect cast
  before rune consumption, converts only the Prayer needed to reach 100% run energy, converts all
  remaining Prayer when that is insufficient, updates run energy immediately and deliberately does
  not stop the player's current action so running can continue.
- Added typed cache references for component child 181, sequence 8978, graphic 8995, cooldown varbit
  12292 and timer 31. The production spell registry explicitly excludes this utility spell from
  offensive attack routing.
- Four focused scenarios cover exact cache assets and spell contract, exact partial conversion and
  cooldown, insufficient-Prayer conversion, atomic no-effect rejection, members/quest/level/rune
  gates and Fountain of Rune behavior.
- Exact no-effect wording, audio and packet ordering, reconstructed-session cooldown restoration and
  direct workspace RuneLite timer rendering remain researched under manual case 87 rather than
  being claimed verified.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,659 integration scenarios
  across 191 classes, with zero failures, errors or skips. The manifest now tracks 391 unique
  groups: 48 discovered, 94 researched, 1 unit-tested, 247 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 2m25s: 807 actionable tasks, with 125 executed, 6 restored from cache and 676
  up-to-date.
- Runtime restart replaced PID 81471 cleanly. PID 90751 owns port 43594 after loading cache revision
  233, 54 plugin modules and 205 scripts with database schema version 6 valid. Existing workspace
  RuneLite PID 24532 still has `/Users/stephenrobb/Dev/OSRSemulator/runelite` as its working
  directory and was not opened again, restarted, modified or otherwise touched.

## 2026-09-12 — Death Charge core and Rite upgrade

- Reconstructed the October 1, 2025 Death Charge contract from the final spell record, the official
  Yama release, the Rite of Vile Transference item/transcript record, cache 233.4 and the workspace
  RuneLite timer implementation. This pins level 80, one death/blood/soul rune, 90 XP, members and
  A Kingdom Divided gates, a 100-tick active window/cooldown, 16-tile credited-kill range, 15%
  special restoration and the ten-cycle recent-PvP suppression rule.
- Added a single-publication credited NPC-kill event at the existing lethal-hit boundary and wired
  Death Charge through it. Ordinary casts consume one qualifying kill; Rite-upgraded casts expose
  two charges and synchronize cache active-state varbit 12411 from two to one to zero. A trigger
  consumes its charge even at full special energy, while the independent cast cooldown continues.
- Added an explicit non-credit policy for the published Amoxliatl, Moons of Peril, Nightmare,
  Wardens, Vasa, Olm and Sire encounter targets. Nex is deliberately suppressed because its live
  ownership is highest eligible contribution rather than the generic lethal-hit source; this stays
  in the encounter-parity group rather than being approximated.
- Added the permanent Rite unlock using cache object 30806 and varbit 16269. The protected Read
  route uses the sourced confirmation page/title/options, revalidates the exact selected slot,
  consumes atomically, plays the red Death Charge presentation and emits the exact success line.
  Declining, losing/moving the selected item and attempting a repeat unlock do not consume it.
- Added typed component, object, sequence, graphic, varbit and timer references. Seven focused
  integration scenarios cover cache contracts, scroll dialogue and atomicity, ordinary/upgraded
  casting, lethal event routing, exclusions, distance and PvP suppression, full-bar consumption,
  exact expiry/cooldown boundaries, all access/resource gates and Fountain behavior.
- Exact sounds, failure/cooldown wording and priorities, packet ordering, active-state
  reconstruction and transition policy, indirect-damage and multi-attacker edge attribution, the
  complete evolving exclusion inventory and Nex contribution ownership remain researched under
  manual case 88 rather than being claimed verified.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,666 integration scenarios
  across 192 classes, with zero failures, errors or skips. The manifest now tracks 393 unique
  groups: 48 discovered, 95 researched, 1 unit-tested, 248 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 2m22s: 807 actionable tasks, with 125 executed, 6 restored from cache and 676
  up-to-date.
- Runtime restart replaced PID 90751 cleanly. PID 16117 owns port 43594 after loading cache
  revision 233, 54 plugin modules and 206 scripts with database schema version 6 valid. Existing
  workspace RuneLite PID 24532 still has `/Users/stephenrobb/Dev/OSRSemulator/runelite` as its
  working directory and was not opened again, restarted, modified or otherwise touched.

## 2026-09-12 — Arceuus Degrime core

- Reconstructed the October 1, 2025 Degrime contract from the final spell record, cache 233.4 and
  rsmod's canonical Herblore cleaning table. The cache corrects the spell recipe to four earth and
  two nature runes and pins object 15345, component 218:179, level 70, 83 Magic XP, sequence 8980,
  preparation sequence 8981, effect sequence 9002 and graphic 1885.
- Added a protected, transactional Degrime self-cast. It enforces members, A Kingdom Divided,
  Druidic Ritual, Magic, rune and eligible-herb gates; evaluates boosted Herblore once; converts all
  eligible grimy herbs in their original slots through one inventory transaction; and floors half
  Herblore XP independently per herb in the server's fine-XP representation.
- Added an exact eight-tick interval with the sourced repeat-cast line, shared rune-substitution and
  Fountain behavior, and conservative active-route rejection. Six focused scenarios cover all
  fifteen canonical herbs including Huasca, mixed boosted eligibility, per-herb rounding, exact
  cadence, atomic gate/no-effect/movement failures, cache presentation and Fountain semantics.
- The matching client's queued-but-never-completes moving-cast defect is intentionally not
  reproduced as a stuck server action. Exact no-herb wording/priority, sounds, preparation/effect
  staging, packet ordering, interruption and moving-cast recovery remain researched under manual
  case 89 rather than being claimed verified.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,672 integration scenarios
  across 193 classes, with zero failures, errors or skips. The manifest now tracks 395 unique
  groups: 48 discovered, 96 researched, 1 unit-tested, 249 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 2m22s: 807 actionable tasks, with 123 executed, 6 restored from cache and 678
  up-to-date.
- Runtime restart replaced PID 16117 cleanly. PID 30739 owns port 43594 after loading cache
  revision 233, 54 plugin modules and 207 scripts with database schema version 6 valid. Existing
  workspace RuneLite PID 24532 still has `/Users/stephenrobb/Dev/OSRSemulator/runelite` as its
  working directory and was not opened again, restarted, modified or otherwise touched. A separate
  installed-app RuneLite process was already present and was likewise not opened or touched.

## 2026-09-12 — Arceuus Shadow Veil cast and state core

- Reconstructed the cutoff contract from the final Shadow Veil record, cache 233.4 and the
  workspace RuneLite timer implementation: members-only Magic 47 and A Kingdom Divided access,
  five fire/earth/cosmic runes, 58 XP, base-Magic-level duration in ticks, a 50-tick cooldown and a
  first-roll 15% failed-pickpocket shield before the dodgy necklace.
- A disposable integration probe resolved object 15344, component 218:180, player sequence 8979,
  cast graphic 1881/effect sequence 8998 and varbits 12291/12414; the probe was removed after its
  evidence was converted into typed references and permanent assertions.
- Added the protected transactional cast, active/cooldown client synchronization, exact activation
  line, base-level duration snapshot and refresh after the cooldown. A public player-state
  repository exposes the exact 15% protection operation so a future Thieving implementation can
  invoke it before other failure protection without depending on the spell plugin.
- Five focused integration scenarios cover cache contracts, base versus boosted duration, exact
  expiry/cooldown boundaries, refresh while active, the 14/15 protection-roll boundary, atomic
  members/quest/level/rune rejection and Fountain semantics. This tree has no production Thieving
  plugin, so pickpocket, dodgy-necklace and blackjacking consumers remain researched under manual
  case 90 rather than being claimed integrated.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,677 integration scenarios
  across 194 classes, with zero failures, errors or skips. The manifest now tracks 397 unique
  groups: 48 discovered, 97 researched, 1 unit-tested, 250 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 2m27s: 807 actionable tasks, with 123 executed, 6 restored from cache and 678
  up-to-date.
- Runtime restart replaced PID 30739 cleanly. PID 42766 owns port 43594 after loading cache
  revision 233, 54 plugin modules and 208 scripts with database schema version 6 valid. Existing
  workspace RuneLite PID 24532 still has `/Users/stephenrobb/Dev/OSRSemulator/runelite` as its
  working directory and was not opened again, restarted, modified or otherwise touched. The
  separate installed-app RuneLite process was also left untouched.

## 2026-09-12 — Arceuus Dark Lure core

- Reconstructed the cutoff contract from the Arceuus spellbook record, the official A Kingdom
  Divided poll blog, the official New Client Features milestone and cache 233.4. This pins
  members-only Magic 50 and A Kingdom Divided access, one death/nature rune, 60 XP, attack/lure
  behavior, immobile rejection and exactly six seconds of two-by-two impling following.
- Added typed references for object 15303, component 218:182, graphics 1882–1884, their effect
  sequences 8999–9001 and cooldown varbit 12289. Dark Lure retains its cache combat-spell type but
  is explicitly excluded from the ordinary damage-spell router and dispatched through its own
  protected target handler.
- The production spell validates movement before resource use, launches the cache projectile,
  validates target identity again at travel-time impact, engages attackable creatures through the
  standard NPC/player interaction and makes only implings follow for ten cycles before restoring
  default movement. Unrelated non-combat NPCs are not forced to follow.
- Six focused integration scenarios cover the cache contract, exact runes/XP/presentation and
  cooldown, attack routing, impling follow/expiry, unrelated-NPC safety, atomic members/quest/
  level/rune/immobile failures and Fountain behavior. The shared Arceuus human casting sequence
  and five-tick cadence remain bounded inferences; exact feedback, audio, packet order, fleeing
  nuances, live two-by-two pathing and catch-ownership grace remain manual case 91.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,683 integration scenarios
  across 195 classes, with zero failures, errors or skips. The manifest now tracks 399 unique
  groups: 48 discovered, 98 researched, 1 unit-tested, 251 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 2m27s: 807 actionable tasks, with 130 executed, 6 restored from cache and 671
  up-to-date.
- Runtime restart replaced PID 42766 cleanly. PID 73874 owns port 43594 after loading cache
  revision 233, 54 plugin modules and 209 scripts with database schema version 6 valid. The
  workspace RuneLite was not opened, restarted, modified or otherwise touched, and the separate
  installed-app RuneLite process was likewise left untouched.

## 2026-09-12 — Resurrect Crops dependency audit

- Reconstructed the legacy spell contract from the official 2016 release, the cutoff Arceuus
  spellbook record, cache 233.4 and the workspace RuneLite revision: Magic 78, no A Kingdom
  Divided requirement, 25 earth/8 blood/12 nature/8 soul runes, 90 XP, component 218:163 and human
  cast sequence 7118.
- The success contract is 50% at current Magic 78 scaling to 75% at 99 with no benefit above 99.
  Success restores the dead crop to growth, failure clears its patch, and a resurrected crop that
  dies again cannot be resurrected a second time.
- A production implementation is deliberately not claimed: this tree has no Farming plugin or
  authoritative patch-growth/death/persistence state to update. Manual case 92 now inventories the
  required patch families, exact interpolation, success/failure state transitions, one-revival
  lifetime flag, persistence, presentation and matching-client boundaries for its future owner.
- This research-only addition raises the manifest to 400 unique groups: 48 discovered, 99
  researched, 1 unit-tested, 251 integration-tested and 1 verified. The previously verified Dark
  Lure runtime remains live as PID 73874 on port 43594; no RuneLite process was touched.

## 2026-09-12 — Arceuus Demonic and Sinister Offering core

- Reconstructed the cutoff contracts from the Arceuus spellbook, official A Kingdom Divided poll
  blog, Prayer/training and Zealot records, cache 233.4 and the instant-cast change history. This
  pins Magic 84/92, exact soul+wrath and blood+wrath costs, 175/180 Magic XP, up to three matching
  remains, three times each remain's base Prayer XP, special two-point Prayer restoration, no
  Zealot preservation and Fountain behavior.
- Added typed references for objects 15346/8796, components 218:177/178, human cast/preparation
  sequences 8975/8976, graphics 1871/1872, effect sequences 8992/8991, shared varbit 12423 and a
  dedicated server timer. The production handlers use the canonical `PrayerRemainsRecipes` table,
  an atomic top-left inventory transaction and one shared cooldown while deliberately preserving
  an existing movement route.
- Seven integration scenarios pin both registry/cache contracts, exact multi-item behavior and
  graphics, every canonical bone and demonic ash, triple per-recipe XP, all special restoration,
  Prayer-level independence, the shared exact server boundary, route preservation, atomic access/
  level/rune/no-remains failures and Fountain semantics. The eight-cycle interval is explicitly a
  bounded cache/evidence reconstruction; exact client timing, feedback, audio, packets and
  lifecycle reconstruction remain manual case 93.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,690 integration scenarios
  across 196 classes, with zero failures, errors or skips. The manifest now tracks 402 unique
  groups: 48 discovered, 100 researched, 1 unit-tested, 252 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 2m25s: 807 actionable tasks, with 130 executed, 6 restored from cache and 671
  up-to-date.
- Runtime restart replaced PID 73874 cleanly. PID 92454 owns port 43594 after loading cache
  revision 233, 54 plugin modules and 210 scripts with database schema version 6 valid. Workspace
  RuneLite PID 24532 was not opened, restarted, modified or otherwise touched; the separate
  installed-app RuneLite process was also left untouched.

## 2026-09-12 — Arceuus destination teleport core

- Reconstructed all twelve cutoff contracts from the teleportation table, individual spell
  records, official A Kingdom Divided poll blog, cache 233.4 and quest database rows. This pins
  membership, absence of an A Kingdom Divided gate, exact levels/runes/XP, the eleven fixed cache
  destinations, configured-default Respawn behavior and all six quest-boundary groups. The direct
  Library sources resolve 9 XP over a stale 10-XP summary, so the local spell overlay was corrected.
- Added typed references for all twelve spellbook components, human cast sequence 6575, spot
  animation 1296 and the four governing quest varps. The production handler uses canonical spell
  registry/rune-manager behavior, four-tick sequence/graphic/synth-200 presentation, protected
  operation-1 dispatch, exact cache destinations, level-20 Wilderness and Tele Block boundaries,
  staff substitution and Fountain suppression of Magic XP.
- Eight integration scenarios cover every registry/cache contract, exact resource and destination
  behavior, quest completion boundaries, membership/spellbook/level/rune atomicity, no-AKD access,
  four-tick arrival, presentation, Wilderness/Tele Block, substitutions, Fountain and non-cast
  operations. Per-player respawn selection/death linkage and the distinct Drezel-warning edge are
  deliberately not claimed and remain manual case 94.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,698 integration scenarios
  across 197 classes, with zero failures, errors or skips. The manifest now tracks 404 unique
  groups: 48 discovered, 101 researched, 1 unit-tested, 253 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 2m24s: 807 actionable tasks, with 125 executed, 6 restored from cache and 676
  up-to-date.
- Runtime restart replaced PID 92454 cleanly. PID 17753 owns port 43594 after loading cache revision
  233, 54 plugin modules and 211 scripts with database schema version 6 valid. The workspace
  RuneLite was not opened, restarted, modified or otherwise touched; the separate installed-app
  RuneLite process was likewise left untouched.

## 2026-09-12 — Arceuus reanimation cast and reward core

- Reconstructed the post-rework four-spell/twenty-three-creature table from the official A Kingdom
  Divided poll, Poll 76 hellhound addition, direct reanimation/head records and cache 233.4. This
  pins levels 16/41/72/90, exact rune costs and 32/80/138/170 Magic XP, all Prayer rewards from 130
  to 1,560, spell objects 11997/2885/6208/6888, components 218:149/151/152/153, head pairs
  13447–13511 and 26996–26997, NPCs 7018–7039 and 11463, Vorkath head 21907, sequences 3865/7198
  and graphics 1288–1290. Processed-head cache parameter 608 links every head to its tier spell.
- Direct spell/head pages consistently define the fresh-drop area as a centered 31-by-31 square,
  while one aggregate snippet described 35-by-35; the implementation follows the direct records
  and the corroborating approximately-fifteen-tile developer statement. Fresh initial ground heads
  therefore cast away from the altar; carried initial heads, processed heads and Vorkath's head use
  the Dark Altar radius until pickup-origin provenance is represented by the object pipelines.
- Added protected inventory and ground-object handlers using rsmod's spell registry, rune manager,
  object/NPC repositories, collision map and AI interaction syntax. Success consumes the head,
  applies cache presentation, creates a finite non-respawning hostile creature after its spawn
  animation, allows its owner attack to bypass single-way occupancy, suppresses ordinary death
  drops and awards exact Prayer XP only on the credited owner kill. Exact too-far, no-room and
  one-active feedback, members/spellbook/level/rune/tier atomicity, Zealot 1.25%-per-piece saving,
  Fountain behavior and logout cleanup are included. The shared NPC-vs-player combat entry point
  is public only so this creature-specific handler can intentionally bypass the generic singles
  gate while retaining the standard melee pipeline.
- Six integration scenarios pin every cache contract and all twenty-three reward variants, fresh
  ground versus altar-only paths, Vorkath substitution, deterministic full-set Zealot preservation,
  one-active and room gates, access/resource failures, credited rewards and Fountain semantics.
  The focused suite and the serialized repository-wide gate both pass. XML totals are 1,662 unit
  scenarios across 107 classes and 1,704 integration scenarios across 198 classes, with zero
  failures, errors or skips. The manifest now tracks 406 unique groups: 48 discovered, 102
  researched, 1 unit-tested, 254 integration-tested and 1 verified. The full `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 2m28s: 807 actionable tasks,
  with 130 executed, 6 restored from cache and 671 up-to-date.
- Exact pickup/bank/trade/Grand Exchange/price-checker origin invalidation, noted-head behavior,
  abyssal Slayer-85 harm gating, Slayer/Soul Bearer credit, dragonfire and other creature-specific
  attacks, KBD-head conversion, authoritative lifetime, exact audio/packet ordering and live client
  observation remain explicitly researched under manual case 95 and are not claimed by the core
  row.
- Runtime restart replaced PID 17753 cleanly. PID 60275 owns port 43594 after loading cache revision
  233, 54 plugin modules and 212 scripts with database schema version 6 valid. The workspace
  RuneLite was not opened, restarted, modified or otherwise touched; the separate installed-app
  RuneLite process was likewise left untouched.

## 2026-09-12 — Arceuus resurrection-thrall core

- Reconstructed all nine lesser, superior and greater ghost/skeleton/zombie spells from the
  official A Kingdom Divided poll, direct spell and family records, the 25 June 2025 Combat Sweep
  and cache 233.4. This pins levels 38/57/76; Magic XP 55/70/88; Prayer costs 2/4/6; exact three-tier
  rune recipes; max hits 1/2/3; spell objects 25506-25514; components 218:184-192; NPCs 10878-10886;
  state varbits 12290/12410/12412/12413; Master status varbit 12867 value 2; and the cache's spawn,
  attack, projectile and impact presentation assets.
- Added dedicated button routing and owner/NPC state using rsmod's spell registry, rune manager,
  protected access, NPC repository, timers and hit events. Casts validate membership, A Kingdom
  Divided, a carried or equipped Book of the Dead, current Prayer, Magic and runes atomically;
  snapshot current Magic for duration before cast XP; double it only for completed Master Combat
  Achievements; synchronize resurrection varbits; and replace the prior thrall only after the
  shared recast boundary. Fountain behavior and logout cleanup use the same native seams as adjacent
  spells.
- The spawned NPC follows its owner and mirrors only genuine offensive NPC interactions. Its
  ghost/magic, skeleton/ranged and zombie/melee styles attack every four cycles, always succeed but
  can roll zero, respect cache `thrall_immunity`, bypass flat armour, attribute damage and kills to
  the owner without ordinary combat XP, and reject all incoming damage. Nine integration scenarios
  cover every tier and style, exact resources/XP, Master duration and expiry, one-active replacement,
  all atomic gates, no-room behavior, accurate/zero/armour interactions, immunity, invulnerability
  and Fountain semantics.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,713 integration scenarios
  across 199 classes, with zero failures, errors or skips. The manifest now tracks 408 unique groups:
  48 discovered, 103 researched, 1 unit-tested, 255 integration-tested and 1 verified. The full
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 2m29s: 807
  actionable tasks, with 121 executed, 6 restored from cache and 680 up-to-date.
- The source conflict between a 16-tick infobox cooldown and ten-second prose is preserved; the
  implementation uses 17 cycles pending client proof. Exact restricted locations and named-instance
  exceptions, room-boundary following, spawn priority, feedback/failure ordering, persistence,
  inferred attack ranges/animations/projectile profile, sound and packet order remain explicitly
  researched under manual case 96 and are not claimed by the core row.
- Runtime restart replaced PID 60275 cleanly. PID 12054 owns port 43594 after loading cache revision
  233, 54 plugin modules and 213 scripts with database schema version 6 valid. The workspace
  RuneLite was not opened, restarted, modified or otherwise touched; the separate installed-app
  RuneLite process was likewise left untouched.

## 2026-09-12 — Standard Mining rock lifecycle core

- Reconstructed the ordinary rock table from the cutoff Mining and eleven direct rock records plus
  cache 233.4. Both visual variants of clay, copper, tin, blurite, iron, silver, coal, gold, mithril,
  adamantite and runite now carry typed level, product, XP, depleted-stage, normal-respawn and
  1/256 success-endpoint parameters. The 25-second blurite source is represented by the nearest
  whole server interval, 42 cycles (25.2 seconds), and retained as an explicit client-test question.
- Added a dedicated rsmod Mining plugin using the repository's object/location editors, protected
  operation model, stat-random and XP services, location replacement and client overlay seam. It
  handles first-option Mine and pickaxe-on-rock starts, internal repeat rolls, the best usable
  equipped or carried standard pickaxe, cache animations, exact ore/XP output, full-inventory and
  level gates, depletion and respawn. Bronze, iron, steel, black, mithril, adamant, rune and gilded
  pickaxes have their cache-backed levels and 8/7/6/5/5/4/3/3-cycle intervals.
- Two cache-contract and four gameplay integration scenarios pass in the focused Mining suite. They
  pin all 22 rock definitions and eight tools, access and capacity failures, selection, cadence,
  award, depletion and respawn. The broader Mining systems and matching-client observations remain
  explicitly researched under manual case 97; this entry does not claim them.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,719 integration scenarios
  across 201 classes, with zero failures, errors or skips. The manifest now tracks 410 unique
  groups: 48 discovered, 104 researched, 1 unit-tested, 256 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 4m1s: 813 actionable tasks, with 456 executed, 7 restored from cache and 350
  up-to-date.
- Runtime restart replaced PID 12054 cleanly. PID 73435 owns port 43594 after loading cache revision
  233, 54 plugin modules and 214 scripts with database schema version 6 valid. The already-open
  workspace RuneLite was not focused, restarted, modified or otherwise touched; the separate
  installed-app RuneLite process was likewise left untouched.

## 2026-09-12 — Special Mining pickaxe rate and access core

- Extended the rsmod Mining object editor from eight standard tools to all 14 cutoff special
  identities: five dragon styles, six charged/depleted Infernal styles, third-age and both crystal
  states. Each form uses its cache sequence, exact Mining level and members contract; crystal also
  requires completed Song of the Elves state 194.
- Added typed fast-interval and crystal-access parameters and implemented the actual per-roll
  two-or-three-cycle distribution: 1/6 for dragon-equivalent families and inactive crystal, 1/4 for
  active crystal. Best carried/equipped selection compares exact expected intervals, so active
  crystal outranks the otherwise equal 61-tier family without rounding.
- The focused Mining integration suite now passes ten scenarios across two classes. Its new cache
  contract pins all 22 total pickaxes and every special animation/rate/access definition; live
  scenarios pin membership and quest rejection, the two-cycle dragon branch and crystal-over-dragon
  priority. Infernal/Crystal charge lifecycles, Infernal smelting, creation/recharge/revert/check
  operations and special attacks remain researched and are not claimed by this slice.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,723 integration scenarios
  across 201 classes, with zero failures, errors or skips. The manifest now tracks 411 unique
  groups: 48 discovered, 104 researched, 1 unit-tested, 257 integration-tested and 1 verified. The
  serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 3m37s: 813 actionable tasks, with 62 executed, 6 restored from cache and 745
  up-to-date.
- Runtime PID 1436 now owns port 43594 after loading cache revision 233, 54 plugin modules and 214
  scripts with database schema version 6 valid. Neither the already-open workspace RuneLite nor the
  separate installed application was opened, focused, restarted or modified.

## 2026-09-12 — Infernal pickaxe creation and ornament lifecycle core

- Added an evidence-bounded Infernal pickaxe lifecycle using the cache-233 object families and the
  shared persisted `infernal_tool_charges` field. A standard dragon pickaxe plus smouldering stone
  now requires unboostable level 61 Mining and 85 Smithing, revalidates both inputs after exactly
  three ticks, transforms atomically even in a full inventory, awards 200 Mining and 350 Smithing XP
  and starts at the implicit 5,000-charge state.
- Registered all four cache-233 cosmetic dragon pickaxes against the stone with the cutoff record's
  exact refusal. Both charged/depleted base Infernal states accept the Trailblazer tool ornament kit;
  Revert returns the kit atomically; Trailblazer and Trailblazer Reloaded forms switch in inventory
  and while worn. Every transform preserves packed charge state, including shifted first-option
  Wield behavior for ornamented forms.
- Seven new integration scenarios pin IDs 13243/13244, 25063/25369 and 30345/30346, cache inventory
  and worn operation arrays, event dispatch, symmetric item ordering, exact timing/XP/base-level
  gates, stale-input cancellation, cosmetic refusals, full-inventory behavior, charge persistence and
  final-charge depletion state. The cutoff source marks its passive Smithing-XP table incomplete and
  conflicts with the server's tenth-XP precision for 6.25/18.75 values, so passive smelting,
  recharge, Check presentation, special attack and matching-client ordering remain researched.
- The XML audit contains 1,662 unit scenarios across 107 classes and 1,730 integration scenarios
  across 202 classes, with zero failures, errors or skips. The manifest now tracks 412 unique groups:
  48 discovered, 104 researched, 1 unit-tested, 258 integration-tested and 1 verified. The serialized
  repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m32s: 813 actionable tasks, with 58 executed, 6 restored from cache and 749 up-to-date.
- The server was stopped cleanly before game-test initialization. Updated Java PID 24787 now owns
  port 43594 after loading cache revision 233, 54 plugin modules, 214 scripts and valid schema 6.
  Neither workspace RuneLite nor the separate installed application was opened, focused, restarted
  or modified.

## 2026-09-12 — Mining Rock Knocker special-attack core

- Completed the cache-233 pickaxe special-attack family across all fourteen supported object
  states: five dragon styles, six charged/depleted Infernal styles, third-age and both crystal
  states. The missing Trailblazer Reloaded dragon/Infernal variants now use dedicated sequence
  11822, while cache enum 906 proved that inactive crystal retains the 1,000-unit special. It uses
  the cache's sole crystal Rock Knocker sequence as a bounded same-family inference pending manual
  case 97.
- Every form now resolves through rsmod's existing instant-special repository. Activation consumes
  the 100% energy contract through the combat-tab caller, applies a +3 Mining boost without lowering
  a stronger existing boost, says “Smashing!”, routes the correct family animation and retains the
  existing area sound behavior.
- Three focused GameTest scenarios pin all object IDs, enum energy entries, registry types, eight
  animation families, boost/cap behavior, overhead phrase and full-bar deduction contract. The XML
  audit contains 1,665 unit scenarios across 108 classes and 1,730 integration scenarios across 202
  classes, with zero failures, errors or skips. The manifest now tracks 413 unique groups: 48
  discovered, 104 researched, 2 unit-tested, 258 integration-tested and 1 verified.
- The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 3m48s: 815 actionable tasks, with 136 executed, 6 restored from cache and 673
  up-to-date. Exact special-bar failure presentation, sound placement and packet order remain in
  workspace-client case 97.
- The server was stopped cleanly before game-test initialization. Updated Java PID 50770 owns port
  43594 after loading cache revision 233, 54 plugin modules, 214 scripts and valid schema 6. Neither
  workspace RuneLite nor the separate installed application was opened, focused, restarted or
  modified.

## 2026-09-12 — Standard gem-rock extraction core

- Pinned the final pre-cutoff Gem rocks revision 14972723 and cache-233.4 location identities
  11380/11381. Both use level 40, 65 Mining XP, exact 99-cycle replacement into model-matched empty
  locations 11390/11391 and shared success endpoints 28/70.
- Added the exact 128-outcome reward partition: opal 60, jade 30, red topaz 15, sapphire 9, emerald
  5, ruby 5 and diamond 4. Rewards use typed object references, award once atomically, publish the
  existing mined-ore event and play cache synth 2655. All charged ordinary/trimmed glory states plus
  eternal glory triple only gem-rock endpoints to 84/210; uncharged glories and ordinary rocks are
  explicit exclusions.
- Six focused cache/game scenarios were added for both location definitions, every weighted-table
  boundary, level 40, reward/XP/sound/replacement behavior, the inclusive 99-cycle lifecycle, full
  inventory and all glory variants. The complete Mining integration suite passes 23 scenarios.
  Provisional success/full-capacity text and exact client packet presentation remain workspace-only
  manual case 98; clue geodes, rock golem, enhancer exclusions and access integration remain in the
  researched Mining row rather than being overclaimed.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,736 integration scenarios
  across 202 classes, with zero failures, errors or skips. The manifest now tracks 414 unique groups:
  48 discovered, 104 researched, 2 unit-tested, 259 integration-tested and 1 verified.
- The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 2m22s: 815 actionable tasks, with 58 executed, 6 restored from cache and 751
  up-to-date. Neither workspace RuneLite nor the separate installed application was opened,
  focused, restarted or modified.
- The server was restarted after verification and is again bound to port 43594 with cache revision
  233, 54 plugin modules, 214 scripts and database schema 6.

## 2026-09-12 — Ordinary-rock Mining gem pre-table core

- Pinned the final pre-cutoff Mining, Gems and `MiningPreGemDropTable` records at oldids 14991661,
  14466622 and 14994095, plus Gem bag transcript oldid 14860843. Every eligible ordinary-rock
  attempt now rolls the pre-table before the ore success roll: 128/32768 normally and 128/8216
  while any charged ordinary, trimmed or eternal amulet of glory is equipped.
- Added the exact ordered 128-weight partition: Nothing 70, uncut sapphire 32, uncut emerald 16,
  uncut ruby 8 and uncut diamond 2. Any table hit consumes that attempt without ore, Mining XP or
  rock depletion; gem rewards use the cutoff article/name message and cache synth 2655, and mining
  continues at the pickaxe's normal cadence. Gem rocks remain on their separate reward table and do
  not roll this pre-table.
- Three focused scenarios cover the diamond endpoint, silent Nothing branch, and ordinary ore
  success after a pre-table miss. Together with exact table-boundary and denominator assertions,
  every charged and uncharged glory state is pinned; the complete Mining integration suite now
  passes 26 scenarios.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,739 integration scenarios
  across 202 classes, with zero failures, errors or skips. The manifest now tracks 415 unique groups:
  48 discovered, 104 researched, 2 unit-tested, 260 integration-tested and 1 verified.
- The serialized repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration`
  gate passed in 2m24s: 815 actionable tasks, with 56 executed, 6 restored from cache and 753
  up-to-date. Open-gem-bag routing, already-worn charged-glory behavior after entering a free world,
  and exact workspace-client message, sound and packet presentation remain bounded by manual case
  99. Neither workspace RuneLite nor the separate installed application was opened, focused,
  restarted or modified.
- The server was restarted after verification and is again bound to port 43594 with cache revision
  233, 54 plugin modules, 214 scripts and database schema 6.

## 2026-09-12 — Gem bag lifecycle and Mining routing core

- Added cache-233 Gem bag identities 12020/24481 and a permanent, never-stacking 300-slot backing
  inventory that enforces the cutoff's independent 60-item caps for uncut sapphire, emerald, ruby,
  diamond and dragonstone. Both visible states now follow their cache operations and the repository's
  plugin syntax for Fill, Open/Close, Check, inventory Empty, bankside Empty and Destroy.
- Implemented symmetric gem-on-bag storage, cut-gem rejection, exact cutoff transcript feedback,
  sapphire-first partial inventory emptying, full bankside transfer and members gating. Destroy
  clears stored contents atomically; the backing store is configured with permanent scope and
  follows the cutoff page's retained-on-death statement pending manual resolution of conflicting
  later prose.
- Added a shared acquisition hook and connected both Mining reward paths. An open bag now intercepts
  eligible sapphire-through-diamond ordinary-rock pre-table rewards and eligible sapphire-through-
  diamond standard gem-rock rewards, with the transcript's second direct-storage message. Ground-
  item pickup, Thieving, one-bag ownership/shop rules and matching-client death/presentation edges
  remain explicitly researched under manual case 100.
- Six focused Gem bag integration scenarios and the expanded 27-scenario Mining suite pass. They pin
  both cache identities and operation arrays, all accepted object IDs, storage type/capacity, fill,
  held-use, cut rejection, state/check messages, partial inventory order, bankside empty, membership,
  destroy and both Mining acquisition routes.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,746 integration scenarios
  across 203 classes, with zero failures, errors or skips. The manifest now tracks 416 unique groups:
  48 discovered, 104 researched, 2 unit-tested, 261 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed
  in an isolated single-worker run after the default 2 GB Kotlin daemon exhausted its heap while
  compiling unrelated Crafting modules concurrently: 821 actionable tasks, with 61 executed, 6
  restored from cache and 754 up-to-date. No source or test regression caused that resource retry.
- The server was restarted after verification and is bound to port 43594 with cache revision 233,
  55 plugin modules, 215 scripts and database schema 6. Neither workspace RuneLite nor the separate
  installed application was opened, focused, restarted or modified.

## 2026-09-12 — Gem bag ground-item Take routing

- Added a reusable `ObjTakeDestination` plugin policy with deterministic priority ordering. The
  standard Take path now performs its destination transaction preflight before removing the ground
  object, re-resolves the destination after walking, and commits only after repository removal,
  closing the prior inventory-race window without changing ordinary inventory fallback behavior.
- Connected the open Gem bag through that policy. Members now route a complete eligible uncut-gem
  ground stack directly into its independent per-type storage when the whole stack fits; same-tile
  and one-tile delayed pickup retain the standard Take cadence and sound. Closed bags, free worlds,
  rejected objects and exhausted per-gem capacity fall back to ordinary inventory semantics.
- Expanded the focused Gem bag suite from six to eight scenarios. The new coverage proves full-
  inventory same-tile interception, delayed pickup, exact direct-storage feedback, closed-bag
  inventory fallback and full-capacity preservation of the ground object and inventory-space
  message. The first production Thieving slice now exists, but its Man/Woman family awards only
  coins, so gem-bearing acquisition routes remain researched under manual case 100.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,748 integration scenarios
  across 203 classes, with zero failures, errors or skips. The manifest still tracks 416 unique
  groups: 48 discovered, 104 researched, 2 unit-tested, 261 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed
  in an isolated single-worker run in 6m14s: 821 actionable tasks, with 57 executed, 6 restored from
  cache and 758 up-to-date.
- The server was restarted after verification and is bound to port 43594 with cache revision 233,
  56 plugin modules, 215 scripts and database schema 6. Neither workspace RuneLite nor the separate
  installed application was opened, focused, restarted or modified.

## 2026-09-12 — Man/Woman Thieving and citizen coin-pouch core

- Added the first production Thieving module using rsmod's protected NPC-operation and inventory-
  transaction structures. It registers all 27 cache-operable historical Man/Woman identities and
  follows the exact level-1, 8-XP, three-coin, 180/240 cutoff contract on a two-cycle success
  cadence. Historical identity 8858 remains deliberately unregistered because cache 233.4 has no
  pickpocket operation for it.
- Added shared coin-pouch accounting across all twenty cache pouch types, exact 28/56/84/140 diary
  caps and atomic citizen `Open`/`Open-all` exchange to three coins per pouch with coin-jingle
  synth 2114.
- Added failed-pickpocket presentation and state: NPC shout, sequence 1054, spotanim 245, queued
  one-point typeless damage, eight-cycle pickpocket block and separate nine-cycle movement block.
  Shadow Veil's 15% roll occurs before the dodgy necklace's 25% roll; successful veil suppression
  prevents the shout, and the necklace consumes its ten item-local charges.
- Added exact Gloves of silence, hard-diary and stacked Thieving-cape cutoff modifiers. The
  location-scoped medium-diary bonus remains open rather than being applied globally.
- Added ten focused integration scenarios for cache identities and operations, cadence, reward,
  failure, movement/action blocking, protection ordering and charges, pouch limits and opening,
  success modifiers, capacity and membership. Only workspace RuneLite source was read to pin the
  timer-compatible chat strings and synth identities; no RuneLite client was opened or modified.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,758 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 418 unique
  groups: 48 discovered, 105 researched, 2 unit-tested, 262 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed
  in a single-worker run in 6m56s: 827 actionable tasks, with 469 executed, 7 restored from cache
  and 351 up-to-date.
- The server was restarted after verification and is bound to port 43594 with cache revision 233,
  56 plugin modules, 216 scripts and database schema 6. Neither workspace RuneLite nor the separate
  installed application was opened, focused, restarted or modified.

## 2026-09-12 — Farmer pickpocket and Rogue reward-multiplier core

- Extended the typed pickpocket registry with all twenty cache-operable ordinary, Varlamore,
  Kastori and Tal Teklan Farmer identities. Every type uses operation slot three and shares the
  dated level-10, 14.5-XP, one-damage, five-second-stun and 150/240 success contract.
- Replaced the fixed pickpocket output with a reusable weighted-reward definition. Farmers now use
  the exact 128-weight table: one nine-coin Farmer pouch at 123/128 or one potato seed at 5/128.
  Farmer pouch `Open` and `Open-all` use the same atomic generalized handler as citizen pouches.
- Added the dated Rogue outfit contract across every supported pickpocket: one through four
  correctly worn pieces give 15/30/45/60% double-loot chances and all five guarantee it. Ordinary
  rewards double their count, while a coin-pouch result remains one pouch and adds the pouch's coin
  value directly. XP remains one award. Four Rogue pieces plus Gloves of silence correctly retain
  the 60% reward chance while applying the Gloves success modifier.
- Reward delivery is a single inventory transaction. A doubled pouch requiring a new direct-coin
  stack rolls back the pouch and XP together if it cannot fit; the unresolved exact production
  presentation for rare Farmer rewards in a full inventory remains manual case 101.
- Expanded the focused Thieving suite from ten to twenty scenarios. It now pins all 47 registered
  cache NPCs, Farmer reward and chance boundaries, both pouch values and operations, every Rogue
  threshold, the 14/15 random boundary, guaranteed pouch/non-pouch doubling, unchanged XP and
  atomic capacity rollback.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,768 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 419 unique
  groups: 48 discovered, 105 researched, 2 unit-tested, 263 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed
  in a single-worker run in 6m14s: 827 actionable tasks, with 58 executed, 6 restored from cache
  and 763 up-to-date.
- The verified server replaced the prior process and is bound to port 43594 with cache revision 233,
  56 plugin modules, 216 scripts and database schema 6. Neither workspace RuneLite nor the separate
  installed application was opened, focused or modified.

## 2026-09-12 — Warrior and ordinary Guard pickpocket cores

- Added the seven cache-operable ordinary and Al Kharid Warrior identities to the shared typed
  pickpocket registry. Their dated contract is level 25, 26 XP, one 18-coin Warrior pouch,
  100/240 success endpoints, two failure damage and the shared five-second stun behavior.
- Added all 91 cache-operable ordinary city and regional Guard identities with level 40, 46.8 XP,
  one 30-coin Guard pouch, 50/240 success endpoints, two failure damage and the shared stun.
  A cache-wide diagnostic reconciled 96 Guard-labelled pickpocket types: these 91 operation-three
  ordinary Guards plus five operation-one H.A.M. storeroom guards 4522–4526. The latter are
  explicitly excluded because their key-loot/ejection contract is a separate unfinished slice.
- Generalized the active pouch registry through the two additional cache objects. Warrior and
  Guard `Open`/`Open-all` now use the same atomic exchange as citizen and Farmer pouches, yielding
  exactly 18 and 30 coins per pouch and participating in the shared mixed-pouch ceiling. Rogue
  equipment doubling automatically retains one pouch and grants the matching coin value directly.
- Added eight focused cache-aware scenarios for the exact NPC sets and operation slots, object
  contracts, level/XP/reward behavior, modifier endpoints, two-damage failures, stun state and
  both pouch operations. The complete focused Thieving suite now contains 28 passing scenarios and
  all 145 registered NPC types are constrained to operation slot three.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,776 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 421 unique
  groups: 48 discovered, 105 researched, 2 unit-tested, 265 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed
  in a single-worker run in 6m14s: 827 actionable tasks, with 58 executed, 6 restored from cache
  and 763 up-to-date.
- Replaced the pre-change server with the verified build. Process 25602 is listening on port 43594
  with cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client
  support. Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Rogue pickpocket and variable coin-pouch core

- Added cache operation-three ordinary Rogue identity 526 to the typed pickpocket registry. The
  wilderness Rogue identity 6603 remains explicitly excluded because cache 233.4 exposes no
  Pickpocket operation for it.
- Implemented the dated Rogue contract: level 32, 36.5 XP, two failure damage, the shared stun,
  and success endpoints of 75/240 normally, 78/252 with Gloves of silence, 82/264 with the hard
  diary and 90/290 with both the diary and Thieving cape.
- Added the exact 144-weight reward table: a 25–40-coin Rogue pouch at 123/144, eight air runes at
  9/144, one jug of wine at 6/144, one lockpick at 5/144 and one poisoned iron dagger at 1/144.
  The Rogue outfit multiplier preserves one coin pouch and awards a separately rolled 25–40 coin
  amount directly, while non-pouch rewards double normally and XP remains a single award.
- Generalized coin-pouch values from a fixed integer to a positive integer range. Fixed-value
  pouches retain deterministic behavior without consuming random state; each variable pouch is
  rolled independently when `Open` or `Open-all` converts it to coins.
- Added five focused scenarios covering the exact cache identities and operation boundary, reward
  table endpoints, failure state, success modifiers, independent variable-pouch rolls and the full
  Rogue outfit interaction. The focused Thieving suite now contains 33 passing scenarios and all
  146 registered NPC types remain constrained to operation slot three.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,781 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 422 unique
  groups: 48 discovered, 105 researched, 2 unit-tested, 266 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m10s: 827 actionable tasks, with 60 executed, 6 restored from
  cache and 761 up-to-date.
- Replaced the pre-change server with the verified build. Process 33806 is listening on port 43594
  with cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client
  support. Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Ardougne and Varlamore Knight pickpocket core

- Added exactly five cache-operable Knight of Ardougne identities (3297, 3300, 8854, 11902 and
  11936) and six Knight of Varlamore identities (13114–13119) to the typed pickpocket registry.
  Every registered identity exposes Pickpocket in operation slot three; same-name dialogue,
  combat and quest variants without that operation remain excluded.
- Implemented the dated shared Knight contract: level 55, 84.3 XP, one 50-coin Knight pouch,
  50/240 success endpoints, three failure damage and the shared stun behavior. Knight pouch
  `Open` and `Open-all` use the same atomic generalized handler as every other active pouch.
- Scoped the medium Ardougne Diary modifier to the fixed Knight of Ardougne family while retaining
  the hard-diary modifier globally. The Thieving cape stacks with either applicable diary bonus.
  Knights of Varlamore deliberately receive no medium-diary bonus. The unresolved general
  coordinate predicate for other Ardougne NPC families remains documented as a separate gap.
- Added four focused cache-aware scenarios covering both exact NPC sets and operation slots,
  level/XP/reward behavior, failure damage and stun state, medium-versus-hard diary behavior,
  cape stacking and fixed-value pouch operations. The focused Thieving suite now contains 37
  passing scenarios and all 157 registered NPC types are constrained to operation slot three.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,785 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 423 unique
  groups: 48 discovered, 105 researched, 2 unit-tested, 267 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m12s: 827 actionable tasks, with 58 executed, 6 restored from
  cache and 763 up-to-date.
- Replaced the pre-change server with the verified build. Process 52291 is listening on port 43594
  with cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client
  support. Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Yanille Watchman pickpocket and atomic reward bundles

- Added the sole cache-operable Yanille Watchman identity 5420 through its typed symbol and normal
  operation-three dispatcher. The dated contract is level 65, 137.5 XP, 15/160 base success
  endpoints, three failure damage and the shared five-second stun behavior.
- Added fixed 60-coin Watchman pouch 22533 to the active pouch registry and guaranteed bread 2309
  alongside it on every success. The source ledger records the coin-pouch page's broad
  “occasional” wording against the Watchman page's subject-specific Always table and bread-based
  strategy; production follows the more direct Watchman evidence pending matching-client review.
- Generalized reward delivery from one object to an atomic bundle without adding a Watchman-only
  script. Rogue equipment now preserves one pouch, adds 60 coins directly and doubles the bread;
  any capacity failure rolls back the pouch, bread, direct coins and XP together. This structure is
  reusable for later fixed multi-output families such as Paladins.
- Added five focused cache-aware scenarios covering identity and operation, level/XP/rewards,
  chance modifiers, damage/stun, single/all pouch opening, whole-bundle Rogue doubling and atomic
  full-inventory rollback. The focused Thieving suite now contains 42 passing scenarios and all
  158 registered NPC types are constrained to operation slot three.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,790 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 424 unique
  groups: 48 discovered, 105 researched, 2 unit-tested, 268 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m09s: 827 actionable tasks, with 58 executed, 6 restored from
  cache and 763 up-to-date.
- Replaced the pre-change server with the verified build. Process 66722 is listening on port 43594
  with cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client
  support. Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Ardougne Paladin guaranteed pickpocket core

- Added all eight cache-operable East and West Ardougne Paladin identities: 3293, 3294, 8853,
  11901 and 11930–11933. Every identity exposes Pickpocket in operation slot three and is
  registered through one typed definition; quest-only Underground Pass Paladins remain separate.
- Implemented the final pre-cutoff post-rebalance contract: level 70, 131.8 XP, 40/170 base
  success endpoints, three failure damage and the shared stun. Medium Ardougne Diary completion
  applies to this fixed family because every production spawn is within Ardougne, yielding 44/187;
  Gloves produce 42/178 and diary-plus-cape produces 48/205.
- Added Paladin pouch 22535 as a fixed 80-coin active pouch and paired it atomically with two
  guaranteed chaos runes 562. Rogue equipment preserves one pouch, grants 80 coins directly and
  doubles the runes to four without doubling XP. Capacity failure rolls back the complete bundle.
- Inventoried the separate 1/1,000 hard-clue tertiary as researched instead of folding an
  incomplete clue award into the green core. It still requires the shared clue ownership,
  stackability, destination and Rogue-equipment contracts before implementation.
- Added five focused cache-aware scenarios covering the exact family, cache operations,
  level/XP/reward behavior, modifier endpoints, damage/stun, pouch operations, Rogue doubling and
  atomic full-inventory rollback. The focused Thieving suite now contains 47 passing scenarios and
  all 166 registered NPC types are constrained to operation slot three.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,795 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 426 unique
  groups: 48 discovered, 106 researched, 2 unit-tested, 269 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m09s: 827 actionable tasks, with 58 executed, 6 restored from
  cache and 763 up-to-date.
- Replaced the interim server with the verified build. Process 74151 is listening on port 43594
  with cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client
  support. Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Kharidian Desert Bandit weighted pickpocket core

- Pinned final pre-cutoff Bandit Camp revision 14990775 and coin-pouch revision 14993878. Together
  they establish exactly two Kharidian production identities, level 53, 79.4 XP, 50/240 base
  success endpoints, three failure damage and a seven-weight result table: 30 coins at 5/7,
  antipoison(1) at 1/7 and a lockpick at 1/7.
- Registered only cache-operable NPCs 690 and 695 through typed operation-three references.
  Pollnivneach/The Feud IDs 734–737 remain outside this ordinary definition because they belong to
  quest and blackjacking flows. The sourced Saradomin/Zamorak equipment aggression is retained as
  a separate combat-AI gap rather than approximated in the Thieving action.
- Added fixed 30-coin Desert Bandit pouch 22530 to the active atomic `Open`/`Open-all` registry and
  modeled the exact 5/1/1 partition with antipoison(1) 179 and lockpick 1523. Rogue equipment keeps
  one pouch plus 30 direct coins on the pouch branch or doubles either non-pouch result without
  doubling XP. A selected reward that cannot fit rolls back the reward and XP.
- Added six focused cache-aware scenarios covering the exact NPC/operation and object contracts,
  all weighted boundaries, level/XP, each reward branch, failure and stun, all success modifiers,
  pouch opening, both Rogue-doubling forms and capacity rollback. The focused Thieving suite now
  contains 53 passing scenarios and all 168 registered NPC types are constrained to operation slot
  three.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,801 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 427 unique
  groups: 48 discovered, 106 researched, 2 unit-tested, 270 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m09s: 827 actionable tasks, with 60 executed, 6 restored from
  cache and 761 up-to-date.
- Replaced process 74151 with the verified build. Process 84935 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Hero weighted pickpocket core and Fremennik evidence boundary

- Pinned final pre-cutoff Hero revision 14940550 and coin-pouch revision 14993878. Together they
  establish all three cache-operable East Ardougne Heroes, level 80, 163.3 XP, 39/160 base chance
  endpoints, three failure damage, a six-second stun and the exact 128-weight reward table: Hero
  pouch at 105, two death runes at 8, wine at 6, one blood rune at 5, one diamond at 1, one fire
  orb at 2 and one gold ore at 1.
- Registered NPCs 3295, 11934 and 11935 through typed operation-three references, added all seven
  exact reward objects, and activated variable Hero pouch 22537 with independent inclusive
  200–300-coin `Open` and `Open-all` rolls. The generalized pouch definition now carries a
  separately sourced Rogue bonus range: Hero doubling keeps one pouch and adds exactly 200 direct
  coins, while every existing pouch retains its prior direct-coin behavior.
- Modeled the source's six-second failure duration through ten action-block and eleven movement-
  block cycles under the established boundary convention. All three fixed East Ardougne identities
  receive the medium-diary modifier; Gloves, diary and diary-plus-cape endpoints are 40/168,
  42/176 and 46/193. Non-pouch Rogue rewards double, XP remains single, and capacity failures roll
  back the complete reward and XP transaction.
- Kept the independent 1/1,400 elite-clue roll as a researched tertiary. Implementing only an item
  drop would misstate parity until shared clue ownership, stackability, inventory/ground routing
  and Rogue-equipment contracts exist.
- Added one exact configuration scenario and six script scenarios covering family/operation and
  object identities, all weighted boundaries, level/XP, modifier endpoints, failure timing,
  variable pouch opening, Hero's fixed direct Rogue coins, non-pouch doubling and atomic rollback.
  The focused Thieving suite now contains 60 passing scenarios and all 171 registered NPC types are
  constrained to operation slot three.
- Pinned final pre-cutoff Fremennik citizen revision 14992177 separately. It establishes nine named
  NPCs, The Fremennik Trials completion, level 45, 65 XP, a guaranteed 40-coin pouch, two failure
  damage and a five-second stun. Both the dated family page and Thieving overview explicitly leave
  success rate unknown, so no endpoint or production action was invented. NPCs 3937–3941 and
  3943–3946, pouch 22528, quest row 57, the 3942 ambiguity and matching-client work are now
  inventoried for manual case 101.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,808 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 430 unique
  groups: 48 discovered, 108 researched, 2 unit-tested, 271 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m07s: 827 actionable tasks, with 58 executed, 6 restored from
  cache and 763 up-to-date.
- Replaced process 84935 with the verified build. Process 99406 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Gnome weighted pickpocket and variable-count reward core

- Pinned final pre-cutoff Gnome revision 14996384, dated 29 September 2025, and coin-pouch revision
  14993878. They establish level 75, 133.5 XP, 43/175 base success endpoints, one failure damage,
  a five-second stun and the exact 128-weight table: 2–4 arrow shafts at 56, fixed 300-coin pouch at
  30, swamp toad at 24, gold ore at 8, earth rune at 5, king worm at 3 and fire orb at 2.
- Used cache operations to resolve the page's shared subject infobox. Ordinary Gnomes 6094–6096
  expose `Pickpocket` in slot three and are registered. Battlefield troop/archer IDs 5968–5970
  expose combat operations but no `Pickpocket`; tests preserve them as explicit exclusions instead
  of manufacturing an action from their Wiki inclusion.
- Added exact typed references for all six historical identities and all seven ordinary outputs.
  Activated fixed-value Gnome pouch 22536 in the generalized atomic `Open`/`Open-all` handler.
- Extended `PickpocketReward` with a validated positive inclusive count range and one protected-
  transaction roll. The Gnome shaft branch rolls 2–4 once; Rogue equipment doubles that chosen
  count to 4–8 rather than rerolling. The pouch branch retains one pouch plus 300 direct coins,
  every other ordinary branch doubles, XP remains single and capacity failure rolls back reward
  and XP together. Every prior fixed-count definition keeps its one-value default range.
- Kept the independent 1/150 medium-clue roll as a researched tertiary because the dated source
  ties full-inventory behavior to X Marks the Spot and scroll-box ownership. That shared clue,
  stacking, destination and Rogue-equipment lifecycle must exist before promotion.
- Added one exact configuration scenario and six script scenarios covering cache identities and
  exclusions, every 128-weight boundary, both shaft-count endpoints, level/XP, modifier endpoints,
  failure timing, pouch operations, both Rogue forms and atomic rollback. The focused Thieving
  suite now contains 67 passing scenarios and all 174 registered NPC types are constrained to
  operation slot three.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,815 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 432 unique
  groups: 48 discovered, 109 researched, 2 unit-tested, 272 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m09s: 827 actionable tasks, with 58 executed, 6 restored from
  cache and 763 up-to-date.
- Replaced process 99406 with the verified build. Process 11362 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Lletya Elf weighted pickpocket core and Cave goblin evidence boundary

- Pinned final pre-cutoff Elf revision 14814353, the four named Lletya NPC revisions 14813343–
  14813346 and coin-pouch revision 14993878. Together they establish level 85, 353.3 XP, 6/100
  base success endpoints, five failure damage, a six-second stun and the exact 128-weight table:
  Elf pouch at 105, two death runes at 8, three nature runes at 5, wine at 6, diamond at 1, fire
  orb at 2 and gold ore at 1. Elf pouch 22538 independently opens for 280–350 coins.
- Resolved the Wiki's version-grouping ambiguity against cache operations. Goreu 5297, Arvel 5299,
  Kelyn 9015 and Mawrth 5300 expose `Pickpocket` in operation slot three and are registered. Kelyn
  form 8914 and Mawrth quest/cutscene forms 8927–8928 have no Pickpocket operation and remain typed,
  tested exclusions. Mourning's End Part I access and NPC phasing own the location prerequisite;
  the generic action does not invent a second quest check.
- Activated variable Elf pouch 22538 in the generalized atomic `Open`/`Open-all` handler and
  modeled the sourced six-second failure as ten action-block and eleven movement-block cycles.
  Gloves, hard diary and hard-diary-plus-cape endpoints are 6/105, 6/110 and 6/121; the medium
  Ardougne Diary correctly does not affect Lletya. Rogue equipment keeps one pouch plus one direct
  280–350 coin payout or doubles the chosen non-pouch result, while XP remains single and capacity
  failure rolls back reward and XP together.
- Kept the Prifddinas family separately researched. Its ordinary table is shared, but every success
  first rolls an independent 1/35 crystal shard and 1/1,024 enhanced crystal teleport seed. Full
  promotion requires the complete cache identity/phase set, exact tertiary order, Rogue behavior
  and capacity contract rather than silently reusing the Lletya definition without those rewards.
- Pinned Cave goblin revision 14926805 and the cutoff Thieving overview 14991198. The sources give
  fourteen subject forms with 2301 explicitly Talk-to-only, Death to the Dorgeshuun access, level
  36, 40 XP, one damage, five-second stun, nearby-guard alert and a complete twenty-share reward
  table including elite-Lumbridge wire doubling. The same dated overview labels success rate
  `Unknown`, so neither a chance curve nor guard-aggression approximation was invented.
- Added one exact configuration scenario and five script scenarios covering Elf identities and
  exclusions, every reward boundary, level/XP, modifier endpoints, failure timing, variable pouch
  opening, both Rogue forms and atomic rollback. The focused Thieving suite now contains 73 passing
  scenarios and all 178 registered NPC types are constrained to operation slot three.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,821 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 435 unique
  groups: 48 discovered, 111 researched, 2 unit-tested, 273 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m16s: 827 actionable tasks, with 62 executed, 6 restored from
  cache and 759 up-to-date.
- Replaced process 11362 with the verified build. Process 36256 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Darkmeyer Vyre weighted pickpocket core

- Pinned final pre-cutoff Vyre revision 14907001 and resolved all thirty named Darkmeyer subjects
  to the contiguous cache identities 9685–9714. Every identity exposes `Pickpocket` in operation
  slot three. They are registered through typed symbols and protected by a complete cache-operation
  test rather than a name-based approximation.
- Implemented level 82, 306.9 XP, 8/128 base success endpoints, five failure damage and the sourced
  six-second stun as ten action-block and eleven movement-block cycles. Gloves, hard diary and
  hard-diary-plus-cape endpoints are 8/134, 8/140 and 8/154; the medium diary correctly has no
  effect on this family.
- Added the exact 132-weight ordinary table: Vyre pouch 24703 at weight 109, two death runes at 8,
  four blood runes at 2, a blood pint at 6, an uncut ruby at 5, a diamond at 1 and cooked mystery
  meat at 1. The pouch independently opens for 230–315 coins through the generalized atomic
  `Open`/`Open-all` handler.
- Preserved the Vyre-specific Rogue-equipment exception: a pouch success keeps one pouch and adds
  exactly 230 direct coins, regardless of the pouch's eventual value. Every non-pouch ordinary
  result doubles, XP remains single and an insufficient-capacity result rolls back the whole item
  and XP transaction.
- Kept the independent 1/5,000 blood-shard roll as a researched tertiary. The source establishes
  that Rogue equipment can double it to two, but exact ordering relative to the ordinary roll and
  full-inventory/atomic routing are not yet pinned strongly enough to claim production parity.
  Darkmeyer access, hostile Vyrewatch Sentinels and noble-clothes behavior remain world/combat
  responsibilities rather than invented gates inside the generic pickpocket action.
- Added one exact configuration scenario and five script scenarios covering the full NPC family,
  operation slot, reward boundaries, level/XP, modifier endpoints, failure timing, pouch handling,
  Vyre's fixed Rogue payout, non-pouch doubling and atomic rollback. The focused Thieving suite now
  contains 79 passing scenarios and all 208 registered NPC types are constrained to operation slot
  three.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,827 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 437 unique
  groups: 48 discovered, 112 researched, 2 unit-tested, 274 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m07s: 827 actionable tasks, with 60 executed, 6 restored from
  cache and 761 up-to-date.
- Replaced process 36256 with the verified build. Process 45714 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — H.A.M. boundary, Menaphite ordinary core and Knight timing correction

- Pinned final pre-cutoff H.A.M. Member revision 14993132 and resolved both cache identities
  2540–2541 plus all 39 ordinary outputs to typed symbols. Cache 233.4 places their Pickpocket
  action in operation slot one, so a focused boundary test protects them from the generic
  operation-three dispatcher. Level 15, 22.2 XP, 135/239 endpoints, one-to-three failure damage,
  four-second stun, the 1/50 clue pre-roll, conditional 100-share table, variable quantities and
  1–21-coin pouch are documented. Production remains absent until the approximate per-piece H.A.M.
  clothing protection and exact three-concussion jail/outside ejection lifecycle are evidenced.
- Implemented the ordinary post-quest Menaphite Thug pickpocket on cache identity 3550. Cache
  identity 3549 exposes only Talk-to and Attack, while 3550 adds operation-three Pickpocket, Lure
  and Knock-Out, so the cache morph owns quest progression and only 3550 is registered. The core
  uses level 65, 137.5 XP, 50/160 base endpoints, five failure damage, a five-second stun, fixed
  60-coin pouch 22534, shared Gloves/diary/cape modifiers and failure protections, Rogue one-pouch-
  plus-60-direct-coins behavior and atomic rollback. Blackjacking remains separate rather than
  approximating its 78/240 knockout, two-pickpocket window, aggression or special exclusions.
- Corrected both Ardougne and Varlamore Knight failure timing from the inherited five-second
  boundary to the six seconds explicitly recorded by both the Knight subject page and cutoff
  Thieving overview: ten action-block and eleven movement-block cycles. Exact configuration and
  script assertions now prevent regression.
- Recorded the dated Vyre source conflict explicitly. The direct subject page says six seconds,
  while the broad cutoff Thieving table says three; production continues to follow the more
  specific page as ten action-block and eleven movement-block cycles, and manual case 101 now
  requires a matching cache-233 client observation to resolve the disagreement.
- Added one H.A.M. boundary scenario and five Menaphite scenarios across configuration and script
  coverage. The focused Thieving suite now contains 85 passing scenarios: 17 configuration and
  68 script scenarios. All 209 production NPC identities remain constrained to operation slot
  three, with H.A.M.'s operation-one boundary tested separately.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,833 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 439 unique
  groups: 48 discovered, 113 researched, 2 unit-tested, 275 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m07s: 827 actionable tasks, with 60 executed, 6 restored from
  cache and 761 up-to-date.
- Replaced process 45714 with the verified build. Process 77762 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Varlamore Wealthy citizen source and cache boundary

- Pinned final pre-cutoff Wealthy citizen revision 14988663 and resolved all four subjects to cache
  NPCs 13302–13305. Each exposes Pickpocket in operation slot one and Talk-to in operation slot
  three, so typed references and an exact cache test keep the family out of the generic
  operation-three dispatcher.
- Typed and tested fixed 85-coin pouch 28822 and House keys 29325. The sourced ordinary contract is
  level 50, 96 XP, 35/200 base endpoints, three failure damage, a four-second stun and an 85-share
  table: pouch at 79, keys at 5 and easy clue at 1. Gloves, hard diary and hard-diary-plus-cape
  endpoints are 36/210, 38/220 and 41/242.
- Kept production undispatched because key and clue rolls are discarded on a full inventory while
  the attempt still completes, which intentionally differs from the shared atomic rollback model,
  and because clue ownership/routing is unfinished. The direct 79/85 pouch table also conflicts
  with the coin-pouch page's broad every-pickpocket wording; matching-client evidence must decide
  it rather than silently selecting one source.
- Recorded the coupled street-urchin loop: three citizens are distracted every 84–99 seconds for
  roughly 20 seconds of automatic guaranteed pickpockets. The exact recent-world-hop state, roughly
  15-second delayed start, initial no-item window and stop tick remain manual case 101.
- Added one configuration scenario, bringing the focused Thieving suite to 86 passing scenarios:
  18 configuration and 68 script scenarios. All 209 production operation-three NPCs remain
  unchanged; H.A.M. members and Wealthy citizens are independently protected operation-one
  boundaries.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,834 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 440 unique
  groups: 48 discovered, 114 researched, 2 unit-tested, 275 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m09s: 827 actionable tasks, with 58 executed, 6 restored from
  cache and 763 up-to-date.
- Replaced process 77762 with the verified build. Process 88516 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Pollnivneach bandit and blackjacking source/cache boundary

- Pinned final pre-cutoff Bandit (Pollnivneach) revision 14990770, Blackjack revision 14898817,
  Coin pouch revision 14993878, The Feud revision 14986366 and cutoff Thieving revision 14991198.
  Together they establish both bearded identities 736–737 at level 45, 65 XP and 40 coins, both
  non-bearded identities 734–735 at level 55, 84.3 XP and 50 coins, awake pouch routing,
  knocked-out direct-coin routing and the shared 80/240 Knock-Out chart with 10 XP.
- Added typed references for all four subjects and exact cache tests for their quest morphs. The
  first deliberate boundary run proved NPC 734 has `[Talk-to, Attack, Pickpocket, null, null]`;
  the second proved 735 has `[Talk-to, Attack, Pickpocket, Lure, Knock-Out]`. The corrected test
  now pins the same pre/post-unlock split on 736/737 and constrains every ID independently.
- Typed and tested stackable, untradeable `Open-all`/`Open` pouches 22529 and 22532. The dated
  source maps 22529 to the bearded 40-coin reward and 22532 to the non-bearded 50-coin reward.
  Both remain excluded from the active pouch registry because neither ordinary family is safely
  dispatchable yet.
- Kept production absent because the dated Thieving overview explicitly records both ordinary
  success rates as `Unknown`. Full blackjacking also still requires exact cache-233 observations
  for The Feud state, lure/dialogue timing, second-bandit line of sight, knockout duration,
  failure-to-aggression probability and tick transition, the guaranteed-two-pickpocket window,
  combat interruption ordering and full-inventory behavior. Manual case 101 now carries each
  measurement rather than allowing an approximation.
- Added one exact configuration scenario. The focused Thieving suite now contains 87 passing
  scenarios: 19 configuration and 68 script scenarios. All 209 production operation-three NPCs
  remain unchanged; the four Pollnivneach variants and both inactive pouches are tested boundaries.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,835 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 441 unique
  groups: 48 discovered, 115 researched, 2 unit-tested, 275 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m14s: 827 actionable tasks, with 59 executed, 6 restored from
  cache and 762 up-to-date.
- Replaced process 88516 with the verified build. Process 4919 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Prifddinas Elf identity and tertiary boundary

- Expanded the pinned Elf revision 14814353 into its exact 49-name Prifddinas population and
  mapped every name through cache symbols. NPCs 9054–9090 and 9106–9117 all expose Pickpocket in
  operation slot three. Similarly named Amrod 9053 and Nimrodel, Maedhros, Finarfin, Gwindor and
  Eldalote 9101–9105 are absent from the cutoff list and expose no Pickpocket operation, so they
  are typed, tested exclusions rather than being absorbed by an ID range.
- Typed and tested Crystal shard 23962 and Enhanced crystal teleport seed 23959. The Elf page puts
  their 1/35 and 1/1,024 rolls before the shared 128-weight table. Final pre-cutoff Crystal shard
  revision 14988101 confirms Rogue doubling and ground routing when the shard accompanies another
  collected resource with only one free slot; Enhanced seed revision 14962702 separately confirms
  partial-set doubling chance and full-set guaranteed doubling.
- Kept production absent because the sources do not yet pin whether shard, seed and main output
  share one Rogue decision or roll independently, the ordering of a simultaneous dual-tertiary
  hit, the enhanced seed's full-inventory destination, or which outputs commit when the main
  result cannot fit. Manual case 101 now asks those exact transaction questions at zero, one and
  two free slots and records ground ownership/lifetime and packet order.
- Added one exact configuration scenario. The focused Thieving suite now contains 88 passing
  scenarios: 20 configuration and 68 script scenarios. The 49 Prifddinas targets remain outside
  the production dispatcher until their mandatory pre-roll transaction can be implemented whole.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,836 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest remains at 441 unique
  groups: 48 discovered, 115 researched, 2 unit-tested, 275 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m12s: 827 actionable tasks, with 59 executed, 6 restored from
  cache and 762 up-to-date.
- Replaced process 4919 with the verified build. Process 15450 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — H.A.M. storeroom guard source/cache boundary

- Pinned final pre-cutoff Guard (H.A.M. Storerooms) revision 14993129, H.A.M. Storerooms revision
  14853414 and Death to the Dorgeshuun revision 14996424. Together they establish the five
  repeatable post-quest subjects, level 20, 22.2 XP, the separate no-ejection failure lifecycle,
  patrol reset/door aggression and the five-patrol-plus-one-arrest quest-stage boundary.
- Added typed references for post-quest operation-one NPCs 4522–4526, quest patrol NPCs 4516–4520
  and arrest NPC 4521. The cache-aware test proves only the post-quest five expose Pickpocket and
  keeps all eleven forms outside the generic operation-three dispatcher. Production still contains
  209 standard identities.
- Pinned the exact 500-share reward table: each bronze/iron/silver/steel key occupies 50 shares and
  the other 300 shares cover the published H.A.M. clothing, weapon, material, variable-quantity,
  pouch, clue and nested-herb outcomes. The existing 39 ordinary H.A.M. objects and 1–21-coin pouch
  22523 cover that shared output family; typed keys map bronze/iron/silver/steel to
  8867/8869/8868/8866.
- Pinned key revisions 14796474, 14716380, 14424090 and 14424334, including chest compass mapping,
  one-key consumption and metal-specific break messages. The first focused test deliberately
  failed because cache 233 reports `tradeable=true`; the corrected boundary now captures that all
  four are non-stackable placeholder objects with `stockmarket=false`, while the dated pages
  require them to remain nontradeable. Future trade handling must override the cache default.
- Kept the action undispatched because the cutoff guard page explicitly carries `Needs skilling
  success chart`, and no independent source pins exact success endpoints. Exact stun duration,
  damage distribution within “up to 3”, clue/capacity and Rogue routing, quest morph tick,
  patrol/line-of-sight timing and cache-versus-Wiki trade enforcement also remain manual case 101.
  No chance or lifecycle value was fabricated.
- Added one cache configuration scenario, bringing the focused Thieving suite to 89 passing
  scenarios: 21 configuration and 68 script scenarios. The XML audit contains 1,665 unit scenarios
  across 108 classes and 1,837 integration scenarios across 205 classes, with zero failures, errors
  or skips. The manifest now tracks 442 unique groups: 48 discovered, 116 researched, 2 unit-tested,
  275 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m31s: 827 actionable tasks, with 61 executed, 6 restored from
  cache and 760 up-to-date.
- Replaced process 15450 with the verified build. Process 34858 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — H.A.M. storeroom Small chest source/cache boundary

- Pinned final pre-cutoff Small chest revision 14770595 and resolved its full cache/map boundary.
  Closed steel/bronze/silver/iron types are 15722/15723/15724/15726 and operationless open type is
  15725. A map-registry integration assertion protects all six placements: bronze south-west,
  steel south-east and both northern positions, silver north-west and iron north-east.
- Typed the twenty cache reward objects and recorded the exact 150-share table: 0–99 coins at 75
  plus nineteen published gem/jewellery bundles whose weights sum to 75. The existing four key
  references now have a cache- and coordinate-proven chest destination rather than compass text
  alone.
- Kept production undispatched because the dated source does not establish missing/wrong-key
  feedback, multi-output inventory capacity and partial-commit semantics, closed/open transform
  and replacement timing, concurrent opener behavior or the zero-coin result's packet/message
  treatment. Manual case 101 now requests those exact observations at every capacity and
  contention boundary.
- Added the registry API only to the Thieving integration-test classpath and added one exact
  scenario. The focused Thieving suite now contains 90 passing scenarios: 22 configuration and 68
  script scenarios. The production registration remains unchanged at 209 operation-three NPCs.
- The manifest now tracks 443 unique groups: 48 discovered, 117 researched, 2 unit-tested, 275
  integration-tested and 1 verified.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,838 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 443 unique
  groups: 48 discovered, 117 researched, 2 unit-tested, 275 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m28s: 827 actionable tasks, with 59 executed, 6 restored from
  cache and 762 up-to-date.
- Replaced process 34858 with the verified build. Process 48146 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — H.A.M. storeroom locked-door source/cache boundary

- Expanded final pre-cutoff H.A.M. Storerooms revision 14853414 into its door contract: four
  central locked rooms, exactly 4 Thieving XP, improved odds with a lockpick, two south-west crack
  bypasses, an unlocked northern-room door and patrol-line-of-sight aggression that is disabled for
  a guard after its failed-pickpocket reset.
- Typed `dttd_ham_door` 15759 and added an exact cache/map scenario for its four coordinates,
  description and lockpick 1523. The first deliberate run exposed that cache 233.4 places
  `Pick-lock` in operation slot four rather than slot two; the corrected test protects the exact
  slot-one `Open`/slot-four `Pick-lock` boundary.
- Kept production undispatched because the cutoff page gives neither a minimum level nor exact
  with/without-lockpick success endpoints, boosted rounding, failure/retry timing, door transition,
  presentation or full observer/patrol state. Case 101 now requests each value rather than
  borrowing a generic door formula.
- Added one exact configuration scenario. The focused Thieving suite now contains 91 passing
  scenarios: 23 configuration and 68 script scenarios. The manifest now tracks 444 unique groups:
  48 discovered, 118 researched, 2 unit-tested, 275 integration-tested and 1 verified.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,839 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 444 unique
  groups: 48 discovered, 118 researched, 2 unit-tested, 275 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m29s: 827 actionable tasks, with 61 executed, 6 restored from
  cache and 760 up-to-date.
- Replaced process 48146 with the verified build. Process 58388 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Phantom Muspah Ancient Ice freeze resistance

- Pinned final pre-cutoff Phantom Muspah revision 14997275: all five listed forms carry 33% freeze
  resistance and remain frozen for roughly two-thirds of the base Ice-spell duration. The same
  dated change history records the 15 February 2023 fix that stopped the duration reduction from
  applying twice; the official 8 February Poll 78 update independently establishes the percentage
  as the production NPC resistance field.
- Added 33% resistance to teleporting form 12082, completing the configured ranged 12077, melee
  12078, teleporting 12082, shielded 12079 and post-shield 12080 set. Ancient Ice now applies the
  clamped NPC percentage once after any sceptre extension, yielding ordinary Muspah durations of
  5/10/16/21 ticks and preventing a bind at 100% resistance. Standard and Arceuus binding were left
  unchanged because the dated source specifically scopes this reduction to Ice spells.
- Added focused integration coverage for all four reduced durations and complete immunity. An
  initial cache-type assertion correctly failed with zeroes because the game-test fixture exposes
  raw cache types before enrichment; it was removed instead of weakening the assertion. The real
  packed-cache/server startup gate owns validation of the TOML enrichment path.
- The focused Ancient spell suite now contains 12 passing scenarios. Production-wide resistance
  and immunity beyond Phantom Muspah, non-Ice consumers, encounter overrides and matching-client
  observation remain explicitly open in the manifest and manual case 83.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,841 integration scenarios
  across 205 classes, with zero failures, errors or skips. The manifest now tracks 445 unique
  groups: 48 discovered, 118 researched, 2 unit-tested, 276 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m30s: 827 actionable tasks, with 61 executed, 6 restored from
  cache and 760 up-to-date.
- Replaced process 58388 with the verified build. Process 78298 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 216 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Poll 85 Fremennik sea boots teleport core

- Pinned the official 17 September 2025 Poll 85 update and final pre-cutoff revisions for all four
  Fremennik sea boots. The sources establish shared daily tier limits of 1/3/5/unlimited and the
  exact Rellekka market destination at 2643,3675; cache 233.4 resolves objects 13129–13132 with
  third held and second worn `Teleport` operations.
- Added a dedicated travel plugin using rsmod's existing event and protected-access patterns. The
  handler enforces members-only use, standard four-tick teleport effects, Tele Block and the normal
  level-20 Wilderness boundary. Two permanent server-only varps retain a UTC-day stamp and shared
  use count across tier changes and relogs; all rejected paths leave that state unchanged.
- Added five focused integration scenarios covering every object and operation hook, persistent
  varp scope, destination/effects, all four tier quotas, cross-tier sharing, UTC rollover and the
  three atomic rejection gates. The interrupted first runner left a partial binary result; a
  module clean removed only that generated output, after which both focused runs passed.
- Added manual case 102 for exact rejection wording, audiovisual and packet order, relog/rollover
  presentation and both inventory/equipment routes through only the workspace RuneLite. The other
  Poll 85 bridge, shop, rope, Harmony-patch and Hunter-kit changes remain separately researched.
- The XML audit contains 1,665 unit scenarios across 108 classes and 1,846 integration scenarios
  across 206 classes, with zero failures, errors or skips. The manifest now tracks 447 unique
  groups: 48 discovered, 119 researched, 2 unit-tested, 277 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 7m10s: 833 actionable tasks, with 463 executed, 7 restored from
  cache and 363 up-to-date.
- Replaced process 78298 with the verified build. Process 12953 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 217 scripts, database schema 6 and Desktop client support.
  Neither workspace RuneLite nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — final cache-233.4 baseline correction

- Audited every OpenRS2 revision-233 capture instead of treating the major revision as a unique
  cache. Archive 2293 was captured on 10 September 2025 and is 233.1; archive 2317 was captured
  on 1 October 2025 at `2025-10-01T10:45:07.114859Z` and is the final 233.4 target.
- Replaced the active vanilla cache and XTEAs with archive 2317 while retaining the old archive
  recoverably at `.data/cache-233.1-archive-2293`. The final vanilla `main_file_cache.dat2` SHA-256
  is `33c723c3032bf33cb7224a3a48a3c94046472ce65962e2223ce870c6cc7de416`.
- Added explicit final-subrevision, archive and capture-time constants to `Build`, changed both
  download URLs to archive 2317, and strengthened `validateCache233Parity` so the manifest and
  source lock cannot silently regress to another capture with the same major revision.
- Cache packing exposed sequence opcode 16. Implemented its signed vertical-offset byte in the
  decoder and retained the value in the sequence type model, with positive and negative unit
  coverage based on the matching RuneLite cache loader format.
- The October shop interface no longer contains `shopmain:desktop_instructions` (component 300:19).
  Removed the obsolete client-text write and public subtext argument, then updated 39 stable
  interface/component identity hashes whose cache definitions changed between the captures.
- Archive 2317 now packs cleanly. The affected shop integration, cache decoder tests, repository
  formatting and the 447-group parity validator pass. Process 41271 is listening on port 43594
  with revision 233, 56 plugin modules, 217 scripts, database schema 6 and Desktop client support.
  RuneLite remained closed; neither workspace RuneLite nor `/Applications/RuneLite.app` was
  opened or modified.

## 2026-09-12 — Poll 85 Fenkenstrain Mausoleum bridge inventory-material core

- Added a dedicated travel plugin for the cache-233.4 bridge family 5159/5160/57710–57714. It
  restores the cave-unlocked island-first repair, persistent two-stage state, Construction 59,
  hammer plus six teak planks and 28 mixed nails per side, 550 XP per side, sourced collapse and
  reinforcement messages, Agility 69 zero-XP crossing and the two-distinct-surface-tile cinematic.
- Added reusable per-player location overrides to the engine and player-output path. Incoming
  location and item-on-location packets resolve those overrides before the shared world registry;
  interaction validity retains them; visible state changes send immediately; map rebuilds resend
  them. The global location registry and collision map remain untouched by a player's repair state.
- Reserved server-only permanent varp 65498, named the five new October bridge locations, and
  repacked the cache successfully. The vanilla archive-2317 baseline remains preserved separately
  from enrichment.
- Added nine bridge integration scenarios and extended the zone processor regression. Focused
  bridge and game-process suites pass, including normal incoming clicks, immediate location packets
  and build-area restoration.
- Exact bridge angle and landing-square confirmation, jump/build/camera timing and audiovisuals,
  failure wording, packet order and plank-sack withdrawal remain manual case 103. No plank-sack
  state model exists in the repository, so the inventory-material core does not pretend to cover it.
- The manifest now tracks 448 unique groups: 48 discovered, 119 researched, 2 unit-tested, 278
  integration-tested and 1 verified. RuneLite remained closed; neither the workspace checkout nor
  `/Applications/RuneLite.app` was opened or modified.
- The XML audit contains 1,666 unit scenarios across 109 classes and 1,855 integration scenarios
  across 207 classes, with zero failures, errors or skips.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 7m27s: 839 actionable tasks, with 373 executed, 7 restored from
  cache and 459 up-to-date.
- Process 88904 is listening on port 43594 with cache revision 233, 56 plugin modules, 218 scripts,
  database schema 6 and Desktop client support. RuneLite remains closed; neither the workspace
  checkout nor `/Applications/RuneLite.app` was opened or modified.

## 2026-09-12 — Poll 85 Baxtorian Falls cache-233.4 first-rope core

- Resolved the Poll 85 rope family through direct final-cache inspection: wrapper locations
  57703/57706 use Kandarin-medium reward varbit 4516 and morph to no-op/`Take` variants
  57704/57705 and 57707/57708. Added their matching Jagex gameval names plus typed references for
  Waterfall Quest varp 65 and the diary reward.
- Corrected the target against the locked archive timestamp instead of importing a modern fix.
  Cache 233.4 was captured on 1 October 2025, after the 17 September hotfix sent the first rope to
  the medium-clue plateau and disabled the second rope, but before the second rope was restored on
  17 December. Production therefore installs only the first wrapper at 2511,3477 and lands at
  2512,3467; it intentionally does not provide direct dungeon-ledge access in this snapshot.
- Enforced members, Waterfall journal-complete state 10 and the medium Kandarin reward. Taking the
  coil neither adds nor consumes an inventory rope. The implementation uses the existing per-player
  location override and protected-access patterns in the travel plugin; the provisional visual
  angle and exact presentation remain isolated in manual case 104 rather than being claimed exact.
- Added five integration scenarios covering all six cache identities and hashes, both morph arrays,
  progression gates, cutoff-only placement, inventory invariance, clue-plateau arrival and the
  ordinary incoming location packet. Removed the temporary cache probe and its temporary registry
  dependency after extracting the evidence. Cache packing completed successfully.
- The XML audit contains 1,666 unit scenarios across 109 classes and 1,860 integration scenarios
  across 208 classes, with zero failures, errors or skips. The manifest now tracks 449 unique
  groups: 48 discovered, 119 researched, 2 unit-tested, 279 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m42s: 839 actionable tasks, with 135 executed, 6 restored from
  cache and 698 up-to-date.
- Replaced process 88904 with the verified build. Process 23460 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 218 scripts, database schema 6 and Desktop client support.
  Codex did not open or modify either RuneLite target. A separately launched
  `/Applications/RuneLite.app` process 24969 was observed after validation and deliberately left
  untouched; the workspace RuneLite was not opened.

## 2026-09-12 — Poll 85 hunting-expert Hunter Kit grant core

- Pinned the release-day Feldip Hills and Rellekka hunting-expert transcripts at revisions
  14987663 and 14987661. Those cutoff pages establish the new option, shared already-claimed state
  and exact refusal while explicitly leaving the successful exchange untranscribed. Later
  transcription-only revisions 15119975 and 15076546 supply the identical offer, item box and
  thanks without changing the dated Poll 85 mechanic.
- Added a dedicated Hunter plugin using cache NPCs 1504 and 1503, their exact Talk-to operation,
  identities and opening/menu text, the successful/repeat grant lines and Hunter kit 11159. The
  surrounding legacy tutorial branches currently retain only their sourced entry dialogue and
  remain explicitly open rather than being represented as complete.
- Reserved permanent server-only varp 65497 as the shared one-time entitlement. Either expert
  grants exactly one kit, the state survives subsequent item loss, and a full inventory rolls back
  without spending the claim. The generic capacity line remains provisional pending case 105.
- Added five cache-backed scenarios covering both identities and event hooks, permanent scope,
  first and repeat claims, cross-expert/item-loss persistence, full-inventory atomicity and an
  independently created Lunar kit. Cache packing completed successfully.
- The manifest now tracks 450 unique groups: 48 discovered, 119 researched, 2 unit-tested, 280
  integration-tested and 1 verified. The workspace RuneLite was not opened; the independently
  running `/Applications/RuneLite.app` process was left untouched.
- The XML audit contains 1,666 unit scenarios across 109 classes and 1,865 integration scenarios
  across 209 classes, with zero failures, errors or skips.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 7m20s: 845 actionable tasks, with 477 executed, 7 restored from
  cache and 361 up-to-date.
- Process 48895 is listening on port 43594 with cache revision 233, 56 plugin modules, 219 scripts,
  database schema 6 and Desktop client support. Codex did not open or modify either RuneLite
  target; the independently running `/Applications/RuneLite.app` process was left untouched.

## 2026-09-12 — Hunting-expert tutorials and Hunter Skillcape core

- Replaced the two deliberately partial expert handlers with the complete legacy tutorial trees
  from the final pre-cutoff Feldip and Rellekka transcript revisions, preserving their option
  order, nested menus, return paths, deliberate spelling and NPC-specific endings.
- Added Feldip's base-Hunter-99 master menu and complete Hunter Skillcape purchase dialogue. The
  sale charges exactly 99,000 inventory coins, includes hood 9950, chooses cape 9948 for a first
  mastery or trimmed cape 9949 when another skill is mastered, and commits all three mutations as
  one atomic transaction.
- Expanded the focused suite from five to fourteen scenarios. New coverage pins the Skillcape
  cache objects and constants; base-level, coin and capacity rejection; untrimmed/trimmed output;
  both ordinary Talk-to packet routes; and a successful purchase through the real level-99
  dialogue menu. All fourteen pass.
- The manifest now tracks 451 unique groups: 48 discovered, 119 researched, 2 unit-tested, 281
  integration-tested and 1 verified. Purchase music, exact expressions/pagination/packet order,
  already-owned-cape trimming and any undocumented hood reclaim remain isolated in manual case
  105. No RuneLite client was opened or modified.
- The XML audit contains 1,666 unit scenarios across 109 classes and 1,874 integration scenarios
  across 209 classes, with zero failures, errors or skips.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m31s: 845 actionable tasks, with 61 executed, 6 restored from
  cache and 778 up-to-date.
- Replaced process 48895 with the verified build. Process 67977 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 219 scripts, database schema 6 and Desktop client support.
  Codex opened no RuneLite client; the workspace checkout remains the only permitted future client
  target and the independently running application outside it was left untouched.

### Hunter Skillcape purchase-jingle completion

- Added the cache-233 `cape_of_accomplishment` jingle symbol and typed reference for ID 231, then
  made the Hunter Skillcape transaction emit it only after a successful commit. Base-level, coin
  and capacity rejection paths remain silent and atomic.
- Tightened the existing fourteen-scenario Hunter Expert suite to pin jingle ID 231, successful
  untrimmed/trimmed/direct-dialogue playback and silence on capacity rejection. All fourteen pass.
- Corrected the Poll 85 records: the release changed desktop shop quantity buttons and the selected
  left-click purchase amount; it did not change shop stock lists. Automatic cape trimming also
  belongs to the second-99 level-up lifecycle, not to a later cape purchase, and remains separately
  tracked.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m35s: 845 actionable tasks, with 137 executed, 6 restored from
  cache and 702 up-to-date. The XML audit remains 1,666 unit scenarios across 109 classes and 1,874
  integration scenarios across 209 classes, with zero failures, errors or skips.
- Replaced process 67977 with the verified build. Process 83321 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 219 scripts, database schema 6 and Desktop client support.
  Codex opened no RuneLite client and left the independently running application outside the
  workspace untouched.

## 2026-09-12 — Hunter Skillcape boost and teleports

- Pinned Hunter cape revision 14843441 and the final pre-cutoff Guildmaster Apatura transcript at
  revision 14689838. Cache 233.4 corrected the equipped operation contract to four direct routes:
  `Boost`, `Hunter Guild`, `Carnivorous Chinchompas` and `Black Chinchompas`; inventory operation
  three opens the `Hunter Areas` list.
- Added both cape variants' visible +1 boost, shared five-use UTC-daily chinchompa quota, exact
  Feldip/Wilderness/Hunter Guild coordinates, members/Tele Block/deep-Wilderness gates, permanent
  Apatura enhancement and unlimited guild teleport. State uses permanent cache varp 1312 while
  preserving unrelated bits.
- Added eight scenarios covering cache identities and hooks, boost cap, menu/unlock state, shared
  quota and rollover, exact four-tick destination movement, unlimited guild travel, atomic access
  failures, state isolation and both cape variants through the real enhancement dialogue. Together
  with the prior fourteen expert scenarios, the complete module integration suite passes.
- The manifest now tracks 452 unique groups: 48 discovered, 119 researched, 2 unit-tested, 282
  integration-tested and 1 verified. Exact Wilderness warning UI, production varp-1312 field
  allocation, locked direct-guild feedback, full non-cape Apatura progression and matching-client
  audiovisual/packet behavior remain isolated in manual case 106. No RuneLite client was opened.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m34s: 845 actionable tasks, with 138 executed, 6 restored from
  cache and 701 up-to-date. The XML audit contains 1,666 unit scenarios across 109 classes and
  1,882 integration scenarios across 210 classes, with zero failures, errors or skips.
- Replaced process 83321 with the verified build. Process 10970 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 219 scripts, database schema 6 and Desktop client support.
  Codex opened no RuneLite client and left the independently running application outside the
  workspace untouched.

## 2026-09-12 — Automatic ordinary Skillcape trimming core

- Pinned Cape of Accomplishment revision 14994254 and all 23 cache-233.4 ordinary
  untrimmed-to-trimmed Skillcape pairs. The source explicitly ties irreversible trimming to the
  second level 99 and distinguishes carried/banked/POH capes, later ordinary pickup, Telekinetic
  Grab, death storage, Forestry clothes pouch and Huntsman's kit.
- Added a dedicated shared Skillcape plugin on rsmod's protected advance-stat event. An actual
  second level-99 transition transforms every mapped cape in inventory, worn equipment and bank in
  its original slot, preserves item variables and rebuilds appearance when the worn cape changes.
- Added four integration scenarios covering all 23 cache pairs, every inventory family, Hunter
  capes across the three connected stores and the first-99/sub-99 no-op boundaries. The focused
  Skillcape integration suite passes.
- POH storage, ordinary pickup versus Telekinetic Grab, death storage, Forestry clothes-pouch and
  Huntsman's-kit integrations remain isolated in manual case 107. No RuneLite client was opened.
- The manifest now tracks 453 unique groups: 48 discovered, 119 researched, 2 unit-tested, 283
  integration-tested and 1 verified. The XML audit contains 1,666 unit scenarios across 109
  classes and 1,886 integration scenarios across 211 classes, with zero failures, errors or skips.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 7m15s: 851 actionable tasks, with 477 executed, 7 restored from
  cache and 367 up-to-date.
- Replaced process 10970 with the verified build. Process 27973 is listening on port 43594 with
  cache revision 233, 56 plugin modules, 220 scripts, database schema 6 and Desktop client support.
  Codex opened no RuneLite client and left the independently running application outside the
  workspace untouched.

### Ordinary pickup and Telekinetic Grab distinction

- Added an ordered, injectable object-identity transform to the ordinary ground `Take` plugin and
  bound the Skillcape policy through a dedicated module. A player with at least two mastered
  production skills now receives the trimmed identity on normal pickup; the ground object is
  removed only after the transformed inventory transaction can commit.
- Kept Telekinetic Grab's independent acquisition path unchanged, preserving the original
  untrimmed identity. Two Skillcape interaction scenarios cover ordinary pickup before and after
  second mastery, and a real spell-utility scenario covers the Telekinetic Grab exception. Both
  focused integration suites pass.
- POH, death-storage, Forestry clothes-pouch and Huntsman's-kit integrations remain manual case
  107. No RuneLite client was opened.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in a single-worker run in 6m22s: 851 actionable tasks, with 66 executed, 6 restored from
  cache and 779 up-to-date. The XML audit contains 1,666 unit scenarios across 109 classes and
  1,889 integration scenarios across 211 classes, with zero failures, errors or skips.
- Replaced process 27973 with the verified build. Process 37335 is listening on port 43594 with
  cache revision 233, 57 plugin modules, 220 scripts, database schema 6 and Desktop client support.
  Codex opened no RuneLite client and left the independently running application outside the
  workspace untouched.

## 2026-09-12 — Ordinary Skillcape visible-boost core

- Pinned the historical Cape of Accomplishment revision 14994254 contract: equip and worn Boost
  apply a visible +1 up to 100, repeat activation waits for return to 99, and Hitpoints/Prayer have
  independent one-minute reuse delays under the ordinary visible-stat restoration lifecycle.
- Unified all 23 production stat/object mappings in the shared Skillcape definition. Successful
  equipment transactions for all 46 ordinary cape identities now activate the corresponding
  effect; object-specific worn handlers cover all 42 cache-exposed non-Hunter cape identities and
  the existing Hunter plugin retains its two cache-specialized handlers.
- Preserved cache 233.4's sole metadata exception: Agility cape objects 9771/9772 have blank worn
  operation-one strings, so they receive the sourced equip effect without a fabricated menu route.
- Added independent permanent server-only ready-at fields 65495/65496 for Hitpoints and Prayer,
  with exact second resolution and a 2025 epoch offset. Only a successful stat change starts the
  cooldown, so blocked and already-capped attempts cannot extend it.
- Added five integration scenarios covering every cache operation and state field, all 42 shared
  worn handlers, all 46 real equipment transactions, cap/reactivation behavior and the independent
  59/60-second boundaries. The focused Skillcape suite passes all 10 scenarios. Exact client decay
  phase, cooldown feedback, relog presentation and the Agility source/cache discrepancy remain
  manual case 108. No RuneLite client was opened.
- The manifest now tracks 454 unique groups: 48 discovered, 119 researched, 2 unit-tested, 284
  integration-tested and 1 verified. The repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in a single-worker run in 6m36s:
  851 actionable tasks, with 138 executed, 6 restored from cache and 707 up-to-date. The XML audit
  contains 1,666 unit scenarios across 109 classes and 1,893 integration scenarios across 211
  classes, with zero failures, errors or skips.
- Replaced process 37335 with the verified build. Process 53896 is listening on port 43594 with
  cache revision 233, 57 plugin modules, 220 scripts, database schema 6 and Desktop client support.
  Codex opened no RuneLite client.

## 2026-09-12 — Ordinary Skillcape emote core

- Pinned Cape of Accomplishment revision 14994254 for the worn-cape and combat restrictions, then
  pinned cache-233.4's complete ordinary set: 46 cape objects, two variants for each of the 23
  released stats, and the stat-to-animation/spot-animation pairs in enums 65520 and 65521.
- Corrected the shared emote resolver's completeness check so it validates both animation maps
  instead of checking the animation map twice. The existing interface route now rejects an
  inventory-only cape and recent combat while selecting the exact cache animation and graphic for
  every worn ordinary cape identity.
- Added five focused scenarios in total. The cache contract test requires the exact 46-object set
  and exactly two variants for every released stat. Runtime tests send the real Skill Cape emote
  button input for all 46 capes and synchronously capture the emitted sequence and spot animation,
  while separately pinning the unworn and combat rejection paths.
- The historical Fishing Trawler restriction is not guessed into an area check because this
  workspace has no authoritative implemented aboard-state contract. It remains isolated with
  packet, sound, height, timing and Music-cape-exception verification in manual case 109. No
  RuneLite client was opened.
- The manifest now tracks 455 unique groups: 48 discovered, 119 researched, 2 unit-tested, 285
  integration-tested and 1 verified. The repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in a single-worker run in 6m33s:
  851 actionable tasks, with 62 executed, 6 restored from cache and 783 up-to-date. The XML audit
  contains 1,666 unit scenarios across 109 classes and 1,896 integration scenarios across 211
  classes, with zero failures, errors or skips.
- Replaced process 53896 with the verified build. Process 71555 is listening on port 43594 with
  cache revision 233, 57 plugin modules, 220 scripts, database schema 6 and Desktop client support.
  Codex opened no RuneLite client.

## 2026-09-12 — Smithing Skillcape gold-XP perk certification

- Pinned the final pre-cutoff Smithing cape revision 14873606 alongside the existing cutoff
  goldsmith-gauntlets revision 14965070 and Superheat Item revision 14981805. Together they require
  both Smithing cape variants to grant 56.2 rather than 22.5 Smithing XP per gold bar while worn,
  including spell-smelted gold.
- Audited the two existing runtime consumers. Ordinary furnace smelting and Superheat Item already
  recognize goldsmith gauntlets, Smithing cape 9795, Smithing cape(t) 9796 and the worn max cape in
  their correct equipment slots, so no production-code change was necessary.
- Expanded both focused integration scenarios to iterate all four qualifying equipment identities.
  The ordinary-furnace and spell-utility suites pass, certifying both cape variants instead of the
  previous one-cape/gauntlets-only coverage split.
- Kept the distinct 27-to-36 coal-bag capacity perk out of this certification: the server can
  consume pre-populated bag state but has no production Fill/Open/Close/Check/Empty/Destroy or
  automatic-mining-fill plugin, and the Blast Furnace activity is absent. Those boundaries remain
  explicit in manual case 110. No RuneLite client was opened.
- The manifest now tracks 456 unique groups: 48 discovered, 119 researched, 2 unit-tested, 286
  integration-tested and 1 verified. After the focused suites rebuilt the changed tests, the full
  repository `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 4s:
  851 actionable tasks, with 4 executed, 6 restored from cache and 841 up-to-date. The XML audit
  remains 1,666 unit scenarios across 109 classes and 1,896 integration scenarios across 211
  classes, with zero failures, errors or skips.
- This slice changed tests and evidence only, so the verified production classes loaded by process
  71555 did not change and no redundant server restart was performed. Process 71555 remains the
  listener on port 43594.

## 2026-09-12 — Coal bag and Smithing Skillcape capacity core

- Pinned final pre-cutoff Coal bag revision 14962565, Smithing cape revision 14873606 and Max cape
  revision 14993265. Together they establish closed/open objects 12019/24480, five inventory
  operations, bank Fill/Empty, 27 ordinary or 36 equipped-Smithing-cape capacity, open-bag mined
  coal insertion, loose-coal-first processing, empty-only destruction and inherited max-cape perks.
- Added a dedicated rsmod plugin using the repository's held, held-use and bankside operation
  patterns. Its permanent six-bit state supports transactional inventory Fill/Empty, Open/Close,
  Check, bank Fill/Empty with placeholders, members gating and cache-native confirmed Destroy for
  empty bags. Removing a qualifying cape never truncates 28–36 stored coal but blocks further
  insertion until the count is below the ordinary cap.
- Integrated the storage service into ordinary Mining. An open bag with space can receive mined
  coal even when every inventory slot is occupied, while closed/full/F2P/wrong-product paths retain
  the existing full-inventory behavior. The same state remains consumed loose-coal-first by furnace
  smelting and Superheat Item.
- Added six isolated coal-bag integration scenarios plus one actual coal-rock scenario. They pin
  the exact cache object operations, varbit bits 25–30, both capacities, all three qualifying worn
  cape identities, partial transfers, bankside round trips, membership and both destruction choices.
  Exact client feedback/presentation, deposit boxes, acquisition/one-bag ownership, alchemy and the
  absent Blast Furnace remain manual case 110. No RuneLite client was opened.
- The manifest now tracks 457 unique groups: 48 discovered, 119 researched, 2 unit-tested, 287
  integration-tested and 1 verified. The repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in a single-worker run in 7m09s:
  857 actionable tasks, with 144 executed, 6 restored from cache and 707 up-to-date. The XML audit
  contains 1,666 unit scenarios across 109 classes and 1,903 integration scenarios across 212
  classes, with zero failures, errors or skips.
- Replaced process 71555 with the verified build. Process 7067 is listening on port 43594 with
  cache revision 233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support.
  Codex opened no RuneLite client.

## 2026-09-12 — Coal-bag alchemy state-clearing exception

- The final pre-cutoff Coal bag revision 14962565 explicitly says alchemy destroys a coal bag even
  when it contains coal. Cache 233.4 marks both bag variants with the generic `no_alchemy`
  parameter, so this is a documented object-specific exception rather than ordinary metadata
  behavior.
- Added a narrow Low/High Level Alchemy exception for object IDs 12019 and 24480. Every other
  cache-marked target retains the generic rejection. A successful cast preserves the ordinary rune,
  XP, coin-value, presentation and cooldown transaction, deletes the targeted bag, and clears its
  permanent storage varbit so replacement bags cannot expose stale contents.
- Added one focused integration scenario covering High Level Alchemy on a closed bag with 27 coal
  and Low Level Alchemy on an open bag with 36 coal, including the conflicting cache flags, deletion,
  coin output and state clearing. Exact live-client warning/prompt and packet ordering remain manual
  case 110. No RuneLite client was opened.
- The manifest remains at 457 groups and the status distribution is unchanged. The complete
  `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 6m34s: 857
  actionable tasks, with 66 executed, 6 restored from cache and 785 up-to-date. The XML audit now
  contains 1,666 unit scenarios across 109 classes and 1,904 integration scenarios across 212
  classes, with zero failures, errors or skips.
- Replaced process 7067 with the verified build. Process 15247 is listening on port 43594 with
  cache revision 233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support.
  Codex opened no RuneLite client.

## 2026-09-12 — Woodcutting Guild invisible-boost core

- Pinned final pre-cutoff Woodcutting Guild revision 14975699. It establishes the exact 47-vertex
  guild boundary, a +7 invisible Woodcutting success modifier that stacks with visible boosts but
  cannot unlock higher-level actions, and exclusion of the Forestry group bonus inside the guild.
- Added the `woodcutting_guild` area symbol and rsmod-native map-area builder across planes 0–3.
  The exact dated polygon packs into map groups `a24_54` and `a25_54`; its world coordinates exclude
  the separately aligned underground dungeon. The existing Woodcutting level-modifier plugin now
  contributes seven through the shared invisible-level path only while `AreaChecker` reports guild
  membership.
- Added a focused integration scenario that independently verifies the generated polygon, decodes
  the packed production cache groups, exercises the production area checker at interior, exterior,
  concave-boundary, upper-plane and dungeon coordinates, and pins the +7/zero results. Exact live
  success sampling and client presentation remain manual case 111, and the Forestry group bonus
  outside the guild remains open. No RuneLite client was opened.
- The manifest now tracks 458 unique groups: 48 discovered, 119 researched, 2 unit-tested, 288
  integration-tested and 1 verified. The repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 7m03s: 857 actionable tasks,
  with 139 executed, 6 restored from cache and 712 up-to-date. The XML audit contains 1,666 unit
  scenarios across 109 classes and 1,905 integration scenarios across 213 classes, with zero
  failures, errors or skips.
- Replaced process 15247 with the verified build. Process 72373 is listening on port 43594 with
  cache revision 233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support.
  Codex opened no RuneLite client.

## 2026-09-12 — Forestry group Woodcutting boost core

- Pinned final pre-cutoff Woodcutting revision 14991189 and Jagex's Forestry Part One update. They
  establish an F2P invisible +1 Woodcutting success level for each player chopping the same tree,
  including the subject, capped at +10, with normal-tree, Farming-grown-tree and Woodcutting Guild
  exclusions and no ability to unlock higher-level actions.
- Extended the existing rsmod-native Woodcutting invisible-level plugin. Outside the exact guild
  area it identifies eligible static Forestry-timed trees through the catalogue's `despawn_time`
  parameter, queries nearby live players, and counts only valid players whose current operation
  targets the identical bound tree and whose avatar passes the production collision-aware loc
  operation range. Guild membership takes priority and remains exactly +7.
- Added a focused integration scenario proving F2P solo +1, exclusion of stopped, different-tree,
  dead, hidden, logging-out, out-of-range and wrong-operation players, a hard +10 cap with a larger
  active group, and zero on normal trees. Exact live success sampling, group-icon cadence and client
  presentation remain manual case 111. No RuneLite client was opened.
- The manifest now tracks 459 unique groups: 48 discovered, 119 researched, 2 unit-tested, 289
  integration-tested and 1 verified. The repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 6m55s: 857 actionable tasks,
  with 63 executed, 6 restored from cache and 788 up-to-date. The XML audit contains 1,666 unit
  scenarios across 109 classes and 1,906 integration scenarios across 213 classes, with zero
  failures, errors or skips.
- Replaced process 72373 with the verified build. Process 223 is listening on port 43594 with cache
  revision 233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. Codex
  opened no RuneLite client.

## 2026-09-12 — Regular Woodcutting bird-nest core

- Pinned final pre-cutoff Bird nest revision 14924135, Yew tree revision 14986999 and Woodcutting
  revision 14991189, plus Mod Ash's published 100-entry/95-entry rabbit-foot selection table and
  adjacent-tile west-bias clarification. They establish a members-only 1/256 roll in place of the
  normal log, improved to 1/230 by the Woodcutting cape perk, exact egg/ring/seed boundaries,
  redwood exclusion, owner-private ground placement and a two-minute lifetime.
- Added an rsmod-native Woodcutting bird-nest service using cache 233.4 object identities. A
  successful eligible log roll now checks the regular nest chance, applies worn Woodcutting,
  trimmed Woodcutting or Max cape eligibility, shifts the seed-nest share only for a worn strung
  rabbit foot, places the selected nest on an adjacent west-biased tile, and suppresses the log,
  XP and `CutLogs` event. F2P, redwood and blisterwood paths are rejected before consuming nest
  randomness.
- Added three focused integration scenarios. They exercise a real yew-tree cycle, exact cape and
  necklace equipment scope, every table boundary, deterministic adjacency, private ownership,
  100-tick reveal and 200-tick expiry, plus F2P and redwood exclusions. Clue nests, Twitcher's
  Gloves, the exact live west-bias weights, sound and matching-client presentation remain manual
  case 112. No RuneLite client was opened.
- The manifest now tracks 461 unique groups: 48 discovered, 120 researched, 2 unit-tested, 290
  integration-tested and 1 verified. The repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 6m56s: 857 actionable tasks,
  with 63 executed, 6 restored from cache and 788 up-to-date. The XML audit contains 1,666 unit
  scenarios across 109 classes and 1,909 integration scenarios across 214 classes, with zero
  failures, errors or skips.
- Replaced process 223 with the verified build. Process 29501 is listening on port 43594 with cache
  revision 233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. Codex
  opened no RuneLite client.

## 2026-09-12 — Special Woodcutting axe harvesting and Lumber Up cores

- Pinned the final pre-cutoff dragon axe revision 15000721, 3rd age axe revision 14926278, Infernal
  axe revision 14997416, Crystal axe revision 14963010 and both Trailblazer ornament revisions,
  together with the dated Crystal equipment, elven/celestial signet and Ghommal's lucky penny
  records. They establish all twelve cache identities, members and level/quest access, relative
  rates, 5,000/10,000/20,000 charge boundaries, Infernal one-in-three burning with half Firemaking
  XP and Pyromancer, per-obtained-log Crystal use, independent 1/10 and 1/20 conservation, depleted
  fallback and Lumber Up.
- Extended the native Woodcutting axe editor and every ordinary tree rate enum with base,
  ornamented, Reloaded, charged and depleted Dragon/Infernal/Crystal forms plus the 3rd age axe.
  Selection now observes the real efficiency order, distinguishes iron from bronze at their shared
  level, applies each object's members flag and gates both Crystal states on completed Song of the
  Elves. Reloaded cache sequences 11939/11940 are used directly.
- Added persistent Infernal and Crystal item-state types and a focused axe-effects service. A
  successful eligible Infernal burn suppresses only the log, grants half ordinary Firemaking XP
  through the shared Pyromancer modifier and consumes a charge unless the equipped penny saves it.
  Active Crystal consumes one charge per obtained log unless an equipped elven/celestial signet or
  penny independently saves it. Final charges transform carried or worn tools to the exact depleted
  identity; ordinary Woodcutting XP and its event remain tied to the successful tree roll.
- Registered every depleted and ornamented variant with the existing Lumber Up implementation.
  The expanded registry test invokes all twelve forms and pins 100% energy, +3 Woodcutting, the
  `Chop chop!` phrase and sequence 2876. Five new Woodcutting integration scenarios pin all IDs,
  access, rates, priority, charge/save/depletion boundaries, Infernal XP and a real tree cycle.
- Extending the shared axe group exposed Canoeing's exhaustive enum contract. Added the six missing
  special states to both station and shaping rates and to shaping animations, using cache sequences
  11819/11820 for Reloaded Infernal/dragon styles and the existing family sequences for depleted
  forms. The one-time cache enum repack completed, and the focused Canoeing suite passes. Exact
  special-tree exceptions plus Infernal/Crystal creation, recharge, shard charging, inspection,
  ornament operations, bank configuration and matching-client presentation remain manual case 113.
  No RuneLite client was opened.
- The manifest now tracks 464 unique groups: 48 discovered, 121 researched, 3 unit-tested, 291
  integration-tested and 1 verified. The repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 7m07s: 857 actionable tasks,
  with 100 executed, 6 restored from cache and 751 up-to-date. The XML audit contains 1,667 unit
  scenarios across 109 classes and 1,914 integration scenarios across 215 classes, with zero
  failures, errors or skips.
- Replaced process 29501 with the verified build. Process 69991 is listening on port 43594 with
  cache revision 233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support.
  Codex opened no RuneLite client.

## 2026-09-12 — Special Woodcutting axe inventory lifecycle core

- Completed the evidence-backed inventory lifecycle around the existing Infernal and Crystal axe
  charge states. A dragon axe plus smouldering stone now performs an atomic three-tick Infernal
  conversion gated by members and unboosted level 61 Woodcutting/85 Firemaking, awards 200/350 XP
  and begins with the implicit 5,000 charges. Every base, original Trailblazer and Reloaded depleted
  state accepts either a smouldering stone or dragon axe, restores 5,000 charges and awards the same
  XP without reapplying creation skill gates.
- Added charged/depleted ornament application and atomic Revert, inventory and worn Change-Style,
  shifted ornamented Wield and carried/worn Check handlers. Packed charge variables survive every
  transform. Infernal Check reports the Wiki-established integer percentage; its exact live wording
  and sub-one-percent rounding remain bounded to manual case 113 because the historical page does
  not preserve that transcript.
- Added Crystal shard charging from active or inactive axes through a capped, stale-input-safe
  quantity dialogue at 100 charges per shard and 20,000 maximum. Carried/worn Check reports exact
  stored charges. Revert atomically returns dragon axe 6739 and crystal tool seed 23953 from either
  state and refuses to destroy charges when the extra output cannot fit.
- Six new integration scenarios pin all cache inventory/worn operation arrays and use-pair dispatch,
  delayed/base-level creation, all recharge styles/materials/orders and XP, ornaments and style
  changes, charge inspection, shard boundaries and active/inactive/full-inventory Revert. The
  focused test and the full 23-scenario Woodcutting integration suite pass. Crystal singing-bowl/NPC
  creation, bank Configure-Charges, special-activity exclusions, exact audiovisual/packet behavior
  and death/trade handling remain researched.
- The manifest now tracks 465 unique groups: 48 discovered, 121 researched, 3 unit-tested, 292
  integration-tested and 1 verified. The serialized repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 6m46s: 857 actionable tasks,
  with 65 executed, 6 restored from cache and 786 up-to-date. The XML audit contains 1,667 unit
  scenarios across 109 classes and 1,920 integration scenarios across 216 classes, with zero
  failures, errors or skips.
- Replaced process 69991 with the verified build. Process 86868 is listening on port 43594 with
  cache revision 233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support.
  Codex opened no RuneLite client.

## 2026-09-12 — Crystal axe singing recipe core

- Added the crystal-axe recipe to the cache-233 Prifddinas singing bowl and the learned Reese and
  Conwenna forms. Cache inspection pins bowl 36552 `Sing-crystal` in operation one, NPCs 9134/9136
  `Sing` in operation three and player singing sequence 8459. Both paths use rsmod's native
  skill-multi product chooser; the complete dedicated multi-product interface remains a bounded gap.
- Self-singing requires members, Song of the Elves state 194 and visible, boostable level 76
  Crafting and Smithing. It consumes a dragon axe, crystal tool seed and 120 shards per result after
  three ticks, converts every complete set in the inventory atomically, awards 6,000 XP in each
  skill per axe and packs the sourced 10,000 initial charges. The learned-singer route uses 180
  shards, ignores the two skill levels and awards no player XP.
- Four integration scenarios extend `WoodcuttingAxeLifecycleTest` with exact cache IDs, operation
  arrays and dispatch, the native chooser packets, boosted two-axe batching, XP and charge state,
  membership/quest/skill/material/stale-action preservation, and the assisted premium. The focused
  ten-scenario lifecycle class and full 27-scenario Woodcutting integration suite pass.
- The manifest now tracks 466 unique groups: 48 discovered, 121 researched, 3 unit-tested, 293
  integration-tested and 1 verified.
- The full `spotlessCheck validateCache233Parity test integration` repository gate passed in 6m 56s:
  857 actionable tasks (140 executed, 6 from cache and 711 up-to-date). Independent JUnit XML
  aggregation records 1,667 unit tests across 109 classes and 1,924 integration tests across 216
  classes, with zero failures, errors or skips in either suite.
- Restarted the verified server as process 9713. It is listening on port 43594 with cache revision
  233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. Codex opened no
  RuneLite client.

## 2026-09-12 — Bank Configure-Charges routing and Crystal axe preference core

- Matched the cache-233 bank item-menu contract used by the workspace RuneLite source: withdrawal
  actions remain operations one through eight, `Configure-Charges` is operation nine when bank item
  options are enabled, Placeholder is operation ten and Examine is operation eleven. The shared
  IF3 input handler now accepts the complete operation range already defined by rsmod instead of
  rejecting operations above ten.
- Added the cache-backed bank item-options toggle, component and `bank_autocharge` parameter alias.
  Configure-Charges validates the current bank slot and object parameter before publishing a typed
  protected event, so chargeable-item plugins can own their exact resource rules without coupling
  those rules to the bank interface.
- Registered active and inactive Crystal axes with that event. Their dialogue stores a permanent,
  capped 0–20,000 desired-withdrawal quantity in cache varbit 16148; zero disables the preference,
  and non-members receive the normal members-object rejection. Actual shard/material deduction,
  automatic refilling during withdrawal and the exact live dialogue presentation remain bounded to
  manual case 113 pending authoritative per-item evidence.
- Three focused integration scenarios pin bank component 12:61, varbit 16125, operation routing,
  DB row 4338, parameter 2257 and permanent varbit 16148 over base varp 4681 bits 0–15. They cover
  toggle state, disabled and enabled routing, Placeholder/Examine continuity, capped preference
  storage and non-member preservation. Both focused suites and the full repository gate pass. No
  RuneLite client was opened.
- The manifest now tracks 468 unique groups: 48 discovered, 121 researched, 3 unit-tested, 295
  integration-tested and 1 verified. The serialized repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 7m17s: 857 actionable tasks,
  with 144 executed, 6 restored from cache and 707 up-to-date. The XML audit contains 1,667 unit
  scenarios across 109 classes and 1,927 integration scenarios across 216 classes, with zero
  failures, errors or skips.
- Restarted the verified server as process 60002. It is listening on port 43594 with cache revision
  233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. Codex opened no
  RuneLite client.

## 2026-09-12 — Woodcutting clue-nest core

- Pinned final pre-cutoff Clue nest revision 14986594. It supplies the complete integer formula,
  all five tier modifiers, every documented tree-family base chance, independent per-tier rolls,
  the corresponding-tier 5% Combat Achievement reduction, Wilderness imbued-ring doubling,
  same-tier ownership suppression, Woodcutting-cape non-effect and Arthur's vessel-clue opt-out.
- Added cache-backed beginner, easy, medium, hard and elite clue nests; all six charged/uncharged
  imbued rings of wealth; permanent opt-out varbit 9365; and Combat Achievement status varbits
  12863–12866. Every currently supported tree product family now carries custom parameter 65525
  with its sourced base chance. The one-time cache addition was packed before running tests.
- Successful cuts now roll every eligible clue tier independently before the separate regular-nest
  replacement. Easy through elite require a members world; a cache-identifiable same-tier clue
  nest, scroll, bottle or geode in inventory or bank skips only that tier. Accepted clues use the
  standard owner-private adjacent floor lifecycle and red message while preserving the log,
  Woodcutting XP, axe effects and `CutLogs` event.
- Four focused integration scenarios pin cache identities and variable layouts, every configured
  family chance, formula rounding and modifiers, membership/ownership/opt-out randomness, private
  multi-tier drops and a real free-to-play successful cut. The complete 32-scenario Woodcutting
  integration suite passes. Sulliuscep activity integration, Twitcher's-glove preference state,
  non-object clue state, exact tile weighting/sound/packet order and the post-X-Marks replacement
  remained manual case 112 at this checkpoint. The cache-absence conclusion recorded here was
  later disproved by semantic object, quest-row and client-script inspection; the corrective
  implementation and evidence are recorded in the later shared-clue entry.
- The manifest now tracks 469 unique groups: 48 discovered, 121 researched, 3 unit-tested, 296
  integration-tested and 1 verified. The serialized repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 2m24s: 857 actionable tasks,
  with 138 executed, 6 restored from cache and 713 up-to-date. The XML audit contains 1,667 unit
  scenarios across 109 classes and 1,931 integration scenarios across 217 classes, with zero
  failures, errors or skips.
- Restarted the verified server as process 93136. It is listening on port 43594 with cache revision
  233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. Codex opened no
  RuneLite client.

## 2026-09-12 — Bryophyta's staff bank preference core

- Registered charged Bryophyta's staff 22370 for the shared protected Configure-Charges bank
  event. The plugin stores count-dialog input in permanent cache varbit 16139 over varp 4677 bits
  20–30, clamped to the staff's zero-to-1,000 capacity; zero disables the desired quantity.
- Extended the cache-contract test to pin BANK_AUTOCHARGE param 2257 to DB row 4340 and event
  registration. A new integration scenario verifies upper-bound clamping and zero storage. The
  focused class and complete Bryophyta's-staff module suite pass.
- Split the previous broad bank row into an integration-tested preference core and a researched
  automatic-withdrawal group. Nature-rune deduction, item-local charge mutation on withdrawal,
  exact configuration presentation and matching-client packet order remain manual case 81.
- The manifest now tracks 470 unique groups: 48 discovered, 121 researched, 3 unit-tested, 297
  integration-tested and 1 verified. The full repository `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 2m21s: 857 actionable tasks,
  with 63 executed, 6 restored from cache and 788 up-to-date. Independent XML aggregation records
  1,667 unit scenarios across 109 classes and 1,932 integration scenarios across 217 classes, with
  zero failures, errors or skips.
- Restarted the verified server as process 3141. It is listening on port 43594 with cache revision
  233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. Codex opened no
  RuneLite client.

## 2026-09-12 — Crystal harpoon bank preference core

- Registered active and inactive Crystal harpoons for the shared protected Configure-Charges bank
  event. The members-only Fishing lifecycle stores count input in permanent crystal-tools varbit
  16148, clamped to zero through 20,000; zero disables the desired withdrawal quantity.
- Extended the Fishing cache-contract scenario to pin active object 23762's BANK_AUTOCHARGE param
  2257 to DB row 4338, varbit 16148 over permanent varp 4681 bits 0–15 and both event routes. A
  focused behavior scenario covers upper-bound storage and non-member preservation; both focused
  tests and the complete Fishing integration suite pass.
- Automatic shard deduction, item-local charge mutation during withdrawal, exact dialogue and
  packet order remain bounded to manual case 71 rather than being implied by this preference core.

## 2026-09-12 — Crystal pickaxe bank preference core

- Added a focused Mining preference service and registered active/inactive Crystal pickaxes for
  the shared protected Configure-Charges event. Members-world count input is stored in permanent
  crystal-tools varbit 16148 and clamped to zero through 20,000.
- Two integration scenarios pin active object 23680's BANK_AUTOCHARGE param 2257 to DB row 4338,
  permanent varp 4681 bits 0–15, both event registrations, capped storage and free-world
  preservation. The focused class and complete Mining integration suite pass.
- Item-local Crystal charge state, manual shard loading, Revert, automatic withdrawal charging,
  exact dialogue and packet order remain bounded to manual case 97.
- The manifest now tracks 472 unique groups: 48 discovered, 121 researched, 3 unit-tested, 299
  integration-tested and 1 verified. The final repository-wide `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate passed in 2m21s: 857 actionable tasks,
  with 66 executed, 6 restored from cache and 785 up-to-date. Independent XML aggregation records
  1,667 unit scenarios across 109 classes and 1,935 integration scenarios across 218 classes, with
  zero failures, errors or skips.
- Restarted the verified server as process 17535. It is listening on port 43594 with cache revision
  233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. Codex opened no
  RuneLite client.

## 2026-09-12 — Lumber Up and Rock Knocker production-startup integration

- Extracted the special-attack cache-loading and installed-map registration sequence into a shared
  bootstrap service. The existing internal plugin script delegates to that service unchanged, so
  production startup and integration tests now exercise one initialization path.
- Added a three-scenario integration suite around the production bootstrap. It registers and
  activates all twelve cache-233 Lumber Up axe states and all fourteen Rock Knocker pickaxe states,
  pinning each 1,000-unit requirement, +3 boost without lowering a stronger boost, overhead phrase,
  animation family and full-energy deduction contract. Lumber Up additionally pins the red/silver
  graphic identity, height 96 and combat spot slot.
- Promoted both manifest groups from unit-tested to integration-tested without widening the bounded
  matching-client gaps in manual cases 97 and 113. The manifest remains at 472 groups: 48
  discovered, 121 researched, 1 unit-tested, 301 integration-tested and 1 verified.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 2m23s: 859 actionable tasks, with 71 executed, 6 restored from cache and 782 up-to-date.
  After removing one stale report from the abandoned content-only test location, independent XML
  aggregation records 1,667 unit scenarios across 109 classes and 1,938 integration scenarios
  across 219 classes, with zero failures, errors or skips.
- Restarted the verified server as process 37322. It is listening on port 43594 with cache revision
  233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. Codex opened no
  RuneLite client.

## 2026-09-12 — Standard elemental scaling production-path integration

- Extended the deterministic game-test random source with observation of the inclusive bounds from
  its most recent integer roll. This leaves roll behavior unchanged while allowing integration
  tests to verify the range requested by production combat code.
- Added a 35-cast integration scenario that registers the production elemental spell handlers,
  drives a representative Strike, Bolt, Blast, Wave and Surge through every lower and upper unlock
  boundary, and asserts the exact live damage range derived from the player's current Magic level.
  The existing five boundary-table unit scenarios remain as a fast second layer.
- Promoted the final unit-only manifest group to integration-tested. The manifest remains at 472
  groups: 48 discovered, 121 researched, 302 integration-tested and 1 verified, with no groups left
  at unit-tested.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 2m33s: 859 actionable tasks, with 128 executed, 6 restored from cache and 725 up-to-date.
  Independent XML aggregation records 1,667 unit scenarios across 109 classes and 1,939 integration
  scenarios across 219 classes, with zero failures, errors or skips.
- Restarted the verified server as process 45522. It is listening on port 43594 with cache revision
  233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. Then launched
  only the workspace-built RuneLite client through `runelite/run-rsmod.sh`; it loaded the server's
  RSA public key and reached the local revision-233 login screen using its isolated
  `.runelite-rsmod` profile. Local mode is now opt-in: the launcher explicitly sets the rsmod flag
  and sibling-server directory, while an ordinary checkout launch retains normal RuneLite behavior.
  The development server ignores passwords, but this client still requires any non-empty disposable
  value before it sends a login request. macOS denied automated keystrokes, so no real credential was
  read or injected. The installed RuneLite application was not opened.

## 2026-09-12 — Deterministic future-cache delta engine

- Added the independent `api/cache-delta` application module. It accepts versioned cache-inventory
  manifests, rejects unsupported schemas, incomplete revision provenance, blank identities and
  fingerprints and duplicate domain/key pairs, then canonicalizes source order, attributes and
  explicit risk tags before comparison.
- The report has a versioned schema, exact baseline/target references, added/removed/changed and
  unchanged counts, stable domain/key ordering and migration tags for typed-symbol, morph,
  map/collision, client-contract and protocol-sensitive changes. Exporter-provided risk tags are
  retained.
- Four unit scenarios prove byte-stable output across shuffled input, exact summary/order/risk
  classification, normalization and invalid-manifest rejection. A command-line fixture smoke test
  produced the expected one-added, zero-removed, one-changed and one-unchanged report.
- Registered one new unit-tested tooling group, taking the manifest to 473 groups: 48 discovered,
  121 researched, 1 unit-tested, 302 integration-tested and 1 verified. Domain-complete cache
  exporters and automatic feature-backlog generation remain explicit gaps; this is the deterministic
  comparison layer, not a claim that the future-cache upgrade architecture is complete.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 3m08s: 866 actionable tasks, with 486 executed, 7 restored from cache and 373 up-to-date.
  Independent XML aggregation records 1,671 unit scenarios across 110 classes and 1,939 integration
  scenarios across 219 classes, with zero failures, errors or skips.
- A post-gate server restart exposed Gradle's unqualified task selection: adding a second application
  had made `./gradlew run` start both the server and the argument-less delta CLI. The delta `run`
  task now skips when no paths are supplied, preserving the root server launcher while explicit
  `:api:cache-delta:run --args='...'` invocations retain strict three-path validation.
- Re-ran the focused module tests, formatting and manifest validator after that wiring correction,
  then exercised the standard root launcher successfully. The verified server is running as process
  71885 on port 43594 with revision 233, 58 plugin modules, 221 scripts, database schema 6 and
  Desktop client support. The existing workspace-built RuneLite process remains open; the installed
  RuneLite application was not opened.

## 2026-09-12 — Core future-cache config exporter

- Extended `api/cache-delta` with a real-cache exporter for all object, NPC and location
  definitions. It opens an independently supplied cache and revision-matched `obj.sym`, `npc.sym`
  and `loc.sym`, fingerprints the exact raw bytes of every definition and emits decoded operations,
  equipment/state/parameter, combat/movement/morph, collision/size/sound and migration-risk
  metadata in canonical order.
- Centralized manifest normalization so both exports and comparisons reject invalid metadata,
  identities, fingerprints and duplicates and produce byte-stable JSON. The original three-path
  diff command remains compatible; `export-core` is an explicit subcommand.
- Added two unit scenarios for behavior-rich object export and byte stability, plus an integration
  scenario that opens the real cache-233.4 archive and pins its complete 31,172-object,
  14,799-NPC and 57,724-location inventory with representative symbol, operation, fingerprint and
  risk assertions.
- Generated `.data/audit/cache-233.4-core-manifest.json`: 103,695 entries with SHA-256
  `baf6400cac0b057c6050b102347f22c55dc1b61499c9ed7fe5171df067ba88f5`. A production self-diff
  reports zero added, removed or changed and 103,695 unchanged. The symbol audit records 22 unnamed
  locations, six unnamed NPCs, no unnamed objects and stale object symbol ID 65534 without
  inventing identities.
- Registered one integration-tested tooling group, taking the manifest to 474 groups: 48
  discovered, 121 researched, 1 unit-tested, 303 integration-tested and 1 verified. Remaining cache
  domains and validated atomic backlog generation remain explicit future-architecture gaps.
- The focused formatting, unit and real-cache integration gate passed in 18 seconds: 359 actionable
  tasks, with 7 executed, 6 restored from cache and 346 up-to-date.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 2m49s: 868 actionable tasks, with 65 executed, 6 restored from cache and 797 up-to-date.
  Independent XML aggregation records 1,673 unit scenarios across 111 classes and 1,940 integration
  scenarios across 220 classes, with zero failures, errors or skips.
- Restarted the verified server as process 88538. It is listening on port 43594 with cache revision
  233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. The existing
  workspace-built RuneLite process remains open; the installed RuneLite application was not opened.

## 2026-09-12 — Semantic future-cache config inventory

- Added `export-configs`, which composes the existing core exporter with every cached varp, varbit,
  inventory, parameter, enum, struct, database table/row, sequence and spot animation. Each entry
  retains the exact raw bytes as a SHA-256 fingerprint and exposes canonical decoded state, data,
  animation, graphics and audio attributes plus migration-risk tags.
- Added revision-matched symbol loading for all ten new domains. Cache definitions remain
  authoritative: missing symbols are retained as null, while symbols with no vanilla definition do
  not manufacture cache entries. Enum records without key/value literals are retained as explicit
  opaque records rather than being silently skipped.
- Added unit coverage for player-variable/bitfield contracts and canonical nested map/set/array
  rendering. A real-cache integration scenario pins all 164,109 definitions across 13 domains,
  unique identities, representative run-option, worn-inventory and teleport-sequence contracts and
  the opaque enum sentinel path.
- Generated the 111-MiB `.data/audit/cache-233.4-semantic-config-manifest.json` with SHA-256
  `e9d21f95c792142ae42a12bdb9b3966c3256d323152227481d209cba45329aa4`. A second independent
  export is byte-identical, and an end-to-end self-diff reports zero added, removed or changed and
  164,109 unchanged.
- Registered one integration-tested tooling group, taking the manifest to 475 groups: 48
  discovered, 121 researched, 1 unit-tested, 304 integration-tested and 1 verified. Maps,
  interfaces/components, client scripts, remaining variable namespaces, projectiles, raw animation
  frames, audio/music, protocol metadata and automatic backlog generation remain explicit gaps.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 2m53s: 868 actionable tasks, with 67 executed, 6 restored from cache and 795 up-to-date.
  Independent XML aggregation records 1,675 unit scenarios across 112 classes and 1,941 integration
  scenarios across 221 classes, with zero failures, errors or skips.
- Restarted the verified server as process 4005. It is listening on port 43594 with cache revision
  233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. The existing
  workspace-built RuneLite process remains open against the local environment; the installed
  RuneLite application was not opened.

## 2026-09-12 — Deterministic atomic cache-upgrade backlog

- Added a versioned machine-readable backlog schema and `plan-upgrade` production command. It
  validates the generated delta, emits exactly one task per changed cache identity and retains both
  fingerprints, symbols, precise changed fields and normalized risk tags.
- Task IDs use the readable domain plus a stable SHA-256-derived suffix and do not depend on list
  position. Critical review covers map/collision, client-contract and protocol changes; all other
  risk-bearing or non-additive changes are high; risk-free additions are normal.
- Every task carries conditional baseline/target cache evidence, revision-matched symbols, dated
  Wiki/update evidence, one review requirement per risk, an implementation-or-evidence-backed no-op
  decision, cache-233.4 regression coverage, target-contract coverage, a manifest update and the
  full unit/integration gate.
- Added unit scenarios for stable task identity/order, summary, priority, exact changed fields,
  evidence/acceptance requirements and inconsistent-report rejection. A file-based integration
  scenario invokes the production command twice and proves byte-identical output.
- Registered one integration-tested tooling group, taking the manifest to 476 groups: 48
  discovered, 121 researched, 1 unit-tested, 305 integration-tested and 1 verified. Remaining cache
  domain exporters and archive acquisition/identity verification remain explicit future-architecture
  gaps.
- The focused formatting, unit, production-command integration and manifest-validation gate passed
  in 22 seconds: 361 actionable tasks, with 16 executed, 6 restored from cache and 339 up-to-date.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 2m52s: 868 actionable tasks, with 72 executed, 6 restored from cache and 790 up-to-date.
  Independent XML aggregation records 1,677 unit scenarios across 113 classes and 1,942 integration
  scenarios across 222 classes, with zero failures, errors or skips.
- Restarted the verified server as process 13177. It is listening on port 43594 with cache revision
  233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. The existing
  workspace-built RuneLite process remains open against the local environment; the installed
  RuneLite application was not opened.

## 2026-09-12 — Vanilla projectile-config boundary

- Extended the semantic exporter and symbol loader for optional JS5 `PROJANIM` definitions. A
  present record retains its exact raw fingerprint plus all seven start/end height, delay, angle,
  length-adjustment, progress and step-multiplier fields with explicit client/timing risks.
- The real cache-233.4 export proved that archive 2317 contains no `PROJANIM` config group, while the
  revision-matched symbol file contains 16 identities backed by rsmod's `ProjAnimBuilds`. The export
  correctly emits zero projectile cache records and remains byte-identical to the prior 164,109-entry
  artifact and its existing SHA-256; no symbol was promoted into a fabricated cache definition.
- Added a seven-field unit contract and real-cache integration assertions for the absent group plus
  all 16 server-enriched symbols. A separate build-layer profile exporter remains an explicit gap.
- The focused formatting, unit, real-cache integration and manifest-validation gate passed in 23
  seconds: 360 actionable tasks, with 12 executed, 6 restored from cache and 342 up-to-date.
- The repository-wide `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 2m48s: 868 actionable tasks, with 65 executed, 6 restored from cache and 797 up-to-date.
  Independent XML aggregation records 1,678 unit scenarios across 113 classes and 1,942 integration
  scenarios across 222 classes, with zero failures, errors or skips.
- Restarted the verified server as process 20553. It is listening on port 43594 with cache revision
  233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. The existing
  workspace-built RuneLite process remains open against the local environment; the installed
  RuneLite application was not opened.

## 2026-09-12 — RSMod-enriched projectile profile inventory

- Added `export-projectiles`, a deliberately separate production command for projectile definitions
  written into rsmod's enriched runtime cache. Its manifest uses the explicit
  `233.4-rsmod-enriched` revision label while preserving upstream archive 2317 provenance.
- The exporter fingerprints the exact raw bytes and decodes all seven trajectory/timing fields for
  all 16 revision-matched symbols. Integration coverage pins the complete count, unique symbols,
  representative arrow contract, final Tumeken's Shadow identity and every entry's risk/fingerprint
  shape.
- Generated `.data/audit/cache-233.4-rsmod-projectile-profiles.json`: 16 entries, 7,563 bytes and
  SHA-256 `3ef92868bd34edeaaf3bc14514ea0c7e4d4ceda6fc7bd0901d6fab8b797d5def`. A second independent
  export is byte-identical; the production self-diff reports zero changes and 16 unchanged.
- Registered one integration-tested tooling group, taking the manifest to 477 groups: 48
  discovered, 121 researched, 1 unit-tested, 306 integration-tested and 1 verified.
- The focused formatting, unit, enriched-cache integration and manifest-validation gate passed in
  24 seconds: 361 actionable tasks, with 16 executed, 6 restored from cache and 339 up-to-date.
- The full repository gate (`spotlessCheck`, `validateCache233Parity`, `test`, and `integration`)
  passed in 2 minutes 49 seconds: 868 actionable tasks, with 65 executed, 6 restored from cache and
  797 up-to-date. The generated reports contain 1,678 unit tests across 113 classes and 1,943
  integration tests across 223 classes, with zero failures, errors or skips.
- Restarted the verified server as process 27276. It is listening on port 43594 with cache revision
  233, 58 plugin modules, 221 scripts, database schema 6 and Desktop client support. The existing
  workspace-built RuneLite process remains open against the local environment; the installed
  RuneLite application was not opened. Its expected upstream price/world-list warnings remain
  non-fatal.

## 2026-09-12 — Raw client-contract archive inventory

- Added `export-client-contracts`, a deterministic raw-file inventory for cache formats that do not
  yet have semantic decoders. It covers animation frames/bases, interface components, client
  scripts, keyframe sets, sound effects, songs, jingles, Vorbis samples and MIDI patches.
- Each entry has a stable domain plus group/file key, exact unpacked-file SHA-256, JS5 archive,
  version, checksum, length and name-hash metadata, and explicit client/audio/animation/interface or
  script risk tags. Documentation explicitly bounds this to byte-level change detection rather than
  semantic or gameplay parity.
- The production baseline contains 225,204 records: 2,464 animation bases, 175,868 animation
  frames, 25,602 interface components, 8,093 client scripts, 629 keyframe sets, 10,751 sound
  effects, 830 songs, 311 jingles, 501 Vorbis samples and 155 MIDI patches.
- Generated `.data/audit/cache-233.4-client-contract-archives.json`: 139,184,075 bytes and SHA-256
  `3214d8b6c2a331401ee95dcbf1727d6e26da945ca5d606607a2f7c5325437e5d`. A second independent
  production export is byte-identical. The focused formatting, unit and integration gate passed in
  32 seconds: 360 actionable tasks, with 14 executed, 6 restored from cache and 340 up-to-date.
- Registered one integration-tested tooling group, taking the manifest to 478 groups: 48
  discovered, 121 researched, 1 unit-tested, 307 integration-tested and 1 verified.
- After pinning the exact domain totals and updating all durable audit records, the focused gate
  including `validateCache233Parity` passed in 56 seconds: 361 actionable tasks, with 149 executed,
  34 restored from cache and 178 up-to-date.
- The full repository `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 3 minutes 1 second: 868 actionable tasks, with 69 executed, 174 restored from cache and
  625 up-to-date. The generated reports contain 1,678 unit tests across 113 classes and 1,944
  integration tests across 224 classes, with zero failures, errors or skips.
- Server process 27276 remained healthy and listening on port 43594 throughout the final checks.
  The existing workspace-built RuneLite process remains open against the local environment; the
  installed RuneLite application was not opened. Its expected upstream world-list warning remains
  non-fatal.
- The production delta engine successfully compared both 225,204-entry manifests at full scale and
  reported zero added, removed or changed entries and 225,204 unchanged.

## 2026-09-12 — Vanilla and RSMod variable namespace inventory

- Extended the vanilla semantic exporter with all 1,261 varclients and 107 varobjects. Strict
  varclient decoding preserves the cache persistence contract, rejects unknown opcodes, trailing
  bytes and missing terminators, and classifies 494 records as persistent and 767 as transient.
  Every varclient has a revision-matched symbol. The 107 vanilla varobjects retain exact raw
  fingerprints as explicit opaque records because no semantic decoder or matching vanilla symbol
  is available; neither meaning nor names are fabricated.
- Regenerated `.data/audit/cache-233.4-semantic-config-manifest.json`: 165,477 entries across 15
  domains, 116,612,793 bytes and SHA-256
  `fe869b9fc02b89a41778a4d8b92695c2b5248ec409192c990acb01d1b53fff7a`. A second independent
  production export is byte-identical.
- Added `export-server-variables`, a separate enriched-runtime inventory for six cache-backed varns,
  eleven rsmod build-layer varobjects, three varcons and one controller. Cache records preserve raw
  fingerprints; code-owned definitions use canonical source fingerprints and explicit provenance.
  All 21 records have symbols and unique identities. The current varnbit and varconbit namespaces
  are explicitly empty.
- Generated `.data/audit/cache-233.4-rsmod-variable-namespaces.json`: 21 entries, 7,704 bytes and
  SHA-256 `7ecefc34c096609e2e65d57056fc654723dcc81838db44a1063d579d06c79ff4`. A second independent
  export is byte-identical; the production self-diff reports zero added, removed or changed and 21
  unchanged.
- Registered one integration-tested tooling group, taking the manifest to 479 groups: 48
  discovered, 121 researched, 1 unit-tested, 308 integration-tested and 1 verified. The focused
  formatting, unit, enriched-cache integration and manifest-validation gate passed in 12.67
  seconds: 363 actionable tasks, with 9 executed, 6 restored from cache and 348 up-to-date.
- The full repository `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 2 minutes 57 seconds: 868 actionable tasks, with 141 executed, 6 restored from cache and
  721 up-to-date. The generated reports contain 1,679 unit tests across 113 classes and 1,945
  integration tests across 225 classes, with zero failures, errors or skips.
- Server process 27276 remained healthy and listening on port 43594. The existing workspace-built
  RuneLite process remains open against the local environment; the installed RuneLite application
  was not opened. Its expected upstream price/world-list warnings remain non-fatal.

## 2026-09-12 — Map and collision-sensitive cache inventory

- Added `export-maps`, pairing an independently staged vanilla cache with its revision-matched XTEA
  file. It rejects duplicate map-square IDs, non-four-word keys, wrong archives and landscape names
  that do not match the packed square coordinates, then inventories both decrypted terrain and
  location files for all 2,266 squares.
- Every terrain record retains exact raw bytes plus decoded height, overlay, underlay, block-walk,
  link-below, roof and rendering-rule totals. Every landscape record retains exact raw bytes,
  XTEA group/key provenance without exposing the key words, and decoded location ID, shape,
  placement and per-level totals. The complete archive contains 4,319,171 location placements.
- The real-cache integration test exposed a bounded format remainder: rsmod's and workspace
  RuneLite's four-plane terrain decoders leave one byte in 2,265 files and 9,564 bytes in `m64_73`.
  The exporter retains all of those bytes in the fingerprint and reports decoded/remainder lengths;
  it does not claim unknown trailing data as decoded collision semantics.
- Generated `.data/audit/cache-233.4-map-square-manifest.json`: 4,532 entries, 3,342,129 bytes and
  SHA-256 `55b0e443569f8f22482f4a8897683b52224d31bd727e2a574966b97f4f6ec3df`. A second independent
  export is byte-identical; the production delta reports zero added, removed or changed and 4,532
  unchanged.
- Registered one integration-tested tooling group, taking the manifest to 480 groups: 48
  discovered, 121 researched, 1 unit-tested, 309 integration-tested and 1 verified. The focused
  formatting, unit, real-cache integration and manifest-validation gate passed in 10.38 seconds:
  360 actionable tasks, with 4 executed, 6 restored from cache and 350 up-to-date.
- The full repository `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 2 minutes 55 seconds: 868 actionable tasks, with 65 executed, 6 restored from cache and
  797 up-to-date. The generated reports contain 1,680 unit tests across 114 classes and 1,946
  integration tests across 226 classes, with zero failures, errors or skips.
- Server process 27276 remained healthy and listening on port 43594. The existing workspace-built
  RuneLite process remains open against the local environment; the installed RuneLite application
  was not opened. Its expected upstream price/world-list warnings remain non-fatal.

## 2026-09-12 — Whole-cache archive-index identity inventory

- Added `export-archive-indexes`, a deterministic whole-cache inventory that covers every present
  JS5 archive/group without reading or decrypting payloads. Each stable archive/group record
  fingerprints version, unsigned checksums, lengths, group name and the exact ordered file
  ID/name-hash set, with explicit archive-identity and client-contract risk tags.
- Corrected the cache-source boundary exposed by the exporter: `.data/cache/vanilla` is the
  untouched build-233 baseline, while `.data/cache/game` is rsmod's deliberately enriched runtime
  cache. The vanilla baseline contains 116,016 groups and 640,720 file identities across 23
  non-empty indexes, accounting for 148,468,920 compressed and 768,183,776 uncompressed indexed
  bytes. These totals agree with OpenRS2's official archive-2317 record. Exact per-archive totals
  and Lumbridge map group `5:12850` are integration-pinned.
- Generated `.data/audit/cache-233.4-archive-index-manifest.json`: 73,039,829 bytes and SHA-256
  `26ef85c11abc5a056df60c9cbc264f031e442ae2d455d49e1a5fa33c295227ae`. Its production self-diff
  reports 116,016 unchanged. Generated the separately labelled
  `.data/audit/cache-233.4-rsmod-enriched-archive-index-manifest.json`: 73,266,266 bytes and
  SHA-256 `d0439f11fa33e31d9e29f28d6d22535db843727d5130e0c36660003e5c4cd791`.
- The vanilla-to-runtime delta is 373 added, zero removed, 12 changed and 116,004 unchanged groups.
  Integration coverage pins the five added config groups, all 368 added archive-5 groups and the
  exact twelve rewritten existing config groups, preventing enriched content from being presented
  as upstream cache evidence.
- Registered one integration-tested tooling group, taking the manifest to 481 groups: 48
  discovered, 121 researched, 1 unit-tested, 310 integration-tested and 1 verified. The focused
  formatting, unit, real-cache integration and manifest-validation gate passed in 28 seconds: 360
  actionable tasks, with 8 executed, 6 restored from cache and 346 up-to-date.
- The full repository `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 28 seconds: 868 actionable tasks, with 8 executed, 6 restored from cache and 854
  up-to-date. The generated reports contain 1,681 unit tests across 115 classes and 1,948
  integration tests across 227 classes, with zero failures, errors or skips.
- This closes deterministic local cache structural identity and the vanilla/enriched provenance
  boundary, including encrypted and undecoded groups. Protocol metadata and safe staged
  authoritative acquisition remain separate open requirements.
- Server process 27276 remained healthy and listening on port 43594. The existing workspace-built
  RuneLite process remains open against the local environment; the installed RuneLite application
  was not opened. Its expected upstream world-list warning remains non-fatal.

## 2026-09-12 — Revision contract and safe cache acquisition

- Added `revision-contract.json`, a machine-readable oldschool/live/en build-233.4 lock for archive
  2317, authoritative capture/catalog/download metadata, vanilla group/file totals, the exact
  archive-index and XTEA fingerprints, and both rsprot module coordinates/version.
- Reworked `validateCache233Parity` to read the contract and cross-check the parity target, all
  relevant `Build` literals, the dependency catalog, and the computed SHA-256 of the checked
  73,039,829-byte vanilla archive-index artifact. Mixed cache, XTEA, build and protocol pins now
  fail one verification gate.
- Reworked `GameServerCacheDownloader` to download and extract into a sibling staging directory.
  Before promotion it now verifies the required cache disk indexes, exact XTEA SHA-256, 116,016
  group and 640,720 file-identity totals, and the regenerated canonical archive-index manifest
  SHA-256. The accepted cache is not moved until all network and identity checks pass.
- Promotion temporarily backs up the accepted vanilla cache and XTEAs and restores them after a
  caught failure. A durable pre-move state journal and post-pair commit marker let server startup
  idempotently recover an interrupted promotion. `CacheDownloadStagingTest` proves incomplete
  input preserves the accepted pair, a complete verified stage replaces both, an uncommitted
  interrupted promotion restores the old pair and a committed promotion retains the new pair.
  Multiple simultaneous journals stop for manual resolution rather than selecting one implicitly.
- A real-cache integration test regenerates and hashes the accepted cache contract and verifies the
  exact checked XTEA bytes through the same production pre-promotion path.
- Registered one integration-tested tooling group, taking the manifest to 482 groups: 48
  discovered, 121 researched, 1 unit-tested, 311 integration-tested and 1 verified.
- The focused contract, installer formatting/unit and real-cache integration gate passed in 12
  seconds after adding startup recovery: 361 actionable tasks, with 17 executed, 6 restored from
  cache and 338 up-to-date.
- The full repository `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 2 minutes 42 seconds: 872 actionable tasks, with 66 executed, 6 restored from cache and
  800 up-to-date. The generated reports contain 1,685 unit tests across 116 classes and 1,949
  integration tests across 228 classes, with zero failures, errors or skips.
- Server process 27276 remained healthy and listening on port 43594. The existing workspace-built
  RuneLite process remains open; the installed RuneLite application was not opened. Its expected
  upstream price/world-list warnings remain non-fatal.

## 2026-09-12 — NPC/player eight-slot interaction engine

- Completed the previously stubbed NPC-to-player operation and approach paths for slots six through
  eight. `InteractionOp`, client player-operation decoding, player and NPC exact/default event
  families, interaction resolution, script DSL registration, direct NPC helpers, the NPC mode
  processor and hunt redispatch now preserve the same slot end to end.
- Kept the extended slots player-target-only. Location, NPC and ground-item interaction routers now
  reject slots six through eight explicitly instead of assigning unsupported meanings.
- Corrected two adjacent ground-item defects found by the audit: `Npc.apObj5` now selects
  `NpcMode.ApObj5` instead of `ApLoc5`, and both the player and NPC default ground-item approach
  routers preserve options two through five instead of collapsing them to option one.
- Added eight focused regression scenarios: client packet decoding for slots six through eight;
  player event resolution for all new operation and approach slots; game-cycle NPC dispatch for all
  six new modes; matching default ground-item approach events for players and NPCs; and the fifth
  NPC ground-item mode. The focused gates passed, including the direct network-handler integration.
- Registered one integration-tested world/NPC group, taking the manifest to 483 groups: 48
  discovered, 121 researched, 1 unit-tested, 312 integration-tested and 1 verified.
- The full repository `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 2 minutes 51 seconds: 872 actionable tasks, with 153 executed, 6 restored from cache and
  713 up-to-date. The generated reports contain 1,688 unit tests across 118 classes and 1,954
  integration tests across 231 classes, with zero failures, errors or skips.
- Recorded the delivery sequence requested for the remaining work: finish cache-233.4 items, all 23
  skills and NPCs first; then Achievement Diaries, Combat Achievements, Fight Caves, Inferno and
  Fortis Colosseum. Fight Caves will establish the reusable wave-system substrate without treating
  the later encounters as interchangeable content.
- Server process 27276 remained healthy and listening on port 43594. The existing workspace-built
  RuneLite process remains open; the installed RuneLite application was not opened. Its expected
  upstream price/world-list warnings remain non-fatal.

## 2026-09-12 — NPC death/drop engine and first exact production family

- Replaced the NPC-death fabricated-bones placeholder with a typed production table registry and a
  composable roll model. Guaranteed items, weighted explicit-nothing tables, independent
  tertiaries, ordered suppressing pre-rolls, conditions, repeated/nested rolls and per-drop
  ownership, placement and lifetime policy are now first-class. Registration is atomic across an
  NPC family and rejects duplicate ownership.
- Routed registered results through the existing ground-object repository. The death service uses
  the credited hero and the production core random source; unregistered NPCs now emit no loot
  rather than inventing bones.
- Corrected the adjacent five-value `GameRandom.pick` overload, whose exclusive bound previously
  made its fifth value unreachable, and added a bound-sensitive regression.
- Added the first closed cache-233.4 production family: Undead chicken NPC 2993 always drops bones
  526 and raw undead chicken 4289, then independently selects five feathers at 64/128, fifteen at
  32/128 or explicit nothing at 32/128. Startup uses typed NPC/object references and follows the
  existing plugin-script syntax.
- Added six roll/registry unit scenarios, two injected repository-path scenarios and one production
  family integration scenario that covers cache identities, all three weight boundaries and the
  real private-owner ground-item result.
- Registered two integration-tested world/NPC groups, taking the manifest to 485 groups: 48
  discovered, 121 researched, 1 unit-tested, 314 integration-tested and 1 verified.
- The full repository `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 2 minutes 49 seconds: 876 actionable tasks, with 153 executed, 6 restored from cache and
  717 up-to-date. The generated reports contain 1,695 unit tests across 119 classes and 1,957
  integration tests across 233 classes, with zero failures, errors or skips.
- Restarted the workspace rsmod process so the new registry and Undead chicken plugin are active.
  Process 88723 loaded 58 plugin modules and 222 scripts, bound port 43594 and reported revision 233
  with Desktop-client support. The already-open workspace-built RuneLite process remains open; the
  installed RuneLite application was not opened. Its expected upstream world-list warnings remain
  non-fatal.

## 2026-09-12 — Explicit Duck no-drop family

- Audited the final pre-cutoff Duck record against cache 233.4 and registered both attackable
  identities, male 1838 and female 1839, to one explicit selected-nothing table. This distinguishes
  an evidence-backed no-loot family from an NPC that simply has no implementation yet.
- Added typed `duck` and `duck_female` references and an injected production test covering both
  cache identities/names, exact registry membership, empty direct rolls and zero repository objects
  through the real death service.
- Kept Rat out of this batch despite its superficially simple normal/Stronghold split: exact parity
  also requires the Rag and Bone Man II rat-bone condition, which must use the future quest-state
  substrate rather than an invented shortcut.
- Registered one integration-tested world/NPC group, taking the manifest to 486 groups: 48
  discovered, 121 researched, 1 unit-tested, 315 integration-tested and 1 verified.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 3
  minutes 5 seconds: 876 actionable tasks, with 155 executed, 6 restored from cache and 715
  up-to-date. The generated reports contain 1,695 unit tests across 119 classes and 1,958
  integration tests across 234 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 98869 loaded 58 plugin modules and 223 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The already-open
  workspace-built RuneLite session remains the only RuneLite process opened for this work; expected
  upstream world-list warnings remain non-fatal.

## 2026-09-12 — Enriched-runtime NPC spawn inventory

- Added `export-npc-spawns`, a deterministic semantic inventory for the server-only `nX_Z` groups
  written by RSMod cache enrichment and consumed at world startup. It preserves raw group identity
  while emitting stable individual placements with typed symbols, cache NPC fields, exact
  coordinates/planes and duplicate ordinals independent of source order.
- The production artifact proves six loaded map groups, 585 placements, 136 distinct NPC
  identities and complete symbol coverage. The six per-square totals are 60, 67, 135, 60, 166 and
  97. Actual `Attack` operations identify 460 placements across 49 distinct combat/drop-risk
  identities; default NPC stat values are deliberately not treated as proof of attackability.
- Generated `.data/audit/cache-233.4-rsmod-npc-spawns.json`: 429,195 bytes and SHA-256
  `1186494718430c80c9d58c1aba388aaac29ca057e6891b7ca7a0bb9e4a525a62`. This records the current
  RSMod-enriched runtime boundary without misrepresenting it as vanilla cache or production-wide
  spawn evidence.
- Added canonical-order and duplicate-ordinal unit coverage, malformed-group rejection, and a real
  enriched-cache integration contract pinning both domain totals, every map-square count, aggregate
  identities, attackable identities, symbols and fingerprints.
- Registered one integration-tested tooling group, taking the manifest to 487 groups: 48
  discovered, 121 researched, 1 unit-tested, 316 integration-tested and 1 verified.
- The focused cache-delta unit and real-cache integration gate passed in 11 seconds. The complete
  repository `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 2
  minutes 59 seconds: 876 actionable tasks, with 80 executed, 6 restored from cache and 790
  up-to-date. The generated reports contain 1,697 unit tests across 120 classes and 1,959
  integration tests across 235 classes, with zero failures, errors or skips.
- The existing workspace rsmod server and workspace-built RuneLite process remained live; this
  tooling-only change required no restart and did not open another client.

## 2026-09-12 — Loaded Duckling drop closure and conditional-tertiary research

- Extended the explicit no-drop family from male/female Ducks 1838/1839 to loaded Duckling 2001.
  The final pre-cutoff Duckling revision has no Drops section or conditional table, while cache
  233.4 independently pins its typed symbol, name, Attack operation and raw-config fingerprint.
- Strengthened the injected production test to cover all three cache IDs, names, Attack operations,
  exact registry ownership, empty direct rolls and zero repository objects through the real death
  service. This closes one more of the 49 currently loaded attackable cache identities without
  conflating an audited empty table with an unimplemented fallback.
- Audited Chicken, Rat, Spider and Giant spider against dated pre-cutoff revisions. Each exposes
  clue, quest, Wilderness or Wilderness Slayer conditional drops, so none was given a partial table.
  Their exact revision evidence and required shared substrates are recorded in the source ledger.
- Kept the manifest at 487 groups because this extends the existing Duck-family feature: 48
  discovered, 121 researched, 1 unit-tested, 316 integration-tested and 1 verified.
- The focused Duck-family production-path gate passed, followed by the complete `spotlessCheck`,
  `validateCache233Parity`, `test` and `integration` gate in 2 minutes 56 seconds: 876 actionable
  tasks, with 155 executed, 6 restored from cache and 715 up-to-date. Generated reports contain
  1,697 unit tests across 120 classes and 1,959 integration tests across 235 classes, with zero
  failures, errors or skips.
- Restarted only the workspace rsmod server. Process 28026 loaded 58 plugin modules and 223 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing
  workspace-built RuneLite session remains live and is still the only RuneLite opened for this
  work; its expected upstream world-list warning remains non-fatal.

## 2026-09-12 — Shared clue ownership and cache-233 scroll boxes

- Corrected the earlier cache interpretation using semantic definitions rather than internal
  symbol names. Cache objects 24361–24366 are the production `Scroll box (beginner)` through
  `Scroll box (master)` objects despite their legacy `league_clue_box_*` symbols; object 5075 is
  the empty bird nest. The dated pre-cutoff Clue nest, Scroll box and X Marks the Spot revisions
  pin the post-quest replacement, stackability, normal three-minute box lifetime, two-to-five
  capacity and successful-roll warning.
- Added a reusable client-script semantic inspector and a real-cache integration contract proving
  that X Marks DB row 162 has completion state 8, client script 4029 routes through quest script
  4024 and that script's row-162 branch reads cluequest varbit 8063. This closes the quest-state
  identity with cache behavior rather than trusting a suggestive symbol.
- Added the shared `api/clue` ownership substrate. It discovers every same-tier clue scroll, nest,
  bottle, geode and scroll box from cache display names; counts inventory and bank quantities;
  excludes placeholders, keys, caskets and juggled ground items; and derives capacity from X Marks,
  all twelve tier case varbits and the Mimic-case varbit. Death's Office remains explicitly outside
  the current inventory model.
- Extended Woodcutting so pre-quest successes retain private two-minute clue nests, while post-quest
  successes place the corresponding private three-minute scroll box and two-minute empty nest on
  one adjacent tile. At capacity, the tier is still rolled and a successful roll emits the exact
  `You have a sneaking suspicion that you would have received a [tier] scroll box.` message without
  consuming placement randomness or creating objects. Logs, XP and `CutLogs` remain tertiary-safe.
- Seven focused Woodcutting clue scenarios now pin the cache identities, quest/case variables,
  inventory and bank classification, capacity progression, exact capped warning, shared placement,
  distinct 200/300-tick expiry and the existing formula, eligibility and successful-cut behavior.
  The client-script semantic integration independently pins the quest routing.
- The manifest remains at 487 groups because this completes the post-X-Marks part of the existing
  Woodcutting clue-nest group: 48 discovered, 121 researched, 1 unit-tested, 316 integration-tested
  and 1 verified. The full `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 3 minutes 13 seconds: 880 actionable tasks, with 214 executed, 6 restored from cache and
  660 up-to-date. Reports contain 1,697 unit tests across 120 classes and 1,963 integration tests
  across 236 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 55944 loaded 58 plugin modules and 223 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing
  workspace-built RuneLite session remained the only RuneLite opened; its expected upstream
  price/world-list warnings remain non-fatal.

## 2026-09-12 — Chicken drop family and shared NPC clue rewards

- Closed the twelve final pre-cutoff production Chicken identities as one atomic drop family:
  1173/1174, 2804–2806, 3661/3662, 9488 and 10494–10497. Every registered identity now gives bones
  526 and raw chicken 2138, then selects five feathers at 64/128, fifteen at 32/128 or explicit
  nothing at 32/128. Cache-only Chicken-shaped identities 10498/10499 remain deliberately
  unregistered because neither the dated Wiki version list nor enriched runtime gives them a
  production role.
- Added the exact members-only medium step reward: carrying clue 2837 while killing any registered
  Chicken always adds key 2838, while another medium clue, no clue or free-to-play state cannot.
  This is independent of both the ordinary feather table and beginner clue roll.
- Extended `ClueInventory` with live player inventory/bank state and a reusable successful-roll
  reward policy. Added `NpcClueDropRolls` so NPC tables share pre-X-Marks same-tier ownership,
  post-X-Marks scroll boxes and two-to-five case capacity, the exact successful capped-roll warning,
  private ground ownership and distinct one-hour clue/three-minute box lifetimes with Woodcutting.
- Extended NPC drop resolutions with ordered player feedback delivered by the ordinary credited-hero
  death path. Unit coverage pins feedback composition; five injected Chicken scenarios pin exact
  family registration, deliberate cache-only exclusions, every weighted boundary, medium key gates,
  the independent 1/300 beginner reward, skipped ownership randomness, X Marks capacity behavior,
  exact warning delivery and repository lifetime expiry.
- Added manual case 114 and reconciled the feature matrix, source ledger and known-gap inventory.
  Registered one integration-tested world/NPC group, taking the manifest to 488 groups: 48
  discovered, 121 researched, 1 unit-tested, 317 integration-tested and 1 verified. The immediate
  loaded-world attackable drop backlog falls from 46 to 44 distinct identities.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 3
  minutes 6 seconds: 880 actionable tasks, with 165 executed, 6 restored from cache and 709
  up-to-date. Generated reports contain 1,698 unit tests across 120 classes and 1,968 integration
  tests across 237 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 82936 loaded 58 plugin modules and 224 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing
  workspace-built RuneLite session remains the only RuneLite opened; its expected upstream
  price/world-list warnings remain non-fatal.

## 2026-09-12 — Cow and Cow calf production family

- Closed all eight final pre-cutoff production Cow-family identities as one shared implementation:
  adults 2790, 2791, 2793, 2795 and 5842, plus calves 2792, 2794 and 2801. Every identity now uses
  an explicit 45-cycle respawn override, matching the dated Wiki's 45-tick (27-second) contract on
  the server's 600-millisecond cycle rate. This corrects the initial reading of the tick-valued
  infobox field as seconds.
- Added one ordered death table for the complete family. Cowhide 1739, raw beef 2132 and bones 526
  are guaranteed in that order, followed by an independent 1/128 beginner clue roll using the
  shared clue-ownership, X Marks the Spot replacement, capacity-warning and private-ground-lifetime
  policies introduced for Chickens.
- Implemented the exact identity-specific item interactions for adult Cows and calves, including
  empty-bucket and bucket-of-milk responses, generic item rejection, and the Zanaris Cow's standard
  Talk-to exchange and Nuff's certificate branch. The separate dairy-cow/Cold War behavior remains
  explicitly outside this family.
- Six injected production-path scenarios pin the cache identities, operations, respawn override,
  content groups, drop ordering, both clue outcomes and expiry, credited death-repository path,
  ownership/capacity behavior, every item branch and both Zanaris conversations. Added manual case
  115 and reconciled the feature matrix, source ledger and known-gap inventory.
- Registered one integration-tested world/NPC group, taking the manifest to 489 groups: 48
  discovered, 121 researched, 1 unit-tested, 318 integration-tested and 1 verified. The immediate
  loaded-world attackable drop backlog falls from 44 to 39 distinct identities.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 2
  minutes 59 seconds: 880 actionable tasks, with 156 executed, 6 restored from cache and 718
  up-to-date. Generated reports contain 1,698 unit tests across 120 classes and 1,974 integration
  tests across 238 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 14916 loaded 58 plugin modules and 225 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing
  workspace-built RuneLite session remains the only RuneLite opened; its expected upstream
  price/world-list warnings remain non-fatal.

## 2026-09-12 — Level-five Frog production contract and Cow tick correction

- Corrected the Cow-family respawn override from 75 to 45 server cycles. The historical monster
  infobox value is 45 game ticks, which is 27 seconds on rsmod's 600-millisecond clock; it was not a
  45-second duration. Updated the integration assertion, manual case, feature matrix, source ledger
  and manifest so the earlier interpretation cannot remain as contradictory evidence.
- Closed loaded level-five Frog 8702 as a self-contained production contract. Added its typed cache
  reference and editor overrides for a 50-tick respawn and 40% Earth weakness while pinning the
  cache's exact 5/4/3/8/1/1 combat-stat tuple and Attack operation.
- Added guaranteed bones 526 followed by the independent 1/90 beginner clue. The table reuses the
  shared clue policy for pre-X-Marks inventory/bank ownership, post-X-Marks scroll boxes, capacity
  warning and distinct one-hour clue/three-minute box ground lifetimes. Three injected scenarios
  cover both probability boundaries, ordering, credited death/repository spawning, expiry and the
  shared ownership/capacity branches.
- Researched Big frog and Giant frog but kept them open: Big frog's conditional quest bone requires
  the shared Rag and Bone Man I per-bone checklist, while Giant frog requires the shared mutually
  exclusive long/curved-bone tertiary policy. No ownership-only or NPC-specific approximation was
  introduced. Added manual case 116 for matching-client combat, timing, statistical and packet
  evidence.
- Registered one integration-tested world/NPC group, taking the manifest to 490 groups: 48
  discovered, 121 researched, 1 unit-tested, 319 integration-tested and 1 verified. The immediate
  loaded-world attackable drop backlog falls from 39 to 38 distinct identities.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 2
  minutes 59 seconds: 880 actionable tasks, with 156 executed, 6 restored from cache and 718
  up-to-date. Generated reports contain 1,698 unit tests across 120 classes and 1,977 integration
  tests across 239 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 45388 loaded 58 plugin modules and 226 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing
  workspace-built RuneLite session remained the only RuneLite opened; its expected upstream
  price/world-list warnings remain non-fatal.

## 2026-09-12 — Reusable standard NPC herb table

- Pinned the final pre-cutoff `HerbDropLines` template revision 14831791 and implemented its exact
  nested 128-weight standard herb table: grimy guam/marrentill/tarromin/harralander/ranarr/irit/
  avantoe/kwuarm/cadantine/lantadyme/dwarf weed at 32/24/18/14/11/8/6/5/4/3/3. The public roll
  resolves typed cache objects 199/201/203/205/207/209/211/213/215/2485/217 rather than embedding
  numeric production objects in NPC plugins.
- Added a pure unit boundary test and an injected cache integration test. Together they select the
  lower and upper boundary of all eleven entries and assert the total is exactly 128, covering the
  reusable probability contract independently of any consumer NPC's entry chance.
- Researched loaded Mugger 513 and the full dated Mugger family. Production registration remains
  open because the 1/128 members-only Easy clue needs the real initial-step selector and because
  the full, Port Sarim jail and plain Piscarilius/Vinery versions require exact location/version
  routing. No arbitrary Easy clue object or broad all-version table was introduced.
- Registered one integration-tested world/NPC group, taking the manifest to 491 groups: 48
  discovered, 121 researched, 1 unit-tested, 320 integration-tested and 1 verified.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 2
  minutes 58 seconds: 880 actionable tasks, with 81 executed, 6 restored from cache and 793
  up-to-date. Generated reports contain 1,699 unit tests across 120 classes and 1,978 integration
  tests across 240 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 59191 loaded 58 plugin modules and 226 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing
  workspace-built RuneLite session remained open and untouched.

## 2026-09-12 — Cache-233 Easy initial-step pool and loaded Lumbridge Mugger

- Pinned the final pre-cutoff Easy clue revision 14986803 and workspace RuneLite 1.11.18 commit
  `d14c12197c80f76a2833c7f744cacec896096d76`. The four cutoff decoders enumerate exactly 165
  unique Easy initial objects: 120 Cryptic, 32 Emote, seven Map and six Music. Cache 233.4 resolves
  exactly the same sorted identities, names and `Read`/`Check steps` operations.
- Added a cache-derived, exact-ID-audited Easy selector. NPC Easy successes uniformly choose any
  of its 165 entries; the shared clue reward path now accepts a lazy object supplier so post-X-
  Marks scroll boxes do not consume a hidden step-selection roll. Three injected scenarios cover
  every identity/category, both selector endpoints, invalid indexes and the lazy box branch. This
  closes only initial reward identity; reading, completing, caskets and rewards remain open.
- Closed loaded Lumbridge Mugger 513 with guaranteed bones; the exact members/free-world 128-share
  main table, including variable bronze bolts, the shared herb subtable and selected nothing; and
  independent 1/80 beginner plus members-only 1/128 Easy clues. Four injected scenarios pin the
  cache/registry contract, all fifteen main-table lower boundaries, nested quantity/herb rolls,
  free-world substitutions, clue order, Easy endpoint and ground lifetimes.
- Kept Mugger 1461, 6996 and 13282 outside production registration until Port Sarim jail, plain
  Piscarilius/Vinery and Varlamore routing is independently evidence-closed. Added manual case 117
  for statistical, matching-client, combat, aggression, placement and variant observations.
- Registered two integration-tested groups, taking the manifest to 493 groups: 48 discovered, 121
  researched, 1 unit-tested, 322 integration-tested and 1 verified. The immediate loaded-world
  attackable drop backlog falls from 38 to 37 distinct identities.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 2
  minutes 59 seconds: 880 actionable tasks, with 157 executed, 6 restored from cache and 717
  up-to-date. Generated reports contain 1,699 unit tests across 120 classes and 1,985 integration
  tests across 242 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 81227 loaded 58 plugin modules and 227 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing
  workspace-built RuneLite session remained open and untouched.

## 2026-09-13 — Five-identity Ram family and exact quest-skull lifecycle

- Pinned final pre-cutoff Ram revision 14959417, Ram skull revision 14550342 and Rag and Bone Man I
  revision 14991439, with cutoff Quest Helper commit
  `241eaec29b19243bda7e88e99d5c16568c0776a6` and cache quest row 114 as independent state
  evidence. Together they establish five Rams 1261–1265, quest varp 714 collection state two,
  completion state four, ram-skull hand-in index three and raw/vinegar/polished objects
  7818/7819/7820.
- Registered all five typed Rams with guaranteed bones, exact 45-tick respawns and 20% Fire
  weakness. Added the exact non-consuming shears refusal to every form.
- Added a reusable Rag and Bone Man I drop policy for the eight monster bones. It requires a
  members account in exact collection state two and suppresses a quest bone when any lifecycle
  form is in inventory/bank or its persistent per-bone hand-in bit is set. The server-only mask at
  reserved varp 65494 prevents false replacement after an individually delivered bone leaves
  storage and exposes an atomic operation for the later quest hand-in plugin.
- Four injected scenarios pin the five cache/editor/registry contracts, ordinary and quest drop
  gates, all lifecycle ownership paths, permanent hand-in state and exact shears behavior. Manual
  case 118 retains live combat, timing, persistence, quest-flow and matching-client observations.
  The wider quest dialogue, vinegar processing and completion content remains explicitly open.
- Registered one integration-tested group, taking the manifest to 494 groups: 48 discovered, 121
  researched, 1 unit-tested, 323 integration-tested and 1 verified. Completing loaded Ram IDs
  1262–1264 reduces the immediate loaded-world attackable drop backlog from 37 to 34 identities;
  the two additional dated identities are covered without inflating the loaded count.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 3
  minutes: 880 actionable tasks, with 154 executed, 6 restored from cache and 720 up-to-date.
  Generated reports contain 1,699 unit tests across 120 classes and 1,989 integration tests across
  243 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 8843 loaded 58 plugin modules and 229 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing
  workspace-built RuneLite session remained open and untouched.

## 2026-09-13 — Loaded level-ten Big frog and sixth quest-bone lifecycle

- Pinned final pre-cutoff Big frog revision 14990022, including the 27 August 2025 level-ten quest-
  drop addition, and independently verified Quest Helper's sixth ordered Rag and Bone Man I entry
  at cutoff commit `241eaec29b19243bda7e88e99d5c16568c0776a6`.
- Closed loaded free-to-play Big frog 8701 with its exact 8/6/4/18/1/1 cache stats, 50-tick
  respawn, 25% Earth weakness, guaranteed bones and independent 1/70 beginner clue. The members-
  only Big frog leg uses shared raw/vinegar/polished inventory/bank ownership and persistent hand-
  in bit five, preserving bones/leg/clue order when both conditional rewards succeed.
- Kept members level-24 identity 478 outside this loaded contract because it has a different
  weighted rune/coin/talisman table. Giant frog 8700 remains open behind the reusable mutually
  exclusive long/curved-bone policy rather than receiving an NPC-local approximation.
- Three injected scenarios pin the cache/editor/registry contract, quest and clue boundary/order,
  all six lifecycle storage branches, persistent hand-in state and clue lifetime. Manual case 119
  retains live probability, combat, presentation, persistence and matching-client observations.
- Registered one integration-tested group, taking the manifest to 495 groups: 48 discovered, 121
  researched, 1 unit-tested, 324 integration-tested and 1 verified. The immediate loaded-world
  attackable drop backlog falls from 34 to 33 identities.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 2
  minutes 58 seconds: 880 actionable tasks, with 153 executed, 6 restored from cache and 721
  up-to-date. Generated reports contain 1,699 unit tests across 120 classes and 1,992 integration
  tests across 244 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 19811 loaded 58 plugin modules and 230 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing workspace-
  built RuneLite session remained open and untouched.

## 2026-09-13 — Shared rare-bone policy and loaded level-thirteen Giant frog

- Pinned final pre-cutoff Giant frog revision 14961999 and the independent Long bone and Curved
  bone revisions 14976983/14967442. Together they prove the shared ordered members roll: 1/400 for
  a long bone first, then 1/5,000 for a curved bone only after the long-bone miss, yielding the
  published effective curved rate of 1/5,012.5 and preventing both rewards on one death.
- Added `NpcRareBoneDropRolls.longOrCurved()` as a reusable typed policy rather than embedding the
  probabilities in one NPC. Its pure boundary test covers free-world suppression, long success,
  curved success after a long miss and the total-miss boundary.
- Closed loaded free-to-play Giant frog 8700 with exact cache stats 10/8/6/23/1/1, size two,
  50-tick respawn, 25% Earth weakness, guaranteed big bones, independent 1/64 beginner clue and
  the shared members-only rare-bone roll. Level-99 identity 477 remains outside this loaded contract
  with its separate weighted table.
- Three injected scenarios pin the cache/editor/registry contract, free-world behavior, clue and
  rare-bone independence/order, mutual exclusion and every probability boundary. Manual case 120
  retains live combat, statistical, presentation and matching-client observations.
- Registered two integration-tested groups, taking the manifest to 497 groups: 48 discovered, 121
  researched, 1 unit-tested, 326 integration-tested and 1 verified. The immediate loaded-world
  attackable drop backlog falls from 33 to 32 identities.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 2
  minutes 50 seconds: 880 actionable tasks, with 160 executed, 6 restored from cache and 714
  up-to-date. Generated reports contain 1,700 unit tests across 120 classes and 1,995 integration
  tests across 245 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 46259 loaded 58 plugin modules and 231 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing workspace-
  built RuneLite session remained open and untouched.

## 2026-09-13 — Loaded ordinary Unicorn and fresh ensouled-head provenance

- Pinned final pre-cutoff Unicorn revision 14989492, Ensouled head revision 14978131 and Ensouled
  unicorn head revision 14420589, plus cutoff Quest Helper commit
  `241eaec29b19243bda7e88e99d5c16568c0776a6`. Together they establish loaded ordinary Unicorn
  2837, its exact 11/13/13/19/1/1 stats, size two, 90-tick respawn, guaranteed bones,
  members-only horn, outstanding second Rag and Bone Man I bone and independent members-only 1/35
  initial ensouled head. The Wiki documents a low-Hitpoints retreat but no exact threshold; cache
  parameter 26 is zero, so production deliberately leaves that behavior open rather than guessing.
- Added the typed Unicorn configuration and drop table. Its quest bone reuses the shared
  raw/vinegar/polished inventory/bank and persistent hand-in policy at zero-based bit one. The fresh
  ensouled reward uses a new shared `NpcEnsouledHeadDropRolls` helper that attaches the death tile
  before the result enters the ordinary credited-player drop path.
- Added server-only per-ground-item variables and preserved them through NPC/player/server spawn,
  private stack splitting, pickup and player re-drop. Ground stacks with different variables no
  longer merge, so heads from distinct origin areas remain distinct. `FreshDropOrigin` packs the
  immutable coordinate and proves the same-level Chebyshev radius-15 boundary.
- Corrected Arceuus reanimation so both carried and ground initial heads require their recorded
  31-by-31 drop-origin area; an unprovenanced initial head cannot bypass the restriction, while
  processed heads remain Dark-Altar-only. Banking now atomically converts structurally matched
  initial heads to their adjacent processed cache identity for single and bulk deposits, and
  restores identity plus origin whenever a deposit fails or leaves a residual. Trade, Grand
  Exchange, price checker and Soul Bearer expiration remain explicit gaps because those systems do
  not yet exist in this repository.
- Unit and injected coverage pins coordinate round-trips and area edges, variable-aware stack
  separation, exact cache identities/stats/respawn/drop gates, every Unicorn quest-bone lifecycle
  branch, 1/35 provenance, ordinary pickup, carried/ground reanimation range, processed rejection,
  successful single/bulk bank expiration and failed-deposit restoration. Manual case 121 retains
  live retreat, probability, ground presentation, matching-client and unavailable-system checks;
  case 95 retains the broader reanimation encounter/client matrix.
- Registered one integration-tested world/NPC group, taking the manifest to 498 groups: 48
  discovered, 121 researched, 1 unit-tested, 327 integration-tested and 1 verified. Updating the
  existing reanimation group does not inflate the count. The immediate loaded-world attackable
  drop backlog falls from 32 to 31 identities.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 3
  minutes 2 seconds: 880 actionable tasks, with 162 executed, 6 restored from cache and 712
  up-to-date. Generated reports contain 1,703 unit tests across 122 classes and 2,002 integration
  tests across 246 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 89739 loaded 58 plugin modules and 232 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing workspace-
  built RuneLite session remained open and untouched.

## 2026-09-12 — Loaded ordinary Imp and shared Champion's scroll lifecycle

- Pinned final pre-cutoff Imp revision 14988743, Imp champion scroll revision 14985323 and
  Champion's scroll revision 14895741, plus three archived Mod Ash clarifications. Together they
  establish ordinary Imp 5007, its exact combat/config contract, guaranteed fiendish ashes, exact
  128-share 32-entry main table, members-only 1/25 fresh ensouled head and members-only 1/5,000
  champion-scroll roll gated by 32 quest points.
- Added the typed Imp NPC editor and drop table. Cache-raw respawn 100 is corrected to the sourced
  50 ticks, Water weakness is 10%, every main-table boundary is explicit, and the ensouled head
  reuses shared immutable fresh-origin provenance.
- Added reusable `NpcChampionScrollDropRolls`. It permits repeat scrolls until the matching
  champion is defeated; a successful roll after victory creates no object and emits the exact
  dark-red would-have-received message. Quest points and the Imp-victory varbit are typed through
  cache-backed base config references.
- Unit and injected coverage pins membership and quest-point eligibility, success/miss boundaries,
  pre-victory repeats, post-victory suppression/message, exact NPC stats/config, all 32 weighted
  boundaries and initial-head provenance. Manual case 122 retains statistical, teleport,
  audiovisual, packet, ground-presentation and matching-client observation.
- Registered one integration-tested world/NPC group, taking the manifest to 499 groups: 48
  discovered, 121 researched, 1 unit-tested, 328 integration-tested and 1 verified. The immediate
  loaded-world attackable drop backlog falls from 31 to 30 identities.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 3
  minutes: 880 actionable tasks, with 154 executed, 6 restored from cache and 720 up-to-date.
  Generated reports contain 1,706 unit tests across 123 classes and 2,007 integration tests across
  247 classes, with zero failures or errors.
- Restarted only the workspace rsmod server. Process 16020 loaded 58 plugin modules and 233 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing workspace-
  built RuneLite session remained open and untouched.

## 2026-09-12 — Six loaded regular Giant rat identities

- Pinned final pre-cutoff Giant rat revision 14961330 and Giant rat bone revision 14550275, plus
  cutoff Quest Helper commit `241eaec29b19243bda7e88e99d5c16568c0776a6`. Together they
  establish the level-three and level-six stat families, aggression, combat cadence, 30-tick
  respawn, guaranteed bones/raw rat meat, independent 1/128 beginner clue and first Rag and Bone
  Man I quest-bone lifecycle.
- Added typed references, a shared NPC editor and one atomic drop-table registration for all six
  identities loaded in the Lumbridge region: 2856, 2859, 2860, 2861, 2863 and 2864 across 27
  placements. The editor corrects cache-raw respawn 100 to 30 and uses the existing standard melee
  hunt contract with cache range five. All current placements are outside the Wilderness, so the
  members-only looting-bag tertiary is deliberately excluded.
- Reused the shared beginner-clue ownership/capacity/lifetime policy and Rag and Bone Man I
  raw/vinegar/polished inventory/bank plus persistent hand-in policy at zero-based bit zero. No
  quest or clue state was approximated inside the NPC-specific plugin.
- Three injected scenarios pin all six cache identities, both exact stat tuples, size, editor and
  registry contracts, guaranteed order, free/member and quest boundaries, clue independence and
  lifetime, all quest-bone storage forms and persistent hand-in suppression. Manual case 123
  retains live aggression edges, combat, probability, ground presentation, persistence and
  matching-client observation.
- Registered one integration-tested world/NPC group, taking the manifest to 500 groups: 48
  discovered, 121 researched, 1 unit-tested, 329 integration-tested and 1 verified. The immediate
  loaded-world attackable drop backlog falls from 30 to 24 identities.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 3
  minutes 2 seconds: 880 actionable tasks, with 153 executed, 6 restored from cache and 721
  up-to-date. Generated reports contain 1,706 unit tests across 123 classes and 2,010 integration
  tests across 248 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 32748 loaded 58 plugin modules and 234 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing workspace-
  built RuneLite session remained open and untouched.

## 2026-09-13 — Sixteen loaded regular Rat placements

- Pinned final pre-cutoff Rat revision 14973360, Rat's tail revision 14679825, Rat bone revision
  14423224, Witch's Potion revision 14994830 and Rag and Bone Man II revision 14990023, plus cutoff
  Quest Helper commit `241eaec29b19243bda7e88e99d5c16568c0776a6`. Together they establish
  ordinary Rat 2854, its exact level-one stats, zero maximum hit, non-aggression, four-tick Crush
  cadence, two-tick respawn and independent quest-drop rules.
- Added the typed Rat editor and drop table for all 16 loaded Lumbridge placements. Witch's Potion
  states one and two always add Rat's tail and deliberately do not suppress another tail already in
  inventory or bank, preserving the documented drop trick. Members in Rag and Bone Man II
  collection state four independently roll the outstanding Rat bone at 1/4; raw, vinegar and
  polished forms in inventory or bank plus permanent hand-in bit six all suppress it. Every current
  placement is outside the Wilderness, so the Rat's members-only looting-bag tertiary is excluded.
- Audited the real cache before assigning the 27-bone hand-in mask. Varp 715 has no cache varbits
  and no direct client-script access, while adjacent varp 716 is already packed by named pot and
  boiler varbits. The existing cache-backed 715 identity is now named and typed rather than
  inventing a server-only duplicate; shared policy validation pins the 27-bit domain and preserves
  unrelated completed bones when Rat bit six is marked.
- Three injected scenarios pin cache fingerprints and configuration, tail and quest-bone state
  boundaries, independent reward ordering, free/member behavior and the complete inventory, bank
  and persistent hand-in lifecycle. Manual case 124 retains live combat, timing, quest-state,
  drop-trick, probability, persistence, ground-presentation and matching-client observation.
- Registered one integration-tested world/NPC group, taking the manifest to 501 groups: 48
  discovered, 121 researched, 1 unit-tested, 330 integration-tested and 1 verified. The immediate
  loaded-world attackable drop backlog falls from 24 to 23 identities.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 3
  minutes 4 seconds: 880 actionable tasks, with 153 executed, 7 restored from cache and 720
  up-to-date. Generated reports contain 1,706 unit tests across 123 classes and 2,013 integration
  tests across 249 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 62756 loaded 58 plugin modules and 235 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing workspace-
  built RuneLite session remained open and untouched.

## 2026-09-13 — Twenty-three loaded Spider-family placements

- Pinned final pre-cutoff Spider revision 14982545 and Giant spider revision 14983292. Together
  they establish regular Spider 3019 and level-two Giant spider 3017 as distinct from their
  Underground Pass, Stronghold and higher-level identities; exact stats, size, four-tick Stab
  cadence, 35-/30-tick respawns, 50% Fire weaknesses and regular non-aggression versus Giant-
  spider aggression are version-locked.
- Added typed references and one shared editor for all seven loaded regular Spiders and sixteen
  loaded Giant spiders. The editor leaves regular Spider without a hunt mode and assigns only the
  Giant spider to the existing standard five-tile melee hunt contract. All 23 enriched-cache
  placements are on level zero between z 3222 and 3256 and are outside the Wilderness.
- Added one atomic drop-table registration for the two loaded identities. Both deliberately have
  no ordinary rewards and independently roll only a 1/128 beginner clue through the existing
  ownership, X Marks scroll-box, capacity, exact warning and private-ground-lifetime policy.
  Wilderness looting bags and the Wilderness Slayer tertiary are excluded by placement rather than
  approximated as universal Spider rewards.
- Three injected scenarios pin both exact cache fingerprints, stats, operations, editor and
  registry contracts; the miss and success boundaries, repository-backed private clue spawn; and
  every shared clue ownership, replacement, capacity, feedback and lifetime branch. Manual case
  125 retains matching-client combat, aggression, timing, distribution, ground-presentation and
  Wilderness-exclusion checks.
- Registered one integration-tested world/NPC group, taking the manifest to 502 groups: 48
  discovered, 121 researched, 1 unit-tested, 331 integration-tested and 1 verified. The immediate
  loaded-world attackable drop backlog falls from 23 to 21 identities.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 2
  minutes 59 seconds: 880 actionable tasks, with 152 executed, 6 restored from cache and 722
  up-to-date. Generated reports contain 1,706 unit tests across 123 classes and 2,016 integration
  tests across 250 classes, with zero failures, errors or skips.
- Restarted only the workspace rsmod server. Process 78966 loaded 58 plugin modules and 236 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. The existing workspace-
  built RuneLite session remained open and untouched.

## 2026-09-13 — 128 loaded Goblin-family placements

- Pinned final pre-cutoff Goblin revision 14956290 and cutoff Quest Helper commit
  `241eaec29b19243bda7e88e99d5c16568c0776a6`. Together they distinguish the eight unarmed
  level-two identities 3029–3036 from armed level-five 3045; establish exact stats, four-/six-tick
  Crush/Stab attacks, non-aggression and 35-tick respawns; and pin the fifth Rag and Bone Man I
  quest-bone hand-in position.
- Added typed NPC/object/varbit references and a production editor for all 127 unarmed placements
  plus 3045 at 3167,3205,0. The editor corrects the generic enricher's two-tick attack cadence on
  all nine identities and its incorrect Crush style on 3045. Every loaded placement is outside the
  Wilderness and neither family receives an invented hunt mode or elemental weakness.
- Added both exact 128-share main tables with free/member selected-empty slots, the free-world
  herb-branch replacement and 3045's cache-model-backed Green goblin mail. Ordered independent
  tertiaries cover the outstanding 1/4 quest skull and its raw/vinegar/polished plus persistent
  hand-in lifecycle; members-only initial ensouled heads at 1/35 or 1/30; beginner clues at 1/64
  or 1/80; Easy clues at 1/128; and the shared 32-quest-point-gated 1/5,000 Goblin champion-scroll
  repeat/victory policy through cache varbit 1455.
- Seven injected scenarios pin all nine exact cache fingerprints and production configuration,
  every boundary of both main tables, free-world behavior, every tertiary rate and order,
  repository-backed fresh-head provenance, the complete quest-skull lifecycle and champion
  completion behavior. Manual case 126 retains live combat, probability, ground-presentation,
  persistence, kill-credit and matching-client observation.
- Registered one integration-tested world/NPC group, taking the manifest to 503 groups: 48
  discovered, 121 researched, 1 unit-tested, 332 integration-tested and 1 verified. The immediate
  loaded-world attackable drop backlog falls from 21 to 12 identities.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 3
  minutes 7 seconds: 880 actionable tasks, with 153 executed, 6 restored from cache and 721
  up-to-date. Generated reports contain 1,706 unit tests across 123 classes and 2,023 integration
  tests across 251 classes, with zero skips, failures or errors.
- Restarted only the workspace rsmod server. Process 97327 loaded 58 plugin modules and 237 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. No RuneLite process was
  opened, closed or changed; the workspace-built clients and pre-existing installed-app process
  were left untouched.

## 2026-09-13 — Final twelve loaded civilian identities

- Pinned final pre-cutoff Man 14941419, Woman 14984028 and Farmer 14941540 revisions, plus exact
  standard-herb and allotment-seed templates 14831791/14831775. Together they establish the final
  twelve open identities in the bounded enriched loaded-world attackable inventory: seven Man/
  Woman identities across sixteen placements and five Farmer identities across seven placements.
- Extended the generic Person editor to the dated 25-tick respawn, four-tick Crush contract and
  registered the exact shared 128-share civilian table. Members/free behavior preserves the
  selected-empty bronze-bolt slot, herb-to-ten-coins substitution, 1/90 beginner and members-only
  1/128 Easy clues. Men alone produce exact key 2838 while carrying medium-step clue 2837.
- Added five typed Farmer identities and a dedicated editor with the exact level-seven stat tuple,
  25-tick respawn, six-tick Stab cadence, +5 Stab accuracy and +6 melee Strength. The free and
  members 128-share tables remain distinct, with guaranteed bones and independent 1/90 beginner/
  1/128 members Easy clues.
- Added reusable `NpcAllotmentSeedDropRolls` beside the existing herb primitive. Its exact
  128-share nested distribution and every quantity range are cache-typed for potato, onion,
  cabbage, tomato, sweetcorn, strawberry, watermelon and snape-grass seeds.
- Ten injected scenarios pin every identity/editor/registry contract, every boundary of the three
  main tables, every allotment-seed boundary and quantity endpoint, all free/member substitutions,
  the exact Man key condition and clue order/eligibility. Manual cases 127 and 128 retain live
  combat, distribution, pickpocket coexistence, ground-presentation and matching-client evidence.
- Registered two integration-tested world/NPC groups, taking the manifest to 505 groups: 48
  discovered, 121 researched, 1 unit-tested, 334 integration-tested and 1 verified. All 49
  attackable identities across the bounded 460-placement loaded-world manifest now have evidence-
  closed combat/drop contracts; production-world NPCs outside the six loaded squares remain open.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 3
  minutes 16 seconds: 880 actionable tasks, with 156 executed, 6 restored from cache and 718
  up-to-date. Generated reports contain 1,706 unit tests across 123 classes and 2,033 integration
  tests across 253 classes, with zero skips, failures or errors.
- Started only the workspace rsmod server. Process 31671 loaded 58 plugin modules and 239 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. No RuneLite process was
  opened, closed or changed; both workspace-built clients and the pre-existing installed-app
  process were left untouched.

## 2026-09-13 — Combination-rune core and post-baseline roadmap

- Pinned the final pre-cutoff Combination rune and Magic Imbue evidence for all twelve directional
  elemental recipes. The production contract now preserves the exact level and altar-specific XP,
  consumes equal pure essence and opposite runes even on failed rolls, consumes one opposing
  talisman per interaction unless Magic Imbue is active, and rolls the base 50% success chance
  independently for every essence.
- Added player-global Binding necklace state through cache varp 487. A worn necklace guarantees
  elemental success for sixteen altar interactions, held Check reports the remaining state through
  the cache-correct third inventory operation, disintegration clears the state, and destroying a
  necklace resets the next necklace to sixteen uses. Explicitly using a talisman still consumes it
  while Magic Imbue is active.
- Added the exceptional level-90 Aether route at the Cosmic Altar. It atomically requires active
  Magic Imbue, a worn Binding necklace, soul runes, pure or Daeyalt essence and Aether catalysts;
  each output consumes one catalyst and awards 20 Runecraft XP. Raiments of the Eye, Scarred
  Extract modifiers, Guardians of the Rift access and exact remaining client audiovisual behavior
  stay explicitly open instead of being approximated.
- Eight injected integration scenarios cover all twelve elemental directions, exact cache
  fingerprints and interaction operations, independent success/failure boundaries, Magic Imbue
  bypass and explicit-talisman behavior, atomic rejection, strict Aether requirements and the
  full sixteen-use necklace lifecycle including real inventory-operation dispatch.
- Registered the combination core as integration-tested and its remaining modifiers/client parity
  as researched. Also recorded—but did not activate—the requested post-baseline cache 238.1
  migration and selectable League-world profile. Cache 233.4 remains the immutable regular-game
  baseline; the future League profile must isolate progression, rates, relics, tasks, area locks,
  seasonal interfaces/content and rewards without leaking state into normal worlds.
- The manifest now contains 508 feature groups: 50 discovered, 121 researched, 1 unit-tested, 335
  integration-tested and 1 verified. These counts measure tracked contract maturity, not percent of
  the whole game; many un-inventoried production features remain outside the denominator.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 3
  minutes 31 seconds: 886 actionable tasks, with 501 executed, 7 restored from cache and 378
  up-to-date. Generated reports contain 1,706 unit tests across 123 classes and 2,041 integration
  tests across 254 classes, with zero skips, failures or errors.
- Restarted only the workspace rsmod server. Process 68163 loaded 59 plugin modules and 240 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. No RuneLite process was
  opened, closed or changed; the pre-existing installed-app process remained untouched.

## 2026-09-13 — Ordinary true rune-altars and standard modifiers

- Pinned final pre-cutoff Runecraft revision 14997191 and reconciled it with cache locations
  34760–34772 plus true Blood altar 43479. The new production table covers all fourteen ordinary
  Air-through-Wrath true altars, exact levels, per-essence XP, rune/pure/Daeyalt eligibility,
  Daeyalt's 50% XP modifier and every visible-level multiple-rune threshold.
- Added atomic operation-one crafting and exact members plus Astral/Death/Blood/Wrath quest gates.
  Client-script 4024 was decoded against the accepted vanilla cache and a durable integration test
  now pins its four quest-row routes to varbits 2448/1103/7255/6104; the server uses the exact
  190/60/138/215 completion values from the quest database rows.
- Implemented matching Warped/Twisted/Mangled/Scarred Extracts with base-level-60 and started-DT2
  eligibility, partial-stack consumption, additive +250/+60 output and no bonus XP. Implemented all
  Raiments hat/torso/leg colour variants and boots using the exact ten-rune block/remainder roll,
  four-piece extra 20% and pre-extract ordering.
- Kept Sunfire, active Blood essence, dense-essence Blood/Soul, golem cores, pouches, Abyss,
  Ourania, Guardians, diaries and client presentation open. In particular, dated 10-output core
  evidence conflicts with a post-cutoff 11-output correction, so no value was guessed.
- Six cache-aware ordinary-altar scenarios plus the existing eight combination scenarios pass,
  along with the semantic cache inspector and formatting checks.
- The manifest now contains 510 feature groups: 50 discovered, 122 researched, 1 unit-tested, 336
  integration-tested and 1 verified. These are tracked maturity counts, not a whole-game
  completion percentage.
- The complete serialized `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 8 minutes 15 seconds: 887 actionable tasks, with 72 executed, 6 restored from cache and
  809 up-to-date. Generated reports contain 1,706 unit tests across 123 classes and 2,048
  integration tests across 255 classes, with zero skips, failures or errors.
- Restarted only the verified workspace rsmod server. Process 93985 loaded 59 plugin modules and
  240 scripts, bound port 43594 and reported revision 233 with Desktop-client support. No RuneLite
  process was opened, closed or changed.

## 2026-09-13 — Active Blood essence true-altar lifecycle

- Reconciled Jagex's official Nex-reward clarification and the cutoff Blood essence article with
  cache objects 26390/26392 and activation sound 146. The stable contract is a members-only
  inactive stack that activates one untradeable item, permits only one active essence, stores
  1,000 successful-extra charges, rolls an independent 50% chance per eligible essence, grants no
  bonus XP and disappears after the final successful extra rune.
- Added item-local charge state in server varobject 65523 and extended the enriched-variable
  manifest contract to pin its ten-bit definition. Registered real held Activate and Check
  operations, atomic inactive-stack splitting, inventory-and-bank uniqueness, activation sound,
  exact Check and duplicate-active text, and capacity-safe rejection.
- Integrated the active item into the true Blood altar transaction for pure and Daeyalt essence.
  Successful rolls alone consume charges; output remains additive after ordinary multiplication,
  Raiments and Scarred Extract calculation; XP is unchanged; and the active item is rewritten or
  destroyed atomically with the altar result.
- Six injected Blood-essence scenarios cover cache identity and operations, real held-operation
  dispatch, membership, capacity, inventory/bank uniqueness, deterministic success and failure,
  charge preservation and exhaustion, Scarred Extract ordering, XP invariance and non-Blood
  exclusion. Dense/Kourend dark fragments, Guardians guardian essence, Ourania, Ultimate Ironman
  death and matching-client presentation remain explicitly assigned to manual case 130.
- The manifest now contains 511 feature groups: 50 discovered, 122 researched, 1 unit-tested, 337
  integration-tested and 1 verified. These maturity counts cover only inventoried slices and do
  not represent whole-game completion.
- The complete serialized `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate
  passed in 7 minutes 27 seconds: 887 actionable tasks, with 124 executed, 6 restored from cache
  and 757 up-to-date. Generated reports contain 1,706 unit tests across 123 classes and 2,054
  integration tests across 256 classes, with zero skips, failures or errors.
- Restarted only the verified workspace rsmod server. Process 26000 loaded 59 plugin modules and
  240 scripts, bound port 43594 and reported revision 233 with Desktop-client support. No RuneLite
  process was opened, closed or changed.

## 2026-09-13 — Essence-pouch storage and degradation core

- Reconciled the cutoff Essence-pouch/Runecraft evidence, official Guardians/Run Energy/Poll 82
  updates, cache client scripts 3197/3198/3670/3671/3695/4592 and native variable layouts. The
  production contract covers all five tiers, four accepted essence types, unboostable base-level
  gates, exact capacities, every degradation/destruction threshold and the 8/16/27/40 Colossal
  level scaling.
- Added native transmitted count/type state for every pouch and native ordinary/Colossal wear
  counters. Ordinary tiers add a uniform random zero through three per stored essence; Colossal
  deterministically adds one, resolving the official 320-essence boundary without importing later
  gameplay. Both Runecraft capes, the standard Max cape and Max-cape variants suppress new wear
  without repairing existing damage.
- Registered real Fill, Empty and Check operations for all nine normal/degraded cache objects,
  dynamic bankside Fill/Empty using the native amount variables, partial emptying, capacity-shrink
  excess return, normal-to-degraded transitions, final destruction and drop-state clearing. Added
  a reusable repair seam that restores held and banked pouch variants without deleting contents.
- Ten injected integration scenarios cover cache objects/operations/variables, all accepted
  essence types, membership/base-level/type gates, all capacity endpoints, ordinary and Colossal
  wear, cape protection, partial inventory behavior, bankside dispatch, repair, drop clearing and
  destruction. Acquisition/Colossal creation and mutual exclusion, login cleanup, Dark mage,
  Astral Contact and Cordelia wiring, Guardians' Redwood-lantern protection, death behavior and
  exact client presentation remain explicitly assigned to manual case 131.
- The manifest now contains 512 feature groups: 50 discovered, 122 researched, 1 unit-tested, 338
  integration-tested and 1 verified. These maturity counts cover only inventoried slices and do
  not represent whole-game completion.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 3
  minutes 1 second: 886 actionable tasks, with 157 executed, 6 restored from cache and 723
  up-to-date. Generated reports contain 1,706 unit tests across 123 classes and 2,064 integration
  tests across 257 classes, with zero skips, failures or errors.
- No RuneLite process was opened, closed or changed. Only source files inside the workspace
  checkout were inspected read-only for the matching pouch-overlay scaling contract.
- Restarted only the verified workspace rsmod server. Process 74261 loaded 59 plugin modules and
  240 scripts, bound port 43594 and reported revision 233 with Desktop-client support.

## 2026-09-13 — Kourend dark-essence and false-altar core

- Reconciled the cutoff Dark essence, false Blood/Soul altar, Scar Essence Mine, Raiments, Blood
  essence and Kourend elite-diary records with cache locations 27978–27980, objects
  565/566/7938/13445/13446, sequence 7202 and varbit 7928.
- Added exact three-tick Dark Altar veneration for every carried dense block at once, preserving
  slots, awarding 2.5 Runecraft XP and draining one bounded Prayer point per block while remaining
  usable at zero Prayer. Added Crafting-38 chisel conversion of one selected dark block into four
  counted fragments in that same slot for 8 XP, including when the inventory is full.
- Added the members-only false Blood/Soul altar paths at Runecraft 77/90 for 23.8/29.7 XP per
  fragment. Scarred Extracts, all Raiments variants, Kourend elite's rounded-down 10% Blood reward
  and active Blood essence compose additively and never increase XP; Blood essence remains inert at
  Soul.
- Extended the transaction engine with an explicit counted-non-stackable insertion/deletion mode.
  This preserves cache fragment 7938 as non-stackable while safely representing four fragments per
  source slot, partial cross-slot consumption and atomic insufficient-count rollback. Three focused
  engine tests and eight injected Kourend scenarios cover the full implemented contract.
- Recorded dense-runestone mining, medium-diary duplication, automatic chiseling, diary task
  credit, Soul-rift/Abyss access, dark-block tablet/nullifier uses, death policy and exact client
  presentation as manual case 132 rather than inferring unsourced behavior.
- Registered one integration-tested group, taking the manifest to 513 groups: 50 discovered, 122
  researched, 1 unit-tested, 339 integration-tested and 1 verified. These are tracked evidence
  maturity counts, not whole-game completion.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in 2
  minutes 59 seconds: 886 actionable tasks, with 149 executed, 6 restored from cache and 731
  up-to-date. Generated reports contain 1,709 unit tests across 123 classes and 2,072 integration
  tests across 258 classes, with zero skips, failures or errors.
- Restarted only the workspace rsmod server. Process 5852 loaded 59 plugin modules and 240 scripts,
  bound port 43594 and reported revision 233 with Desktop-client support. No RuneLite process was
  opened, closed or changed.

## 2026-09-13 — Dense-runestone Mining core

- Reconciled cutoff Dense runestone revision 14908570, Dense essence mine revision 14774197 and
  Runecraft training revision 14997349 with cache locations 8975/8976/8981/10796, block 13445,
  chisel 1755, personal morph varbits 4927/4928 and medium-diary varbit 7926.
- Added members-only carried-pickaxe/chisel and visible Mining/Crafting-38 gates, the fixed
  nine-tick block cadence, exact 12 Mining and 8 Crafting XP, pickaxe-tier independence, no crystal
  charge mutation and automatic continuation with silent last-slot interruption.
- Added exact 210/230 level-99-scaled persistence, personal alternating pillar depletion and the
  medium diary's independent one-in-20 second block without extra XP. Mining cape, Varrock armour
  and rock-golem paths remain deliberately excluded.
- Added fourteen `DenseRunestoneTest` scenarios covering cache contracts, requirements, visible
  boosts, timing, XP, diary output, equipment exclusions, crystal preservation, interruption,
  capacity, both depletion directions, player isolation and all persistence plateaus. The complete
  Mining integration module passes.
- Preserved the historical evidence conflict instead of inventing behavior: the Wiki cites an
  unspecified level-scaled 3-to-2-tick sub-delay but also records observed fixed nine-tick results.
  Re-entry randomisation awaits exact wiring to the three surrounding Agility transports. These
  and literal client presentation remain manual case 132.
- Re-ran the complete parity gate after the slice: `spotlessCheck`, `validateCache233Parity`, all
  unit tests and all integration tests passed in 2m56s across 886 Gradle tasks. The reports contain
  1,709 unit tests in 123 classes and 2,086 integration tests in 259 classes, with zero failures,
  errors or skips. The manifest now tracks 514 rows: 50 discovered, 122 researched, one unit-tested,
  340 integration-tested and one client-verified.
- Restarted only the workspace rsmod server after verification. Process 47790 loaded 59 plugin
  modules and 240 scripts, bound port 43594 and reported revision 233 with Desktop-client support.
  RuneLite was not opened or changed.

## 2026-09-13 — Paladin, Gnome and Hero clue-pickpocket tertiaries

- Reconciled final pre-cutoff Paladin 14931379, Gnome 14996384 and Hero 14940550 revisions with
  the shared clue-inventory contract and cache-233.4 object data. Successful pickpockets now roll
  hard clues at 1/1,000, medium clues at 1/150 and elite clues at 1/1,400; completed matching
  Combat Achievement tiers reduce those exact denominators to 950, 142 and 1,330.
- Added immutable initial-step inventories for all 165 medium, 185 hard and 135 elite cache objects.
  Every accepted object has the exact tier name plus `Read` and `Check steps` operations, and each
  sorted ID population is protected by count, endpoint and SHA-256 checks.
- Integrated pre-X-Marks ownership suppression, post-X-Marks scroll boxes, shared two-to-five
  capacity and the exact capped warning. Loose clues remain blocked when a full inventory has no
  slot, existing scroll boxes can stack, ordinary loot and XP remain committed, and Rogue equipment
  never doubles a clue.
- Added eight focused scenarios to `PickpocketScriptTest`, taking that suite to 76 passing cases.
  They pin every pool contract, all six denominators, a successful award from each NPC family,
  ownership roll suppression, box conversion/capacity, full-inventory routing and Rogue
  non-doubling.
- Promoted all three existing manifest rows from researched to integration-tested. The 514 tracked
  groups are now 50 discovered, 119 researched, one unit-tested, 343 integration-tested and one
  client-verified. Thus 345/514, or 67.12%, of the currently inventoried rows have automated or
  client verification; this is a tracked-backlog coverage metric, not whole-game completion.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m02s across 886 Gradle tasks. Reports contain 1,709 unit tests in 123 classes and 2,094
  integration tests in 259 classes, with zero failures, errors or skips.
- Restarted only the verified workspace rsmod server. Process 87500 loaded 59 plugin modules and
  240 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened or changed.

## 2026-09-13 — Vyre blood-shard tertiary

- Reconciled final pre-cutoff Vyre revision 14907001 with Blood shard revision 14965265 and cache
  object 24777. Every successful Vyre pickpocket now has a separate one-in-5,000 shard roll after
  the ordinary reward commits; the result does not replace the 132-weight main table or add XP.
- Added a reusable typed pickpocket-tertiary definition. The shard uses the pickpocket's Rogue
  double-loot result, so the full set guarantees two. The complete one- or two-shard quantity is
  added atomically; when it cannot fit, every shard is emitted as a private owner-only floor object
  while the ordinary reward and single 306.9 XP award remain committed.
- Added three integrated scenarios for a deterministic miss and hit, full-Rogue doubling, unchanged
  ordinary loot and XP, and the one-free-slot atomic owner-drop boundary. The focused Thieving suite
  now passes all 79 scenarios.
- Promoted the existing blood-shard row from researched to integration-tested. The 514 tracked
  groups are now 50 discovered, 118 researched, one unit-tested, 344 integration-tested and one
  client-verified. Thus 346/514, or 67.32%, of the inventoried rows have automated or client
  verification; this is backlog coverage, not whole-game completion.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m05s across 886 Gradle tasks: 71 executed, six restored from cache and 809 up-to-date. Reports
  contain 1,709 unit tests in 123 classes and 2,097 integration tests in 259 classes, with zero
  failures, errors or skips.
- Matching-client checks still cover partial-Rogue coupling, literal floor lifetime/visibility,
  packet presentation and the Vyre six-second-versus-three-second source conflict. The Wiki does
  not expose Jagex's internal RNG-call position, so it is not presented as an observable contract.
- Restarted only the verified workspace rsmod server. Process 7672 loaded 59 plugin modules and 240
  scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was not
  opened or changed.

## 2026-09-13 — Wealthy Citizen reward-boundary correction

- Re-read final pre-cutoff Wealthy citizen revision 14988663 and reconciled it with the dated coin-
  pouch page, the contemporary Jagex Varlamore: Part One overview and cache 233.4. The official
  overview resolves the earlier wording conflict: the direct 79/85 coin branch yields one pouch
  worth 85 coins, while keys and clues remain separate 5/85 and 1/85 outcomes.
- Pinned two omitted clue rules from the cutoff page: completed Easy Combat Achievements improve
  the easy-clue rate to 1/80, and completed X Marks the Spot converts it to an easy scroll box.
  Keys and clues are discardable on insufficient inventory capacity without cancelling the
  successful pickpocket, and full Rogue equipment doubles keys but not clues.
- Found and typed the previously omitted cache contract that prevents safe production dispatch:
  `varlamore_thieving_house_key_drystreak` is varbit 9942, an eight-bit 2–9 slice of permanent varp
  4175. Public cutoff sources do not specify its increment, threshold, guarantee/chance adjustment
  or reset behavior, so a memoryless 5/85 key implementation would be knowingly incomplete.
- Expanded manual case 101 to capture the dry-streak state machine across coin, key, clue, failure
  and capacity-discard outcomes, plus Easy-CA and X Marks behavior. The Street Urchin distraction
  lifecycle remains a separate timing boundary.
- Extended the focused cache test to pin the stackable key object and the exact dry-streak varbit
  identity, backing varp and bit range. The Thieving configuration test passes; no runtime handler
  was registered and the manifest remains 514 rows with the Wealthy Citizen boundary researched.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  three minutes across 886 Gradle tasks: 71 executed, six restored from cache and 809 up-to-date.
  Reports contain 1,709 unit tests and 2,097 integration tests, with zero failures, errors or skips.
- The already-running workspace rsmod process 7672 remains bound to port 43594 from the expected
  rsmod directory. No runtime behavior changed, so it was not restarted; RuneLite was not opened
  or changed.

## 2026-09-13 — Ferocious barbarian spirit main drop table

- Reconciled the final pre-cutoff Ferocious barbarian spirit revision 14992305 with the exact
  pre-cutoff Rare drop table revision 14991965, GemDropTable documentation revision 14991640 and
  cache 233.4. Registered the complete Jagex-supplied 128-weight main table for NPC 5566, including
  its double standard-herb roll, prayer and antifire mixes, other potion mixes, bars, logs, sharks,
  big bones and all weapon, rune, ammunition and seed outcomes.
- Added a reusable typed standard gem-table roll with its ordinary 128-weight table, Ring of Wealth
  65-weight table, nested 128/15-weight mega-rare variants and pre-Legends nature-talisman branch.
  Every uncharged, charged and imbued Ring of Wealth identity present in cache 233.4 is recognized.
- Added five integrated scenarios that pin all 128 main positions, both herb rolls, exact object and
  amount boundaries, note certificates and stackability, ordinary and wealth gem/mega tables, and
  the Legends' Quest boundary. Owner-only combat attribution, Barbarian training completion and
  the elite-clue challenge pair remain in the separate encounter-completion row rather than being
  represented as finished.
- Split the former combined manifest entry into a main-drop row and an encounter-completion row.
  The 515 tracked groups are now 50 discovered, 118 researched, one unit-tested, 345
  integration-tested and one client-verified. Thus 347/515, or 67.38%, of the inventoried rows have
  automated or client verification; this is backlog coverage, not whole-game completion.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m11s across 886 Gradle tasks. Reports contain 1,709 unit tests in 123 classes and 2,102
  integration tests in 260 classes, with zero failures, errors or skips.
- Restarted only the verified workspace rsmod server. Process 55933 loaded 59 plugin modules and
  241 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened or changed.

## 2026-09-13 — Ordinary and Zanaris Sheep shearing correction

- Reconciled final pre-cutoff ordinary Sheep revision 14970655, Zanaris Sheep revision 14986149
  and transcript revision 14350218 with cache 233.4. The eleven ordinary unsheared identities now
  retain exact one-to-one sheared forms, guaranteed wool and 50-cycle regrowth. Both Zanaris
  colours remain Talk-to-only, accept shears through item use, transform to dark-grey sheared NPC
  5845 rather than unused 5846 and use 100-cycle regrowth.
- Preserved the legacy Zanaris one-in-four escape branch and exact feedback. Its explicit 25%
  documentation was added to the Wiki after the cutoff, so the ledger labels it post-cutoff
  evidence describing an old exception and leaves matching-client confirmation in manual case
  133. Ordinary sheep remain guaranteed because the cutoff page directly records their 2019
  success-rate change.
- Added five integrated scenarios covering the complete 11+2 identity and transformation contract,
  ordinary missing-tool and successful first-option paths, Zanaris Talk-to exclusion and
  deterministic failure/success paths, exact 50/100 lifecycle durations and private floor delivery
  from a full inventory. The focused suite passes all five scenarios.
- Split the work into a production shearing row and a separate researched dialogue/client row. The
  517 tracked groups are now 50 discovered, 119 researched, one unit-tested, 346
  integration-tested and one client-verified. Thus 348/517, or 67.31%, have automated or client
  verification. This is tracked-backlog coverage, not a claim that 67% of the whole game is done;
  the broader regular-game estimate remains approximately 25–30%.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m25s across 886 Gradle tasks: 503 executed, seven restored from cache and 376 up-to-date.
  Reports contain 1,709 unit tests in 123 classes and 2,107 integration tests in 261 classes, with
  zero failures, errors or skips.
- Restarted only the verified workspace rsmod server. Process 83540 loaded 59 plugin modules and
  241 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened or changed.

## 2026-09-13 — Loaded Woodsman tutor and Woodcutting Skillcape sale

- Audited the enriched cache-233.4 Lumbridge spawn inventory before selecting the slice. Every
  loaded attackable NPC family already has a production drop registration; Woodsman tutor 3226
  instead exposed a live level-99 `TODO` crash and several dated-transcript gaps.
- Pinned final pre-cutoff transcript revision 14815453. The tutor now separates the exact 1–18,
  19, 20–28, 29–98 and 99+ menu shapes, restores the conditional bronze-axe grants, both missing
  fletching lines, the advanced Magic/Hollow-tree submenu and the Jewellery wording. Operation one
  is a normal production Talk-to handler rather than an unimplemented-operation fallback.
- Implemented the members-only mastery offer and exact decline, insufficient-money and success
  branches. The 99,000-coin transaction preflights and commits atomically, supplies cache objects
  9807/9808 plus hood 9809, selects the trimmed cape only when another ordinary skill is mastered
  and plays cache jingle 231. The full-inventory response is corroborated by later transcript
  revision 15129091 and remains a specifically labelled post-cutoff observation of legacy behavior.
- Added seven cache-aware integration scenarios covering identities and constants, membership,
  level, money and capacity gates, rollback, first and second mastery, the F2P/member dialogue
  paths and both level-19 and advanced tree menus. The focused suite passes with zero failures.
- The parity inventory now contains 518 rows: 50 discovered, 119 researched, one unit-tested, 347
  integration-tested and one client-verified. Automated-or-client backlog coverage is 349/518, or
  67.37%; this is evidence-row coverage, not whole-game completion.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m14s across 888 Gradle tasks: 72 executed, six restored from cache and 810 up-to-date. Reports
  contain 1,709 unit tests in 123 classes and 2,114 integration tests in 262 classes, with zero
  failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 5008 loaded 59 plugin modules and
  241 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened or changed.

## 2026-09-13 — Loaded Bob Barrows and Moon-equipment repair service

- Pinned the final pre-cutoff Bob revision 14848825, Bob transcript revision 14379729, Barrows-
  equipment revision 14838793 and Moon-equipment revision 14840313 against loaded cache NPC 10619
  and its `Talk-to`, `Trade`, `Repair` operation contract.
- Replaced Bob's unconditional repair refusal with real right-click repair-all and held-item routes.
  Production covers all 24 original Barrows pieces, all four Ahrim ornament variants and all nine
  Moon armour pieces while explicitly excluding the three Moon weapons. Quotes use exact packed
  points when present and safe named-stage boundaries otherwise; fully broken objects ignore stray
  packed state.
- Implemented the exact 60/90/80/100 Barrows and 500 Moon coins-per-missing-point formulas. Every
  quote is revalidated before one atomic inventory transaction removes the coins and replaces each
  selected item in its original slot, so insufficient payment or stale inventory cannot partially
  mutate the player.
- Added nine cache-aware `BobRepairTest` scenarios covering identities, a 330,000-coin fully broken
  Barrows set, exact partial points, 1,500,000-coin Moon pieces, Moon-weapon exclusion, insufficient
  funds, stale quotes, right-click repair-all and both held-item transcript branches. The focused
  suite passes all nine scenarios.
- Combat-time degradation, named-stage transitions, `Check`, death/drop behavior, other repair NPCs
  and the POH armour stand remain explicitly open in manual case 135 rather than being claimed by
  the transaction slice.
- The parity inventory now contains 519 rows: 50 discovered, 119 researched, one unit-tested, 348
  integration-tested and one client-verified. Automated-or-client backlog coverage is 350/519, or
  67.44%; this remains evidence-row coverage, while honest regular-game completion remains about
  25–30% and the complete 238.1-plus-Leagues vision about 15–20%.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m28s across 888 Gradle tasks: 158 executed, six restored from cache and 724 up-to-date. Reports
  contain 1,709 unit tests in 123 classes and 2,123 integration tests in 263 classes, with zero
  failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 37395 loaded 59 plugin modules and
  241 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened or changed.

## 2026-09-13 — Shared Barrows and Moon-armour combat degradation core

- Reconciled the final pre-cutoff Barrows-equipment revision 14838793 and Moon-equipment revision
  14840313 with cache 233.4. The shared catalogue covers all 24 original Barrows pieces, four Ahrim
  ornament pieces and nine Moon armour pieces across 195 cache states; the three Moon weapons are
  explicitly outside the degradable family.
- Added a general validated player-combat engagement event. Player melee, ranged, damaging magic,
  splashes and successful non-damaging magic publish it for the attacker and any player target when
  combat is queued; NPC melee publishes it for the player when the attack launches. This keeps
  degradation independent from projectile delay and includes zero-damage/protection outcomes.
- Implemented exact per-item 1,000-point Barrows and 3,000-point Moon state. All eligible worn pieces
  lose one point on combat entry and another after each 90-tick interval that still satisfies the
  server's active-combat window. Barrows transitions at 750, 500, 250 and zero; Moon armour moves
  through used to broken. Reaching zero replaces the worn object in place, removes bonuses through
  the broken cache definition and rebuilds appearance without forcibly unequipping it.
- Registered every cache-backed held and worn `Check` operation. Added a general post-warning drop
  preparation seam so confirmed ordinary Barrows/Moon drops become broken while a cancelled warning
  cannot mutate inventory; ground objects preserve their server-side item state.
- Refactored Bob's repair service to consume the same catalogue, eliminating duplicate object graphs
  and ensuring repair quotes and degradation use one point model. Six focused degradation scenarios
  plus all nine Bob repair scenarios pass, covering all identities, entry/cadence/re-entry, named and
  broken boundaries, standard attack publication and both equipment families' ordinary drops.
- Death, PvP ownership/breakage, trade eligibility, bank/relog integration, other repairers, the POH
  armour stand and matching-client presentation remain explicit manual cases 135–136.
- The parity inventory now contains 520 rows: 50 discovered, 119 researched, one unit-tested, 349
  integration-tested and one client-verified. Automated-or-client backlog coverage is 351/520, or
  67.50%; this remains evidence-row coverage, while honest regular-game completion remains about
  25–30% and the complete 238.1-plus-Leagues vision about 15–20%.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m35s across 894 Gradle tasks. Reports contain 1,709 unit tests in 123 classes and 2,129
  integration tests in 264 classes, with zero failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 65289 loaded 60 plugin modules and
  242 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened or changed.

## 2026-09-13 — Degradable-equipment persistence and dynamic tradeability

- Audited the production transaction and account pipelines rather than adding a parallel state
  store. Exact per-object `vars` already flow through inventory transfers and the persistent
  `inventory_objs.vars` column; a new bank integration scenario proves two identical Barrows
  objects at 733 and 612 points remain independent through bulk deposit and individual withdrawal.
- Added an in-memory SQLite account regression that saves those two states with
  `CharacterInventoryPipeline`, reloads them through the normal metadata segment and applies them to
  a fresh player. Both slots and exact values survive the complete database round trip.
- Cache trade flags do not uniformly encode the Wiki's repaired-or-broken-only rule. Added a shared,
  monotonic `ObjTradeEvents.Query` denial seam consumed by protected-access price/trade checks and
  standard NPC shops. The degradation script denies every intermediate Barrows and Moon armour
  point while preserving cache eligibility at full and zero; no listener can turn a cache-denied
  object into a tradeable one.
- Seven degradation scenarios, the account round trip, the bank suite and shop unit suite pass.
  Actual player-to-player trading, the Grand Exchange and the full player-death system remain open
  rather than being implied by the shared eligibility rule.
- The parity totals remain 351/520 (67.50%) because this strengthens the already-counted degradation
  row instead of manufacturing a second feature row for cross-cutting persistence evidence.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m33s across 896 Gradle tasks. Reports contain 1,710 unit tests in 124 classes and 2,131
  integration tests in 264 classes, with zero failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 79753 loaded 60 plugin modules and
  242 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened or changed.

## 2026-09-13 — Hans account-age and time-played dialogue

- Replaced loaded Hans 3105's hard-coded zero-value response with a shared formatter used by both
  dialogue option four and the cache-native third operation `Age`.
- Promoted the already-loaded `characters.created_at` account value onto the live `Player` during
  normal metadata application. No schema or parallel persistence store was added. The existing
  permanent playtime varp remains the authoritative online-time counter.
- Added deterministic conversion coverage for days, hours and minutes, future-value clamping, an
  account metadata application regression and an end-to-end NPC operation-three dialogue scenario.
- The focused account unit suite and Lumbridge integration suite pass. The parity inventory now has
  521 rows, with 352 automated-or-client rows (67.56%).
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m39s across 896 Gradle tasks. Reports contain 1,711 unit tests in 124 classes and 2,133
  integration tests in 265 classes, with zero failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 87788 loaded 60 plugin modules and
  242 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened or changed.

## 2026-09-13 — River Lum canoe routing and axe-selection core

- Pinned the final pre-cutoff Canoe revision 14995077 and historical transcript revision 14836756.
  The implementation contract now records all four canoe levels and XP awards, one-use sinking,
  the five build stations, the Wilderness pond and Ferox Enclave's asymmetric Waka-entry but
  level-12 Log-exit behavior.
- Extracted and exhaustively tested the destination graph for every station and canoe tier. The
  existing continuous cutting loop remains source-aligned: success depends on Woodcutting level
  and axe tier rather than the canoe being shaped, and it repeats until successful.
- Corrected canoe axe selection to use full tool priority rather than only level requirements, so
  iron beats bronze at their shared level-one requirement. Members axes are now unavailable on F2P
  worlds, and both active and inactive Crystal axes require Song of the Elves state 194 before they
  can be selected.
- Reconciled Crystal and Infernal charge semantics with their final pre-cutoff definitions. Canoe
  cutting and shaping produce no inventory item and perform no Infernal log-burn success, so this
  activity consumes neither charge; the stale charge TODOs were replaced with that bounded source
  derivation.
- Six cache-aware integration scenarios cover exact configuration, exhaustive enum identity,
  the complete route graph and F2P, membership, quest-state, worn-versus-carried and same-level axe
  priority gates. Client timing, interruption, animations, cutscenes and concurrent-station behavior
  remain explicit in manual case 138 rather than being claimed by the automated slice.
- The parity inventory now contains 522 rows: 50 discovered, 119 researched, one unit-tested, 351
  integration-tested and one client-verified. Automated-or-client backlog coverage is 353/522, or
  67.62%; this is evidence-row coverage, while honest regular-game completion remains about 25–30%
  and the complete 238.1-plus-Leagues vision about 15–20%.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m28s across 896 Gradle tasks: 72 executed, six restored from cache and 818 up-to-date. Reports
  contain 1,711 unit tests in 124 classes and 2,136 integration tests in 265 classes, with zero
  failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 652 loaded 60 plugin modules and 242
  scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was not
  opened, changed or otherwise interacted with.

## 2026-09-13 — Dairy cow Cowbell theft core

- Pinned final pre-cutoff Cowbells revision 14892118, historical Cold War progress varbit revision
  14012126 and cache 233.4 location/object/varbit identities. The exact success chart maps to the
  existing 128-to-200 level-99 skilling curve: 54.296875% at level 15, 62.5% at 43 and 78.515625%
  at 99.
- Replaced Dairy cow 8689's Cold War TODO with a production second-operation resolver. It now
  enforces members-world, quest-started, current Thieving-15, capacity and action-delay gates;
  success atomically adds Cowbells 10593, grants 16 XP and emits the transcript line.
- Failure emits the documented player lines and cow `MOO`, then reuses the shared standard
  Thieving stun state, animation and spot animation. One damage and eight blocked cycles remain
  explicitly provisional because the dated Wiki evidence does not quantify either value; they are
  retained for matching-client case 139 rather than presented as proven parity.
- Four integration scenarios pin all cache identities and operation slots, every eligibility
  boundary, the exact curve endpoints, success reward/XP and bounded failure state. The generic-NPC
  integration module initially contained 77 passing scenarios.
- Closed Cowbells inventory operation one as a fifth focused scenario. Cache varbit 3306 gates the
  penguin disguise, cache sequence 5673 supplies the dedicated animation and the item is preserved.
  The historically corroborated outside-suit line is provisional only for chat type and packet
  presentation. Zanaris Dairy cow 12111's conversation handout, the complete Cold War quest
  lifecycle and exact client choreography remain open.
- The parity inventory now contains 523 rows: 50 discovered, 119 researched, one unit-tested, 352
  integration-tested and one client-verified. Automated-or-client backlog coverage is 354/523, or
  67.69%; this is evidence-row coverage, while honest regular-game completion remains about 25–30%
  and the complete 238.1-plus-Leagues vision about 15–20%.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m25s across 896 Gradle tasks: 72 executed, six restored from cache and 818 up-to-date. Reports
  contain 1,711 unit tests in 124 classes and 2,141 integration tests in 266 classes, with zero
  failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 43326 loaded 60 plugin modules and
  242 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened, changed or otherwise interacted with.

## 2026-09-13 — Penguin-bongo Crafting and Play core

- Researched the Cold War quick guide, Crafting experience table and quest transcript together.
  The production contract is one leather plus one mahogany plank, members content, unboosted
  Crafting 30, 25 XP and the literal result line. The quick guide also groups Cowbells and bongos
  under the same Penguin-suit-only Play restriction.
- Added a focused `content/skills/crafting/penguin-bongos` plugin instead of attaching Crafting
  behavior to the Dairy cow script. Both held-item directions route to one atomic transaction, so a
  full inventory safely consumes two inputs and creates the one output without a capacity precheck.
  Membership, quest and base-level failures leave both ingredients untouched; a visible boost
  cannot bypass the unboosted requirement.
- Compared the adjacent cache sequences. Inventory Play uses short player sequence 5674
  `peng_player_bongo`; longer sequence 5675 `peng_bongo_ready` is recorded but deliberately not
  assigned to the Ping/Pong performance without a matching-client trace. The item is never consumed
  by Play. The exact earliest Cold War state, outside-suit message presentation, audiovisuals,
  action locking and complete quest hand-in/cutscene remain manual case 140.
- Five cache-aware integration scenarios cover identities and operations, every sourced gate,
  boosted-level rejection, full-inventory atomicity, exact XP/result text, reverse-use production
  dispatch and Play state/item preservation.
- The parity inventory now contains 524 rows: 50 discovered, 119 researched, one unit-tested, 353
  integration-tested and one verified. Automated-or-client backlog coverage is 355/524, or 67.75%;
  this is evidence-row coverage, while honest regular-game completion remains about 25–30% and the
  complete 238.1-plus-Leagues vision about 15–20%.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m46s across 902 Gradle tasks: 512 executed, seven restored from cache and 383 up-to-date. Reports
  contain 1,711 unit tests in 124 classes and 2,146 integration tests in 267 classes, with zero
  failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 55413 loaded 60 plugin modules and
  243 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened, changed or otherwise interacted with.

## 2026-09-13 — Bird-house shell Crafting core

- Researched the Bird house trapping recipe table and Redwood bird house timing description, then
  bounded the slice to portable shell creation. All nine regular-through-redwood recipes consume
  one matching log plus one clockwork, require members, a carried chisel and an ordinary or Imcando
  hammer, use boosted Crafting levels 5/15/25/35/45/50/60/75/90 and award exact XP
  15/20/25/30/35/40/45/50/55. Trap placement and Hunter rewards remain explicitly separate.
- Added the focused `content/skills/crafting/birdhouses` plugin and queue symbol 111. Every log and
  clockwork pair routes in either held-item direction, selects cache sequence 7057 for an ordinary
  hammer or 8916 for either carried/worn Imcando form, and performs an atomic two-input/one-output
  exchange that succeeds even when the inventory starts full.
- Implemented the sourced two-tick first result and three-tick Make-All gap. The repeat queue uses
  rsmod's one-tick in-handler rescheduling compensation, matching the established Crafting queue
  convention. Missing levels, membership, tools or materials never remove inputs or award XP, and
  another Crafting action cancels the batch safely.
- Five cache-aware integration scenarios cover every recipe identity, exact level/XP contract,
  membership and tool gates, both item orders, full-inventory replacement, worn off-hand Imcando
  behavior, both animations, first/repeat timing, exhaustion and cancellation.
- The parity inventory now contains 525 rows: 50 discovered, 119 researched, one unit-tested, 354
  integration-tested and one verified. Automated-or-client backlog coverage is 356/525, or 67.81%;
  this is evidence-row coverage, while honest regular-game completion remains about 25–30% and the
  complete 238.1-plus-Leagues vision about 15–20%.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m47s across 908 Gradle tasks: 515 executed, seven restored from cache and 386 up-to-date. Reports
  contain 1,711 unit tests in 124 classes and 2,151 integration tests in 268 classes, with zero
  failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 70029 loaded 60 plugin modules and
  244 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened, changed or otherwise interacted with.

## 2026-09-13 — All-form Fishstabber special-attack core

- Pinned the final pre-cutoff Special attacks, Dragon harpoon, Infernal harpoon and Crystal harpoon
  revisions, then reconciled them with cache 233.4 enum 906 and object, sequence and spot-animation
  identities. Fishstabber consumes the complete 100% special-energy bar, visibly boosts Fishing by
  three without replacing a higher existing boost, and emits `Here fishy fishies!`.
- Audited every cache form instead of testing only the three base harpoons. Production was missing
  five registrations: Trailblazer Reloaded Dragon harpoon, depleted base Infernal harpoon, both
  Reloaded Infernal forms and inactive Crystal harpoon. All 11 cache-233 forms now route through the
  shared stat-boost special implementation.
- Assigned cache sequences by family: 7393 for the base Dragon, 7394 for base Infernal charged and
  depleted, 11823 for original Trailblazer Dragon and Infernal forms, 11824 for their Reloaded
  equivalents, and 8333 for active and inactive Crystal forms. Dragon and Infernal forms use red
  spot animation 246; Crystal uses silver spot animation 1688.
- Added one exhaustive production-startup integration scenario covering all 11 object identities,
  cache energy configuration, exact boost preservation, phrase, sequence, spot animation and final
  energy state. Activation while an active Fishing loop is underway, precise client-side energy
  redraw timing and audiovisual interruption behavior remain explicit in manual case 72.
- The parity inventory now contains 526 rows: 50 discovered, 119 researched, one unit-tested, 355
  integration-tested and one client-verified. Automated-or-client backlog coverage is 357/526, or
  67.87%; this is evidence-row coverage, while honest regular-game completion remains about 25–30%
  and the complete 238.1-plus-Leagues vision about 15–20%.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m31s across 908 Gradle tasks: 75 executed, six restored from cache and 827 up-to-date. Reports
  contain 1,711 unit tests in 124 classes and 2,152 integration tests in 268 classes, with zero
  failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 82584 loaded 60 plugin modules and
  244 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened, changed or otherwise interacted with.

## 2026-09-13 — Bird-house placement and seeding core

- Pinned the exact cache-cutoff Bird house trapping revision 14991901 and
  reconciled them with cache 233.4 locations 30552–30568 and 31827–31841, display varps 1626–1629,
  portable bird-house objects and synth 2433. The authoritative suitable-seed table contains 41
  identities: 30 worth one unit and 11 worth two units.
- Added a focused `content/skills/hunter/birdhouses` plugin for all four Fossil Island sites and all
  nine regular-through-redwood tiers. Placement requires members, an exact empty site, one carried
  portable shell and the boosted Hunter thresholds 5/14/24/34/44/49/59/74/89; successful placement
  atomically consumes the shell and selects the tier's cache-native built state.
- Implemented partial and mixed seed loading through the cache's built/partial/active three-state
  chain. Low seeds contribute one of ten required units, high seeds contribute two, only the amount
  still needed is removed, and reaching ten units stores a durable absolute 3,000-second readiness
  deadline. All four site display varps and four server-only ready-at varps are independent.
- Six cache-aware integration scenarios cover exact site/tier identities, every placement gate,
  atomic full-inventory behavior, all 41 suitable seeds, low/high/mixed loading, partial-state
  preservation, per-site isolation, exact readiness boundaries and build/fill sound 2433. Harvest
  loot, Empty, Dismantle, Reset, automatic raw-material Build and exact matching-client choreography
  remain explicit follow-up work in manual case 142.
- Fixed a production-runtime collision with the existing Crafting project of the same terminal name.
  The Hunter module now has distinct Gradle coordinates, and a server-shared regression test proves
  `BirdhouseTrapModule` is present on the real production classpath instead of being silently
  substituted by `content/skills/crafting/birdhouses`.
- The parity inventory now contains 527 rows: 50 discovered, 119 researched, one unit-tested, 356
  integration-tested and one client-verified. Automated-or-client backlog coverage is 358/527, or
  67.93%; this is evidence-row coverage, while honest regular-game completion remains about 25–30%
  and the complete 238.1-plus-Leagues vision about 15–20%.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m39s across 916 Gradle tasks: 78 executed and 838 up-to-date. Reports contain 1,712 unit tests in
  125 classes and 2,158 integration tests in 269 classes, with zero failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 26452 loaded 61 plugin modules and
  245 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened, changed or otherwise interacted with.

## 2026-09-13 — Mature bird-house harvest core

- Corrected the Bird house trapping source pin to revision 14991901, the final Wiki revision at or
  before the cache-233 cutoff, and pinned success-chart module revision 14931997. Together they
  establish the exact tier XP, guaranteed rewards and rounded visible-Hunter seed-nest formula.
  The cutoff page itself calls the clue-nest selection incomplete, so production does not silently
  invent the ten-roll other-nest branch.
- Added ready-state `Interact` handling for all nine active bird-house locations. A valid mature
  harvest captures the visible Hunter level before XP, atomically returns the site and deadline to
  zero, and awards exact XP 280/420/560/700/820/960/1020/1140/1200.
- Mature rewards now include one returned clockwork, a uniformly selected 30/40/50/60 feathers,
  and at most one seed nest using rounded low-0/high-200 interpolation over 256. Inventory rewards
  privately drop on overflow; all ten raw bird meats always become private ground objects even
  when the inventory has room.
- Expanded `BirdhouseTrapTest` from six to eleven scenarios. The suite pins the four reward object
  identities, all active-location handlers, every XP tier, seed-nest boundary values and the
  pre-XP visible-level snapshot, all feather quantities, private overflow, meat-always-ground,
  atomic site reset and no state/randomness consumption before maturity.
- Early `Empty`, built-state `Dismantle`, `Reset`, the ten other-nest/clue rolls, direct
  raw-material `Build`, ten-bird visual simulation, exact client presentation and diary or
  achievement hooks remain explicit case-142 work.
- The parity inventory now contains 528 rows: 50 discovered, 119 researched, one unit-tested, 357
  integration-tested and one client-verified. Automated-or-client backlog coverage is 359/528, or
  67.99%; this is evidence-row coverage, while honest regular-game completion remains about 25–30%
  and the complete 238.1-plus-Leagues vision about 15–20%.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m18s across 916 Gradle tasks: 81 executed and 835 up-to-date. Reports contain 1,712 unit tests in
  125 classes and 2,163 integration tests in 269 classes, with zero failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 46431 loaded 61 plugin modules and
  245 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened, changed or otherwise interacted with.

## 2026-09-13 — Bird-house direct-build and replacement lifecycle

- Extended the cutoff-pinned Bird house trapping evidence with the February 2025 lifecycle and
  Jagex Poll 83's 91.8% automatic-rebuild approval. Cache location options independently pin
  `Build` on the empty site, `Dismantle` on all nine partial sites, and `Empty` plus `Reset` on all
  nine active sites. The still-unconfirmed clue-nest selection branch remains deliberately absent.
- The empty site's direct `Build` now prefers the best carried usable shell. Without one, it offers
  available logs through the native skill-multi chooser, validates the matching Crafting and Hunter
  levels plus clockwork, chisel and hammer, crafts after two ticks for exact shell XP, and consumes
  that output directly into the site. Delayed completion revalidates distance, site state, tools and
  materials before any mutation.
- Partial `Dismantle` and immature `Empty` now present destructive-action warnings. Declining is
  inert; accepting revalidates the same display and stored seed/deadline state, loses the shell,
  seeds and any immature loot, clears both site variables, and returns or privately grounds only
  the clockwork. No Hunter XP or reward roll is produced.
- Mature `Reset` prefers a carried usable shell and replaces after one tick while returning the
  harvested clockwork. Otherwise it performs the same two-tick raw craft plus the sourced one-tick
  automatic-rebuild penalty, preserves the recovered mechanism in place even when inventory starts
  full, consumes one selected log, awards exact Crafting and Hunter XP and normal mature rewards,
  and leaves the replacement built but unseeded. Exact path timing is documented as an evidence-led
  inference pending revision-matched client observation.
- `BirdhouseCrafting` now exposes its existing one-result transaction and a reset-only log
  transaction so the Hunter plugin reuses the established recipe syntax, tools, XP and atomicity.
  `BirdhouseTrapTest` expanded from eleven to twenty scenarios, adding carried-shell priority,
  direct raw construction, stale-site safety, early Empty accept/decline, partial Dismantle, both
  Reset paths, exact delays and full-inventory clockwork retention.
- The parity inventory now contains 529 rows: 50 discovered, 119 researched, one unit-tested, 358
  integration-tested and one client-verified. Automated-or-client backlog coverage is 360/529, or
  68.05%; this is evidence-row coverage, while honest regular-game completion remains about 25–30%
  and the complete 238.1-plus-Leagues vision about 15–20%.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m25s across 916 Gradle tasks: 86 executed and 830 up-to-date. Reports contain 1,712 unit tests in
  125 classes and 2,172 integration tests in 269 classes, with zero failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 81697 loaded 61 plugin modules and
  245 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened, changed or otherwise interacted with.

## 2026-09-13 — Bird-snare trapping core

- Pinned the final pre-cutoff Wiki revisions for Bird snare, Game tick and all five snare prey,
  then reconciled their requirements, XP, success-rate constants and drops with cache 233.4 object,
  NPC, location and sequence identities. The cache-delta audit found no bird-snare behavior change
  in 233.4. RuneLite source was read only from the workspace copy to corroborate the one-minute
  lifecycle and location-state families; RuneLite was not opened or launched.
- Added a focused `content/skills/hunter/bird-snares` plugin. `Lay` is a four-tick action that
  atomically consumes one stackable snare, validates members access and the boosted Hunter level,
  rejects blocked footprints, enforces boosted-level trap caps 1/2/3/4/5 at levels 1/20/40/60/80,
  and grants the sourced extra trap inside the Wilderness.
- Each placed snare now owns a controller-backed open, trapping, failing, full or broken state.
  Nearby eligible crimson swifts, golden warblers, copper longtails, cerulean twitches and tropical
  wagtails can enter the cache-native two-tick transition, while a player standing directly on the
  trap prevents a catch. Open, full and failed traps collapse after exactly 100 game ticks into a
  private owner-visible ground snare; logout safely clears owned traps and returns or privately
  grounds their snares.
- Catch rolls reuse the project's Wiki-matched low/high interpolation at the visible Hunter level.
  Smoking contributes the sourced two percentage points. A complete Guild Hunter outfit currently
  contributes the sourced 2.5 percentage points additively, but the cutoff Wiki explicitly says
  additive versus multiplicative stacking is unknown, so matching-client confirmation remains in
  manual case 143 rather than being claimed as exact.
- Successful `Check` atomically returns the snare and awards bones, raw bird meat, five-to-ten
  species-coloured feathers and exact Hunter XP 34/47/61.2/64.5/95.2. A full inventory leaves the
  trap intact for retry. Failed and open traps expose only cache-authoritative options; no invented
  reset action was added. Exact attraction radius, scan cadence, investigate wording, outfit
  stacking and client choreography remain explicitly qualified pending revision-matched capture.
- Seven cache-aware integration scenarios cover all location, NPC, drop, animation and hunt
  identities; the four-tick stack consumption; boosted and Wilderness caps; smoke and outfit rate
  inputs; successful and failed transitions; atomic rewards and XP; dismantling and relaying;
  one-minute private collapse; and full-inventory retry safety.
- The parity inventory now contains 530 rows: 50 discovered, 119 researched, one unit-tested, 359
  integration-tested and one client-verified. Automated-or-client evidence coverage is 361/530, or
  68.11%; this is evidence-row coverage, while honest regular-game completion remains about 25–30%
  and the complete 238.1-plus-Leagues vision about 15–20%.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  3m17s across 922 Gradle tasks: 525 executed, one restored from cache and 396 up-to-date. Reports
  contain 1,712 unit tests in 125 classes and 2,179 integration tests in 270 classes, with zero
  failures, errors or skips.
- Restarted only the validated workspace rsmod server. Process 28899 loaded 62 plugin modules and
  246 scripts, bound port 43594 and reported revision 233 with Desktop-client support. RuneLite was
  not opened, changed or otherwise interacted with.

## 2026-09-14 — Varlamore sunlight and moonlight moth core

- Pinned the last usable pre-cutoff Wiki revisions for sunlight moth, moonlight moth, both filled
  jar items and the shared Butterfly Hunter rules. Reconciled them with cache-233.4 NPCs
  12770–12773, net/jar objects 10010/10012/11259/28890/28893 and sequences 5209/5213/5214. Read
  only the workspace RuneLite source to corroborate floor-before-add sunlight restoration, its 21
  affected production stats and flat 22-point moonlight Prayer restoration; RuneLite was not
  launched.
- Added the focused `content/skills/hunter/moths` plugin. It registers only the three NPC variants
  whose cache definitions expose `Catch`, deliberately excluding non-clickable moonlight variant
  12773. Membership, visible Hunter 65/75, wielded ordinary/magic net and +10-level barehanded
  gates are revalidated after the bounded one-tick action.
- Catch rolls use the four sourced interpolation curves. Success awards exactly 74 or 84 Hunter XP
  and schedules the cache-native 100-cycle NPC respawn. A net plus empty jar atomically exchanges
  the jar for the matching filled object even in a full inventory; bare hands or no jar apply the
  direct effect instead.
- Direct sunlight catches restore `floor(base × 20%) + 6` to all 21 documented non-Hitpoints,
  non-Prayer production stats, eight Hitpoints and five percent run energy. Jar release omits run
  restoration. Direct or jarred moonlight restores 22 Prayer, capped at base. Both release routes
  atomically replace the filled jar with one empty jar and use cache sequence 5213.
- Added two unit tests for success-curve thresholds and restoration rounding plus nine cache-aware
  integration scenarios for identities/operations/script registration, level and equipment
  selection, full-inventory atomic swaps, direct and jar effects, every sunlight stat cap,
  deterministic failure preservation and the exact respawn schedule. Exact messages, timing,
  sounds, packet order, sequence-5214 creature visualization, legacy butterflies, rare wings,
  Hunter Rumours and multiplayer sharing remain explicitly in manual case 144.
- The parity inventory now contains 532 rows: 50 discovered, 120 researched, one unit-tested, 360
  integration-tested and one verified. Automated-or-client evidence coverage is 362/532, or
  68.05%; this is evidence-row coverage, not whole-game completion.
- The complete `spotlessCheck`, `validateCache233Parity`, `test` and `integration` gate passed in
  4m59s across 930 Gradle tasks: 525 executed, one restored from cache and 404 up-to-date. Reports
  contain 1,714 unit tests in 126 classes and 2,188 integration tests in 271 classes, with zero
  failures, errors or skips.
- The previously recorded server process 28899 was already absent and no process occupied port
  43594, so there was nothing live to terminate. Started only the freshly validated workspace
  rsmod server. Process 50275 loaded 63 plugin modules and 247 scripts, bound port 43594 and
  reported revision 233 with Desktop-client support. RuneLite was not opened, changed or otherwise
  interacted with.
