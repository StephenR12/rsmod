# Production skill progress

This report covers eleven production and gathering skills for cache 233.4. The early checkpoints below record when each feature entered the checkout. The current playtest guide consolidates later work across skill modules. [Skill coverage, launch instructions and diary checks](SKILLS_AND_DIARIES.md)

## Implemented additions

| Skill | Added playable behavior | Diary/result coverage added in this pass |
| --- | --- | --- |
| Cooking | Ordinary single-bite food eating with native Eat operations, capped healing and shared three-tick food/attack delay; normal, edible, swamp and giant seaweed to soda ash; giant success yields six and failure one; supported POH stoves 1–6 accept food-on-location with the appropriate fire/range classification. | Bread on Lumbridge castle range; thoroughly cooked karambwan; thin snail at the exact Port Phasmatys range; ordered local bass/monkfish/anglerfish/dark-crab catches and cooking; five Catherby sharks with gauntlets and the correct range; Nardah wild pie and Cooking Guild summer pie. |
| Crafting | Nine snail-shell/snelm recipes with native quantity menu; Sbott is present and paid successful tanning carries his identity. | Lumbridge cow-pen coif; Canifis-bank black dragonhide body; snelm outside Mort Myre; Sbott tanning; ordered Rellekka silver mining/smelting/tiara; Barbarian Village bowl shaping/firing; Falador farm basket; Neitiznot dragonstone amulet; ordered Lumbridge diamond-amulet crafting/stringing with the enchantment completion hook supplied to Magic. |
| Smithing | Ordinary native anvil interface 312 with 156 recipes, six metals, quantities, tools, level/quest/member gates, exact bar transactions and Smiths' Uniform timing; separate blurite limbs; dragon square assembly and unique gold helmet smithing. | Doric blurite limbs; Ardougne-area and Nardah mithril platebodies; Draynor-sewer adamant platebody; Resource Area adamant scimitar; Port Phasmatys cannonballs; ordered Varrock rune dart smithing/fletching; West Ardougne dragon square and Resource Area gold helmet. |
| Fletching | Sunlight/moonlight antler bolts and sunlight hunter's crossbow upgrade. | Strung oak shortbow in Gnome Stronghold; strung magic longbow in Tirannwn; Seers-bank maple shortbow; ordered local yew tree → unstrung yew longbow → stringing; Bedabin dragon darts, ordered Varrock rune darts, Witchaven string/Yanille limbs/local stock→rune crossbow, and grown/harvested Guild celastrus→battlestaff. |
| Herblore | Wesley's 17 crushing services at 50 coins per input, including noted materials and lava-scale quantities; Zahur cleans the 15 ordinary herbs at 200 coins each, with noted cleaning gated by Desert Hard completion. Both NPCs have ordinary spawns. | Successful paid Zahur cleaning; Falador Saradomin brew; Varrock super combat; Fremennik super defence; Morytania Guthix balance; rooftop Seers stamina mix; ordered Seers/Catherby irit unfinished potion → superantipoison; desert combat potion outside the modeled city exclusions; horse-shoe mine antivenom; Lovakengj pub strength. |
| Mining | Granite/sandstone largest-first cascade extraction with size-specific XP; native Abandoned Mine mithril walls; mixed normal/golden Prospector outfit XP, full-set bonus, Varrock armour 4 substitute. Parent lane also supplies essence mining/travel. | Al Kharid/Varrock/Piscatoris/Karuulm/Wilderness iron; Wilderness mithril; Rellekka coal and silver; Crafting Guild/Grand Tree gold; Tirannwn adamantite; coal trucks; five northeastern-desert clay and five Jatizso adamantite rewards; peninsula Karamja gold, gem-rock topaz, granite and Abandoned Mine mithril. |
| Fishing | Complete playable aerial-fishing cycle: loans, four fish, level-weighted catches, bait/chunk consumption, cutting fish, pearls/golden tench, reward purchases, boats and island spawns. Mixed Angler/Spirit Angler XP also applies to existing ordinary/karambwan/karambwanji catches. | Al Kharid anchovies; karambwan; Lake Molch bluegill; Catherby mackerel/bass/sharks; Lumbridge salmon; Barbarian Village trout; platform fish; Ape Atoll bass; Wilderness lava eel; Baxtorian sturgeon; Burgh de Rott barehand shark; ordered regional catch/cook rows; Musa Point catches and Molch trout result hooks. |
| Firemaking | Existing log, bow, handheld-light, funeral-pyre and pyre-ship systems now use successful result hooks. The old magic-pyre-ship bit write uses central task completion so tier counts and kill-sequence interruption update. | Ordered Lumbridge oak and regional willow/teak/mahogany/magic/oak/yew burn tasks; Seers bow-lit maple; Chemist bullseye lantern; Lumbridge-basement mining helmet; chewed-bone magic pyre ship; magic/redwood shade cremation; Nardah west balcony yew (floor1). |
| Woodcutting | Source-backed public bird/chinchompa placements make their associated Hunter loops reachable. Existing chop results carry ordered local production state. Native Neitiznot stumps split arctic pine and craft learned round shields. | Draynor willow; dying Lumber Yard tree; Mage Training Arena magic; Kourend redwood; Uzer's northern teak; Karamja teak/mahogany, north-Farming-Guild mahogany and Morytania hollow bark; ordered regional cut/burn rows and Seers yew bow. |
| Thieving | Nineteen ordinary stall definitions, native operation numbers, empty/restock state, reward transactions and nearby witnesses; gloves-of-silence wear/check/repair; 45-seed master-farmer catalogue with Farming-scaled valuable herbs and twenty ordinary farmer/Martin spawns. | Lumbridge citizen; Martin; north-Ardougne master farmer; Falador guard; hero; gnome; elf; Ardougne cake, Varrock tea, Keldagrim bakery/gem and Hosidius fruit stalls. |
| Hunter | Five salamander net traps and five pitfall species; native Burrow stairs/knife-root supplies and90 mainland/Guild moth spawns; twelve impling species with Puro-Puro entry, exit, supplies, jar generators, ordinary/Lucky jar loot and mapped rolling populations; three chinchompa box traps share limits with bird snares; smoke has the 39 Hunter/tool gate; ordinary prey spawns; falconry with rentals/investment, flight, ownership, retrieval, prey rewards, expiry and loan cleanup; four legacy butterflies with nets/barehanding, optional jars, combat/healing effects and 98 dated spawn coordinates. | Golden warbler; cerulean twitch; copper longtail; Kourend chinchompa; dashing kebbit; snowy knight; swamp/orange/red/black salamanders, larupia/graahk/kyatt and Puro essence/eclectic implings. |

Ordered production progress is saved separately from native completion bits. New incomplete
partial steps and counters interrupt the two Wilderness boss kill sequences. Completed production
tasks stop acquiring fresh ordered-step state. Tasks are credited after successful output or paid
service transactions; failed rolls, rejected capacity checks, burnt food, and unpaid services do
not receive credit.

## Explicit approximations and behavior requiring client observation

- **Aerial fishing:** species thresholds, level formula, XP, bait cycle, pearl formula, golden-tench
  denominator and prices follow the dated sources. Six active relocating pools, pool movement
  intervals, flight distance/timing, activity boundary, full-inventory throwback handling, reward
  dialogue presentation and shop stock are bounded server choices. Exact projectiles, pool count,
  UI/shop packets, rare golden-tench XP, clue bottles, heron and collection-log routing remain open.
- **Falconry:** requirements, rentals/investment, rewards, XP and published success curves are sourced.
  Flight/retrieval timing and range, timeout, fence boundary, NPC orientations, yellow hint arrow,
  projectiles, exact dialogue and package routing need observation. Guild rumours now track rare parts. Pouch delivery is covered in the material-delivery section. RemainsDrop now allows central Prayer equipment to consume bones only after successful delivery of all other loot.
- **Legacy butterflies:** requirements, XP, barehanding at ten extra Hunter levels, jar items,
  combat/healing effects and spawn coordinates are sourced. No pre-cutoff page supplies exact catch
  odds: production explicitly uses `LegacyButterflyTable.approximateChance` (50% at requirement,
  +0.5 percentage points per additional level, +5 points for magic net/barehanding, capped at 95%).
  This is playable behavior, **not verified success-rate parity**. One-tick catch timing and inherited
  NPC respawn/wander values need observation. Jar release shares the ordinary potion cooldown;
  The later Hunter mix checkpoint adds multi-combat sharing and Accept Aid checks. Guild rumours and rare parts are covered by the Hunter progression module.
  The Ruby Harvest article's Aldarin table mislabels its creature Copper Longtail; that ambiguous
  nine-spawn block is deliberately omitted.
- **Portable traps:** exact attraction/search timing, placement restrictions, bait effects,
  orientation, animation and sound need live comparison. Guild rumours now track rare parts. Eagles' Peak checks the existing
  completed save state; partially completed quest access has not been reproduced.
- **Stalls:** the two-tick attempt and witness range/facing model are bounded. Exact sight/facing,
  guard selection, messages and shopkeeper stolen-goods cooldown need observation.
- **Master farmer seeds:** herb redistribution uses the dated calculator's exact model, base Farming
  level and the 85 cap. Other subtables normalize the published rounded three-significant-figure
  rates, including snape grass and seaweed spores; exact unrounded weights are unknown. Martin's
  quest dialogue/skillcape purchase and ten-minute talk lock after theft remain open.
- **Seaweed:** giant seaweed guarantees success at boosted level 51. Output-capacity failure preserves
  inputs atomically; the original game's possible overflow-ground behavior has not been observed.
- **Diaries:** facility coordinates are cross-checked with the revision cache where possible. Some
  regional eligibility uses documented conservative rectangles rather than recovered original
  server zone definitions. Ordered material provenance is represented by action state, not by
  tagging individual inventory objects. Repeated reset/interleaving edge cases need observation.

## Additional Hunter coverage and explicit limits

Net traps use native young-tree actions, rope/net recovery and reset, timed trap lifecycle,
smoking/bait, correct Hunter requirements/XP, all five salamander species, Tecu adult odds1/1000,
shared portable trap limits and71 dated creature placements. Pitfalls use native pit morph parts,
knife/log setup, teasing-stick or wielded hunter-spear luring, active melee hits, jumping, catch
collapse, capacity-safe saved rewards and28 dated creature placements. Exact placement/catch
presentation and multi-part net mesh placement still require client observation. Pitfall catch
and fur odds are explicitly approximate (50% at requirement,+0.5 percentage points per level,
95% cap; graahks guaranteed at85; antelope catches guaranteed). Spear teasing is100%, stick95%,
the latter a bounded estimate. Guild rumours track rare parts. The material-delivery section covers pouch delivery.

Implings support all12 species, exact published net/enhanced catch curves, barehanded +10-level
requirements, optional jars outside Puro (mandatory inside), variant-specific XP and no reward
on failed catches. Eleven ordinary jar tables have208 resolved rows; Gourmet grubby keys,
Crystal signets and normal clue chances are independent where sourced. Lucky jars select one
of five tiers equally, then one historical reward outcome:991 outcomes /993 item rows. Each
exact rational table sums to1; the hard triple-potion drop is one bundle. Casket-only tertiary
pets/master clues, Mimic bonuses and completion counters are excluded. Full-inventory loot is
owner-only ground delivery, a bounded choice until original capacity behavior is observed.

Puro-Puro has51 fixed spawns and24 mapped dynamic anchors (the dated prose says35 but supplies
only24 coordinates),48 generic overworld anchors,11 always-low anchors and12 possible Crystal
positions with one rolling precursor. Published category weights are exact. The approximate
2-minute invisible interval and load-relative30-minute/three-attempt overworld scheduler are
explicitly a playable model, not verified world-clock parity. Crystal visibility uses its more
specific5-minute source. Catchable Crystal NPC names/orientations are not pinned to individual
source locations. Imp defenders, moving wheat pattern changes and native collection-log UI
remain absent. Successful standard binding grants5 cycles of other-hunter catch exclusion;
the dated source specifies only 'a few seconds', so that duration is approximate.

Puro's native operation-less2×2 vortex is an engine area exit, retaining the entry field even
after the21-field crop rotation moves. Entry requires base Hunter17; an overworld entry grants
30 minutes of affinity, speeding native op5 wheat pushes by3 ticks. Native success charts,
6/8/10-tick push times,4/2/0 Strength XP and default XP opt-out are implemented. Rotation begins
at the first published field on server startup; visual crop-circle quadrants and actual world
phase need observation. Elnock gives the one-time net/seven-jar kit, buys jars for2000 coins
up to10/day, exchanges jars/repellent/magic net/generators, stores supplies and withdraws them.
Generator charge survives inventory moves;33 impling jars plus one butterfly jar consume100%.
Stored magic-net support, original trade-interface presentation and supply warning dialogue
parity remain open.

## Current production playtest coverage

| Skill | Available to test | Checks still open |
| --- | --- | --- |
| Cooking | Regional ranges, campfires, private-house stoves, baked goods, Hunter meats and three breweries. | Check overflow on a full inventory and the presentation of multi-stage recipes. |
| Crafting | Leather, jewellery, pottery, glass, spinning, sandstone grinding and crystal singing. | Verify specialist supplies through ordinary play and compare production menus with the client. |
| Smithing | Ordinary anvils, Blast Furnace, Elemental Workshop and Giants’ Foundry commissions and rewards. | Compare machinery cadence, progress displays and quality changes. |
| Fletching | Bows, bolts, darts, antler and sunlight weapons, atlatl ammunition and Vale Totems. | Check ent routes, offerings and individual reward frequencies over repeated rounds. |
| Herblore | Mixing, manual and bulk decanting, Mastering Mixology, potion storage and the chugging barrel. | Check bank controls, stored dose combinations and effects under interruption. |
| Mining | Motherlode, Shooting Stars, Blast Mine, Volcanic Mine, Camdozaal, Zalcano, amethyst and diary armour duplication. | Compare group hazards, depletion, contribution rewards and timed vault presentation. |
| Fishing | Rods, nets, harpoons, aerial fishing, Fishing Trawler, drift nets, Tempoross and Camdozaal. | Compare school movement, boat activity, rare rewards and equipment presentation. |
| Firemaking | Ordinary fires, campfires, lights, sacred oil, shade cremation, barbarian pyre ships and Wintertodt. | Check multiplayer timing, visual cues and reward delivery. |
| Woodcutting | Ordinary and specialist trees, Forestry events and rewards, log baskets, felling axes, Sulliusceps and blisterwood. | Group event frequency, per-player eligibility and multiplayer tree depletion need live comparison. |
| Thieving | Pickpockets, safes, blackjacking, artefacts, Pyramid Plunder, Rogues’ Den and Sorceress’s Garden. | Check guards, traps, patrols and reaction timing in the client. |
| Hunter | Trapping, tracking, birdhouses, falconry, implings, rumours, herbiboar, maniacal monkeys and Hunter mixes. | Imp defenders and making impling jars at the Chemist's still remain deferred. Catch curves, moving wheat and some multiplayer behaviour need comparison. |

Ordered diary tasks use saved intermediate state and successful material outputs. Quests use existing saved stages. See the central quest TODO and manual route checks for testing on a fresh account. [Skills and diaries](SKILLS_AND_DIARIES.md)

## Verification checkpoints

All Gradle runs for this lane use a private disposable snapshot and cache, serially, to prevent
concurrent integration mutation of the shared game cache.

- Earlier broad checkpoint: **902 passing integration tests** across Player, Cooking, Snelms,
  Antlers, Herblore, Thieving, Bird Snares/Box Traps, Smelting, Mining, Fishing, Woodcutting,
  Firemaking, Leatherworking, Bow Stringing and Ammunition.
- Later pack and diary batch: **448 passing integration tests**: Player 69, Jewellery 24,
  Log Fletching 10, Bow Stringing 17, Cooking 89, Mining 50, Herblore 189.
- Aerial fishing checkpoint: **186 passing Fishing integration tests**, including nine aerial cases.
- Master farmer checkpoint: **122 passing Thieving integration tests**, including five new cases.
- Tanning checkpoint: **11 passing tests**, including paid Sbott identity/diary checks.
- Firemaking/Weaving and final legacy butterfly/superantipoison results are recorded in the parent
  completion report after their serial verification; these lines intentionally do not predict a pass.

## Additional dated sources

The pre-cutoff Wiki revisions below supply the requirements and tables used by this pass. Native
operation numbers, interface layouts, symbols and facility coordinates are also checked directly
against the local cache. Wiki-derived material retains the attribution/license stated in
`SKILLS_AND_DIARIES.md`.

- [Smithing 14981495](https://oldschool.runescape.wiki/w/Special:PermanentLink/14981495),
  [Smiths' Uniform 14910928](https://oldschool.runescape.wiki/w/Special:PermanentLink/14910928),
  [Blurite limbs 14986463](https://oldschool.runescape.wiki/w/Special:PermanentLink/14986463).
- [Gloves of silence 14937449](https://oldschool.runescape.wiki/w/Special:PermanentLink/14937449),
  [Fruit stall 14906888](https://oldschool.runescape.wiki/w/Special:PermanentLink/14906888),
  [Master Farmer 14966702](https://oldschool.runescape.wiki/w/Special:PermanentLink/14966702),
  [Martin 14972181](https://oldschool.runescape.wiki/w/Special:PermanentLink/14972181),
  [Master farmer special seed calculator 14718052](https://oldschool.runescape.wiki/w/Special:PermanentLink/14718052).
- [Snelm 14863747](https://oldschool.runescape.wiki/w/Special:PermanentLink/14863747),
  [Sbott 14947661](https://oldschool.runescape.wiki/w/Special:PermanentLink/14947661),
  [Zahur 14994347](https://oldschool.runescape.wiki/w/Special:PermanentLink/14994347),
  [Wesley 14960277](https://oldschool.runescape.wiki/w/Special:PermanentLink/14960277).
- [Hunter's sunlight crossbow 14995576](https://oldschool.runescape.wiki/w/Special:PermanentLink/14995576),
  [Sunlight antler bolts 14980583](https://oldschool.runescape.wiki/w/Special:PermanentLink/14980583),
  [Moonlight antler bolts 14936793](https://oldschool.runescape.wiki/w/Special:PermanentLink/14936793).
- [Giant seaweed 14940172](https://oldschool.runescape.wiki/w/Special:PermanentLink/14940172),
  [Soda ash 14916754](https://oldschool.runescape.wiki/w/Special:PermanentLink/14916754),
  [Prospector kit 14989966](https://oldschool.runescape.wiki/w/Special:PermanentLink/14989966).
- [Aerial fishing 14953767](https://oldschool.runescape.wiki/w/Special:PermanentLink/14953767),
  [Alry 14992593](https://oldschool.runescape.wiki/w/Special:PermanentLink/14992593),
  [Alry's Angling Accessories 14893175](https://oldschool.runescape.wiki/w/Special:PermanentLink/14893175),
  [Angler's outfit 14941834](https://oldschool.runescape.wiki/w/Special:PermanentLink/14941834),
  [Boaty 14628664](https://oldschool.runescape.wiki/w/Special:PermanentLink/14628664).
- [Box trap 14969786](https://oldschool.runescape.wiki/w/Special:PermanentLink/14969786),
  [Falconry 14840978](https://oldschool.runescape.wiki/w/Special:PermanentLink/14840978),
  [Matthias 14856807](https://oldschool.runescape.wiki/w/Special:PermanentLink/14856807).
- [Butterfly (Hunter) 14833883](https://oldschool.runescape.wiki/w/Special:PermanentLink/14833883),
  [Ruby harvest 14800303](https://oldschool.runescape.wiki/w/Special:PermanentLink/14800303),
  [Sapphire glacialis 14805052](https://oldschool.runescape.wiki/w/Special:PermanentLink/14805052),
  [Snowy knight 14953827](https://oldschool.runescape.wiki/w/Special:PermanentLink/14953827),
  [Black warlock 14995309](https://oldschool.runescape.wiki/w/Special:PermanentLink/14995309).

Additional dated source revisions for these mechanics:
[Net trap14975913](https://oldschool.runescape.wiki/w/Special:PermanentLink/14975913),
[Pitfall trap14982733](https://oldschool.runescape.wiki/w/Special:PermanentLink/14982733),
[Roots14977258](https://oldschool.runescape.wiki/w/Special:PermanentLink/14977258),
[Food14989962](https://oldschool.runescape.wiki/w/Special:PermanentLink/14989962),
[Granite rocks14991059](https://oldschool.runescape.wiki/w/Special:PermanentLink/14991059),
[Sandstone rocks14968309](https://oldschool.runescape.wiki/w/Special:PermanentLink/14968309),
[Fish stall14907500](https://oldschool.runescape.wiki/w/Special:PermanentLink/14907500),
[Elnock14856949](https://oldschool.runescape.wiki/w/Special:PermanentLink/14856949),
[Jar generator14744515](https://oldschool.runescape.wiki/w/Special:PermanentLink/14744515),
[Easy casket14996452](https://oldschool.runescape.wiki/w/Special:PermanentLink/14996452),
[Medium casket14996453](https://oldschool.runescape.wiki/w/Special:PermanentLink/14996453),
[Hard casket14984628](https://oldschool.runescape.wiki/w/Special:PermanentLink/14984628),
[Elite casket14991796](https://oldschool.runescape.wiki/w/Special:PermanentLink/14991796),
[Master casket14991843](https://oldschool.runescape.wiki/w/Special:PermanentLink/14991843).

Specialist anvil recipes use native anvil/item-use menus, hammers (including supported carried/worn
Imcando forms), level/tool rechecks, atomic material delivery and normal Smiths' Uniform timing.
Dragon square:60 Smithing, both shield halves,75XP; Legends completion gates wielding and shop
access rather than assembly itself. Gold helmet:50 Smithing, three gold bars,30XP; native Between
a Rock progress bit299 at least80, and no existing helmet in any carried/worn/bank inventory.
Neitiznot stump:56 Woodcutting, native Cut-wood menu, three-cycle actions; an axe and one arctic
pine log yield one split log/5 WoodcuttingXP with Lumberjack modifiers. A learned shield requires
Fremennik Isles progress290, hammer, two arctic logs, one bronze nail and rope, yielding34 CraftingXP.
The quest gates are corroborated by pre-cutoff Quest Helper commit
[f0fa06b Between a Rock](https://github.com/Zoinkwiz/quest-helper/blob/f0fa06bcc5426c632bb5ce67db5cf431718f06ea/src/main/java/com/questhelper/helpers/quests/betweenarock/BetweenARock.java) and
[Fremennik Isles](https://github.com/Zoinkwiz/quest-helper/blob/f0fa06bcc5426c632bb5ce67db5cf431718f06ea/src/main/java/com/questhelper/helpers/quests/thefremennikisles/TheFremennikIsles.java),
plus [Dragon square14994522](https://oldschool.runescape.wiki/w/Special:PermanentLink/14994522),
[Gold helmet14907651](https://oldschool.runescape.wiki/w/Special:PermanentLink/14907651),
[Shield14841174](https://oldschool.runescape.wiki/w/Special:PermanentLink/14841174),
[Split log14424328](https://oldschool.runescape.wiki/w/Special:PermanentLink/14424328) and
[Stump14986529](https://oldschool.runescape.wiki/w/Special:PermanentLink/14986529).


Further verified Fishing progression: native Swan Song-complete monkfish (62/120XP),
Piscarilius anglerfish (82/120XP, sandworms), diabolic anglerfish (79.2XP, published13/100 curve,
diabolic-first priority, 50% faster next roll), and dark crabs (85/130XP, dark bait, independent
Wilderness Elite bonus roll followed by the normal roll). Tynan sells the ten dated supplies,
including sandworms and 100-unit bait/sandworm packs, through native Trade and Open operations.
303 dated ordinary fishing positions now supply the existing and new methods; exact concurrent
spot counts, ordinary spot relocation and unmapped variants remain open. All197 Fishing integration
tests pass in the isolated233.4 cache, including11 new high-tier/shop tests.
Sources: [Raw monkfish14791518](https://oldschool.runescape.wiki/w/Special:PermanentLink/14791518),
[Raw anglerfish14903409](https://oldschool.runescape.wiki/w/Special:PermanentLink/14903409),
[Raw dark crab14980753](https://oldschool.runescape.wiki/w/Special:PermanentLink/14980753),
[Tynan14994365](https://oldschool.runescape.wiki/w/Special:PermanentLink/14994365),
[Diabolic worms14956177](https://oldschool.runescape.wiki/w/Special:PermanentLink/14956177),
[Yama update14902205](https://oldschool.runescape.wiki/w/Special:PermanentLink/14902205).
Diabolic worms are now obtainable from the combat lane's ordinary instanced solo Yama reward
path (90 per successful period standard-table roll, weight7/95.11). Yama contracts and duo
contribution scaling remain in that boss lane's limitations; normal anglerfish supplies also use
Tynan's sandworms.
Latest focused verification also passed all39 Woodcutting tests,31 Smelting tests,
96 Player/Spinning/Crossbow Assembly/Battlestaff tests,12 Impling tests and9 Binding Spell tests.


Kourend gathering now includes native lovakite rocks (65 Mining/60XP,58-tick respawn), sulphur
rocks (42 Mining/35XP, published100/220 success curve,42-tick respawn), and mandatory worn
facemask/gas mask/Slayer helmet for sulphur. Existing furnaces can smelt the resulting lovakite ore.
A single dynamic saltpetre deposit rotates between the five published centres; Konoo follows and
supplies a free normal spade. Digging gives1–2 saltpetre, level/100 double chance, no MiningXP,
capacity rechecks and three local success diary tasks. All58 Mining integration tests pass.
Lovakite's16/100 extraction curve is approximate because the dated article has no chart. Saltpetre's
three-tick action,25-yield depletion and600-tick maximum rotation are bounded playable models;
exact original depletion/cycle constants and deposit footprint remain open. Sulphur gas-cloud
population/hazards, mining enhancers and other secondary rewards remain open.
Sources: [Saltpetre14924216](https://oldschool.runescape.wiki/w/Special:PermanentLink/14924216),
[Deposit14770704](https://oldschool.runescape.wiki/w/Special:PermanentLink/14770704),
[Konoo14895068](https://oldschool.runescape.wiki/w/Special:PermanentLink/14895068),
[Sulphur14905386](https://oldschool.runescape.wiki/w/Special:PermanentLink/14905386),
[Sulphur rock14961380](https://oldschool.runescape.wiki/w/Special:PermanentLink/14961380),
[Lovakite14818863](https://oldschool.runescape.wiki/w/Special:PermanentLink/14818863),
[Lovakite rocks14823948](https://oldschool.runescape.wiki/w/Special:PermanentLink/14823948).


Otto now has his ordinary native NPC, training menu, bed rod retrieval and progressive lessons for
existing heavy-rod/barehand Fishing, bow Firemaking, pyre ships and barbarian Herblore. Each lesson
requires the actual existing successful action before reporting completion; the attack mix is handed
in. Farming dialogue uses native9609/9610 stages0→1→2→3 and enables auto-smash9614, with actual
planting/smashing handled in Farming. Six metal bar/log pairs produce all twelve spears/hastae at
native barbarian anvils, with exact5/20/35/55/75/90 levels and25/50/75/100/125/150XP. Initial training
progress requires the Otto anvil; the two Kandarin tasks require actual local adamant spear/rune
hasta output. Zamorakian conversion costs300,000gp, or150,000 after Kandarin Elite completion;
reverse conversion is free. Broken dragon hasta repair is free and specifically needs no training.
All38 Smelting integration tests pass, including a real rod catch→report→barehand catch→report
sequence and full spear→report→hasta→report training after quest prerequisites.
Separate Tamayu combat-XP reward-claimed state and full original dialogue/menu presentation remain
open; Tai Bwo Wannai Trio native quest progress6 is enforced. Quest progression itself remains TODO.
Sources: [Barbarian Training14988720](https://oldschool.runescape.wiki/w/Special:PermanentLink/14988720),
[Otto14939850](https://oldschool.runescape.wiki/w/Special:PermanentLink/14939850),
[Barbarian anvil14781754](https://oldschool.runescape.wiki/w/Special:PermanentLink/14781754),
[Zamorakian hasta14884734](https://oldschool.runescape.wiki/w/Special:PermanentLink/14884734),
[Broken dragon hasta14824911](https://oldschool.runescape.wiki/w/Special:PermanentLink/14824911).

Zahur's native Make unfinished potion(s) operation now combines all sixteen dated water/blood
recipes for200gp each, using clean noted/unnoted inputs, noted outputs, atomic capacity and coin
checks, and no HerbloreXP. Desert Hard completion or an actually worn Herblore cape is required;
paid NPC production does not claim player-made diary sequences. Water recipes are processed
before the blood variant when both vial types compete for cadantine; exact native preference is
unverified. All193 Herblore integration tests pass. Source: Zahur14994347 above.


Wintertodt now supplies a naturally playable Firemaking activity: native50-base-Firemaking Doors
of Dinh entry, lobby tool/concoction crates, four published pyromancers/braziers, bruma chopping,
four-tick fletching (fletching knife3ticks after first), three-tick feeding, lighting/repairing,
herb picking, manual/Brew'ma potion production, dose-based healing and warmth restoration.
The post-October2024 native warmth meter is0..1000; standard/brazier/snow damage uses the dated
formula and four-warm-item cap. The242 native warm-item variants come from the dated list.
Cold interrupts fletching/feeding while root chopping continues. Warnings precede area/shatter
impacts, shattering damages the adjacent3x3-brazier perimeter, and area targeting requires an open
3x3 square. Fourteen-health pyromancers become incapacitated after two hits and need healing to
resume channeling. Each lit healthy station drains1% per14ticks; feeding grants training XP/points
without directly accelerating energy loss. With all channels stopped, energy recovers1% per35ticks.
FullHP food consumption still restores35% warmth when its heal is at least4; rejuvenation restores
30%. Natural warmth regeneration is8%/minute plus1% per warm item. Entry/leave/logout/death clear
points and temporary supplies, and zero warmth uses the ordinary unsafe death path.

A successful round requires at least500points before subdual and actual continued participation.
It gives baseFiremaking×100XP (the dated outfit exception excludes Pyromancer boosts), native kill
count, Kourend Medium11 and persistent native reward-cart rolls:1+floor(points/500), with the
remaining points supplying a fraction/500 chance of another, capped at8,000owed. Native cache
clientscripts1421/1433 drive the energy/warmth bars, score colour and station icons using their
verified10-int/0-int signatures. Native cart Search/Check/Big-search, outside bank chest,
Ignisia exchanges, extra crates and exact sequential unique rewards are available. The60-row
material table uses the dated calculator's six ordered level-quality cascades; all six skill
categories have3/25 base weight, coins5/25, dynamite/saltpetre1/25 each. Three owned gloves/torches
produce the documented seed substitutions, and Pyromancer pieces choose the least-owned piece
with the documented tie order. Delivery is atomic and owed rolls survive a full inventory.
All14 Wintertodt tests and92 Cooking tests passed in the private packed233.4 cache. The fixtures
include actual500-point success,495-point failure, leaving/death negatives, full Pyromancer outfit
completion-XP exception, native cart geometry/operations, resource acquisition, potion/food warmth,
interrupted kindling, continuing root chopping, shatter perimeter and two-hit pyromancer collapse.

Open Wintertodt parity: random attack selection/cadence and warning delay are explicitly approximate;
root success currently reuses ordinary-tree axe endpoints because exact bruma endpoints are not
published; boosted-level XP details and exact interaction/dialogue animation presentation remain
unverified. Hitpoints cape/regen bracelet/prayer warmth regeneration, combat achievements,
collection-log integration and full Phoenix follower/reclaim management remain open. Material
quantities use Reward Cart14997422 where its limestone3–7 differs from the calculator's older2–7.
Sources: [Wintertodt14982107](https://oldschool.runescape.wiki/w/Special:PermanentLink/14982107),
[Strategies14984578](https://oldschool.runescape.wiki/w/Special:PermanentLink/14984578),
[Reward Cart14997422](https://oldschool.runescape.wiki/w/Special:PermanentLink/14997422),
[Material calculator14860131](https://oldschool.runescape.wiki/w/Special:PermanentLink/14860131),
[Warm clothing14982953](https://oldschool.runescape.wiki/w/Special:PermanentLink/14982953),
[Roots14855876](https://oldschool.runescape.wiki/w/Special:PermanentLink/14855876),
[Kindling14956746](https://oldschool.runescape.wiki/w/Special:PermanentLink/14956746),
[Rejuvenation14993897](https://oldschool.runescape.wiki/w/Special:PermanentLink/14993897),
[Bruma herb14799055](https://oldschool.runescape.wiki/w/Special:PermanentLink/14799055),
[Pyromancer14899060](https://oldschool.runescape.wiki/w/Special:PermanentLink/14899060),
[Brew'ma14971184](https://oldschool.runescape.wiki/w/Special:PermanentLink/14971184),
[Ignisia14995680](https://oldschool.runescape.wiki/w/Special:PermanentLink/14995680),
[Skilling equipment14991143](https://oldschool.runescape.wiki/w/Special:PermanentLink/14991143).

### Fishing Trawler (current pass)

Native Port Khazard gangplanks now board the docked ship, wait one minute, then depart for the cache-native sea boat for a five-minute shared voyage. Departure credits Ardougne Easy #5. Lower-deck leaks consume swamp paste; reusable bailing buckets remove water; rope repairs the net; ordinary usable axes chop the enormous tentacle; hammers repair its rail damage. Contribution uses published 2/5/7/10/10-point actions, capped at255; at least50 contribution and base15 Fishing are required for rewards. Construction XP requires an owned house. Native hull morphs, water/net/contribution HUD, ladders, free tool crates, sinking barrels and logout recovery are wired. Murphy and two Khazard shopkeepers have ordinary published placements; the general store carries all16 dated supplies, including swamp paste, rope and the two empty-container packs.

A successful eligible voyage creates a persistent, one-time catch inventory. Inspecting awards the published per-fish XP; native reward buttons take/bank/drop it, capacity failures preserve the catch, manual closing drops remaining items, and logout preserves uncollected rewards. Fish use the published high-to-low150..200/256 level cascade with unboosted Fishing, including81 manta rays and79 sea turtles. Published Angler1/12 rolls exclude owned normal/spirit equivalents until a full set is owned, supporting the second set; full contribution and Ardougne Elite increase fish quantity. Successful manta delivery or ground delivery records persistent provenance; only subsequent successful cooking in Port Khazard completes Ardougne Elite #1. An ordinary manta without that catch, wrong-town cooking and failed delivery give no progress.

Verified in isolated233.4 cache: **208/208 Fishing integration tests**, including11 new Trawler cases, a full500-tick voyage sustained by native leak interactions, departure timing, below50/below15 rejection, reusable buckets, house-only XP, full inventory/bank delivery, ordinary Cooking success and negative provenance, manual-close/logout persistence, and Angler duplicate exclusion. `packCache` passed before the suite. Source revisions: Fishing Trawler14985191 (2025-09-13), Strategies14977587 (2025-09-01), Murphy14930722 (2025-07-02), Khazard General Store14993208 (2025-09-25), Ardougne Diary14989857 (2025-09-21).

Remaining Trawler parity: hazard spawn cadence, water ingress/bailing amount, tentacle hit count, gross catch quantity/group scaling, action animation/timing details and exact town-border extent are explicitly approximate models. The ordinary-tree axe success curve is source-backed. Departure/return cutscenes, exact native net-damage visual animation, Heron pet/follower, collection log and combat-achievement machinery remain open. This provides normal playable access/training/rewards and both diary mechanisms without claiming exact minigame parity.

### Phoenix feathers and Rogues' Den lobby safes (current pass)

Native Desert Phoenix5137 Grab-feather now requires visible25 Thieving, grants the ordinary phoenix feather and26XP on successful inventory delivery, then Desert Medium #4. A feather or quill already owned across persistent inventories prevents another; full inventory and failure grant no item/XP/diary. Failure stuns movement and deals1 damage. **The unpublished Phoenix success endpoints and6-tick stun are explicitly approximate** (bounded80..240/256 curve), rather than verified rates. Published Phoenix/feather revisions14768269 (2024-10-13),14788009 (2024-10-26); normal spawn3419,3155.

Rogues' Den lobby wall safes7236 now require visible50 Thieving, retry every4ticks using the exact published8..160/256 success curve, or16..192 when carrying the stethoscope. Loot uses the exact128-weight10/20/40coins and sapphire/emerald/ruby/diamond table, granting70XP and Falador Hard #8 only after successful delivery. Failed attempts can continue or trigger the source's estimated half-failure trap model (2–6damage); spawned lobby floor traps can be searched, draining1 Thieving. The completed safe closes again after4ticks. Unpublished individual animation offsets remain approximate. The native pub trapdoor, lobby door and exit passage are connected; Martin and Emerald have ordinary lobby placements, and Martin's11-stock general store supplies25 stethoscopes, knives, claws, lockpicks and basic tools. His50-base-Thieving/50-base-Agility or99-base-Thieving permission correctly rejects boosts. Native skillcape-specific shop variants remain open, as does the separate Rogues' Den maze minigame.

Verified private233.4 pack and **131/131 Thieving integration tests**, including8 new native Phoenix/safe/shop/access cases, owned-item and inventory negatives, exact success/XP/loot, stethoscope threshold and actual trap damage. Sources: wall safe(lobby)14997332 (2025-10-01), Martin Thwait's Lost and Found.14993260 (2025-09-25), Rogues' Den14984160 (2025-09-11). City chests belong to the central travel/diary lane; blackjacking, Pyramid Plunder and artefact theft remain next Thieving progression work.

### Complete gnome recipe loops, spider skewers and combo food (current pass)

All13 handmade gnome foods (four crunchies, five battas, four bowls) and seven cocktails now have normal ingredient acquisition and creation paths. Dough consumes its mould/tin/tray into the raw form; normal Range cooking makes the half-baked form; native Prepare selects the dish and consumes its exact ingredients; post-2006 half-made items bake into unfinished/finished foods and return the mould. Garnishing finishes the remaining foods. Gnome spice is reusable for gnome food, while the existing spicy-sauce recipe correctly consumes it. The exact per-stage XP totals are100/102/104/106 for crunchies,150/152/154/156/158 for battas,170/175/185/190 for bowls; the second bake grants XP only when it finishes toad batta or tangled toads. Raw and second-stage bake curves118..492 stop burning at38; burned foods Empty back into exactly one mould, including when the second bake burns. New returned-container-on-success behavior preserves existing ordinary recipes. Creation requires no tutorial completion.

All seven shaker/mix/pour paths consume the published ingredients, return the reusable shaker on pouring, and apply each garnish. Drunk dragon finishes by heating; chocolate Saturday must be poured, heated and then garnished. The dated heated-Saturday page specifically permits32 Cooking for the heat stage, while mixing and final garnishing require33; this exception is preserved. Cocktail totals50/70/110/120/160/170/180XP are verified through all stages. The existing Herblore knife+chocolate path supplies dust and is reused. Native fruit+knife menus make chunks/slices, including four pineapple rings atomically. Native swamp-toad Remove-legs provides the normal toad ingredient. Sixteen ordinary king-worm spawns cover the Stronghold, Taverley and Molch Island. Hudo's26-stock ingredient shop, Funch's17-stock shop and the waiter's13-stock premade-food shop use their dated stock/rates; these NPCs have normal Grand Tree placements.

`PlayerGnomeCookingSuccess(player,output)` is published only after actual handmade finished delivery (including the final bake where appropriate), for the separate restaurant/tutorial activity lane. Premade, unfinished and burned items do not publish it. Making the final chocolate bomb inside the Grand Tree awards Western Medium #10; other locations and failed ingredient/capacity transactions do not. Both spider carcass+skewer and carcass+arrow-shaft assembly work; normal fire/range cooking requires16 Cooking, gives80XP, uses91..438 success endpoints (stop48) and awards Karamja Medium #6 anywhere only on success. Combat's companion Jungle-spider carcass drops and Cleanup's spar→skewer procurement provide ordinary inputs.

All13 handmade foods and their premade counterparts now Eat with the independent three-tick combo timer and two-tick existing-attack penalty. Cooked karambwan uses the same timer for18 healing; ordinary-food→combo works in the same tick and reversed/duplicate combo ordering is blocked. Battas heal11, as corroborated by the dated fast-food table and pinned RuneLite primary source, despite two individual wiki intros saying12. Cocktails have native healing/Strength boosts and Attack drain based on current Attack; glass return and their three-tick consumption/two-tick attack timing are explicitly an unverified presentation/consumption model. Spider eating rolls7–10 healing, returns the skewer and destroys an arrow shaft. Consumption publishes the existing food-healing event for Wintertodt.

Verified: **107/107 Cooking integration cases**, including15 new cases covering every complete food/cocktail path, exact XP, reusable spice/moulds, highest Prepare/Mix menu choices, burning, inventory failure, location checks, native spider preparation/cooking, atomic four-ring cutting, combo ordering/attack delay, buffs and spider consumption. The shared food/cooking consumer suites also passed: **14 Wintertodt** and **11 Fishing Trawler** cases. Private pack passed before the source-only heating-level correction, whose catalogue and full Cooking suite were then recompiled and verified. Restaurant assignment/tutorial/hand-in state belongs to the activity lane and is verified there.

Sources: Gnome cooking14833704 (2025-01-08), Gnome Restaurant14991892 (2025-09-25), individual finished foods14980952–61/14854512–16, individual native half-made stages14421368–80 (2023-06-16), mixed cocktails14422396–406/14924058–66, heated Saturday14434472 (2023-07-10), Grand Tree Groceries14991747, Funch14991764, Gianne's Restaurant14991772 (all2025-09-25), spider stick14787734 and shaft14787735 (2024-10-26), Food/Fast foods14979677 and table14982451 (September2025). RuneLite ItemStatChanges pinned commit9d1e468461caaddf46bf30cff43bda55171ee9db (2025-07-21) corroborates healing and current-Attack drain. Remaining: native full-screen recipe presentation/book details, exact nonbaking animation/action timing and alcoholic consumption container/timer parity; eating raw gnome ingredients and premade cocktail bar stock remain open. Ordinary recipe/training/delivery inputs and both production diaries are functional.


### Ent trunks and Woodcutting Guild dungeon (current pass)

Native Ent-trunk Chop now checks credited-killer ownership on entry and again before delivery, and requires an ordinary usable axe. The dated Ent calculator14807078 (2024-11-24) supplies the exact base-level/axe log table, visible-level16..90 success curve and Wilderness Medium22..102 curve. Successful deliveries are noted, one log in the Guild or two in the Wilderness, and grant25 Woodcutting XP per set. Wilderness yew success awards Wilderness Medium #2; boosted base60 accounts cannot roll the base61 yew column. Full inventory, ownership/axe loss during the delay and unsuccessful rolls grant no log, XP or diary. Existing note stacks can extend in a full inventory. Crystal charge and normal nest machinery are reused; Ent-specific nest details and special-axe table offsets remain unverified, and the four-tick chop cadence is explicitly approximate.

Combat's companion native Ent deaths create an owner-only trunk lasting100 ticks, with normal Guild and Wilderness spawns. The native Guild cave/vine now connects the surface and dungeon with collision-checked adjacent exit tiles. The inner dungeon correctly requires unboosted60 Woodcutting, unlike the boostable surface gate, per Woodcutting Guild14975699 (2025-08-28). Verified private233.4 pack and **48/48 Woodcutting integration cases**, including9 new Ent cases covering ownership, exact loot/XP, diary curve, base-level selection, capacity, nested loot and both native cave operations.


### Native blackjacking (current pass)

Both Pollnivneach bandit tiers and Menaphite thugs now expose functional native Lure/Knock-Out handlers through their quest-state NPC morphs. Native Feud varbit340 index2 is required, as are visible45/55/65 Thieving and one of the nine wielded blackjacks. All blackjack tiers share the published80..240/256 knockout curve and10XP; a conflicting78 low on Menaphite14978160 is resolved in favour of the subject's explicit shared-curve statement and Blackjack14898817. Bandits refuse knockout with a second nearby visible bandit; combat also blocks it. Lure routes the target toward the player from line of sight within8 tiles. Its presentation and maximum range are an explicit playable model. Native placements use quest-morph NPCs for9 bandits,10 thugs and the seller; the two bandit appearances alternate among the dated combined placements because that source does not identify each appearance at each tile.

Successful knockout reserves two guaranteed rapid pickpockets to that player, with a five-tick expiry model. Those attempts deliver direct40/50/60 coins and65/84.3/137.5XP, support the ordinary rogue outfit bonus and bypass coin-pouch limits. Full inventory can extend an existing coin stack; a failed transaction grants no theft XP/diary and does not consume a reserved pick. A real knocked-out Menaphite theft completes Desert Hard #1, while knockout alone, expiry, failure and another player's knockout do not. Failed knockouts may queue normal combat retaliation (unpublished half-failure probability is explicitly approximate); a subsequent pickpocket or successful knockout clears that retaliation. Logout/deletion clears ownership. The seller's normal dialogue offers an atomic600-coin willow blackjack purchase after native quest state28.

Verified private233.4 pack and **10/10 new Blackjacking integration cases**. The final combined Thieving suite passed **145/145 cases** after syncing the central lane's verified routefinder block-access rotation correction and matching city-chest fixture coordinates. Sources: Bandit14990770 (2025-09-23), Blackjack14898817 (2025-05-08), Menaphite14978160 (2025-09-02), seller14974099 (2025-08-26), The Feud14986366 (2025-09-15); native233.4 varbit/morph/operation definitions. Remaining: exact knockout/aggression timing and chances, full lure dialogue semantics, ordinary awake bandit pickpocket probabilities (still unpublished), quest progression itself and Rogue Trader's offensive/defensive shop unlock. Awake Menaphite pickpocketing retains its existing sourced ordinary path.


### Repeatable Arzinian mining (current pass)

Dondakan now offers cannon entry at native Between a Rock... stage90 or later, with the gold helmet worn and membership checked. This admits the actual quest mining stage as well as completed accounts without implementing quest progression or the Avatar encounter. Removing the helmet triggers the one-tick watch and returns the player outside; logout/login also relocate out, preserving the published exit rule. Normal Dondakan, ore-hauler and both ferryman placements are present. Ferry crossings cost2 coins, waive the fee for worn ring of charos(a), and allow a penniless return from the mine side. Cannon landing/cutscene and exact ferry presentation timing remain an explicit implementation model.

All77 native gold-vein placements now reuse ordinary gold's40 Mining/65XP/7..75 extraction endpoints and100-tick respawn, preserving their native depleted-wall form. The source147 total includes ordinary gold rocks, which already worked. Successful gold delivery from either native vein or ordinary mine rock awards Fremennik Medium #9 only with appropriate quest progress and helmet; missing pickaxe, full inventory and failed access do not. The ore-hauler accepts at least two ores after native completed stage110 and banks the remainder atomically. His dated subject-specific charge is floor(n/5)+1, or floor(n/10)+1 with charos(a), rather than the mine overview's conflicting ceiling formula; a full bank preserves all input.

Verified private233.4 pack and **65/65 Mining integration cases**, including7 new mine cases for access/helmet removal, native rotated wall-vein mining, ordinary-rock diary credit, tool/capacity rejection, exact25-ore charges, full-bank rollback and ferry fees. The native rock catalogue now covers36 ordinary definitions, including the3 added veins. Sources: Dondakan mine14997074 (2025-09-30), Dondakan14913206 (2025-06-01), ore-hauler14923820 (2025-06-22), ferries14768113 (2024-10-13), native233.4 locations/quest varbit299 and pinned Quest Helper241eaec29b19243bda7e88e99d5c16568c0776a6 (2025-08-27) for the90/100/complete progression boundary. Quest completion itself, ore-responsive Avatar fight and exact visual travel sequence remain open.


### Repeatable Elemental Workshop production (current pass)

The existing combat lane retains the native surface odd-wall/key/battered-book/slashing and first-stair handlers; the new production module composes with those handlers. Native Exam Centre bookcase17382 now reclaims a beaten book without duplicating an owned copy. The deeper hatch requires native Workshop II stage5 or later. All priming, catwalk and mind-room stairs/doors are connected. Each account receives a copy of the native second/third-level geometry, with one cart reflecting its persistent native machine bits. This isolation is an explicit server implementation model; multiplayer visual parity has not been measured. Logout saves the player at the first workshop floor and destroys that account's cart; re-entry reconstructs the same material/state, avoiding loss or duplication.

The actual ore-on-elemental-furnace interaction consumes one elemental ore/four coal in6 ticks at20 Smithing for8XP; a smithing catalyst uses two coal, consumes one catalyst and grants16XP. Each click makes one bar. The4-tick native workbench recipe menu makes elemental shield/helmet at20 Smithing/20XP or mind shield/helmet at30/30XP, requiring a hammer and the appropriate shield/helmet book. Elemental helmets require completed Workshop II stage11; mind equipment permits the native partial quest stage. Actual delivered mind helmets award Kandarin Medium #9. Missing level/book/hammer/progress, ingredient loss during the action and invalid inventory delivery grant no product or diary. Books/hammers remain reusable.

The full repeatable priming sequence works through the native9 lever/valve controls and persistent varbits: cart placement; lowering/raising/rotating the repaired crane and heating in lava; pressing only the hot bar at the press station; opening the tank and turning the corkscrew twice; closing/filling/cooling/draining and withdrawing it; then drying under the repaired fan and returning to the start. Each stage checks order and the existing native crane/press/pipe/cog repair state. The native cart transmog/position and loc morph variables follow material changes. Taking from a full cart or extractor preserves its material. The extractor accepts a primed bar, requires20 current Magic, drains exactly20 without Magic XP, and yields a primed mind bar. The independent combat companion supplies normal dormant-rock→earth-elemental→ore procurement.

Verified private233.4 pack and **50/50 Smithing smelting/anvil integration cases**, including12 Workshop cases: real ore-on-furnace, standard/catalyzed smelting, all4 finished products, native fourth Smith-menu option, full four-station sequence, wrong-order/unrepaired controls,20-Magic extraction, full inventory, interrupted actions, two-account isolation, beaten-book native approach, quest gates and logout reconstruction. Native bookcase rotation and all landing tiles were checked against the233.4 map. Sources: Workshop II14997203 (2025-09-30), Workshop location14905073 (2025-05-20), metal14734897 (2024-08-31), primed bar14834636 (2025-01-11), mind bar14740441 (2024-09-05), mind helmet14846113 (2025-02-02), workbench14964291 (2025-08-12), elemental helmet14740444, shield14931530, mind shield14931529, beaten book14680500 and native233.4 types/geometry. Native machine-state meanings corroborated by pinned Quest Helper3e3b8bdc7d4fee88934b7e2db501667d6cb4f39b (2025-09-28).

Open Workshop parity: quest progression, repair puzzles and schematic/crate quest state remain TODO; completed/appropriately repaired native save state is required for repeatable machinery. Per-control2-tick and extraction4-tick timing, cart movement transitions, machine effects/animations, full recipe-book text and exact reclaim/drop-trick conditions are explicit approximations or unverified. Published furnace/final-smith timings, ingredients, XP and20-Magic cost are implemented, without claiming the entire quest or exact machine presentation.


### Motherlode Mine training, processing and rewards (current pass)

All266 depleted vein records in the native233.4 map now start as their matching live single/left/centre/right veins. The source-derived binary loc resource preserves all3,297 records and every coordinate/shape/rotation, changing only those266 IDs; its packed output was compared record-for-record. There are205 encoded lower and61 encoded upper vein records in this cache, regardless of the wiki overview's older64-upper claim. The upper veins, ladder, hopper, sack and wheel struts resolve through native LINK_BELOW flags to server plane0. Upper-vein eligibility uses their actual native coordinates, rather than assuming a different server plane.

Pay-dirt mining requires unboosted30 Mining (57 plus Percy's permission upstairs), a usable ordinary pickaxe, real inventory space and a nonfull sack. The published60..105 chance and60XP apply; the ordinary gem pre-table/glory/gem-bag path is reused with no gem XP/geodes. Each successful pay-dirt stores its selected outcome immediately in its persistent item variables: exact1/32 nugget roll, then level-gated descending runite(-20..5), adamant(-90..50), mithril(-19..90), gold(-40..126), otherwise coal. Visible Mining boosts therefore affect the ore when it is mined, and subsequent drains/cleaning boosts cannot change it. The published approximate Elite Falador increases are represented by +3/+6/+3 endpoints for runite/adamant/mithril; this integer model is explicitly unverified. Native pay-dirt is unbankable.

Vein lifetime begins on the first actual pay-dirt, using the dated lower23–27s and upper36–40s windows converted to bounded39–45/60–67-cycle intervals. Exact rounding remains an implementation model. Timed-out unmined veins retain one last ore; lower-level respawn time subtracts the elapsed timeout, and an unused upper vein resets after100ticks. Depleted veins restore after the published100ticks. Rockfalls accept any usable pickaxe without30 Mining, grant10XP and restore after the subject page's18seconds(30ticks); their four-tick mining action is approximate. Normal tools are available from the six native crates, split three hammer/three bronze-pickaxe; the exact per-crate tool assignment is unverified.

The normal hopper atomically accepts only available108/189 sack capacity and preserves excess inventory pay-dirt. The Super Hopper requires its50-nugget unlock. Both waterwheel struts initially block flow; actual hammer repair retries using the source12%..27% Smithing endpoints, grants1.5×current Smithing XP and Falador Easy #9, and restores shared flow. Repairs last two/three/four58-second rounds, modelled as97cycles per round with equal round weights (round-weight/rounding parity unverified). Per-item14tick washing countdowns pause when both wheels are broken and persist across logout. Completing a wash atomically moves its predetermined ore to a separate persistent sack and grants15/15/30/45/75 bonus Mining XP, including current XP modifiers; nuggets grant no wash XP and excess nuggets above3 in a deposited batch are deleted. Processing continues while bank/modal interfaces are open. Collection handles partial ore space and stacked nuggets without losing remaining rewards. The native count/sack morph/HUD are updated and show108/189 capacity.

Native entrances/exits, the upper ladder, two54-Agility tunnels (northern route additionally needs the Medium diary), bank chest and Percy placement are wired. His dialogue shop offers all8 dated goods:40/60/50/30 nugget Prospector pieces,100 each coal/gem bags,10 soft-clay pack and40 gem pack. Outfit purchases carry provenance for correct80% refunds; clothing obtained elsewhere is rejected. Store-specific consumable packs also refund80%. Upper floor100, Super Hopper50 and larger sack200 upgrades are persistent, atomic and reject repeats/boost-only eligibility. Ordinary/golden Prospector helmets now complete Falador Hard #6 when actually entering the Mining Guild through its Motherlode exit with visible60 Mining; the post-2024 task correctly requires a helmet only.

All three native soft-clay-pack variants open for100 noted clay, including the dated free-world allowance. All three gem-pack variants open for40 noted gems using the common distribution confirmed by Mod Ash: onyx1/100,000,000 per gem and the source's explicitly estimated4990/3459/1202/299/50 weights for other gems. Preflight reserves all possible output slots before rolling; insufficient space preserves the pack. Exact native preflight/presentation and onyx collection-log integration remain open.

Verified private233.4 pack and **78/78 Mining integration cases**, including13 Motherlode cases for base-level gates, stored boosted runite, wash timing/XP and halted flow, three-nugget cap, both capacities/excess input, partial/full inventory, actual native strut operation/current-level XP/diary,180-nugget outfit purchase/refunds, permanent upgrade negatives, reward cascade, unbankability, actual Guild entry, both consumable outputs and real hopper processing through an open bank modal. The map comparison independently confirms exact preservation of all non-vein records. Sources: Motherlode14991779 (2025-09-25), pay-dirt14812739 (2024-11-28), vein14919492 (2025-06-14), strut14993536 (2025-09-25), sack14970260 (2025-08-21), hopper14843928 (2025-01-29), Percy14992638/shop14993361 (2025-09-25), kit14989966 (2025-09-22), rockfall14630877 (2024-04-13), clay pack14924221 (2025-06-22), gem pack14987013 (2025-09-16), Mining Guild14965341 (2025-08-14), native233.4 map/types and HUD clientscripts1633–35.

Remaining Motherlode parity: rockfall collapse warning/displacement/damage, autonomous miner NPCs, exact gem pre-table placement, infernal tool charge/enhancer details (crystal charges and celestial enhancements are implemented below), exact strut success/round weights, mining/action animation offsets, finite reward-shop stock/full-screen presentation and bag-resale behavior beyond the implemented clothing/packs remain open. The native playable mining→wash→ore/nugget→equipment progression and both diary actions are functional; unverified models above are not labelled exact parity.


## Piscarilius artefact stealing

Implemented normal Captain Khaled assignments and six cache-native house drawers, lockpick/hairclip
requirements, native linked-floor house ladders, one stolen item per job, 750 Thieving XP on actual
theft, and 40 times the current Thieving level plus 500–1000 coins on successful delivery. The native
4903 target and 4905 stolen-item bits drive the job and captain morph. Membership and visible level
49 are checked; removed favour does not gate the activity. A failed inventory/payment transaction
keeps the relevant state and gives no XP or completion. Delivery grants **Kourend Hard #8** only
after Khaled actually takes the job's artefact and delivers payment.

All eight native patrol variants have ordinary bridge placements and moving patrols. Four-tile
forward-cone detection, two-tick reaction, route anchors and patrol cadence are an explicitly
approximate playable model; exact historical guard paths and lure persistence remain unverified.
Accepted standard teleports, logout, destruction, loss of the item and nonstandard long movement
invalidate the job and confiscate the goods. Ordinary running and house ladders preserve it. The
watch also protects against delivering another job's item. Random-event traversal is caught by the
movement check, but there is no activity-specific random-event integration. Captain dialogue is
functional and condensed; clue dialogues and collection counters remain separate.

Geometry checks place Khaled at the activity map's **1845,3752**, beside the table. The NPC page's
1847,3755 marker is cache table27689 and cannot be used as a reachable NPC square. Confiscation
returns the player to walkable **1805,3730**, immediately north of Leenz's shop; 1805,3729 is its wall.
Sources: Stealing artefacts **14989711 (2025-09-21)**, Captain Khaled **14924152 (2025-06-22)**,
Drawers **14939120 (2025-07-16)**, Patrolman/Patrolwoman **14768581/14768582 (2024-10-13)**,
stolen items **14839558–14839562 (2025-01-19)** and jewelry box **14932047 (2025-07-04)**,
Leenz's General Supplies **14805770 (2024-11-22)**, native map/cache233.4. Assignment weights and
four-tick lock presentation are approximate; published requirements, XP, reward range and loss
semantics are reproduced. Private pack and **156/156 Thieving integration cases**, including
11 new cases with every drawer, negative gates/capacity, automatic patrol movement, logout and
teleport confiscation, linked ladders and an actual movement-packet journey to native NPC hand-in,
passed in `/tmp/rs233-production-artefact-final-tests.log`.


## Native worn operation correction

Worn equipment Op1 is Remove; native wear_op1 starts at Op2. Corrected the Hunter cape's Boost,
Hunter Guild, Carnivorous Chinchompas and Black Chinchompas actions to Op2–Op5, and silence-glove
and forging-ring Check to Op2. The inventory variants remain on their native slots. Actual
WornInteractions integration now checks Boost, charge/damage inspection and successful ordinary
removal; the existing native cache strings are also pinned. Other tool-specific worn handlers in
Fishing, Woodcutting, Mining, Firemaking and blowpipe production were audited and already use the
correct Op2/Op3 mapping. Private serial full suites passed **Hunter Experts22/22, Thieving157/157,
Smelting51/51** in `/tmp/rs233-production-worn-ops-tests.log`; formatter passed afterward. This
source-only correction needs no cache repack.


## Crystal pickaxe lifecycle and interruptible gathering

Crystal pickaxes now consume one native item-variable charge on ordinary ore and rune/pure essence
successes and Motherlode pay-dirt delivery. They begin at 10,000 charges when their initial state is
implicit, accept 100 charges per crystal shard up to 20,000, and become inactive on the last charge;
inactive tools retain the existing dragon-speed fallback and requirements. Native inventory/worn
Check, shard use and dismantling return the dragon pickaxe and crystal tool seed atomically.
Dismantling fails without losing the original tool when the returned materials will not fit.
Elven/celestial signets save 10%, and Ghommal's lucky penny saves 5%, with independent rolls.

The public consumer is
`CrystalPickaxeLifecycle.consumeCharge(access: ProtectedAccess, tool: InvObj): Boolean`.
It locates the actual selected worn/carried tool before changing it and returns false for a different
or missing tool. Call after successful delivery only. Dense essence and Motherlode rockfalls remain
excluded, as are the other no-benefit actions listed in the period article. Crystal pickaxe singing
and complete normal material procurement remain a separate progression gap; this change supplies
the charge lifecycle for already obtained tools. Sources: Crystal pickaxe
[14915165 (2025-06-04)](https://oldschool.runescape.wiki/w/Special:PermanentLink/14915165),
Elven signet14989911 (2025-09-21), Celestial signet14888892 (2025-04-21), Ghommal's lucky penny14964658
(2025-08-13).

`ProtectedAccess.gatheringDelay(cycles)` pauses only the coroutine until the next roll, retaining the
normal modal-resume guard. It does not set the hard player delay that makes native move/op packets
discard input. Motherlode mining/strut/rockfalls, saltpetre, ordinary/big-net/karambwan/karambwanji
fishing, Trawler skilling actions, Ent harvesting and safe cracking now use this cancellable wait
with unchanged yield intervals. Native movement packets now cancel pending Motherlode, fishing and
Ent yields; no XP/resource is delivered after cancellation. This also prevents an indefinite
gathering coroutine from trapping a player between hard delays. Crafting/singing/dialogue and
intentional blocking animation waits are outside this helper's scope.

Private focused Mining23/23 passed, including eight new charge-lifecycle cases and two Motherlode
charge/movement cases. Full serial regressions passed **Mining88/88, Fishing209/209,
Woodcutting49/49 and Thieving157/157 (503 total)**. The new Ent fixture initially needed its known
test UUID unwrapped; after that fixture correction all 49 Woodcutting cases passed. Logs:
`/tmp/rs233-production-crystal-gathering-all.log` and
`/tmp/rs233-production-gathering-woodcutting.log`. Sources and native behavior changed only;
no cache repack is needed.


## Celestial enhancement integration

Ordinary ore, gemstone and quarry success rolls now receive the shared celestial ring/signet's
invisible +4, in addition to the existing invisible level providers. Visible requirements remain
unchanged. Motherlode's pay-dirt success roll also receives +4, while its charged extra-resource
effect remains excluded.

The charged effect uses the shared CelestialMiningRing API after the primary resource is produced,
once per successful eligible node. Existing clay/metal ores through adamantite, quarry sandstone
and granite, lovakite and volcanic sulphur are eligible; native limestone3211 is included for its
future extraction path. Runite, gem rocks, dense/rune/pure essence, Motherlode pay-dirt and saltpetre
do not spend stardust for extra resources. A successful bonus gives a second copy of the actual
rolled quarry size or ore. It grants extra Mining XP only after inventory or open coal-bag delivery
succeeds; the stardust charge is still spent when a full bag prevents that bonus, as the period
source specifies. Kingdom donations retain their separate no-inventory-result flow.

Sources: Mining enhancers14917182 (2025-06-08), Celestial ring14997338 (2025-10-01), and Celestial
signet14888892 (2025-04-21). Six new ordinary-mining cases verify charged ore/XP, full-bag behavior,
the uncharged level boost and visible gate, runite exclusion, failed-roll preservation, and exact
granite size/XP. A seventh case verifies Motherlode's boost without charge use or duplication.
Private focused22/22 and full **Mining95/95** passed in
`/tmp/rs233-production-celestial-all.log`. The shared ring charging/withdrawal UI is owned
and separately verified by the Scar Runecraft lane. No cache repack is needed.


## Pyramid Plunder and Pharaoh's sceptre

The native eight-room pyramid now supports ordinary rotating entrance searches, the guardian's
start/recharge conversations, unboosted21–91 room gates, a five-minute timer, spear traps,
103 placed urns, snake checking/charming, four door searches per room, gold chests, sarcophagi,
owner-only mummies/scarabs, poison, normal dangerous death, expiry/logout/teleport cleanup and
native HUD/room varbits. Opening the first-room sarcophagus awards **Desert Easy9**; searching
the final-room chest awards **Desert Elite5**. Artefacts also feed the existing Simon merchant.
A normally acquired snake charm and basket cost two coins at Ali's money pot, with banked/carried
charm and full-inventory checks. Native initial/charged/empty sceptres support held/worn actions,
all four destinations, Necropolis obelisk attunement, level30 Wilderness/teleblock rules, and
mixed-shape/noted material recharge with claimed Desert-reward capacities3/10/25/50/100.

Published chest/sarcophagus quarter tables, sceptre rates, room levels, total XP, charge prices,
capacities and destinations are implemented directly. Urn artefact weights, check/search XP split,
search/open/teleport timing, mummy occurrence, scarab poison cadence, entrance punch and some
combat presentation remain explicitly approximate models. Correct doors are shared and change
when another game begins, with a fourth-distinct-door fallback. Quest progression and the exact
original dialogue/HUD timer direction require further observation. Bone-producing credited kills
publish RemainsDrop before ground delivery and commit conversion only after successful delivery;
other-player/environmental events cannot consume or duplicate the owner's reward.

Pinned sources: Pyramid Plunder14960285 (2025-08-06), urn14905374 (2025-05-20), chest14979738
(2025-09-04), sarcophagus14832454 (2025-01-06), tomb door14968990 (2025-08-19), guardian14768303
(2024-10-13), sceptre14963392 (2025-08-11), snake charm14987125 (2025-09-16), Ali14768492
(2024-10-13) and Simon14996611 (2025-09-29). Private packCache passed and focused **20/20** plus
full **Thieving177/177** passed in /tmp/rs233-production-pyramid-all.log. New NPC metadata and
placements require a shared cache repack.


## Blast Furnace

Ordinary Keldagrim factory stairs, Foreman and Ordan now provide access to the native furnace.
The Foreman's conversation grants free use at unboosted60 Smithing and awards **Fremennik Hard9**;
otherwise the native Pay operation consumes2500 coins (1250 with equipped activated Charos)
for1000 basement activity cycles. The seven-ore Ordan shop has its dated100-each stock and200-tick
restock with static prices. Native persistent ore/bar/coffer bits drive all eight metals, half-coal
costs, guaranteed iron, current-equipment goldsmith/Smithing-cape XP, deposited catalysts doubling
coal-bar XP, coal/tin254 and primary ore/bar28 capacities. Coal-before-iron ordering matters.
XP arrives on production, with owner-specific retained bars and no inventory delivery until Take.

The coffer accepts/withdraws coins atomically, drains12/tick inside the basement and stops on exit
or logout. Hot bars cool naturally or with a water bucket, which returns its empty container;
ice/smiths gloves(i) collect hot bars immediately. Full bags preserve stored bars. Native anvils
require unboosted60 independently of the Foreman's paid permission. Manual stations support
knife-free coke shovelling/refuelling with a normal spade and30 Firemaking (5XP),30 Strength pump
(2XP/tick),30 Agility pedals (1XP/tick and0.5% energy), shared cooperative machinery, token1.5
Smithing XP contributions with10-tick cooldown, and four breakable native cogs/belt/pipe components
requiring30 Crafting and a hammer for50XP. Pumping, pedalling and repairs use interruptible waits;
walking cancels them without trapping the player in protected access.

The current server has no themed-world activity selection. Funding the coffer therefore opts
that account into paid automation locally; uncharged accounts can operate the shared manual
stations. This is a deliberate playable model, not verified world-selection parity. Conveyor2-tick
and cooling10-tick durations, fuel consumption, temperature/overheating curves, repair3-tick duration,
1/500-per-station-tick break odds, manual contribution recency and paused-outside Foreman permit
expiry are approximate. Original dispenser choice-interface presentation, five moving dwarf
workers, Ordan's random45-tick miner deliveries, full machine models and Chromium ingot service
remain open. Published recipe costs, requirements, XP and fee/storage limits are implemented.

Pinned sources: Blast Furnace14991663 (2025-09-25), Foreman14922198 (2025-06-18), dispenser14862870,
coffer14862873 and conveyor14862872 (2025-03-15), Ordan14991983 (2025-09-25), catalyst14784555
(2024-10-22), stove14853615 (2025-02-20), pump14968665 (2025-08-18), pedals14848981 (2025-02-07),
and repair articles fetched at the same2025-10-01 cutoff. Private packCache and **19/19** new
Blast Furnace cases passed; full **Smithing smelting70/70** passed in
/tmp/rs233-production-blast-all.log. Metadata/NPC changes require a shared repack.


## Ordinary deadfalls and passive trap routing

The five ordinary deadfall families now use45 native boulders and90 dated creature placements:
wild kebbit23/128XP, barb-tailed33/168XP, prickly37/204XP, sabre-toothed51/200XP, and pyre fox57/222XP.
Native Set-trap, Investigate, Dismantle, Check, creature hunting, transitions and restoration are
connected. Normal logs and a knife (including wielded fletching knife) build a single owned deadfall;
arctic pine/redwood logs are excluded. All portable trap families share the overall trap allowance.
Bait and smoke add their published3/2 percentage points. Bones and exact creature products are
awarded atomically on successful Check, with no XP or conversion commits on full bags; others
cannot collect the trap. Native Auburnvale wild-kebbit boulders are included. Player movement or
loss of the tool during building preserves logs. Expiry/logout restores the boulder and releases
ownership. These catches now supply normal harpoons, kebbit claws/spikes/teeth, meat and fox fur.

A true passive-catch regression exposed a generic engine gap: NPC scenery operations walked
straight into a solid loc's origin and stalled on blocked approach faces. AiLocInteractions now
uses the existing RouteFactory with native dimensions, angle and approach flags. Passive catches
are verified without issuing an NPC interaction for deadfalls, birds, chinchompas and all five
salamanders. Deadfall hunting skips a line-of-sight check to the opaque rock's blocked origin,
then uses the actual collision-aware route and contact check; its creatures permit overlap so a
hunter standing by the trap cannot obstruct the catch. Published player-standing behavior is
preserved. The catch curve, three-tick construction, two-tick transition,100-tick idle/full expiry
and50-tick creature respawn are explicitly approximate where period data lacks exact values.
Maniacal-monkey deadfalls, Rumour rare parts and exact model/presentation parity remain open.

Pinned sources: Deadfall14986793 (2025-09-15), wild kebbit14963113 (2025-08-10), barb-tailed14978285
(2025-09-02), prickly14988825 (2025-09-19), sabre-toothed14906725 (2025-05-23), pyre fox14924249
(2025-06-22), plus native cache map geometry for the two Auburnvale boulders. Private packCache,
**14/14** deadfall and **3/3** passive-acquisition cases passed. The full trap module passed
**41/41** in /tmp/rs233-production-deadfall-all.log. The NPC metadata/placements and new hunt/controller
symbols require a shared repack; the generic scenery routing fix is source-only.


## Hunter material delivery, pouches and spike bolts

Successful deadfall, pitfall and falcon retrieval now route suitable meat/fur into the largest
carried open pouch. Normal loot, both pouch families and capacity overflow participate in one
inventory transaction; failure preserves every resource and the catch, with no XP or RemainsDrop
conversion commit. Closed pouches and raw bird/beast meat retain ordinary inventory delivery.
This connects the pre-existing pouch API, previously used only by its own tests, to real catches.
The June4,2025 update's meat-pouch Empty now works near a bank or on the Hunter Guild's top floor;
partial inventory space preserves the remainder. Fur pouches retain their direct-to-bank rule.
Native world-map TzHaar sections prevent meat withdrawal in both Mor Ul Rek areas. Near-bank
proximity is modelled as four tiles; exact native banking-area boundaries remain unverified.
Guild ropes now provide normal base46-Hunter access between the ground and Roost at native
plane2, including the return rope, so the top-floor service is physically reachable.

Prickly kebbit spikes now chisel into12 kebbit bolts at32 Fletching/5.8XP in3 ticks; long spikes
make12 long bolts at42/7.9XP in2 ticks. Both use native reversible item-on-item interactions,
quantity selection, current-level gates and atomic ingredient/product changes. Hunting-bolt
production, including existing antlers, now permits movement cancellation between clicks;
interrupted waits preserve their material. The native hunting-bolts animation is used. Prickly
spikes have ordinary deadfall procurement; long-spike tracking procurement is the next gap.

Sources: small fur pouch14863238 (2025-03-16), small meat pouch14915337 and official update14915246
(2025-06-04), Hunter Guild14968516 (2025-08-18), kebbit bolts14613870 and long bolts14613869
(2024-03-27), with native rope geometry and world-map source sections. New focused coverage:
8 atomic pouch/emptying cases, actual full-bag converted-bone deadfall and falcon catches,2 Guild
rope cases, and6 spike-fletching cases. Full private serial results: **Crafting pouches23/23,
Hunter traps42/42, Falconry9/9, Moths/Guild17/17 and hunting-bolt Fletching11/11 (102 total)** in
/tmp/rs233-production-hunter-materials-all.log. These are source changes using existing native
items/locs/sequences; no additional cache repack is required beyond the earlier deadfall pack.


## Prepared food consumption

Eleven edible pies (22 native full/half forms), cakes/chocolate cakes (six forms), and four
pizzas (eight forms) now use native Eat with selected-slot atomic replacement. Empty pies return
a dish, stew/curry a bowl, and pizzas may be recombined from two real halves. Cake delays are
2/2/3, free-to-play pie and pizza delays1/2, and members' pies1/1, with the ordinary three-tick
penalty only on an already pending attack. Pie skill boosts apply per bite without stacking;
summer pie restores10% run energy. Cooked chompy, ugthanki and lava eel are edible.

Anglerfish use the dated floor formula and cap at base Hitpoints plus their heal; active combat
in Wilderness/PvP areas disables overhealing. Nine Hunter meats apply their initial heal and
replace the pending secondary effect for exactly seven ticks later. Ordinary/combo foods do not
cancel it. Dashing energy and moonlight poison cure/venom-to-poison apply on that secondary heal;
existing poison immunity is preserved. Logout/death clear pending meat effects. Timer116 is
`cooking_hunter_meat_heal` and requires cache packing.

Sources: Food/Fast foods14979677, fast-food table14982451; anglerfish14994511
(2025-09-26), Hunter meats table14666437 (2024-05-21), dashing14873763 and
moonlight14963028. Native233.4 operations and item forms are checked by integration fixtures.
The new12 cases passed; the complete Cooking suite also passed (see private verification log
`/tmp/rs233-production-prepared-food-all.log`). This does not claim every specialist food has
a consumption handler; brewing and the remaining output-to-consumer audit remain open.


## Native noose-wand tracking

Five native methods are playable: Polar(1/30XP), Common(3/36XP), Feldip weasel(7/48XP),
Desert devil(13/66XP) and Razor-backed(49/348.5XP). Twelve native burrows, their actual
clue scenery/end objects and75 native footprint segments are mapped. Each player owns a separate
trail; inspecting the right scenery reveals the next segment, Search identifies the final hiding
place and native Attack requires a worn noose wand. Native polar steps now reach walkable upper
hunting-floor landings and return. Successful catches deliver bones/raw beast meat and the exact
fur/spike, with atomic pouch delivery and RemainsDrop commit only after all loot succeeds. A full
bag preserves the trail; missing wand or a failed catch resets it; movement cancels the one-tick
catch. Logout/death clear only that player's tracks. Ring of pursuit reveals the entire path with
one of ten charges, supports native held/worn Check and Break, and disappears on its final charge.

The four mainland networks are reconstructed from cache233.4 footprint geometry, native varbits
and associated map scenery. Trails never reuse an edge or visit a node more than twice, and stop
at the first available hiding node after at least three segments, following the documented Razor
algorithm. Applying that path selection to other creatures, hidden polar tunnel connections,
footprint brightness/direction interpretation and one-tick catch timing are explicit models pending
client observation. Polar success uses the published238..1080 curve; the other species use an
explicit approximate93% at requirement rising to100% two levels later. Unknown-rate old boots,
The later Hunter progression work added guild rumour rare parts, herbiboar and maniacal monkey hunting. No additional custom cache IDs
are needed.

Sources: Polar14902057, Common14783472, Feldip14964943, Desert14781174, Razor14978827
(including detailed native graph rules), Tracking14957966, Noose wand14990139, and Ring of
pursuit14658514, all before the1October2025 cutoff. **14 new integration cases passed**,
including every real burrow and five complete native operation paths. Full private trap/tracking
suite **56/56** passed, log`/tmp/rs233-production-tracking-all.log`.


## Infernal pickaxe and crystal gathering tools

Infernal pickaxes now actually consume ordinary resources with the dated one-third proc, award
Smithing XP and spend one of5,000 charges only when a resource is destroyed. The selected worn
or carried item is updated, including all three ornament styles, depletion and optional5% lucky
penny saving. Crystal signets do not save Infernal charges. Held/worn native Check reports the
percentage; an empty pick recharges with a normal dragon pickaxe or smouldering stone. Bonus
cape/celestial resources can proc independently. Gems/amethyst/essence/dense/Motherlode are
excluded. Dated listed ore/granite XP values are used (native fine-XP truncates the article's
two-decimal values). The incomplete article gives only a6–21 sandstone range: middle sizes9/15
and otherwise unspecified nonsmeltable XP at20% of required Mining level are explicit models,
not verified parity. Recharge XP follows the existing documented Infernal-tool conversion model.
Eight new integration cases passed; full private Mining **103/103** passed in
`/tmp/rs233-production-infernal-all.log`. Source: Infernal pickaxe14986642, Smouldering
stone14874222, Infernal axe14997416; all before or on1October2025.

Crystal pickaxe and harpoon are now actual products of the shared bowl/singer menu, alongside
the existing axe and externally registered combat equipment. A normal dragon tool, tool seed and
120shards produce a10,000-charge tool after3ticks, granting6,000Crafting and Smithing XP at
boostable76skills. NPC assistance costs180shards, needs no skill levels and gives no XP. Members,
Song of the Elves, exact inputs and stale action checks remain enforced. Shared native menu tests
now derive available extension products and preserve axe-first ordering. Six new cases and full
private Woodcutting passed; log`/tmp/rs233-production-crystal-tool-all.log`. Native IDs only; no
additional pack required. Sources: crystal pickaxe page already pinned in the crystal-charge
section, Crystal harpoon14969857(2025-08-20), existing native233.4 bowl/singer operations.


## Mining Guild, amethyst and mining-glove progression

Native outer and members' Guild doors now enforce60Mining (visible boosts accepted) and
membership as applicable, and allow leaving. Belona has her dated ordinary spawn and native
Talk/Trade/Toggle-minerals operations. Ordinary resource extraction in the members' chambers
receives +7invisible levels and half respawn intervals without unlocking higher-level rocks.
The two native amethyst wall types now mine at92for240XP with the published-18..10curve,
two/three resources per vein and75-second respawn. Western native veins additionally require
actual Elite Falador completion and Belona's saved permission; the southeastern veins remain
available without that permission.

Mining gloves now preserve the exact post-rebalance extra gather counts for ordinary ores,
expert sandstone/runite/amethyst, and enforce unboosted20/55/70equipment levels. Standard
glove tiers leave sandstone/granite unchanged. Preserved rocks continue through the existing
interruptible hidden mining operation. A shared vein's initial resource stock is determined by
its first successful miner's gloves; exact multi-player/glove-switch ownership behavior is an
explicit model, as are equal weights for amethyst's two/three base resources and the bounded
Guild rectangle/door presentation. No additional ore can be created from a completed vein.

One main-action mineral roll uses the dated1/100iron,1/60coal,1/40mithril,1/30adamantite,
1/20runite/amethyst rates. Infernal ore destruction still permits that roll; resource bonuses
do not make additional mineral rolls. A full inventory drops the mineral privately. Belona
accepts40/100minerals for basic/superior gloves and combines both for40more, with atomic
purchases, pack goods and80%native-ID resales. Her dialogue-based stock is a presentation model;
exact finite shared shop stock/restocking/full-screen UI remains open. The parent-owned pack
handlers open both native Guild reward-pack variants. Saved varps65733/65734 are
`mining_guild_minerals_disabled`/`mining_guild_amethyst_permission`; **cache packing required**.
The Mining cape's eligibility now matches the dated celestial table, including clay/quarry and
Kourend resources. Varrock armour duplication and exact remaining enhancement/visual parity
remain open.

Sources: Mining Guild14965341, Mining gloves14881415, Superior14977981, Expert14881417,
Unidentified minerals14978264, Belona14992512, Mineral Exchange14993277, Amethyst crystals
14990641; all before1October2025. Twelve new Guild integration cases passed, including
native wall harvesting/respawn, doors, unboosted glove equip, permission/negative cases,
full-bag private mineral delivery and complete180-mineral expert-glove procurement. Full
private Mining **115/115** passed: `/tmp/rs233-production-mining-guild-all2.log`.


## Potion/oil dose handling and NPC decanting

All ordinary drinking-registry potion families, Surge/Goading, the Barbarian mixes, olive/sacred
oil, Relicym's balm, the two Mort'ton serums and six Hunter mixtures now have compatible fluid
catalogues. Native item-on-item combines doses up to the family's actual maximum and returns
its empty container; empty-container splitting produces2+2,2+1or1+1. Different fluids,
self-slots, noted manual inputs and member fluids on free worlds are rejected. Free-world
Strength potions remain usable. Native Empty operations return the appropriate empty container,
including oils' actual fourth inventory operation. Transactions preserve both selected slots and
award no XP, chemistry extras or potion-creation diary credit. Registration-normalized pour
direction/presentation remains a bounded interaction model.

Bob Barter now has his dated Grand Exchange spawn and native Decant operation, and Zahur's
existing native Decant and common dialogue invoke the same service. Members can choose1–4doses;
two-dose families retain their real maximum. Bulk processing pools doses by fluid, reuses freed
containers across compatible fluids, accepts noted potions/empty containers, and conserves every
dose. Noted inputs force noted outputs; inadequate unnoted capacity retries atomically using
notes. Missing containers cost the shared MarketPrices quote plus2coins each, and insufficient
payment/capacity rolls back all inputs. The current default market provider quotes native item
values, so this is **not a historical/live GE-price claim**. Hunter mixture jars are kept separate
from vials; their empty-jar return is a container model pending direct client observation.
Unnoteable fluids stay unnoted even in the notes fallback.

Fourteen new cases and full private Herblore passed, log
`/tmp/rs233-production-decant-all.log`. New Bob map resource`decanting.toml` requires a shared
cache pack; the module adds api.market. Sources: Decanting14919706, Bob Barter14768244,
Zahur14994347, olive/sacred oil14993840/14993832, serums14993820/14993838,
Moonlight moth mix14901693, Sunlight moth mix14898575 and Hunter's mix period article.
Hunter-mix creation/sharing remains separate work.

## Native brewery checkpoint — 2026-11-12

All three native personal vats (Keldagrim, Port Phasmatys, Aldarin), ten recipes and mature
forms now work through ordinary item-on-vat/valve/barrel operations. Partial ingredients,
water/malt order, optional Stuff before hops, cider-only apple mush, level/member gates,
640-minute player-phased growth, 16/128 bad odds, 6/128 or 82/128 mature odds, eight-pint
barrels, glass/keg collection and atomic selected-container replacement are covered.
Native visual varps510/4449 and new saved65736–65739 preserve progress and contents.
Blandebir/Metarialus/Lucrezia supply yeast with pot/currency/Ghostspeak checks. The unique
Phasmatys trapdoor/ladder is bound; central lane owns Keldagrim pub stairs. Barley malt is
range-only level1/0XP; apple presses use four apples and an interruptible15-tick crush;
knife-on-calquat makes empty kegs. All20 glass forms and80 partial keg forms drink with
source-backed boosts/current-level penalties/healing and standard food cooldown; pouring
a filled keg into a glass retains the exact remainder. Full Cooking135/135 passed,
including16 new brewery cases, log`/tmp/rs233-production-brewery-all3.log`.

Explicit models: Stuff fermentation duration is uniform3–16 growth cycles (normal3);
exact extended-duration distribution is unpublished. Brewing XP is granted once on the
finished valve transfer. Barrel low bits encode remaining pints; automatic fourth-apple
crushing, shared press state, supplier dialogue/placement within Lucrezia's dated polygon,
and drink/press animation remain client-observation work. Vats refresh on login or use,
not while idling; imported native in-progress vats without our new saved cycle do not
advance automatically. Kelda quest brewing remains TODO. Resource`breweries.toml` and
new persistence editors require shared packCache. Dated sources: Brewing14997691,
Fermenting Vat14824406, Dwarven stout14825282, Ale yeast14753979, Blandebir14767352,
Metarialus14994408, Lucrezia14995160, Apple mush14786759, Barley malt14987829,
Calquat keg14419537, Valve14450130, Barrel14782126 and The stuff14772006.

## Specialist tree checkpoint — 2026-11-12

The six cache-native Sulliuscep multilocs now use the player's permanent fossil_cep5808
rotation, with Bone Voyage50/member/65Woodcutting and normal usable-axe gates. Successful
chops give127XP, correct tool curves and1/16 rotation; pre-roll numulite5–16 and four fossil
sizes preserve the dated900/50/25/20/5 out of3500 marginal table. Fossil toggles divert only
their own rolls to the90/9/1 mushroom/fungus/cap table. Nests and343,000-base clue nests
use the shared handlers. Native Inject spends one proboscis and one charged fungicide spray
dose atomically, advances the personal tree and sets the Fossil Island activity flag.

Blisterwood now cuts at62/76XP after native Sins-of-the-Father stage112, never despawns,
has1/10 action stop and no nests. Both ordinary Arboretum doors work in both directions.
Both specialist targets reuse Woodcutting.findAxe, crystal charges, XP modifiers and the
ForestryHarvest.chop felling-ration result; Sulliuscep felling destroys fossils too. Native
Tar Swamp vines require rake or usable axe and interruptible clearing. The native mud pit
consumes nine bittercaps atomically; the rubber cap lands on a decoded walkable bed edge,
consumes its cushion once and deals immediate40–50 damage without one.

Thirteen new integration cases and full Woodcutting passed, log
`/tmp/rs233-production-specialtrees-all.log`. Native visual-persistence/clue-param edits
require a shared pack. Verified sources: Sulliuscep14977071, Template:FossilDropLines14831088,
Blisterwood tree14892415, Tar Swamp14977092, Rubber cap mushroom14121947, Fungicide spray14994007
and Proboscis14977073. Sins stage112 is additionally pinned by Quest Helper commit
241eaec29b19243bda7e88e99d5c16568c0776a6.

Explicit models:4-cycle chop/clear cadence,100-cycle shared vine regrowth, intermediate
blisterwood tool curves interpolated between verified bronze15/50 and dragon61/186, and
the rubber cap's4-cycle animation/40–50 damage distribution. Tar Monsters, spine-mushroom
hazards, hoop-snake stunning and exact client jump trajectory remain open. Peter fossil
toggle dialogue and the separate Fossil Island task-board reward service are not added here.

## Mastering Mixology training/reward checkpoint — 2026-11-12

Native Aldarin basement access, Lalo Talk/Rewards (nativeOp3), HUD882, all30 clean-herb/unf
refiner inputs, six XP per input,3,000-cap personal hoppers, sliding three-lever selection,
all ten level-gated mixtures, and all three workstations now work. Native potion state
preserves processing and digweed per item; cancelled processing leaves the unfinished
potion safely in the inventory. Orders use the dated weights, three independent native
type/modifier fields and proper20%/40% multi-order resin bonuses. Unrequested output
returns the highest reagent's10resin and rounded-third XP. Digweed doubles rewards,
blooms only through active deliveries, expires after33ticks and withers on leaving.
The base60 gate is unboostable and native Children-of-the-Sun end24 is required.

Native enums5542/5545/5546/5548 and client scripts7063/7171 pin recipe ordering and
Homogenous1/Concentrated2/Crystalised3; native item ID order differs. The existing
omnishop3746 and native DB stock rows supply all12 rewards for exact three-resin costs,
including charged50-charge amulets, goggles, aldarium and reagent pouch. Cosmetic/goggle
refunds return40%. All three potion packs have four noted rolls with the dated observed
(approximate) weights and exact quantities/level gates.

The permanent Reagent pouch stores26 of each of30 dated secondaries. Native Fill/Open/
Empty/Check, bank-side fill/empty, confirmed-destroy clearing and GroundItemTaken routing
work. An open carried81Herblore pouch supplies standard potion secondaries near a real
bank; a26-potion batch preserves the external trigger secondary and uses goggles/chemistry
transactions normally. Closed/low-level/distant pouch cases are rejected. Floor-pickup
auto-storage currently happens after ordinary inventory delivery, so full-bag pickup still
needs the ordinary slot. Other producers need explicit acquired-item routing where applicable.

Private pack and full Herblore229/229 passed, including22 new cases, log
`/tmp/rs233-production-mixology-all3.log`. Shared pack required: timer121, inventory951,
varobj65522, native persistent vars and mixology.toml. Potion-storage unlock and
disassembled-barrel acquisition exist; their consumers are the next staged work.

Explicit models: processing targets60progress with level-scaled2–3 progress/tick, retort
extra8progress/2XP clicks, bounded agitator and alembic skillshot windows; exact production
progress functions are unpublished. Digweed uses18–31 active-order intervals scaled by
level, rather than a verified time distribution; post-click expiry reservations remain open.
Refining uses1first/2repeat ticks, allowing active re-clicking. Lalo is placed at the dated
area centre. Exact animation/hit-sound/client presentation and alternate note-book/formula
UI remain open. Sources: Mastering Mixology14994982, Supervisor Lalo14992454,
Retort14874621, Agitator/Alembic14896268/14896269, Digweed14894179, Refiner14968603,
Hopper14792802, Mixology Rewards14939895, Reagent pouch14989854, potion packs
14861730/14861729/14861728.

## Mixology reward containers: verified final consumer pass

Potion storage now uses the103 native dose enums and their real counter/preference fields,
plus empty vials. Raw and noted deposits conserve doses; withdrawal supports remembered or
changed dosage, final partial bottles, notes, quantity modes, X and all-but-one. The native
five-child row layout from client6360, header vial controls from6081, and enum4826/4829 order
are reproduced. Favourites store the native one-dose object IDs, use the native -1 empty sentinel,
and persist in the eight existing fields. Locked/member, overflow and new-type/full-bank checks
preserve inputs; the dated27August2025 existing-type/full-bank exception is supported.

The native tab15 view and ordinary bank share the existing bank-open handler through an optional
`BankOpened` event; deposit interception is likewise optional and otherwise leaves normal banking
unchanged. This avoids duplicate keyed `bank_side` handlers. The central `BaseVarObjBits` now owns
`mixology_potion_state` bits0–2, allowing the cache-delta exporter to enumerate the same definition
used for individual mixture state.

The disassembled chugging barrel now assembles, opens, loads, fills, unloads, reorders and
consumes its contents. Native permanent inventory911 holds five types, each capped at100doses.
Partial bottle handling and bank returns are atomic and obey unlocked bank capacity, including
separate same-ID objects with different item vars. Native Store/Fill/Withdraw quantity operations,
X, drag ordering, bank controls and four native profile controls are bound. The current refill
target survives drinking and insufficient bank stock; preset loading draws from the selected
ordinary bank or potion-storage view. Real consumption events apply ordinary potion effects in
slot order, including barbarian-mix healing and Surge's actual special-energy/cooldown checks.
Dismantling loses contents; ordinary deletion/death preserves the native ghost-storage behavior.
Vials are returned when bank/inventory bottles are drained, and direct pooled-dose loading does
not create additional containers.

Fresh isolated cache233.4 pack plus **Herblore248/248 and bank integration42/42** passed
(`/tmp/rs233-production-containers-full.log`), including19 new container cases. Tests cover every
native raw/noted storage family, native dose/favourite/vial/withdraw packets, normal locked
banking, overflow/full-inventory/full-bank rejection, native barrel Store5/Fill1/WithdrawAll and
drag packets, real stat effects, Surge gates, partial bottle replacement and refill targets.

Remaining limits: bank proximity is the existing four-tile source model rather than recovered
native bank-area polygons; exact barrel animations, dynamically generated client presentation,
and specialized bank exclusions such as Eniola require client observation. Native section-wide
header withdrawals beyond the individual-family and vial controls are not implemented. Vial-only
storage follows the native client limitation that vial controls appear with a potion section.
The reagent pouch currently supplies the standard finished-potion mixer; other distinct producer
classes still need explicit routing. Unpublished Mixology processing distributions and the
observed potion-pack weights remain models as documented above. These are functional training
and reward loops, not a claim of complete Herblore parity.

Sources pinned before1October2025: Potion storage14974977 (27August2025), Chugging barrel14991050
(24September2025), and native cache233.4 client3750/3751/4818/6080/6081/6360/7338–7347/7370.

## Repeatable regional Slayer dungeon access

`RegionalSlayerDungeons` adds the11 historical Braindeath fever-spider positions, nine Tarn
terror-dog positions and six Molch temple shamans, plus Pirate Pete's mainland/island NPCs.
Pirate Pete accepts started Rum Deal without advancing its native state. The native brewery
stairs and basement ladder provide a continuous walk from the boat-side landing to the spiders
and a working return route. Fever spiders have the dated42-Slayer gate and40HP; both terror-dog
sizes have their40-Slayer gate and82/87HP; temple shamans have150HP and sourced combat bonuses.

Slayer Rewards' existing75-point ring purchase now has ordinary held/worn transport and charge
consumption. All five published destinations are offered, with Priest-in-Peril/Haunted-Mine/
Mourning-II requirements read from native quest rows. The last charge returns the enchanted gem
atomically; stale items and blocked teleports consume nothing. Tarn's normal pre-room landing
reaches the repeatable dogs through the native final doorway on a completed Haunted Mine and
Tarn-miniquest account. Quest progression and the full on-foot trap maze remain outside this path.

All four native Molch dwellings enter walkable lower rooms; the strange hole exits onto the
pyramid. Five native barriers use axis-aware crossings,1/10/20 damage tiers and one-minute
per-passage decay. The live Molch room is **native level1**, while its wiki display uses plane0;
the TempleShaman diary hook now explicitly checks level1. Physical player-sourced kills grant
all three relevant diary tasks; identical shaman XY on level0 is rejected.

Fever spiders use their complete dated36-cell ordinary main table. Terror dogs use their128-cell
main table, herbs/gems and size-specific ordinary bones; temple shamans supply big bones through
the central drop registry alongside the existing lizardman fang/talisman provider. Registry
remains delivery continues through the central Prayer conversion path.

Private cache pack and **10/10 new integration cases** passed, plus all17 fixtures available in
this isolated diary-routes snapshot (`/tmp/rs233-production-regional-dungeons-all.log`). Cases
include physical kills, all landing collision, actual native final doors, round-trip Pirate Pete,
continuous brewery approach, native quest rejections, last-charge conservation and barrier damage.
Shared cache pack is required for the28 NPC placements and combat-type edits.

Explicit limits: island Pete's return placement and the boat-side landing are chosen beside the
native boat on verified walkable terrain; exact dialogue, sail cutscene and stair timings need
client observation. The nine historical dog coordinates do not identify their individual size,
so sizes alternate as an explicit population model. Fever-spider glove/disease retaliation,
Tarn-room prayer drain, exact shaman specials/animations and full shaman ordinary loot are not
implemented here. Barrier passage history is session-local and updates native colour when used;
continuous idle visual decay is not implemented. Slayer-ring Rub currently shares the transport
menu; master-contact/partner/log, ring crafting/decanting and exact teleport presentation remain
separate work. Native completed-quest access is implemented without any quest completion bypass.

Sources before1October2025: Fever spider/Terror dog/Lizardman shaman historical location/stat/drop
receipts under`/tmp/rs233slayer-*`; Braindeath Island14968857, Pirate Pete14968881,
Tarn's Lair14912813, Lair of Tarn Razorlor14928777, Lizardman Temple14848782,
Lizard dwelling14848784, Strange hole14988032, Mystical barrier14072926, Slayer ring14979708.
Tarn display coordinates are translated through native world-map area`undead_dungeon` into the
actual repeatable room, independently confirmed by its native boss-door placement.


## Hunter mixes

The six jarred butterflies and moths combine with nine raw Hunter meats and a pestle and mortar. Mixing uses both inputs and creates two doses. These share a three-tick potion cooldown. A Snowy knight mix heals eight Hitpoints per dose and a Moonlight moth mix restores 22 Prayer points. In multi-combat areas, a dose can also affect up to three nearby players with Accept Aid enabled. Player-targeted jars use the same aid and range checks. The server regression covers the tool requirement, both doses, the returned jar, the recipient cap and an opted-out neighbour. Group Ironman eligibility depends on a shared observer identity and still needs a grouped-account client check. [Moonlight moth mix](https://oldschool.runescape.wiki/w/Special:PermanentLink/14901693) · [Snowy knight mix](https://oldschool.runescape.wiki/w/Special:PermanentLink/14839754)

## Hunting-ground ordinary resource drops

`HuntingGroundDrops.kt` and its227-row native-object TSV provide main loot for all17 identities
listed by `HuntingGroundNpcs`: small/desert/large lizards, ordinary dust devils, both ordinary
locust riders, mithril dragons, five green-dragon identities, lava dragons and mogres. Every main
family retains128cells. Shared herb/gem/mega rolls are reused; ordinary RDT, rare/uncommon seeds
and the applicable low-combat general-seed subtable are sourced separately. Lizard herb/fire-rune
and seed/two-waterskin outcomes are correlated rather than counted twice. The two double-gem
cells perform two rolls. Mystic gloves and dust battlestaff pre-rolls preserve ordinary bones;
the chainbody is the1/256 outcome of a single dust-devil main cell.

Mithril dragons always supply three bars and dragon bones; their main table supplies chewed bones,
the shared battleaxe/shark outcome, ancient pages and normal equipment/runes/mixes. Duly noted
requires both the saved unlock and a genuinely active assigned task. Monkey Madness II's native
end state selects dragon-javelin heads instead of the old coin entry. Lava dragons always supply
black hide, lava scale and lava bones. Green dragons always supply green hide and dragon bones.
Green notes require elite completion plus an actual Wilderness kill; lava notes require elite
completion plus saved`cert_lava_d_bones=1`. The two conditions are tested independently.

Visage, eligible long/curved bones, central ownership-aware clues and Wilderness looting bags are
included. Generic green-dragon ensouled heads remain with their existing tertiary provider;
Lava's missing1/18 head provider now uses the fresh head identity/origin helper. Ordinary remains
continue through central `NpcDeath`/`RemainsDrop`; this code does not independently award bones.
Mogre flippers and the source's water-filled fishbowl6668 are normal main-table results.

**13/13 new integration cases passed**, log`/tmp/rs233-production-hunting-drops8.log`, after syncing
the current shared hit/hero-point and death-callback code into the isolated snapshot. Actual
player-sourced deaths deliver Mithril chewed bones+three bars, green bones+hide and lava bones+
black hide+scale. Tests also cover raw/noted negative conditions, Duly noted, all native table
weights/identities, correlated lizard outcomes, double gems, mystic/chainbody branches and Mogre
flippers/fishbowl. Formatting passed separately. Only the independent source, TSV and fixture
were promoted; there are no new custom cache identities.

Limits: the dated uncommon-seed template lists1046weights against its1048denominator; the two
unmatched cells explicitly yield nothing rather than renormalizing published weights. Earlier
Mogre127/128 extraction was corrected by including its image-tagged fishbowl row, so that main
table has no invented gap. Where the dated pages provide unique odds but no definitive engine
ordering, mystic gloves, dust battlestaff and dragon full helm use explicit pre-roll ordering;
exact ordering remains unverified. Quest-only Rag/Bone-Man drops and clue-specific kill rewards
remain outside these ordinary tables. Wild-cave/other modern variants outside `HuntingGroundNpcs`
are not claimed by this table registration. Looting-bag eligibility covers ordinary carried/banked
ownership, with specialized death-storage exceptions still unverified.

The native item and banknote identities were checked against cache 233.4, including coins (995), poisoned rune darts (817) and two-dose superattack mix (11469). The hunting tables use the Old School RuneScape Wiki's October 2025 revisions. [Small lizard](https://oldschool.runescape.wiki/w/Special:PermanentLink/14957409) · [Locust rider](https://oldschool.runescape.wiki/w/Special:PermanentLink/14959404) · [Mithril dragon](https://oldschool.runescape.wiki/w/Special:PermanentLink/14983020) · [Green dragon](https://oldschool.runescape.wiki/w/Special:PermanentLink/14991784) · [Lava dragon](https://oldschool.runescape.wiki/w/Special:PermanentLink/14933309) · [General seed table](https://oldschool.runescape.wiki/w/Special:PermanentLink/14997280)
