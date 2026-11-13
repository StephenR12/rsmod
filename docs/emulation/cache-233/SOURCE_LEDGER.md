# Cache 233 source ledger

## Revision boundary

| Source | Use | Status |
| --- | --- | --- |
| [2025 cache history](https://oldschool.runescape.wiki/w/RuneScape:Cache/2025) | Cache/subrevision dates | verified source |
| [233.1](https://oldschool.runescape.wiki/w/RuneScape:Cache/2025/233.1) | 10 September cache diff | indexed |
| [233.2](https://oldschool.runescape.wiki/w/RuneScape:Cache/2025/233.2) | 17 September cache diff | indexed |
| [233.3](https://oldschool.runescape.wiki/w/RuneScape:Cache/2025/233.3) | 24 September cache diff | indexed |
| [233.4](https://oldschool.runescape.wiki/w/RuneScape:Cache/2025/233.4) | 1 October final cache diff | indexed |
| `api/core/.../Build.kt` | Local revision and OpenRS2 archive identity | verified source |
| [OpenRS2 archive 2317](https://archive.openrs2.org/caches/runescape/2317) | Final 233.4 capture at `2025-10-01T10:45:07.114859Z` | verified source |
| `.data/cache/vanilla` | Unmodified archive-2317 cache evidence; `main_file_cache.dat2` SHA-256 `33c723c3032bf33cb7224a3a48a3c94046472ce65962e2223ce870c6cc7de416` | present |
| `.data/symbols` | Typed cache identities | present |

## Cache-233 update behavior

| Update | Required parity evidence |
| --- | --- |
| [Pet Insurance Rework & More](https://oldschool.runescape.wiki/w/Update:Pet_Insurance_Rework_%26_More) | 233.1 player-facing changes and fixes |
| [Poll 85: Bridges, Boots, Ropes & Roots](https://oldschool.runescape.wiki/w/Update:Poll_85_-_Bridges,_Boots,_Ropes_%26_Roots) | 233.2 construction, travel, shops, farming, Hunter and Slayer changes |
| [Interface Uplift](https://oldschool.runescape.wiki/w/Update:Interface_Uplift) | 233.3 settings, UI and gameplay changes |
| [Interface Uplift Round 2](https://oldschool.runescape.wiki/w/Update:Interface_Uplift_Round_2) | 233.4 UI, travel, storage, raids, Slayer and PvP changes |

## Historical-source discipline

For every gameplay claim, record the page URL, page revision ID or retrieval timestamp, applicable
game revision, exact section, and whether the claim is direct or inferred. Current Wiki pages may
contain Sailing and later changes. Use page history and dated change tables to reconstruct the state
at 233.4. Cache presence alone is not evidence that content was production-accessible.

When sources disagree, record both claims and the resolution. Do not silently choose the more
convenient value. Production observation and contemporary official documentation outrank later
summaries; unresolved claims remain incomplete.

## Standard elemental spell scaling

The spell pages were pinned to their last revisions on or before the 233.4 cutoff. All twenty were
dated 20 August 2025, with revision IDs `14970049` through `14970075` (non-contiguous). The pages
define tier scaling introduced on 29 May 2024: each elemental spell scales to the strongest unlocked
spell in its tier. The tested boundaries are Strike 1/5/9/13, Bolt 17/23/29/35, Blast 41/47/53/59,
Wave 62/65/70/75 and Surge 81/85/90/95.

Representative source: [Wind Strike revision 14970070](https://oldschool.runescape.wiki/w/Wind_Strike?oldid=14970070).
The complete revision IDs are stored on `magic.standard.elemental-scaling` in `manifest.json`.
The integration suite now registers the production elemental handlers and executes 35 casts across
every lower and upper tier boundary, asserting the inclusive damage range passed to the combat
engine from the player's live Magic level.

Those same dated spell pages define the cache contract now checked for every elemental spell:
required Magic level, base cast XP, required rune quantities, standard spellbook/combat
classification and the spell's original unscaled cache max hit. Live max-hit calculation then uses
the tier scaling above. Cache-233 `obj.sym` is the authority for object identity; in particular,
chaos rune is object 562, death rune is object 560 and wrath rune is object 21880.

## Standard stat-drain curses

The last spell-page revisions on or before the 233.4 cutoff are Confuse
[`14922747`](https://oldschool.runescape.wiki/w/Confuse?oldid=14922747), Weaken
[`14797450`](https://oldschool.runescape.wiki/w/Weaken?oldid=14797450), Curse
[`14797456`](https://oldschool.runescape.wiki/w/Curse?oldid=14797456), Vulnerability
[`14968444`](https://oldschool.runescape.wiki/w/Vulnerability?oldid=14968444), Enfeeble
[`14964310`](https://oldschool.runescape.wiki/w/Enfeeble?oldid=14964310) and Stun
[`14797453`](https://oldschool.runescape.wiki/w/Stun?oldid=14797453). They establish the affected
stats, 5%/10% values, rune costs, base Magic XP and five-tick casting speed. The cutoff
[Standard spellbook revision 14977565](https://oldschool.runescape.wiki/w/Standard_spellbook?oldid=14977565)
states that the spells do not stack with themselves or similar curses.

The cutoff [Tome of Water revision 14898660](https://oldschool.runescape.wiki/w/Tome_of_Water?oldid=14898660)
establishes +20% curse accuracy, +50% stat-drain effectiveness and one charge consumed for each
qualifying cast. The combat modifiers, positive-charge validation, per-cast consumption and
conversion to object 25576 (`tome_of_water_uncharged`) are implemented and tested through the
item-local lifecycle described below.

## Standard movement-binding spells

The last relevant spell-page revisions inspected on or before the 233.4 cutoff are Bind
[`14862824`](https://oldschool.runescape.wiki/w/Bind?oldid=14862824), Snare
[`14924067`](https://oldschool.runescape.wiki/w/Snare?oldid=14924067) and Entangle
[`14862815`](https://oldschool.runescape.wiki/w/Entangle?oldid=14862815). They establish levels,
rune costs, five-tick speed, 8/16/24-cycle holds, 0/2/5 gameplay max hits, successful-cast base
Magic XP of 31/61/89, splash XP of 30/60/70 and two added binding cycles for each equipped
Swampbark helm/body/legs piece, capped at six.

The cutoff [Stun status revision 14953416](https://oldschool.runescape.wiki/w/Stun_(status)?oldid=14953416)
establishes movement-only restriction, five cycles of immunity after release and PvP release when
caster and target exceed the maximum ten-tile player attack range. The spell pages provide the exact
already-held, immune and fade-warning text. Cache 233 supplies the casting, projectile, impact and
spell sound identities used by the implementation. No named cache synth has yet been tied to the
fade warning, so warning audio and its client timing remain unresolved rather than inferred.

The cutoff Tome of Water source above also gives binding spells a 20% accuracy bonus. The default
NPC enricher applies `freeze_resistance`, and all five Phantom Muspah forms are configured at their
documented 33%. Ancient Ice consumes that percentage; Standard Bind/Snare/Entangle do not because
the dated Muspah source scopes duration reduction to Ice spells. Production-wide NPC values,
immunities and any other consumers remain a separate open inventory.

## Tome of Water charge lifecycle

The cutoff [Tome of Water revision
14898660](https://oldschool.runescape.wiki/w/Tome_of_Water?oldid=14898660) establishes that each
soaked page adds 20 charges, the tome holds 1,000 pages or 20,000 charges, and a charged tome acts
as an unlimited water-rune source. One charge is consumed by each Standard-spellbook combat cast
requiring water runes, including a splash; utility spells and combat spells from other spellbooks
receive the rune source without spending a charge. It also fixes the 10% PvM and 20% PvP accuracy
and damage modifiers for offensive water spells, 20% accuracy for qualifying curses and binds, and
50% stronger stat-drain curses. The cast that spends the final charge still receives its modifier.

Cache 233.4 independently pins charged tome 25574, empty tome 25576 and soaked page 25578. The empty
inventory form exposes `Wield` in slot two and `Pages` in slot four; the charged form additionally
exposes `Check` in slot three. Varbit 16152 (`charges_tome_of_water_quantity`) is permanent state
based on varp 4682 and belongs to the bank auto-charge preference, not to a live tome. Live charges
therefore use dedicated per-object varobj field `tome_of_water_charges` over bits 0–14, which
preserves independent tomes and can represent the 20,000 maximum. Typed object edits supply the
charged/uncharged variant links required by the shared charge manager.

Production now loads as many selected soaked pages as fit, safely changes the same inventory slot
to the charged identity, treats only a charged left-hand tome with a positive item-local count as
an unlimited source, and spends exactly one charge after every successful resource transaction for
a qualifying cast. Rune validation receives the source conditionally, so a zero-charge charged
identity cannot suppress loose-rune use. Cast results snapshot Tome use before depletion, allowing
the last charge to retain water, curse and binding formula effects after the worn object becomes
empty. Ten cache-aware integration scenarios plus affected curse, accuracy and max-hit regressions
cover cap and partial-fill behavior, failure atomicity, Standard utility/combat classification,
other spellbooks, loose-rune preservation, final-charge transformation and formula propagation.

The cutoff page documents that removing pages can lose the partially consumed page, but the exact
Pages interaction, rounding, confirmation and text have not been captured. Exact load, full and
Check messages in the direct core are bounded presentation placeholders. Bank Configure-Charges,
trade/death behavior, audiovisuals, appearance packet order and matching-client confirmation remain
the researched companion group and manual case 82; no Pages-removal handler was guessed.

## Tele Block

The last inspected Tele Block revision on or before the 233.4 cutoff is
[`14900814`](https://oldschool.runescape.wiki/w/Tele_Block?oldid=14900814). Together with the spell
object it establishes level 85, one law/chaos/death rune, 83.6 base Magic XP, 95 XP on success,
five-tick speed, player-only Wilderness use, a 500-cycle block reduced to 250 cycles when Protect
from Magic is active before the spell lands, the landing message and logout/death/Wilderness-exit
cleanup. Protect from Magic changes duration, not accuracy. The page also records cast sound 202,
impact sound 203 and failure sound 227, and blighted teleport-sack substitution in the Wilderness.

The workspace RuneLite API documents cache varbit 4163 (`teleblock_cycles`) as 0 when inactive,
1–100 during reapplication immunity and 101 or more while blocked. Its timer display subtracts 100
from the active value. The server therefore publishes `blocking cycles + 100` and preserves exactly
100 post-block cycles. This is direct local client evidence from
`runelite-api/src/main/java/net/runelite/api/Varbits.java` and the Timers plugin; no external client
checkout was opened or modified.

Cache 233 directly maps casting sequences 1819/1820, impact spotanim 345 with sequence 1822,
failed-travel spotanim 1300 with sequence 1821, and synths 202/203/227. Cache naming supports the
implemented split—success has a delayed target impact with no projectile, while splash sends the
dedicated failed-travel projectile—but that presentation and its bind-projanim-derived travel delay
remain inferred until workspace-client observation. The exact duplicate-target and outside-
Wilderness rejection text currently comes from lower-confidence historical emulation evidence and
must also be captured from a matching production client/server trace.

## Crumble Undead

The last inspected Crumble Undead revision on or before the 233.4 cutoff is
[`14976922`](https://oldschool.runescape.wiki/w/Crumble_Undead?oldid=14976922). It establishes level
39, two air/two earth/one chaos rune, 24.5 base Magic XP, five-tick speed, undead-NPC-only targeting,
a gameplay max hit of 15, and 18 with Salve amulet (ei). It explicitly excludes the Chaos-gauntlet
bonus and supplies the rejection text. It also lists the restricted autocast weapons implemented in
the local autocast object editor: imbued skull sceptre, Slayer's staff variants, Staff of the dead
variants, Staff of light, Staff of balance and Void Knight mace.

Cache 233 identifies undead NPCs through parameter 65434 (`undead`), which the production NPC cache
attribute collector already exposes. The spell contract uses that parameter rather than NPC-name
matching. The cache also supplies unarmed/staff sequences 724/1166, casting/travel/impact spotanims
145/146/147 with sequences 684/685/686, projectile 11 and synths 122/123/124. Its spell-object max
hit value is the generic restricted-spell sentinel 1, so the source-backed gameplay cap is explicit
in the attack implementation and covered separately by tests.

## Magic Dart

The last inspected Magic Dart revision on or before the 233.4 cutoff is
[`14979557`](https://oldschool.runescape.wiki/w/Magic_Dart?oldid=14979557). It establishes level 50
Magic, level 55 Slayer, four mind/one death rune, 30 base Magic XP, five-tick speed and the six
weapon families that can cast and autocast it. Ordinary max hit is
`10 + floor(current Magic / 10)`. The cutoff page also says the enchanted Slayer's staff changes a
successful projectile from green to white while a splash remains green.

The cutoff Slayer's staff (e) revision is
[`14974411`](https://oldschool.runescape.wiki/w/Slayer%27s_staff_%28e%29?oldid=14974411). Against a
current Slayer assignment it changes Magic Dart's base max hit to
`13 + floor(current Magic / 6)`, consumes one of 2,500 charges only when the spell hits, and reverts
to the ordinary staff at zero. Off task, and on a splash, it neither boosts damage nor consumes a
charge. The local max-hit formulas already encode both damage formulas, but production task
resolution currently returns false unconditionally.

Cache 233 supplies sequence 1576 (`slayer_magicdart_cast`), ordinary travel/impact spotanims
328/329, enchanted travel/impact spotanims 1373/1374 and hit synth 174. The implementation uses the
existing generic magic projectile profile; that profile and the absence of a separate named cast
synth remain pending client observation. The spell object uses the generic max-hit sentinel 1, so
live damage is formula-driven rather than read from that field.

## Iban Blast

The last inspected Iban Blast revision on or before the cutoff is
[`14785260`](https://oldschool.runescape.wiki/w/Iban_Blast?oldid=14785260). It establishes level 50,
five fire/one death rune, 30 base Magic XP, max hit 25, five-tick speed, Underground Pass access and
an equipped Iban's staff requirement. An ordinary staff has 120 charges and the upgraded staff has
2,500; NPC casts consume one charge and PvP casts consume two. A charged Tome of Fire may substitute
fire runes without consuming tome charges or adding fire-spell damage because Iban Blast is not an
elemental fire spell. The cutoff staff revisions are
[`14958564`](https://oldschool.runescape.wiki/w/Iban%27s_staff?oldid=14958564) and
[`14984247`](https://oldschool.runescape.wiki/w/Iban%27s_staff_%28u%29?oldid=14984247).

Cache 233 supplies shared player varp 162 (`ibanmulti`), sequence 708, casting/travel/impact
spotanims 87/88/89 with sequences 655/656/657, and projectile profile 8. The combat implementation
therefore keeps charges on the player rather than individual inventory objects and tests sharing
across regular and upgraded staves. It validates staff and runes, then charges, before any resource
or XP mutation; successful attempts consume one charge against an NPC and two against a player.
No named cache synth has yet been tied to this spell, and exact empty/insufficient-charge client
messages remain lower-confidence until workspace RuneLite observation.

## God spells and Charge

The last inspected spell revisions on or before the cutoff are Saradomin Strike
[`14973036`](https://oldschool.runescape.wiki/w/Saradomin_Strike?oldid=14973036), Claws of Guthix
[`14788338`](https://oldschool.runescape.wiki/w/Claws_of_Guthix?oldid=14788338), Flames of Zamorak
[`14880144`](https://oldschool.runescape.wiki/w/Flames_of_Zamorak?oldid=14880144) and Charge
[`14918809`](https://oldschool.runescape.wiki/w/Charge?oldid=14918809). They establish the level,
rune, staff, cape, XP, five-tick and effect contracts used by the implementation: base max hit 20,
max hit 30 during Charge with the matching god cape, one Prayer drained by Saradomin Strike, and
`1 + floor(base level * 5 / 100)` Defence or Magic drained by Claws or Flames without stacking.
The same pages provide the manual and autocast weapon families. Their Mage Arena claim and
100-cast unlock requirements remain separate progression work.

Charge requires level 80, three air/fire/blood runes and a previously claimed god staff. It lasts
700 game ticks, may be recast after 100 ticks, works when the matching cape is equipped after the
cast, and grants 180 Magic XP. Cache varp 272 (`magearena_charge`) stores half the remaining tick
count: the workspace RuneLite `VarPlayer.CHARGE_GOD_SPELL` documentation and Timers and Buffs code
both multiply its value by two. Cache varbit 12356 exposes the one-minute cooldown. Cache varp 267
is named `magearena`, but its packed staff-claim/unlock layout has not been recovered; the current
nonzero prerequisite is therefore an explicit proxy, not verified behavior.

Cache 233 supplies casting sequence 811 and target spotanims 76, 77 and 78 (whose local Guthix
symbol is misspelled `gunthix_claw`) with animation sequences 647, 648 and 901. No dedicated named
synth has been established. Client delay zero/server impact delay one are implementation inferences
pending workspace RuneLite observation. Exact Charge success, cooldown, expiry and claim-failure
messages, plus active/cooldown behavior across logout, also remain unresolved.

The cutoff regular Saradomin max cape revision
[`14774888`](https://oldschool.runescape.wiki/w/Saradomin_max_cape?oldid=14774888) describes it as a
cosmetic upgrade to the Saradomin cape; the cutoff imbued revision
[`14774889`](https://oldschool.runescape.wiki/w/Imbued_Saradomin_max_cape?oldid=14774889) says the same
of the imbued cape. The corresponding Guthix/Zamorak, normal/imbued and locked variants are treated
as their matching god capes for Charge. This family mapping is integration-tested, but should still
be confirmed in the matching client before verification.

## Low and High Level Alchemy

The last inspected revisions on or before the cutoff are Low Level Alchemy
[`14959703`](https://oldschool.runescape.wiki/w/Low_Level_Alchemy?oldid=14959703) and High Level
Alchemy [`14981208`](https://oldschool.runescape.wiki/w/High_Level_Alchemy?oldid=14981208). They
establish level 21/55, three/five fire plus one nature rune, 31/65 Magic XP, 40%/60% of cache item
value, and one target item per cast. They also document Low Alchemy's ordinary three-tick rate,
High Alchemy's five-tick rate, the Alchemist's Playground exception, Fountain of Rune no-rune/no-XP
casts, Explorer's ring free casts, queued casts, warnings, overrides and restricted activities.

Cache 233 supplies sequences 712/713, spotanims 112/113, synths 98/97, parameter 295
(`no_alchemy`), warning varp 1348 and related varbits 6091/6092. `UnpackedObjType.lowAlch` and
`highAlch` independently encode the same 40% and 60% cost formulas. The core implementation uses
those cache contracts and the shared rune manager. The extended exceptions and exact client
ordering remain researched rather than implemented.

## Bones to Bananas and Bones to Peaches

The last inspected revisions on or before the cutoff are Bones to Bananas
[`14948307`](https://oldschool.runescape.wiki/w/Bones_to_Bananas?oldid=14948307) and Bones to Peaches
[`14945410`](https://oldschool.runescape.wiki/w/Bones_to_Peaches?oldid=14945410). They establish
levels 15/60, 25/35.5 XP, one-tick speed, the rune costs, inventory-wide transformation, eligible
normal/burnt/bat/big/wolf/jogre/monkey/bleached/Alan's/animals' bones and the Peaches Mage Training
Arena unlock. Higher bones and demonic ashes are excluded; Peaches documents the exact no-bones and
unsuitable-bone messages. The Creature Graveyard evidence assigns its four animals' bone variants
one through four fruit each.

Cache 233 supplies spell objects 3280/6884, unlock varbit 1505, shared human sequence 722, casting
spotanim 141 and synth 114. The local item symbols identify all eight monkey-bone variants and the
four Graveyard variants used by the implementation. The full-inventory output cap follows the
capacity implied by Graveyard play but its precise client ordering, along with the locked-spell
message and Tombs restriction, remains lower-confidence pending observation.

## Superheat Item

The last inspected Superheat Item revision before the cutoff is
[`14981805`](https://oldschool.runescape.wiki/w/Superheat_Item?oldid=14981805). It establishes level
43, four fire/one nature rune, 53 Magic XP, three-tick speed, ordinary furnace Smithing levels/XP,
100% iron success, steel priority when two coal and level 30 are available, noted-item rejection,
goldsmith-gauntlet/Smithing-cape gold XP, one queued extra cast, catalyst compatibility, elemental
ore failure, the exact invalid/elemental messages and the jogre-bone exception. The cutoff
[Smithing catalyst revision 14784555](https://oldschool.runescape.wiki/w/Smithing_catalyst?oldid=14784555)
defines one catalyst per coal-using bar, half coal and double Smithing XP. The cutoff
[Coal bag revision 14962565](https://oldschool.runescape.wiki/w/Coal_bag?oldid=14962565) states that
held coal is consumed before bag coal.

The ordinary pre-Sailing set is bronze, iron, silver, steel, gold, lovakite, mithril, adamantite and
runite. Later lead and cupronickel recipes are outside cache 233.4. Two quest recipes are separately
present at the cutoff: [Blurite bar revision 14985272](https://oldschool.runescape.wiki/w/Blurite_bar?oldid=14985272)
defines level 13, 8 XP and completion of The Knight's Sword; [Lunar bar revision 14875608](https://oldschool.runescape.wiki/w/Lunar_bar?oldid=14875608)
defines level 1, 6.2 XP and the Lunar Diplomacy equipment phase. Cache quest rows 83 and 88 expose
completion values 7 and 190, while the cache symbol `lunar_quest_main` and Quest Helper source at
commit `633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a` place helmet creation at progress 155. The shared
recipe model therefore gates blurite at `squire >= 7` and lunar at `lunar_quest_main >= 155`.

[Burnt jogre bones revision 14967444](https://oldschool.runescape.wiki/w/Burnt_jogre_bones?oldid=14967444)
adds the unusual Superheat recipe: unboosted level 30 Cooking, 25 Cooking XP and the normal spell's
53 Magic XP. The last pre-cutoff ['perfect' gold bar revision 14995067](https://oldschool.runescape.wiki/w/%27perfect%27_gold_bar?oldid=14995067)
lists a furnace as its only creation facility; unlike the blurite and lunar pages, it has no
Superheat tab. Perfect gold is therefore excluded pending a confirming client trace rather than
silently inferred from ordinary gold.

Cache 233 supplies spell component child 33, success sequence 725, generic/failure sequence 687,
spotanim 148 and synths 190/191. The success/failure split for sequence 687 and synth 191 is inferred
from cache naming. Exact level, material, Cooking and quest rejection text is not established by the
pinned pages, so those messages and audiovisual ordering remain manual-test requirements.

## Ordinary furnace smelting

The final pre-cutoff [Smithing revision 14981495](https://oldschool.runescape.wiki/w/Smithing?oldid=14981495)
and [Furnace revision 14977215](https://oldschool.runescape.wiki/w/Furnace?oldid=14977215)
establish the ordinary furnace interaction: ores may be used on a furnace or selected through its
Smelt operation, with bronze, iron, silver, steel, gold, mithril, adamantite and runite in the
standard eight-choice menu. Cache 233 pins that menu to interface 270 and clientscript 2046 with
action mode 13. Its loc archive contains 28 usable furnaces: 27 expose Smelt as operation 2 and the
large Lovakite furnace loc 28562 exposes it as operation 1. The implementation registers those
exact loc-operation pairs rather than relying on a broad category match.

The ordinary recipes and their level, material and XP contracts are pinned independently by the
last inspected pre-cutoff pages: [bronze 14977288](https://oldschool.runescape.wiki/w/Bronze_bar?oldid=14977288),
[iron 14918534](https://oldschool.runescape.wiki/w/Iron_bar?oldid=14918534),
[silver 14889168](https://oldschool.runescape.wiki/w/Silver_bar?oldid=14889168),
[steel 14781536](https://oldschool.runescape.wiki/w/Steel_bar?oldid=14781536),
[gold 14970295](https://oldschool.runescape.wiki/w/Gold_bar?oldid=14970295),
[lovakite 14734896](https://oldschool.runescape.wiki/w/Lovakite_bar?oldid=14734896),
[mithril 14918531](https://oldschool.runescape.wiki/w/Mithril_bar?oldid=14918531),
[adamantite 14911795](https://oldschool.runescape.wiki/w/Adamantite_bar?oldid=14911795) and
[runite 14935907](https://oldschool.runescape.wiki/w/Runite_bar?oldid=14935907). Blurite and lunar
reuse the quest gates documented for Superheat, producing an eleven-recipe shared registry. Iron
has a 50% ordinary success chance and the exact failure text `The ore is too impure and you fail to
refine it.` The [ring-of-forging revision 14725343](https://oldschool.runescape.wiki/w/Ring_of_forging?oldid=14725343)
defines one account-wide 140-ore counter, Check and destructive Break operations, and melting after
the final guaranteed iron bar.

The cutoff [goldsmith-gauntlets revision 14965070](https://oldschool.runescape.wiki/w/Goldsmith_gauntlets?oldid=14965070)
sets gold-smelting XP to 56.2, including the Smithing and max-cape equivalents. The pinned catalyst
and coal-bag revisions above establish one catalyst per coal-using bar, halved coal, doubled XP and
loose-coal-before-bag ordering. [Varrock armour revision 14982749](https://oldschool.runescape.wiki/w/Varrock_armour_1?oldid=14982749)
establishes a 10% Edgeville-furnace chance to process two complete material sets and award XP for
both bars, capped at steel/mithril/adamantite/all bars by armour tiers 1–4. The
[Shilo furnace contract](https://oldschool.runescape.wiki/w/Furnace?oldid=14977215) charges 20 coins
per bar unless the elite Karamja Diary varbit is complete. Lovakite is accepted only by its hot
furnace; blurite requires The Knight's Sword and lunar ore requires the relevant Lunar Diplomacy
progress.

The cache supplies human furnace sequence 899 and the existing weak-queue/action infrastructure.
Pinned recipe pages give a four-tick first bar for most metals, five ticks for gold and six for
lunar; ordinary repeats are five ticks. Exact success/failure sound, message and interface-close
ordering still require matching-client capture. Silver/special jewellery forging, cannonballs, elemental and
perfect-gold quest products, Blast Furnace behavior and production diary/quest award coupling are
separate feature families, not silently treated as ordinary bars.

## Ordinary gold-jewellery Crafting

The final inspected pre-cutoff [Crafting revision 14996237](https://oldschool.runescape.wiki/w/Crafting?oldid=14996237)
defines ordinary gold jewellery as one gold bar plus the matching mould, with one cut gem for gem
pieces, used at a furnace. It pins 32 ordinary recipes across rings, necklaces, unstrung amulets and
bracelets: gold, sapphire, emerald, ruby, diamond, dragonstone, onyx and zenyte. The implementation
copies the revision's exact Crafting levels, XP and free-to-play/member boundaries. Gold through
diamond rings, necklaces and amulets are free-to-play; all bracelets and all dragonstone/onyx/zenyte
pieces are members-only.

Cache 233 pins the native selection surface to interface 446 (`crafting_gold`). Its ordinary recipe
components are children 8–15, 23–30, 37–44, 50 and 52–58. Their on-load scripts independently pin
the input gem, output object and ring/necklace/amulet/bracelet mould identities. Child 16 is the
special Slayer ring and is deliberately excluded from the ordinary registry. Cache varbit 13892
stores the last jewellery type, but its exact value encoding has not yet been observed. The same
cache supplies generic furnace sequence 899. Static revision-matched RuneLite constants confirm
interface 446 and varbit 13892; RuneLite was not launched.

The server opens interface 446 and protects all 32 ordinary product components with a validated
1–28 PauseButton quantity range. It performs one atomic bar/gem-to-product transaction every three
ticks, leaves moulds in place, awards exact Crafting XP, stops when the player moves or inputs
change, and shares queue cancellation with ordinary smelting and cannonballs. The current Shilo
policy charges 20 coins per produced piece unless the elite Karamja Diary is complete, while
Lovakite furnaces reject this family. The three-tick cadence and specialized-interface quantity
payload are strongly supported but still require a matching cache-233 client capture before they
can be called client-verified.

Slayer rings, eternal Slayer rings, remembered-interface selection, alternative gem/mould use-on
entry points, exact messages and audiovisual/inventory ordering, diary credit and exact Shilo
presentation remain separate researched work. Gold tiaras are absent from this cache-233 gold
interface and are not silently included based on post-cutoff Wiki state.

## Ordinary silver-jewellery Crafting

The same final inspected pre-cutoff [Crafting revision 14996237](https://oldschool.runescape.wiki/w/Crafting?oldid=14996237)
and [silver-bar revision 14889168](https://oldschool.runescape.wiki/w/Silver_bar?oldid=14889168)
pin the 12 ordinary opal, jade and red-topaz jewellery recipes plus five non-quest silver products.
Each jewellery piece consumes one silver bar, its matching jewellery mould and one cut gem. The
non-quest set is an unstrung holy symbol, unstrung unholy emblem, silver sickle, ten unfinished
silver bolts and a tiara, each consuming one bar and its unique mould. Exact levels/XP range from
opal ring at 1/10 XP through topaz amulet at 45/80 XP; the five miscellaneous products use
16/50, 17/50, 18/50, 21/50 and 23/52.5 respectively. The holy symbol and tiara are free-to-play;
the other 15 recipes are members-only.

Cache 233 pins this selection surface to legacy interface 6 (`silver_crafting`). Its ordinary
selectable components are children 7–9, 11–13, 15–17, 19–21, 23–25, 27 and 28. Children 26, 29 and
30 are the Creature of Fenkenstrain conductor, In Aid of the Myreque Silvthrill rod and Shadow of
the Storm demonic sigil. Cache varbit 13893 names the last silver-crafting selection. The official
[Smithing and Silver Crafting Interfaces update](https://oldschool.runescape.wiki/w/Update:Smithing_and_Silver_Crafting_Interfaces)
confirms Make-1/5/10/X/All selection and unique mould labelling on this interface.

The cache's `[clientscript,silver_crafting_init]` has 25 integer arguments and supplies all 20
product, level and mould triples, including `4201/20/4200`, `7637/25/7649` and `6748/30/6747` for
the three special products. Its operands contain none of quest varbits 13715, 1990 or 1372. The
pinned historical Quest Helper implementations place conductor crafting in
[Creature of Fenkenstrain state 3](https://github.com/Zoinkwiz/quest-helper/blob/633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a/src/main/java/com/questhelper/helpers/quests/creatureoffenkenstrain/CreatureOfFenkenstrain.java)
and Silvthrill crafting in
[In Aid states 375–410](https://github.com/Zoinkwiz/quest-helper/blob/633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a/src/main/java/com/questhelper/helpers/quests/inaidofthemyreque/InAidOfTheMyreque.java)
based on possession of the mould.
[Shadow of the Storm states 40–60](https://github.com/Zoinkwiz/quest-helper/blob/633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a/src/main/java/com/questhelper/helpers/quests/shadowofthestorm/ShadowOfTheStorm.java)
likewise offer sigil smelting when the mould is held and explicitly document making replacement
sigils from that mould. This establishes a possession and material gate for the production action;
acquiring or replacing each mould remains part of its own quest implementation rather than a
hidden Crafting gate.

The implementation protects all 20 product components with a validated 1–28 PauseButton range and
processes one atomic recipe every three ticks. It preserves moulds, produces ten
unfinished bolts per bar, awards exact Crafting XP, uses sequence 899, shares furnace queue
cancellation, applies the current Shilo fee/elite policy and rejects Lovakite furnaces. Exact
quantity packet encoding, last-selection values, messages, audiovisual order and Shilo presentation
still require matching-client capture.

The conductor is documented at level 20/50 XP; the Silvthrill rod at 25/55 XP from one silver bar,
one mithril bar and one sapphire; and the demonic sigil at 30/50 XP. All three are members-only,
consume one unit of each listed material, preserve their mould and support the interface's normal
quantity batches. The atomic multi-material and repeat behavior is integration-tested. Exact
quest-side mould visibility/acquisition presentation and client-observed failure precedence remain
in the extended family.

## Gem cutting and manual amulet stringing

The final inspected pre-cutoff
[Crafting revision 14996237](https://oldschool.runescape.wiki/w/Crafting?oldid=14996237) defines ten
chisel recipes. Opal, jade and red topaz require levels 1, 13 and 16 and award 15, 20 and 25 XP on
success or 3.8, 5 and 6.3 XP when crushed. Sapphire, emerald, ruby, diamond, dragonstone, onyx and
zenyte require levels 20, 27, 34, 43, 55, 67 and 89 and award 50, 67.5, 85, 107.5, 137.5, 167.5
and 50 XP. The 50-XP zenyte value is the production value after the official 27 November 2024
reduction from 200 XP, so it applies to cache 233. Opal, jade, red topaz, dragonstone, onyx and
zenyte are members-only; sapphire through diamond are free-to-play.

The pinned Wiki probability table gives the exact success rates at each Crafting level. These map
without approximation to rsmod's existing 1/256 interpolation as low/high pairs 128/250 for opal,
100/245 for jade and 90/240 for red topaz. For example, this yields 129/256 at level 1 and 251/256
at level 99 for opal, 119/256 at level 13 and 246/256 at 99 for jade, and 114/256 at level 16 and
241/256 at 99 for red topaz. Sapphire and rarer gems cannot be crushed.

Cache 233 resolves the complete uncut/cut object family, crushed gemstone 1633 and ten distinct
sequences: dragonstone 885, diamond 886, ruby 887, sapphire 888, emerald 889, opal 890, jade 891,
red topaz 892, onyx 2717 and zenyte 7185. Dragonstone and onyx use their own sequences while the
remaining precious cuts are likewise explicitly named; no animation was guessed. Static reads of
the workspace RuneLite checkout independently label the seven legacy `GEM_CUTTING_*` constants.
RuneLite was not opened.

The [Crafting experience table](https://oldschool.runescape.wiki/w/Crafting/Experience_table),
[amulet overview](https://oldschool.runescape.wiki/w/Amulet) and
[free-to-play training description](https://oldschool.runescape.wiki/w/Free-to-play_Crafting_training)
establish manual stringing by combining one ball of wool with an unstrung item. Ordinary metal and
gem amulets, unblessed and unpowered symbols, and the M'speak amulet award 4 Crafting XP; each action
takes two ticks and requires no meaningful level beyond the universal level 1. The
[Salve amulet recipe](https://oldschool.runescape.wiki/w/Salve_amulet) is an important exception:
manual wool stringing takes two ticks but awards 0 XP. Item possession is sufficient at the
production step for both quest items; their acquisition remains owned by the respective quest
content.

The implementation registers both item-use orders through the native held-item interaction API,
performs atomic inventory transformations and automatically continues every two ticks while the
same inputs remain. It covers all ten gem cuts and 15 wool transforms: eleven ordinary amulets,
both silver symbols, M'speak and Salve. Starting either handcraft action replaces the other batch.
Exact sounds, message wording and audiovisual/inventory ordering are not encoded by the recipe
sources and remain matching-client work.

## Glassblowing

The final inspected pre-cutoff
[Crafting revision 14996237](https://oldschool.runescape.wiki/w/Crafting?oldid=14996237) and the
[Crafting experience table](https://oldschool.runescape.wiki/w/Crafting/Experience_table) define
the complete ordinary glassblowing table. One molten glass becomes a beer glass at level 1 for
17.5 XP, empty candle lantern at 4 for 19 XP, empty oil lamp at 12 for 25 XP, vial at 33 for 35 XP,
empty fishbowl at 42 for 42.5 XP, unpowered orb at 46 for 52.5 XP, lantern lens at 49 for 55 XP, or
empty light orb at 87 for 70 XP. Every recipe is members-only and preserves the glassblowing pipe.
The Wiki's game-tick reference fixes glassblowing at three ticks per item.

Cache 233 independently resolves the input/tool and eight outputs as object IDs 1775, 1785, 1919,
4527, 4525, 229, 6667, 567, 4542 and 10980. The player sequence is the explicitly named
`human_glassblowing` sequence 884. Selection uses generic interface 270 and clientscript 2046 with
the products in ascending-level order, a maximum bounded by molten-glass count and the cache's
normal Make action mode. Both item-use orders route through the held-item interaction API.

The implementation uses one atomic inventory transaction per item, awards fine XP exactly, repeats
every three ticks, stops if either tool or material disappears and shares cancellation with gem
cutting and manual stringing through a Crafting-common queue registry. Seven integration tests pin
the recipe/cache table, interface payload/events, both-order dispatch, batch cadence, resource and
level failures, atomicity and cross-family cancellation.

The pinned [Molten glass](https://oldschool.runescape.wiki/w/Molten_glass) creation table defines
members-only level-1 furnace production: one soda ash and one bucket of sand become one molten
glass and one returned empty bucket for 20 Crafting XP every two ticks. Cache 233 resolves those
objects as 1781, 1783, 1775 and 1925 and the furnace animation as sequence 899. The implementation
registers both ingredient-on-furnace paths across the shared furnace registry, uses the native
one-product quantity chooser, performs atomic batches, stops on movement or missing materials and
mutually cancels furnace and hand-Crafting queues.

The pinned Crafting table defines empty-oil-lantern assembly at level 26 for 50 XP from object 4525
and frame 4540 into object 4535. The [Light orb](https://oldschool.runescape.wiki/w/Light_orb)
creation table defines an immediate level-87 transform of empty orb 10980 and cave goblin wire
10981 into light orb 10973 for 104 XP. Both item orders are registered and both transforms are
atomic. Four additional integration tests pin the extended cache identities, furnace interface and
two-tick cadence, bucket return, XP, movement/material stops, queue policy, both-order dispatch and
level failure. Exact messages, sounds, prior selection, quest/diary coupling and matching-client
audiovisual/inventory ordering remain open.

## Spinning

The final inspected pre-cutoff
[Crafting revision 14996237](https://oldschool.runescape.wiki/w/Crafting?oldid=14996237) defines the
cache-233 spinning table: wool becomes a ball of wool at level 1 for 2.5 XP and is free-to-play;
flax becomes bow string at level 10 for 15 XP; sinew or ordinary tree roots become crossbow string
at level 10 for 15 XP; magic roots can instead become magic string at level 19 for 30 XP; and yak
hair becomes rope at level 30 for 25 XP. The latter four products are members-only. The bow-string
creation table fixes ordinary spinning at three ticks per item.

Cache 233 independently resolves the five outputs as 1759, 1777, 9438, 6038 and 954; inputs as
1737, 1779, 9436, roots 6043/6045/6047/6049/6051 and yak hair 10814; wheel animation 894; and eleven
usable named wheel identities. The implementation exposes the outputs in source order through
interface 270/clientscript 2046, supports wheel operation and every input-on-wheel path, preserves
the chosen material across an atomic batch, repeats every three ticks, stops on movement or missing
material and shares Crafting queue cancellation.

Six integration tests pin the recipe and cache table, quantity payload/events, exact transforms and
XP, every sinew/root alternative, cadence, movement stop, membership/level/material failures and
cross-family cancellation. Seers' Village diary acceleration, exact text/audio, mixed-material
priority, diary/task credit and matching-client ordering remain open. Linen, hemp and cotton
products are not present in cache 233 and must be introduced only by a later cache delta.

## Pottery

The final inspected pre-cutoff
[Crafting revision 14996237](https://oldschool.runescape.wiki/w/Crafting?oldid=14996237) defines the
complete cache-233 pottery family. One soft clay shapes into a pot at level 1 for 6.3 XP, four
unfired cups at level 3 for 8.5 XP, a pie dish at level 7 for 15 XP, a bowl at level 8 for 18 XP,
a plant pot at level 19 for 20 XP, or a pot lid at level 25 for 20 XP. Cups, plant pots and pot lids
are members-only. Firing has no item-level gate and awards 6.3/8.5/10/15/17.5/20 XP on success.
The [Unfired bowl](https://oldschool.runescape.wiki/w/Unfired_bowl) creation table fixes shaping at
three ticks, while the [Empty plant pot](https://oldschool.runescape.wiki/w/Empty_plant_pot)
creation and probability tables fix firing at seven ticks and give the exact level-1-through-13
success numerators: 181, 187, 194, 200, 206, 213, 219, 225, 232, 238, 244, 251 and 256 out of 256.

Cache 233 independently resolves soft clay and all unfired/fired outputs as objects 1761, 1787,
28193, 1789, 1791, 5352, 4438, 1931, 1980, 2313, 1923, 5350 and 4440; wheel sequence 883; oven
sequence 1317; seven usable named wheels; and four ordinary ovens. Selection uses interface 270 and
clientscript 2046 in ascending-level source order. Both direct location operation and material-on-
location paths are registered. Shaping performs one atomic transform every three ticks, including
the cup recipe's net three-slot expansion; firing consumes one unfired item every seven ticks,
awards XP only on success and safely destroys cracked pottery.

The Wiki identifies pot-lid making as a partial One Small Favour unlock. Cache varp 416 is the
quest's main progress variable, and Quest Helper's state map places the return-to-Tassie teaching
step at 230 and pot-lid production at 235. The implementation therefore requires state 235 rather
than merely a started quest. Eight integration tests pin the complete recipe/cache contract, native
menu payload, XP and four-cup transform, level/membership/space/quest gates, three/seven-tick
cadence, movement cancellation, all 13 success thresholds and no firing-level requirement. Exact
text/audio, oven-glow transitions, diary hooks and matching-client ordering remain open.

## Ordinary leatherworking

The final inspected pre-cutoff
[Crafting revision 14996237](https://oldschool.runescape.wiki/w/Crafting?oldid=14996237) and
[Leather revision 14893148](https://oldschool.runescape.wiki/w/Leather?oldid=14893148) define the
ordinary soft-leather table: gloves, boots, cowl, vambraces, body, chaps and coif at levels
1/7/9/11/14/18/38 for 13.8/16.2/18.5/22/25/27/37 XP. One hard leather makes a hardleather body at
level 28 for 35 XP. All eight cow-leather recipes are free-to-play. The same Crafting revision
defines the members-only dragon-leather table: green vambraces/chaps/body at levels 57/60/63 for
62/124/186 XP, blue at 66/68/71 for 70/140/210 XP, red at 73/75/77 for 78/156/234 XP and black at
79/82/84 for 86/172/258 XP. Vambraces, chaps and bodies consume one, two and three matching
leathers respectively.

The leather source states that a spool is consumed on every fifth armour piece rather than per
hide, and the dated Crafting table describes one spool as making up to five pieces. The
[Death on the Isle release](https://oldschool.runescape.wiki/w/Death_on_the_Isle) establishes the
cache-era costume needle as a post-quest reward that replaces both an ordinary needle and thread.
The implementation consequently preserves a per-player ordinary-thread use counter across batch
replacement during the session, consumes exactly on the fifth completed action and leaves that
counter untouched while a costume needle is used. Durable restoration of a partial spool counter
across logout still lacks an authoritative persistence contract and remains open.

Cache 233 independently resolves ordinary needle/thread as 1733/1734, costume needle as 29920,
five leather inputs as 1741/1743/1745/2505/2507/2509, the 20 outputs as
1059/1061/1167/1063/1129/1095/1169/1131, 1065/1099/1135, 2487/2493/2499,
2489/2495/2501 and 2491/2497/2503, and the leather-crafting sequence as 1249. Selection uses
interface 270/clientscript 2046 in ascending-level order for the chosen leather type. Either tool-
material order is accepted, each result is an atomic three-tick action and all material/tool/level/
membership checks happen before mutation. Nine integration tests pin the cache and recipe tables,
menu payload, every output and XP value, fifth-action thread consumption across separate batches,
costume-needle behavior, atomic failure gates, cadence, material stop and Crafting queue replacement.
The remaining specialist families, exact presentation and matching-client ordering remain open.

## Specialist snakeskin and yak-hide leatherworking

The final inspected pre-cutoff recipe revisions for
[snakeskin boots 14985532](https://oldschool.runescape.wiki/w/Snakeskin_boots?oldid=14985532),
[vambraces 14875111](https://oldschool.runescape.wiki/w/Snakeskin_vambraces?oldid=14875111),
[bandana 14896471](https://oldschool.runescape.wiki/w/Snakeskin_bandana?oldid=14896471),
[chaps 14983500](https://oldschool.runescape.wiki/w/Snakeskin_chaps?oldid=14983500) and
[body 14875113](https://oldschool.runescape.wiki/w/Snakeskin_body?oldid=14875113) define five
members-only, boostable recipes. They require Crafting levels 45/47/48/51/53, consume
6/8/5/12/15 ordinary snakeskins and award 30/35/45/50/55 XP. Every page specifies three ticks and
lists one thread as an ordinary-needle material. This whole-thread contract is intentionally
distinct from the one-spool-per-five-results behavior documented for ordinary cow and dragon
leather. Each page also provides a costume-needle recipe with the same hide, level, XP and timing
but no thread input.

The corresponding pre-cutoff
[yak legs revision 14899211](https://oldschool.runescape.wiki/w/Yak-hide_armour_%28legs%29?oldid=14899211)
and
[yak top revision 14924142](https://oldschool.runescape.wiki/w/Yak-hide_armour_%28top%29?oldid=14924142)
define members-only, boostable level-43 and level-46 recipes. One cured hide makes the legs and
two make the top; both award 32 XP and take three ticks. Their ordinary-needle recipes consume one
thread per result, while the costume-needle variants consume none. Both pages require partial
completion of The Fremennik Isles. The final pre-cutoff
[quest revision 14976250](https://oldschool.runescape.wiki/w/The_Fremennik_Isles?oldid=14976250)
places crafting the set in the traditional-rites stage. The cache names varbit 3311 `fris_quest`,
and the Wiki's
[varbit record 14012125](https://oldschool.runescape.wiki/w/RuneScape:Varbit/3311?oldid=14012125)
identifies it as the quest's main progress value. The pre-cutoff Quest Helper implementation at
[commit 241eaec2](https://github.com/Zoinkwiz/quest-helper/blob/241eaec29b19243bda7e88e99d5c16568c0776a6/src/main/java/com/questhelper/helpers/quests/thefremennikisles/TheFremennikIsles.java)
maps state 275 to the final dialogue before armour, state 280 to obtaining the full yak set and
state 290 to the following shield stage. The implementation therefore admits yak armour from state
280 onward, including completed-quest states, and rejects state 279.

Cache 233 resolves ordinary needle/thread and costume needle as 1733/1734/29920, snakeskin as 6289,
the five outputs as 6328/6330/6326/6324/6322, cured yak hide as 10820 and yak legs/top as
10824/10822. Both families use interface 270, clientscript 2046 and leather-crafting sequence 1249.
The server presents ascending-level per-material choices, caps ordinary-needle batches by both hide
and whole-thread supply, accepts either held-item order and executes each result as an atomic
three-tick action. Twelve integration tests pin every cache and recipe identity, both menu payloads,
all transforms and XP values, whole-thread exhaustion, costume-needle substitution, the exact quest
boundary, members/level/tool/material gates, full-inventory behavior, cadence, interruption,
cross-Crafting cancellation and reverse-order dispatch. Exact production menu order, messages,
sound, audiovisual/inventory ordering, activity restrictions and diary/League-task hooks remain
open, as do Hunter pouches, fur-tailoring services and further quest-textile families.

## Xerician robes

The final inspected pre-cutoff revisions for
[Xerician fabric 14908288](https://oldschool.runescape.wiki/w/Xerician_fabric?oldid=14908288),
[Xerician hat 14935104](https://oldschool.runescape.wiki/w/Xerician_hat?oldid=14935104),
[Xerician robe 14806071](https://oldschool.runescape.wiki/w/Xerician_robe?oldid=14806071) and
[Xerician top 14806069](https://oldschool.runescape.wiki/w/Xerician_top?oldid=14806069) define three
members-only, boostable recipes. Hat, robe and top require Crafting levels 14, 17 and 22, consume
three, four and five Xerician fabrics, award 66, 88 and 110 XP and each take three ticks. Every
ordinary-needle recipe consumes one whole thread per result; the costume-needle alternative uses
the same fabric, level, XP and timing without thread.

Cache 233 resolves Xerician fabric/hat/top/robe as 13383/13385/13387/13389 and shares ordinary
needle/thread 1733/1734, costume needle 29920, interface 270, clientscript 2046 and leather-crafting
sequence 1249 with the existing specialist flow. The server presents hat, robe and top in
ascending-level order, caps batches by fabric and whole-thread supply, accepts either held-item
order and completes each result atomically after three ticks. The expanded thirteen-test
specialist suite pins the cache and exact recipe table, the native three-product menu, a multi-item
costume-needle batch, all transforms and XP values, tool/resource boundaries, gates, interruption
and reverse-order dispatch. Exact production messages, sound and audiovisual/inventory ordering
remain unverified.

## Hunter fur and meat pouch crafting

The final inspected pre-cutoff revisions for
[small fur pouch 14863238](https://oldschool.runescape.wiki/w/Small_fur_pouch?oldid=14863238),
[medium fur pouch 14863239](https://oldschool.runescape.wiki/w/Medium_fur_pouch?oldid=14863239),
[large fur pouch 14863240](https://oldschool.runescape.wiki/w/Large_fur_pouch?oldid=14863240),
[small meat pouch 14915337](https://oldschool.runescape.wiki/w/Small_meat_pouch?oldid=14915337) and
[large meat pouch 14939423](https://oldschool.runescape.wiki/w/Large_meat_pouch?oldid=14939423)
define all five cache-era pouch recipes. Small fur and meat pouches require level 35, four larupia
or fox furs and award 20 or 30 XP. Medium fur requires level 50, a closed small fur pouch and three
graahk furs for 40 XP. Large fur requires level 65, a closed medium fur pouch and three kyatt furs
for 60 XP. Large meat requires level 65, a specifically closed small meat pouch and three sunlight
antelope furs for 60 XP. Every recipe is members-only, boostable and three ticks. Each dated recipe
lists ordinary thread quantity 0.2 or a costume-needle alternative with no thread, so the
implementation shares the established five-action spool lifecycle across ordinary leather, Lunar
clothing and these pouches.

Cache 233 resolves larupia/graahk/kyatt fur as 10095/10099/10103, fox and sunlight-antelope fur as
29163/29177, closed small/large meat pouches as 29295/29297 and closed small/medium/large fur
pouches as 29299/29301/29303. Their open counterparts are 29462/29464 and 29466/29468/29470.
Creation accepts only the required closed input. The server uses the shared leather-crafting
sequence 1249 and native interface 270/clientscript 2046, performs an atomic transaction every
three ticks, caps quantities by fur, base pouch and remaining spool capacity, accepts either held
item order and rechecks every tool/material gate before mutation. Nine integration tests pin all
cache and recipe identities, the one-product menu, exact transforms/timing/XP, cross-family thread
progress, costume-needle batches, closed-state requirement, membership/visible-level/material
gates, full-inventory behavior, interruption, queue replacement and reverse-order dispatch.

The same dated revisions define tier capacities 14/21/28, Open/Close, Fill, Empty, Check and
Destroy, family-wide shared capacity and automatic routing while an appropriate pouch is open.
The permanent fur store accepts larupia, graahk, kyatt, polar kebbit, common kebbit, Feldip weasel,
desert devil, dark kebbit, spotted kebbit, dashing kebbit, fox, sunlight antelope, moonlight
antelope and jaguar fur, while deliberately rejecting tatty outfit fur. The permanent meat store
accepts wild-kebbit, larupia, barb-tailed-kebbit, graahk, kyatt, pyre-fox, dashing-kebbit, sunlight-
antelope and moonlight-antelope meat and deliberately rejects raw bird meat. Multiple pouches do
not multiply capacity; the held tier caps the family store, upgrades retain shared contents and
destroying any family pouch clears them.

Cache 233 has all ten closed/open objects and the server supplies two synthetic size-28 permanent
inventories for the family stores. Every pouch exposes its cache-defined Fill, Empty, Check,
Open/Close and Destroy operations; the cache's bank-side Extra Empty parameter is restored. Bank
interface Empty transfers only the selected family into the permanent bank, while ordinary Empty
currently returns the dated restriction message. `storeAcquired` is the tested Hunter-catch hook:
it selects the largest open tier, enforces the family cap and leaves excluded or overflow objects
to the caller. Six additional integration tests exercise exact cache operations, inventories,
allowlists and tier caps, duplicate-pouch sharing, open/close preservation, automatic routing,
the real bank-side event and real destroy event. Wiring the hook into future Hunter catch plugins,
ordinary Empty at qualifying bank/Hunter Guild areas, deposit-box routing and matching-client
message/audio/operation ordering remain open.

The crafting menu title, exact production failure/success text, sound and audiovisual/inventory
ordering remain implementation inferences pending matching-client capture.

## Asyff Custom Fur Clothing

The final inspected pre-cutoff
[Fancy Clothes Store revision 14991675](https://oldschool.runescape.wiki/w/Fancy_Clothes_Store?oldid=14991675)
defines all 20 custom-fur products and exact fees. Each polar, common, Feldip and desert camouflage
top or pair of legs costs two matching kebbit/weasel/devil furs and 20 coins. Larupia, graahk and
kyatt headwear costs one perfect fur plus 500/750/1000 coins; their tops and legs accept one perfect
or tatty fur plus 100/150/200 coins. Gloves of silence, spotted capes and spottier capes cost two
dark, spotted or dashing kebbit furs plus 600/400/800 coins. The same dated page records the
15 May 2024 change that Asyff prioritises tatty fur when making Hunter outfits.

The final inspected pre-cutoff
[Asyff transcript revision 14846450](https://oldschool.runescape.wiki/w/Transcript:Fancy_dress_shop_owner?oldid=14846450)
provides his standard introduction, hunting branch, unusual-material branch and exact non-members
response. Cache 233 resolves Asyff as NPC 2887 with Talk-to, Trade and Fur Clothing in operation
slots 1, 3 and 4; the Fancy Clothes Store and custom-product inventories as 63 and 482; and Custom
Fur Clothing as interface 477 with dynamic product parent component 6. Client script 732 supplies
the `Custom Fur Clothing` title, three dynamic product rows (slots 0–7, 8–16 and 17–19), native
right-click/long-press guidance and client scripts 2848/2414 for the store-style product controls.

Nine integration tests pin all 20 output slots, exact materials/fees, cache interface/inventory/NPC
identities, members gating, immediate full-inventory atomic transforms, tatty-first body/leg
selection, perfect-only headwear, missing-resource behavior, displayed-slot/object spoof rejection,
stale-action invalidation and Asyff's Trade/Fur Clothing/non-members routes. Exact production menu
operations, success/failure text, activity restrictions, sound and packet/inventory ordering still
require matching-client capture. The Biohazard Priest-gown and Eagle's Peak bird-costume branches
are conditional quest content and remain explicitly open.

## Hydra leather and ferocious gloves

The final inspected pre-cutoff revisions for
[ferocious gloves 14890854](https://oldschool.runescape.wiki/w/Ferocious_gloves?oldid=14890854),
[hydra leather 14800128](https://oldschool.runescape.wiki/w/Hydra_leather?oldid=14800128) and
[Machinery 14792901](https://oldschool.runescape.wiki/w/Machinery?oldid=14792901) establish a
members-only, post-Dragon Slayer II conversion at the Lithkren Vault machine. One hydra leather
makes one untradeable pair of ferocious gloves; the process can be reversed one-for-one, has no
Crafting level or XP requirement and takes seven ticks. A hammer is required.

The final inspected pre-cutoff
[Machinery transcript 14932214](https://oldschool.runescape.wiki/w/Transcript:Machinery?oldid=14932214)
provides the exact leather Inspect text, missing-hammer text, forward and reverse confirmations and
both result messages. Cache 233 resolves hydra leather/ferocious gloves as 22983/22981, the complete
machine as loc 33609 (`ds2_lithkren_vault_machine01_complete`), Dragon Slayer II progress as varbit
6104 (`ds2`) and the ordinary/Imcando hammer-hit sequences as 1755/8915. Client scripts 4024 and
4029 show that quest 32 reads varbit 6104 and treats a quest as complete when its current value is
at least the end value; quest DB row 32 column 19 supplies end value 215. The pre-cutoff Quest
Helper map at
[commit 241eaec2](https://github.com/Zoinkwiz/quest-helper/blob/241eaec29b19243bda7e88e99d5c16568c0776a6/src/main/java/com/questhelper/helpers/quests/dragonslayerii/DragonSlayerII.java)
independently places its final quest dialogue immediately below that completed boundary.

The implementation registers both item-on-machine directions, the cache's direct `Investigate`
operation and hydra leather's `Inspect` operation. It confirms before starting, clears competing
Crafting queues, rechecks location/input/tool/quest state after seven ticks and performs an atomic
one-for-one inventory transaction that also succeeds with 28 occupied slots. Ten integration tests
pin the cache and DB-row contracts, exact transcript text, both transformations, timing, no-XP
behavior, decline path, members/quest/hammer gates, ordinary and both Imcando hammer forms from
inventory/equipment, full-inventory safety, interruption and direct loc dispatch. Sequence choice,
Imcando applicability to this machine, direct-input priority and non-transcript failure messages are
evidence-based implementation inferences pending matching-client capture.

## Lunar Diplomacy Suqah-leather clothing

The final inspected pre-cutoff revisions for
[Suqah leather 14424498](https://oldschool.runescape.wiki/w/Suqah_leather?oldid=14424498),
[Lunar torso 14805286](https://oldschool.runescape.wiki/w/Lunar_torso?oldid=14805286),
[Lunar legs 14805287](https://oldschool.runescape.wiki/w/Lunar_legs?oldid=14805287),
[Lunar gloves 14805288](https://oldschool.runescape.wiki/w/Lunar_gloves?oldid=14805288) and
[Lunar boots 14805289](https://oldschool.runescape.wiki/w/Lunar_boots?oldid=14805289) define four
members-only, single-leather recipes available while making the ceremonial outfit in Lunar
Diplomacy. The torso, trousers, gloves and boots require visible Crafting levels 61, 59, 52 and 52,
award 35, 30, 25 and 25 XP, and each takes three ticks. An ordinary needle and thread work; the
costume needle supplies its own thread.

The final pre-cutoff
[Suqah leather transcript 14351435](https://oldschool.runescape.wiki/w/Transcript:Suqah_leather?oldid=14351435)
supplies the exact `Which item would you like to make?` title, torso/trousers/gloves/boots/Cancel
option order, all four visible-level failures, all four result messages and the exact rejection
after quest completion. The dated
[Lunar Diplomacy revision 14983868](https://oldschool.runescape.wiki/w/Lunar_Diplomacy?oldid=14983868)
confirms four hides, a needle and two thread are collected for the clothing stage. Ordinary
leatherworking consumes a spool every fifth completed action; requiring two spools in the quest is
safe for a player who may already have a partially used spool. The implementation therefore shares
one five-action thread-use repository between ordinary and Suqah leather rather than consuming a
whole spool per garment.

Cache 233 resolves Suqah leather and the four outputs as 9081 and 9097–9100, the quest progress as
varbit 2448 (`lunar_quest_main`) and leather crafting as sequence 1249. Quest DB row 88 column 19
sets completion to state 190. The pre-cutoff Quest Helper map at
[commit 241eaec2](https://github.com/Zoinkwiz/quest-helper/blob/241eaec29b19243bda7e88e99d5c16568c0776a6/src/main/java/com/questhelper/helpers/quests/lunardiplomacy/LunarDiplomacy.java)
places acquisition of the complete ceremonial outfit at state 155 and entry into the dream at
state 160. The implementation admits clothing from 155 through 189 and applies the transcript's
post-quest rejection from 190 onward.

Ten integration tests pin cache objects, sequence, quest varbit and DB completion; the recipe and
transcript contracts; exact three-tick completion and XP; visible-level boundaries; both quest
edges; costume-needle behavior; full-inventory replacement; the shared ordinary-thread lifecycle;
reverse-order dispatch; and atomic tool/material/quest interruption. The pre-155 gate and its text,
members/tool/material failure text, animation/sound/inventory ordering, logout persistence and the
broader interruption matrix remain evidence-based implementation choices pending matching-client
capture.

## Leather add-on Crafting

The final pre-cutoff Wiki revisions for
[studded body 14889170](https://oldschool.runescape.wiki/w/Studded_body?oldid=14889170) and
[studded chaps 14889171](https://oldschool.runescape.wiki/w/Studded_chaps?oldid=14889171) define
members-only, boosted Crafting requirements of 41 and 44. One steel studs object combines with one
leather body or chaps in three ticks to produce the studded form for 40 or 42 XP. The corresponding
cache identities are inputs 1129/1095/2370 and outputs 1133/1097.

The final pre-cutoff recipe revisions for
[leather 14792977](https://oldschool.runescape.wiki/w/Spiky_vambraces?oldid=14792977),
[green 14792976](https://oldschool.runescape.wiki/w/Green_spiky_vambraces?oldid=14792976),
[blue 14792975](https://oldschool.runescape.wiki/w/Blue_spiky_vambraces?oldid=14792975),
[red 14792974](https://oldschool.runescape.wiki/w/Red_spiky_vambraces?oldid=14792974) and
[black 14792971](https://oldschool.runescape.wiki/w/Black_spiky_vambraces?oldid=14792971)
each define one kebbit claw plus the matching vambraces, boosted Crafting level 32, members-only
access, 6 XP and three ticks. Cache 233 resolves the five bases as 1063/1065/2487/2489/2491, claw
10113 and outputs 10077/10079/10081/10083/10085. The dated Crafting overview table shows 5.5 XP in
one spiky-vambrace cell but its adjacent prose and all five recipe-specific revisions say 6 XP. The
implementation follows the more specific unanimous recipe evidence; a matching-client XP capture
remains required before this conflict can be closed as verified.

All seven item pairs accept either use-on order and schedule a single result after three ticks.
They share cache/RuneLite-named leather-crafting sequence 1249, although direct production capture
is still required to prove that presentation choice. Membership/current boosted level/materials are
preflighted, the two-to-one inventory mutation and XP award are atomic even from a full inventory,
and movement, material loss or another Crafting action cancels safely. Seven integration tests pin
the entire recipe table, exact completion boundary, one-action behavior, failure gates, full-
inventory replacement, cancellation and actual reverse-order held-item dispatch. Exact messages,
sound and audiovisual/inventory ordering remain open.

## Leather-shield Crafting

The final inspected pre-cutoff recipe revisions are
[hard leather shield 14809959](https://oldschool.runescape.wiki/w/Hard_leather_shield?oldid=14809959),
[snakeskin shield 14540789](https://oldschool.runescape.wiki/w/Snakeskin_shield?oldid=14540789),
[green d'hide shield 14542508](https://oldschool.runescape.wiki/w/Green_d%27hide_shield?oldid=14542508),
[blue d'hide shield 14684207](https://oldschool.runescape.wiki/w/Blue_d%27hide_shield?oldid=14684207),
[red d'hide shield 14643423](https://oldschool.runescape.wiki/w/Red_d%27hide_shield?oldid=14643423)
and
[black d'hide shield 14830208](https://oldschool.runescape.wiki/w/Black_d%27hide_shield?oldid=14830208).
They define six members-only, boostable Crafting recipes at levels 41/56/62/69/76/83. Every result
requires two pieces of the corresponding hard leather, snakeskin or dragon leather, one oak,
willow, maple, yew, magic or redwood base shield, exactly 15 bronze, iron, steel, mithril,
adamantite or rune nails, and a hammer. They award 70/100/124/140/156/172 XP and each takes five
ticks. The original
[Leather Shields release revision 11842739](https://oldschool.runescape.wiki/w/Update:Leather_Shields_and_Tournament_Worlds?oldid=11842739)
independently states that shield Crafting awards the same XP as the equivalent two-hide armour and
uses two logs through the separately fletched wooden base.

Cache 233 resolves the leathers to 1743/6289/1745/2505/2507/2509, wooden shields to
22251/22254/22257/22260/22263/22266, nails to 4819/4820/1539/4822/4823/4824 and finished shields to
22269/22272/22275/22278/22281/22284. Its six purpose-specific sequences are
7386/7387/7831/7832/7833/7834. The revision-matched workspace RuneLite ItemID table independently
agrees with every nail and output identity; RuneLite was read statically and was not launched.

The cutoff-era
[Imcando hammer revision 14993167](https://oldschool.runescape.wiki/w/Imcando_hammer?oldid=14993167)
states that the repaired hammer works equipped or unequipped in every way a normal hammer does. Its
[off-hand revision 14984772](https://oldschool.runescape.wiki/w/Imcando_hammer_%28off-hand%29?oldid=14984772)
states that the shield-slot variant functions identically. The implementation therefore accepts
normal hammer 2347 and Imcando forms 25644/29775 in inventory or equipment.

Using the matching leather and wooden shield in either order starts one atomic five-tick action;
there is no inferred batch or quantity interface. All level, membership, hammer, two-leather,
base-shield and 15-nail checks precede mutation, and queue replacement, movement or late material
loss cancels safely. Eight integration tests pin all cache identities, recipes, XP and animations;
all four hammer configurations; every gate; full-inventory transformation; one-action semantics;
interruption; and reverse held-item dispatch. The exact initiating-item precedence, messages,
sound, client ordering and task-credit hooks remain pending production capture.

## Tannery service

The pre-cutoff
[Tanner revision 14797115](https://oldschool.runescape.wiki/w/Tanner?oldid=14797115) fixes the
ordinary service at eight transformations: cowhide to soft or hard leather, both snake-hide objects
to snakeskin, and green/blue/red/black dragonhide to the matching leather. It explicitly states that
tanning itself has no Crafting-level requirement. Standard prices are 1/3/15/20/20/20/20/20 coins;
[Sbott revision 14767641](https://oldschool.runescape.wiki/w/Sbott?oldid=14767641) pins the premium
schedule to 2/5/25/45/45/45/45/45. Eodan's undiscounted schedule is
10/30/150/200/200/200/200/200, reduced by 20% for each completed Kourend & Kebos Diary tier. The
pinned Leather source establishes that a full inventory is processed in one game tick, so a button
selection is implemented as one immediate atomic transaction rather than an item-by-item batch.

The Tanner source also establishes the Ranging Guild Leatherworker's dialogue-only access, and the
pinned `Transcript:Leatherworker` text supplies every implemented player/NPC line and conditional
tannable-hide choice. Cache 233 independently resolves interface 324, model components 100–107,
name components 108–115, price components 116–123 and All/X/5/1 components 124–155. It resolves the
six supported service NPCs to 3231/5809/6058/6526/8711/14662 and confirms that 6058 lacks a Trade
operation while the other five expose Trade or Tan-hides in operation slot 3. Object identities are
1739/6287/7801/1753/1751/1749/1747 for inputs and 1741/1743/6289/1745/2505/2507/2509 for outputs.

Ten integration tests pin the complete cache surface, recipe and price tables, native interface
payload, all diary tiers, every immediate transform, affordability/membership/missing-input
atomicity, stale-service rejection and the actual dialogue-to-button Leatherworker route. Exact
presentation for the other tanners, sounds and activity/achievement integration remain open.

## Elemental battlestaff Crafting

The final inspected pre-cutoff
[Crafting revision 14996237](https://oldschool.runescape.wiki/w/Crafting?oldid=14996237),
[water battlestaff revision 14739336](https://oldschool.runescape.wiki/w/Water_battlestaff?oldid=14739336),
[earth battlestaff revision 14800379](https://oldschool.runescape.wiki/w/Earth_battlestaff?oldid=14800379),
[fire battlestaff revision 14800435](https://oldschool.runescape.wiki/w/Fire_battlestaff?oldid=14800435)
and
[air battlestaff revision 14739244](https://oldschool.runescape.wiki/w/Air_battlestaff?oldid=14739244)
define the four members-only orb-attachment recipes. One ordinary battlestaff plus the matching
water, earth, fire or air orb requires boosted Crafting level 54, 58, 62 or 66 and produces one
elemental battlestaff for 100, 112.5, 125 or 137.5 XP. Each dated item recipe specifies two ticks
per staff and separately records that Make-X takes three ticks for the first result, then two ticks
for every remaining result.

Cache 233 independently resolves the ordinary staff as 1391, elemental orbs as 571/575/569/573 and
the corresponding outputs as 1395/1399/1393/1397. It provides shared sequence 7531 and distinct
water/earth/fire/air creation spotanims 1370/1371/1372/306; the revision-matched RuneLite checkout
names the same identities. The implementation uses interface 270 and clientscript 2046 as a
one-product quantity chooser, supports either item order, preflights membership/current boosted
level/materials and commits each output and XP award atomically. Movement clicks use the engine's
normal pending-action cancellation, and all Crafting batches replace one another.

Eight integration tests pin every cache identity and recipe, the native quantity payload and range,
all transforms/XP/animation/effects, membership/level/material failures, the asymmetric first and
repeat cadence, material exhaustion, movement cancellation, cross-Crafting replacement and actual
held-item dispatch. Exact failure/success wording, sound and client ordering still require capture.
The cache-233 specialty products derived from battlestaves, POH crystal-ball elemental conversion
and Farming Guild diary credit are deliberately separate open integration work.

## Loom weaving Crafting

The final inspected pre-cutoff
[Crafting revision 14996237](https://oldschool.runescape.wiki/w/Crafting?oldid=14996237) and
[Loom revision 14918604](https://oldschool.runescape.wiki/w/Loom?oldid=14918604) define the four
members-only cache-233 loom products. Four balls of wool make a strip of cloth at Crafting 10 for
12 XP; four jute fibres make an empty sack at 21 for 38 XP; two jute fibres make a drift net at 26
for 55 XP; and six willow branches make a basket at 36 for 56 XP. The dated loom behavior specifies
four ticks for the first ordinary product and three ticks for each Make-All remainder, while drift
nets take three ticks for both their first and repeated actions.

Cache 233 resolves inputs 1759/5931/5933, outputs 3224/5418/21652/5376, usable loom identities
787/8717/31432 and weaving sequence 2270. The revision-matched RuneLite checkout independently
names the public loom identities and `CRAFTING_LOOM` animation without being launched. The plugin
uses interface 270 and clientscript 2046 for an ordered four-product quantity chooser, accepts the
matching material directly on a loom, preflights membership/current boosted level/material count,
and commits each many-to-one transform and XP award atomically. Distance/material loss stops the
batch and the shared Crafting registry makes every Crafting batch replace every other one.

Seven integration tests pin the recipe/cache/loom/animation table, native interface payload and
range, all transforms and XP, both exact cadence families, membership/level/material atomicity,
movement/material stops and cross-Crafting replacement. Exact messages, sound and client ordering,
direct-input filtering as observed in the client, every physical loom placement, full activity
interruption and diary/League-task hooks remain open capture and integration work.

## Steel cannonball Smithing

The last inspected pre-cutoff [Steel cannonball revision 14893491](https://oldschool.runescape.wiki/w/Steel_cannonball?oldid=14893491),
[ammo mould revision 14893479](https://oldschool.runescape.wiki/w/Ammo_mould?oldid=14893479) and
[double ammo mould revision 14893482](https://oldschool.runescape.wiki/w/Double_ammo_mould?oldid=14893482)
pin the cache-233 production recipe family. Cannonballs require members, level 35 Smithing and
partial completion of Dwarf Cannon. One steel bar with the regular mould produces four cannonballs
for 25.6 XP; two bars with the double mould produce eight for 51.2 XP. Later metal cannonball
families released with Sailing are outside the cutoff.

The [Dwarf Cannon revision 14957886](https://oldschool.runescape.wiki/w/Dwarf_Cannon?oldid=14957886)
states that cannonballs become available after obtaining the ammo mould. Revision-matched Quest
Helper source at commit `633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a` maps that handoff to the
completed `mcannon` value 10, so the implementation uses that threshold instead of full quest
completion. Cache 233 resolves cannonball object 2, ammo mould object 4, double ammo mould object
27012 and sequence 827 (`human_pickupfloor`). Static revision-matched RuneLite source independently
labels sequence 827 as cannonball Smithing and supplies the exact cooling messages for four/eight
cannonballs plus `You remove the cannonballs from the mould`; RuneLite was not launched.

The pinned recipe gives an eight-tick initial action and ten-tick Make-X repeats. Cache interface
270 and clientscript 2046 are reused as a one-choice action-mode-13 quantity prompt. When a steel
bar is used while both moulds are held, prioritising the double mould if two bars are available is
an implementation inference; directly using a mould selects it explicitly. The Furnace page's
20-coin Shilo charge is currently applied once per cannonball action, including a double-mould
action, but the exact double-mould interpretation requires client capture. Exact failure/menu
ordering, Morytania medium diary credit and matching-client audiovisual/inventory timing remain
separate researched work.

## Telekinetic Grab

The final Telekinetic Grab revision inspected before the cutoff is
[`14916358`](https://oldschool.runescape.wiki/w/Telekinetic_Grab?oldid=14916358), dated 6 June 2025.
It establishes level 33, one air/one law rune, 43 Magic XP, a three-tick casting speed, a range of up
to ten tiles and pickup of an entire stackable ground pile. It explicitly permits casting over or
through fences, gates and rivers. The page's exception examples are Ahab's beer, raw pheasant,
Trouble Brewing flowers, dragon token and equipable Achievement Diary rewards. Its trivia records
the exact plane-change message: `Your telegrab fizzles as you move too far away.`

The official 8 May 2024
[Project Rebalance update](https://oldschool.runescape.wiki/w/Update:Project_Rebalance:_Skilling_%26_Poll_81_MTA_Changes)
removed Telekinetic Grab's global action stall and made projectile travel time independent of target
distance. This directly supports a fixed-distance projectile and no player lock. It does not expose
the wire timing values. The implementation's client start/end cycles 35/45 and two-server-tick
impact come from historical third-party emulation and are deliberately classified as an inference
until captured with the matching cache-233 client.

Cache 233 supplies spellbook component child 27, casting sequence 723, presentation sequences
681–683, casting/travel/impact spotanims 142–144 and synth 192. Cache symbols also resolve the five
named exception examples, both looting-bag states and four reward tiers for each of the twelve
Achievement Diaries. All 55 identities are rejected before rune or XP mutation in automated tests.
The looting-bag inclusion is supported by later Wiki documentation but was not listed in the pinned
page's example sentence, so it remains subject to cutoff client confirmation.

The current Wiki additionally documents already-taken, Tower of Life and busy farming-area paths,
but those statements were not present in the pinned revision. The generic ground-item message
`Too late - it's gone!` is implemented for stale targets and full-inventory failure uses the
existing engine message, while their exact Telekinetic Grab applicability remains a manual capture
requirement. Sailing-specific behavior first documented after the production cutoff is excluded.

## Jewellery enchantment spells

The umbrella revision at the cutoff is [Enchantment spells revision
14986087](https://oldschool.runescape.wiki/w/Enchantment_spells?oldid=14986087), dated 14 September
2025. The last inspected individual revisions on or before the cutoff are Lvl-1
[`14904404`](https://oldschool.runescape.wiki/w/Lvl-1_Enchant?oldid=14904404), Lvl-2
[`14904405`](https://oldschool.runescape.wiki/w/Lvl-2_Enchant?oldid=14904405), Lvl-3
[`14904406`](https://oldschool.runescape.wiki/w/Lvl-3_Enchant?oldid=14904406), Lvl-4
[`14904407`](https://oldschool.runescape.wiki/w/Lvl-4_Enchant?oldid=14904407), Lvl-5
[`14904408`](https://oldschool.runescape.wiki/w/Lvl-5_Enchant?oldid=14904408), Lvl-6
[`14904409`](https://oldschool.runescape.wiki/w/Lvl-6_Enchant?oldid=14904409) and Lvl-7
[`14904410`](https://oldschool.runescape.wiki/w/Lvl-7_Enchant?oldid=14904410). They establish levels
7/27/49/57/68/87/93, XP 17.5/37/59/67/78/97/110, the exact rune costs, ordinary three-tick manual
casts and seven-tick uninterrupted automatic repeats. The umbrella history records automatic
same-type continuation on 21 February 2019, one queued click on 21 August 2014 and selection of the
first matching source item on 12 February 2025.

The cutoff target set contains 44 transformations: nine at level 1, ten at level 2, nine at level
3, and four in each of levels 4–7. It covers all four jewellery forms for sapphire/opal,
emerald/jade, ruby/red topaz, diamond, dragonstone, onyx and zenyte, plus the Silvthrill rod,
pre-nature amulet, emerald sickle (b) and ruby sickle (b). The special-target revisions inspected
are [Silvthrill rod 14777174](https://oldschool.runescape.wiki/w/Silvthrill_rod?oldid=14777174),
[pre-nature amulet 14837580](https://oldschool.runescape.wiki/w/Pre-nature_amulet?oldid=14837580),
[emerald sickle (b) 14900656](https://oldschool.runescape.wiki/w/Emerald_sickle_(b)?oldid=14900656)
and [ruby sickle (b) 14900659](https://oldschool.runescape.wiki/w/Ruby_sickle_(b)?oldid=14900659).
Only the sapphire, emerald, ruby and diamond amulets are allowed on free worlds. A ruby necklace
requires prior Digsite-pendant discovery, and a player may own only one nature amulet; the local
core checks inventory, equipment and bank.

Cache 233 supplies Standard-spellbook child components 13/24/37/46/61/74/77, human sequences
719/720/721, spotanims 114/115/116/153/154/452 and core-gem amulet/ring synths 136–147. The
top-level spell enum contains the jewellery submenu opener rather than these seven children, so the
spell registry explicitly includes the child objects. No separate level-7 spotanim or zenyte synth
is named in the cache: level 7 therefore reuses the level-6 spotanim and onyx sounds as a documented
cache-informed fallback. Semi-precious stones reuse their paired core-gem sounds, and graphic height
100 remains a historical-emulator inference. Cache varbit 3644 is named `vm_digsite_talisman` and
is the best local discovery-state evidence, but its boolean interpretation, exact messages and all
presentation/interrupt timing remain pending matching-client capture.

## Crossbow bolt enchantment spells

The last inspected Enchant Crossbow Bolt revision on or before the cutoff is
[`14957210`](https://oldschool.runescape.wiki/w/Enchant_Crossbow_Bolt?oldid=14957210), dated 3 August
2025. It establishes the members-only submenu, all ten child spells, their exact Magic levels,
runes and XP, both regular and dragon tipped-bolt outputs, one full rune/XP cost per set of up to ten
bolts, the exact success message, rapid one-tick manual recasting, and the fact that casting does not
interrupt Mining or Fishing. Its two official screenshots separately show regular and dragon
enchanted-bolt product icons in the quantity interface.

Cache 233 supplies submenu object 5000 at Standard-spellbook child 218:10 and spell objects
24350–24359 for Opal, Sapphire, Jade, Pearl, Emerald, Red Topaz, Ruby, Diamond, Dragonstone and
Onyx. The client contract is generic interface 270 (`skillmulti`), pause-button children 14–23 and
clientscript 2046 (`skillmulti_setup`); action mode 19 renders `Enchant`. The server script arguments
are the action mode, maximum quantity, ten product object IDs, initial quantity and title. Cache
symbols also resolve sequence 4462 (`human_xbow_enchant_arrowtip`), spotanim 759
(`xbows_enchanting_tips_spot`) and synth 2921 (`enchanted_tipping`).

Neither the pinned page nor the cache script contract states which family wins when regular and
dragon tipped bolts coexist, or how a single maximum is derived from several gem types. The current
regular-first transformation/menu policy and maximum of the largest combined regular-plus-dragon
ten-bolt set count are documented implementation inferences pending workspace RuneLite capture.

## Elemental orb-charging spells

The last inspected spell revisions on or before the cutoff are Charge Water Orb
[`14906060`](https://oldschool.runescape.wiki/w/Charge_Water_Orb?oldid=14906060), Charge Earth Orb
[`14904459`](https://oldschool.runescape.wiki/w/Charge_Earth_Orb?oldid=14904459), Charge Fire Orb
[`14904462`](https://oldschool.runescape.wiki/w/Charge_Fire_Orb?oldid=14904462) and Charge Air Orb
[`14904461`](https://oldschool.runescape.wiki/w/Charge_Air_Orb?oldid=14904461), all from May 2025.
They establish members-only levels 56/60/63/66, thirty matching elemental runes, three cosmic
runes and one unpowered orb, with 66/70/73/76 Magic XP. Each spell is cast on only its matching
obelisk and replaces one unpowered orb with the matching elemental orb. Manual casts take three
ticks; uninterrupted automatic continuation repeats every six ticks.

The inspected obelisk revisions are Water
[`14929931`](https://oldschool.runescape.wiki/w/Obelisk_of_Water?oldid=14929931), Earth
[`14824777`](https://oldschool.runescape.wiki/w/Obelisk_of_Earth?oldid=14824777), Fire
[`14824779`](https://oldschool.runescape.wiki/w/Obelisk_of_Fire?oldid=14824779) and Air
[`14947768`](https://oldschool.runescape.wiki/w/Obelisk_of_Air?oldid=14947768). Cache 233 resolves
their location identities to 2151/2150/2153/2152 and the input/output objects to 567 and
571/575/569/573. It also supplies spellbook children 45/49/56/59, player sequence 726, spotanims
149/151/152/150 and named synths 118/115/117/116 for Water/Earth/Fire/Air. The workspace RuneLite
checkout independently identifies sequence 726 as `MAGIC_CHARGING_ORBS`.

The historical pages identify hard Kandarin diary credit for Water, medium Wilderness credit for
Earth and hard Wilderness credit for Air. Those task hooks, exact rejection messages, full
interruption semantics and client/server audiovisual ordering remain pending production integration
or matching-client capture.

## Monster Inspect

The last inspected Monster Inspect revision on or before the cutoff is
[`14965884`](https://oldschool.runescape.wiki/w/Monster_Inspect?oldid=14965884), from August 2025.
It records the spell's 25 June 2025 release, free-to-play level 42 requirement, two body/two mind
rune cost after the 2 July adjustment, 30.5 Magic XP and monster-only targeting. It describes four
tabs: current basic stats; generic attack speed/accuracy/strength values; stab, slash, crush, magic
and three ranged defensive styles; and Slayer, poison, venom, thrall, burn, freeze, demonic,
draconic, vampyric, spectral, rat, golem and elemental-weakness attributes.

Cache 233 resolves the spell to object 28180 and spellbook child 193. Interface 522 is populated by
clientscript 1179, which disassembly confirms accepts one string, splits it into exactly five
pipe-delimited sections and assigns the title plus four tab bodies. The cache also supplies player
sequence 6293, spotanim 1059 and synth 3620 (`lunar_stat_spy`). The current type model can represent
most listed attributes, and enrichment now carries `freeze_resistance`; all five Phantom Muspah
forms are configured at 33%. Burn tiers, vampyric tier, spectral classification, exact
Slayer eligibility and production-wide NPC values are not yet available from the local enrichment
model, while exact rendered labels, omissions and client/server ordering still require capture.

## Standard self teleports

The last inspected spell revisions on or before the cutoff are Varrock
[`14918406`](https://oldschool.runescape.wiki/w/Varrock_Teleport?oldid=14918406), Lumbridge
[`14918404`](https://oldschool.runescape.wiki/w/Lumbridge_Teleport?oldid=14918404), Falador
[`14918402`](https://oldschool.runescape.wiki/w/Falador_Teleport?oldid=14918402), Teleport to House
[`14918405`](https://oldschool.runescape.wiki/w/Teleport_to_House?oldid=14918405), Camelot
[`14918400`](https://oldschool.runescape.wiki/w/Camelot_Teleport?oldid=14918400), Kourend Castle
[`14918403`](https://oldschool.runescape.wiki/w/Kourend_Castle_Teleport?oldid=14918403), Ardougne
[`14918399`](https://oldschool.runescape.wiki/w/Ardougne_Teleport?oldid=14918399), Civitas illa
Fortis [`14918401`](https://oldschool.runescape.wiki/w/Civitas_illa_Fortis_Teleport?oldid=14918401),
Watchtower [`14863767`](https://oldschool.runescape.wiki/w/Watchtower_Teleport?oldid=14863767),
Trollheim [`14863768`](https://oldschool.runescape.wiki/w/Trollheim_Teleport?oldid=14863768) and
Ape Atoll [`14906055`](https://oldschool.runescape.wiki/w/Ape_Atoll_Teleport?oldid=14906055).
Together they establish the cutoff levels, rune/item costs, XP, membership and quest/diary
requirements, ordinary level-20 Wilderness ceiling and four-tick cast duration.

The first implemented batch is Varrock (level 25, 3 air/1 fire/1 law, 35 XP), Lumbridge (31,
3 air/1 earth/1 law, 41 XP), Falador (37, 3 air/1 water/1 law, 48 XP) and Camelot (45,
5 air/1 law, 55.5 XP). Cache 233 supplies spellbook children 23/26/29/34 and base destination
parameters, while the shared presentation resolves to human sequence 714, spotanim 111 and synth
200. The Falador editor value was corrected from 47 to the cutoff's 48 XP. The workspace RuneLite
checkout independently identifies spotanim 111 and synth 200. Landing-area randomisation, diary
configuration flows, exact rejection ordering/text and the server interpretation of the four-tick
duration remain pending matching-client observation.

The second implemented batch is Kourend Castle (level 48, 1 water/1 fire/2 law, 58 XP), Ardougne
(51, 2 water/2 law, 61 XP), Civitas illa Fortis (54, 1 earth/1 fire/2 law, 64 XP), Watchtower (58,
2 earth/2 law, 68 XP), Trollheim (61, 2 fire/2 law, 68 XP) and Ape Atoll (64, 2 fire/2 water/2 law
plus one unnoted banana, 74 XP). Cache database rows and clientscript 4024 tie these to
`veos_progress >= 7`, `elenaquest >= 30`, `vmq2 >= 50`, `itwatchtower >= 14`,
`eadgar_quest >= 110` and `hundred_ilm_quest >= 50`. The Ardougne and Watchtower thresholds are one
state beyond their quest-completion rows because their reward scrolls must be read. The Ape Atoll
spell object's fourth requirement is the normal banana object; server loading overrides that
object's legacy wear position so it is consumed from inventory. The old Watchtower editor
coordinate decoded to an unrelated map area and was corrected to its evidenced tower base at
2546,3114.

The cutoff pages define the three alternate destinations as Grand Exchange at 3165,3479 after the
medium Varrock Diary, Seers' Village bank after the hard Kandarin Diary, and Yanille at 2584,3097
after the hard Ardougne Diary. Cache 233 supplies completion varbits 4480/4477/4460 and saved-choice
varbits 4585/4560/4548 respectively. The server now requires both the completion and matching saved
choice before selecting the alternate; otherwise it keeps the base destination. The Seers' page
defines a polygon rather than a single tile, so 2724,3484 is the current valid representative
square pending landing-area randomisation. Right-click configuration flows remain separate work.

## Home Teleports

The last Lumbridge Home Teleport revision before the cutoff is
[`14924157`](https://oldschool.runescape.wiki/w/Lumbridge_Home_Teleport?oldid=14924157). It defines a
free-to-play, level-1, no-rune, zero-XP teleport to Lumbridge with a 30-minute cooldown shared by all
Home Teleports. The default cast takes 22 ticks, can be interrupted by most actions including
combat, and cannot begin until ten seconds after combat. Early recasts report the remaining whole
minutes. The Speedy override reduces the cast, but override selection is separate unfinished work.

Cache 233 supplies component `magic_spellbook:teleport_home_standard` (218:7), permanent cooldown
varp `aide_tele_timer` (892), current-time varp `date_minutes` (3078), sequences
4847/4850/4853/4855/4857, spotanims 800/802/803/804 and synths 193/196/194/195. The workspace
RuneLite checkout independently documents varp 892 as an epoch-minute timestamp and adds 30 minutes
for its timer. A live-protocol research capture records the default phase chain at ticks
0/4/7/10/13, destination movement at tick 22 and the portal spotanim's unsigned height 65526:
[spellbook cooldown research](https://clockworkrs.com/docs/research/spellbook-teleport-cooldowns)
and [shared cast capture](https://clockworkrs.com/docs/research/minigame-teleport-grouping).
The other cutoff revisions are Edgeville
[`14863787`](https://oldschool.runescape.wiki/w/Edgeville_Home_Teleport?oldid=14863787), Lunar Isle
[`14863797`](https://oldschool.runescape.wiki/w/Lunar_Home_Teleport?oldid=14863797) and Arceuus
[`14863774`](https://oldschool.runescape.wiki/w/Arceuus_Home_Teleport?oldid=14863774). Their map
contracts are an Edgeville polygon spanning 3087..3090 by 3501..3504, a Lunar Isle square centred
at 2113,3915 and an Arceuus Dark Altar square centred at 1700,3882. The 233.4 Edgeville revision
explicitly says it works on F2P even though changing to Ancient Magicks requires membership; Lunar
and Arceuus are members-only. This conflicts with the later generic Home Teleport page's statement
that F2P can only use Lumbridge, so the dated individual page governs the pinned implementation.

Cache components 218:103/104/148 select the Ancient, Lunar and Arceuus variants. All four now share
one channel and cooldown implementation. Destination-area randomisation, Lumbridge's H.A.M. cell
and Draynor Manor ground exceptions, animation overrides and matching-client observation remain
required before these rows can advance beyond integration-tested.

## Teleother

The last inspected revisions on or before the cutoff are Teleother Lumbridge
[`14863770`](https://oldschool.runescape.wiki/w/Teleother_Lumbridge?oldid=14863770), Falador
[`14863771`](https://oldschool.runescape.wiki/w/Teleother_Falador?oldid=14863771) and Camelot
[`14863773`](https://oldschool.runescape.wiki/w/Teleother_Camelot?oldid=14863773). They establish the
members-only level/rune/XP contracts, ten-tick speed, Accept Aid requirement and recipient
confirmation. Accept Aid revision
[`14519585`](https://oldschool.runescape.wiki/w/Accept_Aid?oldid=14519585) confirms Teleother is one
of the governed interactions and records combat distraction plus temporary and Ironman caveats.
Teleportation revision
[`14884658`](https://oldschool.runescape.wiki/w/Teleportation?oldid=14884658) independently requires
both Accept Aid and recipient agreement. Tele Block revision
[`14900814`](https://oldschool.runescape.wiki/w/Tele_Block?oldid=14900814) establishes that the status
prevents most teleportation.

The pages map Lumbridge as a polygon, Falador as a radius-two square at 2964,3378 and Camelot as a
radius-two square at 2757,3479. The cutoff Map-template documentation at
[`14997598`](https://oldschool.runescape.wiki/w/Template:Map/doc?oldid=14997598) states that polygon
vertices align to tiles' south-west corners, yielding the 16 exact Lumbridge landing tiles used by
the implementation.

Cache 233 supplies spell objects 4553/4554/4556; spellbook children 64/70/75; interface 326 with
caster name, location, Accept and Decline children 89/91/97/99; casting/impact sequences 1818/1816;
casting/impact spotanims 343/342; and synth 199. Both confirmation components carry the cache's
pause-button event bit, which determines the server protocol. The Wiki does not settle the exact
failure messages or resource-charge moment, so those remain implementation inferences pending
matching-client capture.

## Teleport to Target

The last inspected spell revision on or before the cutoff is
[`14863772`](https://oldschool.runescape.wiki/w/Teleport_to_Target?oldid=14863772). It establishes
members-only level 85 Magic, one law/chaos/death rune, 45 XP, a four-tick cast, availability on all
four spellbooks, unlock through the target teleport scroll, a target within 12 squares and five
uses per assigned target. The target teleport scroll transcript at
[`14711003`](https://oldschool.runescape.wiki/w/Transcript:Target_teleport_scroll?oldid=14711003)
pins the explanatory text, confirmation title and choices, success message and already-unlocked
message used by the implementation.

The official 7 June 2023 Bounty Hunter update
[`Pride Event 2023`](https://oldschool.runescape.wiki/w/Update:Pride_Event_2023) expands the spell
from the lobby into the crater when the caster has not been in combat during the previous 12
seconds. That is represented as 20 server cycles at 0.6 seconds each. The original Bounty Hunter
release context and spell page establish lobby use. These rules require authoritative minigame
state; no historical evidence supports treating an arbitrary rectangular coordinate range as the
whole lobby or crater.

Cache 233 supplies spell object 17152, spellbook child 218:73, scroll object 12846 and varbits 2010
(`bounty_target_teleport`, varp 1140 bit 31), 6503 (`bounty_target_limit`, varp 1140 bit 30), 236
(`bounty_teleport_warning`, varp 1429 bit 10) and 5604 (`bounty_teleport_quota`, varp 1429 bits
6..9). The spell object's null spellbook is cache evidence for all-spellbook access. The scroll is
members-only, tradeable, valued at 30,000 and exposes `Read` in inventory. Cache data does not pin
the exact failure strings, destination-square search order or production Bounty target lifecycle;
those remain explicit inferences or upstream minigame work.

## Teleport to House

The last inspected spell revision on or before the cutoff is
[`14918405`](https://oldschool.runescape.wiki/w/Teleport_to_House?oldid=14918405). It establishes
members-only level 40 Magic, one air/earth/law rune, 30 XP, a four-tick cast, inside or outside
destinations and the saved default-building-mode choice. Portal revision
[`14980096`](https://oldschool.runescape.wiki/w/Portal_(Player-owned_house)?oldid=14980096) and the
POH-location table revision
[`14751595`](https://oldschool.runescape.wiki/w/Template:Table/POH_locations?oldid=14751595)
establish the nine cache-233 locations: Rimmington, Taverley, Pollnivneach, Hosidius, Rellekka,
Aldarin, Brimhaven, Yanille and Prifddinas.

Cache 233 supplies spell object 8828, spellbook child 218:31 and house portal locs 15477–15482,
28822, 34947 and 55353. Varbits 2176 (`poh_building_mode`, varp 780 bit 0), 2187
(`poh_house_location`, varp 738 bits 0..4), 4744 (`poh_tele_toggle`, varp 1047 bit 23) and 14670
(`poh_teleport_building_mode`, varp 2885 bit 31) pin ownership/location and client settings.
Clientscript 2615 proves operation 1 is inside, operation 2 is the static Outside action, operation 3
is the alternate default Cast action and operations 4/5 are Group choose/previous.

The outside tiles are open squares adjacent to the corresponding cache portals. The exact official
landing-square selection, rejection strings, production instance generation, build-mode state and
Group Ironman ownership flow are not encoded by the spell object. Those remain explicit client or
upstream-content work; the implementation therefore accepts only already-prepared inside entrances
and never fabricates an unloaded house instance.

## Standard log-cutting Fletching

The last inspected general Fletching revision on or before the cutoff is
[`14960092`](https://oldschool.runescape.wiki/w/Fletching?oldid=14960092). The supporting pinned
revisions are Arrow shaft
[`14984421`](https://oldschool.runescape.wiki/w/Arrow_shaft?oldid=14984421), Fletching knife
[`14976006`](https://oldschool.runescape.wiki/w/Fletching_knife?oldid=14976006), Redwood hiking staff
[`14972462`](https://oldschool.runescape.wiki/w/Redwood_hiking_staff?oldid=14972462), Crossbow stock
[`14236652`](https://oldschool.runescape.wiki/w/Crossbow_stock?oldid=14236652), Javelin shaft
[`14421792`](https://oldschool.runescape.wiki/w/Javelin_shaft?oldid=14421792) and Wooden shield
[`14889178`](https://oldschool.runescape.wiki/w/Wooden_shield?oldid=14889178). Together they establish
members-only Fletching and the 35 implemented standard log recipes.

Normal, oak, willow, maple, yew, magic and redwood logs make 15/30/45/60/75/90/105 arrow shafts at
levels 1/15/30/45/60/75/90 for 5/10/15/20/25/30/35 XP. Normal logs additionally make 15 javelin
shafts at level 3 for 5 XP. Shortbows use levels 5/20/35/50/65/80 and award
5/16.5/33.3/50/67.5/83.3 XP; longbows use levels 10/25/40/55/70/85 and award
10/25/41.5/58.3/75/91.5 XP. Normal/oak/willow/teak/maple/mahogany/yew/magic stocks use levels
9/24/39/46/54/61/69/78 and award 6/16/22/27/32/41/50/70 XP. Oak through redwood shields consume
two logs at levels 27/42/57/72/87/92 for 50/83/116.5/150/183/216 XP. The redwood hiking staff uses
one log at level 90 for 10.5 XP.

The Fletching-knife revision records object 31043, introduced by Varlamore: The Final Dawn on 23
July 2025. It reduces supported repeated knife-Fletching actions from three ticks to two, but not
the first result's two-tick Make-All startup. Its speed reward works from inventory or the off-hand,
while a knife must still be in inventory to initiate cutting. Cache 233 independently pins ordinary
knife 946, Fletching knife 31043, redwood hiking staff 31049, interface 270, clientscript 2046 and
human sequence 1248. The implementation's registry preserves the conventional product grouping,
but exact displayed choice order, labels, disabled entries, action messages, sound and packet/
inventory ordering remain matching-client evidence boundaries rather than verified claims.

## Standard bow-stringing Fletching

The cutoff Fletching table at
[`14960092`](https://oldschool.runescape.wiki/w/Fletching?oldid=14960092) and the individually pinned
Shortbow [`14781523`](https://oldschool.runescape.wiki/w/Shortbow?oldid=14781523), Longbow
[`14781568`](https://oldschool.runescape.wiki/w/Longbow?oldid=14781568), oak short/long
[`14972470`](https://oldschool.runescape.wiki/w/Oak_shortbow?oldid=14972470) /
[`14972471`](https://oldschool.runescape.wiki/w/Oak_longbow?oldid=14972471), willow short/long
[`14747315`](https://oldschool.runescape.wiki/w/Willow_shortbow?oldid=14747315) /
[`14753277`](https://oldschool.runescape.wiki/w/Willow_longbow?oldid=14753277), maple short/long
[`14972467`](https://oldschool.runescape.wiki/w/Maple_shortbow?oldid=14972467) /
[`14972468`](https://oldschool.runescape.wiki/w/Maple_longbow?oldid=14972468), yew short/long
[`14747345`](https://oldschool.runescape.wiki/w/Yew_shortbow?oldid=14747345) /
[`14972466`](https://oldschool.runescape.wiki/w/Yew_longbow?oldid=14972466), and magic short/long
[`14938346`](https://oldschool.runescape.wiki/w/Magic_shortbow?oldid=14938346) /
[`14972464`](https://oldschool.runescape.wiki/w/Magic_longbow?oldid=14972464) revisions establish the
12 ordinary bow-stringing recipes. All consume one unstrung bow and one bow string, require the same
boosted Fletching level as cutting that bow, and take two ticks.

Shortbow XP is 5/16.5/33.2/50/67.5/83.2 for normal through magic; longbow XP is
10/25/41.5/58.2/75/91.5. The cutoff overview and the willow-shortbow/maple-longbow item pages
explicitly preserve the 33.3+33.2 and 58.3+58.2 splits. The magic-shortbow item infobox says 83.3
while the cutoff comprehensive table says 83.2; 83.2 is implemented because it preserves the same
166.5 total-XP split pattern, but this conflict remains recorded pending production observation.
Cache 233 pins bow string 1777, all 12 unstrung/output pairs, interface 270, clientscript 2046 and
the distinct stringing sequences 6678–6689.

The final pre-cutoff [Comp ogre bow revision
14989217](https://oldschool.runescape.wiki/w/Comp_ogre_bow?oldid=14989217), dated 20 September
2025, separately pins unstrung comp bow 4825 plus bow string 1777 becoming comp ogre bow 4827 after
two ticks at boosted level 30 for 45 XP. It is members-only and requires partial Zogre Flesh Eaters
completion. Cache 233 exposes the exact learned-production flag as varbit 500
(`thzfe_makecompozogrebow`) and the named ogre-Fletching sequence 4433. The registered recipe uses
the ordinary quantity interface and two-tick atomic stringing queue, accepts either item order, and
works from a full inventory. The cutoff spool page describes use on unfinished bows without an
exception, so the same recipe accepts one stored string charge. Sequence choice and the unlock
failure text remain evidence-backed implementation inferences pending matching-client capture.

The cutoff bow-string-spool revision is
[`14983366`](https://oldschool.runescape.wiki/w/Bow_string_spool?oldid=14983366). It establishes
account-tied storage, a base capacity of 250 scaling through 500/2,000/5,000/10,000/100,000 with up
to six spools, use in place of loose bow strings, automatic filling while spinning flax, noted-string
deposits and bank-proximate withdrawal. Cache 233 supplies object 31052, varp 4761 and varbits
17291/17293/17610/17773; the object's inventory operations are exactly Fill, Check and Empty. The
implemented core uses those cache-backed account fields for persistent charges and capacity,
registers Fill/Check, loose and noted deposits, same-item combining and use on all twelve ordinary
unstrung bows plus the composite ogre bow, and tests exact two-tick charge consumption. Bank-proximate
withdrawal and automatic filling while spinning flax remain separate researched work: the current
repository has no authoritative bank-area service or cross-skill acquisition hook that would justify
guessing those boundaries. Exact option text, feedback, varbit-display policy and client ordering
remain matching-client evidence boundaries.

## Standard ammunition Fletching

The cutoff evidence is Headless arrow
[`14982463`](https://oldschool.runescape.wiki/w/Headless_arrow?oldid=14982463), the arrow table
[`14765271`](https://oldschool.runescape.wiki/w/Template:Table/Fletching/Arrows?oldid=14765271),
the bolt table
[`14765263`](https://oldschool.runescape.wiki/w/Template:Table/Fletching/Bolts?oldid=14765263),
the dart overview [`14803791`](https://oldschool.runescape.wiki/w/Dart?oldid=14803791), and the
official-news copy for [Easter 2023](https://oldschool.runescape.wiki/w/Update:Easter_2023).
Headless arrows and finished arrows are made in sets of 15, while feathered bolts and darts are
made in sets of 10. Headless arrows award 1 XP each. The nine arrow levels/XP are bronze 1/1.3,
iron 15/2.5, steel 30/5, mithril 45/7.5, broad 52/10, adamant 60/10, rune 75/12.5, amethyst
82/13.5 and dragon 90/15. The ten bolt levels/XP are bronze 9/0.5, blurite 24/1, iron 39/1.5,
silver 43/2.5, steel 46/3.5, mithril 54/5, broad 55/3, adamant 61/7, runite 69/10 and dragon
84/12. Broad arrows and bolts additionally require the cache-backed Broader Fletching unlock.
The eight dart levels/XP are bronze 10/1.8, iron 22/3.8, steel 37/7.5, mithril 52/11.2, adamant
67/15, rune 81/18.8, amethyst 90/21 and dragon 95/25. Tourist Trap is required to smith metal dart
tips, not to attach feathers to acquired tips.

Cache 233 pins all 28 implemented input/output contracts, interface 270, clientscript 2046, the
Broader Fletching varbit 3208, Make-X option varbit 10971 and sequences 8463–8471, 8480–8488 and
9108. Arrows always use the native quantity flow and produce one set every two ticks. Darts and
bolts produce one set immediately when Make-X is disabled; when enabled, the menu queues at most ten
sets at the ordinary two-tick cadence. Material counts below one full set make the available partial
set and award per-item XP. The implementation does not claim verified menu wording, sound,
animation/inventory/XP packet ordering, or new-account initialization of the Make-X option. Those
remain matching-client or account-lifecycle evidence boundaries. Tipped, poisoned, ogre and
other special ammunition remain separate families.

## Brutal-arrow Fletching

The cutoff Fletching overview
[`14960092`](https://oldschool.runescape.wiki/w/Fletching?oldid=14960092) pins all seven
bronze-through-rune brutal-arrow levels and per-arrow XP: 7/1.4, 18/2.6, 33/5.1, 38/6.5,
49/7.5, 62/10.2 and 77/12.5. The
[Brutal arrows](https://oldschool.runescape.wiki/w/Brutal_arrows) production record establishes
that Sithik Ints must first teach the player during Zogre Flesh Eaters, four feathers are used in
the preceding flighted-arrow stage, and the final stage combines six flighted ogre arrows with six
nails while a hammer is in inventory. The individually documented
[mithril](https://oldschool.runescape.wiki/w/Mithril_brutal) and
[rune](https://oldschool.runescape.wiki/w/Rune_brutal) creation tables independently pin boosted
levels, membership, six outputs and a two-tick action.

Cache 233 pins flighted ogre arrows as object 2865; steel nails as 1539; bronze, iron, black,
mithril, adamant and rune nails as 4819–4824; and base output objects
4773/4778/4783/4788/4793/4798/4803. It exposes the dedicated Zogre learning flag as varbit 499
(`thzfe_makebrutalarrow`) and human ogre-Fletching sequence 4433. The implementation requires that
flag, a hammer, membership and the boosted tier level; accepts nails and flighted arrows in either
order; atomically produces up to the six available arrows after two ticks; awards per-arrow XP; and
shares cancellation with every other queued Fletching family. Exact rejection/success text, sound,
packet order, quest-dialogue coupling and broader interruption behavior remain matching-client
boundaries. Achey-log shaft cutting, wolfbone-tip cutting and composite ogre bows remain separate
work.

## Ogre-arrow-shaft Fletching

The final inspected pre-cutoff
[Ogre arrow shaft revision 14887298](https://oldschool.runescape.wiki/w/Ogre_arrow_shaft?oldid=14887298),
dated 18 April 2025, defines a members-only action available after starting Big Chompy Bird
Hunting. Using a knife on one Achey tree log requires boosted Fletching level 5, takes three ticks,
and randomly produces 2–6 ogre arrow shafts. Each shaft awards 1.6 Fletching XP, making the
documented typical four-shaft result worth 6.4 XP. Contemporary player reports independently
confirm that the action processes one log at a time rather than automatically consuming the
inventory.

Cache 233 pins Achey tree logs, the ordinary knife, the Fletching knife and ogre arrow shafts as
objects 2862/946/31043/2864; Big Chompy progress as varp 293; and the named human ogre-Fletching
sequence as 4433. The implementation accepts both knife types and either held-item order, consumes
one log and awards yield-scaled XP after three ticks, remains atomic in a full inventory, and
participates in shared Fletching cancellation. State zero rejects and a nonzero quest state admits
the action, matching the cache-backed started-state boundary used across the ogre-arrow chain.

The source publishes the full output range but not separate outcome probabilities. Uniform
inclusive selection through rsmod's injected `GameRandom` remains an explicit inference pending
production sampling. Sequence 4433, exact messages and sound, and animation/inventory/XP packet
ordering also remain matching-client capture work. The dedicated recipe page's 1.6 XP per shaft
takes precedence over the conflicting 1.5 value in the broader Achey-tree-log prose.

The last pre-cutoff [Unstrung comp bow revision
14514176](https://oldschool.runescape.wiki/w/Unstrung_comp_bow?oldid=14514176), dated 7 December
2023, defines the other knife-on-Achey-log product: one log plus one wolf bone becomes unstrung
comp bow 4825 at boosted Fletching level 30 for 45 XP after three ticks. Its prose requires partial
Zogre Flesh Eaters completion, while its recipe note says completion; the more precise cache flag
varbit 500 (`thzfe_makecompozogrebow`) resolves that conflict by recording the point at which Grish
teaches the player to make the bow. Cache 233 and the cutoff composite-bow page independently pin
the output and learned-production flag, and sequence 4433 is the only cache-named human ogre-
Fletching sequence.

Contemporary guides establish that using a knife on the log opens a choice between ogre shafts and
the unstrung composite bow. The implementation therefore routes both ordinary and Fletching knives
through one interface-270/clientscript-2046 menu, limits it to the independently evidenced one-log
action, and uses one queue so the two products cannot register conflicting handlers. The shaft-
first choice order, exact title/messages, and use of sequence 4433 remain cache- and guide-informed
inferences pending workspace-client capture; no repeat cadence or Fletching-knife speed bonus is
claimed for this single-action flow.

## Flighted and final ogre-arrow Fletching

The final pre-cutoff [Flighted ogre arrow revision
14927093](https://oldschool.runescape.wiki/w/Flighted_ogre_arrow?oldid=14927093), dated 26 June
2025, defines a members-only, boosted level-5 action gated by having started Big Chompy Bird
Hunting. Six ogre arrow shafts plus 24 ordinary feathers produce six flighted ogre arrows in two
ticks for 5.4 Fletching XP: four feathers and 0.9 XP per arrow. Cache 233 pins the shaft, feather and
flighted output as objects 2864/314/2865. The implementation generalizes ammunition recipes with
an explicit secondary-material ratio, rejects fewer than four feathers before queuing, accepts both
item orders and atomically produces the smaller of six shafts or complete four-feather groups. It
uses cache sequence 4433 (`human_ogre_fletching`) as the named ogre-Fletching animation and the same
cache-backed quest boundary documented below. Partial-set behavior, exact failure text and sequence
selection remain evidence-backed implementation inferences pending matching-client capture.

The final inspected pre-cutoff
[Ogre arrow revision 14539818](https://oldschool.runescape.wiki/w/Ogre_arrow?oldid=14539818)
defines the final members-only stage as six flighted ogre arrows plus six wolfbone arrowtips at
boosted Fletching level 5. It produces six ogre arrows after two ticks, awards 6 XP in total (1 XP
per arrow), and requires the player to have started Big Chompy Bird Hunting.

Cache 233 pins flighted ogre arrows, wolfbone arrowtips and the finished arrows as objects
2865/2861/2866; Big Chompy progress as varp 293 (`chompybird`); and the dedicated human
ogre-Fletching sequence as 4433. Quest DB row 8 (`quest_bigchompybirdhunting`) inherits unstarted
state 0 from quest-table column 18 and supplies completed state 65 in column 19, so the published
"started" condition has an exact cache boundary: state 0 rejects and every nonzero state admits the
action. The implementation accepts either held-item order, atomically produces up to six available
pairs after two ticks, awards per-arrow XP, works from a full inventory and shares the ammunition
queue with all other Fletching families. Producing a partial set, use of sequence 4433 and the
generic rejection text are evidence-backed implementation inferences pending matching-client
capture; exact success text, sound and animation/inventory/XP packet order remain open.

## Wolfbone-arrowtip Crafting and Fletching

The final inspected pre-cutoff
[Wolfbone arrowtips revision 14765916](https://oldschool.runescape.wiki/w/Wolfbone_arrowtips?oldid=14765916),
dated 10 October 2024, defines a members-only action after partially completing Big Chompy Bird
Hunting. Using a chisel on one wolf bone requires boosted level 5 in both Crafting and Fletching,
takes two ticks, and randomly produces 2–6 wolfbone arrowtips. Each resulting tip awards 2.5
Crafting XP and 2.5 Fletching XP, making the documented typical four-tip result worth 10 XP in
each skill. The revision also records Mod Ash's clarification that the quantity is random and is
not influenced by either skill level.

Cache 233 pins wolf bones, the chisel and wolfbone arrowtips as objects 2859/1755/2861; Big Chompy
progress as varp 293; and the named human ogre-Fletching sequence as 4433. Quest DB row 8 defines
the exact started-state boundary used by the related ogre-arrow stages: state zero rejects and a
nonzero state admits the action. The implementation uses rsmod's injected `GameRandom` inclusive
range API, consumes one bone and awards yield-scaled XP in both skills after two ticks, accepts
both held-item orders, remains atomic in a full inventory, and cancels or is cancelled by the
shared Crafting and Fletching queues.

The source publishes the complete outcome range and confirms stat independence, but does not
publish separate probabilities for the five outcomes. Treating each integer from 2 through 6 as
equally likely is therefore an explicit implementation inference, not a captured production
distribution. Sequence 4433, exact rejection/success text, sound, whether repeated bones continue
automatically, and animation/inventory/XP packet order also remain matching-client capture work.

## Tipped-bolt Fletching

The cutoff standard tipped-bolt table is
[`14930716`](https://oldschool.runescape.wiki/w/Template:Table/Fletching/Tipped_bolts?oldid=14930716)
(2 July 2025), and the dragon table is
[`14765934`](https://oldschool.runescape.wiki/w/Template:Table/Fletching/Tipped_dragon_bolts?oldid=14765934)
(10 October 2024). Together they pin ten-item sets, two-tick actions and all 22 boosted level and
per-bolt XP contracts. Standard pairs are opal/bronze 11/1.6, jade/blurite 26/2.4, pearl/iron
41/3.2, topaz/steel 48/3.9, barbed/bronze 51/9.5, sapphire/mithril 56/4.7,
emerald/mithril 58/5.5, ruby/adamant 63/6.3, diamond/adamant 65/7, dragonstone/runite
71/8.2, onyx/runite 73/9.4 and amethyst/broad 76/10.6. Every gem tip except barbed and amethyst
can instead be attached to dragon bolts at level 84, retaining its standard per-bolt XP except
topaz, whose dragon recipe is explicitly 4 XP.

The cutoff opal page
[`14646766`](https://oldschool.runescape.wiki/w/Opal_bolts?oldid=14646766), dragon-bolt page
[`14939589`](https://oldschool.runescape.wiki/w/Dragon_bolts?oldid=14939589), amethyst broad page
[`14976920`](https://oldschool.runescape.wiki/w/Amethyst_broad_bolts?oldid=14976920) and barbed page
[`14964900`](https://oldschool.runescape.wiki/w/Barbed_bolts?oldid=14964900) independently pin
membership, boosted levels, material counts, two-tick output and XP. Amethyst broad bolts require
Broader Fletching; already-acquired barbed tips have no separate Ranging Guild eligibility gate.
Dragon bolts cannot receive barbed or amethyst tips, and poisoned bolts cannot be tipped.

Cache 233 pins tips 45/46/47/9187–9194/21338, standard bases
877/9139–9144/11875, standard outputs 879/880/881/9335–9342/21316 and dragon base 21905 plus
outputs 21955/21957/21959/21961/21963/21965/21967/21969/21971/21973. Its tier-specific
attachment sequences are 8472–8479. The revision-matched local RuneLite constant names sequence
8473 `FLETCHING_ATTACH_BOLT_TIPS_TO_IRON_BROAD_BOLT`, resolving the otherwise ambiguous amethyst
broad animation to the iron/broad sequence. Make-X option varbit 10971 names both darts and bolts;
disabled use schedules one ten-item or partial set at the documented two-tick boundary, while the
enabled native interface 270/clientscript 2046 flow schedules up to ten sets at the same cadence.
Exact menu-memory initialization, any success feedback or sound, packet ordering, wider
movement/combat/logout/death interruption and League/diary hooks remain matching-client work.

## Standard javelin Fletching

The last javelin-table revision at or before the cutoff is
[`14765341`](https://oldschool.runescape.wiki/w/Template:Table/Fletching/Javelins?oldid=14765341)
(9 October 2024). It pins sets of 15 and the eight boosted level/XP-per-javelin pairs: bronze 3/1,
iron 17/2, steel 32/5, mithril 47/8, adamant 62/10, rune 77/12.4, amethyst 84/13.5 and dragon
92/15. It also states that ordinary production assumes 3,000 sets per hour and that the first two
sets occur in rapid succession. The final cutoff-date bronze page
[`14982925`](https://oldschool.runescape.wiki/w/Bronze_javelin?oldid=14982925) (9 September 2025)
independently pins the first set at one tick and the 15-tip plus 15-shaft transform. Together these
facts establish a one-tick first result, a second result one tick later and two-tick steady repeats;
the latter is an explicit inference from the documented 3,000-set-per-hour long-run rate.

Cache 233 pins tips 19570/19572/19574/19576/19578/19580/21352/19582, shared shafts 19584 and
outputs 825/826/827/828/829/830/21318/19484. No dedicated javelin-making sequence exists among the
cache's Fletching sequences, so the implementation does not substitute the bow-cutting, arrow,
bolt or dart animations. Both item orders open the native one-product quantity flow, capped at ten
sets; every result atomically consumes equal tips and shafts, supports a partial final set and awards
XP per produced javelin. Ten focused integrations now cover all 36 ordinary-ammunition contracts,
the javelin cache identities, all eight transforms, exact XP, the 1/1/2-tick cadence, partial sets,
reverse held-use, quantity cap, failures and cancellation. Exact menu wording, sound and packet
ordering, and direct matching-client confirmation that production has no animation remain open.

## Standard crossbow-assembly Fletching

The last crossbow table revision at or before the cutoff is
[`13747019`](https://oldschool.runescape.wiki/w/Template:Table/Fletching/Crossbows?oldid=13747019)
(28 October 2020). It pins the eight standard stock/limb/string recipes and complete XP split:
bronze level 9 and 12/6 XP, blurite 24 and 32/16, iron 39 and 44/22, steel 46 and 54/27,
mithril 54 and 64/32, adamant 61 and 82/41, rune 69 and 100/50, and dragon 78 and 135/70.
The cutoff crossbow overview
[`14852988`](https://oldschool.runescape.wiki/w/Crossbow_%28weapon%29?oldid=14852988) and pinned
bronze, adamant and dragon unstrung pages
[`14819932`](https://oldschool.runescape.wiki/w/Bronze_crossbow_%28u%29?oldid=14819932),
[`14819937`](https://oldschool.runescape.wiki/w/Adamant_crossbow_%28u%29?oldid=14819937) and
[`14819940`](https://oldschool.runescape.wiki/w/Dragon_crossbow_%28u%29?oldid=14819940) establish
the exact wood/metal pairings, boosted levels, members status, hammer requirement and two-tick limb
attachment. The finished adamant
[`14747195`](https://oldschool.runescape.wiki/w/Adamant_crossbow?oldid=14747195) and dragon
[`14809609`](https://oldschool.runescape.wiki/w/Dragon_crossbow?oldid=14809609) pages independently
pin two-tick stringing. The item pages mark the unfinished and finished items as non-quest items;
The Knight's Sword applies when smithing blurite limbs, not when assembling already-acquired limbs.

Cache 233 pins stocks 9440/9442/9444/9446/9448/9450/9452/21952, limbs
9420/9422/9423/9425/9427/9429/9431/21918, unstrung outputs
9454/9456/9457/9459/9461/9463/9465/21921, finished outputs
9174/9176/9177/9179/9181/9183/9185/21902 and crossbow string 9438. Limb attachment uses tier-specific
sequences 4436–4442 and 7860; stringing uses 6671–6677 and 7961. Both stages use the native
one-product interface 270 / clientscript 2046 quantity flow and atomically produce one result every
two ticks. A normal hammer or either equipped/inventory Imcando hammer satisfies limb assembly.
The conventional filtered success messages are implemented, but their exact punctuation, sound,
menu memory, packet ordering, diary/task credit and wider combat/logout/death interruption still
require matching-client capture.

## Ballista-assembly Fletching

The cutoff Fletching overview
[`14960092`](https://oldschool.runescape.wiki/w/Fletching?oldid=14960092) pins two three-stage
families. At boosted level 47, light frame plus ballista limbs awards 15 XP, incomplete light
ballista plus ballista spring awards 15 XP, and unstrung light ballista plus monkey tail awards
300 XP, for 330 total. At boosted level 72, the corresponding heavy stages award 30, 30 and 600
XP, for 660 total. The ballista-limbs page independently pins the first-stage levels and XP, the
heavy-frame revision
[`14861924`](https://oldschool.runescape.wiki/w/Heavy_frame?oldid=14861924) pins heavy frame plus
limbs, and the unstrung-heavy revision
[`14425161`](https://oldschool.runescape.wiki/w/Unstrung_heavy_ballista?oldid=14425161) pins both
remaining heavy transforms. The components are members' items; the published creation requirements
do not establish a Monkey Madness II state gate for combining already-acquired tradeable parts.

Cache 233 pins finished light/heavy ballistae 19478/19481, frames 19586/19589, shared limbs 19592,
incomplete forms 19595/19598, spring 19601, unstrung forms 19604/19607 and monkey tail 19610. The
cache contains ballista combat sequences, but no evidenced assembly sequence; production therefore
does not misuse a combat animation. Each implemented held-item interaction atomically replaces two
components with one output, works in either order and cancels existing Fletching batches. The Wiki
creation tables leave ticks unknown, so exact delay, messages, sound, packet order, interruption
semantics and any account/activity restrictions remain matching-client blockers. The current
synchronous transform is an explicit unverified presentation boundary, not a verified timing claim.

## Celastrus-battlestaff Fletching

The cutoff Fletching overview
[`14960092`](https://oldschool.runescape.wiki/w/Fletching?oldid=14960092) and the cache-era
[Celastrus bark](https://oldschool.runescape.wiki/w/Celastrus_bark) and
[Battlestaff](https://oldschool.runescape.wiki/w/Battlestaff) creation records establish a
members-only, boostable level-40 recipe that consumes one bark, produces one battlestaff, awards
80 Fletching XP and completes on a four-tick cycle. The bark exposes a native `Fletch` inventory
operation and accepts a knife as its tool; it has no multi-product choice requiring interface 270.
The historical Fletching-knife revision
[`14976006`](https://oldschool.runescape.wiki/w/Fletching_knife?oldid=14976006) and the official
[Final Dawn release post](https://oldschool.runescape.wiki/w/Update:Varlamore:_The_Final_Dawn_Out_Now)
explicitly include battlestaves among actions shortened by one tick after the first item. Therefore
the first result remains four ticks and subsequent results take four ticks with an ordinary knife
or three ticks while the Fletching knife is in the inventory or off hand.

Cache 233 pins Celastrus bark as object 22935 (`celastrus_wood`), battlestaff as object 1391 and
the Fletching knife as object 31043. The revision-matched local RuneLite constants and cache both
identify generic human Fletching sequence 1248; no distinct Celastrus sequence exists. Production
registers the bark's native operation plus both ordinary/Fletching knife held-use routes, rechecks
membership, boosted level, tool and source before every result, atomically replaces the bark even
in a full inventory and participates in shared Fletching cancellation. Exact message text, audio,
packet ordering, spam-click behavior and wider interruption semantics remain matching-client work.

## Toxic-blowpipe creation Fletching

The cutoff Fletching overview
[`14960092`](https://oldschool.runescape.wiki/w/Fletching?oldid=14960092) and the cache-era
[Toxic blowpipe](https://oldschool.runescape.wiki/w/Toxic_blowpipe) creation table establish a
members-only, boostable recipe: use a chisel on one Tanzanite fang at level 78 to create one empty
Toxic blowpipe and award 120 Fletching XP. The requirement was raised from 53 to 78 by
[Project Rebalance on 8 May 2024](https://oldschool.runescape.wiki/w/Update:Project_Rebalance:_Skilling_%26_Poll_81_MTA_Changes),
before the cache-233 cutoff. The creation table reports zero ticks, so the transform is synchronous
and does not open a quantity interface or production queue.

Cache 233 pins Tanzanite fang as object 12922 (`blowpipe_fang`), the empty output as object 12924
(`toxic_blowpipe`) and the distinct charged form as object 12926 (`toxic_blowpipe_loaded`). Both
the cache and revision-matched local RuneLite game values identify creation sequence 3015 as
`snakeboss_fletching`. The implementation registers both chisel/fang held-use orders, cancels any
queued Fletching action, plays that sequence and atomically replaces one fang with the empty form,
including in a full inventory, before awarding 120 XP. It deliberately does not produce object
12926. Exact success/failure wording, sound and animation/inventory/XP packet order remain
matching-client evidence boundaries. Charging and the standard form's inventory lifecycle are
tracked separately below; death and weapon behavior are not part of the creation slice.

## Toxic-blowpipe charging and item lifecycle

The final Toxic-blowpipe article revision before the production cutoff is
[`14978936`](https://oldschool.runescape.wiki/w/Toxic_blowpipe?oldid=14978936), dated 3 September
2025. It establishes that the members-only weapon accepts Zulrah's scales and unpoisoned darts,
holds at most 16,383 of each, returns all remaining darts and scales when uncharged, and may replace
one loaded dart type with another without first unloading. `Unload` recovers the darts separately.
The same revision establishes that dismantling the empty blowpipe returns 20,000 Zulrah's scales
and identifies uncharging sound 2381. It also records the March 2025 bank-side
`Configure-Charges` operation and the October 2020 automatic picked-up-scale behavior; those are
separate integration boundaries rather than silently folded into ordinary inventory use.

Cache 233 pins Zulrah's scales as object 12934 (`snakeboss_scale`). Empty object 12924 exposes
`Dismantle`; loaded object 12926 exposes `Wield`, `Check`, `Unload` and `Uncharge`, and has charge DB
row 4332 (`charges_toxic_blowpipe`). The row's cache table is identity-only and contains no decoded
resource columns. Server varobj symbols 65531–65533 instead define a single persistent packed word:
dart type in bits 0–3, dart count in bits 4–17 and scales in bits 18–31. The two 14-bit fields prove
the independent 16,383 limits. Cache and Wiki evidence do not publish Jagex's internal dart-code
mapping, so the workspace owns an explicit migration contract: original bronze-through-rune metal
darts use codes 1–6, dragon 7, black 8 and amethyst 9, following their historical addition order.
These codes are never derived from enum ordinals or cache item ids and can therefore be translated
when a future cache changes the accepted set.

The standard empty/loaded forms now load all available scales or darts up to the separate caps,
accept either held-use order, and accept exactly the nine unpoisoned cache-233 dart types. Loading a
different type atomically returns the old darts. Check works from inventory and equipment; Unload
returns only darts and preserves scales; confirmed Uncharge returns both resources, changes to the
empty form and plays sound 2381; confirmed Dismantle returns 20,000 scales. Every mutation is one
inventory transaction, including capacity failure and full-inventory replacement, and the exact
object variable survives the repository's existing inventory/equipment and account-persistence
paths. Exact messages, confirmation copy, integer percentage presentation and packet ordering are
server policies pending matching-client capture. Bank `Configure-Charges`, automatic pickup
routing, death/grave behavior, scale/dart combat consumption, depletion-while-equipped and all
ranged bonuses/effects remain explicit open groups.

### Blazing-blowpipe ornament lifecycle

The last pre-cutoff [Trailblazer reloaded blowpipe ornament kit revision
`14787705`](https://oldschool.runescape.wiki/w/Trailblazer_reloaded_blowpipe_ornament_kit?oldid=14787705)
states that the kit changes an empty toxic blowpipe into the blazing blowpipe and that reversing the
process returns both the original item and kit. The last pre-cutoff [Blazing blowpipe revision
`14931538`](https://oldschool.runescape.wiki/w/Blazing_blowpipe?oldid=14931538) pins the empty-only,
members, zero-tick two-input recipe, confirms that the kit changes no bonuses, and gives empty and
loaded IDs 28687 and 28688. It also identifies Found gem as the creation sound and exposes the same
empty `Dismantle`, loaded `Wield`/`Check`/`Unload`/`Uncharge`, and worn `Check` operations as the
standard weapon.

Cache 233 names objects 28687/28688 as `toxic_blowpipe_ornament` and
`toxic_blowpipe_loaded_ornament`, kit 28690 as `toxic_blowpipe_ornament_kit`, and Found gem as synth
2655. Applying the kit consumes exactly the standard empty form and kit, emits the named sound and
creates the empty blazing form in the blowpipe slot, including from a full inventory. The standard
packed state and all scale/dart operations now preserve the blazing style. Uncharging returns to
empty object 28687; dismantling that empty object atomically returns standard empty object 12924
and kit 28690 instead of scales. A full inventory correctly blocks that net-positive dismantle.
Exact application/removal text, dismantle confirmation presentation, packet ordering and the
bank/combat/death/pickup systems remain matching-client or separate-system boundaries.

## Poisoned ammunition

The last pre-cutoff revisions of [Poison
`14994137`](https://oldschool.runescape.wiki/w/Poison?oldid=14994137), [Weapon poison
`14987008`](https://oldschool.runescape.wiki/w/Weapon_poison?oldid=14987008), [Weapon poison(+)
`14991927`](https://oldschool.runescape.wiki/w/Weapon_poison(%2B)?oldid=14991927) and [Weapon
poison(++) `14991890`](https://oldschool.runescape.wiki/w/Weapon_poison(%2B%2B)?oldid=14991890)
establish that members may apply all three poison strengths to arrows, ordinary bolts, darts,
javelins and throwing knives, and that one vial treats five thrown items. Representative cutoff
item recipes pin zero-tick, five-input/one-vial transforms with no skill or XP field: [Bronze arrow
`14993867`](https://oldschool.runescape.wiki/w/Bronze_arrow?oldid=14993867), [Bronze dart
`14993929`](https://oldschool.runescape.wiki/w/Bronze_dart?oldid=14993929), [Bronze javelin
`14982925`](https://oldschool.runescape.wiki/w/Bronze_javelin?oldid=14982925) and [Blurite bolts
`14986493`](https://oldschool.runescape.wiki/w/Blurite_bolts?oldid=14986493). The poison page also
separately names daggers, spears and hastae; those melee transforms, cleaning-cloth reversal and
the resulting combat status are not falsely included in this ammunition slice.

Cache 233 contains exactly 41 supported unpoisoned ammunition bases with all three corresponding
poison identities: eight bronze-through-rune plus amethyst/dragon arrows; eight bronze, blurite,
iron, silver, steel, mithril, adamant and runite bolts; nine bronze-through-rune plus black,
amethyst and dragon darts; eight bronze-through-rune plus amethyst/dragon javelins; and eight
bronze-through-rune plus black/dragon throwing knives. The cache also contains separate 2–5 count-
display objects for some arrows and bolts; these are client quantity models, not additional
recipes, and are deliberately excluded from registration. Weapon poisons are objects 187/5937/
5940 and the returned empty vial is object 229.

The implementation registers all 123 cache-backed ammunition/poison pairs in both held-item
orders and performs an atomic synchronous transaction. It consumes one poison, transforms up to
five items, returns an empty vial, awards no XP, does not invent an animation and cancels queued
Fletching work. The historical recipes explicitly show batches of five but do not state whether a
smaller available stack is accepted; accepting one to five while consuming the vial is therefore
an implementation inference pending workspace-client capture. Exact success/failure text, sound,
inventory packet ordering and full interruption behavior remain capture boundaries.

## Bolt-tip cutting Fletching

The cutoff bolt-tip table is
[`14765931`](https://oldschool.runescape.wiki/w/Template:Table/Fletching/Bolt_tips?oldid=14765931).
Because that revision incorrectly repeats some attachment XP values, the implementation uses the
individually pinned item revisions as the detailed authority: opal
[`14470022`](https://oldschool.runescape.wiki/w/Opal_bolt_tips?oldid=14470022), jade
[`14470126`](https://oldschool.runescape.wiki/w/Jade_bolt_tips?oldid=14470126), pearl
[`14470036`](https://oldschool.runescape.wiki/w/Pearl_bolt_tips?oldid=14470036), topaz
[`14470068`](https://oldschool.runescape.wiki/w/Topaz_bolt_tips?oldid=14470068), sapphire
[`14470078`](https://oldschool.runescape.wiki/w/Sapphire_bolt_tips?oldid=14470078), emerald
[`14470141`](https://oldschool.runescape.wiki/w/Emerald_bolt_tips?oldid=14470141), ruby
[`14514111`](https://oldschool.runescape.wiki/w/Ruby_bolt_tips?oldid=14514111), diamond
[`14470035`](https://oldschool.runescape.wiki/w/Diamond_bolt_tips?oldid=14470035), dragonstone
[`14714347`](https://oldschool.runescape.wiki/w/Dragonstone_bolt_tips?oldid=14714347) and onyx
[`14982044`](https://oldschool.runescape.wiki/w/Onyx_bolt_tips?oldid=14982044). They establish the
boosted level / yield / XP-per-source contracts: opal 11/12/1.5, jade 26/12/2, oyster pearl
41/6/3.2, oyster pearls 41/24/3.2, topaz 48/12/3.9, sapphire 56/12/4, emerald 58/12/5.5, ruby
63/12/6.3, diamond 65/12/7, dragonstone 71/12/8.2 and onyx 73/24/9.4. Each source takes five ticks.

The pinned dragonstone transcript
[`14714344`](https://oldschool.runescape.wiki/w/Transcript:Dragonstone_bolt_tips?oldid=14714344)
provides the exact warning title, choices, success line and decline line. No equivalent warning is
evidenced for onyx. Cache 233 pins sources 411/413/1601/1603/1605/1607/1609/1611/1613/1615/6573,
outputs 45/46/9187–9194, generic quantity interface 270/clientscript 2046, gem sequences 885–892
and 2717, and pearl sequence 4470. The revision-matched workspace RuneLite gameval constant names
4470 `XBOWS_FLETCHING_PEARL`. All eleven recipes use a native one-product quantity selection and
atomically replace one source every five ticks. Exact quantity-memory behavior, sound, packet
ordering and wider movement/combat/interface/logout/death interruption remain matching-client work.
Amethyst bolt tips are deliberately excluded: cutoff revision
[`14470298`](https://oldschool.runescape.wiki/w/Amethyst_bolt_tips?oldid=14470298) defines them as a
separate level-83 Crafting selection that yields 15 tips for 60 XP every two ticks.

## Amethyst-cutting Crafting

The cutoff Crafting overview
[`14996237`](https://oldschool.runescape.wiki/w/Crafting?oldid=14996237) and its experience table
establish one amethyst plus a chisel as the shared input and the boosted level / XP contracts:
bolt tips 83/60, arrowtips 85/60, javelin tips 87/60 and dart tips 89/60. The individually pinned
bolt-tip page [`14470298`](https://oldschool.runescape.wiki/w/Amethyst_bolt_tips?oldid=14470298)
pins its 15-output yield and two-tick action. The cutoff arrow ammunition data pins 15 arrowtips,
the javelin-tip table pins five tips per amethyst, and Jagex's ranged-meta proposal explicitly pins
eight dart tips and 60 Crafting XP per amethyst. These sources form the implemented
15/15/5/8-output family; all four actions award 60 XP and take two ticks.

Cache 233 pins amethyst 21347, chisel 1755, outputs 21338/21350/21352/25853, generic quantity
interface 270 / clientscript 2046 and the dedicated sequence 6295 `human_amethystcutting`. The
implementation presents all four products in level order, atomically replaces one non-stackable
amethyst even in a full inventory, and shares the central Crafting cancellation policy. Exact menu
memory, success/failure text, sound identity, packet ordering and wider
movement/combat/interface/logout/death interruption still require matching-client capture.

## Prayer remains: burial and scattering

The last pre-cutoff [Remains revision
`14995029`](https://oldschool.runescape.wiki/w/Remains?oldid=14995029) supplies the ordinary burial
and scattering XP tables. It pins regular/wolf/burnt bones at 4.5 XP; monkey 5; bat 5.3; big and
jogre 15; wyrmling 21; zogre 22.5; shaikahan 25; baby dragon 30; wyrm 50; dragon and wyvern 72;
drake 80; fayrg 84; lava dragon 85; raurg 96; hydra 110; dagannoth 125; ourg 140; and superior
dragon 150. The same revision pins untradeable remains at 3 XP for Alan's and Ape Atoll skeleton
bones, 4.5 for bleached bones, 5 for small/large zombie-monkey bones, 16 for small ninja-monkey
bones, and 18 for medium ninja, gorilla and bearded-gorilla bones. Its ash table pins fiendish,
vile, malicious, abyssal and infernal ashes at 10/25/65/85/110 XP. Long and curved bones are each
15.1 XP in their pinned individual revisions. [Superior dragon bones revision
`14937007`](https://oldschool.runescape.wiki/w/Superior_dragon_bones?oldid=14937007) establishes an
unboostable Prayer level 70 requirement.

[Pay-to-play Prayer training revision
`14976340`](https://oldschool.runescape.wiki/w/Pay-to-play_Prayer_training?oldid=14976340)
corroborates the members tables. [Free-to-play Prayer training revision
`14979693`](https://oldschool.runescape.wiki/w/Free-to-play_Prayer_training?oldid=14979693)
documents that a bone and ash can be buried and scattered in one tick with two quick clicks. The
implementation therefore uses separate two-tick weak cooldowns for bones and ashes and awards XP
immediately, allowing exactly that combined action without allowing repeated same-family actions.

The cache-233 object-operation audit finds exactly 55 objects with Bury or Scatter: 40 ordinary
remains handled by the shared plugin and 15 quest/minigame objects (610, 3127–3133, 4488, 25199 and
26589–26593) explicitly reserved for their owning scripts. Cache varbit 16616 identifies the
long/curved-bone warning preference. The revision-matched workspace RuneLite constants identify
bone sequence 827 and synth 2738. The exact historical warning copy and checkbox protocol are not
published, so the current two-choice warning is functional but not client-verified. The Wiki's
scattering animation shows a distinct ash action, but no authoritative sequence/synth identity was
found; the ash path deliberately avoids inventing either until a matching-client capture pins it.

## Prayer bone offerings and Zealot preservation

The pre-cutoff [pay-to-play Prayer training table](https://oldschool.runescape.wiki/w/Pay-to-play_Prayer_training)
and the [gilded altar mechanics](https://oldschool.runescape.wiki/w/Gilded_altar) establish the POH
chapel progression. Oak, teak, cloth, mahogany, limestone, marble and gilded altars start at
100/110/125/150/175/200/250% of burial XP; each lit incense burner adds 50 percentage points, to a
maximum of two. This produces the canonical gilded-altar 250/300/350% outcomes. Manual offerings
complete after one game tick and automatic offerings repeat every three ticks. The post-2023,
pre-cutoff behavior consumes the first inventory bone of the selected type in top-left order at a
POH altar, while automatic offering remains locked to that type.

Cache 233 resolves 21 Saradomin/Guthix/Zamorak chapel altars 13179–13199 and seven gnome-child-icon
variants 40872–40878 into the same seven tiers. The [gnome child icon construction
page](https://oldschool.runescape.wiki/w/Gnome_child_icon_(Construction)) corroborates that the icon
changes the altar appearance rather than its XP tier. Only lit incense-burner locations
13209/13211/13213 contribute; unrelated lit chapel lights are deliberately excluded. Cache sequence
3705 (`human_bone_sacrifice`) and spot animation 624 (`poh_bone_sacrifice`) provide the offering
presentation. No authoritative offering sound was found, so none is invented.

The same training source pins the Chaos altar to 350% XP with an independent 50% chance not to
consume the offered bone. Because location 411 is reused, the handler is restricted to cache zone
368,477,0 (absolute tiles 2944–2951,3816–3823), containing the level-38 Wilderness temple altar;
other placements return the gods'-wrath rejection. Unlike POH selection, a manual Chaos offering
revalidates and consumes the exact clicked inventory slot. Its save roll is evaluated before any
equipment preservation roll.

The [Zealot's robes mechanics](https://oldschool.runescape.wiki/w/Zealot%27s_robes) give every worn
piece an independent aggregate contribution of 1.25%, or 5% for the full set, when burying or
offering bones. Cache objects 25434/25436/25438/25440 are the top, bottom, helm and boots. The
implementation makes one denominator-80 roll against the worn-piece count, does not apply the set
to ashes, and evaluates it only after a failed Chaos-altar save. This yields the documented 52.5%
combined full-set preservation chance at the Chaos altar.

## Prayer-point restoration altars

The pre-cutoff [Prayer](https://oldschool.runescape.wiki/w/Prayer) and
[Prayer points](https://oldschool.runescape.wiki/w/Prayer_points) mechanics state that ordinary
altars restore Prayer points to the player's maximum. They identify exactly three above-maximum
families: the Edgeville Monastery altar, the Nature Grotto altar and God Wars Dungeon boss-room
altars. The [Altar catalogue](https://oldschool.runescape.wiki/w/Altar) further identifies the
ordinary altar families and records the 5 April 2023 change that praying resets accumulated
Prayer-point drain progress.

Cache 233 exposes 84 locations with a Pray or Pray-at operation. The permanent integration audit
partitions them without overlap into 63 ordinary restoration locations, the two +2 locations, five
God Wars locations, two hazardous locations, one utility-restoration location, two spellbook
altars, three Mage Arena statues, one transport altar, one non-restoring altar, one non-production
altar and three Goblin Temple components. The ordinary set includes both interactive Devious
Minds states of the [Entrana altar](https://oldschool.runescape.wiki/w/Altar_%28Entrana%29), which
the Wiki identifies as a normal Saradomin recharge altar, and the repaired Tribal Statue unlocked
through Tai Bwo Wannai Trio. The hazardous set contains the Forthos Cursed Altar and Darkmeyer
statue. The spellbook set contains the Ancient Pyramid and Astral altars. The special set contains
the three clickable pieces of one composite Land of the Goblins altar, not three quest states.
None is allowed through the unconditional generic restore path.
The non-restoring set contains the broken sun altar, and the non-production set contains the
unrestricted-world spellbook altar. The transport set contains the Yanille trapdoor altar. The
[Chaos altar](https://oldschool.runescape.wiki/w/Chaos_Altar_%28Prayer%29)
confirms that location 411 restores Prayer while the separate Yanille location 412 opens a
damaging trapdoor. The [Forthos Dungeon](https://oldschool.runescape.wiki/w/Forthos_Dungeon)
confirms that its normal altar restores Prayer, its cursed state drains all Prayer and deals up to
30 damage, and its broken altar does not restore Prayer.

The [Cursed Altar](https://oldschool.runescape.wiki/w/Cursed_Altar) applies damage equal to the
Prayer points drained, with a minimum of 5 and maximum of 30; this remains damaging at zero Prayer
and may kill the player. Cache location 34836 is therefore handled as a direct typeless hit after
Prayer is reduced to zero, rather than routed through ordinary restoration. The
[Darkmeyer](https://oldschool.runescape.wiki/w/Darkmeier) city catalogue identifies its vampyric
statue as a full Prayer restore at the cost of a random 1–3 Hitpoints. Cache location 39234 uses the
ordinary face/sequence/drain-reset restoration path before applying that direct cost. Presentation
beyond the established recharge message remains revision-matched client-capture work.

The [Spirits of the Elid](https://oldschool.runescape.wiki/w/Spirits_of_the_Elid) reward and the
[cutoff Elidinis Statuette revision](https://oldschool.runescape.wiki/w/Elidinis_Statuette?oldid=14925273)
establish that the placed Statuette restores Prayer, Hitpoints, run and special energy, cures
poison and venom, and applies a visible +7 Hitpoints boost after lost Hitpoints are restored. That
same cutoff revision directly states that PvP worlds do not restore Hitpoints, and that Deadman
restores all normal benefits except the additional +7 boost. Cache quest row 139 pins completion to state
60 of `elidquest` varbit 1444, backed by `elid_main` varp 616. Cache location 10439 is the placed
statuette. Poison state uses cache varps 102 and 3777 plus type varbit 6251; the workspace RuneLite
independently identifies varp 102 and varbit 6251 for poison/venom display. The operation preserves
an existing Hitpoints value above base +7 rather than lowering it on standard worlds.

The revision-matched workspace RuneLite `WorldType` catalogue pins the PvP and Deadman masks to
`1 << 2` and `1 << 29` respectively, along with the remaining cache-233 world-list flags. RSMod's
existing `worlds.flags` column is loaded for the selected `ServerConfig.world` into the immutable
`RealmConfig` snapshot. Gameplay reads that snapshot rather than introducing a second world-mode
store. Integration coverage verifies standard-world +7, PvP Hitpoints preservation and Deadman
base-only healing while Prayer, run energy, special energy and poison/venom restoration remain
normal.

The quest page also identifies praying at the Statuette as a Medium Desert Diary task. The task
should be credited only when at least one Prayer point is restored. Cache varps 1198/1199 contain
the Desert Diary state, but this repository does not yet expose a diary-task service or a named
per-task bit. That credit remains explicitly researched rather than writing an unverified packed
bit directly. Exact success/failure text and audiovisual/packet ordering also remain capture work.

The [cutoff Astral Altar revision](https://oldschool.runescape.wiki/w/Astral_Altar?oldid=14960697)
is cache location 34771 with
`Craft-rune` in operation 1 and `Pray` in operation 2. The Wiki requires completed Lunar Diplomacy,
switches every non-Lunar book to Lunar and switches Lunar back to Standard, gives the exact messages
`Lunar spells activated!` and `Lunar spells deactivated!`, and explicitly does not restore Prayer.
Cache quest row 88 pins completion to state 190 of `lunar_quest_main` varbit 2448.

The [Desert Treasure I](https://oldschool.runescape.wiki/w/Desert_Treasure_I) reward and
[Ancient Pyramid](https://oldschool.runescape.wiki/w/Ancient_Pyramid) establish that cache location
6552 switches every non-Ancient book to Ancient and switches Ancient back to Standard after quest
completion, drains all Prayer and remains usable at zero Prayer. Cache quest row 27 pins completion
to state 15 of `deserttreasure` varbit 358 backed by varp 440. The
[cutoff altar-specific Wiki revision](https://oldschool.runescape.wiki/w/Altar_%28Ancient_Pyramid%29?oldid=14918351) pins
`You feel a strange wisdom fill your mind...` when activating Ancient and `You feel a strange drain
upon your memory...` when returning to Standard. Both altars use the cache spellbook varbit
4070 values Standard 0, Ancient 1, Lunar 2 and Arceuus 3 and the established prayer sequence 645.
The pyramid clears accumulated fractional Prayer-drain state when setting Prayer to zero; Astral
preserves both current Prayer and that fractional state. The
[cutoff Autocast revision](https://oldschool.runescape.wiki/w/Autocast?oldid=14930998) says the
selection is remembered except when equipping an incompatible staff or equipping a staff in a PvP
area; a permanent spellbook switch is not an exception. Both altars therefore preserve the active
autocast spell and defensive-mode state plus the saved spell and defensive-mode state for ordinary
and bladed staff categories across the outbound and return switch. Exact pre-completion feedback,
the Desert hard-task credit for praying twice, and audiovisual/packet ordering remain
client-capture or diary-system work.

The [God capes](https://oldschool.runescape.wiki/w/God_capes) catalogue establishes that, after
Mage Arena I, praying at each of the Saradomin, Guthix and Zamorak statues makes its matching cape
appear on the floor and that multiple copies may be claimed from the same statue. Cache locations
2873, 2875 and 2874 expose `Pray-at`; cache objects 2412, 2413 and 2414 are the corresponding capes.
The existing `magearena` varp 267 is also the server's Mage Arena I completion gate for Charge.
Each successful operation faces the statue, uses cache sequence 645, leaves Prayer unchanged and
creates the cape as a player-owned private floor object at the player's interaction tile. Exact
presentation and production ground-item visibility timing remain matching-client capture work.

The [Chaos altar](https://oldschool.runescape.wiki/w/Chaos_Altar_%28Prayer%29) identifies the
Yanille Agility Dungeon altar as a trapdoor that damages the player and drops them to the lower
poison-spider level. Contemporary player reports consistently reproduce a fixed 15-damage fall,
including death when starting at 10 Hitpoints. Legacy world-coordinate references place the spider
pit at 2578,9578; the workspace RuneLite hard-clue definition independently locates the searchable
crate in that same chamber at 2576,9583. Cache location 412 therefore faces the altar, uses the
established prayer sequence 645, applies a direct typeless 15-damage hit and telejumps to
2578,9578 without restoring or draining Prayer. The precise trapdoor animation, movement,
hitmark and packet ordering remain matching-client capture work.

The [Broken sun altar](https://oldschool.runescape.wiki/w/Broken_sun_altar) is cache location
34872. It never restores Prayer and sends the exact message `You pray at the altar, nothing
interesting happens.` Cache sequence symbols pin ordinary `human_pray` to 645 and
`human_sun_prayer` to 8321. The cache morph for Knight of Varlamore NPC 8720 is controlled by
`varlamore_sun_knight` varbit 8409 and reveals NPC 8712 at value 1. The handler therefore faces the
altar and uses sequence 645 while that bit is clear, then sequence 8321 once the knight is present,
without changing current Prayer or fractional drain progress.

The [unrestricted-world altar](https://oldschool.runescape.wiki/w/Altar_%28unrestricted_world%29)
is cache location 7814 and exposes `Pray-at`, `Standard`, `Ancient`, `Lunar` and `Arceuus`. The
source explicitly limits it to temporary unrestricted worlds and identifies the ordinary Ferox
Enclave altar as its production replacement. It is retained in the cache catalogue and tested as
a deliberate no-handler production exclusion; implementing its spellbook operations in the normal
world would add non-production behavior to the 233.4 baseline.

The [Goblin Temple altar](https://oldschool.runescape.wiki/w/Altar_%28Goblin_Temple%29) documents
the exact early response `The Big High War God does not answer your prayer`, then identifies the
same altar as a normal Prayer restore later in Land of the Goblins and as useful between the five
high-priest encounters. Cache locations 43089, 43097 and 43098 are the three `Pray-at` pieces of
that single composite altar; the seven intervening component locations have no Pray operation and
none of the ten locations has a varbit/varp morph. Cache varbit 13599 `lotg`, backed by persistent
varp 3355, holds the quest state, and quest row 165 pins completion to 56. The last public Quest
Helper state map before the production cutoff (commit `f0fa06bcc5426c632bb5ce67db5cf431718f06ea`)
places unlocking the crypt before state 36 and the successive high-priest encounters at states
36–48. Combining that stage map with the Wiki's between-encounters description makes state 36 the
best supported restoration boundary; it is explicitly an inference rather than a cache-authored
threshold. The integration therefore preserves Prayer and drain progress and sends the exact
no-answer message at states 0 and 35, then uses ordinary restoration at states 36 and 56 across
all three clickable pieces. Exact animation timing, sound and packet order remain client-capture
work.

Ordinary altars restore to base Prayer and remove an existing overcharge; the Monastery and Nature
Grotto restore to base +2. Every implemented path faces the altar, uses cache sequence 645
`human_pray`, resets the server's fractional `prayer_drain_counter` state and sends the established
recharge message. No sound was added because no authoritative cache sound identity was found.

The cutoff [God Wars Dungeon revision
14996789](https://oldschool.runescape.wiki/w/God_Wars_Dungeon?oldid=14996789) documents a ten-minute
shared cooldown, an out-of-combat requirement and +1 Prayer above maximum for each worn item
aligned with the altar's god, capped naturally by the eleven equipment slots. Cache varbit 4099
`godwars_altar_delay` is the revision-matched client timer state; the workspace RuneLite renders
each unit as 100 milliseconds. [Nex strategies](https://oldschool.runescape.wiki/w/Nex/Strategies)
adds that the Zaros altar restores Hitpoints, run energy and special attack energy when at least one
Zarosian item is worn. Cache params 40/41/42 classify the 78 wearable Saradomin, 109 Zamorak and 89
Guthix objects. Saradomin and Zamorak restoration therefore use the cache flags directly. Armadyl,
Bandos and Zaros use explicit 34-, 30- and 62-object catalogues assembled from the cutoff Wiki god
families and cache identities; this includes charged/uncharged variants, ornamented variants,
Deadman Armadyl godsword and Echo Virtus, while excluding LMS, rotten, beta and unobtainable copies.

The authoritative cooldown deadline is persisted in server-only varp 65502 as seconds since
1 January 2025. A successful use sets deadline +600 seconds and cache varbit 4099 to 10; rejected
uses and reconnects reconstruct its ceiling-minute client value while the deadline retains exact
server enforcement. The five locations share that deadline. Every successful restoration uses the
ordinary sequence/drain-reset path and adds the number of matching worn items to base Prayer. The
Zaros altar additionally restores Hitpoints to the base level, run energy to 100% and special
energy to 100% only when at least one Zaros item is worn.

Each altar also exposes `Teleport` in cache operation 2. Revision 14996789 explicitly says this
operation is usable even during combat and places the player immediately outside the one-way boss
door. Cache-233 map placements pin the exterior destinations to Zamorak 2925,5332,2; Saradomin
2908,5265,0; Armadyl 2839,5295,2; and Bandos 2863,5354,2. The pre-cutoff [Ancient Prison altar
revision 14647400](https://oldschool.runescape.wiki/w/Altar_%28Ancient_Prison%29?oldid=14647400)
instead specifies the entrance outside Ashuelot Reis's essence-gated safe room. Cache map square
45,81 places that room's inner prison door at 2899,5203 and the altar at 2939,5202; the production
exit tile is 2900,5204,0. Operation 2 performs an immediate telejump with no combat or cooldown
gate and leaves Prayer, Hitpoints, run energy, special energy, fractional Prayer drain and both
altar cooldown values unchanged. Exact production audiovisual and packet ordering remain
matching-client capture work.

## Firemaking ordinary combustible logs

The cutoff [Firemaking revision
14960094](https://oldschool.runescape.wiki/w/Firemaking?oldid=14960094), dated 6 August 2025,
provides the last ordinary-log table before cache 233.4. It pins normal logs at level 1 and 40 XP,
achey at 1/40, oak at 15/60, willow at 30/90, teak at 35/105, arctic pine at 42/125, maple at
45/135, mahogany at 50/157.5, yew at 60/202.5, blisterwood at 62/96, magic at 75/303.8 and redwood
at 90/350. Achey, teak, arctic pine, mahogany, blisterwood, magic and redwood are members-only;
temporary visible boosts may satisfy the level requirement. The same revision documents using a
tinderbox and logs in either inventory order, lighting a ground log through `Light` or tinderbox
targeting, one attempt every four ticks and repeated attempts until success.

The cutoff chance chart is represented exactly by 256-way integer rolls: successful outcomes are
`min(256, round(64 + (level - 1) * 448 / 98) + 1)`. This yields 65 successful values at level 1,
70 at level 2, 74 at level 3, 252 at level 42 and guaranteed success from level 43 onward. Tests pin
both the interpolation boundaries and failure/success roll edges.

The cutoff [Fire revision
14981396](https://oldschool.runescape.wiki/w/Fire?oldid=14981396), dated 7 September 2025,
documents a random 60–119-second lifetime independent of log type, followed by ashes. At the
600-millisecond server cycle this maps to an inclusive 100–199-cycle lifetime. It also establishes
the step-off preference west, east, south, north and remaining on the tile when every direction is
blocked, plus the general restrictions against existing fires, scenery, plants/ferns and closed
door fronts.

Fire placement now resolves complete rotated loc footprints rather than checking only the loc's
south-west origin. It treats diagonal full-tile locs, both centrepiece shapes and ground decoration
as floor scenery, which covers multi-tile scenery and non-blocking plants/ferns across zone
boundaries. Boundary walls, wall decorations and roof shapes do not occupy the floor and remain
legal. Generic opened diagonal doors are excluded from the full-tile rule. Closed single, left
double and right double-door content groups instead use the same `DoorTranslations.translateOpen`
geometry as the generic door plugin, so the exact tile onto which the opened model would move is
blocked in both supported shapes and every orientation while an opened door is legal.

Revision 14981396 publishes exact level-0 map polygons for three unusual restrictions. The
Dwarven restriction is the concave polygon
`(2944,3417)→(3072,3417)→(3072,3520)→(3008,3520)→(3008,3456)→(2880,3456)→(2880,3520)→(2944,3520)`
and sends `The dwarves won't be happy if you light a fire here.`. The area around Gilbert is the
inclusive rectangle 3118–3136 by 3390–3408 and sends
`A colourful magical force won't let you light a fire here.`. Jiggig is the inclusive rectangle
2432–2496 by 3008–3072 and sends `The ogres won't be happy if you light a fire here.`. Unit tests
pin boundary inclusion, concavity and plane isolation; cache-aware integration verifies atomic
inventory rejection and message precedence.

The same cutoff article explicitly marks its prohibited-area section incomplete. It names most
banks, Duel Arena and Pest Control with the generic rejection, then gives exact messages for Blast
Furnace, the Gauntlet lobby, Sisterhood Sanctuary, the Tempoross boat, Theatre of Blood, Tombs of
Amascut, the TzHaar Fight Cave/Pit and Mor Ul Rek, Zulrah's Shrine, God Wars Dungeon, the Killerwatt
plane and the Chasm of Fire around Yama. It does not publish complete tile polygons for those
areas. Names and representative coordinates are insufficient proof of their boundaries,
instance-normalisation policy or overlap priority, so they remain a researched blocker instead of
being implemented as guessed rectangles. Sailing-boat and Shellbane Gryphon Cave rows on the
current page postdate cache 233 and are deliberately excluded.

Cache 233.4 pins tinderbox object 590, ashes 592, fire location 26185 and all 12 log identities.
Each log exposes `Light` in ground-object operation 4. Local cache sequence symbols and the
revision-matched workspace RuneLite agree on firemaking sequence 733; RuneLite's sound catalogue
pins the tinder strike to synth 2597. Integration tests resolve these identities and operation
slots from the cache rather than accepting raw configuration assumptions.

## Coloured fires and the gnomish firelighter

The cutoff [Gnomish firelighters revision
14856213](https://oldschool.runescape.wiki/w/Gnomish_firelighters?oldid=14856213) defines five
stackable members' firelighters. A loose red, green, blue, purple or white firelighter used on
normal logs immediately produces the matching coloured logs and sends
`You coat the logs with the [colour] chemicals.`. Lighting those logs awards 50 Firemaking XP and
always succeeds on the first attempt. Cache 233.4 resolves the loose firelighters as
7329/7330/7331/10326/10327, coloured logs as 7404/7405/7406/10329/10328 and matching fires as
26186/26575/26576/20001/20000 in red/green/blue/purple/white order. The coloured logs expose the
same ground `Light` operation slot as ordinary logs and use the ordinary temporary-fire,
movement and ashes lifecycle.

The revision-matched Firemaking experience table lists every coloured log at level 1 and 50 XP
with a tinderbox and level 21 and 50 XP with a bow. Bow lighting therefore uses the completed
Barbarian-training gate and audiovisual route while retaining the coloured fire identity and
guaranteed first attempt. The loose conversion and lighting implementations cover both inventory
orders, ground routes, membership rejection and transactional fire creation across all five
colours.

The cutoff [Gnomish firelighter revision
14685884](https://oldschool.runescape.wiki/w/Gnomish_firelighter?oldid=14685884) pins uncharged
object 20275 and charged object 20278. It defines one stored colour at a time, a maximum of 1,000
charges, use with normal logs only, one consumed charge for each coloured fire created, and
Uncharge returning every unused loose firelighter. An empty holder is not a tinderbox. Cache 233.4
independently pins inventory operation 4 `Check` on both forms and operation 5 `Uncharge` on the
charged form. The holder produces the selected colour's guaranteed 50-XP fire directly from a
normal log, including explicit holder-on-ground-log targeting and ground `Light` fallback.

Neither holder cache object defines a charge parameter or cache varobj layout. The server therefore
packs the persistent per-item count into custom varobj 65527 bits 0–9 and colour code into custom
varobj 65528 bits 10–12; tests round-trip all five colours and the 1,000-charge boundary. Loading,
top-up and uncharging use inventory transactions, and a charge is consumed only after both fire
registration and ground-log deletion commit. A changed or missing holder during the four-tick
attempt cancels safely without deleting the log or granting XP.

The cutoff pages do not record the holder's exact load, Check, Uncharge, full-capacity,
wrong-colour, full-inventory or final-charge feedback, nor do they unambiguously expose the client
presentation for loading a stack. The implementation loads the available same-colour stack up to
the documented cap and uses functional feedback; those presentation details remain explicit
cache-233.4 production-capture requirements. The final-charge text currently follows later
observational evidence, `Your Gnomish firelighter has run out of charges.`, but is not treated as
cutoff verification.

## Pyre-log preparation

The cutoff [Pyre logs revision
14917219](https://oldschool.runescape.wiki/w/Pyre_logs?oldid=14917219), [Sacred oil revision
14993832](https://oldschool.runescape.wiki/w/Sacred_oil?oldid=14993832) and [Shades of Mort'ton
minigame revision
14979466](https://oldschool.runescape.wiki/w/Shades_of_Mort%27ton_(minigame)?oldid=14979466)
define a members-only, level-1 preparation activity unlocked by completion of Shades of Mort'ton.
Cache database row 128 column 19 resolves that quest's completed state to 85, matching varp 339
`morttonquest`. The quest's own sequence is an important exception: revision-matched cache data and
the Quest Helper state map place sacred-oil/normal-pyre-log preparation at state 65, before the
corpse-burning steps at states 70 and 75. The implementation therefore permits only normal logs at
states 65–84; all other source logs remain completion-gated until state 85.

Each sacred-oil dose awards a flat 5 Firemaking XP, so the exact cutoff recipes are:

| Source logs | Doses | XP | Output object |
| --- | ---: | ---: | ---: |
| Normal or achey | 2 | 10 | 3438 |
| Oak | 2 | 10 | 3440 |
| Arctic pine | 2 | 10 | 10808 |
| Willow | 3 | 15 | 3442 |
| Teak | 3 | 15 | 6211 |
| Maple | 3 | 15 | 3444 |
| Mahogany | 3 | 15 | 6213 |
| Yew | 4 | 20 | 3446 |
| Magic | 4 | 20 | 3448 |
| Redwood | 4 | 20 | 19672 |

Cache 233.4 resolves sacred oil (4), (3), (2) and (1) as objects 3430, 3432, 3434 and 3436.
A successful conversion replaces the selected oil with the correctly reduced dose or an empty vial
and replaces the source log in place, so a full inventory is legal and atomic. The dedicated
pre-cutoff Arctic-pyre-log recipe is decisive at two doses; the broader cutoff Firemaking article's
three-dose row conflicts with both that dedicated recipe and the pyre-log page and is not used.

The [31 July 2024 quality-of-life
update](https://oldschool.runescape.wiki/w/Update:QoL_Improvements_%26_Further_Deadman:_Armageddon_Tweaks)
added Make-All and standardised preparation at 5 XP per dose. Cache interface 270 and client script
2046 provide the repository's native quantity selector. Its maximum is constrained by both source
logs and the total number of complete dose groups across fragmented oil vials. The first selected
pair converts immediately; unattended continuation converts one more pair every four ticks. The
[Firemaking training guide](https://oldschool.runescape.wiki/w/Pay-to-play_Firemaking_training)
also records the production one-tick manual path: holding the selector shortcut while repeatedly
using oil on logs creates one on each new interaction without a tick delay.

The Pyromancer hood, garb, robe and boots are cache objects 20708, 20704, 20706 and 20710 and apply
0.4%, 0.8%, 0.6% and 0.2% respectively. Wearing all four adds the 0.5% full-set bonus, for 2.5% in
total. These bonuses apply to pyre-log preparation as ordinary Firemaking XP; warm gloves 20712 do
not count. This differs from the later funeral-pyre cremation activity, which does not receive the
outfit bonus.

The cutoff pages do not expose exact feedback strings, a preparation sequence or sound, packet
ordering, or the client's automatic choice among several eligible oil vials after the originally
selected vial. The implementation preserves the explicitly selected inputs for the immediate
conversion and then chooses eligible oil and log slots deterministically from top left; those
presentation and ordering details remain matching-client capture work rather than verified facts.

## Shades of Mort'ton funeral pyres

The cutoff [Funeral Pyre revision
14840941](https://oldschool.runescape.wiki/w/Funeral_Pyre?oldid=14840941), [Stone stand revision
14919470](https://oldschool.runescape.wiki/w/Stone_stand?oldid=14919470), [Shades of Mort'ton
minigame revision
14979466](https://oldschool.runescape.wiki/w/Shades_of_Mort%27ton_(minigame)?oldid=14979466), and
the six cutoff remains revisions ([Loar 14905147](https://oldschool.runescape.wiki/w/Loar_remains?oldid=14905147),
[Phrin 14905148](https://oldschool.runescape.wiki/w/Phrin_remains?oldid=14905148),
[Riyl 14905149](https://oldschool.runescape.wiki/w/Riyl_remains?oldid=14905149),
[Asyn 14905145](https://oldschool.runescape.wiki/w/Asyn_remains?oldid=14905145),
[Fiyr 14905146](https://oldschool.runescape.wiki/w/Fiyr_remains?oldid=14905146), and
[Urium 14905150](https://oldschool.runescape.wiki/w/Urium_remains?oldid=14905150)) define the core
cremation contract. It is members-only and normally requires quest completion. Cache varp 339 and
database row 128 column 19 pin completion at state 85. Quest Helper commit
`633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a` is post-cutoff corroboration, not cutoff authority; its
state map identifies sacred-oil preparation at 65, corpse burning at 70 and 75, the post-burn step at
80, and completion at 85. The quest-owned exception is therefore restricted to normal pyre logs
and Loar remains from state 70; normal repeatable training remains completion-gated.

Cache 233.4 location 4093 is the empty pyre with `Build`; loading a log replaces it with one of
4094–4099, 9006, 9007, 21271 or 28865 with `Use`; adding remains replaces that stage with one of
4100–4105, 9008, 9009, 21272 or 28866 with `Light`. Cache sequences 1303, 1304 and 1305 are the
no-wood, no-bones and fire actions, and spot animation 292 is the pyre fire. The six Wiki map-pin
centres are `(3465,3294)`, `(3465,3282)`, `(3467,3270)`, `(3476,3268)`, `(3500,3268)` and
`(3507,3275)`. Location 4065 is the stone stand; each pyre has four stands at cardinal offsets of
four tiles. The 31 July 2024 update made left-click Build and Use automatically choose the best
appropriate carried log and remains while preserving manual item-on-location use.

The exact Firemaking requirements and XP are:

| Pyre logs | Level | Firemaking XP |
| --- | ---: | ---: |
| Normal | 5 | 50 |
| Oak | 20 | 70 |
| Willow | 35 | 100 |
| Teak | 40 | 120 |
| Arctic pine | 47 | 158 |
| Maple | 50 | 175 |
| Mahogany | 55 | 210 |
| Yew | 65 | 255 |
| Magic | 80 | 404.5 |
| Redwood | 95 | 500 |

Prayer XP is determined by both remains and log tier; a dash is an invalid combination:

| Remains | Normal | Oak | Willow | Teak | Arctic | Maple | Mahogany | Yew | Magic | Redwood |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Loar | 25 | 33 | 33.5 | 33.7 | 33.9 | 34 | 34.3 | 34.5 | 35 | 35.5 |
| Phrin | 37.5 | 45.5 | 46 | 46.2 | 46.4 | 46.5 | 46.8 | 47 | 47.5 | 48 |
| Riyl | — | — | 61 | 61.2 | 61.4 | 61.5 | 61.8 | 62 | 62.5 | 63 |
| Asyn | — | — | — | — | — | — | — | 79.5 | 80 | 80.5 |
| Fiyr | — | — | — | — | — | — | — | — | 100 | 100.5 |
| Urium | — | — | — | — | — | — | — | — | — | 120.5 |

The Morytania hard diary adds 50% Prayer XP and the elite diary independently adds 50% Firemaking
XP; cache varbits 4489 and 4490 carry those completion flags. The Pyromancer outfit explicitly does
not boost cremation XP.

Each cremation awards coins on 21% of rolls and otherwise one weighted key. Coins and keys are
placed on a nearby stone stand, can stack, remain private to the cremating player for their entire
five-minute lifespan, and then despawn without a public-reveal phase. The exact reward table is:

| Remains (object) | Coins | Weighted key outcomes |
| --- | --- | --- |
| Loar (3396) | 200–300 | Bronze red 35, brown 30, crimson 21 |
| Phrin (3398) | 400–500 | Steel red 35, brown 30, crimson 20; bronze black 10, purple 6 |
| Riyl (3400) | 600–700 | Black red 35, brown 30, crimson 20; steel black 10, purple 6 |
| Asyn (3402) | 800–900 | Silver red 35, brown 30; black crimson 20, black 10, purple 6 |
| Fiyr (3404) | 2,000–4,000 | Gold red 20; silver brown 20, crimson 45, black 10, purple 6 |
| Urium (25419) | 2,000–7,000 | Gold crimson 45, red 20, brown 20, black 10, purple 6 |

The bronze, steel, black and silver key families are cache objects 3450–3469; gold red, brown,
crimson, black and purple are 25424, 25426, 25428, 25430 and 25432. Reward ranges are inclusive.

The cutoff pages describe the loaded stages as lasting only a brief period but do not pin an exact
tick count. A roughly 30-second historical observation is implemented as 50 ticks pending a
cache-233.4 client capture. The pages likewise do not expose exact feedback, spirit behavior,
sound/packet ordering, the selection rule when multiple stands are equally near, or quest-owned
varp mutation checkpoints. The implementation deterministically selects the nearest valid cardinal
stand (falling back to the same geometry when static map data is unavailable), but that selection
rule and the presentation details remain researched rather than verified.

## Barbarian pyre ships and ancestral Prayer

The cutoff [Funeral pyre ship revision
14952958](https://oldschool.runescape.wiki/w/Funeral_pyre_ship?oldid=14952958), [Chewed bones
revision 14953745](https://oldschool.runescape.wiki/w/Chewed_bones?oldid=14953745), [Mangled bones
revision 14816064](https://oldschool.runescape.wiki/w/Mangled_bones?oldid=14816064) and [Barbarian
Training revision
14988720](https://oldschool.runescape.wiki/w/Barbarian_Training?oldid=14988720) define the
members-only pyre-ship contract. Construction requires the Firemaking training section to have
started, one accepted axe, one ordinary log, chewed or mangled bones, and a tinderbox or one of the
same eligible bows used by Barbarian Firemaking; the Bruma torch is explicitly unsuitable. Cache
varbit 3764 (`brut_craft_ship`) uses states 0 not started, 1 tasked, 2 first sacrifice complete and
3 fully trained. Cache varbit 3767 (`brut_prayer_total`) stores at most 200 ancestral charges.

The exact shared Crafting/Firemaking level, Crafting XP, Firemaking XP and chewed-bone charge rows
are normal/achey 11, 10, 40, 1; oak 25, 15, 60, 2; willow 40, 22.5, 90, 2; teak 45, 26.2, 105, 3;
arctic pine 52, 31.2, 125, 3; maple 55, 33.7, 135, 3; mahogany 60, 39.3, 157.5, 4; yew 70,
50.6, 202.5, 4; magic 85, 75.9, 303.8, 5; and redwood 95, 87, 350, 5. Both skill checks accept
visible boosts. Cache location 25286 is the `Construct` site and 25288–25295 are its eight staged
forms; sequences 6733–6736 animate the fire, launch, ready and sinking models. The six published
site tiles are `(2519,3519)`, `(2518,3519)`, `(2507,3518)`, `(2506,3518)`, `(2503,3499)` and
`(2503,3498)`.

Chewed bones 11338 summon peaceful spirit 5605 with sequence 6724, add the log row's charges up to
200, advance training state 1 to 2 and award one item. Magic logs additionally set bit 10 of cache
varp 1179 for the Kandarin elite task. The reward first rolls Dragon full helm 11335 at 1/256;
otherwise it uses the complete 128-weight table from [Pyre ship reward table revision
14204231](https://oldschool.runescape.wiki/w/Pyre_ship?oldid=14204231): two noted unfinished
ranarr potions; the documented barbarian mixes; 8–15 death or 4–7 blood runes; silver bolts, rune
arrows, runite bolts, two noted diamonds, two mith grapples, poisoned adamant darts or adamant
throwing knives. Integration tests pin all 22 rows, quantities and roll boundaries and verify
private ground fallback when inventory capacity is unavailable.

The cutoff [Peaceful barbarian spirit revision
14897471](https://oldschool.runescape.wiki/w/Peaceful_barbarian_spirit?oldid=14897471) defines the
ancestral blessing: one charge triples the base XP of one eligible direct bone burial and is spent
even when Zealot preserves the bone. It excludes big/giant bones, altar offerings and Hopespear's
Will quest bones. The exact message is `You gain extra prayer experience as the ancestral
barbarian spirits watch over you.` Bonecrusher-triggered burial is also eligible in production but
remains open because this repository has no Bonecrusher processing path.

Mangled bones 11337 instead summon Ferocious barbarian spirit 5566 with appearance sequence 6725,
no reward and no ancestral charges. The cutoff [Ferocious barbarian spirit revision
14992305](https://oldschool.runescape.wiki/w/Ferocious_barbarian_spirit?oldid=14992305) pins its
roughly three-minute duration and combat level 166 profile: 190 Hitpoints, 150 Attack, 140 Strength,
100 Defence, 1 Magic and Ranged, four-tick crush attacks, attack/Strength bonuses 9/15, and defence
bonuses 0/73/72/-3/30/72/72 for stab/slash/crush/magic/standard/light/heavy. Integration tests pin these
values and the implementation summons it aggressively for 300 cycles while preventing its creator
from starting another ship while it remains live.

The same dated page marks the ordinary table rates as supplied by Jagex and resolves one exact
128-weight main selection. Its rows are Mithril mace 5, Adamant mace 4, Mithril hasta 2, Rune mace
1; 5–9 death runes 8; 20 Adamant knives 4, 20 poisoned Adamant darts 4, 10 rune arrows 4 and 10
runite bolts 4; a weight-10 branch that rolls the standard 128-weight grimy-herb table twice;
Ranarr seed 5; Prayer mix(1)/(2) at 16/8; Antifire mix(1)/(2) at 4/4; Super attack mix(2) 3,
Super defence mix(2) 3, Super strength mix(1)/(2) 2/2, Super attack mix(1) 2, Super defence mix(1)
2, Superantipoison mix(2) 1 and Fishing mix(2) 1; three yew logs 2; two noted steel bars 2, two
noted mithril bars 2, one noted adamantite bar 1 and one noted runite bar 1; ten noted big bones 7;
three sharks 6; and the standard gem table at weight 8. All 128 main boundaries and quantities are
covered by integration tests.

The cutoff [Rare drop table revision
14991965](https://oldschool.runescape.wiki/w/Rare_drop_table?oldid=14991965) and [standard gem-table
documentation revision
14991640](https://oldschool.runescape.wiki/w/Template:GemDropTable/doc?oldid=14991640)
resolve that nested branch. Without a Ring of Wealth its 128 weights are nothing 63, uncut
sapphire 32, emerald 16, ruby 8, the map talisman 3, diamond 2, five rune javelins 1, loop half 1,
tooth half 1 and mega-rare entry 1. Any charged, uncharged or imbued Ring of Wealth removes the
nothing row, leaving 65 weights. After Legends' Quest, the mega table is nothing 113, Rune spear
8, shield left half 4 and Dragon spear 3; the ring removes that nothing row and leaves 15 weights.
Before quest completion state 75, the selected mega slot becomes the map talisman instead. This
above-ground encounter uses Nature talisman. The reusable server helper preserves these nested
contracts and the Ferocious-spirit integration suite pins every boundary.

The dated spirit page separately lists an elite clue scroll and elite casket as always-awarded only
while completing the elite clue challenge that requests this kill. That challenge outcome is not
part of the ordinary 128-weight table and is deliberately not generalized without clue state.
Production owner-only attack enforcement, successful-kill Barbarian Training completion, the
challenge pair and exact combat/drop presentation remain open encounter work.

The static cache map does not contain the six interactive sites in the expected overworld square,
so exact site-to-model anchors cannot be proven from cache 233.4 alone. The current north/middle
anchors and southern west-facing anchor are corroborated only by observational 2009Scape code;
peaceful-spirit lifetime and cosmetic/felling-axe animation substitutions are likewise
observational. These transforms, exact messages and audiovisual/packet ordering require capture
with the workspace cache-233 RuneLite before promotion.

## Handheld Firemaking light sources

The cutoff [Light sources revision
14971098](https://oldschool.runescape.wiki/w/Light_sources?oldid=14971098) defines tinderbox
ignition, boostable Firemaking requirements, extinguishing, brightness and gas behavior. The
individual cutoff revisions pin the item pairs and levels: [Torch 14975204](https://oldschool.runescape.wiki/w/Torch?oldid=14975204),
[Candle 14957825](https://oldschool.runescape.wiki/w/Candle?oldid=14957825), [Black candle
14832127](https://oldschool.runescape.wiki/w/Black_candle?oldid=14832127), [Candle lantern
14957407](https://oldschool.runescape.wiki/w/Candle_lantern?oldid=14957407), [Oil lamp
14781654](https://oldschool.runescape.wiki/w/Oil_lamp?oldid=14781654), [Oil lantern
14795975](https://oldschool.runescape.wiki/w/Oil_lantern?oldid=14795975), [Bullseye lantern
14880983](https://oldschool.runescape.wiki/w/Bullseye_lantern?oldid=14880983), [Sapphire lantern
14874361](https://oldschool.runescape.wiki/w/Sapphire_lantern?oldid=14874361), [Emerald lantern
14722549](https://oldschool.runescape.wiki/w/Emerald_lantern?oldid=14722549) and [Mining helmet
14852161](https://oldschool.runescape.wiki/w/Mining_helmet?oldid=14852161). The more specific [Dry
sticks revision 14977083](https://oldschool.runescape.wiki/w/Dry_sticks?oldid=14977083) resolves an
overview ambiguity: item-on-torch use does nothing; choosing the sticks' `Rub-together` operation
at level 30 lights an unlit torch from inventory.

The cache-233 item contract is torch 596→594 at level 1, candle 36→33 at level 1, black candle
38→32 at level 1, white candle lantern 4529→4531 at level 4, black candle lantern 4532→4534 at
level 4, oil lamp 4522→4524 at level 12, oil lantern 4537→4539 at level 26, bullseye lantern
4548→4550 at level 49, sapphire lantern 4701→4702 at level 49, emerald lantern 9064→9065 at
level 49 and mining helmet 5014→5013 at level 65. All are members items and accept visible boosts.
Every pair lights with a tinderbox in either item-use order. Dry sticks 1468 remain in inventory
when their own operation lights the first unlit torch; direct sticks-on-torch use is deliberately
unhandled. Lighting awards no XP. Lit forms dispatch the exact cache inventory operation slot:
torch 4; candle, sapphire 1; both candle lanterns, oil lamp, oil lantern and bullseye 2; emerald and
mining helmet 3. The equipped mining helmet exposes Extinguish after Remove and changes form
without being unequipped.

The same candle-lantern revision defines four assembly routes from empty lantern 4527: unlit white
or black candles require level 1 and create the corresponding unlit lantern; already-lit candles
require level 4 and create the corresponding lit lantern. All four are boostable, members-only and
award zero XP. The sapphire revision uniquely publishes the manual feedback `You extinguish the
lantern.`, which the implementation preserves; the other exact messages and audiovisual order need
a matching-client capture. The current three dry-stick success messages are corroborated by the
observational 2009Scape implementation, not by the cutoff Wiki text, and remain unverified.

Environmental behavior remains a separate researched group. The cutoff source gives brightness
values, an initial darkness warning after roughly 9 seconds, insect damage beginning around 18
seconds, and a seven-tick swamp-gas explosion dealing 25% of total Hitpoints and extinguishing open
flames. It also prohibits manually extinguishing the player's only light in a dark area and lists
heat-source torch ignition plus monster, trap, water and area-specific extinguishing. Those rules
need authoritative zone inventories and shared light-state processing before implementation; they
are not implied by the item-lifecycle tests.

## Lamp-oil still processing

The cutoff [Lamp oil revision
14462026](https://oldschool.runescape.wiki/w/Lamp_oil?oldid=14462026) establishes that lamp oil is
not an inventory object. A player uses one swamp tar per lamp on a still and then uses an empty
container on that loaded still; the oil remains loaded until a container receives it. It lists the
empty oil lamp, empty oil lantern, empty bullseye lantern and sapphire lantern as containers. The
cutoff [Emerald lantern revision
14722549](https://oldschool.runescape.wiki/w/Emerald_lantern?oldid=14722549) independently adds the
empty emerald lantern route.

The cutoff [Lamp oil still revision
14659120](https://oldschool.runescape.wiki/w/Lamp_oil_still?oldid=14659120) pins the Rimmington
Chemist's still to empty/full locations 5909 and 5910. The cutoff [Lamp still revision
14437324](https://oldschool.runescape.wiki/w/Lamp_still?oldid=14437324) pins Miltog's Dorgesh-Kaan
still to location 22773 and explicitly says it cannot make impling jars. Rimmington can separately
process imp repellent followed by a butterfly jar; that non-Firemaking product remains open. The
cutoff [Swamp tar revision
14984967](https://oldschool.runescape.wiki/w/Swamp_tar?oldid=14984967) pins stackable members object
1939 and corroborates both production locations.

Cache 233 supplies the five exact transformations: 4525→4522 empty/oil lamp, 4535→4537 empty/oil
lantern, 4546→4548 empty/bullseye lantern, 4700→4701 empty/sapphire lantern and 20722→9064
empty/emerald lantern. The implementation consumes exactly one tar when the still is loaded,
preserves a loaded still across players until one valid container is filled, performs same-slot
inventory replacement with no XP, changes Rimmington between its two scenery types and tracks the
single-form Dorgesh-Kaan state server-side. The pinned pages do not publish interaction messages,
animation or tick/packet timing, so the present immediate functional behavior is integration-tested
but not verified presentation.

## Ordinary Herblore herb cleaning

The final pre-cutoff [Herblore revision
14995595](https://oldschool.runescape.wiki/w/Herblore?oldid=14995595), dated 28 September 2025,
defines Herblore as members-only, requires completion of Druidic Ritual before training, and pins
the 15 ordinary cleaning rows: guam 3/2.5 XP, marrentill 5/3.8, tarromin 11/5, harralander 20/6.3,
ranarr 25/7.5, toadflax 30/8, irit 40/8.8, avantoe 48/10, kwuarm 54/11.3, huasca 58/11.8,
snapdragon 59/11.8, cadantine 65/12.5, lantadyme 67/13.1, dwarf weed 70/13.8 and torstol 75/15.
The final pre-cutoff [Herb revision
14892384](https://oldschool.runescape.wiki/w/Herb?oldid=14892384) establishes left-click cleaning and
the 10 September 2020 automatic continuation after the first inventory herb. The final pre-cutoff
[Game tick revision
14986190](https://oldschool.runescape.wiki/w/Game_tick?oldid=14986190) pins unattended cleaning to
one herb every two ticks and records an apparent maximum of ten manual cleaning requests per tick.

Cache 233 pins the exact grimy→clean object pairs as 199→249, 201→251, 203→253, 205→255,
207→257, 3049→2998, 209→259, 211→261, 213→263, 30094→30097, 3051→3000, 215→265,
2485→2481, 217→267 and 219→269; every grimy form has `Clean` in inventory operation 1 and all 30
forms are members objects. The cache symbol `druidquest` is varp 80. The revision-matched Quest
Helper maps Druidic Ritual's active steps to values 0–3, so value 4 is the completion boundary used
by this isolated skill plugin until the owning quest implementation centralises quest-state lookup.

The implementation replaces the selected item in place immediately, awards exact fractional XP,
and queues same-type remaining herbs at the sourced two-tick automatic rate. Membership, quest and
boosted-level failures, full inventories and missing queued inputs are atomic. The functional
messages are not published by the pinned Wiki pages; the observed manual ten-request ceiling,
mixed-herb selection, sound, exact feedback and packet ordering remain researched rather than
claimed as verified.

## Ordinary unfinished Herblore potions

The final pre-cutoff [Herblore revision
14995595](https://oldschool.runescape.wiki/w/Herblore?oldid=14995595) defines the standard clean-herb
and vial-of-water process and the 15 water-based levels: guam 3, marrentill 5, tarromin 12, harralander 22,
ranarr 30, toadflax 34, irit 45, avantoe 50, kwuarm 55, huasca 58, snapdragon 63, cadantine 66,
lantadyme 69, dwarf weed 72 and torstol 78. The pre-cutoff [unfinished-potion calculator module
revision 14879158](https://oldschool.runescape.wiki/w/Module:Unfinished_potion_creation_calculator?oldid=14879158)
independently pins those herb/container/output rows. The final pre-cutoff [Game tick revision
14986190](https://oldschool.runescape.wiki/w/Game_tick?oldid=14986190) establishes one tick per herb,
and the pre-cutoff [snapdragon workflow revision
14360431](https://oldschool.runescape.wiki/w/Money_making_guide/Making_snapdragon_potions?oldid=14360431)
pins the item-on-item menu followed by Make-All and confirms that the process awards no XP. The
pre-cutoff [cadantine blood potion (unf) revision
14905990](https://oldschool.runescape.wiki/w/Cadantine_blood_potion_(unf)?oldid=14905990) adds the
sixteenth standard route: clean cadantine plus a vial of blood at boosted level 80, taking one tick
and awarding no XP. The final pre-cutoff [vial of blood revision
14772835](https://oldschool.runescape.wiki/w/Vial_of_blood?oldid=14772835) corroborates its liquid
input and members identity.

Cache 233 maps vial of water to 227 and the outputs to guam 91, marrentill 93, tarromin 95,
harralander 97, ranarr 99, toadflax 3002, irit 101, avantoe 103, kwuarm 105, huasca 30100,
snapdragon 3004, cadantine 107, lantadyme 2483, dwarf weed 109 and torstol 111. It separately maps
vial of blood to 22446 and cadantine blood potion (unf) to 22443. All herbs, outputs and the blood
base are members objects. Cache sequence 363 (`human_herbing_vial`) is corroborated by the
revision-matched RuneLite `HERBLORE_POTIONMAKING` constant for both herb and secondary mixing.

The implementation accepts both held-item orders, derives the Make-All maximum from the smaller
input count, checks membership, Druidic Ritual state 4 and the boosted Herblore level, and produces
one output per tick with zero XP. The transaction consumes one herb and the recipe's liquid base together and is
safe in a full inventory; missing inputs and queue replacement stop without partial consumption.
Exact output-slot choice, message capitalization, animation repetition, sound and packet ordering
remain observational.

## Coconut-milk preparation

The final pre-cutoff [Coconut milk revision
14921101](https://oldschool.runescape.wiki/w/Coconut_milk?oldid=14921101) and [Coconut revision
14748255](https://oldschool.runescape.wiki/w/Coconut?oldid=14748255) establish the complete
preparation chain. A hammer cracks a coconut into a half; using that half on an empty vial produces
coconut milk and leaves a coconut shell. Filling is immediate, with automatic filling after three
ticks and then every four ticks. The recipe has no skill, level, quest or XP field and all coconut
products are members items.

Cache 233.4 resolves hammer 2347, coconut 5974, half 5976, shell 5978, empty vial 229 and coconut
milk 5935. The implementation registers both held-use pairs symmetrically, replaces the selected
coconut in place, and atomically places the shell in the half's slot and milk in the vial's slot.
Automatic filling searches the next available half and vial and participates in shared Herblore
cancellation through queue 102. Full inventories remain safe because both operations are in-place
or two-input/two-output transformations; lost materials stop continuation without mutation.

The two historical pages do not say whether cracking one coconut automatically cracks the rest,
and do not publish messages, animation, sound or packet ordering. The implementation therefore
cracks only the selected coconut and does not invent presentation. Output-slot assignment follows
the natural container/resource replacement model but remains an explicit server choice pending
matching-client capture. The functional core is integration-tested, not client-tested or verified.

## Swamp-tar Herblore production

The final pre-cutoff [Guam tar revision
14628398](https://oldschool.runescape.wiki/w/Guam_tar?oldid=14628398), [Marrentill tar revision
14628399](https://oldschool.runescape.wiki/w/Marrentill_tar?oldid=14628399), [Tarromin tar revision
14628415](https://oldschool.runescape.wiki/w/Tarromin_tar?oldid=14628415) and [Harralander tar revision
14628400](https://oldschool.runescape.wiki/w/Harralander_tar?oldid=14628400) establish the complete
cache-233 production family. Each action requires a pestle and mortar, consumes one clean herb and
15 swamp tar, produces 15 matching salamander tar and takes three game ticks. The respective
boosted Herblore levels and XP are 19/30, 31/42.5, 39/55 and 44/72.5. Guam additionally accepts
ground guam as the herb input. All four products are members content.

Cache 233.4 independently resolves pestle and mortar 233, swamp tar 1939, clean herbs
249/251/253/255, ground guam 6681 and outputs 10142/10143/10144/10145. All outputs are stackable
members objects. Sequence 5249 is explicitly named `human_salamander_tar_grind`; the implementation
uses a dedicated weak queue 101 and the native one-product interface 270/clientscript 2046. The
current Wiki includes Irit tar, but that product arrived with post-cutoff Sailing content and is
deliberately excluded from this cache-233 table.

Production observations show a distinct input-count boundary: one available herb begins the action
directly, while multiple herbs open the quantity chooser. This behavior is implemented and tested
but remains lower-authority than the dated recipe contracts. Both held-use orders preserve the
selected herb identity; the menu maximum is the smaller of herb count and complete 15-tar groups.
Every result atomically consumes one herb and 15 swamp tar, adds 15 output and awards exact
fractional XP after three ticks. Membership, Druidic Ritual state 4, boosted level, pestle and
resources are validated before scheduling and again at each result. Full inventories, tool or
resource loss and replacement by another Herblore action stop without partial mutation.

Six integration scenarios pin every input/output/level/XP contract and handler, exact result timing,
direct-versus-menu selection, three-tick batching, both Guam input identities and item orders,
access gates, boosted levels, full-inventory behavior and interruption. The historical pages do not
publish exact success or failure wording, audio, animation-restart behavior or packet/inventory/XP
ordering. The implemented failure text follows existing Herblore conventions and those presentation
details remain matching-client manual case 63. The functional core is therefore
`integration-tested`, not client-tested or verified.

## Standard finished Herblore potions

The final pre-cutoff [Herblore revision
14995595](https://oldschool.runescape.wiki/w/Herblore?oldid=14995595) pins 31 standard
unfinished-potion-plus-secondary recipes. The 29 water-base products span Attack potion at level
3/25 XP through Menaphite remedy at level 88/200 XP; the two blood-base products are Bastion and
Battlemage potions at level 80/155 XP. That table fixes every primary, secondary and output family,
including Compost, Goading, Prayer regeneration, Surge and Ancient brew. The final pre-cutoff
[Game tick revision
14986190](https://oldschool.runescape.wiki/w/Game_tick?oldid=14986190) independently pins adding a
secondary to an unfinished potion at two ticks per potion.

Cache 233 resolves the 31 output identities to 121, 175, 115, 6472, 127, 3010, 133, 3034, 9741,
139, 145, 181, 151, 3018, 10000, 30140, 157, 30128, 187, 3026, 163, 2454, 169, 3042, 189,
22464, 22452, 6687, 30878, 26342 and 27205. Their exact secondaries are objects 221, 235, 225,
21622, 223, 1975, 239, 2152, 9736, 231, 2970, 10111, 29993, 241, 245, 3138, 247, 6693,
30800, 26368 and 27272 as selected by the recipe. Sequence 363 (`human_herbing_vial`) is the
cache-native potion-mixing sequence and is corroborated by the revision-matched workspace RuneLite
constant.

The implementation accepts either held-item order, derives the quantity ceiling from both inputs,
uses the cache-native one-product quantity interface and performs one atomic transform every two
ticks. Membership, Druidic Ritual state 4, boosted level, full-inventory replacement, input loss
and shared Herblore cancellation are enforced; each transform awards the exact fractional XP.
Exact one-input immediate behavior, message capitalization, output-slot choice, animation
repetition, sound and packet ordering remain observational.

The dated [amulet of chemistry revision
14857005](https://oldschool.runescape.wiki/w/Amulet_of_chemistry?oldid=14857005) specifies five
account-shared charges and a 5% extra-dose roll; [alchemist's amulet revision
14950137](https://oldschool.runescape.wiki/w/Alchemist%27s_amulet?oldid=14950137) specifies a 15% roll,
50 initial uses, ten charges per chemistry amulet and a 5,000 cap; and [prescription goggles
revision
14988816](https://oldschool.runescape.wiki/w/Prescription_goggles?oldid=14988816) specifies a 10%
secondary-preservation roll. Cache 233 pins the equipment identities as chemistry amulet 21163,
unfocused/focused goggles 29974/29976 and uncharged/charged alchemist's amulets 29988/29990; cache
varbit 16173 carries the alchemist's-amulet charge count. The implementation maps all 30 eligible
three-dose outputs to their cache-native four-dose identities, performs the 5% or 15% extra-dose
roll before the independent 10% goggles roll, consumes amulet charges only after the inventory
transaction succeeds, persists chemistry progress in server varp 65535, removes the fifth-use
chemistry amulet, changes a zero-charge alchemist's amulet to object 29988 and never awards extra
XP. Both goggle appearances preserve one ordinary secondary with no message; weapon poison has no
four-dose mapping and therefore does not roll or consume an amulet charge. The revision-matched
workspace RuneLite checkout at commit `d14c12197c80f76a2833c7f744cacec896096d76` supplies production
message recognizers and fixtures introduced in 2020, well before the cutoff: each chemistry success
reports its resulting dose and remaining count, spells the singular as `one charge`, and reports
that the amulet crumbles on its final success. Integration coverage forces success and failure
outcomes, combined amulet/goggle success, charge persistence, exact four-dose messages and both
depletion transforms.

The exact production-roll order is an implementation choice pending matching-client packet/RNG
evidence. Chemistry-amulet Options, direct-Break presentation and exact production audiovisual and
packet sequencing remain researched lifecycle work.

## Strong weapon-poison Herblore production

The final pre-cutoff [Weapon poison(+) revision
14991927](https://oldschool.runescape.wiki/w/Weapon_poison(%2B)?oldid=14991927), [Weapon poison(++)
revision 14991890](https://oldschool.runescape.wiki/w/Weapon_poison(%2B%2B)?oldid=14991890),
[Weapon poison(+) (unf) revision
14929742](https://oldschool.runescape.wiki/w/Weapon_poison(%2B)_(unf)?oldid=14929742) and [Weapon
poison(++) (unf) revision
14929744](https://oldschool.runescape.wiki/w/Weapon_poison(%2B%2B)_(unf)?oldid=14929744) establish
both complete cache-233 recipes. At boosted Herblore level 73, coconut milk plus a cactus spine
produces Weapon poison(+) (unf) in two ticks for zero XP; red spiders' eggs then produces Weapon
poison(+) in another two ticks for 190 XP. At boosted level 82, coconut milk plus Cave nightshade
or stackable Nightshade produces Weapon poison(++) (unf) in two ticks for zero XP; poison ivy
berries then produces Weapon poison(++) in two ticks for 190 XP. The final pre-cutoff plus revision
records an earlier 165-XP value as a historical change; its own active recipe field is 190 XP, so
the cutoff implementation follows 190 under the Wiki-exact policy.

The final pre-cutoff [Cave nightshade revision
14955112](https://oldschool.runescape.wiki/w/Cave_nightshade?oldid=14955112) and [Nightshade revision
14956900](https://oldschool.runescape.wiki/w/Nightshade?oldid=14956900) distinguish non-stackable
object 2398 from stackable object 27790 while allowing both in the same recipe. The final
pre-cutoff [Prescription goggles revision
14988816](https://oldschool.runescape.wiki/w/Prescription_goggles?oldid=14988816) explicitly says
that both the nightshade and poison ivy berry can be preserved while making Weapon poison(++).
Its ordinary final-secondary rule also applies to the red spiders' eggs route. The [amulet of
chemistry revision
14857005](https://oldschool.runescape.wiki/w/Amulet_of_chemistry?oldid=14857005) only adds doses to
eligible dose-bearing outputs; the cache has no strong-weapon-poison dose variant, so neither it nor
the alchemist's amulet rolls or consumes a charge here.

Cache 233 independently resolves coconut milk 5935, cactus spine 6016, the two nightshades
2398/27790, unfinished poisons 5936/5939, red spiders' eggs 223, poison ivy berries 6018 and final
poisons 5937/5940. The implementation extends the existing native interface-270/clientscript-2046
quantity flow and sequence 363, validates members, Druidic Ritual state 4 and the boosted level,
and revalidates every exact base/ingredient pair at each two-tick result. Identifying queued work
by both inputs is necessary because the two nightshade identities share output 5939. Every stage is
atomic, awards only its sourced XP and supports both held-use orders, batching, full inventories,
material loss and shared Herblore cancellation.

Exact success/failure wording, animation restart, sound, output-slot choice, item/XP packet order
and the one-input menu presentation are not pinned by the inspected revisions and remain manual
case 64. This functional core is therefore integration-tested, not client-tested or verified.

## Antidote Herblore production

The final pre-cutoff [Antidote+ revision
14993895](https://oldschool.runescape.wiki/w/Antidote%2B?oldid=14993895), [Antidote++ revision
14993822](https://oldschool.runescape.wiki/w/Antidote%2B%2B?oldid=14993822), [Antidote+ (unf)
revision 14929745](https://oldschool.runescape.wiki/w/Antidote%2B_(unf)?oldid=14929745) and
[Antidote++ (unf) revision
14929746](https://oldschool.runescape.wiki/w/Antidote%2B%2B_(unf)?oldid=14929746) establish both
complete cache-233 recipes and their deliberately different first-stage timing. At boosted
Herblore level 68, coconut milk plus toadflax produces Antidote+ (unf) in two ticks for zero XP;
yew roots then produce Antidote+(4) in two ticks for 155 XP. At boosted level 79, coconut milk plus
irit produces Antidote++ (unf) in one tick for zero XP; magic roots then produce Antidote++(4) in
two ticks for 177.5 XP. The full recipes consequently take four and three ticks respectively.

The final pre-cutoff [Prescription goggles revision
14988816](https://oldschool.runescape.wiki/w/Prescription_goggles?oldid=14988816) explicitly extends
its 10% secondary-preservation effect to toadflax and irit in these unfinished stages. Its ordinary
secondary rule covers the yew- and magic-root final stages. Both recipes directly create four-dose
objects, and cache 233 has no five-dose Antidote identity; the implementation therefore performs no
chemistry- or alchemist's-amulet extra-dose roll and consumes no amulet charge.

Cache 233.4 independently resolves coconut milk 5935, toadflax 2998, irit 259, unfinished objects
5942/5951, yew roots 6049, magic roots 6051 and four-dose outputs 5943/5952. The implementation
extends the same interface-270/clientscript-2046 and sequence-363 recipe structures used by the
other potion families. It accepts both held-use orders, derives quantity from the exact input pair,
checks membership, Druidic Ritual state 4 and boosted levels, and revalidates each atomic result.
Full inventories, material loss and shared Herblore cancellation are safe. Automated coverage pins
both complete contracts, their one-/two-tick distinctions, XP, four-dose outputs, both goggle forms
through shared modifier coverage and both inapplicable amulets.

The dated pages do not publish exact success/failure wording, output-slot selection, animation
restart, sound or interface/inventory/XP packet ordering. Those boundaries remain matching-client
manual case 65, so the functional core is `integration-tested`, not client-tested or verified.

## Anti-venom Herblore production

The final pre-cutoff [Anti-venom revision
14993833](https://oldschool.runescape.wiki/w/Anti-venom?oldid=14993833), dated 25 September 2025,
defines all four dose-aware routes at boosted Herblore level 87. Antidote++(1)/(2)/(3)/(4)
5958/5956/5954/5952 consumes exactly 5/10/15/20 Zulrah's scales 12934 and produces matching
Anti-venom doses 12911/12909/12907/12905 after two ticks for exactly 30/60/90/120 XP. The final
pre-cutoff [Zulrah's scales revision
14868451](https://oldschool.runescape.wiki/w/Zulrah%27s_scales?oldid=14868451) corroborates the
stackable secondary. The Anti-venom page also records a Karamja elite task when mixing in Horseshoe
Mine and a Sherlock master-clue step; those world-owned progression hooks remain the separate
researched presentation/integration entry rather than being inferred into this production core.

The final pre-cutoff [Anti-venom+ revision
14993872](https://oldschool.runescape.wiki/w/Anti-venom%2B?oldid=14993872) pins boosted level 94,
two ticks and 125 XP for Anti-venom(4) 12905 plus either clean torstol 269 or torstol potion (unf)
111, producing Anti-venom+(4) 12913. Its dated history makes the unfinished-torstol alternative a
pre-cutoff route, and the final pre-cutoff [Torstol revision
14990291](https://oldschool.runescape.wiki/w/Torstol?oldid=14990291) independently corroborates the
clean herb. Only the four-dose Anti-venom input is accepted.

The final pre-cutoff [Extended anti-venom+ revision
14993931](https://oldschool.runescape.wiki/w/Extended_anti-venom%2B?oldid=14993931) defines one
Araxyte venom sack per potion dose, boosted level 94 and three ticks per action. Anti-venom+(1),
(2), (3) and (4) 12919/12917/12915/12913 therefore consume 1/2/3/4 sacks 29784 and produce
Extended anti-venom+(1)/(2)/(3)/(4) 29833/29830/29827/29824 for 20/40/60/80 XP. At the cutoff the
source page's canonical title was [Araxyte venom sac revision
14982890](https://oldschool.runescape.wiki/w/Araxyte_venom_sac?oldid=14982890), while its item text
and cache symbol use “sack”; the ledger retains that spelling distinction instead of normalizing
away the evidence.

The final pre-cutoff [Alchemist's amulet revision
14950137](https://oldschool.runescape.wiki/w/Alchemist%27s_amulet?oldid=14950137) and [amulet of
chemistry revision
14857005](https://oldschool.runescape.wiki/w/Amulet_of_chemistry?oldid=14857005) explicitly include
variable-dose Anti-venom and Extended anti-venom+ recipes. They may add one dose only when the input
has fewer than four doses, consume one charge only on success and award neither extra XP nor an
extra-secondary cost. Their wording excludes the always-four-dose Anti-venom+ stage. The sibling
revision-matched RuneLite checkout independently contains two-, three- and four-dose chemistry
messages and a dynamic dose-matching item-charge expression; it was inspected read-only and was
neither modified nor launched.

The cutoff [Prescription goggles revision
14988816](https://oldschool.runescape.wiki/w/Prescription_goggles?oldid=14988816) defines one 10%
roll per potion, preserving one stackable secondary for each dose up to four equivalents and no
extra ingredient for an amulet-added dose. It explicitly covers torstol and torstol potion (unf)
for Anti-venom+. A later [observational clarification revision
15240969](https://oldschool.runescape.wiki/w/Prescription_goggles?oldid=15240969) says an
Anti-venom(4) preservation roll saves all 20 scales and an Anti-venom(3) roll all 15. This later
page is used only to clarify the cutoff rule's group quantity, not to introduce post-cutoff
production. The same cutoff per-dose rule means one successful extended-potion roll preserves the
whole 1/2/3/4-sack input group; goggles remain silent and do not alter output or XP.

Cache 233.4 independently pins every input/output identity, stackable scales and sacks, native
interface 270 with clientscript 2046, vial-mixing sequence 363 and dedicated server queue 103. The
implementation uses a reusable typed dose-scaled recipe table and protected inventory transaction,
derives menu quantity from both potion count and complete secondary groups, revalidates members,
Druidic Ritual state 4, boosted level and resources at every result, and shares cancellation with
all Herblore processes. Six integration scenarios exhaust all ten recipe contracts, exact first and
repeat timing, XP, both orders, menu limits, access/resource/full-inventory boundaries and all
amulet/goggle composition cases. Exact menu behavior with only one available action, ordinary
success/failure text, sound, animation restart, output-slot and packet order plus the two world
progression hooks remain manual case 66. The functional core is therefore `integration-tested`,
not client-tested or verified.

The final pre-cutoff [Alchemist's amulet transcript revision
14827465](https://oldschool.runescape.wiki/w/Transcript:Alchemist%27s_amulet?oldid=14827465), dated 25
December 2024, pins the inventory lifecycle wording. Charge asks how many amulets to apply, consumes
one amulet of chemistry per ten charges and distinguishes the first charge from additional charges;
Check reports the stored total. Uncharge asks for confirmation, replaces the charged object with
the uncharged object and returns noted amulets of chemistry equal to the displayed integer
`charges / 10`. The transcript also pins the no-amulets and fully-charged failure messages. Cache
233 independently pins object 21164 as the noted chemistry amulet and inventory operations as
Options/Break in slots 3/4 on chemistry amulet 21163, Charge in slot 4 on object 29988,
Check/Charge/Uncharge in slots 3/4/5 on object 29990, and Change in slot 3 on both goggles objects
29974/29976. `Drop` is a client-default action and is therefore absent from object 21163's cached
operation array rather than a conflicting cache/Wiki fact.

The lifecycle implementation accepts direct Charge and either held-item order, caps the count
dialog by the available chemistry amulets and whole ten-charge capacity, revalidates the selected
slot and charge count after suspension, and commits consumption, same-slot transform and cache
varbit 16173 only as one successful transaction. Uncharge likewise revalidates after confirmation,
atomically transforms in place, returns the noted refund and clears the charge varbit; any remainder
below ten is discarded. Both goggles Change operations transform in place. Chemistry Break deletes
the exact selected inventory item and resets the server-side shared counter so the next amulet has
all five charges; state resets only after the item transaction succeeds. Eight cache-aware
integration scenarios pin the object identities and operation indices, both held-use orders, the
4,990-to-5,000 boundary, transcript wording, stale-safe state checks, refund behavior, full-inventory
rollback, both goggles directions and Break/reset behavior.

No cutoff source inspected resolves charging when the stored total is 4,991–4,999, where less than
one whole chemistry amulet fits; the current implementation treats that state as fully charged.
Likewise, the direct-Break confirmation/message/sound, Options choices and persistence,
full-inventory uncharge failure text and goggles Change sound/presentation are implementation
choices pending matching-client capture. Bank-side operations and the initial 50-charge amulet
awarded through Mastering Mixology are not part of this lifecycle slice.

## Fixed one-to-one Herblore ingredient crushing

The final pre-cutoff [pestle and mortar revision
14795541](https://oldschool.runescape.wiki/w/Pestle_and_mortar?oldid=14795541) establishes automatic
processing of all matching items and a three-game-tick repeat. The final pre-cutoff output pages pin
the eight ordinary fixed one-input/one-output recipes: [unicorn horn dust revision
14918331](https://oldschool.runescape.wiki/w/Unicorn_horn_dust?oldid=14918331), [chocolate dust
revision 14682255](https://oldschool.runescape.wiki/w/Chocolate_dust?oldid=14682255), [goat horn dust
revision 14466551](https://oldschool.runescape.wiki/w/Goat_horn_dust?oldid=14466551), [kebbit teeth
dust revision 14444094](https://oldschool.runescape.wiki/w/Kebbit_teeth_dust?oldid=14444094), [dragon
scale dust revision 14822018](https://oldschool.runescape.wiki/w/Dragon_scale_dust?oldid=14822018),
[crushed nest revision 14990107](https://oldschool.runescape.wiki/w/Crushed_nest?oldid=14990107),
[crushed superior dragon bones revision
14888364](https://oldschool.runescape.wiki/w/Crushed_superior_dragon_bones?oldid=14888364) and [nihil
dust revision 14918328](https://oldschool.runescape.wiki/w/Nihil_dust?oldid=14918328). Each recipe is
members-only through the pestle route, requires no Herblore level and awards no XP. The individual
pages pin an immediate first result, another after two ticks and subsequent results every three
ticks.

Cache 233 maps pestle and mortar to 233. The exact input→output pairs are 237→235, 1973→1975,
9735→9736, 10109→10111, 243→241, 5075→6693, 22124→21975 and 26231→26368. These are
unicorn horn, chocolate bar, desert goat horn, kebbit teeth, blue dragon scale, empty bird nest,
superior dragon bones and nihil shard respectively. The cache sequences are 364
(`human_herbing_grind`) for the initial action and 11095 (`human_herbing_grind_restart`) for
automatic continuation. The revision-matched workspace RuneLite constant
`HERBLORE_PESTLE_AND_MORTAR` corroborates sequence 364, while its idle notifier classifies both
cache sequences as the same grinding activity.

The implementation accepts both item-use orders, immediately replaces the selected input in its
slot and then follows the sourced two-tick first continuation and three-tick repeat cadence. It
requires membership and the pestle but deliberately has no Druidic Ritual or level gate and awards
no XP. Each replacement is atomic in a full inventory; loss of the tool/input and replacement by
another Herblore process stop stale work safely. Six cache-aware integration scenarios cover every
identity and transformation, real held-use dispatch, cadence, gates, full inventories, resource
loss and shared cancellation. Sequence 11095 is selected from its cache name and RuneLite activity
classification; exact restart packet timing, success text, sound and client presentation remain
observational. Variable-yield lava scales, all-at-once crystal shards, Gorak/quest recipes, the
free-to-play knife route for chocolate and Wesley's crushing service are separate families.

## Additional fixed 0/2/3 pestle processing

Eleven final pre-cutoff item revisions define thirteen further one-input/one-output pestle routes,
all with an immediate first result, a second result after two ticks and subsequent results every
three ticks: [ground guam 14786974](https://oldschool.runescape.wiki/w/Ground_guam?oldid=14786974),
[ground charcoal 14672483](https://oldschool.runescape.wiki/w/Ground_charcoal?oldid=14672483), all
three forms on [karambwan paste
15002689](https://oldschool.runescape.wiki/w/Karambwan_paste?oldid=15002689), [ground mud runes
14722507](https://oldschool.runescape.wiki/w/Ground_mud_runes?oldid=14722507), [garlic powder
14444125](https://oldschool.runescape.wiki/w/Garlic_powder?oldid=14444125), [ground bat bones
14672359](https://oldschool.runescape.wiki/w/Ground_bat_bones?oldid=14672359), [ground ashes
14421300](https://oldschool.runescape.wiki/w/Ground_ashes?oldid=14421300), [rune dust
14444135](https://oldschool.runescape.wiki/w/Rune_dust?oldid=14444135), [anchovy paste
14444130](https://oldschool.runescape.wiki/w/Anchovy_paste?oldid=14444130), [ground thistle
14421294](https://oldschool.runescape.wiki/w/Ground_thistle?oldid=14421294) and [ground seaweed
14786976](https://oldschool.runescape.wiki/w/Ground_seaweed?oldid=14786976). Every recipe is
members-only through the pestle, requires no Herblore level and awards no Herblore XP. Garlic may
explicitly be processed at any quest state; bat bones before, during or after Watchtower; and rune
shards before Garden of Tranquillity. Other quest-object acquisition remains with its owning
content rather than being duplicated by the processing plugin.

Cache 233 resolves the exact pairs as 249→6681, 973→704, 3142→3152, 3146→3153, 3144→3154,
4698→9594, 1550→4668, 530→2391, 592→8865, 6466→6467, 319→11266, 3263→3264 and
401→6683. These represent guam, charcoal, raw/poison/cooked karambwan, mud rune, garlic, bat bones,
ashes, rune shards, anchovies, dried thistle and seaweed respectively. The implementation adds them
to the same typed recipe/interaction table as ordinary fixed crushing, preserving both item orders,
sequences 364/11095, exact cadence and shared cancellation.

The inventory path now distinguishes stackable and non-stackable outputs. It merges into an existing
output stack when legal, reuses a fully consumed input slot, otherwise requires a free slot, and
rolls back without animation or partial mutation when a remaining input stack occupies the selected
slot. This also closes the pre-existing stacked-nihil-shard edge case. Three additional cache-aware
scenarios pin all thirteen contracts and dispatch keys, level/Druidic independence, zero XP,
stack-to-stack merging, stack-to-non-stack capacity failure and atomic recovery. Pirate Pete's
ground crab meat, kelp and cod are deliberately excluded until the exact partial-subquest gate is
promoted from matching-client/quest evidence. Exact feedback, sound and packet ordering remain
observational.

## Free-to-play knife chocolate processing

The final pre-cutoff [chocolate dust revision
14682255](https://oldschool.runescape.wiki/w/Chocolate_dust?oldid=14682255) gives the knife route its
own recipe tab: knife plus one chocolate bar produces one chocolate dust, is free-to-play and has
no level, quest or XP requirement. It pins the same automatic timing as the pestle route: an
immediate first result, another after two ticks and later results every three ticks. Cache 233 maps
the ordinary knife to object 946, chocolate bar to 1973 and chocolate dust to 1975; all three are
free-to-play identities.

The implementation registers both held-item orders through the same interaction DSL as the pestle
recipes, atomically replaces each selected bar in its slot and carries the exact starting tool in
the queued request. A knife-started batch therefore cannot silently continue through a held pestle
after the knife disappears. It uses the cache's shared initial/restart grinding sequences 364 and
11095, shares cancellation with every Herblore production queue and deliberately applies no
members, Druidic Ritual, level or XP gate. Two additional cache-aware scenarios cover real dispatch
in both orders on a free world, a completely full inventory, exact cadence and loss of the starting
knife. The exact knife animation choice, messages, sound and packet/inventory order remain
matching-client observations.

## Crystal-shard batch crushing

The final pre-cutoff [crystal dust revision
14824697](https://oldschool.runescape.wiki/w/Crystal_dust?oldid=14824697) pins this special recipe:
one crystal shard produces ten crystal dust, requires Song of the Elves completion, has no skill or
XP requirement, and processes the entire quantity entered at once. The final pre-cutoff [crystal
shard revision
14988101](https://oldschool.runescape.wiki/w/Crystal_shard?oldid=14988101) corroborates that each
shard supplies ten dust and identifies shards as members-only, untradeable and stackable.

Cache 233 maps the shard to object 23962 (`prif_crystal_shard`), dust to object 23964
(`prif_crystal_shard_crushed`) and the quest-progress field to varbit 9016 (`sote`). The final
pre-cutoff Quest Helper source at commit `4c364f0c2eec7bcaf106865b8031b816e5a13455` maps active
Song of the Elves states through 192, with 188/190/192 all directing the final Baxtorian
conversation. The implementation therefore uses the next two-step state, 194, as completion until
the owning quest implementation centralises status lookup. Cache sequence 364 and RuneLite's
`HERBLORE_PESTLE_AND_MORTAR` constant provide the grinding animation identity.

The implementation accepts both item-use orders, bounds the count entry by the selected stack and
safe 1:10 multiplication, revalidates membership, quest state, tool and the exact selected stack
after the suspended prompt, then atomically converts the requested quantity with zero XP. A
full-stack conversion preserves the selected slot; a partial conversion may use an existing dust
stack or free slot and otherwise rolls back. Five cache-aware integration scenarios cover cache
identity/dispatch, all-at-once conversion, both orders, count capping, both gates, full-inventory
behavior and stale-dialogue changes. The count-prompt wording, quest-failure wording, exact output
placement for partial stacks, sound and packet presentation remain matching-client observations.

## Lava-scale crushing

The final pre-cutoff [lava scale shard revision
14760248](https://oldschool.runescape.wiki/w/Lava_scale_shard?oldid=14760248) pins members-only
pestle processing with no level or XP requirement. One lava scale produces three to six shards;
higher Herblore raises the average, and the completed Hard Wilderness Diary increases each result
by 50%, rounded down. The page also pins an immediate first result, a second after two game ticks
and automatic continuation every three ticks.

The final pre-cutoff [Lava shard chart module revision
14804560](https://oldschool.runescape.wiki/w/Module:Lava_shard_chart?oldid=14804560) exposes the exact
three independent low/high rolls as 101/301, 58/251 and -14/254, adding one shard for each success
to a base yield of three. The cutoff [Skilling success chart module revision
14931997](https://oldschool.runescape.wiki/w/Module:Skilling_success_chart?oldid=14931997) defines the
1–99 linear interpolation and rounding used by those values. This is exactly rsmod's existing
`statRandom` contract, including use of the player's visible Herblore level. Applying
`floor(yield * 1.5)` maps ordinary yields 3/4/5/6 to diary yields 4/6/7/9.

Cache 233 maps lava scale to object 11992 (`lava_scale`), lava scale shard to object 11994
(`lava_shard`) and Hard Wilderness Diary completion to varbit 4468. Sequence 364 is the initial
grinding animation and sequence 11095 is the automatic restart animation. The implementation
accepts both held-item orders, makes all three rolls for each scale, atomically stacks output or
reuses the consumed scale slot, and shares the Herblore cancellation family. It deliberately has
no Druidic Ritual or minimum-level gate and awards no XP. Six cache-aware integration scenarios
cover identity/dispatch, every 3–6 outcome, all four diary results, exact cadence, both item orders,
members/no-level behavior, full inventories and missing tools. Exact feedback, sound, animation
restart packet and client-observable RNG/inventory order remain matching-client observations.

## Gorak-claw crushing

The final pre-cutoff [Gorak claw powder revision
14435792](https://oldschool.runescape.wiki/w/Gorak_claw_powder?oldid=14435792) defines the special
members recipe: pestle and mortar plus one Gorak claw produces one Gorak claw powder with no
Herblore level or XP. Its recipe records an immediate first action and uniform three-tick automatic
crushing, distinct from the ordinary recipes' two-tick first continuation. It states the powder is
made during Fairytale II and can be made the same way after the quest. The final pre-cutoff [Gorak
claws revision
14895536](https://oldschool.runescape.wiki/w/Gorak_claws?oldid=14895536) corroborates the
one-claw/one-powder transformation and quest-item ownership.

Cache 233 maps the claws to object 9016 (`fairytale2_gorak_claws`) and powder to object 9018
(`fairytale2_ground_gorak_claws`). The implementation treats legitimate possession as the recipe
gate and leaves claw acquisition to the quest/drop owners; it does not invent a Fairytale or
Druidic Ritual progress check not stated by the recipe. It registers both held-item orders,
atomically replaces the exact selected slot, uses initial/restart grind sequences 364/11095 and
shares the Herblore cancellation family. Four cache-aware scenarios cover identity/dispatch,
immediate then three-tick cadence, both orders, members and no-level/no-Druidic behavior, zero XP,
full inventories and missing-pestle cancellation. Exact feedback, sound, animation restart and
matching-client packet/inventory order remain observational.

## Suqah-tooth crushing

The final pre-cutoff [ground tooth revision
14932139](https://oldschool.runescape.wiki/w/Ground_tooth?oldid=14932139) defines pestle and mortar
plus one Suqah tooth producing one ground tooth, and uniquely pins automatic output timing as one
tick for the first tooth, two more ticks for the second and three more ticks for subsequent teeth.
It marks both objects as members-only Lunar Diplomacy items and specifies no Herblore level or XP.
The final pre-cutoff [Suqah tooth revision
14424497](https://oldschool.runescape.wiki/w/Suqah_tooth?oldid=14424497) corroborates that one tooth
is ground for the Waking Sleep Potion and later remains useful as spirit-tree protection payment.
The pre-cutoff Quest Helper implementation at commit
`633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a` places grinding in quest state 140 when the tooth is
held, but neither recipe source defines a processing-state restriction. The implementation
therefore leaves acquisition with Lunar Diplomacy and treats legitimate possession as sufficient,
including after the quest, instead of inventing a hidden progress gate.

Cache 233 maps the input to object 9079 (`suqka_tooth`), the output to 9082
(`lunar_groundtooth`) and the pestle to 233. The dedicated queue delays the first mutation by one
tick, then produces at ticks 3 and 6, atomically replacing each non-stackable input in its own slot.
It uses grind/restart sequences 364/11095, supports both held-item orders, shares Herblore queue
cancellation, stops if the pestle disappears, applies membership through the pestle and awards zero
XP without a level or Druidic Ritual check. Four cache-aware scenarios cover identity/dispatch,
exact 1/2/3 cadence, both orders, possession at quest state zero, level one, zero XP, free-world
rejection and pre-result tool loss. Exact text, sound, restart presentation and packet/inventory
ordering remain matching-client observations.

## One-shot quest ingredient crushing

Three final pre-cutoff output pages define immediate single-input pestle recipes without an
automatic-processing note: [cinnamon revision
14419819](https://oldschool.runescape.wiki/w/Cinnamon?oldid=14419819), [Tchiki nut paste revision
14424608](https://oldschool.runescape.wiki/w/Tchiki_nut_paste?oldid=14424608) and [mysterious crushed
meat revision
14422614](https://oldschool.runescape.wiki/w/Mysterious_crushed_meat?oldid=14422614). They establish
Dramen branch→cinnamon for Freeing Sir Amik Varze, Tchiki monkey nuts→paste for Freeing King
Awowogei and mysterious meat→crushed meat for A Taste of Hope. Every recipe is members-only, has
zero ticks for the selected action and specifies no Herblore level or XP. Unlike the ordinary,
Gorak and Suqah pages, none defines an automatic continuation cadence.

Cache 233 maps these pairs to 771→7472 (`dramen_branch`→`chickenquest_cinamon`), 7573→7575
(`hundred_ilm_tchiki_monkey_nuts`→`hundred_ilm_tchiki_monkey_nut_paste`) and 22403→22404
(`myq4_meat`→`myq4_meat_crushed`). The implementation registers both held-item orders from one
typed table, applies membership through pestle 233, uses the initial grinding sequence 364 and
atomically replaces only the selected item in its slot. It has no automatic queue, awards no XP,
adds no unrelated Herblore/Druidic check and leaves legitimate input acquisition with each quest.
Four cache-aware scenarios cover every identity and dispatch key, both orders, immediate timing,
single-action behavior with injected duplicates, level-one/quest-state-zero possession, full
inventories, zero XP and free-world rejection. Exact feedback, audiovisual and packet ordering,
hidden quest-state checks and injected-duplicate production behavior remain matching-client
observations rather than being presented as verified facts.

## Karambwanji-paste processing

The final pre-cutoff [karambwanji paste revision
14970675](https://oldschool.runescape.wiki/w/Karambwanji_paste?oldid=14970675) defines raw and cooked
Tai Bwo Wannai Trio variants. Raw karambwanji with pestle and mortar produces raw paste immediately
when manually selected and automatically repeats every three ticks; the recipe is members-only,
lists Cooking level 1 and awards zero XP. Cooked karambwanji produces the separate cooked paste in
the same members-only recipe family, but its tab specifies neither a delay nor automatic cadence.
Both inputs and both outputs are non-tradeable quest items, so acquisition remains owned by Tai Bwo
Wannai Trio rather than by this processing plugin.

Cache 233 maps raw karambwanji 3150 (`tbwt_raw_karambwanji`) to paste 3155
(`tbwt_raw_karambwanji_paste`) and cooked karambwanji 3151 (`tbwt_cooked_karambwanji`) to paste 3156
(`tbwt_cooked_karambwanji_paste`). Raw input is stackable while its output is not; cooked input and
output are non-stackable. The raw route therefore uses a dedicated immediate/three-tick queue that
requires a destination slot until the final unit can reuse the selected stack slot. The cooked
route uses the shared immediate one-shot table. Both support either held-item order, require pestle
233 and membership, use sequences 364/11095 where applicable, award zero Cooking and Herblore XP,
add no Druidic or invented quest-progress gate and share Herblore cancellation. Four dedicated
cache-aware scenarios plus the extended one-shot table cover identities/dispatch, exact raw cadence,
both orders, level-one/quest-state-zero possession, full-inventory stack capacity, tool loss,
free-world rejection and zero XP. Cooked automatic behavior, exact text, audio, restart presentation
and packet/inventory ordering remain matching-client observations.

## Black-dye pestle processing

The final pre-cutoff [black dye revision
14842486](https://oldschool.runescape.wiki/w/Black_dye?oldid=14842486) defines an immediate
members-only recipe that uses pestle and mortar on a black mushroom while an empty vial is held,
consuming both materials to produce black dye. Its recipe note explicitly requires starting The
Golem. Cache 233 exposes The Golem as varp 437 (`golem_quest`); the pre-cutoff Quest Helper at commit
`633ab56e2eb3eb363f21da3fd75f6f2bc0fa073a` maps state 0 to the quest-start conversation and states
1 onward to in-progress or later steps, establishing state 1 as the first valid boundary.

Cache 233 maps the empty vial to object 229 (`vial_empty`), pestle to 233, black mushroom to 4620
(`golem_mushroom`) and black dye to 4622 (`golem_ink`). The implementation registers both
pestle/mushroom orders, requires membership and `golem_quest >= 1`, consumes the exact selected
mushroom and first empty vial in one inventory transaction, and places dye into the consumed vial
slot. It uses grinding sequence 364, applies no Herblore level, Druidic Ritual or XP rule, and
cancels stale Herblore queues before the immediate action. Four cache-aware scenarios cover cache
identity/dispatch, the state 0/1 boundary, missing vial, both orders, full-inventory atomicity,
members rejection and zero XP. Exact failure text, mushroom/vial selection precedence, output slot,
sound and packet/inventory ordering remain matching-client observations.

## Blamish-snail-slime processing

The final pre-cutoff [blamish snail slime revision
14462907](https://oldschool.runescape.wiki/w/Blamish_snail_slime?oldid=14462907) defines the complete
members recipe: pestle and mortar plus a thin, lean or fat raw snail and one sample bottle produce
one blamish snail slime. The selected manual action is zero ticks; automatic creation is explicitly
one tick for the next result, then two ticks, then three ticks for steady repeats. It specifies no
Herblore level, XP, Druidic Ritual or Heroes' Quest progress check. Heroes' Quest owns legitimate
item use and acquisition, so the processing plugin uses possession rather than an invented state.

Cache 233 maps the three raw snails to 3363/3365/3367 (`snail_corpse1` through
`snail_corpse3`), the sample bottle to 3377 (`empty_dye_bottle`), slime to 1581 and the pestle to
233. The implementation registers both held-item orders for each snail, immediately consumes the
selected snail and first bottle in one transaction, and places slime in the bottle slot. A typed
request preserves the selected snail variant across automatic continuation, which produces at
ticks 1/3/6 after the immediate result. It revalidates membership, pestle, matching snail and bottle
before every result, uses grinding sequences 364/11095, awards zero XP and shares Herblore queue
cancellation. Four cache-aware scenarios cover all identities/dispatch keys, exact cadence, both
orders, all three variants, quest-state-zero possession, level one, full inventory, membership,
material/tool loss and zero XP. Exact messages, cross-size continuation, selection precedence,
audio and packet/inventory ordering remain matching-client observations.

## Blamish-oil Herblore

The final pre-cutoff [blamish oil revision
14719670](https://oldschool.runescape.wiki/w/Blamish_oil?oldid=14719670) defines harralander potion
(unf) plus blamish snail slime producing one blamish oil at boosted Herblore level 25 for exactly
80 XP. It is members-only, takes two ticks, and explicitly says Heroes' Quest completion is not
required. The general cache-233 Herblore contract still requires Druidic Ritual before training.

Cache 233 maps the unfinished potion, slime and oil to 97/1581/1582 and Heroes' Quest to varp 188.
The recipe is registered through the existing finished-potion plugin, so both held-item orders open
the native one-product quantity interface and each requested result is revalidated and committed
atomically every two ticks. Visible boosts work, XP is awarded only after a successful transaction,
and state 0 of varp 188 does not block creation. The focused contract test covers the exact IDs,
level, XP, timing and no-quest boundary; all 78 Herblore integration scenarios pass. Exact mix text,
audio, equipment-modifier applicability and packet presentation remain matching-client observations.

## Oily fishing-rod conversion

The final pre-cutoff [oily fishing rod revision
14985565](https://oldschool.runescape.wiki/w/Oily_fishing_rod?oldid=14985565) defines an immediate
members conversion from ordinary fishing rod plus blamish oil and explicitly permits creation
without starting Heroes' Quest. The final pre-cutoff [oily pearl fishing rod revision
14935072](https://oldschool.runescape.wiki/w/Oily_pearl_fishing_rod?oldid=14935072) defines the pearl
rod variant as a one-tick members conversion. Neither recipe defines a skill, XP or animation.

Cache 233 maps ordinary rod/output to 307/1585 and pearl rod/output to 22846/23122, with oil 1582.
Both combinations are registered in either held-item order. The ordinary pair commits immediately;
the pearl pair schedules queue 81 for one tick and revalidates the exact oil and rod slots before
an atomic two-input/one-output transaction. Output replaces the rod slot, full inventories are safe,
no XP or animation is added, and the shared Herblore cancellation policy applies. Four scenarios
cover identities/dispatch, timing, both orders, Heroes' Quest state zero, level one, membership,
full inventory and stale delayed slots. Exact feedback, packet order and cleaning-cloth reversal
remain matching-client observations.

## Barbarian bow Firemaking

The cutoff [Barbarian Training revision
14988720](https://oldschool.runescape.wiki/w/Barbarian_Training?oldid=14988720), dated 19 September
2025, defines this as members-only training started by Otto Godblessed. The initial task requires
base 35 Firemaking and 11 Crafting, a bow and oak logs. After the first successful oak-log fire the
player must speak to Otto again before continuing. The cache-backed `brut_fire` varbit 3759 uses
bits 4–5 and represents the four relevant states: 0 not assigned, 1 first-fire task assigned, 2
first fire lit and awaiting Otto, and 3 bow Firemaking unlocked. This plugin advances only state 1
to 2 after the fire and log conversion both commit; Otto's owning dialogue must perform state 2 to
3 when the miniquest is implemented.

The same cutoff revision pins the pre-Sailing bow table: normal and achey logs at level 21/40 XP,
oak 35/60, willow 50/90, teak 55/105, arctic pine 62/125, maple 65/135, mahogany 70/157.5, yew
80/202.5, magic 95/303.8 and redwood 99/350. Lighting with a bow awards the same XP as a tinderbox.
The requirements are the ordinary log level plus 20, capped at 99. Blisterwood has no bow-table row
and is excluded. Post-cutoff camphor, ironwood and rosewood rows belong to later Sailing-era caches
and must not leak into cache 233.4.

The cutoff item requirement allows standard log-made bows and explicitly excludes twisted, cursed,
dark, crystal, ogre, rain and signed oak bows. Cache 233 adds decisive audiovisual grouping for the
accepted catalogue: training bow 9705 uses sequence 6713; standard shortbow/longbow 841/839 use
6714; oak 843/845 uses 6715; willow 849/847 uses 6716; maple 853/851 uses 6717; yew 857/855 uses
6718; magic 861/859 and magic shortbow (i) 12788 use 6719; and Seercull 6724 uses the dedicated
dagannoth-bow sequence 6720. Every bow attempt also uses spot animation 1169. The explicit catalogue
avoids accidentally accepting weapon-category peers that the Wiki prohibits. Tests also pin no
handler for representative dark, twisted, Craw's and rain bows.

The [cutoff Firemaking revision
14960094](https://oldschool.runescape.wiki/w/Firemaking?oldid=14960094) establishes inventory bow-on-log
use: the log drops at the player's feet and kindles through the normal fire path. Both item-use
orders are supported. The existing four-tick retry chance, transactional log/fire conversion,
100–199-cycle lifetime, ashes replacement and directional step-off are shared with tinderbox
lighting. Bow attempts substitute the cache-specific sequence and spot animation and do not emit
the tinderbox strike synth.

## Standard meat-and-fish Cooking

The final pre-cutoff [Cooking revision
14978581](https://oldschool.runescape.wiki/w/Cooking?oldid=14978581), dated 2 September 2025,
establishes raw-food use on ranges and fires, lower burn rates on ranges, the Lumbridge Cook's
range, Hosidius diary ranges, cooking gauntlets and Cooking-cape burn prevention. It also defines
the normal interaction shape: using raw food on a source, or selecting `Cook` on a range, opens the
quantity interface when multiple items are available; a single selected item may proceed directly.
Only meat, fish and stew are valid on fires. The final pre-cutoff [burn-level revision
14976624](https://oldschool.runescape.wiki/w/Cooking/Burn_level?oldid=14976624), dated 30 August
2025, supplies source/equipment endpoint tables and no-burn thresholds. Its warning that the table
was still being revised after the stat-random update is retained as a matching-client acceptance
requirement rather than concealed by invented certainty.

The success roll uses the cutoff [Skilling success rate revision
14756461](https://oldschool.runescape.wiki/w/Skilling_success_rate?oldid=14756461): `low` and `high`
are interpolated over visible levels 1–99, rounded through the established half-up integer step,
then converted to the server's 0–255 roll. This is the same `statRandom` path already used by other
rsmod skill plugins. The exact endpoint pairs transcribed for the implemented core are:

| Food | Fire | Range | Lumbridge | Hosidius 5% | Hosidius elite 10% |
| --- | --- | --- | --- | --- | --- |
| Chicken, meat, shrimps, anchovies | 128/512 | 128/512 | 138/532 | 140/524 | 153/537 |
| Sardine | 118/492 | 118/492 | 128/512 | 130/504 | 143/517 |
| Herring | 108/472 | 108/472 | 118/492 | 120/484 | 133/497 |
| Trout | 88/432 | 88/432 | 98/452 | 100/444 | 113/457 |
| Salmon | 68/392 | 68/392 | 78/402 | 80/404 | 93/417 |
| Tuna | 58/372 | 58/372 | 58/372 | 70/384 | 83/397 |
| Lobster | 38/332 | 38/332 | 38/332 | 50/344 | 63/357 |
| Swordfish | 18/292 | 30/310 | 30/310 | 42/322 | 55/335 |
| Monkfish | 11/275 | 13/280 | 13/280 | 25/292 | 38/305 |
| Shark | 1/202 | 1/232 | 1/232 | 13/244 | 26/257 |
| Anglerfish | 1/200 | 1/220 | 1/220 | 13/232 | 26/245 |

Cooking-gauntlet endpoint pairs are lobster 55/368, swordfish 30/310, monkfish 24/290, shark
15/270 and anglerfish 12/260. With the Hosidius 5%/elite 10% modifiers, the stacking pairs are
lobster 67/380 and 80/393, monkfish 36/302 and 49/315, shark 27/282 and 40/295, and anglerfish
24/272 and 37/285. The cutoff table explicitly makes swordfish gauntlets ineffective at Hosidius,
so its ordinary Hosidius pairs remain in force.

The 14 raw/cooked/burnt cache triples are chicken 2138/2140/2144, beef 2132/2142/2146, shrimps
317/315/7954, anchovies 321/319/323, sardine 327/325/369, herring 345/347/357, trout
335/333/343, salmon 331/329/343, tuna 359/361/367, lobster 377/379/381, swordfish 371/373/375,
monkfish 7944/7946/7948, shark 383/385/387 and anglerfish 13439/13441/13443. Their exact
level/XP pairs are 1/30, 1/30, 1/30, 1/30, 1/40, 5/50, 15/70, 25/90, 30/100, 40/120, 45/140,
62/150, 80/210 and 84/230. Monkfish, shark and anglerfish are members-only in the cache; the first
11 rows are free-to-play.

Cache locations 26185, 26181, 114 and 27516/27517 are the ordinary fire, generic range, Lumbridge
range and two Hosidius range identities used by the initial plugin. Cache sequences 897 and 896 are
the fire- and range-cooking animations, corroborated by the workspace RuneLite constants. The
common production cadence is one tick to the first result, three more to the second and four ticks
per later result. Every result atomically converts one raw object into its cooked or burnt output;
only success grants XP. Exact messages, audio, quantity action mode, packet ordering, additional
source locations, overcooking cooked chicken/meat and all non-core recipes remain client-test gaps.

The cache-wide location contract contains exactly 48 location types with a case-insensitive `Cook`
operation. Forty-one ordinary ranges expose it as operation 1. POH fancy-range states 13542,
13543 and 13544 expose it as operation 2, so they cannot be registered through the ordinary
first-operation shortcut. Special operation-1 sources are Lumbridge 114, Hosidius 27516/27517 and
the permanent fire 43475; the latter deliberately uses fire success curves and animation. The
remaining exact IDs are 7183, 7184, 7421, 8750, 9682, 9736, 10377, 12102, 12269, 12611, 16641,
16702, 16893, 17131, 21302, 21792, 22713, 22714, 25730, 26181, 27724, 30932, 31631, 32631,
32739, 35980, 36077, 37728, 39391, 39458, 39490, 40149, 47925, 48169, 50563, 51362, 51540,
52647, 54049, 54449 and 57332. The historical [Gnome cooker revision
14480419](https://oldschool.runescape.wiki/w/Gnome_cooker?oldid=14480419) explicitly corroborates
that source 17131 is functionally an ordinary range rather than a gnome-food-only exception.

Cook's Assistant uses cache varp 29 (`cookquest`), and quest database row 17 pins completion at
column 19 value 2. The cutoff Cooking revision requires completion before using Lumbridge's
Cook-o-matic range; both direct raw-item and range-operation entry points now apply that gate.
Exact pre-completion feedback remains a matching-client observation. This cache-operation inventory
does not prove the absence of cookable scenery without a visible `Cook` operation, so permanent
fires, fireplaces, sulphur vents and other item-use-only heat sources remain a separate audit.

## Ordinary meat aliases and deliberate burning

The final pre-cutoff [Cooked meat revision
14980684](https://oldschool.runescape.wiki/w/Cooked_meat?oldid=14980684), dated 5 September 2025,
lists raw beef, rat, bear, boar and yak meat as level-1, 30-XP inputs producing cooked meat. Rat,
bear and boar meat are free-to-play; yak meat is members-only. It publishes the same fire/range
128/512, Lumbridge 138/532, Hosidius 140/524 and Hosidius-elite 153/537 success endpoints used by
beef, and the standard first-result-at-one, second-at-four, then four-tick cadence. The final
pre-cutoff [Cooked mystery meat revision
14937167](https://oldschool.runescape.wiki/w/Cooked_mystery_meat?oldid=14937167), dated 13 July
2025, independently supplies the same level, XP, endpoint table and cadence for the members-only
mystery-meat route.

The cache-233 identities are raw rat meat 2134, raw bear meat 2136, raw boar meat 25833, raw yak
meat 10816 and raw mystery meat 24782. The first four produce cooked meat 2142 and mystery meat
produces cooked mystery meat 24785; all five failures produce burnt meat 2146. These typed inputs
share the existing transactional source, diary, cape, quantity and interruption policy rather than
duplicating the same behavior in per-item handlers.

The final pre-cutoff [Burnt meat revision
14973773](https://oldschool.runescape.wiki/w/Burnt_meat?oldid=14973773), dated 25 August 2025,
defines cooked meat 2142 and cooked mystery meat 24785 as boosted level-1 inputs that always create
burnt meat 2146, award zero XP and use the 1/3/4 Make-X cadence. Cooked mystery meat remains
members-only. The pre-cutoff [chat-message catalogue revision
14777106](https://oldschool.runescape.wiki/w/User:Skretzo/Chat_messages?oldid=14777106), dated 15
October 2024, pins `You deliberately burn the nicely cooked meat.` as the deliberate transform's
message, plus the ordinary beef success and accidental-burn messages. Equivalent same-output meat
aliases reuse those beef messages pending matching-client confirmation.

The [Burnt chicken revision
14875954](https://oldschool.runescape.wiki/w/Burnt_chicken?oldid=14875954), dated 2 April 2025,
proves that cooked chicken can deliberately become burnt chicken for zero XP and supplies its exact
message, but leaves the recipe's tick field empty. Because the goal forbids silent timing guesses,
that independent route remains researched until cache-233.4 client observation establishes its
single and repeated timing. Whether a worn Cooking or max cape affects either deliberate transform,
and the exact left-click range chooser presentation when cooked and raw foods coexist, also remain
matching-client observations.

The generic cache-233 skill-multiple clientscript 2046 accepts exactly ten product objects. The
server therefore collapses duplicate cooked outputs (notably the five ordinary inputs producing
cooked meat) and presents at most ten products on a range-originated chooser; using an inventory
item directly still selects that exact input regardless of the other food present. The clientscript
argument bound and server-side no-overflow behavior are integration-tested. Which ten products the
production server prioritises when more than ten distinct outputs coexist remains matching-client
evidence rather than a verified ordering claim.

## Legacy rabbit, ugthanki, snail and lava-eel Cooking

Six final pre-cutoff item revisions define another ordinary fire/range family: [Cooked rabbit
14787730](https://oldschool.runescape.wiki/w/Cooked_rabbit?oldid=14787730), [Ugthanki meat
14787727](https://oldschool.runescape.wiki/w/Ugthanki_meat?oldid=14787727), [Thin snail meat
14787732](https://oldschool.runescape.wiki/w/Thin_snail_meat?oldid=14787732), [Lean snail meat
14787736](https://oldschool.runescape.wiki/w/Lean_snail_meat?oldid=14787736), [Fat snail meat
14787738](https://oldschool.runescape.wiki/w/Fat_snail_meat?oldid=14787738) and [Lava eel
14951861](https://oldschool.runescape.wiki/w/Lava_eel?oldid=14951861). All six routes are
members-only and use the standard one-tick selected result and four-tick Make-X family, represented
by the established first-at-one, second-at-four and later-four-tick queue.

The burnable recipes' exact level/XP and success endpoints are:

| Food | Level / XP | Fire | Range | Lumbridge | Hosidius 5% | Hosidius elite 10% |
| --- | --- | --- | --- | --- | --- | --- |
| Rabbit | 1 / 30 | 128/512 | 128/512 | 128/512 | 140/524 | 153/537 |
| Ugthanki meat | 1 / 40 | 40/252 | 30/253 | 30/253 | 42/265 | 55/278 |
| Thin snail | 12 / 70 | 93/444 | 93/444 | 103/464 | 105/456 | 118/469 |
| Lean snail | 17 / 80 | 93/444 | 85/428 | 95/448 | 97/440 | 110/453 |
| Fat snail | 22 / 95 | 73/402 | 73/402 | 83/422 | 85/414 | 98/427 |

The unusual inequalities are intentional transcriptions: ugthanki and lean-snail fire curves are
not interchangeable with their ordinary-range curves. None of these foods has a cooking-gauntlet
curve. Cache 233 resolves rabbit as 3226/3228/2146, ugthanki meat as 1859/1861/2146, thin snail as
3363/3369/3375, lean snail as 3365/3371/3375 and fat snail as 3367/3373/3375. The ordinary shared
pipeline pins typed identities, visible-level checks, membership, every source curve, success-only
XP, zero-XP burns, full-inventory replacement and interruption behavior.

Lava eel is a distinct no-burn row: raw lava eel 2148 always becomes lava eel 2149 at boosted level
53 for 30 XP and has no burnt-object result or success chart. The recipe model therefore records a
nullable burnt output and an explicit always-success rule; it does not manufacture a placeholder
failure. The article places acquisition within Heroes' Quest but does not specify a quest-state
check on Cooking an item already possessed, so production remains possession-based. Thin-snail
Cooking is also a Morytania easy diary task; its future task-credit hook remains in the shared diary
layer rather than mutating a Cooking-owned substitute.

## Chompy and jubbly spit-roast systems

The final pre-cutoff [Cooked chompy revision
14982500](https://oldschool.runescape.wiki/w/Cooked_chompy?oldid=14982500) defines two source-specific
methods that cannot be represented as an ordinary raw-food recipe. Raw chompy 2876 must first be
combined with iron spit 7225 to make skewered chompy 7230. Cooking that item on a fire requires
Cooking 30 and Firemaking 20, takes three ticks, awards 100 XP and returns cooked chompy 2878 plus the iron spit; failure
produces ruined chompy 2880. Its fire success endpoints are 200/255. The separate ogre-spit-roast
route accepts raw chompy directly and takes five ticks. Final pre-cutoff [Skewered chompy revision
14995319](https://oldschool.runescape.wiki/w/Skewered_chompy?oldid=14995319), [Raw chompy revision
14995318](https://oldschool.runescape.wiki/w/Raw_chompy?oldid=14995318) and [Iron spit revision
14995320](https://oldschool.runescape.wiki/w/Iron_spit?oldid=14995320) additionally pin immediate,
zero-XP preparation, boostable Cooking 30 and Firemaking 20 requirements, and the members-only
iron-spit family. The dedicated production handler implements that preparation in either item-use
order and cooks only on cache fires 26185/43475 with sequence 1016, a three-tick result, cape
protection, atomic cooked-or-ruined output plus returned spit, capacity rejection and distance/input
revalidation. The exact success/failure and level/capacity messages remain matching-client
observations. Direct ogre-spit-roast Cooking is implemented separately by the shared state machine
described below.

The final pre-cutoff [Cooked jubbly revision
14978312](https://oldschool.runescape.wiki/w/Cooked_jubbly?oldid=14978312) defines raw/cooked/burnt
objects 7566/7568/7570, boosted level 41, 160 XP and success endpoints 191/245. It is members-only,
requires the relevant Freeing Skrach Uglogwee subquest progress, and can only be cooked on an ogre
spit roast. Cache DB row 2313 pins completion at progress 170; dated Quest Helper commit
[fc4e126](https://github.com/Zoinkwiz/quest-helper/blob/fc4e126d4313380841efa63ecc79f46564080f38/src/main/java/com/questhelper/helpers/quests/recipefordisaster/RFDSkrachUglogwee.java)
from 10 April 2025 maps the raw-jubbly cooking phase to varbit 1904 state 110. The first result takes six
ticks; removing the bird occupies the roast for one further tick, making manually started
consecutive results seven ticks apart.

The cutoff [Cooking revision
14978581](https://oldschool.runescape.wiki/w/Cooking?oldid=14978581), [burn-level revision
14976624](https://oldschool.runescape.wiki/w/Cooking/Burn_level?oldid=14976624) and [ogre-spit-roast
revision 14924516](https://oldschool.runescape.wiki/w/Ogre_spit-roast?oldid=14924516) identify raw
chicken as the third and only other accepted input. The route is members-only, needs boosted
Cooking 30, awards 30 XP and takes six ticks. It reuses raw/cooked/burnt chicken objects
2138/2140/2144 and the standard chicken 128/512 success curve, whose burn behavior stops at level
34. Cache 233 has no distinct burnt-chicken spit morph: player state 3 intentionally shares the
ruined-chompy location 3374.

Cache multi-location 6895 is controlled per player by varbit 1770 and maps empty/raw/cooked/burnt
Chompy, raw/cooked chicken and raw/cooked/burnt jubbly states across locations 3375, 3372, 3373,
3374, 3382, 3383, 12561, 12562 and 12563. Base sequence 1017 and player sequences 3181/3182 pin
the roast and place/remove animations. Production queue 84 implements selected-slot atomic
conversion after five ticks for Chompy and six for chicken/jubbly. Soft timer 12 holds the raw
state while that weak queue exists, clears an interrupted roast on the next tick and exposes a
cooked/burnt result for exactly one tick before restoring empty state. Login and logout also clear
both state and work; movement, replacement work and stale selected slots cannot commit a result.
All three accepted foods are therefore `integration-tested`. Exact messages are retained as
matching-client observations corroborated by the workspace RuneLite Cooking plugin's message
prefixes; sound identity and packet ordering, and the quest-specific seasoned-chompy workflow,
remain open.

## Range-baked bread and pitta bread

The final pre-cutoff [Bread revision
14918368](https://oldschool.runescape.wiki/w/Bread?oldid=14918368) defines bread dough 2307 as a
free-to-play, range-only level-1 input producing bread 2309 for 40 XP or burnt bread 2311. A manual
result takes one tick and Make-X takes four ticks per item. Its success endpoints are ordinary
range 118/492, Lumbridge 128/512, Hosidius 130/504 and Hosidius elite 143/517; ordinary ranges stop
burning it at level 38 and Lumbridge at 34. The associated final [Bread dough revision
14867365](https://oldschool.runescape.wiki/w/Bread_dough?oldid=14867365) confirms that it must be
baked on a range and separately identifies pot-of-flour mixing with bucket, bowl or jug water as
the preparation route.

The final pre-cutoff [Pitta bread revision
14982496](https://oldschool.runescape.wiki/w/Pitta_bread?oldid=14982496) and [Pitta dough revision
14871428](https://oldschool.runescape.wiki/w/Pitta_dough?oldid=14871428) define the members-only,
boostable level-58 range route from pitta dough 1863 to pitta bread 1865 for 40 XP. The ordinary and
Hosidius endpoints are 118/492, 130/504 and 143/517, but the theoretical burn threshold is level
37; therefore every eligible level-58 player succeeds and burnt pitta bread 1867 remains an
unreachable cache object. The result is one tick manually and four ticks in Make-All.

Both recipes reuse the transactional standard Cooking queue and range animation while adding an
explicit allowed-source set. The operation chooser and held-item registrations omit both doughs on
fires but include ordinary, Lumbridge and Hosidius ranges; direct service calls independently
enforce the same boundary. Tests pin cache identities, source registration, bread success and burn
on the 1/3/4 cadence, pitta membership and boosted-level gates, forced pitta success, full recipe
curves and unchanged input/default engine feedback on a fire. The water-and-flour dough chooser,
container return/capacity precedence, exact failure/success messages, audio and packet ordering
remain separate work and are not implied by baking coverage.

## Stew heat stage

The final pre-cutoff [Stew revision
14882114](https://oldschool.runescape.wiki/w/Stew?oldid=14882114) defines uncooked stew 2001 as a
free-to-play, boostable Cooking-25 input accepted by both fires and ranges. A successful result is
stew 2003 and 117 XP; failure is burnt stew 2005, corroborated by final pre-cutoff [Burnt stew
revision 14419629](https://oldschool.runescape.wiki/w/Burnt_stew?oldid=14419629). Manual Cooking
takes one tick and Make-X results follow the explicit 1/3/4 cadence. Fire and ordinary-range
endpoints are 68/392, Lumbridge is 78/412, Hosidius 5% is 80/404 and Hosidius elite is 93/417.
Ordinary fire/range Cooking stops burning at level 58 and Lumbridge at level 54.

The final pre-cutoff [Uncooked stew revision
14928848](https://oldschool.runescape.wiki/w/Uncooked_stew?oldid=14928848) separately defines the
two-step preparation graph: potato plus bowl of water creates one incomplete state; cooked meat or
cooked chicken supplies the other ingredient; either ingredient order produces the same uncooked
stew, while spice or three curry leaves branches it into uncooked curry. That assembly is covered
by the separate preparation slice below rather than being silently folded into this heat stage.

Production registers uncooked stew through the shared cache-wide fire/range dispatch and uses the
existing source-aware transactional queue. Integration tests pin identities, all endpoint pairs,
both source classes, boosted level gating, forced success and burn on the exact cadence, ordinary
level-58 no-burn behavior, max-cape protection and same-slot replacement in a full inventory.
Exact messages, audio and packet ordering remain matching-client work.

## Curry heat stage

The final pre-cutoff [Curry revision
14703279](https://oldschool.runescape.wiki/w/Curry?oldid=14703279), dated 18 July 2024, defines
uncooked curry 2009 as a members-only, boostable Cooking-60 input accepted by both fires and
ranges. Success produces curry 2011 and awards 280 XP; failure produces burnt curry 2013, whose
identity and level-74 no-burn boundary are independently corroborated by final pre-cutoff [Burnt
curry revision 14419630](https://oldschool.runescape.wiki/w/Burnt_curry?oldid=14419630), dated 16
June 2023. The recipe gives a four-tick production duration, which uses the established standard
Cooking manual/Make-X result cadence of ticks 1, 4, 8 and onward.

The success chart pins fire, ordinary-range and Lumbridge-fallback endpoints at 38/332, Hosidius
5% at 50/344 and Hosidius elite at 63/357. The final pre-cutoff [Cooking burn-level revision
14976624](https://oldschool.runescape.wiki/w/Cooking/Burn_level?oldid=14976624), dated 30 August
2025, independently lists level 74 for ordinary sources and level 70 for Hosidius 5%; the elite
column is not applicable because elite diary completion already requires a higher Cooking level.

The final pre-cutoff [Uncooked curry revision
14891634](https://oldschool.runescape.wiki/w/Uncooked_curry?oldid=14891634), dated 27 April 2025,
defines the separate zero-XP two-tick preparation step: one spice or three curry leaves plus
uncooked stew. That item-use graph is covered by the separate preparation slice below.

Production registers uncooked curry through the shared cache-wide fire/range dispatch and uses the
same transactional queue as standard food. Integration tests pin all identities and curves,
membership, boosted-level acceptance, low-level rejection, forced success and burn, 1/3/4
cadence, level-74 guaranteed success and full-inventory same-slot replacement. Exact messages,
audio and packet ordering remain matching-client work.

## Stew and curry preparation

The final pre-cutoff [Uncooked stew revision
14928848](https://oldschool.runescape.wiki/w/Uncooked_stew?oldid=14928848), dated 28 June 2025, and
[Incomplete stew revision
14928840](https://oldschool.runescape.wiki/w/Incomplete_stew?oldid=14928840), also dated 28 June
2025, define both intermediate identities and all six free-to-play routes. Bowl of water 1921 plus
potato 1942 produces potato-first incomplete stew 1997 (`stew1`); bowl water plus cooked meat 2142
or cooked chicken 2140 produces meat-first incomplete stew 1999 (`stew2`). Adding cooked meat or
chicken to 1997, or potato to 1999, produces uncooked stew 2001. Every stage has a boostable
Cooking-25 gate, awards zero XP, takes two ticks and needs no knife. The final pre-cutoff [Bowl of
water revision 14861229](https://oldschool.runescape.wiki/w/Bowl_of_water?oldid=14861229), dated 11
March 2025, and [Potato revision
14943915](https://oldschool.runescape.wiki/w/Potato?oldid=14943915), dated 23 July 2025,
corroborate the starting objects and ingredient-order behavior.

The stew articles preserve the exact potato and meat result patterns: `You cut up the potato and
put it into the bowl.`, `You cut up the meat and put it into the bowl.` and, for the second stage,
`You cut up the meat and put it into the stew.` The source expresses chicken alternatives as
`meat/chicken`; production expands the parallel chicken routes to `You cut up the chicken and put
it into the bowl.` and `You cut up the chicken and put it into the stew.` That expansion is an
explicit inference and must be checked against a matching cache-233.4 client before the text can be
called verified. The potato-final route retains the source's bowl wording.

The final pre-cutoff [Uncooked curry revision
14891634](https://oldschool.runescape.wiki/w/Uncooked_curry?oldid=14891634), dated 27 April 2025,
and [Curry revision 14703279](https://oldschool.runescape.wiki/w/Curry?oldid=14703279), dated 18
July 2024, define two members-only, boostable Cooking-60, zero-XP and two-tick routes: uncooked stew
2001 plus one spice 2007, or uncooked stew plus exactly three non-stackable curry leaves 5970,
produces uncooked curry 2009. The final pre-cutoff [Spice revision
14915541](https://oldschool.runescape.wiki/w/Spice?oldid=14915541), dated 5 June 2025, and [Curry
leaf revision 14463589](https://oldschool.runescape.wiki/w/Curry_leaf?oldid=14463589), dated 12
September 2023, corroborate those ingredient identities and the three-leaf quantity.

Production adds eight recipes to the existing data-driven FoodPreparation queue, for 32 total
recipes and 23 unique held-item registrations. Both held-item orders dispatch symmetrically. Every
original selected and additional slot is revalidated after the two-tick delay, so the three-leaf
route consumes three distinct leaves atomically and cancels without partial consumption if any one
disappears. Integration tests cover all six stew routes, both curry routes, boosted and rejected
levels, F2P/member boundaries, zero XP, exact timing, full-inventory one-for-one replacement,
two-leaf rejection and stale-third-leaf cancellation. Exact curry messages, failure precedence,
output-slot order, animation, sound and packet order have no pinned historical evidence and remain
matching-client observations; no audiovisual behavior was invented.

## Baked-potato heat stage

The final pre-cutoff [Baked potato revision
14889126](https://oldschool.runescape.wiki/w/Baked_potato?oldid=14889126), dated 22 April 2025,
defines a range-only, members-only and boostable Cooking-7 action that converts potato 1942 to
baked potato 6701 for 15 XP or burnt potato 6699 for zero XP. It explicitly rejects fires and pins
manual/Make-All results at ticks 1, 4, 8 and onward. Its success chart gives ordinary-range and
Lumbridge-fallback endpoints 108/472, Hosidius 5% endpoints 120/484 and Hosidius elite endpoints
133/497.

The final pre-cutoff [Potato revision
14943915](https://oldschool.runescape.wiki/w/Potato?oldid=14943915), dated 23 July 2025, confirms
that raw potato 1942 is itself a free-to-play object while baking is restricted to members. The
final pre-cutoff [Burnt potato revision
14799746](https://oldschool.runescape.wiki/w/Burnt_potato?oldid=14799746), dated 14 November 2024,
independently pins burnt potato 6699 and the ordinary level-41 no-burn boundary.

Production uses the typed range-source restriction already shared by bread and pitta. Integration
tests pin the cache's `potato_baked`/`potato_burnt` symbol names, IDs, all curves, membership action
gate, boosted level, forced burn then success, 1/3/4 cadence, full-inventory same-slot replacement,
level-41 guaranteed success and unchanged input/default feedback on a fire. Potato toppings are
covered by the integration-tested preparation graph below; exact messages, audio and packet
ordering remain separate work.

## Ordinary vegetable Cooking heat family

Four final pre-cutoff success pages define a shared members-only fire/range family. [Scrambled egg
14961240](https://oldschool.runescape.wiki/w/Scrambled_egg?oldid=14961240), dated 7 August 2025,
pins uncooked/scrambled/burnt egg 7076/7078/7090, boosted level 13, 50 XP, 90/438 ordinary,
102/450 Hosidius and 115/463 elite endpoints, level-48 no-burn, and an immediate first result with
later +3/+4-tick gaps. [Cooked sweetcorn
14943730](https://oldschool.runescape.wiki/w/Cooked_sweetcorn?oldid=14943730), dated 23 July 2025,
pins sweetcorn/cooked/burnt 5986/5988/5990, boosted level 28, 104 XP, 78/412, 90/424 and 103/437
endpoints, level-54 no-burn and 1/3/4 timing.

[Fried onions 14420813](https://oldschool.runescape.wiki/w/Fried_onions?oldid=14420813), dated 16
June 2023, pins chopped/fried/burnt onion 1871/7084/7092, boosted level 42, 60 XP, 36/322, 48/334
and 61/347 endpoints, level-77 no-burn and 1/3/4 timing. [Fried mushrooms
14420794](https://oldschool.runescape.wiki/w/Fried_mushrooms?oldid=14420794), also dated 16 June
2023, pins sliced/fried/burnt mushrooms 7080/7082/7094, boosted level 46, 60 XP, 16/282, 28/294
and 41/307 endpoints, level-90 no-burn and 1/3/4 timing. The matching final input and burnt-item
revisions independently confirm every object identity and no-burn boundary: [Uncooked egg
14743606](https://oldschool.runescape.wiki/w/Uncooked_egg?oldid=14743606), [Burnt egg
14419627](https://oldschool.runescape.wiki/w/Burnt_egg?oldid=14419627), [Sweetcorn
14938238](https://oldschool.runescape.wiki/w/Sweetcorn?oldid=14938238), [Burnt sweetcorn
14419562](https://oldschool.runescape.wiki/w/Burnt_sweetcorn?oldid=14419562), [Chopped onion
14781651](https://oldschool.runescape.wiki/w/Chopped_onion?oldid=14781651), [Burnt onion
14419634](https://oldschool.runescape.wiki/w/Burnt_onion?oldid=14419634), [Sliced mushrooms
14820158](https://oldschool.runescape.wiki/w/Sliced_mushrooms?oldid=14820158) and [Burnt mushroom
14419628](https://oldschool.runescape.wiki/w/Burnt_mushroom?oldid=14419628).

There is a source presentation conflict. Each success page's behavior prose accepts both a fire
and range, as do the uncooked-egg and sliced-mushroom pages and the final pre-cutoff combined
fire/range table in [Cooking/Burn level
14976624](https://oldschool.runescape.wiki/w/Cooking/Burn_level?oldid=14976624). However, the
embedded recipe templates name only a Cooking range, the egg success chart labels its ordinary
curve `Range`, and the same burn-level page's acquisition shorthand calls burnt egg, onion and
mushroom range-only. The implementation follows the repeated item-specific prose and combined
success table, uses identical ordinary fire/range curves, and retains live cache-233 RuneLite fire
confirmation as a manual acceptance requirement rather than hiding the conflict.

The generic Cooking scheduler now distinguishes results initiated outside its queue processor from
queue-handler results. This preserves every existing 1/3/4 recipe while allowing scrambled egg to
commit at tick 0 and queue its next result exactly three ticks later. Integration coverage exercises
each route's identities, curves, success, burn, boosted and rejected levels, membership and no-burn
boundary, plus full-inventory replacement for the common transactional path. Ingredient assembly,
The bowl-input and topping operations are covered by the following integration-tested slice;
messages, audio and packet ordering remain separate work.

## Potato toppings and prerequisite bowl mixtures

The final pre-cutoff [Potato with butter revision
14799748](https://oldschool.runescape.wiki/w/Potato_with_butter?oldid=14799748), dated 14 November
2024, pins both boostable members-only Cooking-39 routes: baked potato 6701 plus pat of butter 6697
or pat of not garlic butter 24788 produces potato with butter 6703 after two ticks for 40 XP. The
matching [Pat of butter revision
14815125](https://oldschool.runescape.wiki/w/Pat_of_butter?oldid=14815125) confirms ordinary butter's
identity, while [Pat of not garlic butter revision
14781725](https://oldschool.runescape.wiki/w/Pat_of_not_garlic_butter?oldid=14781725) explicitly says
the Darkmeyer item works identically when possessed. Its acquisition is area/progression-limited,
but the item is marked non-quest and the Cooking action has no additional quest-state requirement.

The five final toppings are pinned by [Potato with cheese
14799749](https://oldschool.runescape.wiki/w/Potato_with_cheese?oldid=14799749), [Chilli potato
14419723](https://oldschool.runescape.wiki/w/Chilli_potato?oldid=14419723), [Egg potato
14957180](https://oldschool.runescape.wiki/w/Egg_potato?oldid=14957180), [Mushroom potato
14829336](https://oldschool.runescape.wiki/w/Mushroom_potato?oldid=14829336) and [Tuna potato
14985508](https://oldschool.runescape.wiki/w/Tuna_potato?oldid=14985508). Each is members-only,
boostable and takes two ticks:

| Input used with potato with butter 6703 | Output | Level | XP | Returned container |
| --- | --- | ---: | ---: | --- |
| cheese 1985 | potato with cheese 6705 | 47 | 40 | none |
| chilli con carne 7062 | chilli potato 7054 | 41 | 15 | bowl 1923 |
| egg and tomato 7064 | egg potato 7056 | 51 | 45 | none |
| mushroom and onion 7066 | mushroom potato 7058 | 64 | 55 | none |
| tuna and corn 7068 | tuna potato 7060 | 68 | 10 | bowl 1923 |

The output declarations are intentionally recipe-specific. The cutoff recipe templates explicitly
return bowls from chilli and tuna potatoes but do not list a returned bowl for egg or mushroom
potatoes. Production follows that distinction instead of applying a blanket container rule. The
cutoff [Cheese revision
14963997](https://oldschool.runescape.wiki/w/Cheese?oldid=14963997) also confirms that cheese itself
is free-world, while this potato action remains members-only.

Four final pre-cutoff mixture pages define the branches feeding those toppings: [Chilli con carne
14972296](https://oldschool.runescape.wiki/w/Chilli_con_carne?oldid=14972296), [Egg and tomato
14420407](https://oldschool.runescape.wiki/w/Egg_and_tomato?oldid=14420407), [Mushroom and onion
14545939](https://oldschool.runescape.wiki/w/Mushroom_%26_onion?oldid=14545939) and [Tuna and corn
14424969](https://oldschool.runescape.wiki/w/Tuna_and_corn?oldid=14424969). Together with [Spicy sauce
14915545](https://oldschool.runescape.wiki/w/Spicy_sauce?oldid=14915545), they pin these intermediate
actions:

| Inputs | Output | Level | XP | Ticks | Knife | Extra bowl returned |
| --- | --- | ---: | ---: | ---: | --- | --- |
| chopped garlic 7074 + gnome spice 2169 | spicy sauce 7072 | 9 | 25 | 2 | no | no |
| spicy sauce 7072 + cooked meat 2142 | chilli con carne 7062 | 11 | 0 | 1 | yes | no |
| spicy sauce 7072 + minced meat 7070 | chilli con carne 7062 | 11 | 0 | 1 | no | yes |
| scrambled egg 7078 + tomato 1982 | egg and tomato 7064 | 23 | 0 | 2 | no | no |
| fried mushrooms 7082 + fried onions 7084 | mushroom and onion 7066 | 57 | 0 | 2 | no | no |
| bowl sweetcorn 7088 + tuna 361 | tuna and corn 7068 | 67 | 0 | 2 | yes | no |
| bowl sweetcorn 7088 + chopped tuna 7086 | tuna and corn 7068 | 67 | 0 | 2 | no | yes |
| chopped tuna 7086 + cooked sweetcorn 5988 | tuna and corn 7068 | 67 | 0 | 2 | no | no |

Only gnome spice 2169 is valid for spicy sauce; ordinary spice and Evil Dave spice variants are
explicitly excluded. Only the direct cooked-meat chilli route needs a knife because minced meat is
already cut. Likewise, only the tuna route starting from an uncut cooked tuna requires a knife.
The two one-tick chilli-con-carne routes are not widened to the usual two-tick preparation delay.

The source graph begins with six bowl preparations. Final pre-cutoff revisions [Chopped garlic
14781653](https://oldschool.runescape.wiki/w/Chopped_garlic?oldid=14781653), [Minced meat
14422326](https://oldschool.runescape.wiki/w/Minced_meat?oldid=14422326), [Uncooked egg
14743606](https://oldschool.runescape.wiki/w/Uncooked_egg?oldid=14743606), [Sliced mushrooms
14820158](https://oldschool.runescape.wiki/w/Sliced_mushrooms?oldid=14820158), [Chopped onion
14781651](https://oldschool.runescape.wiki/w/Chopped_onion?oldid=14781651), [Chopped tuna
14898777](https://oldschool.runescape.wiki/w/Chopped_tuna?oldid=14898777) and [Sweetcorn bowl
14795993](https://oldschool.runescape.wiki/w/Sweetcorn_(bowl)?oldid=14795993) establish exact
two-tick transforms. Bowl 1923 plus garlic 1550, cooked meat 2142, bittercap mushroom 6004, onion
1957 or tuna 361 becomes 7074, 7070, 7080, 1871 or 7086 respectively and retains a knife as a
tool; bowl plus cooked sweetcorn 5988 becomes 7088 without a knife. Bowl plus egg 1944 becomes
uncooked egg 7076 at boostable Cooking 13 without a knife. The no-level recipes are represented by
the minimum attainable Cooking level 1 and award zero XP. [Gnome spice
14915546](https://oldschool.runescape.wiki/w/Gnome_spice?oldid=14915546) and [Mushroom
14889096](https://oldschool.runescape.wiki/w/Mushroom?oldid=14889096) independently pin ingredients
2169 and 6004.

Production adds 22 data-driven recipes to the existing FoodPreparation queue, for 80 total recipes
and 71 registered preparation pairs including the three dough-water routes. Every pair works in
either held-item order; exact selected slots, membership, boosted Cooking level, knife presence and
capacity are checked both when the action starts and immediately before its atomic transaction.
Four cache-aware scenarios bring the focused Cooking suite to 47 passing tests and cover the exact
graph, cache identities, levels, XP, one/two-tick results, all four explicit returned-bowl paths in
a full inventory, valid no-knife routes, missing/stale knife cancellation and replacement by a
newer Cooking action. The local cache-233 RuneLite `CookingAction` catalogue independently agrees
with the final item identities, levels and from-scratch XP totals but exposes no preparation
animation, sound, message or packet behavior. Exact missing-knife and success feedback, animation,
sound and slot/packet order therefore remain matching-client observations rather than invented
claims.

## Dairy churn production

The final pre-cutoff [Dairy churn revision
14799015](https://oldschool.runescape.wiki/w/Dairy_churn?oldid=14799015), dated 14 November 2024,
defines the production facility and all seven members-only, boostable paths. The final relevant
[Pot of cream revision
14423018](https://oldschool.runescape.wiki/w/Pot_of_cream?oldid=14423018), [Pat of butter revision
14815125](https://oldschool.runescape.wiki/w/Pat_of_butter?oldid=14815125) and [Cheese revision
14963997](https://oldschool.runescape.wiki/w/Cheese?oldid=14963997) independently pin the input and
output identities, per-stage XP and direct-versus-intermediate timings:

| Input | Output | Level | XP | Result ticks | Returned container |
| --- | --- | ---: | ---: | ---: | --- |
| bucket of milk 1927 | pot of cream 2130 | 21 | 18 | 10 | bucket 1925 |
| bucket of milk 1927 | pat of butter 6697 | 38 | 40.5 | 19 | bucket 1925 |
| bucket of milk 1927 | cheese 1985 | 48 | 64 | 26 | bucket 1925 |
| pot of cream 2130 | pat of butter 6697 | 38 | 22.5 | 10 | none |
| pot of cream 2130 | cheese 1985 | 48 | 46 | 19 | none |
| pat of butter 6697 | cheese 1985 | 48 | 23.5 | 10 | none |
| pat of not garlic butter 24788 | cheese 1985 | 48 | 23.5 | 10 | none |

The churn revision explicitly identifies cache locations 11695, 11696, 25720 and 35937, each of
which exposes first operation `Churn` in cache 233.4. It also records the full-inventory milk edge:
the empty bucket is dropped rather than blocking production. Production uses the shared protected
inventory helper, so the fallback is a private owner drop and the bucket is added normally whenever
space exists. Cream-to-butter consumes the cream pot without returning an empty pot; this follows
both the churn output table and the butter page's direct-route prose rather than assuming all dairy
containers survive processing.

Cache sequences 2793/2794/2795 are the short, medium and long player actions. Churn map spot
animations 459/460, 461/462 and 463/464 are the regular/metal pairs, and their embedded animation
references resolve to churn sequences 2790/2791/2792 respectively. The Cooks' Guild identity 11696
selects the metal variant; the other three use the regular variant. The local revision-matched
RuneLite `AnimationID` and game-value catalogue agree with these identities, while its skill
calculator independently agrees with cheese's level 48 and 64 XP from milk.

Production registers native `Churn` and held-item use for every input at every cache location. The
menu is filtered by the chosen source so milk exposes cream/butter/cheese, cream exposes
butter/cheese, and either butter exposes cheese. Batches revalidate distance, membership, boosted
Cooking level and the selected input immediately before each atomic result, share Cooking's weak
queue interruption policy and replay the matching player and churn animation for every item. Five
cache-aware scenarios bring the focused Cooking suite to 52 passing tests and cover all seven
identities, four location operations, six spot animations, three player/churn sequence tiers,
exact result ticks and XP, repeated batches, boost and free-world boundaries, stale slots,
movement, shared interruption, ordinary bucket return and the full-inventory private drop.

The dated Wiki chat-message catalogue and full-text historical search expose no exact churn
feedback. Older interface inventories support the inherited title `What would you like to make in
the churn?` and three choices ordered cream, butter and cheese, but do not prove the exact cache-233
button labels or action mode. Native-click source priority when milk, cream and butter coexist is
implemented deterministically as milk, cream, butter, then not-garlic butter, but remains an
explicit matching-client observation. Exact success/failure wording, sound, map-graphic placement
and packet ordering likewise remain manual evidence gaps and prevent promotion beyond
`integration-tested`.

## Standard pie preparation

The final pre-cutoff [Pie shell revision
14820627](https://oldschool.runescape.wiki/w/Pie_shell?oldid=14820627), dated 10 December 2024,
defines the common free-world start: pastry dough 1953 plus pie dish 2313 produces pie shell 2315
at boostable Cooking 1, awards no XP and takes two ticks. The historical [Making pie shells
revision 14221361](https://oldschool.runescape.wiki/w/Money_making_guide/Making_pie_shells?oldid=14221361)
independently records 14-and-14 inventory batching, held-item use opening the production choice and
spacebar/Make-All continuation. Production therefore treats this and every standard filling stage
as bounded batch preparation rather than an immediate one-off transform.

The final pre-cutoff intermediate-item revisions pin the complete ordered graph. The three
free-world fillings are [Uncooked berry pie
14800628](https://oldschool.runescape.wiki/w/Uncooked_berry_pie?oldid=14800628), [Uncooked meat pie
14786362](https://oldschool.runescape.wiki/w/Uncooked_meat_pie?oldid=14786362) and [Uncooked apple pie
14786361](https://oldschool.runescape.wiki/w/Uncooked_apple_pie?oldid=14786361). Meat pie accepts either
cooked meat 2142 or cooked chicken 2140. The members-only staged families are evidenced by [Part
mud pie (compost) 14737477](https://oldschool.runescape.wiki/w/Part_mud_pie_(compost)?oldid=14737477),
[Part mud pie (water) 14749063](https://oldschool.runescape.wiki/w/Part_mud_pie_(water)?oldid=14749063)
and [Raw mud pie 14737479](https://oldschool.runescape.wiki/w/Raw_mud_pie?oldid=14737479); [Part garden
pie (tomato) 14737508](https://oldschool.runescape.wiki/w/Part_garden_pie_(tomato)?oldid=14737508),
[Part garden pie (onion) 14734813](https://oldschool.runescape.wiki/w/Part_garden_pie_(onion)?oldid=14734813)
and [Raw garden pie 14734809](https://oldschool.runescape.wiki/w/Raw_garden_pie?oldid=14734809); and
[Part fish pie (trout) 14737506](https://oldschool.runescape.wiki/w/Part_fish_pie_(trout)?oldid=14737506),
[Part fish pie (cod) 14734816](https://oldschool.runescape.wiki/w/Part_fish_pie_(cod)?oldid=14734816)
and [Raw fish pie 14734862](https://oldschool.runescape.wiki/w/Raw_fish_pie?oldid=14734862).

The remaining cutoff revisions are [Uncooked botanical pie
14679525](https://oldschool.runescape.wiki/w/Uncooked_botanical_pie?oldid=14679525), [Uncooked mushroom
pie 14816781](https://oldschool.runescape.wiki/w/Uncooked_mushroom_pie?oldid=14816781), [Part admiral pie
(salmon) 14737493](https://oldschool.runescape.wiki/w/Part_admiral_pie_(salmon)?oldid=14737493), [Part
admiral pie (tuna) 14828934](https://oldschool.runescape.wiki/w/Part_admiral_pie_(tuna)?oldid=14828934),
[Raw admiral pie 14624119](https://oldschool.runescape.wiki/w/Raw_admiral_pie?oldid=14624119),
[Uncooked dragonfruit pie
14425032](https://oldschool.runescape.wiki/w/Uncooked_dragonfruit_pie?oldid=14425032), [Part wild pie
(raw bear meat) 14737489](https://oldschool.runescape.wiki/w/Part_wild_pie_(raw_bear_meat)?oldid=14737489),
[Part wild pie (raw chompy) 14792711](https://oldschool.runescape.wiki/w/Part_wild_pie_(raw_chompy)?oldid=14792711),
[Raw wild pie 14833627](https://oldschool.runescape.wiki/w/Raw_wild_pie?oldid=14833627), [Part summer
pie (strawberry) 14737490](https://oldschool.runescape.wiki/w/Part_summer_pie_(strawberry)?oldid=14737490),
[Part summer pie (watermelon) 14465574](https://oldschool.runescape.wiki/w/Part_summer_pie_(watermelon)?oldid=14465574)
and [Raw summer pie 14871314](https://oldschool.runescape.wiki/w/Raw_summer_pie?oldid=14871314).
Together they define these 26 zero-XP actions:

| Family/stage | Exact cache-233 transform | Level | World |
| --- | --- | ---: | --- |
| Shell | pastry dough 1953 + pie dish 2313 -> pie shell 2315 | 1 | F2P |
| Redberry | shell 2315 + redberries 1951 -> uncooked redberry pie 2321 | 10 | F2P |
| Meat | shell 2315 + cooked meat 2142 **or** cooked chicken 2140 -> uncooked meat pie 2319 | 20 | F2P |
| Apple | shell 2315 + cooking apple 1955 -> uncooked apple pie 2317 | 30 | F2P |
| Mud 1 | shell 2315 + bucket of compost 6032 -> part mud pie 7164 + bucket 1925 | 29 | Members |
| Mud 2 | part 7164 + bucket of water 1929 -> part 7166 + bucket 1925 | 29 | Members |
| Mud 3 | part 7166 + clay 434 -> raw mud pie 7168 | 29 | Members |
| Garden 1 | shell 2315 + tomato 1982 -> part garden pie 7172 | 34 | Members |
| Garden 2 | part 7172 + onion 1957 -> part garden pie 7174 | 34 | Members |
| Garden 3 | part 7174 + cabbage 1965 -> raw garden pie 7176 | 34 | Members |
| Fish 1 | shell 2315 + cooked trout 333 -> part fish pie 7182 | 47 | Members |
| Fish 2 | part 7182 + cooked cod 339 -> part fish pie 7184 | 47 | Members |
| Fish 3 | part 7184 + potato 1942 -> raw fish pie 7186 | 47 | Members |
| Botanical | shell 2315 + Golovanova fruit top 19653 -> uncooked botanical pie 19656 | 52 | Members |
| Mushroom | shell 2315 + sulliuscep cap 21626 -> uncooked mushroom pie 21684 | 60 | Members |
| Admiral 1 | shell 2315 + cooked salmon 329 -> part admiral pie 7192 | 70 | Members |
| Admiral 2 | part 7192 + cooked tuna 361 -> part admiral pie 7194 | 70 | Members |
| Admiral 3 | part 7194 + potato 1942 -> raw admiral pie 7196 | 70 | Members |
| Dragonfruit | shell 2315 + dragonfruit 22929 -> uncooked dragonfruit pie 22789 | 73 | Members |
| Wild 1 | shell 2315 + raw bear meat 2136 -> part wild pie 7202 | 85 | Members |
| Wild 2 | part 7202 + raw chompy 2876 -> part wild pie 7204 | 85 | Members |
| Wild 3 | part 7204 + raw rabbit 3226 -> raw wild pie 7206 | 85 | Members |
| Summer 1 | shell 2315 + strawberry 5504 -> part summer pie 7212 | 95 | Members |
| Summer 2 | part 7212 + watermelon 5982 -> part summer pie 7214 | 95 | Members |
| Summer 3 | part 7214 + cooking apple 1955 -> raw summer pie 7216 | 95 | Members |

Requirements are boostable and each ordinary result is two ticks after selection. The admiral
salmon page uniquely records a one-tick manually initiated first result followed by two-tick
Make-X repeats, so production models that exception without changing the remaining 25 stages. The
historical [raw summer pie production revision
14792507](https://oldschool.runescape.wiki/w/Money_making_guide/Making_raw_summer_pies?oldid=14792507),
[raw wild pie revision
14272533](https://oldschool.runescape.wiki/w/Money_making_guide/Making_raw_wild_pies?oldid=14272533)
and [raw admiral pie revision
14026997](https://oldschool.runescape.wiki/w/Money_making_guide/Making_raw_admiral_pies?oldid=14026997)
independently describe Make-All at every ordered filling stage. Garden pie requires ordinary
cabbage 1965; Draynor Manor cabbage 1967 is explicitly rejected and has no handler.

Production binds all identities through typed cache references and a shared symmetric held-item
dispatcher. A one-product skill-multi choice is bounded by the scarcest required ingredient; every
result re-resolves the next exact pair, rechecks current Cooking level, membership and output
capacity, and commits consumption, returned bucket and output in one inventory transaction. The
43-scenario focused Cooking suite pins the full recipe table, cache IDs, registration count,
free-world/member behavior, boosts and rejection, zero XP, both use orders, menu payload and
quantity bound, 2/4/6 batch results, the admiral 1/3/5 exception, full-inventory bucket return,
stale-open-menu rejection and stale-batch cancellation. Historical sources do not publish exact
preparation messages, animation, sound, literal menu title/action or inventory packet/slot order;
those presentation details remain a workspace cache-233 RuneLite acceptance boundary and were not
invented.

## Standard pie baking

The final Wiki revisions at or before the cache-233.4 cutoff define all 12 ordinary pie-baking
routes: [Redberry pie 14838878](https://oldschool.runescape.wiki/w/Redberry_pie?oldid=14838878),
[Meat pie 14918374](https://oldschool.runescape.wiki/w/Meat_pie?oldid=14918374), [Mud pie
14795758](https://oldschool.runescape.wiki/w/Mud_pie?oldid=14795758), [Apple pie
14994009](https://oldschool.runescape.wiki/w/Apple_pie?oldid=14994009), [Garden pie
14796082](https://oldschool.runescape.wiki/w/Garden_pie?oldid=14796082), [Fish pie
14877509](https://oldschool.runescape.wiki/w/Fish_pie?oldid=14877509), [Botanical pie
14941519](https://oldschool.runescape.wiki/w/Botanical_pie?oldid=14941519), [Mushroom pie
14796077](https://oldschool.runescape.wiki/w/Mushroom_pie?oldid=14796077), [Admiral pie
14930600](https://oldschool.runescape.wiki/w/Admiral_pie?oldid=14930600), [Dragonfruit pie
14796085](https://oldschool.runescape.wiki/w/Dragonfruit_pie?oldid=14796085), [Wild pie
14965107](https://oldschool.runescape.wiki/w/Wild_pie?oldid=14965107) and [Summer pie
14966366](https://oldschool.runescape.wiki/w/Summer_pie?oldid=14966366). Every page specifies a
boostable range-only recipe, a one-tick manual result, four-tick Make-X production and burnt pie on
failure. Redberry, meat and apple are free-to-play; the other nine require members. Their
level/XP pairs are 10/78, 20/110, 29/128, 30/130, 34/138, 47/164, 52/180, 60/200, 70/210,
73/220, 85/240 and 95/260.

The historical success charts pin these endpoint pairs:

| Pie | Range | Lumbridge | Hosidius 5% | Hosidius elite 10% |
| --- | --- | --- | --- | --- |
| Redberry | 98/452 | 108/462 | 110/464 | 123/477 |
| Meat | 78/412 | 88/432 | 90/424 | 103/437 |
| Mud | 58/372 | same as range | 70/384 | 83/397 |
| Apple | 58/372 | same as range | 70/384 | 83/397 |
| Garden | 48/352 | same as range | 60/364 | 73/377 |
| Fish | 38/332 | same as range | 50/344 | 63/357 |
| Botanical | 20/300 | 108/462 | 32/312 | 45/325 |
| Mushroom | 17/285 | 140/450 | 29/297 | 42/310 |
| Admiral | 15/270 | same as range | 27/282 | 40/295 |
| Dragonfruit | 8/250 | same as range | 20/262 | 33/275 |
| Wild | 1/222 | same as range | 13/234 | 26/247 |
| Summer | 1/212 | same as range | 13/224 | 26/237 |

The final pre-cutoff [Cooking/Burn level revision
14976624](https://oldschool.runescape.wiki/w/Cooking/Burn_level?oldid=14976624) independently pins
ordinary/Hosidius no-burn levels: redberry 45/42, meat 54/50, mud and apple 63/59, garden 68/64,
fish 74/70, botanical 84/80, mushroom 89/84 and admiral 94/89. Dragonfruit never becomes safe on
an ordinary range, but becomes safe at 97 in Hosidius or 92 with the elite diary. The item-specific
pages additionally pin the exceptional Lumbridge thresholds at 42 redberry, 49 meat, 52 botanical
and 60 mushroom. Wild and summer retain a non-zero burn chance at level 99 on every range; all
pies become safe while a Cooking or Max cape is worn. Lunar Bake Pie is a separate guaranteed
spell workflow and is not conflated with range Cooking.

Cache 233 resolves the raw/cooked pairs as 2321/2325, 2319/2327, 7168/7170, 2317/2323,
7176/7178, 7186/7188, 19656/19662, 21684/21690, 7196/7198, 22789/22795, 7206/7208 and
7216/7218; all failures resolve to burnt pie 2329. Production binds every identity through typed
references and the shared source-aware, transactional 1/3/4 Cooking queue. The cache contract and
dedicated integration scenario cover dispatch, exact curves, boosts and low-level rejection,
membership, forced success/burn, source rejection, all no-burn classes, cape immunity, diary
selection and full-inventory replacement. Pie-shell/filling assembly is covered by the preceding
integration-tested slice. The separate Bake Pie spell, exact baking messages, audio and
matching-client packet ordering remain open.

## Remaining standard baked-goods heat stages

The final pre-cutoff [Cake revision
14918369](https://oldschool.runescape.wiki/w/Cake?oldid=14918369), dated 12 June 2025, defines
uncooked cake 1889 as a free-to-play, boostable Cooking-40 range input. Success produces cake 1891
for 180 XP and failure produces burnt cake 1903. Its success chart pins ordinary-range and
Lumbridge-fallback endpoints 38/332, Hosidius 5% endpoints 50/344 and Hosidius elite endpoints
63/357. It explicitly gives a one-tick manual result and 1/3/4 Make-X cadence. The matching
[Uncooked cake revision
14424994](https://oldschool.runescape.wiki/w/Uncooked_cake?oldid=14424994) and [Burnt cake revision
14419624](https://oldschool.runescape.wiki/w/Burnt_cake?oldid=14419624) independently pin the raw
and burnt identities and state that both success and failure return cake tin 1887.

The final pre-cutoff [Plain pizza revision
14734873](https://oldschool.runescape.wiki/w/Plain_pizza?oldid=14734873), dated 30 August 2024,
defines uncooked pizza 2287 as a free-to-play, boostable Cooking-35 range input. Success produces
plain pizza 2289 for 143 XP and failure produces burnt pizza 2305. Its success chart pins
ordinary-range and Lumbridge-fallback endpoints 48/352, Hosidius 5% endpoints 60/364 and Hosidius
elite endpoints 73/377. The matching [Uncooked pizza revision
14734874](https://oldschool.runescape.wiki/w/Uncooked_pizza?oldid=14734874) explicitly restricts the
heat stage to ranges, and [Burnt pizza revision
14419623](https://oldschool.runescape.wiki/w/Burnt_pizza?oldid=14419623) pins the failed output.
Together with the standard Cooking Make-X behavior, these fields use results at ticks 1, 4, 8 and
onward.

The final pre-cutoff [Cooking/Burn level revision
14976624](https://oldschool.runescape.wiki/w/Cooking/Burn_level?oldid=14976624) independently pins
ordinary/Hosidius no-burn levels 74/70 for cake and 68/64 for plain pizza. Both routes inherit
Cooking- and Max-cape burn prevention. Production represents the cake tin as a generic optional
returned container and atomically adds it on successful and burnt results. A cake requires one
free slot before each result because one non-stackable input becomes two non-stackable outputs;
the queue stops safely once that capacity is exhausted. The source pages prove container return
but do not pin the exact full-inventory message or production maximum shown by the chooser, so
those details remain matching-client acceptance items rather than sourced presentation claims.

The cache contract and dedicated integration scenario pin all seven involved IDs, exact curves,
F2P and boosted-level behavior, forced success/burn and success-only XP, range-only dispatch,
1/3/4 cadence, each no-burn boundary and its burnable preceding level, cape immunity, pizza's
full-inventory same-slot replacement, and cake's full/near-full inventory behavior. Cake assembly,
the incomplete-pizza ingredient graph and standard toppings are documented in the following slice;
heat-stage messages, audio and packet ordering remain separate work.

## Ordinary dough, cake and pizza preparation

The final pre-cutoff [Pizza base revision
14508359](https://oldschool.runescape.wiki/w/Pizza_base?oldid=14508359), dated 20 November 2023,
establishes that pot of flour 1933 plus bucket, bowl or jug of water 1929/1921/1937 opens a dough
choice rather than directly creating pizza base 2283. It pins boostable Cooking 35, zero XP and a
one-tick result, with empty pot 1931 and the matching empty water container 1925/1923/1935
returned. The corresponding final pre-cutoff [Bread dough revision
14867365](https://oldschool.runescape.wiki/w/Bread_dough?oldid=14867365), [Pastry dough revision
14466559](https://oldschool.runescape.wiki/w/Pastry_dough?oldid=14466559) and [Pitta dough revision
14871428](https://oldschool.runescape.wiki/w/Pitta_dough?oldid=14871428) define the other three
choices. Bread and pastry are free-world level-1, zero-XP one-tick products; pitta is a boostable
members-only level-58, zero-XP one-tick product. Cache 233 resolves the outputs as 2307, 1953,
2283 and 1863.

The final pre-cutoff [Cake tin revision
14781586](https://oldschool.runescape.wiki/w/Cake_tin?oldid=14781586) and [Uncooked cake revision
14424994](https://oldschool.runescape.wiki/w/Uncooked_cake?oldid=14424994) establish all three
cake-tin interactions: cake tin 1887, egg 1944, pot of flour 1933 and bucket of milk 1927 become
uncooked cake 1889, empty pot 1931 and empty bucket 1925 after two ticks at boostable free-world
Cooking 40 with zero XP. The dated [chat-message catalogue revision
14777106](https://oldschool.runescape.wiki/w/User:Skretzo/Chat_messages?oldid=14777106)
independently records the exact result text: `You mix the milk, flour and egg together to make a
raw cake mix.` The handler therefore accepts cake-tin use with any of the other three ingredients,
revalidates every original slot after the delay and commits the four-input/three-output change in
one transaction.

The final pre-cutoff [Incomplete pizza revision
14730580](https://oldschool.runescape.wiki/w/Incomplete_pizza?oldid=14730580) and [Uncooked pizza
revision 14734874](https://oldschool.runescape.wiki/w/Uncooked_pizza?oldid=14734874) pin the ordered
free-world assembly graph: pizza base 2283 plus tomato 1982 becomes incomplete pizza 2285, then
cheese 1985 becomes uncooked pizza 2287. Both stages require boostable Cooking 35, award zero XP
and take two ticks. Cheese cannot be added first; cache dispatch deliberately has no pizza-base
plus cheese registration.

The final pre-cutoff [Chocolate cake revision
14924218](https://oldschool.runescape.wiki/w/Chocolate_cake?oldid=14924218), [Meat pizza revision
14889101](https://oldschool.runescape.wiki/w/Meat_pizza?oldid=14889101), [Anchovy pizza revision
14951935](https://oldschool.runescape.wiki/w/Anchovy_pizza?oldid=14951935) and [Pineapple pizza
revision 14919488](https://oldschool.runescape.wiki/w/Pineapple_pizza?oldid=14919488) pin every
ordinary completed-food topping route. Cake 1891 plus chocolate bar 1973 or chocolate dust 1975
requires boostable free-world level 50, awards 30 XP and produces chocolate cake 1897. Plain pizza
2289 plus cooked meat 2142 or cooked chicken 2140 requires free-world level 45, awards 26 XP and
produces meat pizza 2293; anchovies 319 require free-world level 55, award 39 XP and produce
anchovy pizza 2297; pineapple chunks 2116 or ring 2118 require members and boostable level 65,
award 45 XP and produce pineapple pizza 2301. Every topping result takes two ticks. Only those
exact item identities are registered, so similarly named undead or raw meats cannot enter the
ordinary recipe.

Production uses one data-driven held-item preparation queue for all 24 concrete recipes: twelve
water-container/dough combinations, three cake triggers and nine direct assembly/topping pairs.
Integration coverage pins all identities, recipe cardinality, 15 unique interaction registrations,
both item-use orders, four-choice clientscript 2046 payload, boosts, membership, XP, exact 1/2-tick
boundaries, container return, full-inventory behavior and stale selected/additional-slot
interruption. The source pages do not preserve the exact production dough-menu title or choice
order, non-cake result messages, failure-message precedence, output-slot ordering, animation,
sound or packet order. The current title `What would you like to make?` and level-ordered
bread/pastry/pizza/pitta presentation reuse the cache's generic skill-multi contract but remain
explicit matching-client acceptance items; they are not promoted to verified evidence.

## Additional ordinary-fish Cooking

Seven final pre-cutoff item revisions expose complete success charts for another ordinary
fire/range family: [Mackerel 14918373](https://oldschool.runescape.wiki/w/Mackerel?oldid=14918373),
[Cod 14702667](https://oldschool.runescape.wiki/w/Cod?oldid=14702667), [Pike
14992111](https://oldschool.runescape.wiki/w/Pike?oldid=14992111), [Cooked slimy eel
14774716](https://oldschool.runescape.wiki/w/Cooked_slimy_eel?oldid=14774716), [Bass
14918367](https://oldschool.runescape.wiki/w/Bass?oldid=14918367), [Sea turtle
14747712](https://oldschool.runescape.wiki/w/Sea_turtle?oldid=14747712) and [Manta ray
14952302](https://oldschool.runescape.wiki/w/Manta_ray?oldid=14952302). Their boosted Cooking
level/XP pairs are respectively 10/60, 18/75, 20/80, 28/95, 43/130, 82/211.3 and 91/216.3.
Pike is free-to-play; all six other inputs are members-only. The pike, slimy-eel, sea-turtle and
manta recipe tabs identify a one-tick manual result; pike, sea turtle and manta distinguish the
four-tick Make-X repeat, while slimy eel explicitly records the transitional 1/3/4 cadence. The
cutoff [Cooking revision
14978581](https://oldschool.runescape.wiki/w/Cooking?oldid=14978581) supplies the same production
family and values for the remaining entries.

The historical success charts pin these endpoint pairs:

| Food | Fire | Range | Lumbridge | Hosidius 5% | Hosidius elite 10% |
| --- | --- | --- | --- | --- | --- |
| Mackerel | 98/452 | 98/452 | 108/472 | 110/464 | 123/477 |
| Cod | 83/422 | 88/432 | 93/442 | 100/444 | 113/457 |
| Pike | 78/412 | 78/412 | 88/432 | 90/424 | 103/437 |
| Slimy eel | 63/382 | 63/382 | 63/382 | 75/394 | 88/407 |
| Bass | 33/312 | 33/312 | 33/312 | 45/324 | 58/337 |
| Sea turtle | 1/202 | 1/222 | 1/222 | 13/234 | 26/247 |
| Manta ray | 1/202 | 1/222 | 1/222 | 13/234 | 26/247 |

These entries are not affected by cooking gauntlets. Cod alone in this slice has a distinct
ordinary-range curve; mackerel, cod and pike have independent Lumbridge Cook-o-matic curves.
Sea turtle and manta ray retain a burn chance at visible level 99 on every ordinary source, so
their 1/202 and 1/222 high-level curves must not be replaced with a level-based guaranteed-success
shortcut. The existing Cooking-cape policy remains the only guaranteed-success override for this
family.

Cache 233 resolves the raw/cooked/burnt triples as mackerel 353/355/357, cod 341/339/343, pike
349/351/343, slimy eel 3379/3381/3383, bass 363/365/367, sea turtle 395/397/399 and manta ray
389/391/393. All 21 identities are typed references and reuse the established source dispatch,
boosted-level checks, membership gate, quantity input, success roll, one/three/four-tick weak
queue, transactional replacement, XP and interruption behavior. The cache contract and dedicated
integration scenario pin every identity, source curve, gauntlet non-effect, successful output/XP
and forced-failure output, zero-XP burn and free/member boundary.

The manta-ray article also identifies catching and cooking one as an Ardougne elite diary task.
The repository has completion varbits but no general diary-task progress layer, so this slice does
not invent a private Cooking-owned task variable; that hook remains a named cross-system gap.
Exact success/burn messages outside the already sourced meat, chicken, herring and sardine cases,
audio, packet ordering and the production chooser's priority beyond ten distinct outputs remain
matching-client work.

## Rainbow fish, cave eel and dark crab Cooking

The final pre-cutoff [Rainbow fish revision
14875737](https://oldschool.runescape.wiki/w/Rainbow_fish?oldid=14875737), dated 1 April 2025,
defines a members-only boosted level-35 recipe awarding 110 Cooking XP and producing burnt rainbow
fish on failure. Its success chart gives fire/range 56/370, Hosidius 68/382 and Hosidius-elite
81/395 endpoints. The recipe tab pins one tick for the selected result and explicitly documents
the ordinary automatic 1/3/4 timing family.

The final pre-cutoff [Cave eel revision
14703267](https://oldschool.runescape.wiki/w/Cave_eel?oldid=14703267), dated 18 July 2024, defines a
members-only boosted level-38 recipe awarding 115 Cooking XP. Fire, ordinary range and Lumbridge
share 38/332 endpoints; Hosidius uses 50/344 and its elite improvement uses 63/357. Its recipe tab
pins a one-tick selected result and four-tick Make-X production, which uses the established
first-at-one, second-at-four, then four-tick standard queue.

The final pre-cutoff [Dark crab revision
14870790](https://oldschool.runescape.wiki/w/Dark_crab?oldid=14870790), dated 26 March 2025,
defines a members-only boosted level-90 recipe awarding 215 Cooking XP. Fire, ordinary range and
Lumbridge share 10/222 endpoints; Hosidius uses 22/234 and elite Hosidius uses 35/247. Unlike the
quick-selected standard family, its recipe tab specifies four ticks. The typed recipe therefore
delays its first result four ticks and repeats every four ticks instead of silently inheriting the
one-tick start. None of these three fish has a cooking-gauntlet chart, so gauntlets leave their
ordinary source curves unchanged; Cooking and max capes retain the shared never-burn override.

Cache 233 resolves the raw/cooked/burnt triples as rainbow fish 10138/10136/10140, cave eel
5001/5003/5002 and dark crab 11934/11936/11938. All nine identities are typed references and reuse
the cache-wide appliance dispatch, boosted-level and membership gates, native quantity input,
transactional replacement, success-only XP, cape policy and interruption checks. The exact cache
contract and a dedicated integration scenario pin all 15 source endpoint pairs, gauntlet
non-effect, success and forced-burn outputs, XP, free-world rejection and both cadence families.

The dark-crab article also requires catching and cooking one inside the Wilderness Resource Area
for the Wilderness elite diary. As with the already recorded Ardougne manta-ray task, the
repository has no shared diary-task progress layer; Cooking does not invent a private substitute.
Exact success/burn messages, audio and packet ordering for this trio remain matching-client work.

## Hunter-meat Cooking

Nine final pre-cutoff item revisions define the complete Varlamore Hunter-meat cooking family:
[Cooked wild kebbit 14873760](https://oldschool.runescape.wiki/w/Cooked_wild_kebbit?oldid=14873760),
[Cooked larupia 14873759](https://oldschool.runescape.wiki/w/Cooked_larupia?oldid=14873759),
[Cooked barb-tailed kebbit
14873758](https://oldschool.runescape.wiki/w/Cooked_barb-tailed_kebbit?oldid=14873758), [Cooked
graahk 14873756](https://oldschool.runescape.wiki/w/Cooked_graahk?oldid=14873756), [Cooked kyatt
14873751](https://oldschool.runescape.wiki/w/Cooked_kyatt?oldid=14873751), [Cooked pyre fox
14873755](https://oldschool.runescape.wiki/w/Cooked_pyre_fox?oldid=14873755), [Cooked sunlight
antelope 14961410](https://oldschool.runescape.wiki/w/Cooked_sunlight_antelope?oldid=14961410),
[Cooked dashing kebbit
14873763](https://oldschool.runescape.wiki/w/Cooked_dashing_kebbit?oldid=14873763) and [Cooked
moonlight antelope
14963028](https://oldschool.runescape.wiki/w/Cooked_moonlight_antelope?oldid=14963028). Seven are
dated 29 March 2025; sunlight antelope is dated 8 August and moonlight antelope 10 August 2025.
Every recipe is members-only, boostable and explicitly four ticks.

Their exact level/XP and success endpoints are:

| Food | Level / XP | Fire | Range / Lumbridge | Hosidius 5% | Hosidius elite 10% |
| --- | --- | --- | --- | --- | --- |
| Wild kebbit | 23 / 73 | 84/428 | 84/428 | 96/440 | 109/453 |
| Larupia | 31 / 92 | 65/390 | 65/390 | 77/402 | 90/415 |
| Barb-tailed kebbit | 32 / 106 | 56/370 | 56/370 | 68/382 | 81/395 |
| Graahk | 41 / 124 | 32/328 | 32/328 | 44/340 | 57/353 |
| Kyatt | 51 / 143 | 18/292 | 30/310 | 42/322 | 55/335 |
| Pyre fox | 59 / 154 | 10/273 | 11/276 | 23/288 | 36/301 |
| Sunlight antelope | 68 / 175 | 8/254 | 8/265 | 20/277 | 33/290 |
| Dashing kebbit | 82 / 215 | 1/194 | 1/224 | 13/236 | 26/249 |
| Moonlight antelope | 92 / 220 | 1/185 | 1/200 | 13/212 | 26/225 |

Kyatt alone has a cooking-gauntlet row: gauntlets select the same 30/310 ordinary-range endpoints
but explicitly have no effect at Hosidius. It therefore shares the existing non-stacking policy
with swordfish. All other Hunter meats leave their source curves unchanged when gauntlets are
worn. All nine recipes produce their first result after four ticks and every subsequent result
four ticks later rather than inheriting the quick one-tick ordinary-food start.

Cache 233 maps the raw/cooked identities as wild kebbit 29104/29128, larupia 29122/29146,
barb-tailed kebbit 29101/29131, graahk 29119/29149, kyatt 29125/29152, pyre fox 29110/29137,
sunlight antelope 29116/29140, dashing kebbit 29107/29134 and moonlight antelope 29113/29143.
Wild, barb-tailed and dashing kebbit share burnt kebbit 29155; larupia, graahk and kyatt share
burnt large beast 29157; both antelopes share burnt antelope 29159; pyre fox uses burnt fox meat
29161. Typed references and the standard transactional pipeline retain source dispatch, boosted
requirements, membership, quantity selection, success-only XP, cape burn prevention and clean
interruption.

The exact cache contract and dedicated integration scenario pin all identities, 45 ordinary
source endpoint pairs, gauntlet behavior, every success/burn output, XP, one-below-level and
free-world rejection, and 4/4/4 cadence. Their distinctive immediate-plus-delayed healing occurs
when eating the cooked items and is not Cooking behavior; it remains an explicit item/combat gap.
Exact cooking messages, audio and packet ordering also remain matching-client work.

## Grape-wine making and fermentation

The final pre-cutoff [Unfermented wine revision
14924367](https://oldschool.runescape.wiki/w/Unfermented_wine?oldid=14924367), dated 23 June
2025, and [Jug of wine revision
14864115](https://oldschool.runescape.wiki/w/Jug_of_wine?oldid=14864115), dated 18 March 2025,
pin ordinary wine as free-to-play, boostable Cooking 35. Grapes 1987 plus jug of water 1937 become
unfermented wine 1995 without immediate XP; fermentation succeeds as jug of wine 1993 for 200 XP
or fails as [jug of bad wine revision
14924366](https://oldschool.runescape.wiki/w/Jug_of_bad_wine?oldid=14924366) object 1991 for zero XP.
The last pre-cutoff [Zamorak's unfermented wine
revision 14695829](https://oldschool.runescape.wiki/w/Zamorak%27s_unfermented_wine?oldid=14695829)
and [Wine of Zamorak revision
14990238](https://oldschool.runescape.wiki/w/Wine_of_Zamorak?oldid=14990238) define the members
variant: Zamorak's grapes 20749 plus the same jug of water produce unfermented object 20752 at
boostable Cooking 65, then wine of Zamorak 245 for 200 XP or the shared bad-wine object on failure.
Cache 233.4 independently resolves all seven objects, player sequence 7529, make-wine graphic 47
and that graphic's embedded sequence 7530.

Both unfermented-wine revisions say fermentation occurs 20 game ticks after the most recent
unfermented result, resetting whenever another jug is made, and converts every applicable jug in
inventory and bank simultaneously. Their reciprocal wording establishes a shared 60,000 limit
across ordinary and Zamorak unfermented wine rather than separate per-item limits. The item change
histories associate that limit with the 31 January 2024 Children of the Sun release, while the
[contemporary update page revision
14543243](https://oldschool.runescape.wiki/w/Update:Children_of_the_Sun?oldid=14543243) does not
enumerate the cap; the item revisions are retained as the behavioral evidence.

The dated pages define the large-stack roll rule: divide each wine type into 15 equal batches,
roll each batch once, then roll every remainder jug individually. All rolls use the Cooking level
from before the single delayed XP award. The cutoff success endpoints are 48/352 for ordinary wine
and -125/260 for Zamorak wine, producing natural no-failure levels 68 and 98 respectively.
Production therefore allocates one roll per jug below 15, exactly 15 rolls for divisible larger
stacks, and 15 plus the remainder otherwise. It converts inventory and bank in one protected
transaction and delays the combined 200-XP-per-success award until after every result.

The first result occurs after three ticks and repeats every two ticks with player sequence 7529 and
graphic 47. Local revision-matched RuneLite history preserves the exact mixing spam text, `You
squeeze the grapes into the jug.`, after its cook counter stopped using that message in favor of
the graphic. Its `FermentTimer` displays 13.8 seconds after graphic 47, which does not by itself
disprove the Wiki's exact 20-tick timer from the later item-result boundary. Production follows the
dated Wiki boundary; manual case 30 must capture both packets on the workspace client to reconcile
the apparent offset.

No historical source located so far defines the exact shared-cap rejection, generic quantity-menu
title/action, success/failure audio, result allocation when both inventory and bank contain one
type, or the production behavior when a full bank must split one stored stack into successful and
failed output IDs. The implementation does not invent cap feedback. It conservatively assigns
successes to inventory first and retries fermentation atomically one tick later if the bank cannot
hold every possible result; both policies remain explicit observation gaps. Player timers are not
currently serialized, so logout/offline pause or continuation is also unresolved and prevents
promotion beyond `integration-tested`. Six cache-aware scenarios bring the focused Cooking suite
to 58 passing tests and cover identities, dispatch, curves, boost/member/no-fail boundaries,
3/2/20 timing and reset, delayed XP, shared cap, 15-batch grouping, simultaneous inventory/bank
conversion and capacity-safe retry.

## Nettle tea and hot-water drinks

The final pre-cutoff [Nettle-water revision
14687015](https://oldschool.runescape.wiki/w/Nettle-water?oldid=14687015), dated 25 June 2024,
defines the first production stage: nettles 4241 plus bowl of water 1921 require boostable Cooking
20, take two ticks, give zero XP and produce nettle-water 4237. Its note that the historical one-XP
award no longer applies resolves the creation-stage XP directly. The final pre-cutoff [Nettle tea
revision 14828855](https://oldschool.runescape.wiki/w/Nettle_tea?oldid=14828855), dated 29 December
2024, places the 52 XP on heating nettle-water over either a fire or range. Failure boils the water
over and leaves an empty bowl 1923. Its success endpoints are 78/412 on an ordinary fire or range,
90/424 on a Hosidius range and 103/437 after the elite diary; ordinary fire and range stop failing
at Cooking 54. The [cup-of-nettle-tea revision
14828849](https://oldschool.runescape.wiki/w/Cup_of_tea_(nettle)?oldid=14828849) displays level 20
and 52 XP for the complete multi-step recipe, but its instructions place heating before the
zero-delay pour. The stage-specific nettle-water and nettle-tea pages therefore control and prevent
a duplicate XP award when the finished tea is poured into a cup.

The same dated nettle-tea page and the [milky bowl revision
14547717](https://oldschool.runescape.wiki/w/Nettle_tea_(milky)?oldid=14547717), [ordinary cup
revision 14828849](https://oldschool.runescape.wiki/w/Cup_of_tea_(nettle)?oldid=14828849) and
[milky cup revision
14828854](https://oldschool.runescape.wiki/w/Cup_of_tea_(milky_nettle)?oldid=14828854) define all
four presentation variants. Finished bowl tea 4239 poured into empty cup 1980 becomes cup tea 4242
and leaves bowl 1923; milky bowl tea 4240 similarly becomes milky cup tea 4243 and leaves the bowl.
Bucket of milk 1927 can instead convert either finished bowl or cup variant to its milky form while
returning bucket 1925. These possession-based finishing steps have no independent skill, XP or
quest gate beyond membership. The porcelain cups 4245/4246 are Ghosts Ahoy quest handoffs and are
deliberately not folded into this ordinary Cooking slice.

The [bowl-of-hot-water revision
14714443](https://oldschool.runescape.wiki/w/Bowl_of_hot_water?oldid=14714443) and [cup-of-hot-water
revision 14982508](https://oldschool.runescape.wiki/w/Cup_of_hot_water?oldid=14982508) define the
parallel One Small Favour/Guthix-rest preparation route. Heating bowl water 1921 or cup water 4458
on a range or fire always produces hot water 4456/4460 with no level or XP and a one-tick first
result followed by four-tick automatic results. The bowl variant may then be poured into empty cup
1980 for cup hot water 4460 while preserving bowl 1923. The 7 August 2024 Poll 82 change explicitly
added both direct cup-water heating and a Make-All menu for bowl-to-cup pouring, so both are part of
cache 233 rather than later behavior.

Cache 233.4 independently resolves all thirteen input, intermediate, result and returned-container
objects. The existing Cooking source catalogue supplies every fire and `Cook` range, sequences
897/896, the source-specific Hosidius modifiers and queues 82/85. Four new cache-aware scenarios
bring the focused Cooking suite to 62 passing tests. They pin every identity and handler, level,
membership, boost, curve, no-failure boundary, success/failure output, exact XP stage, heat cadence,
quantity route, both held-item orders and full-inventory container preservation.

The dated pages leave nettle-tea heat duration blank and do not expose exact messages, sounds,
packet ordering, result-slot ordering, nettle-water Make-All presentation or the repeat cadence of
the 2024 bowl-to-cup Make-All route. Production reuses the ordinary Cooking 1/3/4 heat cadence for
nettle tea, the explicitly documented 1/4 hot-water cadence, two-tick nettle-water preparation and
zero-tick single pours with one-tick automatic bowl-to-cup repeats. `You heat the water.`, the
nettle-tea success line and boil-over line are transparent wording inferences from the dated pages,
not claimed observations. Manual case 31 must replace or confirm those provisional presentation
details before this family can advance beyond `integration-tested`.

## Oomlie-wrap preparation and Cooking

The final pre-cutoff [Wrapped oomlie revision
14953838](https://oldschool.runescape.wiki/w/Wrapped_oomlie?oldid=14953838), dated 29 July 2025,
defines the immediate members-only raw-oomlie plus palm-leaf preparation: boosted Cooking 50, 10 XP,
zero ticks and one wrapped oomlie. The final pre-cutoff [Cooked oomlie wrap revision
14787742](https://oldschool.runescape.wiki/w/Cooked_oomlie_wrap?oldid=14787742), dated 26 October
2024, then pins boosted level 50, 30 XP, a two-tick direct result and four-tick Make-X results. Its
success chart supplies fire 106/450, range 112/476, Hosidius 124/488 and elite Hosidius 137/501.
Although that recipe template lists a range as its facility, the same page publishes a fire curve,
and the final pre-cutoff [Burnt oomlie wrap revision
14971352](https://oldschool.runescape.wiki/w/Burnt_oomlie_wrap?oldid=14971352) gives an explicit
fire-based acquisition procedure. The implementation therefore accepts both classified fires and
ranges and does not silently discard the more specific fire evidence.

The burnt-wrap revision also proves an unusual batch invariant. A player must start at level 50,
then may continue Cooking while the cold effect drains the visible level below 44; only those later
results can burn. The standard queue consequently validates the level at action start but, for this
recipe alone, continues after later level drain. Membership, source availability, distance,
inventory and interruption are still revalidated before every result. This preserves the published
way to obtain burnt wrap 2345 without weakening level checks for any other Cooking recipe.

Cache 233.4 resolves raw oomlie 2337, palm leaf 2339, wrapped oomlie 2341, cooked wrap 2343, burnt
wrap 2345 and the distinct unwrapped burnt oomlie 2426. The preparation and heat stages use the
shared atomic held-item and standard-Cooking queues 85/82 and range/fire sequences 896/897. Two new
scenarios bring the focused suite to 64 tests and pin all six identities, symmetric held use,
boost/member failures, XP, curves, 2/4/4 cadence, registration and the continued drained burn path.

The final pre-cutoff [Raw oomlie revision
14787773](https://oldschool.runescape.wiki/w/Raw_oomlie?oldid=14787773) proves that attempting to
cook the unwrapped meat on a fire always produces the distinct burnt object and gives two messages.
It does not publish result timing, automatic-repeat behavior or whether a range accepts the same
misuse. That route remains researched instead of borrowing wrapped-oomlie timing or broadening its
appliance scope. Exact preparation/cooking messages, chooser presentation, sounds, result slots and
packet order also remain manual case 32. Palm-leaf gathering and the Karamja diary task for eating
the result belong to their own world-object and diary/consumption slices.

## Karambwan Cooking

The final pre-cutoff [Raw karambwan revision
14995089](https://oldschool.runescape.wiki/w/Raw_karambwan?oldid=14995089), dated 27 September
2025, makes the unlock sequence explicit: before learning the method from Tinsay after Tai Bwo
Wannai Trio, Cooking a raw karambwan always produces poison karambwan; afterward the player may
choose poison or thoroughly cooked karambwan on either a fire or range. The final pre-cutoff
[Cooked karambwan revision
14995084](https://oldschool.runescape.wiki/w/Cooked_karambwan?oldid=14995084) pins cooked object
3144, 190 XP, one tick for manual cooking and four ticks while automatic, plus ordinary 70/255,
Hosidius 82/267 and elite-Hosidius 95/280 success endpoints. Its infobox lists level 30 while its
specific item note says the action only requires visible Cooking level 1 after the unlock. The
implementation follows the more specific action note and records the conflict rather than silently
inventing a level-30 gate.

The final pre-cutoff [Poison karambwan revision
14841792](https://oldschool.runescape.wiki/w/Poison_karambwan?oldid=14841792) pins object 3146,
visible level 1, 80 XP, a one-tick first result, four-tick automatic results, the same source curves
and burnt-karambwan failure. It also preserves the 28 February 2024 change that defaults the
post-unlock chooser to thorough cooking and guards an explicit poison selection with a toggleable
warning. The final pre-cutoff [Burnt karambwan revision
14837678](https://oldschool.runescape.wiki/w/Burnt_karambwan?oldid=14837678) pins object 3148,
the level-99 ordinary no-burn boundary and the exact reheating rejection: `There is nothing to be
gained from heating a burnt octopus.` The dated [Tai Bwo Wannai Trio revision
14966763](https://oldschool.runescape.wiki/w/Tai_Bwo_Wannai_Trio?oldid=14966763) and [Tiadeche
revision 14992431](https://oldschool.runescape.wiki/w/Tiadeche?oldid=14992431) establish that quest
completion alone is insufficient: the player must subsequently speak to Tinsay for the method.

Cache 233.4 independently resolves raw/cooked/poison/burnt objects 3142/3144/3146/3148, quest DB row
141 completion state 6, Tai Bwo Wannai Trio varps 320/321, poison-warning varbit 9545 and Karamja
hard-diary task varbit 3604. The local revision-matched RuneLite source corroborates those client
identities, the default cooked product, the exact thorough-success message and calculator XP. Since
the cache exposes no distinct persistent Tinsay-reward bit, the server stores that post-quest fact
in typed server-only varbit 65534, packed into unused bit 31 of persistent generic varp 65531. This
is an explicit server state design decision, not asserted cache evidence.

Seven cache-aware scenarios bring the focused Cooking suite to 71 passing tests. They pin every
identity and registration, the two-part durable unlock, pre/post-unlock menus, warning decline and
confirmation, warning-disable preference, full-inventory replacement, both XP values, all three
source curves, success/burn outputs, cape immunity, exact first/automatic cadence, interruption,
successful-thorough diary credit and the burnt-object message. The poison-success wording comes
from historical emulation corroboration and remains secondary evidence. Exact warning preference
UI, menu padding/action mode, sound and packet order remain manual case 33; therefore this group is
`integration-tested`, not client-tested or verified.

## Sinew Cooking

The final pre-cutoff [Sinew revision
14890385](https://oldschool.runescape.wiki/w/Sinew?oldid=14890385), dated 25 April 2025, defines
all three Cooking inputs: raw beef 2132, raw bear meat 2136 and raw boar meat 25833. Members may use
any of them on a Cooking range and choose sinew 9436 at level 1 for 3 XP. Fires are excluded. The
recipe specifies a one-tick selected action and, more precisely, an immediate first Make-All result
followed by three and then four ticks. Its success chart supplies ordinary range 128/512, Hosidius
140/524 and elite Hosidius 153/537 endpoints; Lumbridge is treated as an ordinary range because the
page supplies no separate Cook-o-matic modifier for this product.

The shared dual-output chooser keeps ordinary cooked meat first and sinew second for each eligible
raw meat. Cache 233.4 independently resolves every input, burnt meat 2146, sinew 9436, range
sequence 896 and the ten-product skill-multiple clientscript 2046. The workspace RuneLite source
corroborates the sinew item, level/XP and exact successful message: `You dry a piece of meat and
extract the sinew.` It was inspected locally and was not launched.

The dated success chart proves that the action can fail but does not explicitly name its failure
object or message. The final pre-cutoff [Burnt meat revision
14973773](https://oldschool.runescape.wiki/w/Burnt_meat?oldid=14973773) establishes burnt meat as
the failure object for all three raw-meat identities, so the sinew branch uses the same atomic
raw-to-burnt conversion and ordinary meat burn message. This association is recorded for
matching-client confirmation instead of being presented as direct sinew-page evidence.

Two cache-aware scenarios bring the focused Cooking suite to 73 passing tests. They pin all five
objects, queue 89, every source curve, product order, immediate/3/4 cadence, 3 XP, all three inputs,
range and membership gates, burn output, cape immunity, full-inventory replacement and movement
interruption. Exact one-item chooser presentation, failure wording, range-animation packet timing,
sound and packet order remain manual case 34, so this slice is `integration-tested` rather than
client-tested or verified.

## Ordinary iron-spit Cooking

The final pre-cutoff [roast bird meat revision
14978928](https://oldschool.runescape.wiki/w/Roast_bird_meat?oldid=14978928), dated 3 September 2025,
[roast beast meat revision
14873307](https://oldschool.runescape.wiki/w/Roast_beast_meat?oldid=14873307), dated 28 March 2025,
and [roast rabbit revision
14787731](https://oldschool.runescape.wiki/w/Roast_rabbit?oldid=14787731), dated 26 October 2024,
establish
the three fire-roasted products. Their respective contracts are Cooking 11, 21 and 16; 62.5, 82.5
and 72.5 XP; three result ticks; failure objects burnt bird meat 9982, burnt beast meat 9990 and
burnt rabbit 7222; returned iron spit 7225; level-99 no-burn boundaries; and fire success endpoints
155/255, 180/255 and 160/255.

The corresponding pre-cutoff [skewered bird meat revision
14745632](https://oldschool.runescape.wiki/w/Skewered_bird_meat?oldid=14745632), dated 17 September
2024, [skewered beast revision
14977506](https://oldschool.runescape.wiki/w/Skewered_beast?oldid=14977506), dated 31 August 2025,
and [skewered rabbit revision
14760085](https://oldschool.runescape.wiki/w/Skewered_rabbit?oldid=14760085), dated 2 October 2024,
define immediate, zero-XP preparation from raw meat plus one iron spit at the same boostable Cooking
levels and boostable Firemaking 20. The [iron spit revision
14995320](https://oldschool.runescape.wiki/w/Iron_spit?oldid=14995320), dated 27 September 2025,
corroborates the raw bird,
beast and rabbit families and explicitly excludes raw jubbly from this portable method.

There is a source distinction rather than an assumed shared rule: the roast-bird page explicitly
lists Firemaking 20 for the Cooking step, while the individual roast-beast and roast-rabbit recipes
do not. The implementation therefore rechecks visible Firemaking only for bird roasting after all
three preparation steps have required it. Cache 233.4 resolves the complete object graph
7222-7225 and 9978-9992, sequence 1016, both fire identities 26185/43475 and dedicated queue 90.
The local revision-matched RuneLite Cooking plugin was inspected without being launched; it only
establishes that successful roast feedback begins `You roast a`, not the exact three complete
messages.

Four cache-aware scenarios bring the focused Cooking suite to 77 passing tests. They pin all cache
identities, registrations, levels, XP, curves, immediate symmetric preparation, three-tick success
and burn paths, returned spit, membership, boosted requirements, the recipe-specific Firemaking
recheck, level-99 and Cooking-cape immunity, full-inventory atomicity, selected-slot safety,
movement and shared Cooking interruption. Exact success/burn/level wording, sounds, result-slot and
packet ordering require manual case 35, so the tested provisional messages do not promote the slice
beyond `integration-tested`.

## Lunar Bake Pie

The final pre-cutoff [Bake Pie revision
14834971](https://oldschool.runescape.wiki/w/Bake_Pie?oldid=14834971), dated 11 January 2025,
defines a members-only level-65 Lunar utility spell unlocked by completing Lunar Diplomacy. Each
pie consumes one astral, five fire and four water runes, awards 60 Magic XP and the pie's full
ordinary Cooking XP, uses the player's current visible Cooking level so boosts apply, never burns,
and automatically processes held uncooked pies one by one. Its three-tick speed agrees with the
historical training table's 27 pies in 81 ticks. The same revision explicitly says a summer pie
baked this way satisfies the Varrock elite diary task.

The dated recipe table contains exactly redberry, meat, mud, apple, garden, fish, botanical,
mushroom, admiral, dragonfruit, wild and summer pies at Cooking level/XP pairs 10/78, 20/110,
29/128, 30/130, 34/138, 47/164, 52/180, 60/200, 70/210, 73/220, 85/240 and 95/260. Cache 233.4
resolves their raw/cooked pairs to 2321/2325, 2319/2327, 7168/7170, 2317/2323, 7176/7178,
7186/7188, 19656/19662, 21684/21690, 7196/7198, 22789/22795, 7206/7208 and 7216/7218. Spell
object 9107 and spellbook component child 105 pin the Magic-65, Lunar, members and rune contract;
quest DB row 88 pins `lunar_quest_main` varbit 2448 completion to state 190.

Cache 233 includes player sequences 4413 (`lunar_human_magic_summon2`) and 4422
(`quest_lunar_bakepie`) plus spot animations 727 and 746. The local revision-matched RuneLite
checkout, inspected without launch, identifies 4413 as its shared Bake Pie player animation and 746
as `GraphicID.BAKE_PIE`; its idle notifier also observes graphic 746. Production therefore uses
only those evidenced identities. No cache-backed sound mapping was found, so audio is deliberately
left to matching-client capture rather than guessed.

Six cache-aware scenarios pin the entire table, cache component and presentation IDs, initial and
per-result requirements, inventory-order results at ticks 3/6/9, exact rune and XP consumption,
boost draining, guaranteed success, full-inventory same-slot replacement, stale inventory safety,
rune exhaustion and ordinary action interruption. The Wiki does not resolve mixed-level ordering
or literal failure messages; production currently stops on the first inventory-order pie whose
Cooking level is unmet, with provisional text recorded for manual case 36. Diary credit and exact
sound/packet ordering remain open, so the core is `integration-tested` rather than verified.

## Lunar Humidify

The final pre-cutoff [Humidify revision
14977371](https://oldschool.runescape.wiki/w/Humidify?oldid=14977371), dated 31 August 2025,
defines a members-only level-68 Lunar utility spell unlocked by Dream Mentor (and therefore Lunar
Diplomacy). One cast consumes one astral, three water and one fire rune, awards 65 Magic XP once
regardless of how many targets are filled, transforms all eligible inventory targets together and
has a three-tick casting speed. The same dated page includes buckets, fishbowls, jugs, vials,
watering cans, Gricoller's can, all unwatered seedlings, waterskins, bowls and clay. Its history
pins clay support to 15 August 2013, seedlings to 30 October 2014, Gricoller's can to 9 September
2021 and empty cups to 14 August 2024. The 21 January 2026 Sailing-seed expansion postdates cache
233 and is deliberately excluded.

Cache 233.4 resolves spell object 11145, component `magic_spellbook:humidify` child 110, Dream
Mentor varbit 3618 and quest DB row 33 completion column 19 value 28. Its presentation assets are
player sequence 6294 and spot animations 1061 (rain), 1669 (projectile animation) and 2294
(no-rain). The local revision-matched RuneLite checkout, inspected without launch, independently
names animation 6294 and all three graphics. No named cache synth or RuneLite sound constant was
found, so production uses the ordinary rain graphic 1061 and does not invent audio or an
environment-selection rule.

The cache supplies 19 ordinary item transformations: vial; four depleted waterskins; bowl,
bucket, jug and post-2024 cup; eight depleted watering cans; fishbowl; and clay. It supplies 20
pre-Sailing unwatered/watered seedling pairs: oak, willow, maple, yew, magic, spirit, apple,
banana, orange, curry, pineapple, papaya, palm, calquat, teak, mahogany, celastrus, redwood,
dragonfruit and crystal. The Chambers strategy evidence says its empty gourd vial may be filled by
Humidify, establishing the fortieth item pair, 20800 to 20801. The Wiki also establishes Olm
flame-wall extinguishing and the Desert diary cast condition, but those are separate world-system
integrations.

Seven cache-aware scenarios pin the spell/quest/presentation contract and every pair; verify every
same-slot result; exercise mixed categories, full inventories, one 65-XP award, one rune set,
members/quest/book/Magic failures, missing runes, no eligible targets, staff substitution,
Fountain behavior and the three-tick recast boundary. The no-target line remains provisional.
Gricoller's single cache identity cannot encode its 1,000 charges without an evidenced external
state, and Olm/Soul Wars/diary hooks are not yet available, so those behaviors and exact client
presentation remain researched in manual case 37.

## Lunar Hunter Kit

The final pre-cutoff [Hunter Kit spell revision
14838556](https://oldschool.runescape.wiki/w/Hunter_Kit_(spell)?oldid=14838556), dated 17 January
2025, defines a members-only level-71 Lunar utility spell unlocked by Dream Mentor. It consumes two
astral and two earth runes, awards 70 Magic XP, has a three-tick casting speed and creates a Hunter
kit. The cast is rejected when the player already has a kit or simultaneously owns all eight
contents, with the exact line `Why do you want to create a kit? You already have all the Hunter
equipment it contains.` The page history records that an impling jar joined the kit on 12 March
2015.

The final cache-cutoff [Hunter kit item revision
14987664](https://oldschool.runescape.wiki/w/Hunter_kit?oldid=14987664), dated 17 September 2025,
defines item 11159 as a members-only, untradeable, non-stackable, bankable item with `Open` and
`Destroy` operations. Opening yields one noose wand, butterfly net, bird snare, rabbit snare,
teasing stick, unlit torch, impling jar and box trap. It also supplies the exact Entrana rejection
`Some forms of Hunter gear are not appreciated by the monks.` and records Poll 85's one-time grant
from the Feldip Hills and Rellekka hunting experts. The generic destroy pipeline already reads the
cache-enriched note `You will have to create another using the Lunar Spellbook.`

Cache 233.4 resolves spell object 11146, result item 11159, component
`magic_spellbook:hunter_kit` child 114, Dream Mentor varbit 3618 with quest DB row 33 completion
value 28, player sequence 6303, spot animation 1074 and contents 10150, 10010, 10006, 10031,
10029, 596, 11260 and 10008. Cache sound metadata names synth 3615 `lunar_hunter_kit` and synth
3616 `lunar_hunter_kit_inv`. The local revision-matched RuneLite checkout, inspected without
launch, independently pins item 11159, spell object 11146, component 218:114, sequence 6303,
graphic 1074 and the level/XP contract.

Eight cache-aware scenarios verify the spell and item contracts, exact runes/XP/presentation,
members/quest/book/Magic gates, duplicate-kit and complete/incomplete-content rules, missing-rune
and capacity atomicity, staff substitution, Fountain policy, the exact three-tick boundary, all
eight unpacked contents, opening sound, stale slots and the six-versus-seven-free-slot boundary.
The standard repository capacity message remains provisional. Entrana is not implemented until an
exact production polygon is sourced, and the Poll 85 expert grant remains separate NPC/dialogue
work; both are isolated with matching-client observations in manual case 38.

## Lunar Magic Imbue

The final pre-cutoff [Magic Imbue revision
14915930](https://oldschool.runescape.wiki/w/Magic_Imbue?oldid=14915930), dated 6 June 2025,
defines a members-only level-82 Lunar utility spell unlocked by Lunar Diplomacy. It consumes two
astral, seven fire and seven water runes, awards 86 Magic XP, may be cast anywhere, has no cast
delay and cannot be recast for its 12.6-second or 21-game-tick duration. While active, combination
runes can be crafted without the opposing talisman, including multiple altar batches, but pure
essence and the opposite rune are still required. The spell does not improve the ordinary 50%
binding chance; a Binding necklace remains the 100% success mechanism. The same revision names a
steam battlestaff, mystic steam staff, twinflame staff and the water-staff/Tome-of-fire pairing as
rune substitutions.

The final pre-cutoff [Combination rune revision
14921835](https://oldschool.runescape.wiki/w/Combination_rune?oldid=14921835), dated before the
cache cutoff, independently confirms equal pure essence and opposite elemental runes, one opposing
talisman per altar interaction, independent 50% success per essence and consumption of both
successful and failed attempts. Magic Imbue removes only the talisman requirement. The six rune
families and both directions are level/XP asymmetric: mist 6 at 8.0 Air or 8.5 Water XP, dust 10 at
8.3 Air or 9.0 Earth XP, mud 13 at 9.3 Water or 9.5 Earth XP, smoke 15 at 8.5 Air or 9.5 Fire XP,
steam 19 at 9.3 Water or 10.0 Fire XP, and lava 23 at 10.0 Earth or 10.5 Fire XP.

The cache-era official [Yama, the Master of Pacts: Out Today
update](https://oldschool.runescape.wiki/w/Update%3AYama%2C_the_Master_of_Pacts%3A_Out_Today)
defines the exceptional Aether route. At level 90 Runecraft, the player must cast Magic Imbue,
wear a Binding necklace and use soul runes at the Cosmic Altar while carrying pure or Daeyalt
essence and Aether catalysts. Each created rune awards 20 XP and consumes exactly one catalyst.
Raiments of the Eye and Scarred Extracts can increase output, Guardians of the Rift permits the
route, and catalyst consumption remains one per Aether rune created. Those three multiplier/
activity integrations are not part of the core implemented here and remain explicitly open.

The same pre-cutoff combination-rune evidence defines a worn Binding necklace as 100% success for
16 altar interactions, not 16 created runes. Its charge count is player-global: replacing or
trading a partly used necklace does not reset it, while destroying a necklace resets the next one
to 16. The local revision-matched RuneLite history, inspected without launch, independently pins
cache varp 487 (`necklace_of_binding`) and the exact check, bind/partial-bind and disintegration
chat patterns used by its item-charge tracker.

Cache 233.4 resolves elemental altars 34760/34762/34763/34764, Cosmic Altar 34766, pure essence
7936, Daeyalt essence 24704, Binding necklace 5521, Aether catalyst 30771, Aether rune 30843,
Runecraft sequence 791, graphic 186 and necklace varp 487. The necklace inventory operations are
exactly blank, `Wear`, `Check`, blank and `Destroy`; pure and Daeyalt essence are non-stackable,
while Aether catalysts are stackable. The cache also resolves spell object 9124
(`82_magic_imbue`), Lunar spellbook component child 128,
player sequence 8970 (`human_cast_selfimbue`), alternate sequence 8982 (`self_imbue`) and active
varbit 5438. The varbit occupies bits 2 through 3 of permanent base varp 439. The dated Wiki sound
file metadata resolves `lunar_embue_runes` to synth 2888; sequence 8970 has no embedded sound. The
local revision-matched RuneLite checkout, inspected without launch, independently pins object
9124, component 218:128, sequence 8970, varbit 5438 and its representation as remaining time in
ten-game-tick units.

Seven Magic-Imbue scenarios verify the cache and Lunar Diplomacy contracts, exact rune/XP and
presentation output, every members/quest/book/Magic/resource failure, atomic rune removal, all
three cache-era steam-staff substitutions, Fountain policy, the exact 21-tick state/recast
boundary, the client display's 2-to-1-to-0 transition and login/logout cleanup of the persistent
base varp. Eight additional Runecraft scenarios verify all 12 elemental directions and their
level/XP matrix, per-essence deterministic success/failure, consumption on failure, talisman
consumption and Magic-Imbue bypass, members/level/resource atomicity, the strict Aether route,
mixed pure/Daeyalt essence, exact cache operations and the shared 16-interaction necklace
lifecycle including disintegration, Check and destroy reset. Exact unsourced failure wording,
Raiments/extract multipliers, Guardians of the Rift, sequence 8982's role, graphic height, packet
order and matching-client observation remain isolated in manual cases 39 and 129.

## Lunar Stat Spy

The final pre-cutoff [Stat Spy revision
14277394](https://oldschool.runescape.wiki/w/Stat_Spy?oldid=14277394), dated 26 April 2022 and still
current at the cache-233.4 cutoff, defines a members-only level-75 Lunar spell unlocked by Dream
Mentor and Lunar Diplomacy. It consumes two astral, two cosmic and five body runes, awards 76 Magic
XP, targets another player, reveals current Hitpoints plus every skill and boosted level, opens the
Stat Spy interface and notifies the target with the caster's name. The page labels the spell
`Combat`, but describes no damage, accuracy roll or combat-state effect. It does not publish the
literal notification, Accept Aid/privacy behavior, target graphic, cast/recast timing or packet
order. The dated [Stat Spy animation](https://oldschool.runescape.wiki/w/File:Stat_Spy.gif) is a
5.1-second, 72-frame Jagex-sourced recording, but its file metadata cannot identify protocol IDs.

Cache 233.4 resolves spell object 11147 (`75_stat_spy`), Lunar component 218:118, level 75, 76 XP,
the exact three rune requirements, `Combat` type and nominal max hit 1. It resolves side interface
523 (`dream_player_stats`) with 23 production skills in this exact row order: Attack, Hitpoints,
Mining, Strength, Agility, Smithing, Defence, Herblore, Fishing, Ranged, Thieving, Cooking, Prayer,
Crafting, Firemaking, Magic, Fletching, Woodcutting, Runecrafting, Slayer, Farming, Construction and
Hunter. Each row has a current field at children 1, 5, ... 89 and a base field at children 2, 6,
... 90; child 94 defaults to `Player ~ Name`, and child 95 is the close control. The cache also
pins shared player sequence 6293 (seven ticks), caster spot animation 1060 and synth 3620. The
local revision-matched RuneLite checkout, inspected without launch, independently names object
11147, component 218:118, interface 523, sequence 6293, caster graphic 1060 and the level/XP
contract.

Target presentation and recast cadence require an explicit evidence qualification. A concrete
2008 protocol-era [Stat Spy implementation](https://rune-server.org/threads/508-lunar-stat-spy-508.99682/)
uses sequence 6293, caster graphic 1060, target graphic 736 and interface 523, while a later
[Lunar asset list](https://rune-server.org/threads/lunar-magics-emotes-gfxs.594796/) assigns target
graphic 738 to Stat Spy, Cure Other and Heal Other. Direct frame-by-frame comparison of the dated
Jagex-sourced Stat Spy, Cure Other and Heal Other Wiki GIFs shows that all three recipients use the
same rotating-crystal effect. Cache 233 identifies 738 as
`quest_lunar_spellbook_energy_heal_spot_anim`, and the revision-matched RuneLite checkout names the
same spot animation. This converging visual, cache and asset-list evidence resolves 738 and corrects
the earlier provisional 736 implementation. Exact graphic height/delay and the ordinary five-tick
combat-spell cadence remain matching-client capture boundaries in manual case 40.

Seven cache-aware scenarios verify the cache and quest contract, exact runes/XP, native side-panel
population of all 23 current/base pairs, boosted and drained snapshots, target name/notification,
members/quest/book/Magic/resource atomicity, self-target rejection, Accept Aid independence,
Fountain policy, five-tick lock, standard eight-tile player targeting and the close control. Stat
Spy remains cache-typed as `Combat` for metadata fidelity, but the offensive PvP/PvN registries now
exclude it so its dedicated handler cannot damage, roll accuracy, skull or start combat. Literal
notification and caster feedback, target audiovisual height/timing, cross-action cooldown,
privacy/activity edges and packet ordering remain researched in manual case 40; the core is
`integration-tested`, not client-tested or verified.

## Lunar Spin Flax

The final pre-cutoff [Spin Flax revision
14960688](https://oldschool.runescape.wiki/w/Spin_Flax?oldid=14960688), dated 7 August 2025 and
current at the cache-233.4 cutoff, defines a members-only level-76 Lunar utility spell unlocked by
Lunar Diplomacy. One cast consumes one astral, two nature and five air runes, awards 75 Magic XP,
and converts up to five flax into bow strings for 15 Crafting XP per converted item. The page pins
a five-tick cast rate and the exact empty-input feedback `You don't have any flax to spin.` Its
2022 change history says a second cast during the first is queued in the same manner as High Level
Alchemy and Tan Leather; it does not define third-click replacement, cross-action cancellation,
graphic timing/height or packet ordering.

Cache 233.4 resolves spell object 20589 (`76_spin_flax`), Lunar spellbook component 218:146,
utility classification, level 76, 75 XP and the exact rune requirements. It also resolves flax
1779, bow string 1777, bow-string spool 31052, shared Lunar player sequence 4413, Spin Flax graphic
728 and synth 3615. The local revision-matched RuneLite checkout was inspected without launch and
independently names the spell object, interface component and graphic. The cutoff
[bow-string-spool revision
14983366](https://oldschool.runescape.wiki/w/Bow_string_spool?oldid=14983366) establishes that bow
strings made by spinning flax are automatically stored while the physical spool is carried and
that its charges remain account-backed when the spool is lost.

Production therefore uses the established utility-spell gates and rune substitutions, performs
one atomic inventory-order transform of up to five flax, awards one 75-XP Magic grant plus 15
Crafting XP per result, and locks for five ticks. One additional cast request may be queued during
that lock; further requests do not create more work. Produced strings fill available spool
capacity first and remain loose only for overflow. Eight cache-aware scenarios cover cache
identity, all rune/XP/presentation values, one-to-five conversion, full-inventory replacement,
members/quest/book/Magic/resource failures, staff and Fountain behavior, the exact fifth-tick
single queue, overlay dispatch, and empty/partial/full spool capacity. Matching-client queue
replacement/cancellation, cross-action locking, graphic height/delay and packet ordering remain
isolated in manual case 41; the core is `integration-tested`, not client-tested or verified.

## Lunar Tan Leather

The final pre-cutoff [Tan Leather revision
14982771](https://oldschool.runescape.wiki/w/Tan_Leather?oldid=14982771), dated 9 September 2025,
defines a members-only level-78 Lunar utility spell unlocked by Lunar Diplomacy and the hard
Fremennik Diary. One cast consumes one astral, two nature and five fire runes, awards 81 Magic XP,
and tans up to five hides in three ticks. It explicitly says batches are manually recast rather
than continuing through the inventory and that cowhides can be toggled between soft and hard
leather through the spell's right-click option. Its profit table enumerates soft leather, hard
leather, two snakeskin inputs and green, blue, red and black dragon leather: eight outcomes over
seven input object types. The contemporaneous [Leather revision
14893148](https://oldschool.runescape.wiki/w/Leather?oldid=14893148) independently corroborates
the five-cowhide batch, three-tick rate, rune cost and diary gate.

Cache 233.4 resolves spell object 1741 (`leather`), Lunar spellbook component 218:122 with the
literal operations `Cast` and `Setup`, level 78, 81 XP, utility classification and the exact rune
requirements. The typed tanning registry resolves cowhide 1739 to leather 1741 or hard leather
1743; snake hides 6287 and 7801 to snakeskin 6289; and green, blue, red and black hides 1753,
1751, 1749 and 1747 to leathers 1745, 2505, 2507 and 2509. The cache also resolves player sequence
3014, graphic 322, synth 2879 (historically named `lunar_bake_pie`), hard-diary varbit 4493 and
queue 94. The local revision-matched RuneLite checkout was inspected without launch and
independently names component 218:122, sequence 3014 and graphic 322. The [Tan Leather sound
revision 14890780](https://oldschool.runescape.wiki/w/File:Tan_Leather.ogg?oldid=14890780)
identifies synth 2879 and its historical cache name.

Production reuses the established typed tanning recipes and processes at most the first five
eligible inventory slots in one atomic same-slot transaction, including mixed supported hides.
Cowhide output follows a per-player soft/hard Setup selection; the default is soft and logout
clears the selection. The cast charges one rune set and one 81-XP Magic grant for the entire batch,
awards no Crafting XP, emits the cache presentation and locks for three ticks. One follow-up cast
may queue during that lock. Eight cache-aware scenarios cover cache identities and all eight
outcomes, mixed ordering and the five-hide cap, Setup and logout reset, empty/partial/full-inventory
transactions, every access/resource gate, the exact third-tick queued recast, staff and Fountain
behavior and overlay dispatch. Mixed-hide inventory ordering is an inference from the batch spell
contract and established inventory processing, while the historical sources do not publish the
exact Setup menu title/choices/default/persistence, literal no-hide and diary feedback,
third-click replacement/cancellation, graphic height/delay or packet order. Those points remain
isolated in manual case 43, so the core is `integration-tested`, not client-tested or verified.

## Lunar String Jewellery

The final pre-cutoff [String Jewellery revision
14775009](https://oldschool.runescape.wiki/w/String_Jewellery?oldid=14775009), dated 14 October
2024 and still current at the cache-233.4 cutoff, defines a members-only level-80 Lunar utility
spell unlocked by Lunar Diplomacy. Each eligible item consumes two astral, five water and ten
earth runes, awards 83 Magic XP and 4 Crafting XP, and completes after three ticks. One click
continues through eligible inventory items one at a time in inventory order; 27 items take 48.6
seconds and award 2,241 Magic plus 108 Crafting XP, independently confirming per-item rune and XP
costs and a three-tick first result. Walking or interacting with a player or NPC interrupts the
automatic process.

The cutoff article explicitly adds the salve shard, unstrung holy symbol and unstrung unholy
emblem. Its contemporaneous [profit-table revision
14257547](https://oldschool.runescape.wiki/w/Template:Table/Magic/String_Jewellery?oldid=14257547),
together with the cache-era Magic and Crafting experience tables, enumerates gold, opal, jade,
topaz, sapphire, emerald, ruby, diamond, dragonstone, onyx and zenyte amulets. This produces exactly
14 transforms. Ordinary ball-of-wool stringing also supports an unstrung M'speak amulet, but that
quest item is absent from the String Jewellery spell table and is therefore deliberately excluded.
The spell's fixed 4 Crafting XP applies to the salve shard even though ordinary manual stringing of
that shard has no Crafting XP.

Cache 233.4 resolves spell object 9122 (`80_string_jewelry`), Lunar component 218:126, level 80,
83 Magic XP, utility classification and the exact rune requirements. It resolves the 28 input and
output objects, sequence 4412 (`lunar_human_magic_summon1`), graphic 730
(`quest_lunar_spellbook_string_jewelery_spot_anim`) and synth 2905
(`lunar_string_amulet2`). The local revision-matched RuneLite checkout was inspected without launch
and independently corroborates object 9122, component child 126, sequence 4412 and graphic 730. The
[String Jewellery sound revision
14890777](https://oldschool.runescape.wiki/w/File:String_Jewellery.ogg?oldid=14890777) identifies
synth 2905 and its cache name.

Production reuses the ordinary Crafting amulet-stringing registry, filters only the unsupported
M'speak recipe, and processes the first eligible inventory slot through an interruptible weak
queue. Every result performs an atomic same-slot transform, charges one rune set, awards 83 Magic
and fixed 4 Crafting XP, emits the cache presentation, and schedules the next result three ticks
later. Eight cache-aware scenarios cover the exact 14-recipe set and M'speak exclusion, cache
identity, inventory ordering and timing, rune exhaustion, all access/resource failures, salve XP,
full inventory, stale state, interruption, staff and Fountain behavior, and overlay dispatch. The
final historical article does not publish the literal no-eligible-item message, graphic
height/delay, repeated-sound overlap, queue replacement or exact audiovisual/inventory/rune/XP
packet order. Those points remain isolated in manual case 42, so the core is
`integration-tested`, not client-tested or verified.

## Lunar Plank Make

The final pre-cutoff [Plank Make revision
14937338](https://oldschool.runescape.wiki/w/Plank_Make?oldid=14937338), dated 13 July 2025 and
still current at the cache-233.4 cutoff, defines a members-only level-86 Lunar utility spell
unlocked by Dream Mentor. Each result consumes two astral, one nature and fifteen earth runes,
awards 90 Magic XP and converts one log using a reduced sawmill fee: normal logs to planks for 70
coins, oak logs to oak planks for 175, teak logs to teak planks for 350 and mahogany logs to
mahogany planks for 1,050. Manual casting is three ticks. One cast also begins automatic
processing: the first plank is produced on the initial cast tick, the second five ticks later and
each subsequent plank six ticks after the previous one. The page's 2024 history explicitly changed
selection from the targeted log to the first log in inventory and output from the target slot to
the first available inventory slot. Its 2025 history added four separately toggleable plank-type
confirmation warnings, but does not publish their literal dialogs, button order, defaults or
varbit polarity.

The contemporaneous [Sawmill voucher revision
14992202](https://oldschool.runescape.wiki/w/Sawmill_voucher?oldid=14992202), dated 25 September
2025, establishes stackable voucher object 28628 and one extra matching plank per consumed voucher.
It explicitly retains the voucher when neither ordinary inventory space nor plank-sack capacity
can hold the extra output, and lists all four Plank Make recipes. Its recipe rows summarize
automatic casting as five ticks, which conflicts with the spell article's more precise
initial/+5/+6 schedule. Production follows the spell-specific timing account while manual case 44
keeps the conflict open. The voucher table also labels the normal-log recipe with Lunar Diplomacy,
but the main spell article consistently requires Dream Mentor; production therefore requires the
cache-backed Lunar Diplomacy completion state 190 and Dream Mentor completion state 28 rather than
weakening the spell gate because of that isolated table label.

Cache 233.4 resolves spell object 11149 (`86_plank_make`), Lunar component 218:133, level 86,
90 XP, utility classification and the exact rune requirements. The component's direct operation
array is empty, consistent with spell-on-inventory selection being driven by the spellbook client
scripts rather than a literal component operation. The cache resolves normal/oak/teak/mahogany
logs 1511/1521/6333/6332, outputs 960/8778/8780/8782, sawmill voucher 28628, player sequence 6298
(`dream_player_make_plank_spell`), graphic 1063 (`dream_plank_make_spell_spotanim`), and four
confirmation varbits 624, 2319, 2320 and 3939. The [Plank Make sound revision
14890672](https://oldschool.runescape.wiki/w/File:Plank_Make.ogg?oldid=14890672) identifies synth
3617 and cache name `lunar_make_plank`. The local revision-matched RuneLite checkout was inspected
without launch and independently corroborates spell object 11149, component 218:133, sequence
6298 and graphic 1063.

Production validates the selected target but converts the first supported inventory log
immediately, then schedules automatic continuation after five ticks and six ticks apart. A
manual recast may queue once at the shared three-tick utility boundary and replaces the automatic
continuation. Every result transactionally charges the recipe fee and one rune set, grants 90
Magic XP outside the Fountain of Rune, emits the cache presentation and adds the plank to the
first free slot. A carried voucher is consumed for a second output only when an ordinary inventory
slot is already free; otherwise one plank is produced and the voucher remains. Eight cache-aware
scenarios cover all four recipes and fees, first-free-slot placement, exact immediate/+5/+6
timing, mixed inventory continuation, voucher capacity, every access/resource gate, manual queue
replacement, staff/Fountain behavior and overlay dispatch.

The repository has no plank-sack content/state service, so the voucher's sack-capacity branch is
not implemented. The four warning varbits are typed but their dialog behavior is not guessed.
Exact invalid-target and insufficient-coin messages are provisionally based on secondary emulator
evidence rather than an authoritative cutoff transcript. The historical sources also do not
resolve the Varrock elite 20-mahogany-log task, graphic height/delay, cross-action interruption or
exact audiovisual/inventory/coin/rune/XP packet
order. Those boundaries remain isolated in manual case 44, so the core is `integration-tested`,
not client-tested or verified.

## Lunar Recharge Dragonstone

The final pre-cutoff [Recharge Dragonstone revision
14715163](https://oldschool.runescape.wiki/w/Recharge_Dragonstone?oldid=14715163), dated 8 August
2024 and still current at the cache-233.4 cutoff, defines a members-only level-89 Lunar utility
spell requiring the hard Fremennik Diary. One cast consumes four water, one astral and one soul
rune, awards 97.5 Magic XP and recharges every eligible inventory item to four charges. The
eligible families are amulets of glory, combat bracelets and skills necklaces; the latter two
require completed Legends' Quest. The page explicitly excludes rings of wealth.

The contemporaneous [Amulet of glory revision
14971592](https://oldschool.runescape.wiki/w/Amulet_of_glory?oldid=14971592) independently says the
spell restores all amulets in the inventory to four charges and identifies ordinary charge states
0 through 6. It also establishes that the trimmed glory is functionally identical apart from
appearance and certain Treasure Trail steps, supporting its inclusion as a documented inference.
The [Combat bracelet revision
14955379](https://oldschool.runescape.wiki/w/Combat_bracelet?oldid=14955379) and [Skills necklace
revision 14939702](https://oldschool.runescape.wiki/w/Skills_necklace?oldid=14939702) establish the
same zero-through-six charge families. The public Quest Helper [pre-cutoff Legends' Quest source
at commit f4e8ed2](https://github.com/Zoinkwiz/quest-helper/blob/f4e8ed2e4b0f56a86673e11604f444e08367fbd8/src/main/java/com/questhelper/helpers/quests/legendsquest/LegendsQuest.java)
maps active quest stages through 70 and lists charging skills necklaces and combat bracelets as a
completion unlock. Cache quest database row 85, column 19 supplies the authoritative terminal
state `[75]`, so production gates those two families at varp 139 state 75.

Cache 233.4 resolves spell object 2529 (`caveorb4dot`), Lunar component 218:136 with direct
operation `Cast`, level 89, 97.5 XP, utility classification and the exact rune requirements. It
resolves the sixteen zero-through-three transforms as ordinary glories 1704/1706/1708/1710 to
1712; trimmed glories 10362/10360/10358/10356 to 10354; skills necklaces
11113/11111/11109/11107 to 11105; and combat bracelets 11126/11124/11122/11120 to 11118. The
cache also resolves shared Lunar player sequence 4409 (`quest_lunar_spell_cast_spell_on_group`),
graphic 282 (`lunar_enchant_dragonstone`), synth 139 (`enchant_dragon_amulet`), hard-diary varbit
4493 and Legends' Quest varp 139. The local revision-matched RuneLite checkout was inspected
without launch and independently corroborates component child 136, graphic 282 and all cached
jewellery identities.

Production performs one preflighted, same-slot inventory transaction over every eligible unnoted
item, charges one rune set and one 97.5-XP Magic grant for the entire batch, emits the cache
presentation and applies the shared three-tick utility lock. Glories do not require Legends'
Quest; skills necklaces and combat bracelets do. Equipped items, rings of wealth, eternal glories,
already-four-charge items and five/six-charge items are left unchanged. Eight cache-aware
scenarios cover every transform and cache identity, family-specific quest gating, exclusions,
full-inventory atomicity, all access/resource failures, staff and Fountain behavior, the
three-tick recast boundary and overlay dispatch.

The dated spell wording says items are restored to four charges but does not explicitly state
whether a five- or six-charge item is downgraded. Production conservatively preserves those items
until matching-client capture resolves that destructive edge case. Trimmed-glory support follows
the cited functional-equivalence evidence but also remains capture-bound. The historical sources
do not publish literal empty/Legends failure text, whether casting on an inventory containing only
four-charge items consumes resources, exact noted-item behavior, the precise recast/queue
semantics, graphic height/delay or audiovisual/inventory/rune/XP packet order. These boundaries
remain isolated in manual case 45; the core is `integration-tested`, not client-tested or
verified.

## Lunar Cure Other

The final pre-cutoff [Cure Other revision
14120852](https://oldschool.runescape.wiki/w/Cure_Other?oldid=14120852), dated 13 July 2021 and
still current at the cache-233.4 cutoff, defines a members-only level-68 Lunar combat spell
unlocked by Lunar Diplomacy. It consumes one astral, one law and ten earth runes, awards 65 Magic
XP and cures a targeted poisoned player only when that player has Accept Aid enabled. The page
explicitly says the spell does not grant poison immunity. Its infobox dates the spell to 24 July
2006. It does not publish exact clean-target behavior, literal success/rejection messages, venom
state arithmetic, cast interval, target graphic protocol fields or packet order.

The historical [Cure Other animation revision
4979872](https://oldschool.runescape.wiki/w/File:Cure_Other.gif?oldid=4979872) is a 73-frame,
5.5-second Jagex-sourced capture. Direct comparison with the dated Stat Spy and Heal Other
captures shows the same rotating-crystal recipient effect. A later
[Lunar asset list](https://rune-server.org/threads/lunar-magics-emotes-gfxs.594796/) independently
assigns sequence 4411 and target graphic 738 at height 96 to Cure Other, and assigns the same
target graphic to Stat Spy and Heal Other. Cache 233.4 resolves spell object 9110
(`68_cure_other`), Lunar player-target component 218:109, sequence 4411
(`quest_lunar_pushing_magic_animation`), spot animation 738
(`quest_lunar_spellbook_energy_heal_spot_anim`), synth 2884 (`lunar_cure`), poison varp 102 and
Accept Aid varbit 4180. The local revision-matched RuneLite checkout was inspected without launch
and independently names object 9110, component 218:109 and spot animation 738.

The final pre-cutoff [Venom revision
14984136](https://oldschool.runescape.wiki/w/Venom?oldid=14984136) states that any poison-curing
method converts venom to ordinary poison at the same damage. Combining that rule with RuneLite's
documented poison var contract gives the same deterministic conversion used by Cure Me:
`venom_damage = min(20, (state - 999997) * 2)` followed by
`poison_state = venom_damage * 5`. This maps the 6-through-20 venom stages to ordinary poison
states 30 through 100 without inventing a separate Cure Other formula.

Production requires members, Lunar Diplomacy, Lunar spellbook, visible Magic 68 and target Accept
Aid before atomically removing runes or granting XP. Self-targets, clean state zero, negative
immunity states and casts inside the spell's cooldown no-op without resources, XP or presentation.
Successful casts clear ordinary poison or downgrade venom through the shared formula, preserve
undocumented varp 3777/varbit 6251, award 65 Magic XP outside the Fountain of Rune, animate the
caster with sequence 4411, apply recipient graphic 738 at height 96 and emit typed synth 2884. The
dedicated support handler uses the standard eight-tile player-target route and is excluded from
offensive spell registration despite the cache `Combat` classification. Earth-providing staves,
rune pouches and Fountain behavior remain centralized in the shared rune manager.

The established repository Teleother wording supplies `${target.displayName} is not accepting
aid.` for the rejected Accept Aid path; this is architectural consistency, not proof of Cure
Other's production literal. The ordinary five-tick combat rate is provisional because the Wiki
does not state the exact interval and the secondary asset record only narrows it to four or five
ticks. The historical [Cure Other sound revision
14885139](https://oldschool.runescape.wiki/w/File:Cure_Other.ogg?oldid=14885139) differs in duration
and bytes from Cure Me's recording, while the cache symbols expose only one named `lunar_cure`
synth; exact sound mapping therefore remains capture-bound. Clean/immunity behavior, success
feedback, target graphic slot/delay, exact cooldown sharing, secondary poison state and scheduling,
movement/logout races and audiovisual/var/rune/XP packet order remain manual case 47. The core is
`integration-tested`, not client-tested or verified.

## Lunar Cure Group

The final pre-cutoff [Cure Group revision
14896822](https://oldschool.runescape.wiki/w/Cure_Group?oldid=14896822), dated 8 May 2025 and still
current at the cache-233.4 cutoff, defines a members-only level-74 Lunar combat spell unlocked by
Lunar Diplomacy. It consumes two astral, two cosmic and two law runes, awards 74 Magic XP and cures
players within one square of the caster. This is a Chebyshev radius-one/3×3 area, so diagonals and
other players standing on the caster's square are included. Recipients must have Accept Aid enabled
to benefit. The page does not distinguish the caster's own Accept Aid state, state whether a clean
cast consumes resources, identify which clean or immune recipients receive presentation, describe
venom arithmetic, publish literal messages or specify timing and packet order.

The historical [Cure Group animation revision
4979880](https://oldschool.runescape.wiki/w/File:Cure_Group.gif?oldid=4979880) is a 4.85-second,
Jagex-sourced capture whose frames show the caster and adjacent recipient receiving the same white
burst effect while the caster uses the group-cast motion. Cache 233.4 resolves spell object 9116
(`74_cure_group`), Lunar overlay component 218:117, sequence 4409
(`quest_lunar_spell_cast_spell_on_group`), spot animation 744
(`quest_lunar_spellbook_cure_group_spot_anim`), synth 2884 (`lunar_cure`), poison varp 102 and
Accept Aid varbit 4180. The local revision-matched RuneLite checkout was inspected without launch
and independently names object 9116, component 218:117, sequence 4409 and graphic 744. A later
[Lunar asset list](https://rune-server.org/threads/lunar-magics-emotes-gfxs.594796/) independently
records sequence 4409 and graphic 744 at height 96 on the caster and other players. That list's
one-to-three-tick delay note does not unambiguously define the spell's recast interval.

The cutoff [Venom revision
14984136](https://oldschool.runescape.wiki/w/Venom?oldid=14984136) applies the same rule as the
other cure spells: any poison-curing method downgrades venom to ordinary poison at the same damage.
Production therefore reuses `LunarPoisonCure`, clearing positive sub-million poison state to zero
and mapping venom through `poison_state = min(20, (state - 999997) * 2) * 5`. It preserves clean and
negative immunity state plus undocumented varp 3777/varbit 6251.

Production performs a normal successful cast after members, Lunar Diplomacy, Lunar spellbook,
visible Magic 74 and rune preflight, even if no player in range has a positive poison state. It
awards 74 Magic XP outside the Fountain, stops the current action, animates with sequence 4409,
emits typed synth 2884 and snapshots the current player list. The caster is always included within
the same plane/radius regardless of the caster's Accept Aid setting; every other recipient requires
Accept Aid. All included players receive graphic 744 at height 96, while only positive poison or
venom state mutates. Other planes and distance two are excluded. Cure Group is explicitly excluded
from offensive registration despite its cache `Combat` classification.

Clean-cast success, caster Accept Aid independence and clean-recipient presentation are supported
by the area-spell shape, archived animation and a concrete legacy implementation, but remain
documented inferences rather than production captures. The five-tick rate is also provisional.
The historical [Cure Group sound revision
14885142](https://oldschool.runescape.wiki/w/File:Cure_Group.ogg?oldid=14885142) is 2.1 seconds and
differs from the Cure Me and Cure Other recordings, while cache symbols expose only the shared
`lunar_cure` synth. Exact messages, sound identity, recipient snapshot/order, same-tick movement,
Accept Aid and logout races, clean/immunity graphics, shared cooldown, secondary poison scheduling,
graphic slot/delay and audiovisual/var/rune/XP packet order remain manual case 48. The core is
`integration-tested`, not client-tested or verified.

## Lunar Dream

The final pre-cutoff [Dream revision
14844104](https://oldschool.runescape.wiki/w/Dream?oldid=14844104), dated 29 January 2025 and still
current at the cache-233.4 cutoff, defines a members-only level-79 Lunar combat spell unlocked by
Dream Mentor. It consumes two astral, one cosmic and five body runes, awards 82 Magic XP and makes
the caster regenerate Hitpoints at three times the ordinary rate: one Hitpoint every 20 seconds,
or three per minute, on a timer independent of ordinary Hitpoints regeneration. The player remains
asleep indefinitely until reaching base Hitpoints. Any action except chat or changing armour ends
the spell, and poison damage also interrupts it. A full-health player cannot cast Dream until
damaged again. The separate-timer statement cites an archived [Mod Ash
answer](https://archive.is/fKvex); the Wiki does not establish the first-heal phase or integer game-
tick distribution.

The page also records a Nightmare Zone restriction and a jar-of-dreams easter egg. Its stated
Nightmare Zone message is `You cannot perform Inception here. Dreaming within this dream will
inevitably activate your Kick (i.e. Waking up from the dream).` A historical Nightmare Zone source
instead ended that sentence with `(i.e. death).`, so the literal remains a source conflict pending
cache-233 production capture. Using a jar of dreams while sleeping is described as briefly making
the screen black, showing the Nightmare's face and playing the Armadyl crossbow special-attack
sound. Neither activity behavior is part of the current core.

The historical [Dream animation revision
4979890](https://oldschool.runescape.wiki/w/File:Dream.gif?oldid=4979890) is a 5.28-second,
Jagex-sourced capture showing the player yawn, kneel, assume the sleeping pose and emit floating Z
graphics. Cache 233.4 resolves spell object 11148 (`79_dream`), Lunar overlay component 218:125,
start sequence 7627 (`dream_spell_start`), sleeping sequence 6296
(`dream_player_dream_spell_ready`), end sequence 7628 (`dream_spell_end`), Z sequence 6302
(`dream_zeds`), spot animation 1056 (`dream_zeds_spotanim`), Lunar Diplomacy state 190 and Dream
Mentor completion state 28. No Dream-named cache synth exists, agreeing with the spell infobox's
`sound = No`. The local revision-matched RuneLite checkout was inspected without launch and
independently names object 11148, sequences 6296/7627/7628 and spot animation 1056. Two lower-
confidence historical implementations independently support the conceptual start/sleep/end state
machine, but their older 6295/6296/6297 sequence set is not used as cache-233 identity evidence:
[Dream lunar spell](https://rune-server.org/threads/dream-lunar-spell.455556/) and [Lunar
spells](https://rune-server.org/threads/lunar-spells.462869/).

Production requires members, Lunar Diplomacy, Dream Mentor, Lunar spellbook, visible Magic 79 and
current Hitpoints below base before any rune or XP mutation. It atomically charges the exact spell
runes, awards 82 Magic XP outside the Fountain of Rune, stops the previous action and enters cache
start sequence 7627. After that sequence's cache-derived tick duration, it changes to sleeping
sequence 6296 and emits Z spot animation 1056. A tokenized normal queue then heals one Hitpoint at
elapsed ticks 34, 67 and 100 of every 100-tick period, exactly preserving the source-backed three
Hitpoints per 60 seconds on the server's 600 ms integer tick grid. The 34/33/33 arrangement and
phase origin are explicit deterministic inferences, not claims about the production scheduler.
The independent channel continues until full Hitpoints, then clears the Z effect and emits wake
sequence 7628. Hitpoint loss or a published non-chat player-action interruption follows the same
wake path; chat leaves the channel active, and a direct recast while active is an atomic no-op.
Login/logout clear stale channel state. Fountain casts consume no runes and award no XP through the
shared rune policy.

Eight cache-aware scenarios cover exact cache requirements and presentation identities, successful
rune/XP mutation, the 34/67/100 healing schedule, cache-duration start-to-sleep transition,
full-health termination, action and damage interruption, chat and recast behavior, every access and
resource gate, Fountain behavior, overlay dispatch and session cleanup. Exact first-heal phase,
33/34 distribution, Z repeat/slot/height and wake timing, the armour-change exception, literal
full-health feedback, simultaneous damage/healing order, main-regeneration/Rapid Heal/Hitpoints-
cape/regen-bracelet interaction, Nightmare Zone detection/message, jar-of-dreams client effect,
login/network races and packet order remain manual case 49. The core is `integration-tested`, not
client-tested or verified.

## Lunar Energy Transfer

The final pre-cutoff [Energy Transfer revision
14754923](https://oldschool.runescape.wiki/w/Energy_Transfer?oldid=14754923), dated 27 September
2024, defines a members-only level-91 Lunar combat spell unlocked by Lunar Diplomacy. It consumes
three astral, two law and one nature rune, awards 100 Magic XP, requires the recipient's Accept Aid
setting, costs the caster 100% special-attack energy and exactly 10 Hitpoints, and restores all of
the recipient's run and special-attack energy. The caster must have more than 10 Hitpoints; at 10
or fewer the literal message is `You need more hitpoints to cast this spell.` A separate cutoff
[Special attacks revision
14994987](https://oldschool.runescape.wiki/w/Special_attacks?oldid=14994987) independently agrees
that both recipient energy pools are restored to 100%. A cutoff Combat spells table says only
`some` run energy, but the feature-specific page and the dedicated special-attack page are more
specific and agree, so full restoration is the selected behavior and the table wording is logged
as a source conflict.

Three official update records establish the PvP restrictions. [New Animations & Barbarian Assault
Changes](https://oldschool.runescape.wiki/w/Update:New_Animations_%26_Barbarian_Assault_Changes)
blocked a caster in multi-way Wilderness from targeting a player in single-way Wilderness on 24
September 2015. [The Wintertodt](https://oldschool.runescape.wiki/w/Update:The_Wintertodt)
generalized the rule on 8 September 2016 so Energy Transfer cannot be cast in single-way PvP combat
areas. [Bird Nest Balancing and PvP
Changes](https://oldschool.runescape.wiki/w/Update:Bird_Nest_Balancing_and_PvP_Changes) added the 22
November 2018 rule that a target recently in combat on a PvP world cannot receive it. The updates do
not publish literal rejection text, the length of `recently`, or the safe-zone boundary model. The
implemented core therefore uses the server's active-PvP-combat predicate and rejects any transfer
where either Wilderness participant is single-way. PvP-world safe/single/multi classification
outside the Wilderness remains open rather than treating an entire PvP world as dangerous and
incorrectly blocking safe zones.

The historical Jagex-sourced [Energy Transfer animation revision
4979905](https://oldschool.runescape.wiki/w/File:Energy_Transfer.gif?oldid=4979905) is a
232-by-320, 6.29-second capture whose underlying image was uploaded in 2015. Frame inspection shows
the caster kneel, the recipient become surrounded by the transfer effect and the caster receive a
visible red 10 self-hit. The pre-cutoff [Energy Transfer sound revision
14885107](https://oldschool.runescape.wiki/w/File:Energy_Transfer.ogg?oldid=14885107) is a
Jagex-sourced 4.528-second recording whose source file dates to 2020. Cache 233.4 resolves spell
object 9133 (`91_energy_transfer`), Lunar player-target component 218:139, sequence 4414
(`quest_lunar_spell_energy_exchange`) and recipient graphic 734
(`quest_lunar_spellbook_energy_exchange_spot_anim`). The revision-matched RuneLite checkout was
inspected without launch or modification and independently corroborates the spell, component,
sequence and graphic identities. Its generic animation constant associates 4411 with several
target Lunar spells, but its exact cache-derived gameval assigns Energy Transfer to 4414; the exact
cache identity and historical kneeling capture outweigh the generic grouping.

Two independent legacy cache sound-name lists map synth 2885 to `lunar_energy_transfer` and synth
2906 to `lunar_transfer_other`. Production sends 2885 to the caster and 2906 to the recipient as the
most direct semantic mapping, pending matching-client capture. Recipient graphic height 96 and the
ordinary five-tick support-spell rate are also provisional. One successful cast performs all gates
before mutation, consumes the exact runes, awards XP outside the Fountain of Rune, drains caster
special energy to zero, restores recipient run and special energy to 10,000 and 1,000 internal
units, emits the cache presentation and applies a visible typeless 10-point self-hit. Eight
cache-aware integration scenarios cover cache identities, exact successful mutation and effects,
the 10/11-Hitpoints boundary, self/dead/Accept Aid/incomplete-special atomic failures, all access
and rune failures, single-way Wilderness and PvP-world recent-combat restrictions, five-tick and
Fountain behavior, and range-eight routing.

Energy Transfer's nature-rune requirement exposed a separate false substitute: the shared rune
table treated charged Bryophyta's staff as unlimited nature runes. The official [Bryophyta launch
update](https://oldschool.runescape.wiki/w/Update:Bryophyta:_The_Moss_Giant_Boss) and cutoff
[Bryophyta's staff page](https://oldschool.runescape.wiki/w/Bryophyta%27s_staff?oldid=14917258)
instead define a finite store of at most 1,000 nature runes and a 1/15 chance to save exactly one
stored rune per qualifying cast. The false mapping is removed and the per-item storage core is now
integration-tested; its detailed cache contract, implementation boundary and remaining unknowns are
recorded in the dedicated Bryophyta section below. The Energy Transfer core remains
`integration-tested`, not client-tested or verified; its remaining observations are manual case 51.

## Lunar Heal Other

The historical [Heal Other revision
14538824](https://oldschool.runescape.wiki/w/Heal_Other?oldid=14538824), dated before the cache-233
cutoff, defines a members-only level-92 Lunar combat spell unlocked by Lunar Diplomacy. It consumes
three astral, one blood and three law runes, awards 101 Magic XP, transfers up to 75% of the
caster's current Hitpoints, requires the caster to retain at least 11% Hitpoints, requires the
recipient's Accept Aid setting and cannot be received by an Ironman. The page records that a
5 May 2021 update briefly put Heal Other and Heal Group on one 90-second cooldown, then a same-day
hotfix removed the cooldown from Heal Other. The contemporaneous [Games Room Tables and Fishing
Skill Boss Improvements update](https://oldschool.runescape.wiki/w/Update:Games_Room_Tables_and_Fishing_Skill_Boss_Improvements)
independently records that hotfix; production therefore must not apply Heal Group's special
cooldown to Heal Other.

The historical Jagex-sourced [Heal Other animation revision
4979907](https://oldschool.runescape.wiki/w/File:Heal_Other.gif?oldid=4979907) is a 193-by-297,
5.96-second capture. Frame inspection shows a 99-Hitpoints caster receive a visible red 74 self-hit
and finish near 25 Hitpoints, directly resolving integer rounding as
`floor(current Hitpoints * 75 / 100)`. It also shows the same recipient crystal effect seen in the
dated Cure Other and Stat Spy captures. The historical [Heal Other sound revision
14885149](https://oldschool.runescape.wiki/w/File:Heal_Other.ogg?oldid=14885149) is a 4.346-second
Jagex-sourced recording. Lower-confidence period testing titled `Enhanced Heal Other Data` reports
that the caster loses only the recipient's missing Hitpoints when the 75% maximum exceeds the
deficit. That measured claim agrees with the Wiki's `up to` wording and outweighs an earlier forum
request that asserted the opposite while explicitly asking others to confirm it. The implemented
transfer is therefore `min(floor(current * 75 / 100), target base-current deficit)`, with the cap
classified as a bounded secondary-source inference pending production capture.

Cache 233.4 resolves spell object 9134 (`92_heal_other`), Lunar component 218:140 with player-target
operation `Cast`, sequence 4411 (`quest_lunar_pushing_magic_animation`), target graphic 738
(`quest_lunar_spellbook_energy_heal_spot_anim`) and Accept Aid varbit 4180. A historical Lunar asset
list independently associates Heal Other with sequence 4411 and target graphic 738 at height 96.
Two independent legacy cache sound-name lists map synth 2895 to `lunar_heal_other` and synth 2892
to `lunar_heal_other_individual`; local cache parsing proves synth 2895 exists in cache 233.4 and
has an encoded maximum instrument duration of 2.5 seconds, compatible with the padded Wiki audio
recording. The revision-matched RuneLite checkout was inspected without modification or launch and
independently corroborates component 218:140. Exact caster/recipient sound routing remains unknown,
so production emits the evidence-backed named spell synth 2895 only from the caster pending manual
case 50.

Production requires members, completed Lunar Diplomacy, the Lunar spellbook, visible Magic 92, a
different living player target with Accept Aid enabled, a damaged target and caster current
Hitpoints satisfying `current * 100 >= base * 11`, all before resource mutation. One successful
cast atomically charges the exact runes, awards 101 Magic XP outside the Fountain of Rune, stops the
previous action, emits sequence 4411, target graphic 738 at height 96 and synth 2895, heals the
recipient by the computed transfer and applies the same visible typeless instant hit to the caster.
The ordinary five-tick action lock is provisional because no dated authoritative source states the
cadence. Self, cooldown, access, Accept Aid-off, dead/full target, low caster health and zero-
transfer attempts are atomic no-ops. Cache combat classification is explicitly excluded from the
offensive spell registry.

The Wiki's account-mode restriction cannot yet be implemented because the repository contains only
an unimplemented `account.modes` coverage group and no authoritative runtime account-mode model.
Adding a local proxy flag would create false architecture and is rejected. Exact messages, whether
11% uses base or another maximum-health quantity at every boost boundary, target/caster synth split,
PvP/singles restrictions, support-combat side effects, target movement/logout/death races, ordinary
and cross-action cadence, hitmark/headbar and graphic timing and complete audiovisual/stat/rune/XP
packet order remain manual case 50. Eight cache-aware scenarios cover the exact cache/formula and
presentation contract, 99-to-74 full transfer, recipient-deficit cap, threshold/self/full/dead/
Accept Aid atomic failures, every access/resource gate, five-tick repeat with no 90-second special
cooldown, Fountain policy and range-eight player targeting. The core is `integration-tested`, not
client-tested or verified.

## Lunar Heal Group

The historical [Heal Group revision
14407925](https://oldschool.runescape.wiki/w/Heal_Group?oldid=14407925), which predates the
cache-233.4 cutoff, defines a members-only level-95 Lunar combat spell unlocked by Lunar
Diplomacy. It consumes four astral, three blood and six law runes, awards 124 Magic XP and reduces
the caster's current Hitpoints by 75% to heal up to five injured Accept-Aid players within one
square, including players sharing the caster's tile. It adds five points to the sacrificed amount
before dividing healing evenly, requires the caster to retain the spell's published 11%-health
threshold and has a 150-tick/90-second cooldown. The page also permits the spell in single-way PvM
but excludes single-way PvP.

Jagex's official 5 May 2021 Games Room Tables and Fishing Skill Boss Improvements announcement is
preserved in the [Steam community-announcement
mirror](https://store.steampowered.com/news/posts/?enddate=1620209396&feed=steam_community_announcements).
It independently states that Heal Group divides up to 75% of the caster's health equally among up
to five nearby injured players, adds five healing points, updates an existing attack delay by three
cycles like eating and applies a 90-second cooldown. The same-day hotfix removed Heal Other from
that cooldown but did not remove Heal Group. The cutoff Wiki's generic attack-range page describes
a broader 5-by-5 category, while the feature-specific historical page and Lunar-spellbook evidence
say one square; the implementation records that conflict and follows the more specific exact
3-by-3 area.

Neither authoritative source resolves integer remainder allocation, redistribution when a target
needs less than its equal share, or whether the caster loses the complete nominal sacrifice when
some healing cannot be used. Production therefore uses a bounded structural inference:
`sacrifice = floor(current Hitpoints * 75 / 100)`, `share = floor((sacrifice + 5) / recipient
count)`, heals each selected target by `min(share, base-current deficit)`, discards the remainder,
does not redistribute capped shares and applies the full sacrifice to the caster. These details
remain explicitly provisional pending manual case 53 rather than being represented as verified
Wiki behavior.

Cache 233.4 resolves spell object 9137 (`95_heal_group`), overlay component 218:143, sequence 4409
(`quest_lunar_spell_cast_spell_on_group`), recipient graphic 745
(`quest_lunar_spellbook_heal_group_spot_anim`) and cooldown varbit 925
(`heal_group_timelimit`). Two legacy cache sound-name lists map synth 2894 to `lunar_heal_group`
and synth 2893 to `lunar_heal`. The historical Jagex-licensed [Heal Group sound revision
14885108](https://oldschool.runescape.wiki/w/File:Heal_Group.ogg?oldid=14885108) provides a
2.3-second reference recording. The revision-matched RuneLite checkout was inspected read-only and
not launched: it independently maps object 9137, component 218:143, graphic 745 and varbit 925, and
its timer plugin creates a 150-game-tick Heal Group timer when varbit 925 becomes one.

Production validates membership, Lunar Diplomacy, Lunar spellbook, visible Magic 95, the
11%-of-base-health threshold, exact resources and at least one eligible recipient before mutation.
Eligible recipients are living, injured, Accept-Aid players on the same plane and within Chebyshev
distance one; at most the first five player-list entries are selected. Single-way Wilderness
recipients are excluded, as are active single-way PvP-world combat recipients, while multiway
recipients are allowed. Success charges resources atomically, awards 124 Magic XP outside the
Fountain of Rune, emits caster sequence 4409 and synth 2894, gives recipients graphic 745 at height
96 and synth 2893, applies the recipient heals and matching visible typeless caster self-hit, adds
three cycles only when an attack delay was already active, starts the ordinary provisional
five-tick utility lock and starts the exact 150-tick dedicated cooldown. Varbit 925 is synchronized
on cast, login and expiry.

Twelve cache-aware scenarios cover cache/formula/presentation contracts, one and six candidates,
slot-order selection, exact 3-by-3 and plane filtering, Accept Aid/living/injured selection, every
access/resource/recipient gate, capped-share behavior, tick-149/tick-150 cooldown boundaries,
active versus idle attack delay, single/multi Wilderness and PvP-world restrictions, Fountain of
Rune and direct interface dispatch. Exact feedback, account-mode rules, unresolved distribution
details, durable cooldown persistence beyond the retained player object, exact PvP-area ownership,
movement/logout/death races and full audiovisual/var/stat/rune/XP packet timing remain manual case
53. The core is `integration-tested`, not client-tested or verified.

## Lunar Spellbook Swap

The final pre-cutoff [Spellbook Swap revision
14842132](https://oldschool.runescape.wiki/w/Spellbook_Swap?oldid=14842132), dated 25 January
2025, defines a members-only level-96 Lunar utility spell unlocked by Dream Mentor. It consumes
three astral, two cosmic and one law rune and grants 130 Magic XP only after the player selects a
spellbook they can access. The resulting book lasts for one successful spell or two minutes, after
which the player returns to Lunar. The same page establishes a four-tick interaction block, permits
casting while already moving, prevents a stationary caster from beginning movement until the cast
finishes and suspends run-energy use during the moving cast.

The page's dated change history records that Spellbook Swap has reverted more consistently since
30 January 2014 and that an active swap is cancelled at the beginning of Last Man Standing since
25 October 2023. The cutoff spellbook access policy is corroborated by the [Arceuus spellbook
reference](https://oldschool.runescape.wiki/w/Arceuus_spellbook) and the [Varlamore Activities and
Kourend Changes update](https://oldschool.runescape.wiki/w/Update:Varlamore_Activities_%26_Kourend_Changes):
Standard is always available, Ancient requires Desert Treasure I, and Arceuus is available once
Tyss has unlocked that spellbook. A Kingdom Divided gates many individual Arceuus spells, not the
book choice itself.

Cache 233.4 resolves spell object 11150 (`96_spellbook_swap`), Lunar component 218:144, casting
sequence 6299, graphic 1062, client varbit 3617 (`lunar_spellbook_change`), selection varbit 4823
(`spellbook_swap_book`) and Arceuus-unlock varbit 9631. Legacy cache naming resolves synth 3613 as
`lunar_change_spellbook`. The revision-matched RuneLite checkout was inspected read-only and not
launched; it independently maps object 11150, component 218:144, sequence 6299, graphic 1062 and
both swap varbits, and interprets varbit 3617 as a 120-second timer trigger.

Production exposes Standard first, then Ancient and Arceuus only when their respective unlock
state is present, followed by Cancel. It preflights resources before the menu but does not consume
runes or award XP until a valid selection survives a second access/resource check. Success emits
the cache audiovisuals, switches the spellbook, starts an exact 200-tick expiry and synchronizes
the swap varbits. A common synchronous magic-cast event is emitted only after spell access and rune
transactions succeed; the swap listens to that seam, so failed casts retain it and the next
resource-successful spell from any spell family consumes it. A one-tick soft timer owns timeout
and LMS cancellation. Login retains an active state on the same live player object and repairs
orphaned client swap state by returning to Lunar.

An already-moving route is preserved. Its run-energy drain is suppressed while map clock is below
the cast's exclusive four-tick expiry, while the protected-access delay prevents a stationary
player from beginning another interaction until that boundary. Twelve cache-aware scenarios cover
cache contracts, Standard/Ancient/Arceuus selection, atomic cancellation and every access/resource
gate, tick-199/tick-200 expiry, failed-versus-successful next spells, LMS cancellation, same-object
login repair, Fountain behavior, presentation and route preservation. A separate game-process
regression proves four clocks without run-energy drain followed by ordinary drain at expiry.

No authoritative clientscript decode was found for varbit 4823. Production provisionally stores
the selected `Spellbook.varValue`; exact selection encoding, picker text/order, downstream spell
failures after resource commitment, cold-session persistence, literal feedback and complete packet
ordering remain manual case 54. The core is `integration-tested`, not client-tested or verified.

## Lunar fixed-destination self teleports

The pre-cutoff [Teleportation spells revision
14689651](https://oldschool.runescape.wiki/w/Teleportation_spells?oldid=14689651) and the cutoff
[Lunar spellbook reference](https://oldschool.runescape.wiki/w/Lunar_spellbook) establish eight
members-only, Lunar Diplomacy-gated self teleports. Moonclan is level 69, costs two astral/two
earth/one law and awards 66 Magic XP; Ourania is level 71, costs two astral/six earth/one law and
awards 69 XP; Waterbirth is level 72, costs two astral/one water/one law and awards 71 XP;
Barbarian Outpost is level 75, costs two astral/three fire/two law and awards 76 XP; Khazard is
level 78, costs two astral/four water/two law and awards 80 XP; Fishing Guild is level 85, costs
three astral/ten water/three law and awards 89 XP; Catherby is level 87 with the same rune cost and
92 XP; and Ice Plateau is level 89, costs three astral/eight water/three law and awards 96 XP. All
have a four-tick casting speed.

The Lunar-spellbook reference and [Lunar Diplomacy
guide](https://oldschool.runescape.wiki/w/Lunar_Diplomacy) independently state that Ourania is not
available merely from quest completion: the player must activate the Lunar spellbook and speak to
Baba Yaga. Cache 233.4 resolves that independent account state as varbit 5376
`rc_zmi_lunar_spell`; production requires it to be nonzero. The exact rejection text is not exposed
by the server cache and remains a bounded manual gap rather than an invented message.

The [Ice Plateau Teleport reference](https://oldschool.runescape.wiki/w/Ice_Plateau_Teleport) and
the Lunar-spellbook table place its destination in level 53 Wilderness and require a Deep
Wilderness warning with an abort path before resources are spent. Production therefore preflights
all ordinary requirements, cancels the prior activity on a valid click, presents a two-choice
warning, and revalidates access and resources after acceptance. Cancellation spends no runes and
awards no XP. Exact cache-233 warning interface, wording and settings interaction remain manual
case 55.

Cache 233.4 resolves the eight spell objects as 9111, 20759, 9114, 9117, 9120, 9127, 9129 and 9131
and their spellbook components as 218:111, 218:147, 218:115, 218:119, 218:123, 218:131, 218:134
and 218:137. Their `spell_telecoord` parameters respectively resolve to `(2114,3915,0)`,
`(2468,3246,0)`, `(2546,3756,0)`, `(2543,3569,0)`, `(2636,3167,0)`, `(2611,3391,0)`,
`(2801,3449,0)` and `(2974,3938,0)`. Sequence 4423, graphic 747 and synth 200 provide the shared
Lunar teleport presentation. The contemporaneous [Teleport sound revision
14890691](https://oldschool.runescape.wiki/w/File:Teleport.ogg?oldid=14890691) identifies synth 200
as the generic teleport sound used by these spells. The revision-matched RuneLite checkout was
inspected read-only and independently maps the Lunar teleport sequence and graphic; it was not
launched or modified.

Production enforces members, Lunar Diplomacy, Lunar spellbook, visible Magic level, exact atomic
rune requirements, the separate Ourania unlock, Tele Block and the ordinary level-20 source-
Wilderness boundary. Successful casts award the cache XP outside the Fountain of Rune, emit the
cache sequence/graphic/sound, wait exactly four ticks and teleport to the cache parameter. Ten
cache-aware scenarios cover every cache contract and direct component, all eight success paths,
exact resource/XP/presentation/timing/destination behavior, access and atomic failure boundaries,
Ourania unlock, level-20/21 Wilderness, Ice Plateau acceptance/cancellation, staff substitution and
Fountain behavior. Destination-area variation, exact warning and feedback presentation, movement,
Deadman behavior and complete packet order remain manual case 55. The
core is `integration-tested`, not client-tested or verified.

## Lunar group teleports

The final pre-cache-233.4 Wiki revisions for [Tele Group Moonclan
14863799](https://oldschool.runescape.wiki/w/Tele_Group_Moonclan?oldid=14863799), [Waterbirth
14863801](https://oldschool.runescape.wiki/w/Tele_Group_Waterbirth?oldid=14863801), [Barbarian
14863803](https://oldschool.runescape.wiki/w/Tele_Group_Barbarian?oldid=14863803), [Khazard
14863805](https://oldschool.runescape.wiki/w/Tele_Group_Khazard?oldid=14863805), [Fishing Guild
14863807](https://oldschool.runescape.wiki/w/Tele_Group_Fishing_Guild?oldid=14863807), [Catherby
14863809](https://oldschool.runescape.wiki/w/Tele_Group_Catherby?oldid=14863809) and [Ice Plateau
14863811](https://oldschool.runescape.wiki/w/Tele_Group_Ice_Plateau?oldid=14863811), all dated 17
March 2025, define the complete cutoff family. Each is members-only, requires Lunar Diplomacy and
the Lunar spellbook, and takes four ticks. The caster teleports immediately after that cast while
each eligible nearby player receives an independent confirmation. Candidates occupy the caster's
same-plane 3-by-3 square, including the caster's tile, and must have Accept Aid enabled.

The seven level, rune and XP contracts are respectively: Moonclan 70, four earth/two astral/one
law, 67 XP; Waterbirth 73, five water/two astral/one law, 72 XP; Barbarian 76, six fire/two
astral/two law, 77 XP; Khazard 79, eight water/two astral/two law, 81 XP; Fishing Guild 86,
fourteen water/three astral/three law, 90 XP; Catherby 88, fifteen water/three astral/three law,
93 XP; and Ice Plateau 90, sixteen water/three astral/three law, 99 XP. Cache spell objects
9112/9115/9118/9121/9128/9130/9132 and components 218:112/116/120/124/132/135/138 independently
pin those values and expose no `spell_telecoord`, confirming that the group destinations are
server-owned. All use sequence 4423, spot animation 747 and synth 200. Interface 326 exposes caster
name component 89, destination component 91 and accept/decline components 97/99; varbit 4180 is the
recipient Accept Aid setting.

The historical Wiki maps resolve exact discrete landing sets: Moonclan x=2107..2111 at z=3915;
Waterbirth the 5-by-5 square centred on 2546,3755; Barbarian the 5-by-5 square centred on
2543,3569; Khazard the 5-by-5 square centred on 2636,3167; Fishing Guild the 5-by-5 square centred
on 2613,3391; Catherby the 20-tile rectangle x=2799..2803/z=3448..3451 plus the three tiles
x=2801..2803/z=3447; and Ice Plateau the 7-by-7 square centred on 2975,3938. The polygon boundary
interpretation was cross-checked against the historical Teleother Lumbridge map and the existing
half-open server implementation.

Production snapshots eligible recipients when the caster commits the spell, sends each one the
native confirmation interface, and lets accept, decline, interface close and logout resolve
independently. The caster pays once, receives XP outside the Fountain of Rune and teleports after
four ticks; accepting recipients revalidate their own Tele Block and source Wilderness state before
a second four-tick Lunar presentation and a uniformly selected destination tile. Ice Plateau first
shows the caster's Deep Wilderness confirmation, before resources are committed, and revalidates
after acceptance. Eleven cache-aware scenarios cover all cache contracts, exact costs and XP,
presentation/timing, polygon boundaries, same-tile/diagonal/range/plane/Accept-Aid selection,
independent accept/decline/close/logout paths, access and resource atomicity, caster and recipient
Tele Block/Wilderness restrictions, Ice warning outcomes, interface dispatch, staff substitution
and Fountain behavior.

The Moonclan page says a recipient must previously have visited Lunar Isle, but [varbit 2448's
historical documentation](https://oldschool.runescape.wiki/w/RuneScape:Varbit/2448?oldid=14012141)
is explicitly incomplete and does not expose the exact visited-island threshold. It is not guessed.
The [Accept Aid revision
14519585](https://oldschool.runescape.wiki/w/Accept_Aid?oldid=14519585) also establishes
account-mode restrictions that the server's future account-mode subsystem must own. No
authoritative source found during this slice pins recipient animation/timing, literal confirmation
text/timeout, Ice recipient warning presentation, Deadman double-delay semantics or exact packet
ordering. Reusing the four-tick Lunar presentation for accepted recipients is therefore a bounded
inference, recorded for matching-client manual case 56. The core is `integration-tested`, not
client-tested or verified.

## Lunar Stat Restore Pot Share

The final pre-cache-233.4 [Stat Restore Pot Share revision
14897203](https://oldschool.runescape.wiki/w/Stat_Restore_Pot_Share?oldid=14897203), dated 8 May
2025, defines a members-only level-81 Lunar spell unlocked by Lunar Diplomacy. It consumes ten
earth, ten water and two astral runes and awards 84 Magic XP. One cast shares a dose from any
non-barbarian restoring potion with up to four other players in the caster's same-plane 5-by-5
area, including the caster's tile. Recipients require Accept Aid, except for the documented
same-Group-Ironman exception, and the caster receives the potion effect without consuming an
additional dose. Each other recipient consumes one dose from the selected potion.

The historical page limits the compatible families to Restore, Energy, Prayer, Super energy,
Super restore, Sanfew serum and Stamina potion. Their cutoff revisions pin the immediate effects:
[Restore 14993871](https://oldschool.runescape.wiki/w/Restore_potion?oldid=14993871) restores
Attack, Strength, Defence, Ranged and Magic by `floor(base * 30 / 100) + 10`, capped at base;
[Energy 14993767](https://oldschool.runescape.wiki/w/Energy_potion?oldid=14993767) restores ten run
energy points; [Prayer 14993749](https://oldschool.runescape.wiki/w/Prayer_potion?oldid=14993749)
restores `floor(base Prayer * 25 / 100) + 7`; [Super energy
14993807](https://oldschool.runescape.wiki/w/Super_energy?oldid=14993807) restores twenty run
energy points; and [Super restore
14993752](https://oldschool.runescape.wiki/w/Super_restore?oldid=14993752) restores every skill
except Hitpoints by `floor(base * 25 / 100) + 8`, with Prayer using 27 percent. [Sanfew serum
14993774](https://oldschool.runescape.wiki/w/Sanfew_serum?oldid=14993774) restores every skill
except Hitpoints by `floor(base * 30 / 100) + 4`, with Prayer using 32 percent, cures poison or
venom and disease, and ordinarily grants six minutes of antipoison plus fifteen minutes of disease
immunity. [Stamina potion
14993745](https://oldschool.runescape.wiki/w/Stamina_potion?oldid=14993745) restores twenty run
energy points and reduces run-energy drain by 70 percent for two minutes.

The 11 January 2023 Stat Restore Pot Share change makes every recipient's own prayer-restoration
equipment apply independently. A holy wrench, Prayer cape, Max cape or imbued Ring of the gods
adds two percentage points to the Prayer-potion, Super-restore and Sanfew Prayer formulas. Cache
objects pin holy wrench 6714, Prayer cape 9759/9760, Max cape 13280/13342 and the canonical imbued
Ring of the gods identities 13202/25252/26764. Potion-dose chains are Restore
2430/127/129/131, Energy 3008/3010/3012/3014, Prayer 2434/139/141/143, Super energy
3016/3018/3020/3022, Super restore 3024/3026/3028/3030, Sanfew
10925/10927/10929/10931 and Stamina 12625/12627/12629/12631; the final dose becomes empty vial
229. Cache spell object 9123 and component 218:127 independently pin the level, runes and XP.
The component has no direct operation because this is an item-target spell.

Cache sequence 4413 and graphic 733 define the shared potion-cast presentation. Varp 456 is the
disease value. Varbits 24 and 25 are the stamina duration and active flag; read-only inspection of
the revision-matched RuneLite `AnimationID.MAGIC_LUNAR_SHARED`, `GraphicID.POT_SHARE`,
`VarbitID.STAMINA_DURATION`, `VarbitID.STAMINA_ACTIVE` and its timers-and-buffs logic corroborates
those identities and that each client duration unit represents ten server cycles. The production
repository therefore owns an exact 200-cycle stamina effect, refreshes it on another dose, keeps
the two varbits synchronized and applies the documented 70-percent multiplier in the shared run-
energy drain engine.

Eight cache-aware scenarios cover the cached spell/item/interface contracts, exact 5-by-5 and
plane selection, shared tiles, Accept Aid, four-recipient and available-dose caps, the free caster
effect, every immediate potion family, per-recipient Prayer modifiers, 200-cycle stamina refresh
and expiry, zero-recipient and invalid-potion atomicity, access/rune failures, full-inventory final-
dose replacement, Fountain behavior, the provisional three-cycle lock and direct item-target UI
dispatch. Candidate priority follows deterministic player-list iteration. With zero eligible
recipients the cast is an atomic no-op, a bounded interpretation of the historical requirement for
another player and one consumed dose per other recipient.

The server has no account-mode service capable of proving the Group Ironman exception. The later
protection slice added Sanfew's exact six-minute poison immunity and session-local fifteen-minute
disease immunity, and the later toxin-lifecycle slice added exact recurring poison/venom damage and
interface pausing. Source-specific poison/disease producers, disease death/reconstruction cleanup,
Ring of Endurance extension and durable stamina lifecycle remain open; current stamina state is
session-local and cleared on login/logout. No authoritative cutoff source found during this slice
pins recipient priority, zero-recipient feedback, the exact cast lock, graphic height/delay, a
sound, literal messages or packet order. The three-cycle lock and graphic height 96 are bounded
inferences from potion-share training rates and adjacent Lunar spell presentation. These items
remain matching-client manual case 57, so the core is `integration-tested`, not client-tested or
verified.

## Lunar Boost Potion Share

The final pre-cache-233.4 [Boost Potion Share revision
14766606](https://oldschool.runescape.wiki/w/Boost_Potion_Share?oldid=14766606), dated 12 October
2024, defines a members-only level-84 Lunar combat spell unlocked by Lunar Diplomacy. It consumes
twelve earth, ten water and three astral runes and awards 88 Magic XP. Like Stat Restore Pot Share,
it consumes one dose for each of up to four eligible other players in the caster's same-plane 5-by-
5 area, including the caster's tile, while applying the effect to the caster for free. Recipients
require Accept Aid except for the documented same-Group-Ironman exception.

The same historical revision explicitly prohibits use in Castle Wars with `You don't want to help
your enemies`. The cache-backed multiway-area enrichment pins the ordinary Castle Wars arena to
level 0, x=2368..2431 and z=3072..3135. It also lists an explicit incompatible set: Absorption,
Ancient brew, Castlewars brew, Combat potion, all eight cache-233 Divine potion families, Energy,
Forgotten brew, Goblin potion, Guthix rest, Hunter potion, Magic essence, Menaphite remedy, both
Overloads, Prayer potion, Prayer regeneration potion, Restore potion, Saradomin brew, Stamina,
Super energy, Super magic, Super ranging, Super restore and Zamorak brew. Barbarian mixes are
excluded categorically. This core rejects unregistered families rather than treating every vial-
shaped object as safe.

Thirteen ordinary, tradeable stat-boost families have complete independent cutoff evidence:
[Attack potion 14993858](https://oldschool.runescape.wiki/w/Attack_potion?oldid=14993858),
[Strength potion 14993834](https://oldschool.runescape.wiki/w/Strength_potion?oldid=14993834) and
[Defence potion 14993883](https://oldschool.runescape.wiki/w/Defence_potion?oldid=14993883) add
`floor(base * 10 / 100) + 3`; [Ranging potion
14993762](https://oldschool.runescape.wiki/w/Ranging_potion?oldid=14993762) adds
`floor(base * 10 / 100) + 4`; and [Magic potion
14993839](https://oldschool.runescape.wiki/w/Magic_potion?oldid=14993839) adds four. [Super attack
14993801](https://oldschool.runescape.wiki/w/Super_attack?oldid=14993801), [Super strength
14993795](https://oldschool.runescape.wiki/w/Super_strength?oldid=14993795) and [Super defence
14993779](https://oldschool.runescape.wiki/w/Super_defence?oldid=14993779) each add
`floor(base * 15 / 100) + 5` to their respective stat.

[Super combat potion
14993756](https://oldschool.runescape.wiki/w/Super_combat_potion?oldid=14993756) applies that
super-potion formula to Attack, Strength and Defence. [Bastion potion
14993829](https://oldschool.runescape.wiki/w/Bastion_potion?oldid=14993829) combines the Ranging and
Super-defence formulas, while [Battlemage potion
14993845](https://oldschool.runescape.wiki/w/Battlemage_potion?oldid=14993845) combines the Magic
and Super-defence formulas. [Agility potion
14993849](https://oldschool.runescape.wiki/w/Agility_potion?oldid=14993849) and [Fishing potion
14993896](https://oldschool.runescape.wiki/w/Fishing_potion?oldid=14993896) each add three. The
shared stat-boost primitive adds the calculated amount to a drained current level but caps it at
base plus the formula boost, matching ordinary potion behavior.

Cache spell object 9126 and component 218:130 independently pin the level, runes and XP; the
component exposes no direct operation because the spell targets an inventory item. All 52 dose
objects are resolved through typed symbols, and the final dose becomes vial 229. Cache sequence
4413 and graphic 733 define the presentation. Read-only inspection of the revision-matched
RuneLite checkout independently corroborates `AnimationID.MAGIC_LUNAR_SHARED`,
`GraphicID.POT_SHARE`, item 9126 and component 218:130; it was neither launched nor modified.

Nine cache-aware scenarios cover spell, component, item and presentation contracts; every immediate
formula; same-tile/boundary/range/plane/Accept-Aid selection; four-recipient and available-dose
caps; free caster effects; full-inventory transformations; Combat-potion rejection; Castle Wars;
membership, quest, level and rune failures; Fountain policy; the provisional three-cycle lock and
direct item-target dispatch.

The cutoff [Antipoison 14993758](https://oldschool.runescape.wiki/w/Antipoison?oldid=14993758),
[Superantipoison 14993800](https://oldschool.runescape.wiki/w/Superantipoison?oldid=14993800),
[Antidote+ 14993895](https://oldschool.runescape.wiki/w/Antidote%2B?oldid=14993895) and
[Antidote++ 14993822](https://oldschool.runescape.wiki/w/Antidote%2B%2B?oldid=14993822)
revisions establish poison-immunity durations of 90 seconds, six minutes, nine minutes and twelve
minutes respectively. They cure ordinary poison but only downgrade venom to equal-damage ordinary
poison on the first dose. The cutoff [Anti-venom
14993833](https://oldschool.runescape.wiki/w/Anti-venom?oldid=14993833), [Anti-venom+
14993872](https://oldschool.runescape.wiki/w/Anti-venom%2B?oldid=14993872) and [Extended
anti-venom+ 14993931](https://oldschool.runescape.wiki/w/Extended_anti-venom%2B?oldid=14993931)
revisions instead cure venom outright and establish progressively longer poison and venom
protection. [Sanfew serum 14993774](https://oldschool.runescape.wiki/w/Sanfew_serum?oldid=14993774)
explicitly records Boost Potion Share compatibility, the same six-minute antipoison effect, its
restore formulas and fifteen-minute disease immunity.

The cutoff [Poison 14994137](https://oldschool.runescape.wiki/w/Poison?oldid=14994137) and [Venom
14984136](https://oldschool.runescape.wiki/w/Venom?oldid=14984136) mechanics, corroborated by the
read-only RuneLite `VarPlayer.POISON` contract, explain the exact client encoding: negative varp-102
values advance once per 30 game ticks, -38 through -1 are poison immunity, and values below -38 are
also venom immunity. The starting states are therefore -5, -20, -30, -40, -41, -50 and -59. Global
toxin-phase alignment naturally produces Antidote++'s documented 18–36 second and anti-venom's
36–54 second venom windows; no random duration was invented. Weaker remedies preserve longer
active protection, matching the historical 7 April 2016 change. All 32 cache dose objects are
typed and tested. The timer pauses on logout and resumes from persisted varp state on login.

The four antifire families now have a shared, cache-visible protection lifecycle and a bounded
standard-dragonfire consumer. The cutoff [Antifire potion
14993772](https://oldschool.runescape.wiki/w/Antifire_potion?oldid=14993772), [Extended antifire
14993875](https://oldschool.runescape.wiki/w/Extended_antifire?oldid=14993875), [Super antifire
potion 14993848](https://oldschool.runescape.wiki/w/Super_antifire_potion?oldid=14993848) and
[Extended super antifire
14993908](https://oldschool.runescape.wiki/w/Extended_super_antifire?oldid=14993908) revisions pin
six-, twelve-, three- and six-minute durations. Cache varbits 3981 and 6101, independently
corroborated by the read-only revision-matched RuneLite checkout, expose ordinary antifire in
30-cycle units and superior antifire in 20-cycle units. All 16 cache dose objects are typed; the
state emits the documented 15-second warning and expiry messages, supports same-tier replacement,
and preserves resident-session state across logout/resume.

The final pre-cutoff [Dragonfire revision
14907411](https://oldschool.runescape.wiki/w/Dragonfire?oldid=14907411) establishes 50/30
failed/resisted maximum hits without protection for common chromatic and metallic breath. A
suitable shield caps either result at five. Protect from Magic caps chromatic breath at ten but is
ignored by metallic dragons. Ordinary antifire subtracts 15 from the rolled damage after that cap,
so it produces maximum hits of 35/15 alone and zero with either a shield or, for chromatic breath,
Protect from Magic. Superior antifire alone reduces both families to zero. The reusable resolver
implements exactly this bounded table and post-roll order rather than applying an inert status
flag.

The final pre-cache-233.4 [Goading potion revision
14993925](https://oldschool.runescape.wiki/w/Goading_potion?oldid=14993925) establishes a six-minute
effect in which attackable, otherwise non-aggressive monsters inside the player's 9-by-9 area are
made aggressive. It excludes monsters already aggressive toward another player and monsters above
the player's visible Slayer level. In single-way combat, a replacement becomes aggressive roughly
three seconds after the prior opponent is no longer occupying the player; in multiway combat,
eligible monsters may acquire the player immediately. The same revision explicitly identifies
Jal-Nib and Yt-HurKot healers as unaffected and records that teleporting or drinking a cup of tea
cancels the effect. Its 6 November 2024 change note proves that early expiry has a chat message, but
the literal message is not published in the stable evidence used here.

Cache objects 30137, 30140, 30143 and 30146 pin the four-through-one-dose chain. Cache varbit 11294
is the client-visible `goading_potion_timer`; read-only inspection of the revision-matched RuneLite
timer consumer multiplies it by six, establishing six-cycle display units and the initial value
100 for 600 cycles. The local cache independently pins Jal-Nib 7691 and Yt-HurKot forms 3128, 7701,
7705 and 10624. The implementation admits only NPCs whose visible type exposes the ordinary
`Attack` operation, applies the cache Slayer requirement, preserves an existing interaction, uses
the map-backed multiway area, and routes acquisition through the existing NPC-to-player operation
so ordinary combat validates the attack. The Wiki's “roughly three seconds” is bounded as a
five-cycle single-way scan at the 0.6-second game-cycle cadence; exact phase and multiway fan-out
remain manual observations rather than claimed verified facts. A semantic spell-teleport event now
cancels the effect from every implemented Standard, Lunar, group, Teleother, house, bounty-target
and Home spell teleport while leaving the generic relocation primitive used by doors and stairs
untouched.

The final pre-cutoff [ordinary Cup of tea revision
14943719](https://oldschool.runescape.wiki/w/Cup_of_tea?oldid=14943719), dated 23 July 2025,
disambiguates item 1978 from nettle tea and POH Construction teas. Its `Drink` action heals three
Hitpoints, boosts Attack by `2 + floor(base Attack * 0.02)`, leaves empty cup 1980, forces
`Aaah, nothing like a nice cuppa tea!` overhead and explicitly cancels Goading immediately. Cache
233.4 independently pins item 1978's only inventory operation as `Drink`, empty cup 1980 and
sequence 3702 `human_drink_from_teacup_clay`; any frame sound embedded by that sequence remains a
client concern and no unsupported server synth is emitted. The Wiki's [fast-food delay
reference](https://oldschool.runescape.wiki/w/Food/Fast_foods) and Jagex's 8 March 2023 [Ruinous
Powers delay explanation](https://oldschool.runescape.wiki/w/Update%3AThe_Ruinous_Powers_-_Updated_Greenlight_Poll_Blog)
establish a three-cycle eat delay and a three-cycle addition only when an attack delay is already
active. The shared food-consumption repository implements those independent clocks so later foods
can reuse the policy without coupling Cooking to combat internals.

The Goading change history says that the 6 November 2024 update added a message when the effect
ends early but does not print the literal string. Two contemporaneous player reports from that
production update independently capture `The effects of your Goading potion have worn off.`
([report one](https://www.reddit.com/r/2007scape/comments/1gkwg21), [report
two](https://www.reddit.com/r/2007scape/comments/1gkwjdh)). This lower-authority production
observation is recorded explicitly rather than presented as Wiki prose. Both ordinary-tea and
semantic spell-teleport cancellation now use that message; a generic relocation neither cancels
nor emits it, and drinking tea without an active effect emits no false expiry message. Whether
natural six-minute expiry uses identical wording remains uncaptured and is not guessed.

## Direct Goading-potion consumption

The same final pre-cutoff [Goading potion revision
14993925](https://oldschool.runescape.wiki/w/Goading_potion?oldid=14993925) establishes that the
four-, three-, two- and one-dose objects are members-only consumables and that drinking a dose
starts or refreshes the six-minute effect. The Wiki's
[Potions](https://oldschool.runescape.wiki/w/Potions) timing reference states that a
non-barbarian potion prevents another potion drink for three game cycles, while food remains on
an independent timer and potion drinking does not delay attacks. The direct implementation
therefore owns a separate absolute three-cycle potion clock and publishes the same generic
successful-consumption event used by ordinary tea; the Magic listener applies the existing
Goading lifecycle only after the selected vial has been replaced atomically.

Cache 233.4 pins Goading objects 30137/30140/30143/30146, each with `Drink` as inventory Op1,
ordinary empty vial 229, sequence 829 `human_eat` and setting varbit 5668 `auto_smash_vials`.
Jagex's [Midsummer 2017 update](https://oldschool.runescape.wiki/w/Update%3AMidsummer_2017)
establishes that the toggle destroys the vial after the final potion dose and is unlocked after
Alfred Grimhand's Barcrawl. The cache setting is authoritative at consumption time; its separate
unlock UI is not broadened into this item slice. Read-only inspection of the revision-matched
RuneLite checkout corroborates sequence 829 as `AnimationID.CONSUMING`; it was neither launched
nor modified.

The ordinary-potion message corpus in historical Wiki user revision
[14777106](https://oldschool.runescape.wiki/w/User:Skretzo/Chat_messages?oldid=14777106) records the
stable two-line template: `You drink some of your <name> potion.` followed by `You have N
dose/doses of potion left.` or `You have finished your potion.` The implemented Goading-specific
first line substitutes the cache item name into that template. Because this is a cross-item
template inference rather than a direct cache-233 Goading capture, exact chat text, frame sound
and packet ordering remain manual observations and the feature is not promoted beyond
`integration-tested`.

## Direct ordinary Antipoison consumption

The final pre-cutoff [Antipoison revision
14993758](https://oldschool.runescape.wiki/w/Antipoison?oldid=14993758) establishes that a dose cures
ordinary poison, grants 90 seconds of poison immunity and is free-to-play. It also states that
venom takes two doses: the first reduces venom to equal-damage ordinary poison and the second cures
that poison. The historical Wiki [chat-message revision
14777106](https://oldschool.runescape.wiki/w/User:Skretzo/Chat_messages?oldid=14777106) directly
captures every ordinary Antipoison dose: `You drink some of your antipoison potion.` followed by
the exact three-, two-, one-dose or finished-potion line. Unlike the Goading first line, these
Antipoison strings are direct item observations rather than a cross-item template inference.

Cache 233.4 independently pins the four-through-one-dose identities as 2446/175/177/179, every
inventory Op1 as `Drink`, their free-to-play flags, empty vial 229, consuming sequence 829 and
automatic-vial-smashing varbit 5668. The general
[Potions](https://oldschool.runescape.wiki/w/Potions) timing reference and Jagex's
[Midsummer 2017 update](https://oldschool.runescape.wiki/w/Update%3AMidsummer_2017) establish the
independent three-cycle potion clock and final-vial toggle. Direct Antipoison uses the same shared
clock as direct Goading, does not delay attacks and publishes its effect only after an atomic
selected-slot replacement. Five inventory/cache scenarios plus a cross-module venom regression
cover full inventories, free worlds, all dose messages, both vial-smashing states, cross-family
delay blocking, stale slots, one-dose venom downgrade and second-dose cure into exact varp-102
state -5. Sequence frame sound and inventory/message/varp packet ordering remain matching-client
manual observations, so the group remains `integration-tested`.

## Direct higher poison-remedy consumption

The final pre-cutoff Wiki revisions for
[Superantipoison](https://oldschool.runescape.wiki/w/Superantipoison?oldid=14993800),
[Antidote+](https://oldschool.runescape.wiki/w/Antidote%2B?oldid=14993895),
[Antidote++](https://oldschool.runescape.wiki/w/Antidote%2B%2B?oldid=14993822),
[Anti-venom](https://oldschool.runescape.wiki/w/Anti-venom?oldid=14993833),
[Anti-venom+](https://oldschool.runescape.wiki/w/Anti-venom%2B?oldid=14993872) and
[Extended anti-venom+](https://oldschool.runescape.wiki/w/Extended_anti-venom%2B?oldid=14993931)
establish six members-only four-dose families. Superantipoison gives six minutes of poison
immunity, Antidote+ nine minutes and Antidote++ twelve minutes plus its short venom-protection
window; each needs two doses against venom, with the first converting it to equal-damage poison.
Anti-venom, Anti-venom+ and Extended anti-venom+ cure poison or venom immediately and establish,
respectively, the cutoff ordinary/venom protection windows represented by the existing lifecycle
states -41, -50 and -59. Those effect and duration claims come from the dated item revisions, not
from present-day behavior.

Cache 233.4 independently pins the four-through-one-dose identities as
2448/181/183/185, 5943/5945/5947/5949, 5952/5954/5956/5958,
12905/12907/12909/12911, 12913/12915/12917/12919 and
29824/29827/29830/29833. All 24 are members-only and expose `Drink` as inventory Op1. Their exact
examine stems are `super antipoison potion`, `extra strong antipoison potion`, `super strong
antipoison potion`, `antivenom potion`, `super antivenom potion` and `extended super antivenom
potion`, with the cache's exact four-, three-, two- and one-dose grammar. Empty vial 229, sequence
829 and automatic-vial-smashing varbit 5668 join the same general
[Potions](https://oldschool.runescape.wiki/w/Potions) timer rule and Jagex
[Midsummer 2017 update](https://oldschool.runescape.wiki/w/Update%3AMidsummer_2017) already cited
for the two direct-drinking slices above.

The historical ordinary-potion chat corpus
[revision 14777106](https://oldschool.runescape.wiki/w/User:Skretzo/Chat_messages?oldid=14777106)
supplies the stable `You drink some of your <name> potion.` plus remaining-dose/finished grammar.
No matching cache-233 capture was found for the first five families, so their first lines combine
that stable template with the authoritative cache examine stem and remain bounded inferences. A
later [Extended anti-venom+ revision
15267078](https://oldschool.runescape.wiki/w/Extended_anti-venom%2B?oldid=15267078), dated 18 July
2026, directly records the unusual line `You drink some of your extended super antivenom+
potion`—including `super`, the plus sign and no terminal full stop. That is direct text evidence
but postdates the cache cutoff, so its temporal applicability remains bounded pending a matching-
client capture.

The implementation reuses one standard selected-slot dose engine and the shared three-cycle
potion clock for all 24 objects, including full inventories and both vial-smashing states, and
publishes effects only after atomic replacement. The existing poison repository applies exact
states -20/-30/-40 to the three antipoison families after the documented two-dose venom path and
-41/-50/-59 to the three immediate-cure anti-venoms. Cache contracts, all-dose inventory tests,
free-world rejection, cross-family timing and end-to-end poison/venom state tests are automated.
Because five first lines, all sequence-frame sounds and packet ordering remain observations, the
group remains `integration-tested`.

## Direct Sanfew-serum consumption

The final pre-cutoff [Sanfew serum revision
14993774](https://oldschool.runescape.wiki/w/Sanfew_serum?oldid=14993774) establishes a members-only
four-dose true potion with `Drink`, `Empty` and `Drop` operations. Each dose restores every skill
except Hitpoints by `floor(base * 30 / 100) + 4`; Prayer uses the same formula or 32 percent with
the holy-wrench effect. It combines Super restore, Superantipoison and Relicym's balm behavior:
six minutes of poison immunity, a disease cure and fifteen minutes of disease immunity. The same
dated revision explicitly states that disease immunity is removed on death. Its superantipoison
component preserves the already pinned two-dose venom path rather than inventing an immediate
venom cure.

Cache 233.4 independently pins objects 10925/10927/10929/10931, their members flags, `Drink` as
inventory Op1 and exact descriptions `A 4 dose Sanfew Serum.` through `A 1 dose Sanfew Serum.` The
four-through-one replacements end at vial 229 and use the same sequence 829, varbit 5668 and
general three-cycle potion rule as the preceding direct remedies. The historical chat corpus does
not contain a Sanfew-specific capture. `You drink some of your Sanfew serum.` therefore applies
the stable ordinary-potion template to the cache name and remains a bounded inference; it is not
presented as directly observed text.

Direct consumption reuses the standard atomic selected-slot engine, including a full inventory,
automatic vial smashing, free-world rejection, the shared potion clock and no attack delay. Only
after replacement succeeds, the generic consumable event invokes the same effect implementation
as both Lunar share spells: non-Hitpoints and Prayer restore, poison/venom handling, exact -20
protection state, varp-456 disease cure and 1,500-cycle disease immunity. The standard death
sequence now publishes a composable completion event, rather than competing for its single queue
handler, and the protection listener clears disease immunity from that dated lifecycle point.
Cache, inventory, effect and death-event tests are automated. Exact first-line text, sequence-frame
sound, packet ordering and nonstandard-death/reconstruction behavior remain manual observations,
so the group remains `integration-tested`.

## Direct antifire-potion consumption

The final pre-cutoff revisions for [Antifire potion
14993772](https://oldschool.runescape.wiki/w/Antifire_potion?oldid=14993772), [Extended antifire
14993875](https://oldschool.runescape.wiki/w/Extended_antifire?oldid=14993875), [Super antifire
potion 14993848](https://oldschool.runescape.wiki/w/Super_antifire_potion?oldid=14993848) and
[Extended super antifire
14993908](https://oldschool.runescape.wiki/w/Extended_super_antifire?oldid=14993908) establish four
members-only potion families. Each requires Dragon Slayer I to have been started and applies,
respectively, six, twelve, three and six minutes of protection. Those durations map to the existing
600/1,200/300/600-cycle ordinary/superior repository states and cache varbits 3981/6101 already
pinned by the potion-share antifire slice.

Cache 233.4 independently pins the four-through-one-dose objects as 2452/2454/2456/2458,
11951/11953/11955/11957, 21978/21981/21984/21987 and 22209/22212/22215/22218. All 16 expose
`Drink`, `Empty` and `Drop`, are members-only and have exact family-specific dose descriptions.
Generated symbol `varps.dragonquest` resolves to varp 176. Quest DB row 31 inherits quest table 0
column 18's default not-started state `[0]`, omits an explicit value for that column and sets column
19's completion state to `[10]`; historical production scripts name state 1 as the first spoken-to-
guildmaster step. Direct drinking therefore rejects state 0 and accepts every state from 1 onward.

The exact rejection `You must have started the Dragon Slayer I quest to drink this potion.` appears
in Wiki revision 15071837 dated 29 November 2025, after the cache-233 cutoff. The requirement itself
is present in every pinned pre-cutoff item revision, but the literal failure string is a documented
post-cutoff observation whose stability at 233.4 awaits matching-client capture. A historical
production content reference directly sources ordinary Antifire's first line as `You drink some of
your dragon potion.`. No equivalent direct capture was found for the remaining families, so `You
drink some of your extended antifire potion.`, `You drink some of your super antifire potion.` and
`You drink some of your extended super antifire potion.` remain bounded family-name applications of
the stable ordinary-potion message template.

The implementation registers all 16 Op1 identities through the reusable standard-dose model and
checks membership and quest state before inventory, timer or protection mutation. Successful
drinks atomically follow the selected-slot dose/vial chain in full inventories, honor automatic
vial smashing, play sequence 829, use the independent three-cycle potion clock without delaying an
attack and then publish the generic consumable event. The protection listener applies or resets the
corresponding existing tier and exact duration. Six cache/inventory/access/timing scenarios plus an
end-to-end four-family duration regression are automated. The three inferred first lines, exact
cache-233 quest rejection, sequence-frame sound and inventory/message/varbit packet ordering remain
manual observations, so the group remains `integration-tested`.

## Direct restoration and run-energy potion consumption

The final pre-cache-233.4 revisions for [Restore potion
14993871](https://oldschool.runescape.wiki/w/Restore_potion?oldid=14993871), [Energy potion
14993767](https://oldschool.runescape.wiki/w/Energy_potion?oldid=14993767), [Prayer potion
14993749](https://oldschool.runescape.wiki/w/Prayer_potion?oldid=14993749), [Super energy
14993807](https://oldschool.runescape.wiki/w/Super_energy?oldid=14993807), [Super restore
14993752](https://oldschool.runescape.wiki/w/Super_restore?oldid=14993752) and [Stamina potion
14993745](https://oldschool.runescape.wiki/w/Stamina_potion?oldid=14993745) establish the six direct-
drinking families at the cutoff. Cache 233.4 independently pins their four-through-one-dose object
chains as 2430/127/129/131, 3008/3010/3012/3014, 2434/139/141/143, 3016/3018/3020/3022,
3024/3026/3028/3030 and 12625/12627/12629/12631. Every object exposes `Drink`, `Empty` and `Drop`.
Energy is free-to-play; the remaining five families are members-only. The cache descriptions are
exactly `restore potion`, `energy potion`, `Prayer restore potion`, `super energy potion`, `super
restore potion` and `stamina potion`; notably, Restore's cache examine stem differs from its
observed first drink line.

Restore increases Attack, Strength, Defence, Ranged and Magic by `floor(base * 0.30) + 10`, capped
at base, and does not affect other stats or Prayer. Energy restores 10 percent of maximum run
energy. Prayer restores `floor(base * 0.25) + 7`, while a holy wrench, Prayer cape, Max cape or an
imbued Ring of the gods changes its percentage term to 27. Super energy restores 20 percent of
maximum run energy. Super restore increases every non-Hitpoints stat and Prayer by
`floor(base * 0.25) + 8`, again using 27 percent for Prayer with one of those qualifying items.
Stamina restores 20 percent of maximum run energy and starts or resets exactly 200 game cycles of
70-percent run-drain reduction. Existing cache varbits 25 and 24 expose the active flag and duration
in ten-cycle display units. The Ring of Endurance extension remains outside this core until its
charge lifecycle is implemented and observed.

The local historical production-content corpus directly records `You drink some of your stat
restoration potion.`, `You drink some of your energy potion.`, `You drink some of your restore
prayer potion.` and `You drink some of your super restore potion.`; the Energy, Prayer and Super
restore entries include linked production-video observations, while Restore is retained as a
corpus observation without a linked URL. No direct observation was found for the corresponding
Super energy or Stamina first lines, so `You drink some of your super energy potion.` and `You
drink some of your stamina potion.` are explicitly bounded applications of the stable standard-
potion message template. Dose grammar, sequence 829, vial 229, automatic vial-smashing varbit 5668
and the independent three-cycle potion clock reuse the separately pinned standard-potion evidence.

The Herblore plugin registers all 24 Op1 identities through the reusable standard-dose model.
Successful inventory replacement publishes the generic consumable event, whose shared potion-
effect listener invokes the same formula and Stamina repositories as Lunar Stat Restore Pot Share.
Five cache/inventory/access/timing scenarios cover every family and dose; two end-to-end regressions
cover every immediate effect, Stamina state and each direct Prayer modifier. Exact Super energy and
Stamina first-line captures, sequence-frame sound, packet ordering, Ring of Endurance behavior and
Stamina persistence, death, world-hop and expiry presentation remain manual observations, so the
group remains `integration-tested`.

## Direct ordinary stat-boost potion consumption

The final pre-cache-233.4 revisions for [Attack potion
14993858](https://oldschool.runescape.wiki/w/Attack_potion?oldid=14993858), [Strength potion
14993834](https://oldschool.runescape.wiki/w/Strength_potion?oldid=14993834), [Defence potion
14993883](https://oldschool.runescape.wiki/w/Defence_potion?oldid=14993883), [Ranging potion
14993762](https://oldschool.runescape.wiki/w/Ranging_potion?oldid=14993762), [Magic potion
14993839](https://oldschool.runescape.wiki/w/Magic_potion?oldid=14993839), [Super attack
14993801](https://oldschool.runescape.wiki/w/Super_attack?oldid=14993801), [Super strength
14993795](https://oldschool.runescape.wiki/w/Super_strength?oldid=14993795), [Super defence
14993779](https://oldschool.runescape.wiki/w/Super_defence?oldid=14993779), [Super combat potion
14993756](https://oldschool.runescape.wiki/w/Super_combat_potion?oldid=14993756), [Bastion potion
14993829](https://oldschool.runescape.wiki/w/Bastion_potion?oldid=14993829), [Battlemage potion
14993845](https://oldschool.runescape.wiki/w/Battlemage_potion?oldid=14993845), [Agility potion
14993849](https://oldschool.runescape.wiki/w/Agility_potion?oldid=14993849) and [Fishing potion
14993896](https://oldschool.runescape.wiki/w/Fishing_potion?oldid=14993896) establish the thirteen
ordinary direct-drinking families at the cutoff. Their immediate effects are respectively Attack,
Strength or Defence by `floor(base * 0.10) + 3`; Ranged by `floor(base * 0.10) + 4`; Magic by four;
the three individual super boosts by `floor(base * 0.15) + 5`; all three melee super boosts from
Super combat; Ranged plus Super defence from Bastion; Magic plus Super defence from Battlemage; and
Agility or Fishing by three. The pinned Agility revision describes only the three-level Agility
boost, so direct Agility potion consumption does not restore run energy. Strength is the sole
free-to-play family; the other twelve are members-only.

Cache 233.4 independently pins 52 four-through-one-dose objects: Attack 2428/121/123/125, Strength
113/115/117/119, Defence 2432/133/135/137, Ranging 2444/169/171/173, Magic
3040/3042/3044/3046, Super attack 2436/145/147/149, Super strength 2440/157/159/161, Super defence
2442/163/165/167, Super combat 12695/12697/12699/12701, Bastion 22461/22464/22467/22470,
Battlemage 22449/22452/22455/22458, Agility 3032/3034/3036/3038 and Fishing
2438/151/153/155. Every object exposes `Drink`, `Empty` and `Drop`. Their case-sensitive cache
description stems are `Attack potion`, `Strength potion`, `Defence potion`, `ranging potion`,
`Magic potion`, `super Attack potion`, `super Strength potion`, `super Defence potion`, `super
combat potion`, `bastion potion`, `battlemage potion`, `Agility potion` and `Fishing potion`.

The local historical production-content corpus directly groups ordinary and super Attack under
`You drink some of your attack potion.`, and does the same for the Strength and Defence pairs; its
Attack, Strength and Defence groups link production-video observations. A separate 2024 live
observation independently corroborates the shared Super attack and Super strength wording. The
same historical corpus directly records the Ranging, Magic, Agility and Fishing first lines but
does not link observations for those four. The pinned Bastion and Battlemage revisions directly
record the exact single-message forms `You drink some of your bastion potion. It's a little
tangy.` and `You drink some of your battlemage potion. It's a little tangy.`. No direct Super
combat first-line observation was found, so `You drink some of your super combat potion.` remains
an explicitly bounded stable standard-potion-template inference. Dose grammar, sequence 829, vial
229, automatic vial-smashing varbit 5668 and the independent three-cycle potion clock reuse the
separately pinned standard-potion evidence.

The Herblore plugin registers all 52 Op1 identities through the reusable standard-dose model.
Successful inventory replacement publishes the generic consumable event, whose shared effect
listener resolves these objects to the existing Boost Potion Share families and formula layer.
Five cache/inventory/access/timing scenarios cover every family and dose, while one end-to-end
regression exercises all thirteen immediate effects. Exact Super combat text, sequence-frame
sound, inventory/message/stat packet ordering and ordinary stat-decay/Preserve timing remain
matching-client observations, so the group remains `integration-tested`.

## Direct Combat- and Hunter-potion consumption

The final pre-cache-233.4 [Combat potion revision
14993842](https://oldschool.runescape.wiki/w/Combat_potion?oldid=14993842), dated 25 September 2025,
defines a members-only potion that raises both Attack and Strength by
`floor(base * 0.10) + 3`. The last Hunter-potion revision before the cutoff,
[14790574](https://oldschool.runescape.wiki/w/Hunter_potion?oldid=14790574), defines a members-only
three-level Hunter boost. Both revisions independently pin four dose variants with `Drink`,
`Empty` and `Drop`: Combat 9739/9741/9743/9745 and Hunter 9998/10000/10002/10004 from four doses
through one. Cache 233.4 resolves the same chains, membership flags and case-sensitive description
stems `combat potion` and `Hunter potion`.

Neither cutoff revision records a first-line drink message, and neither family appears in the
local historical production-content message corpus. `You drink some of your combat potion.` and
`You drink some of your hunter potion.` are therefore explicitly bounded family-name applications
of the stable standard-potion template, not direct observations. Dose grammar, sequence 829, vial
229, automatic vial-smashing varbit 5668 and the independent three-cycle potion clock reuse the
separately pinned standard-potion evidence. Boost Potion Share revision 14766606 explicitly lists
both families as incompatible, so their typed effect resolver remains separate from the spell's
accepted family enum.

The Herblore plugin registers all eight Op1 identities through the reusable standard-dose model.
Successful inventory replacement publishes the generic consumable event; the direct-only effect
listener applies the existing stat-boost primitive to Attack and Strength or Hunter. Five cache,
inventory, access and timing scenarios cover every family and dose, while one end-to-end regression
covers both immediate effects and proves their direct-only classification. Exact first-line text,
sequence-frame sound, inventory/message/stat packet ordering and ordinary stat-decay/Preserve
timing remain matching-client observations, so the group remains `integration-tested`.

## Direct Prayer-regeneration potion consumption

The final pre-cutoff [Prayer regeneration potion revision
14993870](https://oldschool.runescape.wiki/w/Prayer_regeneration_potion?oldid=14993870), dated 25
September 2025, pins members-only objects 30125/30128/30131/30134, their four-through-one-dose
ordering, `Drink`/`Empty`/`Drop` operations and case-sensitive descriptions. It defines one Prayer
point every 12 ticks, 66 total restores per dose, and explicitly says the Holy wrench, Prayer cape
and imbued Ring of the gods do not modify the restore. The official [Varlamore: The Rising Darkness
overview](https://oldschool.runescape.wiki/w/Update:Varlamore:_The_Rising_Darkness_-_Overview?oldid=14741713),
dated 7 September 2024, independently gives the same cadence and total and describes the duration
as eight minutes. Sixty-six complete 12-cycle intervals are 792 cycles (475.2 seconds), so the
implementation follows the exact cadence/count while treating “eight minutes” as the published
rounded description.

Cache 233.4 independently resolves those item identities, membership flags, descriptions and
operations; varbit 11361 is named `prayer_regeneration_potion_timer`, while client varc 1200 is
named `buff_prayer_regeneration_start`. The local revision-matched RuneLite source maps the same
varbit as `PRAYER_REGENERATION_POTION_TIMER` and converts its value to a buff duration by
multiplying by 12. This supports storing the number of remaining restoration intervals in the
varbit. The varc's precise write semantics are client-facing and remain manual rather than being
invented server-side.

The official [11 June 2025 Misc Galore & Yama Tweaks
update](https://oldschool.runescape.wiki/w/Update:Misc_Galore_%26_Yama_Tweaks?oldid=14918187)
requires the effect to reset when the player dies or visits a minigame that resets stats. Standard
death now clears the repository and client counter through the shared death-completion event. No
central stat-reset-minigame event exists yet, so those activity-specific cancellations remain an
explicit implementation gap. Resident logout preserves the partial 12-cycle phase, while a cold
reconstruction recovers the remaining grant count from varbit 11361 and conservatively begins a
fresh 12-cycle interval; exact cold-session phase behavior remains capture-bound.

The cutoff item revision explicitly rejects Stat Restore Pot Share. The spell's incompatibility
test now includes object 30125. Neither the item revision, update pages nor local historical message
corpus publishes the first line shown on direct consumption, so `You drink some of your prayer
regeneration potion.` is a bounded standard-potion-template inference. A new timed repository
starts 66 grants on successful direct consumption, restores one point only when below base, consumes
intervals while capped, resets the count and phase provisionally on redose, synchronizes varbit
11361, pauses/resumes resident sessions and clears on standard death. Nine new integration
scenarios cover the cache contract, every dose, access and vial behavior, exact cadence/total/cap,
equipment neutrality, reset, session reconstruction and death; the existing Stat Restore Pot Share
atomic-rejection regression now also covers this family. Exact text, redose behavior,
varc presentation, cold-session phase, nonstandard death/world-hop, minigame resets, frame sound and
packet order remain manual case 59, so the core is `integration-tested`, not verified.

## Direct Saradomin, Zamorak, Ancient and Forgotten brew consumption

The final pre-cutoff [Saradomin brew revision
14993751](https://oldschool.runescape.wiki/w/Saradomin_brew?oldid=14993751), dated 25 September
2025, defines members-only `Drink`/`Empty`/`Drop` doses. Each dose adds
`floor(base Hitpoints * 15 / 100) + 2` Hitpoints and `floor(base Defence * 20 / 100) + 2`
Defence, with both capped at their respective base-plus-formula thresholds. It subtracts
`floor(current * 10 / 100) + 2` from Attack, Strength, Ranged and Magic, so consecutive doses
compound from the newly drained value. The same revision's trivia and the historical
[player-message corpus revision
14777106](https://oldschool.runescape.wiki/w/User:Skretzo/Chat_messages?oldid=14777106) directly
record `You drink some of the foul liquid.` for all four doses.

The final pre-cutoff [Zamorak brew revision
14993780](https://oldschool.runescape.wiki/w/Zamorak_brew?oldid=14993780), also dated 25 September
2025, fixes the Attack boost at `floor(base * 20 / 100) + 2`, Strength at
`floor(base * 12 / 100) + 2`, current-Defence drain at `floor(current * 10 / 100) + 2`,
current-Hitpoints drain at `floor(current * 12 / 100)` with a minimum of one Hitpoint, and Prayer
restoration at `floor(current Prayer / 10)` without overboost. It explicitly rejects Boost Potion
Share. Neither the item revision nor the historical message corpus contains a direct drink line;
the implemented foul-liquid line is a bounded inference supported only by a contemporaneous 2007
player report and remains matching-client case 60.

The final pre-cutoff [Ancient brew revision
14993841](https://oldschool.runescape.wiki/w/Ancient_brew?oldid=14993841), dated 25 September 2025,
defines `floor(base Magic * 5 / 100) + 2`, current-derived 10-percent-plus-2 drains to Attack,
Strength and Defence, and `floor(base Prayer * 10 / 100) + 2` Prayer restoration capped at five
percent over base. The official [Nex Rewards update revision
14253455](https://oldschool.runescape.wiki/w/Update:Nex_Rewards_-_Updated?oldid=14253455)
independently corroborates those release formulas. No direct first-line capture was found, so the
family-name line remains a bounded standard-potion-template inference.

The final pre-cutoff [Forgotten brew revision
14993853](https://oldschool.runescape.wiki/w/Forgotten_brew?oldid=14993853), dated 25 September
2025, defines the stronger `floor(base Magic * 8 / 100) + 3` Magic boost, the same current-derived
10-percent-plus-2 Attack, Strength and Defence drains as Ancient brew, and the same
`floor(base Prayer * 10 / 100) + 2` restoration capped at five percent over base. The official
[Secrets of the North & More! revision
14361577](https://oldschool.runescape.wiki/w/Update:Secrets_of_the_North_%26_More!?oldid=14361577)
and [A Mysterious Quest: Boss Rewards revision
14352390](https://oldschool.runescape.wiki/w/Update:A_Mysterious_Quest:_Boss_Rewards?oldid=14352390)
independently corroborate the release formulas. No direct first-line capture was found, so `You
drink some of your forgotten brew.` remains a bounded standard-potion-template inference.

Cache 233.4 maps Saradomin doses to 6685/6687/6689/6691, Zamorak doses to
2450/189/191/193, Ancient doses to 26340/26342/26344/26346 and Forgotten doses to
27629/27632/27635/27638, with exact case-sensitive examine descriptions and `Drink` in inventory
operation slot one. All are members objects. Typed references cover all sixteen production doses.
The implementation uses the shared atomic selected-slot transition, vial 229, auto-smash varbit
5668, sequence 829 and independent three-cycle potion lock, then applies each immediate effect
through a typed direct-brew resolver. Five generic consumption scenarios cover every dose identity,
and four end-to-end formula scenarios cover access, full-inventory/vial behavior, clock
independence, repeat-dose rounding, caps and floors; the existing atomic failure regressions cover
all four families' rejection by both Lunar potion-share spells. Exact inferred text, frame sound,
packet order and stat-decay timing remain manual case 60, so this slice is `integration-tested`, not
verified.

The same Forgotten brew revision specifies its distinct Herblore upgrade action: add 20 ancient
essence per dose to a matching-dose Ancient brew at boosted level 91, complete after three ticks
and award 36, 72, 108 or 145 XP for one through four doses. Cache 233.4 resolves ancient essence
to object 27616 and the input/output dose pairs to 26346→27638, 26344→27635, 26342→27632 and
26340→27629. The official Boss Rewards revision also pins level 91 and 145 XP for a full four-dose
brew. The implemented held-item handler accepts both item orders, revalidates exact selected slots
after three ticks, consumes 20/40/60/80 stackable essence, replaces the Ancient brew in its own slot
even in a full inventory, awards the exact non-linear XP and participates in shared Herblore queue
cancellation. Members, generic Druidic Ritual training access and boosted level 91 are checked
before scheduling. Exact animation, sound, feedback, automatic-repeat behavior and packet order
were not established by the available textual evidence and remain manual case 60. The evidence-
complete resource/timing/XP core is `integration-tested`, not client-tested or verified.

The final pre-cutoff [Menaphite remedy revision
14993892](https://oldschool.runescape.wiki/w/Menaphite_remedy?oldid=14993892) establishes a distinct
five-minute effect that restores `floor(base * 16 / 100) + 6` every 15 seconds (25 cycles) to
combat stats other than Prayer, never boosts above base, and removes existing divine-potion or
saturated-heart status without removing its visible boost. Cache 233 maps the four doses to
27202/27205/27208/27211, their exact descriptions, members/tradeable flags and `Drink` operation;
sequence 829 is the shared potion animation. The cutoff stat-restoration definition resolves the
otherwise broad wording to Attack, Strength, Defence, Ranged and Magic, excluding Hitpoints and
Prayer. Release-matched local RuneLite names varbit 14448 `MENAPHITE_REMEDY` and documents that it
is set to 20 on consumption and decremented once per 25 ticks with each restoration; its timer
plugin multiplies the value by 25. That independently pins a delayed first pulse, exactly twenty
pulses and 500 total cycles. The same client revision identifies divine timer varbits
8429/8430/8431/8432/8433, combined divine timers 13663/13664/13665 and saturated-heart timer 14704.

The implementation uses the standard protected potion path for every dose, including full-
inventory replacement, optional vial smashing, sequence 829 and the independent three-cycle
potion lock. A typed renewal repository schedules the 20 delayed pulses, applies the exact formula
only when each of the five stats is below base, publishes the remaining-pulse varbit and resets
count and phase on redose. Consumption clears all nine pre-existing divine/saturated timer vars
without changing current boosted levels; a divine timer applied afterward is not cancelled. Both
Lunar potion-share spells explicitly reject the family. Resident-process logout preserves a
partial interval. Because the textual and client evidence does not expose cold-session sub-
interval state, cold login conservatively reconstructs a full 25-cycle interval; standard death
clears the effect using the server's common timed-consumable policy. Exact first drink wording uses
the bounded standard-potion template, while audiovisual/packet presentation, cold phase,
world-hop and nonstandard-death behavior remain matching-client case 60. The renewal core is
therefore `integration-tested`; the complete production divine-effect lifecycle and those client
boundaries remain separately `researched`.

Source NPC attack selection, suitable-shield identity/charging, exact damage messages and rolls,
and KBD, Elvarg, Vorkath, Galvek, drake, wyvern and other boss/special breath variants remain open;
those mechanics are not collapsed into the standard resolver. Cold-reconstruction phase within a
30/20-cycle display bucket and exact death/world-hop cleanup also remain matching-client work.
Goading still requires exhaustive immunity proof, remaining non-spell teleport cancellation,
cold-reconstruction/death policy, natural-expiry wording and matching-client timing. Any other
cutoff candidate remains positively unimplemented rather than receiving inert state.

The same pinned Poison revision defines recurring player poison as `ceil(severity / 5)` damage on
each 30-cycle interval, with severity reduced by one after each hit until it reaches zero. It also
states that both poison damage and antipoison countdown stop while a modal interface is open, the
overdue hit occurs immediately after closure and subsequent hits use fresh 30-cycle intervals. The
pinned Venom revision defines the sequence 6, 8, 10, 12, 14, 16, 18 and 20 damage, with permanent
20-damage hits thereafter. The implementation now applies those formulas through the standard
instant-hit/death queue with cache poison and venom hitsplats, handles interface deferral, resumes
persisted non-zero varp state on login and exposes immunity-aware clean-target poison and standard
venom infliction seams. Thirteen Boost Potion Share scenarios cover these lifecycle boundaries.

Source-specific NPC, poisoned-weapon, smoke-spell and special-attack producers are not yet wired to
the clean-target seams. The cutoff Poison revision says weapon poison can be reapplied but does not
resolve one universal weak-versus-strong replacement rule for every producer, so reapplication
remains deliberately source-specific instead of guessed. NPC poison/venom immunity, Serpentine
helm behavior, varp-3777/varbit-6251 secondary semantics, toxin death cleanup and disease
infliction also remain open. Sanfew disease immunity now clears through the standard death
completion event; nonstandard death and reconstruction policy remain open.
The same unresolved recipient priority, zero-recipient feedback, account-mode exception,
presentation height, sound, literal messages, action races and packet ordering apply here. The
three-cycle lock, height-96 graphic, deterministic player-list priority and exact rectangle-based
Castle Wars check are bounded pending matching-client case 58. These cores are therefore
`integration-tested`, not client-tested or verified; the full spell remains incomplete.

## Lunar Cure Me

The final pre-cutoff [Cure Me revision
14899448](https://oldschool.runescape.wiki/w/Cure_Me?oldid=14899448), dated 13 May 2025, defines a
members-only level-71 Lunar combat spell unlocked by Lunar Diplomacy. It consumes two astral, two
cosmic and one law rune, awards 69 Magic XP, cures ordinary poison without granting poison
immunity and downgrades venom to ordinary poison. The final pre-cutoff [Antipoison revision
14993758](https://oldschool.runescape.wiki/w/Antipoison?oldid=14993758) independently establishes
that an ordinary poison cure removes poison while the first of two venom cures only converts
venom to poison. The [Venom revision
14984136](https://oldschool.runescape.wiki/w/Venom?oldid=14984136) states that a poison-curing
method converts venom to ordinary poison at the same damage. Jagex's [24 May 2023 Bounty Hunter
update](https://secure.runescape.com/m=news/bounty-hunter-is-back?oldschool=1) is the primary dated
source for the once-per-game-tick cast cap.

Cache 233.4 resolves spell object 9113 (`71_cure_me`), Lunar component 218:113 with direct
operation `Cast`, combat classification, exact requirements, sequence 4411
(`quest_lunar_pushing_magic_animation`), graphic 742 (`quest_lunar_cure_me_spot`), synth 2884 and
Lunar Diplomacy varbit 2448. The contemporaneous sound-file [revision
14890793](https://oldschool.runescape.wiki/w/File:Cure_Me.ogg?oldid=14890793) identifies synth 2884.
The local revision-matched RuneLite checkout was inspected without launch and independently
corroborates component 218:113, graphic 742 and poison varp 102.

RuneLite's documented [poison var contract at the inspected
revision](https://github.com/runelite/runelite/blob/d14c12197c80f76a2833c7f744cacec896096d76/runelite-api/src/main/java/net/runelite/api/VarPlayer.java)
defines ordinary poison damage as `ceil(state / 5)` for states 1 through 100 and venom damage as
`min(20, (state - 999997) * 2)` at states 1,000,000 and above. Combining that primary client
contract with the Wiki's same-damage wording yields the deterministic conversion
`poison_state = venom_damage * 5`. This formula maps venom's 6, 8, 10, 12, 14, 16, 18 and capped
20 next-damage stages to ordinary poison states 30 through 100. This is an explicitly documented
formula inference pending production packet capture, not an unsourced claim about Jagex server
internals.

Production removes positive sub-million poison state by writing zero, which grants no immunity,
and downgrades venom through the same-damage conversion. Clean state zero and negative immunity
states no-op without consuming resources or awarding XP. Successful casts emit sequence 4411,
graphic 742 at height 96 and synth 2884, then apply the official one-tick cap; same-tick repeats
send `You can't activate this just yet.` Cache classification does not route Cure Me into the
offensive combat registry. LMS fake-rune and Fountain behavior remain centralized in the shared
rune manager, with Fountain casts suppressing XP under the repository-wide policy.

Cache varbit 6251 (`poison_type`) is backed by varp 3777 (`poison_2`), but neither the cutoff Wiki
nor the inspected RuneLite client defines those fields as part of Cure Me. The implementation
therefore preserves both while changing authoritative player poison varp 102. Exact
varp-3777/varbit-6251 lifecycle semantics, clean direct-component behavior, any success message,
poison-cycle rescheduling, cross-action cooldown interaction, graphic slot/delay and exact
audiovisual/var/rune/XP packet order remain isolated in manual case 46. The core is
`integration-tested`, not client-tested or verified.

## Lunar Superglass Make evidence conflict

The final pre-cutoff [Superglass Make revision
14990136](https://oldschool.runescape.wiki/w/Superglass_Make?oldid=14990136) pins the level, rune,
XP, inventory-wide input and no-furnace behavior and reports approximately 1.3 molten glass per
sand with ordinary seaweed and up to 1.6 with giant seaweed when ground glass is not collected.
However, the contemporaneous [Giant seaweed revision
14940172](https://oldschool.runescape.wiki/w/Giant_seaweed?oldid=14940172) and [Molten glass
revision 14974481](https://oldschool.runescape.wiki/w/Molten_glass?oldid=14974481) publish
incompatible exact no-pickup averages. Those values cannot all describe one random-yield
distribution. Because the production goal requires exact cache-233 behavior, the spell remains
unimplemented until a matching-client statistical capture, deobfuscated script or authoritative
dated source resolves the probability table. The conflict is recorded instead of selecting a
convenient approximation.

## Lunar Vengeance Other and Vengeance rebound

The final pre-cutoff [Vengeance Other revision
14938415](https://oldschool.runescape.wiki/w/Vengeance_Other?oldid=14938415), dated 16 July 2025,
defines a members-only level-93 Lunar combat spell unlocked by Lunar Diplomacy. It costs three
astral, ten earth and two death runes, awards 108 Magic XP, requires the recipient's Accept Aid
setting and gives that player the next-hit Vengeance effect. Its infobox records zero-tick speed and
a 50-tick/30-second cooldown shared with Vengeance. It cannot be cast inside a player-owned house or
in single-combat Wilderness. The exact recipient message is
`You have the power of vengeance!`. Jagex's [31 July 2024 quality-of-life
update](https://oldschool.runescape.wiki/w/Update:QoL_Improvements_%26_Further_Deadman:_Armageddon_Tweaks)
independently added Vengeance Other support to the blighted Vengeance sack.

The final pre-cutoff [Vengeance revision
14996801](https://oldschool.runescape.wiki/w/Vengeance?oldid=14996801), dated 29 September 2025,
defines the shared armed effect: the next damaging sourced hit reflects 75%, rounded down, with a
minimum of one, while the recipient still takes the full incoming damage and says
`Taste vengeance!`. It explicitly excludes poison, venom and disease damage, removes the effect on
logout and POH entry, stacks with a ring of recoil and does not make reflected damage count for
Smite. The cutoff [recoil-immune template revision
14902396](https://oldschool.runescape.wiki/w/Template:Recoil_immune?oldid=14902396), dated 16 May
2025, lists Callisto, Champion of Scabaras, Fragment of Seren, Galvek, Hespori, Judge of Yama,
Menaphite Akh, Nex, The Nightmare, Phosani's Nightmare, Ranis Drakan, Skotizo, Slug Prince, Undead
tree, Vanstrom Klause, Venenatis, Vet'ion, Vorkath, Wighted Leech and Xamphur. Those NPCs neither
receive reflected damage nor consume the armed effect. The current implementation matches resolved
visible names; a future typed morph audit must prove every cache identity.

Cache 233.4 resolves Vengeance Other object 9135 (`93_vengeance_other`), component 218:141,
sequence 4411 (`quest_lunar_pushing_magic_animation`), recipient graphic 725 and synth 2908
(`lunar_vengence_other`). It also exposes active/cooldown varbits 2450/2451. Self Vengeance is object
9136 and component 218:142; the cache provides no-stalling/stalling sequences 8316/8317, graphic 726
and synth 2907 (`lunar_vengence`). The local revision-matched RuneLite checkout was inspected
read-only and corroborates the component, objects, varbits, graphics and sequences; it was neither
launched nor modified. Jagex's [6 June 2019 PvP update](https://oldschool.runescape.wiki/w/Update:PvP_Changes_and_Deadman_Beta)
documents the no-stalling Vengeance animation outside PvP and retained stalling behavior inside
PvP.

The same component's cache operation array places `Cast` at Op1 and `Animation` at Op10. Cache
interface 525 (`animation_picker`) uses clickable dynamic component 525:8 and three-integer
clientscript 7707. DB row 4094/table 83 is titled `Vengeance Animations` and orders `Default`
(display object 956) before `League IV` (unlock object 28696). Varp 4559 identifies the selected
toggle list, varp 4562 stores enabled Vengeance-animation bits, varbit 17316 stores the active
override index and varbit 17327 records the League IV unlock. Cache scripts 7755/7759 constrain the
generic picker to the Vengeance row and allow the default bit plus the League bit only when the
unlock varbit is active. A later-revision [open implementation capture](https://clockworkrs.com/docs/research/minigame-teleport-grouping)
independently corroborates the generic protocol shape—clientscript 7707 receives enabled mask,
unlocked mask and DB row before the modal receives Op1 events—but is supporting evidence only, not
the revision authority.

The official [Trailblazer Reloaded rewards update](https://oldschool.runescape.wiki/w/Update:Leagues_IV:_Trailblazer_Reloaded_-_Rewards),
the cutoff Vengeance page and historical [reward-scroll revision
14535911](https://oldschool.runescape.wiki/w/Trailblazer_reloaded_vengeance_scroll?oldid=14535911),
dated 18 January 2024, establish that object 28696 is a members-only, tradeable, non-stackable
consumable with a `Read` operation. Reading it permanently exposes the League-IV Vengeance option,
which must still be enabled through the spell's Animation menu and does not affect Vengeance Other.
The page's contemporaneous [Jagex-licensed 5.9-second capture revision
14535905](https://oldschool.runescape.wiki/w/File:Reading_Trailblazer_Reloaded_reward_scroll.mp4?oldid=14535905)
identifies a shared reading animation across the four Trailblazer Reloaded reward scrolls. Cache
233.4 resolves that animation as sequence 10625, the alternate player sequence as 10626, graphic
2605, sound 7569 and unlock varbit 17327. The dated [Trailblazer Vengeance sound
file](https://oldschool.runescape.wiki/w/File:Vengence_(Trailblazer_Reloaded_League).wav?oldid=14917394)
corroborates the alternate sound. The implemented held-item route atomically consumes one scroll,
sets varbit 17327, plays sequence 10625 and intentionally leaves the enabled-animation mask on its
prior/default selection. Non-members and already-unlocked accounts retain the scroll. Exact
rejection text and persistence through the repository's future reconstructed-account path are not
published by these sources and remain capture-bound instead of guessed.

Production now routes Vengeance Other as a non-offensive range-eight player spell and enforces all
documented access, target, POH-presence and Wilderness rules before mutation. Success uses the
shared rune manager, including the official blighted sack substitute, awards XP outside the
Fountain, emits the cache presentation, arms varbit 2450 and starts an exact 50-cycle caster
cooldown mirrored through varbit 2451. A new post-application hit event exposes actual Hitpoints
removed. The rebound consumes on the first positive player- or non-immune-NPC-sourced event and
applies `max(1, damage * 3 / 4)` as typeless damage without recursively entering the standard
queued-hit path for a player source. NPC rebound uses the engine's minimum legal one-cycle queue.
Unsourced environmental damage and zero damage retain the effect; this covers poison, venom and
disease in the current hit model because those lack a combat source.

Eleven Vengeance Other scenarios and twelve self/picker/scroll scenarios cover exact
spell/component/assets/varbits/formula contracts, ordinary
and blighted casts, all access/target/area gates, exact 49/50-cycle cooldown behavior, range-eight
routing, player and NPC rebound, minimum-one and lethal-overkill rounding from actual damage,
environmental/zero exclusions, a representative recoil-immune NPC, speech and logout cleanup.
Exact caster cooldown text is a bounded community/legacy-server inference because neither the
cutoff Wiki nor cache publishes it. Already-active, POH and Wilderness rejection text; graphic
height 96; synth audience; rebound impact cycle; ring-of-recoil, Smite and simultaneous-hit order;
typed immunity morph coverage; active-effect POH-entry removal; reconstructed-session cooldown
persistence and packet order remain manual case 52. Self Vengeance additionally covers ordinary,
LMS and blighted costs; every access/state gate; single-way Wilderness; PvP animation selection;
Fountain policy; both cross-spell cooldown directions; native picker packets and masks; locked,
unlocked and last-choice toggles; the complete League IV presentation; exact reward-scroll cache
properties; held-operation routing; consumption; unlock-without-auto-enable; and members/duplicate
atomicity. The cache exposes
`pvp_area_client` varbit 8121, but no production updater currently populates it, so Wilderness is a
safe fallback while non-Wilderness PvP-area classification remains explicit follow-up work. The
implemented spell, picker and rebound cores are `integration-tested`, not client-tested or
verified.

## Cache-233 Surge potion consumption and cooldown

The final pre-cutoff [Surge potion revision
14993912](https://oldschool.runescape.wiki/w/Surge_potion?oldid=14993912), dated 25 September 2025,
defines a members-only, untradeable potion whose four doses restore 25 percent of the special-
attack bar. It may be consumed only once every five minutes, rejects a drink at 100 percent with
`Drinking this would have no effect.`, and reports an active cooldown with `You are still full of
adrenaline. You'll be able to drink in [time remaining].` On natural expiry it sends `You now feel
capable of drinking another dose of surge potion.` in the documented red chat colour.

The contemporary official [Varlamore: The Final Dawn - Out Now revision
14902205](https://oldschool.runescape.wiki/w/Update:Varlamore:_The_Final_Dawn_-_Out_Now?oldid=14902205)
independently pins the four-dose release item, 25-percent restoration and five-minute cooldown. It
also specifies resident-session persistence: logging out with three minutes remaining requires
three further logged-in minutes after return. That rules out wall-clock expiry and supports a
persisted cycle count whose timer is paused offline. The same update names ornate-pool use,
leaving a raid or the Colosseum, and death as resets, and names between-wave Colosseum/Inferno and
between-room Theatre of Blood states as pauses.

The official [29 May 2025 Bounty Hunter update revision
14910043](https://oldschool.runescape.wiki/w/Update:Bounty_Hunter_Changes,_Collection_Log_Updates_%26_Emote_Improvements?oldid=14910043)
adds the PvP restriction: restoration occurs after six game ticks, and is discarded if the player
moves more than 15 tiles from the drinking location. The cutoff item revision further enumerates
the activity policy. The timer pauses between Colosseum and Inferno waves, Theatre of Blood rooms
and Doom levels; it resets on death, raid or Colosseum entry/exit, the Ferox Enclave free-for-all
portal and any player-owned-house pool. Fight Caves waves, Tombs of Amascut rooms, Theatre/Tombs
death and the Elidinis Statuette are expressly not equivalent reset or pause paths. Those rules are
kept as a separate researched feature until their owning world systems expose authoritative phase,
entry, exit, pool and special-death events.

Cache 233.4 resolves four-, three-, two- and one-dose objects to 30875/30878/30881/30884. Each has
the exact `N dose(s) of surge potion.` description and `Drink` in inventory operation slot one;
the terminal container is vial 229. Sequence 829 is the shared consumption animation. Spotanim
3232 (`vfx_surge_potion_01`) uses sequence 12094. Varbit 16270 is the Surge timer. Read-only
inspection of the sibling revision-matched RuneLite source maps that varbit to
`SURGE_POTION_TIMER`, maps varclient 1258 to `BUFF_SURGE_POTION_START`, displays the four-dose icon,
and multiplies the varbit value by ten game cycles. Therefore the five-minute/500-cycle cooldown
is transmitted as 50 ten-cycle units. RuneLite was neither launched nor modified.

Production now registers all four doses through an atomic selected-slot 4→3→2→1→vial path with
automatic vial smashing, members gating, sequence 829 and the independent three-cycle potion
lock. Non-PvP drinking immediately adds 250 hundredth-percent special units, capped at 1,000, and
plays graphic 3232. Wilderness varbit 5963, PvP-area varbit 8121 or a PvP world flag selects a soft-
queued six-cycle restore; the queued effect succeeds at Chebyshev distance 15 or less and is
discarded at 16 or more. A permanent server-only varp stores the 500-cycle cooldown, login resumes
it, logout pauses it, standard death clears it, and varbit 16270 is synchronized with ceiling
division into ten-cycle units. Both Lunar potion-share resolvers explicitly reject the family.

The first drink line `You drink some of your surge potion.` is a bounded standard-potion-template
inference. The source publishes the cooldown sentence template but not the placeholder's exact
minutes/seconds grammar or partial-second rounding; the current rendering uses rounded-up game
seconds and conventional singular/plural wording. No cutoff source found states whether logout
during the six-tick PvP delay pauses, cancels or later resumes the pending restoration, so the
implementation conservatively cancels that non-persisted queue. The exact second-graphic packet,
sequence-frame sound, varclient start behavior and full inventory/message/var/stat packet order
remain matching-client case 61. These evidence qualifications keep the direct core
`integration-tested`, not verified.

## Cache-233 Barbarian-mix production

The final pre-cutoff [Barbarian Training revision
14988720](https://oldschool.runescape.wiki/w/Barbarian_Training?oldid=14988720) publishes the full
29-recipe table. Roe or caviar can complete Attack (level 4, 8 XP), Antipoison (6, 12), Relicym's
balm (9, 14), Strength (14, 17) and Restore (24, 21). The remaining recipes require caviar:
Energy (29, 23), Defence (33, 25), Agility (37, 27), Combat (40, 28), Prayer (42, 29), Super
attack (47, 33), Anti-poison supermix (51, 35), Fishing (53, 38), Super energy (56, 39), Hunting
(58, 40), Super strength (59, 42), Magic essence (61, 43), Super restore (67, 48), Super defence
(71, 50), Antidote+ (74, 52), Antifire (75, 53), Ranging (80, 54), Magic (83, 57), Zamorak
(85, 58), Stamina (86, 60), Extended antifire (91, 61), Ancient (92, 63), Super antifire
(98, 70) and Extended super antifire (99, 78). It also records that Make-All support was added on
8 May 2024.

Three final pre-cutoff item revisions independently bound timing across the range. [Attack mix
revision 14993915](https://oldschool.runescape.wiki/w/Attack_mix?oldid=14993915), [Ancient mix
revision 14993988](https://oldschool.runescape.wiki/w/Ancient_mix?oldid=14993988) and [Extended
super antifire mix revision
14993983](https://oldschool.runescape.wiki/w/Extended_super_antifire_mix?oldid=14993983) each
specify one tick per mixture and a two-dose potion input. Cache 233.4 pins roe 11324, caviar
11326, all 29 potion inputs and all 29 two-dose outputs. It also supplies the native skill-multi
interface 270, setup clientscript 2046 and vial-mixing sequence 363.

The cutoff-era [Quest Helper Barbarian Training implementation at commit
3f0992a](https://github.com/Zoinkwiz/quest-helper/blob/3f0992a8dee4e3a4feeb2a87051997a672702bfa/src/main/java/com/questhelper/helpers/miniquests/barbariantraining/BarbarianTraining.java)
pins the exact normal-chat success line `You combine your potion with the fish eggs.` and the
first-training message-box text `You feel you have learned more of barbarian ways. Otto might wish
to talk to you more.` Cache 233.4 still exposes `brut_herb_potion` varbit 3761. The implementation
uses its established tasked/made/complete values 1/2/3 so the first Attack mix advances 1→2 and
other recipes require 3. Quest Helper had already migrated its own journal tracking to local
configuration by the pinned commit, so production capture must still prove whether the live server
continued to update varbit 3761 or used a different persistent source.

Production registers both held-use orders, validates members, Druidic Ritual, boosted level,
training state and the correct egg before showing the one-product Make-All flow, then atomically
consumes one two-dose potion and one egg per tick and awards the table XP. It revalidates each
iteration, supports a full inventory because two inputs become one output, and shares the Herblore
queue-cancellation family. Consumption is tracked separately below. Production-equipment modifier
behavior, sound and full interface/message/inventory/XP packet order remain matching-client case
62, so this slice remains `integration-tested` rather than verified.

## Cache-233 Barbarian-mix consumption

The final pre-cutoff revisions for all 29 mixes are Attack 14993915, Antipoison 14993972,
Relicym's 14993975, Strength 14993898, Restore 14993968, Energy 14993974, Defence 14993913,
Agility 14993954, Combat 14993916, Prayer 14993952, Superattack 14993906, Anti-poison supermix
14993951, Fishing 14993960, Super energy 14993969, Hunting 14993971, Super strength 14993894,
Magic essence 14993917, Super restore 14993965, Super defence 14993948, Antidote+ 14993966,
Antifire 14993959, Ranging 14993899, Magic 14993956, Zamorak 14993963, Stamina 14993946,
Extended antifire 14993964, Ancient 14993988, Super antifire 14993976 and Extended super
antifire 14993983. Together with [Barbarian Training revision
14988720](https://oldschool.runescape.wiki/w/Barbarian_Training?oldid=14988720), they establish
both doses, the inherited ordinary-potion effects, empty-vial completion, and the exact heal table:
Attack, Antipoison, Relicym's, Strength, Restore, Energy and Combat heal 3; every other mix heals
6. [Zamorak mix revision
14993963](https://oldschool.runescape.wiki/w/Zamorak_mix?oldid=14993963) is exceptional: it deals
`floor((current Hitpoints - 1) / 8)` damage, then heals 6 later in the same game tick. [Relicym's
mix revision 14993975](https://oldschool.runescape.wiki/w/Relicym%27s_mix?oldid=14993975)
explicitly says the dose cures disease, so the implementation clears disease rather than inferring
an undocumented severity decrement.

The dated Potions and Food timing descriptions distinguish ordinary potions' independent
three-cycle consumption clock from Barbarian mixes' food clock. Standard food adds three cycles to
an attack delay only when one is already active. The implementation therefore reuses the shared
food repository with three-cycle eat and attack values; it does not route mixes through the
ordinary potion repository. Cache 233.4 independently pins all 58 members objects, their `Drink`
operation, sequence 829, empty vial 229, poison varp 102, disease varp 456 and the antifire/stamina
display varbits.

Read-only inspection of the sibling RuneLite release-matched commit d14c1219 corroborates all
ordinary stat, restore, run-energy, protection and heal mappings in `ItemStatChanges.MixedPotion`.
Its Zamorak predictor and 2022 regression still model a 12-percent independent Hitpoints effect,
which conflicts with the final pre-cutoff Wiki formula above. Because this project explicitly uses
the final dated Wiki behavior as the gameplay authority, the server implements the Wiki's ordered
one-eighth damage then six-point heal and retains the RuneLite result only as a documented manual
comparison. Exact lumpy-message punctuation is also inconsistent across the dated pages; the
implementation uses `You drink the lumpy potion.` while case 62 must capture the matching client's
channel and punctuation. Sound, animation-frame and full inventory/message/stat packet order remain
unverified, so consumption is `integration-tested`, not verified.

## Cache-233 Stamina-potion production

The final pre-cutoff [Stamina potion revision
14993745](https://oldschool.runescape.wiki/w/Stamina_potion?oldid=14993745), dated 25 September
2025, defines all four members-only recipes at Herblore level 77. Super energy(1)/(2)/(3)/(4)
requires exactly one amylase crystal per dose, takes two game ticks, produces the same-numbered
Stamina dose and awards 25.5 XP per dose: 25.5/51/76.5/102 XP. The final pre-cutoff [Amylase
crystal revision 14918518](https://oldschool.runescape.wiki/w/Amylase_crystal?oldid=14918518)
independently identifies the stackable secondary and repeats the one-crystal and 25.5-XP-per-dose
contract. It also explicitly permits the amulet of chemistry or alchemist's amulet when the Super
energy input contains fewer than four doses.

The cutoff [amulet of chemistry revision
14857005](https://oldschool.runescape.wiki/w/Amulet_of_chemistry?oldid=14857005), [alchemist's
amulet revision 14950137](https://oldschool.runescape.wiki/w/Alchemist%27s_amulet?oldid=14950137)
and [prescription goggles revision
14988816](https://oldschool.runescape.wiki/w/Prescription_goggles?oldid=14988816) supply the shared
production rules. A successful amulet roll upgrades only a sub-four-dose input by one dose, spends
no extra secondary and awards no extra XP; a four-dose input cannot roll or consume a charge. One
successful goggles roll applies per potion and preserves the secondary quantity for that potion,
so a Stamina result retains the complete one-, two-, three- or four-crystal bundle rather than one
crystal. The two equipment rolls remain independent.

Jagex's official [Run Energy Changes update](https://secure.runescape.com/m=news/run-energy-changes?oldschool=1),
preserved in the cutoff-era [Wiki update revision
14833541](https://oldschool.runescape.wiki/w/Update:Run_Energy_Changes?oldid=14833541), states that
from 8 January 2025 a player can start the Make-X process for Stamina, Anti-venom and Antifire
potions without cancelling existing movement. Production therefore does not clear a pending route
when starting the queue. The automated boundary proves service-level route preservation; exact
end-to-end inventory interaction while the matching client is moving remains manual case 67.

Cache 233.4 independently resolves Super energy doses to 3022/3020/3018/3016, Stamina doses to
12631/12629/12627/12625, stackable members amylase to 12640, the skill-multi interface to 270,
setup clientscript to 2046 and vial-mixing sequence to 363. Production uses the existing typed
dose-scaled recipe service and Herblore weak queue 103. All four symmetric held-use registrations,
complete-bundle menu limits, exact two-tick atomic results, boosted/access gates, stale-resource
stops, full inventories, shared cancellation, equipment composition and pending-route preservation
are integration-tested. Exact messages, sounds, animation restart, output-slot and packet order,
plus the client-to-server moving-interaction path, remain researched rather than inferred.

## Cache-233 antifire-upgrade production

The final pre-cutoff [Extended antifire revision
14993875](https://oldschool.runescape.wiki/w/Extended_antifire?oldid=14993875) defines four
members-only, boostable level-84 recipes. Antifire(1)/(2)/(3)/(4) consumes exactly one/two/three/
four lava scale shards, takes two ticks, keeps the dose count and awards 27.5 XP per dose:
27.5/55/82.5/110. It does not list Dragon Slayer II or the superior-antifire unlock as a creation
requirement. Its January 2025 change note applies Jagex's moving Make-X behavior to Antifire
production.

The final pre-cutoff [Super antifire potion revision
14993848](https://oldschool.runescape.wiki/w/Super_antifire_potion?oldid=14993848) defines one
boostable level-92, two-tick route from Antifire(4) plus one crushed superior dragon bones to Super
antifire(4), awarding 130 XP. The final pre-cutoff [Extended super antifire revision
14993908](https://oldschool.runescape.wiki/w/Extended_super_antifire?oldid=14993908) defines five
boostable level-98, two-tick routes: each Super antifire dose consumes the same number of lava
shards for 40 XP per dose, while Extended antifire(4) plus one crushed superior dragon bones makes
Extended super antifire(4) for 180 XP. Both superior pages require Dragon Slayer II completion and
say the process must first be learned from Primula in the Myths' Guild. Cache 233.4 provides the
durable `super_antifire_unlocked` varbit 6140, which is the production gate; Primula's dialogue and
the exact unlock mutation remain a separate world/client capture rather than a fabricated script.

The cutoff [amulet of chemistry revision
14857005](https://oldschool.runescape.wiki/w/Amulet_of_chemistry?oldid=14857005) and [alchemist's
amulet revision 14950137](https://oldschool.runescape.wiki/w/Alchemist%27s_amulet?oldid=14950137)
explicitly include Extended antifire and Extended super antifire among variable-dose recipes: only
one-/two-/three-dose inputs can receive one bonus output dose, without another secondary or extra
XP. They explicitly exclude always-four-dose Super antifire. The cutoff [prescription goggles
revision 14988816](https://oldschool.runescape.wiki/w/Prescription_goggles?oldid=14988816) applies
one preservation roll per potion and, for stackable secondaries, saves the number used for all
input doses. It has no antifire exception, so a successful roll retains the complete lava-shard
bundle or the single non-stackable crushed-bone secondary. Equipment rolls remain independent.

Cache 233.4 resolves the Antifire inputs to 2458/2456/2454/2452, Extended antifire outputs to
11957/11955/11953/11951, lava shards to stackable object 11994, crushed superior dragon bones to
non-stackable object 21975, Super antifire doses to 21987/21984/21981/21978 and Extended super
antifire doses to 22218/22215/22212/22209. It independently pins interface 270, setup clientscript
2046 and sequence 363. The ten recipes use the existing typed dose-scaled service and weak queue
103, including symmetric dispatch, complete-bundle quantity limits, per-result revalidation,
atomic full-inventory handling, shared cancellation, equipment composition and preservation of a
pending route. The unlock rejection sentence is a bounded implementation phrase because no cutoff
source inspected publishes it. Exact Primula acquisition, ordinary messages, audiovisuals,
one-input presentation, output-slot/packet order and end-to-end moving interaction remain manual
case 68; the gameplay core is integration-tested, not verified.

## Cache-233 ordinary small-net and bait Fishing

The final pre-cutoff [Fishing revision
14980550](https://oldschool.runescape.wiki/w/Fishing?oldid=14980550), [small-net/bait spot revision
14993003](https://oldschool.runescape.wiki/w/Fishing_spot_(small_net,_bait)?oldid=14993003) and
[Fishing-spots revision
14968357](https://oldschool.runescape.wiki/w/Fishing_spots?oldid=14968357) establish the ordinary
interaction family. Standard Fishing makes a success roll every five ticks. These spots expose a
small-net/net action and a bait action, move after a random 250–530 ticks, and use sea- or
river-dependent catch availability. The separate cutoff [tick-manipulation revision
14986181](https://oldschool.runescape.wiki/w/Tick_manipulation?oldid=14986181) confirms that player
timing can shorten ordinary cycles, so that behavior is retained as an explicit follow-up rather
than silently folded into the standard five-tick loop.

The pinned [raw anchovies revision
14935360](https://oldschool.runescape.wiki/w/Raw_anchovies?oldid=14935360) and [raw shrimps revision
14935359](https://oldschool.runescape.wiki/w/Raw_shrimps?oldid=14935359) define a cascading small-net
roll: at boosted level 15, anchovies roll first at endpoints 24/128 for 40 XP; shrimp is the level-1
fallback at 48/256 for 10 XP. Their post-27-November-2024 success messages are exactly `You catch
some raw anchovies.` and `You catch some raw shrimps.` The pinned [raw herring revision
14882830](https://oldschool.runescape.wiki/w/Raw_herring?oldid=14882830) and [raw sardine revision
14957879](https://oldschool.runescape.wiki/w/Raw_sardine?oldid=14957879) define the bait cascade:
level-10 herring first at 24/128 for 30 XP, then level-5 sardine at 32/192 for 20 XP. Their exact
current messages are `You catch a raw herring.` and `You catch a raw sardine.` One fishing bait is
consumed only when a fish is caught. The cutoff small-net, fishing-rod and fishing-bait revisions
14997126/14944202/14944253 pin the corresponding equipment, including the cosmetic pearl rod as a
carried or worn equivalent.

Cache 233.4 independently resolves 21 participating spot NPCs
1514/1517/1518/1521/1523/1524/1525/1528/1530/1544/3913/7155/7459/7462/7467/7469/7947/14038/
14040/14041/14524. Their first operation is `Small Net`, except NPC 1530's `Net`; their second is
empty and their third is `Bait`; all examine as `I can see fish swimming in the water.` It resolves
small net 303, rod 307, bait 313, shrimp 317, anchovies 321, sardine 327, herring 345, pearl rod
22846 and sequences 621 `human_smallnet` and 622 `human_fishing_casting`.

The implementation follows the existing RSMod plugin structure with typed cache references,
direct operation registration, protected suspending actions and one inventory transaction per
successful catch. It revalidates distance, boosted level, equipment, bait and output capacity after
every delay. Failure consumes no bait; a full inventory containing exactly one bait succeeds
because its atomic removal frees the output slot, while a larger bait stack remains unchanged when
the fish cannot fit. Nine integration scenarios cover the complete cache contract, both ordered
roll chains, five-tick timing, exact XP/messages, failure/repeat behavior, worn pearl rod,
full-inventory atomicity and mid-action invalidation. Relocation topology, river/sea catch filtering,
tick manipulation and exact client presentation remain researched under manual case 69.

## Cache-233 lure/bait Fishing

The final pre-cutoff [lure/bait spot revision
14993394](https://oldschool.runescape.wiki/w/Rod_Fishing_spot_(lure,_bait)?oldid=14993394)
defines the ordinary river family and its 26 NPC identities. Cache 233.4 independently resolves
all 26 `freshfish` symbols and proves that `Lure` is operation one, operation two is empty, `Bait`
is operation three and the remaining operations are hidden. Every type examines as `I can see fish
swimming in the water.` The production script therefore registers direct first- and third-option
handlers rather than relying on the Wiki's compressed option list.

The pinned [raw salmon revision
14975761](https://oldschool.runescape.wiki/w/Raw_salmon?oldid=14975761) and [raw trout revision
14935651](https://oldschool.runescape.wiki/w/Raw_trout?oldid=14935651) define the cascading fly
table: boosted level-30 salmon rolls first at endpoints 16/96 for 70 XP, followed by level-20 trout
at 32/192 for 50 XP. Both consume one regular feather only after success. Their 27 November 2024
change records pin the unusual no-period messages `You catch a raw salmon` and `You catch a raw
trout`. The cutoff [fly fishing rod revision
14986075](https://oldschool.runescape.wiki/w/Fly_fishing_rod?oldid=14986075) and [pearl fly rod
revision 14935074](https://oldschool.runescape.wiki/w/Pearl_fly_fishing_rod?oldid=14935074) establish
ordinary and carried/worn pearl equivalence.

The pinned [raw pike revision
14957889](https://oldschool.runescape.wiki/w/Raw_pike?oldid=14957889) defines boosted level 25,
60 XP and endpoints 16/96 with fishing bait. It also preserves Mod Ash's server-code observation:
attempts normally resolve every five ticks but an independent 1-in-5 roll extends an attempt to six
ticks. The pinned [raw rainbow fish revision
14791991](https://oldschool.runescape.wiki/w/Raw_rainbow_fish?oldid=14791991) and [stripy feather
revision 14788011](https://oldschool.runescape.wiki/w/Stripy_feather?oldid=14788011) define the
members-only level-38 route, endpoints 8/64, 80 XP and one success-only stripy feather.

Cache 233.4 resolves fly rod 309, regular feather 314, raw salmon 331, raw trout 335, raw pike 349,
stripy feather 10087, raw rainbow fish 10138, pearl fly rod 22844 and pearl fishing rod 22846.
It pins ordinary casting sequence 622 and distinct pearl sequences 8188/8189. The shared Fishing
service now models tools with their own cache animation, chooses the regular fly table when regular
feathers are present and otherwise the stripy-feather table, revalidates all state after each
delay, and commits bait removal plus catch insertion atomically. Sixteen focused scenarios cover
both ordinary spot families and the new cache, dispatch, cascade, cadence, membership, equipment
and transaction boundaries. Dual-feather priority in the matching client, relocation topology,
exact legacy pike/rainbow and rejection wording, audiovisuals and packet order remain explicit
manual case 70 qualifications rather than asserted verification.

## Cache-233 cage/lobster Fishing

The final pre-cutoff [cage/harpoon spot revision
14997261](https://oldschool.runescape.wiki/w/Fishing_spot_(cage,_harpoon)?oldid=14997261), dated
1 October 2025, identifies the ordinary lobster-and-harpoon NPC-name family. Cache 233.4 provides
the decisive interaction contract: 1510, 1519, 1522, 2146, 3657, 3914, 7199, 7460, 7465, 7470,
7946, 9173, 9174, 10515, 12777 and 14039 expose `Cage`, empty, `Harpoon` in their first three
operation slots. Three source-taxonomy candidates are not lobster entries: 5821 and 10514 expose
`Net`, while 12776 exposes `Big Net`; all three retain `Harpoon` in the third slot. Production
therefore registers lobster only for the 16 cache-confirmed `Cage` identities instead of treating
the source page title as an operation guarantee. Every inspected type has the cache-enriched
examine `I can see fish swimming in the water.`

The pinned [raw lobster revision
14935370](https://oldschool.runescape.wiki/w/Raw_lobster?oldid=14935370) establishes boosted Fishing
level 40, 90 Fishing XP and success endpoints 6/95. The pinned [lobster pot revision
14956268](https://oldschool.runescape.wiki/w/Lobster_pot?oldid=14956268) establishes the required
non-consumed tool. The cutoff [Fishing revision
14980550](https://oldschool.runescape.wiki/w/Fishing?oldid=14980550) supplies the ordinary
five-tick attempt cadence. Cache 233.4 independently resolves lobster pot 301, raw lobster 377 and
dedicated player sequence 619 `human_lobster`.

The RSMod plugin uses direct option-one registration, typed object/sequence/NPC references and the
same protected loop as the preceding ordinary methods. Every result tick revalidates adjacency,
boosted level, pot possession and inventory capacity; successful insertion is transactional and
no bait is consumed. Three added scenarios, plus the expanded cache/table contract, bring the
focused Fishing suite to 19 scenarios and cover real option-one routing at five ticks, XP/output/
message behavior, missing level/tool/capacity/distance rejection and all positive and negative
cache operation classifications. The success line `You catch a lobster.` is a bounded legacy
template until matching-client confirmation. Relocation, presentation and the complete harpoon
family remain researched under manual case 71.

## Cache-233 ordinary cage-spot harpoon Fishing

The same final pre-cutoff [cage/harpoon spot revision
14997261](https://oldschool.runescape.wiki/w/Fishing_spot_(cage,_harpoon)?oldid=14997261) enumerates
the ordinary tuna/swordfish family. Cache 233.4 confirms `Harpoon` in operation slot three on the
exact 18 cutoff identities 1510, 1519, 1522, 2146, 3657, 3914, 5821, 7199, 7460, 7465, 7470,
7946, 9173, 9174, 10515, 12776, 12777 and 14039. NPC 10514 also has `Net`, empty, `Harpoon` in the
cache, but is absent from this cutoff source family and remains excluded pending its separate
shark-table slice. Unlike lobster dispatch, the harpoon route intentionally includes the source
page's `Net/Harpoon` NPC 5821 and `Big Net/Harpoon` NPC 12776 because their third operations and
cutoff membership agree.

Pinned revisions for [raw tuna
14882835](https://oldschool.runescape.wiki/w/Raw_tuna?oldid=14882835) and [raw swordfish
14930407](https://oldschool.runescape.wiki/w/Raw_swordfish?oldid=14930407) establish the ordered
swordfish-before-tuna cascade: swordfish requires boosted level 50, awards 100 XP and uses success
endpoints 4/48; tuna requires boosted level 35, awards 80 XP and uses endpoints 8/64. Their source
messages are `You catch a raw swordfish.` and `You catch a raw tuna.` The cutoff [Fishing revision
14980550](https://oldschool.runescape.wiki/w/Fishing?oldid=14980550) supplies the ordinary five-tick
cadence. The pinned [harpoon revision
14838279](https://oldschool.runescape.wiki/w/Harpoon?oldid=14838279) and cache resolve ordinary
harpoon 311 with player sequence 618. The pinned members-only [barb-tail harpoon revision
14978282](https://oldschool.runescape.wiki/w/Barb-tail_harpoon?oldid=14978282), cache object 10129
and cache/RuneLite sequence 5108 establish the carried-or-worn alternate with no Attack-level gate
and the same catch rates.

The pinned [big swordfish revision
14957577](https://oldschool.runescape.wiki/w/Big_swordfish?oldid=14957577) establishes members-only
object 7991 as a one-in-2500 replacement after a successful swordfish roll, preserving 100 Fishing
XP and using `You catch an enormous swordfish!`. Production applies that replacement only on a
members world, never adds a second item, and makes the rare roll only after swordfish succeeds.
The implementation uses typed NPC/object/sequence references, direct operation-three registration
and the shared protected Fishing transaction: every result tick revalidates boosted level, an
eligible carried/worn tool and free capacity before atomically adding the selected catch. Six new
scenarios and the expanded cache/table contract bring the focused Fishing suite to 25 scenarios.

This slice does not infer adjacent mechanics. Fishing Guild's invisible +7; dragon, infernal and
crystal harpoon rates, charges and cooking; barehanded Fishing/Strength gates and Strength XP;
Rada's blessing and spirit-flake doubling (including big swordfish); conditional clue-bottle and
heron rolls; relocation, audiovisuals, exact rejection presentation, packet order, tick
manipulation and interruption remain researched or undiscovered under manual case 71.

## Cache-233 ordinary big-net/harpoon shark Fishing

The final pre-cutoff [big-net/harpoon spot revision
14992635](https://oldschool.runescape.wiki/w/Fishing_spot_(big_net,_harpoon)?oldid=14992635),
dated 25 September 2025, pins the source family to NPCs 1511, 1520, 3419, 3915, 4476, 4477,
5233, 5234, 5820, 7200, 7461, 7466, 8525, 8526, 8527, 9171, 9172, 12775, 14037 and 14523.
Cache 233.4 confirms `Harpoon` in operation slot three and the shared examine text on all 20. It
also reveals a source-taxonomy exception that production and tests preserve explicitly: NPC 5820
has `Cage`, empty, `Harpoon`, while the other identities use `Big Net` or `Net` first. Shark
dispatch follows the source-family membership plus cache-confirmed third operation; no first-option
handler is inferred from the page title.

The pinned [raw shark revision
14926495](https://oldschool.runescape.wiki/w/Raw_shark?oldid=14926495) establishes level 76, 110
Fishing XP and ordinary-harpoon success endpoints 3/40. The pinned [big shark revision
14930403](https://oldschool.runescape.wiki/w/Big_shark?oldid=14930403) establishes members-only
object 7993 as a one-in-5000 replacement only after a successful shark roll, preserving 110 XP and
using `You catch an enormous shark!`; it also identifies the ordinary line as `You catch a shark!`.
Cache 233.4 resolves raw shark 383, big shark 7993, harpoon 311, barb-tail harpoon 10129 and
sequences 618/5108. Revision-matched local RuneLite independently confirms every one of those item
and animation identities.

Production registers direct operation-three dispatch for all 20 typed NPC references and uses the
same protected five-tick transaction as the cage-spot harpoon slice. The method is members-only,
accepts an ordinary carried harpoon or carried/worn barb-tail harpoon, and revalidates membership,
boosted level, tool, adjacency and capacity on the result tick before atomically adding one raw or
big shark. Five added scenarios bring the focused Fishing suite to 30 and cover the cache/event
contract, ordinary shark result, rare replacement, free-world rejection, barb-tail animation and
level/tool/capacity revalidation.

The pinned [big fishing net revision
14997131](https://oldschool.runescape.wiki/w/Big_fishing_net?oldid=14997131), together with the spot
revision, describes a distinct first-option method using big net 305 and cache sequence 620. It
makes nine independent rolls in order—leather boots, seaweed, leather gloves, mackerel, a second
mackerel, oyster, casket, cod and bass—and can therefore produce multiple items per attempt. Pinned
raw-mackerel 14935361, raw-cod 14958042 and raw-bass 14977847 revisions establish levels 16/23/46,
XP 20/45/100 and endpoints 5/65 plus a constant 10/10 second mackerel roll, 4/55 and 3/40. The same
source module, cross-checked against the final pre-cutoff leather-boots 14799016, leather-gloves
14799037, seaweed 14848011, oyster 14861581 and casket 14858594 revisions, pins the junk/product
rows to 10/10 and 1 XP for boots, seaweed and gloves; 3/7 and 10 XP for oyster; and 1/2 and 10 XP
for casket. A charged, worn skills necklace adds exactly 1/256 to the casket roll. Pinned big-bass
revision 14957574 establishes a one-in-1000 replacement after successful bass, unchanged 100 XP
and exact enormous-bass wording. Raw bass also explicitly makes level 46 a base-level unlock: a
temporary boost cannot unlock bass, although visible Fishing continues to drive success chance
after the unlock.

Production now registers direct operation-one dispatch for the 19 source identities whose cache
operation is `Big Net` or `Net`, explicitly excluding NPC 5820's incompatible `Cage`. The dedicated
members-only method requires big net 305, animates with sequence 620, revalidates membership,
base/boosted level, tool, adjacency and free capacity after five ticks, evaluates all eligible
rolls in source order, accepts only as many results as the remaining slots, and commits accepted
products atomically. Six added scenarios bring the focused Fishing suite to 36 and prove dispatch,
roll ordering and independent duplicate mackerel, XP/results, necklace modification, base-level
bass gating, rare replacement, partial capacity and result-tick revalidation. Exact matching-client
junk wording and partial-capacity presentation remain isolated in manual case 72; fish barrel,
Fishing Guild +7, shark lure, special harpoons, barehanded Strength XP, Rada/spirit flakes,
conditional clue/moon-key/heron rolls and relocation remain separate researched work.

## Cache-233 specialist harpoon catch rates

The final pre-cutoff [dragon harpoon revision
14874226](https://oldschool.runescape.wiki/w/Dragon_harpoon?oldid=14874226), [infernal harpoon
revision 14979405](https://oldschool.runescape.wiki/w/Infernal_harpoon?oldid=14979405), [crystal
harpoon revision 14969857](https://oldschool.runescape.wiki/w/Crystal_harpoon?oldid=14969857) and
[Fishing revision 14980550](https://oldschool.runescape.wiki/w/Fishing?oldid=14980550) establish
visible Fishing use levels 61, 75 and 71. Dragon and both charged/depleted infernal harpoons use a
20% higher chance than the regular harpoon; charged crystal uses 35%, while inactive crystal acts
as dragon. Every specialist tool is members-only and can be used from inventory or worn. The
cutoff raw-shark chance graph supplies a precise independent check on calculation order: at level
76 the regular 32/256 chance becomes 38/256 for dragon/infernal and 43/256 for charged crystal,
showing that multiplication is floored back onto the protocol's 1/256 probability grid.

Cache 233.4 resolves base, Trailblazer and Trailblazer Reloaded dragon/infernal styles, charged and
depleted infernal forms, and charged/inactive crystal to objects 21028, 25373, 30349, 21031, 21033,
25059, 25367, 30342, 30343, 23762 and 23764. It resolves their seven presentation routes to
sequences 7401, 7402, 8336, 8784, 88, 11867 and 11868; revision-matched local RuneLite independently
confirms the base item identities and sequences 7401, 7402 and 8336 plus the original Trailblazer
routes 88 and 8784.

Production shares a strongest-usable harpoon table between cage/swordfish and big-net/shark
methods. The table supports carried and worn forms, visible use-level checks, exact animations,
1.20/1.35 multipliers with 1/256 flooring, depleted fallback rates and result-tick validation of
the originally selected tool. Five added scenarios bring the focused Fishing suite to 41 and pin
the full cache/table contract, rate-grid examples, actual dragon/crystal boundary successes,
crystal priority, inactive-crystal fallback and selected-tool removal. Infernal auto-cooking and
5,000-use degradation, crystal charge consumption/conservation, Fishstabber special attacks and
matching-client multi-tool feedback remain separate stateful slices under manual case 72.

## Cache-233 Fishstabber special attack

The final pre-cutoff [Special attacks revision
14994987](https://oldschool.runescape.wiki/w/Special_attacks?oldid=14994987), [dragon harpoon
revision 14874226](https://oldschool.runescape.wiki/w/Dragon_harpoon?oldid=14874226), [infernal
harpoon revision 14979405](https://oldschool.runescape.wiki/w/Infernal_harpoon?oldid=14979405) and
[crystal harpoon revision 14969857](https://oldschool.runescape.wiki/w/Crystal_harpoon?oldid=14969857)
agree that Fishstabber consumes 100% special energy and provides a visible +3 Fishing boost. The
dragon-harpoon source additionally states that activation during Fishing does not disrupt the
gathering action. Standard stat-boost behavior preserves a pre-existing higher visible boost.

Cache 233.4 enum 906 assigns the full 1,000-unit energy cost to all eleven retained dragon,
infernal, original Trailblazer, Reloaded and crystal states: objects
21028/25373/30349/21031/21033/25059/25367/30342/30343/23762/23764. The five relevant sequence
families are base dragon 7393 `fishstabber`, base infernal 7394 `fishstabber_infernal`, active and
inactive crystal 8333 `fishstabber_crystal`, original Trailblazer 11823
`fishstabber_trailblazer`, and Reloaded 11824 `fishstabber_trailblazer_reloaded`. Non-crystal forms
use red special-attack graphic 246; crystal uses silver graphic 1688. Area synth 2538 `rampage`
and overhead phrase `Here fishy fishies!` follow the existing non-combat special contract.

Production previously registered only six of the eleven states. It now registers the depleted base
infernal, both Reloaded infernal forms, Reloaded dragon form and inactive crystal form as well,
without coupling charge state to the instant boost. The production-startup integration scenario
resolves every object through the real cache enum and registry, then pins full energy deduction,
the exact +3 boost, overhead phrase, per-family sequence and graphic. Exact sound audience/packet
order, combat-tab interface timing, activation while an actual Fishing queue is running and stat
restoration/Preserve timing remain matching-client observations in manual case 72.

## Cache-233 Fishing Guild ordinary-spot boost

The final pre-cutoff [Fishing revision
14980550](https://oldschool.runescape.wiki/w/Fishing?oldid=14980550) establishes an invisible +7
Fishing boost while fishing inside the guild. Invisible boosts raise success chance and stack with
visible boosts, but do not satisfy product requirements. This is consistent with the pinned
[raw-tuna revision 14882835](https://oldschool.runescape.wiki/w/Raw_tuna?oldid=14882835), [raw-shark
revision 14926495](https://oldschool.runescape.wiki/w/Raw_shark?oldid=14926495) and [big-net revision
14997131](https://oldschool.runescape.wiki/w/Big_fishing_net?oldid=14997131): the product gate is
checked against the player's visible or explicitly documented base level, while the +7 participates
only in the success-rate calculation.

Cache 233.4 identifies the two guild spots used by the currently implemented ordinary families:
cage/harpoon NPC 1510 (`0_40_53_rarefish`) and big-net/harpoon NPC 1511
(`0_40_53_memberfish`). Production applies +7 only when rolling against those exact typed NPCs. It
does so after the five-tick revalidation and after visible/base product and specialist-tool gates,
then passes the increased effective level through the existing 1/256 success formula. Specialist
harpoon multiplication is applied afterward and remains floored to that grid. At level 76 shark is
therefore 32/256 outside the guild, 35/256 with the guild boost and 42/256 with both the guild boost
and a dragon/infernal-rate tool.

Three added scenarios bring the focused Fishing suite to 44. They pin both NPC identities and the
three shark rate boundaries, prove a guild-only tuna boundary success on NPC 1510, prove the same
chance behavior cannot unlock shark below level 76 on NPC 1511, and prove all eligible big-net
rolls receive +7 without unlocking cod or bass. Future guild methods such as minnows must join this
typed contract when their own production slice is implemented; this slice makes no claim for an
unimplemented method merely because its eventual map location is inside the guild.

## Cache-233 infernal harpoon passive and depletion

The final pre-cutoff [infernal harpoon revision
14979405](https://oldschool.runescape.wiki/w/Infernal_harpoon?oldid=14979405), supported by the
[dragon harpoon revision
14874226](https://oldschool.runescape.wiki/w/Dragon_harpoon?oldid=14874226), establishes a one-in-
three chance to auto-cook a fish caught with a charged infernal harpoon. The raw fish is destroyed,
there is no ordinary burn roll, and the player receives half of its normal Cooking XP. A charge is
spent only when this passive consumes a fish; after 5,000 consumed fish the harpoon changes to its
depleted form and continues to work at the dragon-harpoon catch rate. The pinned [Cooking revision
14978581](https://oldschool.runescape.wiki/w/Cooking?oldid=14978581) supplies normal tuna,
swordfish and shark values 100, 140 and 210, making the passive awards exactly 50, 70 and 105 XP.

Cache 233.4 resolves the three charged-to-depleted style pairs as 21031→21033, 25059→25367 and
30342→30343. Production stores remaining consumptions in the versioned 13-bit
`infernal_tool_charges` item field; a newly created or spawned charged identity's zero storage code
means the canonical 5,000 charges, while subsequent values store exact remaining uses. On the
result tick, big swordfish/shark replacement is resolved first. A big fish is retained, receives no
Cooking XP and spends no infernal charge. An ordinary tuna, swordfish or shark then receives the
one-in-three passive roll; on success, Fishing XP and half Cooking XP are awarded, the item is not
added and the exact carried or worn tool instance loses one charge.

Four added scenarios plus expanded cache/config assertions bring the focused Fishing suite to 48.
They prove full-state initialization, a carried tuna auto-cook and 4,999 remainder, a failed passive
that retains both tuna and all charges, worn Reloaded final-charge transition, all three style
mappings and big-swordfish preservation. Exact passive message/audio/packet presentation and the
smouldering-stone or dragon-harpoon creation/recharge actions remain separate work under manual
cases 71–72.

## Cache-233 crystal harpoon depletion

The final pre-cutoff [crystal harpoon revision
14969857](https://oldschool.runescape.wiki/w/Crystal_harpoon?oldid=14969857) establishes the charged
tool's 35% catch-rate improvement. The pre-cutoff [crystal equipment revision
14889770](https://oldschool.runescape.wiki/w/Crystal_equipment?oldid=14889770) establishes its
shared crystal-tool lifecycle: a newly created tool starts with 10,000 charges, may store at most
20,000 and consumes one charge for each item obtained with it. Cache 233.4 resolves active crystal
harpoon 23762 and inactive crystal harpoon 23764; the preceding specialist-rate slice establishes
that the inactive form remains usable at the dragon-harpoon rate.

Production stores the exact remaining count in a versioned 15-bit `crystal_tool_charges` item
field. A zero storage code on the active identity denotes the canonical initial 10,000 charges.
Every successful ordinary or big tuna/swordfish/shark result decrements the exact selected carried
or worn active instance after its inventory transaction succeeds. Failed attempts consume nothing.
The last successful catch converts that instance to inactive 23764; the next loop therefore falls
back through the existing strongest-usable table at the 1.20 rate.

Four added scenarios plus expanded config assertions bring the focused Fishing suite to 52. They
pin the state field, 10,000 initial and 20,000 maximum values, active/inactive mapping, a carried
ordinary catch leaving 9,999 charges, a failed attempt retaining 10,000, a worn final-charge
transition and charge use by a big-swordfish catch. Crystal-shard charging, dismantling, charge-
inspection presentation and Elven/celestial signet or lucky-penny conservation remain separate
stateful work under manual cases 71–72.

## Cache-233 infernal harpoon creation

The final pre-cutoff [infernal harpoon revision
14979405](https://oldschool.runescape.wiki/w/Infernal_harpoon?oldid=14979405) and [dragon harpoon
revision 14874226](https://oldschool.runescape.wiki/w/Dragon_harpoon?oldid=14874226) establish the
base recipe: combine one dragon harpoon with one smouldering stone, require unboosted level 75
Fishing and level 85 Cooking, permanently create an infernal harpoon, and award 200 Fishing plus
350 Cooking XP. The Wiki's cache-period Cooking experience table independently reports the 350-XP
Cooking side of that same two-material recipe. Cache 233.4 pins the three identities as smouldering
stone 13233, dragon harpoon 21028 and charged infernal harpoon 21031.

Production registers the pair as a held-item interaction, accepts either selection order, resolves
the exact selected slots and performs a single atomic two-input-to-one-output inventory
transaction. It uses base rather than visible levels so boosts cannot satisfy either requirement,
requires a members world and leaves both materials untouched on every failed or stale request. The
output occupies the dragon-harpoon slot even when every other inventory slot is filled, and the
existing zero-state convention makes it a canonical full 5,000-charge tool.

Three added scenarios bring the focused Fishing suite to 55. They pin the cache identities and
dispatch, both held-use orders, exact full-inventory slot replacement, the initial charge state,
both XP awards, membership, unboosted level gates and stale-input safety. Recharge via a depleted
tool plus either another dragon harpoon or a smouldering stone, ornamented creation behavior and
exact matching-client messages, animation, sound and packet order remain separate work under
manual cases 71–72.

## Cache-233 Rada's blessing ordinary single-output catches

The cache-period [Fishing revision
14980550](https://oldschool.runescape.wiki/w/Fishing?oldid=14980550) and Jagex's [Kebos Lowlands
announcement](https://oldschool.runescape.wiki/w/Update%3AOSRS_Reveals%3A_The_Kebos_Lowlands)
establish that Rada's blessing must be equipped and gives tiers 1–4 exact 2%, 4%, 6% and 8%
chances to catch a second fish, with no XP for the additional catch. Cache 233.4 resolves those
tiers to objects 22941, 22943, 22945 and 22947. The cache-period [big-bass revision
14957574](https://oldschool.runescape.wiki/w/Big_bass?oldid=14957574) records the post-rewrite rule
that Rada's blessing can duplicate a big-fish replacement rather than rerolling the replacement.

Production applies the tier chance after an ordinary single-output catch and its rare replacement
have been selected. It adds one or two copies in the same inventory transaction and awards the
selected catch's Fishing XP exactly once. The bounded core covers small net, rod bait, lure,
lobster and harpooning with regular, barb-tail, dragon, depleted-infernal or inactive-crystal tools.
When only one output slot exists, it preserves the base catch without attempting the second. Active
infernal/crystal tools and the nine-independent-roll big-net method remain excluded until exact
modifier/passive/charge and multi-output capacity ordering is separately evidenced.

Six added scenarios bring the focused Fishing suite to 61. They pin all four identities and rate
thresholds, equipped-only activation, two-fish inventory output with one XP award, duplication of
the selected big swordfish, single-slot base-catch preservation and the deliberate active-crystal
boundary. Spirit flakes remain a separate consumable modifier: Jagex's [Fishing Skill Boss and
Easter Event update](https://oldschool.runescape.wiki/w/Update%3AFishing_Skill_Boss_and_Easter_Event)
establishes their 50% extra-fish chance, no extra XP and stacking with Rada's blessing; the next
bounded slice below pins their exact consumption and capacity ordering.

## Cache-233 Spirit flakes ordinary single-output catches

The final pre-cutoff [Spirit flakes revision
14908234](https://oldschool.runescape.wiki/w/Spirit_flakes?oldid=14908234) pins cache object 25588
and closes the key transaction ambiguities. A carried stack gives a 50% chance to double an
eligible catch, consumes one flake on every eligible catch even when the bonus roll fails, gives no
XP for the extra output and does not replace or increase ordinary bait consumption. With only one
output slot the effect neither rolls nor consumes a flake. The same revision says the Rada tier adds
2–8 percentage points to the double-catch chance and permits non-fish catches; its cutoff exclusion
list is Tempoross, Fishing Trawler, Chambers of Xeric and Drift Net Fishing. Jagex's [24 March 2021
release announcement](https://oldschool.runescape.wiki/w/Update%3AFishing_Skill_Boss_and_Easter_Event?oldid=14253445)
independently confirms the 50% chance, no additional XP, broad fish eligibility and Rada stacking.

Production extends the ordinary single-output multiplier transaction with the carried flake. On
members worlds with at least two post-bait output slots, it consumes exactly one flake and rolls
`50 + equipped Rada percent` on a single 0–99 grid. A success atomically inserts two copies of the
already-selected ordinary or big fish; a failure inserts one, and both award Fishing XP once. With
one post-bait slot the base catch succeeds and the flake remains. The implemented ordinary spot
families are outside every exclusion named by the cutoff source.

Five added scenarios bring the focused Fishing suite to 66. They pin object 25588, the 49/50
success boundary, consumption on both bonus outcomes, one-bait use, no extra XP, the combined
Rada-4 57/58 boundary, big-swordfish duplication and the one-slot no-consume rule. Active infernal/
crystal tools remain outside this slice because the same source says each infernal output has an
independent cooking roll while crystal charges are per item obtained. Big net also remains separate
because each action has nine independent primary rolls and partial-capacity behavior.

## Cache-233 Rada and spirit-flake ordinary big-net multiplication

The final pre-cutoff [Spirit flakes revision
14908234](https://oldschool.runescape.wiki/w/Spirit_flakes?oldid=14908234) expressly includes
non-fish catches in the carried flake's 50-percent double-catch effect, consumes one flake for every
eligible catch regardless of the roll outcome, awards no XP for the second output and adds an
equipped Rada tier to the same chance. The final pre-cutoff [Civitas illa Fortis fishing-spot
revision 14992989](https://oldschool.runescape.wiki/w/Fishing_spot_(Civitas_illa_Fortis)?oldid=14992989)
independently states that Rada's blessing and Spirit flakes work at a spot which catches no fish.
That NPC is outside the implemented ordinary big-net family, so it is evidence for non-fish
eligibility rather than a claim that the separate spot is complete. The cache-period [big-bass
revision 14957574](https://oldschool.runescape.wiki/w/Big_bass?oldid=14957574) establishes that the
already-selected big-fish replacement may be duplicated.

Production walks accepted big-net primary results in their existing source order. If at least two
initial output slots remain, each result receives one `50 + equipped Rada percent` roll; a carried
flake is reserved and consumed whether that roll succeeds or fails. A doubled result reserves two
slots before any later primary result is considered, while Fishing XP and its message remain one
per primary result. This applies equally to fish, leather boots, seaweed, gloves, oysters and
caskets, and occurs after a successful bass has become big bass. With only one remaining slot, the
base output is accepted without rolling or consuming a flake.

Five added scenarios bring the focused Fishing suite to 71. They pin non-fish spirit and Rada
doubling, flake consumption after a failed roll, no extra XP, doubled-output capacity priority, big-
bass duplication and the one-slot no-consume rule. Capacity is intentionally bounded to the free
slots present before the action: whether consuming the final flake immediately exposes its former
inventory slot, plus exact multiplier feedback and packet order, remains a matching-client
observation under manual case 72.

## Cache-233 charged-harpoon catch-multiplier coexistence

The final pre-cutoff [infernal harpoon revision
14979405](https://oldschool.runescape.wiki/w/Infernal_harpoon?oldid=14979405) explicitly states that
an extra fish produced by Rada's blessing and/or spirit flakes has a separate independent chance to
be cooked. The same revision pins the 1/3 cook chance, half ordinary Cooking XP, cook-only charge
consumption and big-fish exclusion. The final pre-cutoff [crystal harpoon revision
14969857](https://oldschool.runescape.wiki/w/Crystal_harpoon?oldid=14969857) says one crystal charge
is consumed for every fish caught. Read together with the final pre-cutoff [Spirit flakes revision
14908234](https://oldschool.runescape.wiki/w/Spirit_flakes?oldid=14908234) and [Fishing revision
14980550](https://oldschool.runescape.wiki/w/Fishing?oldid=14980550), this closes the ordinary
charged-harpoon multiplication and per-output charge rules without extrapolating them to excluded
activities.

Production now resolves the additive Rada/spirit-flake multiplier before charged-tool passives. For
an ordinary infernal catch, each resulting non-big fish receives its own 1/3 cook roll; each cooked
output is removed, awards half normal Cooking XP and consumes one infernal charge, while the
primary catch awards Fishing XP once. A crystal harpoon consumes one charge for each resulting
ordinary or big fish. The existing one-output-slot rule still suppresses multiplication, and a
spirit flake remains one-per-primary-catch rather than one-per-output.

Two charged-interaction scenarios plus a big-net multi-result allocation regression bring the
focused Fishing suite to 73. They pin one-cooked/one-raw infernal output with exactly one charge and
one half-XP award, and a Rada-doubled crystal output with exactly two charges and one Fishing XP
award. If a two-output event begins with only one infernal or crystal charge, production transitions
the tool to its depleted form after the event; exact matching-client simultaneity at that boundary
remains under manual cases 71–72 rather than an exact parity claim.

## Cache-233 infernal harpoon recharging

The final pre-cutoff [infernal harpoon revision
14979405](https://oldschool.runescape.wiki/w/Infernal_harpoon?oldid=14979405) states that an
uncharged infernal harpoon can be recharged with either another smouldering stone or another dragon
harpoon and that the material supplies 5,000 charges. It names no Fishing/Cooking requirement or XP
award for recharging, unlike the separately specified creation recipe. The final pre-cutoff
[Trailblazer ornament revision
14853049](https://oldschool.runescape.wiki/w/Infernal_harpoon_(or)?oldid=14853049) and [Reloaded
revision 14811396](https://oldschool.runescape.wiki/w/Infernal_harpoon_(or)_(Trailblazer_Reloaded)?oldid=14811396)
identify the charged/depleted style pairs and establish that the ornament changes aesthetics rather
than gameplay. Cache 233.4 resolves the materials to 13233 and 21028 and the three pairs to
21033→21031, 25367→25059 and 30343→30342.

Production registers both materials against every depleted style. Either selected-item order is
accepted, membership and exact selected slots are revalidated, and a single atomic transaction
deletes the depleted tool plus material and writes the matching charged style into the original
tool slot. The zero-state charge convention restores exactly 5,000 charges; no level check or XP is
applied, including at level zero.

Two added scenarios bring the focused Fishing suite to 75. They pin all six held-item dispatch
pairs, every style/material/order combination in a full inventory, full charge state, zero skill and
XP behavior, members-only rejection and stale-input preservation. Exact recharge text and packet
presentation remain matching-client observations under manual cases 71–72.

## Cache-233 infernal harpoon Trailblazer ornament application

The final pre-cutoff [Trailblazer tool ornament kit revision
14787699](https://oldschool.runescape.wiki/w/Trailblazer_tool_ornament_kit?oldid=14787699) identifies
members-only kit 25090, says it attaches to infernal tools to recolour them, and states that the
process is reversible. The final pre-cutoff [ornamented infernal harpoon revision
14853049](https://oldschool.runescape.wiki/w/Infernal_harpoon_(or)?oldid=14853049) specifies a one-
tick kit-plus-infernal-harpoon recipe, identifies charged/depleted outputs 25059 and 25367, and says
the ornament has no gameplay bonus. Cache 233.4 identifies the corresponding base inputs as 21031
and 21033.

Production registers kit 25090 with both the charged and depleted base infernal harpoon. Either
selection order is accepted, membership and exact slots are revalidated, and one atomic full-
inventory transaction replaces both inputs with the matching original Trailblazer style in the
base-tool slot. The charged route copies the exact packed state, so a partially charged harpoon
retains its remaining charges; the operation has no skill check or XP.

Two added scenarios bring the focused Fishing suite to 77. They pin the kit identity, both dispatch
keys, charged/depleted outputs, both selection orders, full-inventory handling, exact partial-state
preservation, zero-level and zero-XP behavior, members-only rejection and stale-input safety.
Revert and Change-Style are separate object-operation slices, while exact messages, timing and
packet presentation remain matching-client observations under manual cases 71–72.

## Cache-233 infernal harpoon ornament lifecycle

The final pre-cutoff [Trailblazer tool ornament kit revision
14787699](https://oldschool.runescape.wiki/w/Trailblazer_tool_ornament_kit?oldid=14787699) says the
ornament process can be reverted at any time and returns both the tool and the kit. The final pre-
cutoff [ornamented infernal harpoon revision
14853049](https://oldschool.runescape.wiki/w/Infernal_harpoon_(or)?oldid=14853049) identifies the
charged/depleted original Trailblazer forms, their inventory options `Wield, Check, Revert, Change-
Style, Drop` and `Wield, Revert, Change-Style, Drop`, and their worn `Check, Change-Style` and
`Change-Style` operations. It states that Change-Style produces the Reloaded variant. The final
pre-cutoff [Reloaded revision
14811396](https://oldschool.runescape.wiki/w/Infernal_harpoon_(or)_(Trailblazer_Reloaded)?oldid=14811396)
pins charged/depleted IDs 30342/30343, the same inventory/worn operation surfaces, and two-way
alternation with the original Trailblazer variant. Cache 233.4 independently fixes Revert in the
third item-option field, Change-Style in the fourth, and worn Change-Style in `wear_op2` for all
four identities.

Production registers Revert on the cache-protocol third held operation and Change-Style on the
fourth held and third worn operations. Revert accepts either charged/depleted visual style, returns
the matching plain base tool plus kit 25090 atomically, rejects insufficient capacity without
mutation and copies packed state to the plain tool. Change-Style alternates original and Reloaded
forms in either direction, in inventory or while worn, and remains one-for-one in a full inventory.
All paths are members-only, have no skill/XP gate and preserve exact packed state.

Five added scenarios bring the focused Fishing suite to 82. They pin the full cache operation
arrays and worn-operation params, all four Revert identities, component return, full-inventory
atomicity, every charged/depleted two-way style transition in inventory and while worn, packed-
state preservation, zero-level/zero-XP behavior and membership rejection. Whether Revert displays
a confirmation dialogue, plus exact success/failure messages, audiovisuals and packet order,
remains a matching-client observation under manual cases 71–72.

## Cache-233 infernal harpoon inspection and ornamented equip

The final pre-cutoff [infernal harpoon revision
14979405](https://oldschool.runescape.wiki/w/Infernal_harpoon?oldid=14979405) establishes an initial
5,000 charges, one charge consumed for each successful auto-cook, an inventory Check option on the
charged tool and a Check option while it is equipped. The final pre-cutoff [ornamented revision
14853049](https://oldschool.runescape.wiki/w/Infernal_harpoon_(or)?oldid=14853049) and [Reloaded
revision
14811396](https://oldschool.runescape.wiki/w/Infernal_harpoon_(or)_(Trailblazer_Reloaded)?oldid=14811396)
establish the same charged inspection surface for both cosmetic styles and distinguish the depleted
forms, which have no Check operation.

Direct cache 233.4 inspection pins the base charged inventory array as `null, Wield, Check, null,
null`; charged ornament arrays as `Wield, Check, Revert, Change-Style, null`; depleted ornament
arrays as `Wield, null, Revert, Change-Style, null`; and charged worn `wear_op1` as Check. This means
the base Check dispatches through held operation three, ornamented Check through held operation two,
all worn Check through operation two, and ornamented Wield through held operation one. Production
now registers those distinct routes, reports the exact persisted count (with packed zero denoting a
fresh 5,000-use tool), explicitly equips all four ornamented identities, preserves packed state and
enforces membership.

Three added scenarios bring the focused Fishing suite to 85. They pin the raw cache arrays and worn
params, canonical and partial charge reporting for all three charged styles, carried and worn
dispatch, membership rejection and state preservation while equipping each charged/depleted
ornament style. The sentence `The infernal harpoon has N charges remaining.` is a bounded
implementation inference: the historical pages and cache prove the numeric state and operation
surface but do not publish literal game text. Manual cases 71–72 retain exact wording, feedback and
packet-order capture before any client-presentation parity claim.

## Cache-233 crystal harpoon inspection and Revert

The final pre-cutoff [crystal harpoon revision
14969857](https://oldschool.runescape.wiki/w/Crystal_harpoon?oldid=14969857) exposes `Wield, Check,
Revert, Drop` on active 23762, `Wield, Revert, Drop` on inactive 23764, and worn Check only on the
active form. It states that Check reflects the remaining charge state and that Revert may be used at
any time, returning the dragon harpoon and crystal tool seed while forfeiting every shard used to
create or charge the tool. The pre-cutoff [crystal equipment revision
14889770](https://oldschool.runescape.wiki/w/Crystal_equipment?oldid=14889770) independently
describes the same component-returning lifecycle.

Direct cache 233.4 inspection pins the active raw item array as `null, Wield, Check, Revert, null`,
the inactive array as `null, Wield, null, Revert, null`, active `wear_op1` as Check and inactive
`wear_op1` as the empty default. Production therefore registers active inventory Check on held
operation three, active worn Check on operation two and both Revert routes on held operation four.
Check reads the existing versioned item state without mutation; packed zero reports the canonical
10,000 initial uses. Revert is an atomic one-to-two conversion into dragon harpoon 21028 and crystal
tool seed 23953, rejects insufficient inventory capacity and discards the charged state as sourced.
Both operations enforce membership.

Three added scenarios bring the focused Fishing suite to 88. They pin both operation arrays, both
worn params, tool-seed identity, 10,000 and 12,345 charge reporting, carried/worn dispatch,
active/inactive component recovery, full-inventory atomicity, state forfeiture and membership
rejection. The literal Check/rejection messages and whether Revert presents a confirmation are
bounded implementation choices pending matching-client capture under manual cases 71–72. Crystal-
shard quantity selection is intentionally left to its own slice.

## Cache-233 crystal harpoon shard charging

The final pre-cutoff [crystal harpoon revision
14969857](https://oldschool.runescape.wiki/w/Crystal_harpoon?oldid=14969857) says crystal shards may
be used on the harpoon at any time, including while active, each shard provides exactly 100 charges
and the tool stores at most 20,000. The pre-cutoff [crystal equipment revision
14889770](https://oldschool.runescape.wiki/w/Crystal_equipment?oldid=14889770) independently applies
the same rules to active and inactive crystal tools. Cache 233.4 resolves shard 23962 and the active/
inactive harpoon pair 23762/23764; the existing 15-bit item field persists the exact total.

Production registers shard use with either form and accepts both selected-item orders. It computes
the useful shard ceiling from the selected tool's current count and selected shard stack, asks for a
quantity, clamps oversized input, revalidates the exact post-input objects and atomically consumes
the chosen shards while writing active 23762 into the original tool slot. Each shard adds 100 uses;
an inactive tool starts from zero; the final useful shard caps at 20,000 rather than overflowing.
The one-for-one tool rewrite works with every inventory slot occupied. Membership is required.

Two added scenarios bring the focused Fishing suite to 90. They pin both held-use registrations,
both selected-item orders, active 19,850 plus two shards capping at 20,000, inactive plus two shards
becoming active with 200 charges, oversized input clamping, full-inventory operation, full-charge
and free-world rejection and post-dialogue stale-tool preservation. Quantity input is the standard
server interaction implied by applying a stackable charge material, but its literal prompt and all
success/rejection feedback remain bounded pending matching-client capture. Bank Configure-Charges
now has a separate preference-core record.

## Cache-233 crystal harpoon bank preference

Cache 233.4 gives active crystal harpoon 23762 BANK_AUTOCHARGE param 2257 pointing to the shared
crystal-tools DB row 4338. Its account-persistent target quantity is varbit 16148 over permanent
varp 4681 bits 0–15, the same cache preference used by the Crystal axe. Production registers both
active and inactive identities for the typed protected event; the bank's parameter validation
exposes the menu route only when the selected cache object advertises it. The members-only dialogue
clamps input to zero through the tool's 20,000-charge maximum and zero disables the preference.

Two focused Fishing scenarios pin the active parameter row, varbit/base-var layout, event routes,
upper-bound storage and non-member preservation. Automatic shard deduction and item-local charge
mutation on withdrawal, literal dialogue presentation and packet ordering remain manual case 71.

## Cache-233 Fishing-tool charge conservation

The final pre-cutoff [elven signet revision
14989911](https://oldschool.runescape.wiki/w/Elven_signet?oldid=14989911) fixes a 10% chance to save
a crystal-equipment charge. The final pre-cutoff [celestial signet revision
14888892](https://oldschool.runescape.wiki/w/Celestial_signet?oldid=14888892) applies that same
effect to both its uncharged and charged forms. The final pre-cutoff [Ghommal's lucky penny revision
14964658](https://oldschool.runescape.wiki/w/Ghommal%27s_lucky_penny?oldid=14964658) fixes a 5%
charge-saving chance while the penny is equipped, explicitly lists crystal harpoons and every
infernal-harpoon style, and preserves Mod Ash's contemporary confirmation that penny and signet
roll independently on crystal equipment. The pinned crystal-harpoon revision 14969857 independently
describes one charge per caught fish. Cache 233.4 resolves the signets as 23943/25543/25545 in the
ring slot and the penny as internal symbol `ca_ammoslot`, item 27544, in the ammunition slot.

Production now evaluates conservation once for each charge-producing fish. Elven and either
celestial form roll one in ten for a crystal harpoon; the penny rolls one in twenty for crystal and
infernal harpoons. When a crystal user equips both, both rolls are evaluated independently and a
charge is consumed only if both fail. Multiplied fish receive separate pairs of rolls, so a two-fish
result can consume zero, one or two charges. Infernal tools still reach this boundary only for fish
that succeeded on their independent one-in-three auto-cook roll.

Four added scenarios bring the focused Fishing suite to 94. They pin all four item identities and
wear slots, every eligible signet form, success and failure boundaries, penny coverage for crystal
and infernal tools, and independent per-output stacking on a multiplied crystal catch. The source
does not publish a conservation proc message; any audiovisual or packet feedback remains a
matching-client observation under manual cases 71–72 rather than an inferred production claim.

## Cache-233 barehanded Barbarian harpooning

The final pre-cutoff [Barbarian Training revision
14988720](https://oldschool.runescape.wiki/w/Barbarian_Training?oldid=14988720) establishes that
Otto unlocks barehanded harpooning through `Please teach me of your cunning with harpoons.` It pins
the same speed and catch rate as a regular harpoon, a 20-level higher boostable Fishing requirement,
unboostable Strength requirements and the exact catch table: tuna at 55 Fishing/35 Strength for 80
Fishing plus 8 Strength XP; swordfish at 70/50 for 100 plus 10; shark at 96/76 for 110 plus 11. The
final pre-cutoff [Fishing revision
14980550](https://oldschool.runescape.wiki/w/Fishing?oldid=14980550) independently describes the same
requirements and no-harpoon method. Pinned [Quest Helper source at commit
3f0992a](https://github.com/Zoinkwiz/quest-helper/blob/3f0992a8dee4e3a4feeb2a87051997a672702bfa/src/main/java/com/questhelper/helpers/miniquests/barbariantraining/BarbarianTraining.java)
confirms the assignment dialogue, the first successful tuna/swordfish/shark catch signal and the
exact first-catch message-box text.

Cache 233.4 resolves the harpoon-training state as varbit 3757 (`brut_fishing_s`). Its sequence
symbols pin start 6703, ready 6704, shark results 6705/6706, swordfish results 6707/6708, ordinary
loop/end-blank 6709 and tuna results 6710/6711; the workspace RuneLite `AnimationID` independently
maps the same nine identities. Production permits barehanded selection only on members worlds when
the Otto state is at least assigned and no configured usable harpoon exists. Candidate selection
checks boosted Fishing at ordinary requirement plus 20 and base Strength at the ordinary Fishing
requirement, retains the ordinary catch-rate curve and five-tick cadence, and awards the table's
Strength XP once per primary catch. The first catch changes state 1 to 2, sends the ordinary catch
line, opens the sourced training message and stops the action. A usable regular, barb-tail or
specialist harpoon remains higher priority and yields no Strength XP.

Four added scenarios bring the focused Fishing suite to 98. They pin the varbit and complete cache
animation family, locked and free-world behavior, Fishing-boost and base-Strength distinctions,
tuna/swordfish cascade gates, shark gates, exact Strength XP, first-catch state transition and
ordinary-tool priority. Production uses cache loop sequence 6709; exact start/ready and randomized
per-fish result choreography, rejection wording and packet/audiovisual presentation remain explicit
matching-client observations under manual cases 71–72.

## Cache-233 Barbarian heavy-rod Fishing

The final pre-cutoff [Barbarian Training revision
14988720](https://oldschool.runescape.wiki/w/Barbarian_Training?oldid=14988720), [Barbarian fishing
spot revision 14992995](https://oldschool.runescape.wiki/w/Fishing_spot_(barbarian)?oldid=14992995)
and leaping [trout revision
14861104](https://oldschool.runescape.wiki/w/Leaping_trout?oldid=14861104), [salmon revision
14861105](https://oldschool.runescape.wiki/w/Leaping_salmon?oldid=14861105) and [sturgeon revision
14861103](https://oldschool.runescape.wiki/w/Leaping_sturgeon?oldid=14861103) establish the two
locations, training prerequisite, rod choices, exact five-item bait priority, boostable Fishing
requirements, unboostable Strength/Agility requirements, triple-skill XP and 1/256 success
endpoints. The fish are evaluated from highest eligible tier downward: sturgeon at 70/45/45 with
8→64 and 80/7/7 XP, salmon at 58/30/30 with 16→96 and 70/6/6 XP, then trout at 48/15/15 with
32→192 and 50/5/5 XP.

Direct cache 233.4 inspection resolves the two `Use-rod` NPCs as 1542 and 7323, regular and pearl
rods as 11323 and 22842, roe/caviar/leaping fish/offcuts as 11324/11326/11328/11330/11332/11334,
and Otto's rod-training state as varbit 3758. It also pins sequences 9349/9350 for the regular rod
and 8190/8193 for the pearl rod. The workspace RuneLite `AnimationID` independently corroborates
9350, 8190 and 8193; it was read only and was neither launched nor modified. Pinned Quest Helper
commit 3f0992a independently supplies the assigned-to-caught state transition and exact successful
first-catch message.

Six focused scenarios pin both NPC dispatches, all cache identities, training and membership,
boosted-versus-base skill checks, each product boundary and XP tuple, bait priority, full-inventory
slot reclamation, pearl-rod selection, the first-catch transition and bonus-fish coexistence without
extra bait or triple-skill XP. The exact rejection wording and two-phase sequence choreography are
not published by these sources; the implementation's training rejection sentence and steady-loop
sequences are bounded choices pending cache-233 client capture under manual case 73.

## Cache-233 leaping-fish knife gutting

The final pre-cutoff leaping [trout revision
14861104](https://oldschool.runescape.wiki/w/Leaping_trout?oldid=14861104), [salmon revision
14861105](https://oldschool.runescape.wiki/w/Leaping_salmon?oldid=14861105) and [sturgeon revision
14861103](https://oldschool.runescape.wiki/w/Leaping_sturgeon?oldid=14861103) define a two-stage
roll. Gutting success is exactly Cooking level over 150 for trout, capped at 99/150, and Cooking
level over 80 for salmon/sturgeon, capped at certainty at level 80. Failure yields nothing. Success
produces roe for trout/salmon or caviar for sturgeon and awards 10/10/15 Cooking XP; only then does
the action independently roll fish offcuts at 1/2, 3/4 or 5/6. Final pre-cutoff [Fish offcuts
revision 14757666](https://oldschool.runescape.wiki/w/Fish_offcuts?oldid=14757666) independently
publishes the conditional offcut fractions and three-tick automatic cadence.

The official [Poll 77 Changes & More revision
14344231](https://oldschool.runescape.wiki/w/Update:Poll_77_Changes_%26_More?oldid=14344231)
establishes that knife use immediately remains available at will while a slower automatic pass cuts
the inventory. Cache 233.4 resolves knife 946, leaping fish 11328/11330/11332, roe/caviar/offcuts
11324/11326/11334 and the dedicated `brut_human_knifeuse` sequence 6702. Production starts with an
immediate selected fish and schedules another selected-type action every three ticks; another
manual use cancels/restarts that queue after processing immediately.

Five focused scenarios pin all cache identities and held-use dispatches, every recipe tuple and
probability endpoint, success/failure outputs and XP, conditional offcuts, selected-type automatic
cadence, membership/knife revalidation and safe full-inventory behavior. The cutoff sources do not
publish literal messages, mixed-type order, boost/drain semantics or the two-new-stack capacity
boundary. Production conservatively keeps a successful roe/caviar result and omits only a rolled
offcut that has no representable slot; manual case 74 must capture that behavior before it is
claimed as client-exact.

## 2026-09-11 — Fish barrel storage and ordinary-catch integration

Final pre-cutoff [Fish barrel revision
14978270](https://oldschool.runescape.wiki/w/Fish_barrel?oldid=14978270) establishes a combined
28-raw-fish capacity, closed/open state, automatic acquisition routing only while open, Fill,
Check, Open/Close and Destroy operations, bank/deposit-only emptying, and the exact full message
`The barrel is full. It may be emptied at a bank.` It explicitly excludes stackable fish, trophy
fish, Tempoross harpoonfish and fish from the Chambers of Xeric, Ruins of Camdozaal and Neypotzli.
The official [Fishing Skill Boss and Easter Event revision
14253445](https://oldschool.runescape.wiki/w/Update:Fishing_Skill_Boss_and_Easter_Event?oldid=14253445)
independently fixes capacity 28, mixed-species storage and bank-only removal, and identifies the
fish-sack combination as a separate equipable lifecycle.

Cache 233.4 resolves ordinary closed/open identities 25582/25584. Their operation arrays are
`Fill, Check, Open, null, Destroy` and `Fill, Check, Close, null, Destroy`; the allowlist maps to 33
cache identities after applying published exclusions and omitting post-cutoff Sailing fish. A
permanent non-stacking 28-slot server inventory preserves mixed contents across open/close and
banking. Fill transfers only allowlisted fish, bank-side Empty moves contents to bank, and Destroy
clears them. Functional Check opens a message box with stored types/counts; exact interface-193
layout remains matching-client work.

The ordinary small-net, bait, lure, lobster, tuna/swordfish, shark and Barbarian-rod result path now
counts open-barrel capacity before a roll and routes raw output after the inventory-side bait/flake
transaction succeeds. This permits catches at full inventory, preserves trophy exclusion, and
splits a doubled result between the final barrel slot and inventory without duplicating XP, bait or
flakes. Five focused scenarios pin identities/operations, allowlist, capacity, persistence,
Fill/Open/Close, membership, bank/destroy, full-inventory routing and doubled final-slot behavior.
Manual case 75 retains exact presentation, extended lifecycle and other acquisition sources.

## 2026-09-11 — Fish barrel ordinary big-net acquisition

Final pre-cutoff [Fish barrel revision
14978270](https://oldschool.runescape.wiki/w/Fish_barrel?oldid=14978270) says that an open barrel
automatically receives acquired raw fish when space remains and excludes trophy fish. Final
pre-cutoff [Big fishing net revision
14997131](https://oldschool.runescape.wiki/w/Big_fishing_net?oldid=14997131) fixes the ordinary
source order as boots, seaweed, gloves, mackerel, a second mackerel roll, oyster, casket, cod and
bass. Final pre-cutoff [Big bass revision
14957574](https://oldschool.runescape.wiki/w/Big_bass?oldid=14957574) explicitly says that a big
bass caught while carrying a fish barrel goes to inventory, while Rada's blessing or spirit flakes
can duplicate that trophy.

Production now preserves that source order while tracking inventory and open-barrel capacity
separately. Raw mackerel, cod and ordinary bass fill the barrel first; junk, caskets and a selected
big bass require inventory space. Consequently, an otherwise full inventory can reject an early
junk roll but retain a later eligible fish. A doubled eligible fish can use the final barrel slot
and one inventory slot without extra Fishing XP, message or flake consumption. Three focused
scenarios pin full-inventory routing, final-slot splitting and trophy exclusion. Exact multi-result
packet order and final-flake slot reclamation remain manual case 75 observations.

## 2026-09-11 — Fish sack barrel lifecycle core

Final pre-cutoff [Fish barrel revision
14978270](https://oldschool.runescape.wiki/w/Fish_barrel?oldid=14978270) states that a fish barrel
can be combined with a fish sack to make the equipable fish sack barrel, that assembly produces no
confirmation message, and that selecting Dismantle after right-clicking Configure returns the
components. The official [Fishing Skill Boss and Easter Event revision
14253445](https://oldschool.runescape.wiki/w/Update:Fishing_Skill_Boss_and_Easter_Event?oldid=14253445)
independently identifies the combined form as equipable in the cape slot.

Cache 233.4 pins fish sack 22838, closed combined form 25585 and open combined form 25587. Both
combined forms have wear position 1. Their inventory operations are respectively `Fill, Wear,
Configure, Open, Destroy` and `Fill, Wear, Configure, Close, Destroy`; their worn parameters expose
`Check` and the matching `Open` or `Close` operation. Production registers those exact operation
slots, recognizes an open combined barrel in inventory or the cape slot for automatic acquisition,
and retains the same permanent 28-slot store across assembly, equipment, toggling and dismantling.

Six focused scenarios pin cache metadata and dispatch, item-order-independent closed/open
assembly, membership, no success message, carried and worn operations, full-inventory worn
acquisition, state/content preservation, atomic dismantle capacity rejection and Destroy clearing.
The exact visual wording and packet shape of the Configure choice, equipped bank/deposit behavior,
death/gravestone handling and multiple-copy ownership remain bounded by manual case 75.

## 2026-09-11 — Holy Lake karambwanji Fishing core

Final pre-cutoff [raw karambwanji revision
14970676](https://oldschool.runescape.wiki/w/Raw_karambwanji?oldid=14970676) identifies untradeable
stackable object 3150, members-only level 5 small-net fishing, five XP per successful catch and
`floor(visible Fishing level / 5) + 1` items per catch, including 20 at 95–99 and 21 or more while
boosted beyond 99. It explicitly says Rada's blessing doubles the whole quantity without implying
extra XP. Its source chart is an ordered cascade: raw karambwanji at 100/250 from level 5, then raw
shrimps at 48/256. Historical module revision 14808626 independently pins those endpoint pairs,
and final pre-cutoff Fishing revision 14980550 plus Spirit flakes revision 14908234 establish the
equipped 2/4/6/8-percent Rada tiers and carried 50-percent spirit-flake chance, additive stacking,
one flake per eligible accepted catch and no extra XP. Historical Game tick revision 14986190 pins
karambwanji attempts to six ticks.

Final pre-cutoff [fishing-spot revision
14992999](https://oldschool.runescape.wiki/w/Fishing_spot_(karambwanji)?oldid=14992999) identifies
Holy Lake NPC 4710, its first-option `Net`, three map spawns, small-net requirement and the same
level, XP, quantity and cascade data. Cache 233.4 independently fixes NPC symbol
`0_43_47_karambwanji` to 4710, raw karambwanji to 3150, raw shrimps to 317, small fishing net to
303 and small-net sequence 621; cache loading confirms the NPC operation/examine contract and the
stackability difference between both outputs.

The two final-era pages conflict on the full-inventory edge. Revision 14970676 says shrimp may
still be caught, are not placed on the ground and do not cancel fishing; the later revision
14992999 says a full inventory with an existing karambwanji stack no longer catches shrimp and only
catches karambwanji. Production follows the later page's observable rule: failed primary rolls
skip the shrimp cascade while the action continues. Manual case 76 preserves the conflict for a
direct cache-233 client capture. The exact raw-karambwanji catch line is not printed by these
sources; production uses the normalized `You catch some raw karambwanji.` family form as a bounded
inference, while the exact post-2024 shrimp line is sourced as `You catch some raw shrimps.`

The dedicated handler uses protected access and one atomic inventory transaction per accepted
catch. Eight cache-aware scenarios pin identifiers and dispatch, all start gates, six-tick timing,
five XP, boosted quantities, the 57/58 combined modifier boundary with success/failure flake use,
ordered shrimp fallback, full-inventory suppression with continued fishing and distance
revalidation. Spot relocation, clue bottles, the heron and exact audiovisual/packet presentation
remain in the researched lifecycle row rather than being inferred.

## 2026-09-11 — Post-quest karambwan Fishing core

Final pre-cutoff [raw-karambwan revision
14995089](https://oldschool.runescape.wiki/w/Raw_karambwan?oldid=14995089), dated 27 September
2025, pins members-only level 65, loaded karambwan vessel plus raw-karambwanji bait, 50 Fishing XP,
the 5/160 skilling-success endpoints, bait loss on every attempt including failure and the exact
failure line `A Karambwan deftly snatches the Karambwanji from your vessel!`. Final pre-cutoff
[karambwan-vessel revision
14917620](https://oldschool.runescape.wiki/w/Karambwan_vessel?oldid=14917620), dated 10 June 2025,
establishes learning the method from Lubufu, loading the vessel with raw karambwanji and the empty
and loaded vessel identities/examines. Final pre-cutoff [Fishing revision
14980550](https://oldschool.runescape.wiki/w/Fishing?oldid=14980550) supplies the vessel-method
equipment family, while [Game tick revision
14986190](https://oldschool.runescape.wiki/w/Game_tick?oldid=14986190) explicitly fixes karambwan
resource rolls at four ticks. The established cutoff Rada's-blessing and spirit-flake sources apply
their 2/4/6/8-percent and 50-percent additive double-catch behavior without a second XP award.

Final pre-cutoff [karambwan fishing-spot revision
14996427](https://oldschool.runescape.wiki/w/Fishing_spot_(karambwan)?oldid=14996427), dated 29
September 2025, pins NPCs 4712/4713/4714, first-option `Fish`, the exact examine and four spawn
coordinates. It documents the unusual main-ground relocation topology: two ordinary and two
cutscene instances participate in a shared relocation process whose reachable destinations make
the visible production spots effectively stationary across ordinary sessions. Production freezes
4712/4713 but does not yet emulate the underlying four-source/three-destination graph. Conditional
clue-bottle and heron rolls stay in the researched lifecycle row.

Cache 233.4 independently fixes raw karambwan 3142, raw karambwanji 3150, empty vessel 3157, loaded
vessel 3159, main-ground NPCs 4712/4713, Lubufu quest NPC 4714, animation 1193 under cache symbol
`human_octopus_pot`, Tai Bwo Wannai Trio varp 320 and medium Karamja Diary varbit 3588. Cache
operations and examines are asserted directly. The local revision-matched RuneLite tree agrees on
the animation and item/NPC IDs. The final pre-cutoff
[Quest Helper commit](https://github.com/Zoinkwiz/quest-helper/commit/9f8fa419d28a5b69fc333d10a952ff06a97fcbca)
models the quest's main state and observed journal/dialogue transitions, but exposes no stable
client variable that distinguishes the exact moment Lubufu has taught the technique. Therefore the
automated core deliberately requires completed main state 6 and registers only main-ground NPCs;
the quest-only 4714 path remains a researched blocker instead of guessing a persistent unlock.

The final Wiki sources do not print the successful catch or initial lowering lines. Production's
`You lower the Karambwan vessel.` and `You catch a Karambwan!` are bounded presentation evidence
from historical interaction reconstruction and community observations, pending direct cache-233
client capture in manual case 77. A post-cutoff November 2025
[inventory observation](https://www.reddit.com/r/2007scape/comments/1olw4il)
corroborates a longstanding engine detail: the loaded vessel is removed, becomes empty and is
immediately reloaded from loose bait, which moves it to the first available slot. It is used only
to explain the item lifecycle, not as cutoff authority. A later Lost City content reconstruction
also contains the same messages and remove/add vessel model but retains the original 105-XP and
longer-timing behavior, directly conflicting with final OSRS Wiki evidence; those obsolete values
were rejected.

The implementation loads an empty vessel atomically in either held-item order, then performs a
protected four-tick loop. Every resolved attempt consumes the bait inside the loaded vessel;
available loose bait is consumed to recreate a loaded vessel, otherwise an empty vessel is
returned. Success adds one raw karambwan, 50 XP and medium-diary credit. Eligible success can be
doubled by the standard additive Rada/spirit-flake roll and routed to an open fish barrel, without
extra XP. Eight cache-aware scenarios cover cache contracts and dispatch, loading and membership,
all start gates, exact timing, success/failure consumption and reload, last-bait empty transition,
XP/diary state, multiplier/barrel capacity and distance revalidation. Quest acquisition,
relocation, conditional rewards and exact client presentation remain explicit rather than inferred.

## 2026-09-11 — Cave, swamp and lava eel Fishing

Final pre-cutoff [raw-slimy-eel revision
14964519](https://oldschool.runescape.wiki/w/Raw_slimy_eel?oldid=14964519), dated 12 August
2025, establishes members-only level 28, ordinary or pearl fishing rod plus fishing bait, 80 XP in
the Lumbridge Swamp Caves, Dorgesh-Kaan South Dungeon and Ruins of Camdozaal, but 65 XP at Mort
Myre. It also pins 10/80 success endpoints and the exact catch line `You catch a raw slimy eel.`
Final pre-cutoff [raw-cave-eel revision
14882826](https://oldschool.runescape.wiki/w/Raw_cave_eel?oldid=14882826), dated 14 April 2025,
fixes level 38, 80 XP, the same equipment and 10/80 endpoints, and the exact line `You catch a raw
cave eel.` Both pages specify the shared cave cascade: eligible cave eel is attempted first, then
slimy eel. The final pre-cutoff [Game tick revision
14986190](https://oldschool.runescape.wiki/w/Game_tick?oldid=14986190) puts ordinary fishing spots
outside its minnow, karambwan and karambwanji exceptions on the five-tick resource cycle.

Final pre-cutoff [frog-spawn/eels spot revision
14992997](https://oldschool.runescape.wiki/w/Fishing_spot_(frog_spawn,_eels)?oldid=14992997),
dated 25 September 2025, identifies NPCs 1497–1500 and 10653, the first-option Net/Small Net and
third-option Bait operations, exact examine `I can see fish swimming in the water.`, and the cave
eel/slimy eel table. Final pre-cutoff [swamp spot revision
14993004](https://oldschool.runescape.wiki/w/Fishing_spot_(swamp)?oldid=14993004), dated 25
September 2025, identifies NPCs 2653–2655, first-option Bait, exact examine `I can see 'things'
swimming in the water.`, and the Mort Myre slimy-only 65-XP table. Cache 233.4 independently pins
all eight NPC identities and operation slots, raw slimy eel 3379, raw cave eel 5001, rod 307, bait
313, pearl rod 22846 and sequences 622/8188. The cache editor freezes those resource NPCs while
leaving the relocation graph as explicit future work. Area-entry quest and light-source rules are
owned by their transports/environment; no evidence supports duplicating those checks inside the
fishing action once a player can reach a spot.

The production method uses the established protected ordinary-fishing loop: boosted visible-level
gates, carried ordinary or carried/worn pearl rods, five-tick first/repeat attempts, success-only
bait removal, atomic capacity checks, distance/tool/state revalidation, Rada/spirit-flake
multiplier behavior and open-fish-barrel routing. Eight cache-aware scenarios cover cache
contracts and dispatch, ordered cascade, exact XP/messages, all start gates, failure persistence,
pearl animation, distance interruption and multiplier/barrel behavior. Frog spawn and swamp weed
from the separate first-option net method, resource relocation, conditional clue bottles/heron and
exact audiovisual/packet presentation remain researched under manual cases 78–79.

Final pre-cutoff [raw-lava-eel revision
14996876](https://oldschool.runescape.wiki/w/Raw_lava_eel?oldid=14996876), dated 30 September
2025, records level 53, oily rod plus bait, 60 XP, 16/96 endpoints, no heron roll, a deliberately
free-world-capable catch despite the members-marked item, and hard Wilderness Diary credit at the
Lava Maze. Final pre-cutoff [lava-eel spot revision
14993000](https://oldschool.runescape.wiki/w/Fishing_spot_(lava_eel)?oldid=14993000), dated 25
September 2025, pins NPCs 4928/6784 and oily ordinary/pearl rods, but its spot table says 30 XP.
The raw-item value is selected for production: the final pre-cutoff Fishing page and its underlying
fish data also identify 60 Fishing XP, whereas 30 is independently the cooked lava eel's Cooking
XP. The isolated spot-table value is therefore treated as a copied Cooking value, with the conflict
preserved here instead of silently discarded.

The Heroes' Quest transcript establishes that Gerrant teaches the blamish-oil technique and supplies
the blamish snail slime used to make the oily rod. It provides two context-sensitive missing-gear
lines, but no evidence of a catch-time quest-var check once the player possesses the required rod.
The method table likewise lists no separate requirement, and the explicit free-world exception
would be incompatible with a normal members-content gate. Production therefore models the action
as possession-based: visible Fishing 53, oily ordinary or carried/worn oily pearl rod and bait, with
no members-world or varp-188 check. Quest dialogue and rod acquisition remain owned by Heroes'
Quest content rather than duplicated inside Fishing.

Cache 233.4 pins NPCs 4928/6784, first-option `Bait`, examine `I can see eels swimming in the
lava.`, bait 313, oily rods 1585/23122, raw lava eel 2148 and casting sequences 622/8188. Local
RuneLite data independently maps both NPCs as lava-eel fishing spots and distinguishes their
Taverley Dungeon and Wilderness locations. The production plugin freezes both cache NPCs and uses
the established protected fishing loop: five-tick 16/96 attempts, success-only bait deletion,
atomic capacity handling, fish-barrel routing, pearl animation and distance/tool/state
revalidation. Six integration scenarios pin both cache contracts and dispatch, the free-world and
zero-quest-state exception, 60 XP, all start gates, failed-roll persistence, worn pearl behavior and
two interruption paths.

The hard Wilderness Diary aggregate varps 1192/1193 and hard-count varbit 6325 are identifiable,
but neither the cache nor local RuneLite exposes the individual packed task bit. That hook is not
guessed. The sourced no-heron rule, clue-bottle behavior, exact catch line and which of the two
transcript gear lines belongs to each spot, Rada/spirit-flake eligibility, relocation and exact
audiovisual/packet presentation remain researched under manual case 79.

## 2026-09-11 — Frogspawn and swamp-weed net Fishing

Final pre-cutoff [frog-spawn revision
14706989](https://oldschool.runescape.wiki/w/Frog_spawn?oldid=14706989), dated 25 July 2024,
identifies members-only non-stackable object 5004, a small fishing net, visible level 33 and 75 XP.
Its embedded success chart gives frog spawn 16/96 and swamp weed 10/10. Final pre-cutoff
[swamp-weed revision
14424480](https://oldschool.runescape.wiki/w/Swamp_weed?oldid=14424480), dated 16 June 2023,
identifies members-only non-stackable object 10978, level 33, one XP and acquisition while netting
for frog spawn. Final pre-cutoff [frog-spawn/eels spot revision
14992997](https://oldschool.runescape.wiki/w/Fishing_spot_(frog_spawn,_eels)?oldid=14992997), dated
25 September 2025, pins NPCs 1497–1500/10653, their first-option Net/Small Net, third-option Bait
and exact examine. Final pre-cutoff [Game tick revision
14986190](https://oldschool.runescape.wiki/w/Game_tick?oldid=14986190) places this ordinary method
on the five-tick resource cycle.

Historical chart-module revision
[14808626](https://oldschool.runescape.wiki/w/Module:Echo_skill_success_chart?oldid=14808626)
contains the 16/96 and 10/10 pair. Its immediately preceding source revision
[14807756](https://oldschool.runescape.wiki/w/Module:Echo_skill_success_chart?oldid=14807756)
constructs the chart through an explicitly ordered cascading interpolation, so production attempts
frog spawn first and swamp weed only if that roll fails. Under the standard chart formula, the
frog-spawn roll is 43/256 at level 33; the constant swamp-weed roll is 11/256 of the remaining
213/256 failures, or about 35.8/1000 overall. This conflicts directly with the final pre-cutoff
spot table, which prints 11/256 frog spawn and approximately 43/1000 swamp weed. Final pre-cutoff
[Beta Worlds revision
14985798](https://oldschool.runescape.wiki/w/RuneScape:Beta_Worlds?oldid=14985798) corroborates
the uncertainty: its testing backlog states that frog spawn is stat-random but asks whether swamp
weed is stat-random or static. Production follows the explicit chart cascade while this conflict
blocks `verified`; manual case 79 requires level-33 and level-99 sampling on the matching client.

Final pre-cutoff [Spirit flakes revision
14908234](https://oldschool.runescape.wiki/w/Spirit_flakes?oldid=14908234) establishes a carried
50-percent double-catch chance for eligible Fishing outputs, explicitly including non-fish catches,
one flake consumed per accepted catch and no extra XP. Final pre-cutoff [Rada's blessing 4 revision
14962847](https://oldschool.runescape.wiki/w/Rada%27s_blessing_4?oldid=14962847) supplies the generic
8-percent two-output Fishing effect without extra XP; the established tier family provides
2/4/6/8 percent. Neither source names frog spawn or swamp weed individually, so their Rada
eligibility remains a bounded generic-method inference. The spirit-flake Ruins of Camdozaal
exclusion appears on a later page but not revision 14908234 and is therefore not imported into the
cache-233 baseline without the manual observation.

Cache 233.4 independently pins NPCs 1497–1500/10653, small net 303, frog spawn 5004, swamp weed
10978 and small-net sequence 621. Cache definitions confirm both outputs are non-stackable; the
fish-barrel allowlist excludes both. Production uses the ordinary protected Fishing loop with
membership, visible level, net and capacity gates; five-tick first/repeat attempts; ordered rolls;
75/1 XP; Rada/spirit-flake multiplication; and distance/tool/state revalidation. The historical
pages print no catch messages, so `You catch some frog spawn.` and `You catch some swamp weed.` are
explicit family-form inferences pending manual capture. Six cache-aware scenarios cover identifiers
and operation dispatch, method data, ordered outcomes and XP, all start gates, repeat timing,
distance/tool interruption and doubled frog spawn without a second XP award.

## 2026-09-11 — Infernal and sacred eel Fishing

The final pre-cutoff [Infernal eel revision
14817831](https://oldschool.runescape.wiki/w/Infernal_eel?oldid=14817831) establishes a members-only,
non-stackable catch requiring visible Fishing level 80, an oily fishing rod, fishing bait and worn
ice gloves. It awards 95 Fishing XP, uses success endpoints 30/93 and requires access to inner Mor
Ul Rek. The final pre-cutoff [infernal Rod Fishing spot revision
14993393](https://oldschool.runescape.wiki/w/Rod_Fishing_spot_(infernal_eel)?oldid=14993393) fixes
NPC 7676, first option `Bait`, examine `It looks like there are some large eels in the lava.`, five
spot coordinates, and accepts either the ordinary oily rod or oily pearl fishing rod. The latter's
final pre-cutoff [item revision
14935072](https://oldschool.runescape.wiki/w/Oily_pearl_fishing_rod?oldid=14935072) confirms that it
is wieldable and functions as an oily rod. Final pre-cutoff [Ice gloves revision
14990129](https://oldschool.runescape.wiki/w/Ice_gloves?oldid=14990129) identifies object 1580 and
its infernal-eel use, while [Smiths gloves (i) revision
14908813](https://oldschool.runescape.wiki/w/Smiths_gloves_(i)?oldid=14908813) establishes object
27031 and the combined ice-glove effect.

The final pre-cutoff [Sacred eel revision
14903889](https://oldschool.runescape.wiki/w/Sacred_eel?oldid=14903889) establishes a members-only,
non-stackable level-87 catch with rod and bait, 105 Fishing XP and 0/60 success endpoints. It
requires partial Regicide access to Zul-Andra and the player to have spoken to the High Priestess.
The final pre-cutoff [sacred Fishing spot revision
14993002](https://oldschool.runescape.wiki/w/Fishing_spot_(sacred_eel)?oldid=14993002) fixes NPC
6488, first option `Bait`, examine `It looks like something's moving under here.`, two coordinates
and the ordinary or pearl rod. The historical [varbit-4391 revision
14241110](https://oldschool.runescape.wiki/w/RuneScape:Varbit/4391?oldid=14241110) identifies
`PLAYER_EDUCATED_ABOUT_ZULRAH`: value 0 blocks fishing with `You know nothing of the sacred
contract between our tribe and the mighty Zulrah! Ask the High Priestess to educate you about
Zulrah before you meddle further.`; value 1 is educated, and later sacrifice values 2/3 retain
eligibility.

Cache 233.4 independently pins infernal/sacred eel objects 21293/13339, NPCs 7676/6488, rods
1585/23122 and 307/22846, bait 313, gloves 1580/27031, varbit 4391 and casting sequences 622/8188.
It also confirms both fish are accepted by the existing fish-barrel source allowlist. Production
registers both first operations in the standard protected Fishing service. It enforces membership,
boosted visible levels, tools, bait, the interchangeable infernal glove group, sacred education and
capacity; performs five-tick 30/93 or 0/60 rolls; awards 95/105 XP; and composes with existing Rada,
spirit-flake and fish-barrel behavior. Every mutable requirement is rechecked before bait or output
changes. Eight cache-aware scenarios cover identities, dispatch, rewards, every start gate, failed
rolls, wearable pearl tools, Smiths-glove substitution, mid-roll glove/permission loss and doubled
sacred-eel storage. The exact catch lines and infernal missing-glove line are ordinary-family
inferences because these pages do not print them. The final pre-cutoff [Game tick revision
14986190](https://oldschool.runescape.wiki/w/Game_tick?oldid=14986190) does not list either eel as a
special cadence; five ticks follows the historical Fishing table and established standard loop but
remains a matching-client case-80 observation.

Infernal and sacred processing are researched separately rather than implied complete by catch
support. Revision 14817831 gives infernal hammer-smashing outcomes: Tokkul 14–20 at 29/35, one onyx
bolt tip at 2/35, or one-to-five lava scale shards at 4/35. It processes the first eel in inventory
regardless of the selected eel, can automatically continue through the inventory, and manual spam
can process more than one eel in the same tick. Cache sequences are 7553 for ordinary hammering and
8969 for the Imcando variant. Sacred revision 14903889 and final pre-cutoff [Zulrah's scales revision
14868451](https://oldschool.runescape.wiki/w/Zulrah%27s_scales?oldid=14868451) establish a boostable
Cooking-72 knife-dissection gate, scale bands 3–5 at 72–79, 4–6 at 80–87, 5–7 at 88–95, 6–8 at
96–103 and 7–9 from 104 onward, and Cooking XP `100 + 3 × scales` (109–127). Manual dissection is
zero-tick; automatic continuation waits two then three ticks. Cache sequence 7151 identifies the
sacred-eel action. Cache 233.4 also pins the ordinary hammer, main-hand and off-hand Imcando hammer
objects 2347/25644/29775; Tokkul, onyx bolt tips and lava scale shards 6529/9194/11994; knife 946;
Zulrah's scales 12934; and the `Eat` fourth inventory option on both eel objects. Revision 14817831
prints the exact infernal Eat rejection. The sacred Eat line is preserved from the secondary
[Old School RuneScape Wiki Fandom mirror](https://oldschoolrunescape.fandom.com/wiki/Sacred_eel)
because the final pre-cutoff primary revision does not print it; it remains a direct matching-client
verification item rather than primary evidence.

Production now implements the fully evidenced manual infernal core and the sacred processing core.
Infernal item-on-item use validates the selected hammer and eel, processes the first eel, rolls the
exact 35-way reward table, replaces the eel atomically even in a full inventory, selects the cache
animation by hammer family and permits multiple manual requests in one tick. Sacred item-on-item
use validates a visible boosted Cooking level of 72 and knife, processes the first eel immediately,
uses the exact capped scale bands and XP formula, then continues after two ticks and every three
ticks while membership, level, knife and input remain valid. Eight cache-aware integration
scenarios cover cache/dispatch contracts, every reward and level-band boundary, targeting, spam,
membership/level/tool gates, full inventory, timing, interruption, animations, XP and both Eat
messages. The infernal automatic repeat interval is deliberately not implemented: the historical
page proves automatic continuation but does not state cadence, and all relevant sequences having a
four-tick duration is insufficient behavioral evidence. Exact failure presentation, the secondary
sacred Eat line, movement ownership, audio and packet order remain manual case 80 observations.

Final pre-cutoff [Fishing revision
14980550](https://oldschool.runescape.wiki/w/Fishing?oldid=14980550), [Spirit flakes revision
14908234](https://oldschool.runescape.wiki/w/Spirit_flakes?oldid=14908234) and [Rada's blessing 4
revision 14962847](https://oldschool.runescape.wiki/w/Rada%27s_blessing_4?oldid=14962847) supply the
shared equipment and additive multiplier rules. Neither eel page explicitly names those modifiers,
so exact multiplier eligibility remains a generic Fishing-family inference pending case 80. The
inner-Mor-Ul-Rek fire-cape entrance and Zul-Andra quest transport own geographical access; the
sacred education varbit is action-specific and therefore enforced by Fishing itself. Spot relocation,
clue-bottle/heron rolls, Eat presentation and full audiovisual/client ordering remain researched.

## Bryophyta's staff stored-nature lifecycle

The official [Bryophyta launch
update](https://oldschool.runescape.wiki/w/Update:Bryophyta:_The_Moss_Giant_Boss) establishes the
original members route: level 62 Crafting combines Bryophyta's essence with a battlestaff, creating
the staff without Crafting XP; the equipped staff may hold up to 1,000 nature runes, spends stored
runes when casting and has a 1/15 chance not to consume one. The final selected pre-cutoff
[Bryophyta's staff revision
14917258](https://oldschool.runescape.wiki/w/Bryophyta%27s_staff?oldid=14917258) corroborates the
1,000-rune cap and Attack/Magic level-30 requirements, states that the charged form is untradeable,
that Uncharge destroys all stored nature runes, and prints the conservation message `Your staff
saved you a nature rune.` It also records the 23 March 2022 free-to-play availability and Zaff's
50,000-coin creation service, the 5 March 2025 bank Configure-Charges addition, and the dangerous-
death rule that loses stored runes except one; its Ultimate Ironman wording is not precise enough to
encode the exception without a production observation.

Cache 233.4 independently pins uncharged object 22368 (`nature_staff_uncharged`) and charged object
22370 (`nature_staff_charged`). Both expose inventory `Wield` and `Check`; only 22370 exposes
`Uncharge`; both route their worn second option to Check. The cache lacks the charged/uncharged
variant params required by the shared object-charge service, so production supplies typed
`charged_variant`/`uncharged_variant` edits. Live storage uses the existing per-object
`charges_16383` varobj field, preserving independent charge counts on individual staff objects.
Cache varbit 16139 is not the live charge counter: it is `charges_bryophytas_staff_quantity`, a
permanent 11-bit slice at bits 20–30 of varp 4677 (`charges_toggle_quantity_7`). Charged object
22370 carries BANK_AUTOCHARGE param 2257 pointing at DB row 4340 (`charges_bryophytas_staff`). This
contract separates the bank's remembered loading quantity from item-local live charges; bank
configuration now has its own integration-tested preference core. The charged object consumes the
shared protected bank event, clamps count input to zero through 1,000 and persists the result in
varbit 16139; zero disables the desired quantity. Automatic withdrawal charging and nature-rune
deduction remain a distinct researched group pending manual case 81.

Production now registers nature-rune use on either inventory variant, inventory and worn Check,
and charged Uncharge. Loading consumes as many nature runes as fit up to 1,000, safely changes the
same slot to the charged identity even with a full inventory, and keeps the charge count on that
item. Uncharge removes all charges and returns the uncharged identity without returning runes. The
shared rune engine now models finite worn-object sources and takes this stored nature source after
unlimited/fake substitutions but before loose inventory and rune-pouch sources, including partial
splits. The combat rune manager validates every worn source before any mutation, rolls conservation
only for a successful cast, saves exactly one charge on the 1/15 outcome even when the spell needs
multiple nature runes, and changes a depleted worn staff to the uncharged identity with an
appearance refresh. Six cache-aware integration scenarios and two rune-engine unit scenarios cover
cache identities/options, varbit classification, script dispatch, cap/full-inventory loading,
item-local Check, destructive Uncharge, source splitting, depletion, exact conservation feedback
and failed-cast atomicity.

No primary source found in this slice prints the exact loading, full, Check or Uncharge text or
proves whether Uncharge opens a confirmation. Those production strings and direct execution are
bounded inferences. Source priority ahead of loose inventory and pouch runes is likewise a bounded
policy inference: an analogous charged nature-staff description supports stored-first behavior but
is not direct Old School evidence. Manual case 81 therefore owns exact presentation, confirmation,
packet order and priority proof. Bank Configure-Charges and trade/death handling remain researched;
manual creation and equip requirements are covered below, while their presentation and Zaff's
service stay deliberately separate researched groups.

### Manual creation, equip requirements and Zaff boundary

The same official launch update and final pre-cutoff staff revision establish the manual recipe as
one battlestaff plus one Bryophyta's essence at boostable Crafting 62, restricted to members and
awarding zero Crafting XP. The official [Equipment Rebalance Tier Changes and Poll 76
update](https://oldschool.runescape.wiki/w/Update:Equipment_Rebalance_Tier_Changes_%26_Poll_76)
establishes the later free-to-play staff and essence access, base Attack-30 and Magic-30 wield
requirements, and Zaff's alternative service for one essence plus 50,000 coins. The final cutoff
Wiki revision corroborates that Zaff has no Crafting-level requirement and that the manual recipe
remains the members route. It lists the manual creation duration as unknown; no primary source found
in this slice exposes its exact animation, messages or packet ordering.

Cache 233.4 resolves battlestaff 1391, uncharged staff 22368, charged staff 22370 and essence 22372.
It also reveals a metadata collision important to the server architecture: raw object 22368 places
Crafting 62 in its first generic stat-requirement fields even though equipping either staff requires
Attack 30 and Magic 30. Because the shared equipment handler consumes those generic fields,
production supplies typed runtime edits for the two wield requirements and keeps Crafting 62 in the
dedicated recipe policy. Both objects remain non-members in the cache, matching their free-to-play
equip access.

Production now registers battlestaff-on-essence through the existing battlestaff Crafting plugin
and its symmetric held-item dispatcher. It checks the exact selected objects, membership and current
boosted Crafting level, atomically removes both inputs and writes the uncharged staff into the
selected battlestaff slot, including when the inventory was full. It awards no XP and deliberately
does not invent an animation or delay. Five focused integration scenarios cover both item-use
orders, selected-slot and stale-input behavior, full-inventory replacement, membership and boosted
level boundaries, zero XP, cache contracts, both free-to-play base-stat wield gates and charged-item
state preservation.

Zaff is cache NPC 2880, but neither the inspected primary sources nor cache contracts identify the
exact interaction branch, dialogue, confirmation flow or inventory-capacity ordering. That service
therefore remains a distinct researched group rather than being guessed from its one-essence and
50,000-coin economic contract. Manual case 81 owns those observations and the manual recipe's exact
presentation.

## Ancient Magicks combat spells

The final selected pre-cutoff [Ancient Magicks revision
14977571](https://oldschool.runescape.wiki/w/Ancient_Magicks?oldid=14977571) establishes the shared
members-only Desert Treasure I access, Ancient spellbook, five-tick combat cadence and the four
families. The last applicable individual spell revisions pin every level, rune, base-XP and base
maximum-hit contract: Smoke [Rush
14938734](https://oldschool.runescape.wiki/w/Smoke_Rush?oldid=14938734), [Burst
14938742](https://oldschool.runescape.wiki/w/Smoke_Burst?oldid=14938742), [Blitz
14938735](https://oldschool.runescape.wiki/w/Smoke_Blitz?oldid=14938735) and [Barrage
14938736](https://oldschool.runescape.wiki/w/Smoke_Barrage?oldid=14938736); Shadow [Rush
14779876](https://oldschool.runescape.wiki/w/Shadow_Rush?oldid=14779876), [Burst
14997811](https://oldschool.runescape.wiki/w/Shadow_Burst?oldid=14997811), [Blitz
14779886](https://oldschool.runescape.wiki/w/Shadow_Blitz?oldid=14779886) and [Barrage
14779890](https://oldschool.runescape.wiki/w/Shadow_Barrage?oldid=14779890); Blood [Rush
14779879](https://oldschool.runescape.wiki/w/Blood_Rush?oldid=14779879), [Burst
14779883](https://oldschool.runescape.wiki/w/Blood_Burst?oldid=14779883), [Blitz
14779887](https://oldschool.runescape.wiki/w/Blood_Blitz?oldid=14779887) and [Barrage
14817901](https://oldschool.runescape.wiki/w/Blood_Barrage?oldid=14817901); and Ice [Rush
14779880](https://oldschool.runescape.wiki/w/Ice_Rush?oldid=14779880), [Burst
14779884](https://oldschool.runescape.wiki/w/Ice_Burst?oldid=14779884), [Blitz
14840585](https://oldschool.runescape.wiki/w/Ice_Blitz?oldid=14840585) and [Barrage
14787355](https://oldschool.runescape.wiki/w/Ice_Barrage?oldid=14787355).

The resulting exact rows are Smoke Rush 50/30/13 with 1 air, 1 fire, 2 chaos and 2 death; Shadow
Rush 52/31/14 with 1 soul, 2 death, 2 chaos and 1 air; Blood Rush 56/33/15 with 1 blood, 2 death and
2 chaos; Ice Rush 58/34/16 with 2 death, 2 chaos and 2 water; Smoke Burst 62/36/17 with 2 air,
2 fire, 4 chaos and 2 death; Shadow Burst 64/37/18 with 2 soul, 4 chaos, 1 air and 2 death; Blood
Burst 68/39/21 with 2 death, 2 blood and 4 chaos; Ice Burst 70/40/22 with 2 death, 4 chaos and
4 water; Smoke Blitz 74/42/23 with 2 blood, 2 air, 2 death and 2 fire; Shadow Blitz 76/43/24 with
2 soul, 2 blood, 2 air and 2 death; Blood Blitz 80/45/25 with 2 death and 4 blood; Ice Blitz
82/46/26 with 2 death, 2 blood and 3 water; Smoke Barrage 86/48/27 with 2 blood, 4 air, 4 death and
4 fire; Shadow Barrage 88/49/28 with 4 air, 3 soul, 2 blood and 4 death; Blood Barrage 92/51/29
with 4 death, 4 blood and 1 soul; and Ice Barrage 94/52/30 with 4 death, 2 blood and 6 water.
Numbers are written as level/base XP/base max hit, and rune order is descriptive rather than an
execution-order claim.

Burst and Barrage affect at most nine targets in the 3-by-3 square centred on the primary target's
south-west origin, only in multi-combat. A surrounding large NPC qualifies when any occupied tile
intersects that square. Accuracy is rolled independently for every selected target. Rush and Blitz
remain single-target. Smoke has a one-in-eight successful-hit poison roll: severity 10 for
Rush/Burst and 20 for Blitz/Barrage. Shadow drains Attack by 10% or 15% without stacking on an
already-lowered stat. Blood heals 25% of rolled damage across all targets and is not capped by the
victim's remaining health. Ice holds for 8/16/24/32 ticks and shares the five-tick post-release
immunity and PvP distance-release state with Standard binding spells.

The final pre-cutoff [Phantom Muspah revision
14997275](https://oldschool.runescape.wiki/w/Phantom_Muspah?oldid=14997275) explicitly lists ranged
12077, melee 12078, teleporting 12082, shielded 12079 and post-shield 12080, states that every form
has 33% freeze resistance, and scopes the resulting roughly two-thirds hold to Ice spells. Its
change history also records the 15 February 2023 fix for resistance being applied twice. The
official [Poll 78 update](https://oldschool.runescape.wiki/w/Update:Poll_78_Updates_%26_More)
corroborates that freeze resistance is the NPC percentage field. Production therefore extends an
Ancient sceptre duration first, then applies the clamped NPC percentage exactly once with integer
flooring. Ordinary 33%-resistant Rush/Burst/Blitz/Barrage holds become 5/10/16/21 ticks; a value of
100 prevents binding. The Standard and Arceuus families remain unchanged rather than inheriting an
unsupported global rule.

The cutoff [Ancient sceptre](https://oldschool.runescape.wiki/w/Ancient_sceptre) record establishes
a 10% family-effect increase: Smoke severity becomes 11/22, Shadow becomes 11%/16.5%, Blood becomes
27.5%, and Ice durations become 8/17/26/35 after integer flooring. Bloodbark adds two percentage
points of healing per piece to a 35% full-set maximum, then stacks multiplicatively with the
sceptre for 38.5%. Its four enhanced forms additionally establish poisoned-target Smoke healing
reduction of 20% for ten ticks, Shadow Strength and Defence drains, Blood overhealing to 110% of
base Hitpoints and 10% Ice accuracy on unfrozen freezable targets. The [Virtus robes
record](https://oldschool.runescape.wiki/w/Virtus_robes) establishes 5% Ancient damage per piece,
including each piece's ordinary 2%, for a 15% full-set total.

Cache 233.4 independently pins all sixteen spell objects and their spell definitions, Zaros casting
sequence 1979, the named family impact graphics and sounds, the five sceptre identities and the
three Virtus identities. Production registers the sixteen handlers through the existing
`SpellAttackMap` plugin shape, uses one protected rune transaction and cast-XP award, snapshots
equipment before delayed impacts, resolves the primary target first, scans same-type entities for
large-footprint intersection and caps the result at nine. The formula layer accepts an explicit
attack-roll percentage so the enhanced Ice sceptre can apply 110% only while the binding state is
available and NPC freeze resistance is below 100. Player healing is centrally modified while the
enhanced Smoke state is active, so food, potions, Lunar healing, natural regeneration and Blood
healing share the same rounding path.

Twelve focused integration scenarios cover the complete registry and maxima, atomic access gates,
exact rune use and casting animation, delayed Ice impact, area geometry and cap, Shadow drain and
non-stacking, Bloodbark/sceptre overheal, sceptre freeze duration, recurring NPC poison, full Virtus
damage, poisoned-player Smoke healing reduction/expiry, all four 33%-resistant Ice durations and
complete resistance. A formula integration scenario proves
that the Ice accuracy percentage scales the attack roll before hit-chance calculation.

No pre-cutoff source or distinct cache identity establishes separate travel graphics for Shadow
Burst/Barrage or Blood Burst/Barrage; production provisionally reuses the corresponding family
Rush/Blitz travel graphic and manual case 83 owns empirical confirmation. Exact secondary-player
PvP eligibility, Barbarian Assault suppression, production-wide resistance and immunity beyond
Phantom Muspah, NPC healing-reduction consumers, Shadow Sire/Leviathan exceptions, exact feedback,
audiovisual timing, target ordering and packets remain the separate researched companion group.

## Arceuus Grasp spells and Mark of Darkness

The October 1, 2025 cutoff is reconstructed from the final applicable spell records and dated
official update history. The [Arceuus spellbook](https://oldschool.runescape.wiki/w/Arceuus_spellbook)
establishes its members classification and the A Kingdom Divided combat-spell boundary, with
Ghostly Grasp (and Inferior Demonbane outside this slice) explicitly available before quest
completion. The individual [Ghostly](https://osrsindex.com/wiki/ghostly-grasp?site=osrs_wiki),
[Skeletal](https://osrsindex.com/wiki/skeletal-grasp?site=osrs_wiki) and
[Undead](https://osrsindex.com/wiki/undead-grasp?site=osrs_wiki) records pin levels 35/56/79,
base XP 22.5/33/46.5, maximum hits 12/17/24, rune costs 4 air plus 1 chaos, 8 earth plus 1 death,
and 12 fire plus 1 blood, and successful-hit bind chances 10/25/50 percent for 2/3/4 ticks. The
[Grasp-spell family record](https://osrsindex.com/wiki/grasp-spells?site=osrs_wiki) corroborates the
shared five-tick attack rate, while the [hit-delay record](https://osrsindex.com/wiki/hit-delay?site=osrs_wiki)
pins a fixed two-tick impact regardless of distance. Grasp immunity is two ticks; Swampbark does
not extend these holds. An active Mark doubles chance to 20/50/100 percent, not duration, and the
roll occurs only after accuracy succeeds.

The official [Poll 80 changes](https://oldschool.runescape.wiki/w/Update:Poll_80_ToA_Changes_%26_DMM_Tweaks)
establish Mark as a self-buff that doubles Grasp chances and boosts Demonbane spells. The final
pre-cutoff [Summer Sweep Up](https://oldschool.runescape.wiki/w/Update:Summer_Sweep_Up_Slayer_%26_More)
change fixes its ordinary duration at five minutes, or 300 game ticks. The official
[Deadman: Armageddon/WGS update](https://oldschool.runescape.wiki/w/Update:Deadman:_Armageddon_%26_While_Guthix_Sleeps_Tweaks)
establishes the Purging staff's five-times duration, snapshotted as 1,500 ticks when cast. Cache
spell metadata pins Magic 59, one soul plus one cosmic rune and 70 XP. The cache varbit
`buff_mark_of_darkness_disabled` is the separate ten-tick recast lock, not the active-buff timer.
The cutoff workspace RuneLite timer implementation corroborates the exact success line, 300-tick
base duration and Purging multiplier; the recorded warning is exactly `Your Mark of Darkness is
about to run out.` ten ticks before expiry.

Cache 233.4 independently resolves component children 170–174 for Mark, the three Grasps and Ward;
quest varbit 12296 with quest-table row 81 column 19 completion state 150; Grasp casting sequence
8972; Mark sequences 8985/8986; Mark graphics 1852, 1853 and 1886; and Ghostly/Skeletal/Undead
cast-hit-miss graphics 1856–1864. Production follows rsmod's `SpellAttackMap`, protected-access,
queued-impact and plugin-script patterns. Seventeen focused Arceuus scenarios prove contracts, typed
assets, the Ghostly quest exception, atomic access, rune/XP flow, delayed successful binding,
two-tick immunity, Mark chance propagation, distinct active/cooldown state, warning/expiry and the
Purging snapshot. A formula integration scenario independently proves the combat collector reads
the active Mark state instead of the short cooldown varbit.

The cutoff [Ward of Arceuus](https://oldschool.runescape.wiki/w/Ward_of_Arceuus) record pins Magic
73, 83 XP, one cosmic/two nature/four soul runes, members and A Kingdom Divided access, a duration
of one tick per base Magic level snapshotted on cast, a 50-tick cooldown, and one-tick incoming
Grasp binds. It also defines the floor-rounded 10% damage reduction against demons. The official
[Summer Sweep Up: Combat](https://oldschool.runescape.wiki/w/Update:Summer_Sweep_Up:_Combat)
establishes the cutoff 33% reduction to demon prayer-drain effects and multiplicative stacking with
a Spectral spirit shield. Cache 233.4 independently resolves component 174, casting sequence 8982,
graphic 1851 and cooldown varbit 12293. The workspace RuneLite timer source corroborates the exact
success line and base-level duration; client settings varbit 12409 is deliberately not treated as
server cooldown state.

Ward production follows the same protected, transactional Arceuus self-cast shape as Mark. Its
server-authoritative expiry state feeds successful enemy Grasp binding and the central player-hit
modifier, where damage is reduced by `damage / 10` only for demon sources. Focused scenarios prove
atomic free-to-play and quest rejection, exact resource/XP flow, presentation, base-level duration,
cooldown/expiry, one-tick Undead Grasp and the demon-only 19-to-18 and sub-ten rounding cases.

Grasp/Mark/Ward audio and exact packet timing, Mark end sequencing, reconstructed-session state,
production NPC resistance/immunity and full PvP/movement semantics remain researched. Ward's 33%
demon prayer-drain reduction awaits encounter consumers. Manual case 84 owns those observations;
they are not inferred from neighboring spell families.

## Arceuus Lesser and Greater Corruption

The October 1, 2025 contract is reconstructed from the final
[Corruption status](https://oldschool.runescape.wiki/w/Corruption_(status)) record and official
[Poll 80 update](https://oldschool.runescape.wiki/w/Update:Poll_80_ToA_Changes_%26_DMM_Tweaks).
The spells are members-only and require A Kingdom Divided. Lesser requires Magic 64, one death and
two soul runes and awards 75 XP; Greater requires Magic 85, one blood and three soul runes and
awards 95 XP. Both arm the caster's next successful player hit for a 50% corruption roll after the
30-August-2023 change. Mark of Darkness makes that roll guaranteed and halves the drain interval.

An affected player loses Prayer in exactly three escalating pulses: 1, 2 and 3 for Lesser, or 2,
4 and 6 for Greater. Ordinary pulses are ten ticks apart and marked pulses five ticks apart.
Corruption does not stack and has no post-expiry immunity. Ward of Arceuus clears an active status,
blocks a new one while active, and does not consume the attacker's armed Corruption when it blocks
the application. The original official
[Arceuus spell changes](https://store.steampowered.com/news/posts/?enddate=1623835264&feed=steam_community_announcements)
independently corroborate the ordinary/marked intervals, non-stacking behavior and absence of an
immunity window.

Cache 233.4 resolves Lesser/Greater component children 175/176, casting sequence 8996, effect
sequence 8997, cast graphics 1877/1878, impact graphics 1879/1880, corruption hitmark 0, client
state varbits 12136/12140/12141/12142, cooldown varbit 12288 and backing varp 2974. The workspace
RuneLite timer source independently corroborates a 30-second, or 50-game-tick, shared cooldown.

Production follows rsmod's protected self-cast and atomic rune-manager structure. A typed success
flag on the central hit record distinguishes a successful zero-damage accuracy roll from a splash,
allowing the armed effect to activate on the former without being consumed by the latter. Five
focused scenarios cover exact contracts/assets and offensive-registry exclusion, atomic access and
shared cooldown, ordinary escalating drains, Mark's guaranteed/halved behavior, splash and failed-
proc consumption semantics, and Ward cancellation/immunity/preservation.

Exact sounds and packet ordering, matching-client confirmation of zero-damage and already-corrupted
edge priorities, persistence/reconstruction and logout/death/world-transition policy, and precise
buff/cooldown display timing remain researched under manual case 86.

## Arceuus Vile Vigour

The October 1, 2025 [Vile Vigour](https://osrsindex.com/wiki/vile-vigour?site=osrs_wiki) record pins
Magic 66, three air runes, one soul rune, 76 XP, members-only and A Kingdom Divided access, and a
17-tick/10.2-second cooldown. Its final effect converts current Prayer to run energy at one Prayer
point per one run-energy percentage point, but consumes only the amount needed to reach 100%; if
Prayer is insufficient, every available point is converted. The broader
[Arceuus spellbook](https://oldschool.runescape.wiki/w/Arceuus_spellbook) record and original
[A Kingdom Divided spell poll](https://oldschool.runescape.wiki/w/Update:A_Kingdom_Divided_%26_Arceuus_Spellbook_Rework_Poll_Blog)
independently corroborate the contract and conversion purpose. The cutoff
[Runecraft training record](https://oldschool.runescape.wiki/w/Pay-to-play_Runecraft_training)
explicitly establishes that casting Vile Vigour does not interrupt running.

Cache 233.4 independently resolves spell object 15304, component 218:181, casting sequence 8978,
effect sequence 8995, cast graphic 1876 and cooldown varbit 12292. No named cache synth was found.
Production uses the same protected access and atomic rune-manager structure as neighboring Arceuus
self-casts, converts in the server's 10,000-unit run-energy scale at 100 units per Prayer point,
sends the updated run value, and clears the cooldown varbit with a typed 17-tick soft timer. Four
focused scenarios cover the cache contract/assets, exact full and partial conversion arithmetic,
cooldown boundaries, no-effect atomicity, members/quest/level/rune gates and Fountain behavior.

The no-Prayer and full-energy lines are bounded production wording, not matching-client captures.
Their exact text and check priority, cooldown feedback, cast sound, graphics/stat/var/rune/XP packet
order and interaction with active movement/action state remain researched under manual case 87.

## Arceuus Death Charge

The October 1, 2025 [Death Charge](https://oldschool.runescape.wiki/w/Death_Charge) record pins
Magic 80, one death, one blood and one soul rune, 90 XP, members-only and A Kingdom Divided access,
and a 100-tick active window/cooldown beginning on cast. A qualifying NPC death restores 15%, or
150 of the server's 1,000 special-energy units, after the caster receives the credited kill within
16 tiles. The spell suppresses a trigger when the player has participated in PvP during the prior
ten cycles, and a trigger still consumes its charge when the special bar is already full. In
ordinary multi-attacker combat the lethal hit owns the trigger; Nex instead assigns it to the
eligible player with the greatest contribution and therefore remains encounter-specific.

The final record excludes Amoxliatl ice blocks; the Moons of Peril; Nightmare and Phosani totems;
the Wardens' obelisk and Energy Siphons; Vasa Nistirio's glowing crystal; Great Olm's hands; and
Abyssal Sire respiratory systems. Production represents these known non-credit identities as an
explicit policy, rather than accepting every NPC death. Nex is also excluded until its distinct
contribution owner can be derived by the encounter.

The official [Yama release](https://oldschool.runescape.wiki/w/Update%3AYama%2C_the_Master_of_Pacts%3A_Out_Today)
establishes the cutoff Rite of Vile Transference upgrade: it is permanent, increases each cast
from one qualifying kill to two, retains the same one-minute expiry/cooldown and changes the skull
to red. The [Rite transcript and item record](https://osrsindex.com/wiki/rite-of-vile-transference?site=osrs_wiki)
pin object 30806, its `Read` operation, confirmation page/title/options and exact success line.
Cache 233.4 independently resolves spell object 15309, component 218:183, sequence 8983, NPC-kill
sequence 8984, normal graphics 1854/1855, upgraded graphics 3288/3289, cooldown varbit 12138,
active-display varbit 12411 and permanent unlock varbit 16269. The workspace RuneLite timer source
corroborates 60 seconds, active values one/two and the two-to-one transition without resetting the
timer. No named cache synth was proven.

Production uses the protected rune transaction shared by neighboring Arceuus self-casts and a new
single-publication credited-NPC-kill event emitted at the lethal-hit boundary. The spell's active
repository consumes one charge per eligible event, preserves its independent cast cooldown and
synchronizes the client active varbit from two to one to zero. The scroll route revalidates and
deletes the selected inventory slot only after confirmation, then persists the cache unlock varbit.
Seven focused scenarios cover cache and scroll contracts, exact dialogue/atomic consumption,
ordinary and upgraded casting, kill routing, range/PvP/exclusion policy, full-bar consumption,
expiry, cooldown, all access/resource gates and Fountain behavior.

Exact sound identity, failure/cooldown feedback and priority, packet ordering, active-state
reconstruction and logout/death/world-transition policy, indirect-damage attribution, the complete
encounter exclusion inventory and Nex's contribution owner remain researched under manual case 88.

## Arceuus Degrime

The October 1, 2025 [Degrime record](https://osrsindex.com/wiki/degrime?site=osrs_wiki) pins
members-only Magic 70 and A Kingdom Divided access, 83 Magic XP, an eight-tick cast interval and
batch-cleaning of every grimy herb for which the caster has the current Herblore level. Each herb
awards half of its ordinary cleaning XP. Cache 233.4 is authoritative for the final spell recipe:
object 15345 requires four earth and two nature runes, and component 218:179 exposes the Cast
target. It also resolves cast sequence 8980, preparation sequence 8981, effect sequence 9002 and
graphic 1885. No named synth reference was proven.

Production reuses the canonical Herblore cleaning table, including Huasca, and evaluates the
boosted current Herblore level once for the batch. It replaces every eligible herb in its original
slot through one inventory transaction and floors half XP independently in the server's tenths-of-
XP representation before summing. Rune validation/consumption is delegated to the shared spell
manager, Magic XP is suppressed at the Fountain of Rune while Herblore XP remains, and an isolated
eight-tick repository rejects early repeats with the sourced in-progress line. Six focused
scenarios cover cache assets, all fifteen standard herbs, per-herb rounding, boosted mixed
eligibility, exact cadence, atomic access/resource/no-effect/movement rejection and Fountain
behavior.

The documented live-client movement defect queues a cast which never completes until the player
stops and recasts. Production safely rejects while a route is active instead of reproducing the
stuck queued state. Exact no-eligible-herb wording and priority, sounds, preparation/effect staging,
packet order, action interruption and matching-client movement behavior remain researched under
manual case 89.

## Arceuus Shadow Veil

The October 1, 2025 [Shadow Veil](https://oldschool.runescape.wiki/w/Shadow_Veil) record pins
members-only Magic 47 and A Kingdom Divided access, five fire/earth/cosmic runes, 58 Magic XP and
a 50-tick cooldown. Its active lifetime is the caster's base Magic level in ticks, so temporary
boosts do not extend it. On a failed pickpocket, the veil rolls first for an exact 15% chance to
prevent both damage and stun; only after that fails may a dodgy necklace make its separate 25%
roll, producing 36.25% combined protection. Blackjacking is explicitly unaffected.

Cache 233.4 independently resolves spell object 15344, component 218:180, player sequence 8979,
cast graphic 1881, its effect sequence 8998, cooldown varbit 12291 and active varbit 12414. The
workspace RuneLite timer implementation corroborates the base-Magic-duration message trigger,
30-second cooldown and active/cooldown varbits, while also pinning the exact activation and
protection messages. No named synth reference was proven.

Production now provides the protected transactional cast, exact duration/cooldown state, client
varbit synchronization and a public server-authoritative `protects` operation which tests the 15%
boundary before any future secondary protection. Five focused scenarios cover the cache contract,
base-versus-boosted duration, exact expiry/cooldown ticks, active refresh, the 14/15 roll boundary,
atomic access/resource gates and Fountain behavior. The Man/Woman pickpocket core now consumes the
protection operation before the dodgy-necklace roll and preserves the RuneLite timer messages.
Blackjacking exclusion, exact expiry feedback, audio/packet order and matching-client timing remain
researched under manual cases 90 and 101.

## Arceuus Dark Lure

The October 1, 2025 [Arceuus spellbook](https://oldschool.runescape.wiki/w/Arceuus_spellbook)
record pins members-only Magic 50 and A Kingdom Divided access, one death rune, one nature rune
and 60 Magic XP. The official
[A Kingdom Divided poll blog](https://oldschool.runescape.wiki/w/Update%3AA_Kingdom_Divided_%26_Arceuus_Spellbook_Rework_Poll_Blog)
defines the central effect: fleeing imps, unicorns, ghouls and implings are lured back, while
creatures unable to move are invalid targets. The official
[New Client Features milestone](https://oldschool.runescape.wiki/w/Update%3ANew_Client_Features%3A_Milestone_1)
later clarifies the impling behavior: an affected impling follows the caster inside a two-by-two
area for six seconds before returning to wandering.

Cache 233.4 independently resolves spell object 15303, component 218:182, cast/travel/impact
graphics 1882/1883/1884, their sequences 8999/9000/9001 and cooldown varbit 12289. The cache marks
the spell as combat-targeted even though it must bypass ordinary damage-spell routing. No unique
human casting sequence or named synth reference was proven; the implementation's shared Arceuus
Grasp cast sequence and five-tick combat-spell cadence are bounded inferences.

Production now dispatches the cache component through a dedicated protected target spell, rejects
immobile creatures before rune consumption, uses the canonical spell registry for exact resources
and XP, launches the cache projectile and applies its impact after travel time. Attackable targets
enter the ordinary NPC-to-player attack interaction, while implings follow the caster for exactly
ten server cycles (six seconds) before their default movement resumes. Non-attackable non-impling
NPCs are never forced to follow. Six focused scenarios cover cache contracts, exact resources/XP,
presentation and cooldown state, attack routing, impling follow/expiry, unrelated-NPC safety,
atomic gates and Fountain behavior.

Exact target/failure/cooldown wording and priority, sound identity, the human cast animation,
packet ordering, fleeing-state nuances, the live impling two-by-two path, post-lure catch ownership
grace and matching-client timer behavior remain researched under manual case 91.

## Arceuus Resurrect Crops

The official
[28 January 2016 Necromancy Improvements update](https://oldschool.runescape.wiki/w/Update%3ANecromancy_Improvements)
introduces Resurrect Crops at Magic 78 with a 50% success chance at level 78 scaling to 75% at
level 99. Success returns a dead patch to its growth cycle; failure clears it. The cutoff
[Arceuus spellbook](https://oldschool.runescape.wiki/w/Arceuus_spellbook) record pins 25 earth,
8 blood, 12 nature and 8 soul runes plus 90 Magic XP, and explicitly exempts this legacy utility
spell from A Kingdom Divided. The final spell record further establishes that boosts above 99 do
not improve the chance and that a resurrected crop which dies again cannot be resurrected twice.

Cache 233.4 resolves component 218:163, whose target operation is a farming-patch location, while
the workspace RuneLite revision pins human cast sequence 7118. This repository currently has no
production Farming plugin or authoritative patch-growth/death state. Implementing only rune use,
animation and a cosmetic location change would therefore create false parity: it could not safely
restore the exact crop/growth stage, clear the patch on failure, enforce the one-resurrection
lifetime flag or integrate persistence and growth scheduling.

The spell remains researched pending a Farming patch owner. Exact integer interpolation, supported
patch/location inventory, target and failure wording, graphics/sounds, action/cooldown timing,
spellbook-swap return behavior, diary/task credit, persistence and all success/failure patch-state
transitions remain manual case 92.

## Arceuus Offering spells

The cutoff [Arceuus spellbook](https://oldschool.runescape.wiki/w/Arceuus_spellbook) record pins
Demonic Offering at Magic 84, one soul and wrath rune and 175 Magic XP, and Sinister Offering at
Magic 92, one blood and wrath rune and 180 Magic XP. Both are members-only A Kingdom Divided
spells, consume up to the first three matching remains, and award three times each remain's normal
bury or scatter Prayer XP. The official
[A Kingdom Divided poll blog](https://oldschool.runescape.wiki/w/Update%3AA_Kingdom_Divided_%26_Arceuus_Spellbook_Rework_Poll_Blog)
independently establishes Demonic Offering's demonic-ash scope and triple base scattering XP.

The final [Prayer](https://oldschool.runescape.wiki/w/Prayer) and
[Prayer training](https://oldschool.runescape.wiki/w/Pay-to-play_Prayer_training) records establish
one restored Prayer point per offered remain, except infernal ashes and Ourg, Dagannoth, hydra and
superior dragon bones restore two each. The cutoff Zealot-robes record explicitly excludes both
offering spells from preservation. The Fountain of Rune supplies the runes and suppresses Magic XP
without suppressing remains consumption, Prayer XP or Prayer restoration.

Cache 233.4 resolves spell objects 15346/8796, components 218:177/178, shared cooldown varbit
12423, cast sequence 8975 with a three-tick duration, preparation sequence 8976 with a five-tick
duration, graphics 1871/1872 and effect sequences 8992/8991. The June 2021 change record makes the
spells instant and permits Sinister Offering without interrupting other actions. The eight-cycle
shared recast interval is a bounded reconstruction from the cache's five-tick preparation plus
three-tick cast durations and the shared cooldown state, rather than a directly quoted server
constant.

Seven integration scenarios pin both spell registries and presentations, exact top-left three-item
selection, every canonical `PrayerRemainsRecipes` bone and ash, per-item triple XP, special
restoration, the shared boundary, non-interrupted routing, access/level/rune/no-remains atomicity
and Fountain semantics. Exact no-remains and cooldown wording/priority, whether sequence 8976 is
ever transmitted after the instant-cast update, sound and packet order, cooldown persistence and
matching-client animation cadence remain manual case 93.

## Arceuus destination teleports

The cutoff [teleportation-spell table](https://oldschool.runescape.wiki/w/Teleportation_spells) and
[Arceuus spellbook](https://oldschool.runescape.wiki/w/Arceuus_spellbook) establish twelve
members-only destination teleports. They do not require A Kingdom Divided. Their level, rune and
Magic-XP rows are Library 6 / two earth plus one law / 9; Draynor 17 / law, water and earth / 16;
Battlefront 23 / law, earth and fire / 19; Mind Altar 28 / law plus two mind / 22; Respawn 34 / law
plus soul / 27; Salve 40 / law plus two soul / 30; Fenkenstrain 48 / law, soul and earth / 50; West
Ardougne 61 / two law plus two soul / 68; Harmony 65 / law, soul and nature / 74; Cemetery 71 / law,
soul and blood / 82; Barrows 83 / two law, two soul and blood / 90; and Ape Atoll 90 / two law, two
soul and two blood / 100. The official
[A Kingdom Divided poll blog](https://oldschool.runescape.wiki/w/Update%3AA_Kingdom_Divided_%26_Arceuus_Spellbook_Rework_Poll_Blog)
independently pins the proposed replacement Library spell at level 6, two earth plus one law and 9
XP. That direct contemporary source and the dated Library-spell record control over a stale
transportation summary that reported 10 XP; the local spell overlay was corrected to 9 XP and the
conflict is retained here rather than silently discarded.

Cache 233.4 resolves objects 20409, 20410, 22779, 20411, 8092, 20412, 20413, 20414, 20419, 20420,
20404 and 20427 to components 218:150, 154, 166, 156, 157, 158, 159, 160, 161, 162, 164 and 165.
The `spell_telecoord` values pin the eleven fixed destinations; Respawn intentionally has no such
parameter. Sequence 6575 and spot animation 1296, backed by effect sequence 7162, each last three
ticks, while the individual spell records specify a four-tick cast. The shared Arceuus teleport
sound is synth 200 (`teleport_all`).

The cutoff teleport records and cache quest rows pin Priest in Peril state 60 for Salve,
Fenkenstrain and Barrows; Biohazard state 16 for West Ardougne; The Great Brain Robbery state 130
for Harmony Island; and Monkey Madness I state 9 for Ape Atoll. The November 2022 Ape Atoll change
removed the older Daero-training requirement, so varbit 123 is deliberately not gated. The direct
Morytania record also describes having heard Drezel's warning as a possible edge for the three
Morytania spells, but its distinct cache state and exact threshold are not yet proven and are not
claimed by the core.

Eight integration scenarios pin all twelve registries, cache contracts, exact costs/XP, fixed
destinations, the configured realm Respawn destination, four-tick arrival, sequence/graphic/sound,
all six quest-boundary groups, members/spellbook/level/rune atomicity, absence of an A Kingdom
Divided gate, Tele Block and level-20 Wilderness boundaries, staff substitution, Fountain behavior
and operation-1-only dispatch. This repository currently exposes only a realm-wide configured
respawn coordinate and the death path does not persist the player's seven selectable standard
respawn destinations. Per-player selection, Defence-cape exclusion, Drezel-warning state, exact
messages/priorities, destination distribution, lifecycle races and client packet order remain
manual case 94.

## Arceuus Demonbane spells

The October 1, 2025 contract is reconstructed from the final applicable
[Demonbane family record](https://osrsindex.com/wiki/demonbane-spells?site=osrs_wiki) and the
individual [Inferior](https://oldschool.runescape.wiki/w/Inferior_Demonbane),
[Superior](https://oldschool.runescape.wiki/w/Superior_Demonbane) and
[Dark](https://osrsindex.com/wiki/dark-demonbane?site=osrs_wiki) records. They pin levels
44/62/82, cast XP 27/36/43.5, base maxima 16/23/30, a shared five-tick combat cadence, and the
final 3/5/7 fire-rune costs with 1 chaos/1 soul/2 soul. The dated 22 May 2025 change history is
decisive over stale pages that still show the original 4/8/12 fire costs. All three are members-only
and target only demonic creatures; Inferior, like Ghostly Grasp, is explicitly exempt from the
A Kingdom Divided requirement.

The family and [Purging staff](https://oldschool.runescape.wiki/w/Purging_staff) records establish
a normal +20% spell-accuracy modifier, +40% while Mark is active, and Purging-staff doubling to
+40% without Mark or +80% with it. The official
[9 April 2025 Farming & Autocast update](https://oldschool.runescape.wiki/w/Update:Farming_%26_Autocast_QoL_Improvements)
is the controlling cutoff correction: these are additive percentages applied once after preceding
gear/task stages, not the older accidentally compounded calculation. Mark adds 25% damage after
gear, or 50% with Purging. The
[maximum-magic-hit formula](https://oldschool.runescape.wiki/w/Maximum_magic_hit) additionally
pins nested rounding for effectiveness: first floor the Mark-added portion, multiply only that
portion by the target's demonbane effectiveness, floor again, then add it to the rolled damage.
This distinguishes a 70%-effective target from merely scaling the whole final maximum.

The [fixed hit-delay record](https://osrsindex.com/wiki/hit-delay?site=osrs_wiki) assigns every
Demonbane spell a two-tick impact independent of distance. Cache 233.4 independently resolves
spellbook component children 167–169; casting sequence 8977; effect sequences 8993/8994; tiered
cast/hit graphics 1865–1870; the `demon` and `demonbane_resistant` params; and quest-table row 81,
column 19 completion state 150. Production follows the same rsmod `SpellAttackMap`, protected rune
transaction and queued-hit pattern as adjacent spell families. Fourteen Arceuus integration
scenarios now include all Demonbane contracts and typed assets, Inferior's quest exception,
atomic non-demon/player rejection, exact rune and cast-XP use, tier presentation, fixed impact and
active-Mark/Purging propagation. Formula unit scenarios independently lock all ordinary, marked,
Purging and 70%-effective accuracy/damage combinations.

No pre-cutoff source found during this pass pins the exact rejection message or its priority against
membership, quest, level and rune failures; the production sentence is therefore provisional, not
claimed exact. The cache identifies Demonbane effect sequences but no named synth reference was
proven. Exact audio/packet timing, permitted-autocast weapon presentation and persistence, the full
cache-233.4 demonic/effectiveness catalog, effectiveness above 100% (including Yama), and
Tormented-Demon/other encounter overrides remain researched under manual case 85.

## Arceuus resurrection thralls

The official
[A Kingdom Divided and Arceuus spellbook poll](https://oldschool.runescape.wiki/w/Update%3AA_Kingdom_Divided_%26_Arceuus_Spellbook_Rework_Poll_Blog)
defines the three resurrection tiers and their ghost/magic, skeleton/ranged and zombie/melee forms.
The cutoff [Necromancy-spell](https://oldschool.runescape.wiki/w/Necromancy_spell) and
[Thrall](https://osrsindex.com/wiki/thrall?site=osrs_wiki) records pin members-only A Kingdom
Divided and Book of the Dead access, one active thrall, current-Magic duration, four-tick attacks,
invulnerability, automatic targeting of the caster's NPC opponent, no player attacks, guaranteed
accuracy with possible zero damage, flat-armour bypass and player-attributed damage. The direct
[Lesser Skeleton revision](https://oldschool.runescape.wiki/w/Resurrect_Lesser_Skeleton?oldid=14407406),
[Greater Zombie](https://oldschool.runescape.wiki/w/Resurrect_Greater_Zombie) and
[Magic XP table](https://oldschool.runescape.wiki/w/Magic/Experience_table) establish levels
38/57/76, maxima 1/2/3, Prayer costs 2/4/6, XP 55/70/88 and the three exact rune recipes. The
[25 June 2025 combat sweep](https://oldschool.runescape.wiki/w/Update%3ASummer_Sweep_Up%3A_Combat)
controls at this cutoff: completed Master Combat Achievements double duration and the former 50%
reward reduction no longer applies. RuneLite's cache-233 `ca_tier_status_master` enum independently
identifies value 2 as complete.

Cache 233.4 resolves spell objects 25506-25514 to components 218:184-192 and NPCs 10878-10886.
The NPC records have attack-rate parameter 14 value 4 and no player operations; target parameter
`thrall_immunity` supplies the kurask/turoth-era immunity seam. Client state is represented by
cooldown/disabled/used/active varbits 12290/12410/12412/12413, with used values 0-8 ordered lesser
ghost/skeleton/zombie through greater ghost/skeleton/zombie. The cache further pins human sequence
7198 and graphic 1288, spawn sequences 9046-9048, candidate attack sequences 5540/5512/5568 and
ranged/magic travel plus magic-impact graphics 1906/1907/1908.

Production now registers all nine spellbook buttons through the standard spell and rune
repositories, validates membership, quest, book, Prayer, level and runes transactionally, and
creates one finite owner-bound NPC at an open nearby square. Duration snapshots current Magic
before cast XP and doubles only for Master completion. Recast replaces the old thrall after a
17-cycle cooldown. The NPC follows its owner, mirrors only genuine offensive NPC targets, attacks
every four cycles, ignores accuracy and flat armour, accepts a zero roll, respects the cache
immunity parameter, attributes damage to the player without normal combat XP, and is itself
damage-immune. Nine focused integration scenarios cover every tier/style, state and failure gate.

The cooldown evidence conflicts: the direct infobox records 16 ticks while its prose says ten
seconds, which is 16.67 game cycles. Production follows the ten-second description with 17 cycles
until a cache-233 client capture resolves the boundary. The exact restricted-location inventory is
well documented, but this server does not yet expose authoritative ownership for every named
instance; inventing rectangles would overclaim parity. Room-constrained following, exact spawn
priority, feedback, attack range, the inferred attack-animation/projectile mapping, sounds, impact
and packet order, and lifecycle persistence therefore remain researched under manual case 96.

## Standard Mining rock lifecycle

The cutoff [Mining](https://oldschool.runescape.wiki/w/Mining) and direct rock records establish the
ordinary one-ore/depletion lifecycle. The direct records are
[clay](https://oldschool.runescape.wiki/w/Clay_rocks),
[copper](https://osrsindex.com/wiki/copper-rocks?site=osrs_wiki),
[tin](https://osrsindex.com/wiki/tin-rocks?site=osrs_wiki),
[blurite](https://osrsindex.com/wiki/blurite-rocks?site=osrs_wiki),
[iron](https://oldschool.runescape.wiki/w/Iron_rocks),
[silver](https://oldschool.runescape.wiki/w/Silver_rocks),
[coal](https://osrsindex.com/wiki/coal-rocks?site=osrs_wiki),
[gold](https://osrsindex.com/wiki/gold-rocks?site=osrs_wiki),
[mithril](https://osrsindex.com/wiki/mithril-rocks?site=osrs_wiki),
[adamantite](https://oldschool.runescape.wiki/w/Adamantite_rocks) and
[runite](https://oldschool.runescape.wiki/w/Runite_rocks). In that order they pin level and XP as
1/5, 1/17.5, 1/17.5, 10/17.5, 15/35, 20/40, 30/50, 40/65, 55/80, 70/95 and 85/125. Normal
respawns are 1.2, 2.4, 2.4, 25, 5.4, 60, 30, 60, 120, 240 and 720 seconds. On the 600 ms
[game clock](https://oldschool.runescape.wiki/w/RuneScape_clock), production represents those as
2, 4, 4, 42, 9, 100, 50, 100, 200, 400 and 1,200 server cycles. Blurite's published 25 seconds is
not cycle-exact; 42 cycles is 25.2 seconds and remains a bounded rounding decision pending case 97.

The same records supply the 1/256 success endpoints used by the server's existing `statRandom`
interpolation: clay 128/400; copper and tin 100/350; blurite 90/350; iron 96/350; silver 25/200;
coal 16/100; gold 7/75; mithril 3/50; adamantite 0/25; runite 10/18. These are stored as integer
low/high parameters and evaluated against the current effective Mining level, including the shared
invisible-level provider. Endpoint arithmetic inherits rsmod's tested stat-random convention;
the records' rounded chart examples are corroboration, not a replacement formula.

Cache 233.4 identifies the two ordinary visual variants per family as locations 10943, 11161 and
11360–11379, with empty replacements 11390/11391. Their first operation is `Mine`; the ore rocks
are single-tile centrepieces with blocking and no multi-location transform. The cache also pins the
bronze, iron, steel, black, mithril, adamant, rune and gilded pickaxes, their wielded/carried object
identities and mining sequences. The cutoff clock record gives intervals 8/7/6/5/5/4/3/3 cycles;
the cache-backed requirements are 1/1/6/11/21/31/41/41. Production uses rsmod's content groups,
typed object/location parameters, protected operations, location repository and local overlay
client script rather than a parallel skill framework.

Two cache-contract scenarios cover all 22 rocks and all eight pickaxes. Four gameplay scenarios
cover unusable and best usable carried/equipped pickaxes, rock-level rejection, swing and repeated
roll timing, exact ore/XP award, depletion/respawn, and full-inventory atomicity. This is expressly
the standard core: [Mining gloves](https://oldschool.runescape.wiki/w/Mining_gloves), Mining Guild
boosts and respawns, special pickaxes, ordinary-rock gem and other tertiary rolls,
outfit/cape/armour effects, quest and Tutorial Island variants, area/member exceptions, contention,
exact audiovisual/packet behavior and matching-client evidence remain researched under manual case
97. Standard gem-rock extraction is tracked as its own core below.

## Special Mining pickaxe rate and access core

The cutoff [Mining](https://oldschool.runescape.wiki/w/Mining),
[RuneScape clock](https://oldschool.runescape.wiki/w/RuneScape_clock) and
[pay-to-play Mining training](https://oldschool.runescape.wiki/w/Pay-to-play_Mining_training)
records distinguish ordinary fixed intervals from the fast special families. Dragon, Infernal and
third-age pickaxes make the next ore roll after two cycles with probability 1/6 and otherwise after
three, for an exact expected interval of 17/6 cycles (2.83 recurring). An active crystal pickaxe uses
the same two-or-three-cycle shape with probability 1/4, for 11/4 cycles (2.75); an inactive crystal
pickaxe retains dragon-equivalent timing. This is modeled as a per-roll random interval, not as a
rounded constant, and tool priority compares those rational expected intervals exactly.

The direct [dragon pickaxe](https://oldschool.runescape.wiki/w/Dragon_pickaxe),
[Infernal pickaxe](https://oldschool.runescape.wiki/w/Infernal_pickaxe) and
[crystal pickaxe](https://oldschool.runescape.wiki/w/Crystal_pickaxe) records pin level 61 for the
dragon-equivalent families, level 71 plus Song of the Elves for both crystal states, and members-only
access. Cache 233.4 supplies the complete production identity set: dragon 11920/12797/23677/25376/
30351, Infernal 13243/13244/25063/25369/30345/30346, third-age 20014 and crystal 23680/23682. It
also pins family sequences 642, 4482, 7139, 7283, 8346, 8347, 8887 and 11831–11833. Production
therefore recognizes ornamented, Trailblazer and Trailblazer Reloaded forms rather than silently
falling back to a base object.

One cache-contract scenario covers all 14 identities, intervals, rate denominators, animations,
membership and crystal quest metadata. Three live scenarios cover members and Song of the Elves
boundaries, the fast two-cycle dragon branch and active-crystal priority over dragon. The creation
and ornament subset of the Infernal lifecycle is tracked separately below. Its passive, depletion,
recharge and Check presentation, plus crystal charge handling and matching-client timing, remain
researched under manual case 97. Rock Knocker is tracked as a separate completed core below.

## Cache-233 crystal pickaxe bank preference

Cache 233.4 gives active crystal pickaxe 23680 BANK_AUTOCHARGE param 2257 pointing to shared
crystal-tools DB row 4338. Its account-persistent target quantity is varbit 16148 over permanent
varp 4681 bits 0–15. Production registers active and inactive identities for the typed protected
event; the bank's parameter validation exposes the option only on an advertising object. The
members-only dialogue clamps input to zero through 20,000, with zero disabling the preference.

Two focused integration scenarios pin the cache row, permanent variable layout, event routes,
upper-bound storage and non-member preservation. Item-local charge state, shard and Revert actions,
automatic withdrawal charging, exact presentation and packet order remain manual case 97.

## Infernal pickaxe creation and ornament lifecycle core

The pre-cutoff [Infernal pickaxe revision 14986642](https://oldschool.runescape.wiki/w/Infernal_pickaxe?oldid=14986642)
and Jagex's historical [Slayer Expansion v2 development blog](https://oldschool.runescape.wiki/w/Update:Dev_Blog:_Slayer_Expansion_v2)
establish the standard tradeable dragon-pickaxe-only recipe, unboostable level 61 Mining and level
85 Smithing requirements, three-tick duration, 200 Mining and 350 Smithing XP, 5,000 initial
charges and irreversible base conversion. The cutoff item record also supplies the exact rejection
for a cosmetic dragon pickaxe: “The cosmetically upgraded pickaxe is too beautiful to convert into
an infernal pickaxe.” Production registers every cache-233 cosmetic dragon identity against the
stone so none silently bypasses that rule.

The pre-cutoff [Trailblazer ornament revision 14820358](https://oldschool.runescape.wiki/w/Infernal_pickaxe_(or)_(Trailblazer_Reloaded)?oldid=14820358)
and [Reloaded ornament revision 14853053](https://oldschool.runescape.wiki/w/Infernal_pickaxe_(or)?oldid=14853053)
pin charged/depleted identities 25063/25369 and 30345/30346, the one-tick ornament recipe, kit
return on Revert, and Change-Style in inventory and while worn. Cache 233.4 independently pins the
shifted Wield/Check/Revert/Change-Style operation arrays and worn operations. The implementation
uses the shared `infernal_tool_charges` var-object field, treats a zero-packed charged item as 5,000,
and preserves the packed value through ornament, revert, equip and both style directions.

Seven integration scenarios pin all six identities and operation arrays, event dispatch, symmetric
item-on-item ordering, three-tick/full-inventory creation, both unboostable level boundaries,
post-delay input revalidation, all four cosmetic-dragon rejections, charged/depleted ornament and
atomic revert behavior, inventory/worn style changes, and exact charge packing/depletion. This row
does not claim the one-third passive, the cutoff page's explicitly incomplete per-rock Smithing-XP
table, depleted-tool recharge timing/feedback, Check wording, Ghommal's lucky penny, special attack,
or audiovisual/packet order. In particular, the cutoff XP table lists 6.25 for iron and 18.75 for
adamantite even though the server stores tenths and floors fine XP, and omits coal; later/current
tables are not imported into the 233.4 baseline without separate evidence.

## Mining Rock Knocker special-attack core

The final pre-cutoff [Special attacks revision 14994987](https://oldschool.runescape.wiki/w/Special_attacks?oldid=14994987)
lists Rock Knocker for dragon, Infernal, third-age and crystal pickaxes. It costs 100% special energy
and temporarily raises Mining by three levels. Its temporary-event table also explicitly includes
both the original Trailblazer and Trailblazer Reloaded pickaxes with the same effect and cost. The
direct dragon-pickaxe record supplies the “Smashing!” overhead phrase.

Cache 233.4 enum `sa_energy_requirements` independently maps all fourteen supported object states to
1,000 energy units: dragon 11920/12797/23677/25376/30351, Infernal 13243/13244/25063/25369/30345/
30346, third-age 20014, and active/inactive crystal 23680/23682. This cache evidence resolves the
otherwise ambiguous inactive-crystal state in favor of retaining Rock Knocker. The cache supplies
special sequences 7138 (base), 334 (upgraded), 8330 (Zalcano), 11821 (Trailblazer), 11822
(Trailblazer Reloaded), 3410 (Infernal), 7267 (third-age) and 8329 as the sole crystal Rock Knocker
sequence. Applying sequence 8329 to the inactive crystal state is a bounded same-family inference
pending workspace RuneLite case 97.

Production now registers every cache-mapped form, including charged/depleted Infernal ornament
states, and uses rsmod's existing instant-special repository. Activation applies the bounded +3
Mining boost, queues the exact family sequence and overhead phrase, and returns success so the
combat-tab consumer deducts the full 1,000-unit bar. The production plugin script delegates cache
loading and installed-map registration to one shared bootstrap, and an integration suite invokes
that same bootstrap before exercising all fourteen forms. Together with the focused unit scenarios,
it pins every identity, energy entry, registry shape, sequence route, boost cap, phrase and deduction
contract. Exact special-bar failure presentation, found-gem sound placement and client packet order
remain manual case 97 rather than being inferred from server state.

## Standard gem-rock extraction core

The final pre-cutoff [Gem rocks revision 14972723](https://oldschool.runescape.wiki/w/Gem_rocks?oldid=14972723),
dated 23 August 2025, pins the ordinary extraction contract at level 40, 65 Mining XP and a
59.4-second respawn. On rsmod's 600 ms clock that is exactly 99 cycles. Its success chart supplies
the level-1/99 endpoints 28/70 on the shared 1/256 interpolation, and states that a charged amulet
of glory triples the success chance. Production therefore multiplies both endpoints to 84/210 only
for gem rocks and only for charged ordinary or trimmed glories 1–6 and the eternal glory; both
uncharged identities and every non-gem rock keep their original endpoints.

The same cutoff revision supplies the exact 128-outcome reward table: uncut opal 60, jade 30, red
topaz 15, sapphire 9, emerald 5, ruby 5 and diamond 4. The implementation stores those integer
weights directly and tests every first and last boundary rather than converting them to rounded
percentages. Cache 233.4 independently identifies ordinary Gem rocks as locations 11380/11381
(`gemrock1`/`gemrock`), their model-matched empty replacements 11390/11391, all seven uncut object
identities and source-named synth 2655 (`found_gem`). Both rocks retain their cache `Mine` operation
and use the existing Mining protected-operation, typed-parameter, replacement and local-overlay
structure.

Focused integration covers both cache identities and metadata, the complete weight partition,
level rejection, success reward/65 XP/sound, inclusive 99-cycle replacement lifecycle, full
capacity, all thirteen charged glory states and explicit uncharged/non-gem exclusions. The Wiki
also says mining enhancers do not affect gem rocks and lists clue geodes and the rock golem as
conditional rewards; those cross-system rolls, area/quest access and exact client presentation are
not part of this core. The capitalized `You just mined a/an …!` success strings and the full-capacity
string are provisional compatibility choices pending workspace RuneLite case 98. No separately
installed RuneLite client is acceptable evidence.

## Ordinary-rock Mining gem pre-table core

The final pre-cutoff [Mining revision 14991661](https://oldschool.runescape.wiki/w/Mining?oldid=14991661)
and [Gems revision 14466622](https://oldschool.runescape.wiki/w/Gems?oldid=14466622) establish that
ordinary Mining rolls for a gem before each ore roll. If that pre-table is selected, its result
replaces the ore attempt: the rock is not depleted and no Mining XP is awarded. The final
cache-cutoff [MiningPreGemDropTable revision 14994095](https://oldschool.runescape.wiki/w/Template:MiningPreGemDropTable?oldid=14994095)
supplies the exact integer model rather than the older rounded prose. A normal attempt selects one
of 128 pre-table entries out of 32,768; a charged ordinary/trimmed/eternal glory selects one out of
8,216, equivalent to the published recurring 1/64.2 table probability. Both zero-charge glory
identities retain the normal denominator.

The selected 128-entry table is ordered as 70 nothing, 32 uncut sapphire, 16 uncut emerald, 8 uncut
ruby and 2 uncut diamond. The nothing entry still consumes the ore attempt, so it cannot be modeled
as a failed pre-roll followed by the ordinary success formula. Production preserves both random
stages and runs them before `statRandom`; a selected table entry never publishes an ore, advances
XP or changes the location. A gem result emits the pre-cutoff Gem-bag transcript's
`You just found a/an [gem]!` shape and the cache's source-named synth 2655, then continues the
existing Mining action at the same pickaxe cadence.

The focused cache/game tests pin both denominators, every table boundary, all thirteen charged
glory states, both uncharged states, the inclusive threshold, a diamond result, the nothing result,
zero XP, no ore and no depletion. Gem rocks are explicitly excluded because their success and
reward table are the separate core above. The cutoff [Gem bag transcript revision
14860843](https://oldschool.runescape.wiki/w/Transcript:Gem_bag?oldid=14860843) also proves that an
open bag may intercept Mining gems and adds a second message. That route is now implemented by the
separate gem-bag core below. Exact lowercase rendering, sound placement, F2P persistence of
already-worn members jewellery and matching-client packet order remain manual case 99.

## Gem bag lifecycle and Mining routing core

The final pre-cutoff [Gem bag revision 14946679](https://oldschool.runescape.wiki/w/Gem_bag?oldid=14946679),
dated 24 July 2025, pins members-only object identities 12020 closed and 24481 open; capacity of 60
each uncut sapphire, emerald, ruby, diamond and dragonstone; Fill or one-gem use; Check; inventory
and bankside Empty; and open-bag acquisition routing from ground pickup, Mining and Thieving. It
also specifies sapphire-first inventory emptying and says cutoff contents are retained on death.
Cache 233.4 independently pins both identity operation arrays, the bankside `Empty` operation and
all five accepted object identities. The permanent, never-stacking 300-slot backing inventory
models the five independent 60-item caps without collapsing types or tying contents to either
visible bag variant.

The final pre-cutoff [Gem bag transcript revision 14860843](https://oldschool.runescape.wiki/w/Transcript:Gem_bag?oldid=14860843)
pins Open/Close, both Check lines, nothing-suitable, uncut-only, per-type-full and direct-acquisition
feedback. The page's templated add text is represented as singular and plural forms. Eight focused
integration scenarios cover cache contracts, independent caps, Fill, symmetric gem use, cut-gem
rejection, state transformation, Check, sapphire-first partial inventory empty, bankside empty,
membership, Destroy, storage clearing and same-tile/delayed ground pickup. They also prove that a
closed bag falls back to ordinary inventory and a full per-gem bag preserves ordinary Take failure
semantics. Mining integration separately proves that an open bag intercepts both an ordinary pre-
table diamond and a standard gem-rock diamond and emits the second transcript message.

Ground Take now uses a reusable, deterministic destination policy and atomically redirects only a
complete eligible stack; ordinary inventory remains the fallback for closed, free-world or full-
capacity cases. The initial Man/Woman Thieving core awards only coins, so gem-bearing pickpocket
families still do not call the shared acquisition hook. Exact free-world wording,
partial-empty/full-bank presentation, one-bag
ownership and shop enforcement, destroy dialogue, death/recovery behavior and client packet order
remain manual case 100. A later/current Wiki
revision says contents are emptied on death, conflicting with the cutoff record; the cache-233.4
implementation follows the cutoff and retains permanent contents pending matching-client evidence.

## Thieving Man/Woman pickpocket and citizen coin-pouch core

The final pre-cutoff [Thieving revision 14991198](https://oldschool.runescape.wiki/w/Thieving?oldid=14991198),
[Man revision 14941419](https://oldschool.runescape.wiki/w/Man?oldid=14941419), and
[Woman revision 14984028](https://oldschool.runescape.wiki/w/Woman?oldid=14984028) pin the entry-level
contract: members-only level 1, 8 Thieving XP, three coins, one damage on failure and a five-second
stun. The cutoff pickpocket table supplies the exact level-1/99 success endpoints 180/240, with
189/252 for gloves of silence, 198/264 for the applicable Ardougne Diary bonus and 217/290 when
the diary bonus and Thieving cape stack. Production uses the native 1/256 `statRandom` model and
an exact two-cycle successful-attempt interval.

The cutoff [coin pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
pins the shared carried-pouch caps at 28, 56, 84 and 140 for no diary, medium, hard and elite
Ardougne Diary completion. Citizen pouches open atomically into three coins each through both
`Open` and `Open-all`; the cap counts every one of the twenty cache pouch identities rather than
only the citizen variant. Cache 233.4 independently pins citizen pouch 22521, pickpocket sequence
881, stun sequence 1054, stun spotanim 245 and the coin-jingle synth 2114. Workspace RuneLite source
pins synths 4398/4974 and the exact protection messages used by its timers; only this repository's
sibling RuneLite checkout was read.

The final pre-cutoff [Stun revision 14953416](https://oldschool.runescape.wiki/w/Stun_(status)?oldid=14953416)
and [Shadow Veil revision 14964859](https://oldschool.runescape.wiki/w/Shadow_Veil?oldid=14964859)
establish that a failed pickpocket rolls Shadow Veil's 15% first, then a dodgy necklace's 25%, for
36.25% combined protection; a successful veil roll suppresses the NPC shout. Ordinary failure now
applies the shout, one typeless damage, an eight-cycle action block and separate nine-cycle movement
block with the cache animation/graphic. The necklace stores and consumes its ten charges on the
item and crumbles on the last protection.

Ten focused scenarios cover all 27 cache-operable historical Man/Woman identities, cache operation
contracts, success cadence, XP/pouch/audio reward, queued failure damage, action and movement blocks,
veil-before-necklace ordering, charge consumption, all pouch limits and operations, chance modifiers,
capacity and membership. Historical Man identity 8858 is deliberately not registered: the cutoff
page lists it, but cache 233.4 contains no pickpocket operation for that type. The implementation
follows the target cache and records the temporal conflict instead of manufacturing an operation.
Ardougne-medium locality, gem-bag rewards from later NPC families, all other pickpocket targets,
stalls, chests, blackjacking and exact matching-client packet/timing behavior remain open under
manual case 101.

## Farmer pickpocket and Rogue reward-multiplier core

The final pre-cutoff [Farmer revision 14941540](https://oldschool.runescape.wiki/w/Farmer?oldid=14941540)
pins level 10, 14.5 XP, one damage, a five-second stun, 150/240 base success endpoints and a
128-weight reward table: a nine-coin Farmer pouch at 123/128 and one potato seed at 5/128. It also
pins the exact 157/252 Gloves-of-silence, 165/264 applicable-diary and 181/290 diary-plus-cape
endpoints. The dated [Varlamore Farmer revision 14949324](https://oldschool.runescape.wiki/w/Farmer_(Varlamore)?oldid=14949324),
[Kastori Farmer revision 14956027](https://oldschool.runescape.wiki/w/Farmer_(Kastori)?oldid=14956027)
and [Tal Teklan Farmer revision 14952807](https://oldschool.runescape.wiki/w/Farmer_(Tal_Teklan)?oldid=14952807)
all route back to that same pickpocket contract.

Cache 233.4 exposes `Pickpocket` in operation slot three on exactly twenty Farmer identities:
3114, 3243, 3244, 11918–11921, 13228–13235, 14751–14754 and 14773. It independently pins Farmer
pouch 22522 and potato seed 5318. Production registers all twenty through the same typed definition,
selects the exact 123/5 weighted boundary, applies the shared pouch ceiling, and opens the Farmer
pouch atomically into nine coins through both `Open` and `Open-all`.

The final pre-cutoff [Rogue equipment revision 14985965](https://oldschool.runescape.wiki/w/Rogue_equipment?oldid=14985965)
pins a 15/30/45/60% double-loot chance for one through four correctly worn pieces and a guaranteed
double with all five pieces. XP is never doubled. For coin-pouch rewards, the special rule is one
pouch plus its coin value directly, rather than two pouches; ordinary rewards such as Farmer potato
seeds receive twice their object count. Cache 233.4 pins the five worn objects at 5553–5557.
Production evaluates the exact equipment slots, so replacing Rogue gloves with Gloves of silence
leaves four Rogue pieces and a 60% reward chance while still applying the Gloves success modifier.
All multi-output rewards are one inventory transaction and roll back without XP if every output
cannot fit.

Ten additional focused scenarios cover the complete Farmer cache family and operation contracts,
reward-table boundaries, level/XP/chance endpoints, nine-coin pouch exchange, every Rogue outfit
piece threshold, the 14/15 random boundary, guaranteed pouch and ordinary-object doubling, unchanged
XP and atomic capacity rollback. Exact rare-reward/full-inventory presentation and packet order
remain a matching-client observation in manual case 101.

## Warrior and ordinary Guard pickpocket cores

The final pre-cutoff [Warrior (Thieving) revision 14984034](https://oldschool.runescape.wiki/w/Warrior_(Thieving)?oldid=14984034)
pins the ordinary Warrior contract at level 25, 26 XP, an 18-coin Warrior pouch, two damage, a
five-second stun and 100/240 success endpoints. The dated
[Al Kharid warrior revision 14954129](https://oldschool.runescape.wiki/w/Al_Kharid_warrior?oldid=14954129)
confirms that its pickpocketable identity uses the same table. The modifier chart gives exact
105/252 Gloves-of-silence, 110/264 applicable-diary and 121/290 diary-plus-cape endpoints. Cache
233.4 exposes operation-slot-three `Pickpocket` on exactly seven applicable identities: 3260,
3292 and 11925–11929, and pins Warrior pouch 22524. Production registers all seven through one
typed definition and atomically opens one or all pouches into 18 coins each.

The final pre-cutoff [Guard revision 14970362](https://oldschool.runescape.wiki/w/Guard?oldid=14970362)
pins the ordinary Guard contract at level 40, 46.8 XP, a 30-coin Guard pouch, two damage, a
five-second stun and 50/240 success endpoints, with exact 52/252 Gloves, 55/264 applicable-diary
and 60/290 diary-plus-cape boundaries. A complete cache-name-and-operation inventory found 96
Guard-labelled pickpocketable types. Five H.A.M. storeroom guards, 4522–4526, expose Pickpocket in
operation slot one and have a distinct key-loot/ejection contract, so they are deliberately excluded
from the ordinary family. The resulting cache-exact ordinary city/regional set contains 91
operation-slot-three identities:
397–400, 1546–1550, 3010–3011, 3254, 3269–3274, 3283, 5418, 11092–11107, 11911–11917,
11922–11924, 11937–11939, 11942–11947, 13100–13109, 13986–13995, 14663–14670 and
14716–14723. Cache 233.4 pins Guard pouch 22527; production registers those 91 identities and
atomically opens one or all pouches into 30 coins each.

Eight focused scenarios pin both exact cache identity sets, operation slots, levels, XP, rewards,
chance charts, failure damage/stun and both pouch operations. They also prove that the five H.A.M.
storeroom guards cannot enter the ordinary Guard definition. Ardougne-medium locality and exact
matching-client presentation remain open under manual case 101.

## Rogue pickpocket and variable coin-pouch core

The final pre-cutoff [Rogue revision 14941535](https://oldschool.runescape.wiki/w/Rogue?oldid=14941535)
pins ordinary Rogue NPC 526 at level 32, 36.5 XP, two damage, a five-second stun and 75/240 base
success endpoints. Its exact 144-weight table is a 25–40-coin Rogue pouch at 123/144, eight air
runes at 9/144, one jug of wine at 6/144, one lockpick at 5/144 and one iron dagger(p) at 1/144.
The modifier chart gives 78/252 with Gloves of silence, 82/264 with the applicable diary and
90/290 when diary and cape stack. Cache 233.4 independently pins the five output objects at
22525, 556, 1993, 1523 and 1219. NPC 526 exposes `Pickpocket` in operation slot three, while the
otherwise similarly named wilderness Rogue 6603 exposes no Pickpocket operation and is explicitly
excluded.

The dated [coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
lists Rogue pouches at an inclusive 25–40 coins. The contemporary
[Game Integrity: Pickpocketing update](https://oldschool.runescape.wiki/w/Update:Game_Integrity:_Pickpocketing)
states that NPCs with random coin quantities award that random amount when the pouch is opened.
Production therefore represents pouch values as positive inclusive ranges: fixed citizen, Farmer,
Warrior and Guard pouches consume no random roll, while every Rogue pouch rolls independently when
opened. Rogue-outfit doubling preserves one pouch and rolls its direct 25–40 coin counterpart at
reward delivery, without doubling XP.

Five additional focused scenarios pin the cache identity and exclusion, every weighted-table
boundary, level/XP/success chart, failure damage/stun, inclusive pouch endpoints, single/all opening
and variable-pouch Rogue doubling. Exact audiovisual/packet order and matching-client confirmation
remain manual case 101.

## Knight of Ardougne and Knight of Varlamore pickpocket core

The final pre-cutoff
[Knight of Ardougne revision 14969294](https://oldschool.runescape.wiki/w/Knight_of_Ardougne?oldid=14969294)
directly pins level 55, 84.3 XP, one 50-coin pouch, three failure damage, a six-second stun and the
base 50/240 success endpoints. The cutoff Thieving overview independently records the same
six-second duration. The subject page also preserves Mod Ash's exact 55/264 endpoints for the multiplicative medium
Ardougne Diary bonus and states that players become failure-free at level 95. The chart separately
pins 60/290 when the Thieving cape stacks with the diary. The final pre-cutoff
[Knight of Varlamore revision 14922127](https://oldschool.runescape.wiki/w/Knight_of_Varlamore?oldid=14922127)
states that those Knights use the same level, XP, pouch value and damage contract. The dated
[coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
confirms that both regional families yield the same Knight pouch on every success and retain the
one-pouch-plus-direct-coins Rogue-outfit rule.

A cache-wide name-and-operation diagnostic found exactly five production-operable Knights of
Ardougne, IDs 3297, 3300, 8854, 11902 and 11936, and six production-operable Knights of Varlamore,
IDs 13114–13119. Every one exposes `Pickpocket` in operation slot three. The other same-name cache
identities are quest, combat, dialogue, morph or no-operation variants and are not registered.
Cache 233.4 independently pins the shared Knight pouch as stackable, untradeable object 22531 with
`Open-all` and `Open` operations.

Four focused scenarios pin both exact regional identity sets, the cache operation contract, level,
XP, reward, chance endpoints, three damage, the six-second ten-action/eleven-movement-cycle stun,
pouch conversion and the locality distinction: medium
Ardougne Diary completion replaces Gloves of silence for the Ardougne family only, whereas the hard
diary applies to both regions. West Ardougne and Varlamore access remains owned by the Song of the
Elves and Children of the Sun world/morph routes rather than duplicated as an action-time gate.
A reusable exact city-coordinate predicate for other NPC families and all matching-client
presentation remain open under manual case 101.

## Yanille Watchman pickpocket and atomic reward-bundle core

The final pre-cutoff
[Watchman revision 14877307](https://oldschool.runescape.wiki/w/Watchman?oldid=14877307)
pins the one production identity as NPC 5420 and the full pickpocket contract: level 65, 137.5 XP,
three failure damage, a five-second stun and 15/160 base success endpoints. Its modifier chart
gives 15/168 with Gloves of silence, 16/176 with the applicable hard Ardougne Diary bonus and
17/193 when the Thieving cape stacks with the diary. Both its prose and strategy state that every
successful attempt yields one bread and 60 coins; the strategy specifically relies on bread always
being awarded.

The dated [coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
maps Watchman coins to stackable, untradeable pouch 22533 and fixes its value at 60 coins, including
the one-pouch-plus-direct-coins Rogue-outfit rule. Its broad prose calls pouches from a grouped set
including Watchmen “occasional”, while the Watchman-specific page marks both outputs Always. The
implementation follows the direct subject-specific table and preserves one Watchman pouch plus one
bread on every successful attempt; this wording conflict remains recorded for matching-client
observation rather than silently discarding either source.

Cache 233.4 independently exposes `Pickpocket` in operation slot three on exactly the typed
`yanille_watchman` identity 5420 and pins bread 2309 plus pouch 22533 with `Open-all` and `Open`.
Production generalizes pickpocket reward delivery into one atomic bundle: Watchman awards the pouch
and bread together; Rogue equipment keeps one pouch, adds 60 direct coins and doubles bread; any
capacity failure rolls back every output and the XP. Five focused scenarios pin the cache contract,
level, XP, exact endpoints, damage/stun, both pouch operations, bundle doubling and capacity
rollback. Exact shout, audiovisual and packet presentation remain manual case 101.

## Menaphite Thug ordinary pickpocket core and blackjacking boundary

The final pre-cutoff
[Menaphite Thug revision 14978160](https://oldschool.runescape.wiki/w/Menaphite_Thug?oldid=14978160)
pins the ordinary pickpocket contract at level 65, 137.5 XP, one 60-coin pouch, five failure damage,
a five-second stun and 50/160 base success endpoints. Its chart gives 52/168 with Gloves of silence,
55/176 with the hard Ardougne Diary and 60/193 when the Thieving cape stacks with that diary. The
dated [coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
maps the reward to fixed-value pouch 22534 and preserves the one-pouch-plus-60-direct-coins Rogue
rule.

Cache 233.4 supplies the quest-state boundary rather than requiring a duplicate action-time quest
check. Pre-quest NPC 3549 (`feud_egyptian_doorman_1`) exposes only `Talk-to` and `Attack`; post-quest
NPC 3550 (`feud_egyptian_doorman_2`) additionally exposes `Pickpocket` in operation slot three,
`Lure` and `Knock-Out`. Production therefore registers only 3550 in the ordinary dispatcher. One
configuration scenario and four script scenarios pin both identity operation sets, level/XP,
success modifiers, failure damage and five-second eight-action/nine-movement-cycle stun, fixed
pouch opening, shared protections, Rogue conversion and transactional rollback.

Blackjacking deliberately remains separate from the ordinary action. The same dated subject page
records a 78/240 knockout chance, the Lure/Knock-Out route, two pickpockets during the knockout
window, aggression and distinct full-inventory handling, and says Gloves of silence, dodgy
necklaces and Shadow Veil do not affect that system. Exact action/client timing and aggression
lifecycle remain manual case 101; none is approximated inside the ordinary operation-three core.

## Ardougne Paladin guaranteed pickpocket core and clue-tertiary boundary

The final pre-cutoff [Paladin revision 14931379](https://oldschool.runescape.wiki/w/Paladin?oldid=14931379)
pins the post-May-2024 production contract: level 70, 131.8 XP, three failure damage, a five-second
stun and 40/170 base success endpoints. Its chart supplies 42/178 with Gloves of silence, 44/187
with the medium-or-higher Ardougne Diary bonus and 48/205 when a Thieving cape stacks with the
diary. Every successful pickpocket guarantees 80 coins and two chaos runes. This dated revision is
important because it records the 15 May 2024 reduction from 151.8 XP to 131.8 and the later success-
rate improvement; production does not retain the obsolete values hidden in the page comment.

Cache 233.4 exposes operation-slot-three `Pickpocket` on exactly eight applicable typed identities:
3293, 3294, 8853, 11901 and 11930–11933. These cover every East and post-Song-of-the-Elves West
Ardougne Paladin listed by the dated page; the three Underground Pass quest Paladins are separate
non-pickpocket identities. Cache objects independently pin chaos rune 562 and the stackable,
untradeable Paladin pouch 22535 with `Open-all` and `Open`. The dated
[coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
fixes that pouch at 80 coins and supplies the shared one-pouch-plus-direct-coins Rogue rule.

Production registers all eight Paladins through one typed definition, applies the medium diary to
this family because every accessible spawn is inside Ardougne, and grants the pouch plus two runes
through the generalized atomic bundle. Rogue equipment produces one pouch, 80 direct coins and
four chaos runes without doubling XP. Five focused scenarios pin the full family, cache operations,
level/XP/chance contract, failure state, pouch operations, bundle doubling and capacity rollback.

The same dated page independently specifies a 1/1,000 hard-clue scroll roll on a successful
pickpocket, raised from 1/1,500 on 15 May 2024, and its footnote applies the completed Hard Combat
Achievement tier's five-percent reduction for an exact denominator of 950. Cache 233.4 resolves 185
active hard initial-step objects with exact `Read` and `Check steps` operations; their sorted-ID
contract spans 2722–30931 at SHA-256
`ef27ab91ce81430005081b7dd4e954a70b5a4b20efe4edef2b9ecec13a0867ad`.

Production rolls this tertiary only after the ordinary bundle commits. Pre-X-Marks same-tier
ownership suppresses the chance roll; post-X-Marks success awards stackable hard scroll box 24364
within the shared two-to-five capacity, including the exact capped warning. An unstackable loose
clue is blocked when no slot remains, while an existing box stack remains eligible. Rogue equipment
never duplicates the clue. West Ardougne access remains owned by the Song of the Elves world/morph
route; live distribution and exact client presentation remain manual case 101.

## Kharidian Desert Bandit weighted pickpocket core

The final pre-cutoff
[Bandit (Bandit Camp) revision 14990775](https://oldschool.runescape.wiki/w/Bandit_(Bandit_Camp)?oldid=14990775)
pins the two Kharidian Bandit Camp production identities, NPCs 690 and 695, and their complete
pickpocket contract: members-only level 53, 79.4 XP, three failure damage, a five-second stun and
50/240 base success endpoints. Its dated table has exactly seven equally sized outcome shares:
30 coins at 5/7, antipoison(1) at 1/7 and one lockpick at 1/7. The page's Saradomin/Zamorak-
equipment aggression rule is combat AI, not an action-time Thieving gate, and remains separately
open rather than being approximated inside the pickpocket handler.

The dated [coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
maps those 30 coins to stackable, untradeable Desert Bandit pouch 22530 and supplies the shared
one-pouch-plus-direct-coins Rogue-equipment rule. Cache 233.4 independently exposes `Pickpocket`
in operation slot three on typed identities 690 and 695 and pins antipoison(1) 179, lockpick 1523
and pouch 22530 with `Open-all` and `Open`. Same-name IDs 734–737 are The Feud/Pollnivneach
blackjacking families and are deliberately not registered here.

Production models the dated 5/1/1 partition directly. Rogue equipment keeps one pouch and adds 30
coins for the pouch branch, or doubles the selected antipoison or lockpick branch, always awarding
79.4 XP once. Six focused scenarios pin cache identity, weighted boundaries, level/XP, modifiers,
failure state, pouch conversion, both reward-doubling forms and transactional capacity rollback.
Exact shout, aggression, audiovisual and packet presentation remain manual case 101.

## Gnome weighted pickpocket core and medium-clue boundary

The final pre-cutoff [Gnome revision 14996384](https://oldschool.runescape.wiki/w/Gnome?oldid=14996384),
dated 29 September 2025, pins level 75, 133.5 XP, 43/175 base success endpoints, one failure
damage and a five-second stun. Its exact 128-weight ordinary table is 2–4 arrow shafts at 56/128,
300 coins at 30/128, one swamp toad at 24/128, one gold ore at 8/128, one earth rune at 5/128,
one king worm at 3/128 and one fire orb at 2/128. The dated
[coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
maps the coin result to Gnome pouch 22536 and fixes each pouch at 300 coins.

Cache 233.4 sharpens the page's broad identity list. Ordinary Gnomes 6094, 6095 and 6096 expose
`Pickpocket` in operation slot three; battlefield troop/archer identities 5968, 5969 and 5970
expose combat operations but no `Pickpocket`. Production therefore registers only the three
cache-operable identities and tests the other three as exclusions rather than granting a fabricated
action. Cache objects 52, 22536, 2150, 444, 557, 2162 and 569 pin the seven ordinary outputs.

The shared reward contract now supports a positive inclusive item-count range. The 2–4 shaft count
is rolled once inside the protected transaction and that selected count is doubled by Rogue
equipment; it is never rerolled. Gnome pouch doubling keeps one pouch and grants 300 direct coins,
XP remains single, and an output-capacity failure commits neither reward nor XP. Seven focused
scenarios cover exact identities and exclusions, every weight boundary, both shaft endpoints,
level/XP, chance modifiers, failure timing, pouch operations, both Rogue branches and rollback.

The same historical page specifies a separate 1/150 medium-clue roll, reduced to 1/142 by completed
Medium Combat Achievements, and notes that a full inventory blocks it unless X Marks the Spot has
been completed and a scroll box is already present for stacking. Cache 233.4 resolves exactly 165
active medium initial-step objects with `Read` and `Check steps`; their sorted IDs span 2801–30935
at SHA-256 `c82c7cdaba9f9c2e6a69bb5e2cd8a0fef968a0c0d35d20193a7ae0e26c7a7677`.
The integrated tertiary shares clue ownership and two-to-five box capacity, produces medium box
24363 after X Marks, preserves the ordinary loot and XP when a loose clue cannot fit, emits the
shared capped warning on a successful roll and never passes the clue through Rogue doubling.
Manual case 101 retains live distribution and exact client presentation.

## Hero weighted pickpocket core and elite-clue boundary

The final pre-cutoff [Hero revision 14940550](https://oldschool.runescape.wiki/w/Hero?oldid=14940550)
pins cache-operable NPCs 3295, 11934 and 11935 and their complete ordinary reward contract:
members-only level 80, 163.3 XP, 39/160 base success endpoints, three failure damage and a six-
second stun. Its 128-weight table is Hero pouch 22537 at 105/128, two death runes 560 at 8/128,
jug of wine 1993 at 6/128, blood rune 565 at 5/128, diamond 1601 at 1/128, fire orb 569 at 2/128
and gold ore 444 at 1/128. The dated
[coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
confirms that each Hero pouch independently opens for 200–300 coins.

The Hero page also supplies the family-specific Rogue-equipment exception: when doubling triggers
on the pouch branch it keeps one pouch and adds exactly 200 direct coins, not another 200–300 roll.
Non-pouch rewards double normally and XP remains single. Because all three production identities
are East Ardougne Heroes, the medium Ardougne Diary modifier safely applies to the whole family.
Production models the six-second source as ten action-block cycles and eleven movement-block
cycles under the same established server-boundary convention used by the shorter stuns. Seven
focused scenarios pin identities, weighted boundaries, exact modifier endpoints, failure timing,
variable pouch opening, fixed direct Rogue coins, non-pouch doubling and capacity rollback.

The same Hero revision specifies an independent 1/1,400 elite-clue roll after a successful
pickpocket, reduced to 1/1,330 by completed Elite Combat Achievements. Cache 233.4 resolves exactly
135 active elite initial-step objects with `Read` and `Check steps`; their sorted IDs span
12073–30932 at SHA-256 `d74c0e9f41f75bc230b160b28e19be384b7412ec0ee630b45c6ddf0186d7738c`.
The integrated tertiary uses shared ownership, X Marks elite box 24365, capacity/warning and
full-inventory policy, and remains independent of the Hero-specific Rogue coin exception: neither
that exception nor normal Rogue doubling duplicates a clue. Exact client presentation and live
statistical observation remain manual case 101.

## H.A.M. member source and cache boundary

The final pre-cutoff
[H.A.M. Member revision 14993132](https://oldschool.runescape.wiki/w/H.A.M._Member?oldid=14993132)
pins both members-only identities, level 15, 22.2 XP, 1–3 failure damage and a four-second stun.
Its success chart supplies 135/239 base endpoints, 141/250 with Gloves of silence, 148/262 with
the hard Ardougne Diary and 162/288 when a Thieving cape stacks with that diary. Cache 233.4 maps
the male and female members to typed NPCs 2540 and 2541. Unlike every currently dispatched standard
family, both expose `Pickpocket` only in operation slot one; the cache-boundary test keeps them out
of the generic operation-three registration.

The dated page pins a 1/50 easy-clue pre-roll. If the account already owns its maximum scroll cases,
that branch gives nothing, and Rogue equipment never doubles the clue. Conditional on continuing
to the ordinary table, the underlying weights sum to 100 even though the page displays rounded
post-pre-roll rarities such as 1/102. Seven H.A.M. clothing pieces each weigh 1. Bronze arrows,
bronze axe, bronze dagger, bronze pickaxe, iron axe, iron dagger, iron pickaxe and leather body each
weigh 3; steel arrows, steel axe, steel dagger and steel pickaxe each weigh 2. The remaining shares
are buttons 4, H.A.M. pouch 17, feathers 3, knife 2, logs 3, needle 2, raw anchovies 2, raw chicken
2, thread 3, tinderbox 2, uncut opal 2, coal 2, cowhide 3, damaged armour 4, herbs 2, iron ore 2,
rusty sword 4 and uncut jade 2. Bronze and steel arrows roll 1–13, feathers 1–7 and thread 1–10.
The nested herb branch selects grimy guam/marrentill/tarromin at 6/11, 3/11 and 2/11.

The dated [coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
maps the coin outcome to stackable, untradeable pouch 22523 with `Open-all` and `Open`, and fixes
each pouch at an inclusive 1–21 coins. Cache 233.4 now has typed, tested references for all 39
possible ordinary outputs: clothing 4298/4300/4302/4304/4306/4308/4310; weapons and armour 882,
886, 1129, 1203, 1205, 1207, 1265, 1267, 1269, 1349, 1351 and 1353; and the remaining objects 199,
201, 203, 314, 321, 440, 453, 590, 686, 688, 697, 946, 1511, 1609, 1611, 1733, 1734, 1739,
2138, and 22523.

During the relevant Death to the Dorgeshuun stage, a non-clue success has a 1/5 special-table chance
to award one of the seven clothing pieces uniformly; the other 4/5 continues through the ordinary
table. The same page pins the distinctive failure lifecycle: partial Agility avoidance does not
increment the concussion counter, three slight concussions cause jail or outside ejection, and
leaving/re-entering resets the counter. It describes each worn H.A.M. piece as reducing ejection
chance by only “about 4%”, however, and does not pin the exact jail/outside or multi-destination
selection. Those approximations prevent an exact production action. The family remains researched,
with operation routing, concussion state, quest-stage override, clue ownership and ejection
destinations reserved for matching-client case 101 instead of being guessed.

## H.A.M. storeroom guard source and cache boundary

The final pre-cutoff
[Guard (H.A.M. Storerooms) revision 14993129](https://oldschool.runescape.wiki/w/Guard_(H.A.M._Storerooms)?oldid=14993129)
pins the five post-quest subjects as NPCs 4522–4526. They are members-only level-22 combat NPCs
at the five documented map-10074 positions, require level 20 Thieving and award 22.2 XP per
successful pickpocket. Cache 233.4 maps those IDs to `dttd_ham_guard_1_postquest` through
`dttd_ham_guard_5_postquest`; every one exposes `Pickpocket` in operation slot one and none enters
the production operation-three registry. The dated subject page is explicitly categorised
`Needs skilling success chart`, so no exact success endpoints can be derived from it.

That page publishes one exact 500-share reward table. Bronze, iron, silver and steel keys each
occupy 50 shares. Bronze arrows, bronze axe/dagger/pickaxe, iron axe/dagger/pickaxe and leather body
each occupy 9; every one of the seven H.A.M. clothing pieces occupies 3; and steel arrows,
axe, dagger and pickaxe each occupy 6. The remaining shares are buttons 12, H.A.M. pouch 45,
feathers 9, knife 6, logs 9, needle 6, raw anchovies 6, raw chicken 6, thread 9, tinderbox 6,
uncut opal 6, easy clue 6, coal 6, cowhide 9, damaged armour 12, nested herbs 6, iron ore 6,
rusty sword 12 and uncut jade 6. The herb branch selects guam/marrentill/tarromin at 6/11, 3/11
and 2/11. Both arrow entries roll 1–13, feathers 1–7 and thread 1–10. The dated
[coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
maps the pouch branch to object 22523 and its inclusive 1–21 coins. This is the same complete
39-object ordinary output family already typed for H.A.M. members, plus the four keys.

The final pre-cutoff
[H.A.M. Storerooms revision 14853414](https://oldschool.runescape.wiki/w/H.A.M._Storerooms?oldid=14853414)
separates this failure lifecycle from ordinary H.A.M. members: a caught player is never thrown out
and instead takes up to three damage. A failed attempt makes that guard stop patrolling, return to
its start position and cease attacking door pickers; otherwise patrolling guards attack witnessed
door attempts. It also says post-quest guards are non-aggressive unless spoken to without H.A.M.
robes. Exact damage distribution, stun duration, patrol path/return timing and line-of-sight reset
are not published.

The final pre-cutoff
[Death to the Dorgeshuun revision 14996424](https://oldschool.runescape.wiki/w/Death_to_the_Dorgeshuun?oldid=14996424)
distinguishes the quest infiltration from repeatable post-quest thieving: Zanik incapacitates five
patrol guards and a later senior guard arrests the player after the meeting, while quest completion
awards permanent storeroom access. Cache 233.4 independently resolves those five patrol forms as
4516–4520 and the extra arrest form as 4521; none has a Pickpocket operation. This six-to-five cache
morph boundary is typed and tested rather than inferred from an ID range.

The dated key pages pin the exact object mapping:
[bronze revision 14796474](https://oldschool.runescape.wiki/w/Bronze_key_(H.A.M.)?oldid=14796474)
is 8867 for the south-west chest,
[iron revision 14716380](https://oldschool.runescape.wiki/w/Iron_key_(H.A.M.)?oldid=14716380)
is 8869 for north-east,
[silver revision 14424090](https://oldschool.runescape.wiki/w/Silver_key_(H.A.M.)?oldid=14424090)
is 8868 for north-west and
[steel revision 14424334](https://oldschool.runescape.wiki/w/Steel_key_(H.A.M.)?oldid=14424334)
is 8866 for south-east and both northern chests. Every key is consumed and prints its metal-specific
break message when its chest is opened. All four pages specify non-stackable, nontradeable,
non-noteable objects with placeholders. Cache 233.4 agrees on identity, name, non-stackability,
placeholder and stock-market exclusion but leaves the generic `tradeable` field at its true
default. That discrepancy is now regression-tested and requires an explicit server-side trade
restriction when trading is integrated.

Production remains intentionally undispatched. The exact success chart, stun duration, damage
distribution, clue ownership/full-inventory behavior, Rogue transaction semantics, quest morph
tick and patrol/door AI must be observed in the approved cache-233 client before an operation-one
handler can truthfully implement the complete action. The focused configuration suite protects all
eleven NPC forms, all four keys, the operation split, cache metadata mismatch and production
exclusions in the meantime.

## H.A.M. storeroom Small chest source and cache boundary

The final pre-cutoff
[Small chest revision 14770595](https://oldschool.runescape.wiki/w/Small_chest?oldid=14770595)
pins six members-only H.A.M. Storeroom chests and their compass/key mapping. Cache 233.4 resolves
the closed family to `dttd_ham_chest_1` 15722 for steel, `_2` 15723 for bronze, `_3` 15724 for
silver and `_4` 15726 for iron. Each is named `Small chest`, describes itself as `I don't think it
contains ham.` and exposes `Open` in operation slot one. `dttd_ham_chest_open` 15725 has the same
name but no operation. The static map registry independently proves bronze 15723 at (2570,5192),
steel 15722 at (2573,5193), silver 15724 at (2569,5199), iron 15726 at (2573,5198), and two more
steel 15722 chests at (2577,5208) and (2577,5209). Thus bronze key 8867 owns south-west, iron key
8869 north-east, silver key 8868 north-west and steel key 8866 south-east plus both northern
chests; no direction or symbol-name inference remains.

The same dated page publishes one exact 150-share table. Coins 995 roll 0–99 at 75/150. The other
nineteen entries are: sapphire necklace at 6; diamond plus sapphire amulet, necklace and ring at 2;
ruby necklace at 3; gold ring at 5; sapphire amulet, necklace and ring at 6; emerald ring plus
sapphire amulet and necklace at 4; sapphire amulet and necklace at 6; diamond necklace at 2; gold
amulet at 5; diamond ring plus sapphire amulet and necklace at 2; sapphire plus sapphire amulet,
necklace and ring at 5; emerald necklace at 4; emerald amulet plus sapphire necklace at 4; gold
necklace at 6; diamond amulet plus sapphire necklace at 2; ruby plus sapphire amulet, necklace and
ring at 3; ruby ring plus sapphire amulet and necklace at 3; ruby amulet plus sapphire necklace at
3; and emerald plus sapphire amulet, necklace and ring at 4. Those weights sum to 150. Typed cache
references and the focused test pin coins 995; gems 1601/1603/1605/1607; rings
1635/1637/1639/1641/1643; necklaces 1654/1656/1658/1660/1662; and strung amulets
1692/1694/1696/1698/1700, including their exact cache names.

The source says the appropriate key is used and breaks when the chest opens, but it does not give
the missing- or wrong-key response, inventory-capacity policy for the multi-object bundles,
closed-to-open transform tick, open duration, replacement timing, two-player contention behavior
or the observable treatment of the possible zero-coin result. Those are action-defining lifecycle
and transaction rules. Production is therefore withheld instead of borrowing an unrelated chest
implementation or guessing atomicity. Manual case 101 records each remaining matching-client
measurement; the source/cache boundary test protects every safe identity, placement and reward in
the meantime.

## H.A.M. storeroom locked-door source and cache boundary

The final pre-cutoff
[H.A.M. Storerooms revision 14853414](https://oldschool.runescape.wiki/w/H.A.M._Storerooms?oldid=14853414)
states that the four central chest rooms can be entered by picking their door locks for exactly 4
Thieving XP, with a lockpick improving the chance. It separately pins two wall-crack bypasses on
the north and south sides of the south-west room and says the large northern room's door is not
locked. While a guard is patrolling, a witnessed door attempt causes an attack; after a failed
guard pickpocket makes it stop and return to its start position, that guard no longer attacks door
pickers. These are coupled door/world-AI rules rather than generic open-door behavior.

Cache 233.4 resolves the locked scenery as typed symbol `dttd_ham_door`, loc 15759. It is named
`Door`, describes itself as `The door is closed.`, exposes `Open` in operation slot one and
`Pick-lock` in operation slot four, not slot two. The focused map-registry test proves exactly four
instances at (2567,5198), (2576,5198), (2567,5192) and (2576,5192), matching the four central
rooms. It also pins the already shared lockpick symbol to object 1523 and its cache name. No later
dedicated Wiki page is used as a cache-233.4 gameplay authority because that page was created after
the project's 1 October 2025 cutoff.

The cutoff source does not publish a minimum Thieving level, the exact success endpoints with or
without a lockpick, boosted-level rounding, failure and automatic-retry cadence, movement/door
transition behavior, exact presentation or the guard observer and attack timing. Those omissions
prevent a faithful production handler even though identity and placement are now exact. Manual
case 101 measures each branch, including crack/unlocked-door comparisons and patrol-state coupling;
until then this remains an explicit researched boundary.

## Varlamore Wealthy citizen source and cache boundary

The final pre-cutoff
[Wealthy citizen revision 14988663](https://oldschool.runescape.wiki/w/Wealthy_citizen?oldid=14988663)
pins four members-only identities, level 50, 96 XP, three failure damage and a four-second stun.
Its chart gives 35/200 base endpoints, 36/210 with Gloves of silence, 38/220 with the hard Ardougne
Diary and 41/242 when the Thieving cape stacks with that diary. Cache 233.4 resolves those subjects
to typed NPCs 13302–13305. Every one exposes `Pickpocket` in operation slot one and `Talk-to` in
operation slot three; the focused cache test deliberately excludes them from the generic
operation-three dispatcher.

The direct subject page gives one exact 85-share outcome table: one 85-coin pouch at 79/85, House
keys at 5/85 and an easy clue at 1/85. It also says a rolled key or clue is discarded when inventory
capacity cannot fit it while the pickpocket still completes, and full Rogue equipment doubles a key
reward. The easy-clue footnote makes two further cache-233 rules explicit: completed Easy Combat
Achievements improve the clue share from 1/85 to 1/80, and completed X Marks the Spot turns the
reward into an easy scroll box. Cache 233.4 independently pins the stackable `Open-all`/`Open`
pouch as object 28822 and stackable House keys as object 29325. The dated
[coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
fixes the pouch value at 85, while Jagex's contemporary
[Varlamore: Part One overview](https://secure.runescape.com/m=news/varlamore-part-one---overview?oldschool=1)
describes pickpocket rewards as coin pouches with a separate chance for a key. Together those direct
sources resolve the earlier wording conflict: the 79 coin shares each yield one pouch worth 85
coins; they do not replace the key or clue shares.

One newly surfaced cache contract still prevents faithful production dispatch. Varbit 9942,
`varlamore_thieving_house_key_drystreak`, is an eight-bit slice (bits 2–9) of permanent varp 4175.
Its name and dedicated width establish persistent house-key dry-streak state, but neither the final
pre-cutoff subject page, its House keys page nor the contemporary Jagex overview publishes the
increment, threshold, guarantee/chance adjustment or reset rule. A memoryless 5/85 implementation
would knowingly ignore this production state. Manual case 101 therefore has to recover that state
machine before the ordinary reward resolver is registered; clue ownership and discard-on-full
branches can be verified in the same capture.

Street-urchin distraction is a separately coupled lifecycle. The dated subject page states that
every 84–99 seconds three urchins each distract one citizen for roughly 20 seconds, enabling
automatic repeated pickpockets with 100% success until the loop stops. A recent world hop delays
the loop by roughly 15 seconds and initially yields no items, but “recent” and the precise start,
stop and no-item tick boundaries are not exact. Manual case 101 must recover those timings and
world-state transitions before distracted Wealthy citizen production is promoted. These timings
are a separate blocker from the ordinary action's dry-streak state and must remain independently
tracked.

## Pollnivneach bandit and blackjacking source boundary

The final pre-cutoff
[Bandit (Pollnivneach) revision 14990770](https://oldschool.runescape.wiki/w/Bandit_(Pollnivneach)?oldid=14990770)
pins two bearded identities, 736–737, and two non-bearded identities, 734–735. Cache 233.4 sharpens
their quest morph boundary: 734 and 736 expose only `Talk-to`, `Attack` and `Pickpocket`, while 735
and 737 additionally expose `Lure` and `Knock-Out`. The exact four operation arrays are now guarded
by a typed cache test rather than treating all variants as interchangeable.

The bearded family requires level 45, awards 65 XP and always yields 40 coins; the non-bearded
family requires level 55, awards 84.3 XP and always yields 50 coins. An awake pickpocket packages
the reward in a coin pouch, while a knocked-out pickpocket gives coins directly. The dated
[coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
maps the bearded reward to stackable, untradeable `Open-all`/`Open` pouch 22529 and the non-bearded
reward to pouch 22532. Both objects are typed and tested but intentionally excluded from the active
pouch registry until their ordinary actions have an exact success formula. The cutoff
[Thieving revision 14991198](https://oldschool.runescape.wiki/w/Thieving?oldid=14991198)
explicitly labels both ordinary pickpocket success rates `Unknown`, so no endpoints are inferred.

For blackjacking, the subject page gives a shared 80/240 Knock-Out chart from level 45 and awards
10 Thieving XP on success. It also gives exact rejection text when a second bandit has line of sight
or the player is in combat. The final pre-cutoff
[Blackjack revision 14898817](https://oldschool.runescape.wiki/w/Blackjack?oldid=14898817)
establishes that all nine blackjack types are functionally identical for lure/knockout, requires
the learned section of The Feud plus the target's Thieving level, and records two guaranteed rapid
pickpockets after a successful knockout. A failed knockout may cause aggression, and that attack
can be interrupted before its second hit by any pickpocket result or a successful Knock-Out. The
final pre-cutoff [The Feud revision 14986366](https://oldschool.runescape.wiki/w/The_Feud?oldid=14986366)
confirms the quest unlock but does not turn those descriptive boundaries into exact cache-233 tick
state. Lure dialogue timing, observer selection, knockout duration, aggression probability and
transition, two-pickpocket eligibility ticks, interruption ordering and full-inventory behavior
remain matching-client case 101. Ordinary and blackjack production are withheld until those
unknowns are measured rather than approximated.

## Fremennik citizen source boundary

The final pre-cutoff
[Fremennik citizen revision 14992177](https://oldschool.runescape.wiki/w/Fremennik_citizen?oldid=14992177)
names Agnar, Freidir, Borrokar, Lanzig, Pontak, Lensa, Jennella, Sassilik and Inga and pins the known
pickpocket contract: completion of The Fremennik Trials, level 45, 65 XP, a guaranteed 40-coin
reward, two failure damage and a five-second stun. Cache 233.4 maps those names to NPCs 3937–3941
and 3943–3946, the Fremennik pouch to object 22528, and The Fremennik Trials to quest database row
57. Related cache identity 3942 is not one of the dated page's nine named targets and therefore is
not silently absorbed.

Both the dated citizen page and the cutoff Thieving overview explicitly report the success rate as
unknown. No endpoint or substitute formula is inferred, and the exact completed-quest row state is
not yet pinned. This family therefore remains researched rather than implemented. Manual case 101
requires controlled matching-client samples across levels, both sides of quest completion and NPC
3942 before production can claim an exact chance or dispatch boundary.

## Cave goblin source boundary

The final pre-cutoff
[Cave goblin revision 14926805](https://oldschool.runescape.wiki/w/Cave_goblin_(Dorgesh-Kaan)?oldid=14926805)
pins level 36, 40 XP, one failure damage, a five-second stun and the special failed-attempt response
that may alert a nearby Dorgeshuun guard to attack. Dorgesh-Kaan itself is reached after Death to
the Dorgeshuun. The subject infobox identifies fourteen forms: 2268–2284 as listed by the page,
with 2301 explicitly Talk-to-only while the other thirteen expose Pickpocket.

Its complete table is twenty equal shares. Bat shish, coated frogs' legs, fingers, frogburger,
frogspawn gumbo and green gloop soup each occupy 1/20. Coins occupy 7/20 and independently roll
10–50; bullseye lantern, Cave goblin wire, 1–4 iron ore, oil lantern, swamp tar, tinderbox and
unlit torch each occupy 1/20. One wire is awarded normally and two after the elite Lumbridge Diary.
The dated [coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
maps the coin outcome to Cave goblin pouch 22526 and supplies the shared one-pouch-plus-direct-coins
Rogue rule.

The cutoff [Thieving revision 14991198](https://oldschool.runescape.wiki/w/Thieving?oldid=14991198)
explicitly records the Cave goblin fail level/success rate as unknown. Neither an endpoint curve nor
the guard alert radius and targeting contract is inferred. The family therefore remains researched
until matching-client evidence resolves those values; manual case 101 records the exact outstanding
observations.

## Lletya Elf weighted pickpocket core and Prifddinas boundary

The final pre-cutoff
[Elf revision 14814353](https://oldschool.runescape.wiki/w/Elf_(Thieving)?oldid=14814353)
pins the shared level 85, 353.3 XP, five failure damage and six-second stun contract. Its success
chart supplies 6/100 base endpoints, 6/105 with Gloves of silence, 6/110 with the hard Ardougne
Diary and 6/121 when the Thieving cape stacks with the diary. The main 128-weight table is an Elf
pouch at 105/128, two death runes at 8/128, three nature runes at 5/128, a jug of wine at 6/128,
one diamond at 1/128, one fire orb at 2/128 and one gold ore at 1/128. The dated coin-pouch source
fixes Elf pouch 22538 at an inclusive 280–350 coins and states that Rogue doubling retains one pouch
while paying the other half directly as coins.

The dated [Goreu revision 14813345](https://oldschool.runescape.wiki/w/Goreu?oldid=14813345),
[Arvel revision 14813346](https://oldschool.runescape.wiki/w/Arvel?oldid=14813346),
[Kelyn revision 14813344](https://oldschool.runescape.wiki/w/Kelyn?oldid=14813344) and
[Mawrth revision 14813343](https://oldschool.runescape.wiki/w/Mawrth?oldid=14813343) resolve the
four named Lletya citizens. Cache 233.4 then sharpens their usable forms: NPCs 5297, 5299, 9015 and
5300 expose `Pickpocket` in operation slot three. Kelyn form 8914 and Mawrth quest/cutscene forms
8927–8928 expose no Pickpocket operation and remain typed, tested exclusions rather than receiving
a manufactured action. The access prerequisite is owned by the Mourning's End Part I world route
and these phased NPC identities, so the generic action does not invent a duplicate quest-state gate.

Production registers those four cache-operable Lletya forms through one definition, activates the
variable Elf pouch in the shared atomic `Open`/`Open-all` registry and uses the established six-
second ten-action/eleven-movement-cycle boundary. Rogue equipment doubles the chosen non-pouch
count or keeps one pouch plus one independent 280–350 direct payout, with XP awarded once. Six
focused scenarios pin cache identities and exclusions, every weight boundary, level/XP, modifier
endpoints, failure timing, pouch operations, both Rogue branches and atomic rollback. All 178
registered Thieving NPCs are constrained to operation slot three.

The same Elf revision names exactly 49 Prifddinas targets. Cache 233.4 maps them to the contiguous
operation-three interval 9054–9090 plus 9106–9117. Six similarly named symbols—Amrod 9053 and
Nimrodel, Maedhros, Finarfin, Gwindor and Eldalote 9101–9105—are absent from the dated list and have
no Pickpocket operation. Typed inclusion/exclusion sets and a cache test now protect that exact
boundary. Song of the Elves owns access to the city and therefore the reachable spawn set.

The regional family receives two rolls before the shared 128-weight main table: one crystal shard
at 1/35 and one enhanced crystal teleport seed at 1/1,024. Cache 233.4 resolves them to objects
23962 and 23959. Final pre-cutoff
[Crystal shard revision 14988101](https://oldschool.runescape.wiki/w/Crystal_shard?oldid=14988101)
confirms that Rogue equipment can turn the shard into two and says that when a shard accompanies a
collected resource with only one free inventory slot, the shard drops to the ground. Final
pre-cutoff
[Enhanced crystal teleport seed revision 14962702](https://oldschool.runescape.wiki/w/Enhanced_crystal_teleport_seed?oldid=14962702)
separately confirms that partial Rogue equipment gives a chance of two enhanced seeds and the full
set guarantees two.

Those sources do not yet pin whether the shard, seed and main result share one Rogue doubling roll
or make independent rolls, the exact ordering if both tertiaries hit together, the seed's full-
inventory destination, or whether a main-output capacity failure still commits either tertiary.
The family therefore remains researched rather than silently reusing the Lletya definition and
omitting its pre-rolls. Manual case 101 carries the narrowed transaction and packet observations.

## Vyre weighted pickpocket core and blood-shard boundary

The final pre-cutoff [Vyre revision 14907001](https://oldschool.runescape.wiki/w/Vyre?oldid=14907001)
pins thirty named Darkmeyer citizens, level 82, 306.9 XP, five failure damage and a six-second stun.
Its chart supplies 8/128 base endpoints, 8/134 with Gloves of silence, 8/140 with the hard Ardougne
Diary and 8/154 when the Thieving cape stacks with that diary. The ordinary 132-weight table is
230–315 coins at 109/132, two death runes at 8/132, four blood runes at 2/132, one blood pint at
6/132, one uncut ruby at 5/132, one diamond at 1/132 and one cooked mystery meat at 1/132.

The cutoff [Thieving revision 14991198](https://oldschool.runescape.wiki/w/Thieving?oldid=14991198)
instead lists a three-second Vyre stun in its broad overview table. Because that overview describes
itself as incomplete for precise stun behavior while the dated subject page gives an explicit
six-second value, production follows the subject-specific record. This is an evidence-selection
decision, not a claim that the conflict is resolved: manual case 101 must observe the cache-233
client boundary directly.

Cache 233.4 maps those names to the contiguous NPC interval 9685–9714 and exposes `Pickpocket` in
operation slot three on every identity. It pins Vyre pouch 24703, blood pint 24774, uncut ruby 1619,
cooked mystery meat 24785, death rune 560, blood rune 565 and diamond 1601. The dated
[coin-pouch revision 14993878](https://oldschool.runescape.wiki/w/Coin_pouch?oldid=14993878)
confirms the 230–315 pouch range and shared one-pouch-plus-direct-coins rule. The Vyre page then
supplies its family-specific exception: a doubled pouch always adds exactly 230 direct coins,
regardless of the pouch's eventual variable opening payout.

Production registers all thirty typed identities through one definition and models the 132-weight
main table directly. The six-second source uses ten action-block and eleven movement-block cycles.
Rogue equipment keeps one pouch plus 230 direct coins or doubles the selected non-pouch count,
always awarding XP once; transactional capacity failure commits neither reward nor XP. One exact
configuration scenario and five script scenarios cover the complete identity interval, cache
operations, every weight boundary, level/XP, modifiers, failure timing, pouch opening, both Rogue
branches and rollback. The focused Thieving suite now has 79 passing scenarios and all 208
registered NPCs are constrained to operation slot three.

The same dated page lists a blood shard at 1/5,000 independently of the 132-weight ordinary table.
The final pre-cutoff [Blood shard revision 14965265](https://oldschool.runescape.wiki/w/Blood_shard?oldid=14965265)
states that worn Rogue pieces can double the shard, the complete set guarantees two, and both
shards drop to the floor when the inventory is full. Production rolls the tertiary only after a
successful, committed ordinary reward, reuses that pickpocket's Rogue double-loot result, and adds
the complete one- or two-shard quantity atomically. If the quantity cannot fit, rsmod creates
private owner-only floor objects for the entire shard reward; the ordinary result and single XP
award remain committed. The Wiki specifies an independent probability but no player-observable RNG
call order, so the internal post-main call position is deterministic implementation structure rather
than a claim about Jagex's generator sequence. Three integrated scenarios cover hit/miss,
Rogue-guaranteed doubling, unchanged ordinary loot/XP and the one-free-slot owner-drop boundary;
the focused Thieving suite now has 79 passing scenarios. Darkmeyer access and nearby Vyrewatch
Sentinel hostility when not wearing a full vyre-noble set belong to world progression and combat AI;
the pickpocket action does not invent duplicate access or clothing gates.

## Poll 85 Fremennik sea boots teleport core

The official 17 September 2025
[Poll 85 update](https://oldschool.runescape.wiki/w/Update:Poll_85_-_Bridges,_Boots,_Ropes_%26_Roots?oldid=14989447)
raises the Fremennik sea boots daily Rellekka teleports by achievement tier from one to three to
five to unlimited. Final pre-cutoff revisions for
[sea boots 1](https://oldschool.runescape.wiki/w/Fremennik_sea_boots_1?oldid=14987797),
[sea boots 2](https://oldschool.runescape.wiki/w/Fremennik_sea_boots_2?oldid=14987619),
[sea boots 3](https://oldschool.runescape.wiki/w/Fremennik_sea_boots_3?oldid=14987620) and
[sea boots 4](https://oldschool.runescape.wiki/w/Fremennik_sea_boots_4?oldid=14881692) pin the shared
destination to Rellekka market at 2643,3675. Cache 233.4 resolves those objects to 13129–13132,
with inventory operations `Wear`, `Teleport`, `Drop` and equipped operation `Teleport`.

Production registers the cache's third held operation and second worn operation for all four
objects. A persistent server-only UTC-day stamp and shared-use counter enforce the 1/3/5/unlimited
contract across tier changes and relogs. Accepted uses apply the standard teleport sequence,
spotanim and sound for four ticks before moving; non-members, Tele Block and Wilderness levels
above 20 reject atomically. Five focused integration scenarios pin cache operations, event hooks,
destination/effects, every quota, cross-tier sharing, UTC rollover and all three gates. Exact
client-facing wording, audiovisual/packet order and rollover presentation remain manual case 102.

The same Poll 85 update also describes a bridge shortcut by Fenkenstrain's Mausoleum, desktop shop
quantity buttons matching Mobile with a configurable left-click amount, Baxtorian Falls rope
placements, a disease-free Harmony herb patch and Hunter-kit changes. They
remain separate researched work and are not represented as complete by the boots slice.

## Poll 85 Fenkenstrain Mausoleum bridge core

The final pre-cutoff
[Mausoleum bridge revision 14996960](https://oldschool.runescape.wiki/w/Bridge_(Mausoleum)?oldid=14996960)
pins the bridge polygon at 3503–3505,3556–3560, Construction 59, a hammer, six teak planks and 28
nails for each side, 550 Construction XP for each side, the first-side collapse, the exact three
repair messages, a 69 Agility crossing that awards no XP and the requirement to click two distinct
unrepaired tiles consecutively for the mausoleum camera sequence. The 17 September
[Poll 85 update](https://oldschool.runescape.wiki/w/Update:Poll_85_-_Bridges,_Boots,_Ropes_%26_Roots?oldid=14989447)
confirms that this is the 233.2 shortcut being reconstructed.

Direct cache-233.4 inspection pins surface 5159 and mainland unrepaired 5160, then the new mirror
unrepaired/build/build-mirror/repaired/repaired-mirror family 57710–57714. All seven are one-tile
ground decorations with the expected `Cross` or `Build` operation. Map group `l54_55` contains the
old 5159 surface tiles on linked-below raw plane one, which resolve to world plane zero. Cache
varbit 199 is `fenk_unlocked_cavern`; sequence 807 is `human_longjump`. Model mirroring and the
historical polygon support the chosen mainland 3504,3556 and island 3504,3560 overlays, but their
angle, arrival squares and exact presentation remain explicit matching-client inferences.

Production uses a permanent, non-transmitted server varp and a reusable per-player location map so
each account can see and click its own repair stage. Private locations are accepted by ordinary
location packets, remain valid through interaction processing, update immediately while visible
and are resent after build-area changes. Nine bridge scenarios plus the lower-level zone regression
cover identities, both stages, resources, mixed nails, XP, level and tool atomicity, packet routing,
crossing and the cinematic trigger. The current Wiki additionally documents plank-sack use, but no
plank-sack storage contract exists in this repository; that later clarification is recorded only as
an unresolved case-103 boundary and is not represented as completed cutoff behavior.

## Poll 85 Baxtorian Falls cache-233.4 rope core

The official 17 September 2025
[Poll 85 update](https://oldschool.runescape.wiki/w/Update:Poll_85_-_Bridges,_Boots,_Ropes_%26_Roots?oldid=14989447)
requires both Waterfall Quest and the medium Kandarin Diary and announces permanent ropes at
Baxtorian Falls. The later-created
[Rope page revision 15333276](https://oldschool.runescape.wiki/w/Rope_(Baxtorian_Falls)?oldid=15333276)
preserves the release-day hotfix history: the initial route skipped the medium-clue plateau, so the
first rope was changed to land at the second rope and that second rope was made non-interactive.
The page records restoration of the second rope on 17 December 2025. Because the locked 233.4
archive was captured on 1 October, that restoration is explicitly future-cache behavior rather
than part of this target.

Direct cache inspection pins wrapper locations 57703 and 57706 to varbit 4516
`kandarin_medium_reward`. Their no-op/`Take` variants are 57704/57705 and 57707/57708, all using
rope model 1842. The first wrapper resolves to 57705 when the reward varbit is one. The cache retains
the second `Take` variant even though the cutoff server hotfix did not place its wrapper, so the
implementation intentionally installs only 57703. RuneLite's matching gamevals name the families
`DIARY_WATERFALL_ROPE_ROCK` and `DIARY_WATERFALL_ROPE_TREE`.

The rope page's map source places the first anchor at 2511,3477 and the second at 2513,3465. The
last pre-cutoff Quest Helper
[Waterfall commit f10b5d95](https://github.com/Zoinkwiz/quest-helper/commit/f10b5d95e461a085a2b3f0f15ec2523154f88d03)
pins the medium clue to 2512,3467, the dead-tree island to 2512–2513,3465–3475 and every active
Waterfall quest state to 0–8; varp 65's journal-complete state is 10. Production therefore requires
members, varp 65 at least 10 and reward varbit 4516, then moves directly to 2512,3467 without adding
or consuming a rope item. Five integration scenarios pin the six cache types, both progression
gates, cutoff-only placement, inventory invariance, arrival square and normal location-packet morph
routing. Exact anchor angle, movement presentation and the later second-rope route remain manual
case 104.

## Poll 85 hunting-expert Hunter Kit grant core

The official 17 September 2025
[Poll 85 update](https://oldschool.runescape.wiki/w/Update:Poll_85_-_Bridges,_Boots,_Ropes_%26_Roots?oldid=14989447)
and the final pre-cutoff
[Hunter kit revision 14987664](https://oldschool.runescape.wiki/w/Hunter_kit?oldid=14987664)
establish a one-time free Hunter kit from either the Feldip Hills or Rellekka hunting expert. The
two dialogue pages were edited on release day, at
[Feldip revision 14987663](https://oldschool.runescape.wiki/w/Transcript:Hunting_expert?oldid=14987663)
and
[Rellekka revision 14987661](https://oldschool.runescape.wiki/w/Transcript:Hunting_expert_(Rellekka_Hunter_area)?oldid=14987661).
Both cutoff revisions pin the new `Can you give me some hunting traps?` option, the shared
already-received condition, the exact refusal directing players to Yanille and the return to the
expert's other choices. They marked only the successful branch as missing rather than supplying
invented text.

Later transcription-only revisions filled that evidence gap:
[Rellekka revision 15076546](https://oldschool.runescape.wiki/w/Transcript:Hunting_expert_(Rellekka_Hunter_area)?oldid=15076546)
on 3 December 2025 and
[Feldip revision 15119975](https://oldschool.runescape.wiki/w/Transcript:Hunting_expert?oldid=15119975)
on 3 February 2026 record the same offer, item box and thanks. These later pages are used only as
verbatim transcription evidence for behavior introduced by the dated Poll 85 update, not as proof
of a post-cutoff mechanic. They also preserve the NPC-specific continuation: Feldip returns to its
menu after a successful claim, Rellekka ends, and both return after the repeat refusal.

Cache 233.4 resolves the Rellekka expert to NPC 1503 (`hunting_polar_information`) and the Feldip
expert to NPC 1504 (`hunting_jungle_information`). Both are named `Hunting expert`, expose
`Talk-to` in operation slot one and have identity hashes 7359362605644340801 and
7359362605644340802. Production registers those exact routes, awards one Hunter kit 11159 and
sets permanent non-transmitted server varp 65497. The flag is shared by both experts and remains
set if the item is banked, opened, destroyed or otherwise lost. A full inventory leaves both the
inventory and entitlement unchanged; the current generic inventory-space line remains provisional.

Five cache-backed scenarios pin both NPC identities and handlers, permanent scope, the single
award, cross-expert repeat behavior, item-loss persistence, full-inventory rollback and the fact
that an independently created Lunar kit does not spend this one-time entitlement. The legacy
tutorial and Skillcape surfaces are tracked separately in the next section; exact expressions,
item zoom, capacity wording and packet order remain manual case 105 rather than being represented
as part of this focused Poll 85 grant core.

## Hunting-expert tutorials and Hunter Skillcape core

The final pre-cutoff
[Feldip transcript revision 14987663](https://oldschool.runescape.wiki/w/Transcript:Hunting_expert?oldid=14987663)
and
[Rellekka transcript revision 14987661](https://oldschool.runescape.wiki/w/Transcript:Hunting_expert_(Rellekka_Hunter_area)?oldid=14987661)
provide the complete legacy tutorial text, option order, nested creature menus and return/end
behavior for both cache NPCs. Production now follows those two pinned trees rather than stopping
after their entry lines. Feldip additionally switches at base Hunter 99 to the transcript's
`What would you like to talk about?` menu and its `Skillcape of Hunter.` route; temporary boosts do
not unlock that route.

The last applicable
[Cape of Accomplishment revision 14994254](https://oldschool.runescape.wiki/w/Cape_of_Accomplishment?oldid=14994254),
[Hunter cape revision 14843441](https://oldschool.runescape.wiki/w/Hunter_cape?oldid=14843441)
and
[Hunter hood revision 14824381](https://oldschool.runescape.wiki/w/Hunter_hood?oldid=14824381)
pin level 99, the 99,000-coin price, the included matching hood and trimmed-cape selection after a
second mastered skill. Cache 233.4 resolves coins 995, Hunter cape 9948, Hunter cape(t) 9949 and
Hunter hood 9950. The sale is one inventory transaction: coins, cape and hood either all commit or
all remain unchanged, including the boundary where deleting an exact coin stack frees one of the
two required result slots.

The cutoff Cape of Accomplishment page also embeds the music played after purchase. Its dedicated
page was created after the cutoff, so revision 15301964 is used only for stable asset metadata: the
jingle dates to the original 18 October 2006 cape release, lasts 15 seconds and is cache jingle 231.
The final cache independently contains that ID. Production emits the jingle only after the atomic
sale commits; all rejection paths remain silent.

Nine added scenarios pin the four cache objects, sale and jingle constants, base-level rejection,
insufficient coins, untrimmed and trimmed results, capacity rollback without audio, both ordinary
Talk-to menus and the complete level-99 purchase path through real dialogue pause/menu packets.
Exact facial expressions, automatic pagination, client jingle playback/mixing and packet order
still need workspace-client case 105. The historical page is also explicit that possessed and
stored untrimmed capes change when the second skill reaches 99, not when another cape is purchased;
that cross-skill level-up lifecycle and any lost-hood reclaim behavior absent from the pinned
Hunter transcript remain separate gaps rather than being inferred inside this seller.

## Hunter Skillcape boost and teleports

The final pre-cutoff [Hunter cape revision
14843441](https://oldschool.runescape.wiki/w/Hunter_cape?oldid=14843441) defines both item 9948 and
trimmed item 9949 as members objects with inventory `Wear`, `Teleport` and `Drop` operations. It
pins a visible +1 Hunter boost; five shared daily teleports to the carnivorous chinchompas in the
Feldip Hunter area and black chinchompas in the Wilderness; and an unlimited Hunter Guild teleport
after speaking to Guildmaster Apatura with either cape worn or carried. Its map links resolve the
three destination squares to 2556,2917, 3144,3772 and 1558,3046. The current transportation and
temporary-boost tables independently preserve the five-shared-use and +1 contracts; production
uses the programme's established UTC reset convention for daily item teleports.

The final Apatura transcript before the cache cutoff is [revision
14689838](https://oldschool.runescape.wiki/w/Transcript:Guildmaster_Apatura?oldid=14689838), dated
30 June 2024. It pins the complete enhancement exchange from `Nilsal, hunter.` through `Kuani!
That should be done for you now.`, the Yes/No options and the two-option standard follow-up. It
also proves that initial dialogue and 50/100/150-rumour history branches belong to the same NPC;
those broader routes are not silently claimed by this cape slice.

Cache 233.4 independently resolves Guildmaster Apatura as NPC 13120 with operation-one `Talk-to`,
and permanent varp 1312 as `skillcape_perks`. Both cape objects expose the worn operations in this
exact order: `Boost`, `Hunter Guild`, `Carnivorous Chinchompas`, `Black Chinchompas`. Production
registers those four direct worn routes and inventory operation three, keeps the guild choice out
of the inventory menu until permanently unlocked, and preserves the five-use count across the two
limited destinations and both cape variants. Rejections for membership, active Tele Block and
Wilderness levels above 20 leave both position and quota unchanged; the unlimited guild route does
not consume the chinchompa quota.

Eight focused scenarios pin the cache operations, NPC, permanent varp and coordinates; the boost
cap; dynamic menu; shared quota and UTC rollover; four-tick effect boundary; unlimited locked guild
route; access failures; unrelated-varp-bit preservation; and the exact enhancement transcript for
both capes. The standard teleport sequence 714, spot animation 111 and synth 200 follow the existing
rsmod teleport presentation pattern, not a direct historical Hunter-cape capture. Cache varp 1312
does not expose child varbits, so the current Hunter field allocation reserves its low 21 bits and
preserves all others; matching-production bit allocation, warning/toggle behavior before entering
the black-chinchompa area, exact locked-guild response, full Apatura progression and audiovisual or
packet timing remain manual case 106.

## Automatic ordinary Skillcape trimming core

The last applicable [Cape of Accomplishment revision
14994254](https://oldschool.runescape.wiki/w/Cape_of_Accomplishment?oldid=14994254) states that when
a player achieves a second level 99, every untrimmed cape in the player's possession, bank or
costume room becomes trimmed irreversibly. It further records the acquisition distinction: an
ordinary untrimmed cape picked up after that point trims, while a cape dropped before the second
99 and recovered with Telekinetic Grab can remain untrimmed. The same revision names death
storage, the Forestry clothes pouch and the Huntsman's kit as preservation cases. Those details
are source requirements, not permission to invent integrations for storage systems absent from
this repository.

Cache 233.4 independently pins all 23 ordinary untrimmed-to-trimmed object pairs: Attack
9747→9748, Defence 9753→9754, Strength 9750→9751, Hitpoints 9768→9769, Ranged 9756→9757, Prayer
9759→9760, Magic 9762→9763, Cooking 9801→9802, Woodcutting 9807→9808, Fletching 9783→9784,
Fishing 9798→9799, Firemaking 9804→9805, Crafting 9780→9781, Smithing 9795→9796, Mining
9792→9793, Herblore 9774→9775, Agility 9771→9772, Thieving 9777→9778, Slayer 9786→9787,
Farming 9810→9811, Runecraft 9765→9766, Hunter 9948→9949 and Construction 9789→9790.

Production uses rsmod's existing protected advance-stat engine event. Only the transition that
leaves the advanced skill at its maximum level is considered; the player must then have at least
two of the 23 production skills at level 99. Every mapped untrimmed cape in inventory, worn
equipment and bank is replaced in its original slot with the trimmed identity while preserving
count and item variables, and worn changes rebuild appearance. The ordinary ground-object `Take`
pipeline has a general ordered identity-transform seam; the Skillcape policy uses it only when the
player already has at least two mastered production skills. Telekinetic Grab retains its separate
direct acquisition pipeline and therefore preserves the original untrimmed identity exactly as the
historical page requires.

Seven integration scenarios pin all 23 cache pairs, the first-99/sub-99 no-op boundary, every
inventory family, Hunter capes across all three connected stores, ordinary ground pickup before
and after second mastery, and real Telekinetic Grab after second mastery. POH storage, death
storage, Forestry clothes-pouch and Huntsman's-kit behavior remain manual case 107.

## Ordinary Skillcape visible-boost core

The `Skill boost` section of [Cape of Accomplishment revision
14994254](https://oldschool.runescape.wiki/w/Cape_of_Accomplishment?oldid=14994254) states that both
equipping a Cape of Accomplishment and selecting its worn `Boost` option temporarily raise the
matching effective skill by one, up to 100. It explicitly gives Hitpoints and Prayer a one-minute
cooldown between successful uses, prevents every cape from stacking again until the skill has
returned to 99, and assigns the ordinary visible-boost lifetime of up to 60 seconds.

Cache 233.4 independently maps every ordinary untrimmed and trimmed cape to the same 23 production
skills used by the trimming definition. Forty-four cape identities expose worn operation one as
`Boost`: the existing Hunter plugin owns objects 9948/9949 and the shared Skillcape plugin owns the
other 42. Agility objects 9771/9772 are the sole cache exception: their worn operation-one strings
are blank. Production therefore applies Agility's sourced equip boost but does not fabricate an
unreachable client menu entry.

The shared plugin listens to rsmod's post-transaction wear-position event and confirms the incoming
object is actually present in the back slot, so failed equips and unequips cannot activate an
effect. Worn operations use the existing protected object-specific script syntax. The normal
`statBoost` path supplies the base-plus-one cap and shared visible-stat restoration timer.
Hitpoints and Prayer successful changes write independent server-only permanent ready-at values
65495 and 65496, using a 2025 epoch offset to keep exact seconds safely within an integer; blocked
or already-capped attempts neither change the stat nor extend the deadline.

Five new integration scenarios pin the cache operations and persistent fields, exercise the real
worn route for every shared-handler cape variant, equip all 46 identities through `HeldEquipOp`,
prove cap and reactivation behavior, and prove the two exact independent 59/60-second boundaries.
The dedicated Hunter scenario continues to cover its two worn routes. Exact client decay phase,
cooldown feedback, relog presentation and the Agility source/cache discrepancy remain manual case
108.

## Ordinary Skillcape emote core

The `Emote` section of [Cape of Accomplishment revision
14994254](https://oldschool.runescape.wiki/w/Cape_of_Accomplishment?oldid=14994254) states that each
cape has an emote representing its skill and that it can only be performed while the matching
skillcape is worn. The same dated page's change history records that Skillcape emotes cannot be
performed aboard the Fishing Trawler. The Music cape exception belongs to the existing special-cape
branch and is not generalized to the 23 ordinary production skills.

Cache 233.4 enriches exactly 46 ordinary cape objects—untrimmed and trimmed variants for each of
the 23 released stats—with `content.skill_cape` and `statreq1_skill`. The emote plugin's typed
`skill_cape_anims` and `skill_cape_spots` enums contain one animation and one graphic for every
released stat. The runtime reads the worn back-slot object, resolves its stat parameter, rejects
both PvM and PvP combat, applies the mapped pair and keeps protected access for the cache sequence
duration before rebuilding appearance. Its resolver now validates both maps instead of testing the
animation map twice.

The focused integration suite pins all 46 object identities, proves exactly two variants per
released stat, and sends the real `Skill Cape` interface input through an isolated simulated player
for every object. A synchronous `PlayEmote` observer verifies the selected sequence and the graphic
already attached to the player, including one-tick sequences that are cleared by post-tick cleanup.
Separate interaction coverage proves an inventory-only cape is insufficient and combat leaves both
animation and graphic absent. Fishing Trawler state and exact rejection feedback remain manual case
109 because this cutoff workspace has no implemented Trawler activity/area contract to gate against.

## Smithing Skillcape gold-XP perk core

The final pre-cutoff [Smithing cape revision
14873606](https://oldschool.runescape.wiki/w/Smithing_cape?oldid=14873606) states that wearing either
cape variant gives the same experience-boosting effect as goldsmith gauntlets. The cutoff
[goldsmith-gauntlets revision
14965070](https://oldschool.runescape.wiki/w/Goldsmith_gauntlets?oldid=14965070) fixes that effect at
56.2 Smithing XP instead of the ordinary 22.5 for each gold bar, and the cutoff [Superheat Item
revision 14981805](https://oldschool.runescape.wiki/w/Superheat_Item?oldid=14981805) explicitly
includes the Smithing cape effect for spell-smelted gold.

The two existing production paths independently inspect the appropriate worn slot. Ordinary
furnace smelting and Superheat Item both accept goldsmith gauntlets in the hands slot, either
Smithing cape identity in the back slot or the worn max-cape identity. Focused integration tests
now iterate all four equipment identities through each action and pin the 56.2-XP result, closing
the prior coverage gap for the two cape variants without changing runtime behavior.

Revision 14873606 also gives the worn Smithing cape a second perk: coal-bag capacity increases from
27 to 36. The final pre-cutoff [Coal bag revision
14962565](https://oldschool.runescape.wiki/w/Coal_bag?oldid=14962565) defines both object identities,
their five operations, bank use, automatic filling while open and loose-coal-first consumption.
Those behaviors are implemented and certified as the separate coal-bag capacity core below; the
absent Blast Furnace activity remains outside this gold-XP certification and in manual case 110.

## Coal bag and Smithing Skillcape capacity core

The final pre-cutoff [Coal bag revision
14962565](https://oldschool.runescape.wiki/w/Coal_bag?oldid=14962565) specifies a members-only bag
holding 27 coal, or 36 while a Smithing cape is equipped. It identifies closed object 12019 and
open object 24480, lists Fill, Open/Close, Check, Empty and Destroy, permits bank Fill/Empty,
requires the bag to be empty before destruction, and states that mined coal is inserted
automatically only while the bag is open. It also fixes furnace and Superheat consumption order as
loose inventory coal before stored coal. [Smithing cape revision
14873606](https://oldschool.runescape.wiki/w/Smithing_cape?oldid=14873606) independently confirms
the 27-to-36 capacity perk, while [Max cape revision
14993265](https://oldschool.runescape.wiki/w/Max_cape?oldid=14993265) establishes that the worn max
cape inherits production Skillcape perks.

Cache 233.4 independently pins the same two object identities, members flags and five inventory
operations. Both objects expose the bankside extra operation and use varbit
`coal_bag_storage_count`, whose server definition occupies bits 25–30 of the shared permanent
storage varp. The production plugin follows existing rsmod held-operation and bankside-event
patterns for both variants. It uses transactional inventory/bank changes, preserves counts above 27
when qualifying equipment is removed, and only permits an empty bag through the cache-native
confirmation dialogue. Ordinary Mining now bypasses its full-inventory rejection only for coal
that can actually enter an open bag, then increments the same state consumed by ordinary furnace
smelting and Superheat Item.

Six isolated integration scenarios cover the cache contract, ordinary inventory/held-use Fill,
all three qualifying worn cape identities, non-truncating unequip behavior, Open/Close/Check,
partial Empty, bank Fill/Empty, members rejection and both Destroy choices. The Mining suite sends a
real coal-rock interaction with a completely full inventory and proves the coal reaches the open
bag rather than inventory. Exact success/check wording, deposit-box prohibition, placeholder and
equipped-bank presentation, acquisition/one-bag ownership and matching-client packet/audiovisual
order remain manual case 110.

The same dated Coal bag page contains an explicit exception to the generic cache restriction:
attempting either alchemy spell destroys the bag regardless of whether coal is stored. Both bag
objects carry the cache `no_alchemy` parameter, so the production alchemy path admits exactly IDs
12019 and 24480 while retaining the ordinary rejection for every other marked object. Successful
Low or High Level Alchemy performs its usual rune, XP, value, animation, graphic, sound and cooldown
contract, deletes the target variant and clears the permanent coal count so a later replacement bag
cannot recover ghost contents. A focused integration scenario proves both variants at 27 and 36
stored coal and also pins the otherwise-conflicting cache flag. Exact client warning behavior and
packet order remain in manual case 110.

## Woodcutting Guild invisible-boost core

The final pre-cutoff [Woodcutting Guild revision
14975699](https://oldschool.runescape.wiki/w/Woodcutting_Guild?oldid=14975699) defines an invisible
+7 Woodcutting boost throughout the guild. It stacks with visible boosts, improves success chance
without permitting actions above the player's unboosted level, and replaces rather than combines
with the Forestry group bonus while inside the guild. The same dated page supplies the exact
47-vertex map polygon used by the implementation.

The polygon is registered through rsmod's native `MapAreaBuilder` and the new cache area symbol
32763. Planes 0–3 share the boundary, while the similarly aligned underground Woodcutting Guild
dungeon is excluded by its distinct world coordinates. Packing writes the affected map groups
`a24_54` and `a25_54`; production `AreaChecker` membership contributes seven through the existing
`InvisibleLevels` hook, preserving the separate visible-level requirement path.

The integration scenario builds the dated polygon directly, decodes both packed map groups from the
production cache, and checks representative interior, exterior, concave-boundary, upper-plane and
dungeon coordinates through the production area checker. It then proves exactly +7 inside and zero
outside. Exact matching-client success-rate sampling, visible-boost interaction presentation and
boundary observation remain manual case 111.

## Forestry group Woodcutting boost core

The final pre-cutoff [Woodcutting revision
14991189](https://oldschool.runescape.wiki/w/Woodcutting?oldid=14991189) states that each player
chopping the same tree contributes an invisible +1 Woodcutting bonus, including the player being
evaluated, up to +10 for ten players. It also excludes normal trees, every tree in the Woodcutting
Guild and trees grown through Farming. Jagex's [Forestry: The Way of the Forester - Part
One](https://secure.runescape.com/m=news/forestry-the-way-of-the-forester---part-one?oldschool=1)
independently establishes the +1-per-player, +10 cap and normal-tree/Guild exclusions. The group
boost is available in free-to-play and, like the guild boost, changes success chance without
unlocking actions above the player's separately checked visible level.

The implementation counts only online, visible, living players whose current interaction is the
Woodcutting operation on the exact same bound location and whose avatar collides with or touches
that location under rsmod's production `BoundValidator`. It queries the tree's neighbouring zones
and stops at ten matches. Eligibility is tied to the existing static-tree `despawn_time` parameter:
that is the structural distinction already used by the Woodcutting plugin for Forestry-timed world
trees, while normal/dead trees and Farming-grown trees are outside that catalogue. The guild area
branch takes precedence and returns exactly +7 instead of adding the group count.

The integration scenario proves a free-to-play solo +1, excludes stopped players, players on a
different tree, out-of-range players and non-Woodcutting loc operations, pins the +10 cap with a
larger group, and proves normal trees receive zero. Exact matching-client group-icon timing,
success-rate sampling and audiovisual/packet presentation remain manual case 111.

## Regular Woodcutting bird-nest core

The final pre-cutoff [Bird nest revision
14924135](https://oldschool.runescape.wiki/w/Bird_nest?oldid=14924135) states that a regular nest is
rolled at 1/256 each time a player would otherwise receive a log, regardless of eligible tree type,
and that a worn Woodcutting cape adds 10% to that chance. The final pre-cutoff [Yew tree revision
14986999](https://oldschool.runescape.wiki/w/Yew_tree?oldid=14986999) makes the resulting integer
rate explicit as 1/230 with the cape, says the nest is received instead of the log, and pins a floor
drop that despawns after two minutes. The final pre-cutoff [Woodcutting revision
14991189](https://oldschool.runescape.wiki/w/Woodcutting?oldid=14991189) identifies regular nests as
members-only and excludes redwood while still allowing clue nests there. The Bird nest page and
the equivalent Bird's egg evidence also exclude blisterwood from regular nests.

Mod Ash's [26 October 2018 table disclosure](https://twitter.com/JagexAsh/status/1055815273763217414)
defines the exact roll: without the strung rabbit foot select 0–99; with it select 0–94; 0 is the
red egg nest, 1 blue, 2 green, 3–34 ring and every remaining slot the modern seed nest. This yields
3/100 egg, 32/100 ring and 65/100 seed normally, or 3/95, 32/95 and 60/95 while the necklace is
worn. The cache pins objects 5070–5072, 5074, the January-2019 seed nest 22798 and necklace 10132.
The historical page's [13 December 2015 Mod Ash
citation](https://twitter.com/JagexAsh/status/676061064895819778) says the nest lands on a random
tile adjacent to the player with a slight westward bias. The implementation therefore uses every
adjacent tile and one extra west entry; the source does not expose the live weighting, so that
exact probability remains a matching-client qualification rather than a verified claim.

The Woodcutting reward path now tests this only after a successful log roll. An accepted nest
suppresses the log, Woodcutting XP and `CutLogs` event, sends the exact red client-recognized message
and creates an owner-private adjacent floor object using the standard repository lifecycle: reveal
after 100 ticks and deletion after 200 ticks. F2P, redwood and blisterwood exits occur before any
nest random is consumed. A focused integration suite pins both denominators, all table boundaries,
the rabbit-foot table size, adjacent/west-bias structure, reward replacement, ownership, reveal and
expiry. Clue nests, Twitcher's-glove selection, the chirping/rare Armadyl-crossbow sound, exact
west-bias weighting and matching-client packet order remain manual case 112.

## Special Woodcutting axe harvesting and Lumber Up cores

The final pre-cutoff [dragon axe revision
15000721](https://oldschool.runescape.wiki/w/Dragon_axe?oldid=15000721) and [3rd age axe revision
14926278](https://oldschool.runescape.wiki/w/3rd_Age_axe?oldid=14926278) pin members-only level 61,
dragon-equivalent chopping and the shared Lumber Up special attack. The final pre-cutoff [Crystal
axe revision 14963010](https://oldschool.runescape.wiki/w/Crystal_axe?oldid=14963010) pins
members-only level 71 plus Song of the Elves, an active 15% improvement over rune versus dragon's
10%, one charge per item obtained, an inactive dragon-axe fallback, 10,000 initial and 20,000 maximum
charges, and 100 charges per shard. Cache 233.4 independently supplies dragon 6739/25378/30352, 3rd
age 20011, Crystal 23673/23675 and their family Woodcutting sequences.

The final pre-cutoff [Infernal axe revision
14997416](https://oldschool.runescape.wiki/w/Infernal_axe?oldid=14997416) pins members-only level 61,
a 1/3 chance to consume an eligible chopped log, half its ordinary Firemaking XP without applying
the normal Firemaking-level requirement, Pyromancer outfit support, one charge only when a log is
burned, 5,000 burns and a depleted dragon-axe fallback. It expressly excludes bruma roots,
sulliuscep mushrooms and celastrus bark; infected roots consume a charge only when a log is burned
and never burn demonic tears. The pre-cutoff [original Trailblazer ornament revision
14853052](https://oldschool.runescape.wiki/w/Infernal_axe_(or)?oldid=14853052) and [Trailblazer
Reloaded revision
14840824](https://oldschool.runescape.wiki/w/Infernal_axe_(or)_(Trailblazer_Reloaded)?oldid=14840824)
pin the charged/depleted pairs 25066/25371 and 30347/30348 alongside base 13241/13242.

The production Woodcutting editor registers all twelve special identities through the same
content-group, typed level and animation parameters as ordinary axes. Every tree success table maps
all dragon-equivalent identities to the existing dragon endpoint and only active Crystal to the
existing Crystal endpoint. Best-tool selection uses the real efficiency order, distinguishes iron
from bronze at their shared level, rejects members axes on free worlds and requires quest varbit 194
for both Crystal states. Persisted item variables represent implicit full Infernal and Crystal
states; successful burns and obtained logs transform the last charge into the cache's depleted
identity whether the selected axe is carried or worn.

The final pre-cutoff [Elven signet revision
14989911](https://oldschool.runescape.wiki/w/Elven_signet?oldid=14989911) and [Celestial signet
revision 14888892](https://oldschool.runescape.wiki/w/Celestial_signet?oldid=14888892) pin an
equipped 1/10 Crystal-tool charge save for IDs 23943/25543/25545. The final pre-cutoff [Ghommal's
lucky penny revision
14964658](https://oldschool.runescape.wiki/w/Ghommal%27s_lucky_penny?oldid=14964658) pins an
independent equipped 1/20 save for both Crystal and Infernal tools. Production rolls those effects
independently, and the focused five-scenario integration suite covers every identity, access and
rate mapping, carried/worn priority, burns, XP, conservation, partial/final charges and a complete
tree interaction.

The Infernal page also states that every dragon, 3rd age, Infernal and Crystal variant shares Lumber
Up: 100% special energy for a temporary +3 Woodcutting boost, with the ordinary restoration and
Preserve rules. Cache 233.4 maps all twelve identities to the special-energy table, sequence 2876
and the red dragon/Infernal or silver 3rd-age/Crystal graphic family. A registry-level test invokes
every form and pins its energy, boost, `Chop chop!` phrase and sequence. A production-bootstrap
integration suite independently registers and activates all twelve forms, also pinning the selected
graphic, height, combat slot and full-energy deduction contract.

The same Infernal revision pins a three-tick members conversion of dragon axe 6739 plus smouldering
stone 13233, unboosted Woodcutting 61 and Firemaking 85, 200/350 XP and an implicit 5,000-charge
result. It separately states that either a stone or another dragon axe fully recharges a depleted
Infernal axe and awards the same XP. Production implements all three depleted-to-charged style
pairs, preserves packed charge variables through original Trailblazer and Reloaded ornament
transforms, supports cache-shifted inventory/worn operations, and reports remaining charges as an
integer percentage. The exact live percentage sentence and sub-one-percent rounding remain a
matching-client capture item rather than an asserted Wiki fact.

The Crystal revision pins 100 charges per shard beyond the initial 10,000, accepted from either the
active or zero-charge identity up to 20,000, and Revert returning dragon axe 6739 plus crystal tool
seed 23953 while losing all shards. Production uses a native bounded quantity dialogue, revalidates
both inputs after the response, preserves partial charge state, reports the exact stored charge
count and makes Revert atomic when a second output cannot fit. Six focused integration scenarios
cover cache operations and dispatch, creation timing and base-level gates, every recharge style,
both material orders, both ornament styles, carried/worn changes, shard caps, inspection and active
and inactive Revert.

The same historical Crystal revision and the pre-cutoff Dragon axe product table pin crystal-axe
creation from dragon axe 6739, crystal tool seed 23953 and 120 shards at boostable Crafting and
Smithing 76, awarding 6,000 XP in each skill, taking three ticks for an inventory and starting at
10,000 charges. The Crystal equipment overview independently pins a 60-shard singer premium and no
player skill requirement. Reese revision 14897905 identifies IDs 9133/9134 and documents the shared
Sing-option unlock with Conwenna; cache 233.4 resolves the actionable learned forms as 9134/9136,
their `Sing` text in operation three, bowl 36552's `Sing-crystal` first operation and player sequence
8459. Production routes those exact operations through the native skill-multi chooser, revalidates
membership, Song of the Elves state 194, visible boosted levels and all ingredients after three
ticks, converts every available complete set atomically, awards XP only for self-singing and packs
10,000 charges into each result. The full product catalog and dedicated crystal-singing interface,
Reese's exact Talk-to unlock transcript and per-player NPC-state selection, exact excluded-tree and
final-charge presentation, death/trade state and matching-client audiovisual or packet order remain
manual case 113.

## Shared bank Configure-Charges and Crystal axe preference core

The Bank article's documented 27 August 2025 action-code update and the workspace RuneLite
`LayoutManager` agree on the cache-233 menu layout: when bank item options are enabled and the
banked object carries BANK_AUTOCHARGE param 2257, menu index eight is `Configure-Charges`; index
nine is `Placeholder`; index ten is `Examine`. Cache component `bankmain:bankops_toggle` is 12:61
and varbit 16125 is `bank_bankops_toggle_on`. The server now enables and decodes the corresponding
operations 9–11, preserves the existing withdrawal semantics for Placeholder, and publishes a
protected object-keyed event only when the toggle and cache param both permit configuration. This
keeps charge-specific policy in each item plugin and rejects fabricated configuration packets.

Cache 233.4 gives active crystal axe 23673 BANK_AUTOCHARGE param 2257 pointing to DB row 4338
`charges_crystal_tools`. Its account-persistent target quantity is varbit 16148 over permanent varp
4681 bits 0–15. The Woodcutting plugin consumes the shared bank event and stores a members-only
preference bounded from zero (disabled) through the sourced 20,000-charge Crystal-tool maximum.
Focused bank integrations pin the setting toggle, packet-to-operation decoding, event routing and
shifted Placeholder/Examine behavior; the lifecycle integration pins the typed DB-row param, exact
varbit layout, event registration, cap and membership rejection. Exact dialogue presentation and
the resource-consuming automatic refill performed on later withdrawals remain manual case 113.

## Woodcutting clue-nest core

The final pre-cutoff [Clue nest revision
14986594](https://oldschool.runescape.wiki/w/Clue_nest?oldid=14986594) gives the live integer formula
as `floor(floor(BaseChance * CA) / (100 + visible Woodcutting) * ClueTier)`. The completed Combat
Achievement multiplier is 0.95 for the corresponding easy, medium, hard or elite tier and 1
otherwise; beginner has no achievement reduction. The clue-tier multipliers are 0.2, 1.7, 2.0,
3.3 and 10.0. Each eligible tier receives an independent roll, and already owning that tier skips
only its roll. An imbued ring of wealth doubles the rate in the Wilderness, while the Woodcutting
cape does not affect clue nests.

The same revision pins base chances of 317,647 for normal and achey, 361,146 oak, 289,286 willow,
264,336 teak, 360,000 juniper, 221,918 maple, 214,367 hollow, 220,623 mahogany, 145,758 arctic pine,
145,013 yew, 343,000 sulliuscep and 72,321 magic/redwood. The ordinary Woodcutting tree catalogue
now stores each supported-family value in custom parameter 65525 and rolls clue nests only after a successful cut,
before the separate regular-nest replacement. A clue success therefore remains tertiary: the log,
Woodcutting XP and `CutLogs` event continue normally. Cache objects 23127/19712/19714/19716/19718,
Arthur's permanent vessel-clue opt-out varbit 9365, Combat Achievement varbits 12863–12866 and all
six imbued-ring charge identities are pinned by integration tests. Eligible nests use the standard
owner-private adjacent floor-object lifecycle and the client-recognized red nest message.

Sulliuscep is an activity-specific path absent from the ordinary-tree catalogue, so its documented
343,000 base remains unassigned until that activity exists; the integration test pins every other
currently supported product family and does not overstate Sulliuscep coverage.

The dated page records Twitcher's gloves as an approximately 20% chosen-nest modifier and the 2
July 2025 X Marks the Spot change from clue nests to an ordinary clue scroll box plus empty nest.
The final pre-cutoff [Scroll box revision
14987736](https://oldschool.runescape.wiki/w/Scroll_box?oldid=14987736) confirms that boxes are
stackable, use the normal three-minute ground-item despawn instead of a clue scroll's one hour and
replace clues after quest completion. Capacity starts at two, minor and major scroll cases and the
Mimic case each add one through a maximum of five, and a successful clue roll at the cap emits the
exact tiered sneaking-suspicion warning. The final pre-cutoff [X Marks the Spot revision
14994829](https://oldschool.runescape.wiki/w/X_Marks_the_Spot?oldid=14994829) independently records
the ability to receive boxes, a beginner box reward and the first extra clue capacity.

Cache inspection corrects the earlier ledger assumption: objects 24361–24366 have semantic names
`Scroll box (beginner)` through `Scroll box (master)`, the ordinary stackable/tradeable definition
and matching clue descriptions. Their internal `league_clue_box_*` symbols are historical naming,
not evidence that they were inaccessible in the cache-233 production game. Object 5075 is the
empty bird nest. Quest varbit 8063 (`cluequest`) is a six-bit field over varp 2111; DB row 162
column 19 defines completion state 8. Client script 4029 routes X Marks through script 4024, whose
row-162 switch target reads varbit 8063. `ClientScriptSemanticInspectorIntegrationTest` pins that
complete semantic route instead of relying on the symbol name alone.

Scroll-case varbits 16565–16594 encode each tier's minor/major ownership and varbit 16595 encodes
the Mimic case, all over varp 4730. The shared `ClueInventory` now discovers clue scroll, nest,
bottle, geode and scroll-box identities from exact cache display names and counts inventory plus
bank stacks. It intentionally excludes keys, caskets, placeholders and ground items; Death's Office
storage remains an explicit gap. Post-quest Woodcutting successes use the same adjacent tile for the
three-minute box and two-minute empty nest, while pre-quest rolls retain the two-minute clue nest.
Exact Twitcher's-glove selection, legacy nest/box opening, Death's Office ownership, sound, tile
weighting and packet order remain manual case 112.

## Core cache inventory evidence

The future-cache tool opened the staged OpenRS2 archive 2317 cache directly and enumerated every
definition in the three implemented core domains: 31,172 objects, 14,799 NPCs and 57,724 locations,
for 103,695 stable `domain/key` identities. The output at
`.data/audit/cache-233.4-core-manifest.json` has SHA-256
`baf6400cac0b057c6050b102347f22c55dc1b61499c9ed7fe5171df067ba88f5`. A self-comparison through
the production delta engine reports zero added, removed or changed entries and 103,695 unchanged.
The cache definition counts are authoritative for inventory completeness; symbol files provide
names, not permission to omit a cache definition.

The symbol reconciliation found no unnamed objects. Location IDs 57690–57702, 57715–57718 and
57719–57723 are missing from `loc.sym`; visible decoded names include 57693 `Bookcase`, 57719
`Bridge support` and 57723 `Energy Barrier`. NPC IDs 14793–14794 (`Rantz`) and 14795–14798
(`Verzik Vitur`) are missing from `npc.sym`. Conversely, object symbol ID 65534 has no corresponding
cache definition. The exporter retains all unnamed cache identities with a null symbol and does not
manufacture names; the stale symbol is recorded as an audit finding rather than exported as content.

## Semantic config inventory evidence

The broader `export-configs` pass adds 4,876 varps, 18,031 varbits, 1,261 varclients, 107 vanilla
varobjects, 936 inventories, 2,318 parameters, 5,625 enums, 5,879 structs, 117 database tables,
6,655 database rows, 12,521 sequences and 3,456 spot animations to the core inventory. Together the
15 domains contain 165,477 exact cache identities. The 116,612,793-byte local result at
`.data/audit/cache-233.4-semantic-config-manifest.json` has SHA-256
`fe869b9fc02b89a41778a4d8b92695c2b5248ec409192c990acb01d1b53fff7a`. Two independent exports
have that same checksum, and the production delta command reports zero added, removed or changed
and 165,477 unchanged when the result is compared with itself.

Every entry retains its exact raw-config fingerprint. Typed definitions also expose canonical
decoded fields: player-variable scope/transmit and bit ranges; inventory scope/stack/flags/default
stock; parameter type/default/transmit; enum literals/defaults/entries; struct params; database
column types/defaults/attributes and row data; complete sequence frame, delay, loop, movement,
keyframe and sound metadata; and spot-animation model, animation, resize, recolour and retexture
metadata. All 1,261 varclients have revision-matched symbols; strict opcode decoding classifies 494
as persistent and 767 as transient, rejecting unknown opcodes, trailing bytes and unterminated
records. Of the enum records, 3,165 lack a key or value literal and therefore cannot form a typed
enum; the exporter preserves each raw identity as an explicit opaque record instead of dropping it.
The same bounded treatment retains all 107 vanilla varobjects with raw fingerprints: no semantic
decoder or matching vanilla symbol is currently available, so neither meaning nor a name is
fabricated.

The untouched vanilla cache remains authoritative when reconciling the repository's enriched
symbol namespace. Unnamed vanilla definitions total 5,546 enums, 2,145 params, 5,879 structs, 107
varobjects, 5,773 varbits and 2,597 varps, in addition to the previously recorded 22 locations and six NPCs;
inventories, database types, sequences and spot animations have complete symbol coverage. Symbols
without vanilla records total four inventories, 299 params, 135 enums, 2,303 varps, 780 varbits,
three database tables and four database rows; sequences, spot animations and structs have none.
Those names may represent server enrichment, custom types or symbol/cache drift, so they are audit
evidence rather than synthetic vanilla definitions.

## Atomic future-cache backlog evidence

The production `plan-upgrade` command reads the same validated baseline and target manifests,
executes the deterministic delta engine and emits one task for every added, removed or changed
`domain/key` identity. Each ID combines the readable domain with the first 128 bits of a SHA-256 over
domain, key and change kind, so it is stable without depending on list position. The task retains
both fingerprints and symbols, exact changed top-level and decoded-attribute fields, normalized risk
tags and a deterministic review priority.

Every generated task requires the available baseline/target cache definitions, revision-matched
symbols, official Wiki or update evidence and one review item for every risk tag. Its acceptance
list requires the fingerprint to be accounted for, an implementation or evidence-backed no-op,
cache-233.4 regression coverage, target-revision contract coverage, a feature-manifest update and
the full unit/integration gate. Map/collision, client-contract and protocol risks are critical;
other risk-bearing or non-additive deltas are high; a risk-free addition is normal. Unit tests pin
the task count, order, priority, changed fields, evidence and acceptance contracts, stable IDs and
invalid-report rejection. An integration test invokes the file-based production command twice and
proves byte-identical backlog output.

## Projectile config boundary evidence

The semantic exporter now probes JS5 config group 204 (`PROJANIM`) and, when it exists, fingerprints
each raw record and decodes start/end height, delay, angle, length adjustment, progress and step
multiplier with client-contract and projectile-timing risk tags. The staged vanilla archive 2317
does not contain that group. Its absence remains explicit in the current 165,477-entry semantic
manifest with SHA-256
`fe869b9fc02b89a41778a4d8b92695c2b5248ec409192c990acb01d1b53fff7a`.

The revision-matched `projanim.sym` contains 16 identities, numbered 0 through 15, while
`ProjAnimBuilds` supplies their actual projectile profiles in rsmod's enrichment layer. The exporter
does not convert those symbols into fake vanilla cache records. Unit coverage pins all seven decoded
fields for a present record; real-cache integration pins both the absent cache domain and the 16
server symbols.

The separate `export-projectiles` production command opens the rsmod-enriched runtime cache and
emits only those server-owned profiles under revision label `233.4-rsmod-enriched`. The durable
`.data/audit/cache-233.4-rsmod-projectile-profiles.json` contains all 16 identities and is 7,563
bytes with SHA-256
`3ef92868bd34edeaaf3bc14514ea0c7e4d4ceda6fc7bd0901d6fab8b797d5def`. A second independent
export has the same bytes and checksum. The production delta engine reports zero added, removed or
changed and 16 unchanged. This closes the build-layer profile inventory without merging enriched
records into the authoritative vanilla manifest.

## Enriched variable namespace evidence

The production `export-server-variables` command keeps rsmod-owned state definitions separate from
the vanilla cache manifest under revision label `233.4-rsmod-enriched`. It strictly decodes all six
cache-backed varns in config group 202 and would decode optional varnbit group 203 when present. It
also snapshots the build-layer definitions behind the symbol-backed server varobject, varcon and
controller namespaces, using a canonical source representation rather than pretending those
definitions have vanilla cache bytes.

The resulting `.data/audit/cache-233.4-rsmod-variable-namespaces.json` contains 21 identities: six
varns, eleven varobjects, three varcons and one controller. All have revision-matched symbols and
unique domain/key pairs; the current varnbit and varconbit symbol/config sets are empty. Cache-backed
records retain exact raw SHA-256 fingerprints. Build-owned records retain deterministic canonical
fingerprints and an explicit `source=rsmod-build-definition` attribute. Representative contracts
pin aggressive-player varn 0 as bit-protected, gnomish-firelighter-charges varobject 65527 to bits
0–9 (width 10), woodcutting-tree-cut-ticks varcon 0 and woodcutting-tree-duration controller 0.

The durable file is 7,704 bytes with SHA-256
`7ecefc34c096609e2e65d57056fc654723dcc81838db44a1063d579d06c79ff4`. A second independent export
is byte-identical. The production delta engine reports zero added, removed or changed and 21
unchanged.

## Map and collision-sensitive inventory evidence

The production `export-maps` command pairs the staged vanilla cache with its revision-matched XTEA
file. It rejects duplicate map-square IDs, keys that are not exactly four words, non-map archive
records and landscape group names that do not match the packed map-square coordinates. All 2,266
XTEA records pass those identity checks and decrypt both their `mX_Z` terrain and `lX_Z` location
files, yielding 4,532 stable domain/map-square entries.

Every entry fingerprints the complete decrypted file. Terrain records also decode the 16,384
four-plane tiles into explicit-height, overlay, underlay, block-walk, link-below, remove-roof,
visible-below and low-detail counts. Location records decode 4,319,171 placements in total and
retain per-square placement, distinct location ID/shape and per-level totals. The landscape group
ID and a non-reversible XTEA-key fingerprint remain in the attributes, so key-source drift is
reviewable without writing the key words into the manifest.

The four-plane terrain decoder leaves one byte on 2,265 files and 9,564 bytes on `m64_73`; the
workspace RuneLite `MapLoader` and rsmod's production `MapTileDecoder` both stop after the same
four-plane tile loop. The exporter fingerprints those bytes and reports the decoded/remainder byte
counts explicitly. Their semantics remain an open investigation and are not claimed as decoded
collision behavior.

The durable `.data/audit/cache-233.4-map-square-manifest.json` is 3,342,129 bytes with SHA-256
`55b0e443569f8f22482f4a8897683b52224d31bd727e2a574966b97f4f6ec3df`. A second independent export
is byte-identical. The production delta engine reports zero added, removed or changed and 4,532
unchanged. Integration coverage pins both domain counts, the complete paired key set, Lumbridge
square 50_50's collision and 4,728-placement summaries, and the exceptional 64_73 remainder.

## Enriched-runtime NPC placement evidence

The production `export-npc-spawns` command inventories the server-only `nX_Z` map groups that
RSMod's enrichment layer writes and `GameMapDecoder` loads at world startup. It uses the
revision-matched XTEA map-square catalogue to resolve group names, rejects duplicate squares and
malformed or trailing spawn bytes, decodes each packed NPC identity/local coordinate/plane, and
joins the enriched NPC type plus symbol tables. It emits one raw group contract and one canonical
placement contract per spawn. Duplicate NPCs on the same tile receive stable ordinals after
semantic sorting, so source declaration order cannot manufacture placement deltas.

The current cache-233.4 enriched runtime contains six NPC groups: `n49_49`, `n49_50`, `n49_51`,
`n50_49`, `n50_50` and `n50_51`, with respectively 60, 67, 135, 60, 166 and 97 placements. That is
585 placed NPCs across 136 distinct cache identities. All 585 placements resolve a typed symbol;
460 placements across 49 distinct identities expose an actual `Attack` operation and therefore
carry combat/drop-table risk. The inventory retains exact global/local coordinates, plane, name,
operations, combat stats and duplicate ordinal, while each group retains its raw fingerprint,
file length, per-plane total and distinct identity set.

The durable `.data/audit/cache-233.4-rsmod-npc-spawns.json` is 429,195 bytes with SHA-256
`1186494718430c80c9d58c1aba388aaac29ca057e6891b7ca7a0bb9e4a525a62`. Integration coverage pins
both domain totals, all six per-square counts, aggregate and attackable identity counts, symbol
coverage and fingerprint shape. This artifact proves the population that the current server loads;
it is enriched RSMod evidence, not a claim that the vanilla cache contains NPC spawns or that six
Lumbridge-area squares represent the full production OSRS world.

## Raw client-contract archive evidence

The production `export-client-contracts` command opens the staged vanilla cache directly and reads
every file in ten client-facing archives. The resulting stable identities and exact counts are:
2,464 animation bases, 175,868 animation frames, 25,602 interface components, 8,093 client scripts,
629 keyframe sets, 10,751 sound effects, 830 songs, 311 jingles, 501 Vorbis samples and 155 MIDI
patches, for 225,204 records in total.

Every entry records an exact SHA-256 of the unpacked file plus archive/group/file IDs, group version,
compressed and uncompressed index checksums and lengths, and available group/file name hashes. The
durable `.data/audit/cache-233.4-client-contract-archives.json` is 139,184,075 bytes with SHA-256
`3214d8b6c2a331401ee95dcbf1727d6e26da945ca5d606607a2f7c5325437e5d`. A second independent
production export is byte-identical and has the same checksum. Integration coverage pins every
domain count, identity uniqueness, fingerprint/risk shape and the exact bytes and metadata of
animation base `0:0`. The production comparison engine loaded both 225,204-entry results and
reported zero added, removed or changed entries and 225,204 unchanged.

This evidence makes any future byte-level change visible and backlog-addressable. It does not claim
that an unchanged file proves gameplay parity, or that a changed file's semantics are understood;
interfaces, scripts, animations and audio still require semantic decoders or targeted client review.

## Whole-cache archive-index identity evidence

The production `export-archive-indexes` command opens the staged vanilla cache and inventories every
JS5 group in every present archive. It does not read group payloads, so encrypted map landscapes and
currently undecoded formats are covered without requiring keys or pretending to understand their
semantics. Each stable `archive:group` identity fingerprints the group version, unsigned compressed
and uncompressed checksums, compressed and uncompressed lengths, group name hash, and exact ordered
file ID/name-hash set.

The untouched cache-233.4 baseline contains 116,016 groups and 640,720 file identities across 23
non-empty archive indexes. The indexed groups account for 148,468,920 compressed and 768,183,776
uncompressed bytes. Archive 5 contains 14,437 groups; its Lumbridge square group `5:12850` is
pinned to checksum 2,906,976,476, group-name
hash -838,284,128 and canonical fingerprint
`sha256:065e70a11be0fa72db530999794ec7d0b2d0ebb42c3b065f64a9c1b6354b0602`.

The durable `.data/audit/cache-233.4-archive-index-manifest.json` is 73,039,829 bytes with SHA-256
`26ef85c11abc5a056df60c9cbc264f031e442ae2d455d49e1a5fa33c295227ae`. A second independent export
is byte-identical. The production delta engine reports zero added, removed or changed and 116,016
unchanged. Integration coverage pins all 23 per-archive group totals, 640,720 aggregate file
identities, identity uniqueness, fingerprint/risk shape and the Lumbridge group contract.

The [official OpenRS2 archive-2317 record](https://archive.openrs2.org/caches/runescape/2317)
independently identifies oldschool/live/en build 233 captured on 2025-10-01 and reports all 116,016
groups present. It displays 25/25 archive slots; the local exporter intentionally emits records for
the 23 non-empty indexes, because slots 16 and 23 contain no groups.

rsmod's `.data/cache/game` is a derived runtime cache. Its separately labelled
`.data/audit/cache-233.4-rsmod-enriched-archive-index-manifest.json` is 73,266,266 bytes with
SHA-256 `d0439f11fa33e31d9e29f28d6d22535db843727d5130e0c36660003e5c4cd791`. It contains 116,389
groups and 641,449 file identities. The production vanilla-to-enriched delta has 373 additions,
zero removals, 12 changes and 116,004 unchanged groups. The additions are config groups
`2:200`, `2:201`, `2:202`, `2:204`, `2:205` plus 368 archive-5 groups; enrichment rewrites
existing config groups `2:2`, `2:5`, `2:6`, `2:8`, `2:9`, `2:10`, `2:11`, `2:14`, `2:16`,
`2:18`, `2:38` and `2:39`. Integration coverage pins this exact boundary so enriched server
content cannot be misrepresented as vanilla source evidence.

## Revision contract and staged acquisition evidence

`revision-contract.json` is the machine-readable release lock for oldschool/live/en build 233.4.
It binds OpenRS2 archive 2317 and its capture time/catalog/download URLs to 116,016 vanilla groups,
640,720 file identities, the exact archive-index manifest SHA-256
`26ef85c11abc5a056df60c9cbc264f031e442ae2d455d49e1a5fa33c295227ae`, 2,266 XTEA records and
XTEA-file SHA-256 `78a4b5a47254c796aff994d1900d34c78a8486bb1cfdb3d5384b6b49b1c686db`.
It also binds rsprot version `1.0.0-ALPHA-20250909` to modules `net.rsprot:osrs-233-api` and
`net.rsprot:osrs-233-shared`.

The root `validateCache233Parity` task reads that contract and rejects disagreement with parity
target metadata, every relevant `Build` constant, both dependency-catalog modules and the checked
73,039,829-byte vanilla manifest's computed checksum. This turns the revision, cache, XTEA and
protocol pins into one cross-checked release contract.

`GameServerCacheDownloader` no longer deletes the accepted vanilla cache before network work. It
downloads the archive and XTEAs into a sibling staging directory, verifies the required disk index
files, exact XTEA checksum, whole-cache group/file totals and regenerated canonical archive-index
manifest checksum, and only then promotes the pair. The previous cache and XTEAs are moved to a
temporary sibling backup during promotion and restored on a caught promotion failure. A durable
promotion-state journal records which accepted paths existed before either move, and a commit
marker is created only after both verified replacements are in place. Every server startup and
new install attempt recovers one incomplete journal idempotently before inspecting the cache;
multiple journals stop for manual resolution rather than guessing. Unit tests
prove an incomplete stage leaves both accepted paths byte-intact and a complete validated stage
replaces both, an interrupted uncommitted promotion restores the accepted pair, and a committed
promotion keeps the new pair while discarding its backup. This is journaled crash recovery around
two filesystem paths, not a claim that the filesystem supplies a native two-path transaction.

## NPC/player eight-slot interaction-engine evidence

The engine's `NpcMode` namespace already reserves operation modes 7–14 and approach modes 15–22
for eight player-target slots. Before this checkpoint, the mode processor stopped at slot five and
called `TODO()` for slots six through eight; the network player-operation handler also rejected
wire operations six through eight. The interaction enum, player and NPC exact/default event
families, script registration APIs, direct NPC helpers, mode processor and hunt redispatch now
carry the same slot number end to end for all eight player-target operations and approaches.

The extension is intentionally target-specific. Location, NPC and ground-item operation families
remain five-slot contracts and reject player-only slots six through eight with an explicit error;
no cache or gameplay meaning is invented for them. Dedicated unit tests pin player event resolution
for slots six through eight, and injected game tests execute all six corresponding NPC modes through
the ordinary game cycle and verify the registered scripts receive the matching slot.

The same audit exposed two independent ground-item approach defects. `Npc.apObj5` selected the
location mode `ApLoc5` instead of `ApObj5`, and both player and NPC default approach-event routers
returned `Ap1` for operations two through five. These now preserve `ApObj5` and map every approach
slot to its matching default event. Focused tests pin all five default player and NPC mappings and
the fifth NPC ground-item mode. This closes the interaction substrate only; it does not claim that
the production NPC, item, combat, diary or minigame inventories are complete.

## NPC death/drop engine and Undead chicken evidence

The final pre-cutoff [Undead chicken revision
14959424](https://oldschool.runescape.wiki/w/Undead_chicken?oldid=14959424), dated 5 August 2025,
defines a closed table with two guaranteed drops and one 128-weight main roll: one bones, one raw
chicken (undead), five feathers at 64/128, fifteen feathers at 32/128 and nothing at 32/128. It has
no tertiary section. Cache 233.4 independently resolves NPC 2993 `ahoy_undead_chicken` as `Undead
chicken`, object 526 as `bones`, stackable object 314 as `feather`, and members object 4289 as
`raw_chicken_undead` with display name `Raw chicken`. Their raw-config fingerprints are respectively
`6bb292a2382f42e8efebc38214c9d2b6d3e4a796107ce399ab22a890350cf5d6`,
`4a06f198f5b8d512bf7aa1e249543017ff1d45be1bf4b1cf842907c15f333c00`,
`4eb89adc50c2ec1d82ef90acaea39b17d7de3530e9414422d78098d05cefdd88` and
`725dce13289571c99be88615dd8f0901eb782f8f0b4da870705fc3484b7f4b17`.

The reusable engine follows the Wiki's general [drop-table
structure](https://oldschool.runescape.wiki/w/Drop_table): guaranteed outputs, exactly selected
weighted entries including explicit nothing, independent tertiary rolls, ordered pre-rolls that
may suppress the main table, conditions, repeated rolls and nested shared tables are separate
composable operations. A typed registry rejects duplicate NPC ownership atomically. The death path
resolves the credited hero, rolls only a registered table, emits ordered player feedback from the
same resolution and sends every resolved item through the ordinary ground-object repository with
its receiver, coordinates and optional lifetime policy. An unregistered NPC emits no invented
fallback loot. `NpcClueDropRolls` composes this engine with the shared `ClueInventory` ownership,
X Marks the Spot and scroll-case policy instead of encoding those rules per NPC.

Six unit scenarios pin composition, empty pre-roll suppression, weighted boundaries, public
placement/lifetime overrides, invalid definitions and atomic duplicate rejection. Two injected game
tests pin the registered and unregistered repository paths. The Undead chicken integration test pins
the cache identities, all three exact weight boundaries, guaranteed outputs, stackability and
private owner visibility through the actual death service. This establishes the production
substrate; shared rare/gem/herb tables, remaining clue and key conditions, Ironman eligibility,
multi-attacker credit and the remaining NPC inventory are not inferred from it.

## Duck and Duckling no-drop family evidence

The final pre-cutoff [Duck revision
14945997](https://oldschool.runescape.wiki/w/Duck?oldid=14945997), dated 24 July 2025, defines male
and female attackable versions but contains no Drops section or drop-table link. It identifies them
as NPCs 1838 and 1839. Cache 233.4 independently resolves those IDs to symbols `duck` and
`duck_female`, both named `Duck`, both with operation two `Attack`, and raw-config fingerprints
`52656eede958fbb5146297565769f6af0db98936234d0ea62f4d86d40737bca7` and
`b91c71cc65553e14dbe3a194e31b9abb83dd4f0f414b0b957ebd4b06ba6417c9`.

The final pre-cutoff [Duckling revision
14780929](https://oldschool.runescape.wiki/w/Duckling?oldid=14780929), dated 20 October 2024,
likewise contains no Drops section or conditional drop-table link and identifies the single version
as NPC 2001. Cache 233.4 resolves it to symbol `duckling`, name `Duckling`, operation two `Attack`
and raw-config fingerprint
`c492c89a0e2c8652d40be4f916d728d800e4f9031256ac26b4a085b3e024ab08`. The enriched runtime
inventory places this exact identity beside Lumbridge, so it belongs to the immediate loaded-world
drop boundary.

Production registers all three typed identities atomically to an explicit selected-nothing table. This
is intentionally different from merely leaving them unregistered: the registry now records that
the family was audited and is supposed to emit no ground objects. The injected integration test
pins all cache IDs, names and Attack operations, exact family membership, the empty direct roll, and
the actual death service producing no repository object for every identity. Duckling following and
flee behavior belong to NPC AI rather than the drop contract and remain outside this slice.

## Chicken production drop-family evidence

The final pre-cutoff [Chicken revision
14984574](https://oldschool.runescape.wiki/w/Chicken?oldid=14984574), dated 12 September 2025,
identifies twelve production versions: ordinary 1173/1174/10495/10496/9488/10494/10497,
Miscellania 3661/3662 and Falador Farm 2804/2805/2806. Every version shares two guaranteed drops,
one bones and one raw chicken, followed by exactly one 128-weight ordinary roll: five feathers at
64/128, fifteen feathers at 32/128 or nothing at 32/128. It also pins an independent 1/300 beginner
clue and a key that is always awarded only when the kill completes the applicable medium clue step.

The dated [medium cryptic-clue
record](https://oldschool.runescape.wiki/w/In_a_town_where_everyone_has_perfect_vision...?oldid=14773601)
pins that step to `trail_clue_medium_riddle004` object 2837 and its key to
`trail_clue_medium_riddle004_key` object 2838. The historical [Treasure Trails key
record](https://oldschool.runescape.wiki/w/Key_(Treasure_Trails)?oldid=14702760) confirms the key is
obtained only while carrying its clue and disappears when used. Production therefore gates this
key on membership plus the exact carried clue object; another medium clue cannot trigger it.

Cache 233.4 independently pins all twelve registered NPCs as `Chicken` with operation two
`Attack`, level-one Attack/Strength/Defence/Ranged/Magic, three Hitpoints and a 100-tick respawn.
It also pins bones 526, raw chicken 2138, stackable feather 314, clue 2837, key 2838 and the generic
non-stackable beginner clue 23182 with `Read` and `Check steps` operations. Cache NPCs 10498/10499
also decode to Chicken-shaped definitions, but they are absent from the final dated production ID
list and from the enriched runtime spawn inventory. They remain deliberately unregistered until a
production role is evidenced rather than being silently treated as ordinary chickens.

The beginner tertiary uses the shared clue inventory policy. Before X Marks the Spot, a same-tier
inventory or bank clue suppresses the roll and a successful reward is a private clue scroll with
the sourced one-hour ground lifetime. After completion, the successful reward is the cache-233
beginner scroll box with the normal three-minute lifetime; existing same-tier material and the
two-to-five scroll-case capacity are enforced, and a successful capped roll emits the exact
beginner sneaking-suspicion message without creating an item. The key and clue are independent of
the weighted feather branch, and all ordinary results remain private to the credited hero.

Five injected production scenarios pin exact family membership and the deliberate 10498/10499
boundary, every weighted-table edge, exact-clue and members key gates, pre/post-quest ownership and
capacity behavior, skipped random consumption, ordered cap feedback, repository spawning and the
6,000-tick clue lifetime. Manual case 114 retains statistical sampling and matching-client packet,
visibility and presentation evidence.

## Cow and Cow calf production-family evidence

The final pre-cutoff [Cow revision
14984471](https://oldschool.runescape.wiki/w/Cow?oldid=14984471), dated 11 September 2025,
identifies five production adults: ordinary versions 2790/2791/2793, Isle of Souls 2795 and Zanaris
5842. The final pre-cutoff [Cow calf revision
14984481](https://oldschool.runescape.wiki/w/Cow_calf?oldid=14984481), also dated 11 September
2025, identifies calves 2792/2794/2801. Both records pin combat level two, one Attack/Strength/
Defence/Ranged/Magic, eight adult or six calf Hitpoints, crush attacks with a four-tick speed, one
maximum hit, poison and venom immunity and a 45-tick (27-second) respawn.

Both dated drop tables guarantee one cowhide, one raw beef and one bones in that order, followed by
an independent 1/128 beginner clue. The adult history specifically records the 22 November 2018
change that moved cowhide to the top of the ground pile. Cache 233.4 independently resolves the
adult symbols `cow`, `cow2`, `cow3`, `cow_beef` and `fairy_cow`, calf symbols `cow2_calf`,
`cow3_calf` and `calf`, and object IDs cowhide 1739, raw beef 2132, bones 526 and beginner clue
23182. Its NPC records confirm every identity's Attack operation and combat stats, but retain the
generic 100-cycle respawn; the content editor therefore applies the sourced 45 ticks as exactly
45 server cycles (27 seconds) rather than treating the cache default as production truth. The Wiki
monster-infobox `respawn` field is tick-based; the earlier 75-cycle interpretation was corrected
before continuing the NPC inventory.

The dated articles also pin direct item responses. An empty bucket on an ordinary adult says
`Only dairy cows are suitable for milking.`; any other item says `The cow doesn't want that.` An
empty bucket on a calf says `Calves are too young to be milked.`, a bucket of milk says `Calves
aren't really used to drinking milk from buckets!`, and every other item says `The calf doesn't
want that.` The final pre-cutoff [Cow transcript revision
14328539](https://oldschool.runescape.wiki/w/Transcript:Cow?oldid=14328539), dated 27 September
2022, pins the Zanaris cow's two-part Talk-to monologue and the complete Nuff's-certificate exchange,
including the source's space before the final question mark in `Rorschach ink-blots ?`. Cache 233.4
fixes Nuff's certificate as object 9025 and gives only `fairy_cow` the Talk-to operation.

Production registers all eight identities atomically to one table that emits cowhide, raw beef and
bones before the shared clue policy. Before X Marks the Spot, same-tier inventory/bank ownership
skips the 1/128 roll; afterward, a successful roll uses the cache-233 beginner scroll box,
two-to-five case capacity and exact capped warning. Legacy clues retain the sourced one-hour ground
lifetime and boxes the normal three-minute lifetime. The existing Cow/Cow-calf content groups now
include all dated identities, so the direct item scripts and ambient behavior cover the complete
family; the Zanaris-only handlers remain identity-specific.

Six injected scenarios pin the eight IDs, names, Attack operations, content groups, 45-cycle
respawns and shared table; guaranteed order, clue success/miss, repository death path and one-hour
lifetime; ownership, X Marks and cap feedback; every direct bucket/fallback branch; and both exact
Zanaris transcripts through production event routing. Manual case 115 retains statistical,
matching-client, combat-immunity, ground-pile, audiovisual and world-respawn evidence. Five of the
eight identities occur in the enriched loaded-world attackable inventory, reducing its open drop
backlog from 44 to 39 distinct identities.

## Level-five Frog production evidence

The final pre-cutoff [Frog revision
14959339](https://oldschool.runescape.wiki/w/Frog?oldid=14959339), dated 5 August 2025, identifies
the production level-five Frog as NPC 8702. It pins Attack 5, Strength 4, Defence 3, Hitpoints 8,
Ranged 1 and Magic 1; a one-hit maximum crush attack at four-tick speed; a 50-tick (30-second)
respawn; and a 40% Earth elemental weakness. Its complete drop table is one guaranteed bones plus
an independent 1/90 beginner clue, with no membership, quest, area or Wilderness qualifier.

Cache 233.4 independently resolves NPC 8702 as symbol `little_frog_nodrops`, name `Frog`, operation
two `Attack` and exact combat-stat tuple 5/4/3/8/1/1 at fingerprint
`sha256:fae4a5dd21a8eb737cc90158cf606b93f359cd31929e56382545ab12d2157810`. The symbol's historical
`nodrops` suffix is not treated as behavioral evidence because the dated production source has a
drop table. The cache retains its generic 100-cycle respawn and no elemental-weakness parameters,
so a typed NPC editor applies the sourced 50-cycle respawn and Earth/40 parameter pair. Cache
objects independently pin bones 526, beginner clue 23182 and beginner scroll box 24361.

The production table emits bones before the independent clue roll. It reuses the same tested clue
policy as Chicken and Cow: pre-X-Marks same-tier inventory/bank ownership skips the roll; post-X-
Marks successes become scroll boxes subject to two-to-five capacity and the exact capped warning;
legacy clues use the sourced one-hour private-ground lifetime and boxes three minutes. Three
injected scenarios pin the NPC identity and stats, editor overrides, sole registry membership,
1/90 miss/success boundaries, drop order, credited death/repository path, lifetime, ownership skip,
scroll-box replacement and capacity warning. Manual case 116 retains statistical, combat,
matching-client and world-population observations. This closes one loaded attackable identity and
reduces the immediate open backlog from 39 to 38.

## Cache-233 Easy clue initial-step evidence

The final pre-cutoff [Easy clue revision
14986803](https://oldschool.runescape.wiki/w/Clue_scroll_%28easy%29?oldid=14986803), dated 15
September 2025, establishes the four Easy step forms used at the target boundary: cryptic clues,
emote clues, maps and music clues. It also records that a step may repeat within one clue, so the
initial selector does not remove an identity after selection.

Workspace RuneLite commit `d14c12197c80f76a2833c7f744cacec896096d76`, release 1.11.18 dated
24 September 2025, supplies the concrete cutoff decoder inventory. Its `CrypticClue`, `EmoteClue`,
`MapClue` and `MusicClue` classes contain 120, 32, seven and six active Easy object IDs
respectively: 165 unique initial steps in total. Cache 233.4 independently returns exactly the same
165 IDs when selecting internal symbols beginning `trail_clue_easy_` or `trail_easy_` and excluding
`_casket`; every resolved object is named `Clue scroll (easy)` and exposes `Read` then `Check steps`.
The first ID is 2677 and the last is the cutoff addition 30928.

`Cache233EasyClueSteps` performs that cache-derived inventory and refuses startup/test parity if
any identity is added, removed or reordered. `NpcClueDropRolls.independentEasy` uniformly selects
the initial object after the chance and ownership gates. `ClueInventory` accepts a lazy scroll
supplier so a post-X-Marks success becomes scroll box 24362 without consuming the initial-step RNG.
Three injected scenarios pin the exact category counts, all 165 IDs, cache operations, both pool
endpoints, invalid indexes and the no-extra-roll box path. This closes initial Easy reward identity,
not step interactions, completion, caskets or reward tables.

## Standard NPC herb-table and loaded Mugger evidence

The final pre-cutoff [HerbDropLines template revision
14831791](https://oldschool.runescape.wiki/w/Template:HerbDropLines?oldid=14831791), dated 5 January
2025, defines the standard nested herb table as exactly one 128-weight selection: grimy guam leaf
32, grimy marrentill 24, grimy tarromin 18, grimy harralander 14, grimy ranarr weed 11, grimy irit
leaf 8, grimy avantoe 6, grimy kwuarm 5, grimy cadantine 4, grimy lantadyme 3 and grimy dwarf weed
3. Cache 233.4 independently resolves those objects, in the same order, as IDs 199, 201, 203, 205,
207, 209, 211, 213, 215, 2485 and 217.

`NpcHerbDropRolls.standard()` exposes that source as a reusable nested `NpcDropRoll`, retaining
typed cache references instead of numeric production objects. A pure boundary test proves every
lower and upper interval and the 128 total; an injected cache test repeats all 22 boundaries
against the resolved production item IDs. This closes only the shared selection table, not any
NPC's chance to enter it.

The final pre-cutoff [Mugger revision
14995952](https://oldschool.runescape.wiki/w/Mugger?oldid=14995952), dated 28 September 2025, was
used for the first consumer. It distinguishes regular NPCs 513/1461/6996 and Varlamore NPC
13282, and further limits Port Sarim jail and plain Piscarilius/Vinery variants to bones. The full
table guarantees bones, selects one 128-weight main entry, enters this herb table at 13/128, and
has independent beginner 1/80 and members-only Easy 1/128 clues. The exact main weights are bronze
bolts 2–12 at 27 (members-only and selected-empty in free worlds), mind runes 9 at 3, water runes 6
at 2, earth runes 5 at 2, herbs at 13 (replaced by ten coins in free worlds), coins 5/15/25 at
12/3/1, rope at 40, nothing at 13, knife at 1, fishing bait at 6, copper ore at 2, bronze med helm
at 2 and cabbage at 1, totaling 128.

Cache 233.4 independently pins loaded NPC 513 as `mugger`, named `Mugger`, with `Attack` in
operation two and combat stats 5/5/5/8/1/1 at fingerprint
`sha256:d8276624d2ace5c571167ae7f2f05582cf4315558cf2087a3353364da8dc8933`. Its enriched placement
at 3181,3216 is the Lumbridge Castle full-table variant. The production plugin registers only that
typed identity. Four injected scenarios pin the cache and sole-registry contract, all fifteen main
table boundary entries, variable bolt quantity, nested herb endpoint, free-world substitutions,
both independent clue rates/order, the Easy pool's final endpoint and ground lifetimes. Manual case
117 retains live statistical, presentation, combat and placement checks. NPCs 1461, 6996 and 13282
remain open rather than applying the loaded identity's full table across jail/plain variants.

## Ram family and Rag and Bone Man I skull evidence

The final pre-cutoff [Ram revision
14959417](https://oldschool.runescape.wiki/w/Ram?oldid=14959417), dated 5 August 2025, identifies
all five production Rams, NPCs 1261–1265. They are free-to-play combat-level-two NPCs with
Attack/Strength/Defence/Hitpoints/Ranged/Magic values 1/1/1/8/1/1, maximum crush hit one,
four-tick attacks, 45-tick respawns, ordinary poison and venom susceptibility and a 20% Fire
weakness. Their ordinary drop is always bones. A ram skull is an additional guaranteed members
reward only during Rag and Bone Man I. Using shears has the exact response `That one looks a
little too violent to shear...` rather than transforming the animal or consuming the shears.

The final pre-cutoff [Ram skull revision
14550342](https://oldschool.runescape.wiki/w/Ram_skull?oldid=14550342), dated 25 February 2024,
pins the three lifecycle objects: raw ram skull 7818, bone in vinegar 7819 and polished ram skull
7820. The final pre-cutoff [Rag and Bone Man I revision
14991439](https://oldschool.runescape.wiki/w/Rag_and_Bone_Man_I?oldid=14991439), dated 25
September 2025, establishes that each of the eight bones may be handed in separately, cannot be
obtained again after hand-in, and must be recollected if removed from vinegar before hand-in.
Cutoff Quest Helper commit `241eaec29b19243bda7e88e99d5c16568c0776a6` independently maps the
quest to varp 714 (`rag_quest`), collection/preparation state two, and the ram skull to hand-in
index three of eight. Cache database quest row 114 uses completion value four.

Cache 233.4 independently resolves Rams 1261–1265 as `ramsheered`, `ramunsheered`,
`ramunsheered2`, `ramunsheered3` and `ramunsheeredshaggy`. Their exact raw fingerprints are
respectively `sha256:a606b2be585a3b7c3896a5b4b9d2182980a3929d2a1733ad8a2ac3b9b488c8b5`,
`sha256:4563227aba037100bd312dbc0974e6a68fe007f5cb5df26a91a76e843c99f3e0`,
`sha256:4e8236fb9cc0b2e8e516af68fd004cd222303b29c61229a37299fbc0b4c429ce`,
`sha256:434d7a7b2587990b9e8a64e68a5c573d62d5ef330441ad050eda81f42f4165ae` and
`sha256:f4f2db3f7da744b704990a5261c13296ef498dbc996bf2c9d5fe8bacf7cdaf2a`.
The cache preserves the exact six-stat tuple on every identity; the editor supplies the sourced
45-tick respawn and 20% Fire weakness absent from the vanilla definitions.

`RagAndBoneManIDropPolicy` checks members, exact collection state, every raw/vinegar/polished
inventory and bank identity, and a persistent server-only eight-bit hand-in mask. This avoids the
incorrect approximation of granting a replacement after an already-delivered bone leaves storage.
`RamDrops` shares that policy across all five identities while `Ram` owns the shears response.
Four injected scenarios pin cache/editor/registry coverage, quest-state and membership boundaries,
all lifecycle storage locations, persistent hand-in suppression and non-consuming shears behavior.
Manual case 118 retains live combat, timing, audiovisual, persistence and quest-flow observation.
The broader quest dialogue, vinegar processing and completion plugin remains open.

The final pre-cutoff [Big frog revision
14990022](https://oldschool.runescape.wiki/w/Big_frog?oldid=14990022), dated 22 September 2025,
distinguishes loaded free-to-play level-ten Big frog 8701 from members level-24 Big frog 478. NPC
8701 has Attack/Strength/Defence/Hitpoints/Ranged/Magic values 8/6/4/18/1/1, maximum crush hit two,
four-tick attacks, non-aggression, ordinary poison and venom susceptibility, 50-tick respawn and a
25% Earth weakness. It always drops bones, independently rolls a beginner clue at 1/70, and after
the 27 August 2025 update always drops a Big frog leg only during Rag and Bone Man I.

Cache 233.4 independently pins 8701 as `medium_frog_nodrops`, named `Big frog`, with `Attack` in
operation two, the exact six-stat tuple and raw fingerprint
`sha256:dcb2322ce672336e244e9f9e2e775269b04cfa47554902d9bac5f98480e3bfde`. It resolves the raw,
vinegar and polished quest objects as 7908/7909/7910 at fingerprints
`sha256:44e7e7cb8e2e61eb9d6f68d5d22c68ba0efc99c608c180357ebf60460d3e4063`,
`sha256:e0d277c6ac6113fcfbb987390fff927d6380f642eb7776d918d092b9d3f6bd2f` and
`sha256:51df6cb8f3498f019c0fd75eeed998525515765564f7bdd795eeee7953dd02c4`.
Cutoff Quest Helper commit `241eaec29b19243bda7e88e99d5c16568c0776a6` places `BIG_FROG_LEG`
sixth in the ordered eight-bone collection, yielding zero-based hand-in bit five.

`BigFrogDrops` registers only loaded level-ten identity 8701: guaranteed bones, the shared exact
quest-lifecycle roll and an independent shared-policy 1/70 beginner clue. `BigFrogNpcEdits`
supplies the sourced 50-tick respawn and 25% Earth weakness absent from the vanilla definition.
Three injected scenarios pin the cache/editor/registry contract, quest/clue independence and order,
all raw/vinegar/polished inventory and bank paths, persistent bit five and clue lifetime. Manual
case 119 retains combat, presentation, probability, persistence and matching-client observation.
Members level-24 identity 478 is not loaded and retains its separate weighted table backlog.

The final pre-cutoff [Giant frog revision
14961999](https://oldschool.runescape.wiki/w/Giant_frog?oldid=14961999), dated 9 August 2025,
distinguishes loaded free-to-play level-thirteen Giant frog 8700 from members level-99 identity 477.
NPC 8700 has Attack/Strength/Defence/Hitpoints/Ranged/Magic values 10/8/6/23/1/1, maximum crush hit
two, four-tick attacks, non-aggression, ordinary poison and venom susceptibility, 50-tick respawn
and a 25% Earth weakness. It always drops big bones and independently rolls a beginner clue at
1/64. Long and curved bones are members-only tertiaries.

The last pre-cutoff [Long bone revision
14976983](https://oldschool.runescape.wiki/w/Long_bone?oldid=14976983), dated 30 August 2025, and
[Curved bone revision
14967442](https://oldschool.runescape.wiki/w/Curved_bone?oldid=14967442), dated 16 August 2025,
preserve the exact shared roll from Mod Ash: roll the long bone at 1/400 first, then roll the curved
bone at 1/5,000 only if no long bone was selected. The effective curved-bone rate is therefore
399/400 times 1/5,000, or 1/5,012.5; the two rewards cannot coexist on one kill.

The immutable cache-233.4 core manifest pins 8700 as `giant_frog_nodrops`, named `Giant frog`, with
`Attack` in operation two, size two, the exact six-stat tuple and raw fingerprint
`sha256:2036fd2034d5cf8bfdda35a3cbf7f8d5ff7df4c3e279006273fd75230b23d0e2`. It resolves big bones
532, long bone 10976 and curved bone 10977; the latter two have raw fingerprints
`sha256:deabed730813e8e084598ad30b36d6eee8333478861fcbf8fa4e11341bbd5040` and
`sha256:e5cae445b91d9fd5cb328892308fa6dde74f2025ddecf763fcf9218ee8630190`.

`NpcRareBoneDropRolls.longOrCurved()` now exposes the ordered members-only policy for every future
eligible table. `GiantFrogDrops` registers only loaded identity 8700 with guaranteed big bones,
independent 1/64 beginner clue and the shared rare-bone roll; `GiantFrogNpcEdits` supplies the
sourced respawn and weakness absent from the vanilla definition. A pure boundary test proves the
membership gate, long hit, curved-after-long-miss hit and total miss. Three injected scenarios pin
the cache/editor/registry contract, free-world behavior, both rare-bone boundaries, mutual
exclusion, clue independence/order and clue lifetime. Manual case 120 retains live statistical,
combat, presentation and matching-client observations. Members level-99 identity 477 is not loaded
and retains its separate weighted table backlog.

## Loaded Spider-family evidence

The final pre-cutoff [Spider revision
14982545](https://oldschool.runescape.wiki/w/Spider?oldid=14982545), dated 8 September 2025,
separates common Spider 3019 from Underground Pass 4561 and Stronghold 2478. Common Spider is
free-to-play combat level one, size one, non-aggressive and has Attack/Strength/Defence/Hitpoints/
Ranged/Magic values 1/1/1/2/1/1, zero maximum Stab hit, four-tick attacks, ordinary poison and
venom susceptibility, a 35-tick respawn and 50% Fire weakness. It has no ordinary table and an
independent beginner clue at 1/128. Its 1/15 looting bag and Wilderness Slayer tertiary apply only
in the Wilderness.

The final pre-cutoff [Giant spider revision
14983292](https://oldschool.runescape.wiki/w/Giant_spider?oldid=14983292), dated 10 September 2025,
separates level-two 3017 from level-27 3018 and Stronghold level-50 2477. The loaded level-two
identity is free-to-play, size one and aggressive, with Attack/Strength/Defence/Hitpoints/Ranged/
Magic values 1/1/1/5/1/1, maximum Stab hit one, four-tick attacks, ordinary poison and venom
susceptibility, a 30-tick respawn and 50% Fire weakness. It likewise has no ordinary table and an
independent beginner clue at 1/128. Its Wilderness looting bag is 1/15 normally or 1/5 under the
documented alternative condition, and its Wilderness Slayer tertiary is location-specific; none
belongs to a non-Wilderness kill.

The enriched cache-233.4 spawn inventory contains seven 3019 placements between Lumbridge and
Draynor and sixteen 3017 placements west, north and east of Lumbridge. Every placement is on level
zero at z-coordinate 3222 through 3256, well outside the Wilderness. The immutable core manifest
pins 3019 as `spider` at fingerprint
`sha256:2c1a7eb9fac1903779cba7718d8538ff5dd5eb87e607b31544655a23d8081414` and exact stats
1/1/1/2/1/1; it pins 3017 as `giantspider1` at fingerprint
`sha256:351e2db2dfd5294f7367f1cdfbd3e287b0497a4e4d2f6feca410e4522ec8f9d9` and exact stats
1/1/1/5/1/1. Both expose `Attack` in operation two, size one, four-tick attack parameter 50 and
cache hunt range five. The vanilla records retain generic 100-tick respawns and omit elemental-
weakness parameters.

`SpiderNpcEdits` supplies the sourced 35-/30-tick respawns and Fire/50 parameters, assigning only
the aggressive Giant spider to the existing standard melee hunt mode. `SpiderDrops` atomically
registers both loaded identities to the independent shared 1/128 beginner-clue roll and adds no
ordinary reward. Three injected scenarios pin exact cache/editor/registry contracts, miss/success
boundaries, repository-backed private spawn and the shared ownership, X Marks scroll-box,
capacity, warning and ground-lifetime policy. Manual case 125 retains live aggression, combat,
respawn, probability, presentation and matching-client evidence. Unloaded higher-level and
Wilderness identities remain separate contracts rather than inheriting this location-bounded
table.

## Loaded Goblin-family evidence

The final pre-cutoff [Goblin revision
14956290](https://oldschool.runescape.wiki/w/Goblin?oldid=14956290), dated 1 August 2025,
separates two drop tables and combat families relevant to the loaded world. Unarmed level-two
Goblins use Attack/Strength/Defence/Hitpoints/Ranged/Magic values 1/1/1/5/1/1, maximum Crush hit
one, four-tick attacks, non-aggression and a 35-tick respawn. Armed level-five Goblins use
3/1/4/12/1/1, maximum Stab hit one, six-tick attacks, non-aggression and the same 35-tick respawn.
Neither family has an elemental weakness.

The enriched cache-233.4 spawn inventory contains 127 unarmed placements split across NPCs
3029–3036 (25/11/22/12/25/15/9/8), plus one armed NPC 3045 at 3167,3205,0. All 128 placements are
outside the Wilderness. The immutable fingerprints are
`sha256:76c055d8d89abd647665d518cb072eccd4aa95b1c69b3e81327d762791e8a735`,
`sha256:8635b9c8c26177e15e7ecf834648b9cc9859612f87a3ceda78ec50149a81f8d7`,
`sha256:fd5ebe81a61da86cbafbb6c5e66465e13e2b7b4c465cd4d2584dd460d2eded7c`,
`sha256:9233f37fd370991a93e7e1c3585efb4271fa2ec30bf8d930179beb75556daf3b`,
`sha256:fa02f396e011d3aad496a49a155389b2eeb2fa80d6cc822e76989d02415b10be`,
`sha256:be1a5102eac2f42f158fd0df814c0df1152b423fb1744ab9343513a9cd18a4c3`,
`sha256:b866caad499cf99a4d45fa4e3e0e5b8208ce0ab1fbbb0275efa9c02b39dee411`,
`sha256:e55480dfa8b1d485b375e576510389a70cfab1f5f144bc2f0acb3eac1b15e633`
and `sha256:3bd0890a7e5446d40bbc42d9288ad3170ad062f7f2fbf02ee978521bb28d0c78`
respectively. The source editor corrects the cache-enricher's generic two-tick attack rate for all
nine identities and its incorrect Crush style for armed 3045.

The level-two table is exactly 128 shares: bronze sq shield 3; members-only bronze spear 4;
seven body runes 5; six water runes 6; four earth runes 3; eight bronze bolts 3; coin stacks of
5/9/15/20/1 at 28/3/3/2/1; nothing 38; hammer 15; members-only Goblin book 2; Goblin mail 5;
chef's hat 3; beer 2; brass necklace 1; and air talisman 1. The armed table is exactly 128 shares:
bronze axe 3; bronze scimitar 1; members-only bronze spear 9; seven bronze arrows 3; two mind
runes 3; four earth runes 3; two body runes 3; five members-only bronze javelins 2; chaos rune 1;
nature rune 1; standard herb branch 2, replaced by ten coins in free worlds; coin stacks of
1/3/5/16/24 at 34/13/8/7/3; nothing 8; hammer 9; members-only Goblin book 2; Green goblin mail
10; grapes 1; red cape 1; and tin ore 1. The 3045 cache model and dated table jointly distinguish
Green goblin mail 9057 from ordinary Goblin mail 288.

Both tables always drop bones and independently apply their dated tertiary contracts. While Rag
and Bone Man I is in collection state two, a member who still needs the fifth canonical bone rolls
raw Goblin skull 7812 at 1/4; inventory or bank ownership of raw, vinegar 7813 or polished 7814,
and persistent hand-in bit four suppress duplicates. Cutoff Quest Helper commit
`241eaec29b19243bda7e88e99d5c16568c0776a6` independently pins that fifth-position mapping.
Members roll initial ensouled goblin head 13447 at 1/35 from the level-two table or 1/30 from the
armed table. Beginner clues roll at 1/64 and 1/80 respectively; Easy clues roll at 1/128; and the
members-only Goblin champion scroll 6801 uses the shared 32-quest-point, 1/5,000 repeat-before-
victory and exact post-victory suppression policy through cache varbit 1455.

`GoblinConfig` and `GoblinDrops` register only these nine loaded identities. Seven injected
scenarios pin all cache/editor/registry contracts, every boundary in both 128-share tables, free-
world selected-empty and herb-replacement behavior, tertiary order and rates, fresh-head
provenance, quest-bone lifecycle and champion completion policy. Manual case 126 retains live
combat, distribution, ground-presentation, quest persistence and matching-client evidence. Other
Goblin identities, aggressive Goblin Cave variants, Wilderness Ring-of-wealth clue behavior and
post-cutoff Combat Achievement rate changes remain separate contracts.

## Loaded Man and Woman evidence

The final pre-cutoff [Man revision
14941419](https://oldschool.runescape.wiki/w/Man?oldid=14941419), dated 21 July 2025, and [Woman
revision 14984028](https://oldschool.runescape.wiki/w/Woman?oldid=14984028), dated 10 September
2025, establish the shared ordinary civilian contract. Both are combat level two, size one,
non-aggressive, use Attack/Strength/Defence/Hitpoints/Ranged/Magic values 1/1/1/7/1/1, have a
maximum Crush hit of one, attack every four ticks, respawn after 25 ticks and have no elemental
weakness.

The enriched cache-233.4 spawn inventory contains sixteen relevant placements: Man identities
3106/3107/3108/6818 occur 5/1/4/1 times and Woman identities 3111/3112/3113 occur 3/1/1 times.
They span level-zero and first-floor Lumbridge-region tiles from 3206,3219 through 3244,3211 and
none is in the Wilderness. The immutable raw NPC fingerprints for 3106, 3107, 3108, 3111, 3112
and 3113 are respectively
`sha256:0a2f2647b185da88bd804dcabfe4e5a9366021cfd5e31b63a49f101ac677bdd6`,
`sha256:bf847ebadb5f80c7b5ea35a9d07313b096347e4f888e8200088c2f828c2afd03`,
`sha256:0fcb20ef9394212f7fbdc6135fc6a874c22df713c4d91fd840fe5a54765a5c51`,
`sha256:0d07025a4b3a8f7b3a0857de9963d578b4de7e02fce9953002db5a1be3567d40`,
`sha256:673d0ed189bb7db7ebaaa1ecb722063b6ffd75e92ed1e4f982212f749ac5a880`
and `sha256:2dbe4f1faa7ad20b3910dd69248143767de65dc3d444c4816fa64626006f5302`;
6818 has the same raw contract and fingerprint as 3106.

Their guaranteed bones precede one exact 128-share selection: bronze med helm 2; iron dagger 1;
members-only bronze bolts in quantities 2–12 at weight 22; seven bronze arrows 3; four earth
runes 2; six fire runes 2; nine mind runes 2; two chaos runes 1; standard herb branch 23 in
members worlds or ten coins in free worlds; three/five/fifteen/twenty-five coins at weights
38/9/4/1; nothing 8; fishing bait 5; copper ore 2; earth talisman 2; and cabbage 1. The bronze-
bolt branch is a selected empty result in free worlds. Beginner clues roll independently at 1/90
in either world type and Easy clues at 1/128 for members. When a member carries exact medium clue
object 2837, a Man additionally drops its exact key 2838; Women do not.

`GenericPersonConfig` supplies the dated respawn, cadence and Crush style, while `ManWomanDrops`
atomically registers the four Man and three Woman identities with separate roots only for the
Man-specific key condition. Five injected scenarios pin all identities and combat settings, every
main-table boundary, free-world substitutions, exact key condition and clue order, membership,
ownership, capacity and lifetime policy. Manual case 127 retains live combat, distribution,
pickpocket coexistence, presentation and matching-client evidence. Other civilian identities and
Wilderness/location-specific tables remain separate contracts.

## Loaded Farmer and allotment-seed evidence

The final pre-cutoff [Farmer revision
14941540](https://oldschool.runescape.wiki/w/Farmer?oldid=14941540), dated 21 July 2025, defines
the ordinary Farmer as combat level seven, size one and non-aggressive, with
Attack/Strength/Defence/Hitpoints/Ranged/Magic values 3/4/8/12/1/1, a maximum Stab hit of one,
six-tick attacks, +5 Stab accuracy, +6 melee Strength, a 25-tick respawn and no elemental weakness.
The enriched manifest loads only identities 3114, 3244, 11919, 11920 and 11921, at seven level-zero
placements: 3165,3324; 3181,3293; 3227,3290; 3262,3325; 3233,3308; 3154,3293; and 3199,3289.
All are outside the Wilderness. Their immutable raw fingerprints are respectively
`sha256:9db893347ea0cb881e4fd5f5587848a087a30e4d6dbb108681126c70b43fc2b4`,
`sha256:b0f312afb71f17c5ba04ef6d607033bdd969c6bc95244a20d264c9f3068895fa`,
`sha256:4612e3ecca4f19e6993bcf4eb702fe166abdf75c3acf50bae83a3c3a18be8cf1`,
`sha256:9e6b830c50b9558d4a2c89e07403407b0e266fd0656194eeb6ba3dd26cbfd027`
and `sha256:6e9c56d883431a707b07e457c3c2304f7311bf4d1fd2ff65c130937590348671`.

Both world types guarantee bones. The free-world 128-share table is bronze med helm 2; iron
dagger 1; seven bronze arrows 3; four earth runes 2; six fire runes 2; nine mind runes 2; two
chaos runes 1; coin stacks of 3/10/5/15/25 at 38/23/9/4/1; nothing 30; fishing bait 5; copper ore
2; earth talisman 2; and cabbage 1. The members table is four earth runes 2; six fire runes 2;
nine mind runes 2; two chaos runes 1; standard herb branch 11; allotment-seed branch 27; three
coins 38; twenty-five coins 1; nothing 28; fishing bait 5; rake 3; earth talisman 2; gardening
boots 2; seed dibber 2; secateurs 1; and watering can (8) 1. Beginner clues independently roll at
1/90 in both world types and Easy clues at 1/128 for members.

The final pre-cutoff [allotment seed drop-lines revision
14831775](https://oldschool.runescape.wiki/w/Template:AllotmentSeedDropLines?oldid=14831775), dated
5 January 2025, defines the nested 128-share table: potato seeds 1–4 at 64, onion seeds 1–3 at 32,
cabbage seeds 1–3 at 16, tomato seeds 1–2 at 8, sweetcorn seeds 1–2 at 4, strawberry seed at 2,
watermelon seed at 1 and snape grass seeds 1–3 at 1. `NpcAllotmentSeedDropRolls` preserves that
table as a reusable typed primitive next to the standard herb table. `FarmerConfig` and
`FarmerDrops` register only the five loaded identities. Five injected scenarios pin every combat
and registry contract, every boundary in both main tables, every nested seed boundary and quantity
endpoint, plus clue order and membership. Manual case 128 retains live combat, distribution,
pickpocket coexistence and matching-client evidence. Unloaded Farmer identities 3243/11918 and
their future placements remain outside this bounded contract.

## Loaded Unicorn and fresh ensouled-head evidence

The final pre-cutoff [Unicorn revision
14989492](https://oldschool.runescape.wiki/w/Unicorn?oldid=14989492), dated 21 September 2025,
distinguishes ordinary Unicorn 2837 from later identity 14043 and pins the ordinary monster at
combat level 15, size two, Attack/Strength/Defence/Hitpoints/Ranged/Magic values
11/13/13/19/1/1, maximum stab hit two, four-tick attacks, non-aggression, ordinary poison and venom
susceptibility and a 90-tick respawn. Its ordinary drops are bones; a members-only unicorn horn;
a members-only Unicorn bone while it remains outstanding in Rag and Bone Man I; and a members-only
ensouled unicorn head at 1/35. The page documents retreat at low Hitpoints but does not publish an
exact threshold, and it does not list an elemental weakness.

The immutable cache-233.4 core manifest independently pins loaded NPC 2837 as symbol `unicorn`,
name `Unicorn`, operation two `Attack`, size two, the exact six-stat tuple and raw fingerprint
`sha256:9aef4e315f695a23cb4eda07d6bfb202d22ca00170dfe53f9602b4db355e4476`. The enriched spawn
manifest places it west of Lumbridge at 3140,3209. Cache objects resolve bones 526, unicorn horn
237, raw/vinegar/polished quest forms 7821/7822/7823 and initial/processed ensouled heads
13465/13466. Their relevant raw fingerprints are respectively
`sha256:9574830eef865ef58c26f994fda111c3cfd2a4a6e30cb169bd058178ec97d8bc`,
`sha256:db88b19812bc8a58c91b742cadee3edd33c45d518d97cf8cb0e9dd52f15b5086`,
`sha256:3df08554e62c99d6ea1b52da8b701a16161621ccfcac10ed5319c05cba3106a7`,
`sha256:5d430e08f8a88e6b10b364e5dbc96f95ed83951e4ad4d6be162573154355ee86`,
`sha256:fe0dfa3db41f221b4dfe69ce3c060c00a9c26500b10ffba1db53312af1e04dc8` and
`sha256:cf7de5e8c35adf44010cc35558d7285c7f71e526395ded026b184284ac42ec28`.
Cache parameter 26 is zero, so it does not supply the missing retreat threshold; production leaves
that qualitative behavior open rather than inventing a value.

Cutoff Quest Helper commit `241eaec29b19243bda7e88e99d5c16568c0776a6` maps Unicorn second in
the ordered eight-bone Rag and Bone Man I state list, establishing zero-based hand-in bit one.
`UnicornDrops` therefore shares the existing raw/vinegar/polished ownership and persistent hand-in
policy instead of maintaining NPC-local quest state.

The final pre-cutoff [Ensouled head revision
14978131](https://oldschool.runescape.wiki/w/Ensouled_head?oldid=14978131), dated 2 September 2025,
and [Ensouled unicorn head revision
14420589](https://oldschool.runescape.wiki/w/Ensouled_unicorn_head?oldid=14420589), dated 16 June
2023, establish that a freshly dropped head may be reanimated indefinitely within the 31-by-31
square centered on its original drop tile, including after pickup and re-drop. Banking, trading,
Grand Exchange use and price checking expire that origin and convert the item to its processed
identity; Soul Bearer banking therefore also expires it. Basic Reanimation requires Magic 16,
four body and two nature runes, awards 32 Magic XP and yields 494 Prayer XP from the Unicorn.

The shared ground-object path now preserves private per-item server variables through NPC spawn,
stack/split, pickup and player re-drop. `FreshDropOrigin` records the immutable origin, and
reanimation accepts an initial head only within Chebyshev distance 15 on the same level. The bank
transaction converts fresh initial heads to their adjacent processed cache identity and restores
the original identity and origin if the transaction fails or deposits only part of the stack.
Trade, Grand Exchange and price-checker systems are not present in this repository, so their
expiration hooks remain explicit future integration rather than simulated bank behavior. Unit and
injected tests cover coordinate packing, stack separation, NPC-roll provenance, pickup, carried
and ground range boundaries, processed-head rejection away from the Dark Altar, successful single
and bulk banking, and failed-deposit restoration. Manual cases 95 and 121 retain matching-client,
retreat, presentation, lifetime and unavailable-system observations.

## Loaded ordinary Imp and Champion's scroll evidence

The final pre-cutoff [Imp revision
14988743](https://oldschool.runescape.wiki/w/Imp?oldid=14988743), dated 19 September 2025, pins
ordinary identity 5007 separately from God Wars identity 3134. It establishes combat level two,
size one, Attack/Strength/Defence/Hitpoints/Ranged/Magic values 1/1/1/8/1/1, zero maximum stab hit,
four-tick attacks, non-aggression, ordinary poison and venom susceptibility, a 50-tick respawn and
10% Water weakness. The same revision supplies guaranteed fiendish ashes, the exact 128-share
32-entry main table, a members-only ensouled imp head at 1/25 and an Imp champion scroll at 1/5,000.
Its members-only looting bag is Wilderness-only and its ecumenical key uses the separate God Wars
table. The enriched spawn inventory places identity 5007 only at 3217,3226; 3214,3281; and
3240,3307 on level zero, so neither location-conditioned tertiary belongs to this loaded slice.

The final pre-cutoff [Imp champion scroll revision
14985323](https://oldschool.runescape.wiki/w/Imp_champion_scroll?oldid=14985323), dated 13 September
2025, and [Champion's scroll revision
14895741](https://oldschool.runescape.wiki/w/Champion%27s_scroll?oldid=14895741), dated 6 May 2025,
require membership and at least 32 quest points. They permit duplicate scrolls until the matching
champion has been defeated. After that victory, a successful scroll roll yields no object and
instead displays `You have a funny feeling that you would have received a Champion's scroll...` in
dark red. These rules are corroborated by Mod Ash's archived [eligibility answer from 14 January
2019](https://archive.is/9KMB5), [post-victory suppression answer from 18 April
2019](https://archive.is/K97dq), and [duplicate-roll message answer from 16 December
2021](https://archive.ph/LZanp).

The immutable cache-233.4 core manifest resolves NPC 5007, fiendish ashes 25766, initial/processed
ensouled heads 13453/13454, champion scroll 6803, all 32 main-table objects, quest-points varp 101
and Imp-victory varbit 1457. `ImpDrops` composes the guaranteed, weighted and two independent
tertiary rolls. `NpcChampionScrollDropRolls` is the shared membership, quest-point and victory-state
policy for later champion-scroll monsters. Unit and injected tests cover every weighted boundary,
fresh-head provenance, membership and quest-point gates, pre-victory repeats, post-victory
suppression and exact feedback. Manual case 122 retains live distribution, teleport, presentation
and matching-client checks. The Imp page attributes its 25% teleport attempt every 30–120 seconds
to [Mod Ash's 18 October 2023 answer](https://archive.ph/TTEdI), but does not establish an exact
radius or audiovisual sequence; that behavior remains evidence-open rather than guessed.

## Loaded regular Giant rat evidence

The final pre-cutoff [Giant rat revision
14961330](https://oldschool.runescape.wiki/w/Giant_rat?oldid=14961330), dated 8 August 2025,
separates regular level-three identities 2856–2861 and 9483, regular level-six identities
2862–2864, and the level-26 Stronghold family 2510–2512. The level-three family has
Attack/Strength/Defence/Hitpoints/Ranged/Magic values 2/3/2/5/1/1; the level-six family has
6/5/2/10/1/1. Both are size two, aggressive standard-melee monsters with a maximum stab hit of
one, four-tick attacks, ordinary poison and venom susceptibility, a 30-tick respawn and no listed
elemental weakness. Their regular table guarantees bones and raw rat meat and independently rolls
a beginner clue at 1/128. A looting bag is members-only at 1/15 specifically in the Wilderness.

The final pre-cutoff [Giant rat bone revision
14550275](https://oldschool.runescape.wiki/w/Giant_rat_bone?oldid=14550275), dated 25 February
2024, establishes raw object 7824, vinegar object 7825 and polished object 7826 as the Giant-rat
component of Rag and Bone Man I, obtainable only while that bone remains outstanding. Cutoff Quest
Helper commit `241eaec29b19243bda7e88e99d5c16568c0776a6` places it first in the ordered
eight-bone collection, establishing zero-based persistent hand-in bit zero.

The enriched cache-233.4 spawn inventory loads 27 regular Giant rats, all on level zero in the
Lumbridge region: ten of 2856, two of 2859, one each of 2860 and 2861, six of 2863 and seven of
2864. None is in the Wilderness. The immutable core manifest pins those type definitions at raw
fingerprints `sha256:ff946adc57d06582ce5b7846e0f1b1a425ba56678e6e70371a02d5bc0b8972eb`,
`sha256:a3c3915e1a968ccab10222284e7574aec641b626dbcc08df4e2ecf15e1fe9f49`,
`sha256:b44ea3325e5499b50eb626357c572fb158e30a82aed1946e85caae4cc7e17663`,
`sha256:3ccb4e17f0059b7f1a66e14dbb23722e06e9a3045fab7d1ca0c86e7f0c0ba945`,
`sha256:74a306274d504a479a5d64554023f9c6844c77a1201cead6452b89ebe8a3dc22` and
`sha256:48a83530531d2fd85a6f342a8fa4e655f261e85da18912c5ba7c53c329ad60aa`.
Objects bones 526, raw rat meat 2134, quest forms 7824/7825/7826 and beginner clue 23182 have raw
fingerprints `sha256:4a06f198f5b8d512bf7aa1e249543017ff1d45be1bf4b1cf842907c15f333c00`,
`sha256:d2b546e09ba698cfcaead5823855b1468a6e46ccee21371ff0df688836230c23`,
`sha256:bcbcea229589ed22223403c74e89c49194fbceedb715cf1eeab11b4a36f9d58c`,
`sha256:3c378b4ecec1d5b199987a6d2a1607a1dc0d6d7f34ef0263e13a661d3d89e5f7`,
`sha256:14d7ef22ba8a9ff0b8710b9c252df876ca9fe207f2868808bc24c15e9975746b` and
`sha256:bb5d368c410b4fb70e0a2294cddf5fe3da4018c448bad18a8afe55d470718ddf`.

`GiantRatNpcEdits` corrects the cache-raw 100-tick respawn to 30 and assigns the existing
`constant_melee` hunt mode, which preserves line of sight, chase and the standard non-Wilderness
two-times-combat-level aggression cutoff. `GiantRatDrops` atomically registers the six loaded
identities with guaranteed bones and meat, shared Rag and Bone Man I lifecycle policy at bit zero,
and the shared beginner-clue ownership, capacity and lifetime policy. Three injected scenarios pin
every identity, stat/editor/registry contract, free/member and quest gates, drop order, all three
quest-object inventory/bank forms, persistent hand-in suppression and clue lifetime. Manual case
123 retains live aggro-radius, combat, distribution, presentation, persistence and matching-client
observation. Unloaded regular identities, the separate Stronghold table and Wilderness looting-bag
conditions remain outside this loaded-world slice.

## Loaded regular Rat and Rag and Bone Man II evidence

The final pre-cutoff [Rat revision
14973360](https://oldschool.runescape.wiki/w/Rat?oldid=14973360), dated 25 August 2025, separates
regular identities 2854 and 2855 from Stronghold identities 2492 and 2513. Regular Rat is combat
level one, size one, non-aggressive and uses Crush on a four-tick attack cadence. Its
Attack/Strength/Defence/Hitpoints/Ranged/Magic tuple is 1/1/1/2/1/1, its maximum hit is zero, it is
susceptible to poison and venom, and its regular respawn is two ticks. Most regular rats have no
ordinary or guaranteed drop. Their conditional tertiaries are Rat's tail during Witch's Potion,
members-only Rat bone at 1/4 during Rag and Bone Man II, and a members-only looting bag at 1/15
only in the Wilderness. The Stronghold variants separately guarantee bones and use a 30-tick
respawn, so they are not folded into the regular table.

The final pre-cutoff [Rat's tail revision
14679825](https://oldschool.runescape.wiki/w/Rat%27s_tail?oldid=14679825), dated 11 June 2024,
identifies object 300 and makes it a 100% Rat drop after Witch's Potion starts and before state
three completion. The documented drop trick permits more tails while one is already owned, so
inventory and bank ownership deliberately do not suppress the drop. The final pre-cutoff
[Witch's Potion revision
14994830](https://oldschool.runescape.wiki/w/Witch%27s_Potion?oldid=14994830), dated 26 September
2025, confirms that Giant rats do not supply the tail. Cutoff Quest Helper maps states one and two
to the active quest and state three to completion.

The final pre-cutoff [Rat bone revision
14423224](https://oldschool.runescape.wiki/w/Rat_bone?oldid=14423224), dated 16 June 2023, pins raw,
vinegar and polished objects 7836/7837/7838. The final pre-cutoff [Rag and Bone Man II revision
14990023](https://oldschool.runescape.wiki/w/Rag_and_Bone_Man_II?oldid=14990023), dated 22
September 2025, requires 27 distinct monster bones, gives each relevant monster a 1/4 drop while
its bone remains outstanding, allows partial hand-ins and makes the bones unobtainable after
completion. Cutoff Quest Helper commit `241eaec29b19243bda7e88e99d5c16568c0776a6` maps collection
to shared quest varp 714 state four, completion to state six and Rat seventh in its ordered list,
establishing zero-based hand-in bit six.

Cache-233.4 independently pins NPC 2854 as `rat`, name `Rat`, operation two `Attack`, size one,
the exact six-stat tuple, no hunt mode and raw fingerprint
`sha256:7c81f3d5178cb860cde41d999edebc74f101bc436999d5eef18092752a561ee2`.
The enriched spawn inventory contains sixteen 2854 placements on level zero around Lumbridge;
none is in the Wilderness. Cache objects 300, 7836, 7837 and 7838 have raw fingerprints
`sha256:d84b074d9b316ee6c03a6eb119da9fd4221c09ebb06e91320678dce2e2dd28ff`,
`sha256:d12b5f3c8e3f40e1a68052ed0922ca053e3f60efe16287300c3df4962b2a81e7`,
`sha256:33578a6ba7ecc57b23d9ae0181a46f3d0c342af7d7ac0d4699e495982e0ed188` and
`sha256:3a23013f890560d38c2d096b17eddb69c83130a0c1f4883a011b2aed9a5fca55`.
Quest varp 714, permanent adjacent varp 715 and processing varp 716 all have raw fingerprint
`sha256:6e340b9cffb37a989ca544e6bb780a2c78901d3fb33738768511a30617afa01d`.
An all-script cache scan found direct client-script reads only of quest varp 714; varp 716 is
already partitioned by the named pot/boiler varbits, while 715 has no child varbits and no direct
client-script reader or writer. The server therefore reserves existing permanent cache slot 715
for the 27-bit hand-in mask without adding a type or colliding with client state.

`RatNpcEdits` supplies the sourced two-tick respawn. `RatDrops` registers only loaded identity 2854
with repeatable active-Witch's-Potion tails followed by an independent members-only 1/4 Rat-bone
roll. `RagAndBoneManIIDropPolicy` shares raw/vinegar/polished inventory and bank suppression plus
the persistent 27-bit hand-in state with later II monsters. Three injected scenarios pin the
cache/editor/registry contract, exact quest and membership boundaries, roll endpoints, ordering,
repeat-tail behavior, all bone lifecycle locations and bit preservation. Manual case 124 retains
live combat, probability, ground presentation, persistence and matching-client observation.
Unloaded regular 2855, Stronghold 2492/2513, the Wilderness-only looting bag and the wider two
quest lifecycles remain outside this loaded-world slice.

## Ordinary true rune-altars and output modifiers

The final pre-cutoff [Runecraft revision
14997191](https://oldschool.runescape.wiki/w/Runecraft?oldid=14997191), dated 30 September 2025,
pins the ordinary true-altar table: Air 1/5, Mind 2/5.5, Water 5/6, Earth 9/6.5, Fire 14/7, Body
20/7.5, Cosmic 27/8, Chaos 35/8.5, Astral 40/8.7, Nature 44/9, Law 54/9.5, Death 65/10, Blood
77/10.5 and Wrath 95/8, where each pair is required Runecraft level and XP per essence. It also
pins rune essence to the six elemental/free altars, pure essence to every ordinary altar, whole-
inventory conversion, XP per essence rather than per produced rune, and the visible-level multiple-
rune thresholds reproduced in `StandardRuneRecipes`. The dated Daeyalt source and the official
Darkmeyer release establish that Daeyalt follows pure-essence altar eligibility and awards 50%
more Runecraft XP.

Cache-233.4 independently pins altar locations 34760–34772 plus true Blood altar 43479, each with
operation one `Craft-rune`; essence objects 1436/7936/24704; animation 791 and graphic 186. Client
script 4024 routes Lunar Diplomacy row 88 to varbit 2448, Mourning's End Part II row 100 to 1103,
Sins of the Father row 134 to 7255 and Dragon Slayer II row 32 to 6104. The quest database rows pin
their completion values to 190, 60, 138 and 215. A durable semantic-inspector integration test
asserts all four routes directly against the accepted vanilla cache rather than relying on symbol
names alone.

The cutoff Scar Essence Mine update and dated Wiki product tables establish a non-boostable base
Runecraft-60 and started-Desert-Treasure-II gate. Warped extract 28593 adds 250 runes per matching
Air/Mind/Water/Earth/Fire/Body essence; Twisted 28595, Mangled 28597 and Scarred 28599 add 60 for
their matching rune families. One extract is consumed per essence, the fixed output is additive,
is not multiplied by the ordinary level multiplier or Raiments, and grants no extra XP. Cache
varbit 14862 is the accepted Desert Treasure II state identity.

The cutoff Raiments evidence pins 10% per worn piece and an additional 20% for all four pieces.
For base output `n` and modifier units `m` (one per piece, or six for the full set), the server adds
`floor(n / 10) * m`, plus one further `m` when a uniform 0–9 roll is below `n mod 10`. All four
hat, torso and leg colour identities plus boots are accepted. The calculation runs on base runes
before Scar-extract output and never adds XP. Six integration scenarios exercise the complete altar
table, every input class, all gates, threshold endpoints, partial/no extract eligibility and both
Raiments remainder boundaries.

Mind/body/chaos golem cores remain unresolved rather than implemented. Contemporary official and
cutoff-dated text says a core acts as ten essence for ten-times output and XP; a February-2026 Wiki
correction says eleven-times output while preserving ten-times XP. Sunfire inputs, Blood-essence
integration for dense essence, Guardians and Ourania, the dense-essence altars and the wider
Runecraft activity/access systems also remain separate evidence slices. Manual case 130 owns these
observations and the exact client behavior.

## Active Blood essence and true Blood altar integration

Jagex's official [Nex Rewards - Updated](https://oldschool.runescape.wiki/w/Update%3ANex_Rewards_-_Updated)
clarification predates cache 233 and establishes the stable base contract: inactive Blood essence
is stackable and tradeable; activation creates an untradeable active state; only one may be active;
the active item supplies a 50% chance to create an extra Blood rune; and each item can create 1,000
extra runes. The Blood essence Wiki history records no gameplay changes after January 2022, so the
cutoff [Blood essence](https://oldschool.runescape.wiki/w/Blood_essence) behavior safely adds the
per-input detail: pure essence, Daeyalt essence, guardian essence and dark essence fragments are
eligible, successful extras grant no XP, the item disappears after its 1,000th extra rune, Check
prints `Your blood essence has X charges remaining.`, activation plays `enchant_ruby_ring` sound
146, Guardians extras do not create catalytic guardian stones and Ourania neither benefits nor
consumes charges. It also records the exact duplicate-activation response: `You can only have one
active blood essence at a time.`

Cache 233.4 independently resolves inactive object 26390 as members, stackable and inventory
operation one `Activate`; active object 26392 as members, non-stackable with operation one `Check`
and operation five `Destroy`; and sound 146 as `enchant_ruby_ring`. The cache-enrichment contract
marks the active variant untradeable and supplies its exact destroy note. Server varobject 65523
reserves bits 0–9 for the 1,000-charge item-local state, which remains attached through the
repository's existing inventory, bank, ground-object and persistence paths.

`BloodEssenceLifecycle` atomically splits one item from an inactive stack, rejects free worlds,
rejects an already active item in inventory or bank, stores 1,000 charges and plays sound 146.
`StandardRuneCrafting` rolls once per pure or Daeyalt essence on only the true Blood recipe, adds
one rune and consumes one charge only on successful rolls, grants no extra XP, composes additively
with Scarred Extract output and atomically rewrites or destroys the active item. Six injected
scenarios cover cache identity/operations, activation/sound/check, membership, uniqueness,
capacity, deterministic success/failure rolls, charge preservation/depletion, no-XP/additive
ordering and non-Blood exclusion. Dark-fragment, guardian-essence, Ourania, Ultimate Ironman death
and live matching-client behavior remain with their owning future systems and manual case 130.

## Essence-pouch storage and degradation core

The cutoff [Essence pouch](https://oldschool.runescape.wiki/w/Essence_pouch) and
[Runecraft](https://oldschool.runescape.wiki/w/Runecraft) records establish the common contract.
Pouches accept rune, pure, Daeyalt and guardian essence; cannot hold cores, dense blocks or dark
blocks; require unboostable base Runecraft levels 1/25/50/75 for small/medium/large/giant; and use
capacities 3/6/9/12. The Colossal pouch instead requires level 25 and scales 8 at 25–49, 16 at
50–74, 27 at 75–84 and 40 at 85+. The official
[Guardians of the Rift changes](https://oldschool.runescape.wiki/w/Update%3AGuardians_of_the_Rift_Changes_Out_Now%21)
and [Poll 82](https://oldschool.runescape.wiki/w/Update%3APoll_82_-_Minigame_Makeover)
independently pin those Colossal tiers and its first degradation after 320 stored essence. The
official [Run Energy Changes](https://oldschool.runescape.wiki/w/Update%3ARun_Energy_Changes)
pins the 8 January 2025 dynamic left-click change: Fill or Empty according to pouch state and
inventory contents.

The cutoff degradation table is implemented without interpolation. Medium holds 6 below counter
400, 3 through 799 and is destroyed at 800. Large holds 9 below 400, then 7/5/3 at
400/600/800 and is destroyed at 1,000. Giant holds 12 below 200, then 9/8/7/6/5/3 at
200/300/400/600/800/1,000 and is destroyed at 1,200. Colossal holds 40 below 320, then
35/30/25/20/15/10/5 at 320/565/745/870/950/995/1,015 and is destroyed at 1,020;
each capacity is proportionally scaled from the player's Colossal tier with a minimum of one until
the destruction stage. Threshold crossings return newly excess essence to inventory where space
permits, and dropping a pouch clears its contents.

For ordinary degradable pouches, each stored essence adds a uniform random integer from zero
through three to the counter. The cutoff Wiki table describes the shared algorithm imprecisely for
Colossal, but its official 320-essence boundary and eight full 40-essence fills imply deterministic
one-per-essence wear. A post-cutoff RuneLite report,
[issue 19500](https://github.com/runelite/runelite/issues/19500), explicitly confirms that
distinction. Because the report postdates the production cutoff by one month, it is used only to
resolve the already present official/cache arithmetic, not to import a later gameplay change.
Runecraft and Max capes, including Max-cape variants, suppress new wear while worn and do not repair
existing degradation.

Cache 233.4 supplies the exact client contract. Objects 5509, 5510/5511, 5512/5513, 5514/5515 and
26784/26786 expose `Fill`, `Empty`, `Check`. Client scripts 3198, 3670, 3671, 3695 and 4592 encode
the same capacities and thresholds. Script 3197 maps stored-type codes 1–4 to rune object 1436,
pure 7936, Daeyalt 24704 and guardian 26879. Native permanent transmitted varbits 603–606 and
13682 store counts; 2088–2091 and 13684 store those type codes; and varps 486/720 plus
varbit 13683 over varp 3394 store ordinary/Colossal wear. Local RuneLite was inspected read-only:
its pouch overlay uses the same 40/27/16/8 tier scaling. No RuneLite process was opened.

`EssencePouchStorage` uses these native variables for held and bankside interactions, partial
emptying, degradation, normal-to-degraded object transitions, final destruction, drop clearing and
repairing both inventory and bank variants while preserving contents. Ten injected integration
scenarios pin cache operations and native state, all accepted essence types, membership and level
gates, every capacity boundary, ordinary and Colossal degradation, cape protection, partial empty,
dynamic bankside behavior, repair and final destruction.

Acquisition order and drop ownership, Colossal creation and its mutual exclusion with prior tiers,
login duplicate cleanup, direct Dark mage dialogue, Astral Contact and Cordelia cost/unlock wiring,
Guardians' Redwood-lantern exception, death/item-retrieval policy, and exact messages, sounds,
animations and packet ordering remain manual case 131. The reusable inventory/bank repair seam is
implemented so those NPC and spell slices can integrate without duplicating pouch state logic.

## Kourend dark-essence and false-altar production core

The [Dark essence block](https://oldschool.runescape.wiki/w/Dark_essence_block) record establishes
that Dark Altar conversion takes three ticks, transforms every carried dense essence block at once,
awards 2.5 Runecraft XP and drains one Prayer point per block while still working at zero Prayer.
It also pins the Crafting-38 chisel conversion of one dark block into four fragments for 8 Crafting
XP. The cutoff [Blood rune](https://oldschool.runescape.wiki/w/Blood_rune) and
[Soul Altar](https://oldschool.runescape.wiki/w/Soul_Altar?oldid=14923765) records pin false Blood
and Soul requirements 77/90 and 23.8/29.7 XP per fragment. The official
[Scar Essence Mine update](https://oldschool.runescape.wiki/w/Update%3AThe_Scar_Essence_Mine)
includes Blood and Soul in the Scarred Extract's additive 60-rune family and states that extract
output is not multiplied by Raiments or Blood essence. The
[Kourend & Kebos Diary](https://oldschool.runescape.wiki/w/Kourend_%26_Kebos_Diary) pins the elite
reward as 10% additional false-altar Blood runes with no additional XP; the dated strategy record
resolves its whole-craft rounding down. Existing Raiments and Blood-essence evidence applies their
ordinary base-fragment bonuses before these additive outputs.

Cache 233.4 independently pins false Blood altar 27978 operation one `Bind`, Dark Altar 27979
operation one `Venerate`, false Soul altar 27980 operation one `Bind`; dense block 13445, dark block
13446 and fragment 7938; chisel 1755; Blood/Soul runes 565/566; chiseling sequence 7202; and Kourend
elite completion varbit 7928. Fragment 7938 is deliberately non-stackable but exposes `Count` and
stores four units per converted block. The inventory transaction engine now supports this explicit
counted-non-stackable exception without making the cache type globally stackable or merging groups.

`KourendRuneCrafting` registers the three real location operations plus both held-use orders for
chisel and dark block. Veneration queues the exact three-tick mutation, keeps original slots, bounds
Prayer at zero and awards per-block XP. Chiseling atomically reuses the selected slot even when the
inventory is full. Both false altars atomically consume every counted fragment and apply shared
Scarred Extract/Raiments logic; Blood also applies elite-diary and active Blood-essence bonuses.
Eight cache-aware integration scenarios cover identities, operations, timing, zero Prayer,
membership, visible levels, tool/material failures, counted full-inventory mutation, exact XP,
deterministic modifier composition, charge use and Blood-only exclusions. Three transaction-engine
unit scenarios independently pin counted insertion, partial cross-slot deletion and insufficient-
count rollback.

Dense-runestone Mining is tracked separately below. The automatic multi-block chisel cadence and
movement cancellation, exact rejection/success text, Kourend elite task credit, Soul-rift/Abyss
unlock, dark-tablet/nullifier uses, death/bank behavior and packet ordering remain manual case 132.
The group is therefore `integration-tested`, not client-tested or verified.

## Dense-runestone Mining core

The cutoff [Dense runestone revision 14908570](https://oldschool.runescape.wiki/w/Dense_runestone?oldid=14908570)
pins two personal pillars, members-only Mining and Crafting level 38, a carried pickaxe and chisel,
12 Mining XP and 8 Crafting XP per block, pickaxe-tier-independent speed, no crystal-pickaxe charge
use, no Mining-cape or Varrock-armour duplication and no rock-golem roll. It gives persistence
bounds 210/230 over level 99: 219 of 256 successful persistence rolls at level 38, rising through
the exact five-level plateaus to 231 of 256 at levels 97–99. The cutoff
[Runecraft training revision 14997349](https://oldschool.runescape.wiki/w/Pay-to-play_Runecraft_training?oldid=14997349)
pins the medium Kourend diary reward at an independent 5% chance for a second block with no extra
Mining or Crafting XP. [Dense essence mine revision 14774197](https://oldschool.runescape.wiki/w/Dense_essence_mine?oldid=14774197)
confirms the post-2024 absence of a Kourend-favour gate and identifies the three surrounding
Agility entrances.

Cache 233.4 pins active/depleted base locations 8975/8976 with `Chip`/`Check`, personal parent
locations 8981/10796, dense block 13445, chisel 1755 and independent morph varbits 4927/4928. On a
failed persistence roll the selected pillar changes to depleted while the other is forced active;
because the morph variables live on the player, one player's roll cannot deplete another player's
pillar. The diary completion bit is 7926.

`Mining` now dispatches both personal parent morphs through the active cache identity, requires the
two visible levels and carried tools, applies a fixed nine-tick result interval independent of
pickaxe tier, inserts one non-stackable block or the diary's capacity-bounded second block, awards
XP once, and repeats until depletion, resource loss or capacity interruption. A click made while
already full reports the capacity failure; naturally filling the last slot stops the automatic
loop silently. Fourteen cache-aware `DenseRunestoneTest` scenarios cover identities, operations,
morphs, requirements, visible boosts, exact timing and XP, the 5% diary branch, equipment
exclusions, crystal preservation, tool-loss interruption, capacity, both depletion directions,
per-player isolation and every persistence plateau.

The same cutoff Wiki revision records an unresolved source conflict: a Mod Ash explanation refers
to a three-tick skilling delay sometimes shortening to two on a level-scaled skill roll, while the
article's dated fact check says the observed complete process appears always to take nine ticks.
The implementation follows the directly observed nine-tick gathering time and does not invent an
unknown sub-roll. The documented one-third/both-active versus two-thirds/random-one-active state
on leaving and re-entering the mine is also not yet wired because the three cache shortcut
transports and their exact inside/outside transition endpoints remain unimplemented. These two
items, exact success/failure text, sound, animation restart and packet order remain manual case 132,
so the core is `integration-tested`, not `verified`.

## Sheep shearing and Zanaris dialogue evidence

The final pre-cutoff [ordinary Sheep revision
14970655](https://oldschool.runescape.wiki/w/Sheep?oldid=14970655), dated 21 August 2025, identifies
eleven shearable unsheared NPCs 2693–2699 and 2786–2789 and their exact sheared counterparts
1178, 1299–1304, 1308–1309 and 2691–2692. It directly states that ordinary sheep expose `Shear`,
always award one wool, change form and regrow after 50 ticks. It separately says Zanaris sheep have
no `Shear` operation, must receive shears through item use and regrow after 100 ticks. Permanent
already-sheared spawns are not transformation sources.

The final pre-cutoff [Zanaris Sheep revision
14986149](https://oldschool.runescape.wiki/w/Sheep_(Zanaris)?oldid=14986149), dated 14 September
2025, pins unsheared grey NPC 5843, unsheared light-grey NPC 5844, sheared dark-grey NPC 5845 and
unused sheared light-grey NPC 5846. Its trivia directly records that the light-grey sheep becomes
dark grey when shorn, resolving both 5843 and 5844 to 5845 rather than the tempting cache pairing
5844→5846. Cache 233.4 independently pins Talk-to as operation one for 5843/5844 and objects 1735
shears and 1737 wool.

The cutoff page does not publish Zanaris success probability. The current
[Zanaris Sheep page](https://oldschool.runescape.wiki/w/Sheep_(Zanaris)), inspected 13 September
2026 and therefore explicitly post-cutoff evidence, documents a 25% escape chance and the literal
line `The sheep manages to get away from you!`. This describes the legacy exception that ordinary
sheep lost in 2019; production preserves it provisionally and manual case 133 must confirm it in
the matching client. The final pre-cutoff [transcript revision
14350218](https://oldschool.runescape.wiki/w/Transcript:Sheep_(Zanaris)?oldid=14350218), unchanged
from 6 December 2022 through the cutoff, pins the poem opening, Yes/No branches, four complete
poems, Nuff-certificate exchange and sheared refusal. It only says the Cow answers `sometimes`
after two poems, so response probability and exact presentation remain intentionally unimplemented.

## Loaded Woodsman tutor and Woodcutting Skillcape evidence

The final pre-cutoff [Woodsman tutor transcript revision
14815453](https://oldschool.runescape.wiki/w/Transcript:Woodsman_tutor?oldid=14815453), dated 1
December 2024 and unchanged through the cache-233.4 cutoff, is the dialogue authority. It separates
levels 1–18, 19, 20–28 and 29–98; pins conditional bronze-axe and tinderbox replacement; includes
the two fletching sentences missing from the inherited intermediate route; and exposes Magic and
Hollow trees only in the advanced tree menu. At level 99 it offers a members-only Woodcutting cape
for 99,000 coins and pins the exact refusal, insufficient-money and success text.

The final pre-cutoff [Cape of Accomplishment revision
14994254](https://oldschool.runescape.wiki/w/Cape_of_Accomplishment?oldid=14994254) supplies the
shared cape rules: first mastery is untrimmed, a second mastered skill produces the trimmed form,
and the hood is included. Cache 233.4 independently resolves Woodsman tutor 3226, Woodcutting cape
9807, trimmed cape 9808, hood 9809 and Cape-of-Accomplishment jingle 231. The NPC's immutable raw
fingerprint is `sha256:a9f75dcc9bfdaea54ea20dc6db273215a30983674d10656e3ab84a5e384d0456`.

The pre-cutoff transcript does not record the full-inventory branch. Current transcript revision
15129091, inspected on 13 September 2026, documents the standard cape-plus-free-hood capacity
response. It is explicitly labelled post-cutoff corroboration of a legacy transaction boundary,
not evidence of a cache-233 addition. `WoodcuttingSkillcapePurchase` preflights the entire coin,
cape and hood mutation and then commits the same transaction, preventing partial coin deletion or
one-item delivery. Seven `WoodsmanTutorTest` scenarios pin both transactional and dialogue paths.

## Bob's Barrows and Moon-equipment repair service

The final pre-cutoff [Bob revision
14848825](https://oldschool.runescape.wiki/w/Bob?oldid=14848825), dated 7 February 2025, states
that Bob repairs damaged Barrows and Moon equipment. Its history records the right-click `Repair`
operation from 21 May 2015 and the all-Barrows-items single transaction from 13 June 2013. The
unchanged pre-cutoff [Bob transcript revision
14379729](https://oldschool.runescape.wiki/w/Transcript:Bob?oldid=14379729) pins the no-repairable-
item, item-price, success, cancellation and unsupported-item branches used by production.

The pre-cutoff [Barrows equipment revision
14838793](https://oldschool.runescape.wiki/w/Barrows_equipment?oldid=14838793) supplies the
1,000-point, 15-combat-hour lifecycle and named 100/75/50/25/0 stages. NPC pricing is missing
points multiplied by 60 coins for headgear, 90 for body armour, 80 for leg armour and 100 for a
weapon, making a fully broken four-piece set cost 330,000 coins. The pre-cutoff [Moon equipment
revision 14840313](https://oldschool.runescape.wiki/w/Moon_equipment?oldid=14840313) supplies its
3,000-point, 45-combat-hour lifecycle and 500 coins per missing point, explicitly limiting repair
to the armour rather than the three Moon weapons.

Cache 233.4 independently pins loaded Bob 10619 with operations `Talk-to`, `Trade`, `Repair` and
semantic fingerprint `sha256:7a39d05ffc14ba21edc4085b0f267134a0cb46283e889f2d1a9bfeb4119f2b82`.
It resolves all original Barrows states in 4708–4998, the Ahrim ornament states rooted at 30445
and 30519–30574, and Moon armour new/used/broken objects in 29004–29073. `BobRepairService`
retains exact sub-stage points in item state, falls back to the named-stage boundary when no exact
state exists, excludes fully repaired pieces and Moon weapons, verifies the quoted slots again and
atomically exchanges coins and every selected object. Nine `BobRepairTest` scenarios cover cache
identity, complete and partial quotes, all-item repair, payment rollback, stale quotes, weapon
exclusion and the transcript's right-click/item-use branches.

The same Wiki lifecycle evidence requires an immediate combat-entry point loss and one additional
point every 90 ticks while still in combat. It also states that degradation does not reduce
performance before zero, that an item reaching zero remains worn until removed, and that ordinary
drops become broken. Cache 233.4 independently exposes `Check` on the active Barrows and used Moon
objects, retains wear slots on broken objects but removes their bonuses/usable operations, and has
no degradable Moon-weapon states.

`PlayerCombatEvents`, `PlayerAttackManager` and `NvPCombat` now publish the shared lifecycle boundary
when validated melee, ranged or magic combat is queued, including zero-damage and prayer-blocked
outcomes rather than waiting for projectile impact. `DegradableEquipmentCatalog` models all 28
Barrows and nine Moon armour definitions across 195 cache object states and preserves exact
remaining points in each item. `DegradableEquipmentLifecycle` applies the immediate point, uses the
existing eight-cycle active-combat contract to validate each 90-tick continuation, transitions exact
named states through broken while the object remains worn, and routes held/worn `Check`. The drop
prepare event occurs only after a valuable-item warning is accepted, allowing the ground object to
be broken without mutating an item when the player cancels. Six `DegradableEquipmentTest` scenarios
pin the graph, both combat boundaries, exact transitions, a standard attack-manager event and the
Barrows/Moon ground-drop rule.

Cache tradeability alone cannot express the Wiki's repaired-or-broken rule consistently: several
named intermediate definitions retain a true cache trade flag. `ObjTradeEvents.Query` is therefore
a monotonic server-side denial seam. The degradation script denies every exact point strictly
between full and zero, and the existing protected-access price/trade query plus ordinary NPC shops
both consume that decision. The repaired and broken cache states remain permitted. This establishes
the reusable rule but does not invent a player-to-player trade or Grand Exchange system where the
repository has none.

The existing inventory transaction layer carries each object's `vars` independently. A real bank
integration scenario now deposits two same-identity Barrows objects at 733 and 612 points, proves
they occupy independent bank entries and withdraws both without state loss. A separate in-memory
SQLite save/load test exercises `CharacterInventoryPipeline` and `CharacterInventoryApplier` with
the same two states, proving the production `inventory_objs.vars` column survives logout persistence
and login application.

This core does not claim death-office, gravestone, protected-item or PvP-death behavior; the exact
logout/world-hop combat-timer boundary and client messages, sounds and packet order remain in manual
case 136. Other repair NPCs and the POH armour stand remain separate manual case 135 work.

## Hans account age and time played

The cache-233 NPC definition pins Hans as NPC 3105 with `Talk-to` and third-operation `Age`. The
[Hans article](https://oldschool.runescape.wiki/w/Hans) documents that either talking to him or
using `Age` reports both account age and time spent online. The
[Poll 74 improvements blog](https://oldschool.runescape.wiki/w/Update%3APoll_74_Game_Improvements_Blog)
independently distinguishes the account-summary time-played button from character age, which remains
available through Hans. The transcript supplies the days/hours/minutes and days-since-arrival line.

The production account query already selects `characters.created_at`, and `CharacterAccountData`
already carried it, but `CharacterAccountApplier` previously discarded the value. `Player.createdAt`
now receives that persisted timestamp on login, falling back to the current time only for malformed
legacy rows. The existing permanent `playtime` varp advances once per client cycle; 100 cycles are
one minute. `Hans.ageMessage` performs integer whole-unit conversion and clamps negative playtime or
future creation timestamps to zero. Unit coverage proves metadata application, while `HansTest`
pins conversion boundaries and the actual operation-three dialogue path.

## River Lum canoe routing and axe-selection core

The final pre-cutoff [Canoe revision
14995077](https://oldschool.runescape.wiki/w/Canoe?oldid=14995077) pins a reusable canoe until logout,
replacement when another station is prepared, one-use sinking, automatic repeated success rolls
and success chance based on Woodcutting level plus axe tier but independent of canoe type. Its four
tiers are log at level 12 for 30 XP and one stop, dugout at 27 for 60 XP and two stops, stable
dugout at 42 for 90 XP and three stops, and waka at 57 for 150 XP with unrestricted River Lum
distance and exclusive Wilderness access. It also pins the five build stations and one-way
Wilderness pond. Ferox is intentionally asymmetric: only a waka may enter, while a level-12 log
canoe may leave Ferox for adjacent Edgeville. Transcript revision 14836756 independently pins the
Wilderness confirmation and arrival-message families.

Cache 233.4 supplies the five station, four fallen/ready, four floating and four sinking location
families; shaping and destination interfaces; per-player station/type/confirmation variables;
axe-specific cut rates and shaping sequences; paddle/push/sink sounds; and both grass and Wilderness
cutscene assets. `CanoeConfigTest` now pins every level, XP and sinking-stage parameter, exhaustive
axe rate/sequence tables and the entire five-station-by-four-tier destination graph.

The selection helper previously compared only level requirements, allowing a worn bronze axe to
beat a carried iron axe, and did not apply members-world or Song-of-the-Elves access. It now uses
the same ordered axe families as ordinary Woodcutting, rejects members-only tools on free worlds
and requires quest state 194 for either Crystal identity. The final pre-cutoff Crystal-axe revision
14963010 defines one charge per item obtained, while Infernal-axe revision 14997416 defines one
charge only when its one-in-three log burn succeeds. Canoe cutting and shaping yield no item and do
not roll a burn, so both actions preserve their exact carried or worn special-tool state. Case 138
retains matching-client timing, presentation, concurrency, logout and cutscene verification.

## Dairy cow Cowbell theft core

The final pre-cutoff [Cowbells revision
14892118](https://oldschool.runescape.wiki/w/Cowbells?oldid=14892118) pins the members-only dairy-cow
theft route after Cold War has been started: current Thieving level 15, 16 XP and Cowbells object
10593. Its success table is 54.296875% at level 15, 62.5% at 43 and 78.515625% at 99. Those values
map exactly to rsmod's existing inclusive `SkillingSuccessRate(128, 200, level, 99)` contract.
The [Cold War transcript](https://oldschool.runescape.wiki/w/Transcript:Cold_War), inspected on
13 September 2026 because no final pre-cutoff transcript revision was resolved, corroborates the
literal success line `You steal a cowbell.` and failure sequence `You fail to steal a cowbell.`,
`The cow kicks you and stuns you!`, player damage and the Dairy cow's `MOO`. It does not quantify
that damage or stun duration, so it is supporting presentation evidence rather than authority for
those two values. Historical [varbit 3293 revision
14012126](https://oldschool.runescape.wiki/w/RuneScape:Varbit/3293?oldid=14012126) identifies the
main Cold War progress variable.

Cache 233.4 independently resolves location 8689 as `fat_cow`, named `Dairy cow`, with `Milk` and
`Steal-cowbell` operations and semantic fingerprint
`sha256:6ef2c92b9bbdae464e53ae1f0e957aa8b5dac5ec49865a306e3f7df53d754c37`. Cowbells 10593 is
`peng_cowbell`, fingerprint
`sha256:da7ee7e1851278e3540d4c4ef4c56c3a6713088ce8ab4c677aa123c0870dce97`. Varbit 3293 is
`peng_quest`, backed by varp 968 bits 0–7, fingerprint
`sha256:d62db58e9d26dcbb654382147135c3db143c30293d67df094fa604e26cd90977`.

`DairyCow.resolveCowbellTheft` applies the membership, non-zero quest-state, current-level and
capacity gates before rolling the exact success curve. Success adds the selected Cowbells object
and 16 XP atomically. Failure currently shares the ordinary Thieving stunned animation, spot
animation and eight-cycle lock, with one typeless damage. Four `DairyCowTest` scenarios cover the
cache identities/operations and curve endpoints, every eligibility boundary, exact success reward
and XP, and the bounded failure behavior. The production operation wrapper still performs arrival
and facing before entering that resolver.

The same dated Cowbells revision pins inventory operation one `Play` and limits it to the Penguin
suit. Cache varbit 3306 `peng_transmog` is the one-bit disguise state, fingerprint
`sha256:fa1bf0c67bc9dcca5af930035a188d67b2b3588d0f303a5d0dd218bb7849943c`, and cache sequence
5673 `peng_cowbell_ready`, fingerprint
`sha256:df894ca702d03fcef3421f12babe027734174718f078559de4e9fbc875437588`, is the dedicated
45-client-tick Cowbell animation with no embedded frame sound. A [legacy Wiki
mirror](https://wiki.darkan.org/Cowbells), inspected on 13 September 2026, corroborates the
rejection line `You must be wearing the penguin suit to play these.` Production now dispatches
the item operation, rejects a zero disguise varbit and plays sequence 5673 without consuming the
item. The rejection chat type, action-lock duration and packet order remain client checks.

This row does not cover the conversation-based handout from Zanaris Dairy cow 12111 or the wider
Cold War quest lifecycle. Exact damage, stun timing, audiovisuals, approach/facing delays,
interruption, operation packet order and simultaneous-player behavior remain matching-client case
139.

## Penguin-bongo Crafting and Play core

The [Crafting experience table](https://oldschool.runescape.wiki/w/Crafting/Experience_table),
inspected on 13 September 2026, records Penguin bongos as a members product requiring Crafting 30,
granting 25 XP and consuming one mahogany plank plus one leather. The [Cold War quick
guide](https://oldschool.runescape.wiki/w/Cold_War/Quick_guide), inspected the same day, adds the
important exception that making the drums requires an **unboosted** Crafting level 30, directs the
player to combine those two materials after speaking to Ping or Pong, and states that either
Cowbells or bongos can only be played in a penguin suit. The current [Cold War
transcript](https://oldschool.runescape.wiki/w/Transcript:Cold_War) supplies the literal result line
`You use the leather and plank to create penguin bongos.` These pages describe both quest-time and
post-quest replacement creation, but do not identify the exact first accepted value of the main
Cold War progress varbit. Production therefore uses the bounded non-zero gate pending case 140.

Cache 233.4 resolves leather 1741 `leather`, mahogany plank 8782 `plank_mahogany`, and Penguin
bongos 10592 `peng_bongos`. The output is members-only, is named `Penguin bongos`, exposes
inventory operation one `Play`, and has semantic fingerprint
`sha256:72b3a4d69b80c085cc1277091c4fb95af116fefdfbdf4303ea471489272dd214`. The shared Cold War
progress and disguise contracts remain varbits 3293 and 3306 with fingerprints
`sha256:d62db58e9d26dcbb654382147135c3db143c30293d67df094fa604e26cd90977` and
`sha256:fa1bf0c67bc9dcca5af930035a188d67b2b3588d0f303a5d0dd218bb7849943c`.

Two adjacent cached sequences were compared. Sequence 5674 `peng_player_bongo`, fingerprint
`sha256:c76eeff4490e821c1e689c63b1663d247e24c3fa997c12721545a896aa89a100`, is the short
147-client-tick action explicitly named for the player. Sequence 5675 `peng_bongo_ready`,
fingerprint `sha256:d7b62867391094f0b825ab12ebe0f29c3f833cf1f182e192ef0bf41611240d32`, is a 366-client-tick
ready loop. Neither includes cached frame sounds. Inventory Play uses 5674; 5675 is retained but
not assigned to the quest performance until a revision-matched client trace confirms it.

`PenguinBongosTest` pins every identity and operation, rejects membership, quest and unboosted-level
violations without mutation, proves that a visible boost cannot bypass level 30, performs the
two-input/one-output transaction in a full inventory, checks exact XP and transcript text, routes
reverse held use through the production dispatcher, and proves Play preserves the item. Exact
quest-stage values and all remaining client choreography are reserved for case 140.

## Bird-house shell Crafting core

The [Bird house trapping](https://oldschool.runescape.wiki/w/Bird_house_trapping) table, inspected
on 13 September 2026, defines the nine members-only shell recipes. Regular, oak, willow, teak,
maple, mahogany, yew, magic and redwood logs require boosted Crafting levels
5/15/25/35/45/50/60/75/90 and award 15/20/25/30/35/40/45/50/55 Crafting XP. Each recipe combines
one matching log with one reusable clockwork and requires a hammer and chisel. The
[Redwood bird house](https://oldschool.runescape.wiki/w/Redwood_bird_house) action description
separately pins a two-tick first result and three-tick automatic repeat. These current pages are
used only for mechanics that predate and remain represented by the cache-233 objects; they do not
expand the revision boundary.

Cache 233.4 resolves input logs 1511/1521/1519/6333/1517/6332/1515/1513/19669, clockwork 8792
`poh_clockwork_mechanism`, and outputs 21512/21515/21518/21521/22192/22195/22198/22201/22204.
It also resolves normal sequence 7057 `birdhouse_make` and Imcando sequence 8916
`birdhouse_make_imcando_hammer`. Workspace RuneLite independently maps those outputs and the two
sequences through its generated item constants and `AnimationID`; it was inspected as source only
and was not launched. Normal hammer 2347 and both Imcando forms 25644/29775 are accepted, including
worn forms, while chisel 1755 remains an inventory tool.

`BirdhouseCrafting` registers clockwork with every log through the symmetric production held-use
dispatcher. It clears all other Crafting queues, presents the matching hammer animation, produces
the first result after two ticks, and compensates rsmod's in-handler rescheduling so later results
land at exact three-tick gaps. Each result atomically exchanges one log and one clockwork for one
shell, which remains safe in a full inventory. `BirdhouseCraftingTest` exercises all nine contracts,
every gate, both item orders, normal and off-hand Imcando animation selection, exact XP, cadence,
material exhaustion and cancellation. Its single-result transaction is also reused by the separate
Hunter site lifecycle, without importing site behavior into this Crafting evidence row.

## Bird-house placement and seeding core

The [Bird house trapping cutoff revision](https://oldschool.runescape.wiki/w/index.php?title=Bird_house_trapping&oldid=14991901)
is the last page revision at or before the cache-233 cutoff, 1 October 2025 23:59:59 UTC. It supplies
four Fossil Island trapping sites, Hunter requirements 5/14/24/34/44/49/59/74/89, ten low-level or
five high-level seeds, mixed partial loading and an approximately 50-minute maturation period. The
page's suitable-bait table contains 41 identities: 30 one-unit allotment, flower, bush, low-herb
and low-hop seeds plus 11 two-unit high-herb and Wildblood seeds. Its 12 February 2025 change log
adds `Reset` and direct material-building conveniences.

Cache 233.4 resolves the four base site locations as 30565–30568 at coordinates
(3677,3882), (3679,3815), (3768,3761) and (3763,3755), driven by permanent display varps
1626–1629. Each base resolves value zero to not-built location 30552 and values 1–27 to exact
built/partial/active triples for the nine tiers: locations 30553–30564 and 31827–31841. Portable
objects 21512/21515/21518/21521/22192/22195/22198/22201/22204 align with those triples. Cache
synth 2433 is `farming_fillplantpot` and is used for successful build/fill actions. Workspace
RuneLite's `BirdHouseState` independently corroborates the three-value tier grouping, its
`BirdHouseTracker` uses 3,000 seconds, and its tracker maps display varps 1626–1629; it was read as
source only and was not launched.

Four permanent non-transmitted rsmod varps 65536–65539 store partial bait units or an absolute
epoch-second readiness deadline. `BirdhouseTrapTest` pins every cache identity and handler, all
nine boosted-level placements, atomic membership/level/stale/slot failures, all 41 seed identities,
five-versus-ten consumption, mixed partial loading, the exact 3,000-second readiness edge and an
end-to-end item-on-base-multiloc route.

The same cutoff revision defines the evidence-complete mature harvest subset: Hunter XP
280/420/560/700/820/960/1020/1140/1200; one returned clockwork; one of 30/40/50/60 feathers; ten
raw bird meats that always fall to the floor; and at most one seed nest based on visible Hunter
level rather than bird-house tier. The [cutoff success-chart module revision](https://oldschool.runescape.wiki/w/index.php?title=Module:Skilling_success_chart&oldid=14931997)
pins that seed-nest chance as rounded interpolation with low 0 and high 200 over denominator 256:
level 1 is 1/256, level 50 is 101/256 and level 99 is 201/256. Production captures the visible
level before awarding XP, privately grounds full-inventory rewards, always privately grounds the
meat, and resets both site values only after verifying the exact active and mature state.

The same cutoff revision defines the destructive lifecycle: dismantling or emptying before
completion loses the constructed house and inserted seeds but saves the clockwork, while mature
dismantling yields the normal XP and loot. It also records the 12 February 2025 direct-build and
automatic-rebuild update. Jagex's
[Poll 83 result](https://oldschool.runescape.com/polls/2024/1698) independently establishes the
intended automatic rebuild when all required inventory items are present; 54,177 votes, or 91.8%
of non-skips, approved it. The cache provides the operation contract: not-built location 30552 has
`Build`; the nine partial locations expose `Dismantle`; and the nine active locations expose
`Empty` and `Reset`.

Production consequently gives a carried usable shell priority on both `Build` and `Reset`. With no
shell, `Build` selects an available log, validates the corresponding Crafting and Hunter levels plus
clockwork, chisel and hammer, then uses the shell recipe's two-tick result before immediately placing
the output. Raw-material `Reset` is modelled as the same two-tick craft plus the documented one-tick
automatic-rebuild penalty: it harvests normally, retains the site's recovered clockwork without
requiring a temporary free inventory slot, consumes one selected log, awards the shell's Crafting
XP and leaves the replacement built but unseeded. A carried-shell Reset takes one tick and returns
the harvested clockwork. These exact tick assignments are an implementation inference from the
cutoff action descriptions and existing shell cadence; revision-matched client observation remains
required for audiovisual and packet-level confirmation.

Early `Empty` and partial `Dismantle` present explicit loss confirmations, revalidate the same site
state and stored seed/deadline value after the suspension, then clear both values and return or
privately ground only the clockwork. `BirdhouseTrapTest` now covers 20 scenarios, including direct
raw building, carried-shell priority, accept/decline behavior, partial dismantling, both Reset paths,
exact Crafting and Hunter XP, path timing, mature rewards and stale-state revalidation.

The module also pins each other-nest success roll as
`floor(high * (max(50, visible_level) - 1) / 98) / 1000`, with tier highs
100/125/128/130/140/150/160/170/175. However, the cutoff page explicitly says its clue-nest
selection mechanics are incomplete and unconfirmed. The ten-roll other-nest branch therefore
remains manual case 142 rather than receiving an invented production contract; exact confirmation
wording, multiple-log chooser presentation, interruption edges and all client choreography remain
there as observation work rather than being promoted to evidence-complete behavior.

## Bird-snare trapping core

The final pre-cutoff [Bird snare revision
14969782](https://oldschool.runescape.wiki/w/index.php?title=Bird_snare&oldid=14969782), dated 20
August 2025, defines the members-only trap, boosted-level concurrent limits of one through five at
Hunter 1/20/40/60/80, the extra Wilderness trap, smoking's absolute 2% catch bonus, the inability
to catch while the owner stands directly on the snare and a one-minute trap lifecycle. The final
pre-cutoff [game-tick revision
14958366](https://oldschool.runescape.wiki/w/index.php?title=Game_tick&oldid=14958366) defines one
tick as 0.6 seconds and trap placement as four ticks, making the lifecycle 100 server cycles.

Five final pre-cutoff creature revisions pin requirements, XP and success endpoints:
[Crimson swift 14780803](https://oldschool.runescape.wiki/w/index.php?title=Crimson_swift&oldid=14780803)
uses level 1, 34 XP and 100/420;
[Golden warbler 14781115](https://oldschool.runescape.wiki/w/index.php?title=Golden_warbler&oldid=14781115)
uses level 5, 47 XP and 92/400;
[Copper longtail 14945768](https://oldschool.runescape.wiki/w/index.php?title=Copper_longtail&oldid=14945768)
uses level 9, 61.2 XP and 85/390;
[Cerulean twitch 14819584](https://oldschool.runescape.wiki/w/index.php?title=Cerulean_twitch&oldid=14819584)
uses level 11, 64.5 XP and 82/380; and
[Tropical wagtail 14964851](https://oldschool.runescape.wiki/w/index.php?title=Tropical_wagtail&oldid=14964851)
uses level 19, 95.2 XP and 75/370. Each catch returns the snare, bones, raw bird meat and 5–10
species-coloured feathers. Production uses rsmod's shared success interpolation over denominator
256 and adds the documented smoke bonus. The final pre-cutoff [Guild hunter outfit revision
14986327](https://oldschool.runescape.wiki/w/index.php?title=Guild_hunter_outfit&oldid=14986327)
pins a 2.5% full-set increase but explicitly says its additive-versus-multiplicative application is
unknown. Production currently applies that small increase additively; manual case 143 must retain
the qualification until stronger revision-matched evidence exists.

Cache 233.4 resolves snare 10006, birds 5548–5552, raw bird meat 9978, feathers 10087–10091,
open/broken/transition/full locations 9344–9349 and 9373–9379, lay/check player sequences
5208/5207 and bird leave/caught sequences 5172/5173. Its operations are authoritative: the item
exposes first action `Lay`; an open snare exposes `Dismantle` and `Investigate`; a failed snare
exposes only `Dismantle`; and each full snare exposes only `Check`. Workspace RuneLite's
`HunterPlugin`, `HunterTrap` and `AnimationID` independently corroborate these model families, the
one-minute timer and player sequences; that checkout was read as source and was not launched.

`BirdSnareTrapTest` contains seven integration scenarios covering the complete cache contract, all
five prey, placement and limits, smoke/outfit rates, deterministic success and failure, exact
two-cycle transitions, atomic collection, full-inventory retry, dismantling and private-ground
expiration. The cache and dated sources do not encode NPC search radius or exact client
choreography, so the current two-tile attraction radius and three-cycle search rate remain bounded
engineering values pending manual case 143.

## Varlamore sunlight and moonlight moth core

The final usable pre-cutoff [Sunlight moth revision
14786122](https://oldschool.runescape.wiki/w/index.php?title=Sunlight_moth&oldid=14786122)
pins level 65, 74 Hunter XP, level-75 barehanded catching and success endpoints 20/296 for an
ordinary net or bare hands and 40/316 for a magic net. A direct catch without a jar restores eight
Hitpoints, five percent run energy and `floor(base level × 20%) + 6` to every reduced stat except
Hitpoints and Prayer. Releasing a jarred moth applies the stat and Hitpoints restoration but not the
run-energy restoration. The final usable pre-cutoff [Moonlight moth revision
14984905](https://oldschool.runescape.wiki/w/index.php?title=Moonlight_moth&oldid=14984905)
pins level 75, 84 Hunter XP, level-85 barehanded catching, 0/276 regular-net and 20/286
magic-net/barehanded endpoints, and a flat 22 Prayer restoration for direct and jar release.

The final usable pre-cutoff [Sunlight moth item revision
14853728](https://oldschool.runescape.wiki/w/index.php?title=Sunlight_moth_(item)&oldid=14853728)
and [Moonlight moth item revision
14927538](https://oldschool.runescape.wiki/w/index.php?title=Moonlight_moth_(item)&oldid=14927538)
confirm that `Release` returns an empty butterfly jar and that the Holy wrench does not modify the
moonlight value. The [Butterfly Hunter revision
14833883](https://oldschool.runescape.wiki/w/index.php?title=Butterfly_(Hunter)&oldid=14833883)
establishes wielded regular/magic nets, the +10 barehanded threshold and immediate release when no
jar is carried. It does not supply exact success curves for the four legacy butterflies, so they
remain outside production rather than inheriting guessed moth values.

The cache-233.4 semantic manifest independently resolves catchable NPCs 12770/12771/12772 and
non-clickable variant 12773. Their raw fingerprints are respectively
`sha256:1f8a951eb5aad7d6a40e102b418d7582d4115917598ae1fa9063c1b1b95eae9c`,
`sha256:04b06e017434354f2192a236891121f0306bbaf15e11b9a2963e5475df053ec3` for both
moonlight catchable variants, and
`sha256:41a3057c7434be395d56a8cf8a448d462c12b539485cbb6f9cb51463d144e341`.
Only the first three expose `Catch`; all four encode respawn rate 100. Butterfly net 10010, magic
net 11259, empty jar 10012, sunlight jar 28890 and moonlight jar 28893 have fingerprints
`sha256:02092af6fbed49a75934975c4d675ee7b6fd67d37ea52083aee7c73b054d641a`,
`sha256:f04852743e0f7632b69e609a10da2c50b67e787ab81c451458cffadd8bf7a235`,
`sha256:7fdcc27a3e30f93b7598285e28fbb2474d9ee0da5c7c78f7126a3bb0d1b70449`,
`sha256:876a3298e19e6cfda979d2b3c5ade31577f31a29de0ffd2bbcc31ce8479a6b5c` and
`sha256:4d32f6ec13089a783b268d0fcdb6c7b2fcac86e44889efb19786c80e034b706b`.
The nets occupy right-hand slot 3 and both filled jars expose inventory operation four `Release`.

Player sequences 5209/5213/5214 resolve as `human_butterflynet_swing`,
`hunting_opening_butterfly_jar` and `hunting_butterfly_out_jar`, with fingerprints
`sha256:3eef26fdadc68dfa90a208054f244f61a4a67762927562ce05e3ff142adbb070`,
`sha256:71bbdada498e76b83445f83c114fe7ccad5e793d14f66d1b188af982dad01450` and
`sha256:4c594f73f81632836106d0a906a2bdc3d42312971632a341d5d851ff079d3cb5`.
Production uses the player catch and jar-open sequences; sequence 5214 is identity-pinned but its
creature-visual choreography remains manual case 144. Workspace RuneLite's `SunlightMoth`,
`ItemStatChanges` and `ItemStatEffectTest` corroborate the floor-before-add restoration order, the
21 restored production stats and flat moonlight Prayer value. That checkout was read only and was
not launched.

`MothRateCurveTest` pins the four level-threshold probabilities and the sunlight rounding rule.
`MothHuntingTest` contains nine cache-aware scenarios covering cache identities and handlers,
membership/visible-level/equipment selection, full-inventory atomic capture and release, direct
versus jar effects, every sunlight stat cap, deterministic failure preservation, both catchable
moonlight variants and the exact 100-cycle respawn schedule. The one-tick server action, exact
messages and audiovisual/packet choreography remain qualified implementation choices until manual
case 144; the production boundary does not claim legacy butterflies, multiplayer sharing, rare
wings or Hunter Rumour progression.


## Cache-233 production-skill and diary completion pass

The consolidated dated sources, cache checks, functional coverage, explicit approximations and
remaining progression systems for Cooking, Crafting, Smithing, Fletching, Herblore, Mining, Fishing,
Firemaking, Woodcutting, Thieving and Hunter are recorded in
[PRODUCTION_SKILLS.md](PRODUCTION_SKILLS.md). This includes ordinary anvil products, portable box
traps, falconry, aerial fishing, master farmer seeds and legacy butterflies, plus successful-result
diaries and saved ordered production steps. The record distinguishes exact published data from
bounded server models; it does not claim full skill/activity/client parity.


Production-skill follow-up: the current [production ledger](PRODUCTION_SKILLS.md) now records the
native high-tier Fishing/Tynan, Kourend mineral gathering, Otto training/Barbarian Smithing,
Zahur unfinished-potion and post-2024 Wintertodt implementations, dated source revisions,
private packed-cache test counts and explicit remaining approximations.
