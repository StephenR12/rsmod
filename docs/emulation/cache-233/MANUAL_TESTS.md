# Manual cache-233 tests

Record the date, server commit, RuneLite commit/version, account state, steps, expected observations,
actual observations and result for every run. A client test without reproducible state and expected
tick-level behavior is not verification.

For the November 2026 skill checkpoint, start with the [launch commands and short diary playtest](SKILLS_AND_DIARIES.md).

## Connection smoke test

1. Start the RSMod server from this repository without repacking or changing the baseline cache.
2. Start only the revision-matched RuneLite checkout with `../runelite/run-rsmod.sh`. The launcher
   explicitly opts into the local-client mode, binds the sibling `rsmod` directory, loads its RSA
   public key and keeps RuneLite state under `.runelite-rsmod`; do not open an installed RuneLite
   application.
3. Connect to the local server and log in with a test account. The development realm ignores
   passwords, but the revision-233 client still requires a non-empty field, so use a disposable test
   value rather than a real account password.
4. Confirm login, region load, movement, appearance, inventory, equipment, tabs and logout.
5. Record server/client logs and any protocol or cache mismatch.

## Feature test template

```text
Feature id:
Server revision/commit:
Client revision/commit:
Starting coordinates:
Account mode and persistent state:
Stats, inventory, equipment, vars and world setup:
Steps with game ticks:
Expected messages/animations/graphics/sounds/state deltas:
Observed result:
Pass/fail:
Evidence or issue:
```

Client testing supplements automated tests; it does not replace formula, cache-contract or
integration coverage.

## Teleother procedure

1. Log in two members accounts within ten tiles. Give the caster the exact runes and Magic levels
   for Teleother Lumbridge, Falador and Camelot; toggle Accept Aid on for the recipient.
2. Cast each spell on the recipient. Confirm caster sequence 1818, spotanim 343 and synth 199, then
   record the exact caster name, destination name and wording shown by interface 326.
3. Decline one offer and close one offer. Confirm neither moves the recipient and determine from
   observed inventory/XP whether the official client charges the caster when the offer opens or
   only after acceptance.
4. Accept each destination. Confirm recipient sequence 1816, spotanim 342, the exact impact-to-move
   tick and that Lumbridge/Falador/Camelot land only inside their pinned Wiki regions.
5. Repeat with Accept Aid off, a busy recipient, insufficient level/runes, an active Tele Block and
   Wilderness levels 20 and 21. Record exact messages, ordering and whether each failure consumes
   anything.
6. Exercise combat, movement, logout, interface replacement, Ironman restrictions and any temporary
   Accept Aid suppression. Record which action is interrupted, which offer is cancelled and all
   client presentation details.

## Teleport to Target procedure

1. In a cache-233 client session, place two members accounts in Bounty Hunter and assign one as the
   other's target. Keep the caster's target teleport scroll, unlock varbit and quota at zero.
2. Read scroll 12846. Record every page, the confirmation title and both choices. Decline once and
   confirm the scroll remains; accept once and confirm it is consumed, the success message appears
   and spellbook child 218:73 becomes usable on Standard, Ancient, Lunar and Arceuus books.
3. Give the caster level 85 Magic and one law, chaos and death rune. Cast from the lobby and record
   the animation, graphic, sound, resource/XP timing, exact movement tick and landing tile. Confirm
   45 XP and a final distance no greater than 12 tiles from the target.
4. Cast inside the crater at 11.4 seconds and 12.0 seconds after both PvP and PvM combat. Confirm the
   former is rejected atomically and the latter succeeds; record the exact message and check order.
5. Repeat five successful casts to the same target. Record quota/limit client state after each, then
   confirm the sixth cast is rejected without rune or XP use. Assign a new target and confirm the
   quota resets under every normal assignment path.
6. During the four-tick cast, make the target move, leave the crater, die, log out, escape and be
   replaced by a new target. Record whether each outcome consumes costs, cancels movement, changes
   quota or selects a new destination.
7. Try non-members, level 84, missing each rune, locked spell, no target, invalid target and every
   lobby/crater boundary tile. Record exact failure strings and whether warning varbit 236 changes.
8. Compare server packets and visible state with the workspace RuneLite without opening any other
   RuneLite checkout. Preserve tick-level evidence before advancing this row toward client-tested.

## Teleport to House procedure

1. In the workspace cache-233 client, use a members account with level 40 Magic and one air, earth
   and law rune. Test a house at each of Rimmington, Taverley, Pollnivneach, Rellekka, Brimhaven,
   Yanille, Hosidius, Prifddinas and Aldarin.
2. Set the default destination outside and click the primary Cast operation. Record the exact
   movement tick, tile, animation, graphic, sound and resource/XP timing. Repeat through the explicit
   Outside operation and confirm both routes agree.
3. Set the default destination inside, prepare a regular house instance and cast through operation
   1. Repeat with the default Building Mode setting enabled. Confirm the correct instance and exit-
   portal-adjacent tile are used and record how varbits 2176, 4744 and 14670 change.
4. Exercise Group: Choose and Group: Previous with Group Ironman accounts. Record every interface,
   eligibility/ownership rule, previous-house state, cancel path, exact message and instance target.
5. Test no house, an unavailable house instance, non-members, level 39, each missing rune, active
   Tele Block and Wilderness levels 20 and 21. Confirm exact check ordering and that failures are
   atomic.
6. Move, enter combat, start another action, teleport, log out and die during the four-tick cast.
   Record cancellation, locking, costs and visible state. Repeat with staves, combination runes and
   the Fountain of Rune.
7. Compare all nine outside landing tiles with their cache portal positions. Preserve packets and
   tick-level evidence from only the workspace RuneLite before advancing the core beyond
   `integration-tested`.

## Standard stat-drain curse procedure

1. Equip no staff, supply the exact runes and cast each of Confuse, Weaken, Curse, Vulnerability,
   Enfeeble and Stun against a fresh NPC and an opted-in player target.
2. Record the casting animation, launch graphic, projectile, impact graphic, sounds and the exact
   impact tick. Confirm there is no damage hitmark, headbar or defend animation.
3. Confirm base Magic XP is awarded on both hit and splash, no Hitpoints XP is awarded, and the
   affected stat changes only on a successful impact.
4. Try the same and weaker spell against an already-drained stat. Confirm the exact rejection
   message and no rune, charge or XP use.
5. Repeat with a charged Tome of Water. Confirm +20% accuracy, 5% drains becoming 7%, 10% becoming
   15%, and exactly one charge consumed per attempted combat cast. Repeat at one remaining charge
   and confirm the item becomes the empty tome.
6. Confirm none of the six spells can be selected for autocast and that manual casts are limited to
   one every five ticks.

## Standard binding-spell procedure

1. Equip no staff, supply exact runes and cast Bind, Snare and Entangle against a fresh movable NPC
   and an opted-in player target. Record animation, launch graphic, projectile, impact graphic,
   sounds and exact projectile-impact tick on hit and splash.
2. Confirm Bind deals no damage, Snare caps at 2 and Entangle caps at 5. Record base Magic XP of
   31/61/89 on successful casts and 30/60/70 on splashes, plus ordinary damage XP where applicable.
3. Attempt movement each tick and confirm holds of 8/16/24 cycles followed by five cycles of
   refreeze immunity. Confirm the exact already-held and immune messages and no rune or XP use on
   either rejected cast.
4. On a player target, confirm the red `The holding effect is beginning to fade.` message and its
   audio cue shortly before release. Record the precise message and sound ticks rather than assuming
   the automated four-cycle warning timing is final.
5. Repeat Entangle while equipping Swampbark helm, body and legs individually and together. Confirm
   each piece adds two cycles and the three-piece bonus totals six cycles.
6. In PvP, move the caster beyond the maximum ten-tile attack range and confirm immediate release
   into five-cycle immunity. Repeat near the boundary in both movement directions.
7. Repeat with a charged Tome of Water and confirm the 20% accuracy modifier and exactly one charge
   consumed per attempted binding cast, including the empty-tome transition at the last charge.
8. Confirm none of the three spells can be selected for autocast and manual casts are limited to one
   every five ticks. Record NPCs that resist or ignore binding for the production exception dataset.

## Tele Block procedure

1. With two opted-in players in the Wilderness, cast Tele Block with one law, chaos and death rune.
   Record sequences, graphics, sounds and the exact impact tick for both success and splash. Confirm
   95 Magic XP on success, 83.6 on splash, no damage/Hitpoints XP, and one cast every five ticks.
2. Confirm success shows the caster name and five-minute duration on the target, starts target-only
   logout prevention, publishes a five-minute timer, and prevents every teleport path while active.
3. Activate Protect from Magic before casting. Confirm the hit chance is unchanged but a successful
   block lasts two minutes and 30 seconds. Toggle the prayer after launch and record whether the
   duration follows cast-time or impact-time state.
4. Recast during the block and during the following one-minute immunity. Confirm the rejection text
   and no rune, sack or XP use. Capture the 30-second warning, natural-expiry and immunity-end text
   and ticks; these remain unimplemented until exact evidence is recorded.
5. Confirm leaving the Wilderness, dying and logging out clear the block and immunity. Have the
   target kill the original caster and capture the exact cleanup timing/message for implementation.
6. Repeat with only a blighted teleport sack and confirm exactly one sack is consumed. Attempt the
   spell outside the Wilderness and against an NPC, recording exact rejection messages and proving
   that no resources or XP are consumed.

## Crumble Undead procedure

1. With two air, two earth and one chaos rune, cast Crumble Undead on representative cache-tagged
   skeletons, zombies, ghosts and shades. Confirm 24.5 base Magic XP, 0–15 damage, ordinary damage
   and Hitpoints XP, and one cast every five ticks.
2. Record unarmed and staff casting animations, launch/travel/impact graphics, sounds and exact
   impact tick on both hit and splash. Compare them with cache sequences 724/1166, spotanims
   145/146/147 and synths 122/123/124.
3. Cast on a living NPC and an opted-in player. Confirm the exact undead-only rejection and no rune
   or XP use. Repeat across bosses and quest encounters whose undead classification may be unusual,
   recording any mismatch with cache parameter 65434.
4. Equip Chaos gauntlets and confirm the max remains 15. Equip Salve amulet (ei) and confirm it rises
   to 18; test other Salve variants separately so accuracy and damage modifiers are not conflated.
5. Confirm autocast selection is offered only by the imbued skull sceptre, Slayer's staff variants,
   Staff of the dead variants, Staff of light, Staff of balance and Void Knight mace. Confirm an
   ordinary elemental staff cannot autocast the spell.

## Magic Dart procedure

1. At 50 Magic and 55 current Slayer, cast with four mind and one death rune using each permitted
   staff. Confirm 30 base Magic XP, `10 + floor(current Magic / 10)` base damage, ordinary damage and
   Hitpoints XP, and one cast every five ticks. Repeat below both stat requirements and capture exact
   failure text, rune ordering and whether temporary Slayer boosts qualify.
2. Record sequence 1576, ordinary spotanims 328/329, enchanted spotanims 1373/1374, sound 174 and
   exact impact timing. Confirm Slayer's staff (e) uses white effects on a hit but a splash travels
   green. Verify the generic projectile profile rather than assuming the automated selection is final.
3. On a current Slayer assignment, confirm an enchanted staff uses
   `13 + floor(current Magic / 6)`, consumes exactly one charge only on a hit and reverts to an
   ordinary Slayer's staff at zero. Confirm an off-task hit and all splashes consume no charge.
4. Confirm only Slayer's staff, Slayer's staff (e), Staff of the dead, toxic Staff of the dead,
   Staff of light and Staff of balance offer Magic Dart autocast. Check charged and uncharged toxic
   states separately.
5. Attack Turoths and Kurasks with Magic Dart, other spells and every allowed non-magic weapon.
   Record the damage-immunity response so the shared Slayer-equipment gate can be implemented once.

## Iban Blast procedure

1. After Underground Pass, cast Iban Blast with five fire and one death rune using both the regular
   and upgraded Iban staff. Confirm 30 base Magic XP, 0–25 damage, ordinary damage/Hitpoints XP and
   one cast every five ticks.
2. Record sequence 708, casting/travel/impact spotanims 87/88/89, projectile profile 8, exact impact
   timing and all sounds on both hit and splash. Compare spotanim animation sequences with
   655/656/657.
3. Check charges, cast once on an NPC and once on a player, then check again. Confirm the shared
   total drops by one and two respectively regardless of which owned Iban staff is equipped. Capture
   the exact messages at zero charges and at one charge before a PvP attempt, plus every low-charge
   warning threshold.
4. Equip a charged Tome of Fire with no loose fire runes. Confirm the cast uses only one death rune,
   consumes no burnt-page charge and retains a max hit of 25.
5. Confirm only regular and upgraded Iban staves offer Iban Blast autocast. Exercise Koftik and the
   Dark Mage recharge services, staff upgrade, death/loss/reclaim, duplicate staves and login/logout
   persistence, recording all item transformations, prices and messages.

## God spells and Charge procedure

1. In the Mage Arena, claim each god staff and cast its spell 100 times on the battle mages. Record
   the exact varp 267 changes, region boundaries, counters, unlock messages and when each spell first
   becomes usable outside the arena. Verify temporary Magic boosts for the level requirements.
2. For each god spell, record sequence 811, target graphics 76/77/78, all sounds and the exact hit
   and splash ticks. Confirm level 60, 35 base Magic XP, the documented rune/staff alternatives,
   ordinary damage and Hitpoints XP, max hit 20 and one cast every five ticks.
3. On fresh player and NPC targets, verify Saradomin Strike drains one Prayer only from players,
   Claws drains Defence and Flames drains Magic by one plus 5% of base level, rounded down. Recast
   before restoration and confirm the effects do not stack; record NPC immunities and caps.
4. Confirm only Staff of light autocasts Saradomin Strike; Void Knight mace and Staff of balance
   autocast Claws; and Staff of the dead/toxic, Staff of light, Staff of balance and the documented
   revenant sceptres autocast Flames. Verify the three original god staves remain manual-only.
5. Cast Charge at level 80 with three air, fire and blood runes after claiming a god staff. Confirm
   180 Magic XP, seven-minute duration, one-minute recast cooldown, exact messages and timer behavior.
   Log out and back in during both states and record whether elapsed time or logged-in ticks apply.
6. During Charge, pair each god spell with matching and mismatched regular, imbued, locked/trouver,
   regular max and imbued max capes. Confirm only the matching family raises max hit from 20 to 30
   and that changing capes during the seven minutes updates the effect immediately.
7. Cast Flames through each Staff of the dead variant for enough trials to establish the documented
   1-in-7 rune-saving effect. Confirm whether it applies on hits and splashes and capture feedback.

## Low and High Level Alchemy procedure

1. At levels 21 and 55, cast Low and High Alchemy on known-value tradeable and untradeable items.
   Confirm 40%/60% of cache value, 31/65 Magic XP, exact rune use, sequences 712/713, spotanims
   112/113, sounds 98/97 and ordinary three/five-tick rates.
2. Cast on stacks, including nature and fire runes. Confirm one target item is removed in addition
   to any copy used as the spell cost, and verify insufficient same-stack counts reject atomically.
3. Exercise cache objects marked `no_alchemy`, a full inventory, zero-value items and values near
   stack limits. Capture exact messages and prove no item, rune or XP mutation on every rejection.
4. Repeat at the Fountain of Rune and with each eligible Explorer's ring. Confirm the Fountain uses
   no runes and gives no XP; record daily ring counts, item-value rules, messages and animations.
5. Test Fishing Trawler, Guardians of the Rift, Nightmare Zone, Pest Control and Wintertodt, plus
   Low Alchemy in the Alchemist's Playground. Record every location gate and the Playground's
   five-tick exception.
6. Queue a second cast, interrupt with movement/combat/interface changes, change the configured
   animation, and cross each warning threshold. Record exact server/client ordering and persistence.

## Bones to Bananas and Bones to Peaches procedure

1. Cast both spells with their exact runes and Magic levels. Confirm 25/35.5 XP, one-tick rate,
   sequence 722, spotanim 141, sound 114 and no post-2024 action stall.
2. Test every normal, burnt, bat, big, wolf, jogre, monkey, bleached and Alan's bone variant together.
   Confirm all eligible bones transform, dragon/higher bones and ashes remain, and capture messages
   when the inventory has only unsuitable bones or no bones.
3. Before and after buying Bones to Peaches, test the spellbook's locked appearance and cast failure.
   Record the exact message and varbit 1505 transition, then verify temporary Magic boosts during
   purchase and casting.
4. In the Creature Graveyard, convert each of the four animals' bone variants separately and in a
   nearly full inventory. Confirm one/two/three/four fruit yields, overflow behavior, running during
   the cast, chute credit and fruit cleanup/persistence on exit and logout.
5. Test both spells inside Tombs of Amascut and capture the exact restriction. Cast Peaches in Al
   Kharid Palace to verify diary credit, distinguishing the direct spell from the tablet.
6. Exercise both tablet forms outside and inside the Creature Graveyard, with and without the spell
   unlock. Record rune/XP bypass, break animation, simultaneous sounds and interruption ordering.

## Superheat Item procedure

1. At 43 Magic, cast with four fire and one nature rune on every cache-era recipe: bronze, iron,
   silver, steel, gold, lovakite, mithril, adamantite, runite, blurite and lunar. Record exact ore,
   coal, rune, bar and XP deltas plus the success animation, graphic, sound and tick ordering.
2. Test iron below/at level 30 with zero, one and two coal, then repeat with a catalyst. Confirm iron
   always succeeds and steel is selected only when its effective coal and level requirements pass.
3. For every coal bar, repeat with a catalyst and a coal bag containing boundary quantities. Confirm
   one catalyst, half coal, double Smithing XP and loose inventory coal consumed before bag coal.
4. Cast gold with goldsmith gauntlets, each Smithing cape and a max cape, then mismatched equipment.
   Confirm 56.2 versus 22.5 Smithing XP and no accidental catalyst effect on coal-free recipes.
5. Test blurite before and after The Knight's Sword completion and lunar at progress 154/155 and
   after completion. Capture exact quest and level messages. Test perfect gold ore and confirm the
   current furnace-only exclusion rather than assuming it behaves like ordinary gold.
6. Cast on jogre bones at base Cooking 29, boosted current 30 and base 30. Confirm boosts do not
   qualify, success gives 25 Cooking plus 53 Magic XP, and capture the exact failure text.
7. Test noted ore, elemental ore, unrelated items, missing secondary ore/coal, insufficient runes,
   full inventory and the Fountain of Rune. Prove every rejection is atomic and capture exact text.
8. Click three valid targets inside one three-tick window. Confirm the first cast executes, exactly
   one extra cast queues, and determine whether later clicks replace or leave that queued target.
   Interrupt with movement, combat, interfaces, logout and inventory changes and record behavior.

## Ordinary furnace-smelting procedure

1. Exercise Smelt and ore-on-furnace against each of the 28 cache locs. Confirm 27 use operation 2,
   large Lovakite loc 28562 uses operation 1, the standard menu contains the exact eight ordinary
   bars, and blurite/lunar/lovakite appear only through their intended ore-target paths.
2. Smelt every one of the eleven bars at the level boundary. Record all ore, coal, bar and Smithing
   XP deltas. Confirm most first bars complete after four ticks, gold after five, lunar after six,
   and ordinary repeats every five ticks; capture animation, sound, message and interface ordering.
3. Smelt iron through a statistically useful sample without a ring, then exactly 140 ores with rings
   equipped/replaced mid-counter. Confirm 50% ordinary success, the exact impurity message, one
   shared counter across rings, Check output, final-bar melting and Break resetting the next ring.
4. For each coal bar, test exact and insufficient loose/bag coal with and without Smithing catalysts.
   Confirm one catalyst per bar, halved coal, doubled XP and loose coal consumed before bag coal,
   including a batch that crosses inventory and bag boundaries.
5. Smelt gold with goldsmith gauntlets, Smithing cape, trimmed cape and max cape, then mismatched
   equipment. Confirm 56.2 versus 22.5 XP and verify the gold-specific first-action delay.
6. At Edgeville, force or sample the 10% Varrock armour proc with tiers 1–4 at each boundary. Confirm
   two complete material sets are consumed, two bars and both XP awards are produced, and the proc
   cannot exceed the requested/material/ring-charge remainder.
7. At Shilo Village, test exact/insufficient coins and elite diary state across a batch. Confirm 20
   coins per bar and atomic rejection. At the Lovakite furnace, confirm normal bars and lovakite are
   routed correctly; test blurite and lunar immediately before and at their quest thresholds.
8. Walk away, run, teleport, enter combat, open another interface, alter materials and log out during
   first and repeated actions. Capture which events stop the batch, exact messages and whether the
   furnace animation is reset on the same cycle.

## Steel cannonball Smithing procedure

1. Use a steel bar, regular ammo mould and double ammo mould on every ordinary and Shilo furnace.
   Confirm the one-choice quantity menu, maximum value, explicit-mould selection and double-mould
   priority when a steel bar is used while both moulds are held. Confirm Lovakite furnaces reject.
2. At Smithing 34/35 and Dwarf Cannon progress 9/10, test both moulds on members and free worlds.
   Record the exact failure order and messages, including missing mould, one bar with the double
   mould, insufficient coins and boosted Smithing levels.
3. With the regular mould, confirm one steel bar produces four cannonballs and 25.6 Smithing XP.
   With the double mould, confirm two bars produce eight and 51.2 XP. Capture inventory, XP,
   sequence 827 and both exact cooling/removal message cycles.
4. Time one action and a multi-action batch. Confirm the first output after eight ticks and each
   repeat after ten ticks, then determine exact animation, message, interface-close and inventory
   ordering with the matching cache-233 client.
5. At Shilo Village, test regular and double moulds with exact coin boundaries and elite diary
   completion. Determine whether the documented 20-coin charge applies per action, per bar or per
   four cannonballs when using the double mould; do not promote the current per-action policy until
   that observation is pinned.
6. Complete a cannonball action in Morytania with the medium diary task incomplete and verify the
   production diary credit. Walk away, run, teleport, enter combat, mutate bars/moulds, open another
   interface, log out and die during initial and repeated actions; record exact cancellation ticks.

## Ordinary gold-jewellery Crafting procedure

1. Use a gold bar on every ordinary and Shilo furnace. Confirm native interface 446 opens and shows
   the exact gold-through-zenyte ring, necklace, unstrung-amulet and bracelet choices available for
   the held moulds and gems. Confirm the Lovakite furnaces reject the interaction.
2. Exercise all 32 ordinary recipes at one level below and exactly at their requirement. Record the
   gold bar, cut gem, mould, product and fine-XP deltas, and verify the free-to-play/member boundary:
   gold-through-diamond rings, necklaces and amulets are free-to-play; every bracelet and every
   dragonstone/onyx/zenyte piece is members-only.
3. Select 1, 5, 10, X and All through the cache-233 interface. Capture the exact PauseButton packet
   component/subcomponent values, remembered-selection varbit 13892 behavior, maximum derivation,
   spacebar behavior, interface closing and malformed/out-of-range input handling.
4. Time single and multi-item batches. Verify one product every three ticks, mould preservation,
   animation 899, inventory/XP ordering and behavior when a requested quantity exceeds materials.
5. At Shilo Village, test exact and insufficient coin boundaries plus elite diary completion across
   a batch. Verify whether the documented 20-coin furnace fee is charged per jewellery piece and
   capture the exact rejection text and presentation.
6. Walk away, run, teleport, enter combat, alter bars/gems/moulds, start ordinary smelting or
   cannonballs, open another interface, log out and die during initial and repeated actions. Record
   exact cancellation and animation-reset cycles.
7. Separately capture Slayer-ring and eternal-Slayer-ring input priority, Ring Bling gate, output,
   quantity behavior and last-selection interaction. Test gold-bar, gem and mould use-on variants,
   diary credit and cross-family silver/special-product cancellation before promoting those gaps.

## Ordinary silver-jewellery Crafting procedure

1. Use a silver bar on every ordinary and Shilo furnace. Confirm native interface 6 exposes all 12
   opal/jade/topaz jewellery pieces and the holy symbol, unholy emblem, sickle, unfinished bolts and
   tiara choices. Confirm Lovakite furnaces reject the interaction.
2. Exercise all 20 implemented recipes below and exactly at their level requirement. Verify exact
   bar/gem/mould/product and fine-XP deltas, mould preservation, ten unfinished bolts per bar, the
   free-world exception for only the holy symbol and tiara, and members rejection for every other
   product.
3. Select 1, 5, 10, X and All. Capture the exact PauseButton packet values, maximum derivation,
   spacebar behavior, interface-close ordering and last-selection varbit 13893 encoding.
4. Confirm one product action every three ticks and sequence 899. Test quantities beyond materials,
   full inventories, exact/insufficient Shilo coins, the elite waiver, movement, combat, teleport,
   interface replacement, logout, death and cross-family furnace queue replacement.
5. Separately test interface children 26, 29 and 30 at every relevant Creature of Fenkenstrain, In
   Aid of the Myreque and Shadow of the Storm progression boundary. Confirm their visibility follows
   mould possession, then lose/reclaim each mould and capture exact quest-side replacement text.
   Verify conductor and sigil one-bar batches and Silvthrill batches consuming one silver bar, one
   mithril bar and one cut sapphire apiece, including exact failures when each material disappears.

## Gem cutting and amulet stringing procedure

1. Use a chisel on each uncut gem in both item orders. Test one level below and exactly at levels
   1, 13, 16, 20, 27, 34, 43, 55, 67 and 89. Confirm the exact membership boundary: sapphire,
   emerald, ruby and diamond work on free worlds while the other six gems reject atomically.
2. For opal, jade and red topaz, capture large samples at their minimum levels and at level 99.
   Confirm success probabilities 129/256 and 251/256, 119/256 and 246/256, and 114/256 and 241/256;
   verify success XP 15/20/25 and crushed-gem XP 3.8/5/6.3. Confirm all seven precious gems can
   never crush and that zenyte awards the cache-era 50 XP rather than its pre-2024 200 XP.
3. Time single and multi-gem inventories. Confirm one result every two ticks and sequences
   890/891/892/888/889/887/886/885/2717/7185 for opal through zenyte. Capture exact success and
   crush messages, sound IDs, animation reset, inventory/XP ordering and behavior with a full
   inventory.
4. Combine balls of wool in both item orders with gold, sapphire, emerald, ruby, diamond, opal,
   jade, topaz, dragonstone, onyx and zenyte amulets (u), both unstrung silver symbols, the
   unstrung M'speak amulet and a Salve shard. Verify exact output identities and membership gates.
5. Confirm every covered stringing action except Salve awards exactly 4 Crafting XP; Salve awards
   0. Confirm one result every two ticks, Make-All behavior and the exact first/repeat timing for
   Salve after its 2022 interface change.
6. Remove the chisel, uncut gem, wool or unstrung item between repetitions. Then walk, run,
   teleport, enter combat, open/replace interfaces, log out and die during each action. Capture
   exact cancellation, queue-replacement, animation, sound and message behavior, including starting
   stringing during gem cutting and vice versa.
7. Exercise M'speak and Salve production at every relevant Monkey Madness I and Haunted Mine state,
   including replacement items. Confirm the production action follows possession while acquisition
   and area access remain quest-controlled, and record any diary/quest credit emitted by ordinary
   gem cutting or stringing.

## Glassblowing procedure

1. On a members world, use a glassblowing pipe on molten glass in both item orders. Confirm cache
   interface 270 opens through clientscript 2046 with beer glass, empty candle lantern, empty oil
   lamp, vial, empty fishbowl, unpowered orb, lantern lens and empty light orb in that order.
2. Test one level below and exactly at Crafting levels 1, 4, 12, 33, 42, 46, 49 and 87. Confirm
   outputs 1919/4527/4525/229/6667/567/4542/10980 and XP 17.5/19/25/35/42.5/52.5/55/70. Repeat on
   a free world and confirm every recipe rejects without consuming material.
3. Select quantities 1, 5, 10, X and All with exact, excess and changing molten-glass counts.
   Confirm sequence 884 and one atomic product every three ticks, with the pipe always preserved.
4. Remove the pipe or molten glass between repetitions, fill the inventory, and start gem cutting
   or amulet stringing during a batch. Confirm safe cancellation and no lost/duplicated material.
5. Walk, run, teleport, enter combat, replace the chat/modal interface, log out and die during the
   first and repeated actions. Capture exact cancellation tick, animation reset, success/failure
   text, sound identity, inventory/XP ordering and previous-selection behavior.
6. Separately make molten glass at every furnace type from soda ash and a bucket of sand, assemble
   an empty oil lamp with an oil-lantern frame, and add cave goblin wire to an empty light orb.
   Confirm level 1/20 XP/two ticks and the returned bucket for molten glass, level 26/50 XP for the
   empty oil lantern, and level 87/104 XP/zero ticks for the light orb. Capture exact messages,
   sounds, prior-selection behavior, interface ordering and quest/diary coupling.

## Spinning procedure

1. Operate each registered spinning-wheel type and use wool, flax, sinew, every ordinary tree root
   and yak hair directly on a wheel. Confirm interface 270/clientscript 2046 lists ball of wool, bow
   string, crossbow string, magic string and rope in that order.
2. Verify levels 1/10/10/19/30, XP 2.5/15/15/30/25, the free-to-play wool exception and members-only
   status of every other product. With magic roots present, verify both crossbow-string and
   magic-string choices consume the intended material.
3. Select 1, 5, 10, X and All. Confirm sequence 894, one atomic transform every three ticks, safe
   stop on missing materials or movement, and cancellation against every other Crafting batch.
4. Repeat at the Seers' Village wheel before and after the applicable Kandarin diary reward, then
   walk, teleport, enter combat, replace interfaces, log out and die during initial/repeated ticks.
   Capture exact speedup, messages, sounds, credit hooks and audiovisual/inventory ordering.

## Pottery procedure

1. Operate every registered potter's wheel and use soft clay on it. Confirm interface 270 through
   clientscript 2046 lists pot, empty cup, pie dish, bowl, empty plant pot and pot lid in that order.
   Verify one clay creates four unfired cups but only one of every other product.
2. Test one level below and exactly at shaping levels 1/3/7/8/19/25, including free-world/member
   boundaries. For pot lids, test One Small Favour varp 416 at 234 and 235 and confirm the recipe
   unlocks only after Tassie's teaching state. Confirm shaping sequence 883, three ticks and XP
   6.3/8.5/15/18/20/20.
3. Fire every unfired product at each ordinary oven. Confirm oven sequence 1317, seven ticks, no
   item-specific firing level gate and successful XP 6.3/8.5/10/15/17.5/20. At Crafting levels
   1–13, capture enough trials to corroborate success numerators
   181/187/194/200/206/213/219/225/232/238/244/251/256 out of 256.
4. Fill the inventory before shaping cups; remove inputs between repetitions; move away; then walk,
   teleport, enter combat, replace interfaces, log out and die during initial/repeated shaping and
   firing. Confirm atomicity, cancellation, cross-Crafting queue replacement and no duplicate XP.
5. Capture exact success/crack messages, sound identity, oven/glow transitions, direct-item menu
   behavior, inventory/XP ordering and Varrock diary or League-task credit.

## Ordinary leatherworking procedure

1. Use an ordinary needle and a costume needle, in both item orders, on soft leather, hard leather
   and each of the four dragon leathers. Confirm interface 270/clientscript 2046 lists seven soft-
   leather products, one hardleather body or vambraces/chaps/body for the selected dragon colour.
2. Test one level below and exactly at all levels 1/7/9/11/14/18/38, 28, 57/60/63, 66/68/71,
   73/75/77 and 79/82/84. Confirm XP 13.8/16.2/18.5/22/25/27/37, 35, 62/124/186,
   70/140/210, 78/156/234 and 86/172/258, and confirm only dragonhide and costume-needle use are
   members-only.
3. Craft at least eleven mixed armour pieces with an ordinary needle. Confirm one thread is consumed
   on actions 5 and 10 regardless of whether the recipe uses one, two or three leathers, including
   after closing and reopening the menu. Repeat with a costume needle and no thread; confirm every
   action remains available and no ordinary-spool progress changes.
4. Confirm sequence 1249 and one atomic result every three ticks. Remove the needle, thread or
   leather between actions; walk, run, teleport, enter combat, replace the modal, log out and die;
   verify safe cancellation, no duplicate XP and replacement against every other Crafting batch.
5. Capture exact success/failure text and sound, inventory/XP ordering, thread-counter behavior
   across logout/world-hop, previous selection, diary/League-task hooks and every remaining
   specialist hide or textile entry point before promoting the extended row.

## Specialist snakeskin and yak-hide leatherworking procedure

1. Use an ordinary needle and a costume needle, in both item orders, on ordinary snakeskin. Confirm
   interface 270/clientscript 2046 lists boots, vambraces, bandana, chaps and body, and that each
   selected quantity is capped by both its 6/8/5/12/15-skin requirement and available whole thread
   when the ordinary needle is used.
2. Test one level below and exactly at boosted Crafting levels 45/47/48/51/53 on member and free
   worlds. Confirm 30/35/45/50/55 XP, three ticks per result, sequence 1249 and one entire thread
   consumed for every completed snakeskin item. Repeat without thread using the costume needle and
   confirm the same timing, material and XP contracts.
3. Use both needles in both item orders on cured yak hide at Fremennik Isles progress states 279,
   280, 290 and a completed state. Confirm state 279 rejects without mutation, state 280 is the
   exact unlock and all later states remain allowed. Confirm the menu lists legs then top; one hide
   at level 43 makes legs and two hides at level 46 make a top, each for 32 XP in three ticks.
4. Select batches larger than the available hides and thread; remove the needle, thread or material
   between results; fill the inventory; walk, run, teleport, enter combat, replace interfaces, start
   every other Crafting action, log out and die. Confirm atomic stopping, no duplicate XP and exact
   cross-Crafting queue replacement.
5. Capture the production client's exact menu order, success/failure text, sound, animation and
   inventory/XP ordering, previous selection, activity restrictions and diary/League-task credit.
   Repeat the same evidence workflow before implementing Hunter pouches, fur services or further
   quest-textile recipes.

## Xerician robes procedure

1. Use an ordinary needle and a costume needle, in both item orders, on Xerician fabric on a
   members world. Confirm interface 270/clientscript 2046 lists hat, robe and top in that order and
   exposes the normal 1/5/10/X/All quantity operations.
2. Test one level below and exactly at boosted Crafting levels 14/17/22. Confirm hat/robe/top use
   3/4/5 fabric, award 66/88/110 XP and complete atomically every three ticks with sequence 1249.
3. Confirm each result consumes one whole thread with an ordinary needle, while a costume needle
   needs no thread. Test a quantity larger than fabric or thread supply and verify the batch stops
   safely at the exact resource boundary.
4. Remove the tool, thread or fabric between results; fill the inventory; walk, run, teleport,
   enter combat, replace interfaces, start another Crafting action, log out and die. Confirm safe
   cancellation, no duplicate XP and correct cross-Crafting replacement.
5. Capture the production client's exact success/failure messages, sound, menu behavior,
   animation and inventory/XP ordering, previous selection and diary/League-task hooks.

## Hunter pouch Crafting procedure

1. Use ordinary and costume needles, in both item orders, on larupia, graahk, kyatt, fox and
   sunlight antelope fur. Confirm each opens the native one-product interface-270 quantity menu
   for small/medium/large fur pouch and small/large meat pouch respectively.
2. Test one level below and exactly at boosted Crafting levels 35, 50 and 65 on member and free
   worlds. Confirm the five recipes consume 4 larupia, 3 graahk plus a closed small fur pouch,
   3 kyatt plus a closed medium fur pouch, 4 fox fur, and 3 sunlight antelope fur plus a closed
   small meat pouch. Confirm 20/40/60/30/60 XP and one atomic result every three ticks with
   sequence 1249.
3. Confirm an open small/medium pouch cannot be used for an upgrade, especially the Wiki's explicit
   closed-small-meat-pouch requirement. Repeat with pouch contents present and confirm the shared
   family contents survive the upgrade.
4. Craft five mixed ordinary-leather, Lunar-clothing and Hunter-pouch results with one ordinary
   spool and confirm it is consumed on the fifth combined action. Repeat with a costume needle and
   no thread. Test quantity caps at partial-spool, fur and required-pouch boundaries.
5. Remove each tool or ingredient between results; fill the inventory; move, teleport, enter
   combat, replace interfaces, start every other Crafting action, log out and die. Confirm safe
   cancellation, full-inventory replacement and no duplicate XP.
6. Exercise Fill, Check and Open/Close on every state. Confirm fur tiers cap their shared family
   store at 14/21/28, meat tiers at 14/28, duplicate pouches do not increase those limits, and
   closed/open transitions preserve contents. Confirm tatty fur and raw bird meat stay outside.
7. Use bank-side Empty and confirm only that pouch family moves into the bank. Destroy any pouch
   and confirm its whole family store clears. Capture the exact confirmation, Check and restriction
   messages plus operation/audio ordering. Separately verify the eventual ordinary Empty routes at
   bank/Hunter Guild areas, deposit boxes and future Hunter-catch automatic-routing call sites.
8. Capture the exact production menu, messages, sound and inventory/XP ordering.

## Asyff Custom Fur Clothing procedure

1. On a members world, use Asyff's `Fur Clothing` operation and the hunting branch of his
   dialogue. Confirm both open cache interface 477 titled `Custom Fur Clothing`, inventory 482
   displays all 20 products in object-ID order and the cache's right-click/long-press guidance is
   intact. Confirm `Trade` opens the Fancy Clothes Store instead.
2. Make every product and verify the exact Wiki fees and unnoted materials: 20 coins plus two
   matching furs for each camouflage piece; 500/100/100 for larupia head/top/legs;
   750/150/150 for graahk; 1000/200/200 for kyatt; and 600/400/800 plus two matching furs for
   gloves of silence, spotted cape and spottier cape.
3. Hold tatty and perfect larupia, graahk or kyatt fur together. Confirm torso and leg production
   consumes tatty fur first, matching the 15 May 2024 change, while each hat/headdress rejects
   tatty fur and consumes only perfect fur.
4. Repeat on a free world through Talk-to, Trade and Fur Clothing and confirm Asyff says exactly
   `Sorry, we're closed at the moment.` without opening either interface or mutating inventory.
   Test insufficient fur, insufficient coins, noted furs, mismatched/spoofed product IDs, stale
   clicks after closing and a full 28-slot inventory.
5. Capture production's exact item menu operations, success/failure text, activity restrictions,
   sound and packet/inventory ordering. Separately verify the conditional Biohazard Priest-gown and
   Eagle's Peak bird-costume dialogue branches before implementing them.

## Hydra-leather Machinery procedure

1. On a members world after completing Dragon Slayer II, take hydra leather and a normal hammer to
   Machinery 33609 in the Lithkren Vault. Test both item-on-Machinery and direct `Investigate`.
   Confirm the exact untradeable warning, select Yes and verify one ferocious gloves appears after
   exactly seven ticks with no Crafting XP.
2. Repeat with ferocious gloves. Confirm the exact reverse prompt and that one hydra leather returns
   after seven ticks. Select No in both directions and verify the input remains untouched. Inspect
   hydra leather and compare the exact transcript text and automatic line wrapping.
3. Test Dragon Slayer II progress immediately below and at completed state 215, free/member worlds,
   no hammer, a normal hammer, Imcando hammer in inventory/right hand and offhand Imcando hammer in
   inventory/left hand. Capture the exact animation and sound for each valid hammer form and verify
   whether production accepts both Imcando substitutions at this specific machine.
4. Hold both hydra leather and ferocious gloves and use direct `Investigate`; capture which direction
   production prioritises. Repeat with neither input and record the exact response. Fill all 28
   inventory slots and confirm each one-for-one conversion remains atomic.
5. During the seven-tick action, move out of range, remove the hammer or input, click another object,
   replace the interface, enter combat, teleport, log out and die. Confirm no partial transform,
   duplication or delayed completion. Capture exact inferred members/quest/lost-input messages and
   the animation/audio/inventory ordering before promoting those details to verified.

## Lunar Diplomacy Suqah-leather clothing procedure

1. During Lunar Diplomacy's ceremonial-clothing stage, use a needle on Suqah leather in both item
   orders. Confirm the exact title and torso, trousers, gloves, boots, Cancel order. Select each
   garment and verify one leather becomes object 9097/9098/9099/9100 after exactly three ticks.
2. Test visible Crafting levels 60/61 for the torso, 58/59 for trousers and 51/52 for gloves and
   boots. Confirm the exact transcript failures and 35/30/25/25 XP awards. Repeat with boosts and
   drains to establish that the visible level, rather than base level, is authoritative.
3. Test quest progress 154, 155, 159, 160, 189 and completed state 190. Confirm 155 is the first
   production state that admits the menu, 189 remains valid and 190 gives the exact Oneiromancer
   rejection. Capture the exact pre-stage rejection and ordering against level/tool checks.
4. Make four ordinary leather items, then one Lunar garment with an ordinary needle and at least
   two thread spools. Confirm only the fifth combined leather action consumes one spool. Repeat in
   the reverse family order and across logout/world change to determine the production persistence
   boundary. Confirm the costume needle requires no thread and never consumes one.
5. Fill all 28 slots and confirm the one-for-one replacement succeeds. During the three-tick action,
   remove the needle, thread or leather; change quest state; move, enter combat, teleport, replace
   the interface, start another action, log out and die. Capture sound, sequence 1249 and exact
   animation/inventory/XP/message ordering, checking that every interruption remains atomic.

## Leather add-on Crafting procedure

1. Use steel studs on leather bodies and leather chaps in both item orders. At levels 40/41 and
   43/44, confirm the boosted-level boundary, members-only gate, three-tick completion and exact
   40/42 XP awards.
2. Use kebbit claws on leather, green, blue, red and black vambraces in both item orders. At levels
   31/32, confirm all five members-only transformations complete in three ticks and award exactly
   6 XP. This capture must explicitly resolve the dated Crafting overview's conflicting 5.5-XP cell.
3. Hold two or more complete input pairs and perform one use-on action. Confirm only one result is
   produced, no quantity chooser opens, sequence 1249 is correct, and the remaining pair is left
   untouched until another explicit use-on action.
4. Repeat with every inventory slot occupied. Confirm the two inputs atomically become one output.
   Then remove either input during the three-tick delay; walk, run, teleport, enter combat, replace
   interfaces, start every other Crafting batch, log out and die. Confirm safe cancellation with no
   partial items or XP.
5. Capture exact success/failure messages, sound identity, animation/inventory/XP ordering and any
   diary, collection or League-task credit before promoting this family beyond integration-tested.

## Leather-shield Crafting procedure

1. Use two hard leather, snakeskin and each dragon leather on its oak, willow, maple, yew, magic or
   redwood base shield in both item orders. Confirm each use-on action makes only one shield and no
   quantity interface appears, even when multiple complete input sets are held.
2. Test one level below and exactly at boosted Crafting levels 41/56/62/69/76/83 on member and free
   worlds. Confirm the member gate and exact 70/100/124/140/156/172 XP awards, with no partial item
   or XP changes on rejection.
3. For each recipe, confirm exactly two leathers, one wooden shield and 15 matching nails are
   consumed after five ticks. Repeat with 14 and 15 bronze/iron/steel/mithril/adamantite/rune nails,
   the wrong nail tier, wrong base shield and wrong leather to recover exact rejection precedence.
4. Confirm sequences 7386/7387/7831/7832/7833/7834 for hard leather through black dragonhide. In
   only the workspace RuneLite checkout, capture exact animation, sound, inventory, XP and message
   ordering for all six variants.
5. Repeat with a normal hammer and both Imcando hammer forms in inventory, main-hand and off-hand
   equipment. Remove or swap the active hammer during the delay, fill every inventory slot, remove
   each material, walk, run, teleport, enter combat, start every other Crafting action, log out and
   die. Confirm safe atomic cancellation and no duplicate output or XP.
6. Capture the exact initiating-item behavior, every success/failure message, sound identity,
   repeated-click behavior and applicable green/black shield League-task credit before promoting
   this family beyond integration-tested.

## Tannery service procedure

1. Open Ellis, the Crafting Guild Tanner, Sbott, Eodan and Chouani through their third operation.
   Confirm the Ranging Guild Leatherworker has no Trade operation, offers tanning only after the
   two opening dialogue lines when a tannable hide is held, and exposes every informational branch.
2. Confirm interface 324 shows the eight correct leather models, names, prices and All/X/5/1
   controls. Reopen it through different tanners and verify prices refresh without losing button
   functionality.
3. At a standard tanner, verify prices 1/3/15/20/20/20/20/20. At Sbott and Chouani verify
   2/5/25/45/45/45/45/45. At Eodan verify 10/30/150/200/200/200/200/200, then each easy through
   elite Kourend diary tier reduces every price by a further 20% of the base price.
4. Exercise all eight recipes with 1, 5, X and All, including a full inventory. Confirm the selected
   amount converts immediately, never grants Crafting XP, charges exactly per hide, preserves excess
   hides/coins and caps by the lesser of requested hides and affordable quantity.
5. Repeat the cowhide recipes on a free world and confirm they work. Confirm both snakeskins and all
   dragon leathers reject atomically on a free world. Also test zero hides, insufficient coins,
   noted hides, closing/replacing the modal and replaying a stale button packet.
6. Capture exact dialogue, success/failure text, sounds, interface/inventory/message ordering,
   restrictions during other activities and any achievement-diary or League-task credit before
   promoting the extended row.

## Elemental battlestaff Crafting procedure

1. Use water, earth, fire and air orbs on ordinary battlestaves in both item orders. Confirm the
   native one-product interface 270/clientscript 2046 shows the matching output and caps its quantity
   by the lesser held orb/staff count.
2. Test one level below and exactly at Crafting 54/58/62/66, including temporary boosts and a free
   world. Confirm the members gate and exact XP 100/112.5/125/137.5 without partial item changes.
3. Make at least three of each product. Confirm sequence 7531; water/earth/fire/air spot effects
   1370/1371/1372/306; the first result after three ticks; and every later result two ticks apart.
4. Remove the selected orb or battlestaff between results; select a different Crafting batch; walk,
   run, teleport, enter combat, replace interfaces, log out and die. Confirm cancellation, no
   duplicate products or XP, and no consumption after the interrupt.
5. Capture exact success/failure messages, sound identity, animation/effect/inventory/XP ordering,
   Farming Guild diary credit, specialty battlestaff recipes and POH crystal-ball conversions before
   promoting the extended row.

## Loom weaving Crafting procedure

1. Operate the Iorwerth, ordinary and repaired Fossil Island loom identities, then use balls of
   wool, jute fibres and willow branches directly on each. Confirm interface 270/clientscript 2046
   lists strip of cloth, empty sack, drift net and basket in that order and caps the quantity by the
   selected recipe's complete material sets.
2. Test one level below and exactly at Crafting 10/21/26/36, including temporary boosts and a free
   world. Confirm all four recipes are members-only and consume exactly 4 wool, 4 jute, 2 jute or 6
   branches for 12/38/55/56 XP without partial item changes.
3. Make at least three of each product. Confirm sequence 2270; cloth, sacks and baskets produce the
   first result after four ticks and later results three ticks apart; drift nets produce both first
   and later results three ticks apart.
4. Remove materials between results; select another Crafting batch; move away; walk, run, teleport,
   enter combat, replace interfaces, log out and die. Confirm safe cancellation, no duplicate
   products/XP and no consumption after interruption.
5. Capture exact success/failure messages, sound identity, animation/inventory/XP ordering, direct-
   input menu behavior, every world loom placement and any diary or League-task credit before
   promoting the extended row.

## Telekinetic Grab procedure

1. At Magic 32 and 33, cast on a public single item and a stackable pile with one air/one law rune.
   Confirm level failure is atomic, success consumes one of each rune, grants exactly 43 Magic XP
   and moves the entire pile only when the impact arrives.
2. Repeat beside the target and at ten tiles, then at eleven tiles. Place solid fences, gates and a
   river crossing between player and target. Confirm the maximum range, lack of line-of-sight gate,
   fixed projectile travel time and exact animation, graphic and sound ordering.
3. Use every air-rune substitute, rune pouch and Fountain of Rune. Confirm normal substitution,
   Fountain no-rune/no-XP behavior and full-inventory handling for both stackable and unstackable
   targets. Capture every exact failure message.
4. Have another player take the pile while the projectile travels; replace it with the same item
   type on the same tile before impact. Confirm the spell never deletes or duplicates the
   replacement and record the already-taken message.
5. Change plane, walk, run, teleport, enter combat, open/close an interface, log out and die during
   travel. Confirm which actions cancel, fizzle or allow the impact and capture the exact tick and
   message for each path.
6. Test Ahab's beer, raw pheasant, both Trouble Brewing flower colours, dragon token, both
   looting-bag states and every easy/medium/hard/elite equipable Achievement Diary reward. Confirm
   rejection occurs before runes/XP and capture the exact wording.
7. Test free-to-play worlds with members-only targets, Tower of Life basement objects, occupied
   farming patches and every Mage Training Arena telekinetic object/NPC. Record area-specific rune,
   point, movement, target and message overrides separately from the global spell.
8. Click three targets inside one three-tick window and determine queued-target replacement. Repeat
   near and far while recording outgoing projectile fields. Do not import later Sailing behavior
   into the cache-233 result.

## Jewellery enchantment procedure

1. At levels 7/27/49/57/68/87/93, cast each enchantment with its exact runes. Confirm
   17.5/37/59/67/78/97/110 Magic XP, components 13/24/37/46/61/74/77, sequences 719/720/721,
   spotanims 114/115/116/153/154/452, graphic height and every amulet/ring sound.
2. Exercise all 44 source/output pairs: all four jewellery forms for sapphire/opal, emerald/jade,
   ruby/red topaz, diamond, dragonstone, onyx and zenyte, plus the Silvthrill rod, pre-nature
   amulet and both enchanted sickles. Confirm exact charged/uncharged output states and slot choice.
3. Put two identical sources in different slots and target the later one. Confirm the first matching
   source transforms. Mix several valid types, allow automatic continuation and prove it repeats
   only the originally selected source every seven ticks.
4. Click the same valid source three times inside the three-tick manual delay. Confirm one immediate
   cast, exactly one queued cast and replacement of the pending automatic repeat. Interrupt
   automatic and queued casts separately with walking, running, combat, teleport, interface change,
   inventory mutation, logout and death; record exact ticks and whether each action cancels.
5. On a free world, confirm only sapphire, emerald, ruby and diamond amulets work. Test every other
   valid target and every level-5–7 spell, capturing the exact membership text and proving atomic
   rune/item/XP state.
6. Try a ruby necklace before and after discovering the Digsite pendant; record varbit 3644 and the
   exact rejection. Try a pre-nature amulet while a nature amulet is in inventory, equipped, banked
   and in any other storage; capture the exact ownership scope and message.
7. Repeat with elemental staves, combination runes, rune pouches and the Fountain of Rune. Confirm
   normal substitution and Fountain no-rune/no-XP behavior at every tier, including blood/soul
   requirements for Lvl-7 Enchant.
8. Cast each level on invalid items and every applicable Mage Training Arena shape. Record exact
   messages, point/rune rules, movement, automatic behavior and the activity state required. Pay
   special attention to level 7's graphic/sound, semi-precious sounds and client/server ordering;
   these are the current explicit inference boundaries.

## Crossbow bolt enchantment procedure

1. Open spellbook child 218:10 with only regular tipped bolts held. Confirm interface 270 runs
   clientscript 2046 with action `Enchant`, the title `How many sets of bolts to enchant?`, regular
   product icons and ten enabled choice buttons. Repeat with only dragon tipped bolts and confirm all
   ten icons switch to the dragon outputs.
2. At Magic levels 4/7/14/24/27/29/49/57/68/87, cast Opal/Sapphire/Jade/Pearl/Emerald/Red
   Topaz/Ruby/Diamond/Dragonstone/Onyx with the pinned rune costs. Confirm
   9/17.5/19/29/37/33/59/67/78/97 XP and test both regular and dragon input/output pairs.
3. Enchant stacks of 1, 7, 10, 11 and 20 bolts. Confirm every cast transforms at most ten, a partial
   final set still consumes one full rune set and grants full XP, and the selected quantity stops
   exactly after that many casts even when bolts and runes remain.
4. Select several sets and record the immediate first cast plus three-tick repeats. Rapidly click a
   child spell and confirm a new manual cast is accepted after one tick. Mine and fish before and
   during both paths; confirm neither cast stops the skilling action, then repeat while walking,
   running, fighting and changing interfaces.
5. Hold only regular bolts, only dragon bolts, and both families for the same and different gems.
   Record the product-icon family, source priority and displayed maximum for every mixture. These
   are the current inference boundaries; compare the capture before changing their rules.
6. Repeat below level, on a free world, without each rune, without compatible bolts and with a full
   inventory. Confirm exact failure messages, check ordering and atomic rune/item/XP state. Repeat
   with staves, combination runes, rune pouches and the Fountain of Rune.
7. Record sequence 4462, spotanim 759 including height, synth 2921, the exact success message, and
   the relative animation/sound/inventory/XP ordering in the workspace RuneLite.

## Elemental orb-charging procedure

1. At Magic levels 56/60/63/66, bring unpowered orbs, three cosmic runes per cast and thirty
   Water/Earth/Fire/Air runes to the matching obelisk. Confirm 66/70/73/76 Magic XP, one same-slot
   Water/Earth/Fire/Air orb output and no unrelated inventory movement.
2. Record spellbook children 45/49/56/59, player sequence 726, spotanims 149/151/152/150 and synths
   118/115/117/116. Capture the exact animation, graphic, sound and inventory/XP ordering for each
   element in the workspace RuneLite.
3. Fill all 28 inventory slots, keeping the required stacked runes and one unpowered orb. Confirm
   charging succeeds by replacing the orb's source slot. Repeat without an orb, without each rune,
   below the required level, on a free world and against each wrong obelisk; capture exact messages
   and prove no rune, item or XP state changes on failure.
4. Repeat with each elemental staff, combination runes, rune pouch variants and the Fountain of
   Rune. Confirm ordinary substitutions and Fountain no-rune/no-XP behavior while one unpowered orb
   is still consumed.
5. Click repeatedly inside the three-tick manual delay. Confirm one immediate cast and exactly one
   queued cast. Allow automatic continuation and confirm one orb every six ticks until the source
   runs out.
6. Interrupt the queued and automatic paths separately by walking, running, combat, teleport,
   interface change, inventory mutation, obelisk removal/morph, logout and death. Record exact tick
   boundaries and test replacement by every other utility-spell queue.
7. Confirm the Water cast awards the hard Kandarin task, Earth the medium Wilderness task and Air
   the hard Wilderness task exactly once when the corresponding diary system is present. Fire has
   no listed elemental-orb diary task at this cutoff.

## Monster Inspect procedure

1. On a free world, test Magic levels 41 and 42 with exactly two body and two mind runes. Confirm
   failure is atomic, then confirm one successful cast consumes the exact runes, grants 30.5 Magic
   XP and opens the inspection interface. Repeat with every normal rune substitute and at the
   Fountain of Rune, where neither runes nor XP should change.
2. Target the same monster with boosted and drained current combat stats. Confirm the Stats tab
   shows current Hitpoints, Attack, Defence, Strength, Magic and Ranged rather than base values,
   plus the correct combat level.
3. Compare the Aggressive and Defensive tabs against cache values. Record exact signs, percent
   formatting, labels and omission of zero attack bonuses; confirm per-style melee attack bonuses,
   max hit, attack style and flat armour are not displayed.
4. Exercise NPCs covering Slayer eligibility, full/partial poison and venom immunity, thrall and
   burn immunity/tier, freeze resistance, demonic, draconic, vampyric/tier, spectral, rat, golem and
   each elemental weakness. Include morphing NPCs and the four Phantom Muspah combat forms.
5. Cast on attackable, non-attackable, dead/despawning and morphing NPCs at ten and eleven tiles,
   with and without line of sight. Confirm target eligibility, reach behavior and safe cancellation
   if the target changes before processing.
6. In only the workspace RuneLite checkout, capture exact label text, capitalization, row order and
   omission rules, along with sequence 6293, spotanim 1059, synth 3620, interface 522 and
   clientscript 1179. Record animation/sound/interface/script ordering, repeated-click behavior,
   interruption, modal replacement, logout and death behavior.

## First ten fixed Standard teleports procedure

1. Use only the workspace RuneLite checkout. On a free world, test Varrock at Magic 24/25,
   Lumbridge at 30/31 and Falador at 36/37 with exact runes. Confirm atomic failure, then exact rune
   use, 35/41/48 XP and membership availability. Confirm Camelot rejects on the free world and works
   on a members' world at Magic 45 for five air/one law and 55.5 XP.
2. Repeat every spell with elemental staves and all valid combination/substitute rune paths. At the
   Fountain of Rune, confirm neither runes nor XP change. Record exact missing-level, rune and
   membership messages and their check order.
3. At Wilderness levels 20 and 21, and with active, immunity-only and absent Tele Block states,
   record exact acceptance, rejection text and ordering. Repeat underground and after crossing a
   Wilderness boundary to validate the production region updater once it exists.
4. Capture player sequence 714, spotanim 111 and synth 200 and record their exact start/end order.
   Count the server movement cycle, animation lock, repeated-click behavior, walking/running/combat
   interruption, logout and death handling.
5. Sample every landing square repeatedly to recover each base landing area and distribution. After
   the medium Varrock and hard Kandarin diary unlocks, test the Grand Exchange and Seers' Village
   destination toggles, persistence, spell tooltip/menu state and landing areas. The server-side
   selection from completion plus saved-toggle state is automated; this manual pass is for the
   configuration flow, exact menu feedback and random landing polygons.
6. On a members' world, test Kourend Castle at level 48, Ardougne at 51, Civitas illa Fortis at 54,
   Watchtower at 58, Trollheim at 61 and Ape Atoll at 64. Test one progression state below and
   exactly at each unlock. For Ardougne and Watchtower, separately test quest completion before and
   after reading the reward scroll.
7. For Ape Atoll, confirm an unnoted banana is required and consumed together with two fire, two
   water and two law runes. Confirm a noted banana is rejected and that missing the banana leaves
   every rune and all XP untouched. Repeat at the Fountain of Rune and confirm only the banana is
   consumed.
8. Confirm Kourend lands in the castle courtyard, Ardougne in East Ardougne market, Civitas in the
   city, Watchtower at the tower, Trollheim on the mountain and Ape Atoll inside the Temple of
   Marimbo. After the hard Ardougne diary, test Watchtower's Yanille toggle and both destinations;
   the server-side base/alternate selection branches are automated.

## Lumbridge Home Teleport procedure

1. Use only the workspace RuneLite checkout. Cast on both free and members worlds with no runes and
   confirm zero Magic XP, the Lumbridge destination and movement exactly 22 game ticks after click.
2. Capture the five default phases at ticks 0/4/7/10/13: sequences 4847/4850/4853/4855/4857,
   spotanims 800/802/803/804, synths 193/196/194/195 and portal height 65526. Record exact reset and
   loading-zone behavior at the destination.
3. Recast immediately, after one minute, after 29 minutes and at 30 minutes. Confirm the exact
   remaining-minute text, icon dimming, persistence across logout/world change and that switching
   spellbooks cannot bypass the shared cooldown.
4. Interrupt each phase separately with walking, running, combat, inventory and location actions,
   interfaces, another spell, logout and death. Confirm no cooldown is consumed and record exact
   animation/graphic cleanup and any message.
5. Attempt the cast during and exactly ten seconds after combat, at Wilderness levels 20 and 21,
   and with active, immunity-only and absent Tele Block. Record exact rejection ordering/text.
6. Exercise every available Home Teleport animation override, especially Speedy, and recover each
   phase/tick/obstruction rule before implementing those variants.
7. Switch to Ancient Magicks on a members world, move to F2P, and confirm Edgeville Home still lands
   within 3087..3090 by 3501..3504. Confirm Lunar and Arceuus reject on F2P, then land around
   2113,3915 and 1700,3882 respectively on members worlds. Prove all four share one cooldown.
8. Attempt Lumbridge Home from the H.A.M. Hideout cell and across the Draynor Manor grounds to map
   the cutoff-only restricted tiles and exact rejection message before implementing those regions.

## Standard log-cutting Fletching procedure

1. In only the workspace RuneLite checkout, use an ordinary knife on normal, oak, willow, maple,
   yew, magic, teak, mahogany and redwood logs in both item orders. Record interface 270,
   clientscript 2046, exact product order, labels, unavailable-product state, remembered quantity
   and the displayed maximum for one-log and two-log products.
2. Produce every listed product at one level below and exactly at its required boosted level.
   Confirm the 35 recipe levels, one/two-log costs, arrow/javelin shaft stack counts and XP values
   against `SOURCE_LEDGER.md`, including fractional XP accumulation.
3. With an ordinary knife, select three or more products and confirm the first result after two
   ticks followed by one result every three ticks. Repeat with Fletching knife 31043 in inventory
   and confirm the first result remains two ticks while repeats become two ticks.
4. Equip the Fletching knife in the off-hand with an ordinary knife in inventory and repeat the
   timing capture. Then remove all inventory knives while leaving it equipped and confirm cutting
   cannot be initiated. Start a batch first, equip the special knife, and establish whether the
   speed transition applies immediately or only to the next scheduled action.
5. Repeat on a free world, below level, without a knife, with only one log for a shield, with a full
   inventory and with insufficient space for a shaft stack. Capture exact check ordering/messages
   and prove every rejected or failed result leaves logs, products and XP unchanged.
6. Interrupt ordinary and special-knife batches with movement, running, combat, interface changes,
   tool/log removal, another Fletching action, logout and death. Record the precise stop boundary,
   sequence reset and cross-production replacement behavior.
7. Capture sequence 1248 and any sound, then record animation, inventory transform, XP and message
   ordering for the first and repeated results. Treat these captures—not the current canonical
   registry order—as the authority for future presentation refinements.

## Standard bow-stringing Fletching procedure

1. In only the workspace RuneLite checkout, combine loose bow string with every normal-through-
   magic unstrung shortbow and longbow in both item orders. Record the exact interface-270 title,
   object model, enabled operations, remembered quantity, maximum and clientscript-2046 payload.
2. Test one level below and exactly at levels 5/10/20/25/35/40/50/55/65/70/80/85. Confirm one
   unstrung bow and one string become the matching bow, with shortbow XP
   5/16.5/33.2/50/67.5/83.2 and longbow XP 10/25/41.5/58.2/75/91.5.
3. String at least three bows and confirm results exactly every two ticks, including the first.
   Capture sequences 6678–6689 for their matching products and record animation, inventory, XP,
   sound and message ordering.
4. Repeat on a free world, below level, without either material, with mismatched inputs, from a full
   inventory and after removing each material between results. Confirm exact failure ordering/text,
   atomic state and animation reset.
5. Start log cutting during a stringing batch and stringing during log cutting. Confirm replacement
   at the observed boundary, then repeat with movement, running, combat, another interface, logout
   and death to complete the wider interruption matrix.
6. Exercise implemented bow-string spool 31052 behavior: Fill and Check, loose and noted deposits,
   combining through all six capacities, use on every unstrung bow, empty-charge behavior,
   loss/reacquisition and account persistence. Confirm each spool-supplied bow consumes one charge
   on the same two-tick result boundary as loose string.
7. Separately capture Empty Strings/Empty Notes, bank-proximity withdrawal and automatic
   flax-spinning deposit. Record the exact bank-area eligibility, option flow, messages, display
   varbits and cross-skill ordering before implementing these remaining spool integrations.

## Composite-ogre-bow stringing procedure

1. Using only the workspace RuneLite checkout against the isolated cache-233 server, reach the
   point where Grish teaches composite-ogre-bow production, then combine unstrung comp bow 4825 and
   bow string 1777 in both item orders. Confirm interface 270 offers comp ogre bow 4827.
2. At boosted levels 29 and 30, confirm one result consumes one of each material exactly two ticks
   after selection and awards 45 Fletching XP. Capture sequence 4433, sound, messages and packet
   ordering; repeat before and after learned-production varbit 500 is set.
3. Repeat from a full inventory and with the bow-string spool holding one charge. Confirm the loose
   route frees one slot, the spool route consumes exactly one account charge, and both remain atomic
   when interrupted or when a material/charge disappears during the two-tick delay.
4. Separately run the implemented preceding knife-on-Achey-log and wolf-bone stage, capturing its
   exact shared menu presentation, single-action behavior, timing and animation before promoting
   that row beyond integration-tested.

## Standard ammunition Fletching procedure

1. In only the workspace RuneLite checkout, combine shafts/feathers, headless arrows/every ordinary
   arrowhead, feathers/every unfinished ordinary or broad bolt, and feathers/every dart tip in both
   item orders. Confirm cache IDs and the 28 implemented results against `SOURCE_LEDGER.md`.
2. For headless arrows and each finished arrow, record interface 270 and clientscript 2046: exact
   title, set count, model, remembered selection, one-material behavior and the ten-set maximum.
   Confirm one result set every two ticks and correct partial-final-set behavior.
3. Toggle cache varbit 10971 through the normal Gameplay setting. With it disabled, prove bolt and
   dart use makes one set immediately without stopping movement. With it enabled, record the menu
   payload and confirm up to ten sets complete at two ticks per set. Establish the option's exact
   default for newly created accounts separately.
4. Test one below and exactly at every level, and confirm XP per produced item including partial
   sets. Prove no Tourist Trap state is required for feathering acquired dart tips. For broad arrows
   and bolts, test both sides of Broader Fletching varbit 3208.
5. Capture sequences 8463–8471, 8480–8488 and 9108 with the matching recipe. Record sound and the
   animation, inventory, XP and message order. Pay special attention to which cached bolt animation
   the broad-bolt recipe reuses.
6. Repeat with missing materials, a free world, full stacks, movement, combat, another interface,
   another Fletching action, logout and death. Record exact failure text and stop boundaries.
7. Combine every bronze-through-dragon javelin tip with shafts in both item orders. Confirm the
   one-tick first result, one-tick second result and two-tick steady repeats, partial final sets,
   ten-set quantity cap, absence or identity of any animation, and uninterrupted movement. Capture
   exact menu wording, sound and inventory/XP packet ordering before promoting the javelin row.
8. Capture poisoned, ogre and other special ammunition independently rather than
   inheriting standard timings or presentation.

## Poisoned-ammunition procedure

1. Using only the workspace RuneLite checkout against the isolated cache-233 server, combine each
   of weapon poison 187, weapon poison(+) 5937 and weapon poison(++) 5940 with representative
   arrows, ordinary bolts, darts, javelins and throwing knives in both held-item orders. Confirm the
   result is synchronous, has no skill requirement or XP award, consumes one poison, treats exactly
   five items and returns empty vial 229.
2. Exercise the full automated registry: eight arrow bases, eight ordinary-bolt bases, nine dart
   bases, eight javelin bases and eight throwing-knife bases, for 123 transforms. Confirm broad,
   tipped, enchanted and dragon bolts and other unsupported ammunition do not register poison
   interactions. Verify the cache's 2–5 quantity-display objects are never stored as recipe output.
3. Repeat with stacks of one through four, exactly five, six and a large stack. Capture whether a
   partial stack is accepted and whether one vial is always consumed. The implementation currently
   treats up to five and records acceptance of smaller stacks as an explicit inference.
4. Test a free world, an otherwise full inventory with exactly five ammunition, and a full inventory
   retaining more than five ammunition with no existing output or empty-vial stack. Confirm members-
   only enforcement, successful two-slot replacement in the first full-inventory case, atomic
   capacity rejection in the second and safe repeated/spam-click interactions.
5. Begin every queued Fletching family before poisoning ammunition, then repeat while moving, in
   combat, with another interface open, across logout and across death. Capture exact success and
   failure messages, sound, any animation, interruption boundaries and inventory packet ordering.

## Brutal-arrow Fletching procedure

1. In only the workspace RuneLite checkout, combine six flighted ogre arrows 2865 with six each of
   bronze, iron, steel, black, mithril, adamant and rune nails in both item orders while carrying a
   hammer. Confirm outputs 4773/4778/4783/4788/4793/4798/4803 and the exact per-arrow XP contracts.
2. Confirm no quantity interface opens, sequence 4433 starts immediately and the six arrows appear
   two ticks later. Repeat with one through five available pairs to verify partial-set behavior and
   with an otherwise full inventory to verify atomic replacement.
3. Test below and at boosted levels 7/18/33/38/49/62/77, in a free world, without a hammer, and on
   both sides of Zogre learning varbit 499. Confirm failures consume nothing and award no XP.
4. Remove the hammer, arrows or nails during the two-tick action; interrupt with movement, combat,
   another interface, every other Fletching family, logout and death. Capture exact message text,
   filtering, sound, animation duration and animation/inventory/XP packet order.

## Ogre-arrow Fletching procedure

1. In only the workspace RuneLite checkout, combine six flighted ogre arrows 2865 with six
   wolfbone arrowtips 2861 in both item orders. Confirm six ogre arrows 2866, 6 total Fletching XP
   and no quantity interface.
2. Repeat at Big Chompy varp-293 states 0, 1, 64 and 65. Confirm only state 0 rejects, matching the
   cache quest table's unstarted value, while every started/completed state works.
3. Confirm sequence 4433 begins immediately and the result appears exactly two ticks later. Repeat
   with one through five available pairs and an otherwise full inventory to verify partial-set and
   atomic replacement behavior against production.
4. Test below and at boosted Fletching level 5 and on free/member worlds. Remove either input during
   the action; interrupt with movement, combat, another interface, every other Fletching family,
   logout and death. Capture exact messages, sound, animation duration and packet order.

## Bolt-tip cutting Fletching procedure

1. In only the workspace RuneLite checkout, use a chisel with opal, jade, both oyster-pearl forms,
   red topaz, sapphire, emerald, ruby, diamond, dragonstone and onyx in both item orders. Confirm
   all eleven level/yield/XP contracts and cache identities in `SOURCE_LEDGER.md`.
2. Record interface 270 and clientscript 2046 for each one-product quantity menu: exact title,
   model, initial and remembered quantity, maximum, single-source behavior and repeated clicks.
3. Confirm every selected source transforms exactly five ticks apart, including the first result.
   Capture gem sequences 885–892 and 2717 plus pearl sequence 4470, sound, message and the precise
   animation/inventory/XP packet order.
4. For dragonstone, prove the title and both choices against transcript revision 14714344. Accept
   and record its transition into quantity selection; decline and confirm the source is retained
   and the exact decline line is sent. Verify independently that onyx has no value warning.
5. Repeat one level below and exactly at all ten levels, on a free world, without a chisel, with a
   full inventory, and after removing the tool/source between results. Interrupt with movement,
   combat, another interface, every other Fletching family, logout and death.
6. Test amethyst separately as Crafting using the procedure below rather than inheriting this
   Fletching implementation.

## Amethyst-cutting Crafting procedure

1. In only the workspace RuneLite checkout, use a chisel with amethyst in both item orders. Record
   the four-product interface 270 / clientscript 2046 menu, product order, exact title, model,
   initial and remembered quantity, maximum, Make-X behavior and repeated-click behavior.
2. Select bolt tips, arrowtips, javelin tips and dart tips one level below and exactly at levels
   83/85/87/89. Confirm the 15/15/5/8 outputs, 60 Crafting XP per amethyst, members gate, source and
   chisel checks, and that every transform is exactly two ticks apart including the first result.
3. Capture sequence 6295, sound, messages and the exact animation/inventory/XP packet ordering for
   every product. Confirm that a full inventory remains safe because one non-stackable source is
   replaced by a stackable output.
4. Remove the chisel or source between results, then interrupt with movement, combat, another
   interface, every other Crafting family, logout and death. Record exact stop boundaries and prove
   all rejected actions remain atomic before promoting this row beyond integration-tested.

## Tipped-bolt Fletching procedure

1. In only the workspace RuneLite checkout, attach all twelve standard/special tip families to
   their matching bronze-through-runite or broad bolts in both item orders. Repeat all ten supported
   gem tips with dragon bolts; prove barbed and amethyst tips do not register against dragon bolts.
2. Test one level below and exactly at every standard level, then at dragon level 83/84. Confirm the
   22 level, input, output and per-bolt XP contracts in `SOURCE_LEDGER.md`, including topaz's
   standard 3.9 versus dragon 4.0 XP and a final partial set below ten.
3. Confirm every result appears exactly two ticks after initiation. Capture sequences 8472–8479
   against their exact base tier, including sequence 8473 for pearl/iron and amethyst/broad.
   Confirm the June 2024 movement-safe dragon animation behavior.
4. Toggle Make-X varbit 10971 through the normal Gameplay setting. With it disabled, prove one set
   is scheduled without a menu; with it enabled, record interface 270/clientscript 2046 payload,
   remembered quantity and up-to-ten-set cadence. Establish the new-account default separately.
5. Test amethyst broad bolts before and after Broader Fletching varbit 3208. Repeat every family on
   a free world, below level, with either material missing, from a full inventory and after material
   removal between queued sets; confirm all failures are atomic.
6. Record exact menu wording, any messages and sound, animation/inventory/XP packet order, repeated
   clicks, movement/running/combat/interface/logout/death interruption and League/diary/task hooks
   before promoting this row beyond integration-tested.

## Standard crossbow-assembly Fletching procedure

1. In only the workspace RuneLite checkout, combine every matching stock and limb pair from bronze
   through dragon in both item orders with a normal hammer in inventory. Record interface 270,
   clientscript 2046, exact title, displayed model, remembered quantity and maximum.
2. Repeat one level below and exactly at levels 9/24/39/46/54/61/69/78. Confirm one stock and one
   limb become the matching unstrung crossbow for 12/32/44/54/64/82/100/135 XP. Establish that
   assembly of acquired blurite limbs has no Knight's Sword state gate.
3. Attach crossbow string to every unstrung crossbow in both item orders. Confirm the matching
   finished result and 6/16/22/27/32/41/50/70 XP. Confirm both stages produce the first and every
   repeated result exactly two ticks apart.
4. Capture limb sequences 4436–4442 and 7860 and stringing sequences 6671–6677 and 7961 against
   their exact tiers. Record success-message punctuation, filtering, sound, animation, inventory
   and XP packet order; pay special attention to dragon limb sequence 7860's legacy symbol name.
5. Repeat limb attachment with the Imcando hammer in inventory and equipped in each valid hand.
   Test free worlds, boosted levels, missing or mismatched materials/tools, full inventories and
   material/tool removal between results. Confirm all failures remain atomic.
6. Interrupt both stages with movement, running, combat, another interface, each other Fletching
   family, logout and death. Record the exact stop boundary, sequence reset, diary/League-task hooks
   and whether the client retains quantity selection between tiers.

## Ballista-assembly Fletching procedure

1. In only the workspace RuneLite checkout, combine light frame/ballista limbs, incomplete light
   ballista/ballista spring and unstrung light ballista/monkey tail in both item orders. Repeat the
   three equivalent heavy stages and confirm every cache identity in `SOURCE_LEDGER.md`.
2. Test one level below and exactly at boosted levels 47 and 72, plus a free world. Confirm the
   light stages award 15/15/300 XP and the heavy stages 30/30/600 XP, with totals 330 and 660.
   Establish independently whether assembling tradeable parts has any Monkey Madness II gate.
3. Record the exact result tick for every stage, whether any quantity interface appears, all
   messages and sounds, and the inventory/XP packet order. Confirm no cached combat-only ballista
   sequence is played during assembly unless the matching client actually demonstrates it.
4. Repeat with each component missing or mismatched, a full inventory, movement, combat, another
   interface, each other Fletching family, logout and death. Confirm rejected actions remain atomic
   and record whether another action cancels or can coexist with each synchronous assembly stage.

## Celastrus-battlestaff Fletching procedure

1. In only the workspace RuneLite checkout, carry a normal knife and several Celastrus barks. Use
   the bark's `Fletch` operation, then repeat using the knife on the bark in both held-item orders.
   Confirm no quantity menu opens, sequence 1248 begins and one bark becomes one battlestaff.
2. Confirm the first result lands four ticks after initiation and ordinary repeats remain four ticks
   apart. Repeat with the Fletching knife first in inventory and then equipped off hand: the first
   result must remain four ticks and later results must be three ticks apart.
3. Test boosted levels 39 and 40, a free world, no knife, removal of the knife or bark between
   results, one bark in an otherwise full inventory, spam-clicking, movement, combat, another
   interface, each other Fletching family, logout and death. Confirm failures and interruptions do
   not consume bark or award partial XP.
4. Capture exact success/failure messages, filtering, audio, sequence duration, inventory and XP
   packet order, whether the selected tool changes client presentation, and whether native Fletch
   batches the whole inventory or observes any hidden quantity preference.

## Ogre-arrow-shaft Fletching procedure

1. Using only the workspace RuneLite checkout against the isolated cache-233 server, start Big
   Chompy Bird Hunting and use ordinary knife 946 on Achey tree logs 2862 in both item orders.
   Repeat with Fletching knife 31043. Confirm the native single-action menu exposes ogre arrow
   shafts 2864 first and unstrung comp bow 4825 second; capture the exact title, ordering and
   disabled-choice behavior before treating that presentation as verified.
2. Confirm no inventory or XP change after two ticks, then one log becomes between two and six ogre
   arrow shafts 2864 after the third tick. Verify exactly 1.6 Fletching XP per shaft and confirm a
   second log is not processed without another held-use action.
3. Record at least several hundred results to determine whether all five outcomes are uniformly
   weighted. Repeat at boosted levels 4 and 5, before and after starting Big Chompy, on free/member
   worlds, without either knife, and after removing the log or knife during the delay.
4. Repeat with one knife and 27 logs in a full inventory. Confirm the consumed log's slot safely
   receives the stackable shafts. Exercise every other Fletching queue before and after the action
   to confirm mutual cancellation.
5. Capture exact success/failure messages, sound, animation duration, spam-click, movement, combat,
   interface, logout and death boundaries, and inventory/XP packet ordering.

## Composite-ogre-bow cutting procedure

1. In the same shared Achey-log menu, choose unstrung comp bow before and after Grish sets learned-
   production varbit 500. With one log, one wolf bone 2859 and either knife, verify boosted level 29
   rejects atomically and level 30 starts sequence 4433.
2. Confirm no inventory or XP change after two ticks, then exactly one log and one bone become
   unstrung comp bow 4825 after the third tick for exactly 45 Fletching XP. Keep a second material
   pair in inventory and confirm it is not processed without another selection.
3. Repeat both knife identities and both held-item orders, including one log and 26 bones alongside
   the Fletching knife in a full inventory. Remove each input/tool during the delay and exercise
   every other Fletching queue before and after selection; confirm every failure is atomic.
4. Capture the exact unlock/level/material messages, menu filtering, animation and sound identity,
   inventory/XP packet ordering, spam-click behavior and movement/combat/interface/logout/death
   interruption boundaries before promoting either shared-menu row to client-tested.

## Flighted-ogre-arrow Fletching procedure

1. Using only the workspace RuneLite checkout against the isolated cache-233 server, start Big
   Chompy Bird Hunting and use ordinary feathers 314 on ogre arrow shafts 2864 in both item orders.
   Confirm no quantity interface opens and sequence 4433 begins immediately.
2. With six shafts and 24 feathers, confirm nothing changes after one tick, then exactly six
   flighted ogre arrows 2865 appear after the second tick. Verify consumption of all 24 feathers
   and exactly 5.4 Fletching XP.
3. Repeat with four shafts and 17 feathers in an otherwise full inventory. Confirm four outputs,
   one remaining feather, 3.6 XP and no dropped or lost item. Try one shaft with three feathers and
   confirm the action rejects before a queue starts.
4. Repeat before starting Big Chompy, on a free world, at boosted levels 4 and 5, and after removing
   material during the two-tick delay. Capture the exact messages, sounds, animation duration,
   inventory/XP packet order and interruption behavior before promoting the inferred details.

## Wolfbone-arrowtip Crafting/Fletching procedure

1. Using only the workspace RuneLite checkout against the isolated cache-233 server, partially
   complete Big Chompy Bird Hunting and use chisel 1755 on wolf bones 2859 in both item orders.
   Confirm sequence 4433 begins immediately and no inventory or XP change occurs after one tick.
2. Confirm one bone becomes between two and six wolfbone arrowtips 2861 after the second tick.
   Record at least several hundred results to determine whether all five outcomes are uniformly
   weighted. For every result, verify exactly 2.5 Crafting XP and 2.5 Fletching XP per tip.
3. Repeat below and at boosted level 5 separately in Crafting and Fletching, before and after
   starting Big Chompy, on free/member worlds, without the chisel, and after removing the bone or
   chisel during the delay. Confirm rejected and interrupted actions consume nothing and award no
   XP.
4. Repeat with 27 wolf bones plus the chisel in a full inventory. Confirm the consumed bone's slot
   safely receives the stackable tips. Start each Crafting and Fletching queue before this action,
   then begin each other skill action after it, to confirm mutual cancellation.
5. Capture whether the live client automatically continues through multiple bones; exact success
   and failure messages; audio; animation duration; spam-click, movement, combat, interface,
   logout and death interruption boundaries; and inventory/XP packet ordering.

## Toxic-blowpipe creation Fletching procedure

1. Using only the workspace RuneLite checkout against the isolated cache-233 server, use chisel 1755
   on Tanzanite fang 12922 and repeat in reverse order. Confirm the action is immediate, plays
   sequence 3015, consumes one fang, creates empty Toxic blowpipe 12924 rather than charged form
   12926, and awards exactly 120 Fletching XP.
2. Repeat at boosted levels 77 and 78, in members and free worlds, without the chisel, without the
   fang, and with one fang in an otherwise full inventory. Confirm every rejected action is atomic
   and the successful full-inventory replacement does not drop or lose an item.
3. Begin each queued Fletching family and then make a blowpipe. Confirm the prior queue stops and
   only one fang is transformed per click. Repeat while moving, in combat, with another interface
   open, while spam-clicking, and across logout/death to capture exact interruption boundaries.
4. Capture exact success and failure messages, sound, animation duration, and the ordering of the
   inventory, animation and XP updates.

## Toxic-blowpipe charging and item-lifecycle procedure

1. Using only the workspace RuneLite checkout against the isolated cache-233 server, use Zulrah's
   scales 12934 on both empty blowpipe 12924 and loaded blowpipe 12926 in both item orders. Repeat
   with bronze, iron, steel, mithril, adamant, rune, dragon, black and amethyst darts. Confirm
   poisoned darts are rejected and the charged form retains the exact selected dart type.
2. Load below, at and beyond 16,383 scales and darts independently. Confirm only the remaining
   capacity is consumed, further input is inert, and adding more of the same dart increases its
   count. With another dart type already loaded, add a new type and confirm every old dart is
   returned while the new stack is loaded up to capacity. Repeat with a full inventory, both with
   and without a free/mergeable stack for the returned darts, and confirm failures are atomic.
3. Check object 12926 from inventory and while equipped. Record the exact messages, pluralisation,
   percentages, ordering and whether zero scales or zero darts alter the presentation. Use Unload
   on scale-only, dart-only and mixed states; confirm it returns only darts and changes to empty
   object 12924 only when no scales remain.
4. Exercise Uncharge on scale-only, dart-only and mixed states. Decline and confirm the dialogue,
   move or replace the item while the dialogue is open, and repeat with zero, one and two available
   output slots or existing output stacks. Confirm successful uncharging returns everything,
   produces object 12924 and plays synth 2381; every failure must preserve the packed state.
5. Dismantle empty object 12924 with and without a full inventory. Confirm the warning, cancellation
   path and exact return of 20,000 scales. Capture all ordinary loading, unload, uncharge and
   dismantle messages and inventory/sound/packet order before promoting presentation to verified.
6. Use Trailblazer reloaded blowpipe ornament kit 28690 on standard empty object 12924 in both item
   orders. Confirm the zero-tick result is blazing empty object 28687 and synth 2655 plays. Confirm
   a charged standard blowpipe rejects the kit. Charge 28687 with every dart tier and scales, swap
   dart types, Check from inventory/equipment, Unload and Uncharge; verify every transition retains
   the blazing style and Uncharge returns to 28687. Dismantle 28687 and verify it returns object
   12924 plus kit 28690, never scales; repeat with no free slot and confirm the action is atomic.
   Capture exact application/removal wording, confirmation and packet order.
7. Separately test bank `Configure-Charges`, automatic picked-up-scale routing,
   trade/death/grave behavior, depletion while equipped, scale/dart combat consumption, ranged
   bonuses, venom and the special attack. These remain open and must not be inferred from the
   inventory lifecycle.

## Prayer remains procedure

1. On a free world, bury ordinary bones 526, burnt bones 528, big bones 532, newbiebones 2530,
   monkey bones 3183 and tutorial bones 24655. Scatter fiendish ashes 25766 and vile ashes 25769.
   Confirm one item is consumed, the exact fine XP is awarded immediately, and members-only
   remains reject atomically.
2. On a members world, sample every ordinary Bury and Scatter object in the cache audit. Confirm
   the XP table in `SOURCE_LEDGER.md`, bone animation 827, bone synth 2738 and the exact two-tick
   same-family cadence. Rapidly click one bone and one ash in the same tick and confirm both are
   consumed and both XP awards occur.
3. With boosted Prayer 70 but base Prayer 69, attempt superior dragon bones 22124 and confirm no
   consumption or XP. At base Prayer 70, confirm one bone is consumed for 150 XP.
4. Bury long bone 10976 and curved bone 10977 with warning varbit 16616 clear. Exercise decline,
   confirmation and changing the selected slot while the dialogue is open; every rejected/stale
   path must remain atomic. Set the preference varbit and confirm the warning is bypassed. Capture
   the production warning title, choices, checkbox/control and packet order for later replacement.
5. Capture demonic-ash scattering with the revision-matched workspace RuneLite: exact animation,
   sound, messages and client event ordering. Do not promote presentation beyond
   integration-tested until these identities are authoritative.
6. Verify quest/minigame objects 610, 3127–3133, 4488, 25199 and 26589–26593 are inert without their
   owning scripts and are never consumed by the generic Prayer-remains handler.
7. In a POH chapel, use ordinary bones on each oak-through-gilded altar tier and confirm the base
   multipliers are 100/110/125/150/175/200/250%. Repeat with zero, one and two lit incense burners;
   confirm each adds 50 percentage points and the messages progress from accepted, to pleased, to
   very pleased. Repeat with a gnome child icon altar of every tier.
8. Put two stacks/types of bones in non-adjacent inventory slots. Use the later matching bone on a
   POH altar and confirm the first top-left bone of that type is consumed. Confirm the first manual
   result occurs after one tick, automatic results every three ticks, a new manual click restarts
   that cadence, and moving away or removing the altar stops cleanly.
9. At the level-38 Wilderness temple Chaos altar, confirm each offered bone grants 350% burial XP
   and independently has a 50% chance to remain, with `Your sacrifice is spared!` on that outcome.
   Use bones on another placement of location 411 and confirm the action is rejected without XP or
   consumption. Confirm manual use consumes the exact selected slot rather than POH top-left order.
10. Equip zero through four Zealot pieces and statistically sample burial, POH and Chaos offerings.
    Confirm preservation approaches 0/1.25/2.5/3.75/5%, ashes never benefit, the Chaos 50% roll is
    evaluated first, and the four-piece combined Chaos preservation approaches 52.5%.
11. Capture exact offering message/animation/graphic/inventory/XP order and determine whether an
    offering sound exists. Separately validate marrentill lighting, burner lifetime, POH ownership
    and visiting once those Construction/world systems exist; those are not claimed by this slice.
12. Drain Prayer at a representative church, POH chapel, Chaos altar, Ferox altar, Clan Hall altar,
    Kourend altar and Shrine of Twilight. Pray and confirm points return exactly to the base level,
    sequence 645 plays, accumulated fractional drain progress resets, and an existing overcharge is
    normalized down to base. Repeat on a free world at a free-to-play altar.
13. At Prayer 31 or higher, use the upstairs Edgeville Monastery altar and confirm restoration to
    base +2. After Nature Spirit, repeat at the Nature Grotto altar and confirm the same +2 result.
    Capture the exact recharge/full message, animation duration, any sound and packet ordering with
    the revision-matched workspace RuneLite before promoting presentation beyond integration-tested.
14. At each of the four original God Wars boss-room altars, wear zero, one and several items aligned
    with that altar. Out of combat, confirm Prayer restores to base plus exactly one point per
    matching worn item, sequence 645 plays and drain progress resets.
15. Immediately use a different God Wars altar and confirm the shared cooldown rejects it. Verify
    the workspace RuneLite shows ten minutes from varbit 4099, then reconnect partway through and
    confirm its timer is reconstructed from the persistent deadline. Confirm use succeeds at the
    exact 600-second boundary.
16. Enter PvM and PvP combat separately and confirm Pray rejects without starting or extending the
    cooldown. Capture the exact production rejection/cooldown text and audiovisual/packet ordering.
17. At the Zaros altar, first wear no Zaros item: only Prayer should restore. Repeat with one and
    several Zaros items: Prayer gains one point per item and Hitpoints, run and special energy all
    restore to their base/full values.
18. Pray at the Forthos Cursed Altar with 0, 1, 5, 10, 30 and more than 30 Prayer points. Confirm all
    Prayer is drained, the hit is respectively 5, 5, 5, 10, 30 and 30, and low Hitpoints can result
    in death. At the Darkmeyer statue, confirm Prayer restores to base and repeated uses charge only
    1, 2 or 3 Hitpoints. Capture exact messages, hitmarks and packet ordering on the matching client.
19. Before completing Spirits of the Elid, confirm the placed Statuette is unavailable and a forced
    operation at quest state 59 has no effect. At state 60 on a standard world, verify it restores
    Prayer, resets drain progress, fully heals before applying a +7 Hitpoints boost, fills run and
    special energy, cures poison and venom, and does not lower an existing boost above +7. Repeat
    with the selected world carrying the PvP flag and confirm Hitpoints remain unchanged while all
    other benefits apply. Repeat with the Deadman flag and confirm lost Hitpoints restore exactly
    to base without +7 while all other benefits apply. With at least one missing Prayer point,
    confirm the Medium Desert Diary task credits exactly once after its task service is implemented;
    with full Prayer, confirm it does not credit.
20. At Lunar Diplomacy state 189, force Astral Altar operation 2 and record the exact rejection
    feedback. At state 190, begin on Standard, Ancient and Arceuus and confirm each switches to
    Lunar with `Lunar spells activated!`; begin on Lunar and confirm it switches to Standard with
    `Lunar spells deactivated!`. In every case confirm current Prayer and fractional drain progress
    are unchanged. Confirm the active autocast spell, defensive mode and both saved staff-category
    selections survive both directions of the switch. Capture exact sequence timing, sound and
    packet ordering.
21. At Desert Treasure I state 14, force Ancient Pyramid altar operation 1 and record the exact
    rejection feedback. At state 15, begin on Standard, Lunar and Arceuus and confirm each switches
    to Ancient with `You feel a strange wisdom fill your mind...`; begin on Ancient and confirm it
    switches to Standard with `You feel a strange drain upon your memory...`. Confirm both paths
    drain all Prayer, clear fractional drain progress and still switch when Prayer starts at zero.
    Confirm the active autocast spell, defensive mode and both saved staff-category selections
    survive both directions. Pray twice and capture Desert hard-task credit, exact sequence timing,
    sound and packet ordering.
22. Before completing Mage Arena I, force Pray-at on each of the three god statues and confirm no
    cape appears. After completion, pray at Saradomin, Guthix and Zamorak and confirm the matching
    cape appears privately at the player's feet without changing Prayer. Pray twice at one statue
    and confirm two copies may be claimed. Capture exact animation, sound, ground-item visibility
    and despawn timing, and packet ordering with the revision-matched workspace RuneLite.
23. At the Yanille Agility Dungeon Chaos altar, begin above 15 Hitpoints and verify Pray-at deals
    exactly 15 damage and lands at 2578,9578 in the poison-spider chamber without changing Prayer
    or fractional drain progress. Repeat at exactly 15 and below 15 Hitpoints to confirm the fall
    is lethal. Capture the exact trapdoor animation, movement, hitmark, sound and packet ordering.
24. At broken sun altar 34872 with `varlamore_sun_knight` varbit 8409 clear, Pray-at and verify
    sequence 645 plus `You pray at the altar, nothing interesting happens.` Confirm current Prayer
    and fractional drain progress are unchanged. Set the varbit to 1, repeat and verify sequence
    8321 with the same message and unchanged state.
25. In an ordinary production world, confirm the Ferox Enclave uses its standard altar and cache
    location 7814 has no registered production handler. If a matching archived unrestricted world
    is available, independently capture its `Pray-at`, `Standard`, `Ancient`, `Lunar` and `Arceuus`
    options; that temporary-world behavior is not a production acceptance requirement.
26. At all three clickable pieces of the Land of the Goblins Goblin Temple altar (locations 43089,
    43097 and 43098), test quest states 0 and 35. Confirm sequence 645 and `The Big High War God
    does not answer your prayer`, with current Prayer and fractional drain progress unchanged.
    Repeat at states 36 and 56 and confirm normal Prayer restoration, drain-progress reset and the
    standard recharge message. Capture exact animation timing, any sound and packet ordering.
27. In combat and out of combat, use operation 2 (`Teleport`) on the Zamorak, Saradomin, Armadyl,
    Bandos and Zaros boss-room altars. Confirm the immediate destinations are respectively
    2925,5332,2; 2908,5265,0; 2839,5295,2; 2863,5354,2; and 2900,5204,0. Confirm current Prayer,
    Hitpoints, run energy, special energy, fractional Prayer drain and both ten-minute altar timer
    values are unchanged. Capture any animation, sound and packet ordering with the matching
    workspace RuneLite.

## Firemaking ordinary-log procedure

1. On a free world, light normal, oak, willow and maple logs from the inventory. Confirm both
   tinderbox-on-log and log-on-tinderbox work, the log is placed privately at the player's feet
   during the attempt, and exactly one log is consumed only when a fire is created.
2. On a members world, repeat for achey, teak, arctic pine, mahogany, yew, blisterwood, magic and
   redwood logs. Confirm the exact level and XP table in `SOURCE_LEDGER.md`; on a free world, verify
   each members-only entry rejects without moving or consuming anything.
3. Test one level below and exactly at every requirement, then satisfy one requirement only with a
   visible Firemaking boost. Confirm boosted levels work and failures remain atomic.
4. Force a failed chance roll and verify sequence 733 and synth 2597 repeat every four ticks until
   success. At levels 1, 2, 3, 42 and 43, statistically validate the 65/70/74/252/256 success counts
   out of 256 and capture the exact client message, animation, sound and packet order.
5. Drop each log and use its operation-4 `Light`, then target it with a tinderbox from the inventory.
   Remove the tinderbox before starting and during a pending attempt; confirm no unintended log
   loss or XP and compare exact production feedback with the matching client.
6. Place one multi-tile centrepiece with its origin in the south-west neighbouring zone and stand on
   every covered tile. Confirm each attempt rejects atomically with `You can't light a fire here.`
   Repeat with a fern/flower ground decoration and a diagonal full-tile loc. Confirm ordinary walls,
   wall decorations and roofs do not create false positives.
7. With open tiles, verify successful lighting steps west, then block west to select east, west/east
   to select south, and west/east/south to select north. Block all four directions and verify the
   player remains in place while the fire and reward still complete.
8. Force the minimum and maximum random lifetimes. Confirm fire 26185 persists for 100 and 199
   server cycles respectively, then disappears as public ashes 592 appear on that exact cycle.
9. Place each generic closed single, left-double and right-double door in both supported shapes and
   all four orientations. Confirm the tile onto which the closed model swings rejects, then open the
   door and confirm the same tile is legal. Verify the Dwarven polygon's two lobes and the inclusive
   Gilbert/Jiggig rectangle edges produce their exact messages from `SOURCE_LEDGER.md`.
10. Capture authoritative tile boundaries for the still-open named bank/activity restrictions with
    the matching client before extending the catalogue. Also test two simultaneous players on one
    otherwise legal tile: only one fire/reward may result and the losing log remains recoverable.
    Shades funeral pyres, Barbarian pyre ships, permanent fires and activity-specific mechanics
    remain open.

## Coloured-fire and gnomish-holder procedure

1. On a members world, use red, green, blue, purple and white loose firelighters on normal logs in
   both item orders. Confirm one of each input becomes the matching coloured log immediately and
   capture the exact `You coat the logs with the [colour] chemicals.` message and packet order.
2. Light every coloured log with a tinderbox at Firemaking level 1. Confirm the first attempt always
   succeeds, awards exactly 50 XP and creates the matching red 26186, green 26575, blue 26576,
   purple 20001 or white 20000 fire. Repeat with a bow at level 20 and 21 after Barbarian training.
3. On a free world, repeat loose conversion and lighting. Confirm both materials and all state are
   preserved. Let each coloured fire expire at both lifetime boundaries and confirm the ordinary
   movement and same-cycle ashes lifecycle remains unchanged.
4. Load an empty holder 20275 with each loose colour, including one, a partial stack and more than
   the remaining 1,000-charge capacity. Capture exactly how many are loaded per interaction, all
   messages and whether any amount-selection interface appears. Confirm only one colour can be
   stored and a full holder does not consume more firelighters.
5. Use `Check` on empty and charged holders and `Uncharge` on charged holder 20278. Capture exact
   messages and packet order. Repeat with no free slot, with an existing matching loose stack and
   after reconnect to confirm packed colour/charge persistence and atomic return of every charge.
6. Use each charged colour on normal logs from both inventory orders and on a private ground log by
   explicit targeting and its `Light` operation. Confirm a guaranteed 50-XP matching-colour fire,
   exactly one charge consumed only after fire creation commits, and the final charge returns the
   uncharged holder. Capture the exact runout text/capitalisation from cache 233.4.
7. Try oak and every higher/special log, an empty holder, a free world, an occupied/restricted tile,
   holder removal or charge/colour mutation during the four-tick attempt, and two-player tile races.
   Confirm failures preserve the holder, charge, log and XP with no duplicate fire.

## Pyre-log preparation procedure

1. On a members world with Shades of Mort'ton at state 64, use every sacred-oil dose object on a
   source log in both item orders and confirm nothing changes. At state 65, confirm normal logs can
   be prepared for the quest but every other source log rejects. Advance `morttonquest` to
   completion state 85 and repeat; confirm every recipe is available from Firemaking level 1.
   Repeat on a free world and verify both items, XP and queues remain unchanged.
2. Test normal and achey with two doses; oak and arctic pine with two; willow, teak, maple and
   mahogany with three; and yew, magic and redwood with four. Confirm the exact output identities
   and 10/15/20 XP values in `SOURCE_LEDGER.md`.
3. Start with sacred oil (4), (3), (2) and (1). Confirm successful recipes leave the exact lower-dose
   oil or empty vial. Verify a selected vial with too few doses rejects even when another eligible
   vial is present, and no partial vial is consumed.
4. Open the native quantity menu with oil split across several vials. Confirm its maximum is the
   smaller of source-log count and complete dose groups, then mutate or remove the selected log or
   oil before choosing an amount and verify the resumed request cancels atomically.
5. Select one item and confirm the first pyre log is made immediately. Let Make-All continue and
   confirm subsequent logs complete every four ticks and stop exactly when logs or complete oil
   groups run out. Separately hold `1` or space and repeatedly use oil on logs to verify the
   production one-tick manual route creates one per new interaction.
6. Test the hood, garb, robe and boots separately for 0.4%, 0.8%, 0.6% and 0.2%; all four for the
   extra 0.5% full-set bonus; and warm gloves alone and with the set to confirm they add nothing.
7. Fill every inventory slot and verify both in-place replacements still commit atomically. During
   an automatic batch, remove or change a required item, begin ordinary or bow lighting, and start
   another preparation request; confirm only the intended completed conversions award XP.
8. Capture exact cache-233.4 feedback, sequence, sound and packet ordering, plus which eligible oil
   vial the production client selects after the original vial when several choices remain. Keep
   those observations separate from funeral-pyre cremation and Barbarian pyre-ship acceptance.

## Shades of Mort'ton funeral-pyre procedure

1. On a members world, confirm `morttonquest` state 69 rejects an empty funeral pyre. At state 70,
   confirm only normal pyre logs followed by Loar remains can be staged and lit for the quest; all
   other log and remains tiers must reject. At state 85, confirm the complete training table opens.
   Repeat on a free world and verify no item, location, XP or queue changes.
2. Exercise all ten log tiers: normal, oak, willow, teak, arctic pine, maple, mahogany, yew, magic
   and redwood. Confirm their Firemaking levels and XP are exactly 5/50, 20/70, 35/100, 40/120,
   47/158, 50/175, 55/210, 65/255, 80/404.5 and 95/500. Verify boosted Firemaking levels work.
3. Exercise Loar, Phrin, Riyl, Asyn, Fiyr and Urium remains against every log tier. Confirm every
   compatible Prayer-XP cell in `SOURCE_LEDGER.md` and that insufficient log tiers reject without
   consuming either staged material. Test manual item-on-location in every stage and the left-click
   Build/Use routes; with several eligible items held, confirm the highest compatible usable pair
   is chosen.
4. Confirm the empty, logs-loaded and remains-loaded locations expose Build, Use and Light and use
   cache sequences 1303, 1304 and 1305. Light with a tinderbox, confirm spot animation 292 and that
   both staged items are consumed exactly once. Remove the tinderbox, mutate/expire a stage, race a
   second player, and start another Firemaking action; confirm stale actions remain atomic.
5. Verify the Morytania hard diary multiplies only Prayer XP by 1.5 and the elite diary multiplies
   only Firemaking XP by 1.5. Equip each Pyromancer piece and the full set and confirm cremation XP
   never receives the outfit bonus.
6. Force and boundary-test the 21-in-100 coin branch and every weighted key interval. Confirm coin
   ranges of 200–300, 400–500, 600–700, 800–900, 2,000–4,000 and 2,000–7,000 for the six remains
   tiers, and compare every key weight with `SOURCE_LEDGER.md`.
7. Confirm rewards land on a cardinal stone stand, stack there, remain private to the cremating
   player, and disappear after 500 ticks without becoming public. Test player positions equidistant
   from several stands and record the production client's exact stand-selection rule.
8. Capture the exact cache-233.4 unlit-stage lifetime, feedback text, shade-spirit behavior,
   sequence/sound/spot-animation timing and packet order. Separately trace the quest plugin's exact
   transitions through states 65, 70, 75, 80 and 85; do not infer those transitions from the
   training activity alone.

## Handheld light-source procedure

1. On a members account, place a tinderbox and each unlit cache-233 light source in inventory:
   torch, both candles, both candle lanterns, oil lamp, oil lantern, bullseye, sapphire, emerald and
   mining helmet.
2. At the exact requirement and again with only a visible boost meeting it, use the tinderbox on
   each source and reverse the selected/target order. Confirm the source changes to its lit form,
   the tinderbox remains and no Firemaking XP is awarded.
3. Repeat below the requirement and on a free world. Confirm neither item changes and record the
   exact message and packet order. Compare them with the current functional failure messages.
4. With level 30 Firemaking and dry sticks plus an unlit torch in inventory, choose
   `Rub-together`. Confirm the torch lights and the sticks remain; repeat at level 29. Then use the
   sticks directly on the torch and confirm nothing happens. Capture the three current success
   messages because their wording is observational rather than cutoff-Wiki verified. Separately
   capture torch-on-range/fireplace/fire and other cooking heat-source behavior; this is still an
   open environmental route.
5. Invoke Extinguish on every lit inventory form. Confirm all return to the matching unlit object
   through their cache-native menu slots. Equip a lit mining helmet and invoke its worn Extinguish
   option; confirm the unlit helmet remains equipped. Record all feedback, animation, sound and
   inventory-update ordering, especially the sourced sapphire message.
6. Combine empty candle lantern 4527 with unlit and lit white/black candles in both use orders.
   Confirm unlit candles require boosted level 1 and produce unlit lanterns, lit candles require
   boosted level 4 and produce lit lanterns, and every route consumes both inputs for one output
   with zero XP. Repeat in a full inventory to confirm same-slot replacement.
7. In each production dark/gas area, separately capture brightness stacking, the only-light-source
   extinguish refusal, warning/damage cadence, seven-tick gas explosions, 25%-maximum-Hitpoints
   damage rounding, forced extinguishing and water/trap/monster exceptions. These environmental
   checks must remain researched until their area inventory and shared processor are implemented.

## Lamp-oil still procedure

1. At the Rimmington Chemist's empty still 5909, use a stack of swamp tar 1939. Confirm exactly one
   tar is consumed, the scenery changes to full still 5910, no XP is awarded and a second stale use
   cannot consume another tar. Record the exact message, animation, delay and packet order.
2. For each empty container—oil lamp 4525, oil lantern 4535, bullseye 4546, sapphire 4700 and
   emerald 20722—load a fresh still and use the container on it. Confirm its exact unlit filled form
   appears in the same inventory slot, the still returns to 5909 and one load fills only one lamp.
   Repeat with all other inventory slots occupied.
3. Repeat both stages at Dorgesh-Kaan's single-form still 22773. Confirm its scenery does not change,
   its loaded oil is shared between players, a second tar is rejected while loaded and it requires
   a new tar after one lamp is filled. Repeat across logout, restart and instanced maps to determine
   the production persistence and instance key.
4. Repeat both stages on a free world and confirm no item or scenery changes. Capture exact failure
   feedback for membership, an unloaded still, a loaded still and invalid/stale selections.
5. At Rimmington only, capture imp repellent followed by butterfly jar production of an impling jar.
   Confirm that Dorgesh-Kaan rejects that route. Keep this separate product researched until its
   object transformations, messages and presentation are pinned.

## Ordinary Herblore herb-cleaning procedure

1. On a members world with Druidic Ritual complete, left-click Clean on one of each ordinary grimy
   herb: 199, 201, 203, 205, 207, 3049, 209, 211, 213, 30094, 3051, 215, 2485, 217 and 219.
   Confirm the exact clean output and XP table in `SOURCE_LEDGER.md`, including fractional XP.
2. Test each row one boosted Herblore level below and exactly at its requirement. Confirm visible
   boosts satisfy the level and failures consume no herb or award XP. Repeat before quest
   completion and on a free world, recording exact failure messages and packet ordering.
3. Fill the inventory, leave the selected grimy herb in one slot and clean it. Confirm it is
   replaced in that slot without requiring free capacity. Move or remove the next queued herb
   before its action and confirm no duplicate output or XP appears.
4. Begin with a full inventory of one grimy herb type. Confirm the clicked herb cleans immediately,
   then exactly one further herb cleans every two game ticks until none remain. Interrupt with
   movement, combat, teleport, another skill action, logout and death to establish the production
   cancellation matrix.
5. Mix several eligible grimy herb types and start automatic cleaning from each type. Capture the
   exact selection order and whether continuation crosses herb types. Manually click more than ten
   herbs within one tick and confirm the observed request ceiling. Record the exact success sound,
   messages, inventory updates and packet order before promoting presentation beyond researched.

## Ordinary unfinished-potion procedure

1. Combine a vial of water 227 with each of the 15 compatible clean herbs, then combine cadantine
   265 with a vial of blood 22446, in both item orders. Confirm the single-product cache-233
   quantity interface, all 16 output/level rows in `SOURCE_LEDGER.md`, and that Make-All is
   initially capped by the smaller of the two input counts.
2. Select one, a middle quantity and Make-All. Confirm the first unfinished potion appears after
   one tick and each continuation appears one tick later, consuming one herb and vial together and
   awarding no Herblore XP. Repeat with all other slots filled and record the exact output slot.
3. Test each recipe one boosted level below and exactly at its requirement; repeat before Druidic
   Ritual completion and on a free world. Confirm all failures preserve both inputs and capture the
   exact failure text and interface-close ordering.
4. During a batch remove either input, move, enter combat, teleport, start herb cleaning, log out
   and die. Confirm the batch stops at the correct point with no partial transaction. Start a new
   unfinished-potion recipe mid-batch and confirm the old recipe does not resume.
5. Record cache-233.4 sequence 363 repetition, exact success-message capitalization, sound,
   inventory updates and packet order for both water and blood bases.

## Standard finished-potion procedure

1. Combine each of the 31 unfinished-potion and secondary pairs listed in `SOURCE_LEDGER.md` in
   both item orders. Confirm the single-product cache-233 quantity interface, exact three-dose
   output and maximum capped by the smaller input count.
2. Select one, a middle quantity and Make-All. Confirm no output after the first tick, then one
   atomic transform every second tick with exact fractional Herblore XP. Repeat with all remaining
   slots filled and capture the output-slot behavior.
3. Test one boosted level below and exactly at every recipe requirement; repeat before Druidic
   Ritual completion and on a free world. Confirm all failures preserve both inputs and capture
   exact failure text/interface-close ordering.
4. During a batch remove either input, move, enter combat, teleport, start either earlier Herblore
   process, log out and die. Confirm the batch stops without partial consumption and cannot resume
   stale work after a replacement action.
5. Repeat all 30 eligible recipes with an amulet of chemistry and a charged alchemist's amulet.
   Confirm their 5%/15% successful rolls produce the mapped four-dose item, award only normal XP,
   consume a charge only on success, remove the chemistry amulet after its fifth shared success and
   replace a zero-charge alchemist's amulet with object 29988. Confirm each chemistry success reports
   its exact remaining charge count, uses `one charge` in the singular case and sends the exact
   crumble message on the fifth success. Confirm weapon poison has no extra-dose route and consumes
   no amulet charge.
6. Repeat representative recipes with each prescription-goggle appearance. Confirm an independent
   10% roll preserves the secondary while still producing the potion, including when an extra-dose
   roll also succeeds, with no preservation message or extra XP. Capture exact modifier roll and
   packet order, depletion presentation, animation repetition, success messages and sound.
7. In inventory, Charge objects 29988 and 29990 directly and by using object 21163 on each amulet in
   both item orders. Verify the count prompt, ten charges per consumed chemistry amulet, 5,000 cap,
   Check message, same-slot first-charge transform and no mutation after changing an input while the
   prompt is open. Uncharge totals divisible and not divisible by ten; verify confirmation,
   same-slot object 29988, noted object 21164 refund and discarded remainder. Repeat with no spare
   inventory slot and capture the exact failure wording. Probe every total from 4,991 through 4,999,
   bank-side operations and the initial 50-charge acquisition. Use Change on objects 29974 and
   29976 in both directions and capture the exact sound/packet presentation. Give an ordinary
   chemistry amulet a partially consumed shared state, use its slot-4 Break operation and confirm
   the selected item is destroyed and the next amulet starts with all five charges. Capture any
   confirmation, text, sound and packet sequence. Separately exercise every chemistry-amulet
   Options choice and record its default/toggled state and persistence before promoting it.

## Fixed ingredient-crushing procedure

1. Use pestle and mortar 233 on each input in both orders: unicorn horn 237, chocolate bar 1973,
   desert goat horn 9735, kebbit teeth 10109, blue dragon scale 243, empty bird nest 5075,
   superior dragon bones 22124 and nihil shard 26231. Confirm outputs 235, 1975, 9736, 10111,
   241, 6693, 21975 and 26368 respectively, with no Herblore XP or level/quest requirement.
2. Start with three matching inputs. Confirm the selected item changes immediately in its slot,
   the second changes two ticks later and the third changes three ticks after that. Capture initial
   sequence 364, restart sequence 11095, animation restart timing, exact sound, messages and packet
   order with the cache-233.4 RuneLite client.
3. Repeat on a free world and confirm the pestle route is refused without consuming anything;
   separately confirm the knife-on-chocolate route remains distinct. Repeat with a full inventory
   and confirm each same-slot replacement succeeds atomically.
4. Remove the pestle or remaining input before continuation, then replace the work with cleaning,
   unfinished mixing, finished mixing and a different crushing recipe. Confirm stale work stops
   without extra output. Also test movement, combat, teleport, logout and death to complete the
   matching-client interruption matrix.
5. Keep variable-yield lava scales, crystal-shard batches, Gorak/quest recipes and Wesley's crushing
   service out of this fixed one-to-one contract; test each under its separate procedure.

## Additional fixed pestle-processing procedure

1. Use pestle and mortar 233 in both orders with guam 249, charcoal 973, raw/poison/cooked karambwan
   3142/3146/3144, mud rune 4698, garlic 1550, bat bones 530, ashes 592, rune shards 6466,
   anchovies 319, dried thistle 3263 and seaweed 401. Confirm outputs 6681, 704, 3152, 3153, 3154,
   9594, 4668, 2391, 8865, 6467, 11266, 3264 and 6683 respectively.
2. For every route, confirm no Herblore level, Druidic Ritual state or XP applies, membership is
   required by the pestle, the first transform is immediate, the second is two ticks later and later
   transforms repeat every three ticks.
3. Use a multi-count nihil-shard or other stackable-input stack in a full inventory. Confirm a
   stackable output merges into an existing output stack, and otherwise the action is atomic until
   a slot is free. Repeat with stacked mud runes, whose non-stackable outputs require one destination
   slot per result.
4. Exercise garlic before/during/after Desert Treasure, bat bones before/during/after Watchtower and
   rune shards before Garden of Tranquillity. For the other quest-labelled objects, confirm valid
   acquisition stays owned by each quest while possession is sufficient for processing.
5. Capture exact messages, sound, animation restart, output-slot preference and packet order. Test
   the three Pirate Pete ingredients separately at every subquest state before adding their explicit
   partial-completion gate to this family.

## Knife chocolate-processing procedure

1. On a free world, use ordinary knife 946 with chocolate bar 1973 in both item orders. Confirm the
   selected bar immediately becomes chocolate dust 1975 in the same slot with no Herblore XP and no
   level, Druidic Ritual or other quest requirement.
2. Start with three bars. Confirm the second result occurs two ticks after the immediate first and
   the third occurs three ticks later. Remove the knife after the first result while retaining a
   pestle and mortar; confirm the knife-started batch stops instead of switching tools.
3. Repeat with every inventory slot occupied and confirm the same-slot replacement is atomic. Start
   pestle and knife chocolate batches over one another and against cleaning/mixing/lava work, then
   verify only the newest Herblore queue remains.
4. With the workspace cache-233.4 RuneLite client, capture the exact initial/restart animation,
   messages, sound and packet/inventory ordering. Replace sequences 364/11095 only if that direct
   observation disproves the shared grinding-animation inference.

## Crystal-shard crushing procedure

1. Complete Song of the Elves, then use pestle and mortar 233 with a stack of crystal shards 23962
   in both orders. Confirm the count prompt and enter one, a middle quantity, more than held and the
   full stack. Each accepted shard must become ten crystal dust 23964 immediately with no XP.
2. Repeat at quest varbit 9016 values 192 and 194 and on a free world. Confirm only the completed
   members state succeeds, neither failure mutates inventory, and capture the exact prompt/failure
   text and interface-close sequence.
3. Fill every inventory slot. Confirm consuming the full selected shard stack replaces that slot
   with dust; confirm a partial conversion succeeds only when dust can stack or another slot is
   available and otherwise rolls back without animation or partial loss.
4. While the count prompt is open, alter the selected stack, remove the pestle, change quest state,
   or start another action. Confirm stale input never converts. Capture sequence 364, sound, output
   placement and packet ordering with the cache-233.4 RuneLite client.

## Lava-scale crushing procedure

1. Use pestle and mortar 233 with lava scale 11992 in both item orders at visible Herblore levels
   1, several middle levels and 99. Confirm each scale immediately produces 3–6 lava scale shards
   11994, awards no XP and has no Druidic Ritual or minimum-level gate; confirm a free world rejects
   the action atomically.
2. Collect a statistically useful sample at each level and compare the three independent success
   rolls against low/high pairs 101/301, 58/251 and -14/254 using the cutoff interpolation. Verify
   boosts and drains use the visible level and record the client-observable RNG/inventory order.
3. Repeat each forced base yield 3, 4, 5 and 6 with Hard Wilderness Diary varbit 4468 unset and set.
   Confirm the completed diary changes them to exactly 4, 6, 7 and 9 through a floored 50% bonus.
4. Start with three scales. Confirm the first result is immediate, the second occurs two ticks later
   and the third three ticks after that, using initial sequence 364 and restart sequence 11095.
   Capture exact messages, sound, animation restart and packet ordering with only the workspace
   cache-233.4 RuneLite client.
5. Repeat with a full inventory both with and without an existing shard stack. Confirm output stacks
   when possible or reuses the consumed scale slot, and that missing pestle/input and replacement by
   another Herblore action stop continuation without partial loss. Exercise movement, combat,
   teleport, logout and death to finish the interruption matrix.

## Gorak-claw crushing procedure

1. Use pestle and mortar 233 with Gorak claws 9016 in both item orders. Confirm the selected claw
   immediately becomes Gorak claw powder 9018 in the same slot, with zero Herblore XP and no
   Herblore-level or Druidic Ritual requirement; confirm a free world rejects atomically.
2. Start with three claws and confirm there is no ordinary two-tick special case: the second powder
   appears three ticks after the immediate first and the third appears another three ticks later.
3. Test legitimate claws obtained during and after Fairytale II. Also inject possession before the
   quest state and capture whether the production client applies a hidden progress check; until
   disproved, ownership rather than an invented quest varbit is the isolated recipe gate.
4. Repeat in a full inventory and remove the pestle after the first transform. Confirm same-slot
   replacement succeeds and continuation stops safely. Replace the work with every other Herblore
   queue and complete the movement/combat/teleport/logout/death interruption matrix.
5. Capture exact messages, sound, initial/restart animation and packet/inventory order with only the
   workspace cache-233.4 RuneLite client.

## Suqah-tooth crushing procedure

1. Use pestle and mortar 233 with Suqah tooth 9079 in both item orders. Confirm there is no
   immediate transform: ground tooth 9082 replaces the selected tooth after exactly one tick with
   zero Herblore XP and no Herblore-level or Druidic Ritual requirement.
2. Start with three teeth and confirm outputs appear at ticks 1, 3 and 6: a one-tick first result,
   two-tick first continuation and three-tick repeat. Capture initial sequence 364, restart sequence
   11095 and their exact relationship to each inventory update.
3. Test a legitimately obtained tooth during Lunar Diplomacy state 140 and again after completion.
   Also inject possession at state zero to determine whether production has a hidden progress check;
   until direct evidence disproves it, acquisition rather than an invented quest gate owns access.
4. Repeat on a free world and confirm atomic rejection. Remove the pestle before tick one and between
   later results, and replace the action with every other Herblore queue; confirm no delayed output
   survives. Repeat with all 28 slots occupied to confirm each one-for-one replacement is safe.
5. With only the workspace cache-233.4 RuneLite client, capture exact success/failure messages,
   sound, animation restart timing and packet/inventory order. Complete movement, combat, teleport,
   logout and death interruption observations before promoting those details.

## One-shot quest ingredient-crushing procedure

1. Use pestle and mortar 233 in both orders with Dramen branch 771, Tchiki monkey nuts 7573 and
   mysterious meat 22403. Confirm the selected input immediately becomes cinnamon 7472, Tchiki nut
   paste 7575 or mysterious crushed meat 22404 respectively, with zero Herblore XP.
2. Repeat at Herblore level one and Druidic Ritual state zero, during each owning quest's production
   step and after any quest where the input can legitimately remain. Confirm whether possession is
   sufficient or the production client applies a hidden progress check; record exact rejection text.
3. Where duplicate inputs can be legitimately obtained—or in an isolated test account with injected
   duplicates—start with three. Confirm whether only the selected item changes or any automatic
   continuation begins. Do not merge a discovered cadence into ordinary 0/2/3 crushing without a
   cache-233.4 capture.
4. Repeat on a free world and with all 28 slots occupied. Confirm rejection is atomic and valid
   one-for-one replacement reuses the selected slot. Start these actions over all other Herblore
   queues and verify the immediate action cancels stale production.
5. Capture exact text, sound, sequence, animation/inventory order and movement/combat/teleport/logout
   behavior using only the workspace cache-233.4 RuneLite client.

## Karambwanji-paste processing procedure

1. Use pestle and mortar 233 with a stack of raw karambwanji 3150 in both item orders. Confirm one
   raw paste 3155 is produced immediately and further paste appears exactly every three ticks, with
   no Cooking or Herblore XP and no Druidic Ritual requirement.
2. Repeat with cooked karambwanji 3151 and confirm cooked paste 3156 replaces the selected item
   immediately. With legitimately obtained or injected duplicates, determine whether cooked paste
   has any automatic continuation; the cutoff recipe tab does not specify one.
3. Test during Tai Bwo Wannai Trio and with possession before/after its relevant step to determine
   whether any hidden quest-progress check exists. Repeat at Cooking/Herblore level one and on a free
   world, capturing exact rejection text and precedence.
4. Fill all 28 slots and use a multi-count raw stack. Confirm it rejects atomically until a
   destination slot is free, then emits one non-stackable paste per available slot and reuses the
   selected slot for the final unit. Confirm a single cooked input always replaces its own slot.
5. Remove the pestle between raw outputs and replace the action with every other Herblore queue.
   Capture exact messages, sequences 364/11095, sound and packet/inventory ordering with only the
   workspace cache-233.4 RuneLite client.

## Black-dye pestle-processing procedure

1. At The Golem varp 437 states 0 and 1, use pestle and mortar 233 with black mushroom 4620 in both
   orders while holding empty vial 229. Confirm state 0 rejects and state 1 immediately consumes one
   mushroom and vial to produce black dye 4622 with no Herblore XP.
2. Repeat throughout and after The Golem, then during Shadow of the Storm and Land of the Goblins,
   confirming the `started` gate remains valid for later uses. Test a free world and Druidic Ritual
   state zero to establish exact gate precedence and feedback.
3. Hold several mushrooms and vials in non-adjacent slots. Confirm which mushroom/vial are consumed,
   which slot receives the dye and whether any automatic continuation occurs. Repeat with no vial,
   no pestle and a stale selected mushroom.
4. Fill all 28 inventory slots and confirm the two-input/one-output transaction remains atomic.
   Replace the action with every Herblore queue and test movement, combat, teleport, logout and death
   even though the documented transform is immediate.
5. Capture exact failure/success text, sequence 364, sound and inventory/packet order using only the
   workspace cache-233.4 RuneLite client before promoting presentation and output-slot details.

## Blamish-snail-slime processing procedure

1. At Herblore level one and with Druidic Ritual and Heroes' Quest unstarted, use pestle and mortar
   233 with thin, lean and fat raw snails 3363/3365/3367 in both item orders while holding sample
   bottle 3377. Confirm the selected snail and first bottle immediately become slime 1581 with no XP.
2. Start with four matching snails and bottles. Confirm results occur immediately and at ticks
   1/3/6, with sequence 364 initially and restart sequence 11095 during continuation.
3. Mix all three snail sizes and determine whether automatic processing remains on the selected
   size or crosses to another size after exhaustion. Capture the exact stop and restart behavior.
4. Remove the pestle, next matching snail or last sample bottle between automatic results. Confirm
   the next result stops atomically. Repeat on a free world and capture exact rejection precedence.
5. Fill all 28 slots and confirm each two-input/one-output action places slime in the consumed bottle
   slot without capacity failure. Capture exact messages, audio, animation and packet/inventory
   ordering using only the workspace cache-233.4 RuneLite client.

## Blamish-oil Herblore procedure

1. With Druidic Ritual complete but Heroes' Quest varp 188 at state 0, combine harralander potion
   (unf) 97 and blamish snail slime 1581 in both orders. Confirm the native quantity menu opens.
2. At visible/base Herblore 25/20, make one and confirm oil 1582 appears after exactly two ticks and
   awards exactly 80 XP. At visible level 24 confirm both inputs remain and capture exact feedback.
3. Make several through the maximum quantity option. Confirm one result every two ticks, atomic
   full-inventory replacement, resource-loss stops and cancellation by every other Herblore queue.
4. Repeat with chemistry/alchemist amulets and both prescription-goggle forms to verify which
   modifiers apply to this quest potion; do not promote that behavior without matching-client data.
5. Capture exact text, sequence 363, audio and packet/inventory order using only the workspace
   cache-233.4 RuneLite client.

## Oily fishing-rod conversion procedure

1. At Heroes' Quest varp 188 state 0, use blamish oil 1582 with ordinary fishing rod 307 in both
   orders. Confirm oil is consumed and oily rod 1585 replaces the rod immediately with no XP.
2. Repeat with pearl fishing rod 22846. Confirm both inputs remain through tick zero and oily pearl
   rod 23122 replaces the rod exactly one tick later, again with no XP or animation.
3. Repeat on a free world and capture exact rejection text. Fill all 28 slots and confirm the
   two-input/one-output transaction succeeds atomically without needing a spare slot.
4. For the delayed pearl route, move or remove either exact input before the result and confirm it
   stops without consuming the other. Replace it with every Herblore queue and test interruption.
5. Test cleaning cloth reversal for both rods and capture exact text and slot behavior. Capture all
   audio and packet/inventory ordering with only the workspace cache-233.4 RuneLite client.

## Barbarian pyre-ship procedure

1. At each of the six published lake-site tiles, exercise both members and free worlds and
   `brut_craft_ship` states 0–3. Confirm only started training permits construction and record the
   exact failure text, site-facing direction, stage anchor, rotation and footprint with the
   workspace cache-233 RuneLite.
2. Test normal, achey, oak, willow, teak, arctic pine, maple, mahogany, yew, magic and redwood logs
   one level below and exactly at 11/11, 25/25, 40/40, 45/45, 52/52, 55/55, 60/60, 70/70, 85/85
   and 95/95 Crafting/Firemaking. Confirm visible boosts work and the exact Crafting XP,
   Firemaking XP and 1/1/2/2/3/3/3/4/4/5/5 charge table in `SOURCE_LEDGER.md`.
3. Repeat with all 26 accepted ordinary, ornamented, infernal, crystal, third-age, Trailblazer and
   felling axes. Capture the exact carving sequence used by each. Try no axe, a tinderbox, every
   supported Barbarian bow and the Bruma torch; confirm only tinderbox or a supported bow lights
   the completed ship and every rejection remains atomic.
4. Hold both bone types and confirm chewed bones take priority. With chewed bones, verify peaceful
   spirit 5605, training state 1 to 2, Prayer charges capped at 200, all 22 weighted rewards and the
   1/256 Dragon full helm pre-roll. Fill the inventory and confirm the reward falls privately at
   the player's tile. Use magic logs and verify only the Kandarin elite pyre-ship task bit changes.
5. Bury every eligible bone directly with charges available. Confirm one charge gives triple base
   XP and the exact ancestral message, including when Zealot preserves the bone. Confirm big/giant
   bones, demonic ashes, all altar-offering routes and Hopespear quest bones neither multiply XP
   nor consume a charge. Once a Bonecrusher handler exists, repeat every automatic-burial path.
6. With mangled bones, confirm no reward or charge, appearance sequence 6725, immediate aggression,
   the exact level-166 combat profile and a roughly 300-cycle despawn. Confirm the creator cannot
   build another ship while that spirit is live, can do so after its death/despawn, and other
   players cannot attack it. Reconcile the automated 128-weight main table, twice-rolled standard
   herb branch, ordinary and Ring-of-Wealth gem/mega tables, and pre-Legends Nature-talisman
   substitution with floor-object timing and packet order. Kill it before despawn and confirm the
   Barbarian Training state transition occurs only for the creator's successful kill. During the
   matching elite-clue challenge, confirm the separate always-awarded elite clue and elite casket;
   prove neither appears on an ordinary kill.
7. Remove or mutate logs/bones at every 4-tick construction checkpoint, race both halves of one
   physical station with another player, move, teleport, enter combat, log out and die. Confirm
   materials are consumed only at the commit point, XP/reward/state occur exactly once and every
   temporary model/station reservation is cleaned up.
8. Capture exact construction, launch, ready, sinking and spirit durations plus animations, sounds,
   messages and packet order using only the workspace RuneLite. Replace the observational site
   transforms, peaceful-spirit lifetime and cosmetic-axe sequence mappings only with preserved
   cache-233.4 evidence.

## Barbarian bow Firemaking procedure

1. On a members world with `brut_fire` at state 0, use an ordinary shortbow on oak logs in both item
   orders. Confirm no log is dropped or consumed and capture Otto-training feedback.
2. Have Otto assign the first-fire task (state 1). Verify base 34 Firemaking plus a visible boost to
   35 rejects, as does base Crafting 10; base 35 Firemaking and 11 Crafting succeeds. Confirm every
   log except oak rejects during this state.
3. Successfully light oak logs. Confirm state 1 remains until the fire successfully registers and
   the log is consumed, then advances to state 2. Verify state 2 rejects additional bow fires until
   Otto's follow-up dialogue advances it to state 3.
4. At state 3, test all 11 rows and their exact level/XP table in `SOURCE_LEDGER.md`, one level below
   and exactly at each requirement. Confirm a visible boost satisfies the post-unlock requirement,
   and that bow lighting always rejects on a free world.
5. Use all 15 supported bow objects and verify the sequence grouping 6713–6720 plus spot animation
   1169. Confirm the bow remains in inventory, the tinderbox synth 2597 is absent, and both
   bow-on-log and log-on-bow work.
6. Try dark bow 11235, twisted bow 20997, Craw's bow 22547 and rain bow 23357. Confirm none dispatches
   Barbarian Firemaking. Try blisterwood logs 24691 with a valid bow and confirm no log loss or XP.
7. Force a failed roll at level 21 and confirm bow sequence/spot animation and the attempt repeat on
   the same four-tick cadence. Then force success and verify the ordinary transactional fire,
   movement, random lifetime and ashes behavior is unchanged.
8. Capture the matching cache-233.4 client's exact messages, spot-animation timing and packet order.
   Otto's state-2-to-3 dialogue remains part of the broader Barbarian Training miniquest acceptance.

## Standard meat-and-fish Cooking procedure

1. On cache 233.4, cook each of the 44 raw inputs listed in `SOURCE_LEDGER.md` one visible Cooking
   level below and exactly at its requirement. Confirm a visible boost qualifies; rat, bear, boar
   and pike work on free worlds; and yak, mystery meat, rabbit, ugthanki meat, all three snails,
   lava eel, mackerel, cod, slimy eel, bass, monkfish, shark, sea turtle, anglerfish, rainbow fish,
   cave eel, dark crab and manta ray reject there without item loss. Confirm all nine Hunter meats
   likewise reject on free worlds.
2. Exercise every one of the 48 cache `Cook` locations listed in `SOURCE_LEDGER.md`. Confirm the 41
   ordinary and four special operation-1 sources, all three operation-2 POH fancy-range states and
   raw-food use dispatch correctly. Verify permanent fire 43475 uses fire curves/sequence, Cook's
   Assistant varp 29 state 2 gates Lumbridge range 114, Easy Kourend & Kebos Diary completion gates
   Hosidius 27516/27517, and elite completion selects its stronger curve.
3. Force the low/high endpoint boundaries for every row and source in the Cooking ledger. Repeat
   lobster, swordfish, monkfish, shark, anglerfish and kyatt with cooking gauntlets, including the
   swordfish and kyatt non-stacking exceptions at Hosidius. Confirm each success grants the exact
   XP once and each failure creates the exact burnt object with zero XP. Force the nominal failure
   edge for lava eel and confirm it still succeeds, awards 30 XP and creates no burnt object.
4. Equip the normal and trimmed Cooking capes and the max cape in turn. At a level/source that can
   burn, force a failure roll and confirm each cape prevents it for all 43 burnable implemented raw
   foods; lava eel must remain independently always-successful without relying on a cape.
5. Use one raw item on a source and confirm the quantity chooser is skipped. Repeat with multiple
   units and several eligible raw types, select each entry and quantity, and confirm results at
   ticks 1, 4, 8, 12 and onward with range sequence 896 or fire sequence 897. Dark crab is the
   explicit exception: confirm its first and every later result at ticks 4, 8, 12 and onward. All
   nine Hunter meats use that same fixed four-tick result family.
6. During every delay, remove or replace the selected raw input, move out of range, teleport, enter
   combat, log out, die or begin another queued skill action. Confirm no stale conversion, duplicate
   XP or partial transaction occurs and the animation resets appropriately.
7. With a full inventory, force both cooked and burnt results. Confirm same-slot replacement is
   atomic. Race source removal/replacement and repeated clicks and confirm only committed raw food
   is consumed.
8. Deliberately burn cooked meat and cooked mystery meat on both a range and fire. Confirm the
   first/second/later results occur at ticks 1/4/8, award zero XP and emit the ledger message; test
   free-world mystery-meat rejection, a full inventory, all three burn-prevention capes and a mixed
   raw/cooked left-click chooser. Separately capture cooked-chicken single and repeat timing before
   implementing that unresolved route.
9. Using only the workspace RuneLite, capture exact cache-233.4 chooser action mode, messages,
   sounds, animation/packet ordering and random-boundary behavior. Catalogue every item-use-only
   fire, fireplace, vent and other appliance plus any source-specific restrictions, and verify
   cooked-chicken overcooking before promoting extended coverage beyond `researched`.
10. Catch and cook a manta ray while eligible for the Ardougne elite diary task. Record the exact
    task var/state transition and packet timing so the Cooking route can be connected when the
    diary-task subsystem is implemented; ordinary Cooking must remain independent of task state.
11. Catch and cook a dark crab in the Wilderness Resource Area while eligible for the Wilderness
    elite diary task. Record the catch/cook coupling, exact task var/state transition and packet
    timing for the future shared diary-task subsystem.
12. Separately eat each cooked Hunter meat and capture its immediate and delayed healing amounts,
    delay timing, combo-food restrictions, full-HP behavior, interruptions and packet order. These
    consumption effects belong to the item/combat layer and are not implied by Cooking coverage.
13. Cook a thin snail while eligible for the Morytania easy diary task. Record the exact task
    transition and packet timing for the future shared diary-task layer; ordinary Cooking must not
    depend on task state.
14. At multiple Heroes' Quest states, cook an already-owned lava eel on both a fire and a range.
    Confirm the Cooking action is possession-based, always succeeds on the standard 1/3/4 cadence
    and does not invent a quest-state rejection.
15. Exercise the implemented iron-spit route in both item orders and with boosted Cooking 30 and
    Firemaking 20. Cook the selected skewered chompy on both ordinary and permanent fires; confirm
    sequence 1016, a three-tick result, endpoints 200/255, 100 XP only on success, iron-spit return,
    ruined-chompy failure, level-99/cape protection, full-inventory rejection, selected-slot and
    movement interruption. Then use raw chompy directly on each ogre-spit-roast identity. Confirm
    sequences 3181/3182, raw state 1, the five-tick result, cooked/burnt states 2/3 for one tick,
    empty-state restoration, session/interruption cleanup, 100 XP and the same 200/255 curve.
    Capture exact messages, any sounds and packet ordering, and separately exercise the
    quest-specific seasoned-chompy path before claiming that quest workflow.
16. Across every Freeing Skrach Uglogwee boundary, attempt raw jubbly on an ogre spit roast. Confirm
    state 110 is the first accepted state; level 41, 160 XP, objects 7566/7568/7570 and endpoints
    191/245; raw/cooked/burnt states 6/7/8; six ticks to the result and seven ticks between manually
    started consecutive results. Interrupt via movement, another queue, logout and death and verify
    state 0 restoration. Capture exact messages, any sounds and packet ordering.
17. Use raw chicken on the ogre spit roast in members and free worlds at boosted Cooking levels 29,
    30, 33 and 34. Confirm a six-tick result, 30 XP only on success, objects 2138/2140/2144, raw and
    cooked states 4/5, shared burnt state 3, the standard 128/512 curve, no burn at level 34 and
    one-tick result cleanup. Try every other raw food and confirm the roast accepts only raw chompy,
    raw chicken and raw jubbly and emits the exact default no-op feedback without opening a chooser.
18. Bake bread dough 2307 and pitta dough 1863 on ordinary, Lumbridge and both Hosidius ranges.
    Confirm one tick for a selected item and results at ticks 1, 4, 8 and onward for Make-X; 40 XP
    per success; bread/burnt-bread outputs 2309/2311 and endpoints 118/492, 128/512, 130/504 and
    143/517; and pitta bread 1865 always succeeding at its boostable members-only level-58 gate.
    Use both doughs on ordinary and permanent fires and verify no conversion or chooser. Capture the
    exact fire rejection, success/burn messages, sounds and packet ordering. Separately exercise
    pot-of-flour plus bucket/bowl/jug water, all dough choices, container return and full-inventory
    precedence before promoting dough preparation.
19. Cook uncooked stew 2001 on ordinary and permanent fires, ordinary and Lumbridge ranges, and
    both Hosidius ranges. Confirm boostable level 25, 117 XP only on success, stew/burnt-stew
    outputs 2003/2005, source endpoints 68/392, 78/412, 80/404 and 93/417, results at ticks 1, 4, 8
    and onward, ordinary no-burn at level 58 and Lumbridge no-burn at 54. Repeat with each
    burn-prevention cape, a full inventory, movement and another Cooking action. Capture exact
    messages, sounds and packet ordering. Recheck the integration-tested preparation graph using
    the dedicated acceptance case below before promoting its presentation beyond that status.
20. Cook uncooked curry 2009 on ordinary and permanent fires, ordinary and Lumbridge ranges, and
    both Hosidius ranges. Confirm the boostable members-only Cooking-60 gate, 280 XP only on
    success, curry/burnt-curry outputs 2011/2013, ordinary and Lumbridge endpoints 38/332,
    Hosidius endpoints 50/344 and 63/357, results at ticks 1, 4, 8 and onward, and ordinary
    no-burn at level 74. Repeat with a full inventory, movement and another Cooking action. Capture
    exact messages, sounds and packet ordering. Recheck the integration-tested preparation graph
    using the dedicated acceptance case below before promoting its presentation beyond that status.
21. Bake potato 1942 on ordinary and Lumbridge ranges and both Hosidius ranges. Confirm the raw
    potato remains a free-to-play cache item while the action itself requires membership and
    boostable Cooking 7; success/burn outputs 6701/6699; 15 XP only on success; endpoints 108/472,
    120/484 and 133/497; results at ticks 1, 4, 8 and onward; and no burn at level 41. Repeat with a
    full inventory and interruptions. Use it on ordinary and permanent fires and confirm no
    conversion or chooser. Capture exact messages, sounds and packet ordering before promoting
    presentation parity.
22. Cook uncooked egg 7076, sweetcorn 5986, chopped onion 1871 and sliced mushrooms 7080 on fires
    and every range class. Confirm exact outputs 7078/7090, 5988/5990, 7084/7092 and 7082/7094;
    boosted members-only levels 13/28/42/46; success XP 50/104/60/60; ordinary endpoints 90/438,
    78/412, 36/322 and 16/282; the recorded Hosidius endpoints; and no-burn levels 48/54/77/90.
    Scrambled egg must resolve immediately and then at +3/+4-tick gaps; the other three must resolve
    at ticks 1, 4, 8 and onward. Repeat each with failures, full inventory and interruptions, and
    capture exact messages, sounds and packet ordering. Specifically confirm fire acceptance to
    resolve the source-ledger conflict with the recipe templates' range-only facility shorthand.
23. Bake every standard pie on ordinary, Lumbridge and both Hosidius ranges: uncooked redberry
    2321, meat 2319, mud 7168, apple 2317, garden 7176, fish 7186, botanical 19656, mushroom 21684,
    admiral 7196, dragonfruit 22789, wild 7206 and summer 7216. Confirm cooked outputs
    2325/2327/7170/2323/7178/7188/19662/21690/7198/22795/7208/7218, shared burnt pie 2329, exact
    boosted levels and XP, free-world access only for redberry/meat/apple, and results at ticks 1,
    4, 8 and onward. Confirm ordinary no-burn levels 45/54/63/63/68/74/84/89/94 for the first nine,
    Hosidius levels 42/50/59/59/64/70/80/84/89/97 through dragonfruit, elite dragonfruit at 92,
    Lumbridge levels 42/49/52/60 for redberry/meat/botanical/mushroom, and that wild and summer can
    still burn at 99 without a Cooking cape. Try every pie on ordinary and permanent fires and
    confirm no chooser or conversion. Repeat with a full inventory and interruptions, then capture
    exact success/burn/source-rejection messages, sounds, animation/event ordering and the separate
    Lunar Bake Pie path using only the workspace cache-233 RuneLite client.
24. Bake uncooked cake 1889 and uncooked pizza 2287 on ordinary, Lumbridge and both Hosidius
    ranges. Confirm boostable free-world levels 40/35, success outputs 1891/2289, burnt outputs
    1903/2305, success-only XP 180/143, ordinary endpoints 38/332 and 48/352, Hosidius endpoints
    50/344 and 60/364, elite endpoints 63/357 and 73/377, and results at ticks 1, 4, 8 and onward.
    Confirm ordinary no-burn at 74/68 and Hosidius no-burn at 70/64, with the preceding levels
    remaining burnable, and verify cape immunity. Cake must return tin 1887 on both outcomes: try a
    full inventory and inventories with exactly one free slot, then record the exact rejection text,
    chooser maximum, stop point, sounds and packet ordering. Confirm pizza remains safe as a
    one-for-one full-inventory replacement. Try both inputs on all fire types and verify no chooser
    or conversion. Separately exercise cake assembly, pizza-base/tomato/cheese assembly, toppings
    and chocolate cake without treating those preparation routes as heat-stage coverage.
25. Using only the workspace cache-233 RuneLite client, combine pot of flour 1933 with bucket,
    bowl and jug of water 1929/1921/1937 in both item-use orders. Capture the exact menu title,
    bread/pastry/pizza/pitta order, action label, clientscripts and packet order; select every choice
    and confirm one-tick results, matching empty containers, level 35/58 boost behavior, pitta's
    members gate and full-inventory precedence. Assemble uncooked cake from cake tin, egg, flour
    and milk by initiating with each ingredient; confirm the exact two-tick result message and
    output slots. Then make incomplete and uncooked pizza in tomato-then-cheese order, prove cheese
    cannot be added first, and apply bar/dust chocolate, meat/chicken, anchovy and chunks/ring
    pineapple toppings at levels 50/45/55/65. Record every exact success/failure message,
    animation, sound, inventory-slot ordering and interruption packet. Compare the captures with
    the integration-tested requirements, XP 30/26/39/45 and two-tick result boundaries before
    promoting this family beyond `integration-tested`.
26. Using only the workspace cache-233 RuneLite client, make stew through all six routes. Combine
    bowl water 1921 with potato 1942, cooked meat 2142 or cooked chicken 2140 in both held-item
    orders; confirm exact intermediate IDs 1997/1999, then add the missing ingredient to obtain
    uncooked stew 2001. At base 24 with a boost to 25, verify free-world access, zero XP, no knife
    requirement and the result exactly two ticks after each use. Record the literal potato, meat
    and especially chicken text instead of assuming the Wiki's `meat/chicken` shorthand. Then, in
    a members world at base 59 boosted to 60, combine uncooked stew with spice 2007 and with exactly
    three curry leaves 5970; two leaves must fail and three must be consumed atomically after two
    ticks for uncooked curry 2009 with zero XP. Mutate the third leaf before completion and confirm
    no partial consumption. Capture exact curry success/failure messages, failure precedence,
    animation, sound, inventory-slot order and packet order before promotion beyond
    `integration-tested`.
27. Using only the workspace cache-233 RuneLite client, exercise all 26 standard pie-preparation
    handlers in both held-item orders. In a free world make shell 2315 from pastry dough 1953 and
    pie dish 2313, then redberry 2321, meat 2319 with both cooked meat 2142 and chicken 2140, and
    apple 2317; prove every later family is members-only. In members, follow each ordered chain:
    mud 2315+6032->7164+1925, 7164+1929->7166+1925, 7166+434->7168; garden
    2315+1982->7172, +1957->7174, +ordinary cabbage 1965->7176; fish 2315+333->7182,
    +339->7184, +1942->7186; botanical 2315+19653->19656; mushroom 2315+21626->21684;
    admiral 2315+329->7192, +361->7194, +1942->7196; dragonfruit 2315+22929->22789; wild
    2315+2136->7202, +2876->7204, +3226->7206; and summer 2315+5504->7212,
    +5982->7214, +1955->7216. Confirm boostable levels 1/10/20/30/29/34/47/52/60/70/73/85/95,
    zero XP, a quantity maximum bounded by the scarcest ingredient, and ordinary results at ticks
    2/4/6. Confirm the first admiral salmon result at tick 1 and repeats at ticks 3/5. Draynor
    cabbage 1967 and every out-of-order filling must fail. Test full inventory on both bucket-return
    mud stages, changing a selected slot while the menu is open, exhausting or replacing the next
    batch ingredient, movement and starting another Cooking action. Capture the literal menu title,
    action labels, success/failure messages, animation, sound, result-slot choice and packet order;
    compare those captures with the integration-tested functional contract before promotion.
28. Using only the workspace cache-233 RuneLite client, exercise all 22 potato and prerequisite
    bowl routes in both held-item orders. Confirm baked potato 6701 plus either butter 6697 or not
    garlic butter 24788 produces 6703 at boosted level 39 after two ticks for 40 XP. From 6703,
    verify cheese 1985->6705 at 47/40 XP; chilli con carne 7062->7054 at 41/15 XP plus bowl 1923;
    egg and tomato 7064->7056 at 51/45 XP with no bowl; mushroom and onion 7066->7058 at 64/55 XP
    with no bowl; and tuna and corn 7068->7060 at 68/10 XP plus bowl. Build every topping from its
    prerequisites: bowl+garlic with knife ->7074, then gnome spice 2169->7072; bowl+cooked meat
    with knife ->7070; spicy sauce plus cooked meat with knife or minced meat without one ->7062 at
    level 11 after one tick, returning the second bowl only for minced meat; bowl+egg ->7076 at
    level 13, then cooked egg 7078+tomato ->7064 at 23; bowl+mushroom 6004 and bowl+onion 1957 with
    knife ->7080/1871, cook them to 7082/7084 and combine at 57 ->7066; bowl+tuna with knife ->7086
    and bowl+cooked sweetcorn ->7088, then test all three level-67 routes to 7068, with a knife only
    for whole tuna and a returned bowl only when combining two bowl items. Confirm all actions are
    members-only, levels are boostable, knife loss during a delayed action cancels atomically,
    ordinary spice 2007 cannot replace gnome spice, cooked chicken cannot replace cooked meat, all
    explicit bowl returns work in a full inventory, and a newer Cooking action interrupts the old
    one. Capture exact success, level, membership and missing-knife wording; any animation/sound;
    result-slot choice; and packet order before promotion beyond `integration-tested`.
29. Using only the workspace cache-233 RuneLite client, operate each churn identity 11695, 11696,
    25720 and 35937 by native `Churn` and held-item use. From milk 1927 verify cream 2130 at boosted
    level 21 after 10 ticks for 18 XP, butter 6697 at 38 after 19 ticks for 40.5 XP, and cheese 1985
    at 48 after 26 ticks for 64 XP, returning bucket 1925 every time. From cream verify butter at
    38/22.5 XP/10 ticks and cheese at 48/46 XP/19 ticks with no pot return. From ordinary butter
    6697 and not-garlic butter 24788 verify cheese at 48/23.5 XP/10 ticks. Confirm the source-filtered
    choice order and quantity maximum, exact 10/19/26-tick repeat gaps, membership and boosted-level
    boundaries, selected-slot mutation, walking away and interruption by another Cooking action.
    With a full inventory, confirm each milk result succeeds and drops one empty bucket visible only
    to its owner; confirm all non-milk results remain one-for-one and create no container. Put milk,
    cream and both butters in the inventory together and record native-click source priority. On the
    Cooks' Guild metal churn confirm the metal 460/462/464 graphics; on the other three confirm the
    regular 459/461/463 graphics, alongside player sequences 2793/2794/2795. Capture the exact menu
    title, button text/action, no-input, membership, level and success feedback, sound, graphic
    placement and packet order before promotion beyond `integration-tested`.
30. Using only the workspace cache-233 RuneLite client, combine grapes 1987 and Zamorak's grapes
    20749 with jug of water 1937 in both held-item orders. Capture the exact quantity interface,
    action label, cap rejection, membership/level failures, sequence 7529, graphic 47, mixing
    message, any sound and inventory packet order. Confirm first results at tick 3 and repeats every
    2 ticks, with no immediate XP. Establish whether each result restarts fermentation from its own
    boundary and reconcile the Wiki's 20-result-tick rule with RuneLite's 13.8-second display from
    the graphic packet.
    Test ordinary wine at levels 35, 67 and 68 and Zamorak wine at 65, 97 and 98, forcing both
    success and failure where possible. Confirm outputs 1993/245 or shared bad wine 1991, exactly
    200 XP only per success, and that all ordinary and Zamorak unfermented wine in inventory and
    bank converts together using the pre-award level. Use counts 14, 15, 16, 30 and 32 to verify
    the fifteen-batch-plus-remainder roll grouping.
    Approach and reach the shared 60,000 cap with both types split across inventory and bank. Record
    the literal rejection and whether noted/storage variants count. Fill the bank so a stack with
    mixed success and failure needs an extra result slot; determine whether production defers,
    partially converts or uses another policy. When both stores hold wine, record how successes and
    failures are allocated between them. Log out at several points in the 20-tick window and record
    whether the timer pauses, continues offline or restarts. Compare every observation with the
    integration-tested functional contract before promotion.
31. Using only the workspace cache-233 RuneLite client, make the complete ordinary nettle-tea and
    hot-water chain. At boosted Cooking 20, combine bowl water 1921 and nettles 4241 in both item
    orders; capture the exact quantity interface, action, first/repeat result boundaries, messages,
    sounds and slots while producing nettle-water 4237 for zero XP. Heat it on fire and ordinary,
    Lumbridge and both Hosidius range classes at levels 20, 53 and 54, forcing success/failure where
    possible. Confirm empty-bowl failure 1923, tea success 4239, exactly 52 XP, sequences 897/896,
    78/412 ordinary and 90/424 or 103/437 Hosidius behavior, and whether Lumbridge has any hidden
    advantage. Record the literal success and boil-over messages and exact 1/3/4 timing rather than
    accepting the current evidence-based wording/timing inference.

    Pour bowl tea 4239 into empty cup 1980 for cup tea 4242, and test milky bowl 4240 to milky cup
    4243. Add bucket milk 1927 before and after pouring, confirming bucket 1925 and bowl 1923 returns,
    zero extra XP, both held-item orders, full inventories and exact synchronous timing, messages,
    sounds and result slots. Verify no production quest-state gate and separately keep porcelain-cup
    4245/4246 behavior in the Ghosts Ahoy acceptance suite.

    Heat two or more bowl-water 1921 and cup-water 4458 items on fires and every range class,
    confirming outputs 4456/4460, zero level/XP, exact one-tick first and four-tick automatic cadence,
    animations, messages and sounds. Finally pour multiple bowls 4456 into empty cups through the
    post-Poll-82 Make-All interface. Confirm the menu maximum, whether the first conversion is truly
    zero-tick and repeats one tick apart, returned bowls, interruption, stale selected slots and full
    inventory behavior before promoting this family beyond `integration-tested`.
32. Using only the workspace cache-233 RuneLite client, combine raw oomlie 2337 with palm leaf 2339
    in both held-item orders at boosted Cooking 50. Confirm immediate wrapped oomlie 2341, exactly
    10 XP, members/level failure precedence, result-slot choice, literal messages, any animation or
    sound and packet order. Cook wrapped oomlies on a fire, an ordinary range and both Hosidius
    variants, confirming sequences 897/896, 30 XP, the 106/450, 112/476, 124/488 and 137/501 curves,
    a two-tick direct result and four-tick Make-X results. Start at level 50 near a cold-drain area,
    let Cooking fall below 44 without restarting the batch and confirm production continues and can
    create burnt wrap 2345. Capture the exact chooser, success/burn feedback, animation duration,
    sound, result slots, interruption and packet order.

    Separately use raw unwrapped oomlie on a fire and range. Confirm which sources accept it, exact
    result timing, whether several items repeat, distinct burnt object 2426 and the two historical
    messages. Do not promote that forced-burn route until its source/timing boundary is captured.
    Keep palm-tree leaf acquisition, eating effects and Karamja diary credit in their respective
    world-object, consumption and diary acceptance suites.
33. Using only the workspace cache-233 RuneLite client, test raw karambwan 3142 on a fire, ordinary
    range and both Hosidius range classes before and after the full Tai Bwo Wannai Trio/Tinsay
    reward sequence. Before the reward, confirm poison karambwan 3146 is the only possible product;
    after it, confirm cooked karambwan 3144 is the default first choice and poison remains the
    second choice. Record the exact skill-menu title, entries, padding, quantity behavior and action
    mode. Select poison with warning varbit 9545 enabled, verify the literal warning and No/Yes
    order, decline once, then confirm once for exactly 80 XP; disable the warning in Settings and
    verify the same selection starts without confirmation. Confirm thorough success gives exactly
    190 XP and sets Karamja hard task varbit 3604 only on success.

    Force endpoint rolls at visible Cooking 1 and the ordinary level-99 boundary, then repeat at
    Hosidius and elite-Hosidius using 70/255, 82/267 and 95/280. Verify burnt karambwan 3148 on
    failure, Cooking/max-cape immunity, a one-tick manual result and four-tick automatic results,
    movement and newer-Cooking interruption, level-drain behavior, exact animations, success/burn
    messages, sounds, slots and packet order. Reheat burnt karambwan and confirm the exact octopus
    rejection. Log out and back in after the Tinsay reward and warning toggle to verify both survive
    account persistence before promotion beyond `integration-tested`.
34. Using only the workspace cache-233 RuneLite client, use raw beef 2132, raw bear meat 2136 and
    raw boar meat 25833 on every range class with membership enabled. Confirm cooked meat 2142 is
    the first choice and sinew 9436 the second, including when only one raw item is held; record the
    exact title, product labels, padding, quantities and action mode. Choose sinew at visible
    Cooking level 1, confirm 3 XP and `You dry a piece of meat and extract the sinew.`, then pin the
    exact boundary for a one-tick direct choice versus the documented immediate/3/4 Make-All
    results.

    Force success and failure at the 128/512 ordinary, 140/524 Hosidius and 153/537 elite-Hosidius
    endpoints. Confirm whether Lumbridge uses the ordinary curve, whether failure produces burnt
    meat 2146 with the ordinary meat message, and whether Cooking/max capes prevent it. Verify fires
    never offer sinew, free worlds retain only ordinary cooked meat, a full inventory safely
    replaces raw meat one-for-one, movement and a newer Cooking action interrupt the batch, and
    capture the exact animation, sound, result slot and packet order before promotion.
35. Using only the workspace cache-233 RuneLite client, combine iron spit 7225 in both held-item
    orders with raw bird meat 9978, raw beast meat 9986 and raw rabbit 3226. At boosted Cooking
    11/21/16 and boosted Firemaking 20, confirm immediate zero-XP outputs 9984/9992/7224, the exact
    messages, animation/sound, output slot and packet order. Repeat one level short in each skill,
    on a free world and with a full inventory to pin rejection precedence and wording.

    Use each skewered meat on ordinary fire 26185 and permanent cooking fire 43475. Confirm sequence
    1016, a result exactly three ticks later, roasted outputs 9980/9988/7223, burnt outputs
    9982/9990/7222, returned spit, and exactly 62.5/82.5/72.5 XP only on success. Force the
    155/255, 180/255 and 160/255 endpoints and verify level 99 plus Cooking/max cape immunity.
    Specifically lower Firemaking below 20 after preparation: confirm that bird roasting is blocked
    while the individually documented beast/rabbit recipes remain usable, or record contrary live
    behavior and reconcile the source conflict. Capture literal success/burn/level/capacity text,
    sound, output slots and packet order, then verify full-inventory refusal, walking away, stale
    selected slots and interruption by a newer Cooking action before promotion.
36. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy, switch to the
    Lunar spellbook and cast Bake Pie at Magic 65 with all 12 uncooked pies. Confirm component child
    105, one result every three ticks beginning at tick 3, inventory-slot order, player animation
    4413, graphic 746, guaranteed cooked outputs, full ordinary Cooking XP and 60 Magic XP, and one
    astral/five fire/four water runes consumed for every result. Repeat with a steam staff or other
    valid rune substitutions, a rune pouch, exactly one rune set, a full inventory and a boost that
    drains between two high-level pies.

    Capture the literal no-pie, quest, Cooking and rune failure text, sound ID, graphic height and
    packet/XP/inventory ordering. Mix lower-level and currently ineligible higher-level pies before
    and after one another to establish whether the production scan stops, skips or reorders them;
    reconcile that result before changing the current strict inventory-order stop. Interrupt before
    tick 3 and between results with walking, another spell and another skilling action. Finally bake
    a summer pie for the Varrock elite task and record its exact diary-state mutation. Compare cache
    sequence 4422 and spot animation 727 during the capture without substituting either unless the
    client actually observes it.
37. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy and Dream Mentor,
    switch to the Lunar spellbook and cast Humidify at Magic 68. Confirm spellbook child 110; one
    astral, three water and one fire rune; exactly 65 Magic XP once per successful cast; player
    animation 6294; rain graphic 1061; and a three-tick recast boundary. Capture the exact sound,
    graphic height, inventory/XP/rune packet order, whether graphic 2294 replaces 1061 indoors or
    under any other condition, and the literal members, quest, spellbook, level, rune and no-target
    feedback. Do not accept the current provisional no-target line without this capture.

    In mixed and full inventories, verify that one cast fills every empty vial, bowl, bucket, jug,
    cup, fishbowl and Chambers gourd vial; all four depleted waterskins; all eight depleted watering
    cans; clay; and each of the 20 pre-Sailing seedlings. Confirm result slots, unrelated-item
    preservation, full containers remaining unchanged, staff/combination/rune-pouch substitutions,
    spam clicks before ticks 1/2/3, movement and another utility spell. Verify that the three
    Sailing seedlings introduced in January 2026 do not appear in the cache-233 baseline.

    Fill Gricoller's can from empty and partially filled states, relog, Check it and consume charges
    before and after casting to identify the exact persistent 1,000-charge representation. Cast
    while holding an empty waterskin and record the Desert hard-diary mutation. In Chambers of
    Xeric, fill empty gourd vials and extinguish each applicable Great Olm flame-wall segment. In
    Soul Wars, capture the restriction and exact cloud-related rejection. These integrations must
    remain below `verified` until their backing systems and observations exist.
38. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy and Dream Mentor,
    switch to the Lunar spellbook and cast Hunter Kit at Magic 71. Confirm spellbook child 114; two
    astral and two earth runes; exactly 70 Magic XP; player animation 6303; graphic 1074; cast sound
    3615; and a three-tick recast boundary. Repeat with an earth staff, combination/rune-pouch
    substitutions, Fountain of Rune, every members/quest/book/level failure and a full inventory.
    Capture exact full-inventory/member feedback, graphic height and packet ordering.

    Cast while already holding item 11159 and while holding all eight contents, confirming the
    exact duplicate-equipment message. Remove one content and confirm casting becomes valid. Open
    the kit with exactly seven additional free slots and confirm one each of noose wand 10150,
    butterfly net 10010, bird snare 10006, rabbit snare 10031, teasing stick 10029, unlit torch
    596, impling jar 11260 and box trap 10008, plus opening sound 3616; repeat with six free slots
    and confirm the kit and inventory are unchanged. Capture result-slot order and inventory/audio
    packet order, F2P behavior and the generic Destroy confirmation/note.

    On every shoreline, bridge and transport boundary into Entrana, attempt to open the kit and
    prove the exact production area plus `Some forms of Hunter gear are not appreciated by the
    monks.` rejection. Then use the cache-233 Feldip Hills and Rellekka hunting experts before and
    after claiming the one-time Poll 85 kit, across relog, and record exact NPC IDs, dialogue,
    prerequisite and persistent state. Do not promote the extended group until both integrations
    are implemented and observed.
39. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy, switch to the
    Lunar spellbook and cast Magic Imbue at Magic 82. Confirm spellbook child 128; two astral,
    seven fire and seven water runes; exactly 86 Magic XP; player sequence 8970; synth 2888; no
    action delay or shared utility-spell cooldown; and an exact 21-tick recast boundary. Repeat
    with steam battlestaff, mystic steam staff and twinflame staff, water staff plus Tome of fire,
    combination and rune-pouch substitutions, Fountain of Rune and every members/quest/book/level
    and rune failure. Record literal failure and active-recast feedback, packet ordering, whether
    sequence 8982 participates and the exact buff-display transition while varbit 5438 moves from
    2 to 1 to 0.

    During the active 21 ticks, exercise the now-implemented elemental and Aether core summarized
    in case 129. Confirm multiple batches under one cast and test expiry immediately before and
    after altar interaction.

    Cast immediately before logout, login, death, teleport, region change and interruption; record
    whether the effect survives each boundary and whether client state ever becomes stale. Keep
    the matching-client group open until every presentation and lifecycle observation is closed.
40. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy and Dream Mentor,
    switch to the Lunar spellbook and cast Stat Spy at Magic 75 on another player. Confirm spellbook
    child 118; two astral, two cosmic and five body runes; exactly 76 Magic XP; player sequence
    6293; caster graphic 1060; synth 3620; native side interface 523; the `Player ~ Name` heading;
    and all 23 current/base skill pairs in cache child order. Boost and drain multiple target stats,
    including Hitpoints, immediately before casting and confirm the snapshot values, values above
    99 and no virtual-level substitution.

    Capture the exact target notification, capitalization and punctuation; whether the caster also
    receives a message; confirm target graphic 738 and capture its exact height, slot and delay;
    and record the precise audiovisual/interface/XP/rune packet order. Measure the earliest
    possible recast after returning from the Stat Spy side panel and how that lock interacts with
    offensive combat spells and other utility spells. Repeat with Accept Aid off, privacy-related
    client settings, a busy target, both players moving, a target logging out, maximum range, just
    outside maximum range, self-target packet injection, Fountain of Rune and rune-pouch sources.
    Confirm that no cast deals damage, rolls accuracy, starts combat, retaliates, skulls either
    player or changes PvP timers. Do not promote the extended group until these observations are
    reconciled and automated.
41. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy, switch to the
    Lunar spellbook and cast Spin Flax at Magic 76. Confirm spellbook child 146; one astral, two
    nature and five air runes; exactly 75 Magic XP per successful cast; at most five flax converted
    in inventory order; exactly 15 Crafting XP per converted flax; player sequence 4413, graphic
    728 and synth 3615; a five-tick cast interval; and the exact no-target message
    `You don't have any flax to spin.` Test one through six flax, a full inventory, boosted and
    drained Crafting, rune pouch, an air-providing staff and Fountain of Rune.

    Click a second time during the first cast and verify that exactly one follow-up cast executes
    at the fifth tick; click a third time and capture whether it replaces, preserves or rejects the
    queued cast. Cancel or replace the queue through movement, teleport, combat, another utility
    spell, banking and interface changes, recording the exact behavior. Capture graphic height and
    delay plus the audiovisual, inventory, rune, XP and message packet order.

    Repeat while carrying an empty, partially filled and full bow-string spool at every capacity
    tier. Confirm that automatically produced strings enter its account-backed charge state first,
    that only capacity overflow remains loose, and that losing/reacquiring the physical spool does
    not erase stored charges. Do not promote the extended group until the matching-client
    observations are reconciled and automated.
42. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy, switch to the
    Lunar spellbook and cast String Jewellery at Magic 80. Confirm spellbook child 126; two astral,
    five water and ten earth runes per result; exactly 83 Magic and 4 Crafting XP per item; player
    sequence 4412, graphic 730 and synth 2905; the first result on tick three and every subsequent
    result three ticks later in inventory order. Test gold, opal, jade, topaz, sapphire, emerald,
    ruby, diamond, dragonstone, onyx and zenyte amulets, unstrung holy symbol, unstrung unholy
    emblem and salve shard. Confirm that the unstrung M'speak amulet is ignored even though manual
    ball-of-wool stringing accepts it, and capture the literal no-eligible-item feedback.

    Repeat with mixed eligible items, duplicates, one through 27 outputs, a full inventory, a salve
    shard at Crafting 1, depleted and boosted Magic, rune pouch, combination runes, an
    earth-providing staff and Fountain of Rune. Remove or replace the next item and exhaust each
    rune source immediately before a scheduled result; confirm no partial item, rune or XP change.
    Record the precise animation/graphic/sound, item, rune and XP packet order, graphic height and
    delay, whether synth 2905 restarts or overlaps on consecutive results, and second-click queue
    replacement behavior.

    Interrupt the automatic process by walking, interacting with a player, interacting with an
    NPC, entering combat, teleporting, banking, changing spellbook and casting another spell.
    Record the exact cancellation boundary and any feedback. Do not promote the client-parity
    group until every observation is reconciled with automated behavior.
43. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy and the hard
    Fremennik Diary, switch to the Lunar spellbook and cast Tan Leather at Magic 78. Confirm
    spellbook child 122 exposes `Cast` and `Setup`; one astral, two nature and five fire runes plus
    exactly 81 Magic XP are charged once per batch; at most five hides transform after three ticks;
    and player sequence 3014, graphic 322 and synth 2879 play. Exercise cowhide to soft and hard
    leather, both snake-hide inputs to snakeskin, and all four dragonhide colours. Confirm that no
    Crafting XP is awarded.

    Use Setup before any cast, after choosing each mode, after logout/login and after reconnect to
    capture its exact title, option text, default and persistence. Cast with interleaved cowhide,
    both snake hides, all four dragonhides and unrelated items to prove the exact mixed-hide slot
    order and five-item cap. Capture literal no-hide, diary, quest, members and level feedback.
    Repeat with a full inventory, partial batches, rune pouch, combination runes, a fire-providing
    staff and Fountain of Rune.

    Click a second and third time during the first cast and determine whether the third request is
    rejected, retained or replaces the queued request. Cancel or replace it through movement,
    teleport, combat, another utility spell, banking and interface changes. Record graphic height
    and delay plus the exact animation, graphic, sound, item, rune, XP and message packet order. Do
    not promote the client-parity group until every observation is reconciled and automated.
44. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy and Dream Mentor,
    switch to the Lunar spellbook and cast Plank Make at Magic 86. Confirm spellbook child 133;
    two astral, one nature and fifteen earth runes; exactly 90 Magic XP per plank; player sequence
    6298, graphic 1063 and synth 3617; and no Construction XP. Manually target normal, oak, teak
    and mahogany logs and confirm fees of 70, 175, 350 and 1,050 coins, outputs 960, 8778, 8780 and
    8782, and the post-2024 rule that each plank enters the first available inventory slot. Repeat
    while the cast consumes the last rune stacks to capture whether newly freed rune slots are
    eligible before the output is inserted.

    Enable and disable each of the four Plank Make warnings associated with cache varbits 624,
    2319, 2320 and 3939. Capture exact dialog wording, button order, default state, setting polarity,
    persistence, whether manual and automatic results both consult the warning, and whether one
    acceptance starts the whole automatic batch. Capture exact invalid-target, insufficient-coin,
    quest, members, spellbook and level feedback. Use mixed log types and target a later slot to
    corroborate the dated change note and automated rule that both the immediate result and
    automatic continuation use the first supported log of any type in inventory order.

    Measure result ticks from the outbound interaction: the spell article specifies an immediate
    first result, a second result five ticks later and six-tick subsequent results, while recipe
    tables summarize autocasting as five ticks. Resolve the conflict with repeated captures. Click
    again at ticks one, two and three and then a third time to determine queue replacement. Repeat
    with rune pouch, combination runes, an earth-providing staff, Fountain of Rune, exact resource
    exhaustion boundaries and a full inventory.

    Carry sawmill vouchers with zero, one and several ordinary free slots and verify that one
    voucher produces one extra matching plank only when capacity exists. Repeat with every plank
    sack capacity boundary to establish whether the extra plank enters the sack and when a voucher
    is retained. Cast on 20 mahogany logs inside the Varrock sawmill and verify elite-diary credit.
    Interrupt automatic production by walking, banking, combat, teleport, interface changes and
    another spell; record exact cancellation, graphic height/delay and audiovisual, inventory,
    coin, rune and XP packet order. Do not promote the extended group until all observations are
    reconciled and automated.
45. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy and the hard
    Fremennik Diary, switch to the Lunar spellbook and cast Recharge Dragonstone at Magic 89.
    Confirm spellbook child 136 exposes `Cast`; four water, one astral and one soul rune are
    charged once for the whole inventory; exactly 97.5 Magic XP is awarded once; and player
    sequence 4409, graphic 282 and synth 139 play. Exercise every zero-through-three charge state
    of ordinary and trimmed amulets of glory and confirm output at four charges in the same slots.
    Before and after Legends' Quest state 75, repeat for every zero-through-three charge state of
    skills necklaces and combat bracelets. Confirm rings of wealth and eternal glories never
    change.

    Isolate ordinary and trimmed glories, skills necklaces and combat bracelets at four, five and
    six charges. Determine whether each is ignored, accepted without change or downgraded to four,
    and whether a cast containing only already-four-or-higher items consumes runes or awards XP.
    Repeat with unnoted, noted and equipped variants, including mixed inventory/equipment cases,
    to prove the exact inventory boundary. Capture the literal feedback for no jewellery,
    Legends'-locked-only jewellery, missing diary, missing Lunar Diplomacy, wrong spellbook, low
    Magic and each missing rune.

    Cast with mixed eligible families, duplicates, a full inventory, rune pouch, combination
    runes, a water-providing staff and Fountain of Rune. Click again at ticks zero through three
    and try other utility/combat actions to establish the exact cooldown, queue and cancellation
    semantics. Record graphic height/delay and exact animation, graphic, sound, item, rune, XP and
    message packet order. Do not promote the client-parity group until the five/six-charge and
    trimmed-glory observations are reconciled and all remaining boundaries are automated.
46. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy, switch to the
    Lunar spellbook and cast Cure Me at Magic 71. Confirm component 218:113 exposes `Cast`; two
    astral, two cosmic and one law rune are consumed; exactly 69 Magic XP is awarded; and sequence
    4411, graphic 742 at height 96 and synth 2884 play. Capture the exact success message, graphic
    slot/delay and audiovisual, var, rune and XP packet order.

    Apply every ordinary poison stage from 1 through 100 and every venom next-damage stage from 6
    through 20. Confirm ordinary poison becomes state zero without negative immunity, while venom
    becomes ordinary poison whose next hit has exactly the same damage. Capture varps 102 and 3777
    plus varbit 6251 before the cast, immediately after the cast and through the next poison cycle
    to establish whether the undocumented secondary state changes and whether the damage timer is
    reset. Repeat during poison and venom immunity.

    Attempt a direct spellbook-component cast while clean and while immune, independently of the
    health-orb shortcut, and record whether the client sends the operation and whether the server
    returns text. Spam at multiple points inside one tick and verify the exact cooldown message,
    then alternate Cure Me with combat spells and other Lunar utility spells to establish whether
    the one-tick lock is spell-local, combat-shared or global. Repeat with missing quest access,
    wrong spellbook, low Magic, every missing rune, rune pouch, LMS fake-rune pouch and Fountain of
    Rune. Do not promote the client-parity group until all observations are reconciled and
    automated in the shared poison lifecycle.
47. Using only the workspace cache-233 RuneLite client with two connected players, complete Lunar
    Diplomacy, switch both accounts to the Lunar spellbook and cast Cure Other at Magic 68. Confirm
    component 218:109 targets a player and exposes `Cast`; one astral, one law and ten earth runes
    are consumed; exactly 65 Magic XP is awarded; the caster uses sequence 4411; the recipient uses
    graphic 738 at nominal height 96; and the cast emits the cache Cure synth 2884. Capture the exact
    success feedback, whether either player receives it, graphic slot/delay and the complete
    animation, graphic, sound, var, rune and XP packet order.

    Apply every ordinary poison state from 1 through 100 and each venom next-damage stage from 6
    through 20 to the recipient. Confirm ordinary poison becomes zero without immunity and venom
    becomes ordinary poison whose next hit has the same damage. Record varps 102 and 3777 plus
    varbit 6251 before, immediately after and through the next poison cycle to establish secondary
    mutation and timer rescheduling. Repeat with a clean recipient and during poison and venom
    immunity, recording whether the client sends the target operation and whether any resources,
    XP, presentation or messages occur.

    Disable Accept Aid and confirm the exact rejection text, capitalization and punctuation, then
    repeat while enabled. Test self-target packet injection; exactly range eight and one tile
    beyond; either player moving; target logout between click and arrival; every missing access,
    level and rune requirement; rune pouch; an earth-providing staff; LMS fake runes; and Fountain
    of Rune. Measure the earliest repeat cast and alternate Cure Other with offensive and utility
    spells to resolve whether the provisional five-tick lock is local, combat-shared or global.
    Confirm no attempt damages, rolls accuracy, starts combat, retaliates, skulls or changes PvP
    timers. Compare the production sound with Cure Me because the archived Wiki recordings differ
    while cache symbols expose only one named Cure synth. Do not promote the extended group until
    every observation is reconciled and automated.
48. Using only the workspace cache-233 RuneLite client with enough connected accounts to cover a
    3×3 square, complete Lunar Diplomacy, switch to the Lunar spellbook and cast Cure Group at Magic
    74. Confirm component 218:117 exposes `Cast`; two astral, two cosmic and two law runes are
    consumed; exactly 74 Magic XP is awarded; the caster uses sequence 4409; each affected player
    receives graphic 744 at nominal height 96; and the cache Cure synth 2884 plays. Capture the
    exact caster and recipient messages plus the animation, graphic, sound, var, rune and XP packet
    order.

    Poison the caster and every surrounding account, with one account on each adjacent and diagonal
    square and one sharing the caster's square. Confirm the effect is the exact one-square
    Chebyshev/3×3 area, includes the caster and same-tile accounts, excludes distance two and other
    planes, and requires Accept Aid for every recipient other than the caster. Toggle the caster's
    own Accept Aid to establish whether self-benefit remains independent. Repeat with mixed
    ordinary poison, every venom next-damage stage from 6 through 20, clean state, poison immunity
    and venom immunity. Record which clean or immune players receive graphic 744 and whether their
    secondary poison state or timer changes.

    Cast while nobody in range is poisoned and record whether runes, XP, animation, graphic and
    sound are still produced. Move, teleport and log recipients in or out between click and cast;
    swap their Accept Aid state during the same window; and establish the recipient snapshot and
    iteration timing. Spam-cast across tick boundaries and alternate Cure Group with Cure Me, Cure
    Other, combat spells and other Lunar utilities to resolve the provisional five-tick interval
    and shared lock. Repeat with missing quest access, wrong spellbook, low Magic, each missing rune,
    ordinary rune pouch and Fountain of Rune. Compare the sound against Cure Me and Cure Other,
    whose archived Wiki recordings differ despite the single named cache Cure synth. Confirm no cast
    damages, rolls accuracy, starts combat, retaliates, skulls or changes PvP timers. Do not promote
    the extended group until every observation is reconciled and automated.
49. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy and Dream Mentor,
    switch to the Lunar spellbook, lower Hitpoints and cast Dream at Magic 79. Confirm component
    218:125 exposes `Cast`; two astral, one cosmic and five body runes are consumed; exactly 82
    Magic XP is awarded; sequence 7627 transitions to sleeping sequence 6296; Z graphic 1056 backed
    by sequence 6302 appears; sequence 7628 wakes the player; and no sound is emitted. Capture the
    exact transition ticks, graphic slot/height/repeat cadence and animation, graphic, rune, XP and
    message packet order.

    Starting immediately after the cast at multiple phases of the ordinary Hitpoints-regeneration
    timer, record the first ten Dream heals and prove whether they occur every 33/34 ticks, every 20
    seconds in wall-clock time or on another pattern. Confirm exactly three additional Hitpoints per
    minute, a timer independent of normal regeneration, indefinite sleep until base Hitpoints and
    wake ordering on the final heal. Repeat with Rapid Heal, a Hitpoints/max cape and a regen
    bracelet separately and combined; log all simultaneous ordinary/Dream heal boundaries.

    Interrupt at each presentation phase with walking, running, combat, direct damage, poison and
    venom damage, eating, potion use, prayers, inventory operations, object/NPC/player interactions,
    another spell, interfaces, teleport, logout and death. Confirm chat does not interrupt and
    changing each armour slot does not interrupt, including inventory-full swaps. Force damage on a
    scheduled Dream-heal tick to establish processing order. Spam direct recasts while active and
    after reaching full health, recording resource behavior and exact full-health feedback.

    Attempt Dream inside every Nightmare Zone state and capture the exact rejection text, resolving
    the historical `Waking up from the dream` versus `death` conflict. While successfully asleep,
    use jar of dreams 24495 and capture the black-screen duration, Nightmare face/interface,
    Armadyl-crossbow-special sound and interruption behavior. Repeat all gates with wrong quest
    states, wrong spellbook, Magic 78, each missing rune, rune pouch, body/cosmic substitutions and
    Fountain of Rune. Exercise login/world-hop/network-loss races at each phase. Do not promote the
    client-parity group until these observations are reconciled and automated.
50. Using only the workspace cache-233 RuneLite client and a second account with Accept Aid enabled,
    complete Lunar Diplomacy, switch to the Lunar spellbook and cast Heal Other at Magic 92. Confirm
    component 218:140 exposes `Cast`; three astral, one blood and three law runes are consumed;
    exactly 101 Magic XP is awarded; the caster uses sequence 4411; the recipient receives graphic
    738 at nominal height 96; and synth 2895 is heard by the correct participant. Capture whether
    recipient-only synth 2892 is also sent, and record the exact success feedback plus animation,
    graphic, sound, Hitpoints, hitmark, headbar, rune and XP packet order.

    At 99 current/base caster Hitpoints and a sufficiently damaged recipient, prove that the caster
    takes a visible typeless 74 hit and the recipient gains 74 Hitpoints. Repeat across current/base
    pairs around every integer boundary, including boosted Hitpoints, to prove the minimum-11%-of-
    base gate and floor-75%-of-current formula. Leave the recipient short by exactly one Hitpoint
    and establish that both heal and caster loss cap to one; repeat at full, overhealed and zero
    Hitpoints and capture exact rejection text and whether the target packet is accepted. Test
    simultaneous incoming lethal/non-lethal caster damage and target healing to resolve processing,
    death, interruption, prayer, recoil and combat-state behavior.

    Disable Accept Aid and record exact capitalization/punctuation; inject self-target; test an
    ordinary, Ironman, Hardcore, Ultimate and Group Ironman recipient once account modes exist; and
    exercise safe, Wilderness, single-way, multiway and singles-plus areas. Confirm whether any
    restriction depends on caster or recipient mode and whether a support cast starts combat,
    retaliation, skull, PJ ownership or logout timers. Move, teleport, die and log either player
    out between click, route completion and resolution; test exactly range eight and one tile
    beyond; and repeat on another plane.

    Spam casts across tick boundaries and alternate Heal Other with Cure Other, Heal Group,
    offensive spells, food, potions and other actions to resolve the provisional five-tick ordinary
    lock and all cross-locks. Explicitly prove that Heal Other has no 90-second special cooldown,
    unlike Heal Group. Repeat with wrong membership/quest/spellbook, Magic 91, every missing rune,
    rune pouch, combination substitutions and Fountain of Rune. Do not promote the account/client-
    parity group until every observation is reconciled and automated.
51. Using only the workspace cache-233 RuneLite client and two ordinary member accounts, complete
    Lunar Diplomacy, select the Lunar spellbook and cast Energy Transfer at Magic 91. Start the
    caster at exactly 100% special energy and above 10 Hitpoints; start the Accept-Aid-enabled
    recipient with partial run and special energy. Confirm component 218:139 exposes `Cast`, three
    astral/two law/one nature runes and exactly 100 Magic XP are charged, the caster's special bar
    becomes zero, the recipient's run and special bars both become full, and a visible typeless
    10-Hitpoints self-hit leaves the caster alive. Capture sequence 4414, recipient graphic 734 and
    its exact slot/height/delay, and determine whether synths 2885 and 2906 are respectively local
    to caster and recipient. Capture the complete audiovisual/stat/run/special/rune/XP/hitmark/
    headbar packet order.

    Repeat at caster Hitpoints 10 and 11 and confirm the exact published low-health message. Test
    99% special energy, self-target, dead recipient, Accept Aid off, recipient already full in one
    or both energy pools, wrong membership/quest/spellbook, Magic 90, every missing rune, rune
    pouch and combination substitutions, and Fountain of Rune. Record exact feedback and prove
    every rejected attempt is atomic. Equip Bryophyta's staff with zero, one and multiple stored
    nature charges; separately capture its cache-233 varbit/storage behavior, one-charge use, 1/15
    single-charge saving roll and message, depletion transition, Configure-Charges/Check/Uncharge,
    bank/trade/death behavior, and interaction with a spell that needs more than one nature rune.

    Exercise both participants across normal-world Wilderness single-to-single, multi-to-single,
    single-to-multi and multi-to-multi boundaries. On a PvP world, repeat in safe zones and every
    modeled single/multi PvP area, then target a player at each tick after PvP combat to determine
    the exact meaning of `recently`. Test the ordinary and cross-action cast rate with adjacent
    support/offensive spells, target movement from range eight to nine, plane changes, logout and
    death between click, route completion and resolution. Do not promote either Energy Transfer
    parity group or the Bryophyta's-staff group until all observations are reconciled and automated.
52. Using only the workspace cache-233 RuneLite client and two ordinary member accounts, complete
    Lunar Diplomacy, select the Lunar spellbook and cast Vengeance Other at Magic 93. Confirm
    component 218:141 exposes player-target `Cast`; exactly three astral, ten earth and two death
    runes and 108 Magic XP are charged; the caster uses sequence 4411; the recipient receives
    graphic 725 and `You have the power of vengeance!`; and the caster, recipient and nearby
    observers hear the correct routing of synth 2908. Capture the graphic's exact slot, height and
    delay and the complete animation/graphic/sound/message/var/rune/XP packet order.

    Cast again on ticks 0 through 50 and capture the exact cooldown boundary, varbit 2451 lifetime,
    icon state and literal rejection text. Alternate Vengeance Other with self Vengeance to prove
    the shared cooldown. Test self-target, dead recipient, Accept Aid off, already-armed recipient,
    both players inside and outside a POH, every caster/recipient single-way and multiway Wilderness
    pairing, singles-plus, wrong membership/quest/spellbook, Magic 92, each missing rune, rune pouch,
    earth substitutions, Fountain of Rune and a blighted Vengeance sack with blighted items disabled
    and enabled. Record every exact failure and prove rejected casts are atomic. Log out, world-hop,
    reconnect and enter/leave a POH at every cooldown/effect phase to resolve persistence and cleanup.

    Cast self Vengeance at Magic 94 with exactly four astral, ten earth and two death runes and
    confirm 112 Magic XP, graphic 726, synth 2907 and no movement interruption outside PvP. Repeat
    in every Wilderness/PvP/safe-area boundary and prove sequence 8316 is used outside PvP while
    sequence 8317 is used in PvP; capture the authoritative server updates for client varbit 8121.
    Confirm self Vengeance remains legal in single-way Wilderness and test ordinary runes, the LMS
    fake rune pouch and an enabled/disabled blighted Vengeance sack. Verify shared cooldown in both
    directions with Vengeance Other and the exact 49/50-tick boundary.

    Select component 218:142's `Animation` operation. Confirm interface 525 opens with DB row 4094,
    default and League IV entries, varps 4559/4562 and clientscript 7707 arguments in exact packet
    order. Unlock with scroll 28696 and verify one item is consumed, sequence 10625 plays, varbit
    17327 persists and the alternate remains disabled until explicitly selected. Capture exact
    non-members and duplicate-use feedback and verify both retain the scroll; log out, world-hop and
    reconstruct the account to prove durable persistence. Toggle each animation, try disabling the
    last enabled choice, enable both and statistically verify per-cast selection. Confirm the League
    IV result uses sequence 10626, graphic 2605, synth 7569 and override varbit 17316, including PvP
    areas.

    Arm Vengeance and receive player and NPC melee, ranged, magic and typeless hits at damage 0, 1,
    odd/even rounding boundaries, overkill and lethal values. Confirm full incoming damage remains,
    the rebound is `max(1, floor(actual damage * 0.75))`, the target says `Taste vengeance!`, the
    effect is consumed once, and determine its exact impact cycle. Repeat with poison, venom,
    disease, environmental and self damage; simultaneous hits; protection prayers; Smite; ring of
    recoil; reflected lethal damage; player/NPC death and logout. Exercise every NPC in historical
    recoil-immune revision 14902396 and every visible morph. Do not promote the Vengeance groups
    until self Vengeance is implemented and all matching-client observations are reconciled and
    automated.
53. Using only the workspace cache-233 RuneLite client and at least six ordinary member accounts,
    complete Lunar Diplomacy, select the Lunar spellbook and cast Heal Group at Magic 95. Confirm
    component 218:143 exposes `Cast`; exactly four astral, three blood and six law runes and 124
    Magic XP are charged; the caster uses sequence 4409 and synth 2894; every selected recipient
    receives graphic 745 at its exact slot/height/delay and synth 2893; and varbit 925 starts the
    RuneLite 90-second timer. Capture the complete animation/sound/graphic/Hitpoints/hitmark/
    headbar/var/rune/XP packet order and exact success feedback.

    Arrange injured Accept-Aid players on the caster's tile, every adjacent tile, two tiles away
    and another plane. Prove the exact area, then place six eligible recipients in that area and
    determine selection order across login order, player-list slot, distance and position. Repeat
    with full-health, overhealed, zero-Hitpoints and Accept-Aid-disabled candidates, including a
    mix where fewer than five are eligible. Exercise normal, Ironman, Hardcore, Ultimate and Group
    Ironman modes when the account-mode runtime exists and capture every eligibility rule and
    literal rejection.

    Test caster current/base Hitpoints and recipient deficits around every integer boundary. Use
    recipient counts one through five where `(floor(current * 75 / 100) + 5)` both divides evenly
    and leaves each possible remainder. Give one or more recipients deficits below the equal share
    and establish whether unused shares or integer remainders are redistributed, and whether the
    caster loses the full nominal sacrifice or only effective healing. Repeat at 10% and 11% of
    base health, with boosted Hitpoints, simultaneous incoming damage and a lethal boundary.

    Recast on ticks 0 through 150 to prove the exact cooldown boundary, varbit lifetime, icon and
    literal cooldown feedback. Log out, world-hop, reconnect and reconstruct the account during the
    cooldown to prove persistence. Compare an idle weapon with active attack delays at one through
    several remaining cycles; verify Heal Group adds exactly three cycles only to an existing
    delay, and establish ordering against queued attacks, standard food, combo food and other Lunar
    support spells. Resolve the provisional ordinary five-tick lock and every cross-action lock.

    Repeat with caster and recipients in ordinary PvM singles, Wilderness singles/multi and
    singles-plus, PvP-world safe/singles/multi areas, and every active/recent-combat permutation.
    Move, teleport, log out or kill candidates between click and resolution. Test wrong membership,
    quest, spellbook and Magic 94; every missing rune; rune pouch and substitutions; Fountain of
    Rune; no eligible recipients; and all-full recipients. Do not promote the Heal Group groups
    until every observation is reconciled and automated.
54. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy and Dream Mentor,
    select the Lunar spellbook and cast Spellbook Swap at Magic 96. Confirm component 218:144,
    three astral/two cosmic/one law runes, 130 Magic XP, sequence 6299, graphic 1062 and synth 3613.
    Record the exact picker labels, order, hotkeys, cancellation behavior and literal messages.
    Observe varbits 3617 and 4823 for Standard, Ancient and Arceuus selections and determine the
    authoritative encoding of 4823 rather than assuming the spellbook enum value.

    Test a new account, Desert Treasure I complete/incomplete accounts and Tyss-unlocked/locked
    Arceuus accounts. Prove Standard is always offered, Ancient only with its unlock and Arceuus
    only after Tyss, including an account that has not completed A Kingdom Divided. Repeat every
    failure boundary: non-member, both quest boundaries, visible and boosted Magic 95/96, each
    missing rune, rune pouch and every substitution, Fountain of Rune, cancel, changed unlocks or
    resources while the picker is open, duplicate input and a second Spellbook Swap attempt.

    After each selected book, attempt every spell routing family: self utility, inventory target,
    ground item, location, NPC target, player support, player/NPC combat, teleport and home
    teleport. For each family compare an access failure, resource failure, committed cast whose
    downstream effect later fails, splash/miss and success; establish exactly which state consumes
    the swap. Prove reversion immediately after the qualifying cast and at 199/200 tick timeout
    boundaries, including simultaneous timer/cast ordering.

    Cast while stationary and while already walking/running. Confirm the exact four-tick input and
    movement lock, that the moving route is not cancelled, that no run energy is consumed during
    all four cast ticks and that normal drain resumes on the first eligible boundary. Attempt new
    routes, food, combat, banking, teleporting, damage, logout and death on each tick. Capture the
    complete interface/animation/graphic/sound/var/rune/XP ordering.

    Enter Last Man Standing with an active swap and establish the exact start-cycle cancellation
    order against map entry, equipment/stat setup and queued casts. Log out, world-hop, reconnect
    and reconstruct the account at several remaining durations to determine whether expiry is
    persisted or deliberately session-local. Do not promote either Spellbook Swap group until all
    observations are reconciled and automated.
55. Using only the workspace cache-233 RuneLite client, complete Lunar Diplomacy, activate the
    Lunar spellbook and test Moonclan, Ourania, Waterbirth, Barbarian Outpost, Khazard, Fishing
    Guild, Catherby and Ice Plateau teleports at their exact level boundaries. Confirm objects
    9111/20759/9114/9117/9120/9127/9129/9131, components
    218:111/147/115/119/123/131/134/137, each rune and XP contract, sequence 4423, graphic 747,
    synth 200, the four-tick cast boundary and the precise landing tile or landing-area variation.

    Test Ourania before and after speaking to Baba Yaga while on Lunar. Capture the literal locked
    message, varbit-5376 transition and whether level, quest, spellbook or rune failures take
    precedence. Reconnect and reconstruct the account to prove the unlock persists. Test all other
    spells at Lunar Diplomacy 189/190, non-members, wrong spellbook, visible and boosted Magic one
    below/at requirement, each missing rune, rune pouch, elemental staves, combination runes and
    Fountain of Rune.

    For Ice Plateau, capture the exact Deep Wilderness warning interface, title, body, choice text,
    hotkeys and default focus. Confirm cancel/close/death/logout/movement and changed resources or
    access while the warning is open are atomic. Determine whether a warning setting can suppress
    the spell warning at cache 233, and distinguish it from the Ice plateau teleport-tablet
    setting. Capture the full click/warning/rune/XP/animation/graphic/sound/teleport packet order.

    Repeat every spell while stationary, walking, running, fighting and performing a skilling
    action; test Tele Block and source Wilderness levels 20/21, Deadman teleport delays, damage and
    death on each cast tick, spam clicks and other spell/item/interface input. Then research all
    seven `Tele Group` variants separately: exact 3-by-3 candidate area, shared-tile and plane
    rules, Accept Aid, recipient prompt and timeout, caster-versus-recipient timing, movement/logout/
    death races, Wilderness warnings and Deadman behavior. Do not promote either Lunar destination-
    teleport group until every observation is reconciled and automated.
56. Using only the workspace cache-233 RuneLite client and at least three ordinary member accounts,
    test Tele Group Moonclan, Waterbirth, Barbarian, Khazard, Fishing Guild, Catherby and Ice Plateau
    at exact Magic levels 70/73/76/79/86/88/90. Confirm spell objects
    9112/9115/9118/9121/9128/9130/9132, components 218:112/116/120/124/132/135/138, their exact
    rune and XP contracts, sequence 4423, graphic 747, synth 200 and four-tick caster teleport.

    Place recipients on the caster's tile, every adjacent tile, two tiles away and another plane.
    Toggle Accept Aid independently and prove each eligible player receives a separate interface
    326 offer with exact caster-name, destination, Accept and Decline text. Capture offer timeout,
    default focus, duplicate-offer replacement and the effects of close, decline, logout, death,
    movement, teleport, plane change and a second caster's overlapping offer.

    Accept on every tick before expiry and capture the recipient's exact animation, graphic, sound,
    movement lock and teleport tick. Determine whether recipient presentation is four ticks, whether
    source Tele Block and Wilderness level are checked at offer or acceptance, and the full interface/
    animation/graphic/sound/teleport packet order. Verify independent outcomes when several players
    accept, decline or disconnect from one cast and statistically validate every landing polygon.

    For Moonclan, compare recipients who have never visited Lunar Isle with accounts at each nearby
    Lunar Diplomacy varbit-2448 state and after their first visit. Identify the exact server-side
    threshold and literal rejection. Repeat all seven spells with normal, Ironman, Hardcore,
    Ultimate and Group Ironman recipients to pin Accept Aid/account-mode exclusions once that
    subsystem exists.

    For Ice Plateau, capture both caster and recipient Deep Wilderness warnings, settings, wording,
    cancellation and changed-state revalidation. Repeat every family member across source Wilderness
    20/21, Tele Block boundaries, PvP/Deadman worlds, combat and moving/skilling actions. Establish
    Deadman's exact extra delay and all interruption behavior. Do not promote the group-teleport
    core or extended destination-teleport group until every observation is reconciled and automated.
57. Using only the workspace cache-233 RuneLite client and at least six member accounts, select a
    Restore, Energy, Prayer, Super energy, Super restore, Sanfew serum or Stamina potion and target
    Stat Restore Pot Share at component 218:127. Confirm spell object 9123, Magic 81, ten earth/ten
    water/two astral runes, 84 Magic XP, sequence 4413 and graphic 733. Capture the exact sound,
    graphic slot/height/delay, cast lock, messages and inventory/rune/XP/stat packet order.

    For every family and every dose size, place recipients on the caster's tile, every 5-by-5
    boundary tile, one tile beyond and another plane. Toggle Accept Aid independently. Vary potion
    doses from one through four with more eligible recipients than doses, reorder nearby players,
    and determine authoritative candidate priority. Confirm precisely one dose per affected other
    player, no extra caster dose, the final vial transformation, full-inventory behavior, and the
    exact zero-eligible-recipient outcome and feedback.

    Test every formula around integer boundaries and caps with drained and boosted stats. For each
    Prayer-family dose, independently equip or carry holy wrench 6714, Prayer cape 9759/9760, Max
    cape 13280/13342 and each live imbued Ring of the gods form 13202/25252/26764 on different
    recipients. Confirm each player's own modifier is used and that duplicate qualifying items do
    not stack. Test ordinary poison, every venom stage and disease values before and after Sanfew;
    time the exact antipoison and disease-immunity windows and all refresh/expiry boundaries.

    For Stamina, prove exact 200-cycle duration, varbits 24/25, a refreshed dose at every boundary,
    70-percent run-drain reduction and rounding across weights and Agility levels. Repeat with Ring
    of Endurance charges, logout/login, world hop, reconstruction and death to establish extension,
    persistence and cleanup. Exercise free/member worlds, Lunar Diplomacy incomplete/complete,
    visible and boosted Magic 80/81, each missing rune, rune pouch, substitutions and Fountain of
    Rune. Repeat with normal, Ironman, Hardcore, Ultimate and same/different Group Ironman players
    to pin the documented same-group exception.

    Direct-drink every dose of Restore 2430/127/129/131, Energy 3008/3010/3012/3014, Prayer
    2434/139/141/143, Super energy 3016/3018/3020/3022, Super restore 3024/3026/3028/3030 and
    Stamina 12625/12627/12629/12631. Automated coverage pins exact cache descriptions and `Drink`
    dispatch, full-inventory same-slot transformations, vial smashing, sequence 829, the shared
    independent three-cycle potion clock, no attack delay, Energy-only free-world access and every
    immediate formula above. Confirm the directly observed first lines for Restore, Energy, Prayer
    and Super restore; capture Super energy and Stamina rather than relying on the stable template.
    Record sequence-frame sound and exact inventory/message/stat/run-energy/varbit packet order.
    Repeat Prayer and Super restore with every qualifying holy item alone and in combination. For
    direct Stamina, test reset and natural expiry at every cycle boundary, Ring of Endurance charge
    extension, logout/login, world hop, reconstruction and every death type.

    Finally move, teleport, change plane, log out or die between click and resolution; spam-click
    the spell; replace or consume the selected potion; start combat, movement and every other Lunar
    support action at each tick. Do not promote either Stat Restore Pot Share group until all
    observations are reconciled and automated.
58. Using only the workspace cache-233 RuneLite client and at least six member accounts, target
    Boost Potion Share component 218:130 at every dose of Attack, Strength, Defence, Ranging,
    Magic, Super attack, Super strength, Super defence, Super combat, Bastion, Battlemage, Agility
    and Fishing potion. Confirm spell object 9126, Magic 84, twelve earth/ten water/three astral
    runes, 88 Magic XP, sequence 4413 and graphic 733. Verify every boost formula around integer,
    drained-stat, existing-higher-boost and level-255 boundaries.

    Direct-drink all four doses of each of those thirteen families. Automated coverage pins the 52
    cache identities/descriptions and `Drink` operations, exact same-slot full-inventory dose/vial
    chains, automatic vial smashing, sequence 829, the shared independent three-cycle potion clock,
    no attack delay, Strength-only free-world access and every immediate formula. Confirm ordinary
    and super Attack, Strength and Defence use their shared base-family first lines; confirm the
    directly recorded Ranging, Magic, Agility and Fishing lines; and confirm Bastion/Battlemage say
    `You drink some of your ... potion. It's a little tangy.` as one message. Capture Super combat's
    exact first line rather than relying on the stable template. For every family, capture the
    sequence-frame sound and inventory/message/stat packet order with vial smashing both disabled
    and enabled. Time ordinary one-point stat decay and Preserve from every integer boundary,
    including drained stats and a pre-existing equal or higher boost.

    Direct-drink Combat potion objects 9739/9741/9743/9745 and Hunter potion objects
    9998/10000/10002/10004. Automated coverage pins both members-only families, their exact cache
    descriptions and `Drink` operations, same-slot full-inventory dose/vial chains, automatic vial
    smashing, sequence 829, the shared independent three-cycle potion clock, no attack delay and
    exact Attack-plus-Strength 10-percent-plus-3 and Hunter fixed-three effects. Capture both exact
    first lines instead of relying on the stable family-name templates. For every dose, capture the
    sequence-frame sound and inventory/message/stat packet order with vial smashing disabled and
    enabled. Verify both remain rejected by Boost Potion Share. Time their one-point stat decay and
    Preserve behavior from every integer boundary, drained stat and pre-existing boost state.

    Repeat the full spatial, plane, Accept Aid, one-through-four-dose and more-than-four-recipient
    matrix from case 57. Reorder candidates to establish priority and capture zero-recipient
    behavior. Confirm the caster effect is free, final-vial and full-inventory transformations are
    exact, and each recipient receives the selected family independently. Repeat with normal,
    Ironman, Hardcore, Ultimate and same/different Group Ironman accounts.

    Attempt every incompatible family named by revision 14766606, every barbarian mix, noted and
    placeholder form, and every accessible cache-233 potion not in that table. Positively determine
    the complete allowed set. For allowed antipoison, antidote, anti-venom, goading,
    Sanfew or other non-stat families, capture exact immediate effects, durations, immunity/
    refresh/extension behavior, vars, area restrictions and equipment modifiers before adding
    production behavior.

    Automated coverage now pins all eight antipoison/anti-venom/Sanfew families, their 32 dose
    objects, cure distinctions, varp-102 interval states, exact recurring poison/venom damage and
    hitsplats, modal-interface deferral, natural poison expiry and fatal-hit death queuing.
    Manually confirm matching-client global phase and immediate post-interface hit timing,
    login/world-hop/reconstruction persistence, every source-specific NPC/weapon/spell infliction
    and reapplication rule, poison/disease immunity rejection and Sanfew disease cleanup on every
    death type. Automated antifire coverage now includes all four families, all 16 dose objects,
    cache duration varbits, exact warnings/expiry and the standard chromatic/metallic protection
    matrix. Manually verify matching-client refresh, logout/world-hop/reconstruction and death
    behavior, source NPC attack selection, shield charging/messages and every boss/special breath.
    Automated Goading coverage now pins objects 30137/30140/30143/30146, the 600-cycle effect,
    varbit 11294's six-cycle display units, refresh/expiry and resident logout pause. It also covers
    the exact 9-by-9 boundary, visible Slayer-level gate, occupied-monster protection, ordinary
    NPC-combat dispatch, five-cycle single-way replacement and the cache-typed Jal-Nib/Yt-HurKot
    exclusions and semantic cancellation from every implemented Standard, Lunar, group, Teleother,
    house, bounty-target and Home spell teleport. Automated coverage also drinks ordinary Cup of
    tea 1978, replaces it with empty cup 1980 and asserts the exact early-expiry message
    `The effects of your Goading potion have worn off.`; generic protected relocation remains
    active and silent. Manually verify immediate multiway fan-out and the authoritative single-way
    phase;
    automated direct-drinking coverage also pins the 4/3/2/1-dose chain, sequence 829, independent
    three-cycle potion delay, no attack delay, vial 229 output, auto-smash varbit 5668 and effect
    reset. Capture the matching client's exact Goading first-line drink text, singular/plural dose
    messages, sequence frame sound and inventory/message/varbit packet ordering with vial smashing
    both disabled and enabled;
    automated ordinary-Antipoison coverage now pins all four doses, free-to-play use, exact
    directly captured messages, the shared three-cycle potion clock, full-inventory replacement,
    vial smashing, ordinary-poison cure and two-dose venom downgrade/cure. Capture sequence 829's
    frame sound and the exact inventory/message/varp packet order for clean, poisoned and every
    venom-damage state, with vial smashing disabled and enabled;
    automated higher-remedy coverage now pins all four doses of Superantipoison, Antidote+,
    Antidote++, Anti-venom, Anti-venom+ and Extended anti-venom+, including members rejection,
    cache examine descriptions, full-inventory replacement, vial smashing, the shared potion
    clock and exact -20/-30/-40/-41/-50/-59 lifecycle states. On a matching cache-233 client,
    capture the first-line text for the first five families rather than relying on cache-stem
    composition; confirm the documented Extended anti-venom+ line exactly as `You drink some of
    your extended super antivenom+ potion` with no full stop. For every dose and clean, poisoned
    and venom state, capture sequence 829's frame sound and exact inventory/message/varp packet
    order with smashing disabled and enabled;
    automated direct-Sanfew coverage now pins all four cache doses, members rejection, exact cache
    descriptions, full-inventory replacement, vial smashing, shared potion timing, stat and Prayer
    restoration, Hitpoints exclusion, two-dose venom downgrade/cure, -20 poison protection,
    disease cure, 1,500-cycle disease immunity and its removal by the standard death sequence.
    Capture the exact item-specific first line instead of relying on `You drink some of your
    Sanfew serum.`; capture sequence 829's frame sound and inventory/message/stat/varp packet order
    for every dose and effect state. Verify every nonstandard death, reconstruction, logout and
    world-hop route before extending the standard-death policy;
    automated direct-antifire coverage now pins all four doses of Antifire, Extended antifire,
    Super antifire and Extended super antifire, including members rejection, Dragon Slayer I state
    0 rejection/state 1 acceptance, exact cache descriptions, full-inventory replacement, vial
    smashing, shared potion timing and exact 600/1,200/300/600-cycle ordinary/superior protection.
    On a matching cache-233 client, verify the exact state-0 rejection text and capture the first
    lines for the extended and super families rather than relying on the family-name template;
    confirm ordinary antifire says `You drink some of your dragon potion.`. For every dose and both
    prior protection tiers, capture sequence 829's frame sound and the exact inventory/message/
    varbit packet order with smashing disabled and enabled;
    every attackable/non-aggressive NPC family and visible morph; NPCs already attacking every
    player/account mode; boosted/drained Slayer boundaries; line-of-sight, collision, instancing,
    logout/world hop/reconstruction and death. Use every remaining non-spell teleport source and
    prove the complete immunity list. With ordinary tea, capture matching-client sequence/frame
    sound, forced-chat/message and varbit ordering on both early cancellation and no-active-effect
    consumption. Test tea flask doses, nettle tea and every POH Construction-tea identity
    separately; do not infer that the Wiki's ordinary `cup of tea` cancellation applies to those
    distinct items. Confirm whether natural six-minute expiry uses the same message.
    The exhaustive remaining-candidate portion of this case is still open.

    Test every tile and plane of the ordinary and instanced Castle Wars lobby, arena, spawn rooms,
    tunnels and spectator spaces, recording where `You don't want to help your enemies` applies.
    Exercise all access/rune/substitution/Fountain boundaries, then movement, combat, potion
    replacement, spam input, logout and death at every cast tick. Capture the exact cooldown,
    graphic height/delay, sound, messages and full inventory/rune/XP/stat packet order. Do not
    promote either Boost Potion Share group until every observation is reconciled and automated.
59. Using only the workspace cache-233 RuneLite client, direct-drink Prayer regeneration potion
    objects 30125/30128/30131/30134 on a members world and repeat on a free world. With vial
    smashing disabled and enabled, capture the exact first-line text, singular/plural/final-dose
    lines, sequence 829 frame sound and the complete inventory/message/stat/varbit/varc packet
    order. Confirm the 4→3→2→1→vial chain in a full inventory and that the ordinary three-cycle
    potion clock does not change an existing attack delay.

    Start at zero of 99 Prayer and record every change to Prayer, varbit 11361 and varc 1200 from
    the drinking cycle through expiry. Confirm there is no immediate grant, each grant occurs
    exactly 12 cycles apart, the total is exactly 66, and RuneLite treats the remaining varbit as
    12-cycle units. Repeat at base Prayer, above-base Prayer and with Holy wrench, Prayer cape,
    Max cape and every imbued Ring of the gods form alone and together; no item may increase a
    one-point grant, and capped ticks must still consume an interval.

    Drink a second dose on every phase boundary from 0 through 11 cycles before the next grant and
    determine whether remaining grants and phase reset, extend or preserve. Repeat across ordinary
    logout/login, world hop, forced disconnect, cold account reconstruction and every safe or
    dangerous death. Enter every minigame that resets player stats and verify the 11 June 2025
    cancellation change. Identify a reusable server-side stat-reset event before automating those
    minigame paths. Finally target every dose with Stat Restore Pot Share and prove the action is an
    atomic no-op. Do not promote the core beyond `integration-tested` until all results and exact
    client presentation are reconciled.
60. Using only the workspace cache-233 RuneLite client, direct-drink Saradomin brew objects
    6685/6687/6689/6691, Zamorak brew objects 2450/189/191/193 and Ancient brew objects
    26340/26342/26344/26346, plus Forgotten brew objects 27629/27632/27635/27638. Repeat on members
    and free worlds, with a full inventory and automatic vial smashing disabled and enabled.
    Confirm each exact 4→3→2→1→vial transition, sequence 829, the shared three-cycle potion lock,
    unchanged attack delay, singular/plural/final-dose lines and both Lunar potion-share
    rejections. Capture the sequence-frame sound and exact inventory/message/stat packet order.

    For Saradomin brew, repeat at base 99 and varied current values and prove Hitpoints restores
    `floor(base * 15 / 100) + 2`, Defence boosts `floor(base * 20 / 100) + 2`, and Attack,
    Strength, Ranged and Magic each lose `floor(current * 10 / 100) + 2` per dose. Verify the
    directly recorded first line `You drink some of the foul liquid.` and overboost caps after
    repeated doses.

    For Zamorak brew, prove Attack gains `floor(base * 20 / 100) + 2`, Strength gains
    `floor(base * 12 / 100) + 2`, Defence loses `floor(current * 10 / 100) + 2`, Hitpoints loses
    `floor(current * 12 / 100)` without falling below one and Prayer restores
    `floor(current * 10 / 100)` without overboosting. Capture the exact first line to replace the
    historical-community-only foul-liquid observation.

    For Ancient brew, prove Magic gains `floor(base * 5 / 100) + 2`, Attack/Strength/Defence each
    lose `floor(current * 10 / 100) + 2`, and Prayer gains `floor(base * 10 / 100) + 2` up to
    `base + floor(base * 5 / 100)`. Capture its exact first line rather than relying on the stable
    family-name template.

    For Forgotten brew, prove Magic gains `floor(base * 8 / 100) + 3`, Attack/Strength/Defence
    each lose `floor(current * 10 / 100) + 2`, and Prayer gains `floor(base * 10 / 100) + 2` up to
    `base + floor(base * 5 / 100)`. Capture its exact first line rather than relying on `You drink
    some of your forgotten brew.` as a standard-potion-template inference. For all four families,
    time ordinary one-point restoration/drain and Preserve/Rapid Restore behavior from every phase
    boundary and test pre-existing stronger boosts, deeper drains, level-one floors, death, logout
    and world hop.

    Separately combine ancient essence object 27616 with matching Ancient brew doses
    26346/26344/26342/26340. Confirm one-, two-, three- and four-dose conversions require exactly
    20/40/60/80 essence, produce Forgotten brew 27638/27635/27632/27629 after three ticks and award
    exactly 36/72/108/145 Herblore XP. Test base and boosted levels 90/91, both held-use orders,
    insufficient and exact essence quantities, mismatched dose handling, a full inventory,
    resource loss during the delay, repeated interaction and queue interruption. Capture which
    item must be used on which, exact messages, animation, sounds, inventory/XP packet ordering and
    whether the action offers or automatically repeats. Keep this upgrade feature separate from
    direct consumption until those presentation details are pinned.

    Separately capture Menaphite remedy objects 27202/27205/27208/27211 from consumption through
    five-minute expiry and compare the implemented first pulse after 25 cycles, every later
    25-cycle phase, 20-pulse countdown in varbit 14448 and redose reset. Confirm the affected set is
    exactly Attack, Strength, Defence, Ranged and Magic; lowered stats receive
    `6 + floor(base * 16 / 100)`, while Hitpoints, Prayer and boosted stats remain unchanged. Start
    each of the eight divine-potion families and a saturated-heart effect before the remedy, prove
    their timer state is removed without immediately changing the visible boost, then apply each
    effect after the remedy and prove it remains active. Capture the exact first drink line,
    animation/graphic/sound and inventory/message/stat/var packet order. Establish cold-login
    sub-interval state, ordinary and nonstandard death behavior, logout and world-hop persistence;
    specifically reconcile the implemented resident-session phase preservation, full-interval
    cold reconstruction and standard-death clear before promoting those boundaries.
61. Using only the workspace cache-233 RuneLite client, direct-drink Surge potion objects
    30875/30878/30881/30884 on a members world and repeat on a free world. With a full inventory
    and automatic vial smashing disabled and enabled, confirm the exact 4→3→2→1→vial transitions,
    sequence 829, independent three-cycle potion lock, unchanged attack delay, singular/plural/
    final-dose lines, first-line wording, frame sound and complete inventory/message/var/stat packet
    order. Spam-click before the ordinary potion lock expires and prove only one dose is consumed.
    Target every dose with both Lunar potion-share spells and confirm an atomic no-op.

    Outside PvP, begin at 0, 749, 750, 999 and 1,000 special-attack units. Prove the successful
    drink adds exactly 250 units and caps at 1,000, graphic 3232 uses sequence 12094, and full
    special produces exactly `Drinking this would have no effect.` without consuming a dose or
    starting either timer. Capture varbit 16270 and varclient 1258 from consumption through natural
    expiry. Prove the five-minute cooldown is exactly 500 game cycles, the client value uses ten-
    cycle units, and the natural-expiry line and colour match the cutoff source. Attempt a second
    dose at every ten-cycle display boundary and capture the precise remaining-time substitution,
    singular/plural grammar and partial-second rounding. Log out at every boundary and verify that
    elapsed offline time does not reduce the remaining cooldown.

    In the Wilderness, Bounty Hunter and every other PvP-enabled area, confirm there is no immediate
    restoration and the second graphic/restoration occurs exactly six ticks later. During the
    delay, move by every transport type and test Chebyshev offsets 15 and 16 in both axes and
    diagonally; 15 must restore and 16 must discard. Repeat at the special cap, on death, logout,
    world hop and forced disconnect during every pending tick, and capture the presently unstated
    logout-pending policy. Verify production continuously updates `inside_wilderness` varbit 5963
    and `pvp_area_client` varbit 8121 rather than relying only on a PvP world flag.

    Exercise every documented lifecycle boundary: pause between Colosseum and Inferno waves,
    Theatre of Blood rooms and Doom levels; reset on safe and dangerous death, raid/Colosseum entry
    and exit, the Ferox free-for-all portal and every POH pool. Prove the timer does not pause between
    Fight Caves waves or Tombs of Amascut rooms, and does not reset on Theatre/Tombs death or at the
    Elidinis Statuette. Record exact ordering against pending PvP restoration and ordinary potion
    delay. Do not promote the world/activity feature or direct core to verified until every result
    is reconciled and automated.
62. Using only the workspace cache-233 RuneLite client, complete Otto's Barbarian Herblore
    training and capture every journal/dialogue transition. Determine whether production still
    updates `brut_herb_potion` varbit 3761 through 0/1/2/3 despite the cutoff Quest Helper moving
    its own progress tracking to local configuration. Make the first Attack mix at state 1 and
    confirm the exact ordering and channels of `You combine your potion with the fish eggs.` and
    `You feel you have learned more of barbarian ways. Otto might wish to talk to you more.` Test
    whether another Attack mix is permitted at state 2 and whether the training message interrupts
    or cancels a selected Make-All batch.

    At each exact boosted level and one below it, make all 29 mixes from their two-dose source
    potions. Prove roe and caviar both work for Attack, Antipoison, Relicym's balm, Strength and
    Restore, while roe atomically fails for the other 24 and caviar succeeds. Verify native
    Make-All, both held-use orders, exactly one result per game tick, exact XP, sequence 363,
    animation restart, sound, full-inventory handling, resource removal during the queue and all
    interface/message/inventory/XP packet ordering. Repeat with the chemistry amulet, alchemist's
    amulet and both prescription-goggle forms to determine whether any production modifier applies.

    Separately observe both doses of every mix. Automated coverage now pins each 2→1→vial
    transition, automatic vial smashing, members gating, the shared three-cycle food clock,
    conditional three-cycle attack-delay extension, every inherited immediate effect and every
    three- or six-Hitpoints heal. Capture the exact punctuation/channel of the lumpy first line and
    the first/final message order, sound, animation frame and inventory/stat packet order. Reconcile
    Zamorak mix at full and damaged Hitpoints against the cache-matched RuneLite independent-effect
    cases, and confirm Relicym's mix fully clears rather than merely weakens disease. Exercise
    logout, death and world-hop protection persistence plus both Lunar potion-share spells. Keep
    production and consumption as separate manifest features until these observations are
    reconciled; do not regress the automated core.
63. Using only the workspace cache-233 RuneLite client, make Guam tar 10142 from both clean guam
    249 and ground guam 6681, Marrentill tar 10143 from marrentill 251, Tarromin tar 10144 from
    tarromin 253 and Harralander tar 10145 from harralander 255. For every route, begin with pestle
    and mortar 233 and exact quantities around the 15-swamp-tar boundary: 14, 15, 29, 30 and 45 of
    object 1939. Confirm one herb and 15 swamp tar become exactly 15 output after three game ticks,
    with boosted levels 19/31/39/44 and exact XP 30/42.5/55/72.5. Repeat at one level below, on a
    free world, before Druidic Ritual completion, without the pestle, with a full inventory and
    after removing the herb, tar or pestle on each pending tick. Every rejection or interruption
    must be atomic.

    Compare exactly one available herb against two or more. Confirm whether one starts immediately
    with no quantity interface and multiple herbs use interface 270/clientscript 2046; capture its
    title, action mode, initial/max values and whether tar supply truncates the maximum. Test both
    item-use orders and distinguish the two Guam inputs despite their common output. During Make-X,
    pin results at ticks 3/6/9, animation 5249 start/restart behavior, interruption by movement and
    another Herblore action, success/failure text and channel, sound, output-slot selection, and
    exact inventory/XP/interface packet ordering. Confirm no Irit-tar option exists in the
    cache-233 production client; treat any later-cache Irit recipe as a future revision delta.
64. Using only the workspace cache-233 RuneLite client, use hammer 2347 with coconut 5974 in both
    orders and capture whether exactly one selected coconut becomes half 5976 immediately or
    whether additional coconuts continue automatically. Pin the message, animation, sound,
    selected/output slot behavior, full-inventory packets and interruption rules. Then use coconut
    half 5976 with empty vial 229 in both orders. Confirm immediate milk 5935 plus shell 5978,
    identify which result occupies each source slot, and pin later results at three and then every
    four ticks. Remove either material at every boundary and capture all presentation and packet
    order.

    At boosted Herblore levels 73 and 82, make unfinished Weapon poison(+) 5936 from coconut milk
    5935 plus cactus spine 6016 and unfinished Weapon poison(++) 5939 separately from Cave
    nightshade 2398 and stackable Nightshade 27790. Confirm two ticks, zero XP, both held-use
    orders, native one-product quantity presentation, full-inventory behavior and atomic failure
    one level below, before Druidic Ritual, on free worlds and after material loss. Finish 5936
    with red spiders' eggs 223 into 5937 and 5939 with poison ivy berries 6018 into 5940; confirm
    two ticks and exactly 190 XP for each, resolving the historical 165-XP change note against the
    active cutoff recipe. Force prescription-goggle rolls on all four preservable inputs and prove
    cactus spine is not preserved. Equip each extra-dose amulet and confirm no extra output or
    charge use. Capture exact messages, sounds, animation start/restart, output slots and complete
    interface/inventory/XP packet ordering.
65. Using only the workspace cache-233 RuneLite client, make Antidote+ (unf) 5942 from coconut milk
    5935 plus toadflax 2998 at boosted Herblore level 68, and Antidote++ (unf) 5951 from coconut milk
    plus irit 259 at level 79. Confirm the deliberately different two-tick and one-tick completion,
    zero XP, both held-use orders, native one-product quantity presentation, full-inventory safety
    and atomic rejection one level below, before Druidic Ritual, on free worlds and after either
    material disappears. Force both prescription-goggle appearances to preserve the herb, capture
    exact success/failure messages, sound, animation start/restart and output-slot/packet order.

    Finish Antidote+ (unf) with yew roots 6049 into Antidote+(4) 5943 and Antidote++ (unf) with
    magic roots 6051 into Antidote++(4) 5952. Confirm two ticks, exactly 155/177.5 XP, four-dose
    outputs, both held-use orders and all access/resource/full-inventory boundaries above. Force
    prescription-goggle preservation for each root. Equip charged chemistry and alchemist's
    amulets separately and prove neither attempts an extra-dose roll nor consumes a charge. Capture
    the exact menu, messages, animation, sound, result slot and interface/inventory/XP packet order.
66. Using only the workspace cache-233 RuneLite client, make Anti-venom from every Antidote++ dose:
    5958/5956/5954/5952 plus exactly 5/10/15/20 Zulrah's scales 12934 into
    12911/12909/12907/12905. Confirm boosted level 87, two ticks and exactly 30/60/90/120 XP. Then
    make Anti-venom+(4) 12913 from Anti-venom(4) plus clean torstol 269 and separately torstol
    potion (unf) 111 at boosted level 94; confirm two ticks and 125 XP for both routes. Finally make
    Extended anti-venom+ from doses 12919/12917/12915/12913 plus exactly 1/2/3/4 Araxyte venom
    sacks 29784 into 29833/29830/29827/29824; confirm three ticks and 20/40/60/80 XP.

    For every family, confirm both held-use orders, native quantity maximum by complete ingredient
    bundles, full-inventory safety, batching and atomic rejection one level below, before Druidic
    Ritual, on free worlds and after either material disappears. Force both prescription-goggle
    appearances and prove they preserve the full 5/10/15/20-scale or 1/2/3/4-sack bundle, preserve
    torstol and torstol potion (unf), consume no extra ingredient for an amulet-added dose and emit
    no goggle message. Force chemistry and alchemist's-amulet successes on one-, two- and three-dose
    inputs, confirming the matching two-/three-/four-dose output, exact dynamic chemistry wording,
    one charge and no extra XP; prove four-dose inputs use no amulet charge. Capture exact ordinary
    messages, sounds, animation restart, result slot and complete interface/inventory/XP/equipment
    packet order. Make Anti-venom in Horseshoe Mine and during the relevant Sherlock master-clue
    step to pin the Karamja elite and clue-completion hooks.
67. Using only the workspace cache-233 RuneLite client, make Stamina potion from every Super
    energy dose: 3022/3020/3018/3016 plus exactly 1/2/3/4 amylase crystals 12640 into
    12631/12629/12627/12625. Confirm boosted Herblore level 77, exactly two ticks and exactly
    25.5/51/76.5/102 XP. Test both held-use orders, native quantity maximums at every incomplete
    crystal boundary, a full inventory, batching, one-level-below/members/Druidic rejection and
    removal of either input on every pending tick. Each rejection or interruption must be atomic.

    Start every one-input and Make-X form while walking and while running. Confirm the 8 January
    2025 change end to end: opening or starting Stamina Make-X must not cancel the existing route;
    capture whether reaching the destination or issuing a later movement request interrupts the
    production queue. Pin interface 270/clientscript 2046 arguments, exact success and failure
    text, sequence 363 start/restart, sounds, output-slot choice and complete interface/inventory/
    XP packet order.

    Force both prescription-goggle appearances for each dose size and prove one successful roll
    preserves the complete 1/2/3/4-crystal bundle, with no preservation message. Force chemistry
    and alchemist's-amulet successes for one-, two- and three-dose inputs and confirm the matching
    two-/three-/four-dose output, one success-only charge, no extra crystal and no extra XP. Prove
    a four-dose input never rolls or consumes an amulet charge. Capture the exact equipment sound,
    messages, graphic/animation interaction and packet ordering before promoting client parity.
68. Using only the workspace cache-233 RuneLite client, make Extended antifire from Antifire
    doses 2458/2456/2454/2452 plus exactly 1/2/3/4 lava scale shards 11994 into
    11957/11955/11953/11951. Confirm boosted Herblore level 84, two ticks and exact
    27.5/55/82.5/110 XP without requiring the superior-antifire unlock. Make Super antifire(4)
    21978 from Antifire(4) 2452 plus crushed superior dragon bones 21975 at boosted level 92 for
    130 XP. Then make Extended super antifire from Super antifire doses
    21987/21984/21981/21978 plus 1/2/3/4 lava shards into 22218/22215/22212/22209 at level 98 for
    40/80/120/160 XP, and separately make 22209 from Extended antifire(4) 11951 plus crushed bones
    for 180 XP. Confirm every result occurs after exactly two ticks.

    Before and after speaking to Primula, capture her complete dialogue and cache varbit 6140.
    Prove all Super and Extended-super routes fail atomically while locked and succeed after the
    durable unlock, while ordinary Extended-antifire production is unaffected. Repeat at one level
    below, boosted to the requirement, before Druidic Ritual, on a free world, with incomplete
    lava-shard bundles, after either input disappears, and in a full inventory. Pin both held-use
    orders, native quantity maxima, one-input behavior, queue interruption, exact feedback,
    sequence 363 start/restart, sounds, output slots and complete interface/inventory/varbit/XP
    packet order.

    Start every route while walking and running and confirm the January 2025 moving-Make-X behavior
    end to end. Force both prescription-goggle forms on all ten recipes, proving one roll preserves
    all 1/2/3/4 lava shards or the single non-stackable crushed-bone item without text. Force both
    amulet types on every one-/two-/three-dose variable route, proving the next-dose output, one
    charge, no extra secondary and no extra XP. Prove all four-dose and crushed-bone routes never
    roll or consume a charge. Capture equipment audiovisual and packet ordering before promotion.
69. Using only the workspace cache-233 RuneLite client, visit every ordinary small-net/bait spot
    represented by NPCs 1514, 1517, 1518, 1521, 1523, 1524, 1525, 1528, 1530, 1544, 3913, 7155,
    7459, 7462, 7467, 7469, 7947, 14038, 14040, 14041 and 14524. Confirm the first option label is
    `Small Net` except `Net` on 1530, that `Bait` occupies the third slot, and that small-net and
    rod animations are sequences 621 and 622. Record the exact animation restart/continuity,
    sounds and complete interaction/inventory/message/stat packet order.

    With deterministic level and roll boundaries, prove ordinary rolls occur every five ticks,
    anchovies roll before shrimp at level 15 with endpoints 24/128 then 48/256, and herring rolls
    before sardine at level 10 with endpoints 24/128 then 32/192. Confirm exact 40/10/30/20 XP and
    the four `raw` catch messages. Exercise an ordinary rod, carried pearl rod and worn pearl rod;
    prove one bait is consumed only on success. Repeat with no tool, no bait, one level below,
    boosts crossing each requirement, a full inventory with one bait, a full inventory with two
    bait and resources removed on the result tick. Capture every exact rejection sentence.

    Map each spot's relocation partner and prove its independent random 250–530-tick lifetime,
    including logout, world-hop and multiple-player observation. Separate sea and river locations
    and prove the location-specific herring availability rather than assuming one catch table for
    every identity. Interrupt with walking, running, teleport, combat, a second skilling action,
    logout and death on every tick boundary. Finally perform each known small-net/bait tick-
    manipulation setup and pin its client/server cadence before promoting the lifecycle row.
70. Using only the workspace cache-233 RuneLite client, visit every lure/bait spot represented by
    NPCs 394, 1506, 1507, 1508, 1509, 1512, 1513, 1515, 1516, 1526, 1527, 1529, 3417, 3418,
    7463, 7464, 7468, 8524, 12774, 14036, 14521, 14522, 14525, 14526, 14527 and 14528. Confirm
    `Lure` occupies the first option, the second is empty, `Bait` occupies the third, the last two
    are hidden, and every examine is `I can see fish swimming in the water.` Map every relocation
    partner and prove each independent random 250–530-tick lifetime and its logout/world-hop
    behavior.

    With deterministic boundary observations, prove salmon rolls before trout at boosted level 30
    with endpoints 16/96 then 32/192, exactly 70/50 XP, five-tick attempts and the no-period raw
    catch messages. Exercise ordinary and carried/worn pearl fly rods, proving sequences 622 and
    8189 and one regular feather consumed only on success. With only stripy feathers, prove
    members-only level-38 rainbow fish at endpoints 8/64 and 80 XP. Carry both feather types and
    pin which table and bait take priority at levels 20, 30 and 38. Capture the exact rainbow
    success and missing-bait/member rejection wording rather than relying on bounded strings.

    Through the third option, prove level-25 pike at endpoints 16/96 and 60 XP, with four out of
    five attempts resolving after five ticks and one out of five after six ticks. Exercise ordinary
    and carried/worn pearl rods, proving sequences 622 and 8188, success-only fishing-bait use and
    the exact pike success wording. Repeat every method with no tool, no bait, one level below,
    boosted across each threshold, a full inventory with one bait, a full inventory with multiple
    bait and resources removed on the result tick. Capture animation continuity, sounds and full
    interaction/inventory/message/stat packet order, then interrupt on each tick with movement,
    teleport, combat, replacement skilling, logout and death before promoting client parity.
71. Using only the workspace cache-233 RuneLite client, visit cage/harpoon NPCs 1510, 1519, 1522,
    2146, 3657, 3914, 7199, 7460, 7465, 7470, 7946, 9173, 9174, 10515, 12777 and 14039. Confirm
    `Cage` occupies the first option, the second is empty, `Harpoon` occupies the third and the
    examine is `I can see fish swimming in the water.` Separately confirm that 5821 and 10514 use
    `Net/Harpoon`, 12776 uses `Big Net/Harpoon`, and none of those three starts lobster fishing
    through option one. Map every relocation partner and prove each independent random
    250–530-tick lifetime, including logout, world-hop and multi-player observation.

    With deterministic boundary observations, prove the lobster-pot route requires boosted level
    40, rolls at endpoints 6/95, awards exactly 90 Fishing XP, uses sequence 619 and attempts every
    five ticks. Confirm the exact success, low-level, missing-tool and full-inventory sentences and
    that neither the pot nor any bait is consumed. Repeat with one level below, a boost crossing
    level 40, a full inventory, the pot removed on the result tick and movement beyond adjacency.
    Capture animation continuity, sound identity and complete interaction/inventory/message/stat
    packet order, then interrupt on every tick with movement, teleport, combat, replacement
    skilling, logout and death.

    Through the third option, confirm all 18 cutoff identities above dispatch harpooning and that
    10514 remains excluded from this tuna/swordfish family. Reconfirm regular and carried/worn
    barb-tail harpoons, five-tick rolls, swordfish-before-tuna selection, boosted levels 50/35,
    endpoints 4/48 and 8/64, exactly 100/80 XP and both ordinary success lines. On a members world,
    prove the 1/2500 successful-swordfish replacement with `You catch an enormous swordfish!`; on
    a free world, prove the replacement cannot occur and a barb-tail harpoon cannot be selected.

    Automated coverage pins Fishing Guild cage/harpoon NPC 1510's invisible +7 chance effect after
    visible-level gates, including a boundary success that would fail without the bonus. Separately
    Automated coverage pins infernal 1/3 auto-cooking, half Cooking XP, 5,000-use persisted state,
    charged-to-empty style transitions and big-fish exclusion. Base held-use creation now pins
    dragon harpoon 21028 plus smouldering stone 13233 in either order, unboosted 75 Fishing/85
    Cooking, 200/350 XP and full-inventory conversion to 21031. Pin exact creation/passive messages,
    audiovisuals and packets. Automated coverage pins both depleted-tool
    recharge materials for the base, Trailblazer and Reloaded styles, either held-use order, no
    skill requirement or XP, full 5,000-charge state and full-inventory atomicity; confirm exact
    recharge messages and packet presentation in the matching client. Automated coverage also pins
    Trailblazer kit 25090 application to charged/depleted base harpoons, either order, full-inventory
    atomicity, no level/XP gate and partial-charge preservation. Automated coverage now also pins
    cache-slot Revert for both ornament styles, component return and full-inventory rejection, plus
    two-way Change-Style from inventory and while worn without losing packed state. Confirm whether
    Revert prompts for confirmation and capture exact messages, audiovisuals and packet order.
    Automated coverage pins inventory and worn Check for all three charged styles at canonical
    5,000 and partial charge states, plus first-option Wield for all four ornamented charged/depleted
    identities with state preservation. Capture the exact Check sentence at 5,000, one and an
    intermediate count, the free-world rejection text, Wield feedback and precise packet order.
    Automated coverage pins crystal 10,000-initial/20,000-maximum persisted charge state, successful-
    catch-only use, big-fish use and final transition to inactive 23764. Automated coverage pins
    active inventory/worn Check at canonical and partial counts, plus active/inactive fourth-option
    Revert returning dragon harpoon 21028 and crystal tool seed 23953 atomically. Automated shard-
    use coverage pins both item orders, quantity input, 100 charges per shard, the
    20,000 ceiling, partial-final-shard clamping, inactive-to-active conversion, full-inventory use
    and stale-input safety. Bank preference automation now pins the shared param-2257/DB-row-4338
    route, permanent varbit 16148, members gate and zero-to-20,000 bound. Separately pin the literal
    quantity and Configure-Charges prompts and feedback, automatic shard deduction and item-local
    charging on withdrawal, exact Check/Revert wording and confirmation, Song of the Elves access and dangerous-
    PvP death. Automated coverage pins 10% charge saving from elven and both celestial signets, 5%
    from Ghommal's lucky penny for crystal and infernal harpoons, and independent per-fish rolls
    when signet and penny are combined. Capture whether either conservation effect emits feedback
    and its packet ordering; separately prove long-run probability boundaries. Automated coverage
    now pins the members-only Otto state, regular-harpoon priority, boosted 55/70 Fishing and base
    35/50 Strength gates, regular-harpoon catch rate, 8/10 Strength XP, cache loop sequence 6709 and
    the first-catch state/feedback transition for barehanded tuna and swordfish. Capture the exact
    low-level/training rejection text and the 6703/6704 start/ready plus 6707/6708 and 6710/6711
    result choreography. Automated coverage pins equipped Rada's blessing 1–4 at
    2/4/6/8 percent for ordinary single-output methods, no extra XP, big-swordfish duplication and
    safe one-slot fallback. Automated coverage also pins carried spirit flakes at 50 percent,
    consumption on successful and failed bonus rolls, unchanged ordinary bait use, additive Rada
    stacking, big-swordfish duplication and the one-slot no-consume rule. Automated charged-tool
    coverage pins an independent infernal cook roll for each multiplied fish, half Cooking XP and
    cook-only charges, plus one crystal charge per multiplied fish. Separately pin simultaneous
    doubled-output behavior with only one infernal or crystal charge remaining; conditional clue-
    bottle and heron rolls; exact
    rejection/presentation details, sounds and packet order. Keep those observations outside the
    production claim until each has its own focused implementation and affected/full gates.
72. Using only the workspace cache-233 RuneLite client, visit big-net/harpoon NPCs 1511, 1520,
    3419, 3915, 4476, 4477, 5233, 5234, 5820, 7200, 7461, 7466, 8525, 8526, 8527, 9171, 9172,
    12775, 14037 and 14523. Confirm operation three is `Harpoon`, the shared examine text and exact
    first-operation label on every identity; specifically reproduce cache 233.4's `Cage` first
    operation on source-family member 5820 while the remaining identities expose `Big Net` or
    `Net`. Map every relocation partner and prove the 250–530-tick lifetime, static/reduced-movement
    exceptions, logout, world-hop and multi-player observation behavior.

    On members worlds, prove ordinary and carried/worn barb-tail harpoons catch sharks at boosted
    level 76, endpoints 3/40, exactly 110 XP and five-tick attempts. Confirm `You catch a shark!`,
    the 1/5000 post-success replacement `You catch an enormous shark!`, and that big shark keeps
    110 XP. Repeat with level 75, a boost crossing 76, no tool, a free world, full inventory, tool
    removal and movement on the result tick. Separately pin shark lure. Automated coverage now pins
    Fishing Guild big-net/harpoon NPC 1511's invisible +7 chance effect without unlocking shark or
    higher-level big-net products, every cache-233 dragon/infernal/crystal base, ornamented,
    Reloaded and depleted variant; visible use levels 61/75/71; their cache animations; strongest-
    usable selection; and exact 1/256-quantized 1.20×/1.35× success rates. In the matching client,
    confirm tool-selection presentation when several variants coexist. Automated coverage pins all
    three infernal styles' 1/3 ordinary-fish auto-cook, half Cooking XP, success-only charge use,
    5,000-use persisted state, matching depleted transition and big-fish exclusion. Separately pin
    base infernal creation, all three styles' two-material recharge and charged/depleted Trailblazer
    kit application, cache-slot Revert with component recovery and two-way inventory/worn style
    switching. Automated coverage also pins charged inventory/worn Check at canonical and partial
    state plus shifted first-option Wield for all four ornamented identities; separately pin the
    literal Check/Wield text and exact creation/recharge/passive/ornament-operation presentation.
    Automated coverage pins crystal
    default/maximum state, successful-catch-only depletion, big-fish charge use and inactive
    transition. Automated coverage now pins active inventory/worn Check and active/inactive Revert
    with component recovery and full-inventory safety. Automated shard-use coverage now pins
    quantity selection, both forms/item orders, 100 charges per shard, 20,000 maximum, partial-
    shard cap, full-inventory operation and stale-input safety. Bank preference automation pins the
    shared cache route and bounded persistent quantity; separately pin automatic withdrawal
    charging, exact Configure-Charges/charging/Check/Revert presentation and confirmation, Song of the Elves access
    and dangerous-PvP death. Automated coverage pins each elven/celestial 10% roll, the lucky
    penny's 5% crystal/infernal roll and independent per-fish stacking; capture any conservation
    feedback and packet ordering. Automated coverage now pins barehanded shark at boosted level 96
    Fishing, base level 76 Strength, regular-harpoon rate, 11 Strength XP, Otto-state gating,
    ordinary-harpoon priority and loop sequence 6709. Capture the exact rejection presentation and
    6703/6704 start/ready plus 6705/6706 shark-result choreography. Automated coverage pins Rada's ordinary
    single-output percentages, spirit-flake additive stacking/consumption and big-fish duplication,
    independent infernal cooking for multiplied fish and per-fish crystal charge use. Separately pin
    the one-charge/two-output boundary and fish-barrel
    treatment, clue bottles,
    Varlamore moon-key halves, heron rolls, sounds, packets, tick manipulation and interruption.

    Activate Fishstabber from all eleven dragon, infernal, original Trailblazer, Reloaded and
    active/inactive crystal harpoon states. Confirm 100% energy, a visible +3 Fishing boost that
    never lowers a larger boost, `Here fishy fishies!`, exact red/silver graphics, all five sequence
    families and area sound. Activate while an actual five-tick Fishing action is underway and prove
    the catch queue, selected spot/tool and next result timing are unchanged. Capture combat-tab and
    orb behavior, insufficient-energy feedback, stat restoration and Preserve timing, sound
    audience and exact packet order.

    The automated core now proves that the 19 cache identities with a compatible first operation
    dispatch big net 305 and sequence 620, while NPC 5820 does not. It covers nine independent rolls
    in exact source order—leather boots, seaweed, leather gloves, mackerel, second mackerel, oyster,
    casket, cod and bass—their gates, endpoints and XP, the charged-skills-necklace casket modifier,
    base-level bass unlock, the 1/1000 successful-bass replacement, and atomic partial-capacity
    acceptance. It also proves one additive multiplier roll per accepted primary result, including
    leather boots and selected big bass, one spirit flake consumed on a success or failure, one XP
    award per primary result, doubled output consuming capacity before later rolls, and no roll or
    consumption with only one slot left. In the matching client, confirm literal junk and multi-
    output messages, multiplier feedback/packet order, and whether consuming a final flake makes
    its former slot available during the same action. Separately exercise fish barrel and every
    conditional reward before promoting those lifecycle features.

73. Using only the workspace cache-233 RuneLite client, test Barbarian heavy-rod fishing at both
    Otto's Grotto and Mount Quidamortem spots, cache NPCs 1542 and 7323. Confirm first-option
    `Use-rod`, a carried barbarian rod 11323 and a worn pearl barbarian rod 22842. Test before Otto
    assigns rod training, at assigned state, after the first successful catch and after completion;
    also test a free world. Capture the exact rejection text and packet ordering for training,
    membership, insufficient Fishing, insufficient base Strength or Agility, missing rod, missing
    bait and full inventory.

    At the exact boundaries, verify leaping trout at 48 Fishing/15 Strength/15 Agility for
    50/5/5 XP, salmon at 58/30/30 for 70/6/6 XP and sturgeon at 70/45/45 for 80/7/7 XP. Prove that
    Fishing boosts unlock products while Strength and Agility boosts do not, and verify the ordered
    sturgeon→salmon→trout cascade at raw endpoints 8→64, 16→96 and 32→192 on the 256 grid.
    With every bait present, consume exactly one in the priority order fish offcuts 11334, fishing
    bait 313, feathers 314, roe 11324 and caviar 11326; remove or move the selected bait and rod on
    the result tick, and repeat with a full inventory where consuming the final bait creates the
    result slot. Confirm five-tick first and repeat attempts and the exact first-catch message and
    state transition.

    Capture the regular rod's 9349 cast-to-9350 loop/result choreography and the pearl rod's
    8190-to-8193 choreography, including sequence frames, sounds and packet order. Exercise spot
    relocation, tick manipulation, movement, logout, death and interruption. Separately verify
    equipped Rada's blessings, spirit flakes, fish barrel, clue bottles and heron rolls, proving
    bait use and Fishing/Strength/Agility XP are not duplicated by a bonus fish.

74. Using only the workspace cache-233 RuneLite client, use knife 946 in both item orders on
    leaping trout 11328, salmon 11330 and sturgeon 11332. At Cooking 1 and 99, pin trout's
    `level/150` boundary; at Cooking 1, 79 and 80, pin salmon and sturgeon's `level/80` boundary.
    Confirm that failed gutting destroys the fish without Cooking XP or output, while success gives
    roe 11324 for trout/salmon or caviar 11326 for sturgeon and exactly 10/10/15 Cooking XP. On
    successful cuts, separately pin the 1/2, 3/4 and 5/6 fish-offcut 11334 boundary rolls.

    Confirm that the selected fish is cut immediately, sequence 6702 is visible, and automatic
    processing continues every three ticks. Mix all three fish types to establish whether the
    queue finishes only the selected type or changes type and, if so, the exact priority. Re-click
    the knife and fish on consecutive ticks to prove the faster manual behavior and establish
    whether manual input replaces, advances or coexists with the pending automatic action. Repeat
    with boosted and drained Cooking to determine the level source.

    Capture exact success, failure, membership and capacity messages, animation duration, sounds,
    and item/XP/message packet order. Test a full inventory with neither output stack present, only
    roe/caviar present, only offcuts present and both present; determine whether an unavailable
    secondary offcut is suppressed, dropped or rejects the action. Exercise knife removal or slot
    movement, fish movement, other inventory actions, movement, combat, interfaces, logout and
    death during the automatic queue.

75. Using only the workspace cache-233 RuneLite client, test closed fish barrel 25582 and open fish
    barrel 25584 on a members world and a free world. Pin every item-option slot, exact Fill,
    no-suitable-fish, full, membership and Open/Close text, and the interface-193 Check layout for
    an empty barrel, one species and several species. Confirm ordering, capitalization, line breaks
    and whether Fill/Check interrupt Fishing. Repeat with exactly 27 and 28 stored fish and with a
    full inventory.

    Fill the barrel with each of the 33 pre-Sailing accepted identities covered by automation.
    Separately prove rejection of raw karambwanji and other stackable fish, big bass/swordfish/shark
    and other trophy fish, raw harpoonfish, Chambers of Xeric fish, Camdozaal guppy/cavefish/tetra/
    catfish, and Neypotzli fish. At a bank and deposit box, capture exact empty, success, partial-bank
    and full-bank behavior/messages; on Ultimate Ironman, capture the appropriate bank-stand use
    and withdrawal flow.

    With the barrel open, catch every implemented ordinary small-net, bait, lure, lobster, tuna/
    swordfish, shark and Barbarian-rod result. Confirm automatic storage with ordinary space, a full
    inventory, the final barrel slot, Rada/spirit-flake doubles, big-fish replacement and infernal
    auto-cooking. Capture exact catch/full-barrel message and packet order. Automation now covers
    source-ordered big-net mackerel, cod and bass routing, full-inventory junk rejection with later
    fish acceptance, final-slot Rada/spirit-flake splitting and inventory-only big bass. In the
    client, confirm multi-result ordering and whether a final consumed flake releases its slot in
    time for a same-action output. Then cover every other Fishing source before promoting its
    automatic routing.

    Automation now pins fish sack 22838 plus closed/open barrel assembly into fish sack barrels
    25585/25587 in either item order, the absence of a success message, cape-slot metadata,
    carried/worn Check and Open/Close dispatch, open worn acquisition at full inventory,
    state/content-preserving Configure/Dismantle, full-inventory rejection and Destroy clearing.
    In the client, capture the exact Configure dialogue presentation and default equipment packets,
    then test bank/deposit interaction while equipped. Finally, capture multiple-barrel ownership,
    closing/destroying one copy, logout/world hop, PvM and PvP deaths on both sides of level-20
    Wilderness, and gravestone persistence for both ordinary and fish sack barrel forms.

76. Using only the workspace cache-233 RuneLite client, travel to the Holy Lake south of Tai Bwo
    Wannai and test the three NPC-4710 `Net` spots with a small fishing net. On a free world, at
    Fishing 4, without the net and with a completely full inventory lacking raw karambwanji,
    capture the exact rejection text and whether the animation or facing changes. At Fishing 5,
    use video or tick counters to confirm the first and repeat attempt every six ticks and capture
    sequence 621, sounds, exact raw-karambwanji and raw-shrimp catch lines, XP/item/message packet
    order, movement, combat, interface, logout and death interruption.

    At visible boosted Fishing levels 5, 9, 10, 94, 95, 99 and 100+, confirm the per-success stack
    quantities 2, 2, 3, 19, 20, 20 and 21+ and exactly five Fishing XP per successful roll rather
    than per item. Record a large deterministic sample to validate the ordered 100/250 raw-
    karambwanji then 48/256 raw-shrimp cascade and the possibility of neither output. With exactly
    one free slot, then a full inventory containing an existing raw-karambwanji stack, determine
    directly whether failed primary rolls suppress the shrimp roll or roll and silently discard it;
    this resolves the conflict between Wiki revisions 14970676 and 14992999. Confirm fishing does
    not stop in either case.

    Equip each Rada's blessing tier and confirm 2/4/6/8-percent doubling of the entire quantity,
    not one extra item, with no extra XP. Repeat with carried spirit flakes alone and combined with
    Rada's blessing 4 to confirm 50% and 58% boundaries, one-flake use on success and failure, and
    last-flake/full-inventory capacity behavior. Confirm raw karambwanji never enters either fish-
    barrel form. Finally, capture all three spots' relocation destinations and lifetimes plus clue-
    bottle and heron eligibility/rates before advancing the lifecycle row beyond researched.

77. Using only the already-open workspace cache-233 RuneLite client, complete Tai Bwo Wannai Trio
    and travel to the main karambwan grounds north of the Karamja Ship Yard. Confirm NPCs 4712 and
    4713 expose first-option `Fish`, remain effectively stationary across logout/restart and share
    the observed three-destination relocation anomaly. On a free world, before quest completion,
    at Fishing 64, without a vessel, with an empty vessel and with a loaded vessel, capture the
    exact rejection order/text plus any facing, animation or sound changes. Repeat at visible 65
    from a base level below 65 to prove the requirement is boostable.

    Use raw karambwanji 3150 on empty vessel 3157 in both item orders, including a full inventory,
    and capture exact item-slot movement, message, sound and packet order. Fish with loaded vessel
    3159 and loose bait. Confirm `You lower the Karambwan vessel.`, sequence 1193, one roll every
    four ticks, 5/160 endpoints, 50 Fishing XP only on success, `You catch a Karambwan!`, the exact
    failed-roll line and one bait consumed on every attempt. Watch the vessel become empty and
    reload from the loose stack, including the final bait, multiple loaded/empty vessels, a moved
    selected slot and full/near-full inventories.

    Confirm every successful catch sets the medium Karamja Diary task, then repeat with every
    Rada's blessing tier, spirit flakes alone and the combined 58-percent case. Verify one flake
    per eligible accepted catch, no extra XP, final-slot/last-flake ordering, and one or two raw
    karambwan routed into an open carried or worn fish barrel when the inventory is full. Capture
    clue-bottle and heron eligibility/rates, animation/sound/message/item/XP/var packet order,
    manual-click tick manipulation, movement, combat, interface, logout and death interruption.

    During the quest, repeat at Lubufu's NPC 4714 before and after he teaches the technique. Record
    the precise persistent var/quest transition that unlocks fishing there, all vessel grant and
    replacement dialogue, when that spot becomes unavailable, and Tiadeche's post-quest shop
    transition. Do not promote the quest-lifecycle row until this substate and the main-spot client
    observations are reproduced by automation.

78. Using only the already-open workspace cache-233 RuneLite client, test the third-option `Bait`
    operation at every frogspawn/eel NPC 1497, 1498, 1499, 1500 and 10653, then the first-option
    `Bait` operation at Mort Myre swamp NPCs 2653, 2654 and 2655. Confirm the exact examines and that
    the server's stationary representation agrees with visible production behavior until the full
    relocation graph is implemented.

    At Fishing 27, 28, 37 and 38, with ordinary rod 307 and carried or worn pearl rod 22846, prove
    the boosted-level gate and the ordered cave-eel-before-slimy-eel cascade. Confirm endpoints
    10/80 for both fish, 80 XP for cave-area slimy/cave eels, 65 XP for Mort Myre slimy eels, exact
    catch lines and one bait 313 consumed only on success. Capture first and repeat five-tick
    timing, sequence continuity, sounds, facing and item/XP/message packet order.

    Repeat on a free world, without each requirement, at full inventory, with exactly one final
    bait, after moving/removing the selected rod or bait, and while stepping out of range. Exercise
    equipped Rada tiers, spirit flakes alone and together, carried and worn open fish barrels, clue
    bottles and heron rolls. Capture tick manipulation plus movement, combat, interface, logout and
    death interruption. Verify that access restrictions for Lumbridge caves, Dorgesh-Kaan,
    Camdozaal and Mort Myre are enforced by their entrances/environment and do not add a second
    hidden gate after the character is already beside the spot.

79. At the same frogspawn/eel NPCs, test first-option Net/Small Net with small fishing net 303 at
    visible Fishing levels 32, 33 and 99. Confirm the members and level gates, first and repeat
    five-tick sequence 621, 75 XP for frog spawn 5004, 1 XP for swamp weed 10978, non-stackability,
    exact catch messages, animation continuity, facing, sounds and item/XP/message packet order.

    Collect a statistically useful sample at levels 33 and 99 with raw roll logging. Distinguish the
    historical success-chart model—ordered level-scaled frog spawn at 16/96 followed only on failure
    by constant swamp weed at 10/10—from the same-era spot table's printed 11/256 frog-spawn and
    approximately 43/1000 swamp-weed rarities. In particular, validate whether swamp weed uses a
    stat-random roll; the contemporary Wiki beta backlog calls this unresolved while stating that
    frog spawn does. Do not promote the lifecycle row until this conflict is reproduced and resolved.

    Repeat with each equipped Rada tier, spirit flakes alone and together, and specifically at the
    Ruins of Camdozaal to establish whether the later documented spirit-flake exclusion already
    existed at cache 233. Confirm that each accepted output is doubled as a whole with no extra XP—or
    record the exact exception—and that neither object enters an open fish barrel. Exercise a full
    inventory, one and two remaining slots, moving away, removing the net, free-world access, tick
    manipulation, combat, interfaces, logout and death. Record exact rejection and multiplier
    presentation before promoting the matching-client lifecycle coverage.

    Then validate the implemented lava-eel core on NPCs 4928 and 6784 with oily ordinary rod 1585
    and carried/worn oily pearl rod 23122 plus bait 313. Production deliberately has no catch-time
    Heroes' Quest var or members-world gate once the player owns the tool; verify that behavior at
    every relevant quest state and on a free world, while separately capturing Gerrant's oily-rod
    teaching and acquisition transitions. At level 53, validate 16/96 endpoints, five-tick attempts
    and the selected 60 Fishing XP against the live cache-233 client; record any evidence that the
    spot page's isolated 30-XP value is not its apparent copy of the Cooking XP.

    At the Lava Maze spot, identify the individual hard Wilderness Diary task-bit transition and
    exact feedback; do not substitute aggregate varps 1192/1193 or hard-count varbit 6325. Prove the
    no-heron rule and establish clue-bottle eligibility. Capture which of the two Heroes' Quest
    transcript gear-rejection lines applies to each spot, the exact catch line, animation, sounds,
    packets, relocation, Rada/spirit-flake eligibility and all ordinary interruption, capacity and
    barrel cases before promoting the dedicated quest/diary/client-parity row.

80. Using only the already-open workspace cache-233 RuneLite client, enter inner Mor Ul Rek through
    its production fire-cape access route and test first-option `Bait` on all five infernal-eel spot
    coordinates for NPC 7676. At visible Fishing 79/80, on free/member worlds, with ordinary oily
    rod 1585 and carried or worn oily pearl rod 23122, bait 313, no gloves, ice gloves 1580 and
    Smiths gloves (i) 27031, capture exact gate order and text. Confirm sequence 622/8188, first and
    repeat five-tick timing, 30/93 endpoints, 95 XP, exact catch and Eat lines, facing, sounds and
    item/XP/message packet order. Remove the gloves, rod or bait during a roll and test full inventory,
    final bait, movement, combat, interface, logout, death, tick manipulation, every Rada tier,
    spirit flakes alone/together and carried/worn open fish barrels.

    Complete Regicide, reach Zul-Andra and compare sacred-eel NPC 6488 before and after the High
    Priestess education conversation. Record the exact transition of varbit 4391 through values
    0/1/2/3 and confirm that 0 rejects while every educated state remains eligible. At visible
    Fishing 86/87, test ordinary and carried/worn pearl rods with bait; confirm sequence 622/8188,
    five-tick attempts, 0/60 endpoints, 105 XP, exact catch and Eat lines, both spot coordinates,
    stationary/relocation behavior and the same interruption, capacity, multiplier, barrel,
    audiovisual and packet-order matrix as infernal eels. Establish clue-bottle and heron eligibility
    and rates for both species.

    Finally, smash infernal eels with ordinary, main-hand Imcando and off-hand Imcando inventory
    variants, and dissect sacred eels with a knife. Confirm that the first matching eel is processed
    regardless of the selected slot. Automated coverage already pins immediate manual processing,
    sequences 7553/8969, same-tick spam-click behavior and the exact 29/35 Tokkul 14–20, 2/35 one
    onyx-bolt-tip and 4/35 one-to-five lava-scale-shard distribution; use this client run to discover
    the still-unknown automatic repeat cadence and its stop/restart behavior without inferring it
    from the four-tick sequence duration. For sacred eels, test boosted
    Cooking levels 71, 72, 79, 80, 87, 88, 95, 96, 103, 104 and above; verify scale bands 3–5,
    4–6, 5–7, 6–8 and 7–9, the level-104 cap, exactly `100 + 3 × scales` Cooking XP, manual zero-tick
    processing and automatic two-then-three-tick cadence. Capture the exact membership and level
    failure text, confirm the sacred Eat line currently known only from a secondary mirror, and
    record sound, XP/item/message order, movement, combat/interface interruption, logout and death.
    Do not promote either client-parity row until those observations are recorded.

81. Using only the already-open workspace cache-233 RuneLite client, obtain uncharged object 22368
    and charged object 22370 through production routes. Confirm the level-30 Attack and Magic wield
    requirements and free-to-play availability. Use nature runes on both variants at 0, 1, 999,
    1,000 and over-cap quantities with a full and non-full inventory. Record the exact load and
    already-full messages, selected-slot behavior, charged transformation, whether partial loading
    is offered, and every item/var/message packet. Check both variants in inventory and while worn;
    capture exact zero/singular/plural/cap text.

    With the staff worn, cast nature spells requiring one and multiple nature runes while varying
    stored, loose-inventory and rune-pouch quantities around every insufficiency and split-source
    boundary. Establish source priority empirically. Run enough controlled casts to confirm the 1/15
    conservation event saves exactly one rune rather than the entire multi-rune cost, and capture
    `Your staff saved you a nature rune.` plus its packet order. Verify a failed cast neither spends
    nor rolls charges, and confirm the precise charged-to-uncharged transition and appearance update
    on the final spent charge.

    Select Uncharge at one and 1,000 stored runes, with full inventory, and determine whether a
    confirmation interface appears. Confirm all stored runes are destroyed and capture exact text.
    In the bank, verify the implemented Configure-Charges preference around varbit 16139 (varp 4677
    bits 20–30), object param 2257 and DB row 4340 at zero, 1, 1,000 and rejected over-cap input;
    capture the exact dialogue, cancellation, persistence and feedback. Then exercise automatic
    loading on withdrawal and record nature-rune deduction, partial-resource behavior, item-local
    charge updates, bank deposit behavior and packet order. Test trade, safe and dangerous
    death, protected/unprotected state and Ultimate Ironman behavior to settle the one-rune retention
    wording. Finally reproduce the members-only manual recipe at boosted Crafting 61/62 boundaries
    with both item-use orders, a full inventory and stale selected slots. Confirm zero XP, exact
    animation and duration, success/rejection text, interruption behavior and item/message order;
    the cutoff Wiki leaves the manual duration unknown. On free-to-play, verify both staff variants
    require base Attack 30 and Magic 30 (including boosted-level edge cases). Then test Zaff's
    one-essence plus 50,000-coin service without a Crafting requirement and capture its exact NPC
    route, dialogue, confirmation, capacity and atomic ordering. Do not promote the stored-rune,
    automatic-bank-charge, death/trade, manual-presentation or Zaff rows until each applicable observation is
    reconciled and covered by automation.

82. Using only the already-open workspace cache-233 RuneLite client, obtain empty Tome of Water
    25576, charged Tome 25574 and soaked pages 25578. With the tome in inventory, use pages on both
    variants at 0, 1, 19, 20, 19,990 and 20,000 charges, with single and stacked pages and with a
    full inventory. Confirm 20 charges per page, the 20,000 cap, same-slot transformation, any
    partial final-page loss, exact load/full messages and item/var/message packet order. Exercise
    inventory and worn Check at zero, one, multiples of 20 and the cap; record exact singular and
    plural wording, appearance refresh and whether the empty variant offers any server action.

    Use both cached Pages operations to remove pages around exact and partial-page boundaries.
    Record the quantity interface or confirmation, rounding direction, whether the partially used
    page is lost, capacity handling, exact messages and packet order. In the bank, enable item
    options and exercise Configure-Charges around varbit 16152 on varp 4682: determine its bit range,
    accepted quantity bounds, zero/disable semantics, withdrawal auto-loading, insufficient-page
    behavior and persistence. Test multiple tomes with distinct item-local charge counts, bank
    deposit/withdrawal, trade, safe and dangerous death, protected/unprotected state and Ultimate
    Ironman rules.

    With one or more positive charges, cast every Standard combat family that requires water runes,
    including offensive water, Confuse/Weaken/Curse/Vulnerability/Enfeeble/Stun and
    Bind/Snare/Entangle, on both hit and splash. Confirm exactly one charge per attempted cast, loose
    water-rune preservation, 10% PvM versus 20% PvP offensive accuracy/damage, 20% curse/bind
    accuracy and 50% stronger stat drains. Repeat on the final charge and verify that its modifiers
    still apply after the tome becomes empty. Then cast Standard utility spells and water-requiring
    Ancient/Lunar/Arceuus combat and utility spells: confirm unlimited water runes but no charge use.
    At zero charges, verify that the charged identity supplies no runes or effects and ordinary
    inventory/pouch/combo-rune priority resumes. Capture exact failure text, audiovisual timing and
    packet order before promoting the client/bank companion row.

83. Using only the already-open workspace cache-233 RuneLite client, unlock Ancient Magicks through
    Desert Treasure I and test every Smoke, Shadow, Blood and Ice Rush/Burst/Blitz/Barrage spell.
    Capture access-failure order, exact text, five-tick cadence, sequence 1979, projectile/impact
    graphics, cast/impact sounds, rune and XP packet order, splashes and zero-damage successful hits.
    For Burst/Barrage, test single- and multi-combat, more than nine eligible targets, mixed valid
    and invalid secondary players, shared tiles, and large NPCs around every edge of the 3-by-3 zone
    centred on the primary target's south-west origin. Confirm independent accuracy and damage rolls
    and exact target ordering.

    For Smoke, establish the one-in-eight poison proc and 10/20 severity, immune and already-toxic
    targets, ordinary/Smoke sceptre 10% effect scaling, and the enhanced 20% healing reduction for
    exactly ten ticks on an already-poisoned player and NPC across food, potions, natural recovery,
    Blood spells and encounter healing. For Shadow, confirm 10%/15% Attack drain, non-stacking,
    ordinary sceptre scaling, enhanced Strength/Defence drains and the Shadow Sire/Leviathan
    exceptions. For Blood, verify 25% healing from rolled rather than applied damage, summed AoE
    healing, every Bloodbark piece, multiplicative sceptre stacking and enhanced 10% overheal. For
    Ice, verify 8/16/24/32-tick holds, five-tick refreeze immunity and PvP distance release.
    Automated coverage now pins 33% resistance on all five Phantom Muspah forms, exactly one
    duration reduction to 5/10/16/21 ticks for ordinary Rush/Burst/Blitz/Barrage, and no bind at
    100% resistance. Re-observe those four durations on each targetable Muspah form, then repeat
    with an ordinary Ancient sceptre and enhanced Ice sceptre to confirm operation ordering and
    10% accuracy only on unfrozen freezable targets. Catalogue all other NPC resistance/immunity
    cases separately. Confirm 5% Ancient damage per Virtus piece and no extra non-Ancient
    bonus. Repeat inside Barbarian Assault to establish the exact unavailable behavior before
    promoting the client/encounter row.

84. Using only the already-open workspace cache-233 RuneLite client, select the Arceuus spellbook
    and test Ghostly Grasp before A Kingdom Divided, then Skeletal Grasp, Undead Grasp and Mark of
    Darkness immediately below and at the cache quest-completion state 150. Confirm the exact
    access-failure order, spellbook/component behavior, rune and XP packet order, five-tick combat
    cadence and fixed two-tick hit delay. Capture the cast, projectile, hit and splash animation,
    graphic and sound timeline for all three Grasp spells, including zero-damage successful hits.

    With deterministic samples, verify successful-hit bind chances of 10%, 25% and 50%, hold
    durations of 2, 3 and 4 ticks, no bind roll on a splash, and exactly two ticks of refreeze
    immunity. Repeat against players, moving targets, immune and resistance-bearing NPCs, existing
    binds and targets crossing distance boundaries. Cast Mark with and without a Purging staff and
    confirm its exact success line, ten-tick recast lock, 300- versus 1,500-tick duration snapshot,
    ten-tick warning, expiry presentation and persistence through logout, reconnect, death and
    world transition. Prove Mark doubles only the bind chances to 20%, 50% and 100%, and capture its
    demonbane accuracy/damage effects separately from the short cooldown varbit. Finally verify
    Ward of Arceuus consumes 1 cosmic, 2 nature and 4 soul runes, awards 83 XP, snapshots base Magic
    level as its tick duration, uses a 50-tick cooldown, reduces a successful incoming Grasp to one
    tick and floor-reduces demon damage by 10%. Exercise Lesser and Greater Corruption before and
    during Ward, cast Ward while corrupted, and prove cancellation, immunity and non-consumption of
    the attacker's corruption buff. Verify the 33% demon prayer-drain reduction alone and
    multiplicatively with a Spectral spirit shield. Record exact persistence, messages, graphics,
    sounds, packets and ordering before promoting the client/encounter row.

85. Using only the already-open workspace cache-233 RuneLite client, cast Inferior, Superior and
    Dark Demonbane at a normal demon, a non-demon, a player, Duke Sucellus, a Tormented Demon in
    each shield state and Yama. Repeat below and at A Kingdom Divided completion state 150, with
    and without Mark of Darkness, and with and without a Purging staff. Record the exact rejection
    text and access-check priority, all cast/splash/hit animations, graphics, sounds and packets,
    the fixed two-tick impact, rune/XP order and five-tick cadence. Measure +20/+40 unmarked and
    +40/+80 marked accuracy, +25%/+50% post-gear damage, nested effectiveness rounding and every
    below/above-100% target modifier. Exercise every staff that offers Demonbane autocasting,
    including selection persistence across insufficient levels/runes under the 9-April-2025 rules,
    and catalog every cache-233.4 demonic target and encounter override before promoting this row.

86. Using only the already-open workspace cache-233 RuneLite client, cast Lesser and Greater
    Corruption below and at their level requirements, before and after A Kingdom Divided completion,
    and with exact, substituted, insufficient and Fountain-provided runes. Capture exact cast
    animation, graphics, sounds, messages and rune/XP/varbit packet order, including the shared
    30-second cooldown and replacing an already-armed tier after that cooldown.

    Land successful positive- and zero-damage melee, ranged and magic hits against players, plus
    splashes/misses, and measure ordinary 50% activation and consumption behavior. Repeat while
    Mark of Darkness is active to prove guaranteed activation and five-tick rather than ten-tick
    pulses. Confirm Lesser drains 1/2/3 Prayer and Greater 2/4/6, including Prayer below each pulse,
    exact hitmarks, graphics and target/source messages. Exercise an already-corrupted target,
    expiry followed by immediate reapplication, Ward cast during corruption, hits into active Ward,
    Ward expiry and the preserved armed effect. Record the exact priority and persistence through
    logout, reconnect, death and world transition before promoting the client/encounter row.

87. Using only the already-open workspace cache-233 RuneLite client, cast Vile Vigour below and at
    Magic 66, before and after A Kingdom Divided completion, with exact, substituted, insufficient
    and Fountain-provided runes. Confirm the exact rejection priority, rune/XP behavior, sequence
    8978, graphic 1876, sound and packet order. Test zero Prayer, full run energy, every fractional
    run-energy boundary around a displayed percentage, Prayer insufficient for the missing energy
    and Prayer exceeding it. Prove only enough Prayer is consumed to reach 100%, and that the
    cooldown begins and ends on the exact 17-tick boundary. Cast while walking, running, routed,
    skilling and in combat to capture which actions continue or stop and the precise update order
    before promoting the client-parity row.

88. Using only the already-open workspace cache-233 RuneLite client, read Rite of Vile
    Transference object 30806 with unlock varbit 16269 clear. Capture the exact initial page,
    confirmation title/options, cancellation behavior, success animation/graphic/sound/message,
    item/varbit packet ordering, repeat-read behavior and persistence through logout, reconnect,
    death and world transition. Confirm later Death Charge casts use the red cast/end graphics and
    permit exactly two qualifying triggers without changing the 100-tick expiry or cooldown.

    Cast ordinary and upgraded Death Charge below and at Magic 80, before and after A Kingdom
    Divided completion, with exact/substituted/insufficient/Fountain-provided runes. Record exact
    failure and cooldown priorities, animation 8983, kill animation 8984, graphics 1854/1855 and
    3288/3289, sound, XP/rune/varbit ordering and the exact 100-tick boundaries. Kill ordinary NPCs
    at distances 16 and 17, with zero and full special energy, after PvP inside/outside ten cycles,
    through thralls, poison and delayed damage, and with several attackers in single/multi combat.
    Prove the credited player receives 15% and each qualifying kill consumes one charge even at a
    full bar. Exercise Amoxliatl ice blocks, Moons of Peril, Nightmare/Phosani totems, Wardens
    obelisk/siphons, Vasa crystal, Olm hands, Sire respiratory systems and Nex; establish Nex's
    highest-contribution ownership separately before promoting the client/encounter row.

89. Using only the already-open workspace cache-233 RuneLite client, cast Degrime at Magic 69/70,
    before/after A Kingdom Divided and Druidic Ritual, with exact, substituted, insufficient and
    Fountain-provided runes. Record exact failure/no-herb/cooldown wording and priority, sequence
    8980, preparation sequence 8981, effect sequence 9002, graphic 1885, any sounds, and the exact
    rune/Magic-XP/Herblore-XP/inventory packet order. Fill an inventory with every grimy herb,
    include duplicate and interleaved non-herb slots, and test current Herblore levels immediately
    below/at every requirement plus temporary boosts and drains. Confirm half XP is rounded per
    herb and every eligible item remains in its original slot.

    Cast while stationary, walking, running, already routed, skilling and in combat. Reproduce the
    documented moving cast that queues but never completes, capture the exact point at which `Your
    last spell is still in progress!` begins, then stop and recast to establish recovery and the
    exact eight-tick boundary. Compare these captures with the server's conservative active-route
    rejection before promoting the client-parity row.

90. Using only the already-open workspace cache-233 RuneLite client, cast Shadow Veil at Magic
    46/47 and base Magic 47/50/99, with temporary boosts and drains, before/after A Kingdom Divided,
    and with exact, substituted, insufficient and Fountain-provided runes. Confirm the exact
    activation/cooldown/expiry wording and priorities, sequence 8979, graphic 1881/effect 8998,
    sounds, varbits 12414/12291, packet order, the base-level-duration boundary, 50-tick refresh and
    persistence through logout, death and world transition.

    After a production Thieving plugin exists, force failed pickpockets with neither protection,
    Shadow Veil alone, a dodgy necklace alone and both together. Prove the veil rolls first at
    exactly 15%, a successful veil roll emits `Your attempt to steal goes unnoticed.` without
    charging the necklace, a failed veil roll permits the independent 25% necklace roll, and the
    combined rate is 36.25%. Verify ordinary NPCs across failure stun/damage variants and prove
    blackjacking never invokes the veil before promoting the integration row.

91. Using only the already-open workspace cache-233 RuneLite client, cast Dark Lure at Magic
    49/50, before/after A Kingdom Divided, with exact, substituted, insufficient and
    Fountain-provided runes. Record exact failure/cooldown wording and priority, the human cast
    sequence, cast/travel/impact graphics 1882/1883/1884 with effect sequences 8999/9000/9001,
    sound identities, projectile travel, varbit 12289 and rune/XP/animation/graphic/var packet
    ordering. Establish the exact recast boundary and whether movement or another action interrupts
    either the cast or its delayed impact.

    Target fleeing and stationary imps, unicorns and ghouls, an immobile creature and every impling
    tier. Confirm attackable targets engage the caster, immobile targets consume nothing, and an
    impling remains in a two-by-two area following the caster for exactly six seconds before
    wandering again. Test obstacles, region boundaries, target despawn/respawn and player
    logout/death during projectile travel and follow. Capture the exact post-lure catch-ownership
    grace and contention rules with two players before promoting the client-integration row.

92. After a production Farming patch lifecycle exists, use only the workspace cache-233 RuneLite
    client to cast Resurrect Crops on every supported dead allotment, flower, herb, hops, bush,
    tree, fruit-tree, cactus, mushroom, belladonna, calquat, hardwood, seaweed, anima, redwood,
    crystal-tree, celastrus and special patch family. Prove which patches are legal targets and
    capture the exact target cursor, wording, failure priorities, sequence 7118, graphics, sounds,
    rune/XP/patch packet order and action/cooldown timing.

    At current Magic 77/78 and every level through 99, plus boosts above 99 and drains below the
    requirement, collect enough deterministic or instrumented rolls to reconstruct the exact
    integer interpolation between 50% and 75%. On success, prove the restored growth stage, growth
    clock, compost/protection/yield state and later disease behavior. On failure, prove the exact
    empty-patch transition. Kill an already resurrected crop again and verify it cannot be revived
    twice. Exercise logout, reconnect, world hop, restart, Spellbook Swap, diary/League task credit
    and concurrent-player interaction before promoting this row.

93. Using only the already-open workspace cache-233 RuneLite client, cast Demonic Offering at
    Magic 83/84 and Sinister Offering at 91/92 before and after A Kingdom Divided, with exact,
    substituted, insufficient and Fountain-provided runes. Capture the exact membership, quest,
    level, rune, no-remains and active-cooldown wording and priority. Confirm objects 15346/8796,
    components 218:177/178, human cast/preparation sequences 8975/8976, graphics 1871/1872 with
    effect sequences 8992/8991, varbit 12423, sounds and the rune/Magic-XP/Prayer-XP/item/stat/
    animation/graphic/varbit packet order.

    Test inventories containing zero through four matching remains, duplicate and interleaved
    bone/ash types and every supported `PrayerRemainsRecipes` identity. Prove only the top-left
    three matching items are consumed, each awards exactly three times its own bury/scatter XP,
    ordinary remains restore one Prayer point, and infernal ashes plus Ourg, Dagannoth, hydra and
    superior dragon bones restore two. Equip every Zealot piece and the full set to prove neither
    spell preserves remains. At the Fountain, prove runes are supplied and only Magic XP is
    suppressed.

    Measure the shared cross-spell recast boundary cycle by cycle, and determine whether the client
    transmits preparation sequence 8976 after the instant-cast update. Cast while stationary,
    walking, running, routed, skilling and in combat to verify that actions remain uninterrupted.
    Exercise logout, reconnect, death and world transition during the cooldown and record exact
    persistence/reconstruction before promoting the client/lifecycle row.

94. Using only the already-open workspace cache-233 RuneLite client, cast Arceuus Library, Draynor
    Manor, Battlefront, Mind Altar, Respawn, Salve Graveyard, Fenkenstrain's Castle, West Ardougne,
    Harmony Island, Cemetery, Barrows and Ape Atoll Teleport one level below and at their respective
    requirements. Test exact, substituted, insufficient and Fountain-provided runes, membership,
    every quest boundary, no A Kingdom Divided completion, Tele Block, and Wilderness levels 20 and
    21. Capture exact failure priority and wording, four-tick timing, sequence 6575, graphic 1296
    with effect sequence 7162, synth 200, rune/XP/animation/graphic/sound/teleport packet order,
    landing distribution and behavior while walking, routed, skilling and in combat.

    For Salve, Fenkenstrain and Barrows, isolate Priest in Peril completion from the distinct
    `heard Drezel's warning` condition and identify its authoritative varp/varbit threshold. For Ape
    Atoll, prove Monkey Madness I completion works without Daero's training. Set each of Lumbridge,
    Falador, Camelot, Edgeville, Prifddinas, Ferox Enclave and Kourend Castle as the standard respawn
    location and prove Respawn Teleport follows it; verify the Defence-cape Ardougne override does
    not count. Change the location, then exercise logout, reconnect, death, world hop and server
    restart to pin persistence and integration with the death destination before promoting the
    client/respawn row.

95. Using only the already-open workspace cache-233 RuneLite client, test Basic, Adept, Expert and
    Master Reanimation with every initial and processed ensouled head, including the Poll 76
    hellhound addition. Verify levels 16/41/72/90, exact body/nature/soul/blood costs, 32/80/138/170
    Magic XP, each of the twenty-three Prayer rewards from 130 through 1,560, components 218:149/
    151/152/153, player sequence 7198, creature sequence 3865, graphics 1288–1290 and the exact
    rune/item/XP/animation/projectile/spawn packet order. Prove that A Kingdom Divided is not a gate.

    Drop each initial head and cast at its origin, at every boundary tile of the documented
    31-by-31 area and one tile beyond. Then pick it up, bank it, trade it, list/retrieve it through
    the Grand Exchange and pass it through the price checker before repeating the boundary test.
    Establish the exact transition that removes origin eligibility and prove processed heads only
    work within 15 tiles of the Dark Altar. Include noted heads, Vorkath's head and the three-head
    King Black Dragon conversion. Capture exact too-far, no-room, one-active, wrong-head/tier,
    members, spellbook, level and rune failure priority and wording.

    Block every surrounding footprint for sizes one through five, test single-way and multiway
    areas, and prove one creature can be active per player while several players can reanimate
    independently. Measure spawn delay, lifetime, despawn, logout, reconnect, death and world-hop
    behavior. Verify every creature's attack style, dragonfire and other special attacks, no bone or
    ordinary drop, Prayer XP only for the credited owner, abyssal Slayer-85 harm restriction, Slayer
    task credit and Soul Bearer interaction. Roll each Zealot piece and the full set at its exact
    1.25%/5% boundaries, and verify the saved-head message and full XP. At the Fountain, prove runes
    and Magic XP are suppressed while the head, fight and Prayer reward remain normal. Capture all
    audio, projectile timing, packets and client state before promoting the client-parity row.

96. Using only the already-open workspace cache-233 RuneLite client, cast all nine Resurrect
    Lesser, Superior and Greater Ghost, Skeleton and Zombie spells at Magic 37/38, 56/57 and 75/76,
    before and after A Kingdom Divided, with the Book of the Dead absent, carried and equipped.
    Prove exact air/cosmic/mind, earth/cosmic/death and fire/blood/cosmic costs, Prayer costs 2/4/6,
    Magic XP 55/70/88, components 218:184-192, NPCs 10878-10886, player sequence 7198, player
    graphic 1288, spawn sequences 9046-9048, travel/impact graphics 1906-1908 and varbits 12290,
    12410, 12412 and 12413. Capture exact feedback, failure priority, sounds and packet order with
    exact, substituted, insufficient and Fountain-provided runes.

    At every current Magic level, including temporary boosts and drains, time natural despawn and
    prove completed Master Combat Achievements (varbit 12867 value 2) doubles duration while lower
    tier/status values do not. Recast cycle by cycle to resolve the 16-tick infobox versus ten-second
    prose conflict, prove one-active replacement, and exercise logout, reconnect, death, world hop
    and spellbook swap. Block each nearby square to identify spawn priority and the exact no-room
    boundary.

    Attack targets with melee, ranged and Magic while each thrall is active. Prove target mirroring,
    four-tick cadence, six-range behavior, one-tick-earlier ghost impact, always-successful 0-max
    rolls, no ordinary combat XP, player kill/damage credit, flat-armour bypass, invulnerability,
    no player targeting and kurask/turoth immunity. Measure line-of-sight, pathing, target loss,
    doors and exact room-boundary refusal. Repeat in every documented restricted location and named
    quest encounter, including the Chasm of Fire/Yama exception and Jormungand's distinct soft-ground
    message, before promoting the client-parity row.

97. Using only the already-open RuneLite client built from the workspace cache-233 checkout, mine
    both ordinary cache variants of clay, copper, tin, blurite, iron, silver, coal, gold, mithril,
    adamantite and runite rocks. Never open, focus, restart or modify the separately installed
    `/Applications/RuneLite.app`. At each exact level boundary, record the start/failure text,
    Mining XP, ore, first and repeated roll cadence, depletion identity and respawn duration. Test
    bronze, iron, steel, black, mithril, adamant, rune and gilded pickaxes carried and equipped,
    including mixed inventories, unusable higher-tier tools, item-on-rock, full inventory, movement,
    combat, interfaces, logout, death, two-player contention and depletion on the deciding cycle.

    Separately exercise Mining Guild rocks and the invisible +7 boost; all Mining-glove tiers and
    their extra-gather counts. Confirm the automated dragon/infernal/third-age 1/6 and active-crystal
    1/4 two-cycle timing distributions, inactive-crystal fallback, exact family animations, member/
    Song of the Elves rejection precedence and mixed carried/equipped priority. Separately test
    the automated Infernal standard-dragon-only creation, three-tick timing, unboostable 61 Mining/
    85 Smithing gates, 200/350 XP, cosmetic rejection, ornament/revert and inventory/worn style
    switching. Exercise the automated 100%-energy Rock Knocker on all fourteen cache identities,
    including both depleted families, both Trailblazer styles and inactive crystal; capture the
    exact special-bar refusal, overhead text, animation, found-gem sound and packet order. Then test
    the unresolved Infernal passive smelting, depletion, recharge and Check presentation. Crystal
    bank preference automation pins active object 23680's shared row-4338 route, permanent varbit
    16148, members gate and zero-to-20,000 bound; separately capture exact Configure-Charges
    presentation, automatic shard deduction and withdrawal mutation, item-local charges, manual
    shard charging and Revert operations; Varrock armour,
    celestial ring/signets, Mining
    cape and prospector effects; gem, unidentified-mineral, crystal-shard, geode, rock-golem and moon-
    key rolls; Tutorial Island XP limits; perfect-gold quest gates; membership and area-specific
    rock variants. Capture exact sound, animation continuity, overlay colour/duration, message and
    inventory/stat/location packet order, interruption rules and respawn exceptions before promoting
    the extended Mining row. This case remains pending; no installed RuneLite application is an
    acceptable substitute for the workspace client.

98. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test both cache
    Gem rocks identities 11380 and 11381 against the running rsmod server. Never open, focus,
    restart or modify `/Applications/RuneLite.app`. At Mining 39 and 40, capture failure/start text,
    animation and first-roll timing. Force or statistically observe all seven rewards and confirm
    exact capitalization and articles, 65 XP, inventory placement, synth 2655, depletion identity,
    overlay and the full 99-cycle respawn. Repeat with a full inventory.

    Measure normal endpoints against ordinary and trimmed glories at charges 1–6, eternal glory,
    both uncharged glories and no necklace; confirm the charged forms triple only gem-rock success.
    Exercise Karamja access/quest boundaries, all Mining-enhancer exclusions, clue-geode and
    rock-golem rolls, movement/combat/interface/logout interruption, two-player contention and a
    same-cycle success race. Capture exact message, sound, animation, inventory/stat/location and
    overlay packet order before promoting any client-facing or cross-system row. This case remains
    pending until observed through that workspace client.

99. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, mine each
    ordinary clay-through-runite rock family against the running rsmod server. Never open, focus,
    restart or modify `/Applications/RuneLite.app`. Capture a normal pre-table gemstone and verify
    that it appears before the ore roll, says `You just found a/an [lowercase gem]!`, plays the
    found-gem sound, awards no Mining XP, leaves the rock present and continues mining on the normal
    pickaxe cadence. Repeat for sapphire, emerald, ruby and diamond and confirm the 70/128 nothing
    result also consumes a roll without a message, ore, XP or depletion.

    Statistically distinguish the exact 128/32,768 normal rate from the 128/8,216 charged-glory
    rate. Test ordinary and trimmed charges 1–6, eternal glory, both uncharged glories, no necklace,
    free/member-world boundaries and a glory left equipped as membership expires. Repeat the open-
    bag Mining interception from case 100. Capture animation continuity, message/sound/inventory
    order, interruption, logout and same-cycle contention. This case remains pending until observed
    through that workspace client.

100. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test Gem bag
    12020 and Open gem bag 24481 against the running rsmod server. Never open, focus, restart or
    modify `/Applications/RuneLite.app`. Confirm the five inventory operations on each state, the
    bankside Empty operation, exact Open/Close and two-line Check feedback, Fill and symmetric
    gem-on-bag behavior for uncut sapphire, emerald, ruby, diamond and dragonstone, rejection of
    each cut form, independent 60-item caps, and the nothing-suitable and full messages. Fill all
    five caps concurrently, then empty into inventory with fewer than five free slots to prove
    sapphire-first order; repeat at a bank with limited and unlimited capacity.

    With the bag open, acquire every accepted gem from ordinary-rock and gem-rock Mining, ground
    pickup and every relevant Thieving source. Prove capacity fallthrough behavior at 59/60,
    free/member-world boundaries, logout/reconnect persistence, only-one-bag ownership/shop rules,
    closed-bag behavior, Destroy confirmation/cancellation and content handling, death in ordinary
    and protected-item conditions, and recovery. Resolve the cutoff page's retained-on-death claim
    against later conflicting prose. Capture exact messages, interfaces, sounds and inventory/bank/
    persistence packet order before promoting client parity. This case remains pending until
    observed through that workspace client.

101. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, connect to the
    running rsmod server and test every locally reachable Man, Woman, Farmer, Warrior, Rogue,
    ordinary Guard, Kharidian Bandit, Knight, Yanille Watchman and Ardougne Paladin `Pickpocket`
    identity, including
    Varlamore, Kastori and Tal Teklan Farmers. Never open, focus, restart or modify
    `/Applications/RuneLite.app`. At
    level 1, confirm
    successful Man/Woman attempts award exactly 8 XP and one citizen coin pouch, repeat every two
    ticks and produce three
    coins per pouch through both Open and Open-all. Confirm the shared pouch ceiling at 28 and each
    56/84/140 diary tier while mixing other pouch identities in the inventory. At level 10, confirm
    Farmers award exactly 14.5 XP, nine-coin Farmer pouches at 123/128 and potato seeds at 5/128,
    with the same two-tick success cadence and both pouch operations.

    At level 25, confirm all seven ordinary/Al Kharid Warrior identities award exactly 26 XP and
    one 18-coin Warrior pouch, use 100/240 base success endpoints and deal two damage on an
    unprotected failure. At level 40, confirm the 91 registered ordinary city/regional Guard
    identities award exactly 46.8 XP and one 30-coin Guard pouch, use 50/240 endpoints and deal two
    damage. Verify Warrior and Guard pouch `Open` and `Open-all`, the shared mixed-pouch ceiling,
    their exact Gloves/diary/cape modifier endpoints, and Rogue-equipment doubling. Confirm H.A.M.
    storeroom guards 4522–4526 retain their separate operation-one interaction and never enter the
    ordinary Guard dispatcher.

    At level 32, test ordinary Rogue 526 and confirm 36.5 XP, two-damage failures, 75/240 base
    endpoints and the exact 144-weight output partition: a 25–40-coin Rogue pouch at 123/144, eight
    air runes at 9/144, wine at 6/144, a lockpick at 5/144 and an iron dagger(p) at 1/144. Open Rogue
    pouches singly and as a stack, recording independent inclusive 25–40 rolls and their coin-jingle
    order. With Rogue equipment doubling, confirm one pouch plus one direct 25–40 coin roll and no
    extra XP. Confirm wilderness Rogue 6603 has no Pickpocket operation in the matching client.

    At level 15, probe male and female H.A.M. members 2540 and 2541 and confirm that the matching
    client dispatches their cache operation-one `Pickpocket`, not the standard operation-three
    route. Confirm 22.2 XP, 135/239 base endpoints, 141/250 with Gloves of silence, 148/262 with
    the hard Ardougne Diary and 162/288 with diary plus cape. Measure the four-second stun and the
    inclusive 1–3 failure damage. Force the 1/50 easy-clue pre-roll, including maximum scroll-case
    ownership and the rule that Rogue equipment does not double the clue.

    Conditional on reaching the ordinary H.A.M. table, validate its exact 100 shares: each of the
    seven clothing pieces at 1; the eight bronze/iron/leather entries at 3 each; the four steel
    entries at 2 each; buttons 4, the 1–21-coin pouch 17, feathers 3, knife 2, logs 3, needle 2,
    raw anchovies 2, raw chicken 2, thread 3, tinderbox 2, uncut opal 2, coal 2, cowhide 3, damaged
    armour 4, nested herbs 2, iron ore 2, rusty sword 4 and uncut jade 2. Check 1–13 bronze/steel
    arrows, 1–7 feathers, 1–10 thread, and the herb branch's 6/11 guam, 3/11 marrentill and 2/11
    tarromin split. Open pouch 22523 singly and in bulk at both 1 and 21 coins and record Rogue
    doubling for every ordinary output.

    During the relevant Death to the Dorgeshuun stage, verify that a non-clue success rolls a 1/5
    uniform seven-piece H.A.M.-clothing override before the 4/5 ordinary route. On failures, recover
    the exact Agility partial-avoidance formula, per-piece robe reduction, three-slight-concussion
    state, jail-versus-outside probability, each destination weight, and reset rules across leaving,
    re-entering, logout, world hop and death. Do not promote this family until the Wiki's approximate
    “about 4%” robe value and all ejection routing are replaced by exact matching-client evidence.

    After completing Death to the Dorgeshuun, enter the H.A.M. Storerooms and inspect post-quest
    guards 4522–4526. Confirm each exposes operation-one Pickpocket, requires level 20 and grants
    exactly 22.2 XP. Before/during the infiltration stage, inspect quest patrol forms 4516–4520 and
    arrest form 4521; confirm none exposes Pickpocket, record the exact var/quest transition that
    replaces them with 4522–4526, and verify the five Zanik knockouts plus final arrest lifecycle.

    Recover the missing success chart for post-quest guards at boosted Thieving levels 20 through
    99, then repeat with Gloves of silence, medium and hard Ardougne Diary states and Thieving cape.
    Record exact integer endpoints/rounding, whether the local medium diary applies underground,
    failure stun cycles, the inclusive damage distribution up to three, player/NPC sequences,
    spot effects, sounds, messages, target facing and retry cadence. Confirm no failure ever adds a
    H.A.M.-member concussion or ejects the player, with zero through seven H.A.M. pieces and varied
    Agility levels.

    Force every boundary of the exact 500-share table: bronze/iron/silver/steel keys at 50 shares
    each; the eight bronze/iron/leather entries at 9 each; seven H.A.M. clothes at 3 each; four steel
    entries at 6 each; buttons 12, pouch 45, feathers 9, knife 6, logs 9, needle 6, raw anchovies 6,
    raw chicken 6, thread 9, tinderbox 6, opal 6, easy clue 6, coal 6, cowhide 9, damaged armour 12,
    nested herbs 6, iron ore 6, rusty sword 12 and jade 6. Verify 1–13 arrows, 1–7 feathers, 1–10
    thread, the 6/11 guam, 3/11 marrentill, 2/11 tarromin herb branch, and pouch 22523's independent
    1–21 coin opening. With partial/full Rogue equipment, recover whether the single 500-way result
    shares the ordinary doubling roll and how keys, pouch, clue and nested-herb outcomes double.

    Repeat each result at zero and one free inventory slots, with maximum easy-clue ownership and
    mixed coin pouches at every cap. Record discard, rollback or ground routing and whether XP still
    commits. Fail against every guard while it patrols: verify it stops, returns to its own start
    tile and no longer attacks witnessed door attempts; recover exact path, return tick, persistence
    across logout/world hop and reset trigger. Map the door line-of-sight test and confirm ordinary
    patrol aggression versus the speak-without-full-H.A.M.-robes aggression. Finally obtain key
    objects 8867/8869/8868/8866, verify they cannot be traded despite cache 233's generic
    `tradeable=true` metadata, and confirm their south-west/north-east/north-west/south-east chest
    mapping, one-key consumption and exact metal-specific break messages. Keep this family out of
    production until these measurements replace the missing success chart and timing values.

    At each of the four central-room locked doors—15759 at (2567,5198), (2576,5198),
    (2567,5192) and (2576,5192)—confirm `Open` dispatches as operation one and `Pick-lock` as
    operation four. Recover the minimum Thieving level and full integer success chart at every
    relevant boosted level both without and with lockpick 1523, including whether the tool must be
    carried or can be equipped and whether it is ever consumed. Confirm exactly 4 XP on success;
    record failure and automatic-retry cadence, walk/face/animation state, door passage or transform
    behavior, interruption rules, messages, sounds and packet ordering. Verify both south-west wall
    cracks bypass that room's lock and that the northern-room door is unlocked. Repeat while every
    guard patrols, returns after a failed pickpocket, is engaged, and has/loses line of sight; map
    exact observer selection, attack tick, multi-guard contention and reset behavior before
    production dispatch.

    Inspect the six H.A.M. Storeroom Small chests and confirm closed types/coordinates exactly:
    bronze 15723 at (2570,5192), steel 15722 at (2573,5193), silver 15724 at (2569,5199), iron
    15726 at (2573,5198), and steel 15722 at (2577,5208) and (2577,5209). Confirm every opened
    chest becomes operationless type 15725. Exercise bronze key 8867 south-west, iron key 8869
    north-east, silver key 8868 north-west and steel key 8866 on the south-east and both northern
    chests, including every wrong-key and no-key combination. Record exact missing/wrong-key text,
    key-removal tick, closed-to-open transform tick, open duration, replacement tick and animation,
    sound, message and inventory packet order.

    Force all 150 reward shares: 0–99 coins at 75; sapphire necklace at 6; diamond+sapphire
    amulet+sapphire necklace+sapphire ring at 2; ruby necklace at 3; gold ring at 5; sapphire
    amulet+necklace+ring at 6; emerald ring+sapphire amulet+necklace at 4; sapphire amulet+necklace
    at 6; diamond necklace at 2; gold amulet at 5; diamond ring+sapphire amulet+necklace at 2;
    sapphire+sapphire amulet+necklace+ring at 5; emerald necklace at 4; emerald amulet+sapphire
    necklace at 4; gold necklace at 6; diamond amulet+sapphire necklace at 2;
    ruby+sapphire amulet+necklace+ring at 3; ruby ring+sapphire amulet+necklace at 3; ruby
    amulet+sapphire necklace at 3; and emerald+sapphire amulet+necklace+ring at 4. Repeat each
    bundle with zero through four free slots, existing jewellery stacks where relevant and a
    forced zero-coin roll. Record whether the key is consumed, chest opens and any partial reward
    commits on insufficient capacity. Have two players open the same chest on the same and adjacent
    ticks; capture winner, loser feedback, reward/key atomicity, personal/global transform scope and
    logout/world-hop reset. Do not dispatch the chests until these observations establish one
    complete transaction and lifecycle contract.

    At level 50, test Wealthy citizen NPCs 13302–13305 and confirm operation-one Pickpocket followed
    by Talk-to, 96 XP, 35/200 base endpoints, three-damage failure and a four-second stun. Confirm
    Gloves produce 36/210, the hard Ardougne Diary produces 38/220 and diary plus cape produces
    41/242. Force the exact 85-share table: one fixed 85-coin pouch 28822 at 79/85, House keys 29325
    at 5/85 and an easy clue at 1/85. Complete the Easy Combat Achievement tier and prove the clue
    rate changes to 1/80; test before and after X Marks the Spot to capture loose-scroll versus
    scroll-box selection and already-owned/capacity behavior. With a full inventory, prove that a
    rolled key or clue is discarded while the pickpocket still completes; with full Rogue
    equipment, verify two keys and recover the clue behavior. Capture permanent varbit 9942 before
    and after every coin, key, clue, failure and capacity-discard outcome, beginning from controlled
    values across 0–255; recover its increment, cap, key-guarantee or chance-adjustment threshold
    and reset rule before registering the ordinary action. Confirm the official overview's
    coin-pouch-plus-key wording against the direct subject table's 79/85 branch. Observe street
    urchins spawning or selecting
    three citizens every 84–99 seconds, the approximately 20-second automatic guaranteed-
    pickpocket window and its stop condition. World-hop immediately before distraction and recover
    the exact recency boundary, roughly 15-second delayed start and no-item period before promotion.

    In Pollnivneach, inspect bearded Bandit morphs 736/737 and non-bearded morphs 734/735 on both
    sides of The Feud's blackjack unlock. Confirm that 734/736 expose Talk-to, Attack and Pickpocket
    only, while 735/737 add Lure and Knock-Out. For ordinary awake pickpockets, confirm level 45,
    65 XP and one fixed 40-coin pouch 22529 for bearded bandits, and level 55, 84.3 XP and one fixed
    50-coin pouch 22532 for non-bearded bandits. Open each pouch singly and in bulk, verify the
    shared mixed-pouch cap and Rogue one-pouch-plus-direct-coins behavior, then recover exact
    ordinary success endpoints across levels; do not promote either family while the cutoff Wiki
    reports those rates as `Unknown`.

    Equip every ordinary, offensive and defensive oak, willow and maple blackjack and prove that
    all nine have identical Lure/Knock-Out behavior. Confirm the 80/240 Knock-Out endpoints, 10 XP
    on success and direct 40/50 coins instead of pouches for knocked-out targets. Recover the exact
    lure dialogue/tick sequence, observer search and line-of-sight rule, including the message
    `Perhaps I shouldn't do this here, I think another bandit will see me.` Confirm combat rejects
    Knock-Out with `You can't knock-out the bandit during combat.` Measure knockout duration and
    the exact two guaranteed-pickpocket eligibility window. On failure, recover aggression chance,
    first/second-hit timing and the ordering by which any Pickpocket result or successful Knock-Out
    interrupts the second hit. Repeat with a full inventory, zero/28/diary-raised pouch counts,
    Gloves of silence, dodgy necklace, Shadow Veil and Rogue equipment before promotion.

    At level 53, test Kharidian Bandit Camp NPCs 690 and 695. Confirm 79.4 XP, 50/240 endpoints,
    three-damage failures and the exact seven-weight output partition: a fixed 30-coin Desert
    Bandit pouch at 5/7, antipoison(1) at 1/7 and a lockpick at 1/7. Verify pouch `Open` and
    `Open-all`, hard-diary and cape modifier endpoints, one-pouch-plus-30-direct-coins Rogue
    behavior, doubled antipoison and lockpick rewards without extra XP, and atomic full-inventory
    rollback. Confirm similarly named Pollnivneach/The Feud bandits are not handled by this
    ordinary-pickpocket definition, and separately record the combat-AI aggression response to
    Saradomin and Zamorak equipment.

    At level 55, test all five registered Knights of Ardougne and six Knights of Varlamore. Confirm
    84.3 XP, one 50-coin Knight pouch, 50/240 endpoints, three-damage failures and a six-second stun
    represented by ten action-block and eleven movement-block cycles.
    Confirm medium Ardougne Diary completion produces 55/264 endpoints for Ardougne Knights but not
    Varlamore Knights, while the hard diary produces 55/264 everywhere and the Thieving cape stacks
    to 60/290. Verify `Open`, `Open-all`, the mixed-pouch ceiling and Rogue-equipment direct coins.
    Confirm the Song of the Elves and Children of the Sun access/morph boundaries prevent their
    respective regional identities from being reached early rather than adding a second action-time
    quest check.

    At level 65, test Yanille Watchman 5420. Confirm 137.5 XP, one 60-coin Watchman pouch and one
    bread on every success, 15/160 base endpoints, three-damage failures and the shared stun.
    Confirm Gloves of silence produce 15/168, the hard Ardougne Diary produces 16/176 and a
    Thieving cape plus diary produces 17/193. Verify both pouch operations, the mixed-pouch ceiling,
    one-pouch-plus-60-direct-coins Rogue behavior, doubled bread, and atomic rollback when either
    bundle output cannot fit. Resolve the coin-pouch page's broad “occasional” wording against the
    Watchman page's subject-specific Always entries and bread-based strategy.

    Before The Feud's cache morph, inspect Menaphite Thug 3549 and confirm it exposes Talk-to and
    Attack but no Pickpocket. After the morph, test 3550 and confirm its Pickpocket, Lure and
    Knock-Out operations. For the ordinary Pickpocket action, confirm level 65, 137.5 XP, one fixed
    60-coin Menaphite pouch, 50/160 base endpoints, five-damage failure and a five-second stun
    represented by eight action-block and nine movement-block cycles. Confirm Gloves produce
    52/168, the hard Ardougne Diary produces 55/176 and diary plus cape produces 60/193; verify both
    shared failure protections, pouch `Open`/`Open-all`, one pouch plus 60 direct Rogue coins,
    single XP and atomic rollback. Separately exercise blackjacking: recover the 78/240 knockout
    roll, exact Lure/Knock-Out sequence and timing, two-pickpocket window, aggression transition and
    full-inventory behavior, and confirm Gloves, dodgy necklaces and Shadow Veil do not modify it.

    At level 70, test all eight registered East/West Ardougne Paladins. Confirm 131.8 XP, one
    80-coin Paladin pouch plus two chaos runes on every success, 40/170 base endpoints and
    three-damage failures. Confirm Gloves produce 42/178, the medium diary produces 44/187 and cape
    plus diary produces 48/205. Verify `Open`, `Open-all`, the shared cap, one-pouch-plus-80-direct-
    coins and four-rune Rogue doubling, and atomic rollback. Confirm Song of the Elves owns West
    Ardougne access. Automated coverage pins the independent 1/1,000 hard-clue roll, 1/950 Hard-
    Combat-Achievement rate, all 185 active initial steps, ownership, X Marks boxes/capacity,
    full-inventory blocking and Rogue non-doubling. Retain only live distribution and exact client
    presentation here.

    At level 75, test ordinary Gnome NPCs 6094, 6095 and 6096. Confirm 133.5 XP, 43/175 base
    endpoints, one-damage failures and the five-second shared stun. Observe the exact 128-weight
    table: 2–4 arrow shafts at 56/128, one fixed 300-coin Gnome pouch at 30/128, one swamp toad at
    24/128, one gold ore at 8/128, one earth rune at 5/128, one king worm at 3/128 and one fire orb
    at 2/128. Confirm Gloves produce 45/183, hard Ardougne Diary completion produces 47/192, and
    hard diary plus cape produces 51/211; the medium diary must not affect these non-Ardougne
    targets. Verify pouch `Open`/`Open-all`, one-pouch-plus-300-direct-coins Rogue behavior, and
    that a single inclusive 2–4 shaft roll is doubled to 4–8 rather than rerolled. Confirm whole-
    reward rollback when the selected item cannot fit. Check battlefield IDs 5968–5970 and confirm
    they expose combat but no Pickpocket operation in the matching client. Automated coverage pins
    the separate 1/150 medium-clue roll, 1/142 Medium-Combat-Achievement rate, all 165 active initial
    steps, X Marks boxes/capacity, ownership, loose-clue full-inventory blocking, existing-box
    stacking and Rogue non-doubling. Retain live distribution and exact client presentation.

    At level 80, test Hero NPCs 3295, 11934 and 11935. Confirm 163.3 XP, 39/160 base endpoints,
    three-damage failures, and a six-second stun represented by ten action-block and eleven
    movement-block cycles. Observe the exact 128-weight table: a Hero pouch at 105/128, two death
    runes at 8/128, wine at 6/128, one blood rune at 5/128, one diamond at 1/128, one fire orb at
    2/128 and one gold ore at 1/128. Confirm Gloves produce 40/168, the medium Ardougne Diary
    produces 42/176, and diary plus cape produces 46/193. Open Hero pouches singly and in bulk and
    prove each independently yields an inclusive 200–300 coins. With Rogue equipment, confirm the
    pouch branch gives one pouch plus exactly 200 direct coins rather than repeating its variable
    opening roll; every non-pouch branch doubles and XP remains 163.3. Verify atomic rollback with
    insufficient space. Automated coverage pins the separate 1/1,400 elite-clue roll, 1/1,330
    Elite-Combat-Achievement rate, all 135 active initial steps, ownership, X Marks boxes/capacity,
    inventory blocking and Rogue non-doubling. Retain live distribution and client presentation.

    After completing The Fremennik Trials, probe Agnar 3937, Freidir 3938, Borrokar 3939, Lanzig
    3940, Pontak 3941, Lensa 3943, Jennella 3944, Sassilik 3945 and Inga 3946. Confirm level 45,
    65 XP, one 40-coin Fremennik pouch, two-damage failure and five-second stun, then gather enough
    controlled attempts at several Thieving levels to recover the exact success endpoints. Repeat
    immediately before and after quest completion to pin the exact quest-state boundary. Check NPC
    3942 separately because cache exposes a related identity but the dated page omits it. Do not
    promote or implement this family from the Wiki's explicitly `Unknown` success-rate field.

    After Death to the Dorgeshuun, probe Cave goblin NPCs 2268–2284 and confirm that all listed
    identities except Talk-to-only 2301 expose Pickpocket. Confirm level 36, 40 XP, one-damage
    failure, five-second stun and the nearby-guard attack response. Force all twenty equal shares:
    six Dorgeshuun foods at 1/20 each, a 10–50-coin pouch at 7/20, and bullseye lantern, wire,
    1–4 iron ore, oil lantern, swamp tar, tinderbox and unlit torch at 1/20 each. Verify one wire
    before and two after the elite Lumbridge Diary. Recover the exact success endpoints across
    levels and the guard alert radius/targeting; do not promote the family while the dated Wiki
    still reports that rate as `Unknown`.

    At level 85, test Lletya Elf forms 5297, 5299, 5300 and 9015. Confirm 353.3 XP, 6/100 base
    endpoints, five-damage failure and a six-second stun represented by ten action-block and eleven
    movement-block cycles. Force the exact 128-weight table: Elf pouch at 105/128, two death runes
    at 8/128, three nature runes at 5/128, wine at 6/128, diamond at 1/128, fire orb at 2/128 and
    gold ore at 1/128. Confirm Gloves produce 6/105, medium Ardougne Diary has no effect, hard diary
    produces 6/110 and hard diary plus cape produces 6/121. Open pouches singly and in bulk for
    independent 280–350 payouts. With Rogue equipment, confirm one pouch plus one direct 280–350
    roll or doubled non-pouch loot, single XP and atomic rollback. Confirm cache forms 8914, 8927
    and 8928 have no Pickpocket operation. After Song of the Elves, verify all 49 Prifddinas IDs
    9054–9090 and 9106–9117 expose the shared ordinary contract, while Amrod 9053 and 9101–9105 do
    not expose Pickpocket. Force the 1/35 crystal-shard 23962 and 1/1,024 enhanced-teleport-seed
    23959 rolls before the ordinary table, including a simultaneous dual-tertiary hit. With one
    through five Rogue pieces, determine whether shard, seed and main output share one doubling
    decision or roll independently; confirm full Rogue guarantees two of each tertiary. Test zero,
    one and two free slots with stackable and non-stackable main outcomes: confirm the documented
    shard ground drop when only one slot must hold the main result, recover the seed destination,
    and determine whether either tertiary commits if the main result cannot fit. Capture exact
    ground ownership, lifetime and packet/order evidence before promoting the family.

    At level 82, test every Darkmeyer Vyre NPC from 9685 through 9714. Confirm 306.9 XP, 8/128
    base endpoints, five-damage failure and a six-second stun represented by ten action-block and
    eleven movement-block cycles. Explicitly compare this subject-page six-second record with the
    cutoff broad Thieving table's conflicting three-second entry and capture the matching-client
    release tick. Force the exact 132-weight table: Vyre pouch at 109/132, two
    death runes at 8/132, four blood runes at 2/132, one blood pint at 6/132, one uncut ruby at
    5/132, one diamond at 1/132 and one cooked mystery meat at 1/132. Confirm Gloves produce
    8/134, medium Ardougne Diary has no effect, hard diary produces 8/140 and hard diary plus cape
    produces 8/154. Open pouches singly and in bulk for independent 230–315 payouts. With Rogue
    equipment, confirm one pouch plus exactly 230 direct coins or doubled non-pouch loot, single XP
    and atomic rollback. The separate 1/5,000 blood-shard tertiary is integrated with hit/miss,
    full-Rogue doubling and private owner-floor routing when the complete shard quantity cannot fit.
    On the matching client, statistically confirm the distribution; with one through five Rogue
    pieces, observe whether the shard and ordinary output share one partial-set doubling decision;
    capture the exact floor lifetime, visibility and packet/presentation order. Do not treat the
    Wiki's unspecified internal RNG-call position as an observable gameplay contract. Separately
    verify Darkmeyer access and Vyrewatch Sentinel hostility/full-vyre-noble-clothing behavior as
    world and combat systems rather than pickpocket action gates.

    Force ordinary failures and capture the exact NPC shout, one damage, sequence 1054, spotanim
    245, action and movement release ticks, facing, sound and packet order. Repeat with Shadow Veil,
    a fresh dodgy necklace, both protections, Gloves of silence, hard diary, medium diary on
    Ardougne and Varlamore Knights plus other NPCs inside and outside Ardougne, and a Thieving cape.
    Verify veil protection suppresses the shout and rolls
    before the necklace. Record all ten necklace charge transitions and last-charge crumble. Probe
    historical NPC 8858 to resolve the Wiki/cache operation conflict. For every implemented family,
    repeat with one through five Rogue pieces and verify 15/30/45/60/100% reward doubling without
    doubled XP. Confirm that a doubled pouch yields one pouch plus 3, 9, 18, 30 or 50 direct coins, a
    Farmer potato seed yields two seeds, four Rogue pieces plus Gloves of silence retain a 60%
    reward chance, and an inventory unable to fit every pouch, direct-coin and bundled-item output
    changes nothing. Confirm supported fixed direct-coin values now include 3, 9, 18, 30, 50, 60
    and 80.
    Capture the exact Farmer rare-reward/full-inventory behavior. This case remains pending until
    observed through that workspace client.

102. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, connect to the
    cache-233 rsmod server with a members account carrying and then wearing Fremennik sea boots
    1–4. Exercise both `Teleport` routes and confirm the standard animation, graphic and sound, the
    four-tick movement boundary, and the exact Rellekka market landing at 2643,3675. For each UTC
    day, confirm shared limits of 1, 3, 5 and unlimited uses for Easy, Medium, Hard and Elite boots;
    switch between tiers mid-day to prove that changing boots cannot reset the shared count. Log
    out and reconnect before exhaustion and after exhaustion to prove persistence, then observe a
    UTC-day rollover and record any client message or state refresh. Repeat as a non-member, while
    Tele Blocked, and at Wilderness levels 20 and 21; capture exact rejection wording and prove
    rejected attempts consume no quota. Record animation, graphic, sound, message, movement,
    var/state and packet order for accepted and rejected paths. Do not open or use any RuneLite
    outside the workspace checkout.
103. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, connect a
    members account to the cache-233 server at Fenkenstrain's Mausoleum bridge. Before unlocking
    the experiment-cave passage, confirm neither bank-side nor island-side repair is offered. After
    the cave unlock, approach from the island and verify only that side exposes `Build`. At current
    Construction levels 58 and 59, with and without a hammer, and with 5/6 teak planks and 27/28
    nails, capture exact rejection priority and wording and prove no partial consumption. Repeat at
    a boosted visible 59 and with a crystal saw but no visible boost to confirm ordinary boosts work
    while the crystal saw does not.

    Build the first side with six teak planks and 28 nails. Capture the exact location ID, square,
    shape, angle, build animation, cheer, collapse timing, messages, 550 XP award and private scenery
    transition. Relog and cross a build-area boundary to prove that only this account retains the
    intermediate stage. Build the mainland side with another six planks and 28 nails, including a
    mixed-nail inventory, and capture the second 550 XP award and both repaired location overlays.
    Repeat both builds using a plank sack, including inventory-full and partially filled sack cases;
    record exact withdrawal order and capacity behavior before adding that unsupported path.

    At current Agility 68 and 69, cross in both directions. Confirm zero Agility XP, exact approach
    and landing squares, long-jump sequence, facing, movement delays, lock duration and packet order.
    Before repair, click the same surface tile twice and confirm no cinematic, then click two distinct
    consecutive surface tiles and capture the complete mausoleum camera movement, look-at target,
    duration, reset and interruption behavior. Compare observed endpoint coordinates, overlay angles,
    animation/camera ticks and all messages with the inferred production constants. Do not promote
    those fields or plank-sack support until this workspace-client capture is attached. Do not open
    or use any RuneLite outside the workspace checkout.

104. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, connect to the
    locked 1 October 2025 cache-233.4 server and reach Baxtorian Falls with four accounts/states:
    neither prerequisite, Waterfall Quest only, medium Kandarin reward only through controlled
    state setup, and both prerequisites. Confirm the first coil is anchored at 2511,3477 and morphs
    from no operation to `Take` only in the valid members state. Capture its exact shape, angle,
    approach square, facing, animation, movement type/tick, message/sound and packet order. Taking
    it must add and consume no inventory item and must land on the medium-clue plateau at 2512,3467,
    not at the dungeon ledge.

    At the dead tree, prove that the second Poll 85 wrapper is absent/non-interactive in this
    October snapshot and that the ordinary carried-rope route is still required. This is the
    documented 17 September hotfix state, not an omission to silently repair. For the future-cache
    upgrade rehearsal, repeat against a cache captured on or after the 17 December 2025 hotfix,
    recover the second wrapper's effective plane, shape, angle, approach and exact ledge arrival,
    then add revision-gated placement/dispatch without changing the frozen 233.4 contract. Do not
    open or use any RuneLite outside the workspace checkout.
105. Using only the workspace cache-233 RuneLite client, talk to NPC 1504 in Feldip Hills and NPC
    1503 in the Rellekka Hunter area on a fresh account. Confirm each exact opening line and option
    order, then select `Can you give me some hunting traps?`. Record the offer, Hunter kit 11159
    item box/zoom, inventory-update timing, `Thanks! I will put this to good use.`, expressions and
    packet order. Confirm Feldip asks whether anything else is needed and returns to its menu while
    Rellekka ends immediately after the thanks.

    Repeat at the same expert and then the other expert, before and after logout, banking, opening
    and destroying the kit. Confirm the shared refusal, Yanille direction and return to the other
    choices, and confirm no second item is awarded. Repeat while already holding a Lunar-created
    kit and with 28 occupied inventory slots; capture whether the former still receives the
    one-time grant and the exact latter feedback, while proving a rejected full-inventory attempt
    does not consume the entitlement. Traverse every implemented legacy tutorial branch and the
    level-99 Skillcape route, comparing text, pagination, menu loops and expressions to the two
    pinned transcripts. Buy with 98,999 and 99,000 coins, with zero/one/two free slots, as a first
    and second skill mastery, and while already owning the cape or hood. Confirm the exact cape,
    hood, cache-jingle-231 playback/mixing and packet behavior; specifically record whether a
    missing hood can be reclaimed from this expert and how already-owned untrimmed capes transition
    when the second skill reaches 99. Do not open or use any RuneLite outside the workspace
    checkout.
106. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, connect to the
    cache-233 server with both Hunter cape 9948 and Hunter cape(t) 9949. In inventory, confirm
    `Wear`, `Teleport`, `Drop`; while worn, confirm `Boost`, `Hunter Guild`, `Carnivorous
    Chinchompas`, `Black Chinchompas` in exactly that order. Boost at current Hunter 99 and 100 and
    confirm a visible cap of base plus one, including restoration timing and interaction with other
    boosts.

    Before enhancement, open `Hunter Areas` from each inventory cape and confirm only the two
    chinchompa destinations and Cancel. Use both destinations in mixed order five times, relog and
    swap cape variants between uses, then confirm the exact exhaustion message and no sixth move.
    Observe the 00:00 UTC rollover. For the Wilderness destination, capture every warning screen,
    remembered warning toggle, confirmation choice and cancellation path before production adds a
    guessed modal. Record exact destination squares, facing, animation, graphic, sound, four-tick
    movement boundary and packet order.

    Carry and then wear each variant while talking to Guildmaster Apatura 13120. Compare every line,
    expression, option and continuation to transcript revision 14689838; decline once, accept once,
    relog, bank/remove the cape and prove the unlock is permanent and the Hunter Guild route is
    unlimited without changing the five-use counter. Also select the worn `Hunter Guild` operation
    before unlocking and capture its exact response. Repeat as a non-member, while Tele Blocked and
    at Wilderness levels 20 and 21, proving every rejection is atomic. Exercise Apatura on a fresh
    account and at 50/100/150 rumours to recover the full NPC progression and determine the exact
    production bit allocation within `skillcape_perks` varp 1312. Do not open or use any RuneLite
    outside the workspace checkout.
107. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, prepare one
    account at its first level 98 and another with one existing level 99 and a second skill at 98.
    Distribute ordinary untrimmed capes from all 23 cache-era skills across inventory, worn cape
    slot, bank, POH cape rack, death storage, Forestry clothes pouch and Huntsman's kit. Confirm the
    first level 99 and every sub-99 advance leave all capes unchanged. On the second level 99,
    confirm inventory, worn and banked capes transform in place, the equipped model changes in the
    same update, item variables survive, and the transformation cannot be reversed. Repeat on a
    third level 99 while carrying newly obtained untrimmed capes.

    Before the second 99, drop one untrimmed cape and leave a second on the ground. After the level
    arrives, pick one up normally and recover the other with Telekinetic Grab; confirm the
    implemented ordinary pickup trims and the telegrabbed cape remains untrimmed, including exact
    pickup/cast inventory and ground-object packet order. Recover each special-storage family and
    compare it to revision 14994254's preservation rules. Capture exact inventory, equipment,
    bank, appearance and message packet order at the level-up boundary. Do not promote the POH,
    death-storage, Forestry or Huntsman's-kit paths until their storage hooks are implemented and
    observed. Do not open or use any RuneLite outside the workspace checkout.
108. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test both
    ordinary variants of every cache-era production Skillcape at visible level 99. Equip each cape
    from inventory and confirm the corresponding skill immediately becomes 100. For all capes
    except Agility, activate the cache-exposed worn `Boost` operation after returning to 99 and
    confirm the same result. Confirm cache objects 9771 and 9772 expose no worn operation-one menu
    entry, while equipping either still raises Agility to 100; capture this cutoff cache/Wiki
    discrepancy without adding a synthetic client option.

    For an ordinary non-Hitpoints/non-Prayer cape, activate at 99, retry at 100, allow natural stat
    restoration to 99 and activate again. Capture the exact first restoration cycle, menu behavior,
    stat-update packet ordering and whether any feedback appears at the cap. Repeat while the skill
    is drained below 99 to establish the exact wording of revision 14994254's `dropped back to 99`
    rule.

    For Hitpoints and Prayer separately, activate at 99, return the visible level to 99 early, and
    retry at 59 and 60 seconds. Confirm the channels are independent, a blocked attempt does not
    extend the deadline, and equip swapping cannot bypass it. Relog and restart the client during
    each cooldown to prove the permanent server deadline survives and capture any cooldown message
    or silent behavior. Do not open or use any RuneLite outside the workspace checkout.
109. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, connect one
    cache-233.4 account with each ordinary untrimmed and trimmed Skillcape. Keep the cape only in
    inventory and click the `Skill Cape` emote, then wear it and repeat. Confirm inventory-only use
    gives the implemented worn-cape rejection and that every worn variant plays the animation and
    graphic for its own skill. Capture exact animation length, graphic slot/height/delay, appearance
    rebuild, interface-close behavior, movement/action interruption and packet/audio order for all
    23 pairs. Repeat during recent PvM and PvP combat and confirm the exact rejection with no visual
    effect.

    Board an active Fishing Trawler and retry with representative ordinary capes. Revision 14994254
    says Skillcape emotes are forbidden aboard; recover the authoritative activity/area state, exact
    feedback and boundary between the waiting boat, active voyage, sinking/sunk states and reward
    area. Do not promote a Trawler gate from interface presence or an unexplained varp value. Also
    confirm the existing Music cape exception remains special and does not allow any ordinary cape
    emote while unworn. Do not open or use any RuneLite outside the workspace checkout.
110. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, smelt gold at an
    ordinary furnace and cast Superheat Item while wearing, in turn, goldsmith gauntlets, Smithing
    cape 9795, Smithing cape(t) 9796 and the production max cape. Confirm each grants exactly 56.2
    Smithing XP per bar, then repeat with every item only in inventory and with unrelated back-slot
    equipment to confirm the 22.5-XP fallback. Capture XP, inventory and equipment packet ordering.

    Repeat at the Blast Furnace once that activity exists and prove whether the perk is sampled at
    ore deposit, processing or bar collection. Separately exercise closed and open coal bags at 27
    coal, equip each Smithing cape to reach 36, unequip at counts 28–36, and test Fill, Check, Empty,
    bank Fill/Empty, automatic mined-coal insertion, Destroy and alchemy. The production core now
    supplies those interaction/mining hooks; capture exact success/check/full/empty/member feedback,
    deposit-box prohibition, bank-placeholder and equipped-bank presentation, one-bag acquisition
    and ownership rules, and packet order and audiovisual timing. Low and High Level Alchemy now
    deliberately bypass the bags' cache no-alchemy flag, destroy either variant even when full and
    clear stored state; verify the exact warning behavior plus rune, coin, XP and presentation order.
    Confirm a bag holding 28–36 is preserved after cape removal but cannot accept more coal until it
    falls below 27. Do not open or use any RuneLite outside the workspace checkout.
111. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, compare the same
    tree and axe at identical visible Woodcutting levels immediately inside and outside the final
    pre-cutoff Woodcutting Guild polygon. Use a fixed, statistically meaningful sample or temporary
    deterministic instrumentation to prove the effective success calculation gains exactly seven
    levels inside, while a base level below a tree's requirement still cannot start that action.

    Repeat with visible potion and special-attack boosts to confirm they stack with the invisible
    guild modifier. Check representative interior points, every edge around the two concave notches,
    the upper floors and the similarly aligned underground dungeon; compare the observed boundary
    to revision 14975699 and the packed `a24_54`/`a25_54` area groups. Confirm the Forestry group
    bonus is not active inside the guild.

    Outside the guild, validate the implemented final pre-cutoff Forestry group boost with one
    through at least eleven players. Confirm +1 per valid active player chopping the exact same
    eligible static timed tree, including the subject, capped at +10 and available on a free-to-play
    account. Confirm stopped, dead, logging-out, travelling/out-of-range and different-tree players,
    plus non-Woodcutting loc operations, are not counted. Repeat on normal and Farming-grown trees
    for zero and with visible boosts to confirm stacking without unlocking higher-level trees.

    Capture exact success timing, Forestry group-icon/update cadence, XP-drop and feedback behavior
    and confirm neither invisible modifier changes the client-visible Woodcutting level. Do not open
    or use any RuneLite outside the workspace checkout.
112. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, force or sample
    regular bird nests from several eligible trees on a members world. Confirm the 1/256 baseline,
    1/230 worn Woodcutting-cape/trimmed-cape/max-cape rate, no inventory log, no Woodcutting XP drop,
    exact red `A bird's nest falls out of the tree.` message, RuneLite notification, private floor
    ownership, one-minute public reveal and two-minute total despawn. Repeat on free-to-play,
    redwood and blisterwood for zero regular nests, while retaining clue-nest eligibility where the
    dated sources require it.

    Sample the red/blue/green egg, ring and January-2019 seed-nest identities to confirm the exact
    3/32/65 distribution, then wear a strung rabbit foot and confirm 3/32/60 over 95 slots without
    changing the overall nest roll. Compare the integrated beginner-through-elite clue-nest rolls
    on every integrated tree family. Confirm each tier rolls independently from its documented base chance and
    visible Woodcutting level, easy-and-higher tiers require members, a same-tier nest/scroll/
    bottle/geode in inventory or bank skips only that tier, completed Combat Achievement tiers
    reduce their matching denominator by 5%, and an imbued ring of wealth doubles rates only in the
    Wilderness. Confirm the Woodcutting cape has no effect, Arthur's vessel-clue opt-out suppresses
    every roll without consuming randomness, and successful clues remain tertiary to the log, XP
    and `CutLogs` event.

    Implement the Sulliuscep activity before testing its documented 343,000 clue base; it is not in
    the current ordinary-tree catalogue and must not be represented as a generic log tree.

    Complete X Marks the Spot and confirm each successful tier roll instead drops the corresponding
    cache-233 scroll box plus an empty bird nest on one adjacent tile. Confirm the empty nest's
    two-minute lifetime, the scroll box's three-minute lifetime, the initial capacity of two and
    minor/major/mimic scroll-case increases through five. At capacity, confirm the exact tiered
    sneaking-suspicion warning appears only after a successful clue roll and no floor objects appear.
    Confirm ordinary clues, nests, bottles, geodes and boxes in inventory/bank count toward the
    matching tier, while medium keys/caskets and juggled ground clues do not. Death's Office storage,
    opening a legacy clue nest after the quest and opening/claiming scroll boxes remain unimplemented.

    Exercise each Twitcher's-glove preference and establish its
    exact approximately-20% selection algorithm before implementation. Record the complete adjacent
    tile distribution to replace the currently sourced-but-unquantified west-bias weighting. Capture
    the ordinary chirp, the rare 1/250 Armadyl-crossbow special sound, packet/event ordering and tree
    depletion ordering. Do not open or use any RuneLite outside the workspace checkout.
113. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, compare every
    cache-233 dragon, 3rd age, Infernal and Crystal axe identity in inventory and while worn. Confirm
    members gating, levels 61/71, the Song of the Elves requirement for both Crystal states, exact
    animations, dragon-equivalent rates for every non-active-Crystal form and active Crystal's 15%
    improvement over rune. Put competing axes in both locations and confirm active Crystal wins,
    every dragon-equivalent special axe wins over rune, and iron wins over bronze at level one.

    Force Infernal burns for every ordinary log, including blisterwood and redwood, and confirm a
    1/3 replacement, half normal Firemaking XP without a Firemaking-level gate, Pyromancer's 2.5%
    modifier, one charge only when a log burns, and an exact 5,000th-use transition. Confirm bruma
    roots, sulliuscep mushrooms and celastrus bark are never consumed, and infected roots consume a
    charge only for a burned log while never burning demonic tears. Repeat Crystal success, failure,
    bird-nest replacement and final-charge boundaries to prove one charge per obtained log, 10,000
    initial and 20,000 maximum uses, and inactive dragon-rate fallback. Independently and together,
    sample equipped elven/celestial signets at 1/10 and Ghommal's lucky penny at 1/20.

    Compare the integrated Infernal three-tick creation, both full recharge inputs, Check
    percentage, original/Reloaded ornament application, Revert and Change-Style at partial-charge
    and full-inventory boundaries. Establish the exact percentage sentence and rounding below 1%.
    Compare integrated Crystal shard charging, Check and Revert. At bowl 36552 and learned Reese
    9134/Conwenna 9136, compare the integrated crystal-axe route at boosted/unboosted 76/76, 120/180
    shard boundaries, multi-set inventory batches, three-tick completion, 6,000/6,000 player XP,
    assisted zero XP and 10,000 initial charges. Record the dedicated full crystal-singing interface,
    every other product and Reese's Talk-to unlock/NPC-state behavior. Compare the integrated bank
    item-options toggle and crystal-axe Configure-Charges preference at 0, 1, 10,000, 20,000 and
    out-of-range input; capture its exact dialogue and feedback, then establish automatic withdrawal
    resource selection, partial-resource and insufficient-resource behavior for later implementation.
    Capture exact messages, confirmations, sounds, animations, item variables,
    final-charge transforms, death and trade behavior, plus inventory/equipment/XP packet ordering.
    Activate Lumber Up from all twelve forms and confirm 100% energy, +3 Woodcutting, normal
    restoration/Preserve behavior, the shared phrase and family-coloured graphic. Do not open or use
    any RuneLite outside the workspace checkout.
114. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, kill a
    statistically meaningful sample of each final pre-cutoff Chicken identity: ordinary
    1173/1174/9488/10494–10497, Miscellania 3661/3662 and Falador Farm 2804–2806. Confirm every kill
    gives one bones and one raw chicken, then establish the 64/128 five-feather, 32/128
    fifteen-feather and 32/128 nothing distribution. Verify all ordinary results are initially
    private to the credited player, reveal and expire on the production schedule, and preserve the
    same tile/nearby-tile placement behavior under occupied ground stacks and blocked coordinates.

    Carry exact clue 2837 for `In a town where everyone has perfect vision...` on a members world
    and confirm any registered Chicken always adds key 2838 independently of the feather roll. Try
    another medium clue, no clue and free-to-play state for no key. Continue the real clue through
    key use to establish disappearance, step progression and duplicate-key behavior; those clue-step
    interactions are outside the current drop-only implementation.

    Force or sample the independent 1/300 beginner roll before X Marks the Spot. Confirm an existing
    beginner clue/nest/bottle/geode in inventory or bank suppresses the roll, then time an awarded
    private clue scroll for the sourced one-hour lifetime. Complete X Marks and repeat for scroll box
    24361 with its three-minute lifetime and normal stack behavior. Exercise capacity two, every
    minor/major/Mimic case increase through five, each same-tier holder type, full inventory, bank,
    juggled floor clues and Death's Office storage. At capacity, prove the sneaking-suspicion message
    appears only after a successful clue roll and that no object is created. Capture exact object
    placement, reveal timing, message color/channel, packet order and kill-credit behavior for solo,
    tied, grouped and Ironman kills. Do not open or use any RuneLite outside the workspace checkout.
115. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test every
    final pre-cutoff adult Cow identity 2790/2791/2793/2795/5842 and Cow calf identity
    2792/2794/2801. Confirm combat level, Hitpoints, one maximum crush hit, four-tick attacks and
    poison/venom immunity, then kill a statistically meaningful sample of each. Every kill must put
    one cowhide above one raw beef above one bones and independently award a beginner clue at
    1/128. Confirm the 45-tick (27-second) respawn for each identity under normal population
    conditions and record any world-population scaling.

    Before X Marks the Spot, verify same-tier inventory/bank ownership suppresses the roll and an
    awarded clue remains private for one hour. After completion, verify scroll box 24361, its
    three-minute lifetime, capacities two through five and the exact successful capped-roll warning.
    Exercise solo, tied, grouped and Ironman credit plus occupied or blocked ground stacks; record
    exact ground-pile ordering, placement, reveal/expiry timing and packet order.

    Use empty bucket 1925 and unrelated items on adults; use empty bucket, bucket of milk 1927 and
    unrelated items on calves; confirm every exact recorded response. On Zanaris cow 5842, compare
    the complete Talk-to monologue and Nuff's certificate 9025 exchange, including faces,
    animations, pause interfaces and the source-preserved `ink-blots ?` spacing. Sample ambient Moo,
    grazing animation and area sound cadence on adults and calves. Separately inspect dairy-cow and
    Cold War variants without treating them as this production drop family. Do not open or use any
    RuneLite outside the workspace checkout.
116. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test the
    production level-five Frog 8702. Confirm combat level five, Attack/Strength/Defence/Hitpoints/
    Ranged/Magic values 5/4/3/8/1/1, one maximum crush hit, four-tick attacks, 50-tick
    (30-second) respawn and 40% Earth weakness through both damage sampling and Monster Examine.

    Kill a statistically meaningful sample and confirm one bones 526 on every kill plus an
    independent beginner clue 23182 at 1/90. Before X Marks the Spot, verify same-tier inventory
    and bank ownership suppresses the roll and an awarded clue remains private for one hour. After
    completion, verify scroll box 24361, its three-minute lifetime, capacities two through five and
    the exact successful capped-roll warning. Exercise solo, tied, grouped and Ironman credit plus
    occupied or blocked ground stacks; capture exact placement, reveal/expiry timing, packet order,
    combat animation/sound and world-population respawn scaling. Keep Big frog and Giant frog out of
    this case: their distinct tables are covered by cases 119 and 120. Do not open or use any
    RuneLite outside the workspace checkout.
117. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test loaded
    Lumbridge Mugger 513 at 3181,3216. Confirm combat level six, stats 5/5/5/8/1/1, one maximum
    crush hit, four-tick attacks, aggression, ordinary poison/venom behavior and production respawn.
    Kill a statistically meaningful members sample and establish guaranteed bones plus the exact
    128-share main distribution: bronze bolts 2–12 at 27; mind rune 9 at 3; water rune 6 at 2;
    earth rune 5 at 2; standard herbs at 13; coins 5/15/25 at 12/3/1; rope at 40; nothing at 13;
    knife at 1; fishing bait at 6; copper ore at 2; bronze med helm at 2; and cabbage at 1. Repeat
    in a free world to prove selected bronze-bolt entries give no main drop and herb entries give
    ten coins.

    Independently establish the 1/80 beginner and members-only 1/128 Easy clue rates. For Easy
    clues before X Marks the Spot, sample initial objects across cryptic, emote, map and music forms,
    including cutoff ID 30928, and confirm repeats remain possible. After X Marks, confirm scroll
    box 24362 replaces the scroll without a hidden extra selection. Exercise inventory/bank
    ownership, capacity two through five, capped warning, full inventory, occupied/blocked piles,
    reveal/expiry timing and solo/tied/grouped/Ironman kill credit. Verify Port Sarim jail,
    Piscarilius/Vinery and Varlamore Mugger identities separately without treating them as the
    loaded 513 contract. Capture exact animations, sounds, aggression, messages and packet order.
    Do not open or use any RuneLite outside the workspace checkout.
118. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test all five
    Ram identities 1261–1265. Confirm combat level two, stats 1/1/1/8/1/1, one maximum crush hit,
    four-tick attacks, non-aggression, ordinary poison/venom susceptibility, 45-tick (27-second)
    respawns and 20% Fire weakness through damage sampling and Monster Examine. Use shears on every
    form and verify `That one looks a little too violent to shear...`, no item consumption and no
    NPC transformation.

    Kill each identity before the quest, in free worlds, during Rag and Bone Man I collection state
    two, after handing in only the polished ram skull, and after quest completion. Confirm bones 526
    always drop and raw ram skull 7818 additionally drops only for a member while still outstanding.
    Independently place raw 7818, vinegar 7819 and polished 7820 in inventory and bank, confirming
    each suppresses another skull. Hand in the polished skull, remove all three lifecycle objects,
    relog and restart the server, then prove the persistent hand-in bit still suppresses it while the
    other seven quest bones remain independently outstanding. Empty a bone from vinegar before
    hand-in and prove recollection is allowed. Capture exact ground placement, ownership, reveal and
    expiry timing, death/respawn animation, sounds, packets and quest-dialogue order. Do not infer
    completion of the wider Rag and Bone Man I flow from this NPC contract, and do not open or use
    any RuneLite outside the workspace checkout.
119. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test loaded
    level-ten Big frog 8701 throughout the Lumbridge Swamp. Confirm stats 8/6/4/18/1/1, two maximum
    crush hit, four-tick attacks, non-aggression, ordinary poison/venom susceptibility, 50-tick
    (30-second) respawn and 25% Earth weakness through damage sampling and Monster Examine.

    Kill a statistically meaningful sample and confirm guaranteed bones plus an independent 1/70
    beginner clue. Before X Marks the Spot, verify same-tier inventory/bank ownership suppression
    and one-hour private clue lifetime; after completion, verify beginner scroll-box replacement,
    capacity two through five, exact capped warning and three-minute lifetime. During Rag and Bone
    Man I collection state two, confirm a member additionally receives raw Big frog leg 7908 on
    every kill while it is outstanding, with bones/leg/clue order preserved on a simultaneous clue
    success. Confirm free worlds and every non-collection quest state suppress only the leg. Test raw
    7908, vinegar 7909 and polished 7910 separately in inventory and bank, then hand in only this
    sixth bone, relog and restart to prove persistent suppression without affecting the other seven
    quest bones. Exercise ground placement, occupied/blocked piles and solo/tied/grouped/Ironman
    kill credit; capture exact animations, sounds, packets and respawn population scaling. Keep
    level-24 Big frog 478 outside this loaded identity's table. Do not open or use any RuneLite
    outside the workspace checkout.
120. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test every
    loaded level-thirteen Giant frog 8700 in the Lumbridge Swamp. Confirm combat level 13, size two,
    stats 10/8/6/23/1/1, two maximum crush hit, four-tick attacks, non-aggression, ordinary poison/
    venom susceptibility, 50-tick (30-second) respawn and 25% Earth weakness through damage
    sampling and Monster Examine.

    Kill a statistically meaningful free-world sample and confirm one big bones 532 on every kill,
    an independent beginner clue at 1/64 and no long or curved bones. Before X Marks the Spot,
    verify same-tier inventory/bank ownership suppression and the one-hour private clue lifetime;
    after completion, verify beginner scroll-box replacement, capacity two through five, the exact
    capped warning and three-minute lifetime.

    Repeat on a members world and establish the ordered rare-bone contract: long bone 10976 at
    1/400; only after that roll misses, curved bone 10977 at 1/5,000, for an effective 1/5,012.5;
    never both on one kill. Confirm both are independent of the clue roll and preserve big-bones,
    clue, rare-bone ground-pile order on simultaneous successes. Exercise occupied/blocked piles
    and solo/tied/grouped/Ironman kill credit; capture exact placement, reveal/expiry timing,
    animations, sounds, packets and population-scaled respawn. Keep level-99 Giant frog 477 outside
    this loaded identity's table. Do not open or use any RuneLite outside the workspace checkout.
121. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test loaded
    ordinary Unicorn 2837 west of Lumbridge at 3140,3209. Confirm combat level 15, size two, stats
    11/13/13/19/1/1, two maximum stab hit, four-tick attacks, non-aggression, ordinary poison and
    venom susceptibility and a 90-tick (54-second) respawn. Observe its documented low-Hitpoints
    retreat repeatedly and establish the exact threshold and route behavior before adding an
    editor parameter; verify Monster Examine shows no elemental weakness.

    Kill it in free and members worlds. Confirm bones 526 always drop, unicorn horn 237 drops only
    for members, and raw Unicorn bone 7821 additionally drops only for a member in Rag and Bone Man
    I collection state two while the second bone remains outstanding. Independently place raw
    7821, vinegar 7822 and polished 7823 in inventory and bank, then hand in only this bone, relog
    and restart to prove persistent suppression without affecting the other seven quest bones.

    Establish the independent members-only 1/35 initial ensouled unicorn head 13465. Cast Basic
    Reanimation at its origin, every edge of the 31-by-31 centered area and one tile beyond; repeat
    after pickup and player re-drop to prove the original area does not move. Bank the fresh head
    with single and bulk deposit, withdraw processed head 13466 and prove it now works only near the
    Dark Altar. Fill the bank and confirm a failed deposit preserves both the fresh identity and
    original area. Once trade, Grand Exchange, price checker and Soul Bearer systems exist, repeat
    each route and require the same expiration; those routes are not currently implemented.
    Confirm exact private/public ground timing, stack behavior for different origins, death-credit
    ownership, animations, sounds, messages and packet order. Do not open or use any RuneLite
    outside the workspace checkout.
122. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test ordinary
    Imp 5007 at all three loaded placements: 3217,3226; 3214,3281; and 3240,3307 on level zero.
    Confirm combat level two, size one, stats 1/1/1/8/1/1, zero maximum stab hit, four-tick attacks,
    non-aggression, ordinary poison/venom susceptibility, 50-tick (30-second) respawn and 10% Water
    weakness through damage sampling and Monster Examine.

    Kill a statistically meaningful sample. Confirm one fiendish ashes 25766 on every kill and the
    exact 128-share main distribution: each coloured bead at five; bronze bolts at eight; blue
    wizard hat at eight; egg/raw chicken at five each; burnt bread/burnt meat at four each;
    cabbage/bread dough at two each; bread/cooked meat at one each; hammer at eight; tinderbox at
    five; shears/bucket at four each; bucket of water at two; jug/jug of water/pot/pot of flour at
    two each; ball of wool at eight; mind talisman at seven; ashes at six; clay/cadavaberries at
    four each; grain at three; chef's hat/flyer at two each; and Apothecary potion 195 at one.

    In members and free worlds, establish that the initial ensouled imp head 13453 is independently
    members-only at 1/25 and retains the fresh-origin behavior in case 95. With at least 32 quest
    points and Imp champion 1457 not defeated, establish the members-only 1/5,000 Imp champion
    scroll 6803 and confirm repeat scrolls remain possible. Repeat below 32 quest points and in a
    free world to prove suppression. Set the Imp champion victory bit, force or statistically reach
    the same successful roll, and confirm no scroll appears and the exact dark-red message reads
    `You have a funny feeling that you would have received a Champion's scroll...`. Use the
    deterministic integration suite for all rare boundaries before relying on live sampling.

    Observe the documented 30–120-second, 25% teleport attempts to establish exact radius,
    destination collision, combat interaction, smoke animation/graphic/sound and packet order
    before implementing that behavior. Do not expect a Wilderness looting bag or God Wars
    ecumenical key from these three placements; test GWD identity 3134 separately if it is later
    loaded. Do not open or use any RuneLite outside the workspace checkout.
123. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test all six
    regular Giant rat identities loaded around Lumbridge: level-three 2856, 2859, 2860 and 2861,
    and level-six 2863 and 2864. Representative clusters include 3154,3186 through 3207,3187 for
    2856; 3192,3207 through 3197,3201 for 2859–2861; and 3200,3174 through 3233,3161 for
    2863–2864, all on level zero. Confirm size two, the exact 2/3/2/5/1/1 and 6/5/2/10/1/1 stat
    tuples, one maximum stab hit, four-tick attacks, ordinary poison and venom susceptibility,
    30-tick (18-second) respawn and no elemental weakness through Monster Examine and sampling.

    Exercise acquisition from every direction with line-of-sight obstacles, players below and
    above twice the rat's combat level, combat interruption, chase boundaries and return-to-spawn.
    Confirm the cache-backed five-tile hunt range and standard non-Wilderness aggression cutoff,
    and capture the exact first-attack timing, disengagement, animation, sound and packet order.

    Kill a statistically meaningful free-world sample for every identity. Confirm one bones 526
    and one raw rat meat 2134 on every kill, plus an independent beginner clue 23182 at 1/128.
    Before X Marks the Spot, verify same-tier inventory/bank ownership suppression and the one-hour
    private clue lifetime; after completion, verify beginner scroll-box replacement, capacity two
    through five, exact capped warning and three-minute lifetime. Repeat in members worlds during
    Rag and Bone Man I collection state two and confirm raw Giant rat bone 7824 appears on every
    kill while the first bone remains outstanding, independently of the clue and after meat in the
    ground-pile order. Test vinegar 7825 and polished 7826 separately in inventory and bank, then
    hand in only this bone, relog and restart to prove persistent suppression without affecting the
    other seven bones.

    Exercise occupied and blocked piles, solo/tied/grouped/Ironman kill credit, private reveal and
    expiry, population-scaled respawn and exact audiovisual and packet presentation. Confirm no
    looting bag appears from these non-Wilderness placements. Keep unloaded regular identities
    2857/2858/2862/9483 and Stronghold identities 2510–2512 outside this table until their own
    placements and location/quest conditions are implemented. Do not open or use any RuneLite
    outside the workspace checkout.
124. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test regular
    Rat 2854 at all sixteen loaded level-zero placements around Lumbridge: 3191,3204; 3193,3205;
    3196,3206; 3197,3204; 3222,3215; 3229,3223; 3232,3229; 3236,3222; 3205,3203;
    3205,3204; 3205,3209; 3206,3202; 3206,3204; 3206,3209; 3207,3202; and 3207,3203.
    Confirm combat level one, size one, stats 1/1/1/2/1/1, zero maximum crush hit, four-tick
    attacks, non-aggression, ordinary poison/venom susceptibility and two-tick respawn through
    Monster Examine and timing. Confirm there is no ordinary loot or bones.

    During Witch's Potion state zero, confirm no Rat's tail. During states one and two, confirm
    Rat's tail 300 appears on every kill. Keep a tail in inventory and repeat, then bank it and
    repeat, proving the documented drop trick is not suppressed by ownership. Complete the quest
    at state three and confirm tails stop. Verify that Giant rats do not supply this quest item.

    In a members world during Rag and Bone Man II collection state four, sample or force the 1/4
    independent Rat bone 7836 roll; repeat in a free world and at states three, five and six to
    prove suppression. Test raw 7836, vinegar 7837 and polished 7838 separately in inventory and
    bank. Hand in only the Rat bone, relog and restart the server, then prove bit-six suppression
    persists without altering another recorded bone. When both quests are active, force the bone
    roll and confirm the ground-pile order is tail followed by Rat bone.

    Exercise occupied and blocked piles, solo/tied/grouped/Ironman kill credit, private reveal and
    expiry, respawn timing and exact animation, sound and packet presentation. Confirm no looting
    bag appears from these non-Wilderness placements. Keep unloaded regular identity 2855 and
    Stronghold identities 2492/2513 outside this table until their placements and distinct
    conditions are implemented. Do not open or use any RuneLite outside the workspace checkout.
125. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test regular
    Spider 3019 at all seven loaded placements between Lumbridge and Draynor—3157,3225;
    3158,3225; 3163,3226; 3165,3247; 3166,3248; 3168,3245; and 3169,3243—and level-two Giant
    spider 3017 at all sixteen loaded placements from 3159,3223 through 3247,3236. Confirm both are
    size one and use four-tick Stab attacks with ordinary poison/venom susceptibility and 50% Fire
    weakness. Confirm regular Spider stats 1/1/1/2/1/1, zero maximum hit, non-aggression and a
    35-tick respawn; confirm Giant spider stats 1/1/1/5/1/1, maximum hit one, a 30-tick respawn and
    standard aggression below the production combat-level cutoff.

    Exercise Giant-spider acquisition from every direction, line-of-sight obstacles, players below
    and above twice its combat level, combat interruption, the cache-backed five-tile hunt range,
    chase and return-to-spawn. Confirm the regular Spider never initiates combat. Capture exact
    first-attack, disengagement, animation, sound and packet timing for both identities.

    Kill a statistically meaningful sample of each identity. Confirm neither produces bones nor
    any ordinary loot and both independently roll one beginner clue at 1/128. Before X Marks the
    Spot, prove same-tier inventory/bank suppression and the one-hour private clue lifetime; after
    completion, prove beginner scroll-box replacement, capacity two through five, the exact capped
    warning and three-minute lifetime. Exercise occupied and blocked piles, solo/tied/grouped/
    Ironman kill credit, private reveal and expiry, and exact ground presentation.

    Confirm no looting bag or Wilderness Slayer tertiary appears at any of these 23 non-Wilderness
    placements. Keep Spider identities 4561/2478 and Giant-spider identities 3018/2477, plus every
    Wilderness placement, outside this table until their distinct level, location and Slayer
    contracts are implemented. Do not open or use any RuneLite outside the workspace checkout.
126. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test all eight
    loaded level-two Goblin identities 3029–3036 across their 127 placements and armed level-five
    Goblin 3045 at 3167,3205,0. Confirm both families are non-aggressive, have no elemental
    weakness and respawn after 35 ticks. Confirm level-two stats 1/1/1/5/1/1, maximum Crush hit one
    and four-tick attacks; confirm level-five stats 3/1/4/12/1/1, maximum Stab hit one and six-tick
    attacks. Capture exact animations, sounds, overheads, acquisition and packet timing.

    Kill statistically meaningful free and members samples and compare against the two exact
    128-share tables in the source ledger. In free worlds prove selected members-only spear,
    javelin and Goblin-book slots remain empty, while the armed table's herb branch becomes ten
    coins. Confirm 3045 drops Green goblin mail 9057 rather than ordinary Goblin mail 288. Force
    all deterministic boundaries before relying on live frequencies, and verify bones precede the
    main reward and all independent tertiaries in the ground-pile order.

    During Rag and Bone Man I collection state two, confirm an outstanding raw Goblin skull 7812
    rolls independently at 1/4 for members. Test raw, vinegar 7813 and polished 7814 forms in both
    inventory and bank, then hand in only the fifth bone, relog and restart to prove persistent
    bit-four suppression without changing the other seven bones. Establish members-only initial
    ensouled goblin head 13447 rates of 1/35 for level two and 1/30 for level five, preserving
    fresh-origin behavior. Establish beginner clues at 1/64 and 1/80, Easy clues at 1/128 and the
    members-only Goblin champion scroll 6801 at 1/5,000 with at least 32 quest points. Confirm
    repeat scrolls before victory, ineligibility below 32 points/in free worlds, and exact dark-red
    suppression feedback after champion varbit 1455 is complete. Exercise occupied/blocked piles,
    clue ownership/capacity/lifetime, solo/tied/grouped/Ironman credit and private reveal/expiry.
    Keep other Goblin identities, Goblin Cave aggression, Wilderness Ring-of-wealth clue rates and
    post-cutoff Combat Achievement modifiers outside this contract. Do not open or use any RuneLite
    outside the workspace checkout.
127. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test all four
    loaded Man identities 3106, 3107, 3108 and 6818 and all three Woman identities 3111–3113 at
    their sixteen placements. These are 3206,3219,1; 3207,3227,0; 3216,3219,0; 3223,3240,0;
    3231,3207,0; 3235,3219,0; 3209,3215,1; 3217,3236,0; 3221,3219,0; 3236,3201,0;
    3211,3213,1; 3217,3205,0; 3235,3206,0; 3236,3203,0; 3244,3211,0; and 3229,3238,0.
    Confirm combat level two, size one, stats 1/1/1/7/1/1, maximum Crush hit one, four-tick
    attacks, non-aggression, ordinary poison/venom susceptibility, no elemental weakness and a
    25-tick respawn. Confirm Attack remains operation two and Pickpocket operation three.

    Kill statistically meaningful free and members samples and compare with the exact 128-share
    table in the source ledger. Confirm bones always come first. In free worlds prove that the
    weight-22 bronze-bolt branch is selected but empty and that the weight-23 herb branch becomes
    ten coins; in members worlds verify bronze-bolt quantities 2–12 and every standard-herb
    boundary. Establish independent beginner clues at 1/90 and members-only Easy clues at 1/128,
    including ownership, X Marks scroll-box capacity, exact capped warning and private lifetime.
    Carry medium clue object 2837 and prove only Men drop key 2838; repeat with Women and in a free
    world to prove suppression. Exercise occupied/blocked piles, solo/tied/grouped/Ironman credit,
    reveal/expiry, exact animations, sounds and packet ordering. Confirm no Wilderness looting bag
    from these placements and keep other Man/Woman identities outside this contract. Do not open
    or use any RuneLite outside the workspace checkout.
128. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test loaded
    Farmers 3114 at 3165,3324; 3181,3293; and 3227,3290, Farmer 3244 at 3262,3325, and Farmers
    11919/11920/11921 at 3233,3308; 3154,3293; and 3199,3289 respectively, all on level zero.
    Confirm combat level seven, size one, stats 3/4/8/12/1/1, +5 Stab accuracy, +6 melee Strength,
    maximum Stab hit one, six-tick attacks, non-aggression, ordinary poison/venom susceptibility,
    no elemental weakness and a 25-tick respawn. Confirm Attack/Pickpocket operations remain intact.

    Kill statistically meaningful free and members samples and compare with both exact 128-share
    tables in the source ledger. Confirm guaranteed bones and every ordinary item/nothing boundary.
    In members worlds force the weight-11 standard-herb branch and weight-27 allotment-seed branch.
    Within the latter verify potato 5318 quantities 1–4 at 64/128, onion 5319 quantities 1–3 at
    32/128, cabbage 5324 quantities 1–3 at 16/128, tomato 5322 quantities 1–2 at 8/128, sweetcorn
    5320 quantities 1–2 at 4/128, strawberry 5323 at 2/128, watermelon 5321 at 1/128 and snape
    grass 22879 quantities 1–3 at 1/128. Establish independent beginner clues at 1/90 in both
    world types and Easy clues at 1/128 for members with the shared ownership/capacity/lifetime
    policy. Exercise occupied/blocked piles, kill-credit modes, reveal/expiry, respawn, exact
    animation/sound/packet order and coexistence with Farmer pickpocket behavior. Keep unloaded
    Farmer identities 3243/11918 outside this contract. Do not open or use any RuneLite outside
    the workspace checkout.
129. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, use the opposite
    elemental rune and talisman on both valid altars for mist, dust, mud, smoke, steam and lava.
    Confirm levels 6/10/13/15/19/23 and directional XP 8.0/8.5, 8.3/9.0, 9.3/9.5, 8.5/9.5,
    9.3/10.0 and 10.0/10.5. Establish that every pure essence and opposite rune is consumed even
    on a failed independent 50% roll, and that one opposing talisman is consumed per interaction.
    Cast Magic Imbue and prove it bypasses only that talisman without changing the 50% chance.

    Wear Binding necklace 5521, use it from fresh through all 16 interactions and capture exact
    Check text, success/partial/failure text, charge persistence after swapping, trading or alching
    another necklace, and disintegration on interaction 16. Destroy a partly used necklace and
    prove the next one begins at 16. Confirm animation 791, graphic 186 including height/delay and
    exact packet/audiovisual ordering. Repeat every members/level/resource/stale-slot/full-inventory
    failure without losing unrelated inputs.

    At Runecraft 90, cast Magic Imbue, wear the necklace and use soul runes on Cosmic Altar 34766
    with pure essence, Daeyalt essence and Aether catalysts. Confirm Aether rune 30843, 20 XP each,
    one catalyst per rune created and one necklace charge per interaction. Prove rejection without
    either Magic Imbue or the worn necklace. Separately document Raiments of the Eye, Twisted and
    Scarred Extract multipliers and Guardians of the Rift behavior before implementing them; do not
    promote those modifiers from research on observation alone. Do not open or use any RuneLite
    outside the workspace checkout.
130. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, click operation
    one on ordinary altars 34760–34772 and true Blood altar 43479. At every level boundary in
    `StandardRuneRecipes`, confirm all eligible essence converts in one interaction, the multiplier
    uses the visible level before XP is awarded, rune essence works only for Air through Body, pure
    essence works everywhere, and Daeyalt follows pure essence while awarding exactly 50% more XP.
    Confirm XP remains per essence when multiple runes are produced. Exercise free/member worlds,
    boosted and drained levels, mixed essence, full inventory, stale interactions and the exact
    Lunar Diplomacy, Mourning's End Part II, Sins of the Father and Dragon Slayer II boundaries.

    At base Runecraft 59/60 and before/after starting Desert Treasure II, test partial and complete
    stacks of matching Warped, Twisted, Mangled and Scarred Extracts. Confirm one extract per
    essence, +250 or +60 fixed runes, additive ordering after ordinary multiplication, no bonus XP,
    no consumption when ineligible and no effect from a mismatched extract. Repeat with zero to
    four Raiments pieces in every colour. For outputs with and without remainders, statistically
    establish the ten-rune-block roll and the full-set 60% bonus, and prove Scar-extract output is
    not multiplied.

    Separately observe mind/body/chaos golem cores at every multiplier boundary to resolve whether
    cache-233.4 produced ten or eleven base runes per core while granting ten-essence XP. Capture
    Sunfire crafting input invocation and consumption. For the now automated active Blood-essence
    true-altar core, confirm the exact activation and full-inventory failure presentation,
    sound/packet order, item charge persistence through bank/relog/restart, independent 50% rolls,
    exact Check text and destruction packet on the 1,000th extra rune. Extend it to dark fragments
    at Kourend and guardian essence in Guardians, prove the Ourania no-effect boundary and Ultimate
    Ironman death/item-retrieval destruction. Also capture dense Blood/Soul behavior, pouch
    degradation, Abyss/Ourania/Guardians access, diary/task credit, exact failure text,
    animation/graphic height and delay, interruption, sound and packet ordering.
    Do not implement a disputed value from post-cutoff documentation and do not open or use any
    RuneLite outside the workspace checkout.
131. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, obtain the
    small, medium, large and giant essence pouches through the Dark mage, Temple of the Eye,
    Abyssal creatures and Rewards Guardian. Establish increasing-size ownership, inventory/bank
    duplicate prevention, the absence of a level requirement for acquisition, and the exact
    Enter-the-Abyss gate. Create the Colossal pouch with every prior tier and an abyssal needle at
    Runecraft 25/Crafting 56; verify mutual exclusion, additional-pouch rejection and login cleanup.

    For every normal and degraded object, compare held Fill, Empty and Check plus bankside dynamic
    left-click. Repeat with rune, pure, Daeyalt and guardian essence, mixed types, empty/full/partial
    inventory, all Runecraft tier boundaries, a boosted level and every degradation threshold.
    Confirm ordinary uniform 0–3 wear per stored essence, deterministic one-per-essence Colossal
    wear, exact excess-return or loss behavior, normal-to-degraded transforms and final destruction.
    Wear both Runecraft capes, the ordinary Max cape and every Max-cape variant to prove wear is
    suppressed but existing damage is not repaired. In Guardians, repeat with a Redwood-lit
    Abyssal lantern and Firemaking below/at 90.

    Repair damaged held and banked pouches through direct Dark mage dialogue, Astral Contact and
    Apprentice Cordelia before/after her 25-pearl unlock, with the one-pearl repeat cost. Capture
    exact dialogue, messages, animations, sounds and packet order. Drop each pouch with contents,
    confirm state clears and same-size drop-trick acquisition remains blocked. Test ordinary death,
    Ultimate Ironman death, private ground visibility, relog/restart persistence and item retrieval.
    Do not open or use any RuneLite outside the workspace checkout.
132. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, mine every
    dense-runestone identity around the Arceuus mine at Mining/Crafting 37, 38 and boosted 38 with
    every pickaxe tier and the chisel carried or absent. Capture attempt cadence, success chance,
    12 Mining XP and 8 Crafting XP, animation/sound, per-player depletion and respawn, and the
    medium Kourend diary's independent 5% extra-block roll with no extra XP.

    Automated coverage now pins both parent morphs and active/depleted cache identities, visible
    level and tool gates, the fixed nine-tick result, exact XP, every 210/230 persistence plateau,
    both alternating depletion directions, second-player isolation, the diary branch, last-slot
    interruption, crystal preservation and cape/Varrock exclusions. Use the client pass to resolve
    the remaining conflict between the cited 3-to-2-tick sub-delay and the observed fixed nine-tick
    process. Leave and re-enter through each western, northern and eastern shortcut repeatedly to
    verify the one-third both-active and two-thirds random-single-active distribution and its exact
    transition tick. Capture literal start/success/full/depleted text, animation restarts, sound and
    packet order; do not infer them from the automated server messages.

    Carry one through 27 dense blocks to Dark Altar 27979. Confirm operation one `Venerate`, exact
    three-tick completion, simultaneous same-slot conversion, 2.5 Runecraft XP and one Prayer
    point per block including current Prayer below the block count and exactly zero. Interrupt with
    movement, teleport, damage, logout and a second action, capturing exact messages and packet
    order. Chisel dark blocks in both held-use orders with a full inventory and while running;
    establish automatic repeat cadence, selected/next-slot order, interruption rules, exact text,
    animation restart and the client display/count behavior of four non-stackable fragments per
    source block.

    Bind at false Blood altar 27978 and Soul altar 27980 around visible levels 77 and 90. Confirm
    23.8 and 29.7 XP per fragment, all-fragment one-tick consumption, members/resource failures,
    full inventory and exact presentation. Repeat with every Raiments count/colour and remainder,
    partial Scarred Extract stacks before and after starting Desert Treasure II at base Runecraft
    59/60, elite Kourend completion and deterministic active Blood-essence rolls. Prove the
    bonuses are additive, only base fragments award XP, the diary rounds down and affects only
    Blood, and Blood essence never changes on Soul. Confirm elite diary task credit on the first
    eligible Blood craft.

    Finally test the Soul-rift permanent Abyss unlock with both dark blocks and fragments, every
    dark-block tablet/nullifier recipe, bank/trade/drop/death and Ultimate-Ironman retrieval rules,
    stale-slot races, relog/restart persistence, sounds, animation/graphic height and full packet
    ordering. Do not open or use any RuneLite outside the workspace checkout.
133. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, shear each of
    the eleven ordinary unsheared NPC identities and both Zanaris identities 5843/5844. Confirm
    ordinary sheep expose left-click `Shear`, always award exactly one wool and retain their exact
    paired colour/model for 50 ticks. Confirm both Zanaris sheep expose `Talk-to`, do nothing when
    left-clicked without beginning a shearing action, accept shears through item use, transform to
    dark-grey sheared NPC 5845 rather than unused 5846 and regrow after 100 ticks.

    Run a statistically meaningful Zanaris sample while capturing every deterministic random
    boundary. Confirm the provisional one-in-four escape chance and exact line `The sheep manages
    to get away from you!`; record whether failure moves either participant, plays an animation or
    sound, changes facing, delays the player, or affects immediate re-use. For successes, capture
    player sequence, sheep facing/movement, shearing synth, message and inventory/transformation
    packet order. Repeat with shears carried in every slot, a full inventory, blocked and occupied
    ground tiles, rapid competing players, movement, damage, teleport, interface interruption,
    logout and world hop. Verify private wool ownership, reveal/expiry and regrowth across those
    interruptions.

    Exercise the Zanaris Talk-to transcript's Yes and No paths, all four random poems, both poems
    that can receive Cow responses, the Nuff-certificate exchange and dialogue with NPC 5845.
    Measure the optional Cow-response probability and identify which nearby Cow is selected under
    zero, one and multiple candidates; capture speaker identity, facing, pauses, interfaces and
    packet order. Do not implement a guessed probability. Do not open or use any RuneLite outside
    the workspace checkout.
134. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, talk to loaded
    Woodsman tutor 3226 at base Woodcutting levels 1, 18, 19, 20, 28, 29, 98 and 99. Confirm every
    menu boundary against transcript revision 14815453, including starter-item replacement with
    and without inventory space, both fletching lines, the advanced-only Magic/Hollow-tree submenu,
    every return-to-teaching route and exact chathead expressions, line wrapping and packet order.

    At level 99, decline the initial offer; accept on both free and members worlds; decline the
    99,000-coin price; accept with 98,999, exactly 99,000 and more coins; and repeat with zero, one
    and two free slots while the coin stack is exact or leaves change. Confirm no partial mutation,
    the free-world refusal, exact capacity text, cape 9807 plus hood 9809 for a first mastery,
    trimmed cape 9808 plus hood for a second mastery, final success line and jingle 231 timing.
    Repeat after banking, dropping, equipping and losing earlier capes to establish whether duplicate
    purchases or replacement hoods expose any cache-233 branch absent from the cutoff transcript.
    Do not open or use any RuneLite outside the workspace checkout.
135. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, use loaded Bob
    10619's fourth operation `Repair` with one and several damaged original Barrows pieces, all four
    Ahrim ornament pieces and each of the nine Moon armour pieces. Test the 100/75/50/25/0 Barrows
    identities at their boundaries and internal points between boundaries. Confirm 60/90/80/100
    coins per missing point for helm/body/legs/weapon, respectively; 500 coins per missing Moon
    armour point; 330,000 coins for a fully broken four-piece Barrows set; and 1,500,000 coins for
    each fully broken Moon armour piece.

    Repeat through item-on-Bob with a repairable object, every Moon weapon and an unrelated object.
    Confirm the exact no-item, unsupported-item, price, cancel, insufficient-money and success
    dialogue; repair-all scope; singular/plural wording; chatheads; sounds; line wrapping and packet
    order. Exercise exact funds, one coin short, excess funds, full inventory, changed slots during
    the dialogue, duplicate damaged items, packed state, bank/relog persistence and simultaneous
    players. No failure or stale quote may delete coins or replace any item.

    Compare Bob with Tindel, Dunstan, the Squire and Aneirin, then measure POH armour-stand
    discounts. Keep Perdu/PvP repair behavior separate. Do not open or use any RuneLite outside the
    workspace checkout.
136. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, wear multiple
    original Barrows, Ahrim ornament and Moon armour pieces together, with at least one Moon weapon.
    Enter combat as the attacker with melee, ranged, damaging magic, splashing magic and a successful
    non-damaging spell; then enter combat only as the target of another player and an NPC. Confirm
    every eligible worn armour piece loses one point immediately even on zero damage or protection,
    the Moon weapon never changes, repeated engagement inside the active-combat window does not take
    extra points, and continuous combat takes exactly one further point every 90 ticks.

    Exercise every 1,000/750/500/250/0 Barrows boundary and the 3,000/0 Moon boundaries, including
    exact internal point values. Confirm the cache object changes at the right point, bonuses remain
    unchanged before zero, the broken item remains worn when it reaches zero, its bonuses stop, it
    cannot be re-equipped after removal, and held/worn `Check` displays the matching value. Stop
    combat, wait beyond the active window and re-enter; verify the immediate point and reset 90-tick
    cadence. Repeat across logout and world hop to capture the exact production boundary.

    Drop full, partial and broken Barrows/Moon armour with the valuable-item warning accepted and
    cancelled. Accepted ordinary drops must appear broken and cancellation must leave the exact item
    untouched. Confirm bank and relog preservation with multiple identical pieces at different exact
    points. When player trading and the Grand Exchange exist, test fully repaired, intermediate and
    broken states through their complete offer/accept/cancel flows and verify they consume the shared
    dynamic rule already used by price checks and shops. Separately test ordinary death, gravestones,
    Death's Office and protected items; every PvP death ownership/protection boundary; simultaneous
    players; messages, warning order, sounds, appearance rebuilds and packet timing. Do not infer
    those still-open paths from the ordinary-drop test. Do not open or use any RuneLite outside the
    workspace checkout.
137. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, talk to loaded
    Hans 3105 and select `Can you tell me how long I've been here?`, then repeat with his right-click
    `Age` operation. Confirm both routes report identical whole days, hours and minutes played and
    whole days since character creation. Stay logged in across minute and day boundaries, log out
    and back in, and restart the server; verify time played advances only while logged in and both
    values retain their database-backed progress. Capture exact chathead, line wrapping, interface
    and packet order against the cache-233 client. Do not open or use any RuneLite outside the
    workspace checkout.
138. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, make every canoe
    tier at each of Lumbridge, the Champions' Guild, Barbarian Village, Edgeville and Ferox Enclave.
    Confirm exact level gates 12/27/42/57, XP 30/60/90/150, automatic retries, axe-specific success
    rates, shaping sequences, tree-fall/push/paddle/sink sounds and every private station morph.
    Verify iron is selected over bronze at their shared level, the best eligible carried or worn
    axe is used, members-only axes are rejected on free worlds, and both Crystal identities require
    Song of the Elves. Cut and shape with active Crystal and each charged Infernal style at one
    remaining charge; confirm neither phase consumes or transforms the tool because no item is
    obtained and no Infernal log burn occurs.

    For log, dugout, stable dugout and waka, verify the complete destination menu from every station.
    Confirm only waka enters Ferox or the Wilderness pond, while a log made at Ferox can travel one
    stop to Edgeville; confirm every lower-tier stop limit in both directions. Exercise the initial
    Wilderness warning's decline, one-time accept and permanent do-not-show-again setting, every
    exact arrival line, one-use sinking and the inability to return from the pond.

    Leave a prepared canoe unused, teleport away and return, prepare another station, log out and
    back in, restart the server, interrupt every cut/shape/push phase with movement, damage,
    interfaces and teleportation, and repeat with two players at the same station. Capture precise
    action delays, morph visibility, camera, overlays, NPC scenery, appearance rebuilds and packet
    order for both grass and Wilderness cutscenes. Do not open or use any RuneLite outside the
    workspace checkout.
139. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, use Dairy cow
    8689's `Steal-cowbell` operation on a free world and a members world, with Cold War varbit 3293
    at zero, an active value and its completed value. Repeat at current Thieving levels 14, 15, 43
    and 99, including boosted and drained boundaries, and with zero and one free inventory slot.
    Confirm rejected attempts do not animate, damage, stun, grant XP or alter inventory. Sample
    enough deterministic rolls at 15/43/99 to verify the 128/200, 160/200 and 200/255 rsmod integer
    boundaries corresponding to 54.296875%, 62.5% and 78.515625%.

    On success, confirm exactly one Cowbells 10593 and 16 Thieving XP plus the literal line `You
    steal a cowbell.` On failure, capture the two player lines, Dairy cow `MOO`, exact damage, stun
    duration, player/cow sequences, spot animations, sounds, hitsplat timing, facing, approach and
    all packet order. Specifically verify or correct the provisional one damage and eight blocked
    cycles. Exercise rapid repeat clicks, movement, damage, teleport, interface interruption,
    logout and two players attempting the same cow concurrently.

    Separately exercise Zanaris Dairy cow 12111 and record its conversation-based Cowbell handout
    under every Cold War state and inventory boundary. Use Cowbells `Play` with and without the
    complete penguin disguise, then carry the result through every quest step that obtains,
    consumes or replaces it. Keep these unimplemented quest/item routes distinct from Dairy cow
    8689's theft core. Do not open or use any RuneLite outside the workspace checkout.
140. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, combine leather
    1741 and mahogany plank 8782 in both directions at every reachable Cold War varbit-3293 value,
    before starting, immediately after starting, before and after speaking to Ping/Pong, after the
    instrument hand-in and after quest completion. Pin the earliest accepted state and verify all
    later replacement states. At each state test members and free worlds, base Crafting 29/30 with
    current Crafting drained or boosted across 30, missing either ingredient, a full inventory and
    one free slot. Rejections must preserve both exact slots and XP; success must consume one of
    each, create Penguin bongos 10592, grant exactly 25 Crafting XP and emit `You use the leather
    and plank to create penguin bongos.` Capture any sequence, delay, sound and packet ordering.

    Use inventory `Play` while human, in every partial suit/equipment state and while completely
    transformed. Confirm the exact rejection text/chat type, whether sequence 5674 or 5675 is used,
    animation duration, action delay, sound/music, appearance state, interruption and item
    preservation. Then hand Cowbells and bongos to Ping/Pong and trace exact consumption, dialogue,
    cutscene sequences, sounds, camera, quest-state transition, logout/re-entry and simultaneous
    players. Do not open or use any RuneLite outside the workspace checkout.
141. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, combine
    clockwork 8792 with regular, oak, willow, teak, maple, mahogany, yew, magic and redwood logs in
    both directions. Test one below and exactly at boosted Crafting levels
    5/15/25/35/45/50/60/75/90 on members and free worlds. Confirm outputs
    21512/21515/21518/21521/22192/22195/22198/22201/22204 and exact XP
    15/20/25/30/35/40/45/50/55, with no loss on any failed gate.

    Repeat with no tool, ordinary hammer 2347, carried and worn Imcando hammer 25644, and carried
    and worn off-hand Imcando hammer 29775. Confirm chisel 1755 must be carried, tool priority when
    several hammers are present, sequence 7057 for ordinary hammer and 8916 for either Imcando form.
    Fill every inventory slot and prove the two-input/one-output transaction succeeds atomically.
    Confirm the first result lands after two ticks and every Make-All repeat after three more ticks.

    Remove each input or tool between results; move, run, teleport, enter combat, replace an
    interface, start every other Crafting action, log out and die. Capture exact success/rejection
    text, sounds, animation and inventory packet order, repeat-click behavior and simultaneous
    interactions. Keep bird-house spot placement, seed loading, timers, catches, nest rolls,
    dismantling, clockwork return and Hunter XP as a separate lifecycle test. Do not open or use any
    RuneLite outside the workspace checkout.
142. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, visit all four
    Fossil Island bird-house sites at (3677,3882), (3679,3815), (3768,3761) and (3763,3755).
    Place every regular-through-redwood shell one level below and exactly at boosted Hunter levels
    5/14/24/34/44/49/59/74/89. Confirm exact failure wording and that successful placement consumes
    one shell, plays sound 2433 and resolves display values 1/4/7/10/13/16/19/22/25 to the correct
    built model. Repeat on free worlds, with invalid items, stale location clicks, changed slots and
    concurrent players; no rejected action may consume an item or change either site variable.

    Arm every tier with every suitable bait identity. Confirm the sourced 30 low seeds consume ten
    and the 11 high seeds consume five, `Seeds` chooses the expected inventory priority, and mixed
    loads such as four Barley plus three Ranarr retain the part-filled model before becoming active.
    Test one unit short, excess stacks, changed slots, rapid repeated clicks, movement, interfaces,
    combat, death, logout, world hop and a full server restart. Verify readiness occurs at exactly
    3,000 seconds of wall-clock time and remains correct across logout and restart, including clock
    adjustment boundaries. Capture exact messages, animation, sound audience, varp/inventory packet
    order and all built/part-filled/active model transitions.

    After the timer, exercise `Interact`, `Empty`, `Dismantle` and `Reset` on every tier. Pin ten-bird
    completion, feathers, raw bird meat, every empty/ring/seed nest roll, Hunter XP
    280/420/560/700/820/960/1020/1140/1200, clockwork return, inventory overflow and ground-drop
    behavior. Automated coverage now pins mature `Interact`, exact XP, clockwork, all four feather
    quantities, meat-always-ground, overflow privacy, seed-nest visible-level boundaries, warned
    partial `Dismantle`, warned early `Empty`, direct raw-material `Build`, carried-shell priority and
    both mature `Reset` paths. Use this pass to match their exact client presentation, multiple-log
    chooser, interruption edges and the explicitly unconfirmed clue branch, plus all diary,
    achievement and collection-log hooks. Do not open or use any RuneLite outside the workspace
    checkout.
143. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, test bird snare
    10006 against crimson swift 5549, golden warbler 5551, copper longtail 5552, cerulean twitch
    5550 and tropical wagtail 5548. Confirm the four-tick `Lay` cadence, step-off direction, exact
    placement rejection footprint, stack handling and boosted-level trap caps at 1/20/40/60/80,
    including the one-extra-trap Wilderness rule. Observe the exact attraction radius and scan
    cadence rather than assuming the current bounded two-tile/three-cycle values.

    For every bird, capture approach path, facing and sequences; successful and failed two-tick
    location transitions; one-minute open, failed and full expiration; private snare ground-drop
    lifetime; bird despawn and respawn timing; and owner/non-owner interaction. Prove that standing
    on the trap blocks a catch, that insufficient Hunter level rejects higher prey, and that exact
    XP is 34/47/61.2/64.5/95.2 with one snare, bones, raw bird meat and 5–10 matching feathers
    returned atomically by `Check`.

    Use a lit ordinary torch and Bruma torch on an open trap, then compare unsmoked, smoked and full
    Guild Hunter outfit catch samples against the +2% and +2.5% absolute modifiers. Capture exact
    `Investigate`, smoke, ownership, capacity and dismantle messages, chat types, animations,
    sounds and packet order. Repeat movement, teleport, combat, death, logout, world-hop, rapid
    clicks, full inventory and simultaneous-player boundaries. Keep Hunter Rumours, diaries,
    achievements, League tasks and other portable traps as separate work. Do not open or use any
    RuneLite outside the workspace checkout.
144. Using only RuneLite built from `/Users/stephenrobb/Dev/OSRSemulator/runelite`, catch sunlight
    moth 12770 and both clickable moonlight variants 12771/12772 with butterfly net 10010, magic
    butterfly net 11259 and bare hands. Confirm membership, visible Hunter thresholds 65/75 and
    barehand thresholds 75/85, exact success distributions for 20/296, 40/316, 0/276 and 20/286,
    74/84 XP, failed-attempt behavior and the 100-tick despawn/respawn interval. Verify NPC 12773
    remains non-clickable. Measure exact approach distance, catch cadence, facing, sequence 5209,
    sounds, messages, interruption, repeated clicks, two-player contention and packet order.

    Repeat each catch with no jar, one empty butterfly jar 10012, several jars, a full inventory
    whose only replaceable slot is the jar and changed inventory/equipment state during the action.
    Confirm net catches fill 28890/28893 atomically while barehanded catches immediately release and
    never consume a carried jar. For direct sunlight catches, verify eight Hitpoints, five percent
    run and `floor(base × 20%) + 6` restoration for Attack, Defence, Strength, Ranged, Magic,
    Cooking, Woodcutting, Fletching, Fishing, Firemaking, Crafting, Smithing, Mining, Herblore,
    Agility, Thieving, Slayer, Farming, Runecraft, Hunter and Construction, capped at each base.
    Confirm Prayer is untouched. For direct moonlight catches, verify exactly 22 Prayer capped at
    base and no Holy-wrench modifier.

    Use filled-jar operation four `Release` for both species in full and non-full inventories.
    Confirm one empty jar is returned, sunlight restores the same stats and Hitpoints but no run,
    moonlight restores exactly 22 Prayer, and sequence 5213 plus the cache-5214 moth visual use the
    exact audience and timing. Exercise free-world release, stale slots, movement, combat,
    teleport, logout/death and rapid repeats. Separately capture Accept Aid and jar-on-player sharing
    rules, legacy ruby/sapphire/snowy/black butterfly success curves, rare moth-wing rolls and Hunter
    Rumour pity/progress behavior; none is claimed by the production core. Do not open or use any
    RuneLite outside the workspace checkout.
