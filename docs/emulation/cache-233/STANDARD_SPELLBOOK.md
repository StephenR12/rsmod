# Cache 233 standard spellbook inventory

This inventory starts from the 73 standard-spellbook objects configured by `SpellObjEditor`: 71
castable spell entries and two submenu entries. Metadata in the cache or editor does not mean that
server behavior exists. Each named spell remains incomplete until its requirements, success and
failure paths, timing, side effects, XP, audiovisuals, restrictions and client behavior are tested.

## Implemented spell behavior

The following 36 combat attacks are registered; Charge, both alchemy spells, both bone-conversion
spells, Superheat Item, Telekinetic Grab, all seven jewellery-enchantment spells, all four
orb-charging spells, all ten crossbow-bolt-enchantment child spells, Monster Inspect, Lumbridge Home
Teleport, and Varrock, Lumbridge, Falador and
Camelot Teleport also have handlers. Kourend Castle, Ardougne, Civitas illa Fortis, Watchtower,
Trollheim, Ape Atoll, all three Teleother spells, Teleport to Target and Teleport to House have
handlers as well. The 20 elemental attacks have level,
cast XP, rune quantities, spellbook/type and cache max-hit metadata covered by cache-backed tests.
Their live tier scaling has boundary unit tests. Representative Wind Strike integration tests cover atomic level/rune
failures, rune and staff substitution consumption, cast/damage/Hitpoints XP, splash behavior,
animation/launch/projectile/impact metadata, matching elemental weakness, autocast re-engagement
and the five-tick combat cadence. Client observation, sound assertions and full outcome tests over
every spell/tier are still required before any entry can become `verified`.

- Strike: Wind Strike, Water Strike, Earth Strike, Fire Strike.
- Bolt: Wind Bolt, Water Bolt, Earth Bolt, Fire Bolt.
- Blast: Wind Blast, Water Blast, Earth Blast, Fire Blast.
- Wave: Wind Wave, Water Wave, Earth Wave, Fire Wave.
- Surge: Wind Surge, Water Surge, Earth Surge, Fire Surge.

The six stat-drain curses also have cache/editor-backed level, rune, XP, affected-stat and drain
percentage contracts: Confuse (Attack 5%), Weaken (Strength 5%), Curse (Defence 5%),
Vulnerability (Defence 10%), Enfeeble (Strength 10%) and Stun (Attack 10%). Integrated tests cover
successful NPC and player application at projectile impact, splash XP without a drain or Hitpoints
XP, exact already-weakened rejection before resource use, non-stacking, Tome of Water's 20%
accuracy and 50% drain modifiers, audiovisual selection and the five-tick manual-cast cadence.
Successful curses use retaliation without a damage event, hitmark, headbar or defend animation and
stop rather than autocast.

These curses are not `verified`: Tome charge consumption and its final-charge transition are now
integrated, but every NPC-specific immunity/cap/regeneration rule, exhaustive spell-by-spell
audiovisual observation and RuneLite confirmation remain open.

Bind, Snare and Entangle are registered on a shared movement-binding state model. Cache-aware
integrations cover their levels and rune costs, standard/combat classification, hit and splash
paths, Bind's non-damaging impact, Snare's 2 and Entangle's 5 gameplay caps, base successful-cast
Magic XP of 31/61/89, splash XP of 30/60/70, cache audiovisual selection and the five-tick manual
cast cadence. Successful impacts hold movement for 8/16/24 cycles; Swampbark helm, body and legs
each add two cycles. Targets then receive five cycles of refreeze immunity. Recasts reject before
resource or XP use with the cutoff messages, player binds break beyond the ten-tile attack range,
and players receive the red fade warning four cycles before release. Tome of Water's 20% Bind-family
accuracy modifier is formula-tested.

The binding family is not `verified`: the warning's accompanying sound and production timing need
client capture, the cache's generic spell max-hit metadata does not encode the wiki gameplay caps,
NPC-specific freeze resistance/immunity is only wired for the four Phantom Muspah combat forms,
and RuneLite observation remains open. Tome charge use and its last-charge cast snapshot are
integrated.

Crumble Undead is registered as an undead-NPC-only combat spell. Cache-aware integrations cover its
level 39 requirement, two air/two earth/one chaos rune cost, 24.5 base Magic XP, exact rejection
message before resource use, hit and splash paths, dedicated cache graphics and sounds, rune-staff
substitution, and five-tick cadence. Its gameplay max hit is 15, increased to 18 by Salve amulet
(ei); Chaos gauntlets do not modify it. Restricted autocasting is tested for the imbued skull
sceptre, both Slayer staves, Staff of the dead family, Staff of light, Staff of balance and both
Void Knight mace states, while an ordinary elemental staff is rejected.

Crumble Undead is not `verified`: the implementation trusts cache parameter 65434 as the production
undead classification, but the full classified NPC inventory and encounter-specific exceptions have
not been audited. Exact audiovisual timing still needs observation in the workspace RuneLite.

Magic Dart is registered for NPC and player combat. Cache-aware integrations cover level 50 Magic,
current level 55 Slayer, four mind/one death rune, atomic staff and Slayer failures, the full manual
staff-substitution family, the six documented autocast weapon types, 30 base Magic XP, hit/splash
paths and five-tick cadence. Damage follows `10 + floor(current Magic / 10)`. An enchanted Slayer's
staff uses white travel/impact graphics on a hit but retains the ordinary green travel graphic on a
splash; ordinary, off-task damage still uses the base formula. Boundary formula tests also cover
the enchanted on-task formula `13 + floor(current Magic / 6)`.

Magic Dart resolves the live Slayer assignment, master, location, enchanted-staff charges and
eligible target. The staff consumes its on-task charge and reverts on depletion. Turoths and
Kurasks enforce their special weapon rules. Exact projectile timing and sound still need a client
capture.

Iban Blast is registered for NPC and player combat. Cache-aware integrations cover level 50 Magic,
five fire/one death rune, both Iban staff variants, atomic staff/charge rejection, 30 base Magic XP,
the fixed 25 max hit, hit and splash paths, dedicated cache graphics, restricted autocast and the
five-tick cadence. Charges are stored once per player, shared between every regular and upgraded
staff they own, and consumed once per NPC cast or twice per PvP cast. Tome of Fire substitutes the
five fire runes without adding its elemental fire damage modifier.

Iban Blast checks Underground Pass varp 161 at stage 11. After the quest, Koftik replaces a lost
staff with a broken one. The Dark Mage repairs, upgrades and recharges staves for the documented
fees. The paladin well recharges both versions for free. Inventory and equipment Check report
remaining charges, and low-charge casts report the remaining count. The quest story itself and the
full route through Underground Pass are incomplete. [Iban Blast](https://oldschool.runescape.wiki/?oldid=14785260) · [Paladin well](https://oldschool.runescape.wiki/?oldid=14976094)

Saradomin Strike, Claws of Guthix and Flames of Zamorak are registered for NPC and player combat.
Cache-aware integrations cover their level 60 requirement, spell-specific rune and weapon gates,
35 base Magic XP, hit/splash paths, base max hit of 20, dedicated target graphics and five-tick
cadence. Successful player-target Saradomin Strike drains one Prayer point; Claws drains Defence and
Flames drains Magic by `1 + floor(base level * 5 / 100)`, without stacking until the stat restores.
The cutoff manual/autocast weapon matrices are wired. Charge is available at level 80 for three air,
fire and blood runes, grants 180 Magic XP, publishes a RuneLite-compatible seven-minute varp, and
has a one-minute recast cooldown. While active, a matching regular, imbued, locked/trouver or max
god cape raises only its respective god spell's max hit from 20 to 30.

A level-60 member can enter through the Wilderness levers, challenge Kolodion through five
forms, choose a statue cape and collect the matching first staff. Native varp 267 reaches 6, 7 and
8 on those last three actions. Further god staffs cost 80,000 coins. Varp 268, 269 or 270 counts
each paid cast, including a splash, on a battle mage in the arena. At 100 the corresponding spell
works elsewhere. The Staff of the dead family saves the rune cost of one in seven eligible combat
casts after validating its requirements. Server tests cover the final unlock and off-site cast,
spell-specific counters, staff conservation and denied casts. Exact audio, timing and UI appearance
still await a client capture. [Mage Arena I](https://oldschool.runescape.wiki/?oldid=14994825) · [Battle mages](https://oldschool.runescape.wiki/?oldid=14973934) · [Staff of the dead](https://oldschool.runescape.wiki/?oldid=14953074)

Low and High Level Alchemy are registered as inventory-target utility spells. Cache-aware
integrations cover their level 21/55 requirements, three/five fire plus one nature rune costs,
31/65 Magic XP, cache item-value conversion at 40%/60%, one-item stack conversion, distinct target
and spell-rune consumption when alchemising runes, staff substitution, non-alchable cache objects,
Fountain of Rune no-rune/no-XP behavior, sequences 712/713, spotanims 112/113, sounds 98/97 and
three/five-tick cast rates. Inventory mutation is preflighted before runes or XP are consumed.

Alchemy is not `verified`: Low Alchemy's five-tick Alchemist's Playground exception, the queued
second-cast behavior, restricted activities, Explorer's ring daily casts, warning settings,
animation overrides and exact failure messages still require implementation or cutoff client
evidence. Client-observed sequencing and interruption behavior are also open.

Bones to Bananas and Bones to Peaches are registered as inventory-wide utility spells. Cache-aware
integrations cover levels 15/60, their two-water/two-earth/one-nature and
four-water/four-earth/two-nature costs, 25/35.5 Magic XP, the Peaches unlock varbit, all cutoff
normal, burnt, bat, big, wolf, jogre, monkey, bleached, Alan's and Creature Graveyard bone variants,
one-to-four Graveyard fruit yields, higher-tier bone exclusion, empty-target rejection, shared
sequence 722, spotanim 141, sound 114 and one-tick cast rate. All eligible held bones transform in
one cast; fruit output is capped to the post-conversion inventory capacity.

The bone conversions are not `verified`: the precise locked-spell message, Tombs of Amascut
restriction, output-overflow ordering, interruption/client ordering, Lumbridge diary trigger and
spell-tablet variants still require cutoff evidence or client testing. Mage Training Arena purchase
and point systems are not yet present, so tests arrange the cache unlock varbit directly.

Superheat Item is registered as an inventory-target utility spell backed by a reusable Smithing
smelting-recipe registry. Cache-aware integrations cover level 43 Magic, four fire/one nature rune,
53 Magic XP, the three-tick rate and one-deep queued cast, all nine ordinary cache-era bars,
The Knight's Sword-gated blurite and partial-Lunar-Diplomacy-gated lunar bars, normal Smithing
levels/XP, 100% iron success, iron-to-steel priority, catalyst coal/XP effects, loose-coal-before-
coal-bag ordering, goldsmith gauntlet/Smithing cape gold XP, Fountain of Rune behavior and atomic
failure paths. Jogre bones require unboosted level 30 Cooking and produce burnt jogre bones for 25
Cooking XP. Noted/non-ore and elemental ore targets use the documented cutoff messages.

Superheat Item is not `verified`: exact level, material and quest failure text, successful and
failure audiovisual ordering, interruption behavior, queue replacement rules and RuneLite
observation remain open. Perfect gold ore is deliberately excluded because its last pre-cutoff
recipe documents furnace creation only, while the pre-cutoff blurite and lunar pages explicitly
include Superheat recipes. That negative distinction still needs matching-client confirmation.

Telekinetic Grab is registered through a reusable interface-on-ground-item interaction path.
Cache-aware integrations cover level 33, one air/one law rune, 43 Magic XP, whole-pile pickup,
ordinary rune and staff substitution, Fountain of Rune no-rune/no-XP behavior, ten-tile targeting
without line-of-sight, identity-safe delayed pickup, full-inventory and already-taken atomicity,
plane-change fizzling, one queued extra cast during the three-tick rate, cache audiovisuals and a
distance-independent projectile. The blocked set covers Ahab's beer, raw pheasant, both Trouble
Brewing flowers, dragon token, both looting-bag states and all 48 equipable Achievement Diary
rewards.

Telekinetic Grab is not `verified`: projectile start/end values 35/45 and the two-tick server impact
are historical-emulator inferences pending matching-client capture. Exact rejection, full-inventory
and already-taken text, movement/combat/interface interruption, queued-target replacement,
free-world member-item behavior, Mage Training Arena overrides, Tower of Life interference and
farming-patch busy handling remain open. Sailing behavior added after the cutoff is not part of
cache 233.4.

Tele Block is registered as a player-only Wilderness spell. Its cache/editor contract covers level
85, one law/chaos/death rune, 83.6 splash XP, 95 successful-cast XP and a five-tick cadence.
Successful impacts apply 500 cycles of blocking, or 250 when Protect from Magic was active at cast,
followed by the cache/client-defined 100-cycle reapplication-immunity window. The target receives
the cache timer varbit, exact duration message and combat logout prevention; leaving the Wilderness,
logging out and dying clear both the block and immunity. Active and immune recasts reject before
runes or XP are used, and blighted teleport sacks substitute in the Wilderness. The cache casting,
success-impact and failed-travel graphics and cast/impact/splash sounds are selected.

Tele Block is not `verified`: the first ten fixed self teleports, Teleport to House and Lumbridge Home Teleport consume
the shared blocking state, but other teleport paths do not yet. There is no production updater for
`inside_wilderness`, no
caster-death attribution hook, and no evidenced exact 30-second-warning or natural-expiry messages.
Successful presentation and travel timing are cache-informed inferences pending observation in the
workspace RuneLite.

Lvl-1 through Lvl-7 Enchant are registered as inventory-target utility spells, including explicit
spell-registry discovery for the cache's jewellery submenu children. Cache-aware integrations cover
levels 7/27/49/57/68/87/93, exact rune costs, 17.5/37/59/67/78/97/110 Magic XP and all 44 cutoff
transformations. The registry contains sapphire/opal, emerald/jade, ruby/red-topaz, diamond,
dragonstone, onyx and zenyte rings, necklaces, bracelets and amulets, plus the Silvthrill rod,
pre-nature amulet and two enchanted sickles. The February 2025 first-matching-source rule, four F2P
standard-gem amulet exceptions, members-only remainder, Digsite-pendant discovery gate, one-owned-
nature-amulet rule across inventory/equipment/bank, ordinary rune substitution and Fountain of Rune
behavior are automated. Manual casts use a three-tick rate with one queued extra cast; uninterrupted
automatic casts repeat the same source every seven ticks and stop on movement or combat. Cache
sequences 719/720/721, spotanims 114/115/116/153/154/452 and the named gem sounds are selected.

Jewellery enchantment is not `verified`: the Mage Training Arena shape behavior and its activity
state are absent. Exact invalid/member/nature messages, the Digsite unlock varbit interpretation,
the complete interface/logout/inventory-change interruption matrix and client ordering still need
matching-client capture. Cache 233 has no separately named level-7 graphic or zenyte sounds, so the
current level-6 graphic and onyx sounds are explicit cache-informed fallbacks. Graphic height 100
and semi-precious/core-gem sound reuse are historical-emulator inferences pending observation.

Enchant Crossbow Bolt is registered through the cache's submenu opener and all ten child spell
objects. Cache-aware integrations cover the Opal, Sapphire, Jade, Pearl, Emerald, Red Topaz, Ruby,
Diamond, Dragonstone and Onyx levels, rune costs and XP; all regular and dragon tipped-bolt input and
enchanted output pairs; members, level, rune and missing-source failures; staff substitution; and
Fountain of Rune no-rune/no-XP behavior. Each successful cast transforms up to ten held bolts for
one full rune cost and full spell XP, including a final partial set.

The opener uses cache interface 270 and clientscript 2046 with all ten pause-button quantity inputs.
It selects the separate regular or dragon enchanted-bolt icon family shown in the official Wiki
screenshots, offers the held inventory's maximum number of ten-bolt sets, casts the first set
immediately, and processes the chosen remainder every three ticks. A new manual choice can cast one
tick after the prior cast. Enchanting preserves unrelated skilling/action queues and selected sets
continue across movement, matching the pinned zero-time rule. Sequence 4462, spotanim 759, synth
2921 and the exact success message are emitted.

Bolt enchantment is not `verified`: the Wiki and cache do not settle menu priority when regular and
dragon tipped bolts are held together, or the precise maximum-quantity derivation across mixed
types. The implementation uses regular-family priority and the largest combined regular/dragon set
count per gem as explicit deterministic rules. Exact failure text, inventory-full behavior,
audiovisual/inventory ordering and matching-client timing remain open.

Charge Water, Earth, Fire and Air Orb are registered as location-target utility spells. Cache-aware
integrations cover levels 56/60/63/66, thirty matching elemental and three cosmic runes, one
unpowered orb, 66/70/73/76 Magic XP, members-only use and exact matching-obelisk identity. A cast
replaces one unpowered orb in its existing slot, so a full inventory is valid; ordinary rune and
elemental-staff substitution and Fountain of Rune no-rune/no-XP behavior use the shared rune
manager. Manual casts use a three-tick rate with one queued extra cast, while uninterrupted
automatic casts repeat every six ticks. Movement, combat, source removal and obelisk removal stop
queued automatic charging. Cache component children 45/49/56/59, sequence 726, spotanims
149/151/152/150 and synths 118/115/117/116 are selected for Water/Earth/Fire/Air respectively.

Orb charging is not `verified`: the Water, Earth and Air diary-task hooks are absent, and exact
wrong-obelisk, membership, missing-orb and resource messages still need matching-client capture.
Client/server ordering, the complete interface/teleport/logout/death and cross-utility queue
interruption matrix, and exact automatic-stop behavior after inventory mutation also remain open.

Monster Inspect is registered as an NPC-target utility spell. Cache-aware integrations cover its
free-to-play level 42 requirement, two body/two mind rune cost, 30.5 Magic XP, ordinary
substitution and Fountain behavior, ten-tile target interaction, sequence 6293, spotanim 1059,
synth 3620, interface 522 and the one-string contract of clientscript 1179. Its four tabs expose
current combat stats; generic attack speed and nonzero attack, strength and magic-damage bonuses;
the seven stab/slash/crush/magic/light/standard/heavy defensive bonuses; and the locally modelled
Slayer, poison, venom, thrall, burn, freeze, demonic, draconic, rat, golem and elemental-weakness
attributes.

Monster Inspect is not `verified`: exact matching-client wording, capitalization, row omission and
interface/audiovisual ordering still need capture in the workspace RuneLite. The cache model does
not yet distinguish burn tiers or expose vampyric tier and spectral attributes, and Slayer
eligibility is currently inferred from positive Slayer XP rather than a complete category
inventory. Production freeze values have only been added for the four Phantom Muspah combat forms;
all NPC morph/value coverage remains to be audited.

The first ten fixed-destination self teleports are registered as utility spells. Varrock,
Lumbridge, Falador and Camelot use levels 25/31/37/45, exact rune costs, 35/41/48/55.5 Magic XP and
spellbook children 23/26/29/34; the first three are available on free worlds. The members-only
quest group is Kourend Castle (48, water/fire/two law, 58 XP, Client of Kourend), Ardougne (51, two
water/two law, 61 XP, learned from the post-Plague City scroll), Civitas illa Fortis (54,
earth/fire/two law, 64 XP, Twilight's Promise), Watchtower (58, two earth/two law, 68 XP, learned
from the Watchtower reward scroll), Trollheim (61, two fire/two law, 68 XP, Eadgar's Ruse) and Ape
Atoll (64, two fire/two water/two law plus one unnoted banana, 74 XP, Freeing King Awowogei).

Cache-backed boundary tests use the actual progression variables and distinguish quest completion
from learning the Ardougne and Watchtower spells. The Ape Atoll loader explicitly treats the
banana as an inventory consumable despite the object's legacy wear-position metadata. All ten use
the shared rune manager, including elemental staff substitution and Fountain of Rune behavior.
Casting is rejected atomically while an active Tele Block is present or above level 20 Wilderness;
level 20 itself is permitted. A successful cast uses player sequence 714, spotanim 111 and synth
200, then moves the player after four game cycles. The previously malformed Watchtower destination
has been corrected to the base tower area at 2546,3114.

The three cutoff diary alternates are wired to the cache's persistent state. A completed medium
Varrock Diary plus `varrock_ge_teleport` selects Grand Exchange at 3165,3479; a completed hard
Kandarin Diary plus `seers_camelot_teleport` selects a representative square in the documented
Seers' Village bank polygon; and a completed hard Ardougne Diary plus
`yanille_teleport_location` selects Yanille at 2584,3097. Without both the unlock and saved toggle,
the base destination remains selected. Cache-aware integrations cover all six branches.

These teleports are not `verified`: base and alternate landing-area randomisation, the right-click
configuration flows, exact message text and check ordering, interruption and movement-lock
behavior, and matching-client audiovisual timing remain open. The Wilderness gate still depends
on `inside_wilderness`, which has no production region updater. The four-cycle movement is the
historical Wiki cast duration interpreted as a server delay and needs matching-client capture.

Lumbridge Home Teleport is registered as a free-to-play, no-rune, no-XP utility spell. It begins
the cache's five-part aide animation immediately and advances through the chalk, sitting, book,
incantation/portal and departure phases before moving to Lumbridge on tick 22. The cache-backed
presentation uses sequences 4847/4850/4853/4855/4857, spotanims 800/802/803/804 and synths
193/196/194/195; the captured portal height is transmitted as unsigned value 65526. Movement,
replacement actions and incoming combat interrupt the channel without consuming its cooldown.
Casting is blocked for ten seconds after combat, by active Tele Block, and above level 20
Wilderness.

The successful cast stamps permanent varp 892 with the current epoch minute. The client-facing
date-minute varp 3078 is synchronized on login and each game cycle, enabling the cache clientscript
to render the shared 30-minute Home Teleport cooldown. Early recasts report the rounded remaining
minutes, and the cooldown is only consumed after the destination jump. This state is deliberately
shared by the implemented Ancient, Lunar and Arceuus Home Teleports. Their cutoff destinations are
Edgeville (3087,3504), Lunar Isle (2113,3915) and the Arceuus Dark Altar (1700,3882). The cutoff
Edgeville page explicitly allows its spell on F2P after switching to Ancients on a members world;
Lunar and Arceuus require a members world.

Lumbridge Home Teleport is not `verified`: the default phase boundaries and portal offset are based
on cache identities plus a live-protocol research capture and still need observation in the
workspace RuneLite. Exact cancellation presentation, all possible interrupting actions, the
Speedy and other animation overrides, restricted Lumbridge-area exceptions, and landing-area
randomisation remain open.

Acceptance requires exact target eligibility, batch behavior where applicable, inventory-full and
missing-item paths, rune substitution, item transformations and values, diary/quest unlocks,
location checks, delay/interrupt behavior, XP and client feedback. The jewellery and bolt-enchant
subinterfaces have separate child inventories; their two menu objects are navigation actions, not
casts.

## Teleother

Teleother Lumbridge, Falador and Camelot now use the player-target spell path and the cache-native
interface 326 confirmation flow. Their cutoff contracts are level 74 with one soul, law and earth
rune for 84 XP; level 82 with one soul, law and water rune for 92 XP; and level 90 with two soul
runes and one law rune for 100 XP. All three are members-only and use a ten-tick cast rate. A cast
requires the recipient's Accept Aid varbit 4180 and an available recipient action slot, consumes the
runes and grants XP atomically, then sends human sequence 1818, spotanim 343 and synth 199 from the
caster.

The recipient sees cache children 326:89/91 populated with caster and destination names. Children
326:97/99 are cache-defined pause buttons, so the server enables and awaits the native
accept/decline protocol instead of treating them as ordinary interface operations. Declining or
closing removes the offer without refunding the completed cast. Accepting rechecks Tele Block and
the level-20 Wilderness ceiling, plays human sequence 1816 and spotanim 342, waits the cache
sequence duration, interrupts the recipient's prior action and teleports them.

Landing selection follows the pinned Wiki maps. Lumbridge uses the 16 tiles enclosed by its
south-west-corner polygon; Falador and Camelot randomize within radius-two squares centred at
2964,3378 and 2757,3479. Automated coverage fixes the polygon boundary, all three spell contracts,
native button metadata, offer lifecycle, accepted timing, restrictions, atomic failures, cast rate
and real player-target dispatch.

This row is not `verified`: the Wiki confirms Accept Aid and recipient confirmation but does not pin
the exact official rejection strings or whether resource charging precedes acceptance. Temporary
Accept Aid suppression, Ironman rules, full combat/action interruption behavior and matching-client
modal, sound and movement observation remain open.

## Teleport to Target

Teleport to Target now uses the cache-233 all-spellbook contract: level 85 Magic, one law, chaos and
death rune, 45 Magic XP and a four-tick cast. Reading target teleport scroll 12846 presents the
pinned confirmation transcript, consumes the scroll only on confirmation and sets permanent unlock
varbit 2010. The shared rune manager treats a null cache spellbook as an all-spellbook wildcard, so
the spell works from Standard, Ancient, Lunar and Arceuus books as the cache and cutoff Wiki define.

The spell consumes a reusable Bounty Hunter target-state contract rather than guessing arena state
from coordinates. A cast requires an assigned, live target in the crater. It is allowed from the
lobby and from inside the crater once 20 game cycles (12 seconds) have elapsed since combat. After
the standard teleport presentation, it revalidates the same target and chooses a reachable square
within 12 tiles of the target's current position. Each success updates cache quota varbit 5604;
the fifth also sets limit varbit 6503, and a sixth attempt is rejected before runes or XP are used.
Assignment and location state are cleaned on logout.

The spell core is not `verified`: the production Bounty Hunter plugin still needs matchmaking,
target lifecycle and authoritative lobby/crater transitions. Exact cast-failure text, destination
tile selection, interruption and lock behavior, warning varbit 236, scroll presentation pagination,
all four spellbook client states and audiovisual ordering require cutoff client observation. The
current non-transcript failure messages and reachable-tile selection are documented implementation
inferences.

## Teleport to House

Teleport to House now uses its cache-233 Standard-spell contract: level 40 Magic, one air, earth
and law rune, 30 Magic XP, members-only access and a four-tick standard teleport presentation.
House-location varbit 2187 selects Rimmington, Taverley, Pollnivneach, Rellekka, Brimhaven, Yanille,
Hosidius, Prifddinas or Aldarin. Outside casts land on an open tile beside the corresponding cache
portal. A missing or unknown house-location value is rejected before runes or XP are used.

Cache clientscript 2615 governs the five component operations. Operation 1 casts inside; operations
2 and 3 cast outside as the client swaps the visible default `Cast` operation; operations 4 and 5
are Group Ironman choose/previous routes. Inside casts use a dedicated POH-entry repository and
honor default-building-mode varbit 14670. This repository is the boundary by which a future dynamic
house plugin publishes an already-loaded regular or build-mode entrance. It deliberately does not
invent a house instance; if no matching entrance is prepared, the cast rejects atomically. Logout
cleans stale entries. Group routes likewise reject without cost until grouping and house ownership
are implemented.

Automated coverage pins spell object 8828, component 218:31, all four POH varbit layouts, all nine
location values and destinations, four-tick costs/XP/presentation, inside and build-mode routing,
all five operations, membership, level, rune, Tele Block and Wilderness failures, and logout
cleanup. The core is not `verified`: production house instancing, build-mode initialization, Group
Ironman selection, exact official failure text and outside landing tiles, interruption/locking and
matching-client observation remain open.

## Remaining teleport integration

Every Standard spellbook teleport entry now has a server handler, but none of the teleport families
is fully verified. Teleport to House still depends on the future production POH instance and Group
Ironman systems. The three diary alternates cast correctly from saved cache state, while their
right-click configuration flows remain open.

Acceptance requires unlocks, destination selection and landing-square behavior, Wilderness and
teleblock restrictions, animation lock and exact movement tick, interruption, rune use, cast XP,
sounds and graphics.

## Submenu entries

- View Jewellery Enchantment Spells.
- View Bolt Enchantment Spells.

These are navigation actions. Their child spells, interface state, availability states, tooltips and
casting operations are catalogued separately rather than treating the menu entry as the spell.

## Next implementation order

1. Expand representative elemental outcome coverage across every tier and element, including
   damage-arrival timing, sounds, all staff/combo/substitute rune families, PvP targets and client
   observation.
2. Capture the client presentation of tome page removal and bank configuration, verify NPC
   stat-drain exceptions, and client-test all six stat-drain spells.
3. Complete the alternate-destination configuration menus and landing-area randomisation for the
   first ten self teleports, the production Wilderness updater and Tele Block's caster-death
   integration, then client-test timing, presentation and messages.
4. Audit Crumble Undead's production NPC classification and capture staff recharge, Slayer Dart and
   Mage Arena spell animations, sounds and saved state across a real client login.
5. Finish jewellery-enchantment, bolt-enchantment, orb-charging and Monster Inspect client/data
   integration, then continue the remaining non-Standard spellbooks.

### Elemental tome charges

A matching page supplies 20 charges, up to 20,000. The inventory and worn Pages menus load pages or return complete unused pages. Each bank has a saved withdrawal target per tome. A tome with fewer charges takes enough whole pages to meet that target, subject to bank stock. A tome already at or above target consumes nothing. Searing fire pages use a distinct per-item flag and cannot mix with burnt pages. Their minimum hit is calculated before the fire tome's damage bonus. Eligible casts capture their tome effect before the last charge reverts the worn item. Fire and earth tomes exempt other spells that happen to require those runes, including Iban Blast. [Tome of fire](https://oldschool.runescape.wiki/?oldid=14964731) · [Tome of earth](https://oldschool.runescape.wiki/?oldid=14963973) · [Tome of water](https://oldschool.runescape.wiki/?oldid=14898660)
