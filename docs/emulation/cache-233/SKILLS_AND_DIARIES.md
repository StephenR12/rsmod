# Skills and Achievement Diaries

> The active world now targets [revision 238 for Demonic Pacts](../cache-238/README.md). This report records the earlier cache-233 checkpoint.

## In-game test checkpoint, 12 November 2026

All 23 skills listed below have playable server-side actions in the 1 October 2025 world (cache 233.4). Each skill row gives a first in-game check and its expected result. The combat and production reports record the remaining encounter rules, uncertain rates and client presentation details. [Combat and Slayer](COMBAT_AND_SLAYER.md) · [Production skills](PRODUCTION_SKILLS.md)

### Start the local world

To restart the server, open the `rsmod` checkout and run `./gradlew --offline run`. In a second terminal in the same directory run `../runelite/run-rsmod.sh`. The sibling RuneLite launcher uses the project's RSA public key and connects to the local revision-233 server on port 43594. The development realm accepts a test username and a nonempty test password. The development realm’s default XP rate is 150×. Use `::xprate 1` on a test account when comparing XP against published base values. After changing cache symbols, maps or NPC definitions, stop the world and run `./gradlew --offline packCache` before restarting.

### First in-game checks

| Where to test | Action | Expected result |
| --- | --- | --- |
| Draynor Village rooftop course | On a members account, finish the level 1 course, open the Lumbridge & Draynor diary and log back in. | The course grants Agility XP and its easy task stays checked after login. A second lap grants XP without advancing the diary twice. |
| Warriors’ Guild | With Attack and Strength totalling at least 130, animate a complete armour set, defeat it and take the tokens upstairs. | The fighting style determines combat XP. The token balance pays for cyclops access. |
| Player-owned house and a nearby farming patch | Build furniture, plant a crop, then leave and log back in. | The house layout and planted crop persist. Inspect the patch again after a growth interval. |
| Tower of Life basement | Use a Tower of Life–complete save. At the southwest altar, offer a raw swordfish and raw chicken. Activate it and kill the Swordchick. | Each ingredient is consumed once. The kill drops feathers and checks the Ardougne Medium task. |
| God Wars Dungeon | With 83 Slayer and access to the main dungeon, defeat a spiritual mage. Repeat inside the Wilderness God Wars Dungeon. | The first kill checks Fremennik Elite and the second checks Wilderness Elite. |
| Camdozaal | On a Below Ice Mountain–complete save, mine and crush a barronite deposit, then prepare and offer a caught fish. | The crusher resolves the deposit, and the fish offering grants Prayer XP. |
| Achievement Diary reward | On a save with a completed tier, speak to that region's taskmaster with inventory space. Claim the reward and use the lamp, then speak again. | The equipment and lamp arrive together. The saved claim prevents a second lamp. |

For a disposable development account the server supports `::setlevel agility 10`, `::mypos` and `::tele x z level`. For the Swordchick check, `::varbit tol_prog 18` supplies the earned Tower of Life state. Leave diary completion flags to the actions above. The full manual test guide includes a template for recording coordinates, equipment, save state and observed behaviour. [Manual tests](MANUAL_TESTS.md)

## Achievement Diaries

The journal covers 492 tasks across 12 regions and 48 tiers. Each row reads the same saved bit or
counter used by the cache client. Ordinary skill, combat and travel scripts award individual tasks
after their success conditions. Opening the journal reconstructs tier counts from saved tasks.
Claiming a reward requires the completed tier and all previous claims for that region. The upgrade
and lamp use one inventory transaction. Karamja uses its own lamp and legacy counter rules.

The regional catalogue maps all 492 rows to gameplay completion handlers. The count is a code audit. Individual routes have server tests, and a full 492-row in-client playthrough remains open. Integration tests exercise individual activities, including ordered multi-step tasks, the four Western chompy-hat thresholds, Pyramid Plunder and the Blast Furnace foreman. The journal also displays saved progress on imported accounts.

The journal tests exercise the sidebar, repeated actions, ordered tier claims, lamp selection and
the Lumbridge Easy reward. Equipment tests cover account-wide charges across duplicate copies,
lost-item replacement, Prayer restoration and the Explorer's ring alchemy interface. The
equipment script implements teleport destinations, daily counters and the appropriate actions
on worn and carried rewards. Individual rewards such as the staff-free fairy ring check consult
the saved tier directly.

## Skill coverage

This table describes gameplay entry points in this checkout. The feature matrix and source
ledger retain the individual recipe lists, historical evidence and client-capture gaps.

| Skill | Implemented gameplay | First in-game check |
| --- | --- | --- |
| Attack | Melee accuracy, weapon requirements, accurate and controlled styles, training dummies, Warriors’ Guild training and defenders. | Use an accurate melee style on a training dummy below level 8, then on an ordinary NPC. Check the Attack XP counter. |
| Strength | Aggressive and controlled training, effective Strength, melee maximum hits, Warriors’ Guild access and training. | With a melee weapon, switch to an aggressive style and land a hit. Check that Strength gains the style XP. |
| Defence | Defensive, controlled and longrange styles, defensive equipment, protection and defence rolls. | Use a defensive melee style on a damageable NPC. Check Defence XP and the equipped shield after combat. |
| Hitpoints | Eligible combat damage awards XP. Death, regeneration, Rapid Heal, cape effects, ordinary food and delayed Hunter-meat heals use the player’s live Hitpoints. | Take damage, eat an ordinary fish and watch the current HP value. Then land a damaging combat hit and check Hitpoints XP. |
| Ranged | Weapon styles, projectiles and the toxic blowpipe. Includes ammunition recovery, Ava’s devices, chinchompas, cannons and rechargeable crystal bows. | Fire a shortbow with arrows at an NPC. Check Ranged XP and ammunition after each attack. |
| Agility | Rooftop and overground courses, shortcuts, Grace and marks, Brimhaven Arena, Agility Pyramid, Prifddinas, Colossal Wyrm and five Hallowed Sepulchre floors. | Finish the Draynor rooftop course and check the lap XP and the Lumbridge & Draynor easy task. |
| Slayer | Master assignments and eligibility, credited kills and XP, Slayer points, task cancellation, blocking and storage, level/equipment restrictions, finishing blows, superior monsters and remote master contact. Includes task-aware boss encounters, cave kraken whirlpools, wyverns, ordinary hydras, wyrms, warped creatures, spiritual creatures and their dungeon routes. | Get a master assignment. Kill an assigned target and an unrelated monster, then compare the remaining count and XP. |
| Construction | Estate agents, saved house layouts, room and furniture building, kitchen facilities, pools, altars, costume storage, menageries, sawmill, Mahogany Homes and STASH units. Houses persist across three floors. Upper rooms, basement stairs, marble upgrades, passages and furniture survive reloads. | Build a chair in an owned house, leave the house, log back in and re-enter to check the saved furniture. |
| Farming | Saved crop growth, disease and protection, seedlings, grapes, specialist patches, Guild contracts and seed packs, Hespori and Tithe Farm. | Rake an allotment and plant potato seeds with a seed dibber. Check the patch after a growth interval and after logging back in. |
| Prayer | Activation, drain and restoration, bones, ashes, house and wilderness altars, Teomat, Forthos, ensouled heads, demonic offerings, the Ectofuntus and Robin’s diary allowance. | Bury a bone and check XP. Activate an unlocked prayer, watch its points drain and restore them at an altar. |
| Magic | Standard, Ancient, Lunar and Arceuus casting, teleports, utility and combat spells, rune pouches, charged staves, reanimation and thralls. | Cast Wind Strike on a valid NPC with air and mind runes. Check rune use and Magic XP on cast. |
| Runecraft | Ordinary and combination runes, altar entry and exit, tiaras, rechargeable and colossal pouches, Kourend, Ourania and Sunfire altars, Camdozaal cores, essence mining, the Scar Essence Mine, Guardians of the Rift, and wizard travel. | Enter the air altar with a talisman or tiara and craft a load of rune essence. Check output and XP. |
| Crafting | Tanning, leatherworking, pottery, glass, spinning and snail helmets. Includes desert sandstone grinding and sand collection, dragonhide, jewellery, crystal tools and bow singing. | Cut an uncut sapphire with a chisel at 20 Crafting. Confirm a cut sapphire and Crafting XP. |
| Mining | Ordinary rocks, gems, quarry rocks, amethyst, crystal and infernal pickaxes, and celestial equipment. Includes coal and gem storage, mining gloves, Motherlode, Shooting Stars, Blast Mine, Volcanic Mine, Zalcano and Camdozaal barronite processing. Fossil ash and Cam Torum deposits use their own yield and depletion rules. | Mine copper with a bronze pickaxe. Check the ore, XP, depleted rock and respawn. |
| Smithing | Furnaces, cannonballs, Blast Furnace, ordinary anvil interface and 156 recipes, blurite limbs, dragon square shields, gold helmets and crystal singing. Includes the Elemental Workshop and Giants’ Foundry commissions, alloy processing, heat management, the Smiths’ Uniform and reward purchases. | Smelt copper and tin into a bronze bar. Use a hammer and anvil to make a bronze dagger. |
| Fishing | Rod, net, cage and harpoon fishing, karambwan, aerial fishing, Fishing Trawler, Fossil Island drift nets and Tempoross with reward fishing and tackle storage. Camdozaal fishing, preparation and offerings connect Fishing to Cooking and Prayer. | Net shrimp from a small-net spot. Check catches and XP, and cook a catch on a fire. |
| Cooking | Fire, campfire, range and private-house cooking, seaweed, pies, cakes, pizzas, gnome recipes, skewers, food consumption and regional catch-and-cook tasks. Includes three breweries, all ten ordinary and mature ales, kegs, multi-bite foods and delayed Hunter-meat healing. | Use bread dough on the Lumbridge Castle range on a save with range access. Successful bread checks the easy diary task. |
| Firemaking | Log, coloured and bow fires, tending campfires, light sources, Wintertodt, pyre ships, shade cremation and the Mort’ton temple repair and sacred-oil loop. | Light ordinary logs with a tinderbox. Check XP, cook on the fire and observe burnout. |
| Woodcutting | Tree yields, axes and crystal charges, nests, Ents and the Guild dungeon, canoes, Neitiznot stumps and regional cut-and-burn tasks. Includes nine Forestry events, the Forestry shop, log baskets, felling axes and canoes, Sulliusceps and blisterwood. | Chop an ordinary tree with a usable axe. Check the log, XP, stump and regrowth. |
| Herblore | Herb cleaning, potion mixing, grinding, Wesley’s bulk service, Zahur’s paid cleaning, sacred oil and location-specific diary recipes. Includes Mastering Mixology, digweed, reagent packs, prescription goggles, aldarium, the reagent pouch, bank potion storage and the chugging barrel. | On a Herblore-unlocked save, clean a grimy guam, combine it with a vial of water and add an eye of newt. Check the attack potion and dose count. |
| Thieving | Pickpockets, stalls, master farmers, safes, city chests, blackjacking and stealing Piscarilius artefacts. Includes Rogues’ Den, Sorceress’s Garden, the rogue outfit and Pyramid Plunder. | Pickpocket a man or woman. A success gives coins and XP. A failed attempt can stun the player. |
| Fletching | Cutting and stringing bows, crossbows and stocks, bolts, darts, antler ammunition, the hunters’ sunlight crossbow and kebbit spikes. Includes atlatl ammunition and Vale Totems, with carved totems, decorations, ent offerings and reward collection. | Use a knife on ordinary logs and select arrow shafts. Check the selected quantity and Fletching XP. |
| Hunter | Bird, box, net, deadfall and pitfall traps, butterfly and moth hunting, five noose-wand trails, falconry, birdhouses, Puro-Puro implings, aerial fishing and drift nets. Includes guild rumours, saved streaks, rare parts, sunlight and moonlight antelopes, herbiboar, jerboas, maniacal monkeys and two-dose Hunter mixes. | Set a bird snare near crimson swifts. Collect a successful catch and check that the trap returns to inventory. |

## Verification

**4,981 automated tests passed** on the final shared checkout. The Gradle reports contain 3,230 integration tests across 118 modules and 1,751 unit tests across 41 modules, with zero failures, errors or skips. The integration count includes 69 combat-training, 59 diary travel, six diary journal, 132 Mining and 18 Hunter moth tests. The cache pack and revision check also passed.

The development world loaded 620 scripts and accepted a revision-233 JS5 handshake and master-index request on port 43594. Startup applied the existing house pet rock migration to the local save database (schema v11). Client login, graphics, menus and the manual routes above await player testing.

Run `./gradlew --offline packCache` after changing world data. The module task is `integration`.
For example, `./gradlew --offline :content:interfaces:journal-tab:integration
:content:travel:diary-routes:integration :content:skills:runecraft:integration` exercises the
journal, equipment, travel, rune crafting, pouch repairs and full-inventory transactions.
Individual world-action tests exercise successful and rejected actions through server operations
and interface packets.

### Remaining playtest work

Client comparisons remain open for projectiles, animations, interface timing, encounter choreography and some harvest and reward probabilities. Spatial tests verify selected dungeon entrances, spawns and kills. An exhaustive test of every Slayer assignment or diary prerequisite through an ordinary account remains open. Hunter imp defenders and player-made impling jars remain deferred. Players can already acquire jars from Elnock and his jar generator.

## Quest TODO

Implement quest progression and the original quest rewards if quests become part of the project.
Preserve the existing save variables and use them for eligibility. Examples include Rune Mysteries
for teleportation to the essence mine, Eagles' Peak for box trapping, Lunar Diplomacy for Lunar
spells, and Mourning's End Part II for death runes. Accounts starting from zero progress require
the relevant quest implementation to earn those unlocks in game.

## Sources

The diary lists are adapted from the Old School RuneScape Wiki and its contributors. The adaptation
removes markup and requirement columns, groups tasks by tier, and associates entries with native
variables. The wiki-derived diary content retains its [CC BY-NC-SA 3.0 license](https://creativecommons.org/licenses/by-nc-sa/3.0/).
Task flag assignments were cross-checked against the [Signpost diary data](https://github.com/reece-backhouse/signpost/blob/f25af2b92fc4d4c5e96a35adf6a7fc7ebed447ae/src/main/resources/kb/diaries.json)
and tier counts against clientscript 2200 in the revision-matched cache.

- [Ardougne Diary](https://oldschool.runescape.wiki/w/Special:PermanentLink/14989857)
- [Desert Diary](https://oldschool.runescape.wiki/w/Special:PermanentLink/14995592)
- [Falador Diary](https://oldschool.runescape.wiki/w/Special:PermanentLink/14995040)
- [Fremennik Diary](https://oldschool.runescape.wiki/w/Special:PermanentLink/14988557)
- [Kandarin Diary](https://oldschool.runescape.wiki/w/Special:PermanentLink/14990816)
- [Karamja Diary](https://oldschool.runescape.wiki/w/Special:PermanentLink/14989774)
- [Kourend & Kebos Diary](https://oldschool.runescape.wiki/w/Special:PermanentLink/14973640)
- [Lumbridge & Draynor Diary](https://oldschool.runescape.wiki/w/Special:PermanentLink/14973189)
- [Morytania Diary](https://oldschool.runescape.wiki/w/Special:PermanentLink/14990508)
- [Varrock Diary](https://oldschool.runescape.wiki/w/Special:PermanentLink/14973949)
- [Western Provinces Diary](https://oldschool.runescape.wiki/w/Special:PermanentLink/14975182)
- [Wilderness Diary](https://oldschool.runescape.wiki/w/Special:PermanentLink/14990198)

Ourania rune cutoffs follow [Mod Ash’s published table](https://pbs.twimg.com/media/EjhU5x4XYAAu2qa?format=png). The diary doubling estimates and essence XP multipliers follow the [historical Ourania Altar article](https://oldschool.runescape.wiki/w/Special:PermanentLink/14994195).


The fairy ring coordinates and quest gates follow the [historical fairy ring network](https://oldschool.runescape.wiki/w/Special:PermanentLink/14997474).
Prayer inputs, token exchange and worship rewards follow the [Ectofuntus](https://oldschool.runescape.wiki/w/Special:PermanentLink/14967511).
The Abyss layout and obstacle rolls follow the [historical Abyss](https://oldschool.runescape.wiki/w/Special:PermanentLink/14995568).
Rift unlocks follow the [blood rift](https://oldschool.runescape.wiki/w/Special:PermanentLink/14450084)
and [soul rift](https://oldschool.runescape.wiki/w/Special:PermanentLink/14881072).
Equipment charges follow the [Explorer's ring](https://oldschool.runescape.wiki/w/Special:PermanentLink/14962848)
and [Morytania legs](https://oldschool.runescape.wiki/w/Special:PermanentLink/14962849).
