# Known gaps

For the November 2026 playtest, use the [current skill and diary report](SKILLS_AND_DIARIES.md) and its production and combat links. The dated sections below retain earlier implementation checkpoints.

The detailed entries below record the original audit and incremental parity work. The [skill and diary coverage ledger](SKILLS_AND_DIARIES.md) tracks current playable routes, task hooks, and the scope of the achievement journal.

## Coverage gaps

- The deterministic future-cache comparison engine now validates two versioned inventory manifests,
  normalizes source ordering and emits stable machine-readable added, removed and changed entries
  with migration-risk tags. Its first integration-tested exporter decodes every object, NPC and
  location definition, fingerprints its exact raw config bytes and records behavior-relevant fields
  plus matching typed symbols. Cache 233.4 exposes 103,695 such definitions; 22 locations and six
  NPCs have no matching typed symbol, while object symbol ID 65534 has no cache definition. Those
  identities remain explicit audit gaps rather than receiving fabricated names. The second
  integration-tested exporter adds every cached varp, varbit, varclient, vanilla varobject,
  inventory, parameter, enum, struct, database table/row, sequence and spot animation, bringing the
  semantic inventory to 165,477 definitions across 15 domains. All 1,261 varclients have decoded
  persistence contracts and symbols; the 107 vanilla varobjects remain raw, explicit identities
  pending a semantic decoder and matching vanilla names. A separate integration-tested enriched
  variable exporter inventories all six varns, eleven build-layer varobjects, three varcons and one
  controller; no varnbit or varconbit definitions exist in the current enriched cache/symbol set.
  A map exporter now fingerprints all 2,266 terrain and 2,266 XTEA-protected location maps and
  records collision-sensitive terrain rules plus decoded placement summaries. This closes exact
  raw map/landscape change detection, but not per-tile semantic delta descriptions; all terrain
  decoder remainders are preserved as explicit findings. A separate enriched-runtime NPC exporter
  now proves all six server-loaded `nX_Z` groups: 585 placements, 136 distinct typed identities and
  49 distinct Attack/drop-risk identities across only map squares 49_49 through 50_51. This closes
  deterministic change detection for the world the server currently loads, not the missing
  production-wide spawn dataset; every other accessible region and its NPC population remains an
  explicit parity gap. A separate integration-tested raw
  client-contract exporter fingerprints all 225,204 files across animation frames/bases, interfaces, client scripts,
  keyframes and five audio/music archives while preserving JS5 index metadata. This closes exact
  file-level change detection for those ten archives, not semantic decoding or behavior parity.
  A whole-cache archive-index exporter now fingerprints all 116,016 JS5 groups and their 640,720
  ordered file identities across 23 non-empty vanilla archive indexes. A separate enriched-runtime
  artifact pins rsmod's 373 added and 12 rewritten groups so build-layer mutations cannot be
  mistaken for upstream cache content. This closes local structural cache identity and replacement
  detection even for encrypted or undecoded payloads, and the baseline totals and build/capture
  identity have been reconciled with the official OpenRS2 archive-2317 record. The machine-readable
  revision contract now locks those bytes, the exact XTEAs, `Build` constants and both rsprot
  modules, while the downloader stages and verifies before promotion. Remote catalog authenticity,
  protocol packet-semantic inventory and automatic resolution of multiple concurrent promotion
  journals remain open;
  per-tile map/collision semantic deltas and semantic decoders for the raw client-contract domains
  and vanilla varobjects are also still open before the upgrade playbook is end-to-end. The
  semantic exporter now handles a vanilla projectile-motion group when present, but cache 233.4 has
  no such group: all 16 `projanim.sym` identities come from rsmod's build layer. The dedicated
  enriched-cache exporter now inventories and diffs all 16 separately without contaminating the
  vanilla manifest. Delta-to-backlog conversion is integration-tested: it emits one stable task per
  changed cache identity with explicit evidence, risk-review, regression and target-contract gates,
  but those tasks cannot cover domains whose exporters do not yet exist.
- Poll 85's Fremennik sea boots 1–4 now have their cache-exact held/worn operations, Rellekka market
  destination, shared UTC-daily 1/3/5/unlimited limits and the ordinary members, Tele Block and
  level-20 Wilderness boundaries. Exact rejection wording, animation/graphic/sound and packet
  order, relog persistence and the UTC rollover presentation still require workspace RuneLite case
  102. The Fenkenstrain Mausoleum bridge now has its inventory-material repair core, persistent
  per-player scenery, 59 Construction/69 Agility gates, 550 XP per side, zero-XP crossing and
  two-distinct-tile cinematic trigger. Plank-sack material withdrawal, exact location angle and
  endpoint confirmation, animation/camera/tick presentation, failure wording and packet order remain
  case 103. The cache-233.4 Baxtorian Falls first rope now enforces Waterfall Quest plus the medium
  Kandarin reward and lands on the clue plateau without creating an inventory rope. The snapshot's
  17 September hotfix-disabled second rope, exact first-rope angle/presentation and the post-cutoff
  17 December restoration remain case 104. Poll 85's desktop shop quantity buttons/selected
  left-click amount, the disease-free Harmony patch and Hunter-kit contents remain separately
  researched.
- Woodcutting, ordinary, coloured, handheld-light-source, Barbarian bow, Barbarian pyre-ship, pyre-log-preparation and funeral-pyre Firemaking, standard log-cutting Fletching, furnace/cannonball
  Smithing and the documented Crafting slices have substantive production-skill source; all are
  still partial relative to the full 23-skill target.
- Of 71 castable standard-spellbook entries, 36 combat attacks, Charge, both alchemy spells, both
  bone-conversion spells, Superheat Item, Telekinetic Grab, all seven jewellery enchantments, all
  four orb-charging spells, Monster Inspect, Lumbridge Home Teleport, and the first ten fixed self
  teleports plus Teleport to Target and Teleport to House have handlers. All ten child spells under
  the bolt-enchantment submenu also have handlers;
  see
  `STANDARD_SPELLBOOK.md` for the remaining inventory.
- Cooking now has an integration-tested 58-input standard heat core, including all five
  ordinary meat aliases; rabbit and ugthanki meat; thin, lean and fat snails; lava eel; mackerel,
  cod, pike, slimy eel, rainbow fish, cave eel, bass, sea turtle, dark crab and manta ray; plus the
  nine Hunter meats from wild kebbit through moonlight antelope.
  Bread and pitta dough are now range-only inputs with their exact levels, XP, source curves,
  cadence and pitta's guaranteed-success boundary. Their shared pot-flour and bucket/bowl/jug-water
  preparation route now produces bread, pastry, pizza or pitta dough with exact level, membership,
  timing and container-return rules. Uncooked stew and uncooked curry now have their complete fire/range heat
  stages. Their preparation graph is also integration-tested: bowl water accepts potato or cooked
  meat/chicken in either order through both incomplete-stew identities, and uncooked stew accepts
  one spice or exactly three curry leaves. Cooked meat and cooked mystery meat can also be deliberately burned
  with zero XP. Raw potato now has its range-only baked/burnt route, while scrambled egg, cooked
  sweetcorn, fried onions and fried mushrooms have their fire/range heat stages. All 12 standard
  pies now have their range-only baking stage, exact source curves, shared burnt output and
  natural/cape no-burn rules. Their complete 26-route preparation graph is integration-tested from
  pastry dough plus pie dish through every ordered filling stage, including cooked-meat/chicken
  alternatives, returned compost/water buckets, zero XP, native Make-All and the admiral-pie
  first-result timing exception. Uncooked cake and plain pizza also have their complete range-only
  heat stages, including their distinct burnt outputs, no-burn rules and cake-tin return on either
  outcome. Cake assembly, ordered tomato/cheese pizza assembly, chocolate cake and all standard
  meat/anchovy/pineapple pizza toppings are integration-tested. The chicken-specific stew wording is
  expanded from the Wiki's meat/chicken shorthand, while curry feedback, audiovisuals, slot/packet
  order and failure precedence still need matching-client capture. Exact pie-preparation menu
  presentation, messages, animation, sound, packet/slot ordering and the admiral first-result
  client boundary also need matching-client capture. The complete 22-route potato-topping graph is
  integration-tested, including all prerequisite bowl mixtures, knife checks, alternate butter,
  XP/ticks and the four historically explicit bowl-return paths. Its exact missing-knife feedback,
  success messages, audiovisuals and packet/slot order still need matching-client capture. All seven
  dairy-churn routes are integration-tested across the four cache churn identities with exact
  level/XP/tick tables, source-filtered batches, cache-backed player/churn animation tiers and
  returned or privately owner-dropped milk buckets. The exact production menu labels and action
  mode, native-click priority when several source types coexist, success/failure feedback, sound,
  graphic placement and packet ordering still need matching-client capture. Ordinary and Zamorak
  grape-wine production is integration-tested through its shared cap and fermentation lifecycle.
  The exact cap rejection, menu presentation, output allocation between inventory and bank,
  logout/offline timer behavior, full-bank production behavior, graphic timing and sound/packet
  order remain matching-client gaps. The local RuneLite timer starts a 13.8-second display from
  graphic 47 while the dated Wiki specifies fermentation 20 ticks after the latest item result;
  production follows the Wiki result boundary pending a packet-timeline capture. The complete
  ordinary nettle-tea and hot-water production chain is integration-tested, including all cache
  identities, containers, level/XP/burn rules, heat sources and quantity routes. Exact messages,
  the nettle-water and bowl-to-cup menu presentation, the zero/one-tick pour cadence, sounds and
  packet/slot ordering remain matching-client gaps; the current heat feedback is explicitly an
  inference from the dated recipe wording. Ordinary Cup of tea 1978 consumption is now separately
  integration-tested with its exact heal/Attack boost, empty-cup replacement, forced speech,
  sequence, standard food/active-attack delays and Goading cancellation. Tea flask doses, POH
  construction teas, nettle-tea drinking effects, nettle gathering/glove checks and the Ghosts
  Ahoy porcelain-cup handoff belong to separate item, Construction, gathering and quest slices and
  remain open. The exact ordinary-tea frame sound and full matching-client packet ordering remain
  manual observations.
  Direct Goading-potion drinking is now integration-tested for every dose, the independent
  three-cycle potion clock, sequence 829, exact same-slot vial chain, automatic vial smashing and
  six-minute effect application/reset. The item-specific first chat line follows the established
  ordinary-potion template but remains a bounded inference until a matching-client capture.
  Ordinary Antipoison is now separately integration-tested across all four cache doses with its
  exact directly captured messages, free-to-play access, the same timer/sequence/vial behavior and
  the full two-dose venom-downgrade/cure path. Superantipoison, Antidote+, Antidote++, Anti-venom,
  Anti-venom+ and Extended anti-venom+ are also integration-tested across all 24 dose objects with
  their exact members flags, cache examine stems, dose chains, shared timer and poison/venom
  protection states. The Extended anti-venom+ first line is directly documented; the other five
  first lines compose the authoritative cache stems with the stable potion-message template and
  remain bounded inferences pending matching-client capture. Direct Sanfew serum is likewise
  integration-tested across all four doses, including full stat/Prayer restoration, toxin and
  disease effects and standard-death removal of disease immunity; its first line is also a bounded
  template inference. All four antifire families are now integration-tested across their 16 dose
  objects, Dragon Slayer I started-state gate, exact 600/1,200/300/600-cycle effects and ordinary/
  superior tier replacement. Ordinary antifire's `You drink some of your dragon potion.` line is
  directly observed in the historical content reference; Extended antifire, Super antifire and
  Extended super antifire use bounded family-name template inferences. The exact quest-failure
  wording is supported only by a post-cutoff Wiki revision and remains a matching-client check.
  Restore, Energy, Prayer, Super energy, Super restore and Stamina are now integration-tested
  across all 24 dose objects. Coverage pins Energy as the only free-to-play family, exact stat,
  Prayer and run-energy formulas, each player's holy-wrench/cape/ring Prayer modifier and the
  exact 200-cycle Stamina reset. Restore, Energy, Prayer and Super restore first lines are directly
  observed in the historical content corpus; Super energy and Stamina retain bounded stable-
  template inferences. Ring of Endurance extension, Stamina persistence/death/world-hop policy,
  expiry sound/message, sequence-frame sounds and exact inventory/message/stat/varbit packet
  ordering remain open. The thirteen ordinary stat-boost families are also integration-tested
  across all 52 dose objects with exact cache membership, immediate formulas and dose behavior.
  Historical content directly pins ten first-line families, including ordinary/super Attack,
  Strength and Defence sharing the base-family wording; the pinned Wiki revisions directly pin
  Bastion and Battlemage's full tangy lines. Super combat remains a bounded stable-template
  inference. Minute stat decay, Preserve interaction, sequence-frame sound and exact inventory/
  message/stat packet ordering remain matching-client work. Every other direct potion family
  remains open.
  Wrapped-oomlie preparation and heat production are integration-tested with exact levels, XP,
  curves, manual/automatic cadence and continued production through the historically documented
  cold drain. Exact messages, menu presentation, sounds, packet/slot ordering and Karamja diary
  credit for eating the result remain open. The distinct raw-oomlie forced-burn path is not yet
  implemented because dated evidence pins fire use and two messages, but not result timing or
  whether ranges accept it. Karambwan Cooking is integration-tested before and after its durable
  Tai Bwo Wannai Trio/Tinsay unlock: poison and thorough outputs, warning preference, success and
  burn paths, XP, source curves, cadence, interruption, cape immunity and successful-thorough
  Karamja hard-diary credit are covered. The exact poison-warning preference screen, skill-menu
  padding/action mode, sounds, packet order and source-specific client feedback still require the
  matching cache-233 client; the Tinsay reward itself remains part of the open quest slice. Sinew
  production from raw beef, bear meat and boar meat is integration-tested on all ranges, including
  the dual-output chooser, 3 XP, source curves, immediate/3/4 cadence, full-inventory replacement,
  burn, cape and interruption behavior. The exact failure message/product association, direct
  one-item chooser presentation, range-animation packet timing and sound remain matching-client
  observations. Ordinary iron-spit bird, beast and rabbit preparation/roasting is integration-tested
  with exact cache objects, levels, XP, curves, timing, requirements, returned tool and failure
  outputs. The historical recipe pages do not publish exact feedback or audio, and only the bird
  roast page explicitly rechecks Firemaking 20 at the fire; those messages, sounds, packet/slot
  ordering and requirement distinction remain matching-client observations. Bake Pie's core spell
  workflow is integration-tested under Magic; other bowl dishes and the remaining
  vegetable recipes stay open. Remaining special fish and unusual-meat families and non-nettle drinks,
  item-use-only heat sources, appliance-specific restrictions, cooked-chicken overcooking, other
  special methods and exact client presentation remain open. Exact cake capacity feedback and the
  production client's maximum cake-batch presentation remain open. The exact production dough
  chooser title/order, non-cake preparation messages, failure precedence, output-slot ordering,
  animation, sound and packet order also remain open. The
  pre-cutoff burnt-chicken recipe does not publish its repeat timing, so that route is retained as a
  researched observation instead of inheriting an unproven cadence.

  Every cache location with an explicit `Cook` operation is classified and registered. Manta-ray
  Cooking does not yet advance its Ardougne elite task because the broader diary-task system is
  absent. Dark-crab Cooking likewise does not advance the Wilderness elite catch-and-cook task,
  and thin-snail Cooking does not yet advance the Morytania easy task. Lava eel is possession-gated:
  its Heroes' Quest acquisition context is not converted into an unsupported Cooking quest-state
  check. Chompy iron-spit preparation and three-tick fire Cooking are now integration-tested as a
  dedicated workflow, including returned-tool capacity and interruption. The ogre spit roast now
  integration-tests every accepted cache-233 input: raw chompy, raw chicken and raw jubbly. Its
  per-player cache morphs, exact result ticks, one-tick cooked/burnt display, atomic inventory
  replacement, membership/level/quest gates and interruption/login/logout cleanup are covered by a
  dedicated state machine. Exact client messages beyond the matching-client observations, sound
  identity, packet ordering and the Big Chompy Bird Hunting quest's seasoned-chompy preparation
  route remain open.
  The Hunter foods' staged on-eat healing is separate item/combat behavior and remains
  unimplemented. Fishing now has production cores for all 21 ordinary small-net/bait spot types,
  all 26 ordinary lure/bait spot types, the 16 cache-233 spots whose first operation is truly
  `Cage` and the exact 18-NPC cutoff family whose third operation is ordinary harpooning. Exact
  cache operation slots, protected five-tick rolls, ordinary cascades, lobster/tuna/swordfish
  levels, XP and rates, regular and carried/worn barb-tail harpoons, members-only 1/2500 big
  swordfish replacement, equipment, success-only bait, pearl animations, pike's 1-in-5 extra tick
  and atomic inventory behavior are integration-tested. Cache inspection also prevents lobster from
  being incorrectly dispatched on the two `Net/Harpoon` and one `Big Net/Harpoon` variants that
  appeared among the source-taxonomy candidates. The wider Fishing skill remains incomplete. The
  250–530-tick relocation graphs, river/sea catch distinction, dual-feather priority,
  pike/rainbow/lobster message confirmation, tick manipulation, sounds and exact client
  presentation remain manual cases 69–71. Barehanded tuna/swordfish now has an integration-tested
  Otto-state gate, ordinary-tool priority, boosted 55/70 Fishing gates, base 35/50 Strength gates,
  regular-harpoon rates, 8/10 Strength XP and first-catch training transition. Exact multi-stage
  animation choreography and conditional clue-bottle/heron rolls remain open for that family.
  Equipped Rada's blessings now have their exact 2/4/6/8-percent ordinary single-output
  duplication with no extra XP, including big-fish replacement. Carried spirit flakes now add 50
  percentage points, consume one per eligible catch, preserve ordinary bait use and do not consume
  with only one output slot. Active infernal extra fish now gets its own independent cook roll,
  half Cooking XP and cook-only charge use; active crystal tools consume one charge per multiplied
  fish. Simultaneous doubled-output behavior when only one tool charge remains still needs matching-
  client evidence. The exact 20-NPC
  members-only big-net/harpoon shark family is now integration-tested for regular and barb-tail
  tools, five-tick level-76 110-XP catches at 3/40 endpoints and the 1/5000 big-shark replacement,
  including the source/cache operation mismatch on NPC 5820. The other 19 source-family identities'
  first-slot big-net operation is now integration-tested with its members/tool/base-level gates,
  five-tick sequence, nine independent ordered rolls, two mackerel rolls, exact level/XP/rate
  table, charged-skills-necklace casket modifier, 1/1000 big bass and atomic partial-capacity
  behavior. Every accepted big-net primary result now receives the additive carried-spirit-flake
  and equipped-Rada multiplier, including non-fish junk and selected big bass. A flake is consumed
  on each eligible accepted result, doubled output takes capacity before later primary rolls, and
  XP/messages remain per primary result. Matching-client junk wording, multiplier presentation and
  whether consuming the last flake immediately reclaims its inventory slot remain manual
  observations rather than exact claims. All cache-233 dragon, infernal, crystal,
  ornamented, Reloaded and depleted harpoon variants are integration-tested for carried/worn
  selection, use levels, cache animations and exact quantized 20%/35% rate increases. The exact
  Fishing Guild NPC 1510/1511 ordinary methods now apply the invisible +7 to success chance after
  visible-level gates, including specialist-harpoon stacking and big-net independent rolls. Shark
  lure, exact infernal creation/recharge/passive/ornament-operation presentation, bank crystal
  Configure-Charges, clue bottles,
  Varlamore moon-key halves, pet rolls, aerial/minnow methods and every Fishing activity
  remain open under manual case 72 or later slices. Frogspawn first-option small-net fishing is now
  integration-tested across NPCs 1497–1500/10653: members-only visible level 33, five-tick attempts,
  small net, a level-scaled frog-spawn 16/96 roll followed by the constant swamp-weed 10/10 fallback,
  75/1 XP, Rada/spirit-flake multiplication, fish-barrel exclusion and protected revalidation. It
  cannot advance beyond `integration-tested` because the final pre-cutoff item-page success chart
  implies 43/256 frog spawn at level 33 and about 35.8/1000 fallback swamp weed, while the same-era
  spot table instead prints 11/256 frog spawn and about 43/1000 swamp weed. The Wiki beta-testing
  backlog explicitly says frog spawn is stat-random but still asks whether swamp weed is. Exact
  catch messages are also inferred from the ordinary Fishing family, Rada eligibility for these
  non-fish outputs is generic rather than named, and the spirit-flake Camdozaal exclusion appears
  only after the cutoff. Manual case 79 therefore requires direct cache-233 rate and presentation
  captures. Ordinary eel bait
  fishing is now integration-tested separately across cache frogspawn/cave NPCs 1497–1500/10653
  and Mort Myre NPCs 2653–2655. It covers exact option dispatch, members-only ordinary/worn-pearl
  rod and bait gates, five-tick attempts, the level-38 cave-eel before level-28 slimy-eel cascade,
  80 cave XP versus 65 Mort Myre XP, 10/80 endpoints, success-only bait, Rada/spirit-flake
  multiplication, fish-barrel routing and protected revalidation. Area access is owned by the
  cave/quest transports rather than the fishing action, while relocation, clue bottles, heron,
  exact rejection/audiovisual/packet presentation and last-bait ordering remain manual case 78.
  Lava-eel production is now integration-tested on cache NPCs 4928/6784: first-option Bait, visible
  level 53, oily ordinary or carried/worn oily pearl rod, bait, five-tick 16/96 rolls, 60 Fishing
  XP, the documented free-world exception, success-only bait consumption and protected capacity,
  distance and tool revalidation. The 60-XP value is selected over the spot page's isolated 30-XP
  row because the cutoff raw-item page and Fishing data independently agree on 60, while 30 is the
  cooked lava eel's Cooking XP. Heroes' Quest owns learning and acquiring an oily rod; no source
  establishes a second catch-time quest-state gate after the player has one, so production is
  possession-based and a free-world/zero-quest-state regression is pinned. The exact Heroes'
  Quest acquisition flow, the hard Wilderness Diary task bit, no-heron/clue behavior, exact
  context-sensitive messages, Rada/spirit-flake applicability, relocation and matching-client
  presentation remain researched under manual case 79.
  Lava eels remain unimplemented: the final pre-cutoff raw-item page says 60 XP while the final
  pre-cutoff spot page says 30 XP, and the exact Heroes' Quest technique state plus Lava Maze diary
  variable still require resolution before behavior is committed. Infernal and sacred eel catching
  is now integration-tested on cache NPCs 7676/6488. It covers their first-option dispatch,
  membership and boosted 80/87 gates, ordinary and wearable pearl rods, fishing bait, infernal ice-
  glove/Smiths-gloves-(i) equipment, sacred Zulrah-education varbit 4391, five-tick 30/93 and 0/60
  rolls, 95/105 XP, Rada/spirit-flake doubling, fish-barrel routing and protected revalidation.
  Their catch lines and infernal missing-glove line are ordinary-family inferences pending direct
  client capture. Source pages establish the inner-Mor-Ul-Rek and Zul-Andra/High-Priestess access
  context, but entrance and quest-state implementations remain separate dependencies. Infernal-eel
  manual infernal-eel hammer smashing and sacred-eel knife dissection are now integration-tested.
  The infernal core uses the first eel, exact 29/35 Tokkul, 2/35 onyx-tip and 4/35 lava-shard
  branches, both cache animations, atomic output and same-tick spam. Sacred processing uses visible
  Cooking 72, the exact three-to-nine-scale bands, `100 + 3 × scales` XP, the first eel and a
  two-then-three-tick automatic queue with requirement revalidation. Infernal automatic cadence is
  deliberately still unimplemented because neither historical evidence nor sequence duration fixes
  its repeat interval. Direct cache-233 capture must also confirm both inferred failure lines, the
  sacred Eat line currently supported only by a secondary mirror, sounds, packet order, movement,
  combat/interface interruption and animation continuity under manual case 80. Post-quest
  karambwan Fishing is now
  integration-tested separately for cache NPCs 4712/4713, vessel loading and automatic reloading,
  completed-quest/membership/visible-level/capacity gates, four-tick 5/160 rolls, success and
  failure bait use, 50 XP, medium-diary credit, Rada/spirit-flake doubling, fish-barrel routing and
  protected revalidation. Lubufu's quest-only NPC 4714 and exact pre-completion learned-technique
  state remain deliberately unregistered because cache varp 320 alone does not expose that
  substep. Exact acquisition/replacement dialogues, success/start/rejection presentation, main-
  spot relocation anomaly, conditional clue bottles/heron, audiovisuals, packet order, tick
  manipulation and broad interruption behavior remain open under manual case 77.
  Holy Lake karambwanji acquisition is now integration-tested separately: cache NPC 4710, the
  members/level/small-net/capacity gates, six-tick 100/250→48/256 cascade, five XP, boosted-level
  stack quantity, Rada/spirit-flake whole-catch doubling and the later pre-cutoff full-inventory
  shrimp suppression are covered. Exact relocation timing, clue-bottle/heron rewards, raw-catch
  wording and audiovisual/packet presentation remain open under manual case 76. There is also a
  preserved source conflict: raw-karambwanji revision 14970676 says a full inventory still rolls
  and silently discards shrimp without stopping, while the later fishing-spot revision 14992999
  says shrimp are no longer caught. Production follows the later revision's observable rule;
  direct cache-233 client capture is required before this lifecycle row can advance.
  The infernal harpoon's gameplay passive is now integration-tested for all three charged styles:
  persisted 5,000-use state, 1/3 ordinary-fish auto-cooking, half Cooking XP, success-only charge
  use, carried/worn depletion into the matching empty style, and exclusion of big fish. Base
  dragon-harpoon plus smouldering-stone creation is now integration-tested in both held-use orders,
  including the members-only unboosted 75 Fishing/85 Cooking gates, 200/350 XP and atomic full-
  inventory conversion. All three depleted styles now recharge to their matching full 5,000-charge
  style with either a smouldering stone or dragon harpoon, without skill gates or XP. Trailblazer
  kit application now converts charged or depleted base tools atomically while preserving partial
  charge state. All four ornamented charged/depleted identities now expose cache-slot Revert and
  Change-Style behavior: Revert returns the matching plain tool and kit atomically, while inventory
  and worn style changes alternate original Trailblazer and Reloaded forms without losing packed
  state. Charged base, original Trailblazer and Reloaded tools now report canonical or partial
  remaining-charge state from their cache-distinct inventory Check slots and shared worn Check
  slot. All four ornamented charged/depleted identities now Wield through their shifted first item
  option while preserving packed state. Matching-client confirmation behavior, exact Check wording,
  other messages, audiovisuals and packet order for the creation, recharge, passive and ornament
  operations remain open.
  Crystal-harpoon depletion is also integration-tested: 10,000 initial and 20,000 maximum persisted
  charges, one charge per successful ordinary or big catch, none on failed attempts, carried/worn
  final-charge transition to inactive 23764, and inactive dragon-rate fallback. Active inventory
  and worn Check now report canonical or partial persisted charge state. Active and inactive cache-
  slot Revert atomically return dragon harpoon 21028 plus crystal tool seed 23953, with full-
  inventory safety. Shard use now charges either form through bounded quantity input at exactly 100
  uses per shard up to 20,000, including inactive-to-active conversion and full-inventory safety.
  Active crystal harpoons now consume the shared bank Configure-Charges event and store a members-
  only zero-to-20,000 preference in permanent varbit 16148 through cache param 2257 and DB row 4338.
  Automatic shard deduction and item-local charging during withdrawal plus exact configuration
  presentation remain matching-client work.
  Equipped elven and both celestial signets now save each crystal-harpoon charge at exactly 10%;
  equipped Ghommal's lucky penny saves each crystal or infernal-harpoon charge at exactly 5%.
  Signet and penny rolls remain independent for each resulting fish, including multiplied catches.
  Barehanded shark is integration-tested at boosted Fishing 96, base Strength 76, the ordinary
  harpoon success rate and 11 Strength XP. The shared implementation gives an ordinary usable
  harpoon priority, uses cache loop sequence 6709 and pins the complete 6703–6711 animation family.
  Exact start/ready/catch sequence choreography and matching-client rejection presentation remain
  open. Barbarian heavy-rod Fishing is now integration-tested on cache NPCs 1542/7323 with Otto's
  state gate, both regular and worn pearl rods, the exact five-item bait priority, boosted Fishing
  and base Strength/Agility requirements, ordered leaping-sturgeon/salmon/trout cascade, source
  success endpoints, triple-skill XP and first-catch transition. The literal training, low-stat,
  missing-tool and missing-bait rejection text; two-phase 9349→9350 and 8190→8193 animation
  choreography; relocation and tick manipulation; clue bottles and heron
  rolls remain matching-client work rather than exact production claims. Knife gutting of all
  three leaping fish is now integration-tested with the exact success and conditional-offcut
  fractions, roe/caviar outputs, Cooking XP, failure loss, cache sequence 6702 and automatic
  three-tick selected-type continuation that can be accelerated by another item-use action. Exact
  messages, mixed-fish continuation order, boost/drain behavior, manual/automatic precedence,
  interruption packets and the two-new-stack full-inventory outcome remain matching-client work;
  production conservatively preserves the primary roe/caviar and suppresses only an offcut that
  cannot fit until that boundary is captured.
  The ordinary fish barrel core is now integration-tested for cache objects 25582/25584: all 33
  pre-Sailing eligible fish can be filled into permanent 28-slot storage, contents survive
  Open/Close, bank-side emptying and Destroy are wired, and an open barrel accepts ordinary
  single-output Fishing results even when the inventory is full. Doubled catches split safely
  between the last barrel slot and inventory, with the sourced full-barrel message. Exact Check
  interface formatting and operation feedback, bank/deposit failure behavior, UIM withdrawal,
  death/gravestone rules, multiple barrels and every other non-ordinary acquisition source remain
  manual case 75 and later slices. Fish-sack-barrel assembly, cape-slot equipment, held/worn
  Check/Open/Close, open worn acquisition, content-preserving dismantling and Destroy clearing are
  now integration-tested; exact Configure presentation, equipped-bank behavior and death or
  multiple-copy ownership remain manual. Ordinary big-net raw mackerel, cod and
  bass now route into open storage in source order, including full-inventory and final-slot doubled
  catches; junk and trophy bass remain inventory-only. Exact multi-result packet ordering and the
  final-spirit-flake slot-reclamation boundary are still matching-client work.
  Bank Configure-Charges, Song of the Elves access integration, exact charging/Check/Revert wording
  and Revert confirmation, PvP-death behavior and any matching-client conservation proc feedback
  remain open.
- Lunar Bake Pie now has its complete cache-233 pie table and core automatic spell workflow, but
  the historical Wiki does not publish its literal no-pie, quest or Cooking rejection text, sound
  identity, mixed eligible/ineligible inventory behavior, or packet ordering. Cache sequences 4413
  and 4422 and spot animations 727 and 746 coexist; local revision-matched RuneLite identifies
  player animation 4413 and graphic 746, while the unused presentation pair must not be guessed
  into production. Varrock elite summer-pie credit also depends on the still-open diary framework.
  These observations are isolated in manual case 36, so the core stays `integration-tested`.
- Lunar Humidify now atomically fills all 40 item-to-item targets evidenced for cache 233: ordinary
  containers, every partial waterskin and watering can, clay, the Chambers gourd vial and all 20
  pre-Sailing seedlings. Its cache spell/rune/XP contract, members and both quest gates, rune
  substitutions, Fountain policy, presentation and three-tick recast rate are integration-tested.
  Gricoller's can remains open because cache 233 exposes only one item identity and this repository
  has no evidenced 1,000-charge backing state. Great Olm flame-wall extinguishing, Soul Wars
  restriction and Desert hard-diary credit depend on absent encounter/activity/diary systems. The
  historical Wiki does not publish the literal no-target or quest feedback, sound, graphic height,
  rain/no-rain choice or packet order. These are isolated in manual case 37 rather than guessed.
- Lunar Hunter Kit now implements the cache-233 spell and item core: members, Lunar Diplomacy,
  Dream Mentor, Lunar spellbook and Magic-71 gates; two astral/two earth runes; 70 Magic XP;
  animation 6303, graphic 1074 and sounds 3615/3616; the exact duplicate-equipment rejection; and
  atomic unpacking into all eight contents when seven additional slots are free. The Wiki's exact
  Entrana rejection is known, but the production island boundary is not yet evidenced in this
  repository and is not approximated. Poll 85's one-time Feldip Hills/Rellekka hunting-expert grant
  is now integration-tested against NPCs 1504/1503, exact successful/repeat lines and a shared
  permanent server-only entitlement. Their complete pinned legacy tutorial trees and the Feldip
  base-Hunter-99 Skillcape route are also implemented, including exact 99,000-coin charging, the
  free hood, first-mastery untrimmed and multi-mastery trimmed cape selection, atomic capacity
  rejection and cache jingle 231 after a successful purchase. Both cape variants now also expose
  their cache-exact held `Teleport` and worn `Boost`, `Hunter Guild`, `Carnivorous Chinchompas` and
  `Black Chinchompas` operations. The boost, shared five-use UTC-daily quota, exact three landing
  squares, members/Tele Block/deep-Wilderness rejection and permanent Apatura guild unlock are
  integration-tested. The shared second-level-99 lifecycle now irreversibly trims all 23 ordinary
  cape families in inventory, worn equipment and bank, preserving item variables and rebuilding
  worn appearance. Ordinary ground `Take` now trims for a player with multiple masteries, while
  the independent Telekinetic Grab path preserves the untrimmed cape. POH cape-rack storage, death
  storage, Forestry clothes-pouch and Huntsman's-kit exceptions remain open because those backing
  stores are not yet connected to the shared lifecycle. All 46 ordinary cape identities now apply
  their corresponding +1 visible boost on successful equip. Every cache-exposed worn `Boost`
  route is registered: 42 generic non-Hunter cape identities plus the two Hunter identities in
  their dedicated plugin. Cache 233.4 leaves both Agility-cape worn-operation-one fields blank, so
  no synthetic Agility menu operation is added; its equip boost remains active. Hitpoints and
  Prayer use independent permanent server-only absolute timestamps for their exact 60-second reuse
  boundaries, while ordinary stat restoration remains owned by rsmod's shared visible-stat timer.
  Exact client decay phase, relog presentation, silent-versus-message cooldown feedback and the
  Agility cache/Wiki discrepancy remain manual case 108. The grant's
  literal full-inventory feedback, any lost-hood reclaim route not present in the pinned Hunter
  transcript, exact facial animations/item zoom, client jingle playback/mixing, packet/item
  ordering and matching-client behavior remain manual cases 38, 105, 107 and 108. Skillcape teleport
  presentation, Wilderness-destination warning, locked direct-guild feedback, Apatura's initial and
  Hunters' Rumours branches, and the exact internal field allocation within cache varp 1312 remain
  isolated in manual case 106 rather than being presented as verified.
- Lunar Magic Imbue now implements its standalone cache-233 spell core: members, Lunar Diplomacy,
  Lunar spellbook and Magic-82 gates; two astral/seven fire/seven water runes; 86 Magic XP;
  sequence 8970 and synth 2888; a zero-tick cast that does not claim the shared utility cooldown;
  and an exact 21-tick active/recast boundary consumed by Runecraft code. Client varbit 5438
  is synchronized in ten-tick units and explicitly cleared on login/logout because its cache base
  varp is permanent. The server now implements all 12 elemental combination-rune directions with
  exact levels/directional XP, pure essence and opposite-rune consumption on success or failure,
  one talisman per interaction unless Magic Imbue bypasses it, unchanged independent 50% rolls and
  multiple batches per cast. Binding necklace is 100%, stores its 16-use counter in cache varp 487
  across item swaps, exposes the exact held Check operation, disintegrates on use 16 and resets on
  destroy. Aether is the stricter level-90 Cosmic-Altar route requiring Magic Imbue, a worn Binding
  necklace, soul runes, pure or Daeyalt essence and one catalyst per created rune. Raiments,
  Twisted/Scarred Extracts, Guardians of the Rift, exact unsourced rejection text, death/region
  effect boundaries, sequence 8982, graphic height, packet ordering and matching-client behavior
  remain manual cases 39 and 129 rather than being guessed.
- Ordinary cache-233.4 true-altar crafting now covers Air, Mind, Water, Earth, Fire, Body, Cosmic,
  Chaos, Astral, Nature, Law, Death, Blood and Wrath. One altar interaction atomically converts all
  eligible rune/pure/Daeyalt essence, uses the pre-action visible level for every multiple-rune
  threshold, awards XP per essence rather than per output, and gives Daeyalt its exact 50% XP
  increase. Members and the four quest-completion gates are enforced through client-script-4024-
  resolved cache varbits. Matching Warped/Twisted/Mangled/Scarred Extracts apply only from base
  Runecraft 60 after Desert Treasure II has started, consume at most one per essence, add their
  fixed 250/60 outputs and never add XP. Every Raiments colour applies the exact ten-rune block and
  remainder roll, with the four-piece 20% set bonus, only to the base output before extracts.
  Sunfire crafting, mind/body/chaos golem cores, pouches, the Abyss, Ourania, Guardians of the
  Rift, diary/task credit, exact
  audiovisual and packet order, failure wording and matching-client observation remain manual
  case 130. Contemporary cutoff material describes each golem core as ten essence for ten-times
  output and XP, while a post-cutoff February-2026 correction says eleven-times output but
  ten-times XP; the server does not guess across that historical conflict.
- The Kourend dense-essence production core is now integration-tested. Dark Altar 27979 converts
  every dense block after exactly three ticks for 2.5 Runecraft XP each, drains one Prayer point
  per block without requiring Prayer, and preserves each slot. Chiseling at Crafting 38 replaces
  one selected dark block with a counted group of four fragments in that same slot for 8 Crafting
  XP, including in a full inventory. False Blood/Soul altars 27978/27980 consume all fragments at
  levels 77/90 for 23.8/29.7 XP each. Scarred Extracts, all Raiments variants, Kourend elite's
  rounded-down 10% Blood bonus and active Blood essence compose additively without bonus XP.
  Dense-runestone mining is now integration-tested separately: carried pickaxe/chisel and visible
  Mining/Crafting-38 gates, fixed nine-tick blocks, 12/8 XP, exact 210/230 persistence plateaus,
  personal alternating depletion, the medium diary's 5% capacity-bounded second block, crystal
  preservation and cape/Varrock exclusions. Its one-third re-entry randomisation awaits the three
  surrounding Agility transports; the sourced 3-to-2-tick sub-delay claim conflicts with observed
  fixed nine-tick gathering and has no published formula. Those conflicts plus exact text, sound,
  animation and packets remain manual case 132. Automatic chisel cadence/interruption, Blood-diary
  task credit, Soul-rift/Abyss unlocks, tablet/nullifier uses and death/bank policy also remain.
- Active Blood essence now has an integration-tested true-altar lifecycle: cache identities
  26390/26392, members-only activation, one active across inventory/bank, item-local 1,000 charges,
  exact Check text, independent 50% pure/Daeyalt rolls, one charge per successful extra rune, no
  extra XP and atomic destruction at zero. Its dark-essence/Kourend integration is now covered by
  the Kourend core; guardian-essence/Guardians remains coupled to that unimplemented minigame. The Ourania no-effect
  boundary, Ultimate Ironman death and item-retrieval destruction, exact activation/full-space
  presentation and live packet/audio order remain manual case 130 rather than being inferred.
- Essence pouches now have an integration-tested cache-233.4 functional core. Small, medium, large,
  giant and Colossal pouches use the cache's permanent transmitted amount/type/degradation
  variables, accept rune, pure, Daeyalt and guardian essence, enforce unboostable base Runecraft
  levels and implement every capacity, shrink and destruction threshold. Ordinary pouches add a
  random 0–3 wear per essence; Colossal adds exactly one, matching its official 320-essence first
  degradation and the post-cutoff RuneLite correction. Runecraft and Max-cape variants suppress
  further wear without repairing existing damage. Held Fill/Empty/Check, dynamic bankside
  Fill/Empty, partial emptying, threshold excess return, drop clearing and repair across inventory
  and bank are covered by ten integration scenarios. Acquisition order, Colossal creation and
  mutual exclusion, login cleanup, Dark mage/Astral Contact/Cordelia presentation, Guardians'
  Redwood-lantern protection, death/item-retrieval behavior and exact messages, sounds and packet
  order remain manual case 131, so this core is not client-tested or verified.
- Lunar Stat Spy now implements its cache-233 player-information core: members, Lunar Diplomacy and
  Dream Mentor gates; Lunar spellbook and visible Magic-75 requirements; two astral/two cosmic/five
  body runes; 76 Magic XP; all 23 current/base skill rows; the native side panel, target name,
  close control, sequence 6293, caster graphic 1060, synth 3620, target notification and a
  five-tick lock. Cache 233 labels the spell `Combat` with nominal max hit 1, so the offensive PvP
  and PvN registries now explicitly exclude it; the dedicated handler does no damage and does not
  consult Accept Aid. Direct comparison of the dated Stat Spy, Cure Other and Heal Other Wiki
  captures shows the same target crystal effect; the later asset record and cache identity resolve
  that effect as graphic 738, correcting the earlier provisional 736 choice. The historical Wiki
  still does not publish literal notification text, graphic timing/height, recast timing, packet
  ordering or activity/privacy edge cases. These points remain manual case 40 and block
  client-tested/verified promotion.
- Lunar Spin Flax now implements its cache-233 spell core: members, Lunar Diplomacy, Lunar
  spellbook and visible Magic-76 gates; one astral/two nature/five air runes; 75 Magic XP; up to
  five inventory-order flax-to-bow-string transforms with 15 Crafting XP each; the five-tick rate;
  one queued follow-up cast; exact no-flax feedback; same-slot full-inventory replacement; shared
  staff, rune-pouch and Fountain policy; sequence 4413, graphic 728 and synth 3615; and automatic
  account-backed bow-string-spool storage with loose overflow when its capacity is exhausted. The
  final pre-cutoff Wiki documents the one-deep queue but not its replacement/cancellation rules,
  cross-action lock behavior, graphic height/delay, packet order or movement/interface
  interruption. Those observations remain manual case 41 and block client-tested/verified
  promotion.
- Lunar Tan Leather now implements its cache-233 spell core: members, Lunar Diplomacy, hard
  Fremennik Diary, Lunar spellbook and visible Magic-78 gates; one astral/two nature/five fire
  runes and 81 Magic XP once per cast; up to five mixed eligible hides transformed in inventory
  order; all eight cache-era tanning outcomes; soft/hard cowhide Setup; a three-tick one-deep
  queued recast; same-slot full-inventory replacement; shared rune-substitution and Fountain
  policies; and sequence 3014, graphic 322 and synth 2879. The final pre-cutoff Wiki establishes
  the batch, costs, rate, manual recasting and cowhide toggle, but does not publish mixed-hide
  ordering, exact Setup menu/default/persistence, literal empty-input/diary feedback, third-click
  replacement/cancellation, graphic timing/height or packet order. Those observations remain
  manual case 43 and block client-tested/verified promotion.
- Lunar String Jewellery now implements its cache-233 spell core for the 14 outputs supported by
  the cutoff spell and experience tables: gold, opal, jade, topaz, sapphire, emerald, ruby,
  diamond, dragonstone, onyx and zenyte amulets, unblessed holy symbol, unpowered unholy emblem and
  salve amulet. It enforces members, Lunar Diplomacy, Lunar spellbook and visible Magic-80 gates;
  charges two astral/five water/ten earth runes and awards 83 Magic plus fixed 4 Crafting XP for
  each three-tick inventory-order result; uses same-slot atomic transforms and cache sequence 4412,
  graphic 730 and synth 2905; and follows shared rune-substitution and Fountain policies. M'speak
  is intentionally excluded: ordinary ball-of-wool stringing supports it, but the cache-era spell
  table does not. Exact empty-input feedback is not published by the final pre-cutoff Wiki, and
  repeated sound overlap, packet order, graphic timing/height and cross-action interruption remain
  manual case 42. Those observations block client-tested/verified promotion.
- Lunar Plank Make now implements its cache-233 core for normal, oak, teak and mahogany logs with
  exact 70/175/350/1,050-coin fees, members/Lunar Diplomacy/Dream Mentor/Lunar spellbook/visible
  Magic-86 gates, two astral/one nature/fifteen earth runes and 90 Magic XP per result. Manual
  casting converts the first supported inventory log immediately even when a later log was
  targeted; the automatic loop continues in inventory order after five ticks and then every six
  ticks, while a new manual request may queue at
  the shared three-tick utility boundary. Outputs use the first free slot, vouchers double one
  result only when an ordinary inventory slot is already free, and cache sequence 6298, graphic
  1063 and synth 3617 are emitted. Four cache varbits prove per-plank confirmation settings exist,
  but their dialog text, option ordering, default/polarity and application points remain unknown
  and are not implemented. The spell article's precise immediate/+5/+6 schedule conflicts with
  recipe tables that label autocasting as five ticks; production follows the spell-specific
  account pending matching-client capture. Exact invalid-target/coin feedback is only supported by
  secondary emulator evidence. Plank-sack voucher capacity, Varrock elite diary credit,
  movement/interface/cross-action interruption, graphic timing/height and
  packet order remain manual case 44 and block client-tested/verified promotion.
- Superglass Make remains deliberately unimplemented because final pre-cutoff sources disagree on
  the no-pickup excess-glass distribution: the spell page gives approximately 1.3 ordinary and up
  to 1.6 giant-seaweed glass per sand, while contemporaneous giant-seaweed and molten-glass pages
  publish incompatible exact averages. Its deterministic inputs, rune/XP contract and animation
  are researched, but production must not guess the random-yield algorithm before a cache-233
  client capture or authoritative dated source resolves the conflict.
- Most other production skills still have no dedicated content projects.
- Firemaking now has an integration-tested ordinary-log core for all 12 combustible log objects
  present in the cutoff Wiki table: normal, achey, oak, willow, teak, arctic pine, maple, mahogany,
  yew, blisterwood, magic and redwood. It covers exact boosted levels, XP, membership, all four
  inventory/ground interaction directions, four-tick attempts and retries, the cutoff success
  curve, cache animation 733 and sound 2597, transactional conversion, west/east/south/north
  step-off, blocked movement, same-tile races, random 100–199-tick fire lifetime and ashes. Fire
  placement now checks complete rotated scenery footprints across origin-zone boundaries, every
  generic single/double closed-door swing tile, and the cutoff Wiki's exact Dwarven, Gilbert and
  Jiggig polygons/messages; open doors, ordinary walls and roofs remain legal. The cutoff article
  marks its area list incomplete and supplies no tile geometry for most banks, Duel Arena, Pest
  Control, Blast Furnace, Gauntlet, Sisterhood Sanctuary, Tempoross, Theatre of Blood, Tombs of
  Amascut, TzHaar activities/Mor Ul Rek, Zulrah, God Wars, Killerwatt or Yama, so those named
  prohibitions remain researched rather than guessed. Barbarian bow lighting covers its 11 cutoff
  log levels and XP values, membership, the cache-backed four-state training gate, initial oak
  task, supported bow catalogue, per-bow sequences, spot animation and shared ordinary-fire
  lifecycle. Otto's dialogue transition from first fire to full unlock remains owned by the
  unimplemented Barbarian Training miniquest. All five loose firelighter/coloured-log routes and
  the chargeable gnomish firelighter's one-colour, 1,000-charge, Check, Uncharge, normal-log-only
  and one-charge-per-fire lifecycle are integration-tested. The loose coating text is sourced
  exactly, but the holder's load/check/uncharge/full/runout feedback and load-quantity presentation
  still need cache-233.4 production capture; current functional messages and immediate stack load
  must not be called verified. Pyre-log preparation now covers all 11 cutoff recipes, exact oil
  doses and vial remainders, level-1 access, membership, the quest's state-65 normal-log exception
  and state-85 completed-quest training gate, native Make-All, immediate manual conversion,
  four-tick automatic batching, flat 5 XP per dose, Pyromancer bonuses, full-inventory replacement
  and interruption. Its exact feedback, audiovisual and packet order, and the production client's
  choice of the next oil vial when several qualify still require a matching cache-233.4 capture.
  Shades of Mort'ton funeral-pyre cremation now covers all ten log tiers, all six remains families,
  compatibility and level gates, cache stage locations/operations, the quest-step normal-log/Loar
  exception, exact Firemaking and Prayer XP, independent Morytania diary boosts, the exclusion of
  Pyromancer bonuses, the 21% coin branch, every weighted key branch, private five-minute rewards
  on stone stands, and stale-stage/race handling. Exact unlit-stage lifetime, spirit and audiovisual
  packet order, ambiguous reward-stand choice, feedback wording and the quest plugin's state
  transitions still need cache-233.4 production capture or quest integration. Barbarian pyre
  ships now cover all 11 ordinary-log tiers, exact boosted Crafting/Firemaking requirements and
  XP, the cutoff axe and lighting-tool catalogues, chewed/mangled precedence, staged atomic
  construction, training and Kandarin elite progress, the complete chewed-bones reward table,
  Prayer-charge cap and peaceful/ferocious spirit lifecycle. The exact six production site
  transforms, peaceful-spirit lifetime, cosmetic-axe carving sequence mapping, messages and
  audiovisual/packet order remain observational. The Ferocious spirit now has its exact
  Jagex-supplied 128-weight main drop table, twice-rolled standard herb branch and standard
  gem/mega-rare branch, including Ring of Wealth and Legends' Quest behavior. Owner-only attack
  enforcement, successful-kill training completion, and the elite-clue challenge's guaranteed
  elite clue/casket pair remain open until reusable combat ownership and clue-challenge state are
  available. The handheld-light
  lifecycle now covers every cutoff torch, candle, lantern and mining-helmet pair, exact boosted
  levels, tinderbox and level-30 dry-stick Rub-together ignition, cache-native inventory
  Extinguish operations, worn
  mining-helmet extinguishing and all four candle-lantern assembly routes without XP. It does not
  yet implement dark-area brightness and insect damage, only-light-source extinguish refusal,
  heat-source torch lighting, gas explosions, monster/trap/water extinguishing, the Lumbridge hard
  diary hook or exact non-sapphire feedback and audiovisual ordering. Lamp-oil processing now
  consumes one swamp tar to load either cache still, fills all five cutoff empty lamp/lantern
  containers, changes the Rimmington scenery between its cache empty/full forms and tracks the
  single-form Dorgesh-Kaan still's loaded state. Exact feedback, animation and packet cadence,
  persistence/instance behavior and Rimmington's separate imp-repellent/butterfly-jar route remain
  open. Other special-log consumers,
  Phoenix recolouring, permanent fires, Forestry campfires, Wintertodt, balloons, quest/minigame
  fires, broader diary integration and matching-client packet ordering remain open.
- Herblore now has an integration-tested ordinary cleaning core for all 15 cache-233 grimy herbs,
  including exact IDs, boosted levels, XP, membership, the Druidic Ritual completion gate,
  selected-slot replacement and two-tick same-type automatic continuation. Exact success/failure
  presentation, the observed maximum of ten manual cleaning requests per tick, mixed-herb
  continuation and the complete interruption matrix still need matching-client evidence. Zahur
  cleaning/unfinished-potion/decanting services and Degrime remain open. Ordinary unfinished
  potion mixing now covers all 16 standard recipes—the 15 water-based routes and level-80
  cadantine with a vial of blood—their native Make-All flow, exact one-tick cadence, zero XP and
  atomic gates. Its exact message/audio/animation repetition and inventory/packet ordering remain
  capture work. Standard finished-potion mixing now covers all 31 unfinished-plus-secondary
  recipes with exact three-dose outputs, levels, XP, two-tick cadence and atomic gates. Its
  production modifiers now cover the 30 eligible four-dose outputs, 5% chemistry-amulet and 15%
  alchemist's-amulet success-only charge consumption, persistent chemistry progress, alchemist
  depletion to the uncharged item, both prescription-goggle forms and independent 10% secondary
  preservation without extra XP. The alchemist's-amulet lifecycle now supports inventory Charge,
  Check and Uncharge, both item-use orders, the 5,000-charge cap, noted chemistry-amulet refunds,
  stale-dialogue guards and atomic failure; both prescription-goggle appearances also support
  cache-native Change. Chemistry-amulet Break now destroys the selected item and resets its shared
  five-charge state, while successful production sends the exact remaining-charge or crumble text.
  Chemistry Options and exact Break presentation, the alchemist's initial 50-charge acquisition and
  bank-side operations, the 4,991–4,999 near-cap boundary, exact full-inventory failure text,
  lifecycle audio, exact modifier roll and packet order, exact menu/audiovisual
  ordering remain open presentation/lifecycle work. Fixed one-to-one pestle processing now covers
  unicorn horn, chocolate bar, desert goat horn, kebbit teeth, blue dragon scale, empty bird nest,
  superior dragon bones and nihil shard with zero level/XP, an immediate first result and
  sourced 2-then-3-tick continuation. Exact crushing feedback/audio/packet ordering remains
  observational. Crystal shards now use a separate count input and immediately convert the entered
  quantity into ten dust per shard, gated by the cache Song of the Elves state; its exact prompt,
  quest-failure feedback and packet presentation remain observational. Lava scales now use three
  exact visible-Herblore-level success rolls for a 3–6 shard yield and the Hard Wilderness Diary's
  floored 50% multiplier, with the same sourced immediate, two-tick, then three-tick cadence as
  ordinary pestle processing. Exact lava-scale feedback, animation restart and packet/RNG ordering
  remain observational. Chocolate bars can now also be cut with a knife in free-to-play using both
  item orders, the sourced automatic cadence and exact starting-tool continuity; exact knife-route
  animation, feedback and packet order remain observational. Gorak claws now convert to powder as a
  separate possession-based members recipe, immediately and then every three ticks, without a
  Druidic Ritual, Herblore-level or XP rule. Exact Gorak feedback, animation and packet order remain
  observational. Thirteen additional fixed 0/2/3 pestle routes now cover guam, charcoal, all three
  karambwan states, mud rune, garlic, bat bones, ashes, rune shards, anchovies, dried thistle and
  seaweed, including safe stack-to-stack and stack-to-non-stack capacity handling. Their exact
  presentation and quest-item acquisition remain external or observational. Suqah teeth now use
  their distinct sourced delayed 1/2/3 cadence and possession-based Lunar Diplomacy contract; exact
  messages, audio, animation restart and packet order remain observational. Dramen branches, Tchiki
  monkey nuts and mysterious meat now have separate one-shot possession-based transforms; exact
  feedback, hidden quest-state behavior and the absence of injected multi-item automatic processing
  remain matching-client observations. Raw karambwanji now converts immediately and automatically
  every three ticks with safe stack-to-non-stack capacity; cooked karambwanji has its separate
  immediate paste route. Cooked automatic behavior, exact feedback and presentation remain
  observational. Black dye now consumes a mushroom and empty vial after The Golem is started;
  exact quest-failure/vial-failure feedback, output-slot order and presentation remain observational.
  All three raw snail sizes now consume sample bottles for blamish slime with the sourced immediate,
  then 1/2/3 automatic cadence and no invented Heroes' Quest gate. Exact success/failure text,
  cross-size continuation, bottle selection, audio and packet presentation remain observational.
  Blamish oil now uses the ordinary finished-potion flow with its exact boosted level 25, 80 XP,
  two-tick cadence and no Heroes' Quest gate; exact text and equipment-modifier presentation remain
  observational. Blamish oil now also converts ordinary and pearl fishing rods with their distinct
  zero/one-tick timing and no quest gate; exact feedback, packet presentation and cleaning-cloth
  reversal remain observational or open.
  Swamp-tar production now covers every cache-233 route: clean and ground Guam, marrentill,
  tarromin and harralander each consume one herb plus 15 swamp tar and produce 15 matching tar
  after exactly three ticks, with exact boosted levels, XP, pestle/access gates, single-action
  direct start, native multi-action selection and atomic batching. The later Irit-tar recipe is
  post-cutoff Sailing content and is deliberately excluded. Exact success/failure wording, audio,
  animation restart and inventory/XP packet order remain matching-client observations.
  Coconut preparation now covers hammer-to-half replacement and half-plus-vial filling into coconut
  milk and a retained shell, including the sourced immediate/three/four cadence, both held orders,
  full inventories and shared cancellation. The historical sources do not establish whether
  cracking automatically continues through further coconuts, nor its exact message, animation,
  sound or packet order; only the selected coconut is cracked until matching-client capture.
  Strong weapon-poison production now covers both cache-233 chains: cactus spine produces Weapon
  poison(+) (unf), while either non-stackable Cave nightshade or stackable Nightshade produces the
  common Weapon poison(++) (unf); red spiders' eggs and poison ivy berries finish them. Both stages
  use two ticks, the unfinished stage gives zero XP, the finished stage gives 190 XP, and boosted
  levels 73/82 apply. Prescription goggles can save both nightshade identities, poison ivy berries
  and the ordinary final secondary roll, while neither extra-dose amulet applies. Exact success and
  failure text, sound, inventory/XP packet order, animation restart and one-input menu behavior
  remain matching-client work.
  Antidote production now covers both cache-233 coconut-milk chains. Toadflax produces Antidote+
  (unf) after two ticks at boosted level 68, while irit produces Antidote++ (unf) after one tick at
  level 79; yew and magic roots finish them after two ticks into four-dose outputs for 155 and
  177.5 XP. Prescription goggles can preserve both unfinished-stage herbs and both final roots,
  while neither extra-dose amulet applies. Exact success/failure text, sound, output slots,
  animation restart, one-input menu behavior and interface/inventory/XP packet order remain
  matching-client work.
  Anti-venom production now covers every cache-233 dose route. Antidote++ uses five Zulrah's scales
  per dose at boosted level 87, Anti-venom(4) accepts either clean torstol or torstol potion (unf)
  at level 94, and Anti-venom+ uses one Araxyte venom sack per dose at level 94. Exact two-/three-
  tick timing, dose-scaled XP, atomic quantity limits and the full equipment composition are
  automated: goggles preserve the complete ingredient bundle, eligible sub-four-dose inputs can
  gain one output dose without extra ingredients or XP, and four-dose inputs never consume an
  amulet charge. Exact presentation, Horseshoe Mine Karamja-elite progression and the Sherlock
  master-clue step remain researched matching-client/world integrations rather than silently
  claimed behavior.
  Stamina-potion production now covers all four cache-233 dose routes. Each Super energy dose uses
  one amylase crystal at boosted level 77, completes after two ticks and awards 25.5 XP per dose.
  Native quantity limits, both held-use orders, atomic batches, full inventories, stale resources,
  shared Herblore cancellation and preservation of a pending movement route when production starts
  are automated. The shared equipment path preserves the complete amylase bundle on a successful
  goggles roll, permits a single bonus dose only from one-/two-/three-dose inputs without extra XP
  or crystals, and leaves amulet charges untouched for four-dose inputs. Exact messages, sounds,
  animation restarts, result-slot and packet order, plus end-to-end moving client interaction and
  equipment audiovisual ordering remain researched matching-client case 67.
  Antifire-upgrade production now covers every cache-233 route: all four Extended antifire doses,
  Antifire(4) plus crushed superior dragon bones into Super antifire(4), all four Extended super
  antifire lava-shard routes, and Extended antifire(4) plus crushed bones into Extended super
  antifire(4). Exact boosted levels 84/92/98, two-tick timing, XP, whole dose-scaled secondary
  bundles, native quantity flow, both orders, atomic safety, equipment behavior and moving-start
  preservation are automated. Superior routes require cache varbit 6140, while ordinary Extended
  antifire does not. Primula's exact dialogue and unlock mutation, locked-state wording, ordinary
  presentation, full packet order and end-to-end moving client behavior remain researched in case
  68 rather than being silently claimed.
  Barbarian-mix production now covers all 29 cutoff recipes with exact potion/mix identities,
  boosted levels, XP, roe/caviar rules, one-tick native Make-All processing, both item orders,
  training-state progression, sourced success/training text channels and atomic interruption.
  Direct consumption now covers both doses of all 29 mixes, every three- or six-Hitpoints heal,
  inherited potion effect, the shared food-style delay and conditional attack-delay extension,
  selected-slot vial transitions and automatic smashing. Relicym's mix uses its dated item-page
  disease cure; the cache-matched RuneLite model independently pins Zamorak mix's unusual combined
  Hitpoints result. Exact lumpy-message punctuation, sound/packet ordering, production-equipment
  modifier applicability, and the rest of Otto's Herblore-training dialogue and journal lifecycle
  remain matching-client work.
  The three Pirate Pete ingredients still need their exact subquest-state gate; other
  special/multi-input pestle recipes, Wesley services, special/quest potions, remaining potion
  effects and Mastering Mixology remain open Herblore families.
- Prayer now has an integration-tested ordinary-remains core for all 35 non-quest bone objects and
  all five demonic ashes exposed by cache 233. POH chapel offerings cover all seven altar tiers and
  all 28 god/icon variants, already-lit incense burners, manual/automatic timing and Zealot
  preservation; the Deep Wilderness Chaos altar covers its location guard, 350% XP and both save
  rolls. The 63 ordinary cache-233 Pray/Pray-at locations now restore to base Prayer, including
  both interactive Entrana quest-state altars and the repaired Tribal Statue, while the
  Edgeville Monastery and Nature Grotto locations restore to base +2; all use sequence 645 and
  reset accumulated Prayer-drain progress. All five God Wars boss-room altars now enforce their
  shared persistent ten-minute cooldown and combat gate, add one Prayer point per aligned worn
  item, reconstruct cache varbit 4099 on reconnect, and conditionally restore Hitpoints, run and
  special energy at the Zaros altar. The Forthos Cursed Altar now drains all remaining Prayer and
  deals matching damage bounded to 5–30, including lethal damage; the Darkmeyer statue restores
  Prayer and charges a random 1–3 Hitpoints. The Astral Altar and Ancient Pyramid altar now enforce
  their cache-backed quest-completion states, toggle from any other book to Lunar/Ancient and back
  to Standard from their own book, use exact success messages, preserve Prayer at Astral and drain
  all Prayer at the pyramid even when switching from zero. Both outbound and return switches
  preserve the active autocast selection, defensive mode and the saved staff and bladed-staff
  selections. Their pre-completion feedback, Ancient Pyramid Desert hard-task credit and exact
  audiovisual/packet ordering remain open. All three Mage Arena statues now require the existing
  Mage Arena I completion varp
  and create the matching god cape privately at the player's feet without restoring Prayer;
  repeated claims work. Exact statue presentation and ground-item timing remain capture work. The
  Yanille trapped Chaos altar now deals a fixed, lethal 15 typeless damage, drops the player into
  the poison-spider chamber at 2578,9578 and preserves Prayer state. Exact trapdoor animation,
  movement, hitmark and packet ordering remain matching-client capture work. The broken sun altar
  now preserves Prayer and fractional drain state and sends `You pray at the altar, nothing
  interesting happens.`; it uses ordinary prayer sequence 645 before the Knight of Varlamore is
  present and sun-prayer sequence 8321 once cache varbit 8409 reveals him. Cache location 7814 is
  deliberately unhandled: it was the multi-spellbook altar on temporary unrestricted worlds and
  is not production cache-233 gameplay. The three clickable pieces of the composite Land of the
  Goblins temple altar now send `The Big High War God does not answer your prayer` without
  changing Prayer through quest state 35, then restore Prayer normally from state 36 through
  completion state 56. The state-36 boundary is inferred from the cutoff Quest Helper stage map
  and the Wiki's description of using the altar between the high-priest encounters; exact
  animation timing, sound and packet ordering still need matching-client capture. All five God
  Wars boss-room Teleport operations now exit immediately even during combat and preserve the
  restoration cooldown and player resources. Exact production audiovisual and packet ordering
  remain matching-client capture work. The
  Elidinis Statuette now enforces Spirits of
  the Elid completion state 60, fully restores Prayer, fills run/special energy and clears
  poison/venom in every supported world mode. On a standard world it heals then boosts Hitpoints
  by seven while preserving stronger overheals; on a PvP world it leaves Hitpoints unchanged; on
  Deadman it heals only to the base level. These rules use the selected world's existing database
  flags through the realm configuration. Restoring at least one Prayer
  point there awards the Desert medium diary task. POH chapel furniture and timed incense,
  Bonecrusher, ash sanctifier, Catacombs burial restoration, Ectofuntus, and the two Arceuus
  Offering spells have server-side gameplay. Chewed-bone pyre charges apply to direct burials
  and Bonecrusher conversions, including partial batches, and exclude big bones, ashes and
  altar offerings. The 15 quest/minigame Bury or Scatter objects still depend on their owning
  content. Exact ash-scattering animation, sound, event ordering and the long/curved-bone
  warning copy and disable-checkbox behaviour still need matching-client verification.
- Only a small set of areas, NPCs, locations, interfaces, special attacks, weapons and travel routes
  are represented by content projects.
- Broad quest, diary, clue, minigame, raid, boss, drop-table, Grand Exchange, pet and account-mode
  content inventories do not yet exist.

## Confirmed code gaps that affect parity

- Player death does not yet drop death inventories.
- NPC death now processes registered production tables through an integration-tested composable
  engine. It supports guaranteed items, weighted tables with explicit nothing entries, independent
  tertiaries, ordered pre-roll suppression, conditions, repeated and nested rolls, per-result
  placement/ownership/lifetime policy, ordered player feedback and atomic typed NPC-family
  registration. Unregistered NPCs intentionally drop nothing instead of the former fabricated
  bones placeholder. Undead chicken 2993 is complete: bones and raw undead chicken are guaranteed,
  then feathers resolve as 5 at 64/128, 15 at 32/128 or nothing at 32/128. The twelve dated
  production Chicken identities are also complete for drops: guaranteed bones/raw chicken, that
  same feather table, the exact carried-medium-clue key and the independent 1/300 beginner reward
  through shared pre/post-X-Marks ownership and scroll-case policy. Cache Chicken-shaped identities
  10498/10499 remain unregistered because neither the dated production version list nor enriched
  spawn inventory gives them a production role. The eight dated adult Cow/Cow-calf identities are
  now complete for guaranteed cowhide/raw-beef/bones ordering, the independent 1/128 beginner clue,
  exact 45-tick (27-second) respawn configuration, bucket/fallback item responses and the Zanaris cow's
  standard and Nuff-certificate transcripts. Exact ambient cadence and audiovisual packet order,
  ground-pile presentation, poison/venom immunity enforcement and complete combat AI remain open.
  Dairy cow 8689 now has a separately bounded Cowbell-theft core, detailed below. The remaining production
  NPC inventory is mostly open. Male and female Ducks 1838/1839 and Duckling 2001 are separately
  registered with an explicit no-drop table, distinguishing verified empty identities from the
  unimplemented fallback. Loaded level-five Frog 8702 is complete for its exact combat stats,
  50-tick respawn, 40% Earth weakness, guaranteed bones and independent 1/90 beginner clue. Big
  frog 8701 is now complete for its exact combat stats, 50-tick respawn, 25% Earth weakness,
  guaranteed bones, independent 1/70 beginner clue and sixth Rag and Bone Man I Big frog leg
  lifecycle. Loaded level-thirteen Giant frog 8700 is complete for its exact combat stats, 50-tick
  respawn, 25% Earth weakness, guaranteed big bones, independent 1/64 beginner clue and the shared
  members-only 1/400 long-bone then conditional 1/5,000 curved-bone tertiary. Members level-24 Big
  frog 478 and level-99 Giant frog 477 remain outside the loaded-world contracts and retain their
  separate weighted tables.
  The reusable standard herb subtable is now complete at its exact 128-weight eleven-herb
  distribution and cache object identities. The cache-233 Easy-clue initial-step pool is also
  complete for all 165 active cutoff decoder objects, with exact identity drift detection and
  lazy scroll-box replacement. Loaded Lumbridge Mugger 513 is now complete for guaranteed bones,
  its exact members/free-world 128-share table and independent 1/80 beginner plus members-only
  1/128 Easy clues. Other Mugger identities 1461, 6996 and 13282 remain unregistered until their
  Port Sarim jail, plain Piscarilius/Vinery and full-table location/version routing is independently
  closed. Easy-clue reading, step completion, caskets and rewards remain part of the broader clue
  backlog; the selector only makes an awarded initial scroll exact.
  All five dated Ram identities 1261–1265 are complete for guaranteed bones, 45-tick respawn, 20%
  Fire weakness and the exact shears refusal. Their members-only ram-skull reward is restricted to
  Rag and Bone Man I collection state two and is suppressed when the raw, vinegar or polished form
  is in inventory/bank or its persistent hand-in bit is set. The wider Rag and Bone Man I dialogue,
  vinegar processing, hand-in and completion flow remains open; the shared policy exposes the
  atomic hand-in operation for that later quest plugin instead of pretending that quest execution
  already exists.
  Loaded ordinary Unicorn 2837 is complete for its cache stats and size, exact 90-tick respawn,
  guaranteed bones, members-only horn, outstanding second Rag and Bone Man I bone lifecycle and
  members-only 1/35 initial ensouled-head roll. Fresh-head origin state now survives ground spawn,
  private stacking/splitting, pickup and player re-drop; the documented 31-by-31 reanimation area
  is enforced for ground and carried initial heads, and bank deposits atomically expire them into
  the processed identity. Trade, Grand Exchange and price-checker systems do not exist here yet,
  so those additional expiration routes remain open. The Wiki only says the Unicorn retreats at
  low Hitpoints and cache parameter 26 is zero; its exact retreat threshold therefore remains an
  evidence gap rather than a guessed production rule.
  Loaded ordinary Imp 5007 is complete for its cache stats, corrected 50-tick respawn, 10% Water
  weakness, guaranteed fiendish ashes, exact 128-share main table, members-only 1/25 initial
  ensouled head and members-only 1/5,000 champion-scroll roll. The reusable scroll policy requires
  32 quest points, permits repeat scrolls until the matching champion is defeated, and replaces a
  successful post-victory roll with the exact dark-red would-have-received message. Its documented
  30–120-second, 25% short-distance teleport remains open because the source does not pin the
  radius, destination validation or smoke audiovisual order. Magic-box trapping remains with the
  Hunter backlog. None of the three loaded 5007 placements is in the Wilderness or God Wars
  Dungeon, so the Wilderness-only looting bag and GWD ecumenical-key rolls are deliberately absent;
  GWD Imp 3134 is not loaded.
  The six loaded regular Giant rat identities 2856, 2859, 2860, 2861, 2863 and 2864 are complete
  across their 27 Lumbridge-region placements for exact level-three/level-six stats, size two,
  30-tick respawn, standard melee aggression, guaranteed bones and raw rat meat, independent 1/128
  beginner clue and the outstanding first Rag and Bone Man I bone lifecycle. The cache supplies a
  hunt range of five and production uses the existing line-of-sight, chase and standard
  non-Wilderness two-times-combat-level cutoff; exact matching-client acquisition and disengagement
  edges remain in manual case 123. Regular identities 2857, 2858, 2862 and 9483 are not loaded.
  Stronghold identities 2510–2512 have a separate Witch's Potion rat-tail table and are also not
  loaded. Every current placement is outside the Wilderness, so the members-only 1/15 looting-bag
  roll is deliberately excluded until a location-aware Wilderness placement exists.
  Loaded regular Rat 2854 is complete across its sixteen level-zero Lumbridge-region placements
  for exact stats, size, non-aggression, two-tick respawn, deliberately empty ordinary table,
  repeatable guaranteed Rat's tail during Witch's Potion states one and two, and the independent
  members-only 1/4 seventh Rag and Bone Man II bone during collection state four. Raw 7836,
  vinegar 7837 and polished 7838 ownership in inventory or bank suppress replacement, as does the
  persistent hand-in bit six stored in existing cache varp 715; the other mask bits are preserved.
  Exact varp selection is evidence-bounded: 715 is permanent, has no cache varbits and has no
  direct cache-233 client-script readers/writers, whereas 716 is occupied by pot/boiler state.
  The full Witch's Potion and Rag and Bone Man II dialogue, processing, journal, hand-in and reward
  lifecycles remain open. Regular 2855 and Stronghold 2492/2513 are not loaded, and none of the
  current 2854 placements is in the Wilderness, so the members-only 1/15 looting bag is excluded.
  Loaded regular Spider 3019 and level-two Giant spider 3017 are complete across seven and sixteen
  level-zero Lumbridge-region placements respectively. Both have their exact cache stats, size,
  four-tick Stab cadence, 50% Fire weakness and 35-/30-tick respawns; the regular Spider remains
  non-aggressive while the Giant spider uses the standard five-tile melee hunt contract. Their
  deliberately empty ordinary tables and independent 1/128 beginner clues use the shared clue
  ownership, X Marks scroll-box, capacity, warning and lifetime policy. None of the 23 placements
  is in the Wilderness, so looting bags and the Wilderness Slayer tertiary are excluded. The
  Underground Pass and Stronghold Spider identities 4561/2478 and level-27/50 Giant spiders
  3018/2477 remain outside this loaded-world contract, as do all Wilderness variants and tables.
  Loaded Goblins 3029–3036 and 3045 are complete across all 128 non-Wilderness placements for
  exact level-two/level-five stats, four-/six-tick Crush/Stab cadence, non-aggression, 35-tick
  respawn and their distinct exact 128-share tables. Their independent outstanding fifth Rag and
  Bone Man I skull, 1/35 or 1/30 initial ensouled head, 1/64 or 1/80 beginner clue, 1/128 Easy clue
  and 1/5,000 Goblin champion-scroll contracts are integrated with membership, quest-point,
  ownership, persistent hand-in and champion-victory boundaries. Other Goblin identities,
  aggressive cave families, Wilderness-only Ring-of-wealth clue rates and post-cutoff Combat
  Achievement modifiers remain outside this loaded-world contract.
  Loaded Man identities 3106–3108/6818 and Woman identities 3111–3113 are complete across all
  sixteen Lumbridge-region placements for exact level-two stats, four-tick Crush cadence,
  non-aggression, 25-tick respawn and the dated 128-share table. Members-only bronze-bolt and
  herb branches, their free-world selected-empty/ten-coin behavior, independent 1/90 beginner and
  members-only 1/128 Easy clues are integrated. The exact carried medium-clue-step object 2837
  yields key 2838 from Men only. Other Man/Woman identities, Wilderness looting bags and other
  location-specific tables remain outside this bounded loaded-world contract.
  Loaded Farmers 3114, 3244 and 11919–11921 are complete across all seven non-Wilderness
  placements for exact level-seven stats, six-tick Stab cadence, attack/Strength bonuses,
  non-aggression and 25-tick respawn. Their distinct exact members/free 128-share tables include
  the shared herb table and a reusable exact 128-share allotment-seed subtable, plus independent
  1/90 beginner and members-only 1/128 Easy clues. Unloaded Farmer identities 3243 and 11918,
  pickpocket rewards/guards and any other regional variants remain separate Thieving/NPC work.
  The enriched-runtime spawn manifest narrows the immediate loaded-world backlog to 49 distinct
  attackable identities across 460 placements, while also proving that the current six-square
  Lumbridge population is far short of the production world. All 49 currently loaded attackable
  identities now have evidence-closed combat/drop contracts; this does not close or estimate the
  production-wide NPC inventory outside those six loaded squares.
  Shared rare/gem tables, remaining clue and key conditions, kill-credit and Ironman eligibility, loot
  visibility nuances, encounter-specific placement and boss/raid reward systems remain open.
- Recoil, retribution and related hit-processing effects are unfinished.
- Ironman hit ownership and loot eligibility are unfinished.
- PvP skull assignment, updated multiway logic and singles-plus behavior are unfinished.
- NPC-to-player operation and approach modes 6–8 no longer call `TODO()`: the engine, client
  packet handler, exact/default event routing, script DSL, direct helpers and NPC hunt redispatch
  now support all eight player-target slots. Slots 6–8 are deliberately rejected for location,
  NPC and ground-item targets rather than assigned invented semantics. Complete production NPC
  behavior, option inventories, combat AI and drops remain open under the broader NPC backlog.
- Slayer-task and vampyre formula modifiers are unfinished.
- The Woodcutting Guild now has its final pre-cutoff exact polygon and +7 invisible boost through
  the packed map-area path. The final pre-cutoff Forestry group boost is also implemented outside
  the guild for eligible static timed trees. Regular members-only bird nests now replace successful
  ordinary log rolls with the sourced cape and rabbit-foot tables, exclusions, red message and
  two-minute adjacent floor lifecycle. All cache-233 dragon, 3rd age, Infernal and Crystal axe
  identities now participate in ordinary harvesting with exact access, relative rates, charge
  consumption/saving, depletion, Infernal burning and Lumber Up registration. Infernal creation,
  both recharge materials, percentage inspection and both ornament styles are integrated, as are
  Crystal shard charging, inspection, Revert and the crystal-axe recipe at the Prifddinas bowl or
  learned Reese/Conwenna. Independent beginner-through-elite clue nests now use the sourced
  per-tree, visible-level and clue-tier formula, membership and same-tier ownership gates,
  completed Combat Achievement reduction, Wilderness imbued-ring doubling and Arthur opt-out.
  X Marks the Spot now switches successful Woodcutting clue rolls from the legacy clue nest to the
  cache-233 scroll box plus an empty nest. Inventory and bank ownership, the initial two-box cap,
  minor/major/mimic scroll-case increases through five, the exact blocked-roll warning and distinct
  two-/three-minute ground lifetimes are integrated. Woodcutting still lacks Twitcher's-glove nest
  selection, Sulliuscep activity integration and its 343,000 clue base, Death's Office clue
  ownership, legacy clue-nest and scroll-box opening interactions, the
  rest of the multi-product crystal-singing catalog and exact dedicated interface, singer Talk-to
  unlock flow/NPC-state selection, automatic bank withdrawal refilling and exact Configure-Charges
  presentation, Forestry events and felling-axe systems;
  exact special-tree exceptions, final-charge messages, Infernal percentage wording/rounding,
  regular-nest tile-bias probability and sound, matching-client success sampling, group-icon
  cadence and broader presentation remain manual cases 111–113.
- Fletching now has an integration-tested 35-recipe standard log-cutting core. It covers every
  normal/oak/willow/maple/yew/magic arrow-shaft, unstrung-bow, stock and shield recipe present in
  the pinned tables, plus normal-log javelin shafts, teak/mahogany stocks and the cache-233 redwood
  hiking staff. Membership, boosted levels, both knife identities, two-log shields, output stacks,
  exact XP, sequence 1248, native quantity menus, atomic resource handling and the 2-tick startup /
  3-tick ordinary repeat / 2-tick Fletching-knife repeat cadence are automated. A knife must be in
  inventory to initiate cutting; the Fletching knife's speed effect works from inventory or the
  left hand. The companion bow-stringing core covers all 12 normal-through-magic shortbows and
  longbows with exact boosted levels, historical split XP, output identities and cache sequences.
  It performs full-inventory-safe atomic transforms every two ticks, handles either item order,
  interrupts on resource loss and shares cancellation with log cutting. Composite-ogre-bow
  stringing adds the exact level-30/45-XP final stage, Zogre learning varbit 500, loose-string and
  spool routes, sequence 4433 and the same atomic two-tick behavior. The bow-string-spool core
  now keeps charges in the cache-backed account varbit across loss/reacquisition, accepts loose and
  noted strings, enforces the six 250/500/2,000/5,000/10,000/100,000 capacity tiers, supports
  Fill/Check and combining, supplies every ordinary bow recipe with exact two-tick charge use, and
  automatically stores Spin Flax output with loose-string overflow when its capacity is reached.
  The standard ammunition core adds headless arrows, all nine ordinary/broad arrows, all ten
  ordinary/broad feathered bolts, all eight darts and all eight bronze-through-dragon javelins. It
  pins exact cache objects, levels, per-item XP and partial final sets; implements two-tick arrows,
  rapid one-set darts/bolts, optional ten-set Make-X, Broader Fletching, the javelin 1/1/2-tick
  cadence without an invented animation, and shared cancellation.
  The poisoned-ammunition core covers every cache-backed combination of eight arrow, eight ordinary
  bolt, nine dart, eight javelin and eight throwing-knife bases with standard, extra-strong and
  super-strong weapon poison: 123 held-item interactions in total. Each members-only zero-tick
  transaction consumes one vial and up to five unpoisoned items, returns an empty vial, creates the
  matching poison-strength stack, awards no XP, supports both held orders and remains atomic on
  capacity failure. Exact success/failure wording, audio, packet ordering and whether production
  refuses rather than consumes partial stacks remain matching-client capture boundaries; the
  implemented up-to-five partial behavior is an explicit inference from the source's per-vial cap.
  Brutal-arrow attachment now covers all seven bronze-through-rune tiers in six-item or partial
  sets. Exact boosted levels, XP, cache items, Zogre unlock varbit 499, hammer/members requirements,
  dedicated sequence 4433, two-tick production, reverse held use, full-inventory atomicity and
  shared cancellation are integration-tested. The preceding flighted-ogre-arrow stage now consumes
  four feathers per ogre shaft, makes up to six after two ticks and awards 0.9 XP per arrow. Final
  ordinary ogre-arrow attachment similarly covers flighted arrows plus wolfbone tips at level 5,
  one XP each, after exactly two ticks. Both stages enforce membership and the cache DB-backed Big
  Chompy state greater than zero, accept either item order, handle partial sets and remain atomic in
  a full inventory. Wolfbone-arrowtip cutting now consumes one bone after two ticks, produces a
  random inclusive 2–6 tips, and awards 2.5 XP per tip in both Crafting and Fletching. It enforces
  boosted level 5 in both skills, membership and the same cache-backed Big Chompy started state,
  works in both held-item orders and from a full inventory, and participates in both skills' shared
  cancellation. The source states that yield is simply random and stat-independent but does not
  publish individual outcome weights, so uniform inclusive selection remains an explicit
  implementation inference pending production sampling. Achey-log shaft cutting is now also
  covered as the documented single-shot action: either supported knife transforms one log into a
  random inclusive 2–6 shafts after three ticks for 1.6 XP each, with exact membership, boosted
  level 5 and Big Chompy started-state gates. The shared single-action Achey-log menu now exposes
  both shafts and unstrung composite bow choices. The bow choice atomically consumes one log and
  one wolf bone after three ticks at boosted level 30 for 45 XP, gated by members and learned-
  production varbit 500, and supports both knife identities/item orders and full inventories.
  Uniform shaft-outcome weights, exact single-action menu presentation and choice ordering,
  sequence 4433, failure/success text, audio and client packet ordering remain explicit production-
  capture boundaries.
  Standard crossbow assembly covers all eight bronze-through-dragon stock/limb and unstrung/string
  stages with exact boosted levels, XP, cache objects and tier-specific animations. Both stages use
  native quantity menus and two-tick atomic batches; limb attachment accepts normal and both
  Imcando hammer forms from inventory or equipment.
  Bolt-tip cutting now covers all eleven Fletching recipes from opal through onyx, including both
  pearl inputs, exact yields/XP, ten cache animations, native quantity selection, five-tick
  batches and the exact dragonstone value-warning transcript. Amethyst bolt tips remain a separate
  level-83 Crafting product-selection action and are not incorrectly included in this family.
  Ballista assembly now covers all six light/heavy stages: frames plus limbs, incomplete weapons
  plus springs, and unstrung weapons plus monkey tails. Exact cache objects, boosted levels 47/72,
  330/660 total XP, membership, atomic full-inventory transforms, both item orders and cancellation
  of existing Fletching batches are integration-tested. Published sources do not pin per-stage
  delay, animation, sound or messages; those presentation and timing details remain explicitly
  unverified rather than being inferred from the cached combat-only ballista sequences.
  Celastrus-bark battlestaff production now covers the native Fletch option and both knife-on-bark
  orders, exact level/XP/items, four-tick ordinary production, the Fletching knife's three-tick
  repeat cadence, sequence 1248, full-inventory replacement, requirements, interruptions and
  shared cancellation. Exact success/failure message wording, audio, packet ordering, spam-click
  behavior and the wider combat/interface/logout/death interruption matrix remain client-test work.
  Toxic-blowpipe creation now covers both chisel-on-Tanzanite-fang orders, the members and boosted
  level-78 gates, exact empty output, 120 XP, cache sequence 3015, synchronous zero-tick atomic
  replacement, full-inventory safety and shared cancellation. The standard blowpipe's packed
  charging lifecycle now covers all nine unpoisoned darts, scales, both 16,383 caps, different-dart
  swaps, inventory/worn Check, Unload, confirmed Uncharge and 20,000-scale Dismantle with atomic
  capacity handling. The blazing variant now covers exact empty-only Trailblazer-kit application,
  creation sound, persistent style-preserving charge operations and reversible Dismantle back to
  both components. Bank `Configure-Charges`, automatic pickup routing,
  death/grave recovery, combat consumption and depletion, bonuses, venom and special attack remain
  separate incomplete systems. Exact creation/lifecycle messages, confirmations, percentage and
  inventory/audio/packet order still require matching-client capture.
  Bank-proximate Empty withdrawal and automatic deposits while spinning flax remain blocked on
  authoritative area/cross-skill hooks. Exact menu presentation, messages/audio/inventory ordering
  and the wider interruption matrix still require matching-client capture. New-account Make-X
  initialization, other special ammunition, the toxic blowpipe's bank/ornament/pickup/death/combat
  integration,
  melee weapon poisoning/cleaning, poison-on-hit combat behavior, special products and Vale Totems
  remain unimplemented. Tipped-bolt
  attachment now covers every standard/special and dragon recipe, but exact matching-client menu,
  message/audio and packet ordering plus wider interruption and task hooks remain open. Crossbow
  diary/task credit and exact matching-client message punctuation, audio and operation ordering
  remain open.
- The three native elemental tomes now accept their own pages, support bank targets, consume
  charges from eligible casts, and retain the last cast's passive. The three Pages menus can
  remove pages and discard the spent fraction of the last page. Trade/death handling and the
  exact matching-client presentation of these operations remain open under manual case 82.
- All sixteen Ancient combat spells now have their cache-233 contracts and shared combat core.
  Remaining work is deliberately split out rather than implied complete: secondary players selected
  by Burst/Barrage still need the production PvP attackability/team/single-combat rule inventory;
  Barbarian Assault spell suppression has no authoritative area/activity hook; freeze-resistance
  percentages and hard immunities are not yet applied by the shared binding repository; NPC healing
  has no central consumer for the Smoke-sceptre reduction; and the Shadow Sire/Leviathan special
  interactions are not implemented. Shadow Burst/Barrage and Blood Burst/Barrage travel graphics
  reuse their family Rush/Blitz cache identities because no distinct travel types were found, but
  this must be confirmed in the matching client. Exact messages, audiovisual timing, packets and
  multi-target ordering remain manual case 83.
- Ghostly, Skeletal and Undead Grasp plus Mark of Darkness now have an integration-tested core.
  Ghostly is deliberately the pre-A Kingdom Divided exception; the other three spells require the
  cache quest row's completion state 150. Grasp binds only after a successful accuracy roll, uses
  the fixed two-tick impact delay, holds for 2/3/4 ticks, and doubles its 10/25/50-percent chance
  while Mark is active without extending duration. Mark's 300-tick state, 1,500-tick Purging-staff
  snapshot, exact ten-tick cast lock and ten-tick warning are separate server states. Ward of
  Arceuus now has an integration-tested members/quest-gated self-cast: exact runes and XP, a
  cast-time base-Magic duration snapshot, 50-tick cooldown, sequence 8982, graphic 1851, exact
  success line, one-tick incoming Grasp binds and floor-rounded 10% demon damage reduction through
  the central hit path. Lesser and Greater Corruption now have an integration-tested self-cast and
  player-impact core: final 50% proc chance, Mark-guaranteed proc and halved interval, three exact
  escalating Prayer drains, non-stacking, and Ward cancellation/immunity without consuming the
  attacker's armed effect. Exact corruption audio and packet order, target-state edge priorities,
  reconstructed-session/logout/death/world-transition behavior and matching-client display timing
  remain manual case 86. Ward's 33% demon prayer-drain reduction and its multiplicative Spectral
  stacking still need encounter consumers; remaining Grasp/Mark/Ward observations stay in case 84.
- Vile Vigour now has an integration-tested core: exact members/quest/level/rune/XP gates, capped
  one-Prayer-point-to-one-run-percentage-point conversion, only enough Prayer consumed to fill the
  missing energy, partial conversion, cache sequence 8978, graphic 1876 and 17-tick cooldown. The
  cast deliberately does not stop movement, matching the documented ability to use it while
  running. Exact no-Prayer/full-energy/cooldown feedback and priority, sound identity, packet order
  and matching-client movement/action timing remain manual case 87.
- Death Charge now has an integration-tested core: exact members/quest/level/rune/XP gates, a
  100-tick active window and independent cooldown, 16-tile credited-NPC-kill routing, recent-PvP
  suppression, unconditional charge consumption for a full special bar, and the published
  non-credit encounter targets. Reading object 30806 uses the sourced confirmation transcript,
  safely consumes the selected scroll, permanently sets varbit 16269 and upgrades later casts to
  two qualifying kills with red graphics. Generic multi-attacker NPC deaths currently credit the
  source of the lethal hit. Nex is deliberately excluded because live behavior instead awards its
  trigger to the eligible highest-contribution player; that encounter-specific attribution, the
  complete evolving exclusion inventory, exact sound/messages/packet order, logout/death/world-
  transition reconstruction and matching-client timing remain manual case 88.
- Degrime now has an integration-tested core: cache 233.4's level 70, four-earth/two-nature and
  83-XP contract; members, A Kingdom Divided, Druidic Ritual and resource gates; all fifteen
  canonical grimy herbs; boosted-level filtering; same-slot atomic replacement; independently
  floored half Herblore XP; an exact eight-tick interval; cast sequence 8980 and graphic 1885.
  Routed movement is conservatively rejected. The live-client queued-but-never-casts movement
  defect, exact recovery interaction, no-herb wording/priority, audio, preparation/effect staging,
  packet order and action interruption remain manual case 89.
- Shadow Veil now has an integration-tested cast/state core: exact level/runes/XP and cache assets,
  base-Magic-duration snapshot, 50-tick cooldown/refresh, active and cooldown varbits, Fountain
  semantics, and a public 15% first-roll protection operation. The Man/Woman production consumer
  now enforces veil-before-dodgy ordering and item-local necklace charges. Blackjacking exclusion,
  protection and expiry feedback, audio/packet timing and lifecycle reconstruction remain manual
  cases 90 and 101.
- Dark Lure now has an integration-tested target-spell core: exact level/runes/XP and cache
  component/graphic/cooldown assets, protected immobile rejection, travel-timed impact, ordinary
  attack engagement, a ten-cycle impling follow with default-mode restoration and unrelated-NPC
  safety. The shared Arceuus human cast sequence and five-tick cadence are bounded inferences.
  Exact feedback/priority, sound, packet ordering, fleeing-state nuances, live two-by-two impling
  pathing, post-lure catch ownership grace and matching-client timing remain manual case 91.
- Resurrect Crops is source- and cache-pinned at legacy no-quest Magic 78, 25 earth/8 blood/
  12 nature/8 soul, 90 XP and a level-scaled 50–75% chance, with failure clearing the patch and a
  revived crop limited to one resurrection. No production Farming plugin currently owns crop
  growth, death, clearing or persisted per-plant resurrection state, so a truthful gameplay
  implementation is deferred with the complete target/state/client matrix in manual case 92.
- Inferior, Superior and Dark Demonbane now have an integration-tested core. Cache 233.4 supplies
  the post-22-May costs of 3/5/7 fire runes plus 1 chaos/1 soul/2 soul, levels 44/62/82, XP
  27/36/43.5 and maxima 16/23/30. Inferior is the pre-A Kingdom Divided exception; every spell is
  members-only and rejects non-demons atomically. The production formula applies the April 2025
  corrected +20/+40 unmarked accuracy and +40/+80 marked accuracy, post-gear +25%/+50% Mark
  damage, and the nested rounding for a 70%-effective target. Sequence 8977, tier cast/hit graphics
  1865–1870 and the fixed two-tick impact are typed and tested. Exact rejection text and check
  priority are provisional, and exact audio/packet timing, the complete allowed-autocast weapon
  matrix, production-wide demonic/effectiveness enrichment, greater-than-100% targets such as Yama,
  and encounter-specific overrides remain manual case 85.
- NPC-specific stat-drain immunity, drain floors/caps and encounter reset rules are not catalogued
  or implemented. Generic NPC drain and ordinary regeneration alone are insufficient.
- Binding warning audio and its exact client-observed timing are unverified. The exact red warning
  message is automated four cycles before release, but no cache synth is yet evidenced for its cue.
- Ancient Ice now applies the enriched NPC freeze-resistance percentage exactly once after any
  sceptre extension. All five Phantom Muspah forms are configured at 33%, yielding base
  Rush/Burst/Blitz/Barrage holds of 5/10/16/21 ticks, and 100% resistance prevents a bind. The
  remaining production NPC resistance/immunity inventory, any non-Ice consumers, encounter
  overrides and matching-client duration observation are not yet catalogued.
- The first ten fixed self teleports, Teleport to House and Lumbridge Home Teleport consult Tele Block's shared state,
  but every other teleport path still needs enforcement. `inside_wilderness` also has no production
  region updater, so the
  cast gate depends on a varbit that only tests currently arrange.
- Tele Block records its caster but caster-kill cleanup is not connected to death attribution. The
  exact 30-second-warning and natural-expiry messages are not evidenced or emitted. Successful
  no-projectile presentation and bind-derived impact timing remain cache-informed inferences until
  workspace RuneLite capture.
- Crumble Undead uses the cache's production `undead` parameter, but the complete set of tagged NPCs
  and any encounter-specific exclusions or overrides has not been audited. Its exact presentation
  timing also remains unverified in the workspace RuneLite.
- Magic Dart resolves real Slayer tasks and consumes enchanted charges. The exact native sounds,
  projectile timing and level-failure feedback still need a client capture.
- Iban Blast uses the native post-quest stage. Koftik, the Dark Mage and the paladin recharge
  well have server handlers. Underground Pass quest execution and its full dungeon route remain open.
- Mage Arena I uses all five combat forms and persistent staff and cast progression. Mage Arena II
  has personal rotating tracker locations, three god followers and repeat cape imbuing. Exact
  follower specials, dialogue, client timing and state across live world hops need further checks.
- Low and High Level Alchemy have an integration-tested core, but Explorer's ring free casts,
  restricted-activity gates, the Alchemist's Playground timing exception, queued casts, warning
  settings, animation overrides, exact failure text and client-observed sequencing are absent or
  unverified. Their in-memory rate limiter is intentionally not treated as persistent state.
- Bones to Bananas and Bones to Peaches have an integration-tested spell core, but their precise
  unlock failure text, Tombs of Amascut restriction, full-inventory overflow ordering, diary credit,
  tablet variants and client-observed sequencing remain absent or unverified. The Mage Training
  Arena purchase flow that sets the Peaches unlock varbit is not implemented.
- Superheat Item has an integration-tested cache-era core and reusable furnace recipe data, but
  exact Smithing/Cooking/material/quest failure messages, successful/failure client ordering,
  interruption and queue replacement still need matching-client capture. Sequence 687 and failure
  synth 191 are cache-name inferences. Perfect gold ore remains excluded because its pre-cutoff
  creation recipe lists only a furnace; that negative behavior must be observed before verification.
- Telekinetic Grab has an integration-tested cache-era core and a reusable interface-on-ground-item
  interaction path. Fixed projectile timings 35/45 and the two-tick impact are historical-emulator
  inferences, not matching-client evidence. Exact blocked/full/taken messages, movement and combat
  interruption, queued-target replacement, free-world member-item handling, Mage Training Arena
  object/NPC overrides, Tower of Life interference and farming-patch busy behavior remain absent or
  unverified. The later Sailing interaction is deliberately outside cache 233.4.
- Lvl-1 through Lvl-7 Enchant have an integration-tested 44-recipe core, but Mage Training Arena
  shapes and activity state are absent. The exact invalid/member/nature messages, complete
  interface/logout/inventory-change interruption behavior and audiovisual ordering need matching-
  client capture. Cache 233 exposes no distinct level-7 graphic or zenyte synth names, so level 7
  currently reuses level 6's graphic and onyx sounds; semi-precious targets reuse the paired core-
  gem sounds and graphic height 100 is inferred. Cache varbit `vm_digsite_talisman` is the named
  discovery-state candidate and is tested as a boolean gate, but its exact packed semantics are not
  yet client-proven.
- Enchant Crossbow Bolt has an integration-tested cache-era core for all ten regular and dragon
  bolt families. The two official Wiki screenshots support separate regular and dragon product-icon
  menus, but behavior when both families are held is not stated; the server deterministically gives
  the regular family priority. The interface maximum is derived from the largest combined
  regular-plus-dragon count for one gem, rounded into ten-bolt sets. That mixed-inventory priority,
  the exact maximum rule, failure text,
  full-inventory ordering and audiovisual timing still require matching-client capture.
- The four elemental orb spells have an integration-tested cache-era core, but their Water/Earth/Air
  Achievement Diary credit is not connected. Exact wrong-obelisk, membership, missing-orb and
  resource messages, client audiovisual ordering, cross-utility queue replacement and the complete
  interface/teleport/logout/death/inventory-mutation interruption matrix remain absent or unverified.
- Monster Inspect has an integration-tested cache-era core, including its target path and
  five-section client payload. Exact client wording, capitalization, row omission, morph handling
  and audiovisual/interface ordering remain unobserved. Burn and vampyric tiers, spectral status
  and exact Slayer eligibility are not represented by the current enriched NPC type; production
  freeze resistance is only configured for the five Phantom Muspah forms. A complete NPC
  value/classification inventory and a freshly packed-cache runtime/client check are still required.
- Varrock, Lumbridge, Falador, Camelot, Kourend Castle, Ardougne, Civitas illa Fortis, Watchtower,
  Trollheim and Ape Atoll Teleport have an integration-tested core. The Varrock Grand Exchange,
  Camelot Seers' Village and Watchtower Yanille destination choices now honor their exact cache
  diary-completion and saved-toggle varbits. Their configuration-menu flows, landing-area
  randomisation, exact rejection text/check ordering, interruption and movement locking, and
  matching-client audiovisual timing remain absent or unverified. The four-cycle move is based on
  the historical Wiki cast duration and still needs client confirmation. Lumbridge Home Teleport
  and the Edgeville, Lunar Isle and Arceuus variants now share a default 22-tick interruptible
  channel and persistent 30-minute cooldown. The Speedy and other animation overrides, exact
  cancellation presentation, landing-area randomisation and Lumbridge's H.A.M./Draynor-area
  exceptions are absent or unverified. The three Teleother spells now have an integration-tested
  core: Accept Aid, cache-native confirmation, exact spell contracts, ten-tick cast rate, cache
  presentation, Wiki landing regions and accepted-target teleport restrictions. Exact official
  failure text and resource-charge ordering, every interruption edge, Ironman/temporary Accept Aid
  overrides and matching-client observation remain unverified. Teleport to Target now has an
  integration-tested spell and scroll-unlock core, but production Bounty Hunter matchmaking,
  target lifecycle, lobby/crater state transitions, warning behavior, exact inferred messages and
  matching-client observation remain absent or unverified. Teleport to House now has an
  integration-tested outside/core path for all nine cache-233 house locations and a prepared-entry
  contract for regular/build-mode house instances. The production Construction/POH plugin does not
  yet create dynamic house zones or register their entrance tiles, so an inside cast rejects before
  consuming resources unless that contract has been populated. Group Ironman choose/previous
  operations also reject atomically until the grouping and house-ownership systems exist. Exact
  landing tiles, messages, operation switching, interruption and audiovisual timing still require
  matching-client capture.
- Ordinary furnace smelting now consumes the shared eleven-bar registry through all exact cache-233
  furnace loc operations. Remaining furnace families include elemental and perfect-gold quest
  products and the Blast Furnace. Production diary/quest credit, exact
  non-success messages, success sounds, interface-close ordering and matching-client timing remain
  unverified; the ring Break message and persistence lifecycle also need client/database capture.
- The Smithing Skillcape gold-XP perk is integration-tested for both cape variants and the worn max
  cape through ordinary furnaces and Superheat Item. The separate coal-bag core now implements both
  cache variants, every inventory operation, held-use Fill, bank Fill/Empty, empty-only confirmed
  Destroy, permanent 27/36 capacity, non-truncating cape removal and automatic open-bag coal Mining
  even with a full inventory. Both alchemy spells also implement the Wiki-specific destruction and
  stored-state clearing exception for the cache-marked bags. Deposit-box rejection, acquisition and
  one-bag ownership, equipped-bank/placeholder presentation, exact success/check/alchemy warning
  feedback and matching-client packet order remain unverified. Blast Furnace
  consumption/collection is absent; all boundaries remain manual case 110.
- Steel cannonball Smithing now has an integration-tested regular/double-mould core. Morytania
  medium diary credit, exact failure and menu ordering, the Shilo fee interpretation for a double
  mould action, and matching-client audiovisual/inventory timing remain unverified.
- Ordinary gold-jewellery Crafting now has an integration-tested 32-recipe core using cache interface
  446. Slayer and eternal Slayer rings, last-selection varbit encoding, exact quantity payload and
  alternate use-on entry points, exact messages, diary coupling, Shilo presentation and
  matching-client audiovisual/inventory timing remain unverified. Silver and special quest-mould
  jewellery are implemented as a separate production family.
- Silver-jewellery Crafting now has an integration-tested 20-recipe core using cache interface 6,
  including the conductor, three-material Silvthrill rod and demonic sigil. Cache clientscript 1596
  and the pinned Quest Helper source establish mould possession rather than a hidden quest-varbit
  production gate. Quest-side mould acquisition/replacement presentation, last-selection varbit
  13893, exact quantity payload/use-on variants/messages, diary coupling, Shilo presentation and
  matching-client audiovisual ordering remain unverified.
- Gem cutting now has an integration-tested ten-recipe core, including the exact three
  semiprecious success curves and failure XP, guaranteed precious-gem cuts, the 2024-rebalanced
  50-XP zenyte value, all ten cache-named animations and two-tick batching. Exact sound identity,
  success/failure message spelling and ordering, boosted-level behavior, diary credit and the full
  movement/combat/interface/logout/death interruption matrix still need matching-client capture.
- Amethyst cutting now has an integration-tested four-product Crafting core. Chisel-on-amethyst
  opens one native quantity menu for bolt tips, arrowtips, javelin tips and dart tips; the boosted
  level 83/85/87/89 gates, 15/15/5/8 yields, 60 XP per source, members gate, cache sequence 6295,
  two-tick atomic batching, full-inventory replacement and shared Crafting cancellation are covered.
  Exact menu memory, success/failure text, sound, packet ordering, wider interruption behavior and
  matching-client observation remain unverified.
- Manual stringing now has an integration-tested 15-recipe core: all eleven ordinary metal/gem
  amulets, both silver symbols, the M'speak amulet and the Salve amulet. The Salve action correctly
  awards no Crafting XP while every other covered transform awards 4 XP. Exact animation/sound and
  inventory ordering, Salve Make-All presentation, quest-side acquisition coupling and full
  interruption behavior remain unverified.
- Glassblowing now has an integration-tested eight-recipe core using cache skill-multi interface
  270/clientscript 2046, sequence 884 and the exact beer-glass-through-empty-light-orb level/XP
  table. Furnace production of molten glass, assembling empty oil lamps with frames and wiring
  empty light orbs are also integration-tested with exact levels, XP, material transforms and
  documented timing. Exact success/failure messages and sounds, previous-selection behavior,
  quest/diary coupling, and the full movement/combat/interface/logout/death interruption matrix
  remain unverified.
- Spinning now has an integration-tested cache-233 core for wool, flax, sinew, oak/willow/maple/yew
  and magic roots, and yak hair. It covers all five outputs, the dual use of magic roots, native
  quantity selection, exact levels/XP/membership and three-tick batches at eleven cache wheel
  identities. The Seers' Village diary speedup, exact messages/sounds, mixed-material consumption
  priority, diary/task credit and full interruption behavior remain unverified. Linen, hemp and
  cotton spinning are absent from cache 233 and belong to a future-cache delta.
- Pottery now has an integration-tested six-product cache-233 core. Shaping and firing cover the
  exact item/XP table, four cups per soft clay, free/member boundaries, three- and seven-tick
  batches, sequence 883, oven sequence 1317, all usable named wheel/oven identities, the exact
  181/256 through 256/256 firing-success curve and the One Small Favour state-235 pot-lid unlock.
  Exact crack/success text and sound, oven-glow transitions, item-on-oven menu filtering,
  diary/task hooks, boosted-level interpretation and the full combat/interface/logout/death
  interruption matrix still require matching-client capture.
- Ordinary leatherworking now has an integration-tested 20-recipe cache-233 core: all seven
  soft-leather products, hardleather bodies and green/blue/red/black dragonhide vambraces, chaps
  and bodies. It covers exact levels, XP, material counts, free/member boundaries, interface 270,
  sequence 1249, three-tick batches, one spool consumed every fifth armour action and the costume
  needle's unlimited-thread behavior. Exact messages/sounds, durable thread-use state across
  logout, diary/task hooks and the complete interruption/client-ordering matrix remain unverified.
- Specialist leatherworking now has an integration-tested ten-recipe cache-233 core. Ordinary
  snakeskin makes boots/vambraces/bandana/chaps/body at levels 45/47/48/51/53 from
  6/8/5/12/15 skins for 30/35/45/50/55 XP. Cured yak hide makes legs/top at levels 43/46 from
  one/two hides for 32 XP, gated at Fremennik Isles progress state 280. All are members-only,
  expose native per-material quantity menus, use sequence 1249 and complete atomically every three
  ticks. Unlike ordinary leather armour, each result consumes a whole thread; the costume needle
  substitutes for it. Xerician fabric additionally makes hat/robe/top at levels 14/17/22 from
  3/4/5 fabric for 66/88/110 XP with the same tool, membership and three-tick contracts. Movement,
  tool/thread/material loss and another Crafting action stop safely. Exact messages, sound,
  audiovisual/inventory ordering, activity restrictions and diary/League-task hooks remain
  unverified. Further quest-textile families remain unimplemented.
- Hunter pouch Crafting now has an integration-tested five-recipe cache-233 core. Four larupia or
  fox furs make the small fur/meat pouches at level 35 for 20/30 XP; three graahk, kyatt or sunlight
  antelope furs upgrade the required closed pouch into medium/large variants at levels 50/65 for
  40/60 XP. All are members-only, use sequence 1249, complete atomically every three ticks and
  consume one fifth of an ordinary thread spool per action; the costume needle substitutes for the
  needle and thread. Exact production menu/message/audio ordering is inferred pending capture.
  The companion storage core implements Fill, Check, Open/Close, shared permanent family storage,
  exact 14/21/28 tier limits, exact fur/meat allowlists and exclusions, bank-interface Empty,
  destroy clearing and an open-pouch Hunter-acquisition routing hook. Crafting an upgrade preserves
  the shared contents. Wiring that acquisition hook into future Hunter catch plugins, ordinary
  inventory Empty at qualifying bank/Hunter Guild areas, deposit-box routing, exact messages/audio
  and matching-client operation ordering remain open.
- Asyff's Custom Fur Clothing service now has an integration-tested cache-233 core on native
  interface 477 and inventory 482. All 20 polar/common/Feldip/desert camouflage, larupia/graahk/
  kyatt outfit, gloves-of-silence and spotted/spottier-cape recipes use their exact unnoted-fur and
  coin costs. Torso and leg recipes prefer tatty fur before perfect fur, while headwear requires
  perfect fur. The service is members-only, converts immediately and atomically in a full
  inventory, validates the displayed slot/object pair, rejects stale actions and is reachable
  through Asyff's cache-defined Trade and Fur Clothing operations. His dated standard hunting,
  unusual-material and closed-world dialogue is implemented. Biohazard Priest-gown and Eagle's
  Peak bird-costume branches, exact production success/failure text, activity restrictions, sound
  and matching-client packet/inventory ordering remain open.
- Leather add-on Crafting now has an integration-tested seven-recipe cache-233 core. Steel studs
  combine with leather bodies/chaps at levels 41/44 for 40/42 XP, and kebbit claws combine with
  leather plus all four ordinary dragonhide vambraces at level 32 for 6 XP. All recipes require
  members, accept both item orders, use a single three-tick sequence-1249 action, replace output in
  a full inventory atomically, and stop on movement/material loss or another Crafting action. The
  dated Crafting overview's 5.5-XP spiky-vambrace cell conflicts with its own 6-XP prose and every
  item recipe's 6-XP value; recipe-specific evidence is implemented while production observation
  remains required. Exact messages, sound and audiovisual/inventory ordering remain unverified.
- Leather-shield Crafting now has an integration-tested six-recipe cache-233 core. Two pieces of
  hard leather, snakeskin or matching dragon leather combine with the corresponding oak, willow,
  maple, yew, magic or redwood shield and exactly 15 bronze, iron, steel, mithril, adamantite or rune
  nails at boosted Crafting levels 41/56/62/69/76/83 for 70/100/124/140/156/172 XP. Every recipe is
  members-only, accepts both leather/base-shield item orders, uses its distinct cache sequence and
  completes as one five-tick atomic action. A normal hammer or either main-/off-hand Imcando hammer
  works from inventory or equipment, including full-inventory production. Movement, material or
  tool loss and another Crafting action cancel safely. Exact use-on precedence, messages, sound,
  audiovisual/inventory ordering and diary/League-task credit remain unverified.
- Hydra-leather Crafting now has an integration-tested cache-233 machine core. Machinery 33609
  accepts hydra leather 22983 to make ferocious gloves 22981 and reverses the gloves back to
  leather, one-for-one with no XP after seven ticks. It requires members, Dragon Slayer II state
  215 and a normal or either Imcando hammer form. The exact cutoff-era Inspect text, both
  confirmations, both result messages and the missing-hammer message are implemented from the
  dated Machinery transcript; full-inventory replacement, declining, movement/tool/material loss,
  both directions and direct Investigate routing are covered. Sequence 1755/8915 selection is a
  cache-name plus Wiki-animation inference pending matching-client capture. The inferred members,
  quest, missing-input and mid-action-loss text; Investigate priority when both inputs are held;
  Imcando substitution on this specific machine; exact animation/audio/inventory ordering; and
  broader interface/combat/teleport/logout/death interruption remain unverified in production.
- Lunar clothing now has an integration-tested cache-233 quest-Crafting core. During Lunar
  Diplomacy states 155 through 189, one Suqah leather makes a Lunar torso, trousers, gloves or
  boots at visible Crafting levels 61/59/52/52 for 35/30/25/25 XP. The exact five-choice transcript
  menu, four level failures, four success messages and post-quest rejection are implemented. Each
  result takes three ticks, accepts either held-item order and uses ordinary needle/thread or a
  costume needle; ordinary Suqah and standard leatherworking now share one five-action spool
  lifecycle. Full-inventory replacement and mid-action tool/material/quest changes are atomic.
  State 155 is supported by the dated Quest Helper clothing stage and state 190 by cache quest DB
  row 88, but the exact pre-stage failure, members/tool/material failures, sequence/audio ordering,
  persistence across logout and broader interruption behavior still require production capture.
- Tanning now has an integration-tested eight-recipe cache-233 service core on native interface 324.
  Ellis, the Crafting Guild Tanner, Sbott, Eodan and Chouani expose their cache-defined third
  operation, while the Ranging Guild Leatherworker correctly reaches the service only through his
  first-operation dialogue. Standard, premium and Eodan price schedules, all four Kourend diary
  discounts, member-hide gates, affordability caps and immediate full-inventory atomic conversion
  are covered. Exact dialogue for the five direct-trade tanners, sound identity, final message and
  interface ordering, activity restrictions and diary/task hooks remain unverified.
- Elemental battlestaff Crafting now has an integration-tested four-recipe cache-233 core. Water,
  earth, fire and air orbs combine with ordinary battlestaves at levels 54/58/62/66 for
  100/112.5/125/137.5 XP using interface 270, sequence 7531, the four cache elemental spot effects,
  a three-tick Make-X startup and two-tick repeats. Membership, boosted levels, both item orders,
  requested quantities, material exhaustion, movement-click interruption and atomic inventory/XP
  updates are covered. Bone, Bryophyta's, purging and twinflame staff creation, POH crystal-ball
  elemental conversions, exact text/sound/order, Farming Guild diary credit and matching-client
  observation remain absent or unverified.
- Loom weaving now has an integration-tested four-recipe cache-233 core. Four balls of wool, four
  jute fibres, two jute fibres or six willow branches make a strip of cloth, empty sack, drift net
  or basket at the exact levels and XP. The three usable cache loom identities, members gate,
  native four-product quantity chooser, sequence 2270, ordinary 4-tick startup/3-tick repeats,
  drift-net 3-tick startup/repeats, atomic transforms, movement/material stops and cross-Crafting
  replacement are covered. Exact messages and sound, client event ordering, direct-input menu
  filtering, every world placement, broader activity interruption and diary/League-task hooks
  remain absent or unverified.
- Lunar Recharge Dragonstone now has an integration-tested cache-233 inventory core. It charges
  ordinary and trimmed glories at zero through three charges, plus Legends' Quest-gated skills
  necklaces and combat bracelets at zero through three charges, to four in one atomic batch. The
  exact handling of five/six-charge jewellery is not stated by the cutoff sources, so production
  preserves it rather than risk a destructive downgrade. Trimmed-glory equivalence, exact
  empty/Legends feedback, already-four-only resource consumption, noted/equipped behavior, exact
  recast semantics, graphic timing/height and packet ordering still require matching-client
  capture before the slice can advance beyond `integration-tested`.
- Lunar Cure Me now has an integration-tested cache-233 self-cast core. It removes ordinary
  poison without immunity, downgrades every venom stage to ordinary poison with the same next
  damage, applies the official one-tick cap and uses the exact cache presentation. Varp 102 is
  authoritative to the inspected RuneLite client, while the semantics of varp 3777 and its
  `poison_type` varbit 6251 are undocumented; those secondary fields are deliberately preserved.
  The shared varp-102 lifecycle now supplies exact poison/venom scheduling and damage. Clean direct-
  component behavior, success feedback, source-specific infliction/reapplication, secondary-state
  mutation, cross-action cooldowns, graphic slot/delay and packet ordering require matching-client
  capture before the slice can advance beyond `integration-tested`.
- Lunar Cure Other now has an integration-tested cache-233 targeted-support core. It enforces
  members, Lunar Diplomacy, Lunar spellbook, visible Magic 68 and target Accept Aid; charges one
  astral, one law and ten earth runes; awards 65 Magic XP; removes ordinary poison without
  immunity; and downgrades venom to ordinary poison with the same next damage through the shared
  Cure Me formula. Self, clean, immune, non-aiding and cooldown attempts are atomic no-ops. Direct
  archived animation comparison plus cache and legacy asset evidence resolves target graphic 738
  at the recorded height 96, while sequence 4411 and the sole typed cache synth 2884 provide the
  remaining presentation. Exact clean/immune behavior, success feedback, the provisional five-tick
  rate, sound identity, secondary poison state, scheduling, movement/logout races, graphic
  slot/delay and packet ordering remain manual case 47 before promotion beyond
  `integration-tested`.
- Lunar Cure Group now has an integration-tested cache-233 area-support core. It enforces members,
  Lunar Diplomacy, Lunar spellbook and visible Magic 74; charges two each of astral, cosmic and law
  runes; awards 74 Magic XP; and applies the shared poison/venom cure to the caster plus every
  Accept Aid recipient within one square on the same plane. The caster is included even with
  Accept Aid off, ordinary poison clears without immunity, venom preserves its next damage, and
  the cache sequence 4409, group graphic 744 at height 96 and sole typed Lunar cure synth 2884 are
  emitted. Production currently treats a clean cast as successful, shows the group graphic to clean
  eligible recipients and uses a five-tick rate; the Wiki does not resolve those behaviors. Exact
  feedback, clean/immunity presentation, recipient ordering, movement/logout races, cooldown
  sharing, sound identity, secondary poison scheduling, graphic slot/delay and packet order remain
  manual case 48 before promotion beyond `integration-tested`.
- Lunar Dream now has an integration-tested cache-233 self-channel core. It enforces members,
  Lunar Diplomacy, Dream Mentor, Lunar spellbook, visible Magic 79 and damaged Hitpoints; charges
  two astral/one cosmic/five body runes; awards 82 Magic XP; uses cache start/sleep/end sequences
  7627/6296/7628 and Z graphic 1056; and heals exactly three Hitpoints per 100 server ticks on an
  independent 34/33/33 schedule until full health. Chat and active recasts do not stop it, while
  ordinary published actions and observed Hitpoint loss do. The precise first-heal phase and
  33/34 distribution are deterministic integer-tick inferences. The generic action event cannot
  yet exempt armour changes as the Wiki requires. Exact Z cadence and protocol fields, full-health
  feedback, main-regeneration/Rapid Heal/cape/bracelet interaction, simultaneous hit/heal order,
  Nightmare Zone restriction and conflicting message, jar-of-dreams easter egg, login/network
  races and packet ordering remain manual case 49 before promotion beyond `integration-tested`.
- Lunar Heal Other now has an integration-tested cache-233 targeted-support core. It enforces
  members, Lunar Diplomacy, Lunar spellbook, visible Magic 92, Accept Aid, non-self and living,
  damaged-recipient gates; charges three astral, one blood and three law runes; awards 101 Magic
  XP; and transfers `min(floor(caster current Hitpoints * 75 / 100), recipient deficit)` while
  applying the same visible typeless self-hit to the caster. A caster needs at least 11% of base
  Hitpoints, and the obsolete shared 90-second healing cooldown is deliberately absent. Sequence
  4411, recipient graphic 738 at height 96 and synth 2895 provide the cache presentation. The exact
  ordinary cast interval remains a documented five-tick provisional choice. The repository has no
  implemented account-mode model, so the published prohibition on Ironman recipients cannot yet
  be enforced honestly. Exact feedback, target/caster sound routing including synth 2892, PvP and
  singles restrictions, boosted/death boundary behavior, movement/logout/death races, shared-lock
  semantics, graphic/self-hit/headbar timing and packet order remain manual case 50 before
  promotion beyond `integration-tested`.
- Lunar Heal Group now has an integration-tested cache-233 area-support core. It enforces members,
  Lunar Diplomacy, Lunar spellbook, visible Magic 95, the caster's 11%-of-base-health threshold and
  at least one living, injured, Accept-Aid recipient in the exact same-plane 3-by-3 area. It charges
  four astral, three blood and six law runes, awards 124 Magic XP, selects at most five recipients,
  applies the floor-75%-of-current sacrifice plus five pooled healing, and mirrors the exact
  150-tick cooldown through varbit 925. Sequence 4409, recipient graphic 745 and named
  caster/recipient synths 2894/2893 provide the cache presentation; an existing attack delay gains
  three cycles while an idle weapon remains ready. Integer remainder handling, capped-share
  redistribution and whether unused healing reduces caster loss are not resolved by authoritative
  sources, so the current floor/discard/no-redistribution/full-sacrifice policy remains explicit
  inference. Exact feedback, account modes, recipient ordering, authoritative safe/singles/multi/
  singles-plus PvP-area state, reconstructed-session cooldown persistence, movement/logout/death
  races, ordinary/shared locks and packet order remain manual case 53 before promotion beyond
  `integration-tested`.
- Lunar Energy Transfer now has an integration-tested cache-233 targeted-support core. It enforces
  members, Lunar Diplomacy, Lunar spellbook, visible Magic 91, non-self, living-recipient, Accept
  Aid, caster above 10 Hitpoints and exactly-full special-attack energy before charging three
  astral, two law and one nature rune. A successful cast awards 100 Magic XP, drains the caster's
  full special bar, applies a visible exact 10-point typeless self-hit and restores the recipient's
  run and special energy to full. Single-way Wilderness transfers and targets with active recorded
  PvP combat on PvP worlds are rejected. Sequence 4414, recipient graphic 734 at provisional height
  96 and synths 2885/2906 provide the cache-named presentation; the ordinary five-tick lock is also
  provisional. Non-Wilderness PvP-world safe-zone/single-way classification is not modeled, and
  the exact `recently` duration, rejection text, already-full recipient behavior, sound/graphic
  timing, movement/logout/death races, shared locks, combat side effects and packet order remain
  manual case 51 before promotion beyond `integration-tested`.
- Lunar Vengeance Other now has an integration-tested cache-233 support-spell core. It enforces
  members, Lunar Diplomacy, Lunar spellbook, visible Magic 93, non-self, living-recipient, Accept
  Aid, no-POH and no-single-way-Wilderness gates before charging three astral, ten earth and two
  death runes. Success awards 108 Magic XP, arms recipient varbit 2450, starts the caster's exact
  50-tick cooldown through varbit 2451, accepts one blighted Vengeance sack where blighted items
  are enabled, uses sequence 4411, recipient graphic 725 at provisional height 96 and synth 2908,
  and sends the published recipient message.
- Self Vengeance now has an integration-tested cache-233 core. It enforces members, Lunar
  Diplomacy, Lunar spellbook and visible Magic 94 before charging four astral, ten earth and two
  death runes, awards 112 Magic XP outside the Fountain, arms the caster and shares the same exact
  50-tick cooldown. It accepts the LMS fake rune pouch and blighted Vengeance sack through the
  shared rune manager, remains legal in single-way Wilderness, uses no-stalling sequence 8316
  outside PvP and stalling sequence 8317 when client PvP varbit 8121 or Wilderness varbit 5963 is
  active, and emits self graphic 726 at provisional height 96 with synth 2907. The repository does
  not yet have an authoritative producer for client PvP-area varbit 8121, so non-Wilderness PvP
  zones must remain a cross-system gap rather than being inferred from world type.
- Cache-233's Vengeance `Animation` operation now opens native animation picker 525 through
  clientscript 7707 and DB row 4094, normalizes varp 4562 to unlocked choices, prevents disabling
  the last choice and mirrors the selected index to varbit 17316. The unlocked Trailblazer IV
  choice uses sequence 10626, graphic 2605 and synth 7569. Members can now `Read` Vengeance scroll
  28696: exactly one item is consumed, shared reward-scroll sequence 10625 plays and unlock varbit
  17327 is set without automatically enabling the alternate animation. Non-members and duplicate
  use preserve the scroll. Exact rejection feedback and persistence across a reconstructed account
  session still require a matching-client/persistence capture.
- Vengeance rebound now observes the actual damage applied by the standard queued player-hit
  processor. The next positive player- or NPC-sourced impact reflects
  `max(1, floor(actual Hitpoints lost * 3 / 4))`, does not reduce the incoming damage, consumes the
  armed effect once and speaks the published phrase. Unsourced environmental damage and zeroes do
  not consume it. The cutoff recoil-immune roster is guarded by resolved visible NPC name; every
  morph identity still needs a typed cache audit. POH entry/exit controllers do not yet exist, so
  the presence seam rejects casts during test-owned POH state but cannot yet clear active Vengeance
  on real POH entry. Exact rejection text, graphic/sound routing, NPC rebound cycle, Smite and ring-
  of-recoil ordering, simultaneous impacts, reconstructed-session cooldown persistence and packet
  order, animation-picker random-selection behavior and alternate audiovisual packet timing remain
  manual case 52 before the groups can advance beyond `integration-tested`.
- Bryophyta's staff now has an integration-tested per-item stored-nature core instead of the old
  false unlimited-rune mapping. Inventory loading caps at 1,000; Check works in inventory and while
  worn; Uncharge destroys the stored runes; casts source stored charges before loose inventory and
  pouch runes; the 1/15 success saves exactly one charge; and depletion changes the worn object to
  its uncharged identity. Cache varbit 16139 was initially suspected to hold live charges, but its
  actual cache contract is the permanent bank auto-charge preference over varp 4677 bits 20–30;
  live charges therefore remain item-local through `charges_16383`. Exact load/full/check/uncharge
  text and whether Uncharge has a confirmation are bounded inferences, as is source priority before
  inventory and pouch. Manual members-only creation and both charged/uncharged equip gates now have
  integration coverage: the recipe uses a boostable Crafting level of 62, consumes the selected
  battlestaff and essence atomically for zero XP, and the staff uses base Attack/Magic levels of 30
  on free-to-play worlds. Cache object 22368 overloads its first generic stat requirement with the
  Crafting recipe requirement, so typed runtime edits expose the two wield requirements while the
  recipe owns Crafting 62 explicitly. Exact manual animation, duration, messages and packet order,
  plus Zaff's free-world service, remain researched. The charged staff now consumes the shared bank
  Configure-Charges event and persists a zero-to-1,000 preference through cache varbit 16139,
  including param 2257/DB row 4340 dispatch. Withdrawals consume the available nature runes up to the configured
  target and transform depleted staves. Exact configuration presentation and trade/death
  behaviour still require manual case 81.
- Lunar Spellbook Swap now has an integration-tested 200-tick, one-successful-spell lifecycle with
  access-gated Standard/Ancient/Arceuus selection, atomic post-selection charging, LMS cancellation,
  cache presentation and the moving-cast run-energy exemption. Varbit 4823's selected-book encoding
  remains a bounded `Spellbook.varValue` inference because no authoritative clientscript decode was
  found. Exact picker presentation, literal feedback, downstream-effect failure semantics, all
  spell-family event paths, LMS start-cycle ordering, cold reconstructed-session persistence and
  matching-client packet/timing proof remain manual case 54 before promotion beyond
  `integration-tested`.
- The eight fixed-destination Lunar self teleports now have an integration-tested cache-233 core,
  including exact cache costs, XP, destinations, presentation, a separate Ourania/Baba Yaga unlock,
  Tele Block and source-Wilderness gates, and an atomic Ice Plateau warning. Exact Ourania locked
  feedback and Ice Plateau warning interface/text/settings remain uncaptured. Destination landing
  variation, route and interruption behavior, Deadman delay rules and packet timing remain manual
  case 55 before promotion beyond `integration-tested`.
- All seven Lunar group teleports now have an integration-tested cache-233 core with exact
  level/rune/XP/component contracts, exact Wiki landing polygons, same-plane 3-by-3 Accept-Aid
  candidate selection, native independent recipient offers, four-tick caster timing, caster and
  recipient Tele Block/Wilderness revalidation, atomic Ice confirmation and shared Lunar
  presentation. The exact Lunar Diplomacy progress threshold that proves a Moonclan recipient has
  visited Lunar Isle is unpublished and deliberately unenforced. Account-mode Accept Aid rules,
  recipient timing/animation, offer text/timeout, Ice recipient warning presentation, movement/
  logout/death races, destination random distribution, Deadman delay semantics and packet ordering
  remain manual case 56; the accepted-recipient four-tick presentation is an explicit bounded
  inference. The group remains `integration-tested`, not client-tested or verified.
- Lunar Stat Restore Pot Share now has an integration-tested cache-233 core covering exact
  level/rune/XP/item contracts, the seven compatible potion families, transactional dose use, the
  free caster effect, same-plane 5-by-5 Accept-Aid selection capped at four recipients, exact
  immediate restores and cures, per-recipient Prayer modifiers and exact 200-cycle stamina/run-
  drain behavior. Sanfew now grants the exact varp-backed six-minute poison immunity and fifteen-
  minute disease immunity through shared protection repositories, with the dated standard-death
  removal rule. The Group Ironman
  exception awaits account modes; source-specific poison/disease infliction consumers, toxin
  reapplication, nonstandard-death/reconstruction disease policy, Ring of Endurance extension and
  durable stamina lifecycle remain open. Exact zero-recipient feedback, recipient
  priority, cast cooldown, graphic height/delay, sound, literal messages, movement/logout/death
  races and packet order remain manual case 57. The three-cycle lock, 96-height graphic and
  deterministic player-list priority are bounded inferences, so the feature remains
  `integration-tested`, not client-tested or verified.
  Direct Restore, Energy, Prayer, Super energy, Super restore and Stamina drinking now shares the
  same formula implementation through the generic successful-consumption event. All 24 dose
  identities, cache descriptions, membership gates, full-inventory replacements, vial smashing,
  independent three-cycle clock and no-attack-delay behavior are integration-tested. Restore
  affects Attack, Strength, Defence, Ranged and Magic only by `floor(base * 0.30) + 10`; Energy
  restores 10 percent maximum run energy; Prayer restores `floor(base * 0.25) + 7`; Super energy
  restores 20 percent; Super restore restores every non-Hitpoints stat and Prayer by
  `floor(base * 0.25) + 8`; Stamina restores 20 percent and starts or resets the exact 200-cycle,
  70-percent-drain-reduction lifecycle. Holy wrench, Prayer cape, Max cape and imbued Ring of the
  gods use the existing 27-percent Prayer modifier for direct Prayer and Super restore doses.
  Ring of Endurance extension, reconstructed-session persistence, death/world-hop behavior,
  Stamina expiry presentation, the two inferred first lines and exact packet ordering remain open
  under manual case 57.
- Lunar Boost Potion Share now has integration-tested ordinary stat-boost, poison-protection,
  antifire and goading cores. These cover all eight cutoff poison-remedy families, all four
  antifire families and Goading potion, their 52 dose chains, exact toxin cures, exact
  600/1,200/300/600-cycle antifire durations and the six-minute goading duration with cache varbit
  displays. A reusable consumer resolves the full ordinary chromatic and metallic dragonfire
  protection matrix. The goading consumer covers passive attackable monsters in the exact 9-by-9
  area, visible Slayer requirements, occupied-monster protection, immediate multiway acquisition,
  roughly three-second single-way replacement and the cache-typed Jal-Nib/Yt-HurKot exclusions.
  The full spell remains incomplete until every other cache-233 non-barbarian candidate is
  positively classified and implemented. Source NPC dragonfire selection, shield identification
  and charging, exact combat messages, and boss/special breath tables remain open. Goading now
  cancels through every implemented spell-teleport family and ordinary Cup of tea 1978, emitting
  the observed exact early-expiry message while generic relocations remain unaffected. It still
  needs exhaustive immunity proof, the remaining non-spell teleport sources, natural-expiry
  message confirmation, definitive single-way phase capture and matching-client multiway fan-out
  observation.
  Direct Goading consumption now applies and resets this same lifecycle from all four cache dose
  objects. Exact presentation/packet ordering and reconstructed-session potion-delay behavior
  remain matching-client work.
  Direct ordinary Antipoison consumption now shares that three-cycle potion clock and performs the
  exact poison cure or first-dose venom downgrade before applying the -5 immunity state. Its frame
  sound and inventory/message/varp packet ordering remain matching-client observations.
  Direct Superantipoison, Antidote+, Antidote++, Anti-venom, Anti-venom+ and Extended anti-venom+
  consumption uses the same clock and exact existing -20/-30/-40/-41/-50/-59 lifecycle states.
  The first three preserve the documented two-dose venom downgrade/cure path, while all three
  anti-venom families cure venom immediately. Matching-client first-line capture is still required
  for the five cache-stem-derived messages; all six still need sequence-frame sound and exact
  inventory/message/varp packet-order capture.
  Direct Sanfew serum consumption applies its exact non-Hitpoints/Prayer restore, superantipoison
  path, disease cure and 1,500-cycle immunity through those same shared repositories. Standard
  death now emits a composable completion event and clears that disease immunity as dated. Its
  inferred first line, sequence-frame sound, packet ordering and nonstandard-death/reconstruction
  policy remain matching-client work.
  Direct Antifire, Extended antifire, Super antifire and Extended super antifire consumption now
  share the same clock and atomically apply/reset the existing ordinary or superior protection
  lifecycle for exactly 600/1,200/300/600 cycles. Cache quest table 0 and Dragon Slayer I row 31
  pin not-started state 0, started state 1 and completion state 10. The three extended/super family
  first lines, the post-cutoff-sourced exact quest rejection, sequence-frame sound and inventory/
  message/varbit packet order remain matching-client observations.
  Direct Attack, Strength, Defence, Ranging, Magic, Super attack, Super strength, Super defence,
  Super combat, Bastion, Battlemage, Agility and Fishing potion consumption now shares the exact
  existing Boost Potion Share formulas. All 52 dose objects, cache descriptions, `Drink`
  operations, Strength-only free-world access, selected-slot full-inventory transformations, vial
  smashing and the independent three-cycle clock are integration-tested. Exact effects cover the
  ordinary 10-percent-plus-3 melee boosts, Ranging's 10-percent-plus-4 boost, Magic/Agility/Fishing
  fixed boosts, the three 15-percent-plus-5 super boosts and the combined Super combat, Bastion and
  Battlemage applications. The shared ordinary/super Attack, Strength and Defence wording and the
  four other historical-family lines are directly recorded; the pinned Bastion and Battlemage
  revisions include the exact `It's a little tangy.` suffix. Super combat's first line, sequence-
  frame sound, exact packet order and end-to-end stat-decay/Preserve timing remain matching-client
  observations under manual case 58.
  Direct Combat and Hunter potion consumption now covers all eight cache dose objects, members-only
  access, full-inventory dose/vial chains, vial smashing, the shared potion clock and exact immediate
  Attack-plus-Strength 10-percent-plus-3 and Hunter fixed-three boosts. The direct-only resolver
  keeps both incompatible with Boost Potion Share. Neither cutoff item revision nor the historical
  production-content corpus records either first-line drink message; those two template lines,
  sequence-frame sound, packet order and end-to-end stat-decay/Preserve timing remain matching-
  client observations under manual case 58.
  Direct Prayer regeneration now covers all four cache dose objects, members-only rejection,
  selected-slot full-inventory dose/vial chains, automatic vial smashing, the shared potion clock,
  cache timer varbit 11361, 66 one-point grants at exact 12-cycle intervals, base-level capping,
  holy-equipment neutrality, resident logout/login phase preservation, persisted-count
  reconstruction, standard-death clearing and Stat Restore Pot Share rejection. Cache 233 has no
  central event for the stat-reset minigames named by Jagex, so those cancellation hooks remain
  incomplete. The exact first-line message, redose/phase semantics, cold-session sub-interval,
  varc-1200 buff start, world-hop/nonstandard-death behavior, sequence-frame sound and packet order
  remain matching-client observations under manual case 59.
  Direct Saradomin, Zamorak, Ancient and Forgotten brews now cover all 16 cache dose objects,
  members-only
  access, full-inventory dose/vial chains, vial smashing, the shared potion clock and their exact
  immediate boost, drain, healing, self-damage and Prayer formulas. Both Lunar sharing spells
  reject all four families atomically. Saradomin's first line is directly captured; Zamorak's
  matching foul-liquid line has only a historical community observation, and Ancient's family-name
  line and Forgotten's `You drink some of your forgotten brew.` line remain bounded template
  inferences. Sequence-frame sound, exact stat/message/inventory packet order and ordinary stat-
  decay/Preserve timing remain matching-client case 60. The separate Forgotten-brew upgrade now
  implements every matching dose, exact 20-essence-per-dose ratio, boosted level 91, three-tick
  action, full-inventory same-slot output and 36/72/108/145 XP with stale-resource and cancellation
  safety. Its animation, sound, exact feedback, repeat behavior and packet presentation still
  require matching-client proof under case 60.
  Direct Menaphite remedy consumption now covers all four cache objects, full-inventory dose/vial
  chains, vial smashing, the shared potion clock, a delayed first restoration and exactly twenty
  25-cycle pulses over five minutes. Each pulse applies the cutoff formula only to lowered Attack,
  Strength, Defence, Ranged and Magic; Hitpoints, Prayer and already-boosted values are untouched.
  Redosing resets the count and phase, varbit 14448 tracks remaining pulses, both Lunar sharing
  spells reject the family, and consumption clears all eight divine-potion plus saturated-heart
  timer states without changing their visible stat boosts. The typed cancellation seam is ready
  for those effects, but their complete production lifecycles remain unimplemented. Exact first
  drink wording, audiovisual and packet presentation, cold-session sub-interval persistence,
  world-hop/nonstandard-death behavior and matching-client proof of standard-death clearing remain
  case 60 rather than being claimed as exact.
  Direct Surge potion consumption now covers all four cache dose objects, members-only access,
  full-inventory dose/vial chains, vial smashing, the shared potion clock, immediate 25-percent
  special restoration, the exact 500-cycle cooldown paused while logged out, ten-cycle client
  timer units, standard-death clearing and the six-cycle PvP restore with its 15-tile boundary.
  However, the server's Colosseum, Inferno, Theatre of Blood, Doom, raids, Ferox portal and POH-pool
  systems do not yet expose the complete phase/transition events needed to reproduce every
  documented pause and reset. Production also has no general updater for `inside_wilderness` or
  `pvp_area_client`, so non-Wilderness PvP classification still depends on the world flag until
  area ownership is integrated. The first drink line and exact remaining-time grammar are bounded
  inferences; logout during the six-tick restore has no pinned rule and currently cancels; the
  second graphic, varclient-1258 start, sequence-frame sound and packet ordering remain manual case
  61. The direct core is integration-tested, while the world/activity hooks remain researched.
  The shared player
  lifecycle now applies exact recurring
  poison/venom damage and interface pausing, but source-specific NPC/weapon/spell poison and disease
  infliction, toxin reapplication, Sanfew nonstandard-death/reconstruction policy, Group Ironman
  eligibility,
  exact Castle Wars instance coverage, zero-recipient feedback, recipient priority, cooldown,
  graphic height/
  delay, sound, messages, action races and packet ordering remain manual case 58. The three-cycle
  lock, height-96 graphic and deterministic player-list priority remain bounded inferences.
- Banking, music timing, follower UI, authentication and reconnection contain incomplete paths.

- Demonic and Sinister Offering now have an integration-tested core over every canonical Prayer
  remains recipe. Their exact no-remains/cooldown wording and priority, directly observed eight-tick
  boundary, preparation-sequence behavior after the instant-cast update, audiovisual/packet order
  and cooldown persistence across lifecycle transitions remain manual case 93. The eight-cycle
  implementation is a bounded cache/evidence reconstruction, not yet a matching-client capture.
- All twelve Arceuus destination teleports now have an integration-tested cache-233 core, including
  eleven fixed cache destinations and the realm's configured default Respawn destination. Exact
  per-player selection and persistence for the seven standard respawn locations are not represented
  in player state or linked to the current death path; the Defence-cape Ardougne exception must not
  count. The separate Priest in Peril Drezel-warning edge, exact quest/failure wording and priority,
  landing distribution, audiovisual/packet ordering and lifecycle races remain manual case 94.
- All four Arceuus reanimation tiers and twenty-three creature mappings retain their integration-
  tested cast/reward core. Initial heads dropped by NPC tables can now carry an immutable origin
  through ground registration, pickup and re-drop; both carried and ground initial heads work only
  in the same-level 31-by-31 area centered on that origin. Banking expires the initial identity to
  the processed cache object, including bulk deposits, while a failed or partial transaction
  restores any undeposited fresh item and its origin. Trade, Grand Exchange and price-checker
  expiration cannot be wired until those systems exist. Noted-head handling, Soul Bearer routing,
  abyssal Slayer restrictions/credit, creature-specific attacks, KBD conversion, exact lifetime and
  respawn edges, failure priority, audio, packets and matching-client behavior remain manual case
  95; those gaps are not implied complete by the provenance substrate.
- All nine Arceuus resurrection-thrall spells now have an integration-tested server core. The
  documented exclusion inventory (Castle Wars, Clan Wars, most of the Chasm of Fire, selected
  Temple Trekking and quest encounters, Farming Guild outside Hespori, Grand Exchange, Jormungand
  Prison, player-owned houses, Soul Wars, Ungael crater, Tombs/Theatre lobbies, sailing boats and
  Guardians of the Rift) is not yet backed by authoritative instance/area owners and is therefore
  deliberately not represented as guessed coordinate rectangles. Thralls can still follow across
  a room boundary because the server has no shared room-identity contract. Spawn-square priority,
  exact rejection/cooldown wording and priority, persistence across reconnect/death/world hop,
  attack ranges, inferred attack sequences/projectile profile, sound and packet order remain manual
  case 96. The individual spell record's 16-tick cooldown conflicts with the ten-second prose; the
  implementation currently uses 17 server cycles, consistent with ten seconds on the 600 ms game
  clock, until matching-client observation resolves it.
- The standard Mining rock lifecycle now covers both cache variants of clay through runite and all
  eight ordinary bronze-through-rune/gilded pickaxes. A separate integration-tested slice covers all
  14 cache-233 dragon, infernal, third-age and crystal identities, their exact use/access gates,
  animations, two-or-three-cycle rate rolls and expected-speed tool priority. Infernal-pickaxe
  creation and cosmetics now cover the standard-dragon-only recipe, unboostable 61 Mining/85
  Smithing gates, three-tick atomic conversion, 200/350 XP, implicit 5,000-charge state, exact
  cosmetic-dragon rejection, both charged/depleted Trailblazer ornaments, revert and Trailblazer/
  Reloaded inventory/worn style switching with charge-variable preservation. Standard gem-rock
  extraction now covers both cache identities, level/XP/success/respawn metadata, the exact
  128-weight gem table, charged-glory multiplier, reward and found-gem sound. The separate ordinary-
  rock gem pre-table now covers its exact 32,768/8,216 denominators, 70/32/16/8/2 partition,
  nothing outcome, ore-roll replacement, no-XP/no-depletion contract and glory variants. The gem
  bag now has cache-backed closed/open operations, permanent 60-each sapphire-through-dragonstone
  storage, fill/use/check/open/close/inventory-empty/bank-empty/destroy behavior and direct routing
  for complete eligible ground stacks plus ordinary-rock and gem-rock Mining rewards. Gem-bearing
  Thieving acquisition routing, one-bag ownership/shop enforcement, exact free-world feedback, partial-empty client
  presentation and death/recovery observation remain unresolved; the cutoff record says contents
  survive death, while later Wiki prose conflicts. Mining also does not yet implement gem-rock
  clue-geode/rock-golem rolls; Mining-glove extra gathers; Varrock armour,
  celestial equipment or Mining-cape duplication; prospector XP; Mining Guild +7 and respawn
  variants; Infernal passive smelting, depletion, recharge or Check presentation. Crystal pickaxe
  bank preference now uses active object 23680's param 2257/DB row 4338 and permanent varbit 16148,
  with members-only zero-to-20,000 storage. Item-local Crystal charge persistence, shard/revert
  operations, automatic withdrawal charging and exact Configure-Charges presentation remain open;
  Tutorial Island XP caps; perfect-
  gold quest state; member and area-specific rock variants; unidentified minerals, crystal shards,
  other geodes, rock golem or moon-key pieces; or all
  respawn-area exceptions. Gem-rock access/quest integration, mining-enhancer exclusions and exact
  client timing remain unverified. Blurite's published
  25-second respawn is currently rounded to 42 server cycles. Exact audio, overlay colour, message
  and packet order, interruption, two-player contention and same-cycle depletion races remain
  workspace-client case 97 and are not claimed as exact.

Every source TODO must eventually be classified as in-scope behavior, engineering cleanup, obsolete,
or proven out of scope. An unclassified TODO blocks completion.

## Thieving gaps after the Man/Woman, Farmer, Warrior, Rogue, Guard, Desert Bandit, Knight, Watchman, Menaphite Thug, Paladin, Gnome, Hero, Vyre and Lletya Elf cores

Later production work adds nineteen ordinary stalls, master farmers/Martin, seed tables, gloves of
silence and diary hooks; see [the current production coverage record](PRODUCTION_SKILLS.md). The
older research notes below retain their original scope and should not be read as the current
implementation inventory.

- The 27 cache-operable Man/Woman identities, all 20 ordinary/Varlamore/Kastori/Tal Teklan Farmers,
  all seven Warriors, all 91 ordinary city/regional Guards, all eleven ordinary Ardougne/Varlamore
  Knights and their fixed coin-pouch identities are implemented, along with ordinary Rogue 526
  and its 25–40-coin pouch. Both Kharidian Bandit Camp identities are implemented with their 5/7
  pouch, 1/7 antipoison(1) and 1/7 lockpick table. All three Heroes are implemented with their
  exact 128-weight pouch/rune/wine/gem/orb/ore table and variable 200–300-coin pouch. H.A.M.
  members, their five distinct storeroom guards and all six key-operated Small chests are now
  source/cache-pinned but not dispatched;
  cave goblins, Fremennik,
  Pollnivnian/blackjack bandits, Prifddinas elves and
  wealthy-citizen production remains, along with every stall, chest, door, blackjacking, Sorceress's Garden
  and minigame/quest route.
- Yanille Watchman 5420 is implemented with its exact level, XP, success endpoints, three-damage
  failure, 60-coin pouch and guaranteed bread. The generalized reward transaction now supports
  fixed multi-item bundles, doubles every component through Rogue equipment, and rolls back the
  whole bundle and XP if any output cannot fit.
- Post-quest Menaphite Thug 3550 is implemented for its ordinary operation-three pickpocket with
  level 65, 137.5 XP, 50/160 endpoints, five failure damage, a five-second stun and one fixed
  60-coin pouch 22534. Cache 233.4 proves that pre-quest identity 3549 has Talk-to and Attack but
  no Pickpocket operation, while 3550 additionally exposes Pickpocket, Lure and Knock-Out; the
  cache morph therefore owns quest progression and only 3550 enters the ordinary dispatcher.
  Gloves, hard diary, cape, Shadow Veil, dodgy necklace, Rogue one-pouch-plus-60-direct-coins
  behavior and atomic rollback use the shared exact contracts. Menaphite blackjacking remains a
  separate researched system: its 78/240 knockout roll, Lure/Knock-Out sequencing, two eligible
  pickpockets, aggression, inventory behavior and exclusion of Gloves/dodgy/Shadow Veil must not
  be folded into this ordinary action.
- All eight operation-three East/West Ardougne Paladins are implemented with exact level, XP,
  success endpoints, three-damage failure, 80-coin pouch and two guaranteed chaos runes. Their
  medium-diary bonus is safe to apply by family because every production spawn is inside Ardougne.
  The independent hard-clue tertiary is now implemented at 1/1,000 or 1/950 after completed Hard
  Combat Achievements, with the exact 185-step cache pool, ownership, X Marks boxes/capacity,
  full-inventory blocking and Rogue non-doubling. Only live distribution and matching-client
  presentation remain in manual case 101.
- Heroes 3295, 11934 and 11935 are implemented with level 80, 163.3 XP, 39/160 endpoints,
  three-damage failure, the sourced six-second stun and the exact 105/8/6/5/1/2/1 reward weights.
  Hero pouch 22537 rolls 200–300 coins when opened. Its Rogue-equipment exception deliberately
  awards one pouch plus exactly 200 direct coins, while every non-pouch result doubles and XP is
  awarded once. Its independent elite-clue tertiary is implemented at 1/1,400 or 1/1,330 after
  completed Elite Combat Achievements, with the exact 135-step cache pool and the same shared
  ownership, box/capacity, inventory and Rogue non-doubling policy.
- Ordinary Gnomes 6094–6096 are implemented with level 75, 133.5 XP, 43/175 endpoints, one-
  damage failure, five-second stun and the exact 56/30/24/8/5/3/2 reward weights. The shared
  reward model now rolls the sourced 2–4 arrow-shaft quantity once and doubles that result through
  Rogue equipment; Gnome pouch 22536 opens for fixed 300 coins. Battlefield IDs 5968–5970 remain
  excluded because cache 233 exposes Attack but no Pickpocket operation despite their inclusion
  in the Wiki subject infobox. Its independent medium-clue tertiary is implemented at 1/150 or
  1/142 after completed Medium Combat Achievements, with the exact 165-step pool, X Marks scroll
  boxes, ownership, capacity warnings, loose-clue full-inventory blocking and existing-box stacking.
- H.A.M. members 2540–2541 and all 39 possible ordinary outputs now have typed cache references
  and a focused cache-boundary test. The final pre-cutoff page pins level 15, 22.2 XP, 135/239
  base endpoints, 1–3 failure damage, a four-second stun, the 1/50 easy-clue pre-roll, a conditional
  100-share main table and 1–21-coin pouch. It also pins the special Death to the Dorgeshuun robe
  override and three-slight-concussion ejection model. Production remains absent because each worn
  H.A.M. piece is only documented as reducing ejection chance by “about 4%”, while exact jail versus
  outside selection, destination choice and lifecycle state are unresolved. Cache 233 additionally
  places both members' Pickpocket action in operation slot one, so they must not enter the generic
  operation-three dispatcher.
- H.A.M. storeroom post-quest guards 4522–4526, quest patrol guards 4516–4520, arrest guard 4521
  and bronze/iron/silver/steel keys 8867/8869/8868/8866 now have typed cache references and a
  focused boundary test. The final pre-cutoff guard page pins level 20, 22.2 XP and an exact
  500-share reward table: each key occupies 50 shares, with the remaining 300 shares covering the
  same H.A.M. equipment/material family, pouch 22523, a six-share easy clue and a six-share nested
  herb branch. The dated storeroom page proves failures never eject the player, deal up to three
  damage, and stop/reset that guard's patrol and door-aggression behavior. Production remains
  absent because the guard page is explicitly categorised as needing a skilling success chart;
  neither an exact success formula nor an exact stun duration/damage distribution is published.
  Clue ownership/full-inventory behavior, quest-state morph timing and patrol/line-of-sight state
  also remain matching-client work. All four dated key pages say the keys are nontradeable, while
  cache 233 leaves their generic `tradeable` flag true but `stockmarket` false; eventual trade
  handling must enforce the sourced behavior explicitly instead of trusting that cache default.
- H.A.M. storeroom closed Small chests 15722–15724 and 15726, open type 15725 and all six static
  cache placements now have typed references and a focused map/cache test. Final pre-cutoff Small
  chest revision 14770595 maps bronze to the south-west chest, iron to north-east, silver to
  north-west and steel to south-east plus both northern chests. Its exact 150-share reward table
  is pinned across 0–99 coins and nineteen gem/jewellery bundles, with all twenty reward objects
  resolved to cache identities. Production remains absent because the dated source does not state
  the inventory-capacity contract for multi-item bundles, missing-key response, chest-open and
  replacement durations, simultaneous-player contention semantics or whether a zero-coin roll
  transmits an inventory/message event. Those are matching-client case 101 measurements, not safe
  defaults to infer from unrelated chests.
- H.A.M. storeroom locked Door 15759 and its four static central-room placements now have a typed
  cache reference and focused map/cache test. Cache 233.4 places `Open` in operation slot one and
  `Pick-lock` in slot four, and resolves the exact description plus lockpick 1523. The dated
  storeroom page pins 4 XP, improved success with a lockpick, two crack bypasses for the south-west
  room, an unlocked northern-room door and attacks by patrolling guards with line of sight. It does
  not publish the Thieving level, exact with/without-lockpick success endpoints, failure/retry
  cadence, movement/door transition, messages or the patrol observer/timing rules. Production is
  withheld until case 101 recovers those action-defining values.
- Wealthy citizens 13302–13305, coin pouch 28822 and House keys 29325 now have typed cache
  references, along with dedicated eight-bit permanent House-key dry-streak varbit 9942, and a
  focused boundary test. All four NPCs expose Pickpocket in operation slot one,
  followed by Talk-to, so none belongs in the generic operation-three dispatcher. The final
  pre-cutoff page pins level 50, 96 XP, 35/200 base endpoints, three failure damage, a four-second
  stun and an 85-share table: an 85-coin pouch at 79/85, House keys at 5/85 and an easy clue at
  1/85, improved to 1/80 by completed Easy Combat Achievements and converted to a scroll box after
  X Marks the Spot. Gloves, hard diary and hard-diary-plus-cape endpoints are 36/210, 38/220 and
  41/242. The contemporary Jagex overview resolves coin pouches and the separate key chance, but
  production remains absent because no source publishes how dry-streak varbit 9942 changes or
  resets the nominal 5/85 key branch. Full-inventory key/clue discards also need non-atomic reward
  routing and direct observation. Street urchins distract
  three citizens every 84–99 seconds for about 20 seconds, enabling automatic guaranteed attempts;
  exact world-hop recency state and the roughly 15-second no-item delay remain to be observed.
- Pollnivneach Bandits 734–737 and both pouch identities now have typed cache references and a
  focused boundary test. NPCs 734/736 expose only Talk-to, Attack and Pickpocket, while their
  unlocked morphs 735/737 additionally expose Lure and Knock-Out. The bearded pair 736–737 uses
  level 45, 65 XP and 40 coins mapped to pouch 22529; the non-bearded pair 734–735 uses level 55,
  84.3 XP and 50 coins mapped to pouch 22532. Awake pickpockets produce pouches while successful
  knocked-out pickpockets produce direct coins. The shared Knock-Out chart is 80/240 and awards
  10 XP, but the cutoff Thieving overview explicitly marks both ordinary pickpocket success rates
  unknown. Production therefore remains absent rather than inventing either curve. Full
  blackjacking also needs the exact The Feud morph state, lure/dialogue timing, second-bandit
  line-of-sight selection, failure-to-aggression transition, knockout duration, guaranteed-two-
  pickpocket window, combat interruption and inventory behavior.
- The final pre-cutoff Fremennik citizen page pins the quest prerequisite, level 45, 65 XP,
  guaranteed 40-coin pouch, two-damage failure, five-second stun and nine named production
  identities. It also explicitly labels the success rate unknown. Cache NPCs 3937–3941 and
  3943–3946, pouch 22528 and quest row 57 are inventoried, but production remains absent until
  matching-client or first-party evidence supplies the success endpoints and the exact completed-
  quest state; NPC 3942 is excluded from the dated nine-name list pending the same evidence.
- The final pre-cutoff Cave goblin page pins level 36, 40 XP, one failure damage, five-second stun,
  Death to the Dorgeshuun area access, the nearby-guard attack response and a complete twenty-share
  table. Thirteen of its fourteen identities expose Pickpocket; NPC 2301 is explicitly Talk-to-only.
  The source gives six cuisine results at 1/20 each, a 10–50-coin pouch at 7/20 and seven other
  1/20 results, including 1–4 iron ore and one wire doubled after the elite Lumbridge Diary. The
  cutoff Thieving overview explicitly labels its success rate unknown, so no production chance or
  guard-aggression approximation is invented pending matching-client evidence.
- Lletya Elves 5297, 5299, 5300 and 9015 are implemented with level 85, 353.3 XP, 6/100 endpoints,
  five failure damage, a six-second stun and the exact 105/8/5/6/1/2/1 table. Elf pouch 22538 opens
  for an independent 280–350 coins; Rogue equipment keeps one pouch plus one direct 280–350 payout
  or doubles the selected non-pouch result without doubling XP. Cache 233 excludes Kelyn 8914 and
  Mawrth quest/cutscene forms 8927–8928 because none exposes Pickpocket, even though their subject
  pages group them under named versions. The final cutoff page names exactly 49 Prifddinas targets,
  mapped and tested as operation-three NPCs 9054–9090 and 9106–9117. Similarly named citizens 9053
  and 9101–9105 have no Pickpocket operation and are typed exclusions. Crystal shard 23962 rolls at
  1/35 and enhanced crystal teleport seed 23959 at 1/1,024 before the shared main table; both can
  double under Rogue equipment, and the dated shard source requires a shard to fall to the ground
  when it accompanies another collected resource with only one free slot. Production remains
  absent until exact independence/order of both tertiary rolls, whether one Rogue roll couples all
  three outputs, seed/full-inventory destination and the combined transaction boundary are observed.
- All thirty contiguous Darkmeyer Vyres 9685–9714 are implemented with level 82, 306.9 XP, 8/128
  endpoints, five failure damage, a six-second stun and the exact 109/8/2/6/5/1/1 main-table
  weights. Vyre pouch 24703 opens for 230–315 coins. Its sourced Rogue exception keeps one pouch
  plus exactly 230 direct coins; selected non-pouch rewards double and XP remains single. The
  independent 1/5,000 blood-shard tertiary is now integrated: Rogue equipment can double it, the
  full set guarantees two, and an inventory unable to hold the complete shard quantity sends every
  shard to private owner-only floor objects while preserving the ordinary reward and single XP.
  Partial-Rogue statistical coupling, literal floor lifetime/presentation and live distribution
  remain in manual case 101; the Wiki does not expose Jagex's internal RNG-call order. Darkmeyer
  access and hostile Vyrewatch Sentinel behavior belong to world progression and combat AI rather
  than an invented action-time quest or clothing check. The direct Vyre page explicitly records a
  six-second stun, whereas the cutoff broad Thieving table records three seconds. Production
  follows the more specific subject page as ten action-block and eleven movement-block cycles;
  manual case 101 must resolve that dated-source conflict against the matching cache-233 client
  before the timing is considered independently verified.
- Kharidian Bandits 690 and 695 are isolated from the similarly named quest and blackjacking
  families and use exact level 53, 79.4 XP, 50/240 endpoints, three-damage failure, fixed 30-coin
  pouch 22530 and weighted one-dose-antipoison/lockpick alternatives. Their Saradomin/Zamorak
  equipment aggression belongs to combat AI and remains outside this pickpocket core.
- The medium Ardougne Diary's bonus is modeled for the fixed Knight of Ardougne family and is
  explicitly excluded from Knights of Varlamore. Both Knight families use the directly sourced
  six-second stun as ten action-block and eleven movement-block cycles. A reusable coordinate-scoped Ardougne check is
  still needed before the medium bonus can be applied to other supported families that may spawn
  both inside and outside the city; the globally applicable hard-diary bonus and cape stacking are
  covered. Rogue equipment now doubles rewards
  for every supported pickpocket definition, including its direct-coin pouch exception; gem-bag
  interception for future gem-bearing pickpockets remains absent.
- The cutoff Man page lists NPC 8858, while cache 233.4 exposes no pickpocket operation on that
  identity. It remains excluded pending matching-client/cache evidence rather than receiving a
  fabricated operation.
- Exact shout variants, stun/action boundaries, target facing, animation/graphic/audio packet order,
  multiplayer contention, interruption behavior and Farmer rare-reward/full-inventory presentation
  remain pending workspace RuneLite case 101. The ordinary Guard registration intentionally does
  not absorb H.A.M. storeroom guards 4522–4526: their operation-one key-loot and ejection behavior
  requires its own researched vertical slice. The storeroom's six key-operated Small chests are
  likewise source/cache-pinned but await their exact interaction lifecycle. Wilderness Rogue 6603 has no Pickpocket operation in
  cache 233.4 and remains excluded from the ordinary Rogue core.

- The ordinary Skillcape emote core maps all 46 cache-233.4 cape identities to the 23 stat-specific
  animation/graphic pairs and enforces the worn-cape and combat rules. The dated Cape of
  Accomplishment history also forbids these emotes aboard the Fishing Trawler, but this workspace
  has no implemented Trawler activity or authoritative aboard-state contract. That restriction,
  its exact feedback, animation/graphic height and packet timing remain isolated in manual case 109
  rather than being guessed from the unrelated Trawler interface varps.

## Test baseline

The root project has no `integrationTest` task. Integration suites use the module-scoped task name
`integration`. The first attempted baseline therefore failed during task selection and says nothing
about test health. Use `./gradlew test` plus the module integration tasks documented in
`IMPLEMENTATION_LOG.md`.

## Sheep and Zanaris Sheep

- The production shearing core now distinguishes the eleven ordinary variants from Zanaris NPCs
  5843/5844. Ordinary first-option shearing always succeeds and uses a 50-cycle transformation;
  Zanaris retains Talk-to in operation one, accepts shears only through item use, fails one attempt
  in four, transforms either colour into dark-grey sheared NPC 5845 and uses 100 cycles. The unused
  light-grey sheared cache identity 5846 is deliberately not registered as the transformation.
- The cutoff Zanaris page does not state the per-attempt failure chance. A later page revision,
  inspected on 13 September 2026, explicitly documents the legacy 25% failure and exact line. It is
  retained as post-cutoff evidence describing old behavior, not as proof that a new mechanic was
  introduced after cache 233.4. Matching-client case 133 must independently confirm it.
- The final pre-cutoff transcript pins the opening, Yes/No branches, all four poems, Nuff's
  certificate exchange and the sheared refusal. It says the Cow responds only sometimes after two
  poems, without publishing the probability or precise presentation. No probability is invented;
  dialogue plus exact failure movement, animation, sound and packet order remain researched.

## Loaded Woodsman tutor

- The inherited level-99 crash, level-19 menu error, omitted advanced tree menu, missing axe grants
  and incomplete intermediate/advanced lines are closed. The members-only 99,000-coin cape plus
  hood sale is atomic and integration-tested for first and later mastery.
- Exact chathead expressions, line wrapping, cape-purchase packet ordering and whether the jingle
  begins before or after the final NPC line still require matching cache-233 client capture in
  manual case 134. The capacity wording is a labelled post-cutoff observation because the final
  pre-cutoff transcript did not record that branch.

## Bob repair and degradable armour lifecycle

- Bob's loaded right-click `Repair` route and held-item route now repair every original Barrows
  piece, the four Ahrim ornament variants and the nine Moon armour pieces. Exact partial charge
  counts determine the quote, named cache stages provide safe canonical fallbacks, broken objects
  ignore stray packed state, and the complete coin/item mutation is atomic.
- Shared combat wear is integration-tested for every one of the 28 Barrows and nine Moon armour
  definitions. Validated player melee, ranged and magic hits and NPC melee launches establish the
  combat-entry boundary; all eligible worn pieces lose one point immediately and once per 90 ticks
  while combat remains active. Exact Barrows named stages and Moon used/broken transitions preserve
  per-item points, remain worn when reaching zero, expose held/worn `Check`, and become broken only
  after an ordinary drop warning has been accepted. Moon weapons are deliberately excluded.
- Exact state is integration-tested through bank deposit/withdraw and database save/login with
  multiple same-object copies. A monotonic shared tradeability decision now denies intermediate
  states to the existing price/trade query and NPC-shop paths while keeping fully repaired and
  broken states eligible. The still-missing player-to-player trade and Grand Exchange systems must
  consume that same decision when implemented; this slice does not claim those systems exist.
- Protected-item selection, gravestone/death-office behavior, PvP death breakage and the exact
  logout/world-hop combat-timer boundary remain open. Drop warnings, `Check`/break messages, sounds
  and packet ordering still require matching cache-233 client capture in manual case 136.
- Tindel Marchant, Dunstan, the Void Knight Outpost Squire and Aneirin repair routes, POH armour-
  stand discounts, PvP-broken/Perdu separation, exact failure presentation, sounds and packet
  ordering remain open. They are isolated in manual case 135 and must not be inferred as complete
  from Bob's transaction coverage.

## Loaded Hans account age

- Hans's former zero-value placeholder is closed. The live player now receives the character's
  persisted `created_at` timestamp during normal account metadata application, while the permanent
  `playtime` varp continues to advance once per client cycle and persists through ordinary saves.
  Dialogue option four and the cache-native third operation `Age` share one formatter.
- Exact cache-233 chathead expression, line wrapping and packet order remain a presentation-only
  matching-client check in manual case 137. The account-summary side panel is a separate interface
  consumer of playtime and is not claimed by this NPC row.

## River Lum canoe routing and axe selection

- The final pre-cutoff Canoe revision now pins all four level/XP tiers and the complete route
  contract. Automated coverage checks every destination from Lumbridge, the Champions' Guild,
  Barbarian Village, Edgeville and Ferox Enclave for every canoe tier, including the deliberately
  asymmetric rule that a log can leave Ferox for Edgeville while only a waka can enter Ferox.
- Canoe axe selection now uses actual tier ordering rather than only the level requirement. This
  closes iron-versus-bronze ties, excludes members-only axes on free worlds and applies the same
  Song-of-the-Elves state-194 gate to active and inactive Crystal axes as ordinary Woodcutting.
- Canoe cutting and shaping create no inventory item and perform no Infernal log-burn roll. Under
  the pre-cutoff Crystal and Infernal definitions, neither action consumes a special-tool charge;
  the stale degradation TODOs were replaced with that bounded rule. Matching-client case 138 still
  covers action cadence, state transitions, audiovisual and packet order, interruption, logout,
  concurrent use, warning presentation and the complete cutscene rather than treating the routing
  contract as end-to-end client parity.

## Dairy cow Cowbell theft

- Dairy cow 8689 now dispatches its cache-native second operation for members who have started Cold
  War, have current Thieving level 15 and have inventory capacity. The exact 128-to-200 level-99
  success curve, Cowbells 10593 reward, 16 Thieving XP, success line and failure transcript are
  covered. The quest gate consumes cache varbit 3293 as non-zero, intentionally allowing both an
  active and completed quest.
- The failure currently reuses the standard Thieving contract: one typeless damage, eight blocked
  cycles, the shared stunned animation/spot animation and the documented cow `MOO`. The dated
  Cowbells page and transcript do not pin the exact damage, duration, animation, spot animation,
  sound or packet ordering, so those values remain provisional until matching-client case 139.
- Cowbells `Play` now requires cache varbit 3306's penguin-disguise state and runs cache sequence
  5673 without consuming the item. The rejection wording is historically corroborated but its
  exact chat type, action lock, animation duration and packet order remain in matching-client case
  139. Zanaris Dairy cow 12111's conversation-based handout and the complete Cold War
  acquisition/consumption progression remain separate quest work.
- Automated coverage invokes the production resolver after arrival and separately proves cache
  operation identity. Exact client operation routing, approach/facing delay, repeat-click behavior,
  interruption and simultaneous-player presentation remain in manual case 139.

## Penguin bongos

- The cache-233 item recipe and Wiki contract are implemented: leather plus one mahogany plank,
  members content, unboosted Crafting 30, 25 XP, the transcript result line and Penguin-suit-only
  Play. The current Cold War gate deliberately accepts any non-zero main quest state so both the
  quest instrument step and post-quest replacement remain possible. The exact earliest accepted
  varbit-3293 value is not yet production-traced and remains case 140 rather than being asserted.
- Cache sequence 5674 `peng_player_bongo` is the short 147-client-tick player action and is used by
  inventory Play. Sequence 5675 `peng_bongo_ready` is a much longer 366-client-tick ready loop and
  remains reserved for the Ping/Pong performance until client observation pins its role. Neither
  cached sequence embeds a frame sound, so exact music/synth behavior, action locking, transformed
  appearance choreography and packet order remain matching-client work.
- The outside-suit rejection uses the historically corroborated Cowbells wording because the Wiki
  groups both instruments under the same Penguin-suit restriction. Its exact chat type and wording
  for Penguin bongos remain provisional. Full Cold War acquisition, the Ping/Pong hand-in and
  performance cutscene, quest consumption/replacement, logout and concurrent interaction remain
  separate quest work.

## Bird-house shell Crafting

- The nine portable shell recipes are closed for cache identities, inputs, boosted Crafting levels,
  XP, tools, animations, first-result/repeat cadence, both held-item directions and atomic failure.
  This row deliberately stops at creating the empty bird house in inventory.
- Bird-house spot placement and seed loading now have a separate Hunter row covering all four
  Fossil Island sites, all nine tiers, all 41 suitable seed identities, mixed partial loads and a
  permanent 50-minute readiness deadline. A second Hunter row covers mature harvesting, tier XP,
  clockwork, feathers, raw bird meat and the seed-nest roll. Both remain outside this Crafting row.
- Exact cache-233 success and rejection wording, any synth, animation/inventory packet ordering,
  tool-selection priority when several hammer variants are present and every client interruption
  boundary remain matching-client case 141. No RuneLite outside the workspace checkout may be used.

## Bird-house placement, seeding and mature-harvest lifecycle

- Production now places every portable regular-through-redwood bird house at each of the four
  cache-233 Fossil Island sites. The current boosted Hunter level is checked against exact levels
  5/14/24/34/44/49/59/74/89, and placement consumes one shell before moving the site's real display
  varp into the matching built state.
- All 41 seeds in the sourced suitable-bait table are accepted. Thirty low-level seeds contribute
  one of ten required units and eleven high-level seeds contribute two units, so high-level bait
  consumes five. Different seed types can complete the same load; partial units remain durable in
  a permanent server-only varp and completion replaces that value with an absolute 3,000-second
  deadline. Sound 2433 accompanies successful placement and loading.
- A mature active site's `Interact` route now atomically clears the display/deadline and awards the
  exact tier XP 280/420/560/700/820/960/1020/1140/1200. It returns one clockwork, one uniformly
  selected feather quantity of 30/40/50/60, and at most one seed nest from the cutoff Wiki's rounded
  visible-Hunter interpolation. Those inventory rewards privately ground on overflow. All ten raw
  bird meats always become private ground objects even when inventory space is available.
- Production now also follows the February 2025 convenience lifecycle. `Build` prefers the best
  carried usable shell and otherwise crafts one selected carried log plus clockwork directly into
  the empty site. Partial `Dismantle` and early `Empty` require a loss warning, discard the shell and
  seeds (and any immature loot), and return only the clockwork. Mature `Reset` prefers a carried
  usable shell; otherwise it reuses the harvested clockwork in place with one selected log, while
  awarding the same Crafting and Hunter XP and mature rewards as the separate actions.
- This lifecycle still does not claim tick-by-tick ten-bird catch simulation, the ten independent
  other-nest rolls or their clue-nest branch. The cutoff Wiki explicitly marks the clue mechanics
  incomplete, so production does not substitute guessed odds. Exact confirmation wording, full
  animation/sound/packet ordering, multiple-log menu presentation, clock-adjustment policy,
  interruption edges and diary/achievement hooks remain matching-client case 142.

## Fishstabber presentation and live-action continuity

- All eleven cache-233 harpoon states now resolve through the production special registry and have
  automated energy, boost, phrase, sequence and graphic coverage. Depleted tools intentionally
  retain Fishstabber; the instant boost does not inspect or mutate their charge state.
- Exact sound audience and timing, combat-tab/orb interface updates, packet order, stat restoration
  and Preserve interaction still require revision-matched client observation. The Wiki explicitly
  says activation does not interrupt Fishing; production's instant path does not clear a Fishing
  queue, but case 72 must demonstrate that end to end before client-level parity is claimed.

## Bird-snare trapping

- The complete cache-233 server lifecycle is implemented for the five ordinary bird-snare prey:
  placement, limits, AI attraction, success/failure models, smoking, catch bonuses, rewards,
  dismantling, expiration and logout cleanup. Cache operations—not secondary emulator conventions—
  establish that a failed snare has only `Dismantle` and a full snare has only `Check`.
- The two-tile attraction radius and three-cycle AI scan rate are bounded engineering values because
  the dated Wiki pages and cache do not encode the server search radius. Exact bird approach path,
  facing, animation frame timing, sound, smoke/Investigate wording and packet order remain
  revision-matched observation work in manual case 143. The cutoff Guild hunter outfit page pins
  a 2.5% increase but explicitly says whether it is additive or multiplicative is unknown;
  production currently applies it additively and case 143 retains that qualification.
- Hunter Rumour tailfeather requests, Hunter-area/tutorial restrictions and League hooks remain
  separate backlog work. Box traps, falconry, source-backed prey placements and relevant ordinary
  skill diary hooks are now implemented; see `PRODUCTION_SKILLS.md`. Anti-odour salt is not represented
  by a cache-233 symbol and is not imported from later revisions.

## Varlamore moths and extended butterfly lifecycle

- The cache-233 sunlight and two clickable moonlight NPC variants now have exact level, XP,
  success-rate, optional-jar, restoration and respawn coverage. The visually identical moonlight
  NPC 12773 has no cache operation and is deliberately not registered as catchable.
- The final pre-cutoff Butterfly Hunter page does not expose exact success curves for ruby harvest,
  sapphire glacialis, snowy knight or black warlock. A later production pass implements their
  source-backed requirements, XP, jars, effects, 98 spawn coordinates and Snowy knight diary hook
  with an explicitly approximate catch model. Exact odds remain unverified; unrelated moth curves
  are not reused. See `LegacyButterflyTable.approximateChance` and `PRODUCTION_SKILLS.md`.
- Rare sunlight/moonlight wings, Hunter Rumour assignment/progress/pity state and reward routing
  remain separate Hunter Guild work. Their existence must not be inferred from the ordinary moth
  catch path.
- Accept Aid, jar-to-player sharing, recipient level rules and simultaneous-player ownership are
  not implemented. Exact failure/success text, catch and release delays, sounds, packet ordering,
  interruption semantics and the role/audience of cache sequence 5214 remain matching-client case
  144. The current one-tick action and player-only jar-open sequence are explicitly bounded server
  choices, not verified client parity.
